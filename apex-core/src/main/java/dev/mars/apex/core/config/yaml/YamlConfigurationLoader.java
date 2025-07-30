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
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
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
        
        // Validate rules
        if (config.getRules() != null) {
            for (YamlRule rule : config.getRules()) {
                validateRule(rule);
            }
        }
        
        // Validate rule groups
        if (config.getRuleGroups() != null) {
            for (YamlRuleGroup group : config.getRuleGroups()) {
                validateRuleGroup(group);
            }
        }
        
        // Validate categories
        if (config.getCategories() != null) {
            for (YamlCategory category : config.getCategories()) {
                validateCategory(category);
            }
        }
        
        LOGGER.fine("Configuration validation completed successfully");
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
     * Validate a category configuration.
     */
    private void validateCategory(YamlCategory category) throws YamlConfigurationException {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new YamlConfigurationException("Category name is required");
        }
    }
}
