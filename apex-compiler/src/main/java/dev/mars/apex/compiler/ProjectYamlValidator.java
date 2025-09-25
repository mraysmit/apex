package dev.mars.apex.compiler;

import dev.mars.apex.compiler.lexical.ApexYamlLexicalValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Standalone utility to validate all YAML files in the APEX project.
 * This provides comprehensive validation reporting and analysis.
 */
public class ProjectYamlValidator {

    private final ApexYamlLexicalValidator validator;
    private final List<ValidationReport> reports;

    public ProjectYamlValidator() {
        this.validator = new ApexYamlLexicalValidator();
        this.reports = new ArrayList<>();
    }

    public static void main(String[] args) {
        ProjectYamlValidator projectValidator = new ProjectYamlValidator();
        
        try {
            projectValidator.validateAllYamlFiles();
        } catch (Exception e) {
            System.err.println("X Validation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void validateAllYamlFiles() throws IOException {
        List<Path> yamlFiles = findAllYamlFiles();
        
        System.out.println("🔍 APEX YAML Project Validation");
        System.out.println("=" + "=".repeat(50));
        System.out.println("Found " + yamlFiles.size() + " YAML files to validate\n");
        
        int validFiles = 0;
        int invalidFiles = 0;
        
        for (Path yamlFile : yamlFiles) {
            String relativePath = getRelativePath(yamlFile);
            
            try {
                ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);
                ValidationReport report = new ValidationReport(relativePath, result);
                reports.add(report);
                
                if (result.isValid()) {
                    validFiles++;
                    System.out.println("✅ " + relativePath);
                } else {
                    invalidFiles++;
                    System.out.println("X " + relativePath);
                    result.getErrors().forEach(error -> 
                        System.out.println("   • " + error));
                    
                    if (!result.getWarnings().isEmpty()) {
                        result.getWarnings().forEach(warning -> 
                            System.out.println("   ⚠️  " + warning));
                    }
                }
                
            } catch (Exception e) {
                invalidFiles++;
                ValidationReport report = new ValidationReport(relativePath, e);
                reports.add(report);
                System.out.println("💥 " + relativePath + " - " + e.getMessage());
            }
        }
        
        // Summary
        System.out.println("\n" + "=".repeat(50));
        System.out.println("📊 VALIDATION SUMMARY");
        System.out.println("=".repeat(50));
        System.out.println("Total files:   " + yamlFiles.size());
        System.out.println("Valid files:   " + validFiles);
        System.out.println("Invalid files: " + invalidFiles);
        System.out.println("Success rate:  " + String.format("%.1f%%", 
            (validFiles * 100.0) / yamlFiles.size()));
        
        // Detailed analysis
        printDetailedAnalysis();
        printCommonFixes();
        printRecommendations();
    }

    private void printDetailedAnalysis() {
        System.out.println("\n📋 DETAILED ANALYSIS");
        System.out.println("-".repeat(30));
        
        Map<String, Integer> errorCounts = new HashMap<>();
        
        for (ValidationReport report : reports) {
            if (!report.isValid()) {
                for (String error : report.getErrors()) {
                    String errorType = extractErrorType(error);
                    errorCounts.merge(errorType, 1, Integer::sum);
                }
            }
        }
        
        System.out.println("Common Issues:");
        errorCounts.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .forEach(entry -> 
                System.out.println("  • " + entry.getKey() + ": " + entry.getValue() + " files"));
    }

    private void printCommonFixes() {
        System.out.println("\n🔧 FILES NEEDING COMMON FIXES");
        System.out.println("-".repeat(40));
        
        List<String> needsIdField = new ArrayList<>();
        List<String> needsTypeField = new ArrayList<>();
        List<String> needsAuthorField = new ArrayList<>();
        
        for (ValidationReport report : reports) {
            if (!report.isValid()) {
                for (String error : report.getErrors()) {
                    if (error.contains("Missing required field: id")) {
                        needsIdField.add(report.getFilePath());
                    }
                    if (error.contains("Missing required field: type")) {
                        needsTypeField.add(report.getFilePath());
                    }
                    if (error.contains("Missing required field: author")) {
                        needsAuthorField.add(report.getFilePath());
                    }
                }
            }
        }
        
        System.out.println("Files missing 'id' field (" + needsIdField.size() + "):");
        needsIdField.stream().limit(5).forEach(file -> System.out.println("  • " + file));
        if (needsIdField.size() > 5) {
            System.out.println("  ... and " + (needsIdField.size() - 5) + " more");
        }
        
        System.out.println("\nFiles missing 'type' field (" + needsTypeField.size() + "):");
        needsTypeField.stream().limit(5).forEach(file -> System.out.println("  • " + file));
        if (needsTypeField.size() > 5) {
            System.out.println("  ... and " + (needsTypeField.size() - 5) + " more");
        }
        
        System.out.println("\nFiles missing 'author' field (" + needsAuthorField.size() + "):");
        needsAuthorField.stream().limit(5).forEach(file -> System.out.println("  • " + file));
        if (needsAuthorField.size() > 5) {
            System.out.println("  ... and " + (needsAuthorField.size() - 5) + " more");
        }
    }

    private void printRecommendations() {
        System.out.println("\n💡 RECOMMENDATIONS");
        System.out.println("-".repeat(20));
        System.out.println("1. Fix missing 'id' fields - add unique identifiers to metadata");
        System.out.println("2. Fix missing 'type' fields - specify document type (rule-config, etc.)");
        System.out.println("3. Fix missing 'author' fields - add author information for compliance");
        System.out.println("4. Use automated tools to batch-fix common issues");
        System.out.println("5. Integrate validation into CI/CD pipeline");
        
        System.out.println("\n🚀 NEXT STEPS");
        System.out.println("-".repeat(15));
        System.out.println("• Run: mvn exec:java -Dexec.mainClass=\"dev.mars.apex.compiler.ProjectYamlValidator\"");
        System.out.println("• Add validation to build process");
        System.out.println("• Create automated fix scripts for common issues");
        System.out.println("• Set up pre-commit hooks for YAML validation");
    }

    private String extractErrorType(String error) {
        if (error.contains("Missing required field: id")) return "Missing 'id' field";
        if (error.contains("Missing required field: type")) return "Missing 'type' field";
        if (error.contains("Missing required field: author")) return "Missing 'author' field";
        if (error.contains("Invalid document type")) return "Invalid document type";
        if (error.contains("Missing 'metadata' section")) return "Missing metadata section";
        if (error.contains("YAML syntax error")) return "YAML syntax error";
        return "Other validation error";
    }

    private List<Path> findAllYamlFiles() throws IOException {
        Path projectRoot = Paths.get("..").toAbsolutePath().normalize();
        
        try (Stream<Path> paths = Files.walk(projectRoot)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.endsWith(".yaml") || fileName.endsWith(".yml");
                })
                .filter(path -> !path.toString().contains("target"))
                .filter(path -> !path.toString().contains("node_modules"))
                .filter(path -> !path.toString().contains(".git"))
                .sorted()
                .toList();
        }
    }

    private String getRelativePath(Path yamlFile) {
        Path projectRoot = Paths.get("..").toAbsolutePath().normalize();
        return projectRoot.relativize(yamlFile).toString().replace("\\", "/");
    }

    /**
     * Validation report for a single YAML file
     */
    private static class ValidationReport {
        private final String filePath;
        private final ApexYamlLexicalValidator.ValidationResult result;
        private final Exception exception;

        public ValidationReport(String filePath, ApexYamlLexicalValidator.ValidationResult result) {
            this.filePath = filePath;
            this.result = result;
            this.exception = null;
        }

        public ValidationReport(String filePath, Exception exception) {
            this.filePath = filePath;
            this.result = null;
            this.exception = exception;
        }

        public boolean isValid() {
            return result != null && result.isValid();
        }

        public List<String> getErrors() {
            if (result != null) {
                return result.getErrors();
            } else if (exception != null) {
                return List.of(exception.getMessage());
            }
            return List.of();
        }

        public String getFilePath() {
            return filePath;
        }
    }
}
