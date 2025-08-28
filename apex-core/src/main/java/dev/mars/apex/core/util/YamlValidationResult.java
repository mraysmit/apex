package dev.mars.apex.core.util;

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


import java.util.ArrayList;
import java.util.List;

/**
 * Result of validating a single YAML file.
 * 
 * Contains validation status, errors, warnings, and metadata about the
 * validation process for a specific YAML file.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class YamlValidationResult {
    
    private final String filePath;
    private final List<String> errors;
    private final List<String> warnings;
    private final long validationTimestamp;
    
    public YamlValidationResult(String filePath) {
        this.filePath = filePath;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.validationTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Adds an error to the validation result.
     */
    public void addError(String error) {
        errors.add(error);
    }
    
    /**
     * Adds a warning to the validation result.
     */
    public void addWarning(String warning) {
        warnings.add(warning);
    }
    
    /**
     * Checks if the validation passed (no errors).
     */
    public boolean isValid() {
        return errors.isEmpty();
    }
    
    /**
     * Checks if there are any warnings.
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    /**
     * Gets the validation status as a string.
     */
    public String getStatus() {
        if (!isValid()) {
            return "INVALID";
        } else if (hasWarnings()) {
            return "VALID_WITH_WARNINGS";
        } else {
            return "VALID";
        }
    }
    
    /**
     * Gets a summary of the validation result.
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("File: ").append(filePath).append("\n");
        summary.append("Status: ").append(getStatus()).append("\n");
        summary.append("Errors: ").append(errors.size()).append("\n");
        summary.append("Warnings: ").append(warnings.size()).append("\n");
        
        if (!errors.isEmpty()) {
            summary.append("\nErrors:\n");
            for (String error : errors) {
                summary.append("  - ").append(error).append("\n");
            }
        }
        
        if (!warnings.isEmpty()) {
            summary.append("\nWarnings:\n");
            for (String warning : warnings) {
                summary.append("  - ").append(warning).append("\n");
            }
        }
        
        return summary.toString();
    }
    
    // Getters
    public String getFilePath() {
        return filePath;
    }
    
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    public int getErrorCount() {
        return errors.size();
    }
    
    public int getWarningCount() {
        return warnings.size();
    }
    
    public long getValidationTimestamp() {
        return validationTimestamp;
    }
    
    @Override
    public String toString() {
        return "YamlValidationResult{" +
                "filePath='" + filePath + '\'' +
                ", status='" + getStatus() + '\'' +
                ", errors=" + errors.size() +
                ", warnings=" + warnings.size() +
                '}';
    }
}


