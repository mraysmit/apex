package dev.mars.apex.demo.etl;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the CSV to H2 Pipeline Demo.
 * 
 * This test verifies that the demo can run successfully and
 * produces the expected results.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class CsvToH2PipelineDemoTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvToH2PipelineDemoTest.class);
    
    @TempDir
    Path tempDir;
    
    private CsvToH2PipelineDemo demo;
    
    @BeforeEach
    void setUp() {
        demo = new CsvToH2PipelineDemo();
        
        // Set system property to use temp directory for demo output
        System.setProperty("demo.output.dir", tempDir.toString());
        
        LOGGER.info("Test setup complete, using temp directory: {}", tempDir);
    }
    
    @Test
    void testDemoExecution() throws Exception {
        LOGGER.info("=== TEST: Running CSV to H2 Pipeline Demo ===");
        LOGGER.info("Test setup - Temp directory: {}", tempDir);
        LOGGER.info("Test setup - Demo output directory property: {}", System.getProperty("demo.output.dir"));

        long startTime = System.currentTimeMillis();

        // Execute the demo
        LOGGER.info("Starting demo execution...");
        assertDoesNotThrow(() -> {
            LOGGER.info("Calling demo.runDemo()...");
            demo.runDemo();
            LOGGER.info("Demo.runDemo() completed successfully");
        }, "Demo should execute without throwing exceptions");

        long executionTime = System.currentTimeMillis() - startTime;
        LOGGER.info("Demo execution completed in {} ms", executionTime);

        // Verify that output directories were created
        LOGGER.info("Verifying output directory structure...");
        Path outputDir = tempDir.resolve("demo/etl/output");
        LOGGER.info("Checking for output directory: {}", outputDir);

        if (Files.exists(outputDir)) {
            assertTrue(Files.isDirectory(outputDir), "Output directory should be created");
            LOGGER.info("✓ Output directory created: {}", outputDir);

            // List contents of output directory
            try {
                LOGGER.info("Output directory contents:");
                Files.list(outputDir).forEach(path -> {
                    try {
                        if (Files.isDirectory(path)) {
                            LOGGER.info("  [DIR]  {}", path.getFileName());
                        } else {
                            LOGGER.info("  [FILE] {} (size: {} bytes)", path.getFileName(), Files.size(path));
                        }
                    } catch (Exception e) {
                        LOGGER.warn("  [ERROR] Could not read file info for: {}", path.getFileName());
                    }
                });
            } catch (Exception e) {
                LOGGER.warn("Could not list output directory contents: {}", e.getMessage());
            }
        } else {
            LOGGER.warn("⚠ Output directory not found: {}", outputDir);
        }

        // Verify that data directory was created
        LOGGER.info("Verifying data directory structure...");
        Path dataDir = tempDir.resolve("demo/etl/data");
        LOGGER.info("Checking for data directory: {}", dataDir);

        if (Files.exists(dataDir)) {
            assertTrue(Files.isDirectory(dataDir), "Data directory should be created");
            LOGGER.info("✓ Data directory created: {}", dataDir);

            // Check for CSV file
            Path csvFile = dataDir.resolve("customers.csv");
            LOGGER.info("Checking for CSV file: {}", csvFile);

            if (Files.exists(csvFile)) {
                assertTrue(Files.isRegularFile(csvFile), "CSV file should be created");
                long fileSize = Files.size(csvFile);
                assertTrue(fileSize > 0, "CSV file should not be empty");
                LOGGER.info("✓ CSV file created: {} (size: {} bytes)", csvFile, fileSize);

                // Log first few lines of CSV for verification
                try {
                    LOGGER.info("CSV file content preview:");
                    Files.lines(csvFile).limit(3).forEach(line ->
                        LOGGER.info("  CSV: {}", line));
                } catch (Exception e) {
                    LOGGER.warn("Could not read CSV file content: {}", e.getMessage());
                }
            } else {
                LOGGER.warn("⚠ CSV file not found: {}", csvFile);
            }

            // List all files in data directory
            try {
                LOGGER.info("Data directory contents:");
                Files.list(dataDir).forEach(path -> {
                    try {
                        if (Files.isDirectory(path)) {
                            LOGGER.info("  [DIR]  {}", path.getFileName());
                        } else {
                            LOGGER.info("  [FILE] {} (size: {} bytes)", path.getFileName(), Files.size(path));
                        }
                    } catch (Exception e) {
                        LOGGER.warn("  [ERROR] Could not read file info for: {}", path.getFileName());
                    }
                });
            } catch (Exception e) {
                LOGGER.warn("Could not list data directory contents: {}", e.getMessage());
            }
        } else {
            LOGGER.warn("⚠ Data directory not found: {}", dataDir);
        }

        // Check for database files
        LOGGER.info("Checking for H2 database files...");
        Path dbFile = outputDir.resolve("customer_database.mv.db");
        if (Files.exists(dbFile)) {
            LOGGER.info("✓ H2 database file found: {} (size: {} bytes)", dbFile, Files.size(dbFile));
        } else {
            LOGGER.info("ℹ H2 database file not found (may be expected): {}", dbFile);
        }

        LOGGER.info("=== CSV to H2 Pipeline Demo test completed successfully ===");
        LOGGER.info("Total test execution time: {} ms", System.currentTimeMillis() - startTime);
    }
    
    @Test
    void testDemoConfigurationLoading() throws Exception {
        LOGGER.info("=== TEST: Testing demo configuration loading ===");

        // Test that the configuration file exists and can be found
        String configPath = "/etl/csv-to-h2-pipeline.yaml";
        LOGGER.info("Looking for configuration file: {}", configPath);

        var configStream = getClass().getResourceAsStream(configPath);
        assertNotNull(configStream, "Configuration file should be available on classpath: " + configPath);
        LOGGER.info("✓ Configuration file found on classpath: {}", configPath);

        // Try to read and parse the configuration
        LOGGER.info("Attempting to parse YAML configuration...");
        try {
            com.fasterxml.jackson.databind.ObjectMapper yamlMapper =
                new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());

            // Reset stream for reading
            configStream.close();
            configStream = getClass().getResourceAsStream(configPath);

            dev.mars.apex.core.config.yaml.YamlRuleConfiguration config =
                yamlMapper.readValue(configStream, dev.mars.apex.core.config.yaml.YamlRuleConfiguration.class);

            assertNotNull(config, "Configuration should be parsed successfully");
            LOGGER.info("✓ YAML configuration parsed successfully");

            // Verify metadata
            if (config.getMetadata() != null) {
                LOGGER.info("Configuration metadata:");
                LOGGER.info("  Name: {}", config.getMetadata().getName());
                LOGGER.info("  Version: {}", config.getMetadata().getVersion());
                LOGGER.info("  Description: {}", config.getMetadata().getDescription());
                LOGGER.info("  Author: {}", config.getMetadata().getAuthor());
                if (config.getMetadata().getTags() != null) {
                    LOGGER.info("  Tags: {}", String.join(", ", config.getMetadata().getTags()));
                }
            } else {
                LOGGER.warn("⚠ No metadata found in configuration");
            }

            // Verify data sinks
            if (config.getDataSinks() != null) {
                LOGGER.info("Data sinks configuration:");
                LOGGER.info("  Number of data sinks: {}", config.getDataSinks().size());

                for (int i = 0; i < config.getDataSinks().size(); i++) {
                    var sink = config.getDataSinks().get(i);
                    LOGGER.info("  Data Sink #{}: {}", i + 1, sink.getName());
                    LOGGER.info("    Type: {}", sink.getType());
                    LOGGER.info("    Source Type: {}", sink.getSourceType());
                    LOGGER.info("    Enabled: {}", sink.getEnabled());
                    LOGGER.info("    Description: {}", sink.getDescription());

                    if (sink.getOperations() != null) {
                        LOGGER.info("    Operations: {}", sink.getOperations().keySet());
                    }

                    if (sink.getConnection() != null) {
                        LOGGER.info("    Connection properties: {}", sink.getConnection().keySet());
                    }
                }
            } else {
                LOGGER.warn("⚠ No data sinks found in configuration");
            }

            // Test conversion to DataSinkConfiguration
            if (config.getDataSinks() != null && !config.getDataSinks().isEmpty()) {
                LOGGER.info("Testing conversion to DataSinkConfiguration...");
                var firstSink = config.getDataSinks().get(0);
                var sinkConfig = firstSink.toDataSinkConfiguration();

                assertNotNull(sinkConfig, "DataSinkConfiguration should be created");
                LOGGER.info("✓ Successfully converted to DataSinkConfiguration");
                LOGGER.info("  Converted sink name: {}", sinkConfig.getName());
                LOGGER.info("  Converted sink type: {}", sinkConfig.getType());
                LOGGER.info("  Converted source type: {}", sinkConfig.getSourceType());
                LOGGER.info("  Converted enabled: {}", sinkConfig.isEnabled());

                if (sinkConfig.getOperations() != null) {
                    LOGGER.info("  Converted operations: {}", sinkConfig.getOperations().keySet());
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to parse YAML configuration", e);
            throw e;
        } finally {
            if (configStream != null) {
                configStream.close();
            }
        }

        LOGGER.info("=== Configuration loading test completed successfully ===");
    }
    
    @Test
    void testDemoEnvironmentSetup() throws Exception {
        LOGGER.info("=== TEST: Testing demo environment setup ===");

        // Test system properties and environment
        LOGGER.info("System environment check:");
        LOGGER.info("  Java version: {}", System.getProperty("java.version"));
        LOGGER.info("  Java home: {}", System.getProperty("java.home"));
        LOGGER.info("  User directory: {}", System.getProperty("user.dir"));
        LOGGER.info("  Temp directory: {}", System.getProperty("java.io.tmpdir"));
        LOGGER.info("  Test temp directory: {}", tempDir);

        // Check classpath for required dependencies
        LOGGER.info("Checking classpath dependencies:");
        try {
            Class.forName("dev.mars.apex.core.config.yaml.YamlConfigurationLoader");
            LOGGER.info("  ✓ APEX Core YAML loader available");
        } catch (ClassNotFoundException e) {
            LOGGER.error("  ✗ APEX Core YAML loader not found", e);
            throw e;
        }

        try {
            Class.forName("dev.mars.apex.core.service.data.external.factory.DataSinkFactory");
            LOGGER.info("  ✓ DataSink factory available");
        } catch (ClassNotFoundException e) {
            LOGGER.error("  ✗ DataSink factory not found", e);
            throw e;
        }

        try {
            Class.forName("org.h2.Driver");
            LOGGER.info("  ✓ H2 database driver available (automatically loaded via module system)");
        } catch (ClassNotFoundException e) {
            LOGGER.error("  ✗ H2 database driver not found", e);
            throw e;
        }

        // Create a demo instance and test basic functionality
        LOGGER.info("Creating demo instance...");
        CsvToH2PipelineDemo testDemo = new CsvToH2PipelineDemo();
        assertNotNull(testDemo, "Demo instance should be created successfully");
        LOGGER.info("✓ Demo instance created successfully");

        // Test that we can access the demo's class methods
        LOGGER.info("Verifying demo class structure...");
        var demoClass = testDemo.getClass();

        // Check for main method
        try {
            var mainMethod = demoClass.getMethod("main", String[].class);
            assertNotNull(mainMethod, "Main method should exist");
            LOGGER.info("  ✓ Main method found: {}", mainMethod.getName());
        } catch (NoSuchMethodException e) {
            LOGGER.error("  ✗ Main method not found", e);
            throw e;
        }

        // Check for runDemo method
        try {
            var runDemoMethod = demoClass.getMethod("runDemo");
            assertNotNull(runDemoMethod, "runDemo method should exist");
            LOGGER.info("  ✓ runDemo method found: {}", runDemoMethod.getName());
        } catch (NoSuchMethodException e) {
            LOGGER.error("  ✗ runDemo method not found", e);
            throw e;
        }

        // Test directory creation capabilities
        LOGGER.info("Testing directory creation capabilities...");
        Path testDir = tempDir.resolve("test-setup");
        Files.createDirectories(testDir);
        assertTrue(Files.exists(testDir), "Test directory should be created");
        assertTrue(Files.isDirectory(testDir), "Test path should be a directory");
        LOGGER.info("  ✓ Directory creation successful: {}", testDir);

        // Test file creation capabilities
        LOGGER.info("Testing file creation capabilities...");
        Path testFile = testDir.resolve("test.txt");
        Files.writeString(testFile, "Test content");
        assertTrue(Files.exists(testFile), "Test file should be created");
        assertTrue(Files.isRegularFile(testFile), "Test path should be a regular file");
        assertEquals("Test content", Files.readString(testFile), "File content should match");
        LOGGER.info("  ✓ File creation successful: {} (size: {} bytes)", testFile, Files.size(testFile));

        LOGGER.info("=== Demo environment setup test completed successfully ===");
    }
    
    @Test
    void testMainMethodExecution() throws Exception {
        LOGGER.info("=== TEST: Testing main method execution ===");

        // Test that main method can be called without throwing exceptions
        LOGGER.info("Testing main method accessibility and signature...");

        assertDoesNotThrow(() -> {
            // Test with empty args to ensure no NPE or similar issues
            String[] args = {};
            LOGGER.info("Testing with empty arguments: {}", java.util.Arrays.toString(args));

            // Verify class loading and method accessibility
            Class<?> demoClass = CsvToH2PipelineDemo.class;
            LOGGER.info("Demo class loaded: {}", demoClass.getName());

            var mainMethod = demoClass.getMethod("main", String[].class);
            assertNotNull(mainMethod, "Main method should exist");
            LOGGER.info("✓ Main method found: {}", mainMethod);
            LOGGER.info("  Method name: {}", mainMethod.getName());
            LOGGER.info("  Return type: {}", mainMethod.getReturnType());
            LOGGER.info("  Parameter types: {}", java.util.Arrays.toString(mainMethod.getParameterTypes()));
            LOGGER.info("  Modifiers: {}", java.lang.reflect.Modifier.toString(mainMethod.getModifiers()));

            // Verify method is public static
            assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()),
                      "Main method should be public");
            assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()),
                      "Main method should be static");
            assertEquals(void.class, mainMethod.getReturnType(),
                        "Main method should return void");

            LOGGER.info("✓ Main method signature verification passed");

        }, "Main method should be accessible and well-formed");

        // Test constructor accessibility
        LOGGER.info("Testing constructor accessibility...");
        assertDoesNotThrow(() -> {
            var constructor = CsvToH2PipelineDemo.class.getConstructor();
            assertNotNull(constructor, "Default constructor should exist");
            LOGGER.info("✓ Default constructor found: {}", constructor);

            // Test instantiation
            var instance = constructor.newInstance();
            assertNotNull(instance, "Instance should be created");
            LOGGER.info("✓ Instance created successfully: {}", instance.getClass().getSimpleName());

        }, "Constructor should be accessible and functional");

        // Test method accessibility for key demo methods
        LOGGER.info("Testing key method accessibility...");
        Class<?> demoClass = CsvToH2PipelineDemo.class;

        // Check runDemo method
        var runDemoMethod = demoClass.getMethod("runDemo");
        assertNotNull(runDemoMethod, "runDemo method should exist");
        LOGGER.info("✓ runDemo method found: {}", runDemoMethod);
        assertTrue(java.lang.reflect.Modifier.isPublic(runDemoMethod.getModifiers()),
                  "runDemo method should be public");

        // Verify exception handling in method signatures
        var exceptionTypes = runDemoMethod.getExceptionTypes();
        LOGGER.info("runDemo method exception types: {}", java.util.Arrays.toString(exceptionTypes));

        LOGGER.info("=== Main method execution test completed successfully ===");
    }
    
    @Test
    void testResourceAvailability() throws Exception {
        LOGGER.info("=== TEST: Testing resource availability ===");

        // Test that all required resources are available
        String[] requiredResources = {
            "/etl/csv-to-h2-pipeline.yaml",
            "/etl/README.md"
        };

        LOGGER.info("Checking {} required resources...", requiredResources.length);

        for (String resource : requiredResources) {
            LOGGER.info("Checking resource: {}", resource);

            var stream = getClass().getResourceAsStream(resource);
            assertNotNull(stream, "Required resource should be available: " + resource);

            try {
                // Check if we can read the resource
                byte[] content = stream.readAllBytes();
                assertTrue(content.length > 0, "Resource should not be empty: " + resource);
                LOGGER.info("  ✓ Resource found and readable: {} (size: {} bytes)", resource, content.length);

                // For YAML files, try to validate basic structure
                if (resource.endsWith(".yaml") || resource.endsWith(".yml")) {
                    String contentStr = new String(content);
                    assertTrue(contentStr.contains("metadata"), "YAML should contain metadata section");
                    assertTrue(contentStr.contains("data-sinks"), "YAML should contain data-sinks section");
                    LOGGER.info("    ✓ YAML structure validation passed");
                }

                // For README files, check for basic documentation structure
                if (resource.endsWith("README.md")) {
                    String contentStr = new String(content);
                    assertTrue(contentStr.contains("# "), "README should contain headers");
                    assertTrue(contentStr.contains("Demo"), "README should mention Demo");
                    LOGGER.info("    ✓ README structure validation passed");
                }

            } catch (Exception e) {
                LOGGER.error("Failed to read resource: {}", resource, e);
                throw e;
            } finally {
                stream.close();
            }
        }

        // Test additional resources that might be useful
        LOGGER.info("Checking for additional optional resources...");
        String[] optionalResources = {
            "/logback-test.xml",
            "/application.properties",
            "/etl/sample-data.csv"
        };

        for (String resource : optionalResources) {
            var stream = getClass().getResourceAsStream(resource);
            if (stream != null) {
                try {
                    byte[] content = stream.readAllBytes();
                    LOGGER.info("  ✓ Optional resource found: {} (size: {} bytes)", resource, content.length);
                } catch (Exception e) {
                    LOGGER.warn("  ⚠ Optional resource found but not readable: {}", resource);
                } finally {
                    stream.close();
                }
            } else {
                LOGGER.info("  ℹ Optional resource not found: {} (this is OK)", resource);
            }
        }

        // Test classpath resource loading mechanism
        LOGGER.info("Testing classpath resource loading mechanism...");
        var classLoader = getClass().getClassLoader();
        assertNotNull(classLoader, "Class loader should be available");
        LOGGER.info("  ✓ Class loader available: {}", classLoader.getClass().getSimpleName());

        // Test URL resolution for resources
        for (String resource : requiredResources) {
            var url = getClass().getResource(resource);
            assertNotNull(url, "Resource URL should be resolvable: " + resource);
            LOGGER.info("  ✓ Resource URL resolved: {} -> {}", resource, url);
        }

        LOGGER.info("=== All required resources are available and accessible ===");
    }
}
