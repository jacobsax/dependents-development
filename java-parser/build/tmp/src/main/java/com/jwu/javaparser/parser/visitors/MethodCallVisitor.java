package com.jwu.javaparser.parser.visitors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.jwu.javaparser.dependencygraph.DependencyGraph;

import java.util.HashMap;
import java.util.Optional;

/**
 * MethodCallVisitor identifies all method calls and adds them to the dependency call graph.
 */
public class MethodCallVisitor extends VoidVisitorAdapter<Void> {
    DependencyGraph graph;

    public MethodCallVisitor(DependencyGraph graph) {
        this.graph = graph;
    }

    /**
     * Identify parent node recurses up the AST tree to identify immediate
     * parent declarator of a node (i.e. the method, class or constructor it is contained in).
     * @param inputNode
     * @return
     */
    private Optional<Node> identifyParentNode(Node inputNode) {
        Optional<Node> optParentNode = inputNode.getParentNode();

        if (!optParentNode.isPresent()) {
            return Optional.empty();
        } else {
            Node parentNode = optParentNode.get();

            if (!(parentNode instanceof MethodDeclaration || parentNode instanceof ClassOrInterfaceDeclaration || parentNode instanceof ConstructorDeclaration)) {
                return identifyParentNode(parentNode);
            } else {
                return Optional.of(parentNode);
            }
        }
    }

    /**
     * @param mc
     * @param arg
     */
    @Override
    public void visit(MethodCallExpr mc, Void arg) {
        super.visit(mc, arg);

        try {
            System.out.println("visiting method name " + mc.getName());
            System.out.println(mc.getArguments());
            mc.getBegin().ifPresent(p -> {
                System.out.println(p.line);
            });

            ResolvedMethodDeclaration resolvedMethodDeclaration = mc.resolve(); //resolving takes a long time. So only do it once
            System.out.println("visiting method qualified" + resolvedMethodDeclaration.getQualifiedName());

            // Find the main parent node of the method call. This is method declaration, class, constructor etc. which
            // contains tne node.
            this.identifyParentNode(mc).ifPresent(node -> {
                System.out.println("Parent node of type identified: " + node.getClass().getName());

                // find the identified methods call name, class and package
                String methodCall = resolvedMethodDeclaration.getName();
                String methodCallClass = resolvedMethodDeclaration.getClassName();
                String methodCallPackage =  resolvedMethodDeclaration.getPackageName();

                // add the found method call to the graph
                graph.addMethod(methodCallPackage, methodCallClass, methodCall);

                // if the parent node is a method declaration, add the parent method to the graph also, and a connecting calls edge
                if (node instanceof  MethodDeclaration) {
                    MethodDeclaration parentMC = (MethodDeclaration) node;
                    ResolvedMethodDeclaration resolvedParentMethodDeclaration = parentMC.resolve();

                    System.out.println("Method qualified name: " + resolvedParentMethodDeclaration.getQualifiedName());

                    // the parent tree of the method call is part of the AST tree being parsed, therefore every component can be considered part of the tree
                    graph.addMethod(resolvedParentMethodDeclaration.getPackageName(), resolvedParentMethodDeclaration.getClassName(), resolvedParentMethodDeclaration.getName());

                    String qualifiedName = resolvedParentMethodDeclaration.getPackageName() + "." + resolvedParentMethodDeclaration.getClassName() + "." + resolvedParentMethodDeclaration.getName();
                    graph.addCallsEdge(qualifiedName, resolvedMethodDeclaration.getQualifiedName());

                // if the parent node is a class or interface declaration, add the parent to the graph, and then the calls edge
                } else if (node instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration parentCI = (ClassOrInterfaceDeclaration) node;
                    ResolvedReferenceTypeDeclaration resolvedParentMethodDeclaration = parentCI.resolve();

                    graph.addClass(resolvedParentMethodDeclaration.getPackageName(), resolvedParentMethodDeclaration.getClassName());

                    String qualifiedName = resolvedParentMethodDeclaration.getPackageName() + "." + resolvedParentMethodDeclaration.getClassName();
                    graph.addCallsEdge(qualifiedName, resolvedMethodDeclaration.getQualifiedName());

                    System.out.println("Class or interface qualified name: " + resolvedParentMethodDeclaration.getQualifiedName());

                // if the parent node is a constructor declaration, add the constructor and its parents (i.e. class, package) to the graph, and then the calls edge
                } else if (node instanceof ConstructorDeclaration) {
                    ConstructorDeclaration parentCD = (ConstructorDeclaration) node;
                    ResolvedConstructorDeclaration resolvedParentMethodDeclaration = parentCD.resolve();

                    graph.addMethod(resolvedParentMethodDeclaration.getPackageName(), resolvedParentMethodDeclaration.getClassName(), resolvedParentMethodDeclaration.getName());

                    String qualifiedName = resolvedParentMethodDeclaration.getPackageName() + "." + resolvedParentMethodDeclaration.getClassName() + "." + resolvedParentMethodDeclaration.getName();
                    graph.addCallsEdge(qualifiedName, resolvedMethodDeclaration.getQualifiedName());
                }
            });

            // sometimes the type solver can get stuck and cause a stack overflow
        } catch (Exception | StackOverflowError e) {
            System.out.println("Error occurred attempting to add calls edge");
            System.out.println(e.toString());
        }
    }
}