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
 * TradeTransformerDemoTest - APEX Trade Data Transformation Demo
 * 
 * PURPOSE: Showcase APEX's trade transformation capabilities through:
 * - APEX field enrichment for instrument and priority classification
 * - APEX calculation enrichment for value categorization and risk scoring
 * - APEX conditional processing based on trade attributes
 * - APEX SpEL expressions for complex business logic
 * 
 * TRADE TRANSFORMATION FEATURES DEMONSTRATED:
 * - Instrument Classification: EQUITY, BOND categorization
 * - Priority Processing: Trade priority classification
 * - Value Categorization: SMALL, MEDIUM, LARGE trade classification
 * - Risk Scoring: Mathematical risk calculation using SpEL
 * 
 * CRITICAL TRADE PROCESSING CHECKLIST APPLIED:
 *  Verify 4 trade enrichments process successfully
 *  Validate instrument classification logic
 *  Check value categorization calculations
 *  Assert risk scoring mathematical accuracy
 *  Confirm conditional processing based on trade data
 * 
 * ALL TRADE LOGIC IS IN APEX YAML FILES - NO CUSTOM JAVA LOGIC
 * Tests validate APEX trade transformation using established patterns
 * 
 * Following established patterns from previous lookup tests
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Trade Transformation Tests")
public class TradeTransformerDemoTest {
    
    private static final Logger logger = LoggerFactory.getLogger(TradeTransformerDemoTest.class);

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
            // Load trade transformation configuration
            config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/TradeTransformerDemoTest.yaml");
            
            logger.info("✅ APEX services initialized for trade transformation testing");
            logger.info("  - Configuration loaded: {}", config.getMetadata().getName());
            logger.info("  - Trade enrichments: {}", config.getEnrichments().size());
            
        } catch (YamlConfigurationException e) {
            logger.error("X Failed to load configuration: {}", e.getMessage());
            fail("Failed to load configuration: " + e.getMessage());
        }
    }

    /**
     * Create RulesEngine with EnrichmentService for trade processing
     * Following the established pattern from previous tests
     */
    private RulesEngine createRulesEngineWithEnrichmentService(YamlRuleConfiguration config) throws YamlConfigurationException {
        // Create basic configuration from YAML using the standard method
        RulesEngine baseEngine = rulesEngineService.createRulesEngineFromYamlConfig(config);
        RulesEngineConfiguration rulesConfig = baseEngine.getConfiguration();

        // Create RulesEngine with EnrichmentService
        RulesEngine engine = new RulesEngine(rulesConfig, new SpelExpressionParser(),
                                           new ErrorRecoveryService(), new RulePerformanceMonitor(), enrichmentService);

        assertNotNull(engine, "RulesEngine should be created");
        logger.info("✅ RulesEngine created with EnrichmentService for trade transformation");

        return engine;
    }

    @Test
    @DisplayName("Test Equity Trade Transformation")
    void testEquityTradeTransformation() {
        logger.info("=== Testing Equity Trade Transformation ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test equity trade with high value
            logger.info("Testing equity trade transformation...");
            Map<String, Object> equityTrade = new HashMap<>();
            equityTrade.put("instrumentType", "EQUITY");
            equityTrade.put("priority", "HIGH");
            equityTrade.put("tradeValue", 2500000.0); // Large trade

            RuleResult result = engine.evaluate(config, equityTrade);
            assertNotNull(result, "Equity trade result should not be null");
            
            Map<String, Object> enrichedData = result.getEnrichedData();
            logger.info("✓ Equity trade transformation successful:");
            logger.info("  - Instrument Category: {}", enrichedData.get("instrumentCategory"));
            logger.info("  - Priority Category: {}", enrichedData.get("priorityCategory"));
            logger.info("  - Value Category: {}", enrichedData.get("valueCategory"));
            logger.info("  - Risk Score: {}", enrichedData.get("riskScore"));

            assertEquals("EQUITY", enrichedData.get("instrumentCategory"), "Instrument category should be EQUITY");
            assertEquals("HIGH", enrichedData.get("priorityCategory"), "Priority category should be HIGH");
            assertEquals("LARGE", enrichedData.get("valueCategory"), "Value category should be LARGE");
            
            // Risk score should be 0.8 * (2500000 / 1000000) = 2.0
            assertEquals(2.0, (Double) enrichedData.get("riskScore"), 0.01, "Risk score should be 2.0");

        } catch (Exception e) {
            logger.error("X Equity trade transformation failed: {}", e.getMessage());
            fail("Equity trade transformation failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Bond Trade Transformation")
    void testBondTradeTransformation() {
        logger.info("=== Testing Bond Trade Transformation ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test bond trade with medium value
            logger.info("Testing bond trade transformation...");
            Map<String, Object> bondTrade = new HashMap<>();
            bondTrade.put("instrumentType", "BOND");
            bondTrade.put("priority", "MEDIUM");
            bondTrade.put("tradeValue", 500000.0); // Medium trade

            RuleResult result = engine.evaluate(config, bondTrade);
            assertNotNull(result, "Bond trade result should not be null");
            
            Map<String, Object> enrichedData = result.getEnrichedData();
            logger.info("✓ Bond trade transformation successful:");
            logger.info("  - Instrument Category: {}", enrichedData.get("instrumentCategory"));
            logger.info("  - Priority Category: {}", enrichedData.get("priorityCategory"));
            logger.info("  - Value Category: {}", enrichedData.get("valueCategory"));
            logger.info("  - Risk Score: {}", enrichedData.get("riskScore"));

            assertEquals("BOND", enrichedData.get("instrumentCategory"), "Instrument category should be BOND");
            assertEquals("MEDIUM", enrichedData.get("priorityCategory"), "Priority category should be MEDIUM");
            assertEquals("MEDIUM", enrichedData.get("valueCategory"), "Value category should be MEDIUM");
            
            // Risk score should be 0.3 * (500000 / 1000000) = 0.15
            assertEquals(0.15, (Double) enrichedData.get("riskScore"), 0.01, "Risk score should be 0.15");

        } catch (Exception e) {
            logger.error("X Bond trade transformation failed: {}", e.getMessage());
            fail("Bond trade transformation failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Small Trade Transformation")
    void testSmallTradeTransformation() {
        logger.info("=== Testing Small Trade Transformation ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test small trade
            logger.info("Testing small trade transformation...");
            Map<String, Object> smallTrade = new HashMap<>();
            smallTrade.put("instrumentType", "OPTION");
            smallTrade.put("priority", "LOW");
            smallTrade.put("tradeValue", 50000.0); // Small trade

            RuleResult result = engine.evaluate(config, smallTrade);
            assertNotNull(result, "Small trade result should not be null");
            
            Map<String, Object> enrichedData = result.getEnrichedData();
            logger.info("✓ Small trade transformation successful:");
            logger.info("  - Instrument Category: {}", enrichedData.get("instrumentCategory"));
            logger.info("  - Priority Category: {}", enrichedData.get("priorityCategory"));
            logger.info("  - Value Category: {}", enrichedData.get("valueCategory"));
            logger.info("  - Risk Score: {}", enrichedData.get("riskScore"));

            assertEquals("OPTION", enrichedData.get("instrumentCategory"), "Instrument category should be OPTION");
            assertEquals("LOW", enrichedData.get("priorityCategory"), "Priority category should be LOW");
            assertEquals("SMALL", enrichedData.get("valueCategory"), "Value category should be SMALL");
            
            // Risk score should be 0.5 * (50000 / 1000000) = 0.025
            assertEquals(0.025, (Double) enrichedData.get("riskScore"), 0.001, "Risk score should be 0.025");

        } catch (Exception e) {
            logger.error("X Small trade transformation failed: {}", e.getMessage());
            fail("Small trade transformation failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Complete Trade Portfolio Transformation")
    void testCompleteTradePortfolioTransformation() {
        logger.info("=== Testing Complete Trade Portfolio Transformation ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test complete trade portfolio with all data
            logger.info("Testing complete trade portfolio transformation...");
            Map<String, Object> portfolioTrade = new HashMap<>();
            portfolioTrade.put("instrumentType", "EQUITY");
            portfolioTrade.put("priority", "HIGH");
            portfolioTrade.put("tradeValue", 5000000.0); // Institutional trade

            RuleResult result = engine.evaluate(config, portfolioTrade);
            assertNotNull(result, "Portfolio trade result should not be null");
            
            Map<String, Object> enrichedData = result.getEnrichedData();
            
            logger.info("✓ Complete trade portfolio transformation processed successfully");
            logger.info("  - Trade Details: {} {} trade valued at ${}", 
                enrichedData.get("priorityCategory"), 
                enrichedData.get("instrumentCategory"), 
                portfolioTrade.get("tradeValue"));
            logger.info("  - Classification: {} value category with risk score {}", 
                enrichedData.get("valueCategory"), 
                enrichedData.get("riskScore"));

            // Validate all transformations worked correctly
            assertEquals("EQUITY", enrichedData.get("instrumentCategory"), "Should be EQUITY instrument");
            assertEquals("HIGH", enrichedData.get("priorityCategory"), "Should be HIGH priority");
            assertEquals("LARGE", enrichedData.get("valueCategory"), "Should be LARGE value category");
            
            // Risk score should be 0.8 * (5000000 / 1000000) = 4.0
            assertEquals(4.0, (Double) enrichedData.get("riskScore"), 0.01, "Risk score should be 4.0");

        } catch (Exception e) {
            logger.error("X Complete trade portfolio transformation failed: {}", e.getMessage());
            fail("Complete trade portfolio transformation failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Partial Trade Data Transformation")
    void testPartialTradeDataTransformation() {
        logger.info("=== Testing Partial Trade Data Transformation ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test with only instrument type (missing priority and value)
            logger.info("Testing partial trade data transformation...");
            Map<String, Object> partialTrade = new HashMap<>();
            partialTrade.put("instrumentType", "BOND");
            // Missing priority and tradeValue

            RuleResult result = engine.evaluate(config, partialTrade);
            assertNotNull(result, "Partial trade result should not be null");
            
            Map<String, Object> enrichedData = result.getEnrichedData();
            logger.info("✓ Partial trade data transformation handled gracefully");
            logger.info("  - Enriched data size: {}", enrichedData.size());
            logger.info("  - Available data: {}", enrichedData.keySet());

            // Should have instrument category but not others due to missing conditions
            assertEquals("BOND", enrichedData.get("instrumentCategory"), "Should have instrument category");
            assertNull(enrichedData.get("priorityCategory"), "Should not have priority category");
            assertNull(enrichedData.get("valueCategory"), "Should not have value category");
            assertNull(enrichedData.get("riskScore"), "Should not have risk score");

        } catch (Exception e) {
            logger.error("X Partial trade data transformation failed: {}", e.getMessage());
            fail("Partial trade data transformation failed: " + e.getMessage());
        }
    }
}
