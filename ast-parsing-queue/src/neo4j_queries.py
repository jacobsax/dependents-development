def add_attribute_to_project(tx, github_short_url, attribute_name, attribute_value):
        tx.run("MERGE (a:ProjectAttribute {{id: '{}.{}', name: '{}', value: '{}'}});".format(github_short_url, attribute_name, attribute_name, attribute_value))
        tx.run("MATCH (a:ProjectAttribute {{id: '{}.{}'}}),(p:Project {{id: '{}'}}) MERGE (a)-[:Attribute]-(p);".format(github_short_url, attribute_name, github_short_url))

def update_attribute_of_project(tx, github_short_url, attribute_name, attribute_value):
        tx.run("MATCH (a:ProjectAttribute {{id: '{}.{}'}}) SET a.value = '{}';".format(github_short_url, attribute_name, attribute_value))


def retrieve_attribute_value(tx, github_short_url, attribute_name):
        result = tx.run("MATCH (a:ProjectAttribute {{id: '{}.{}'}}) RETURN a.value AS value;".format(github_short_url, attribute_name))

        if (result == None):
            return None

        single = result.single()
        return single
        