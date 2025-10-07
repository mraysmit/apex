package dev.mars.apex.core.service.scenario;

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


import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Configuration class representing a complete data type processing scenario.
 *
 * A scenario defines the complete processing pipeline for a specific data type,
 * including validation rules, enrichment configurations, and associated datasets.
 * This enables systematic routing of different data types through appropriate
 * processing pipelines.
 *
 * CONFIGURATION STRUCTURE:
 * - Scenario identification and metadata
 * - Data types this scenario applies to (backward compatibility)
 * - Classification rules for Map-based data routing (new)
 * - Processing pipeline configuration (legacy rule-configurations or new processing-stages)
 * - Rules and rule chains to execute
 * - Enrichment configurations
 * - Associated datasets
 *
 * STAGE-BASED PROCESSING:
 * - Explicit processing stages with dependencies and failure policies
 * - Backward compatibility with legacy rule-configurations
 * - Enhanced monitoring and control over processing pipeline
 *
 * CLASSIFICATION-BASED ROUTING:
 * - SpEL-based classification rules for Map<String, Object> data
 * - Embedded classification rules in scenario files (Option B)
 * - Validation ensures either classification-rule OR data-types exists
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ScenarioConfiguration {

    private static final Logger logger = Logger.getLogger(ScenarioConfiguration.class.getName());
    private static final ExpressionParser parser = new SpelExpressionParser();

    private String scenarioId;
    private String name;
    private String description;
    private List<String> dataTypes;
    private List<String> ruleConfigurations; // Legacy support
    private List<ScenarioStage> processingStages; // NEW: Stage-based configuration
    private Map<String, Object> metadata;

    // NEW: Classification rule support for Map-based data routing
    private String classificationRuleCondition;
    private String classificationRuleDescription;
    
    // Constructors
    public ScenarioConfiguration() {
        this.processingStages = new ArrayList<>();
    }

    public ScenarioConfiguration(String scenarioId, String name, List<String> dataTypes, List<String> ruleConfigurations) {
        this();
        this.scenarioId = scenarioId;
        this.name = name;
        this.dataTypes = dataTypes;
        this.ruleConfigurations = ruleConfigurations;
    }

    /**
     * Factory method to create a scenario with stage-based configuration.
     *
     * @param scenarioId the scenario ID
     * @param name the scenario name
     * @param dataTypes the data types this scenario applies to
     * @param processingStages the processing stages
     * @return a new ScenarioConfiguration with stage-based configuration
     */
    public static ScenarioConfiguration withStages(String scenarioId, String name, List<String> dataTypes, List<ScenarioStage> processingStages) {
        ScenarioConfiguration scenario = new ScenarioConfiguration();
        scenario.scenarioId = scenarioId;
        scenario.name = name;
        scenario.dataTypes = dataTypes;
        scenario.processingStages = processingStages != null ? new ArrayList<>(processingStages) : new ArrayList<>();
        return scenario;
    }
    
    // Getters and Setters
    public String getScenarioId() {
        return scenarioId;
    }
    
    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getDataTypes() {
        return dataTypes;
    }
    
    public void setDataTypes(List<String> dataTypes) {
        this.dataTypes = dataTypes;
    }

    /**
     * Gets rule configurations with backward compatibility support.
     *
     * If processing stages are defined, returns the config files from stages
     * sorted by execution order. Otherwise returns legacy rule configurations.
     *
     * @return list of rule configuration file paths
     */
    public List<String> getRuleConfigurations() {
        if (hasStageConfiguration()) {
            return processingStages.stream()
                .sorted(Comparator.comparingInt(ScenarioStage::getExecutionOrder))
                .map(ScenarioStage::getConfigFile)
                .collect(Collectors.toList());
        }
        return ruleConfigurations;
    }

    /**
     * Sets legacy rule configurations.
     *
     * @deprecated Use setProcessingStages for new stage-based configuration
     */
    @Deprecated
    public void setRuleConfigurations(List<String> ruleConfigurations) {
        this.ruleConfigurations = ruleConfigurations;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Gets the classification rule condition.
     *
     * @return the SpEL expression for classification or null if not set
     */
    public String getClassificationRuleCondition() {
        return classificationRuleCondition;
    }

    /**
     * Sets the classification rule condition.
     *
     * @param classificationRuleCondition the SpEL expression for classification
     */
    public void setClassificationRuleCondition(String classificationRuleCondition) {
        this.classificationRuleCondition = classificationRuleCondition;
    }

    /**
     * Gets the classification rule description.
     *
     * @return the description of the classification rule or null if not set
     */
    public String getClassificationRuleDescription() {
        return classificationRuleDescription;
    }

    /**
     * Sets the classification rule description.
     *
     * @param classificationRuleDescription the description of the classification rule
     */
    public void setClassificationRuleDescription(String classificationRuleDescription) {
        this.classificationRuleDescription = classificationRuleDescription;
    }

    /**
     * Gets the processing stages configuration.
     *
     * @return list of processing stages or empty list if not configured
     */
    public List<ScenarioStage> getProcessingStages() {
        return processingStages != null ? new ArrayList<>(processingStages) : new ArrayList<>();
    }

    /**
     * Sets the processing stages configuration.
     *
     * @param processingStages list of processing stages
     */
    public void setProcessingStages(List<ScenarioStage> processingStages) {
        this.processingStages = processingStages != null ? new ArrayList<>(processingStages) : new ArrayList<>();
    }

    /**
     * Adds a processing stage to the scenario.
     *
     * @param stage the stage to add
     */
    public void addProcessingStage(ScenarioStage stage) {
        if (processingStages == null) {
            processingStages = new ArrayList<>();
        }
        processingStages.add(stage);
    }

    /**
     * Gets processing stages sorted by execution order.
     *
     * @return list of stages sorted by execution order
     */
    public List<ScenarioStage> getStagesByExecutionOrder() {
        if (processingStages == null) {
            return new ArrayList<>();
        }
        return processingStages.stream()
            .sorted(Comparator.comparingInt(ScenarioStage::getExecutionOrder))
            .collect(Collectors.toList());
    }

    /**
     * Gets a processing stage by name.
     *
     * @param stageName the name of the stage to find
     * @return the stage or null if not found
     */
    public ScenarioStage getStageByName(String stageName) {
        if (processingStages == null || stageName == null) {
            return null;
        }
        return processingStages.stream()
            .filter(stage -> stageName.equals(stage.getStageName()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Checks if this scenario has stage-based configuration.
     *
     * @return true if processing stages are configured
     */
    public boolean hasStageConfiguration() {
        return processingStages != null && !processingStages.isEmpty();
    }

    /**
     * Checks if this scenario uses legacy rule-configurations only.
     *
     * @return true if only legacy configuration is used
     */
    public boolean isLegacyConfiguration() {
        return !hasStageConfiguration() && ruleConfigurations != null && !ruleConfigurations.isEmpty();
    }
    
    // Utility methods
    
    /**
     * Checks if this scenario applies to the given data type.
     * 
     * @param dataType the data type to check
     * @return true if this scenario applies to the data type
     */
    public boolean appliesToDataType(String dataType) {
        return dataTypes != null && dataTypes.contains(dataType);
    }
    
    /**
     * Gets the business domain from metadata.
     * 
     * @return business domain or null if not set
     */
    public String getBusinessDomain() {
        return metadata != null ? (String) metadata.get("business-domain") : null;
    }
    
    /**
     * Gets the risk category from metadata.
     * 
     * @return risk category or null if not set
     */
    public String getRiskCategory() {
        return metadata != null ? (String) metadata.get("risk-category") : null;
    }
    
    /**
     * Gets the processing SLA in milliseconds from metadata.
     * 
     * @return processing SLA in ms or null if not set
     */
    public Integer getProcessingSlaMs() {
        Object sla = metadata != null ? metadata.get("processing-sla-ms") : null;
        return sla instanceof Number ? ((Number) sla).intValue() : null;
    }
    
    /**
     * Gets the owner from metadata.
     *
     * @return owner or null if not set
     */
    public String getOwner() {
        return metadata != null ? (String) metadata.get("owner") : null;
    }

    /**
     * Checks if this scenario has a classification rule defined.
     *
     * @return true if classification rule condition is set and not empty
     */
    public boolean hasClassificationRule() {
        return classificationRuleCondition != null && !classificationRuleCondition.trim().isEmpty();
    }

    /**
     * Evaluates the classification rule against the provided data.
     *
     * Uses SpEL (Spring Expression Language) to evaluate the classification rule condition
     * against a Map of data. The data is available in the expression as #data variable.
     *
     * @param data the data to evaluate against (Map<String, Object>)
     * @return true if the classification rule matches, false otherwise
     */
    public boolean matchesClassificationRule(Map<String, Object> data) {
        if (!hasClassificationRule()) {
            return false;
        }

        if (data == null) {
            logger.fine("Cannot evaluate classification rule against null data for scenario: " + scenarioId);
            return false;
        }

        try {
            Expression expression = parser.parseExpression(classificationRuleCondition);
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("data", data);

            Boolean result = expression.getValue(context, Boolean.class);
            return result != null && result;

        } catch (Exception e) {
            logger.warning("Failed to evaluate classification rule for scenario '" + scenarioId + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Validates the scenario configuration.
     *
     * A scenario MUST have either:
     * - classification-rule (for Map-based data routing), OR
     * - data-types (for Java class-based routing - backward compatibility)
     *
     * A scenario without either is invalid because it can never be selected.
     *
     * @throws IllegalStateException if neither classification-rule nor data-types are present
     */
    public void validate() {
        boolean hasClassificationRule = hasClassificationRule();
        boolean hasDataTypes = dataTypes != null && !dataTypes.isEmpty();

        if (!hasClassificationRule && !hasDataTypes) {
            throw new IllegalStateException(
                "Scenario '" + scenarioId + "' must have either 'classification-rule' or 'data-types'. " +
                "A scenario without classification cannot be selected.");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ScenarioConfiguration{");
        sb.append("scenarioId='").append(scenarioId).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", dataTypes=").append(dataTypes);

        if (hasStageConfiguration()) {
            sb.append(", processingStages=").append(processingStages.size()).append(" stages");
            sb.append(", stageNames=").append(
                processingStages.stream()
                    .map(ScenarioStage::getStageName)
                    .collect(Collectors.toList())
            );
        } else if (ruleConfigurations != null) {
            sb.append(", ruleConfigurations=").append(ruleConfigurations);
        }

        sb.append(", businessDomain='").append(getBusinessDomain()).append('\'');
        sb.append(", configType=").append(hasStageConfiguration() ? "stage-based" : "legacy");
        sb.append('}');

        return sb.toString();
    }
}

/**
 * Configuration for routing data to scenarios.
 */
class RoutingConfiguration {
    private String strategy;
    private String defaultScenario;
    private List<RoutingRule> rules;
    
    // Getters and Setters
    public String getStrategy() {
        return strategy;
    }
    
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
    
    public String getDefaultScenario() {
        return defaultScenario;
    }
    
    public void setDefaultScenario(String defaultScenario) {
        this.defaultScenario = defaultScenario;
    }
    
    public List<RoutingRule> getRules() {
        return rules;
    }
    
    public void setRules(List<RoutingRule> rules) {
        this.rules = rules;
    }
    
    /**
     * Routes data to a scenario based on routing rules.
     * 
     * @param data the data object
     * @param dataType the determined data type
     * @return scenario ID or null if no rule matches
     */
    public String routeData(Object data, String dataType) {
        if (rules == null) {
            return null;
        }
        
        for (RoutingRule rule : rules) {
            if (rule.matches(data, dataType)) {
                return rule.getTargetScenario();
            }
        }
        
        return null;
    }
}

/**
 * Individual routing rule.
 */
class RoutingRule {
    private String condition;
    private String targetScenario;
    
    // Getters and Setters
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public String getTargetScenario() {
        return targetScenario;
    }
    
    public void setTargetScenario(String targetScenario) {
        this.targetScenario = targetScenario;
    }
    
    /**
     * Checks if this routing rule matches the given data.
     * 
     * @param data the data object
     * @param dataType the determined data type
     * @return true if the rule matches
     */
    public boolean matches(Object data, String dataType) {
        if (condition == null) {
            return false;
        }
        
        // Simple condition evaluation - in a real implementation,
        // this would use the SpEL expression evaluator
        if (condition.contains("class.simpleName")) {
            String expectedType = extractSimpleClassName(condition);
            return dataType.equals(expectedType);
        }
        
        if (condition.contains("dataType")) {
            String expectedType = extractDataType(condition);
            return dataType.equals(expectedType);
        }
        
        // Add more condition evaluation logic as needed
        return false;
    }
    
    private String extractSimpleClassName(String condition) {
        // Extract class name from condition like "#data.class.simpleName == 'OtcOption'"
        int start = condition.indexOf("'") + 1;
        int end = condition.lastIndexOf("'");
        return start > 0 && end > start ? condition.substring(start, end) : "";
    }
    
    private String extractDataType(String condition) {
        // Extract data type from condition like "#dataType == 'CommoditySwap'"
        int start = condition.indexOf("'") + 1;
        int end = condition.lastIndexOf("'");
        return start > 0 && end > start ? condition.substring(start, end) : "";
    }
}
