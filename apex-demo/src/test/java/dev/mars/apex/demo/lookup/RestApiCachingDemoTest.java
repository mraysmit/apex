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
package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.infrastructure.DemoTestBase;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REST API Caching Demonstration Test
 *
 * This test demonstrates the effectiveness of REST API caching by comparing:
 * 1. Slow endpoint (10-second delay) - shows dramatic cache improvement
 * 2. Fast endpoint (no delay) - shows baseline cache improvement
 *
 * Key Features Demonstrated:
 * - Cache miss vs cache hit timing differences
 * - Multiple currency lookups to test cache isolation
 * - Performance metrics and logging
 * - Cache effectiveness validation
 *
 * Debug Logging:
 * - Enable with: -Dorg.slf4j.simpleLogger.log.dev.mars.apex.demo.lookup.RestApiCachingDemoTest=debug
 * - Or set logger level to DEBUG in your IDE/logback configuration
 * - Additional cache debugging: -Dorg.slf4j.simpleLogger.log.dev.mars.apex.core.service.data.external.cache=debug
 *
 * @author APEX Demo Team
 * @since 2025-09-22
 * @version 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RestApiCachingDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RestApiCachingDemoTest.class);

    private static RestApiTestableServer slowServer;
    private static RestApiTestableServer fastServer;
    private static String slowBaseUrl;
    private static String fastBaseUrl;

    @BeforeAll
    static void setup() throws Exception {
        logger.info("================================================================================");
        logger.info("Setting up REST API Caching Demonstration");
        logger.info("================================================================================");

        // Create slow server (10-second delay)
        slowServer = new RestApiTestableServer(10);
        slowServer.start();
        slowBaseUrl = slowServer.getBaseUrl();
        logger.info("Slow server started at: {} (10-second delay)", slowBaseUrl);

        // Create fast server (no delay)
        fastServer = new RestApiTestableServer(0);
        fastServer.start();
        fastBaseUrl = fastServer.getBaseUrl();
        logger.info("Fast server started at: {} (no delay)", fastBaseUrl);

        logger.info("================================================================================");
    }

    @AfterAll
    static void teardown() {
        logger.info("================================================================================");
        logger.info("Tearing down REST API Caching Demonstration");
        logger.info("================================================================================");

        if (slowServer != null) {
            slowServer.stop();
            logger.info("Slow server stopped");
        }
        if (fastServer != null) {
            fastServer.stop();
            logger.info("Fast server stopped");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should demonstrate dramatic cache improvement with slow endpoint")
    void testSlowEndpointCachingEffectiveness() throws Exception {
        logger.info("================================================================================");
        logger.info("Testing Slow Endpoint Caching Effectiveness (10-second delay)");
        logger.info("================================================================================");

        // Load YAML configuration for slow endpoint
        var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/RestApiCachingDemoTest-slow.yaml");
        updateRestApiBaseUrl(config, slowBaseUrl);

        logger.debug("DEBUG: Configuration has {} data sources", config.getDataSources().size());
        config.getDataSources().forEach(ds ->
            logger.debug("DEBUG: Data source: {} (type: {})", ds.getName(), ds.getType())
        );

        // Test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("currencyCode", "USD");

        logger.debug("DEBUG: Input test data: {}", testData);
        logger.debug("DEBUG: Input test data keys: {}", testData.keySet());
        testData.forEach((key, value) ->
            logger.debug("DEBUG: Input field: {} = {}", key, value)
        );

        logger.info("First call (cache miss) - expecting ~10 second delay...");
        logger.debug("DEBUG: About to call enrichmentService.enrichObject for first time...");
        long startTime = System.currentTimeMillis();

        // First call - should take ~10 seconds (cache miss)
        Object result1 = enrichmentService.enrichObject(config, testData);

        long firstCallTime = System.currentTimeMillis() - startTime;
        logger.info("First call completed in: {}ms", firstCallTime);
        logger.debug("DEBUG: First call enrichmentService.enrichObject completed");

        // Validate first call result
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData1 = (Map<String, Object>) result1;

        logger.debug("DEBUG: First call result: {}", result1);
        logger.debug("DEBUG: First call result type: {}", result1 != null ? result1.getClass().getSimpleName() : "null");
        if (enrichedData1 != null) {
            logger.debug("DEBUG: First call result keys: {}", enrichedData1.keySet());
            enrichedData1.forEach((key, value) ->
                logger.debug("DEBUG: First call result field: {} = {}", key, value)
            );
        }

        assertEquals("US Dollar", enrichedData1.get("currencyName"), "First call should return correct currency name");

        // Validate timing - should be at least 9.5 seconds
        assertTrue(firstCallTime >= 9500, 
            "First call should take >= 9500ms (cache miss with 10s delay), was: " + firstCallTime + "ms");

        logger.info("Second call (cache hit) - expecting <100ms...");
        logger.debug("DEBUG: About to call enrichmentService.enrichObject for second time (should be cached)...");
        startTime = System.currentTimeMillis();

        // Second call - should be very fast (cache hit)
        Object result2 = enrichmentService.enrichObject(config, testData);

        long secondCallTime = System.currentTimeMillis() - startTime;
        logger.info("Second call completed in: {}ms", secondCallTime);
        logger.debug("DEBUG: Second call enrichmentService.enrichObject completed");

        // Validate second call result
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData2 = (Map<String, Object>) result2;

        logger.debug("DEBUG: Second call result: {}", result2);
        logger.debug("DEBUG: Second call result type: {}", result2 != null ? result2.getClass().getSimpleName() : "null");
        if (enrichedData2 != null) {
            logger.debug("DEBUG: Second call result keys: {}", enrichedData2.keySet());
            enrichedData2.forEach((key, value) ->
                logger.debug("DEBUG: Second call result field: {} = {}", key, value)
            );
        }

        assertEquals("US Dollar", enrichedData2.get("currencyName"), "Second call should return correct currency name");

        // Validate timing - should be very fast (cache hit)
        assertTrue(secondCallTime < 100, 
            "Second call should take < 100ms (cache hit), was: " + secondCallTime + "ms");

        // Calculate improvement
        double improvementRatio = (double) firstCallTime / secondCallTime;
        logger.debug("DEBUG: Calculating cache performance improvement...");
        logger.debug("DEBUG: First call time: {}ms, Second call time: {}ms", firstCallTime, secondCallTime);
        logger.debug("DEBUG: Improvement ratio calculation: {} / {} = {}", firstCallTime, secondCallTime, improvementRatio);

        logger.info("Cache Performance Improvement:");
        logger.info("   First call (cache miss): {}ms", firstCallTime);
        logger.info("   Second call (cache hit): {}ms", secondCallTime);
        logger.info("   Improvement ratio: {}x faster", String.format("%.1f", improvementRatio));
        logger.info("   Time saved: {}ms", firstCallTime - secondCallTime);

        // Assert significant improvement
        assertTrue(improvementRatio > 50, 
            "Cache should provide at least 50x improvement, was: " + String.format("%.1f", improvementRatio) + "x");

        logger.info("Slow endpoint caching demonstration completed successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Should demonstrate cache effectiveness with fast endpoint")
    void testFastEndpointCachingBaseline() throws Exception {
        logger.info("================================================================================");
        logger.info("Testing Fast Endpoint Caching Baseline (no delay)");
        logger.info("================================================================================");

        // Load YAML configuration for fast endpoint
        var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/RestApiCachingDemoTest-fast.yaml");
        updateRestApiBaseUrl(config, fastBaseUrl);

        logger.debug("DEBUG: Fast endpoint configuration has {} data sources", config.getDataSources().size());
        config.getDataSources().forEach(ds ->
            logger.debug("DEBUG: Fast endpoint data source: {} (type: {})", ds.getName(), ds.getType())
        );

        // Test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("currencyCode", "EUR");

        logger.debug("DEBUG: Fast endpoint input test data: {}", testData);

        logger.info("First call (cache miss) - expecting <500ms...");
        long startTime = System.currentTimeMillis();

        // First call - should be fast but not cached
        Object result1 = enrichmentService.enrichObject(config, testData);

        long firstCallTime = System.currentTimeMillis() - startTime;
        logger.info("First call completed in: {}ms", firstCallTime);

        // Validate first call result
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData1 = (Map<String, Object>) result1;
        assertEquals("Euro", enrichedData1.get("currencyName"), "First call should return correct currency name");

        logger.info("Second call (cache hit) - expecting even faster...");
        startTime = System.currentTimeMillis();

        // Second call - should be even faster (cache hit)
        Object result2 = enrichmentService.enrichObject(config, testData);

        long secondCallTime = System.currentTimeMillis() - startTime;
        logger.info("Second call completed in: {}ms", secondCallTime);

        // Validate second call result
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData2 = (Map<String, Object>) result2;
        assertEquals("Euro", enrichedData2.get("currencyName"), "Second call should return correct currency name");

        // Calculate improvement
        double improvementRatio = firstCallTime > 0 ? (double) firstCallTime / Math.max(secondCallTime, 1) : 1.0;
        logger.info("Cache Performance Improvement:");
        logger.info("   First call (cache miss): {}ms", firstCallTime);
        logger.info("   Second call (cache hit): {}ms", secondCallTime);
        logger.info("   Improvement ratio: {}x faster", String.format("%.1f", improvementRatio));
        logger.info("   Time saved: {}ms", Math.max(firstCallTime - secondCallTime, 0));

        // Assert some improvement (even if small)
        assertTrue(secondCallTime <= firstCallTime, 
            "Second call should be <= first call time due to caching");

        logger.info("Fast endpoint caching baseline completed successfully");
    }

    @Test
    @Order(3)
    @DisplayName("Should demonstrate cache isolation with multiple currencies")
    void testCacheIsolationWithMultipleCurrencies() throws Exception {
        logger.info("================================================================================");
        logger.info("Testing Cache Isolation with Multiple Currencies");
        logger.info("================================================================================");

        // Load YAML configuration for slow endpoint
        var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/RestApiCachingDemoTest-slow.yaml");
        updateRestApiBaseUrl(config, slowBaseUrl);

        // Test different currencies to verify cache isolation
        String[] currencies = {"GBP", "JPY", "GBP"}; // Note: GBP repeated to test cache hit
        String[] expectedNames = {"British Pound", "Japanese Yen", "British Pound"};

        for (int i = 0; i < currencies.length; i++) {
            String currency = currencies[i];
            String expectedName = expectedNames[i];
            boolean shouldBeCached = i == 2; // Third call (GBP again) should be cached

            logger.info("Testing currency: {} ({})", currency, shouldBeCached ? "cache hit expected" : "cache miss expected");
            logger.debug("DEBUG: Cache isolation test - currency: {}, iteration: {}, shouldBeCached: {}", currency, i, shouldBeCached);

            Map<String, Object> testData = new HashMap<>();
            testData.put("currencyCode", currency);

            logger.debug("DEBUG: Cache isolation test data: {}", testData);

            long startTime = System.currentTimeMillis();
            logger.debug("DEBUG: About to call enrichmentService.enrichObject for currency: {}", currency);
            Object result = enrichmentService.enrichObject(config, testData);
            long callTime = System.currentTimeMillis() - startTime;
            logger.debug("DEBUG: Call completed for currency: {} in {}ms", currency, callTime);

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("DEBUG: Cache isolation result for {}: {}", currency, result);
            if (enrichedData != null) {
                logger.debug("DEBUG: Cache isolation result keys for {}: {}", currency, enrichedData.keySet());
                enrichedData.forEach((key, value) ->
                    logger.debug("DEBUG: Cache isolation result field for {}: {} = {}", currency, key, value)
                );
            }

            assertEquals(expectedName, enrichedData.get("currencyName"),
                "Currency " + currency + " should return correct name");

            if (shouldBeCached) {
                assertTrue(callTime < 100,
                    "Cached call for " + currency + " should be < 100ms, was: " + callTime + "ms");
                logger.info("Cache hit for {}: {}ms", currency, callTime);
            } else {
                assertTrue(callTime >= 9500,
                    "Non-cached call for " + currency + " should be >= 9500ms, was: " + callTime + "ms");
                logger.info("Cache miss for {}: {}ms", currency, callTime);
            }
        }

        logger.info("Cache isolation test completed successfully");
    }
}
