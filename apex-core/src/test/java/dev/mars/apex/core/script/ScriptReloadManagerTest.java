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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ScriptReloadManagerTest {

    @TempDir
    Path scriptsDir;

    private RuntimeScriptRegistry registry;
    private GroovyScriptCompiler compiler;
    private ScriptReloadManager reloadManager;
    private ScriptExecutor executor;

    @BeforeEach
    void setUp() throws IOException {
        // Write initial script
        Files.writeString(scriptsDir.resolve("calc.groovy"),
                "class Calc { def run(Map payload) { return payload.get('value') * 2 } }");

        registry = new RuntimeScriptRegistry(List.of(scriptsDir), List.of());
        registry.loadScripts();
        compiler = new GroovyScriptCompiler("use-last-good");
        executor = new ScriptExecutor();
    }

    @AfterEach
    void tearDown() {
        if (reloadManager != null) {
            reloadManager.stop();
        }
    }

    @Test
    void startAndStopLifecycle() {
        reloadManager = new ScriptReloadManager(registry, compiler, 100);

        assertFalse(reloadManager.isRunning());
        reloadManager.start();
        assertTrue(reloadManager.isRunning());
        reloadManager.stop();
        assertFalse(reloadManager.isRunning());
    }

    @Test
    void doubleStartIsNoOp() {
        reloadManager = new ScriptReloadManager(registry, compiler, 100);
        reloadManager.start();
        reloadManager.start(); // should not throw
        assertTrue(reloadManager.isRunning());
    }

    @Test
    void detectsChangedScriptOnDisk() throws Exception {
        // First compile to populate cache
        ScriptMetadata meta = registry.getScript("calc");
        Class<?> clazz = compiler.getOrCompile(meta);
        Object result1 = executor.execute(clazz, "run", new Object[]{Map.of("value", 5)}, 5000);
        assertEquals(10, ((Number) result1).intValue());

        // Start reload manager with short interval
        reloadManager = new ScriptReloadManager(registry, compiler, 50);
        reloadManager.start();

        // Modify script - multiply by 10 instead of 2
        Thread.sleep(50); // ensure filesystem timestamp changes
        Files.writeString(scriptsDir.resolve("calc.groovy"),
                "class Calc { def run(Map payload) { return payload.get('value') * 10 } }");

        // Wait for at least 2 polling cycles
        Thread.sleep(200);

        // Re-fetch metadata (registry should have updated checksum)
        ScriptMetadata updatedMeta = registry.getScript("calc");
        assertNotEquals(meta.checksum(), updatedMeta.checksum(), "Checksum should have changed");

        // Recompile with updated metadata (cache was invalidated)
        Class<?> newClazz = compiler.getOrCompile(updatedMeta);
        Object result2 = executor.execute(newClazz, "run", new Object[]{Map.of("value", 5)}, 5000);
        assertEquals(50, ((Number) result2).intValue());
    }

    @Test
    void detectsNewScriptOnDisk() throws Exception {
        reloadManager = new ScriptReloadManager(registry, compiler, 50);
        reloadManager.start();

        assertEquals(1, registry.size());

        // Add a new script file
        Files.writeString(scriptsDir.resolve("greeting.groovy"),
                "class Greeting { def run(Map payload) { return 'Hello ' + payload.get('name') } }");

        // Wait for polling
        Thread.sleep(200);

        assertEquals(2, registry.size());
        ScriptMetadata greetingMeta = registry.getScript("greeting");
        assertNotNull(greetingMeta);

        Class<?> clazz = compiler.getOrCompile(greetingMeta);
        Object result = executor.execute(clazz, "run", new Object[]{Map.of("name", "APEX")}, 5000);
        assertEquals("Hello APEX", result);
    }

    @Test
    void compileErrorPreservesLastGoodWithUseLastGoodMode() throws Exception {
        // Initial compile
        ScriptMetadata meta = registry.getScript("calc");
        Class<?> goodClazz = compiler.getOrCompile(meta);
        Object result1 = executor.execute(goodClazz, "run", new Object[]{Map.of("value", 3)}, 5000);
        assertEquals(6, ((Number) result1).intValue());

        // Start reload manager
        reloadManager = new ScriptReloadManager(registry, compiler, 50);
        reloadManager.start();

        // Replace with broken script
        Thread.sleep(50);
        Files.writeString(scriptsDir.resolve("calc.groovy"),
                "class Calc { def run(Map payload) { if( } }");

        // Wait for polling to detect change
        Thread.sleep(200);

        // Registry detects new checksum; compiler still has old cached class.
        // getOrCompile sees checksum mismatch, tries to compile new (broken) source,
        // and falls back to last-good version because fail-mode is use-last-good.
        ScriptMetadata updatedMeta = registry.getScript("calc");
        assertNotEquals(meta.checksum(), updatedMeta.checksum(), "Checksum should have changed");

        Class<?> clazz = compiler.getOrCompile(updatedMeta);
        assertNotNull(clazz, "use-last-good should return previous compiled class");
        Object result2 = executor.execute(clazz, "run", new Object[]{Map.of("value", 3)}, 5000);
        assertEquals(6, ((Number) result2).intValue(), "Should still produce original result");
    }

    @Test
    void parallelEvaluationsDuringReload() throws Exception {
        // Start reload manager
        reloadManager = new ScriptReloadManager(registry, compiler, 50);
        reloadManager.start();

        int threadCount = 8;
        int iterationsPerThread = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Concurrently evaluate while modifications happen
        for (int t = 0; t < threadCount; t++) {
            pool.submit(() -> {
                try {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        try {
                            ScriptMetadata meta = registry.getScript("calc");
                            Class<?> clazz = compiler.getOrCompile(meta);
                            Object result = executor.execute(clazz, "run", new Object[]{Map.of("value", 5)}, 5000);
                            assertNotNull(result);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                        Thread.sleep(5);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Modify script mid-flight
        Thread.sleep(50);
        Files.writeString(scriptsDir.resolve("calc.groovy"),
                "class Calc { def run(Map payload) { return payload.get('value') * 3 } }");

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        // All evaluations should succeed (no crashes from concurrent modification)
        assertEquals(0, errorCount.get(), "No errors expected during concurrent evaluation");
        assertEquals(threadCount * iterationsPerThread, successCount.get());
    }
}
