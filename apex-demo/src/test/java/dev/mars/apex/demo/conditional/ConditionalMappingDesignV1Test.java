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

package dev.mars.apex.demo.conditional;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Conditional Mapping Design V1 functionality.
 * Tests the enhanced conditional field mapping with OR conditions and database lookups.
 * 
 * This test validates the ConditionalMappingDesignV1Test.yaml file which demonstrates:
 * - Enhanced conditional field mapping syntax
 * - OR logic in conditional mappings
 * - Database lookup integration
 * - FX transaction NDF processing
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConditionalMappingDesignV1Test extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ConditionalMappingDesignV1Test.class);
    
    @BeforeAll
    static void setupDatabase() {
        logger.info("================================================================================");
        logger.info("Setting up H2 database for Conditional Mapping Design V1 demo...");
        logger.info("================================================================================");
        
        try {
            // Create H2 database connection (file-based to match external config)
            Connection conn = DriverManager.getConnection("jdbc:h2:./target/h2-demo/conditional_mapping_v1;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
            Statement stmt = conn.createStatement();
            
            // Create currency rank table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS currency_rank (
                    currency_code VARCHAR(3) PRIMARY KEY,
                    currency_rank INTEGER NOT NULL
                )
            """);
            
            // Insert currency rank data
            stmt.execute("MERGE INTO currency_rank (currency_code, currency_rank) VALUES ('USD', 1)");
            stmt.execute("MERGE INTO currency_rank (currency_code, currency_rank) VALUES ('EUR', 2)");
            stmt.execute("MERGE INTO currency_rank (currency_code, currency_rank) VALUES ('GBP', 3)");
            stmt.execute("MERGE INTO currency_rank (currency_code, currency_rank) VALUES ('JPY', 4)");
            stmt.execute("MERGE INTO currency_rank (currency_code, currency_rank) VALUES ('CHF', 5)");
            
            // Create translation parameters table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS translation_parameters (
                    translation_type VARCHAR(50),
                    client_code VARCHAR(50),
                    system_code VARCHAR(50),
                    external_code VARCHAR(50),
                    internal_code VARCHAR(50),
                    PRIMARY KEY (translation_type, client_code, system_code, external_code)
                )
            """);
            
            // Insert translation data for NDF processing
            stmt.execute("MERGE INTO translation_parameters VALUES ('IS_NDF', 'CLIENT1', 'SWIFT', 'Y', 'YES')");
            stmt.execute("MERGE INTO translation_parameters VALUES ('IS_NDF', 'CLIENT1', 'SWIFT', 'N', 'NO')");
            stmt.execute("MERGE INTO translation_parameters VALUES ('IS_NDF', 'CLIENT1', 'SWIFT', 'TRUE', 'YES')");
            stmt.execute("MERGE INTO translation_parameters VALUES ('IS_NDF', 'CLIENT1', 'SWIFT', 'FALSE', 'NO')");
            stmt.execute("MERGE INTO translation_parameters VALUES ('IS_NDF', 'CLIENT1', 'SWIFT', 'TRUE', 'YES')");

            stmt.close();
            conn.close();
            
            logger.info("✓ H2 database setup completed successfully");
            
        } catch (Exception e) {
            logger.error("Failed to setup H2 database", e);
            throw new RuntimeException("Database setup failed", e);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should load and validate conditional mapping design v1 YAML")
    void shouldLoadConditionalMappingDesignV1Yaml() {
        logger.info("=== Testing Conditional Mapping Design V1 YAML Loading ===");
        
        try {
            // Load the actual ConditionalMappingDesignV1Test.yaml file
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalMappingDesignV1Test.yaml");
            assertNotNull(config, "Configuration should not be null");
            
            // Verify metadata
            assertEquals("Enhanced Conditional Field Mapping - FX Transaction", config.getMetadata().getName());
            assertEquals("2.0.0", config.getMetadata().getVersion());

            // Verify enrichments are present
            assertNotNull(config.getEnrichments(), "Enrichments should not be null");
            assertEquals(3, config.getEnrichments().size(), "Should have exactly 3 enrichments");
            
            logger.info("✓ Configuration loaded successfully: {}", config.getMetadata().getName());
            logger.info("  - Version: {}", config.getMetadata().getVersion());
            logger.info("  - Enrichments: {}", config.getEnrichments().size());
            
        } catch (Exception e) {
            logger.error("Failed to load YAML configuration", e);
            fail("Should be able to load YAML configuration: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should process direct NDF mapping for SWIFT system with valid values")
    void shouldProcessDirectNdfMappingForSwiftSystem() {
        logger.info("=== Testing Direct NDF Mapping for SWIFT System ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalMappingDesignV1Test.yaml");
            
            // Test data for direct mapping (SWIFT system with '1' value)
            Map<String, Object> testData = new HashMap<>();
            testData.put("BUY_CURRENCY", "USD");
            testData.put("IS_NDF", "1");
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("CLIENT_CODE", "CLIENT1");
            
            logger.debug("Direct mapping test data: {}", testData);
            
            // Process enrichments
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            logger.debug("Direct mapping enriched data: {}", enrichedData);
            
            // Verify currency rank was enriched
            assertEquals(1, enrichedData.get("BUY_CURRENCY_RANK"), "USD should have rank 1");
            
            // Verify direct NDF mapping was applied (should keep original value)
            assertEquals("1", enrichedData.get("IS_NDF"), "IS_NDF should remain '1' for direct mapping");
            
            logger.info("✓ Direct NDF mapping completed successfully");
            logger.info("  - Buy Currency: {} (Rank: {})", enrichedData.get("BUY_CURRENCY"), enrichedData.get("BUY_CURRENCY_RANK"));
            logger.info("  - IS_NDF: {} (Direct mapping)", enrichedData.get("IS_NDF"));
            
        } catch (Exception e) {
            logger.error("Direct NDF mapping test failed", e);
            fail("Direct NDF mapping test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should process NDF mapping with Y flag for SWIFT system")
    void shouldProcessNdfMappingWithYFlag() {
        logger.info("=== Testing NDF Mapping with Y Flag ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalMappingDesignV1Test.yaml");
            
            // Test data for Y flag mapping
            Map<String, Object> testData = new HashMap<>();
            testData.put("BUY_CURRENCY", "EUR");
            testData.put("IS_NDF", "Y");
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("CLIENT_CODE", "CLIENT1");
            
            logger.debug("Y flag mapping test data: {}", testData);
            
            // Process enrichments
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            logger.debug("Y flag mapping enriched data: {}", enrichedData);
            
            // Verify currency rank was enriched
            assertEquals(2, enrichedData.get("BUY_CURRENCY_RANK"), "EUR should have rank 2");
            
            // Verify Y flag mapping was applied (should keep original value)
            assertEquals("Y", enrichedData.get("IS_NDF"), "IS_NDF should remain 'Y' for direct mapping");
            
            logger.info("✓ Y flag NDF mapping completed successfully");
            logger.info("  - Buy Currency: {} (Rank: {})", enrichedData.get("BUY_CURRENCY"), enrichedData.get("BUY_CURRENCY_RANK"));
            logger.info("  - IS_NDF: {} (Y flag mapping)", enrichedData.get("IS_NDF"));
            
        } catch (Exception e) {
            logger.error("Y flag NDF mapping test failed", e);
            fail("Y flag NDF mapping test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should process translation mapping for non-standard values")
    void shouldProcessTranslationMappingForNonStandardValues() {
        logger.info("=== Testing Translation Mapping for Non-Standard Values ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalMappingDesignV1Test.yaml");
            
            // Test data for translation mapping (TRUE value needs translation)
            Map<String, Object> testData = new HashMap<>();
            testData.put("BUY_CURRENCY", "GBP");
            testData.put("IS_NDF", "TRUE");
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("CLIENT_CODE", "CLIENT1");
            
            logger.debug("Translation mapping test data: {}", testData);
            
            // Process enrichments
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            logger.debug("Translation mapping enriched data: {}", enrichedData);
            
            // Verify currency rank was enriched
            assertEquals(3, enrichedData.get("BUY_CURRENCY_RANK"), "GBP should have rank 3");
            
            // Verify translation parameters were set
            assertEquals("IS_NDF", enrichedData.get("TRANSLATION_TYPE"), "Translation type should be set");
            assertEquals("TRUE", enrichedData.get("EXTERNAL_CODE"), "External code should be set to original value");
            
            // Verify translation was applied (TRUE should become YES)
            assertEquals("YES", enrichedData.get("IS_NDF"), "IS_NDF should be translated to 'YES'");
            
            logger.info("✓ Translation mapping completed successfully");
            logger.info("  - Buy Currency: {} (Rank: {})", enrichedData.get("BUY_CURRENCY"), enrichedData.get("BUY_CURRENCY_RANK"));
            logger.info("  - Translation: {} -> {}", enrichedData.get("EXTERNAL_CODE"), enrichedData.get("IS_NDF"));
            logger.info("  - Translation Type: {}", enrichedData.get("TRANSLATION_TYPE"));
            
        } catch (Exception e) {
            logger.error("Translation mapping test failed", e);
            fail("Translation mapping test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should use default mapping for edge cases")
    void shouldUseDefaultMappingForEdgeCases() {
        logger.info("=== Testing Default Mapping for Edge Cases ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalMappingDesignV1Test.yaml");
            
            // Test data for default mapping (non-SWIFT system)
            Map<String, Object> testData = new HashMap<>();
            testData.put("BUY_CURRENCY", "JPY");
            testData.put("IS_NDF", "UNKNOWN");
            testData.put("SYSTEM_CODE", "OTHER");
            testData.put("CLIENT_CODE", "CLIENT1");
            
            logger.debug("Default mapping test data: {}", testData);
            
            // Process enrichments
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            logger.debug("Default mapping enriched data: {}", enrichedData);
            
            // Verify currency rank was enriched
            assertEquals(4, enrichedData.get("BUY_CURRENCY_RANK"), "JPY should have rank 4");
            
            // Verify default mapping was applied (should keep original value)
            assertEquals("UNKNOWN", enrichedData.get("IS_NDF"), "IS_NDF should remain 'UNKNOWN' for default mapping");
            
            logger.info("✓ Default mapping completed successfully");
            logger.info("  - Buy Currency: {} (Rank: {})", enrichedData.get("BUY_CURRENCY"), enrichedData.get("BUY_CURRENCY_RANK"));
            logger.info("  - IS_NDF: {} (Default mapping)", enrichedData.get("IS_NDF"));
            
        } catch (Exception e) {
            logger.error("Default mapping test failed", e);
            fail("Default mapping test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Should validate APEX services initialization")
    void shouldValidateApexServicesInitialization() {
        logger.info("=== Testing APEX Services Initialization ===");
        
        // Verify all APEX services are properly initialized
        assertNotNull(yamlLoader, "YAML loader should be initialized");
        assertNotNull(enrichmentService, "Enrichment service should be initialized");
        
        logger.info("✓ All APEX services properly initialized for conditional mapping design v1");
    }
}
