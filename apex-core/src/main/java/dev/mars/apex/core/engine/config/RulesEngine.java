package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.config.datasink.DataSinkConfiguration;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.config.pipeline.PipelineConfiguration;
import dev.mars.apex.core.config.yaml.*;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.config.execution.EnrichmentGroupExecutor;
import dev.mars.apex.core.engine.config.execution.RuleChainExecutor;
import dev.mars.apex.core.engine.config.execution.RuleGroupExecutor;
import dev.mars.apex.core.engine.config.execution.SequentialProcessor;
import dev.mars.apex.core.engine.config.scenario.ScenarioParser;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.engine.model.EnrichmentGroupResult;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleBase;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleGroupEvaluationResult;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.pipeline.DataPipelineException;
import dev.mars.apex.core.engine.pipeline.PipelineExecutor;
import dev.mars.apex.core.engine.pipeline.YamlPipelineExecutionResult;
import dev.mars.apex.core.service.data.external.DataSink;
import dev.mars.apex.core.service.data.external.DataSinkException;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.factory.DataSinkFactory;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.data.external.manager.ExternalDataSourceManager;
import dev.mars.apex.core.service.enrichment.EnrichmentGroupFactory;
import dev.mars.apex.core.service.enrichment.YamlEnrichmentProcessor;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.core.service.engine.UnifiedRuleEvaluator;
import dev.mars.apex.core.service.transformation.YamlTransformationProcessor;
import dev.mars.apex.core.util.LoggingContext;
import dev.mars.apex.core.util.RulesEngineLogger;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import dev.mars.apex.core.service.scenario.ScenarioExecutionResult;
import dev.mars.apex.core.service.scenario.ScenarioStage;
import dev.mars.apex.core.service.scenario.ScenarioStageExecutor;
import dev.mars.apex.core.engine.config.util.DataCopyUtility;
import dev.mars.apex.core.engine.config.scenario.ScenarioParser;

import java.util.*;
import java.util.concurrent.*;

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
 * For production code, use {@link dev.mars.apex.core.config.yaml.RulesEngineService} to create
 * RulesEngine instances. This provides simplified, content-agnostic YAML processing that handles
 * all YAML content types (enrichments, rules, rule-groups, transformations, etc.) automatically.
 *
 * <p>Example:</p>
 * <pre>
 * // RECOMMENDED (production code):
 * RulesEngineService service = new RulesEngineService();
 * RulesEngine engine = service.createRulesEngineFromFile(yamlFile);
 *
 * // ACCEPTABLE (tests and simple cases):
 * RulesEngine engine = new RulesEngine(config, parser, errorService, monitor, enrichmentService);
 * </pre>
 */
public class RulesEngine {
    private static final RulesEngineLogger logger = new RulesEngineLogger(RulesEngine.class);
    private final ExpressionParser parser;
    private final ExpressionEvaluatorService evaluatorService;
    private final RulesEngineConfiguration configuration;
    private final ErrorRecoveryService errorRecoveryService;
    private final RulePerformanceMonitor performanceMonitor;
    private YamlEnrichmentProcessor enrichmentProcessor;  // Non-final to allow re-initialization with data sources
    private final UnifiedRuleEvaluator unifiedEvaluator;
    private final List<String> initializationErrors = new ArrayList<>();
    private final ScenarioParser scenarioParser;  // For parsing scenario configurations
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
     * Pipeline execution components (lazy-initialized when needed).
     */
    private final DataSourceFactory dataSourceFactory;
    private final DataSinkFactory dataSinkFactory;
    private final Map<String, ExternalDataSource> dataSources;
    private final Map<String, DataSink> dataSinks;
    private PipelineExecutor pipelineExecutor; // Lazy-initialized

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
    private RulesEngine(RulesEngineConfiguration configuration, YamlRuleConfiguration yamlConfig,
                       Map<String, ScenarioConfiguration> scenarioRegistry) {
        this.configuration = configuration;
        this.yamlConfig = yamlConfig;
        this.scenarioRegistry = scenarioRegistry;
        this.parser = new SpelExpressionParser();
        this.evaluatorService = new ExpressionEvaluatorService(this.parser);
        this.errorRecoveryService = new ErrorRecoveryService();
        this.performanceMonitor = new RulePerformanceMonitor();
        this.scenarioParser = new ScenarioParser();  // Initialize scenario parser
        // Note: enrichmentProcessor will be re-initialized after data sources are created
        // to ensure it has access to the data source registry
        this.enrichmentProcessor = new YamlEnrichmentProcessor(new LookupServiceRegistry(), this.evaluatorService);

        // Load error recovery configuration from YAML if available, otherwise use defaults
        ErrorRecoveryConfig errorRecoveryConfig = loadErrorRecoveryConfig(yamlConfig);

        // Initialize the unified evaluator with error recovery configuration from YAML
        this.unifiedEvaluator = new UnifiedRuleEvaluator(this.evaluatorService, errorRecoveryService, performanceMonitor, errorRecoveryConfig);

        // Initialize executors (after dependencies are initialized)
        this.enrichmentGroupExecutor = new EnrichmentGroupExecutor(this.enrichmentProcessor);
        this.ruleGroupExecutor = new RuleGroupExecutor(this.unifiedEvaluator);
        this.ruleChainExecutor = new RuleChainExecutor(this.parser, this.unifiedEvaluator, this.enrichmentGroupExecutor);
        this.sequentialProcessor = new SequentialProcessor(
            this.configuration,
            this.enrichmentProcessor,
            this.unifiedEvaluator,
            this.evaluatorService,
            this.enrichmentGroupExecutor,
            this.ruleGroupExecutor,
            this.ruleChainExecutor
        );

        // Initialize pipeline components
        this.dataSourceFactory = DataSourceFactory.getInstance();
        this.dataSinkFactory = DataSinkFactory.getInstance();
        this.dataSources = new HashMap<>();
        this.dataSinks = new HashMap<>();
        this.pipelineExecutor = null; // Lazy-initialized when needed

        // Initialize data sources and sinks if yamlConfig is provided
        if (yamlConfig != null) {
            initializePipelineComponents(yamlConfig);
        }

        // Initialize logging context
        LoggingContext.initializeContext();

        logger.configuration("RulesEngine", "Initialized with configuration: " + configuration.getClass().getSimpleName());
        logger.debug("Using parser: {}", parser.getClass().getSimpleName());
        logger.debug("Using error recovery service: {}", errorRecoveryService.getClass().getSimpleName());
        logger.debug("Using error recovery config: enabled={}, default-strategy={}",errorRecoveryConfig.isEnabled(), errorRecoveryConfig.getDefaultStrategy());
        logger.debug("Using performance monitor: {}", performanceMonitor.getClass().getSimpleName());
        logger.debug("Using enrichment processor: {}", enrichmentProcessor != null ? enrichmentProcessor.getClass().getSimpleName() : "none");
    }

    /**
     * Initialize pipeline components (data sources and sinks) from YAML configuration.
     * This method is called during construction if yamlConfig is provided.
     *
     * @param yamlConfig The YAML configuration containing data sources and sinks
     */
    private void initializePipelineComponents(YamlRuleConfiguration yamlConfig) {
        try {
            // Initialize data sources
            if (yamlConfig.getDataSources() != null && !yamlConfig.getDataSources().isEmpty()) {
                logger.info("Initializing {} data sources", yamlConfig.getDataSources().size());
                for (YamlDataSource yamlDataSource : yamlConfig.getDataSources()) {
                    try {
                        DataSourceConfiguration config = yamlDataSource.toDataSourceConfiguration();
                        ExternalDataSource dataSource = dataSourceFactory.createDataSource(config);
                        dataSources.put(config.getName(), dataSource);
                        logger.debug("Initialized data source: {}", config.getName());
                    } catch (DataSourceException e) {
                        logger.warn("Failed to initialize data source '{}': {}", yamlDataSource.getName(), e.getMessage());
                        initializationErrors.add("Failed to initialize data source '" + yamlDataSource.getName() + "': " + e.getMessage());
                    }
                }
                
                // Re-initialize enrichment processor with data source registry
                // This ensures enrichments can reuse existing data sources instead of creating duplicates
                logger.debug("Re-initializing enrichment processor with {} data sources from registry", dataSources.size());
                this.enrichmentProcessor = new YamlEnrichmentProcessor(
                    new LookupServiceRegistry(), 
                    this.evaluatorService,
                    this.dataSources
                );
            }

            // Initialize data sinks
            if (yamlConfig.getDataSinks() != null && !yamlConfig.getDataSinks().isEmpty()) {
                logger.info("Initializing {} data sinks", yamlConfig.getDataSinks().size());
                for (YamlDataSink yamlDataSink : yamlConfig.getDataSinks()) {
                    try {
                        DataSinkConfiguration config = yamlDataSink.toDataSinkConfiguration();
                        DataSink dataSink = dataSinkFactory.createDataSink(config);
                        dataSinks.put(config.getName(), dataSink);
                        logger.debug("Initialized data sink: {}", config.getName());
                    } catch (DataSinkException e) {
                        logger.warn("Failed to initialize data sink '{}': {}", yamlDataSink.getName(), e.getMessage());
                        initializationErrors.add("Failed to initialize data sink '" + yamlDataSink.getName() + "': " + e.getMessage());
                    }
                }
            }

            logger.info("Pipeline components initialized: {} data sources, {} data sinks",
                    dataSources.size(), dataSinks.size());

        } catch (Exception e) {
            logger.warn("Failed to initialize pipeline components: {}", e.getMessage());
            logger.debug("Pipeline initialization exception details:", e);
            initializationErrors.add("Failed to initialize pipeline components: " + e.getMessage());
        }
    }

    /**
     * Execute a pipeline configuration.
     *
     * @param pipeline The pipeline configuration to execute
     * @param inputData The input data for the pipeline
     * @return RuleResult indicating success or failure
     */
    private RuleResult executePipeline(PipelineConfiguration pipeline, Map<String, Object> inputData) {
        try {
            logger.info("Executing pipeline: {}", pipeline.getName());

            // Lazy-initialize pipeline executor
            if (pipelineExecutor == null) {
                pipelineExecutor = new PipelineExecutor(new DataSourceManagerAdapter());

                // Add all data sinks to executor
                for (Map.Entry<String, DataSink> entry : dataSinks.entrySet()) {
                    pipelineExecutor.addDataSink(entry.getKey(), entry.getValue());
                }
            }

            // Execute pipeline
            YamlPipelineExecutionResult result = pipelineExecutor.execute(pipeline);

            // Convert pipeline steps to ExecutionSteps for tracing
            List<ExecutionStep> pipelineSteps = new ArrayList<>();
            if (result.getStepResults() != null) {
                for (dev.mars.apex.core.engine.pipeline.PipelineStepResult stepResult : result.getStepResults()) {
                    String status = stepResult.isSuccess() ? "SUCCESS" : (stepResult.isSkipped() ? "SKIPPED" : "FAILURE");
                    String message = stepResult.getError() != null ? stepResult.getError() :
                                   (stepResult.isSkipped() ? "Step skipped" : "Step completed successfully");

                    // Use new constructor that captures step data and metrics
                    pipelineSteps.add(new ExecutionStep(
                        stepResult.getStepName(),
                        "PIPELINE_STEP",
                        status,
                        message,
                        stepResult.getDurationMs(),
                        stepResult.getData(),              // Capture step data
                        stepResult.getRecordsProcessed(),  // Capture metrics
                        stepResult.getRecordsFailed()      // Capture metrics
                    ));
                }
            }

            // Convert to RuleResult
            RuleResult ruleResult;
            if (result.isSuccess()) {
                logger.info("Pipeline '{}' executed successfully in {}ms",
                        pipeline.getName(), result.getDurationMs());
                ruleResult = RuleResult.match("pipeline:" + pipeline.getName(),
                        "Pipeline executed successfully", SeverityConstants.INFO);
            } else {
                logger.error("Pipeline '{}' execution failed: {}", pipeline.getName(), result.getError());
                ruleResult = RuleResult.error("pipeline:" + pipeline.getName(),
                        "Pipeline execution failed: " + result.getError());
            }
            
            // Attach the execution path
            ruleResult.setExecutionPath(pipelineSteps);
            return ruleResult;
            
        } catch (DataPipelineException e) {
            logger.error("Pipeline execution failed with exception", e);
            return RuleResult.error("pipeline:" + pipeline.getName(),
                    "Pipeline execution failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during pipeline execution", e);
            return RuleResult.error("pipeline:unknown",
                    "Pipeline execution failed: " + e.getMessage());
        }
    }

    /**
     * Adapter to provide ExternalDataSourceManager interface to PipelineExecutor.
     */
    private class DataSourceManagerAdapter implements ExternalDataSourceManager {
        @Override
        public ExternalDataSource getDataSource(String name) {
            return dataSources.get(name);
        }

        @Override
        public void addDataSource(String name, ExternalDataSource dataSource) {
            dataSources.put(name, dataSource);
        }

        @Override
        public void removeDataSource(String name) {
            dataSources.remove(name);
        }

        @Override
        public boolean hasDataSource(String name) {
            return dataSources.containsKey(name);
        }
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
     * @throws YamlConfigurationException if the file cannot be loaded or parsed
     */
    public static RulesEngine fromFile(String filePath) throws YamlConfigurationException {
        logger.info("Creating RulesEngine from file: {}", filePath);

        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration yamlConfig = loader.loadFromFile(filePath);

        YamlRuleFactory ruleFactory = new YamlRuleFactory();
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
     * @throws YamlConfigurationException if the resource cannot be found or loaded
     * @since 2.1.0
     */
    public static RulesEngine fromClasspath(String resourcePath) throws YamlConfigurationException {
        logger.info("Creating RulesEngine from classpath resource: {}", resourcePath);

        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration yamlConfig = loader.loadFromClasspath(resourcePath);

        YamlRuleFactory ruleFactory = new YamlRuleFactory();
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
     * YamlConfigurationLoader loader = new YamlConfigurationLoader();
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
     * @throws YamlConfigurationException if the configuration cannot be processed
     */
    public static RulesEngine fromYamlConfig(YamlRuleConfiguration yamlConfig) throws YamlConfigurationException {
        logger.info("Creating RulesEngine from YamlRuleConfiguration");

        YamlRuleFactory ruleFactory = new YamlRuleFactory();
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
        
        try {
            // Step 1: Parse and validate YAML
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlString);
            
            // Step 2: Create engine
            RulesEngine engine = fromYamlConfig(yamlConfig);
            
            // Step 3: Evaluate (this method already handles runtime errors)
            return engine.evaluate(inputData);
            
        } catch (YamlConfigurationException e) {
            // YAML parsing or validation error - return as RuleResult
            logger.error("YAML configuration error: {}", e.getMessage());
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("YAML configuration error: " + e.getMessage());
            if (e.getCause() != null) {
                failureMessages.add("Caused by: " + e.getCause().getMessage());
            }
            Map<String, Object> data = inputData != null ? new HashMap<>(inputData) : new HashMap<>();
            return RuleResult.evaluationFailure(failureMessages, data, "yaml-configuration", "YAML configuration error");
            
        } catch (Exception e) {
            // Any other unexpected error - return as RuleResult
            logger.error("Unexpected error during YAML evaluation: {}", e.getMessage(), e);
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("Unexpected error: " + e.getMessage());
            Map<String, Object> data = inputData != null ? new HashMap<>(inputData) : new HashMap<>();
            return RuleResult.evaluationFailure(failureMessages, data, "unexpected-error", "Unexpected error during evaluation");
        }
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
        
        try {
            // Step 1: Parse and validate YAML file
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration yamlConfig = loader.loadFromFile(yamlFilePath);
            
            // Step 2: Create engine
            RulesEngine engine = fromYamlConfig(yamlConfig);
            
            // Step 3: Evaluate
            return engine.evaluate(inputData);
            
        } catch (YamlConfigurationException e) {
            logger.error("YAML configuration error: {}", e.getMessage());
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("YAML configuration error: " + e.getMessage());
            if (e.getCause() != null) {
                failureMessages.add("Caused by: " + e.getCause().getMessage());
            }
            Map<String, Object> data = inputData != null ? new HashMap<>(inputData) : new HashMap<>();
            return RuleResult.evaluationFailure(failureMessages, data, "yaml-configuration", "YAML configuration error");
            
        } catch (Exception e) {
            logger.error("Unexpected error during YAML file evaluation: {}", e.getMessage(), e);
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("Unexpected error: " + e.getMessage());
            Map<String, Object> data = inputData != null ? new HashMap<>(inputData) : new HashMap<>();
            return RuleResult.evaluationFailure(failureMessages, data, "unexpected-error", "Unexpected error during evaluation");
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
     * @throws YamlConfigurationException if the registry file cannot be loaded or parsed
     * @since 3.0
     * @see #evaluateScenario(String, Map)
     * @see #evaluateWithClassification(Map)
     * @see #asScenario()
     */
    public static RulesEngine fromScenarioRegistry(String registryPath) throws YamlConfigurationException {
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
            throw new YamlConfigurationException("Failed to load scenario registry: " + registryPath, e);
        }

        if (scenarios == null || scenarios.isEmpty()) {
            throw new YamlConfigurationException(
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
     * @return The scenario registry map, or null if not a scenario-based engine
     * @since 3.0
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
        // Delegate to the unified evaluator for consistent behavior
        RuleResult result = unifiedEvaluator.evaluateRule(rule, facts);

        // Phase 5: Store result in facts if result-field is configured
        if (rule.getResultField() != null && !rule.getResultField().trim().isEmpty()) {
            facts.put(rule.getResultField(), result.isTriggered());
            logger.debug("Stored rule result in facts: {} = {}", rule.getResultField(), result.isTriggered());
        }

        return result;
    }

    /**
     * Execute a list of Rule objects against the provided facts.
     *
     * @param rules The list of Rule objects to execute
     * @param facts The facts to evaluate the rules against
     * @return The result of the first rule that matches, or a default result if no rules match
     */
    public RuleResult executeRulesList(List<Rule> rules, Map<String, Object> facts) {
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
        StandardEvaluationContext context = createContext(facts);
        return ruleGroupExecutor.executeRuleGroupsList(ruleGroups, facts, context);
    }

    /**
     * Execute a list of enrichment groups with proper result aggregation.
     * Delegates to EnrichmentGroupExecutor for actual execution.
     *
     * @param enrichmentGroups The list of enrichment groups to execute
     * @param targetObject The target object to enrich
     * @param yamlConfig The YAML configuration (required for database lookups)
     * @return The result of enrichment group execution
     */
    private RuleResult executeEnrichmentGroupsList(List<EnrichmentGroup> enrichmentGroups, Object targetObject, YamlRuleConfiguration yamlConfig) {
        return enrichmentGroupExecutor.executeEnrichmentGroupsList(enrichmentGroups, targetObject, yamlConfig);
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
        logger.info("Executing rules for category: {}", category);
        List<RuleBase> rules = configuration.getRulesForCategory(category);
        logger.debug("Found {} rules/rule groups in category: {}", rules.size(), category);
        return executeRules(rules, facts);
    }

    /**
     * Simple evaluation method that returns only a boolean indicating whether a rule was triggered.
     * This method is provided for simplicity when only the boolean result is needed.
     *
     * @param rule The Rule object to evaluate
     * @param facts The facts to evaluate the rule against
     * @return true if the rule was triggered, false otherwise
     */
    public boolean evaluateRule(Rule rule, Map<String, Object> facts) {
        RuleResult result = executeRule(rule, facts);
        return result.isTriggered();
    }

    /**
     * Simple evaluation method that returns only a boolean indicating whether any rule in the list was triggered.
     * This method is provided for simplicity when only the boolean result is needed.
     *
     * @param rules The list of Rule objects to evaluate
     * @param facts The facts to evaluate the rules against
     * @return true if any rule was triggered, false otherwise
     */
    public boolean evaluateRules(List<RuleBase> rules, Map<String, Object> facts) {
        RuleResult result = executeRules(rules, facts);
        return result.isTriggered();
    }

    /**
     * Simple evaluation method that returns only a boolean indicating whether any rule in the specified category was triggered.
     * This method is provided for simplicity when only the boolean result is needed.
     *
     * @param category The category of rules to evaluate
     * @param facts The facts to evaluate the rules against
     * @return true if any rule was triggered, false otherwise
     */
    public boolean evaluateRulesForCategory(String category, Map<String, Object> facts) {
        RuleResult result = executeRulesForCategory(category, facts);
        return result.isTriggered();
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
        logger.info("Starting unified evaluation with enrichments and rules");

        // Check for initialization errors first
        if (!initializationErrors.isEmpty()) {
            logger.error("Engine has initialization errors: {}", initializationErrors);
            Map<String, Object> data = inputData != null ? new HashMap<>(inputData) : new HashMap<>();
            return RuleResult.evaluationFailure(initializationErrors, data, "initialization", "Engine initialization failed");
        }

        // Handle null inputs gracefully
        if (yamlConfig == null) {
            logger.warn("YAML configuration is null");
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("YAML configuration is null");
            Map<String, Object> enrichedData = inputData != null ? new HashMap<>(inputData) : new HashMap<>();
            return RuleResult.evaluationFailure(failureMessages, enrichedData, "evaluation", "Null YAML configuration");
        }

        if (inputData == null) {
            logger.warn("Input data is null");
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("Input data is null");
            return RuleResult.evaluationFailure(failureMessages, new HashMap<>(), "evaluation", "Null input data");
        }

        // Determine execution order
        List<String> sectionOrder = yamlConfig.getSectionOrder();
        
        if (sectionOrder == null || sectionOrder.isEmpty()) {
            // Fallback to standard legacy order if no section order is defined
            logger.info("No section order defined - using default standard order");
            sectionOrder = Arrays.asList("rules", "rule-groups", "enrichments", "enrichment-groups", "transformations", "pipeline");
        }

        logger.info("Executing sections in order: {}", sectionOrder);
        return evaluateSequential(yamlConfig, inputData, sectionOrder);
    }

    /**
     * Evaluate using sequential processing - execute sections in YAML document order.
     * This respects the developer's intent as expressed through YAML structure.
     *
     * <p>Delegates to SequentialProcessor which handles both item-level and section-level processing.</p>
     *
     * @param yamlConfig The YAML configuration to evaluate
     * @param inputData The input data to process
     * @param sectionOrder The order of sections from the YAML document (used for fallback)
     * @return RuleResult containing execution results, enriched data, and execution path
     */
    private RuleResult evaluateSequential(YamlRuleConfiguration yamlConfig, Map<String, Object> inputData, List<String> sectionOrder) {
        // Delegate to SequentialProcessor with method references for executeRule, executePipeline, and createContext
        return sequentialProcessor.evaluateSequential(
            yamlConfig,
            inputData,
            sectionOrder,
            ctx -> executeRule(ctx.getRule(), ctx.getData()),
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
        if (this.yamlConfig == null) {
            throw new IllegalStateException(
                "Cannot use simplified evaluate(Map) method - this RulesEngine was not created with a YAML configuration. " +
                "Either use RulesEngine.fromFile() or RulesEngine.fromYamlConfig() to create the engine, " +
                "or use the explicit evaluate(YamlRuleConfiguration, Map) method instead."
            );
        }

        return evaluate(this.yamlConfig, inputData);
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
     * @since 3.0
     */
    public ScenarioExecutionResult evaluateScenario(Map<String, Object> inputData) {
        if (inputData == null) {
            throw new NullPointerException("Input data cannot be null");
        }

        if (this.yamlConfig == null) {
            throw new IllegalStateException(
                "Cannot use evaluateScenario(Map) method - this RulesEngine was not created with a YAML configuration. " +
                "Use RulesEngine.fromFile() or RulesEngine.fromYamlConfig() to create the engine."
            );
        }

        // Check if YAML configuration contains a scenario
        if (!this.yamlConfig.hasScenario()) {
            throw new IllegalStateException(
                "YAML configuration does not contain a scenario section. " +
                "Use a scenario configuration file or RulesEngine.fromScenarioRegistry() for scenario evaluation."
           
            );
        }

        logger.info("Evaluating scenario from YAML configuration");

        // Parse scenario configuration from YAML using ScenarioParser
        ScenarioConfiguration scenario = scenarioParser.parseFromYaml(this.yamlConfig);

        // Create ScenarioStageExecutor and execute stages
        ScenarioStageExecutor executor = new ScenarioStageExecutor();

        // Deep copy input data to protect against callers sharing the same map across concurrent calls
        Map<String, Object> safeInputData = DataCopyUtility.deepCopyMap(inputData);

        return executor.executeStages(scenario, safeInputData);
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
     * @since 3.0
     */
    public ScenarioExecutionResult evaluateScenario(String scenarioId, Map<String, Object> inputData) {
        if (scenarioId == null) {
            throw new NullPointerException("Scenario ID cannot be null");
        }
        if (inputData == null) {
            throw new NullPointerException("Input data cannot be null");
        }

        if (this.scenarioRegistry == null) {
            throw new IllegalStateException(
                "Cannot use evaluateScenario(String, Map) method - this RulesEngine was not created with a scenario registry. " +
                "Use RulesEngine.fromScenarioRegistry() to create the engine."
            );
        }

        logger.info("Evaluating scenario by ID: {}", scenarioId);

        // Look up scenario from registry
        ScenarioConfiguration scenario = getScenario(scenarioId);

        // Create ScenarioStageExecutor and execute stages
        ScenarioStageExecutor executor = new ScenarioStageExecutor();

        // Deep copy input data to protect against callers sharing the same map across concurrent calls
        Map<String, Object> safeInputData = DataCopyUtility.deepCopyMap(inputData);

        return executor.executeStages(scenario, safeInputData);
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
     * @since 3.0
     */
    public ScenarioExecutionResult evaluateWithClassification(Map<String, Object> inputData) {
        if (inputData == null) {
            throw new NullPointerException("Input data cannot be null");
        }

        if (this.scenarioRegistry == null) {
            throw new IllegalStateException(
                "Cannot use evaluateWithClassification(Map) method - this RulesEngine was not created with a scenario registry. " +
                "Use RulesEngine.fromScenarioRegistry() to create the engine."
            );
        }

        logger.info("Evaluating scenario using classification-based routing");

        // Find matching scenario based on classification rules
        ScenarioConfiguration scenario = findMatchingScenario(inputData);

        if (scenario == null) {
            throw new IllegalStateException(
                "No matching scenario found for the provided input data. " +
                "Ensure that at least one scenario's classification rule matches the data."
            );
        }

        logger.info("Matched scenario: {}", scenario.getScenarioId());

        // Create ScenarioStageExecutor and execute stages
        ScenarioStageExecutor executor = new ScenarioStageExecutor();

        // Deep copy input data to protect against callers sharing the same map across concurrent calls
        Map<String, Object> safeInputData = DataCopyUtility.deepCopyMap(inputData);

        return executor.executeStages(scenario, safeInputData);
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
     * @since 3.0
     * @see ScenarioEvaluator
     */
    public ScenarioEvaluator asScenario() {
        if (this.yamlConfig == null && this.scenarioRegistry == null) {
            throw new IllegalStateException(
                "Cannot use asScenario() method - this RulesEngine was not created with a scenario configuration or registry. " +
                "Use RulesEngine.fromFile() or RulesEngine.fromScenarioRegistry() to create the engine."
            );
        }

        return new ScenarioEvaluatorImpl(this);
    }

    /**
     * Get the priority value for a severity level.
     * Higher values indicate higher severity.
     *
     * @param severity The severity level (ERROR, WARNING, INFO)

     * @return The priority value (3 for ERROR, 2 for WARNING, 1 for INFO)
     */
    private int getSeverityPriority(String severity) {
        return SeverityConstants.getSeverityPriority(severity);
    }

    /**
     * Convert an object to a Map.
     * If the object is already a Map, return a shallow copy.
     * Otherwise, wrap it in a Map with key "data".
     * 
     * <p>Note: For parallel enrichment processing, use {@link #deepCopyMap(Map)} instead
     * to ensure nested structures are fully isolated between threads.</p>
     *
     * @param object The object to convert
     * @return A Map representation of the object
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object object) {
        if (object instanceof Map) {
            return new HashMap<>((Map<String, Object>) object);
        } else {
            // For non-Map objects, create a simple wrapper
            Map<String, Object> result = new HashMap<>();
            result.put("data", object);
            return result;
        }
    }

    /**
     * Creates a deep copy of a Map, recursively copying all nested Maps and Lists.
     * This ensures complete isolation for parallel processing where enrichments
     * may mutate nested structures.
     * 
     * <p>Handles the following types:</p>
     * <ul>
     *   <li>Map - recursively deep copied</li>
     *   <li>List - recursively deep copied (elements that are Maps/Lists are also copied)</li>
     *   <li>Primitive wrappers, Strings, etc. - referenced directly (immutable)</li>
     * </ul>
     *
     * @param original The original map to copy
     * @return A deep copy of the map with all nested structures copied
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopyMap(Map<String, Object> original) {
        if (original == null) {
            return null;
        }
        
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }
        return copy;
    }

    /**
     * Deep copies a single value, handling Maps, Lists, and other types.
     *
     * @param value The value to copy
     * @return A deep copy of the value if it's a Map or List, otherwise the original value
     */
    @SuppressWarnings("unchecked")
    private Object deepCopyValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Map) {
            // Recursively copy nested maps
            Map<String, Object> mapValue = (Map<String, Object>) value;
            return deepCopyMap(mapValue);
        }
        
        if (value instanceof List) {
            // Recursively copy list elements
            List<Object> listValue = (List<Object>) value;
            List<Object> listCopy = new ArrayList<>(listValue.size());
            for (Object item : listValue) {
                listCopy.add(deepCopyValue(item));
            }
            return listCopy;
        }
        
        // For immutable types (String, Number, Boolean, etc.), return as-is
        // For other mutable types, we rely on the fact that enrichments typically
        // don't modify arbitrary objects - they work with Maps and primitives
        return value;
    }

    /**
     * Shutdown the RulesEngine and release all resources.
     * This method should be called when the engine is no longer needed to properly
     * clean up data sources, data sinks, and other resources.
     */
    public void shutdown() {
        logger.info("Shutting down RulesEngine");

        // Shutdown data sources
        for (Map.Entry<String, ExternalDataSource> entry : dataSources.entrySet()) {
            try {
                logger.debug("Shutting down data source: {}", entry.getKey());
                entry.getValue().shutdown();
            } catch (Exception e) {
                logger.warn("Error shutting down data source '{}': {}", entry.getKey(), e.getMessage());
            }
        }

        // Shutdown data sinks
        for (Map.Entry<String, DataSink> entry : dataSinks.entrySet()) {
            try {
                logger.debug("Shutting down data sink: {}", entry.getKey());
                entry.getValue().shutdown();
            } catch (Exception e) {
                logger.warn("Error shutting down data sink '{}': {}", entry.getKey(), e.getMessage());
            }
        }

        dataSources.clear();
        dataSinks.clear();

        logger.info("RulesEngine shutdown complete");
    }

    // ========================================
    // Internal Scenario Processing Methods
    // ========================================

    /**
     * Get a scenario by ID from the scenario registry.
     *
     * <p>This method retrieves a scenario by its ID and validates that it is enabled.
     * Disabled scenarios cannot be executed directly by ID.</p>
     *
     * @param scenarioId The scenario ID to look up
     * @return The scenario configuration
     * @throws IllegalArgumentException if scenario not found or is disabled
     * @throws IllegalStateException if scenario registry is not initialized
     */
    private ScenarioConfiguration getScenario(String scenarioId) {
        if (this.scenarioRegistry == null) {
            throw new IllegalStateException("Scenario registry is not initialized");
        }

        ScenarioConfiguration scenario = this.scenarioRegistry.get(scenarioId);

        if (scenario == null) {
            throw new IllegalArgumentException(
                "Scenario not found: " + scenarioId + ". " +
                "Available scenarios: " + this.scenarioRegistry.keySet()
            );
        }

        // Check if scenario is enabled
        if (!scenario.isEnabled()) {
            throw new IllegalArgumentException(
                "Scenario '" + scenarioId + "' is disabled and cannot be executed. " +
                "Enable the scenario in the registry or use a different scenario."
            );
        }

        return scenario;
    }

    /**
     * Find the first matching scenario based on classification rules.
     * Iterates through all scenarios in the registry and evaluates their classification rules
     * against the provided input data using SpEL expressions.
     *
     * <p>Only enabled scenarios are considered for matching. Disabled scenarios are skipped
     * during classification-based routing.</p>
     *
     * @param inputData The input data to match against classification rules
     * @return The first matching enabled scenario, or null if no match found
     */
    private ScenarioConfiguration findMatchingScenario(
            Map<String, Object> inputData) {

        if (this.scenarioRegistry == null || this.scenarioRegistry.isEmpty()) {
            logger.warn("Scenario registry is empty - no scenarios to match");
            return null;
        }

        logger.debug("Evaluating {} scenarios for classification match", this.scenarioRegistry.size());

        for (ScenarioConfiguration scenario : this.scenarioRegistry.values()) {
            // Skip disabled scenarios
            if (!scenario.isEnabled()) {
                logger.debug("Scenario {} is disabled - skipping", scenario.getScenarioId());
                continue;
            }

            if (scenario.hasClassificationRule()) {
                logger.debug("Evaluating classification rule for scenario: {}", scenario.getScenarioId());

                if (scenario.matchesClassificationRule(inputData, this.evaluatorService)) {
                    logger.info("Found matching scenario: {} ({})",
                        scenario.getScenarioId(), scenario.getClassificationRuleDescription());
                    return scenario;
                }
            } else {
                logger.debug("Scenario {} has no classification rule - skipping", scenario.getScenarioId());
            }
        }

        logger.warn("No matching scenario found for input data");
        return null;
    }

    /**
     * Parse scenario configuration from YamlRuleConfiguration.
     *
     * @param yamlConfig The YAML configuration containing scenario data
     * @return Parsed ScenarioConfiguration
     * @throws IllegalStateException if scenario data is missing or invalid
     */
    @SuppressWarnings("unchecked")
    private ScenarioConfiguration parseScenarioFromYaml(
            dev.mars.apex.core.config.yaml.YamlRuleConfiguration yamlConfig) {

        if (!yamlConfig.hasScenario()) {
            throw new IllegalStateException("YAML configuration does not contain a scenario section");
        }

        Object scenarioData = yamlConfig.getScenarioData();
        if (!(scenarioData instanceof Map)) {
            throw new IllegalStateException("Scenario data must be a Map");
        }

        Map<String, Object> scenarioMap = (Map<String, Object>) scenarioData;
        return parseScenarioConfiguration(scenarioMap);
    }

    /**
     * Parse scenario configuration from YAML data map.
     * Follows the same pattern as DataTypeScenarioService.parseScenarioConfiguration.
     *
     * @param scenarioData The scenario data map from YAML
     * @return Parsed ScenarioConfiguration
     */
    @SuppressWarnings("unchecked")
    private ScenarioConfiguration parseScenarioConfiguration(
            Map<String, Object> scenarioData) {

        ScenarioConfiguration scenario = new ScenarioConfiguration();

        scenario.setScenarioId((String) scenarioData.get("scenario-id"));
        scenario.setName((String) scenarioData.get("name"));
        scenario.setDescription((String) scenarioData.get("description"));

        // Parse data types (legacy)
        List<String> dataTypes = (List<String>) scenarioData.get("data-types");
        if (dataTypes != null) {
            scenario.setDataTypes(dataTypes);
        }

        // Parse classification rule (modern Map-based routing)
        Map<String, Object> classificationRule =
            (Map<String, Object>) scenarioData.get("classification-rule");
        if (classificationRule != null) {
            String condition = (String) classificationRule.get("condition");
            String description = (String) classificationRule.get("description");

            if (condition != null) {
                scenario.setClassificationRuleCondition(condition);
            }
            if (description != null) {
                scenario.setClassificationRuleDescription(description);
            }
        }

        // Parse rule configurations (legacy)
        List<String> ruleConfigurations =
            (List<String>) scenarioData.get("rule-configurations");
        if (ruleConfigurations != null) {
            scenario.setRuleConfigurations(ruleConfigurations);
        }

        // Parse processing stages (modern stage-based configuration)
        List<Map<String, Object>> processingStages =
            (List<Map<String, Object>>) scenarioData.get("processing-stages");
        if (processingStages != null) {
            List<ScenarioStage> stages = new ArrayList<>();
            for (Map<String, Object> stageData : processingStages) {
                ScenarioStage stage = parseScenarioStage(stageData);
                if (stage != null) {
                    stages.add(stage);
                }
            }

            // Preserve classification rule fields when creating stage-based scenario
            String classificationCondition = scenario.getClassificationRuleCondition();
            String classificationDescription = scenario.getClassificationRuleDescription();
            String description = scenario.getDescription();

            scenario = ScenarioConfiguration.withStages(
                scenario.getScenarioId(), scenario.getName(), scenario.getDataTypes(), stages);
            scenario.setDescription(description);
            scenario.setClassificationRuleCondition(classificationCondition);
            scenario.setClassificationRuleDescription(classificationDescription);
        }

        return scenario;
    }

    /**
     * Parse a scenario stage from YAML data.
     * Follows the same pattern as DataTypeScenarioService.parseScenarioStage.
     *
     * @param stageData The stage data map from YAML
     * @return Parsed ScenarioStage or null if parsing fails
     */
    @SuppressWarnings("unchecked")
    private ScenarioStage parseScenarioStage(
            Map<String, Object> stageData) {

        try {
            String stageName = (String) stageData.get("stage-name");
            String configFile = (String) stageData.get("config-file");
            Integer executionOrder = (Integer) stageData.get("execution-order");
            String failurePolicy = (String) stageData.get("failure-policy");
            String condition = (String) stageData.get("condition");
            Boolean required = (Boolean) stageData.get("required");

            if (stageName == null || configFile == null || executionOrder == null) {
                logger.warn("Missing required stage fields: stage-name, config-file, or execution-order");
                return null;
            }

            ScenarioStage stage =
                new ScenarioStage(stageName, configFile, executionOrder);

            if (failurePolicy != null) {
                stage.setFailurePolicy(failurePolicy);
            }

            if (condition != null) {
                stage.setCondition(condition);
            }

            if (required != null) {
                stage.setRequired(required);
            }

            // Parse dependencies
            List<String> dependsOn = (List<String>) stageData.get("depends-on");
            if (dependsOn != null) {
                for (String dependency : dependsOn) {
                    stage.addDependency(dependency);
                }
            }

            // Parse stage metadata
            Map<String, Object> stageMetadata =
                (Map<String, Object>) stageData.get("stage-metadata");
            if (stageMetadata != null) {
                stage.setStageMetadata(stageMetadata);
            }

            return stage;

        } catch (Exception e) {
            logger.error("Error parsing scenario stage: {}", e.getMessage());
            return null;
        }
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
     * @since 3.0
     */
    private static class ScenarioEvaluatorImpl implements ScenarioEvaluator {
        private final RulesEngine engine;

        /**
         * Create a new ScenarioEvaluatorImpl wrapping the given RulesEngine.
         *
         * @param engine The RulesEngine instance to delegate to
         */
        ScenarioEvaluatorImpl(RulesEngine engine) {
            this.engine = engine;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ScenarioExecutionResult evaluate(Map<String, Object> inputData) {
            return engine.evaluateScenario(inputData);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ScenarioExecutionResult evaluate(String scenarioId, Map<String, Object> inputData) {
            return engine.evaluateScenario(scenarioId, inputData);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ScenarioExecutionResult evaluateWithClassification(Map<String, Object> inputData) {
            return engine.evaluateWithClassification(inputData);
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
    private RuleResult processRuleChainItem(String chainId, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
        return ruleChainExecutor.processRuleChain(chainId, yamlConfig, data, this::createContext);
    }

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
     * @return A new Builder instance
     * @since 3.0
     */
    public static Builder builder() {
        return new Builder();
    }

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
     * @since 3.0
     */
    public static class Builder {
        private static final RulesEngineLogger builderLogger = new RulesEngineLogger(Builder.class);
        
        private final List<String> filesystemPaths = new ArrayList<>();
        private final List<String> classpathPrefixes = new ArrayList<>();
        private final Map<String, Object> contextVariables = new HashMap<>();
        private String sourcePath;
        private SourceType sourceType;

        private enum SourceType {
            FILE, SCENARIO_REGISTRY, YAML_CONFIG
        }

        /**
         * Create a new Builder instance.
         * Typically accessed via {@link RulesEngine#builder()}.
         */
        public Builder() {
            builderLogger.debug("Created new RulesEngine.Builder");
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
        public Builder addSearchPath(String path) {
            if (path != null && !path.trim().isEmpty()) {
                String expanded = expandEnvironmentVariables(path.trim());
                filesystemPaths.add(expanded);
                builderLogger.debug("Added filesystem search path: {}", expanded);
            }
            return this;
        }

        /**
         * Add multiple filesystem search paths.
         *
         * @param paths The filesystem paths to add
         * @return this builder for method chaining
         */
        public Builder addSearchPaths(String... paths) {
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
        public Builder addSearchPaths(Collection<String> paths) {
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
        public Builder addClasspathPrefix(String prefix) {
            if (prefix != null && !prefix.trim().isEmpty()) {
                String normalized = prefix.trim();
                // Ensure prefix ends with /
                if (!normalized.endsWith("/")) {
                    normalized = normalized + "/";
                }
                classpathPrefixes.add(normalized);
                builderLogger.debug("Added classpath prefix: {}", normalized);
            }
            return this;
        }

        /**
         * Add multiple classpath prefixes.
         *
         * @param prefixes The classpath prefixes to add
         * @return this builder for method chaining
         */
        public Builder addClasspathPrefixes(String... prefixes) {
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
        public Builder withContext(String name, Object value) {
            if (name != null && !name.trim().isEmpty()) {
                contextVariables.put(name.trim(), value);
                builderLogger.debug("Added context variable: {} = {}", name, value);
            }
            return this;
        }

        /**
         * Add multiple context variables from a map.
         *
         * @param context The context variables map
         * @return this builder for method chaining
         */
        public Builder withContext(Map<String, Object> context) {
            if (context != null) {
                contextVariables.putAll(context);
                builderLogger.debug("Added {} context variables", context.size());
            }
            return this;
        }

        /**
         * Configure the builder to load from a scenario registry.
         *
         * @param registryPath The path to the scenario registry YAML file
         * @return this builder for method chaining
         */
        public Builder fromScenarioRegistry(String registryPath) {
            this.sourcePath = registryPath;
            this.sourceType = SourceType.SCENARIO_REGISTRY;
            builderLogger.debug("Set source: scenario registry at {}", registryPath);
            return this;
        }

        /**
         * Configure the builder to load from a YAML configuration file.
         *
         * @param filePath The path to the YAML configuration file
         * @return this builder for method chaining
         */
        public Builder fromFile(String filePath) {
            this.sourcePath = filePath;
            this.sourceType = SourceType.FILE;
            builderLogger.debug("Set source: file at {}", filePath);
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

            builderLogger.info("Building RulesEngine from {} at {}", sourceType, sourcePath);

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
            builderLogger.debug("Creating ScenarioRegistryLoader with {} filesystem paths, {} classpath prefixes",
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
                    builderLogger.info("Loaded {} scenarios from classpath registry: {}", scenarios.size(), sourcePath);
                } else {
                    // Fallback to filesystem
                    scenarios = loader.loadRegistry(sourcePath);
                    builderLogger.info("Loaded {} scenarios from filesystem registry: {}", scenarios.size(), sourcePath);
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
         * Build RulesEngine from YAML file with configured search paths.
         */
        private RulesEngine buildFromFile() throws YamlConfigurationException {
            builderLogger.debug("Loading YAML configuration from: {}", sourcePath);

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
                    builderLogger.debug("Resolved {} to {}", filePath, candidate);
                    return candidate;
                }
            }

            // Try classpath prefixes
            for (String prefix : classpathPrefixes) {
                String candidate = combineClasspath(prefix, filePath);
                if (getClass().getClassLoader().getResource(candidate) != null) {
                    builderLogger.debug("Resolved {} to classpath:{}", filePath, candidate);
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
}
