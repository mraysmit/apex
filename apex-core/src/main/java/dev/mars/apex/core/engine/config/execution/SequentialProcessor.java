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

import dev.mars.apex.core.config.yaml.*;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;
import dev.mars.apex.core.config.pipeline.PipelineConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentGroupFactory;
import dev.mars.apex.core.service.enrichment.YamlEnrichmentProcessor;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.engine.UnifiedRuleEvaluator;
import dev.mars.apex.core.service.transformation.YamlTransformationProcessor;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

/**
 * Handles sequential processing of YAML configurations in document order.
 * 
 * <p>This processor supports two processing modes:</p>
 * <ul>
 *   <li><b>Item-level processing (APEX 2.1+):</b> Process individual items (rules, enrichments, etc.) 
 *       in the exact order they appear in the YAML document using itemOrder.</li>
 *   <li><b>Section-level processing (Legacy):</b> Process entire sections (enrichments, rules, etc.) 
 *       in sectionOrder when itemOrder is not available.</li>
 * </ul>
 * 
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li>Execute items/sections in document order</li>
 *   <li>Aggregate enriched data across all processing steps</li>
 *   <li>Track execution path with timing information</li>
 *   <li>Handle error recovery and fail-fast scenarios</li>
 *   <li>Coordinate with all specialized executors (enrichment, rule, chain, etc.)</li>
 * </ul>
 * 
 * <p>This class is part of the APEX 2.1 refactoring to decompose the monolithic RulesEngine
 * into focused, single-responsibility executors.</p>
 * 
 * @author APEX Team
 * @since 2.1
 */
public class SequentialProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SequentialProcessor.class);

    private final RulesEngineConfiguration configuration;
    private final YamlEnrichmentProcessor enrichmentProcessor;
    private final ExpressionEvaluatorService evaluatorService;
    private final EnrichmentGroupExecutor enrichmentGroupExecutor;
    private final RuleGroupExecutor ruleGroupExecutor;
    private final RuleChainExecutor ruleChainExecutor;

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
    }

    /**
     * Evaluates a YAML configuration in sequential order.
     * 
     * <p>If item-level order is available, processes individual items in document order.
     * Otherwise, falls back to section-level processing.</p>
     * 
     * @param yamlConfig The YAML configuration to evaluate
     * @param inputData The input data to process
     * @param sectionOrder The order of sections from the YAML document (used for fallback)
     * @param executeRule Function to execute a single rule (method reference from RulesEngine)
     * @param executePipeline Function to execute a pipeline (method reference from RulesEngine)
     * @return RuleResult containing execution results, enriched data, and execution path
     */
    public RuleResult evaluateSequential(
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> inputData,
            List<String> sectionOrder,
            Function<RuleExecutionContext, RuleResult> executeRule,
            Function<PipelineExecutionContext, RuleResult> executePipeline,
            Function<Map<String, Object>, org.springframework.expression.spel.support.StandardEvaluationContext> createContext) {

        List<String> failureMessages = new ArrayList<>();
        Map<String, Object> enrichedData = new HashMap<>(inputData);
        boolean overallSuccess = true;
        List<RuleResult> individualRuleResults = new ArrayList<>();
        List<ExecutionStep> executionPath = new ArrayList<>();

        // Store createContext for use in process methods (especially processRuleChainItem)
        final var contextFactory = createContext;

        try {
            // Check if item-level order is available
            List<ProcessingItem> itemOrder = yamlConfig.getItemOrder();

            if (itemOrder != null && !itemOrder.isEmpty()) {
                // Item-level processing (APEX 2.1+)
                processItemOrder(
                    itemOrder, yamlConfig, enrichedData, failureMessages, 
                    individualRuleResults, executionPath, executeRule, createContext);

                // Execute pipeline if present (since it's not an item in itemOrder)
                if (yamlConfig.getPipeline() != null) {
                    logger.info("Processing pipeline (post-item-processing): {}", yamlConfig.getPipeline().getName());
                    PipelineExecutionContext pipelineCtx = new PipelineExecutionContext(yamlConfig.getPipeline(), enrichedData);
                    RuleResult pipelineResult = executePipeline.apply(pipelineCtx);

                    if (pipelineResult.getExecutionPath() != null) {
                        executionPath.addAll(pipelineResult.getExecutionPath());
                    }

                    if (pipelineResult.getResultType() == RuleResult.ResultType.ERROR) {
                        overallSuccess = false;
                        failureMessages.add("Pipeline execution error: " + pipelineResult.getMessage());
                    }
                }
            } else {
                // Section-level processing (Legacy fallback)
                processSectionOrder(
                    sectionOrder, yamlConfig, enrichedData, failureMessages, 
                    individualRuleResults, executionPath, executeRule, executePipeline, contextFactory);
            }

            // Check if there were any failures
            overallSuccess = failureMessages.isEmpty();

            // Return comprehensive result
            if (overallSuccess) {
                logger.info("Sequential evaluation completed successfully with {} individual rule results", individualRuleResults.size());
                RuleResult result = RuleResult.evaluationSuccess(enrichedData, "evaluation", "Sequential evaluation completed successfully", individualRuleResults);
                result.setExecutionPath(executionPath);
                return result;
            } else {
                logger.info("Sequential evaluation completed with {} failures", failureMessages.size());
                logger.debug("Final enriched data keys (failure): {}", enrichedData.keySet());
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
     * Process items in document order (APEX 2.1+).
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

        for (ProcessingItem item : itemOrder) {
            logger.debug("Processing item: {} ({})", item.getItemId(), item.getSectionType());

            long start = System.currentTimeMillis();
            RuleResult itemResult = processItem(item, yamlConfig, enrichedData, executeRule, createContext);
            long duration = System.currentTimeMillis() - start;

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
     * Process sections in document order (Legacy fallback).
     */
    private void processSectionOrder(
            List<String> sectionOrder,
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> enrichedData,
            List<String> failureMessages,
            List<RuleResult> individualRuleResults,
            List<ExecutionStep> executionPath,
            Function<RuleExecutionContext, RuleResult> executeRule,
            Function<PipelineExecutionContext, RuleResult> executePipeline,
            Function<Map<String, Object>, org.springframework.expression.spel.support.StandardEvaluationContext> contextFactory) {

        logger.info("No item order available, falling back to section-level processing");
        logger.info("Processing {} sections in document order", sectionOrder.size());

        for (String section : sectionOrder) {
            logger.debug("Processing section: {}", section);
            long sectionStart = System.currentTimeMillis();
            String sectionStatus = "SUCCESS";
            String sectionMessage = "Section processed";

            switch (section) {
                case "enrichments":
                    processEnrichmentsSection(yamlConfig, enrichedData, failureMessages, sectionStart, executionPath);
                    break;

                case "rules":
                    processRulesSection(yamlConfig, enrichedData, failureMessages, individualRuleResults, executeRule);
                    break;

                case "rule-groups":
                    processRuleGroupsSection(yamlConfig, enrichedData, failureMessages, contextFactory);
                    break;

                case "enrichment-groups":
                    processEnrichmentGroupsSection(yamlConfig, enrichedData, failureMessages);
                    break;

                case "pipeline":
                    processPipelineSection(yamlConfig, enrichedData, failureMessages, executionPath, executePipeline);
                    break;

                case "rule-chains":
                    processRuleChainsSection(yamlConfig, enrichedData, failureMessages, contextFactory);
                    break;

                case "transformations":
                    processTransformationsSection(yamlConfig, enrichedData, failureMessages, sectionStart, executionPath);
                    break;

                case "metadata":
                case "data-sources":
                case "data-source-refs":
                case "rule-refs":
                case "enrichment-refs":
                case "data-sinks":
                case "categories":
                case "error-recovery":
                    // Configuration sections - not executed
                    logger.debug("Skipping configuration section: {}", section);
                    break;

                default:
                    logger.warn("Unknown section encountered during sequential processing: {}", section);
                    break;
            }

            long duration = System.currentTimeMillis() - sectionStart;
            // Only record step if it wasn't a skipped configuration section
            if (!isConfigurationSection(section)) {
                executionPath.add(new ExecutionStep(section, "SECTION", sectionStatus, sectionMessage, duration));
            }
        }
    }

    /**
     * Check if a section is a configuration section (not executable).
     */
    private boolean isConfigurationSection(String section) {
        return section.equals("metadata") || section.equals("data-sources") || 
               section.equals("data-source-refs") || section.equals("rule-refs") || 
               section.equals("enrichment-refs") || section.equals("data-sinks") || 
               section.equals("categories") || section.equals("error-recovery");
    }

    /**
     * Process a single item based on its section type.
     */
    private RuleResult processItem(
            ProcessingItem item,
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> data,
            Function<RuleExecutionContext, RuleResult> executeRule,
            Function<Map<String, Object>, org.springframework.expression.spel.support.StandardEvaluationContext> createContext) {

        String sectionType = item.getSectionType();
        String itemId = item.getItemId();

        logger.debug("processItem called with sectionType='{}', itemId='{}'", sectionType, itemId);

        switch (sectionType) {
            case "enrichments":
                return processEnrichmentItem(itemId, yamlConfig, data);
            case "rules":
                return processRuleItem(itemId, yamlConfig, data, executeRule);
            case "enrichment-groups":
                return processEnrichmentGroupItem(itemId, yamlConfig, data);
            case "rule-groups":
                return processRuleGroupItem(itemId, yamlConfig, data, createContext);
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
        YamlEnrichment enrichment = findEnrichmentById(yamlConfig, enrichmentId);
        if (enrichment == null) {
            logger.warn("Enrichment not found: {}", enrichmentId);
            return RuleResult.error("enrichment:" + enrichmentId, "Enrichment not found");
        }

        return enrichmentProcessor.processEnrichmentWithResult(enrichment, data, yamlConfig);
    }

    /**
     * Process a single rule by ID.
     */
    private RuleResult processRuleItem(
            String ruleId,
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> data,
            Function<RuleExecutionContext, RuleResult> executeRule) {

        Rule rule = null;
        if (yamlConfig != null && yamlConfig.getRules() != null) {
            for (YamlRule yamlRule : yamlConfig.getRules()) {
                if (ruleId.equals(yamlRule.getId())) {
                    YamlRuleFactory ruleFactory = new YamlRuleFactory();
                    rule = ruleFactory.createRuleWithMetadata(yamlRule);
                    break;
                }
            }
        }

        if (rule == null) {
            rule = configuration.getRuleById(ruleId);
        }

        if (rule == null) {
            logger.warn("Rule not found: {}", ruleId);
            return RuleResult.error("rule:" + ruleId, "Rule not found");
        }

        RuleExecutionContext ctx = new RuleExecutionContext(rule, data);
        RuleResult result = executeRule.apply(ctx);

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
        EnrichmentGroup group = null;
        if (yamlConfig != null && yamlConfig.getEnrichmentGroups() != null) {
            for (YamlEnrichmentGroup yamlGroup : yamlConfig.getEnrichmentGroups()) {
                if (groupId.equals(yamlGroup.getId())) {
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

        if (group == null) {
            group = configuration.getEnrichmentGroupById(groupId);
        }

        if (group == null) {
            logger.warn("Enrichment group not found: {}", groupId);
            return RuleResult.error("enrichment-group:" + groupId, "Enrichment group not found");
        }

        return enrichmentGroupExecutor.executeEnrichmentGroupsList(List.of(group), data, yamlConfig);
    }

    /**
     * Process a single rule group by ID.
     */
    private RuleResult processRuleGroupItem(String groupId, YamlRuleConfiguration yamlConfig, Map<String, Object> data,
                                              Function<Map<String, Object>, org.springframework.expression.spel.support.StandardEvaluationContext> createContext) {
        RuleGroup group = null;
        if (yamlConfig != null && yamlConfig.getRuleGroups() != null) {
            for (YamlRuleGroup yamlGroup : yamlConfig.getRuleGroups()) {
                if (groupId.equals(yamlGroup.getId())) {
                    try {
                        YamlRuleFactory ruleFactory = new YamlRuleFactory();
                        RulesEngineConfiguration tempConfig = new RulesEngineConfiguration();
                        List<Rule> rules = ruleFactory.createRules(yamlConfig);
                        for (Rule rule : rules) {
                            tempConfig.registerRule(rule);
                        }
                        List<RuleGroup> groups = ruleFactory.createRuleGroups(yamlConfig, tempConfig);
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

        if (group == null) {
            group = configuration.getRuleGroupById(groupId);
        }

        if (group == null) {
            logger.warn("Rule group not found: {}", groupId);
            return RuleResult.error("rule-group:" + groupId, "Rule group not found");
        }

        RuleResult result = ruleGroupExecutor.executeRuleGroupsList(List.of(group), data, createContext.apply(data));

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
        YamlTransformation transformation = findTransformationById(yamlConfig, transformationId);
        if (transformation == null) {
            logger.warn("Transformation not found: {}", transformationId);
            return RuleResult.error("transformation:" + transformationId, "Transformation not found");
        }

        YamlTransformationProcessor processor = new YamlTransformationProcessor(this.evaluatorService);
        RuleResult transformationResult = processor.processTransformationsWithResult(List.of(transformation), data);

        if (transformationResult.getResultType() == RuleResult.ResultType.ERROR) {
            logger.error("Transformation processing failed: {}", transformationResult.getMessage());
        }

        return transformationResult;
    }

    /**
     * Process a single rule chain by ID.
     */
    private RuleResult processRuleChainItem(String chainId, YamlRuleConfiguration yamlConfig, Map<String, Object> data, Function<Map<String, Object>, org.springframework.expression.spel.support.StandardEvaluationContext> contextFactory) {
        return ruleChainExecutor.processRuleChain(chainId, yamlConfig, data, contextFactory);
    }

    // ===================================
    // Section Processors (Legacy)
    // ===================================

    /**
     * Process enrichments section.
     */
    private void processEnrichmentsSection(
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> enrichedData,
            List<String> failureMessages,
            long sectionStart,
            List<ExecutionStep> executionPath) {

        if (enrichmentProcessor != null && yamlConfig.getEnrichments() != null && !yamlConfig.getEnrichments().isEmpty()) {
            logger.info("Processing {} enrichments", yamlConfig.getEnrichments().size());
            String sectionStatus = "SUCCESS";
            String sectionMessage = "Section processed";

            try {
                RuleResult enrichmentResult = enrichmentProcessor.processEnrichmentsWithResult(
                    yamlConfig.getEnrichments(), enrichedData, yamlConfig);

                if (enrichmentResult.getResultType() == RuleResult.ResultType.ERROR) {
                    sectionStatus = "FAILURE";
                    sectionMessage = enrichmentResult.getMessage();
                    failureMessages.add("Enrichment processing failed: " + enrichmentResult.getMessage());
                    if (enrichmentResult.hasFailures()) {
                        failureMessages.addAll(enrichmentResult.getFailureMessages());
                    }
                    logger.error("Enrichment processing failed: {}", enrichmentResult.getMessage());
                }

                if (enrichmentResult.getEnrichedData() != null && !enrichmentResult.getEnrichedData().isEmpty()) {
                    enrichedData.putAll(enrichmentResult.getEnrichedData());
                    logger.debug("Enrichment completed, enriched data size: {}", enrichedData.size());
                }
            } catch (Exception e) {
                logger.error("CRITICAL: Enrichment processing exception: {}", e.getMessage());
                logger.debug("Full stack trace for enrichment processing exception:", e);
                sectionStatus = "FAILURE";
                sectionMessage = e.getMessage();
                failureMessages.add("Enrichment processing failed: " + e.getMessage());
            }

            long duration = System.currentTimeMillis() - sectionStart;
            executionPath.add(new ExecutionStep("enrichments", "SECTION", sectionStatus, sectionMessage, duration));
        }
    }

    /**
     * Process rules section.
     */
    private void processRulesSection(
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> enrichedData,
            List<String> failureMessages,
            List<RuleResult> individualRuleResults,
            Function<RuleExecutionContext, RuleResult> executeRule) {

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

            for (Rule rule : allRules) {
                RuleExecutionContext ctx = new RuleExecutionContext(rule, enrichedData);
                RuleResult ruleResult = executeRule.apply(ctx);
                individualRuleResults.add(ruleResult);

                if (ruleResult.getResultType() == RuleResult.ResultType.ERROR) {
                    failureMessages.add("Rule evaluation error: " + ruleResult.getMessage());
                }

                if (ruleResult.getEnrichedData() != null) {
                    enrichedData.putAll(ruleResult.getEnrichedData());
                }
            }
        }
    }

    /**
     * Process rule-groups section.
     */
    private void processRuleGroupsSection(
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> enrichedData,
            List<String> failureMessages,
            Function<Map<String, Object>, org.springframework.expression.spel.support.StandardEvaluationContext> contextFactory) {

        List<RuleGroup> allRuleGroups = null;
        if (yamlConfig.getRuleGroups() != null && !yamlConfig.getRuleGroups().isEmpty()) {
            logger.info("Using rule groups from yamlConfig parameter");
            try {
                YamlRuleFactory ruleFactory = new YamlRuleFactory();
                RulesEngineConfiguration tempConfig = new RulesEngineConfiguration();
                List<Rule> rules = ruleFactory.createRules(yamlConfig);
                for (Rule rule : rules) {
                    tempConfig.registerRule(rule);
                }
                allRuleGroups = ruleFactory.createRuleGroups(yamlConfig, tempConfig);
            } catch (YamlConfigurationException e) {
                logger.error("Failed to create rule groups: {}", e.getMessage());
                failureMessages.add("Rule group creation error: " + e.getMessage());
                return;
            }
        } else if (configuration.getAllRuleGroups() != null && !configuration.getAllRuleGroups().isEmpty()) {
            logger.info("Using rule groups from engine's internal configuration");
            allRuleGroups = configuration.getAllRuleGroups();
        }

        if (allRuleGroups != null && !allRuleGroups.isEmpty()) {
            logger.info("Processing {} rule groups", allRuleGroups.size());
            RuleResult ruleGroupResult = ruleGroupExecutor.executeRuleGroupsList(allRuleGroups, enrichedData, contextFactory.apply(enrichedData));

            if (ruleGroupResult.getResultType() == RuleResult.ResultType.ERROR) {
                failureMessages.add("Rule group evaluation error: " + ruleGroupResult.getMessage());
            }
        }
    }

    /**
     * Process enrichment-groups section.
     */
    private void processEnrichmentGroupsSection(
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> enrichedData,
            List<String> failureMessages) {

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
            RuleResult enrichmentGroupResult = enrichmentGroupExecutor.executeEnrichmentGroupsList(allEnrichmentGroups, enrichedData, yamlConfig);

            logger.debug("Enrichment group result type: {}", enrichmentGroupResult.getResultType());
            logger.debug("Enrichment group result data keys: {}", enrichmentGroupResult.getEnrichedData().keySet());

            if (enrichmentGroupResult.getResultType() == RuleResult.ResultType.ERROR) {
                failureMessages.add("Enrichment group evaluation error: " + enrichmentGroupResult.getMessage());
            }

            if (enrichmentGroupResult.getEnrichedData() != null) {
                enrichedData.putAll(enrichmentGroupResult.getEnrichedData());
            }
        }
    }

    /**
     * Process pipeline section.
     */
    private void processPipelineSection(
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> enrichedData,
            List<String> failureMessages,
            List<ExecutionStep> executionPath,
            Function<PipelineExecutionContext, RuleResult> executePipeline) {

        if (yamlConfig.getPipeline() != null) {
            logger.info("Processing pipeline: {}", yamlConfig.getPipeline().getName());
            PipelineExecutionContext ctx = new PipelineExecutionContext(yamlConfig.getPipeline(), enrichedData);
            RuleResult pipelineResult = executePipeline.apply(ctx);

            if (pipelineResult.getExecutionPath() != null) {
                executionPath.addAll(pipelineResult.getExecutionPath());
            }

            if (pipelineResult.getResultType() == RuleResult.ResultType.ERROR) {
                failureMessages.add("Pipeline execution error: " + pipelineResult.getMessage());
            }
        }
    }

    /**
     * Process rule-chains section.
     */
    private void processRuleChainsSection(
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> enrichedData,
            List<String> failureMessages,
            Function<Map<String, Object>, org.springframework.expression.spel.support.StandardEvaluationContext> contextFactory) {

        if (yamlConfig.getRuleChains() != null && !yamlConfig.getRuleChains().isEmpty()) {
            logger.info("Processing {} rule chains", yamlConfig.getRuleChains().size());
            for (YamlRuleChain chain : yamlConfig.getRuleChains()) {
                RuleResult chainResult = processRuleChainItem(chain.getId(), yamlConfig, enrichedData, contextFactory);
                if (chainResult.getResultType() == RuleResult.ResultType.ERROR) {
                    failureMessages.add("Rule chain '" + chain.getId() + "' error: " + chainResult.getMessage());
                }
            }
        }
    }

    /**
     * Process transformations section.
     */
    private void processTransformationsSection(
            YamlRuleConfiguration yamlConfig,
            Map<String, Object> enrichedData,
            List<String> failureMessages,
            long sectionStart,
            List<ExecutionStep> executionPath) {

        if (yamlConfig.getTransformations() != null && !yamlConfig.getTransformations().isEmpty()) {
            logger.info("Processing {} transformations", yamlConfig.getTransformations().size());
            String sectionStatus = "SUCCESS";
            String sectionMessage = "Section processed";

            YamlTransformationProcessor transformationProcessor = new YamlTransformationProcessor(this.evaluatorService);
            RuleResult transformationResult = transformationProcessor.processTransformationsWithResult(
                yamlConfig.getTransformations(), enrichedData);

            if (transformationResult.getResultType() == RuleResult.ResultType.ERROR) {
                sectionStatus = "FAILURE";
                sectionMessage = transformationResult.getMessage();
                failureMessages.add("Transformation processing failed: " + transformationResult.getMessage());
                if (transformationResult.hasFailures()) {
                    failureMessages.addAll(transformationResult.getFailureMessages());
                }
                logger.error("Transformation processing failed: {}", transformationResult.getMessage());
            }

            if (transformationResult.getEnrichedData() != null && !transformationResult.getEnrichedData().isEmpty()) {
                enrichedData.putAll(transformationResult.getEnrichedData());
            }

            long duration = System.currentTimeMillis() - sectionStart;
            executionPath.add(new ExecutionStep("transformations", "SECTION", sectionStatus, sectionMessage, duration));
        }
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
