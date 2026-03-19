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
 * Simplest possible test for the unified condition resolution feature.
 * A lookup condition resolves a country name from an inline dataset;
 * unknown codes fall through to a default rule.
 */
public class SimpleTypedConditionDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleTypedConditionDemoTest.class);

        private static final String CONFIG_PATH =
            "dev/mars/apex/demo/conditional/SimpleTypedConditionDemoTest.yaml";

    @Test
    @DisplayName("Known country code resolves to country name")
    void shouldResolveKnownCountry() throws Exception {
        logger.info("=== Testing Simple Typed Condition: Known Country ===");
        logger.info("Flow: lookup GB -> country_info != null -> COUNTRY_NAME='United Kingdom'");

        YamlRuleConfiguration config = loadAndValidateYaml(CONFIG_PATH);
        logger.info("[OK] Configuration loaded: {} enrichment(s)", config.getEnrichments().size());

        Map<String, Object> data = new HashMap<>();
        data.put("COUNTRY_CODE", "GB");
        logger.info("Input: {}", data);

        RuleResult result = RulesEngine.fromYamlConfig(config).evaluate(config, data);
        Map<String, Object> enrichedData = result.getEnrichedData();
        logger.info("[OK] Enriched data: {}", enrichedData);

        assertEquals("United Kingdom", enrichedData.get("COUNTRY_NAME"));
        logger.info("[OK] COUNTRY_NAME='{}'", enrichedData.get("COUNTRY_NAME"));
    }

    @Test
    @DisplayName("Unknown country code falls through to default")
    void shouldFallbackForUnknownCountry() throws Exception {
        logger.info("=== Testing Simple Typed Condition: Unknown Country Fallback ===");
        logger.info("Flow: lookup ZZ -> country_info == null -> fallback rule -> COUNTRY_NAME='UNKNOWN'");

        YamlRuleConfiguration config = loadAndValidateYaml(CONFIG_PATH);
        logger.info("[OK] Configuration loaded: {} enrichment(s)", config.getEnrichments().size());

        Map<String, Object> data = new HashMap<>();
        data.put("COUNTRY_CODE", "ZZ");
        logger.info("Input: {}", data);

        RuleResult result = RulesEngine.fromYamlConfig(config).evaluate(config, data);
        Map<String, Object> enrichedData = result.getEnrichedData();
        logger.info("[OK] Enriched data: {}", enrichedData);

        assertEquals("UNKNOWN", enrichedData.get("COUNTRY_NAME"));
        logger.info("[OK] COUNTRY_NAME='{}'", enrichedData.get("COUNTRY_NAME"));
    }
}
