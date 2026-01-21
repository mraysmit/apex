package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.ProcessingItem;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Investigation test to examine itemOrder contents and identify the reference expansion bug.
 */
public class ItemOrderInvestigationTest extends DemoTestBase {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemOrderInvestigationTest.class);
    
    @Test
    @DisplayName("Investigate itemOrder for enrichment-group-refs file")
    public void testEnrichmentGroupRefsItemOrder() throws Exception {
        LOGGER.info("=== INVESTIGATING: ItemOrder for Enrichment-Group-Refs ===");
        
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/EnrichmentGroupRefsSequentialOrderTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        
        List<ProcessingItem> itemOrder = config.getItemOrder();
        assertNotNull(itemOrder, "ItemOrder should not be null");
        
        LOGGER.info("* ItemOrder size: {}", itemOrder.size());
        LOGGER.info("* ItemOrder contents:");
        for (int i = 0; i < itemOrder.size(); i++) {
            ProcessingItem item = itemOrder.get(i);
            LOGGER.info("  {}. {} : {}", i+1, item.getSectionType(), item.getItemId());
        }
        
        // Expected itemOrder (CORRECT):
        // 1. enrichments:enrich-counterparty-data (inline)
        // 2. enrichment-refs:* (placeholder - should execute enrichment groups at this point)
        // 3. rules:validate-all-data-enriched (inline)
        
        // Actual itemOrder (WRONG - if bug exists):
        // 1. enrichments:enrich-counterparty-data (inline)
        // 2. enrichments:enrich-market-data-group (from external file)
        // 3. enrichments:calculate-greeks-group (from external file)
        // 4. enrichments:calculate-var-group (from external file)
        // 5. enrichments:calculate-exposure-group (from external file)
        // 6. enrichment-groups:market-data-enrichment-group (from external file)
        // 7. enrichment-groups:risk-metrics-enrichment-group (from external file)
        // 8. rules:validate-all-data-enriched (inline)
        
        LOGGER.info("");
        LOGGER.info("* Analysis:");
        
        // Count enrichments and enrichment-groups
        long enrichmentCount = itemOrder.stream()
            .filter(item -> "enrichments".equals(item.getSectionType()))
            .count();
        long enrichmentGroupCount = itemOrder.stream()
            .filter(item -> "enrichment-groups".equals(item.getSectionType()))
            .count();
        
        LOGGER.info("  - Enrichments in itemOrder: {}", enrichmentCount);
        LOGGER.info("  - Enrichment-groups in itemOrder: {}", enrichmentGroupCount);
        
        if (enrichmentCount > 1 && enrichmentGroupCount > 0) {
            LOGGER.error("  BUG CONFIRMED: Both enrichments AND enrichment-groups are in itemOrder!");
            LOGGER.error("     Enrichments will be executed TWICE:");
            LOGGER.error("     1. Once as individual enrichments");
            LOGGER.error("     2. Once as part of enrichment groups");
        }
        
        // Check if enrichment-refs placeholder is still present
        boolean hasEnrichmentRefsPlaceholder = itemOrder.stream()
            .anyMatch(item -> "enrichment-refs".equals(item.getSectionType()));
        
        if (!hasEnrichmentRefsPlaceholder) {
            LOGGER.error("  BUG CONFIRMED: enrichment-refs placeholder was removed!");
            LOGGER.error("     The placeholder should remain and be executed at that position.");
        }
    }
    
    @Test
    @DisplayName("Investigate itemOrder for enrichment-refs file")
    public void testEnrichmentRefsItemOrder() throws Exception {
        LOGGER.info("=== INVESTIGATING: ItemOrder for Enrichment-Refs ===");
        
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/EnrichmentRefsSequentialOrderTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        
        List<ProcessingItem> itemOrder = config.getItemOrder();
        assertNotNull(itemOrder, "ItemOrder should not be null");
        
        LOGGER.info("* ItemOrder size: {}", itemOrder.size());
        LOGGER.info("* ItemOrder contents:");
        for (int i = 0; i < itemOrder.size(); i++) {
            ProcessingItem item = itemOrder.get(i);
            LOGGER.info("  {}. {} : {}", i+1, item.getSectionType(), item.getItemId());
        }
        
        // Expected itemOrder (CORRECT):
        // 1. enrichments:enrich-counterparty-data (inline)
        // 2. enrichment-refs:* (placeholder - should execute enrichments at this point)
        // 3. rules:validate-counterparty-enriched (inline)
        // 4. rules:validate-market-data-enriched (inline)
        // 5. rules:validate-greeks-calculated (inline)
        
        // Actual itemOrder (WRONG - if bug exists):
        // 1. enrichments:enrich-counterparty-data (inline)
        // 2. enrichments:enrich-market-data (from external file)
        // 3. enrichments:calculate-greeks (from external file)
        // 4. rules:validate-counterparty-enriched (inline)
        // 5. rules:validate-market-data-enriched (inline)
        // 6. rules:validate-greeks-calculated (inline)
        
        LOGGER.info("");
        LOGGER.info("* Analysis:");
        
        // Check if enrichment-refs placeholder is still present
        boolean hasEnrichmentRefsPlaceholder = itemOrder.stream()
            .anyMatch(item -> "enrichment-refs".equals(item.getSectionType()));
        
        if (!hasEnrichmentRefsPlaceholder) {
            LOGGER.error("  BUG CONFIRMED: enrichment-refs placeholder was removed!");
            LOGGER.error("     The placeholder should remain and be executed at that position.");
        } else {
            LOGGER.info("  enrichment-refs placeholder is present");
        }
    }
}

