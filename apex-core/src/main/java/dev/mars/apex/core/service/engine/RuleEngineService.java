package dev.mars.apex.core.service.engine;

import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import org.springframework.expression.EvaluationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
 * Service for evaluating business rules using SpEL expressions.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Service for evaluating business rules using SpEL expressions.
 * This class handles rule evaluation and result reporting.
 */
public class RuleEngineService {
    private static final Logger logger = LoggerFactory.getLogger(RuleEngineService.class);
    private final ExpressionEvaluatorService evaluatorService;
    private boolean printResults = true;

    public RuleEngineService(ExpressionEvaluatorService evaluatorService) {
        logger.info("Initializing RuleEngineService");
        this.evaluatorService = evaluatorService;
        logger.debug("Using evaluator service: " + evaluatorService.getClass().getSimpleName());
    }

    /**
     * Set whether to print results to the console.
     * 
     * @param printResults True to print results, false to suppress output
     * @return This service for method chaining
     */
    public RuleEngineService setPrintResults(boolean printResults) {
        logger.debug("Setting printResults to: " + printResults);
        this.printResults = printResults;
        return this;
    }

    /**
     * Evaluates a list of rules against the given context and returns the results.
     * 
     * @param rules The rules to evaluate
     * @param context The evaluation context
     * @return A list of RuleResult objects, one for each rule that was evaluated
     */
    public List<RuleResult> evaluateRules(List<Rule> rules, EvaluationContext context) {
        logger.info("Evaluating " + (rules != null ? rules.size() : 0) + " rules");
        List<RuleResult> results = new ArrayList<>();

        if (rules == null || rules.isEmpty()) {
            logger.info("No rules to evaluate");
            return results;
        }

        for (Rule rule : rules) {
            logger.debug("Evaluating rule: " + rule.getName());
            try {
                // Use evaluateWithResult instead of evaluateQuietly for better error handling
                RuleResult baseResult = evaluatorService.evaluateWithResult(rule.getCondition(), context, Object.class);

                // Create a proper RuleResult with the rule name and message
                RuleResult ruleResult;
                if (baseResult.getResultType() == RuleResult.ResultType.MATCH) {
                    ruleResult = RuleResult.match(rule.getName(), rule.getMessage(), rule.getSeverity());
                } else if (baseResult.getResultType() == RuleResult.ResultType.ERROR) {
                    String severity = rule.getSeverity() != null ? rule.getSeverity() : SeverityConstants.ERROR;
                    ruleResult = RuleResult.error(rule.getName(), baseResult.getMessage(), severity);
                    // Print to System.err for test verification
                    System.err.println("Error evaluating rule '" + rule.getName() + "': " + baseResult.getMessage());
                } else {
                    ruleResult = RuleResult.noMatch();
                }

                results.add(ruleResult);
                logger.debug("Rule '" + rule.getName() + "' evaluated, result type: " + ruleResult.getResultType());

                if (printResults) {
                    logger.info(rule.getName() + ": " + rule.getMessage());
                    logger.info("Result type: " + ruleResult.getResultType());
                    // Also print to System.out for test verification
                    System.out.println(rule.getName() + ": " + rule.getMessage());
                    System.out.println("Result: " + (ruleResult.isTriggered() ? "true" : "false"));
                }
            } catch (Exception e) {
                // Create structured error result with severity from rule configuration
                String severity = rule.getSeverity() != null ? rule.getSeverity() : SeverityConstants.ERROR;
                String errorMessage = String.format("Rule evaluation failed: %s", e.getMessage());
                RuleResult errorResult = RuleResult.error(rule.getName(), errorMessage, severity);
                results.add(errorResult);

                // Log error details at appropriate level based on severity (no stack traces in logs)
                if (SeverityConstants.CRITICAL.equalsIgnoreCase(severity)) {
                    logger.error("CRITICAL rule evaluation error for '" + rule.getName() + "': " + e.getMessage());
                } else if (SeverityConstants.WARNING.equalsIgnoreCase(severity)) {
                    logger.info("Rule evaluation warning for '" + rule.getName() + "': " + e.getMessage());
                } else {
                    logger.info("Rule evaluation error for '" + rule.getName() + "': " + e.getMessage());
                }

                // Log full exception details only at FINE level for debugging
                logger.debug("Full exception details for rule '" + rule.getName() + "':", e);
            }
        }

        logger.info("Evaluated " + results.size() + " rules successfully");
        return results;
    }
}
