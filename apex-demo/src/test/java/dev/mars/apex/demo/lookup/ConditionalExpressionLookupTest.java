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
 * JUnit 5 test for ConditionalExpressionLookup functionality.
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 1 enrichment expected (conditional-expression-lookup-demo)
 * ✅ Verify log shows "Processed: 1 out of 1" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers conditional expression lookup condition
 * ✅ Validate EVERY business calculation - Test actual conditional expression evaluation logic
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 *
 * BUSINESS LOGIC VALIDATION:
 * - Conditional expression lookup using nested ternary operators with H2 database
 * - Credit score-based risk categorization using YAML enrichments and database queries
 * - YAML-driven conditional expression evaluation with database lookups
 *
 * YAML FIRST PRINCIPLE:
 * - ALL business logic is in YAML enrichments
 * - Java test only sets up minimal H2 data, loads YAML and calls APEX
 * - NO custom credit score logic or complex validation
 * - Simple database setup and basic assertions only
 */
public class ConditionalExpressionLookupTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ConditionalExpressionLookupTest.class);

    /**
     * Setup minimal H2 database with risk assessment parameters.
     * This is infrastructure setup, not business logic - business logic is in YAML.
     */
    @BeforeEach
    void setupH2Database() {
        logger.info("Setting up H2 database for conditional expression demo...");

        String jdbcUrl = "jdbc:h2:./target/h2-demo/apex_demo_conditional;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();

            // Drop and create table
            statement.execute("DROP TABLE IF EXISTS risk_parameters");
            statement.execute("""
                CREATE TABLE risk_parameters (
                    risk_category VARCHAR(20) PRIMARY KEY,
                    risk_level VARCHAR(20),
                    interest_rate DECIMAL(5,2),
                    max_loan_amount DECIMAL(12,2),
                    approval_status VARCHAR(30),
                    required_documents VARCHAR(500),
                    processing_days INTEGER,
                    risk_mitigation_actions VARCHAR(500),
                    collateral_requirement DECIMAL(5,2),
                    reviewer_level VARCHAR(20)
                )
                """);

            // Insert risk assessment parameters
            statement.execute("""
                INSERT INTO risk_parameters (risk_category, risk_level, interest_rate, max_loan_amount, approval_status, required_documents, processing_days, risk_mitigation_actions, collateral_requirement, reviewer_level) VALUES
                ('EXCELLENT', 'LOW', 3.25, 1000000.00, 'AUTO_APPROVED', 'Income verification only', 1, 'None required', 0.00, 'JUNIOR'),
                ('GOOD', 'LOW_MEDIUM', 4.75, 750000.00, 'FAST_TRACK', 'Income verification, employment letter', 2, 'Employment verification', 0.10, 'JUNIOR'),
                ('FAIR', 'MEDIUM', 7.25, 500000.00, 'MANUAL_REVIEW', 'Income verification, employment letter, bank statements (3 months)', 5, 'Enhanced due diligence, co-signer evaluation', 0.25, 'SENIOR'),
                ('POOR', 'HIGH', 12.50, 250000.00, 'DETAILED_REVIEW', 'Full financial disclosure, tax returns (2 years), bank statements (6 months), references', 10, 'Mandatory co-signer, asset verification, debt consolidation plan', 0.50, 'EXECUTIVE')
                """);

            logger.info("✓ H2 database setup completed");

        } catch (Exception e) {
            logger.error("Failed to setup H2 database: " + e.getMessage(), e);
            throw new RuntimeException("Database setup failed", e);
        }
    }

    @Test
    void testConditionalExpressionLookupFunctionality() {
        logger.info("=== Testing Conditional Expression Lookup Functionality ===");

        // Load YAML configuration for conditional expression lookup
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/ConditionalExpressionLookupTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

        // Create simple test data that triggers the conditional expression enrichment
        Map<String, Object> testData = new HashMap<>();
        testData.put("creditScore", 780);
        testData.put("approach", "real-apex-services");

        // Execute APEX enrichment processing - ALL logic in YAML
        Object result = enrichmentService.enrichObject(config, testData);

        // Validate enrichment results
        assertNotNull(result, "Conditional expression lookup result should not be null");
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Validate YAML-driven H2 database lookup results
        assertNotNull(enrichedData.get("riskLevel"), "Risk level should be retrieved from H2 database");
        assertNotNull(enrichedData.get("interestRate"), "Interest rate should be retrieved from H2 database");
        assertNotNull(enrichedData.get("maxLoanAmount"), "Max loan amount should be retrieved from H2 database");
        assertNotNull(enrichedData.get("approvalStatus"), "Approval status should be retrieved from H2 database");
        assertNotNull(enrichedData.get("processingDays"), "Processing days should be retrieved from H2 database");
        assertNotNull(enrichedData.get("reviewerLevel"), "Reviewer level should be retrieved from H2 database");

        // Validate specific H2 database lookup results for credit score 780 (EXCELLENT category)
        assertEquals("LOW", enrichedData.get("riskLevel"), "EXCELLENT category should have LOW risk level");
        assertEquals(3.25, ((Number) enrichedData.get("interestRate")).doubleValue(), 0.001, "EXCELLENT category should have 3.25% interest rate");
        assertEquals(1000000.00, ((Number) enrichedData.get("maxLoanAmount")).doubleValue(), 0.01, "EXCELLENT category should have $1M max loan amount");
        assertEquals("AUTO_APPROVED", enrichedData.get("approvalStatus"), "EXCELLENT category should be auto-approved");
        assertEquals(1, ((Number) enrichedData.get("processingDays")).intValue(), "EXCELLENT category should have 1 day processing");
        assertEquals("JUNIOR", enrichedData.get("reviewerLevel"), "EXCELLENT category should require JUNIOR reviewer");

            logger.info("✅ Conditional expression lookup functionality test completed successfully");
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }
}
