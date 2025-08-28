package dev.mars.apex.core.config.yaml;

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


import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-component validation and error handling tests for YAML configuration.
 * 
 * Tests focus on:
 * - Cross-component validation between rules, groups, categories, and data sources
 * - Error handling for malformed configurations and missing dependencies
 * - Configuration integrity validation across all components
 * - Edge cases and boundary conditions
 * - Integration validation scenarios
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class ConfigurationValidationTest {

    private YamlConfigurationLoader loader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
    }

    // ========================================
    // Cross-Component Reference Validation Tests
    // ========================================

    @Test
    @DisplayName("Should validate rule references in rule groups")
    void testRuleGroupRuleReferences() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing invalid rule references in groups");
        
        Path configFile = tempDir.resolve("invalid-rule-group-refs.yaml");
        String invalidConfig = """
            metadata:
              name: "Invalid Rule Group References"
              type: "rule-configuration"
            
            rules:
              - id: "existing-rule-1"
                name: "Existing Rule 1"
                condition: "true"
                message: "This rule exists"
              
              - id: "existing-rule-2"
                name: "Existing Rule 2"
                condition: "true"
                message: "This rule also exists"
            
            rule-groups:
              - id: "invalid-group"
                name: "Invalid Group"
                rule-ids:
                  - "existing-rule-1"
                  - "non-existent-rule"  # This rule doesn't exist
                  - "another-missing-rule"  # This rule also doesn't exist
            """;
        Files.writeString(configFile, invalidConfig);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(configFile.toString());
        }, "Should throw exception for non-existent rule references");
        
        assertTrue(exception.getMessage().contains("Rule reference not found"), 
                  "Exception message should indicate missing rule references");
        assertTrue(exception.getMessage().contains("non-existent-rule") || 
                  exception.getMessage().contains("Rule 'non-existent-rule' referenced"), 
                  "Exception should mention the specific missing rule");
    }

    @Test
    @DisplayName("Should handle data source references in rules (currently not validated)")
    void testRuleDataSourceReferences() throws Exception {
        // Note: This test is updated to reflect that YamlRule doesn't currently have direct data source references

        Path configFile = tempDir.resolve("datasource-refs.yaml");
        String config = """
            metadata:
              name: "Data Source References"

            data-sources:
              - name: "existing-db"
                type: "postgresql"
                enabled: true
                connection:
                  host: "localhost"
                  port: 5432
                  database: "testdb"

            rules:
              - id: "rule-with-metadata-datasource"
                name: "Rule with Metadata Data Source"
                condition: "true"
                message: "Uses data source via metadata"
                metadata:
                  data-source: "existing-db"
            """;
        Files.writeString(configFile, config);

        // This should not throw an exception since data source validation is not currently implemented
        assertDoesNotThrow(() -> {
            YamlRuleConfiguration loadedConfig = loader.loadFromFile(configFile.toString());
            assertNotNull(loadedConfig, "Configuration should be loaded");
            assertEquals(1, loadedConfig.getRules().size(), "Should have 1 rule");
            assertEquals(1, loadedConfig.getDataSources().size(), "Should have 1 data source");
        }, "Should handle configuration with data source references in metadata");
    }

    @Test
    @DisplayName("Should validate rule chain rule references")
    void testRuleChainRuleReferences() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing invalid rule references in rule chains");
        
        Path configFile = tempDir.resolve("invalid-chain-refs.yaml");
        String invalidConfig = """
            metadata:
              name: "Invalid Rule Chain References"
              type: "rule-configuration"
            
            rules:
              - id: "trigger-rule"
                name: "Trigger Rule"
                condition: "#amount > 1000"
                message: "High amount trigger"
            
            rule-chains:
              - id: "invalid-chain"
                name: "Invalid Chain"
                pattern: "conditional-chaining"
                configuration:
                  trigger-rule:
                    rule-id: "non-existent-trigger"  # This rule doesn't exist
                  conditional-rules:
                    on-trigger:
                      - rule-id: "missing-rule"  # This rule also doesn't exist
            """;
        Files.writeString(configFile, invalidConfig);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(configFile.toString());
        }, "Should throw exception for non-existent rule references in chains");
        
        assertTrue(exception.getMessage().contains("Rule reference not found") || 
                  exception.getMessage().contains("non-existent-trigger"), 
                  "Exception message should indicate missing rule reference in chain");
    }

    // ========================================
    // Configuration Integrity Validation Tests
    // ========================================

    @Test
    @DisplayName("Should validate duplicate rule IDs")
    void testDuplicateRuleIds() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing duplicate rule IDs");
        
        Path configFile = tempDir.resolve("duplicate-rule-ids.yaml");
        String invalidConfig = """
            metadata:
              name: "Duplicate Rule IDs"
              type: "rule-configuration"
            
            rules:
              - id: "duplicate-rule"
                name: "First Rule"
                condition: "true"
                message: "First rule with this ID"
              
              - id: "unique-rule"
                name: "Unique Rule"
                condition: "true"
                message: "This rule has a unique ID"
              
              - id: "duplicate-rule"  # Duplicate ID
                name: "Second Rule"
                condition: "false"
                message: "Second rule with duplicate ID"
            """;
        Files.writeString(configFile, invalidConfig);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(configFile.toString());
        }, "Should throw exception for duplicate rule IDs");
        
        assertTrue(exception.getMessage().contains("Duplicate rule ID") || 
                  exception.getMessage().contains("duplicate-rule"), 
                  "Exception message should indicate duplicate rule ID");
    }

    @Test
    @DisplayName("Should validate duplicate data source names")
    void testDuplicateDataSourceNames() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing duplicate data source names");
        
        Path configFile = tempDir.resolve("duplicate-datasource-names.yaml");
        String invalidConfig = """
            metadata:
              name: "Duplicate Data Source Names"
              type: "rule-configuration"
            
            data-sources:
              - name: "main-db"
                type: "postgresql"
                enabled: true
                connection:
                  host: "localhost"
                  port: 5432
              
              - name: "cache-db"
                type: "redis"
                enabled: true
                connection:
                  host: "localhost"
                  port: 6379
              
              - name: "main-db"  # Duplicate name
                type: "mysql"
                enabled: true
                connection:
                  host: "localhost"
                  port: 3306
            """;
        Files.writeString(configFile, invalidConfig);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(configFile.toString());
        }, "Should throw exception for duplicate data source names");
        
        assertTrue(exception.getMessage().contains("Duplicate data source name") || 
                  exception.getMessage().contains("main-db"), 
                  "Exception message should indicate duplicate data source name");
    }

    @Test
    @DisplayName("Should validate circular dependencies in rule chains")
    void testCircularDependenciesInRuleChains() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing circular dependencies in rule chains");
        
        Path configFile = tempDir.resolve("circular-dependencies.yaml");
        String invalidConfig = """
            metadata:
              name: "Circular Dependencies"
              type: "rule-configuration"
            
            rules:
              - id: "rule-a"
                name: "Rule A"
                condition: "true"
                message: "Rule A"
              
              - id: "rule-b"
                name: "Rule B"
                condition: "true"
                message: "Rule B"
              
              - id: "rule-c"
                name: "Rule C"
                condition: "true"
                message: "Rule C"
            
            rule-chains:
              - id: "circular-chain"
                name: "Circular Chain"
                pattern: "sequential-dependency"
                configuration:
                  stages:
                    - stage: 1
                      name: "Stage A"
                      rule-id: "rule-a"
                      depends-on: ["3"]  # Depends on stage 3
                    - stage: 2
                      name: "Stage B"
                      rule-id: "rule-b"
                      depends-on: ["1"]  # Depends on stage 1
                    - stage: 3
                      name: "Stage C"
                      rule-id: "rule-c"
                      depends-on: ["2"]  # Depends on stage 2, creating a cycle: 1->3->2->1
            """;
        Files.writeString(configFile, invalidConfig);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(configFile.toString());
        }, "Should throw exception for circular dependencies");
        
        assertTrue(exception.getMessage().contains("Circular dependency") || 
                  exception.getMessage().contains("cycle detected"), 
                  "Exception message should indicate circular dependency");
    }

    // ========================================
    // Data Source Configuration Validation Tests
    // ========================================

    @Test
    @DisplayName("Should validate required connection properties for database data sources")
    void testDatabaseConnectionValidation() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing missing database connection properties");
        
        Path configFile = tempDir.resolve("invalid-db-connection.yaml");
        String invalidConfig = """
            metadata:
              name: "Invalid Database Connection"
              type: "rule-configuration"
            
            data-sources:
              - name: "incomplete-db"
                type: "postgresql"
                enabled: true
                connection:
                  host: "localhost"
                  # Missing required port, database, username, password
            """;
        Files.writeString(configFile, invalidConfig);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(configFile.toString());
        }, "Should throw exception for incomplete database connection");
        
        assertTrue(exception.getMessage().contains("Missing required connection property") || 
                  exception.getMessage().contains("port") || 
                  exception.getMessage().contains("database"), 
                  "Exception message should indicate missing connection properties");
    }

    @Test
    @DisplayName("Should validate REST API endpoint configuration")
    void testRestApiEndpointValidation() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing invalid REST API configuration");
        
        Path configFile = tempDir.resolve("invalid-rest-api.yaml");
        String invalidConfig = """
            metadata:
              name: "Invalid REST API"
              type: "rule-configuration"
            
            data-sources:
              - name: "invalid-api"
                type: "rest-api"
                enabled: true
                connection:
                  # Missing required base-url
                  timeout: 30000
                endpoints:
                  getUser: "/users/{id}"
            """;
        Files.writeString(configFile, invalidConfig);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(configFile.toString());
        }, "Should throw exception for missing base URL in REST API");
        
        assertTrue(exception.getMessage().contains("Missing required property 'base-url'") || 
                  exception.getMessage().contains("base-url is required"), 
                  "Exception message should indicate missing base-url");
    }

    @Test
    @DisplayName("Should validate file system data source configuration")
    void testFileSystemDataSourceValidation() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing invalid file system configuration");
        
        Path configFile = tempDir.resolve("invalid-file-system.yaml");
        String invalidConfig = """
            metadata:
              name: "Invalid File System"
              type: "rule-configuration"
            
            data-sources:
              - name: "invalid-files"
                type: "file-system"
                enabled: true
                connection:
                  # Missing required base-path
                  file-pattern: "*.csv"
                file-format:
                  type: "csv"
                  delimiter: ","
            """;
        Files.writeString(configFile, invalidConfig);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(configFile.toString());
        }, "Should throw exception for missing base path in file system data source");
        
        assertTrue(exception.getMessage().contains("Missing required property 'base-path'") || 
                  exception.getMessage().contains("base-path is required"), 
                  "Exception message should indicate missing base-path");
    }

    // ========================================
    // Rule Chain Pattern Validation Tests
    // ========================================

    @Test
    @DisplayName("Should validate conditional-chaining pattern configuration")
    void testConditionalChainingPatternValidation() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing invalid conditional-chaining pattern");
        
        Path configFile = tempDir.resolve("invalid-conditional-chaining.yaml");
        String invalidConfig = """
            metadata:
              name: "Invalid Conditional Chaining"
              type: "rule-configuration"
            
            rule-chains:
              - id: "invalid-conditional"
                name: "Invalid Conditional Chain"
                pattern: "conditional-chaining"
                configuration:
                  # Missing required trigger-rule
                  conditional-rules:
                    on-trigger:
                      - condition: "true"
                        message: "Trigger action"
            """;
        Files.writeString(configFile, invalidConfig);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(configFile.toString());
        }, "Should throw exception for missing trigger-rule in conditional-chaining");
        
        assertTrue(exception.getMessage().contains("Missing required 'trigger-rule'") || 
                  exception.getMessage().contains("trigger-rule is required"), 
                  "Exception message should indicate missing trigger-rule");
    }

    @Test
    @DisplayName("Should validate sequential-dependency pattern configuration")
    void testSequentialDependencyPatternValidation() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing invalid sequential-dependency pattern");
        
        Path configFile = tempDir.resolve("invalid-sequential-dependency.yaml");
        String invalidConfig = """
            metadata:
              name: "Invalid Sequential Dependency"
              type: "rule-configuration"
            
            rule-chains:
              - id: "invalid-sequential"
                name: "Invalid Sequential Chain"
                pattern: "sequential-dependency"
                configuration:
                  # Missing required stages
                  metadata:
                    description: "Sequential processing"
            """;
        Files.writeString(configFile, invalidConfig);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(configFile.toString());
        }, "Should throw exception for missing stages in sequential-dependency");
        
        assertTrue(exception.getMessage().contains("Missing required 'stages'") || 
                  exception.getMessage().contains("stages is required"), 
                  "Exception message should indicate missing stages");
    }

    @Test
    @DisplayName("Should validate accumulative-chaining pattern configuration")
    void testAccumulativeChainingPatternValidation() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing invalid accumulative-chaining pattern");
        
        Path configFile = tempDir.resolve("invalid-accumulative-chaining.yaml");
        String invalidConfig = """
            metadata:
              name: "Invalid Accumulative Chaining"
              type: "rule-configuration"
            
            rule-chains:
              - id: "invalid-accumulative"
                name: "Invalid Accumulative Chain"
                pattern: "accumulative-chaining"
                configuration:
                  # Missing required accumulator-variable and accumulation-rules
                  initial-value: 0
            """;
        Files.writeString(configFile, invalidConfig);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(configFile.toString());
        }, "Should throw exception for missing accumulator configuration");
        
        assertTrue(exception.getMessage().contains("Missing required 'accumulator-variable'") || 
                  exception.getMessage().contains("accumulation-rules"), 
                  "Exception message should indicate missing accumulator configuration");
    }

    // ========================================
    // Edge Cases and Boundary Conditions Tests
    // ========================================

    @Test
    @DisplayName("Should handle extremely large configuration files")
    void testLargeConfigurationFile() throws Exception {
        Path largeConfigFile = tempDir.resolve("large-config.yaml");
        StringBuilder largeConfig = new StringBuilder();
        
        largeConfig.append("""
            metadata:
              name: "Large Configuration"
              type: "rule-configuration"
              description: "Configuration with many rules for performance testing"
            
            rules:
            """);
        
        // Generate 1000 rules
        for (int i = 1; i <= 1000; i++) {
            largeConfig.append(String.format("""
              - id: "rule-%d"
                name: "Rule %d"
                condition: "#value > %d"
                message: "Rule %d triggered"
                enabled: true
                priority: %d
            """, i, i, i * 10, i, i % 100));
        }
        
        largeConfig.append("""
            
            rule-groups:
              - id: "large-group"
                name: "Large Group"
                rule-ids:
            """);
        
        // Add all rules to the group
        for (int i = 1; i <= 1000; i++) {
            largeConfig.append(String.format("          - \"rule-%d\"\n", i));
        }
        
        Files.writeString(largeConfigFile, largeConfig.toString());

        // This should not throw an exception, just test performance
        assertDoesNotThrow(() -> {
            YamlRuleConfiguration config = loader.loadFromFile(largeConfigFile.toString());
            assertNotNull(config, "Large configuration should be loaded");
            assertEquals(1000, config.getRules().size(), "Should have 1000 rules");
            assertEquals(1, config.getRuleGroups().size(), "Should have 1 rule group");
            assertEquals(1000, config.getRuleGroups().get(0).getRuleIds().size(), "Group should contain all 1000 rules");
        }, "Should handle large configuration files without errors");
    }

    @Test
    @DisplayName("Should handle configuration with special characters and Unicode")
    void testSpecialCharactersAndUnicode() throws Exception {
        Path unicodeConfigFile = tempDir.resolve("unicode-config.yaml");
        String unicodeConfig = """
            metadata:
              name: "Unicode Configuration 测试配置"
              description: "Configuration with special characters and Unicode"
              author: "测试用户 (Test User)"

            rules:
              - id: "unicode-rule-测试"
                name: "Unicode Rule 规则"
                condition: "#message.contains('测试') || #message.contains('🚀')"
                message: "Unicode message: 测试成功! 🎉"
                enabled: true

              - id: "emoji-rule-🚀"
                name: "Emoji Rule 🎯"
                condition: "#status == '成功'"
                message: "Emoji test: 🚀🎉🎯💯"
                enabled: true

            rule-groups:
              - id: "unicode-group-组"
                name: "Unicode Group 测试组"
                description: "Group with Unicode: 中文测试 🌟"
                rule-ids:
                  - "unicode-rule-测试"
                  - "emoji-rule-🚀"
            """;
        Files.writeString(unicodeConfigFile, unicodeConfig);

        assertDoesNotThrow(() -> {
            YamlRuleConfiguration config = loader.loadFromFile(unicodeConfigFile.toString());
            assertNotNull(config, "Unicode configuration should be loaded");
            assertEquals("Unicode Configuration 测试配置", config.getMetadata().getName(), "Unicode name should be preserved");
            assertEquals(2, config.getRules().size(), "Should have 2 rules");
            assertEquals("unicode-rule-测试", config.getRules().get(0).getId(), "Unicode rule ID should be preserved");
            assertEquals("Emoji Rule 🎯", config.getRules().get(1).getName(), "Emoji in rule name should be preserved");
            assertTrue(config.getRules().get(1).getMessage().contains("🚀🎉🎯💯"), "Emojis in message should be preserved");
        }, "Should handle Unicode and special characters correctly");
    }

    @Test
    @DisplayName("Should validate configuration with empty collections")
    void testEmptyCollectionsValidation() throws Exception {
        Path emptyConfigFile = tempDir.resolve("empty-collections.yaml");
        String emptyConfig = """
            metadata:
              name: "Empty Collections Configuration"
              type: "rule-configuration"
              description: "Configuration with empty collections"
            
            rules: []
            rule-groups: []
            categories: []
            data-sources: []
            enrichments: []
            transformations: []
            rule-chains: []
            """;
        Files.writeString(emptyConfigFile, emptyConfig);

        assertDoesNotThrow(() -> {
            YamlRuleConfiguration config = loader.loadFromFile(emptyConfigFile.toString());
            assertNotNull(config, "Configuration with empty collections should be loaded");
            assertNotNull(config.getRules(), "Rules should not be null");
            assertTrue(config.getRules().isEmpty(), "Rules should be empty");
            assertNotNull(config.getRuleGroups(), "Rule groups should not be null");
            assertTrue(config.getRuleGroups().isEmpty(), "Rule groups should be empty");
        }, "Should handle empty collections gracefully");
    }

    @Test
    @DisplayName("Should validate configuration with null values")
    void testNullValuesValidation() throws Exception {
        System.out.println("TEST: Triggering intentional error - testing null values in required fields");
        
        Path nullConfigFile = tempDir.resolve("null-values.yaml");
        String nullConfig = """
            metadata:
              name: null  # Null name should cause validation error
              type: "rule-configuration"
            
            rules:
              - id: "test-rule"
                name: "Test Rule"
                condition: null  # Null condition should cause validation error
                message: "Test message"
            """;
        Files.writeString(nullConfigFile, nullConfig);

        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile(nullConfigFile.toString());
        }, "Should throw exception for null values in required fields");
        
        assertTrue(exception.getMessage().contains("null") || 
                  exception.getMessage().contains("required"), 
                  "Exception message should indicate null value in required field");
    }
}
