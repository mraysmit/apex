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

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compiles Groovy scripts and caches compiled classes.
 * Cache key is scriptId + checksum to ensure stale entries are not reused.
 * Supports two fail modes: "use-last-good" and "fail-fast".
 */
public class GroovyScriptCompiler implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(GroovyScriptCompiler.class);

    private final String failMode;
    private final GroovyClassLoader classLoader;

    /** Cache keyed by scriptId, value is the entry with checksum and compiled class. */
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    record CacheEntry(String checksum, Class<?> compiledClass) {}

    public GroovyScriptCompiler(String failMode) {
        this.failMode = failMode != null ? failMode : "use-last-good";
        this.classLoader = new GroovyClassLoader(getClass().getClassLoader());
    }

    /**
     * Get a compiled class for the given script metadata.
     * Returns cached version if checksum matches; otherwise compiles from source.
     *
     * @param meta Script metadata with path and checksum
     * @return Compiled Groovy class
     * @throws ScriptCompilationException if compilation fails and fail-mode is "fail-fast"
     *         or no cached version exists
     */
    public Class<?> getOrCompile(ScriptMetadata meta) {
        CacheEntry existing = cache.get(meta.id());

        // Cache hit — checksum matches
        if (existing != null && existing.checksum().equals(meta.checksum())) {
            logger.debug("Cache hit for script '{}' (checksum {})", meta.id(), meta.checksum());
            return existing.compiledClass();
        }

        // Cache miss or stale — compile
        return compile(meta, existing);
    }

    /**
     * Invalidate cached entry for a script ID.
     * Used when the reload manager detects a file change.
     */
    public void invalidate(String scriptId) {
        CacheEntry removed = cache.remove(scriptId);
        if (removed != null) {
            logger.debug("Invalidated cache for script '{}'", scriptId);
        }
    }

    /**
     * Check if a compiled class exists in cache for the given script ID.
     */
    public boolean isCached(String scriptId) {
        return cache.containsKey(scriptId);
    }

    @Override
    public void close() {
        cache.clear();
        try {
            classLoader.close();
        } catch (IOException e) {
            logger.warn("Error closing GroovyClassLoader: {}", e.getMessage());
        }
    }

    private Class<?> compile(ScriptMetadata meta, CacheEntry previousEntry) {
        try {
            String source = Files.readString(meta.path());
            Class<?> compiledClass = classLoader.parseClass(source, meta.id() + ".groovy");
            cache.put(meta.id(), new CacheEntry(meta.checksum(), compiledClass));
            logger.info("Compiled script '{}' (checksum {}, version {})", meta.id(), meta.checksum(), meta.version());
            return compiledClass;
        } catch (CompilationFailedException e) {
            return handleCompileFailure(meta, previousEntry, e);
        } catch (IOException e) {
            return handleCompileFailure(meta, previousEntry,
                    new CompilationFailedException(0, null, e));
        }
    }

    private Class<?> handleCompileFailure(ScriptMetadata meta, CacheEntry previousEntry, Exception e) {
        if ("use-last-good".equals(failMode) && previousEntry != null) {
            logger.warn("Compile error for script '{}', keeping last good version: {}",
                    meta.id(), e.getMessage());
            return previousEntry.compiledClass();
        }
        // fail-fast or no previous version
        logger.error("Compile error for script '{}': {}", meta.id(), e.getMessage());
        throw new ScriptCompilationException(
                "Failed to compile script '" + meta.id() + "': " + e.getMessage(), e);
    }
}
