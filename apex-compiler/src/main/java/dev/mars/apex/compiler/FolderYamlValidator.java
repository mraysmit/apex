package dev.mars.apex.compiler;

import dev.mars.apex.compiler.lexical.ApexYamlLexicalValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Standalone utility to validate YAML files in a specific folder.
 * Provides focused validation reporting for targeted folder analysis.
 * 
 * Usage:
 *   java -cp "target/classes:../apex-core/target/classes" \
 *     dev.mars.apex.compiler.FolderYamlValidator <folder-path>
 * 
 * Example:
 *   java -cp "target/classes:../apex-core/target/classes" \
 *     dev.mars.apex.compiler.FolderYamlValidator \
 *     "../apex-demo/src/test/java/dev/mars/apex/demo/basic-rules"
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-23
 * @version 1.0
 */
public class FolderYamlValidator {

    private final ApexYamlLexicalValidator validator;
    private final List<ValidationReport> reports;
    private final boolean generateReport;

    public FolderYamlValidator() {
        this(false);
    }

    public FolderYamlValidator(boolean generateReport) {
        this.validator = new ApexYamlLexicalValidator();
        this.reports = new ArrayList<>();
        this.generateReport = generateReport;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String folderPath = args[0];
        boolean generateReport = args.length > 1 && "--report".equals(args[1]);
        
        FolderYamlValidator folderValidator = new FolderYamlValidator(generateReport);
        
        try {
            ValidationSummary summary = folderValidator.validateFolder(folderPath);
            
            if (generateReport) {
                folderValidator.generateMarkdownReport(folderPath, summary);
            }
            
            // Exit with error code if validation failed
            System.exit(summary.hasFailures() ? 1 : 0);
            
        } catch (Exception e) {
            System.err.println("Validation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("APEX Folder YAML Validator");
        System.out.println("==========================");
        System.out.println();
        System.out.println("Usage: java -cp \"target/classes:../apex-core/target/classes\" \\");
        System.out.println("         dev.mars.apex.compiler.FolderYamlValidator <folder-path> [--report]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  folder-path    Path to folder containing YAML files to validate");
        System.out.println("  --report       Generate detailed markdown report (optional)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Validate basic-rules folder");
        System.out.println("  java -cp \"target/classes:../apex-core/target/classes\" \\");
        System.out.println("    dev.mars.apex.compiler.FolderYamlValidator \\");
        System.out.println("    \"../apex-demo/src/test/java/dev/mars/apex/demo/basic-rules\"");
        System.out.println();
        System.out.println("  # Validate with detailed report generation");
        System.out.println("  java -cp \"target/classes:../apex-core/target/classes\" \\");
        System.out.println("    dev.mars.apex.compiler.FolderYamlValidator \\");
        System.out.println("    \"../apex-demo/src/test/java/dev/mars/apex/demo/basic-rules\" --report");
    }

    public ValidationSummary validateFolder(String folderPath) throws IOException {
        Path targetFolder = Paths.get(folderPath).toAbsolutePath().normalize();
        
        if (!Files.exists(targetFolder)) {
            throw new IllegalArgumentException("Folder does not exist: " + targetFolder);
        }
        
        if (!Files.isDirectory(targetFolder)) {
            throw new IllegalArgumentException("Path is not a directory: " + targetFolder);
        }

        List<Path> yamlFiles = findYamlFilesInFolder(targetFolder);
        
        System.out.println("APEX Folder YAML Validation");
        System.out.println("============================");
        System.out.println("Target Folder: " + getRelativePath(targetFolder));
        System.out.println("Found " + yamlFiles.size() + " YAML files to validate");
        System.out.println();
        
        int validFiles = 0;
        int invalidFiles = 0;
        
        for (Path yamlFile : yamlFiles) {
            String fileName = yamlFile.getFileName().toString();
            String relativePath = getRelativePath(yamlFile);
            
            try {
                ApexYamlLexicalValidator.ValidationResult result = validator.validateFile(yamlFile);
                ValidationReport report = new ValidationReport(fileName, relativePath, result);
                reports.add(report);
                
                if (result.isValid()) {
                    validFiles++;
                    System.out.println("VALID: " + fileName);
                } else {
                    invalidFiles++;
                    System.out.println("INVALID: " + fileName);
                    result.getErrors().forEach(error -> 
                        System.out.println("   • " + error));
                    
                    if (!result.getWarnings().isEmpty()) {
                        result.getWarnings().forEach(warning -> 
                            System.out.println("   ⚠️  " + warning));
                    }
                }
                
            } catch (Exception e) {
                invalidFiles++;
                ValidationReport report = new ValidationReport(fileName, relativePath, e);
                reports.add(report);
                System.out.println("ERROR: " + fileName + " - " + e.getMessage());
            }
        }
        
        // Summary
        System.out.println();
        System.out.println("VALIDATION SUMMARY");
        System.out.println("==================");
        System.out.println("Total files:   " + yamlFiles.size());
        System.out.println("Valid files:   " + validFiles);
        System.out.println("Invalid files: " + invalidFiles);
        System.out.println("Success rate:  " + String.format("%.1f%%", 
            yamlFiles.isEmpty() ? 0.0 : (validFiles * 100.0) / yamlFiles.size()));
        
        if (invalidFiles > 0) {
            printCommonIssues();
        }
        
        return new ValidationSummary(yamlFiles.size(), validFiles, invalidFiles, reports);
    }

    private void printCommonIssues() {
        System.out.println();
        System.out.println("COMMON ISSUES");
        System.out.println("=============");
        
        Map<String, Integer> errorCounts = new HashMap<>();
        
        for (ValidationReport report : reports) {
            if (!report.isValid()) {
                for (String error : report.getErrors()) {
                    String errorType = extractErrorType(error);
                    errorCounts.merge(errorType, 1, Integer::sum);
                }
            }
        }
        
        errorCounts.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .forEach(entry -> 
                System.out.println("  • " + entry.getKey() + ": " + entry.getValue() + " files"));
    }

    private void generateMarkdownReport(String folderPath, ValidationSummary summary) throws IOException {
        String reportFileName = "FOLDER_VALIDATION_REPORT_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".md";
        
        Path reportPath = Paths.get(reportFileName);
        
        StringBuilder report = new StringBuilder();
        report.append("# APEX Folder YAML Validation Report\n\n");
        report.append("**Generated**: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("**Target Folder**: `").append(folderPath).append("`\n");
        report.append("**Validation Tool**: APEX Compiler (ApexYamlLexicalValidator)\n\n");
        
        report.append("## Summary\n\n");
        report.append("- **Total Files**: ").append(summary.getTotalFiles()).append("\n");
        report.append("- **Valid Files**: ").append(summary.getValidFiles()).append("\n");
        report.append("- **Invalid Files**: ").append(summary.getInvalidFiles()).append("\n");
        report.append("- **Success Rate**: ").append(String.format("%.1f%%", summary.getSuccessRate())).append("\n\n");
        
        if (summary.hasFailures()) {
            report.append("## Validation Results\n\n");
            report.append("| File | Status | Errors |\n");
            report.append("|------|--------|--------|\n");
            
            for (ValidationReport validationReport : reports) {
                report.append("| `").append(validationReport.getFileName()).append("` | ");
                report.append(validationReport.isValid() ? "VALID" : "INVALID").append(" | ");
                
                if (!validationReport.isValid()) {
                    String errors = String.join("<br>", validationReport.getErrors());
                    report.append(errors);
                } else {
                    report.append("-");
                }
                report.append(" |\n");
            }
        }
        
        Files.writeString(reportPath, report.toString());
        System.out.println();
        System.out.println("Detailed report generated: " + reportPath.toAbsolutePath());
    }

    private String extractErrorType(String error) {
        if (error.contains("Missing required metadata field: id")) return "Missing 'id' field";
        if (error.contains("Missing required metadata field: type")) return "Missing 'type' field";
        if (error.contains("Missing required field for type")) return "Missing type-specific field";
        if (error.contains("Invalid document type")) return "Invalid document type";
        if (error.contains("Missing required 'metadata' section")) return "Missing metadata section";
        if (error.contains("YAML syntax error")) return "YAML syntax error";
        if (error.contains("Document type") && error.contains("requires at least one of")) return "Missing required section";
        return "Other validation error";
    }

    private List<Path> findYamlFilesInFolder(Path folder) throws IOException {
        try (Stream<Path> paths = Files.list(folder)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.endsWith(".yaml") || fileName.endsWith(".yml");
                })
                .sorted()
                .toList();
        }
    }

    private String getRelativePath(Path path) {
        Path currentDir = Paths.get("").toAbsolutePath();
        try {
            return currentDir.relativize(path.toAbsolutePath()).toString().replace("\\", "/");
        } catch (IllegalArgumentException e) {
            return path.toAbsolutePath().toString().replace("\\", "/");
        }
    }

    /**
     * Validation report for a single YAML file
     */
    private static class ValidationReport {
        private final String fileName;
        private final String relativePath;
        private final ApexYamlLexicalValidator.ValidationResult result;
        private final Exception exception;

        public ValidationReport(String fileName, String relativePath, ApexYamlLexicalValidator.ValidationResult result) {
            this.fileName = fileName;
            this.relativePath = relativePath;
            this.result = result;
            this.exception = null;
        }

        public ValidationReport(String fileName, String relativePath, Exception exception) {
            this.fileName = fileName;
            this.relativePath = relativePath;
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

        public String getFileName() {
            return fileName;
        }

        public String getRelativePath() {
            return relativePath;
        }
    }

    /**
     * Summary of validation results
     */
    public static class ValidationSummary {
        private final int totalFiles;
        private final int validFiles;
        private final int invalidFiles;
        private final List<ValidationReport> reports;

        public ValidationSummary(int totalFiles, int validFiles, int invalidFiles, List<ValidationReport> reports) {
            this.totalFiles = totalFiles;
            this.validFiles = validFiles;
            this.invalidFiles = invalidFiles;
            this.reports = new ArrayList<>(reports);
        }

        public int getTotalFiles() { return totalFiles; }
        public int getValidFiles() { return validFiles; }
        public int getInvalidFiles() { return invalidFiles; }
        public boolean hasFailures() { return invalidFiles > 0; }
        public double getSuccessRate() { 
            return totalFiles == 0 ? 0.0 : (validFiles * 100.0) / totalFiles; 
        }
        public List<ValidationReport> getReports() { return new ArrayList<>(reports); }
    }
}
