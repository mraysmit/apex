# Engine Executor Testing Examples and Best Practices

## Overview

This document provides practical examples and best practices for testing each executor pattern in the Apex Rules Engine. It complements the main testing methodology document with concrete code examples and implementation guidance.

## Quick Reference: Executor Pattern Testing

| Pattern | Primary Focus | Key Test Scenarios | Configuration Complexity |
|---------|---------------|-------------------|-------------------------|
| **RuleChainExecutor** | Pattern routing | Pattern enumeration, routing logic, error handling | Low |
| **ConditionalChainingExecutor** | Trigger-based branching | Trigger evaluation, conditional paths | Medium |
| **SequentialDependencyExecutor** | Stage dependencies | Sequential execution, dependency validation | Medium |
| **ResultBasedRoutingExecutor** | Dynamic routing | Route determination, path execution | Medium |
| **AccumulativeChainingExecutor** | Score accumulation | Weighted calculations, accumulation logic | High |
| **ComplexWorkflowExecutor** | Multi-stage workflows | Workflow orchestration, complex dependencies | High |
| **FluentBuilderExecutor** | Rule tree navigation | Tree traversal, fluent API composition | Medium |

## Pattern-Specific Testing Examples

### 1. RuleChainExecutor - Foundation Testing

**Purpose**: Validates the core routing mechanism that delegates to pattern-specific executors.

```java
@Test
@DisplayName("Should route to all supported patterns without errors")
void testComprehensivePatternRouting() {
    String[] supportedPatterns = ruleChainExecutor.getSupportedPatterns();
    
    for (String pattern : supportedPatterns) {
        YamlRuleChain ruleChain = RuleChainTestBuilder.create()
            .withId("routing-test-" + pattern)
            .withPattern(pattern)
            .withConfiguration(createMinimalConfigForPattern(pattern))
            .build();
        
        assertDoesNotThrow(() -> {
            RuleChainResult result = ruleChainExecutor.executeRuleChain(ruleChain, context);
            assertNotNull(result, "Result should not be null for pattern: " + pattern);
            assertEquals(pattern, result.getPattern(), "Result should indicate correct pattern");
        }, "Should handle pattern: " + pattern + " without throwing exceptions");
    }
}

@Test
@DisplayName("Should handle malformed rule chain gracefully")
void testMalformedRuleChainHandling() {
    // Test with null pattern
    YamlRuleChain nullPatternChain = RuleChainTestBuilder.create()
        .withId("null-pattern-test")
        .withPattern(null)
        .build();
    
    assertDoesNotThrow(() -> {
        RuleChainResult result = ruleChainExecutor.executeRuleChain(nullPatternChain, context);
        assertNotNull(result, "Should return result even with null pattern");
    });
    
    // Test with unsupported pattern
    YamlRuleChain unsupportedPatternChain = RuleChainTestBuilder.create()
        .withId("unsupported-pattern-test")
        .withPattern("non-existent-pattern")
        .build();
    
    assertDoesNotThrow(() -> {
        RuleChainResult result = ruleChainExecutor.executeRuleChain(unsupportedPatternChain, context);
        assertNotNull(result, "Should return result even with unsupported pattern");
    });
}
```

### 2. ConditionalChainingExecutor - Branching Logic Testing

**Purpose**: Validates trigger-based conditional execution paths.

```java
@Test
@DisplayName("Should execute different paths based on trigger evaluation")
void testConditionalPathExecution() {
    // Test scenario: High-value transaction processing
    YamlRuleChain ruleChain = RuleChainTestBuilder.create()
        .withPattern("conditional-chaining")
        .withConfiguration(createHighValueTransactionConfig())
        .build();
    
    // Test high-value scenario (trigger fires)
    context.setVariable("transactionAmount", 150000);
    context.setVariable("customerTier", "PREMIUM");
    
    RuleChainResult highValueResult = conditionalExecutor.execute(ruleChain, 
        ruleChain.getConfiguration(), context);
    
    assertNotNull(highValueResult, "High-value result should not be null");
    assertTrue(highValueResult.isSuccessful(), "High-value processing should succeed");
    
    // Verify enhanced validation was triggered
    assertTrue(context.hasVariable("enhancedValidationRequired"), 
              "Enhanced validation should be triggered for high-value transactions");
    
    // Test low-value scenario (trigger doesn't fire)
    ChainedEvaluationContext lowValueContext = new ChainedEvaluationContext();
    lowValueContext.setVariable("transactionAmount", 5000);
    lowValueContext.setVariable("customerTier", "STANDARD");
    
    RuleChainResult lowValueResult = conditionalExecutor.execute(ruleChain, 
        ruleChain.getConfiguration(), lowValueContext);
    
    assertNotNull(lowValueResult, "Low-value result should not be null");
    assertTrue(lowValueResult.isSuccessful(), "Low-value processing should succeed");
    
    // Verify standard processing was used
    assertFalse(lowValueContext.hasVariable("enhancedValidationRequired"), 
               "Enhanced validation should not be triggered for low-value transactions");
}

private Map<String, Object> createHighValueTransactionConfig() {
    Map<String, Object> config = new HashMap<>();
    
    // Trigger rule: High-value transaction detection
    Map<String, Object> triggerRule = new HashMap<>();
    triggerRule.put("id", "high-value-detection");
    triggerRule.put("condition", "#transactionAmount > 100000");
    triggerRule.put("message", "High-value transaction detected");
    config.put("trigger-rule", triggerRule);
    
    // Conditional rules
    Map<String, Object> conditionalRules = new HashMap<>();
    
    // On-trigger: Enhanced validation required
    List<Map<String, Object>> onTriggerRules = new ArrayList<>();
    Map<String, Object> enhancedValidation = new HashMap<>();
    enhancedValidation.put("id", "enhanced-validation");
    enhancedValidation.put("condition", "true");
    enhancedValidation.put("message", "Enhanced validation required");
    enhancedValidation.put("action", "setVariable('enhancedValidationRequired', true)");
    onTriggerRules.add(enhancedValidation);
    
    // Additional compliance check for premium customers
    Map<String, Object> complianceCheck = new HashMap<>();
    complianceCheck.put("id", "compliance-check");
    complianceCheck.put("condition", "#customerTier == 'PREMIUM'");
    complianceCheck.put("message", "Premium customer compliance check");
    complianceCheck.put("action", "setVariable('complianceCheckRequired', true)");
    onTriggerRules.add(complianceCheck);
    
    conditionalRules.put("on-trigger", onTriggerRules);
    
    // On-no-trigger: Standard processing
    List<Map<String, Object>> onNoTriggerRules = new ArrayList<>();
    Map<String, Object> standardProcessing = new HashMap<>();
    standardProcessing.put("id", "standard-processing");
    standardProcessing.put("condition", "true");
    standardProcessing.put("message", "Standard transaction processing");
    standardProcessing.put("action", "setVariable('standardProcessing', true)");
    onNoTriggerRules.add(standardProcessing);
    
    conditionalRules.put("on-no-trigger", onNoTriggerRules);
    
    config.put("conditional-rules", conditionalRules);
    return config;
}
```

### 3. SequentialDependencyExecutor - Pipeline Testing

**Purpose**: Validates sequential execution with proper dependency management.

```java
@Test
@DisplayName("Should execute loan approval pipeline with proper dependencies")
void testLoanApprovalPipeline() {
    YamlRuleChain loanPipeline = RuleChainTestBuilder.create()
        .withPattern("sequential-dependency")
        .withConfiguration(createLoanApprovalPipelineConfig())
        .build();
    
    // Set up loan application context
    context.setVariable("applicantAge", 35);
    context.setVariable("annualIncome", 75000);
    context.setVariable("creditScore", 720);
    context.setVariable("requestedAmount", 250000);
    context.setVariable("employmentYears", 8);
    
    RuleChainResult result = sequentialExecutor.execute(loanPipeline, 
        loanPipeline.getConfiguration(), context);
    
    assertNotNull(result, "Pipeline result should not be null");
    assertTrue(result.isSuccessful(), "Loan approval pipeline should succeed");
    
    // Verify stage execution order and dependencies
    assertTrue(context.hasVariable("eligibilityScore"), 
              "Eligibility assessment should be completed");
    assertTrue(context.hasVariable("riskCategory"), 
              "Risk assessment should be completed");
    assertTrue(context.hasVariable("approvedAmount"), 
              "Loan amount calculation should be completed");
    assertTrue(context.hasVariable("finalDecision"), 
              "Final approval decision should be made");
    
    // Verify dependency chain
    Object eligibilityScore = context.getVariable("eligibilityScore");
    Object riskCategory = context.getVariable("riskCategory");
    Object finalDecision = context.getVariable("finalDecision");
    
    assertNotNull(eligibilityScore, "Eligibility score should be calculated");
    assertNotNull(riskCategory, "Risk category should be determined");
    assertNotNull(finalDecision, "Final decision should be made");
    
    // Verify business logic
    assertTrue((Integer) eligibilityScore >= 60, "Eligibility score should meet minimum threshold");
    assertEquals("LOW", riskCategory, "Risk category should be LOW for this profile");
    assertEquals("APPROVED", finalDecision, "Loan should be approved for qualified applicant");
}

private Map<String, Object> createLoanApprovalPipelineConfig() {
    Map<String, Object> config = new HashMap<>();
    List<Object> stages = new ArrayList<>();
    
    // Stage 1: Eligibility Assessment
    Map<String, Object> eligibilityStage = new HashMap<>();
    eligibilityStage.put("stage", 1);
    eligibilityStage.put("name", "Eligibility Assessment");
    eligibilityStage.put("description", "Calculate applicant eligibility score");
    
    Map<String, Object> eligibilityRule = new HashMap<>();
    eligibilityRule.put("id", "eligibility-calculation");
    eligibilityRule.put("condition", 
        "(#applicantAge >= 18 && #applicantAge <= 65 ? 20 : 0) + " +
        "(#annualIncome >= 50000 ? 25 : (#annualIncome >= 30000 ? 15 : 0)) + " +
        "(#creditScore >= 700 ? 30 : (#creditScore >= 600 ? 20 : 10)) + " +
        "(#employmentYears >= 5 ? 25 : (#employmentYears >= 2 ? 15 : 5))");
    eligibilityRule.put("message", "Eligibility score calculated");
    eligibilityStage.put("rule", eligibilityRule);
    eligibilityStage.put("output-variable", "eligibilityScore");
    stages.add(eligibilityStage);
    
    // Stage 2: Risk Assessment (depends on eligibility)
    Map<String, Object> riskStage = new HashMap<>();
    riskStage.put("stage", 2);
    riskStage.put("name", "Risk Assessment");
    riskStage.put("description", "Determine risk category based on eligibility");
    riskStage.put("depends-on", Arrays.asList("eligibilityScore"));
    riskStage.put("dependency-condition", "#eligibilityScore >= 60");
    
    Map<String, Object> riskRule = new HashMap<>();
    riskRule.put("id", "risk-categorization");
    riskRule.put("condition", 
        "#eligibilityScore >= 85 ? 'LOW' : " +
        "(#eligibilityScore >= 70 ? 'MEDIUM' : 'HIGH')");
    riskRule.put("message", "Risk category determined");
    riskStage.put("rule", riskRule);
    riskStage.put("output-variable", "riskCategory");
    stages.add(riskStage);
    
    // Stage 3: Loan Amount Calculation (depends on risk assessment)
    Map<String, Object> amountStage = new HashMap<>();
    amountStage.put("stage", 3);
    amountStage.put("name", "Loan Amount Calculation");
    amountStage.put("description", "Calculate approved loan amount");
    amountStage.put("depends-on", Arrays.asList("riskCategory"));
    
    Map<String, Object> amountRule = new HashMap<>();
    amountRule.put("id", "amount-calculation");
    amountRule.put("condition", 
        "#riskCategory == 'LOW' ? (#requestedAmount <= #annualIncome * 5 ? #requestedAmount : #annualIncome * 5) : " +
        "(#riskCategory == 'MEDIUM' ? (#requestedAmount <= #annualIncome * 3 ? #requestedAmount : #annualIncome * 3) : " +
        "(#requestedAmount <= #annualIncome * 2 ? #requestedAmount : #annualIncome * 2))");
    amountRule.put("message", "Approved loan amount calculated");
    amountStage.put("rule", amountRule);
    amountStage.put("output-variable", "approvedAmount");
    stages.add(amountStage);
    
    // Stage 4: Final Decision (depends on all previous stages)
    Map<String, Object> decisionStage = new HashMap<>();
    decisionStage.put("stage", 4);
    decisionStage.put("name", "Final Decision");
    decisionStage.put("description", "Make final approval decision");
    decisionStage.put("depends-on", Arrays.asList("eligibilityScore", "riskCategory", "approvedAmount"));
    
    Map<String, Object> decisionRule = new HashMap<>();
    decisionRule.put("id", "final-decision");
    decisionRule.put("condition", 
        "#eligibilityScore >= 60 && #approvedAmount > 0 ? 'APPROVED' : 'REJECTED'");
    decisionRule.put("message", "Final loan decision made");
    decisionStage.put("rule", decisionRule);
    decisionStage.put("output-variable", "finalDecision");
    stages.add(decisionStage);
    
    config.put("stages", stages);
    return config;
}
```

### 4. ResultBasedRoutingExecutor - Dynamic Routing Testing

**Purpose**: Validates dynamic routing based on rule evaluation results.

```java
@Test
@DisplayName("Should route customer service requests based on priority and type")
void testCustomerServiceRouting() {
    YamlRuleChain routingChain = RuleChainTestBuilder.create()
        .withPattern("result-based-routing")
        .withConfiguration(createCustomerServiceRoutingConfig())
        .build();
    
    // Test high-priority technical issue (should route to technical-escalation)
    context.setVariable("issueType", "TECHNICAL");
    context.setVariable("customerTier", "PREMIUM");
    context.setVariable("issueComplexity", "HIGH");
    context.setVariable("previousEscalations", 2);
    
    RuleChainResult technicalResult = routingExecutor.execute(routingChain, 
        routingChain.getConfiguration(), context);
    
    assertNotNull(technicalResult, "Technical routing result should not be null");
    assertTrue(technicalResult.isSuccessful(), "Technical routing should succeed");
    
    // Verify correct routing occurred
    assertTrue(context.hasVariable("routingDecision"), "Routing decision should be made");
    assertEquals("technical-escalation", context.getVariable("routingDecision"), 
                "Should route to technical escalation");
    
    // Test billing inquiry (should route to billing-support)
    ChainedEvaluationContext billingContext = new ChainedEvaluationContext();
    billingContext.setVariable("issueType", "BILLING");
    billingContext.setVariable("customerTier", "STANDARD");
    billingContext.setVariable("issueComplexity", "LOW");
    billingContext.setVariable("disputeAmount", 150.00);
    
    RuleChainResult billingResult = routingExecutor.execute(routingChain, 
        routingChain.getConfiguration(), billingContext);
    
    assertNotNull(billingResult, "Billing routing result should not be null");
    assertTrue(billingResult.isSuccessful(), "Billing routing should succeed");
    
    assertEquals("billing-support", billingContext.getVariable("routingDecision"), 
                "Should route to billing support");
    
    // Test general inquiry (should route to general-support)
    ChainedEvaluationContext generalContext = new ChainedEvaluationContext();
    generalContext.setVariable("issueType", "GENERAL");
    generalContext.setVariable("customerTier", "BASIC");
    generalContext.setVariable("issueComplexity", "LOW");
    
    RuleChainResult generalResult = routingExecutor.execute(routingChain, 
        routingChain.getConfiguration(), generalContext);
    
    assertNotNull(generalResult, "General routing result should not be null");
    assertTrue(generalResult.isSuccessful(), "General routing should succeed");
    
    assertEquals("general-support", generalContext.getVariable("routingDecision"), 
                "Should route to general support");
}

private Map<String, Object> createCustomerServiceRoutingConfig() {
    Map<String, Object> config = new HashMap<>();
    
    // Router rule that determines the support queue
    Map<String, Object> routerRule = new HashMap<>();
    routerRule.put("id", "support-queue-router");
    routerRule.put("condition", 
        "#issueType == 'TECHNICAL' && (#customerTier == 'PREMIUM' || #issueComplexity == 'HIGH') ? 'technical-escalation' : " +
        "(#issueType == 'BILLING' ? 'billing-support' : " +
        "(#issueType == 'ACCOUNT' && #customerTier == 'PREMIUM' ? 'premium-support' : 'general-support'))");
    routerRule.put("message", "Support queue routing decision");
    config.put("router-rule", routerRule);
    
    // Define routes for different support queues
    Map<String, Object> routes = new HashMap<>();
    
    // Technical escalation route
    List<Map<String, Object>> technicalRules = new ArrayList<>();
    Map<String, Object> technicalAssignment = new HashMap<>();
    technicalAssignment.put("id", "technical-assignment");
    technicalAssignment.put("condition", "true");
    technicalAssignment.put("message", "Assigned to technical escalation team");
    technicalAssignment.put("action", "setVariable('assignedTeam', 'TECHNICAL_ESCALATION')");
    technicalRules.add(technicalAssignment);
    routes.put("technical-escalation", technicalRules);
    
    // Billing support route
    List<Map<String, Object>> billingRules = new ArrayList<>();
    Map<String, Object> billingAssignment = new HashMap<>();
    billingAssignment.put("id", "billing-assignment");
    billingAssignment.put("condition", "true");
    billingAssignment.put("message", "Assigned to billing support team");
    billingAssignment.put("action", "setVariable('assignedTeam', 'BILLING_SUPPORT')");
    billingRules.add(billingAssignment);
    routes.put("billing-support", billingRules);
    
    // Premium support route
    List<Map<String, Object>> premiumRules = new ArrayList<>();
    Map<String, Object> premiumAssignment = new HashMap<>();
    premiumAssignment.put("id", "premium-assignment");
    premiumAssignment.put("condition", "true");
    premiumAssignment.put("message", "Assigned to premium support team");
    premiumAssignment.put("action", "setVariable('assignedTeam', 'PREMIUM_SUPPORT')");
    premiumRules.add(premiumAssignment);
    routes.put("premium-support", premiumRules);
    
    // General support route (default)
    List<Map<String, Object>> generalRules = new ArrayList<>();
    Map<String, Object> generalAssignment = new HashMap<>();
    generalAssignment.put("id", "general-assignment");
    generalAssignment.put("condition", "true");
    generalAssignment.put("message", "Assigned to general support team");
    generalAssignment.put("action", "setVariable('assignedTeam', 'GENERAL_SUPPORT')");
    generalRules.add(generalAssignment);
    routes.put("general-support", generalRules);
    
    config.put("routes", routes);
    return config;
}
```

## Testing Best Practices Summary

### 1. **Configuration Management**
- Create reusable configuration templates for different complexity levels
- Use builder patterns for test data creation
- Maintain separate configurations for positive and negative test cases

### 2. **Context Management**
- Use fresh context instances for each test to avoid state pollution
- Set up realistic test data that reflects actual business scenarios
- Verify context state changes after execution

### 3. **Assertion Strategies**
- Test both successful execution and error handling paths
- Verify business logic outcomes, not just technical execution
- Use descriptive assertion messages that explain expected behavior

### 4. **Error Testing**
- Include intentional error logging for expected failure scenarios
- Test null safety and edge cases comprehensively
- Validate graceful degradation under error conditions

### 5. **Performance Considerations**
- Include performance benchmarks for complex scenarios
- Test memory usage patterns for long-running executions
- Validate concurrent execution safety

This comprehensive testing approach ensures robust, maintainable test coverage for all executor patterns while providing clear examples for future development and maintenance.
