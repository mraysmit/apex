package dev.mars.apex.core.config.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

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
 * Service for loading YAML configuration files into rule configuration objects.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Service for loading YAML configuration files into rule configuration objects.
 * This class handles the parsing and validation of YAML configuration files.
 */
public class YamlConfigurationLoader {
    
    private static final Logger LOGGER = Logger.getLogger(YamlConfigurationLoader.class.getName());
    
    private final ObjectMapper yamlMapper;
    
    /**
     * Constructor that initializes the YAML object mapper.
     */
    public YamlConfigurationLoader() {
        this.yamlMapper = createYamlMapper();
    }
    
    /**
     * Load configuration from a file path.
     * 
     * @param filePath The path to the YAML configuration file
     * @return The loaded configuration
     * @throws YamlConfigurationException if loading fails
     */
    public YamlRuleConfiguration loadFromFile(String filePath) throws YamlConfigurationException {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new YamlConfigurationException("Configuration file not found: " + filePath);
            }
            
            LOGGER.info("Loading YAML configuration from file: " + filePath);
            YamlRuleConfiguration config = yamlMapper.readValue(path.toFile(), YamlRuleConfiguration.class);
            
            validateConfiguration(config);
            LOGGER.info("Successfully loaded configuration: " + 
                       (config.getMetadata() != null ? config.getMetadata().getName() : "unnamed"));
            
            return config;
            
        } catch (IOException e) {
            throw new YamlConfigurationException("Failed to load configuration from file: " + filePath, e);
        }
    }
    
    /**
     * Load configuration from a File object.
     * 
     * @param file The YAML configuration file
     * @return The loaded configuration
     * @throws YamlConfigurationException if loading fails
     */
    public YamlRuleConfiguration loadFromFile(File file) throws YamlConfigurationException {
        try {
            if (!file.exists()) {
                throw new YamlConfigurationException("Configuration file not found: " + file.getAbsolutePath());
            }
            
            LOGGER.info("Loading YAML configuration from file: " + file.getAbsolutePath());
            YamlRuleConfiguration config = yamlMapper.readValue(file, YamlRuleConfiguration.class);
            
            validateConfiguration(config);
            LOGGER.info("Successfully loaded configuration: " + 
                       (config.getMetadata() != null ? config.getMetadata().getName() : "unnamed"));
            
            return config;
            
        } catch (IOException e) {
            throw new YamlConfigurationException("Failed to load configuration from file: " + file.getAbsolutePath(), e);
        }
    }
    
    /**
     * Load configuration from an InputStream (useful for classpath resources).
     * 
     * @param inputStream The input stream containing YAML configuration
     * @return The loaded configuration
     * @throws YamlConfigurationException if loading fails
     */
    public YamlRuleConfiguration loadFromStream(InputStream inputStream) throws YamlConfigurationException {
        try {
            LOGGER.info("Loading YAML configuration from input stream");
            YamlRuleConfiguration config = yamlMapper.readValue(inputStream, YamlRuleConfiguration.class);
            
            validateConfiguration(config);
            LOGGER.info("Successfully loaded configuration: " + 
                       (config.getMetadata() != null ? config.getMetadata().getName() : "unnamed"));
            
            return config;
            
        } catch (IOException e) {
            throw new YamlConfigurationException("Failed to load configuration from input stream", e);
        }
    }
    
    /**
     * Load configuration from a classpath resource.
     * 
     * @param resourcePath The classpath resource path
     * @return The loaded configuration
     * @throws YamlConfigurationException if loading fails
     */
    public YamlRuleConfiguration loadFromClasspath(String resourcePath) throws YamlConfigurationException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new YamlConfigurationException("Configuration resource not found: " + resourcePath);
            }
            
            LOGGER.info("Loading YAML configuration from classpath: " + resourcePath);
            return loadFromStream(inputStream);
            
        } catch (IOException e) {
            throw new YamlConfigurationException("Failed to load configuration from classpath: " + resourcePath, e);
        }
    }
    
    /**
     * Save configuration to a file.
     * 
     * @param configuration The configuration to save
     * @param filePath The target file path
     * @throws YamlConfigurationException if saving fails
     */
    public void saveToFile(YamlRuleConfiguration configuration, String filePath) throws YamlConfigurationException {
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            
            LOGGER.info("Saving YAML configuration to file: " + filePath);
            yamlMapper.writeValue(path.toFile(), configuration);
            LOGGER.info("Successfully saved configuration to: " + filePath);
            
        } catch (IOException e) {
            throw new YamlConfigurationException("Failed to save configuration to file: " + filePath, e);
        }
    }
    
    /**
     * Convert configuration to YAML string.
     * 
     * @param configuration The configuration to convert
     * @return YAML string representation
     * @throws YamlConfigurationException if conversion fails
     */
    public String toYamlString(YamlRuleConfiguration configuration) throws YamlConfigurationException {
        try {
            return yamlMapper.writeValueAsString(configuration);
        } catch (IOException e) {
            throw new YamlConfigurationException("Failed to convert configuration to YAML string", e);
        }
    }
    
    /**
     * Load raw YAML content as a Map for dependency analysis.
     *
     * @param filePath The path to the YAML file
     * @return The YAML content as a Map
     * @throws YamlConfigurationException if loading fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadAsMap(String filePath) throws YamlConfigurationException {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new YamlConfigurationException("Configuration file not found: " + filePath);
            }

            LOGGER.info("Loading YAML file as Map: " + filePath);
            Map<String, Object> yamlContent = yamlMapper.readValue(path.toFile(), Map.class);

            // Validate metadata if present
            validateMetadata(yamlContent, filePath);

            return yamlContent;

        } catch (IOException e) {
            throw new YamlConfigurationException("Failed to load YAML file as Map: " + filePath, e);
        }
    }

    /**
     * Validates basic metadata requirements for YAML files.
     *
     * @param yamlContent The loaded YAML content
     * @param filePath The file path for error reporting
     * @throws YamlConfigurationException if validation fails
     */
    @SuppressWarnings("unchecked")
    private void validateMetadata(Map<String, Object> yamlContent, String filePath) throws YamlConfigurationException {
        Object metadataObj = yamlContent.get("metadata");
        if (metadataObj == null) {
            LOGGER.warning("YAML file missing metadata section: " + filePath);
            return; // Don't fail for missing metadata, just warn
        }

        if (!(metadataObj instanceof Map)) {
            throw new YamlConfigurationException("Invalid metadata section in file: " + filePath + " - must be a map/object");
        }

        Map<String, Object> metadata = (Map<String, Object>) metadataObj;

        // Check for required 'type' field
        Object typeObj = metadata.get("type");
        if (typeObj == null) {
            throw new YamlConfigurationException("Missing required 'type' field in metadata for file: " + filePath);
        }

        if (!(typeObj instanceof String) || ((String) typeObj).trim().isEmpty()) {
            throw new YamlConfigurationException("Invalid 'type' field in metadata for file: " + filePath + " - must be a non-empty string");
        }

        String type = (String) typeObj;
        LOGGER.fine("Validated YAML file type '" + type + "' for file: " + filePath);
    }

    /**
     * Parse YAML string into configuration.
     *
     * @param yamlString The YAML string to parse
     * @return The parsed configuration
     * @throws YamlConfigurationException if parsing fails
     */
    public YamlRuleConfiguration fromYamlString(String yamlString) throws YamlConfigurationException {
        try {
            YamlRuleConfiguration config = yamlMapper.readValue(yamlString, YamlRuleConfiguration.class);
            validateConfiguration(config);
            return config;
        } catch (IOException e) {
            throw new YamlConfigurationException("Failed to parse YAML string", e);
        }
    }
    
    /**
     * Create and configure the YAML ObjectMapper.
     * 
     * @return Configured ObjectMapper for YAML processing
     */
    private ObjectMapper createYamlMapper() {
        YAMLFactory yamlFactory = new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR);
        
        ObjectMapper mapper = new ObjectMapper(yamlFactory);
        
        // Configure mapper for better handling of missing properties
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        
        return mapper;
    }
    
    /**
     * Validate the loaded configuration.
     *
     * @param config The configuration to validate
     * @throws YamlConfigurationException if validation fails
     */
    private void validateConfiguration(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config == null) {
            throw new YamlConfigurationException("Configuration is null");
        }

        // Step 1: Validate individual components
        validateRules(config);
        validateRuleGroups(config);
        validateCategories(config);
        validateDataSources(config);
        validateRuleChains(config);

        // Step 2: Validate cross-component references
        validateCrossComponentReferences(config);

        // Step 3: Validate for duplicates
        validateDuplicates(config);

        LOGGER.fine("Configuration validation completed successfully");
    }
    
    /**
     * Validate all rules in the configuration.
     */
    private void validateRules(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getRules() != null) {
            for (YamlRule rule : config.getRules()) {
                validateRule(rule);
            }
        }
    }

    /**
     * Validate a rule configuration.
     */
    private void validateRule(YamlRule rule) throws YamlConfigurationException {
        if (rule.getId() == null || rule.getId().trim().isEmpty()) {
            throw new YamlConfigurationException("Rule ID is required");
        }
        if (rule.getName() == null || rule.getName().trim().isEmpty()) {
            throw new YamlConfigurationException("Rule name is required for rule: " + rule.getId());
        }
        if (rule.getCondition() == null || rule.getCondition().trim().isEmpty()) {
            throw new YamlConfigurationException("Rule condition is required for rule: " + rule.getId());
        }
    }
    
    /**
     * Validate all rule groups in the configuration.
     */
    private void validateRuleGroups(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getRuleGroups() != null) {
            for (YamlRuleGroup group : config.getRuleGroups()) {
                validateRuleGroup(group);
            }
        }
    }

    /**
     * Validate a rule group configuration.
     */
    private void validateRuleGroup(YamlRuleGroup group) throws YamlConfigurationException {
        if (group.getId() == null || group.getId().trim().isEmpty()) {
            throw new YamlConfigurationException("Rule group ID is required");
        }
        if (group.getName() == null || group.getName().trim().isEmpty()) {
            throw new YamlConfigurationException("Rule group name is required for group: " + group.getId());
        }
    }
    
    /**
     * Validate all categories in the configuration.
     */
    private void validateCategories(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getCategories() != null) {
            for (YamlCategory category : config.getCategories()) {
                validateCategory(category);
            }
        }
    }

    /**
     * Validate a category configuration.
     */
    private void validateCategory(YamlCategory category) throws YamlConfigurationException {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new YamlConfigurationException("Category name is required");
        }
    }

    /**
     * Validate all data sources in the configuration.
     */
    private void validateDataSources(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getDataSources() != null) {
            for (YamlDataSource dataSource : config.getDataSources()) {
                validateDataSource(dataSource);
            }
        }
    }

    /**
     * Validate a data source configuration.
     */
    private void validateDataSource(YamlDataSource dataSource) throws YamlConfigurationException {
        if (dataSource.getName() == null || dataSource.getName().trim().isEmpty()) {
            throw new YamlConfigurationException("Data source name is required");
        }
        if (dataSource.getType() == null || dataSource.getType().trim().isEmpty()) {
            throw new YamlConfigurationException("Data source type is required for data source: " + dataSource.getName());
        }

        // Validate type-specific requirements
        validateDataSourceTypeSpecificRequirements(dataSource);
    }

    /**
     * Validate type-specific requirements for data sources.
     */
    private void validateDataSourceTypeSpecificRequirements(YamlDataSource dataSource) throws YamlConfigurationException {
        String type = dataSource.getType().toLowerCase();
        String name = dataSource.getName();

        switch (type) {
            case "postgresql":
            case "mysql":
            case "oracle":
            case "sqlserver":
                validateDatabaseDataSource(dataSource, name);
                break;
            case "rest-api":
                validateRestApiDataSource(dataSource, name);
                break;
            case "file-system":
                validateFileSystemDataSource(dataSource, name);
                break;
            // Other types are optional validation
        }
    }

    /**
     * Validate database data source requirements.
     */
    private void validateDatabaseDataSource(YamlDataSource dataSource, String name) throws YamlConfigurationException {
        Map<String, Object> connection = dataSource.getConnection();
        if (connection == null || connection.isEmpty()) {
            throw new YamlConfigurationException("Missing required connection configuration for database data source: " + name);
        }

        if (!connection.containsKey("host") || connection.get("host") == null) {
            throw new YamlConfigurationException("Missing required connection property 'host' for database data source: " + name);
        }
        if (!connection.containsKey("port") || connection.get("port") == null) {
            throw new YamlConfigurationException("Missing required connection property 'port' for database data source: " + name);
        }
        if (!connection.containsKey("database") || connection.get("database") == null) {
            throw new YamlConfigurationException("Missing required connection property 'database' for database data source: " + name);
        }
    }

    /**
     * Validate REST API data source requirements.
     */
    private void validateRestApiDataSource(YamlDataSource dataSource, String name) throws YamlConfigurationException {
        Map<String, Object> connection = dataSource.getConnection();
        if (connection == null || connection.isEmpty()) {
            throw new YamlConfigurationException("Missing required connection configuration for REST API data source: " + name);
        }

        if (!connection.containsKey("base-url") || connection.get("base-url") == null) {
            throw new YamlConfigurationException("Missing required property 'base-url' for REST API data source: " + name);
        }
    }

    /**
     * Validate file system data source requirements.
     */
    private void validateFileSystemDataSource(YamlDataSource dataSource, String name) throws YamlConfigurationException {
        Map<String, Object> connection = dataSource.getConnection();
        if (connection == null || connection.isEmpty()) {
            throw new YamlConfigurationException("Missing required connection configuration for file system data source: " + name);
        }

        if (!connection.containsKey("base-path") || connection.get("base-path") == null) {
            throw new YamlConfigurationException("Missing required property 'base-path' for file system data source: " + name);
        }
    }

    /**
     * Validate all rule chains in the configuration.
     */
    private void validateRuleChains(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getRuleChains() != null) {
            for (YamlRuleChain ruleChain : config.getRuleChains()) {
                validateRuleChain(ruleChain);
            }
        }
    }

    /**
     * Validate a rule chain configuration.
     */
    private void validateRuleChain(YamlRuleChain ruleChain) throws YamlConfigurationException {
        if (ruleChain.getId() == null || ruleChain.getId().trim().isEmpty()) {
            throw new YamlConfigurationException("Rule chain ID is required");
        }
        if (ruleChain.getName() == null || ruleChain.getName().trim().isEmpty()) {
            throw new YamlConfigurationException("Rule chain name is required for chain: " + ruleChain.getId());
        }
        if (ruleChain.getPattern() == null || ruleChain.getPattern().trim().isEmpty()) {
            throw new YamlConfigurationException("Rule chain pattern is required for chain: " + ruleChain.getId());
        }

        // Validate pattern-specific configuration
        validateRuleChainPattern(ruleChain);
    }

    /**
     * Validate pattern-specific rule chain configuration.
     */
    @SuppressWarnings("unchecked")
    private void validateRuleChainPattern(YamlRuleChain ruleChain) throws YamlConfigurationException {
        String pattern = ruleChain.getPattern();
        String chainId = ruleChain.getId();
        Map<String, Object> config = ruleChain.getConfiguration();

        if (config == null || config.isEmpty()) {
            throw new YamlConfigurationException("Rule chain configuration is required for pattern '" + pattern + "' in chain: " + chainId);
        }

        switch (pattern) {
            case "conditional-chaining":
                validateConditionalChainingPattern(config, chainId);
                break;
            case "sequential-dependency":
                validateSequentialDependencyPattern(config, chainId);
                break;
            case "result-based-routing":
                validateResultBasedRoutingPattern(config, chainId);
                break;
            case "accumulative-chaining":
                validateAccumulativeChainingPattern(config, chainId);
                break;
            case "complex-workflow":
                validateComplexWorkflowPattern(config, chainId);
                break;
            case "fluent-builder":
                validateFluentBuilderPattern(config, chainId);
                break;
            default:
                LOGGER.warning("Unknown rule chain pattern '" + pattern + "' for chain: " + chainId);
        }
    }

    /**
     * Validate conditional-chaining pattern configuration.
     */
    private void validateConditionalChainingPattern(Map<String, Object> config, String chainId) throws YamlConfigurationException {
        if (!config.containsKey("trigger-rule") || config.get("trigger-rule") == null) {
            throw new YamlConfigurationException("Missing required 'trigger-rule' for conditional-chaining pattern in chain: " + chainId);
        }
        if (!config.containsKey("conditional-rules") || config.get("conditional-rules") == null) {
            throw new YamlConfigurationException("Missing required 'conditional-rules' for conditional-chaining pattern in chain: " + chainId);
        }
    }

    /**
     * Validate sequential-dependency pattern configuration.
     */
    @SuppressWarnings("unchecked")
    private void validateSequentialDependencyPattern(Map<String, Object> config, String chainId) throws YamlConfigurationException {
        if (!config.containsKey("stages") || config.get("stages") == null) {
            throw new YamlConfigurationException("Missing required 'stages' for sequential-dependency pattern in chain: " + chainId);
        }

        Object stagesObj = config.get("stages");
        if (!(stagesObj instanceof List)) {
            throw new YamlConfigurationException("'stages' must be a list for sequential-dependency pattern in chain: " + chainId);
        }

        List<Object> stages = (List<Object>) stagesObj;
        if (stages.isEmpty()) {
            throw new YamlConfigurationException("'stages' cannot be empty for sequential-dependency pattern in chain: " + chainId);
        }
    }

    /**
     * Validate result-based-routing pattern configuration.
     */
    private void validateResultBasedRoutingPattern(Map<String, Object> config, String chainId) throws YamlConfigurationException {
        if (!config.containsKey("router-rule") || config.get("router-rule") == null) {
            throw new YamlConfigurationException("Missing required 'router-rule' for result-based-routing pattern in chain: " + chainId);
        }
        if (!config.containsKey("routes") || config.get("routes") == null) {
            throw new YamlConfigurationException("Missing required 'routes' for result-based-routing pattern in chain: " + chainId);
        }
    }

    /**
     * Validate accumulative-chaining pattern configuration.
     */
    private void validateAccumulativeChainingPattern(Map<String, Object> config, String chainId) throws YamlConfigurationException {
        if (!config.containsKey("accumulator-variable") || config.get("accumulator-variable") == null) {
            throw new YamlConfigurationException("Missing required 'accumulator-variable' for accumulative-chaining pattern in chain: " + chainId);
        }
        if (!config.containsKey("accumulation-rules") || config.get("accumulation-rules") == null) {
            throw new YamlConfigurationException("Missing required 'accumulation-rules' for accumulative-chaining pattern in chain: " + chainId);
        }
    }

    /**
     * Validate complex-workflow pattern configuration.
     */
    @SuppressWarnings("unchecked")
    private void validateComplexWorkflowPattern(Map<String, Object> config, String chainId) throws YamlConfigurationException {
        if (!config.containsKey("stages") || config.get("stages") == null) {
            throw new YamlConfigurationException("Missing required 'stages' for complex-workflow pattern in chain: " + chainId);
        }

        Object stagesObj = config.get("stages");
        if (!(stagesObj instanceof List)) {
            throw new YamlConfigurationException("'stages' must be a list for complex-workflow pattern in chain: " + chainId);
        }

        List<Object> stages = (List<Object>) stagesObj;
        if (stages.isEmpty()) {
            throw new YamlConfigurationException("'stages' cannot be empty for complex-workflow pattern in chain: " + chainId);
        }
    }

    /**
     * Validate fluent-builder pattern configuration.
     */
    private void validateFluentBuilderPattern(Map<String, Object> config, String chainId) throws YamlConfigurationException {
        if (!config.containsKey("root-rule") || config.get("root-rule") == null) {
            throw new YamlConfigurationException("Missing required 'root-rule' for fluent-builder pattern in chain: " + chainId);
        }
    }

    /**
     * Validate cross-component references in the configuration.
     */
    private void validateCrossComponentReferences(YamlRuleConfiguration config) throws YamlConfigurationException {
        // Build reference maps for validation
        Set<String> ruleIds = buildRuleIdSet(config);
        Set<String> dataSourceNames = buildDataSourceNameSet(config);

        // Validate rule group references
        validateRuleGroupReferences(config, ruleIds);

        // Note: Rule data source references are not part of current YamlRule API
        // validateRuleDataSourceReferences(config, dataSourceNames);

        // Validate rule chain references
        validateRuleChainReferences(config, ruleIds);

        // Validate circular dependencies in rule chains
        validateCircularDependencies(config);
    }

    /**
     * Build a set of all rule IDs in the configuration.
     */
    private Set<String> buildRuleIdSet(YamlRuleConfiguration config) {
        Set<String> ruleIds = new HashSet<>();
        if (config.getRules() != null) {
            for (YamlRule rule : config.getRules()) {
                if (rule.getId() != null) {
                    ruleIds.add(rule.getId());
                }
            }
        }
        return ruleIds;
    }

    /**
     * Build a set of all data source names in the configuration.
     */
    private Set<String> buildDataSourceNameSet(YamlRuleConfiguration config) {
        Set<String> dataSourceNames = new HashSet<>();
        if (config.getDataSources() != null) {
            for (YamlDataSource dataSource : config.getDataSources()) {
                if (dataSource.getName() != null) {
                    dataSourceNames.add(dataSource.getName());
                }
            }
        }
        return dataSourceNames;
    }

    /**
     * Validate rule references in rule groups.
     */
    private void validateRuleGroupReferences(YamlRuleConfiguration config, Set<String> ruleIds) throws YamlConfigurationException {
        if (config.getRuleGroups() != null) {
            for (YamlRuleGroup group : config.getRuleGroups()) {
                if (group.getRuleIds() != null) {
                    for (String ruleId : group.getRuleIds()) {
                        if (ruleId != null && !ruleId.trim().isEmpty() && !ruleIds.contains(ruleId)) {
                            throw new YamlConfigurationException("Rule reference not found: Rule '" + ruleId +
                                "' referenced in rule group '" + group.getId() + "' does not exist");
                        }
                    }
                }
            }
        }
    }

    /**
     * Validate data source references in rules.
     * Note: Currently disabled as YamlRule doesn't have direct data source references in the API.
     * Data source references may be in metadata or custom properties.
     */
    @SuppressWarnings("unused")
    private void validateRuleDataSourceReferences(YamlRuleConfiguration config, Set<String> dataSourceNames) throws YamlConfigurationException {
        // This validation is currently disabled as YamlRule doesn't have a getDataSource() method
        // Future implementation could check metadata or custom properties for data source references
    }

    /**
     * Validate rule references in rule chains.
     */
    @SuppressWarnings("unchecked")
    private void validateRuleChainReferences(YamlRuleConfiguration config, Set<String> ruleIds) throws YamlConfigurationException {
        if (config.getRuleChains() != null) {
            for (YamlRuleChain ruleChain : config.getRuleChains()) {
                Map<String, Object> chainConfig = ruleChain.getConfiguration();
                if (chainConfig != null) {
                    validateRuleReferencesInChainConfig(chainConfig, ruleIds, ruleChain.getId());
                }
            }
        }
    }

    /**
     * Recursively validate rule references in rule chain configuration.
     */
    @SuppressWarnings("unchecked")
    private void validateRuleReferencesInChainConfig(Map<String, Object> config, Set<String> ruleIds, String chainId) throws YamlConfigurationException {
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Check for rule-id references
            if ("rule-id".equals(key) && value instanceof String) {
                String ruleId = (String) value;
                if (!ruleId.trim().isEmpty() && !ruleIds.contains(ruleId)) {
                    throw new YamlConfigurationException("Rule reference not found: Rule '" + ruleId +
                        "' referenced in rule chain '" + chainId + "' does not exist");
                }
            }

            // Recursively check nested maps and lists
            if (value instanceof Map) {
                validateRuleReferencesInChainConfig((Map<String, Object>) value, ruleIds, chainId);
            } else if (value instanceof List) {
                List<Object> list = (List<Object>) value;
                for (Object item : list) {
                    if (item instanceof Map) {
                        validateRuleReferencesInChainConfig((Map<String, Object>) item, ruleIds, chainId);
                    }
                }
            }
        }
    }

    /**
     * Validate for circular dependencies in rule chains.
     */
    @SuppressWarnings("unchecked")
    private void validateCircularDependencies(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getRuleChains() != null) {
            for (YamlRuleChain ruleChain : config.getRuleChains()) {
                if ("sequential-dependency".equals(ruleChain.getPattern())) {
                    validateSequentialDependencyCircularDependencies(ruleChain);
                }
            }
        }
    }

    /**
     * Validate circular dependencies in sequential-dependency rule chains.
     */
    @SuppressWarnings("unchecked")
    private void validateSequentialDependencyCircularDependencies(YamlRuleChain ruleChain) throws YamlConfigurationException {
        Map<String, Object> config = ruleChain.getConfiguration();
        if (config == null || !config.containsKey("stages")) {
            return; // Already validated in pattern validation
        }

        Object stagesObj = config.get("stages");
        if (!(stagesObj instanceof List)) {
            return; // Already validated in pattern validation
        }

        List<Map<String, Object>> stages = (List<Map<String, Object>>) stagesObj;
        Map<String, Set<String>> dependencies = new HashMap<>();

        // Build dependency graph
        for (Map<String, Object> stage : stages) {
            String stageId = getStageId(stage);
            if (stageId != null) {
                Set<String> stageDeps = new HashSet<>();
                Object dependsOnObj = stage.get("depends-on");
                if (dependsOnObj instanceof List) {
                    List<String> dependsOn = (List<String>) dependsOnObj;
                    stageDeps.addAll(dependsOn);
                }
                dependencies.put(stageId, stageDeps);
            }
        }

        // Check for circular dependencies using DFS
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String stageId : dependencies.keySet()) {
            if (hasCircularDependency(stageId, dependencies, visited, recursionStack)) {
                throw new YamlConfigurationException("Circular dependency detected in rule chain '" +
                    ruleChain.getId() + "' involving stage: " + stageId);
            }
        }
    }

    /**
     * Get stage ID from stage configuration.
     */
    private String getStageId(Map<String, Object> stage) {
        Object stageObj = stage.get("stage");
        if (stageObj != null) {
            return stageObj.toString();
        }
        Object ruleIdObj = stage.get("rule-id");
        if (ruleIdObj != null) {
            return ruleIdObj.toString();
        }
        return null;
    }

    /**
     * Check for circular dependencies using DFS.
     */
    private boolean hasCircularDependency(String stageId, Map<String, Set<String>> dependencies,
                                         Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(stageId)) {
            return true; // Circular dependency found
        }
        if (visited.contains(stageId)) {
            return false; // Already processed
        }

        visited.add(stageId);
        recursionStack.add(stageId);

        Set<String> deps = dependencies.get(stageId);
        if (deps != null) {
            for (String dep : deps) {
                if (hasCircularDependency(dep, dependencies, visited, recursionStack)) {
                    return true;
                }
            }
        }

        recursionStack.remove(stageId);
        return false;
    }

    /**
     * Validate for duplicate identifiers in the configuration.
     */
    private void validateDuplicates(YamlRuleConfiguration config) throws YamlConfigurationException {
        validateDuplicateRuleIds(config);
        validateDuplicateDataSourceNames(config);
        validateDuplicateRuleGroupIds(config);
        validateDuplicateRuleChainIds(config);
        validateDuplicateCategoryNames(config);
    }

    /**
     * Validate for duplicate rule IDs.
     */
    private void validateDuplicateRuleIds(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getRules() != null) {
            Set<String> seenIds = new HashSet<>();
            for (YamlRule rule : config.getRules()) {
                if (rule.getId() != null && !rule.getId().trim().isEmpty()) {
                    String ruleId = rule.getId().trim();
                    if (seenIds.contains(ruleId)) {
                        throw new YamlConfigurationException("Duplicate rule ID found: '" + ruleId +
                            "'. Rule IDs must be unique within the configuration.");
                    }
                    seenIds.add(ruleId);
                }
            }
        }
    }

    /**
     * Validate for duplicate data source names.
     */
    private void validateDuplicateDataSourceNames(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getDataSources() != null) {
            Set<String> seenNames = new HashSet<>();
            for (YamlDataSource dataSource : config.getDataSources()) {
                if (dataSource.getName() != null && !dataSource.getName().trim().isEmpty()) {
                    String name = dataSource.getName().trim();
                    if (seenNames.contains(name)) {
                        throw new YamlConfigurationException("Duplicate data source name found: '" + name +
                            "'. Data source names must be unique within the configuration.");
                    }
                    seenNames.add(name);
                }
            }
        }
    }

    /**
     * Validate for duplicate rule group IDs.
     */
    private void validateDuplicateRuleGroupIds(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getRuleGroups() != null) {
            Set<String> seenIds = new HashSet<>();
            for (YamlRuleGroup group : config.getRuleGroups()) {
                if (group.getId() != null && !group.getId().trim().isEmpty()) {
                    String groupId = group.getId().trim();
                    if (seenIds.contains(groupId)) {
                        throw new YamlConfigurationException("Duplicate rule group ID found: '" + groupId +
                            "'. Rule group IDs must be unique within the configuration.");
                    }
                    seenIds.add(groupId);
                }
            }
        }
    }

    /**
     * Validate for duplicate rule chain IDs.
     */
    private void validateDuplicateRuleChainIds(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getRuleChains() != null) {
            Set<String> seenIds = new HashSet<>();
            for (YamlRuleChain ruleChain : config.getRuleChains()) {
                if (ruleChain.getId() != null && !ruleChain.getId().trim().isEmpty()) {
                    String chainId = ruleChain.getId().trim();
                    if (seenIds.contains(chainId)) {
                        throw new YamlConfigurationException("Duplicate rule chain ID found: '" + chainId +
                            "'. Rule chain IDs must be unique within the configuration.");
                    }
                    seenIds.add(chainId);
                }
            }
        }
    }

    /**
     * Validate for duplicate category names.
     */
    private void validateDuplicateCategoryNames(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getCategories() != null) {
            Set<String> seenNames = new HashSet<>();
            for (YamlCategory category : config.getCategories()) {
                if (category.getName() != null && !category.getName().trim().isEmpty()) {
                    String name = category.getName().trim();
                    if (seenNames.contains(name)) {
                        throw new YamlConfigurationException("Duplicate category name found: '" + name +
                            "'. Category names must be unique within the configuration.");
                    }
                    seenNames.add(name);
                }
            }
        }
    }
}
