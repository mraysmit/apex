package dev.mars.apex.demo.infrastructure;

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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Utility class for loading externalized demo data from YAML/JSON files.
 * 
 * This class provides a centralized way to load demo data with proper error handling,
 * fallback mechanisms, and validation. It supports both YAML and JSON formats and
 * provides type-safe data access methods.
 * 
 * Key Features:
 * - YAML and JSON file loading
 * - Fallback to default data if files are missing
 * - Type-safe data access methods
 * - Comprehensive error handling and logging
 * - Caching for performance optimization
 * - Schema validation support
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class DemoDataLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(DemoDataLoader.class);
    
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    
    // Cache for loaded data to improve performance
    private static final Map<String, Object> dataCache = new HashMap<>();
    
    /**
     * Load demo data from a YAML file with fallback to default data.
     * 
     * @param resourcePath Path to the YAML file in the classpath
     * @param defaultDataSupplier Supplier for default data if file loading fails
     * @param dataType Class type for the expected data structure
     * @return Loaded data or default data if loading fails
     */
    @SuppressWarnings("unchecked")
    public static <T> T loadYamlData(String resourcePath, java.util.function.Supplier<T> defaultDataSupplier, Class<T> dataType) {
        String cacheKey = "yaml:" + resourcePath;
        
        // Check cache first
        if (dataCache.containsKey(cacheKey)) {
            logger.debug("Returning cached data for: {}", resourcePath);
            return (T) dataCache.get(cacheKey);
        }
        
        try {
            logger.info("Loading YAML data from: {}", resourcePath);
            InputStream inputStream = DemoDataLoader.class.getClassLoader().getResourceAsStream(resourcePath);
            
            if (inputStream == null) {
                logger.warn("YAML file not found: {}, using default data", resourcePath);
                T defaultData = defaultDataSupplier.get();
                dataCache.put(cacheKey, defaultData);
                return defaultData;
            }
            
            T data = yamlMapper.readValue(inputStream, dataType);
            dataCache.put(cacheKey, data);
            logger.info("Successfully loaded YAML data from: {}", resourcePath);
            return data;
            
        } catch (IOException e) {
            logger.error("Failed to load YAML data from: {}, using default data. Error: {}", resourcePath, e.getMessage());
            T defaultData = defaultDataSupplier.get();
            dataCache.put(cacheKey, defaultData);
            return defaultData;
        }
    }
    
    /**
     * Load demo data from a JSON file with fallback to default data.
     * 
     * @param resourcePath Path to the JSON file in the classpath
     * @param defaultDataSupplier Supplier for default data if file loading fails
     * @param dataType Class type for the expected data structure
     * @return Loaded data or default data if loading fails
     */
    @SuppressWarnings("unchecked")
    public static <T> T loadJsonData(String resourcePath, java.util.function.Supplier<T> defaultDataSupplier, Class<T> dataType) {
        String cacheKey = "json:" + resourcePath;
        
        // Check cache first
        if (dataCache.containsKey(cacheKey)) {
            logger.debug("Returning cached data for: {}", resourcePath);
            return (T) dataCache.get(cacheKey);
        }
        
        try {
            logger.info("Loading JSON data from: {}", resourcePath);
            InputStream inputStream = DemoDataLoader.class.getClassLoader().getResourceAsStream(resourcePath);
            
            if (inputStream == null) {
                logger.warn("JSON file not found: {}, using default data", resourcePath);
                T defaultData = defaultDataSupplier.get();
                dataCache.put(cacheKey, defaultData);
                return defaultData;
            }
            
            T data = jsonMapper.readValue(inputStream, dataType);
            dataCache.put(cacheKey, data);
            logger.info("Successfully loaded JSON data from: {}", resourcePath);
            return data;
            
        } catch (IOException e) {
            logger.error("Failed to load JSON data from: {}, using default data. Error: {}", resourcePath, e.getMessage());
            T defaultData = defaultDataSupplier.get();
            dataCache.put(cacheKey, defaultData);
            return defaultData;
        }
    }
    
    /**
     * Load a generic Map from YAML file.
     * 
     * @param resourcePath Path to the YAML file
     * @param defaultDataSupplier Supplier for default data
     * @return Loaded data as Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadYamlMap(String resourcePath, java.util.function.Supplier<Map<String, Object>> defaultDataSupplier) {
        return DemoDataLoader.<Map<String, Object>>loadYamlData(resourcePath, defaultDataSupplier, (Class<Map<String, Object>>) (Class<?>) Map.class);
    }
    
    /**
     * Load a generic List from YAML file.
     * 
     * @param resourcePath Path to the YAML file
     * @param defaultDataSupplier Supplier for default data
     * @return Loaded data as List
     */
    @SuppressWarnings("unchecked")
    public static List<Object> loadYamlList(String resourcePath, java.util.function.Supplier<List<Object>> defaultDataSupplier) {
        return DemoDataLoader.<List<Object>>loadYamlData(resourcePath, defaultDataSupplier, (Class<List<Object>>) (Class<?>) List.class);
    }
    
    /**
     * Load configuration data from YAML file.
     * 
     * @param resourcePath Path to the configuration YAML file
     * @return Configuration data as Map
     */
    public static Map<String, Object> loadConfiguration(String resourcePath) {
        return loadYamlMap(resourcePath, () -> {
            logger.warn("Using default configuration due to missing file: {}", resourcePath);
            Map<String, Object> defaultConfig = new HashMap<>();
            defaultConfig.put("demo.fallback", true);
            defaultConfig.put("demo.source", "default");
            return defaultConfig;
        });
    }
    
    /**
     * Load customer data from external file.
     * 
     * @param resourcePath Path to the customer data file
     * @return List of customer data
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> loadCustomerData(String resourcePath) {
        return (List<Map<String, Object>>) loadYamlData(resourcePath, DemoDataLoader::createDefaultCustomerData, List.class);
    }
    
    /**
     * Load financial trade data from external file.
     * 
     * @param resourcePath Path to the trade data file
     * @return List of trade data
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> loadTradeData(String resourcePath) {
        return (List<Map<String, Object>>) loadYamlData(resourcePath, DemoDataLoader::createDefaultTradeData, List.class);
    }
    
    /**
     * Clear the data cache (useful for testing or reloading data).
     */
    public static void clearCache() {
        dataCache.clear();
        logger.info("Demo data cache cleared");
    }
    
    /**
     * Check if a resource file exists in the classpath.
     * 
     * @param resourcePath Path to check
     * @return true if the resource exists
     */
    public static boolean resourceExists(String resourcePath) {
        InputStream inputStream = DemoDataLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.debug("Error closing input stream for resource check: {}", e.getMessage());
            }
            return true;
        }
        return false;
    }
    
    /**
     * Create default customer data when external file is not available.
     * 
     * @return Default customer data
     */
    private static List<Map<String, Object>> createDefaultCustomerData() {
        List<Map<String, Object>> customers = new ArrayList<>();
        
        Map<String, Object> customer1 = new HashMap<>();
        customer1.put("customerId", "CUST001");
        customer1.put("firstName", "John");
        customer1.put("lastName", "Smith");
        customer1.put("email", "john.smith@example.com");
        customer1.put("age", 35);
        customer1.put("country", "US");
        customer1.put("accountBalance", 15000.50);
        customer1.put("status", "ACTIVE");
        customer1.put("tier", "GOLD");
        customers.add(customer1);
        
        Map<String, Object> customer2 = new HashMap<>();
        customer2.put("customerId", "CUST002");
        customer2.put("firstName", "Sarah");
        customer2.put("lastName", "Johnson");
        customer2.put("email", "sarah.johnson@example.com");
        customer2.put("age", 28);
        customer2.put("country", "GB");
        customer2.put("accountBalance", 8750.25);
        customer2.put("status", "ACTIVE");
        customer2.put("tier", "SILVER");
        customers.add(customer2);
        
        Map<String, Object> customer3 = new HashMap<>();
        customer3.put("customerId", "CUST003");
        customer3.put("firstName", "Invalid");
        customer3.put("lastName", "Customer");
        customer3.put("email", "invalid-email");
        customer3.put("age", 16);
        customer3.put("country", "XX");
        customer3.put("accountBalance", -100.0);
        customer3.put("status", "INACTIVE");
        customer3.put("tier", "BASIC");
        customers.add(customer3);
        
        return customers;
    }
    
    /**
     * Create default trade data when external file is not available.
     * 
     * @return Default trade data
     */
    private static List<Map<String, Object>> createDefaultTradeData() {
        List<Map<String, Object>> trades = new ArrayList<>();
        
        Map<String, Object> trade1 = new HashMap<>();
        trade1.put("tradeId", "TRD001");
        trade1.put("amount", 1500000.0);
        trade1.put("currency", "USD");
        trade1.put("counterparty", "Goldman Sachs");
        trade1.put("instrumentType", "BOND");
        trade1.put("status", "NEW");
        trades.add(trade1);
        
        Map<String, Object> trade2 = new HashMap<>();
        trade2.put("tradeId", "TRD002");
        trade2.put("amount", 5000000.0);
        trade2.put("currency", "EUR");
        trade2.put("counterparty", "Deutsche Bank");
        trade2.put("instrumentType", "EQUITY");
        trade2.put("status", "PENDING");
        trades.add(trade2);
        
        return trades;
    }
}
