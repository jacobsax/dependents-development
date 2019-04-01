package com.jwu.javaparser.dependencygraph.edges;

import com.jwu.javaparser.dependencygraph.nodes.DependencyNode;
import com.jwu.javaparser.dependencygraph.edges.types.EdgeType;

import java.io.Serializable;

public class DependencyEdge implements Serializable {

    public final EdgeType type;

    public DependencyEdge(EdgeType type) {
        this.type = type;
    }

    public String toString() {
        return this.type.toString();
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(Object o) {
        return (o instanceof DependencyNode) && (toString().equals(o.toString()));
    }
}
