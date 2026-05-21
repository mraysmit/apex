package dev.mars.apex.engine.core;

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

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MapPropertyAccessor null-safe dot-style access on nested Map payloads.
 *
 * <p>Validates that missing map keys return null (via the null-safe ?. operator) rather
 * than throwing EL1008E "Property or field ... cannot be found".
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-05-21
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
@DisplayName("MapPropertyAccessor - null-safe dot-style access")
class MapPropertyAccessorTest {

    private static final Logger logger = LoggerFactory.getLogger(MapPropertyAccessorTest.class);

    private SpelExpressionParser parser;
    private StandardEvaluationContext context;

    @BeforeEach
    void setUp() {
        parser = new SpelExpressionParser();
        context = new StandardEvaluationContext();
        context.addPropertyAccessor(new MapPropertyAccessor());
    }

    // -------------------------------------------------------------------------
    // canRead contract
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("canRead returns true for a Map target regardless of key presence")
    void canReadReturnsTrueForMapTarget() throws Exception {
        MapPropertyAccessor accessor = new MapPropertyAccessor();
        Map<String, Object> map = new HashMap<>();
        map.put("existingKey", "value");

        assertTrue(accessor.canRead(context, map, "existingKey"),
                "Should return true for present key");
        assertTrue(accessor.canRead(context, map, "missingKey"),
                "Should return true for absent key (fix: no longer false)");
        assertFalse(accessor.canRead(context, "notAMap", "key"),
                "Should return false for non-Map target");
    }

    // -------------------------------------------------------------------------
    // Happy-path: existing keys
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Dot-style access returns the correct value for a present key")
    void dotStyleAccessReturnsPresentValue() {
        Map<String, Object> trade = new HashMap<>();
        Map<String, Object> fxTrade = new HashMap<>();
        fxTrade.put("transactionType", "FXCONF");
        trade.put("fxTrade", fxTrade);

        context.setVariable("trade", trade);

        String result = parser.parseExpression("#trade?.fxTrade?.transactionType")
                .getValue(context, String.class);

        assertEquals("FXCONF", result);
        logger.info("[OK] Present key returns correct value: {}", result);
    }

    // -------------------------------------------------------------------------
    // Core fix: missing keys must not throw EL1008E
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Missing leaf key returns null instead of throwing EL1008E")
    void missingLeafKeyReturnsNull() {
        Map<String, Object> trade = new HashMap<>();
        Map<String, Object> fxTrade = new HashMap<>();
        fxTrade.put("transactionType", "FXCONF");
        // tradePositionType intentionally absent
        trade.put("fxTrade", fxTrade);

        context.setVariable("trade", trade);

        Object result = parser.parseExpression("#trade?.fxTrade?.tradePositionType")
                .getValue(context);

        assertNull(result, "Absent leaf key should return null, not throw EL1008E");
        logger.info("[OK] Missing leaf key returned null (no EL1008E)");
    }

    @Test
    @DisplayName("Missing intermediate key returns null instead of throwing EL1008E")
    void missingIntermediateKeyReturnsNull() {
        Map<String, Object> trade = new HashMap<>();
        Map<String, Object> fxTrade = new HashMap<>();
        fxTrade.put("transactionType", "FXCONF");
        // underlyingInstrumentDetails intentionally absent inside fxTrade
        trade.put("fxTrade", fxTrade);

        context.setVariable("trade", trade);

        // Before fix this threw: EL1008E: Property or field 'underlyingInstrumentDetails'
        // cannot be found on object of type java.util.HashMap
        Object result = parser.parseExpression("#trade?.fxTrade?.underlyingInstrumentDetails?.isin")
                .getValue(context);

        assertNull(result, "Absent intermediate key should return null, not throw EL1008E");
        logger.info("[OK] Missing intermediate key returned null (no EL1008E)");
    }

    // -------------------------------------------------------------------------
    // Null-safe inequality check: the primary use-case from SpEL-behavior.txt
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Null-safe != null check evaluates to false when intermediate key is absent")
    void nullSafeInequalityReturnsFalseForMissingIntermediateKey() {
        Map<String, Object> trade = new HashMap<>();
        Map<String, Object> fxTrade = new HashMap<>();
        fxTrade.put("transactionType", "FXCONF");
        // underlyingInstrumentDetails absent
        trade.put("fxTrade", fxTrade);

        context.setVariable("trade", trade);

        Boolean result = parser.parseExpression(
                "#trade?.fxTrade?.underlyingInstrumentDetails?.isin != null")
                .getValue(context, Boolean.class);

        assertFalse(result, "Should evaluate to false when chain resolves to null");
        logger.info("[OK] '!= null' check returned false for absent intermediate key");
    }

    @Test
    @DisplayName("Null-safe != null check evaluates to true when full chain is present")
    void nullSafeInequalityReturnsTrueWhenChainPresent() {
        Map<String, Object> trade = new HashMap<>();
        Map<String, Object> fxTrade = new HashMap<>();
        Map<String, Object> underlying = new HashMap<>();
        underlying.put("isin", "US0378331005");
        fxTrade.put("underlyingInstrumentDetails", underlying);
        trade.put("fxTrade", fxTrade);

        context.setVariable("trade", trade);

        Boolean result = parser.parseExpression(
                "#trade?.fxTrade?.underlyingInstrumentDetails?.isin != null")
                .getValue(context, Boolean.class);

        assertTrue(result, "Should evaluate to true when full chain is present");
        logger.info("[OK] '!= null' check returned true when full chain present");
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Entirely absent top-level variable returns null without exception")
    void absentTopLevelVariableReturnsNull() {
        // trade not set in context at all
        Object result = parser.parseExpression("#trade?.fxTrade?.transactionType")
                .getValue(context);

        assertNull(result, "Unset top-level variable should resolve to null");
        logger.info("[OK] Unset variable returned null");
    }

    @Test
    @DisplayName("Deeply nested absent key returns null without exception")
    void deeplyNestedAbsentKeyReturnsNull() {
        Map<String, Object> trade = new HashMap<>();
        Map<String, Object> fxTrade = new HashMap<>();
        trade.put("fxTrade", fxTrade);
        // nothing inside fxTrade

        context.setVariable("trade", trade);

        Object result = parser.parseExpression(
                "#trade?.fxTrade?.a?.b?.c?.d")
                .getValue(context);

        assertNull(result, "Deep absent chain should return null");
        logger.info("[OK] Deep absent chain returned null");
    }
}
