package com.jwu.javaparser.parser.visitors;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.jwu.javaparser.dependencygraph.DependencyGraph;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * MethodOverloadOverrideVisitor visits class and interface declarations, and then
 * infers any parents which they have to identify method overrides and overloads
 */
public class MethodOverloadOverrideVisitor extends VoidVisitorAdapter<Void> {
    DependencyGraph graph;

    public MethodOverloadOverrideVisitor(DependencyGraph graph) {
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

            System.out.println("Class identified by MethodOverloadOverrideVisitor " + ci.getNameAsString());

            this.graph.addClass(ci.resolve().getPackageName(), ci.resolve().getClassName());
            List<MethodDeclaration> currentClassMethods = ci.getMethods(); // store the methods of the current class

            NodeList<ClassOrInterfaceType> extendedTypes = ci.getExtendedTypes();
            extendedTypes.forEach(type -> {
                System.out.println("Found extended type: " + type.getName().asString());
                this.graph.addClass(type.resolve().getTypeDeclaration().getPackageName(), type.resolve().getTypeDeclaration().getClassName());

                for (ResolvedMethodDeclaration parentMethodDeclaration: type.resolve().getAllMethodsVisibleToInheritors()) {

                    Predicate<MethodDeclaration> filterByName = md -> {
                        String name = md.getNameAsString();
                        String parentName = parentMethodDeclaration.getName();

                        return name.equals(parentName);
                    };

                    currentClassMethods.stream().filter(filterByName).collect(Collectors.toList()).forEach(childMethod -> {
                        this.graph.addMethod(parentMethodDeclaration.getPackageName(), parentMethodDeclaration.getClassName(), parentMethodDeclaration.getName());
                        this.graph.addMethod(childMethod.resolve().getPackageName(), childMethod.resolve().getClassName(), childMethod.resolve().getName());

                        String qualifiedParentName = parentMethodDeclaration.getPackageName() + "." + parentMethodDeclaration.getClassName() + "." + parentMethodDeclaration.getName();
                        String qualifiedChildName = childMethod.resolve().getPackageName() + "." + childMethod.resolve().getClassName() + "." + childMethod.resolve().getName();

                        this.graph.addOverridesOverloadsEdge(qualifiedChildName, qualifiedParentName);
                    });
                }
            });
        } catch (Exception e) {
            System.out.println("Error occurred in MethodOverloadOverrideVisitor");
        }
    }
}