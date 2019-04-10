package com.jwu.javaparser.analyser;

import com.jwu.javaparser.dependencygraph.DependencyGraph;
import com.jwu.javaparser.dependencygraph.edges.DependencyEdge;
import com.jwu.javaparser.dependencygraph.nodes.BaseNode;
import com.jwu.javaparser.dependencygraph.nodes.DependencyNode;
import com.jwu.javaparser.dependencygraph.nodes.ProjectNode;
import com.jwu.javaparser.dependencygraph.nodes.types.NodeType;
import org.jgrapht.Graphs;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.GraphExporter;
import org.jgrapht.io.GraphImporter;
import org.jgrapht.io.ImportException;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class Analyser {

    static void analyseFromProject() throws FileNotFoundException, ImportException {
        // Prints "Hello, World" to the terminal window.
        System.out.println("Hello, World");

        GraphImporter importer = DependencyGraph.getDOTImporter();
        DependencyGraph graph = DependencyGraph.readGraphFromFile(importer, "fastjson-package-only.dot");
        DependencyGraph graph2 = DependencyGraph.readGraphFromFile(importer, "spark-demo-beginner.dot");

        Graphs.addGraph(graph, graph2);

//        GraphIterator<BaseNode, DependencyEdge> iterator = new DepthFirstIterator(graph, new ProjectNode(projectName, projectID));
//        while (iterator.hasNext()) {
//            if (iterator.next() instanceof DependencyNode) {
//                DependencyNode next = (DependencyNode) iterator.next();
//                System.out.print("Found: " + next.type);
//                System.out.println(" with name: " + next.name);
//            }
////            System.out.println( iterator.next() );
//        }

        System.out.println("Imported graph");

        DependencyGraphSearcher graphSearcher = new DependencyGraphSearcher(graph, projectName, projectID);
        List<DependencyNode> projectPackages = graphSearcher.getPackagesInProject(); // retrieve a list of all packages in the project
//        System.out.println("Packages in project: " + projectPackages);

//        // finds all dependent calls (transitive or otherwise)
//        for (DependencyNode projectPackage: projectPackages) {
//            System.out.println("Package " + projectPackage.toString());
//
//            for (DependencyNode packageClassOrInterface : graphSearcher.getClassesAndInterfacesInPackage(projectPackage)) {
//                System.out.println("    Has Class/Interface " + packageClassOrInterface.toString());
//
//                for (DependencyNode methodInClassOrInterface: graphSearcher.getMethodsInClassOrInterface(packageClassOrInterface)) {
//                    System.out.println("        Has Method " + methodInClassOrInterface.toString());
//
//                    for (DependencyNode callingMethod: graphSearcher.getCallsOfMethod(methodInClassOrInterface)) {
//                        System.out.println("            Is called by " + callingMethod.toString());
//                        graphSearcher.getProjectOfNode(callingMethod).ifPresent(p -> System.out.println("                Of project " + p.name));
//
//                        for (DependencyNode transitiveCallingMethod: graphSearcher.getAllCallsOfMethod(callingMethod)) {
//                            System.out.println("            Is transitively called by " + transitiveCallingMethod.toString());
//                            graphSearcher.getProjectOfNode(transitiveCallingMethod).ifPresent(p -> System.out.println("                Of project " + p.name));
//                        }
//                    }
//                }
//            }
//        }

        // finds only external calls (transitive or otherwise)
        for (DependencyNode projectPackage: projectPackages) {
            System.out.println("Package " + projectPackage.toString());

            for (DependencyNode packageClassOrInterface : graphSearcher.getClassesAndInterfacesInPackage(projectPackage)) {
                System.out.println("    Has Class/Interface " + packageClassOrInterface.toString());

                for (DependencyNode methodInClassOrInterface: graphSearcher.getMethodsInClassOrInterface(packageClassOrInterface)) {
                    System.out.println("        Has Method " + methodInClassOrInterface.toString());

                    for (DependencyNode callingMethod: graphSearcher.getCallsOfMethod(methodInClassOrInterface)) {
                        graphSearcher.getProjectOfNode(callingMethod).ifPresent(p -> {
                            if (!p.id.equals(projectID)) {
                                System.out.println("            Is called by " + callingMethod.toString());
                                System.out.println("                Of project " + p.name);

                                for (DependencyNode transitiveCallingMethod: graphSearcher.getAllCallsOfMethod(callingMethod)) {
                                    System.out.println("            Is transitively called by " + transitiveCallingMethod.toString());
                                    graphSearcher.getProjectOfNode(transitiveCallingMethod).ifPresent(tp -> System.out.println("                Of project " + tp.name));
                                }
                            }
                        });
                    }
                }
            }
        }
    }

//    static void analyseFromDependent(String depentendName, String dependentId, String projectName, String projectID) throws FileNotFoundException, ImportException {
//        // Prints "Hello, World" to the terminal window.
//        System.out.println("Hello, World");
//
//        GraphImporter importer = DependencyGraph.getDOTImporter();
//        DependencyGraph graph = DependencyGraph.readGraphFromFile(importer, "fastjson2.dot");
//        DependencyGraph graph2 = DependencyGraph.readGraphFromFile(importer, "spark-demo-beginner.dot");
//
//        Graphs.addGraph(graph, graph2);
//
////        GraphIterator<BaseNode, DependencyEdge> iterator = new DepthFirstIterator(graph, new ProjectNode(projectName, projectID));
////        while (iterator.hasNext()) {
////            if (iterator.next() instanceof DependencyNode) {
////                DependencyNode next = (DependencyNode) iterator.next();
////                System.out.print("Found: " + next.type);
////                System.out.println(" with name: " + next.name);
////            }
//////            System.out.println( iterator.next() );
////        }
//
//        System.out.println("Imported graph");
//
//        DependencyGraphSearcher graphSearcher = new DependencyGraphSearcher(graph, projectName, projectID);
//
//    }


//    static String projectName = "sparkdemobeginner";
//    static String projectID = "sparkdemobeginner";

    static String projectName = "fastjson";
    static String projectID = "alibabafastjson";

    public static void main(String[] args) throws IOException, ImportException, ExportException {

        analyseFromProject();

//        analyseFromDependent("sparkdemobeginner", "sparkdemobeginner", projectName, projectID);



        // TODO: List method overrides in project
        // TODO: List class extensions/inheritance
    }
}
