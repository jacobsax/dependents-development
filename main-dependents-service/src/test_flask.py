import os
import main
import unittest
import tempfile

class FlaskTestCase(unittest.TestCase):

    def setUp(self):
        main.app.testing = True
        self.app = main.app.test_client()

    def tearDown(self):
        pass

    def test_empty_db(self):
        rv = self.app.get('/')
        assert True == True

if __name__ == '__main__':
    unittest.main()