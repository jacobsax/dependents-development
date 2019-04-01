from flask import Blueprint, render_template, abort, jsonify, request
from jinja2 import TemplateNotFound
import traceback
import os
import requests
import neo4j_queries
import utils

ast_blueprint = Blueprint('ast_blueprint', __name__)

@ast_blueprint.route("/<group>/<project>/dependent", methods=['Get'])
def fetch_dependent_ast(group, project):
    print (group)  
    print (project)

    dependent_group = request.args.get('group')
    print(dependent_group)
    dependent_repo = request.args.get('repo')
    print(dependent_repo)

    sub_node_label  = request.args.get('label')
    sub_node_id = request.args.get('id')

    try:
        driver = utils.get_neo4j()
        try:
            with driver.session() as session:

                if session.read_transaction(neo4j_queries.project_exists, group, project) and session.read_transaction(neo4j_queries.project_exists, dependent_group, dependent_repo):
                    ast_result = session.read_transaction(neo4j_queries.ast_tree_dependent, group, project, dependent_group, dependent_repo, sub_node_label, sub_node_id)
                    
                    return jsonify({'status': 'ok', 
                        'ast': ast_result, 
                    }), 200
                else:
                    return jsonify({'status': 'ERROR', 'reason': 'Artifact does not exist in Neo4j'}), 400
        except:
            traceback.print_exc()
            return jsonify({'status': 'SERVER_ERROR'}), 500
        finally:
            driver.session().close()
    except:
        traceback.print_exc()
        return jsonify({'status': 'SERVER_ERROR'}), 500

    return jsonify({'status': 'SERVER_ERROR'}), 500


# returns the current Abstract Syntax Tree Parsing State
@ast_blueprint.route('/<group>/<project>/state', methods=['GET'])
def is_ast_parsed(group, project):
    print (group)
    print (project)

    try:
        driver = utils.get_neo4j()
        try:
            with driver.session() as session:
                if session.read_transaction(neo4j_queries.project_exists, group, project):
                    ast_parse_state = session.read_transaction(neo4j_queries.retrieve_project_attribute_value, "{}/{}".format(group, project), 'ast-parsed')
                    if (ast_parse_state == None):
                        return jsonify({'status': 'ok', 'state': "not-parsed"}), 200
                    else:
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