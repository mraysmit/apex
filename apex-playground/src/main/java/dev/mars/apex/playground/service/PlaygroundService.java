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
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.config.yaml.YamlDataSourceRef;
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

import java.util.ArrayList;
import java.util.List;
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

            // Parse YAML to config object
            YamlConfigurationLoader configLoader = new YamlConfigurationLoader();
            YamlRuleConfiguration yamlConfig = configLoader.fromYamlString(request.getYamlRules());

            // Extract enrichment sources
            List<String> enrichmentSources = new ArrayList<>();
            if (yamlConfig.getDataSources() != null) {
                for (YamlDataSource ds : yamlConfig.getDataSources()) {
                    if (ds.getName() != null) {
                        enrichmentSources.add(ds.getName());
                    }
                }
            }
            if (yamlConfig.getDataSourceRefs() != null) {
                for (YamlDataSourceRef ref : yamlConfig.getDataSourceRefs()) {
                    if (ref.getName() != null) {
                        enrichmentSources.add(ref.getName());
                    }
                }
            }
            response.getEnrichment().setEnrichmentSources(enrichmentSources);

            // Create engine from config
            RulesEngine rulesEngine = RulesEngine.fromYamlConfig(yamlConfig);

            // Execute unified evaluation
            RuleResult result = rulesEngine.evaluate(parsedData);

            response.getMetrics().setRulesExecutionTimeMs(System.currentTimeMillis() - rulesStartTime);

            // Step 4: Map results to response
            response.setSuccess(result.isSuccess());
            response.setMessage(result.getMessage());
            response.setTrace(result.getExecutionPath());

            // Handle enrichment data
            if (result.getEnrichedData() != null) {
                response.getEnrichment().setEnrichedData(result.getEnrichedData());

                // Calculate fields added (approximate)
                int fieldsAdded = result.getEnrichedData().size() - parsedData.size();
                response.getEnrichment().setFieldsAdded(Math.max(0, fieldsAdded));
                
                // Only mark as enriched if fields were added
                response.getEnrichment().setEnriched(fieldsAdded > 0);
            }

            // Handle failures
            if (result.hasFailures()) {
                for (String failure : result.getFailureMessages()) {
                    response.addError(failure);
                }
            }

            // Populate validation results from individual rule results
            List<RuleResult> childResults = result.getChildResults();
            if (childResults != null && !childResults.isEmpty()) {
                logger.info("Processing {} individual rule results", childResults.size());
                for (RuleResult childResult : childResults) {
                    RuleExecutionResult executionResult = new RuleExecutionResult(
                        childResult.getRuleId() != null ? childResult.getRuleId() : childResult.getRuleName(),
                        childResult.getRuleName(),
                        childResult.isTriggered(),
                        childResult.getMessage()
                    );
                    response.getValidation().addResult(executionResult);
                }
            } else {
                // Fallback: create a single overall result if no child results
                logger.info("No individual rule results, creating overall result");
                RuleExecutionResult executionResult = new RuleExecutionResult(
                    "evaluation-" + System.currentTimeMillis(),
                    "Overall Evaluation",
                    result.isTriggered(),
                    result.getMessage()
                );
                response.getValidation().addResult(executionResult);
            }

            // Step 5: Set final metrics
            response.getMetrics().setTotalTimeMs(System.currentTimeMillis() - startTime);

            logger.info("Processing completed successfully in {}ms", response.getMetrics().getTotalTimeMs());

        } catch (YamlConfigurationException e) {
            logger.error("YAML configuration error: {}", e.getMessage());
            response.setSuccess(false);
            response.setMessage("YAML configuration error: " + e.getMessage());
            response.addError("YAML configuration error: " + e.getMessage());
            
            // Add system error to validation results to ensure UI reflects failure
            response.getValidation().addResult(new RuleExecutionResult(
                "yaml-error", 
                "Configuration Error", 
                false, 
                e.getMessage(),
                null,
                0,
                "ERROR",
                "system"
            ));
        } catch (Exception e) {
            logger.error("Processing error: {}", e.getMessage());
            logger.debug("Full exception details:", e);
            response.setSuccess(false);
            response.setMessage("Processing failed: " + e.getMessage());
            response.addError("Processing error: " + e.getMessage());
            
            // Add system error to validation results to ensure UI reflects failure
            response.getValidation().addResult(new RuleExecutionResult(
                "system-error", 
                "System Error", 
                false, 
                e.getMessage(),
                null,
                0,
                "ERROR",
                "system"
            ));
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
}