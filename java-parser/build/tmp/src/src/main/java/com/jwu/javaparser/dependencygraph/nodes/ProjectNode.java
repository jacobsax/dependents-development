package com.jwu.javaparser.dependencygraph.nodes;

import org.jgrapht.io.Attribute;
import org.jgrapht.io.DefaultAttribute;

import java.util.Map;

public class ProjectNode extends BaseNode {
    public ProjectNode(String projectName, String projectID) {
        super(projectName, projectID);
    }

    public ProjectNode(Map<String, Attribute> attributes) {
        super(attributes);
    }

    public boolean equals(Object o) {
        return (this instanceof  ProjectNode) && (o instanceof  ProjectNode) && (o.hashCode() == this.hashCode());
    }

    public ProjectNode castNode() {
        return this;
    }

    public Map<String, Attribute> generateExportAttributeMap() {
        Map<String, Attribute> m = super.generateExportAttributeMap();
        return m;
    }
}