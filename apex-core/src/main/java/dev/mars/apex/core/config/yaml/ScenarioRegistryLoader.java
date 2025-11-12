package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import dev.mars.apex.core.service.scenario.ScenarioStage;
import dev.mars.apex.core.util.RulesEngineLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
 * Loader for scenario registry YAML files.
 * 
 * <p>This class loads a scenario registry file that contains references to multiple
 * scenario configuration files. It parses the registry, loads all referenced scenario
 * files, and returns a map of scenario configurations indexed by scenario ID.</p>
 * 
 * <p><b>Registry YAML Structure:</b></p>
 * <pre>
 * metadata:
 *   id: "scenario-registry"
 *   type: "scenario-registry"
 * 
 * scenarios:
 *   - scenario-id: "basic-trade-processing"
 *     config-file: "scenarios/basic-trade-processing.yaml"
 *     business-domain: "Trading"
 *   
 *   - scenario-id: "complex-trade-processing"
 *     config-file: "scenarios/complex-trade-processing.yaml"
 *     business-domain: "Trading"
 * 
 * routing:
 *   strategy: "classification-based"
 *   default-scenario: "basic-trade-processing"
 * </pre>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 * @see ScenarioConfiguration
 */
public class ScenarioRegistryLoader {
    
    private static final RulesEngineLogger logger = new RulesEngineLogger(ScenarioRegistryLoader.class);
    
    private final YamlConfigurationLoader configLoader;
    
    /**
     * Create a new ScenarioRegistryLoader with default configuration loader.
     */
    public ScenarioRegistryLoader() {
        this.configLoader = new YamlConfigurationLoader();
    }
    
    /**
     * Create a new ScenarioRegistryLoader with a custom configuration loader.
     * 
     * @param configLoader The YAML configuration loader to use
     */
    public ScenarioRegistryLoader(YamlConfigurationLoader configLoader) {
        this.configLoader = configLoader;
    }
    
    /**
     * Load a scenario registry and all referenced scenario configuration files.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Loads the registry YAML file</li>
     *   <li>Parses the 'scenarios' section</li>
     *   <li>Loads each referenced scenario configuration file</li>
     *   <li>Returns a map of ScenarioConfiguration objects indexed by scenario-id</li>
     * </ol>
     * 
     * <p><b>Example Usage:</b></p>
     * <pre>
     * ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
     * Map&lt;String, ScenarioConfiguration&gt; scenarios = loader.loadRegistry("registry.yaml");
     * 
     * ScenarioConfiguration scenario = scenarios.get("basic-trade-processing");
     * </pre>
     * 
     * @param registryPath The path to the scenario registry YAML file
     * @return A map of scenario configurations indexed by scenario-id
     * @throws YamlConfigurationException if the registry file cannot be loaded,
     *         if any referenced scenario file cannot be loaded,
     *         or if the registry structure is invalid
     */
    public Map<String, ScenarioConfiguration> loadRegistry(String registryPath) throws YamlConfigurationException {
        logger.info("Loading scenario registry from: {}", registryPath);
        
        try {
            // Validate registry file exists
            Path registryFilePath = Paths.get(registryPath);
            if (!Files.exists(registryFilePath)) {
                throw new YamlConfigurationException("Scenario registry file not found: " + registryPath);
            }
            
            // Load the registry YAML configuration as a Map
            Map<String, Object> registryConfig = configLoader.loadAsMap(registryPath);
            
            // Validate registry metadata
            validateRegistryMetadata(registryConfig, registryPath);
            
            // Parse scenarios section
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scenarioRegistry = (List<Map<String, Object>>) registryConfig.get("scenarios");
            
            if (scenarioRegistry == null || scenarioRegistry.isEmpty()) {
                throw new YamlConfigurationException(
                    "Scenario registry file does not contain a 'scenarios' section or it is empty: " + registryPath
                );
            }
            
            // Load all referenced scenario files
            Map<String, ScenarioConfiguration> scenarios = new LinkedHashMap<>();
            Path registryDir = registryFilePath.getParent();
            
            for (Map<String, Object> registryEntry : scenarioRegistry) {
                String scenarioId = (String) registryEntry.get("scenario-id");
                String configFile = (String) registryEntry.get("config-file");
                
                if (scenarioId == null || scenarioId.trim().isEmpty()) {
                    logger.warn("Skipping registry entry with missing or empty scenario-id");
                    continue;
                }
                
                if (configFile == null || configFile.trim().isEmpty()) {
                    throw new YamlConfigurationException(
                        "Scenario '" + scenarioId + "' in registry has missing or empty config-file"
                    );
                }
                
                logger.debug("Loading scenario '{}' from file: {}", scenarioId, configFile);
                
                // Resolve config file path relative to registry directory
                String resolvedConfigPath = resolveConfigFilePath(configFile, registryDir);
                
                // Load the scenario configuration
                ScenarioConfiguration scenario = loadScenarioFromFile(resolvedConfigPath);
                
                // Validate that scenario-id matches
                if (scenario.getScenarioId() != null && !scenario.getScenarioId().equals(scenarioId)) {
                    logger.warn(
                        "Scenario ID mismatch: registry specifies '{}' but config file contains '{}'. Using registry ID.",
                        scenarioId, scenario.getScenarioId()
                    );
                }
                
                // Ensure scenario has the correct ID from registry
                scenario.setScenarioId(scenarioId);
                
                // Store in map
                scenarios.put(scenarioId, scenario);
                logger.debug("Successfully loaded scenario: {}", scenarioId);
            }
            
            logger.info("Successfully loaded {} scenarios from registry: {}", scenarios.size(), registryPath);
            return scenarios;
            
        } catch (YamlConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new YamlConfigurationException("Failed to load scenario registry from: " + registryPath, e);
        }
    }
    
    /**
     * Validate registry metadata.
     * 
     * @param registryConfig The registry configuration map
     * @param registryPath The registry file path (for error messages)
     * @throws YamlConfigurationException if metadata is invalid
     */
    private void validateRegistryMetadata(Map<String, Object> registryConfig, String registryPath) 
            throws YamlConfigurationException {
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) registryConfig.get("metadata");
        
        if (metadata == null) {
            logger.warn("Scenario registry file does not contain metadata section: {}", registryPath);
            return;
        }
        
        String type = (String) metadata.get("type");
        if (type != null && !"scenario-registry".equals(type)) {
            logger.warn(
                "Registry metadata type is '{}' but expected 'scenario-registry': {}", 
                type, registryPath
            );
        }
    }
    
    /**
     * Resolve config file path relative to registry directory.
     *
     * @param configFile The config file path from registry
     * @param registryDir The directory containing the registry file
     * @return The resolved absolute path
     */
    private String resolveConfigFilePath(String configFile, Path registryDir) {
        Path configPath = Paths.get(configFile);

        // If absolute path, use as-is
        if (configPath.isAbsolute()) {
            return configFile;
        }

        // Check if the config file path already exists as-is (it might be a project-relative path)
        if (Files.exists(configPath)) {
            return configFile;
        }

        // If relative path, resolve relative to registry directory
        if (registryDir != null) {
            Path resolvedPath = registryDir.resolve(configFile);
            if (Files.exists(resolvedPath)) {
                return resolvedPath.toString();
            }
        }

        // Fallback to config file as-is (will fail later with proper error message)
        return configFile;
    }
    
    /**
     * Load a scenario configuration from a YAML file.
     * 
     * @param configFile The path to the scenario configuration file
     * @return The loaded ScenarioConfiguration
     * @throws YamlConfigurationException if the file cannot be loaded or parsed
     */
    private ScenarioConfiguration loadScenarioFromFile(String configFile) throws YamlConfigurationException {
        try {
            // Load the YAML file as a Map
            Map<String, Object> config = configLoader.loadAsMap(configFile);
            
            // Look for 'scenario' section
            @SuppressWarnings("unchecked")
            Map<String, Object> scenarioData = (Map<String, Object>) config.get("scenario");
            
            if (scenarioData != null) {
                ScenarioConfiguration scenario = parseScenarioConfiguration(scenarioData);
                
                // Also parse metadata from the file level if scenario doesn't have it
                @SuppressWarnings("unchecked")
                Map<String, Object> fileMetadata = (Map<String, Object>) config.get("metadata");
                if (fileMetadata != null && scenario.getMetadata() == null) {
                    scenario.setMetadata(fileMetadata);
                }
                
                return scenario;
            } else {
                throw new YamlConfigurationException(
                    "Scenario configuration file does not contain a 'scenario' section: " + configFile
                );
            }
            
        } catch (YamlConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new YamlConfigurationException("Failed to load scenario from file: " + configFile, e);
        }
    }
    
    /**
     * Parse scenario configuration from YAML data map.
     * This follows the same pattern as DataTypeScenarioService.parseScenarioConfiguration.
     * 
     * @param scenarioData The scenario data map from YAML
     * @return Parsed ScenarioConfiguration
     */
    @SuppressWarnings("unchecked")
    private ScenarioConfiguration parseScenarioConfiguration(Map<String, Object> scenarioData) {
        ScenarioConfiguration scenario = new ScenarioConfiguration();
        
        scenario.setScenarioId((String) scenarioData.get("scenario-id"));
        scenario.setName((String) scenarioData.get("name"));
        scenario.setDescription((String) scenarioData.get("description"));
        
        // Parse data types
        List<String> dataTypes = (List<String>) scenarioData.get("data-types");
        if (dataTypes != null) {
            scenario.setDataTypes(dataTypes);
        }
        
        // Parse classification rule - can be either a string or an object with condition/description
        Object classificationRuleObj = scenarioData.get("classification-rule");
        if (classificationRuleObj != null) {
            if (classificationRuleObj instanceof String) {
                // Simple string format
                scenario.setClassificationRuleCondition((String) classificationRuleObj);
            } else if (classificationRuleObj instanceof Map) {
                // Object format with condition and description
                Map<String, Object> classificationRuleMap = (Map<String, Object>) classificationRuleObj;
                String condition = (String) classificationRuleMap.get("condition");
                if (condition != null) {
                    scenario.setClassificationRuleCondition(condition);
                }
            }
        }
        
        // Parse metadata
        Map<String, Object> metadata = (Map<String, Object>) scenarioData.get("metadata");
        if (metadata != null) {
            scenario.setMetadata(metadata);
        }
        
        // Parse processing stages (modern approach)
        List<Map<String, Object>> stagesData = (List<Map<String, Object>>) scenarioData.get("processing-stages");
        if (stagesData != null) {
            List<ScenarioStage> stages = new ArrayList<>();
            for (Map<String, Object> stageData : stagesData) {
                ScenarioStage stage = parseScenarioStage(stageData);
                stages.add(stage);
            }
            scenario.setProcessingStages(stages);
        }
        
        // Parse rule configurations (legacy approach)
        List<String> ruleConfigurations = (List<String>) scenarioData.get("rule-configurations");
        if (ruleConfigurations != null) {
            scenario.setRuleConfigurations(ruleConfigurations);
        }
        
        return scenario;
    }
    
    /**
     * Parse a scenario stage from YAML data.
     *
     * @param stageData The stage data map from YAML
     * @return Parsed ScenarioStage
     */
    @SuppressWarnings("unchecked")
    private ScenarioStage parseScenarioStage(Map<String, Object> stageData) {
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

        // Detect and log if this is a component file
        if (configFile != null) {
            validateAndLogComponentFile(configFile, stageName);
        }

        // Set failure policy
        stage.setFailurePolicy((String) stageData.get("failure-policy"));

        // Set execution order
        Object executionOrder = stageData.get("execution-order");
        if (executionOrder != null) {
            if (executionOrder instanceof Integer) {
                stage.setExecutionOrder((Integer) executionOrder);
            } else if (executionOrder instanceof String) {
                try {
                    stage.setExecutionOrder(Integer.parseInt((String) executionOrder));
                } catch (NumberFormatException e) {
                    // Ignore invalid execution order
                }
            }
        }

        // Set required flag
        Object required = stageData.get("required");
        if (required != null) {
            if (required instanceof Boolean) {
                stage.setRequired((Boolean) required);
            } else if (required instanceof String) {
                stage.setRequired(Boolean.parseBoolean((String) required));
            }
        }

        // Set description - try both direct "description" and nested "stage-metadata.description"
        String description = (String) stageData.get("description");
        if (description == null) {
            Map<String, Object> stageMetadata = (Map<String, Object>) stageData.get("stage-metadata");
            if (stageMetadata != null) {
                description = (String) stageMetadata.get("description");
            }
        }
        if (description != null) {
            stage.setDescription(description);
        }

        // Parse dependencies
        List<String> dependencies = (List<String>) stageData.get("depends-on");
        if (dependencies != null) {
            stage.setDependsOn(dependencies);
        }

        return stage;
    }

    /**
     * Validate and log component file detection.
     *
     * <p>This method checks if a config file is a component file and logs the detection.
     * It also validates that the component file exists and is accessible.</p>
     *
     * @param configFile The config file path to check
     * @param stageName The stage name (for logging)
     */
    private void validateAndLogComponentFile(String configFile, String stageName) {
        try {
            // Check if this is a component file
            if (configLoader.isComponentFile(configFile)) {
                logger.info("Stage '{}' references a component file: {}", stageName, configFile);

                // Validate that the component file exists and is accessible
                // The isComponentFile() method already loads the file, so if we get here, it exists
                logger.debug("Component file '{}' validated successfully", configFile);
            } else {
                logger.debug("Stage '{}' references a regular config file: {}", stageName, configFile);
            }
        } catch (Exception e) {
            // Log warning but don't fail - the actual validation will happen at execution time
            logger.warn("Unable to validate config file '{}' for stage '{}': {}",
                       configFile, stageName, e.getMessage());
        }
    }
}

