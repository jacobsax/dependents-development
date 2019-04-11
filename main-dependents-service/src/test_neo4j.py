import os
import neo4j_queries
import unittest
from unittest.mock import Mock

# Converts a dictionary to a class with attributes, whilst maintaining the keys
# from https://stackoverflow.com/questions/4984647/accessing-dict-keys-like-an-attribute/29548234
class AttrDict(dict):
    def __init__(self, *args, **kwargs):
        super(AttrDict, self).__init__(*args, **kwargs)
        self.__dict__ = self

# Used to mock out the Neo4j tx Class
class Neo4jTXMock:
    def run(match_string):
        pass

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
    
    """
    Validate that when createTreeFromEdges is given no edges or vertices,
    it returns an empty list.
    """
    def test_create_tree_from_edges_returns_list_on_empty(self):
        edges = []
        vertices = {}

        result = neo4j_queries.createTreeFromEdges(edges, vertices)
        assert result == []    

    """
    Validate that when createTreeFromEdges returns a graph given a set of edges and vertices
    """
    def test_create_tree_from_edges_returns_list_on_empty(self):
        edges = [
            ("vertex1", "vertex2"),
            ("vertex1", "vertex3")
        ]

        vertices = {
            "vertex1": 
                AttrDict({'_properties': {
                    "id": "vertex1"
                },
                "_labels": ["label1"]})
            ,
            "vertex2": 
                AttrDict({'_properties': {
                    "id": "vertex1"
                },
                "_labels": ["label1"]})
            ,
            "vertex3": 
                AttrDict(
                    { '_properties': {
                        "id": "vertex1"
                    },
                    "_labels": ["label1"]
                    })
        }


        result = neo4j_queries.createTreeFromEdges(edges, vertices)
        
        """
        A graph structure should now exist in result:

                vertex1
                   |
                  / \
            vertex2  vertex3
        """

        # A single root node should remain
        assert len(result) == 1   
        assert result[0].get("id") == "vertex1"   

        # The root node should have two children    
        assert len(result[0].get("children")) == 2

        # Assert that the two children are vertex2 and vertex3
        assert (v in [ k.get("id") for k in result[0].get("children")] for v in ["vertex2", "vertex3"])  

if __name__ == '__main__':
    unittest.main()