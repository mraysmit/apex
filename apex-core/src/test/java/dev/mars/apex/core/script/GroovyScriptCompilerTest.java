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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GroovyScriptCompiler.
 * Verifies compilation, caching, invalidation, and fail-mode behavior.
 */
@DisplayName("GroovyScriptCompiler Tests")
class GroovyScriptCompilerTest {

    @TempDir
    Path scriptsDir;

    private ScriptMetadata createMetadata(String id, String source) throws IOException {
        Path scriptPath = scriptsDir.resolve(id + ".groovy");
        Files.writeString(scriptPath, source);
        return new ScriptMetadata(id, scriptPath, computeSimpleChecksum(source), System.currentTimeMillis(), true, 1);
    }

    private String computeSimpleChecksum(String content) {
        // Simple checksum for test purposes
        return Integer.toHexString(content.hashCode());
    }

    @Test
    @DisplayName("Should compile a valid Groovy script and invoke run()")
    void testCompileValidScript() throws Exception {
        ScriptMetadata meta = createMetadata("test-script",
                "BigDecimal run(Map payload) { return 42.0 }");

        GroovyScriptCompiler compiler = new GroovyScriptCompiler("fail-fast");
        Class<?> compiledClass = compiler.getOrCompile(meta);

        assertNotNull(compiledClass);
        Object instance = compiledClass.getDeclaredConstructor().newInstance();
        Method runMethod = compiledClass.getMethod("run", Map.class);
        Object result = runMethod.invoke(instance, Map.of("key", "value"));
        assertEquals(new java.math.BigDecimal("42.0"), result);
    }

    @Test
    @DisplayName("Should cache compiled script and return same class on second call")
    void testCacheHit() throws IOException {
        ScriptMetadata meta = createMetadata("cached-script",
                "boolean run(Map payload) { return true }");

        GroovyScriptCompiler compiler = new GroovyScriptCompiler("fail-fast");
        Class<?> first = compiler.getOrCompile(meta);
        Class<?> second = compiler.getOrCompile(meta);

        assertSame(first, second, "Should return same cached class");
    }

    @Test
    @DisplayName("Should recompile when checksum changes")
    void testRecompileOnChecksumChange() throws IOException {
        String source1 = "BigDecimal run(Map payload) { return 1.0 }";
        String source2 = "BigDecimal run(Map payload) { return 2.0 }";

        ScriptMetadata meta1 = createMetadata("recompile-script", source1);

        GroovyScriptCompiler compiler = new GroovyScriptCompiler("fail-fast");
        Class<?> first = compiler.getOrCompile(meta1);

        // Update file and create new metadata with different checksum
        Files.writeString(meta1.path(), source2);
        ScriptMetadata meta2 = new ScriptMetadata(meta1.id(), meta1.path(),
                computeSimpleChecksum(source2), System.currentTimeMillis(), true, 2);

        Class<?> second = compiler.getOrCompile(meta2);

        assertNotSame(first, second, "Should return different class after recompilation");
    }

    @Test
    @DisplayName("Should throw on compile error with fail-fast mode")
    void testFailFastOnCompileError() throws IOException {
        ScriptMetadata meta = createMetadata("bad-script",
                "class Bad { def run(Map payload) { if( } }");

        GroovyScriptCompiler compiler = new GroovyScriptCompiler("fail-fast");

        assertThrows(ScriptCompilationException.class, () -> compiler.getOrCompile(meta));
    }

    @Test
    @DisplayName("Should keep last good version on compile error with use-last-good mode")
    void testUseLastGoodOnCompileError() throws IOException {
        String goodSource = "BigDecimal run(Map payload) { return 42.0 }";
        ScriptMetadata goodMeta = createMetadata("good-script", goodSource);

        GroovyScriptCompiler compiler = new GroovyScriptCompiler("use-last-good");
        Class<?> goodClass = compiler.getOrCompile(goodMeta);
        assertNotNull(goodClass);

        // Now write bad source and create metadata with new checksum
        String badSource = "class Bad { def run(Map payload) { if( } }";
        Files.writeString(goodMeta.path(), badSource);
        ScriptMetadata badMeta = new ScriptMetadata(goodMeta.id(), goodMeta.path(),
                computeSimpleChecksum(badSource), System.currentTimeMillis(), true, 2);

        // Should return the last good compiled class
        Class<?> result = compiler.getOrCompile(badMeta);
        assertSame(goodClass, result, "Should return last good compiled class");
    }

    @Test
    @DisplayName("Should throw on compile error with use-last-good when no previous version")
    void testUseLastGoodNoPreviousVersion() throws IOException {
        ScriptMetadata meta = createMetadata("new-bad-script",
                "class Bad { def run(Map payload) { if( } }");

        GroovyScriptCompiler compiler = new GroovyScriptCompiler("use-last-good");

        assertThrows(ScriptCompilationException.class, () -> compiler.getOrCompile(meta));
    }

    @Test
    @DisplayName("Invalidate should remove cached entry")
    void testInvalidate() throws IOException {
        ScriptMetadata meta = createMetadata("inv-script",
                "boolean run(Map payload) { return true }");

        GroovyScriptCompiler compiler = new GroovyScriptCompiler("fail-fast");
        compiler.getOrCompile(meta);
        assertTrue(compiler.isCached("inv-script"));

        compiler.invalidate("inv-script");
        assertFalse(compiler.isCached("inv-script"));
    }
}
