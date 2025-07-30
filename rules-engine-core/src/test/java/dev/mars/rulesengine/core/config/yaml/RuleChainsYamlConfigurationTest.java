package dev.mars.rulesengine.core.config.yaml;

import dev.mars.rulesengine.core.engine.context.ChainedEvaluationContext;
import dev.mars.rulesengine.core.engine.executor.RuleChainExecutor;
import dev.mars.rulesengine.core.engine.model.RuleChainResult;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import dev.mars.rulesengine.core.service.engine.RuleEngineService;
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
        assertEquals(new BigDecimal("82000"), result.getStageResult("finalAmount"));

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
