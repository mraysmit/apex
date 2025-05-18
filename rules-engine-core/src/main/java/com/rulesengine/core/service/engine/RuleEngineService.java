package com.rulesengine.core.service.engine;

import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleResult;
import org.springframework.expression.EvaluationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for evaluating business rules using SpEL expressions.
 * This class handles rule evaluation and result reporting.
 */
public class RuleEngineService {
    private static final Logger LOGGER = Logger.getLogger(RuleEngineService.class.getName());
    private final ExpressionEvaluatorService evaluatorService;
    private boolean printResults = true;

    public RuleEngineService(ExpressionEvaluatorService evaluatorService) {
        LOGGER.info("Initializing RuleEngineService");
        this.evaluatorService = evaluatorService;
        LOGGER.fine("Using evaluator service: " + evaluatorService.getClass().getSimpleName());
    }

    /**
     * Set whether to print results to the console.
     * 
     * @param printResults True to print results, false to suppress output
     * @return This service for method chaining
     */
    public RuleEngineService setPrintResults(boolean printResults) {
        LOGGER.fine("Setting printResults to: " + printResults);
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
        LOGGER.info("Evaluating " + (rules != null ? rules.size() : 0) + " rules");
        List<RuleResult> results = new ArrayList<>();

        if (rules == null || rules.isEmpty()) {
            LOGGER.info("No rules to evaluate");
            return results;
        }

        for (Rule rule : rules) {
            LOGGER.fine("Evaluating rule: " + rule.getName());
            try {
                Object result = evaluatorService.evaluateQuietly(rule.getCondition(), context, Object.class);
                RuleResult ruleResult = new RuleResult(rule.getName(), rule.getMessage());
                results.add(ruleResult);
                LOGGER.fine("Rule '" + rule.getName() + "' evaluated, result: " + result);

                if (printResults) {
                    LOGGER.info(rule.getName() + ": " + rule.getMessage());
                    LOGGER.info("Result: " + result);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error evaluating rule '" + rule.getName() + "': " + e.getMessage(), e);
            }
        }

        LOGGER.info("Evaluated " + results.size() + " rules successfully");
        return results;
    }
}