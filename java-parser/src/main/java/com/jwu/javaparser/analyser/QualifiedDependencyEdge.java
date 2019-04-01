package com.jwu.javaparser.analyser;

import com.jwu.javaparser.dependencygraph.edges.DependencyEdge;
import com.jwu.javaparser.dependencygraph.nodes.BaseNode;
import com.jwu.javaparser.dependencygraph.nodes.DependencyNode;
import com.jwu.javaparser.dependencygraph.nodes.ProjectNode;

import java.util.Optional;

public class QualifiedDependencyEdge extends DependencyEdge {
    BaseNode parentNode;
    BaseNode childNode;

    QualifiedDependencyEdge(BaseNode parentNode, BaseNode childNode, DependencyEdge dependencyEdge) {
        super(dependencyEdge.type);

        this.parentNode = parentNode;
        this.childNode = childNode;
    }

    public BaseNode getParentNode() {
        return parentNode;
    }

    public Optional<DependencyNode> getParentDependencyNode() {
        if (this.parentNode instanceof DependencyNode) {
            return Optional.of((DependencyNode) this.parentNode);
        }

        return Optional.empty();
    }

    public Optional<ProjectNode> getParentProjectNode() {
        if (this.parentNode instanceof ProjectNode) {
            return Optional.of((ProjectNode) this.parentNode);
        }

        return Optional.empty();
    }

    public BaseNode getChildNode() {
        return childNode;
    }

    public Optional<DependencyNode> getChildDependencyNode() {
        if (this.childNode instanceof DependencyNode) {
            return Optional.of((DependencyNode) this.childNode);
        }

        return Optional.empty();
    }

    public Optional<ProjectNode> getChildProjectNode() {
        if (this.childNode instanceof ProjectNode) {
            return Optional.of((ProjectNode) this.childNode);
        }

        return Optional.empty();
    }

}
