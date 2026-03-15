package dev.mars.apex.core.script;

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

import dev.mars.apex.engine.core.ExpressionEvaluatorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for ScriptBridge + ExpressionEvaluatorService.
 * Validates that #script(...) works end-to-end in SpEL evaluation.
 */
@DisplayName("ScriptBridge SpEL Integration Tests")
class ScriptBridgeIntegrationTest {

    @TempDir
    Path scriptsDir;

    private ExpressionEvaluatorService evaluatorService;
    private ScriptBridge bridge;
    private ScriptExecutor executor;

    @BeforeEach
    void setUp() throws IOException {
        // Write test scripts
        Files.writeString(scriptsDir.resolve("risk-score.groovy"), """
                BigDecimal run(Map payload) {
                    BigDecimal notional = (payload.get('notional') ?: 0) as BigDecimal
                    String rating = (payload.get('creditRating') ?: 'B').toString()
                    BigDecimal base = rating in ['AAA', 'AA'] ? 70 : 40
                    return base + (notional > 1000000 ? 10 : 0)
                }
                
                boolean isEligible(String counterpartyId, BigDecimal notional, String currency) {
                    return counterpartyId?.startsWith('CP') && notional > 0 && currency in ['USD', 'EUR', 'GBP']
                }
                """);

        Files.writeString(scriptsDir.resolve("formatter.groovy"), """
                Map run(Map payload) {
                    return [formatted: payload.get('name')?.toString()?.toUpperCase()]
                }
                """);

        // Set up registry, compiler, executor, bridge
        RuntimeScriptRegistry registry = new RuntimeScriptRegistry(List.of(scriptsDir), null);
        registry.loadScripts();

        GroovyScriptCompiler compiler = new GroovyScriptCompiler("fail-fast");
        executor = new ScriptExecutor();

        bridge = new ScriptBridge(registry, compiler, executor, 5000);

        // Set up evaluator service with script bridge
        evaluatorService = new ExpressionEvaluatorService();
        evaluatorService.enableScriptBridge(bridge);
    }

    @AfterEach
    void tearDown() {
        bridge.deactivate();
        executor.shutdown();
    }

    @Test
    @DisplayName("Should evaluate #script('risk-score', #data) in SpEL condition")
    void testScriptBridgeDefaultFunction() {
        Map<String, Object> facts = new HashMap<>();
        facts.put("notional", 2000000);
        facts.put("creditRating", "AAA");

        bridge.activate();
        StandardEvaluationContext context = evaluatorService.createEvaluationContext(facts);

        // Evaluate the script call — should invoke run(Map)
        Object result = evaluatorService.evaluate(
                "#script('risk-score', #root)", context, Object.class);

        assertNotNull(result);
        // AAA rating (70) + notional > 1M (10) = 80
        assertEquals(0, new java.math.BigDecimal("80").compareTo((java.math.BigDecimal) result));
    }

    @Test
    @DisplayName("Should evaluate #script('risk-score', 'isEligible', ...) with named function")
    void testScriptBridgeNamedFunction() {
        Map<String, Object> facts = new HashMap<>();
        facts.put("counterpartyId", "CP001");
        facts.put("notional", new java.math.BigDecimal("50000"));
        facts.put("currency", "USD");

        bridge.activate();
        StandardEvaluationContext context = evaluatorService.createEvaluationContext(facts);

        Object result = evaluatorService.evaluate(
                "#script('risk-score', 'isEligible', #counterpartyId, #notional, #currency)",
                context, Object.class);

        assertEquals(true, result);
    }

    @Test
    @DisplayName("Should evaluate #script returning Map and access fields")
    void testScriptBridgeMapReturn() {
        Map<String, Object> facts = new HashMap<>();
        facts.put("name", "John Doe");

        bridge.activate();
        StandardEvaluationContext context = evaluatorService.createEvaluationContext(facts);

        Object result = evaluatorService.evaluate(
                "#script('formatter', #root)['formatted']", context, Object.class);

        assertEquals("JOHN DOE", result);
    }

    @Test
    @DisplayName("Should produce clear error when bridge not activated")
    void testScriptBridgeNotActivated() {
        Map<String, Object> facts = Map.of("key", "value");

        // Deliberately NOT calling bridge.activate()
        StandardEvaluationContext context = evaluatorService.createEvaluationContext(facts);

        // evaluate returns null on errors (existing behavior)
        Object result = evaluatorService.evaluate(
                "#script('risk-score', #root)", context, Object.class);
        assertNull(result, "Should return null when bridge not activated (error handled internally)");
    }

    @Test
    @DisplayName("Should work without script bridge (backward compat)")
    void testNoScriptBridgeBackwardCompat() {
        ExpressionEvaluatorService plainService = new ExpressionEvaluatorService();
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 100);

        StandardEvaluationContext context = plainService.createEvaluationContext(facts);

        // Regular SpEL still works
        Object result = plainService.evaluate("#amount > 50", context, Boolean.class);
        assertEquals(true, result);
    }

    @Test
    @DisplayName("Should evaluate boolean script result in condition expression")
    void testScriptInBooleanCondition() {
        Map<String, Object> facts = new HashMap<>();
        facts.put("counterpartyId", "CP001");
        facts.put("notional", new java.math.BigDecimal("50000"));
        facts.put("currency", "USD");

        bridge.activate();
        StandardEvaluationContext context = evaluatorService.createEvaluationContext(facts);

        Boolean result = evaluatorService.evaluate(
                "#script('risk-score', 'isEligible', #counterpartyId, #notional, #currency) == true",
                context, Boolean.class);

        assertTrue(result);
    }
}
