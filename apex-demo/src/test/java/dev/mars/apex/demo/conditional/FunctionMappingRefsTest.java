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

package dev.mars.apex.demo.conditional;

import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.sequential.ProcessingItem;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the Markit Rule Builder pattern:
 * enrichment group loaded via enrichment-refs (like Java import) is NOT auto-executed.
 * 
 * The enrichment group is a definition only — it executes solely when invoked
 * at runtime via function mapping's enrichment-group-ref.
 * 
 * This validates the fix in ItemOrderProcessor.applyGroupsOnlyLogic() that scans
 * enrichment-group-ref in function mapping rules and filters those groups from itemOrder.
 * 
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * 1. Count enrichments in YAML - 1 inline enrichment (conditional-mapping-enrichment)
 * 2. External file has 1 enrichment + 1 enrichment-group (imported via enrichment-refs)
 * 3. Imported enrichment-group must NOT appear in final itemOrder
 * 4. Imported enrichment-group MUST execute when function mapping invokes it
 * 5. When function mapping condition is NOT met, group must NOT execute
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Function Mapping with Enrichment-Refs Tests")
public class FunctionMappingRefsTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(FunctionMappingRefsTest.class);

    private static final String CONFIG_PATH =
            "src/test/resources/dev/mars/apex/demo/conditional/FunctionMappingRefsTest.yaml";

    @Test
    @DisplayName("Referenced enrichment group is filtered from itemOrder by applyGroupsOnlyLogic")
    void testReferencedGroupFilteredFromItemOrder() throws ConfigurationException {
        logger.info("=== Testing: Referenced enrichment group filtered from itemOrder ===");
        logger.info("External file has enrichment-group 'translate-group' loaded via enrichment-refs");
        logger.info("translate-group is referenced by enrichment-group-ref in function mapping");
        logger.info("Expected: translate-group must NOT appear in final itemOrder");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);
        assertNotNull(config, "Configuration should load successfully");

        // Verify enrichment group was loaded from external file
        assertNotNull(config.getEnrichmentGroups(), "Enrichment groups should not be null");
        assertTrue(config.getEnrichmentGroups().stream()
                        .anyMatch(g -> "translate-group".equals(g.getId())),
                "translate-group from external file should be present in config");
        logger.info("[OK] translate-group loaded from external file");

        // Verify itemOrder does NOT contain the referenced enrichment group
        List<ProcessingItem> itemOrder = config.getItemOrder();
        assertNotNull(itemOrder, "itemOrder should not be null");

        boolean groupInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType())
                        && "translate-group".equals(item.getItemId()));
        assertFalse(groupInItemOrder,
                "translate-group must NOT be in itemOrder — it should be filtered by applyGroupsOnlyLogic "
                        + "because it is referenced via enrichment-group-ref in function mapping");

        logger.info("[OK] translate-group correctly filtered from itemOrder");
        logger.info("[OK] itemOrder contains {} items: {}", itemOrder.size(), itemOrder);
    }

    @Test
    @DisplayName("Imported enrichment group does not execute when function mapping condition not met")
    void testImportedGroupDoesNotAutoExecute() throws ConfigurationException {
        logger.info("=== Testing: Imported enrichment group does NOT auto-execute ===");
        logger.info("When INPUT_CODE is null, function mapping condition fails, so translate-group");
        logger.info("should NOT execute. translation_result must NOT appear in enriched data.");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        // No INPUT_CODE — function mapping condition (#INPUT_CODE != null) fails
        Map<String, Object> testData = new HashMap<>();
        testData.put("OTHER_FIELD", "something");

        RuleResult result = engine.evaluate(config, testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData);

        // translation_result is set by rb-translate-enrichment inside translate-group.
        // If translate-group had auto-executed at definition position, translation_result
        // would be present (even though the function mapping didn't invoke it).
        // Since translate-group is correctly filtered from itemOrder, it only runs
        // via function mapping — which was NOT invoked (no INPUT_CODE).
        assertNull(enrichedData.get("translation_result"),
                "translation_result should NOT be set — translate-group must not auto-execute. "
                        + "It should only run when invoked by function mapping.");

        // Fallback should be applied
        assertEquals("NO_TRANSLATION", enrichedData.get("OUTPUT_CODE"),
                "Fallback should apply when function mapping condition not met");

        logger.info("[OK] translation_result is null — translate-group did not auto-execute");
        logger.info("[OK] OUTPUT_CODE='{}' — fallback applied correctly", enrichedData.get("OUTPUT_CODE"));
    }

    @Test
    @DisplayName("Function mapping invokes imported enrichment group at runtime")
    void testFunctionMappingInvokesImportedGroup() throws ConfigurationException {
        logger.info("=== Testing: Function mapping invokes imported enrichment group ===");
        logger.info("translate-via-function enrichment references translate-group via enrichment-group-ref");
        logger.info("Expected: INPUT_CODE='EUR' -> Translation_Type='CURRENCY', Input_Code='EUR'");
        logger.info("          -> translate-group runs -> translation_result='TRANSLATED_CURRENCY_EUR'");
        logger.info("          -> output-field extracts -> OUTPUT_CODE='TRANSLATED_CURRENCY_EUR'");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> testData = new HashMap<>();
        testData.put("INPUT_CODE", "EUR");

        RuleResult result = engine.evaluate(config, testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData);

        assertEquals("TRANSLATED_CURRENCY_EUR", enrichedData.get("OUTPUT_CODE"),
                "Function mapping should invoke imported translate-group and extract translation_result");

        logger.info("[OK] OUTPUT_CODE='{}' — function mapping invoked imported group correctly",
                enrichedData.get("OUTPUT_CODE"));
    }

    @Test
    @DisplayName("Fallback when no input code provided (function condition not met)")
    void testFallbackWhenNoInputCode() throws ConfigurationException {
        logger.info("=== Testing: Fallback when function mapping condition not met ===");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> testData = new HashMap<>();
        testData.put("OTHER_FIELD", "something");

        RuleResult result = engine.evaluate(config, testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData);

        assertEquals("NO_TRANSLATION", enrichedData.get("OUTPUT_CODE"),
                "Should fall back to direct mapping when INPUT_CODE is not present");

        logger.info("[OK] OUTPUT_CODE='{}' — fallback applied correctly", enrichedData.get("OUTPUT_CODE"));
    }
}
