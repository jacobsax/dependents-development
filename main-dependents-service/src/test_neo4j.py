import os
import neo4j_queries
import unittest
from unittest.mock import Mock

# Used to mock out the Neo4j tx Class
class Neo4jTXMock:
    def run(match_string):
        pass

# Converts a dictionary to an object with attributes matching the dictionaries keys
class AttrDict(dict):
    def __init__(self, *args, **kwargs):
        super(AttrDict, self).__init__(*args, **kwargs)
        self.__dict__ = self

class Neo4jTestCase(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    '''
    Validate that dependents_from_node method of the neo4j_queries module
    correctly requests and formats data.
    '''
    def test_ast_dependents_from_node_retrieval(self):
        tx = Neo4jTXMock()
        tx.run = Mock()
        tx.run.return_value = [
            {"d": AttrDict({'_labels': ["test_label_0"], "_properties": {"id": "test_id_0"}, "v": 0})},
            {"d": AttrDict({'_labels': ["test_label_1"], "_properties": {"id": "test_id_1"}, "v": 1})}
            ]

        result = neo4j_queries.dependents_from_node(tx, "testgroup", "testproject", None, None)

        assert len(result) == 2
        assert result[0].get('label') == 'test_label_0'
        assert result[0].get('id') == 'test_id_0'
        assert result[0].get('properties') == {"id": "test_id_0"}

        assert result[1].get('label') == 'test_label_1'
        assert result[1].get('id') == 'test_id_1'
        assert result[1].get('properties') == {"id": "test_id_1"}

        tx.run.assert_called()

    '''
    Validate that dependents_from_node method of the neo4j_queries module
     does not fail if an vertex is missing an attribute
    '''
    def test_ast_dependents_from_node_faliure(self):
        tx = Neo4jTXMock()
        tx.run = Mock()
        tx.run.return_value = [
            {"d": AttrDict({'_labels': ["test_label_0"], "_properties": {}, "v": 0})},
            {"d": AttrDict({'_labels': ["test_label_1"], "_properties": {}, "v": 1})}
            ]

        result = neo4j_queries.dependents_from_node(tx, "testgroup", "testproject", None, None)

        assert len(result) == 2
        assert result[0].get('label') == 'test_label_0'
        assert result[0].get('id') == None
        assert result[0].get('properties') == {}

        assert result[1].get('label') == 'test_label_1'
        assert result[1].get('id') == None
        assert result[1].get('properties') == {}

        tx.run.assert_called()

    ''' 
    Validate that dependents_from_node method of the neo4j_queries module
    correctly returns an empty list when no information is available
    '''
    def test_ast_dependents_from_node_on_empty(self):
        tx = Neo4jTXMock()
        tx.run = Mock()
        tx.run.return_value = []

        result = neo4j_queries.dependents_from_node(tx, "testgroup", "testproject", None, None)
        assert result == []
        tx.run.assert_called()

        

if __name__ == '__main__':
    unittest.main()