
def add_project_node(tx, github_short_url):
    tx.run("MERGE (p:Project {{id: '{}'}});".format(github_short_url))


def add_project_depends_project_edge(tx, github_short_url, parent_github_short_url):
    tx.run("MATCH (d:Project {{id: '{}'}}), (p:Project {{id: '{}'}}) MERGE (d)-[:Depends]->(p);".format(
        github_short_url, parent_github_short_url))


def add_artifact_node(tx, groupId, artifactId):
    tx.run("MERGE (p:Artifact {{id: '{}.{}', group: '{}', artifact: '{}'}});".format(
        groupId, artifactId, groupId, artifactId))


def add_project_artifact_edge(tx, github_short_url, groupId, artifactId):
    tx.run("MATCH (p:Project {{id: '{}'}}), (a:Artifact {{id: '{}.{}'}}) MERGE (p)-[:Contains]->(a);".format(
        github_short_url, groupId, artifactId))


def add_artifact_depends_edge(tx, groupId, artifactId, dependencyGroupId, dependencyArtifactId):
    tx.run("MATCH (a:Artifact {{id: '{}.{}'}}),(d:Artifact {{id: '{}.{}'}}) MERGE (a)-[:Depends]->(d);".format(
        groupId, artifactId, dependencyGroupId, dependencyArtifactId))


def add_attribute_to_project(tx, github_short_url, attribute_name, attribute_value):
    tx.run("MERGE (a:ProjectAttribute {{id: '{}.{}', value: '{}'}});".format(
        github_short_url, attribute_name, attribute_value))
    tx.run("MATCH (a:ProjectAttribute {{id: '{}.{}', value: '{}'}}),(p:Project {{id: '{}'}}) MERGE (a)-[:Attribute]-(p);".format(
        github_short_url, attribute_name, attribute_value, github_short_url))


def update_attribute_of_project(tx, github_short_url, attribute_name, attribute_value):
    tx.run("MATCH (a:ProjectAttribute {{id: '{}.{}'}}) SET a.value = '{}';".format(
        github_short_url, attribute_name, attribute_value))


def add_attribute_to_artifact(tx, groupId, artifactId, attribute_name, attribute_value):
    tx.run("MERGE (a:ArtifactAttribute {{id: '{}.{}.{}', name: '{}', value: '{}'}});".format(
        groupId, artifactId, attribute_name, attribute_name, attribute_value))
    tx.run("MATCH (a:ArtifactAttribute {{id: '{}.{}.{}', name: '{}', value: '{}'}}),(p:Artifact {{id: '{}.{}'}}) MERGE (a)-[:Attribute]-(p);".format(
        groupId, artifactId, attribute_name, attribute_name, attribute_name, attribute_value, groupId, artifactId))


def update_attribute_of_artifact(tx, groupId, artifactId, attribute_name, attribute_value):
    tx.run("MATCH (a:ArtifactAttribute {{id: '{}.{}.{}'}}) SET a.value = '{}';".format(
        groupId, artifactId, attribute_name, attribute_value))


def is_package_parsed(tx, groupId, artifactId):
    result = tx.run("MATCH (a:ArtifactAttribute {{id: '{}.{}.dependentsearch', name: 'dependentsearch'}}) RETURN a.value".format(
        groupId, artifactId))
    if (result == None):
        return False

    single = result.single()
    if (single == None):
        return False

    return single[0]
