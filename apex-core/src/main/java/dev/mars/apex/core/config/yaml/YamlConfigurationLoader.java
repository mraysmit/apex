package dev.mars.apex.core.config.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.mars.apex.core.service.data.external.DataSourceResolver;
import dev.mars.apex.core.service.data.external.ExternalDataSourceConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final DataSourceResolver dataSourceResolver;
    
    /**
     * Constructor that initializes the YAML object mapper and data-source resolver.
     */
    public YamlConfigurationLoader() {
        this.yamlMapper = createYamlMapper();
        this.dataSourceResolver = new DataSourceResolver();
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

            // Read raw content and resolve properties before parsing
            String rawContent = Files.readString(path);
            String resolvedContent = resolveProperties(rawContent);

            YamlRuleConfiguration config = yamlMapper.readValue(resolvedContent, YamlRuleConfiguration.class);

            // Process external data-source references
            processDataSourceReferences(config);

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

            // Read raw content and resolve properties before parsing
            String rawContent = new String(inputStream.readAllBytes());
            String resolvedContent = resolveProperties(rawContent);

            YamlRuleConfiguration config = yamlMapper.readValue(resolvedContent, YamlRuleConfiguration.class);

            // Process external data-source references
            processDataSourceReferences(config);

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
            // Resolve properties in the YAML string before parsing
            String resolvedYamlString = resolveProperties(yamlString);

            YamlRuleConfiguration config = yamlMapper.readValue(resolvedYamlString, YamlRuleConfiguration.class);
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
     * Process external data-source references and merge them into the configuration.
     *
     * This method resolves external data-source references defined in the 'data-source-refs'
     * section and merges them with any existing inline data-sources.
     *
     * @param config The configuration to process
     * @throws YamlConfigurationException if reference resolution fails
     */
    private void processDataSourceReferences(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getDataSourceRefs() == null || config.getDataSourceRefs().isEmpty()) {
            LOGGER.fine("No external data-source references to process");
            return;
        }

        LOGGER.info("Processing " + config.getDataSourceRefs().size() + " external data-source references");

        // Initialize data-sources list if it doesn't exist
        if (config.getDataSources() == null) {
            config.setDataSources(new ArrayList<>());
        }

        // Process each data-source reference
        for (YamlDataSourceRef ref : config.getDataSourceRefs()) {
            if (!ref.isEnabled()) {
                LOGGER.info("Skipping disabled data-source reference: " + ref.getName());
                continue;
            }

            try {
                LOGGER.info("Resolving external data-source reference: " + ref.getName() + " from " + ref.getSource());

                // Resolve the external configuration
                ExternalDataSourceConfig externalConfig = dataSourceResolver.resolveDataSource(ref.getSource());

                // Convert external configuration to YamlDataSource
                YamlDataSource yamlDataSource = convertExternalToYamlDataSource(externalConfig, ref);

                // Add to the configuration
                config.getDataSources().add(yamlDataSource);

                LOGGER.info("Successfully resolved and added data-source: " + ref.getName());

            } catch (Exception e) {
                throw new YamlConfigurationException(
                    "Failed to resolve data-source reference '" + ref.getName() + "' from '" + ref.getSource() + "'", e);
            }
        }

        LOGGER.info("Successfully processed all external data-source references");
    }

    /**
     * Convert external data-source configuration to YamlDataSource.
     */
    private YamlDataSource convertExternalToYamlDataSource(ExternalDataSourceConfig externalConfig, YamlDataSourceRef ref) {
        YamlDataSource yamlDataSource = new YamlDataSource();

        // Use the reference name, not the external config name
        yamlDataSource.setName(ref.getName());

        // Map from external config spec
        if (externalConfig.getSpec() != null) {
            yamlDataSource.setType(externalConfig.getSpec().getType());
            yamlDataSource.setSourceType(externalConfig.getSpec().getSourceType());
            yamlDataSource.setEnabled(externalConfig.getSpec().getEnabled());
            yamlDataSource.setConnection(externalConfig.getSpec().getConnection());
            yamlDataSource.setQueries(externalConfig.getSpec().getQueries());
            yamlDataSource.setCache(externalConfig.getSpec().getCache());

            // Handle parameters - convert from Map to String array if needed
            if (externalConfig.getSpec().getParameters() != null) {
                Map<String, Object> params = externalConfig.getSpec().getParameters();
                if (params.keySet() != null) {
                    yamlDataSource.setParameterNames(params.keySet().toArray(new String[0]));
                }
            }
        }

        // Use description from reference if available, otherwise from external config
        String description = ref.getDescription();
        if (description == null && externalConfig.getMetadata() != null) {
            description = externalConfig.getMetadata().getDescription();
        }
        yamlDataSource.setDescription(description);

        return yamlDataSource;
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
        validateEnrichments(config);

        // Step 2: Validate cross-component references
        validateCrossComponentReferences(config);
        validateEnrichmentReferences(config);

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
     * Validate enrichments in the configuration.
     * Validates all enrichment attributes according to patterns documented in lookups.md.
     */
    private void validateEnrichments(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getEnrichments() == null || config.getEnrichments().isEmpty()) {
            LOGGER.fine("No enrichments to validate");
            return;
        }

        LOGGER.fine("Validating " + config.getEnrichments().size() + " enrichments");

        for (YamlEnrichment enrichment : config.getEnrichments()) {
            validateEnrichment(enrichment);
        }

        LOGGER.fine("Enrichment validation completed successfully");
    }

    /**
     * Validate a single enrichment configuration.
     */
    private void validateEnrichment(YamlEnrichment enrichment) throws YamlConfigurationException {
        String enrichmentId = enrichment.getId();

        // Validate basic enrichment structure
        validateEnrichmentBasicStructure(enrichment);

        // Validate enrichment type and type-specific requirements
        validateEnrichmentType(enrichment);

        // Validate condition expression if present
        validateEnrichmentCondition(enrichment);

        // Validate lookup configuration for lookup enrichments
        if ("lookup-enrichment".equals(enrichment.getType())) {
            validateLookupConfiguration(enrichment);
        }

        // Validate field mappings
        validateFieldMappings(enrichment.getFieldMappings(), enrichmentId);

        LOGGER.fine("Validated enrichment: " + enrichmentId);
    }

    /**
     * Validate basic enrichment structure (required fields).
     */
    private void validateEnrichmentBasicStructure(YamlEnrichment enrichment) throws YamlConfigurationException {
        if (enrichment.getId() == null || enrichment.getId().trim().isEmpty()) {
            throw new YamlConfigurationException("Enrichment ID is required");
        }

        String enrichmentId = enrichment.getId();

        if (enrichment.getType() == null || enrichment.getType().trim().isEmpty()) {
            throw new YamlConfigurationException("Enrichment type is required for enrichment: " + enrichmentId);
        }

        // Validate ID format (alphanumeric, hyphens, underscores only)
        if (!enrichmentId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new YamlConfigurationException("Enrichment ID '" + enrichmentId + "' contains invalid characters. Use only letters, numbers, hyphens, and underscores");
        }
    }

    /**
     * Validate enrichment type and type-specific requirements.
     */
    private void validateEnrichmentType(YamlEnrichment enrichment) throws YamlConfigurationException {
        String type = enrichment.getType();
        String enrichmentId = enrichment.getId();

        Set<String> validTypes = Set.of("lookup-enrichment", "field-enrichment", "calculation-enrichment");
        if (!validTypes.contains(type)) {
            throw new YamlConfigurationException("Invalid enrichment type '" + type + "' for enrichment: " + enrichmentId + ". Valid types: " + validTypes);
        }

        // Type-specific validation
        switch (type) {
            case "lookup-enrichment":
                if (enrichment.getLookupConfig() == null) {
                    throw new YamlConfigurationException("lookup-enrichment type requires 'lookup-config' for enrichment: " + enrichmentId);
                }
                break;
            case "field-enrichment":
                if (enrichment.getFieldMappings() == null || enrichment.getFieldMappings().isEmpty()) {
                    throw new YamlConfigurationException("field-enrichment type requires 'field-mappings' for enrichment: " + enrichmentId);
                }
                break;
            case "calculation-enrichment":
                // Calculation enrichments should have field mappings with transformations
                if (enrichment.getFieldMappings() == null || enrichment.getFieldMappings().isEmpty()) {
                    throw new YamlConfigurationException("calculation-enrichment type requires 'field-mappings' for enrichment: " + enrichmentId);
                }
                break;
        }
    }

    /**
     * Validate enrichment condition expression.
     */
    private void validateEnrichmentCondition(YamlEnrichment enrichment) throws YamlConfigurationException {
        String condition = enrichment.getCondition();
        String enrichmentId = enrichment.getId();

        if (condition != null && !condition.trim().isEmpty()) {
            // Validate SpEL syntax
            if (!isValidSpELExpression(condition)) {
                throw new YamlConfigurationException("Invalid SpEL expression in condition '" + condition + "' for enrichment: " + enrichmentId);
            }

            // Validate common condition patterns
            validateConditionPatterns(condition, enrichmentId);
        }
    }

    /**
     * Validate lookup configuration for lookup enrichments.
     */
    private void validateLookupConfiguration(YamlEnrichment enrichment) throws YamlConfigurationException {
        YamlEnrichment.LookupConfig lookupConfig = enrichment.getLookupConfig();
        String enrichmentId = enrichment.getId();

        if (lookupConfig == null) {
            throw new YamlConfigurationException("lookup-config is required for lookup enrichment: " + enrichmentId);
        }

        // Validate lookup configuration structure
        validateLookupConfigStructure(lookupConfig, enrichmentId);

        // Validate lookup key expression
        validateLookupKeyExpression(lookupConfig.getLookupKey(), enrichmentId);

        // Validate dataset configuration if present
        if (lookupConfig.getLookupDataset() != null) {
            validateLookupDataset(lookupConfig.getLookupDataset(), enrichmentId);
        }

        // Validate caching configuration
        validateCachingConfiguration(lookupConfig, enrichmentId);
    }

    /**
     * Validate lookup configuration structure.
     */
    private void validateLookupConfigStructure(YamlEnrichment.LookupConfig lookupConfig, String enrichmentId) throws YamlConfigurationException {
        // Must have either lookup-service OR lookup-dataset
        boolean hasService = lookupConfig.getLookupService() != null && !lookupConfig.getLookupService().trim().isEmpty();
        boolean hasDataset = lookupConfig.getLookupDataset() != null;

        if (!hasService && !hasDataset) {
            throw new YamlConfigurationException("lookup-config must specify either 'lookup-service' or 'lookup-dataset' for enrichment: " + enrichmentId);
        }

        // Cannot have both (this is a design decision - could be relaxed if needed)
        if (hasService && hasDataset) {
            throw new YamlConfigurationException("lookup-config cannot specify both 'lookup-service' and 'lookup-dataset' for enrichment: " + enrichmentId + ". Choose one approach");
        }

        // Validate lookup-key is present
        if (lookupConfig.getLookupKey() == null || lookupConfig.getLookupKey().trim().isEmpty()) {
            throw new YamlConfigurationException("lookup-key is required in lookup-config for enrichment: " + enrichmentId);
        }
    }

    /**
     * Validate lookup key expression according to patterns from lookups.md.
     */
    private void validateLookupKeyExpression(String lookupKey, String enrichmentId) throws YamlConfigurationException {
        if (lookupKey == null || lookupKey.trim().isEmpty()) {
            throw new YamlConfigurationException("lookup-key is required for lookup enrichment: " + enrichmentId);
        }

        // Validate SpEL syntax
        if (!isValidSpELExpression(lookupKey)) {
            throw new YamlConfigurationException("Invalid SpEL expression in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId);
        }

        // Validate lookup key patterns from lookups.md
        validateLookupKeyPatterns(lookupKey, enrichmentId);
    }

    /**
     * Validate complex lookup key patterns documented in lookups.md.
     */
    private void validateLookupKeyPatterns(String lookupKey, String enrichmentId) throws YamlConfigurationException {
        // Pattern 1: String concatenation (compound keys)
        if (lookupKey.contains("+") && lookupKey.contains("'")) {
            validateStringConcatenationPattern(lookupKey, enrichmentId);
        }

        // Pattern 2: Conditional expressions (ternary operators)
        if (lookupKey.contains("?") && lookupKey.contains(":")) {
            validateConditionalExpressionPattern(lookupKey, enrichmentId);
        }

        // Pattern 3: String manipulation methods
        if (lookupKey.contains(".substring(") || lookupKey.contains(".toUpperCase(") || lookupKey.contains(".toLowerCase(")) {
            validateStringManipulationPattern(lookupKey, enrichmentId);
        }

        // Pattern 4: Hash-based compound keys
        if (lookupKey.contains("T(java.lang.String).valueOf") || lookupKey.contains(".hashCode()")) {
            validateHashBasedKeyPattern(lookupKey, enrichmentId);
        }

        // Pattern 5: Hierarchical field access
        if (lookupKey.contains(".") && !lookupKey.contains("(")) {
            validateHierarchicalFieldAccess(lookupKey, enrichmentId);
        }

        // Pattern 6: Safe navigation operator
        if (lookupKey.contains("?.")) {
            validateSafeNavigationPattern(lookupKey, enrichmentId);
        }
    }

    /**
     * Validate string concatenation patterns in lookup keys.
     */
    private void validateStringConcatenationPattern(String lookupKey, String enrichmentId) throws YamlConfigurationException {
        // Check for balanced quotes in concatenation
        long singleQuoteCount = lookupKey.chars().filter(ch -> ch == '\'').count();
        if (singleQuoteCount % 2 != 0) {
            throw new YamlConfigurationException("Unbalanced single quotes in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId);
        }

        // Check for proper concatenation syntax
        if (lookupKey.contains("++") || lookupKey.contains("+ +")) {
            throw new YamlConfigurationException("Invalid concatenation syntax in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Use single '+' for concatenation");
        }
    }

    /**
     * Validate conditional expression patterns in lookup keys.
     */
    private void validateConditionalExpressionPattern(String lookupKey, String enrichmentId) throws YamlConfigurationException {
        // Handle Elvis operator (?:) - safe navigation with null coalescing
        if (lookupKey.contains("?:")) {
            // Elvis operator is valid - just ensure basic syntax
            if (lookupKey.indexOf("?:") == lookupKey.length() - 2) {
                throw new YamlConfigurationException("Elvis operator (?:) missing right operand in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId);
            }
            return; // Elvis operator is valid, skip ternary validation
        }

        // Count ternary operators (excluding safe navigation ?. and string literals)
        String withoutSafeNav = lookupKey.replace("?.", "X."); // Replace ?. with X. to avoid counting

        // Remove string literals to avoid counting colons inside strings
        String withoutStrings = withoutSafeNav.replaceAll("'[^']*'", "''"); // Replace 'text' with ''
        withoutStrings = withoutStrings.replaceAll("\"[^\"]*\"", "\"\""); // Replace "text" with ""

        long questionMarkCount = withoutStrings.chars().filter(ch -> ch == '?').count();
        long colonCount = withoutStrings.chars().filter(ch -> ch == ':').count();

        // Basic ternary validation - should have matching ? and :
        if (questionMarkCount != colonCount) {
            throw new YamlConfigurationException("Unbalanced ternary operators in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Each '?' must have a matching ':'");
        }

        // Check for nested ternary complexity (warn if too complex)
        if (questionMarkCount > 2) {
            LOGGER.warning("Complex nested ternary expression in lookup-key for enrichment: " + enrichmentId + ". Consider simplifying for maintainability");
        }
    }

    /**
     * Validate string manipulation patterns in lookup keys.
     */
    private void validateStringManipulationPattern(String lookupKey, String enrichmentId) throws YamlConfigurationException {
        // Validate substring calls
        if (lookupKey.contains(".substring(")) {
            validateSubstringCalls(lookupKey, enrichmentId);
        }

        // Validate case conversion calls
        if (lookupKey.contains(".toUpperCase(") || lookupKey.contains(".toLowerCase(")) {
            validateCaseConversionCalls(lookupKey, enrichmentId);
        }
    }

    /**
     * Validate substring method calls in lookup keys.
     */
    private void validateSubstringCalls(String lookupKey, String enrichmentId) throws YamlConfigurationException {
        // Check for proper substring syntax
        if (lookupKey.contains(".substring()")) {
            throw new YamlConfigurationException("Invalid substring call in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". substring() requires parameters");
        }

        // For now, just validate that substring calls have basic structure - detailed validation can be done at runtime
        // This is a simple check to ensure substring calls are not malformed
        if (lookupKey.contains(".substring(") && !lookupKey.contains(")")) {
            throw new YamlConfigurationException("Malformed substring call in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Missing closing parenthesis");
        }
    }

    /**
     * Validate case conversion method calls in lookup keys.
     */
    private void validateCaseConversionCalls(String lookupKey, String enrichmentId) throws YamlConfigurationException {
        // Check for proper method call syntax
        if (lookupKey.contains(".toUpperCase") && !lookupKey.contains(".toUpperCase()")) {
            throw new YamlConfigurationException("Invalid toUpperCase call in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Use toUpperCase()");
        }

        if (lookupKey.contains(".toLowerCase") && !lookupKey.contains(".toLowerCase()")) {
            throw new YamlConfigurationException("Invalid toLowerCase call in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Use toLowerCase()");
        }
    }

    /**
     * Validate hash-based compound key patterns.
     */
    private void validateHashBasedKeyPattern(String lookupKey, String enrichmentId) throws YamlConfigurationException {
        // Validate T(java.lang.String).valueOf usage
        if (lookupKey.contains("T(java.lang.String).valueOf") && !lookupKey.contains("T(java.lang.String).valueOf(")) {
            throw new YamlConfigurationException("Invalid T(java.lang.String).valueOf usage in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Must include opening parenthesis");
        }

        // Validate hashCode usage
        if (lookupKey.contains(".hashCode") && !lookupKey.contains(".hashCode()")) {
            throw new YamlConfigurationException("Invalid hashCode call in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Use hashCode()");
        }

        // Warn about hash collision potential
        if (lookupKey.contains(".hashCode()")) {
            LOGGER.warning("Hash-based lookup key in enrichment: " + enrichmentId + ". Be aware of potential hash collisions in production data");
        }
    }

    /**
     * Validate hierarchical field access patterns.
     */
    private void validateHierarchicalFieldAccess(String lookupKey, String enrichmentId) throws YamlConfigurationException {
        // Check for excessive nesting depth
        long dotCount = lookupKey.chars().filter(ch -> ch == '.').count();
        if (dotCount > 5) {
            LOGGER.warning("Deep hierarchical field access in lookup-key for enrichment: " + enrichmentId + ". Consider flattening data structure for better performance");
        }

        // Check for field access on potentially null objects without safe navigation
        if (lookupKey.contains(".") && !lookupKey.contains("?.") && !lookupKey.contains("!= null")) {
            LOGGER.info("Consider using safe navigation operator (?.) in lookup-key for enrichment: " + enrichmentId + " to handle null values gracefully");
        }
    }

    /**
     * Validate safe navigation operator patterns.
     */
    private void validateSafeNavigationPattern(String lookupKey, String enrichmentId) throws YamlConfigurationException {
        // Check for proper safe navigation syntax
        if (lookupKey.contains("? .")) {
            throw new YamlConfigurationException("Invalid safe navigation syntax in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Use '?.' without space");
        }

        // Validate that safe navigation is used consistently
        if (lookupKey.contains("?.") && lookupKey.contains(".") && !lookupKey.contains("?:")) {
            LOGGER.info("Mixed safe and unsafe navigation in lookup-key for enrichment: " + enrichmentId + ". Consider using consistent safe navigation or null checks");
        }
    }

    /**
     * Validate condition patterns in enrichment conditions.
     */
    private void validateConditionPatterns(String condition, String enrichmentId) throws YamlConfigurationException {
        // Check for common condition patterns
        if (condition.contains("!= null") || condition.contains("== null")) {
            // Good - explicit null checks
        } else if (condition.contains(".") && !condition.contains("?.")) {
            LOGGER.info("Consider adding null checks in condition for enrichment: " + enrichmentId + " to prevent NullPointerException");
        }

        // Check for boolean logic complexity
        long andCount = condition.split("&&").length - 1;
        long orCount = condition.split("\\|\\|").length - 1;
        if (andCount + orCount > 3) {
            LOGGER.warning("Complex boolean logic in condition for enrichment: " + enrichmentId + ". Consider simplifying for maintainability");
        }
    }

    /**
     * Validate lookup dataset configuration.
     */
    private void validateLookupDataset(YamlEnrichment.LookupDataset dataset, String enrichmentId) throws YamlConfigurationException {
        if (dataset.getType() == null || dataset.getType().trim().isEmpty()) {
            throw new YamlConfigurationException("Dataset type is required for enrichment: " + enrichmentId);
        }

        String type = dataset.getType().toLowerCase();
        Set<String> validTypes = Set.of("inline", "yaml-file", "csv-file", "database", "rest-api");

        if (!validTypes.contains(type)) {
            throw new YamlConfigurationException("Invalid dataset type '" + type + "' for enrichment: " + enrichmentId + ". Valid types: " + validTypes);
        }

        // Type-specific validation
        switch (type) {
            case "inline":
                validateInlineDataset(dataset, enrichmentId);
                break;
            case "yaml-file":
            case "csv-file":
                validateFileDataset(dataset, enrichmentId, type);
                break;
            case "database":
                validateDatabaseDataset(dataset, enrichmentId);
                break;
            case "rest-api":
                validateRestApiDataset(dataset, enrichmentId);
                break;
        }
    }

    /**
     * Validate inline dataset configuration.
     */
    private void validateInlineDataset(YamlEnrichment.LookupDataset dataset, String enrichmentId) throws YamlConfigurationException {
        if (dataset.getData() == null || dataset.getData().isEmpty()) {
            throw new YamlConfigurationException("Inline dataset must have 'data' array for enrichment: " + enrichmentId);
        }

        if (dataset.getKeyField() == null || dataset.getKeyField().trim().isEmpty()) {
            throw new YamlConfigurationException("Inline dataset must specify 'key-field' for enrichment: " + enrichmentId);
        }

        // Validate that all data records have the key field
        String keyField = dataset.getKeyField();
        for (int i = 0; i < dataset.getData().size(); i++) {
            Map<String, Object> record = dataset.getData().get(i);
            if (!record.containsKey(keyField)) {
                throw new YamlConfigurationException("Data record at index " + i + " missing key field '" + keyField + "' for enrichment: " + enrichmentId);
            }

            Object keyValue = record.get(keyField);
            if (keyValue == null) {
                throw new YamlConfigurationException("Data record at index " + i + " has null value for key field '" + keyField + "' for enrichment: " + enrichmentId);
            }
        }

        // Check for duplicate keys
        Set<Object> keyValues = new HashSet<>();
        for (int i = 0; i < dataset.getData().size(); i++) {
            Object keyValue = dataset.getData().get(i).get(keyField);
            if (!keyValues.add(keyValue)) {
                throw new YamlConfigurationException("Duplicate key value '" + keyValue + "' found in inline dataset for enrichment: " + enrichmentId);
            }
        }
    }

    /**
     * Validate file-based dataset configuration.
     */
    private void validateFileDataset(YamlEnrichment.LookupDataset dataset, String enrichmentId, String type) throws YamlConfigurationException {
        if (dataset.getFilePath() == null || dataset.getFilePath().trim().isEmpty()) {
            throw new YamlConfigurationException(type + " dataset must specify 'file-path' for enrichment: " + enrichmentId);
        }

        if (dataset.getKeyField() == null || dataset.getKeyField().trim().isEmpty()) {
            throw new YamlConfigurationException(type + " dataset must specify 'key-field' for enrichment: " + enrichmentId);
        }

        // Validate file extension matches type
        String filePath = dataset.getFilePath().toLowerCase();
        if ("yaml-file".equals(type) && !filePath.endsWith(".yaml") && !filePath.endsWith(".yml")) {
            LOGGER.warning("YAML dataset file path should end with .yaml or .yml for enrichment: " + enrichmentId);
        }

        if ("csv-file".equals(type) && !filePath.endsWith(".csv")) {
            LOGGER.warning("CSV dataset file path should end with .csv for enrichment: " + enrichmentId);
        }
    }

    /**
     * Validate database dataset configuration.
     */
    private void validateDatabaseDataset(YamlEnrichment.LookupDataset dataset, String enrichmentId) throws YamlConfigurationException {
        // Database datasets typically don't use key-field (they use SQL queries)
        // This is a placeholder for future database-specific validation
        LOGGER.fine("Database dataset validation for enrichment: " + enrichmentId);
    }

    /**
     * Validate REST API dataset configuration.
     */
    private void validateRestApiDataset(YamlEnrichment.LookupDataset dataset, String enrichmentId) throws YamlConfigurationException {
        // REST API datasets typically don't use key-field (they use URL patterns)
        // This is a placeholder for future REST API-specific validation
        LOGGER.fine("REST API dataset validation for enrichment: " + enrichmentId);
    }

    /**
     * Validate caching configuration.
     */
    private void validateCachingConfiguration(YamlEnrichment.LookupConfig lookupConfig, String enrichmentId) throws YamlConfigurationException {
        if (lookupConfig.getCacheTtlSeconds() != null) {
            Integer ttl = lookupConfig.getCacheTtlSeconds();
            if (ttl < 0) {
                throw new YamlConfigurationException("Cache TTL cannot be negative for enrichment: " + enrichmentId);
            }

            if (ttl > 86400) { // 24 hours
                LOGGER.warning("Cache TTL is very long (" + ttl + " seconds) for enrichment: " + enrichmentId + ". Consider if this is appropriate for your use case");
            }
        }
    }

    /**
     * Validate field mappings configuration.
     */
    private void validateFieldMappings(List<YamlEnrichment.FieldMapping> fieldMappings, String enrichmentId) throws YamlConfigurationException {
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            // Field mappings are optional for some enrichment types
            return;
        }

        Set<String> targetFields = new HashSet<>();

        for (int i = 0; i < fieldMappings.size(); i++) {
            YamlEnrichment.FieldMapping mapping = fieldMappings.get(i);

            // Validate required fields
            if (mapping.getSourceField() == null || mapping.getSourceField().trim().isEmpty()) {
                throw new YamlConfigurationException("Field mapping at index " + i + " missing 'source-field' for enrichment: " + enrichmentId);
            }

            if (mapping.getTargetField() == null || mapping.getTargetField().trim().isEmpty()) {
                throw new YamlConfigurationException("Field mapping at index " + i + " missing 'target-field' for enrichment: " + enrichmentId);
            }

            // Check for duplicate target fields
            String targetField = mapping.getTargetField();
            if (!targetFields.add(targetField)) {
                throw new YamlConfigurationException("Duplicate target field '" + targetField + "' in field mappings for enrichment: " + enrichmentId);
            }

            // Validate transformation expressions if present
            if (mapping.getTransformation() != null && !mapping.getTransformation().trim().isEmpty()) {
                String transformation = mapping.getTransformation();
                if (!isValidSpELExpression(transformation)) {
                    throw new YamlConfigurationException("Invalid transformation expression '" + transformation + "' in field mapping for enrichment: " + enrichmentId);
                }

                // Validate transformation patterns
                validateTransformationPatterns(transformation, enrichmentId, i);
            }

            // Validate conditional mappings if present
            validateConditionalMappings(mapping, enrichmentId, i);
        }
    }

    /**
     * Validate transformation patterns in field mappings.
     */
    private void validateTransformationPatterns(String transformation, String enrichmentId, int mappingIndex) throws YamlConfigurationException {
        // Check for common transformation patterns
        if (transformation.contains("T(java.") && !transformation.contains("T(java.lang.") && !transformation.contains("T(java.time.") && !transformation.contains("T(java.math.")) {
            LOGGER.warning("Transformation uses Java type reference in field mapping " + mappingIndex + " for enrichment: " + enrichmentId + ". Ensure the class is available at runtime");
        }

        // Check for potentially unsafe operations
        if (transformation.contains(".getClass()") || transformation.contains("T(java.lang.Class)")) {
            LOGGER.warning("Transformation uses reflection in field mapping " + mappingIndex + " for enrichment: " + enrichmentId + ". This may have security implications");
        }

        // Check for null safety
        if (transformation.contains(".") && !transformation.contains("?.") && !transformation.contains("!= null")) {
            LOGGER.info("Consider adding null safety to transformation in field mapping " + mappingIndex + " for enrichment: " + enrichmentId);
        }
    }

    /**
     * Validate conditional mappings in field mappings.
     */
    private void validateConditionalMappings(YamlEnrichment.FieldMapping mapping, String enrichmentId, int mappingIndex) throws YamlConfigurationException {
        // This is a placeholder for future conditional mapping validation
        // The current YamlEnrichment.FieldMapping class doesn't have conditional mapping support
        // but this method is here for future extensibility
        LOGGER.finest("Conditional mapping validation for field mapping " + mappingIndex + " in enrichment: " + enrichmentId);
    }

    /**
     * Validate SpEL expression syntax.
     */
    private boolean isValidSpELExpression(String expression) {
        try {
            // Handle template expressions (#{...}) by extracting and validating individual expressions
            if (expression.contains("#{") && expression.contains("}")) {
                return validateTemplateExpression(expression);
            }

            // Use a simple SpEL parser to validate syntax for regular expressions
            org.springframework.expression.ExpressionParser parser = new org.springframework.expression.spel.standard.SpelExpressionParser();
            parser.parseExpression(expression);
            return true;
        } catch (Exception e) {
            LOGGER.fine("Invalid SpEL expression: " + expression + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate template expressions containing #{...} syntax.
     */
    private boolean validateTemplateExpression(String template) {
        try {
            // Extract expressions from #{...} blocks and validate each one
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#\\{([^}]+)\\}");
            java.util.regex.Matcher matcher = pattern.matcher(template);

            org.springframework.expression.ExpressionParser parser = new org.springframework.expression.spel.standard.SpelExpressionParser();

            while (matcher.find()) {
                String expression = matcher.group(1);
                parser.parseExpression(expression); // This will throw if invalid
            }

            return true;
        } catch (Exception e) {
            LOGGER.fine("Invalid template expression: " + template + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Cross-component references in the configuration.
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
     * Validate enrichment references to data sources and other components.
     */
    private void validateEnrichmentReferences(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getEnrichments() == null || config.getEnrichments().isEmpty()) {
            return;
        }

        // Build reference maps for validation
        Set<String> dataSourceNames = buildDataSourceNameSet(config);
        Set<String> ruleIds = buildRuleIdSet(config);

        for (YamlEnrichment enrichment : config.getEnrichments()) {
            String enrichmentId = enrichment.getId();

            // Validate lookup service references
            if (enrichment.getLookupConfig() != null && enrichment.getLookupConfig().getLookupService() != null) {
                String serviceName = enrichment.getLookupConfig().getLookupService();
                if (!dataSourceNames.contains(serviceName)) {
                    throw new YamlConfigurationException("Enrichment '" + enrichmentId + "' references unknown lookup service: " + serviceName);
                }
            }

            // Validate file path references for file-based datasets
            if (enrichment.getLookupConfig() != null && enrichment.getLookupConfig().getLookupDataset() != null) {
                YamlEnrichment.LookupDataset dataset = enrichment.getLookupConfig().getLookupDataset();
                if (dataset.getFilePath() != null && !dataset.getFilePath().trim().isEmpty()) {
                    validateFilePathReference(dataset.getFilePath(), enrichmentId);
                }
            }

            // Validate target type references if specified
            if (enrichment.getTargetType() != null && !enrichment.getTargetType().trim().isEmpty()) {
                validateTargetTypeReference(enrichment.getTargetType(), enrichmentId);
            }
        }

        LOGGER.fine("Enrichment reference validation completed successfully");
    }

    /**
     * Validate file path references in enrichments.
     */
    private void validateFilePathReference(String filePath, String enrichmentId) throws YamlConfigurationException {
        // Check for absolute vs relative paths
        if (filePath.startsWith("/") || filePath.matches("^[A-Za-z]:.*")) {
            LOGGER.warning("Enrichment '" + enrichmentId + "' uses absolute file path: " + filePath + ". Consider using relative paths for portability");
        }

        // Check for potentially problematic path patterns
        if (filePath.contains("..")) {
            LOGGER.warning("Enrichment '" + enrichmentId + "' uses parent directory references in file path: " + filePath + ". This may cause security or portability issues");
        }

        // Check for common file path issues
        if (filePath.contains("\\")) {
            LOGGER.info("Enrichment '" + enrichmentId + "' uses backslashes in file path: " + filePath + ". Consider using forward slashes for cross-platform compatibility");
        }
    }

    /**
     * Validate target type references in enrichments.
     */
    private void validateTargetTypeReference(String targetType, String enrichmentId) throws YamlConfigurationException {
        // Check for valid Java class name format
        if (!targetType.matches("^[a-zA-Z_$][a-zA-Z\\d_$]*(?:\\.[a-zA-Z_$][a-zA-Z\\d_$]*)*$")) {
            throw new YamlConfigurationException("Invalid target type format '" + targetType + "' for enrichment: " + enrichmentId + ". Must be a valid Java class name");
        }

        // Warn about common issues
        if (targetType.contains("..")) {
            throw new YamlConfigurationException("Invalid target type '" + targetType + "' for enrichment: " + enrichmentId + ". Contains consecutive dots");
        }

        if (targetType.startsWith(".") || targetType.endsWith(".")) {
            throw new YamlConfigurationException("Invalid target type '" + targetType + "' for enrichment: " + enrichmentId + ". Cannot start or end with dot");
        }
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
        validateDuplicateEnrichmentIds(config);
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
     * Validate for duplicate enrichment IDs.
     */
    private void validateDuplicateEnrichmentIds(YamlRuleConfiguration config) throws YamlConfigurationException {
        if (config.getEnrichments() == null) {
            return;
        }

        Set<String> enrichmentIds = new HashSet<>();
        for (YamlEnrichment enrichment : config.getEnrichments()) {
            String id = enrichment.getId();
            if (id != null) {
                if (!enrichmentIds.add(id)) {
                    throw new YamlConfigurationException("Duplicate enrichment ID found: " + id);
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

    // ========================================================================
    // PROPERTY RESOLUTION METHODS (Phase 1 - Not Used Yet)
    // ========================================================================

    /**
     * Resolve environment variables and system properties in configuration values.
     * Supports: ${VAR}, ${VAR:default}
     *
     * NOTE: This method is added in Phase 1 but not used yet to ensure zero impact.
     *
     * @param value The configuration value that may contain property placeholders
     * @return The value with resolved properties
     * @throws YamlConfigurationException if a required property is not found
     */
    private String resolveProperties(String value) throws YamlConfigurationException {
        if (value == null || !value.contains("${")) {
            return value;
        }

        LOGGER.fine("Resolving properties in value: " + maskSensitiveValue(value));

        // Simple regex-based replacement for ${VAR} and ${VAR:default} patterns
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(value);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String resolved = resolveSingleProperty(placeholder);
            matcher.appendReplacement(result, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(result);

        String finalResult = result.toString();
        LOGGER.fine("Property resolution completed: " + maskSensitiveValue(finalResult));

        return finalResult;
    }

    /**
     * Resolve a single property placeholder.
     *
     * @param placeholder The placeholder (e.g., "VAR" or "VAR:default")
     * @return The resolved value
     * @throws YamlConfigurationException if the property is not found and no default is provided
     */
    private String resolveSingleProperty(String placeholder) throws YamlConfigurationException {
        // Handle default values: VAR:default
        String[] parts = placeholder.split(":", 2);
        String key = parts[0].trim();
        String defaultValue = parts.length > 1 ? parts[1].trim() : null;

        // Resolution order: System Properties -> Environment Variables -> Default
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        if (value == null && defaultValue != null) {
            value = defaultValue;
        }
        if (value == null) {
            throw new YamlConfigurationException("Property not found: " + key +
                " (checked system properties and environment variables)");
        }

        // Log resolution (mask sensitive values)
        String logValue = isSensitiveProperty(key) ? "[MASKED]" : value;
        LOGGER.fine("Resolved property: ${" + placeholder + "} -> " + logValue);

        return value;
    }

    /**
     * Check if a property key contains sensitive information.
     *
     * @param key The property key
     * @return true if the property is considered sensitive
     */
    private boolean isSensitiveProperty(String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") ||
               lowerKey.contains("secret") ||
               lowerKey.contains("token") ||
               lowerKey.contains("key") ||
               lowerKey.contains("pwd");
    }

    /**
     * Mask sensitive values for logging.
     *
     * @param value The value to potentially mask
     * @return The masked value if it appears to contain sensitive data
     */
    private String maskSensitiveValue(String value) {
        if (value == null) {
            return null;
        }

        // If the value contains ${PASSWORD}, ${SECRET}, etc., mask the whole thing
        String lowerValue = value.toLowerCase();
        if (lowerValue.contains("password") || lowerValue.contains("secret") ||
            lowerValue.contains("token") || lowerValue.contains("key")) {
            return "[MASKED_VALUE_WITH_SENSITIVE_PLACEHOLDERS]";
        }

        return value;
    }
}
