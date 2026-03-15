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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RuntimeScriptRegistry.
 * Verifies script loading, allowlist enforcement, path safety, and refresh behavior.
 */
@DisplayName("RuntimeScriptRegistry Tests")
class RuntimeScriptRegistryTest {

    @TempDir
    Path scriptsDir;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(scriptsDir.resolve("risk-score.groovy"),
                "BigDecimal run(Map payload) { return 42.0 }");
        Files.writeString(scriptsDir.resolve("eligibility.groovy"),
                "boolean run(Map payload) { return true }");
    }

    @Test
    @DisplayName("Should load scripts from directory")
    void testLoadScriptsFromDirectory() throws IOException {
        RuntimeScriptRegistry registry = new RuntimeScriptRegistry(
                List.of(scriptsDir), null);
        registry.loadScripts();

        assertEquals(2, registry.size());
        assertTrue(registry.getScriptIds().contains("risk-score"));
        assertTrue(registry.getScriptIds().contains("eligibility"));
    }

    @Test
    @DisplayName("Should retrieve script metadata by ID")
    void testGetScriptById() throws IOException {
        RuntimeScriptRegistry registry = new RuntimeScriptRegistry(
                List.of(scriptsDir), null);
        registry.loadScripts();

        ScriptMetadata meta = registry.getScript("risk-score");

        assertEquals("risk-score", meta.id());
        assertTrue(meta.path().toString().endsWith("risk-score.groovy"));
        assertNotNull(meta.checksum());
        assertFalse(meta.checksum().isEmpty());
        assertTrue(meta.enabled());
        assertEquals(1, meta.version());
    }

    @Test
    @DisplayName("Should throw ScriptNotFoundException for missing ID")
    void testGetScriptMissingId() throws IOException {
        RuntimeScriptRegistry registry = new RuntimeScriptRegistry(
                List.of(scriptsDir), null);
        registry.loadScripts();

        ScriptNotFoundException ex = assertThrows(ScriptNotFoundException.class,
                () -> registry.getScript("nonexistent"));
        assertTrue(ex.getMessage().contains("nonexistent"));
    }

    @Test
    @DisplayName("Should enforce allowlist — script on disk but not in allowlist")
    void testAllowlistEnforcement() throws IOException {
        // Only allow "risk-score", not "eligibility"
        RuntimeScriptRegistry registry = new RuntimeScriptRegistry(
                List.of(scriptsDir), List.of("risk-score"));
        registry.loadScripts();

        // risk-score should work
        ScriptMetadata meta = registry.getScript("risk-score");
        assertTrue(meta.enabled());

        // eligibility is loaded but not enabled
        assertThrows(ScriptNotAllowedException.class,
                () -> registry.getScript("eligibility"));
    }

    @Test
    @DisplayName("Should allow all scripts when allowlist is null")
    void testNullAllowlistAllowsAll() throws IOException {
        RuntimeScriptRegistry registry = new RuntimeScriptRegistry(
                List.of(scriptsDir), null);
        registry.loadScripts();

        assertDoesNotThrow(() -> registry.getScript("risk-score"));
        assertDoesNotThrow(() -> registry.getScript("eligibility"));
    }

    @Test
    @DisplayName("Should allow all scripts when allowlist is empty")
    void testEmptyAllowlistAllowsAll() throws IOException {
        RuntimeScriptRegistry registry = new RuntimeScriptRegistry(
                List.of(scriptsDir), List.of());
        registry.loadScripts();

        assertDoesNotThrow(() -> registry.getScript("risk-score"));
        assertDoesNotThrow(() -> registry.getScript("eligibility"));
    }

    @Test
    @DisplayName("Should reject path traversal attempts")
    void testPathTraversalRejected() throws IOException {
        RuntimeScriptRegistry registry = new RuntimeScriptRegistry(
                List.of(scriptsDir), null);
        registry.loadScripts();

        Path traversalPath = scriptsDir.resolve("../../etc/passwd");
        assertFalse(registry.isPathSafe(traversalPath));
    }

    @Test
    @DisplayName("Should accept paths within base locations")
    void testPathWithinBaseLocation() throws IOException {
        RuntimeScriptRegistry registry = new RuntimeScriptRegistry(
                List.of(scriptsDir), null);

        Path validPath = scriptsDir.resolve("risk-score.groovy");
        assertTrue(registry.isPathSafe(validPath));
    }

    @Test
    @DisplayName("refresh() should detect file content change")
    void testRefreshDetectsChange() throws IOException {
        RuntimeScriptRegistry registry = new RuntimeScriptRegistry(
                List.of(scriptsDir), null);
        registry.loadScripts();

        String originalChecksum = registry.getScript("risk-score").checksum();

        // Modify the file
        Files.writeString(scriptsDir.resolve("risk-score.groovy"),
                "BigDecimal run(Map payload) { return 99.0 }");

        Set<String> changed = registry.refresh();

        assertTrue(changed.contains("risk-score"), "risk-score should be detected as changed");
        assertNotEquals(originalChecksum, registry.getScript("risk-score").checksum());
        assertEquals(2, registry.getScript("risk-score").version(), "version should be incremented");
    }

    @Test
    @DisplayName("refresh() should not report unchanged scripts")
    void testRefreshNoFalsePositives() throws IOException {
        RuntimeScriptRegistry registry = new RuntimeScriptRegistry(
                List.of(scriptsDir), null);
        registry.loadScripts();

        Set<String> changed = registry.refresh();

        assertTrue(changed.isEmpty(), "No scripts should be reported as changed");
    }

    @Test
    @DisplayName("refresh() should discover new scripts added after initial load")
    void testRefreshDiscoversNewScripts() throws IOException {
        RuntimeScriptRegistry registry = new RuntimeScriptRegistry(
                List.of(scriptsDir), null);
        registry.loadScripts();
        assertEquals(2, registry.size());

        // Add a new script
        Files.writeString(scriptsDir.resolve("new-script.groovy"),
                "String run(Map payload) { return 'hello' }");

        Set<String> changed = registry.refresh();

        assertTrue(changed.contains("new-script"));
        assertEquals(3, registry.size());
    }

    @Test
    @DisplayName("Should handle non-existent directory gracefully")
    void testNonExistentDirectory() throws IOException {
        Path bogus = scriptsDir.resolve("does-not-exist");
        RuntimeScriptRegistry registry = new RuntimeScriptRegistry(
                List.of(bogus), null);

        // Should not throw, just log warning
        assertDoesNotThrow(() -> registry.loadScripts());
        assertEquals(0, registry.size());
    }

    @Test
    @DisplayName("Should ignore non-groovy files in script directory")
    void testIgnoreNonGroovyFiles() throws IOException {
        Files.writeString(scriptsDir.resolve("readme.txt"), "not a script");
        Files.writeString(scriptsDir.resolve("config.yaml"), "key: value");

        RuntimeScriptRegistry registry = new RuntimeScriptRegistry(
                List.of(scriptsDir), null);
        registry.loadScripts();

        // Should only include the 2 .groovy files from setUp()
        assertEquals(2, registry.size());
    }
}
