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
import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Smoke matrix for RuleBuilder mapping patterns backed by unified typed condition resolution.
 *
 * This class validates the canonical patterns end-to-end using classpath-based YAML loading:
 * 1) IF=Lookup
 * 2) THEN=Lookup
 * 3) IF=Function + THEN=Function
 * 4) THEN=Function
 */
class RuleBuilderPatternSmokeMatrixTest extends DemoTestBase {

    private static final String LOOKUP_CONDITION_YAML =
            "dev/mars/apex/demo/conditional/LookupConditionDemoTest.yaml";
    private static final String LOOKUP_MAPPING_YAML =
            "dev/mars/apex/demo/conditional/LookupMappingDemoTest.yaml";
    private static final String FUNCTION_CONDITION_YAML =
            "dev/mars/apex/demo/conditional/FunctionConditionDemoTest.yaml";
    private static final String FUNCTION_MAPPING_YAML =
            "dev/mars/apex/demo/conditional/FunctionMappingTypeDemoTest.yaml";

    @Test
    @DisplayName("RuleBuilder IF=Lookup pattern routes USD to standard settlement")
    void shouldExecuteIfLookupPattern() throws Exception {
        YamlRuleConfiguration config = loadAndValidateYaml(LOOKUP_CONDITION_YAML);

        Map<String, Object> data = new HashMap<>();
        data.put("CURRENCY_CODE", "USD");
        data.put("AMOUNT", 1000000);

        Map<String, Object> enriched = evaluate(config, data);
        assertEquals("STANDARD_AMER_17:00", enriched.get("SETTLEMENT_INSTRUCTION"));
    }

    @Test
    @DisplayName("RuleBuilder THEN=Lookup pattern resolves GS counterparty attributes")
    void shouldExecuteThenLookupPattern() throws Exception {
        YamlRuleConfiguration config = loadAndValidateYaml(LOOKUP_MAPPING_YAML);

        Map<String, Object> data = new HashMap<>();
        data.put("COUNTERPARTY_CODE", "GS");

        Map<String, Object> enriched = evaluate(config, data);
        assertEquals("Goldman Sachs Group Inc.", enriched.get("COUNTERPARTY_NAME"));
        assertEquals("784F5XWPLTWKTBV8GR34", enriched.get("COUNTERPARTY_LEI"));
        assertEquals("US", enriched.get("REGULATORY_REGION"));
    }

    @Test
    @DisplayName("RuleBuilder IF=Function + THEN=Function routes high-risk notional")
    void shouldExecuteIfFunctionThenFunctionPattern() throws Exception {
        YamlRuleConfiguration config = loadAndValidateYaml(FUNCTION_CONDITION_YAML);

        Map<String, Object> data = new HashMap<>();
        data.put("NOTIONAL", 10000000);
        data.put("DESK_CODE", "FX");

        Map<String, Object> enriched = evaluate(config, data);
        assertEquals("ROUTE_COMPLIANCE_FX_PRIORITY", enriched.get("TRADE_ROUTING"));
    }

    @Test
    @DisplayName("RuleBuilder THEN=Function pattern invokes translator group")
    void shouldExecuteThenFunctionPattern() throws Exception {
        YamlRuleConfiguration config = loadAndValidateYaml(FUNCTION_MAPPING_YAML);

        Map<String, Object> data = new HashMap<>();
        data.put("INPUT_VALUE", "TRADE_001");
        data.put("CLIENT_CODE", "CLIENT_A");

        Map<String, Object> enriched = evaluate(config, data);
        assertEquals("TRANSLATED_IS_NDF_TRADE_001", enriched.get("FINAL_RESULT"));
    }

    private Map<String, Object> evaluate(YamlRuleConfiguration config, Map<String, Object> data) throws Exception {
        RuleResult result = RulesEngine.fromYamlConfig(config).evaluate(config, data);
        return result.getEnrichedData();
    }
}
