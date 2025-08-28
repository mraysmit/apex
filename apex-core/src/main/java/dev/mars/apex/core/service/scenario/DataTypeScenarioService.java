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


import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing data type scenarios and routing data records to appropriate
 * processing pipelines based on their type and business context.
 * 
 * This service implements the scenario-based configuration pattern that associates
 * data record types (e.g., OtcOption, CommoditySwap) with their complete processing
 * pipelines including rules, enrichments, and datasets.
 * 
 * CORE FUNCTIONALITY:
 * - Load scenario configurations from YAML files
 * - Route data records to appropriate scenarios based on type
 * - Provide scenario-specific processing configurations
 * - Cache scenario configurations for performance
 * - Support multiple scenarios per data type for different contexts
 * 
 * USAGE EXAMPLE:
 * ```java
 * DataTypeScenarioService scenarioService = new DataTypeScenarioService();
 * scenarioService.loadScenarios("config/data-type-scenarios.yaml");
 * 
 * // Route an OTC Option to its processing scenario
 * OtcOption option = new OtcOption(...);
 * ScenarioConfiguration scenario = scenarioService.getScenarioForData(option);
 * 
 * // Get processing pipeline for the scenario
 * ProcessingPipeline pipeline = scenario.getProcessingPipeline();
 * ```
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class DataTypeScenarioService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataTypeScenarioService.class);
    
    // Cache for loaded scenario configurations
    private final Map<String, ScenarioConfiguration> scenarioCache = new ConcurrentHashMap<>();
    
    // Mapping from data type to scenario IDs
    private final Map<String, List<String>> dataTypeToScenarios = new ConcurrentHashMap<>();
    
    // Routing configuration
    private RoutingConfiguration routingConfig;
    
    // Configuration loader
    private final YamlConfigurationLoader configLoader;
    
    public DataTypeScenarioService() {
        this.configLoader = new YamlConfigurationLoader();
    }
    
    /**
     * Loads scenario configurations from a registry YAML file.
     *
     * @param registryPath path to the scenario registry configuration file
     * @throws Exception if configuration loading fails
     */
    public void loadScenarios(String registryPath) throws Exception {
        logger.info("Loading scenario registry from: {}", registryPath);

        try {
            // Load the registry YAML configuration
            Map<String, Object> registryConfig = configLoader.loadAsMap(registryPath);

            // Parse scenario registry section
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scenarioRegistry = (List<Map<String, Object>>) registryConfig.get("scenario-registry");

            if (scenarioRegistry != null) {
                for (Map<String, Object> registryEntry : scenarioRegistry) {
                    String scenarioId = (String) registryEntry.get("scenario-id");
                    String configFile = (String) registryEntry.get("config-file");

                    if (scenarioId != null && configFile != null) {
                        logger.debug("Loading scenario '{}' from file: {}", scenarioId, configFile);
                        ScenarioConfiguration scenario = loadIndividualScenario(configFile);
                        if (scenario != null) {
                            registerScenario(scenario);
                        }
                    }
                }
            }

            // Parse routing configuration if present
            @SuppressWarnings("unchecked")
            Map<String, Object> routingData = (Map<String, Object>) registryConfig.get("routing");
            if (routingData != null) {
                this.routingConfig = parseRoutingConfiguration(routingData);
            }

            logger.info("Successfully loaded {} scenarios from registry", scenarioCache.size());

        } catch (Exception e) {
            logger.error("Failed to load scenario registry from: {}", registryPath, e);
            throw new RuntimeException("Scenario registry loading failed", e);
        }
    }

    /**
     * Loads an individual scenario configuration from its YAML file.
     *
     * @param configFile path to the individual scenario configuration file
     * @return parsed scenario configuration or null if loading fails
     */
    private ScenarioConfiguration loadIndividualScenario(String configFile) {
        try {
            logger.debug("Loading individual scenario from: {}", configFile);

            // Load the individual scenario YAML configuration
            Map<String, Object> config = configLoader.loadAsMap(configFile);

            // Parse scenario section
            @SuppressWarnings("unchecked")
            Map<String, Object> scenarioData = (Map<String, Object>) config.get("scenario");

            if (scenarioData != null) {
                ScenarioConfiguration scenario = parseScenarioConfiguration(scenarioData);

                // Also parse metadata from the file level
                @SuppressWarnings("unchecked")
                Map<String, Object> fileMetadata = (Map<String, Object>) config.get("metadata");
                if (fileMetadata != null && scenario.getMetadata() == null) {
                    scenario.setMetadata(fileMetadata);
                }

                logger.debug("Successfully loaded scenario: {}", scenario.getScenarioId());
                return scenario;
            } else {
                logger.warn("No 'scenario' section found in file: {}", configFile);
                return null;
            }

        } catch (Exception e) {
            logger.error("Failed to load individual scenario from: {}", configFile, e);
            return null;
        }
    }
    
    /**
     * Gets the appropriate scenario configuration for a data record.
     * 
     * @param data the data record to route
     * @return scenario configuration or null if no matching scenario found
     */
    public ScenarioConfiguration getScenarioForData(Object data) {
        if (data == null) {
            return null;
        }
        
        String dataType = determineDataType(data);
        logger.debug("Determining scenario for data type: {}", dataType);
        
        // Try direct data type mapping first
        List<String> scenarioIds = dataTypeToScenarios.get(dataType);
        if (scenarioIds != null && !scenarioIds.isEmpty()) {
            // For now, return the first matching scenario
            // In future, could implement more sophisticated routing logic
            String scenarioId = scenarioIds.get(0);
            ScenarioConfiguration scenario = scenarioCache.get(scenarioId);
            logger.debug("Found scenario '{}' for data type '{}'", scenarioId, dataType);
            return scenario;
        }
        
        // Try routing rules if direct mapping fails
        if (routingConfig != null) {
            String scenarioId = routingConfig.routeData(data, dataType);
            if (scenarioId != null) {
                ScenarioConfiguration scenario = scenarioCache.get(scenarioId);
                logger.debug("Routed to scenario '{}' via routing rules for data type '{}'", scenarioId, dataType);
                return scenario;
            }
        }
        
        // Try default scenario
        if (routingConfig != null && routingConfig.getDefaultScenario() != null) {
            ScenarioConfiguration defaultScenario = scenarioCache.get(routingConfig.getDefaultScenario());
            logger.debug("Using default scenario '{}' for data type '{}'", routingConfig.getDefaultScenario(), dataType);
            return defaultScenario;
        }
        
        logger.warn("No scenario found for data type: {}", dataType);
        return null;
    }
    
    /**
     * Gets a scenario configuration by ID.
     * 
     * @param scenarioId the scenario identifier
     * @return scenario configuration or null if not found
     */
    public ScenarioConfiguration getScenario(String scenarioId) {
        return scenarioCache.get(scenarioId);
    }
    
    /**
     * Gets all available scenario IDs.
     * 
     * @return set of scenario identifiers
     */
    public Set<String> getAvailableScenarios() {
        return new HashSet<>(scenarioCache.keySet());
    }
    
    /**
     * Gets all data types that have associated scenarios.
     * 
     * @return set of data type names
     */
    public Set<String> getSupportedDataTypes() {
        return new HashSet<>(dataTypeToScenarios.keySet());
    }
    
    /**
     * Registers a scenario configuration.
     * 
     * @param scenario the scenario to register
     */
    private void registerScenario(ScenarioConfiguration scenario) {
        String scenarioId = scenario.getScenarioId();
        scenarioCache.put(scenarioId, scenario);
        
        // Register data type mappings
        for (String dataType : scenario.getDataTypes()) {
            dataTypeToScenarios.computeIfAbsent(dataType, k -> new ArrayList<>()).add(scenarioId);
        }
        
        logger.debug("Registered scenario '{}' for data types: {}", scenarioId, scenario.getDataTypes());
    }
    
    /**
     * Determines the data type identifier for a data record.
     * 
     * @param data the data record
     * @return data type identifier
     */
    private String determineDataType(Object data) {
        if (data == null) {
            return "null";
        }
        
        Class<?> dataClass = data.getClass();
        
        // Try full class name first
        String fullClassName = dataClass.getName();
        if (dataTypeToScenarios.containsKey(fullClassName)) {
            return fullClassName;
        }
        
        // Try simple class name
        String simpleClassName = dataClass.getSimpleName();
        if (dataTypeToScenarios.containsKey(simpleClassName)) {
            return simpleClassName;
        }
        
        // Check if data has a type field
        try {
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) data;
                Object typeField = dataMap.get("dataType");
                if (typeField != null) {
                    return typeField.toString();
                }
            }
        } catch (Exception e) {
            // Ignore and continue with class-based detection
        }
        
        // Return simple class name as fallback
        return simpleClassName;
    }
    
    /**
     * Parses a scenario configuration from YAML data.
     *
     * @param scenarioData the YAML scenario data
     * @return parsed scenario configuration
     */
    private ScenarioConfiguration parseScenarioConfiguration(Map<String, Object> scenarioData) {
        ScenarioConfiguration scenario = new ScenarioConfiguration();

        scenario.setScenarioId((String) scenarioData.get("scenario-id"));
        scenario.setName((String) scenarioData.get("name"));
        scenario.setDescription((String) scenarioData.get("description"));

        // Parse data types
        @SuppressWarnings("unchecked")
        List<String> dataTypes = (List<String>) scenarioData.get("data-types");
        if (dataTypes != null) {
            scenario.setDataTypes(dataTypes);
        }

        // Parse rule configurations (references to existing rule files)
        @SuppressWarnings("unchecked")
        List<String> ruleConfigurations = (List<String>) scenarioData.get("rule-configurations");
        if (ruleConfigurations != null) {
            scenario.setRuleConfigurations(ruleConfigurations);
        }

        return scenario;
    }

    
    /**
     * Parses routing configuration.
     */
    private RoutingConfiguration parseRoutingConfiguration(Map<String, Object> routingData) {
        RoutingConfiguration routing = new RoutingConfiguration();
        
        routing.setStrategy((String) routingData.get("strategy"));
        routing.setDefaultScenario((String) routingData.get("default-scenario"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rulesData = (List<Map<String, Object>>) routingData.get("rules");
        if (rulesData != null) {
            List<RoutingRule> rules = new ArrayList<>();
            for (Map<String, Object> ruleData : rulesData) {
                RoutingRule rule = new RoutingRule();
                rule.setCondition((String) ruleData.get("condition"));
                rule.setTargetScenario((String) ruleData.get("target-scenario"));
                rules.add(rule);
            }
            routing.setRules(rules);
        }
        
        return routing;
    }
}
