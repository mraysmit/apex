package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.DatasetLookupService;
import dev.mars.apex.core.service.lookup.DatasetLookupServiceFactory;
import dev.mars.apex.core.service.lookup.LookupService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.config.yaml.YamlRule;
import dev.mars.apex.core.config.yaml.YamlRuleGroup;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
* This class is part of the APEX A powerful expression processor for Java applications.
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
    @SuppressWarnings("unused") // Reserved for future expression evaluation enhancements
    private final ExpressionEvaluatorService evaluatorService;
    private final SpelExpressionParser parser;

    // Cache for compiled expressions to improve performance
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    // Cache for lookup results (if caching is enabled)
    private final Map<String, CachedLookupResult> lookupCache = new ConcurrentHashMap<>();

    // Current configuration context for database lookups
    private dev.mars.apex.core.config.yaml.YamlRuleConfiguration currentConfiguration;

    // Rule result tracking for conditional mapping support
    private final Map<String, Map<String, Boolean>> ruleGroupResults = new ConcurrentHashMap<>();
    private final Map<String, Boolean> individualRuleResults = new ConcurrentHashMap<>();
    
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
        return processEnrichments(enrichments, targetObject, null);
    }

    /**
     * Process a list of enrichments on a target object with full configuration context.
     * This method is required for database lookups that need access to dataSources configuration.
     *
     * @param enrichments The list of enrichments to apply
     * @param targetObject The object to enrich
     * @param configuration The full YAML configuration (required for database lookups)
     * @return The enriched object
     */
    public Object processEnrichments(List<YamlEnrichment> enrichments, Object targetObject,
                                   dev.mars.apex.core.config.yaml.YamlRuleConfiguration configuration) {
        // Set current configuration for database lookups
        this.currentConfiguration = configuration;



        // Process rules and rule groups first to populate rule results
        if (configuration != null && (configuration.getRules() != null || configuration.getRuleGroups() != null)) {
            processRulesAndRuleGroups(configuration, targetObject);
        }

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
            case "conditional-mapping-enrichment":
                return processConditionalMappingEnrichment(enrichment, targetObject);
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
            LOGGER.fine("Lookup returned null result for key: " + lookupKey + ", applying default values");
        }

        // 4. Apply field mappings (even if lookup result is null, to apply default values)
        Object result = applyFieldMappings(enrichment.getFieldMappings(), lookupResult, targetObject);

        // If applyFieldMappings returns null, it means a required field mapping failed
        if (result == null) {
            LOGGER.warning("Enrichment '" + enrichment.getId() + "' failed due to required field mapping failure");
            // Return original target object to continue processing other enrichments
            return targetObject;
        }

        return result;
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
            // Phase 3A Enhancement: Check if calculation has a default-value for error recovery
            if (calcConfig.getDefaultValue() != null) {
                LOGGER.info("Using calculation default value for recovery: enrichment='" + enrichment.getId() +
                    "', defaultValue='" + calcConfig.getDefaultValue() + "'");

                // Set the default value in the result field
                if (calcConfig.getResultField() != null) {
                    setFieldValue(targetObject, calcConfig.getResultField(), calcConfig.getDefaultValue());
                }

                return targetObject;
            }

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
        // Process conditional mappings first (if present)
        if (enrichment.getConditionalMappings() != null && !enrichment.getConditionalMappings().isEmpty()) {
            targetObject = processConditionalMappings(enrichment.getConditionalMappings(), targetObject);
        }

        // Apply regular field mappings (if present)
        if (enrichment.getFieldMappings() != null && !enrichment.getFieldMappings().isEmpty()) {
            targetObject = applyFieldMappings(enrichment.getFieldMappings(), targetObject, targetObject);
        }

        return targetObject;
    }

    /**
     * Process conditional mappings for field-enrichment.
     *
     * @param conditionalMappings The conditional mapping configurations
     * @param targetObject The target object
     * @return The enriched object
     */
    private Object processConditionalMappings(List<YamlEnrichment.ConditionalMapping> conditionalMappings, Object targetObject) {
        LOGGER.fine("Processing " + conditionalMappings.size() + " conditional mappings");

        for (YamlEnrichment.ConditionalMapping conditionalMapping : conditionalMappings) {
            try {
                // Evaluate condition group
                if (evaluateConditionGroup(conditionalMapping.getConditions(), targetObject)) {
                    LOGGER.fine("Conditional mapping conditions met, applying field mappings");
                    // Apply field mappings for this conditional mapping
                    targetObject = applyFieldMappings(conditionalMapping.getFieldMappings(), targetObject, targetObject);
                    // Continue to next conditional mapping (don't break - multiple can apply)
                } else {
                    LOGGER.finest("Conditional mapping conditions not met, skipping");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to process conditional mapping: " + e.getMessage(), e);
            }
        }

        return targetObject;
    }

    /**
     * Process a conditional-mapping-enrichment.
     * This enrichment type uses priority-based mapping rules with first-match-wins logic.
     *
     * @param enrichment The enrichment configuration
     * @param targetObject The target object
     * @return The enriched object
     */
    private Object processConditionalMappingEnrichment(YamlEnrichment enrichment, Object targetObject) {
        String targetField = enrichment.getTargetField();
        List<YamlEnrichment.MappingRule> mappingRules = enrichment.getMappingRules();
        YamlEnrichment.ExecutionSettings executionSettings = enrichment.getExecutionSettings();

        if (targetField == null || targetField.trim().isEmpty()) {
            LOGGER.warning("Conditional mapping enrichment '" + enrichment.getId() + "' has no target field");
            return targetObject;
        }

        if (mappingRules == null || mappingRules.isEmpty()) {
            LOGGER.warning("Conditional mapping enrichment '" + enrichment.getId() + "' has no mapping rules");
            return targetObject;
        }

        LOGGER.fine("Processing conditional mapping enrichment for target field: " + targetField);

        // Sort mapping rules by priority (lower numbers = higher priority)
        mappingRules.sort((r1, r2) -> {
            int priority1 = r1.getPriority() != null ? r1.getPriority() : 999;
            int priority2 = r2.getPriority() != null ? r2.getPriority() : 999;
            return Integer.compare(priority1, priority2);
        });

        // Default execution settings
        boolean stopOnFirstMatch = executionSettings != null && executionSettings.getStopOnFirstMatch() != null ?
                                  executionSettings.getStopOnFirstMatch() : true;
        boolean logMatchedRule = executionSettings != null && executionSettings.getLogMatchedRule() != null ?
                                executionSettings.getLogMatchedRule() : false;

        // Process rules in priority order
        for (YamlEnrichment.MappingRule rule : mappingRules) {
            try {
                // Check if rule conditions are met
                if (evaluateMappingRuleConditions(rule, targetObject)) {
                    if (logMatchedRule) {
                        LOGGER.info("Matched mapping rule: " + rule.getId() + " (priority: " + rule.getPriority() + ")");
                    }

                    // Apply the mapping
                    Object mappedValue = applyMappingRule(rule, targetObject);

                    // Set the target field
                    setFieldValue(targetObject, targetField, mappedValue);

                    LOGGER.fine("Applied mapping rule '" + rule.getId() + "' to field '" + targetField + "' with value: " + mappedValue);

                    // Stop on first match if configured to do so
                    if (stopOnFirstMatch) {
                        LOGGER.fine("Stopping after first match as configured");
                        break;
                    }
                } else {
                    LOGGER.finest("Mapping rule '" + rule.getId() + "' conditions not met, skipping");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to process mapping rule '" + rule.getId() + "': " + e.getMessage(), e);
            }
        }

        return targetObject;
    }

    /**
     * Evaluate a condition group with OR/AND logic.
     *
     * @param conditionGroup The condition group to evaluate
     * @param targetObject The target object for context
     * @return true if conditions are met, false otherwise
     */
    private boolean evaluateConditionGroup(YamlEnrichment.ConditionGroup conditionGroup, Object targetObject) {
        if (conditionGroup == null || conditionGroup.getRules() == null || conditionGroup.getRules().isEmpty()) {
            LOGGER.fine("No conditions to evaluate, returning true");
            return true;
        }

        String operator = conditionGroup.getOperator();
        if (operator == null) {
            operator = "AND"; // Default to AND if not specified
        }

        LOGGER.finest("Evaluating condition group with operator: " + operator);

        StandardEvaluationContext context = createEvaluationContext(targetObject);

        boolean result;
        if ("OR".equalsIgnoreCase(operator)) {
            result = evaluateOrConditions(conditionGroup.getRules(), context);
        } else if ("AND".equalsIgnoreCase(operator)) {
            result = evaluateAndConditions(conditionGroup.getRules(), context);
        } else {
            LOGGER.warning("Unknown condition operator: " + operator + ", defaulting to AND");
            result = evaluateAndConditions(conditionGroup.getRules(), context);
        }

        LOGGER.fine("Condition group evaluation result: " + result);
        return result;
    }

    /**
     * Evaluate conditions with OR logic.
     */
    private boolean evaluateOrConditions(List<YamlEnrichment.ConditionRule> rules, StandardEvaluationContext context) {
        for (YamlEnrichment.ConditionRule rule : rules) {
            try {
                if (evaluateConditionRule(rule, context)) {
                    LOGGER.finest("OR condition met: " + rule.getCondition());
                    return true; // Short-circuit on first true condition
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to evaluate OR condition: " + rule.getCondition() + " - " + e.getMessage(), e);
            }
        }
        return false; // No conditions were true
    }

    /**
     * Evaluate conditions with AND logic.
     */
    private boolean evaluateAndConditions(List<YamlEnrichment.ConditionRule> rules, StandardEvaluationContext context) {
        for (YamlEnrichment.ConditionRule rule : rules) {
            try {
                if (!evaluateConditionRule(rule, context)) {
                    LOGGER.finest("AND condition failed: " + rule.getCondition());
                    return false; // Short-circuit on first false condition
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to evaluate AND condition: " + rule.getCondition() + " - " + e.getMessage(), e);
                return false; // Treat evaluation errors as false for AND logic
            }
        }
        return true; // All conditions were true
    }

    /**
     * Evaluate a single condition rule.
     */
    private boolean evaluateConditionRule(YamlEnrichment.ConditionRule rule, StandardEvaluationContext context) {
        if (rule.getCondition() == null || rule.getCondition().trim().isEmpty()) {
            return true; // Empty condition is considered true
        }

        try {
            Expression expression = parser.parseExpression(rule.getCondition());
            Object result = expression.getValue(context);

            // Convert result to boolean
            if (result instanceof Boolean) {
                return (Boolean) result;
            } else if (result != null) {
                // Non-null values are considered true
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to evaluate condition: " + rule.getCondition() + " - " + e.getMessage(), e);
            return false;
        }
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
        Object result = lookupService.transform(lookupKey);
        
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

        // Track if any required field mapping failed
        boolean hasRequiredFieldFailure = false;

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
                        LOGGER.severe("CRITICAL ERROR: Required field '" + mapping.getSourceField() +
                                    "' is missing from lookup result");
                        hasRequiredFieldFailure = true;
                        // Skip this mapping and continue with next one
                        continue;
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

        // Return null if any required field mapping failed to signal enrichment failure
        if (hasRequiredFieldFailure) {
            return null;
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

        // Handle regular objects using proper getter methods instead of reflection
        try {
            // Try to find a getter method first (proper OOP approach)
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method getter = object.getClass().getMethod(getterName);
            Object value = getter.invoke(object);
            LOGGER.finest("Getter method lookup for '" + fieldName + "' returned: " + value);
            return value;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Try boolean getter (isXxx)
            try {
                String booleanGetterName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                Method booleanGetter = object.getClass().getMethod(booleanGetterName);
                Object value = booleanGetter.invoke(object);
                LOGGER.finest("Boolean getter method lookup for '" + fieldName + "' returned: " + value);
                return value;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e2) {
                LOGGER.fine("No getter method found for field '" + fieldName + "' on object of type " +
                           object.getClass().getSimpleName());
                return null;
            }
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

        // Handle regular objects using proper setter methods instead of reflection
        try {
            // Try to find a setter method first (proper OOP approach)
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method setter = object.getClass().getMethod(setterName, value.getClass());
            setter.invoke(object, value);
            LOGGER.finest("Successfully set field '" + fieldName + "' to: " + value);
        } catch (NoSuchMethodException e) {
            // Try with different parameter types if exact match fails
            try {
                String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                Method[] methods = object.getClass().getMethods();
                for (Method method : methods) {
                    if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                        Class<?> paramType = method.getParameterTypes()[0];
                        if (paramType.isAssignableFrom(value.getClass())) {
                            method.invoke(object, value);
                            LOGGER.finest("Successfully set field '" + fieldName + "' to: " + value);
                            return;
                        }
                    }
                }
                LOGGER.warning("No suitable setter method found for field '" + fieldName + "' on object of type " +
                              object.getClass().getSimpleName());
            } catch (IllegalAccessException | InvocationTargetException e2) {
                LOGGER.log(Level.WARNING, "Could not invoke setter for field '" + fieldName + "' on object of type " +
                          object.getClass().getSimpleName() + ": " + e2.getMessage(), e2);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.log(Level.WARNING, "Could not invoke setter for field '" + fieldName + "' on object of type " +
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

        // Add custom property accessor for Maps to enable nested field access
        context.addPropertyAccessor(new dev.mars.apex.core.engine.config.MapPropertyAccessor());

        // Add common variables and functions
        context.setVariable("serviceRegistry", serviceRegistry);

        // Add rule results for conditional mapping support (only if they exist)
        if (!ruleGroupResults.isEmpty()) {
            context.setVariable("ruleGroupResults", ruleGroupResults);
        }
        if (!individualRuleResults.isEmpty()) {
            context.setVariable("ruleResults", individualRuleResults);
        }

        // If the root object is a Map, add its entries as variables for easier access
        if (rootObject instanceof Map) {
            Map<?, ?> rootMap = (Map<?, ?>) rootObject;
            for (Map.Entry<?, ?> entry : rootMap.entrySet()) {
                if (entry.getKey() instanceof String) {
                    context.setVariable((String) entry.getKey(), entry.getValue());
                }
            }
        }

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
                    .createDatasetLookupService(datasetServiceName, dataset, this.currentConfiguration);

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
     * Process rules and rule groups to populate rule results for conditional mapping.
     *
     * @param configuration The YAML configuration containing rules and rule groups
     * @param targetObject The object to evaluate rules against
     */
    private void processRulesAndRuleGroups(dev.mars.apex.core.config.yaml.YamlRuleConfiguration configuration, Object targetObject) {
        // Clear previous results
        ruleGroupResults.clear();
        individualRuleResults.clear();

        LOGGER.fine("Processing rules and rule groups for conditional mapping...");

        try {
            StandardEvaluationContext context = createEvaluationContext(targetObject);

            // Process individual rules first
            if (configuration.getRules() != null) {
                LOGGER.fine("Processing " + configuration.getRules().size() + " individual rules...");
                for (YamlRule yamlRule : configuration.getRules()) {
                    try {
                        // Create Rule object from YAML configuration
                        Rule rule = new Rule(yamlRule.getName() != null ? yamlRule.getName() : yamlRule.getId(),
                                           yamlRule.getCondition(),
                                           yamlRule.getMessage() != null ? yamlRule.getMessage() : "Rule " + yamlRule.getId());

                        // Evaluate rule
                        Expression exp = getOrCompileExpression(rule.getCondition());
                        Boolean result = exp.getValue(context, Boolean.class);

                        if (result == null) {
                            result = false;
                        }

                        // Store individual rule result using YAML rule ID
                        individualRuleResults.put(yamlRule.getId(), result);

                        LOGGER.fine("Rule '" + yamlRule.getId() + "' evaluated to: " + result);

                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error evaluating rule '" + yamlRule.getId() + "': " + e.getMessage(), e);
                        individualRuleResults.put(yamlRule.getId(), false);
                    }
                }
            }

            // Process rule groups
            if (configuration.getRuleGroups() != null) {
                LOGGER.fine("Processing " + configuration.getRuleGroups().size() + " rule groups...");
                for (YamlRuleGroup yamlRuleGroup : configuration.getRuleGroups()) {
                    try {
                        // Create RuleGroup object from YAML configuration
                        boolean isAndOperator = "AND".equalsIgnoreCase(yamlRuleGroup.getOperator());
                        RuleGroup ruleGroup = new RuleGroup(
                            yamlRuleGroup.getId(),
                            "default",
                            yamlRuleGroup.getName(),
                            yamlRuleGroup.getDescription(),
                            yamlRuleGroup.getPriority() != null ? yamlRuleGroup.getPriority() : 100,
                            isAndOperator,
                            yamlRuleGroup.getStopOnFirstFailure() != null ? yamlRuleGroup.getStopOnFirstFailure() : false,
                            yamlRuleGroup.getParallelExecution() != null ? yamlRuleGroup.getParallelExecution() : false,
                            yamlRuleGroup.getDebugMode() != null ? yamlRuleGroup.getDebugMode() : false
                        );

                        // Add rules to the group
                        if (yamlRuleGroup.getRuleIds() != null) {
                            int sequence = 1;
                            for (String ruleId : yamlRuleGroup.getRuleIds()) {
                                // Find the rule in the configuration
                                YamlRule yamlRule = findRuleById(configuration, ruleId);
                                if (yamlRule != null) {
                                    Rule rule = new Rule(yamlRule.getName() != null ? yamlRule.getName() : yamlRule.getId(),
                                                       yamlRule.getCondition(),
                                                       yamlRule.getMessage() != null ? yamlRule.getMessage() : "Rule " + yamlRule.getId());
                                    ruleGroup.addRule(rule, sequence++);
                                }
                            }
                        }

                        // Evaluate rule group
                        boolean groupResult = ruleGroup.evaluate(context);

                        // Store rule group results
                        Map<String, Boolean> groupRuleResults = new HashMap<>();
                        groupRuleResults.put("passed", groupResult);
                        groupRuleResults.putAll(ruleGroup.getRuleResults());
                        ruleGroupResults.put(yamlRuleGroup.getId(), groupRuleResults);

                        LOGGER.fine("Rule group '" + yamlRuleGroup.getId() + "' evaluated to: " + groupResult);

                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error evaluating rule group '" + yamlRuleGroup.getId() + "': " + e.getMessage(), e);
                        Map<String, Boolean> failedResult = new HashMap<>();
                        failedResult.put("passed", false);
                        ruleGroupResults.put(yamlRuleGroup.getId(), failedResult);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing rules and rule groups: " + e.getMessage(), e);
        }
    }

    /**
     * Find a rule by ID in the configuration.
     *
     * @param configuration The YAML configuration
     * @param ruleId The rule ID to find
     * @return The YamlRule if found, null otherwise
     */
    private YamlRule findRuleById(dev.mars.apex.core.config.yaml.YamlRuleConfiguration configuration, String ruleId) {
        if (configuration.getRules() != null) {
            for (YamlRule rule : configuration.getRules()) {
                if (ruleId.equals(rule.getId())) {
                    return rule;
                }
            }
        }
        return null;
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

    /**
     * Evaluate conditions for a mapping rule.
     *
     * @param rule The mapping rule
     * @param targetObject The target object for context
     * @return true if conditions are met, false otherwise
     */
    private boolean evaluateMappingRuleConditions(YamlEnrichment.MappingRule rule, Object targetObject) {
        YamlEnrichment.ConditionGroup conditions = rule.getConditions();

        // If no conditions specified, this is a default rule that always matches
        if (conditions == null) {
            LOGGER.finest("No conditions specified for rule '" + rule.getId() + "', treating as default rule");
            return true;
        }

        // Use existing condition group evaluation logic
        return evaluateConditionGroup(conditions, targetObject);
    }

    /**
     * Apply a mapping rule to get the mapped value.
     *
     * @param rule The mapping rule
     * @param targetObject The target object for context
     * @return The mapped value
     */
    private Object applyMappingRule(YamlEnrichment.MappingRule rule, Object targetObject) {
        YamlEnrichment.MappingConfig mapping = rule.getMapping();

        if (mapping == null) {
            LOGGER.warning("Mapping rule '" + rule.getId() + "' has no mapping configuration");
            return null;
        }

        String mappingType = mapping.getType();
        if (mappingType == null) {
            mappingType = "direct"; // Default to direct mapping
        }

        try {
            if ("direct".equalsIgnoreCase(mappingType)) {
                return applyDirectMapping(mapping, targetObject);
            } else if ("lookup".equalsIgnoreCase(mappingType)) {
                return applyLookupMapping(mapping, targetObject);
            } else {
                LOGGER.warning("Unknown mapping type '" + mappingType + "' for rule: " + rule.getId());
                return null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to apply mapping for rule '" + rule.getId() + "': " + e.getMessage(), e);

            // Try fallback value if available
            if (mapping.getFallbackValue() != null && !mapping.getFallbackValue().trim().isEmpty()) {
                try {
                    StandardEvaluationContext context = createEvaluationContext(targetObject);
                    Expression fallbackExpr = getOrCompileExpression(mapping.getFallbackValue());
                    return fallbackExpr.getValue(context);
                } catch (Exception fallbackException) {
                    LOGGER.log(Level.WARNING, "Failed to apply fallback value: " + fallbackException.getMessage(), fallbackException);
                }
            }

            return null;
        }
    }

    /**
     * Apply direct mapping (source field with optional transformation).
     */
    private Object applyDirectMapping(YamlEnrichment.MappingConfig mapping, Object targetObject) {
        StandardEvaluationContext context = createEvaluationContext(targetObject);

        // If transformation is specified, use it
        if (mapping.getTransformation() != null && !mapping.getTransformation().trim().isEmpty()) {
            Expression transformExpr = getOrCompileExpression(mapping.getTransformation());
            return transformExpr.getValue(context);
        }

        // Otherwise, use source field directly
        if (mapping.getSourceField() != null && !mapping.getSourceField().trim().isEmpty()) {
            Expression sourceExpr = getOrCompileExpression("#" + mapping.getSourceField());
            return sourceExpr.getValue(context);
        }

        LOGGER.warning("Direct mapping has neither transformation nor source-field");
        return null;
    }

    /**
     * Apply lookup mapping (database/external lookup with transformation).
     */
    private Object applyLookupMapping(YamlEnrichment.MappingConfig mapping, Object targetObject) {
        // This is a simplified implementation - in a full implementation,
        // you would use the lookup-config to perform the actual lookup
        LOGGER.warning("Lookup mapping not fully implemented yet for conditional-mapping-enrichment");

        // For now, fall back to transformation if available
        if (mapping.getTransformation() != null && !mapping.getTransformation().trim().isEmpty()) {
            StandardEvaluationContext context = createEvaluationContext(targetObject);
            Expression transformExpr = getOrCompileExpression(mapping.getTransformation());
            return transformExpr.getValue(context);
        }

        return null;
    }

    // ========================================
    // RuleResult-returning methods (Phase 4)
    // ========================================

    /**
     * Process a list of enrichments on a target object and return detailed results.
     * This method provides programmatic access to enrichment success/failure status and detailed error information.
     *
     * @param enrichments List of YAML enrichment configurations
     * @param targetObject The object to enrich
     * @return A RuleResult containing success status, enriched data, and failure messages
     */
    public RuleResult processEnrichmentsWithResult(List<YamlEnrichment> enrichments, Object targetObject) {
        return processEnrichmentsWithResult(enrichments, targetObject, null);
    }

    /**
     * Process a list of enrichments on a target object with full configuration context and return detailed results.
     * This method provides programmatic access to enrichment success/failure status and detailed error information.
     *
     * @param enrichments The list of enrichments to apply
     * @param targetObject The object to enrich
     * @param configuration The full YAML configuration (required for database lookups)
     * @return A RuleResult containing success status, enriched data, and failure messages
     */
    public RuleResult processEnrichmentsWithResult(List<YamlEnrichment> enrichments, Object targetObject,
                                                  dev.mars.apex.core.config.yaml.YamlRuleConfiguration configuration) {
        LOGGER.fine("Processing enrichments with result tracking for " + (enrichments != null ? enrichments.size() : 0) + " enrichments");

        // Convert target object to Map for consistent handling
        Map<String, Object> originalData = convertToMap(targetObject);
        List<String> failureMessages = new ArrayList<>();
        boolean overallSuccess = true;

        try {
            // Process enrichments using existing method
            Object enrichmentResult = processEnrichments(enrichments, targetObject, configuration);

            // Convert result to Map for analysis
            Map<String, Object> enrichedData = convertToMap(enrichmentResult);

            // Detect enrichment failures by checking for required field mapping failures
            if (enrichments != null && !enrichments.isEmpty()) {
                boolean enrichmentFailed = detectEnrichmentFailures(enrichments, enrichedData);

                if (enrichmentFailed) {
                    overallSuccess = false;
                    failureMessages.add("Required field enrichment failed - check logs for CRITICAL ERROR details");
                    LOGGER.warning("Enrichment failed due to required field mapping failures");
                }
            }

            // Aggregate severity from processed enrichments
            String aggregatedSeverity = aggregateEnrichmentSeverity(enrichments, overallSuccess);

            // Return appropriate RuleResult with aggregated severity
            if (overallSuccess) {
                LOGGER.fine("Enrichment processing completed successfully with severity: " + aggregatedSeverity);
                return RuleResult.enrichmentSuccess(enrichedData, aggregatedSeverity);
            } else {
                LOGGER.warning("Enrichment processing completed with failures, severity: " + aggregatedSeverity);
                return RuleResult.enrichmentFailure(failureMessages, enrichedData, aggregatedSeverity);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception during enrichment processing: " + e.getMessage(), e);
            failureMessages.add("Enrichment processing exception: " + e.getMessage());
            return RuleResult.enrichmentFailure(failureMessages, originalData, SeverityConstants.ERROR);
        }
    }

    /**
     * Process a single enrichment on a target object and return detailed results.
     * This method provides programmatic access to enrichment success/failure status and detailed error information.
     *
     * @param enrichment The YAML enrichment configuration
     * @param targetObject The object to enrich
     * @return A RuleResult containing success status, enriched data, and failure messages
     */
    public RuleResult processEnrichmentWithResult(YamlEnrichment enrichment, Object targetObject) {
        if (enrichment == null) {
            LOGGER.fine("No enrichment provided");
            Map<String, Object> resultData = convertToMap(targetObject);
            return RuleResult.enrichmentSuccess(resultData);
        }

        List<YamlEnrichment> enrichmentList = new ArrayList<>();
        enrichmentList.add(enrichment);
        return processEnrichmentsWithResult(enrichmentList, targetObject, null);
    }

    /**
     * Detect enrichment failures by checking if required fields were successfully enriched.
     * This method examines the enrichment configuration and checks if required target fields
     * are present in the enriched data.
     *
     * @param enrichments The list of enrichments that were processed
     * @param enrichedData The enriched data map
     * @return true if enrichment failures were detected, false otherwise
     */
    private boolean detectEnrichmentFailures(List<YamlEnrichment> enrichments, Map<String, Object> enrichedData) {
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

    /**
     * Aggregate severity from a list of enrichments.
     * This method determines the overall severity based on the enrichments processed
     * and whether the processing was successful.
     *
     * @param enrichments The list of enrichments that were processed
     * @param overallSuccess Whether the enrichment processing was successful
     * @return The aggregated severity level
     */
    private String aggregateEnrichmentSeverity(List<YamlEnrichment> enrichments, boolean overallSuccess) {
        if (enrichments == null || enrichments.isEmpty()) {
            return SeverityConstants.INFO;
        }

        // If processing failed, use ERROR severity
        if (!overallSuccess) {
            return SeverityConstants.ERROR;
        }

        // Find the highest severity among all enrichments
        String highestSeverity = SeverityConstants.INFO;
        int highestPriority = SeverityConstants.SEVERITY_PRIORITY.get(SeverityConstants.INFO);

        for (YamlEnrichment enrichment : enrichments) {
            String enrichmentSeverity = enrichment.getSeverity();
            if (enrichmentSeverity == null) {
                enrichmentSeverity = SeverityConstants.INFO; // Default severity
            }

            Integer priority = SeverityConstants.SEVERITY_PRIORITY.get(enrichmentSeverity);
            if (priority != null && priority > highestPriority) {
                highestSeverity = enrichmentSeverity;
                highestPriority = priority;
            }
        }

        LOGGER.fine("Aggregated enrichment severity: " + highestSeverity + " from " + enrichments.size() + " enrichments");
        return highestSeverity;
    }
}
