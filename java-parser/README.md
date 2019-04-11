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

## Testing

Testing for this component can be carried out using a [test harness](./test/README.md), which can be found in the `./test/` directory. 

## Reasons for Failure

Sometimes, the tool is unable to resolve a method. This can occur when the method is of an external dependency, and isn't currently defined in any of those dependencies. For example, take the UML_parser repository used as an external test case for the harness. It fails to resolve the symbol `Modifier.getAccessSpecifier(...)`, for the following reason:

    UnsolvedSymbolException{context='null', name='We are unable to find the method declaration corresponding to Modifier.getAccessSpecifier(modifiers)', cause='null'}

What this error indicates is that the method which has been called has not been declared in any of the places JavaParser can look (i.e. Jar files, source code). On investigation, I discovered that the getAccessSpecified method had been removed from the external library which defines the Modifier class in [2018](https://github.com/javaparser/javaparser/commit/e2a4bc99a36893f538240b2b9cd90a4b0264e738#diff-77ce92ffe7fb59b25ebdaf59d801fbcb). This is therefore unresolvable.

