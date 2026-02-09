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
package dev.mars.apex.core.engine.config.scenario;

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import dev.mars.apex.core.service.scenario.ScenarioStage;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages scenario registry operations including lookup, classification matching,
 * and YAML parsing for scenario configurations.
 *
 * <p>This manager handles three core responsibilities:</p>
 * <ul>
 *   <li><b>Scenario Lookup:</b> Retrieves scenarios by ID with validation (enabled check)</li>
 *   <li><b>Classification Matching:</b> Finds matching scenarios via SpEL-based classification rules</li>
 *   <li><b>YAML Parsing:</b> Parses scenario configurations from YAML with legacy/modern support</li>
 * </ul>
 *
 * <p><b>Design Pattern:</b> Extracted from RulesEngine to separate scenario registry concerns
 * from core rule execution. Supports both legacy (data-types, rule-configurations) and modern
 * (classification-rule, processing-stages) scenario formats.</p>
 *
 * @since 2026-01-22
 * @see ScenarioConfiguration
 * @see ScenarioStage
 */
public class ScenarioRegistryManager {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioRegistryManager.class);

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
        if (!scenario.isEnabled()) {
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
            if (!scenario.isEnabled()) {
                logger.debug("Scenario {} is disabled - skipping", scenario.getScenarioId());
                continue;
            }

            if (scenario.hasClassificationRule()) {
                logger.debug("Evaluating classification rule for scenario: {}", scenario.getScenarioId());

                if (scenario.matchesClassificationRule(inputData, this.evaluatorService)) {
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

    /**
     * Parse scenario configuration from YamlRuleConfiguration.
     *
     * @param yamlConfig The YAML configuration containing scenario data
     * @return Parsed ScenarioConfiguration
     * @throws IllegalStateException if scenario data is missing or invalid
     */
    @SuppressWarnings("unchecked")
    public ScenarioConfiguration parseScenarioFromYaml(
            YamlRuleConfiguration yamlConfig) {

        if (!yamlConfig.hasScenario()) {
            throw new IllegalStateException("YAML configuration does not contain a scenario section");
        }

        Object scenarioData = yamlConfig.getScenarioData();
        if (!(scenarioData instanceof Map)) {
            throw new IllegalStateException("Scenario data must be a Map");
        }

        Map<String, Object> scenarioMap = (Map<String, Object>) scenarioData;
        return parseScenarioConfiguration(scenarioMap);
    }

    /**
     * Parse scenario configuration from YAML data map.
     *
     * @param scenarioData The scenario data map from YAML
     * @return Parsed ScenarioConfiguration
     */
    @SuppressWarnings("unchecked")
    public ScenarioConfiguration parseScenarioConfiguration(
            Map<String, Object> scenarioData) {

        ScenarioConfiguration scenario = new ScenarioConfiguration();

        scenario.setScenarioId((String) scenarioData.get("scenario-id"));
        scenario.setName((String) scenarioData.get("name"));
        scenario.setDescription((String) scenarioData.get("description"));

        // Parse data types (legacy)
        List<String> dataTypes = (List<String>) scenarioData.get("data-types");
        if (dataTypes != null) {
            scenario.setDataTypes(dataTypes);
        }

        // Parse classification rule (modern Map-based routing)
        Map<String, Object> classificationRule =
            (Map<String, Object>) scenarioData.get("classification-rule");
        if (classificationRule != null) {
            String condition = (String) classificationRule.get("condition");
            String description = (String) classificationRule.get("description");

            if (condition != null) {
                scenario.setClassificationRuleCondition(condition);
            }
            if (description != null) {
                scenario.setClassificationRuleDescription(description);
            }
        }

        // Parse rule configurations (legacy)
        List<String> ruleConfigurations =
            (List<String>) scenarioData.get("rule-configurations");
        if (ruleConfigurations != null) {
            scenario.setRuleConfigurations(ruleConfigurations);
        }

        // Parse processing stages (modern stage-based configuration)
        List<Map<String, Object>> processingStages =
            (List<Map<String, Object>>) scenarioData.get("processing-stages");
        if (processingStages != null) {
            List<ScenarioStage> stages = new ArrayList<>();
            for (Map<String, Object> stageData : processingStages) {
                ScenarioStage stage = parseScenarioStage(stageData);
                if (stage != null) {
                    stages.add(stage);
                }
            }

            // Preserve classification rule fields when creating stage-based scenario
            String classificationCondition = scenario.getClassificationRuleCondition();
            String classificationDescription = scenario.getClassificationRuleDescription();
            String description = scenario.getDescription();

            scenario = ScenarioConfiguration.withStages(
                scenario.getScenarioId(), scenario.getName(), scenario.getDataTypes(), stages);
            scenario.setDescription(description);
            scenario.setClassificationRuleCondition(classificationCondition);
            scenario.setClassificationRuleDescription(classificationDescription);
        }

        return scenario;
    }

    /**
     * Parse a scenario stage from YAML data.
     *
     * @param stageData The stage data map from YAML
     * @return Parsed ScenarioStage or null if parsing fails
     */
    @SuppressWarnings("unchecked")
    public ScenarioStage parseScenarioStage(
            Map<String, Object> stageData) {

        try {
            String stageName = (String) stageData.get("stage-name");
            String configFile = (String) stageData.get("config-file");
            Integer executionOrder = (Integer) stageData.get("execution-order");
            String failurePolicy = (String) stageData.get("failure-policy");
            String condition = (String) stageData.get("condition");
            Boolean required = (Boolean) stageData.get("required");

            if (stageName == null || configFile == null || executionOrder == null) {
                logger.warn("Missing required stage fields: stage-name, config-file, or execution-order");
                return null;
            }

            ScenarioStage stage =
                new ScenarioStage(stageName, configFile, executionOrder);

            if (failurePolicy != null) {
                stage.setFailurePolicy(failurePolicy);
            }

            if (condition != null) {
                stage.setCondition(condition);
            }

            if (required != null) {
                stage.setRequired(required);
            }

            // Parse dependencies
            List<String> dependsOn = (List<String>) stageData.get("depends-on");
            if (dependsOn != null) {
                for (String dependency : dependsOn) {
                    stage.addDependency(dependency);
                }
            }

            // Parse stage metadata
            Map<String, Object> stageMetadata =
                (Map<String, Object>) stageData.get("stage-metadata");
            if (stageMetadata != null) {
                stage.setStageMetadata(stageMetadata);
            }

            return stage;

        } catch (Exception e) {
            logger.error("Error parsing scenario stage: {}", e.getMessage());
            return null;
        }
    }
}
