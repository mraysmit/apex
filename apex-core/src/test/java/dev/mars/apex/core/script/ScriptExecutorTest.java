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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScriptExecutor.
 * Verifies function invocation, timeout, numeric coercion, and error handling.
 */
@DisplayName("ScriptExecutor Tests")
class ScriptExecutorTest {

    @TempDir
    Path scriptsDir;

    private GroovyScriptCompiler compiler;
    private ScriptExecutor executor;

    @BeforeEach
    void setUp() {
        compiler = new GroovyScriptCompiler("fail-fast");
        executor = new ScriptExecutor();
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    private Class<?> compileScript(String id, String source) throws IOException {
        Path scriptPath = scriptsDir.resolve(id + ".groovy");
        Files.writeString(scriptPath, source);
        ScriptMetadata meta = new ScriptMetadata(id, scriptPath,
                Integer.toHexString(source.hashCode()), System.currentTimeMillis(), true, 1);
        return compiler.getOrCompile(meta);
    }

    @Test
    @DisplayName("Should invoke run(Map) with default function name")
    void testInvokeRunDefault() throws IOException {
        Class<?> clazz = compileScript("test-run",
                "Map run(Map payload) { return [result: payload.get('x')] }");

        Object result = executor.execute(clazz, null, new Object[]{Map.of("x", "hello")}, 5000);

        assertInstanceOf(Map.class, result);
        assertEquals("hello", ((Map<?, ?>) result).get("result"));
    }

    @Test
    @DisplayName("Should invoke named function with positional args")
    void testInvokeNamedFunction() throws IOException {
        Class<?> clazz = compileScript("test-named", """
                boolean isEligible(String id, BigDecimal amount) {
                    return id?.startsWith('CP') && amount > 0
                }
                """);

        Object result = executor.execute(clazz, "isEligible",
                new Object[]{"CP001", new BigDecimal("100")}, 5000);

        assertEquals(true, result);
    }

    @Test
    @DisplayName("Should return numeric result")
    void testNumericReturn() throws IOException {
        Class<?> clazz = compileScript("test-numeric",
                "BigDecimal run(Map payload) { return 42.5 }");

        Object result = executor.execute(clazz, null, new Object[]{Map.of()}, 5000);

        assertInstanceOf(BigDecimal.class, result);
        assertEquals(new BigDecimal("42.5"), result);
    }

    @Test
    @DisplayName("Should coerce int to BigDecimal when target parameter type is BigDecimal")
    void testNumericCoercion() throws IOException {
        Class<?> clazz = compileScript("test-coerce",
                "BigDecimal calc(BigDecimal x) { return x.multiply(new BigDecimal('2')) }");

        // Passing int 5 should be coerced to BigDecimal
        Object result = executor.execute(clazz, "calc", new Object[]{5}, 5000);

        assertInstanceOf(BigDecimal.class, result);
        assertEquals(new BigDecimal("10"), result);
    }

    @Test
    @DisplayName("Should throw timeout exception for long-running script")
    void testTimeout() throws IOException {
        Class<?> clazz = compileScript("test-timeout",
                "def run(Map payload) { Thread.sleep(10000); return 'done' }");

        assertThrows(ScriptExecutionTimeoutException.class,
                () -> executor.execute(clazz, null, new Object[]{Map.of()}, 200));
    }

    @Test
    @DisplayName("Should handle null return gracefully")
    void testNullReturn() throws IOException {
        Class<?> clazz = compileScript("test-null",
                "def run(Map payload) { return null }");

        Object result = executor.execute(clazz, null, new Object[]{Map.of()}, 5000);
        assertNull(result);
    }

    @Test
    @DisplayName("Should throw ScriptExecutionException on runtime error")
    void testRuntimeError() throws IOException {
        Class<?> clazz = compileScript("test-error",
                "def run(Map payload) { throw new RuntimeException('boom') }");

        ScriptExecutionException ex = assertThrows(ScriptExecutionException.class,
                () -> executor.execute(clazz, null, new Object[]{Map.of()}, 5000));
        assertTrue(ex.getMessage().contains("boom"));
    }

    @Test
    @DisplayName("Should throw when method not found")
    void testMethodNotFound() throws IOException {
        Class<?> clazz = compileScript("test-no-method",
                "def run(Map payload) { return 'ok' }");

        assertThrows(ScriptExecutionException.class,
                () -> executor.execute(clazz, "nonExistentMethod", new Object[]{}, 5000));
    }
}
