package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Phase 2: Conditional Mappings functionality.
 * Tests the new conditional-mappings syntax in YamlEnrichmentProcessor.
 */
public class ConditionalMappingsTest {

    private static final Logger logger = Logger.getLogger(ConditionalMappingsTest.class.getName());

    @Test
    @DisplayName("Should create conditional-mappings structure from YAML")
    void shouldCreateConditionalMappingsStructure() {
        logger.info("=== Testing Conditional Mappings Structure Creation ===");

        try {
            // Create YAML config with conditional mappings
            String yamlConfig = """
                metadata:
                  id: "test-or-conditions"
                  name: "Test OR Conditions"
                  version: "1.0.0"
                  description: "Test OR conditions in conditional mappings"
                  type: "rule-config"

                enrichments:
                  - id: "test-or-conditions"
                    type: "field-enrichment"
                    conditional-mappings:
                      - conditions:
                          operator: "OR"
                          rules:
                            - condition: "#testField == 'VALUE1'"
                              description: "Test value 1"
                            - condition: "#testField == 'VALUE2'"
                              description: "Test value 2"
                        field-mappings:
                          - source-field: "testField"
                            target-field: "result"
                            expression: "'OR_MATCHED'"
                """;

            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

            // Verify the structure was created correctly
            assertNotNull(config, "Config should not be null");
            assertNotNull(config.getEnrichments(), "Enrichments should not be null");
            assertEquals(1, config.getEnrichments().size(), "Should have one enrichment");

            YamlEnrichment enrichment = config.getEnrichments().get(0);
            assertEquals("test-or-conditions", enrichment.getId(), "Enrichment ID should match");
            assertEquals("field-enrichment", enrichment.getType(), "Should be field-enrichment type");

            // Verify conditional-mappings are present
            assertNotNull(enrichment.getConditionalMappings(), "Conditional mappings should not be null");
            assertEquals(1, enrichment.getConditionalMappings().size(), "Should have one conditional mapping");

            YamlEnrichment.ConditionalMapping conditionalMapping = enrichment.getConditionalMappings().get(0);
            assertNotNull(conditionalMapping.getConditions(), "Conditions should not be null");
            assertEquals("OR", conditionalMapping.getConditions().getOperator(), "Operator should be OR");
            assertEquals(2, conditionalMapping.getConditions().getRules().size(), "Should have 2 condition rules");

            // Verify field mappings
            assertNotNull(conditionalMapping.getFieldMappings(), "Field mappings should not be null");
            assertEquals(1, conditionalMapping.getFieldMappings().size(), "Should have one field mapping");

            YamlEnrichment.FieldMapping fieldMapping = conditionalMapping.getFieldMappings().get(0);
            assertEquals("testField", fieldMapping.getSourceField(), "Source field should match");
            assertEquals("result", fieldMapping.getTargetField(), "Target field should match");
            assertEquals("'OR_MATCHED'", fieldMapping.getExpression(), "Expression should match");

            logger.info("✓ Conditional mappings structure creation successful");

        } catch (Exception e) {
            logger.severe("Failed to create conditional mappings structure: " + e.getMessage());
            fail("Should be able to create conditional mappings structure: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process OR conditions in conditional mappings")
    void shouldProcessOrConditions() {
        logger.info("=== Testing OR Conditions Processing ===");

        try {
            // Create test data that matches first OR condition
            Map<String, Object> data = new HashMap<>();
            data.put("testField", "VALUE1");

            // Create YAML config with OR conditions
            String yamlConfig = """
                metadata:
                  id: "test-or-conditions"
                  name: "Test OR Conditions"
                  version: "1.0.0"
                  description: "Test OR conditions in conditional mappings"
                  type: "rule-config"

                enrichments:
                  - id: "test-or-conditions"
                    type: "field-enrichment"
                    conditional-mappings:
                      - conditions:
                          operator: "OR"
                          rules:
                            - condition: "#testField == 'VALUE1'"
                              description: "Test value 1"
                            - condition: "#testField == 'VALUE2'"
                              description: "Test value 2"
                        field-mappings:
                          - source-field: "testField"
                            target-field: "result"
                            expression: "'OR_MATCHED'"
                """;

            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

            // Process enrichment using RulesEngine
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(data);
            Map<String, Object> enrichedData = result.getEnrichedData();

            // Verify the conditional mapping was applied
            assertNotNull(enrichedData, "Result should not be null");
            assertEquals("OR_MATCHED", enrichedData.get("result"), "Result should be 'OR_MATCHED'");

            logger.info("✓ OR conditions processing successful");

        } catch (Exception e) {
            logger.severe("Failed to process OR conditions: " + e.getMessage());
            fail("Should be able to process OR conditions: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process AND conditions in conditional mappings")
    void shouldProcessAndConditions() {
        logger.info("=== Testing AND Conditions Processing ===");

        try {
            // Create test data that matches both AND conditions
            Map<String, Object> data = new HashMap<>();
            data.put("testField", "VALUE3");
            data.put("systemCode", "TEST");

            // Create YAML config with AND conditions
            String yamlConfig = """
                metadata:
                  id: "test-and-conditions"
                  name: "Test AND Conditions"
                  version: "1.0.0"
                  description: "Test AND conditions in conditional mappings"
                  type: "rule-config"

                enrichments:
                  - id: "test-and-conditions"
                    type: "field-enrichment"
                    conditional-mappings:
                      - conditions:
                          operator: "AND"
                          rules:
                            - condition: "#testField == 'VALUE3'"
                              description: "Test value 3"
                            - condition: "#systemCode == 'TEST'"
                              description: "Test system"
                        field-mappings:
                          - source-field: "testField"
                            target-field: "result"
                            expression: "'AND_MATCHED'"
                          - source-field: "systemCode"
                            target-field: "system"
                            expression: "#systemCode"
                """;

            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

            // Process enrichment using RulesEngine
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(data);
            Map<String, Object> enrichedData = result.getEnrichedData();

            // Verify the conditional mapping was applied
            assertNotNull(enrichedData, "Result should not be null");
            assertEquals("AND_MATCHED", enrichedData.get("result"), "Result should be 'AND_MATCHED'");
            assertEquals("TEST", enrichedData.get("system"), "System should be 'TEST'");

            logger.info("✓ AND conditions processing successful");

        } catch (Exception e) {
            logger.severe("Failed to process AND conditions: " + e.getMessage());
            fail("Should be able to process AND conditions: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle failed conditions gracefully")
    void shouldHandleFailedConditions() {
        logger.info("=== Testing Failed Conditions Handling ===");

        try {
            // Create test data that doesn't match any conditions
            Map<String, Object> data = new HashMap<>();
            data.put("testField", "NO_MATCH");

            // Create YAML config with OR conditions
            String yamlConfig = """
                metadata:
                  id: "test-or-conditions"
                  name: "Test OR Conditions"
                  version: "1.0.0"
                  description: "Test OR conditions in conditional mappings"
                  type: "rule-config"

                enrichments:
                  - id: "test-or-conditions"
                    type: "field-enrichment"
                    conditional-mappings:
                      - conditions:
                          operator: "OR"
                          rules:
                            - condition: "#testField == 'VALUE1'"
                              description: "Test value 1"
                            - condition: "#testField == 'VALUE2'"
                              description: "Test value 2"
                        field-mappings:
                          - source-field: "testField"
                            target-field: "result"
                            expression: "'OR_MATCHED'"
                """;

            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

            // Process enrichment using RulesEngine
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(data);
            Map<String, Object> enrichedData = result.getEnrichedData();

            // Verify no conditional mapping was applied
            assertNotNull(enrichedData, "Result should not be null");
            assertNull(enrichedData.get("result"), "Result should be null when no conditions match");

            logger.info("✓ Failed conditions handling successful");

        } catch (Exception e) {
            logger.severe("Failed to handle failed conditions: " + e.getMessage());
            fail("Should be able to handle failed conditions: " + e.getMessage());
        }
    }

}
