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
 * Structured OR Conditions Demo Test.
 *
 * <p>Demonstrates the new {@code conditions} block on rules with OR operator.
 * Validates that:</p>
 * <ul>
 *   <li>OR operator is correctly deserialized</li>
 *   <li>Multiple expression predicates are parsed</li>
 *   <li>Single-predicate structured conditions work (edge case)</li>
 *   <li>Severity and message fields coexist with structured conditions</li>
 * </ul>
 *
 * <p><strong>Note:</strong> Phase 3 (runtime evaluation of structured conditions)
 * is not yet implemented. This test validates parsing and model wiring only.</p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.5
 */
@DisplayName("Structured OR Conditions Demo")
public class StructuredOrConditionsDemoTest extends DemoTestBase {

    private static final String YAML_PATH =
            "src/test/java/dev/mars/apex/demo/conditional/StructuredOrConditionsDemoTest.yaml";

    @Test
    @DisplayName("Should load structured OR conditions from YAML")
    void testLoadStructuredOrConditions() {
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);

            assertNotNull(config, "Configuration should load successfully");
            assertEquals(2, config.getRules().size(), "Should have 2 rules");

            YamlRule orRule = config.getRules().get(0);
            assertEquals("escalation-required", orRule.getId());
            assertNull(orRule.getCondition(), "String condition should be null");

            SharedConditionGroup group = orRule.getConditions();
            assertNotNull(group, "Conditions group should be present");
            assertEquals("OR", group.getOperator());

            List<SharedConditionRule> predicates = group.getRules();
            assertEquals(3, predicates.size(), "Should have 3 OR predicates");

            assertEquals("#amount > 50000", predicates.get(0).getCondition());
            assertEquals("Amount exceeds escalation threshold", predicates.get(0).getDescription());

            assertEquals("#priority == 'URGENT'", predicates.get(1).getCondition());
            assertEquals("#region == 'RESTRICTED'", predicates.get(2).getCondition());

            // Verify severity coexists with structured conditions
            assertEquals("ERROR", orRule.getSeverity());

            logger.info("Structured OR conditions loaded successfully with {} predicates",
                    predicates.size());
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle single-predicate structured conditions")
    void testSinglePredicateStructuredConditions() {
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);

            YamlRule singleRule = config.getRules().get(1);
            assertEquals("single-predicate-structured", singleRule.getId());

            SharedConditionGroup group = singleRule.getConditions();
            assertNotNull(group);
            assertEquals("OR", group.getOperator());
            assertEquals(1, group.getRules().size(), "Should have exactly 1 predicate");

            SharedConditionRule predicate = group.getRules().get(0);
            assertEquals("expression", predicate.getType());
            assertEquals("#status == 'PENDING_REVIEW'", predicate.getCondition());
            assertEquals("Status requires review", predicate.getDescription());

            logger.info("Single-predicate structured conditions validated");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }
}
