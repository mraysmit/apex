package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.config.model.condition.SharedConditionGroup;
import dev.mars.apex.core.config.model.condition.SharedConditionRule;
import dev.mars.apex.engine.core.RuleBuilder;
import dev.mars.apex.engine.model.Rule;

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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RuleBuilder structured conditions support.
 *
 * Verifies:
 * - Building a Rule with a SharedConditionGroup via withConditions()
 * - Mutual exclusivity enforcement (condition vs conditions)
 * - Rule accessor returns the conditions group
 * - Traditional condition path still works unaffected
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.5
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class StructuredConditionRuleBuilderTest {

    // ========================================
    // Successful Build Tests
    // ========================================

    @Test
    @DisplayName("Should build rule with structured conditions instead of string condition")
    void testBuildWithStructuredConditions() {
        SharedConditionGroup group = createAndGroup(
                createExpressionPredicate("#amount > 1000"),
                createExpressionPredicate("#currency == 'USD'")
        );

        Rule rule = new RuleBuilder("struct-1")
                .withName("Structured Rule")
                .withConditions(group)
                .withMessage("High USD amount")
                .build();

        assertNotNull(rule);
        assertEquals("struct-1", rule.getId());
        assertNull(rule.getCondition(), "String condition should be null");
        assertNotNull(rule.getConditions(), "Structured conditions should be set");
        assertEquals("AND", rule.getConditions().getOperator());
        assertEquals(2, rule.getConditions().getRules().size());
    }

    @Test
    @DisplayName("Should build rule with traditional string condition")
    void testBuildWithTraditionalCondition() {
        Rule rule = new RuleBuilder("trad-1")
                .withName("Traditional Rule")
                .withCondition("#age >= 18")
                .withMessage("Age verified")
                .build();

        assertNotNull(rule);
        assertEquals("#age >= 18", rule.getCondition());
        assertNull(rule.getConditions(), "Structured conditions should be null");
    }

    @Test
    @DisplayName("Should build rule with OR conditions group")
    void testBuildWithOrConditions() {
        SharedConditionGroup group = createOrGroup(
                createExpressionPredicate("#priority == 'URGENT'"),
                createExpressionPredicate("#amount > 50000")
        );

        Rule rule = new RuleBuilder("or-1")
                .withName("OR Rule")
                .withConditions(group)
                .withMessage("Escalation required")
                .build();

        assertEquals("OR", rule.getConditions().getOperator());
        assertEquals(2, rule.getConditions().getRules().size());
        assertEquals("#priority == 'URGENT'", rule.getConditions().getRules().get(0).getCondition());
    }

    // ========================================
    // Mutual Exclusivity Enforcement Tests
    // ========================================

    @Test
    @DisplayName("Should reject build when both condition and conditions are set")
    void testRejectBothConditionAndConditions() {
        SharedConditionGroup group = createAndGroup(
                createExpressionPredicate("#a > 1")
        );

        RuleBuilder builder = new RuleBuilder("dual-1")
                .withName("Dual Rule")
                .withCondition("#b > 2")
                .withConditions(group)
                .withMessage("Should fail");

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(ex.getMessage().contains("both condition and conditions"),
                "Exception should mention both condition and conditions");
    }

    @Test
    @DisplayName("Should reject build when neither condition nor conditions is set")
    void testRejectNeitherConditionNorConditions() {
        RuleBuilder builder = new RuleBuilder("empty-1")
                .withName("Empty Rule")
                .withMessage("Should fail");

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(ex.getMessage().contains("condition") || ex.getMessage().contains("conditions"),
                "Exception should mention missing condition");
    }

    // ========================================
    // RuleFactory Integration (via YAML round-trip)
    // ========================================

    @Test
    @DisplayName("Structured conditions should survive Rule construction")
    void testConditionsSurviveConstruction() {
        SharedConditionRule pred1 = createExpressionPredicate("#x > 10");
        pred1.setDescription("X threshold");

        SharedConditionRule pred2 = createExpressionPredicate("#y < 5");
        pred2.setDescription("Y floor");

        SharedConditionGroup group = createAndGroup(pred1, pred2);

        Rule rule = new RuleBuilder("survive-1")
                .withName("Survival Test")
                .withConditions(group)
                .withMessage("Conditions present")
                .build();

        // Verify full fidelity
        SharedConditionGroup retrieved = rule.getConditions();
        assertNotNull(retrieved);
        assertEquals("AND", retrieved.getOperator());
        assertEquals(2, retrieved.getRules().size());
        assertEquals("#x > 10", retrieved.getRules().get(0).getCondition());
        assertEquals("X threshold", retrieved.getRules().get(0).getDescription());
        assertEquals("#y < 5", retrieved.getRules().get(1).getCondition());
        assertEquals("Y floor", retrieved.getRules().get(1).getDescription());
    }

    // ========================================
    // Helpers
    // ========================================

    private SharedConditionRule createExpressionPredicate(String condition) {
        SharedConditionRule rule = new SharedConditionRule();
        rule.setType("expression");
        rule.setCondition(condition);
        return rule;
    }

    private SharedConditionGroup createAndGroup(SharedConditionRule... predicates) {
        SharedConditionGroup group = new SharedConditionGroup();
        group.setOperator("AND");
        group.setRules(List.of(predicates));
        return group;
    }

    private SharedConditionGroup createOrGroup(SharedConditionRule... predicates) {
        SharedConditionGroup group = new SharedConditionGroup();
        group.setOperator("OR");
        group.setRules(List.of(predicates));
        return group;
    }
}
