package com.ryddlesoft.devi4j;

import java.util.*;

public class CycleDetector {

    private int index;
    private final Stack<String> stack;
    private final Map<String, Integer> ids;
    private final Map<String, Integer> lowLink;
    private final Map<String, Boolean> onStack;
    private final Map<String, Set<String>> graph;
    private final List<List<String>> sccs;

    public CycleDetector(Map<String, Set<String>> graph) {
        this.graph = graph;
        this.index = 0;
        this.stack = new Stack<>();
        this.ids = new HashMap<>();
        this.lowLink = new HashMap<>();
        this.onStack = new HashMap<>();
        this.sccs = new ArrayList<>();
    }

    public List<List<String>> findCycles() {
        return findCycles(graph.keySet());
    }

    public List<List<String>> findCycles(Set<String> nodesToSearch) {
        for (String node : nodesToSearch) {
            if (graph.containsKey(node) && !ids.containsKey(node)) {
                dfs(node);
            }
        }

        List<List<String>> cycles = new ArrayList<>();
        for (List<String> scc : sccs) {
            if (scc.size() > 1) {
                cycles.add(scc);
            } else {
                // Check for self-loops
                String node = scc.get(0);
                if (graph.get(node) != null && graph.get(node).contains(node)) {
                    cycles.add(scc);
                }
            }
        }
        return cycles;
    }

    private void dfs(String at) {
        stack.push(at);
        onStack.put(at, true);
        ids.put(at, index);
        lowLink.put(at, index);
        index++;

        Set<String> neighbors = graph.get(at);
        if (neighbors != null) {
            for (String to : neighbors) {
                if (!ids.containsKey(to)) {
                    dfs(to);
                    lowLink.put(at, Math.min(lowLink.get(at), lowLink.get(to)));
                } else if (onStack.get(to)) {
                    lowLink.put(at, Math.min(lowLink.get(at), ids.get(to)));
                }
            }
        }

        if (ids.get(at).equals(lowLink.get(at))) {
            List<String> scc = new ArrayList<>();
            while (true) {
                String node = stack.pop();
                onStack.put(node, false);
                scc.add(node);
                if (node.equals(at)) break;
            }
            sccs.add(scc);
        }
    }
}
