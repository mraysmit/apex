package dev.mars.apex.compiler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Paths;

/**
 * Test class for FolderYamlValidator to validate specific folders.
 * This provides a convenient way to run folder-specific validation through Maven.
 */
public class FolderYamlValidatorTest {

    @Test
    @DisplayName("Validate Basic Rules Folder")
    public void validateBasicRulesFolder() throws Exception {
        String folderPath = "../apex-demo/src/test/java/dev/mars/apex/demo/basic";
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("APEX FOLDER VALIDATION TEST - BASIC RULES");
        System.out.println("=".repeat(60));
        
        FolderYamlValidator validator = new FolderYamlValidator();
        FolderYamlValidator.ValidationSummary summary = validator.validateFolder(folderPath);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TEST RESULTS");
        System.out.println("=".repeat(60));
        System.out.println("This test is informational - it shows validation results");
        System.out.println("but does not fail the build based on YAML validation errors.");
        System.out.println();
        
        if (summary.hasFailures()) {
            System.out.println("YAML VALIDATION ISSUES FOUND:");
            System.out.println("- " + summary.getInvalidFiles() + " files have validation errors");
            System.out.println("- These files need metadata fixes (id, type, author fields)");
            System.out.println("- Files are currently non-compliant with APEX standards");
        } else {
            System.out.println("ALL FILES VALID:");
            System.out.println("- All " + summary.getValidFiles() + " files passed APEX validation");
            System.out.println("- Files are compliant with APEX standards");
        }
        
        System.out.println("\nTo fix validation errors, add required metadata fields:");
        System.out.println("  metadata:");
        System.out.println("    id: \"unique-identifier\"");
        System.out.println("    type: \"rule-config\"");
        System.out.println("    author: \"developer@company.com\"");
        System.out.println("    # ... existing fields");
        
        // Note: We don't fail the test here to keep it informational
        // In a real scenario, you might want to fail based on validation results
    }

    @Test
    @DisplayName("Validate Rule Groups Folder")
    public void validateRuleGroupsFolder() throws Exception {
        String folderPath = "../apex-demo/src/test/java/dev/mars/apex/demo/rulegroups";
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("APEX FOLDER VALIDATION TEST - RULE GROUPS");
        System.out.println("=".repeat(60));
        
        FolderYamlValidator validator = new FolderYamlValidator();
        
        try {
            FolderYamlValidator.ValidationSummary summary = validator.validateFolder(folderPath);
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("TEST RESULTS - RULE GROUPS");
            System.out.println("=".repeat(60));
            
            if (summary.hasFailures()) {
                System.out.println("YAML VALIDATION ISSUES FOUND:");
                System.out.println("- " + summary.getInvalidFiles() + " files have validation errors");
            } else {
                System.out.println("ALL FILES VALID:");
                System.out.println("- All " + summary.getValidFiles() + " files passed APEX validation");
            }
            
        } catch (Exception e) {
            System.out.println("Folder validation failed: " + e.getMessage());
            System.out.println("This may be expected if the folder doesn't exist or has no YAML files.");
        }
    }

    @Test
    @DisplayName("Validate Lookup Demo Folder")
    public void validateLookupDemoFolder() throws Exception {
        String folderPath = "../apex-demo/src/test/java/dev/mars/apex/demo/lookup";
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("APEX FOLDER VALIDATION TEST - LOOKUP DEMOS");
        System.out.println("=".repeat(60));
        
        FolderYamlValidator validator = new FolderYamlValidator();
        
        try {
            FolderYamlValidator.ValidationSummary summary = validator.validateFolder(folderPath);
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("TEST RESULTS - LOOKUP DEMOS");
            System.out.println("=".repeat(60));
            
            System.out.println("Total files: " + summary.getTotalFiles());
            System.out.println("Valid files: " + summary.getValidFiles());
            System.out.println("Invalid files: " + summary.getInvalidFiles());
            System.out.println("Success rate: " + String.format("%.1f%%", summary.getSuccessRate()));
            
        } catch (Exception e) {
            System.out.println("Folder validation failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Demonstrate Usage Examples")
    public void demonstrateUsageExamples() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("FOLDER YAML VALIDATOR - USAGE EXAMPLES");
        System.out.println("=".repeat(60));
        
        System.out.println("\n1. MAVEN TEST EXECUTION:");
        System.out.println("   mvn test -Dtest=FolderYamlValidatorTest#validateBasicRulesFolder -pl apex-compiler");
        
        System.out.println("\n2. COMMAND LINE EXECUTION:");
        System.out.println("   cd apex-compiler");
        System.out.println("   mvn exec:java -Dexec.mainClass=\"dev.mars.apex.compiler.FolderYamlValidator\" \\");
        System.out.println("     -Dexec.args=\"../apex-demo/src/test/java/dev/mars/apex/demo/basic\"");

        System.out.println("\n3. WITH REPORT GENERATION:");
        System.out.println("   mvn exec:java -Dexec.mainClass=\"dev.mars.apex.compiler.FolderYamlValidator\" \\");
        System.out.println("     -Dexec.args=\"../apex-demo/src/test/java/dev/mars/apex/demo/basic --report\"");

        System.out.println("\n4. DIRECT JAVA EXECUTION (with proper classpath):");
        System.out.println("   java -cp \"target/classes:../apex-core/target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)\" \\");
        System.out.println("     dev.mars.apex.compiler.FolderYamlValidator \\");
        System.out.println("     \"../apex-demo/src/test/java/dev/mars/apex/demo/basic\"");

        System.out.println("\n5. POWERSHELL SCRIPT:");
        System.out.println("   $folderPath = \"../apex-demo/src/test/java/dev/mars/apex/demo/basic\"");
        System.out.println("   mvn exec:java -Dexec.mainClass=\"dev.mars.apex.compiler.FolderYamlValidator\" `");
        System.out.println("     -Dexec.args=\"$folderPath\"");

        System.out.println("\n" + "=".repeat(60));
        System.out.println("AVAILABLE FOLDERS TO VALIDATE:");
        System.out.println("=".repeat(60));
        System.out.println("• ../apex-demo/src/test/java/dev/mars/apex/demo/basic");
        System.out.println("• ../apex-demo/src/test/java/dev/mars/apex/demo/rulegroups");
        System.out.println("• ../apex-demo/src/test/java/dev/mars/apex/demo/lookup");
        System.out.println("• ../apex-demo/src/test/java/dev/mars/apex/demo/enrichment");
        System.out.println("• ../apex-demo/src/test/java/dev/mars/apex/demo/conditional");
        System.out.println("• ../apex-demo/src/test/java/dev/mars/apex/demo/etl");
        System.out.println("• ../apex-core/src/test/resources/rulegroups");
        
        System.out.println("\n" + "=".repeat(60));
    }
}
