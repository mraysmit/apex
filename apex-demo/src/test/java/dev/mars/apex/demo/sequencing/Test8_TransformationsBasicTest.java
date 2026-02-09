package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.engine.core.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test 8: Transformations Basic Test
 *
 * <p>Tests that transformations execute in exact document order.
 *
 * <p>Expected execution order: transform-1, transform-2, transform-3
 *
 * <p>NOTE: Disabled until transformations section is fully implemented in SequentialYamlProcessor
 */
@DisplayName("Test 8: Transformations Basic Test")
public class Test8_TransformationsBasicTest extends DemoTestBase {

    @Test
    @DisplayName("Transformations execute in document order")
    void testTransformationsDocumentOrder() throws Exception {
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/Test8_TransformationsBasicTest.yaml";
        
        ExecutionTracker.clear();
        
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        Map<String, Object> data = new HashMap<>();
        data.put("value", "test");
        
        RuleResult result = engine.evaluate(data);
        
        assertTrue(result.isSuccess(), "Execution should succeed");
        
        List<String> executionOrder = ExecutionTracker.getExecutionLog();
        
        // Verify transformations executed in document order
        assertEquals(List.of("transform-1", "transform-2", "transform-3"), executionOrder,
            "Transformations must execute in exact document order");
        
        System.out.println("TEST 8 PASSED: Transformations execute in document order");
        System.out.println("Execution order: " + executionOrder);
    }
}

