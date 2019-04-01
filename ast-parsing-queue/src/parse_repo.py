import os
import tempfile
import contextlib
import shutil
import utils
import subprocess
import traceback
import neo4j_queries

# function from https://stackoverflow.com/questions/3223604/how-to-create-a-temporary-directory-and-get-the-path-file-name-in-python
@contextlib.contextmanager
def cd(newdir, cleanup=lambda: True):
        prevdir = os.getcwd()
        os.chdir(os.path.expanduser(newdir))
        try:
                yield
        finally:
                os.chdir(prevdir)
                cleanup()

# function from https://stackoverflow.com/questions/3223604/how-to-create-a-temporary-directory-and-get-the-path-file-name-in-python
@contextlib.contextmanager
def tempdir():
        dirpath = tempfile.mkdtemp()
        def cleanup():
                shutil.rmtree(dirpath)
        with cd(dirpath, cleanup):
                yield dirpath

# parse_repo parses a repo
# parsing type should be 'all' or 'packageclassonly'
def parse_repo(git_short_url, parsing_type):
        print ("Parsing project: " + git_short_url)

        if (parsing_type != 'all' and parsing_type != 'packageclassonly'):
                print ("ERROR: parsing type must either be 'all' or 'packageclassonly'")
                print (parsing_type)
                return

        # add an attribute to the project, describing the parsing that has taken place
        try:
                driver = utils.get_neo4j()
                try:
                        with driver.session() as session:
                                # TODO: a potential race condition exists here. If two jobs are submitted to parse a repo, 
                                # and both are accepted by two separate parsing workers at the same time, then both may query
                                # the parsing state at the same time, and both may carry out parsing. This won't actually cause
                                # any errors, but it does waste resources
                                result = session.write_transaction(neo4j_queries.retrieve_attribute_value, git_short_url, 'ast-parsed')
                                if (result == parsing_type or result == 'in-progress' or result == 'all'):
                                        print("AST tree already parsed for project {}".format(git_short_url))
                                        return

                                result = session.write_transaction(neo4j_queries.update_attribute_of_project, git_short_url, 'ast-parsed', 'in-progress')
                except Exception:
                        traceback.print_exc()
                finally:
                        driver.close()
        except:
                traceback.print_exc()
                return

        # create a new temporary directory
        with tempdir() as dirpath:
                print("using dir " + dirpath)
                os.chdir(dirpath)

                clone_path = "git clone https://github.com/{}.git".format(git_short_url)
                print(clone_path)
                os.system(clone_path) # clone the git repo
                cloned_dir = os.listdir(dirpath)
                if (len(cloned_dir) == 1):
                        os.chdir(dirpath + "/" + cloned_dir[0])

                        os.system("mvn dependency:copy-dependencies") # download all dependencies to ./target/dependencies
                        
                        # Run parser
                        print("Parsing Java Project to produce AST tree")
                        result = os.system("java -Xmx1g -jar /java-parser/target/java_parser_cli.jar-jar-with-dependencies.jar -i {} -j {}/{} -s {}/{} -l cypher -t {} -o {}/output.cypher".format(git_short_url, dirpath, cloned_dir[0], dirpath, cloned_dir[0], parsing_type, dirpath))
                        if (result != 0):
                                print("Error occurred parsing AST tree")
                                try:
                                        driver = utils.get_neo4j()
                                        try:
                                                with driver.session() as session:
                                                        session.write_transaction(neo4j_queries.update_attribute_of_project, git_short_url, 'ast-parsed', 'failed')
                                        except Exception:
                                                traceback.print_exc()
                                        finally:
                                                driver.close()
                                except:
                                        traceback.print_exc()
                                        return
                                return

                        print("Exporting to neo4j")
                        # Export output to neo4j, subprocess.call returns the status code of the call
                        export_to_neo4j_output = subprocess.call("set -eo pipefail; cat {}/output.cypher | cypher-shell -a $NEO4J_IP -u $NEO4J_USER -p $NEO4J_PASS".format(dirpath), shell=True, executable='/bin/bash')
                        if (export_to_neo4j_output != 0):
                                try:
                                        driver = utils.get_neo4j()
                                        try:
                                                with driver.session() as session:
                                                        session.write_transaction(neo4j_queries.update_attribute_of_project, git_short_url, 'ast-parsed', 'failed')
                                        except Exception:
                                                traceback.print_exc()
                                        finally:
                                                driver.close()
                                except:
                                        traceback.print_exc()
                                        return

                                print("Error occurred adding call graph to Neo4j")
                                return
                        
                        # add an attribute to the project, describing the parsing that has taken place
                        try:
                                driver = utils.get_neo4j()
                                try:
                                        with driver.session() as session:
                                                print("Completed parsing of project")
                                                session.write_transaction(neo4j_queries.update_attribute_of_project, git_short_url, 'ast-parsed', parsing_type)
                                except Exception:
                                        traceback.print_exc()
                                finally:
                                        driver.close()
                        except:
                                traceback.print_exc()
                                return
                return