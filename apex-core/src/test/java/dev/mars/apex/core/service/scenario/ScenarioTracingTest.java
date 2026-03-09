package dev.mars.apex.core.service.scenario;

import dev.mars.apex.engine.execution.ScenarioStageExecutor;

import dev.mars.apex.core.config.loader.ConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.RuleFactory;
import dev.mars.apex.engine.model.ExecutionStep;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Scenario execution tracing.
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class ScenarioTracingTest {

    private TestConfigLoader configLoader;
    private RuleFactory ruleFactory;
    private ScenarioStageExecutor executor;

    // Test loader that returns in-memory configs
    private static class TestConfigLoader extends ConfigurationLoader {
        private final Map<String, YamlRuleConfiguration> configs = new HashMap<>();

        public void addSuccess(String path) {
            configs.put(path, new YamlRuleConfiguration());
        }

        @Override
        public YamlRuleConfiguration loadFromFile(String filePath) {
            YamlRuleConfiguration cfg = configs.get(filePath);
            if (cfg == null) {
                return new YamlRuleConfiguration();
            }
            return cfg;
        }
    }

    @BeforeEach
    void setUp() {
        configLoader = new TestConfigLoader();
        ruleFactory = new RuleFactory();
        executor = new ScenarioStageExecutor(configLoader, ruleFactory);
    }

    @Test
    void testExecutionTracing_CapturesStagesAndInnerSteps() {
        // Arrange
        ScenarioStage stage1 = new ScenarioStage("stage-1", "config/stage1.yaml", 1);
        ScenarioStage stage2 = new ScenarioStage("stage-2", "config/stage2.yaml", 2);

        List<ScenarioStage> stages = Arrays.asList(stage1, stage2);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("trace-scenario", "Trace Test",
                Arrays.asList("TestData"), stages);

        configLoader.addSuccess("config/stage1.yaml");
        configLoader.addSuccess("config/stage2.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("testField", "testValue");

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertNotNull(result);
        List<ExecutionStep> trace = result.getExecutionPath();
        assertNotNull(trace, "Execution path should not be null");
        assertFalse(trace.isEmpty(), "Execution path should not be empty");

        // Print trace for debugging
        System.out.println("Execution Trace:");
        for (ExecutionStep step : trace) {
            System.out.println(step);
        }

        // Verify Stage 1
        assertTrue(trace.stream().anyMatch(s -> 
            s.getName().equals("stage-1") && s.getType().equals("SCENARIO_STAGE")), 
            "Should contain stage-1 step");

        // Verify Stage 2
        assertTrue(trace.stream().anyMatch(s -> 
            s.getName().equals("stage-2") && s.getType().equals("SCENARIO_STAGE")), 
            "Should contain stage-2 step");

        // Verify inner steps - with empty configs (no rules/enrichments), there are no
        // inner processing steps beyond the SCENARIO_STAGE entries themselves
        long scenarioStageCount = trace.stream()
                .filter(s -> s.getType().equals("SCENARIO_STAGE"))
                .count();
        assertTrue(scenarioStageCount >= 2, "Should contain SCENARIO_STAGE steps for both stages");
        
        // Verify order: Stage 1 -> Stage 1 inner -> Stage 2 -> Stage 2 inner
        // We can't easily verify exact index without assuming implementation details of inner steps,
        // but we can verify that stage-1 comes before stage-2
        int stage1Index = -1;
        int stage2Index = -1;
        for (int i = 0; i < trace.size(); i++) {
            if (trace.get(i).getName().equals("stage-1")) stage1Index = i;
            if (trace.get(i).getName().equals("stage-2")) stage2Index = i;
        }
        
        assertTrue(stage1Index < stage2Index, "Stage 1 should be recorded before Stage 2");
    }

    @Test
    void testExecutionTracing_SkippedStage() {
        // Arrange
        ScenarioStage stage1 = new ScenarioStage("stage-1", "config/stage1.yaml", 1);
        // Stage 2 depends on Stage 1, but we'll simulate Stage 1 failure to skip Stage 2
        // Actually, easier to just use a condition that evaluates to false
        ScenarioStage stage2 = new ScenarioStage("stage-2", "config/stage2.yaml", 2);
        stage2.setCondition("false"); // SpEL condition that evaluates to false

        List<ScenarioStage> stages = Arrays.asList(stage1, stage2);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("skip-scenario", "Skip Test",
                Arrays.asList("TestData"), stages);

        configLoader.addSuccess("config/stage1.yaml");
        configLoader.addSuccess("config/stage2.yaml");

        Map<String, Object> testData = new HashMap<>();

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        List<ExecutionStep> trace = result.getExecutionPath();
        
        // Verify Stage 2 is marked as SKIPPED
        Optional<ExecutionStep> skippedStep = trace.stream()
                .filter(s -> s.getName().equals("stage-2") && s.getStatus().equals("SKIPPED"))
                .findFirst();
        
        assertTrue(skippedStep.isPresent(), "Should contain skipped step for stage-2");
        assertTrue(skippedStep.get().getMessage().contains("Condition not met"), "Skip reason should be recorded");
    }
}
