package com.jwu.javaparser.dependencygraph.io;

import com.jwu.javaparser.dependencygraph.edges.DependencyEdge;
import com.jwu.javaparser.dependencygraph.nodes.DependencyNode;
import org.jgrapht.io.*;

import java.util.HashMap;
import java.util.Map;

/**
 * From https://github.com/jgrapht/jgrapht/blob/master/jgrapht-demo/src/main/java/org/jgrapht/demo/GraphMLDemo.java
 * Create exporter
 */


public class DependencyGraphExporter {

        public DependencyGraphExporter() { }

        public GraphMLExporter<DependencyNode, DependencyEdge> getExporter(){
                ComponentNameProvider<DependencyNode> vertexIdProvider = node -> node.toString().replace(".", "_");

                ComponentNameProvider<DependencyNode> vertexLabelProvider = node -> (node.prettyString());

                ComponentNameProvider<DependencyEdge> edgeLabelProvider = edge -> (edge.toString().toLowerCase());

                /*
                 * The exporter may need to generate for each vertex a set of attributes. Attributes must
                 * also be registered as shown later on.
                 */
                ComponentAttributeProvider<DependencyNode> vertexAttributeProvider = v -> {
                        Map<String, Attribute> m = new HashMap<>();
//                        if (v.getColor() != null) {
////                                m.put("color", DefaultAttribute.createAttribute(v.getColor().toString()));
////                        }

                        m.put("type", DefaultAttribute.createAttribute(v.type.toString()));
                        m.put("name", DefaultAttribute.createAttribute("node-" + v.name));

                        return m;
                };

                ComponentAttributeProvider<DependencyEdge> edgeAttributeProvider = e -> {
                        Map<String, Attribute> m = new HashMap<>();
                        m.put("name", DefaultAttribute.createAttribute(e.toString()));
                        return m;
                };

                ComponentNameProvider<DependencyEdge> edgeIdProvider =
                        new IntegerComponentNameProvider<>();

                GraphMLExporter<DependencyNode,
                        DependencyEdge> exporter = new GraphMLExporter<>(
                        vertexIdProvider, vertexLabelProvider, vertexAttributeProvider, edgeIdProvider,
                        edgeLabelProvider, edgeAttributeProvider);

                /*
                 * Set to export the internal edge weights
                 */
                exporter.setExportEdgeWeights(true);

                /*
                 * Register additional color attribute for vertices
                 */
                exporter.registerAttribute("color", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);

                /*
                 * Register additional name attribute for vertices and edges
                 */
                exporter.registerAttribute("name", GraphMLExporter.AttributeCategory.ALL, AttributeType.STRING);

                return exporter;
        }
}

