
# Java Parsing Queue Manager/Worker

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
