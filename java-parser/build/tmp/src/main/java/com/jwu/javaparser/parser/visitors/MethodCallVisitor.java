package com.jwu.javaparser.parser.visitors;

import com.github.javaparser.ast.CompilationUnit;
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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

/**
 * MethodCallVisitor identifies all method calls and adds them to the dependency call graph.
 */
public class MethodCallVisitor extends VoidVisitorAdapter<Void> {
    DependencyGraph graph;
    String projectID;
    String projectName;

    public MethodCallVisitor(DependencyGraph graph, String projectID, String projectName) {
        this.graph = graph;
        this.projectID = projectID;
        this.projectName = projectName;
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
        super.visit(mc, null);

        System.out.println("===================");

        try {
            System.out.println("Method called: " + mc.getName());

            ResolvedMethodDeclaration resolvedMethodDeclaration = mc.resolve(); //resolving takes a long time. So only do it once
            System.out.println("Method called qualified name: " + resolvedMethodDeclaration.getQualifiedName());

            // Find the main parent node of the method call. This is method declaration, class, constructor etc. which
            // calls the found method
            this.identifyParentNode(mc).ifPresent(node -> {
                // find the identified methods call name, class and package
                String methodCall = resolvedMethodDeclaration.getName();
                String methodCallClass = resolvedMethodDeclaration.getClassName();
                String methodCallPackage =  resolvedMethodDeclaration.getPackageName();

                if (methodCallPackage == "") {
                    methodCallPackage = methodCallClass;
                }

                // construct a qualified name for the method
                String methodCallQualifiedName = methodCallPackage + "." + methodCallClass + "." + methodCall;

                // retrieve the file path which the currently being traversed AST is generated from
                Path filePath = mc.findCompilationUnit().get().getStorage().get().getPath();

                // add the found method call to the graph
                graph.addMethod(methodCallPackage, methodCallClass, methodCall);

                // if the parent node is a method declaration, add the parent method to the graph also, and a connecting calls edge
                if (node instanceof  MethodDeclaration) {
                    MethodDeclaration parentMC = (MethodDeclaration) node;
                    ResolvedMethodDeclaration resolvedParentMethodDeclaration = parentMC.resolve();

                    System.out.println("Method called by parent method with qualified name: " + resolvedParentMethodDeclaration.getQualifiedName());
                    System.out.println("Package name: " + resolvedParentMethodDeclaration.getPackageName());

                    String packageName = resolvedParentMethodDeclaration.getPackageName();
                    String className = resolvedParentMethodDeclaration.getClassName();
                    String methodName = resolvedParentMethodDeclaration.getName();

                    // if no package has been defined, specify the package name the same as the project id
                    // this ensures that the package remains unique, even if lots of projects just have a 'Main' class
                    if (packageName == "") {
                        packageName = this.projectID;
                        // as we have just 'invented' a package, we need to claim it for the project
                        this.graph.claimPackageForProject(this.projectName, this.projectID, packageName);
                    }

                    // construct the qualified name of the calling method
                    String qualifiedName = packageName + "." + className + "." + methodName;

                    // the parent tree of the method call is part of the AST tree being parsed, therefore every component can be considered part of the tree
                    graph.addMethod(packageName, className, methodName);                    

                    graph.addCallsEdge(qualifiedName, methodCallQualifiedName);

                // if the parent node is a class or interface declaration, add the parent to the graph, and then the calls edge
                } else if (node instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration parentCI = (ClassOrInterfaceDeclaration) node;
                    ResolvedReferenceTypeDeclaration resolvedParentMethodDeclaration = parentCI.resolve();

                    String packageName = resolvedParentMethodDeclaration.getPackageName();
                    String className = resolvedParentMethodDeclaration.getClassName();

                    // if no package has been defined, specify the package name the same as the project id
                    if (packageName == "") {
                        packageName = this.projectID;
                        // as we have just 'invented' a package, we need to claim it for the project
                        this.graph.claimPackageForProject(this.projectName, this.projectID, packageName);
                    }

                    // construct the qualified name of the calling class
                    String qualifiedName = packageName + "." + className;

                    graph.addClass(packageName, className);

                    graph.addCallsEdge(qualifiedName, methodCallQualifiedName);

                    System.out.println("Method called by class or interface with qualified name: " + resolvedParentMethodDeclaration.getQualifiedName());

                // if the parent node is a constructor declaration, add the constructor and its parents (i.e. class, package) to the graph, and then the calls edge
                } else if (node instanceof ConstructorDeclaration) {
                    ConstructorDeclaration parentCD = (ConstructorDeclaration) node;
                    ResolvedConstructorDeclaration resolvedParentMethodDeclaration = parentCD.resolve();

                    String packageName = resolvedParentMethodDeclaration.getPackageName();
                    String className = resolvedParentMethodDeclaration.getClassName();
                    String methodName = resolvedParentMethodDeclaration.getName();

                    // if no package has been defined, specify the package name the same as the project id
                    if (packageName == "") {
                        packageName = this.projectID;

                        // as we have just 'invented' a package, we need to claim it for the project
                        this.graph.claimPackageForProject(this.projectName, this.projectID, packageName);
                    }

                    // construct the qualified name of the calling method
                    String qualifiedName = packageName + "." + className + "." + methodName;

                    graph.addMethod(packageName, className, methodName);

                    graph.addCallsEdge(qualifiedName, methodCallQualifiedName);
                }
            });

            // sometimes the type solver can get stuck and cause a stack overflow
        } catch (Exception | StackOverflowError e) {
            System.out.println("Error occurred attempting to add calls edge");
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
}