package com.jwu.javaparser.dependencygraph.nodes.types;

import java.io.Serializable;
import java.text.Normalizer;

public enum NodeType implements Serializable {
    PACKAGE("Package"),
    CLASS_OR_INTERFACE("ClassOrInterface"),
    METHOD("Method"),
    PROJECT("Project");

    private final String nodeName;

    NodeType(final String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public String toString() {
        String toReturn =  Normalizer.normalize(this.nodeName, Normalizer.Form.NFD);
        return toReturn.replaceAll("[^\\x00-\\x7F]", "");
    }
}