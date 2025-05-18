package com.rulesengine.core.service.transform;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.service.common.NamedService;
import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for EnrichmentService.
 */
public class EnrichmentServiceTest {
    private EnrichmentService enrichmentService;
    private MockLookupServiceRegistry registry;
    private MockEnricher mockEnricher;
    private TestRulesEngine testRulesEngine;

    @BeforeEach
    public void setUp() {
        registry = new MockLookupServiceRegistry();
        testRulesEngine = new TestRulesEngine();
        enrichmentService = new EnrichmentService(registry, testRulesEngine);
        mockEnricher = new MockEnricher("TestEnricher");
        registry.registerService(mockEnricher);
    }

    @Test
    public void testEnrichWithExistingEnricher() {
        // Configure the mock enricher
        mockEnricher.setEnrichmentResult("enrichedValue");

        // Test enrichment
        Object result = enrichmentService.enrich("TestEnricher", "testValue");

        // Verify the result
        assertEquals("enrichedValue", result);

        // Verify the enricher was called with the correct value
        assertEquals("testValue", mockEnricher.getLastEnrichedValue());
    }

    @Test
    public void testEnrichWithNonExistentEnricher() {
        // Test enrichment with a non-existent enricher
        Object originalValue = "testValue";
        Object result = enrichmentService.enrich("NonExistentEnricher", originalValue);

        // Verify the original value is returned
        assertSame(originalValue, result);

        // Verify the mock enricher wasn't called
        assertNull(mockEnricher.getLastEnrichedValue());
    }

    @Test
    public void testEnrichWithNullEnricherName() {
        // Test enrichment with a null enricher name
        Object originalValue = "testValue";
        Object result = enrichmentService.enrich(null, originalValue);

        // Verify the original value is returned
        assertSame(originalValue, result);

        // Verify the mock enricher wasn't called
        assertNull(mockEnricher.getLastEnrichedValue());
    }

    @Test
    public void testEnrichWithNullValue() {
        // Configure the mock enricher
        mockEnricher.setEnrichmentResult("enrichedValue");

        // Test enrichment with a null value
        Object result = enrichmentService.enrich("TestEnricher", null);

        // Verify the result
        assertEquals("enrichedValue", result);

        // Verify the enricher was called with null
        assertNull(mockEnricher.getLastEnrichedValue());
    }

    @Test
    public void testEnrichWithDifferentValueTypes() {
        // Test with integer value
        mockEnricher.setEnrichmentResult(456);
        Object intResult = enrichmentService.enrich("TestEnricher", 123);
        assertEquals(456, intResult);
        assertEquals(123, mockEnricher.getLastEnrichedValue());

        // Test with boolean value
        mockEnricher.setEnrichmentResult(false);
        Object boolResult = enrichmentService.enrich("TestEnricher", true);
        assertEquals(false, boolResult);
        assertEquals(true, mockEnricher.getLastEnrichedValue());

        // Test with object value
        Object originalObject = new Object();
        Object enrichedObject = new Object();
        mockEnricher.setEnrichmentResult(enrichedObject);
        Object objResult = enrichmentService.enrich("TestEnricher", originalObject);
        assertSame(enrichedObject, objResult);
        assertSame(originalObject, mockEnricher.getLastEnrichedValue());
    }

    @Test
    public void testApplyRuleWithMatchingRule() {
        // Configure the mock enricher
        mockEnricher.setEnrichmentResult("enrichedValue");

        // Configure the test rules engine to return a successful result
        testRulesEngine.setRuleResult(RuleResult.match("Test Rule", "Test Message"));

        // Create a rule
        Rule rule = new Rule("Test Rule", "#coreData != null", "Test rule");

        // Test applying the rule
        Object result = enrichmentService.applyRule(rule, "coreData", "lookupData", "TestEnricher");

        // Verify the result
        assertEquals("enrichedValue", result);

        // Verify the enricher was called with the correct value
        assertEquals("coreData", mockEnricher.getLastEnrichedValue());

        // Verify the rules engine was called with the correct parameters
        assertNotNull(testRulesEngine.getLastRules());
        assertEquals(1, testRulesEngine.getLastRules().size());
        assertEquals("Test Rule", testRulesEngine.getLastRules().get(0).getName());
        assertEquals("#coreData != null", testRulesEngine.getLastRules().get(0).getCondition());

        assertNotNull(testRulesEngine.getLastFacts());
        assertEquals("coreData", testRulesEngine.getLastFacts().get("coreData"));
        assertEquals("lookupData", testRulesEngine.getLastFacts().get("lookupData"));
    }

    @Test
    public void testApplyRuleWithNonMatchingRule() {
        // Configure the mock enricher
        mockEnricher.setEnrichmentResult("enrichedValue");

        // Configure the test rules engine to return a failed result
        testRulesEngine.setRuleResult(RuleResult.noMatch());

        // Create a rule
        Rule rule = new Rule("Test Rule", "#coreData == null", "Test rule");

        // Test applying the rule
        Object coreData = "coreData";
        Object result = enrichmentService.applyRule(rule, coreData, "lookupData", "TestEnricher");

        // Verify the original value is returned
        assertSame(coreData, result);

        // Verify the enricher wasn't called
        assertNull(mockEnricher.getLastEnrichedValue());

        // Verify the rules engine was called with the correct parameters
        assertNotNull(testRulesEngine.getLastRules());
        assertEquals(1, testRulesEngine.getLastRules().size());
        assertEquals("Test Rule", testRulesEngine.getLastRules().get(0).getName());
        assertEquals("#coreData == null", testRulesEngine.getLastRules().get(0).getCondition());

        assertNotNull(testRulesEngine.getLastFacts());
        assertEquals("coreData", testRulesEngine.getLastFacts().get("coreData"));
        assertEquals("lookupData", testRulesEngine.getLastFacts().get("lookupData"));
    }

    @Test
    public void testApplyRuleWithNonExistentEnricher() {
        // Configure the test rules engine to return a successful result
        testRulesEngine.setRuleResult(RuleResult.match("Test Rule", "Test Message"));

        // Create a rule
        Rule rule = new Rule("Test Rule", "#coreData != null", "Test rule");

        // Test applying the rule with a non-existent enricher
        Object coreData = "coreData";
        Object result = enrichmentService.applyRule(rule, coreData, "lookupData", "NonExistentEnricher");

        // Verify the original value is returned
        assertSame(coreData, result);

        // Verify the mock enricher wasn't called
        assertNull(mockEnricher.getLastEnrichedValue());
    }

    @Test
    public void testApplyRuleCondition() {
        // Configure the mock enricher
        mockEnricher.setEnrichmentResult("enrichedValue");

        // Configure the test rules engine to return a successful result
        testRulesEngine.setRuleResult(RuleResult.match("Enrichment Rule", "Test Message"));

        // Test applying a rule condition
        Object result = enrichmentService.applyRuleCondition("#coreData != null", "coreData", "lookupData", "TestEnricher");

        // Verify the result
        assertEquals("enrichedValue", result);

        // Verify the enricher was called with the correct value
        assertEquals("coreData", mockEnricher.getLastEnrichedValue());

        // Verify the rules engine was called with the correct parameters
        assertNotNull(testRulesEngine.getLastRules());
        assertEquals(1, testRulesEngine.getLastRules().size());
        assertEquals("Enrichment Rule", testRulesEngine.getLastRules().get(0).getName());
        assertEquals("#coreData != null", testRulesEngine.getLastRules().get(0).getCondition());

        assertNotNull(testRulesEngine.getLastFacts());
        assertEquals("coreData", testRulesEngine.getLastFacts().get("coreData"));
        assertEquals("lookupData", testRulesEngine.getLastFacts().get("lookupData"));
    }

    /**
     * Mock implementation of LookupServiceRegistry for testing.
     */
    private static class MockLookupServiceRegistry extends LookupServiceRegistry {
        private Enricher<?> enricher;

        public void registerService(Enricher<?> enricher) {
            this.enricher = enricher;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends NamedService> T getService(String name, Class<T> type) {
            if (enricher != null && enricher.getName().equals(name) && type.isInstance(enricher)) {
                return (T) enricher;
            }
            return null;
        }
    }

    /**
     * Mock implementation of Enricher for testing.
     */
    private static class MockEnricher implements Enricher<Object> {
        private final String name;
        private Object enrichmentResult;
        private Object lastEnrichedValue;

        public MockEnricher(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object enrich(Object value) {
            lastEnrichedValue = value;
            return enrichmentResult;
        }

        @Override
        public Class<Object> getType() {
            return Object.class;
        }

        public void setEnrichmentResult(Object enrichmentResult) {
            this.enrichmentResult = enrichmentResult;
        }

        public Object getLastEnrichedValue() {
            return lastEnrichedValue;
        }
    }

    /**
     * Test implementation of RulesEngine for testing.
     */
    private static class TestRulesEngine extends RulesEngine {
        private RuleResult ruleResult;
        private List<Rule> lastRules;
        private Map<String, Object> lastFacts;

        public TestRulesEngine() {
            super(new RulesEngineConfiguration());
            this.ruleResult = RuleResult.noMatch();
        }

        @Override
        public RuleResult executeRulesList(List<Rule> rules, Map<String, Object> facts) {
            this.lastRules = rules;
            this.lastFacts = facts;
            return ruleResult;
        }

        public void setRuleResult(RuleResult ruleResult) {
            this.ruleResult = ruleResult;
        }

        public List<Rule> getLastRules() {
            return lastRules;
        }

        public Map<String, Object> getLastFacts() {
            return lastFacts;
        }
    }
}
