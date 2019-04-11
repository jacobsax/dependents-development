# Testing

To test that CLI parser is functioning correctly, I have constructed a test harness, which automates the process of running the CLI tool against different projects, and validates the results. 

The projects used for testing purposes are stored in the `./acceptance/` directory. The tests themselves are written as python unit tests, and can be found in the `./harness/` directory. Output from the tests include logs produced by the CLI tool, and produced cypher queries. These all end up in the `./tooling` directory (this will be automatically created if not there).

To run the tests, run `./run.sh build`. This builds the CLI tool and then runs the tests. If you don't want to build the cli tool, just use `./run.sh`.