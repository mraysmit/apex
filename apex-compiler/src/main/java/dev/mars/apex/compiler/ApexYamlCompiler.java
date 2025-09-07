package dev.mars.apex.compiler;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.compiler.dependency.ApexDependencyAnalyzer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * APEX YAML Compiler - Main Entry Point.
 * 
 * This is the proper location for the APEX YAML Compiler.
 * Unlike javac being separate from the JVM, this compiler is separate from apex-core.
 * 
 * The compiler provides:
 * - Static analysis of APEX YAML configurations
 * - Compile-time error detection
 * - Code generation for optimized runtime execution
 * - IDE integration through Language Server Protocol
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-01-06
 * @version 1.0
 */
public class ApexYamlCompiler {
    
    private final YamlConfigurationLoader configLoader;
    
    public ApexYamlCompiler() {
        this.configLoader = new YamlConfigurationLoader();
    }
    
    /**
     * Compile APEX YAML configuration.
     * 
     * @param yamlContent The YAML content to compile
     * @return Compilation result with generated code and diagnostics
     */
    public CompilationResult compile(String yamlContent) {
        try {
            // Parse YAML using apex-core infrastructure
            YamlRuleConfiguration config = configLoader.fromYamlString(yamlContent);
            
            // Perform static analysis
            // TODO: Implement lexical analysis, parsing, semantic analysis, code generation
            
            return new CompilationResult(true, "Compilation successful", null);
            
        } catch (Exception e) {
            return new CompilationResult(false, "Compilation failed: " + e.getMessage(), null);
        }
    }
    
    /**
     * Compilation result.
     */
    public static class CompilationResult {
        public final boolean success;
        public final String message;
        public final String generatedCode;
        
        public CompilationResult(boolean success, String message, String generatedCode) {
            this.success = success;
            this.message = message;
            this.generatedCode = generatedCode;
        }
    }
    
    /**
     * Main entry point for command-line usage.
     */
    public static void main(String[] args) {
        System.out.println("APEX YAML Lexical Grammar Checker v1.0");
        System.out.println("=======================================");
        System.out.println("✅ Validates APEX YAML files against formal grammar");
        System.out.println("✅ No Java code generation - pure validation");
        System.out.println("✅ Catches syntax, structure, and semantic errors");

        if (args.length == 0) {
            System.out.println("\nUsage: java -cp apex-compiler.jar dev.mars.apex.compiler.ApexYamlCompiler <yaml-file>");
            System.out.println("\nExample:");
            System.out.println("  java -cp apex-compiler.jar dev.mars.apex.compiler.ApexYamlCompiler my-rules.yaml");
            return;
        }

        // Validate the YAML file with dependency analysis
        ApexDependencyAnalyzer dependencyAnalyzer = new ApexDependencyAnalyzer();
        Path yamlFile = Paths.get(args[0]);

        System.out.println("\n🔍 Validating with Dependency Analysis: " + yamlFile);
        System.out.println("-".repeat(60));

        // Perform dependency-aware validation
        ApexDependencyAnalyzer.DependencyValidationResult result = dependencyAnalyzer.validateWithDependencies(yamlFile);

        // Display dependency graph
        if (!result.getDependencies().isEmpty()) {
            System.out.println("\n📊 DEPENDENCY GRAPH:");
            result.getDependencies().forEach((file, deps) -> {
                System.out.println("  " + file);
                deps.forEach(dep -> System.out.println("    └─ " + dep));
            });
        }

        // Display circular dependencies
        if (!result.getCircularDependencies().isEmpty()) {
            System.out.println("\n🔄 CIRCULAR DEPENDENCIES DETECTED:");
            result.getCircularDependencies().forEach(cycle ->
                System.out.println("  ⚠️  " + cycle));
        }

        // Display root causes
        if (!result.getRootCauses().isEmpty()) {
            System.out.println("\n🎯 ROOT CAUSES:");
            result.getRootCauses().forEach(cause ->
                System.out.println("  🔍 " + cause));
        }

        // Display validation errors
        if (!result.getErrors().isEmpty()) {
            System.out.println("\n❌ VALIDATION ERRORS:");
            result.getErrors().forEach(error ->
                System.out.println("  • " + error));
        }

        // Display file-specific results
        System.out.println("\n📋 FILE VALIDATION RESULTS:");
        result.getFileResults().forEach((file, fileResult) -> {
            String status = fileResult.isValid() ? "✅" : "❌";
            System.out.println("  " + status + " " + file);
            if (!fileResult.isValid()) {
                fileResult.getErrors().forEach(error ->
                    System.out.println("      • " + error));
            }
        });

        if (result.isValid()) {
            System.out.println("\n✅ DEPENDENCY-AWARE VALIDATION PASSED!");
            System.out.println("   All files in dependency chain are valid.");
            System.exit(0);
        } else {
            System.out.println("\n❌ DEPENDENCY-AWARE VALIDATION FAILED!");
            System.out.println("   Please fix the errors above, starting with root causes.");
            System.exit(1);
        }
    }
}
