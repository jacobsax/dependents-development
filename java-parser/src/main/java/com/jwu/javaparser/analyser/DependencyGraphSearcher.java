package com.jwu.javaparser.analyser;

import com.jwu.javaparser.dependencygraph.DependencyGraph;
import com.jwu.javaparser.dependencygraph.edges.DependencyEdge;
import com.jwu.javaparser.dependencygraph.edges.types.EdgeType;
import com.jwu.javaparser.dependencygraph.nodes.BaseNode;
import com.jwu.javaparser.dependencygraph.nodes.DependencyNode;
import com.jwu.javaparser.dependencygraph.nodes.ProjectNode;
import com.jwu.javaparser.dependencygraph.nodes.types.NodeType;
import org.jgrapht.Graphs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyGraphSearcher {
    DependencyGraph graph;
    String projectName;
    String projectID;

    ConcurrentHashMap<DependencyNode, Set<DependencyNode>> transitiveCallCache;

    DependencyGraphSearcher(DependencyGraph graph, String projectName, String projectID) {
        this.graph = graph;
        this.projectName = projectName;
        this.projectID = projectID;
        this.transitiveCallCache = new ConcurrentHashMap();
    }

    /**
     * filterNodesForType filters a list of nodes and returns the dependencyNodes of a specific
     * type
     * @param nodes
     * @param nodeType
     * @return
     */
    public List<DependencyNode> filterNodesForType(List<BaseNode> nodes, NodeType nodeType) {
        return nodes.stream()
                .filter(node ->  (node instanceof DependencyNode))
                .map(node -> (DependencyNode) node)
                .filter( node -> node.type.equals(nodeType))
                .collect(Collectors.toList());
    }

    public Optional<ProjectNode> getProjectOfNode(DependencyNode startNode) {
        List<BaseNode> predecessorNodes = this.getPredecessorNodes(startNode, EdgeType.CONTAINS);

        List<ProjectNode> projectNodes = predecessorNodes.stream()
                .filter(node -> (node instanceof ProjectNode))
                .map(node -> (ProjectNode) node)
                .collect(Collectors.toList());

        // the node should have a single project associated with it
        if (projectNodes.size() == 1) {
            return Optional.of(projectNodes.get(0));
        } else {
            // else, the node should be contained within a single package
            List<DependencyNode> packageNodes = this.filterNodesForType(predecessorNodes, NodeType.PACKAGE);
            if (packageNodes.size() == 1) {
                return getProjectOfNode(packageNodes.get(0));
            } else {

                // else, the node should be contained within a single class/enum
                List<DependencyNode> classInterfaceNodes = this.filterNodesForType(predecessorNodes, NodeType.CLASS_OR_INTERFACE);

                if (classInterfaceNodes.size() == 1) {
                    return getProjectOfNode(classInterfaceNodes.get(0));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * getPackagesInProject returns a list of packages in a specified project
     * @return
     */
    public  List<DependencyNode> getPackagesInProject() {
        List<BaseNode> nodes = this.getSuccessorNodes(new ProjectNode(this.projectName, this.projectID), EdgeType.CONTAINS);
        return this.filterNodesForType(nodes, NodeType.PACKAGE);
    }

    public Set<DependencyNode> getClassesAndInterfacesInPackage(DependencyNode node) {
        Set<DependencyNode> identifiedClasses = new HashSet();

        if (node.type == NodeType.PACKAGE) {
            identifiedClasses.addAll(this.searchSubGraphForNodes(node, NodeType.CLASS_OR_INTERFACE, EdgeType.CONTAINS));
        }

        return identifiedClasses;
    }

    public Set<DependencyNode> searchSubGraphForNodes(BaseNode node, NodeType nodeType, EdgeType edgeType) {
        List<BaseNode> successorNodes = this.getSuccessorNodes(node, edgeType);
        Set<DependencyNode> identifiedNodes = new HashSet(this.filterNodesForType(successorNodes, nodeType));

        for (BaseNode successorNode : successorNodes) {
            identifiedNodes.addAll(this.searchSubGraphForNodes(successorNode, nodeType, edgeType));
        }

        return identifiedNodes;
    }

    public Set<DependencyNode> searchSuperGraphForNodes(DependencyNode node, NodeType nodeType, EdgeType edgeType) {
        return this.searchSuperGraphForNodes(node, nodeType, edgeType, new HashSet<DependencyNode>());
    }

    public Set<DependencyNode> searchSuperGraphForNodes(DependencyNode node, NodeType nodeType, EdgeType edgeType, HashSet<DependencyNode> traversedNodes) {

        List<BaseNode> predecessorNodes = this.getPredecessorNodes(node, edgeType);
        // create a list of all nodes of the correct type which are connected to this node with the correct edge
        HashSet<DependencyNode> identifiedNodes = new HashSet(this.filterNodesForType(predecessorNodes, nodeType));

        // if the current node has not yet had its dependent calls cached, search for
        // dependents, and add to the cache
        if (!this.transitiveCallCache.contains(node)) {
            HashSet<DependencyNode> newTraversedNodes = new HashSet<>(traversedNodes);
            newTraversedNodes.addAll(identifiedNodes);

            // Recurse the search on each of the identified nodes
            for (DependencyNode identifiedNode : new HashSet<DependencyNode>(identifiedNodes)) {
                    if (!traversedNodes.contains(identifiedNode)) {
                        Set<DependencyNode> dependentNodes = this.searchSuperGraphForNodes(identifiedNode, nodeType, edgeType, newTraversedNodes);
                        identifiedNodes.addAll(dependentNodes);
                    }
            }

            this.transitiveCallCache.put(node, identifiedNodes);
        } else {
            System.out.println("USED CACHE!!!");
            identifiedNodes.addAll(this.transitiveCallCache.get(node));
        }

        return identifiedNodes;
    }


    public Set<DependencyNode> getClassesAndInterfacesInProject() {
        Set<DependencyNode> identifiedClasses = new HashSet();
        this.getPackagesInProject().forEach(node -> {
            identifiedClasses.addAll(this.getClassesAndInterfacesInPackage(node));
        });

        return identifiedClasses;
    }

    // returns a set of al methods in the project
    public Set<DependencyNode> getMethodsInClassOrInterface(DependencyNode node) {
        Set<DependencyNode> identifiedMethods = new HashSet();

        if (node.type == NodeType.CLASS_OR_INTERFACE) {
            identifiedMethods.addAll(this.searchSubGraphForNodes(node, NodeType.METHOD, EdgeType.CONTAINS));
        }

        return identifiedMethods;
    }

    // returns a set of al methods in the project
    public Set<DependencyNode> getMethodsInProject() {
        Set<DependencyNode> identifiedMethods = new HashSet();
        this.getPackagesInProject().forEach(node -> {
            identifiedMethods.addAll(this.getMethodsInClassOrInterface(node));
        });

        return identifiedMethods;
    }

    // returns a set of al methods called by methods, classes etc. in the project
    public Set<DependencyNode> getMethodCallsInProject() {
        Set<DependencyNode> searchStartPositions = this.getClassesAndInterfacesInProject();
        searchStartPositions.addAll(this.getMethodsInProject());

        Set<DependencyNode> calledMethods = new HashSet();

        for (DependencyNode searchStartNode : searchStartPositions) {
            calledMethods.addAll(
                    this.filterNodesForType(
                            this.getSuccessorNodes(searchStartNode, EdgeType.CALLS),
                            NodeType.METHOD)
            );
        }

        return calledMethods;
    }

    // returns a set of all methods which directly call the selected method
    public Set<DependencyNode> getCallsOfMethod(DependencyNode node) {

        Set<DependencyNode> calledMethods = new HashSet();

        calledMethods.addAll(this.filterNodesForType(this.getPredecessorNodes(node, EdgeType.CALLS), NodeType.METHOD));

        return calledMethods;
    }

    // returns a set of all methods which directly call the selected method
    public Set<DependencyNode> getAllCallsOfMethod(DependencyNode node) {
        return this.searchSuperGraphForNodes(node, NodeType.METHOD, EdgeType.CALLS);
    }

    /**
     * Finds and returns a set of all methods called.
     *
     * TODO: This should be carried out more elegantly, and not dependent
     * on first identifying methods and classes in the project
     * @return
     */
    public Set<DependencyNode> getExternalMethodCalls() {
        Set<DependencyNode> methodsInProject = this.getMethodsInProject();
        Set<DependencyNode> methodCallsInProject = this.getMethodCallsInProject();

        // find the method calls made by the project to external packages
        methodCallsInProject.removeAll(methodsInProject);
        return methodCallsInProject;
    }

        /**
         * Returns the edge between two nodes
         * @param n1
         * @param n2
         * @return
         */
    public DependencyEdge getEdgeBetweenNodes(BaseNode n1, BaseNode n2) {
        return this.graph.getEdge(n1, n2);
    }

    /**
     * Returns the successors of a specified node
     * @param parentNode
     * @return
     */
    public List<BaseNode> getSuccessorNodes(BaseNode parentNode) {
        return Graphs.successorListOf(this.graph, parentNode);
    }

    public List<BaseNode> getPredecessorNodes(BaseNode childNode) {
        List<BaseNode> nodes = Graphs.predecessorListOf(this.graph, childNode);
        return nodes;
    }

    public List<BaseNode> getSuccessorNodes(BaseNode parentNode, EdgeType edgeType) {
        return this.getOutgoingEdges(parentNode).stream()
                .filter(edge -> edge.type.equals(edgeType))
                .map(edge -> edge.getChildNode())
                .collect(Collectors.toList());
    }

    public List<BaseNode> getPredecessorNodes(BaseNode childNode, EdgeType edgeType) {
        return this.getIncomingEdges(childNode).stream()
                .filter(edge -> edge.type.equals(edgeType))
                .map(edge -> edge.getParentNode())
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of qualified outgoing edges
     * @param node
     * @return
     */
    public List<QualifiedDependencyEdge> getOutgoingEdges(BaseNode node) {
        List<QualifiedDependencyEdge> qualifiedDependencyEdges = new ArrayList<>();
        List<BaseNode> successors = this.getSuccessorNodes(node);

        for (BaseNode successorNode: successors) {
            DependencyEdge edge = graph.getEdge(node, successorNode);
            qualifiedDependencyEdges.add(new QualifiedDependencyEdge(node, successorNode, edge));
        }

        return qualifiedDependencyEdges;
    }

    /**
     * Returns a list of qualified incoming edges
     * @param node
     * @return
     */
    public List<QualifiedDependencyEdge> getIncomingEdges(BaseNode node) {
        List<QualifiedDependencyEdge> qualifiedDependencyEdges = new ArrayList<>();
        List<BaseNode> predecessorNodes = this.getPredecessorNodes(node);

        for (BaseNode predecessorNode: predecessorNodes) {
            DependencyEdge edge = graph.getEdge(predecessorNode, node);
            qualifiedDependencyEdges.add(new QualifiedDependencyEdge(predecessorNode, node, edge));
        }

        return qualifiedDependencyEdges;
    }

}
