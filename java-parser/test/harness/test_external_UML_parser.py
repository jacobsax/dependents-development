import unittest
import shared
import os

parent_dir = os.path.join(os.path.dirname(os.path.realpath(__file__)), os.pardir)
cli_tool_path = os.path.join(parent_dir, "tooling/cli.jar")
short_url = "testing/testing"
source_code_dir = os.path.join(parent_dir, "acceptance/external/UML_parser/src")
jar_dir = os.path.join(parent_dir, "acceptance/external/UML_parser/target/dependency")
parsing_type = "all"
output_file_path = os.path.join(parent_dir, "tooling/test_external_UML_Parser.cypher")
log_file_path = os.path.join(parent_dir, "tooling/test_external_UML_Parser.log")

test_file = []

"""
CLI testing using the external UML_parser project. This is an interesting project as it has no package defined,
only a single Main class. Therefore, a fake package node will be generated, with the same name as the project ID,
to properly namespace child classes/methods etc. 
"""
class UMLParserExternalTestCase(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        shared.remove_test_file(output_file_path)
        shared.remove_test_file(log_file_path)
        shared.run_cli(cli_tool_path, short_url, source_code_dir, jar_dir, parsing_type, output_file_path, log_file_path)
    
    @classmethod
    def tearDownClass(cls):
        # shared.remove_test_file(output_file_path)
        pass

    """
    Validate external static method calls
    """
    def test_static_method_calls(self):
        output = shared.read_test_file(output_file_path)
        
        assert "MATCH (source:Method { id: 'testing/testing.Main.getFiles'}),(target:Method { id: 'com.github.javaparser.JavaParser.parse'}) MERGE (source)-[:Calls]->(target);" in output

    """
    Validate that the project contains the package
    """
    def test_project_contains_package(self):
        output = shared.read_test_file(output_file_path)
        
        assert "MATCH (source:Project { id: 'testing/testing'}),(target:Package { id: 'testing/testing'}) MERGE (source)-[:Contains]->(target);" in output




if __name__ == '__main__':
    unittest.main()