package dev.mars.apex.core.config.component;

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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.apex.core.util.EnabledFilter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.mars.apex.core.config.exception.ResourceNotFoundException;
import dev.mars.apex.core.config.ResourceResolver;
import dev.mars.apex.core.config.exception.YamlConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Loader for APEX component configuration files.
 *
 * This class handles loading component YAML files, resolving nested component references,
 * detecting circular dependencies, and validating nesting depth limits.
 *
 * RESOURCE RESOLUTION (Phase 3):
 * - Uses {@link ResourceResolver} for unified classpath/filesystem resolution
 * - Supports loading from classpath with context tracking via {@code classpathBase}
 * - Maintains loading context through recursive component resolution
 * - Mixed mode: parent from filesystem can reference child from classpath (and vice versa)
 *
 * NESTING DEPTH LIMITS:
 * - Levels 1-2: Normal operation (no warnings)
 * - Levels 3-5: WARNING logs issued
 * - Level 6+: CRITICAL ERROR - fails to load
 *
 * CIRCULAR REFERENCE DETECTION:
 * - Uses DFS algorithm to detect cycles in component references
 * - Maintains recursion stack to track current path
 * - Works for both classpath and filesystem resources
 * - Fails fast on circular dependency detection
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-12
 */
public class ComponentLoader {

    private static final Logger logger = LoggerFactory.getLogger(ComponentLoader.class);
    private static final int MAX_NESTING_DEPTH = 5;
    private static final int WARNING_NESTING_DEPTH = 3;

    private final ObjectMapper yamlMapper;
    private final ResourceResolver resourceResolver;

    /**
     * Create a ComponentLoader with default ResourceResolver (CLASSPATH_FIRST strategy).
     */
    public ComponentLoader() {
        this(ResourceResolver.builder()
                .strategy(ResourceResolver.ResolutionStrategy.CLASSPATH_FIRST)
                .build());
    }

    /**
     * Create a ComponentLoader with a custom ResourceResolver.
     *
     * @param resourceResolver the resource resolver to use for file loading
     */
    public ComponentLoader(ResourceResolver resourceResolver) {
        this.yamlMapper = createYamlMapper();
        this.resourceResolver = resourceResolver != null 
                ? resourceResolver 
                : ResourceResolver.builder()
                        .strategy(ResourceResolver.ResolutionStrategy.CLASSPATH_FIRST)
                        .build();
    }

    /**
     * Create and configure the YAML ObjectMapper.
     * Uses same configuration as YamlConfigurationLoader for compatibility.
     *
     * @return Configured ObjectMapper for YAML processing
     */
    private ObjectMapper createYamlMapper() {
        YAMLFactory yamlFactory = new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR);

        ObjectMapper mapper = new ObjectMapper(yamlFactory);

        // Configure mapper for better handling of missing properties
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        return mapper;
    }

    /**
     * Load a component configuration from a file path.
     *
     * Supports both file system and classpath loading via ResourceResolver.
     *
     * @param componentFilePath the path to the component YAML file
     * @return the loaded component configuration
     * @throws YamlConfigurationException if loading fails
     */
    public ComponentConfiguration loadComponent(String componentFilePath) throws YamlConfigurationException {
        return loadComponent(componentFilePath, null);
    }

    /**
     * Load a component configuration from a file path with a classpath base context.
     *
     * When loading from classpath, the classpathBase provides context for resolving
     * relative file references within the component.
     *
     * @param componentFilePath the path to the component YAML file
     * @param classpathBase optional base path for classpath resolution (e.g., "config/components/")
     * @return the loaded component configuration
     * @throws YamlConfigurationException if loading fails
     */
    public ComponentConfiguration loadComponent(String componentFilePath, String classpathBase) 
            throws YamlConfigurationException {
        logger.info("Loading component from: {} (classpathBase: {})", componentFilePath, classpathBase);

        try {
            ComponentConfiguration component = loadComponentFile(componentFilePath, classpathBase);

            // Validate the component
            component.validate();

            // Detect circular references - need to pass classpathBase for proper resolution
            detectCircularReferences(component, componentFilePath, classpathBase);

            // Log enabled status
            boolean enabled = EnabledFilter.isEnabled(component);
            logger.info("Successfully loaded component: {} (id: {}, enabled: {})",
                       component.getName(), component.getId(), enabled);
            return component;

        } catch (Exception e) {
            throw new YamlConfigurationException("Failed to load component from: " + componentFilePath, e);
        }
    }

    /**
     * Load a component configuration from an InputStream.
     *
     * @param inputStream the input stream containing the YAML configuration
     * @return the loaded component configuration
     * @throws YamlConfigurationException if loading fails
     */
    public ComponentConfiguration loadComponent(InputStream inputStream) throws YamlConfigurationException {
        return loadComponent(inputStream, null);
    }

    /**
     * Load a component configuration from an InputStream with classpath base context.
     *
     * The classpathBase parameter is crucial for resolving relative file references
     * when the component is loaded from a classpath resource. For example, if loading
     * from "config/components/validation.yaml", the classpathBase should be "config/components/"
     * to properly resolve relative references like "rules/amount-validation.yaml".
     *
     * @param inputStream the input stream containing the YAML configuration
     * @param classpathBase base path for resolving relative references (e.g., "config/components/")
     * @return the loaded component configuration
     * @throws YamlConfigurationException if loading fails
     */
    public ComponentConfiguration loadComponent(InputStream inputStream, String classpathBase) 
            throws YamlConfigurationException {
        logger.info("Loading component from InputStream (classpathBase: {})", classpathBase);

        try {
            ComponentConfiguration component = yamlMapper.readValue(inputStream, ComponentConfiguration.class);

            // Validate the component
            component.validate();

            // For stream-loaded components, we can't detect circular references without a file path
            // The caller should ensure the component structure is valid
            
            boolean enabled = EnabledFilter.isEnabled(component);
            logger.info("Successfully loaded component from stream: {} (id: {}, enabled: {})",
                       component.getName(), component.getId(), enabled);
            return component;

        } catch (IOException e) {
            throw new YamlConfigurationException("Failed to load component from InputStream", e);
        }
    }

    /**
     * Load a component configuration from a classpath resource.
     *
     * This is a convenience method that derives the classpathBase automatically
     * from the resource path.
     *
     * @param resourcePath the classpath resource path (e.g., "config/components/validation.yaml")
     * @return the loaded component configuration
     * @throws YamlConfigurationException if loading fails
     */
    public ComponentConfiguration loadComponentFromClasspath(String resourcePath) throws YamlConfigurationException {
        String classpathBase = resourceResolver.getClasspathBase(resourcePath);
        logger.debug("Loading component from classpath: {} (derived classpathBase: {})", resourcePath, classpathBase);
        return loadComponent(resourcePath, classpathBase);
    }

    /**
     * Check if a component is enabled.
     *
     * @param component the component to check
     * @return true if enabled (default), false if explicitly disabled
     */
    public boolean isComponentEnabled(ComponentConfiguration component) {
        return EnabledFilter.isEnabled(component);
    }

    /**
     * Load component file using ResourceResolver for unified file/classpath resolution.
     *
     * @param filePath the file path or classpath resource path
     * @param classpathBase optional base path for relative classpath resolution
     * @return the loaded component configuration
     * @throws IOException if the file cannot be loaded
     */
    private ComponentConfiguration loadComponentFile(String filePath, String classpathBase) throws IOException {
        logger.debug("Loading component file: {} (classpathBase: {})", filePath, classpathBase);
        
        try (InputStream is = resourceResolver.resolve(filePath, classpathBase)) {
            return yamlMapper.readValue(is, ComponentConfiguration.class);
        } catch (ResourceNotFoundException e) {
            throw new IOException("Component file not found: " + filePath + 
                    (classpathBase != null ? " (classpathBase: " + classpathBase + ")" : ""), e);
        }
    }

    /**
     * Resolve all file references from a component, including nested components.
     *
     * This method recursively expands component references and returns a flat list
     * of all configuration files in the correct execution order.
     *
     * @param component the component to resolve
     * @param componentFilePath the path to the component file (for relative path resolution)
     * @return list of all resolved file paths in execution order
     * @throws YamlConfigurationException if resolution fails
     */
    public List<ResolvedFileReference> resolveAllReferences(ComponentConfiguration component, String componentFilePath)
            throws YamlConfigurationException, IOException {
        return resolveAllReferences(component, componentFilePath, null);
    }

    /**
     * Resolve all file references from a component with classpath context tracking.
     *
     * This method recursively expands component references and returns a flat list
     * of all configuration files in the correct execution order. When a classpathBase
     * is provided, relative references are resolved within that classpath context.
     *
     * @param component the component to resolve
     * @param componentFilePath the path to the component file (for relative path resolution)
     * @param classpathBase optional base path for classpath resolution
     * @return list of all resolved file paths in execution order
     * @throws YamlConfigurationException if resolution fails
     */
    public List<ResolvedFileReference> resolveAllReferences(
            ComponentConfiguration component, 
            String componentFilePath,
            String classpathBase)
            throws YamlConfigurationException, IOException {
        
        Set<String> visitedComponents = new HashSet<>();
        List<ResolvedFileReference> resolvedFiles = new ArrayList<>();
        
        resolveReferencesRecursive(component, componentFilePath, classpathBase, 
                visitedComponents, resolvedFiles, 1);
        
        return resolvedFiles;
    }

    /**
     * Recursively resolve component references with nesting depth and classpath context tracking.
     * Disabled components are skipped during resolution.
     */
    private void resolveReferencesRecursive(
            ComponentConfiguration component,
            String componentFilePath,
            String classpathBase,
            Set<String> visitedComponents,
            List<ResolvedFileReference> resolvedFiles,
            int depth) throws YamlConfigurationException, IOException {

        // Skip disabled components
        if (!isComponentEnabled(component)) {
            logger.debug("Skipping disabled component: {} (id: {})", componentFilePath, component.getId());
            return;
        }

        // Check nesting depth
        validateNestingDepth(component.getId(), depth);

        // Mark this component as visited
        visitedComponents.add(componentFilePath);

        // Get all references in execution order
        List<ComponentConfiguration.FileReference> allRefs = component.getAllReferences();

        for (ComponentConfiguration.FileReference ref : allRefs) {
            String filePath = ref.getFile();
            String resolvedPath = resolveRelativePath(componentFilePath, filePath, classpathBase);
            
            // Derive new classpathBase for the resolved file (for nested components)
            String newClasspathBase = resourceResolver.getClasspathBase(resolvedPath);

            // Check if this is a component reference
            if (isComponentFile(resolvedPath, newClasspathBase)) {
                // Load and recursively resolve nested component
                ComponentConfiguration nestedComponent = loadComponentFile(resolvedPath, newClasspathBase);
                resolveReferencesRecursive(nestedComponent, resolvedPath, newClasspathBase,
                        visitedComponents, resolvedFiles, depth + 1);
            } else {
                // Add regular file reference
                resolvedFiles.add(new ResolvedFileReference(
                    resolvedPath,
                    ref.getFailurePolicy(),
                    depth
                ));
            }
        }
    }

    /**
     * Detect circular references in component dependencies.
     *
     * Uses DFS algorithm with recursion stack to detect cycles.
     *
     * @param component the component to check
     * @param componentFilePath the path to the component file
     * @throws YamlConfigurationException if circular reference detected
     */
    public void detectCircularReferences(ComponentConfiguration component, String componentFilePath) 
            throws YamlConfigurationException {
        detectCircularReferences(component, componentFilePath, null);
    }

    /**
     * Detect circular references in component dependencies with classpath context.
     *
     * Uses DFS algorithm with recursion stack to detect cycles.
     *
     * @param component the component to check
     * @param componentFilePath the path to the component file
     * @param classpathBase optional base path for classpath resolution
     * @throws YamlConfigurationException if circular reference detected
     */
    public void detectCircularReferences(ComponentConfiguration component, String componentFilePath, String classpathBase) 
            throws YamlConfigurationException {
        
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        List<String> path = new ArrayList<>();
        
        if (hasCircularReference(component, componentFilePath, classpathBase, visited, recursionStack, path)) {
            throw new YamlConfigurationException(
                "Circular component reference detected: " + String.join(" -> ", path)
            );
        }
    }

    /**
     * DFS-based circular reference detection with classpath context tracking.
     */
    private boolean hasCircularReference(
            ComponentConfiguration component,
            String componentFilePath,
            String classpathBase,
            Set<String> visited,
            Set<String> recursionStack,
            List<String> path) throws YamlConfigurationException {
        
        // Check if we're in a cycle
        if (recursionStack.contains(componentFilePath)) {
            path.add(componentFilePath);
            return true;
        }
        
        // Already processed this component
        if (visited.contains(componentFilePath)) {
            return false;
        }
        
        // Mark as visited and add to recursion stack
        visited.add(componentFilePath);
        recursionStack.add(componentFilePath);
        path.add(componentFilePath);
        
        // Check all component references
        for (ComponentConfiguration.FileReference ref : component.getComponentRefs()) {
            String resolvedPath = resolveRelativePath(componentFilePath, ref.getFile(), classpathBase);
            String newClasspathBase = resourceResolver.getClasspathBase(resolvedPath);
            
            try {
                ComponentConfiguration nestedComponent = loadComponentFile(resolvedPath, newClasspathBase);
                if (hasCircularReference(nestedComponent, resolvedPath, newClasspathBase, visited, recursionStack, path)) {
                    return true;
                }
            } catch (IOException e) {
                throw new YamlConfigurationException("Failed to load component for circular reference check: " + resolvedPath, e);
            }
        }
        
        // Remove from recursion stack and path
        recursionStack.remove(componentFilePath);
        path.remove(path.size() - 1);
        
        return false;
    }

    /**
     * Validate nesting depth and issue warnings/errors.
     */
    private void validateNestingDepth(String componentId, int depth) throws YamlConfigurationException {
        if (depth > MAX_NESTING_DEPTH) {
            throw new YamlConfigurationException(
                "CRITICAL ERROR: Component nesting depth exceeds maximum of " + MAX_NESTING_DEPTH + 
                " levels. Component '" + componentId + "' is at depth " + depth + 
                ". Nesting beyond " + MAX_NESTING_DEPTH + " levels is not supported."
            );
        } else if (depth >= WARNING_NESTING_DEPTH) {
            logger.warn(
                "WARNING: Component '{}' is nested at depth {}. " +
                "Nesting depths 3-5 may impact performance and maintainability. " +
                "Consider flattening the component structure.",
                componentId, depth
            );
        }
    }

    /**
     * Check if a file is a component file by loading and checking its type.
     *
     * @param filePath the file path to check
     * @param classpathBase optional classpath base for resolution
     * @return true if the file is a component file
     */
    private boolean isComponentFile(String filePath, String classpathBase) throws IOException {
        try {
            ComponentConfiguration component = loadComponentFile(filePath, classpathBase);
            return "component".equals(component.getType());
        } catch (Exception e) {
            // If we can't load it as a component, it's not a component file
            return false;
        }
    }

    /**
     * Resolve relative file paths using ResourceResolver.
     *
     * The resolution follows this order:
     * 1. If the referenced file is absolute, return it as-is
     * 2. If the referenced file starts with "src/" (project-relative), return as-is
     * 3. If the referenced file contains path separators and is not a simple filename, return as-is
     * 4. Otherwise, resolve relative to the component file location using ResourceResolver
     *
     * @param componentFilePath the path to the component file
     * @param referencedFile the file reference from the component
     * @param classpathBase optional classpath base for resolution
     * @return the resolved path
     */
    private String resolveRelativePath(String componentFilePath, String referencedFile, String classpathBase) {
        // Handle explicit classpath prefix
        if (referencedFile.startsWith("classpath:")) {
            return referencedFile.substring(10); // Remove "classpath:" prefix
        }
        
        // Check if the referenced file is already absolute
        if (isAbsolutePath(referencedFile)) {
            return referencedFile;
        }
        
        // Check if it starts with a known project root
        if (referencedFile.startsWith("src/") || referencedFile.startsWith("src\\")) {
            return referencedFile;
        }

        // Check if the referenced file contains path separators (not a simple filename)
        // and is not a relative path like "../something" or "./something"
        if ((referencedFile.contains("/") || referencedFile.contains("\\"))
                && !referencedFile.startsWith("../") && !referencedFile.startsWith("./")
                && !referencedFile.startsWith("..\\") && !referencedFile.startsWith(".\\")) {
            return referencedFile;
        }

        // Use ResourceResolver for relative path resolution
        return resourceResolver.resolveRelativePath(referencedFile, getBasePath(componentFilePath, classpathBase));
    }

    /**
     * Get the base path for relative reference resolution.
     *
     * @param componentFilePath the component file path
     * @param classpathBase optional classpath base
     * @return the base path for resolution
     */
    private String getBasePath(String componentFilePath, String classpathBase) {
        // If we have a classpathBase, use it
        if (classpathBase != null && !classpathBase.isEmpty()) {
            return classpathBase;
        }
        
        // Otherwise derive from component file path
        return resourceResolver.getClasspathBase(componentFilePath);
    }

    /**
     * Check if a path is absolute.
     */
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

    /**
     * Get the ResourceResolver used by this loader.
     *
     * @return the resource resolver
     */
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    /**
     * Represents a resolved file reference with its failure policy and nesting depth.
     */
    public static class ResolvedFileReference {
        private final String filePath;
        private final String failurePolicy;
        private final int nestingDepth;

        public ResolvedFileReference(String filePath, String failurePolicy, int nestingDepth) {
            this.filePath = filePath;
            this.failurePolicy = failurePolicy;
            this.nestingDepth = nestingDepth;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getFailurePolicy() {
            return failurePolicy;
        }

        public int getNestingDepth() {
            return nestingDepth;
        }

        @Override
        public String toString() {
            return "ResolvedFileReference{" +
                    "filePath='" + filePath + '\'' +
                    ", failurePolicy='" + failurePolicy + '\'' +
                    ", nestingDepth=" + nestingDepth +
                    '}';
        }
    }
}

