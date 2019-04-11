package com.jwu.javaparser.parser;

import com.jwu.javaparser.dependencygraph.DependencyGraph;
import com.jwu.javaparser.dependencygraph.io.Neo4jCypherExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.GraphExporter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;


public class Parser { 
    // the directory of a project to be analysed
    static String FILE_PATH = "./contrib/test_files";

    // A collection of locations to look for jar files for type resolution
    static Collection<Path> JAR_PATHS = new ArrayList<Path>() {{
        add(Paths.get("/Users/jwu/.m2")); // standard maven jar store
        add(Paths.get(FILE_PATH)); // also look in the project being parsed
    }};

    // the name and ID of the project under analysis
    static String projectName = "sample";
    static String projectID = "sample";

    static DependencyGraph dependencyGraph;

    public static void main( String[] args ) throws IOException, ExportException {
        // create an empty dependency graph to store the results of the analysis
        dependencyGraph = new DependencyGraph();
        
        // Use a DirectoryParser to identify and analyse all source code in the directory specified 
        // in the FILE_PATH variable
        DirectoryParser directoryParser = new DirectoryParser(projectName, projectID, true);
        try {
            directoryParser.parseMethods(dependencyGraph, Paths.get(FILE_PATH), JAR_PATHS);
        } catch (Throwable t) {
            System.out.println(t);
            // do nothing
        }

        // export the produced dependency graph as a series of cypher queries
        Neo4jCypherExporter exporter = DependencyGraph.getNeo4jExporter();
        exporter.exportGraph(dependencyGraph, new FileWriter("output.cypher"));
    }
}
