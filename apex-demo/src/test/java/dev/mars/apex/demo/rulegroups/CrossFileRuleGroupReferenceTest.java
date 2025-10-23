package dev.mars.apex.demo.rulegroups;

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cross-file rule-group references using the new two-phase approach.
 * This demonstrates that rule groups can reference other rule groups defined in different files.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Cross-File Rule Group Reference Tests")
public class CrossFileRuleGroupReferenceTest extends DemoTestBase {

    private static final String BASE_GROUPS_PATH = "src/test/java/dev/mars/apex/demo/rulegroups/CrossFileBaseRuleGroups.yaml";
    private static final String COMPOSITE_GROUPS_PATH = "src/test/java/dev/mars/apex/demo/rulegroups/CrossFileCompositeRuleGroups.yaml";

    @Test
    @DisplayName("Cross-file rule-group reference: composite group references base group from another file")
    void testCrossFileRuleGroupReference() throws YamlConfigurationException {
        // Create rules engine from multiple files using the new cross-file reference capability
        YamlRulesEngineService service = new YamlRulesEngineService();
        RulesEngine engine = service.createRulesEngineFromMultipleFiles(BASE_GROUPS_PATH, COMPOSITE_GROUPS_PATH);
        
        assertNotNull(engine, "Rules engine should be created successfully");
        
        // Verify the composite group exists and contains rules from both files
        RuleGroup compositeGroup = engine.getConfiguration().getRuleGroupById("cf_composite");
        assertNotNull(compositeGroup, "Composite group should exist");
        
        // The composite group should have 3 rules: income-validation + age-validation + email-validation
        assertEquals(3, compositeGroup.getRules().size(), 
            "Composite group should contain 3 rules: 1 local + 2 from referenced base group");
        
        // Test with valid data - should pass all validations
        Map<String, Object> validData = new HashMap<>();
        validData.put("age", 25);
        validData.put("email", "test@example.com");
        validData.put("income", 50000);
        
        // Test with valid data - should pass
        RuleResult result = engine.executeRuleGroupsList(List.of(compositeGroup), validData);
        assertTrue(result.isTriggered(), "Validation should pass with valid data");

        // Test with invalid age - should fail
        Map<String, Object> invalidAgeData = new HashMap<>();
        invalidAgeData.put("age", 16);  // Too young
        invalidAgeData.put("email", "test@example.com");
        invalidAgeData.put("income", 50000);

        RuleResult failResult = engine.executeRuleGroupsList(List.of(compositeGroup), invalidAgeData);
        assertFalse(failResult.isTriggered(), "Validation should fail with invalid age");

        // Test with missing email - should fail
        Map<String, Object> invalidEmailData = new HashMap<>();
        invalidEmailData.put("age", 25);
        invalidEmailData.put("email", "invalid-email");  // No @ symbol
        invalidEmailData.put("income", 50000);

        RuleResult failResult2 = engine.executeRuleGroupsList(List.of(compositeGroup), invalidEmailData);
        assertFalse(failResult2.isTriggered(), "Validation should fail with invalid email");
        
        logger.info("✅ Cross-file rule-group reference test completed successfully!");
        logger.info("   - Composite group successfully references base group from another file");
        logger.info("   - All 3 rules (1 local + 2 from referenced group) are working correctly");
    }

    @Test
    @DisplayName("Verify base group works independently")
    void testBaseGroupIndependently() throws YamlConfigurationException {
        // Test that the base group works on its own
        YamlRulesEngineService service = new YamlRulesEngineService();
        RulesEngine engine = service.createRulesEngineFromMultipleFiles(BASE_GROUPS_PATH);
        
        RuleGroup baseGroup = engine.getConfiguration().getRuleGroupById("base_validation");
        assertNotNull(baseGroup, "Base group should exist");
        assertEquals(2, baseGroup.getRules().size(), "Base group should have 2 rules");
        
        // Test with valid data
        Map<String, Object> validData = new HashMap<>();
        validData.put("age", 25);
        validData.put("email", "test@example.com");
        
        RuleResult result = engine.executeRuleGroupsList(List.of(baseGroup), validData);
        assertTrue(result.isTriggered(), "Base validation should pass with valid data");
        
        logger.info("✅ Base group works independently");
    }
}
