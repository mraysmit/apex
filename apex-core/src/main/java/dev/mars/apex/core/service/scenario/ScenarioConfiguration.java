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


import java.util.List;
import java.util.Map;

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
 * - Data types this scenario applies to
 * - Processing pipeline configuration
 * - Rules and rule chains to execute
 * - Enrichment configurations
 * - Associated datasets
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ScenarioConfiguration {
    
    private String scenarioId;
    private String name;
    private String description;
    private List<String> dataTypes;
    private List<String> ruleConfigurations;
    private Map<String, Object> metadata;
    
    // Constructors
    public ScenarioConfiguration() {}

    public ScenarioConfiguration(String scenarioId, String name, List<String> dataTypes, List<String> ruleConfigurations) {
        this.scenarioId = scenarioId;
        this.name = name;
        this.dataTypes = dataTypes;
        this.ruleConfigurations = ruleConfigurations;
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

    public List<String> getRuleConfigurations() {
        return ruleConfigurations;
    }

    public void setRuleConfigurations(List<String> ruleConfigurations) {
        this.ruleConfigurations = ruleConfigurations;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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
    
    @Override
    public String toString() {
        return "ScenarioConfiguration{" +
                "scenarioId='" + scenarioId + '\'' +
                ", name='" + name + '\'' +
                ", dataTypes=" + dataTypes +
                ", ruleConfigurations=" + ruleConfigurations +
                ", businessDomain='" + getBusinessDomain() + '\'' +
                '}';
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
