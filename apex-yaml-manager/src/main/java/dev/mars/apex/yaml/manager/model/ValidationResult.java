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
import java.util.*;

/**
 * Represents the result of a validation check on a YAML file.
 * 
 * Contains validation status, issues found, and recommendations for fixes.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ValidationResult {
    
    private String filePath;
    private boolean valid;
    private int totalIssues;
    private int errorCount;
    private int warningCount;
    private int infoCount;
    private List<ValidationIssue> issues;
    private long validationTimeMs;
    private String validationType; // STRUCTURAL, REFERENCE, CONSISTENCY, PERFORMANCE, COMPLIANCE

    public ValidationResult(String filePath) {
        this.filePath = filePath;
        this.valid = true;
        this.issues = new ArrayList<>();
        this.errorCount = 0;
        this.warningCount = 0;
        this.infoCount = 0;
    }

    public void addIssue(ValidationIssue issue) {
        this.issues.add(issue);
        this.totalIssues++;
        
        switch (issue.getSeverity()) {
            case ERROR:
                this.errorCount++;
                this.valid = false;
                break;
            case WARNING:
                this.warningCount++;
                break;
            case INFO:
                this.infoCount++;
                break;
        }
    }

    public void addIssues(List<ValidationIssue> newIssues) {
        newIssues.forEach(this::addIssue);
    }

    public boolean hasErrors() {
        return errorCount > 0;
    }

    public boolean hasWarnings() {
        return warningCount > 0;
    }

    public boolean hasIssues() {
        return totalIssues > 0;
    }

    // Getters and Setters
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getTotalIssues() {
        return totalIssues;
    }

    public void setTotalIssues(int totalIssues) {
        this.totalIssues = totalIssues;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public int getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(int infoCount) {
        this.infoCount = infoCount;
    }

    public List<ValidationIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<ValidationIssue> issues) {
        this.issues = issues;
    }

    public long getValidationTimeMs() {
        return validationTimeMs;
    }

    public void setValidationTimeMs(long validationTimeMs) {
        this.validationTimeMs = validationTimeMs;
    }

    public String getValidationType() {
        return validationType;
    }

    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "filePath='" + filePath + '\'' +
                ", valid=" + valid +
                ", totalIssues=" + totalIssues +
                ", errorCount=" + errorCount +
                ", warningCount=" + warningCount +
                ", infoCount=" + infoCount +
                ", validationType='" + validationType + '\'' +
                '}';
    }
}

