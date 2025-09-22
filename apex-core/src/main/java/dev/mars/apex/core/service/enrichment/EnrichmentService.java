package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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

/**
 * Service for managing and executing enrichment operations.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class EnrichmentService {
    
    private static final Logger LOGGER = Logger.getLogger(EnrichmentService.class.getName());
    
    private final YamlEnrichmentProcessor processor;
    private final LookupServiceRegistry serviceRegistry;
    
    public EnrichmentService(LookupServiceRegistry serviceRegistry, 
                           ExpressionEvaluatorService evaluatorService) {
        this.serviceRegistry = serviceRegistry;
        this.processor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
        
        LOGGER.info("EnrichmentService initialized");
    }
    
    /**
     * Enrich an object using YAML-defined enrichment configurations.
     * 
     * @param yamlConfig The YAML configuration containing enrichments
     * @param targetObject The object to enrich
     * @return The enriched object
     */
    public Object enrichObject(YamlRuleConfiguration yamlConfig, Object targetObject) {
        if (yamlConfig == null || yamlConfig.getEnrichments() == null) {
            LOGGER.fine("No enrichments found in YAML configuration");
            return targetObject;
        }

        return processor.processEnrichments(yamlConfig.getEnrichments(), targetObject, yamlConfig);
    }
    
    /**
     * Enrich an object using a specific list of enrichments.
     * 
     * @param enrichments The list of enrichment configurations
     * @param targetObject The object to enrich
     * @return The enriched object
     */
    public Object enrichObject(List<YamlEnrichment> enrichments, Object targetObject) {
        return processor.processEnrichments(enrichments, targetObject);
    }
    
    /**
     * Enrich an object using a single enrichment configuration.
     * 
     * @param enrichment The enrichment configuration
     * @param targetObject The object to enrich
     * @return The enriched object
     */
    public Object enrichObject(YamlEnrichment enrichment, Object targetObject) {
        return processor.processEnrichment(enrichment, targetObject);
    }
    
    /**
     * Get the lookup service registry.
     * 
     * @return The service registry
     */
    public LookupServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }
    
    /**
     * Get the enrichment processor.
     * 
     * @return The processor
     */
    public YamlEnrichmentProcessor getProcessor() {
        return processor;
    }
    
    /**
     * Clear all caches.
     */
    public void clearCaches() {
        processor.clearCache();
        LOGGER.info("All enrichment caches cleared");
    }
    
    /**
     * Get enrichment statistics.
     *
     * @return Statistics map
     */
    public java.util.Map<String, Object> getStatistics() {
        return processor.getCacheStatistics();
    }

    // ========================================
    // RuleResult-returning methods (Phase 4)
    // ========================================

    /**
     * Enrich an object using YAML-defined enrichment configurations and return detailed results.
     * This method provides programmatic access to enrichment success/failure status and detailed error information.
     *
     * @param yamlConfig The YAML configuration containing enrichments
     * @param targetObject The object to enrich
     * @return A RuleResult containing success status, enriched data, and failure messages
     */
    public RuleResult enrichObjectWithResult(YamlRuleConfiguration yamlConfig, Object targetObject) {
        if (yamlConfig == null || yamlConfig.getEnrichments() == null) {
            LOGGER.fine("No enrichments found in YAML configuration");
            // Return success with original data when no enrichments to process
            Map<String, Object> resultData = convertToMap(targetObject);
            return RuleResult.enrichmentSuccess(resultData);
        }

        return processEnrichmentsWithFailureDetection(yamlConfig.getEnrichments(), targetObject, yamlConfig);
    }

    /**
     * Enrich an object using a specific list of enrichments and return detailed results.
     * This method provides programmatic access to enrichment success/failure status and detailed error information.
     *
     * @param enrichments The list of enrichment configurations
     * @param targetObject The object to enrich
     * @return A RuleResult containing success status, enriched data, and failure messages
     */
    public RuleResult enrichObjectWithResult(List<YamlEnrichment> enrichments, Object targetObject) {
        if (enrichments == null || enrichments.isEmpty()) {
            LOGGER.fine("No enrichments provided");
            // Return success with original data when no enrichments to process
            Map<String, Object> resultData = convertToMap(targetObject);
            return RuleResult.enrichmentSuccess(resultData);
        }

        return processEnrichmentsWithFailureDetection(enrichments, targetObject, null);
    }

    /**
     * Enrich an object using a single enrichment configuration and return detailed results.
     * This method provides programmatic access to enrichment success/failure status and detailed error information.
     *
     * @param enrichment The enrichment configuration
     * @param targetObject The object to enrich
     * @return A RuleResult containing success status, enriched data, and failure messages
     */
    public RuleResult enrichObjectWithResult(YamlEnrichment enrichment, Object targetObject) {
        if (enrichment == null) {
            LOGGER.fine("No enrichment provided");
            // Return success with original data when no enrichment to process
            Map<String, Object> resultData = convertToMap(targetObject);
            return RuleResult.enrichmentSuccess(resultData);
        }

        List<YamlEnrichment> enrichmentList = new ArrayList<>();
        enrichmentList.add(enrichment);
        return processEnrichmentsWithFailureDetection(enrichmentList, targetObject, null);
    }

    /**
     * Process enrichments with comprehensive failure detection and return RuleResult.
     * This method replicates the failure detection logic from RulesEngine.detectEnrichmentFailures().
     *
     * @param enrichments The list of enrichments to process
     * @param targetObject The object to enrich
     * @param yamlConfig The full YAML configuration (optional, needed for database lookups)
     * @return A RuleResult with detailed success/failure information
     */
    private RuleResult processEnrichmentsWithFailureDetection(List<YamlEnrichment> enrichments, Object targetObject, YamlRuleConfiguration yamlConfig) {
        LOGGER.fine("Processing enrichments with failure detection for " + enrichments.size() + " enrichments");

        // Store original data size for failure detection
        Map<String, Object> originalData = convertToMap(targetObject);
        int originalDataSize = originalData.size();

        List<String> failureMessages = new ArrayList<>();

        try {
            // Process enrichments using existing processor
            Object enrichmentResult;
            if (yamlConfig != null) {
                enrichmentResult = processor.processEnrichments(enrichments, targetObject, yamlConfig);
            } else {
                enrichmentResult = processor.processEnrichments(enrichments, targetObject);
            }

            // Convert result to Map for analysis
            Map<String, Object> enrichedData = convertToMap(enrichmentResult);

            // Detect enrichment failures using the same logic as RulesEngine
            boolean enrichmentFailed = detectEnrichmentFailures(enrichments, enrichedData, originalDataSize);

            if (enrichmentFailed) {
                failureMessages.add("Required field enrichment failed - check logs for CRITICAL ERROR details");
                LOGGER.warning("Enrichment failed due to required field mapping failures");
                return RuleResult.enrichmentFailure(failureMessages, enrichedData);
            } else {
                LOGGER.fine("Enrichment completed successfully");
                return RuleResult.enrichmentSuccess(enrichedData);
            }

        } catch (Exception e) {
            LOGGER.severe("Exception during enrichment processing: " + e.getMessage());
            failureMessages.add("Enrichment processing exception: " + e.getMessage());
            return RuleResult.enrichmentFailure(failureMessages, originalData);
        }
    }

    /**
     * Detect enrichment failures by checking if required fields were successfully enriched.
     * This method replicates the logic from RulesEngine.detectEnrichmentFailures().
     *
     * @param enrichments The list of enrichments that were processed
     * @param enrichedData The enriched data map
     * @param originalDataSize The original data size before enrichment
     * @return true if enrichment failures were detected, false otherwise
     */
    private boolean detectEnrichmentFailures(List<YamlEnrichment> enrichments, Map<String, Object> enrichedData, int originalDataSize) {
        if (enrichments == null || enrichments.isEmpty()) {
            return false;
        }

        boolean hasFailures = false;

        for (YamlEnrichment enrichment : enrichments) {
            if (enrichment.getFieldMappings() != null) {
                for (YamlEnrichment.FieldMapping mapping : enrichment.getFieldMappings()) {
                    // Check if this is a required field mapping
                    if (mapping.getRequired() != null && mapping.getRequired()) {
                        String targetField = mapping.getTargetField();

                        // Check if the required target field is missing or null in enriched data
                        if (!enrichedData.containsKey(targetField) || enrichedData.get(targetField) == null) {
                            LOGGER.fine("Required field '" + targetField + "' is missing from enriched data");
                            hasFailures = true;
                        }
                    }
                }
            }
        }

        return hasFailures;
    }

    /**
     * Convert an object to a Map for consistent data handling.
     * This method handles both Map objects and regular objects.
     *
     * @param object The object to convert
     * @return A Map representation of the object
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object object) {
        if (object instanceof Map) {
            return new HashMap<>((Map<String, Object>) object);
        } else {
            // For non-Map objects, create a simple wrapper
            Map<String, Object> result = new HashMap<>();
            result.put("data", object);
            return result;
        }
    }
}
