package dev.mars.apex.core.service.transformation;

import dev.mars.apex.core.config.yaml.YamlTransformation;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for deprecation warnings in YamlTransformationProcessor.
 *
 * Day 5 Requirements:
 * - Test 1: Deprecated method still works (backward compatibility)
 * - Test 2: New method works correctly
 * - Test 3: Deprecated method handles failures gracefully (cannot propagate errors)
 * - Test 4: New method propagates errors correctly
 *
 * NOTE: We cannot easily test runtime logging warnings without adding Logback test dependencies.
 * Instead, we focus on testing that deprecated methods still work (backward compatibility)
 * and that new methods properly propagate errors.
 */
@DisplayName("YamlTransformationProcessor Deprecation Tests")
class YamlTransformationProcessorDeprecationTest {

    private static final Logger logger = LoggerFactory.getLogger(YamlTransformationProcessorDeprecationTest.class);
    private YamlTransformationProcessor processor;

    @BeforeEach
    void setUp() {
        logger.info("Setting up YamlTransformationProcessor for deprecation tests");
        processor = new YamlTransformationProcessor();
    }

    @Test
    @DisplayName("Test 1: Deprecated processTransformations() should still work (backward compatibility)")
    void testDeprecatedMethodStillWorks() {
        logger.info("=== Test 1: Testing deprecated method backward compatibility ===");

        // Given: A simple transformation
        List<YamlTransformation> transformations = new ArrayList<>();
        YamlTransformation transformation = createSimpleTransformation();
        transformations.add(transformation);

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 100.0);

        // When: Call deprecated method
        Object result = processor.processTransformations(transformations, testData);

        // Then: Should still work
        assertNotNull(result, "Deprecated method should still return result");
        assertTrue(result instanceof Map, "Result should be a Map");

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals(100.0, resultMap.get("amount"), "Original data should be preserved");

        logger.info("✅ Deprecated method still works correctly");
    }

    @Test
    @DisplayName("Test 2: New method works correctly and returns RuleResult")
    void testNewMethodWorksCorrectly() {
        logger.info("=== Test 2: Testing new method returns RuleResult ===");

        // Given: A simple transformation
        List<YamlTransformation> transformations = new ArrayList<>();
        YamlTransformation transformation = createSimpleTransformation();
        transformations.add(transformation);

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 100.0);

        // When: Call new method
        RuleResult result = processor.processTransformationsWithResult(transformations, testData);

        // Then: Should return RuleResult with success
        assertNotNull(result, "New method should return RuleResult");
        assertTrue(result.isSuccess(), "Result should indicate success");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(), "Result type should be MATCH");

        logger.info("✅ New method works correctly and returns RuleResult");
    }

    @Test
    @DisplayName("Test 3: Deprecated method cannot propagate errors (returns original object)")
    void testDeprecatedMethodCannotPropagateErrors() {
        logger.info("=== INTENTIONAL ERROR TEST: Deprecated method with invalid transformation ===");
        logger.info("=== Test 3: Testing deprecated method cannot propagate errors ===");

        // Given: An invalid transformation that will fail
        List<YamlTransformation> transformations = new ArrayList<>();
        YamlTransformation transformation = new YamlTransformation();
        transformation.setId("invalid-transformation");
        transformation.setType(null); // This will cause failure
        transformations.add(transformation);

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 100.0);

        // When: Call deprecated method
        Object result = processor.processTransformations(transformations, testData);

        // Then: Should return original object (error is lost!)
        assertNotNull(result, "Deprecated method returns original object even on failure");
        assertTrue(result instanceof Map, "Result should be a Map");

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals(100.0, resultMap.get("amount"), "Original data returned - error was lost!");

        logger.info("⚠️ Deprecated method returned original object - error was lost (this is the fundamental flaw)");
    }

    @Test
    @DisplayName("Test 4: New method properly propagates errors via RuleResult")
    void testNewMethodPropagatesErrors() {
        logger.info("=== INTENTIONAL ERROR TEST: Transformation with invalid type ===");
        logger.info("=== Test 4: Testing new method propagates errors ===");

        // Given: An invalid transformation that will fail
        List<YamlTransformation> transformations = new ArrayList<>();
        YamlTransformation transformation = new YamlTransformation();
        transformation.setId("invalid-transformation");
        transformation.setType(null); // This will cause failure
        transformations.add(transformation);

        Map<String, Object> testData = new HashMap<>();

        // When: Call new method
        RuleResult result = processor.processTransformationsWithResult(transformations, testData);

        // Then: Should return RuleResult with error
        assertNotNull(result, "New method should return RuleResult");
        assertFalse(result.isSuccess(), "Result should indicate failure");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), "Result type should be ERROR");

        // Note: The current implementation may not populate failureMessages for all error types
        // The key point is that the error is propagated via RuleResult.resultType = ERROR
        logger.info("✅ New method properly propagated error via RuleResult");
        logger.info("   Result type: {}", result.getResultType());
        logger.info("   Success: {}", result.isSuccess());
    }

    private YamlTransformation createSimpleTransformation() {
        YamlTransformation transformation = new YamlTransformation();
        transformation.setId("test-transformation");
        transformation.setType("field-transformation");
        transformation.setSourceField("amount");
        transformation.setTargetField("transformedAmount");
        transformation.setExpression("#value * 2");
        return transformation;
    }
}

