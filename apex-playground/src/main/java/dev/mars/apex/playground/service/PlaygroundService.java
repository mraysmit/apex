package dev.mars.apex.playground.service;

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


import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.model.RuleBase;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.playground.model.PlaygroundRequest;
import dev.mars.apex.playground.model.PlaygroundResponse;
import dev.mars.apex.playground.model.RuleExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;

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

    @Autowired
    public PlaygroundService(DataProcessingService dataProcessingService,
                           YamlValidationService yamlValidationService) {
        this.dataProcessingService = dataProcessingService;
        this.yamlValidationService = yamlValidationService;
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
            
            // Parse YAML to config object first to check for pipeline
            YamlConfigurationLoader configLoader = new YamlConfigurationLoader();
            YamlRuleConfiguration yamlConfig = configLoader.fromYamlString(request.getYamlRules());
            
            RulesEngine rulesEngine = RulesEngine.fromYamlConfig(yamlConfig);

            // Check for pipeline execution
            if (yamlConfig.getPipeline() != null) {
                logger.info("Executing pipeline: {}", yamlConfig.getPipeline().getName());
                try {
                    RuleResult pipelineResult = rulesEngine.evaluate(parsedData);
                    
                    // Add pipeline result to response
                    RuleExecutionResult executionResult = new RuleExecutionResult(
                        "pipeline-" + System.currentTimeMillis(),
                        yamlConfig.getPipeline().getName(),
                        pipelineResult.getResultType() == RuleResult.ResultType.MATCH,
                        pipelineResult.getMessage()
                    );
                    
                    if (pipelineResult.getPerformanceMetrics() != null) {
                        executionResult.setExecutionTimeMs(pipelineResult.getPerformanceMetrics().getEvaluationTimeMillis());
                    }
                    
                    response.getValidation().addResult(executionResult);
                    
                } catch (Exception e) {
                    logger.error("Error executing pipeline", e);
                    RuleExecutionResult errorResult = new RuleExecutionResult(
                        "pipeline-error-" + System.currentTimeMillis(),
                        yamlConfig.getPipeline().getName(),
                        false,
                        "Pipeline execution error: " + e.getMessage()
                    );
                    response.getValidation().addResult(errorResult);
                }
            }

            // Execute rules individually to capture all results
            List<RuleBase> rules = rulesEngine.getConfiguration().getRulesForCategory("default");
            if (rules != null && !rules.isEmpty()) {
                logger.info("Executing {} rules individually for playground", rules.size());
                for (RuleBase rule : rules) {
                    RuleResult result = null;
                    try {
                        if (rule instanceof Rule) {
                            result = rulesEngine.executeRule((Rule) rule, parsedData);
                        } else if (rule instanceof RuleGroup) {
                            result = rulesEngine.executeRuleGroupsList(Collections.singletonList((RuleGroup) rule), parsedData);
                        }
                        
                        if (result != null) {
                            addRuleResultToResponse(result, response);
                        }
                    } catch (Exception e) {
                        logger.error("Error executing rule '{}': {}", rule.getName(), e.getMessage());
                        // Add error result
                        RuleExecutionResult errorResult = new RuleExecutionResult(
                            "rule-error-" + System.currentTimeMillis(),
                            rule.getName(),
                            false,
                            "Execution error: " + e.getMessage()
                        );
                        response.getValidation().addResult(errorResult);
                    }
                }
            } else {
                logger.info("No rules found in 'default' category");
            }
            
            response.getMetrics().setRulesExecutionTimeMs(System.currentTimeMillis() - rulesStartTime);

            // Step 4: Process enrichments
            processEnrichments(request, parsedData, response);

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
     * Add a single rule result to the response.
     */
    private void addRuleResultToResponse(RuleResult ruleResult, PlaygroundResponse response) {
        PlaygroundResponse.ValidationResult validation = response.getValidation();

        if (ruleResult.isTriggered()) {
            // Rule was triggered (passed)
            RuleExecutionResult executionResult = new RuleExecutionResult(
                "rule-" + System.currentTimeMillis() + "-" + ruleResult.getRuleName().hashCode(),
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
                "rule-" + System.currentTimeMillis() + "-" + ruleResult.getRuleName().hashCode(),
                ruleResult.getRuleName() != null ? ruleResult.getRuleName() : "unknown",
                false,
                ruleResult.getMessage() != null ? ruleResult.getMessage() : "Rule condition not met"
            );

            if (ruleResult.getPerformanceMetrics() != null) {
                executionResult.setExecutionTimeMs(ruleResult.getPerformanceMetrics().getEvaluationTimeMillis());
            }

            validation.addResult(executionResult);
        }
        
        logger.debug("Processed rule result: name={}, triggered={}, message={}",
                    ruleResult.getRuleName(), ruleResult.isTriggered(), ruleResult.getMessage());
    }

    /**
     * Process enrichments and populate the response.
     */
    private void processEnrichments(PlaygroundRequest request, Map<String, Object> originalData, PlaygroundResponse response) {
        PlaygroundResponse.EnrichmentResult enrichment = response.getEnrichment();

        try {
            // Parse YAML configuration to get enrichments
            YamlConfigurationLoader configLoader = new YamlConfigurationLoader();
            YamlRuleConfiguration yamlConfig = configLoader.fromYamlString(request.getYamlRules());

            if (yamlConfig.getEnrichments() != null && !yamlConfig.getEnrichments().isEmpty()) {
                // Create a copy of original data for enrichment
                Map<String, Object> dataToEnrich = new HashMap<>(originalData);

                // Apply real enrichments using APEX engine (RulesEngine)
                // Create a config for enrichment only to avoid re-executing rules
                YamlRuleConfiguration enrichmentConfig = new YamlRuleConfiguration();
                enrichmentConfig.setEnrichments(yamlConfig.getEnrichments());
                enrichmentConfig.setDataSources(yamlConfig.getDataSources());
                enrichmentConfig.setDataSourceRefs(yamlConfig.getDataSourceRefs());
                
                // Create a temporary engine for enrichment
                RulesEngine enrichmentEngine = RulesEngine.fromYamlConfig(enrichmentConfig);
                
                RuleResult result = enrichmentEngine.evaluate(dataToEnrich);

                // Set the actual enriched data
                if (result.getEnrichedData() != null) {
                    Map<String, Object> enrichedMap = result.getEnrichedData();
                    enrichment.setEnrichedData(enrichedMap);

                    // Calculate how many fields were added
                    int fieldsAdded = enrichedMap.size() - originalData.size();
                    enrichment.setFieldsAdded(Math.max(0, fieldsAdded));
                    enrichment.setEnriched(fieldsAdded > 0 || !enrichedMap.equals(originalData));
                } else {
                    enrichment.setEnrichedData(originalData);
                    enrichment.setEnriched(false);
                    enrichment.setFieldsAdded(0);
                }
            } else {
                // No enrichments defined in YAML
                enrichment.setEnrichedData(originalData);
                enrichment.setEnriched(false);
                enrichment.setFieldsAdded(0);
            }
        } catch (Exception e) {
            logger.error("Error processing enrichments: {}", e.getMessage(), e);
            // Fallback to original data on error
            enrichment.setEnrichedData(originalData);
            enrichment.setEnriched(false);
            enrichment.setFieldsAdded(0);
        }
    }
}