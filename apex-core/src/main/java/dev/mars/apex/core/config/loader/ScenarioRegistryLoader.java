package dev.mars.apex.core.config.loader;

import dev.mars.apex.core.config.exception.*;
import dev.mars.apex.engine.scenario.ScenarioParser;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import dev.mars.apex.core.service.scenario.ScenarioStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Loader for scenario registry YAML files.
 * 
 * <p>This class loads a scenario registry file that contains references to multiple
 * scenario configuration files. It parses the registry, loads all referenced scenario
 * files, and returns a map of scenario configurations indexed by scenario ID.</p>
 * 
 * <p><b>Registry YAML Structure:</b></p>
 * <pre>
 * metadata:
 *   id: "scenario-registry"
 *   type: "scenario-registry"
 * 
 * scenarios:
 *   - scenario-id: "basic-trade-processing"
 *     config-file: "scenarios/basic-trade-processing.yaml"
 *     business-domain: "Trading"
 *   
 *   - scenario-id: "complex-trade-processing"
 *     config-file: "scenarios/complex-trade-processing.yaml"
 *     business-domain: "Trading"
 * 
 * routing:
 *   strategy: "classification-based"
 *   default-scenario: "basic-trade-processing"
 * </pre>
 * 
 * <p><b>Search Paths Configuration (Optional):</b></p>
 * <p>Each registry can define its own search paths for resolving scenario files:</p>
 * <pre>
 * search-paths:
 *   filesystem:
 *     - "/etc/apex/trading"
 *     - "./configs/trading"
 *   classpath:
 *     - "trading/"
 *     - "META-INF/apex/trading/"
 * </pre>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-03
 * @see ScenarioConfiguration
 */
public class ScenarioRegistryLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(ScenarioRegistryLoader.class);
    
    private final ConfigurationLoader configLoader;
    private final ScenarioParser scenarioParser;
    
    // Global search paths (can be set programmatically or via environment)
    private final List<String> globalFilesystemPaths;
    private final List<String> globalClasspathPrefixes;
    
    /**
     * Create a new ScenarioRegistryLoader with default configuration loader.
     */
    public ScenarioRegistryLoader() {
        this.configLoader = new ConfigurationLoader();
        this.scenarioParser = new ScenarioParser();
        this.globalFilesystemPaths = new ArrayList<>();
        this.globalClasspathPrefixes = new ArrayList<>();
        initializeFromEnvironment();
    }
    
    /**
     * Create a new ScenarioRegistryLoader with a custom configuration loader.
     * 
     * @param configLoader The YAML configuration loader to use
     */
    public ScenarioRegistryLoader(ConfigurationLoader configLoader) {
        this.configLoader = configLoader;
        this.scenarioParser = new ScenarioParser();
        this.globalFilesystemPaths = new ArrayList<>();
        this.globalClasspathPrefixes = new ArrayList<>();
        initializeFromEnvironment();
    }
    
    /**
     * Load a scenario registry and all referenced scenario configuration files.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Loads the registry YAML file</li>
     *   <li>Parses the 'scenarios' section</li>
     *   <li>Loads each referenced scenario configuration file</li>
     *   <li>Returns a map of ScenarioConfiguration objects indexed by scenario-id</li>
     * </ol>
     * 
     * <p><b>Example Usage:</b></p>
     * <pre>
     * ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
     * Map&lt;String, ScenarioConfiguration&gt; scenarios = loader.loadRegistry("registry.yaml");
     * 
     * ScenarioConfiguration scenario = scenarios.get("basic-trade-processing");
     * </pre>
     * 
     * @param registryPath The path to the scenario registry YAML file
     * @return A map of scenario configurations indexed by scenario-id
     * @throws ConfigurationException if the registry file cannot be loaded,
     *         if any referenced scenario file cannot be loaded,
     *         or if the registry structure is invalid
     */
    public Map<String, ScenarioConfiguration> loadRegistry(String registryPath) throws ConfigurationException {
        logger.info("Loading scenario registry from: {}", registryPath);
        
        try {
            // Validate registry file exists
            Path registryFilePath = Paths.get(registryPath);
            if (!Files.exists(registryFilePath)) {
                throw new ConfigurationException("Scenario registry file not found: " + registryPath);
            }
            
            // Load the registry YAML configuration as a Map
            Map<String, Object> registryConfig = configLoader.loadAsMap(registryPath);
            
            // Validate registry metadata
            validateRegistryMetadata(registryConfig, registryPath);
            
            // Parse search-paths from registry (Phase 6 feature)
            SearchPathConfig registrySearchPaths = parseSearchPaths(registryConfig);
            
            // Parse scenarios section
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scenarioRegistry = (List<Map<String, Object>>) registryConfig.get("scenarios");
            
            if (scenarioRegistry == null || scenarioRegistry.isEmpty()) {
                throw new ConfigurationException(
                    "Scenario registry file does not contain a 'scenarios' section or it is empty: " + registryPath
                );
            }
            
            // Load all referenced scenario files
            Map<String, ScenarioConfiguration> scenarios = new LinkedHashMap<>();
            Path registryDir = registryFilePath.getParent();
            String registryDirStr = registryDir != null ? registryDir.toString() : "";
            
            for (Map<String, Object> registryEntry : scenarioRegistry) {
                String scenarioId = (String) registryEntry.get("scenario-id");
                String configFile = (String) registryEntry.get("config-file");

                if (scenarioId == null || scenarioId.trim().isEmpty()) {
                    logger.warn("Skipping registry entry with missing or empty scenario-id");
                    continue;
                }

                if (configFile == null || configFile.trim().isEmpty()) {
                    throw new ConfigurationException(
                        "Scenario '" + scenarioId + "' in registry has missing or empty config-file"
                    );
                }

                // Parse enabled flag from registry entry (default: true)
                boolean enabled = parseEnabledFlag(registryEntry);

                logger.debug("Loading scenario '{}' from file: {} (enabled: {})", scenarioId, configFile, enabled);

                // Try to resolve using search paths first (Phase 6 feature)
                ResolvedPath resolved = resolveConfigFileWithSearchPaths(configFile, registrySearchPaths, registryDirStr, false);
                
                String resolvedConfigPath;
                ScenarioConfiguration scenario;
                
                if (resolved != null) {
                    resolvedConfigPath = resolved.path();
                    if (resolved.isClasspath()) {
                        // Load from classpath
                        scenario = loadScenarioFromClasspath(resolvedConfigPath);
                    } else {
                        // Load from filesystem
                        scenario = loadScenarioFromFile(resolvedConfigPath);
                    }
                } else {
                    // Fallback to legacy resolution
                    resolvedConfigPath = resolveConfigFilePath(configFile, registryDir);
                    scenario = loadScenarioFromFile(resolvedConfigPath);
                }

                // Validate that scenario-id matches
                if (scenario.getScenarioId() != null && !scenario.getScenarioId().equals(scenarioId)) {
                    logger.warn(
                        "Scenario ID mismatch: registry specifies '{}' but config file contains '{}'. Using registry ID.",
                        scenarioId, scenario.getScenarioId()
                    );
                }

                // Ensure scenario has the correct ID from registry
                scenario.setScenarioId(scenarioId);

                // Set enabled flag from registry entry
                scenario.setEnabled(enabled);

                // Store in map
                scenarios.put(scenarioId, scenario);
                logger.debug("Successfully loaded scenario: {} (enabled: {})", scenarioId, enabled);
            }
            
            logger.info("Successfully loaded {} scenarios from registry: {}", scenarios.size(), registryPath);
            return scenarios;
            
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Failed to load scenario registry from: " + registryPath, e);
        }
    }

    /**
     * Load a scenario registry from an InputStream.
     * 
     * <p>This method enables loading scenario registries from classpath resources
     * or other stream-based sources, such as JAR-packaged resources. When loading
     * from a stream, scenario file references (config-file) are resolved as classpath
     * resources.</p>
     * 
     * <p><b>Note:</b> When using this method, all scenario config-file references
     * in the registry must be resolvable as classpath resources, since there is
     * no filesystem context for relative path resolution.</p>
     * 
     * @param inputStream The input stream containing the registry YAML content
     * @return A map of scenario configurations indexed by scenario-id
     * @throws ConfigurationException if the registry cannot be loaded or parsed
     */
    public Map<String, ScenarioConfiguration> loadRegistry(InputStream inputStream) throws ConfigurationException {
        return loadRegistry(inputStream, null);
    }

    /**
     * Load a scenario registry from an InputStream with a classpath base for resolving
     * relative scenario file references.
     * 
     * <p>This method enables loading scenario registries from classpath resources
     * while supporting relative path resolution. The classpathBase parameter specifies
     * the base path for resolving relative config-file references.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * // If registry is at "scenarios/registry.yaml" and contains:
     * //   config-file: "trade-processing.yaml"
     * // Use classpathBase = "scenarios/" to resolve to "scenarios/trade-processing.yaml"
     * 
     * InputStream is = getClass().getClassLoader().getResourceAsStream("scenarios/registry.yaml");
     * Map&lt;String, ScenarioConfiguration&gt; scenarios = loader.loadRegistry(is, "scenarios/");
     * </pre>
     * 
     * @param inputStream The input stream containing the registry YAML content
     * @param classpathBase The base path for resolving relative config-file references
     *                      (e.g., "scenarios/"). Can be null if all paths are absolute.
     * @return A map of scenario configurations indexed by scenario-id
     * @throws ConfigurationException if the registry cannot be loaded or parsed
     */
    public Map<String, ScenarioConfiguration> loadRegistry(InputStream inputStream, String classpathBase) 
            throws ConfigurationException {
        
        if (inputStream == null) {
            throw new ConfigurationException("Input stream cannot be null");
        }
        
        logger.info("Loading scenario registry from input stream (classpathBase: {})", 
                   classpathBase != null ? classpathBase : "<none>");
        
        try {
            // Load the registry YAML configuration as a Map from stream
            Map<String, Object> registryConfig = configLoader.loadAsMap(inputStream);
            
            // Validate registry metadata
            validateRegistryMetadata(registryConfig, "<stream>");
            
            // Parse search-paths from registry (Phase 6 feature)
            SearchPathConfig registrySearchPaths = parseSearchPaths(registryConfig);
            
            // Parse scenarios section
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scenarioRegistry = (List<Map<String, Object>>) registryConfig.get("scenarios");
            
            if (scenarioRegistry == null || scenarioRegistry.isEmpty()) {
                throw new ConfigurationException(
                    "Scenario registry does not contain a 'scenarios' section or it is empty"
                );
            }
            
            // Load all referenced scenario files
            Map<String, ScenarioConfiguration> scenarios = new LinkedHashMap<>();
            
            for (Map<String, Object> registryEntry : scenarioRegistry) {
                String scenarioId = (String) registryEntry.get("scenario-id");
                String configFile = (String) registryEntry.get("config-file");

                if (scenarioId == null || scenarioId.trim().isEmpty()) {
                    logger.warn("Skipping registry entry with missing or empty scenario-id");
                    continue;
                }

                if (configFile == null || configFile.trim().isEmpty()) {
                    throw new ConfigurationException(
                        "Scenario '" + scenarioId + "' in registry has missing or empty config-file"
                    );
                }

                // Parse enabled flag from registry entry (default: true)
                boolean enabled = parseEnabledFlag(registryEntry);

                logger.debug("Loading scenario '{}' from classpath: {} (enabled: {})", scenarioId, configFile, enabled);

                // Try to resolve using search paths first (Phase 6 feature)
                ResolvedPath resolved = resolveConfigFileWithSearchPaths(configFile, registrySearchPaths, classpathBase, true);
                
                ScenarioConfiguration scenario;
                
                if (resolved != null) {
                    if (resolved.isClasspath()) {
                        scenario = loadScenarioFromClasspath(resolved.path());
                    } else {
                        scenario = loadScenarioFromFile(resolved.path());
                    }
                } else {
                    // Fallback to legacy resolution
                    String resolvedPath = resolveClasspathConfigFile(configFile, classpathBase);
                    scenario = loadScenarioFromClasspath(resolvedPath);
                }

                // Validate that scenario-id matches
                if (scenario.getScenarioId() != null && !scenario.getScenarioId().equals(scenarioId)) {
                    logger.warn(
                        "Scenario ID mismatch: registry specifies '{}' but config file contains '{}'. Using registry ID.",
                        scenarioId, scenario.getScenarioId()
                    );
                }

                // Ensure scenario has the correct ID from registry
                scenario.setScenarioId(scenarioId);

                // Set enabled flag from registry entry
                scenario.setEnabled(enabled);

                // Store in map
                scenarios.put(scenarioId, scenario);
                logger.debug("Successfully loaded scenario from classpath: {} (enabled: {})", scenarioId, enabled);
            }
            
            logger.info("Successfully loaded {} scenarios from registry stream", scenarios.size());
            return scenarios;
            
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Failed to load scenario registry from input stream", e);
        }
    }

    /**
     * Load a scenario registry from a classpath resource.
     * 
     * <p>This is a convenience method that combines resource lookup and stream-based
     * loading. The classpath base is automatically derived from the resource path
     * for resolving relative scenario file references.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * // Load registry from classpath, automatically resolving relative paths
     * Map&lt;String, ScenarioConfiguration&gt; scenarios = 
     *     loader.loadRegistryFromClasspath("scenarios/registry.yaml");
     * </pre>
     * 
     * @param resourcePath The classpath resource path to the registry file
     * @return A map of scenario configurations indexed by scenario-id
     * @throws ConfigurationException if the resource is not found or loading fails
     */
    public Map<String, ScenarioConfiguration> loadRegistryFromClasspath(String resourcePath) 
            throws ConfigurationException {
        
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            throw new ConfigurationException("Resource path cannot be null or empty");
        }
        
        logger.info("Loading scenario registry from classpath: {}", resourcePath);
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new ConfigurationException("Scenario registry resource not found: " + resourcePath);
            }
            
            // Derive classpath base from resource path
            String classpathBase = deriveClasspathBase(resourcePath);
            
            return loadRegistry(inputStream, classpathBase);
            
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load scenario registry from classpath: " + resourcePath, e);
        }
    }

    /**
     * Load a scenario configuration from a classpath resource.
     * 
     * @param resourcePath The classpath resource path to the scenario file
     * @return The loaded ScenarioConfiguration
     * @throws ConfigurationException if the resource is not found or loading fails
     */
    public ScenarioConfiguration loadScenarioFromClasspath(String resourcePath) throws ConfigurationException {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            throw new ConfigurationException("Resource path cannot be null or empty");
        }
        
        logger.debug("Loading scenario from classpath: {}", resourcePath);
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new ConfigurationException("Scenario resource not found: " + resourcePath);
            }
            
            ScenarioConfiguration scenario = loadScenarioFromStream(inputStream);

            // Attempt filesystem-relative stage path resolution.
            // When the classpath resource path also exists as a filesystem path (the
            // typical externalized-repo deployment), stage config-file values that are
            // short relative paths (e.g. "stages/validation.yaml") are resolved against
            // the scenario file's parent directory.  For truly classpath-only resources
            // the filesystem check in resolveStageConfigFilePaths fails silently and the
            // paths are left as-is for later classpath fallback.
            resolveStageConfigFilePaths(scenario, resourcePath);

            return scenario;
            
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load scenario from classpath: " + resourcePath, e);
        }
    }

    /**
     * Load a scenario configuration from an InputStream.
     * 
     * @param inputStream The input stream containing the scenario YAML content
     * @return The loaded ScenarioConfiguration
     * @throws ConfigurationException if the stream cannot be parsed
     */
    public ScenarioConfiguration loadScenarioFromStream(InputStream inputStream) throws ConfigurationException {
        if (inputStream == null) {
            throw new ConfigurationException("Input stream cannot be null");
        }
        
        try {
            // Load the YAML content as a Map from stream
            Map<String, Object> config = configLoader.loadAsMap(inputStream);
            
            // Look for 'scenario' section
            @SuppressWarnings("unchecked")
            Map<String, Object> scenarioData = (Map<String, Object>) config.get("scenario");
            
            if (scenarioData != null) {
                ScenarioConfiguration scenario = parseScenarioConfiguration(scenarioData);
                
                // Also parse metadata from the file level if scenario doesn't have it
                @SuppressWarnings("unchecked")
                Map<String, Object> fileMetadata = (Map<String, Object>) config.get("metadata");
                if (fileMetadata != null && scenario.getMetadata() == null) {
                    scenario.setMetadata(fileMetadata);
                }
                
                return scenario;
            } else {
                throw new ConfigurationException(
                    "Scenario configuration does not contain a 'scenario' section"
                );
            }
            
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Failed to load scenario from input stream", e);
        }
    }

    /**
     * Resolve a config file path for classpath loading.
     * 
     * @param configFile The config file path from the registry
     * @param classpathBase The classpath base for relative resolution (can be null)
     * @return The resolved classpath resource path
     */
    private String resolveClasspathConfigFile(String configFile, String classpathBase) {
        // If it looks like an absolute classpath path (starts with /), strip the leading /
        if (configFile.startsWith("/")) {
            return configFile.substring(1);
        }
        
        // If no classpath base, use the config file as-is
        if (classpathBase == null || classpathBase.isEmpty()) {
            return configFile;
        }
        
        // If the config file already starts with the base, use as-is
        if (configFile.startsWith(classpathBase)) {
            return configFile;
        }
        
        // Resolve relative to classpath base
        String base = classpathBase.endsWith("/") ? classpathBase : classpathBase + "/";
        
        // Handle "./" prefix
        if (configFile.startsWith("./")) {
            configFile = configFile.substring(2);
        }
        
        return base + configFile;
    }

    /**
     * Derive the classpath base directory from a resource path.
     * 
     * @param resourcePath The full resource path
     * @return The base directory path (with trailing slash) or empty string if at root
     */
    private String deriveClasspathBase(String resourcePath) {
        int lastSlash = resourcePath.lastIndexOf('/');
        if (lastSlash > 0) {
            return resourcePath.substring(0, lastSlash + 1);
        }
        return "";
    }
    
    /**
     * Validate registry metadata.
     * 
     * @param registryConfig The registry configuration map
     * @param registryPath The registry file path (for error messages)
     * @throws ConfigurationException if metadata is invalid
     */
    private void validateRegistryMetadata(Map<String, Object> registryConfig, String registryPath) 
            throws ConfigurationException {
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) registryConfig.get("metadata");
        
        if (metadata == null) {
            logger.warn("Scenario registry file does not contain metadata section: {}", registryPath);
            return;
        }
        
        String type = (String) metadata.get("type");
        if (type != null && !"scenario-registry".equals(type)) {
            logger.warn(
                "Registry metadata type is '{}' but expected 'scenario-registry': {}",
                type, registryPath
            );
        }
    }

    /**
     * Parse the enabled flag from a registry entry.
     *
     * <p>The enabled flag controls whether a scenario is active and can be used
     * for classification-based routing or direct execution. If not specified,
     * the default value is true (enabled).</p>
     *
     * @param registryEntry The registry entry map
     * @return true if enabled (default), false if explicitly disabled
     */
    private boolean parseEnabledFlag(Map<String, Object> registryEntry) {
        Object enabledValue = registryEntry.get("enabled");

        if (enabledValue == null) {
            // Default to enabled if not specified
            return true;
        }

        if (enabledValue instanceof Boolean) {
            return (Boolean) enabledValue;
        }

        if (enabledValue instanceof String) {
            return Boolean.parseBoolean((String) enabledValue);
        }

        // For any other type, default to enabled
        logger.warn("Invalid 'enabled' value type: {}. Defaulting to true.",
                   enabledValue.getClass().getSimpleName());
        return true;
    }

    /**
     * Resolve config file path relative to registry directory.
     *
     * @param configFile The config file path from registry
     * @param registryDir The directory containing the registry file
     * @return The resolved absolute path
     */
    private String resolveConfigFilePath(String configFile, Path registryDir) {
        Path configPath = Paths.get(configFile);

        // If absolute path, use as-is
        if (configPath.isAbsolute()) {
            return configFile;
        }

        // Check if the config file path already exists as-is (it might be a project-relative path)
        if (Files.exists(configPath)) {
            return configFile;
        }

        // If relative path, resolve relative to registry directory
        if (registryDir != null) {
            Path resolvedPath = registryDir.resolve(configFile);
            if (Files.exists(resolvedPath)) {
                return resolvedPath.toString();
            }
        }

        // Fallback to config file as-is (will fail later with proper error message)
        return configFile;
    }
    
    /**
     * Load a scenario configuration from a YAML file.
     * 
     * @param configFile The path to the scenario configuration file
     * @return The loaded ScenarioConfiguration
     * @throws ConfigurationException if the file cannot be loaded or parsed
     */
    private ScenarioConfiguration loadScenarioFromFile(String configFile) throws ConfigurationException {
        try {
            // Load the YAML file as a Map
            Map<String, Object> config = configLoader.loadAsMap(configFile);
            
            // Look for 'scenario' section
            @SuppressWarnings("unchecked")
            Map<String, Object> scenarioData = (Map<String, Object>) config.get("scenario");
            
            if (scenarioData != null) {
                ScenarioConfiguration scenario = parseScenarioConfiguration(scenarioData);
                
                // Also parse metadata from the file level if scenario doesn't have it
                @SuppressWarnings("unchecked")
                Map<String, Object> fileMetadata = (Map<String, Object>) config.get("metadata");
                if (fileMetadata != null && scenario.getMetadata() == null) {
                    scenario.setMetadata(fileMetadata);
                }

                // Resolve stage config-file paths relative to this scenario file's directory.
                // This allows externalized YAML repos to use short relative paths like
                // "stages/validation.yaml" rather than full absolute paths.
                resolveStageConfigFilePaths(scenario, configFile);

                return scenario;
            } else {
                throw new ConfigurationException(
                    "Scenario configuration file does not contain a 'scenario' section: " + configFile
                );
            }
            
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Failed to load scenario from file: " + configFile, e);
        }
    }

    /**
     * Resolves each stage's config-file path relative to the scenario file that declared it.
     *
     * <p>Resolution order (first match wins):
     * <ol>
     *   <li>Absolute path — used as-is.</li>
     *   <li>Exists relative to the JVM working directory — used as-is (preserves existing behaviour
     *       for tests that use full {@code src/test/java/...} prefixes).</li>
     *   <li>Resolves relative to the scenario file's parent directory — used when the file exists
     *       there (the new externalized-repo case).</li>
     *   <li>Left unchanged — will fail later with a meaningful "not found" message.</li>
     * </ol>
     *
     * @param scenario         the parsed scenario whose stages will be updated in-place
     * @param scenarioFilePath the path from which the scenario was loaded
     */
    private void resolveStageConfigFilePaths(ScenarioConfiguration scenario, String scenarioFilePath) {
        if (scenario.getProcessingStages() == null || scenario.getProcessingStages().isEmpty()) {
            return;
        }
        Path scenarioDir = Paths.get(scenarioFilePath).toAbsolutePath().getParent();
        for (ScenarioStage stage : scenario.getProcessingStages()) {
            String raw = stage.getConfigFile();
            if (raw == null || raw.isBlank()) {
                continue;
            }
            Path rawPath = Paths.get(raw);
            // Step 1: already absolute
            if (rawPath.isAbsolute()) {
                continue;
            }
            // Step 2: exists relative to CWD (full src/test/java/... style paths)
            if (Files.exists(rawPath)) {
                continue;
            }
            // Step 3: resolve relative to the scenario file's own directory
            Path resolved = scenarioDir.resolve(raw).normalize();
            if (Files.exists(resolved)) {
                logger.debug("Resolved stage '{}' config-file '{}' -> '{}'",
                    stage.getStageName(), raw, resolved);
                stage.setConfigFile(resolved.toString());
                continue;
            }
            // Step 4: leave as-is — loader will emit a clear "not found" error
        }
    }

    /**
     * Parse scenario configuration from YAML data map.
     *
     * @param scenarioData The scenario data map from YAML
     * @return Parsed ScenarioConfiguration
     */
    /**
     * Parse scenario configuration from YAML data map.
     * Delegates to {@link ScenarioParser} for all parsing logic.
     *
     * @param scenarioData The scenario data map from YAML
     * @return Parsed ScenarioConfiguration
     */
    private ScenarioConfiguration parseScenarioConfiguration(Map<String, Object> scenarioData) {
        ScenarioConfiguration scenario = scenarioParser.parseScenarioConfiguration(scenarioData);

        // Post-parse: validate component files for each stage (Loader-specific concern)
        if (scenario.getProcessingStages() != null) {
            for (ScenarioStage stage : scenario.getProcessingStages()) {
                if (stage.getConfigFile() != null) {
                    validateAndLogComponentFile(stage.getConfigFile(), stage.getStageName());
                }
            }
        }

        return scenario;
    }

    /**
     * Validate and log component file detection.
     *
     * <p>This method checks if a config file is a component file and logs the detection.
     * It also validates that the component file exists and is accessible.</p>
     *
     * @param configFile The config file path to check
     * @param stageName The stage name (for logging)
     */
    private void validateAndLogComponentFile(String configFile, String stageName) {
        try {
            // Check if this is a component file
            if (configLoader.isComponentFile(configFile)) {
                logger.info("Stage '{}' references a component file: {}", stageName, configFile);

                // Validate that the component file exists and is accessible
                // The isComponentFile() method already loads the file, so if we get here, it exists
                logger.debug("Component file '{}' validated successfully", configFile);
            } else {
                logger.debug("Stage '{}' references a regular config file: {}", stageName, configFile);
            }
        } catch (Exception e) {
            // Log warning but don't fail - the actual validation will happen at execution time
            logger.warn("Unable to validate config file '{}' for stage '{}': {}",
                       configFile, stageName, e.getMessage());
        }
    }

    // ========================================================================
    // Search Path Configuration Methods
    // ========================================================================

    /**
     * Initialize search paths from environment variables and system properties.
     * 
     * <p>Precedence (highest to lowest):</p>
     * <ol>
     *   <li>System properties: {@code apex.config.searchPaths}, {@code apex.config.classpathPrefixes}</li>
     *   <li>Environment variables: {@code APEX_CONFIG_SEARCH_PATHS}, {@code APEX_CONFIG_CLASSPATH_PREFIXES}</li>
     * </ol>
     */
    private void initializeFromEnvironment() {
        // System properties take precedence over environment variables
        String sysPropPaths = System.getProperty("apex.config.searchPaths");
        String sysPropPrefixes = System.getProperty("apex.config.classpathPrefixes");
        
        String envPaths = System.getenv("APEX_CONFIG_SEARCH_PATHS");
        String envPrefixes = System.getenv("APEX_CONFIG_CLASSPATH_PREFIXES");
        
        // Use system property if set, otherwise use environment variable
        String filesystemPaths = sysPropPaths != null ? sysPropPaths : envPaths;
        String classpathPrefixes = sysPropPrefixes != null ? sysPropPrefixes : envPrefixes;
        
        if (filesystemPaths != null && !filesystemPaths.trim().isEmpty()) {
            String separator = filesystemPaths.contains(";") ? ";" : 
                              (filesystemPaths.contains(":") && !filesystemPaths.matches("^[A-Za-z]:.*") ? ":" : ",");
            for (String path : filesystemPaths.split(separator)) {
                String trimmed = path.trim();
                if (!trimmed.isEmpty()) {
                    globalFilesystemPaths.add(trimmed);
                    logger.debug("Added global filesystem search path from environment: {}", trimmed);
                }
            }
        }
        
        if (classpathPrefixes != null && !classpathPrefixes.trim().isEmpty()) {
            String separator = classpathPrefixes.contains(";") ? ";" : 
                              (classpathPrefixes.contains(":") ? ":" : ",");
            for (String prefix : classpathPrefixes.split(separator)) {
                String trimmed = prefix.trim();
                if (!trimmed.isEmpty()) {
                    globalClasspathPrefixes.add(trimmed);
                    logger.debug("Added global classpath prefix from environment: {}", trimmed);
                }
            }
        }
    }

    /**
     * Add a global filesystem search path.
     * 
     * <p>Global paths are searched after registry-specific paths.</p>
     *
     * @param path The filesystem path to add
     * @return this loader for method chaining
     */
    public ScenarioRegistryLoader addSearchPath(String path) {
        if (path != null && !path.trim().isEmpty()) {
            globalFilesystemPaths.add(path.trim());
            logger.debug("Added global filesystem search path: {}", path);
        }
        return this;
    }

    /**
     * Add a global classpath prefix.
     * 
     * <p>Global prefixes are searched after registry-specific prefixes.</p>
     *
     * @param prefix The classpath prefix to add (e.g., "apex/", "META-INF/apex/")
     * @return this loader for method chaining
     */
    public ScenarioRegistryLoader addClasspathPrefix(String prefix) {
        if (prefix != null && !prefix.trim().isEmpty()) {
            globalClasspathPrefixes.add(prefix.trim());
            logger.debug("Added global classpath prefix: {}", prefix);
        }
        return this;
    }

    /**
     * Set global filesystem search paths, replacing any existing paths.
     *
     * @param paths The list of filesystem paths
     * @return this loader for method chaining
     */
    public ScenarioRegistryLoader setSearchPaths(List<String> paths) {
        globalFilesystemPaths.clear();
        if (paths != null) {
            for (String path : paths) {
                addSearchPath(path);
            }
        }
        return this;
    }

    /**
     * Set global classpath prefixes, replacing any existing prefixes.
     *
     * @param prefixes The list of classpath prefixes
     * @return this loader for method chaining
     */
    public ScenarioRegistryLoader setClasspathPrefixes(List<String> prefixes) {
        globalClasspathPrefixes.clear();
        if (prefixes != null) {
            for (String prefix : prefixes) {
                addClasspathPrefix(prefix);
            }
        }
        return this;
    }

    /**
     * Get the configured global filesystem search paths.
     *
     * @return An unmodifiable list of search paths
     */
    public List<String> getSearchPaths() {
        return Collections.unmodifiableList(globalFilesystemPaths);
    }

    /**
     * Get the configured global classpath prefixes.
     *
     * @return An unmodifiable list of classpath prefixes
     */
    public List<String> getClasspathPrefixes() {
        return Collections.unmodifiableList(globalClasspathPrefixes);
    }

    /**
     * Parse search-paths configuration from registry YAML.
     *
     * @param registryConfig The registry configuration map
     * @return SearchPathConfig containing parsed paths
     */
    @SuppressWarnings("unchecked")
    private SearchPathConfig parseSearchPaths(Map<String, Object> registryConfig) {
        SearchPathConfig config = new SearchPathConfig();
        
        Map<String, Object> searchPaths = (Map<String, Object>) registryConfig.get("search-paths");
        if (searchPaths == null) {
            return config;
        }
        
        // Parse filesystem paths
        Object filesystemObj = searchPaths.get("filesystem");
        if (filesystemObj instanceof List) {
            for (Object pathObj : (List<?>) filesystemObj) {
                if (pathObj != null) {
                    String path = expandEnvironmentVariables(pathObj.toString().trim());
                    if (!path.isEmpty()) {
                        config.filesystemPaths.add(path);
                        logger.debug("Registry search path (filesystem): {}", path);
                    }
                }
            }
        }
        
        // Parse classpath prefixes
        Object classpathObj = searchPaths.get("classpath");
        if (classpathObj instanceof List) {
            for (Object prefixObj : (List<?>) classpathObj) {
                if (prefixObj != null) {
                    String prefix = expandEnvironmentVariables(prefixObj.toString().trim());
                    if (!prefix.isEmpty()) {
                        config.classpathPrefixes.add(prefix);
                        logger.debug("Registry search path (classpath): {}", prefix);
                    }
                }
            }
        }
        
        return config;
    }

    /**
     * Expand environment variables in a path string.
     * Supports ${VAR_NAME} syntax.
     *
     * @param path The path that may contain environment variables
     * @return The path with environment variables expanded
     */
    private String expandEnvironmentVariables(String path) {
        if (path == null || !path.contains("${")) {
            return path;
        }
        
        String result = path;
        int start;
        while ((start = result.indexOf("${")) != -1) {
            int end = result.indexOf("}", start);
            if (end == -1) break;
            
            String varName = result.substring(start + 2, end);
            String value = System.getenv(varName);
            if (value == null) {
                value = System.getProperty(varName, "");
            }
            result = result.substring(0, start) + value + result.substring(end + 1);
        }
        
        return result;
    }

    /**
     * Resolve a config file using search paths.
     * 
     * <p>Resolution order:</p>
     * <ol>
     *   <li>Absolute path or classpath: prefix - use directly</li>
     *   <li>Registry-level filesystem paths (in order)</li>
     *   <li>Registry-level classpath prefixes (in order)</li>
     *   <li>Global filesystem paths (in order)</li>
     *   <li>Global classpath prefixes (in order)</li>
     *   <li>Relative to fallback base (filesystem or classpath)</li>
     * </ol>
     *
     * @param configFile The config file reference from the registry
     * @param searchPathConfig The registry-specific search paths
     * @param fallbackBase The fallback base path (registry directory or classpath base)
     * @param useClasspath If true, fallback is a classpath base; if false, filesystem
     * @return The resolved config file path/resource, or null if not found
     */
    private ResolvedPath resolveConfigFileWithSearchPaths(
            String configFile, 
            SearchPathConfig searchPathConfig,
            String fallbackBase,
            boolean useClasspath) {
        
        logger.debug("Resolving config file '{}' with search paths", configFile);
        
        // 1. Check for absolute path
        if (isAbsolutePath(configFile)) {
            logger.debug("Config file is absolute path: {}", configFile);
            return new ResolvedPath(configFile, false);
        }
        
        // 2. Check for explicit classpath: prefix
        if (configFile.startsWith("classpath:")) {
            String classpathResource = configFile.substring("classpath:".length());
            logger.debug("Config file uses classpath: prefix: {}", classpathResource);
            return new ResolvedPath(classpathResource, true);
        }
        
        // 2.5. Try the raw path directly as a classpath resource
        if (classpathResourceExists(configFile)) {
            logger.debug("Found config file as direct classpath resource: {}", configFile);
            return new ResolvedPath(configFile, true);
        }
        
        // 3. Try registry-level filesystem paths
        for (String searchPath : searchPathConfig.filesystemPaths) {
            String candidate = combinePath(searchPath, configFile);
            if (fileExists(candidate)) {
                logger.debug("Found config file in registry filesystem path: {}", candidate);
                return new ResolvedPath(candidate, false);
            }
        }
        
        // 4. Try registry-level classpath prefixes
        for (String prefix : searchPathConfig.classpathPrefixes) {
            String candidate = combineClasspath(prefix, configFile);
            if (classpathResourceExists(candidate)) {
                logger.debug("Found config file in registry classpath prefix: {}", candidate);
                return new ResolvedPath(candidate, true);
            }
        }
        
        // 5. Try global filesystem paths
        for (String searchPath : globalFilesystemPaths) {
            String candidate = combinePath(searchPath, configFile);
            if (fileExists(candidate)) {
                logger.debug("Found config file in global filesystem path: {}", candidate);
                return new ResolvedPath(candidate, false);
            }
        }
        
        // 6. Try global classpath prefixes
        for (String prefix : globalClasspathPrefixes) {
            String candidate = combineClasspath(prefix, configFile);
            if (classpathResourceExists(candidate)) {
                logger.debug("Found config file in global classpath prefix: {}", candidate);
                return new ResolvedPath(candidate, true);
            }
        }
        
        // 7. Fallback to base path
        if (fallbackBase != null && !fallbackBase.isEmpty()) {
            if (useClasspath) {
                String candidate = combineClasspath(fallbackBase, configFile);
                if (classpathResourceExists(candidate)) {
                    logger.debug("Found config file relative to classpath base: {}", candidate);
                    return new ResolvedPath(candidate, true);
                }
            } else {
                String candidate = combinePath(fallbackBase, configFile);
                if (fileExists(candidate)) {
                    logger.debug("Found config file relative to registry directory: {}", candidate);
                    return new ResolvedPath(candidate, false);
                }
            }
        }
        
        // Not found - return null to allow caller to handle
        logger.error("Config file '{}' not found in any search path", configFile);
        return null;
    }

    /**
     * Check if a path is absolute.
     */
    private boolean isAbsolutePath(String path) {
        if (path == null || path.isEmpty()) return false;
        // Unix absolute path
        if (path.startsWith("/")) return true;
        // Windows absolute path (C:\, D:\, etc.)
        if (path.length() >= 3 && path.charAt(1) == ':' && (path.charAt(2) == '\\' || path.charAt(2) == '/')) {
            return true;
        }
        return false;
    }

    /**
     * Combine a base path with a relative path.
     */
    private String combinePath(String basePath, String relativePath) {
        if (basePath == null || basePath.isEmpty()) return relativePath;
        String base = basePath.endsWith("/") || basePath.endsWith("\\") 
                     ? basePath : basePath + "/";
        return base + relativePath;
    }

    /**
     * Combine a classpath prefix with a resource path.
     */
    private String combineClasspath(String prefix, String resourcePath) {
        if (prefix == null || prefix.isEmpty()) return resourcePath;
        String base = prefix.endsWith("/") ? prefix : prefix + "/";
        // Remove leading slash from resource if present
        String resource = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        return base + resource;
    }

    /**
     * Check if a filesystem file exists.
     */
    private boolean fileExists(String path) {
        try {
            return Files.exists(Paths.get(path));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a classpath resource exists.
     */
    private boolean classpathResourceExists(String resourcePath) {
        return getClass().getClassLoader().getResource(resourcePath) != null;
    }

    /**
     * Configuration holder for registry-specific search paths.
     */
    private static class SearchPathConfig {
        final List<String> filesystemPaths = new ArrayList<>();
        final List<String> classpathPrefixes = new ArrayList<>();
    }

    /**
     * Resolved path holder indicating whether path is filesystem or classpath.
     * This class is public to allow testing and external resolution queries.
     */
    public static class ResolvedPath {
        private final String path;
        private final Type type;
        
        /**
         * The type of resolved path.
         */
        public enum Type {
            /** Path is a filesystem path */
            FILESYSTEM,
            /** Path is a classpath resource */
            CLASSPATH
        }
        
        ResolvedPath(String path, boolean isClasspath) {
            this.path = path;
            this.type = isClasspath ? Type.CLASSPATH : Type.FILESYSTEM;
        }
        
        /**
         * Get the resolved path string.
         * @return The path
         */
        public String path() {
            return path;
        }
        
        /**
         * Get the type of resolution (filesystem or classpath).
         * @return The resolution type
         */
        public Type type() {
            return type;
        }
        
        /**
         * Check if this is a classpath resource.
         * @return true if classpath, false if filesystem
         */
        public boolean isClasspath() {
            return type == Type.CLASSPATH;
        }
    }
    
    /**
     * Resolve a config file using search paths.
     * 
     * <p>This is a public API for testing and external use. It uses the provided
     * registry and classpath paths along with any configured global paths.</p>
     * 
     * <p>Resolution order:</p>
     * <ol>
     *   <li>Absolute path - used directly</li>
     *   <li>Registry-level filesystem paths (in order)</li>
     *   <li>Global filesystem paths (in order)</li>
     *   <li>Registry-level classpath prefixes (in order)</li>
     *   <li>Global classpath prefixes (in order)</li>
     * </ol>
     *
     * @param configFile The config file reference
     * @param registryFilesystemPaths Registry-specific filesystem paths
     * @param registryClasspathPrefixes Registry-specific classpath prefixes
     * @return The resolved path, or null if not found
     */
    public ResolvedPath resolveConfigFileWithSearchPaths(
            String configFile, 
            List<String> registryFilesystemPaths,
            List<String> registryClasspathPrefixes) {
        
        SearchPathConfig searchPathConfig = new SearchPathConfig();
        if (registryFilesystemPaths != null) {
            searchPathConfig.filesystemPaths.addAll(registryFilesystemPaths);
        }
        if (registryClasspathPrefixes != null) {
            searchPathConfig.classpathPrefixes.addAll(registryClasspathPrefixes);
        }
        
        return resolveConfigFileWithSearchPaths(configFile, searchPathConfig, null, false);
    }
}

