

# Implementation of RuleResult Features in SpelAdvancedFeaturesDemo

To demonstrate the features of RuleResult as outlined in the requirements, I need to add a new method to the SpelAdvancedFeaturesDemo class and update the main method to call it. Here's the implementation:

## 1. Add a New Method to Demonstrate RuleResult Features

Add the following method to the SpelAdvancedFeaturesDemo class:

```java
/**
 * Demonstrates RuleResult features and capabilities.
 * This method showcases the different result types, properties, and how to use
 * RuleResult for conditional rule execution.
 */
private void demonstrateRuleResultFeatures() {
    System.out.println("\n=== RuleResult Features and Capabilities ===");
    
    // Create context with test data
    StandardEvaluationContext context = new StandardEvaluationContext();
    Customer customer = dataServiceManager.requestData("customer");
    List<Product> products = dataServiceManager.requestData("products");
    context.setVariable("customer", customer);
    context.setVariable("products", products);
    context.setVariable("investmentAmount", 150000);
    context.setVariable("accountType", "retirement");
    context.setVariable("clientRiskScore", 8);
    context.setVariable("marketVolatility", 0.25);
    context.setVariable("kycVerified", false);
    
    System.out.println("\n1. Demonstrating RuleResult Types:");
    
    // 1. MATCH result type
    RuleResult matchResult = evaluatorService.evaluateWithResult(
        "#investmentAmount > 100000", context, Boolean.class);
    System.out.println("\nMATCH Result:");
    printRuleResultDetails(matchResult);
    
    // 2. NO_MATCH result type
    RuleResult noMatchResult = evaluatorService.evaluateWithResult(
        "#investmentAmount < 50000", context, Boolean.class);
    System.out.println("\nNO_MATCH Result:");
    printRuleResultDetails(noMatchResult);
    
    // 3. ERROR result type
    RuleResult errorResult = evaluatorService.evaluateWithResult(
        "#undefinedVariable > 100", context, Boolean.class);
    System.out.println("\nERROR Result:");
    printRuleResultDetails(errorResult);
    
    // 4. NO_RULES result type (create manually since it's hard to trigger naturally)
    RuleResult noRulesResult = RuleResult.noRules();
    System.out.println("\nNO_RULES Result:");
    printRuleResultDetails(noRulesResult);
    
    System.out.println("\n2. Using RuleResult for Conditional Rule Execution:");
    
    // Create a sequence of rules to demonstrate conditional execution
    Rule highValueRule = new Rule(
        "High-Value Investment",
        "#investmentAmount > 100000",
        "High-value investment detected"
    );
    
    Rule retirementAccountRule = new Rule(
        "Retirement Account",
        "#accountType == 'retirement'",
        "Retirement account detected"
    );
    
    Rule highRiskClientRule = new Rule(
        "High-Risk Client",
        "#clientRiskScore > 7",
        "High-risk client detected"
    );
    
    Rule volatileMarketRule = new Rule(
        "Volatile Market",
        "#marketVolatility > 0.2",
        "Volatile market conditions detected"
    );
    
    Rule kycVerificationRule = new Rule(
        "KYC Verification",
        "!#kycVerified",
        "KYC verification required"
    );
    
    // Demonstrate conditional execution based on triggered status
    System.out.println("\nConditional Execution Based on Triggered Status:");
    executeRuleWithConditionalFollowup(highValueRule, retirementAccountRule, context);
    
    // Demonstrate execution based on result type
    System.out.println("\nExecution Based on Result Type:");
    executeRuleBasedOnResultType(highRiskClientRule, context);
    
    // Demonstrate rule chaining using result message
    System.out.println("\nRule Chaining Using Result Message:");
    executeRuleChain(volatileMarketRule, kycVerificationRule, context);
    
    // Demonstrate dynamic rule selection
    System.out.println("\nDynamic Rule Selection:");
    executeDynamicRuleSelection(context);
}

/**
 * Helper method to print details of a RuleResult.
 */
private void printRuleResultDetails(RuleResult result) {
    System.out.println("  - ID: " + result.getId());
    System.out.println("  - Rule Name: " + result.getRuleName());
    System.out.println("  - Message: " + result.getMessage());
    System.out.println("  - Triggered: " + result.isTriggered());
    System.out.println("  - Result Type: " + result.getResultType());
    System.out.println("  - Timestamp: " + result.getTimestamp());
}

/**
 * Demonstrates conditional execution based on triggered status.
 */
private void executeRuleWithConditionalFollowup(Rule rule1, Rule rule2, EvaluationContext context) {
    // Create a list with the first rule
    List<Rule> rules = new ArrayList<>();
    rules.add(rule1);
    
    // Evaluate the first rule
    List<RuleResult> results = ruleEngineService.evaluateRules(rules, context);
    RuleResult result = results.get(0);
    
    System.out.println("Evaluated rule: " + rule1.getName());
    System.out.println("Result: " + (result.isTriggered() ? "Triggered" : "Not triggered"));
    
    // Conditional execution based on the result
    if (result.isTriggered()) {
        System.out.println("First rule triggered, executing second rule...");
        
        // Create a list with the second rule
        List<Rule> followupRules = new ArrayList<>();
        followupRules.add(rule2);
        
        // Evaluate the second rule
        List<RuleResult> followupResults = ruleEngineService.evaluateRules(followupRules, context);
        RuleResult followupResult = followupResults.get(0);
        
        System.out.println("Evaluated rule: " + rule2.getName());
        System.out.println("Result: " + (followupResult.isTriggered() ? "Triggered" : "Not triggered"));
    } else {
        System.out.println("First rule not triggered, skipping second rule.");
    }
}

/**
 * Demonstrates execution based on result type.
 */
private void executeRuleBasedOnResultType(Rule rule, EvaluationContext context) {
    // Create a list with the rule
    List<Rule> rules = new ArrayList<>();
    rules.add(rule);
    
    // Evaluate the rule
    List<RuleResult> results = ruleEngineService.evaluateRules(rules, context);
    RuleResult result = results.get(0);
    
    System.out.println("Evaluated rule: " + rule.getName());
    
    // Execute different actions based on result type
    switch (result.getResultType()) {
        case MATCH:
            System.out.println("MATCH result: Executing actions for successful match...");
            System.out.println("Action: Flagging client for enhanced due diligence");
            break;
        case NO_MATCH:
            System.out.println("NO_MATCH result: Executing default actions...");
            System.out.println("Action: Proceeding with standard processing");
            break;
        case ERROR:
            System.out.println("ERROR result: Executing error handling...");
            System.out.println("Action: Logging error and notifying administrator");
            break;
        case NO_RULES:
            System.out.println("NO_RULES result: No rules to execute");
            break;
    }
}

/**
 * Demonstrates rule chaining using result message.
 */
private void executeRuleChain(Rule rule1, Rule rule2, EvaluationContext context) {
    // Create a list with the first rule
    List<Rule> rules = new ArrayList<>();
    rules.add(rule1);
    
    // Evaluate the first rule
    List<RuleResult> results = ruleEngineService.evaluateRules(rules, context);
    RuleResult result = results.get(0);
    
    System.out.println("Evaluated rule: " + rule1.getName());
    
    // Use the message from the first rule to determine the next action
    if (result.isTriggered()) {
        System.out.println("First rule triggered with message: " + result.getMessage());
        
        // The message indicates market volatility, so we check KYC verification
        if (result.getMessage().contains("Volatile market")) {
            System.out.println("Market volatility detected, checking KYC verification status...");
            
            // Create a list with the second rule
            List<Rule> followupRules = new ArrayList<>();
            followupRules.add(rule2);
            
            // Evaluate the second rule
            List<RuleResult> followupResults = ruleEngineService.evaluateRules(followupRules, context);
            RuleResult followupResult = followupResults.get(0);
            
            System.out.println("Evaluated rule: " + rule2.getName());
            
            if (followupResult.isTriggered()) {
                System.out.println("KYC verification required. Investment on hold until verification complete.");
            } else {
                System.out.println("KYC verification complete. Proceeding with investment despite market volatility.");
            }
        }
    } else {
        System.out.println("Market conditions stable, proceeding with standard investment process.");
    }
}

/**
 * Demonstrates dynamic rule selection.
 */
private void executeDynamicRuleSelection(EvaluationContext context) {
    // Create a map of rules that can be selected dynamically
    Map<String, Rule> ruleRepository = new HashMap<>();
    ruleRepository.put("Rule-HighValue", new Rule(
        "High-Value Investment",
        "#investmentAmount > 100000",
        "High-value investment detected"
    ));
    ruleRepository.put("Rule-Retirement", new Rule(
        "Retirement Account",
        "#accountType == 'retirement'",
        "Retirement account detected"
    ));
    ruleRepository.put("Rule-HighRisk", new Rule(
        "High-Risk Client",
        "#clientRiskScore > 7",
        "High-risk client detected"
    ));
    
    // Start with a rule to determine the investment type
    Rule investmentTypeRule = new Rule(
        "Investment Type Determination",
        "#investmentAmount > 100000 ? 'HighValue' : (#accountType == 'retirement' ? 'Retirement' : 'Standard')",
        "Determining investment type"
    );
    
    // Evaluate the rule to determine which rule to execute next
    RuleResult result = evaluatorService.evaluateWithResult(
        investmentTypeRule.getCondition(), context, String.class);
    
    System.out.println("Evaluated rule: " + investmentTypeRule.getName());
    
    if (result.isTriggered()) {
        // Use the result to dynamically select the next rule
        String investmentType = evaluatorService.evaluate(
            investmentTypeRule.getCondition(), context, String.class);
        
        System.out.println("Investment type determined: " + investmentType);
        
        String nextRuleName = "Rule-" + investmentType;
        Rule nextRule = ruleRepository.get(nextRuleName);
        
        if (nextRule != null) {
            System.out.println("Dynamically selected rule: " + nextRule.getName());
            
            // Create a list with the selected rule
            List<Rule> selectedRules = new ArrayList<>();
            selectedRules.add(nextRule);
            
            // Evaluate the selected rule
            List<RuleResult> selectedResults = ruleEngineService.evaluateRules(selectedRules, context);
            RuleResult selectedResult = selectedResults.get(0);
            
            System.out.println("Result: " + (selectedResult.isTriggered() ? "Triggered" : "Not triggered"));
            if (selectedResult.isTriggered()) {
                System.out.println("Message: " + selectedResult.getMessage());
            }
        } else {
            System.out.println("No rule found for investment type: " + investmentType);
        }
    } else {
        System.out.println("Could not determine investment type.");
    }
}
```

## 2. Update the Main Method

Add a call to the new method in the main method:

```java
public static void main(String[] args) {
    // Create services
    ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
    RuleEngineService ruleEngineService = new RuleEngineService(evaluatorService);
    TemplateProcessorService templateProcessorService = new TemplateProcessorService(evaluatorService);
    PricingServiceDemo pricingService = new PricingServiceDemo();

    // Create main class with injected services
    SpelAdvancedFeaturesDemo spelAdvancedFeaturesDemo = new SpelAdvancedFeaturesDemo(
        evaluatorService,
        ruleEngineService,
        templateProcessorService
    );

    // Example 1: Collection and array operations
    spelAdvancedFeaturesDemo.demonstrateCollectionOperations();

    // Example 2: Advanced rule engine with collection filtering
    spelAdvancedFeaturesDemo.demonstrateAdvancedRuleEngine();

    // Example 3: Dynamic method resolution and execution
    spelAdvancedFeaturesDemo.demonstrateDynamicMethodExecution(pricingService);

    // Example 4: Template expressions with placeholders
    spelAdvancedFeaturesDemo.demonstrateTemplateExpressions();

    // Example 5: XML template expressions with placeholders
    spelAdvancedFeaturesDemo.demonstrateXmlTemplateExpressions();

    // Example 6: JSON template expressions with placeholders
    spelAdvancedFeaturesDemo.demonstrateJsonTemplateExpressions();

    // Example 7: Dynamic lookup service
    spelAdvancedFeaturesDemo.demonstrateDynamicLookupService();
    
    // Example 8: RuleResult features and capabilities
    spelAdvancedFeaturesDemo.demonstrateRuleResultFeatures();
}
```

## Features Demonstrated

This implementation demonstrates all the required RuleResult features:

1. **RuleResult Types**:
   - MATCH: Shows a successful rule match
   - NO_MATCH: Shows when no rule is matched
   - NO_RULES: Shows when no rules are provided
   - ERROR: Shows when an error occurs during evaluation

2. **RuleResult Properties**:
   - Rule Identification (ruleName)
   - Evaluation Status (triggered)
   - Result Message (message)
   - Result Type (resultType)
   - Metadata (id and timestamp)

3. **Conditional Rule Execution**:
   - Conditional Execution Based on Triggered Status
   - Execution Based on Result Type
   - Rule Chaining Using Result Message
   - Dynamic Rule Selection

This implementation provides a comprehensive demonstration of RuleResult capabilities and how they can be used to create sophisticated rule-based workflows.