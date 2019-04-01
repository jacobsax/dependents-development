
import utils
import time
from flask import Flask, jsonify, request
import os
import requests
import neo4j_queries
import traceback
import project
from project.api import project_blueprint
from artifact.api import artifact_blueprint
from ast.api import ast_blueprint

queue_manager = utils.get_queue_manager()
ast_parsing_queue_manager = utils.get_ast_queue_manager()
dependents_service = utils.get_depends_service()

app = Flask(__name__)

app.register_blueprint(project_blueprint, url_prefix='/project')
app.register_blueprint(artifact_blueprint, url_prefix='/artifact')
app.register_blueprint(ast_blueprint, url_prefix='/ast')

# initiates a dependents search
@app.route('/init/dependents-search/pom', methods=['POST'])
def create_pom_parse_project_task():
    print("here 1")

    print(request)

    print(request.json)

    if not request.json or not 'github_short_url' in request.json:
        return jsonify({'status': 'error'}), 400

    task = {
        'github_short_url': request.json['github_short_url'],
    }

    forward_request = requests.post("http://{}/dependents/project".format(queue_manager), json = {'github_short_url': task.get('github_short_url')})
    if (forward_request.status_code != 200):
        print("Error occurred trying to request parsing of next section")
        return jsonify({"Error from backend service", forward_request.text}), forward_request.status_code

    return jsonify({'task': task}), 200

# initiates a dependents search
@app.route('/init/ast-search/java', methods=['POST'])
def create_ast_parse_project_task():
    # TODO: Don't parse if already parsed or parsing in progress
    # TODO: Should have to post two github urls, the root and the dependent - both should be parsed
    # TODO: Don't parse if project doesn't exist
    
    if not request.json or not 'github_short_url' in request.json or not 'parsing_type' in request.json:
        return jsonify({'Error': 'Must POST JSON request with github_short_url and parsing_type fields'}), 400

    if (request.json['parsing_type'] != 'all' and request.json['parsing_type'] != 'packageclassonly'):
        return jsonify({'Error': "Parsing type must either be 'all' or 'packageclassonly'"}), 400

    task = {
        'github_short_url': request.json['github_short_url'],
        'parsing_type': request.json['parsing_type'],
    }

    forward_request = requests.post("http://{}/java/parse".format(ast_parsing_queue_manager), json = task)
    if (forward_request.status_code != 200):
        print("Error occurred trying to request parsing of next section")
        return jsonify({"Error from backend service", forward_request.text}), forward_request.status_code

    return jsonify({'task': task}), 200

if __name__ == '__main__':
    app.run(debug=True)
