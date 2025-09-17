package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Phase 2: Conditional Mappings functionality.
 * Tests the new conditional-mappings syntax in YamlEnrichmentProcessor.
 */
public class ConditionalMappingsTest {

    private static final Logger logger = Logger.getLogger(ConditionalMappingsTest.class.getName());

    private LookupServiceRegistry serviceRegistry;
    private ExpressionEvaluatorService evaluatorService;
    private YamlEnrichmentProcessor processor;
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    void setUp() {
        serviceRegistry = new LookupServiceRegistry();
        evaluatorService = new ExpressionEvaluatorService();
        processor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
        yamlLoader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("Should create conditional-mappings structure programmatically")
    void shouldCreateConditionalMappingsStructure() {
        logger.info("=== Testing Conditional Mappings Structure Creation ===");

        try {
            // Create test enrichment with conditional mappings
            YamlEnrichment enrichment = createTestEnrichmentWithOrConditions();

            // Verify the structure was created correctly
            assertNotNull(enrichment, "Enrichment should not be null");
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
            assertEquals("'OR_MATCHED'", fieldMapping.getTransformation(), "Transformation should match");

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
            // Create test enrichment with OR conditions
            YamlEnrichment enrichment = createTestEnrichmentWithOrConditions();

            // Create test data that matches first OR condition
            Map<String, Object> data = new HashMap<>();
            data.put("testField", "VALUE1");

            // Process the enrichment
            Object result = processor.processEnrichment(enrichment, data);
            assertNotNull(result, "Result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify the conditional mapping was applied
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
            // Create test enrichment with AND conditions
            YamlEnrichment enrichment = createTestEnrichmentWithAndConditions();

            // Create test data that matches both AND conditions
            Map<String, Object> data = new HashMap<>();
            data.put("testField", "VALUE3");
            data.put("systemCode", "TEST");

            // Process the enrichment
            Object result = processor.processEnrichment(enrichment, data);
            assertNotNull(result, "Result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify the conditional mapping was applied
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
            // Create test enrichment with OR conditions
            YamlEnrichment enrichment = createTestEnrichmentWithOrConditions();

            // Create test data that doesn't match any conditions
            Map<String, Object> data = new HashMap<>();
            data.put("testField", "NO_MATCH");

            // Process the enrichment
            Object result = processor.processEnrichment(enrichment, data);
            assertNotNull(result, "Result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify no conditional mapping was applied
            assertNull(enrichedData.get("result"), "Result should be null when no conditions match");

            logger.info("✓ Failed conditions handling successful");

        } catch (Exception e) {
            logger.severe("Failed to handle failed conditions: " + e.getMessage());
            fail("Should be able to handle failed conditions: " + e.getMessage());
        }
    }

    // Helper methods to create test enrichments
    private YamlEnrichment createTestEnrichmentWithOrConditions() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("test-or-conditions");
        enrichment.setType("field-enrichment");

        // Create conditional mapping with OR conditions
        YamlEnrichment.ConditionalMapping conditionalMapping = new YamlEnrichment.ConditionalMapping();
        
        YamlEnrichment.ConditionGroup conditionGroup = new YamlEnrichment.ConditionGroup();
        conditionGroup.setOperator("OR");
        
        YamlEnrichment.ConditionRule rule1 = new YamlEnrichment.ConditionRule();
        rule1.setCondition("#testField == 'VALUE1'");
        rule1.setDescription("Test value 1");
        
        YamlEnrichment.ConditionRule rule2 = new YamlEnrichment.ConditionRule();
        rule2.setCondition("#testField == 'VALUE2'");
        rule2.setDescription("Test value 2");
        
        conditionGroup.setRules(List.of(rule1, rule2));
        conditionalMapping.setConditions(conditionGroup);
        
        // Create field mapping
        YamlEnrichment.FieldMapping fieldMapping = new YamlEnrichment.FieldMapping();
        fieldMapping.setSourceField("testField");
        fieldMapping.setTargetField("result");
        fieldMapping.setTransformation("'OR_MATCHED'");
        
        conditionalMapping.setFieldMappings(List.of(fieldMapping));
        enrichment.setConditionalMappings(List.of(conditionalMapping));
        
        return enrichment;
    }

    private YamlEnrichment createTestEnrichmentWithAndConditions() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("test-and-conditions");
        enrichment.setType("field-enrichment");

        // Create conditional mapping with AND conditions
        YamlEnrichment.ConditionalMapping conditionalMapping = new YamlEnrichment.ConditionalMapping();
        
        YamlEnrichment.ConditionGroup conditionGroup = new YamlEnrichment.ConditionGroup();
        conditionGroup.setOperator("AND");
        
        YamlEnrichment.ConditionRule rule1 = new YamlEnrichment.ConditionRule();
        rule1.setCondition("#testField == 'VALUE3'");
        rule1.setDescription("Test value 3");
        
        YamlEnrichment.ConditionRule rule2 = new YamlEnrichment.ConditionRule();
        rule2.setCondition("#systemCode == 'TEST'");
        rule2.setDescription("Test system");
        
        conditionGroup.setRules(List.of(rule1, rule2));
        conditionalMapping.setConditions(conditionGroup);
        
        // Create field mappings
        YamlEnrichment.FieldMapping fieldMapping1 = new YamlEnrichment.FieldMapping();
        fieldMapping1.setSourceField("testField");
        fieldMapping1.setTargetField("result");
        fieldMapping1.setTransformation("'AND_MATCHED'");
        
        YamlEnrichment.FieldMapping fieldMapping2 = new YamlEnrichment.FieldMapping();
        fieldMapping2.setSourceField("systemCode");
        fieldMapping2.setTargetField("system");
        fieldMapping2.setTransformation("#systemCode");
        
        conditionalMapping.setFieldMappings(List.of(fieldMapping1, fieldMapping2));
        enrichment.setConditionalMappings(List.of(conditionalMapping));
        
        return enrichment;
    }
}
