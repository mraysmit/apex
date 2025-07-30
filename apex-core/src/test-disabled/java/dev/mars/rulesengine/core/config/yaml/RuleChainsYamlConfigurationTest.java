package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.engine.context.ChainedEvaluationContext;
import dev.mars.apex.core.engine.executor.RuleChainExecutor;
import dev.mars.apex.core.engine.model.RuleChainResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.engine.RuleEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for YAML configuration of rule chains with nested rules and rule chaining patterns.
 * Tests the first 3 implemented patterns: conditional-chaining, sequential-dependency, result-based-routing.
 */
public class RuleChainsYamlConfigurationTest {

    private YamlConfigurationLoader loader;
    private RuleChainExecutor ruleChainExecutor;
    private ExpressionEvaluatorService evaluatorService;
    private RuleEngineService ruleEngineService;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
        evaluatorService = new ExpressionEvaluatorService();
        ruleEngineService = new RuleEngineService(evaluatorService);
        ruleChainExecutor = new RuleChainExecutor(ruleEngineService, evaluatorService);
    }

    @Test
    void testConditionalChainingYamlConfiguration() throws YamlConfigurationException {
        System.out.println("=== Testing Conditional Chaining YAML Configuration ===");
        
        String yamlContent = """
            metadata:
              name: "Conditional Chaining Test Configuration"
              version: "1.0.0"
              description: "Test configuration for conditional chaining pattern"
            
            rule-chains:
              - id: "high-value-processing"
                name: "High Value Transaction Processing"
                description: "Execute enhanced validation only for high-value customers"
                pattern: "conditional-chaining"
                enabled: true
                priority: 10
                configuration:
                  trigger-rule:
                    id: "high-value-check"
                    condition: "#customerType == 'PREMIUM' && #transactionAmount > 100000"
                    message: "High-value customer transaction detected"
                  conditional-rules:
                    on-trigger:
                      - id: "enhanced-due-diligence"
                        condition: "#accountAge >= 3"
                        message: "Enhanced due diligence check passed"
                    on-no-trigger:
                      - id: "standard-processing"
                        condition: "true"
                        message: "Standard processing applied"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        assertNotNull(yamlConfig);
        assertNotNull(yamlConfig.getRuleChains());
        assertEquals(1, yamlConfig.getRuleChains().size());

        YamlRuleChain ruleChain = yamlConfig.getRuleChains().get(0);
        assertEquals("high-value-processing", ruleChain.getId());
        assertEquals("conditional-chaining", ruleChain.getPattern());
        assertTrue(ruleChain.isEnabled());

        // Test execution with high-value customer (trigger path)
        Map<String, Object> context = Map.of(
            "customerType", "PREMIUM",
            "transactionAmount", new BigDecimal("150000"),
            "accountAge", 5
        );

        ChainedEvaluationContext chainedContext = new ChainedEvaluationContext(context);
        RuleChainResult result = ruleChainExecutor.executeRuleChain(ruleChain, chainedContext);

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals("TRIGGERED_PATH_COMPLETED", result.getFinalOutcome());
        assertTrue(result.hasStageResult("triggerResult"));
        assertTrue((Boolean) result.getStageResult("triggerResult"));

        System.out.println("✓ Conditional chaining executed successfully");
        System.out.println("  Execution path: " + String.join(" → ", result.getExecutionPath()));
        System.out.println("  Final outcome: " + result.getFinalOutcome());
    }

    @Test
    void testSequentialDependencyYamlConfiguration() throws YamlConfigurationException {
        System.out.println("\n=== Testing Sequential Dependency YAML Configuration ===");
        
        String yamlContent = """
            metadata:
              name: "Sequential Dependency Test Configuration"
              version: "1.0.0"
            
            rule-chains:
              - id: "discount-calculation-pipeline"
                name: "Discount Calculation Pipeline"
                pattern: "sequential-dependency"
                enabled: true
                configuration:
                  stages:
                    - stage: 1
                      name: "Base Discount Calculation"
                      rule:
                        id: "base-discount"
                        condition: "#customerTier == 'GOLD' ? 0.15 : (#customerTier == 'SILVER' ? 0.10 : 0.05)"
                        message: "Base discount calculated"
                      output-variable: "baseDiscount"
                    - stage: 2
                      name: "Regional Multiplier"
                      rule:
                        id: "regional-multiplier"
                        condition: "#region == 'US' ? #baseDiscount * 1.2 : #baseDiscount"
                        message: "Regional multiplier applied"
                      output-variable: "finalDiscount"
                    - stage: 3
                      name: "Final Amount Calculation"
                      rule:
                        id: "final-amount"
                        condition: "#baseAmount * (1 - #finalDiscount)"
                        message: "Final amount calculated"
                      output-variable: "finalAmount"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        YamlRuleChain ruleChain = yamlConfig.getRuleChains().get(0);

        // Test execution with sequential dependency
        Map<String, Object> context = Map.of(
            "baseAmount", new BigDecimal("100000"),
            "customerTier", "GOLD",
            "region", "US"
        );

        ChainedEvaluationContext chainedContext = new ChainedEvaluationContext(context);
        RuleChainResult result = ruleChainExecutor.executeRuleChain(ruleChain, chainedContext);

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals("SEQUENTIAL_PIPELINE_COMPLETED", result.getFinalOutcome());
        
        // Verify stage results
        assertTrue(result.hasStageResult("baseDiscount"));
        assertTrue(result.hasStageResult("finalDiscount"));
        assertTrue(result.hasStageResult("finalAmount"));
        
        // Verify calculations
        assertEquals(0.15, ((Number) result.getStageResult("baseDiscount")).doubleValue(), 0.001);
        assertEquals(0.18, ((Number) result.getStageResult("finalDiscount")).doubleValue(), 0.001);
        assertEquals(82000.0, ((Number) result.getStageResult("finalAmount")).doubleValue(), 0.1);

        System.out.println("✓ Sequential dependency executed successfully");
        System.out.println("  Base discount: " + result.getStageResult("baseDiscount"));
        System.out.println("  Final discount: " + result.getStageResult("finalDiscount"));
        System.out.println("  Final amount: " + result.getStageResult("finalAmount"));
    }

    @Test
    void testResultBasedRoutingYamlConfiguration() throws YamlConfigurationException {
        System.out.println("\n=== Testing Result-Based Routing YAML Configuration ===");
        
        String yamlContent = """
            metadata:
              name: "Result-Based Routing Test Configuration"
              version: "1.0.0"
            
            rule-chains:
              - id: "risk-based-routing"
                name: "Risk-Based Processing Router"
                pattern: "result-based-routing"
                enabled: true
                configuration:
                  router-rule:
                    id: "risk-assessment"
                    condition: "#riskScore > 70 ? 'HIGH_RISK' : (#riskScore > 30 ? 'MEDIUM_RISK' : 'LOW_RISK')"
                    message: "Risk level determined"
                    output-variable: "riskLevel"
                  routes:
                    HIGH_RISK:
                      rules:
                        - id: "manager-approval-required"
                          condition: "#transactionAmount > 100000"
                          message: "Manager approval required"
                        - id: "compliance-review-required"
                          condition: "#riskScore > 80"
                          message: "Compliance review required"
                    MEDIUM_RISK:
                      rules:
                        - id: "auto-approval-limit"
                          condition: "#transactionAmount <= 50000"
                          message: "Within auto-approval limit"
                    LOW_RISK:
                      rules:
                        - id: "basic-validation"
                          condition: "#transactionAmount > 0"
                          message: "Basic validation check"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        YamlRuleChain ruleChain = yamlConfig.getRuleChains().get(0);

        // Test execution with high-risk scenario
        Map<String, Object> context = Map.of(
            "riskScore", 85,
            "transactionAmount", new BigDecimal("500000"),
            "customerHistory", "EXCELLENT"
        );

        ChainedEvaluationContext chainedContext = new ChainedEvaluationContext(context);
        RuleChainResult result = ruleChainExecutor.executeRuleChain(ruleChain, chainedContext);

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals("ROUTE_HIGH_RISK_COMPLETED", result.getFinalOutcome());
        
        // Verify routing
        assertTrue(result.hasStageResult("routeKey"));
        assertEquals("HIGH_RISK", result.getStageResult("routeKey"));
        assertTrue(result.hasStageResult("riskLevel"));
        assertEquals("HIGH_RISK", result.getStageResult("riskLevel"));

        System.out.println("✓ Result-based routing executed successfully");
        System.out.println("  Route determined: " + result.getStageResult("routeKey"));
        System.out.println("  Execution path: " + String.join(" → ", result.getExecutionPath()));
        System.out.println("  Final outcome: " + result.getFinalOutcome());
    }

    @Test
    void testRuleChainValidation() {
        System.out.println("\n=== Testing Rule Chain Validation ===");
        
        // Test validation of supported patterns
        assertTrue(ruleChainExecutor.validateRuleChain(createValidRuleChain()));
        
        // Test validation failures
        assertFalse(ruleChainExecutor.validateRuleChain(null));
        assertFalse(ruleChainExecutor.validateRuleChain(createInvalidRuleChain()));
        
        System.out.println("✓ Rule chain validation working correctly");
    }

    @Test
    void testAccumulativeChainingYamlConfiguration() throws YamlConfigurationException {
        System.out.println("\n=== Testing Accumulative Chaining YAML Configuration ===");

        String yamlContent = """
            metadata:
              name: "Accumulative Chaining Test Configuration"
              version: "1.0.0"

            rule-chains:
              - id: "credit-scoring-accumulation"
                name: "Credit Score Accumulation"
                description: "Build up credit score across multiple criteria"
                pattern: "accumulative-chaining"
                enabled: true
                priority: 40
                configuration:
                  accumulator-variable: "totalScore"
                  initial-value: 0
                  accumulation-rules:
                    - id: "credit-score-component"
                      condition: "#creditScore >= 700 ? 25 : (#creditScore >= 650 ? 15 : (#creditScore >= 600 ? 10 : 0))"
                      message: "Credit score component calculated"
                      weight: 1.0
                    - id: "income-component"
                      condition: "#annualIncome >= 80000 ? 20 : (#annualIncome >= 60000 ? 15 : (#annualIncome >= 40000 ? 10 : 0))"
                      message: "Income component calculated"
                      weight: 1.0
                    - id: "employment-component"
                      condition: "#employmentYears >= 5 ? 15 : (#employmentYears >= 2 ? 10 : 5)"
                      message: "Employment stability component"
                      weight: 1.0
                    - id: "debt-ratio-component"
                      condition: "(#existingDebt / #annualIncome) < 0.2 ? 10 : ((#existingDebt / #annualIncome) < 0.4 ? 0 : -10)"
                      message: "Debt-to-income ratio component"
                      weight: 1.0
                  final-decision-rule:
                    id: "loan-decision"
                    condition: "#totalScore >= 60 ? 'APPROVED' : (#totalScore >= 40 ? 'CONDITIONAL' : 'DENIED')"
                    message: "Final loan decision based on accumulated score"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        assertNotNull(yamlConfig);
        assertNotNull(yamlConfig.getRuleChains());
        assertEquals(1, yamlConfig.getRuleChains().size());

        YamlRuleChain ruleChain = yamlConfig.getRuleChains().get(0);
        assertEquals("credit-scoring-accumulation", ruleChain.getId());
        assertEquals("accumulative-chaining", ruleChain.getPattern());
        assertTrue(ruleChain.isEnabled());

        // Test execution with good credit profile (should get approved)
        Map<String, Object> context = Map.of(
            "creditScore", 750,
            "annualIncome", new BigDecimal("85000"),
            "employmentYears", 8,
            "existingDebt", new BigDecimal("15000")
        );

        ChainedEvaluationContext chainedContext = new ChainedEvaluationContext(context);
        RuleChainResult result = ruleChainExecutor.executeRuleChain(ruleChain, chainedContext);

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals("APPROVED", result.getFinalOutcome());

        // Verify accumulator results
        assertTrue(result.hasStageResult("totalScore_initial"));
        assertTrue(result.hasStageResult("totalScore_final"));
        assertTrue(result.hasStageResult("finalDecision"));

        // Verify initial value
        assertEquals(0, ((Number) result.getStageResult("totalScore_initial")).intValue());

        // Verify final decision
        assertEquals("APPROVED", result.getStageResult("finalDecision"));

        // Verify final score (should be high enough for approval)
        double finalScore = ((Number) result.getStageResult("totalScore_final")).doubleValue();
        assertTrue(finalScore >= 60, "Final score should be >= 60 for approval, was: " + finalScore);

        // Verify component scores exist
        assertTrue(result.hasStageResult("component_1_credit-score-component_score"));
        assertTrue(result.hasStageResult("component_2_income-component_score"));
        assertTrue(result.hasStageResult("component_3_employment-component_score"));
        assertTrue(result.hasStageResult("component_4_debt-ratio-component_score"));

        System.out.println("✓ Accumulative chaining executed successfully");
        System.out.println("  Initial score: " + result.getStageResult("totalScore_initial"));
        System.out.println("  Final score: " + result.getStageResult("totalScore_final"));
        System.out.println("  Final decision: " + result.getStageResult("finalDecision"));
        System.out.println("  Credit component: " + result.getStageResult("component_1_credit-score-component_score"));
        System.out.println("  Income component: " + result.getStageResult("component_2_income-component_score"));
        System.out.println("  Employment component: " + result.getStageResult("component_3_employment-component_score"));
        System.out.println("  Debt ratio component: " + result.getStageResult("component_4_debt-ratio-component_score"));
    }

    @Test
    void testAccumulativeChainingConditionalApproval() throws YamlConfigurationException {
        System.out.println("\n=== Testing Accumulative Chaining Conditional Approval ===");

        String yamlContent = """
            metadata:
              name: "Accumulative Chaining Conditional Test"
              version: "1.0.0"

            rule-chains:
              - id: "credit-scoring-conditional"
                name: "Credit Score Conditional Test"
                pattern: "accumulative-chaining"
                enabled: true
                configuration:
                  accumulator-variable: "totalScore"
                  initial-value: 10
                  accumulation-rules:
                    - id: "credit-score-component"
                      condition: "#creditScore >= 650 ? 15 : 5"
                      message: "Credit score component"
                      weight: 1.0
                    - id: "income-component"
                      condition: "#annualIncome >= 60000 ? 15 : 10"
                      message: "Income component"
                      weight: 1.0
                    - id: "employment-component"
                      condition: "#employmentYears >= 2 ? 10 : 5"
                      message: "Employment component"
                      weight: 1.0
                  final-decision-rule:
                    id: "loan-decision"
                    condition: "#totalScore >= 60 ? 'APPROVED' : (#totalScore >= 40 ? 'CONDITIONAL' : 'DENIED')"
                    message: "Final loan decision"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        YamlRuleChain ruleChain = yamlConfig.getRuleChains().get(0);

        // Test execution with moderate credit profile (should get conditional approval)
        Map<String, Object> context = Map.of(
            "creditScore", 650,
            "annualIncome", new BigDecimal("55000"),
            "employmentYears", 3
        );

        ChainedEvaluationContext chainedContext = new ChainedEvaluationContext(context);
        RuleChainResult result = ruleChainExecutor.executeRuleChain(ruleChain, chainedContext);

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals("CONDITIONAL", result.getFinalOutcome());

        // Verify the score calculation: 10 (initial) + 15 (credit) + 10 (income) + 10 (employment) = 45
        double finalScore = ((Number) result.getStageResult("totalScore_final")).doubleValue();
        assertEquals(45.0, finalScore, 0.1);

        // Verify conditional decision
        assertEquals("CONDITIONAL", result.getStageResult("finalDecision"));

        System.out.println("✓ Conditional approval scenario executed successfully");
        System.out.println("  Final score: " + finalScore + " (should be 45)");
        System.out.println("  Decision: " + result.getStageResult("finalDecision"));
    }

    @Test
    void testComplexWorkflowYamlConfiguration() throws YamlConfigurationException {
        System.out.println("\n=== Testing Complex Workflow YAML Configuration ===");

        String yamlContent = """
            metadata:
              name: "Complex Workflow Test Configuration"
              version: "1.0.0"

            rule-chains:
              - id: "trade-processing-workflow"
                name: "Trade Processing Workflow"
                description: "Multi-stage trade processing with dependencies"
                pattern: "complex-workflow"
                enabled: true
                priority: 50
                configuration:
                  stages:
                    - stage: "pre-validation"
                      name: "Pre-Validation Stage"
                      rules:
                        - id: "basic-data-check"
                          condition: "#tradeType != null && #notionalAmount != null && #counterparty != null"
                          message: "Basic trade data validation"
                      failure-action: "terminate"
                    - stage: "risk-assessment"
                      name: "Risk Assessment Stage"
                      depends-on: ["pre-validation"]
                      rules:
                        - id: "risk-calculation"
                          condition: "#notionalAmount > 1000000 && #marketVolatility > 0.2 ? 'HIGH' : (#notionalAmount > 500000 ? 'MEDIUM' : 'LOW')"
                          message: "Risk level assessed"
                      output-variable: "riskLevel"
                    - stage: "approval"
                      name: "Approval Stage"
                      depends-on: ["risk-assessment"]
                      conditional-execution:
                        condition: "#riskLevel == 'HIGH'"
                        on-true:
                          rules:
                            - id: "senior-approval"
                              condition: "#seniorApprovalObtained == true"
                              message: "Senior approval required for high-risk trades"
                        on-false:
                          rules:
                            - id: "standard-approval"
                              condition: "true"
                              message: "Standard approval applied"
                    - stage: "settlement"
                      name: "Settlement Processing"
                      depends-on: ["approval"]
                      rules:
                        - id: "settlement-calculation"
                          condition: "#tradeType == 'DERIVATIVE' ? 5 : (#tradeType == 'EQUITY' ? 3 : 2)"
                          message: "Settlement days calculated"
                      output-variable: "settlementDays"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        assertNotNull(yamlConfig);
        assertNotNull(yamlConfig.getRuleChains());
        assertEquals(1, yamlConfig.getRuleChains().size());

        YamlRuleChain ruleChain = yamlConfig.getRuleChains().get(0);
        assertEquals("trade-processing-workflow", ruleChain.getId());
        assertEquals("complex-workflow", ruleChain.getPattern());
        assertTrue(ruleChain.isEnabled());

        // Test execution with high-risk trade scenario
        Map<String, Object> context = Map.of(
            "tradeType", "DERIVATIVE",
            "notionalAmount", new BigDecimal("2000000"),
            "counterparty", "BANK_A",
            "marketVolatility", 0.25,
            "seniorApprovalObtained", true
        );

        ChainedEvaluationContext chainedContext = new ChainedEvaluationContext(context);
        RuleChainResult result = ruleChainExecutor.executeRuleChain(ruleChain, chainedContext);

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals("COMPLEX_WORKFLOW_COMPLETED", result.getFinalOutcome());

        // Verify stage results
        assertTrue(result.hasStageResult("riskLevel"));
        assertEquals("HIGH", result.getStageResult("riskLevel"));

        assertTrue(result.hasStageResult("settlementDays"));
        assertEquals(5, ((Number) result.getStageResult("settlementDays")).intValue());

        // Verify stage execution results
        assertTrue(result.hasStageResult("stage_pre-validation_result"));
        assertEquals("SUCCESS", result.getStageResult("stage_pre-validation_result"));

        System.out.println("✓ Complex workflow executed successfully");
        System.out.println("  Risk Level: " + result.getStageResult("riskLevel"));
        System.out.println("  Settlement Days: " + result.getStageResult("settlementDays"));
        System.out.println("  Execution path: " + String.join(" → ", result.getExecutionPath()));
    }

    @Test
    void testFluentBuilderYamlConfiguration() throws YamlConfigurationException {
        System.out.println("\n=== Testing Fluent Builder YAML Configuration ===");

        String yamlContent = """
            metadata:
              name: "Fluent Builder Test Configuration"
              version: "1.0.0"

            rule-chains:
              - id: "customer-processing-tree"
                name: "Customer Processing Decision Tree"
                description: "Complex decision tree for customer processing"
                pattern: "fluent-builder"
                enabled: true
                priority: 60
                configuration:
                  root-rule:
                    id: "customer-type-check"
                    condition: "#customerType == 'VIP' || #customerType == 'PREMIUM'"
                    message: "High-tier customer detected"
                    on-success:
                      rule:
                        id: "high-value-check"
                        condition: "#transactionAmount > 100000"
                        message: "High-value transaction detected"
                        on-success:
                          rule:
                            id: "regional-compliance"
                            condition: "#region == 'US' ? #accountAge >= 5 : #accountAge >= 3"
                            message: "Regional compliance check passed"
                            on-success:
                              rule:
                                id: "final-approval"
                                condition: "true"
                                message: "Final approval granted"
                            on-failure:
                              rule:
                                id: "compliance-review"
                                condition: "true"
                                message: "Compliance review required"
                        on-failure:
                          rule:
                            id: "standard-processing"
                            condition: "true"
                            message: "Standard processing applied"
                    on-failure:
                      rule:
                        id: "basic-validation"
                        condition: "#transactionAmount > 0"
                        message: "Basic validation check"
                        on-success:
                          rule:
                            id: "standard-customer-approval"
                            condition: "true"
                            message: "Standard customer approved"
                        on-failure:
                          rule:
                            id: "transaction-rejected"
                            condition: "true"
                            message: "Transaction rejected"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        assertNotNull(yamlConfig);
        assertNotNull(yamlConfig.getRuleChains());
        assertEquals(1, yamlConfig.getRuleChains().size());

        YamlRuleChain ruleChain = yamlConfig.getRuleChains().get(0);
        assertEquals("customer-processing-tree", ruleChain.getId());
        assertEquals("fluent-builder", ruleChain.getPattern());
        assertTrue(ruleChain.isEnabled());

        // Test execution with VIP customer, high-value transaction, US region, sufficient account age
        Map<String, Object> context = Map.of(
            "customerType", "VIP",
            "transactionAmount", new BigDecimal("150000"),
            "region", "US",
            "accountAge", 6
        );

        ChainedEvaluationContext chainedContext = new ChainedEvaluationContext(context);
        RuleChainResult result = ruleChainExecutor.executeRuleChain(ruleChain, chainedContext);

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals("SUCCESS", result.getFinalOutcome());

        // Verify individual rule results
        assertTrue(result.hasStageResult("fluent_rule_customer-type-check_result"));
        assertTrue((Boolean) result.getStageResult("fluent_rule_customer-type-check_result"));

        assertTrue(result.hasStageResult("fluent_rule_high-value-check_result"));
        assertTrue((Boolean) result.getStageResult("fluent_rule_high-value-check_result"));

        assertTrue(result.hasStageResult("fluent_rule_regional-compliance_result"));
        assertTrue((Boolean) result.getStageResult("fluent_rule_regional-compliance_result"));

        System.out.println("✓ Fluent builder executed successfully");
        System.out.println("  Final outcome: " + result.getFinalOutcome());
        System.out.println("  Execution path: " + String.join(" → ", result.getExecutionPath()));
        System.out.println("  Rules executed: " + result.getExecutedRulesCount());
    }

    @Test
    void testFluentBuilderFailurePath() throws YamlConfigurationException {
        System.out.println("\n=== Testing Fluent Builder Failure Path ===");

        String yamlContent = """
            rule-chains:
              - id: "simple-decision-tree"
                pattern: "fluent-builder"
                configuration:
                  root-rule:
                    id: "customer-check"
                    condition: "#customerType == 'VIP'"
                    message: "VIP customer check"
                    on-success:
                      rule:
                        id: "vip-processing"
                        condition: "true"
                        message: "VIP processing applied"
                    on-failure:
                      rule:
                        id: "standard-check"
                        condition: "#transactionAmount <= 10000"
                        message: "Standard customer transaction limit check"
                        on-success:
                          rule:
                            id: "standard-approval"
                            condition: "true"
                            message: "Standard approval granted"
                        on-failure:
                          rule:
                            id: "transaction-denied"
                            condition: "true"
                            message: "Transaction denied - over limit"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        YamlRuleChain ruleChain = yamlConfig.getRuleChains().get(0);

        // Test with standard customer, high transaction amount (should follow failure path)
        Map<String, Object> context = Map.of(
            "customerType", "STANDARD",
            "transactionAmount", new BigDecimal("15000")
        );

        ChainedEvaluationContext chainedContext = new ChainedEvaluationContext(context);
        RuleChainResult result = ruleChainExecutor.executeRuleChain(ruleChain, chainedContext);

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals("SUCCESS", result.getFinalOutcome()); // Final rule always succeeds

        // Verify the failure path was taken
        assertFalse((Boolean) result.getStageResult("fluent_rule_customer-check_result"));
        assertFalse((Boolean) result.getStageResult("fluent_rule_standard-check_result"));
        assertTrue((Boolean) result.getStageResult("fluent_rule_transaction-denied_result"));

        System.out.println("✓ Fluent builder failure path executed successfully");
        System.out.println("  Final outcome: " + result.getFinalOutcome());
        System.out.println("  Execution path: " + String.join(" → ", result.getExecutionPath()));
    }

    @Test
    void testWeightBasedRuleSelection() throws YamlConfigurationException {
        System.out.println("\n=== Testing Weight-Based Rule Selection ===");

        String yamlContent = """
            rule-chains:
              - id: "weight-threshold-selection"
                pattern: "accumulative-chaining"
                configuration:
                  accumulator-variable: "totalScore"
                  initial-value: 0
                  rule-selection:
                    strategy: "weight-threshold"
                    weight-threshold: 0.7
                  accumulation-rules:
                    - id: "high-weight-rule"
                      condition: "30"
                      message: "High weight rule"
                      weight: 0.9
                      priority: "HIGH"
                    - id: "medium-weight-rule"
                      condition: "20"
                      message: "Medium weight rule"
                      weight: 0.7
                      priority: "MEDIUM"
                    - id: "low-weight-rule"
                      condition: "10"
                      message: "Low weight rule"
                      weight: 0.5
                      priority: "LOW"
                  final-decision-rule:
                    condition: "#totalScore >= 40 ? 'APPROVED' : 'DENIED'"
                    message: "Final decision"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        YamlRuleChain ruleChain = yamlConfig.getRuleChains().get(0);

        Map<String, Object> context = Map.of("testValue", 100);
        ChainedEvaluationContext chainedContext = new ChainedEvaluationContext(context);
        RuleChainResult result = ruleChainExecutor.executeRuleChain(ruleChain, chainedContext);

        assertNotNull(result);
        assertTrue(result.isSuccessful());

        // Should only execute rules with weight >= 0.7 (high-weight-rule and medium-weight-rule)
        assertEquals(3, (Integer) result.getStageResult("total_rules_available"));
        assertEquals(2, (Integer) result.getStageResult("rules_selected_for_execution"));

        // Final score should be (30 × 0.9) + (20 × 0.7) = 27.0 + 14.0 = 41.0
        assertEquals(41.0, (Double) result.getStageResult("totalScore_final"), 0.01);
        assertEquals("APPROVED", result.getFinalOutcome());

        System.out.println("✓ Weight threshold selection executed successfully");
        System.out.println("  Total rules available: " + result.getStageResult("total_rules_available"));
        System.out.println("  Rules selected: " + result.getStageResult("rules_selected_for_execution"));
        System.out.println("  Final score: " + result.getStageResult("totalScore_final"));
    }

    @Test
    void testTopWeightedRuleSelection() throws YamlConfigurationException {
        System.out.println("\n=== Testing Top-Weighted Rule Selection ===");

        String yamlContent = """
            rule-chains:
              - id: "top-weighted-selection"
                pattern: "accumulative-chaining"
                configuration:
                  accumulator-variable: "totalScore"
                  initial-value: 0
                  rule-selection:
                    strategy: "top-weighted"
                    max-rules: 2
                  accumulation-rules:
                    - id: "rule-1"
                      condition: "10"
                      message: "Rule 1"
                      weight: 0.5
                    - id: "rule-2"
                      condition: "20"
                      message: "Rule 2"
                      weight: 0.9
                    - id: "rule-3"
                      condition: "15"
                      message: "Rule 3"
                      weight: 0.7
                    - id: "rule-4"
                      condition: "25"
                      message: "Rule 4"
                      weight: 0.8
                  final-decision-rule:
                    condition: "#totalScore >= 30 ? 'APPROVED' : 'DENIED'"
                    message: "Final decision"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        YamlRuleChain ruleChain = yamlConfig.getRuleChains().get(0);

        Map<String, Object> context = Map.of("testValue", 100);
        ChainedEvaluationContext chainedContext = new ChainedEvaluationContext(context);
        RuleChainResult result = ruleChainExecutor.executeRuleChain(ruleChain, chainedContext);

        assertNotNull(result);
        assertTrue(result.isSuccessful());

        // Should execute top 2 rules by weight (rule-2: 0.9, rule-4: 0.8)
        assertEquals(4, (Integer) result.getStageResult("total_rules_available"));
        assertEquals(2, (Integer) result.getStageResult("rules_selected_for_execution"));

        // Final score should be (20 × 0.9) + (25 × 0.8) = 18.0 + 20.0 = 38.0
        assertEquals(38.0, (Double) result.getStageResult("totalScore_final"), 0.01);
        assertEquals("APPROVED", result.getFinalOutcome());

        System.out.println("✓ Top-weighted selection executed successfully");
        System.out.println("  Final score: " + result.getStageResult("totalScore_final"));
    }

    @Test
    void testPriorityBasedRuleSelection() throws YamlConfigurationException {
        System.out.println("\n=== Testing Priority-Based Rule Selection ===");

        String yamlContent = """
            rule-chains:
              - id: "priority-based-selection"
                pattern: "accumulative-chaining"
                configuration:
                  accumulator-variable: "totalScore"
                  initial-value: 0
                  rule-selection:
                    strategy: "priority-based"
                    min-priority: "MEDIUM"
                  accumulation-rules:
                    - id: "high-priority-rule"
                      condition: "30"
                      message: "High priority rule"
                      weight: 0.8
                      priority: "HIGH"
                    - id: "medium-priority-rule"
                      condition: "20"
                      message: "Medium priority rule"
                      weight: 0.9
                      priority: "MEDIUM"
                    - id: "low-priority-rule"
                      condition: "10"
                      message: "Low priority rule"
                      weight: 1.0
                      priority: "LOW"
                  final-decision-rule:
                    condition: "#totalScore >= 40 ? 'APPROVED' : 'DENIED'"
                    message: "Final decision"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        YamlRuleChain ruleChain = yamlConfig.getRuleChains().get(0);

        Map<String, Object> context = Map.of("testValue", 100);
        ChainedEvaluationContext chainedContext = new ChainedEvaluationContext(context);
        RuleChainResult result = ruleChainExecutor.executeRuleChain(ruleChain, chainedContext);

        assertNotNull(result);
        assertTrue(result.isSuccessful());

        // Should execute rules with priority >= MEDIUM (HIGH and MEDIUM priority rules)
        assertEquals(3, (Integer) result.getStageResult("total_rules_available"));
        assertEquals(2, (Integer) result.getStageResult("rules_selected_for_execution"));

        // Final score should be (30 × 0.8) + (20 × 0.9) = 24.0 + 18.0 = 42.0
        assertEquals(42.0, (Double) result.getStageResult("totalScore_final"), 0.01);
        assertEquals("APPROVED", result.getFinalOutcome());

        System.out.println("✓ Priority-based selection executed successfully");
        System.out.println("  Final score: " + result.getStageResult("totalScore_final"));
    }

    @Test
    void testDynamicThresholdRuleSelection() throws YamlConfigurationException {
        System.out.println("\n=== Testing Dynamic Threshold Rule Selection ===");

        String yamlContent = """
            rule-chains:
              - id: "dynamic-threshold-selection"
                pattern: "accumulative-chaining"
                configuration:
                  accumulator-variable: "totalScore"
                  initial-value: 0
                  rule-selection:
                    strategy: "dynamic-threshold"
                    threshold-expression: "#riskLevel == 'HIGH' ? 0.8 : 0.6"
                  accumulation-rules:
                    - id: "rule-1"
                      condition: "10"
                      message: "Rule 1"
                      weight: 0.5
                    - id: "rule-2"
                      condition: "20"
                      message: "Rule 2"
                      weight: 0.7
                    - id: "rule-3"
                      condition: "30"
                      message: "Rule 3"
                      weight: 0.9
                  final-decision-rule:
                    condition: "#totalScore >= 20 ? 'APPROVED' : 'DENIED'"
                    message: "Final decision"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        YamlRuleChain ruleChain = yamlConfig.getRuleChains().get(0);

        // Test with HIGH risk level (threshold should be 0.8)
        Map<String, Object> context = Map.of("riskLevel", "HIGH");
        ChainedEvaluationContext chainedContext = new ChainedEvaluationContext(context);
        RuleChainResult result = ruleChainExecutor.executeRuleChain(ruleChain, chainedContext);

        assertNotNull(result);
        assertTrue(result.isSuccessful());

        // With HIGH risk, threshold is 0.8, so only rule-3 (weight 0.9) should execute
        assertEquals(3, (Integer) result.getStageResult("total_rules_available"));
        assertEquals(1, (Integer) result.getStageResult("rules_selected_for_execution"));
        // Final score should be 30 × 0.9 = 27.0
        assertEquals(27.0, (Double) result.getStageResult("totalScore_final"), 0.01);

        System.out.println("✓ Dynamic threshold selection executed successfully");
        System.out.println("  Risk level: HIGH, Threshold: 0.8");
        System.out.println("  Rules selected: " + result.getStageResult("rules_selected_for_execution"));
        System.out.println("  Final score: " + result.getStageResult("totalScore_final"));
    }

    private YamlRuleChain createValidRuleChain() {
        YamlRuleChain ruleChain = new YamlRuleChain();
        ruleChain.setId("test-chain");
        ruleChain.setPattern("conditional-chaining");
        
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> triggerRule = new HashMap<>();
        triggerRule.put("condition", "#value > 0");
        config.put("trigger-rule", triggerRule);
        
        Map<String, Object> conditionalRules = new HashMap<>();
        config.put("conditional-rules", conditionalRules);
        
        ruleChain.setConfiguration(config);
        return ruleChain;
    }

    private YamlRuleChain createInvalidRuleChain() {
        YamlRuleChain ruleChain = new YamlRuleChain();
        ruleChain.setId("invalid-chain");
        ruleChain.setPattern("unsupported-pattern");
        return ruleChain;
    }
}
