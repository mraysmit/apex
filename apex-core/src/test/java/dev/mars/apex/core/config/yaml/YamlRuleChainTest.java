package dev.mars.apex.core.config.yaml;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pattern-specific integration tests for YamlRuleChain.
 * 
 * Tests focus on:
 * - All 6 supported patterns with actual configuration structures
 * - Pattern-specific validation logic and configuration requirements
 * - Priority handling and enabled/disabled logic
 * - Enterprise metadata and custom properties
 * - Map-based configuration validation for each pattern
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class YamlRuleChainTest {

    private YamlRuleChain ruleChain;

    @BeforeEach
    void setUp() {
        ruleChain = new YamlRuleChain();
    }

    // ========================================
    // Constructor and Basic Properties Tests
    // ========================================

    @Test
    @DisplayName("Should create YamlRuleChain with null values initially")
    void testDefaultConstructor() {
        YamlRuleChain chain = new YamlRuleChain();
        
        assertNotNull(chain, "Rule chain should be created");
        assertNull(chain.getId(), "ID should be null initially");
        assertNull(chain.getName(), "Name should be null initially");
        assertNull(chain.getPattern(), "Pattern should be null initially");
        assertNull(chain.getEnabled(), "Enabled should be null initially");
        assertTrue(chain.isEnabled(), "Should be enabled by default (null = enabled)");
        assertNull(chain.getPriority(), "Priority should be null initially");
        assertEquals(100, chain.getPriorityValue(), "Priority value should default to 100");
        assertNull(chain.getConfiguration(), "Configuration should be null initially");
    }

    @Test
    @DisplayName("Should handle basic property operations")
    void testBasicProperties() {
        ruleChain.setId("test-chain-001");
        ruleChain.setName("Test Rule Chain");
        ruleChain.setDescription("A comprehensive test rule chain");
        ruleChain.setPattern("conditional-chaining");
        ruleChain.setEnabled(true);
        ruleChain.setPriority(150);
        ruleChain.setCategory("test-category");
        ruleChain.setCategories(Arrays.asList("category1", "category2"));

        assertEquals("test-chain-001", ruleChain.getId(), "ID should match");
        assertEquals("Test Rule Chain", ruleChain.getName(), "Name should match");
        assertEquals("A comprehensive test rule chain", ruleChain.getDescription(), "Description should match");
        assertEquals("conditional-chaining", ruleChain.getPattern(), "Pattern should match");
        assertTrue(ruleChain.getEnabled(), "Should be enabled");
        assertTrue(ruleChain.isEnabled(), "isEnabled() should return true");
        assertEquals(150, ruleChain.getPriority(), "Priority should match");
        assertEquals(150, ruleChain.getPriorityValue(), "Priority value should match");
        assertEquals("test-category", ruleChain.getCategory(), "Category should match");
        assertEquals(2, ruleChain.getCategories().size(), "Should have 2 categories");
    }

    // ========================================
    // Pattern 1: Conditional Chaining Tests
    // ========================================

    @Test
    @DisplayName("Should support conditional-chaining pattern with trigger and conditional rules")
    void testConditionalChainingPattern() {
        ruleChain.setId("conditional-chain");
        ruleChain.setName("High-Value Transaction Processing");
        ruleChain.setPattern("conditional-chaining");

        Map<String, Object> config = createConditionalChainingConfiguration();
        ruleChain.setConfiguration(config);

        assertEquals("conditional-chaining", ruleChain.getPattern(), "Pattern should be conditional-chaining");
        assertNotNull(ruleChain.getConfiguration(), "Configuration should not be null");
        
        // Verify trigger rule structure
        assertTrue(ruleChain.getConfiguration().containsKey("trigger-rule"), "Should contain trigger-rule");
        @SuppressWarnings("unchecked")
        Map<String, Object> triggerRule = (Map<String, Object>) ruleChain.getConfiguration().get("trigger-rule");
        assertEquals("#customerType == 'PREMIUM' && #transactionAmount > 100000", triggerRule.get("condition"), 
                    "Trigger condition should match");
        assertEquals("High-value customer transaction detected", triggerRule.get("message"), 
                    "Trigger message should match");
        
        // Verify conditional rules structure
        assertTrue(ruleChain.getConfiguration().containsKey("conditional-rules"), "Should contain conditional-rules");
        @SuppressWarnings("unchecked")
        Map<String, Object> conditionalRules = (Map<String, Object>) ruleChain.getConfiguration().get("conditional-rules");
        assertTrue(conditionalRules.containsKey("on-trigger"), "Should contain on-trigger rules");
        assertTrue(conditionalRules.containsKey("on-no-trigger"), "Should contain on-no-trigger rules");
    }

    // ========================================
    // Pattern 2: Sequential Dependency Tests
    // ========================================

    @Test
    @DisplayName("Should support sequential-dependency pattern with staged processing")
    void testSequentialDependencyPattern() {
        ruleChain.setId("discount-pipeline");
        ruleChain.setName("Discount Calculation Pipeline");
        ruleChain.setPattern("sequential-dependency");

        Map<String, Object> config = createSequentialDependencyConfiguration();
        ruleChain.setConfiguration(config);

        assertEquals("sequential-dependency", ruleChain.getPattern(), "Pattern should be sequential-dependency");
        assertNotNull(ruleChain.getConfiguration(), "Configuration should not be null");
        
        // Verify stages structure
        assertTrue(ruleChain.getConfiguration().containsKey("stages"), "Should contain stages");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stages = (List<Map<String, Object>>) ruleChain.getConfiguration().get("stages");
        assertEquals(3, stages.size(), "Should have 3 stages");
        
        // Verify first stage
        Map<String, Object> stage1 = stages.get(0);
        assertEquals(1, stage1.get("stage"), "First stage should be stage 1");
        assertEquals("Base Discount", stage1.get("name"), "First stage name should match");
        assertEquals("baseDiscount", stage1.get("output-variable"), "First stage output variable should match");
        
        // Verify stage with dependency
        Map<String, Object> stage2 = stages.get(1);
        assertEquals(2, stage2.get("stage"), "Second stage should be stage 2");
        assertTrue(stage2.containsKey("depends-on"), "Second stage should have dependencies");
        @SuppressWarnings("unchecked")
        List<String> dependencies = (List<String>) stage2.get("depends-on");
        assertTrue(dependencies.contains("baseDiscount"), "Should depend on baseDiscount");
    }

    // ========================================
    // Pattern 3: Result-Based Routing Tests
    // ========================================

    @Test
    @DisplayName("Should support result-based-routing pattern with router and routes")
    void testResultBasedRoutingPattern() {
        ruleChain.setId("risk-routing");
        ruleChain.setName("Risk-Based Processing Router");
        ruleChain.setPattern("result-based-routing");

        Map<String, Object> config = createResultBasedRoutingConfiguration();
        ruleChain.setConfiguration(config);

        assertEquals("result-based-routing", ruleChain.getPattern(), "Pattern should be result-based-routing");
        assertNotNull(ruleChain.getConfiguration(), "Configuration should not be null");
        
        // Verify router rule
        assertTrue(ruleChain.getConfiguration().containsKey("router-rule"), "Should contain router-rule");
        @SuppressWarnings("unchecked")
        Map<String, Object> routerRule = (Map<String, Object>) ruleChain.getConfiguration().get("router-rule");
        assertEquals("#riskScore > 70 ? 'HIGH_RISK' : 'LOW_RISK'", routerRule.get("condition"), 
                    "Router condition should match");
        
        // Verify routes
        assertTrue(ruleChain.getConfiguration().containsKey("routes"), "Should contain routes");
        @SuppressWarnings("unchecked")
        Map<String, Object> routes = (Map<String, Object>) ruleChain.getConfiguration().get("routes");
        assertTrue(routes.containsKey("HIGH_RISK"), "Should contain HIGH_RISK route");
        assertTrue(routes.containsKey("LOW_RISK"), "Should contain LOW_RISK route");
        
        // Verify HIGH_RISK route structure
        @SuppressWarnings("unchecked")
        Map<String, Object> highRiskRoute = (Map<String, Object>) routes.get("HIGH_RISK");
        assertTrue(highRiskRoute.containsKey("rules"), "HIGH_RISK route should contain rules");
    }

    // ========================================
    // Pattern 4: Accumulative Chaining Tests
    // ========================================

    @Test
    @DisplayName("Should support accumulative-chaining pattern with weighted scoring")
    void testAccumulativeChainingPattern() {
        ruleChain.setId("credit-scoring");
        ruleChain.setName("Credit Score Accumulation");
        ruleChain.setPattern("accumulative-chaining");

        Map<String, Object> config = createAccumulativeChainingConfiguration();
        ruleChain.setConfiguration(config);

        assertEquals("accumulative-chaining", ruleChain.getPattern(), "Pattern should be accumulative-chaining");
        assertNotNull(ruleChain.getConfiguration(), "Configuration should not be null");
        
        // Verify accumulator configuration
        assertEquals("creditScore", ruleChain.getConfiguration().get("accumulator-variable"), 
                    "Accumulator variable should match");
        assertEquals(0, ruleChain.getConfiguration().get("initial-value"), 
                    "Initial value should be 0");
        
        // Verify accumulation rules
        assertTrue(ruleChain.getConfiguration().containsKey("accumulation-rules"), "Should contain accumulation-rules");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> accumulationRules = (List<Map<String, Object>>) 
            ruleChain.getConfiguration().get("accumulation-rules");
        assertEquals(3, accumulationRules.size(), "Should have 3 accumulation rules");
        
        // Verify weighted rule structure
        Map<String, Object> weightedRule = accumulationRules.get(0);
        assertTrue(weightedRule.containsKey("weight"), "Rule should have weight");
        assertTrue(weightedRule.containsKey("accumulation-expression"), "Rule should have accumulation expression");
        assertEquals(2.0, weightedRule.get("weight"), "Weight should match");
    }

    // ========================================
    // Pattern 5: Complex Workflow Tests
    // ========================================

    @Test
    @DisplayName("Should support complex-workflow pattern with multi-stage processing")
    void testComplexWorkflowPattern() {
        ruleChain.setId("trade-settlement");
        ruleChain.setName("Trade Settlement Workflow");
        ruleChain.setPattern("complex-workflow");

        Map<String, Object> config = createComplexWorkflowConfiguration();
        ruleChain.setConfiguration(config);

        assertEquals("complex-workflow", ruleChain.getPattern(), "Pattern should be complex-workflow");
        assertNotNull(ruleChain.getConfiguration(), "Configuration should not be null");
        
        // Verify workflow stages
        assertTrue(ruleChain.getConfiguration().containsKey("stages"), "Should contain stages");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stages = (List<Map<String, Object>>) ruleChain.getConfiguration().get("stages");
        assertEquals(3, stages.size(), "Should have 3 workflow stages");
        
        // Verify stage with conditional execution
        Map<String, Object> approvalStage = stages.get(2);
        assertEquals("approval-workflow", approvalStage.get("stage"), "Approval stage should match");
        assertTrue(approvalStage.containsKey("conditional-execution"), "Should have conditional execution");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> conditionalExecution = (Map<String, Object>) approvalStage.get("conditional-execution");
        assertEquals("#riskLevel == 'HIGH'", conditionalExecution.get("condition"), 
                    "Conditional execution condition should match");
        assertTrue(conditionalExecution.containsKey("on-true"), "Should have on-true rules");
        assertTrue(conditionalExecution.containsKey("on-false"), "Should have on-false rules");
        
        // Verify failure action
        Map<String, Object> validationStage = stages.get(0);
        assertEquals("terminate", validationStage.get("failure-action"), "Failure action should be terminate");
    }

    // ========================================
    // Pattern 6: Fluent Builder Tests
    // ========================================

    @Test
    @DisplayName("Should support fluent-builder pattern with rule tree structure")
    void testFluentBuilderPattern() {
        ruleChain.setId("customer-decision-tree");
        ruleChain.setName("Customer Processing Decision Tree");
        ruleChain.setPattern("fluent-builder");

        Map<String, Object> config = createFluentBuilderConfiguration();
        ruleChain.setConfiguration(config);

        assertEquals("fluent-builder", ruleChain.getPattern(), "Pattern should be fluent-builder");
        assertNotNull(ruleChain.getConfiguration(), "Configuration should not be null");
        
        // Verify root rule
        assertTrue(ruleChain.getConfiguration().containsKey("root-rule"), "Should contain root-rule");
        @SuppressWarnings("unchecked")
        Map<String, Object> rootRule = (Map<String, Object>) ruleChain.getConfiguration().get("root-rule");
        assertEquals("customer-type-check", rootRule.get("id"), "Root rule ID should match");
        assertEquals("#customerType == 'VIP' || #customerType == 'PREMIUM'", rootRule.get("condition"), 
                    "Root rule condition should match");
        
        // Verify on-success path
        assertTrue(rootRule.containsKey("on-success"), "Root rule should have on-success");
        @SuppressWarnings("unchecked")
        Map<String, Object> onSuccess = (Map<String, Object>) rootRule.get("on-success");
        assertTrue(onSuccess.containsKey("rule"), "On-success should contain rule");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> successRule = (Map<String, Object>) onSuccess.get("rule");
        assertEquals("high-value-check", successRule.get("id"), "Success rule ID should match");
        assertTrue(successRule.containsKey("on-success"), "Success rule should have nested on-success");
        assertTrue(successRule.containsKey("on-failure"), "Success rule should have nested on-failure");
        
        // Verify on-failure path
        assertTrue(rootRule.containsKey("on-failure"), "Root rule should have on-failure");
        @SuppressWarnings("unchecked")
        Map<String, Object> onFailure = (Map<String, Object>) rootRule.get("on-failure");
        assertTrue(onFailure.containsKey("rule"), "On-failure should contain rule");
    }

    // ========================================
    // Priority and Enabled Logic Tests
    // ========================================

    @Test
    @DisplayName("Should handle priority values correctly")
    void testPriorityHandling() {
        // Test null priority (default)
        ruleChain.setPriority(null);
        assertNull(ruleChain.getPriority(), "Priority should be null");
        assertEquals(100, ruleChain.getPriorityValue(), "Priority value should default to 100");

        // Test explicit priority values
        ruleChain.setPriority(50);
        assertEquals(50, ruleChain.getPriority(), "Priority should be 50");
        assertEquals(50, ruleChain.getPriorityValue(), "Priority value should be 50");

        ruleChain.setPriority(0);
        assertEquals(0, ruleChain.getPriority(), "Priority should be 0");
        assertEquals(0, ruleChain.getPriorityValue(), "Priority value should be 0");

        ruleChain.setPriority(-10);
        assertEquals(-10, ruleChain.getPriority(), "Priority should be -10");
        assertEquals(-10, ruleChain.getPriorityValue(), "Priority value should be -10");
    }

    @Test
    @DisplayName("Should handle enabled/disabled states correctly")
    void testEnabledDisabledStates() {
        // Test null enabled (default to true)
        ruleChain.setEnabled(null);
        assertNull(ruleChain.getEnabled(), "Enabled should be null");
        assertTrue(ruleChain.isEnabled(), "isEnabled() should return true for null");

        // Test explicitly enabled
        ruleChain.setEnabled(true);
        assertTrue(ruleChain.getEnabled(), "Enabled should be true");
        assertTrue(ruleChain.isEnabled(), "isEnabled() should return true");

        // Test explicitly disabled
        ruleChain.setEnabled(false);
        assertFalse(ruleChain.getEnabled(), "Enabled should be false");
        assertFalse(ruleChain.isEnabled(), "isEnabled() should return false");
    }

    // ========================================
    // Enterprise Metadata Tests
    // ========================================

    @Test
    @DisplayName("Should handle enterprise metadata and custom properties")
    void testEnterpriseMetadata() {
        ruleChain.setId("enterprise-chain");
        ruleChain.setName("Enterprise Rule Chain");
        ruleChain.setCreatedBy("john.doe@company.com");
        ruleChain.setBusinessDomain("Risk Management");
        ruleChain.setBusinessOwner("Risk Team");
        ruleChain.setSourceSystem("APEX-RULES-ENGINE");
        ruleChain.setEffectiveDate("2024-01-01T00:00:00Z");
        ruleChain.setExpirationDate("2024-12-31T23:59:59Z");

        // Set custom properties
        Map<String, Object> customProperties = new HashMap<>();
        customProperties.put("compliance.sox", true);
        customProperties.put("compliance.gdpr", true);
        customProperties.put("audit.level", "HIGH");
        customProperties.put("monitoring.enabled", true);
        ruleChain.setCustomProperties(customProperties);

        // Set metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("version", "1.2.0");
        metadata.put("last-reviewed", "2024-01-15");
        metadata.put("reviewer", "compliance.team@company.com");
        ruleChain.setMetadata(metadata);

        // Set tags
        ruleChain.setTags(Arrays.asList("enterprise", "compliance", "risk-management", "production"));

        // Verify enterprise metadata
        assertEquals("john.doe@company.com", ruleChain.getCreatedBy(), "Created by should match");
        assertEquals("Risk Management", ruleChain.getBusinessDomain(), "Business domain should match");
        assertEquals("Risk Team", ruleChain.getBusinessOwner(), "Business owner should match");
        assertEquals("APEX-RULES-ENGINE", ruleChain.getSourceSystem(), "Source system should match");
        assertEquals("2024-01-01T00:00:00Z", ruleChain.getEffectiveDate(), "Effective date should match");
        assertEquals("2024-12-31T23:59:59Z", ruleChain.getExpirationDate(), "Expiration date should match");

        // Verify custom properties
        assertNotNull(ruleChain.getCustomProperties(), "Custom properties should not be null");
        assertEquals(4, ruleChain.getCustomProperties().size(), "Should have 4 custom properties");
        assertTrue((Boolean) ruleChain.getCustomProperties().get("compliance.sox"), "SOX compliance should be true");
        assertTrue((Boolean) ruleChain.getCustomProperties().get("compliance.gdpr"), "GDPR compliance should be true");
        assertEquals("HIGH", ruleChain.getCustomProperties().get("audit.level"), "Audit level should be HIGH");

        // Verify metadata
        assertNotNull(ruleChain.getMetadata(), "Metadata should not be null");
        assertEquals("1.2.0", ruleChain.getMetadata().get("version"), "Version should match");

        // Verify tags
        assertNotNull(ruleChain.getTags(), "Tags should not be null");
        assertEquals(4, ruleChain.getTags().size(), "Should have 4 tags");
        assertTrue(ruleChain.getTags().contains("enterprise"), "Should contain enterprise tag");
        assertTrue(ruleChain.getTags().contains("compliance"), "Should contain compliance tag");
    }

    // ========================================
    // toString() and Object Integrity Tests
    // ========================================

    @Test
    @DisplayName("Should provide meaningful toString representation")
    void testToStringRepresentation() {
        ruleChain.setId("test-chain");
        ruleChain.setName("Test Chain");
        ruleChain.setPattern("conditional-chaining");
        ruleChain.setEnabled(true);
        ruleChain.setPriority(75);

        String toString = ruleChain.toString();
        
        assertNotNull(toString, "toString should not be null");
        assertTrue(toString.contains("test-chain"), "toString should contain ID");
        assertTrue(toString.contains("Test Chain"), "toString should contain name");
        assertTrue(toString.contains("conditional-chaining"), "toString should contain pattern");
        assertTrue(toString.contains("true"), "toString should contain enabled state");
        assertTrue(toString.contains("75"), "toString should contain priority");
    }

    @Test
    @DisplayName("Should maintain object integrity after modifications")
    void testObjectIntegrity() {
        // Set initial state
        ruleChain.setId("integrity-test");
        ruleChain.setPattern("sequential-dependency");
        ruleChain.setPriority(50);
        ruleChain.setEnabled(true);

        Map<String, Object> config = createSequentialDependencyConfiguration();
        ruleChain.setConfiguration(config);

        // Verify initial state
        assertEquals("integrity-test", ruleChain.getId());
        assertEquals("sequential-dependency", ruleChain.getPattern());
        assertEquals(50, ruleChain.getPriorityValue());
        assertTrue(ruleChain.isEnabled());
        assertNotNull(ruleChain.getConfiguration());

        // Modify configuration
        ruleChain.getConfiguration().put("new-property", "new-value");
        
        // Verify modifications don't break integrity
        assertEquals("integrity-test", ruleChain.getId(), "ID should remain unchanged");
        assertEquals("sequential-dependency", ruleChain.getPattern(), "Pattern should remain unchanged");
        assertEquals("new-value", ruleChain.getConfiguration().get("new-property"), "New property should be added");
        assertTrue(ruleChain.getConfiguration().containsKey("stages"), "Original configuration should remain");
    }

    // ========================================
    // Helper Methods for Configuration Creation
    // ========================================

    private Map<String, Object> createConditionalChainingConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        // Trigger rule
        Map<String, Object> triggerRule = new HashMap<>();
        triggerRule.put("condition", "#customerType == 'PREMIUM' && #transactionAmount > 100000");
        triggerRule.put("message", "High-value customer transaction detected");
        config.put("trigger-rule", triggerRule);
        
        // Conditional rules
        Map<String, Object> conditionalRules = new HashMap<>();
        
        List<Map<String, Object>> onTriggerRules = new ArrayList<>();
        Map<String, Object> onTriggerRule = new HashMap<>();
        onTriggerRule.put("condition", "#accountAge >= 3");
        onTriggerRule.put("message", "Enhanced due diligence check");
        onTriggerRules.add(onTriggerRule);
        conditionalRules.put("on-trigger", onTriggerRules);
        
        List<Map<String, Object>> onNoTriggerRules = new ArrayList<>();
        Map<String, Object> onNoTriggerRule = new HashMap<>();
        onNoTriggerRule.put("condition", "true");
        onNoTriggerRule.put("message", "Standard processing applied");
        onNoTriggerRules.add(onNoTriggerRule);
        conditionalRules.put("on-no-trigger", onNoTriggerRules);
        
        config.put("conditional-rules", conditionalRules);
        return config;
    }

    private Map<String, Object> createSequentialDependencyConfiguration() {
        Map<String, Object> config = new HashMap<>();

        List<Map<String, Object>> stages = new ArrayList<>();

        // Stage 1: Base Discount
        Map<String, Object> stage1 = new HashMap<>();
        stage1.put("stage", 1);
        stage1.put("name", "Base Discount");
        Map<String, Object> rule1 = new HashMap<>();
        rule1.put("condition", "#customerTier == 'GOLD' ? 0.15 : 0.05");
        rule1.put("message", "Base discount calculated");
        stage1.put("rule", rule1);
        stage1.put("output-variable", "baseDiscount");
        stages.add(stage1);

        // Stage 2: Regional Multiplier (depends on stage 1)
        Map<String, Object> stage2 = new HashMap<>();
        stage2.put("stage", 2);
        stage2.put("name", "Regional Multiplier");
        stage2.put("depends-on", Arrays.asList("baseDiscount"));
        Map<String, Object> rule2 = new HashMap<>();
        rule2.put("condition", "#region == 'US' ? #baseDiscount * 1.2 : #baseDiscount");
        rule2.put("message", "Regional multiplier applied");
        stage2.put("rule", rule2);
        stage2.put("output-variable", "regionalDiscount");
        stages.add(stage2);

        // Stage 3: Final Calculation
        Map<String, Object> stage3 = new HashMap<>();
        stage3.put("stage", 3);
        stage3.put("name", "Final Discount");
        stage3.put("depends-on", Arrays.asList("regionalDiscount"));
        Map<String, Object> rule3 = new HashMap<>();
        rule3.put("condition", "#regionalDiscount > 0.20 ? 0.20 : #regionalDiscount");
        rule3.put("message", "Final discount capped at 20%");
        stage3.put("rule", rule3);
        stage3.put("output-variable", "finalDiscount");
        stages.add(stage3);

        config.put("stages", stages);
        return config;
    }

    private Map<String, Object> createResultBasedRoutingConfiguration() {
        Map<String, Object> config = new HashMap<>();

        // Router rule
        Map<String, Object> routerRule = new HashMap<>();
        routerRule.put("condition", "#riskScore > 70 ? 'HIGH_RISK' : 'LOW_RISK'");
        routerRule.put("message", "Risk level determined");
        config.put("router-rule", routerRule);

        // Routes
        Map<String, Object> routes = new HashMap<>();

        // High risk route
        Map<String, Object> highRiskRoute = new HashMap<>();
        List<Map<String, Object>> highRiskRules = new ArrayList<>();
        Map<String, Object> highRiskRule = new HashMap<>();
        highRiskRule.put("condition", "#transactionAmount > 100000");
        highRiskRule.put("message", "Manager approval required");
        highRiskRules.add(highRiskRule);
        highRiskRoute.put("rules", highRiskRules);
        routes.put("HIGH_RISK", highRiskRoute);

        // Low risk route
        Map<String, Object> lowRiskRoute = new HashMap<>();
        List<Map<String, Object>> lowRiskRules = new ArrayList<>();
        Map<String, Object> lowRiskRule = new HashMap<>();
        lowRiskRule.put("condition", "#transactionAmount > 0");
        lowRiskRule.put("message", "Basic validation");
        lowRiskRules.add(lowRiskRule);
        lowRiskRoute.put("rules", lowRiskRules);
        routes.put("LOW_RISK", lowRiskRoute);

        config.put("routes", routes);
        return config;
    }

    private Map<String, Object> createAccumulativeChainingConfiguration() {
        Map<String, Object> config = new HashMap<>();

        config.put("accumulator-variable", "creditScore");
        config.put("initial-value", 0);

        List<Map<String, Object>> accumulationRules = new ArrayList<>();

        // Credit history rule
        Map<String, Object> creditRule = new HashMap<>();
        creditRule.put("id", "credit-history");
        creditRule.put("condition", "#creditHistory > 5 ? 25 : (#creditHistory > 2 ? 15 : 5)");
        creditRule.put("message", "Credit history assessment");
        creditRule.put("weight", 2.0);
        creditRule.put("accumulation-expression", "#creditScore + (#ruleResult * #weight)");
        accumulationRules.add(creditRule);

        // Income rule
        Map<String, Object> incomeRule = new HashMap<>();
        incomeRule.put("id", "income-assessment");
        incomeRule.put("condition", "#annualIncome > 100000 ? 30 : (#annualIncome > 50000 ? 20 : 10)");
        incomeRule.put("message", "Income level assessment");
        incomeRule.put("weight", 1.5);
        incomeRule.put("accumulation-expression", "#creditScore + (#ruleResult * #weight)");
        accumulationRules.add(incomeRule);

        // Employment rule
        Map<String, Object> employmentRule = new HashMap<>();
        employmentRule.put("id", "employment-stability");
        employmentRule.put("condition", "#employmentYears > 5 ? 20 : (#employmentYears > 2 ? 10 : 5)");
        employmentRule.put("message", "Employment stability assessment");
        employmentRule.put("weight", 1.0);
        employmentRule.put("accumulation-expression", "#creditScore + (#ruleResult * #weight)");
        accumulationRules.add(employmentRule);

        config.put("accumulation-rules", accumulationRules);
        return config;
    }

    private Map<String, Object> createComplexWorkflowConfiguration() {
        Map<String, Object> config = new HashMap<>();

        List<Map<String, Object>> stages = new ArrayList<>();

        // Stage 1: Trade Validation
        Map<String, Object> validationStage = new HashMap<>();
        validationStage.put("stage", "trade-validation");
        validationStage.put("name", "Trade Data Validation");
        List<Map<String, Object>> validationRules = new ArrayList<>();
        Map<String, Object> validationRule = new HashMap<>();
        validationRule.put("condition", "#tradeType != null && #notionalAmount != null && #counterparty != null");
        validationRule.put("message", "Basic trade data validation");
        validationRules.add(validationRule);
        validationStage.put("rules", validationRules);
        validationStage.put("failure-action", "terminate");
        stages.add(validationStage);

        // Stage 2: Risk Assessment (depends on validation)
        Map<String, Object> riskStage = new HashMap<>();
        riskStage.put("stage", "risk-assessment");
        riskStage.put("name", "Risk Assessment");
        riskStage.put("depends-on", Arrays.asList("trade-validation"));
        List<Map<String, Object>> riskRules = new ArrayList<>();
        Map<String, Object> riskRule = new HashMap<>();
        riskRule.put("condition", "#notionalAmount > 1000000 && #marketVolatility > 0.2 ? 'HIGH' : 'MEDIUM'");
        riskRule.put("message", "Risk level assessment");
        riskRules.add(riskRule);
        riskStage.put("rules", riskRules);
        riskStage.put("output-variable", "riskLevel");
        stages.add(riskStage);

        // Stage 3: Approval Workflow (conditional execution based on risk)
        Map<String, Object> approvalStage = new HashMap<>();
        approvalStage.put("stage", "approval-workflow");
        approvalStage.put("name", "Approval Workflow");
        approvalStage.put("depends-on", Arrays.asList("risk-assessment"));

        Map<String, Object> conditionalExecution = new HashMap<>();
        conditionalExecution.put("condition", "#riskLevel == 'HIGH'");

        // On-true: High risk approval rules
        List<Map<String, Object>> onTrueRules = new ArrayList<>();
        Map<String, Object> managerApprovalRule = new HashMap<>();
        managerApprovalRule.put("condition", "#managerApproval == true");
        managerApprovalRule.put("message", "Manager approval required for high-risk trades");
        onTrueRules.add(managerApprovalRule);
        conditionalExecution.put("on-true", onTrueRules);

        // On-false: Standard approval rules
        List<Map<String, Object>> onFalseRules = new ArrayList<>();
        Map<String, Object> autoApprovalRule = new HashMap<>();
        autoApprovalRule.put("condition", "true");
        autoApprovalRule.put("message", "Automatic approval for medium/low risk trades");
        onFalseRules.add(autoApprovalRule);
        conditionalExecution.put("on-false", onFalseRules);

        approvalStage.put("conditional-execution", conditionalExecution);
        stages.add(approvalStage);

        config.put("stages", stages);
        return config;
    }

    private Map<String, Object> createFluentBuilderConfiguration() {
        Map<String, Object> config = new HashMap<>();

        // Root rule: Customer type check
        Map<String, Object> rootRule = new HashMap<>();
        rootRule.put("id", "customer-type-check");
        rootRule.put("condition", "#customerType == 'VIP' || #customerType == 'PREMIUM'");
        rootRule.put("message", "High-tier customer detected");

        // On-success: High-value check
        Map<String, Object> onSuccess = new HashMap<>();
        Map<String, Object> highValueRule = new HashMap<>();
        highValueRule.put("id", "high-value-check");
        highValueRule.put("condition", "#transactionAmount > 100000");
        highValueRule.put("message", "High-value transaction detected");

        // Nested on-success: Final approval
        Map<String, Object> nestedOnSuccess = new HashMap<>();
        Map<String, Object> finalApprovalRule = new HashMap<>();
        finalApprovalRule.put("id", "final-approval");
        finalApprovalRule.put("condition", "true");
        finalApprovalRule.put("message", "Final approval granted");
        nestedOnSuccess.put("rule", finalApprovalRule);
        highValueRule.put("on-success", nestedOnSuccess);

        // Nested on-failure: Standard processing
        Map<String, Object> nestedOnFailure = new HashMap<>();
        Map<String, Object> standardProcessingRule = new HashMap<>();
        standardProcessingRule.put("id", "standard-processing");
        standardProcessingRule.put("condition", "true");
        standardProcessingRule.put("message", "Standard processing applied");
        nestedOnFailure.put("rule", standardProcessingRule);
        highValueRule.put("on-failure", nestedOnFailure);

        onSuccess.put("rule", highValueRule);
        rootRule.put("on-success", onSuccess);

        // On-failure: Basic validation
        Map<String, Object> onFailure = new HashMap<>();
        Map<String, Object> basicValidationRule = new HashMap<>();
        basicValidationRule.put("id", "basic-validation");
        basicValidationRule.put("condition", "#transactionAmount > 0");
        basicValidationRule.put("message", "Basic validation check");
        onFailure.put("rule", basicValidationRule);
        rootRule.put("on-failure", onFailure);

        config.put("root-rule", rootRule);
        return config;
    }
}
