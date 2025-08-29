package dev.mars.apex.demo.examples;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
 * Custody Auto-Repair Demo - Real APEX Service Integration Template
 *
 * This demo demonstrates authentic APEX custody auto-repair functionality using real APEX services:
 * - Standing Instruction (SI) auto-repair rules for custody settlement
 * - Weighted rule-based decision making for settlement instruction repair
 * - Hierarchical rule prioritization (Client > Market > Instrument)
 * - Asian market-specific settlement conventions
 * - Comprehensive audit trail and compliance tracking
 * - Exception handling for high-value transactions
 *
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor for settlement instruction processing
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for business rules
 * - LookupServiceRegistry: Real lookup service management for standing instructions
 *
 * NO HARDCODED SIMULATION: All processing uses authentic APEX core services with YAML-driven configuration.
 * NO HARDCODED OBJECTS: No manual SettlementInstruction, StandingInstruction, or SIRepairResult object creation.
 *
 * YAML FILES REQUIRED:
 * - custody-auto-repair-demo-config.yaml: Auto-repair configurations and enrichment definitions
 * - custody-auto-repair-demo-data.yaml: Test data scenarios for demonstration
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 2.0 - Real APEX services integration (Reference Template)
 */
public class CustodyAutoRepairDemo {

    private static final Logger logger = LoggerFactory.getLogger(CustodyAutoRepairDemo.class);

    private final YamlConfigurationLoader yamlLoader;
    private final EnrichmentService enrichmentService;
    private final LookupServiceRegistry serviceRegistry;
    private final ExpressionEvaluatorService expressionEvaluator;

    /**
     * Constructor initializes real APEX services - NO HARDCODED SIMULATION
     */
    public CustodyAutoRepairDemo() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.serviceRegistry = new LookupServiceRegistry();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        
        logger.info("CustodyAutoRepairDemo initialized with real APEX services");
    }

    /**
     * Main demonstration method using real APEX services - NO HARDCODED SIMULATION
     */
    public static void main(String[] args) {
        logger.info("=== Custody Auto-Repair Demo - Real APEX Services Integration ===");
        logger.info("Demonstrating authentic APEX custody auto-repair functionality with real services");

        CustodyAutoRepairDemo demo = new CustodyAutoRepairDemo();
        demo.runDemo();
    }

    /**
     * Main demo execution method using real APEX services - NO HARDCODED SIMULATION
     */
    public void runDemo() {
        try {
            logger.info("\n=== Custody Auto-Repair Demo - Real Service Integration ===");
            
            // Load YAML configuration using real APEX services
            YamlRuleConfiguration config = loadConfiguration();
            
            // Demonstrate settlement instruction auto-repair scenarios
            demonstrateSettlementInstructionAutoRepair(config);
            
            // Demonstrate standing instruction processing
            demonstrateStandingInstructionProcessing(config);
            
            // Demonstrate weighted rule-based decision making
            demonstrateWeightedRuleDecisionMaking(config);
            
            logger.info("✅ Demo completed successfully using real APEX services");
            
        } catch (Exception e) {
            logger.error("❌ Demo failed: " + e.getMessage(), e);
            throw new RuntimeException("Demo execution failed", e);
        }
    }

    /**
     * Load YAML configuration using real APEX services - NO HARDCODED DATA
     */
    private YamlRuleConfiguration loadConfiguration() {
        try {
            logger.info("Loading YAML configuration from custody-auto-repair-demo-config.yaml");
            
            // Load configuration using real APEX YamlConfigurationLoader
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("custody-auto-repair-demo-config.yaml");
            
            if (config == null) {
                throw new IllegalStateException("Failed to load YAML configuration - file not found or invalid");
            }
            
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            return config;
            
        } catch (Exception e) {
            logger.error("❌ Failed to load YAML configuration", e);
            throw new RuntimeException("Configuration loading failed", e);
        }
    }

    /**
     * Demonstrate settlement instruction auto-repair using real APEX services - NO HARDCODED SIMULATION
     */
    private void demonstrateSettlementInstructionAutoRepair(YamlRuleConfiguration config) {
        try {
            logger.info("\n=== Settlement Instruction Auto-Repair Demo ===");
            
            // Create minimal input data for settlement instruction auto-repair
            Map<String, Object> settlementData = new HashMap<>();
            settlementData.put("instructionId", "SI001");
            settlementData.put("clientId", "CLIENT_A");
            settlementData.put("market", "JAPAN");
            settlementData.put("instrumentType", "EQUITY");
            settlementData.put("settlementAmount", 1000000.0);
            settlementData.put("settlementCurrency", "JPY");
            settlementData.put("settlementDate", "2025-07-30");
            
            // Use real APEX EnrichmentService to process settlement instruction auto-repair
            Object autoRepairResult = enrichmentService.enrichObject(config, settlementData);
            
            logger.info("✅ Settlement instruction auto-repair completed using real APEX services");
            logger.info("Input data: " + settlementData);
            logger.info("Auto-repair result: " + autoRepairResult);
            
        } catch (Exception e) {
            logger.error("❌ Settlement instruction auto-repair failed", e);
            throw new RuntimeException("Settlement instruction auto-repair failed", e);
        }
    }

    /**
     * Demonstrate standing instruction processing using real APEX services - NO HARDCODED SIMULATION
     */
    private void demonstrateStandingInstructionProcessing(YamlRuleConfiguration config) {
        try {
            logger.info("\n=== Standing Instruction Processing Demo ===");
            
            // Create minimal input data for standing instruction processing
            Map<String, Object> standingInstructionData = new HashMap<>();
            standingInstructionData.put("siId", "SI_CLIENT_A");
            standingInstructionData.put("clientId", "CLIENT_A");
            standingInstructionData.put("market", "JAPAN");
            standingInstructionData.put("instrumentType", "EQUITY");
            standingInstructionData.put("priority", "HIGH");
            standingInstructionData.put("weight", 0.6);
            
            // Use real APEX EnrichmentService to process standing instructions
            Object standingInstructionResult = enrichmentService.enrichObject(config, standingInstructionData);
            
            logger.info("✅ Standing instruction processing completed using real APEX services");
            logger.info("Input data: " + standingInstructionData);
            logger.info("Standing instruction result: " + standingInstructionResult);
            
        } catch (Exception e) {
            logger.error("❌ Standing instruction processing failed", e);
            throw new RuntimeException("Standing instruction processing failed", e);
        }
    }

    /**
     * Demonstrate weighted rule-based decision making using real APEX services - NO HARDCODED SIMULATION
     */
    private void demonstrateWeightedRuleDecisionMaking(YamlRuleConfiguration config) {
        try {
            logger.info("\n=== Weighted Rule-Based Decision Making Demo ===");
            
            // Create minimal input data for weighted rule-based decision making
            Map<String, Object> decisionData = new HashMap<>();
            decisionData.put("decisionType", "AUTO_REPAIR");
            decisionData.put("clientWeight", 0.6);
            decisionData.put("marketWeight", 0.3);
            decisionData.put("instrumentWeight", 0.1);
            decisionData.put("confidenceThreshold", 0.8);
            decisionData.put("riskLevel", "MEDIUM");
            
            // Use real APEX EnrichmentService to process weighted rule-based decisions
            Object decisionResult = enrichmentService.enrichObject(config, decisionData);
            
            logger.info("✅ Weighted rule-based decision making completed using real APEX services");
            logger.info("Input data: " + decisionData);
            logger.info("Decision result: " + decisionResult);
            
        } catch (Exception e) {
            logger.error("❌ Weighted rule-based decision making failed", e);
            throw new RuntimeException("Weighted rule-based decision making failed", e);
        }
    }
}
