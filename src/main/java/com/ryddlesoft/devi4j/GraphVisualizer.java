package com.ryddlesoft.devi4j;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GraphVisualizer extends JPanel {

    private Viewer viewer;
    private ViewPanel viewPanel;

    private final String STYLESHEET =
        "graph { padding: 100px;}" +
        "node { " +
        "   shape: box; " +
        "   size-mode: fit; " +
        "   padding: 10px, 5px; " +
        "   text-size: 12px; " +
        "   text-alignment: center; " +
        "   stroke-mode: plain; " +
        "   stroke-color: black; " +
        "   fill-color: white; " +
        "} " +
        "node.package { " +
        "   fill-color: #B0E0E6; " +
        "} " +
        "node.cycle { " +
        "   fill-color: red; " +
        "   text-color: white; " +
        "   stroke-color: #900; " +
        "} " +
        "edge { " +
        "   arrow-shape: arrow;" +
        "   arrow-size: 8px, 5px;" +
        "}" +
        "edge.cycle { " +
        "   fill-color: red; " +
        "}";

    public GraphVisualizer() {
        super(new BorderLayout());
        // Add a placeholder label for when no graph is loaded
        add(new JLabel("No project loaded. Open or create a project to see the graph.", SwingConstants.CENTER), BorderLayout.CENTER);
    }

    public Graph createStyledGraph(String id) {
        System.setProperty("org.graphstream.ui", "swing");
        Graph graph = new SingleGraph(id);
        graph.setAttribute("ui.stylesheet", STYLESHEET);
        graph.setAttribute("ui.quality");
        graph.setAttribute("ui.antialias");
        return graph;
    }

    public void applyHeuristicLayout(Graph graph) {
        if (graph.getNodeCount() <= 1) {
            // No special layout needed for trivial graphs
            return;
        }

        // 1. Find the node with the highest degree
        Node centralNode = null;
        int maxDegree = -1;
        for (Node node : graph) {
            if (node.getDegree() > maxDegree) {
                maxDegree = node.getDegree();
                centralNode = node;
            }
        }

        if (centralNode == null) {
            return; // Should not happen in a non-empty graph
        }

        Set<Node> positionedNodes = new HashSet<>();

        // 2. Position the central node
        centralNode.setAttribute("x", 0);
        centralNode.setAttribute("y", 0);
        positionedNodes.add(centralNode);

        // 3. Position the first-level neighbors in a ring
        java.util.List<Node> neighbors = new ArrayList<>();
        centralNode.neighborNodes().forEach(neighbors::add);

        double radius1 = 150; // Radius for the first ring
        if (!neighbors.isEmpty()) {
            double angleStep1 = 2 * Math.PI / neighbors.size();
            for (int i = 0; i < neighbors.size(); i++) {
                Node neighbor = neighbors.get(i);
                double angle = i * angleStep1;
                neighbor.setAttribute("x", radius1 * Math.cos(angle));
                neighbor.setAttribute("y", radius1 * Math.sin(angle));
                positionedNodes.add(neighbor);
            }
        }

        // 4. Position all other nodes in a second, larger ring
        java.util.List<Node> otherNodes = new ArrayList<>();
        for (Node node : graph) {
            if (!positionedNodes.contains(node)) {
                otherNodes.add(node);
            }
        }

        if (!otherNodes.isEmpty()) {
            double radius2 = 300; // Radius for the second ring
            double angleStep2 = 2 * Math.PI / (otherNodes.isEmpty() ? 1 : otherNodes.size());
            for (int i = 0; i < otherNodes.size(); i++) {
                Node other = otherNodes.get(i);
                double angle = i * angleStep2;
                other.setAttribute("x", radius2 * Math.cos(angle));
                other.setAttribute("y", radius2 * Math.sin(angle));
            }
        }
    }

     private void highlightCycles(Graph graph, List<List<String>> cycles) {
        for (List<String> cycle : cycles) {
            for (String pkg : cycle) {
                Node node = graph.getNode(pkg);
                if (node != null) {
                    node.setAttribute("ui.class", "cycle");
                }
            }
            // Highlight edges within the cycle
            for (int i = 0; i < cycle.size(); i++) {
                String from = cycle.get(i);
                String to = cycle.get((i + 1) % cycle.size()); // Next node in cycle
                // This is an approximation, we need to check if the edge actually exists
                // in the original graph to be precise.
                if (graph.getNode(from) != null && graph.getEdge(from + "->" + to) != null) {
                     graph.getEdge(from + "->" + to).setAttribute("ui.class", "cycle");
                }
            }
        }
    }

    public void updateGraph(Graph graph, List<List<String>> cycles) {
        highlightCycles(graph, cycles);

        // Clean up previous view if it exists
        if (viewer != null) {
            viewer.close();
        }
        if (viewPanel != null) {
            remove(viewPanel);
        }

        // Create a new viewer and view panel
        viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.disableAutoLayout();

        Layout layout = new SpringBox(false);
        graph.addSink(layout);

        // Run the layout algorithm for a fixed number of iterations
        for (int i = 0; i < 2000; i++) {
            layout.compute();
        }

        graph.removeSink(layout); // Stop the layout algorithm

        viewPanel = (ViewPanel) viewer.addDefaultView(false);

        // Add the new view panel to this component
        removeAll();
        add(viewPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}
