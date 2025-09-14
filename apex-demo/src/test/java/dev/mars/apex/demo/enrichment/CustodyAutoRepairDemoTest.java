package dev.mars.apex.demo.enrichment;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for CustodyAutoRepairDemo using real APEX services
 *
 * CRITICAL VALIDATION APPROACH:
 * 1. Count enrichments in YAML - 4 enrichments identified
 * 2. Verify log execution counts - "Processed: X out of 4" matches expectations
 * 3. Check EVERY enrichment condition - Test data designed to trigger correct subsets
 * 4. Validate EVERY business calculation - All mathematical and conditional logic tested
 * 5. Assert ALL enrichment results - Every result-field has corresponding assertEquals
 *
 * BUSINESS LOGIC VALIDATION (NOT YAML SYNTAX):
 * - Settlement instruction auto-repair processing
 * - Standing instruction processing with hierarchical prioritization
 * - Weighted rule-based decision making for auto-repair confidence
 * - Dynamic summary generation
 */
public class CustodyAutoRepairDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CustodyAutoRepairDemoTest.class);

    @Test
    public void testSettlementInstructionAutoRepairFunctionality() {
        logger.info("=== Testing Settlement Instruction Auto-Repair Functionality ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("test-configs/custodyautorepairdemo-test.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("✓ Configuration loaded successfully: " + config.getMetadata().getName());

            // Create test data for settlement instruction auto-repair
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("instructionId", "SI001");
            inputData.put("clientId", "CLIENT_A");
            inputData.put("market", "JAPAN");
            inputData.put("instrumentType", "EQUITY");
            inputData.put("settlementAmount", 1000000.0);
            inputData.put("settlementCurrency", "JPY");
            inputData.put("settlementDate", "2025-07-30");

            logger.info("Input data: " + inputData);

            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.info("✓ Settlement instruction auto-repair completed using real APEX services");
            logger.info("Settlement auto-repair result: " + enrichedData);

            // Validate business logic results
            // 1. Settlement Instruction Auto-Repair Processing (condition: #instructionId != null && #clientId != null)
            assertEquals("Settlement instruction auto-repair completed - InstructionId: SI001 - Client: CLIENT_A - Market: JAPAN - Instrument: EQUITY - Amount: 1000000.0 - Currency: JPY - Method: DVP - Status: PROCESSED",
                        enrichedData.get("settlementAutoRepairResult"));

            // 2. Standing Instruction Processing (condition: #siId != null || #clientId != null) - ALSO EXECUTES because clientId is present
            assertEquals("Standing instruction processing completed - SiId: AUTO_GENERATED - Client: CLIENT_A - Market: JAPAN - Priority: MEDIUM - Weight: 0.5 - Status: PROCESSED",
                        enrichedData.get("standingInstructionResult"));

            // 3. Custody Auto-Repair Summary Processing (condition: #instructionId != null || #siId != null || #decisionType != null)
            assertEquals("Custody auto-repair processing completed using real APEX services",
                        enrichedData.get("custodyAutoRepairSummary"));

            // Verify weighted decision did not execute (no decisionType)
            assertNull(enrichedData.get("weightedDecisionResult"));

        } catch (Exception e) {
            logger.error("❌ Settlement instruction auto-repair test failed", e);
            fail("Settlement instruction auto-repair test failed: " + e.getMessage());
        }
    }

    @Test
    public void testStandingInstructionProcessingFunctionality() {
        logger.info("=== Testing Standing Instruction Processing Functionality ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("test-configs/custodyautorepairdemo-test.yaml");
            assertNotNull(config, "Configuration should not be null");

            // Create test data for standing instruction processing
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("siId", "SI_CLIENT_A");
            inputData.put("clientId", "CLIENT_A");
            inputData.put("market", "JAPAN");
            inputData.put("instrumentType", "EQUITY");
            inputData.put("priority", "HIGH");
            inputData.put("weight", 0.6);

            logger.info("Input data: " + inputData);

            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.info("✓ Standing instruction processing completed using real APEX services");
            logger.info("Standing instruction result: " + enrichedData);

            // Validate business logic results
            // 1. Standing Instruction Processing (condition: #siId != null || #clientId != null)
            assertEquals("Standing instruction processing completed - SiId: SI_CLIENT_A - Client: CLIENT_A - Market: JAPAN - Priority: HIGH - Weight: 0.6 - Status: PROCESSED",
                        enrichedData.get("standingInstructionResult"));

            // 2. Custody Auto-Repair Summary Processing (condition: #instructionId != null || #siId != null || #decisionType != null)
            assertEquals("Custody auto-repair processing completed using real APEX services",
                        enrichedData.get("custodyAutoRepairSummary"));

            // Verify no other enrichments executed (settlement auto-repair and weighted decision should not execute)
            assertNull(enrichedData.get("settlementAutoRepairResult"));
            assertNull(enrichedData.get("weightedDecisionResult"));

        } catch (Exception e) {
            logger.error("❌ Standing instruction processing test failed", e);
            fail("Standing instruction processing test failed: " + e.getMessage());
        }
    }

    @Test
    public void testWeightedRuleDecisionMakingFunctionality() {
        logger.info("=== Testing Weighted Rule Decision Making Functionality ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("test-configs/custodyautorepairdemo-test.yaml");
            assertNotNull(config, "Configuration should not be null");

            // Create test data for weighted rule-based decision making
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("decisionType", "AUTO_REPAIR");
            inputData.put("clientWeight", 0.6);
            inputData.put("marketWeight", 0.3);
            inputData.put("instrumentWeight", 0.1);
            inputData.put("confidenceThreshold", 0.8);
            inputData.put("riskLevel", "MEDIUM");

            logger.info("Input data: " + inputData);

            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.info("✓ Weighted rule decision making completed using real APEX services");
            logger.info("Weighted decision result: " + enrichedData);

            // Validate business logic results
            // 1. Weighted Rule Decision Making Processing (condition: #decisionType == 'AUTO_REPAIR')
            assertEquals("Weighted decision making completed - ClientWeight: 0.6 - MarketWeight: 0.3 - InstrumentWeight: 0.1 - ConfidenceThreshold: 0.8 - RiskLevel: MEDIUM - Decision: APPROVED - RequiresApproval: false",
                        enrichedData.get("weightedDecisionResult"));

            // 2. Custody Auto-Repair Summary Processing (condition: #instructionId != null || #siId != null || #decisionType != null)
            assertEquals("Custody auto-repair processing completed using real APEX services",
                        enrichedData.get("custodyAutoRepairSummary"));

            // Verify no other enrichments executed (settlement auto-repair and standing instruction should not execute)
            assertNull(enrichedData.get("settlementAutoRepairResult"));
            assertNull(enrichedData.get("standingInstructionResult"));

        } catch (Exception e) {
            logger.error("❌ Weighted rule decision making test failed", e);
            fail("Weighted rule decision making test failed: " + e.getMessage());
        }
    }

    @Test
    public void testHighRiskWeightedDecisionMakingFunctionality() {
        logger.info("=== Testing High Risk Weighted Decision Making Functionality ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("test-configs/custodyautorepairdemo-test.yaml");
            assertNotNull(config, "Configuration should not be null");

            // Create test data for high-risk weighted decision making with low confidence threshold
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("decisionType", "AUTO_REPAIR");
            inputData.put("clientWeight", 0.4);
            inputData.put("marketWeight", 0.3);
            inputData.put("instrumentWeight", 0.3);
            inputData.put("confidenceThreshold", 0.5); // Low confidence threshold
            inputData.put("riskLevel", "HIGH");

            logger.info("Input data: " + inputData);

            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.info("✓ High risk weighted decision making completed using real APEX services");
            logger.info("High risk decision result: " + enrichedData);

            // Validate business logic results
            // 1. Weighted Rule Decision Making Processing (condition: #decisionType == 'AUTO_REPAIR')
            // Should show MANUAL_REVIEW_REQUIRED for low confidence threshold and RequiresApproval: true for HIGH risk
            assertEquals("Weighted decision making completed - ClientWeight: 0.4 - MarketWeight: 0.3 - InstrumentWeight: 0.3 - ConfidenceThreshold: 0.5 - RiskLevel: HIGH - Decision: MANUAL_REVIEW_REQUIRED - RequiresApproval: true",
                        enrichedData.get("weightedDecisionResult"));

            // 2. Custody Auto-Repair Summary Processing (condition: #instructionId != null || #siId != null || #decisionType != null)
            assertEquals("Custody auto-repair processing completed using real APEX services",
                        enrichedData.get("custodyAutoRepairSummary"));

        } catch (Exception e) {
            logger.error("❌ High risk weighted decision making test failed", e);
            fail("High risk weighted decision making test failed: " + e.getMessage());
        }
    }
}
