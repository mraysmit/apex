package dev.mars.apex.playground.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Response model for YAML validation operations.
 * 
 * Contains the results of validating YAML configuration syntax and structure,
 * including any errors, warnings, and validation metadata.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@Schema(description = "Response from YAML validation operations")
public class YamlValidationResponse {

    @JsonProperty("valid")
    @Schema(description = "Whether the YAML is valid", example = "true")
    private boolean valid;

    @JsonProperty("message")
    @Schema(description = "Overall validation message", example = "YAML configuration is valid")
    private String message;

    @JsonProperty("timestamp")
    @Schema(description = "Validation timestamp")
    private Instant timestamp;

    @JsonProperty("errors")
    @Schema(description = "List of validation errors")
    private List<ValidationIssue> errors;

    @JsonProperty("warnings")
    @Schema(description = "List of validation warnings")
    private List<ValidationIssue> warnings;

    @JsonProperty("metadata")
    @Schema(description = "Metadata information from the YAML")
    private YamlMetadata metadata;

    @JsonProperty("statistics")
    @Schema(description = "Validation statistics")
    private ValidationStatistics statistics;

    // Default constructor
    public YamlValidationResponse() {
        this.valid = true; // Default to valid, set to false if issues found
        this.timestamp = Instant.now();
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.statistics = new ValidationStatistics();
    }

    // Constructor for simple validation result
    public YamlValidationResponse(boolean valid, String message) {
        this();
        this.valid = valid;
        this.message = message;
    }

    // Getters and setters
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<ValidationIssue> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationIssue> errors) {
        this.errors = errors;
    }

    public List<ValidationIssue> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<ValidationIssue> warnings) {
        this.warnings = warnings;
    }

    public YamlMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(YamlMetadata metadata) {
        this.metadata = metadata;
    }

    public ValidationStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(ValidationStatistics statistics) {
        this.statistics = statistics;
    }

    public void addError(String message, int line, int column) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(new ValidationIssue("ERROR", message, line, column));
        this.valid = false;
        this.statistics.errorCount++;
    }

    public void addWarning(String message, int line, int column) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(new ValidationIssue("WARNING", message, line, column));
        this.statistics.warningCount++;
    }

    /**
     * Represents a validation issue (error or warning).
     */
    @Schema(description = "A validation issue with location information")
    public static class ValidationIssue {
        
        @JsonProperty("type")
        @Schema(description = "Type of issue", allowableValues = {"ERROR", "WARNING"})
        private String type;

        @JsonProperty("message")
        @Schema(description = "Description of the issue")
        private String message;

        @JsonProperty("line")
        @Schema(description = "Line number where the issue occurs")
        private int line;

        @JsonProperty("column")
        @Schema(description = "Column number where the issue occurs")
        private int column;

        public ValidationIssue() {}

        public ValidationIssue(String type, String message, int line, int column) {
            this.type = type;
            this.message = message;
            this.line = line;
            this.column = column;
        }

        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getLine() { return line; }
        public void setLine(int line) { this.line = line; }
        public int getColumn() { return column; }
        public void setColumn(int column) { this.column = column; }
    }

    /**
     * YAML metadata information.
     */
    @Schema(description = "Metadata information extracted from YAML")
    public static class YamlMetadata {
        
        @JsonProperty("name")
        private String name;

        @JsonProperty("version")
        private String version;

        @JsonProperty("description")
        private String description;

        @JsonProperty("type")
        private String type;

        @JsonProperty("author")
        private String author;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
    }

    /**
     * Validation statistics.
     */
    @Schema(description = "Statistics about the validation process")
    public static class ValidationStatistics {
        
        @JsonProperty("errorCount")
        private int errorCount = 0;

        @JsonProperty("warningCount")
        private int warningCount = 0;

        @JsonProperty("rulesCount")
        private int rulesCount = 0;

        @JsonProperty("enrichmentsCount")
        private int enrichmentsCount = 0;

        // Getters and setters
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        public int getWarningCount() { return warningCount; }
        public void setWarningCount(int warningCount) { this.warningCount = warningCount; }
        public int getRulesCount() { return rulesCount; }
        public void setRulesCount(int rulesCount) { this.rulesCount = rulesCount; }
        public int getEnrichmentsCount() { return enrichmentsCount; }
        public void setEnrichmentsCount(int enrichmentsCount) { this.enrichmentsCount = enrichmentsCount; }
    }

    @Override
    public String toString() {
        return "YamlValidationResponse{" +
                "valid=" + valid +
                ", message='" + message + '\'' +
                ", errorCount=" + (errors != null ? errors.size() : 0) +
                ", warningCount=" + (warnings != null ? warnings.size() : 0) +
                '}';
    }
}
