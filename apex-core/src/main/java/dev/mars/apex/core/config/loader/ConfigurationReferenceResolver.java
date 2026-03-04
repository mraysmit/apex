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
package dev.mars.apex.core.config.loader;

import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.service.data.external.DataSourceResolver;
import dev.mars.apex.core.service.data.external.ExternalDataSourceConfig;
import dev.mars.apex.core.util.EnabledFilter;
import dev.mars.apex.core.util.PropertyResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Resolves external references (rule-refs, enrichment-refs, data-source-refs)
 * in YAML configurations and merges them into the parent configuration.
 *
 * <p>Extracted from {@link ConfigurationLoader} to separate reference resolution
 * concerns from the main loading orchestration.
 *
 * <p>This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
class ConfigurationReferenceResolver {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationReferenceResolver.class);

    private final ObjectMapper yamlMapper;
    private final DataSourceResolver dataSourceResolver;

    /**
     * Constructor.
     *
     * @param yamlMapper        The YAML object mapper for parsing configuration files
     * @param dataSourceResolver The resolver for external data-source configurations
     */
    ConfigurationReferenceResolver(ObjectMapper yamlMapper, DataSourceResolver dataSourceResolver) {
        this.yamlMapper = yamlMapper;
        this.dataSourceResolver = dataSourceResolver;
    }

    // ========================================================================
    // RULE REFERENCE PROCESSING
    // ========================================================================

    /**
     * Process external rule references in the configuration.
     *
     * <p>This method loads external rule files referenced in the rule-refs
     * section and merges them with any existing inline rules.
     *
     * @param config The configuration to process
     * @throws ConfigurationException if reference resolution fails
     */
    void processRuleReferences(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getRuleRefs() == null || config.getRuleRefs().isEmpty()) {
            logger.debug("No external rule references to process");
            return;
        }
        logger.info("Processing " + config.getRuleRefs().size() + " external rule references");
        processRuleReferencesRecursive(config, new HashSet<>());
    }

    /**
     * Process external rule references recursively with cycle detection.
     *
     * <p>This method loads external rule files referenced in the rule-refs
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

    // ========================================================================
    // ENRICHMENT REFERENCE PROCESSING
    // ========================================================================

    /**
     * Process external enrichment file references.
     *
     * <p>This method loads external enrichment files referenced in the enrichment-refs
     * section and merges them with any existing inline enrichments and enrichment groups.
     *
     * @param config The configuration to process
     * @throws ConfigurationException if reference resolution fails
     */
    void processEnrichmentReferences(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getEnrichmentRefs() == null || config.getEnrichmentRefs().isEmpty()) {
            logger.debug("No external enrichment references to process");
            return;
        }
        logger.info("Processing " + config.getEnrichmentRefs().size() + " external enrichment references");
        processEnrichmentReferencesRecursive(config, new HashSet<>());
    }

    /**
     * Process external enrichment references recursively with cycle detection.
     *
     * <p>This method loads external enrichment files referenced in the enrichment-refs
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

    // ========================================================================
    // DATA-SOURCE REFERENCE PROCESSING
    // ========================================================================

    /**
     * Process external data-source references and merge them into the configuration.
     *
     * <p>This method resolves external data-source references defined in the 'data-source-refs'
     * section and merges them with any existing inline data-sources.
     *
     * @param config The configuration to process
     * @throws ConfigurationException if reference resolution fails
     */
    void processDataSourceReferences(YamlRuleConfiguration config) throws ConfigurationException {
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

    // ========================================================================
    // FILE LOADING (WITHOUT PROCESSING)
    // ========================================================================

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
     * <p>This method is used when loading referenced rule files to avoid infinite recursion.
     *
     * @param file The file to load
     * @return The loaded configuration (without processing)
     * @throws ConfigurationException if loading fails
     */
    YamlRuleConfiguration loadFromFileWithoutProcessing(File file) throws ConfigurationException {
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
     * <p>This method is used when loading referenced rule files to avoid infinite recursion.
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

    // ========================================================================
    // CONVERSION HELPERS
    // ========================================================================

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

    // ========================================================================
    // PROPERTY RESOLUTION
    // ========================================================================

    /**
     * Resolve environment variables and system properties in configuration values.
     * Delegates to the centralized PropertyResolver utility.
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
}
