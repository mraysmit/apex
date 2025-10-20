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
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single validation issue found during validation checks.
 *
 * Contains issue details, severity level, and recommendations for resolution.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ValidationIssue {

    public enum Severity {
        @JsonProperty("ERROR")
        ERROR,
        @JsonProperty("WARNING")
        WARNING,
        @JsonProperty("INFO")
        INFO
    }

    public enum Category {
        @JsonProperty("STRUCTURAL")
        STRUCTURAL,      // YAML syntax, required fields
        @JsonProperty("REFERENCE")
        REFERENCE,       // Missing files, broken links
        @JsonProperty("CONSISTENCY")
        CONSISTENCY,     // Unique IDs, naming conventions
        @JsonProperty("PERFORMANCE")
        PERFORMANCE,     // Deep chains, complexity
        @JsonProperty("COMPLIANCE")
        COMPLIANCE       // Required metadata, standards
    }
    
    private String code;
    private String message;
    private Severity severity;
    private Category category;
    private String location;  // e.g., "metadata.id" or "rule-groups[0]"
    private String recommendation;
    private String affectedElement;

    // Default constructor for Jackson deserialization
    public ValidationIssue() {
    }

    public ValidationIssue(String code, String message, Severity severity, Category category) {
        this.code = code;
        this.message = message;
        this.severity = severity;
        this.category = category;
    }

    public ValidationIssue(String code, String message, Severity severity, Category category, 
                          String location, String recommendation) {
        this.code = code;
        this.message = message;
        this.severity = severity;
        this.category = category;
        this.location = location;
        this.recommendation = recommendation;
    }

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getAffectedElement() {
        return affectedElement;
    }

    public void setAffectedElement(String affectedElement) {
        this.affectedElement = affectedElement;
    }

    @Override
    public String toString() {
        return "ValidationIssue{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", severity=" + severity +
                ", category=" + category +
                ", location='" + location + '\'' +
                '}';
    }
}

