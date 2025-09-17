package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Phase 3: Conditional Mapping Enrichment functionality.
 * Tests the new conditional-mapping-enrichment type with priority-based processing.
 */
public class ConditionalMappingEnrichmentTest {

    private LookupServiceRegistry serviceRegistry;
    private ExpressionEvaluatorService evaluatorService;
    private YamlEnrichmentProcessor processor;

    @BeforeEach
    void setUp() {
        this.serviceRegistry = new LookupServiceRegistry();
        this.evaluatorService = new ExpressionEvaluatorService();
        this.processor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
    }

    @Test
    @DisplayName("Should create conditional mapping enrichment structure")
    void shouldCreateConditionalMappingEnrichmentStructure() {
        // Create a conditional-mapping-enrichment
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("test-conditional-mapping");
        enrichment.setType("conditional-mapping-enrichment");
        enrichment.setTargetField("IS_NDF");

        // Create mapping rules
        List<YamlEnrichment.MappingRule> mappingRules = new ArrayList<>();
        
        // Rule 1: High priority direct mapping
        YamlEnrichment.MappingRule rule1 = new YamlEnrichment.MappingRule();
        rule1.setId("high-priority-rule");
        rule1.setPriority(1);
        
        // Create conditions for rule 1
        YamlEnrichment.ConditionGroup conditions1 = new YamlEnrichment.ConditionGroup();
        conditions1.setOperator("AND");
        List<YamlEnrichment.ConditionRule> conditionRules1 = new ArrayList<>();
        YamlEnrichment.ConditionRule condRule1 = new YamlEnrichment.ConditionRule();
        condRule1.setCondition("#SYSTEM_CODE == 'SWIFT'");
        conditionRules1.add(condRule1);
        conditions1.setRules(conditionRules1);
        rule1.setConditions(conditions1);
        
        // Create mapping for rule 1
        YamlEnrichment.MappingConfig mapping1 = new YamlEnrichment.MappingConfig();
        mapping1.setType("direct");
        mapping1.setTransformation("'HIGH_PRIORITY'");
        rule1.setMapping(mapping1);
        
        mappingRules.add(rule1);

        // Rule 2: Lower priority default rule
        YamlEnrichment.MappingRule rule2 = new YamlEnrichment.MappingRule();
        rule2.setId("default-rule");
        rule2.setPriority(999);
        // No conditions = default rule
        
        YamlEnrichment.MappingConfig mapping2 = new YamlEnrichment.MappingConfig();
        mapping2.setType("direct");
        mapping2.setTransformation("'DEFAULT_VALUE'");
        rule2.setMapping(mapping2);
        
        mappingRules.add(rule2);

        enrichment.setMappingRules(mappingRules);

        // Create execution settings
        YamlEnrichment.ExecutionSettings executionSettings = new YamlEnrichment.ExecutionSettings();
        executionSettings.setStopOnFirstMatch(true);
        executionSettings.setLogMatchedRule(true);
        enrichment.setExecutionSettings(executionSettings);

        // Verify structure
        assertNotNull(enrichment);
        assertEquals("conditional-mapping-enrichment", enrichment.getType());
        assertEquals("IS_NDF", enrichment.getTargetField());
        assertEquals(2, enrichment.getMappingRules().size());
        assertTrue(enrichment.getExecutionSettings().getStopOnFirstMatch());
        assertTrue(enrichment.getExecutionSettings().getLogMatchedRule());
    }

    @Test
    @DisplayName("Should process high priority rule first")
    void shouldProcessHighPriorityRuleFirst() {
        // Create test data that matches high priority rule
        Map<String, Object> testData = new HashMap<>();
        testData.put("SYSTEM_CODE", "SWIFT");
        testData.put("IS_NDF", "original_value");

        // Create enrichment with priority-based rules
        YamlEnrichment enrichment = createPriorityBasedEnrichment();

        // Process enrichment
        List<YamlEnrichment> enrichments = new ArrayList<>();
        enrichments.add(enrichment);
        Object result = processor.processEnrichments(enrichments, testData, null);

        // Verify high priority rule was applied
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals("HIGH_PRIORITY", resultMap.get("IS_NDF"));
    }

    @Test
    @DisplayName("Should fall back to default rule when conditions not met")
    void shouldFallBackToDefaultRule() {
        // Create test data that doesn't match high priority rule
        Map<String, Object> testData = new HashMap<>();
        testData.put("SYSTEM_CODE", "OTHER");
        testData.put("IS_NDF", "original_value");

        // Create enrichment with priority-based rules
        YamlEnrichment enrichment = createPriorityBasedEnrichment();

        // Process enrichment
        List<YamlEnrichment> enrichments = new ArrayList<>();
        enrichments.add(enrichment);
        Object result = processor.processEnrichments(enrichments, testData, null);

        // Verify default rule was applied
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals("DEFAULT_VALUE", resultMap.get("IS_NDF"));
    }

    @Test
    @DisplayName("Should stop on first match when configured")
    void shouldStopOnFirstMatch() {
        // Create test data that would match multiple rules
        Map<String, Object> testData = new HashMap<>();
        testData.put("SYSTEM_CODE", "SWIFT");
        testData.put("IS_NDF", "original_value");

        // Create enrichment with multiple matching rules
        YamlEnrichment enrichment = createMultipleMatchingRulesEnrichment();

        // Process enrichment
        List<YamlEnrichment> enrichments = new ArrayList<>();
        enrichments.add(enrichment);
        Object result = processor.processEnrichments(enrichments, testData, null);

        // Verify only first (highest priority) rule was applied
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals("FIRST_MATCH", resultMap.get("IS_NDF"));
    }

    /**
     * Helper method to create a priority-based enrichment for testing.
     */
    private YamlEnrichment createPriorityBasedEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("priority-test");
        enrichment.setType("conditional-mapping-enrichment");
        enrichment.setTargetField("IS_NDF");

        List<YamlEnrichment.MappingRule> mappingRules = new ArrayList<>();
        
        // High priority rule
        YamlEnrichment.MappingRule highPriorityRule = new YamlEnrichment.MappingRule();
        highPriorityRule.setId("high-priority");
        highPriorityRule.setPriority(1);
        
        YamlEnrichment.ConditionGroup conditions = new YamlEnrichment.ConditionGroup();
        conditions.setOperator("AND");
        List<YamlEnrichment.ConditionRule> conditionRules = new ArrayList<>();
        YamlEnrichment.ConditionRule condRule = new YamlEnrichment.ConditionRule();
        condRule.setCondition("#SYSTEM_CODE == 'SWIFT'");
        conditionRules.add(condRule);
        conditions.setRules(conditionRules);
        highPriorityRule.setConditions(conditions);
        
        YamlEnrichment.MappingConfig mapping = new YamlEnrichment.MappingConfig();
        mapping.setType("direct");
        mapping.setTransformation("'HIGH_PRIORITY'");
        highPriorityRule.setMapping(mapping);
        
        mappingRules.add(highPriorityRule);

        // Default rule (no conditions)
        YamlEnrichment.MappingRule defaultRule = new YamlEnrichment.MappingRule();
        defaultRule.setId("default");
        defaultRule.setPriority(999);
        
        YamlEnrichment.MappingConfig defaultMapping = new YamlEnrichment.MappingConfig();
        defaultMapping.setType("direct");
        defaultMapping.setTransformation("'DEFAULT_VALUE'");
        defaultRule.setMapping(defaultMapping);
        
        mappingRules.add(defaultRule);

        enrichment.setMappingRules(mappingRules);

        YamlEnrichment.ExecutionSettings settings = new YamlEnrichment.ExecutionSettings();
        settings.setStopOnFirstMatch(true);
        enrichment.setExecutionSettings(settings);

        return enrichment;
    }

    /**
     * Helper method to create enrichment with multiple matching rules.
     */
    private YamlEnrichment createMultipleMatchingRulesEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("multiple-match-test");
        enrichment.setType("conditional-mapping-enrichment");
        enrichment.setTargetField("IS_NDF");

        List<YamlEnrichment.MappingRule> mappingRules = new ArrayList<>();
        
        // First rule (priority 1)
        YamlEnrichment.MappingRule rule1 = new YamlEnrichment.MappingRule();
        rule1.setId("first-rule");
        rule1.setPriority(1);
        
        YamlEnrichment.ConditionGroup conditions1 = new YamlEnrichment.ConditionGroup();
        conditions1.setOperator("AND");
        List<YamlEnrichment.ConditionRule> conditionRules1 = new ArrayList<>();
        YamlEnrichment.ConditionRule condRule1 = new YamlEnrichment.ConditionRule();
        condRule1.setCondition("#SYSTEM_CODE == 'SWIFT'");
        conditionRules1.add(condRule1);
        conditions1.setRules(conditionRules1);
        rule1.setConditions(conditions1);
        
        YamlEnrichment.MappingConfig mapping1 = new YamlEnrichment.MappingConfig();
        mapping1.setType("direct");
        mapping1.setTransformation("'FIRST_MATCH'");
        rule1.setMapping(mapping1);
        
        mappingRules.add(rule1);

        // Second rule (priority 2) - would also match
        YamlEnrichment.MappingRule rule2 = new YamlEnrichment.MappingRule();
        rule2.setId("second-rule");
        rule2.setPriority(2);
        
        YamlEnrichment.ConditionGroup conditions2 = new YamlEnrichment.ConditionGroup();
        conditions2.setOperator("AND");
        List<YamlEnrichment.ConditionRule> conditionRules2 = new ArrayList<>();
        YamlEnrichment.ConditionRule condRule2 = new YamlEnrichment.ConditionRule();
        condRule2.setCondition("#SYSTEM_CODE != null");
        conditionRules2.add(condRule2);
        conditions2.setRules(conditionRules2);
        rule2.setConditions(conditions2);
        
        YamlEnrichment.MappingConfig mapping2 = new YamlEnrichment.MappingConfig();
        mapping2.setType("direct");
        mapping2.setTransformation("'SECOND_MATCH'");
        rule2.setMapping(mapping2);
        
        mappingRules.add(rule2);

        enrichment.setMappingRules(mappingRules);

        YamlEnrichment.ExecutionSettings settings = new YamlEnrichment.ExecutionSettings();
        settings.setStopOnFirstMatch(true);
        enrichment.setExecutionSettings(settings);

        return enrichment;
    }
}
