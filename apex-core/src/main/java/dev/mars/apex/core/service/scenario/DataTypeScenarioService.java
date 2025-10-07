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
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.util.TestAwareLogger;
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
    
    // Cache for loaded scenario configurations (LinkedHashMap preserves insertion order for classification priority)
    // Synchronized for thread-safety while maintaining insertion order
    protected final Map<String, ScenarioConfiguration> scenarioCache = Collections.synchronizedMap(new LinkedHashMap<>());
    
    // Mapping from data type to scenario IDs
    private final Map<String, List<String>> dataTypeToScenarios = new ConcurrentHashMap<>();
    
    // Routing configuration
    private RoutingConfiguration routingConfig;
    
    // Configuration loader
    private final YamlConfigurationLoader configLoader;

    // Stage executor for stage-aware processing
    private final ScenarioStageExecutor stageExecutor;

    // Rule factory for creating engines
    private final YamlRuleFactory ruleFactory;

    // Enrichment service for processing enrichments
    private final EnrichmentService enrichmentService;

    public DataTypeScenarioService() {
        this.configLoader = new YamlConfigurationLoader();
        this.ruleFactory = new YamlRuleFactory();
        this.enrichmentService = createDefaultEnrichmentService();
        this.stageExecutor = new ScenarioStageExecutor(configLoader, ruleFactory, enrichmentService);
    }

    public DataTypeScenarioService(YamlConfigurationLoader configLoader, YamlRuleFactory ruleFactory) {
        this.configLoader = configLoader != null ? configLoader : new YamlConfigurationLoader();
        this.ruleFactory = ruleFactory != null ? ruleFactory : new YamlRuleFactory();
        this.enrichmentService = createDefaultEnrichmentService();
        this.stageExecutor = new ScenarioStageExecutor(this.configLoader, this.ruleFactory, this.enrichmentService);
    }

    public DataTypeScenarioService(YamlConfigurationLoader configLoader, YamlRuleFactory ruleFactory, EnrichmentService enrichmentService) {
        this.configLoader = configLoader != null ? configLoader : new YamlConfigurationLoader();
        this.ruleFactory = ruleFactory != null ? ruleFactory : new YamlRuleFactory();
        this.enrichmentService = enrichmentService != null ? enrichmentService : createDefaultEnrichmentService();
        this.stageExecutor = new ScenarioStageExecutor(this.configLoader, this.ruleFactory, this.enrichmentService);
    }

    private EnrichmentService createDefaultEnrichmentService() {
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        return new EnrichmentService(serviceRegistry, expressionEvaluator);
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

            // Parse scenario registry section (using 'scenarios' per APEX YAML spec)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scenarioRegistry = (List<Map<String, Object>>) registryConfig.get("scenarios");

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
     * Processes data through a scenario using stage-aware processing if available,
     * otherwise falls back to legacy rule-based processing.
     *
     * @param data the data to process
     * @return processing result (ScenarioExecutionResult for stage-based, RuleResult for legacy)
     */
    public Object processData(Object data) {
        ScenarioConfiguration scenario = getScenarioForData(data);
        if (scenario == null) {
            TestAwareLogger.warn(logger, "No scenario found for data type: {}", determineDataType(data));
            return RuleResult.noRules();
        }

        return processDataWithScenario(data, scenario);
    }

    /**
     * Processes data through a specific scenario using stage-aware processing if available,
     * otherwise falls back to legacy rule-based processing.
     *
     * @param data the data to process
     * @param scenario the scenario configuration to use
     * @return processing result (ScenarioExecutionResult for stage-based, RuleResult for legacy)
     */
    public Object processDataWithScenario(Object data, ScenarioConfiguration scenario) {
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario configuration cannot be null");
        }

        logger.info("Processing data with scenario '{}' (stage-based: {})",
                   scenario.getScenarioId(), scenario.hasStageConfiguration());

        if (scenario.hasStageConfiguration()) {
            // Use new stage-based processing
            return stageExecutor.executeStages(scenario, data);
        } else {
            // Use legacy rule-based processing
            return processDataLegacy(data, scenario);
        }
    }

    /**
     * Processes data using stage-aware processing.
     *
     * @param data the data to process
     * @param scenarioId the scenario ID to use
     * @return stage execution result
     * @throws IllegalArgumentException if scenario not found or doesn't have stage configuration
     */
    public ScenarioExecutionResult processDataWithStages(Object data, String scenarioId) {
        ScenarioConfiguration scenario = getScenario(scenarioId);
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario not found: " + scenarioId);
        }

        if (!scenario.hasStageConfiguration()) {
            throw new IllegalArgumentException("Scenario '" + scenarioId + "' does not have stage configuration");
        }

        return stageExecutor.executeStages(scenario, data);
    }
    
    /**
     * Registers a scenario configuration.
     *
     * @param scenario the scenario to register
     */
    void registerScenario(ScenarioConfiguration scenario) {
        String scenarioId = scenario.getScenarioId();
        scenarioCache.put(scenarioId, scenario);

        // Register data type mappings (only if data types are specified)
        List<String> dataTypes = scenario.getDataTypes();
        if (dataTypes != null && !dataTypes.isEmpty()) {
            for (String dataType : dataTypes) {
                dataTypeToScenarios.computeIfAbsent(dataType, k -> new ArrayList<>()).add(scenarioId);
            }
            logger.debug("Registered scenario '{}' for data types: {}", scenarioId, dataTypes);
        } else {
            logger.debug("Registered scenario '{}' with no data type associations", scenarioId);
        }
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

        // Parse classification rule (new Map-based data routing)
        @SuppressWarnings("unchecked")
        Map<String, Object> classificationRule = (Map<String, Object>) scenarioData.get("classification-rule");
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

        // Parse rule configurations (references to existing rule files)
        @SuppressWarnings("unchecked")
        List<String> ruleConfigurations = (List<String>) scenarioData.get("rule-configurations");
        if (ruleConfigurations != null) {
            scenario.setRuleConfigurations(ruleConfigurations);
        }

        // Parse processing stages if present (new stage-based configuration)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> processingStages = (List<Map<String, Object>>) scenarioData.get("processing-stages");
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

            scenario = ScenarioConfiguration.withStages(scenario.getScenarioId(), scenario.getName(),
                                                       scenario.getDataTypes(), stages);
            scenario.setDescription(description);
            scenario.setClassificationRuleCondition(classificationCondition);
            scenario.setClassificationRuleDescription(classificationDescription);
        }

        return scenario;
    }

    /**
     * Parses a scenario stage configuration from YAML data.
     *
     * @param stageData the YAML stage data
     * @return parsed scenario stage or null if parsing fails
     */
    private ScenarioStage parseScenarioStage(Map<String, Object> stageData) {
        try {
            String stageName = (String) stageData.get("stage-name");
            String configFile = (String) stageData.get("config-file");
            Integer executionOrder = (Integer) stageData.get("execution-order");
            String failurePolicy = (String) stageData.get("failure-policy");
            Boolean required = (Boolean) stageData.get("required");

            if (stageName == null || configFile == null || executionOrder == null) {
                TestAwareLogger.warn(logger, "Missing required stage fields: stage-name, config-file, or execution-order");
                return null;
            }

            ScenarioStage stage = new ScenarioStage(stageName, configFile, executionOrder);

            if (failurePolicy != null) {
                stage.setFailurePolicy(failurePolicy);
            }

            if (required != null) {
                stage.setRequired(required);
            }

            // Parse dependencies
            @SuppressWarnings("unchecked")
            List<String> dependsOn = (List<String>) stageData.get("depends-on");
            if (dependsOn != null) {
                for (String dependency : dependsOn) {
                    stage.addDependency(dependency);
                }
            }

            // Parse stage metadata
            @SuppressWarnings("unchecked")
            Map<String, Object> stageMetadata = (Map<String, Object>) stageData.get("stage-metadata");
            if (stageMetadata != null) {
                stage.setStageMetadata(stageMetadata);
            }

            return stage;

        } catch (Exception e) {
            TestAwareLogger.error(logger, "Error parsing scenario stage: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Processes data using legacy rule-based processing.
     *
     * @param data the data to process
     * @param scenario the scenario configuration
     * @return rule execution result
     */
    private RuleResult processDataLegacy(Object data, ScenarioConfiguration scenario) {
        logger.info("Processing data with legacy rule-based approach for scenario '{}'", scenario.getScenarioId());

        try {
            List<String> ruleConfigurations = scenario.getRuleConfigurations();
            if (ruleConfigurations == null || ruleConfigurations.isEmpty()) {
                TestAwareLogger.warn(logger, "No rule configurations found for scenario '{}'", scenario.getScenarioId());
                return RuleResult.noRules();
            }

            // Load and merge all rule configurations
            YamlRuleConfiguration mergedConfig = null;
            for (String configFile : ruleConfigurations) {
                YamlRuleConfiguration config = configLoader.loadFromFile(configFile);
                if (mergedConfig == null) {
                    mergedConfig = config;
                } else {
                    // Simple merge - in production might need more sophisticated merging
                    logger.debug("Merging rule configuration from: {}", configFile);
                }
            }

            if (mergedConfig == null) {
                TestAwareLogger.warn(logger, "Failed to load any rule configurations for scenario '{}'", scenario.getScenarioId());
                return RuleResult.error("No rule configurations loaded", "No configurations could be loaded", "ERROR", null);
            }

            // Create rules engine and execute
            RulesEngine engine = new RulesEngine(ruleFactory.createRulesEngineConfiguration(mergedConfig));

            // Create facts map
            Map<String, Object> facts = new HashMap<>();
            facts.put("data", data);
            facts.put("scenarioId", scenario.getScenarioId());
            facts.put("dataType", determineDataType(data));

            return engine.evaluate(mergedConfig, facts);

        } catch (Exception e) {
            TestAwareLogger.error(logger, "Error in legacy processing for scenario '{}': {}", scenario.getScenarioId(), e.getMessage(), e);
            return RuleResult.error("Legacy processing error", e.getMessage(), "ERROR", null);
        }
    }

    /**
     * Gets the appropriate scenario for Map-based data using classification rules.
     *
     * Evaluates classification rules embedded in scenario files (Option B).
     * Returns the first scenario whose classification rule matches the data.
     *
     * @param data the Map data to classify
     * @return matching scenario or null if no match found
     */
    public ScenarioConfiguration getScenarioForMapData(Map<String, Object> data) {
        if (data == null) {
            TestAwareLogger.warn(logger, "Cannot get scenario for null data");
            return null;
        }

        logger.debug("Evaluating classification rules for Map data with {} fields", data.size());

        // Evaluate all scenarios with classification rules
        for (ScenarioConfiguration scenario : scenarioCache.values()) {
            // Only evaluate scenarios with classification rules (Option B)
            // Scenarios with only data-types use class-based routing
            if (scenario.hasClassificationRule()) {
                if (scenario.matchesClassificationRule(data)) {
                    logger.info("Matched scenario '{}' via embedded classification rule",
                               scenario.getScenarioId());
                    return scenario;
                }
            }
        }

        logger.debug("No scenario matched for Map data");
        return null;
    }

    /**
     * Processes Map-based data through the matched scenario's processing stages.
     *
     * This is the main entry point for Map-based data processing:
     * 1. Finds matching scenario using classification rules
     * 2. Executes the scenario's processing stages
     * 3. Returns the execution result
     *
     * @param data the Map data to process
     * @return execution result or error if no scenario matches
     */
    public ScenarioExecutionResult processMapData(Map<String, Object> data) {
        if (data == null) {
            TestAwareLogger.warn(logger, "Cannot process null data");
            ScenarioExecutionResult result = new ScenarioExecutionResult("unknown");
            result.addWarning("Cannot process null data");
            result.setTerminated(true);
            return result;
        }

        // Find matching scenario
        ScenarioConfiguration scenario = getScenarioForMapData(data);

        if (scenario == null) {
            TestAwareLogger.warn(logger, "No scenario found for Map data with fields: {}", data.keySet());
            ScenarioExecutionResult result = new ScenarioExecutionResult("unknown");
            result.addWarning("No scenario matched the provided data");
            result.setTerminated(true);
            return result;
        }

        // Execute the scenario's processing stages
        logger.info("Processing Map data through scenario: {}", scenario.getScenarioId());
        return stageExecutor.executeStages(scenario, data);
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
