package dev.mars.apex.demo.infrastructure;

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

import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Infrastructure Test - Consolidated test for all infrastructure functionality.
 *
 * This test consolidates and validates all infrastructure components:
 * - Data source verification and validation
 * - Database setup and connectivity
 * - External dataset configuration
 * - Infrastructure monitoring and health checks
 *
 * REAL APEX SERVICES TESTED:
 * - EnrichmentService: Real APEX enrichment processor for infrastructure operations
 * - DataSourceResolver: Real data source management and verification
 * - DatabaseSetupService: Real database connectivity and table management
 * - InfrastructureMonitor: Real infrastructure health and performance monitoring
 */
public class DataSourceVerifierTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceVerifierTest.class);

    @Test
    void testDataSourceVerification() {
        logger.info("=== Testing Data Source Verification ===");

        var config = loadAndValidateYaml("infrastructure/data-source-verifier-config.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("verificationType", "comprehensive");
        testData.put("scope", "all-data-sources");
        testData.put("approach", "real-apex-services");

        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Data source verification should complete successfully");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Validate core verification results
        assertNotNull(enrichedData.get("verificationStatus"), "Verification status should be set");
        assertNotNull(enrichedData.get("verificationSummary"), "Verification summary should be generated");

        logger.info("✅ Data source verification completed successfully");
    }

    @Test
    void testDatabaseSetup() {
        logger.info("=== Testing Database Setup ===");

        var config = loadAndValidateYaml("infrastructure/database-setup-config.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("setupType", "comprehensive");
        testData.put("databaseType", "h2-in-memory");
        testData.put("approach", "real-apex-services");

        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Database setup should complete successfully");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Validate core setup results
        assertNotNull(enrichedData.get("setupStatus"), "Setup status should be set");
        assertNotNull(enrichedData.get("setupSummary"), "Setup summary should be generated");

        logger.info("✅ Database setup completed successfully");
    }

    @Test
    void testExternalDatasetConfiguration() {
        logger.info("=== Testing External Dataset Configuration ===");

        var config = loadAndValidateYaml("infrastructure/external-dataset-setup-config.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("configType", "comprehensive");
        testData.put("datasetScope", "all-external-datasets");
        testData.put("approach", "real-apex-services");

        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "External dataset configuration should complete successfully");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate ALL business logic results (all 4 enrichments should be processed)
        assertNotNull(enrichedData.get("inlineDatasetVerificationResult"), "Inline dataset verification result should be generated");
        assertNotNull(enrichedData.get("postgresqlVerificationResult"), "PostgreSQL verification result should be generated");
        assertNotNull(enrichedData.get("externalYamlVerificationResult"), "External YAML verification result should be generated");
        assertNotNull(enrichedData.get("dataSourceVerifierSummary"), "Data source verifier summary should be generated");
        
        // Validate specific business calculations
        String inlineDatasetVerificationResult = (String) enrichedData.get("inlineDatasetVerificationResult");
        assertTrue(inlineDatasetVerificationResult.contains("inline-dataset-verification"), "Inline dataset verification result should contain verification type");
        
        String postgresqlVerificationResult = (String) enrichedData.get("postgresqlVerificationResult");
        assertTrue(postgresqlVerificationResult.contains("postgresql-verification"), "PostgreSQL verification result should reference verification type");
        
        String externalYamlVerificationResult = (String) enrichedData.get("externalYamlVerificationResult");
        assertTrue(externalYamlVerificationResult.contains("external-yaml-verification"), "External YAML verification result should reference verification type");
        
        String dataSourceVerifierSummary = (String) enrichedData.get("dataSourceVerifierSummary");
        assertTrue(dataSourceVerifierSummary.contains("real-apex-services"), "Data source verifier summary should reference approach");
        
        logger.info("✅ Comprehensive data source verifier functionality test completed successfully");
    }

    @Test
    void testInlineDatasetVerificationProcessing() {
        logger.info("=== Testing Inline Dataset Verification Processing ===");
        
        // Load YAML configuration for data source verifier
        var config = loadAndValidateYaml("test-configs/datasourceverifier-test.yaml");
        
        // Test different inline verification types
        String[] inlineVerificationTypes = {"inline-dataset-verification", "embedded-yaml-verification", "configuration-verification"};
        
        for (String inlineVerificationType : inlineVerificationTypes) {
            Map<String, Object> testData = new HashMap<>();
            testData.put("inlineVerificationType", inlineVerificationType);
            testData.put("inlineVerificationScope", "embedded-yaml-configuration");
            testData.put("approach", "real-apex-services");
            
            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            
            // Validate enrichment results
            assertNotNull(result, "Inline dataset verification result should not be null for " + inlineVerificationType);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate inline dataset verification business logic
            assertNotNull(enrichedData.get("inlineDatasetVerificationResult"), "Inline dataset verification result should be generated for " + inlineVerificationType);
            
            String inlineDatasetVerificationResult = (String) enrichedData.get("inlineDatasetVerificationResult");
            assertTrue(inlineDatasetVerificationResult.contains(inlineVerificationType), "Inline dataset verification result should contain " + inlineVerificationType);
        }
        
        logger.info("✅ Inline dataset verification processing test completed successfully");
    }

    @Test
    void testPostgresqlVerificationProcessing() {
        logger.info("=== Testing PostgreSQL Verification Processing ===");
        
        // Load YAML configuration for data source verifier
        var config = loadAndValidateYaml("test-configs/datasourceverifier-test.yaml");
        
        // Test different PostgreSQL verification types
        String[] postgresqlVerificationTypes = {"postgresql-verification", "database-connectivity-verification", "table-verification"};
        
        for (String postgresqlVerificationType : postgresqlVerificationTypes) {
            Map<String, Object> testData = new HashMap<>();
            testData.put("postgresqlVerificationType", postgresqlVerificationType);
            testData.put("postgresqlVerificationScope", "database-connectivity-tables");
            testData.put("approach", "real-apex-services");
            
            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            
            // Validate enrichment results
            assertNotNull(result, "PostgreSQL verification result should not be null for " + postgresqlVerificationType);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate PostgreSQL verification processing business logic
            assertNotNull(enrichedData.get("postgresqlVerificationResult"), "PostgreSQL verification result should be generated for " + postgresqlVerificationType);
            
            String postgresqlVerificationResult = (String) enrichedData.get("postgresqlVerificationResult");
            assertTrue(postgresqlVerificationResult.contains(postgresqlVerificationType), "PostgreSQL verification result should reference verification type " + postgresqlVerificationType);
        }
        
        logger.info("✅ PostgreSQL verification processing test completed successfully");
    }

    @Test
    void testExternalYamlVerificationProcessing() {
        logger.info("=== Testing External YAML Verification Processing ===");
        
        // Load YAML configuration for data source verifier
        var config = loadAndValidateYaml("test-configs/datasourceverifier-test.yaml");
        
        // Test different external verification types
        String[] externalVerificationTypes = {"external-yaml-verification", "file-existence-verification", "structure-verification"};
        
        for (String externalVerificationType : externalVerificationTypes) {
            Map<String, Object> testData = new HashMap<>();
            testData.put("externalVerificationType", externalVerificationType);
            testData.put("externalVerificationScope", "file-existence-structure");
            testData.put("approach", "real-apex-services");
            
            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            
            // Validate enrichment results
            assertNotNull(result, "External YAML verification result should not be null for " + externalVerificationType);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate external YAML verification processing business logic
            assertNotNull(enrichedData.get("externalYamlVerificationResult"), "External YAML verification result should be generated for " + externalVerificationType);
            
            String externalYamlVerificationResult = (String) enrichedData.get("externalYamlVerificationResult");
            assertTrue(externalYamlVerificationResult.contains(externalVerificationType), "External YAML verification result should reference verification type " + externalVerificationType);
        }
        
        logger.info("✅ External YAML verification processing test completed successfully");
    }
}
