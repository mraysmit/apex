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

import dev.mars.apex.core.config.model.YamlRule;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.model.condition.SharedConditionGroup;
import dev.mars.apex.core.config.model.condition.SharedConditionRule;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Structured AND Conditions Demo Test.
 *
 * <p>Demonstrates the new {@code conditions} block on rules with AND operator
 * and expression-type predicates. Validates that:</p>
 * <ul>
 *   <li>YAML with structured conditions loads and validates successfully</li>
 *   <li>The SharedConditionGroup is correctly deserialized with AND operator</li>
 *   <li>All expression predicates are parsed with type, condition, and description</li>
 *   <li>Traditional single-condition rules coexist in the same configuration</li>
 * </ul>
 *
 * <p><strong>Note:</strong> Phase 3 (runtime evaluation of structured conditions)
 * is not yet implemented. This test validates parsing and model wiring only.</p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.5
 */
@DisplayName("Structured AND Conditions Demo")
public class StructuredAndConditionsDemoTest extends DemoTestBase {

    private static final String YAML_PATH =
            "src/test/java/dev/mars/apex/demo/conditional/StructuredAndConditionsDemoTest.yaml";

    @Test
    @DisplayName("Should load structured AND conditions from YAML")
    void testLoadStructuredAndConditions() {
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);

            assertNotNull(config, "Configuration should load successfully");
            assertEquals(2, config.getRules().size(), "Should have 2 rules (1 structured + 1 traditional)");

            // Verify the structured-condition rule
            YamlRule structuredRule = config.getRules().get(0);
            assertEquals("high-value-usd-trade", structuredRule.getId());
            assertNull(structuredRule.getCondition(), "String condition should be null for structured rule");

            SharedConditionGroup group = structuredRule.getConditions();
            assertNotNull(group, "Conditions group should be present");
            assertEquals("AND", group.getOperator());

            List<SharedConditionRule> predicates = group.getRules();
            assertEquals(3, predicates.size(), "Should have 3 AND predicates");

            // Verify each predicate
            assertEquals("expression", predicates.get(0).getType());
            assertEquals("#amount > 10000", predicates.get(0).getCondition());
            assertEquals("Trade amount exceeds threshold", predicates.get(0).getDescription());

            assertEquals("expression", predicates.get(1).getType());
            assertEquals("#currency == 'USD'", predicates.get(1).getCondition());
            assertEquals("Trade is denominated in USD", predicates.get(1).getDescription());

            assertEquals("expression", predicates.get(2).getType());
            assertEquals("#counterparty != null", predicates.get(2).getCondition());
            assertEquals("Counterparty is specified", predicates.get(2).getDescription());

            logger.info("Structured AND conditions loaded successfully with {} predicates",
                    predicates.size());
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should coexist with traditional condition rules")
    void testMixedConditionTypes() {
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);

            // Traditional rule
            YamlRule traditionalRule = config.getRules().get(1);
            assertEquals("basic-amount-check", traditionalRule.getId());
            assertEquals("#amount > 0", traditionalRule.getCondition());
            assertNull(traditionalRule.getConditions(), "Structured conditions should be null for traditional rule");

            // Structured rule
            YamlRule structuredRule = config.getRules().get(0);
            assertNull(structuredRule.getCondition());
            assertNotNull(structuredRule.getConditions());

            logger.info("Mixed rule types (traditional + structured) coexist in same config");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }
}
