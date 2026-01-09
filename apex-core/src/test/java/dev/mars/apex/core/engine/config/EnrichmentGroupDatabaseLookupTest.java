package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CRITICAL REGRESSION TEST: Enrichment Groups with Database Lookups
 * 
 * This test addresses a critical test coverage gap where enrichment-groups
 * containing database lookup enrichments were never functionally tested.
 * 
 * BUG DISCOVERED: When enrichment-groups execute database lookups, the
 * yamlConfig parameter (containing datasource configurations) was not
 * being passed through the enrichment-group processing chain, causing
 * database lookups to fail with null configuration.
 * 
 * Root Cause Analysis:
 * - 110 YAML files test enrichment-groups (with inline enrichments)
 * - 363 YAML files test database lookups (as standalone enrichments)
 * - ZERO tests executed enrichment-groups WITH database lookups
 * 
 * This test ensures:
 * 1. Enrichment-groups can execute database lookup enrichments
 * 2. YamlConfig (with datasources) is passed through the call chain
 * 3. Database connections are established and queries execute
 * 4. Results are properly enriched into the target object
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-12-03
 */
@DisplayName("CRITICAL: Enrichment Groups with Database Lookups")
public class EnrichmentGroupDatabaseLookupTest {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentGroupDatabaseLookupTest.class);

    private YamlConfigurationLoader yamlLoader;
    private Connection h2Connection;

    @BeforeEach
    void setUp() throws Exception {
        yamlLoader = new YamlConfigurationLoader();
        
        // Setup H2 in-memory database with test data
        setupH2Database();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (h2Connection != null && !h2Connection.isClosed()) {
            h2Connection.close();
        }
    }

    private void setupH2Database() throws Exception {
        logger.info("Setting up H2 database with test data...");
        
        // Create H2 in-memory database
        h2Connection = DriverManager.getConnection(
            "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        
        // Create test table and insert data
        try (Statement stmt = h2Connection.createStatement()) {
            
            // Create customers table
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute(
                "CREATE TABLE customers (" +
                "  customer_id VARCHAR(50) PRIMARY KEY," +
                "  customer_name VARCHAR(100) NOT NULL," +
                "  credit_rating VARCHAR(20)," +
                "  country VARCHAR(50)" +
                ")"
            );
            
            // Insert test data
            stmt.execute("INSERT INTO customers VALUES ('CUST001', 'Acme Corp', 'AAA', 'USA')");
            stmt.execute("INSERT INTO customers VALUES ('CUST002', 'Global Industries', 'AA', 'UK')");
            stmt.execute("INSERT INTO customers VALUES ('CUST003', 'Tech Solutions', 'A', 'Germany')");
            
            // DEBUG: Verify data was inserted
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers");
            rs.next();
            int count = rs.getInt(1);
            logger.info("✓ H2 database setup complete with {} test customers", count);
            
            // DEBUG: Verify we can query the first customer
            rs = stmt.executeQuery("SELECT customer_name FROM customers WHERE customer_id = 'CUST001'");
            if (rs.next()) {
                String name = rs.getString(1);
                logger.info("✓ Verified CUST001 exists with name: {}", name);
            } else {
                logger.error("ERROR: CUST001 not found after insert!");
            }
        }
    }

    @Test
    @DisplayName("CRITICAL: Enrichment-group with database lookup should pass yamlConfig and execute successfully")
    void testEnrichmentGroupWithDatabaseLookup() throws Exception {
        logger.info("TEST START: Enrichment-group with database lookup");
        
        // Load configuration with enrichment-group containing database lookup
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/resources/lookups/enrichment-group-database-lookup-test.yaml"
        );
        
        assertNotNull(config, "Configuration should load successfully");
        assertNotNull(config.getDataSources(), "Configuration should have datasources");
        assertFalse(config.getDataSources().isEmpty(), "Configuration should have at least one datasource");
        
        // Create rules engine using static factory method
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        
        // Create input data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("customerId", "CUST001");
        
        logger.info("Executing enrichment-group with database lookup for customer: CUST001");
        
        // Execute - this will trigger enrichment-group processing with database lookup
        RuleResult result = engine.evaluate(config, inputData);
        
        // Validate execution succeeded
        assertTrue(result.isSuccess(), 
            "Enrichment-group with database lookup should succeed. Errors: " + 
            String.join(", ", result.getFailureMessages()));
        
        assertFalse(result.hasFailures(), 
            "Should have no failures. Failures: " + result.getFailureMessages());
        
        // Validate enriched data contains database lookup results
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        
        // Verify customer name was enriched from database
        assertTrue(enrichedData.containsKey("customerName"), 
            "Enriched data should contain customerName from database lookup");
        assertEquals("Acme Corp", enrichedData.get("customerName"),
            "Customer name should match database value");
        
        // Verify credit rating was enriched from database
        assertTrue(enrichedData.containsKey("creditRating"),
            "Enriched data should contain creditRating from database lookup");
        assertEquals("AAA", enrichedData.get("creditRating"),
            "Credit rating should match database value");
        
        // Verify country was enriched from database
        assertTrue(enrichedData.containsKey("country"),
            "Enriched data should contain country from database lookup");
        assertEquals("USA", enrichedData.get("country"),
            "Country should match database value");
        
        logger.info("✓ TEST PASSED: Enrichment-group successfully executed database lookup");
        logger.info("  - customerName: " + enrichedData.get("customerName"));
        logger.info("  - creditRating: " + enrichedData.get("creditRating"));
        logger.info("  - country: " + enrichedData.get("country"));
        
        engine.shutdown();
    }

    @Test
    @DisplayName("CRITICAL: Multiple enrichment-groups with database lookups should all execute successfully")
    void testMultipleEnrichmentGroupsWithDatabaseLookups() throws Exception {
        logger.info("TEST START: Multiple enrichment-groups with database lookups");
        
        // Load configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/resources/lookups/enrichment-group-database-lookup-test.yaml"
        );
        
        // Create rules engine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        
        // Test multiple customers
        String[] customerIds = {"CUST001", "CUST002", "CUST003"};
        String[] expectedNames = {"Acme Corp", "Global Industries", "Tech Solutions"};
        String[] expectedRatings = {"AAA", "AA", "A"};
        
        for (int i = 0; i < customerIds.length; i++) {
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("customerId", customerIds[i]);
            
            logger.info("Executing for customer: " + customerIds[i]);
            
            RuleResult result = engine.evaluate(config, inputData);
            
            assertTrue(result.isSuccess(), 
                "Enrichment should succeed for " + customerIds[i]);
            
            Map<String, Object> enrichedData = result.getEnrichedData();
            assertEquals(expectedNames[i], enrichedData.get("customerName"),
                "Customer name should match for " + customerIds[i]);
            assertEquals(expectedRatings[i], enrichedData.get("creditRating"),
                "Credit rating should match for " + customerIds[i]);
            
            logger.info("  ✓ " + customerIds[i] + ": " + enrichedData.get("customerName") + 
                       " (Rating: " + enrichedData.get("creditRating") + ")");
        }
        
        logger.info("✓ TEST PASSED: All enrichment-groups with database lookups executed successfully");
        engine.shutdown();
    }

    @Test
    @DisplayName("EDGE CASE: Enrichment-group with database lookup should handle missing customer gracefully")
    void testEnrichmentGroupWithDatabaseLookupMissingCustomer() throws Exception {
        logger.info("TEST START: Enrichment-group with database lookup for non-existent customer");
        
        // Load configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/resources/lookups/enrichment-group-database-lookup-test.yaml"
        );
        
        // Create rules engine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        
        // Create input data with non-existent customer
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("customerId", "NONEXISTENT");
        
        logger.info("Executing enrichment-group with database lookup for non-existent customer");
        
        // Execute - should handle gracefully (no exception)
        RuleResult result = engine.evaluate(config, inputData);
        
        // Should succeed but with no enriched values (or default values if configured)
        assertNotNull(result, "Result should not be null");
        
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        
        // Customer name should either be null or have default value
        Object customerName = enrichedData.get("customerName");
        logger.info("  customerName for non-existent customer: " + customerName);
        
        logger.info("✓ TEST PASSED: Enrichment-group handled missing customer gracefully");
        engine.shutdown();
    }
}
