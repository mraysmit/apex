package dev.mars.rulesengine.demo.advanced;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import dev.mars.rulesengine.core.service.engine.RuleEngineService;
import dev.mars.rulesengine.core.service.engine.TemplateProcessorService;
import dev.mars.rulesengine.demo.model.Customer;
import dev.mars.rulesengine.demo.model.Product;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
 * Configuration class for SpelAdvancedFeaturesDemo.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Configuration class for SpelAdvancedFeaturesDemo.
 * This class provides configuration, rules, and data for the SpelAdvancedFeaturesDemo.
 */
public class SpelAdvancedFeaturesDemoConfig {
    private static final Logger LOGGER = Logger.getLogger(SpelAdvancedFeaturesDemoConfig.class.getName());
    
    private final RulesEngine rulesEngine;
    private final ExpressionEvaluatorService evaluatorService;
    private final RuleEngineService ruleEngineService;
    private final TemplateProcessorService templateProcessorService;
    private final SpelAdvancedFeaturesDataProvider dataProvider;
    
    /**
     * Create a new SpelAdvancedFeaturesDemoConfig with the specified services.
     *
     * @param rulesEngine The rules engine to use
     * @param evaluatorService The expression evaluator service to use
     * @param ruleEngineService The rule engine service to use
     * @param templateProcessorService The template processor service to use
     */
    public SpelAdvancedFeaturesDemoConfig(
            RulesEngine rulesEngine,
            ExpressionEvaluatorService evaluatorService,
            RuleEngineService ruleEngineService,
            TemplateProcessorService templateProcessorService) {
        this.rulesEngine = rulesEngine;
        this.evaluatorService = evaluatorService;
        this.ruleEngineService = ruleEngineService;
        this.templateProcessorService = templateProcessorService;
        this.dataProvider = new SpelAdvancedFeaturesDataProvider();
    }
    
    /**
     * Create a standard evaluation context with data from the data provider.
     *
     * @return The evaluation context
     */
    public StandardEvaluationContext createContext() {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // Add products data
        List<Product> products = dataProvider.getData("products");
        context.setVariable("products", products);
        
        // Add inventory data
        List<Product> inventory = dataProvider.getData("inventory");
        context.setVariable("inventory", inventory);
        
        // Add customer data
        Customer customer = dataProvider.getData("customer");
        context.setVariable("customer", customer);
        
        return context;
    }
    
    /**
     * Create a context for template expressions.
     *
     * @return The evaluation context
     */
    public StandardEvaluationContext createTemplateContext() {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // Add template customer data
        Customer customer = dataProvider.getData("templateCustomer");
        context.setVariable("customer", customer);
        
        // Add products data
        List<Product> products = dataProvider.getData("products");
        context.setVariable("products", products);
        
        return context;
    }
    
    /**
     * Create rules for investment recommendations.
     *
     * @return The list of rules
     */
    public List<Rule> createInvestmentRules() {
        List<Rule> rules = new ArrayList<>();
        rules.add(createInvestmentRecommendationsRule());
        rules.add(createGoldTierInvestorOffersRule());
        rules.add(createLowCostInvestmentOptionsRule());
        return rules;
    }
    
    /**
     * Create a rule for investment recommendations.
     *
     * @return The rule
     */
    public Rule createInvestmentRecommendationsRule() {
        return new Rule(
            "InvestmentRecommendationsRule",
            "#inventory.?[category == #customer.preferredCategories[0]].size() > 0",
            "Found investment options matching customer's primary preference"
        );
    }
    
    /**
     * Create a rule for gold tier investor offers.
     *
     * @return The rule
     */
    public Rule createGoldTierInvestorOffersRule() {
        return new Rule(
            "GoldTierInvestorOffersRule",
            "#customer.membershipLevel == 'Gold' && #inventory.?[price > 1000].size() > 0",
            "Found premium investment options for Gold tier customer"
        );
    }
    
    /**
     * Create a rule for low cost investment options.
     *
     * @return The rule
     */
    public Rule createLowCostInvestmentOptionsRule() {
        return new Rule(
            "LowCostInvestmentOptionsRule",
            "#inventory.?[price < 500].size() > 0",
            "Found low-cost investment options"
        );
    }
    
    /**
     * Create rules for rule result features demonstration.
     *
     * @return The list of rules
     */
    public List<Rule> createRuleResultRules() {
        List<Rule> rules = new ArrayList<>();
        
        // Rule for conditional follow-up
        rules.add(new Rule(
            "HighValueCustomerRule",
            "#customer.membershipLevel == 'Gold' || #customer.membershipLevel == 'Platinum'",
            "Customer is a high-value member"
        ));
        
        // Rule for follow-up action
        rules.add(new Rule(
            "PremiumOfferRule",
            "true",
            "Offer premium investment options"
        ));
        
        // Rule for result type demonstration
        rules.add(new Rule(
            "CustomerCategoryRule",
            "#customer.preferredCategories",
            "Customer preferred categories"
        ));
        
        // Rules for rule chain
        rules.add(new Rule(
            "InitialAssessmentRule",
            "#customer.age > 50",
            "Customer is over 50 years old"
        ));
        
        rules.add(new Rule(
            "FollowUpAssessmentRule",
            "#customer.membershipLevel == 'Gold'",
            "Customer is a Gold member"
        ));
        
        // Rules for dynamic selection
        rules.add(new Rule(
            "YoungInvestorRule",
            "#customer.age < 30",
            "Young investor strategy recommended"
        ));
        
        rules.add(new Rule(
            "MidAgeInvestorRule",
            "#customer.age >= 30 && #customer.age < 60",
            "Mid-age investor strategy recommended"
        ));
        
        rules.add(new Rule(
            "SeniorInvestorRule",
            "#customer.age >= 60",
            "Senior investor strategy recommended"
        ));
        
        return rules;
    }
    
    /**
     * Get the rules engine.
     *
     * @return The rules engine
     */
    public RulesEngine getRulesEngine() {
        return rulesEngine;
    }
    
    /**
     * Get the expression evaluator service.
     *
     * @return The expression evaluator service
     */
    public ExpressionEvaluatorService getEvaluatorService() {
        return evaluatorService;
    }
    
    /**
     * Get the rule engine service.
     *
     * @return The rule engine service
     */
    public RuleEngineService getRuleEngineService() {
        return ruleEngineService;
    }
    
    /**
     * Get the template processor service.
     *
     * @return The template processor service
     */
    public TemplateProcessorService getTemplateProcessorService() {
        return templateProcessorService;
    }
    
    /**
     * Get the data provider.
     *
     * @return The data provider
     */
    public SpelAdvancedFeaturesDataProvider getDataProvider() {
        return dataProvider;
    }
}