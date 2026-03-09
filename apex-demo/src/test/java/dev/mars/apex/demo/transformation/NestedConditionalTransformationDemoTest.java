package dev.mars.apex.demo.transformation;

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.model.YamlTransformation;
import dev.mars.apex.core.service.transform.TransformationProcessor;
import dev.mars.apex.core.config.loader.ConfigurationLoader;
import dev.mars.apex.engine.model.RuleResult;
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
public class NestedConditionalTransformationDemoTest {

    private static final Logger logger = LoggerFactory.getLogger(NestedConditionalTransformationDemoTest.class);
    private ConfigurationLoader yamlLoader;
    private TransformationProcessor transformationProcessor;

    @BeforeEach
    public void setUp() {
        yamlLoader = new ConfigurationLoader();
        transformationProcessor = new TransformationProcessor();
    }

    @Test
    public void testDeeplyNestedTransformation() throws Exception {
        // Load the configuration
        String filePath = "src/test/java/dev/mars/apex/demo/transformation/NestedConditionalTransformationTest.yaml";
        File configFile = new File(filePath);
        String absolutePath = configFile.getAbsolutePath();
        
        logger.info("Loading configuration from: {}", absolutePath);
        
        // Load transformations
        YamlRuleConfiguration config = yamlLoader.loadFromFile(absolutePath);
        assertNotNull(config, "Configuration should not be null");
        
        List<YamlTransformation> transformationList = config.getTransformations();
        assertNotNull(transformationList, "Transformations list should not be null");
        assertFalse(transformationList.isEmpty(), "Transformations list should not be empty");
        
        Map<String, YamlTransformation> transformations = new HashMap<>();
        for (YamlTransformation t : transformationList) {
            transformations.put(t.getId(), t);
        }
        
        YamlTransformation transformation = transformations.get("nested-discount-logic");
        assertNotNull(transformation, "Transformation 'nested-discount-logic' should exist");

        // Test Case 1: All conditions met (Happy Path)
        Map<String, Object> data = new HashMap<>();
        data.put("region", "NA");
        data.put("amount", 1500);
        data.put("currency", "USD");
        data.put("customerType", "VIP");

        logger.info("Testing deep nesting with data: {}", data);
        
        RuleResult ruleResult = transformationProcessor.processTransformationWithResult(transformation, data);
        assertNotNull(ruleResult, "Rule result should not be null");
        
        Map<String, Object> result = ruleResult.getEnrichedData();
        
        logger.info("Result: {}", result);
        
        assertEquals("passed", result.get("regionCheck"));
        assertEquals("passed", result.get("amountCheck"));
        assertEquals("passed", result.get("currencyCheck"));
        assertEquals("passed", result.get("finalCheck"));
        assertEquals(0.20, result.get("discount"));
    }

    @Test
    public void testNestedTransformationPartialMatch() throws Exception {
        // Load the configuration
        String filePath = "src/test/java/dev/mars/apex/demo/transformation/NestedConditionalTransformationTest.yaml";
        File configFile = new File(filePath);
        String absolutePath = configFile.getAbsolutePath();
        
        YamlRuleConfiguration config = yamlLoader.loadFromFile(absolutePath);
        List<YamlTransformation> transformationList = config.getTransformations();
        
        Map<String, YamlTransformation> transformations = new HashMap<>();
        for (YamlTransformation t : transformationList) {
            transformations.put(t.getId(), t);
        }
        
        YamlTransformation transformation = transformations.get("nested-discount-logic");

        // Test Case 2: Fail at level 3 (Currency)
        Map<String, Object> data = new HashMap<>();
        data.put("region", "NA");
        data.put("amount", 1500);
        data.put("currency", "EUR"); // Wrong currency
        data.put("customerType", "VIP");

        logger.info("Testing partial match with data: {}", data);
        
        RuleResult ruleResult = transformationProcessor.processTransformationWithResult(transformation, data);
        assertNotNull(ruleResult, "Rule result should not be null");
        
        Map<String, Object> result = ruleResult.getEnrichedData();
        
        logger.info("Result: {}", result);
        
        assertEquals("passed", result.get("regionCheck"));
        assertEquals("passed", result.get("amountCheck"));
        assertEquals("failed-currency-mismatch", result.get("currencyCheck"), "Currency check should have failed with specific message");
        assertNull(result.get("discount"), "Discount should not be applied");
    }
}
