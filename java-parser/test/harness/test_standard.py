import unittest
import shared
import os

parent_dir = os.path.join(os.path.dirname(os.path.realpath(__file__)), os.pardir)
cli_tool_path = os.path.join(parent_dir, "tooling/cli.jar")
short_url = "testing/testing"
source_code_dir = os.path.join(parent_dir, "acceptance/standard/src/main")
jar_dir = source_code_dir
parsing_type = "all"
output_file_path = os.path.join(parent_dir, "tooling/test.cypher")
log_file_path = os.path.join(parent_dir, "tooling/test.log")

test_file = []

class StandardTestCase(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        shared.remove_test_file(output_file_path)
        shared.remove_test_file(log_file_path)
        shared.run_cli(cli_tool_path, short_url, source_code_dir, jar_dir, parsing_type, output_file_path, log_file_path)
    
    @classmethod
    def tearDownClass(cls):
        # shared.remove_test_file(output_file_path)
        # shared.remove_test_file(log_file_path)
        pass

    """
    Validate that the output contains the testing package
    """
    def test_contains_package(self):
        output = shared.read_test_file(output_file_path)

        assert "MERGE (p:Package {id: 'testing', name: 'testing'});" in output

    """
    Validate that the output contains the testing.ReversePolishNotation class
    """
    def test_contains_class(self):
        output = shared.read_test_file(output_file_path)

        assert "MERGE (p:ClassOrInterface {id: 'testing.ReversePolishNotation', name: 'ReversePolishNotation'});" in output


    """
    Validate that the output contains the correct method calls to the reverse polish notation classes
    """
    def test_identifies_basic_calls(self):
        output = shared.read_test_file(output_file_path)

        assert "MATCH (source:Method { id: 'testing.ReversePolishNotation.launchCalc'}),(target:Method { id: 'testing.ReversePolishNotation.calc'}) MERGE (source)-[:Calls]->(target);" in output
        assert "MATCH (source:Method { id: 'testing.App.main'}),(target:Method { id: 'testing.ReversePolishNotation.launchCalc'}) MERGE (source)-[:Calls]->(target);" in output
        assert "MATCH (source:Method { id: 'testing.App.main'}),(target:Method { id: 'java.io.PrintStream.println'}) MERGE (source)-[:Calls]->(target);" in output
        assert "MATCH (source:Method { id: 'testing.ReversePolishNotation.calc'}),(target:Method { id: 'java.lang.String.split'}) MERGE (source)-[:Calls]->(target);" in output

    """
    Validate that the output contains the correct method calls to the reverse polish notation classes
    """
    def test_identifies_calls_in_stream(self):
        output = shared.read_test_file(output_file_path)

        assert "MATCH (source:Method { id: 'testing.ReversePolishNotation.calc'}),(target:Method { id: 'java.util.Stack.pop'}) MERGE (source)-[:Calls]->(target);" in output
        assert "MATCH (source:Method { id: 'testing.ReversePolishNotation.calc'}),(target:Method { id: 'java.util.Stack.push'}) MERGE (source)-[:Calls]->(target);" in output
        assert "MATCH (source:Method { id: 'testing.ReversePolishNotation.calc'}),(target:Method { id: 'java.util.stream.Stream.forEach'}) MERGE (source)-[:Calls]->(target);" in output

    """
    Validate that static method calls are recognised
    """
    def test_static_method_calls(self):
        output = shared.read_test_file(output_file_path)

        assert "MATCH (source:Method { id: 'testing.App.main'}),(target:Method { id: 'testing.ReversePolishNotation.fetchZero'}) MERGE (source)-[:Calls]->(target);" in output

if __name__ == '__main__':
    unittest.main()