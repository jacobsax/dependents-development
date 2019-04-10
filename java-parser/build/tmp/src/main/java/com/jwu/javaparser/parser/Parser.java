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
//    static String FILE_PATH = "/Users/jwu/3rd-year-project/Development/testing/java/JavaParser/contrib/test_files/";
//    static String FILE_PATH = "/Users/jwu/3rd-year-project/Development/testing/java/JavaParser/contrib/test_projects/appengine-endpoints-tictactoe-java";
//    static String FILE_PATH = "/Users/jwu/3rd-year-project/Development/testing/java/JavaParser/contrib/test_projects/ChessOOP";
//    static String FILE_PATH = "/Users/jwu/3rd-year-project/Development/testing/java/JavaParser/contrib/test_projects/jgrapht";
//    static String FILE_PATH = "/Users/jwu/3rd-year-project/Development/testing/java/JavaParser/src";
    static String FILE_PATH = "/Users/jwu/3rd-year-project/Development/testing/java/JavaParser/contrib/test-manual-dependents/repos/javaparser";
//    static String FILE_PATH = "/Users/jwu/3rd-year-project/Development/testing/java/JavaParser/contrib/test-manual-dependents/repos/fastjson";
//    static String FILE_PATH = "/Users/jwu/3rd-year-project/Development/testing/java/JavaParser/contrib/test-manual-dependents/repos/spark-demo-beginner";

//    static String FILE_PATH = "/Users/jwu/3rd-year-project/Development/testing/java/JavaParser/contrib/test-manual-dependents/repos/testPay" +

    // A collection of locations to look for jar files for type resolution
    static Collection<Path> JAR_PATHS = new ArrayList<Path>() {{
        add(Paths.get("/Users/jwu/.m2")); // standard maven jar store
        add(Paths.get(FILE_PATH)); // also look in the project being parsed
    }};

//    static String projectName = "fastjson";
//    static String projectID = "alibabafastjson";

    static String projectName = "javaparser";
    static String projectID = "comjavaparser";

    static DependencyGraph dependencyGraph;

    public static void main( String[] args ) throws IOException, ExportException {
        dependencyGraph = new DependencyGraph();
        DirectoryParser directoryParser = new DirectoryParser(projectName, projectID, true);

        try {
            directoryParser.parseMethods(dependencyGraph, Paths.get(FILE_PATH), JAR_PATHS);
        } catch (Throwable t) {
            System.out.println(t);
            // do nothing
        }

        Neo4jCypherExporter exporter = DependencyGraph.getNeo4jExporter();
        exporter.exportGraph(dependencyGraph, new FileWriter("javaparser-2.cypher"));
//        DependencyGraph.saveGraphToFile(exporter, dependencyGraph, "spark.csv");
    }
}
