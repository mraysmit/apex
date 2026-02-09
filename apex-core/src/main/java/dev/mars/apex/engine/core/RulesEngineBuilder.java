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
package dev.mars.apex.engine.core;

import dev.mars.apex.core.config.loader.ScenarioRegistryLoader;
import dev.mars.apex.core.config.exception.YamlConfigurationException;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Fluent builder for RulesEngine configuration.
 * 
 * <p>Provides a clean API for configuring search paths, classpath prefixes,
 * context variables, and building a RulesEngine from various sources.</p>
 * 
 * <p><b>Search Path Precedence:</b></p>
 * <ol>
 *   <li>Registry-level paths (from registry YAML {@code search-paths} section)</li>
 *   <li>Programmatic paths (via {@code addSearchPath()}, {@code addClasspathPrefix()})</li>
 *   <li>System property paths ({@code apex.config.searchPaths})</li>
 *   <li>Environment variable paths ({@code APEX_CONFIG_SEARCH_PATHS})</li>
 *   <li>Default resolution (relative to source file)</li>
 * </ol>
 *
 * @since 2026-01-22
 */
public class RulesEngineBuilder {
    private static final Logger logger = LoggerFactory.getLogger(RulesEngineBuilder.class);
    
    private final List<String> filesystemPaths = new ArrayList<>();
    private final List<String> classpathPrefixes = new ArrayList<>();
    private final Map<String, Object> contextVariables = new HashMap<>();
    private String sourcePath;
    private SourceType sourceType;

    /**
     * Source type for RulesEngine configuration.
     */
    private enum SourceType {
        FILE, SCENARIO_REGISTRY, YAML_CONFIG
    }

    /**
     * Create a new RulesEngineBuilder instance.
     * Typically accessed via {@link RulesEngine#builder()}.
     */
    public RulesEngineBuilder() {
        logger.debug("Created new RulesEngineBuilder");
    }

    /**
     * Add a filesystem search path for config file resolution.
     * 
     * <p>Paths are searched in the order they are added. Supports environment
     * variable expansion using {@code ${VAR_NAME}} syntax.</p>
     *
     * @param path The filesystem path (absolute or relative)
     * @return this builder for method chaining
     */
    public RulesEngineBuilder addSearchPath(String path) {
        if (path != null && !path.trim().isEmpty()) {
            String expanded = expandEnvironmentVariables(path.trim());
            filesystemPaths.add(expanded);
            logger.debug("Added filesystem search path: {}", expanded);
        }
        return this;
    }

    /**
     * Add multiple filesystem search paths.
     *
     * @param paths The filesystem paths to add
     * @return this builder for method chaining
     */
    public RulesEngineBuilder addSearchPaths(String... paths) {
        if (paths != null) {
            for (String path : paths) {
                addSearchPath(path);
            }
        }
        return this;
    }

    /**
     * Add multiple filesystem search paths from a collection.
     *
     * @param paths The filesystem paths to add
     * @return this builder for method chaining
     */
    public RulesEngineBuilder addSearchPaths(Collection<String> paths) {
        if (paths != null) {
            for (String path : paths) {
                addSearchPath(path);
            }
        }
        return this;
    }

    /**
     * Add a classpath prefix for config file resolution.
     * 
     * <p>Prefixes are searched in the order they are added.</p>
     *
     * @param prefix The classpath prefix (e.g., "apex/", "META-INF/apex/")
     * @return this builder for method chaining
     */
    public RulesEngineBuilder addClasspathPrefix(String prefix) {
        if (prefix != null && !prefix.trim().isEmpty()) {
            String normalized = prefix.trim();
            // Ensure prefix ends with /
            if (!normalized.endsWith("/")) {
                normalized = normalized + "/";
            }
            classpathPrefixes.add(normalized);
            logger.debug("Added classpath prefix: {}", normalized);
        }
        return this;
    }

    /**
     * Add multiple classpath prefixes.
     *
     * @param prefixes The classpath prefixes to add
     * @return this builder for method chaining
     */
    public RulesEngineBuilder addClasspathPrefixes(String... prefixes) {
        if (prefixes != null) {
            for (String prefix : prefixes) {
                addClasspathPrefix(prefix);
            }
        }
        return this;
    }

    /**
     * Add a context variable for environment variable substitution.
     * 
     * <p>Context variables can be used in YAML files with {@code ${name}} syntax.</p>
     *
     * @param name The variable name
     * @param value The variable value
     * @return this builder for method chaining
     */
    public RulesEngineBuilder withContext(String name, Object value) {
        if (name != null && !name.trim().isEmpty()) {
            contextVariables.put(name.trim(), value);
            logger.debug("Added context variable: {} = {}", name, value);
        }
        return this;
    }

    /**
     * Add multiple context variables from a map.
     *
     * @param context The context variables map
     * @return this builder for method chaining
     */
    public RulesEngineBuilder withContext(Map<String, Object> context) {
        if (context != null) {
            contextVariables.putAll(context);
            logger.debug("Added {} context variables", context.size());
        }
        return this;
    }

    /**
     * Configure the builder to load from a scenario registry.
     *
     * @param registryPath The path to the scenario registry YAML file
     * @return this builder for method chaining
     */
    public RulesEngineBuilder fromScenarioRegistry(String registryPath) {
        this.sourcePath = registryPath;
        this.sourceType = SourceType.SCENARIO_REGISTRY;
        logger.debug("Set source: scenario registry at {}", registryPath);
        return this;
    }

    /**
     * Configure the builder to load from a YAML configuration file.
     *
     * @param filePath The path to the YAML configuration file
     * @return this builder for method chaining
     */
    public RulesEngineBuilder fromFile(String filePath) {
        this.sourcePath = filePath;
        this.sourceType = SourceType.FILE;
        logger.debug("Set source: file at {}", filePath);
        return this;
    }

    /**
     * Get the configured filesystem search paths.
     *
     * @return An unmodifiable list of search paths
     */
    public List<String> getSearchPaths() {
        return Collections.unmodifiableList(filesystemPaths);
    }

    /**
     * Get the configured classpath prefixes.
     *
     * @return An unmodifiable list of classpath prefixes
     */
    public List<String> getClasspathPrefixes() {
        return Collections.unmodifiableList(classpathPrefixes);
    }

    /**
     * Get the configured context variables.
     *
     * @return An unmodifiable map of context variables
     */
    public Map<String, Object> getContextVariables() {
        return Collections.unmodifiableMap(contextVariables);
    }

    /**
     * Build the RulesEngine with the configured options.
     *
     * @return A configured RulesEngine instance
     * @throws YamlConfigurationException if configuration fails
     * @throws IllegalStateException if no source is configured
     */
    public RulesEngine build() throws YamlConfigurationException {
        if (sourceType == null || sourcePath == null) {
            throw new IllegalStateException(
                "No source configured. Call fromFile(), fromScenarioRegistry(), or fromYamlConfig() before build()."
            );
        }

        logger.info("Building RulesEngine from {} at {}", sourceType, sourcePath);

        switch (sourceType) {
            case SCENARIO_REGISTRY:
                return buildFromScenarioRegistry();
            case FILE:
                return buildFromFile();
            default:
                throw new IllegalStateException("Unknown source type: " + sourceType);
        }
    }

    /**
     * Build RulesEngine from scenario registry with configured search paths.
     */
    private RulesEngine buildFromScenarioRegistry() throws YamlConfigurationException {
        logger.debug("Creating ScenarioRegistryLoader with {} filesystem paths, {} classpath prefixes",
                          filesystemPaths.size(), classpathPrefixes.size());

        ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
        
        // Apply configured search paths to the loader
        loader.setSearchPaths(filesystemPaths);
        loader.setClasspathPrefixes(classpathPrefixes);

        Map<String, ScenarioConfiguration> scenarios;

        // Try classpath first
        try (java.io.InputStream is = RulesEngine.class.getClassLoader().getResourceAsStream(sourcePath)) {
            if (is != null) {
                String classpathBase = deriveClasspathBase(sourcePath);
                scenarios = loader.loadRegistry(is, classpathBase);
                logger.info("Loaded {} scenarios from classpath registry: {}", scenarios.size(), sourcePath);
            } else {
                // Fallback to filesystem
                scenarios = loader.loadRegistry(sourcePath);
                logger.info("Loaded {} scenarios from filesystem registry: {}", scenarios.size(), sourcePath);
            }
        } catch (java.io.IOException e) {
            throw new YamlConfigurationException("Failed to load scenario registry: " + sourcePath, e);
        }

        if (scenarios == null || scenarios.isEmpty()) {
            throw new YamlConfigurationException(
                "Scenario registry is empty or failed to load: " + sourcePath
            );
        }

        RulesEngineConfiguration config = new RulesEngineConfiguration();
        return new RulesEngine(config, null, scenarios);
    }

    /**
     * Derive classpath base directory from a registry path.
     */
    private String deriveClasspathBase(String registryPath) {
        int lastSlash = Math.max(registryPath.lastIndexOf('/'), registryPath.lastIndexOf('\\'));
        return lastSlash >= 0 ? registryPath.substring(0, lastSlash + 1) : "";
    }

    /**
     * Build RulesEngine from YAML file with configured search paths.
     */
    private RulesEngine buildFromFile() throws YamlConfigurationException {
        logger.debug("Loading YAML configuration from: {}", sourcePath);

        // Resolve the file using search paths
        String resolvedPath = resolveFilePath(sourcePath);
        if (resolvedPath == null) {
            throw new YamlConfigurationException("Config file not found: " + sourcePath);
        }

        return RulesEngine.fromFile(resolvedPath);
    }

    /**
     * Resolve a file path using configured search paths.
     */
    private String resolveFilePath(String filePath) {
        // Check if path is absolute
        if (isAbsolutePath(filePath)) {
            return java.nio.file.Files.exists(java.nio.file.Paths.get(filePath)) ? filePath : null;
        }

        // Try each search path
        for (String searchPath : filesystemPaths) {
            String candidate = combinePath(searchPath, filePath);
            if (java.nio.file.Files.exists(java.nio.file.Paths.get(candidate))) {
                logger.debug("Resolved {} to {}", filePath, candidate);
                return candidate;
            }
        }

        // Try classpath prefixes
        for (String prefix : classpathPrefixes) {
            String candidate = combineClasspath(prefix, filePath);
            if (getClass().getClassLoader().getResource(candidate) != null) {
                logger.debug("Resolved {} to classpath:{}", filePath, candidate);
                return "classpath:" + candidate;
            }
        }

        // Fallback: check if file exists as-is
        if (java.nio.file.Files.exists(java.nio.file.Paths.get(filePath))) {
            return filePath;
        }

        // Check classpath as-is
        if (getClass().getClassLoader().getResource(filePath) != null) {
            return "classpath:" + filePath;
        }

        return null;
    }

    private boolean isAbsolutePath(String path) {
        if (path == null || path.isEmpty()) return false;
        if (path.startsWith("/")) return true;
        if (path.length() >= 3 && path.charAt(1) == ':' && (path.charAt(2) == '\\' || path.charAt(2) == '/')) {
            return true;
        }
        return false;
    }

    private String combinePath(String basePath, String relativePath) {
        if (basePath == null || basePath.isEmpty()) return relativePath;
        String base = basePath.endsWith("/") || basePath.endsWith("\\") 
                     ? basePath : basePath + "/";
        return base + relativePath;
    }

    private String combineClasspath(String prefix, String resourcePath) {
        if (prefix == null || prefix.isEmpty()) return resourcePath;
        String base = prefix.endsWith("/") ? prefix : prefix + "/";
        String resource = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        return base + resource;
    }

    private String expandEnvironmentVariables(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        
        String result = value;
        int start;
        while ((start = result.indexOf("${")) != -1) {
            int end = result.indexOf("}", start);
            if (end == -1) break;
            
            String varName = result.substring(start + 2, end);
            String varValue = contextVariables.containsKey(varName) 
                             ? String.valueOf(contextVariables.get(varName))
                             : System.getenv(varName);
            if (varValue == null) {
                varValue = System.getProperty(varName, "");
            }
            result = result.substring(0, start) + varValue + result.substring(end + 1);
        }
        
        return result;
    }
}
