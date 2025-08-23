package dev.mars.apex.playground.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response model for playground data processing operations.
 * 
 * Contains the results of processing source data with YAML rules,
 * including validation results, enrichment data, and performance metrics.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@Schema(description = "Response from processing data with YAML rules in the playground")
public class PlaygroundResponse {

    @JsonProperty("success")
    @Schema(description = "Whether the processing was successful", example = "true")
    private boolean success;

    @JsonProperty("message")
    @Schema(description = "Overall processing message", example = "Processing completed successfully")
    private String message;

    @JsonProperty("timestamp")
    @Schema(description = "Processing timestamp")
    private Instant timestamp;

    @JsonProperty("validation")
    @Schema(description = "Validation results")
    private ValidationResult validation;

    @JsonProperty("enrichment")
    @Schema(description = "Enrichment results")
    private EnrichmentResult enrichment;

    @JsonProperty("metrics")
    @Schema(description = "Performance metrics")
    private ProcessingMetrics metrics;

    @JsonProperty("errors")
    @Schema(description = "List of errors that occurred during processing")
    private List<String> errors;

    // Default constructor
    public PlaygroundResponse() {
        this.timestamp = Instant.now();
        this.errors = new ArrayList<>();
        this.validation = new ValidationResult();
        this.enrichment = new EnrichmentResult();
        this.metrics = new ProcessingMetrics();
    }

    // Constructor for success response
    public PlaygroundResponse(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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

    public ValidationResult getValidation() {
        return validation;
    }

    public void setValidation(ValidationResult validation) {
        this.validation = validation;
    }

    public EnrichmentResult getEnrichment() {
        return enrichment;
    }

    public void setEnrichment(EnrichmentResult enrichment) {
        this.enrichment = enrichment;
    }

    public ProcessingMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(ProcessingMetrics metrics) {
        this.metrics = metrics;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.success = false;
    }

    /**
     * Validation results from rule processing.
     */
    @Schema(description = "Validation results from rule processing")
    public static class ValidationResult {
        
        @JsonProperty("valid")
        @Schema(description = "Whether all validation rules passed", example = "true")
        private boolean valid = true;

        @JsonProperty("rulesExecuted")
        @Schema(description = "Number of validation rules executed", example = "3")
        private int rulesExecuted = 0;

        @JsonProperty("rulesPassed")
        @Schema(description = "Number of validation rules that passed", example = "3")
        private int rulesPassed = 0;

        @JsonProperty("rulesFailed")
        @Schema(description = "Number of validation rules that failed", example = "0")
        private int rulesFailed = 0;

        @JsonProperty("results")
        @Schema(description = "Detailed results for each rule")
        private List<RuleExecutionResult> results = new ArrayList<>();

        // Getters and setters
        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public int getRulesExecuted() {
            return rulesExecuted;
        }

        public void setRulesExecuted(int rulesExecuted) {
            this.rulesExecuted = rulesExecuted;
        }

        public int getRulesPassed() {
            return rulesPassed;
        }

        public void setRulesPassed(int rulesPassed) {
            this.rulesPassed = rulesPassed;
        }

        public int getRulesFailed() {
            return rulesFailed;
        }

        public void setRulesFailed(int rulesFailed) {
            this.rulesFailed = rulesFailed;
        }

        public List<RuleExecutionResult> getResults() {
            return results;
        }

        public void setResults(List<RuleExecutionResult> results) {
            this.results = results;
        }

        public void addResult(RuleExecutionResult result) {
            if (this.results == null) {
                this.results = new ArrayList<>();
            }
            this.results.add(result);
            this.rulesExecuted++;
            if (result.isPassed()) {
                this.rulesPassed++;
            } else {
                this.rulesFailed++;
                this.valid = false;
            }
        }
    }

    /**
     * Enrichment results from data processing.
     */
    @Schema(description = "Enrichment results from data processing")
    public static class EnrichmentResult {
        
        @JsonProperty("enriched")
        @Schema(description = "Whether data was enriched", example = "true")
        private boolean enriched = false;

        @JsonProperty("fieldsAdded")
        @Schema(description = "Number of fields added during enrichment", example = "2")
        private int fieldsAdded = 0;

        @JsonProperty("enrichedData")
        @Schema(description = "The enriched data")
        private Map<String, Object> enrichedData = new HashMap<>();

        @JsonProperty("enrichmentSources")
        @Schema(description = "Sources used for enrichment")
        private List<String> enrichmentSources = new ArrayList<>();

        // Getters and setters
        public boolean isEnriched() {
            return enriched;
        }

        public void setEnriched(boolean enriched) {
            this.enriched = enriched;
        }

        public int getFieldsAdded() {
            return fieldsAdded;
        }

        public void setFieldsAdded(int fieldsAdded) {
            this.fieldsAdded = fieldsAdded;
        }

        public Map<String, Object> getEnrichedData() {
            return enrichedData;
        }

        public void setEnrichedData(Map<String, Object> enrichedData) {
            this.enrichedData = enrichedData;
        }

        public List<String> getEnrichmentSources() {
            return enrichmentSources;
        }

        public void setEnrichmentSources(List<String> enrichmentSources) {
            this.enrichmentSources = enrichmentSources;
        }
    }

    /**
     * Performance metrics for processing.
     */
    @Schema(description = "Performance metrics for processing")
    public static class ProcessingMetrics {
        
        @JsonProperty("totalTimeMs")
        @Schema(description = "Total processing time in milliseconds", example = "150")
        private long totalTimeMs = 0;

        @JsonProperty("yamlParsingTimeMs")
        @Schema(description = "YAML parsing time in milliseconds", example = "25")
        private long yamlParsingTimeMs = 0;

        @JsonProperty("dataParsingTimeMs")
        @Schema(description = "Data parsing time in milliseconds", example = "15")
        private long dataParsingTimeMs = 0;

        @JsonProperty("rulesExecutionTimeMs")
        @Schema(description = "Rules execution time in milliseconds", example = "85")
        private long rulesExecutionTimeMs = 0;

        @JsonProperty("enrichmentTimeMs")
        @Schema(description = "Enrichment time in milliseconds", example = "25")
        private long enrichmentTimeMs = 0;

        // Getters and setters
        public long getTotalTimeMs() {
            return totalTimeMs;
        }

        public void setTotalTimeMs(long totalTimeMs) {
            this.totalTimeMs = totalTimeMs;
        }

        public long getYamlParsingTimeMs() {
            return yamlParsingTimeMs;
        }

        public void setYamlParsingTimeMs(long yamlParsingTimeMs) {
            this.yamlParsingTimeMs = yamlParsingTimeMs;
        }

        public long getDataParsingTimeMs() {
            return dataParsingTimeMs;
        }

        public void setDataParsingTimeMs(long dataParsingTimeMs) {
            this.dataParsingTimeMs = dataParsingTimeMs;
        }

        public long getRulesExecutionTimeMs() {
            return rulesExecutionTimeMs;
        }

        public void setRulesExecutionTimeMs(long rulesExecutionTimeMs) {
            this.rulesExecutionTimeMs = rulesExecutionTimeMs;
        }

        public long getEnrichmentTimeMs() {
            return enrichmentTimeMs;
        }

        public void setEnrichmentTimeMs(long enrichmentTimeMs) {
            this.enrichmentTimeMs = enrichmentTimeMs;
        }
    }

    @Override
    public String toString() {
        return "PlaygroundResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", errorsCount=" + (errors != null ? errors.size() : 0) +
                '}';
    }
}
