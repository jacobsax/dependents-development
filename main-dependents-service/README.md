# Retrieval of Data for Dependency Call Graph Visualisation and Management Service

This service provides an abstraction layer between front end services and the Neo4j database, so that they do not need to be aware of the storage method used by the produced system. It is responsible for returning data produced from analysis to be rendered in the front end as tables and visualisations, and also forwarding requests to the relevant services to initiate analysis of a repository.

This service is interfaceable via a REST API, and returns information in a JSON format. This service is stateless and can be horizontally scaled to help maintain system performance under load.

## Building and Running

To specify the environment to for this service to run in, run `cp ./credentials.sh.template ./credentials.sh` and populate the required environment variables.

To run this service (i.e. for testing), first run `./credentials.sh` to set up appropriate environment variables, and then run `python3 ./src/main.py`. To build this service as a docker container for production deployment, run `./build.sh`.

To configure the service to point at your desired deployment, modify the `credentials.sh` file.

## Testing

A number of unit tests have been written for this service, to validate that it correctly handles the transformation of data from Neo4j to JSON. To run these unit tests, cd into the src directory, and run `python3 -m unittest discover`.

## Technology Choices

This service is written in Python3, utilising the Python Flask library to handle HTTP requests, and the neo4j client library to handle communications with neo4j. To deploy the application, the `tiangolo/uwsgi-nginx-flask:python3.7` docker image is used.

## API Documentation

The API for this service is defined in a [postman collection](./pom-search-service.postman_collection.json).
