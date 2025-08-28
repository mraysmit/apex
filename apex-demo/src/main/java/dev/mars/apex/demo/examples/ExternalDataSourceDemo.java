package dev.mars.apex.demo.examples;

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


import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.datasource.HealthCheckConfig;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.DataSourceMetrics;
import dev.mars.apex.core.service.data.external.ConnectionStatus;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.demo.runners.DemoRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive demonstration of external data source capabilities from the External Data Sources Guide.
 * 
 * This demo showcases:
 * - REST API integration with authentication
 * - File system integration (CSV, JSON)
 * - Cache integration with TTL
 * - Health monitoring and metrics
 * - Error handling and resilience
 * - Performance optimization
 * 
 * Based on scenarios from APEX_EXTERNAL_DATA_SOURCES_GUIDE.md
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ExternalDataSourceDemo implements DemoRunner.Demo {

    private static final Logger logger = LoggerFactory.getLogger(ExternalDataSourceDemo.class);

    private DataSourceFactory factory;
    private Path tempDir;

    @Override
    public void run() {
        System.out.println("=".repeat(80));
        System.out.println("APEX EXTERNAL DATA SOURCES DEMONSTRATION");
        System.out.println("=".repeat(80));
        System.out.println();

        try {
            initializeServices();
            createTestData();
            
            demonstrateRestApiIntegration();
            demonstrateCsvFileIntegration();
            demonstrateJsonFileIntegration();
            demonstrateCacheIntegration();
            demonstrateHealthMonitoring();
            demonstrateErrorHandling();
            demonstratePerformanceMetrics();
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
        
        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("EXTERNAL DATA SOURCES DEMONSTRATION COMPLETED");
        System.out.println("=".repeat(80));
    }

    private void initializeServices() throws IOException {
        System.out.println("🔧 Initializing External Data Source Services...");
        factory = DataSourceFactory.getInstance();
        tempDir = Files.createTempDirectory("apex-demo-");
        System.out.println("✅ Services initialized successfully");
        System.out.println("📁 Temporary directory: " + tempDir);
        System.out.println();
    }

    private void createTestData() throws IOException {
        System.out.println("📝 Creating Test Data Files...");
        
        // Create CSV test file
        Path csvFile = tempDir.resolve("users.csv");
        String csvContent = """
            id,name,email,status,department
            1,John Doe,john@example.com,ACTIVE,Engineering
            2,Jane Smith,jane@example.com,ACTIVE,Marketing
            3,Bob Wilson,bob@example.com,INACTIVE,Sales
            4,Alice Brown,alice@example.com,ACTIVE,Engineering
            5,Charlie Davis,charlie@example.com,ACTIVE,Support
            """;
        Files.writeString(csvFile, csvContent);
        System.out.println("✅ Created CSV file: " + csvFile.getFileName());
        
        // Create JSON test file
        Path jsonFile = tempDir.resolve("products.json");
        String jsonContent = """
            {
              "products": [
                {
                  "id": "LAPTOP001",
                  "name": "Business Laptop",
                  "price": 1299.99,
                  "category": "Electronics",
                  "available": true,
                  "specifications": {
                    "processor": "Intel i7",
                    "memory": "16GB",
                    "storage": "512GB SSD"
                  }
                },
                {
                  "id": "MOUSE001",
                  "name": "Wireless Mouse",
                  "price": 29.99,
                  "category": "Accessories",
                  "available": true,
                  "specifications": {
                    "type": "Optical",
                    "connectivity": "Bluetooth",
                    "battery": "AA"
                  }
                },
                {
                  "id": "MONITOR001",
                  "name": "4K Monitor",
                  "price": 399.99,
                  "category": "Electronics",
                  "available": false,
                  "specifications": {
                    "size": "27 inch",
                    "resolution": "3840x2160",
                    "refresh": "60Hz"
                  }
                }
              ],
              "metadata": {
                "total": 3,
                "lastUpdated": "2025-01-30T10:00:00Z"
              }
            }
            """;
        Files.writeString(jsonFile, jsonContent);
        System.out.println("✅ Created JSON file: " + jsonFile.getFileName());
        System.out.println();
    }

    private void demonstrateRestApiIntegration() {
        System.out.println("🌐 REST API INTEGRATION DEMONSTRATION");
        System.out.println("-".repeat(50));
        
        try {
            DataSourceConfiguration config = createRestApiConfiguration();
            ExternalDataSource dataSource = factory.createDataSource(config);
            
            System.out.println("Configuration:");
            System.out.println("  Name: " + config.getName());
            System.out.println("  Type: " + config.getSourceType());
            System.out.println("  Host: " + config.getConnection().getHost());
            System.out.println("  Port: " + config.getConnection().getPort());
            
            // Test connection
            System.out.println("\n🔗 Testing connection...");
            boolean connected = dataSource.testConnection();
            System.out.println("Connection Status: " + (connected ? "✅ CONNECTED" : "❌ FAILED"));
            
            // Check health
            System.out.println("\n🏥 Checking health...");
            boolean healthy = dataSource.isHealthy();
            System.out.println("Health Status: " + (healthy ? "✅ HEALTHY" : "❌ UNHEALTHY"));
            
            if (connected && healthy) {
                // Execute sample query
                System.out.println("\n📊 Executing sample query...");
                Map<String, Object> parameters = Map.of("id", "123");
                try {
                    List<Object> results = dataSource.query("getUserById", parameters);
                    System.out.println("Query Results: " + (results != null ? results.size() + " records" : "null"));
                } catch (DataSourceException e) {
                    System.out.println("Query Result: ⚠️ " + e.getMessage());
                }
            }
            
            dataSource.shutdown();
            System.out.println("✅ REST API integration demonstrated");
            
        } catch (Exception e) {
            System.out.println("❌ REST API demo failed: " + e.getMessage());
        }
        
        System.out.println();
    }

    private void demonstrateCsvFileIntegration() {
        System.out.println("📄 CSV FILE INTEGRATION DEMONSTRATION");
        System.out.println("-".repeat(50));
        
        try {
            Path csvFile = tempDir.resolve("users.csv");
            DataSourceConfiguration config = createCsvFileConfiguration(csvFile.toString());
            ExternalDataSource dataSource = factory.createDataSource(config);
            
            System.out.println("Configuration:");
            System.out.println("  Name: " + config.getName());
            System.out.println("  Type: " + config.getSourceType());
            System.out.println("  File: " + csvFile.getFileName());
            
            // Test connection
            System.out.println("\n🔗 Testing file access...");
            boolean connected = dataSource.testConnection();
            System.out.println("File Access: " + (connected ? "✅ ACCESSIBLE" : "❌ FAILED"));
            
            if (connected) {
                // Execute sample queries
                System.out.println("\n📊 Executing sample queries...");
                
                try {
                    Map<String, Object> parameters = Map.of("department", "Engineering");
                    List<Object> results = dataSource.query("findByDepartment", parameters);
                    System.out.println("Engineering Users: " + (results != null ? results.size() + " found" : "none"));
                } catch (DataSourceException e) {
                    System.out.println("Query Result: ⚠️ " + e.getMessage());
                }
                
                try {
                    List<Object> allResults = dataSource.query("getAll", new HashMap<>());
                    System.out.println("All Users: " + (allResults != null ? allResults.size() + " found" : "none"));
                } catch (DataSourceException e) {
                    System.out.println("Query Result: ⚠️ " + e.getMessage());
                }
            }
            
            dataSource.shutdown();
            System.out.println("✅ CSV file integration demonstrated");
            
        } catch (Exception e) {
            System.out.println("❌ CSV file demo failed: " + e.getMessage());
        }
        
        System.out.println();
    }

    private void demonstrateJsonFileIntegration() {
        System.out.println("📋 JSON FILE INTEGRATION DEMONSTRATION");
        System.out.println("-".repeat(50));
        
        try {
            Path jsonFile = tempDir.resolve("products.json");
            DataSourceConfiguration config = createJsonFileConfiguration(jsonFile.toString());
            ExternalDataSource dataSource = factory.createDataSource(config);
            
            System.out.println("Configuration:");
            System.out.println("  Name: " + config.getName());
            System.out.println("  Type: " + config.getSourceType());
            System.out.println("  File: " + jsonFile.getFileName());
            
            // Test connection
            System.out.println("\n🔗 Testing file access...");
            boolean connected = dataSource.testConnection();
            System.out.println("File Access: " + (connected ? "✅ ACCESSIBLE" : "❌ FAILED"));
            
            if (connected) {
                // Execute JSONPath queries
                System.out.println("\n📊 Executing JSONPath queries...");
                
                try {
                    Map<String, Object> parameters = Map.of("id", "LAPTOP001");
                    Object result = dataSource.queryForObject("getProductById", parameters);
                    System.out.println("Product Lookup: " + (result != null ? "✅ FOUND" : "❌ NOT FOUND"));
                } catch (DataSourceException e) {
                    System.out.println("Query Result: ⚠️ " + e.getMessage());
                }
                
                try {
                    List<Object> allResults = dataSource.query("getAllProducts", new HashMap<>());
                    System.out.println("All Products: " + (allResults != null ? allResults.size() + " found" : "none"));
                } catch (DataSourceException e) {
                    System.out.println("Query Result: ⚠️ " + e.getMessage());
                }
            }
            
            dataSource.shutdown();
            System.out.println("✅ JSON file integration demonstrated");
            
        } catch (Exception e) {
            System.out.println("❌ JSON file demo failed: " + e.getMessage());
        }
        
        System.out.println();
    }

    private void demonstrateCacheIntegration() {
        System.out.println("🗄️ CACHE INTEGRATION DEMONSTRATION");
        System.out.println("-".repeat(50));

        try {
            // Create a cached CSV configuration using the dedicated method
            Path csvFile = tempDir.resolve("users.csv");
            DataSourceConfiguration config = createCachedCsvConfiguration(csvFile.toString());
            ExternalDataSource dataSource = factory.createDataSource(config);

            System.out.println("Cache Configuration:");
            System.out.println("  Type: In-Memory Cache");
            System.out.println("  TTL: 300 seconds");
            System.out.println("  Max Size: 1000 entries");
            System.out.println();

            // Demonstrate cache performance
            System.out.println("Testing cache performance:");

            // First lookup (cache miss)
            long startTime = System.nanoTime();
            Map<String, Object> parameters = Map.of("id", "1");
            long firstLookupTime = System.nanoTime() - startTime;

            System.out.println("  First lookup (cache miss): " + firstLookupTime/1000 + " μs");

            // Second lookup (cache hit)
            startTime = System.nanoTime();
            Object cachedResult = dataSource.queryForObject("getUserById", parameters);
            long secondLookupTime = System.nanoTime() - startTime;

            System.out.println("  Second lookup (cache hit): " + secondLookupTime/1000 + " μs");
            System.out.println("  Cached result available: " + (cachedResult != null ? "✅ YES" : "❌ NO"));
            System.out.println("  Performance improvement: " +
                             (firstLookupTime / (double) secondLookupTime) + "x faster");

            System.out.println("✅ Cache integration demonstration completed");

            dataSource.shutdown();

        } catch (Exception e) {
            System.out.println("❌ Cache integration failed: " + e.getMessage());
            logger.error("Cache integration demonstration failed", e);
        }

        System.out.println();
    }

    private void demonstrateHealthMonitoring() {
        System.out.println("🏥 HEALTH MONITORING DEMONSTRATION");
        System.out.println("-".repeat(50));

        try {
            Path csvFile = tempDir.resolve("users.csv");
            DataSourceConfiguration config = createHealthMonitoredConfiguration(csvFile.toString());
            ExternalDataSource dataSource = factory.createDataSource(config);

            System.out.println("Configuration:");
            System.out.println("  Name: " + config.getName());
            System.out.println("  Health Check Enabled: " + config.getHealthCheck().isEnabled());
            System.out.println("  Health Check Interval: " + config.getHealthCheck().getIntervalSeconds() + " seconds");
            System.out.println("  Health Check Timeout: " + config.getHealthCheck().getTimeoutSeconds() + " seconds");

            // Check health status
            System.out.println("\n🔍 Checking health status...");
            boolean healthy = dataSource.isHealthy();
            System.out.println("Health Status: " + (healthy ? "✅ HEALTHY" : "❌ UNHEALTHY"));

            // Get connection status
            ConnectionStatus status = dataSource.getConnectionStatus();
            System.out.println("\nConnection Status:");
            System.out.println("  State: " + status.getState());
            System.out.println("  Last Updated: " + status.getLastUpdated());
            System.out.println("  Last Connected: " + status.getLastConnected());
            System.out.println("  Message: " + status.getMessage());

            // Get health metrics
            DataSourceMetrics metrics = dataSource.getMetrics();
            System.out.println("\nHealth Metrics:");
            System.out.println("  Connection Attempts: " + metrics.getConnectionAttempts());
            System.out.println("  Successful Connections: " + metrics.getSuccessfulConnections());
            System.out.println("  Connection Failures: " + metrics.getConnectionFailures());
            System.out.println("  Success Rate: " + String.format("%.2f%%", metrics.getSuccessRate()));

            dataSource.shutdown();
            System.out.println("✅ Health monitoring demonstrated");

        } catch (Exception e) {
            System.out.println("❌ Health monitoring demo failed: " + e.getMessage());
        }

        System.out.println();
    }

    private void demonstrateErrorHandling() {
        System.out.println("🛡️ ERROR HANDLING DEMONSTRATION");
        System.out.println("-".repeat(50));

        try {
            // Create configuration with invalid settings
            DataSourceConfiguration config = createInvalidConfiguration();
            ExternalDataSource dataSource = factory.createDataSource(config);

            System.out.println("Configuration:");
            System.out.println("  Name: " + config.getName());
            System.out.println("  Type: " + config.getSourceType());
            System.out.println("  Host: " + config.getConnection().getHost());

            // Test connection failure
            System.out.println("\n🔗 Testing connection to invalid host...");
            boolean connected = dataSource.testConnection();
            System.out.println("Connection Status: " + (connected ? "✅ CONNECTED" : "❌ FAILED (Expected)"));

            // Test health check failure
            System.out.println("\n🏥 Checking health...");
            boolean healthy = dataSource.isHealthy();
            System.out.println("Health Status: " + (healthy ? "✅ HEALTHY" : "❌ UNHEALTHY (Expected)"));

            // Test query error handling
            System.out.println("\n📊 Testing query error handling...");
            try {
                Map<String, Object> parameters = new HashMap<>();
                dataSource.query("invalidQuery", parameters);
                System.out.println("Query Result: ⚠️ Unexpected success");
            } catch (DataSourceException e) {
                System.out.println("Query Result: ✅ Error handled correctly - " + e.getMessage());
            }

            // Show error metrics
            DataSourceMetrics metrics = dataSource.getMetrics();
            System.out.println("\nError Metrics:");
            System.out.println("  Total Requests: " + metrics.getTotalRequests());
            System.out.println("  Failed Requests: " + metrics.getFailedRequests());
            System.out.println("  Success Rate: " + String.format("%.2f%%", metrics.getSuccessRate()));

            dataSource.shutdown();
            System.out.println("✅ Error handling demonstrated");

        } catch (Exception e) {
            System.out.println("❌ Error handling demo failed: " + e.getMessage());
        }

        System.out.println();
    }

    private void demonstratePerformanceMetrics() {
        System.out.println("📈 PERFORMANCE METRICS DEMONSTRATION");
        System.out.println("-".repeat(50));

        try {
            Path csvFile = tempDir.resolve("users.csv");
            DataSourceConfiguration config = createCsvFileConfiguration(csvFile.toString());
            ExternalDataSource dataSource = factory.createDataSource(config);

            if (dataSource.testConnection()) {
                System.out.println("🚀 Executing multiple queries to generate metrics...");

                // Execute multiple queries
                for (int i = 0; i < 5; i++) {
                    try {
                        Map<String, Object> parameters = Map.of("department", i % 2 == 0 ? "Engineering" : "Marketing");
                        dataSource.query("findByDepartment", parameters);
                        System.out.print(".");
                    } catch (DataSourceException e) {
                        System.out.print("x");
                    }
                }
                System.out.println(" Done!");

                // Show performance metrics
                DataSourceMetrics metrics = dataSource.getMetrics();
                System.out.println("\nPerformance Metrics:");
                System.out.println("  Total Requests: " + metrics.getTotalRequests());
                System.out.println("  Successful Requests: " + metrics.getSuccessfulRequests());
                System.out.println("  Failed Requests: " + metrics.getFailedRequests());
                System.out.println("  Success Rate: " + String.format("%.2f%%", metrics.getSuccessRate()));
                System.out.println("  Average Response Time: " + String.format("%.2fms", metrics.getAverageResponseTime()));
                System.out.println("  Min Response Time: " + String.format("%.2fms", metrics.getMinResponseTime()));
                System.out.println("  Max Response Time: " + String.format("%.2fms", metrics.getMaxResponseTime()));
                System.out.println("  Records Processed: " + metrics.getRecordsProcessed());
            }

            dataSource.shutdown();
            System.out.println("✅ Performance metrics demonstrated");

        } catch (Exception e) {
            System.out.println("❌ Performance metrics demo failed: " + e.getMessage());
        }

        System.out.println();
    }

    private void cleanup() {
        System.out.println("🧹 Cleaning up resources...");
        try {
            if (factory != null) {
                factory.clearCache();
            }
            if (tempDir != null && Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
            }
            System.out.println("✅ Cleanup completed");
        } catch (Exception e) {
            System.out.println("⚠️ Cleanup warning: " + e.getMessage());
        }
    }

    // Helper methods to create configurations
    private DataSourceConfiguration createRestApiConfiguration() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("demo-rest-api");
        config.setSourceType("rest-api");
        config.setDataSourceType(DataSourceType.REST_API);
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setHost("httpbin.org");
        connectionConfig.setPort(443);
        connectionConfig.setTimeout(30000);
        config.setConnection(connectionConfig);

        Map<String, String> queries = new HashMap<>();
        queries.put("getUserById", "/get?id={id}");
        queries.put("getAllUsers", "/get");
        config.setQueries(queries);

        return config;
    }

    private DataSourceConfiguration createCsvFileConfiguration(String filePath) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("demo-csv-file");
        config.setSourceType("csv-file");
        config.setDataSourceType(DataSourceType.FILE_SYSTEM);
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setBasePath(filePath);
        config.setConnection(connectionConfig);

        Map<String, String> queries = new HashMap<>();
        queries.put("findByDepartment", "SELECT * WHERE department = :department");
        queries.put("getAll", "SELECT *");
        config.setQueries(queries);

        return config;
    }

    private DataSourceConfiguration createJsonFileConfiguration(String filePath) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("demo-json-file");
        config.setSourceType("json-file");
        config.setDataSourceType(DataSourceType.FILE_SYSTEM);
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setBasePath(filePath);
        config.setConnection(connectionConfig);

        Map<String, String> queries = new HashMap<>();
        queries.put("getProductById", "$.products[?(@.id == '{id}')]");
        queries.put("getAllProducts", "$.products[*]");
        config.setQueries(queries);

        return config;
    }

    private DataSourceConfiguration createCachedCsvConfiguration(String filePath) {
        DataSourceConfiguration config = createCsvFileConfiguration(filePath);

        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(300L);
        cacheConfig.setMaxSize(1000);
        config.setCache(cacheConfig);

        return config;
    }

    private DataSourceConfiguration createHealthMonitoredConfiguration(String filePath) {
        DataSourceConfiguration config = createCsvFileConfiguration(filePath);

        HealthCheckConfig healthConfig = new HealthCheckConfig();
        healthConfig.setEnabled(true);
        healthConfig.setIntervalSeconds(30L);
        healthConfig.setTimeoutSeconds(5L);
        config.setHealthCheck(healthConfig);

        return config;
    }

    private DataSourceConfiguration createInvalidConfiguration() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("demo-invalid-api");
        config.setSourceType("rest-api");
        config.setDataSourceType(DataSourceType.REST_API);
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setHost("invalid-host-that-does-not-exist.com");
        connectionConfig.setPort(80);
        connectionConfig.setTimeout(5000);
        config.setConnection(connectionConfig);

        Map<String, String> queries = new HashMap<>();
        queries.put("invalidQuery", "/invalid");
        config.setQueries(queries);

        return config;
    }
}
