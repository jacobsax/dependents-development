from rq import Connection, Queue, Worker
import requests
import utils

if __name__ == '__main__':
    # Tell rq what Redis connection to use
    with Connection(utils.get_redis()):
        q = Queue('java_ast')
        Worker(q).work()