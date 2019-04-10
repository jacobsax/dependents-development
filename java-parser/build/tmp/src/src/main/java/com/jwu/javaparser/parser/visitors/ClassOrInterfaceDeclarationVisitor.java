package com.jwu.javaparser.parser.visitors;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.jwu.javaparser.dependencygraph.DependencyGraph;

/**
 * ClassOrInterfaceDeclarationVisitor visits class, interface and enum declarations to
 * identify any class inheritance or interface implementations, which are then added to the
 * dependency call graph.
 */
public class ClassOrInterfaceDeclarationVisitor extends VoidVisitorAdapter<Void> {
    DependencyGraph graph;

    public ClassOrInterfaceDeclarationVisitor(DependencyGraph graph) {
        this.graph = graph;
    }


    /**
     * @param ci
     * @param arg
     */
    @Override
    public void visit(ClassOrInterfaceDeclaration ci, Void arg) {

        try {
            super.visit(ci, arg);

            System.out.println("Class identified by ClassOrInterfaceDeclarationVisitor " + ci.getNameAsString());

            this.graph.addClass(ci.resolve().getPackageName(), ci.resolve().getClassName());
            String qualifiedName = ci.resolve().getPackageName() + "." + ci.resolve().getClassName();

            NodeList<ClassOrInterfaceType> extendedTypes = ci.getExtendedTypes();
            extendedTypes.forEach(type -> {
                System.out.println("Found extended type: " + type.getName().asString());
                this.graph.addClass(type.resolve().getTypeDeclaration().getPackageName(), type.resolve().getTypeDeclaration().getClassName());

                String parentQualifiedName = type.resolve().getTypeDeclaration().getPackageName() + "." + type.resolve().getTypeDeclaration().getClassName();
                this.graph.addExtendsEdge(parentQualifiedName, qualifiedName);
            });
        } catch (Exception e) {
            System.out.println("Error occurred in ClassOrInterfaceDeclarationVisitor");
        }
    }
}