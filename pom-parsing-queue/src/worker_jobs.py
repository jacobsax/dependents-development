import urllib
import re
import utils
import requests
import json
import traceback
import neo4j_queries


"""
parse_pom parses a pom file of a repository to identify dependencies and artifacts declared 
in the pom file. For each of the identified dependency, it adds the dependency to the Neo4j database. 
For each of the artifacts, it then sends a request to the queue manager to search for repositories 
dependent on the artifact.

Arguments:
 - session - a Neo4j session object
 - pom - a pom object retrieved from the pom search service
 - repo_name - the git short url of the repo
 - priority - the priority of subsequent dependent searches to be completed (can be 'high' or 'low')
"""
def parse_pom(session, pom, repo_name, priority):
        if (pom == None):
                return

        # retrieve the URL of the queue manager
        queue_manager = utils.get_queue_manager()

        # Add each dependency to Neo4j
        for dependency in pom.get('dependencies'):
                dependency_group = dependency.get('group')
                dependency_artifact = dependency.get('artifact')

                print ("parsed dependency from pom " + str(dependency_group or ''))
                print ("with artifact " + str(dependency_artifact or ''))

                if (dependency_group != None and dependency_artifact != None):
                        session.write_transaction(neo4j_queries.add_artifact_node, dependency_group, dependency_artifact)

        # Then for each artifact declared as produced in the pom file, request search
        # of dependent repositories to this artifact 
        for artifact in pom.get('artifacts'):
                artifact_group = artifact.get('group')
                artifact_artifact = artifact.get('artifact')

                print ("parsed group from pom " + str(artifact_group or ''))
                print ("with artifact " + str(artifact_artifact or ''))

                if (artifact_group != None and artifact_artifact != None):
                        # Add the artifact to neo4j
                        session.write_transaction(neo4j_queries.add_artifact_node, artifact_group, artifact_artifact)
                        session.write_transaction(neo4j_queries.add_project_artifact_edge, repo_name, artifact_group, artifact_artifact)
                        
                        # Request a search for dependents to the artifact. This request is made to the
                        # queue manager.
                        next_parse_response = requests.post("http://{}/dependents/package".format(queue_manager), json = {'group': artifact_group, 'artifact': artifact_artifact, 'start': 1, 'end': 1000, 'priority': priority, 'parent': repo_name})
                        if (next_parse_response.status_code != 200):
                                print("Error occurred trying to request parsing of next section")

                        # Add each identified dependency to the Neo4j database
                        for dependency in pom.get('dependencies'):
                                dependency_group = dependency.get('group')
                                dependency_artifact = dependency.get('artifact')

                                if (dependency_group != None and dependency_artifact != None):
                                        session.write_transaction(neo4j_queries.add_artifact_depends_edge, artifact_group, artifact_artifact, dependency_group, dependency_artifact)

"""
Fetches information about a project from the pom-search-service. fetch_project is a python-RQ job, 
automatically invoked on receipt of a fetch_project job.

Arguments:
 - github_short_url - the short URL of the repository to fetch information about (a String in the format "org/repository")
"""
def fetch_project(github_short_url):
        try:
                driver = utils.get_neo4j()
                depends_service_url = utils.get_depends_service()
                try:
                        # request information about the project from the pom-search-service
                        response = requests.get("{}/java/project/{}?remote=true".format(depends_service_url, github_short_url))
                        if (response.status_code != 200):
                                return

                        # the response is returned as JSON, parse it into a python object  
                        parsed_response = json.loads(response.content)
                        
                        # retrieve the name of the repository from response
                        repo_name = parsed_response.get('github_repo_name') 
                        if (repo_name != None):
                                with driver.session() as session:
                                        # add the repository to neo4j as a new vertices
                                        session.write_transaction(neo4j_queries.add_project_node, repo_name)

                                        # record that this project has been searched
                                        session.write_transaction(neo4j_queries.add_attribute_to_project, repo_name, "projectsearch", "True")

                                        # Parse the pom file associated with the pom file. This will
                                        # initiate searches for any repositories dependent on
                                        # artifacts produced by this repository.
                                        for pom in parsed_response.get('pom'):
                                                parse_pom(session, pom, repo_name, 'high')
                except Exception:
                        traceback.print_exc()
                finally:
                        driver.close()
        except Exception:
                traceback.print_exc()
                return

"""
fetch_package is a python-rq job, that is automatically initiated by python-rq on receipt of a job.

It initiates a search for dependent repositories of a defined artifact, and then stores the identified
dependent repositories to Neo4j.

Arguments:
        - package_group - the group name of the artifact
        - package_artifact - the artifact name for the artifact
        - search_start - the start position for the search results (search results are returned by the pom-search-service in a paginated manner)
        - search_end - the end position for the search results
        - parent_project - the project which produces the defined artifact
        - continuation_priority - the priority at which futher searches for dependent's of this artifact
                 should be executed. Can be 'high' or 'low'. 
"""
def fetch_package(package_group, package_artifact, search_start, search_end, parent_project, continuation_priority):
        # validate that all arguments are correct
        if (package_group == None or package_artifact == None or search_start < 0 or search_end < search_start):
                print ("Cannot parse package " + str(package_group or '') + "." + str(package_artifact or ''))
                return

        try:
                driver = utils.get_neo4j()
                depends_service_url = utils.get_depends_service()
                queue_manager = utils.get_queue_manager()
                try:
                        with driver.session() as session:
                                try:
                                        # don't carry out parsing if the search is already completed
                                        parsed = session.read_transaction(neo4j_queries.is_package_parsed, package_group, package_artifact)
                                        if (parsed == "completed"):
                                                print ("Package " + package_group + "." + package_artifact + " at parsing state " + parsed)
                                                return

                                        print ("Parsing package " + package_group + "." + package_artifact)
                                        session.write_transaction(neo4j_queries.add_attribute_to_artifact, package_group, package_artifact, "dependentsearch", "in-progress")
                                        
                                        # search for dependents using the pom-search-service
                                        response = requests.get("{}/java/package/{}/{}/dependents/local?pom=true&start={}&end={}".format(depends_service_url, package_group, package_artifact, search_start, search_end - search_start))
                                        if (response.status_code != 200):
                                                print("Couldn't retrieve dependents from dependents service")
                                                return

                                                
                                        parsed_response = json.loads(response.content)

                                        print ("Received packages from dependents service " + package_group + "." + package_artifact)

                                        total_count = parsed_response.get("total_count")

                                        # returned repositories are paginated - meaning that only a subset of all are returned
                                        # at any one time. If the current end to the paginated search is less than the total count,
                                        # then send a request to the queue manager to carry out the search with the next set of 
                                        # paginated results
                                        if (search_end < total_count):
                                                # Identify the new end position for pagination, maintaining the same number of returned
                                                # results as in this search
                                                new_search_end = search_end + (search_end - search_start)
                                                if (new_search_end > total_count):
                                                        new_search_end = total_count

                                                # request the next search
                                                next_parse_response = requests.post("http://{}/dependents/package".format(queue_manager), json = {'group': package_group, 'artifact': package_artifact, 'start': search_end, 'end': new_search_end, 'parent': parent_project, 'priority': continuation_priority})
                                                if (next_parse_response.status_code != 200):
                                                        print("Error occurred trying to request parsing of next section")

                                        # store the total count of dependents to the artifact
                                        session.write_transaction(neo4j_queries.add_attribute_to_artifact, package_group, package_artifact, "total_count", total_count)
                                        
                                        # for each identified dependent project, add it to Neo4j
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