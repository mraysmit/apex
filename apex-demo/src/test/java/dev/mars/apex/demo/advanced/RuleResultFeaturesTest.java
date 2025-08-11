package dev.mars.apex.demo.advanced;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
 * Test class for RuleResult features and capabilities.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for RuleResult features and capabilities.
 * This class tests RuleResult features without depending on demo classes.
 */
public class RuleResultFeaturesTest {
    
    private ExpressionEvaluatorService evaluatorService;
    private RulesEngine rulesEngine;
    private StandardEvaluationContext context;
    
    @BeforeEach
    public void setUp() {
        // Initialize services
        evaluatorService = new ExpressionEvaluatorService();
        rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        
        // Initialize context
        context = new StandardEvaluationContext();
        
        // Add variables for testing
        context.setVariable("investmentAmount", 150000);
        context.setVariable("accountType", "retirement");
        context.setVariable("clientRiskScore", 8);
        context.setVariable("marketVolatility", 0.25);
        context.setVariable("kycVerified", false);
    }
    
    /**
     * Test different RuleResult types.
     */
    @Test
    public void testRuleResultTypes() {
        // Test MATCH result type
        RuleResult matchResult = evaluatorService.evaluateWithResult(
            "#investmentAmount > 100000", context, Boolean.class);
        
        assertTrue(matchResult.isTriggered(), "Match result should be triggered");
        assertEquals(RuleResult.ResultType.MATCH, matchResult.getResultType(), "Result type should be MATCH");
        
        // Test NO_MATCH result type
        RuleResult noMatchResult = evaluatorService.evaluateWithResult(
            "#investmentAmount < 50000", context, Boolean.class);
        
        assertFalse(noMatchResult.isTriggered(), "No match result should not be triggered");
        assertEquals(RuleResult.ResultType.NO_MATCH, noMatchResult.getResultType(), "Result type should be NO_MATCH");
        
        // Test ERROR result type
        RuleResult errorResult = evaluatorService.evaluateWithResult(
            "#undefinedVariable > 100", context, Boolean.class);
        
        assertFalse(errorResult.isTriggered(), "Error result should not be triggered");
        assertEquals(RuleResult.ResultType.ERROR, errorResult.getResultType(), "Result type should be ERROR");
        
        // Test NO_RULES result type
        RuleResult noRulesResult = RuleResult.noRules();
        
        assertFalse(noRulesResult.isTriggered(), "No rules result should not be triggered");
        assertEquals(RuleResult.ResultType.NO_RULES, noRulesResult.getResultType(), "Result type should be NO_RULES");
    }
    
    /**
     * Test conditional rule execution based on RuleResult.
     */
    @Test
    public void testConditionalRuleExecution() {
        // Create rules
        Rule highValueRule = new Rule(
            "HighValueInvestment",
            "#investmentAmount > 100000",
            "High-value investment detected"
        );
        
        Rule retirementAccountRule = new Rule(
            "RetirementAccount",
            "#accountType == 'retirement'",
            "Retirement account detected"
        );
        
        // Create facts for rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("investmentAmount", 150000);
        facts.put("accountType", "retirement");
        
        // Evaluate the first rule
        List<Rule> rules = new ArrayList<>();
        rules.add(highValueRule);
        RuleResult result = rulesEngine.executeRulesList(rules, facts);
        
        // Verify the result
        assertTrue(result.isTriggered(), "High value rule should be triggered");
        assertEquals("HighValueInvestment", result.getRuleName(), "Rule name should match");
        
        // Conditional execution based on the result
        if (result.isTriggered()) {
            // Evaluate the second rule
            List<Rule> followupRules = new ArrayList<>();
            followupRules.add(retirementAccountRule);
            RuleResult followupResult = rulesEngine.executeRulesList(followupRules, facts);
            
            // Verify the followup result
            assertTrue(followupResult.isTriggered(), "Retirement account rule should be triggered");
            assertEquals("RetirementAccount", followupResult.getRuleName(), "Rule name should match");
        }
    }
    
    /**
     * Test rule chaining using RuleResult message.
     */
    @Test
    public void testRuleChaining() {
        // Create rules
        Rule volatileMarketRule = new Rule(
            "VolatileMarket",
            "#marketVolatility > 0.2",
            "Volatile market conditions detected"
        );
        
        Rule kycVerificationRule = new Rule(
            "KYCVerification",
            "!#kycVerified",
            "KYC verification required"
        );
        
        // Create facts for rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("marketVolatility", 0.25);
        facts.put("kycVerified", false);
        
        // Evaluate the first rule
        List<Rule> rules = new ArrayList<>();
        rules.add(volatileMarketRule);
        RuleResult result = rulesEngine.executeRulesList(rules, facts);
        
        // Verify the result
        assertTrue(result.isTriggered(), "Volatile market rule should be triggered");
        assertEquals("VolatileMarket", result.getRuleName(), "Rule name should match");
        
        // Use the message from the first rule to determine the next action
        if (result.isTriggered() && result.getMessage().contains("Volatile market")) {
            // Evaluate the second rule
            List<Rule> followupRules = new ArrayList<>();
            followupRules.add(kycVerificationRule);
            RuleResult followupResult = rulesEngine.executeRulesList(followupRules, facts);
            
            // Verify the followup result
            assertTrue(followupResult.isTriggered(), "KYC verification rule should be triggered");
            assertEquals("KYCVerification", followupResult.getRuleName(), "Rule name should match");
        }
    }
    
    /**
     * Test dynamic rule selection based on RuleResult.
     */
    @Test
    public void testDynamicRuleSelection() {
        // Create a rule to determine the investment type
        Rule investmentTypeRule = new Rule(
            "InvestmentTypeDetermination",
            "#investmentAmount > 100000 ? 'HighValue' : (#accountType == 'retirement' ? 'Retirement' : 'Standard')",
            "Determining investment type"
        );
        
        // Create rules for different investment types
        Rule highValueRule = new Rule(
            "HighValueInvestment",
            "#investmentAmount > 100000",
            "High-value investment detected"
        );
        
        Rule retirementAccountRule = new Rule(
            "RetirementAccount",
            "#accountType == 'retirement'",
            "Retirement account detected"
        );
        
        Rule standardInvestmentRule = new Rule(
            "StandardInvestment",
            "true",
            "Standard investment detected"
        );
        
        // Create a map of rules that can be selected dynamically
        Map<String, Rule> ruleRepository = new HashMap<>();
        ruleRepository.put("HighValue", highValueRule);
        ruleRepository.put("Retirement", retirementAccountRule);
        ruleRepository.put("Standard", standardInvestmentRule);
        
        // Create facts for rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("investmentAmount", 150000);
        facts.put("accountType", "retirement");
        
        // Evaluate the investment type rule
        RuleResult result = evaluatorService.evaluateWithResult(
            investmentTypeRule.getCondition(), context, String.class);
        
        // Verify the result
        assertTrue(result.isTriggered(), "Investment type rule should be triggered");
        
        // Use the result to dynamically select the next rule
        String investmentType = evaluatorService.evaluate(
            investmentTypeRule.getCondition(), context, String.class);
        
        // Verify the investment type
        assertEquals("HighValue", investmentType, "Investment type should be HighValue");
        
        // Get the rule for the investment type
        Rule selectedRule = ruleRepository.get(investmentType);
        
        // Verify the selected rule
        assertNotNull(selectedRule, "Selected rule should not be null");
        assertEquals("HighValueInvestment", selectedRule.getName(), "Selected rule name should match");
        
        // Evaluate the selected rule
        List<Rule> selectedRules = new ArrayList<>();
        selectedRules.add(selectedRule);
        RuleResult selectedResult = rulesEngine.executeRulesList(selectedRules, facts);
        
        // Verify the selected result
        assertTrue(selectedResult.isTriggered(), "Selected rule should be triggered");
        assertEquals("HighValueInvestment", selectedResult.getRuleName(), "Selected rule name should match");
    }
    
    /**
     * Test RuleResult properties.
     */
    @Test
    public void testRuleResultProperties() {
        // Create a rule
        Rule rule = new Rule(
            "TestRule",
            "true",
            "Test message"
        );
        
        // Create facts for rule evaluation
        Map<String, Object> facts = new HashMap<>();
        
        // Evaluate the rule
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);
        RuleResult result = rulesEngine.executeRulesList(rules, facts);
        
        // Verify the result properties
        assertNotNull(result.getId(), "Result ID should not be null");
        assertEquals("TestRule", result.getRuleName(), "Rule name should match");
        assertEquals("Test message", result.getMessage(), "Message should match");
        assertTrue(result.isTriggered(), "Result should be triggered");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(), "Result type should be MATCH");
        assertNotNull(result.getTimestamp(), "Timestamp should not be null");
    }
}
