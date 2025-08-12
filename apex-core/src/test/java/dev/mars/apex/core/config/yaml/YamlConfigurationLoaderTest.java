package dev.mars.apex.core.config.yaml;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API-specific integration tests for YamlConfigurationLoader.
 * 
 * Tests focus on:
 * - Actual API methods (loadFromFile, loadFromStream, loadFromClasspath, etc.)
 * - Integration with real YAML configurations
 * - Error handling and validation scenarios
 * - Configuration conversion and validation
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class YamlConfigurationLoaderTest {

    private YamlConfigurationLoader loader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
    }

    // ========================================
    // Constructor and Basic API Tests
    // ========================================

    @Test
    @DisplayName("Should create YamlConfigurationLoader successfully")
    void testConstructor() {
        YamlConfigurationLoader testLoader = new YamlConfigurationLoader();
        assertNotNull(testLoader, "Loader should be created successfully");
    }

    // ========================================
    // File Loading Tests (loadFromFile)
    // ========================================

    @Test
    @DisplayName("Should load valid YAML configuration from file path")
    void testLoadFromFilePath() throws Exception {
        // Create a valid YAML configuration file
        Path yamlFile = tempDir.resolve("valid-config.yaml");
        String validYaml = createValidRuleConfigurationYaml();
        Files.writeString(yamlFile, validYaml);

        // Load using file path
        YamlRuleConfiguration config = loader.loadFromFile(yamlFile.toString());

        assertNotNull(config, "Configuration should be loaded");
        assertNotNull(config.getMetadata(), "Metadata should be present");
        assertEquals("Test Configuration", config.getMetadata().getName(), "Name should match");
        // Note: ConfigurationMetadata doesn't have getType() method in actual API
    }

    @Test
    @DisplayName("Should load valid YAML configuration from File object")
    void testLoadFromFileObject() throws Exception {
        // Create a valid YAML configuration file
        Path yamlFile = tempDir.resolve("valid-config.yaml");
        String validYaml = createValidRuleConfigurationYaml();
        Files.writeString(yamlFile, validYaml);

        // Load using File object
        YamlRuleConfiguration config = loader.loadFromFile(yamlFile.toFile());

        assertNotNull(config, "Configuration should be loaded");
        assertNotNull(config.getMetadata(), "Metadata should be present");
        assertEquals("Test Configuration", config.getMetadata().getName(), "Name should match");
    }

    @Test
    @DisplayName("Should throw YamlConfigurationException for non-existent file")
    void testLoadFromNonExistentFile() {
        System.out.println("TEST: Triggering intentional error - testing non-existent file loading");
        
        String nonExistentFile = tempDir.resolve("non-existent.yaml").toString();
        
        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(nonExistentFile);
        }, "Should throw YamlConfigurationException for non-existent file");
        
        assertTrue(exception.getMessage().contains("Configuration file not found"), 
                  "Exception message should indicate file not found");
    }

    @Test
    @DisplayName("Should throw YamlConfigurationException for invalid YAML syntax")
    void testLoadFromFileWithInvalidYaml() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing invalid YAML syntax");
        
        Path invalidYamlFile = tempDir.resolve("invalid.yaml");
        String invalidYaml = """
            metadata:
              name: "Invalid YAML
              type: rule-configuration
            rules:
              - id: test-rule
                condition: [unclosed bracket
            """;
        Files.writeString(invalidYamlFile, invalidYaml);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(invalidYamlFile.toString());
        }, "Should throw YamlConfigurationException for invalid YAML");
        
        assertTrue(exception.getMessage().contains("Failed to load configuration"), 
                  "Exception message should indicate loading failure");
    }

    // ========================================
    // Stream Loading Tests (loadFromStream)
    // ========================================

    @Test
    @DisplayName("Should load configuration from InputStream")
    void testLoadFromInputStream() throws Exception {
        String validYaml = createValidRuleConfigurationYaml();

        try (InputStream inputStream = new ByteArrayInputStream(validYaml.getBytes())) {
            YamlRuleConfiguration config = loader.loadFromStream(inputStream);
            
            assertNotNull(config, "Configuration should be loaded from stream");
            assertNotNull(config.getMetadata(), "Metadata should be present");
            assertEquals("Test Configuration", config.getMetadata().getName(), "Name should match stream content");
        }
    }

    @Test
    @DisplayName("Should handle empty InputStream")
    void testLoadFromEmptyInputStream() {
        System.out.println("TEST: Triggering intentional error - testing empty InputStream");
        
        try (InputStream emptyStream = new ByteArrayInputStream(new byte[0])) {
            YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
                loader.loadFromStream(emptyStream);
            }, "Should throw exception for empty stream");
            
            assertTrue(exception.getMessage().contains("Failed to load configuration"), 
                      "Exception message should indicate loading failure");
        } catch (IOException e) {
            fail("IOException should not be thrown during test setup: " + e.getMessage());
        }
    }

    // ========================================
    // Classpath Loading Tests (loadFromClasspath)
    // ========================================

    @Test
    @DisplayName("Should handle non-existent classpath resource")
    void testLoadFromNonExistentClasspathResource() {
        System.out.println("TEST: Triggering intentional error - testing non-existent classpath resource");
        
        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromClasspath("non-existent-resource.yaml");
        }, "Should throw exception for non-existent classpath resource");
        
        assertTrue(exception.getMessage().contains("Configuration resource not found"), 
                  "Exception message should indicate resource not found");
    }

    // ========================================
    // YAML String Conversion Tests
    // ========================================

    @Test
    @DisplayName("Should convert configuration to YAML string")
    void testToYamlString() throws Exception {
        // Create a configuration
        YamlRuleConfiguration config = createTestConfiguration();
        
        String yamlString = loader.toYamlString(config);
        
        assertNotNull(yamlString, "YAML string should not be null");
        assertFalse(yamlString.trim().isEmpty(), "YAML string should not be empty");
        assertTrue(yamlString.contains("metadata:"), "YAML should contain metadata section");
        assertTrue(yamlString.contains("name: Test Configuration"), "YAML should contain configuration name");
    }

    @Test
    @DisplayName("Should parse YAML string into configuration")
    void testFromYamlString() throws Exception {
        String validYaml = createValidRuleConfigurationYaml();
        
        YamlRuleConfiguration config = loader.fromYamlString(validYaml);
        
        assertNotNull(config, "Configuration should be parsed from YAML string");
        assertNotNull(config.getMetadata(), "Metadata should be present");
        assertEquals("Test Configuration", config.getMetadata().getName(), "Name should match");
    }

    @Test
    @DisplayName("Should handle invalid YAML string")
    void testFromInvalidYamlString() {
        System.out.println("TEST: Triggering intentional error - testing invalid YAML string parsing");
        
        String invalidYaml = "invalid: yaml: content: [unclosed";
        
        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.fromYamlString(invalidYaml);
        }, "Should throw exception for invalid YAML string");
        
        assertTrue(exception.getMessage().contains("Failed to parse YAML string"), 
                  "Exception message should indicate parsing failure");
    }

    // ========================================
    // Map Loading Tests (loadAsMap)
    // ========================================

    @Test
    @DisplayName("Should load YAML file as Map")
    void testLoadAsMap() throws Exception {
        Path yamlFile = tempDir.resolve("map-test.yaml");
        String validYaml = createValidRuleConfigurationYaml();
        Files.writeString(yamlFile, validYaml);

        Map<String, Object> yamlMap = loader.loadAsMap(yamlFile.toString());

        assertNotNull(yamlMap, "YAML map should not be null");
        assertTrue(yamlMap.containsKey("metadata"), "Map should contain metadata key");
        assertTrue(yamlMap.containsKey("rules"), "Map should contain rules key");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) yamlMap.get("metadata");
        assertEquals("Test Configuration", metadata.get("name"), "Metadata name should match");
        // Note: Type validation is handled by the loader's metadata validation
    }

    @Test
    @DisplayName("Should validate metadata when loading as Map")
    void testLoadAsMapWithInvalidMetadata() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing invalid metadata validation");
        
        Path yamlFile = tempDir.resolve("invalid-metadata.yaml");
        String invalidMetadataYaml = """
            metadata:
              name: "Missing Type"
              # Missing required 'type' field
            rules: []
            """;
        Files.writeString(yamlFile, invalidMetadataYaml);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadAsMap(yamlFile.toString());
        }, "Should throw exception for missing required metadata fields");
        
        assertTrue(exception.getMessage().contains("Missing required 'type' field"), "Exception message should indicate missing type field");
    }

    // ========================================
    // File Saving Tests (saveToFile)
    // ========================================

    @Test
    @DisplayName("Should save configuration to file")
    void testSaveToFile() throws Exception {
        YamlRuleConfiguration config = createTestConfiguration();
        Path outputFile = tempDir.resolve("saved-config.yaml");

        loader.saveToFile(config, outputFile.toString());

        assertTrue(Files.exists(outputFile), "Output file should exist");
        
        // Verify by loading it back
        YamlRuleConfiguration loadedConfig = loader.loadFromFile(outputFile.toString());
        assertNotNull(loadedConfig, "Loaded configuration should not be null");
        assertEquals(config.getMetadata().getName(), loadedConfig.getMetadata().getName(), "Saved and loaded configuration names should match");
    }

    // ========================================
    // Configuration Validation Tests
    // ========================================

    @Test
    @DisplayName("Should validate configuration with missing rule ID")
    void testValidationWithMissingRuleId() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing rule validation with missing ID");
        
        Path yamlFile = tempDir.resolve("missing-rule-id.yaml");
        String invalidRuleYaml = """
            metadata:
              name: "Invalid Rule Configuration"
              type: "rule-configuration"
            rules:
              - name: "Rule without ID"
                condition: "true"
                message: "This rule has no ID"
            """;
        Files.writeString(yamlFile, invalidRuleYaml);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(yamlFile.toString());
        }, "Should throw exception for rule without ID");
        
        assertTrue(exception.getMessage().contains("Rule ID is required"), "Exception message should indicate missing rule ID");
    }

    @Test
    @DisplayName("Should validate configuration with missing rule condition")
    void testValidationWithMissingRuleCondition() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing rule validation with missing condition");
        
        Path yamlFile = tempDir.resolve("missing-rule-condition.yaml");
        String invalidRuleYaml = """
            metadata:
              name: "Invalid Rule Configuration"
              type: "rule-configuration"
            rules:
              - id: "test-rule"
                name: "Rule without condition"
                message: "This rule has no condition"
            """;
        Files.writeString(yamlFile, invalidRuleYaml);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(yamlFile.toString());
        }, "Should throw exception for rule without condition");
        
        assertTrue(exception.getMessage().contains("Rule condition is required"), "Exception message should indicate missing rule condition");
    }

    @Test
    @DisplayName("Should validate rule group configuration")
    void testRuleGroupValidation() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing rule group validation");
        
        Path yamlFile = tempDir.resolve("invalid-rule-group.yaml");
        String invalidGroupYaml = """
            metadata:
              name: "Invalid Group Configuration"
              type: "rule-configuration"
            rule-groups:
              - id: "test-group"
                # Missing required name field
                rules:
                  - "rule-1"
            """;
        Files.writeString(yamlFile, invalidGroupYaml);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(yamlFile.toString());
        }, "Should throw exception for rule group without name");
        
        assertTrue(exception.getMessage().contains("Rule group name is required"), "Exception message should indicate missing rule group name");
    }

    // ========================================
    // Integration Tests with Complex Configurations
    // ========================================

    @Test
    @DisplayName("Should load complex configuration with all sections")
    void testLoadComplexConfiguration() throws Exception {
        Path complexFile = tempDir.resolve("complex-config.yaml");
        String complexYaml = createComplexRuleConfigurationYaml();
        Files.writeString(complexFile, complexYaml);

        YamlRuleConfiguration config = loader.loadFromFile(complexFile.toString());

        assertNotNull(config, "Complex configuration should be loaded");
        
        // Verify metadata
        assertNotNull(config.getMetadata(), "Metadata should be present");
        assertEquals("Complex Configuration", config.getMetadata().getName(), "Name should match");
        // Note: ConfigurationMetadata doesn't have getType() method in actual API
        
        // Verify rules
        assertNotNull(config.getRules(), "Rules should be present");
        assertFalse(config.getRules().isEmpty(), "Rules list should not be empty");
        
        // Verify rule groups
        assertNotNull(config.getRuleGroups(), "Rule groups should be present");
        assertFalse(config.getRuleGroups().isEmpty(), "Rule groups list should not be empty");
        
        // Verify data sources
        assertNotNull(config.getDataSources(), "Data sources should be present");
        assertFalse(config.getDataSources().isEmpty(), "Data sources list should not be empty");
    }

    // ========================================
    // Helper Methods
    // ========================================

    private String createValidRuleConfigurationYaml() {
        return """
            metadata:
              name: "Test Configuration"
              type: "rule-configuration"
              version: "1.0.0"
              description: "Test configuration for unit tests"
            
            rules:
              - id: "test-rule-1"
                name: "Test Rule 1"
                condition: "true"
                message: "Test rule executed"
                enabled: true
              
              - id: "test-rule-2"
                name: "Test Rule 2"
                condition: "#amount > 1000"
                message: "High amount detected"
                enabled: true
            
            rule-groups:
              - id: "test-group"
                name: "Test Group"
                description: "Test rule group"
                rules:
                  - "test-rule-1"
                  - "test-rule-2"
            """;
    }

    private String createComplexRuleConfigurationYaml() {
        return """
            metadata:
              name: "Complex Configuration"
              type: "rule-configuration"
              version: "2.0.0"
              description: "Complex test configuration"
              author: "Test Suite"
            
            rules:
              - id: "complex-rule-1"
                name: "Complex Rule 1"
                condition: "#amount > 1000 && #region == 'US'"
                message: "High value US transaction"
                enabled: true
                priority: 100
              
              - id: "complex-rule-2"
                name: "Complex Rule 2"
                condition: "#customerTier == 'PREMIUM'"
                message: "Premium customer detected"
                enabled: true
                priority: 200
            
            rule-groups:
              - id: "transaction-group"
                name: "Transaction Rules"
                description: "Rules for transaction processing"
                rules:
                  - "complex-rule-1"
                  - "complex-rule-2"
            
            categories:
              - name: "financial"
                display-name: "Financial Rules"
                description: "Financial processing rules"
                enabled: true
            
            data-sources:
              - name: "test-db"
                type: "postgresql"
                enabled: true
                connection:
                  host: "localhost"
                  port: 5432
                  database: "testdb"
            """;
    }

    private YamlRuleConfiguration createTestConfiguration() {
        YamlRuleConfiguration config = new YamlRuleConfiguration();

        YamlRuleConfiguration.ConfigurationMetadata metadata = new YamlRuleConfiguration.ConfigurationMetadata();
        metadata.setName("Test Configuration");
        metadata.setVersion("1.0.0");
        config.setMetadata(metadata);

        return config;
    }
}
