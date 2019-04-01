
# Java Parsing Queue Manager/Worker

This collection of services are the implementation of the single Abstract Syntax Tree Parsing Service specified in the system architecture at the design stage of this project 4.1. This service receives the GitHub Short URL of a repository. It parses the source code of the given repository to construct an Abstract Syntax Tree, and then traverses the tree to generate a Call Graph at the Method level. It stores the produced Call Graph for a project in the Neo4j database.

This dir stores two components:
* Java Parsing Worker is a python-based worker, which parses the AST tree of Java projects on github. This requires the java-parser-cli component.
* Queue Manager - adds parsing jobs to the queue.

The worker operates as follows:
    1. Reads parsing jobs from a redis based python-rq job queue (http://python-rq.org).
    2. Clones the git repo specified
    3. Installs all dependencies etc. via Maven
    4. Parses the source code to build a dependency call graph for the project.
    5. Pushes the parsed source code to a neo4j database.
    6. Cleans up (removes code etc.)

The parsing of source code is handled by the [Java Parser CLI Tool](../java-parser/), which is invoked by the worker during operation.

## Development and Deployment

To build this project into a Docker container for deployment, run `./build.sh`.

To run this service for testing and development purposes, first run:

`cp credentials.sh.template credentials.sh` and then modify the specified environment variables in this script. 

Then run `source credentials.sh` to set these environment variables in your shell. To run the manager, run `python3 src/main.py`. To run the worker, run `python3 worker.py`. You will need to set up a Redis Database and a Neo4j Database and point to them in the credentials.sh file.



