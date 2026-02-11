package dev.mars.apex.engine.scenario;

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

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import dev.mars.apex.core.service.scenario.ScenarioStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parser for converting YAML scenario configurations into domain objects.
 * 
 * <p>This is the <b>single canonical parser</b> for all scenario YAML data. Both the
 * {@code ScenarioEvaluationManager} (YAML-based evaluation) and the
 * {@code ScenarioRegistryLoader} (registry-based loading) delegate to this class.</p>
 * 
 * <p>Supports both legacy and modern scenario formats:</p>
 * <ul>
 *   <li><b>Legacy:</b> data-types, rule-configurations, stage-id, rule-configuration</li>
 *   <li><b>Modern:</b> processing-stages, classification-rule (String or Map), metadata, enabled</li>
 * </ul>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-22
 */
public class ScenarioParser {
    
    private static final Logger logger = LoggerFactory.getLogger(ScenarioParser.class);
    
    /**
     * Parse scenario configuration from YAML configuration object.
     * 
     * @param yamlConfig The YAML configuration containing scenario data
     * @return Parsed ScenarioConfiguration
     * @throws IllegalStateException if YAML does not contain a scenario section or data is invalid
     */
    @SuppressWarnings("unchecked")
    public ScenarioConfiguration parseFromYaml(YamlRuleConfiguration yamlConfig) {
        if (yamlConfig == null) {
            throw new IllegalArgumentException("YAML configuration cannot be null");
        }
        
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
     * <p>This method handles both legacy and modern scenario configurations,
     * ensuring backward compatibility while supporting new features like
     * classification-based routing and stage-based processing.</p>
     *
     * <p>Supported YAML fields:</p>
     * <ul>
     *   <li>{@code scenario-id}, {@code name}, {@code description} — identity</li>
     *   <li>{@code data-types} — legacy type list</li>
     *   <li>{@code classification-rule} — String (condition only) or Map (condition + description)</li>
     *   <li>{@code rule-configurations} — legacy file list</li>
     *   <li>{@code processing-stages} — modern stage-based configuration</li>
     *   <li>{@code metadata} — arbitrary metadata map</li>
     *   <li>{@code enabled} — Boolean or String, defaults to true</li>
     * </ul>
     *
     * @param scenarioData The scenario data map from YAML
     * @return Parsed ScenarioConfiguration
     */
    @SuppressWarnings("unchecked")
    public ScenarioConfiguration parseScenarioConfiguration(Map<String, Object> scenarioData) {
        if (scenarioData == null || scenarioData.isEmpty()) {
            throw new IllegalArgumentException("Scenario data cannot be null or empty");
        }

        ScenarioConfiguration scenario = new ScenarioConfiguration();

        scenario.setScenarioId((String) scenarioData.get("scenario-id"));
        scenario.setName((String) scenarioData.get("name"));
        scenario.setDescription((String) scenarioData.get("description"));

        // Parse data types (legacy)
        List<String> dataTypes = (List<String>) scenarioData.get("data-types");
        if (dataTypes != null) {
            scenario.setDataTypes(dataTypes);
        }

        // Parse classification rule - can be either a string or a map with condition/description
        Object classificationRuleObj = scenarioData.get("classification-rule");
        if (classificationRuleObj != null) {
            if (classificationRuleObj instanceof String) {
                // Simple string format (condition only)
                scenario.setClassificationRuleCondition((String) classificationRuleObj);
            } else if (classificationRuleObj instanceof Map) {
                // Object format with condition and optional description
                Map<String, Object> classificationRuleMap = (Map<String, Object>) classificationRuleObj;
                String condition = (String) classificationRuleMap.get("condition");
                String ruleDescription = (String) classificationRuleMap.get("description");

                if (condition != null) {
                    scenario.setClassificationRuleCondition(condition);
                }
                if (ruleDescription != null) {
                    scenario.setClassificationRuleDescription(ruleDescription);
                }
            }
        }

        // Parse metadata
        Map<String, Object> metadata = (Map<String, Object>) scenarioData.get("metadata");
        if (metadata != null) {
            scenario.setMetadata(metadata);
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
            scenario.setProcessingStages(stages);
        }

        // Parse enabled flag (default: true)
        parseBoolean(scenarioData.get("enabled")).ifPresent(scenario::setEnabled);

        return scenario;
    }

    /**
     * Parse a scenario stage from YAML data.
     * 
     * <p>Stages represent individual processing units within a scenario, each with
     * its own configuration file, execution order, and failure handling policy.</p>
     *
     * <p>Supports legacy key aliases: {@code stage-id} for {@code stage-name},
     * {@code rule-configuration} for {@code config-file}.</p>
     *
     * @param stageData The stage data map from YAML
     * @return Parsed ScenarioStage or null if parsing fails
     */
    @SuppressWarnings("unchecked")
    public ScenarioStage parseScenarioStage(Map<String, Object> stageData) {
        if (stageData == null) {
            logger.warn("Stage data is null");
            return null;
        }

        try {
            ScenarioStage stage = new ScenarioStage();

            // Set stage name - try both "stage-name" (modern) and "stage-id" (legacy)
            String stageName = (String) stageData.get("stage-name");
            if (stageName == null) {
                stageName = (String) stageData.get("stage-id");
            }
            stage.setStageName(stageName);

            // Set config file - try both "config-file" (modern) and "rule-configuration" (legacy)
            String configFile = (String) stageData.get("config-file");
            if (configFile == null) {
                configFile = (String) stageData.get("rule-configuration");
            }
            stage.setConfigFile(configFile);

            // Set failure policy
            stage.setFailurePolicy((String) stageData.get("failure-policy"));

            // Set condition (optional SpEL expression for conditional execution)
            stage.setCondition((String) stageData.get("condition"));

            // Set execution order (Integer or String)
            Object executionOrder = stageData.get("execution-order");
            if (executionOrder != null) {
                if (executionOrder instanceof Integer) {
                    stage.setExecutionOrder((Integer) executionOrder);
                } else if (executionOrder instanceof String) {
                    try {
                        stage.setExecutionOrder(Integer.parseInt((String) executionOrder));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid execution-order value: {}", executionOrder);
                    }
                }
            }

            // Set required flag (Boolean or String)
            parseBoolean(stageData.get("required")).ifPresent(stage::setRequired);

            // Set enabled flag (Boolean or String, default: true)
            parseBoolean(stageData.get("enabled")).ifPresent(stage::setEnabled);

            // Set description - try both direct "description" and nested "stage-metadata.description"
            String description = (String) stageData.get("description");
            Map<String, Object> stageMetadata =
                (Map<String, Object>) stageData.get("stage-metadata");
            if (description == null && stageMetadata != null) {
                description = (String) stageMetadata.get("description");
            }
            if (description != null) {
                stage.setDescription(description);
            }

            // Set stage metadata
            if (stageMetadata != null) {
                stage.setStageMetadata(stageMetadata);
            }

            // Parse dependencies
            List<String> dependencies = (List<String>) stageData.get("depends-on");
            if (dependencies != null) {
                stage.setDependsOn(dependencies);
            }

            return stage;

        } catch (Exception e) {
            logger.error("Error parsing scenario stage: {}", e.getMessage());
            logger.debug("Full exception details:", e);
            return null;
        }
    }

    /**
     * Parse a Boolean or String value into an Optional Boolean.
     *
     * @param value The value to parse (Boolean, String, or null)
     * @return Optional containing the parsed boolean, or empty if null
     */
    private static java.util.Optional<Boolean> parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return java.util.Optional.of((Boolean) value);
        }
        if (value instanceof String) {
            return java.util.Optional.of(Boolean.parseBoolean((String) value));
        }
        return java.util.Optional.empty();
    }
}
