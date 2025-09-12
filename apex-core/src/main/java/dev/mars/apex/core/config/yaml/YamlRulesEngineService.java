package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
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
 * High-level service for creating and managing rules engines from YAML configuration.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class YamlRulesEngineService {
    
    private static final Logger LOGGER = Logger.getLogger(YamlRulesEngineService.class.getName());
    
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
     */
    public RulesEngine createRulesEngineWithGenericArchitecture(String filePath) throws YamlConfigurationException {
        LOGGER.info("Creating rules engine with generic architecture from YAML file: " + filePath);

        YamlRuleConfiguration yamlConfig = configLoader.loadFromFile(filePath);
        return createRulesEngineFromYamlConfig(yamlConfig);
    }

    /**
     * Create a rules engine from a YAML configuration using the generic architecture.
     *
     * @param yamlConfig The YAML configuration
     * @return A configured RulesEngine with full enterprise metadata support
     * @throws YamlConfigurationException if configuration processing fails
     */
    public RulesEngine createRulesEngineFromYamlConfig(YamlRuleConfiguration yamlConfig) throws YamlConfigurationException {
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
     * @deprecated Use createRulesEngineWithGenericArchitecture for enhanced features
     */
    @Deprecated
    public RulesEngine createRulesEngineFromFile(String filePath) throws YamlConfigurationException {
        LOGGER.info("Creating rules engine from YAML file (legacy): " + filePath);

        YamlRuleConfiguration yamlConfig = configLoader.loadFromFile(filePath);
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);

        RulesEngine engine = new RulesEngine(config);

        LOGGER.info("Successfully created rules engine from file: " + filePath);
        return engine;
    }
    
    /**
     * Create a rules engine from a YAML configuration file.
     * 
     * @param file The YAML configuration file
     * @return A configured RulesEngine
     * @throws YamlConfigurationException if configuration loading or processing fails
     */
    public RulesEngine createRulesEngineFromFile(File file) throws YamlConfigurationException {
        LOGGER.info("Creating rules engine from YAML file: " + file.getAbsolutePath());
        
        YamlRuleConfiguration yamlConfig = configLoader.loadFromFile(file);
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);
        
        RulesEngine engine = new RulesEngine(config);
        
        LOGGER.info("Successfully created rules engine from file: " + file.getAbsolutePath());
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
        LOGGER.info("Creating rules engine from classpath resource: " + resourcePath);
        
        YamlRuleConfiguration yamlConfig = configLoader.loadFromClasspath(resourcePath);
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);
        
        RulesEngine engine = new RulesEngine(config);
        
        LOGGER.info("Successfully created rules engine from classpath resource: " + resourcePath);
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
        LOGGER.info("Creating rules engine from input stream");
        
        YamlRuleConfiguration yamlConfig = configLoader.loadFromStream(inputStream);
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);
        
        RulesEngine engine = new RulesEngine(config);
        
        LOGGER.info("Successfully created rules engine from input stream");
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
        LOGGER.info("Creating rules engine from YAML string");
        
        YamlRuleConfiguration yamlConfig = configLoader.fromYamlString(yamlString);
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);
        
        RulesEngine engine = new RulesEngine(config);
        
        LOGGER.info("Successfully created rules engine from YAML string");
        return engine;
    }
    
    /**
     * Load and merge multiple YAML configuration files into a single rules engine.
     * 
     * @param filePaths Array of file paths to load and merge
     * @return A configured RulesEngine with merged configuration
     * @throws YamlConfigurationException if any configuration loading or processing fails
     */
    public RulesEngine createRulesEngineFromMultipleFiles(String... filePaths) throws YamlConfigurationException {
        LOGGER.info("Creating rules engine from multiple YAML files: " + String.join(", ", filePaths));

        // First, load all YAML files without validation and merge them
        YamlRuleConfiguration mergedYamlConfig = new YamlRuleConfiguration();

        for (String filePath : filePaths) {
            LOGGER.fine("Loading file without validation: " + filePath);
            YamlRuleConfiguration yamlConfig = configLoader.loadFromFileWithoutValidation(filePath);

            // Merge YAML configurations
            mergeYamlConfigurations(mergedYamlConfig, yamlConfig);
        }

        // Now process rule references and data source references on the merged configuration
        configLoader.processReferencesAndValidate(mergedYamlConfig);

        // Create the final rules engine configuration
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(mergedYamlConfig);
        RulesEngine engine = new RulesEngine(config);

        LOGGER.info("Successfully created rules engine from " + filePaths.length + " YAML files");
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
        LOGGER.info("Updating rules engine from YAML file: " + filePath);
        
        YamlRuleConfiguration yamlConfig = configLoader.loadFromFile(filePath);
        RulesEngineConfiguration newConfig = ruleFactory.createRulesEngineConfiguration(yamlConfig);
        
        // Merge new configuration with existing
        mergeConfigurations(engine.getConfiguration(), newConfig);
        
        LOGGER.info("Successfully updated rules engine from file: " + filePath);
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
        LOGGER.info("Exporting rules engine to YAML file: " + filePath);
        
        // Convert RulesEngineConfiguration back to YAML format
        YamlRuleConfiguration yamlConfig = convertToYamlConfiguration(engine.getConfiguration());
        configLoader.saveToFile(yamlConfig, filePath);
        
        LOGGER.info("Successfully exported rules engine to file: " + filePath);
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
        
        LOGGER.fine("Merged configuration with " + source.getAllRules().size() +
                   " rules and " + source.getAllRuleGroups().size() + " rule groups");
    }

    /**
     * Merge two YAML rule configurations.
     *
     * This method merges all components from the source configuration into the target configuration.
     */
    private void mergeYamlConfigurations(YamlRuleConfiguration target, YamlRuleConfiguration source) {
        // Merge metadata (prefer target if both exist)
        if (target.getMetadata() == null && source.getMetadata() != null) {
            target.setMetadata(source.getMetadata());
        }

        // Merge data sources
        if (source.getDataSources() != null) {
            if (target.getDataSources() == null) {
                target.setDataSources(new ArrayList<>());
            }
            target.getDataSources().addAll(source.getDataSources());
        }

        // Merge data source references
        if (source.getDataSourceRefs() != null) {
            if (target.getDataSourceRefs() == null) {
                target.setDataSourceRefs(new ArrayList<>());
            }
            target.getDataSourceRefs().addAll(source.getDataSourceRefs());
        }

        // Merge rule references
        if (source.getRuleRefs() != null) {
            if (target.getRuleRefs() == null) {
                target.setRuleRefs(new ArrayList<>());
            }
            target.getRuleRefs().addAll(source.getRuleRefs());
        }

        // Merge data sinks
        if (source.getDataSinks() != null) {
            if (target.getDataSinks() == null) {
                target.setDataSinks(new ArrayList<>());
            }
            target.getDataSinks().addAll(source.getDataSinks());
        }

        // Merge categories
        if (source.getCategories() != null) {
            if (target.getCategories() == null) {
                target.setCategories(new ArrayList<>());
            }
            target.getCategories().addAll(source.getCategories());
        }

        // Merge rules
        if (source.getRules() != null) {
            if (target.getRules() == null) {
                target.setRules(new ArrayList<>());
            }
            target.getRules().addAll(source.getRules());
        }

        // Merge rule groups
        if (source.getRuleGroups() != null) {
            if (target.getRuleGroups() == null) {
                target.setRuleGroups(new ArrayList<>());
            }
            target.getRuleGroups().addAll(source.getRuleGroups());
        }

        // Merge enrichments
        if (source.getEnrichments() != null) {
            if (target.getEnrichments() == null) {
                target.setEnrichments(new ArrayList<>());
            }
            target.getEnrichments().addAll(source.getEnrichments());
        }

        // Merge rule chains
        if (source.getRuleChains() != null) {
            if (target.getRuleChains() == null) {
                target.setRuleChains(new ArrayList<>());
            }
            target.getRuleChains().addAll(source.getRuleChains());
        }

        LOGGER.fine("Merged YAML configuration with " +
                   (source.getRules() != null ? source.getRules().size() : 0) + " rules and " +
                   (source.getRuleGroups() != null ? source.getRuleGroups().size() : 0) + " rule groups");
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
        LOGGER.info("Basic configuration export completed. Rule details not included in this implementation.");

        return yamlConfig;
    }
}
