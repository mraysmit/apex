package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

/**
 * Test class for YAML configuration functionality.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for YAML configuration functionality.
 */
class YamlConfigurationTest {
    
    private YamlConfigurationLoader loader;
    private YamlRuleFactory factory;
    private YamlRulesEngineService service;
    
    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
        factory = new YamlRuleFactory();
        service = new YamlRulesEngineService();
    }
    
    @Test
    void testLoadSimpleYamlConfiguration() throws YamlConfigurationException {
        String yamlContent = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
              description: "Test configuration for unit tests"
            
            categories:
              - name: "test"
                display-name: "Test Category"
                priority: 10
                enabled: true
            
            rules:
              - id: "test-rule-1"
                name: "Test Rule 1"
                description: "First test rule"
                category: "test"
                condition: "#value != null"
                message: "Value is required"
                priority: 10
                enabled: true
              
              - id: "test-rule-2"
                name: "Test Rule 2"
                description: "Second test rule"
                category: "test"
                condition: "#value > 0"
                message: "Value must be positive"
                priority: 20
                enabled: true
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlContent);
        
        assertNotNull(config);
        assertNotNull(config.getMetadata());
        assertEquals("Test Configuration", config.getMetadata().getName());
        assertEquals("1.0.0", config.getMetadata().getVersion());
        
        assertNotNull(config.getCategories());
        assertEquals(1, config.getCategories().size());
        assertEquals("test", config.getCategories().get(0).getName());
        
        assertNotNull(config.getRules());
        assertEquals(2, config.getRules().size());
        assertEquals("test-rule-1", config.getRules().get(0).getId());
        assertEquals("test-rule-2", config.getRules().get(1).getId());
    }
    
    @Test
    void testCreateRulesEngineFromYaml() throws YamlConfigurationException {
        String yamlContent = """
            metadata:
              name: "Engine Test Configuration"
              version: "1.0.0"
            
            categories:
              - name: "validation"
                priority: 10
                enabled: true
            
            rules:
              - id: "required-field"
                name: "Required Field Check"
                category: "validation"
                condition: "#field != null && #field.trim().length() > 0"
                message: "Field is required"
                priority: 10
                enabled: true
            
            rule-groups:
              - id: "basic-validation"
                name: "Basic Validation Group"
                category: "validation"
                priority: 10
                enabled: true
                rule-ids:
                  - "required-field"
            """;
        
        RulesEngine engine = service.createRulesEngineFromString(yamlContent);
        
        assertNotNull(engine);
        assertNotNull(engine.getConfiguration());
        
        List<Rule> rules = engine.getConfiguration().getAllRules();
        assertEquals(1, rules.size());
        assertEquals("required-field", rules.get(0).getId());
        assertEquals("Required Field Check", rules.get(0).getName());
        
        List<RuleGroup> groups = engine.getConfiguration().getAllRuleGroups();
        assertEquals(1, groups.size());
        assertEquals("basic-validation", groups.get(0).getId());
    }
    
    @Test
    void testRuleFactory() throws YamlConfigurationException {
        String yamlContent = """
            rules:
              - id: "test-rule"
                name: "Test Rule"
                description: "A test rule"
                category: "test"
                condition: "#amount > 1000"
                message: "Amount must be greater than 1000"
                priority: 15
                enabled: true
                tags: ["amount", "validation"]
            """;
        
        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        RulesEngineConfiguration config = factory.createRulesEngineConfiguration(yamlConfig);
        
        assertNotNull(config);
        
        List<Rule> rules = config.getAllRules();
        assertEquals(1, rules.size());
        
        Rule rule = rules.get(0);
        assertEquals("test-rule", rule.getId());
        assertEquals("Test Rule", rule.getName());
        assertEquals("A test rule", rule.getDescription());
        assertEquals("#amount > 1000", rule.getCondition());
        assertEquals("Amount must be greater than 1000", rule.getMessage());
        assertEquals(15, rule.getPriority());
    }
    
    @Test
    void testInvalidYamlConfiguration() {
        String invalidYaml = """
            rules:
              - id: ""  # Invalid: empty ID
                name: "Invalid Rule"
                condition: "#value != null"
                message: "Test message"
            """;
        
        assertThrows(YamlConfigurationException.class, () -> {
            loader.fromYamlString(invalidYaml);
        });
    }
    
    @Test
    void testDisabledRulesNotLoaded() throws YamlConfigurationException {
        String yamlContent = """
            rules:
              - id: "enabled-rule"
                name: "Enabled Rule"
                category: "test"
                condition: "#value != null"
                message: "Value required"
                enabled: true
              
              - id: "disabled-rule"
                name: "Disabled Rule"
                category: "test"
                condition: "#value > 0"
                message: "Value must be positive"
                enabled: false
            """;
        
        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        RulesEngineConfiguration config = factory.createRulesEngineConfiguration(yamlConfig);
        
        List<Rule> rules = config.getAllRules();
        assertEquals(1, rules.size());
        assertEquals("enabled-rule", rules.get(0).getId());
    }
    
    @Test
    void testYamlToStringConversion() throws YamlConfigurationException {
        YamlRuleConfiguration config = new YamlRuleConfiguration();
        
        YamlRuleConfiguration.ConfigurationMetadata metadata = new YamlRuleConfiguration.ConfigurationMetadata();
        metadata.setName("Test Config");
        metadata.setVersion("1.0");
        config.setMetadata(metadata);
        
        String yamlString = loader.toYamlString(config);
        
        assertNotNull(yamlString);
        assertTrue(yamlString.contains("name: Test Config"));
        // YAML mapper with MINIMIZE_QUOTES may generate version: 1.0 or version: '1.0'
        assertTrue(yamlString.contains("version: 1.0") || yamlString.contains("version: '1.0'"));
    }
}
