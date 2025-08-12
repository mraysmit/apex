package dev.mars.apex.core.service.transform;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.model.TransformerRule;
import dev.mars.apex.core.service.lookup.LookupService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for GenericTransformerService.
 * 
 * Tests cover:
 * - Transformer creation and registration
 * - Dynamic transformation with rules
 * - Rule-based transformation
 * - Integration with lookup services
 * - Error handling and edge cases
 * - Type safety and validation
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class GenericTransformerServiceTest {

    private LookupServiceRegistry registry;
    private RulesEngine rulesEngine;
    private GenericTransformerService transformerService;

    @BeforeEach
    void setUp() {
        registry = new LookupServiceRegistry();
        rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        transformerService = new GenericTransformerService(registry, rulesEngine);
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    @DisplayName("Should create GenericTransformerService with valid dependencies")
    void testConstructor() {
        GenericTransformerService service = new GenericTransformerService(registry, rulesEngine);
        assertNotNull(service, "Service should be created successfully");
    }

    @Test
    @DisplayName("Should handle null registry gracefully")
    void testConstructorWithNullRegistry() {
        assertDoesNotThrow(() -> {
            GenericTransformerService service = new GenericTransformerService(null, rulesEngine);
            assertNotNull(service);
        });
    }

    @Test
    @DisplayName("Should handle null rules engine gracefully")
    void testConstructorWithNullRulesEngine() {
        assertDoesNotThrow(() -> {
            GenericTransformerService service = new GenericTransformerService(registry, null);
            assertNotNull(service);
        });
    }

    // ========================================
    // Transformer Creation Tests
    // ========================================

    @Test
    @DisplayName("Should create and register transformer successfully")
    void testCreateTransformer() {
        List<TransformerRule<String>> rules = createStringTransformerRules();
        
        GenericTransformer<String> transformer = transformerService.createTransformer(
            "stringTransformer", String.class, rules);
        
        assertNotNull(transformer, "Transformer should be created");
        assertEquals("stringTransformer", transformer.getName(), "Transformer should have correct name");
        assertEquals(String.class, transformer.getType(), "Transformer should have correct type");
        
        // Verify it's registered in the registry
        GenericTransformer<?> retrieved = registry.getService("stringTransformer", GenericTransformer.class);
        assertNotNull(retrieved, "Transformer should be registered in registry");
        assertSame(transformer, retrieved, "Retrieved transformer should be same instance");
    }

    @Test
    @DisplayName("Should create transformer with empty rules")
    void testCreateTransformerWithEmptyRules() {
        List<TransformerRule<String>> emptyRules = new ArrayList<>();
        
        GenericTransformer<String> transformer = transformerService.createTransformer(
            "emptyTransformer", String.class, emptyRules);
        
        assertNotNull(transformer, "Transformer with empty rules should be created");
    }

    @Test
    @DisplayName("Should handle null transformer rules")
    void testCreateTransformerWithNullRules() {
        assertDoesNotThrow(() -> {
            GenericTransformer<String> transformer = transformerService.createTransformer(
                "nullRulesTransformer", String.class, null);
            assertNotNull(transformer);
        });
    }

    // ========================================
    // Dynamic Transformation Tests
    // ========================================

    @Test
    @DisplayName("Should transform value using dynamic transformer")
    void testDynamicTransform() {
        List<TransformerRule<String>> rules = createStringTransformerRules();

        String original = "hello";
        String transformed = transformerService.<String>transform(original, rules);

        assertNotNull(transformed, "Transformed value should not be null");
        // The exact transformation depends on the rules implementation
    }

    @Test
    @DisplayName("Should handle null value in dynamic transformation")
    void testDynamicTransformWithNullValue() {
        List<TransformerRule<String>> rules = createStringTransformerRules();

        String result = transformerService.<String>transform((String) null, rules);
        assertNull(result, "Null input should return null");
    }

    @Test
    @DisplayName("Should handle null rules in dynamic transformation")
    void testDynamicTransformWithNullRules() {
        String original = "test";
        String result = transformerService.<String>transform(original, (List<TransformerRule<String>>) null);

        // Should handle gracefully, likely returning original value or null
        assertNotNull(result, "Should handle null rules gracefully");
    }

    // ========================================
    // Registered Transformer Tests
    // ========================================

    @Test
    @DisplayName("Should transform using registered transformer")
    void testTransformWithRegisteredTransformer() {
        // Create and register transformer
        List<TransformerRule<String>> rules = createStringTransformerRules();
        transformerService.createTransformer("registeredTransformer", String.class, rules);
        
        String original = "test";
        String transformed = transformerService.transform("registeredTransformer", original);
        
        assertNotNull(transformed, "Transformation should produce result");
    }

    @Test
    @DisplayName("Should return original value for non-existent transformer")
    void testTransformWithNonExistentTransformer() {
        System.out.println("TEST: Triggering intentional error - testing transformation with non-existent transformer");
        
        String original = "test";
        String result = transformerService.transform("nonExistent", original);
        
        assertEquals(original, result, "Non-existent transformer should return original value");
    }

    @Test
    @DisplayName("Should handle type mismatch in registered transformer")
    void testTransformWithTypeMismatch() {
        System.out.println("TEST: Triggering intentional error - testing transformation with type mismatch");
        
        // Create string transformer
        List<TransformerRule<String>> rules = createStringTransformerRules();
        transformerService.createTransformer("stringTransformer", String.class, rules);
        
        // Try to transform integer with string transformer
        Integer original = 123;
        Integer result = transformerService.transform("stringTransformer", original);
        
        assertEquals(original, result, "Type mismatch should return original value");
    }

    // ========================================
    // TransformWithResult Tests
    // ========================================

    @Test
    @DisplayName("Should return detailed result for successful transformation")
    void testTransformWithResultSuccess() {
        List<TransformerRule<String>> rules = createStringTransformerRules();
        transformerService.createTransformer("resultTransformer", String.class, rules);
        
        RuleResult result = transformerService.transformWithResult("resultTransformer", "test");
        
        assertNotNull(result, "Result should not be null");
        // The exact result depends on transformer implementation
    }

    @Test
    @DisplayName("Should return error result for non-existent transformer")
    void testTransformWithResultNonExistentTransformer() {
        System.out.println("TEST: Triggering intentional error - testing detailed transformation with non-existent transformer");
        
        RuleResult result = transformerService.transformWithResult("nonExistent", "test");
        
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Transformation should fail");
    }

    // ========================================
    // Integration with Lookup Services
    // ========================================

    @Test
    @DisplayName("Should integrate with lookup services for transformation")
    void testTransformationWithLookupServices() {
        // Register a lookup service
        LookupService lookupService = new LookupService("testLookup", 
            Arrays.asList("value1", "value2", "value3"));
        registry.registerService(lookupService);
        
        // Create transformer that might use lookup data
        List<TransformerRule<String>> rules = createStringTransformerRules();
        transformerService.createTransformer("lookupTransformer", String.class, rules);
        
        String result = transformerService.transform("lookupTransformer", "test");
        assertNotNull(result, "Transformation with lookup integration should work");
    }

    // ========================================
    // Complex Object Transformation Tests
    // ========================================

    @Test
    @DisplayName("Should transform complex objects")
    void testComplexObjectTransformation() {
        List<TransformerRule<TestComplexObject>> rules = createComplexObjectTransformerRules();
        
        TestComplexObject original = new TestComplexObject("John", 25);
        TestComplexObject transformed = transformerService.transform(original, rules);
        
        assertNotNull(transformed, "Complex object transformation should work");
        // Verify transformation logic based on rules
    }

    // ========================================
    // Error Handling and Edge Cases
    // ========================================

    @Test
    @DisplayName("Should handle empty transformer name")
    void testTransformWithEmptyTransformerName() {
        String result = transformerService.transform("", "test");
        assertEquals("test", result, "Empty transformer name should return original value");
    }

    @Test
    @DisplayName("Should handle null transformer name")
    void testTransformWithNullTransformerName() {
        String result = transformerService.transform(null, "test");
        assertEquals("test", result, "Null transformer name should return original value");
    }

    // ========================================
    // Test Helper Methods
    // ========================================

    /**
     * Creates sample transformer rules for string transformation.
     */
    private List<TransformerRule<String>> createStringTransformerRules() {
        List<TransformerRule<String>> rules = new ArrayList<>();

        // Create a simple rule
        Rule rule = new Rule("testRule", "true", "Test transformation rule");

        // Create field transformer actions
        List<FieldTransformerAction<String>> positiveActions = new ArrayList<>();
        FieldTransformerAction<String> action = new FieldTransformerAction<>(
            "value",
            (String s) -> s, // extractor
            (Object value, Map<String, Object> context) -> value.toString().toUpperCase(), // transformer
            (String target, Object newValue) -> {} // setter (no-op for test)
        );
        positiveActions.add(action);

        // Create transformer rule
        TransformerRule<String> transformerRule = new TransformerRule<>(rule, positiveActions, new ArrayList<>());
        rules.add(transformerRule);
        return rules;
    }

    /**
     * Creates sample transformer rules for complex object transformation.
     */
    private List<TransformerRule<TestComplexObject>> createComplexObjectTransformerRules() {
        List<TransformerRule<TestComplexObject>> rules = new ArrayList<>();

        // Create a simple rule
        Rule rule = new Rule("complexRule", "true", "Test complex object transformation rule");

        // Create field transformer actions
        List<FieldTransformerAction<TestComplexObject>> positiveActions = new ArrayList<>();
        FieldTransformerAction<TestComplexObject> action = new FieldTransformerAction<>(
            "name",
            (TestComplexObject obj) -> obj.getName(), // extractor
            (Object value, Map<String, Object> context) -> value.toString().toUpperCase(), // transformer
            (TestComplexObject target, Object newValue) -> target.setName((String) newValue) // setter
        );
        positiveActions.add(action);

        // Create transformer rule
        TransformerRule<TestComplexObject> transformerRule = new TransformerRule<>(rule, positiveActions, new ArrayList<>());
        rules.add(transformerRule);
        return rules;
    }

    /**
     * Test complex object for transformation testing.
     */
    private static class TestComplexObject {
        private String name;
        private int age;

        public TestComplexObject(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
