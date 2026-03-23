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
 * Tests that enrichment groups referenced by function CONDITIONS are correctly
 * filtered from itemOrder by {@code ItemOrderProcessor.applyGroupsOnlyLogic()}.
 *
 * <p>This complements {@link FunctionMappingRefsTest} which tests function MAPPING references.
 * The fix ensures both paths in {@code applyGroupsOnlyLogic()} scan for enrichment-group-ref:</p>
 * <ol>
 *   <li>{@code MappingRule.getMapping().getEnrichmentGroupRef()} — function mappings (existing)</li>
 *   <li>{@code ConditionRule.getEnrichmentGroupRef()} via mapping-rules conditions (new)</li>
 *   <li>{@code ConditionRule.getEnrichmentGroupRef()} via conditional-mappings conditions (new)</li>
 * </ol>
 *
 * <p>YAML contains:</p>
 * <ul>
 *   <li>{@code validate-group}: referenced by function condition in mapping-rules → FILTERED</li>
 *   <li>{@code cm-validate-group}: referenced by function condition in conditional-mappings → FILTERED</li>
 *   <li>{@code unreferenced-group}: not referenced → REMAINS in itemOrder (auto-executes)</li>
 * </ul>
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * 1. Count enrichment groups in YAML — 3 groups (validate-group, cm-validate-group, unreferenced-group)
 * 2. Verify itemOrder filtering — 2 referenced groups filtered, 1 unreferenced group remains
 * 3. Check function condition paths — mapping-rules AND conditional-mappings both handled
 * 4. Validate runtime behavior — function condition invokes group, fallback works
 * 5. Assert unreferenced group auto-executes — standalone_result is set
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Function Condition Enrichment-Group-Ref Filtering Tests")
public class FunctionConditionRefsTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(FunctionConditionRefsTest.class);

    private static final String CONFIG_PATH =
            "src/test/resources/dev/mars/apex/demo/conditional/FunctionConditionRefsTest.yaml";

    // ── ItemOrder Positive Tests ───────────────────────────────────────────────

    @Test
    @DisplayName("POSITIVE: Group referenced by function condition in mapping-rules filtered from itemOrder")
    void testMappingRulesConditionGroupFilteredFromItemOrder() throws ConfigurationException {
        logger.info("=== Testing: mapping-rules function condition group filtered from itemOrder ===");
        logger.info("validate-group is referenced by enrichment-group-ref in a function condition (mapping-rules path)");
        logger.info("Expected: validate-group must NOT appear in final itemOrder");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);
        assertNotNull(config, "Configuration should load successfully");

        assertNotNull(config.getEnrichmentGroups(), "Enrichment groups should not be null");
        assertTrue(config.getEnrichmentGroups().stream()
                        .anyMatch(g -> "validate-group".equals(g.getId())),
                "validate-group should be present in config");
        logger.info("[OK] validate-group present in config");

        List<ProcessingItem> itemOrder = config.getItemOrder();
        assertNotNull(itemOrder, "itemOrder should not be null");

        boolean groupInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType())
                        && "validate-group".equals(item.getItemId()));
        assertFalse(groupInItemOrder,
                "validate-group must NOT be in itemOrder — it is referenced via enrichment-group-ref "
                        + "in a function condition within mapping-rules");

        logger.info("[OK] validate-group correctly filtered from itemOrder");
    }

    @Test
    @DisplayName("POSITIVE: Group referenced by function condition in conditional-mappings filtered from itemOrder")
    void testConditionalMappingsConditionGroupFilteredFromItemOrder() throws ConfigurationException {
        logger.info("=== Testing: conditional-mappings function condition group filtered from itemOrder ===");
        logger.info("cm-validate-group is referenced by enrichment-group-ref in a function condition (conditional-mappings path)");
        logger.info("Expected: cm-validate-group must NOT appear in final itemOrder");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);
        assertNotNull(config, "Configuration should load successfully");

        assertTrue(config.getEnrichmentGroups().stream()
                        .anyMatch(g -> "cm-validate-group".equals(g.getId())),
                "cm-validate-group should be present in config");
        logger.info("[OK] cm-validate-group present in config");

        List<ProcessingItem> itemOrder = config.getItemOrder();
        assertNotNull(itemOrder, "itemOrder should not be null");

        boolean groupInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType())
                        && "cm-validate-group".equals(item.getItemId()));
        assertFalse(groupInItemOrder,
                "cm-validate-group must NOT be in itemOrder — it is referenced via enrichment-group-ref "
                        + "in a function condition within conditional-mappings");

        logger.info("[OK] cm-validate-group correctly filtered from itemOrder");
    }

    // ── ItemOrder Negative Test ────────────────────────────────────────────────

    @Test
    @DisplayName("NEGATIVE: Unreferenced group remains in itemOrder")
    void testUnreferencedGroupRemainsInItemOrder() throws ConfigurationException {
        logger.info("=== Testing: Unreferenced group remains in itemOrder ===");
        logger.info("unreferenced-group is NOT referenced by any function condition or mapping");
        logger.info("Expected: unreferenced-group MUST appear in final itemOrder");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);
        assertNotNull(config, "Configuration should load successfully");

        List<ProcessingItem> itemOrder = config.getItemOrder();
        assertNotNull(itemOrder, "itemOrder should not be null");

        boolean groupInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType())
                        && "unreferenced-group".equals(item.getItemId()));
        assertTrue(groupInItemOrder,
                "unreferenced-group MUST be in itemOrder — it is not referenced by any function "
                        + "condition or mapping, so it should auto-execute at its definition position");

        logger.info("[OK] unreferenced-group correctly remains in itemOrder");
        logger.info("[OK] Final itemOrder: {}", itemOrder);
    }

    // ── Runtime Positive Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("POSITIVE: Function condition invokes group and routes correctly at runtime")
    void testFunctionConditionInvokesGroupAtRuntime() throws ConfigurationException {
        logger.info("=== Testing: Function condition invokes validate-group at runtime ===");
        logger.info("Flow: routing-enrichment condition invokes validate-group");
        logger.info("      → validator sets validation_status='VALID_FX'");
        logger.info("      → condition passes → mapping produces 'ROUTED_VALID_FX'");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> testData = new HashMap<>();
        testData.put("TRADE_TYPE", "FX");

        RuleResult result = engine.evaluate(config, testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData);

        assertEquals("ROUTED_VALID_FX", enrichedData.get("ROUTING_RESULT"),
                "Function condition should invoke validate-group, "
                        + "get VALID_FX, and produce ROUTED_VALID_FX");

        logger.info("[OK] ROUTING_RESULT='{}' — function condition invoked group correctly",
                enrichedData.get("ROUTING_RESULT"));
    }

    @Test
    @DisplayName("POSITIVE: Fallback when function condition evaluates to false")
    void testFallbackWhenConditionNotMet() throws ConfigurationException {
        logger.info("=== Testing: Fallback when function condition evaluates to false ===");
        logger.info("When TRADE_TYPE is null, validator produces 'INVALID'");
        logger.info("Condition #validation_status.startsWith('VALID') fails → default route");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> testData = new HashMap<>();
        testData.put("OTHER_FIELD", "something");

        RuleResult result = engine.evaluate(config, testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData);

        assertEquals("DEFAULT_ROUTE", enrichedData.get("ROUTING_RESULT"),
                "Should fall back to default route when validation condition not met");

        logger.info("[OK] ROUTING_RESULT='{}' — fallback applied correctly",
                enrichedData.get("ROUTING_RESULT"));
    }

    // ── Runtime Negative Test ──────────────────────────────────────────────────

    @Test
    @DisplayName("NEGATIVE: Unreferenced group auto-executes and produces output")
    void testUnreferencedGroupAutoExecutes() throws ConfigurationException {
        logger.info("=== Testing: Unreferenced group auto-executes at definition position ===");
        logger.info("unreferenced-group is NOT filtered from itemOrder");
        logger.info("Expected: standalone_result='STANDALONE_OK' (group auto-executed)");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> testData = new HashMap<>();
        testData.put("TRADE_TYPE", "FX");

        RuleResult result = engine.evaluate(config, testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData);

        assertEquals("STANDALONE_OK", enrichedData.get("standalone_result"),
                "standalone_result should be set — unreferenced-group must auto-execute "
                        + "at its definition position since it is not filtered from itemOrder");

        logger.info("[OK] standalone_result='{}' — unreferenced group auto-executed correctly",
                enrichedData.get("standalone_result"));
    }
}
