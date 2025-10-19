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
 * Result of impact analysis for a YAML configuration file.
 *
 * Provides detailed information about what would be affected by changes to a file:
 * - Direct and transitive dependents
 * - Direct and transitive dependencies
 * - Impact radius and score
 * - Critical paths
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
public class ImpactAnalysisResult {

    private String analyzedFile;
    private Set<String> directDependents;
    private Set<String> transitiveDependents;
    private Set<String> directDependencies;
    private Set<String> transitiveDependencies;
    private int impactRadius; // Number of affected files
    private int impactScore; // 0-100, higher = more critical
    private Set<String> criticalPaths;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private String recommendation;

    public ImpactAnalysisResult(String analyzedFile) {
        this.analyzedFile = analyzedFile;
        this.directDependents = new HashSet<>();
        this.transitiveDependents = new HashSet<>();
        this.directDependencies = new HashSet<>();
        this.transitiveDependencies = new HashSet<>();
        this.criticalPaths = new HashSet<>();
        this.riskLevel = "LOW";
        this.recommendation = "";
    }

    // Getters and Setters

    public String getAnalyzedFile() {
        return analyzedFile;
    }

    public Set<String> getDirectDependents() {
        return directDependents;
    }

    public void setDirectDependents(Set<String> directDependents) {
        this.directDependents = directDependents;
    }

    public Set<String> getTransitiveDependents() {
        return transitiveDependents;
    }

    public void setTransitiveDependents(Set<String> transitiveDependents) {
        this.transitiveDependents = transitiveDependents;
    }

    public Set<String> getDirectDependencies() {
        return directDependencies;
    }

    public void setDirectDependencies(Set<String> directDependencies) {
        this.directDependencies = directDependencies;
    }

    public Set<String> getTransitiveDependencies() {
        return transitiveDependencies;
    }

    public void setTransitiveDependencies(Set<String> transitiveDependencies) {
        this.transitiveDependencies = transitiveDependencies;
    }

    public int getImpactRadius() {
        return impactRadius;
    }

    public void setImpactRadius(int impactRadius) {
        this.impactRadius = impactRadius;
    }

    public int getImpactScore() {
        return impactScore;
    }

    public void setImpactScore(int impactScore) {
        this.impactScore = Math.max(0, Math.min(100, impactScore));
    }

    public Set<String> getCriticalPaths() {
        return criticalPaths;
    }

    public void setCriticalPaths(Set<String> criticalPaths) {
        this.criticalPaths = criticalPaths;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    // Utility methods

    public int getTotalAffectedFiles() {
        return transitiveDependents.size();
    }

    public boolean isCritical() {
        return "CRITICAL".equals(riskLevel);
    }

    public boolean isHighRisk() {
        return "HIGH".equals(riskLevel) || isCritical();
    }

    public void calculateRiskLevel() {
        if (impactScore >= 80) {
            this.riskLevel = "CRITICAL";
            this.recommendation = "Extensive testing required. Coordinate changes with all dependent teams.";
        } else if (impactScore >= 60) {
            this.riskLevel = "HIGH";
            this.recommendation = "Thorough testing required. Notify dependent teams of changes.";
        } else if (impactScore >= 30) {
            this.riskLevel = "MEDIUM";
            this.recommendation = "Standard testing required. Review dependent configurations.";
        } else {
            this.riskLevel = "LOW";
            this.recommendation = "Safe to change. Minimal impact on other configurations.";
        }
    }

    @Override
    public String toString() {
        return "ImpactAnalysisResult{" +
                "analyzedFile='" + analyzedFile + '\'' +
                ", directDependents=" + directDependents.size() +
                ", transitiveDependents=" + transitiveDependents.size() +
                ", impactRadius=" + impactRadius +
                ", impactScore=" + impactScore +
                ", riskLevel='" + riskLevel + '\'' +
                '}';
    }
}

