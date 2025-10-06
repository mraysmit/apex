package dev.mars.apex.compiler.dependency;

import dev.mars.apex.compiler.lexical.ApexYamlLexicalValidator;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * APEX YAML Dependency Chain Analyzer.
 * 
 * Analyzes dependency relationships between APEX YAML files and provides
 * comprehensive validation with dependency resolution.
 * 
 * Features:
 * 1. Dependency Graph Construction
 * 2. Circular Dependency Detection
 * 3. Cascading Validation
 * 4. Root Cause Analysis
 * 5. Dependency-Aware Error Reporting
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-07
 * @version 1.0
 */
public class ApexDependencyAnalyzer {
    
    private final ApexYamlLexicalValidator validator;
    private final Yaml yaml;
    
    public ApexDependencyAnalyzer() {
        this.validator = new ApexYamlLexicalValidator();
        this.yaml = new Yaml();
    }
    
    /**
     * Comprehensive validation result with dependency analysis
     */
    public static class DependencyValidationResult {
        private final boolean isValid;
        private final List<String> errors;
        private final List<String> warnings;
        private final Map<String, List<String>> dependencies;
        private final List<String> circularDependencies;
        private final Map<String, ApexYamlLexicalValidator.ValidationResult> fileResults;
        private final List<String> rootCauses;
        
        public DependencyValidationResult(boolean isValid, List<String> errors, List<String> warnings,
                                        Map<String, List<String>> dependencies, List<String> circularDependencies,
                                        Map<String, ApexYamlLexicalValidator.ValidationResult> fileResults,
                                        List<String> rootCauses) {
            this.isValid = isValid;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
            this.dependencies = new HashMap<>(dependencies);
            this.circularDependencies = new ArrayList<>(circularDependencies);
            this.fileResults = new HashMap<>(fileResults);
            this.rootCauses = new ArrayList<>(rootCauses);
        }
        
        public boolean isValid() { return isValid; }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
        public Map<String, List<String>> getDependencies() { return new HashMap<>(dependencies); }
        public List<String> getCircularDependencies() { return new ArrayList<>(circularDependencies); }
        public Map<String, ApexYamlLexicalValidator.ValidationResult> getFileResults() { return new HashMap<>(fileResults); }
        public List<String> getRootCauses() { return new ArrayList<>(rootCauses); }
    }
    
    /**
     * Validate a file with full dependency analysis
     */
    public DependencyValidationResult validateWithDependencies(Path yamlFile) {
        try {
            // Build dependency graph
            Map<String, List<String>> dependencyGraph = buildDependencyGraph(yamlFile);
            
            // Detect circular dependencies
            List<String> circularDeps = detectCircularDependencies(dependencyGraph);
            
            // Validate all files in dependency chain
            Map<String, ApexYamlLexicalValidator.ValidationResult> fileResults = new HashMap<>();
            List<String> allErrors = new ArrayList<>();
            List<String> allWarnings = new ArrayList<>();
            List<String> rootCauses = new ArrayList<>();
            
            // Validate in dependency order (dependencies first)
            List<String> validationOrder = getValidationOrder(dependencyGraph);
            
            for (String filePath : validationOrder) {
                Path file = resolveFilePath(yamlFile, filePath);
                if (Files.exists(file)) {
                    ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(file);
                    fileResults.put(filePath, result);
                    
                    if (!result.isValid()) {
                        allErrors.addAll(result.getErrors().stream()
                            .map(error -> filePath + ": " + error)
                            .collect(Collectors.toList()));
                        
                        // Check if this is a root cause (dependency file with errors)
                        if (!filePath.equals(yamlFile.toString()) && 
                            dependencyGraph.values().stream().anyMatch(deps -> deps.contains(filePath))) {
                            rootCauses.add("Dependency file has validation errors: " + filePath);
                        }
                    }
                } else {
                    allErrors.add("Referenced file not found: " + filePath);
                    rootCauses.add("Missing dependency file: " + filePath);
                }
            }
            
            // Add circular dependency errors
            if (!circularDeps.isEmpty()) {
                allErrors.addAll(circularDeps.stream()
                    .map(cycle -> "Circular dependency detected: " + cycle)
                    .collect(Collectors.toList()));
            }
            
            boolean isValid = allErrors.isEmpty();
            
            return new DependencyValidationResult(
                isValid, allErrors, allWarnings, dependencyGraph, 
                circularDeps, fileResults, rootCauses
            );
            
        } catch (Exception e) {
            return new DependencyValidationResult(
                false, 
                Arrays.asList("Dependency analysis failed: " + e.getMessage()),
                new ArrayList<>(),
                new HashMap<>(),
                new ArrayList<>(),
                new HashMap<>(),
                Arrays.asList("Analysis exception: " + e.getMessage())
            );
        }
    }
    
    /**
     * Build dependency graph for a YAML file
     */
    private Map<String, List<String>> buildDependencyGraph(Path yamlFile) throws IOException {
        Map<String, List<String>> graph = new HashMap<>();
        Set<String> visited = new HashSet<>();
        
        buildDependencyGraphRecursive(yamlFile.toString(), graph, visited, yamlFile.getParent());
        
        return graph;
    }
    
    /**
     * Recursively build dependency graph
     */
    private void buildDependencyGraphRecursive(String filePath, Map<String, List<String>> graph, 
                                             Set<String> visited, Path baseDir) throws IOException {
        if (visited.contains(filePath)) {
            return;
        }
        visited.add(filePath);
        
        Path file = resolveFilePath(Paths.get(filePath), filePath);
        if (!Files.exists(file)) {
            graph.put(filePath, new ArrayList<>());
            return;
        }
        
        List<String> dependencies = extractDependencies(file);
        graph.put(filePath, dependencies);
        
        // Recursively analyze dependencies
        for (String dependency : dependencies) {
            buildDependencyGraphRecursive(dependency, graph, visited, baseDir);
        }
    }
    
    /**
     * Extract dependencies from a YAML file
     */
    private List<String> extractDependencies(Path yamlFile) throws IOException {
        List<String> dependencies = new ArrayList<>();
        
        try {
            String content = Files.readString(yamlFile);
            Map<String, Object> yamlData = yaml.load(content);
            
            if (yamlData != null) {
                // Extract external-data-sources references
                extractExternalDataSources(yamlData, dependencies);
                
                // Extract other reference patterns
                extractOtherReferences(yamlData, dependencies);
            }
            
        } catch (YAMLException e) {
            // If YAML is invalid, we can't extract dependencies
            // This will be caught by the main validation
        }
        
        return dependencies;
    }
    
    /**
     * Extract external-data-sources references
     */
    @SuppressWarnings("unchecked")
    private void extractExternalDataSources(Map<String, Object> yamlData, List<String> dependencies) {
        Object externalDataSources = yamlData.get("external-data-sources");
        if (externalDataSources instanceof List) {
            List<Map<String, Object>> sources = (List<Map<String, Object>>) externalDataSources;
            for (Map<String, Object> source : sources) {
                Object sourceValue = source.get("source");
                if (sourceValue instanceof String) {
                    dependencies.add((String) sourceValue);
                }
            }
        }
    }
    
    /**
     * Extract other reference patterns (can be extended)
     */
    private void extractOtherReferences(Map<String, Object> yamlData, List<String> dependencies) {
        // Add other reference patterns here as needed
        // For example: imports, includes, extends, etc.
    }
    
    /**
     * Detect circular dependencies using DFS
     */
    private List<String> detectCircularDependencies(Map<String, List<String>> graph) {
        List<String> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String node : graph.keySet()) {
            if (!visited.contains(node)) {
                detectCyclesRecursive(node, graph, visited, recursionStack, new ArrayList<>(), cycles);
            }
        }
        
        return cycles;
    }
    
    /**
     * Recursive cycle detection
     */
    private void detectCyclesRecursive(String node, Map<String, List<String>> graph,
                                     Set<String> visited, Set<String> recursionStack,
                                     List<String> currentPath, List<String> cycles) {
        visited.add(node);
        recursionStack.add(node);
        currentPath.add(node);
        
        List<String> neighbors = graph.getOrDefault(node, new ArrayList<>());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                detectCyclesRecursive(neighbor, graph, visited, recursionStack, currentPath, cycles);
            } else if (recursionStack.contains(neighbor)) {
                // Found a cycle
                int cycleStart = currentPath.indexOf(neighbor);
                List<String> cycle = currentPath.subList(cycleStart, currentPath.size());
                cycle.add(neighbor); // Complete the cycle
                cycles.add(String.join(" -> ", cycle));
            }
        }
        
        recursionStack.remove(node);
        currentPath.remove(currentPath.size() - 1);
    }
    
    /**
     * Get validation order (dependencies first)
     */
    private List<String> getValidationOrder(Map<String, List<String>> graph) {
        List<String> order = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        
        for (String node : graph.keySet()) {
            if (!visited.contains(node)) {
                topologicalSortRecursive(node, graph, visited, order);
            }
        }
        
        Collections.reverse(order); // Dependencies first
        return order;
    }
    
    /**
     * Topological sort for dependency ordering
     */
    private void topologicalSortRecursive(String node, Map<String, List<String>> graph,
                                        Set<String> visited, List<String> order) {
        visited.add(node);
        
        List<String> neighbors = graph.getOrDefault(node, new ArrayList<>());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                topologicalSortRecursive(neighbor, graph, visited, order);
            }
        }
        
        order.add(node);
    }
    
    /**
     * Resolve file path relative to base file
     */
    private Path resolveFilePath(Path baseFile, String relativePath) {
        if (Paths.get(relativePath).isAbsolute()) {
            return Paths.get(relativePath);
        }
        
        // If it's the same as baseFile, return baseFile
        if (relativePath.equals(baseFile.toString())) {
            return baseFile;
        }
        
        // Resolve relative to base file's directory
        Path baseDir = baseFile.getParent();
        if (baseDir == null) {
            baseDir = Paths.get(".");
        }
        
        return baseDir.resolve(relativePath).normalize();
    }
}
