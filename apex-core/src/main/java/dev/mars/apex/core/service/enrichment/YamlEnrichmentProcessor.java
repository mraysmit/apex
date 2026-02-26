package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.cache.ApexCacheManager;
import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.engine.core.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.DatasetLookupService;
import dev.mars.apex.core.service.lookup.DatasetLookupServiceFactory;
import dev.mars.apex.core.service.lookup.DatasetSignature;
import dev.mars.apex.core.service.lookup.LookupService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.engine.model.Rule;
import dev.mars.apex.engine.core.RuleBuilder;
import dev.mars.apex.engine.model.RuleGroup;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.engine.model.EnrichmentGroup;
import dev.mars.apex.engine.model.EnrichmentGroupResult;
import dev.mars.apex.core.config.model.YamlRule;
import dev.mars.apex.core.config.model.YamlRuleGroup;
import dev.mars.apex.engine.execution.RuleGroupEvaluationService;
import dev.mars.apex.core.service.data.external.cache.CacheStatistics;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    
    private static final Logger logger = LoggerFactory.getLogger(YamlEnrichmentProcessor.class);

    private final LookupServiceRegistry serviceRegistry;
    private final ExpressionEvaluatorService evaluatorService;
    private final SpelExpressionParser parser;

    // Unified cache manager for all caching needs
    private final ApexCacheManager cacheManager;

    // Data source registry from RulesEngine - prevents duplicate data source creation
    private final Map<String, dev.mars.apex.core.service.data.external.ExternalDataSource> dataSourceRegistry;

    // Rule group evaluation service for canonical evaluation path (Phase 2)
    private final RuleGroupEvaluationService ruleGroupEvaluationService;

    // Current configuration context for database lookups
    private dev.mars.apex.core.config.model.YamlRuleConfiguration currentConfiguration;

    // Rule result tracking for conditional mapping support
    private final Map<String, Map<String, Object>> ruleGroupResults = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Boolean> individualRuleResults = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Constructor with all required dependencies.
     *
     * @param serviceRegistry The lookup service registry
     * @param evaluatorService The expression evaluator service
     * @param dataSourceRegistry Data source registry from RulesEngine (null-safe, defaults to empty map)
     * @param ruleGroupEvaluationService Service for canonical rule group evaluation (required)
     */
    public YamlEnrichmentProcessor(LookupServiceRegistry serviceRegistry,
                                   ExpressionEvaluatorService evaluatorService,
                                   Map<String, dev.mars.apex.core.service.data.external.ExternalDataSource> dataSourceRegistry,
                                   RuleGroupEvaluationService ruleGroupEvaluationService) {
        this.serviceRegistry = serviceRegistry;
        this.evaluatorService = evaluatorService;
        this.parser = new SpelExpressionParser();
        this.cacheManager = ApexCacheManager.getInstance();
        this.dataSourceRegistry = dataSourceRegistry != null ? dataSourceRegistry : new java.util.HashMap<>();
        this.ruleGroupEvaluationService = java.util.Objects.requireNonNull(ruleGroupEvaluationService,
                "RuleGroupEvaluationService is required — use RuleGroupEvaluationService(new UnifiedRuleEvaluator())");

        logger.info("YamlEnrichmentProcessor initialized with unified cache manager" + 
                   (dataSourceRegistry != null ? " and data source registry (" + dataSourceRegistry.size() + " data sources)" : "") +
                   " and RuleGroupEvaluationService");
    }
    
    /**
     * Process a single enrichment on a target object.
     *
     * @param enrichment The YAML enrichment configuration
     * @param targetObject The object to enrich
     * @return The enriched object
     */
    private Object processEnrichment(YamlEnrichment enrichment, Object targetObject) {
        logger.debug("Processing enrichment: " + enrichment.getId() + " (type: " + enrichment.getType() + ")");

        // Check if enrichment should be processed
        boolean conditionMatched = shouldProcessEnrichment(enrichment, targetObject);
        if (!conditionMatched) {
            logger.debug("Enrichment " + enrichment.getId() + " should not be processed");

            // Evaluate error code when condition doesn't match
            if (enrichment.getErrorCode() != null) {
                StandardEvaluationContext context = createEvaluationContext(targetObject);
                String evaluatedErrorCode = evaluateCode(enrichment.getErrorCode(), context);

                // Apply field mappings with error code
                if (enrichment.getMapToField() != null) {
                    applyCodeFieldMappings(enrichment.getMapToField(), context, targetObject, null, evaluatedErrorCode);
                }

                logger.debug("Enrichment condition not met, error code evaluated: " + evaluatedErrorCode);
            }

            return targetObject;
        }

        logger.debug("Enrichment " + enrichment.getId() + " passed conditions, proceeding with processing");

        // Store result-field for field-enrichment (condition matched)
        if ("field-enrichment".equals(enrichment.getType()) && enrichment.getResultField() != null) {
            setFieldValue(targetObject, enrichment.getResultField(), true);
            logger.info("Stored field-enrichment result in field: " + enrichment.getResultField() + " = true");
        }

        // Process the enrichment based on type
        Object result;
        switch (enrichment.getType()) {
            case "lookup-enrichment":
                result = processLookupEnrichment(enrichment, targetObject);
                break;
            case "calculation-enrichment":
                result = processCalculationEnrichment(enrichment, targetObject);
                break;
            case "field-enrichment":
                result = processFieldEnrichment(enrichment, targetObject);
                break;
            case "conditional-mapping-enrichment":
                result = processConditionalMappingEnrichment(enrichment, targetObject);
                break;
            default:
                logger.warn("Unknown enrichment type: " + enrichment.getType());
                result = targetObject;
        }

        // Evaluate success code when enrichment succeeds
        if (enrichment.getSuccessCode() != null) {
            StandardEvaluationContext context = createEvaluationContext(result);
            String evaluatedSuccessCode = evaluateCode(enrichment.getSuccessCode(), context);

            // Apply field mappings with success code
            if (enrichment.getMapToField() != null) {
                applyCodeFieldMappings(enrichment.getMapToField(), context, result, evaluatedSuccessCode, null);
            }

            logger.debug("Enrichment succeeded, success code evaluated: " + evaluatedSuccessCode);
        }

        return result;
    }
    
    /**
     * Check if an enrichment should be processed based on its condition.
     *
     * @param enrichment The enrichment configuration
     * @param targetObject The target object (can be null)
     * @return true if the enrichment should be processed
     */
    private boolean shouldProcessEnrichment(YamlEnrichment enrichment, Object targetObject) {
        // Defensive null check - fail fast if targetObject is null
        if (targetObject == null) {
            logger.warn("Cannot process enrichment '" + enrichment.getId() +
                          "' - target object is null. Skipping enrichment.");
            return false;
        }

        // Check if enrichment is enabled
        if (!dev.mars.apex.core.util.EnabledFilter.isEnabled(enrichment)) {
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
                return false;
            }
        }

        // Evaluate condition if specified
        if (enrichment.getCondition() != null && !enrichment.getCondition().trim().isEmpty()) {
            try {
                StandardEvaluationContext context = createEvaluationContext(targetObject);
                Expression conditionExpr = getOrCompileExpression(enrichment.getCondition());
                Boolean result = conditionExpr.getValue(context, Boolean.class);

                return result != null && result;
            } catch (Exception e) {
                // Condition evaluation failure is a configuration error — propagate to caller
                // The caller's catch block will properly record this in failureMessages/RuleResult
                logger.error("[APEX-ENRICH-005] Enrichment condition evaluation failed for '{}' - condition: '{}' - Error: {}",
                          enrichment.getId(), enrichment.getCondition(), e.getMessage());
                logger.debug("Full stack trace for condition evaluation failure:", e);
                throw new EnrichmentException(
                    "Condition evaluation failed for enrichment '" + enrichment.getId() + "': " + e.getMessage(), e);
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
            logger.warn("Lookup enrichment '" + enrichment.getId() + "' has no lookup configuration");
            return targetObject;
        }
        
        // 1. Resolve lookup service (either from registry or create from dataset)
        LookupService lookupService = resolveLookupService(enrichment.getId(), lookupConfig);

        logger.debug("Processing lookup enrichment with service: " + lookupService.getName());
        
        // 2. Extract lookup key using SpEL expression
        Object lookupKey;
        try {
            StandardEvaluationContext context = createEvaluationContext(targetObject);
            Expression keyExpr = getOrCompileExpression(lookupConfig.getLookupKey());
            lookupKey = keyExpr.getValue(context);

            if (lookupKey == null) {
                logger.error("LOOKUP KEY EVALUATION FAILED: Lookup key expression '" + lookupConfig.getLookupKey() +
                           "' evaluated to NULL for enrichment '" + enrichment.getId() + "'. " +
                           "Check: (1) expression syntax is correct, (2) referenced fields exist in target object, " +
                           "(3) field values are not null. Enrichment will be skipped.");
                return targetObject;
            }

            logger.debug("Extracted lookup key: " + lookupKey);
        } catch (Exception e) {
            throw new EnrichmentException("Failed to extract lookup key using expression '" +
                                        lookupConfig.getLookupKey() + "'", e);
        }
        
        // 3. Perform lookup (with caching if enabled)
        Object lookupResult = performLookup(lookupService, lookupKey, lookupConfig);

        logger.debug("Lookup result for key '" + lookupKey + "': " + lookupResult +
                   " (type: " + (lookupResult != null ? lookupResult.getClass().getSimpleName() : "null") + ")");

        if (lookupResult == null) {
            logger.debug("Lookup returned null result for key: " + lookupKey + ", applying default values");
        }

        // Store result-field if configured (boolean indicating lookup success)
        boolean lookupSucceeded = (lookupResult != null);
        if (enrichment.getResultField() != null) {
            setFieldValue(targetObject, enrichment.getResultField(), lookupSucceeded);
            logger.info("Stored lookup result in field: " + enrichment.getResultField() + " = " + lookupSucceeded);
        }

        // 4. Apply field mappings (even if lookup result is null, to apply default values)
        Object result = applyFieldMappings(enrichment.getFieldMappings(), lookupResult, targetObject);

        // If applyFieldMappings returns null, it means a required field mapping failed
        if (result == null) {
            // CRITICAL: Field mapping failed - throw exception to propagate failure to RuleResult
            String errorMsg = "Lookup enrichment '" + enrichment.getId() + "' failed: one or more field mappings could not be applied. " +
                             "Check: (1) target paths exist, (2) intermediate structures are pre-created, (3) SpEL expressions are valid.";
            logger.error(errorMsg);
            throw new EnrichmentException(errorMsg);
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
            logger.warn("Calculation enrichment '" + enrichment.getId() + "' has no calculation configuration");
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

            // Apply field mappings (if present) - this was missing!
            if (enrichment.getFieldMappings() != null && !enrichment.getFieldMappings().isEmpty()) {
                Object mappedResult = applyFieldMappings(enrichment.getFieldMappings(), targetObject, targetObject);
                if (mappedResult == null) {
                    // CRITICAL: Field mapping failed - throw exception to propagate failure to RuleResult
                    String errorMsg = "Calculation enrichment '" + enrichment.getId() + "' failed: one or more field mappings could not be applied. " +
                                     "Check: (1) target paths exist, (2) intermediate structures are pre-created, (3) SpEL expressions are valid.";
                    logger.error(errorMsg);
                    throw new EnrichmentException(errorMsg);
                }
                targetObject = mappedResult;
            }

            logger.debug("Calculation enrichment completed. Result: " + result);
            return targetObject;

        } catch (Exception e) {
            // Phase 3A Enhancement: Check if calculation has a default-value for error recovery
            if (calcConfig.getDefaultValue() != null) {
                logger.info("Using calculation default value for recovery: enrichment='" + enrichment.getId() +
                    "', defaultValue='" + calcConfig.getDefaultValue() + "'");

                // Set the default value in the result field
                if (calcConfig.getResultField() != null) {
                    setFieldValue(targetObject, calcConfig.getResultField(), calcConfig.getDefaultValue());
                }

                // Apply field mappings even with default value
                if (enrichment.getFieldMappings() != null && !enrichment.getFieldMappings().isEmpty()) {
                    Object mappedResult = applyFieldMappings(enrichment.getFieldMappings(), targetObject, targetObject);
                    if (mappedResult != null) {
                        targetObject = mappedResult;
                    }
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
            Object result = applyFieldMappings(enrichment.getFieldMappings(), targetObject, targetObject);
            if (result != null) {
                targetObject = result;
            } else {
                // CRITICAL: Field mapping failed - throw exception to propagate failure to RuleResult
                String errorMsg = "Field enrichment '" + enrichment.getId() + "' failed: one or more field mappings could not be applied. " +
                                 "Check: (1) target paths exist, (2) intermediate structures are pre-created, (3) SpEL expressions are valid.";
                logger.error(errorMsg);
                throw new EnrichmentException(errorMsg);
            }
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
        logger.debug("Processing " + conditionalMappings.size() + " conditional mappings");

        for (YamlEnrichment.ConditionalMapping conditionalMapping : conditionalMappings) {
            try {
                // Evaluate condition group
                if (evaluateConditionGroup(conditionalMapping.getConditions(), targetObject)) {
                    logger.debug("Conditional mapping conditions met, applying field mappings");
                    // Apply field mappings for this conditional mapping
                    targetObject = applyFieldMappings(conditionalMapping.getFieldMappings(), targetObject, targetObject);
                    // Continue to next conditional mapping (don't break - multiple can apply)
                } else {
                    logger.trace("Conditional mapping conditions not met, skipping");
                }
            } catch (Exception e) {
                logger.error("[APEX-ENRICH-005] Failed to process conditional mapping: {}", e.getMessage());
                logger.debug("Stack trace for conditional mapping processing failure:", e);
                throw new EnrichmentException(
                    "Conditional mapping processing failed: " + e.getMessage(), e);
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
            logger.warn("Conditional mapping enrichment '" + enrichment.getId() + "' has no target field");
            return targetObject;
        }

        if (mappingRules == null || mappingRules.isEmpty()) {
            logger.warn("Conditional mapping enrichment '" + enrichment.getId() + "' has no mapping rules");
            return targetObject;
        }

        logger.debug("Processing conditional mapping enrichment for target field: " + targetField);

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

        // Track whether any mapping rule matched
        boolean anyRuleMatched = false;

        // Process rules in priority order
        for (YamlEnrichment.MappingRule rule : mappingRules) {
            try {
                // Check if rule conditions are met
                if (evaluateMappingRuleConditions(rule, targetObject)) {
                    anyRuleMatched = true;  // Track that a rule matched

                    if (logMatchedRule) {
                        logger.info("Matched mapping rule: " + rule.getId() + " (priority: " + rule.getPriority() + ")");
                    }

                    // Apply the mapping
                    Object mappedValue = applyMappingRule(rule, targetObject);

                    // Set the target field
                    setFieldValue(targetObject, targetField, mappedValue);

                    logger.debug("Applied mapping rule '" + rule.getId() + "' to field '" + targetField + "' with value: " + mappedValue);

                    // Stop on first match if configured to do so
                    if (stopOnFirstMatch) {
                        logger.debug("Stopping after first match as configured");
                        break;
                    }
                } else {
                    logger.trace("Mapping rule '" + rule.getId() + "' conditions not met, skipping");
                }
            } catch (Exception e) {
                logger.warn("Failed to process mapping rule '{}': {}", rule.getId(), e.getMessage());
                logger.debug("Stack trace for mapping rule processing failure:", e);
            }
        }

        // Store result-field if configured (boolean indicating if any mapping matched)
        if (enrichment.getResultField() != null) {
            setFieldValue(targetObject, enrichment.getResultField(), anyRuleMatched);
            logger.info("Stored conditional-mapping result in field: " + enrichment.getResultField() + " = " + anyRuleMatched);
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
            logger.debug("No conditions to evaluate, returning true");
            return true;
        }

        String operator = conditionGroup.getOperator();
        if (operator == null) {
            operator = "AND"; // Default to AND if not specified
        }

        logger.trace("Evaluating condition group with operator: " + operator);

        StandardEvaluationContext context = createEvaluationContext(targetObject);

        boolean result;
        if ("OR".equalsIgnoreCase(operator)) {
            result = evaluateOrConditions(conditionGroup.getRules(), context);
        } else if ("AND".equalsIgnoreCase(operator)) {
            result = evaluateAndConditions(conditionGroup.getRules(), context);
        } else {
            logger.warn("Unknown condition operator: " + operator + ", defaulting to AND");
            result = evaluateAndConditions(conditionGroup.getRules(), context);
        }

        logger.debug("Condition group evaluation result: " + result);
        return result;
    }

    /**
     * Evaluate conditions with OR logic.
     */
    private boolean evaluateOrConditions(List<YamlEnrichment.ConditionRule> rules, StandardEvaluationContext context) {
        for (YamlEnrichment.ConditionRule rule : rules) {
            try {
                if (evaluateConditionRule(rule, context)) {
                    logger.trace("OR condition met: " + rule.getCondition());
                    return true; // Short-circuit on first true condition
                }
            } catch (Exception e) {
                // ERROR: OR condition evaluation failure indicates configuration problem
                logger.error("[APEX-ENRICH-005] Failed to evaluate OR condition: '{}' - Error: {}",
                          rule.getCondition(), e.getMessage());
                logger.debug("Full stack trace for OR condition evaluation failure:", e);
                throw new EnrichmentException(
                    "OR condition evaluation failed: '" + rule.getCondition() + "': " + e.getMessage(), e);
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
                    logger.trace("AND condition failed: " + rule.getCondition());
                    return false; // Short-circuit on first false condition
                }
            } catch (Exception e) {
                // ERROR: AND condition evaluation failure indicates configuration problem
                logger.error("[APEX-ENRICH-005] Failed to evaluate AND condition: '{}' - Error: {}",
                          rule.getCondition(), e.getMessage());
                logger.debug("Full stack trace for AND condition evaluation failure:", e);
                throw new EnrichmentException(
                    "AND condition evaluation failed: '" + rule.getCondition() + "': " + e.getMessage(), e);
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
            // Condition evaluation failure indicates a configuration problem — propagate to caller
            logger.error("[APEX-ENRICH-006] Failed to evaluate condition rule: '{}' - Error: {}",
                      rule.getCondition(), e.getMessage());
            logger.debug("Full exception details for condition rule evaluation failure:", e);
            throw new EnrichmentException(
                "[APEX-ENRICH-006] Condition rule evaluation failed for '" + rule.getCondition() + "': " + e.getMessage(), e);
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
            Object cached = cacheManager.get(ApexCacheManager.LOOKUP_RESULT_CACHE, cacheKey);
            if (cached != null) {
                logger.trace("Cache hit for lookup key: " + lookupKey);
                return cached;
            }
        }

        // Perform actual lookup
        Object result = lookupService.transform(lookupKey);

        // Cache result if caching is enabled
        if (lookupConfig.getCacheEnabled() != null && lookupConfig.getCacheEnabled()) {
            long ttlSeconds = lookupConfig.getCacheTtlSeconds() != null ?
                           lookupConfig.getCacheTtlSeconds() : 300L;
            cacheManager.put(ApexCacheManager.LOOKUP_RESULT_CACHE, cacheKey, result, ttlSeconds);
            logger.trace("Cached lookup result for key: " + lookupKey);
        }

        return result;
    }

    /**
     * Apply field mappings from lookup result to target object.
     *
     * <p><strong>Null Value Handling (v2.1):</strong></p>
     * <ul>
     *   <li>Required fields with null values → Enrichment fails</li>
     *   <li>Optional fields with null values → Attempts to set null; skips if setter incompatible (e.g., primitives)</li>
     *   <li>Non-null values that fail to set → Enrichment fails</li>
     * </ul>
     *
     * @param fieldMappings The field mapping configurations
     * @param sourceObject The source object (lookup result)
     * @param targetObject The target object to enrich
     * @return The enriched target object, or null if any required field mapping failed
     */
    private Object applyFieldMappings(List<YamlEnrichment.FieldMapping> fieldMappings,
                                     Object sourceObject, Object targetObject) {
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            logger.debug("No field mappings to apply");
            return targetObject;
        }

        // Check if source object is a simple value (failed lookup)
        boolean isFailedLookup = sourceObject != null && !(sourceObject instanceof Map) &&
                                sourceObject.getClass().equals(String.class);

        // Track if any required field mapping failed
        boolean hasRequiredFieldFailure = false;

        if (isFailedLookup) {
            logger.debug("Source object is a simple value (likely failed lookup), applying only default values");
        }

        logger.debug("Applying " + fieldMappings.size() + " field mappings from " +
                   (sourceObject != null ? sourceObject.getClass().getSimpleName() : "null") +
                   " to " + targetObject.getClass().getSimpleName());

        for (YamlEnrichment.FieldMapping mapping : fieldMappings) {
            try {
                logger.debug("Processing field mapping: source-field='" + mapping.getSourceField() +
                           "' -> target-field='" + mapping.getTargetField() +
                           "', expression='" + mapping.getExpression() +
                           "', default-value='" + mapping.getDefaultValue() + "'");

                Object sourceValue = null;
                boolean isConstantMapping = "constant".equals(mapping.getSourceField());
                boolean isImplicitConstant = mapping.getSourceField() == null || mapping.getSourceField().trim().isEmpty();

                // For constant mappings (explicit or implicit), skip field lookup and directly evaluate expression
                if (isConstantMapping || isImplicitConstant) {
                    logger.debug("Constant mapping detected (explicit=" + isConstantMapping + "), will evaluate expression directly");
                    // For constant mappings, expression/transformation is required
                    if (mapping.getExpression() == null || mapping.getExpression().trim().isEmpty()) {
                        logger.error("FIELD MAPPING FAILED: source-field 'constant' (or missing) requires 'expression' or 'transformation' to be specified for target-field '" + mapping.getTargetField() + "'");
                        continue;
                    }
                } else if (!isFailedLookup) {
                    // For non-constant mappings, extract source value from source object
                    sourceValue = getFieldValue(sourceObject, mapping.getSourceField());
                    logger.debug("Source value for '" + mapping.getSourceField() + "': " + sourceValue);
                }

                // Use default value if source value is null (or for failed lookups)
                Object valueToSet = sourceValue != null ? sourceValue : mapping.getDefaultValue();
                logger.trace("Value to set (after defaults): " + valueToSet);

                // Apply expression if specified
                if (mapping.getExpression() != null && !mapping.getExpression().trim().isEmpty()) {
                    valueToSet = applyExpression(mapping.getExpression(), valueToSet, targetObject);
                    logger.trace("Value after expression: " + valueToSet);
                }

                // Consolidated handling for setting mapped value (including nulls)
                String targetField = mapping.getTargetField();
                String sourceField = mapping.getSourceField();
                boolean isRequired = mapping.getRequired() != null && mapping.getRequired();

                if (valueToSet == null && isRequired) {
                    // Required field produced null - this is a failure
                    logger.error("REQUIRED FIELD MAPPING FAILED: source-field '" + sourceField +
                               "' -> target-field '" + targetField +
                               "' produced NULL value but field is marked as required. " +
                               "Check: (1) source field exists, (2) expression is valid, (3) default-value is provided.");
                    hasRequiredFieldFailure = true;
                } else {
                    // BEHAVIORAL CHANGE (v2.1): Now attempts to set null values on optional fields
                    // Previous versions skipped null values entirely. This allows clearing fields.
                    // Attempt to set the value (may be null for non-required mappings)
                    boolean setSuccess = setFieldValue(targetObject, targetField, valueToSet);
                    if (setSuccess) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Successfully mapped field: " + sourceField + " -> " +
                                       targetField + " (value: " + valueToSet + ")");
                        }
                    } else {
                        if (valueToSet == null) {
                            // If setting null fails (e.g., primitive target), log and continue without failing the enrichment
                            if (logger.isDebugEnabled()) {
                                logger.debug("Skipping null set for non-required target field '" + targetField + "' (setter not compatible or missing)");
                            }
                        } else {
                            // Non-null value failed to set - this is a serious error
                            logger.error("FIELD SET FAILED: source-field '" + sourceField +
                                       "' -> target-field '" + targetField +
                                       "' with value '" + valueToSet + "'. The setFieldValue operation failed. " +
                                       "Check: (1) target path exists, (2) intermediate structures are pre-created, " +
                                       "(3) SpEL expression is valid. This failure will be reported in the RuleResult.");
                            hasRequiredFieldFailure = true;
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("FIELD MAPPING EXCEPTION: Failed to apply field mapping: " +
                          mapping.getSourceField() + " -> " + mapping.getTargetField() +
                          ": " + e.getMessage() + ". This failure will be reported in the RuleResult.", e);
                hasRequiredFieldFailure = true;
            }
        }

        // Return null if any field mapping failed to signal enrichment failure
        if (hasRequiredFieldFailure) {
            return null;
        }

        return targetObject;
    }

    /**
     * Apply expression to a value.
     *
     * @param expression The SpEL expression
     * @param value The value to transform
     * @param context The context object
     * @return The transformed value
     */
    private Object applyExpression(String expression, Object value, Object context) {
        try {
            StandardEvaluationContext evalContext = createEvaluationContext(context);
            evalContext.setVariable("value", value);

            Expression expr = getOrCompileExpression(expression);
            return expr.getValue(evalContext);

        } catch (Exception e) {
            logger.warn("Failed to apply expression '" + expression +
                      "' to value: " + value, e);
            return value; // Return original value on expression failure
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
            logger.debug("getFieldValue called with null object or fieldName");
            return null;
        }

        // NEW: If fieldName starts with #, treat it as a SpEL expression
        if (fieldName.startsWith("#")) {
            try {
                logger.trace("Evaluating SpEL expression for field: " + fieldName);
                StandardEvaluationContext context = createEvaluationContext(object);
                Expression expr = getOrCompileExpression(fieldName);
                Object value = expr.getValue(context);
                logger.trace("SpEL expression '" + fieldName + "' evaluated to: " + value);
                return value;
            } catch (Exception e) {
                logger.error("SPEL EXPRESSION EVALUATION FAILED: Failed to evaluate SpEL expression '" + fieldName +
                           "' for field lookup. Error: " + e.getMessage() + ". " +
                           "Check: (1) expression syntax is correct, (2) referenced fields/methods exist, " +
                           "(3) object context is valid. Returning NULL.");
                return null;
            }
        }

        // EXISTING: Simple field lookup for non-SpEL field names
        logger.trace("Getting field '" + fieldName + "' from object of type: " + object.getClass().getSimpleName());

        // Handle Map objects
        if (object instanceof Map) {
            Object value = ((Map<?, ?>) object).get(fieldName);
            logger.trace("Map lookup for '" + fieldName + "' returned: " + value);
            return value;
        }

        // Handle regular objects using proper getter methods instead of reflection
        try {
            // Try to find a getter method first (proper OOP approach)
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method getter = object.getClass().getMethod(getterName);
            Object value = getter.invoke(object);
            logger.trace("Getter method lookup for '" + fieldName + "' returned: " + value);
            return value;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Try boolean getter (isXxx)
            try {
                String booleanGetterName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                Method booleanGetter = object.getClass().getMethod(booleanGetterName);
                Object value = booleanGetter.invoke(object);
                logger.trace("Boolean getter method lookup for '" + fieldName + "' returned: " + value);
                return value;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e2) {
                logger.debug("No getter method found for field '" + fieldName + "' on object of type " +
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
     * @return true if the field was set successfully, false if it failed
     */
    private boolean setFieldValue(Object object, String fieldName, Object value) {
        if (object == null || fieldName == null) {
            logger.error("FIELD SET FAILED: setFieldValue called with null object or fieldName. " +
                        "object=" + (object == null ? "null" : object.getClass().getSimpleName()) +
                        ", fieldName=" + fieldName);
            return false;
        }

        // NEW: If fieldName starts with #, treat it as a SpEL expression for setting
        if (fieldName.startsWith("#")) {
            try {
                logger.trace("Setting value via SpEL expression: " + fieldName);
                StandardEvaluationContext context = createEvaluationContext(object);
                Expression expr = getOrCompileExpression(fieldName);
                expr.setValue(context, value);
                logger.trace("Successfully set field via SpEL '" + fieldName + "' to: " + value);
                return true;
            } catch (Exception e) {
                logger.error("SPEL EXPRESSION SET FAILED: Failed to set field via SpEL expression '" + fieldName +
                           "' to value '" + value + "'. Error: " + e.getMessage() + ". " +
                           "Check: (1) expression syntax is correct, (2) target field/property exists and is writable, " +
                           "(3) value type is compatible with target field type, (4) intermediate structures exist. " +
                           "Field was NOT set. This is a CRITICAL error that will be reported in the RuleResult.");
                return false;
            }
        }

        // EXISTING: Simple field setting for non-SpEL field names
        logger.trace("Setting field '" + fieldName + "' to value: " + value +
                     " on object of type: " + object.getClass().getSimpleName());

        // Handle Map objects
        if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) object;
            map.put(fieldName, value);
            logger.trace("Successfully set map key '" + fieldName + "' to: " + value);
            return true;
        }

        // Handle regular objects using proper setter methods instead of reflection
        try {
            // Try to find a setter method first (proper OOP approach)
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method setter = object.getClass().getMethod(setterName, value.getClass());
            setter.invoke(object, value);
            logger.trace("Successfully set field '" + fieldName + "' to: " + value);
            return true;
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
                            logger.trace("Successfully set field '" + fieldName + "' to: " + value);
                            return true;
                        }
                    }
                }
                logger.error("SETTER METHOD NOT FOUND: No suitable setter method found for field '" + fieldName +
                           "' on object of type " + object.getClass().getSimpleName() + ". " +
                           "Check: (1) setter method exists (e.g., set" + Character.toUpperCase(fieldName.charAt(0)) +
                           fieldName.substring(1) + "), (2) field name is correct, (3) object type supports this field. " +
                           "Field was NOT set.");
                return false;
            } catch (IllegalAccessException | InvocationTargetException e2) {
                logger.error("SETTER INVOCATION FAILED: Could not invoke setter for field '" + fieldName +
                           "' on object of type " + object.getClass().getSimpleName() + ". Error: " + e2.getMessage() + ". " +
                           "Check: (1) setter method is accessible, (2) value type is compatible. Field was NOT set.", e2);
                return false;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("SETTER INVOCATION FAILED: Could not invoke setter for field '" + fieldName +
                       "' on object of type " + object.getClass().getSimpleName() + ". Error: " + e.getMessage() + ". " +
                       "Check: (1) setter method is accessible, (2) value type is compatible. Field was NOT set.", e);
            return false;
        }
    }

    /**
     * Create evaluation context for SpEL expressions.
     *
     * @param rootObject The root object for the context
     * @return The evaluation context
     */
    private StandardEvaluationContext createEvaluationContext(Object rootObject) {
        StandardEvaluationContext context = evaluatorService.createEvaluationContext(rootObject);

        // Add common variables and functions
        context.setVariable("serviceRegistry", serviceRegistry);

        // Add rule results for conditional mapping support (only if they exist)
        if (!ruleGroupResults.isEmpty()) {
            context.setVariable("ruleGroupResults", ruleGroupResults);
        }
        if (!individualRuleResults.isEmpty()) {
            context.setVariable("ruleResults", individualRuleResults);
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
        // Check cache first
        Object cached = cacheManager.get(ApexCacheManager.EXPRESSION_CACHE, expressionString);
        if (cached instanceof Expression) {
            return (Expression) cached;
        }

        // Compile and cache
        Expression expression = parser.parseExpression(expressionString);
        cacheManager.put(ApexCacheManager.EXPRESSION_CACHE, expressionString, expression);
        return expression;
    }



    /**
     * Resolve lookup service from either service registry or dataset configuration.
     * Uses dataset caching to avoid creating duplicate DatasetLookupService instances
     * for identical datasets.
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

            logger.debug("Resolved external lookup service: " + serviceName);
            return service;
        }

        // Priority 2: Dataset configuration with caching
        if (lookupConfig.getLookupDataset() != null) {
            YamlEnrichment.LookupDataset dataset = lookupConfig.getLookupDataset();

            // Generate content-based signature for the dataset
            // Pass the configuration to include schema in the signature for database datasets
            DatasetSignature signature = DatasetSignature.from(dataset, this.currentConfiguration);
            String cacheKey = signature.toString();

            // Check cache first
            Object cached = cacheManager.get(ApexCacheManager.DATASET_CACHE, cacheKey);
            if (cached instanceof DatasetLookupService) {
                logger.info("Dataset cache HIT for signature: " + signature.toShortString());
                return (DatasetLookupService) cached;
            }

            // Create new dataset service
            String datasetServiceName = "dataset-" + signature.toShortString();

            try {
                DatasetLookupService datasetService = DatasetLookupServiceFactory
                    .createDatasetLookupService(datasetServiceName, dataset, this.currentConfiguration, this.dataSourceRegistry);

                // Cache the dataset service
                cacheManager.put(ApexCacheManager.DATASET_CACHE, cacheKey, datasetService);

                logger.info("Dataset cache MISS - Created and cached dataset lookup service: " + datasetServiceName +
                           " (type: " + dataset.getType() + ", records: " +
                           datasetService.getAllRecords().size() + ", signature: " + signature.toShortString() + ")");

                return datasetService;
            } catch (Exception e) {
                throw new EnrichmentException("Failed to create dataset lookup service for enrichment '" +
                                            enrichmentId + "': " + e.getMessage(), e);
            }
        }

        throw new EnrichmentException("No lookup service or dataset configured for enrichment: " + enrichmentId);
    }













    /**
     * Store a rule group result for use in conditional mapping expressions.
     * This method is called by RulesEngine when processing rule groups in document order mode.
     *
     * @param ruleGroupId The ID of the rule group
     * @param passed Whether the rule group passed
     * @param ruleResults Map of individual rule results within the group
     */
    public void storeRuleGroupResult(String ruleGroupId, boolean passed, Map<String, Boolean> ruleResults) {
        Map<String, Object> groupRuleResults = new HashMap<>();
        groupRuleResults.put("passed", passed);

        if (ruleResults != null) {
            groupRuleResults.putAll(ruleResults);

            // Add passedRules and failedRules lists
            List<String> passedRules = new ArrayList<>();
            List<String> failedRules = new ArrayList<>();
            for (Map.Entry<String, Boolean> entry : ruleResults.entrySet()) {
                if (entry.getValue()) {
                    passedRules.add(entry.getKey());
                } else {
                    failedRules.add(entry.getKey());
                }
            }
            groupRuleResults.put("passedRules", passedRules);
            groupRuleResults.put("failedRules", failedRules);

            // ALSO store individual rule results in the individualRuleResults map
            // This allows enrichments to reference #ruleResults['rule-id'] in document order mode
            for (Map.Entry<String, Boolean> entry : ruleResults.entrySet()) {
                individualRuleResults.put(entry.getKey(), entry.getValue());
                logger.debug("Stored individual rule result from group: " + entry.getKey() + " -> passed=" + entry.getValue());
            }
        }

        ruleGroupResults.put(ruleGroupId, groupRuleResults);
        logger.debug("Stored rule group result: " + ruleGroupId + " -> passed=" + passed);
    }

    /**
     * Store individual rule result for conditional mapping in enrichments.
     * This allows enrichments to reference #ruleResults in document order mode.
     *
     * @param ruleId The ID of the rule
     * @param passed Whether the rule passed
     */
    public void storeIndividualRuleResult(String ruleId, boolean passed) {
        individualRuleResults.put(ruleId, passed);
        logger.debug("Stored individual rule result: " + ruleId + " -> passed=" + passed);
    }

    /**
     * Process rules and rule groups to populate rule results for conditional mapping.
     *
     * @param configuration The YAML configuration containing rules and rule groups
     * @param targetObject The object to evaluate rules against
     */
    private void processRulesAndRuleGroups(dev.mars.apex.core.config.model.YamlRuleConfiguration configuration, Object targetObject) {
        // Clear previous results
        ruleGroupResults.clear();
        individualRuleResults.clear();

        logger.debug("Processing rules and rule groups for conditional mapping...");

        try {
            StandardEvaluationContext context = createEvaluationContext(targetObject);

            // Process individual rules first
            if (configuration.getRules() != null) {
                logger.debug("Processing " + configuration.getRules().size() + " individual rules...");
                for (YamlRule yamlRule : configuration.getRules()) {
                    try {
                        // Create Rule object from YAML configuration
                        Rule rule = new RuleBuilder()
                            .withName(yamlRule.getName() != null ? yamlRule.getName() : yamlRule.getId())
                            .withCondition(yamlRule.getCondition())
                            .withMessage(yamlRule.getMessage() != null ? yamlRule.getMessage() : "Rule " + yamlRule.getId())
                            .withSeverity(SeverityConstants.INFO)
                            .build();

                        // Evaluate rule
                        Expression exp = getOrCompileExpression(rule.getCondition());
                        Boolean result = exp.getValue(context, Boolean.class);

                        if (result == null) {
                            result = false;
                        }

                        // Store individual rule result using YAML rule ID
                        individualRuleResults.put(yamlRule.getId(), result);

                        logger.debug("Rule '" + yamlRule.getId() + "' evaluated to: " + result);

                    } catch (Exception e) {
                        // CRITICAL: Rule evaluation failure is a serious configuration error
                        logger.error("Rule evaluation failed for '" + yamlRule.getId() +
                                  "' - condition: '" + yamlRule.getCondition() + "' - Error: " + e.getMessage());
                        logger.debug("Full exception details:", e);
                        individualRuleResults.put(yamlRule.getId(), false);
                    }
                }
            }

            // Process rule groups
            if (configuration.getRuleGroups() != null) {
                logger.debug("Processing " + configuration.getRuleGroups().size() + " rule groups...");
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
                                    Rule rule = new RuleBuilder()
                                        .withName(yamlRule.getName() != null ? yamlRule.getName() : yamlRule.getId())
                                        .withCondition(yamlRule.getCondition())
                                        .withMessage(yamlRule.getMessage() != null ? yamlRule.getMessage() : "Rule " + yamlRule.getId())
                                        .withSeverity(SeverityConstants.INFO)
                                        .build();
                                    ruleGroup.addRule(rule, sequence++);
                                }
                            }
                        }

                        // Evaluate rule group through canonical path
                        boolean groupResult = ruleGroupEvaluationService.evaluate(ruleGroup, context);

                        // Store rule group results
                        Map<String, Object> groupRuleResults = new HashMap<>();
                        groupRuleResults.put("passed", groupResult);
                        groupRuleResults.putAll(ruleGroup.getRuleResults());

                        // Add passedRules and failedRules lists
                        List<String> passedRules = new ArrayList<>();
                        List<String> failedRules = new ArrayList<>();
                        for (Map.Entry<String, Boolean> entry : ruleGroup.getRuleResults().entrySet()) {
                            if (entry.getValue()) {
                                passedRules.add(entry.getKey());
                            } else {
                                failedRules.add(entry.getKey());
                            }
                        }
                        groupRuleResults.put("passedRules", passedRules);
                        groupRuleResults.put("failedRules", failedRules);

                        ruleGroupResults.put(yamlRuleGroup.getId(), groupRuleResults);

                        logger.debug("Rule group '" + yamlRuleGroup.getId() + "' evaluated to: " + groupResult);

                    } catch (Exception e) {
                        // CRITICAL: Rule group evaluation failure is a serious configuration error
                        logger.error("Rule group evaluation failed for '" + yamlRuleGroup.getId() +
                                  "' - Error: " + e.getMessage());
                        logger.debug("Full exception details:", e);
                        Map<String, Object> failedResult = new HashMap<>();
                        failedResult.put("passed", false);
                        failedResult.put("passedRules", new ArrayList<String>());
                        failedResult.put("failedRules", new ArrayList<String>());
                        ruleGroupResults.put(yamlRuleGroup.getId(), failedResult);
                    }
                }
            }

        } catch (Exception e) {
            // CRITICAL: General rules/rule groups processing failure is a serious system error
            logger.error("CRITICAL: Error processing rules and rule groups - System Error: {}", e.getMessage());
            logger.debug("Full stack trace for rules/rule groups processing error:", e);
        }
    }

    /**
     * Find a rule by ID in the configuration.
     *
     * @param configuration The YAML configuration
     * @param ruleId The rule ID to find
     * @return The YamlRule if found, null otherwise
     */
    private YamlRule findRuleById(dev.mars.apex.core.config.model.YamlRuleConfiguration configuration, String ruleId) {
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
            logger.trace("No conditions specified for rule '" + rule.getId() + "', treating as default rule");
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
            logger.warn("Mapping rule '" + rule.getId() + "' has no mapping configuration");
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
                logger.warn("Unknown mapping type '" + mappingType + "' for rule: " + rule.getId());
                return null;
            }
        } catch (Exception e) {
            logger.warn("Failed to apply mapping for rule '{}': {}", rule.getId(), e.getMessage());
            logger.debug("Stack trace for mapping rule application failure:", e);

            // Try fallback value if available
            if (mapping.getFallbackValue() != null && !mapping.getFallbackValue().trim().isEmpty()) {
                try {
                    StandardEvaluationContext context = createEvaluationContext(targetObject);
                    Expression fallbackExpr = getOrCompileExpression(mapping.getFallbackValue());
                    return fallbackExpr.getValue(context);
                } catch (Exception fallbackException) {
                    logger.warn("Failed to apply fallback value: " + fallbackException.getMessage());
                    logger.debug("Full exception details:", fallbackException);
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

        // If expression is specified, use it
        if (mapping.getExpression() != null && !mapping.getExpression().trim().isEmpty()) {
            Expression expr = getOrCompileExpression(mapping.getExpression());
            return expr.getValue(context);
        }

        // Otherwise, use source field directly
        if (mapping.getSourceField() != null && !mapping.getSourceField().trim().isEmpty()) {
            Expression sourceExpr = getOrCompileExpression("#" + mapping.getSourceField());
            return sourceExpr.getValue(context);
        }

        logger.warn("Direct mapping has neither expression nor source-field");
        return null;
    }

    /**
     * Apply lookup mapping (database/external lookup with expression).
     */
    private Object applyLookupMapping(YamlEnrichment.MappingConfig mapping, Object targetObject) {
        // This is a simplified implementation - in a full implementation,
        // you would use the lookup-config to perform the actual lookup
        logger.warn("Lookup mapping not fully implemented yet for conditional-mapping-enrichment");

        // For now, fall back to expression if available
        if (mapping.getExpression() != null && !mapping.getExpression().trim().isEmpty()) {
            StandardEvaluationContext context = createEvaluationContext(targetObject);
            Expression expr = getOrCompileExpression(mapping.getExpression());
            return expr.getValue(context);
        }

        return null;
    }

    // ========================================
    // RuleResult-returning methods (Phase 4)
    // ========================================

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
                                                  dev.mars.apex.core.config.model.YamlRuleConfiguration configuration) {
        logger.debug("processEnrichmentsWithResult() entry - enrichments count: {}, targetObject type: {}", 
                    enrichments != null ? enrichments.size() : 0, 
                    targetObject != null ? targetObject.getClass().getSimpleName() : "null");

        List<String> failureMessages = new ArrayList<>();
        boolean overallSuccess = true;

        // Set current configuration for database lookups
        this.currentConfiguration = configuration;

        if (enrichments == null || enrichments.isEmpty()) {
            logger.debug("processEnrichmentsWithResult() - no enrichments to process, returning success");
            Map<String, Object> resultData = convertToMap(targetObject);
            return RuleResult.enrichmentSuccess(resultData);
        }

        // Sort enrichments by priority (lower numbers = higher priority)
        enrichments.sort((e1, e2) -> {
            int priority1 = e1.getPriority() != null ? e1.getPriority() : 100;
            int priority2 = e2.getPriority() != null ? e2.getPriority() : 100;
            return Integer.compare(priority1, priority2);
        });
        logger.debug("processEnrichmentsWithResult() - enrichments sorted by priority, processing order: {}", 
                    enrichments.stream().map(YamlEnrichment::getId).toList());

        Object enrichedObject = targetObject;

        for (YamlEnrichment enrichment : enrichments) {
            logger.debug("Processing enrichment '{}' (type: {}, priority: {})", 
                        enrichment.getId(), enrichment.getType(), enrichment.getPriority());
            try {
                if (shouldProcessEnrichment(enrichment, enrichedObject)) {
                    enrichedObject = processEnrichment(enrichment, enrichedObject);
                    logger.debug("Successfully processed enrichment: {} - data keys after: {}", 
                                enrichment.getId(), 
                                enrichedObject instanceof Map ? ((Map<?,?>)enrichedObject).keySet() : "N/A");
                } else {
                    logger.debug("Skipping enrichment (condition not met): {}", enrichment.getId());

                    // Store result-field for field-enrichment (condition did not match)
                    if ("field-enrichment".equals(enrichment.getType()) && enrichment.getResultField() != null) {
                        setFieldValue(enrichedObject, enrichment.getResultField(), false);
                        logger.info("Stored field-enrichment result in field: {} = false", enrichment.getResultField());
                    }
                }
            } catch (Exception e) {
                logger.error("CRITICAL: Enrichment processing failed: {} - {}", enrichment.getId(), e.getMessage());
                logger.debug("Full stack trace for enrichment processing failure:", e);
                overallSuccess = false;
                failureMessages.add("Enrichment '" + enrichment.getId() + "' failed: " + e.getMessage());
            }
        }

        try {
            // Use the resultObject for analysis
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = convertToMap(enrichedObject);

            // Detect enrichment failures by checking for required field mapping failures
            if (enrichments != null && !enrichments.isEmpty()) {
                boolean enrichmentFailed = detectEnrichmentFailures(enrichments, enrichedData);

                if (enrichmentFailed) {
                    overallSuccess = false;
                    failureMessages.add("Required field enrichment failed - check logs for CRITICAL ERROR details");
                    logger.error("Enrichment failed due to required field mapping failures");
                }
            }

            // Aggregate severity from processed enrichments
            String aggregatedSeverity = aggregateEnrichmentSeverity(enrichments, overallSuccess);

            // Return appropriate RuleResult with aggregated severity
            if (overallSuccess) {
                logger.debug("processEnrichmentsWithResult() completed successfully - enrichedData keys: {}, severity: {}", 
                            enrichedData.keySet(), aggregatedSeverity);
                return RuleResult.enrichmentSuccess(enrichedData, aggregatedSeverity);
            } else {
                logger.error("Enrichment processing completed with failures, severity: {}, failures: {}", 
                            aggregatedSeverity, failureMessages.size());
                logger.debug("processEnrichmentsWithResult() failure messages: {}", failureMessages);
                return RuleResult.enrichmentFailure(failureMessages, enrichedData, aggregatedSeverity, "APEX-ENRICH-001");
            }

        } catch (Exception e) {
            logger.error("CRITICAL: Exception during enrichment processing: {}", e.getMessage());
            logger.debug("Full stack trace for enrichment exception:", e);
            // Business logic failure - return error result
            return RuleResult.errorWithCode(
                "enrichments",
                "Enrichment processing failed: " + e.getMessage(),
                SeverityConstants.ERROR,
                "APEX-ENRICH-999"
            );
        }
    }

    /**
     * Process a single enrichment on a target object with full configuration context and return detailed results.
     * This method provides programmatic access to enrichment success/failure status and detailed error information.
     * The configuration parameter is required for enrichments that reference rules or rule groups via #ruleGroupResults.
     *
     * @param enrichment The enrichment to apply
     * @param targetObject The object to enrich
     * @param configuration The full YAML configuration (required for conditional mapping with rule group results)
     * @return A RuleResult containing success status, enriched data, and failure messages
     */
    public RuleResult processEnrichmentWithResult(YamlEnrichment enrichment, Object targetObject,
                                                  dev.mars.apex.core.config.model.YamlRuleConfiguration configuration) {
        if (enrichment == null) {
            logger.debug("No enrichment provided");
            Map<String, Object> resultData = convertToMap(targetObject);
            return RuleResult.enrichmentSuccess(resultData);
        }

        List<YamlEnrichment> enrichmentList = new ArrayList<>();
        enrichmentList.add(enrichment);
        return processEnrichmentsWithResult(enrichmentList, targetObject, configuration);
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
                            logger.debug("Required field '" + targetField + "' is missing from enriched data");
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

        logger.debug("Aggregated enrichment severity: " + highestSeverity + " from " + enrichments.size() + " enrichments");
        return highestSeverity;
    }

    /**
     * Evaluate a success or error code expression.
     * Phase 4 Enhancement: Supports both constant strings and SpEL expressions.
     *
     * @param codeExpression The code expression (constant or SpEL starting with #)
     * @param context The evaluation context for SpEL expressions
     * @return The evaluated code string, or null if expression is null or evaluation fails
     */
    private String evaluateCode(String codeExpression, StandardEvaluationContext context) {
        if (codeExpression == null || codeExpression.trim().isEmpty()) {
            return null;
        }

        try {
            // Check if it's a SpEL expression (starts with #)
            if (codeExpression.trim().startsWith("#")) {
                Expression exp = parser.parseExpression(codeExpression);
                Object result = exp.getValue(context);
                return result != null ? result.toString() : null;
            } else {
                // It's a constant string
                return codeExpression;
            }
        } catch (Exception e) {
            logger.warn("Error evaluating code expression '{}': {}", codeExpression, e.getMessage());
            logger.debug("Stack trace for code expression evaluation error:", e);
            return null;
        }
    }

    /**
     * Apply field mappings for success/error codes to the target object.
     * Phase 4 Enhancement: Supports generic field mapping using SpEL expressions.
     *
     * @param mapToField The field mapping configuration
     * @param context The evaluation context for SpEL expressions
     * @param targetObject The target object to update with mapped values
     * @param successCode The evaluated success code (available as #success_code in expressions)
     * @param errorCode The evaluated error code (available as #error_code in expressions)
     */
    private void applyCodeFieldMappings(List<String> mapToField, StandardEvaluationContext context, Object targetObject,
                                       String successCode, String errorCode) {
        if (mapToField == null) {
            return;
        }

        try {
            // Create a new context with success_code and error_code variables
            StandardEvaluationContext mappingContext = evaluatorService.createEvaluationContext(context.getRootObject().getValue());

            // Copy ALL variables from the original context using reflection
            // This ensures that variables like #notionalValue, #delta, etc. are available in map-to-field expressions
            try {
                java.lang.reflect.Field variablesField = StandardEvaluationContext.class.getDeclaredField("variables");
                variablesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Object> originalVariables = (Map<String, Object>) variablesField.get(context);
                if (originalVariables != null) {
                    for (Map.Entry<String, Object> entry : originalVariables.entrySet()) {
                        mappingContext.setVariable(entry.getKey(), entry.getValue());
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to copy variables from original context: " + e.getMessage());
                // Fallback: copy only the "this" variable
                if (context.lookupVariable("this") != null) {
                    mappingContext.setVariable("this", context.lookupVariable("this"));
                }
            }

            // Add/override success_code and error_code variables
            if (successCode != null) {
                mappingContext.setVariable("success_code", successCode);
            }
            if (errorCode != null) {
                mappingContext.setVariable("error_code", errorCode);
            }

            // Apply each mapping
            for (String mapping : mapToField) {
                applyCodeFieldMapping(mapping, mappingContext, targetObject);
            }
        } catch (Exception e) {
            logger.warn("Error applying field mappings: {}", e.getMessage());
            logger.debug("Stack trace for field mappings application error:", e);
        }
    }

    /**
     * Apply a single field mapping expression for success/error codes.
     * Parses expressions like "fieldName = #success_code" or "status = #amount > 100 ? 'HIGH' : 'LOW'"
     *
     * @param mapping The mapping expression
     * @param context The evaluation context
     * @param targetObject The target object to update
     */
    private void applyCodeFieldMapping(String mapping, StandardEvaluationContext context, Object targetObject) {
        try {
            // Parse the mapping: "fieldName = expression"
            String[] parts = mapping.split("=", 2);
            if (parts.length != 2) {
                logger.warn("Invalid field mapping format: " + mapping + ". Expected 'fieldName = expression'");
                return;
            }

            String fieldName = parts[0].trim();
            String expression = parts[1].trim();

            // Evaluate the expression
            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(context);

            // Store the mapped value in the target object
            setFieldValue(targetObject, fieldName, value);
            logger.info("Applied field mapping: " + fieldName + " = " + value);
        } catch (Exception e) {
            logger.warn("Error applying field mapping '{}': {}", mapping, e.getMessage());
            logger.debug("Stack trace for field mapping application error:", e);
        }
    }
}
