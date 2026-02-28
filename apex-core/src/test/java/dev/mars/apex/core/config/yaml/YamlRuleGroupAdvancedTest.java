package dev.mars.apex.core.config.yaml;
import dev.mars.apex.core.config.RuleFactory;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.loader.*;
import dev.mars.apex.core.config.exception.*;
import dev.mars.apex.core.config.service.*;

import dev.mars.apex.engine.core.RulesEngineConfiguration;
import dev.mars.apex.engine.model.RuleGroup;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;


import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for advanced YAML RuleGroup configuration features including:
 * - operator configuration (AND/OR)
 * - stop-on-first-failure configuration
 * - parallel-execution configuration
 * - debug-mode configuration
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd 
 * @since 1.0.0
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class YamlRuleGroupAdvancedTest {

    private RuleFactory RuleFactory;
    private RulesEngineConfiguration config;

    @BeforeEach
    void setUp() {
        RuleFactory = new RuleFactory();
        config = new RulesEngineConfiguration();
    }

    // ========================================
    // YAML Configuration Tests
    // ========================================

    @Nested
    @DisplayName("YAML Configuration Parsing")
    class YamlConfigurationTests {

        @Test
        @DisplayName("Should parse operator configuration")
        void testOperatorConfiguration() throws ConfigurationException {
            // Test AND operator
            YamlRuleGroup andGroup = new YamlRuleGroup();
            andGroup.setId("and-group");
            andGroup.setName("AND Group");
            andGroup.setDescription("Test AND group");
            andGroup.setOperator("AND");
            
            RuleGroup createdAndGroup = RuleFactory.createRuleGroup(andGroup, config);
            assertTrue(createdAndGroup.isAndOperator(), "Should create AND group");
            
            // Test OR operator
            YamlRuleGroup orGroup = new YamlRuleGroup();
            orGroup.setId("or-group");
            orGroup.setName("OR Group");
            orGroup.setDescription("Test OR group");
            orGroup.setOperator("OR");
            
            RuleGroup createdOrGroup = RuleFactory.createRuleGroup(orGroup, config);
            assertFalse(createdOrGroup.isAndOperator(), "Should create OR group");
        }

        @Test
        @DisplayName("Should handle case-insensitive operator configuration")
        void testCaseInsensitiveOperator() throws ConfigurationException {
            YamlRuleGroup group = new YamlRuleGroup();
            group.setId("case-test");
            group.setName("Case Test");
            group.setDescription("Test case sensitivity");
            group.setOperator("or"); // lowercase
            
            RuleGroup createdGroup = RuleFactory.createRuleGroup(group, config);
            assertFalse(createdGroup.isAndOperator(), "Should handle lowercase 'or'");
        }

        @Test
        @DisplayName("Should default to AND operator for invalid values")
        void testInvalidOperatorDefaultsToAnd() throws ConfigurationException {
            YamlRuleGroup group = new YamlRuleGroup();
            group.setId("invalid-op");
            group.setName("Invalid Operator");
            group.setDescription("Test invalid operator");
            group.setOperator("INVALID");
            
            RuleGroup createdGroup = RuleFactory.createRuleGroup(group, config);
            assertTrue(createdGroup.isAndOperator(), "Should default to AND for invalid operator");
        }

        @Test
        @DisplayName("Should parse stop-on-first-failure configuration")
        void testStopOnFirstFailureConfiguration() throws ConfigurationException {
            // Test enabled
            YamlRuleGroup enabledGroup = new YamlRuleGroup();
            enabledGroup.setId("enabled-stop");
            enabledGroup.setName("Enabled Stop");
            enabledGroup.setDescription("Test enabled stop");
            enabledGroup.setStopOnFirstFailure(true);
            
            RuleGroup createdEnabledGroup = RuleFactory.createRuleGroup(enabledGroup, config);
            assertTrue(createdEnabledGroup.isStopOnFirstFailure(), "Should enable stop-on-first-failure");
            
            // Test disabled
            YamlRuleGroup disabledGroup = new YamlRuleGroup();
            disabledGroup.setId("disabled-stop");
            disabledGroup.setName("Disabled Stop");
            disabledGroup.setDescription("Test disabled stop");
            disabledGroup.setStopOnFirstFailure(false);
            
            RuleGroup createdDisabledGroup = RuleFactory.createRuleGroup(disabledGroup, config);
            assertFalse(createdDisabledGroup.isStopOnFirstFailure(), "Should disable stop-on-first-failure");
        }

        @Test
        @DisplayName("Should parse parallel-execution configuration")
        void testParallelExecutionConfiguration() throws ConfigurationException {
            // Test enabled
            YamlRuleGroup parallelGroup = new YamlRuleGroup();
            parallelGroup.setId("parallel-group");
            parallelGroup.setName("Parallel Group");
            parallelGroup.setDescription("Test parallel execution");
            parallelGroup.setParallelExecution(true);
            
            RuleGroup createdParallelGroup = RuleFactory.createRuleGroup(parallelGroup, config);
            assertTrue(createdParallelGroup.isParallelExecution(), "Should enable parallel execution");
            
            // Test disabled
            YamlRuleGroup sequentialGroup = new YamlRuleGroup();
            sequentialGroup.setId("sequential-group");
            sequentialGroup.setName("Sequential Group");
            sequentialGroup.setDescription("Test sequential execution");
            sequentialGroup.setParallelExecution(false);
            
            RuleGroup createdSequentialGroup = RuleFactory.createRuleGroup(sequentialGroup, config);
            assertFalse(createdSequentialGroup.isParallelExecution(), "Should disable parallel execution");
        }

        @Test
        @DisplayName("Should parse debug-mode configuration")
        void testDebugModeConfiguration() throws ConfigurationException {
            // Test enabled
            YamlRuleGroup debugGroup = new YamlRuleGroup();
            debugGroup.setId("debug-group");
            debugGroup.setName("Debug Group");
            debugGroup.setDescription("Test debug mode");
            debugGroup.setDebugMode(true);
            
            RuleGroup createdDebugGroup = RuleFactory.createRuleGroup(debugGroup, config);
            assertTrue(createdDebugGroup.isDebugMode(), "Should enable debug mode");
            
            // Test disabled
            YamlRuleGroup normalGroup = new YamlRuleGroup();
            normalGroup.setId("normal-group");
            normalGroup.setName("Normal Group");
            normalGroup.setDescription("Test normal mode");
            normalGroup.setDebugMode(false);
            
            RuleGroup createdNormalGroup = RuleFactory.createRuleGroup(normalGroup, config);
            assertFalse(createdNormalGroup.isDebugMode(), "Should disable debug mode");
        }
    }

    // ========================================
    // Default Values Tests
    // ========================================

    @Nested
    @DisplayName("Default Values")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should use correct default values")
        void testDefaultValues() throws ConfigurationException {
            YamlRuleGroup group = new YamlRuleGroup();
            group.setId("default-test");
            group.setName("Default Test");
            group.setDescription("Test default values");
            // Don't set any optional fields
            
            RuleGroup createdGroup = RuleFactory.createRuleGroup(group, config);
            
            assertTrue(createdGroup.isAndOperator(), "Should default to AND operator");
            assertFalse(createdGroup.isStopOnFirstFailure(), "Should default to stop-on-first-failure disabled (YAML default)");
            assertFalse(createdGroup.isParallelExecution(), "Should default to parallel execution disabled");
            assertFalse(createdGroup.isDebugMode(), "Should default to debug mode disabled");
        }

        @Test
        @DisplayName("Should handle null configuration values")
        void testNullConfigurationValues() throws ConfigurationException {
            YamlRuleGroup group = new YamlRuleGroup();
            group.setId("null-test");
            group.setName("Null Test");
            group.setDescription("Test null values");
            group.setOperator(null);
            group.setStopOnFirstFailure(null);
            group.setParallelExecution(null);
            group.setDebugMode(null);
            
            RuleGroup createdGroup = RuleFactory.createRuleGroup(group, config);
            
            assertTrue(createdGroup.isAndOperator(), "Should default to AND for null operator");
            assertFalse(createdGroup.isStopOnFirstFailure(), "Should default to false for null stop-on-first-failure (YAML default)");
            assertFalse(createdGroup.isParallelExecution(), "Should default to false for null parallel-execution");
            assertFalse(createdGroup.isDebugMode(), "Should default to false for null debug-mode");
        }
    }

    // ========================================
    // System Property Tests
    // ========================================

    @Nested
    @DisplayName("System Property Integration")
    class SystemPropertyTests {

        @Test
        @DisplayName("Should respect system property for debug mode")
        void testSystemPropertyDebugMode() throws ConfigurationException {
            // Set system property
            System.setProperty("apex.rulegroup.debug", "true");
            
            try {
                YamlRuleGroup group = new YamlRuleGroup();
                group.setId("sysprop-test");
                group.setName("System Property Test");
                group.setDescription("Test system property");
                // Don't set debug-mode in YAML
                
                RuleGroup createdGroup = RuleFactory.createRuleGroup(group, config);
                
                assertTrue(createdGroup.isDebugMode(), "Should enable debug mode via system property");
            } finally {
                // Clean up system property
                System.clearProperty("apex.rulegroup.debug");
            }
        }

        @Test
        @DisplayName("YAML configuration should override system property")
        void testYamlOverridesSystemProperty() throws ConfigurationException {
            // Set system property to true
            System.setProperty("apex.rulegroup.debug", "true");
            
            try {
                YamlRuleGroup group = new YamlRuleGroup();
                group.setId("override-test");
                group.setName("Override Test");
                group.setDescription("Test YAML override");
                group.setDebugMode(false); // Explicitly disable in YAML
                
                RuleGroup createdGroup = RuleFactory.createRuleGroup(group, config);
                
                assertFalse(createdGroup.isDebugMode(), "YAML configuration should override system property");
            } finally {
                // Clean up system property
                System.clearProperty("apex.rulegroup.debug");
            }
        }
    }

    // ========================================
    // Complex Configuration Tests
    // ========================================

    @Nested
    @DisplayName("Complex Configuration Scenarios")
    class ComplexConfigurationTests {

        @Test
        @DisplayName("Should handle all advanced features together")
        void testAllAdvancedFeaturesTogether() throws ConfigurationException {
            YamlRuleGroup group = new YamlRuleGroup();
            group.setId("complex-group");
            group.setName("Complex Group");
            group.setDescription("Test all features");
            group.setOperator("OR");
            group.setStopOnFirstFailure(false);
            group.setParallelExecution(true);
            group.setDebugMode(true);
            
            RuleGroup createdGroup = RuleFactory.createRuleGroup(group, config);
            
            assertFalse(createdGroup.isAndOperator(), "Should be OR group");
            assertFalse(createdGroup.isStopOnFirstFailure(), "Should disable short-circuiting");
            assertTrue(createdGroup.isParallelExecution(), "Should enable parallel execution");
            assertTrue(createdGroup.isDebugMode(), "Should enable debug mode");
        }

        @Test
        @DisplayName("Should handle production-optimized configuration")
        void testProductionOptimizedConfiguration() throws ConfigurationException {
            YamlRuleGroup group = new YamlRuleGroup();
            group.setId("production-group");
            group.setName("Production Group");
            group.setDescription("Production optimized");
            group.setOperator("AND");
            group.setStopOnFirstFailure(true);  // Enable short-circuiting for performance
            group.setParallelExecution(false);  // Disable parallel for simplicity
            group.setDebugMode(false);          // Disable debug for performance
            
            RuleGroup createdGroup = RuleFactory.createRuleGroup(group, config);
            
            assertTrue(createdGroup.isAndOperator(), "Should be AND group");
            assertTrue(createdGroup.isStopOnFirstFailure(), "Should enable short-circuiting");
            assertFalse(createdGroup.isParallelExecution(), "Should disable parallel execution");
            assertFalse(createdGroup.isDebugMode(), "Should disable debug mode");
        }

        @Test
        @DisplayName("Should handle debug-optimized configuration")
        void testDebugOptimizedConfiguration() throws ConfigurationException {
            YamlRuleGroup group = new YamlRuleGroup();
            group.setId("debug-optimized");
            group.setName("Debug Optimized");
            group.setDescription("Debug optimized");
            group.setOperator("AND");
            group.setStopOnFirstFailure(false); // Disable short-circuiting for complete evaluation
            group.setParallelExecution(false);  // Disable parallel for deterministic debugging
            group.setDebugMode(true);           // Enable debug logging
            
            RuleGroup createdGroup = RuleFactory.createRuleGroup(group, config);
            
            assertTrue(createdGroup.isAndOperator(), "Should be AND group");
            assertFalse(createdGroup.isStopOnFirstFailure(), "Should disable short-circuiting");
            assertFalse(createdGroup.isParallelExecution(), "Should disable parallel execution");
            assertTrue(createdGroup.isDebugMode(), "Should enable debug mode");
        }
    }

    // ========================================
    // Getter/Setter Tests
    // ========================================

    @Nested
    @DisplayName("YamlRuleGroup Getter/Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should handle operator getter/setter")
        void testOperatorGetterSetter() {
            YamlRuleGroup group = new YamlRuleGroup();
            
            assertNull(group.getOperator(), "Initial operator should be null");
            
            group.setOperator("AND");
            assertEquals("AND", group.getOperator(), "Should set AND operator");
            
            group.setOperator("OR");
            assertEquals("OR", group.getOperator(), "Should set OR operator");
        }

        @Test
        @DisplayName("Should handle debug-mode getter/setter")
        void testDebugModeGetterSetter() {
            YamlRuleGroup group = new YamlRuleGroup();
            
            assertNull(group.getDebugMode(), "Initial debug mode should be null");
            
            group.setDebugMode(true);
            assertTrue(group.getDebugMode(), "Should set debug mode to true");
            
            group.setDebugMode(false);
            assertFalse(group.getDebugMode(), "Should set debug mode to false");
        }

        @Test
        @DisplayName("Should handle all new fields")
        void testAllNewFields() {
            YamlRuleGroup group = new YamlRuleGroup();
            
            // Test initial values
            assertNull(group.getOperator());
            assertNull(group.getDebugMode());
            
            // Test setting values
            group.setOperator("OR");
            group.setDebugMode(true);
            
            // Test getting values
            assertEquals("OR", group.getOperator());
            assertTrue(group.getDebugMode());
        }
    }
}
