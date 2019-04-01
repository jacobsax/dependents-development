import urllib
import re
import utils
import requests
import json
import traceback
import neo4j_queries

def parse_pom(session, pom, repo_name, priority):
        if (pom == None):
                return

        queue_manager = utils.get_queue_manager()

        for dependency in pom.get('dependencies'):
                dependency_group = dependency.get('group')
                dependency_artifact = dependency.get('artifact')

                print ("parsed dependency from pom " + str(dependency_group or ''))
                print ("with artifact " + str(dependency_artifact or ''))

                if (dependency_group != None and dependency_artifact != None):
                        session.write_transaction(neo4j_queries.add_artifact_node, dependency_group, dependency_artifact)

        for artifact in pom.get('artifacts'):
                artifact_group = artifact.get('group')
                artifact_artifact = artifact.get('artifact')

                print ("parsed group from pom " + str(artifact_group or ''))
                print ("with artifact " + str(artifact_artifact or ''))

                if (artifact_group != None and artifact_artifact != None):
                        session.write_transaction(neo4j_queries.add_artifact_node, artifact_group, artifact_artifact)
                        session.write_transaction(neo4j_queries.add_project_artifact_edge, repo_name, artifact_group, artifact_artifact)
                        
                        next_parse_response = requests.post("http://{}/dependents/package".format(queue_manager), json = {'group': artifact_group, 'artifact': artifact_artifact, 'start': 1, 'end': 1000, 'priority': priority, 'parent': repo_name})
                        if (next_parse_response.status_code != 200):
                                print("Error occurred trying to request parsing of next section")

                        for dependency in pom.get('dependencies'):
                                dependency_group = dependency.get('group')
                                dependency_artifact = dependency.get('artifact')

                                if (dependency_group != None and dependency_artifact != None):
                                        session.write_transaction(neo4j_queries.add_artifact_depends_edge, artifact_group, artifact_artifact, dependency_group, dependency_artifact)

def fetch_project(github_short_url):
        try:
                driver = utils.get_neo4j()
                depends_service_url = utils.get_depends_service()
                try:
                        response = requests.get("{}/java/project/{}?remote=true".format(depends_service_url, github_short_url))
                        if (response.status_code != 200):
                                return
                                
                        parsed_response = json.loads(response.content)
                        
                        repo_name = parsed_response.get('github_repo_name') 
                        if (repo_name != None):
                                with driver.session() as session:
                                        session.write_transaction(neo4j_queries.add_project_node, repo_name)

                                        # record that this project has been searched
                                        session.write_transaction(neo4j_queries.add_attribute_to_project, repo_name, "projectsearch", "True")

                                        for pom in parsed_response.get('pom'):
                                                parse_pom(session, pom, repo_name, 'high')
                except Exception:
                        traceback.print_exc()
                finally:
                        driver.close()
        except Exception:
                traceback.print_exc()
                return

def fetch_package(package_group, package_artifact, search_start, search_end, parent_project, continuation_priority):
        if (package_group == None or package_artifact == None or search_start < 0 or search_end < search_start):
                print ("Cannot parse package " + str(package_group or '') + "." + str(package_artifact or ''))
                return

        print("here 1")

        try:
                driver = utils.get_neo4j()
                depends_service_url = utils.get_depends_service()
                queue_manager = utils.get_queue_manager()
                try:
                        with driver.session() as session:
                                try:
                                        print("here 2")
                                        # don't parse if the search is already completed
                                        parsed = session.read_transaction(neo4j_queries.is_package_parsed, package_group, package_artifact)
                                        if (parsed == "completed"):
                                                print ("Package " + package_group + "." + package_artifact + " at parsing state " + parsed)
                                                return

                                        print ("Parsing package " + package_group + "." + package_artifact)
                                        session.write_transaction(neo4j_queries.add_attribute_to_artifact, package_group, package_artifact, "dependentsearch", "in-progress")

                                        response = requests.get("{}/java/package/{}/{}/dependents/local?pom=true&start={}&end={}".format(depends_service_url, package_group, package_artifact, search_start, search_end - search_start))
                                        if (response.status_code != 200):
                                                print("Couldn't retrieve dependents from dependents service")
                                                return

                                                
                                        parsed_response = json.loads(response.content)

                                        print ("Received packages from dependents service " + package_group + "." + package_artifact)

                                        total_count = parsed_response.get("total_count")
                        

                                        if (search_end < total_count):
                                                new_search_end = search_end + (search_end - search_start)
                                                if (new_search_end > total_count):
                                                        new_search_end = total_count

                                                next_parse_response = requests.post("http://{}/dependents/package".format(queue_manager), json = {'group': package_group, 'artifact': package_artifact, 'start': search_end, 'end': new_search_end, 'parent': parent_project, 'priority': continuation_priority})
                                                if (next_parse_response.status_code != 200):
                                                        print("Error occurred trying to request parsing of next section")

                                        session.write_transaction(neo4j_queries.add_attribute_to_artifact, package_group, package_artifact, "total_count", total_count)
                                        project_count = search_start
                                        for project in parsed_response.get("projects"):
                                                repo_name = project.get('github_repo_name') 

                                                session.write_transaction(neo4j_queries.add_project_node, repo_name)
                                                session.write_transaction(neo4j_queries.add_project_node, parent_project)
                                                session.write_transaction(neo4j_queries.add_project_depends_project_edge, repo_name, parent_project)
                                
                                                for pom in project.get("pom"):
                                                        parse_pom(session, pom, repo_name, 'low')

                                                project_count = project_count + 1

                                        #if search_end >= total_count, then add attribute to package stating it has been parsed. 
                                        if (search_end >= total_count):
                                                print ("adding")
                                                session.write_transaction(neo4j_queries.update_attribute_of_artifact, package_group, package_artifact, "dependentsearch", "completed")
                                                print ("added")
                                except Exception:
                                        traceback.print_exc()
                                        session.write_transaction(neo4j_queries.update_attribute_of_artifact, package_group, package_artifact, "dependentsearch", "failed")
                except Exception:
                        traceback.print_exc()
                finally:
                        driver.close()
                        return
        except:
                print("Error occurred parsing package " + str(package_group or '') + "." + str(package_artifact or ''))
                return