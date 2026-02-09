/*
 * * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd 
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
package dev.mars.apex.core.engine.config.execution;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.config.util.DataCopyUtility;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.engine.model.EnrichmentGroupResult;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.enrichment.YamlEnrichmentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Executes enrichment groups with support for parallel and sequential processing.
 * Extracted from RulesEngine to improve maintainability and separation of concerns.
 *
 * <p>This class handles:
 * <ul>
 *   <li>Sequential enrichment execution with short-circuit support</li>
 *   <li>Parallel enrichment execution with thread-safe data copying</li>
 *   <li>AND/OR operator aggregation</li>
 *   <li>Result aggregation and error handling</li>
 * </ul>
 *
 * @since 2.1.0
 */
public class EnrichmentGroupExecutor {
    private static final Logger logger = LoggerFactory.getLogger(EnrichmentGroupExecutor.class);

    private final YamlEnrichmentProcessor enrichmentProcessor;

    /**
     * Create a new EnrichmentGroupExecutor with the specified enrichment processor.
     *
     * @param enrichmentProcessor The processor to use for individual enrichments
     */
    public EnrichmentGroupExecutor(YamlEnrichmentProcessor enrichmentProcessor) {
        this.enrichmentProcessor = enrichmentProcessor;
    }

    /**
     * Execute a list of enrichment groups with proper result aggregation.
     *
     * @param enrichmentGroups The list of enrichment groups to execute
     * @param targetObject The target object to enrich
     * @param yamlConfig The YAML configuration (required for database lookups)
     * @return The result of enrichment group execution
     */
    public RuleResult executeEnrichmentGroupsList(List<EnrichmentGroup> enrichmentGroups, Object targetObject, 
                                                   YamlRuleConfiguration yamlConfig) {
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
                EnrichmentGroupResult groupResult = processEnrichmentGroup(group, enrichedData, yamlConfig);
                
                if (!groupResult.isSuccess()) {
                    overallSuccess = false;
                    failureMessages.add(String.format("Enrichment group '%s' failed: %s", 
                        group.getId(), groupResult.getMessage()));
                }
                
                // Collect enriched data from all enrichment results in the group
                if (groupResult.getEnrichmentResults() != null) {
                    for (RuleResult enrichmentResult : groupResult.getEnrichmentResults()) {
                        if (enrichmentResult.getEnrichedData() != null) {
                            enrichedData.putAll(enrichmentResult.getEnrichedData());
                        }
                    }
                }
                
                logger.debug("Enriched data keys after group '{}': {}", group.getName(), enrichedData.keySet());
            } catch (Exception e) {
                overallSuccess = false;
                failureMessages.add(String.format("Error executing enrichment group '%s': %s", 
                    group.getName(), e.getMessage()));
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
     *
     * @param group The enrichment group to process
     * @param targetObject The target object to enrich
     * @param yamlConfig The YAML configuration (required for database lookups)
     * @return The result of enrichment group processing
     */
    public EnrichmentGroupResult processEnrichmentGroup(EnrichmentGroup group, Object targetObject, 
                                                        YamlRuleConfiguration yamlConfig) {
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
            results = processEnrichmentGroupParallel(ordered, targetObject, yamlConfig);
        } else {
            // Sequential execution with possible short-circuit
            results = processEnrichmentGroupSequential(ordered, targetObject, andOp, shortCircuit, yamlConfig);
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
     * @param yamlConfig The YAML configuration (required for database lookups)
     * @return List of enrichment results
     */
    private List<RuleResult> processEnrichmentGroupParallel(List<YamlEnrichment> enrichments, Object targetObject, 
                                                            YamlRuleConfiguration yamlConfig) {
        List<RuleResult> results = new ArrayList<>();
        List<Callable<RuleResult>> tasks = new ArrayList<>();

        // Convert to map once for deep copying
        Map<String, Object> sourceMap = convertToMap(targetObject);

        for (YamlEnrichment enrichment : enrichments) {
            tasks.add(() -> {
                // Create a deep copy of the data for each parallel task
                // This prevents race conditions when enrichments mutate nested structures
                // (e.g., barrierTerms['knockoutConditions']['rebateTerms'])
                Map<String, Object> taskTargetObject = DataCopyUtility.deepCopyMap(sourceMap);
                try {
                    return enrichmentProcessor.processEnrichmentWithResult(enrichment, taskTargetObject, yamlConfig);
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
     * Process enrichments sequentially.
     *
     * @param enrichments The list of enrichments to process
     * @param targetObject The target object to enrich
     * @param andOp Whether to use AND operator (true) or OR operator (false)
     * @param shortCircuit Whether to stop on first failure/success
     * @param yamlConfig The YAML configuration (required for database lookups)
     * @return List of enrichment results
     */
    private List<RuleResult> processEnrichmentGroupSequential(List<YamlEnrichment> enrichments, Object targetObject,
                                                               boolean andOp, boolean shortCircuit, 
                                                               YamlRuleConfiguration yamlConfig) {
        List<RuleResult> results = new ArrayList<>();

        for (YamlEnrichment enrichment : enrichments) {
            RuleResult r = enrichmentProcessor.processEnrichmentWithResult(enrichment, targetObject, yamlConfig);
            results.add(r);
            boolean ok = r.isSuccess();

            if (andOp) {
                // AND logic: stop if any fails (when short-circuit enabled)
                if (!ok && shortCircuit) {
                    break;
                }
            } else {
                // OR logic: stop if any succeeds (when short-circuit enabled)
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
        if (!andOp) {
            overall = false;
        }

        for (RuleResult r : results) {
            boolean ok = r.isSuccess();
            if (andOp) {
                overall = overall && ok;
            } else {
                overall = overall || ok;
            }
        }

        return overall;
    }

    /**
     * Convert an object to a Map.
     *
     * @param obj The object to convert
     * @return A Map representation of the object
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return new HashMap<>();
    }
}
