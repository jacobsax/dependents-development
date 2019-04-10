package com.jwu.javaparser.dependencygraph.nodes;

import org.jgrapht.io.Attribute;
import org.jgrapht.io.DefaultAttribute;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class BaseNode {
    public final String name;
    public final String id;

    BaseNode(String name, String id) {
        this.name = name;
        this.id = id;
    }

    BaseNode(Map<String, Attribute> attributes) {
        if (attributes.containsKey("name") && attributes.containsKey("id")) {
            this.name = attributes.get("name").getValue();
            this.id = attributes.get("id").getValue();
        } else {
            throw new IllegalArgumentException("Attribute was missing from map for BaseNode.");
        }
    }

    @Override
    public final int hashCode() {
        return this.id.hashCode();
    }

    public String toString() {
        String toReturn =  Normalizer.normalize(this.name, Normalizer.Form.NFD);
        return toReturn.replaceAll("[^\\x00-\\x7F]", "");
    }

    public String prettyString() {
        return this.toString();
    }

    public abstract boolean equals(Object o);

    public abstract Object castNode();

    public Map<String, Attribute> generateExportAttributeMap() {
        Map<String, Attribute> m = new HashMap();

        m.put("name", DefaultAttribute.createAttribute(this.name));
        m.put("id", DefaultAttribute.createAttribute(this.id));

        return m;
    }
}
