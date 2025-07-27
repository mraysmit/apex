package dev.mars.rulesengine.core.service.enrichment;

import dev.mars.rulesengine.core.config.yaml.YamlEnrichment;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
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
        // Check if enrichment is enabled
        if (enrichment.getEnabled() != null && !enrichment.getEnabled()) {
            return false;
        }
        
        // Check target type if specified
        if (enrichment.getTargetType() != null) {
            String targetType = enrichment.getTargetType();
            if (!targetObject.getClass().getSimpleName().equals(targetType) &&
                !targetObject.getClass().getName().equals(targetType)) {
                LOGGER.fine("Target type mismatch. Expected: " + targetType + 
                           ", Actual: " + targetObject.getClass().getSimpleName());
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
                LOGGER.log(Level.WARNING, "Error evaluating enrichment condition '" + 
                          enrichment.getCondition() + "': " + e.getMessage(), e);
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
        
        LOGGER.fine("Processing lookup enrichment with service: " + lookupConfig.getLookupService());
        
        // 1. Resolve lookup service by name
        String serviceName = lookupConfig.getLookupService();
        LookupService lookupService = serviceRegistry.getService(serviceName, LookupService.class);
        
        if (lookupService == null) {
            throw new EnrichmentException("Lookup service not found: " + serviceName);
        }
        
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
