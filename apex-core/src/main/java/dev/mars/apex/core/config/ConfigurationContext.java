package dev.mars.apex.core.config;

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

import dev.mars.apex.core.config.component.ComponentConfiguration;
import dev.mars.apex.core.config.component.ComponentLoader;
import dev.mars.apex.core.config.YamlConfigurationLoader;
import dev.mars.apex.core.config.YamlDataSource;
import dev.mars.apex.core.config.YamlRuleConfiguration;
import dev.mars.apex.core.config.ScenarioRegistryLoader;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Central registry for name-based resolution of APEX configurations.
 *
 * ConfigurationContext provides a unified mechanism for:
 * - Registering configurations, data sources, scenarios, and components by name
 * - Looking up registered items by name (instead of file path)
 * - Bulk loading from filesystem search paths or classpath prefixes
 * - Thread-safe access for concurrent environments
 *
 * This enables APEX configurations to reference each other by logical name
 * rather than file path, supporting:
 * - Portable configurations that work across filesystem and JAR deployments
 * - Clean separation of business logic from infrastructure paths
 * - Runtime discovery and registration of configurations
 *
 * USAGE:
 * <pre>{@code
 * ConfigurationContext context = ConfigurationContext.builder()
 *     .withResourceResolver(resolver)
 *     .addSearchPath("/etc/apex/configs")
 *     .addClasspathPrefix("apex/")
 *     .build();
 *
 * // Manual registration
 * context.registerConfiguration("trade-validation", config);
 *
 * // Name-based lookup
 * YamlRuleConfiguration config = context.getConfiguration("trade-validation");
 *
 * // Bulk loading
 * context.loadAllFromSearchPaths();
 * context.loadAllFromClasspath("apex/configs/");
 * }</pre>
 *
 * THREAD SAFETY:
 * All internal maps use ConcurrentHashMap for thread-safe access.
 * Registration and lookup operations are safe for concurrent use.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-08
 */
public class ConfigurationContext {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationContext.class);

    // Thread-safe maps for name-based lookups
    private final Map<String, YamlRuleConfiguration> configurationsByName;
    private final Map<String, YamlDataSource> dataSourcesByName;
    private final Map<String, ScenarioConfiguration> scenariosByName;
    private final Map<String, ComponentConfiguration> componentsByName;

    // Resource resolution
    private final ResourceResolver resourceResolver;

    // Configuration loaders
    private final YamlConfigurationLoader yamlLoader;
    private final ScenarioRegistryLoader scenarioLoader;
    private final ComponentLoader componentLoader;

    /**
     * Creates a new ConfigurationContext with default ResourceResolver.
     */
    public ConfigurationContext() {
        this(ResourceResolver.builder().build());
    }

    /**
     * Creates a new ConfigurationContext with a custom ResourceResolver.
     *
     * @param resourceResolver the resolver for classpath/filesystem resources
     */
    public ConfigurationContext(ResourceResolver resourceResolver) {
        this.resourceResolver = Objects.requireNonNull(resourceResolver, "ResourceResolver cannot be null");
        this.configurationsByName = new ConcurrentHashMap<>();
        this.dataSourcesByName = new ConcurrentHashMap<>();
        this.scenariosByName = new ConcurrentHashMap<>();
        this.componentsByName = new ConcurrentHashMap<>();
        
        this.yamlLoader = new YamlConfigurationLoader();
        this.scenarioLoader = new ScenarioRegistryLoader();
        this.componentLoader = new ComponentLoader(resourceResolver);
    }

    // ==================== Registration Methods ====================

    /**
     * Registers a rule configuration by name.
     * If a configuration with the same name already exists, it will be overwritten.
     *
     * @param name the logical name for lookup
     * @param configuration the configuration to register
     * @throws IllegalArgumentException if name or configuration is null
     */
    public void registerConfiguration(String name, YamlRuleConfiguration configuration) {
        Objects.requireNonNull(name, "Configuration name cannot be null");
        Objects.requireNonNull(configuration, "Configuration cannot be null");
        
        YamlRuleConfiguration previous = configurationsByName.put(name, configuration);
        if (previous != null) {
            logger.debug("Overwriting existing configuration with name: {}", name);
        } else {
            logger.debug("Registered configuration: {}", name);
        }
    }

    /**
     * Registers a data source by name.
     * If a data source with the same name already exists, it will be overwritten.
     *
     * @param name the logical name for lookup
     * @param dataSource the data source to register
     * @throws IllegalArgumentException if name or dataSource is null
     */
    public void registerDataSource(String name, YamlDataSource dataSource) {
        Objects.requireNonNull(name, "DataSource name cannot be null");
        Objects.requireNonNull(dataSource, "DataSource cannot be null");
        
        YamlDataSource previous = dataSourcesByName.put(name, dataSource);
        if (previous != null) {
            logger.debug("Overwriting existing data source with name: {}", name);
        } else {
            logger.debug("Registered data source: {}", name);
        }
    }

    /**
     * Registers a scenario by name.
     * If a scenario with the same name already exists, it will be overwritten.
     *
     * @param name the logical name for lookup
     * @param scenario the scenario to register
     * @throws IllegalArgumentException if name or scenario is null
     */
    public void registerScenario(String name, ScenarioConfiguration scenario) {
        Objects.requireNonNull(name, "Scenario name cannot be null");
        Objects.requireNonNull(scenario, "Scenario cannot be null");
        
        ScenarioConfiguration previous = scenariosByName.put(name, scenario);
        if (previous != null) {
            logger.debug("Overwriting existing scenario with name: {}", name);
        } else {
            logger.debug("Registered scenario: {}", name);
        }
    }

    /**
     * Registers a component by name.
     * If a component with the same name already exists, it will be overwritten.
     *
     * @param name the logical name for lookup
     * @param component the component to register
     * @throws IllegalArgumentException if name or component is null
     */
    public void registerComponent(String name, ComponentConfiguration component) {
        Objects.requireNonNull(name, "Component name cannot be null");
        Objects.requireNonNull(component, "Component cannot be null");
        
        ComponentConfiguration previous = componentsByName.put(name, component);
        if (previous != null) {
            logger.debug("Overwriting existing component with name: {}", name);
        } else {
            logger.debug("Registered component: {}", name);
        }
    }

    // ==================== Lookup Methods ====================

    /**
     * Retrieves a rule configuration by name.
     *
     * @param name the logical name to look up
     * @return the configuration, or null if not found
     */
    public YamlRuleConfiguration getConfiguration(String name) {
        if (name == null) {
            return null;
        }
        return configurationsByName.get(name);
    }

    /**
     * Retrieves a data source by name.
     *
     * @param name the logical name to look up
     * @return the data source, or null if not found
     */
    public YamlDataSource getDataSource(String name) {
        if (name == null) {
            return null;
        }
        return dataSourcesByName.get(name);
    }

    /**
     * Retrieves a scenario by name.
     *
     * @param name the logical name to look up
     * @return the scenario, or null if not found
     */
    public ScenarioConfiguration getScenario(String name) {
        if (name == null) {
            return null;
        }
        return scenariosByName.get(name);
    }

    /**
     * Retrieves a component by name.
     *
     * @param name the logical name to look up
     * @return the component, or null if not found
     */
    public ComponentConfiguration getComponent(String name) {
        if (name == null) {
            return null;
        }
        return componentsByName.get(name);
    }

    // ==================== Contains/Exists Methods ====================

    /**
     * Checks if a configuration with the given name is registered.
     *
     * @param name the name to check
     * @return true if registered, false otherwise
     */
    public boolean containsConfiguration(String name) {
        return name != null && configurationsByName.containsKey(name);
    }

    /**
     * Checks if a data source with the given name is registered.
     *
     * @param name the name to check
     * @return true if registered, false otherwise
     */
    public boolean containsDataSource(String name) {
        return name != null && dataSourcesByName.containsKey(name);
    }

    /**
     * Checks if a scenario with the given name is registered.
     *
     * @param name the name to check
     * @return true if registered, false otherwise
     */
    public boolean containsScenario(String name) {
        return name != null && scenariosByName.containsKey(name);
    }

    /**
     * Checks if a component with the given name is registered.
     *
     * @param name the name to check
     * @return true if registered, false otherwise
     */
    public boolean containsComponent(String name) {
        return name != null && componentsByName.containsKey(name);
    }

    // ==================== Collection Access ====================

    /**
     * Returns an unmodifiable view of all registered configuration names.
     *
     * @return set of configuration names
     */
    public Set<String> getConfigurationNames() {
        return Collections.unmodifiableSet(configurationsByName.keySet());
    }

    /**
     * Returns an unmodifiable view of all registered data source names.
     *
     * @return set of data source names
     */
    public Set<String> getDataSourceNames() {
        return Collections.unmodifiableSet(dataSourcesByName.keySet());
    }

    /**
     * Returns an unmodifiable view of all registered scenario names.
     *
     * @return set of scenario names
     */
    public Set<String> getScenarioNames() {
        return Collections.unmodifiableSet(scenariosByName.keySet());
    }

    /**
     * Returns an unmodifiable view of all registered component names.
     *
     * @return set of component names
     */
    public Set<String> getComponentNames() {
        return Collections.unmodifiableSet(componentsByName.keySet());
    }

    /**
     * Returns the total number of registered items across all types.
     *
     * @return total count of registered items
     */
    public int size() {
        return configurationsByName.size() + dataSourcesByName.size() 
             + scenariosByName.size() + componentsByName.size();
    }

    /**
     * Checks if the context has no registered items.
     *
     * @return true if no items are registered
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    // ==================== Bulk Loading Methods ====================

    /**
     * Loads all YAML configurations from the configured filesystem search paths.
     * Files are registered using their name from metadata, or filename without extension.
     *
     * @return count of configurations loaded
     */
    public int loadAllFromSearchPaths() {
        int count = 0;
        for (String searchPath : resourceResolver.getSearchPaths()) {
            count += loadFromDirectory(Paths.get(searchPath));
        }
        logger.info("Loaded {} configurations from search paths", count);
        return count;
    }

    /**
     * Loads all YAML configurations from a specific classpath prefix.
     * Files are registered using their name from metadata, or filename without extension.
     *
     * @param classpathPrefix the classpath prefix to scan (e.g., "apex/configs/")
     * @return count of configurations loaded
     */
    public int loadAllFromClasspath(String classpathPrefix) {
        Objects.requireNonNull(classpathPrefix, "Classpath prefix cannot be null");
        
        int count = 0;
        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources(classpathPrefix);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                count += loadFromClasspathUrl(url, classpathPrefix);
            }
        } catch (IOException e) {
            logger.error("Failed to scan classpath prefix: {} - {}", classpathPrefix, e.getMessage());
            logger.debug("Full stack trace for classpath scan failure:", e);
        }
        
        logger.debug("Loaded {} configurations from classpath prefix: {}", count, classpathPrefix);
        return count;
    }

    /**
     * Loads a single configuration from a file path or classpath resource.
     * The configuration is registered using the name from its metadata,
     * or the filename without extension if no name is specified.
     *
     * @param path the path to load from
     * @return the loaded configuration, or null if loading failed
     */
    public YamlRuleConfiguration loadConfiguration(String path) {
        try (InputStream is = resourceResolver.resolve(path)) {
            Map<String, Object> yamlMap = yamlLoader.loadAsMap(is);
            YamlRuleConfiguration config = yamlLoader.loadFromFile(path);
            
            String name = extractName(yamlMap, path);
            registerConfiguration(name, config);
            return config;
        } catch (Exception e) {
            logger.error("Failed to load configuration from: {} - {}", path, e.getMessage());
            logger.debug("Full stack trace for configuration load failure:", e);
            return null;
        }
    }

    /**
     * Loads a single component from a file path or classpath resource.
     * The component is registered using the name from its metadata,
     * or the filename without extension if no name is specified.
     *
     * @param path the path to load from
     * @return the loaded component, or null if loading failed
     */
    public ComponentConfiguration loadComponent(String path) {
        try {
            ComponentConfiguration component = componentLoader.loadComponent(path);
            
            String name = component.getMetadata() != null && component.getMetadata().getName() != null
                    ? component.getMetadata().getName()
                    : extractFilenameWithoutExtension(path);
            registerComponent(name, component);
            return component;
        } catch (Exception e) {
            logger.error("Failed to load component from: {} - {}", path, e.getMessage());
            logger.debug("Full stack trace for component load failure:", e);
            return null;
        }
    }

    // ==================== Clear/Remove Methods ====================

    /**
     * Removes all registered items from the context.
     */
    public void clear() {
        configurationsByName.clear();
        dataSourcesByName.clear();
        scenariosByName.clear();
        componentsByName.clear();
        logger.debug("Cleared all registered items from context");
    }

    /**
     * Removes a configuration by name.
     *
     * @param name the name to remove
     * @return the removed configuration, or null if not found
     */
    public YamlRuleConfiguration removeConfiguration(String name) {
        return configurationsByName.remove(name);
    }

    /**
     * Removes a data source by name.
     *
     * @param name the name to remove
     * @return the removed data source, or null if not found
     */
    public YamlDataSource removeDataSource(String name) {
        return dataSourcesByName.remove(name);
    }

    /**
     * Removes a scenario by name.
     *
     * @param name the name to remove
     * @return the removed scenario, or null if not found
     */
    public ScenarioConfiguration removeScenario(String name) {
        return scenariosByName.remove(name);
    }

    /**
     * Removes a component by name.
     *
     * @param name the name to remove
     * @return the removed component, or null if not found
     */
    public ComponentConfiguration removeComponent(String name) {
        return componentsByName.remove(name);
    }

    // ==================== Accessors ====================

    /**
     * Returns the ResourceResolver used by this context.
     *
     * @return the resource resolver
     */
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    // ==================== Private Helper Methods ====================

    private int loadFromDirectory(Path directory) {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            logger.debug("Skipping non-existent or non-directory path: {}", directory);
            return 0;
        }

        int count = 0;
        try (Stream<Path> files = Files.walk(directory)) {
            List<Path> yamlFiles = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                    .toList();
            
            for (Path file : yamlFiles) {
                if (loadSingleFile(file)) {
                    count++;
                }
            }
        } catch (IOException e) {
            logger.error("Failed to scan directory: {} - {}", directory, e.getMessage());
            logger.debug("Full stack trace for directory scan failure:", e);
        }
        return count;
    }

    private boolean loadSingleFile(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            Map<String, Object> yamlMap = yamlLoader.loadAsMap(is);
            String type = detectConfigurationType(yamlMap);
            String name = extractName(yamlMap, file.toString());

            switch (type) {
                case "component":
                    ComponentConfiguration component = componentLoader.loadComponent(file.toString());
                    registerComponent(name, component);
                    break;
                case "data-source":
                case "external-data-config":
                    // Load data source - for now just log, full implementation would parse YamlDataSource
                    logger.debug("Detected data source file: {} - skipping for now", file);
                    break;
                case "scenario":
                    // Load scenario - for now just log
                    logger.debug("Detected scenario file: {} - skipping for now", file);
                    break;
                default:
                    // Assume rule configuration
                    YamlRuleConfiguration config = yamlLoader.loadFromFile(file.toString());
                    registerConfiguration(name, config);
                    break;
            }
            return true;
        } catch (Exception e) {
            logger.error("Failed to load file: {} - {}", file, e.getMessage());
            logger.debug("Full stack trace for file load failure:", e);
            return false;
        }
    }

    private int loadFromClasspathUrl(URL url, String prefix) {
        // Classpath scanning is complex (works differently for file: vs jar: URLs)
        // For now, provide basic support for file: URLs (development/test environments)
        if ("file".equals(url.getProtocol())) {
            try {
                Path directory = Paths.get(url.toURI());
                return loadFromDirectory(directory);
            } catch (Exception e) {
                logger.error("Failed to load from classpath URL: {} - {}", url, e.getMessage());
                logger.debug("Full stack trace for classpath URL load failure:", e);
            }
        } else {
            logger.debug("JAR classpath scanning not yet implemented for URL: {}", url);
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private String detectConfigurationType(Map<String, Object> yamlMap) {
        // Check metadata.type first
        if (yamlMap.containsKey("metadata")) {
            Object metadata = yamlMap.get("metadata");
            if (metadata instanceof Map) {
                Map<String, Object> metadataMap = (Map<String, Object>) metadata;
                if (metadataMap.containsKey("type")) {
                    return String.valueOf(metadataMap.get("type")).toLowerCase();
                }
            }
        }
        
        // Infer from content
        if (yamlMap.containsKey("component-refs") || yamlMap.containsKey("config-files")) {
            return "component";
        }
        if (yamlMap.containsKey("processing-stages") || yamlMap.containsKey("scenario-id")) {
            return "scenario";
        }
        if (yamlMap.containsKey("connection") && yamlMap.containsKey("queries")) {
            return "data-source";
        }
        
        return "rule-config";
    }

    @SuppressWarnings("unchecked")
    private String extractName(Map<String, Object> yamlMap, String filePath) {
        // Try metadata.name first
        if (yamlMap.containsKey("metadata")) {
            Object metadata = yamlMap.get("metadata");
            if (metadata instanceof Map) {
                Map<String, Object> metadataMap = (Map<String, Object>) metadata;
                if (metadataMap.containsKey("name")) {
                    return String.valueOf(metadataMap.get("name"));
                }
                if (metadataMap.containsKey("id")) {
                    return String.valueOf(metadataMap.get("id"));
                }
            }
        }
        
        // Fall back to filename without extension
        return extractFilenameWithoutExtension(filePath);
    }

    private String extractFilenameWithoutExtension(String path) {
        String filename = Paths.get(path).getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    // ==================== Builder ====================

    /**
     * Creates a new Builder for ConfigurationContext.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ConfigurationContext with fluent API.
     */
    public static class Builder {
        private ResourceResolver resourceResolver;
        private final List<String> searchPaths = new ArrayList<>();
        private final List<String> classpathPrefixes = new ArrayList<>();

        /**
         * Sets a custom ResourceResolver.
         *
         * @param resolver the resource resolver
         * @return this builder
         */
        public Builder withResourceResolver(ResourceResolver resolver) {
            this.resourceResolver = resolver;
            return this;
        }

        /**
         * Adds a filesystem search path.
         *
         * @param path the search path
         * @return this builder
         */
        public Builder addSearchPath(String path) {
            if (path != null && !path.isEmpty()) {
                searchPaths.add(path);
            }
            return this;
        }

        /**
         * Adds multiple filesystem search paths.
         *
         * @param paths the search paths
         * @return this builder
         */
        public Builder addSearchPaths(List<String> paths) {
            if (paths != null) {
                paths.stream().filter(p -> p != null && !p.isEmpty()).forEach(searchPaths::add);
            }
            return this;
        }

        /**
         * Adds a classpath prefix for resource scanning.
         *
         * @param prefix the classpath prefix
         * @return this builder
         */
        public Builder addClasspathPrefix(String prefix) {
            if (prefix != null && !prefix.isEmpty()) {
                classpathPrefixes.add(prefix);
            }
            return this;
        }

        /**
         * Adds multiple classpath prefixes.
         *
         * @param prefixes the classpath prefixes
         * @return this builder
         */
        public Builder addClasspathPrefixes(List<String> prefixes) {
            if (prefixes != null) {
                prefixes.stream().filter(p -> p != null && !p.isEmpty()).forEach(classpathPrefixes::add);
            }
            return this;
        }

        /**
         * Builds the ConfigurationContext.
         *
         * @return a new ConfigurationContext
         */
        public ConfigurationContext build() {
            ResourceResolver resolver = resourceResolver;
            if (resolver == null) {
                ResourceResolver.Builder resolverBuilder = ResourceResolver.builder();
                searchPaths.forEach(resolverBuilder::addSearchPath);
                classpathPrefixes.forEach(resolverBuilder::addClasspathPrefix);
                resolver = resolverBuilder.build();
            } else {
                // Apply search paths to existing resolver
                searchPaths.forEach(resolver::addSearchPath);
                classpathPrefixes.forEach(resolver::addClasspathPrefix);
            }
            
            return new ConfigurationContext(resolver);
        }
    }
}
