package com.jwu.javaparser.parser.visitors;


import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.jwu.javaparser.dependencygraph.DependencyGraph;

/**
 * PackageDeclarationVisitor determines which packages are part of the parsed project, and claims
 * the packages for the project by attaching them to a project 'root' node on the dependency graph
 */
public class PackageDeclarationVisitor extends VoidVisitorAdapter<Void> {
    DependencyGraph graph;
    String projectID;
    String projectName;

    public PackageDeclarationVisitor(DependencyGraph graph, String projectName, String projectID) {
        this.graph = graph;
        this.projectID = projectID;
        this.projectName = projectName;
    }

    /**
     * @param d
     * @param arg
     */
    @Override
    public void visit(PackageDeclaration d, Void arg) {

        System.out.println("Package found: " + d.getName());
        this.graph.claimPackageForProject(this.projectName, this.projectID, d.getNameAsString());
    }
}
