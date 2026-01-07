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
    
    private static final String EXAMPLES_DIR = "examples";

    /**
     * Get all available example categories and their configurations.
     */
    public Map<String, Object> getAllExamples() {
        Map<String, Object> examples = new LinkedHashMap<>();
        
        try {
            // Load examples from the examples directory and its subdirectories
            java.io.File examplesDir = new java.io.File(EXAMPLES_DIR);
            if (examplesDir.exists() && examplesDir.isDirectory()) {
                // Scan subdirectories
                java.io.File[] subDirs = examplesDir.listFiles(java.io.File::isDirectory);
                if (subDirs != null) {
                    for (java.io.File subDir : subDirs) {
                        List<Map<String, Object>> categoryExamples = getExamplesFromDir(subDir);
                        if (!categoryExamples.isEmpty()) {
                            examples.put(subDir.getName(), categoryExamples);
                        }
                    }
                }
                
                // Scan root directory for uncategorized examples
                List<Map<String, Object>> rootExamples = getExamplesFromDir(examplesDir);
                if (!rootExamples.isEmpty()) {
                    examples.put("uncategorized", rootExamples);
                }
            } else {
                examples.put("message", "No examples found in " + examplesDir.getAbsolutePath());
            }
            
            logger.info("Loaded examples from {}", EXAMPLES_DIR);
            
        } catch (Exception e) {
            logger.error("Error loading examples: {}", e.getMessage());
            logger.debug("Full exception details:", e);
            examples.put("error", "Failed to load examples: " + e.getMessage());
        }
        
        return examples;
    }
    
    /**
     * Get a specific example by category and name.
     */
    public Map<String, Object> getExample(String category, String name) {
        try {
            String yamlContent = loadExampleFile(category, name + ".yaml");
            
            String displayName = name.replace("-", " ");
            displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);

            Map<String, Object> example = new HashMap<>();
            example.put("name", displayName);
            example.put("category", category);
            example.put("yaml", yamlContent);
            example.put("sampleData", getSampleDataForExample(category, name));
            
            return example;
            
        } catch (Exception e) {
            logger.error("Error loading example {}/{}: {}", category, name, e.getMessage());
            logger.debug("Full exception details:", e);
            Map<String, Object> errorExample = new HashMap<>();
            errorExample.put("error", "Failed to load example: " + e.getMessage());
            return errorExample;
        }
    }

    /**
     * Save YAML content for a specific example.
     */
    public void saveExampleYaml(String category, String name, String content) throws IOException {
        java.io.File file;
        if ("uncategorized".equals(category)) {
            file = new java.io.File(EXAMPLES_DIR, name + ".yaml");
        } else {
            file = new java.io.File(new java.io.File(EXAMPLES_DIR, category), name + ".yaml");
        }
        
        // Ensure parent directory exists
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        
        java.nio.file.Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        logger.info("Saved YAML example to {}", file.getAbsolutePath());
    }

    /**
     * Save data content for a specific example.
     */
    public void saveExampleData(String category, String name, String content) throws IOException {
        java.io.File file;
        if ("uncategorized".equals(category)) {
            file = new java.io.File(EXAMPLES_DIR, name + ".json");
        } else {
            file = new java.io.File(new java.io.File(EXAMPLES_DIR, category), name + ".json");
        }
        
        // Ensure parent directory exists
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        
        java.nio.file.Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        logger.info("Saved data example to {}", file.getAbsolutePath());
    }

    private List<Map<String, Object>> getExamplesFromDir(java.io.File dir) {
        List<Map<String, Object>> examples = new ArrayList<>();
        
        if (dir.exists() && dir.isDirectory()) {
            java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));
            if (files != null) {
                for (java.io.File file : files) {
                    String name = file.getName();
                    String displayName = name.replace(".yaml", "").replace(".yml", "").replace("-", " ");
                    displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
                    
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", name.replace(".yaml", "").replace(".yml", ""));
                    info.put("name", displayName);
                    info.put("description", "Loaded from " + name);
                    info.put("available", true);
                    info.put("size", file.length());
                    
                    examples.add(info);
                }
            }
        }
        return examples;
    }

    private String loadExampleFile(String category, String fileName) throws IOException {
        java.io.File file;
        if ("uncategorized".equals(category)) {
            file = new java.io.File(EXAMPLES_DIR, fileName);
        } else {
            file = new java.io.File(new java.io.File(EXAMPLES_DIR, category), fileName);
        }
        
        if (file.exists()) {
            return java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
        }
        throw new IOException("File not found: " + file.getAbsolutePath());
    }


    private Map<String, Object> getSampleDataForExample(String category, String name) {
        // Try to load corresponding JSON file
        try {
            String jsonContent = loadExampleFile(category, name + ".json");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = new com.fasterxml.jackson.databind.ObjectMapper().readValue(jsonContent, Map.class);
            return data;
        } catch (Exception e) {
            logger.warn("Could not load JSON data for example {}/{}: {}", category, name, e.getMessage());
            
            // Fallback to hardcoded logic if JSON file not found
            if (name.contains("financial")) {
                return createFinancialSampleData();
            } else if (name.contains("quick-start")) {
                Map<String, Object> data = new HashMap<>();
                data.put("amount", 100.00);
                data.put("currency", "USD");
                return data;
            }
            
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
