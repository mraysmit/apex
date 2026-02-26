package dev.mars.apex.engine.core;

import dev.mars.apex.core.config.exception.YamlConfigurationException;
import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.DisplayName;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RulesEngine static factory methods.
 * Verifies that the new simplified API works correctly.
 */
@DisplayName("RulesEngine Static Factory Methods Tests")
class RulesEngineStaticFactoryMethodsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("fromFile() should create RulesEngine from YAML file")
    void testFromFile() throws Exception {
        // Create a simple YAML file
        String yamlContent = """
                metadata:
                  name: "Test Configuration"
                  version: "1.0"

                rules:
                  - id: "test-rule-1"
                    name: "Test Rule"
                    condition: "#age > 18"
                    message: "Adult"
                    priority: 100
                """;

        Path yamlFile = tempDir.resolve("test-config.yaml");
        Files.writeString(yamlFile, yamlContent);

        // Test: Create engine using static factory method
        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());

        // Verify: Engine was created successfully
        assertNotNull(engine);
        assertNotNull(engine.getConfiguration());

        // Verify: Can evaluate using simplified method
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("age", 25);

        RuleResult result = engine.evaluate(inputData);

        assertNotNull(result);
        assertTrue(result.isTriggered());
    }

    @Test
    @DisplayName("fromYamlConfig() should create RulesEngine from YamlRuleConfiguration")
    void testFromYamlConfig() throws Exception {
        // Create a simple YAML file
        String yamlContent = """
                metadata:
                  name: "Test Configuration"
                  version: "1.0"

                rules:
                  - id: "test-rule-1"
                    name: "Test Rule"
                    condition: "#score >= 90"
                    message: "Excellent"
                    priority: 100
                """;

        Path yamlFile = tempDir.resolve("test-config.yaml");
        Files.writeString(yamlFile, yamlContent);

        // Load configuration
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration yamlConfig = loader.loadFromFile(yamlFile.toString());

        // Test: Create engine using static factory method
        RulesEngine engine = RulesEngine.fromYamlConfig(yamlConfig);

        // Verify: Engine was created successfully
        assertNotNull(engine);
        assertNotNull(engine.getConfiguration());

        // Verify: Can evaluate using simplified method
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("score", 95);

        RuleResult result = engine.evaluate(inputData);

        assertNotNull(result);
        assertTrue(result.isTriggered());
    }

    @Test
    @DisplayName("evaluate(Object) should work with Map input")
    void testEvaluateWithMapInput() throws Exception {
        // Create a simple YAML file
        String yamlContent = """
                metadata:
                  name: "Test Configuration"
                  version: "1.0"

                rules:
                  - id: "test-rule-1"
                    name: "Test Rule"
                    condition: "#status == 'active'"
                    message: "Active user"
                    priority: 100
                """;

        Path yamlFile = tempDir.resolve("test-config.yaml");
        Files.writeString(yamlFile, yamlContent);

        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());

        // Test: Evaluate with Map input
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("status", "active");

        RuleResult result = engine.evaluate(inputData);

        assertNotNull(result);
        assertTrue(result.isTriggered());
    }

    @Test
    @DisplayName("evaluate(Map) should return error RuleResult when engine created without YAML config")
    void testEvaluateReturnsErrorWhenNoYamlConfig() {
        // Create engine using constructor (without YAML config)
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        RulesEngine engine = new RulesEngine(config);

        // Test: Simplified evaluate should return error RuleResult
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("test", "value");

        RuleResult result = engine.evaluate(inputData);

        // Returns RuleResult with failure details instead of throwing
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Result should indicate failure");
        assertFalse(result.getFailureMessages().isEmpty(), "Should have failure messages");
        assertTrue(result.getFailureMessages().stream().anyMatch(m -> m.contains("Cannot use simplified evaluate(Map) method")),
                  "Failure messages should explain the problem");
        assertTrue(result.getFailureMessages().stream().anyMatch(m -> m.contains("RulesEngine.fromFile()")),
                  "Failure messages should suggest using static factory methods");
    }

    @Test
    @DisplayName("fromFile() should handle enrichments correctly")
    void testFromFileWithEnrichments() throws Exception {
        // Create YAML with enrichments
        String yamlContent = """
                metadata:
                  name: "Test Configuration with Enrichments"
                  version: "1.0"

                enrichments:
                  - id: "enrich-1"
                    name: "Add Full Name"
                    type: "field-enrichment"
                    condition: "#firstName != null && #lastName != null"
                    field-mappings:
                      - source-field: "#firstName + ' ' + #lastName"
                        target-field: "fullName"

                rules:
                  - id: "test-rule-1"
                    name: "Test Rule"
                    condition: "#fullName != null"
                    message: "Full name present"
                    priority: 100
                """;

        Path yamlFile = tempDir.resolve("test-enrichment-config.yaml");
        Files.writeString(yamlFile, yamlContent);

        // Test: Create engine and evaluate
        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("firstName", "John");
        inputData.put("lastName", "Doe");

        RuleResult result = engine.evaluate(inputData);

        // Verify: Enrichment was applied and rule triggered
        assertNotNull(result);
        assertTrue(result.isTriggered());
        assertEquals("John Doe", result.getEnrichedData().get("fullName"));
    }

    @Test
    @DisplayName("Static factory methods should provide better ergonomics than verbose pattern")
    void testErgonomicsComparison() throws Exception {
        // Create a simple YAML file
        String yamlContent = """
                metadata:
                  name: "Ergonomics Test"
                  version: "1.0"

                rules:
                  - id: "test-rule-1"
                    name: "Test Rule"
                    condition: "#value > 0"
                    message: "Positive value"
                    priority: 100
                """;

        Path yamlFile = tempDir.resolve("test-config.yaml");
        Files.writeString(yamlFile, yamlContent);

        // NEW PATTERN (2 lines) - Simple and ergonomic
        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());
        RuleResult result = engine.evaluate(Map.of("value", 42));

        // Verify: Works correctly
        assertNotNull(result);
        assertTrue(result.isTriggered());

        // Compare with VERBOSE PATTERN (7 lines) - Still works but more complex
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration yamlConfig = loader.loadFromFile(yamlFile.toString());

        dev.mars.apex.core.config.YamlRuleFactory ruleFactory = new dev.mars.apex.core.config.YamlRuleFactory();
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);

        RulesEngine verboseEngine = new RulesEngine(config);
        RuleResult verboseResult = verboseEngine.evaluate(yamlConfig, Map.of("value", 42));

        // Verify: Both patterns produce same result
        assertEquals(result.isTriggered(), verboseResult.isTriggered());
    }
}

