# Java Parsing CLI Tool

This is a Java based tool interfacable through a Command Line Interface. It accepts a directory of source code to parse, a set of directories the containing Jar files of dependencies required by the source code to be parsed, and an identifier for the repository being parsed. It then traverses the Abstract Syntax Tree of the given source code, and constructs a Method Level Call Graph, which is outputted as a series of Neo4j Cypher Queries, or as a GraphViz .dot file.

To build this project, run `mvn package`. This will produce two important artifacts in the target directory:
    * `java_parser.jar-jar-with-dependencies`
    * `java_parser_cli.jar-jar-with-dependencies`

Running `java -jar java_parser.jar-jar-with-dependencies` will run the parser/Parser.java entrypoint to the program, and is useful for carrying out tests without having to specify CLI inputs, or for running the program from an IDE.

Running `java -jar java_parser_cli.jar-jar-with-dependencies` will run the parser/ParserCLI.java entrypoint to the program. This is the CLI tool, with the following options:

```
Usage: parse -i=<projectID> -l=<outputOpts> -o=<outputFile> -s=<sourceDir>
            -t=<parseType> [-j=<jarDirs>...]...
Parser parses a java project to build a dependency call graph,
exported as a graphviz DOT file.

Options:
-j, --jar_dir=<jarDirs>...

-s, --source_dir=<sourceDir>

-o, --output_file=<outputFile>

-l=<outputOpts>    Output file format: dot, cypher
-t=<parseType>     Parsing type: packageclassonly, all
-i, --project_id=<projectID>
```

The source_dir to be specified is the parent directory containing source code to be analysed. The jar_dir is a directory of jar files which are the required dependencies for the source code to be analysed (i.e. these are what Maven downloads when you run `maven build` for the first time). 