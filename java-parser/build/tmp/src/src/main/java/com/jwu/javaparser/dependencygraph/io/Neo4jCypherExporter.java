package com.jwu.javaparser.dependencygraph.io;

import com.jwu.javaparser.dependencygraph.DependencyGraph;
import com.jwu.javaparser.dependencygraph.edges.DependencyEdge;
import com.jwu.javaparser.dependencygraph.nodes.BaseNode;
import com.jwu.javaparser.dependencygraph.nodes.DependencyNode;
import com.jwu.javaparser.dependencygraph.nodes.ProjectNode;
import com.jwu.javaparser.dependencygraph.nodes.types.NodeType;
import org.jgrapht.io.*;
import org.jgrapht.*;
import org.neo4j.driver.v1.*;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 */
public class Neo4jCypherExporter {

    /**
     * Default graph id used by the exporter.
     */
    public static final String DEFAULT_GRAPH_ID = "G";

    private ComponentNameProvider<BaseNode> vertexIDProvider;
    private ComponentNameProvider<DependencyEdge> edgeIDProvider;
    private final ComponentNameProvider<BaseNode> vertexLabelProvider;
    private final ComponentNameProvider<DependencyEdge> edgeLabelProvider;
    private final ComponentAttributeProvider<BaseNode> vertexAttributeProvider;
    private final ComponentAttributeProvider<DependencyEdge> edgeAttributeProvider;
    private final Map<String, String> graphAttributes;
    private final Map<DependencyEdge, String> vertexIds;
    ComponentNameProvider<BaseNode> vertexNodeTypeProvider;


    private static final String INDENT = "  ";


    public Neo4jCypherExporter(
            ComponentNameProvider<BaseNode> vertexIDProvider, ComponentNameProvider<BaseNode> vertexLabelProvider,
            ComponentNameProvider<DependencyEdge> edgeLabelProvider,
            ComponentAttributeProvider<BaseNode> vertexAttributeProvider,
            ComponentAttributeProvider<DependencyEdge> edgeAttributeProvider,
            ComponentNameProvider<BaseNode> vertexNodeTypeProvider
            )
    {
        this.vertexIDProvider = vertexIDProvider;
        this.edgeIDProvider = null; // an edge ID provider is unnecessary
        this.vertexLabelProvider = vertexLabelProvider;
        this.edgeLabelProvider = edgeLabelProvider;
        this.vertexAttributeProvider = vertexAttributeProvider;
        this.edgeAttributeProvider = edgeAttributeProvider;
        this.vertexNodeTypeProvider = vertexNodeTypeProvider;
        this.graphAttributes = new LinkedHashMap<>();
        this.vertexIds = new HashMap<>();
    }

    public void exportGraph(DependencyGraph graph, Writer writer) {

        PrintWriter cypherOut = new PrintWriter(writer);

        for (BaseNode vertex: graph.vertexSet()) {
            // Project nodes only have an ID, and must therefore be treated differently to other nodes
            if (vertex instanceof ProjectNode) {
                String transaction = String.format("MERGE (p:%s {id: '%s'});", this.vertexNodeTypeProvider.getName(vertex), this.vertexIDProvider.getName(vertex));
                cypherOut.println(transaction);
            } else {
                String transaction = String.format("MERGE (p:%s {id: '%s', name: '%s'});", this.vertexNodeTypeProvider.getName(vertex), this.vertexIDProvider.getName(vertex), this.vertexLabelProvider.getName(vertex));
                cypherOut.println(transaction);
            }
        }

        for (DependencyEdge edge: graph.edgeSet()) {
            BaseNode edgeSource = graph.getEdgeSource(edge);
            BaseNode edgeTarget = graph.getEdgeTarget(edge);

            String transaction = String.format("MATCH (source:%s { id: '%s'}),(target:%s { id: '%s'})" +
                            " MERGE (source)-[:%s]->(target);",
                    this.vertexNodeTypeProvider.getName(edgeSource),
                    this.vertexIDProvider.getName(edgeSource),
                    this.vertexNodeTypeProvider.getName(edgeTarget),
                    this.vertexIDProvider.getName(edgeTarget),
                    edge.toString());

            cypherOut.println(transaction);

        }

        cypherOut.flush();
    }

    /**
     * Get the vertex id provider
     *
     * @return the vertex id provider
     */
    public ComponentNameProvider<BaseNode> getVertexIDProvider()
    {
        return this.vertexIDProvider;
    }

    /**
     * Set the vertex id provider
     *
     * @param vertexIDProvider the new vertex id provider. Must not be null.
     */
    public void setVertexIDProvider(ComponentNameProvider<BaseNode> vertexIDProvider)
    {
        this.vertexIDProvider =
                Objects.requireNonNull(vertexIDProvider, "Vertex id provider cannot be null");
    }

    /**
     * Get the edge id provider
     *
     * @return The edge provider
     */
    public ComponentNameProvider<DependencyEdge> getEdgeIDProvider()
    {
        return this.edgeIDProvider;
    }

    /**
     * Set the edge id provider.
     *
     * @param edgeIDProvider the new edge id provider. Must not be null.
     */
    public void setEdgeIDProvider(ComponentNameProvider<DependencyEdge> edgeIDProvider)
    {
        this.edgeIDProvider = edgeIDProvider;
    }

}
