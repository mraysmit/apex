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

/**
 * Represents a health score for a YAML configuration file.
 * 
 * Scores range from 0-100 with component-based breakdown.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HealthScore {
    
    public enum Grade {
        EXCELLENT(90, 100),
        GOOD(75, 89),
        FAIR(60, 74),
        POOR(40, 59),
        CRITICAL(0, 39);
        
        private final int minScore;
        private final int maxScore;
        
        Grade(int minScore, int maxScore) {
            this.minScore = minScore;
            this.maxScore = maxScore;
        }
        
        public static Grade fromScore(int score) {
            for (Grade grade : Grade.values()) {
                if (score >= grade.minScore && score <= grade.maxScore) {
                    return grade;
                }
            }
            return CRITICAL;
        }
    }
    
    private String filePath;
    private int overallScore;
    private Grade grade;
    private int structuralScore;
    private int referenceScore;
    private int consistencyScore;
    private int performanceScore;
    private int complianceScore;
    private int metadataScore;
    private String trend;  // IMPROVING, STABLE, DECLINING
    private long lastCheckedTime;

    public HealthScore(String filePath) {
        this.filePath = filePath;
        this.overallScore = 100;
        this.structuralScore = 100;
        this.referenceScore = 100;
        this.consistencyScore = 100;
        this.performanceScore = 100;
        this.complianceScore = 100;
        this.metadataScore = 100;
        this.trend = "STABLE";
        this.lastCheckedTime = System.currentTimeMillis();
    }

    public void calculateOverallScore() {
        // Weighted average: structural (25%), reference (25%), consistency (15%), 
        // performance (15%), compliance (10%), metadata (10%)
        this.overallScore = (int) (
            (structuralScore * 0.25) +
            (referenceScore * 0.25) +
            (consistencyScore * 0.15) +
            (performanceScore * 0.15) +
            (complianceScore * 0.10) +
            (metadataScore * 0.10)
        );
        this.grade = Grade.fromScore(this.overallScore);
    }

    public boolean isHealthy() {
        return overallScore >= 75;
    }

    public boolean hasCriticalIssues() {
        return overallScore < 40;
    }

    // Getters and Setters
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(int overallScore) {
        this.overallScore = overallScore;
    }

    public Grade getGrade() {
        return grade;
    }

    public void setGrade(Grade grade) {
        this.grade = grade;
    }

    public int getStructuralScore() {
        return structuralScore;
    }

    public void setStructuralScore(int structuralScore) {
        this.structuralScore = structuralScore;
    }

    public int getReferenceScore() {
        return referenceScore;
    }

    public void setReferenceScore(int referenceScore) {
        this.referenceScore = referenceScore;
    }

    public int getConsistencyScore() {
        return consistencyScore;
    }

    public void setConsistencyScore(int consistencyScore) {
        this.consistencyScore = consistencyScore;
    }

    public int getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(int performanceScore) {
        this.performanceScore = performanceScore;
    }

    public int getComplianceScore() {
        return complianceScore;
    }

    public void setComplianceScore(int complianceScore) {
        this.complianceScore = complianceScore;
    }

    public int getMetadataScore() {
        return metadataScore;
    }

    public void setMetadataScore(int metadataScore) {
        this.metadataScore = metadataScore;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public long getLastCheckedTime() {
        return lastCheckedTime;
    }

    public void setLastCheckedTime(long lastCheckedTime) {
        this.lastCheckedTime = lastCheckedTime;
    }

    @Override
    public String toString() {
        return "HealthScore{" +
                "filePath='" + filePath + '\'' +
                ", overallScore=" + overallScore +
                ", grade=" + grade +
                ", trend='" + trend + '\'' +
                '}';
    }
}

