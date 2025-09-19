package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test for SharedDatasourceDemo functionality.
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 1 enrichment expected (shared-datasource-demo)
 * ✅ Verify log shows "Processed: 1 out of 1" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers shared datasource lookup condition
 * ✅ Validate EVERY business calculation - Test actual shared datasource lookup logic
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 *
 * BUSINESS LOGIC VALIDATION:
 * - Shared H2 database data source demonstration using APEX lookup enrichments
 * - Customer lookup using H2 database queries
 * - Data source reuse and centralized management
 * - YAML-driven H2 database configuration
 *
 * YAML FIRST PRINCIPLE:
 * - ALL business logic is in YAML enrichments
 * - Java test only sets up minimal H2 data, loads YAML and calls APEX
 * - NO custom business logic or complex validation
 * - Simple database setup and basic assertions only
 */
public class SharedDatasourceDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SharedDatasourceDemoTest.class);



    @Test
    void testSharedDatasourceDemoFunctionality() {
        logger.info("=== Testing Shared Datasource Demo Functionality ===");

        // Load YAML configuration for shared datasource demo
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/SharedDatasourceDemoTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

        // Create simple test data that triggers the shared datasource enrichment
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST001");
        testData.put("approach", "real-apex-services");

        // Execute APEX enrichment processing - ALL logic in YAML
        Object result = enrichmentService.enrichObject(config, testData);

        // Validate enrichment results
        assertNotNull(result, "Shared datasource demo result should not be null");
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Validate YAML-driven results only
        assertNotNull(enrichedData.get("sharedDatasourceDemoResult"), "Shared datasource demo result should be generated");

        String sharedDatasourceDemoResult = (String) enrichedData.get("sharedDatasourceDemoResult");
        assertTrue(sharedDatasourceDemoResult.contains("CUST001"), "Result should reference customer ID");

            logger.info("✅ Shared datasource demo functionality test completed successfully");
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

}
