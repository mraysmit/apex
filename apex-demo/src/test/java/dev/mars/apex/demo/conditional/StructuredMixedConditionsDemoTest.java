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

import dev.mars.apex.core.config.model.YamlEnrichment;
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
 * Structured Mixed Conditions Demo Test.
 *
 * <p>Demonstrates a realistic structured conditions block mixing all three
 * predicate types in a single AND group:</p>
 * <ol>
 *   <li>Expression predicates (simple SpEL field checks, no explicit type)</li>
 *   <li>Lookup predicate with database lookup-config, parameters, and condition gate</li>
 *   <li>Function predicate without explicit condition (defaults to true at runtime)</li>
 * </ol>
 *
 * <p>This mirrors the real-world financial services pattern where expression checks,
 * database lookups, and function invocations all contribute to a single rule evaluation.</p>
 *
 * <p><strong>Note:</strong> Phase 3 (runtime evaluation of structured conditions)
 * is not yet implemented. This test validates parsing and model wiring only.</p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.5
 */
@DisplayName("Structured Mixed Conditions Demo")
public class StructuredMixedConditionsDemoTest extends DemoTestBase {

    private static final String YAML_PATH =
            "src/test/java/dev/mars/apex/demo/conditional/StructuredMixedConditionsDemoTest.yaml";

    @Test
    @DisplayName("Should load mixed expression, lookup, and function predicates")
    void testLoadMixedPredicates() {
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);

            assertNotNull(config);
            assertEquals(1, config.getRules().size());

            YamlRule rule = config.getRules().get(0);
            assertEquals("ndf-swift-trade-validation", rule.getId());
            assertNull(rule.getCondition(), "String condition should be null for structured rule");

            SharedConditionGroup group = rule.getConditions();
            assertNotNull(group);
            assertEquals("AND", group.getOperator());

            List<SharedConditionRule> predicates = group.getRules();
            assertEquals(4, predicates.size(), "Should have 4 predicates (2 expression + 1 lookup + 1 function)");

            logger.info("Mixed predicates loaded: {} total in AND group", predicates.size());
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should parse expression predicates without explicit type")
    void testExpressionPredicatesDefaultType() {
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);
            List<SharedConditionRule> predicates = config.getRules().get(0).getConditions().getRules();

            // First two predicates: expression type (no 'type' field specified)
            SharedConditionRule expr1 = predicates.get(0);
            assertNull(expr1.getType(), "Type should be null (defaults to expression at validation/runtime)");
            assertEquals("#SYSTEM_CODE == 'SWIFT'", expr1.getCondition());

            SharedConditionRule expr2 = predicates.get(1);
            assertNull(expr2.getType());
            assertEquals("#IS_NDF == 'Y'", expr2.getCondition());

            logger.info("Expression predicates parsed correctly");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should parse lookup predicate with full database config")
    void testLookupPredicateWithDatabaseConfig() {
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);
            SharedConditionRule lookup = config.getRules().get(0).getConditions().getRules().get(2);

            assertEquals("lookup", lookup.getType());
            assertEquals("#exists_flag", lookup.getCondition());
            assertEquals("exists_flag", lookup.getResultField());

            // Verify lookup-config structure
            YamlEnrichment.LookupConfig lookupConfig = lookup.getLookupConfig();
            assertNotNull(lookupConfig, "lookup-config should be present");
            assertNotNull(lookupConfig.getLookupKey());
            assertTrue(lookupConfig.getLookupKey().contains("CLIENT_CODE"));

            // Verify lookup-dataset
            YamlEnrichment.LookupDataset dataset = lookupConfig.getLookupDataset();
            assertNotNull(dataset, "lookup-dataset should be present");
            assertEquals("database", dataset.getType());
            assertEquals("postgres-database", dataset.getDataSourceRef());
            assertEquals("lookup-translation-rule", dataset.getQueryRef());

            // Verify parameters
            List<YamlEnrichment.LookupDataset.ParameterMapping> params = dataset.getParameters();
            assertNotNull(params);
            assertEquals(3, params.size());
            assertEquals("CLIENT_CODE", params.get(0).getField());
            assertEquals("string", params.get(0).getType());
            assertEquals("SYSTEM_CODE", params.get(1).getField());
            assertEquals("EXTERNAL_CODE", params.get(2).getField());

            logger.info("Lookup predicate with database config parsed: data-source-ref={}, query-ref={}",
                    dataset.getDataSourceRef(), dataset.getQueryRef());
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should parse function predicate without explicit condition")
    void testFunctionPredicateWithoutCondition() {
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);
            SharedConditionRule function = config.getRules().get(0).getConditions().getRules().get(3);

            assertEquals("function", function.getType());
            assertNull(function.getCondition(), "Function condition should be null (defaults to true at runtime)");
            assertEquals("Classify trade risk level", function.getDescription());
            assertEquals("risk-classifier-group", function.getEnrichmentGroupRef());
            assertEquals("rbresult1", function.getOutputField());

            // Verify input-parameters
            List<YamlEnrichment.FieldMapping> inputs = function.getInputParameters();
            assertNotNull(inputs);
            assertEquals(1, inputs.size());
            assertEquals("#INPUT_VALUE", inputs.get(0).getSourceField());
            assertEquals("compute_input", inputs.get(0).getTargetField());

            logger.info("Function predicate parsed: enrichment-group-ref={}, output-field={}, condition=null (defaults to true)",
                    function.getEnrichmentGroupRef(), function.getOutputField());
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }
}
