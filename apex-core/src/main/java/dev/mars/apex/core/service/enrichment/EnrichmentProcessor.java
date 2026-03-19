package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.cache.ApexCacheManager;
import dev.mars.apex.core.config.EnrichmentGroupFactory;
import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.engine.core.ExpressionEvaluatorService;
import dev.mars.apex.engine.execution.EnrichmentGroupExecutor;
import dev.mars.apex.core.service.lookup.LookupService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.engine.model.EnrichmentGroup;
import dev.mars.apex.engine.model.EnrichmentGroupResult;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.core.config.model.YamlRule;
import dev.mars.apex.engine.execution.RuleGroupEvaluationService;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
public class EnrichmentProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(EnrichmentProcessor.class);

    private final LookupServiceRegistry serviceRegistry;
    private final ExpressionEvaluatorService evaluatorService;
    private final ExpressionParser parser;

    // Unified cache manager for all caching needs
    private final ApexCacheManager cacheManager;

    // Data source registry from RulesEngine - prevents duplicate data source creation
    private final Map<String, dev.mars.apex.core.service.data.external.ExternalDataSource> dataSourceRegistry;

    // Rule group evaluation service for canonical evaluation path (Phase 2)
    private final RuleGroupEvaluationService ruleGroupEvaluationService;

    // Rule result tracking for conditional mapping support (Phase 13 extraction)
    private final RuleResultTracker ruleResultTracker = new RuleResultTracker();

    // Field access and mapping (Phase 13 extraction)
    private final FieldAccessor fieldAccessor;

    // Condition evaluation (Phase 13 extraction)
    private final EnrichmentConditionEvaluator conditionEvaluator;

    // Code mapping (Phase 13 extraction)
    private final CodeMappingProcessor codeMappingProcessor;

    // Lookup enrichment handling (Phase 13 extraction)
    private final LookupEnrichmentHandler lookupHandler;

    // Condition action executor for typed condition predicates (lookup/function)
    private final ConditionActionExecutor conditionActionExecutor;

    // Result analysis (Phase 13 extraction)
    private final EnrichmentResultBuilder resultBuilder = new EnrichmentResultBuilder();

    // Lazy reference to EnrichmentGroupExecutor to support function mapping type.
    // Set after construction to break the circular dependency:
    // EnrichmentProcessor -> EnrichmentGroupExecutor -> EnrichmentProcessor
    private Supplier<EnrichmentGroupExecutor> enrichmentGroupExecutorSupplier;

    // Recursion depth guard for function mapping to prevent infinite loops.
    // ThreadLocal is removed (not just reset to 0) on final unwind to avoid retaining state on pooled threads.
    private static final int MAX_FUNCTION_MAPPING_DEPTH = 5;
    private static final ThreadLocal<Integer> functionMappingDepth = ThreadLocal.withInitial(() -> 0);

    /**
     * Enter a function-mapping recursion level.
     * @param groupRef the enrichment-group-ref being entered (for error messages)
     * @return the depth <em>before</em> entry, or -1 if the limit has been reached
     */
    private static int enterFunctionMapping(String groupRef) {
        int depth = functionMappingDepth.get();
        if (depth >= MAX_FUNCTION_MAPPING_DEPTH) {
            logger.error("Function mapping recursion depth exceeded (max {}). " +
                        "Possible circular enrichment-group-ref chain involving '{}'",
                        MAX_FUNCTION_MAPPING_DEPTH, groupRef);
            return -1;
        }
        functionMappingDepth.set(depth + 1);
        return depth;
    }

    /**
     * Exit a function-mapping recursion level.
     * If unwinding back to depth 0, removes the ThreadLocal entirely
     * so pooled threads don't retain stale state.
     *
     * @param previousDepth the value returned by {@link #enterFunctionMapping}
     */
    private static void exitFunctionMapping(int previousDepth) {
        if (previousDepth == 0) {
            functionMappingDepth.remove();
        } else {
            functionMappingDepth.set(previousDepth);
        }
    }

    /**
     * Constructor with all required dependencies.
     *
     * @param serviceRegistry The lookup service registry
     * @param evaluatorService The expression evaluator service
     * @param dataSourceRegistry Data source registry from RulesEngine (null-safe, defaults to empty map)
     * @param ruleGroupEvaluationService Service for canonical rule group evaluation (required)
     */
    public EnrichmentProcessor(LookupServiceRegistry serviceRegistry,
                                   ExpressionEvaluatorService evaluatorService,
                                   Map<String, dev.mars.apex.core.service.data.external.ExternalDataSource> dataSourceRegistry,
                                   RuleGroupEvaluationService ruleGroupEvaluationService) {
        this.serviceRegistry = serviceRegistry;
        this.evaluatorService = evaluatorService;
        this.parser = evaluatorService.getParser();
        this.cacheManager = ApexCacheManager.getInstance();
        this.dataSourceRegistry = dataSourceRegistry != null ? dataSourceRegistry : new java.util.HashMap<>();
        this.ruleGroupEvaluationService = java.util.Objects.requireNonNull(ruleGroupEvaluationService,
                "RuleGroupEvaluationService is required — use RuleGroupEvaluationService(new UnifiedRuleEvaluator())");
        this.fieldAccessor = new FieldAccessor(this.parser, this.cacheManager, this::createEvaluationContext);
        this.conditionEvaluator = new EnrichmentConditionEvaluator(this.parser, this::createEvaluationContext);
        this.codeMappingProcessor = new CodeMappingProcessor(this.parser, this.evaluatorService, this.fieldAccessor);
        this.lookupHandler = new LookupEnrichmentHandler(this.fieldAccessor, this::createEvaluationContext,
            this.cacheManager, this.serviceRegistry, this.dataSourceRegistry);
        this.conditionActionExecutor = new ConditionActionExecutor(
            this.lookupHandler, this.fieldAccessor, this.parser, this::createEvaluationContext);
        this.conditionEvaluator.setActionExecutor(this.conditionActionExecutor);

        logger.info("EnrichmentProcessor initialized with unified cache manager" + 
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
    private Object processEnrichment(YamlEnrichment enrichment, Object targetObject,
                                     dev.mars.apex.core.config.model.YamlRuleConfiguration configuration) {
        logger.debug("Processing enrichment: " + enrichment.getId() + " (type: " + enrichment.getType() + ")");

        // Note: condition check is performed by the caller (processEnrichmentsWithResult)
        // which also handles error-code evaluation when condition fails.
        // This method is only called when the condition has already been verified as true.

        // Store result-field for field-enrichment (condition matched)
        if ("field-enrichment".equals(enrichment.getType()) && enrichment.getResultField() != null) {
            setFieldValue(targetObject, enrichment.getResultField(), true);
            logger.info("Stored field-enrichment result in field: " + enrichment.getResultField() + " = true");
        }

        // Process the enrichment based on type
        Object result;
        switch (enrichment.getType()) {
            case "lookup-enrichment":
                result = processLookupEnrichment(enrichment, targetObject, configuration);
                break;
            case "calculation-enrichment":
                result = processCalculationEnrichment(enrichment, targetObject);
                break;
            case "field-enrichment":
                result = processFieldEnrichment(enrichment, targetObject);
                break;
            case "conditional-mapping-enrichment":
                result = processConditionalMappingEnrichment(enrichment, targetObject, configuration);
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
     * Delegates to {@link LookupEnrichmentHandler#processLookupEnrichment}.
     */
    private Object processLookupEnrichment(YamlEnrichment enrichment, Object targetObject,
                                           dev.mars.apex.core.config.model.YamlRuleConfiguration configuration) {
        return lookupHandler.processLookupEnrichment(enrichment, targetObject, configuration);
    }

    /**
     * Process a multi-row lookup enrichment.
     * Delegates to {@link LookupEnrichmentHandler#processMultiRowLookup}.
     */
    private Object processMultiRowLookup(YamlEnrichment enrichment, LookupService lookupService,
                                         Object lookupKey, YamlEnrichment.LookupConfig lookupConfig,
                                         Object targetObject) {
        return lookupHandler.processMultiRowLookup(enrichment, lookupService, lookupKey, lookupConfig, targetObject);
    }

    /**
     * Perform a multi-row lookup.
     * Delegates to {@link LookupEnrichmentHandler#performMultiRowLookup}.
     */
    @SuppressWarnings("unchecked")
    private java.util.List<java.util.Map<String, Object>> performMultiRowLookup(
            LookupService lookupService, Object lookupKey,
            YamlEnrichment.LookupConfig lookupConfig) {
        return lookupHandler.performMultiRowLookup(lookupService, lookupKey, lookupConfig);
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

            logger.error("Calculation enrichment '{}' failed for expression '{}': {}",
                    enrichment.getId(), calcConfig.getExpression(), e.getMessage());
            logger.debug("Stack trace for calculation enrichment failure", e);

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
    private Object processConditionalMappingEnrichment(YamlEnrichment enrichment, Object targetObject,
                                                         YamlRuleConfiguration configuration) {
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
                if (evaluateMappingRuleConditions(rule, targetObject, configuration)) {
                    anyRuleMatched = true;  // Track that a rule matched

                    if (logMatchedRule) {
                        logger.info("Matched mapping rule: " + rule.getId() + " (priority: " + rule.getPriority() + ")");
                    }

                    // Apply the mapping
                    Object mappedValue = applyMappingRule(rule, targetObject, configuration);

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
     * Delegates to {@link EnrichmentConditionEvaluator#evaluateConditionGroup}.
     */
    private boolean evaluateConditionGroup(YamlEnrichment.ConditionGroup conditionGroup, Object targetObject) {
        return conditionEvaluator.evaluateConditionGroup(conditionGroup, targetObject);
    }

    /**
     * Perform lookup operation with caching support.
     * Delegates to {@link LookupEnrichmentHandler#performLookup}.
     */
    private Object performLookup(LookupService lookupService, Object lookupKey,
                                YamlEnrichment.LookupConfig lookupConfig) {
        return lookupHandler.performLookup(lookupService, lookupKey, lookupConfig);
    }

    /**
     * Apply field mappings from lookup result to target object.
     * Delegates to {@link FieldAccessor#applyFieldMappings}.
     */
    private Object applyFieldMappings(List<YamlEnrichment.FieldMapping> fieldMappings,
                                     Object sourceObject, Object targetObject) {
        return fieldAccessor.applyFieldMappings(fieldMappings, sourceObject, targetObject);
    }

    /**
     * Apply expression to a value.
     * Delegates to {@link FieldAccessor#applyExpression}.
     */
    private Object applyExpression(String expression, Object value, Object context) {
        return fieldAccessor.applyExpression(expression, value, context);
    }

    /**
     * Get field value from an object using reflection or map access.
     * Delegates to {@link FieldAccessor#getFieldValue}.
     */
    private Object getFieldValue(Object object, String fieldName) {
        return fieldAccessor.getFieldValue(object, fieldName);
    }

    /**
     * Set field value on an object using reflection or map access.
     * Delegates to {@link FieldAccessor#setFieldValue}.
     */
    private boolean setFieldValue(Object object, String fieldName, Object value) {
        return fieldAccessor.setFieldValue(object, fieldName, value);
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
        if (!ruleResultTracker.getRuleGroupResults().isEmpty()) {
            context.setVariable("ruleGroupResults", ruleResultTracker.getRuleGroupResults());
        }
        if (!ruleResultTracker.getIndividualRuleResults().isEmpty()) {
            context.setVariable("ruleResults", ruleResultTracker.getIndividualRuleResults());
        }

        return context;
    }

    /**
     * Get or compile SpEL expression with caching.
     * Delegates to {@link FieldAccessor#getOrCompileExpression}.
     */
    private Expression getOrCompileExpression(String expressionString) {
        return fieldAccessor.getOrCompileExpression(expressionString);
    }

    /**
     * Clear rule results before starting a new evaluation pass.
     * Delegates to {@link RuleResultTracker}.
     */
    public void clearRuleResults() {
        ruleResultTracker.clearRuleResults();
    }

    /**
     * Store a rule group result for use in conditional mapping expressions.
     * Delegates to {@link RuleResultTracker}.
     *
     * @param ruleGroupId The ID of the rule group
     * @param passed Whether the rule group passed
     * @param ruleResults Map of individual rule results within the group
     */
    public void storeRuleGroupResult(String ruleGroupId, boolean passed, Map<String, Boolean> ruleResults) {
        ruleResultTracker.storeRuleGroupResult(ruleGroupId, passed, ruleResults);
    }

    /**
     * Store individual rule result for conditional mapping in enrichments.
     * Delegates to {@link RuleResultTracker}.
     *
     * @param ruleId The ID of the rule
     * @param passed Whether the rule passed
     */
    public void storeIndividualRuleResult(String ruleId, boolean passed) {
        ruleResultTracker.storeIndividualRuleResult(ruleId, passed);
    }

    /**
     * Evaluate conditions for a mapping rule.
     * Delegates to {@link EnrichmentConditionEvaluator#evaluateMappingRuleConditions}.
     */
    private boolean evaluateMappingRuleConditions(YamlEnrichment.MappingRule rule, Object targetObject,
                                                   YamlRuleConfiguration configuration) {
        return conditionEvaluator.evaluateMappingRuleConditions(rule, targetObject, configuration);
    }

    /**
     * Apply a mapping rule to get the mapped value.
     *
     * @param rule The mapping rule
     * @param targetObject The target object for context
     * @return The mapped value
     */
    private Object applyMappingRule(YamlEnrichment.MappingRule rule, Object targetObject,
                                     YamlRuleConfiguration configuration) {
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
                return applyLookupMapping(mapping, targetObject, configuration);
            } else if ("function".equalsIgnoreCase(mappingType)) {
                return applyFunctionMapping(mapping, targetObject, configuration);
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
     * Apply lookup mapping (database/external lookup) within a conditional-mapping-enrichment.
     * Delegates to {@link LookupEnrichmentHandler} for service resolution and lookup execution.
     *
     * @param mapping      The mapping configuration containing lookup-config
     * @param targetObject The target object (shared context)
     * @param configuration The YAML configuration for data-source-ref resolution
     * @return The lookup result (single row as Map), or null if lookup fails/returns no data
     */
    private Object applyLookupMapping(YamlEnrichment.MappingConfig mapping, Object targetObject,
                                       YamlRuleConfiguration configuration) {
        YamlEnrichment.LookupConfig lookupConfig = mapping.getLookupConfig();
        if (lookupConfig == null) {
            logger.warn("Lookup mapping has no lookup-config");
            // Fall back to expression if available
            if (mapping.getExpression() != null && !mapping.getExpression().trim().isEmpty()) {
                StandardEvaluationContext context = createEvaluationContext(targetObject);
                Expression expr = getOrCompileExpression(mapping.getExpression());
                return expr.getValue(context);
            }
            return null;
        }

        try {
            // 1. Resolve lookup service via LookupEnrichmentHandler
            LookupService lookupService = lookupHandler.resolveLookupService(
                    "conditional-mapping-lookup", lookupConfig, configuration);

            // 2. Extract lookup key
            StandardEvaluationContext context = createEvaluationContext(targetObject);
            Expression keyExpr = getOrCompileExpression(lookupConfig.getLookupKey());
            Object lookupKey = keyExpr.getValue(context);

            if (lookupKey == null) {
                logger.warn("Lookup mapping key expression '{}' evaluated to null", lookupConfig.getLookupKey());
                return null;
            }

            // 3. Perform lookup
            Object lookupResult = lookupHandler.performLookup(lookupService, lookupKey, lookupConfig);
            logger.debug("Lookup mapping result for key '{}': {}", lookupKey, lookupResult);

            // 4. Extract output-field if specified
            if (mapping.getOutputField() != null && !mapping.getOutputField().trim().isEmpty()) {
                // If outputField is specified, store full result and extract specific field
                if (lookupResult instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> resultMap = (java.util.Map<String, Object>) lookupResult;
                    return resultMap.get(mapping.getOutputField());
                }
                return lookupResult;
            }

            // 5. Apply expression/transformation on the result if specified
            if (lookupResult != null && mapping.getExpression() != null
                    && !mapping.getExpression().trim().isEmpty()) {
                return applyExpression(mapping.getExpression(), lookupResult, targetObject);
            }

            return lookupResult;
        } catch (Exception e) {
            logger.warn("Lookup mapping failed: {}", e.getMessage());
            logger.debug("Stack trace for lookup mapping failure:", e);
            return null;
        }
    }

    /**
     * Apply function mapping: invoke an enrichment group with bound input parameters
     * and extract a specific output field.
     *
     * @param mapping The mapping configuration containing enrichment-group-ref, input-parameters, and output-field
     * @param targetObject The target object (shared context)
     * @param configuration The YAML configuration for resolving enrichment groups
     * @return The extracted output value, or null if the function call fails
     */
    private Object applyFunctionMapping(YamlEnrichment.MappingConfig mapping, Object targetObject,
                                         YamlRuleConfiguration configuration) {
        String groupRef = mapping.getEnrichmentGroupRef();
        if (groupRef == null || groupRef.trim().isEmpty()) {
            logger.warn("Function mapping has no enrichment-group-ref");
            return null;
        }

        if (enrichmentGroupExecutorSupplier == null) {
            logger.warn("Function mapping requires EnrichmentGroupExecutor but none is configured");
            return null;
        }

        if (configuration == null) {
            logger.warn("Function mapping requires YamlRuleConfiguration to resolve enrichment-group-ref '{}'", groupRef);
            return null;
        }

        // Recursion depth guard
        int previousDepth = enterFunctionMapping(groupRef);
        if (previousDepth < 0) {
            return null;
        }

        try {
            // 1. Apply input-parameters into the shared targetObject
            List<YamlEnrichment.FieldMapping> inputParams = mapping.getInputParameters();
            if (inputParams != null && !inputParams.isEmpty()) {
                for (YamlEnrichment.FieldMapping param : inputParams) {
                    try {
                        Object value;
                        boolean isConstant = "constant".equals(param.getSourceField())
                                || param.getSourceField() == null
                                || param.getSourceField().trim().isEmpty();

                        if (isConstant) {
                            if (param.getExpression() != null && !param.getExpression().trim().isEmpty()) {
                                StandardEvaluationContext ctx = createEvaluationContext(targetObject);
                                Expression expr = getOrCompileExpression(param.getExpression());
                                value = expr.getValue(ctx);
                            } else {
                                value = param.getDefaultValue();
                            }
                        } else {
                            // Evaluate source-field as SpEL expression against the context
                            StandardEvaluationContext ctx = createEvaluationContext(targetObject);
                            String sourceExpr = param.getSourceField().startsWith("#")
                                    ? param.getSourceField()
                                    : "#" + param.getSourceField();
                            Expression expr = getOrCompileExpression(sourceExpr);
                            value = expr.getValue(ctx);

                            // Apply expression/transformation if specified
                            if (value != null && param.getExpression() != null
                                    && !param.getExpression().trim().isEmpty()) {
                                value = applyExpression(param.getExpression(), value, targetObject);
                            }
                        }

                        if (param.getTargetField() != null) {
                            setFieldValue(targetObject, param.getTargetField(), value);
                            logger.debug("Function mapping input: {} -> {} = {}",
                                    param.getSourceField(), param.getTargetField(), value);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to apply function mapping input parameter '{}' -> '{}': {}",
                                param.getSourceField(), param.getTargetField(), e.getMessage());
                        logger.debug("Stack trace for input parameter failure:", e);
                    }
                }
            }

            // 2. Resolve the enrichment group by ID
            List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(configuration);
            EnrichmentGroup targetGroup = groups.stream()
                    .filter(g -> groupRef.equals(g.getId()))
                    .findFirst()
                    .orElse(null);

            if (targetGroup == null) {
                logger.warn("enrichment-group-ref '{}' not found in configuration", groupRef);
                return null;
            }

            // 3. Execute the enrichment group via EnrichmentGroupExecutor
            EnrichmentGroupExecutor executor = enrichmentGroupExecutorSupplier.get();
            EnrichmentGroupResult groupResult = executor.processEnrichmentGroup(
                    targetGroup, targetObject, configuration);

            if (!groupResult.isSuccess()) {
                logger.warn("Function mapping enrichment group '{}' execution failed: {}",
                        groupRef, groupResult.getMessage());

                // Apply fallback-value if configured
                if (mapping.getFallbackValue() != null && !mapping.getFallbackValue().trim().isEmpty()) {
                    try {
                        StandardEvaluationContext ctx = createEvaluationContext(targetObject);
                        Expression fallbackExpr = getOrCompileExpression(mapping.getFallbackValue());
                        Object fallbackResult = fallbackExpr.getValue(ctx);
                        logger.debug("Function mapping fallback applied for group '{}': {}", groupRef, fallbackResult);
                        return fallbackResult;
                    } catch (Exception fallbackEx) {
                        logger.warn("Failed to apply fallback value for function mapping group '{}': {}",
                                groupRef, fallbackEx.getMessage());
                    }
                }
            }

            // 4. Extract the output-field value from the mutated targetObject
            String outputField = mapping.getOutputField();
            if (outputField == null || outputField.trim().isEmpty()) {
                logger.warn("Function mapping has no output-field specified for enrichment-group-ref '{}'", groupRef);
                return null;
            }

            Object outputValue = getFieldValue(targetObject, outputField);
            logger.debug("Function mapping output: {} = {} (from group '{}')", outputField, outputValue, groupRef);

            return outputValue;
        } finally {
            exitFunctionMapping(previousDepth);
        }
    }

    /**
     * Set the enrichment group executor supplier for function mapping support.
     * Called after construction to break the circular dependency between
     * EnrichmentProcessor and EnrichmentGroupExecutor.
     *
     * @param supplier Supplier that provides the EnrichmentGroupExecutor instance
     */
    public void setEnrichmentGroupExecutorSupplier(Supplier<EnrichmentGroupExecutor> supplier) {
        this.enrichmentGroupExecutorSupplier = supplier;
        // Also wire to ConditionActionExecutor for function condition predicates
        this.conditionActionExecutor.setEnrichmentGroupExecutorSupplier(supplier);
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
                    enrichedObject = processEnrichment(enrichment, enrichedObject, configuration);
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

                    // Evaluate error code when condition doesn't match
                    if (enrichment.getErrorCode() != null) {
                        StandardEvaluationContext context = createEvaluationContext(enrichedObject);
                        String evaluatedErrorCode = evaluateCode(enrichment.getErrorCode(), context);

                        // Apply field mappings with error code
                        if (enrichment.getMapToField() != null) {
                            applyCodeFieldMappings(enrichment.getMapToField(), context, enrichedObject, null, evaluatedErrorCode);
                        }

                        logger.debug("Enrichment condition not met, error code evaluated: {}", evaluatedErrorCode);
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
     * Detect enrichment failures.
     * Delegates to {@link EnrichmentResultBuilder#detectEnrichmentFailures}.
     */
    private boolean detectEnrichmentFailures(List<YamlEnrichment> enrichments, Map<String, Object> enrichedData) {
        return resultBuilder.detectEnrichmentFailures(enrichments, enrichedData);
    }

    /**
     * Convert an object to a Map for consistent data handling.
     * This method handles both Map objects and regular objects.
     *
     * @param object The object to convert
     * @return A Map representation of the object
     */
    /**
     * Convert an object to a Map representation.
     * Delegates to {@link FieldAccessor#convertToMap}.
     */
    private Map<String, Object> convertToMap(Object object) {
        return fieldAccessor.convertToMap(object);
    }

    /**
     * Aggregate severity from a list of enrichments.
     * Delegates to {@link EnrichmentResultBuilder#aggregateEnrichmentSeverity}.
     */
    private String aggregateEnrichmentSeverity(List<YamlEnrichment> enrichments, boolean overallSuccess) {
        return resultBuilder.aggregateEnrichmentSeverity(enrichments, overallSuccess);
    }

    /**
     * Evaluate a success or error code expression.
     * Delegates to {@link CodeMappingProcessor#evaluateCode}.
     */
    private String evaluateCode(String codeExpression, StandardEvaluationContext context) {
        return codeMappingProcessor.evaluateCode(codeExpression, context);
    }

    /**
     * Apply field mappings for success/error codes to the target object.
     * Delegates to {@link CodeMappingProcessor#applyCodeFieldMappings}.
     */
    private void applyCodeFieldMappings(List<String> mapToField, StandardEvaluationContext context, Object targetObject,
                                       String successCode, String errorCode) {
        codeMappingProcessor.applyCodeFieldMappings(mapToField, context, targetObject, successCode, errorCode);
    }

    /**
     * Apply a single field mapping expression for success/error codes.
     * Delegates to {@link CodeMappingProcessor#applyCodeFieldMapping}.
     */
    private void applyCodeFieldMapping(String mapping, StandardEvaluationContext context, Object targetObject) {
        codeMappingProcessor.applyCodeFieldMapping(mapping, context, targetObject);
    }
}
