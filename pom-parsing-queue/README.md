# POM Parsing Queue

This component is responsible for finding all directly dependent and transitively dependent GitHub repositories to a given GitHub repository. It stores this information, as well as the Artifacts produced by each project, to a Neo4j graph database. 

This service is made up of two components - a queue manager, and a queue worker. Jobs to be executed by queue workers are stored into a Redis database - the queue manager adds jobs to this database, and workers retrieve and execute these jobs.

<b>POM Parsing Queue Manager</b> The Pom Parsing Queue Manager is responsible for managing the current dependent analysis jobs supplied to the workers. This component is written in Python. It uses the Python-RQ library to manage a distributed job queue, and the Python Flask Library to serve a REST API. The queue manager is horizontally scalable.

<b>POM Parsing Workers</b> A collection of horizontally scalable workers are responsible for triggering searches to be carried out by the [Dependent Finder Service](../pom-search-service/), and storing the retrieved data to Neo4j. This component is written in Python, and uses the Python-RQ library to receive jobs from a distributed job queue. It uses the official Neo4j python library to interface with Python for data storage, and the Python Requests library to make REST API calls.

<b>Redis Database</b> A Redis database exists to store the queue of jobs managed by the Python-RQ library for workers to complete. Redis is a highly performant in memory key-value store, which works well under high load.

To test this service, you will need to deploy a Redis database, and a Neo4j Database. Run 'cp credentials.sh.template credentials.sh' and fill out the required variables. Then execute 'source credentials.sh' to set up the shell environment correctly; run 'python3 src/main.py' to run the manager; and 'python3 src/worker.py' to run a worker.

To build the worker and manager into docker containers, run ./build.sh. How you should deploy these services is defined in the [docker-compose-services.yml](../docker-compose-services.yml.template) file at the top level of this repository.

The HTTP REST API to use to submit jobs to the manager is documented in a [Postman collection](./pom-queue-manager.postman_collection.json).