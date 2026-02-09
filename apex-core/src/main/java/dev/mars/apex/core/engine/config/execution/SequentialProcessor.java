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
package dev.mars.apex.core.engine.config.execution;

import dev.mars.apex.core.config.*;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.sequential.ProcessingItem;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.config.YamlRuleFactory;
import dev.mars.apex.core.config.pipeline.PipelineConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentGroupFactory;
import dev.mars.apex.core.service.enrichment.YamlEnrichmentProcessor;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.engine.UnifiedRuleEvaluator;
import dev.mars.apex.core.service.transform.YamlTransformationProcessor;
import dev.mars.apex.core.config.exception.YamlConfigurationException;
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
    private final YamlEnrichmentProcessor enrichmentProcessor;
    private final ExpressionEvaluatorService evaluatorService;
    private final EnrichmentGroupExecutor enrichmentGroupExecutor;
    private final RuleGroupExecutor ruleGroupExecutor;
    private final RuleChainExecutor ruleChainExecutor;
    private final YamlRuleFactory ruleFactory;

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
            YamlEnrichmentProcessor enrichmentProcessor,
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
        this.ruleFactory = new YamlRuleFactory();
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
                    logger.info("Synthesized item order with {} items from config sections", itemOrder.size());
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

                if (pipelineResult.getResultType() == RuleResult.ResultType.ERROR) {
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
                logger.info("Sequential evaluation completed successfully with {} individual rule results", individualRuleResults.size());
                logger.debug("Final enriched data keys (success): {}", enrichedData.keySet());
                logger.debug("Execution path summary: {} steps executed", executionPath.size());
                RuleResult result = RuleResult.evaluationSuccess(enrichedData, "evaluation", "Sequential evaluation completed successfully", individualRuleResults);
                result.setExecutionPath(executionPath);
                return result;
            } else {
                logger.info("Sequential evaluation completed with {} failures", failureMessages.size());
                logger.debug("Final enriched data keys (failure): {}", enrichedData.keySet());
                logger.debug("Failure messages: {}", failureMessages);
                RuleResult result = new RuleResult("evaluation", "Sequential evaluation completed with failures",
                                     false, RuleResult.ResultType.ERROR, enrichedData, failureMessages, false, individualRuleResults);
                result.setExecutionPath(executionPath);
                return result;
            }

        } catch (Exception e) {
            logger.error("Sequential evaluation failed with exception: {}", e.getMessage());
            logger.debug("Full sequential evaluation exception details:", e);
            failureMessages.add("Sequential evaluation failed: " + e.getMessage());
            return RuleResult.evaluationFailure(failureMessages, enrichedData, "evaluation", "Sequential evaluation failed");
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

        logger.info("Processing {} items in document order", itemOrder.size());
        logger.debug("Item order details: {}", itemOrder.stream()
                    .map(i -> i.getItemId() + "(" + i.getSectionType() + ")")
                    .toList());

        // Phase 4 optimisation: build lookup maps once for O(1) access during item processing
        Map<String, Rule> ruleIndex = ruleFactory.createRuleIndex(yamlConfig);
        Map<String, RuleGroup> groupIndex;
        try {
            RulesEngineConfiguration tempConfig = new RulesEngineConfiguration();
            for (Rule r : ruleIndex.values()) {
                tempConfig.registerRule(r);
            }
            groupIndex = ruleFactory.createRuleGroupIndex(yamlConfig, tempConfig);
        } catch (YamlConfigurationException e) {
            logger.error("Failed to build rule-group index: {}", e.getMessage());
            groupIndex = Map.of();
        }

        for (ProcessingItem item : itemOrder) {
            logger.debug("Processing item: {} ({})", item.getItemId(), item.getSectionType());
            logger.debug("Current enrichedData keys before item '{}': {}", item.getItemId(), enrichedData.keySet());

            long start = System.currentTimeMillis();
            RuleResult itemResult = processItem(item, yamlConfig, enrichedData, executeRule, createContext, ruleIndex, groupIndex);
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
            if (itemResult.getResultType() == RuleResult.ResultType.ERROR) {
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
            Map<String, RuleGroup> groupIndex) {

        String sectionType = item.getSectionType();
        String itemId = item.getItemId();

        logger.debug("processItem called with sectionType='{}', itemId='{}'", sectionType, itemId);

        switch (sectionType) {
            case "enrichments":
                return processEnrichmentItem(itemId, yamlConfig, data);
            case "rules":
                return processRuleItem(itemId, yamlConfig, data, executeRule, ruleIndex);
            case "enrichment-groups":
                return processEnrichmentGroupItem(itemId, yamlConfig, data);
            case "rule-groups":
                return processRuleGroupItem(itemId, yamlConfig, data, createContext, groupIndex);
            case "transformations":
                logger.debug("Matched transformations case, calling processTransformationItem");
                return processTransformationItem(itemId, yamlConfig, data);
            case "rule-chains":
                return processRuleChainItem(itemId, yamlConfig, data, createContext);
            default:
                logger.error("Unknown section type: {}", sectionType);
                return RuleResult.error(sectionType + ":" + itemId, "Unknown section type");
        }
    }

    // ===================================
    // Individual Item Processors
    // ===================================

    /**
     * Process a single enrichment by ID.
     */
    private RuleResult processEnrichmentItem(String enrichmentId, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
        logger.debug("processEnrichmentItem() - looking up enrichment id: '{}'", enrichmentId);
        YamlEnrichment enrichment = findEnrichmentById(yamlConfig, enrichmentId);
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
     * Process a single enrichment group by ID.
     */
    private RuleResult processEnrichmentGroupItem(String groupId, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
        logger.debug("processEnrichmentGroupItem() - looking up enrichment group id: '{}'", groupId);
        EnrichmentGroup group = null;
        if (yamlConfig != null && yamlConfig.getEnrichmentGroups() != null) {
            for (YamlEnrichmentGroup yamlGroup : yamlConfig.getEnrichmentGroups()) {
                if (groupId.equals(yamlGroup.getId())) {
                    List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(yamlConfig);
                    for (EnrichmentGroup g : groups) {
                        if (groupId.equals(g.getId())) {
                            group = g;
                            logger.debug("processEnrichmentGroupItem() - found enrichment group '{}' with {} enrichments", 
                                        groupId, g.getEnrichmentsInOrder().size());
                            break;
                        }
                    }
                    break;
                }
            }
        }

        if (group == null) {
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
     */
    private RuleResult processTransformationItem(String transformationId, YamlRuleConfiguration yamlConfig, Map<String, Object> data) {
        logger.debug("processTransformationItem() - looking up transformation id: '{}'", transformationId);
        YamlTransformation transformation = findTransformationById(yamlConfig, transformationId);
        if (transformation == null) {
            logger.warn("Transformation not found: {}", transformationId);
            return RuleResult.error("transformation:" + transformationId, "Transformation not found");
        }

        logger.debug("processTransformationItem() - found transformation '{}', executing with {} data keys", 
                    transformationId, data.size());
        YamlTransformationProcessor processor = new YamlTransformationProcessor(this.evaluatorService);
        RuleResult transformationResult = processor.processTransformationsWithResult(List.of(transformation), data);

        if (transformationResult.getResultType() == RuleResult.ResultType.ERROR) {
            logger.error("Transformation processing failed: {}", transformationResult.getMessage());
        } else {
            logger.debug("processTransformationItem() - transformation '{}' completed successfully", transformationId);
        }

        return transformationResult;
    }

    /**
     * Process a single rule chain by ID.
     */
    private RuleResult processRuleChainItem(String chainId, YamlRuleConfiguration yamlConfig, Map<String, Object> data, Function<Map<String, Object>, org.springframework.expression.spel.support.StandardEvaluationContext> contextFactory) {
        logger.debug("processRuleChainItem() - executing rule chain id: '{}'", chainId);
        RuleResult result = ruleChainExecutor.processRuleChain(chainId, yamlConfig, data, contextFactory);
        logger.debug("processRuleChainItem() - rule chain '{}' completed - success={}, resultType={}", 
                    chainId, result.isSuccess(), result.getResultType());
        return result;
    }

    // ===================================
    // Helper Methods
    // ===================================

    /**
     * Find a transformation by ID in the configuration.
     */
    private YamlTransformation findTransformationById(YamlRuleConfiguration config, String transformationId) {
        if (config.getTransformations() != null) {
            for (YamlTransformation transformation : config.getTransformations()) {
                if (transformationId.equals(transformation.getId())) {
                    return transformation;
                }
            }
        }
        return null;
    }

    /**
     * Find an enrichment by ID in the configuration.
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
