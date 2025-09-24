# APEX YAML Validation Guide

This document provides comprehensive instructions for validating YAML files using the APEX compiler.

## ⚠️ CRITICAL: YAML Creation Guidelines

**NEVER create APEX YAML examples without first verifying syntax against actual apex-core source code!**

### Mandatory Pre-Creation Verification Process:

1. **ALWAYS examine existing working YAML files first** - Look at actual files in apex-demo/src/test/java/dev/mars/apex/demo/lookup/
2. **ALWAYS check YamlRuleConfiguration.java** - Verify what sections are actually supported (@JsonProperty fields)
3. **ALWAYS check specific classes** - For data sources: YamlDataSource.java, for enrichments: YamlEnrichment.java, etc.
4. **NEVER assume or guess syntax** - Only use patterns found in working examples
5. **ALWAYS validate against APEX_YAML_REFERENCE.md** - But verify the reference is accurate against source code

### Use This Prompt When Creating YAML:

```
Before creating any APEX YAML configuration:

1. First, search the codebase for existing working examples of the type of YAML I need to create
2. Examine the relevant Java configuration classes (YamlRuleConfiguration, YamlDataSource, YamlEnrichment, etc.) to see what fields are actually supported
3. Only use syntax patterns that exist in working examples
4. Never invent or assume YAML keywords - only use what's proven to work in the codebase
5. If uncertain about any syntax, ask the user to clarify or provide an existing example to follow

CRITICAL: Do not create YAML files with invented keywords. Always verify against actual apex-core implementation first.
```

## Overview

The APEX compiler provides robust validation capabilities for YAML configuration files, ensuring they conform to APEX standards and requirements. The validation engine checks for:

- Required metadata fields (id, name, version, description, type)
- Type-specific required fields (author, business-domain, etc.)
- Document structure and content sections
- YAML syntax and formatting
- Cross-reference validation and dependencies

## Known Validation Issues

### SpEL Expression Validation False Positives

**Issue**: The APEX compiler incorrectly flags `##` patterns in message strings as SpEL validation errors.

**Affected Files**: Files containing `validation-config` sections with message fields like:
```yaml
validation-config:
  field: "customerId"
  rules:
    - type: "regex"
      pattern: "^CUST\\d{3}$"
      message: "Customer ID must be in format CUST### (e.g., CUST001)"  # ❌ Flagged as invalid SpEL
```

**Root Cause**: The compiler treats ALL string fields containing `#` as SpEL expressions, including plain text message fields that contain `##` for documentation purposes.

**Impact**:
- Compiler reports "double hash not allowed" errors
- Files are marked as invalid despite working correctly at runtime
- Tests pass because message fields are not executed as SpEL

**Workaround**: These are false positives and can be safely ignored. The APEX runtime correctly treats message fields as plain text, not executable SpEL expressions.

**Future Fix**: The compiler will be updated to be context-aware and only validate SpEL syntax in expression fields (`condition`, `lookup-key`, etc.), not in documentation fields (`message`, `description`, `name`).

## Validation Methods

### Method 1: Project-Wide Validation (Recommended)

**Using ProjectYamlValidator**
```bash
# From project root directory
cd apex-compiler
mvn compile
java -cp "target/classes:../apex-core/target/classes:target/dependency/*" \
  dev.mars.apex.compiler.ProjectYamlValidator
```

**Features:**
- Scans entire project for all YAML files
- Validates each file against APEX compiler rules
- Provides comprehensive validation report
- Shows detailed error messages for invalid files

### Method 2: Maven Test Execution

**Run All Project YAML Validation**
```bash
# From project root
mvn test -Dtest=ProjectYamlValidationTest#validateAllProjectYamlFiles -pl apex-compiler
```

**Run Specific Validation Tests**
```bash
# Test specific file batches
mvn test -Dtest=ProjectYamlValidationTest#validateFixedCriticalFiles -pl apex-compiler

# Test dependency analysis
mvn test -Dtest=ProjectYamlValidationTest#testDependencyChainAnalysis -pl apex-compiler
```

**View Test Results**
```bash
# Check surefire reports for detailed output
cat apex-compiler/target/surefire-reports/dev.mars.apex.compiler.ProjectYamlValidationTest.txt
```

### Method 3: Single File Validation

**Command Line**
```bash
# Validate single file
java -cp "apex-compiler/target/classes:apex-core/target/classes" \
  dev.mars.apex.compiler.ApexYamlCompiler "path/to/file.yaml"
```

**Maven Exec Plugin**
```bash
# From apex-compiler directory
mvn exec:java -Dexec.mainClass="dev.mars.apex.compiler.ApexYamlCompiler" \
  -Dexec.args="../apex-demo/src/test/java/dev/mars/apex/demo/basic-rules/rules.yaml"
```

### Method 4: Folder-Specific Validation (Recommended)

**Using FolderYamlValidator Utility**

The `FolderYamlValidator` is a standalone utility class specifically designed for validating YAML files in a targeted folder. This addresses the common need to validate a specific set of files without running validation on the entire project.

**Maven Test Execution (Easiest):**
```bash
# Validate basic-rules folder
mvn test -Dtest=FolderYamlValidatorTest#validateBasicRulesFolder -pl apex-compiler

# Validate rule-groups folder
mvn test -Dtest=FolderYamlValidatorTest#validateRuleGroupsFolder -pl apex-compiler

# Validate lookup demos folder
mvn test -Dtest=FolderYamlValidatorTest#validateLookupDemoFolder -pl apex-compiler

# Show usage examples and available folders
mvn test -Dtest=FolderYamlValidatorTest#demonstrateUsageExamples -pl apex-compiler
```

**Maven Exec Plugin:**
```bash
cd apex-compiler

# Basic folder validation
mvn exec:java -Dexec.mainClass="dev.mars.apex.compiler.FolderYamlValidator" \
  -Dexec.args="../apex-demo/src/test/java/dev/mars/apex/demo/basic-rules"

# With detailed markdown report generation
mvn exec:java -Dexec.mainClass="dev.mars.apex.compiler.FolderYamlValidator" \
  -Dexec.args="../apex-demo/src/test/java/dev/mars/apex/demo/basic-rules --report"
```

**Direct Command Line Usage:**
```bash
java -cp "target/classes:../apex-core/target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
  dev.mars.apex.compiler.FolderYamlValidator \
  "<folder-path>" [--report]
```

**Key Features:**
- **Targeted Validation**: Validates only files in specified folder (not entire project)
- **Focused Reporting**: Shows validation results specific to the target folder
- **Detailed Analysis**: Provides common issues breakdown and error statistics
- **Report Generation**: Optional detailed markdown reports with timestamps
- **Maven Integration**: Seamless integration with Maven build system
- **CI/CD Ready**: Returns appropriate exit codes (0=success, 1=failures)
- **Cross-Platform**: Works on Windows, Linux, and macOS
- **Test Integration**: Includes JUnit test methods for easy execution

**Available Test Folders:**
```bash
# Basic rules (test files)
../apex-demo/src/test/java/dev/mars/apex/demo/basic-rules

# Rule groups
../apex-demo/src/test/java/dev/mars/apex/demo/rulegroups

# Lookup demos
../apex-demo/src/test/java/dev/mars/apex/demo/lookup

# Enrichment demos
../apex-demo/src/test/java/dev/mars/apex/demo/enrichment

# Conditional logic demos
../apex-demo/src/test/java/dev/mars/apex/demo/conditional

# ETL pipeline demos
../apex-demo/src/test/java/dev/mars/apex/demo/etl

# Core test resources
../apex-core/src/test/resources/rulegroups
```

**Example Output:**
```
APEX Folder YAML Validation
============================
Target Folder: ../apex-demo/src/test/java/dev/mars/apex/demo/basic-rules
Found 10 YAML files to validate

INVALID: combined-config.yaml
   • Missing required metadata field: type
   • Missing required metadata field: id
INVALID: rules.yaml
   • Missing required metadata field: type
   • Missing required metadata field: id
[... more files ...]

VALIDATION SUMMARY
==================
Total files:   10
Valid files:   0
Invalid files: 10
Success rate:  0.0%

COMMON ISSUES
=============
  • Missing 'type' field: 10 files
  • Missing 'id' field: 10 files
```

**PowerShell Script (Alternative)**
```powershell
# Set target folder
$folderPath = "../apex-demo/src/test/java/dev/mars/apex/demo/basic-rules"

# Run validation using Maven (recommended)
cd apex-compiler
mvn exec:java -Dexec.mainClass="dev.mars.apex.compiler.FolderYamlValidator" `
  -Dexec.args="$folderPath"

# Alternative: Manual file-by-file validation
$yamlFiles = Get-ChildItem -Path "apex-demo\src\test\java\dev\mars\apex\demo\basic-rules" -Filter "*.yaml"
foreach ($file in $yamlFiles) {
    Write-Host "Validating: $($file.Name)"
    $result = java -cp "apex-compiler\target\classes;apex-core\target\classes" `
        dev.mars.apex.compiler.ApexYamlCompiler $file.FullName 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "VALID: $($file.Name)" -ForegroundColor Green
    } else {
        Write-Host "INVALID: $($file.Name)" -ForegroundColor Red
        Write-Host "   $result" -ForegroundColor Red
    }
}
```

### Method 5: Programmatic Validation

**Java Code Example**
```java
import dev.mars.apex.compiler.lexical.ApexYamlLexicalValidator;
import java.nio.file.Path;
import java.io.File;

public class ValidateFolder {
    public static void main(String[] args) {
        ApexYamlLexicalValidator validator = new ApexYamlLexicalValidator();
        
        // Target folder
        File folder = new File("apex-demo/src/test/java/dev/mars/apex/demo/basic-rules");
        File[] yamlFiles = folder.listFiles((dir, name) -> name.endsWith(".yaml"));
        
        for (File yamlFile : yamlFiles) {
            var result = validator.validateFile(yamlFile.toPath());
            
            if (result.isValid()) {
                System.out.println("VALID: " + yamlFile.getName());
            } else {
                System.out.println("INVALID: " + yamlFile.getName());
                result.getErrors().forEach(error -> 
                    System.out.println("   • " + error));
            }
        }
    }
}
```

## Validation Requirements

### Required Metadata Fields (All Documents)
- `id`: Unique document identifier
- `name`: Human-readable name
- `version`: Semantic version (e.g., "1.0.0")
- `description`: Purpose description
- `type`: Document type (rule-config, enrichment, dataset, etc.)

### Type-Specific Required Fields
- **rule-config**: `author`
- **enrichment**: `author`
- **dataset**: `source`
- **scenario**: `business-domain`, `owner`
- **scenario-registry**: `created-by`
- **bootstrap**: `business-domain`, `created-by`
- **rule-chain**: `author`
- **external-data-config**: `author`
- **pipeline-config**: `author`

### Document Structure Requirements
- **rule-config**: Must have `rules` or `enrichments` section
- **enrichment**: Must have `enrichments` section
- **dataset**: Must have `data` section
- **scenario**: Must have `scenario`, `data-types`, or `rule-configurations` section
- **scenario-registry**: Must have `scenarios` section
- **bootstrap**: Must have `bootstrap` or `data-sources` section
- **rule-chain**: Must have `rule-chains` section
- **external-data-config**: Must have `dataSources` or `configuration` section

## Common Validation Errors

### Missing Required Metadata Fields
```
Missing required metadata field: id
Missing required metadata field: type
Missing required field for type 'rule-config': author
```

### Invalid Document Types
```
Invalid document type: invalid-type. Valid types: [rule-config, enrichment, dataset, scenario, scenario-registry, bootstrap, rule-chain, external-data-config, pipeline-config]
```

### Missing Required Sections
```
Document type 'rule-config' requires at least one of: [rules, enrichments]
```

### YAML Syntax Errors
```
YAML syntax error: expected a single document in the stream
```

## Specific Use Cases

### Validating Basic Rules Folder

**Using FolderYamlValidator (Recommended):**
```bash
# Method 1: Maven test execution (easiest)
mvn test -Dtest=FolderYamlValidatorTest#validateBasicRulesFolder -pl apex-compiler

# Method 2: Maven exec plugin
cd apex-compiler
mvn exec:java -Dexec.mainClass="dev.mars.apex.compiler.FolderYamlValidator" \
  -Dexec.args="../apex-demo/src/test/java/dev/mars/apex/demo/basic-rules"

# Method 3: With detailed report generation
mvn exec:java -Dexec.mainClass="dev.mars.apex.compiler.FolderYamlValidator" \
  -Dexec.args="../apex-demo/src/test/java/dev/mars/apex/demo/basic-rules --report"
```

**Alternative Methods:**
```bash
# Method 4: Using ProjectYamlValidator (validates entire project)
cd apex-compiler
mvn compile
java -cp "target/classes:../apex-core/target/classes" dev.mars.apex.compiler.ProjectYamlValidator

# Method 5: Manual validation of each file in basic-rules folder
cd apex-compiler
for file in ../apex-demo/src/test/java/dev/mars/apex/demo/basic-rules/*.yaml; do
    echo "Validating: $(basename "$file")"
    java -cp "target/classes:../apex-core/target/classes" dev.mars.apex.compiler.ApexYamlCompiler "$file"
done
```

**Validation Results for Basic Rules Folder:**

**CONFIRMED RESULTS** (as of 2025-09-23):
- **Total Files**: 10 YAML files
- **Valid Files**: 0
- **Invalid Files**: 10
- **Success Rate**: 0% (100% failure rate)

**Specific Files with Issues:**
- `combined-config.yaml` - Missing `id`, `type` fields
- `rule-groups.yaml` - Missing `id`, `type` fields
- `rules.yaml` - Missing `id`, `type` fields
- `severity-comprehensive-test.yaml` - Missing `id`, `type` fields
- `severity-default-behavior.yaml` - Missing `id`, `type` fields
- `severity-edge-cases.yaml` - Missing `id`, `type` fields
- `severity-mixed-rules.yaml` - Missing `id`, `type` fields
- `severity-rule-groups-mixed.yaml` - Missing `id`, `type` fields
- `severity-validation-negative.yaml` - Missing `id`, `type` fields
- `value-threshold-rule.yaml` - Missing `id`, `type` fields

**Primary Issues Found:**
- **Missing `id` field**: All 10 files lack required unique identifier
- **Missing `type` field**: All 10 files lack required document type specification
- **Expected additional issues**: Missing `author` field (required for `rule-config` type)

This confirms that the APEX compiler validation is working correctly and catches non-compliant YAML files as expected.

## Integration with Build Process

### Maven Integration
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-antrun-plugin</artifactId>
    <version>3.1.0</version>
    <executions>
        <execution>
            <phase>validate</phase>
            <goals>
                <goal>run</goal>
            </goals>
            <configuration>
                <target>
                    <apply executable="java" failonerror="true">
                        <arg value="-cp"/>
                        <arg value="${project.build.outputDirectory}:${maven.compile.classpath}"/>
                        <arg value="dev.mars.apex.compiler.ApexYamlCompiler"/>
                        <fileset dir="src/main/resources" includes="**/*.yaml"/>
                    </apply>
                </target>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### CI/CD Pipeline Integration

**GitHub Actions:**
```yaml
- name: Validate YAML Files
  run: |
    cd apex-compiler
    mvn test -Dtest=FolderYamlValidatorTest#validateBasicRulesFolder
```

**Jenkins Pipeline:**
```groovy
stage('YAML Validation') {
    steps {
        dir('apex-compiler') {
            sh 'mvn test -Dtest=FolderYamlValidatorTest#validateBasicRulesFolder'
        }
    }
}
```

**Exit Codes:**
- **0**: All files valid or validation completed successfully
- **1**: Validation errors found or execution failed

### CI/CD Pipeline Integration
```yaml
# GitHub Actions example
jobs:
  validate-yaml:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          
      - name: Build APEX Compiler
        run: |
          cd apex-compiler
          mvn clean compile
          
      - name: Validate YAML Files
        run: |
          cd apex-compiler
          find .. -name "*.yaml" -not -path "*/target/*" | while read file; do
            echo "Validating: $file"
            java -cp "target/classes:../apex-core/target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
              dev.mars.apex.compiler.ApexYamlCompiler "$file"
          done
```

## Report Generation

When using the `--report` flag with `FolderYamlValidator`, a detailed markdown report is generated:

```
FOLDER_VALIDATION_REPORT_20250923_181940.md
```

**Report Contents:**
- Summary statistics
- Detailed validation results table
- Error breakdown by file
- Remediation recommendations

## Troubleshooting

### Common Issues

1. **ClassNotFoundException**: Ensure Maven dependencies are compiled
   ```bash
   mvn compile -pl apex-core,apex-compiler
   ```

2. **Folder not found**: Use absolute paths or verify relative path from apex-compiler directory

3. **No YAML files found**: Check folder contains .yaml or .yml files

### ClassPath Issues
If you encounter `NoClassDefFoundError`, ensure the classpath includes:
- `apex-compiler/target/classes`
- `apex-core/target/classes`
- All required dependencies

### Maven Dependency Resolution
```bash
# Build dependencies first
mvn compile -pl apex-core,apex-compiler
```

### Debug Mode
Add `-X` flag to Maven commands for detailed debug output:
```bash
mvn -X test -Dtest=FolderYamlValidatorTest#validateBasicRulesFolder -pl apex-compiler
```

### Viewing Detailed Test Output
```bash
# Run tests with output
mvn test -Dtest=ProjectYamlValidationTest -pl apex-compiler

# Check surefire reports
ls apex-compiler/target/surefire-reports/
cat apex-compiler/target/surefire-reports/dev.mars.apex.compiler.ProjectYamlValidationTest.txt
```

## Utility Classes and Components

### Core Validation Classes
- **`ApexYamlLexicalValidator`**: Core validation engine that performs APEX YAML validation
  - Location: `apex-compiler/src/main/java/dev/mars/apex/compiler/lexical/ApexYamlLexicalValidator.java`
  - Purpose: Validates individual YAML files against APEX standards

- **`ApexYamlCompiler`**: Single-file validation utility
  - Location: `apex-compiler/src/main/java/dev/mars/apex/compiler/ApexYamlCompiler.java`
  - Purpose: Command-line tool for validating individual YAML files

- **`ProjectYamlValidator`**: Project-wide validation utility
  - Location: `apex-compiler/src/main/java/dev/mars/apex/compiler/ProjectYamlValidator.java`
  - Purpose: Validates all YAML files in the entire project (558+ files)

- **`FolderYamlValidator`**: Folder-specific validation utility (NEW)
  - Location: `apex-compiler/src/main/java/dev/mars/apex/compiler/FolderYamlValidator.java`
  - Purpose: Validates YAML files in a specific target folder

### Test Integration Classes
- **`FolderYamlValidatorTest`**: JUnit test class for folder validation (NEW)
  - Location: `apex-compiler/src/test/java/dev/mars/apex/compiler/FolderYamlValidatorTest.java`
  - Purpose: Provides Maven test methods for easy folder validation execution

- **`ProjectYamlValidationTest`**: JUnit test class for project validation
  - Location: `apex-compiler/src/test/java/dev/mars/apex/compiler/ProjectYamlValidationTest.java`
  - Purpose: Provides Maven test methods for project-wide validation

### Documentation Files
- **`APEX_COMPILER_YAML_VALIDATION_GUIDE.md`**: Comprehensive validation guide (this file)
- **`FOLDER_VALIDATION_USAGE.md`**: Quick usage guide for folder validation
- **`BASIC_RULES_VALIDATION_REPORT.md`**: Specific validation report for basic-rules folder

## Summary

The APEX compiler provides comprehensive YAML validation capabilities through multiple interfaces:
- **Folder-specific validation**: `FolderYamlValidator` for targeted validation
- **Project-wide validation**: `ProjectYamlValidator` for comprehensive analysis
- **Single-file validation**: `ApexYamlCompiler` for individual file checking
- **Maven test integration**: JUnit test classes for build pipeline validation
- **Command-line tools**: Direct execution for scripting and automation
- **Programmatic APIs**: Core validation classes for custom workflows

**Recommended Approach**: Use `FolderYamlValidator` via Maven test execution for most validation needs:
```bash
mvn test -Dtest=FolderYamlValidatorTest#validateBasicRulesFolder -pl apex-compiler
```

## Key Features Summary

- **Focused Validation**: Validates only files in specified folder
- **Detailed Reporting**: Shows specific validation errors for each file
- **Common Issues Analysis**: Groups and counts common validation problems
- **Maven Integration**: Works seamlessly with Maven build system
- **CI/CD Ready**: Provides appropriate exit codes for automated builds
- **Report Generation**: Optional detailed markdown reports
- **Cross-Platform**: Works on Windows, Linux, and macOS

## Next Steps

1. **Fix Validation Errors**: Add missing metadata fields (id, type, author)
2. **Integrate into Build**: Add validation to your Maven build process
3. **Automate Fixes**: Create scripts to batch-fix common issues
4. **Set up Pre-commit Hooks**: Validate YAML files before commits

Choose the method that best fits your workflow and validation requirements.
