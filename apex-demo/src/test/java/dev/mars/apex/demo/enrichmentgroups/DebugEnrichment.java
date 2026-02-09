package dev.mars.apex.demo.enrichmentgroups;

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.core.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.enrichment.EnrichmentGroupFactory;
import dev.mars.apex.core.engine.model.EnrichmentGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugEnrichment {
    public static void main(String[] args) {
        try {
            System.out.println("Starting DebugEnrichment...");
            
            String configPath = "apex-demo/src/test/java/dev/mars/apex/demo/enrichmentgroups/BasicYamlEnrichmentGroupProcessingTest-combined-config.yaml";
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile(configPath);
            
            System.out.println("Config loaded.");
            
            List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
            System.out.println("Built " + groups.size() + " enrichment groups:");
            for (EnrichmentGroup g : groups) {
                System.out.println(" - " + g.getId() + " (" + g.getEnrichmentsInOrder().size() + " enrichments)");
                for (var e : g.getEnrichmentsInOrder()) {
                    System.out.println("   - " + e.getId());
                }
            }
            
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> data = new HashMap<>();
            data.put("a", "A");
            data.put("b", "B");
            // Missing c
            
            System.out.println("Evaluating with data: " + data);
            
            RuleResult result = engine.evaluate(data);
            
            System.out.println("Result success: " + result.isSuccess());
            Map<String, Object> enrichedData = result.getEnrichedData();
            System.out.println("Enriched Data: " + enrichedData);
            
            if (enrichedData.get("a_copy") == null) {
                System.out.println("FAILURE: a_copy is null!");
            } else {
                System.out.println("SUCCESS: a_copy is " + enrichedData.get("a_copy"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
