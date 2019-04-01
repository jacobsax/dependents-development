from flask import Blueprint, render_template, abort, jsonify, request
from jinja2 import TemplateNotFound
import traceback
import os
import requests
import neo4j_queries
import utils

artifact_blueprint = Blueprint('artifact_blueprint', __name__)

@artifact_blueprint.route('/<group>/<project>/dependents/transitive', methods=['GET'])
def fetch_package_transitive_dependents(group, project):
    print (group)
    print (project)

    try:
        driver = utils.get_neo4j()
        try:
            with driver.session() as session:

                if session.read_transaction(neo4j_queries.artifact_exists, group, project):
                    transitive_artifacts_search = session.read_transaction(neo4j_queries.get_transitive_artifact_dependents, group, project)
                    artifacts_cache_count= session.read_transaction(neo4j_queries.get_artifact_dependents_total_cached, group, project)
                    artifacts_search_count = session.read_transaction(neo4j_queries.get_artifact_dependents_count, group, project)
                    estimated_transitive_artifacts_count = int(float(transitive_artifacts_search.get('count') / artifacts_search_count) * float(artifacts_cache_count))

                    return jsonify({'status': 'ok', 
                    'transitive-artifacts-search': transitive_artifacts_search, 
                    'artifacts-cache': {'count': artifacts_cache_count},
                    'artifacts-search': {'count': artifacts_search_count},
                    'predictions': {'estimated-transitive-artifacts-count': estimated_transitive_artifacts_count}
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

# returns the current dependents search state
@artifact_blueprint.route('/<group>/<project>/dependents/state', methods=['GET'])
def are_artifacts_parsed(group, project):
    print (group)
    print (project)

    try:
        driver = utils.get_neo4j()
        try:
            with driver.session() as session:
                if session.read_transaction(neo4j_queries.project_exists, group, project):
                    artifacts = session.read_transaction(neo4j_queries.get_project_packages, group, project)

                    for artifact in artifacts:
                        ast_parse_state = session.read_transaction(neo4j_queries.retrieve_artifact_attribute_value, artifact.get("group"), artifact.get("artifact"), 'dependentsearch')
                        if (ast_parse_state == None):
                            artifact["search-state"] = "not-searched"
                        else:
                            artifact["search-state"] = ast_parse_state

                    return jsonify({'status': 'ok', 'artifacts': artifacts}), 200
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