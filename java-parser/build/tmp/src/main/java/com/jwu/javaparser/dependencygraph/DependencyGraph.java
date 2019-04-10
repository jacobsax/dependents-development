package com.jwu.javaparser.dependencygraph;

import com.jwu.javaparser.dependencygraph.edges.DependencyEdge;
import com.jwu.javaparser.dependencygraph.io.Neo4jCypherExporter;
import com.jwu.javaparser.dependencygraph.nodes.BaseNode;
import com.jwu.javaparser.dependencygraph.nodes.DependencyNode;
import com.jwu.javaparser.dependencygraph.nodes.ProjectNode;
import com.jwu.javaparser.dependencygraph.edges.types.EdgeType;
import com.jwu.javaparser.dependencygraph.nodes.types.NodeType;
import org.apache.commons.lang3.ArrayUtils;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.io.*;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * DependencyGraph is an extension of a jgrapht DefaultDirectedGraph which allows for the easier
 * construction of a dependency call graph.
 */
public class DependencyGraph extends DefaultDirectedGraph<BaseNode, DependencyEdge> implements Serializable {

    public DependencyGraph() {
        super(DependencyEdge.class);
    }

    /**
     * Builds a qualified name from an array of strings
     * @param parts array of strings (i.e. ["package", "class", "method"])
     * @return the qualified name (i.e. package.class.method)
     */
    private String buildQualifiedName(String[] parts) {
        String qualifiedName = "";
        for (int i = 0; i < parts.length; i++) {
            qualifiedName += parts[i];

            if (i < parts.length - 1) {
                qualifiedName += ".";
            }
        }

        return qualifiedName;
    }

    /**
     * Builds a qualified name with a maximum of endPos parts
     * @param parts array of strings (i.e. ["package", "class", "method"])
     * @param endPos the position of the last element of the string to use
     * @return the qualified name (i.e. package.class.method)
     */
    private String buildQualifiedName(String[] parts, int endPos) {
        String qualifiedName = "";

        if (endPos > parts.length) {
            endPos = parts.length - 1;
        }

        for (int i = 0; i <= endPos; i++) {
            qualifiedName += parts[i];

            if (i < endPos) {
                qualifiedName += ".";
            }
        }

        return qualifiedName;
    }

    /**
     * generateTree generates a tree of elements, all of the same type.
     * @param orderedElements a list of element names to be added to the graph
     * @param nodeType the type of the node
     * @return
     */
    private DependencyGraph generateTree(String[] orderedElements, NodeType nodeType) {
        DependencyGraph tmp = new DependencyGraph();

        for (int i = 0; i < orderedElements.length; i++) {
            tmp.addVertex(new DependencyNode(nodeType,  buildQualifiedName(orderedElements, i)));
        }

        for (int i = 1; i < orderedElements.length; i++) {
            tmp.addEdge(new DependencyNode(nodeType, buildQualifiedName(orderedElements, i - 1)), new DependencyNode(nodeType, buildQualifiedName(orderedElements, i)), new DependencyEdge(EdgeType.CONTAINS));
        }

        return tmp;
    }

    /**
     * addPackage adds a package to the graph.
     * @param packageName the name of the package (i.e. com, or com.package)
     * @return
     */
    public boolean addPackage(String packageName) {
        String pkg[] = packageName.split("\\.");
        DependencyGraph packageGraph = this.generateTree(pkg, NodeType.PACKAGE);

        return Graphs.addGraph(this, packageGraph);
    }

    /**
     * Constructs a graph of both packages and classes in order.
     * @param packages
     * @param classes
     * @return
     */
    private DependencyGraph buildPackageClassTree(String[] packages, String[] classes) {
        DependencyGraph graph = new DependencyGraph();

        String[] combinedArray = ArrayUtils.addAll(packages.clone(), classes.clone());

        String[] adjustedClasses = classes.clone();
        adjustedClasses[0] = buildQualifiedName(packages) + "." + adjustedClasses[0];

        if (packages.length > 0 && classes.length > 0) {

            System.out.println(packages.length);

            DependencyGraph pkgGraph = this.generateTree(packages, NodeType.PACKAGE);
            DependencyGraph classGraph = this.generateTree(adjustedClasses, NodeType.CLASS_OR_INTERFACE);

            System.out.println(pkgGraph.toString());
            System.out.println(classGraph.toString());

            Graphs.addGraph(graph, pkgGraph);
            Graphs.addGraph(graph, classGraph);

            System.out.println(buildQualifiedName(combinedArray, packages.length - 1));
            System.out.println(buildQualifiedName(combinedArray, packages.length ));

            graph.addEdge(new DependencyNode(NodeType.PACKAGE, buildQualifiedName(combinedArray, packages.length - 1)), new DependencyNode(NodeType.CLASS_OR_INTERFACE, buildQualifiedName(combinedArray, packages.length)), new DependencyEdge(EdgeType.CONTAINS));
        }

        return graph;
    }

    /**
     * claimPackageForProject claims a package for a particular project. This adds a new node
     * to represent the project, and adds an edge showing that the package is contained by the
     * project. If a multi-depth package is used (i.e. com.package.name), then the final node
     * of the packages graph (i.e. node) is connected to the project.
     *
     * For example:
     *
     *   com
     *    |      project node
     *    v          /
     * package     /
     *    |      /
     *    v    v
     *    name
     *
     * TODO: The approach explained above is inefficient. You can end up with multiple nodes in the
     * same package graph hierarchy being connected to the same project. I can't think of a nice
     * way to avoid this though.
     * @param projectName
     * @param projectID
     * @param packageName
     * @return
     */
    public boolean claimPackageForProject(String projectName, String projectID, String packageName) {

        String pkg[] = packageName.split("\\.");

        if (pkg.length > 0) {
            // construct a new temporary graph representing only the package hierarchy
            DependencyGraph packageGraph = this.generateTree(pkg, NodeType.PACKAGE);

            // add the project node to the new temp graph, and connect the project node to the final package node
            ProjectNode projectNode = new ProjectNode(projectName, projectID);
            packageGraph.addVertex(projectNode);
            packageGraph.addEdge(projectNode, new DependencyNode(NodeType.PACKAGE, packageName), new DependencyEdge(EdgeType.CONTAINS));

            // now add the temporary graph to this dependency graph. This has the effect of combining the
            // two graphs. Returns true if a change is made to this graph
            return Graphs.addGraph(this, packageGraph);
        }

        // no change was made to this graph so retunr false
        return false;
    }

    public boolean addClass(String packageName, String className) {
        String pkg[] = packageName.split("\\.");
        String cls[] = className.split("\\.");

        DependencyGraph graph = this.buildPackageClassTree(pkg, cls);

        return Graphs.addGraph(this, graph);
    }

    public boolean addMethod(String packageName, String className, String methodName) {
        String pkg[] = packageName.split("\\.");

        String cls[] = className.split("\\.");

        String[] combinedArray = ArrayUtils.addAll(pkg.clone(), cls.clone());

        String qualifiedName = packageName + "." + className + "." + methodName;

        DependencyGraph graph = this.buildPackageClassTree(pkg, cls);

        if (cls.length > 0) {
            DependencyNode methodNode = new DependencyNode(NodeType.METHOD, qualifiedName);
            graph.addVertex(methodNode);
            graph.addEdge(new DependencyNode(NodeType.CLASS_OR_INTERFACE, buildQualifiedName(combinedArray)), methodNode, new DependencyEdge(EdgeType.CONTAINS));
        }

        return Graphs.addGraph(this, graph);
    }

    public boolean addCallsEdge(String callerQualifiedName, String targetQualifiedName) {
        return this.addEdge(new DependencyNode(NodeType.METHOD, callerQualifiedName), new DependencyNode(NodeType.METHOD, targetQualifiedName), new DependencyEdge(EdgeType.CALLS));
    }

    public boolean addExtendsEdge(String extendedQualifiedName, String extenderQualifiedName) {
        return this.addEdge(new DependencyNode(NodeType.CLASS_OR_INTERFACE, extendedQualifiedName), new DependencyNode(NodeType.CLASS_OR_INTERFACE, extenderQualifiedName), new DependencyEdge(EdgeType.EXTENDED_BY));
    }

    public boolean addOverridesOverloadsEdge(String overridingMethodQualifiedName, String overridenMethodQualifiedName) {
        return this.addEdge(new DependencyNode(NodeType.METHOD, overridenMethodQualifiedName), new DependencyNode(NodeType.METHOD, overridingMethodQualifiedName), new DependencyEdge(EdgeType.OVERRIDDEN_OR_OVERLOADED_BY));
    }

    public boolean addVertex(DependencyNode v) {
        boolean vertexAdded = super.addVertex(v);

        if (!vertexAdded) {
            Set<BaseNode> nodes = this.vertexSet();

            for (Iterator<BaseNode> n = nodes.iterator(); n.hasNext(); ) {
                BaseNode node = n.next();
                if (node.equals(v)) {

                }
            }
        }

        return vertexAdded;
    }


    public static Neo4jCypherExporter getNeo4jExporter() {

        ComponentNameProvider<BaseNode> neoNodeTypeProvider = node -> {
            if (node instanceof ProjectNode) {
                return "Project";
            }

            if (node instanceof DependencyNode) {
                return ((DependencyNode) node).type.toString();
            }

            return "Base";
        };

        ComponentNameProvider<BaseNode> vertexIdProvider = node -> node.id;

        ComponentNameProvider<BaseNode> vertexLabelProvider = node -> (node.toString());

        ComponentNameProvider<DependencyEdge> edgeLabelProvider = edge -> (edge.toString().toLowerCase());

        ComponentAttributeProvider<BaseNode> nodeComponentAttributeProvider = baseNode -> {
            Map<String, Attribute> m = baseNode.generateExportAttributeMap();
            m.put("jgrapht_vertex_type", DefaultAttribute.createAttribute(baseNode.getClass().getName())); // store the name of the nodes class

            return m;
        };

        ComponentAttributeProvider<DependencyEdge> dependencyEdgeComponentAttributeProvider = edge -> {
            Map<String, Attribute> m = new HashMap<>();

            m.put("type", DefaultAttribute.createAttribute(edge.type.toString()));

            return m;
        };


        Neo4jCypherExporter exporter = new Neo4jCypherExporter(vertexIdProvider, vertexLabelProvider, edgeLabelProvider, nodeComponentAttributeProvider, dependencyEdgeComponentAttributeProvider, neoNodeTypeProvider);

        return exporter;
    }


    public static GraphExporter<BaseNode, DependencyEdge> getDOTExporter() {

        ComponentNameProvider<BaseNode> vertexIdProvider = node -> String.valueOf(node.hashCode());

        ComponentNameProvider<BaseNode> vertexLabelProvider = node -> (node.prettyString());

        ComponentNameProvider<DependencyEdge> edgeLabelProvider = edge -> (edge.toString().toLowerCase());

        ComponentAttributeProvider<BaseNode> nodeComponentAttributeProvider = baseNode -> {
                Map<String, Attribute> m = baseNode.generateExportAttributeMap();
                m.put("jgrapht_vertex_type", DefaultAttribute.createAttribute(baseNode.getClass().getName())); // store the name of the nodes class

                return m;
            };

        ComponentAttributeProvider<DependencyEdge> dependencyEdgeComponentAttributeProvider = edge -> {
            Map<String, Attribute> m = new HashMap<>();

            m.put("type", DefaultAttribute.createAttribute(edge.type.toString()));

            return m;
        };

        GraphExporter<BaseNode, DependencyEdge> exporter = new DOTExporter(vertexIdProvider, vertexLabelProvider, edgeLabelProvider, nodeComponentAttributeProvider, dependencyEdgeComponentAttributeProvider);

        return exporter;
    }

    public static GraphImporter<BaseNode, DependencyEdge> getDOTImporter() {
        VertexProvider<BaseNode> nodeProvider = (id, attributes) -> {


            String jgraphtVertexType = attributes.get("jgrapht_vertex_type").getValue();
            if (jgraphtVertexType.equals(DependencyNode.class.getName())) {
                DependencyNode node = new DependencyNode(attributes);
                return node;

            } else if (jgraphtVertexType.equals(ProjectNode.class.getName())) {
                ProjectNode node = new ProjectNode(attributes);
                return node;
            }

            return null;
        };

        EdgeProvider<BaseNode, DependencyEdge> edgeProvider =
                (from, to, label, attributes) -> {

                    EdgeType type = EdgeType.valueOf(attributes.get("type").getValue());

                    DependencyEdge dependencyEdge = new DependencyEdge(type);

                    return dependencyEdge;
                };

        DOTImporter<BaseNode, DependencyEdge> dotImporter = new DOTImporter(nodeProvider, edgeProvider);

        return dotImporter;
    }

    public static void saveGraphToFile (GraphExporter exporter, DependencyGraph graph,  String fileName) throws IOException, ExportException {
        exporter.exportGraph(graph, new FileWriter(fileName));
    }

    public static DependencyGraph readGraphFromFile (GraphImporter importer, String fileName) throws FileNotFoundException, ImportException {
        DependencyGraph graph = new DependencyGraph();
        importer.importGraph(graph, new FileReader(fileName));

        return graph;
    }

    public static void serialize(String filename, DependencyGraph dependencyGraph) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(dependencyGraph);

        out.close();
    }


    /**
     * deserialize a DependencyGraph from a file name
     * @param filename
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static DependencyGraph deserialize(String filename) throws IOException, ClassNotFoundException {

        FileInputStream fileInputStream = new FileInputStream(filename);
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        DependencyGraph graph = (DependencyGraph) objectInputStream.readObject();

        objectInputStream.close();

        return graph;
    }
}
