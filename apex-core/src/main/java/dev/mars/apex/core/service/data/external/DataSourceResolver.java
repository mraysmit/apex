package dev.mars.apex.core.service.data.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.mars.apex.core.util.PropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for resolving external data-source configuration references.
 * 
 * This service enables separation of infrastructure configuration (data-sources)
 * from business logic configuration (enrichments) by providing a reference
 * resolution mechanism.
 * 
 * Features:
 * - Load external data-source YAML configurations
 * - Cache loaded configurations for performance
 * - Resolve file-based and classpath-based references
 * - Support for environment-specific configurations
 * 
 * @author Mark A Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0.0
 */
public class DataSourceResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceResolver.class);

    private final ObjectMapper yamlMapper;
    private final Map<String, ExternalDataSourceConfig> configCache;
    
    public DataSourceResolver() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.configCache = new ConcurrentHashMap<>();
        logger.debug("DataSourceResolver initialized");
    }
    
    /**
     * Resolve an external data-source configuration by reference.
     *
     * @param reference The reference to the external configuration (e.g., "data-sources/customer-database.yaml")
     * @return The resolved data-source configuration
     * @throws DataSourceResolutionException if the reference cannot be resolved
     */
    public ExternalDataSourceConfig resolveDataSource(String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            throw new DataSourceResolutionException("Data-source reference cannot be null or empty");
        }

        // Check cache first
        ExternalDataSourceConfig cached = configCache.get(reference);
        if (cached != null) {
            logger.debug("Returning cached data-source configuration for reference: {}", reference);
            return cached;
        }

        logger.info("Resolving external data-source configuration: {}", reference);

        try {
            // Try file system first, then classpath
            ExternalDataSourceConfig config = loadFromFileSystemOrClasspath(reference);

            // Cache the loaded configuration
            configCache.put(reference, config);

            logger.info("Successfully resolved and cached data-source configuration: {} (name: {})",
                       reference, config.getMetadata().getName());

            return config;

        } catch (Exception e) {
            throw new DataSourceResolutionException(
                "Failed to resolve data-source reference: " + reference, e);
        }
    }
    
    /**
     * Load external data-source configuration from file system or classpath.
     * Tries file system first, then falls back to classpath.
     */
    private ExternalDataSourceConfig loadFromFileSystemOrClasspath(String reference) {
        logger.debug("Attempting to load data-source configuration from file system or classpath: {}", reference);

        // Try file system first
        try {
            ExternalDataSourceConfig config = loadFromFileSystem(reference);
            logger.debug("Successfully loaded data-source configuration from file system: {}", reference);
            return config;
        } catch (DataSourceResolutionException e) {
            // Check if this is a validation or property resolution error - if so, don't try classpath
            if (e.getMessage().contains("metadata is missing") ||
                e.getMessage().contains("name is missing") ||
                e.getMessage().contains("spec is missing") ||
                e.getMessage().contains("Property not found")) {
                logger.debug("Validation/property error from file system, not trying classpath: {}", e.getMessage());
                logger.debug("Full validation exception details:", e);
                throw e; // Re-throw validation/property errors immediately
            }
            logger.debug("Failed to load from file system, trying classpath: {}", e.getMessage());
            logger.debug("Full exception details from file system attempt:", e);
        } catch (Exception e) {
            logger.debug("Failed to load from file system (unexpected error), trying classpath: {}", e.getMessage());
            logger.debug("Full exception details:", e);
        }

        // Fall back to classpath
        try {
            ExternalDataSourceConfig config = loadFromClasspath(reference);
            logger.debug("Successfully loaded data-source configuration from classpath: {}", reference);
            return config;
        } catch (Exception e) {
            logger.debug("Failed to load from classpath: {}", e.getMessage());
            logger.debug("Full exception details from classpath attempt:", e);
        }

        // Both failed
        throw new DataSourceResolutionException(
            "Data-source configuration not found in file system or classpath: " + reference);
    }

    /**
     * Load external data-source configuration from file system.
     */
    private ExternalDataSourceConfig loadFromFileSystem(String reference) {
        logger.debug("Loading data-source configuration from file system: {}", reference);

        Path path = Paths.get(reference);
        if (!Files.exists(path)) {
            throw new DataSourceResolutionException(
                "Data-source configuration file not found: " + reference);
        }

        try {
            // Read file content as string for property resolution
            String rawContent = Files.readString(path, StandardCharsets.UTF_8);
            
            // Resolve environment variables and system properties
            String resolvedContent = resolveProperties(rawContent);
            
            ExternalDataSourceConfig config = yamlMapper.readValue(resolvedContent, ExternalDataSourceConfig.class);

            // Validate the loaded configuration
            validateConfiguration(config, reference);

            return config;

        } catch (DataSourceResolutionException e) {
            // Re-throw validation exceptions as-is
            throw e;
        } catch (Exception e) {
            throw new DataSourceResolutionException(
                "Failed to load data-source configuration from file system: " + reference, e);
        }
    }

    /**
     * Load external data-source configuration from classpath.
     */
    private ExternalDataSourceConfig loadFromClasspath(String reference) {
        logger.debug("Loading data-source configuration from classpath: {}", reference);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(reference)) {
            if (inputStream == null) {
                throw new DataSourceResolutionException(
                    "Data-source configuration not found on classpath: " + reference);
            }

            // Read input stream content as string for property resolution
            String rawContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            
            // Resolve environment variables and system properties
            String resolvedContent = resolveProperties(rawContent);
            
            ExternalDataSourceConfig config = yamlMapper.readValue(resolvedContent, ExternalDataSourceConfig.class);

            // Validate the loaded configuration
            validateConfiguration(config, reference);

            return config;

        } catch (DataSourceResolutionException e) {
            // Re-throw resolution exceptions as-is
            throw e;
        } catch (Exception e) {
            throw new DataSourceResolutionException(
                "Failed to load data-source configuration from classpath: " + reference, e);
        }
    }
    
    /**
     * Validate the loaded external data-source configuration.
     * Uses standard APEX format: metadata + data-sources array.
     */
    private void validateConfiguration(ExternalDataSourceConfig config, String reference) {
        if (config == null) {
            throw new DataSourceResolutionException("Loaded configuration is null for reference: " + reference);
        }
        
        if (config.getMetadata() == null) {
            throw new DataSourceResolutionException("Configuration metadata is missing for reference: " + reference);
        }
        
        if (config.getMetadata().getName() == null || config.getMetadata().getName().trim().isEmpty()) {
            throw new DataSourceResolutionException("Configuration name is missing for reference: " + reference);
        }
        
        if (config.getDataSources() == null || config.getDataSources().isEmpty()) {
            throw new DataSourceResolutionException("Configuration data-sources is missing for reference: " + reference);
        }
        
        logger.debug("Configuration validation passed for reference: {}", reference);
    }
    
    /**
     * Clear the configuration cache.
     * Useful for testing or when configurations change at runtime.
     */
    public void clearCache() {
        configCache.clear();
        logger.info("Data-source configuration cache cleared");
    }
    
    /**
     * Get the number of cached configurations.
     */
    public int getCacheSize() {
        return configCache.size();
    }
    
    /**
     * Resolve environment variables and system properties in configuration values.
     * Delegates to the centralized PropertyResolver utility.
     *
     * @param value The configuration value that may contain property placeholders
     * @return The value with resolved properties
     * @throws DataSourceResolutionException if a required property is not found
     */
    private String resolveProperties(String value) {
        try {
            return PropertyResolver.resolve(value, true);
        } catch (PropertyResolver.PropertyResolutionException e) {
            throw new DataSourceResolutionException(e.getMessage(), e);
        }
    }
}
