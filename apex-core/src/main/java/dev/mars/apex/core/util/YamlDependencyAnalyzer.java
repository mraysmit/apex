package dev.mars.apex.core.util;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
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
 * @since 1.0.0
 */
public class YamlDependencyAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(YamlDependencyAnalyzer.class);
    
    // Pattern to match YAML file references
    private static final Pattern YAML_FILE_PATTERN = Pattern.compile(".*\\.ya?ml$");
    
    // Keys that typically contain file references
    private static final Set<String> FILE_REFERENCE_KEYS = Set.of(
        "rule-configurations",
        "rule-chains", 
        "enrichment-refs",
        "config-files",
        "include",
        "import",
        "source-config",
        "lookup-config"
    );
    
    private final YamlConfigurationLoader configLoader;
    private final String basePath;
    
    public YamlDependencyAnalyzer() {
        this("apex-demo/src/main/resources");
    }
    
    public YamlDependencyAnalyzer(String basePath) {
        this.configLoader = new YamlConfigurationLoader();
        this.basePath = basePath;
    }
    
    /**
     * Analyzes YAML dependencies starting from the specified file.
     * 
     * @param yamlFilePath path to the root YAML file to analyze
     * @return complete dependency graph
     */
    public YamlDependencyGraph analyzeYamlDependencies(String yamlFilePath) {
        logger.info("Starting YAML dependency analysis for: {}", yamlFilePath);
        
        YamlDependencyGraph graph = new YamlDependencyGraph(yamlFilePath);
        Set<String> visited = new HashSet<>();
        
        try {
            analyzeFileRecursively(yamlFilePath, graph, visited, 0);
            logger.info("Completed YAML dependency analysis. Found {} files, max depth: {}", 
                graph.getTotalFiles(), graph.getMaxDepth());
        } catch (Exception e) {
            logger.error("Failed to analyze YAML dependencies for: {}", yamlFilePath, e);
            throw new RuntimeException("YAML dependency analysis failed", e);
        }
        
        return graph;
    }
    
    /**
     * Recursively analyzes a YAML file and its dependencies.
     */
    private void analyzeFileRecursively(String filePath, YamlDependencyGraph graph, 
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
        YamlNode node = createYamlNode(filePath);
        graph.addNode(node);
        
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
            // For now, treat all references as relative to the base directory
            // In a more sophisticated implementation, we could resolve relative to the referencing file
            String resolvedPath = referencedFile;

            // Add dependency edge
            YamlDependency dependency = new YamlDependency(filePath, resolvedPath, "yaml-reference");
            graph.addDependency(dependency);

            // Recursively analyze the referenced file
            analyzeFileRecursively(resolvedPath, graph, visited, depth + 1);
        }
    }
    
    /**
     * Creates a YamlNode for the specified file.
     */
    private YamlNode createYamlNode(String filePath) {
        YamlNode node = new YamlNode(filePath);
        
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
            logger.error("Failed to extract references from: {}", filePath, e);
        }
        
        return references;
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
                // Handle list of file references
                List<Object> list = (List<Object>) value;
                for (Object item : list) {
                    if (item instanceof String) {
                        String fileRef = (String) item;
                        if (YAML_FILE_PATTERN.matcher(fileRef).matches()) {
                            references.add(fileRef);
                            logger.debug("Found YAML reference at {}: {}", currentPath, fileRef);
                        }
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
     * Determines the file type based on the file path.
     */
    private YamlFileType determineFileType(String filePath) {
        if (filePath.contains("scenarios/")) {
            return YamlFileType.SCENARIO;
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
    public String generateTextReport(YamlDependencyGraph graph) {
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
    private void generateTreeReport(YamlDependencyGraph graph, String filePath, 
                                   StringBuilder report, String prefix, Set<String> visited) {
        
        if (visited.contains(filePath)) {
            report.append(prefix).append("└── ").append(filePath).append(" (circular reference)\n");
            return;
        }
        
        visited.add(filePath);
        
        YamlNode node = graph.getNode(filePath);
        String status = node != null && node.exists() ? "✓" : "✗";
        
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
}
