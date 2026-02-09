package dev.mars.apex.core.engine.config.scenario;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.util.DataCopyUtility;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import dev.mars.apex.core.service.scenario.ScenarioExecutionResult;
import dev.mars.apex.core.service.scenario.ScenarioStageExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Manages scenario evaluation including YAML-based, registry-based, and classification-based routing.
 * 
 * <p>This manager provides three evaluation strategies:</p>
 * <ul>
 *   <li><b>YAML-based</b>: Evaluate a single scenario from YAML configuration</li>
 *   <li><b>Registry-based</b>: Evaluate a specific scenario by ID from a registry</li>
 *   <li><b>Classification-based</b>: Automatically select and evaluate the matching scenario</li>
 * </ul>
 *
 * <p>All methods perform deep copying of input data to ensure thread safety and data isolation.</p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-22
 */
public class ScenarioEvaluationManager {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioEvaluationManager.class);

    private final YamlRuleConfiguration yamlConfig;
    private final Map<String, ScenarioConfiguration> scenarioRegistry;
    private final ScenarioParser scenarioParser;
    private final ScenarioLookupStrategy scenarioLookup;

    /**
     * Create a new ScenarioEvaluationManager.
     *
     * @param yamlConfig The YAML configuration (may be null if using registry)
     * @param scenarioRegistry The scenario registry (may be null if using YAML config)
     * @param scenarioParser Parser for YAML-based scenarios
     * @param scenarioLookup Strategy for looking up scenarios from registry
     */
    public ScenarioEvaluationManager(
            YamlRuleConfiguration yamlConfig,
            Map<String, ScenarioConfiguration> scenarioRegistry,
            ScenarioParser scenarioParser,
            ScenarioLookupStrategy scenarioLookup) {
        this.yamlConfig = yamlConfig;
        this.scenarioRegistry = scenarioRegistry;
        this.scenarioParser = scenarioParser;
        this.scenarioLookup = scenarioLookup;
    }

    /**
     * Evaluate a single scenario configuration with the provided input data.
     *
     * <p>This method is used when the RulesEngine was created from a single
     * scenario configuration file (not a registry). It processes the input data
     * through all stages defined in the scenario configuration.</p>
     *
     * @param inputData The input data to process through the scenario stages
     * @return ScenarioExecutionResult containing the results of all stage executions
     * @throws IllegalStateException if the configuration does not contain a scenario
     * @throws NullPointerException if inputData is null
     */
    public ScenarioExecutionResult evaluateScenario(Map<String, Object> inputData) {
        if (inputData == null) {
            throw new NullPointerException("Input data cannot be null");
        }

        if (this.yamlConfig == null) {
            throw new IllegalStateException(
                "Cannot use evaluateScenario(Map) method - this RulesEngine was not created with a YAML configuration. " +
                "Use RulesEngine.fromFile() or RulesEngine.fromYamlConfig() to create the engine."
            );
        }

        // Check if YAML configuration contains a scenario
        if (!this.yamlConfig.hasScenario()) {
            throw new IllegalStateException(
                "YAML configuration does not contain a scenario section. " +
                "Use a scenario configuration file or RulesEngine.fromScenarioRegistry() for scenario evaluation."
            );
        }

        logger.info("Evaluating scenario from YAML configuration");

        // Parse scenario configuration from YAML using ScenarioParser
        ScenarioConfiguration scenario = scenarioParser.parseFromYaml(this.yamlConfig);

        // Create ScenarioStageExecutor and execute stages
        ScenarioStageExecutor executor = new ScenarioStageExecutor();

        // Deep copy input data to protect against callers sharing the same map across concurrent calls
        Map<String, Object> safeInputData = DataCopyUtility.deepCopyMap(inputData);

        return executor.executeStages(scenario, safeInputData);
    }

    /**
     * Evaluate a specific scenario by ID from a scenario registry.
     *
     * <p>This method is used when the RulesEngine was created from a scenario
     * registry file containing multiple scenario definitions. It looks up the
     * scenario by ID and processes the input data through its stages.</p>
     *
     * @param scenarioId The unique identifier of the scenario to evaluate
     * @param inputData The input data to process through the scenario stages
     * @return ScenarioExecutionResult containing the results of all stage executions
     * @throws IllegalArgumentException if scenarioId is not found in the registry
     * @throws IllegalStateException if the configuration does not contain a scenario registry
     * @throws NullPointerException if scenarioId or inputData is null
     */
    public ScenarioExecutionResult evaluateScenario(String scenarioId, Map<String, Object> inputData) {
        if (scenarioId == null) {
            throw new NullPointerException("Scenario ID cannot be null");
        }
        if (inputData == null) {
            throw new NullPointerException("Input data cannot be null");
        }

        if (this.scenarioRegistry == null) {
            throw new IllegalStateException(
                "Cannot use evaluateScenario(String, Map) method - this RulesEngine was not created with a scenario registry. " +
                "Use RulesEngine.fromScenarioRegistry() to create the engine."
            );
        }

        logger.info("Evaluating scenario by ID: {}", scenarioId);

        // Look up scenario from registry using the lookup strategy
        ScenarioConfiguration scenario = scenarioLookup.getScenario(scenarioId);

        // Create ScenarioStageExecutor and execute stages
        ScenarioStageExecutor executor = new ScenarioStageExecutor();

        // Deep copy input data to protect against callers sharing the same map across concurrent calls
        Map<String, Object> safeInputData = DataCopyUtility.deepCopyMap(inputData);

        return executor.executeStages(scenario, safeInputData);
    }

    /**
     * Automatically select and evaluate the matching scenario based on classification rules.
     *
     * <p>This method evaluates the classification rules of all scenarios in the registry
     * and executes the first scenario whose classification rule matches the input data.
     * Classification rules are SpEL expressions that evaluate against the input data.</p>
     *
     * @param inputData The input data to classify and process
     * @return ScenarioExecutionResult containing the results of the matched scenario execution
     * @throws IllegalStateException if the configuration does not contain a scenario registry or no match found
     * @throws NullPointerException if inputData is null
     */
    public ScenarioExecutionResult evaluateWithClassification(Map<String, Object> inputData) {
        if (inputData == null) {
            throw new NullPointerException("Input data cannot be null");
        }

        if (this.scenarioRegistry == null) {
            throw new IllegalStateException(
                "Cannot use evaluateWithClassification(Map) method - this RulesEngine was not created with a scenario registry. " +
                "Use RulesEngine.fromScenarioRegistry() to create the engine."
            );
        }

        logger.info("Evaluating scenario using classification-based routing");

        // Find matching scenario based on classification rules using the lookup strategy
        ScenarioConfiguration scenario = scenarioLookup.findMatchingScenario(inputData);

        if (scenario == null) {
            throw new IllegalStateException(
                "No matching scenario found for the provided input data. " +
                "Ensure that at least one scenario's classification rule matches the data."
            );
        }

        logger.info("Matched scenario: {}", scenario.getScenarioId());

        // Create ScenarioStageExecutor and execute stages
        ScenarioStageExecutor executor = new ScenarioStageExecutor();

        // Deep copy input data to protect against callers sharing the same map across concurrent calls
        Map<String, Object> safeInputData = DataCopyUtility.deepCopyMap(inputData);

        return executor.executeStages(scenario, safeInputData);
    }

    /**
     * Get a fluent API evaluator for type-safe scenario evaluation.
     *
     * @return A ScenarioEvaluator instance for fluent scenario evaluation
     * @throws IllegalStateException if the configuration does not contain scenarios
     */
    public ScenarioEvaluator asScenario() {
        if (this.yamlConfig == null && this.scenarioRegistry == null) {
            throw new IllegalStateException(
                "Cannot use asScenario() method - this RulesEngine was not created with a scenario configuration or registry. " +
                "Use RulesEngine.fromFile() or RulesEngine.fromScenarioRegistry() to create the engine."
            );
        }

        return new ScenarioEvaluatorImpl(this);
    }

    /**
     * Fluent API interface for scenario evaluation.
     * Provides type-safe method chaining for scenario operations.
     *
     * @since 2026-01-22
     */
    public interface ScenarioEvaluator {
        /**
         * Evaluate a scenario with the provided input data.
         * Delegates to evaluateScenario(Map).
         *
         * @param inputData The input data to process
         * @return ScenarioExecutionResult containing execution results
         */
        ScenarioExecutionResult evaluate(Map<String, Object> inputData);

        /**
         * Evaluate a specific scenario by ID.
         * Delegates to evaluateScenario(String, Map).
         *
         * @param scenarioId The scenario ID to evaluate
         * @param inputData The input data to process
         * @return ScenarioExecutionResult containing execution results
         */
        ScenarioExecutionResult evaluate(String scenarioId, Map<String, Object> inputData);

        /**
         * Evaluate using classification-based routing.
         * Delegates to evaluateWithClassification(Map).
         *
         * @param inputData The input data to classify and process
         * @return ScenarioExecutionResult containing execution results
         */
        ScenarioExecutionResult evaluateWithClassification(Map<String, Object> inputData);
    }

    /**
     * Implementation of ScenarioEvaluator interface.
     * Delegates all calls back to the ScenarioEvaluationManager.
     *
     * @since 2026-01-22
     */
    private static class ScenarioEvaluatorImpl implements ScenarioEvaluator {
        private final ScenarioEvaluationManager manager;

        /**
         * Create a new ScenarioEvaluatorImpl wrapping the given manager.
         *
         * @param manager The ScenarioEvaluationManager instance to delegate to
         */
        ScenarioEvaluatorImpl(ScenarioEvaluationManager manager) {
            this.manager = manager;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ScenarioExecutionResult evaluate(Map<String, Object> inputData) {
            return manager.evaluateScenario(inputData);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ScenarioExecutionResult evaluate(String scenarioId, Map<String, Object> inputData) {
            return manager.evaluateScenario(scenarioId, inputData);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ScenarioExecutionResult evaluateWithClassification(Map<String, Object> inputData) {
            return manager.evaluateWithClassification(inputData);
        }
    }

    /**
     * Strategy interface for looking up scenarios from a registry.
     * Allows for different lookup implementations.
     */
    public interface ScenarioLookupStrategy {
        /**
         * Get a scenario by ID from the registry.
         *
         * @param scenarioId The scenario ID to look up
         * @return The scenario configuration
         * @throws IllegalArgumentException if scenario not found or is disabled
         */
        ScenarioConfiguration getScenario(String scenarioId);

        /**
         * Find the first matching scenario based on classification rules.
         *
         * @param inputData The input data to match against classification rules
         * @return The first matching enabled scenario, or null if no match found
         */
        ScenarioConfiguration findMatchingScenario(Map<String, Object> inputData);
    }
}
