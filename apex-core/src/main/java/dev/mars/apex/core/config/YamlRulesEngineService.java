package dev.mars.apex.core.config;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import java.io.File;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * High-level service for creating and managing rules engines from YAML configuration.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class YamlRulesEngineService {
    
    private static final Logger logger = LoggerFactory.getLogger(YamlRulesEngineService.class);
    
    private final YamlConfigurationLoader configLoader;
    private final YamlRuleFactory ruleFactory;
    
    /**
     * Constructor with default configuration loader and rule factory.
     */
    public YamlRulesEngineService() {
        this.configLoader = new YamlConfigurationLoader();
        this.ruleFactory = new YamlRuleFactory();
    }
    
    /**
     * Constructor with custom configuration loader and rule factory.
     * 
     * @param configLoader The YAML configuration loader
     * @param ruleFactory The rule factory
     */
    public YamlRulesEngineService(YamlConfigurationLoader configLoader, YamlRuleFactory ruleFactory) {
        this.configLoader = configLoader;
        this.ruleFactory = ruleFactory;
    }
    
    /**
     * Create a rules engine from a YAML configuration file using the new generic architecture.
     * This method leverages GenericRuleSet for enhanced validation, metadata support, and audit trails.
     *
     * @param filePath The path to the YAML configuration file
     * @return A configured RulesEngine with full enterprise metadata support
     * @throws YamlConfigurationException if configuration loading or processing fails
     * @deprecated since 2.0, for removal in 3.0. This method is redundant - use {@link #createRulesEngineFromYamlConfig(YamlRuleConfiguration)}
     *             with {@link YamlConfigurationLoader#loadFromFile(String)} instead. The "generic architecture" is now the standard approach.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public RulesEngine createRulesEngineWithGenericArchitecture(String filePath) throws YamlConfigurationException {
        logger.warn("DEPRECATED: createRulesEngineWithGenericArchitecture() is deprecated. Use createRulesEngineFromYamlConfig() with YamlConfigurationLoader.loadFromFile() instead.");
        logger.info("Creating rules engine with generic architecture from YAML file: " + filePath);

        YamlRuleConfiguration yamlConfig = configLoader.loadFromFile(filePath);
        return createRulesEngineFromYamlConfig(yamlConfig);
    }

    /**
     * Create a rules engine from a YAML configuration using the generic architecture.
     *
     * @param yamlConfig The YAML configuration
     * @return A configured RulesEngine with full enterprise metadata support
     * @throws YamlConfigurationException if configuration processing fails
     * @deprecated since 3.0, for removal in 4.0. This factory method is redundant - developers should use the universal pattern:
     *             {@code new RulesEngine(ruleFactory.createRulesEngineConfiguration(yamlConfig))} directly.
     *             This eliminates the need for content-aware service selection and provides a single, universal entry point.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public RulesEngine createRulesEngineFromYamlConfig(YamlRuleConfiguration yamlConfig) throws YamlConfigurationException {
        logger.warn("DEPRECATED: createRulesEngineFromYamlConfig() is deprecated. Use new RulesEngine(ruleFactory.createRulesEngineConfiguration(yamlConfig)) instead.");
        try {
            // Use the factory's method which has proper category metadata inheritance
            RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);

            return new RulesEngine(config);
        } catch (Exception e) {
            throw new YamlConfigurationException("Failed to create rules engine with generic architecture", e);
        }
    }

    /**
     * Create a rules engine from a YAML configuration file (legacy method).
     *
     * @param filePath The path to the YAML configuration file
     * @return A configured RulesEngine
     * @throws YamlConfigurationException if configuration loading or processing fails
     * @deprecated since 3.0, for removal in 4.0. This factory method is redundant - use the universal pattern:
     *             {@code YamlConfigurationLoader loader = new YamlConfigurationLoader();
     *             YamlRuleConfiguration yamlConfig = loader.loadFromFile(filePath);
     *             RulesEngine engine = new RulesEngine(ruleFactory.createRulesEngineConfiguration(yamlConfig));}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public RulesEngine createRulesEngineFromFile(String filePath) throws YamlConfigurationException {
        logger.warn("DEPRECATED: createRulesEngineFromFile(String) is deprecated. Use YamlConfigurationLoader + new RulesEngine() instead.");
        logger.info("Creating rules engine from YAML file (legacy): " + filePath);

        YamlRuleConfiguration yamlConfig = configLoader.loadFromFile(filePath);
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);

        RulesEngine engine = new RulesEngine(config);

        logger.info("Successfully created rules engine from file: " + filePath);
        return engine;
    }
    
    /**
     * Create a rules engine from a YAML configuration file.
     *
     * @param file The YAML configuration file
     * @return A configured RulesEngine
     * @throws YamlConfigurationException if configuration loading or processing fails
     * @deprecated since 2.0, for removal in 3.0. This method is a redundant wrapper - use {@link #createRulesEngineFromYamlConfig(YamlRuleConfiguration)}
     *             with {@link YamlConfigurationLoader#loadFromFile(File)} instead for better separation of concerns.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public RulesEngine createRulesEngineFromFile(File file) throws YamlConfigurationException {
        logger.warn("DEPRECATED: createRulesEngineFromFile(File) is deprecated. Use createRulesEngineFromYamlConfig() with YamlConfigurationLoader.loadFromFile() instead.");
        logger.info("Creating rules engine from YAML file: " + file.getAbsolutePath());

        YamlRuleConfiguration yamlConfig = configLoader.loadFromFile(file);
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);

        RulesEngine engine = new RulesEngine(config);

        logger.info("Successfully created rules engine from file: " + file.getAbsolutePath());
        return engine;
    }
    
    /**
     * Create a rules engine from a classpath resource.
     * 
     * @param resourcePath The classpath resource path
     * @return A configured RulesEngine
     * @throws YamlConfigurationException if configuration loading or processing fails
     */
    public RulesEngine createRulesEngineFromClasspath(String resourcePath) throws YamlConfigurationException {
        logger.info("Creating rules engine from classpath resource: " + resourcePath);

        YamlRuleConfiguration yamlConfig = configLoader.loadFromClasspath(resourcePath);
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);

        RulesEngine engine = new RulesEngine(config);

        logger.info("Successfully created rules engine from classpath resource: " + resourcePath);
        return engine;
    }
    
    /**
     * Create a rules engine from an input stream.
     * 
     * @param inputStream The input stream containing YAML configuration
     * @return A configured RulesEngine
     * @throws YamlConfigurationException if configuration loading or processing fails
     */
    public RulesEngine createRulesEngineFromStream(InputStream inputStream) throws YamlConfigurationException {
        logger.info("Creating rules engine from input stream");
        
        YamlRuleConfiguration yamlConfig = configLoader.loadFromStream(inputStream);
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);

        RulesEngine engine = new RulesEngine(config);

        logger.info("Successfully created rules engine from input stream");
        return engine;
    }

    /**
     * Create a rules engine from a YAML string.
     *
     * @param yamlString The YAML configuration as a string
     * @return A configured RulesEngine
     * @throws YamlConfigurationException if configuration parsing or processing fails
     */
    public RulesEngine createRulesEngineFromString(String yamlString) throws YamlConfigurationException {
        logger.info("Creating rules engine from YAML string");

        YamlRuleConfiguration yamlConfig = configLoader.fromYamlString(yamlString);
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);

        RulesEngine engine = new RulesEngine(config);

        logger.info("Successfully created rules engine from YAML string");
        return engine;
    }
    
    /**
     * Load and merge multiple YAML configuration files into a single rules engine.
     *
     * @param filePaths Array of file paths to load and merge
     * @return A configured RulesEngine with merged configuration
     * @throws YamlConfigurationException if any configuration loading or processing fails
     * @deprecated since 3.0, for removal in 4.0. This factory method is redundant - developers should load and merge
     *             YAML configurations manually, then use the universal pattern: {@code new RulesEngine(config)}.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public RulesEngine createRulesEngineFromMultipleFiles(String... filePaths) throws YamlConfigurationException {
        logger.warn("DEPRECATED: createRulesEngineFromMultipleFiles() is deprecated. Load and merge YAML configs manually, then use new RulesEngine().");
        logger.info("Creating rules engine from multiple YAML files: " + String.join(", ", filePaths));

        // First, load all YAML files without validation and merge them
        YamlRuleConfiguration mergedYamlConfig = new YamlRuleConfiguration();

        for (String filePath : filePaths) {
            logger.debug("Loading file without validation: " + filePath);
            YamlRuleConfiguration yamlConfig = configLoader.loadFromFileWithoutValidation(filePath);

            // Merge YAML configurations
            mergeYamlConfigurations(mergedYamlConfig, yamlConfig);
        }

        // Now process rule references and data source references on the merged configuration
        configLoader.processReferencesAndValidate(mergedYamlConfig);

        // Create the final rules engine configuration
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(mergedYamlConfig);

        RulesEngine engine = new RulesEngine(config);

        logger.info("Successfully created rules engine from " + filePaths.length + " YAML files");
        return engine;
    }
    
    /**
     * Update an existing rules engine with new YAML configuration.
     * 
     * @param engine The existing rules engine
     * @param filePath The path to the new YAML configuration file
     * @return The updated RulesEngine
     * @throws YamlConfigurationException if configuration loading or processing fails
     */
    public RulesEngine updateRulesEngineFromFile(RulesEngine engine, String filePath) throws YamlConfigurationException {
        logger.info("Updating rules engine from YAML file: " + filePath);
        
        YamlRuleConfiguration yamlConfig = configLoader.loadFromFile(filePath);
        RulesEngineConfiguration newConfig = ruleFactory.createRulesEngineConfiguration(yamlConfig);
        
        // Merge new configuration with existing
        mergeConfigurations(engine.getConfiguration(), newConfig);
        
        logger.info("Successfully updated rules engine from file: " + filePath);
        return engine;
    }
    
    /**
     * Export a rules engine configuration to YAML file.
     * 
     * @param engine The rules engine to export
     * @param filePath The target file path
     * @throws YamlConfigurationException if export fails
     */
    public void exportRulesEngineToFile(RulesEngine engine, String filePath) throws YamlConfigurationException {
        logger.info("Exporting rules engine to YAML file: " + filePath);
        
        // Convert RulesEngineConfiguration back to YAML format
        YamlRuleConfiguration yamlConfig = convertToYamlConfiguration(engine.getConfiguration());
        configLoader.saveToFile(yamlConfig, filePath);
        
        logger.info("Successfully exported rules engine to file: " + filePath);
    }
    
    /**
     * Get the YAML configuration loader.
     * 
     * @return The configuration loader
     */
    public YamlConfigurationLoader getConfigLoader() {
        return configLoader;
    }
    
    /**
     * Get the rule factory.
     * 
     * @return The rule factory
     */
    public YamlRuleFactory getRuleFactory() {
        return ruleFactory;
    }
    
    /**
     * Merge two rules engine configurations.
     */
    private void mergeConfigurations(RulesEngineConfiguration target, RulesEngineConfiguration source) {
        // Add all rules from source to target
        source.getAllRules().forEach(target::registerRule);
        
        // Add all rule groups from source to target
        source.getAllRuleGroups().forEach(target::registerRuleGroup);
        
        logger.debug("Merged configuration with " + source.getAllRules().size() +
                   " rules and " + source.getAllRuleGroups().size() + " rule groups");
    }

    /**
     * Merge two YAML rule configurations.
     *
     * This method merges all components from the source configuration into the target configuration.
     *
     * @deprecated Use {@link YamlConfigurationMerger#merge(YamlRuleConfiguration, YamlRuleConfiguration)} instead.
     *             This method is kept for backward compatibility and delegates to the utility class.
     */
    @Deprecated
    private void mergeYamlConfigurations(YamlRuleConfiguration target, YamlRuleConfiguration source) {
        YamlConfigurationMerger.merge(target, source);
    }

    /**
     * Convert RulesEngineConfiguration back to YAML format.
     * This is a simplified conversion for basic export functionality.
     */
    private YamlRuleConfiguration convertToYamlConfiguration(RulesEngineConfiguration config) {
        // This is a placeholder implementation
        // In a full implementation, you would convert all rules, groups, and categories
        // back to their YAML representation
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        
        // Add metadata
        YamlRuleConfiguration.ConfigurationMetadata metadata = new YamlRuleConfiguration.ConfigurationMetadata();
        metadata.setName("Exported Configuration");
        metadata.setVersion("1.0");
        metadata.setDescription("Configuration exported from rules engine");
        metadata.setAuthor("APEX Rules Engine");
        yamlConfig.setMetadata(metadata);

        // Note: Full rule conversion would require complex mapping from Rule objects back to YAML format
        // This basic implementation provides the configuration structure with metadata
        logger.info("Basic configuration export completed. Rule details not included in this implementation.");

        return yamlConfig;
    }
}
