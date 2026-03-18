package dev.mars.apex.engine.core;

import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.config.model.YamlRuntimeScriptConfig;
import dev.mars.apex.core.config.pipeline.PipelineConfiguration;
import dev.mars.apex.core.config.*;
import dev.mars.apex.core.script.*;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.config.exception.*;
import dev.mars.apex.core.config.loader.*;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.engine.execution.EnrichmentGroupExecutor;
import dev.mars.apex.engine.execution.PipelineExecutionManager;
import dev.mars.apex.engine.execution.RuleChainExecutor;
import dev.mars.apex.engine.execution.RuleGroupExecutor;
import dev.mars.apex.engine.execution.SequentialProcessor;
import dev.mars.apex.engine.scenario.ScenarioEvaluationManager;
import dev.mars.apex.engine.scenario.ScenarioParser;
import dev.mars.apex.engine.scenario.ScenarioRegistryManager;
import dev.mars.apex.engine.model.Rule;
import dev.mars.apex.engine.model.RuleBase;
import dev.mars.apex.engine.model.RuleGroup;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.core.service.data.external.DataSink;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.factory.DataSinkFactory;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.enrichment.EnrichmentProcessor;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import dev.mars.apex.core.service.scenario.ScenarioExecutionResult;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

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
 * This class implements a business rules engine using SpEL.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * This class implements a business rules engine using SpEL.
 * It provides a flexible, configurable rules system that can be easily extended
 * and modified without changing the core code.
 *
 * This class is responsible only for rule evaluation, not configuration.
 * Configuration is handled by the RulesEngineConfiguration class.
 *
 * <p><b>Recommended Usage:</b></p>
 * Use the static factory methods on this class to create RulesEngine instances:
 *
 * <p>Example:</p>
 * <pre>
 * // One-line pattern (simplest):
 * RulesEngine engine = RulesEngine.fromFile("config.yaml");
 *
 * // From pre-loaded configuration:
 * RulesEngine engine = RulesEngine.fromYamlConfig(yamlConfig);
 *
 * // Scenario-based:
 * RulesEngine engine = RulesEngine.fromScenarioRegistry("registry.yaml");
 * </pre>
 */
public class RulesEngine {
    private static final Logger logger = LoggerFactory.getLogger(RulesEngine.class);

    private enum LifecycleState {
        ACTIVE,
        SHUTTING_DOWN,
        SHUT_DOWN
    }

    private final ExpressionEvaluatorService evaluatorService;
    private final RulesEngineConfiguration configuration;
    private final ErrorRecoveryService errorRecoveryService;
    private final RulePerformanceMonitor performanceMonitor;
    private volatile EnrichmentProcessor enrichmentProcessor;  // volatile: re-assigned after construction during pipeline init
    private final UnifiedRuleEvaluator unifiedEvaluator;
    private final List<String> initializationErrors = new CopyOnWriteArrayList<>();
    private final ScenarioParser scenarioParser;  // For parsing scenario configurations
    private final ScenarioEvaluationManager scenarioEvaluationManager;  // For scenario evaluation
    private final ScenarioRegistryManager scenarioRegistryManager;  // For scenario registry operations
    private final EnrichmentGroupExecutor enrichmentGroupExecutor;  // For executing enrichment groups
    private final RuleGroupExecutor ruleGroupExecutor;  // For executing rule groups
    private final RuleChainExecutor ruleChainExecutor;  // For executing rule chains
    private final SequentialProcessor sequentialProcessor;  // For sequential document-order processing

    /**
     * The YAML configuration used to create this engine (if created via static factory methods).
     * This is stored to enable the simplified evaluate(Object) method.
     * Will be null if the engine was created directly via the constructor.
     */
    private final YamlRuleConfiguration yamlConfig;

    /**
     * Scenario registry for scenario-based evaluation.
     * Maps scenario IDs to their configurations.
     * Will be null if the engine was not created from a scenario registry.
     */
    private final Map<String, ScenarioConfiguration> scenarioRegistry;

    /**
     * Pipeline execution manager (handles pipeline initialization and execution).
     */
    private final PipelineExecutionManager pipelineExecutionManager;
    private final Map<String, ExternalDataSource> dataSources;
    private final Map<String, DataSink> dataSinks;
    private final Object lifecycleMonitor = new Object();
    private volatile LifecycleState lifecycleState = LifecycleState.ACTIVE;
    private final AtomicInteger activeEvaluations = new AtomicInteger();

    // Runtime script system components (null when runtime-scripts not configured)
    private final ScriptBridge scriptBridge;
    private final ScriptReloadManager scriptReloadManager;
    private final GroovyScriptCompiler scriptCompiler;
    private final ScriptExecutor scriptExecutor;

    /**
     * Create a new RulesEngine with the specified configuration.
     * This is the public constructor for RulesEngine.
     *
     * <p>For simpler usage with YAML files, consider using the static factory methods:
     * {@link #fromFile(String)} or {@link #fromYamlConfig(YamlRuleConfiguration)}.</p>
     *
     * @param configuration The configuration for this rules engine
     */
    public RulesEngine(RulesEngineConfiguration configuration) {
        this(configuration, null);
    }

    /**
     * Private constructor that accepts both configuration and yamlConfig.
     * Used by static factory methods to store the YAML configuration for simplified evaluate() method.
     *
     * @param configuration The configuration for this rules engine
     * @param yamlConfig The YAML configuration (can be null)
     */
    private RulesEngine(RulesEngineConfiguration configuration, YamlRuleConfiguration yamlConfig) {
        this(configuration, yamlConfig, null);
    }

    /**
     * Private constructor that accepts configuration, yamlConfig, and scenarioRegistry.
     * Used by fromScenarioRegistry() static factory method.
     *
     * @param configuration The configuration for this rules engine
     * @param yamlConfig The YAML configuration (can be null)
     * @param scenarioRegistry The scenario registry (can be null)
     */
    RulesEngine(RulesEngineConfiguration configuration, YamlRuleConfiguration yamlConfig,
                       Map<String, ScenarioConfiguration> scenarioRegistry) {
        this.configuration = configuration;
        this.yamlConfig = yamlConfig;
        this.scenarioRegistry = freezeScenarioRegistry(scenarioRegistry);
        this.evaluatorService = new ExpressionEvaluatorService(SpelParserHolder.INSTANCE);

        // Initialize runtime script system if configured
        ScriptBridge bridge = null;
        ScriptReloadManager reloader = null;
        GroovyScriptCompiler compiler = null;
        ScriptExecutor executor = null;
        if (yamlConfig != null && yamlConfig.getRuntimeScripts() != null
                && yamlConfig.getRuntimeScripts().isEnabled()) {
            YamlRuntimeScriptConfig scriptConfig = yamlConfig.getRuntimeScripts();
            try {
                List<Path> locations = resolveScriptLocations(scriptConfig);
                if (locations.isEmpty()) {
                    logger.warn("Runtime scripts configured but no valid filesystem locations were resolved");
                    throw new IllegalArgumentException("No valid runtime script locations configured");
                }
                RuntimeScriptRegistry scriptRegistry = new RuntimeScriptRegistry(
                        locations, scriptConfig.getAllowlist());
                scriptRegistry.loadScripts();

                compiler = new GroovyScriptCompiler(
                        scriptConfig.getFailMode());
                executor = new ScriptExecutor();

                bridge = new ScriptBridge(scriptRegistry, compiler, executor,
                        scriptConfig.getExecutionTimeoutMs());
                this.evaluatorService.enableScriptBridge(bridge);

                if (scriptConfig.getPollingIntervalMs() > 0) {
                    reloader = new ScriptReloadManager(
                            scriptRegistry, compiler, scriptConfig.getPollingIntervalMs());
                    reloader.start();
                }
                logger.info("Runtime script system initialized with {} location(s): {}", locations.size(), locations);
            } catch (Exception e) {
                logger.error("Failed to initialize runtime script system", e);
                if (executor != null) {
                    executor.shutdown();
                }
                if (compiler != null) {
                    compiler.close();
                }
                throw new IllegalStateException("Failed to initialize runtime script system", e);
            }
        }
        this.scriptBridge = bridge;
        this.scriptReloadManager = reloader;
        this.scriptCompiler = compiler;
        this.scriptExecutor = executor;

        this.errorRecoveryService = new ErrorRecoveryService();
        this.performanceMonitor = new RulePerformanceMonitor();
        this.scenarioParser = new ScenarioParser();  // Initialize scenario parser
        this.scenarioRegistryManager = new ScenarioRegistryManager(
            this.scenarioRegistry,
            this.evaluatorService
        );
        this.scenarioEvaluationManager = new ScenarioEvaluationManager(
            yamlConfig,
            this.scenarioRegistry,
            this.scenarioParser,
            new ScenarioLookupStrategyImpl()
        );
        // Load error recovery configuration from YAML if available, otherwise use defaults
        ErrorRecoveryConfig errorRecoveryConfig = loadErrorRecoveryConfig(yamlConfig);

        // Initialize the unified evaluator with error recovery configuration from YAML
        this.unifiedEvaluator = new UnifiedRuleEvaluator(this.evaluatorService, errorRecoveryService, performanceMonitor, errorRecoveryConfig);

        // Initialize ruleGroupExecutor first so its evaluation service is available for enrichmentProcessor
        this.ruleGroupExecutor = new RuleGroupExecutor(this.unifiedEvaluator);

        // Note: enrichmentProcessor will be re-initialized after data sources are created
        // to ensure it has access to the data source registry.
        // pass RuleGroupEvaluationService so rule groups within enrichments
        // are evaluated through the canonical UnifiedRuleEvaluator path.
        this.enrichmentProcessor = new EnrichmentProcessor(
            new LookupServiceRegistry(), this.evaluatorService, null,
            this.ruleGroupExecutor.getGroupEvaluationService());

        // Initialize remaining executors (after dependencies are initialized)
        this.enrichmentGroupExecutor = new EnrichmentGroupExecutor(this.enrichmentProcessor);

        // Wire the enrichment group executor back into the enrichment processor
        // to support function mapping type (breaks circular dependency via lazy supplier)
        this.enrichmentProcessor.setEnrichmentGroupExecutorSupplier(() -> this.enrichmentGroupExecutor);

        this.ruleChainExecutor = new RuleChainExecutor(this.unifiedEvaluator, this.enrichmentGroupExecutor);
        this.sequentialProcessor = new SequentialProcessor(
            this.configuration,
            this.enrichmentProcessor,
            this.unifiedEvaluator,
            this.evaluatorService,
            this.enrichmentGroupExecutor,
            this.ruleGroupExecutor,
            this.ruleChainExecutor
        );

        // Initialize pipeline components with thread-safe maps for parallel scenario evaluation
        this.dataSources = new java.util.concurrent.ConcurrentHashMap<>();
        this.dataSinks = new HashMap<>();
        this.pipelineExecutionManager = new PipelineExecutionManager(
            DataSourceFactory.getInstance(),
            DataSinkFactory.getInstance(),
            this.dataSources,
            this.dataSinks,
            this.initializationErrors,
            this.evaluatorService,
            this.ruleGroupExecutor.getGroupEvaluationService()
        );

        // Initialize data sources and sinks if yamlConfig is provided
        if (yamlConfig != null) {
            EnrichmentProcessor updatedProcessor = pipelineExecutionManager.initializePipelineComponents(yamlConfig);
            if (updatedProcessor != null) {
                this.enrichmentProcessor = updatedProcessor;
            }
        }

        logger.info("RulesEngine initialized with configuration: {}", configuration.getClass().getSimpleName());
        logger.debug("Using parser: {}", SpelParserHolder.INSTANCE.getClass().getSimpleName());
        logger.debug("Using error recovery service: {}", errorRecoveryService.getClass().getSimpleName());
        logger.debug("Using error recovery config: enabled={}, default-strategy={}",errorRecoveryConfig.isEnabled(), errorRecoveryConfig.getDefaultStrategy());
        logger.debug("Using performance monitor: {}", performanceMonitor.getClass().getSimpleName());
        logger.debug("Using enrichment processor: {}", enrichmentProcessor != null ? enrichmentProcessor.getClass().getSimpleName() : "none");
    }

    private List<Path> resolveScriptLocations(YamlRuntimeScriptConfig scriptConfig) {
        if (scriptConfig.getLocations() == null || scriptConfig.getLocations().isEmpty()) {
            return List.of();
        }

        List<Path> resolved = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        for (String rawLocation : scriptConfig.getLocations()) {
            if (rawLocation == null || rawLocation.trim().isEmpty()) {
                continue;
            }

            String location = rawLocation.trim();
            try {
                if (location.startsWith("classpath:")) {
                    String classpathRef = location.substring("classpath:".length());
                    while (classpathRef.startsWith("/")) {
                        classpathRef = classpathRef.substring(1);
                    }

                    URL url = classLoader.getResource(classpathRef);
                    if (url == null) {
                        logger.warn("Runtime script classpath location not found: {}", location);
                        continue;
                    }
                    if (!"file".equalsIgnoreCase(url.getProtocol())) {
                        logger.warn("Runtime script classpath location '{}' uses unsupported protocol '{}' (filesystem paths only)",
                                location, url.getProtocol());
                        continue;
                    }

                    URI uri = url.toURI();
                    resolved.add(Path.of(uri).toAbsolutePath().normalize());
                } else {
                    resolved.add(Path.of(location).toAbsolutePath().normalize());
                }
            } catch (IllegalArgumentException | URISyntaxException ex) {
                logger.warn("Ignoring invalid runtime script location '{}': {}", location, ex.getMessage());
            }
        }

        return resolved;
    }

    private static Map<String, ScenarioConfiguration> freezeScenarioRegistry(
            Map<String, ScenarioConfiguration> scenarioRegistry) {
        if (scenarioRegistry == null) {
            return null;
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(scenarioRegistry));
    }

    /**
     * Execute a pipeline configuration.
     * Delegates to PipelineExecutionManager.
     *
     * @param pipeline The pipeline configuration to execute
     * @param inputData The input data for the pipeline
     * @return RuleResult indicating success or failure
     */
    private RuleResult executePipeline(PipelineConfiguration pipeline, Map<String, Object> inputData) {
        return pipelineExecutionManager.executePipeline(pipeline, inputData);
    }

    // Static Factory Methods

    /**
     * Create a RulesEngine from a YAML file.
     * This is the simplest way to create a RulesEngine for most use cases.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * // Simple 2-line usage
     * RulesEngine engine = RulesEngine.fromFile("config.yaml");
     * RuleResult result = engine.evaluate(inputData);
     * </pre>
     *
     * @param filePath The path to the YAML configuration file
     * @return A configured RulesEngine ready to evaluate rules
     * @throws ConfigurationException if the file cannot be loaded or parsed
     */
    public static RulesEngine fromFile(String filePath) throws ConfigurationException {
        logger.info("Creating RulesEngine from file: {}", filePath);

        ConfigurationLoader loader = new ConfigurationLoader();
        YamlRuleConfiguration yamlConfig = loader.loadFromFile(filePath);

        RuleFactory ruleFactory = new RuleFactory();
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);

        return new RulesEngine(config, yamlConfig);
    }

    /**
     * Create a RulesEngine from a classpath resource.
     * This is the recommended method for loading test configurations and bundled resources.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * // Load from classpath (e.g., src/test/resources/config/test-config.yaml)
     * RulesEngine engine = RulesEngine.fromClasspath("config/test-config.yaml");
     * RuleResult result = engine.evaluate(inputData);
     * </pre>
     *
     * <p><b>Benefits over fromFile():</b></p>
     * <ul>
     *   <li>Works consistently across different environments (dev, CI/CD, production)</li>
     *   <li>No dependency on working directory or absolute paths</li>
     *   <li>Follows Maven/Gradle conventions for test resources</li>
     *   <li>Resources are packaged in JAR files automatically</li>
     * </ul>
     *
     * @param resourcePath The classpath resource path (e.g., "config/test-config.yaml")
     * @return A configured RulesEngine ready to evaluate rules
     * @throws ConfigurationException if the resource cannot be found or loaded
     * @since 2026-01-18
     */
    public static RulesEngine fromClasspath(String resourcePath) throws ConfigurationException {
        logger.info("Creating RulesEngine from classpath resource: {}", resourcePath);

        ConfigurationLoader loader = new ConfigurationLoader();
        YamlRuleConfiguration yamlConfig = loader.loadFromClasspath(resourcePath);

        RuleFactory ruleFactory = new RuleFactory();
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);

        return new RulesEngine(config, yamlConfig);
    }

    /**
     * Create a RulesEngine from a YamlRuleConfiguration object.
     * Use this when you need to inspect or modify the YAML configuration before creating the engine.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * // Advanced usage with config inspection
     * ConfigurationLoader loader = new ConfigurationLoader();
     * YamlRuleConfiguration yamlConfig = loader.loadFromFile("config.yaml");
     *
     * // Inspect or modify config if needed
     * if (yamlConfig.getMetadata() != null) {
     *     System.out.println("Config version: " + yamlConfig.getMetadata().getVersion());
     * }
     *
     * RulesEngine engine = RulesEngine.fromYamlConfig(yamlConfig);
     * RuleResult result = engine.evaluate(inputData);
     * </pre>
     *
     * @param yamlConfig The YAML configuration object
     * @return A configured RulesEngine ready to evaluate rules
     * @throws ConfigurationException if the configuration cannot be processed
     */
    public static RulesEngine fromYamlConfig(YamlRuleConfiguration yamlConfig) throws ConfigurationException {
        logger.info("Creating RulesEngine from YamlRuleConfiguration");

        RuleFactory ruleFactory = new RuleFactory();
        RulesEngineConfiguration config = ruleFactory.createRulesEngineConfiguration(yamlConfig);

        return new RulesEngine(config, yamlConfig);
    }

    /**
     * Safely evaluate a YAML configuration string against input data.
     * 
     * <p>This method follows the APEX error handling contract: ALL errors are returned
     * via RuleResult, never thrown as exceptions. This includes:</p>
     * <ul>
     *   <li>YAML parsing errors</li>
     *   <li>YAML validation errors</li>
     *   <li>Engine initialization errors</li>
     *   <li>Runtime evaluation errors</li>
     * </ul>
     * 
     * <p><b>Usage Example:</b></p>
     * <pre>
     * String yaml = """
     *     metadata:
     *       name: "My Rules"
     *     rules:
     *       - id: "rule-1"
     *         condition: "#value > 100"
     *         message: "Value exceeds threshold"
     *     """;
     * 
     * Map&lt;String, Object&gt; data = Map.of("value", 150);
     * RuleResult result = RulesEngine.evaluateYaml(yaml, data);
     * 
     * if (!result.isSuccess()) {
     *     // Handle error - check result.getFailureMessages() for details
     * }
     * </pre>
     *
     * @param yamlString The YAML configuration as a string
     * @param inputData The input data to evaluate against
     * @return RuleResult containing either success with enriched data, or failure with error details
     */
    public static RuleResult evaluateYaml(String yamlString, Map<String, Object> inputData) {
        logger.info("Starting safe YAML evaluation (no exceptions thrown)");
        ConfigurationLoader loader = new ConfigurationLoader();
        return safeEvaluate(() -> loader.fromYamlString(yamlString), inputData);
    }

    /**
     * Safely evaluate a YAML configuration file against input data.
     * 
     * <p>This method follows the APEX error handling contract: ALL errors are returned
     * via RuleResult, never thrown as exceptions.</p>
     *
     * @param yamlFilePath The path to the YAML configuration file
     * @param inputData The input data to evaluate against
     * @return RuleResult containing either success with enriched data, or failure with error details
     */
    public static RuleResult evaluateYamlFile(String yamlFilePath, Map<String, Object> inputData) {
        logger.info("Starting safe YAML file evaluation: {}", yamlFilePath);
        ConfigurationLoader loader = new ConfigurationLoader();
        return safeEvaluate(() -> loader.loadFromFile(yamlFilePath), inputData);
    }

    /**
     * Shared error-handling helper for safe YAML evaluation methods.
     * Encapsulates the parse → create engine → evaluate pattern with consistent error handling.
     *
     * <p>All errors are captured and returned as {@link RuleResult} — never thrown.</p>
     *
     * @param configLoader Callable that loads and parses the YAML configuration (may throw checked exceptions)
     * @param inputData The input data to evaluate against
     * @return RuleResult containing either success with enriched data, or failure with error details
     */
    private static RuleResult safeEvaluate(Callable<YamlRuleConfiguration> configLoader, Map<String, Object> inputData) {
        try {
            // Step 1: Parse and validate YAML
            YamlRuleConfiguration yamlConfig = configLoader.call();

            // Step 2: Create engine
            RulesEngine engine = fromYamlConfig(yamlConfig);

            // Step 3: Evaluate
            return engine.evaluate(inputData);

        } catch (ConfigurationException e) {
            logger.error("[APEX-CFG-001] YAML configuration error: {}", e.getMessage());
            logger.debug("Full exception details for YAML configuration error:", e);
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("[APEX-CFG-001] YAML configuration error: " + e.getMessage());
            if (e.getCause() != null) {
                failureMessages.add("Caused by: " + e.getCause().getMessage());
            }
            Map<String, Object> data = inputData != null ? new HashMap<>(inputData) : new HashMap<>();
            return RuleResult.evaluationFailure(failureMessages, data, "yaml-configuration", "YAML configuration error", SeverityConstants.ERROR);

        } catch (Exception e) {
            logger.error("[APEX-RULE-999] Unexpected error during YAML evaluation: {}", e.getMessage());
            logger.debug("Full exception details:", e);
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("[APEX-RULE-999] Unexpected error: " + e.getMessage());
            Map<String, Object> data = inputData != null ? new HashMap<>(inputData) : new HashMap<>();
            return RuleResult.evaluationFailure(failureMessages, data, "unexpected-error", "Unexpected error during evaluation", SeverityConstants.ERROR);
        }
    }

    /**
     * Create a RulesEngine from a scenario registry file.
     *
     * <p>This static factory method loads a scenario registry YAML file that contains
     * multiple scenario definitions. Each scenario in the registry can be evaluated
     * by ID or through classification-based routing.</p>
     *
     * <p><b>Scenario Registry YAML Structure:</b></p>
     * <pre>
     * scenario-registry:
     *   scenarios:
     *     - scenario-id: "basic-trade-processing"
     *       config-file: "scenarios/basic-trade-processing.yaml"
     *     - scenario-id: "complex-trade-processing"
     *       config-file: "scenarios/complex-trade-processing.yaml"
     * </pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>
     * // Create engine from scenario registry
     * RulesEngine engine = RulesEngine.fromScenarioRegistry("registry.yaml");
     *
     * // Evaluate specific scenario by ID
     * ScenarioExecutionResult result = engine.evaluateScenario("basic-trade-processing", data);
     *
     * // Or use classification-based routing
     * ScenarioExecutionResult result = engine.evaluateWithClassification(data);
     *
     * // Or use fluent API
     * ScenarioExecutionResult result = engine.asScenario()
     *     .evaluate("basic-trade-processing", data);
     * </pre>
     *
     * @param registryPath The path to the scenario registry YAML file
     * @return A configured RulesEngine ready to evaluate scenarios
     * @throws ConfigurationException if the registry file cannot be loaded or parsed
     * @since 2025-11-03
     * @see #evaluateScenario(String, Map)
     * @see #evaluateWithClassification(Map)
     * @see #asScenario()
     */
    public static RulesEngine fromScenarioRegistry(String registryPath) throws ConfigurationException {
        logger.info("Creating RulesEngine from scenario registry: {}", registryPath);

        ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
        Map<String, ScenarioConfiguration> scenarios;
        
        // Try classpath first (enables JAR-packaged resources and test resources)
        try (java.io.InputStream is = RulesEngine.class.getClassLoader().getResourceAsStream(registryPath)) {
            if (is != null) {
                // Derive classpath base for relative path resolution
                String classpathBase = deriveClasspathBase(registryPath);
                scenarios = loader.loadRegistry(is, classpathBase);
                logger.info("Loaded {} scenarios from classpath registry: {}", scenarios.size(), registryPath);
            } else {
                // Fallback to filesystem loading (existing behavior)
                scenarios = loader.loadRegistry(registryPath);
                logger.info("Loaded {} scenarios from filesystem registry: {}", scenarios.size(), registryPath);
            }
        } catch (java.io.IOException e) {
            throw new ConfigurationException("Failed to load scenario registry: " + registryPath, e);
        }

        if (scenarios == null || scenarios.isEmpty()) {
            throw new ConfigurationException(
                "Scenario registry is empty or failed to load: " + registryPath
            );
        }

        // Create a minimal RulesEngineConfiguration for scenario-only engine
        RulesEngineConfiguration config = new RulesEngineConfiguration();

        // Create RulesEngine with scenario registry
        return new RulesEngine(config, null, scenarios);
    }

    /**
     * Derive the classpath base directory from a resource path.
     * Used for resolving relative config-file references in scenario registries.
     * 
     * @param resourcePath The full resource path
     * @return The base directory path (with trailing slash) or empty string if at root
     */
    private static String deriveClasspathBase(String resourcePath) {
        int lastSlash = resourcePath.lastIndexOf('/');
        return lastSlash > 0 ? resourcePath.substring(0, lastSlash + 1) : "";
    }

    /**
     * Get the configuration for this rules engine.
     *
     * @return The configuration for this rules engine
     */
    public RulesEngineConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Get the performance monitor for this rules engine.
     *
     * @return The performance monitor
     */
    public RulePerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    /**
     * Get the scenario registry for this rules engine.
     * 
     * <p>The scenario registry maps scenario IDs to their configurations.
     * This is only populated when the engine is created via 
     * {@link #fromScenarioRegistry(String)}.</p>
     *
     * @return An unmodifiable scenario registry snapshot, or null if not a scenario-based engine
     * @since 2025-11-03
     */
    public Map<String, ScenarioConfiguration> getScenarioRegistry() {
        return scenarioRegistry;
    }

    // Rule Execution Methods

    /**
     * Create an evaluation context with the provided facts.
     *
     * @param facts The facts to add to the context
     * @return A new StandardEvaluationContext with the facts added as variables
     */
    private StandardEvaluationContext createContext(Map<String, Object> facts) {
        logger.debug("Creating evaluation context via ExpressionEvaluatorService");
        return evaluatorService.createEvaluationContext(facts);
    }

    /**
     * Execute a single Rule object against the provided facts.
     *
     * @param rule The Rule object to execute
     * @param facts The facts to evaluate the rule against
     * @return The result of the rule evaluation, indicating whether it matched or not
     */
    public RuleResult executeRule(Rule rule, Map<String, Object> facts) {
        return runWhileEngineActive("executeRule", () -> executeRuleInternal(rule, facts));
    }

    private RuleResult executeRuleInternal(Rule rule, Map<String, Object> facts) {
        // Delegate to the unified evaluator for consistent behavior
        // Note: result-field storage is handled by UnifiedRuleEvaluator.evaluateRule(Rule, Map)
        // which supports nested field paths and enrichedData population
        return unifiedEvaluator.evaluateRule(rule, facts);
    }

    /**
     * Execute a list of Rule objects against the provided facts.
     *
     * @param rules The list of Rule objects to execute
     * @param facts The facts to evaluate the rules against
     * @return The result of the first rule that matches, or a default result if no rules match
     */
    public RuleResult executeRulesList(List<Rule> rules, Map<String, Object> facts) {
        return runWhileEngineActive("executeRulesList", () -> executeRulesListInternal(rules, facts));
    }

    private RuleResult executeRulesListInternal(List<Rule> rules, Map<String, Object> facts) {
        // Delegate to the unified evaluator for consistent behavior
        return unifiedEvaluator.evaluateRules(rules, facts);
    }

    /**
     * Execute a list of RuleGroup objects against the provided facts.
     * Delegates to RuleGroupExecutor for actual execution.
     *
     * @param ruleGroups The list of RuleGroup objects to execute
     * @param facts The facts to evaluate the rule groups against
     * @return The result of the first rule group that matches, or a default result if no rule groups match
     */
    public RuleResult executeRuleGroupsList(List<RuleGroup> ruleGroups, Map<String, Object> facts) {
        return runWhileEngineActive("executeRuleGroupsList", () -> executeRuleGroupsListInternal(ruleGroups, facts));
    }

    private RuleResult executeRuleGroupsListInternal(List<RuleGroup> ruleGroups, Map<String, Object> facts) {
        StandardEvaluationContext context = createContext(facts);
        return ruleGroupExecutor.executeRuleGroupsList(ruleGroups, facts, context);
    }



    /**
     * Execute a list of rules against the provided facts.
     * This method determines the type of objects in the list and delegates to the appropriate method.
     * Delegates to RuleGroupExecutor for actual execution.
     *
     * @param rules The list of rules to execute (can be a mix of Rule and RuleGroup objects)
     * @param facts The facts to evaluate the rules against
     * @return The result of the first rule that matches, or a default result if no rules match
     */
    public RuleResult executeRules(List<RuleBase> rules, Map<String, Object> facts) {
        return runWhileEngineActive("executeRules", () -> executeRulesInternal(rules, facts));
    }

    private RuleResult executeRulesInternal(List<RuleBase> rules, Map<String, Object> facts) {
        StandardEvaluationContext context = createContext(facts);
        return ruleGroupExecutor.executeRules(rules, facts, context);
    }

    /**
     * Execute rules for a specific category against the provided facts.
     *
     * @param category The category of rules to execute
     * @param facts The facts to evaluate the rules against
     * @return The result of the first rule that matches, or a default result if no rules match
     */
    public RuleResult executeRulesForCategory(String category, Map<String, Object> facts) {
        return runWhileEngineActive("executeRulesForCategory", () -> executeRulesForCategoryInternal(category, facts));
    }

    private RuleResult executeRulesForCategoryInternal(String category, Map<String, Object> facts) {
        logger.info("Executing rules for category: {}", category);
        List<RuleBase> rules = configuration.getRulesForCategory(category);
        logger.debug("Found {} rules/rule groups in category: {}", rules.size(), category);
        return executeRulesInternal(rules, facts);
    }





    /**
     * Unified evaluation method that processes both enrichments and rules, returning comprehensive results.
     * This method provides the complete APEX evaluation workflow with enrichment processing followed by rule evaluation.
     *
     * @param yamlConfig The YAML configuration containing enrichments and rules
     * @param inputData The input data to process
     * @return A comprehensive RuleResult containing success status, enriched data, and failure messages
     */
    public RuleResult evaluate(YamlRuleConfiguration yamlConfig, Map<String, Object> inputData) {
        return runWhileEngineActive("evaluate", () -> evaluateInternal(yamlConfig, inputData));
    }

    private RuleResult evaluateInternal(YamlRuleConfiguration yamlConfig, Map<String, Object> inputData) {
        logger.debug("Starting unified evaluation with enrichments and rules");

        // Check for initialization errors first
        if (!initializationErrors.isEmpty()) {
            logger.error("Engine has initialization errors: {}", initializationErrors);
            Map<String, Object> data = inputData != null ? new HashMap<>(inputData) : new HashMap<>();
            return RuleResult.evaluationFailure(initializationErrors, data, "initialization", "Engine initialization failed");
        }

        // Handle null inputs gracefully
        if (yamlConfig == null) {
            logger.error("YAML configuration is null");
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("YAML configuration is null");
            Map<String, Object> enrichedData = inputData != null ? new HashMap<>(inputData) : new HashMap<>();
            return RuleResult.evaluationFailure(failureMessages, enrichedData, "evaluation", "Null YAML configuration");
        }

        if (inputData == null) {
            logger.error("Input data is null");
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("Input data is null");
            return RuleResult.evaluationFailure(failureMessages, new HashMap<>(), "evaluation", "Null input data");
        }

        // Delegate to SequentialProcessor for document-order processing
        return sequentialProcessor.evaluateSequential(
            yamlConfig,
            inputData,
            ctx -> executeRuleInternal(ctx.getRule(), ctx.getData()),
            ctx -> executePipeline(ctx.getPipeline(), ctx.getData()),
            this::createContext
        );
    }

    /**
     * Simplified evaluation method that uses the stored YAML configuration.
     * This method is only available when the RulesEngine was created using static factory methods
     * ({@link #fromFile(String)} or {@link #fromYamlConfig(YamlRuleConfiguration)}).
     *
     * <p><b>Example:</b></p>
     * <pre>
     * RulesEngine engine = RulesEngine.fromFile("config.yaml");
     * RuleResult result = engine.evaluate(inputData);
     * </pre>
     *
     * @param inputData The input data to process
     * @return A comprehensive RuleResult containing success status, enriched data, and failure messages
     * @throws IllegalStateException if this engine was not created with a YAML configuration
     */
    public RuleResult evaluate(Map<String, Object> inputData) {
        return runWhileEngineActive("evaluate", () -> evaluateStoredConfiguration(inputData));
    }

    private RuleResult evaluateStoredConfiguration(Map<String, Object> inputData) {
        if (this.yamlConfig == null) {
            logger.error("[APEX-CFG-999] Cannot use simplified evaluate(Map) method - engine was not created with a YAML configuration");
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("Cannot use simplified evaluate(Map) method - this RulesEngine was not created with a YAML configuration. " +
                "Either use RulesEngine.fromFile() or RulesEngine.fromYamlConfig() to create the engine, " +
                "or use the explicit evaluate(YamlRuleConfiguration, Map) method instead.");
            return RuleResult.evaluationFailure(failureMessages, inputData != null ? new HashMap<>(inputData) : new HashMap<>(),
                "evaluation", "Engine not configured with YAML configuration", SeverityConstants.ERROR);
        }

        return evaluateInternal(this.yamlConfig, inputData);
    }

    // ========================================
    // Scenario Evaluation Methods (Style 1: Direct Methods)
    // ========================================

    /**
     * Evaluate a single scenario configuration with the provided input data.
     *
     * <p>This method is used when the RulesEngine was created from a single
     * scenario configuration file (not a registry). It processes the input data
     * through all stages defined in the scenario configuration.</p>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * RulesEngine engine = RulesEngine.fromFile("scenario-config.yaml");
     * Map&lt;String, Object&gt; data = new HashMap&lt;&gt;();
     * data.put("tradeType", "OTCOption");
     * data.put("notional", 1000000);
     *
     * ScenarioExecutionResult result = engine.evaluateScenario(data);
     * if (result.isSuccessful()) {
     *     System.out.println("Scenario executed successfully");
     * }
     * </pre>
     *
     * @param inputData The input data to process through the scenario stages.
     *                  Must be a Map containing the data fields required by the scenario.
     * @return ScenarioExecutionResult containing the results of all stage executions,
     *         warnings, review flags, and overall execution status
     * @throws IllegalStateException if the configuration does not contain a scenario
     * @throws NullPointerException if inputData is null
     * @since 2025-11-03
     */
    public ScenarioExecutionResult evaluateScenario(Map<String, Object> inputData) {
        return runWhileEngineActive("evaluateScenario", () -> scenarioEvaluationManager.evaluateScenario(inputData));
    }

    /**
     * Evaluate a specific scenario by ID from a scenario registry.
     *
     * <p>This method is used when the RulesEngine was created from a scenario
     * registry file containing multiple scenario definitions. It looks up the
     * scenario by ID and processes the input data through its stages.</p>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * RulesEngine engine = RulesEngine.fromScenarioRegistry("registry.yaml");
     * Map&lt;String, Object&gt; data = new HashMap&lt;&gt;();
     * data.put("tradeType", "OTCOption");
     *
     * ScenarioExecutionResult result = engine.evaluateScenario("basic-trade-processing", data);
     * </pre>
     *
     * @param scenarioId The unique identifier of the scenario to evaluate.
     *                   Must match a scenario-id defined in the registry.
     * @param inputData The input data to process through the scenario stages.
     *                  Must be a Map containing the data fields required by the scenario.
     * @return ScenarioExecutionResult containing the results of all stage executions,
     *         warnings, review flags, and overall execution status
     * @throws IllegalArgumentException if scenarioId is not found in the registry
     * @throws IllegalStateException if the configuration does not contain a scenario registry
     * @throws NullPointerException if scenarioId or inputData is null
     * @since 2025-11-03
     */
    public ScenarioExecutionResult evaluateScenario(String scenarioId, Map<String, Object> inputData) {
        return runWhileEngineActive("evaluateScenario", () -> scenarioEvaluationManager.evaluateScenario(scenarioId, inputData));
    }

    /**
     * Automatically select and evaluate the matching scenario based on classification rules.
     *
     * <p>This method evaluates the classification rules of all scenarios in the registry
     * and executes the first scenario whose classification rule matches the input data.
     * Classification rules are SpEL expressions that evaluate against the input data.</p>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * // Registry contains scenarios with classification rules like:
     * // "#'tradeType'] == 'OTCOption' && #'region'] == 'US'"
     *
     * RulesEngine engine = RulesEngine.fromScenarioRegistry("registry.yaml");
     * Map&lt;String, Object&gt; data = new HashMap&lt;&gt;();
     * data.put("tradeType", "OTCOption");
     * data.put("region", "US");
     *
     * ScenarioExecutionResult result = engine.evaluateWithClassification(data);
     * if (result.isSuccessful()) {
     *     System.out.println("Matched scenario: " + result.getScenarioId());
     * }
     * </pre>
     *
     * @param inputData The input data to classify and process.
     *                  Must be a Map containing the data fields used by classification rules.
     * @return ScenarioExecutionResult containing the results of the matched scenario execution.
     *         If no scenario matches, returns a result with status indicating no match found.
     * @throws IllegalStateException if the configuration does not contain a scenario registry
     * @throws NullPointerException if inputData is null
     * @since 2025-11-03
     */
    public ScenarioExecutionResult evaluateWithClassification(Map<String, Object> inputData) {
        return runWhileEngineActive("evaluateWithClassification",
            () -> scenarioEvaluationManager.evaluateWithClassification(inputData));
    }

    // ========================================
    // Fluent API Method (Style 2)
    // ========================================

    /**
     * Get a fluent API evaluator for type-safe scenario evaluation.
     *
     * <p>This method returns a {@link ScenarioEvaluator} interface that provides
     * a fluent, type-safe way to evaluate scenarios without requiring casting.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>
     * // Direct scenario evaluation
     * ScenarioExecutionResult result = RulesEngine.fromFile("scenario-config.yaml")
     *     .asScenario()
     *     .evaluate(data);
     *
     * // Registry-based with scenario ID
     * ScenarioExecutionResult result = RulesEngine.fromScenarioRegistry("registry.yaml")
     *     .asScenario()
     *     .evaluate("basic-trade-processing", data);
     *
     * // Classification-based routing
     * ScenarioExecutionResult result = RulesEngine.fromScenarioRegistry("registry.yaml")
     *     .asScenario()
     *     .evaluateWithClassification(data);
     * </pre>
     *
     * @return A ScenarioEvaluator instance for fluent scenario evaluation
     * @throws IllegalStateException if the configuration does not contain scenarios
     * @since 2025-11-03
     * @see ScenarioEvaluator
     */
    public ScenarioEvaluationManager.ScenarioEvaluator asScenario() {
        if (this.yamlConfig == null && this.scenarioRegistry == null) {
            throw new IllegalStateException(
                "Cannot use asScenario() method - this RulesEngine was not created with a scenario configuration or registry. " +
                "Use RulesEngine.fromFile() or RulesEngine.fromScenarioRegistry() to create the engine."
            );
        }

        return new LifecycleAwareScenarioEvaluator();
    }

    /**
     * Shutdown the RulesEngine and release all resources.
     * This method should be called when the engine is no longer needed to properly
     * clean up data sources, data sinks, and other resources.
     */
    public void shutdown() {
        logger.info("Shutting down RulesEngine");

        waitForInFlightEvaluationsToDrain();

        // Shutdown data sources
        for (Map.Entry<String, ExternalDataSource> entry : dataSources.entrySet()) {
            try {
                logger.debug("Shutting down data source: {}", entry.getKey());
                entry.getValue().shutdown();
            } catch (Exception e) {
                logger.warn("Error shutting down data source '{}': {}", entry.getKey(), e.getMessage());
                logger.debug("Full exception details:", e);
            }
        }

        // Shutdown data sinks
        for (Map.Entry<String, DataSink> entry : dataSinks.entrySet()) {
            try {
                logger.debug("Shutting down data sink: {}", entry.getKey());
                entry.getValue().shutdown();
            } catch (Exception e) {
                logger.warn("Error shutting down data sink '{}': {}", entry.getKey(), e.getMessage());
                logger.debug("Full exception details:", e);
            }
        }

        dataSources.clear();
        dataSinks.clear();

        // Shutdown runtime script system
        if (scriptReloadManager != null) {
            try {
                scriptReloadManager.stop();
            } catch (Exception e) {
                logger.warn("Error shutting down script reload manager: {}", e.getMessage());
            }
        }
        if (scriptExecutor != null) {
            try {
                scriptExecutor.shutdown();
            } catch (Exception e) {
                logger.warn("Error shutting down script executor: {}", e.getMessage());
            }
        }
        if (scriptCompiler != null) {
            try {
                scriptCompiler.close();
            } catch (Exception e) {
                logger.warn("Error shutting down script compiler: {}", e.getMessage());
            }
        }

        synchronized (lifecycleMonitor) {
            lifecycleState = LifecycleState.SHUT_DOWN;
            lifecycleMonitor.notifyAll();
        }

        logger.info("RulesEngine shutdown complete");
    }

    private <T> T runWhileEngineActive(String operationName, Callable<T> action) {
        beginEvaluation(operationName);
        if (scriptBridge != null) {
            scriptBridge.activate();
        }
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected checked exception during " + operationName, e);
        } finally {
            if (scriptBridge != null) {
                scriptBridge.deactivate();
            }
            endEvaluation();
        }
    }

    private void beginEvaluation(String operationName) {
        if (lifecycleState != LifecycleState.ACTIVE) {
            throw new IllegalStateException(
                "RulesEngine is shutting down or has already shut down; cannot start " + operationName);
        }

        activeEvaluations.incrementAndGet();

        if (lifecycleState != LifecycleState.ACTIVE) {
            notifyEvaluationCompleted(activeEvaluations.decrementAndGet());
            throw new IllegalStateException(
                "RulesEngine is shutting down or has already shut down; cannot start " + operationName);
        }
    }

    private void endEvaluation() {
        notifyEvaluationCompleted(activeEvaluations.decrementAndGet());
    }

    private void notifyEvaluationCompleted(int remainingEvaluations) {
        if (remainingEvaluations == 0 && lifecycleState != LifecycleState.ACTIVE) {
            synchronized (lifecycleMonitor) {
                lifecycleMonitor.notifyAll();
            }
        }
    }

    private void waitForInFlightEvaluationsToDrain() {
        boolean interrupted = false;

        synchronized (lifecycleMonitor) {
            while (lifecycleState == LifecycleState.SHUTTING_DOWN) {
                try {
                    lifecycleMonitor.wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }

            if (lifecycleState == LifecycleState.SHUT_DOWN) {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                return;
            }

            lifecycleState = LifecycleState.SHUTTING_DOWN;

            while (activeEvaluations.get() > 0) {
                try {
                    lifecycleMonitor.wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                    logger.warn("Interrupted while waiting for in-flight evaluations to complete during shutdown");
                }
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private class LifecycleAwareScenarioEvaluator implements ScenarioEvaluationManager.ScenarioEvaluator {
        @Override
        public ScenarioExecutionResult evaluate(Map<String, Object> inputData) {
            return RulesEngine.this.evaluateScenario(inputData);
        }

        @Override
        public ScenarioExecutionResult evaluate(String scenarioId, Map<String, Object> inputData) {
            return RulesEngine.this.evaluateScenario(scenarioId, inputData);
        }

        @Override
        public ScenarioExecutionResult evaluateWithClassification(Map<String, Object> inputData) {
            return RulesEngine.this.evaluateWithClassification(inputData);
        }
    }

    // ========================================
    // Internal Scenario Processing Methods
    // ========================================

    /**
     * Get a scenario by ID from the scenario registry.
     * Delegates to ScenarioRegistryManager.
     *
     * @param scenarioId The scenario ID to look up
     * @return The scenario configuration
     * @throws IllegalArgumentException if scenario not found or is disabled
     * @throws IllegalStateException if scenario registry is not initialized
     */
    private ScenarioConfiguration getScenario(String scenarioId) {
        return scenarioRegistryManager.getScenario(scenarioId);
    }

    /**
     * Find the first matching scenario based on classification rules.
     * Delegates to ScenarioRegistryManager.
     *
     * @param inputData The input data to match against classification rules
     * @return The first matching enabled scenario, or null if no match found
     */
    private ScenarioConfiguration findMatchingScenario(
            Map<String, Object> inputData) {
        return scenarioRegistryManager.findMatchingScenario(inputData);
    }







    /**
     * Load error recovery configuration from YAML if available, otherwise return defaults.
     *
     * @param yamlConfig The YAML configuration (can be null)
     * @return ErrorRecoveryConfig with settings from YAML or defaults
     */
    private ErrorRecoveryConfig loadErrorRecoveryConfig(YamlRuleConfiguration yamlConfig) {
        if (yamlConfig != null && yamlConfig.getErrorRecovery() != null) {
            logger.info("Loading error recovery configuration from YAML");
            ErrorRecoveryConfig config = yamlConfig.getErrorRecovery().toErrorRecoveryConfig();
            logger.debug("Error recovery config loaded: enabled={}, default-strategy={}",
                        config.isEnabled(), config.getDefaultStrategy());
            return config;
        } else {
            logger.debug("No error recovery configuration in YAML, using defaults");
            return new ErrorRecoveryConfig(); // Returns default configuration
        }
    }

    /**
     * Private implementation of ScenarioEvaluator interface for fluent API support.
     *
     * <p>This inner class provides a type-safe, fluent API for scenario evaluation
     * by delegating all operations to the parent RulesEngine instance. It eliminates
     * the need for casting when working with scenarios.</p>
     *
     * <p>Instances are created via {@link RulesEngine#asScenario()} and provide
     * three evaluation modes:</p>
     * <ul>
     *   <li>Direct evaluation - for single scenario configurations</li>
     *   <li>Registry-based evaluation - for specific scenarios by ID</li>
     *   <li>Classification-based evaluation - for automatic scenario selection</li>
     * </ul>
     *
     * @since 2026-01-08
     */
    private class ScenarioLookupStrategyImpl implements ScenarioEvaluationManager.ScenarioLookupStrategy {
        @Override
        public ScenarioConfiguration getScenario(String scenarioId) {
            return RulesEngine.this.getScenario(scenarioId);
        }

        @Override
        public ScenarioConfiguration findMatchingScenario(Map<String, Object> inputData) {
            return RulesEngine.this.findMatchingScenario(inputData);
        }
    }

    /**
     * Process a single rule chain by ID.
     * Delegates to RuleChainExecutor for actual execution.
     *
     * @param chainId The rule chain ID to process
     * @param yamlConfig The YAML configuration
     * @param data The data to evaluate
     * @return RuleResult from processing the rule chain
     */

    // ========================================================================
    // Builder Pattern for Fluent API Configuration
    // ========================================================================

    /**
     * Create a new Builder for fluent RulesEngine configuration.
     * 
     * <p>The Builder pattern provides a fluent API for configuring search paths
     * and building a RulesEngine from various sources.</p>
     * 
     * <p><b>Example Usage:</b></p>
     * <pre>{@code
     * // Basic usage with search paths
     * RulesEngine engine = RulesEngine.builder()
     *     .addSearchPath("/etc/apex/trading")
     *     .addSearchPath("/opt/apex/configs")
     *     .addClasspathPrefix("apex/trading/")
     *     .fromScenarioRegistry("scenario-registry.yaml")
     *     .build();
     * 
     * // With EvaluationContext for variable substitution
     * RulesEngine engine = RulesEngine.builder()
     *     .addSearchPath("${APEX_HOME}/configs")
     *     .withContext("environment", "production")
     *     .withContext("region", "us-east-1")
     *     .fromFile("trading-rules.yaml")
     *     .build();
     * }</pre>
     *
     * @return A new RulesEngineBuilder instance
     * @since 2026-01-08
     */
    public static RulesEngineBuilder builder() {
        return new RulesEngineBuilder();
    }
}

