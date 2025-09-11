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

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * APEX-Compliant Compound Key Lookup Demo.
 *
 * Demonstrates compound key lookup using string concatenation with real APEX services.
 * This example shows how to combine multiple field values to create a compound lookup key
 * for customer-region specific pricing and tier information.
 *
 * ============================================================================
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor for compound key lookup
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for compound keys
 * - LookupServiceRegistry: Real lookup service integration for dataset lookups
 * ============================================================================
 *
 * CRITICAL: This class eliminates ALL hardcoded lookup logic and uses:
 * - YAML-driven lookup configuration from external files
 * - Real APEX enrichment services for all compound key lookups
 * - Fail-fast error handling (no hardcoded fallbacks)
 *
 * Pattern Demonstrated: lookup-key: "#customerId + '-' + #region"
 * Use Case: Customer-region specific pricing and tier information lookup
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-22
 * @version 1.0
 */
public class CompoundKeyLookupDemo {

    private static final Logger logger = LoggerFactory.getLogger(CompoundKeyLookupDemo.class);

    private final EnrichmentService enrichmentService;
    private final Map<String, Object> configurationData;

    /**
     * Constructor initializes real APEX services.
     */
    public CompoundKeyLookupDemo() {
        logger.info("Starting APEX-compliant compound key lookup demonstration...");

        // Initialize real APEX services
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);

        logger.info("CompoundKeyLookupDemo initialized with real APEX services");

        // Load external YAML configurations
        this.configurationData = loadExternalConfiguration();
        logger.info("External compound key lookup YAML loaded successfully");
    }

    /**
     * Load external YAML configuration files.
     */
    private Map<String, Object> loadExternalConfiguration() {
        try {
            logger.info("Loading external compound key lookup YAML...");

            Map<String, Object> configs = new HashMap<>();
            YamlConfigurationLoader loader = new YamlConfigurationLoader();

            // Load main configuration
            YamlRuleConfiguration mainConfig = loader.loadFromClasspath("lookup/compound-key-lookup.yaml");
            configs.put("mainConfig", mainConfig);

            return configs;

        } catch (Exception e) {
            logger.warn("External compound key lookup YAML files not found, APEX enrichment will use fail-fast approach: {}", e.getMessage());
            throw new RuntimeException("Required compound key lookup configuration YAML files not found", e);
        }
    }

    public static void main(String[] args) {
        try {
            CompoundKeyLookupDemo demo = new CompoundKeyLookupDemo();
            demo.runDemo();
        } catch (Exception e) {
            logger.error("Demo failed: {}", e.getMessage(), e);
            System.err.println("Demonstration failed: " + e.getMessage());
        }
    }

    /**
     * Run the comprehensive compound key lookup demonstration.
     */
    public void runDemo() {
        System.out.println("=================================================================");
        System.out.println("APEX COMPOUND KEY LOOKUP DEMONSTRATION");
        System.out.println("=================================================================");
        System.out.println("Demo Purpose: Customer-region pricing enrichment with real APEX services");
        System.out.println("Processing Methods: Real APEX Enrichment + YAML Configurations");
        System.out.println("Lookup Pattern: Compound key (#customerId + '-' + #region)");
        System.out.println("Data Sources: Real APEX Services + External YAML Files");
        System.out.println("=================================================================");
        System.out.println();

        // Process customer orders
        processCustomerOrders();

        System.out.println("=================================================================");
        System.out.println("COMPOUND KEY LOOKUP DEMONSTRATION COMPLETED SUCCESSFULLY");
        System.out.println("=================================================================");
        System.out.println("All customer orders processed using real APEX services");
        System.out.println("Total processing: 10+ compound key lookup operations");
        System.out.println("Configuration: YAML files with comprehensive lookup definitions");
        System.out.println("Integration: 100% real APEX enrichment services");
        System.out.println("=================================================================");

        logger.info("APEX-compliant compound key lookup demonstration completed successfully");
    }

    /**
     * Process customer orders using real APEX enrichment.
     */
    private void processCustomerOrders() {
        System.out.println("----- CUSTOMER ORDER PROCESSING (Real APEX Enrichment) -----");

        // Generate test orders
        List<Map<String, Object>> orders = generateTestOrders();

        for (Map<String, Object> order : orders) {
            String compoundKey = order.get("customerId") + "-" + order.get("region");
            System.out.printf("Processing order %s with compound key %s using real APEX enrichment...\n",
                    order.get("orderId"), compoundKey);

            Map<String, Object> result = processCustomerOrder(order);

            System.out.printf("  Customer Tier: %s\n", result.get("customerTier"));
            System.out.printf("  Regional Discount: %s\n", result.get("regionalDiscount"));
            System.out.printf("  Special Pricing: %s\n", result.get("specialPricing"));
            System.out.printf("  Customer Name: %s\n", result.get("customerName"));
        }
    }

    /**
     * Process a single customer order using real APEX enrichment.
     */
    public Map<String, Object> processCustomerOrder(Map<String, Object> orderData) {
        try {
            logger.info("Processing customer order '{}' using real APEX enrichment...", orderData.get("orderId"));

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main compound key lookup configuration not found");
            }

            // Use real APEX enrichment service for compound key lookup
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, orderData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Customer order '{}' processed successfully using real APEX enrichment", orderData.get("orderId"));
            return result;

        } catch (Exception e) {
            logger.error("Failed to process customer order '{}' with APEX enrichment: {}", orderData.get("orderId"), e.getMessage());
            throw new RuntimeException("Customer order processing failed: " + orderData.get("orderId"), e);
        }
    }

    /**
     * Generate test customer orders for demonstration.
     */
    private List<Map<String, Object>> generateTestOrders() {
        List<Map<String, Object>> orders = new ArrayList<>();

        // Create diverse order data with different customer-region combinations
        orders.add(createOrder("ORD-001", "CUST001", "NA", "1250.00", "Software License"));
        orders.add(createOrder("ORD-002", "CUST002", "NA", "850.75", "Consulting Services"));
        orders.add(createOrder("ORD-003", "CUST003", "EU", "425.50", "Hardware Purchase"));
        orders.add(createOrder("ORD-004", "CUST004", "APAC", "2100.00", "Training Program"));
        orders.add(createOrder("ORD-005", "CUST001", "EU", "750.25", "Support Contract"));

        return orders;
    }

    /**
     * Create an order map for testing.
     */
    private Map<String, Object> createOrder(String orderId, String customerId, String region, String amount, String description) {
        Map<String, Object> order = new HashMap<>();
        order.put("orderId", orderId);
        order.put("customerId", customerId);
        order.put("region", region);
        order.put("orderAmount", new BigDecimal(amount));
        order.put("description", description);
        order.put("orderDate", LocalDateTime.now().minusDays((int) (Math.random() * 30)));
        order.put("status", "PENDING");
        return order;
    }
}
