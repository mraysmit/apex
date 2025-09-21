/*
 * Copyright (c) 2025 APEX Rules Engine Contributors
 * Licensed under the Apache License, Version 2.0
 * Author: APEX Demo Team
 */
package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REST API Simple YAML Test
 *
 * The simplest possible YAML-based test for REST API lookup validation.
 * Uses RestApiTestableServer + APEX enrichment + minimal YAML configuration.
 *
 * @author APEX Demo Team
 * @since 2025-09-21
 * @version 2.0.0 (Renamed with RestApi prefix for consistency)
 */
class RestApiSimpleYamlTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RestApiSimpleYamlTest.class);

    private static RestApiTestableServer testServer;

    @BeforeAll
    static void setup() throws Exception {
        testServer = new RestApiTestableServer();
        testServer.start();
        logger.info("Test server started at: {}", testServer.getBaseUrl());
    }

    @AfterAll
    static void teardown() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    @Test
    void testSimpleRestApiLookup() throws Exception {
        logger.info("Testing simple REST API lookup with YAML...");

        // Load YAML configuration
        var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/RestApiSimpleYamlTest.yaml");

        // Update base URL in config to use our test server
        updateRestApiBaseUrl(config, testServer.getBaseUrl());

        logger.info("Updated YAML config to use test server at: " + testServer.getBaseUrl());

        // Test data (use mutable HashMap)
        Map<String, Object> testData = new HashMap<>();
        testData.put("currencyCode", "USD");
        logger.info("DEBUG: Input test data: {}", testData);

        logger.info("DEBUG: Configuration has {} data sources", config.getDataSources().size());
        config.getDataSources().forEach(ds -> 
            logger.info("DEBUG: Data source: {} (type: {})", ds.getName(), ds.getType())
        );

        logger.info("DEBUG: About to call enrichmentService.enrichObject...");

        // Debug source dataset
        logger.info("DEBUG: Source dataset (before enrichment): {}", testData);
        logger.info("DEBUG: Source dataset keys: {}", testData.keySet());
        testData.forEach((key, value) -> 
            logger.info("DEBUG: Source field: {} = {}", key, value)
        );

        // Process with APEX
        Object result = enrichmentService.enrichObject(config, testData);

        logger.info("DEBUG: enrichmentService.enrichObject completed");

        // Debug enriched dataset
        logger.info("DEBUG: Source dataset (after enrichment): {}", testData);
        logger.info("DEBUG: Source dataset keys after enrichment: {}", testData.keySet());
        testData.forEach((key, value) -> 
            logger.info("DEBUG: Source field after enrichment: {} = {}", key, value)
        );

        logger.info("DEBUG: Enrichment result: {}", result);
        logger.info("DEBUG: Result type: {}", result != null ? result.getClass().getSimpleName() : "null");

        // Cast result to Map for assertions
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        if (enrichedData != null) {
            logger.info("DEBUG: Result keys: {}", enrichedData.keySet());
            enrichedData.forEach((key, value) ->
                logger.info("DEBUG:   {} = {}", key, value)
            );
        }

        // Simple assertion
        logger.info("DEBUG: Checking currencyName field...");
        Object currencyName = enrichedData.get("currencyName");
        logger.info("DEBUG: currencyName value: {} (type: {})", currencyName, currencyName != null ? currencyName.getClass().getSimpleName() : "null");
        assertEquals("US Dollar", currencyName, "Currency name should be 'US Dollar'");
        
        logger.info("✅ Simple REST API lookup completed successfully");
    }

    @Test
    void testSimpleRestApiLookupWithEUR() throws Exception {
        logger.info("Testing simple REST API lookup with EUR...");

        // Load YAML configuration
        var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/RestApiSimpleYamlTest.yaml");

        // Update base URL in config to use our test server
        updateRestApiBaseUrl(config, testServer.getBaseUrl());

        // Test data for EUR
        Map<String, Object> testData = new HashMap<>();
        testData.put("currencyCode", "EUR");

        // Process with APEX
        Object result = enrichmentService.enrichObject(config, testData);

        // Cast result to Map for assertions
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Assertions for EUR
        assertEquals("Euro", enrichedData.get("currencyName"), "Currency name should be 'Euro'");
        
        logger.info("✅ Simple REST API lookup with EUR completed successfully");
    }
}
