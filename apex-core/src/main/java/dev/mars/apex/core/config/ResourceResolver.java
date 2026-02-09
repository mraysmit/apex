package dev.mars.apex.core.config;

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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Unified resource resolver for APEX configuration files.
 * 
 * <p>This class provides a single abstraction for resolving resources from multiple sources:
 * <ul>
 *   <li>Classpath resources (JAR-packaged files, test resources)</li>
 *   <li>Filesystem paths (absolute and relative)</li>
 *   <li>Configurable search paths</li>
 * </ul>
 * 
 * <p><b>Resolution Strategy:</b></p>
 * <p>By default, the resolver tries classpath first, then filesystem. This enables:
 * <ul>
 *   <li>JAR-packaged applications to load embedded configurations</li>
 *   <li>Test resources to be found on classpath</li>
 *   <li>Fallback to filesystem for development and external configs</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>
 * // Create resolver with default settings
 * ResourceResolver resolver = new ResourceResolver();
 * 
 * // Resolve a resource (tries classpath first, then filesystem)
 * try (InputStream is = resolver.resolve("config/rules.yaml")) {
 *     // Process the stream
 * }
 * 
 * // Resolve with a base path for relative references
 * try (InputStream is = resolver.resolve("rules.yaml", "config/scenarios/")) {
 *     // Resolves to "config/scenarios/rules.yaml"
 * }
 * 
 * // Add custom search paths
 * resolver.addSearchPath("/etc/apex/configs");
 * resolver.addClasspathPrefix("META-INF/apex/");
 * </pre>
 * 
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe. Search paths and classpath prefixes can be modified
 * concurrently, though it's recommended to configure paths during initialization.</p>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-08
 * @see dev.mars.apex.core.config.YamlConfigurationLoader
 * @see dev.mars.apex.core.config.ScenarioRegistryLoader
 */
public class ResourceResolver {

    private static final Logger logger = LoggerFactory.getLogger(ResourceResolver.class);

    /**
     * Resolution strategy determining the order of resolution attempts.
     */
    public enum ResolutionStrategy {
        /** Try classpath first, then filesystem (default) */
        CLASSPATH_FIRST,
        /** Try filesystem first, then classpath */
        FILESYSTEM_FIRST,
        /** Only try classpath */
        CLASSPATH_ONLY,
        /** Only try filesystem */
        FILESYSTEM_ONLY
    }

    private final List<String> searchPaths;
    private final List<String> classpathPrefixes;
    private final ClassLoader classLoader;
    private ResolutionStrategy resolutionStrategy;

    /**
     * Create a new ResourceResolver with default settings.
     * 
     * <p>Uses the current thread's context class loader and CLASSPATH_FIRST strategy.</p>
     */
    public ResourceResolver() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Create a new ResourceResolver with a specific class loader.
     * 
     * @param classLoader The class loader to use for classpath resolution
     */
    public ResourceResolver(ClassLoader classLoader) {
        this.searchPaths = Collections.synchronizedList(new ArrayList<>());
        this.classpathPrefixes = Collections.synchronizedList(new ArrayList<>());
        this.classLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        this.resolutionStrategy = ResolutionStrategy.CLASSPATH_FIRST;
    }

    // ========================================================================
    // Primary Resolution Methods
    // ========================================================================

    /**
     * Resolve a resource reference to an InputStream.
     * 
     * <p>This method attempts to resolve the reference according to the configured
     * resolution strategy (classpath first by default).</p>
     * 
     * @param reference The resource reference (path or classpath resource)
     * @return An InputStream for the resolved resource
     * @throws ResourceNotFoundException if the resource cannot be found
     * @throws IllegalArgumentException if reference is null or empty
     */
    public InputStream resolve(String reference) throws ResourceNotFoundException {
        return resolve(reference, null);
    }

    /**
     * Resolve a resource reference with a base path for relative resolution.
     * 
     * <p>If the reference is a relative path, it will be resolved against the base path.
     * The base path is typically the directory containing the referencing file.</p>
     * 
     * @param reference The resource reference (can be relative or absolute)
     * @param basePath The base path for relative resolution (can be null)
     * @return An InputStream for the resolved resource
     * @throws ResourceNotFoundException if the resource cannot be found
     * @throws IllegalArgumentException if reference is null or empty
     */
    public InputStream resolve(String reference, String basePath) throws ResourceNotFoundException {
        if (reference == null || reference.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource reference cannot be null or empty");
        }

        logger.debug("Resolving resource: '{}' with basePath: '{}'", reference, basePath);

        // Normalize the reference
        String normalizedRef = normalizeReference(reference, basePath);

        InputStream result = null;

        switch (resolutionStrategy) {
            case CLASSPATH_FIRST:
                result = tryClasspathThenFilesystem(normalizedRef, reference, basePath);
                break;
            case FILESYSTEM_FIRST:
                result = tryFilesystemThenClasspath(normalizedRef, reference, basePath);
                break;
            case CLASSPATH_ONLY:
                result = resolveFromClasspathInternal(normalizedRef);
                break;
            case FILESYSTEM_ONLY:
                result = resolveFromFilesystemInternal(normalizedRef);
                break;
        }

        if (result == null) {
            throw new ResourceNotFoundException(
                "Resource not found: '" + reference + "'" +
                (basePath != null ? " (base: '" + basePath + "')" : "") +
                " [strategy: " + resolutionStrategy + "]"
            );
        }

        return result;
    }

    /**
     * Resolve a resource explicitly from the filesystem.
     * 
     * <p>This method only searches the filesystem, ignoring classpath resources.
     * It checks the reference as an absolute path, then searches configured search paths.</p>
     * 
     * @param path The filesystem path to resolve
     * @return An InputStream for the resolved file
     * @throws ResourceNotFoundException if the file cannot be found
     * @throws IllegalArgumentException if path is null or empty
     */
    public InputStream resolveFromFilesystem(String path) throws ResourceNotFoundException {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        InputStream result = resolveFromFilesystemInternal(path);
        if (result == null) {
            throw new ResourceNotFoundException("File not found: " + path);
        }
        return result;
    }

    /**
     * Resolve a resource explicitly from the classpath.
     * 
     * <p>This method only searches the classpath, ignoring filesystem paths.
     * It checks the reference directly, then with configured classpath prefixes.</p>
     * 
     * @param resourcePath The classpath resource path
     * @return An InputStream for the resolved resource
     * @throws ResourceNotFoundException if the resource cannot be found
     * @throws IllegalArgumentException if resourcePath is null or empty
     */
    public InputStream resolveFromClasspath(String resourcePath) throws ResourceNotFoundException {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource path cannot be null or empty");
        }

        InputStream result = resolveFromClasspathInternal(resourcePath);
        if (result == null) {
            throw new ResourceNotFoundException("Classpath resource not found: " + resourcePath);
        }
        return result;
    }

    // ========================================================================
    // Path Management
    // ========================================================================

    /**
     * Add a filesystem search path.
     * 
     * <p>Search paths are checked in order when resolving filesystem resources.
     * The reference is appended to each search path until a match is found.</p>
     * 
     * @param path The filesystem path to add (directory)
     * @throws IllegalArgumentException if path is null or empty
     */
    public void addSearchPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Search path cannot be null or empty");
        }
        String normalizedPath = normalizePath(path);
        if (!searchPaths.contains(normalizedPath)) {
            searchPaths.add(normalizedPath);
            logger.debug("Added search path: {}", normalizedPath);
        }
    }

    /**
     * Add a classpath prefix for resource resolution.
     * 
     * <p>Classpath prefixes are checked in order when resolving classpath resources.
     * The reference is appended to each prefix until a match is found.</p>
     * 
     * @param prefix The classpath prefix to add
     * @throws IllegalArgumentException if prefix is null or empty
     */
    public void addClasspathPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Classpath prefix cannot be null or empty");
        }
        String normalizedPrefix = normalizeClasspathPrefix(prefix);
        if (!classpathPrefixes.contains(normalizedPrefix)) {
            classpathPrefixes.add(normalizedPrefix);
            logger.debug("Added classpath prefix: {}", normalizedPrefix);
        }
    }

    /**
     * Set the filesystem search paths, replacing any existing paths.
     * 
     * @param paths The list of search paths to set
     */
    public void setSearchPaths(List<String> paths) {
        synchronized (searchPaths) {
            searchPaths.clear();
            if (paths != null) {
                for (String path : paths) {
                    if (path != null && !path.trim().isEmpty()) {
                        searchPaths.add(normalizePath(path));
                    }
                }
            }
        }
        logger.debug("Set {} search paths", searchPaths.size());
    }

    /**
     * Set the classpath prefixes, replacing any existing prefixes.
     * 
     * @param prefixes The list of classpath prefixes to set
     */
    public void setClasspathPrefixes(List<String> prefixes) {
        synchronized (classpathPrefixes) {
            classpathPrefixes.clear();
            if (prefixes != null) {
                for (String prefix : prefixes) {
                    if (prefix != null && !prefix.trim().isEmpty()) {
                        classpathPrefixes.add(normalizeClasspathPrefix(prefix));
                    }
                }
            }
        }
        logger.debug("Set {} classpath prefixes", classpathPrefixes.size());
    }

    /**
     * Get an unmodifiable view of the configured search paths.
     * 
     * @return The list of filesystem search paths
     */
    public List<String> getSearchPaths() {
        return Collections.unmodifiableList(new ArrayList<>(searchPaths));
    }

    /**
     * Get an unmodifiable view of the configured classpath prefixes.
     * 
     * @return The list of classpath prefixes
     */
    public List<String> getClasspathPrefixes() {
        return Collections.unmodifiableList(new ArrayList<>(classpathPrefixes));
    }

    /**
     * Set the resolution strategy.
     * 
     * @param strategy The resolution strategy to use
     */
    public void setResolutionStrategy(ResolutionStrategy strategy) {
        this.resolutionStrategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
        logger.debug("Set resolution strategy: {}", strategy);
    }

    /**
     * Get the current resolution strategy.
     * 
     * @return The current resolution strategy
     */
    public ResolutionStrategy getResolutionStrategy() {
        return resolutionStrategy;
    }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    /**
     * Resolve a relative path against a base path.
     * 
     * <p>This method handles path normalization and supports both classpath
     * and filesystem path conventions.</p>
     * 
     * @param reference The relative reference
     * @param basePath The base path
     * @return The resolved path
     */
    public String resolveRelativePath(String reference, String basePath) {
        if (reference == null) {
            return null;
        }
        if (basePath == null || basePath.isEmpty()) {
            return reference;
        }
        return normalizeReference(reference, basePath);
    }

    /**
     * Extract the base directory from a resource path.
     * 
     * <p>For example, "config/scenarios/registry.yaml" returns "config/scenarios/".</p>
     * 
     * @param resourcePath The resource path
     * @return The base directory with trailing slash, or empty string if at root
     */
    public String getClasspathBase(String resourcePath) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            return "";
        }
        int lastSlash = resourcePath.lastIndexOf('/');
        if (lastSlash > 0) {
            return resourcePath.substring(0, lastSlash + 1);
        }
        return "";
    }

    /**
     * Check if a resource exists at the given reference.
     * 
     * @param reference The resource reference to check
     * @return true if the resource exists, false otherwise
     */
    public boolean exists(String reference) {
        return exists(reference, null);
    }

    /**
     * Check if a resource exists at the given reference with base path.
     * 
     * @param reference The resource reference to check
     * @param basePath The base path for relative resolution
     * @return true if the resource exists, false otherwise
     */
    public boolean exists(String reference, String basePath) {
        if (reference == null || reference.trim().isEmpty()) {
            return false;
        }
        try (InputStream is = resolve(reference, basePath)) {
            return is != null;
        } catch (ResourceNotFoundException | IOException e) {
            return false;
        }
    }

    // ========================================================================
    // Internal Resolution Methods
    // ========================================================================

    private InputStream tryClasspathThenFilesystem(String normalizedRef, String originalRef, String basePath) {
        // Try classpath first
        InputStream result = resolveFromClasspathInternal(normalizedRef);
        if (result != null) {
            logger.debug("Resolved '{}' from classpath", normalizedRef);
            return result;
        }

        // Try with original reference (might be different due to normalization)
        if (!normalizedRef.equals(originalRef)) {
            result = resolveFromClasspathInternal(originalRef);
            if (result != null) {
                logger.debug("Resolved '{}' from classpath (original ref)", originalRef);
                return result;
            }
        }

        // Fall back to filesystem
        result = resolveFromFilesystemInternal(normalizedRef);
        if (result != null) {
            logger.debug("Resolved '{}' from filesystem", normalizedRef);
            return result;
        }

        // Try filesystem with original reference
        if (!normalizedRef.equals(originalRef)) {
            result = resolveFromFilesystemInternal(originalRef);
            if (result != null) {
                logger.debug("Resolved '{}' from filesystem (original ref)", originalRef);
                return result;
            }
        }

        return null;
    }

    private InputStream tryFilesystemThenClasspath(String normalizedRef, String originalRef, String basePath) {
        // Try filesystem first
        InputStream result = resolveFromFilesystemInternal(normalizedRef);
        if (result != null) {
            logger.debug("Resolved '{}' from filesystem", normalizedRef);
            return result;
        }

        // Try with original reference
        if (!normalizedRef.equals(originalRef)) {
            result = resolveFromFilesystemInternal(originalRef);
            if (result != null) {
                logger.debug("Resolved '{}' from filesystem (original ref)", originalRef);
                return result;
            }
        }

        // Fall back to classpath
        result = resolveFromClasspathInternal(normalizedRef);
        if (result != null) {
            logger.debug("Resolved '{}' from classpath", normalizedRef);
            return result;
        }

        // Try classpath with original reference
        if (!normalizedRef.equals(originalRef)) {
            result = resolveFromClasspathInternal(originalRef);
            if (result != null) {
                logger.debug("Resolved '{}' from classpath (original ref)", originalRef);
                return result;
            }
        }

        return null;
    }

    private InputStream resolveFromClasspathInternal(String resourcePath) {
        // Strip leading slash if present (classpath resources don't use leading slash)
        String normalizedPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;

        // Try direct lookup
        InputStream result = classLoader.getResourceAsStream(normalizedPath);
        if (result != null) {
            return result;
        }

        // Try with each classpath prefix
        synchronized (classpathPrefixes) {
            for (String prefix : classpathPrefixes) {
                String prefixedPath = prefix + normalizedPath;
                result = classLoader.getResourceAsStream(prefixedPath);
                if (result != null) {
                    logger.debug("Found resource with prefix: {}", prefixedPath);
                    return result;
                }
            }
        }

        return null;
    }

    private InputStream resolveFromFilesystemInternal(String path) {
        // Try as absolute path first
        Path absolutePath = Paths.get(path);
        if (absolutePath.isAbsolute() && Files.exists(absolutePath)) {
            try {
                return new FileInputStream(absolutePath.toFile());
            } catch (FileNotFoundException e) {
                logger.debug("File not found at absolute path: {}", absolutePath);
            }
        }

        // Try relative to current working directory
        Path relativePath = Paths.get(path);
        if (Files.exists(relativePath)) {
            try {
                return new FileInputStream(relativePath.toFile());
            } catch (FileNotFoundException e) {
                logger.debug("File not found at relative path: {}", relativePath);
            }
        }

        // Try each search path
        synchronized (searchPaths) {
            for (String searchPath : searchPaths) {
                Path combinedPath = Paths.get(searchPath, path);
                if (Files.exists(combinedPath)) {
                    try {
                        logger.debug("Found file in search path: {}", combinedPath);
                        return new FileInputStream(combinedPath.toFile());
                    } catch (FileNotFoundException e) {
                        logger.debug("File not found at search path: {}", combinedPath);
                    }
                }
            }
        }

        return null;
    }

    // ========================================================================
    // Path Normalization
    // ========================================================================

    private String normalizeReference(String reference, String basePath) {
        // Handle explicit classpath prefix
        if (reference.startsWith("classpath:")) {
            return reference.substring(10); // Remove "classpath:" prefix
        }

        // Handle absolute paths (filesystem)
        if (isAbsolutePath(reference)) {
            return reference;
        }

        // Handle "./" prefix
        String normalizedRef = reference;
        if (normalizedRef.startsWith("./")) {
            normalizedRef = normalizedRef.substring(2);
        }

        // If no base path, return normalized reference
        if (basePath == null || basePath.isEmpty()) {
            return normalizedRef;
        }

        // Handle "./" prefix in base path
        String normalizedBase = basePath;
        if (normalizedBase.startsWith("./")) {
            normalizedBase = normalizedBase.substring(2);
        }

        // Ensure base path ends with separator
        if (!normalizedBase.endsWith("/") && !normalizedBase.endsWith("\\")) {
            normalizedBase = normalizedBase + "/";
        }

        // If reference already starts with base, return as-is
        if (normalizedRef.startsWith(normalizedBase)) {
            return normalizedRef;
        }

        return normalizedBase + normalizedRef;
    }

    private boolean isAbsolutePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        // Unix absolute path
        if (path.startsWith("/")) {
            return true;
        }
        // Windows absolute path (e.g., C:\, D:\)
        if (path.length() >= 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':') {
            return true;
        }
        return false;
    }

    private String normalizePath(String path) {
        // Normalize path separators to system default
        String normalized = path.replace('\\', '/');
        // Remove trailing separator
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeClasspathPrefix(String prefix) {
        // Normalize to forward slashes (classpath convention)
        String normalized = prefix.replace('\\', '/');
        // Ensure trailing slash
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        // Remove leading slash
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    // ========================================================================
    // Builder Pattern
    // ========================================================================

    /**
     * Create a new ResourceResolver builder.
     * 
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating ResourceResolver instances with fluent configuration.
     */
    public static class Builder {
        private ClassLoader classLoader;
        private ResolutionStrategy strategy = ResolutionStrategy.CLASSPATH_FIRST;
        private final List<String> searchPaths = new ArrayList<>();
        private final List<String> classpathPrefixes = new ArrayList<>();

        /**
         * Set the class loader for classpath resolution.
         * 
         * @param classLoader The class loader to use
         * @return This builder for chaining
         */
        public Builder classLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        /**
         * Set the resolution strategy.
         * 
         * @param strategy The resolution strategy
         * @return This builder for chaining
         */
        public Builder strategy(ResolutionStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        /**
         * Add a filesystem search path.
         * 
         * @param path The search path to add
         * @return This builder for chaining
         */
        public Builder addSearchPath(String path) {
            if (path != null && !path.trim().isEmpty()) {
                searchPaths.add(path);
            }
            return this;
        }

        /**
         * Add a classpath prefix.
         * 
         * @param prefix The classpath prefix to add
         * @return This builder for chaining
         */
        public Builder addClasspathPrefix(String prefix) {
            if (prefix != null && !prefix.trim().isEmpty()) {
                classpathPrefixes.add(prefix);
            }
            return this;
        }

        /**
         * Build the ResourceResolver instance.
         * 
         * @return A new ResourceResolver configured with this builder's settings
         */
        public ResourceResolver build() {
            ResourceResolver resolver = new ResourceResolver(classLoader);
            resolver.setResolutionStrategy(strategy);
            resolver.setSearchPaths(searchPaths);
            resolver.setClasspathPrefixes(classpathPrefixes);
            return resolver;
        }
    }
}
