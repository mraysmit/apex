package dev.mars.apex.core.examples;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.DataSourceType;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.config.DataSourceConfigurationService;
import dev.mars.apex.core.service.data.external.manager.DataSourceManager;
import dev.mars.apex.core.service.data.external.manager.DataSourceManagerStatistics;
import dev.mars.apex.core.service.data.external.registry.DataSourceRegistry;
import dev.mars.apex.core.service.data.external.registry.RegistryStatistics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo application showcasing external data source integration.
 * 
 * This demo demonstrates:
 * - Creating data sources programmatically
 * - Loading data sources from YAML configuration
 * - Using the data source manager for operations
 * - Monitoring and statistics collection
 * - Error handling and recovery
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DataSourceDemo {
    
    private static final String DEMO_DATA_DIR = "demo-data";
    
    public static void main(String[] args) {
        System.out.println("=== SpEL Rules Engine - Data Source Integration Demo ===\n");
        
        DataSourceDemo demo = new DataSourceDemo();
        
        try {
            // Setup demo environment
            demo.setupDemoEnvironment();
            
            // Run different demo scenarios
            demo.runProgrammaticDemo();
            demo.runYamlConfigurationDemo();
            demo.runMultiSourceDemo();
            demo.runPerformanceDemo();
            demo.runMonitoringDemo();
            
            System.out.println("\n=== Demo completed successfully! ===");
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            demo.cleanup();
        }
    }
    
    /**
     * Setup demo environment with test data files.
     */
    private void setupDemoEnvironment() throws IOException {
        System.out.println("1. Setting up demo environment...");
        
        // Create demo data directory
        Path demoDir = Paths.get(DEMO_DATA_DIR);
        Files.createDirectories(demoDir);
        
        // Create sample CSV file
        Path csvFile = demoDir.resolve("users.csv");
        String csvContent = "id,name,email,status\n" +
                           "1,John Doe,john@example.com,active\n" +
                           "2,Jane Smith,jane@example.com,active\n" +
                           "3,Bob Johnson,bob@example.com,inactive\n";
        Files.writeString(csvFile, csvContent);
        
        // Create sample JSON file
        Path jsonFile = demoDir.resolve("config.json");
        String jsonContent = "{\n" +
                             "  \"appName\": \"Demo Application\",\n" +
                             "  \"version\": \"1.0.0\",\n" +
                             "  \"features\": [\"caching\", \"monitoring\", \"analytics\"]\n" +
                             "}";
        Files.writeString(jsonFile, jsonContent);
        
        System.out.println("   ✓ Demo environment setup complete\n");
    }
    
    /**
     * Demonstrate programmatic data source creation.
     */
    private void runProgrammaticDemo() throws DataSourceException {
        System.out.println("2. Programmatic Data Source Demo");
        System.out.println("   Creating data sources programmatically...");
        
        DataSourceManager manager = new DataSourceManager();
        
        try {
            // Create cache data source configuration
            DataSourceConfiguration cacheConfig = new DataSourceConfiguration();
            cacheConfig.setName("demo-cache");
            cacheConfig.setSourceType("memory");
            cacheConfig.setDataSourceType(DataSourceType.CACHE);
            
            CacheConfig cache = new CacheConfig();
            cache.setEnabled(true);
            cache.setMaxSize(1000);
            cache.setTtlSeconds(300L);
            cacheConfig.setCache(cache);
            
            // Create file system data source configuration
            DataSourceConfiguration fileConfig = new DataSourceConfiguration();
            fileConfig.setName("demo-files");
            fileConfig.setSourceType("file-system");
            fileConfig.setDataSourceType(DataSourceType.FILE_SYSTEM);
            
            ConnectionConfig fileConnection = new ConnectionConfig();
            fileConnection.setBasePath(DEMO_DATA_DIR);
            fileConnection.setFilePattern("*.csv");
            fileConfig.setConnection(fileConnection);
            
            // Initialize manager with configurations
            manager.initialize(List.of(cacheConfig, fileConfig));
            
            // Test data source operations
            ExternalDataSource cacheSource = manager.getDataSource("demo-cache");
            ExternalDataSource fileSource = manager.getDataSource("demo-files");
            
            System.out.println("   ✓ Cache data source: " + (cacheSource != null ? "OK" : "FAILED"));
            System.out.println("   ✓ File data source: " + (fileSource != null ? "OK" : "FAILED"));
            System.out.println("   ✓ Cache healthy: " + (cacheSource != null && cacheSource.isHealthy()));
            System.out.println("   ✓ File healthy: " + (fileSource != null && fileSource.isHealthy()));
            
            // Test data retrieval
            if (fileSource != null) {
                Object data = fileSource.getData("csv", "users.csv");
                System.out.println("   ✓ File data retrieved: " + (data != null ? "OK" : "FAILED"));
            }
            
        } finally {
            manager.shutdown();
        }
        
        System.out.println("   Programmatic demo completed\n");
    }
    
    /**
     * Demonstrate YAML configuration loading.
     */
    private void runYamlConfigurationDemo() throws DataSourceException {
        System.out.println("3. YAML Configuration Demo");
        System.out.println("   Loading data sources from YAML configuration...");
        
        DataSourceConfigurationService configService = DataSourceConfigurationService.getInstance();
        
        try {
            // Create YAML configuration
            YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
            
            // Cache data source
            YamlDataSource cacheDataSource = new YamlDataSource();
            cacheDataSource.setName("yaml-cache");
            cacheDataSource.setType("cache");
            cacheDataSource.setSourceType("memory");
            cacheDataSource.setEnabled(true);
            cacheDataSource.setTags(List.of("demo", "cache"));
            
            Map<String, Object> cacheSettings = new HashMap<>();
            cacheSettings.put("enabled", true);
            cacheSettings.put("maxSize", 500);
            cacheSettings.put("ttlSeconds", 600);
            cacheDataSource.setCache(cacheSettings);
            
            // File data source
            YamlDataSource fileDataSource = new YamlDataSource();
            fileDataSource.setName("yaml-files");
            fileDataSource.setType("file-system");
            fileDataSource.setBasePath(DEMO_DATA_DIR);
            fileDataSource.setFilePattern("*.json");
            fileDataSource.setEnabled(true);
            fileDataSource.setTags(List.of("demo", "files"));
            
            yamlConfig.setDataSources(List.of(cacheDataSource, fileDataSource));
            
            // Initialize service
            configService.initialize(yamlConfig);
            
            // Verify data sources
            ExternalDataSource cacheSource = configService.getDataSource("yaml-cache");
            ExternalDataSource fileSource = configService.getDataSource("yaml-files");
            
            System.out.println("   ✓ YAML cache source: " + (cacheSource != null ? "OK" : "FAILED"));
            System.out.println("   ✓ YAML file source: " + (fileSource != null ? "OK" : "FAILED"));
            System.out.println("   ✓ Configuration count: " + configService.getConfigurationNames().size());
            
            // Test data retrieval
            if (fileSource != null) {
                Object configData = fileSource.getData("json", "config.json");
                System.out.println("   ✓ JSON config retrieved: " + (configData != null ? "OK" : "FAILED"));
            }
            
        } finally {
            configService.shutdown();
        }
        
        System.out.println("   YAML configuration demo completed\n");
    }
    
    /**
     * Demonstrate multi-source operations.
     */
    private void runMultiSourceDemo() throws DataSourceException {
        System.out.println("4. Multi-Source Operations Demo");
        System.out.println("   Demonstrating operations across multiple data sources...");
        
        DataSourceManager manager = new DataSourceManager();
        
        try {
            // Create multiple data source configurations
            DataSourceConfiguration cache1 = createCacheConfig("cache-1", 1000);
            DataSourceConfiguration cache2 = createCacheConfig("cache-2", 500);
            DataSourceConfiguration fileConfig = createFileConfig("files", DEMO_DATA_DIR);
            
            manager.initialize(List.of(cache1, cache2, fileConfig));
            
            // Test load balancing
            ExternalDataSource cacheSource1 = manager.getDataSourceWithLoadBalancing(DataSourceType.CACHE);
            ExternalDataSource cacheSource2 = manager.getDataSourceWithLoadBalancing(DataSourceType.CACHE);
            
            System.out.println("   ✓ Load balanced source 1: " + (cacheSource1 != null ? cacheSource1.getName() : "FAILED"));
            System.out.println("   ✓ Load balanced source 2: " + (cacheSource2 != null ? cacheSource2.getName() : "FAILED"));
            
            // Test failover query
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("key", "test");
            
            try {
                List<Object> results = manager.queryWithFailover(DataSourceType.CACHE, "test-*", parameters);
                System.out.println("   ✓ Failover query: OK (returned " + results.size() + " results)");
            } catch (DataSourceException e) {
                System.out.println("   ✓ Failover query: Expected failure - " + e.getMessage());
            }
            
            // Test health monitoring
            List<ExternalDataSource> healthySources = manager.getHealthyDataSources();
            List<ExternalDataSource> unhealthySources = manager.getUnhealthyDataSources();
            
            System.out.println("   ✓ Healthy sources: " + healthySources.size());
            System.out.println("   ✓ Unhealthy sources: " + unhealthySources.size());
            
        } finally {
            manager.shutdown();
        }
        
        System.out.println("   Multi-source demo completed\n");
    }
    
    /**
     * Demonstrate performance characteristics.
     */
    private void runPerformanceDemo() throws DataSourceException {
        System.out.println("5. Performance Demo");
        System.out.println("   Testing performance characteristics...");
        
        DataSourceManager manager = new DataSourceManager();
        
        try {
            // Create cache for performance testing
            DataSourceConfiguration cacheConfig = createCacheConfig("perf-cache", 10000);
            manager.initialize(List.of(cacheConfig));
            
            ExternalDataSource cache = manager.getDataSource("perf-cache");
            
            if (cache != null) {
                // Performance test: multiple operations
                long startTime = System.currentTimeMillis();
                int operationCount = 1000;
                
                for (int i = 0; i < operationCount; i++) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("key", "test-key-" + i);
                    
                    try {
                        cache.query("test-pattern", params);
                    } catch (DataSourceException e) {
                        // Expected for cache queries
                    }
                }
                
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                double opsPerSecond = (double) operationCount / (duration / 1000.0);
                
                System.out.println("   ✓ Operations: " + operationCount);
                System.out.println("   ✓ Duration: " + duration + "ms");
                System.out.println("   ✓ Operations/second: " + String.format("%.2f", opsPerSecond));
            }
            
        } finally {
            manager.shutdown();
        }
        
        System.out.println("   Performance demo completed\n");
    }
    
    /**
     * Demonstrate monitoring and statistics.
     */
    private void runMonitoringDemo() throws DataSourceException {
        System.out.println("6. Monitoring and Statistics Demo");
        System.out.println("   Collecting metrics and statistics...");
        
        DataSourceManager manager = new DataSourceManager();
        DataSourceRegistry registry = DataSourceRegistry.getInstance();
        
        try {
            // Create data sources for monitoring
            DataSourceConfiguration cache = createCacheConfig("monitor-cache", 1000);
            DataSourceConfiguration files = createFileConfig("monitor-files", DEMO_DATA_DIR);
            
            manager.initialize(List.of(cache, files));
            
            // Perform some operations to generate metrics
            ExternalDataSource cacheSource = manager.getDataSource("monitor-cache");
            ExternalDataSource fileSource = manager.getDataSource("monitor-files");
            
            if (cacheSource != null && fileSource != null) {
                // Generate some activity
                for (int i = 0; i < 10; i++) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("key", "metric-test-" + i);
                    
                    try {
                        cacheSource.query("pattern", params);
                        fileSource.getData("csv", "users.csv");
                    } catch (DataSourceException e) {
                        // Expected
                    }
                }
            }
            
            // Collect statistics
            DataSourceManagerStatistics managerStats = manager.getStatistics();
            RegistryStatistics registryStats = registry.getStatistics();
            
            System.out.println("   Manager Statistics:");
            System.out.println("     ✓ Total data sources: " + registryStats.getTotalDataSources());
            System.out.println("     ✓ Healthy data sources: " + registryStats.getHealthyDataSources());
            System.out.println("     ✓ Health percentage: " + String.format("%.1f%%", registryStats.getHealthPercentage()));
            System.out.println("     ✓ Type groups: " + managerStats.getTypeGroupCount());
            
            System.out.println("   Registry Statistics:");
            System.out.println("     ✓ " + registryStats.getSummary());
            
            // Individual data source metrics
            if (cacheSource != null) {
                var cacheMetrics = cacheSource.getMetrics();
                System.out.println("   Cache Metrics:");
                System.out.println("     ✓ Successful requests: " + cacheMetrics.getSuccessfulRequests());
                System.out.println("     ✓ Failed requests: " + cacheMetrics.getFailedRequests());
                System.out.println("     ✓ Cache hits: " + cacheMetrics.getCacheHits());
                System.out.println("     ✓ Cache misses: " + cacheMetrics.getCacheMisses());
            }
            
        } finally {
            manager.shutdown();
        }
        
        System.out.println("   Monitoring demo completed\n");
    }
    
    /**
     * Helper method to create cache configuration.
     */
    private DataSourceConfiguration createCacheConfig(String name, int maxSize) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName(name);
        config.setSourceType("memory");
        config.setDataSourceType(DataSourceType.CACHE);
        
        CacheConfig cache = new CacheConfig();
        cache.setEnabled(true);
        cache.setMaxSize(maxSize);
        cache.setTtlSeconds(300L);
        config.setCache(cache);
        
        return config;
    }
    
    /**
     * Helper method to create file system configuration.
     */
    private DataSourceConfiguration createFileConfig(String name, String basePath) {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName(name);
        config.setSourceType("file-system");
        config.setDataSourceType(DataSourceType.FILE_SYSTEM);
        
        ConnectionConfig connection = new ConnectionConfig();
        connection.setBasePath(basePath);
        connection.setFilePattern("*.*");
        config.setConnection(connection);
        
        return config;
    }
    
    /**
     * Cleanup demo environment.
     */
    private void cleanup() {
        try {
            // Clean up demo data directory
            Path demoDir = Paths.get(DEMO_DATA_DIR);
            if (Files.exists(demoDir)) {
                Files.walk(demoDir)
                     .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                     .forEach(path -> {
                         try {
                             Files.delete(path);
                         } catch (IOException e) {
                             // Ignore cleanup errors
                         }
                     });
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
        
        System.out.println("Demo cleanup completed.");
    }
}
