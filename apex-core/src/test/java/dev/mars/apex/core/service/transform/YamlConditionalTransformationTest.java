package dev.mars.apex.core.service.transform;

import dev.mars.apex.core.config.model.YamlTransformation;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Conditional Transformations
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class YamlConditionalTransformationTest {

    private static final Logger logger = LoggerFactory.getLogger(YamlConditionalTransformationTest.class);

    private YamlTransformationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new YamlTransformationProcessor();
    }

    @Test
    @DisplayName("Test Conditional Transformation - Condition True")
    void testConditionalTransformationTrue() {
        YamlTransformation transformation = new YamlTransformation();
        transformation.setId("cond-trans-1");
        transformation.setType("conditional-transformation");
        
        YamlTransformation.TransformationRule rule = new YamlTransformation.TransformationRule();
        rule.setCondition("#amount > 100");
        
        YamlTransformation.TransformationAction action = new YamlTransformation.TransformationAction();
        action.setType("set-field");
        action.setField("status");
        action.setValue("HIGH_VALUE");
        
        rule.setActions(Collections.singletonList(action));
        transformation.setTransformationRules(Collections.singletonList(rule));

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("amount", 150.0);

        RuleResult result = processor.processTransformationsWithResult(Collections.singletonList(transformation), inputData);

        assertTrue(result.isSuccess());
        assertEquals("HIGH_VALUE", result.getEnrichedData().get("status"));
    }

    @Test
    @DisplayName("Test Conditional Transformation - Condition False (Else Actions)")
    void testConditionalTransformationFalse() {
        YamlTransformation transformation = new YamlTransformation();
        transformation.setId("cond-trans-2");
        transformation.setType("conditional-transformation");
        
        YamlTransformation.TransformationRule rule = new YamlTransformation.TransformationRule();
        rule.setCondition("#amount > 100");
        
        YamlTransformation.TransformationAction action = new YamlTransformation.TransformationAction();
        action.setType("set-field");
        action.setField("status");
        action.setValue("HIGH_VALUE");
        
        YamlTransformation.TransformationAction elseAction = new YamlTransformation.TransformationAction();
        elseAction.setType("set-field");
        elseAction.setField("status");
        elseAction.setValue("STANDARD_VALUE");
        
        rule.setActions(Collections.singletonList(action));
        rule.setElseActions(Collections.singletonList(elseAction));
        transformation.setTransformationRules(Collections.singletonList(rule));

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("amount", 50.0);

        RuleResult result = processor.processTransformationsWithResult(Collections.singletonList(transformation), inputData);

        assertTrue(result.isSuccess());
        assertEquals("STANDARD_VALUE", result.getEnrichedData().get("status"));
    }

    @Test
    @DisplayName("Test Multiple Actions")
    void testMultipleActions() {
        YamlTransformation transformation = new YamlTransformation();
        transformation.setId("multi-action-trans");
        transformation.setType("conditional-transformation");
        
        YamlTransformation.TransformationRule rule = new YamlTransformation.TransformationRule();
        rule.setCondition("true");
        
        List<YamlTransformation.TransformationAction> actions = new ArrayList<>();
        
        // Action 1: Set field
        YamlTransformation.TransformationAction a1 = new YamlTransformation.TransformationAction();
        a1.setType("set-field");
        a1.setField("flag");
        a1.setValue(true);
        actions.add(a1);
        
        // Action 2: Calculate field
        YamlTransformation.TransformationAction a2 = new YamlTransformation.TransformationAction();
        a2.setType("calculate-field");
        a2.setField("doubleAmount");
        a2.setExpression("#amount * 2");
        actions.add(a2);
        
        // Action 3: Copy field
        YamlTransformation.TransformationAction a3 = new YamlTransformation.TransformationAction();
        a3.setType("copy-field");
        a3.setField("originalAmount");
        a3.setSourceField("amount");
        actions.add(a3);

        rule.setActions(actions);
        transformation.setTransformationRules(Collections.singletonList(rule));

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("amount", 100.0);

        RuleResult result = processor.processTransformationsWithResult(Collections.singletonList(transformation), inputData);

        assertTrue(result.isSuccess());
        Map<String, Object> data = result.getEnrichedData();
        assertEquals(true, data.get("flag"));
        assertEquals(200.0, data.get("doubleAmount"));
        assertEquals(100.0, data.get("originalAmount"));
    }
    
    @Test
    @DisplayName("Test Remove Field Action")
    void testRemoveFieldAction() {
        YamlTransformation transformation = new YamlTransformation();
        transformation.setId("remove-field-trans");
        transformation.setType("conditional-transformation");
        
        YamlTransformation.TransformationRule rule = new YamlTransformation.TransformationRule();
        rule.setCondition("true");
        
        YamlTransformation.TransformationAction action = new YamlTransformation.TransformationAction();
        action.setType("remove-field");
        action.setField("sensitiveData");
        
        rule.setActions(Collections.singletonList(action));
        transformation.setTransformationRules(Collections.singletonList(rule));

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("amount", 100.0);
        inputData.put("sensitiveData", "secret");

        RuleResult result = processor.processTransformationsWithResult(Collections.singletonList(transformation), inputData);

        assertTrue(result.isSuccess());
        assertFalse(result.getEnrichedData().containsKey("sensitiveData"));
        assertTrue(result.getEnrichedData().containsKey("amount"));
    }

    @Test
    @DisplayName("Test Error Handling in Conditional Transformation")
    void testErrorHandling() {
        logger.info("=== INTENTIONAL ERROR TEST: Division by zero in conditional transformation ===");
        YamlTransformation transformation = new YamlTransformation();
        transformation.setId("error-trans");
        transformation.setType("conditional-transformation");
        
        YamlTransformation.TransformationRule rule = new YamlTransformation.TransformationRule();
        rule.setCondition("true");
        
        YamlTransformation.TransformationAction action = new YamlTransformation.TransformationAction();
        action.setType("calculate-field");
        action.setField("result");
        action.setExpression("1 / 0"); // Division by zero to cause exception
        
        rule.setActions(Collections.singletonList(action));
        transformation.setTransformationRules(Collections.singletonList(rule));

        Map<String, Object> inputData = new HashMap<>();

        RuleResult result = processor.processTransformationsWithResult(Collections.singletonList(transformation), inputData);

        assertFalse(result.isSuccess());
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType());
        assertTrue(result.getMessage().contains("Transformation processing failed"));
    }
}
