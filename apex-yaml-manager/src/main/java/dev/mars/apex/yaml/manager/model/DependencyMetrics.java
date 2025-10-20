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

import java.util.HashSet;
import java.util.Set;

/**
 * Metrics for YAML dependency analysis.
 *
 * Provides comprehensive metrics about the dependency graph including:
 * - Graph size and depth metrics
 * - Complexity scoring
 * - Circular dependency detection
 * - Orphaned and critical file identification
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
public class DependencyMetrics {

    private int totalFiles;
    private int maxDepth;
    private double averageDepth;
    private int totalDependencies;
    private int totalDependents;
    private int complexityScore; // 0-100
    private Set<String> circularDependencies;
    private Set<String> orphanedFiles;
    private Set<String> criticalFiles;
    private int warningCount;
    private int errorCount;

    public DependencyMetrics() {
        this.circularDependencies = new HashSet<>();
        this.orphanedFiles = new HashSet<>();
        this.criticalFiles = new HashSet<>();
        this.warningCount = 0;
        this.errorCount = 0;
    }

    // Getters and Setters

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public double getAverageDepth() {
        return averageDepth;
    }

    public void setAverageDepth(double averageDepth) {
        this.averageDepth = averageDepth;
    }

    public int getTotalDependencies() {
        return totalDependencies;
    }

    public void setTotalDependencies(int totalDependencies) {
        this.totalDependencies = totalDependencies;
    }

    public int getTotalDependents() {
        return totalDependents;
    }

    public void setTotalDependents(int totalDependents) {
        this.totalDependents = totalDependents;
    }

    public int getComplexityScore() {
        return complexityScore;
    }

    public void setComplexityScore(int complexityScore) {
        this.complexityScore = Math.max(0, Math.min(100, complexityScore));
    }

    public Set<String> getCircularDependencies() {
        return circularDependencies;
    }

    public void setCircularDependencies(Set<String> circularDependencies) {
        this.circularDependencies = circularDependencies;
    }

    public Set<String> getOrphanedFiles() {
        return orphanedFiles;
    }

    public void setOrphanedFiles(Set<String> orphanedFiles) {
        this.orphanedFiles = orphanedFiles;
    }

    public Set<String> getCriticalFiles() {
        return criticalFiles;
    }

    public void setCriticalFiles(Set<String> criticalFiles) {
        this.criticalFiles = criticalFiles;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    // Utility methods

    public boolean hasCircularDependencies() {
        return !circularDependencies.isEmpty();
    }

    public boolean hasOrphanedFiles() {
        return !orphanedFiles.isEmpty();
    }

    public boolean hasCriticalFiles() {
        return !criticalFiles.isEmpty();
    }

    public String getComplexityLevel() {
        if (complexityScore <= 30) {
            return "SIMPLE";
        } else if (complexityScore <= 60) {
            return "MODERATE";
        } else if (complexityScore <= 80) {
            return "COMPLEX";
        } else {
            return "VERY_COMPLEX";
        }
    }

    @Override
    public String toString() {
        return "DependencyMetrics{" +
                "totalFiles=" + totalFiles +
                ", maxDepth=" + maxDepth +
                ", averageDepth=" + averageDepth +
                ", complexityScore=" + complexityScore +
                ", complexityLevel='" + getComplexityLevel() + '\'' +
                ", circularDependencies=" + circularDependencies.size() +
                ", orphanedFiles=" + orphanedFiles.size() +
                ", criticalFiles=" + criticalFiles.size() +
                ", warnings=" + warningCount +
                ", errors=" + errorCount +
                '}';
    }
}

