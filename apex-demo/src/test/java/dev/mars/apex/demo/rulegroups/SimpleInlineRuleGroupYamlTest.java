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

import dev.mars.apex.core.config.exception.YamlConfigurationException;
import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.model.YamlRuleGroup;

/**
 * Simple YAML Test for Inline Rule Group References.
 *
 * This test only verifies YAML loading and structure without executing rules.
 */
public class SimpleInlineRuleGroupYamlTest {

    public static void main(String[] args) {
        System.out.println("🧪 Testing Simple Inline Rule Group YAML Structure...");
        
        try {
            // Load the YAML configuration (without validation to avoid Spring dependencies)
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFileWithoutValidation(
                "src/test/java/dev/mars/apex/demo/rulegroups/SimpleInlineRuleGroupTest-rules.yaml"
            );
            
            System.out.println("YAML configuration loaded successfully");
            
            // Test 1: Verify metadata
            System.out.println("\nTest 1: Verify Metadata");
            if (config.getMetadata() == null) {
                throw new RuntimeException("Metadata is null!");
            }
            
            System.out.println("Metadata found: " + config.getMetadata().getId());
            
            // Test 2: Verify rules
            System.out.println("\nTest 2: Verify Rules");
            if (config.getRules() == null || config.getRules().size() != 2) {
                throw new RuntimeException("Expected 2 rules, found: " + 
                    (config.getRules() == null ? "null" : config.getRules().size()));
            }
            
            System.out.println("Found 2 rules:");
            config.getRules().forEach(rule -> 
                System.out.println("   • " + rule.getId() + ": " + rule.getCondition()));
            
            // Test 3: Verify rule groups
            System.out.println("\nTest 3: Verify Rule Groups");
            if (config.getRuleGroups() == null || config.getRuleGroups().size() != 2) {
                throw new RuntimeException("Expected 2 rule groups, found: " + 
                    (config.getRuleGroups() == null ? "null" : config.getRuleGroups().size()));
            }
            
            System.out.println("Found 2 rule groups:");
            
            // Find base group
            YamlRuleGroup baseGroup = config.getRuleGroups().stream()
                .filter(g -> "base-validation".equals(g.getId()))
                .findFirst()
                .orElse(null);
            
            if (baseGroup == null) {
                throw new RuntimeException("Base group 'base-validation' not found!");
            }
            
            System.out.println("   • " + baseGroup.getId() + " (" + baseGroup.getOperator() + ")");
            System.out.println("     - Rules: " + baseGroup.getRuleIds());
            
            // Find composite group
            YamlRuleGroup compositeGroup = config.getRuleGroups().stream()
                .filter(g -> "composite-validation".equals(g.getId()))
                .findFirst()
                .orElse(null);
            
            if (compositeGroup == null) {
                throw new RuntimeException("Composite group 'composite-validation' not found!");
            }
            
            System.out.println("   • " + compositeGroup.getId() + " (" + compositeGroup.getOperator() + ")");
            System.out.println("     - Rule Group References: " + compositeGroup.getRuleGroupReferences());
            
            // Test 4: Verify inline reference
            System.out.println("\nTest 4: Verify Inline Reference");
            if (compositeGroup.getRuleGroupReferences() == null || 
                compositeGroup.getRuleGroupReferences().size() != 1) {
                throw new RuntimeException("Composite group should have 1 rule group reference!");
            }
            
            String referencedGroupId = compositeGroup.getRuleGroupReferences().get(0);
            if (!"base-validation".equals(referencedGroupId)) {
                throw new RuntimeException("Expected reference to 'base-validation', found: " + referencedGroupId);
            }
            
            System.out.println("Composite group correctly references: " + referencedGroupId);
            
            // Test 5: Verify operators
            System.out.println("\nTest 5: Verify Operators");
            if (!"AND".equals(baseGroup.getOperator())) {
                throw new RuntimeException("Base group should use AND operator, found: " + baseGroup.getOperator());
            }
            
            if (!"OR".equals(compositeGroup.getOperator())) {
                throw new RuntimeException("Composite group should use OR operator, found: " + compositeGroup.getOperator());
            }
            
            System.out.println("Base group uses AND operator");
            System.out.println("Composite group uses OR operator");
            
            // Success!
            System.out.println("\n🎉 ALL YAML STRUCTURE TESTS PASSED!");
            System.out.println("YAML file structure is correct");
            System.out.println("Inline rule-group-id reference is properly defined");
            System.out.println("Both rule groups have correct configuration");
            System.out.println("\nYAML STRUCTURE SUMMARY:");
            System.out.println("   • 2 rules: simple-rule-1 (true), simple-rule-2 (false)");
            System.out.println("   • 2 rule groups: base-validation (AND), composite-validation (OR)");
            System.out.println("   • 1 inline reference: composite-validation → base-validation");
            System.out.println("   • Base group rule-ids: " + baseGroup.getRuleIds());
            System.out.println("   • Composite group references: " + compositeGroup.getRuleGroupReferences());
            System.out.println("\n🚀 YAML structure for inline rule-group references is correct!");
            
        } catch (YamlConfigurationException e) {
            System.err.println("YAML Configuration Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Test Failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
