/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.mars.apex.engine.execution;

import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.sequential.ProcessingItem;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.engine.model.EnrichmentGroup;
import dev.mars.apex.engine.model.ExecutionStep;
import dev.mars.apex.engine.model.Rule;
import dev.mars.apex.engine.model.RuleGroup;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.engine.core.RulesEngineConfiguration;
import dev.mars.apex.core.config.RuleFactory;
import dev.mars.apex.core.config.pipeline.PipelineConfiguration;
import dev.mars.apex.core.config.EnrichmentGroupFactory;
import dev.mars.apex.core.service.enrichment.EnrichmentProcessor;
import dev.mars.apex.engine.core.ExpressionEvaluatorService;
import dev.mars.apex.engine.core.UnifiedRuleEvaluator;
import dev.mars.apex.core.service.transform.TransformationProcessor;
import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.core.util.EnabledFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

/**
 * Processes YAML configurations in document order.
 *
 * <p>Each item (rule, enrichment, rule group, transformation, rule chain) is processed
 * in the exact order it appears in the YAML file. This is the only processing mode in APEX —
 * the engine always respects the sequence of the actual file contents.</p>
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li>Execute items in document order</li>
 *   <li>Aggregate enriched data across all processing steps</li>
 *   <li>Track execution path with timing information</li>
 *   <li>Handle error recovery and fail-fast scenarios</li>
 *   <li>Coordinate with all specialized executors (enrichment, rule, chain, etc.)</li>
 * </ul>
 *
 * @author APEX Team
 * @since 2026-01-22
 */
public class SequentialProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SequentialProcessor.class);

    private final RulesEngineConfiguration configuration;
    private final EnrichmentProcessor enrichmentProcessor;
    private final ExpressionEvaluatorService evaluatorService;
    private final EnrichmentGroupExecutor enrichmentGroupExecutor;
    private final RuleGroupExecutor ruleGroupExecutor;
    private final RuleChainExecutor ruleChainExecutor;
    private final RuleFactory ruleFactory;
    private final TransformationProcessor transformationProcessor;
    private final Map<YamlRuleConfiguration, PreparedProcessingState> processingStateCache;
    private volatile YamlRuleConfiguration primaryProcessingConfig;
    private volatile PreparedProcessingState primaryProcessingState;

    /**
     * Constructs a new SequentialProcessor with required dependencies.
     * 
     * @param configuration The rules engine configuration containing registered rules, groups, etc.
     * @param enrichmentProcessor Processor for enrichments
     * @param unifiedEvaluator Evaluator for individual rules
     * @param evaluatorService Service for evaluating expressions
     * @param pipelineService Service for pipeline execution
     * @param enrichmentGroupExecutor Executor for enrichment groups
     * @param ruleGroupExecutor Executor for rule groups
     * @param ruleChainExecutor Executor for rule chains
     */
    public SequentialProcessor(
            RulesEngineConfiguration configuration,
            EnrichmentProcessor enrichmentProcessor,
            UnifiedRuleEvaluator unifiedEvaluator,
            ExpressionEvaluatorService evaluatorService,
            EnrichmentGroupExecutor enrichmentGroupExecutor,
            RuleGroupExecutor ruleGroupExecutor,
            RuleChainExecutor ruleChainExecutor) {
        this.configuration = configuration;
        this.enrichmentProcessor = enrichmentProcessor;
        this.evaluatorService = evaluatorService;
        this.enrichmentGroupExecutor = enrichmentGroupExecutor;
        this.ruleGroupExecutor = ruleGroupExecutor;
        this.ruleChainExecutor = ruleChainExecutor;
        this.ruleFactory = new RuleFactory();
        this.transformationProcessor = new TransformationProcessor(evaluatorService);
        this.processingStateCache = Collections.synchronizedMap(new IdentityHashMap<>());
    }

    /**
     * Evaluates a YAML configuration in document order.
     *
     * <p>Processes individual items in the exact order they appear in the YAML file.
     * The item order is always available — it is captured by {@code OrderedYamlParser}
     * during YAML loading.</p>
     *
     * @param yamlConfig The YAML configuration to evaluate
     * @param inputData The input data to process
     * @param executeRule Function to execute a single rule (method reference from RulesEngine)
     * @param executePipeline Function to execute a pipeline (method reference from RulesEngine)
     * @param createContext Function to create a SpEL evaluation context
     * @return RuleResult containing execution results, enriched data, and execution path
     */
    public RuleResult evaluateSequential(
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> inputData,
            Function<RuleExecutionContext, RuleResult> executeRule,
            Function<PipelineExecutionContext, RuleResult> executePipeline,
            Function<Map<String, Object>, org.springframework.expression.spel.support.StandardEvaluationContext> createContext) {

        List<String> failureMessages = new ArrayList<>();
        Map<String, Object> enrichedData = new HashMap<>(inputData);
        boolean overallSuccess = true;
        List<RuleResult> individualRuleResults = new ArrayList<>();
        List<ExecutionStep> executionPath = new ArrayList<>();

        logger.debug("evaluateSequential() entry - inputData keys: {}, inputData size: {}",
                    inputData.keySet(), inputData.size());
        logger.debug("evaluateSequential() config details - hasPipeline: {}",
                    yamlConfig.getPipeline() != null);

        try {
            List<ProcessingItem> itemOrder = yamlConfig.getItemOrder();

            if (itemOrder == null || itemOrder.isEmpty()) {
                itemOrder = synthesizeItemOrder(yamlConfig);
                if (!itemOrder.isEmpty()) {
                    logger.debug("Synthesized item order with {} items from config sections", itemOrder.size());
                }
            }

            if (!itemOrder.isEmpty()) {
                logger.debug("Processing {} items in document order", itemOrder.size());
                processItemOrder(
                    itemOrder, yamlConfig, enrichedData, failureMessages,
                    individualRuleResults, executionPath, executeRule, createContext);
            }

            // Execute pipeline if present (pipelines are not tracked in itemOrder)
            if (yamlConfig.getPipeline() != null) {
                logger.info("Processing pipeline: {}", yamlConfig.getPipeline().getName());
                logger.debug("Pipeline execution starting with enrichedData keys: {}", enrichedData.keySet());
                PipelineExecutionContext pipelineCtx = new PipelineExecutionContext(yamlConfig.getPipeline(), enrichedData);
                RuleResult pipelineResult = executePipeline.apply(pipelineCtx);

                if (pipelineResult.getExecutionPath() != null) {
                    executionPath.addAll(pipelineResult.getExecutionPath());
                }

                if (pipelineResult.isError()) {
                    overallSuccess = false;
                    failureMessages.add("Pipeline execution error: " + pipelineResult.getMessage());
                    logger.debug("Pipeline execution failed: {}", pipelineResult.getMessage());
                } else {
                    logger.debug("Pipeline execution completed successfully");
                }
            }

            // Check if there were any failures
            overallSuccess = failureMessages.isEmpty();

            // Return comprehensive result
            if (overallSuccess) {
                logger.debug("Sequential evaluation completed successfully with {} individual rule results", individualRuleResults.size());
                logger.debug("Final enriched data keys (success): {}", enrichedData.keySet());
                logger.debug("Execution path summary: {} steps executed", executionPath.size());
                RuleResult result = RuleResult.evaluationSuccess(enrichedData, "evaluation", "Sequential evaluation completed successfully", individualRuleResults)
                        .toBuilder().executionPath(executionPath).build();
                return result;
            } else {
                logger.debug("Sequential evaluation completed with {} failures", failureMessages.size());
                logger.debug("Final enriched data keys (failure): {}", enrichedData.keySet());
                logger.debug("Failure messages: {}", failureMessages);
                RuleResult result = RuleResult.builder()
                        .ruleName("evaluation")
                        .message("Sequential evaluation completed with failures")
                        .triggered(false)
                        .resultType(RuleResult.ResultType.ERROR)
                        .severity(SeverityConstants.ERROR)
                        .enrichedData(enrichedData)
                        .failureMessages(failureMessages)
                        .success(false)
                        .childResults(individualRuleResults)
                        .executionPath(executionPath)
                        .build();
                return result;
            }

        } catch (Exception e) {
            logger.error("[APEX-RULE-999] Sequential evaluation failed with exception: {}", e.getMessage());
            logger.debug("Full sequential evaluation exception details:", e);
            failureMessages.add("[APEX-RULE-999] Sequential evaluation failed: " + e.getMessage());
            return RuleResult.evaluationFailure(failureMessages, enrichedData, "evaluation", "Sequential evaluation failed", SeverityConstants.ERROR);
        }
    }

    /**
     * Synthesizes an item order from the config's available sections when no explicit
     * itemOrder exists (e.g., programmatic configs not loaded via OrderedYamlParser).
     * Items are added in a fixed section order: enrichments, rules, rule-groups,
     * enrichment-groups, rule-chains, transformations.
     */
    private List<ProcessingItem> synthesizeItemOrder(YamlRuleConfiguration yamlConfig) {
        List<ProcessingItem> items = new ArrayList<>();

        if (yamlConfig.getEnrichments() != null) {
            for (var e : yamlConfig.getEnrichments()) {
                if (e.getId() != null) {
                    items.add(new ProcessingItem("enrichments", e.getId(), e.getType(), e.getName()));
                }
            }
        }
        if (yamlConfig.getRules() != null) {
            for (var r : yamlConfig.getRules()) {
                if (r.getId() != null) {
                    items.add(new ProcessingItem("rules", r.getId()));
                }
            }
        }
        if (yamlConfig.getRuleGroups() != null) {
            for (var g : yamlConfig.getRuleGroups()) {
                if (g.getId() != null) {
                    items.add(new ProcessingItem("rule-groups", g.getId()));
                }
            }
        }
        if (yamlConfig.getEnrichmentGroups() != null) {
            for (var eg : yamlConfig.getEnrichmentGroups()) {
                if (eg.getId() != null) {
                    items.add(new ProcessingItem("enrichment-groups", eg.getId()));
                }
            }
        }
        if (yamlConfig.getRuleChains() != null) {
            for (var rc : yamlConfig.getRuleChains()) {
                if (rc.getId() != null) {
                    items.add(new ProcessingItem("rule-chains", rc.getId()));
                }
            }
        }
        if (yamlConfig.getTransformations() != null) {
            for (var t : yamlConfig.getTransformations()) {
                if (t.getId() != null) {
                    items.add(new ProcessingItem("transformations", t.getId()));
                }
            }
        }

        return items;
    }

    /**
     * Process items in document order.
     */
    private void processItemOrder(
            List<ProcessingItem> itemOrder,
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> enrichedData,
            List<String> failureMessages,
            List<RuleResult> individualRuleResults,
            List<ExecutionStep> executionPath,
            Function<RuleExecutionContext, RuleResult> executeRule,
            Function<Map<String, Object>, org.springframework.expression.spel.support.StandardEvaluationContext> createContext) {

        logger.debug("Processing {} items in document order", itemOrder.size());
        logger.debug("Item order details: {}", itemOrder.stream()
                    .map(i -> i.getItemId() + "(" + i.getSectionType() + ")")
                    .toList());

        // Clear rule results from any previous evaluation to prevent stale state
        if (enrichmentProcessor != null) {
            enrichmentProcessor.clearRuleResults();
        }

        PreparedProcessingState processingState = getProcessingState(yamlConfig);
        if (processingState.getPreparationFailureMessage() != null) {
            failureMessages.add(processingState.getPreparationFailureMessage());
        }

        for (ProcessingItem item : itemOrder) {
            logger.debug("Processing item: {} ({})", item.getItemId(), item.getSectionType());
            logger.debug("Current enrichedData keys before item '{}': {}", item.getItemId(), enrichedData.keySet());

            long start = System.currentTimeMillis();
            RuleResult itemResult = processItem(item, yamlConfig, enrichedData, executeRule, createContext,
                    processingState.getRuleIndex(),
                    processingState.getGroupIndex(),
                    processingState.getEnrichmentGroupIndex(),
                    processingState.getEnrichmentIndex(),
                    processingState.getTransformationIndex());
            long duration = System.currentTimeMillis() - start;

            logger.debug("Item '{}' completed in {}ms - success: {}, resultType: {}", 
                        item.getItemId(), duration, itemResult.isSuccess(), itemResult.getResultType());

            executionPath.add(new ExecutionStep(
                item.getItemId(), 
                item.getSectionType(), 
                itemResult.isSuccess() ? "SUCCESS" : "FAILURE", 
                itemResult.getMessage(), 
                duration
            ));

            // Collect individual rule results for rules section
            if ("rules".equals(item.getSectionType())) {
                individualRuleResults.add(itemResult);
            }

            // Check for ERROR result type
            if (itemResult.isError()) {
                failureMessages.add(item.getSectionType() + " '" + item.getItemId() + "' error: " + itemResult.getMessage());
            }

            // Update enriched data with results
            if (itemResult.getEnrichedData() != null) {
                enrichedData.putAll(itemResult.getEnrichedData());
            }
        }
    }

    /**
     * Process a single item based on its section type.
     */
    private RuleResult processItem(
            ProcessingItem item,
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> data,
            Function<RuleExecutionContext, RuleResult> executeRule,
            Function<Map<String, Object>, org.springframework.expression.spel.support.StandardEvaluationContext> createContext,
            Map<String, Rule> ruleIndex,
            Map<String, RuleGroup> groupIndex,
            Map<String, EnrichmentGroup> enrichmentGroupIndex,
            Map<String, YamlEnrichment> enrichmentIndex,
            Map<String, YamlTransformation> transformationIndex) {

        String sectionType = item.getSectionType();
        String itemId = item.getItemId();

        logger.debug("processItem called with sectionType='{}', itemId='{}'", sectionType, itemId);

        switch (sectionType) {
            case "enrichments":
                return processEnrichmentItem(itemId, yamlConfig, data, enrichmentIndex);
            case "rules":
                return processRuleItem(itemId, yamlConfig, data, executeRule, ruleIndex);
            case "enrichment-groups":
                return processEnrichmentGroupItem(itemId, yamlConfig, data, enrichmentGroupIndex);
            case "rule-groups":
                return processRuleGroupItem(itemId, yamlConfig, data, createContext, groupIndex);
            case "transformations":
                logger.debug("Matched transformations case, calling processTransformationItem");
                return processTransformationItem(itemId, yamlConfig, data, transformationIndex);
            case "rule-chains":
                return processRuleChainItem(itemId, yamlConfig, data, createContext, enrichmentGroupIndex);
            default:
                logger.error("Unknown section type: {}", sectionType);
                return RuleResult.error(sectionType + ":" + itemId, "Unknown section type");
        }
    }

    // ===================================
    // Individual Item Processors
    // ===================================

    /**
     * Process a single enrichment by ID using the pre-built enrichment index.
     */
    private RuleResult processEnrichmentItem(String enrichmentId, YamlRuleConfiguration yamlConfig, Map<String, Object> data,
                                             Map<String, YamlEnrichment> enrichmentIndex) {
        logger.debug("processEnrichmentItem() - looking up enrichment id: '{}' (indexed)", enrichmentId);
        YamlEnrichment enrichment = enrichmentIndex.get(enrichmentId);
        if (enrichment == null) {
            logger.warn("Enrichment not found: {}", enrichmentId);
            return RuleResult.error("enrichment:" + enrichmentId, "Enrichment not found");
        }

        // Check if enrichment is enabled
        if (!dev.mars.apex.core.util.EnabledFilter.isEnabled(enrichment)) {
            logger.info("Enrichment '{}' is disabled, skipping execution", enrichmentId);
            return RuleResult.noMatch(enrichmentId, "Enrichment is disabled", SeverityConstants.INFO);
        }

        logger.debug("processEnrichmentItem() - found enrichment '{}', type: {}", 
                    enrichmentId, enrichment.getType() != null ? enrichment.getType() : "default");
        RuleResult result = enrichmentProcessor.processEnrichmentWithResult(enrichment, data, yamlConfig);
        logger.debug("processEnrichmentItem() - enrichment '{}' completed with success={}", enrichmentId, result.isSuccess());
        return result;
    }

    /**
     * Process a single rule by ID.
     * Process a single rule by ID using the pre-built rule index.
     */
    private RuleResult processRuleItem(
            String ruleId,
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> data,
            Function<RuleExecutionContext, RuleResult> executeRule,
            Map<String, Rule> ruleIndex) {

        logger.debug("processRuleItem() - looking up rule id: '{}' (indexed)", ruleId);

        // Check if the rule is disabled at YAML level.
        // createRuleIndex() filters out disabled rules, so a missing index entry
        // for a rule that exists in the YAML means it was disabled, not missing.
        Rule rule = ruleIndex.get(ruleId);

        if (rule == null) {
            rule = configuration.getRuleById(ruleId);
            if (rule != null) {
                logger.debug("processRuleItem() - found rule '{}' in engine configuration", ruleId);
            }
        }

        if (rule == null) {
            // Before returning error, check if the rule exists but is disabled
            if (yamlConfig != null && yamlConfig.getRules() != null) {
                for (YamlRule yamlRule : yamlConfig.getRules()) {
                    if (ruleId.equals(yamlRule.getId()) && !EnabledFilter.isEnabled(yamlRule)) {
                        logger.info("Rule '{}' is disabled, skipping execution", ruleId);
                        return RuleResult.noMatch(ruleId, "Rule is disabled", SeverityConstants.INFO);
                    }
                }
            }
            logger.warn("Rule not found: {}", ruleId);
            return RuleResult.error("rule:" + ruleId, "Rule not found");
        }

        logger.debug("processRuleItem() - executing rule '{}' with condition: {}", ruleId, rule.getCondition());
        RuleExecutionContext ctx = new RuleExecutionContext(rule, data);
        RuleResult result = executeRule.apply(ctx);
        logger.debug("processRuleItem() - rule '{}' completed - success={}, resultType={}", 
                    ruleId, result.isSuccess(), result.getResultType());

        // Store individual rule result for conditional mapping
        if (enrichmentProcessor != null) {
            boolean passed = result.isSuccess() && result.isTriggered();
            enrichmentProcessor.storeIndividualRuleResult(ruleId, passed);
            logger.debug("Stored individual rule result for '{}': passed={}", ruleId, passed);
        }

        return result;
    }

    /**
     * Process a single enrichment group by ID using the pre-built enrichment group index.
     *
     * <p>Phase 7 optimisation: uses the cached enrichment group index built once in
     * {@code processItemOrder()} instead of calling {@code EnrichmentGroupFactory.buildEnrichmentGroups()}
     * per item — eliminates O(n×m) factory rebuilds.</p>
     */
    private RuleResult processEnrichmentGroupItem(String groupId, YamlRuleConfiguration yamlConfig, Map<String, Object> data,
                                                  Map<String, EnrichmentGroup> enrichmentGroupIndex) {
        logger.debug("processEnrichmentGroupItem() - looking up enrichment group id: '{}' (indexed)", groupId);
        EnrichmentGroup group = enrichmentGroupIndex.get(groupId);

        if (group != null) {
            logger.debug("processEnrichmentGroupItem() - found enrichment group '{}' with {} enrichments",
                        groupId, group.getEnrichmentsInOrder().size());
        } else {
            // Fallback: check engine configuration (for programmatic registrations)
            group = configuration.getEnrichmentGroupById(groupId);
            if (group != null) {
                logger.debug("processEnrichmentGroupItem() - found enrichment group '{}' in engine configuration", groupId);
            }
        }

        if (group == null) {
            logger.warn("Enrichment group not found: {}", groupId);
            return RuleResult.error("enrichment-group:" + groupId, "Enrichment group not found");
        }

        RuleResult result = enrichmentGroupExecutor.executeEnrichmentGroupsList(List.of(group), data, yamlConfig);
        logger.debug("processEnrichmentGroupItem() - enrichment group '{}' completed with success={}", groupId, result.isSuccess());
        return result;
    }

    /**
     * Process a single rule group by ID.
     * Process a single rule group by ID using the pre-built group index.
     */
    private RuleResult processRuleGroupItem(String groupId, YamlRuleConfiguration yamlConfig, Map<String, Object> data,
                                              Function<Map<String, Object>, org.springframework.expression.spel.support.StandardEvaluationContext> createContext,
                                              Map<String, RuleGroup> groupIndex) {
        logger.debug("processRuleGroupItem() - looking up rule group id: '{}' (indexed)", groupId);
        RuleGroup group = groupIndex.get(groupId);

        if (group == null) {
            group = configuration.getRuleGroupById(groupId);
            if (group != null) {
                logger.debug("processRuleGroupItem() - found rule group '{}' in engine configuration", groupId);
            }
        }

        if (group == null) {
            // Before returning error, check if the group exists but is disabled
            if (yamlConfig != null && yamlConfig.getRuleGroups() != null) {
                for (YamlRuleGroup yamlGroup : yamlConfig.getRuleGroups()) {
                    if (groupId.equals(yamlGroup.getId()) && !EnabledFilter.isEnabled(yamlGroup)) {
                        logger.info("Rule group '{}' is disabled, skipping execution", groupId);
                        return RuleResult.noMatch(groupId, "Rule group is disabled", SeverityConstants.INFO);
                    }
                }
            }
            logger.warn("Rule group not found: {}", groupId);
            return RuleResult.error("rule-group:" + groupId, "Rule group not found");
        }

        logger.debug("processRuleGroupItem() - executing rule group '{}' with {} rules", 
                    groupId, group.getRules() != null ? group.getRules().size() : 0);
        RuleResult result = ruleGroupExecutor.executeRuleGroupsList(List.of(group), data, createContext.apply(data));
        logger.debug("processRuleGroupItem() - rule group '{}' completed - success={}, resultType={}", 
                    groupId, result.isSuccess(), result.getResultType());

        // Store rule group results for conditional mapping
        // Extract individual rule results from the RuleResult's enrichedData
        if (enrichmentProcessor != null) {
            boolean passed = result.isSuccess() && result.isTriggered();
            @SuppressWarnings("unchecked")
            Map<String, Boolean> ruleResults = (Map<String, Boolean>) result.getEnrichedData()
                    .getOrDefault("_individualRuleResults", new HashMap<>());
            enrichmentProcessor.storeRuleGroupResult(groupId, passed, ruleResults);
            logger.debug("Stored rule group result for '{}': passed={}, ruleResults={}", groupId, passed, ruleResults);
        }

        return result;
    }

    /**
     * Process a single transformation by ID using the pre-built transformation index.
     */
    private RuleResult processTransformationItem(String transformationId, YamlRuleConfiguration yamlConfig, Map<String, Object> data,
                                                 Map<String, YamlTransformation> transformationIndex) {
        logger.debug("processTransformationItem() - looking up transformation id: '{}' (indexed)", transformationId);
        YamlTransformation transformation = transformationIndex.get(transformationId);
        if (transformation == null) {
            logger.warn("Transformation not found: {}", transformationId);
            return RuleResult.error("transformation:" + transformationId, "Transformation not found");
        }

        logger.debug("processTransformationItem() - found transformation '{}', executing with {} data keys", 
                    transformationId, data.size());
        RuleResult transformationResult = transformationProcessor.processTransformationsWithResult(List.of(transformation), data);

        if (transformationResult.isError()) {
            logger.error("Transformation processing failed: {}", transformationResult.getMessage());
        } else {
            logger.debug("processTransformationItem() - transformation '{}' completed successfully", transformationId);
        }

        return transformationResult;
    }

    /**
     * Process a single rule chain by ID.
     */
    private RuleResult processRuleChainItem(String chainId, YamlRuleConfiguration yamlConfig, Map<String, Object> data,
                                            Function<Map<String, Object>, org.springframework.expression.spel.support.StandardEvaluationContext> contextFactory,
                                            Map<String, EnrichmentGroup> enrichmentGroupIndex) {
        logger.debug("processRuleChainItem() - executing rule chain id: '{}'", chainId);
        RuleResult result = ruleChainExecutor.processRuleChain(chainId, yamlConfig, data, contextFactory, enrichmentGroupIndex);
        logger.debug("processRuleChainItem() - rule chain '{}' completed - success={}, resultType={}", 
                    chainId, result.isSuccess(), result.getResultType());
        return result;
    }

    // ===================================
    // Index Builders
    // ===================================

    /**
     * Build an enrichment group index from YAML configuration.
     * Calls {@link EnrichmentGroupFactory#buildEnrichmentGroups(YamlRuleConfiguration)} once
     * and indexes the results by ID for O(1) lookup.
     *
     * @param yamlConfig The YAML configuration
     * @return Map of enrichment group ID → EnrichmentGroup
     */
    private Map<String, EnrichmentGroup> buildEnrichmentGroupIndex(YamlRuleConfiguration yamlConfig) {
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(yamlConfig);
        Map<String, EnrichmentGroup> index = new HashMap<>();
        for (EnrichmentGroup g : groups) {
            index.put(g.getId(), g);
        }
        logger.debug("Built enrichment group index with {} entries", index.size());
        return index;
    }

    /**
     * Build an enrichment index from YAML configuration for O(1) lookup by ID.
     *
     * @param yamlConfig The YAML configuration
     * @return Map of enrichment ID → YamlEnrichment
     */
    private Map<String, YamlEnrichment> buildEnrichmentIndex(YamlRuleConfiguration yamlConfig) {
        Map<String, YamlEnrichment> index = new HashMap<>();
        if (yamlConfig.getEnrichments() != null) {
            for (YamlEnrichment enrichment : yamlConfig.getEnrichments()) {
                if (enrichment.getId() != null) {
                    index.put(enrichment.getId(), enrichment);
                }
            }
        }
        logger.debug("Built enrichment index with {} entries", index.size());
        return index;
    }

    /**
     * Build a transformation index from YAML configuration for O(1) lookup by ID.
     *
     * @param yamlConfig The YAML configuration
     * @return Map of transformation ID → YamlTransformation
     */
    private Map<String, YamlTransformation> buildTransformationIndex(YamlRuleConfiguration yamlConfig) {
        Map<String, YamlTransformation> index = new HashMap<>();
        if (yamlConfig.getTransformations() != null) {
            for (YamlTransformation transformation : yamlConfig.getTransformations()) {
                if (transformation.getId() != null) {
                    index.put(transformation.getId(), transformation);
                }
            }
        }
        logger.debug("Built transformation index with {} entries", index.size());
        return index;
    }

    private PreparedProcessingState getProcessingState(YamlRuleConfiguration yamlConfig) {
        if (yamlConfig == null) {
            return PreparedProcessingState.empty();
        }

        PreparedProcessingState fastPathState = primaryProcessingState;
        if (yamlConfig == primaryProcessingConfig && fastPathState != null) {
            return fastPathState;
        }

        synchronized (processingStateCache) {
            PreparedProcessingState cachedState = processingStateCache.get(yamlConfig);
            if (cachedState != null) {
                if (primaryProcessingConfig == null) {
                    primaryProcessingConfig = yamlConfig;
                    primaryProcessingState = cachedState;
                }
                return cachedState;
            }
        }

        PreparedProcessingState preparedState = prepareProcessingState(yamlConfig);

        synchronized (processingStateCache) {
            PreparedProcessingState cachedState = processingStateCache.get(yamlConfig);
            if (cachedState != null) {
                if (primaryProcessingConfig == null) {
                    primaryProcessingConfig = yamlConfig;
                    primaryProcessingState = cachedState;
                }
                return cachedState;
            }
            processingStateCache.put(yamlConfig, preparedState);
            if (primaryProcessingConfig == null) {
                primaryProcessingConfig = yamlConfig;
                primaryProcessingState = preparedState;
            }
            return preparedState;
        }
    }

    private PreparedProcessingState prepareProcessingState(YamlRuleConfiguration yamlConfig) {
        Map<String, Rule> ruleIndex = ruleFactory.createRuleIndex(yamlConfig);
        Map<String, RuleGroup> groupIndex;
        String preparationFailureMessage = null;

        try {
            RulesEngineConfiguration tempConfig = new RulesEngineConfiguration();
            for (Rule rule : ruleIndex.values()) {
                tempConfig.registerRule(rule);
            }
            groupIndex = ruleFactory.createRuleGroupIndex(yamlConfig, tempConfig);
        } catch (ConfigurationException e) {
            logger.error("[APEX-CFG-003] Failed to build rule-group index: {}", e.getMessage());
            logger.debug("Full exception details for rule-group index build failure:", e);
            preparationFailureMessage = "[APEX-CFG-003] Rule-group index build failed: " + e.getMessage();
            groupIndex = Map.of();
        }

        Map<String, EnrichmentGroup> enrichmentGroupIndex = buildEnrichmentGroupIndex(yamlConfig);
        Map<String, YamlEnrichment> enrichmentIndex = buildEnrichmentIndex(yamlConfig);
        Map<String, YamlTransformation> transformationIndex = buildTransformationIndex(yamlConfig);

        logger.debug(
                "Prepared processing state for config {} with {} rules, {} rule groups, {} enrichment groups, {} enrichments, {} transformations",
                System.identityHashCode(yamlConfig),
                ruleIndex.size(),
                groupIndex.size(),
                enrichmentGroupIndex.size(),
                enrichmentIndex.size(),
                transformationIndex.size());

        return new PreparedProcessingState(
                Collections.unmodifiableMap(new LinkedHashMap<>(ruleIndex)),
                Collections.unmodifiableMap(new LinkedHashMap<>(groupIndex)),
                Collections.unmodifiableMap(new LinkedHashMap<>(enrichmentGroupIndex)),
                Collections.unmodifiableMap(new LinkedHashMap<>(enrichmentIndex)),
                Collections.unmodifiableMap(new LinkedHashMap<>(transformationIndex)),
                preparationFailureMessage);
    }

    private static final class PreparedProcessingState {
        private static final PreparedProcessingState EMPTY = new PreparedProcessingState(
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                null);

        private final Map<String, Rule> ruleIndex;
        private final Map<String, RuleGroup> groupIndex;
        private final Map<String, EnrichmentGroup> enrichmentGroupIndex;
        private final Map<String, YamlEnrichment> enrichmentIndex;
        private final Map<String, YamlTransformation> transformationIndex;
        private final String preparationFailureMessage;

        private PreparedProcessingState(
                Map<String, Rule> ruleIndex,
                Map<String, RuleGroup> groupIndex,
                Map<String, EnrichmentGroup> enrichmentGroupIndex,
                Map<String, YamlEnrichment> enrichmentIndex,
                Map<String, YamlTransformation> transformationIndex,
                String preparationFailureMessage) {
            this.ruleIndex = ruleIndex;
            this.groupIndex = groupIndex;
            this.enrichmentGroupIndex = enrichmentGroupIndex;
            this.enrichmentIndex = enrichmentIndex;
            this.transformationIndex = transformationIndex;
            this.preparationFailureMessage = preparationFailureMessage;
        }

        private static PreparedProcessingState empty() {
            return EMPTY;
        }

        private Map<String, Rule> getRuleIndex() {
            return ruleIndex;
        }

        private Map<String, RuleGroup> getGroupIndex() {
            return groupIndex;
        }

        private Map<String, EnrichmentGroup> getEnrichmentGroupIndex() {
            return enrichmentGroupIndex;
        }

        private Map<String, YamlEnrichment> getEnrichmentIndex() {
            return enrichmentIndex;
        }

        private Map<String, YamlTransformation> getTransformationIndex() {
            return transformationIndex;
        }

        private String getPreparationFailureMessage() {
            return preparationFailureMessage;
        }
    }

    // ===================================
    // Context Classes for Function References
    // ===================================

    /**
     * Context for rule execution via method reference.
     */
    public static class RuleExecutionContext {
        private final Rule rule;
        private final Map<String, Object> data;

        public RuleExecutionContext(Rule rule, Map<String, Object> data) {
            this.rule = rule;
            this.data = data;
        }

        public Rule getRule() {
            return rule;
        }

        public Map<String, Object> getData() {
            return data;
        }
    }

    /**
     * Context for pipeline execution via method reference.
     */
    public static class PipelineExecutionContext {
        private final PipelineConfiguration pipeline;
        private final Map<String, Object> data;

        public PipelineExecutionContext(PipelineConfiguration pipeline, Map<String, Object> data) {
            this.pipeline = pipeline;
            this.data = data;
        }

        public PipelineConfiguration getPipeline() {
            return pipeline;
        }

        public Map<String, Object> getData() {
            return data;
        }
    }
}
