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
package dev.mars.apex.engine.scenario;

import dev.mars.apex.engine.core.MapPropertyAccessor;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import dev.mars.apex.core.util.EnabledFilter;
import dev.mars.apex.engine.core.ExpressionEvaluatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

/**
 * Manages scenario registry operations including lookup and classification matching.
 *
 * <p>This manager handles two core responsibilities:</p>
 * <ul>
 *   <li><b>Scenario Lookup:</b> Retrieves scenarios by ID with validation (enabled check)</li>
 *   <li><b>Classification Matching:</b> Finds matching scenarios via SpEL-based classification rules</li>
 * </ul>
 *
 * <p><b>Design Pattern:</b> Extracted from RulesEngine to separate scenario registry concerns
 * from core rule execution.</p>
 *
 * @since 2026-01-22
 * @see ScenarioConfiguration
 * @see ScenarioParser
 */
public class ScenarioRegistryManager {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioRegistryManager.class);
    private static final ExpressionParser parser = dev.mars.apex.engine.core.SpelParserHolder.INSTANCE;

    private final Map<String, ScenarioConfiguration> scenarioRegistry;
    private final ExpressionEvaluatorService evaluatorService;

    /**
     * Constructs a ScenarioRegistryManager with required dependencies.
     *
     * @param scenarioRegistry The scenario registry map (name → configuration)
     * @param evaluatorService The SpEL expression evaluator for classification rules
     */
    public ScenarioRegistryManager(
            Map<String, ScenarioConfiguration> scenarioRegistry,
            ExpressionEvaluatorService evaluatorService) {
        this.scenarioRegistry = scenarioRegistry;
        this.evaluatorService = evaluatorService;
    }

    /**
     * Get a scenario by ID from the scenario registry.
     *
     * <p>This method retrieves a scenario by its ID and validates that it is enabled.
     * Disabled scenarios cannot be executed directly by ID.</p>
     *
     * @param scenarioId The scenario ID to look up
     * @return The scenario configuration
     * @throws IllegalArgumentException if scenario not found or is disabled
     * @throws IllegalStateException if scenario registry is not initialized
     */
    public ScenarioConfiguration getScenario(String scenarioId) {
        if (this.scenarioRegistry == null) {
            throw new IllegalStateException("Scenario registry is not initialized");
        }

        ScenarioConfiguration scenario = this.scenarioRegistry.get(scenarioId);

        if (scenario == null) {
            throw new IllegalArgumentException(
                "Scenario not found: " + scenarioId + ". " +
                "Available scenarios: " + this.scenarioRegistry.keySet()
            );
        }

        // Check if scenario is enabled
        if (!EnabledFilter.isEnabled(scenario)) {
            throw new IllegalArgumentException(
                "Scenario '" + scenarioId + "' is disabled and cannot be executed. " +
                "Enable the scenario in the registry or use a different scenario."
            );
        }

        return scenario;
    }

    /**
     * Find the first matching scenario based on classification rules.
     * Iterates through all scenarios in the registry and evaluates their classification rules
     * against the provided input data using SpEL expressions.
     *
     * <p>Only enabled scenarios are considered for matching. Disabled scenarios are skipped
     * during classification-based routing.</p>
     *
     * @param inputData The input data to match against classification rules
     * @return The first matching enabled scenario, or null if no match found
     */
    public ScenarioConfiguration findMatchingScenario(
            Map<String, Object> inputData) {

        if (this.scenarioRegistry == null || this.scenarioRegistry.isEmpty()) {
            logger.warn("Scenario registry is empty - no scenarios to match");
            return null;
        }

        logger.debug("Evaluating {} scenarios for classification match", this.scenarioRegistry.size());

        for (ScenarioConfiguration scenario : this.scenarioRegistry.values()) {
            // Skip disabled scenarios
            if (!EnabledFilter.isEnabled(scenario)) {
                logger.debug("Scenario {} is disabled - skipping", scenario.getScenarioId());
                continue;
            }

            if (scenario.hasClassificationRule()) {
                logger.debug("Evaluating classification rule for scenario: {}", scenario.getScenarioId());

                if (matchesClassificationRule(scenario, inputData, this.evaluatorService)) {
                    logger.info("Found matching scenario: {} ({})",
                        scenario.getScenarioId(), scenario.getClassificationRuleDescription());
                    return scenario;
                }
            } else {
                logger.debug("Scenario {} has no classification rule - skipping", scenario.getScenarioId());
            }
        }

        logger.warn("No matching scenario found for input data");
        return null;
    }

    // ========================================
    // Classification Rule Evaluation
    // ========================================

    /**
     * Evaluates the classification rule of a scenario against the provided data
     * using the given evaluator service for context creation.
     *
     * <p>Uses SpEL (Spring Expression Language) to evaluate the classification rule condition
     * against a Map of data. Data fields are accessible directly by name (e.g., tradeId, amount)
     * or as variables with # prefix (e.g., #tradeId, #amount).</p>
     *
     * @param scenario the scenario whose classification rule to evaluate
     * @param data the data to evaluate against (Map&lt;String, Object&gt;)
     * @param evaluatorService the service to use for creating the evaluation context
     * @return true if the classification rule matches, false otherwise
     */
    public static boolean matchesClassificationRule(ScenarioConfiguration scenario,
                                                     Map<String, Object> data,
                                                     ExpressionEvaluatorService evaluatorService) {
        if (!scenario.hasClassificationRule()) {
            return false;
        }

        if (data == null) {
            logger.debug("Cannot evaluate classification rule against null data for scenario: {}",
                    scenario.getScenarioId());
            return false;
        }

        try {
            Expression expression = parser.parseExpression(scenario.getClassificationRuleCondition());
            StandardEvaluationContext context = evaluatorService.createEvaluationContext(data);

            Boolean result = expression.getValue(context, Boolean.class);
            return result != null && result;

        } catch (Exception e) {
            logger.error("Failed to evaluate classification rule for scenario '{}': {}",
                    scenario.getScenarioId(), e.getMessage());
            return false;
        }
    }

    /**
     * Evaluates the classification rule of a scenario against the provided data
     * using a self-contained evaluation context.
     *
     * <p>Uses SpEL (Spring Expression Language) to evaluate the classification rule condition
     * against a Map of data. Data fields are accessible via bracket notation (e.g., ['tradeId'])
     * or as variables with # prefix (e.g., #tradeId).</p>
     *
     * <p>This method creates its own evaluation context with {@link MapPropertyAccessor}
     * and is suitable for standalone evaluation without an {@link ExpressionEvaluatorService}.</p>
     *
     * @param scenario the scenario whose classification rule to evaluate
     * @param data the data to evaluate against (Map&lt;String, Object&gt;)
     * @return true if the classification rule matches, false otherwise
     */
    public static boolean matchesClassificationRule(ScenarioConfiguration scenario,
                                                     Map<String, Object> data) {
        if (!scenario.hasClassificationRule()) {
            return false;
        }

        if (data == null) {
            logger.debug("Cannot evaluate classification rule against null data for scenario: {}",
                    scenario.getScenarioId());
            return false;
        }

        try {
            Expression expression = parser.parseExpression(scenario.getClassificationRuleCondition());
            StandardEvaluationContext context = new StandardEvaluationContext(data);
            context.addPropertyAccessor(new MapPropertyAccessor());
            // Add map entries as variables for #fieldName access
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }

            Boolean result = expression.getValue(context, Boolean.class);
            return result != null && result;

        } catch (Exception e) {
            logger.error("Failed to evaluate classification rule for scenario '{}': {}",
                    scenario.getScenarioId(), e.getMessage());
            return false;
        }
    }
}
