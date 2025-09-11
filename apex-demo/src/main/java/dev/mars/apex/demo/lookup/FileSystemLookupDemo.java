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

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * File System Lookup Demo.
 *
 * Demonstrates the new file-system dataset type functionality for loading data
 * from JSON and XML files and performing enrichment using YAML-configured lookup rules.
 *
 * ============================================================================
 * NEW FUNCTIONALITY DEMONSTRATED:
 * - File-system dataset type for JSON and XML file loading
 * - Real APEX enrichment services with file-based data sources
 * - YAML-only configuration with no hardcoded lookup logic
 * - Interactive demo showing both JSON and XML file lookup examples
 * ============================================================================
 *
 * CRITICAL: This demo eliminates ALL hardcoded lookup logic and uses:
 * - Real APEX YAML configuration for file-system data source definitions
 * - Real APEX enrichment services for file processing and data retrieval
 * - Real APEX lookup services for field mapping and enrichment
 */
public class FileSystemLookupDemo {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemLookupDemo.class);

    private YamlConfigurationLoader yamlLoader;
    private EnrichmentService enrichmentService;
    private LookupServiceRegistry lookupRegistry;
    private ExpressionEvaluatorService expressionEvaluator;

    public FileSystemLookupDemo() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.lookupRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(lookupRegistry, expressionEvaluator);
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("                    APEX FILE SYSTEM LOOKUP DEMO");
        System.out.println("================================================================================");
        System.out.println("This demo showcases the new file-system dataset type functionality:");
        System.out.println("- Load data from JSON and XML files");
        System.out.println("- Perform enrichment using YAML-configured lookup rules");
        System.out.println("- Use real APEX services with no hardcoded simulation logic");
        System.out.println("================================================================================");
        System.out.println();

        FileSystemLookupDemo demo = new FileSystemLookupDemo();
        demo.runInteractiveDemo();
    }

    /**
     * Run interactive demo with user choices.
     */
    public void runInteractiveDemo() {
        Scanner scanner = new Scanner(System.in);
        boolean continueDemo = true;

        while (continueDemo) {
            displayMenu();
            String choice = scanner.nextLine().trim().toLowerCase();

            switch (choice) {
                case "1":
                case "json":
                    runJsonFileLookupDemo();
                    break;
                case "2":
                case "xml":
                    runXmlFileLookupDemo();
                    break;
                case "3":
                case "both":
                    runJsonFileLookupDemo();
                    System.out.println();
                    runXmlFileLookupDemo();
                    break;
                case "4":
                case "info":
                    displayTechnicalInfo();
                    break;
                case "5":
                case "q":
                case "quit":
                case "exit":
                    continueDemo = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }

            if (continueDemo) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }

        System.out.println("\n================================================================================");
        System.out.println("                    DEMO COMPLETED - THANK YOU!");
        System.out.println("================================================================================");
        scanner.close();
    }

    /**
     * Display the demo menu.
     */
    private void displayMenu() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("                    DEMO MENU");
        System.out.println("=".repeat(60));
        System.out.println("1. JSON File Lookup Demo");
        System.out.println("2. XML File Lookup Demo");
        System.out.println("3. Run Both Demos");
        System.out.println("4. Technical Information");
        System.out.println("5. Quit");
        System.out.println("=".repeat(60));
        System.out.print("Enter your choice (1-5): ");
    }

    /**
     * Run JSON file lookup demonstration.
     */
    public void runJsonFileLookupDemo() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                    JSON FILE LOOKUP DEMO");
        System.out.println("=".repeat(80));
        System.out.println("Configuration: lookup/json-file-lookup.yaml");
        System.out.println("Data Source: demo-data/json/products.json");
        System.out.println("Dataset Type: file-system");
        System.out.println("=".repeat(80));

        try {
            // Create input data with product ID
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("productId", "PROD001");

            System.out.println("\nInput Data:");
            System.out.println("  Product ID: " + inputData.get("productId"));

            // Load configuration and perform enrichment
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("lookup/json-file-lookup.yaml");
            Map<String, Object> enrichedResult = new HashMap<>(inputData);
            
            // Apply enrichment using APEX services
            enrichmentService.enrichObject(config, enrichedResult);

            // Display results
            System.out.println("\nEnrichment Results:");
            System.out.println("  Product Name: " + enrichedResult.get("productName"));
            System.out.println("  Product Price: " + enrichedResult.get("productPrice"));
            System.out.println("  Product Category: " + enrichedResult.get("productCategory"));
            System.out.println("  Product Available: " + enrichedResult.get("productAvailable"));

            // Validate results
            if (enrichedResult.get("productName") != null) {
                System.out.println("\n✅ JSON file lookup successful!");
            } else {
                System.out.println("\n⚠️ JSON file lookup returned no results");
            }
            
        } catch (Exception e) {
            System.err.println("\n❌ JSON file lookup demo failed: " + e.getMessage());
            logger.error("JSON file lookup demo failed", e);
        }
    }

    /**
     * Run XML file lookup demonstration.
     */
    public void runXmlFileLookupDemo() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                    XML FILE LOOKUP DEMO");
        System.out.println("=".repeat(80));
        System.out.println("Configuration: lookup/xml-file-lookup.yaml");
        System.out.println("Data Source: demo-data/xml/products.xml");
        System.out.println("Dataset Type: file-system");
        System.out.println("=".repeat(80));

        try {
            // Create input data with product ID
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("productId", "PROD002");

            System.out.println("\nInput Data:");
            System.out.println("  Product ID: " + inputData.get("productId"));

            // Load configuration and perform enrichment
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("lookup/xml-file-lookup.yaml");
            Map<String, Object> enrichedResult = new HashMap<>(inputData);
            
            // Apply enrichment using APEX services
            enrichmentService.enrichObject(config, enrichedResult);

            // Display results
            System.out.println("\nEnrichment Results:");
            System.out.println("  Product Name: " + enrichedResult.get("productName"));
            System.out.println("  Product Price: " + enrichedResult.get("productPrice"));
            System.out.println("  Product Category: " + enrichedResult.get("productCategory"));
            System.out.println("  Product Available: " + enrichedResult.get("productAvailable"));

            // Validate results
            if (enrichedResult.get("productName") != null) {
                System.out.println("\n✅ XML file lookup successful!");
            } else {
                System.out.println("\n⚠️ XML file lookup returned no results (XML parsing may need configuration)");
            }
            
        } catch (Exception e) {
            System.err.println("\n❌ XML file lookup demo failed: " + e.getMessage());
            logger.error("XML file lookup demo failed", e);
        }
    }

    /**
     * Display technical information about the file-system dataset functionality.
     */
    private void displayTechnicalInfo() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                    TECHNICAL INFORMATION");
        System.out.println("=".repeat(80));
        System.out.println("NEW FILE-SYSTEM DATASET TYPE:");
        System.out.println("- Type: 'file-system' in YAML lookup-dataset configuration");
        System.out.println("- Supports: JSON and XML file loading");
        System.out.println("- Uses: Existing JsonDataLoader and XmlDataLoader from apex-core");
        System.out.println("- Configuration: file-path property specifies the data file location");
        System.out.println();
        System.out.println("APEX SERVICES USED:");
        System.out.println("- YamlConfigurationLoader: Real YAML configuration loading and validation");
        System.out.println("- EnrichmentService: Real APEX enrichment processor");
        System.out.println("- LookupServiceRegistry: Real lookup service integration");
        System.out.println("- ExpressionEvaluatorService: Real SpEL expression evaluation");
        System.out.println();
        System.out.println("IMPLEMENTATION DETAILS:");
        System.out.println("- DatasetLookupServiceFactory: Extended to support file-system type");
        System.out.println("- YamlConfigurationLoader: Updated validation to accept file-system type");
        System.out.println("- No hardcoded simulation logic - uses real APEX services throughout");
        System.out.println("- Backward compatible - all existing functionality remains intact");
        System.out.println("=".repeat(80));
    }
}
