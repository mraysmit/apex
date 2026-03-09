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

import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleGroup;
import dev.mars.apex.engine.model.RuleResult;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Simple Standalone Test for Inline Rule Group References.
 *
 * This is a standalone test (no JUnit dependencies) that verifies
 * inline rule-group-id references work correctly within the same YAML file.
 */
public class SimpleInlineRuleGroupStandaloneTest {

    public static void main(String[] args) {
        System.out.println("🧪 Testing Simple Inline Rule Group References...");
        
        try {
            // Create the rules engine using static factory method
            RulesEngine rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/rulegroups/SimpleInlineRuleGroupTest-rules.yaml");
            
            // Create test context (empty for this simple test)
            Map<String, Object> testContext = new HashMap<>();
            
            System.out.println("Rules engine created successfully");
            
            // Test 1: Verify both rule groups exist
            System.out.println("\nTest 1: Verify Rule Groups Exist");
            RuleGroup baseGroup = rulesEngine.getConfiguration().getRuleGroupById("base-validation");
            RuleGroup compositeGroup = rulesEngine.getConfiguration().getRuleGroupById("composite-validation");
            
            if (baseGroup == null) {
                throw new RuntimeException("Base rule group not found!");
            }
            if (compositeGroup == null) {
                throw new RuntimeException("Composite rule group not found!");
            }
            
            System.out.println("Base group found: " + baseGroup.getId() + " (" + baseGroup.getRules().size() + " rules)");
            System.out.println("Composite group found: " + compositeGroup.getId() + " (" + compositeGroup.getRules().size() + " rules)");
            
            // Test 2: Verify rule counts
            System.out.println("\nTest 2: Verify Rule Counts");
            if (baseGroup.getRules().size() != 2) {
                throw new RuntimeException("Base group should have 2 rules, but has " + baseGroup.getRules().size());
            }
            if (compositeGroup.getRules().size() != 2) {
                throw new RuntimeException("Composite group should have 2 rules, but has " + compositeGroup.getRules().size());
            }
            
            System.out.println("Both groups have correct number of rules (2 each)");
            
            // Test 3: Verify inline reference resolution
            System.out.println("\nTest 3: Verify Inline Reference Resolution");
            boolean hasRule1 = compositeGroup.getRules().stream()
                .anyMatch(rule -> "simple-rule-1".equals(rule.getId()));
            boolean hasRule2 = compositeGroup.getRules().stream()
                .anyMatch(rule -> "simple-rule-2".equals(rule.getId()));
            
            if (!hasRule1) {
                throw new RuntimeException("Composite group missing simple-rule-1 from base group");
            }
            if (!hasRule2) {
                throw new RuntimeException("Composite group missing simple-rule-2 from base group");
            }
            
            System.out.println("Composite group contains both rules from referenced base group");
            
            // Test 4: Execute base group (AND logic - should fail)
            System.out.println("\nTest 4: Execute Base Group (AND logic)");
            RuleResult baseResult = rulesEngine.executeRuleGroupsList(List.of(baseGroup), testContext);
            
            if (baseResult == null) {
                throw new RuntimeException("Base group execution returned null result");
            }
            
            // AND group with one false rule should fail
            if (baseResult.isTriggered()) {
                throw new RuntimeException("Base group (AND) should fail but passed");
            }
            
            System.out.println("Base group (AND) failed as expected (rule-1=true, rule-2=false)");
            
            // Test 5: Execute composite group (OR logic - should pass)
            System.out.println("\nTest 5: Execute Composite Group (OR logic)");
            RuleResult compositeResult = rulesEngine.executeRuleGroupsList(List.of(compositeGroup), testContext);
            
            if (compositeResult == null) {
                throw new RuntimeException("Composite group execution returned null result");
            }
            
            // OR group with one true rule should pass
            if (!compositeResult.isTriggered()) {
                throw new RuntimeException("Composite group (OR) should pass but failed");
            }
            
            System.out.println("Composite group (OR) passed as expected (rule-1=true, rule-2=false)");
            
            // Success!
            System.out.println("\n🎉 ALL TESTS PASSED!");
            System.out.println("Inline rule-group-id references are working correctly!");
            System.out.println("Composite group successfully references base group by ID");
            System.out.println("Rule group reference resolution works within same YAML file");
            System.out.println("\nSUMMARY:");
            System.out.println("   • 2 rules: simple-rule-1 (always true), simple-rule-2 (always false)");
            System.out.println("   • 2 rule groups: base-validation (AND), composite-validation (OR)");
            System.out.println("   • 1 inline reference: composite-validation → base-validation");
            System.out.println("   • Base group (AND): FAILED [OK] (expected)");
            System.out.println("   • Composite group (OR): PASSED [OK] (expected)");
            System.out.println("\n🚀 Inline rule-group references implementation is working!");
            
        } catch (ConfigurationException e) {
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
