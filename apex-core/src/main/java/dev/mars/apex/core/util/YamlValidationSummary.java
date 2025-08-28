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
 * Summary of validation results for multiple YAML files.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public class YamlValidationSummary {
    
    private final List<YamlValidationResult> results;
    
    public YamlValidationSummary() {
        this.results = new ArrayList<>();
    }
    
    /**
     * Adds a validation result to the summary.
     */
    public void addResult(YamlValidationResult result) {
        results.add(result);
    }
    
    /**
     * Gets the number of valid files.
     */
    public int getValidCount() {
        return (int) results.stream().filter(YamlValidationResult::isValid).count();
    }
    
    /**
     * Gets the number of invalid files.
     */
    public int getInvalidCount() {
        return (int) results.stream().filter(result -> !result.isValid()).count();
    }
    
    /**
     * Gets the number of files with warnings.
     */
    public int getWarningCount() {
        return (int) results.stream().filter(YamlValidationResult::hasWarnings).count();
    }
    
    /**
     * Gets the total number of files validated.
     */
    public int getTotalCount() {
        return results.size();
    }
    
    /**
     * Gets all validation results.
     */
    public List<YamlValidationResult> getResults() {
        return new ArrayList<>(results);
    }
    
    /**
     * Gets only the invalid results.
     */
    public List<YamlValidationResult> getInvalidResults() {
        return results.stream()
                .filter(result -> !result.isValid())
                .toList();
    }
    
    /**
     * Gets only the results with warnings.
     */
    public List<YamlValidationResult> getResultsWithWarnings() {
        return results.stream()
                .filter(YamlValidationResult::hasWarnings)
                .toList();
    }
    
    /**
     * Checks if all files are valid.
     */
    public boolean isAllValid() {
        return getInvalidCount() == 0;
    }
    
    /**
     * Gets a comprehensive summary report.
     */
    public String getReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("YAML Validation Summary Report\n");
        report.append("==============================\n\n");
        
        report.append("Total Files: ").append(getTotalCount()).append("\n");
        report.append("Valid Files: ").append(getValidCount()).append("\n");
        report.append("Invalid Files: ").append(getInvalidCount()).append("\n");
        report.append("Files with Warnings: ").append(getWarningCount()).append("\n");
        report.append("Overall Status: ").append(isAllValid() ? "PASS" : "FAIL").append("\n\n");
        
        // Show invalid files
        List<YamlValidationResult> invalidResults = getInvalidResults();
        if (!invalidResults.isEmpty()) {
            report.append("Invalid Files:\n");
            report.append("--------------\n");
            for (YamlValidationResult result : invalidResults) {
                report.append(result.getSummary()).append("\n");
            }
        }
        
        // Show files with warnings
        List<YamlValidationResult> warningResults = getResultsWithWarnings();
        if (!warningResults.isEmpty()) {
            report.append("Files with Warnings:\n");
            report.append("--------------------\n");
            for (YamlValidationResult result : warningResults) {
                if (result.isValid()) { // Only show warnings for valid files (invalid files already shown above)
                    report.append(result.getSummary()).append("\n");
                }
            }
        }
        
        return report.toString();
    }
    
    @Override
    public String toString() {
        return "YamlValidationSummary{" +
                "totalFiles=" + getTotalCount() +
                ", validFiles=" + getValidCount() +
                ", invalidFiles=" + getInvalidCount() +
                ", filesWithWarnings=" + getWarningCount() +
                ", overallStatus=" + (isAllValid() ? "PASS" : "FAIL") +
                '}';
    }
}
