package com.rulesengine.core.engine.config;

import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleBase;
import com.rulesengine.core.engine.model.RuleGroup;
import com.rulesengine.core.engine.model.RuleResult;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements a business rules engine using SpEL.
 * It provides a flexible, configurable rules system that can be easily extended
 * and modified without changing the core code.
 * 
 * This class is responsible only for rule evaluation, not configuration.
 * Configuration is handled by the RulesEngineConfiguration class.
 */
public class RulesEngine {
    private static final Logger LOGGER = Logger.getLogger(RulesEngine.class.getName());
    private final ExpressionParser parser;
    private final RulesEngineConfiguration configuration;


    /**
     * Create a new RulesEngine with the given configuration.
     *
     * @param configuration The configuration for this rules engine
     */
    public RulesEngine(RulesEngineConfiguration configuration) {
        this(configuration, new SpelExpressionParser());
    }

    /**
     * Create a new RulesEngine with the given configuration and expression parser.
     *
     * @param configuration The configuration for this rules engine
     * @param parser The expression parser to use
     */
    public RulesEngine(RulesEngineConfiguration configuration, ExpressionParser parser) {
        this.configuration = configuration;
        this.parser = parser;
        LOGGER.info("RulesEngine initialized with configuration: " + configuration.getClass().getSimpleName());
        LOGGER.fine("Using parser: " + parser.getClass().getSimpleName());
    }

    /**
     * Get the configuration for this rules engine.
     *
     * @return The configuration for this rules engine
     */
    public RulesEngineConfiguration getConfiguration() {
        return configuration;
    }

    // Rule Execution Methods

    /**
     * Create an evaluation context with the provided facts.
     *
     * @param facts The facts to add to the context
     * @return A new StandardEvaluationContext with the facts added as variables
     */
    private StandardEvaluationContext createContext(Map<String, Object> facts) {
        LOGGER.fine("Creating evaluation context");
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Add all facts to the evaluation context
        if (facts != null) {
            LOGGER.fine("Adding " + facts.size() + " facts to context");
            for (Map.Entry<String, Object> fact : facts.entrySet()) {
                context.setVariable(fact.getKey(), fact.getValue());
                LOGGER.finest("Added fact: " + fact.getKey() + " = " + 
                    (fact.getValue() != null ? fact.getValue().getClass().getSimpleName() : "null"));
            }
        } else {
            LOGGER.fine("No facts provided to context");
        }

        return context;
    }

    /**
     * Execute a single Rule object against the provided facts.
     *
     * @param rule The Rule object to execute
     * @param facts The facts to evaluate the rule against
     * @return The result of the rule evaluation, indicating whether it matched or not
     */
    public RuleResult executeRule(Rule rule, Map<String, Object> facts) {
        if (rule == null) {
            LOGGER.info("No rule provided for execution");
            return RuleResult.noRules();
        }

        LOGGER.info("Executing rule: " + rule.getName());
        LOGGER.fine("Facts provided: " + (facts != null ? facts.keySet() : "none"));

        // Check for missing parameters
        Set<String> missingParameters = com.rulesengine.core.util.RuleParameterExtractor.validateParameters(rule, facts);
        if (!missingParameters.isEmpty()) {
            LOGGER.warning("Missing parameters for rule '" + rule.getName() + "': " + missingParameters);
            return RuleResult.error(rule.getName(), "Missing parameters: " + missingParameters);
        }

        StandardEvaluationContext context = createContext(facts);

        // Evaluate the rule
        LOGGER.fine("Evaluating rule: " + rule.getName());
        try {
            Expression exp = parser.parseExpression(rule.getCondition());
            Boolean result = exp.getValue(context, Boolean.class);
            LOGGER.fine("Rule '" + rule.getName() + "' evaluated to: " + result);

            if (result != null && result) {
                LOGGER.info("Rule matched: " + rule.getName());
                return RuleResult.match(rule.getName(), rule.getMessage());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error evaluating rule '" + rule.getName() + "': " + e.getMessage(), e);
            return RuleResult.error(rule.getName(), "Error evaluating rule: " + e.getMessage());
        }

        LOGGER.info("Rule did not match: " + rule.getName());
        return RuleResult.noMatch();
    }

    /**
     * Execute a list of Rule objects against the provided facts.
     *
     * @param rules The list of Rule objects to execute
     * @param facts The facts to evaluate the rules against
     * @return The result of the first rule that matches, or a default result if no rules match
     */
    public RuleResult executeRulesList(List<Rule> rules, Map<String, Object> facts) {
        if (rules == null || rules.isEmpty()) {
            LOGGER.info("No rules provided for execution");
            return RuleResult.noRules();
        }

        LOGGER.info("Executing " + rules.size() + " rules");
        LOGGER.fine("Facts provided: " + (facts != null ? facts.keySet() : "none"));

        StandardEvaluationContext context = createContext(facts);

        // Evaluate rules in priority order
        for (Rule rule : rules) {
            LOGGER.fine("Evaluating rule: " + rule.getName());
            try {
                Expression exp = parser.parseExpression(rule.getCondition());
                Boolean result = exp.getValue(context, Boolean.class);
                LOGGER.fine("Rule '" + rule.getName() + "' evaluated to: " + result);

                if (result != null && result) {
                    LOGGER.info("Rule matched: " + rule.getName());
                    return RuleResult.match(rule.getName(), rule.getMessage());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error evaluating rule '" + rule.getName() + "': " + e.getMessage(), e);
            }
        }

        LOGGER.info("No rules matched");
        return RuleResult.noMatch();
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
            LOGGER.info("No rule groups provided for execution");
            return RuleResult.noRules();
        }

        LOGGER.info("Executing " + ruleGroups.size() + " rule groups");
        LOGGER.fine("Facts provided: " + (facts != null ? facts.keySet() : "none"));

        StandardEvaluationContext context = createContext(facts);

        // Evaluate rule groups in priority order
        for (RuleGroup group : ruleGroups) {
            LOGGER.fine("Evaluating rule group: " + group.getName());
            try {
                boolean result = group.evaluate(context);
                LOGGER.fine("Rule group '" + group.getName() + "' evaluated to: " + result);

                if (result) {
                    LOGGER.info("Rule group matched: " + group.getName());
                    return RuleResult.match(group.getName(), group.getMessage());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error evaluating rule group '" + group.getName() + "': " + e.getMessage(), e);
            }
        }

        LOGGER.info("No rule groups matched");
        return RuleResult.noMatch();
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
            LOGGER.info("No rules provided for execution");
            return RuleResult.noRules();
        }

        LOGGER.info("Executing " + rules.size() + " rules/rule groups");
        LOGGER.fine("Facts provided: " + (facts != null ? facts.keySet() : "none"));

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
            LOGGER.fine("All objects are Rule instances, delegating to executeRulesList");
            @SuppressWarnings("unchecked")
            List<Rule> rulesList = (List<Rule>) (List<?>) rules;
            return executeRulesList(rulesList, facts);
        } else if (allRuleGroups) {
            // All objects are RuleGroup instances, so we can safely cast and delegate
            LOGGER.fine("All objects are RuleGroup instances, delegating to executeRuleGroupsList");
            @SuppressWarnings("unchecked")
            List<RuleGroup> ruleGroupsList = (List<RuleGroup>) (List<?>) rules;
            return executeRuleGroupsList(ruleGroupsList, facts);
        }

        LOGGER.fine("Mixed list of rules and rule groups, processing manually");
        // Mixed list or unknown types, process manually
        StandardEvaluationContext context = createContext(facts);

        // Evaluate rules in priority order
        for (RuleBase ruleObj : rules) {
            LOGGER.fine("Evaluating rule/rule group: " + ruleObj.getName());
            try {
                if (ruleObj instanceof Rule) {
                    Rule rule = (Rule) ruleObj;
                    Expression exp = parser.parseExpression(rule.getCondition());
                    Boolean result = exp.getValue(context, Boolean.class);
                    LOGGER.fine("Rule '" + rule.getName() + "' evaluated to: " + result);

                    if (result != null && result) {
                        LOGGER.info("Rule matched: " + rule.getName());
                        return RuleResult.match(rule.getName(), rule.getMessage());
                    }
                } else if (ruleObj instanceof RuleGroup) {
                    RuleGroup group = (RuleGroup) ruleObj;
                    boolean result = group.evaluate(context);
                    LOGGER.fine("Rule group '" + group.getName() + "' evaluated to: " + result);

                    if (result) {
                        LOGGER.info("Rule group matched: " + group.getName());
                        return RuleResult.match(group.getName(), group.getMessage());
                    }
                }
            } catch (Exception e) {
                String ruleName = ruleObj.getName();
                LOGGER.log(Level.WARNING, "Error evaluating rule/rule group '" + ruleName + "': " + e.getMessage(), e);
            }
        }

        LOGGER.info("No rules or rule groups matched");
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
        LOGGER.info("Executing rules for category: " + category);
        List<RuleBase> rules = configuration.getRulesForCategory(category);
        LOGGER.fine("Found " + rules.size() + " rules/rule groups in category: " + category);
        return executeRules(rules, facts);
    }
}
