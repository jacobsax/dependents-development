package com.jwu.javaparser.dependencygraph.nodes;

import com.jwu.javaparser.dependencygraph.nodes.types.NodeType;
import org.jgrapht.io.Attribute;
import org.jgrapht.io.DefaultAttribute;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.Map;

public class DependencyNode extends BaseNode implements Serializable {
    public final NodeType type;

    public DependencyNode(NodeType type, String qualifiedName) {
        super(DependencyNode.simpleName(qualifiedName), qualifiedName);

        this.type = type;
    }

    public static String simpleName(String qualifiedName) {
        String[] splitQualifiedName = qualifiedName.split("\\.");
        String name = splitQualifiedName[splitQualifiedName.length - 1];
        name = Normalizer.normalize(name, Normalizer.Form.NFD);
        name = name.replaceAll("[^\\x00-\\x7F]", "");

        return name;
    }

    public DependencyNode(Map<String, Attribute> attributes) {
        super(attributes);

        if (attributes.containsKey("type")) {
            this.type = NodeType.valueOf(attributes.get("type").getValue());
        } else {
            throw new IllegalArgumentException("Attribute was missing from map for DependencyNode.");
        }
    }

    public String toString() {
        String toReturn =  Normalizer.normalize(this.name, Normalizer.Form.NFD);
        return toReturn.replaceAll("[^\\x00-\\x7F]", "");
    }


    public String prettyString() {
        String typeString =  this.type.toString();

        return typeString.substring(0, 1).toUpperCase() + typeString.substring(1).toLowerCase() + ": " + this.toString();
    }

    public boolean equals(Object o) {
        return (this instanceof  DependencyNode) && (o instanceof  DependencyNode) && (o.hashCode() == this.hashCode());
    }

    public DependencyNode castNode() {
        return this;
    }

    public Map<String, Attribute> generateExportAttributeMap() {
        Map<String, Attribute> m = super.generateExportAttributeMap();

        m.put("type", DefaultAttribute.createAttribute(this.type.toString()));
        m.put("qualified_name", DefaultAttribute.createAttribute(this.id));

        return m;
    }
}
