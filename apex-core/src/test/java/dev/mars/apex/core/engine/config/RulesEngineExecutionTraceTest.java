package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.model.ExecutionStep;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

public class RulesEngineExecutionTraceTest {

    @Test
    public void testExecutionTrace() throws Exception {
        // Use absolute path or relative to project root. 
        // Since we are running from root, "apex-core/src/test/resources/trace-test.yaml" might be needed if running from root.
        // But RulesEngine.fromFile takes a path.
        // Let's try relative to module first if running inside module, or relative to root.
        // The workspace root is c:\Users\markr\dev\java\corejava\apex-rules-engine
        // So the file is at apex-core/src/test/resources/trace-test.yaml
        
        RulesEngine engine = RulesEngine.fromFile("src/test/resources/tracing/trace-test.yaml");
        Map<String, Object> data = new HashMap<>();
        
        RuleResult result = engine.evaluate(data);
        
        assertNotNull(result.getExecutionPath(), "Execution path should not be null");
        assertEquals(1, result.getExecutionPath().size(), "Should have 1 execution step");
        
        ExecutionStep step = result.getExecutionPath().get(0);
        assertEquals("rule1", step.getName());
        assertEquals("rules", step.getType());
        assertEquals("SUCCESS", step.getStatus());
        assertTrue(step.getDurationMs() >= 0);
    }

    @Test
    public void testExecutionTraceLegacy() throws Exception {
        dev.mars.apex.core.config.yaml.YamlConfigurationLoader loader = new dev.mars.apex.core.config.yaml.YamlConfigurationLoader();
        dev.mars.apex.core.config.yaml.YamlRuleConfiguration config = loader.loadFromFile("src/test/resources/tracing/trace-test-legacy.yaml");
        
        // Force legacy mode by clearing item order
        config.setItemOrder(null);
        
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        Map<String, Object> data = new HashMap<>();
        
        RuleResult result = engine.evaluate(data);
        
        assertNotNull(result.getExecutionPath(), "Execution path should not be null");
        // Should have at least "rules" section
        boolean foundRules = false;
        for (ExecutionStep step : result.getExecutionPath()) {
            if ("rules".equals(step.getName()) && "SECTION".equals(step.getType())) {
                foundRules = true;
                break;
            }
        }
        assertTrue(foundRules, "Should have rules section trace");
    }
}
