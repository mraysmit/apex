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
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static dev.mars.apex.demo.ColoredTestOutputExtension.*;

/**
 * Separate Files Rule Group Processing Tests.
 *
 * Tests APEX's multi-file loading capability with separate YAML files for rules and rule groups.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Separate Files Rule Group Processing Tests")
public class SeparateFilesRuleGroupProcessingTest {

    private static final Logger logger = LoggerFactory.getLogger(SeparateFilesRuleGroupProcessingTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.rulesEngineService = new YamlRulesEngineService();
    }

    @Test
    @DisplayName("Simple multi-file loading test with separate rules and rule groups")
    void testMultiFileLoading() throws Exception {
        logInfo("Testing multi-file loading with separate rules and rule groups");
        
        // Rules YAML string - would be in a separate file
        String rulesYaml = """
            metadata:
              name: "Customer Rules"
              version: "1.0.0"
            
            rules:
              - id: "age-check"
                name: "Age Validation"
                condition: "#age >= 18"
                message: "Customer must be 18 or older"
                severity: "ERROR"
                priority: 1
              - id: "email-validation"
                name: "Email Validation"
                condition: "#email != null && #email.contains('@')"
                message: "Valid email required"
                severity: "ERROR"
                priority: 2
            """;
        
        // Rule Groups YAML string - would be in a separate file, references the rules above
        String ruleGroupsYaml = """
            metadata:
              name: "Customer Validation Groups"
              version: "1.0.0"
            
            rule-groups:
              - id: "customer-validation"
                name: "Customer Validation Group"
                operator: "AND"
                rule-ids:
                  - "age-check"
                  - "email-validation"
            """;
        
        // Create temporary files
        Path rulesFile = Files.createTempFile("rules", ".yaml");
        Path ruleGroupsFile = Files.createTempFile("rule-groups", ".yaml");
        
        try {
            // Write YAML content to temporary files
            Files.writeString(rulesFile, rulesYaml);
            Files.writeString(ruleGroupsFile, ruleGroupsYaml);
            
            // Load using multi-file approach
            RulesEngine engine = rulesEngineService.createRulesEngineFromMultipleFiles(
                rulesFile.toString(),
                ruleGroupsFile.toString()
            );
            
            // Verify and test
            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("customer-validation");
            assertNotNull(ruleGroup, "Rule group should be found");
            
            Map<String, Object> testData = Map.of("age", 25, "email", "test@example.com");
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
            assertTrue(result.isTriggered(), "Rule group should pass");
            
            logSuccess("Multi-file loading test passed");
            
        } finally {
            // Cleanup
            Files.deleteIfExists(rulesFile);
            Files.deleteIfExists(ruleGroupsFile);
        }
    }
}
