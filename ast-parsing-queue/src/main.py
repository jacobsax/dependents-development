from rq import Queue
from redis import Redis
from parse_repo import *
import utils
import time
from flask import Flask, jsonify, request
import neo4j_queries

# Tell RQ what Redis connection to use
redis_conn = utils.get_redis()
q = Queue('java_ast', connection=redis_conn)  # no args implies the default queue

app = Flask(__name__)

def is_package_parsed(tx, git_short_url, parsing_type):
        result = tx.run("MATCH (p:Project {{id: '{}'}})-[:Attribute]-(a:ProjectAttribute {{name: 'ast-parsed'}}) RETURN a.value".format(git_short_url))
        if (result == None):
                return False

        single = result.single()
        if (single == None):
                return False
        
        if (single[0] == parsing_type):
                return True
        else:
                return False

@app.route('/java/is-parsed', methods=['POST'])
def java_is_parsed():
    if not request.json or not 'github_short_url' in request.json or not 'parsing_type' in request.json:
        return jsonify({'Error': 'Must POST JSON request with github_short_url and parsing_type fields'}), 400

    if (request.json['parsing_type'] != 'all' and request.json['parsing_type'] != 'packageclassonly'):
        return jsonify({'Error': "Parsing type must either be 'all' or 'packageclassonly'"}), 400

    task = {
        'github_short_url': request.json['github_short_url'],
        'parsing_type': request.json['parsing_type'],
    }

    try:
        driver = utils.get_neo4j()
        try:
            with driver.session() as session:
                result = session.read_transaction(is_package_parsed, tasks.get('github_short_url'), task.get('parsing_type'), parsing_type)
                return jsonify({'is-parsed': result}), 200
        except Exception:
            traceback.print_exc()
            return jsonify({'Error': 'Error occurred connecting to neo4j'}), 500
        finally:
            driver.close()
            return
    except:
        traceback.print_exc()
        return jsonify({'Error': 'Error occurred fetching neo4j driver'}), 500


@app.route('/java/parse', methods=['POST'])
def create_parse_project_task():
    if not request.json or not 'github_short_url' in request.json or not 'parsing_type' in request.json:
        return jsonify({'Error': 'Must POST JSON request with github_short_url and parsing_type fields'}), 400

    if (request.json['parsing_type'] != 'all' and request.json['parsing_type'] != 'packageclassonly'):
        return jsonify({'Error': "Parsing type must either be 'all' or 'packageclassonly'"}), 400

    task = {
        'github_short_url': request.json['github_short_url'],
        'parsing_type': request.json['parsing_type'],
    }

    try:
        driver = utils.get_neo4j()
        try:
            with driver.session() as session:
                # check if the project has already been parsed, or if parsing is in progress. If it is, then
                # don't requeue the project for parsing
                result = session.write_transaction(neo4j_queries.retrieve_attribute_value, request.json['github_short_url'], 'ast-parsed')
                if (result == request.json['parsing_type'] or result == 'in-progress' or result == 'all' or result == 'queued'):
                    print("AST tree already parsed for project {}".format(request.json['github_short_url']))
                    return

                session.write_transaction(neo4j_queries.add_attribute_to_project, request.json['github_short_url'], 'ast-parsed', 'queued')
        except Exception:
            traceback.print_exc()
        finally:
            driver.close()
    except:
        traceback.print_exc()
        return

    job = q.enqueue(parse_repo, task.get('github_short_url'), task.get('parsing_type'), timeout=7200)

    return jsonify({'task': task}), 200

if __name__ == '__main__':
    app.run(debug=True)