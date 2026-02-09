package dev.mars.apex.demo.test;

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.core.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ColoredTestOutputExtension.class)
class UndefinedVariableErrorTest {

    @Test
    void testUndefinedVariableProducesHelpfulError() throws Exception {
        // Load YAML configuration with undefined variable reference
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.loadFromFile(
            "src/test/resources/test-undefined-variable.yaml"
        );

        // Create rules engine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        // Get the rule that references undefined variable
        var testRule = engine.getConfiguration().getRuleById("test-undefined-var");
        assertNotNull(testRule, "Test rule should be loaded");

        // Test data WITHOUT the variable referenced in YAML
        Map<String, Object> data = new HashMap<>();
        data.put("someOtherField", "value");

        // Execute rule - should produce helpful error message
        RuleResult result = engine.executeRule(testRule, data);

        // Check if error messages contain helpful text about undefined variable
        String errorMessages = result.toString();
        System.out.println("=== ERROR MESSAGE ===");
        System.out.println(errorMessages);
        System.out.println("===================");
        
        // The new error handling should mention the undefined variable
        assertTrue(
            errorMessages.contains("references undefined") || 
            errorMessages.contains("inaccessible variable") ||
            errorMessages.contains("undefinedVariable"),
            "Error message should mention the undefined variable: " + errorMessages
        );
    }
}
