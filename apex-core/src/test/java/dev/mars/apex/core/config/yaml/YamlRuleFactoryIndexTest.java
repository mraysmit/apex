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
package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link YamlRuleFactory} index methods added in Phase 4 refactoring.
 * Validates that pre-built lookup maps provide O(1) access equivalent to iterating YAML lists.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("YamlRuleFactory Index Methods (Phase 4)")
class YamlRuleFactoryIndexTest {

    private YamlRuleFactory ruleFactory;

    @BeforeEach
    void setUp() {
        ruleFactory = new YamlRuleFactory();
    }

    // =========================================================================
    // createRuleIndex
    // =========================================================================
    @Nested
    @DisplayName("createRuleIndex")
    class CreateRuleIndexTests {

        @Test
        @DisplayName("Returns empty map when no rules defined")
        void emptyRules() {
            YamlRuleConfiguration config = new YamlRuleConfiguration();
            Map<String, Rule> index = ruleFactory.createRuleIndex(config);
            assertNotNull(index);
            assertTrue(index.isEmpty());
        }

        @Test
        @DisplayName("Indexes rules by ID")
        void indexesByRuleId() {
            YamlRuleConfiguration config = createConfigWithRules();
            Map<String, Rule> index = ruleFactory.createRuleIndex(config);

            assertEquals(2, index.size());
            assertTrue(index.containsKey("rule-1"));
            assertTrue(index.containsKey("rule-2"));
            assertEquals("Amount Check", index.get("rule-1").getName());
            assertEquals("Currency Check", index.get("rule-2").getName());
        }

        @Test
        @DisplayName("Disabled rules are excluded from index")
        void disabledRulesExcluded() {
            YamlRuleConfiguration config = createConfigWithRules();
            // Add a disabled rule
            YamlRule disabled = new YamlRule();
            disabled.setId("rule-disabled");
            disabled.setName("Disabled Rule");
            disabled.setCondition("true");
            disabled.setMessage("Should be excluded");
            disabled.setEnabled(false);
            config.getRules().add(disabled);

            Map<String, Rule> index = ruleFactory.createRuleIndex(config);
            assertEquals(2, index.size());
            assertFalse(index.containsKey("rule-disabled"));
        }

        @Test
        @DisplayName("Index provides same rules as createRules")
        void equivalenceWithCreateRules() {
            YamlRuleConfiguration config = createConfigWithRules();
            List<Rule> rulesList = ruleFactory.createRules(config);

            // Need a new factory because createRules may have populated internal caches
            YamlRuleFactory fresh = new YamlRuleFactory();
            Map<String, Rule> index = fresh.createRuleIndex(config);

            assertEquals(rulesList.size(), index.size());
            for (Rule rule : rulesList) {
                String key = rule.getId() != null ? rule.getId() : rule.getName();
                assertTrue(index.containsKey(key), "Index should contain rule: " + key);
            }
        }
    }

    // =========================================================================
    // createRuleGroupIndex
    // =========================================================================
    @Nested
    @DisplayName("createRuleGroupIndex")
    class CreateRuleGroupIndexTests {

        @Test
        @DisplayName("Returns empty map when no rule groups defined")
        void emptyGroups() throws YamlConfigurationException {
            YamlRuleConfiguration config = new YamlRuleConfiguration();
            RulesEngineConfiguration engineConfig = new RulesEngineConfiguration();

            Map<String, RuleGroup> index = ruleFactory.createRuleGroupIndex(config, engineConfig);
            assertNotNull(index);
            assertTrue(index.isEmpty());
        }

        @Test
        @DisplayName("Indexes rule groups by ID")
        void indexesByGroupId() throws YamlConfigurationException {
            YamlRuleConfiguration config = createConfigWithRulesAndGroups();
            RulesEngineConfiguration engineConfig = buildEngineConfig(config);

            Map<String, RuleGroup> index = ruleFactory.createRuleGroupIndex(config, engineConfig);

            assertEquals(1, index.size());
            assertTrue(index.containsKey("validation-group"));
            RuleGroup group = index.get("validation-group");
            assertEquals("Validation Group", group.getName());
        }

        @Test
        @DisplayName("Disabled rule groups are excluded from index")
        void disabledGroupsExcluded() throws YamlConfigurationException {
            YamlRuleConfiguration config = createConfigWithRulesAndGroups();
            // Add a disabled group
            YamlRuleGroup disabledGroup = new YamlRuleGroup();
            disabledGroup.setId("disabled-group");
            disabledGroup.setName("Disabled Group");
            disabledGroup.setOperator("AND");
            disabledGroup.setRuleIds(List.of("rule-1"));
            disabledGroup.setEnabled(false);
            config.getRuleGroups().add(disabledGroup);

            RulesEngineConfiguration engineConfig = buildEngineConfig(config);
            Map<String, RuleGroup> index = ruleFactory.createRuleGroupIndex(config, engineConfig);

            assertEquals(1, index.size());
            assertFalse(index.containsKey("disabled-group"));
        }

        @Test
        @DisplayName("Index provides same groups as createRuleGroups")
        void equivalenceWithCreateRuleGroups() throws YamlConfigurationException {
            YamlRuleConfiguration config = createConfigWithRulesAndGroups();
            RulesEngineConfiguration engineConfig = buildEngineConfig(config);

            List<RuleGroup> groupsList = ruleFactory.createRuleGroups(config, engineConfig);

            YamlRuleFactory fresh = new YamlRuleFactory();
            RulesEngineConfiguration freshConfig = buildEngineConfig(config);
            Map<String, RuleGroup> index = fresh.createRuleGroupIndex(config, freshConfig);

            assertEquals(groupsList.size(), index.size());
            for (RuleGroup group : groupsList) {
                assertTrue(index.containsKey(group.getId()), "Index should contain group: " + group.getId());
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private YamlRuleConfiguration createConfigWithRules() {
        YamlRuleConfiguration config = new YamlRuleConfiguration();

        YamlRule rule1 = new YamlRule();
        rule1.setId("rule-1");
        rule1.setName("Amount Check");
        rule1.setCondition("#amount > 0");
        rule1.setMessage("Amount must be positive");
        rule1.setSeverity("ERROR");

        YamlRule rule2 = new YamlRule();
        rule2.setId("rule-2");
        rule2.setName("Currency Check");
        rule2.setCondition("#currency != null");
        rule2.setMessage("Currency is required");
        rule2.setSeverity("WARNING");

        config.setRules(new java.util.ArrayList<>(List.of(rule1, rule2)));
        return config;
    }

    private YamlRuleConfiguration createConfigWithRulesAndGroups() {
        YamlRuleConfiguration config = createConfigWithRules();

        YamlRuleGroup group = new YamlRuleGroup();
        group.setId("validation-group");
        group.setName("Validation Group");
        group.setOperator("AND");
        group.setRuleIds(List.of("rule-1", "rule-2"));

        config.setRuleGroups(new java.util.ArrayList<>(List.of(group)));
        return config;
    }

    private RulesEngineConfiguration buildEngineConfig(YamlRuleConfiguration yamlConfig) {
        RulesEngineConfiguration engineConfig = new RulesEngineConfiguration();
        YamlRuleFactory factory = new YamlRuleFactory();
        for (Rule rule : factory.createRules(yamlConfig)) {
            engineConfig.registerRule(rule);
        }
        return engineConfig;
    }
}
