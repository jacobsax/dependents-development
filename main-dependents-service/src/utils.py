import os
from redis import Redis
from neo4j import GraphDatabase

def get_neo4j():
    neo4j_ip = os.environ.get('NEO4J_IP') 
    if (neo4j_ip == None):
        neo4j_ip = "localhost"
    neo4j_ip = "bolt://" + neo4j_ip + ":7687"

    neo4j_user = os.environ.get('NEO4J_USER') 
    if (neo4j_user == None):
        raise ValueError('NEO4J_USER environment variable must be set.')

    neo4j_password = os.environ.get('NEO4J_PASS') 
    if (neo4j_password == None):
        raise ValueError('NEO4J_PASS environment variable must be set.')

    return GraphDatabase.driver(neo4j_ip, auth=(neo4j_user, neo4j_password))

def get_depends_service():
    return os.environ.get("DEPENDS_SERVICE_URL")


def get_queue_manager():
    return os.environ.get("QUEUE_MANAGER")

def get_ast_queue_manager():
    return os.environ.get("AST_QUEUE_MANAGER")

def get_domain():
    return os.environ.get("DOMAIN")
