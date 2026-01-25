package dev.mars.apex.core.engine.config.scenario;

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

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
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
 * <p>This class handles the transformation of raw YAML data structures into
 * strongly-typed {@link ScenarioConfiguration} objects with proper validation
 * and error handling.</p>
 * 
 * <p>Supports both legacy and modern scenario formats:</p>
 * <ul>
 *   <li><b>Legacy:</b> data-types, rule-configurations</li>
 *   <li><b>Modern:</b> processing-stages, classification-rule</li>
 * </ul>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.2.0
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
     * <p>Stages represent individual processing units within a scenario, each with
     * its own configuration file, execution order, and failure handling policy.</p>
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
