package dev.mars.apex.core.util;

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


import dev.mars.apex.core.config.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility class for analyzing YAML file dependencies.
 * 
 * This analyzer traces the complete chain of YAML file dependencies starting
 * from a scenario file, following all references through rule configuration
 * files, enrichment files, and other YAML configurations.
 * 
 * ANALYSIS PROCESS:
 * 1. Parse the root YAML file (typically a scenario file)
 * 2. Extract references to other YAML files
 * 3. Recursively analyze referenced files
 * 4. Build complete dependency graph
 * 5. Validate all dependencies and detect issues
 * 
 * SUPPORTED REFERENCE PATTERNS:
 * - rule-configurations: [list of files]
 * - rule-chains: [list of files]
 * - enrichment-refs: [list of files]
 * - config-files: [list of files]
 * - include/import statements
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-02
 */
public class DependencyAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(DependencyAnalyzer.class);
    
    // Pattern to match YAML file references
    private static final Pattern YAML_FILE_PATTERN = Pattern.compile(".*\\.ya?ml$");
    
    // Keys that typically contain file references
    private static final Set<String> FILE_REFERENCE_KEYS = Set.of(
        "rule-configurations",
        "rule-chains",
        "enrichment-refs",
        "component-refs",
        "config-files",
        "config-file",
        "file",  // Used in component-refs, enrichment-refs, etc. for individual file references
        "include",
        "import",
        "source-config",
        "lookup-config"
    );
    
    private final ConfigurationLoader configLoader;
    private final String basePath;
    
    public DependencyAnalyzer() {
        this("apex-demo/src/main/resources");
    }
    
    public DependencyAnalyzer(String basePath) {
        this.configLoader = new ConfigurationLoader();
        this.basePath = basePath;
    }
    
    /**
     * Analyzes YAML dependencies starting from the specified file.
     * 
     * @param yamlFilePath path to the root YAML file to analyze
     * @return complete dependency graph
     */
    public DependencyGraph analyzeYamlDependencies(String yamlFilePath) {
        logger.info("Starting YAML dependency analysis for: {}", yamlFilePath);
        
        DependencyGraph graph = new DependencyGraph(yamlFilePath);
        Set<String> visited = new HashSet<>();
        
        try {
            analyzeFileRecursively(yamlFilePath, graph, visited, 0);
            logger.info("Completed YAML dependency analysis. Found {} files, max depth: {}", 
                graph.getTotalFiles(), graph.getMaxDepth());
        } catch (Exception e) {
            logger.error("Failed to analyze YAML dependencies for: {}: {}", yamlFilePath, e.getMessage());
            logger.debug("Full exception details:", e);
            throw new IllegalStateException("YAML dependency analysis failed for: " + yamlFilePath, e);
        }
        
        return graph;
    }
    
    /**
     * Recursively analyzes a YAML file and its dependencies.
     */
    private void analyzeFileRecursively(String filePath, DependencyGraph graph,
                                       Set<String> visited, int depth) {

        // Avoid infinite recursion
        if (visited.contains(filePath)) {
            logger.debug("Already visited file: {}", filePath);
            return;
        }

        visited.add(filePath);
        graph.updateMaxDepth(depth);

        logger.debug("Analyzing YAML file at depth {}: {}", depth, filePath);

        // Create node for this file
        Node node = createYamlNode(filePath);
        graph.addNode(node);

        // Check component nesting depth and issue warnings
        if (node.getFileType() == YamlFileType.COMPONENT) {
            checkComponentNestingDepth(filePath, depth);
        }
        
        // If file doesn't exist or is invalid, stop here
        if (!node.exists() || !node.isYamlValid()) {
            logger.warn("Skipping analysis of invalid/missing file: {}", filePath);
            return;
        }
        
        // Extract YAML references from this file
        List<String> referencedFiles = extractYamlReferences(filePath);
        node.setReferencedFiles(referencedFiles);
        
        // Recursively analyze referenced files
        for (String referencedFile : referencedFiles) {
            // Resolve referenced file path relative to the parent file's directory
            String resolvedPath = resolveReferencedFilePath(filePath, referencedFile);

            // Add dependency edge
            Dependency dependency = new Dependency(filePath, resolvedPath, "yaml-reference");
            graph.addDependency(dependency);

            // Recursively analyze the referenced file
            analyzeFileRecursively(resolvedPath, graph, visited, depth + 1);
        }
    }
    
    /**
     * Creates a Node for the specified file.
     */
    private Node createYamlNode(String filePath) {
        Node node = new Node(filePath);
        
        // Check if file exists
        File file = new File(basePath, filePath);
        node.setExists(file.exists());
        
        if (node.exists()) {
            // Determine file type based on path and content
            node.setFileType(determineFileType(filePath));

            // Validate YAML syntax
            try {
                String fullPath = new File(basePath, filePath).getAbsolutePath();
                configLoader.loadAsMap(fullPath);
                node.setYamlValid(true);
            } catch (Exception e) {
                logger.warn("Invalid YAML syntax in file: {}", filePath);
                logger.debug("YAML parsing error details", e);
                node.setYamlValid(false);
            }
        }
        
        return node;
    }
    
    /**
     * Extracts YAML file references from the specified file.
     */
    private List<String> extractYamlReferences(String filePath) {
        List<String> references = new ArrayList<>();
        
        try {
            String fullPath = new File(basePath, filePath).getAbsolutePath();
            Map<String, Object> config = configLoader.loadAsMap(fullPath);
            extractReferencesFromMap(config, references, "");
        } catch (Exception e) {
            logger.error("Failed to extract references from: {}: {}", filePath, e.getMessage());
            logger.debug("Full exception details:", e);
        }
        
        return references;
    }

    /**
     * Resolves a referenced file path relative to the parent file's directory.
     *
     * @param parentFilePath The path of the file containing the reference
     * @param referencedFile The referenced file (may be just a filename or a relative path)
     * @return The resolved path relative to the base directory
     */
    private String resolveReferencedFilePath(String parentFilePath, String referencedFile) {
        // If the referenced file is already an absolute path or contains directory separators,
        // assume it's already properly resolved
        if (referencedFile.contains("/") || referencedFile.contains("\\")) {
            return referencedFile;
        }

        // Get the parent file's directory
        int lastSeparator = Math.max(parentFilePath.lastIndexOf('/'), parentFilePath.lastIndexOf('\\'));
        if (lastSeparator > 0) {
            String parentDir = parentFilePath.substring(0, lastSeparator);
            return parentDir + "/" + referencedFile;
        }

        // If parent file has no directory component, just return the referenced file as-is
        return referencedFile;
    }

    /**
     * Recursively extracts file references from a YAML configuration map.
     */
    @SuppressWarnings("unchecked")
    private void extractReferencesFromMap(Map<String, Object> map, List<String> references, String path) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String currentPath = path.isEmpty() ? key : path + "." + key;
            
            if (FILE_REFERENCE_KEYS.contains(key) && value instanceof List) {
                // Handle list of file references (can be strings or maps with 'file' field)
                List<Object> list = (List<Object>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof String) {
                        String fileRef = (String) item;
                        if (YAML_FILE_PATTERN.matcher(fileRef).matches()) {
                            references.add(fileRef);
                            logger.debug("Found YAML reference at {}: {}", currentPath, fileRef);
                        }
                    } else if (item instanceof Map) {
                        // Recursively process map items (e.g., component-refs, config-files with execution-order)
                        extractReferencesFromMap((Map<String, Object>) item, references, currentPath + "[" + i + "]");
                    }
                }
            } else if (FILE_REFERENCE_KEYS.contains(key) && value instanceof String) {
                // Handle single file reference
                String fileRef = (String) value;
                if (YAML_FILE_PATTERN.matcher(fileRef).matches()) {
                    references.add(fileRef);
                    logger.debug("Found YAML reference at {}: {}", currentPath, fileRef);
                }
            } else if (value instanceof Map) {
                // Recursively process nested maps
                extractReferencesFromMap((Map<String, Object>) value, references, currentPath);
            } else if (value instanceof List) {
                // Process lists that might contain maps
                List<Object> list = (List<Object>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        extractReferencesFromMap((Map<String, Object>) item, references, currentPath + "[" + i + "]");
                    }
                }
            }
        }
    }
    
    /**
     * Determines the file type based on the file path and content.
     * For component files, reads the metadata.type field to accurately detect them.
     */
    private YamlFileType determineFileType(String filePath) {
        // Try to read metadata.type from the file for accurate detection
        try {
            String fullPath = new File(basePath, filePath).getAbsolutePath();
            Map<String, Object> config = configLoader.loadAsMap(fullPath);
            if (config != null && config.containsKey("metadata")) {
                Map<String, Object> metadata = (Map<String, Object>) config.get("metadata");
                if (metadata != null && metadata.containsKey("type")) {
                    String type = (String) metadata.get("type");
                    if ("component".equals(type)) {
                        return YamlFileType.COMPONENT;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not read metadata.type from {}, falling back to path-based detection", filePath);
        }

        // Fall back to path-based detection
        if (filePath.contains("scenarios/")) {
            return YamlFileType.SCENARIO;
        } else if (filePath.contains("components/")) {
            return YamlFileType.COMPONENT;
        } else if (filePath.contains("bootstrap/")) {
            return YamlFileType.RULE_CONFIG;
        } else if (filePath.contains("enrichments/")) {
            return YamlFileType.ENRICHMENT;
        } else if (filePath.contains("rule-chains/")) {
            return YamlFileType.RULE_CHAIN;
        } else if (filePath.contains("datasets/")) {
            return YamlFileType.DATASET;
        } else {
            return YamlFileType.RULE_CONFIG; // Default
        }
    }
    

    /**
     * Generates a text report of the dependency analysis.
     */
    public String generateTextReport(DependencyGraph graph) {
        StringBuilder report = new StringBuilder();
        
        report.append("YAML Dependency Analysis for: ").append(graph.getRootFile()).append("\n\n");
        
        // Summary
        report.append("Summary:\n");
        report.append("├── Total YAML Files: ").append(graph.getTotalFiles()).append("\n");
        report.append("├── Max Depth: ").append(graph.getMaxDepth()).append("\n");
        report.append("├── Missing Files: ").append(graph.getMissingFiles().size()).append("\n");
        report.append("└── Invalid YAML Files: ").append(graph.getInvalidYamlFiles().size()).append("\n\n");
        
        // Dependency tree
        report.append("Dependency Tree:\n");
        generateTreeReport(graph, graph.getRootFile(), report, "", new HashSet<>());
        
        // Missing files
        if (!graph.getMissingFiles().isEmpty()) {
            report.append("\nMissing Files:\n");
            for (String missingFile : graph.getMissingFiles()) {
                report.append("✗ ").append(missingFile).append("\n");
            }
        }
        
        // Invalid YAML files
        if (!graph.getInvalidYamlFiles().isEmpty()) {
            report.append("\nInvalid YAML Files:\n");
            for (String invalidFile : graph.getInvalidYamlFiles()) {
                report.append("⚠ ").append(invalidFile).append("\n");
            }
        }
        
        return report.toString();
    }
    
    /**
     * Recursively generates the tree structure for the report.
     */
    private void generateTreeReport(DependencyGraph graph, String filePath, 
                                   StringBuilder report, String prefix, Set<String> visited) {
        
        if (visited.contains(filePath)) {
            report.append(prefix).append("└── ").append(filePath).append(" (circular reference)\n");
            return;
        }
        
        visited.add(filePath);
        
        Node node = graph.getNode(filePath);
        String status = node != null && node.exists() ? "[OK]" : "✗";
        
        report.append(prefix).append("└── ").append(filePath).append(" ").append(status).append("\n");
        
        if (node != null && node.getReferencedFiles() != null) {
            List<String> references = node.getReferencedFiles();
            for (int i = 0; i < references.size(); i++) {
                String referencedFile = references.get(i);
                // Use the referenced file path directly (already resolved during analysis)
                boolean isLast = (i == references.size() - 1);
                String newPrefix = prefix + (isLast ? "    " : "│   ");

                generateTreeReport(graph, referencedFile, report, newPrefix, new HashSet<>(visited));
            }
        }
    }

    /**
     * Checks component nesting depth and issues appropriate warnings.
     *
     * Nesting depth policy:
     * - Levels 1-2: Normal operation (no warnings)
     * - Levels 3-5: Log WARNING
     * - Level 6+: Log CRITICAL ERROR and throw exception
     */
    private void checkComponentNestingDepth(String filePath, int depth) {
        if (depth >= 6) {
            String errorMsg = String.format(
                "Component nesting depth exceeded maximum allowed (6+). " +
                "Component '%s' is at depth %d. This indicates excessive nesting and may cause " +
                "performance issues or circular dependencies. Please refactor to reduce nesting depth.",
                filePath, depth
            );
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        } else if (depth >= 3) {
            logger.warn(
                "Component nesting depth warning: Component '{}' is at depth {}. " +
                "Consider refactoring if depth exceeds 5 to avoid complexity and performance issues.",
                filePath, depth
            );
        }
        // Levels 1-2: No warning needed
    }
}
