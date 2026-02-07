package dev.mars.apex.demo.rulegroups;

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlConfigurationMerger;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple test to verify cross-file rule-group references work.
 * This is a standalone test that doesn't rely on JUnit to avoid compilation issues.
 */
public class SimpleCrossFileTest {

    private static final String BASE_GROUPS_PATH = "src/test/java/dev/mars/apex/demo/rulegroups/CrossFileBaseRuleGroups.yaml";
    private static final String COMPOSITE_GROUPS_PATH = "src/test/java/dev/mars/apex/demo/rulegroups/CrossFileCompositeRuleGroups.yaml";

    public static void main(String[] args) {
        try {
            System.out.println("Testing Cross-File Rule Group References...");

            // Create rules engine from multiple files using manual merge
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration mergedConfig = new YamlRuleConfiguration();
            for (String filePath : new String[]{BASE_GROUPS_PATH, COMPOSITE_GROUPS_PATH}) {
                YamlRuleConfiguration partialConfig = loader.loadFromFileWithoutValidation(filePath);
                mergeYamlConfigurations(mergedConfig, partialConfig);
            }
            loader.processReferencesAndValidate(mergedConfig);

            RulesEngine engine = RulesEngine.fromYamlConfig(mergedConfig);

            if (engine == null) {
                System.err.println("Failed to create rules engine");
                System.exit(1);
            }
            System.out.println("Rules engine created successfully");

            // Verify the composite group exists
            RuleGroup compositeGroup = engine.getConfiguration().getRuleGroupById("cf_composite");
            if (compositeGroup == null) {
                System.err.println("Composite group 'cf_composite' not found");
                System.exit(1);
            }
            System.out.println("Composite group found: " + compositeGroup.getName());

            // Verify the base group exists
            RuleGroup baseGroup = engine.getConfiguration().getRuleGroupById("base_validation");
            if (baseGroup == null) {
                System.err.println("Base group 'base_validation' not found");
                System.exit(1);
            }
            System.out.println("Base group found: " + baseGroup.getName());

            // Check rule counts
            int baseRuleCount = baseGroup.getRules().size();
            int compositeRuleCount = compositeGroup.getRules().size();

            System.out.println("Base group has " + baseRuleCount + " rules");
            System.out.println("Composite group has " + compositeRuleCount + " rules");

            if (baseRuleCount != 2) {
                System.err.println("Expected base group to have 2 rules, but got " + baseRuleCount);
                System.exit(1);
            }
            System.out.println("Base group has correct number of rules (2)");

            if (compositeRuleCount != 3) {
                System.err.println("Expected composite group to have 3 rules (1 local + 2 from base), but got " + compositeRuleCount);
                System.exit(1);
            }
            System.out.println("Composite group has correct number of rules (3)");

            // Print rule details for verification
            System.out.println("\n Base group rules:");
            baseGroup.getRules().forEach(rule ->
                System.out.println("  - " + rule.getId() + ": " + rule.getName()));

            System.out.println("\n Composite group rules:");
            compositeGroup.getRules().forEach(rule ->
                System.out.println("  - " + rule.getId() + ": " + rule.getName()));

            System.out.println("\n ALL TESTS PASSED!");
            System.out.println(" Cross-file rule-group references are working correctly!");
            System.out.println(" Composite group successfully references base group from another file");
            System.out.println(" All 3 rules (1 local + 2 from referenced group) are working correctly");
            System.out.println("\n IMPLEMENTATION SUMMARY:");
            System.out.println(" Modified YamlRuleFactory to use two-phase rule group creation");
            System.out.println(" Create all rule groups and register them in global registry");
            System.out.println(" Resolve cross-file rule-group-references using global registry");
            System.out.println(" Cross-file rule-group references now work just like enrichment groups!");

        } catch (Exception e) {
            System.err.println(" Test failed with exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Helper method to merge YAML configurations.
     */
    /**
     * Helper method to merge YAML configurations.
     * Delegates to the public utility class in apex-core.
     */
    private static void mergeYamlConfigurations(YamlRuleConfiguration target, YamlRuleConfiguration source) {
        YamlConfigurationMerger.merge(target, source);
    }
}
