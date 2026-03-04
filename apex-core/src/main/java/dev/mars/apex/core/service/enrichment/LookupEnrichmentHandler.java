/*
 * Copyright (c) 2025-2026 Mars Software - All Rights Reserved.
 *
 * This file is part of the APEX Rules Engine.
 * Unauthorized copying or distribution is prohibited.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.4
 * @created 2026-03-04
 */
package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.cache.ApexCacheManager;
import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.lookup.DatasetLookupService;
import dev.mars.apex.core.service.lookup.DatasetLookupServiceFactory;
import dev.mars.apex.core.service.lookup.DatasetSignature;
import dev.mars.apex.core.service.lookup.LookupService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Handles lookup-based enrichments: resolving lookup services, performing single-row
 * and multi-row lookups with caching, and mapping results onto target objects.
 *
 * <p>Extracted from {@link EnrichmentProcessor} (Phase 13 decomposition) to isolate
 * lookup orchestration from the main enrichment processor.</p>
 *
 * @since 2.4
 */
public class LookupEnrichmentHandler {

    private static final Logger logger = LoggerFactory.getLogger(LookupEnrichmentHandler.class);

    private final FieldAccessor fieldAccessor;
    private final Function<Object, StandardEvaluationContext> contextFactory;
    private final ApexCacheManager cacheManager;
    private final LookupServiceRegistry serviceRegistry;
    private final Map<String, ExternalDataSource> dataSourceRegistry;
    private final Supplier<YamlRuleConfiguration> configurationSupplier;

    /**
     * @param fieldAccessor         Field accessor for value read/write and expression compilation
     * @param contextFactory        Creates SpEL evaluation contexts from root objects
     * @param cacheManager          Unified cache manager
     * @param serviceRegistry       Registry for resolving named lookup services
     * @param dataSourceRegistry    Registry of external data sources
     * @param configurationSupplier Supplies the current YAML configuration (may change between evaluations)
     */
    public LookupEnrichmentHandler(FieldAccessor fieldAccessor,
                                   Function<Object, StandardEvaluationContext> contextFactory,
                                   ApexCacheManager cacheManager,
                                   LookupServiceRegistry serviceRegistry,
                                   Map<String, ExternalDataSource> dataSourceRegistry,
                                   Supplier<YamlRuleConfiguration> configurationSupplier) {
        this.fieldAccessor = fieldAccessor;
        this.contextFactory = contextFactory;
        this.cacheManager = cacheManager;
        this.serviceRegistry = serviceRegistry;
        this.dataSourceRegistry = dataSourceRegistry;
        this.configurationSupplier = configurationSupplier;
    }

    // ─── Main Lookup Processing ──────────────────────────────────────────

    /**
     * Process a lookup-based enrichment.
     *
     * @param enrichment   The enrichment configuration
     * @param targetObject The target object
     * @return The enriched object
     */
    public Object processLookupEnrichment(YamlEnrichment enrichment, Object targetObject) {
        YamlEnrichment.LookupConfig lookupConfig = enrichment.getLookupConfig();
        if (lookupConfig == null) {
            logger.warn("Lookup enrichment '" + enrichment.getId() + "' has no lookup configuration");
            return targetObject;
        }

        // 1. Resolve lookup service
        LookupService lookupService = resolveLookupService(enrichment.getId(), lookupConfig);

        logger.debug("Processing lookup enrichment with service: " + lookupService.getName());

        // 2. Extract lookup key using SpEL expression
        Object lookupKey;
        try {
            StandardEvaluationContext context = contextFactory.apply(targetObject);
            Expression keyExpr = fieldAccessor.getOrCompileExpression(lookupConfig.getLookupKey());
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

        // 3. Check multi-row mode
        boolean isMultiRow = lookupConfig.getLookupDataset() != null && lookupConfig.getLookupDataset().isMultiRow();

        if (isMultiRow) {
            return processMultiRowLookup(enrichment, lookupService, lookupKey, lookupConfig, targetObject);
        }

        // SINGLE-ROW PATH (default)
        Object lookupResult = performLookup(lookupService, lookupKey, lookupConfig);

        logger.debug("Lookup result for key '" + lookupKey + "': " + lookupResult +
                   " (type: " + (lookupResult != null ? lookupResult.getClass().getSimpleName() : "null") + ")");

        if (lookupResult == null) {
            logger.debug("Lookup returned null result for key: " + lookupKey + ", applying default values");
        }

        // Store result-field if configured
        boolean lookupSucceeded = (lookupResult != null);
        if (enrichment.getResultField() != null) {
            fieldAccessor.setFieldValue(targetObject, enrichment.getResultField(), lookupSucceeded);
            logger.info("Stored lookup result in field: " + enrichment.getResultField() + " = " + lookupSucceeded);
        }

        // 4. Apply field mappings
        Object result = fieldAccessor.applyFieldMappings(enrichment.getFieldMappings(), lookupResult, targetObject);

        if (result == null) {
            String errorMsg = "Lookup enrichment '" + enrichment.getId() + "' failed: one or more field mappings could not be applied. " +
                             "Check: (1) target paths exist, (2) intermediate structures are pre-created, (3) SpEL expressions are valid.";
            logger.error(errorMsg);
            throw new EnrichmentException(errorMsg);
        }

        return result;
    }

    // ─── Multi-Row Lookup ────────────────────────────────────────────────

    /**
     * Process a multi-row lookup enrichment (rows: "all").
     */
    Object processMultiRowLookup(YamlEnrichment enrichment, LookupService lookupService,
                                 Object lookupKey, YamlEnrichment.LookupConfig lookupConfig,
                                 Object targetObject) {
        logger.info("Processing multi-row lookup enrichment '" + enrichment.getId() + "' with key: " + lookupKey);

        List<Map<String, Object>> allRows = performMultiRowLookup(lookupService, lookupKey, lookupConfig);

        logger.debug("Multi-row lookup for key '" + lookupKey + "' returned " + allRows.size() + " rows");

        boolean lookupSucceeded = !allRows.isEmpty();
        if (enrichment.getResultField() != null) {
            fieldAccessor.setFieldValue(targetObject, enrichment.getResultField(), lookupSucceeded);
            logger.info("Stored multi-row lookup result in field: " + enrichment.getResultField() + " = " + lookupSucceeded);
        }

        if (enrichment.getFieldMappings() != null && !enrichment.getFieldMappings().isEmpty()) {
            for (YamlEnrichment.FieldMapping mapping : enrichment.getFieldMappings()) {
                String targetField = mapping.getTargetField();
                if (targetField == null || targetField.trim().isEmpty()) {
                    continue;
                }

                boolean setSuccess = fieldAccessor.setFieldValue(targetObject, targetField, allRows);
                if (setSuccess) {
                    logger.debug("Multi-row mapping: set " + allRows.size() + " rows on target field '" + targetField + "'");
                } else {
                    boolean isRequired = mapping.getRequired() != null && mapping.getRequired();
                    if (isRequired) {
                        String errorMsg = "Multi-row lookup enrichment '" + enrichment.getId() +
                                        "' failed: could not set target field '" + targetField + "'";
                        logger.error(errorMsg);
                        throw new EnrichmentException(errorMsg);
                    }
                    logger.warn("Multi-row mapping: failed to set target field '" + targetField + "' (non-required, continuing)");
                }
            }
        }

        return targetObject;
    }

    // ─── Lookup Execution ────────────────────────────────────────────────

    /**
     * Perform a multi-row lookup, returning all matching rows.
     */
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> performMultiRowLookup(LookupService lookupService, Object lookupKey,
                                                    YamlEnrichment.LookupConfig lookupConfig) {

        String cacheKey = "all:" + lookupService.getName() + ":" + lookupKey.toString();

        if (lookupConfig.getCacheEnabled() != null && lookupConfig.getCacheEnabled()) {
            Object cached = cacheManager.get(ApexCacheManager.LOOKUP_RESULT_CACHE, cacheKey);
            if (cached instanceof List) {
                logger.trace("Cache hit for multi-row lookup key: " + lookupKey);
                return (List<Map<String, Object>>) cached;
            }
        }

        List<Map<String, Object>> results = lookupService.transformAll(lookupKey);

        if (lookupConfig.getCacheEnabled() != null && lookupConfig.getCacheEnabled()) {
            long ttlSeconds = lookupConfig.getCacheTtlSeconds() != null ?
                           lookupConfig.getCacheTtlSeconds() : 300L;
            cacheManager.put(ApexCacheManager.LOOKUP_RESULT_CACHE, cacheKey, results, ttlSeconds);
            logger.trace("Cached multi-row lookup result for key: " + lookupKey + " (" + results.size() + " rows)");
        }

        return results;
    }

    /**
     * Perform lookup operation with caching support.
     */
    Object performLookup(LookupService lookupService, Object lookupKey,
                         YamlEnrichment.LookupConfig lookupConfig) {

        String cacheKey = lookupService.getName() + ":" + lookupKey.toString();

        if (lookupConfig.getCacheEnabled() != null && lookupConfig.getCacheEnabled()) {
            Object cached = cacheManager.get(ApexCacheManager.LOOKUP_RESULT_CACHE, cacheKey);
            if (cached != null) {
                logger.trace("Cache hit for lookup key: " + lookupKey);
                return cached;
            }
        }

        Object result = lookupService.transform(lookupKey);

        if (lookupConfig.getCacheEnabled() != null && lookupConfig.getCacheEnabled()) {
            long ttlSeconds = lookupConfig.getCacheTtlSeconds() != null ?
                           lookupConfig.getCacheTtlSeconds() : 300L;
            cacheManager.put(ApexCacheManager.LOOKUP_RESULT_CACHE, cacheKey, result, ttlSeconds);
            logger.trace("Cached lookup result for key: " + lookupKey);
        }

        return result;
    }

    // ─── Service Resolution ──────────────────────────────────────────────

    /**
     * Resolve lookup service from either service registry or dataset configuration.
     */
    LookupService resolveLookupService(String enrichmentId, YamlEnrichment.LookupConfig lookupConfig) {
        // Priority 1: External service
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
            YamlRuleConfiguration currentConfig = configurationSupplier.get();

            DatasetSignature signature = DatasetSignature.from(dataset, currentConfig);
            String cacheKey = signature.toString();

            Object cached = cacheManager.get(ApexCacheManager.DATASET_CACHE, cacheKey);
            if (cached instanceof DatasetLookupService) {
                logger.info("Dataset cache HIT for signature: " + signature.toShortString());
                return (DatasetLookupService) cached;
            }

            String datasetServiceName = "dataset-" + signature.toShortString();

            try {
                DatasetLookupService datasetService = DatasetLookupServiceFactory
                    .createDatasetLookupService(datasetServiceName, dataset, currentConfig, this.dataSourceRegistry);

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
}
