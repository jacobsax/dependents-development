from rq import Connection, Queue, Worker
import requests
import utils
import os

# Start a worker and begin listening on the redis job queue
if __name__ == '__main__':
    # Tell rq what Redis connection to use
    with Connection(utils.get_redis()):
        queue = os.environ.get('RQ_QUEUE')
        q = Queue(queue)
        Worker(q).work()