package dev.mars.apex.demo.basic;

import dev.mars.apex.core.config.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class demonstrating the result-field feature for enrichments.
 * 
 * This test validates Phase 5 enhancement (Phase 2): storing boolean enrichment evaluation results
 * in the facts map for use by subsequent enrichments and rules.
 * 
 * Key Features Tested:
 * - lookup-enrichment: Store boolean indicating lookup success
 * - field-enrichment: Store boolean indicating condition match
 * - conditional-mapping-enrichment: Store boolean indicating if any mapping matched
 * - Enrichment chaining: Using enrichment results in subsequent enrichment conditions
 * 
 * Following prompts.txt guidelines:
 * - Tests actual functionality, not YAML syntax
 * - Uses real APEX enrichment operations
 * - Validates business logic outcomes
 * - Follows existing working patterns
 * - Uses middle office trade processing domain (OTC options)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Enrichment Result Field Storage Test")
public class EnrichmentResultFieldTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentResultFieldTest.class);

    @Test
    @Order(1)
    @DisplayName("Test lookup-enrichment with result-field")
    public void testLookupEnrichmentResultField() {
        logger.info("=== Testing lookup-enrichment with result-field ===");
        
        try {
            // Create YAML configuration with lookup-enrichment
            String yaml = """
                version: "1.0"
                name: "Lookup Enrichment Result Field Test"

                enrichments:
                  - id: "counterparty-lookup"
                    name: "Lookup Counterparty Details"
                    type: "lookup-enrichment"
                    result-field: "counterpartyFound"
                    lookup-config:
                      lookup-key: "#counterparty"
                      lookup-dataset:
                        type: "inline"
                        key-field: "counterpartyId"
                        data:
                          - counterpartyId: "BANK_A"
                            name: "Bank A Corp"
                            rating: "AAA"
                          - counterpartyId: "BANK_B"
                            name: "Bank B Ltd"
                            rating: "AA"
                    field-mappings:
                      - source-field: "name"
                        target-field: "counterpartyName"
                      - source-field: "rating"
                        target-field: "counterpartyRating"

                  - id: "validate-lookup"
                    name: "Validate Lookup Success"
                    type: "field-enrichment"
                    condition: "#counterpartyFound == true"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "validationStatus"
                        transformation: "'VALIDATED'"

                enrichment-groups:
                  - id: "lookup-test"
                    name: "Lookup Test Group"
                    execution-mode: "sequential"
                    operator: "AND"
                    enrichment-ids:
                      - "counterparty-lookup"
                      - "validate-lookup"
                """;

            YamlRuleConfiguration config = yamlLoader.fromYamlString(yaml);
            assertNotNull(config, "Configuration should not be null");

            // Test Case 1: Counterparty found
            Map<String, Object> data1 = new HashMap<>();
            data1.put("counterparty", "BANK_A");

            logger.info("Testing lookup with existing counterparty: {}", data1);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result1 = engine.evaluate(data1);
            assertNotNull(result1, "Rule result should not be null");

            Map<String, Object> enrichedData1 = result1.getEnrichedData();
            assertTrue((Boolean) enrichedData1.get("counterpartyFound"),
                "Lookup should have succeeded and stored counterpartyFound=true");
            assertEquals("Bank A Corp", enrichedData1.get("counterpartyName"),
                "Counterparty name should be enriched");
            assertEquals("VALIDATED", enrichedData1.get("validationStatus"),
                "Validation should have passed based on counterpartyFound");

            // Test Case 2: Counterparty not found
            Map<String, Object> data2 = new HashMap<>();
            data2.put("counterparty", "UNKNOWN_BANK");

            logger.info("Testing lookup with non-existing counterparty: {}", data2);

            RuleResult result2 = engine.evaluate(data2);
            Map<String, Object> enrichedData2 = result2.getEnrichedData();
            
            assertFalse((Boolean) enrichedData2.get("counterpartyFound"),
                "Lookup should have failed and stored counterpartyFound=false");
            assertNull(enrichedData2.get("validationStatus"),
                "Validation should not have run because counterpartyFound=false");

            logger.info("[OK] Lookup enrichment result-field test completed successfully");
            
        } catch (Exception e) {
            logger.error("Lookup enrichment result-field test failed", e);
            fail("Lookup enrichment result-field test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test field-enrichment with result-field")
    public void testFieldEnrichmentResultField() {
        logger.info("=== Testing field-enrichment with result-field ===");
        
        try {
            // Create YAML configuration with field-enrichment
            String yaml = """
                version: "1.0"
                name: "Field Enrichment Result Field Test"

                enrichments:
                  - id: "high-value-check"
                    name: "Check if High Value Trade"
                    type: "field-enrichment"
                    condition: "#notionalAmount > 10000000"
                    result-field: "isHighValue"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "tradeCategory"
                        transformation: "'HIGH_VALUE'"

                  - id: "approval-required"
                    name: "Set Approval Required"
                    type: "field-enrichment"
                    condition: "#isHighValue == true"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "requiresApproval"
                        transformation: "true"

                enrichment-groups:
                  - id: "field-test"
                    name: "Field Test Group"
                    execution-mode: "sequential"
                    operator: "AND"
                    enrichment-ids:
                      - "high-value-check"
                      - "approval-required"
                """;

            YamlRuleConfiguration config = yamlLoader.fromYamlString(yaml);
            assertNotNull(config, "Configuration should not be null");

            // Test Case 1: High value trade
            Map<String, Object> data1 = new HashMap<>();
            data1.put("notionalAmount", 15000000.0);

            logger.info("Testing field-enrichment with high value: {}", data1);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result1 = engine.evaluate(data1);
            Map<String, Object> enrichedData1 = result1.getEnrichedData();

            assertTrue((Boolean) enrichedData1.get("isHighValue"),
                "Field enrichment should have stored isHighValue=true");
            assertEquals("HIGH_VALUE", enrichedData1.get("tradeCategory"),
                "Trade category should be set");
            assertTrue((Boolean) enrichedData1.get("requiresApproval"),
                "Approval should be required based on isHighValue");

            // Test Case 2: Low value trade
            Map<String, Object> data2 = new HashMap<>();
            data2.put("notionalAmount", 5000000.0);

            logger.info("Testing field-enrichment with low value: {}", data2);

            RuleResult result2 = engine.evaluate(data2);
            Map<String, Object> enrichedData2 = result2.getEnrichedData();

            assertFalse((Boolean) enrichedData2.get("isHighValue"),
                "Field enrichment should have stored isHighValue=false");
            assertNull(enrichedData2.get("tradeCategory"),
                "Trade category should not be set for low value");
            assertNull(enrichedData2.get("requiresApproval"),
                "Approval should not be set because isHighValue=false");

            logger.info("[OK] Field enrichment result-field test completed successfully");
            
        } catch (Exception e) {
            logger.error("Field enrichment result-field test failed", e);
            fail("Field enrichment result-field test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test conditional-mapping-enrichment with result-field")
    public void testConditionalMappingEnrichmentResultField() {
        logger.info("=== Testing conditional-mapping-enrichment with result-field ===");
        
        try {
            // Create YAML configuration with conditional-mapping-enrichment
            String yaml = """
                version: "1.0"
                name: "Conditional Mapping Enrichment Result Field Test"

                enrichments:
                  - id: "risk-classification"
                    name: "Classify Risk Level"
                    type: "conditional-mapping-enrichment"
                    target-field: "riskLevel"
                    result-field: "riskClassified"
                    mapping-rules:
                      - id: "high-risk"
                        priority: 1
                        conditions:
                          operator: "AND"
                          rules:
                            - condition: "#notionalAmount > 10000000"
                        mapping:
                          type: "direct"
                          transformation: "'HIGH'"
                      - id: "medium-risk"
                        priority: 2
                        conditions:
                          operator: "AND"
                          rules:
                            - condition: "#notionalAmount > 5000000"
                        mapping:
                          type: "direct"
                          transformation: "'MEDIUM'"

                  - id: "approval-workflow"
                    name: "Set Approval Workflow"
                    type: "field-enrichment"
                    condition: "#riskClassified == true"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "approvalWorkflow"
                        transformation: "'RISK_BASED_APPROVAL'"

                enrichment-groups:
                  - id: "mapping-test"
                    name: "Mapping Test Group"
                    execution-mode: "sequential"
                    operator: "AND"
                    enrichment-ids:
                      - "risk-classification"
                      - "approval-workflow"
                """;

            YamlRuleConfiguration config = yamlLoader.fromYamlString(yaml);
            assertNotNull(config, "Configuration should not be null");

            // Test Case 1: High risk (rule matches)
            Map<String, Object> data1 = new HashMap<>();
            data1.put("notionalAmount", 15000000.0);

            logger.info("Testing conditional-mapping with high risk: {}", data1);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result1 = engine.evaluate(data1);
            Map<String, Object> enrichedData1 = result1.getEnrichedData();

            assertTrue((Boolean) enrichedData1.get("riskClassified"),
                "Conditional mapping should have stored riskClassified=true");
            assertEquals("HIGH", enrichedData1.get("riskLevel"),
                "Risk level should be HIGH");
            assertEquals("RISK_BASED_APPROVAL", enrichedData1.get("approvalWorkflow"),
                "Approval workflow should be set based on riskClassified");

            // Test Case 2: Low risk (no rule matches)
            Map<String, Object> data2 = new HashMap<>();
            data2.put("notionalAmount", 1000000.0);

            logger.info("Testing conditional-mapping with low risk: {}", data2);

            RuleResult result2 = engine.evaluate(data2);
            Map<String, Object> enrichedData2 = result2.getEnrichedData();

            assertFalse((Boolean) enrichedData2.get("riskClassified"),
                "Conditional mapping should have stored riskClassified=false");
            assertNull(enrichedData2.get("riskLevel"),
                "Risk level should not be set");
            assertNull(enrichedData2.get("approvalWorkflow"),
                "Approval workflow should not be set because riskClassified=false");

            logger.info("[OK] Conditional mapping enrichment result-field test completed successfully");
            
        } catch (Exception e) {
            logger.error("Conditional mapping enrichment result-field test failed", e);
            fail("Conditional mapping enrichment result-field test failed: " + e.getMessage());
        }
    }
}

