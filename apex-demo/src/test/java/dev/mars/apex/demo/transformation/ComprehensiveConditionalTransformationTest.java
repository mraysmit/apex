package dev.mars.apex.demo.transformation;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlTransformation;
import dev.mars.apex.core.service.transform.YamlTransformationProcessor;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ColoredTestOutputExtension.class)
public class ComprehensiveConditionalTransformationTest {

    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveConditionalTransformationTest.class);
    private YamlConfigurationLoader yamlLoader;
    private YamlTransformationProcessor transformationProcessor;
    private Map<String, YamlTransformation> transformations;

    @BeforeEach
    public void setUp() throws Exception {
        yamlLoader = new YamlConfigurationLoader();
        transformationProcessor = new YamlTransformationProcessor();
        
        // Load the configuration once
        String filePath = "src/test/java/dev/mars/apex/demo/transformation/ComprehensiveConditionalTransformationTest.yaml";
        File configFile = new File(filePath);
        String absolutePath = configFile.getAbsolutePath();
        
        YamlRuleConfiguration config = yamlLoader.loadFromFile(absolutePath);
        List<YamlTransformation> transformationList = config.getTransformations();
        
        transformations = new HashMap<>();
        for (YamlTransformation t : transformationList) {
            transformations.put(t.getId(), t);
        }
    }

    @Test
    public void testPriorityCheck() {
        // Scenario: actions-true should take precedence over actions
        YamlTransformation transformation = transformations.get("priority-check");
        assertNotNull(transformation, "Transformation 'priority-check' should exist");

        Map<String, Object> data = new HashMap<>();
        RuleResult result = transformationProcessor.processTransformationWithResult(transformation, data);
        
        assertTrue(result.isSuccess());
        assertEquals("modern", result.getEnrichedData().get("priorityResult"), 
            "actions-true should take precedence over legacy actions");
    }

    @Test
    public void testLegacyFallback() {
        // Scenario: actions should be used if actions-true is missing
        YamlTransformation transformation = transformations.get("legacy-fallback");
        assertNotNull(transformation, "Transformation 'legacy-fallback' should exist");

        Map<String, Object> data = new HashMap<>();
        RuleResult result = transformationProcessor.processTransformationWithResult(transformation, data);
        
        assertTrue(result.isSuccess());
        assertEquals("legacy", result.getEnrichedData().get("fallbackResult"), 
            "Legacy actions should be used when actions-true is missing");
    }

    @Test
    public void testFalsePathCheck() {
        // Scenario: actions-false should be executed when condition is false
        YamlTransformation transformation = transformations.get("false-path-check");
        assertNotNull(transformation, "Transformation 'false-path-check' should exist");

        Map<String, Object> data = new HashMap<>();
        RuleResult result = transformationProcessor.processTransformationWithResult(transformation, data);
        
        assertTrue(result.isSuccess());
        assertEquals("correct-false-path", result.getEnrichedData().get("falsePathResult"), 
            "actions-false should be executed when condition is false");
    }

    @Test
    public void testSiblingRules() {
        // Scenario: Multiple rules at the same level should all be evaluated
        YamlTransformation transformation = transformations.get("sibling-rules");
        assertNotNull(transformation, "Transformation 'sibling-rules' should exist");

        Map<String, Object> data = new HashMap<>();
        data.put("type", "A");
        data.put("value", 20);

        RuleResult result = transformationProcessor.processTransformationWithResult(transformation, data);
        
        assertTrue(result.isSuccess());
        Map<String, Object> enriched = result.getEnrichedData();
        
        assertEquals("A", enriched.get("mark"), "First sibling rule should execute");
        assertEquals("big", enriched.get("size"), "Second sibling rule should execute");
    }

    @Test
    public void testComplexNestingPaths_TruePath() {
        // Scenario: L1 True -> L2 True
        YamlTransformation transformation = transformations.get("complex-nesting-paths");
        assertNotNull(transformation, "Transformation 'complex-nesting-paths' should exist");

        Map<String, Object> data = new HashMap<>();
        data.put("l1", true);
        data.put("l2", true);

        RuleResult result = transformationProcessor.processTransformationWithResult(transformation, data);
        
        assertTrue(result.isSuccess());
        Map<String, Object> enriched = result.getEnrichedData();
        
        assertEquals(true, enriched.get("l1_executed"));
        assertEquals("true-path", enriched.get("l2_result"));
        assertNull(enriched.get("l2_false_nested"), "False path nested actions should not execute");
    }

    @Test
    public void testComplexNestingPaths_FalsePath() {
        // Scenario: L1 True -> L2 False -> Nested in False Path
        YamlTransformation transformation = transformations.get("complex-nesting-paths");
        assertNotNull(transformation, "Transformation 'complex-nesting-paths' should exist");

        Map<String, Object> data = new HashMap<>();
        data.put("l1", true);
        data.put("l2", false);

        RuleResult result = transformationProcessor.processTransformationWithResult(transformation, data);
        
        assertTrue(result.isSuccess());
        Map<String, Object> enriched = result.getEnrichedData();
        
        assertEquals(true, enriched.get("l1_executed"));
        assertEquals("false-path", enriched.get("l2_result"));
        assertEquals("executed", enriched.get("l2_false_nested"), "Nested actions inside actions-false should execute");
    }
}
