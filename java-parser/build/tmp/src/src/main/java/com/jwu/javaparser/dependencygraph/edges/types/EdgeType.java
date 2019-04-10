package com.jwu.javaparser.dependencygraph.edges.types;

import java.io.Serializable;

public enum EdgeType implements Serializable {
    CONTAINS("Contains"),
    CALLS("Calls"),
    EXTENDED_BY("ExtendedBy"),
    OVERRIDDEN_OR_OVERLOADED_BY("OverriddenOrOverloadedBy");

    private final String edgeName;

    EdgeType(final String edgeName) {
        this.edgeName = edgeName;
    }

    @Override
    public String toString() {
        return edgeName;
    }
}
