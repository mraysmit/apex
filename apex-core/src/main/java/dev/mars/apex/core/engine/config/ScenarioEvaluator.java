package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.service.scenario.ScenarioExecutionResult;

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
 * Fluent API interface for type-safe scenario evaluation.
 * 
 * <p>This interface provides a fluent, type-safe way to evaluate scenarios
 * without requiring casting. It is obtained from {@link RulesEngine#asScenario()}
 * and provides three evaluation modes:</p>
 * 
 * <ul>
 *   <li><b>Direct evaluation</b> - Evaluate a single scenario configuration</li>
 *   <li><b>Registry-based evaluation</b> - Evaluate a specific scenario by ID from a registry</li>
 *   <li><b>Classification-based evaluation</b> - Automatically select and evaluate the matching scenario</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * 
 * <pre>
 * // Direct scenario evaluation
 * ScenarioExecutionResult result = RulesEngine.fromFile("scenario-config.yaml")
 *     .asScenario()
 *     .evaluate(data);
 * 
 * // Registry-based with scenario ID
 * ScenarioExecutionResult result = RulesEngine.fromScenarioRegistry("registry.yaml")
 *     .asScenario()
 *     .evaluate("basic-trade-processing", data);
 * 
 * // Classification-based routing (automatic scenario selection)
 * ScenarioExecutionResult result = RulesEngine.fromScenarioRegistry("registry.yaml")
 *     .asScenario()
 *     .evaluateWithClassification(data);
 * </pre>
 * 
 * <p><b>Benefits:</b></p>
 * <ul>
 *   <li>No casting required - returns {@link ScenarioExecutionResult} directly</li>
 *   <li>Type-safe at compile time</li>
 *   <li>Fluent, readable API with method chaining</li>
 *   <li>Clear intent - developer knows they're working with scenarios</li>
 * </ul>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 * @see RulesEngine#asScenario()
 * @see ScenarioExecutionResult
 */
public interface ScenarioEvaluator {
    
    /**
     * Evaluate a single scenario configuration with the provided input data.
     * 
     * <p>This method is used when the RulesEngine was created from a single
     * scenario configuration file (not a registry).</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * ScenarioExecutionResult result = RulesEngine.fromFile("scenario-config.yaml")
     *     .asScenario()
     *     .evaluate(data);
     * </pre>
     * 
     * @param inputData The input data to process through the scenario stages.
     *                  Must be a Map containing the data fields required by the scenario.
     * @return ScenarioExecutionResult containing the results of all stage executions,
     *         warnings, review flags, and overall execution status
     * @throws IllegalStateException if the configuration does not contain a scenario
     * @throws NullPointerException if inputData is null
     */
    ScenarioExecutionResult evaluate(Map<String, Object> inputData);
    
    /**
     * Evaluate a specific scenario by ID from a scenario registry.
     * 
     * <p>This method is used when the RulesEngine was created from a scenario
     * registry file containing multiple scenario definitions.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * ScenarioExecutionResult result = RulesEngine.fromScenarioRegistry("registry.yaml")
     *     .asScenario()
     *     .evaluate("basic-trade-processing", data);
     * </pre>
     * 
     * @param scenarioId The unique identifier of the scenario to evaluate.
     *                   Must match a scenario-id defined in the registry.
     * @param inputData The input data to process through the scenario stages.
     *                  Must be a Map containing the data fields required by the scenario.
     * @return ScenarioExecutionResult containing the results of all stage executions,
     *         warnings, review flags, and overall execution status
     * @throws IllegalArgumentException if scenarioId is not found in the registry
     * @throws IllegalStateException if the configuration does not contain a scenario registry
     * @throws NullPointerException if scenarioId or inputData is null
     */
    ScenarioExecutionResult evaluate(String scenarioId, Map<String, Object> inputData);
    
    /**
     * Automatically select and evaluate the matching scenario based on classification rules.
     * 
     * <p>This method evaluates the classification rules of all scenarios in the registry
     * and executes the first scenario whose classification rule matches the input data.
     * Classification rules are SpEL expressions that evaluate against the input data.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * // Registry contains scenarios with classification rules like:
     * // "#'tradeType'] == 'OTCOption' && #'region'] == 'US'"
     * 
     * Map&lt;String, Object&gt; data = new HashMap&lt;&gt;();
     * data.put("tradeType", "OTCOption");
     * data.put("region", "US");
     * 
     * ScenarioExecutionResult result = RulesEngine.fromScenarioRegistry("registry.yaml")
     *     .asScenario()
     *     .evaluateWithClassification(data);
     * </pre>
     * 
     * @param inputData The input data to classify and process.
     *                  Must be a Map containing the data fields used by classification rules.
     * @return ScenarioExecutionResult containing the results of the matched scenario execution.
     *         If no scenario matches, returns a result with status indicating no match found.
     * @throws IllegalStateException if the configuration does not contain a scenario registry
     * @throws NullPointerException if inputData is null
     */
    ScenarioExecutionResult evaluateWithClassification(Map<String, Object> inputData);
}

