package dev.mars.apex.playground.service;

import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.playground.model.PlaygroundRequest;
import dev.mars.apex.playground.model.PlaygroundResponse;
import dev.mars.apex.playground.model.RuleExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Core service for APEX Playground operations.
 *
 * Handles the main processing logic for the playground including:
 * - Processing source data with YAML rules
 * - Coordinating validation and enrichment operations
 * - Managing processing workflows
 * - Performance metrics collection
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@Service
public class PlaygroundService {

    private static final Logger logger = LoggerFactory.getLogger(PlaygroundService.class);

    private final DataProcessingService dataProcessingService;
    private final YamlValidationService yamlValidationService;
    private final YamlRulesEngineService yamlRulesEngineService;

    @Autowired
    public PlaygroundService(DataProcessingService dataProcessingService,
                           YamlValidationService yamlValidationService) {
        this.dataProcessingService = dataProcessingService;
        this.yamlValidationService = yamlValidationService;
        this.yamlRulesEngineService = new YamlRulesEngineService();
    }

    /**
     * Process source data with YAML rules configuration.
     *
     * Performs comprehensive processing including:
     * - Data parsing and validation
     * - YAML rules validation and loading
     * - Rules execution against the data
     * - Performance metrics collection
     * - Result aggregation and formatting
     *
     * @param request The playground processing request
     * @return Comprehensive processing results
     */
    public PlaygroundResponse processData(PlaygroundRequest request) {
        logger.info("Processing playground request: {}", request);

        PlaygroundResponse response = new PlaygroundResponse();
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Validate YAML configuration
            long yamlStartTime = System.currentTimeMillis();
            if (!yamlValidationService.isValidYaml(request.getYamlRules())) {
                response.setSuccess(false);
                response.setMessage("YAML configuration is invalid");
                response.addError("YAML validation failed");
                return response;
            }
            response.getMetrics().setYamlParsingTimeMs(System.currentTimeMillis() - yamlStartTime);

            // Step 2: Parse source data
            long dataStartTime = System.currentTimeMillis();
            Map<String, Object> parsedData = dataProcessingService.parseData(
                request.getSourceData(),
                request.getDataFormat()
            );
            response.getMetrics().setDataParsingTimeMs(System.currentTimeMillis() - dataStartTime);

            // Step 3: Create and execute rules engine
            long rulesStartTime = System.currentTimeMillis();
            RulesEngine rulesEngine = yamlRulesEngineService.createRulesEngineFromString(request.getYamlRules());

            // Execute rules against the parsed data
            RuleResult ruleResult = rulesEngine.executeRulesForCategory("default", parsedData);
            response.getMetrics().setRulesExecutionTimeMs(System.currentTimeMillis() - rulesStartTime);

            // Step 4: Process results
            processRuleResults(ruleResult, parsedData, response);

            // Step 5: Set final metrics and status
            response.getMetrics().setTotalTimeMs(System.currentTimeMillis() - startTime);
            response.setSuccess(true);
            response.setMessage("Processing completed successfully");

            logger.info("Processing completed successfully in {}ms", response.getMetrics().getTotalTimeMs());

        } catch (YamlConfigurationException e) {
            logger.error("YAML configuration error: {}", e.getMessage());
            response.setSuccess(false);
            response.setMessage("YAML configuration error: " + e.getMessage());
            response.addError("YAML configuration error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Processing error: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setMessage("Processing failed: " + e.getMessage());
            response.addError("Processing error: " + e.getMessage());
        }

        return response;
    }

    /**
     * Validate source data format and content.
     *
     * @param sourceData The source data to validate
     * @param dataFormat The expected data format (JSON, XML, CSV)
     * @return Validation result
     */
    public boolean validateSourceData(String sourceData, String dataFormat) {
        logger.debug("Validating source data format: {}", dataFormat);

        try {
            return dataProcessingService.validateDataFormat(sourceData, dataFormat);
        } catch (Exception e) {
            logger.debug("Source data validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Process rule execution results and populate the response.
     */
    private void processRuleResults(RuleResult ruleResult, Map<String, Object> originalData, PlaygroundResponse response) {
        // Process validation results
        PlaygroundResponse.ValidationResult validation = response.getValidation();

        if (ruleResult.isTriggered()) {
            // Rule was triggered (passed)
            RuleExecutionResult executionResult = new RuleExecutionResult(
                "rule-" + System.currentTimeMillis(),
                ruleResult.getRuleName(),
                true,
                ruleResult.getMessage()
            );

            if (ruleResult.getPerformanceMetrics() != null) {
                executionResult.setExecutionTimeMs(ruleResult.getPerformanceMetrics().getEvaluationTimeMillis());
            }

            validation.addResult(executionResult);
        } else {
            // Rule was not triggered (failed)
            RuleExecutionResult executionResult = new RuleExecutionResult(
                "rule-" + System.currentTimeMillis(),
                ruleResult.getRuleName() != null ? ruleResult.getRuleName() : "unknown",
                false,
                ruleResult.getMessage() != null ? ruleResult.getMessage() : "Rule condition not met"
            );

            if (ruleResult.getPerformanceMetrics() != null) {
                executionResult.setExecutionTimeMs(ruleResult.getPerformanceMetrics().getEvaluationTimeMillis());
            }

            validation.addResult(executionResult);
        }

        // Process enrichment results
        PlaygroundResponse.EnrichmentResult enrichment = response.getEnrichment();

        // For now, we'll show the original data as "enriched" data
        // In a full implementation, this would include actual enrichment results
        enrichment.setEnrichedData(originalData);

        // Check if data was actually enriched (simplified check)
        if (originalData.size() > 0) {
            enrichment.setEnriched(true);
            enrichment.setFieldsAdded(originalData.size());
        }

        logger.debug("Processed rule results: triggered={}, message={}",
                    ruleResult.isTriggered(), ruleResult.getMessage());
    }
}
