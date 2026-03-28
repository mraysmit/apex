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

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for the double-nesting bug in
 * {@code EnrichmentConditionEvaluator.evaluateLookupCondition()}.
 *
 * <p>When the lookup returns a Map whose key matches the {@code result-field}
 * (e.g., result = {@code {"exists_flag": false}}, result-field = "exists_flag"),
 * the old code stashed the entire Map under that key, producing
 * {@code {"exists_flag": {"exists_flag": false}}}. The SpEL condition then
 * compared a Map to a boolean, which always evaluated incorrectly.</p>
 *
 * <p>The fix extracts the scalar value via {@code containsKey} before stashing.</p>
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * 1. Count enrichments in YAML — 1 conditional-mapping-enrichment with 3 rules
 * 2. Verify log shows "Processed: X out of X" — 100% execution rate
 * 3. Check EVERY condition — lookup conditions where result-field == Map key
 * 4. Validate EVERY routing decision — exists_flag true/false/null paths
 * 5. Assert ALL enrichment results — TRANSLATION_STATUS for each case
 */
public class LookupConditionResultFieldCollisionTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(LookupConditionResultFieldCollisionTest.class);

    private static final String CONFIG_PATH =
            "src/test/resources/dev/mars/apex/demo/conditional/LookupConditionResultFieldCollisionTest.yaml";

    @Test
    @DisplayName("Lookup returns {exists_flag: true} — result-field extracts scalar, condition matches")
    void shouldExtractScalarWhenResultFieldMatchesMapKey_True() throws Exception {
        logger.info("=== Regression: result-field collides with Map key (exists_flag=true) ===");
        logger.info("Bug scenario: stashing entire Map instead of scalar → condition comparison fails");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);
        logger.info("[OK] Configuration loaded: {} enrichments", config.getEnrichments().size());

        Map<String, Object> testData = new HashMap<>();
        testData.put("TRANSLATION_TYPE", "FX_RATE");
        logger.info("Input: {}", testData);

        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult ruleResult = engine.evaluate(config, testData);
        Map<String, Object> enrichedData = ruleResult.getEnrichedData();

        assertNotNull(enrichedData);
        logger.info("[OK] Enriched data: {}", enrichedData);

        // This is the key assertion: before the fix, the condition "exists_flag == true"
        // would fail because exists_flag contained a Map, not a boolean.
        assertEquals("TRANSLATION_FOUND", enrichedData.get("TRANSLATION_STATUS"),
                "FX_RATE has exists_flag=true — must extract scalar from Map, not stash entire Map");

        logger.info("[OK] TRANSLATION_STATUS='{}' — scalar extraction from Map works correctly",
                enrichedData.get("TRANSLATION_STATUS"));
    }

    @Test
    @DisplayName("Lookup returns {exists_flag: false} — result-field extracts scalar, condition matches false branch")
    void shouldExtractScalarWhenResultFieldMatchesMapKey_False() throws Exception {
        logger.info("=== Regression: result-field collides with Map key (exists_flag=false) ===");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);

        Map<String, Object> testData = new HashMap<>();
        testData.put("TRANSLATION_TYPE", "UNKNOWN");
        logger.info("Input: {}", testData);

        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult ruleResult = engine.evaluate(config, testData);
        Map<String, Object> enrichedData = ruleResult.getEnrichedData();

        assertNotNull(enrichedData);
        logger.info("[OK] Enriched data: {}", enrichedData);

        // exists_flag=false → rule 1 (exists_flag == true) fails → rule 2 (exists_flag == false) matches
        assertEquals("TRANSLATION_NOT_FOUND", enrichedData.get("TRANSLATION_STATUS"),
                "UNKNOWN has exists_flag=false — the false-branch condition must match");

        logger.info("[OK] TRANSLATION_STATUS='{}' — false-branch routing correct",
                enrichedData.get("TRANSLATION_STATUS"));
    }

    @Test
    @DisplayName("Lookup returns null (no match) — falls through to default rule")
    void shouldFallThroughWhenLookupReturnsNull() throws Exception {
        logger.info("=== Regression: no matching record → null result → fallback ===");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);

        Map<String, Object> testData = new HashMap<>();
        testData.put("TRANSLATION_TYPE", "NONEXISTENT");
        logger.info("Input: {}", testData);

        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult ruleResult = engine.evaluate(config, testData);
        Map<String, Object> enrichedData = ruleResult.getEnrichedData();

        assertNotNull(enrichedData);
        logger.info("[OK] Enriched data: {}", enrichedData);

        assertEquals("NO_TRANSLATION_CONFIG", enrichedData.get("TRANSLATION_STATUS"),
                "Non-existent type should fall through to the catch-all rule");

        logger.info("[OK] TRANSLATION_STATUS='{}' — null-fallback works",
                enrichedData.get("TRANSLATION_STATUS"));
    }
}
