from flask import Blueprint, render_template, abort, jsonify, request
from jinja2 import TemplateNotFound
import traceback
import os
import requests
import neo4j_queries
import utils
import json

project_blueprint = Blueprint('project_blueprint', __name__)
pom_search_service = utils.get_depends_service()

@project_blueprint.route('/<group>/<project>/validate', methods=['GET'])
def validate_github_repository(group, project):
    response = requests.get("{}/java/project/{}/{}?remote=true".format(pom_search_service, group, project))

    if (response.status_code == 404):
        return jsonify({"state": "invalid-repo"}), 404
    elif (response.status_code != 200):
        return jsonify({"state": "error"}), response.status_code

    return jsonify({'state': "ok"}), 200

"""
This endpoint retrieves the direct children vertices of a node in the call graph of
the specified project. Each child is returned with a url, which can be called to retrieve
the children vertices of the child.
"""
@project_blueprint.route("/<group>/<project>/retrieve/children", methods=['Get'])
def retrieve_children_of_node(group, project):
    node_label = request.args.get('label')
    node_id = request.args.get('id')

    dependent_group = request.args.get('dependent_group')
    dependent_repo = request.args.get('dependent_repo')

    try:
        driver = utils.get_neo4j()
        try:
            with driver.session() as session:
                if session.read_transaction(neo4j_queries.project_exists, group, project):
                    if (dependent_group != None and dependent_repo != None):
                        if session.read_transaction(neo4j_queries.project_exists, dependent_group, dependent_repo):
                            result = session.read_transaction(neo4j_queries.contains_from_node, group, project, node_label, node_id, dependent_group, dependent_repo)
                            return jsonify({'status': 'ok', 'data': result}), 200                           
                        else:
                            return jsonify({'status': 'ERROR', 'reason': 'Project does not exist in Neo4j. Have you submitted a parse job to /init/dependents-search/pom yet?'}), 400
                    else:
                        result = session.read_transaction(neo4j_queries.contains_from_node, group, project, node_label, node_id, None, None)
                        return jsonify({'status': 'ok', 'data': result}), 200
                else:
                    return jsonify({'status': 'ERROR', 'reason': 'Project does not exist in Neo4j. Have you submitted a parse job to /init/dependents-search/pom yet?'}), 400
        except:
            traceback.print_exc()
            return jsonify({'status': 'SERVER_ERROR'}), 500
        finally:
            driver.session().close()
    except:
        traceback.print_exc()
        return jsonify({'status': 'SERVER_ERROR'}), 500

    return jsonify({'status': 'SERVER_ERROR'}), 500

# retrieve the dependents of an AST node of the project based on method calls
@project_blueprint.route("/<group>/<project>/retrieve/dependents", methods=['Get'])
def retrieve_dependents_of_node(group, project):
    node_label = request.args.get('label')
    node_id = request.args.get('id')

    try:
        driver = utils.get_neo4j()
        try:
            with driver.session() as session:
                if session.read_transaction(neo4j_queries.project_exists, group, project):
                    result = session.read_transaction(neo4j_queries.dependents_from_node, group, project, node_label, node_id)
                    return jsonify({'status': 'ok', 'data': result}), 200
                else:
                    return jsonify({'status': 'ERROR', 'reason': 'Project does not exist in Neo4j. Have you submitted a parse job to /init/dependents-search/pom yet?'}), 400
        except:
            traceback.print_exc()
            return jsonify({'status': 'SERVER_ERROR'}), 500
        finally:
            driver.session().close()
    except:
        traceback.print_exc()
        return jsonify({'status': 'SERVER_ERROR'}), 500

    return jsonify({'status': 'SERVER_ERROR'}), 500

@project_blueprint.route('/<group>/<project>/artifacts', methods=['GET'])
def fetch_project_artifacts(group, project):
    print (group)
    print (project)

    try:
        driver = utils.get_neo4j()
        try:
            with driver.session() as session:
                if session.read_transaction(neo4j_queries.project_exists, group, project):
                    result = session.read_transaction(neo4j_queries.get_project_packages, group, project)
                    return jsonify({'status': 'ok', 'packages': result}), 200
                else:
                    return jsonify({'status': 'ERROR', 'reason': 'Project does not exist in Neo4j. Have you submitted a parse job to /init/dependents-search/pom yet?'}), 400
        except:
            traceback.print_exc()
            return jsonify({'status': 'SERVER_ERROR'}), 500
        finally:
            driver.session().close()
    except:
        traceback.print_exc()
        return jsonify({'status': 'SERVER_ERROR'}), 500

    return jsonify({'status': 'SERVER_ERROR'}), 500

# retrieve the dependents of the project
@project_blueprint.route('/<group>/<project>/dependents', methods=['GET'])
def fetch_project_dependents(group, project):
    print (group)
    print (project)

    try:
        driver = utils.get_neo4j()
        try:
            with driver.session() as session:
                if session.read_transaction(neo4j_queries.project_exists, group, project):
                    projects_search = session.read_transaction(neo4j_queries.get_project_dependents, group, project)
                    projects_search_count = session.read_transaction(neo4j_queries.get_project_dependents_total_cached, group, project)

                    return jsonify({'status': 'ok', 'projects-search': projects_search, 'projects-cache': {'count': projects_search_count}}), 200
                else:
                    return jsonify({'status': 'ERROR', 'reason': 'Project does not exist in Neo4j. Have you submitted a parse job to /init/dependents-search/pom yet?'}), 400
        except:
            traceback.print_exc()
            return jsonify({'status': 'SERVER_ERROR'}), 500
        finally:
            driver.session().close()
    except:
        traceback.print_exc()
        return jsonify({'status': 'SERVER_ERROR'}), 500

    return jsonify({'status': 'SERVER_ERROR'}), 500

# fetch the dependencies of the project
@project_blueprint.route('/<group>/<project>/dependencies', methods=['GET'])
def fetch_project_dependencies(group, project):
    print (group)
    print (project)

    try:
        driver = utils.get_neo4j()
        try:
            with driver.session() as session:
                if session.read_transaction(neo4j_queries.project_exists, group, project):
                    dependencies = session.read_transaction(neo4j_queries.get_project_dependencies, group, project)
                    
                    projects_search_count = None    
                    return jsonify({'status': 'ok', 'artifacts': dependencies.get('artifacts'), 'count': dependencies.get("count")}), 200
                else:
                    return jsonify({'status': 'ERROR', 'reason': 'Project does not exist in Neo4j. Have you submitted a parse job to /init/dependents-search/pom yet?'}), 400
        except:
            traceback.print_exc()
            return jsonify({'status': 'SERVER_ERROR'}), 500
        finally:
            driver.session().close()
    except:
        traceback.print_exc()
        return jsonify({'status': 'SERVER_ERROR'}), 500

    return jsonify({'status': 'SERVER_ERROR'}), 500

# returns the current dependents search state
@project_blueprint.route('/<group>/<project>/dependents/state', methods=['GET'])
def is_ast_parsed(group, project):
    print (group)
    print (project)

    try:
        driver = utils.get_neo4j()
        try:
            with driver.session() as session:
                if session.read_transaction(neo4j_queries.project_exists, group, project):
                    ast_parse_state = session.read_transaction(neo4j_queries.retrieve_project_attribute_value, "{}/{}".format(group, project), 'projectsearch')
                    print(ast_parse_state)
                    return jsonify({'status': 'ok', 'state': ast_parse_state}), 200
                else:
                    return jsonify({'status': 'ERROR', 'reason': 'Project does not exist in Neo4j. Have you submitted a parse job to /init/dependents-search/pom yet?'}), 400
        except:
            traceback.print_exc()
            return jsonify({'status': 'SERVER_ERROR'}), 500
        finally:
            driver.session().close()
    except:
        traceback.print_exc()
        return jsonify({'status': 'SERVER_ERROR'}), 500

    return jsonify({'status': 'SERVER_ERROR'}), 500