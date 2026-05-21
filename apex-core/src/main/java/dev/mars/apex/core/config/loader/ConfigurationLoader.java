package dev.mars.apex.core.config.loader;

import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.sequential.OrderedYamlConfiguration;
import dev.mars.apex.core.config.sequential.OrderedYamlParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.mars.apex.core.service.data.external.DataSourceResolver;
import dev.mars.apex.core.util.PropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.mars.apex.core.config.validation.MetadataValidator;
import dev.mars.apex.core.config.validation.CrossReferenceValidator;
import dev.mars.apex.core.config.validation.DataSourceValidator;
import dev.mars.apex.core.config.validation.DuplicateValidator;
import dev.mars.apex.core.config.validation.EnrichmentValidator;
import dev.mars.apex.core.config.validation.RuleChainValidator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
 * Service for loading YAML configuration files into rule configuration objects.
 *
 * <p>This class handles the parsing and validation of YAML configuration files.
 * It delegates reference resolution to {@link ConfigurationReferenceResolver},
 * item ordering to {@link ItemOrderProcessor}, and inline validation to
 * {@link InlineConfigurationValidator}.</p>
 *
 * <p>This class is part of the APEX, a powerful expression processor for Java applications.</p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class ConfigurationLoader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationLoader.class);

    private final ObjectMapper yamlMapper;
    private final DataSourceResolver dataSourceResolver;
    private final OrderedYamlParser orderedYamlParser;
    private final DuplicateValidator duplicateValidator;
    private final RuleChainValidator ruleChainValidator;
    private final DataSourceValidator dataSourceValidator;
    private final CrossReferenceValidator crossReferenceValidator;
    private final EnrichmentValidator enrichmentValidator;
    private final ItemOrderProcessor itemOrderProcessor;
    private final InlineConfigurationValidator inlineValidator;
    private final ConfigurationReferenceResolver referenceResolver;

    /**
     * Constructor that initializes the YAML object mapper and data-source resolver.
     */
    public ConfigurationLoader() {
        this.yamlMapper = createYamlMapper();
        this.dataSourceResolver = new DataSourceResolver();
        this.orderedYamlParser = new OrderedYamlParser();
        this.duplicateValidator = new DuplicateValidator();
        this.ruleChainValidator = new RuleChainValidator();
        this.dataSourceValidator = new DataSourceValidator();
        this.crossReferenceValidator = new CrossReferenceValidator();
        this.enrichmentValidator = new EnrichmentValidator();
        this.itemOrderProcessor = new ItemOrderProcessor();
        this.inlineValidator = new InlineConfigurationValidator();
        this.referenceResolver = new ConfigurationReferenceResolver(this.yamlMapper, this.dataSourceResolver);
    }

    /**
     * Load configuration from a file path.
     *
     * @param filePath The path to the YAML configuration file
     * @return The loaded configuration
     * @throws ConfigurationException if loading fails
     */
    public YamlRuleConfiguration loadFromFile(String filePath) throws ConfigurationException {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new ConfigurationException("Configuration file not found: " + filePath);
            }

            logger.info("Loading YAML configuration from file: " + filePath);
            String rawContent = Files.readString(path);
            return loadFromResolvedContent(resolveProperties(rawContent), filePath,
                path.toAbsolutePath().getParent().toString());

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration from file: " + filePath, e);
        }
    }

    /**
     * Load configuration from a File object.
     *
     * @param file The YAML configuration file
     * @return The loaded configuration
     * @throws ConfigurationException if loading fails
     */
    public YamlRuleConfiguration loadFromFile(File file) throws ConfigurationException {
        try {
            if (!file.exists()) {
                throw new ConfigurationException("Configuration file not found: " + file.getAbsolutePath());
            }

            logger.info("Loading YAML configuration from file: " + file.getAbsolutePath());
            String rawContent = Files.readString(file.toPath());
            return loadFromResolvedContent(resolveProperties(rawContent), file.getAbsolutePath(),
                file.toPath().toAbsolutePath().getParent().toString());

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration from file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Load configuration from an InputStream (useful for classpath resources).
     *
     * @param inputStream The input stream containing YAML configuration
     * @return The loaded configuration
     * @throws ConfigurationException if loading fails
     */
    public YamlRuleConfiguration loadFromStream(InputStream inputStream) throws ConfigurationException {
        try {
            logger.info("Loading YAML configuration from input stream");
            String rawContent = new String(inputStream.readAllBytes());
            return loadFromResolvedContent(resolveProperties(rawContent), "<stream>", null);

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration from input stream", e);
        }
    }

    /**
     * Common loading pipeline: ordered parse → process references → expand → validate.
     *
     * @param resolvedContent The YAML content with properties already resolved
     * @param sourceName      The source name for logging and ordered parser context
     * @return The fully loaded and validated configuration
     * @throws ConfigurationException if any processing or validation step fails
     */
    private YamlRuleConfiguration loadFromResolvedContent(String resolvedContent, String sourceName, String sourceDirectory) throws ConfigurationException {
        // Use OrderedYamlParser to preserve section order
        OrderedYamlConfiguration orderedConfig = orderedYamlParser.parseYamlString(resolvedContent, sourceName);
        YamlRuleConfiguration config = orderedConfig.getConfiguration();

        // Copy section order and item order into the configuration
        config.setSectionOrder(orderedConfig.getSectionOrder());
        config.setItemOrder(orderedConfig.getItemOrder());
        logger.debug("Section order from YAML: " + orderedConfig.getSectionOrder());
        logger.debug("Item order from YAML: " + orderedConfig.getItemOrder().size() + " items");

        // Set source directory before processing references so relative refs can be resolved
        if (sourceDirectory != null) {
            config.setSourceDirectory(sourceDirectory);
        }

        // Process external rule references
        processRuleReferences(config);

        // Process external enrichment references
        processEnrichmentReferences(config);

        // Process external data-source references
        processDataSourceReferences(config);

        // Expand reference placeholders in item order
        expandReferencePlaceholders(config);

        // Apply groups-only logic to filter itemOrder AFTER expanding references
        // This ensures that enrichment-groups/rule-groups loaded from external files
        // are also subject to groups-only filtering if they're referenced by groups in the main file
        applyGroupsOnlyLogic(config);

        validateConfiguration(config);
        logger.info("Successfully loaded configuration: " +
                   (config.getMetadata() != null ? config.getMetadata().getName() : "unnamed"));

        return config;
    }

    /**
     * Load configuration from a classpath resource.
     *
     * @param resourcePath The classpath resource path
     * @return The loaded configuration
     * @throws ConfigurationException if loading fails
     */
    public YamlRuleConfiguration loadFromClasspath(String resourcePath) throws ConfigurationException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new ConfigurationException("Configuration resource not found: " + resourcePath);
            }

            logger.info("Loading YAML configuration from classpath: " + resourcePath);
            String rawContent = new String(inputStream.readAllBytes());

            // Derive sourceDirectory when the classpath resource is a file on disk
            // (common in test environments and exploded classpath deployments)
            String sourceDirectory = null;
            java.net.URL resourceUrl = getClass().getClassLoader().getResource(resourcePath);
            if (resourceUrl != null && "file".equals(resourceUrl.getProtocol())) {
                try {
                    sourceDirectory = Paths.get(resourceUrl.toURI()).getParent().toString();
                } catch (Exception ignored) {
                    logger.debug("Could not derive sourceDirectory for classpath resource: {}", resourcePath);
                }
            }

            return loadFromResolvedContent(resolveProperties(rawContent), resourcePath, sourceDirectory);

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration from classpath: " + resourcePath, e);
        }
    }

    /**
     * Save configuration to a file.
     *
     * @param configuration The configuration to save
     * @param filePath The target file path
     * @throws ConfigurationException if saving fails
     */
    public void saveToFile(YamlRuleConfiguration configuration, String filePath) throws ConfigurationException {
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());

            logger.info("Saving YAML configuration to file: " + filePath);
            yamlMapper.writeValue(path.toFile(), configuration);
            logger.info("Successfully saved configuration to: " + filePath);

        } catch (IOException e) {
            throw new ConfigurationException("Failed to save configuration to file: " + filePath, e);
        }
    }

    /**
     * Convert configuration to YAML string.
     *
     * @param configuration The configuration to convert
     * @return YAML string representation
     * @throws ConfigurationException if conversion fails
     */
    public String toYamlString(YamlRuleConfiguration configuration) throws ConfigurationException {
        try {
            return yamlMapper.writeValueAsString(configuration);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to convert configuration to YAML string", e);
        }
    }

    /**
     * Load raw YAML content as a Map for dependency analysis.
     *
     * @param filePath The path to the YAML file
     * @return The YAML content as a Map
     * @throws ConfigurationException if loading fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadAsMap(String filePath) throws ConfigurationException {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new ConfigurationException("Configuration file not found: " + filePath);
            }

            logger.info("Loading YAML file as Map: " + filePath);
            Map<String, Object> yamlContent = yamlMapper.readValue(path.toFile(), Map.class);

            // Validate metadata using MetadataValidator
            MetadataValidator.validateMetadataAndThrow(yamlContent, filePath);

            return yamlContent;

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load YAML file as Map: " + filePath, e);
        }
    }

    /**
     * Load raw YAML content as a Map from an InputStream.
     * 
     * <p>This method is useful for loading YAML content from classpath resources
     * or other stream-based sources, such as JAR-packaged resources.</p>
     *
     * @param inputStream The input stream containing YAML content
     * @return The YAML content as a Map
     * @throws ConfigurationException if loading fails or inputStream is null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadAsMap(InputStream inputStream) throws ConfigurationException {
        if (inputStream == null) {
            throw new ConfigurationException("Input stream cannot be null");
        }
        
        try {
            logger.info("Loading YAML from input stream as Map");
            Map<String, Object> yamlContent = yamlMapper.readValue(inputStream, Map.class);

            // Validate metadata using MetadataValidator
            MetadataValidator.validateMetadataAndThrow(yamlContent, "<stream>");

            return yamlContent;

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load YAML from input stream as Map", e);
        }
    }

    /**
     * Load raw YAML content as a Map from a classpath resource.
     * 
     * <p>This is a convenience method that combines resource lookup and
     * stream-based loading for classpath resources.</p>
     *
     * @param resourcePath The classpath resource path
     * @return The YAML content as a Map
     * @throws ConfigurationException if the resource is not found or loading fails
     */
    public Map<String, Object> loadAsMapFromClasspath(String resourcePath) throws ConfigurationException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new ConfigurationException("Classpath resource not found: " + resourcePath);
            }

            logger.info("Loading YAML from classpath as Map: " + resourcePath);
            return loadAsMap(inputStream);

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load YAML from classpath as Map: " + resourcePath, e);
        }
    }

    /**
     * Check if a YAML file is a component file by examining its type field.
     * Supports both file system paths and classpath resources.
     *
     * @param filePath The path to the YAML file (file system or classpath)
     * @return true if the file has type="component", false otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean isComponentFile(String filePath) {
        logger.debug("Checking if file is component: {}", filePath);
        try {
            Map<String, Object> yamlContent;

            // Try file system first
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                logger.debug("Loading from file system: {}", filePath);
                yamlContent = yamlMapper.readValue(path.toFile(), Map.class);
            } else {
                // Try classpath
                logger.debug("File not found in file system, trying classpath: {}", filePath);
                InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
                if (is == null) {
                    logger.debug("File not found in file system or classpath: {}", filePath);
                    return false;
                }
                logger.debug("Loading from classpath: {}", filePath);
                yamlContent = yamlMapper.readValue(is, Map.class);
            }

            Object metadataObj = yamlContent.get("metadata");

            if (metadataObj instanceof Map) {
                Map<String, Object> metadata = (Map<String, Object>) metadataObj;
                Object typeObj = metadata.get("type");

                if (typeObj instanceof String) {
                    boolean isComponent = "component".equals(typeObj);
                    if (isComponent) {
                        logger.info("[OK] Detected component file: {}", filePath);
                    } else {
                        logger.debug("File is not a component (type={}): {}", typeObj, filePath);
                    }
                    return isComponent;
                }
            }

            logger.debug("File has no type metadata: {}", filePath);
            return false;
        } catch (Exception e) {
            logger.error("Failed to check if file is component: {} - {}", filePath, e.getMessage());
            logger.debug("Full exception details:", e);
            return false;
        }
    }

    /**
     * Parse YAML string into configuration.
     *
     * @param yamlString The YAML string to parse
     * @return The parsed configuration
     * @throws ConfigurationException if parsing fails
     */
    public YamlRuleConfiguration fromYamlString(String yamlString) throws ConfigurationException {
        // Resolve properties in the YAML string before parsing
        String resolvedYamlString = resolveProperties(yamlString);

        // Use OrderedYamlParser to preserve section order
        OrderedYamlConfiguration orderedConfig = orderedYamlParser.parseYamlString(resolvedYamlString, "<string>");
        YamlRuleConfiguration config = orderedConfig.getConfiguration();

        // Copy section order and item order into the configuration
        config.setSectionOrder(orderedConfig.getSectionOrder());
        config.setItemOrder(orderedConfig.getItemOrder());

        validateConfiguration(config);
        return config;
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

        // Configure mapper to fail on unknown properties (strict validation)
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        return mapper;
    }

    /**
     * Process external data-source references. Delegates to {@link ConfigurationReferenceResolver}.
     */
    private void processDataSourceReferences(YamlRuleConfiguration config) throws ConfigurationException {
        referenceResolver.processDataSourceReferences(config);
    }

    /**
     * Process external rule references. Delegates to {@link ConfigurationReferenceResolver}.
     */
    private void processRuleReferences(YamlRuleConfiguration config) throws ConfigurationException {
        referenceResolver.processRuleReferences(config);
    }

    /**
     * Process external enrichment references. Delegates to {@link ConfigurationReferenceResolver}.
     */
    private void processEnrichmentReferences(YamlRuleConfiguration config) throws ConfigurationException {
        referenceResolver.processEnrichmentReferences(config);
    }

    /**
     * Apply groups-only logic to filter itemOrder.
     * <p>
     * When enrichments/rules/groups are referenced by enrichment-groups/rule-groups,
     * they should only execute via the group (via flattening), not at their definition position.
     * <p>
     * This method must be called AFTER expandReferencePlaceholders() to ensure that:
     * - Enrichment-groups/rule-groups loaded from external files are in itemOrder
     * - Groups in the main file can reference and filter groups from external files
     * - All referenced items (enrichments, rules, enrichment-groups, rule-groups) are properly filtered
     * <p>
     * This method filters the itemOrder to remove enrichments/rules/groups that are referenced by groups,
     * so they only execute via the group (not at their definition position).
     *
     * @param config The configuration with itemOrder to filter
     */
    private void applyGroupsOnlyLogic(YamlRuleConfiguration config) {
        itemOrderProcessor.applyGroupsOnlyLogic(config);
    }

    /**
     * Expand reference placeholders in item order.
     * This replaces "*-refs" placeholders with actual items from referenced files.
     * Must be called AFTER processRuleReferences() and processEnrichmentReferences().
     *
     * @param config Configuration with item order and tracked referenced IDs
     */
    private void expandReferencePlaceholders(YamlRuleConfiguration config) {
        itemOrderProcessor.expandReferencePlaceholders(config);
    }

    /**
     * Load configuration from a file without processing references or validation.
     *
     * This method is used for multi-file loading where validation should happen
     * after all files are merged.
     *
     * @param filePath The file path to load
     * @return The loaded configuration (without processing or validation)
     * @throws ConfigurationException if loading fails
     */
    public YamlRuleConfiguration loadFromFileWithoutValidation(String filePath) throws ConfigurationException {
        return referenceResolver.loadFromFileWithoutProcessing(new File(filePath));
    }

    /**
     * Process rule references, data source references, and validate configuration.
     *
     * This method is used for multi-file loading after all files are merged.
     *
     * @param config The configuration to process and validate
     * @throws ConfigurationException if processing or validation fails
     */
    public void processReferencesAndValidate(YamlRuleConfiguration config) throws ConfigurationException {
        // Process external rule references
        processRuleReferences(config);

        // Process external enrichment references
        processEnrichmentReferences(config);

        // Process external data-source references
        processDataSourceReferences(config);

        // Expand reference placeholders in item order
        expandReferencePlaceholders(config);

        // Validate the complete merged configuration
        validateConfiguration(config);

        logger.info("Successfully processed references and validated merged configuration: " +
                   (config.getMetadata() != null ? config.getMetadata().getName() : "unnamed"));
    }

    /**
     * Validate the loaded configuration.
     *
     * @param config The configuration to validate
     * @throws ConfigurationException if validation fails
     */
    private void validateConfiguration(YamlRuleConfiguration config) throws ConfigurationException {
        if (config == null) {
            throw new ConfigurationException("Configuration is null");
        }

        // Step 1: Validate individual components (inline validation)
        inlineValidator.validateRules(config);
        inlineValidator.validateRuleGroups(config);
        inlineValidator.validateCategories(config);
        dataSourceValidator.validate(config);
        ruleChainValidator.validate(config);

        // Validate enrichments and enrichment groups
        enrichmentValidator.validate(config);

        // Step 2: Validate cross-component references
        crossReferenceValidator.validateCrossComponentReferences(config);
        crossReferenceValidator.validateEnrichmentReferences(config);

        // Step 3: Validate for duplicates
        duplicateValidator.validate(config);

        // Step 4: Validate no unresolved property placeholders remain
        validateNoUnresolvedPlaceholdersInConfiguration(config);

        logger.debug("Configuration validation completed successfully");
    }

    // ========================================================================
    // PROPERTY RESOLUTION METHODS (Phase 1 - Not Used Yet)
    // ========================================================================

    /**
     * Resolve environment variables and system properties in configuration values.
     * Delegates to the centralized PropertyResolver utility.
     * Supports: ${VAR}, ${VAR:default}, $(VAR), $(VAR:default)
     *
     * @param value The configuration value that may contain property placeholders
     * @return The value with resolved properties
     * @throws ConfigurationException if a required property is not found
     */
    private String resolveProperties(String value) throws ConfigurationException {
        try {
            return PropertyResolver.resolve(value, true);
        } catch (PropertyResolver.PropertyResolutionException e) {
            throw new ConfigurationException(e.getMessage(), e);
        }
    }

    /**
     * Validate that no unresolved property placeholders remain in the entire configuration.
     *
     * @param config The configuration to check for unresolved placeholders
     * @throws ConfigurationException if unresolved placeholders are found
     */
    private void validateNoUnresolvedPlaceholdersInConfiguration(YamlRuleConfiguration config) throws ConfigurationException {
        try {
            // Convert the configuration back to YAML string to check for placeholders
            String yamlString = yamlMapper.writeValueAsString(config);
            validateNoUnresolvedPlaceholders(yamlString);
        } catch (Exception e) {
            if (e instanceof ConfigurationException) {
                throw (ConfigurationException) e;
            }
            throw new ConfigurationException("Failed to validate configuration for unresolved placeholders", e);
        }
    }

    /**
     * Validate that no unresolved property placeholders remain in the value.
     * Delegates to the centralized PropertyResolver utility.
     *
     * @param value The value to check for unresolved placeholders
     * @throws ConfigurationException if unresolved placeholders are found
     */
    private void validateNoUnresolvedPlaceholders(String value) throws ConfigurationException {
        try {
            PropertyResolver.validateNoUnresolvedPlaceholders(value);
        } catch (PropertyResolver.PropertyResolutionException e) {
            throw new ConfigurationException(e.getMessage(), e);
        }
    }

}
