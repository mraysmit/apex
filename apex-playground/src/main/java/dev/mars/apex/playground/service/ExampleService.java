package dev.mars.apex.playground.service;

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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service for loading example YAML configurations and sample data from the apex-demo module.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
@Service
public class ExampleService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExampleService.class);
    
    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
    
    /**
     * Get all available example categories and their configurations.
     */
    public Map<String, Object> getAllExamples() {
        Map<String, Object> examples = new LinkedHashMap<>();
        
        try {
            // Load examples from different categories
            examples.put("quickstart", getQuickStartExamples());
            examples.put("financial", getFinancialExamples());
            examples.put("validation", getValidationExamples());
            examples.put("lookup", getLookupExamples());
            examples.put("advanced", getAdvancedExamples());
            
            logger.info("Loaded {} example categories", examples.size());
            
        } catch (Exception e) {
            logger.error("Error loading examples", e);
            examples.put("error", "Failed to load examples: " + e.getMessage());
        }
        
        return examples;
    }
    
    /**
     * Get a specific example by category and name.
     */
    public Map<String, Object> getExample(String category, String name) {
        try {
            String resourcePath = getResourcePath(category, name);
            String yamlContent = loadResourceContent(resourcePath);
            
            Map<String, Object> example = new HashMap<>();
            example.put("name", name);
            example.put("category", category);
            example.put("yaml", yamlContent);
            example.put("sampleData", getSampleDataForExample(category, name));
            
            return example;
            
        } catch (Exception e) {
            logger.error("Error loading example {}/{}", category, name, e);
            Map<String, Object> errorExample = new HashMap<>();
            errorExample.put("error", "Failed to load example: " + e.getMessage());
            return errorExample;
        }
    }
    
    private List<Map<String, Object>> getQuickStartExamples() {
        List<Map<String, Object>> examples = new ArrayList<>();

        examples.add(createExampleInfo(
            "quick-start",
            "Quick Start Demo",
            "Simple validation rules for getting started",
            "demo-rules/quick-start.yaml"
        ));

        examples.add(createExampleInfo(
            "file-processing",
            "File Processing Configuration",
            "YAML configuration for file processing",
            "yaml-examples/file-processing-config.yaml"
        ));

        return examples;
    }
    
    private List<Map<String, Object>> getFinancialExamples() {
        List<Map<String, Object>> examples = new ArrayList<>();

        examples.add(createExampleInfo(
            "financial-validation",
            "Financial Validation Rules",
            "Financial trade validation and enrichment",
            "demo-rules/financial-validation.yaml"
        ));

        examples.add(createExampleInfo(
            "financial-enrichment",
            "Financial Enrichment Rules",
            "Comprehensive financial enrichment processing",
            "config/financial-enrichment-rules.yaml"
        ));

        examples.add(createExampleInfo(
            "settlement-validation",
            "Settlement Validation Rules",
            "Settlement validation and processing rules",
            "config/settlement-validation-rules.yaml"
        ));

        return examples;
    }
    
    private List<Map<String, Object>> getValidationExamples() {
        List<Map<String, Object>> examples = new ArrayList<>();

        examples.add(createExampleInfo(
            "custody-auto-repair",
            "Custody Auto Repair Rules",
            "Comprehensive custody auto repair validation",
            "demo-rules/custody-auto-repair-rules.yaml"
        ));

        examples.add(createExampleInfo(
            "derivatives-validation",
            "Derivatives Validation Rules",
            "Complex derivatives validation rules",
            "config/derivatives-validation-rules.yaml"
        ));

        return examples;
    }
    
    private List<Map<String, Object>> getLookupExamples() {
        List<Map<String, Object>> examples = new ArrayList<>();

        examples.add(createExampleInfo(
            "comprehensive-lookup",
            "Comprehensive Lookup Demo",
            "Advanced lookup patterns and enrichment",
            "demo-configs/comprehensive-lookup-demo.yaml"
        ));

        examples.add(createExampleInfo(
            "compound-key-lookup",
            "Compound Key Lookup",
            "Advanced lookup with compound keys",
            "examples/lookups/compound-key-lookup.yaml"
        ));

        return examples;
    }
    
    private List<Map<String, Object>> getAdvancedExamples() {
        List<Map<String, Object>> examples = new ArrayList<>();

        examples.add(createExampleInfo(
            "batch-processing",
            "Batch Processing Demo",
            "Batch data processing with rules",
            "batch-processing.yaml"
        ));

        examples.add(createExampleInfo(
            "dataset-enrichment",
            "Dataset Enrichment Rules",
            "Advanced dataset enrichment patterns",
            "demo-rules/dataset-enrichment.yaml"
        ));

        examples.add(createExampleInfo(
            "rule-chains",
            "Rule Chains Patterns",
            "Complex rule chaining patterns",
            "demo-rules/rule-chains-patterns.yaml"
        ));

        return examples;
    }
    
    private Map<String, Object> createExampleInfo(String id, String name, String description, String resourcePath) {
        Map<String, Object> example = new HashMap<>();
        example.put("id", id);
        example.put("name", name);
        example.put("description", description);
        example.put("resourcePath", resourcePath);
        
        // Try to load the actual content to verify it exists
        try {
            String content = loadResourceContent(resourcePath);
            example.put("available", true);
            example.put("size", content.length());
        } catch (Exception e) {
            example.put("available", false);
            example.put("error", e.getMessage());
            logger.warn("Example {} not available at {}: {}", id, resourcePath, e.getMessage());
        }
        
        return example;
    }
    
    private String getResourcePath(String category, String name) {
        // Map category/name combinations to actual resource paths
        // Use the same paths as defined in the example creation methods
        switch (category) {
            case "quickstart":
                switch (name) {
                    case "quick-start":
                        return "demo-rules/quick-start.yaml";
                    case "file-processing":
                        return "yaml-examples/file-processing-config.yaml";
                    case "basic-validation":
                        return "demo-rules/basic-validation.yaml";
                    default:
                        return "demo-rules/" + name + ".yaml";
                }
            case "financial":
                switch (name) {
                    case "financial-validation":
                        return "demo-rules/financial-validation.yaml";
                    case "financial-enrichment":
                        return "config/financial-enrichment-rules.yaml";
                    case "settlement-validation":
                        return "config/settlement-validation-rules.yaml";
                    case "trade-validation":
                        return "config/trade-validation-rules.yaml";
                    default:
                        return "config/" + name + ".yaml";
                }
            case "validation":
                switch (name) {
                    case "custody-auto-repair":
                        return "demo-rules/custody-auto-repair-rules.yaml";
                    case "derivatives-validation":
                        return "config/derivatives-validation-rules.yaml";
                    case "data-validation":
                        return "demo-rules/data-validation-rules.yaml";
                    default:
                        return "demo-rules/" + name + ".yaml";
                }
            case "lookup":
                switch (name) {
                    case "comprehensive-lookup":
                        return "demo-configs/comprehensive-lookup-demo.yaml";
                    case "compound-key-lookup":
                        return "examples/lookups/compound-key-lookup.yaml";
                    case "simple-lookup":
                        return "examples/lookups/simple-lookup.yaml";
                    default:
                        return "examples/lookups/" + name + ".yaml";
                }
            case "advanced":
                switch (name) {
                    case "batch-processing":
                        return "batch-processing.yaml";
                    case "dataset-enrichment":
                        return "demo-rules/dataset-enrichment.yaml";
                    case "rule-chains":
                        return "demo-rules/rule-chains-patterns.yaml";
                    default:
                        return "demo-rules/" + name + ".yaml";
                }
            default:
                // For truly unknown categories, we should return an error
                throw new IllegalArgumentException("Unknown example category: " + category);
        }
    }
    
    private String loadResourceContent(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                logger.warn("Resource not found: {}, providing mock YAML content", resourcePath);
                return generateMockYamlContent(resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String generateMockYamlContent(String resourcePath) {
        // Extract name from path for mock content
        String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        String name = fileName.replace(".yaml", "").replace("-", " ");

        return String.format("""
            # Mock YAML Configuration for %s
            metadata:
              name: "%s"
              version: "1.0.0"
              description: "Mock configuration for playground demonstration"
              type: "rule-config"
              author: "APEX Playground"

            rules:
              - name: "sample-rule"
                condition: "#amount > 0"
                message: "Amount must be positive"
                enabled: true

            enrichments:
              - name: "sample-enrichment"
                type: "lookup-enrichment"
                enabled: true
                lookupConfig:
                  lookupKey: "#currency"
                  lookupDataset:
                    keyField: "code"
                    data:
                      - code: "USD"
                        name: "US Dollar"
                      - code: "EUR"
                        name: "Euro"
            """, name, name);
    }
    
    private Map<String, Object> getSampleDataForExample(String category, String name) {
        // Return appropriate sample data based on the example type
        switch (category) {
            case "financial":
                return createFinancialSampleData();
            case "validation":
                return createValidationSampleData();
            case "lookup":
                return createLookupSampleData();
            default:
                return createDefaultSampleData();
        }
    }
    
    private Map<String, Object> createFinancialSampleData() {
        Map<String, Object> data = new HashMap<>();
        data.put("tradeId", "TRD-001");
        data.put("amount", 150000.00);
        data.put("currency", "USD");
        data.put("counterparty", "BANK-ABC");
        data.put("tradeDate", "2024-08-24");
        data.put("settlementDate", "2024-08-26");
        return data;
    }
    
    private Map<String, Object> createValidationSampleData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John Doe");
        data.put("age", 30);
        data.put("email", "john.doe@example.com");
        data.put("phone", "+1-555-0123");
        data.put("country", "US");
        return data;
    }
    
    private Map<String, Object> createLookupSampleData() {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", "CUST-001");
        data.put("productId", "PROD-123");
        data.put("region", "US-EAST");
        data.put("quantity", 100);
        return data;
    }
    
    private Map<String, Object> createDefaultSampleData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Sample Data");
        data.put("value", 42);
        data.put("active", true);
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }
}
