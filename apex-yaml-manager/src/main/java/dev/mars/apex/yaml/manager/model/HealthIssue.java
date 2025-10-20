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

/**
 * Represents a health issue detected in a YAML configuration.
 *
 * Captures information about configuration problems including:
 * - Issue type and description
 * - Severity level (CRITICAL, WARNING, INFO)
 * - Affected file and location
 * - Actionable recommendation for resolution
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
public class HealthIssue {

    private String id;
    private String type; // STRUCTURAL, REFERENCE, CONSISTENCY, PERFORMANCE, COMPLIANCE
    private String severity; // CRITICAL, WARNING, INFO
    private String description;
    private String affectedFile;
    private int lineNumber;
    private String recommendation;
    private int estimatedEffortMinutes;

    public HealthIssue() {
    }

    public HealthIssue(String type, String severity, String description, String affectedFile) {
        this.type = type;
        this.severity = severity;
        this.description = description;
        this.affectedFile = affectedFile;
        this.lineNumber = 0;
        this.estimatedEffortMinutes = 15;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAffectedFile() {
        return affectedFile;
    }

    public void setAffectedFile(String affectedFile) {
        this.affectedFile = affectedFile;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public int getEstimatedEffortMinutes() {
        return estimatedEffortMinutes;
    }

    public void setEstimatedEffortMinutes(int estimatedEffortMinutes) {
        this.estimatedEffortMinutes = estimatedEffortMinutes;
    }

    // Utility methods

    public boolean isCritical() {
        return "CRITICAL".equals(severity);
    }

    public boolean isWarning() {
        return "WARNING".equals(severity);
    }

    public boolean isInfo() {
        return "INFO".equals(severity);
    }

    public String getSeverityIcon() {
        return switch (severity) {
            case "CRITICAL" -> "🔴";
            case "WARNING" -> "🟡";
            case "INFO" -> "🔵";
            default -> "⚪";
        };
    }

    @Override
    public String toString() {
        return "HealthIssue{" +
                "type='" + type + '\'' +
                ", severity='" + severity + '\'' +
                ", description='" + description + '\'' +
                ", affectedFile='" + affectedFile + '\'' +
                ", lineNumber=" + lineNumber +
                ", estimatedEffortMinutes=" + estimatedEffortMinutes +
                '}';
    }
}

