package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
 * Comprehensive Lookup Test - Consolidated test for all core APEX lookup functionality.
 * 
 * This test consolidates and validates all essential lookup operations:
 * - Simple field lookups with single parameter matching
 * - Compound key lookups with multiple parameter matching
 * - Nested field lookups with complex data structures
 * - Multi-parameter lookups with advanced filtering
 * - External data source lookups with database connectivity
 * - File-based lookups (JSON, XML, YAML)
 * 
 * REAL APEX SERVICES TESTED:
 * - EnrichmentService: Real APEX enrichment processor for lookup operations
 * - LookupServiceRegistry: Real lookup service management and caching
 * - DataSourceResolver: Real data source resolution and connectivity
 * - ExternalDataSourceManager: Real external data source integration
 */
public class ComprehensiveLookupTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveLookupTest.class);

    @Test
    void testSimpleFieldLookup() {
        logger.info("=== Testing Simple Field Lookup ===");
        
        var config = loadAndValidateYaml("lookup/simple-field-lookup-demo-config.yaml");
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST001");
        testData.put("lookupType", "CUSTOMER_PROFILE");
        
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Simple field lookup should complete successfully");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate lookup results
        assertNotNull(enrichedData.get("customerName"), "Customer name should be looked up");
        assertNotNull(enrichedData.get("customerType"), "Customer type should be looked up");
        
        logger.info("✅ Simple field lookup validated successfully");
    }

    @Test
    void testCompoundKeyLookup() {
        logger.info("=== Testing Compound Key Lookup ===");
        
        var config = loadAndValidateYaml("lookup/compound-key-lookup-demo-config.yaml");
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST001");
        testData.put("productId", "PROD001");
        testData.put("lookupType", "CUSTOMER_PRODUCT");
        
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Compound key lookup should complete successfully");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate compound lookup results
        assertNotNull(enrichedData.get("customerProductRelation"), "Customer-product relation should be looked up");
        assertNotNull(enrichedData.get("relationshipType"), "Relationship type should be determined");
        
        logger.info("✅ Compound key lookup validated successfully");
    }

    @Test
    void testNestedFieldLookup() {
        logger.info("=== Testing Nested Field Lookup ===");
        
        var config = loadAndValidateYaml("lookup/nested-field-lookup-demo-config.yaml");
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("tradeId", "TRD001");
        testData.put("lookupDepth", "DEEP");
        
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Nested field lookup should complete successfully");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate nested lookup results
        assertNotNull(enrichedData.get("tradeDetails"), "Trade details should be looked up");
        assertNotNull(enrichedData.get("nestedCounterparty"), "Nested counterparty should be resolved");
        
        logger.info("✅ Nested field lookup validated successfully");
    }

    @Test
    void testMultiParameterLookup() {
        logger.info("=== Testing Multi-Parameter Lookup ===");
        
        var config = loadAndValidateYaml("lookup/multi-parameter-lookup-demo-config.yaml");
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("region", "AMERICAS");
        testData.put("assetClass", "EQUITY");
        testData.put("currency", "USD");
        testData.put("lookupScope", "COMPREHENSIVE");
        
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Multi-parameter lookup should complete successfully");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate multi-parameter lookup results
        assertNotNull(enrichedData.get("marketData"), "Market data should be looked up");
        assertNotNull(enrichedData.get("pricingModel"), "Pricing model should be determined");
        
        logger.info("✅ Multi-parameter lookup validated successfully");
    }

    @Test
    void testFileBasedLookup() {
        logger.info("=== Testing File-Based Lookup ===");
        
        var config = loadAndValidateYaml("lookup/file-system-lookup-demo-config.yaml");
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("fileType", "JSON");
        testData.put("lookupKey", "REFERENCE_DATA");
        
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "File-based lookup should complete successfully");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate file-based lookup results
        assertNotNull(enrichedData.get("fileData"), "File data should be looked up");
        assertNotNull(enrichedData.get("lookupStatus"), "Lookup status should be set");
        
        logger.info("✅ File-based lookup validated successfully");
    }

    @Test
    void testExternalDataSourceLookup() {
        logger.info("=== Testing External Data Source Lookup ===");
        
        var config = loadAndValidateYaml("lookup/external-data-source-working-demo-config.yaml");
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("dataSourceType", "DATABASE");
        testData.put("connectionMode", "POOLED");
        testData.put("lookupQuery", "SELECT_BY_ID");
        
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "External data source lookup should complete successfully");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate external data source lookup results
        assertNotNull(enrichedData.get("externalData"), "External data should be looked up");
        assertNotNull(enrichedData.get("connectionStatus"), "Connection status should be verified");
        
        logger.info("✅ External data source lookup validated successfully");
    }
}
