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
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that enrichment groups referenced by function CONDITIONS on RULES
 * are correctly filtered from itemOrder by {@code ItemOrderProcessor.applyGroupsOnlyLogic()}.
 *
 * <p>Bug: {@code applyGroupsOnlyLogic()} scans enrichment mapping-rules and
 * conditional-mappings for enrichment-group-ref, but does NOT scan rules'
 * conditions blocks. This causes enrichment groups referenced by rule-level
 * function conditions to appear in itemOrder at their definition position.</p>
 *
 * <p>Fix requires two changes:</p>
 * <ol>
 *   <li>{@code YamlRule} needs a {@code conditions} field (type {@code YamlEnrichment.ConditionGroup})</li>
 *   <li>{@code applyGroupsOnlyLogic()} must iterate {@code config.getRules()} and scan each rule's conditions</li>
 * </ol>
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * 1. Count enrichment groups in YAML — 2 groups (rule-validate-group, unreferenced-group)
 * 2. Verify itemOrder filtering — 1 referenced group filtered, 1 unreferenced group remains
 * 3. Check rule conditions path — function condition on YamlRule with enrichment-group-ref
 * 4. Assert unreferenced group remains — unreferenced-group stays in itemOrder
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Rule-Level Function Condition Enrichment-Group-Ref Filtering Tests")
public class RuleConditionRefsTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RuleConditionRefsTest.class);

    private static final String CONFIG_PATH =
            "src/test/resources/dev/mars/apex/demo/conditional/RuleConditionRefsTest.yaml";

    @Test
    @DisplayName("POSITIVE: Group referenced by function condition on a RULE is filtered from itemOrder")
    void testRuleConditionGroupFilteredFromItemOrder() throws ConfigurationException {
        logger.info("=== Testing: rule-level function condition group filtered from itemOrder ===");
        logger.info("rule-validate-group is referenced by enrichment-group-ref in a function condition on trade-currency-pass rule");
        logger.info("Expected: rule-validate-group must NOT appear in final itemOrder");

        YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);
        assertNotNull(config, "Configuration should load successfully");

        assertNotNull(config.getEnrichmentGroups(), "Enrichment groups should not be null");
        assertTrue(config.getEnrichmentGroups().stream()
                        .anyMatch(g -> "rule-validate-group".equals(g.getId())),
                "rule-validate-group should be present in config");
        logger.info("[OK] rule-validate-group present in config");

        List<ProcessingItem> itemOrder = config.getItemOrder();
        assertNotNull(itemOrder, "itemOrder should not be null");

        boolean groupInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType())
                        && "rule-validate-group".equals(item.getItemId()));
        assertFalse(groupInItemOrder,
                "rule-validate-group must NOT be in itemOrder — it is referenced via enrichment-group-ref "
                        + "in a function condition on rule 'trade-currency-pass'");

        logger.info("[OK] rule-validate-group correctly filtered from itemOrder");
    }

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
}
