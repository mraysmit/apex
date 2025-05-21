package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleGroup;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.service.common.NamedService;
import com.rulesengine.core.service.validation.Validator;
import com.rulesengine.core.util.RuleParameterExtractor;
import com.rulesengine.demo.model.Trade;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * A validator for Trade objects using the RulesEngine.
 * This class provides methods to validate trades against defined criteria.
 */
public class TradeValidator implements Validator<Trade>, NamedService {
    private final String name;
    private final Map<String, Object> parameters;
    private final RulesEngine rulesEngine;
    private final RuleGroup validationRuleGroup;
    private final StandardEvaluationContext context;

    /**
     * Create a new TradeValidator with the specified parameters.
     *
     * @param name The name of the validator
     * @param parameters Map of validation parameters (allowedValues, allowedCategories, etc.)
     * @param config The configuration to use for creating validation rules
     */
    public TradeValidator(String name, Map<String, Object> parameters, DynamicTradeValidatorDemoConfig config) {
        this.name = name;
        this.parameters = new HashMap<>(parameters);
        this.rulesEngine = config.getRulesEngine();
        this.context = new StandardEvaluationContext();

        // Initialize context with validation parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        // Create validation rule group using the config
        this.validationRuleGroup = config.createValidationRuleGroup(name, parameters);
    }

    /**
     * Create a new TradeValidator with the specified criteria.
     * This constructor is provided for backward compatibility.
     *
     * @param name The name of the validator
     * @param allowedValues The allowed values for a valid trade
     * @param allowedCategories The allowed categories for a valid trade
     */
    public TradeValidator(String name, String[] allowedValues, String[] allowedCategories) {
        this.name = name;
        this.parameters = new HashMap<>();
        parameters.put("allowedValues", Arrays.asList(allowedValues));
        parameters.put("allowedCategories", Arrays.asList(allowedCategories));
        
        this.rulesEngine = new com.rulesengine.core.engine.config.RulesEngine(
            new com.rulesengine.core.engine.config.RulesEngineConfiguration());
        this.context = new StandardEvaluationContext();

        // Initialize context with validation parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        // Create validation rule group
        DynamicTradeValidatorDemoConfig config = new DynamicTradeValidatorDemoConfig(rulesEngine);
        this.validationRuleGroup = config.createValidationRuleGroup(name, parameters);
    }

    /**
     * Create a new TradeValidator with the specified criteria.
     * This constructor is provided for backward compatibility.
     *
     * @param name The name of the validator
     * @param allowedValues The allowed values for a valid trade
     * @param allowedCategories The allowed categories for a valid trade
     */
    public TradeValidator(String name, List<String> allowedValues, List<String> allowedCategories) {
        this.name = name;
        this.parameters = new HashMap<>();
        parameters.put("allowedValues", allowedValues);
        parameters.put("allowedCategories", allowedCategories);
        
        this.rulesEngine = new com.rulesengine.core.engine.config.RulesEngine(
            new com.rulesengine.core.engine.config.RulesEngineConfiguration());
        this.context = new StandardEvaluationContext();

        // Initialize context with validation parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        // Create validation rule group
        DynamicTradeValidatorDemoConfig config = new DynamicTradeValidatorDemoConfig(rulesEngine);
        this.validationRuleGroup = config.createValidationRuleGroup(name, parameters);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean validate(Trade trade) {
        RuleResult result = validateWithResult(trade);
        return result.isTriggered();
    }

    @Override
    public Class<Trade> getType() {
        return Trade.class;
    }

    @Override
    public RuleResult validateWithResult(Trade trade) {
        // Set the trade in the context
        context.setVariable("trade", trade);

        // Create initial facts map with trade data and parameters
        Map<String, Object> initialFacts = new HashMap<>(parameters);
        initialFacts.put("trade", trade);

        // Use RuleParameterExtractor to ensure all required parameters exist in the facts map
        Map<String, Object> facts = RuleParameterExtractor.ensureParameters(validationRuleGroup, initialFacts);

        // Execute the rule group using the rules engine
        return rulesEngine.executeRuleGroupsList(Collections.singletonList(validationRuleGroup), facts);
    }

    /**
     * Validate a trade using a dynamic expression.
     *
     * @param trade The trade to validate
     * @param expression The expression to evaluate
     * @param config The configuration to use for creating the dynamic rule
     * @return True if the expression evaluates to true, false otherwise
     */
    public boolean validateWithExpression(Trade trade, String expression, DynamicTradeValidatorDemoConfig config) {
        // Set the trade in the context
        context.setVariable("trade", trade);

        // Create a rule with the dynamic expression
        Rule dynamicRule = config.createDynamicValidationRule(expression);

        // Create initial facts map with trade data and parameters
        Map<String, Object> initialFacts = new HashMap<>(parameters);
        initialFacts.put("trade", trade);

        // Use RuleParameterExtractor to ensure all required parameters for the dynamic rule exist in the facts map
        Map<String, Object> facts = RuleParameterExtractor.ensureParameters(dynamicRule, initialFacts);

        // Execute the rule using the rules engine
        RuleResult result = rulesEngine.executeRule(dynamicRule, facts);
        return result.isTriggered();
    }
}