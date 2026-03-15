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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodically polls for script file changes and refreshes the registry/compiler cache.
 * <p>
 * Thread safety: the compiler cache is a {@code ConcurrentHashMap}, so invalidation
 * does not disrupt in-flight evaluations—the old entry remains readable until replaced
 * by the next {@code getOrCompile} call.
 */
public class ScriptReloadManager {

    private static final Logger logger = LoggerFactory.getLogger(ScriptReloadManager.class);

    private final RuntimeScriptRegistry registry;
    private final GroovyScriptCompiler compiler;
    private final long pollingIntervalMs;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    /**
     * @param registry         Script registry to refresh
     * @param compiler         Compiler whose cache entries are invalidated on change
     * @param pollingIntervalMs Polling interval in milliseconds
     */
    public ScriptReloadManager(RuntimeScriptRegistry registry, GroovyScriptCompiler compiler, long pollingIntervalMs) {
        this.registry = registry;
        this.compiler = compiler;
        this.pollingIntervalMs = pollingIntervalMs;
    }

    /**
     * Start the periodic polling task.
     * No-op if already running.
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            logger.warn("ScriptReloadManager is already running");
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "apex-script-reload");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::pollForChanges, pollingIntervalMs, pollingIntervalMs, TimeUnit.MILLISECONDS);
        logger.info("ScriptReloadManager started with polling interval {}ms", pollingIntervalMs);
    }

    /**
     * Stop the polling task and shut down the executor.
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("ScriptReloadManager stopped");
        }
    }

    /**
     * @return true if the reload manager is currently running
     */
    public boolean isRunning() {
        return running.get();
    }

    private void pollForChanges() {
        try {
            Set<String> changed = registry.refresh();
            if (!changed.isEmpty()) {
                logger.info("Detected {} changed script(s): {} — next getOrCompile will recompile with fallback", changed.size(), changed);
            }
        } catch (IOException e) {
            logger.error("Error during script reload poll: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during script reload poll: {}", e.getMessage(), e);
        }
    }
}
