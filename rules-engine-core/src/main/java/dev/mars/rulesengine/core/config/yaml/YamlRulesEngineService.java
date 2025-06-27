package dev.mars.rulesengine.core.config.yaml;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * High-level service for creating and managing rules engines from YAML configuration.
 * This service combines YAML loading, rule factory, and rules engine initialization.
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
     * Create a rules engine from a YAML configuration file.
     * 
     * @param filePath The path to the YAML configuration file
     * @return A configured RulesEngine
     * @throws YamlConfigurationException if configuration loading or processing fails
     */
    public RulesEngine createRulesEngineFromFile(String filePath) throws YamlConfigurationException {
        LOGGER.info("Creating rules engine from YAML file: " + filePath);
        
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
        
        RulesEngineConfiguration mergedConfig = new RulesEngineConfiguration();
        
        for (String filePath : filePaths) {
            LOGGER.fine("Processing file: " + filePath);
            YamlRuleConfiguration yamlConfig = configLoader.loadFromFile(filePath);
            RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);
            
            // Merge configurations
            mergeConfigurations(mergedConfig, config);
        }
        
        RulesEngine engine = new RulesEngine(mergedConfig);
        
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
        yamlConfig.setMetadata(metadata);
        
        // TODO: Implement full conversion from RulesEngineConfiguration to YamlRuleConfiguration
        LOGGER.warning("Export functionality is currently limited - full conversion not yet implemented");
        
        return yamlConfig;
    }
}
