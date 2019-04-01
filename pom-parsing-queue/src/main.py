from rq import Queue
from redis import Redis
from worker_jobs import *
import utils
import time
from flask import Flask, jsonify, request
import neo4j_queries

app = Flask(__name__)

# Tell RQ what Redis connection to use
redis_conn = utils.get_redis()

# define three different priority queues for jobs to be added to
q_high = Queue('highdepends', connection=redis_conn)  
q_medium = Queue('mediumdepends', connection=redis_conn) 
q_low = Queue('lowdepends', connection=redis_conn)  


@app.route('/dependents/project', methods=['POST'])
def create_parse_project_task():
    if not request.json or not 'github_short_url' in request.json:
        return 400

    task = {
        'github_short_url': request.json['github_short_url'],
    }

    job = q_high.enqueue(fetch_project, task.get('github_short_url'), timeout=180)

    return jsonify({'task': task}), 200

@app.route('/dependents/package', methods=['POST'])
def create_parse_package_task():
    if not request.json:
        return 400

    if not 'group' in request.json or not 'artifact' in request.json or not 'start' in request.json or not 'end' in request.json or not 'parent' in request.json:
        return 400

    task = {
        'group': request.json['group'],
        'artifact': request.json['artifact'],
        'start': request.json['start'],
        'end': request.json['end'],
        'priority': request.json.get('priority'),
        'parent': request.json['parent']
    }

    driver = utils.get_neo4j()
    try:
        with driver.session() as session:
            """
            Guard against re-searching the same repository. This is an imperfect solution, 
            as it only guards against the parsing being initiated again with a start value for
            the records to be retrieved during search <= 1. It doesn't guard against a search
            which overlaps with a previous search. 

            For example, it doesn't guard against the following
                1. search request received to search identified repositories 1 to 100
                2. search request received to search identified repositories 50 to 100.

                In this instance, the repositories 50 to 100 wil be searched twice.
            """
            parsed = session.read_transaction(neo4j_queries.is_package_parsed, task.get('group'), task.get('artifact'))
            
            if (parsed == "completed"):
                print("Parsing already completed on repository")
                return jsonify({'state': "completed"}), 202

            if (parsed == "in-progress" and task.get("start") <= 1):
                print("Parsing already in progress on repository")
                return jsonify({'state': "in-progress"}), 201

    except Exception:
        traceback.print_exc()
    finally:
        driver.close()

    if (task.get('priority') == 'high'):
        job = q_medium.enqueue(fetch_package, task.get('group'), task.get('artifact'), task.get('start'), task.get('end'), task.get('parent'), task.get('priority'), timeout=3600)
    else:
        job = q_low.enqueue(fetch_package, task.get('group'), task.get('artifact'), task.get('start'), task.get('end'), task.get('parent'), task.get('priority'), timeout=3600)

    return jsonify({'task': task}), 200

if __name__ == '__main__':
    app.run(debug=True)
