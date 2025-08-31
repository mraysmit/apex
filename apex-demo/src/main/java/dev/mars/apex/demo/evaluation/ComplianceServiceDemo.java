package dev.mars.apex.demo.evaluation;

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

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * APEX-Compliant Compliance Service Demo for Regulatory Compliance Processing.
 *
 * This class demonstrates authentic APEX integration using real APEX core services
 * instead of hardcoded simulation. Following the SimplePostgreSQLLookupDemo pattern:
 *
 * ============================================================================
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor for compliance processing
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for compliance rules
 * - LookupServiceRegistry: Real lookup service integration for regulatory data
 * ============================================================================
 *
 * CRITICAL: This class eliminates ALL hardcoded compliance logic and uses:
 * - YAML-driven compliance configuration from external files
 * - Real APEX enrichment services for regulatory processing
 * - Fail-fast error handling (no hardcoded fallbacks)
 * - Authentic APEX service integration throughout
 *
 * REFACTORING NOTES:
 * - Replaced hardcoded regulatory constants with YAML-driven configuration
 * - Eliminated embedded compliance rules and business logic
 * - Uses real APEX enrichment services for compliance evaluation
 * - Follows fail-fast approach when YAML configurations are missing
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 2.0 - Real APEX services integration (Reference Template)
 */
public class ComplianceServiceDemo {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceServiceDemo.class);

    // Real APEX services for authentic integration
    private final YamlConfigurationLoader yamlLoader;
    private final EnrichmentService enrichmentService;
    private final LookupServiceRegistry serviceRegistry;
    private final ExpressionEvaluatorService expressionEvaluator;

    // Configuration data (populated via real APEX processing)
    private Map<String, Object> configurationData;

    /**
     * Initialize the compliance service demo with real APEX services.
     */
    public ComplianceServiceDemo() {
        // Initialize real APEX services for authentic integration
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);

        logger.info("ComplianceServiceDemo initialized with real APEX services");

        try {
            loadExternalConfiguration();
        } catch (Exception e) {
            logger.error("Failed to initialize ComplianceServiceDemo: {}", e.getMessage());
            throw new RuntimeException("Compliance service demo initialization failed", e);
        }
    }

    /**
     * Loads external YAML configuration.
     */
    private void loadExternalConfiguration() throws Exception {
        logger.info("Loading external compliance configuration YAML...");

        configurationData = new HashMap<>();
        
        try {
            // Load main compliance configuration
            YamlRuleConfiguration mainConfig = yamlLoader.loadFromClasspath("evaluation/compliance-service-demo.yaml");
            configurationData.put("mainConfig", mainConfig);
            
            // Load regulatory requirements configuration
            YamlRuleConfiguration regulatoryConfig = yamlLoader.loadFromClasspath("evaluation/compliance/regulatory-requirements.yaml");
            configurationData.put("regulatoryConfig", regulatoryConfig);
            
            // Load reporting deadlines configuration
            YamlRuleConfiguration deadlinesConfig = yamlLoader.loadFromClasspath("evaluation/compliance/reporting-deadlines.yaml");
            configurationData.put("deadlinesConfig", deadlinesConfig);
            
            // Load compliance rules configuration
            YamlRuleConfiguration rulesConfig = yamlLoader.loadFromClasspath("evaluation/compliance/compliance-rules.yaml");
            configurationData.put("rulesConfig", rulesConfig);
            
            // Load test data configuration
            YamlRuleConfiguration testDataConfig = yamlLoader.loadFromClasspath("evaluation/compliance/compliance-test-data.yaml");
            configurationData.put("testDataConfig", testDataConfig);
            
            logger.info("External compliance configuration YAML loaded successfully");
            
        } catch (Exception e) {
            logger.warn("External compliance YAML files not found, APEX enrichment will use fail-fast approach: {}", e.getMessage());
            throw new RuntimeException("Required compliance configuration YAML files not found", e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT COMPLIANCE PROCESSING METHODS (Real APEX Service Integration)
    // ============================================================================

    /**
     * Processes compliance requirements for a trade using real APEX enrichment.
     */
    public Map<String, Object> processTradeCompliance(Map<String, Object> tradeData) {
        try {
            logger.info("Processing trade compliance using real APEX enrichment...");

            // Load main compliance configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main compliance configuration not found");
            }

            // Use real APEX enrichment service for compliance processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, tradeData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Trade compliance processed successfully using real APEX enrichment");
            return result;

        } catch (Exception e) {
            logger.error("Failed to process trade compliance with APEX enrichment: {}", e.getMessage());
            throw new RuntimeException("Trade compliance processing failed", e);
        }
    }

    /**
     * Checks MiFID II reporting requirements using real APEX enrichment.
     */
    public Map<String, Object> checkMiFIDReporting(Map<String, Object> tradeData) {
        try {
            logger.info("Checking MiFID II reporting requirements using real APEX enrichment...");

            // Add regulation-specific context
            Map<String, Object> enrichedTradeData = new HashMap<>(tradeData);
            enrichedTradeData.put("regulationType", "MiFID_II");
            enrichedTradeData.put("checkType", "reporting-requirement");

            return processTradeCompliance(enrichedTradeData);

        } catch (Exception e) {
            logger.error("Failed to check MiFID II reporting requirements: {}", e.getMessage());
            throw new RuntimeException("MiFID II reporting check failed", e);
        }
    }

    /**
     * Checks EMIR reporting requirements using real APEX enrichment.
     */
    public Map<String, Object> checkEMIRReporting(Map<String, Object> tradeData) {
        try {
            logger.info("Checking EMIR reporting requirements using real APEX enrichment...");

            // Add regulation-specific context
            Map<String, Object> enrichedTradeData = new HashMap<>(tradeData);
            enrichedTradeData.put("regulationType", "EMIR");
            enrichedTradeData.put("checkType", "reporting-requirement");

            return processTradeCompliance(enrichedTradeData);

        } catch (Exception e) {
            logger.error("Failed to check EMIR reporting requirements: {}", e.getMessage());
            throw new RuntimeException("EMIR reporting check failed", e);
        }
    }

    /**
     * Checks Dodd-Frank reporting requirements using real APEX enrichment.
     */
    public Map<String, Object> checkDoddFrankReporting(Map<String, Object> tradeData) {
        try {
            logger.info("Checking Dodd-Frank reporting requirements using real APEX enrichment...");

            // Add regulation-specific context
            Map<String, Object> enrichedTradeData = new HashMap<>(tradeData);
            enrichedTradeData.put("regulationType", "DODD_FRANK");
            enrichedTradeData.put("checkType", "reporting-requirement");

            return processTradeCompliance(enrichedTradeData);

        } catch (Exception e) {
            logger.error("Failed to check Dodd-Frank reporting requirements: {}", e.getMessage());
            throw new RuntimeException("Dodd-Frank reporting check failed", e);
        }
    }

    /**
     * Checks Basel III reporting requirements using real APEX enrichment.
     */
    public Map<String, Object> checkBaselReporting(Map<String, Object> tradeData) {
        try {
            logger.info("Checking Basel III reporting requirements using real APEX enrichment...");

            // Add regulation-specific context
            Map<String, Object> enrichedTradeData = new HashMap<>(tradeData);
            enrichedTradeData.put("regulationType", "BASEL_III");
            enrichedTradeData.put("checkType", "reporting-requirement");

            return processTradeCompliance(enrichedTradeData);

        } catch (Exception e) {
            logger.error("Failed to check Basel III reporting requirements: {}", e.getMessage());
            throw new RuntimeException("Basel III reporting check failed", e);
        }
    }

    /**
     * Checks SFTR reporting requirements using real APEX enrichment.
     */
    public Map<String, Object> checkSFTRReporting(Map<String, Object> tradeData) {
        try {
            logger.info("Checking SFTR reporting requirements using real APEX enrichment...");

            // Add regulation-specific context
            Map<String, Object> enrichedTradeData = new HashMap<>(tradeData);
            enrichedTradeData.put("regulationType", "SFTR");
            enrichedTradeData.put("checkType", "reporting-requirement");

            return processTradeCompliance(enrichedTradeData);

        } catch (Exception e) {
            logger.error("Failed to check SFTR reporting requirements: {}", e.getMessage());
            throw new RuntimeException("SFTR reporting check failed", e);
        }
    }

    /**
     * Checks for compliance issues using real APEX enrichment.
     */
    public Map<String, Object> checkComplianceIssues(Map<String, Object> tradeData) {
        try {
            logger.info("Checking for compliance issues using real APEX enrichment...");

            // Add compliance check context
            Map<String, Object> enrichedTradeData = new HashMap<>(tradeData);
            enrichedTradeData.put("checkType", "compliance-issues");

            return processTradeCompliance(enrichedTradeData);

        } catch (Exception e) {
            logger.error("Failed to check compliance issues: {}", e.getMessage());
            throw new RuntimeException("Compliance issues check failed", e);
        }
    }

    /**
     * Generates compliance reports using real APEX enrichment.
     */
    public Map<String, Object> generateComplianceReport(Map<String, Object> tradeData, String reportType) {
        try {
            logger.info("Generating compliance report using real APEX enrichment...");

            // Add report generation context
            Map<String, Object> enrichedTradeData = new HashMap<>(tradeData);
            enrichedTradeData.put("reportType", reportType);
            enrichedTradeData.put("checkType", "report-generation");

            return processTradeCompliance(enrichedTradeData);

        } catch (Exception e) {
            logger.error("Failed to generate compliance report: {}", e.getMessage());
            throw new RuntimeException("Compliance report generation failed", e);
        }
    }

    // ============================================================================
    // DEMONSTRATION METHODS (APEX-Compliant)
    // ============================================================================

    /**
     * Demonstrates equity trade compliance processing with sample data.
     */
    public void demonstrateEquityTradeCompliance() {
        logger.info("Demonstrating equity trade compliance with real APEX processing...");

        // Create sample equity trade data
        Map<String, Object> equityTrade = new HashMap<>();
        equityTrade.put("tradeId", "T1001");
        equityTrade.put("tradeType", "Equity");
        equityTrade.put("category", "Stock");
        equityTrade.put("amount", 100000);
        equityTrade.put("currency", "EUR");
        equityTrade.put("counterparty", "BANK_A");

        // Process using real APEX enrichment
        Map<String, Object> result = processTradeCompliance(equityTrade);

        logger.info("Equity trade compliance result: {}", result);
        System.out.println("=== Equity Trade Compliance Demo Results ===");
        System.out.println("Input: " + equityTrade);
        System.out.println("Result: " + result);
    }

    /**
     * Demonstrates derivative trade compliance processing with sample data.
     */
    public void demonstrateDerivativeTradeCompliance() {
        logger.info("Demonstrating derivative trade compliance with real APEX processing...");

        // Create sample derivative trade data
        Map<String, Object> derivativeTrade = new HashMap<>();
        derivativeTrade.put("tradeId", "T1003");
        derivativeTrade.put("tradeType", "Derivative");
        derivativeTrade.put("category", "Option");
        derivativeTrade.put("amount", 250000);
        derivativeTrade.put("currency", "GBP");
        derivativeTrade.put("counterparty", "BANK_C");

        // Process using real APEX enrichment
        Map<String, Object> result = processTradeCompliance(derivativeTrade);

        logger.info("Derivative trade compliance result: {}", result);
        System.out.println("=== Derivative Trade Compliance Demo Results ===");
        System.out.println("Input: " + derivativeTrade);
        System.out.println("Result: " + result);
    }

    /**
     * Demonstrates MiFID II reporting check with sample data.
     */
    public void demonstrateMiFIDReportingCheck() {
        logger.info("Demonstrating MiFID II reporting check with real APEX processing...");

        // Create sample trade data for MiFID II check
        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("tradeId", "T2001");
        tradeData.put("tradeType", "FixedIncome");
        tradeData.put("category", "Bond");
        tradeData.put("amount", 500000);
        tradeData.put("currency", "USD");

        // Process using real APEX enrichment
        Map<String, Object> result = checkMiFIDReporting(tradeData);

        logger.info("MiFID II reporting check result: {}", result);
        System.out.println("=== MiFID II Reporting Check Demo Results ===");
        System.out.println("Input: " + tradeData);
        System.out.println("Result: " + result);
    }

    /**
     * Demonstrates compliance issues detection with sample data.
     */
    public void demonstrateComplianceIssuesCheck() {
        logger.info("Demonstrating compliance issues check with real APEX processing...");

        // Create sample trade data with potential issues
        Map<String, Object> problematicTrade = new HashMap<>();
        problematicTrade.put("tradeId", ""); // Missing trade ID
        problematicTrade.put("tradeType", "Equity");
        problematicTrade.put("category", "Stock");
        problematicTrade.put("amount", 50000);
        problematicTrade.put("riskLevel", "High");

        // Process using real APEX enrichment
        Map<String, Object> result = checkComplianceIssues(problematicTrade);

        logger.info("Compliance issues check result: {}", result);
        System.out.println("=== Compliance Issues Check Demo Results ===");
        System.out.println("Input: " + problematicTrade);
        System.out.println("Result: " + result);
    }

    /**
     * Demonstrates comprehensive compliance processing with sample data.
     */
    public void demonstrateComprehensiveCompliance() {
        logger.info("Demonstrating comprehensive compliance processing with real APEX processing...");

        // Create sample comprehensive trade data
        Map<String, Object> comprehensiveTrade = new HashMap<>();
        comprehensiveTrade.put("tradeId", "C1001");
        comprehensiveTrade.put("tradeType", "Derivative");
        comprehensiveTrade.put("category", "Interest Rate Swap");
        comprehensiveTrade.put("amount", 2000000);
        comprehensiveTrade.put("currency", "EUR");
        comprehensiveTrade.put("counterparty", "MAJOR_BANK");
        comprehensiveTrade.put("riskLevel", "Medium");
        comprehensiveTrade.put("jurisdiction", "EU");

        // Process using real APEX enrichment
        Map<String, Object> result = processTradeCompliance(comprehensiveTrade);

        logger.info("Comprehensive compliance result: {}", result);
        System.out.println("=== Comprehensive Compliance Demo Results ===");
        System.out.println("Input: " + comprehensiveTrade);
        System.out.println("Result: " + result);
    }

    // ============================================================================
    // MAIN METHOD FOR DEMONSTRATION
    // ============================================================================

    /**
     * Main method to demonstrate APEX-compliant compliance processing.
     */
    public static void main(String[] args) {
        try {
            logger.info("Starting APEX-compliant compliance service demonstration...");

            // Initialize with real APEX services
            ComplianceServiceDemo demo = new ComplianceServiceDemo();

            // Run all demonstrations
            demo.demonstrateEquityTradeCompliance();
            System.out.println();

            demo.demonstrateDerivativeTradeCompliance();
            System.out.println();

            demo.demonstrateMiFIDReportingCheck();
            System.out.println();

            demo.demonstrateComplianceIssuesCheck();
            System.out.println();

            demo.demonstrateComprehensiveCompliance();

            logger.info("APEX-compliant compliance service demonstration completed successfully");

        } catch (Exception e) {
            logger.error("Compliance service demonstration failed: {}", e.getMessage());
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
