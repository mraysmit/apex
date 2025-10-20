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
 * Represents a recommendation for fixing a validation issue or improving health.
 * 
 * Contains actionable steps and expected impact.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Recommendation {
    
    public enum Priority {
        CRITICAL,  // Must fix immediately
        HIGH,      // Should fix soon
        MEDIUM,    // Should fix when possible
        LOW        // Nice to have
    }
    
    public enum ImpactLevel {
        HIGH,      // Significant improvement (>10 points)
        MEDIUM,    // Moderate improvement (5-10 points)
        LOW        // Minor improvement (<5 points)
    }
    
    private String id;
    private String title;
    private String description;
    private Priority priority;
    private ImpactLevel expectedImpact;
    private int estimatedEffortMinutes;
    private String action;  // Specific action to take
    private String example;  // Example of how to fix
    private String relatedIssueCode;
    private int estimatedScoreImprovement;

    public Recommendation(String id, String title, String description, Priority priority) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.expectedImpact = ImpactLevel.MEDIUM;
        this.estimatedEffortMinutes = 15;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public ImpactLevel getExpectedImpact() {
        return expectedImpact;
    }

    public void setExpectedImpact(ImpactLevel expectedImpact) {
        this.expectedImpact = expectedImpact;
    }

    public int getEstimatedEffortMinutes() {
        return estimatedEffortMinutes;
    }

    public void setEstimatedEffortMinutes(int estimatedEffortMinutes) {
        this.estimatedEffortMinutes = estimatedEffortMinutes;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getRelatedIssueCode() {
        return relatedIssueCode;
    }

    public void setRelatedIssueCode(String relatedIssueCode) {
        this.relatedIssueCode = relatedIssueCode;
    }

    public int getEstimatedScoreImprovement() {
        return estimatedScoreImprovement;
    }

    public void setEstimatedScoreImprovement(int estimatedScoreImprovement) {
        this.estimatedScoreImprovement = estimatedScoreImprovement;
    }

    @Override
    public String toString() {
        return "Recommendation{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", priority=" + priority +
                ", expectedImpact=" + expectedImpact +
                '}';
    }
}

