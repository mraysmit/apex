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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive health report for YAML configurations.
 *
 * Provides detailed assessment of configuration quality including:
 * - Overall health score (0-100)
 * - Component health metrics
 * - Identified issues with severity levels
 * - Actionable recommendations
 * - Estimated effort to fix issues
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
public class HealthReport {

    private int overallScore; // 0-100
    private int structuralHealth; // 0-100
    private int referenceHealth; // 0-100
    private int consistencyHealth; // 0-100
    private int performanceHealth; // 0-100
    private int complianceHealth; // 0-100
    private List<HealthIssue> issues;
    private List<String> recommendations;
    private LocalDateTime generatedAt;
    private int estimatedEffortHours;
    private int filesAnalyzed;
    private int filesWithIssues;

    public HealthReport() {
        this.issues = new ArrayList<>();
        this.recommendations = new ArrayList<>();
        this.generatedAt = LocalDateTime.now();
        this.overallScore = 50;
        this.structuralHealth = 50;
        this.referenceHealth = 50;
        this.consistencyHealth = 50;
        this.performanceHealth = 50;
        this.complianceHealth = 50;
    }

    // Getters and Setters

    public int getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(int overallScore) {
        this.overallScore = Math.max(0, Math.min(100, overallScore));
    }

    public int getStructuralHealth() {
        return structuralHealth;
    }

    public void setStructuralHealth(int structuralHealth) {
        this.structuralHealth = Math.max(0, Math.min(100, structuralHealth));
    }

    public int getReferenceHealth() {
        return referenceHealth;
    }

    public void setReferenceHealth(int referenceHealth) {
        this.referenceHealth = Math.max(0, Math.min(100, referenceHealth));
    }

    public int getConsistencyHealth() {
        return consistencyHealth;
    }

    public void setConsistencyHealth(int consistencyHealth) {
        this.consistencyHealth = Math.max(0, Math.min(100, consistencyHealth));
    }

    public int getPerformanceHealth() {
        return performanceHealth;
    }

    public void setPerformanceHealth(int performanceHealth) {
        this.performanceHealth = Math.max(0, Math.min(100, performanceHealth));
    }

    public int getComplianceHealth() {
        return complianceHealth;
    }

    public void setComplianceHealth(int complianceHealth) {
        this.complianceHealth = Math.max(0, Math.min(100, complianceHealth));
    }

    public List<HealthIssue> getIssues() {
        return issues;
    }

    public void addIssue(HealthIssue issue) {
        this.issues.add(issue);
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getEstimatedEffortHours() {
        return estimatedEffortHours;
    }

    public void setEstimatedEffortHours(int estimatedEffortHours) {
        this.estimatedEffortHours = estimatedEffortHours;
    }

    public int getFilesAnalyzed() {
        return filesAnalyzed;
    }

    public void setFilesAnalyzed(int filesAnalyzed) {
        this.filesAnalyzed = filesAnalyzed;
    }

    public int getFilesWithIssues() {
        return filesWithIssues;
    }

    public void setFilesWithIssues(int filesWithIssues) {
        this.filesWithIssues = filesWithIssues;
    }

    // Utility methods

    public String getHealthLevel() {
        if (overallScore >= 80) {
            return "EXCELLENT";
        } else if (overallScore >= 60) {
            return "GOOD";
        } else if (overallScore >= 40) {
            return "FAIR";
        } else {
            return "POOR";
        }
    }

    public int getCriticalIssueCount() {
        return (int) issues.stream()
                .filter(issue -> "CRITICAL".equals(issue.getSeverity()))
                .count();
    }

    public int getWarningIssueCount() {
        return (int) issues.stream()
                .filter(issue -> "WARNING".equals(issue.getSeverity()))
                .count();
    }

    public int getInfoIssueCount() {
        return (int) issues.stream()
                .filter(issue -> "INFO".equals(issue.getSeverity()))
                .count();
    }

    public double getIssueResolutionRate() {
        if (filesAnalyzed == 0) {
            return 0.0;
        }
        return ((double) (filesAnalyzed - filesWithIssues) / filesAnalyzed) * 100;
    }

    @Override
    public String toString() {
        return "HealthReport{" +
                "overallScore=" + overallScore +
                ", healthLevel='" + getHealthLevel() + '\'' +
                ", criticalIssues=" + getCriticalIssueCount() +
                ", warnings=" + getWarningIssueCount() +
                ", filesAnalyzed=" + filesAnalyzed +
                ", filesWithIssues=" + filesWithIssues +
                ", estimatedEffortHours=" + estimatedEffortHours +
                '}';
    }
}

