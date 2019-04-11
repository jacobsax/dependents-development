package com.jwu.javaparser.parser;

import com.jwu.javaparser.dependencygraph.DependencyGraph;
import com.jwu.javaparser.dependencygraph.io.Neo4jCypherExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.GraphExporter;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.Arrays;

enum OutputOpts {
    dot,
    cypher
        }

enum ParseType {
    packageclassonly,
    all
    }

/**
 * ParserCLI implements a CLI interface for analysing Java code to produce a call graph.
 * 
 * It can be invoked as follows:
 * 
 *      Usage: parse -i=<projectID> -l=<outputOpts> -o=<outputFile> -s=<sourceDir>
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
 * 
 * 
 */
@CommandLine.Command(name = "parse", sortOptions = false,
        header = "Parser parses a java project.",
        description = {
                "Parser parses a java project to build a dependency call graph, ",
                "exported as a graphviz DOT file.", },
        optionListHeading = "@|bold %nOptions|@:%n",
        footer = {""})
public class ParserCLI implements Runnable  {

    // A list of directories to search for jar files in. These jar files should be dependencies of the 
    // analysed source code.
    @Option(names = {"-j", "--jar_dir"}, arity = "1..*")
    Path[] jarDirs;

    // The source directory of the source code to be analysed
    @Option(names = {"-s", "--source_dir"}, required = true, arity = "1" )
    Path sourceDir;

    // The output file to save the generated call graph to.
    @Option(names = {"-o", "--output_file"}, required = true, arity = "1" )
    String outputFile;

    // The format the output file should take. This can either be a graphviz dot file, or a series of Neo4j
    // Cypher queries to reconstruct the graph in a Neo4j database.
    @Option(names = "-l", required = true, arity = "1", description = "Output file format: ${COMPLETION-CANDIDATES}")
    OutputOpts outputOpts = null;

    // The level of granularity to analyse the source code at. This can be 'all' to analyse method calls etc.
    // or 'packageclassonly' to only identify packages and classes in the source code
    @Option(names = "-t", required = true, arity = "1", description = "Parsing type: ${COMPLETION-CANDIDATES}")
    ParseType parseType = null;

    // The id of the project. An additional root vertices will be added to the produced graph with this id
    @Option(names = {"-i", "--project_id"}, required = true, arity = "1" )
    String projectID;

    static DependencyGraph dependencyGraph;

    public static void main(String[] args) {
        CommandLine.run(new ParserCLI(), System.err, args);
    }

    public void run() {

        // create a new empty dependency graph to store the results of analysis
        dependencyGraph = new DependencyGraph();

        // instantiate a directory parser for the given project, parsing at the granularity specified by the user
        DirectoryParser directoryParser = new DirectoryParser(projectID, projectID, (parseType == ParseType.all));

        try {
            // try and parse all jar files in the source directory given
            directoryParser.parseMethods(dependencyGraph, sourceDir, Arrays.asList(jarDirs));
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
         * The user can opt to export the graph as a graphviz dot file, or as a Neo4j file.
         * 
         * The export has to be handled differently for each type.
         */
        if (outputOpts == OutputOpts.dot) {
            // build an exporter to export the graph in dot format
            GraphExporter exporter = DependencyGraph.getDOTExporter();

            try {
                // try and export the graph
                DependencyGraph.saveGraphToFile(exporter, dependencyGraph, outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ExportException e) {
                e.printStackTrace();
            }
        } else if (outputOpts == OutputOpts.cypher) {
            Neo4jCypherExporter exporter = DependencyGraph.getNeo4jExporter();

            try {
                exporter.exportGraph(dependencyGraph, new FileWriter(outputFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}