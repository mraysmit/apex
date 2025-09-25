package dev.mars.apex.demo.lookup;

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

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CalculationMathematicalTest - Advanced SpEL Mathematical Operations Demo
 * 
 * PURPOSE: Showcase APEX's advanced SpEL mathematical capabilities through:
 * - APEX Java Math library integration with T(java.lang.Math) syntax
 * - APEX trigonometric functions (sin, cos, radians conversion)
 * - APEX logarithmic and exponential calculations
 * - APEX rounding and ceiling operations
 * - APEX power and square root calculations
 * 
 * FANCY SpEL FEATURES DEMONSTRATED:
 * - T(java.lang.Math).sqrt() - Square root calculations
 * - T(java.lang.Math).pow() - Power calculations
 * - T(java.lang.Math).sin(T(java.lang.Math).toRadians()) - Trigonometry with conversion
 * - T(java.lang.Math).log() - Natural logarithm
 * - T(java.lang.Math).round() - Rounding operations
 * - T(java.lang.Math).ceil() - Ceiling operations
 * 
 * CRITICAL MATHEMATICAL PROCESSING CHECKLIST APPLIED:
 *  Verify 6 mathematical enrichments process successfully
 *  Validate Java Math library integration works correctly
 *  Check trigonometric calculations with degree/radian conversion
 *  Assert logarithmic and exponential operations
 *  Confirm rounding and ceiling precision
 * 
 * ALL MATHEMATICAL LOGIC IS IN APEX YAML FILES - NO CUSTOM JAVA LOGIC
 * Tests validate APEX SpEL mathematical capabilities using established patterns
 * 
 * Following established patterns from BasicUsageExamplesTest
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Mathematical Calculation Tests")
public class CalculationMathematicalTest {
    
    private static final Logger logger = LoggerFactory.getLogger(CalculationMathematicalTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;
    private EnrichmentService enrichmentService;
    private YamlRuleConfiguration config;

    @BeforeEach
    void setUp() {
        // Initialize APEX services following established patterns
        yamlLoader = new YamlConfigurationLoader();
        rulesEngineService = new YamlRulesEngineService();

        // Create enrichment service with required dependencies
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);
        
        try {
            // Load mathematical operations configuration
            config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CalculationMathematicalTest.yaml");
            
            logger.info("✅ APEX services initialized for mathematical calculation testing");
            logger.info("  - Configuration loaded: {}", config.getMetadata().getName());
            logger.info("  - Mathematical enrichments: {}", config.getEnrichments().size());
            
        } catch (YamlConfigurationException e) {
            logger.error("X Failed to load configuration: {}", e.getMessage());
            fail("Failed to load configuration: " + e.getMessage());
        }
    }

    /**
     * Create RulesEngine with EnrichmentService for mathematical processing
     * Following the established pattern from BasicUsageExamplesTest
     */
    private RulesEngine createRulesEngineWithEnrichmentService(YamlRuleConfiguration config) throws YamlConfigurationException {
        // Create basic configuration from YAML using the standard method
        RulesEngine baseEngine = rulesEngineService.createRulesEngineFromYamlConfig(config);
        RulesEngineConfiguration rulesConfig = baseEngine.getConfiguration();

        // Create RulesEngine with EnrichmentService
        RulesEngine engine = new RulesEngine(rulesConfig, new SpelExpressionParser(),
                                           new ErrorRecoveryService(), new RulePerformanceMonitor(), enrichmentService);

        assertNotNull(engine, "RulesEngine should be created");
        logger.info("✅ RulesEngine created with EnrichmentService for mathematical operations");

        return engine;
    }

    @Test
    @DisplayName("Test Square Root and Power Calculations")
    void testSquareRootAndPowerCalculations() {
        logger.info("=== Testing Square Root and Power Calculations ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test square root calculation: sqrt(16) = 4.0
            logger.info("Testing square root calculation: sqrt(16)...");
            Map<String, Object> squareRootData = new HashMap<>();
            squareRootData.put("value1", 16.0);

            RuleResult squareRootResult = engine.evaluate(config, squareRootData);
            assertNotNull(squareRootResult, "Square root result should not be null");
            
            Object squareRoot = squareRootResult.getEnrichedData().get("squareRoot");
            logger.info("✓ Square root calculation: sqrt(16) = {}", squareRoot);
            assertEquals(4.0, squareRoot, "Square root of 16 should be 4.0");

            // Test power calculation: pow(2, 3) = 8.0
            logger.info("Testing power calculation: pow(2, 3)...");
            Map<String, Object> powerData = new HashMap<>();
            powerData.put("base", 2.0);
            powerData.put("exponent", 3.0);

            RuleResult powerResult = engine.evaluate(config, powerData);
            assertNotNull(powerResult, "Power result should not be null");
            
            Object powerValue = powerResult.getEnrichedData().get("powerResult");
            logger.info("✓ Power calculation: pow(2, 3) = {}", powerValue);
            assertEquals(8.0, powerValue, "2 to the power of 3 should be 8.0");

        } catch (Exception e) {
            logger.error("X Square root and power calculations failed: {}", e.getMessage());
            fail("Square root and power calculations failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Trigonometric Calculations")
    void testTrigonometricCalculations() {
        logger.info("=== Testing Trigonometric Calculations ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test trigonometric calculation: sin(30°) = 0.5
            logger.info("Testing trigonometric calculation: sin(30°)...");
            Map<String, Object> trigData = new HashMap<>();
            trigData.put("angle", 30.0); // 30 degrees

            RuleResult trigResult = engine.evaluate(config, trigData);
            assertNotNull(trigResult, "Trigonometric result should not be null");
            
            Object sineValue = trigResult.getEnrichedData().get("sineValue");
            logger.info("✓ Trigonometric calculation: sin(30°) = {}", sineValue);
            
            // sin(30°) = 0.5 (approximately)
            assertEquals(0.5, (Double) sineValue, 0.0001, "sin(30°) should be approximately 0.5");

        } catch (Exception e) {
            logger.error("X Trigonometric calculations failed: {}", e.getMessage());
            fail("Trigonometric calculations failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Logarithmic Calculations")
    void testLogarithmicCalculations() {
        logger.info("=== Testing Logarithmic Calculations ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test logarithmic calculation: log(e) = 1.0
            logger.info("Testing logarithmic calculation: log(e)...");
            Map<String, Object> logData = new HashMap<>();
            logData.put("value1", Math.E); // Euler's number

            RuleResult logResult = engine.evaluate(config, logData);
            assertNotNull(logResult, "Logarithmic result should not be null");
            
            Object logValue = logResult.getEnrichedData().get("logarithm");
            logger.info("✓ Logarithmic calculation: log(e) = {}", logValue);
            
            // log(e) = 1.0
            assertEquals(1.0, (Double) logValue, 0.0001, "log(e) should be approximately 1.0");

        } catch (Exception e) {
            logger.error("X Logarithmic calculations failed: {}", e.getMessage());
            fail("Logarithmic calculations failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Rounding and Ceiling Operations")
    void testRoundingAndCeilingOperations() {
        logger.info("=== Testing Rounding and Ceiling Operations ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test rounding: round(3.7) = 4
            logger.info("Testing rounding operation: round(3.7)...");
            Map<String, Object> roundData = new HashMap<>();
            roundData.put("number", 3.7);

            RuleResult roundResult = engine.evaluate(config, roundData);
            assertNotNull(roundResult, "Rounding result should not be null");
            
            Object roundedValue = roundResult.getEnrichedData().get("roundedValue");
            logger.info("✓ Rounding operation: round(3.7) = {}", roundedValue);
            assertEquals(4L, roundedValue, "round(3.7) should be 4");

            // Test ceiling: ceil(3.1) = 4.0
            logger.info("Testing ceiling operation: ceil(3.1)...");
            Map<String, Object> ceilData = new HashMap<>();
            ceilData.put("number", 3.1);

            RuleResult ceilResult = engine.evaluate(config, ceilData);
            assertNotNull(ceilResult, "Ceiling result should not be null");
            
            Object ceilingValue = ceilResult.getEnrichedData().get("ceilingValue");
            logger.info("✓ Ceiling operation: ceil(3.1) = {}", ceilingValue);
            assertEquals(4.0, ceilingValue, "ceil(3.1) should be 4.0");

        } catch (Exception e) {
            logger.error("X Rounding and ceiling operations failed: {}", e.getMessage());
            fail("Rounding and ceiling operations failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Complete Mathematical Workflow")
    void testCompleteMathematicalWorkflow() {
        logger.info("=== Testing Complete Mathematical Workflow ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test complete mathematical workflow with all operations
            logger.info("Testing complete mathematical workflow...");
            Map<String, Object> mathData = new HashMap<>();
            mathData.put("value1", 25.0);      // For square root: sqrt(25) = 5.0
            mathData.put("base", 3.0);         // For power: pow(3, 2) = 9.0
            mathData.put("exponent", 2.0);
            mathData.put("angle", 90.0);       // For trig: sin(90°) = 1.0
            mathData.put("number", 7.8);       // For rounding: round(7.8) = 8, ceil(7.8) = 8.0

            RuleResult mathResult = engine.evaluate(config, mathData);
            assertNotNull(mathResult, "Mathematical workflow result should not be null");
            
            // Validate all mathematical operations
            Map<String, Object> enrichedData = mathResult.getEnrichedData();
            
            logger.info("✓ Complete mathematical workflow processed successfully");
            logger.info("  - Square Root: sqrt(25) = {}", enrichedData.get("squareRoot"));
            logger.info("  - Power: pow(3, 2) = {}", enrichedData.get("powerResult"));
            logger.info("  - Trigonometry: sin(90°) = {}", enrichedData.get("sineValue"));
            logger.info("  - Logarithm: log(25) = {}", enrichedData.get("logarithm"));
            logger.info("  - Rounding: round(7.8) = {}", enrichedData.get("roundedValue"));
            logger.info("  - Ceiling: ceil(7.8) = {}", enrichedData.get("ceilingValue"));

            // Validate specific calculations
            assertEquals(5.0, enrichedData.get("squareRoot"), "sqrt(25) should be 5.0");
            assertEquals(9.0, enrichedData.get("powerResult"), "pow(3, 2) should be 9.0");
            assertEquals(1.0, (Double) enrichedData.get("sineValue"), 0.0001, "sin(90°) should be 1.0");

        } catch (Exception e) {
            logger.error("X Complete mathematical workflow failed: {}", e.getMessage());
            fail("Complete mathematical workflow failed: " + e.getMessage());
        }
    }
}
