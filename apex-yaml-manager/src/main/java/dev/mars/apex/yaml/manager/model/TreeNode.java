package dev.mars.apex.yaml.manager.model;

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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in a hierarchical tree structure for dependency visualization.
 * Uses the nested children format (D3 Hierarchy standard) for compatibility with
 * visualization libraries and frameworks.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TreeNode {
    private String name;
    private String id;
    private String path;
    private String type;
    private int depth;
    private int height;
    private int childCount;
    private boolean isCircular;
    private String circularReference;
    private List<TreeNode> children;
    private YamlContentSummary contentSummary;
    private List<String> dependencies;
    private List<String> dependents;
    private List<String> allDependencies;
    private int healthScore;
    private String author;
    private String created;
    private String lastModified;
    private String version;
    private List<String> circularDependencies;

    /**
     * Default constructor.
     */
    public TreeNode() {
        this.children = new ArrayList<>();
        this.depth = 0;
        this.height = 0;
        this.childCount = 0;
        this.isCircular = false;
    }

    /**
     * Constructor with name.
     */
    public TreeNode(String name) {
        this();
        this.name = name;
        this.id = name;
    }

    /**
     * Constructor with name and depth.
     */
    public TreeNode(String name, int depth) {
        this(name);
        this.depth = depth;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getChildCount() {
        return childCount;
    }

    public void setChildCount(int childCount) {
        this.childCount = childCount;
    }

    public boolean isCircular() {
        return isCircular;
    }

    public void setCircular(boolean circular) {
        isCircular = circular;
    }

    public String getCircularReference() {
        return circularReference;
    }

    public void setCircularReference(String circularReference) {
        this.circularReference = circularReference;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<TreeNode> children) {
        this.children = children;
        this.childCount = children != null ? children.size() : 0;
    }

    public YamlContentSummary getContentSummary() {
        return contentSummary;
    }

    public void setContentSummary(YamlContentSummary contentSummary) {
        this.contentSummary = contentSummary;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public List<String> getDependents() {
        return dependents;
    }

    public void setDependents(List<String> dependents) {
        this.dependents = dependents;
    }

    public List<String> getAllDependencies() {
        return allDependencies;
    }

    public void setAllDependencies(List<String> allDependencies) {
        this.allDependencies = allDependencies;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(int healthScore) {
        this.healthScore = healthScore;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getCircularDependencies() {
        return circularDependencies;
    }

    public void setCircularDependencies(List<String> circularDependencies) {
        this.circularDependencies = circularDependencies;
    }

    /**
     * Add a child node to this node.
     */
    public void addChild(TreeNode child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
        this.childCount = this.children.size();
    }

    /**
     * Calculate the height of this node (greatest distance to any leaf).
     * Height is 0 for leaf nodes.
     */
    public void calculateHeight() {
        if (children == null || children.isEmpty()) {
            this.height = 0;
        } else {
            int maxChildHeight = 0;
            for (TreeNode child : children) {
                child.calculateHeight();
                maxChildHeight = Math.max(maxChildHeight, child.getHeight());
            }
            this.height = maxChildHeight + 1;
        }
    }

    /**
     * Get total number of descendants (including self).
     */
    public int getDescendantCount() {
        int count = 1; // Count self
        if (children != null) {
            for (TreeNode child : children) {
                count += child.getDescendantCount();
            }
        }
        return count;
    }

    /**
     * Get maximum depth of this subtree.
     */
    public int getMaxDepth() {
        if (children == null || children.isEmpty()) {
            return this.depth;
        }
        int maxDepth = this.depth;
        for (TreeNode child : children) {
            maxDepth = Math.max(maxDepth, child.getMaxDepth());
        }
        return maxDepth;
    }

    @Override
    public String toString() {
        return "TreeNode{" +
                "name='" + name + '\'' +
                ", depth=" + depth +
                ", height=" + height +
                ", childCount=" + childCount +
                ", isCircular=" + isCircular +
                '}';
    }
}

