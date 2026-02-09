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
package dev.mars.apex.core.util;

import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlRule;
import dev.mars.apex.core.config.model.YamlRuleChain;
import dev.mars.apex.core.config.model.YamlRuleGroup;
import dev.mars.apex.core.config.model.YamlTransformation;
import dev.mars.apex.core.engine.model.Category;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.config.RuleBuilder;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EnabledFilter} utility class.
 * Phase 3 refactoring: validates centralised enabled-check logic.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("EnabledFilter - Centralised Enabled Checks")
class EnabledFilterTest {

    // =========================================================================
    // Rule (domain model - boolean primitive, default true)
    // =========================================================================
    @Nested
    @DisplayName("Rule (domain model)")
    class RuleTests {

        @Test
        @DisplayName("Enabled rule returns true")
        void enabledRule() {
            Rule rule = new RuleBuilder().withName("test-rule").withCondition("1 == 1").withMessage("Test rule").withSeverity("ERROR").build();
            assertTrue(EnabledFilter.isEnabled(rule));
        }

        @Test
        @DisplayName("Disabled rule returns false")
        void disabledRule() {
            // Use full 16-param constructor to set enabled=false
            Rule rule = new Rule("r1", new HashSet<>(),
                    "Disabled Rule", "1 == 1", "msg", "desc", 1, "ERROR",
                    null, null, null, null, null, null, null, false);
            assertFalse(EnabledFilter.isEnabled(rule));
        }

        @Test
        @DisplayName("Null rule treated as enabled")
        void nullRule() {
            assertTrue(EnabledFilter.isEnabled((Rule) null));
        }
    }

    // =========================================================================
    // YamlRule (nullable Boolean, null = enabled)
    // =========================================================================
    @Nested
    @DisplayName("YamlRule")
    class YamlRuleTests {

        @Test
        @DisplayName("Enabled YamlRule returns true")
        void enabledYamlRule() {
            YamlRule rule = new YamlRule();
            rule.setEnabled(true);
            assertTrue(EnabledFilter.isEnabled(rule));
        }

        @Test
        @DisplayName("Disabled YamlRule returns false")
        void disabledYamlRule() {
            YamlRule rule = new YamlRule();
            rule.setEnabled(false);
            assertFalse(EnabledFilter.isEnabled(rule));
        }

        @Test
        @DisplayName("Null-enabled YamlRule treated as enabled")
        void nullEnabledYamlRule() {
            YamlRule rule = new YamlRule();
            // enabled is null by default
            assertTrue(EnabledFilter.isEnabled(rule));
        }

        @Test
        @DisplayName("Null YamlRule treated as enabled")
        void nullYamlRule() {
            assertTrue(EnabledFilter.isEnabled((YamlRule) null));
        }
    }

    // =========================================================================
    // YamlEnrichment
    // =========================================================================
    @Nested
    @DisplayName("YamlEnrichment")
    class YamlEnrichmentTests {

        @Test
        @DisplayName("Enabled enrichment returns true")
        void enabledEnrichment() {
            YamlEnrichment enrichment = new YamlEnrichment();
            enrichment.setEnabled(true);
            assertTrue(EnabledFilter.isEnabled(enrichment));
        }

        @Test
        @DisplayName("Disabled enrichment returns false")
        void disabledEnrichment() {
            YamlEnrichment enrichment = new YamlEnrichment();
            enrichment.setEnabled(false);
            assertFalse(EnabledFilter.isEnabled(enrichment));
        }

        @Test
        @DisplayName("Null-enabled enrichment treated as enabled")
        void nullEnabledEnrichment() {
            YamlEnrichment enrichment = new YamlEnrichment();
            assertTrue(EnabledFilter.isEnabled(enrichment));
        }

        @Test
        @DisplayName("Null enrichment treated as enabled")
        void nullEnrichment() {
            assertTrue(EnabledFilter.isEnabled((YamlEnrichment) null));
        }
    }

    // =========================================================================
    // YamlTransformation
    // =========================================================================
    @Nested
    @DisplayName("YamlTransformation")
    class YamlTransformationTests {

        @Test
        @DisplayName("Enabled transformation returns true")
        void enabledTransformation() {
            YamlTransformation transformation = new YamlTransformation();
            transformation.setEnabled(true);
            assertTrue(EnabledFilter.isEnabled(transformation));
        }

        @Test
        @DisplayName("Disabled transformation returns false")
        void disabledTransformation() {
            YamlTransformation transformation = new YamlTransformation();
            transformation.setEnabled(false);
            assertFalse(EnabledFilter.isEnabled(transformation));
        }

        @Test
        @DisplayName("Null-enabled transformation treated as enabled")
        void nullEnabledTransformation() {
            YamlTransformation transformation = new YamlTransformation();
            assertTrue(EnabledFilter.isEnabled(transformation));
        }

        @Test
        @DisplayName("Null transformation treated as enabled")
        void nullTransformation() {
            assertTrue(EnabledFilter.isEnabled((YamlTransformation) null));
        }
    }

    // =========================================================================
    // YamlRuleGroup
    // =========================================================================
    @Nested
    @DisplayName("YamlRuleGroup")
    class YamlRuleGroupTests {

        @Test
        @DisplayName("Enabled rule group returns true")
        void enabledRuleGroup() {
            YamlRuleGroup group = new YamlRuleGroup();
            group.setEnabled(true);
            assertTrue(EnabledFilter.isEnabled(group));
        }

        @Test
        @DisplayName("Disabled rule group returns false")
        void disabledRuleGroup() {
            YamlRuleGroup group = new YamlRuleGroup();
            group.setEnabled(false);
            assertFalse(EnabledFilter.isEnabled(group));
        }

        @Test
        @DisplayName("Null-enabled rule group treated as enabled")
        void nullEnabledRuleGroup() {
            YamlRuleGroup group = new YamlRuleGroup();
            assertTrue(EnabledFilter.isEnabled(group));
        }

        @Test
        @DisplayName("Null rule group treated as enabled")
        void nullRuleGroup() {
            assertTrue(EnabledFilter.isEnabled((YamlRuleGroup) null));
        }
    }

    // =========================================================================
    // YamlRuleChain
    // =========================================================================
    @Nested
    @DisplayName("YamlRuleChain")
    class YamlRuleChainTests {

        @Test
        @DisplayName("Enabled rule chain returns true")
        void enabledRuleChain() {
            YamlRuleChain chain = new YamlRuleChain();
            chain.setEnabled(true);
            assertTrue(EnabledFilter.isEnabled(chain));
        }

        @Test
        @DisplayName("Disabled rule chain returns false")
        void disabledRuleChain() {
            YamlRuleChain chain = new YamlRuleChain();
            chain.setEnabled(false);
            assertFalse(EnabledFilter.isEnabled(chain));
        }

        @Test
        @DisplayName("Null-enabled rule chain treated as enabled")
        void nullEnabledRuleChain() {
            YamlRuleChain chain = new YamlRuleChain();
            assertTrue(EnabledFilter.isEnabled(chain));
        }

        @Test
        @DisplayName("Null rule chain treated as enabled")
        void nullRuleChain() {
            assertTrue(EnabledFilter.isEnabled((YamlRuleChain) null));
        }
    }

    // =========================================================================
    // List filtering helpers
    // =========================================================================
    @Nested
    @DisplayName("List Filtering")
    class ListFilteringTests {

        @Test
        @DisplayName("filterRules removes disabled rules")
        void filterRulesRemovesDisabled() {
            Rule enabled = new RuleBuilder().withName("enabled-rule").withCondition("1 == 1").withMessage("Enabled").withSeverity("ERROR").build();
            Rule disabled = new Rule("r2", new HashSet<>(),
                    "Disabled Rule", "1 == 1", "msg", "desc", 1, "ERROR",
                    null, null, null, null, null, null, null, false);

            List<Rule> filtered = EnabledFilter.filterRules(Arrays.asList(enabled, disabled));
            assertEquals(1, filtered.size());
            assertEquals("enabled-rule", filtered.get(0).getName());
        }

        @Test
        @DisplayName("filterRules handles null list")
        void filterRulesNullList() {
            List<Rule> filtered = EnabledFilter.filterRules(null);
            assertNotNull(filtered);
            assertTrue(filtered.isEmpty());
        }

        @Test
        @DisplayName("filterRules handles empty list")
        void filterRulesEmptyList() {
            List<Rule> filtered = EnabledFilter.filterRules(Collections.emptyList());
            assertNotNull(filtered);
            assertTrue(filtered.isEmpty());
        }

        @Test
        @DisplayName("filterYamlRules removes disabled YAML rules")
        void filterYamlRulesRemovesDisabled() {
            YamlRule enabled = new YamlRule();
            enabled.setId("enabled");
            enabled.setEnabled(true);

            YamlRule disabled = new YamlRule();
            disabled.setId("disabled");
            disabled.setEnabled(false);

            YamlRule nullEnabled = new YamlRule();
            nullEnabled.setId("null-enabled");
            // enabled is null - should be treated as enabled

            List<YamlRule> filtered = EnabledFilter.filterYamlRules(Arrays.asList(enabled, disabled, nullEnabled));
            assertEquals(2, filtered.size());
        }

        @Test
        @DisplayName("filterEnrichments removes disabled enrichments")
        void filterEnrichmentsRemovesDisabled() {
            YamlEnrichment enabled = new YamlEnrichment();
            enabled.setEnabled(true);

            YamlEnrichment disabled = new YamlEnrichment();
            disabled.setEnabled(false);

            List<YamlEnrichment> filtered = EnabledFilter.filterEnrichments(Arrays.asList(enabled, disabled));
            assertEquals(1, filtered.size());
        }

        @Test
        @DisplayName("filterEnrichments handles null list")
        void filterEnrichmentsNullList() {
            List<YamlEnrichment> filtered = EnabledFilter.filterEnrichments(null);
            assertNotNull(filtered);
            assertTrue(filtered.isEmpty());
        }

        @Test
        @DisplayName("filterTransformations removes disabled transformations")
        void filterTransformationsRemovesDisabled() {
            YamlTransformation enabled = new YamlTransformation();
            enabled.setEnabled(true);

            YamlTransformation disabled = new YamlTransformation();
            disabled.setEnabled(false);

            YamlTransformation nullEnabled = new YamlTransformation();
            // null = enabled

            List<YamlTransformation> filtered = EnabledFilter.filterTransformations(
                    Arrays.asList(enabled, disabled, nullEnabled));
            assertEquals(2, filtered.size());
        }

        @Test
        @DisplayName("filterTransformations handles null list")
        void filterTransformationsNullList() {
            List<YamlTransformation> filtered = EnabledFilter.filterTransformations(null);
            assertNotNull(filtered);
            assertTrue(filtered.isEmpty());
        }
    }
}
