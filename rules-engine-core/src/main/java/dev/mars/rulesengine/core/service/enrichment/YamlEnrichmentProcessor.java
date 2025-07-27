package dev.mars.rulesengine.core.service.enrichment;

import dev.mars.rulesengine.core.config.yaml.YamlEnrichment;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import dev.mars.rulesengine.core.service.lookup.DatasetLookupService;
import dev.mars.rulesengine.core.service.lookup.DatasetLookupServiceFactory;
import dev.mars.rulesengine.core.service.lookup.LookupService;
import dev.mars.rulesengine.core.service.lookup.LookupServiceRegistry;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
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
 * Processor for executing YAML-defined enrichment configurations.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Processor for executing YAML-defined enrichment configurations.
 * This class bridges the gap between YAML configuration and runtime enrichment execution.
 */
public class YamlEnrichmentProcessor {
    
    private static final Logger LOGGER = Logger.getLogger(YamlEnrichmentProcessor.class.getName());
    
    private final LookupServiceRegistry serviceRegistry;
    private final ExpressionEvaluatorService evaluatorService;
    private final SpelExpressionParser parser;
    
    // Cache for compiled expressions to improve performance
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
    
    // Cache for lookup results (if caching is enabled)
    private final Map<String, CachedLookupResult> lookupCache = new ConcurrentHashMap<>();
    
    public YamlEnrichmentProcessor(LookupServiceRegistry serviceRegistry, 
                                   ExpressionEvaluatorService evaluatorService) {
        this.serviceRegistry = serviceRegistry;
        this.evaluatorService = evaluatorService;
        this.parser = new SpelExpressionParser();
        
        LOGGER.info("YamlEnrichmentProcessor initialized with service registry and expression evaluator");
    }
    
    /**
     * Process a list of enrichments on a target object.
     * 
     * @param enrichments List of YAML enrichment configurations
     * @param targetObject The object to enrich
     * @return The enriched object
     */
    public Object processEnrichments(List<YamlEnrichment> enrichments, Object targetObject) {
        if (enrichments == null || enrichments.isEmpty()) {
            LOGGER.fine("No enrichments to process");
            return targetObject;
        }
        
        LOGGER.info("Processing " + enrichments.size() + " enrichments for object type: " +
                   targetObject.getClass().getSimpleName());
        
        // Sort enrichments by priority (lower numbers = higher priority)
        enrichments.sort((e1, e2) -> {
            int priority1 = e1.getPriority() != null ? e1.getPriority() : 100;
            int priority2 = e2.getPriority() != null ? e2.getPriority() : 100;
            return Integer.compare(priority1, priority2);
        });
        
        Object enrichedObject = targetObject;
        int processedCount = 0;
        
        for (YamlEnrichment enrichment : enrichments) {
            try {
                if (shouldProcessEnrichment(enrichment, enrichedObject)) {
                    enrichedObject = processEnrichment(enrichment, enrichedObject);
                    processedCount++;
                    LOGGER.fine("Successfully processed enrichment: " + enrichment.getId());
                } else {
                    LOGGER.fine("Skipping enrichment (condition not met): " + enrichment.getId());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to process enrichment '" + enrichment.getId() + 
                          "': " + e.getMessage(), e);
                // Continue processing other enrichments
            }
        }
        
        LOGGER.info("Completed processing enrichments. Processed: " + processedCount + 
                   " out of " + enrichments.size());
        
        return enrichedObject;
    }
    
    /**
     * Process a single enrichment on a target object.
     * 
     * @param enrichment The YAML enrichment configuration
     * @param targetObject The object to enrich
     * @return The enriched object
     */
    public Object processEnrichment(YamlEnrichment enrichment, Object targetObject) {
        LOGGER.fine("Processing enrichment: " + enrichment.getId() + " (type: " + enrichment.getType() + ")");

        // Check if enrichment should be processed
        if (!shouldProcessEnrichment(enrichment, targetObject)) {
            LOGGER.fine("Enrichment " + enrichment.getId() + " should not be processed");
            return targetObject;
        }

        LOGGER.fine("Enrichment " + enrichment.getId() + " passed conditions, proceeding with processing");

        switch (enrichment.getType()) {
            case "lookup-enrichment":
                return processLookupEnrichment(enrichment, targetObject);
            case "calculation-enrichment":
                return processCalculationEnrichment(enrichment, targetObject);
            case "field-enrichment":
                return processFieldEnrichment(enrichment, targetObject);
            default:
                LOGGER.warning("Unknown enrichment type: " + enrichment.getType());
                return targetObject;
        }
    }
    
    /**
     * Check if an enrichment should be processed based on its condition.
     * 
     * @param enrichment The enrichment configuration
     * @param targetObject The target object
     * @return true if the enrichment should be processed
     */
    private boolean shouldProcessEnrichment(YamlEnrichment enrichment, Object targetObject) {
        LOGGER.fine("Evaluating enrichment: " + enrichment.getId() + " for object type: " +
                   targetObject.getClass().getSimpleName());

        // Check if enrichment is enabled
        if (enrichment.getEnabled() != null && !enrichment.getEnabled()) {
            LOGGER.fine("Enrichment " + enrichment.getId() + " is disabled");
            return false;
        }
        
        // Check target type if specified (more flexible matching)
        if (enrichment.getTargetType() != null) {
            String targetType = enrichment.getTargetType();
            String actualSimpleName = targetObject.getClass().getSimpleName();
            String actualFullName = targetObject.getClass().getName();

            // Allow flexible matching: exact match, simple name match, or contains match
            boolean typeMatches = actualSimpleName.equals(targetType) ||
                                actualFullName.equals(targetType) ||
                                actualSimpleName.contains(targetType) ||
                                targetType.equals("Trade") && actualSimpleName.contains("Trade");

            if (!typeMatches) {
                LOGGER.fine("Target type mismatch. Expected: " + targetType +
                           ", Actual: " + actualSimpleName + " (full: " + actualFullName + ")");
                return false;
            } else {
                LOGGER.fine("Target type match successful. Expected: " + targetType +
                           ", Actual: " + actualSimpleName);
            }
        }
        
        // Evaluate condition if specified
        if (enrichment.getCondition() != null && !enrichment.getCondition().trim().isEmpty()) {
            try {
                StandardEvaluationContext context = createEvaluationContext(targetObject);
                Expression conditionExpr = getOrCompileExpression(enrichment.getCondition());
                Boolean result = conditionExpr.getValue(context, Boolean.class);

                LOGGER.fine("Condition evaluation for " + enrichment.getId() +
                           ": '" + enrichment.getCondition() + "' = " + result);

                return result != null && result;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error evaluating enrichment condition '" +
                          enrichment.getCondition() + "' for enrichment " + enrichment.getId() +
                          ": " + e.getMessage(), e);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Process a lookup-based enrichment.
     * 
     * @param enrichment The enrichment configuration
     * @param targetObject The target object
     * @return The enriched object
     */
    private Object processLookupEnrichment(YamlEnrichment enrichment, Object targetObject) {
        YamlEnrichment.LookupConfig lookupConfig = enrichment.getLookupConfig();
        if (lookupConfig == null) {
            LOGGER.warning("Lookup enrichment '" + enrichment.getId() + "' has no lookup configuration");
            return targetObject;
        }
        
        // 1. Resolve lookup service (either from registry or create from dataset)
        LookupService lookupService = resolveLookupService(enrichment.getId(), lookupConfig);

        LOGGER.fine("Processing lookup enrichment with service: " + lookupService.getName());
        
        // 2. Extract lookup key using SpEL expression
        Object lookupKey;
        try {
            StandardEvaluationContext context = createEvaluationContext(targetObject);
            Expression keyExpr = getOrCompileExpression(lookupConfig.getLookupKey());
            lookupKey = keyExpr.getValue(context);
            
            if (lookupKey == null) {
                LOGGER.fine("Lookup key evaluated to null for enrichment: " + enrichment.getId());
                return targetObject;
            }
            
            LOGGER.fine("Extracted lookup key: " + lookupKey);
        } catch (Exception e) {
            throw new EnrichmentException("Failed to extract lookup key using expression '" + 
                                        lookupConfig.getLookupKey() + "'", e);
        }
        
        // 3. Perform lookup (with caching if enabled)
        Object lookupResult = performLookup(lookupService, lookupKey, lookupConfig);

        LOGGER.fine("Lookup result for key '" + lookupKey + "': " + lookupResult +
                   " (type: " + (lookupResult != null ? lookupResult.getClass().getSimpleName() : "null") + ")");

        if (lookupResult == null) {
            LOGGER.fine("Lookup returned null result for key: " + lookupKey);
            return targetObject;
        }
        
        // 4. Apply field mappings
        return applyFieldMappings(enrichment.getFieldMappings(), lookupResult, targetObject);
    }
    
    /**
     * Process a calculation-based enrichment.
     * 
     * @param enrichment The enrichment configuration
     * @param targetObject The target object
     * @return The enriched object
     */
    private Object processCalculationEnrichment(YamlEnrichment enrichment, Object targetObject) {
        YamlEnrichment.CalculationConfig calcConfig = enrichment.getCalculationConfig();
        if (calcConfig == null) {
            LOGGER.warning("Calculation enrichment '" + enrichment.getId() + "' has no calculation configuration");
            return targetObject;
        }
        
        try {
            StandardEvaluationContext context = createEvaluationContext(targetObject);
            Expression calcExpr = getOrCompileExpression(calcConfig.getExpression());
            Object result = calcExpr.getValue(context);
            
            // Set the result field
            if (calcConfig.getResultField() != null) {
                setFieldValue(targetObject, calcConfig.getResultField(), result);
            }
            
            LOGGER.fine("Calculation enrichment completed. Result: " + result);
            return targetObject;
            
        } catch (Exception e) {
            throw new EnrichmentException("Failed to process calculation enrichment", e);
        }
    }
    
    /**
     * Process a field-based enrichment.
     * 
     * @param enrichment The enrichment configuration
     * @param targetObject The target object
     * @return The enriched object
     */
    private Object processFieldEnrichment(YamlEnrichment enrichment, Object targetObject) {
        // Apply field mappings directly
        return applyFieldMappings(enrichment.getFieldMappings(), targetObject, targetObject);
    }
    
    /**
     * Perform lookup operation with caching support.
     * 
     * @param lookupService The lookup service
     * @param lookupKey The lookup key
     * @param lookupConfig The lookup configuration
     * @return The lookup result
     */
    private Object performLookup(LookupService lookupService, Object lookupKey, 
                                YamlEnrichment.LookupConfig lookupConfig) {
        
        String cacheKey = lookupService.getName() + ":" + lookupKey.toString();
        
        // Check cache if enabled
        if (lookupConfig.getCacheEnabled() != null && lookupConfig.getCacheEnabled()) {
            CachedLookupResult cached = lookupCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                LOGGER.finest("Cache hit for lookup key: " + lookupKey);
                return cached.getResult();
            }
        }
        
        // Perform actual lookup
        Object result = lookupService.enrich(lookupKey);
        
        // Cache result if caching is enabled
        if (lookupConfig.getCacheEnabled() != null && lookupConfig.getCacheEnabled()) {
            int ttlSeconds = lookupConfig.getCacheTtlSeconds() != null ? 
                           lookupConfig.getCacheTtlSeconds() : 300;
            lookupCache.put(cacheKey, new CachedLookupResult(result, ttlSeconds));
            LOGGER.finest("Cached lookup result for key: " + lookupKey);
        }
        
        return result;
    }

    /**
     * Apply field mappings from lookup result to target object.
     *
     * @param fieldMappings The field mapping configurations
     * @param sourceObject The source object (lookup result)
     * @param targetObject The target object to enrich
     * @return The enriched target object
     */
    private Object applyFieldMappings(List<YamlEnrichment.FieldMapping> fieldMappings,
                                     Object sourceObject, Object targetObject) {
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            LOGGER.fine("No field mappings to apply");
            return targetObject;
        }

        // Check if source object is a simple value (failed lookup)
        boolean isFailedLookup = sourceObject != null && !(sourceObject instanceof Map) &&
                                sourceObject.getClass().equals(String.class);

        if (isFailedLookup) {
            LOGGER.fine("Source object is a simple value (likely failed lookup), applying only default values");
        }

        LOGGER.fine("Applying " + fieldMappings.size() + " field mappings from " +
                   (sourceObject != null ? sourceObject.getClass().getSimpleName() : "null") +
                   " to " + targetObject.getClass().getSimpleName());

        for (YamlEnrichment.FieldMapping mapping : fieldMappings) {
            try {
                LOGGER.finest("Processing field mapping: " + mapping.getSourceField() + " -> " + mapping.getTargetField());

                Object sourceValue = null;

                // For failed lookups, don't try to extract source values
                if (!isFailedLookup) {
                    sourceValue = getFieldValue(sourceObject, mapping.getSourceField());
                    LOGGER.finest("Source value for '" + mapping.getSourceField() + "': " + sourceValue);

                    // Handle missing required fields (only for successful lookups)
                    if (sourceValue == null && mapping.getRequired() != null && mapping.getRequired()) {
                        throw new EnrichmentException("Required field '" + mapping.getSourceField() +
                                                    "' is missing from lookup result");
                    }
                }

                // Use default value if source value is null (or for failed lookups)
                Object valueToSet = sourceValue != null ? sourceValue : mapping.getDefaultValue();
                LOGGER.finest("Value to set (after defaults): " + valueToSet);

                // Apply transformation if specified
                if (mapping.getTransformation() != null && !mapping.getTransformation().trim().isEmpty()) {
                    valueToSet = applyTransformation(mapping.getTransformation(), valueToSet, targetObject);
                    LOGGER.finest("Value after transformation: " + valueToSet);
                }

                // Set the target field only if we have a value to set
                if (valueToSet != null) {
                    setFieldValue(targetObject, mapping.getTargetField(), valueToSet);
                    LOGGER.fine("Successfully mapped field: " + mapping.getSourceField() + " -> " +
                               mapping.getTargetField() + " (value: " + valueToSet + ")");
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to apply field mapping: " +
                          mapping.getSourceField() + " -> " + mapping.getTargetField() +
                          ": " + e.getMessage(), e);
            }
        }

        return targetObject;
    }

    /**
     * Apply transformation expression to a value.
     *
     * @param transformation The SpEL transformation expression
     * @param value The value to transform
     * @param context The context object
     * @return The transformed value
     */
    private Object applyTransformation(String transformation, Object value, Object context) {
        try {
            StandardEvaluationContext evalContext = createEvaluationContext(context);
            evalContext.setVariable("value", value);

            Expression transformExpr = getOrCompileExpression(transformation);
            return transformExpr.getValue(evalContext);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to apply transformation '" + transformation +
                      "' to value: " + value, e);
            return value; // Return original value on transformation failure
        }
    }

    /**
     * Get field value from an object using reflection or map access.
     *
     * @param object The object to get the field from
     * @param fieldName The field name
     * @return The field value
     */
    private Object getFieldValue(Object object, String fieldName) {
        if (object == null || fieldName == null) {
            LOGGER.fine("getFieldValue called with null object or fieldName");
            return null;
        }

        LOGGER.finest("Getting field '" + fieldName + "' from object of type: " + object.getClass().getSimpleName());

        // Handle Map objects
        if (object instanceof Map) {
            Object value = ((Map<?, ?>) object).get(fieldName);
            LOGGER.finest("Map lookup for '" + fieldName + "' returned: " + value);
            return value;
        }

        // Handle regular objects using reflection
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(object);
            LOGGER.finest("Reflection lookup for '" + fieldName + "' returned: " + value);
            return value;
        } catch (NoSuchFieldException e) {
            LOGGER.fine("Field '" + fieldName + "' not found on object of type " +
                       object.getClass().getSimpleName());
            return null;
        } catch (IllegalAccessException e) {
            LOGGER.warning("Could not access field '" + fieldName + "' on object of type " +
                          object.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Set field value on an object using reflection or map access.
     *
     * @param object The object to set the field on
     * @param fieldName The field name
     * @param value The value to set
     */
    private void setFieldValue(Object object, String fieldName, Object value) {
        if (object == null || fieldName == null) {
            LOGGER.fine("setFieldValue called with null object or fieldName");
            return;
        }

        LOGGER.finest("Setting field '" + fieldName + "' to value: " + value +
                     " on object of type: " + object.getClass().getSimpleName());

        // Handle Map objects
        if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) object;
            map.put(fieldName, value);
            LOGGER.finest("Successfully set map key '" + fieldName + "' to: " + value);
            return;
        }

        // Handle regular objects using reflection
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
            LOGGER.finest("Successfully set field '" + fieldName + "' to: " + value);
        } catch (NoSuchFieldException e) {
            LOGGER.warning("Field '" + fieldName + "' not found on object of type " +
                          object.getClass().getSimpleName());
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.WARNING, "Could not set field '" + fieldName + "' on object of type " +
                      object.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Create evaluation context for SpEL expressions.
     *
     * @param rootObject The root object for the context
     * @return The evaluation context
     */
    private StandardEvaluationContext createEvaluationContext(Object rootObject) {
        StandardEvaluationContext context = new StandardEvaluationContext(rootObject);

        // Add common variables and functions
        context.setVariable("serviceRegistry", serviceRegistry);

        return context;
    }

    /**
     * Get or compile SpEL expression with caching.
     *
     * @param expressionString The expression string
     * @return The compiled expression
     */
    private Expression getOrCompileExpression(String expressionString) {
        return expressionCache.computeIfAbsent(expressionString, parser::parseExpression);
    }

    /**
     * Clear the lookup cache.
     */
    public void clearCache() {
        lookupCache.clear();
        LOGGER.info("Lookup cache cleared");
    }

    /**
     * Get cache statistics.
     *
     * @return Map containing cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", lookupCache.size());
        stats.put("expressionCacheSize", expressionCache.size());

        long expiredEntries = lookupCache.values().stream()
            .mapToLong(entry -> entry.isExpired() ? 1 : 0)
            .sum();
        stats.put("expiredEntries", expiredEntries);

        return stats;
    }

    /**
     * Resolve lookup service from either service registry or dataset configuration.
     *
     * @param enrichmentId The enrichment ID for error messages
     * @param lookupConfig The lookup configuration
     * @return The resolved lookup service
     * @throws EnrichmentException if no service or dataset is configured
     */
    private LookupService resolveLookupService(String enrichmentId, YamlEnrichment.LookupConfig lookupConfig) {
        // Priority 1: External service (existing approach)
        if (lookupConfig.getLookupService() != null) {
            String serviceName = lookupConfig.getLookupService();
            LookupService service = serviceRegistry.getService(serviceName, LookupService.class);

            if (service == null) {
                throw new EnrichmentException("Lookup service not found: " + serviceName);
            }

            LOGGER.fine("Resolved external lookup service: " + serviceName);
            return service;
        }

        // Priority 2: Dataset configuration (new approach)
        if (lookupConfig.getLookupDataset() != null) {
            YamlEnrichment.LookupDataset dataset = lookupConfig.getLookupDataset();

            // Generate a unique service name for the dataset
            String datasetServiceName = "dataset-" + enrichmentId + "-" + dataset.getType();

            try {
                DatasetLookupService datasetService = DatasetLookupServiceFactory
                    .createDatasetLookupService(datasetServiceName, dataset);

                LOGGER.fine("Created dataset lookup service: " + datasetServiceName +
                           " (type: " + dataset.getType() + ", records: " +
                           datasetService.getAllRecords().size() + ")");

                return datasetService;
            } catch (Exception e) {
                throw new EnrichmentException("Failed to create dataset lookup service for enrichment '" +
                                            enrichmentId + "': " + e.getMessage(), e);
            }
        }

        throw new EnrichmentException("No lookup service or dataset configured for enrichment: " + enrichmentId);
    }

    /**
     * Cached lookup result with TTL support.
     */
    private static class CachedLookupResult {
        private final Object result;
        private final long expirationTime;

        public CachedLookupResult(Object result, int ttlSeconds) {
            this.result = result;
            this.expirationTime = System.currentTimeMillis() + (ttlSeconds * 1000L);
        }

        public Object getResult() {
            return result;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
}
