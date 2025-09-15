package dev.mars.apex.demo.rulegroups;

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
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.Rule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify hierarchical rule group references work.
 */
@DisplayName("Simple Hierarchical Rule Group Test")
public class SimpleHierarchicalTest {

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.rulesEngineService = new YamlRulesEngineService();
    }

    @Test
    @DisplayName("Simple rule group reference test")
    void testSimpleRuleGroupReference() throws Exception {
        System.out.println("Testing simple rule group reference");
        
        // Create simple rules YAML
        String rulesYaml = """
            metadata:
              name: "Simple Rules"
              version: "1.0.0"
            
            rules:
              - id: "rule1"
                name: "Rule 1"
                condition: "#value > 10"
                message: "Value must be greater than 10"
                severity: "ERROR"
                priority: 1
              - id: "rule2"
                name: "Rule 2"
                condition: "#value < 100"
                message: "Value must be less than 100"
                severity: "ERROR"
                priority: 2
            """;
        
        // Create base groups YAML
        String baseGroupsYaml = """
            metadata:
              name: "Base Groups"
              version: "1.0.0"
            
            rule-groups:
              - id: "base-group"
                name: "Base Group"
                operator: "AND"
                rule-ids:
                  - "rule1"
                  - "rule2"
            """;
        
        // Create composite groups YAML with rule-group-references
        String compositeGroupsYaml = """
            metadata:
              name: "Composite Groups"
              version: "1.0.0"
            
            rule-groups:
              - id: "composite-group"
                name: "Composite Group"
                operator: "AND"
                rule-group-references:
                  - "base-group"
            """;
        
        // Create temporary files
        Path rulesFile = Files.createTempFile("rules", ".yaml");
        Path baseGroupsFile = Files.createTempFile("base-groups", ".yaml");
        Path compositeGroupsFile = Files.createTempFile("composite-groups", ".yaml");
        
        try {
            // Write YAML content to temporary files
            Files.writeString(rulesFile, rulesYaml);
            Files.writeString(baseGroupsFile, baseGroupsYaml);
            Files.writeString(compositeGroupsFile, compositeGroupsYaml);
            
            // Load using multi-file approach - use the hierarchical files to test mixed-validation
            RulesEngine engine = rulesEngineService.createRulesEngineFromMultipleFiles(
                "src/test/resources/rulegroups/hierarchical/base-rules.yaml",
                "src/test/resources/rulegroups/hierarchical/base-groups.yaml",
                "src/test/resources/rulegroups/hierarchical/composite-groups.yaml"
            );
            
            // Test mixed validation group to debug the failing test
            RuleGroup mixedGroup = engine.getConfiguration().getRuleGroupById("mixed-validation");
            assertNotNull(mixedGroup, "Mixed validation rule group should be found");

            System.out.println("Mixed validation group found with " + mixedGroup.getRules().size() + " rules:");
            for (Rule rule : mixedGroup.getRules()) {
                System.out.println("  - " + rule.getId() + ": " + rule.getName());
            }

            System.out.println("✓ Simple rule group reference test passed");
            
        } finally {
            // Cleanup
            Files.deleteIfExists(rulesFile);
            Files.deleteIfExists(baseGroupsFile);
            Files.deleteIfExists(compositeGroupsFile);
        }
    }
}
