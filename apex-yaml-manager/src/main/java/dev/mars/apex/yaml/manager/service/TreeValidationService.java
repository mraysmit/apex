package dev.mars.apex.yaml.manager.service;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import dev.mars.apex.yaml.manager.model.EnhancedYamlDependencyGraph;
import dev.mars.apex.yaml.manager.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Validates the dependency tree and graph prior to UI rendering.
 * Provides structural checks (required fields, unique paths),
 * basic graph integrity checks (missing references), and summary stats.
 */
@Service
public class TreeValidationService {
    private static final Logger logger = LoggerFactory.getLogger(TreeValidationService.class);

    /**
     * Validate the given tree built from the graph and produce a structured report.
     */
    public Map<String, Object> validate(EnhancedYamlDependencyGraph graph, TreeNode tree) {
        Map<String, Object> report = new HashMap<>();
        Map<String, List<String>> issues = new LinkedHashMap<>();
        issues.put("CRITICAL", new ArrayList<>());
        issues.put("HIGH", new ArrayList<>());
        issues.put("MEDIUM", new ArrayList<>());
        issues.put("LOW", new ArrayList<>());

        if (tree == null) {
            issues.get("CRITICAL").add("Tree is null (no data to render)");
            report.put("status", "error");
            report.put("issues", issues);
            return report;
        }

        // Traverse tree and collect node stats
        Set<String> uniquePaths = new HashSet<>();
        List<String> duplicatePaths = new ArrayList<>();
        int[] nodeCount = new int[]{0};
        int[] maxDepthObserved = new int[]{0};

        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(tree);
        while (!stack.isEmpty()) {
            TreeNode n = stack.pop();
            nodeCount[0]++;
            String path = n.getPath();
            String name = n.getName();

            if (path == null || path.isBlank()) {
                issues.get("CRITICAL").add("Node missing required 'path'");
            }
            if (name == null || name.isBlank()) {
                issues.get("HIGH").add("Node missing required 'name' for path=" + String.valueOf(path));
            }

            if (path != null) {
                if (!uniquePaths.add(path)) {
                    duplicatePaths.add(path);
                }
            }

            // Track depth (stored on node if present)
            maxDepthObserved[0] = Math.max(maxDepthObserved[0], n.getDepth());

            List<TreeNode> children = n.getChildren();
            if (children != null) {
                for (TreeNode c : children) {
                    if (c == null) {
                        issues.get("HIGH").add("Null child encountered under path=" + path);
                        continue;
                    }
                    stack.push(c);
                }
            }
        }

        if (!duplicatePaths.isEmpty()) {
            issues.get("HIGH").add("Duplicate node paths: " + String.join(", ", new LinkedHashSet<>(duplicatePaths)));
        }

        // Graph integrity checks (limited to nodes present in this tree)
        if (graph != null) {
            Map<String, Set<String>> fwd = graph.getForwardEdges();
            // Only consider edges from nodes present in the tree
            for (String src : new ArrayList<>(fwd.keySet())) {
                if (!uniquePaths.contains(src)) continue;
                for (String tgt : fwd.getOrDefault(src, Collections.emptySet())) {
                    if (!uniquePaths.contains(tgt)) {
                        issues.get("MEDIUM").add("Missing dependency in tree: " + src + " -> " + tgt);
                    }
                }
            }

            // Surface core analyzer findings for missing/invalid files
            try {
                Set<String> missingFiles = new LinkedHashSet<>(graph.getMissingFiles());
                for (String missing : missingFiles) {
                    issues.get("CRITICAL").add("Missing referenced file: " + missing);
                }
            } catch (Exception ignored) { /* defensive: underlying graph may not support */ }

            try {
                Set<String> invalidFiles = new LinkedHashSet<>(graph.getInvalidYamlFiles());
                for (String invalid : invalidFiles) {
                    issues.get("HIGH").add("Invalid YAML file: " + invalid);
                }
            } catch (Exception ignored) { /* defensive */ }
        }

        // Summaries
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("nodeCount", nodeCount[0]);
        stats.put("uniquePaths", uniquePaths.size());
        stats.put("duplicatePathCount", duplicatePaths.size());
        stats.put("maxDepthObserved", maxDepthObserved[0]);

        boolean hasCritical = !issues.get("CRITICAL").isEmpty();
        report.put("status", hasCritical ? "error" : "ok");
        report.put("issues", issues);
        report.put("stats", stats);
        return report;
    }
}

