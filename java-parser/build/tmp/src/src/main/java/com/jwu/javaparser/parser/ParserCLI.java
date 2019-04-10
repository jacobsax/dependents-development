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

@CommandLine.Command(name = "parse", sortOptions = false,
        header = "Parser parses a java project.",
        //descriptionHeading = "@|bold %nDescription|@:%n",
        description = {
                "Parser parses a java project to build a dependency call graph, ",
                "exported as a graphviz DOT file.", },
        optionListHeading = "@|bold %nOptions|@:%n",
        footer = {""})
public class ParserCLI implements Runnable  {
    @Option(names = {"-j", "--jar_dir"}, arity = "1..*")
    Path[] jarDirs;

    @Option(names = {"-s", "--source_dir"}, required = true, arity = "1" )
    Path sourceDir;

    @Option(names = {"-o", "--output_file"}, required = true, arity = "1" )
    String outputFile;

    @Option(names = "-l", required = true, arity = "1", description = "Output file format: ${COMPLETION-CANDIDATES}")
    OutputOpts outputOpts = null;

    @Option(names = "-t", required = true, arity = "1", description = "Parsing type: ${COMPLETION-CANDIDATES}")
    ParseType parseType = null;

    @Option(names = {"-i", "--project_id"}, required = true, arity = "1" )
    String projectID;

    static DependencyGraph dependencyGraph;

    public static void main(String[] args) {
        CommandLine.run(new ParserCLI(), System.err, args);
    }

    public void run() {

        // create a new empty dependency graph
        dependencyGraph = new DependencyGraph();

        // instantiate a directory parser for the given project
        DirectoryParser directoryParser;

        if (parseType == ParseType.all) {
            directoryParser = new DirectoryParser(projectID, projectID, true);
        } else {
            directoryParser = new DirectoryParser(projectID, projectID, false);
        }

        try {
            // try and parse all jar files in the source directory given
            directoryParser.parseMethods(dependencyGraph, sourceDir, Arrays.asList(jarDirs));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (outputOpts == OutputOpts.dot) {
            // build an exported
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