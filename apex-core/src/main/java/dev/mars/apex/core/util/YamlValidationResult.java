package dev.mars.apex.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of validating a single YAML file.
 * 
 * Contains validation status, errors, warnings, and metadata about the
 * validation process for a specific YAML file.
 * 
 * @author APEX Rules Engine Team
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

/**
 * Summary of validation results for multiple YAML files.
 */
class YamlValidationSummary {
    
    private final List<YamlValidationResult> results;
    private final long validationTimestamp;
    
    public YamlValidationSummary() {
        this.results = new ArrayList<>();
        this.validationTimestamp = System.currentTimeMillis();
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
