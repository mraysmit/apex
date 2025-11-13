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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Loader for APEX component configuration files.
 *
 * This class handles loading component YAML files, resolving nested component references,
 * detecting circular dependencies, and validating nesting depth limits.
 *
 * NESTING DEPTH LIMITS:
 * - Levels 1-2: Normal operation (no warnings)
 * - Levels 3-5: WARNING logs issued
 * - Level 6+: CRITICAL ERROR - fails to load
 *
 * CIRCULAR REFERENCE DETECTION:
 * - Uses DFS algorithm to detect cycles in component references
 * - Maintains recursion stack to track current path
 * - Fails fast on circular dependency detection
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.2.0
 */
public class ComponentLoader {

    private static final Logger logger = LoggerFactory.getLogger(ComponentLoader.class);
    private static final int MAX_NESTING_DEPTH = 5;
    private static final int WARNING_NESTING_DEPTH = 3;

    private final ObjectMapper yamlMapper;

    public ComponentLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * Load a component configuration from a file path.
     *
     * Supports both file system and classpath loading.
     *
     * @param componentFilePath the path to the component YAML file
     * @return the loaded component configuration
     * @throws YamlConfigurationException if loading fails
     */
    public ComponentConfiguration loadComponent(String componentFilePath) throws YamlConfigurationException {
        logger.info("Loading component from: {}", componentFilePath);

        try {
            ComponentConfiguration component = loadComponentFile(componentFilePath);
            
            // Validate the component
            component.validate();
            
            // Detect circular references
            detectCircularReferences(component, componentFilePath);
            
            logger.info("Successfully loaded component: {} (id: {})", component.getName(), component.getId());
            return component;
            
        } catch (Exception e) {
            throw new YamlConfigurationException("Failed to load component from: " + componentFilePath, e);
        }
    }

    /**
     * Load component file from file system or classpath.
     */
    private ComponentConfiguration loadComponentFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        
        if (Files.exists(path)) {
            // Load from file system
            logger.debug("Loading component from file system: {}", filePath);
            return yamlMapper.readValue(path.toFile(), ComponentConfiguration.class);
        } else {
            // Load from classpath
            logger.debug("Loading component from classpath: {}", filePath);
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(filePath)) {
                if (is == null) {
                    throw new IOException("Component file not found: " + filePath);
                }
                return yamlMapper.readValue(is, ComponentConfiguration.class);
            }
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
        
        Set<String> visitedComponents = new HashSet<>();
        List<ResolvedFileReference> resolvedFiles = new ArrayList<>();
        
        resolveReferencesRecursive(component, componentFilePath, visitedComponents, resolvedFiles, 1);
        
        return resolvedFiles;
    }

    /**
     * Recursively resolve component references with nesting depth tracking.
     */
    private void resolveReferencesRecursive(
            ComponentConfiguration component,
            String componentFilePath,
            Set<String> visitedComponents,
            List<ResolvedFileReference> resolvedFiles,
            int depth) throws YamlConfigurationException, IOException {
        
        // Check nesting depth
        validateNestingDepth(component.getId(), depth);
        
        // Mark this component as visited
        visitedComponents.add(componentFilePath);
        
        // Get all references in execution order
        List<ComponentConfiguration.FileReference> allRefs = component.getAllReferences();
        
        for (ComponentConfiguration.FileReference ref : allRefs) {
            String filePath = ref.getFile();
            String resolvedPath = resolveRelativePath(componentFilePath, filePath);
            
            // Check if this is a component reference
            if (isComponentFile(resolvedPath)) {
                // Load and recursively resolve nested component
                ComponentConfiguration nestedComponent = loadComponentFile(resolvedPath);
                resolveReferencesRecursive(nestedComponent, resolvedPath, visitedComponents, resolvedFiles, depth + 1);
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
        
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        List<String> path = new ArrayList<>();
        
        if (hasCircularReference(component, componentFilePath, visited, recursionStack, path)) {
            throw new YamlConfigurationException(
                "Circular component reference detected: " + String.join(" -> ", path)
            );
        }
    }

    /**
     * DFS-based circular reference detection.
     */
    private boolean hasCircularReference(
            ComponentConfiguration component,
            String componentFilePath,
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
            String resolvedPath = resolveRelativePath(componentFilePath, ref.getFile());
            
            try {
                ComponentConfiguration nestedComponent = loadComponentFile(resolvedPath);
                if (hasCircularReference(nestedComponent, resolvedPath, visited, recursionStack, path)) {
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
     */
    private boolean isComponentFile(String filePath) throws IOException {
        try {
            ComponentConfiguration component = loadComponentFile(filePath);
            return "component".equals(component.getType());
        } catch (Exception e) {
            // If we can't load it as a component, it's not a component file
            return false;
        }
    }

    /**
     * Resolve relative file paths based on the component file location.
     * If the referenced file is already absolute or starts with a known root (like "src/"),
     * return it as-is without resolving.
     */
    private String resolveRelativePath(String componentFilePath, String referencedFile) {
        // Check if the referenced file is already absolute or starts with a known root
        Path referencedPath = Paths.get(referencedFile);
        if (referencedPath.isAbsolute() || referencedFile.startsWith("src/") || referencedFile.startsWith("src\\")) {
            return referencedFile;
        }

        // Check if the referenced file is already a full classpath path
        // (e.g., "dev/mars/apex/demo/scenario/file.yaml" when component is also in "dev/mars/apex/demo/scenario/")
        // If the referenced file contains a path separator, treat it as an absolute classpath path
        if (referencedFile.contains("/") || referencedFile.contains("\\")) {
            return referencedFile;
        }

        // Only resolve relative paths (e.g., "file.yaml" or "../other/file.yaml")
        Path componentPath = Paths.get(componentFilePath).getParent();
        if (componentPath == null) {
            return referencedFile;
        }

        Path resolvedPath = componentPath.resolve(referencedFile).normalize();
        return resolvedPath.toString();
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

