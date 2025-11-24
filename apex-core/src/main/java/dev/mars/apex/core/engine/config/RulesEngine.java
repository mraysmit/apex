package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.config.datasink.DataSinkConfiguration;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.config.pipeline.PipelineConfiguration;
import dev.mars.apex.core.config.yaml.*;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.engine.model.EnrichmentGroupResult;
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
 * // ✅ RECOMMENDED (production code):
 * RulesEngineService service = new RulesEngineService();
 * RulesEngine engine = service.createRulesEngineFromFile(yamlFile);
 *
 * // ✅ ACCEPTABLE (tests and simple cases):
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
    private final YamlEnrichmentProcessor enrichmentProcessor;
    private final UnifiedRuleEvaluator unifiedEvaluator;

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
    private final Map<String, dev.mars.apex.core.service.scenario.ScenarioConfiguration> scenarioRegistry;

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
                       Map<String, dev.mars.apex.core.service.scenario.ScenarioConfiguration> scenarioRegistry) {
        this.configuration = configuration;
        this.yamlConfig = yamlConfig;
        this.scenarioRegistry = scenarioRegistry;
        this.parser = new SpelExpressionParser();
        this.evaluatorService = new ExpressionEvaluatorService(this.parser);
        this.errorRecoveryService = new ErrorRecoveryService();
        this.performanceMonitor = new RulePerformanceMonitor();
        this.enrichmentProcessor = new YamlEnrichmentProcessor(new LookupServiceRegistry(), this.evaluatorService);

        // Load error recovery configuration from YAML if available, otherwise use defaults
        ErrorRecoveryConfig errorRecoveryConfig = loadErrorRecoveryConfig(yamlConfig);

        // Initialize the unified evaluator with error recovery configuration from YAML
        this.unifiedEvaluator = new UnifiedRuleEvaluator(this.evaluatorService, errorRecoveryService, performanceMonitor, errorRecoveryConfig);

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
        logger.debug("Using error recovery config: enabled={}, default-strategy={}",
                    errorRecoveryConfig.isEnabled(), errorRecoveryConfig.getDefaultStrategy());
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
                    }
                }
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
                    }
                }
            }

            logger.info("Pipeline components initialized: {} data sources, {} data sinks",
                    dataSources.size(), dataSinks.size());

        } catch (Exception e) {
            logger.warn("Failed to initialize pipeline components: {}", e.getMessage());
            logger.debug("Pipeline initialization exception details:", e);
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

            // Convert to RuleResult
            if (result.isSuccess()) {
                logger.info("Pipeline '{}' executed successfully in {}ms",
                        pipeline.getName(), result.getDurationMs());
                return RuleResult.match("pipeline:" + pipeline.getName(),
                        "Pipeline executed successfully", SeverityConstants.INFO);
            } else {
                logger.error("Pipeline '{}' execution failed: {}", pipeline.getName(), result.getError());
                return RuleResult.error("pipeline:" + pipeline.getName(),
                        "Pipeline execution failed: " + result.getError());
            }
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

        // Load scenario registry using ScenarioRegistryLoader
        ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
        Map<String, dev.mars.apex.core.service.scenario.ScenarioConfiguration> scenarios = loader.loadRegistry(registryPath);

        if (scenarios == null || scenarios.isEmpty()) {
            throw new YamlConfigurationException(
                "Scenario registry is empty or failed to load: " + registryPath
            );
        }

        logger.info("Loaded {} scenarios from registry: {}", scenarios.size(), registryPath);

        // Create a minimal RulesEngineConfiguration for scenario-only engine
        RulesEngineConfiguration config = new RulesEngineConfiguration();

        // Create RulesEngine with scenario registry
        return new RulesEngine(config, null, scenarios);
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
     *
     * @param ruleGroups The list of RuleGroup objects to execute
     * @param facts The facts to evaluate the rule groups against
     * @return The result of the first rule group that matches, or a default result if no rule groups match
     */
    public RuleResult executeRuleGroupsList(List<RuleGroup> ruleGroups, Map<String, Object> facts) {

        if (ruleGroups == null || ruleGroups.isEmpty()) {
            logger.info("No rule groups provided for execution");
            return RuleResult.noRules();
        }

        logger.info("Executing {} rule groups", ruleGroups.size());
        logger.debug("Facts provided: {}", facts != null ? facts.keySet() : "none");

        StandardEvaluationContext context = createContext(facts);

        // Track the highest severity from failed rule groups
        String highestFailedSeverity = SeverityConstants.INFO;
        String lastFailedGroupName = null;
        String lastFailedGroupMessage = null;

        // Evaluate rule groups in priority order
        for (RuleGroup group : ruleGroups) {
            logger.debug("Evaluating rule group: {}", group.getName());
            try {
                // Use detailed evaluation to get severity aggregation
                RuleGroupEvaluationResult evaluationResult = group.evaluateWithDetails(context);
                boolean result = evaluationResult.isGroupResult();
                String aggregatedSeverity = evaluationResult.getAggregatedSeverity();

                logger.debug("Rule group '{}' evaluated to: {} with aggregated severity: {}",
                           group.getName(), result, aggregatedSeverity);

                // Debug: Log individual results
                logger.debug("Individual results count: {}", evaluationResult.getIndividualResults().size());
                for (RuleResult individualResult : evaluationResult.getIndividualResults()) {
                    logger.debug("Individual result: type={}, success={}, message={}",
                               individualResult.getResultType(), individualResult.isSuccess(), individualResult.getMessage());
                }

                // Check if any individual rule had an ERROR result type (not just ERROR severity)
                // This indicates a rule evaluation exception, which is a business logic failure
                for (RuleResult individualResult : evaluationResult.getIndividualResults()) {
                    if (individualResult.getResultType() == RuleResult.ResultType.ERROR) {
                        // CRITICAL: Rule evaluation exception is a business logic failure
                        // This is NOT a "rule didn't match" scenario - it's a system failure
                        logger.error("CRITICAL: Rule evaluation failed in group '{}': {}",
                                   group.getName(), individualResult.getMessage());
                        return RuleResult.error(
                            group.getName(),
                            "Rule group evaluation failed: " + individualResult.getMessage(),
                            SeverityConstants.ERROR
                        );
                    }
                }

                if (result) {
                    logger.info("Rule group matched: {}", group.getName());
                    return RuleResult.match(group.getName(), group.getMessage(), aggregatedSeverity);
                } else {
                    // Track failed group with highest severity
                    if (getSeverityPriority(aggregatedSeverity) > getSeverityPriority(highestFailedSeverity)) {
                        highestFailedSeverity = aggregatedSeverity;
                        lastFailedGroupName = group.getName();
                        lastFailedGroupMessage = group.getMessage();
                    }
                }
            } catch (Exception e) {
                // CRITICAL: Rule group evaluation exception is a business logic failure
                // This is NOT a "rule didn't match" scenario - it's a system failure
                logger.error("CRITICAL: Rule group evaluation failed for '{}': {}", group.getName(), e.getMessage(), e);
                return RuleResult.error(
                    group.getName(),
                    "Rule group evaluation failed: " + e.getMessage(),
                    SeverityConstants.ERROR
                );
            }
        }

        logger.info("No rule groups matched");

        // Return result with highest severity from failed groups
        if (lastFailedGroupName != null) {
            return RuleResult.noMatch(lastFailedGroupName, lastFailedGroupMessage, highestFailedSeverity);
        } else {
            return RuleResult.noMatch();
        }
    }

    /**
     * Execute a list of enrichment groups against the provided target object.
     * This is a PRIVATE method - enrichment groups are processed automatically by evaluate().
     *
     * @param enrichmentGroups The list of enrichment groups to execute
     * @param targetObject The target object to enrich
     * @return The result of enrichment group execution
     */
    private RuleResult executeEnrichmentGroupsList(List<EnrichmentGroup> enrichmentGroups, Object targetObject) {
        if (enrichmentGroups == null || enrichmentGroups.isEmpty()) {
            logger.info("No enrichment groups provided for execution");
            return RuleResult.noRules();
        }

        logger.info("Executing {} enrichment groups", enrichmentGroups.size());

        List<String> failureMessages = new ArrayList<>();
        boolean overallSuccess = true;
        Map<String, Object> enrichedData = convertToMap(targetObject);

        for (EnrichmentGroup group : enrichmentGroups) {
            logger.debug("Evaluating enrichment group: {}", group.getName());
            logger.debug("Enriched data keys before group '{}': {}", group.getName(), enrichedData.keySet());
            try {
                EnrichmentGroupResult result = processEnrichmentGroup(group, enrichedData);
                logger.debug("Enriched data keys after group '{}': {}", group.getName(), enrichedData.keySet());

                if (!result.isSuccess()) {
                    overallSuccess = false;
                    failureMessages.add("Enrichment group '" + group.getId() + "' failed: " + result.getMessage());
                }

                // Collect enriched data from all enrichment results in the group
                if (result.getEnrichmentResults() != null) {
                    for (RuleResult enrichmentResult : result.getEnrichmentResults()) {
                        if (enrichmentResult.getEnrichedData() != null) {
                            enrichedData.putAll(enrichmentResult.getEnrichedData());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Enrichment group '{}' failed with exception: {}", group.getName(), e.getMessage());
                overallSuccess = false;
                failureMessages.add("Enrichment group '" + group.getId() + "' exception: " + e.getMessage());
            }
        }

        if (overallSuccess) {
            return RuleResult.enrichmentSuccess(enrichedData, SeverityConstants.INFO);
        } else {
            logger.debug("Returning enrichment failure with data keys: {}", enrichedData.keySet());
            return RuleResult.enrichmentFailure(failureMessages, enrichedData, SeverityConstants.ERROR);
        }
    }

    /**
     * Process a single enrichment group.
     * This is a PRIVATE helper method that migrates logic from YamlEnrichmentProcessor.
     *
     * @param group The enrichment group to process
     * @param targetObject The target object to enrich
     * @return The result of enrichment group processing
     */
    private EnrichmentGroupResult processEnrichmentGroup(EnrichmentGroup group, Object targetObject) {
        if (group == null) {
            return EnrichmentGroupResult.of("<null>", true, "No group", List.of(), 0L);
        }

        long start = System.currentTimeMillis();
        boolean andOp = group.isAndOperator();
        boolean shortCircuit = group.isStopOnFirstFailure() && !group.isDebugMode();

        List<YamlEnrichment> ordered = group.getEnrichmentsInOrder();
        List<RuleResult> results = new ArrayList<>();

        if (group.isParallelExecution() && ordered.size() > 1) {
            // Parallel execution - no short-circuit
            results = processEnrichmentGroupParallel(ordered, targetObject);
        } else {
            // Sequential execution with possible short-circuit
            results = processEnrichmentGroupSequential(ordered, targetObject, andOp, shortCircuit);
        }

        // Aggregate overall based on AND/OR semantics
        boolean overall = aggregateEnrichmentResults(results, andOp);

        long elapsed = System.currentTimeMillis() - start;
        String message = overall ? "Enrichment group succeeded" : "Enrichment group failed";
        return EnrichmentGroupResult.of(group.getId(), overall, message, results, elapsed);
    }

    /**
     * Process enrichments in parallel.
     *
     * @param enrichments The list of enrichments to process
     * @param targetObject The target object to enrich
     * @return List of enrichment results
     */
    private List<RuleResult> processEnrichmentGroupParallel(List<YamlEnrichment> enrichments, Object targetObject) {
        List<RuleResult> results = new ArrayList<>();
        List<Callable<RuleResult>> tasks = new ArrayList<>();

        for (YamlEnrichment enrichment : enrichments) {
            tasks.add(() -> {
                // Create a thread-local copy of the data for each parallel task
                // This prevents ConcurrentModificationException and data corruption
                // as HashMap is not thread-safe for concurrent modifications
                Object taskTargetObject = convertToMap(targetObject);
                try {
                    return enrichmentProcessor.processEnrichmentWithResult(enrichment, taskTargetObject);
                } catch (Exception e) {
                    List<String> msgs = new ArrayList<>();
                    msgs.add("Parallel enrichment exception: " + e.getMessage());
                    Map<String, Object> data = convertToMap(taskTargetObject);
                    return RuleResult.enrichmentFailure(msgs, data, SeverityConstants.ERROR);
                }
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(
            Math.min(tasks.size(), Runtime.getRuntime().availableProcessors())
        );
        try {
            List<Future<RuleResult>> futures = executor.invokeAll(tasks);
            for (Future<RuleResult> f : futures) {
                try {
                    results.add(f.get());
                } catch (Exception e) {
                    List<String> msgs = new ArrayList<>();
                    msgs.add("Error getting parallel enrichment result: " + e.getMessage());
                    Map<String, Object> data = convertToMap(targetObject);
                    results.add(RuleResult.enrichmentFailure(msgs, data, SeverityConstants.ERROR));
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            List<String> msgs = new ArrayList<>();
            msgs.add("Parallel execution interrupted: " + ie.getMessage());
            Map<String, Object> data = convertToMap(targetObject);
            results.add(RuleResult.enrichmentFailure(msgs, data, SeverityConstants.ERROR));
        } finally {
            executor.shutdownNow();
        }

        return results;
    }

    /**
     * Process enrichments sequentially with possible short-circuit.
     *
     * @param enrichments The list of enrichments to process
     * @param targetObject The target object to enrich
     * @param andOp Whether to use AND operator (true) or OR operator (false)
     * @param shortCircuit Whether to stop on first failure/success
     * @return List of enrichment results
     */
    private List<RuleResult> processEnrichmentGroupSequential(List<YamlEnrichment> enrichments, Object targetObject,
                                                               boolean andOp, boolean shortCircuit) {
        List<RuleResult> results = new ArrayList<>();

        for (YamlEnrichment enrichment : enrichments) {
            RuleResult r = enrichmentProcessor.processEnrichmentWithResult(enrichment, targetObject);
            results.add(r);
            boolean ok = r.isSuccess();

            if (andOp) {
                // AND: stop if failure and short-circuit enabled
                if (!ok && shortCircuit) {
                    break;
                }
            } else {
                // OR: stop if success and short-circuit enabled
                if (ok && shortCircuit) {
                    break;
                }
            }
        }

        return results;
    }

    /**
     * Aggregate enrichment results based on AND/OR operator.
     *
     * @param results The list of enrichment results
     * @param andOp Whether to use AND operator (true) or OR operator (false)
     * @return true if overall success, false otherwise
     */
    private boolean aggregateEnrichmentResults(List<RuleResult> results, boolean andOp) {
        boolean overall = andOp; // AND starts true, OR starts false
        if (!andOp) overall = false;

        for (RuleResult r : results) {
            boolean ok = r != null && r.isSuccess();
            if (andOp) {
                if (!ok) {
                    overall = false;
                }
            } else { // OR
                if (ok) {
                    overall = true;
                }
            }
        }

        return overall;
    }

    /**
     * Execute a list of rules against the provided facts.
     * This method determines the type of objects in the list and delegates to the appropriate method.
     *
     * @param rules The list of rules to execute (can be a mix of Rule and RuleGroup objects)
     * @param facts The facts to evaluate the rules against
     * @return The result of the first rule that matches, or a default result if no rules match
     */
    public RuleResult executeRules(List<RuleBase> rules, Map<String, Object> facts) {
        if (rules == null || rules.isEmpty()) {
            logger.info("No rules provided for execution");
            return RuleResult.noRules();
        }

        logger.info("Executing {} rules/rule groups", rules.size());
        logger.debug("Facts provided: {}", facts != null ? facts.keySet() : "none");

        // Check if all rules are of the same type and delegate to the appropriate method
        boolean allRules = true;
        boolean allRuleGroups = true;

        for (RuleBase ruleObj : rules) {
            if (!(ruleObj instanceof Rule)) {
                allRules = false;
            }
            if (!(ruleObj instanceof RuleGroup)) {
                allRuleGroups = false;
            }
        }

        if (allRules) {
            // All objects are Rule instances, so we can safely cast and delegate
            logger.debug("All objects are Rule instances, delegating to executeRulesList");
            @SuppressWarnings("unchecked")
            List<Rule> rulesList = (List<Rule>) (List<?>) rules;
            return executeRulesList(rulesList, facts);
        } else if (allRuleGroups) {
            // All objects are RuleGroup instances, so we can safely cast and delegate
            logger.debug("All objects are RuleGroup instances, delegating to executeRuleGroupsList");
            @SuppressWarnings("unchecked")
            List<RuleGroup> ruleGroupsList = (List<RuleGroup>) (List<?>) rules;
            return executeRuleGroupsList(ruleGroupsList, facts);
        }

        logger.debug("Mixed list of rules and rule groups, processing manually");
        // Mixed list or unknown types, process manually
        StandardEvaluationContext context = createContext(facts);

        // Evaluate rules in priority order
        for (RuleBase ruleObj : rules) {
            logger.debug("Evaluating rule/rule group: {}", ruleObj.getName());
            try {
                if (ruleObj instanceof Rule) {
                    Rule rule = (Rule) ruleObj;
                    Expression exp = parser.parseExpression(rule.getCondition());
                    Boolean result = exp.getValue(context, Boolean.class);
                    logger.debug("Rule '{}' evaluated to: {}", rule.getName(), result);

                    if (result != null && result) {
                        logger.info("Rule matched: {}", rule.getName());
                        return RuleResult.match(rule.getName(), rule.getMessage(), rule.getSeverity());
                    }
                } else if (ruleObj instanceof RuleGroup) {
                    RuleGroup group = (RuleGroup) ruleObj;
                    boolean result = group.evaluate(context);
                    logger.debug("Rule group '{}' evaluated to: {}", group.getName(), result);

                    if (result) {
                        logger.info("Rule group matched: {}", group.getName());
                        return RuleResult.match(group.getName(), group.getMessage());
                    }
                }
            } catch (Exception e) {
                String ruleName = ruleObj.getName();
                String errorMessage = String.format("Rule evaluation failed: %s", e.getMessage());

                // Get severity from rule configuration
                String severity = SeverityConstants.ERROR; // Default severity for evaluation errors
                if (ruleObj instanceof Rule) {
                    Rule rule = (Rule) ruleObj;
                    severity = rule.getSeverity() != null ? rule.getSeverity() : SeverityConstants.ERROR;
                }

                // Log error details at appropriate level based on severity
                if (SeverityConstants.CRITICAL.equalsIgnoreCase(severity)) {
                    logger.error("CRITICAL rule evaluation error for '{}': {}", ruleName, e.getMessage());
                } else if (SeverityConstants.WARNING.equalsIgnoreCase(severity)) {
                    logger.info("Rule evaluation warning for '{}': {}", ruleName, e.getMessage());
                } else {
                    logger.info("Rule evaluation error for '{}': {}", ruleName, e.getMessage());
                }

                // Always log full exception details at DEBUG level for troubleshooting
                logger.debug("Full exception details for rule/rule group '{}':", ruleName, e);

                return RuleResult.error(ruleName, errorMessage, severity);
            }
        }

        logger.info("No rules or rule groups matched");
        return RuleResult.noMatch();
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
            sectionOrder = Arrays.asList("rules", "rule-groups", "enrichments", "enrichment-groups");
        }

        logger.info("Executing sections in order: {}", sectionOrder);
        return evaluateSequential(yamlConfig, inputData, sectionOrder);
    }

    /**
     * Evaluate using sequential processing - execute sections in YAML document order.
     * This respects the developer's intent as expressed through YAML structure.
     *
     * If item-level order is available, processes individual items in document order.
     * Otherwise, falls back to section-level processing.
     */
    private RuleResult evaluateSequential(YamlRuleConfiguration yamlConfig, Map<String, Object> inputData, List<String> sectionOrder) {
        List<String> failureMessages = new ArrayList<>();
        Map<String, Object> enrichedData = new HashMap<>(inputData);
        boolean overallSuccess = true;

        try {
            // Check if item-level order is available
            List<ProcessingItem> itemOrder = yamlConfig.getItemOrder();

            if (itemOrder != null && !itemOrder.isEmpty()) {
                // NEW: Item-level processing
                logger.info("Processing {} items in document order", itemOrder.size());

                for (ProcessingItem item : itemOrder) {
                    logger.debug("Processing item: {} ({})", item.getItemId(), item.getSectionType());

                    RuleResult itemResult = processItem(item, yamlConfig, enrichedData);

                    // Check for ERROR result type - this includes validation rules with ERROR severity
                    // when error recovery is disabled (default for ERROR severity per APEX_ERROR_HANDLING_GUIDE)
                    if (itemResult.getResultType() == RuleResult.ResultType.ERROR) {
                        overallSuccess = false;
                        failureMessages.add(item.getSectionType() + " '" + item.getItemId() + "' error: " + itemResult.getMessage());
                    }

                    // Update enriched data with results
                    if (itemResult.getEnrichedData() != null) {
                        enrichedData.putAll(itemResult.getEnrichedData());
                    }
                }
            } else {
                // FALLBACK: Section-level processing
                logger.info("No item order available, falling back to section-level processing");
                logger.info("Processing {} sections in document order", sectionOrder.size());

                // Process sections in the order they appear in the YAML document
                for (String section : sectionOrder) {
                    logger.debug("Processing section: {}", section);

                switch (section) {
                    case "enrichments":
                        if (enrichmentProcessor != null && yamlConfig.getEnrichments() != null && !yamlConfig.getEnrichments().isEmpty()) {
                            logger.info("Processing {} enrichments", yamlConfig.getEnrichments().size());
                            try {
                                // Process enrichments with result tracking
                                RuleResult enrichmentResult = enrichmentProcessor.processEnrichmentsWithResult(
                                    yamlConfig.getEnrichments(), enrichedData, yamlConfig);

                                // Check for enrichment errors
                                if (enrichmentResult.getResultType() == RuleResult.ResultType.ERROR) {
                                    overallSuccess = false;
                                    failureMessages.add("Enrichment processing failed: " + enrichmentResult.getMessage());
                                    if (enrichmentResult.hasFailures()) {
                                        failureMessages.addAll(enrichmentResult.getFailureMessages());
                                    }
                                    logger.error("CRITICAL: Enrichment processing failed: {}", enrichmentResult.getMessage());
                                    // Return error immediately (fail-fast) but include enriched data
                                    return RuleResult.enrichmentFailure(failureMessages, enrichmentResult.getEnrichedData(), SeverityConstants.ERROR);
                                }

                                // Update enriched data from result
                                if (enrichmentResult.getEnrichedData() != null && !enrichmentResult.getEnrichedData().isEmpty()) {
                                    enrichedData.putAll(enrichmentResult.getEnrichedData());
                                    logger.debug("Enrichment completed, enriched data size: {}", enrichedData.size());
                                }
                            } catch (Exception e) {
                                logger.error("CRITICAL: Enrichment processing exception: {}", e.getMessage(), e);
                                overallSuccess = false;
                                failureMessages.add("Enrichment processing failed: " + e.getMessage());
                            }
                        }
                        break;

                    case "rules":
                        // Use rules from yamlConfig if available, otherwise fall back to engine's configuration
                        List<Rule> allRules = null;
                        if (yamlConfig.getRules() != null && !yamlConfig.getRules().isEmpty()) {
                            logger.info("Using rules from yamlConfig parameter");
                            YamlRuleFactory ruleFactory = new YamlRuleFactory();
                            allRules = ruleFactory.createRules(yamlConfig);
                        } else if (configuration.getAllRules() != null && !configuration.getAllRules().isEmpty()) {
                            logger.info("Using rules from engine's internal configuration");
                            allRules = configuration.getAllRules();
                        }

                        if (allRules != null && !allRules.isEmpty()) {
                            logger.info("Processing {} individual rules", allRules.size());
                            RuleResult ruleResult = executeRulesList(allRules, enrichedData);

                            // Check for ERROR result type - this includes validation rules with ERROR severity
                            // when error recovery is disabled (default for ERROR severity per APEX_ERROR_HANDLING_GUIDE)
                            if (ruleResult.getResultType() == RuleResult.ResultType.ERROR) {
                                overallSuccess = false;
                                failureMessages.add("Rule evaluation error: " + ruleResult.getMessage());
                            }

                            // Update enriched data with results from rules (field mappings)
                            if (ruleResult.getEnrichedData() != null) {
                                enrichedData.putAll(ruleResult.getEnrichedData());
                            }
                        }
                        break;

                    case "rule-groups":
                        // Use rule groups from yamlConfig if available, otherwise fall back to engine's configuration
                        List<RuleGroup> allRuleGroups = null;
                        if (yamlConfig.getRuleGroups() != null && !yamlConfig.getRuleGroups().isEmpty()) {
                            logger.info("Using rule groups from yamlConfig parameter");
                            YamlRuleFactory ruleFactory = new YamlRuleFactory();
                            // Need to create a temporary config to resolve rule references
                            RulesEngineConfiguration tempConfig = new RulesEngineConfiguration();
                            // First create rules and register them
                            List<Rule> rules = ruleFactory.createRules(yamlConfig);
                            for (Rule rule : rules) {
                                tempConfig.registerRule(rule);
                            }
                            // Then create rule groups
                            allRuleGroups = ruleFactory.createRuleGroups(yamlConfig, tempConfig);
                        } else if (configuration.getAllRuleGroups() != null && !configuration.getAllRuleGroups().isEmpty()) {
                            logger.info("Using rule groups from engine's internal configuration");
                            allRuleGroups = configuration.getAllRuleGroups();
                        }

                        if (allRuleGroups != null && !allRuleGroups.isEmpty()) {
                            logger.info("Processing {} rule groups", allRuleGroups.size());
                            RuleResult ruleGroupResult = executeRuleGroupsList(allRuleGroups, enrichedData);

                            // Check for ERROR result type - this includes validation rules with ERROR severity
                            // when error recovery is disabled (default for ERROR severity per APEX_ERROR_HANDLING_GUIDE)
                            if (ruleGroupResult.getResultType() == RuleResult.ResultType.ERROR) {
                                overallSuccess = false;
                                failureMessages.add("Rule group evaluation error: " + ruleGroupResult.getMessage());
                            }
                        }
                        break;

                    case "enrichment-groups":
                        // Use enrichment groups from yamlConfig if available, otherwise fall back to engine's configuration
                        List<EnrichmentGroup> allEnrichmentGroups = null;
                        if (yamlConfig.getEnrichmentGroups() != null && !yamlConfig.getEnrichmentGroups().isEmpty()) {
                            logger.info("Using enrichment groups from yamlConfig parameter");
                            allEnrichmentGroups = EnrichmentGroupFactory.buildEnrichmentGroups(yamlConfig);
                        } else if (configuration.getAllEnrichmentGroups() != null && !configuration.getAllEnrichmentGroups().isEmpty()) {
                            logger.info("Using enrichment groups from engine's internal configuration");
                            allEnrichmentGroups = configuration.getAllEnrichmentGroups();
                        }

                        if (allEnrichmentGroups != null && !allEnrichmentGroups.isEmpty()) {
                            logger.info("Processing {} enrichment groups", allEnrichmentGroups.size());
                            RuleResult enrichmentGroupResult = executeEnrichmentGroupsList(allEnrichmentGroups, enrichedData);

                            logger.debug("Enrichment group result type: {}", enrichmentGroupResult.getResultType());
                            logger.debug("Enrichment group result data keys: {}", enrichmentGroupResult.getEnrichedData().keySet());

                            if (enrichmentGroupResult.getResultType() == RuleResult.ResultType.ERROR) {
                                overallSuccess = false;
                                failureMessages.add("Enrichment group evaluation error: " + enrichmentGroupResult.getMessage());
                            }

                            // Update enriched data with results from enrichment groups
                            if (enrichmentGroupResult.getEnrichedData() != null) {
                                enrichedData.putAll(enrichmentGroupResult.getEnrichedData());
                            }
                        }
                        break;

                    case "pipeline":
                        if (yamlConfig.getPipeline() != null) {
                            logger.info("Processing pipeline: {}", yamlConfig.getPipeline().getName());
                            RuleResult pipelineResult = executePipeline(yamlConfig.getPipeline(), enrichedData);

                            if (pipelineResult.getResultType() == RuleResult.ResultType.ERROR) {
                                overallSuccess = false;
                                failureMessages.add("Pipeline execution error: " + pipelineResult.getMessage());
                            }
                        }
                        break;

                    case "metadata":
                    case "data-sources":
                    case "data-source-refs":
                    case "rule-refs":
                    case "enrichment-refs":
                    case "data-sinks":
                    case "categories":
                        // These sections are configuration/metadata - not executed
                        logger.debug("Skipping configuration section: {}", section);
                        break;

                    case "rule-chains":
                        if (yamlConfig.getRuleChains() != null && !yamlConfig.getRuleChains().isEmpty()) {
                            logger.info("Processing {} rule chains", yamlConfig.getRuleChains().size());
                            for (YamlRuleChain chain : yamlConfig.getRuleChains()) {
                                RuleResult chainResult = processRuleChainItem(chain.getId(), yamlConfig, enrichedData);
                                if (chainResult.getResultType() == RuleResult.ResultType.ERROR) {
                                    overallSuccess = false;
                                    failureMessages.add("Rule chain '" + chain.getId() + "' error: " + chainResult.getMessage());
                                }
                            }
                        }
                        break;

                    case "error-recovery":
                        // These sections are configuration/metadata - not executed
                        logger.debug("Skipping configuration section: {}", section);
                        break;

                    case "transformations":
                        // Process transformations section
                        if (yamlConfig.getTransformations() != null && !yamlConfig.getTransformations().isEmpty()) {
                            logger.info("Processing {} transformations", yamlConfig.getTransformations().size());
                            YamlTransformationProcessor transformationProcessor = new YamlTransformationProcessor(this.evaluatorService);

                            // Process transformations with result tracking
                            RuleResult transformationResult = transformationProcessor.processTransformationsWithResult(
                                yamlConfig.getTransformations(), enrichedData);

                            // Check for transformation errors
                            if (transformationResult.getResultType() == RuleResult.ResultType.ERROR) {
                                overallSuccess = false;
                                failureMessages.add("Transformation processing failed: " + transformationResult.getMessage());
                                if (transformationResult.hasFailures()) {
                                    failureMessages.addAll(transformationResult.getFailureMessages());
                                }
                                logger.error("CRITICAL: Transformation processing failed: {}", transformationResult.getMessage());
                                // Return error immediately (fail-fast) but include enriched data
                                return RuleResult.evaluationFailure(failureMessages, transformationResult.getEnrichedData(), "transformations", transformationResult.getMessage(), SeverityConstants.ERROR);
                            }

                            // Update enrichedData with transformed data
                            if (transformationResult.getEnrichedData() != null && !transformationResult.getEnrichedData().isEmpty()) {
                                enrichedData.putAll(transformationResult.getEnrichedData());
                            }
                        }
                        break;

                    default:
                        logger.warn("Unknown section encountered during sequential processing: {}", section);
                        break;
                }
            }
            } // End of fallback section-level processing

            // Return comprehensive result
            if (overallSuccess && failureMessages.isEmpty()) {
                logger.info("Sequential evaluation completed successfully");
                return RuleResult.evaluationSuccess(enrichedData, "evaluation", "Sequential evaluation completed successfully");
            } else {
                logger.info("Sequential evaluation completed with {} failures", failureMessages.size());
                logger.debug("Final enriched data keys (failure): {}", enrichedData.keySet());
                return RuleResult.evaluationFailure(failureMessages, enrichedData, "evaluation", "Sequential evaluation completed with failures");
            }

        } catch (Exception e) {
            logger.error("Sequential evaluation failed with exception: {}", e.getMessage());
            logger.debug("Full sequential evaluation exception details:", e);
            failureMessages.add("Sequential evaluation failed: " + e.getMessage());
            return RuleResult.evaluationFailure(failureMessages, enrichedData, "evaluation", "Sequential evaluation failed");
        }
    }

    /**
     * Process a single item based on its section type.
     * This method dispatches to the appropriate processor based on the item's section type.
     *
     * @param item The processing item containing section type and item ID
     * @param yamlConfig The YAML configuration
     * @param data The data to process
     * @return RuleResult from processing the item
     */
    private RuleResult processItem(ProcessingItem item, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
        String sectionType = item.getSectionType();
        String itemId = item.getItemId();

        logger.debug("processItem called with sectionType='{}', itemId='{}'", sectionType, itemId);

        switch (sectionType) {
            case "enrichments":
                return processEnrichmentItem(itemId, yamlConfig, data);
            case "rules":
                return processRuleItem(itemId, yamlConfig, data);
            case "enrichment-groups":
                return processEnrichmentGroupItem(itemId, yamlConfig, data);
            case "rule-groups":
                return processRuleGroupItem(itemId, yamlConfig, data);
            case "transformations":
                logger.debug("Matched transformations case, calling processTransformationItem");
                return processTransformationItem(itemId, yamlConfig, data);
            case "rule-chains":
                return processRuleChainItem(itemId, yamlConfig, data);
            default:
                logger.warn("Unknown section type: {}", sectionType);
                return RuleResult.error(sectionType + ":" + itemId, "Unknown section type");
        }
    }

    /**
     * Process a single enrichment by ID.
     *
     * @param enrichmentId The enrichment ID to process
     * @param yamlConfig The YAML configuration
     * @param data The data to enrich
     * @return RuleResult from processing the enrichment
     */
    private RuleResult processEnrichmentItem(String enrichmentId, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
        // Find enrichment in yamlConfig.getEnrichments()
        YamlEnrichment enrichment = findEnrichmentById(yamlConfig, enrichmentId);
        if (enrichment == null) {
            logger.warn("Enrichment not found: {}", enrichmentId);
            return RuleResult.error("enrichment:" + enrichmentId, "Enrichment not found");
        }

        // Process single enrichment using YamlEnrichmentProcessor.processEnrichmentWithResult()
        // Pass yamlConfig so that enrichments can access #ruleGroupResults context variable
        // This method returns RuleResult directly (unlike processEnrichment() which returns Object)
        // NOTE: We do NOT need to skip rule processing here because processEnrichmentWithResult()
        // no longer calls processRulesAndRuleGroups() - APEX processes YAML in STRICT DOCUMENT ORDER ONLY
        return enrichmentProcessor.processEnrichmentWithResult(enrichment, data, yamlConfig);
    }

    /**
     * Process a single rule by ID.
     *
     * @param ruleId The rule ID to process
     * @param yamlConfig The YAML configuration
     * @param data The data to evaluate
     * @return RuleResult from processing the rule
     */
    private RuleResult processRuleItem(String ruleId, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
        // First try to find rule in yamlConfig
        Rule rule = null;
        if (yamlConfig != null && yamlConfig.getRules() != null) {
            for (YamlRule yamlRule : yamlConfig.getRules()) {
                if (ruleId.equals(yamlRule.getId())) {
                    // Convert YamlRule to Rule using factory
                    YamlRuleFactory ruleFactory = new YamlRuleFactory();
                    rule = ruleFactory.createRuleWithMetadata(yamlRule);
                    break;
                }
            }
        }

        // Fall back to engine's internal configuration if not found in yamlConfig
        if (rule == null) {
            rule = configuration.getRuleById(ruleId);
        }

        if (rule == null) {
            logger.warn("Rule not found: {}", ruleId);
            return RuleResult.error("rule:" + ruleId, "Rule not found");
        }

        // Execute single rule using executeRulesList()
        RuleResult result = executeRulesList(List.of(rule), data);

        // Store individual rule result for conditional mapping in enrichments
        // This allows enrichments to reference #ruleResults in document order mode
        if (enrichmentProcessor != null) {
            boolean passed = result.isSuccess() && result.isTriggered();
            enrichmentProcessor.storeIndividualRuleResult(ruleId, passed);
            logger.debug("Stored individual rule result for '{}': passed={}", ruleId, passed);
        }

        return result;
    }

    /**
     * Process a single enrichment group by ID.
     *
     * @param groupId The enrichment group ID to process
     * @param yamlConfig The YAML configuration
     * @param data The data to enrich
     * @return RuleResult from processing the enrichment group
     */
    private RuleResult processEnrichmentGroupItem(String groupId, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
        // First try to find enrichment group in yamlConfig
        EnrichmentGroup group = null;
        if (yamlConfig != null && yamlConfig.getEnrichmentGroups() != null) {
            for (YamlEnrichmentGroup yamlGroup : yamlConfig.getEnrichmentGroups()) {
                if (groupId.equals(yamlGroup.getId())) {
                    // Convert YamlEnrichmentGroup to EnrichmentGroup using factory
                    List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(yamlConfig);
                    // Find the specific group we need
                    for (EnrichmentGroup g : groups) {
                        if (groupId.equals(g.getId())) {
                            group = g;
                            break;
                        }
                    }
                    break;
                }
            }
        }

        // Fall back to engine's internal configuration if not found in yamlConfig
        if (group == null) {
            group = configuration.getEnrichmentGroupById(groupId);
        }

        if (group == null) {
            logger.warn("Enrichment group not found: {}", groupId);
            return RuleResult.error("enrichment-group:" + groupId, "Enrichment group not found");
        }

        // Execute single enrichment group using executeEnrichmentGroupsList()
        return executeEnrichmentGroupsList(List.of(group), data);
    }

    /**
     * Process a single rule group by ID.
     *
     * @param groupId The rule group ID to process
     * @param yamlConfig The YAML configuration
     * @param data The data to evaluate
     * @return RuleResult from processing the rule group
     */
    private RuleResult processRuleGroupItem(String groupId, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
        // First try to find rule group in yamlConfig
        RuleGroup group = null;
        if (yamlConfig != null && yamlConfig.getRuleGroups() != null) {
            for (YamlRuleGroup yamlGroup : yamlConfig.getRuleGroups()) {
                if (groupId.equals(yamlGroup.getId())) {
                    try {
                        // Convert YamlRuleGroup to RuleGroup using factory
                        YamlRuleFactory ruleFactory = new YamlRuleFactory();
                        // Need to create a temporary config to resolve rule references
                        RulesEngineConfiguration tempConfig = new RulesEngineConfiguration();
                        // First create rules and register them
                        List<Rule> rules = ruleFactory.createRules(yamlConfig);
                        for (Rule rule : rules) {
                            tempConfig.registerRule(rule);
                        }
                        // Then create rule groups
                        List<RuleGroup> groups = ruleFactory.createRuleGroups(yamlConfig, tempConfig);
                        // Find the specific group we need
                        for (RuleGroup g : groups) {
                            if (groupId.equals(g.getId())) {
                                group = g;
                                break;
                            }
                        }
                    } catch (YamlConfigurationException e) {
                        logger.error("Failed to create rule group from YAML: {}", groupId, e);
                        return RuleResult.error("rule-group:" + groupId, "Failed to create rule group: " + e.getMessage());
                    }
                    break;
                }
            }
        }

        // Fall back to engine's internal configuration if not found in yamlConfig
        if (group == null) {
            group = configuration.getRuleGroupById(groupId);
        }

        if (group == null) {
            logger.warn("Rule group not found: {}", groupId);
            return RuleResult.error("rule-group:" + groupId, "Rule group not found");
        }

        // Execute single rule group using executeRuleGroupsList()
        RuleResult result = executeRuleGroupsList(List.of(group), data);

        // Store rule group results for conditional mapping in enrichments
        // This allows enrichments to reference #ruleGroupResults in document order mode
        if (enrichmentProcessor != null) {
            boolean passed = result.isSuccess() && result.isTriggered();
            Map<String, Boolean> ruleResults = group.getRuleResults();
            enrichmentProcessor.storeRuleGroupResult(groupId, passed, ruleResults);
            logger.debug("Stored rule group result for '{}': passed={}, ruleResults={}", groupId, passed, ruleResults);
        }

        return result;
    }

    /**
     * Process a single transformation by ID.
     *
     * @param transformationId The transformation ID to process
     * @param yamlConfig The YAML configuration
     * @param data The data to transform
     * @return RuleResult from processing the transformation
     */
    private RuleResult processTransformationItem(String transformationId, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
        // Find transformation in yamlConfig.getTransformations()
        dev.mars.apex.core.config.yaml.YamlTransformation transformation = findTransformationById(yamlConfig, transformationId);
        if (transformation == null) {
            logger.warn("Transformation not found: {}", transformationId);
            return RuleResult.error("transformation:" + transformationId, "Transformation not found");
        }

        // Create transformation processor
        dev.mars.apex.core.service.transformation.YamlTransformationProcessor processor =
            new dev.mars.apex.core.service.transformation.YamlTransformationProcessor(this.evaluatorService);

        // Process single transformation with result tracking
        RuleResult transformationResult = processor.processTransformationsWithResult(List.of(transformation), data);

        // Check for transformation errors
        if (transformationResult.getResultType() == RuleResult.ResultType.ERROR) {
            logger.error("CRITICAL: Transformation processing failed: {}", transformationResult.getMessage());
            return transformationResult; // Return error result directly
        }

        // Return transformed data in RuleResult (do NOT modify input data map)
        return transformationResult;
    }

    /**
     * Find a transformation by ID in the configuration.
     *
     * @param config The YAML configuration
     * @param transformationId The transformation ID to find
     * @return The YamlTransformation if found, null otherwise
     */
    private dev.mars.apex.core.config.yaml.YamlTransformation findTransformationById(YamlRuleConfiguration config, String transformationId) {
        if (config.getTransformations() != null) {
            for (dev.mars.apex.core.config.yaml.YamlTransformation transformation : config.getTransformations()) {
                if (transformationId.equals(transformation.getId())) {
                    return transformation;
                }
            }
        }
        return null;
    }

    /**
     * Find an enrichment by ID in the configuration.
     * Similar to existing findRuleById() method in YamlEnrichmentProcessor.
     *
     * @param config The YAML configuration
     * @param enrichmentId The enrichment ID to find
     * @return The YamlEnrichment if found, null otherwise
     */
    private YamlEnrichment findEnrichmentById(YamlRuleConfiguration config, String enrichmentId) {
        if (config.getEnrichments() != null) {
            for (YamlEnrichment enrichment : config.getEnrichments()) {
                if (enrichmentId.equals(enrichment.getId())) {
                    return enrichment;
                }
            }
        }
        return null;
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
    public dev.mars.apex.core.service.scenario.ScenarioExecutionResult evaluateScenario(Map<String, Object> inputData) {
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

        // Parse scenario configuration from YAML
        dev.mars.apex.core.service.scenario.ScenarioConfiguration scenario = parseScenarioFromYaml(this.yamlConfig);

        // Create ScenarioStageExecutor and execute stages
        dev.mars.apex.core.service.scenario.ScenarioStageExecutor executor =
            new dev.mars.apex.core.service.scenario.ScenarioStageExecutor();

        return executor.executeStages(scenario, inputData);
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
    public dev.mars.apex.core.service.scenario.ScenarioExecutionResult evaluateScenario(String scenarioId, Map<String, Object> inputData) {
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
        dev.mars.apex.core.service.scenario.ScenarioConfiguration scenario = getScenario(scenarioId);

        // Create ScenarioStageExecutor and execute stages
        dev.mars.apex.core.service.scenario.ScenarioStageExecutor executor =
            new dev.mars.apex.core.service.scenario.ScenarioStageExecutor();

        return executor.executeStages(scenario, inputData);
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
     * // "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'"
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
    public dev.mars.apex.core.service.scenario.ScenarioExecutionResult evaluateWithClassification(Map<String, Object> inputData) {
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
        dev.mars.apex.core.service.scenario.ScenarioConfiguration scenario = findMatchingScenario(inputData);

        if (scenario == null) {
            throw new IllegalStateException(
                "No matching scenario found for the provided input data. " +
                "Ensure that at least one scenario's classification rule matches the data."
            );
        }

        logger.info("Matched scenario: {}", scenario.getScenarioId());

        // Create ScenarioStageExecutor and execute stages
        dev.mars.apex.core.service.scenario.ScenarioStageExecutor executor =
            new dev.mars.apex.core.service.scenario.ScenarioStageExecutor();

        return executor.executeStages(scenario, inputData);
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
     * If the object is already a Map, return a copy.
     * Otherwise, wrap it in a Map with key "data".
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
     * @param scenarioId The scenario ID to look up
     * @return The scenario configuration
     * @throws IllegalArgumentException if scenario not found
     */
    private dev.mars.apex.core.service.scenario.ScenarioConfiguration getScenario(String scenarioId) {
        if (this.scenarioRegistry == null) {
            throw new IllegalStateException("Scenario registry is not initialized");
        }

        dev.mars.apex.core.service.scenario.ScenarioConfiguration scenario = this.scenarioRegistry.get(scenarioId);

        if (scenario == null) {
            throw new IllegalArgumentException(
                "Scenario not found: " + scenarioId + ". " +
                "Available scenarios: " + this.scenarioRegistry.keySet()
            );
        }

        return scenario;
    }

    /**
     * Find the first matching scenario based on classification rules.
     * Iterates through all scenarios in the registry and evaluates their classification rules
     * against the provided input data using SpEL expressions.
     *
     * @param inputData The input data to match against classification rules
     * @return The first matching scenario, or null if no match found
     */
    private dev.mars.apex.core.service.scenario.ScenarioConfiguration findMatchingScenario(
            Map<String, Object> inputData) {

        if (this.scenarioRegistry == null || this.scenarioRegistry.isEmpty()) {
            logger.warn("Scenario registry is empty - no scenarios to match");
            return null;
        }

        logger.debug("Evaluating {} scenarios for classification match", this.scenarioRegistry.size());

        for (dev.mars.apex.core.service.scenario.ScenarioConfiguration scenario : this.scenarioRegistry.values()) {
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
    private dev.mars.apex.core.service.scenario.ScenarioConfiguration parseScenarioFromYaml(
            dev.mars.apex.core.config.yaml.YamlRuleConfiguration yamlConfig) {

        if (!yamlConfig.hasScenario()) {
            throw new IllegalStateException("YAML configuration does not contain a scenario section");
        }

        Object scenarioData = yamlConfig.getScenarioData();
        if (!(scenarioData instanceof java.util.Map)) {
            throw new IllegalStateException("Scenario data must be a Map");
        }

        java.util.Map<String, Object> scenarioMap = (java.util.Map<String, Object>) scenarioData;
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
    private dev.mars.apex.core.service.scenario.ScenarioConfiguration parseScenarioConfiguration(
            java.util.Map<String, Object> scenarioData) {

        dev.mars.apex.core.service.scenario.ScenarioConfiguration scenario =
            new dev.mars.apex.core.service.scenario.ScenarioConfiguration();

        scenario.setScenarioId((String) scenarioData.get("scenario-id"));
        scenario.setName((String) scenarioData.get("name"));
        scenario.setDescription((String) scenarioData.get("description"));

        // Parse data types (legacy)
        java.util.List<String> dataTypes = (java.util.List<String>) scenarioData.get("data-types");
        if (dataTypes != null) {
            scenario.setDataTypes(dataTypes);
        }

        // Parse classification rule (modern Map-based routing)
        java.util.Map<String, Object> classificationRule =
            (java.util.Map<String, Object>) scenarioData.get("classification-rule");
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
        java.util.List<String> ruleConfigurations =
            (java.util.List<String>) scenarioData.get("rule-configurations");
        if (ruleConfigurations != null) {
            scenario.setRuleConfigurations(ruleConfigurations);
        }

        // Parse processing stages (modern stage-based configuration)
        java.util.List<java.util.Map<String, Object>> processingStages =
            (java.util.List<java.util.Map<String, Object>>) scenarioData.get("processing-stages");
        if (processingStages != null) {
            java.util.List<dev.mars.apex.core.service.scenario.ScenarioStage> stages = new java.util.ArrayList<>();
            for (java.util.Map<String, Object> stageData : processingStages) {
                dev.mars.apex.core.service.scenario.ScenarioStage stage = parseScenarioStage(stageData);
                if (stage != null) {
                    stages.add(stage);
                }
            }

            // Preserve classification rule fields when creating stage-based scenario
            String classificationCondition = scenario.getClassificationRuleCondition();
            String classificationDescription = scenario.getClassificationRuleDescription();
            String description = scenario.getDescription();

            scenario = dev.mars.apex.core.service.scenario.ScenarioConfiguration.withStages(
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
    private dev.mars.apex.core.service.scenario.ScenarioStage parseScenarioStage(
            java.util.Map<String, Object> stageData) {

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

            dev.mars.apex.core.service.scenario.ScenarioStage stage =
                new dev.mars.apex.core.service.scenario.ScenarioStage(stageName, configFile, executionOrder);

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
            java.util.List<String> dependsOn = (java.util.List<String>) stageData.get("depends-on");
            if (dependsOn != null) {
                for (String dependency : dependsOn) {
                    stage.addDependency(dependency);
                }
            }

            // Parse stage metadata
            java.util.Map<String, Object> stageMetadata =
                (java.util.Map<String, Object>) stageData.get("stage-metadata");
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
        public dev.mars.apex.core.service.scenario.ScenarioExecutionResult evaluate(Map<String, Object> inputData) {
            return engine.evaluateScenario(inputData);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public dev.mars.apex.core.service.scenario.ScenarioExecutionResult evaluate(String scenarioId, Map<String, Object> inputData) {
            return engine.evaluateScenario(scenarioId, inputData);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public dev.mars.apex.core.service.scenario.ScenarioExecutionResult evaluateWithClassification(Map<String, Object> inputData) {
            return engine.evaluateWithClassification(inputData);
        }
    }

    /**
     * Process a single rule chain by ID.
     *
     * @param chainId The rule chain ID to process
     * @param yamlConfig The YAML configuration
     * @param data The data to evaluate
     * @return RuleResult from processing the rule chain
     */
    private RuleResult processRuleChainItem(String chainId, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
        // Find rule chain in yamlConfig
        YamlRuleChain chain = findRuleChainById(yamlConfig, chainId);
        if (chain == null) {
            logger.warn("Rule chain not found: {}", chainId);
            return RuleResult.error("rule-chain:" + chainId, "Rule chain not found");
        }

        if (!chain.isEnabled()) {
            logger.info("Rule chain '{}' is disabled, skipping", chainId);
            return RuleResult.noMatch(chainId, "Rule chain disabled", SeverityConstants.INFO);
        }

        logger.info("Processing rule chain: {} (Pattern: {})", chain.getName(), chain.getPattern());

        // Handle different patterns
        if ("conditional-chaining".equals(chain.getPattern())) {
            return executeConditionalChainingPattern(chain, data);
        } else if ("result-based-routing".equals(chain.getPattern())) {
            return executeResultBasedRoutingPattern(chain, yamlConfig, data);
        } else {
            logger.warn("Rule chain pattern '{}' not yet supported", chain.getPattern());
            return RuleResult.noMatch(chainId, "Pattern not supported: " + chain.getPattern(), SeverityConstants.INFO);
        }
    }

    /**
     * Find a rule chain by ID in the configuration.
     *
     * @param config The YAML configuration
     * @param chainId The rule chain ID to find
     * @return The YamlRuleChain if found, null otherwise
     */
    private YamlRuleChain findRuleChainById(YamlRuleConfiguration config, String chainId) {
        if (config.getRuleChains() != null) {
            for (YamlRuleChain chain : config.getRuleChains()) {
                if (chainId.equals(chain.getId())) {
                    return chain;
                }
            }
        }
        return null;
    }

    /**
     * Execute a rule chain with the 'result-based-routing' pattern.
     *
     * @param chain The rule chain to execute
     * @param yamlConfig The YAML configuration
     * @param data The data to evaluate
     * @return RuleResult from execution
     */
    @SuppressWarnings("unchecked")
    private RuleResult executeResultBasedRoutingPattern(YamlRuleChain chain, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
        Map<String, Object> config = chain.getConfiguration();
        if (config == null) {
            return RuleResult.error(chain.getId(), "Missing configuration for rule chain");
        }

        // 1. Evaluate Router Rule
        Map<String, Object> routerRuleConfig = (Map<String, Object>) config.get("router-rule");
        if (routerRuleConfig == null) {
            return RuleResult.error(chain.getId(), "Missing router-rule configuration");
        }

        String condition = (String) routerRuleConfig.get("condition");
        String resultField = (String) routerRuleConfig.get("result-field");

        StandardEvaluationContext context = createContext(data);
        String routeKey = null;
        try {
            Expression exp = parser.parseExpression(condition);
            Object result = exp.getValue(context);
            routeKey = result != null ? result.toString() : "null";
        } catch (Exception e) {
            logger.error("Error evaluating router rule for chain '{}': {}", chain.getId(), e.getMessage());
            return RuleResult.error(chain.getId(), "Router evaluation failed: " + e.getMessage());
        }

        // Set result field if specified
        if (resultField != null && !resultField.isEmpty()) {
            data.put(resultField, routeKey);
            logger.debug("Set result field '{}' to {}", resultField, routeKey);
        }

        logger.info("Router evaluated to route: '{}'", routeKey);

        // 2. Execute Route Rules
        Map<String, Object> routes = (Map<String, Object>) config.get("routes");
        if (routes != null) {
            Object routeObj = routes.get(routeKey);
            List<Map<String, Object>> rulesConfig = null;
            
            if (routeObj instanceof Map) {
                Map<String, Object> routeConfig = (Map<String, Object>) routeObj;
                rulesConfig = (List<Map<String, Object>>) routeConfig.get("rules");

                // Handle enrichment groups
                List<String> enrichmentGroupRefs = null;
                if (routeConfig.containsKey("enrichment-group-references")) {
                    enrichmentGroupRefs = (List<String>) routeConfig.get("enrichment-group-references");
                } else if (routeConfig.containsKey("enrichment-groups")) {
                    enrichmentGroupRefs = (List<String>) routeConfig.get("enrichment-groups");
                }

                if (enrichmentGroupRefs != null && !enrichmentGroupRefs.isEmpty()) {
                    logger.info("Executing {} enrichment groups for route '{}'", enrichmentGroupRefs.size(), routeKey);
                    List<EnrichmentGroup> groupsToExecute = new ArrayList<>();
                    
                    for (String groupId : enrichmentGroupRefs) {
                        EnrichmentGroup group = null;
                        
                        // Try to find in YAML config first
                        if (yamlConfig != null && yamlConfig.getEnrichmentGroups() != null) {
                            for (YamlEnrichmentGroup yamlGroup : yamlConfig.getEnrichmentGroups()) {
                                if (groupId.equals(yamlGroup.getId())) {
                                    // Found in YAML, build it
                                    List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(yamlConfig);
                                    for (EnrichmentGroup g : groups) {
                                        if (groupId.equals(g.getId())) {
                                            group = g;
                                            break;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        
                        // Fallback to engine configuration
                        if (group == null) {
                            group = configuration.getEnrichmentGroupById(groupId);
                        }

                        if (group != null) {
                            groupsToExecute.add(group);
                        } else {
                            logger.warn("Enrichment group '{}' not found for route '{}'", groupId, routeKey);
                        }
                    }
                    
                    if (!groupsToExecute.isEmpty()) {
                        RuleResult enrichmentResult = executeEnrichmentGroupsList(groupsToExecute, data);
                        if (enrichmentResult.getEnrichedData() != null) {
                            data.putAll(enrichmentResult.getEnrichedData());
                            logger.debug("Merged enriched data from route '{}' into context", routeKey);
                        }
                    }
                }
            } else if (routeObj instanceof List) {
                // Support direct list for backward compatibility or simpler syntax
                rulesConfig = (List<Map<String, Object>>) routeObj;
            }
            
            if (rulesConfig != null) {
                logger.info("Executing route '{}' for chain '{}'", routeKey, chain.getName());
                // Convert simple map configs to Rule objects and execute
                List<Rule> rules = new ArrayList<>();
                for (Map<String, Object> rc : rulesConfig) {
                    String ruleId = (String) rc.get("id");
                    String ruleCondition = (String) rc.get("condition");
                    String ruleMessage = (String) rc.get("message");
                    String ruleResultField = (String) rc.get("result-field");
                    logger.debug("Creating rule '{}' with result-field: '{}'", ruleId, ruleResultField);
                    
                    // Use full constructor to include resultField and severity
                    Rule r = new Rule(
                        ruleId, 
                        Collections.singleton(new dev.mars.apex.core.engine.model.Category("default", 100)), 
                        "Rule-" + ruleId, 
                        ruleCondition, 
                        ruleMessage, 
                        ruleMessage, 
                        100, 
                        SeverityConstants.INFO, 
                        null, // metadata
                        null, // defaultValue
                        null, // successCode
                        null, // errorCode
                        null, // mapToField
                        ruleResultField // resultField
                    );
                    rules.add(r);
                }
                executeRulesList(rules, data);
                return RuleResult.match(chain.getId(), "Executed route: " + routeKey);
            } else {
                // If we executed enrichment groups but no rules, consider it a match
                if (routeObj instanceof Map) {
                    Map<String, Object> routeConfig = (Map<String, Object>) routeObj;
                    if (routeConfig.containsKey("enrichment-group-references") || routeConfig.containsKey("enrichment-groups")) {
                        return RuleResult.match(chain.getId(), "Executed route (enrichment only): " + routeKey);
                    }
                }
                
                logger.info("No rules defined for route '{}' in chain '{}'", routeKey, chain.getName());
                return RuleResult.noMatch(chain.getId(), "No rules for route: " + routeKey, SeverityConstants.INFO);
            }
        }

        return RuleResult.noMatch(chain.getId(), "No routes configuration found", SeverityConstants.WARNING);
    }

    /**
     * Execute a rule chain with the 'conditional-chaining' pattern.
     *
     * @param chain The rule chain to execute
     * @param data The data to evaluate
     * @return RuleResult from execution
     */
    @SuppressWarnings("unchecked")
    private RuleResult executeConditionalChainingPattern(YamlRuleChain chain, Map<String, Object> data) {
        Map<String, Object> config = chain.getConfiguration();
        if (config == null) {
            return RuleResult.error(chain.getId(), "Missing configuration for rule chain");
        }

        // 1. Evaluate Trigger Rule
        Map<String, Object> triggerRuleConfig = (Map<String, Object>) config.get("trigger-rule");
        if (triggerRuleConfig == null) {
            return RuleResult.error(chain.getId(), "Missing trigger-rule configuration");
        }

        String condition = (String) triggerRuleConfig.get("condition");
        String message = (String) triggerRuleConfig.get("message");
        String resultField = (String) triggerRuleConfig.get("result-field");

        StandardEvaluationContext context = createContext(data);
        boolean triggered = false;
        try {
            Expression exp = parser.parseExpression(condition);
            Boolean result = exp.getValue(context, Boolean.class);
            triggered = result != null && result;
        } catch (Exception e) {
            logger.error("Error evaluating trigger rule for chain '{}': {}", chain.getId(), e.getMessage());
            return RuleResult.error(chain.getId(), "Trigger evaluation failed: " + e.getMessage());
        }

        // Set result field if specified
        if (resultField != null && !resultField.trim().isEmpty()) {
            data.put(resultField, triggered);
            logger.debug("Set result field '{}' to {}", resultField, triggered);
        }

        // 2. Execute Conditional Rules
        Map<String, Object> conditionalRules = (Map<String, Object>) config.get("conditional-rules");
        if (conditionalRules != null) {
            String sectionToExecute = triggered ? "on-trigger" : "on-no-trigger";
            List<Map<String, Object>> rulesConfig = (List<Map<String, Object>>) conditionalRules.get(sectionToExecute);
            
            if (rulesConfig != null) {
                logger.info("Executing '{}' path for chain '{}'", sectionToExecute, chain.getName());
                // Convert simple map configs to Rule objects and execute
                List<Rule> rules = new ArrayList<>();
                for (Map<String, Object> rc : rulesConfig) {
                    String ruleId = (String) rc.get("id");
                    String ruleCondition = (String) rc.get("condition");
                    String ruleMessage = (String) rc.get("message");
                    String ruleResultField = (String) rc.get("result-field");
                    String ruleSeverity = (String) rc.get("severity");
                    
                    // Use full constructor to include resultField and severity
                    Rule r = new Rule(
                        ruleId, 
                        Collections.singleton(new dev.mars.apex.core.engine.model.Category("default", 100)), 
                        "Rule-" + ruleId, 
                        ruleCondition, 
                        ruleMessage, 
                        ruleMessage, 
                        100, 
                        ruleSeverity != null ? ruleSeverity : SeverityConstants.INFO, 
                        null, // metadata
                        null, // defaultValue
                        null, // successCode
                        null, // errorCode
                        null, // mapToField
                        ruleResultField // resultField
                    );
                    rules.add(r);
                }
                executeRulesList(rules, data);
            }
        }

        if (triggered) {
            return RuleResult.match(chain.getId(), message != null ? message : "Rule chain triggered");
        } else {
            return RuleResult.noMatch(chain.getId(), "Rule chain not triggered", SeverityConstants.INFO);
        }
    }
}
