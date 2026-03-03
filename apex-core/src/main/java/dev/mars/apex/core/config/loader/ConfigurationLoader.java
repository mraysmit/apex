package dev.mars.apex.core.config.loader;

import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.sequential.OrderedYamlConfiguration;
import dev.mars.apex.core.config.sequential.OrderedYamlParser;
import dev.mars.apex.core.config.sequential.ProcessingItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.service.data.external.DataSourceResolver;
import dev.mars.apex.core.service.data.external.ExternalDataSourceConfig;
import dev.mars.apex.core.util.EnabledFilter;
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

            // Read raw content and resolve properties before parsing
            String rawContent = Files.readString(path);
            String resolvedContent = resolveProperties(rawContent);

            // Use OrderedYamlParser to preserve section order
            OrderedYamlConfiguration orderedConfig = orderedYamlParser.parseYamlString(resolvedContent, filePath);
            YamlRuleConfiguration config = orderedConfig.getConfiguration();

            // Copy section order and item order into the configuration
            List<String> sectionOrder = orderedConfig.getSectionOrder();
            config.setSectionOrder(sectionOrder);
            logger.debug("Section order from YAML: " + sectionOrder);

            List<ProcessingItem> itemOrder = orderedConfig.getItemOrder();
            config.setItemOrder(itemOrder);
            logger.debug("Item order from YAML: " + itemOrder.size() + " items");

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

            // Read raw content and resolve properties before parsing
            String rawContent = Files.readString(file.toPath());
            String resolvedContent = resolveProperties(rawContent);

            // Use OrderedYamlParser to preserve section order
            OrderedYamlConfiguration orderedConfig = orderedYamlParser.parseYamlString(resolvedContent, file.getAbsolutePath());
            YamlRuleConfiguration config = orderedConfig.getConfiguration();

            // Copy section order and item order into the configuration
            config.setSectionOrder(orderedConfig.getSectionOrder());
            config.setItemOrder(orderedConfig.getItemOrder());

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

            // Read raw content and resolve properties before parsing
            String rawContent = new String(inputStream.readAllBytes());
            String resolvedContent = resolveProperties(rawContent);

            // Use OrderedYamlParser to preserve section order
            OrderedYamlConfiguration orderedConfig = orderedYamlParser.parseYamlString(resolvedContent, "<stream>");
            YamlRuleConfiguration config = orderedConfig.getConfiguration();

            // Copy section order and item order into the configuration
            config.setSectionOrder(orderedConfig.getSectionOrder());
            config.setItemOrder(orderedConfig.getItemOrder());

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

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration from input stream", e);
        }
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
            return loadFromStream(inputStream);

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
     * Process external data-source references and merge them into the configuration.
     *
     * This method resolves external data-source references defined in the 'data-source-refs'
     * section and merges them with any existing inline data-sources.
     *
     * @param config The configuration to process
     * @throws ConfigurationException if reference resolution fails
     */
    private void processDataSourceReferences(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getDataSourceRefs() == null || config.getDataSourceRefs().isEmpty()) {
            logger.debug("No external data-source references to process");
            return;
        }

        logger.info("Processing " + config.getDataSourceRefs().size() + " external data-source references");

        // Initialize data-sources list if it doesn't exist
        if (config.getDataSources() == null) {
            config.setDataSources(new ArrayList<>());
        }

        // Process each data-source reference
        for (YamlDataSourceRef ref : config.getDataSourceRefs()) {
            if (!EnabledFilter.isEnabled(ref)) {
                logger.info("Skipping disabled data-source reference: " + ref.getName());
                continue;
            }

            try {
                logger.info("Resolving external data-source reference: " + ref.getName() + " from " + ref.getSource());

                // Resolve the external configuration
                ExternalDataSourceConfig externalConfig = dataSourceResolver.resolveDataSource(ref.getSource());

                // Convert external configuration to YamlDataSource
                YamlDataSource yamlDataSource = convertExternalToYamlDataSource(externalConfig, ref);

                // Add to the configuration
                config.getDataSources().add(yamlDataSource);

                logger.info("Successfully resolved and added data-source: " + ref.getName());

            } catch (Exception e) {
                throw new ConfigurationException(
                    "Failed to resolve data-source reference '" + ref.getName() + "' from '" + ref.getSource() + "'", e);
            }
        }

        logger.info("Successfully processed all external data-source references");
    }

    /**
     * Process external rule references in the configuration.
     *
     * This method loads external rule files referenced in the rule-refs
     * section and merges them with any existing inline rules.
     *
     * @param config The configuration to process
     * @throws ConfigurationException if reference resolution fails
     */
    private void processRuleReferences(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getRuleRefs() == null || config.getRuleRefs().isEmpty()) {
            logger.debug("No external rule references to process");
            return;
        }

        logger.info("Processing " + config.getRuleRefs().size() + " external rule references");

        // Track referenced rule IDs and rule group IDs (use LinkedHashSet to preserve order)
        Set<String> referencedRuleIds = new LinkedHashSet<>();
        Set<String> referencedRuleGroupIds = new LinkedHashSet<>();

        // Track loaded files to prevent duplicates across all rule-refs
        Set<String> loadedFiles = new HashSet<>();

        // Process each rule reference
        for (YamlRuleRef ref : config.getRuleRefs()) {
            if (!EnabledFilter.isEnabled(ref)) {
                logger.info("Skipping disabled rule reference: " + ref.getName());
                continue;
            }

            try {
                logger.info("Resolving external rule reference: " + ref.getName() + " from " + ref.getSource());

                // Load the referenced rule file recursively with shared loadedFiles set
                YamlRuleConfiguration referencedConfig = loadRuleFileRecursive(ref.getSource(), loadedFiles);

                // Check if external file has BOTH rules and rule-groups
                boolean hasRules = referencedConfig.getRules() != null && !referencedConfig.getRules().isEmpty();
                boolean hasRuleGroups = referencedConfig.getRuleGroups() != null && !referencedConfig.getRuleGroups().isEmpty();

                // Merge rules from referenced file
                if (hasRules) {
                    // Initialize rules list if it doesn't exist and we have rules to add
                    if (config.getRules() == null) {
                        config.setRules(new ArrayList<>());
                    }

                    // Track IDs ONLY if there are NO rule-groups
                    // When groups exist, rules are DEFINITIONS ONLY
                    if (!hasRuleGroups) {
                        for (YamlRule rule : referencedConfig.getRules()) {
                            if (rule.getId() != null) {
                                referencedRuleIds.add(rule.getId());
                            }
                        }
                        logger.info("Tracked " + referencedConfig.getRules().size() + " rule IDs for execution (no groups present)");
                    } else {
                        logger.info("Skipped tracking rule IDs (rule-groups present - rules are definitions only)");
                    }

                    config.getRules().addAll(referencedConfig.getRules());
                    logger.info("Merged " + referencedConfig.getRules().size() + " rules from: " + ref.getName());
                }

                // Merge rule groups from referenced file
                if (hasRuleGroups) {
                    // Initialize rule groups list if it doesn't exist and we have groups to add
                    if (config.getRuleGroups() == null) {
                        config.setRuleGroups(new ArrayList<>());
                    }

                    // Track group IDs for execution
                    for (YamlRuleGroup group : referencedConfig.getRuleGroups()) {
                        if (group.getId() != null) {
                            referencedRuleGroupIds.add(group.getId());
                        }
                    }

                    config.getRuleGroups().addAll(referencedConfig.getRuleGroups());
                    logger.info("Merged " + referencedConfig.getRuleGroups().size() + " rule groups from: " + ref.getName());
                }

                logger.info("Successfully resolved and merged rules from: " + ref.getName());

            } catch (Exception e) {
                throw new ConfigurationException(
                    "Failed to resolve rule reference '" + ref.getName() + "' from '" + ref.getSource() + "'", e);
            }
        }

        // Store tracked IDs in configuration
        config.setReferencedRuleIds(referencedRuleIds);
        config.setReferencedRuleGroupIds(referencedRuleGroupIds);

        logger.info("Successfully processed all external rule references (tracked " +
                   referencedRuleIds.size() + " rules, " + referencedRuleGroupIds.size() + " rule groups)");
    }

    /**
     * Process external rule references recursively with cycle detection.
     *
     * This method loads external rule files referenced in the rule-refs
     * section and merges them with any existing inline rules. It supports
     * nested rule-refs by recursively processing referenced files.
     *
     * @param config The configuration to process
     * @param loadedFiles Set of already loaded files to detect cycles
     * @throws ConfigurationException if reference resolution fails
     */
    private void processRuleReferencesRecursive(YamlRuleConfiguration config, Set<String> loadedFiles) throws ConfigurationException {
        if (config.getRuleRefs() == null || config.getRuleRefs().isEmpty()) {
            logger.debug("No external rule references to process");
            return;
        }

        logger.debug("Processing " + config.getRuleRefs().size() + " external rule references (recursive)");

        // Track referenced rule IDs and rule group IDs (use LinkedHashSet to preserve order)
        Set<String> referencedRuleIds = new LinkedHashSet<>();
        Set<String> referencedRuleGroupIds = new LinkedHashSet<>();

        // Process each rule reference
        for (YamlRuleRef ref : config.getRuleRefs()) {
            if (!EnabledFilter.isEnabled(ref)) {
                logger.debug("Skipping disabled rule reference: " + ref.getName());
                continue;
            }

            try {
                logger.debug("Resolving external rule reference (recursive): " + ref.getName() + " from " + ref.getSource());

                // Load the referenced rule file recursively
                YamlRuleConfiguration referencedConfig = loadRuleFileRecursive(ref.getSource(), loadedFiles);

                // Check if external file has BOTH rules and rule-groups
                boolean hasRules = referencedConfig.getRules() != null && !referencedConfig.getRules().isEmpty();
                boolean hasRuleGroups = referencedConfig.getRuleGroups() != null && !referencedConfig.getRuleGroups().isEmpty();

                // Merge rules from referenced file
                if (hasRules) {
                    // Initialize rules list if it doesn't exist and we have rules to add
                    if (config.getRules() == null) {
                        config.setRules(new ArrayList<>());
                    }

                    // Track IDs ONLY if there are NO rule-groups
                    // When groups exist, rules are DEFINITIONS ONLY
                    if (!hasRuleGroups) {
                        for (YamlRule rule : referencedConfig.getRules()) {
                            if (rule.getId() != null) {
                                referencedRuleIds.add(rule.getId());
                            }
                        }
                        logger.debug("Tracked " + referencedConfig.getRules().size() + " rule IDs for execution (no groups present)");
                    } else {
                        logger.debug("Skipped tracking rule IDs (rule-groups present - rules are definitions only)");
                    }

                    config.getRules().addAll(referencedConfig.getRules());
                    logger.debug("Merged " + referencedConfig.getRules().size() + " rules from: " + ref.getName());
                }

                // Merge rule groups from referenced file
                if (hasRuleGroups) {
                    // Initialize rule groups list if it doesn't exist and we have groups to add
                    if (config.getRuleGroups() == null) {
                        config.setRuleGroups(new ArrayList<>());
                    }

                    // Track group IDs for execution
                    for (YamlRuleGroup group : referencedConfig.getRuleGroups()) {
                        if (group.getId() != null) {
                            referencedRuleGroupIds.add(group.getId());
                        }
                    }

                    config.getRuleGroups().addAll(referencedConfig.getRuleGroups());
                    logger.debug("Merged " + referencedConfig.getRuleGroups().size() + " rule groups from: " + ref.getName());
                }

                logger.debug("Successfully resolved and merged rules from: " + ref.getName());

            } catch (Exception e) {
                throw new ConfigurationException(
                    "Failed to resolve rule reference '" + ref.getName() + "' from '" + ref.getSource() + "'", e);
            }
        }

        // Store tracked IDs in configuration
        config.setReferencedRuleIds(referencedRuleIds);
        config.setReferencedRuleGroupIds(referencedRuleGroupIds);

        logger.debug("Successfully processed all external rule references (recursive) (tracked " +
                   referencedRuleIds.size() + " rules, " + referencedRuleGroupIds.size() + " rule groups)");
    }

    /**
     * Process external enrichment file references.
     *
     * This method loads external enrichment files referenced in the enrichment-refs
     * section and merges them with any existing inline enrichments and enrichment groups.
     *
     * @param config The configuration to process
     * @throws ConfigurationException if reference resolution fails
     */
    private void processEnrichmentReferences(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getEnrichmentRefs() == null || config.getEnrichmentRefs().isEmpty()) {
            logger.debug("No external enrichment references to process");
            return;
        }

        logger.info("Processing " + config.getEnrichmentRefs().size() + " external enrichment references");

        // Track referenced enrichment IDs and enrichment group IDs (use LinkedHashSet to preserve order)
        Set<String> referencedEnrichmentIds = new LinkedHashSet<>();
        Set<String> referencedEnrichmentGroupIds = new LinkedHashSet<>();

        // Track loaded files to prevent duplicates across all enrichment-refs
        Set<String> loadedFiles = new HashSet<>();

        // Process each enrichment reference
        for (YamlEnrichmentRef ref : config.getEnrichmentRefs()) {
            if (!EnabledFilter.isEnabled(ref)) {
                logger.info("Skipping disabled enrichment reference: " + ref.getName());
                continue;
            }

            try {
                logger.info("Resolving external enrichment reference: " + ref.getName() + " from " + ref.getSource());

                // Load the referenced enrichment file recursively with shared loadedFiles set
                YamlRuleConfiguration referencedConfig = loadRuleFileRecursive(ref.getSource(), loadedFiles);

                // Check if external file has enrichments and/or enrichment-groups
                boolean hasEnrichments = referencedConfig.getEnrichments() != null && !referencedConfig.getEnrichments().isEmpty();
                boolean hasEnrichmentGroups = referencedConfig.getEnrichmentGroups() != null && !referencedConfig.getEnrichmentGroups().isEmpty();

                // Collect enrichment IDs that are referenced by enrichment-groups
                Set<String> referencedByGroups = new HashSet<>();
                if (hasEnrichmentGroups) {
                    for (YamlEnrichmentGroup group : referencedConfig.getEnrichmentGroups()) {
                        if (group.getEnrichmentIds() != null) {
                            referencedByGroups.addAll(group.getEnrichmentIds());
                        }
                    }
                    logger.debug("Found " + referencedByGroups.size() + " enrichment IDs referenced by groups: " + referencedByGroups);
                }

                // Merge enrichments from referenced file
                if (hasEnrichments) {
                    // Initialize enrichments list if it doesn't exist and we have enrichments to add
                    if (config.getEnrichments() == null) {
                        config.setEnrichments(new ArrayList<>());
                    }

                    // Track enrichment IDs that are NOT referenced by any enrichment-group
                    // Enrichments referenced by groups are definitions only (executed by the group)
                    // Enrichments NOT referenced by groups execute directly
                    int trackedCount = 0;
                    int skippedCount = 0;
                    for (YamlEnrichment enrichment : referencedConfig.getEnrichments()) {
                        if (enrichment.getId() != null) {
                            if (!referencedByGroups.contains(enrichment.getId())) {
                                // Not referenced by any group - track for direct execution
                                referencedEnrichmentIds.add(enrichment.getId());
                                trackedCount++;
                            } else {
                                // Referenced by a group - skip tracking (definition only)
                                skippedCount++;
                            }
                        }
                    }

                    if (hasEnrichmentGroups) {
                        logger.info("Tracked " + trackedCount + " standalone enrichments for execution, " +
                                   "skipped " + skippedCount + " enrichments (referenced by groups - definitions only)");
                    } else {
                        logger.info("Tracked " + trackedCount + " enrichment IDs for execution (no groups present)");
                    }

                    config.getEnrichments().addAll(referencedConfig.getEnrichments());
                    logger.info("Merged " + referencedConfig.getEnrichments().size() + " enrichments from: " + ref.getName());
                }

                // Merge enrichment groups from referenced file
                if (hasEnrichmentGroups) {
                    // Initialize enrichment groups list if it doesn't exist and we have groups to add
                    if (config.getEnrichmentGroups() == null) {
                        config.setEnrichmentGroups(new ArrayList<>());
                    }

                    // Track group IDs for execution
                    for (YamlEnrichmentGroup group : referencedConfig.getEnrichmentGroups()) {
                        if (group.getId() != null) {
                            referencedEnrichmentGroupIds.add(group.getId());
                        }
                    }

                    config.getEnrichmentGroups().addAll(referencedConfig.getEnrichmentGroups());
                    logger.info("Merged " + referencedConfig.getEnrichmentGroups().size() + " enrichment groups from: " + ref.getName());
                }

                logger.info("Successfully resolved and merged enrichments from: " + ref.getName());

            } catch (Exception e) {
                throw new ConfigurationException(
                    "Failed to resolve enrichment reference '" + ref.getName() + "' from '" + ref.getSource() + "'", e);
            }
        }

        // Store tracked IDs in configuration
        config.setReferencedEnrichmentIds(referencedEnrichmentIds);
        config.setReferencedEnrichmentGroupIds(referencedEnrichmentGroupIds);

        logger.info("Successfully processed all external enrichment references (tracked " +
                   referencedEnrichmentIds.size() + " enrichments, " + referencedEnrichmentGroupIds.size() + " enrichment groups)");
    }

    /**
     * Process external enrichment references recursively with cycle detection.
     *
     * This method loads external enrichment files referenced in the enrichment-refs
     * section and merges them with any existing inline enrichments and enrichment groups.
     * It supports nested enrichment-refs by recursively processing referenced files.
     *
     * @param config The configuration to process
     * @param loadedFiles Set of already loaded files to detect cycles
     * @throws ConfigurationException if reference resolution fails
     */
    private void processEnrichmentReferencesRecursive(YamlRuleConfiguration config, Set<String> loadedFiles) throws ConfigurationException {
        if (config.getEnrichmentRefs() == null || config.getEnrichmentRefs().isEmpty()) {
            logger.debug("No external enrichment references to process");
            return;
        }

        logger.debug("Processing " + config.getEnrichmentRefs().size() + " external enrichment references (recursive)");

        // Track referenced enrichment IDs and enrichment group IDs (use LinkedHashSet to preserve order)
        Set<String> referencedEnrichmentIds = new LinkedHashSet<>();
        Set<String> referencedEnrichmentGroupIds = new LinkedHashSet<>();

        // Process each enrichment reference
        for (YamlEnrichmentRef ref : config.getEnrichmentRefs()) {
            if (!EnabledFilter.isEnabled(ref)) {
                logger.debug("Skipping disabled enrichment reference: " + ref.getName());
                continue;
            }

            try {
                logger.debug("Resolving external enrichment reference (recursive): " + ref.getName() + " from " + ref.getSource());

                // Load the referenced enrichment file recursively
                YamlRuleConfiguration referencedConfig = loadRuleFileRecursive(ref.getSource(), loadedFiles);

                // Check if external file has enrichments and/or enrichment-groups
                boolean hasEnrichments = referencedConfig.getEnrichments() != null && !referencedConfig.getEnrichments().isEmpty();
                boolean hasEnrichmentGroups = referencedConfig.getEnrichmentGroups() != null && !referencedConfig.getEnrichmentGroups().isEmpty();

                // Collect enrichment IDs that are referenced by enrichment-groups
                Set<String> referencedByGroups = new HashSet<>();
                if (hasEnrichmentGroups) {
                    for (YamlEnrichmentGroup group : referencedConfig.getEnrichmentGroups()) {
                        if (group.getEnrichmentIds() != null) {
                            referencedByGroups.addAll(group.getEnrichmentIds());
                        }
                    }
                    logger.debug("Found " + referencedByGroups.size() + " enrichment IDs referenced by groups: " + referencedByGroups);
                }

                // Merge enrichments from referenced file
                if (hasEnrichments) {
                    // Initialize enrichments list if it doesn't exist and we have enrichments to add
                    if (config.getEnrichments() == null) {
                        config.setEnrichments(new ArrayList<>());
                    }

                    // Track enrichment IDs that are NOT referenced by any enrichment-group
                    // Enrichments referenced by groups are definitions only (executed by the group)
                    // Enrichments NOT referenced by groups execute directly
                    int trackedCount = 0;
                    int skippedCount = 0;
                    for (YamlEnrichment enrichment : referencedConfig.getEnrichments()) {
                        if (enrichment.getId() != null) {
                            if (!referencedByGroups.contains(enrichment.getId())) {
                                // Not referenced by any group - track for direct execution
                                referencedEnrichmentIds.add(enrichment.getId());
                                trackedCount++;
                            } else {
                                // Referenced by a group - skip tracking (definition only)
                                skippedCount++;
                            }
                        }
                    }

                    if (hasEnrichmentGroups) {
                        logger.debug("Tracked " + trackedCount + " standalone enrichments for execution, " +
                                   "skipped " + skippedCount + " enrichments (referenced by groups - definitions only)");
                    } else {
                        logger.debug("Tracked " + trackedCount + " enrichment IDs for execution (no groups present)");
                    }

                    config.getEnrichments().addAll(referencedConfig.getEnrichments());
                    logger.debug("Merged " + referencedConfig.getEnrichments().size() + " enrichments from: " + ref.getName());
                }

                // Merge enrichment groups from referenced file
                if (hasEnrichmentGroups) {
                    // Initialize enrichment groups list if it doesn't exist and we have groups to add
                    if (config.getEnrichmentGroups() == null) {
                        config.setEnrichmentGroups(new ArrayList<>());
                    }

                    // Track group IDs for execution
                    for (YamlEnrichmentGroup group : referencedConfig.getEnrichmentGroups()) {
                        if (group.getId() != null) {
                            referencedEnrichmentGroupIds.add(group.getId());
                        }
                    }

                    config.getEnrichmentGroups().addAll(referencedConfig.getEnrichmentGroups());
                    logger.debug("Merged " + referencedConfig.getEnrichmentGroups().size() + " enrichment groups from: " + ref.getName());
                }

                logger.debug("Successfully resolved and merged enrichments from: " + ref.getName());

            } catch (Exception e) {
                throw new ConfigurationException(
                    "Failed to resolve enrichment reference '" + ref.getName() + "' from '" + ref.getSource() + "'", e);
            }
        }

        // Store tracked IDs in configuration
        config.setReferencedEnrichmentIds(referencedEnrichmentIds);
        config.setReferencedEnrichmentGroupIds(referencedEnrichmentGroupIds);

        logger.debug("Successfully processed all external enrichment references (recursive) (tracked " +
                   referencedEnrichmentIds.size() + " enrichments, " + referencedEnrichmentGroupIds.size() + " enrichment groups)");
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
        logger.info("=== APPLYING GROUPS-ONLY LOGIC ===");

        if (config.getItemOrder() == null || config.getItemOrder().isEmpty()) {
            logger.info("No item order to filter - skipping groups-only logic");
            return;
        }

        logger.info("Item order size BEFORE filtering: " + config.getItemOrder().size());

        // Collect enrichment IDs referenced by enrichment-groups (use LinkedHashSet to preserve order)
        Set<String> referencedEnrichmentIds = new LinkedHashSet<>();
        if (config.getEnrichmentGroups() != null && !config.getEnrichmentGroups().isEmpty()) {
            for (YamlEnrichmentGroup group : config.getEnrichmentGroups()) {
                // Collect from enrichment-ids (simple string list)
                if (group.getEnrichmentIds() != null) {
                    referencedEnrichmentIds.addAll(group.getEnrichmentIds());
                }
                // Collect from enrichment-references (structured objects with enrichment-id field)
                if (group.getEnrichmentReferences() != null) {
                    for (YamlEnrichmentGroup.EnrichmentReference ref : group.getEnrichmentReferences()) {
                        if (ref.getEnrichmentId() != null) {
                            referencedEnrichmentIds.add(ref.getEnrichmentId());
                        }
                    }
                }
            }
            logger.info("Found " + referencedEnrichmentIds.size() + " enrichment IDs referenced by groups: " + referencedEnrichmentIds);
        }

        // Collect rule IDs referenced by rule-groups (use LinkedHashSet to preserve order)
        Set<String> referencedRuleIds = new LinkedHashSet<>();
        if (config.getRuleGroups() != null && !config.getRuleGroups().isEmpty()) {
            for (YamlRuleGroup group : config.getRuleGroups()) {
                // Collect from rule-ids (simple string list)
                if (group.getRuleIds() != null) {
                    referencedRuleIds.addAll(group.getRuleIds());
                }
                // Collect from rule-references (structured objects with rule-id field)
                if (group.getRuleReferences() != null) {
                    for (YamlRuleGroup.RuleReference ref : group.getRuleReferences()) {
                        if (ref.getRuleId() != null) {
                            referencedRuleIds.add(ref.getRuleId());
                        }
                    }
                }
            }
            logger.info("Found " + referencedRuleIds.size() + " rule IDs referenced by groups: " + referencedRuleIds);
        }

        // Collect enrichment-group IDs referenced by other enrichment-groups (use LinkedHashSet to preserve order)
        Set<String> referencedEnrichmentGroupIds = new LinkedHashSet<>();
        if (config.getEnrichmentGroups() != null && !config.getEnrichmentGroups().isEmpty()) {
            for (YamlEnrichmentGroup group : config.getEnrichmentGroups()) {
                if (group.getEnrichmentGroupReferences() != null) {
                    referencedEnrichmentGroupIds.addAll(group.getEnrichmentGroupReferences());
                }
            }
            logger.info("Found " + referencedEnrichmentGroupIds.size() + " enrichment-group IDs referenced by other groups: " + referencedEnrichmentGroupIds);
        }

        // Collect rule-group IDs referenced by other rule-groups (use LinkedHashSet to preserve order)
        Set<String> referencedRuleGroupIds = new LinkedHashSet<>();
        if (config.getRuleGroups() != null && !config.getRuleGroups().isEmpty()) {
            for (YamlRuleGroup group : config.getRuleGroups()) {
                if (group.getRuleGroupReferences() != null) {
                    referencedRuleGroupIds.addAll(group.getRuleGroupReferences());
                }
            }
            logger.info("Found " + referencedRuleGroupIds.size() + " rule-group IDs referenced by other groups: " + referencedRuleGroupIds);
        }

        // If no groups exist, no filtering needed
        if (referencedEnrichmentIds.isEmpty() && referencedRuleIds.isEmpty() &&
            referencedEnrichmentGroupIds.isEmpty() && referencedRuleGroupIds.isEmpty()) {
            logger.info("No groups found - skipping groups-only logic (all items execute at definition position)");
            return;
        }

        // Filter itemOrder: Remove enrichments/rules/groups referenced by groups
        List<ProcessingItem> originalOrder = new ArrayList<>(config.getItemOrder());
        List<ProcessingItem> filteredOrder = new ArrayList<>();
        int enrichmentsFiltered = 0;
        int rulesFiltered = 0;
        int enrichmentGroupsFiltered = 0;
        int ruleGroupsFiltered = 0;

        for (ProcessingItem item : originalOrder) {
            boolean shouldRemove = false;

            if ("enrichments".equals(item.getSectionType()) &&
                referencedEnrichmentIds.contains(item.getItemId())) {
                shouldRemove = true;  // Skip - will execute via enrichment-group
                enrichmentsFiltered++;
                logger.debug("Filtering enrichment '" + item.getItemId() + "' from itemOrder (referenced by group - definition only)");
            } else if ("rules".equals(item.getSectionType()) &&
                       referencedRuleIds.contains(item.getItemId())) {
                shouldRemove = true;  // Skip - will execute via rule-group
                rulesFiltered++;
                logger.debug("Filtering rule '" + item.getItemId() + "' from itemOrder (referenced by group - definition only)");
            } else if ("enrichment-groups".equals(item.getSectionType()) &&
                       referencedEnrichmentGroupIds.contains(item.getItemId())) {
                shouldRemove = true;  // Skip - will execute via parent enrichment-group
                enrichmentGroupsFiltered++;
                logger.debug("Filtering enrichment-group '" + item.getItemId() + "' from itemOrder (referenced by another group - definition only)");
            } else if ("rule-groups".equals(item.getSectionType()) &&
                       referencedRuleGroupIds.contains(item.getItemId())) {
                shouldRemove = true;  // Skip - will execute via parent rule-group
                ruleGroupsFiltered++;
                logger.debug("Filtering rule-group '" + item.getItemId() + "' from itemOrder (referenced by another group - definition only)");
            }

            if (!shouldRemove) {
                filteredOrder.add(item);
            }
        }

        // Update configuration with filtered order
        config.setItemOrder(filteredOrder);

        logger.info("Applied groups-only logic: filtered " + enrichmentsFiltered + " enrichments, " +
                   rulesFiltered + " rules, " + enrichmentGroupsFiltered + " enrichment-groups, and " +
                   ruleGroupsFiltered + " rule-groups from itemOrder (original: " + originalOrder.size() +
                   " items, filtered: " + filteredOrder.size() + " items)");
    }

    /**
     * Expand reference placeholders in item order.
     * This replaces "*-refs" placeholders with actual items from referenced files.
     * Must be called AFTER processRuleReferences() and processEnrichmentReferences().
     *
     * @param config Configuration with item order and tracked referenced IDs
     */
    private void expandReferencePlaceholders(YamlRuleConfiguration config) {
        if (config.getItemOrder() == null || config.getItemOrder().isEmpty()) {
            logger.debug("No item order to expand");
            return;
        }

        List<ProcessingItem> expandedOrder = new ArrayList<>();
        int originalSize = config.getItemOrder().size();

        for (ProcessingItem item : config.getItemOrder()) {
            String sectionType = item.getSectionType();
            String itemId = item.getItemId();

            if (sectionType.equals("enrichment-refs") && itemId.equals("*")) {
                // Expand enrichment references
                logger.debug("Expanding enrichment-refs placeholder");

                if (config.getReferencedEnrichmentIds() != null) {
                    for (String enrichmentId : config.getReferencedEnrichmentIds()) {
                        expandedOrder.add(new ProcessingItem("enrichments", enrichmentId));
                        logger.debug("  Added enrichment: " + enrichmentId);
                    }
                }

                if (config.getReferencedEnrichmentGroupIds() != null) {
                    for (String groupId : config.getReferencedEnrichmentGroupIds()) {
                        expandedOrder.add(new ProcessingItem("enrichment-groups", groupId));
                        logger.debug("  Added enrichment-group: " + groupId);
                    }
                }
            } else if (sectionType.equals("rule-refs") && itemId.equals("*")) {
                // Expand rule references
                logger.debug("Expanding rule-refs placeholder");

                if (config.getReferencedRuleIds() != null) {
                    for (String ruleId : config.getReferencedRuleIds()) {
                        expandedOrder.add(new ProcessingItem("rules", ruleId));
                        logger.debug("  Added rule: " + ruleId);
                    }
                }

                if (config.getReferencedRuleGroupIds() != null) {
                    for (String groupId : config.getReferencedRuleGroupIds()) {
                        expandedOrder.add(new ProcessingItem("rule-groups", groupId));
                        logger.debug("  Added rule-group: " + groupId);
                    }
                }
            } else {
                // Keep non-placeholder items as-is
                expandedOrder.add(item);
            }
        }

        config.setItemOrder(expandedOrder);
        logger.info("Expanded item order from " + originalSize + " to " + expandedOrder.size() + " items");
    }

    /**
     * Load a rule file recursively with cycle detection and duplicate prevention.
     *
     * @param source The source path (file system or classpath)
     * @param loadedFiles Set of already loaded files to detect cycles and prevent duplicates
     * @return The loaded rule configuration
     * @throws ConfigurationException if loading fails or cycle detected
     */
    private YamlRuleConfiguration loadRuleFileRecursive(String source, Set<String> loadedFiles) throws ConfigurationException {
        // Normalize path for cycle detection and duplicate prevention
        String normalizedSource = Paths.get(source).normalize().toString();

        // Check if already loaded - if so, return empty config to avoid duplicates
        if (loadedFiles.contains(normalizedSource)) {
            logger.debug("Skipping already loaded file: " + source);
            // Return empty config - rules/enrichments from this file were already merged
            YamlRuleConfiguration emptyConfig = new YamlRuleConfiguration();
            emptyConfig.setRules(new ArrayList<>());
            emptyConfig.setRuleGroups(new ArrayList<>());
            emptyConfig.setEnrichments(new ArrayList<>());
            emptyConfig.setEnrichmentGroups(new ArrayList<>());
            return emptyConfig;
        }

        // Add to loaded files set (stays in set for entire loading process)
        loadedFiles.add(normalizedSource);

        try {
            // Try file system first, then classpath (same pattern as DataSourceResolver)
            Path path = Paths.get(source);
            YamlRuleConfiguration config;

            if (Files.exists(path)) {
                // Load from file system
                logger.debug("Loading rule file from file system: " + source);
                config = loadFromFileWithoutProcessing(path.toFile());
            } else {
                // Load from classpath
                logger.debug("Loading rule file from classpath: " + source);
                config = loadFromClasspathWithoutProcessing(source);
            }

            // Recursively process rule-refs in the loaded file
            if (config.getRuleRefs() != null && !config.getRuleRefs().isEmpty()) {
                logger.debug("Processing " + config.getRuleRefs().size() + " nested rule-refs in: " + source);
                processRuleReferencesRecursive(config, loadedFiles);
            }

            // Recursively process enrichment-refs in the loaded file
            if (config.getEnrichmentRefs() != null && !config.getEnrichmentRefs().isEmpty()) {
                logger.debug("Processing " + config.getEnrichmentRefs().size() + " nested enrichment-refs in: " + source);
                processEnrichmentReferencesRecursive(config, loadedFiles);
            }

            return config;

        } catch (Exception e) {
            throw new ConfigurationException("Failed to load rule file: " + source, e);
        }
    }

    /**
     * Load configuration from a file without processing rule-refs or data-source-refs.
     *
     * This method is used when loading referenced rule files to avoid infinite recursion.
     *
     * @param file The file to load
     * @return The loaded configuration (without processing)
     * @throws ConfigurationException if loading fails
     */
    private YamlRuleConfiguration loadFromFileWithoutProcessing(File file) throws ConfigurationException {
        try {
            if (!file.exists()) {
                throw new ConfigurationException("Configuration file not found: " + file.getAbsolutePath());
            }

            logger.debug("Loading YAML configuration from file (without processing): " + file.getAbsolutePath());

            // Read raw content and resolve properties before parsing
            String rawContent = Files.readString(file.toPath());
            String resolvedContent = resolveProperties(rawContent);

            YamlRuleConfiguration config = yamlMapper.readValue(resolvedContent, YamlRuleConfiguration.class);

            // Skip processRuleReferences() and processDataSourceReferences() to avoid recursion
            // Skip validateConfiguration() as this will be done on the merged configuration

            logger.debug("Successfully loaded configuration (without processing): " +
                       (config.getMetadata() != null ? config.getMetadata().getName() : "unnamed"));

            return config;

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration from file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Load configuration from classpath without processing rule-refs or data-source-refs.
     *
     * This method is used when loading referenced rule files to avoid infinite recursion.
     *
     * @param resourcePath The classpath resource path
     * @return The loaded configuration (without processing)
     * @throws ConfigurationException if loading fails
     */
    private YamlRuleConfiguration loadFromClasspathWithoutProcessing(String resourcePath) throws ConfigurationException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new ConfigurationException("Configuration resource not found: " + resourcePath);
            }

            logger.debug("Loading YAML configuration from classpath (without processing): " + resourcePath);

            // Read raw content and resolve properties before parsing
            String rawContent = new String(inputStream.readAllBytes());
            String resolvedContent = resolveProperties(rawContent);

            YamlRuleConfiguration config = yamlMapper.readValue(resolvedContent, YamlRuleConfiguration.class);

            // Skip processRuleReferences() and processDataSourceReferences() to avoid recursion
            // Skip validateConfiguration() as this will be done on the merged configuration

            logger.debug("Successfully loaded configuration (without processing): " +
                       (config.getMetadata() != null ? config.getMetadata().getName() : "unnamed"));

            return config;

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration from classpath: " + resourcePath, e);
        }
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
        return loadFromFileWithoutProcessing(new File(filePath));
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
     * @throws ConfigurationException if validation fails
     */
    private void validateConfiguration(YamlRuleConfiguration config) throws ConfigurationException {
        if (config == null) {
            throw new ConfigurationException("Configuration is null");
        }

        // Step 1: Validate individual components
        validateRules(config);
        validateRuleGroups(config);
        validateCategories(config);
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

    /**
     * Validate all rules in the configuration.
     */
    private void validateRules(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getRules() != null) {
            for (YamlRule rule : config.getRules()) {
                validateRule(rule);
            }
        }
    }

    /**
     * Validate a rule configuration.
     */
    private void validateRule(YamlRule rule) throws ConfigurationException {
        if (rule.getId() == null || rule.getId().trim().isEmpty()) {
            throw new ConfigurationException("Rule ID is required");
        }
        if (rule.getName() == null || rule.getName().trim().isEmpty()) {
            throw new ConfigurationException("Rule name is required for rule: " + rule.getId());
        }
        if (rule.getCondition() == null || rule.getCondition().trim().isEmpty()) {
            throw new ConfigurationException("Rule condition is required for rule: " + rule.getId());
        }

        // Validate severity if present
        if (rule.getSeverity() != null) {
            String severity = rule.getSeverity().trim().toUpperCase();
            if (!SeverityConstants.VALID_SEVERITIES.contains(severity)) {
                throw new ConfigurationException("Rule '" + rule.getId() + "' has invalid severity '" +
                    rule.getSeverity() + "'. Must be one of: " + String.join(", ", SeverityConstants.VALID_SEVERITIES));
            }
        }
    }

    /**
     * Validate all rule groups in the configuration.
     */
    private void validateRuleGroups(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getRuleGroups() != null) {
            for (YamlRuleGroup group : config.getRuleGroups()) {
                validateRuleGroup(group);
            }
        }
    }

    /**
     * Validate a rule group configuration.
     */
    private void validateRuleGroup(YamlRuleGroup group) throws ConfigurationException {
        if (group.getId() == null || group.getId().trim().isEmpty()) {
            throw new ConfigurationException("Rule group ID is required");
        }
        if (group.getName() == null || group.getName().trim().isEmpty()) {
            throw new ConfigurationException("Rule group name is required for group: " + group.getId());
        }
    }

    /**
     * Validate all categories in the configuration.
     */
    private void validateCategories(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getCategories() != null) {
            for (YamlCategory category : config.getCategories()) {
                validateCategory(category);
            }
        }
    }

    /**
     * Validate a category configuration.
     */
    private void validateCategory(YamlCategory category) throws ConfigurationException {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new ConfigurationException("Category name is required");
        }
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
