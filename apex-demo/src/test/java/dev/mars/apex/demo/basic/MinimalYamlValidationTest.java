/*
 * Minimal YAML Validation Test
 * Shows the absolute simplest YAML validation rule in APEX
 */

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import java.util.Map;

public class MinimalYamlValidationTest {
    
    public static void main(String[] args) throws Exception {
        
        // 1. Define the simplest possible YAML rule
        String yamlRule = """
            metadata:
              name: "Age Check"
              type: "rule-config"
              version: "1.0.0"
            
            rules:
              - id: "age-validation"
                condition: "#age >= 18"
                message: "Must be 18 or older"
            """;
        
        // 2. Load and create rules engine
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRulesEngineService service = new YamlRulesEngineService();
        
        RulesEngine engine = service.createRulesEngineFromYamlConfig(
            loader.fromYamlString(yamlRule)
        );
        
        // 3. Test with data
        Map<String, Object> person = Map.of("age", 25);
        RuleResult result = engine.evaluate(person);
        
        // 4. Check result
        System.out.println("Age: " + person.get("age"));
        System.out.println("Valid: " + result.isTriggered());
        System.out.println("Message: " + result.getMessage());
        
        // Test with invalid age
        Map<String, Object> youngPerson = Map.of("age", 16);
        RuleResult invalidResult = engine.evaluate(youngPerson);
        
        System.out.println("\nAge: " + youngPerson.get("age"));
        System.out.println("Valid: " + invalidResult.isTriggered());
        System.out.println("Message: " + invalidResult.getMessage());
    }
}
