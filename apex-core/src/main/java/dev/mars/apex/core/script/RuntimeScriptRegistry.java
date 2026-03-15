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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Registry for runtime scripts.
 * Resolves script files from configured filesystem locations, tracks metadata,
 * and enforces allowlist and path-safety constraints.
 */
public class RuntimeScriptRegistry {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeScriptRegistry.class);
    private static final String GROOVY_EXTENSION = ".groovy";

    private final List<Path> baseLocations;
    private final Set<String> allowlist;
    private final ConcurrentHashMap<String, ScriptMetadata> scripts = new ConcurrentHashMap<>();

    /**
     * @param locations Configured script directories (resolved to absolute paths)
     * @param allowlist Script IDs permitted for execution; null or empty means allow all
     */
    public RuntimeScriptRegistry(List<Path> locations, List<String> allowlist) {
        this.baseLocations = locations.stream()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .toList();
        this.allowlist = (allowlist != null && !allowlist.isEmpty())
                ? Set.copyOf(allowlist)
                : Collections.emptySet();
    }

    /**
     * Scan configured locations and load all script metadata.
     *
     * @throws IOException if a location cannot be scanned
     */
    public void loadScripts() throws IOException {
        for (Path location : baseLocations) {
            if (!Files.isDirectory(location)) {
                logger.warn("Script location does not exist or is not a directory: {}", location);
                continue;
            }
            try (Stream<Path> files = Files.list(location)) {
                files.filter(p -> p.toString().endsWith(GROOVY_EXTENSION))
                        .filter(Files::isRegularFile)
                        .forEach(this::registerScript);
            }
        }
        logger.info("Script registry loaded {} script(s): {}", scripts.size(), scripts.keySet());
    }

    private void registerScript(Path scriptPath) {
        String filename = scriptPath.getFileName().toString();
        String id = filename.substring(0, filename.length() - GROOVY_EXTENSION.length());
        try {
            String checksum = computeChecksum(scriptPath);
            long lastModified = Files.getLastModifiedTime(scriptPath).toMillis();
            boolean enabled = allowlist.isEmpty() || allowlist.contains(id);
            ScriptMetadata meta = new ScriptMetadata(id, scriptPath.toAbsolutePath().normalize(), checksum, lastModified, enabled, 1);
            scripts.put(id, meta);
            logger.debug("Registered script '{}' from {}", id, scriptPath);
        } catch (IOException e) {
            logger.error("Failed to register script '{}': {}", id, e.getMessage());
        }
    }

    /**
     * Retrieve script metadata by ID.
     *
     * @param id Script identifier
     * @return The metadata for the script
     * @throws ScriptNotFoundException if the script ID is not found in the registry
     * @throws ScriptNotAllowedException if the script ID is not on the allowlist
     */
    public ScriptMetadata getScript(String id) {
        ScriptMetadata meta = scripts.get(id);
        if (meta == null) {
            throw new ScriptNotFoundException("Script not found in registry: '" + id + "'");
        }
        if (!meta.enabled()) {
            logger.info("Script '{}' rejected — not on allowlist", id);
            throw new ScriptNotAllowedException("Script '" + id + "' is not on the allowlist");
        }
        return meta;
    }

    /**
     * Re-scan locations and detect changed scripts.
     *
     * @return Set of script IDs whose checksum changed
     * @throws IOException if a location cannot be scanned
     */
    public Set<String> refresh() throws IOException {
        Set<String> changed = new HashSet<>();
        for (Path location : baseLocations) {
            if (!Files.isDirectory(location)) {
                continue;
            }
            try (Stream<Path> files = Files.list(location)) {
                files.filter(p -> p.toString().endsWith(GROOVY_EXTENSION))
                        .filter(Files::isRegularFile)
                        .forEach(scriptPath -> {
                            String filename = scriptPath.getFileName().toString();
                            String id = filename.substring(0, filename.length() - GROOVY_EXTENSION.length());
                            try {
                                String newChecksum = computeChecksum(scriptPath);
                                long newLastModified = Files.getLastModifiedTime(scriptPath).toMillis();
                                ScriptMetadata existing = scripts.get(id);
                                if (existing == null) {
                                    // New script discovered
                                    boolean enabled = allowlist.isEmpty() || allowlist.contains(id);
                                    scripts.put(id, new ScriptMetadata(id, scriptPath.toAbsolutePath().normalize(), newChecksum, newLastModified, enabled, 1));
                                    changed.add(id);
                                    logger.info("Discovered new script '{}' during refresh", id);
                                } else if (!existing.checksum().equals(newChecksum)) {
                                    // Existing script changed
                                    scripts.put(id, existing.withUpdated(newChecksum, newLastModified));
                                    changed.add(id);
                                    logger.info("Script '{}' changed (version {})", id, existing.version() + 1);
                                }
                            } catch (IOException e) {
                                logger.error("Failed to refresh script '{}': {}", id, e.getMessage());
                            }
                        });
            }
        }
        return changed;
    }

    /**
     * Validate that a script path is under one of the configured base locations.
     * Prevents path traversal attacks.
     *
     * @param scriptPath Path to validate
     * @return true if the path is safe
     */
    public boolean isPathSafe(Path scriptPath) {
        Path normalized = scriptPath.toAbsolutePath().normalize();
        return baseLocations.stream().anyMatch(normalized::startsWith);
    }

    /**
     * @return Unmodifiable view of all script IDs currently in the registry
     */
    public Set<String> getScriptIds() {
        return Collections.unmodifiableSet(scripts.keySet());
    }

    /**
     * @return Number of scripts in the registry
     */
    public int size() {
        return scripts.size();
    }

    private static String computeChecksum(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in all JVM implementations
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
