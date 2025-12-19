# Selenium Test Development Guide for APEX Visual Editor

**Version:** 1.0  
**Last Updated:** December 19, 2025  
**Target Audience:** Developers creating Selenium tests for YAML import validation

---

## Table of Contents
1. [Overview](#overview)
2. [Getting Started](#getting-started)
3. [BaseYamlImportSeleniumTest API](#baseyamlimportseleniumtest-api)
4. [Creating New Tests](#creating-new-tests)
5. [Best Practices](#best-practices)
6. [Debugging Selenium Failures](#debugging-selenium-failures)
7. [Common Pitfalls](#common-pitfalls)
8. [CI/CD Integration](#cicd-integration)

---

## Overview

The APEX Visual Editor Selenium test framework provides a structured approach to testing YAML import/export functionality in the Blockly-based visual editor. The framework includes:

- **Base Class:** `BaseYamlImportSeleniumTest` with all common infrastructure
- **Screenshot Capture:** Automatic screenshots on test failures
- **Helper Methods:** 15+ helper methods for block validation
- **Spring Boot Integration:** `@SpringBootTest` with random port allocation
- **WebDriverManager:** Automatic ChromeDriver version management

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Chrome browser installed (version 120+)
- APEX Playground running on localhost

### Running Existing Tests

```bash
# Run all YAML import tests
cd apex-playground
mvn test -Dtest=YamlImport*Test

# Run specific test class
mvn test -Dtest=YamlImportValidationUITest

# Run in headed mode (see browser)
mvn test -Dtest=YamlImportValidationUITest -Dselenium.headless=false
```

### Project Structure

```
apex-playground/src/test/java/dev/mars/apex/playground/ui/
├── BaseYamlImportSeleniumTest.java          # Abstract base class
├── ScreenshotOnFailureExtension.java        # Screenshot capture on failure
├── YamlImportValidationUITest.java          # Tests 1-6 (baseline)
└── YamlImportPhase1BlockTypesTest.java      # Tests 7-35 (to be created)

apex-playground/examples/
├── validation/
│   ├── basic-rules-test.yaml
│   └── error-recovery-test.yaml
├── lookup/
│   └── lookup-enrichment-test.yaml
├── enrichment/
│   └── calculation-enrichment-test.yaml
└── ... (more test YAML files)
```

---

## BaseYamlImportSeleniumTest API

### Core Infrastructure

#### Fields
```java
protected WebDriver driver;              // Selenium WebDriver instance
protected WebDriverWait wait;            // WebDriverWait with 15-second timeout
protected String baseUrl;                // http://localhost:{port}
protected JavascriptExecutor js;         // For Blockly workspace interaction
protected int port;                      // Spring Boot random port
```

#### Lifecycle Methods
```java
@BeforeAll static void setupClass()      // Initializes WebDriverManager
@BeforeEach void setUp()                 // Creates WebDriver, ChromeOptions
@AfterEach void tearDown()               // Cleans up WebDriver with quit()
```

### Helper Methods

#### YAML Import/Export
```java
// Load YAML from file system
String yaml = loadYamlFile("examples/validation/basic-rules-test.yaml");

// Import YAML into visual editor
importYamlContent(yaml);

// Wait for blocks to render (1-second delay)
waitForBlocksToRender();

// Export YAML from workspace
String exported = exportYamlContent();
```

#### Block Validation
```java
// Get total block count
int count = getBlockCount();

// Verify specific block type exists with expected count
verifyBlockExists("apex_rule", 3, "Should have 3 Rule blocks");

// Get field value from first block of type
String ruleId = getBlockFieldValue("apex_rule", "ID");

// Count nested blocks within parent
int nested = countNestedBlocks("apex_component", "RULES");
```

#### Enhanced Validation (New in v1.0)
```java
// Verify multiple field values on a block
verifyBlockFieldValues("apex_configuration", Map.of(
    "NAME", "My Config",
    "VERSION", "1.0",
    "TYPE", "rule-config"
));

// Verify nested structure with expected counts
verifyNestedBlockStructure("apex_scenario", Map.of(
    "CLASSIFICATION_ENTRIES", 3,
    "RULES", 2
));

// Get all blocks of a type for iteration
List<String> blockIds = getBlocksByType("apex_rule");

// Verify block order
verifyBlockOrder(List.of("apex_configuration", "apex_rule", "apex_enrichment"));

// Verify YAML structure has required sections
verifyYamlStructure(exported, List.of("metadata", "rules", "enrichments"));
```

---

## Creating New Tests

### Step 1: Create YAML Test Sample

Create a YAML file in the appropriate `examples/` subdirectory:

```yaml
# examples/validation/my-new-test.yaml
metadata:
  name: "My New Test"
  version: "1.0"
  type: "rule-config"

rules:
  - id: "test-rule-001"
    condition: "#amount > 1000"
    message: "Amount exceeds limit"
    severity: "ERROR"
```

### Step 2: Create Test Class

```java
package dev.mars.apex.playground.ui;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class YamlImportPhase1BlockTypesTest extends BaseYamlImportSeleniumTest {

    @Test
    @Order(7)
    @DisplayName("Test 7: Import My New Test - Should create 1 Rule block")
    void testImportMyNewTest() throws IOException {
        // Given
        String yamlContent = loadYamlFile("examples/validation/my-new-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        // When
        importYamlContent(yamlContent);

        // Then
        verifyBlockExists("apex_configuration", 1, "Should have 1 Configuration block");
        verifyBlockExists("apex_rule", 1, "Should have 1 Rule block");
        
        String ruleId = getBlockFieldValue("apex_rule", "ID");
        assertEquals("test-rule-001", ruleId, "Rule ID should be 'test-rule-001'");
    }

    @Test
    @Order(8)
    @DisplayName("Round-trip Test 7: My New Test - Import → Export → Verify")
    void testRoundTripMyNewTest() throws IOException {
        // Given
        String originalYaml = loadYamlFile("examples/validation/my-new-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        // When
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        // Then
        verifyYamlStructure(exportedYaml, List.of("metadata", "rules"));
        assertTrue(exportedYaml.contains("test-rule-001"), "Should preserve rule ID");
        assertTrue(exportedYaml.contains("#amount > 1000"), "Should preserve condition");
        assertTrue(exportedYaml.contains("ERROR"), "Should preserve severity");
    }
}
```

### Step 3: Run Your Tests

```bash
mvn test -Dtest=YamlImportPhase1BlockTypesTest
```

---

## Best Practices

### 1. Use Explicit Waits (Not Implicit)
```java
// ✅ GOOD - Explicit wait for specific condition
wait.until(ExpectedConditions.elementToBeClickable(By.id("importBtn")));

// ❌ BAD - Implicit wait (creates race conditions)
driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
```

### 2. Always Call `waitForBlocklyWorkspaceToLoad()`
```java
// ✅ GOOD - Wait for workspace before interacting
driver.get(baseUrl + "/playground/apex_editor_main.html");
waitForBlocklyWorkspaceToLoad();
importYamlContent(yaml);

// ❌ BAD - Import without waiting
driver.get(baseUrl + "/playground/apex_editor_main.html");
importYamlContent(yaml);  // May fail if workspace not ready
```

### 3. Use `@Order` Annotations for Sequential Tests
```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MyTest extends BaseYamlImportSeleniumTest {
    
    @Test
    @Order(1)
    void testFirst() { }
    
    @Test
    @Order(2)
    void testSecond() { }
}
```

### 4. Structure Tests with Given/When/Then
```java
@Test
void myTest() throws IOException {
    // Given - Setup test data
    String yaml = loadYamlFile("examples/test.yaml");
    driver.get(baseUrl + "/playground/apex_editor_main.html");
    waitForBlocklyWorkspaceToLoad();

    // When - Perform action
    importYamlContent(yaml);

    // Then - Verify results
    verifyBlockExists("apex_rule", 3, "Should have 3 rules");
}
```

### 5. Use Enhanced Helpers for Complex Validation
```java
// Instead of multiple individual assertions:
String name = getBlockFieldValue("apex_config", "NAME");
assertEquals("My Config", name);
String version = getBlockFieldValue("apex_config", "VERSION");
assertEquals("1.0", version);

// Use batch validation:
verifyBlockFieldValues("apex_config", Map.of(
    "NAME", "My Config",
    "VERSION", "1.0"
));
```

---

## Debugging Selenium Failures

### 1. Screenshot Analysis
All test failures automatically capture screenshots to `target/selenium-screenshots/`:

```
target/selenium-screenshots/
├── Test_7_Import_My_New_Test-20251219-143022-456.png
└── Round-trip_Test_7-20251219-143045-789.png
```

Open the screenshot to see the visual state when the test failed.

### 2. Run in Headed Mode
```bash
# Remove --headless to see browser
mvn test -Dtest=YamlImportValidationUITest#testImportBasicRulesConfiguration
```

Then manually modify `setUp()` to comment out headless:
```java
@BeforeEach
void setUp() {
    ChromeOptions options = new ChromeOptions();
    // options.addArguments("--headless");  // COMMENT THIS OUT
    options.addArguments("--no-sandbox");
    // ...
}
```

### 3. Add Debug Breakpoints
```java
@Test
void myTest() throws IOException {
    importYamlContent(yaml);
    
    // Add breakpoint here to inspect workspace
    int blockCount = getBlockCount();  // <-- BREAKPOINT
    
    verifyBlockExists("apex_rule", 3, "Should have 3 rules");
}
```

### 4. Check Browser Console Logs
```java
LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
for (LogEntry entry : logs) {
    System.out.println(entry);
}
```

### 5. Increase Wait Timeout for Slow Systems
```java
// In BaseYamlImportSeleniumTest, change:
wait = new WebDriverWait(driver, Duration.ofSeconds(30));  // Increase from 15
```

---

## Common Pitfalls

### 1. StaleElementReferenceException
**Problem:** Element reference becomes stale after DOM update.

**Solution:** Re-query the element or use helper methods that query fresh.

```java
// ❌ BAD - Element may become stale
WebElement button = driver.findElement(By.id("importBtn"));
// ... some action that updates DOM ...
button.click();  // FAILS

// ✅ GOOD - Query fresh element
wait.until(ExpectedConditions.elementToBeClickable(By.id("importBtn"))).click();
```

### 2. ElementNotInteractableException
**Problem:** Element exists but not visible/clickable yet.

**Solution:** Use explicit wait for clickable condition.

```java
// ❌ BAD
driver.findElement(By.id("importBtn")).click();

// ✅ GOOD
wait.until(ExpectedConditions.elementToBeClickable(By.id("importBtn"))).click();
```

### 3. JavaScript Execution Failures
**Problem:** Blockly workspace not loaded when executing JavaScript.

**Solution:** Always call `waitForBlocklyWorkspaceToLoad()` first.

```java
// ✅ GOOD
waitForBlocklyWorkspaceToLoad();
int count = getBlockCount();  // Safe
```

### 4. YAML File Not Found
**Problem:** Test YAML file path is incorrect.

**Solution:** Use exact path from `examples/` directory.

```java
// ✅ GOOD - Relative to examples/
loadYamlFile("examples/validation/basic-rules-test.yaml");

// ✅ ALSO GOOD - Without examples/ prefix (auto-prepended)
loadYamlFile("validation/basic-rules-test.yaml");
```

### 5. Test Order Dependencies
**Problem:** Tests depend on each other's execution order.

**Solution:** Make each test independent.

```java
// ❌ BAD - Test 2 depends on Test 1
@Test @Order(1)
void test1() { importYamlContent(yaml1); }

@Test @Order(2)
void test2() { 
    // Assumes workspace from test1
    verifyBlockExists("apex_rule", 1);
}

// ✅ GOOD - Each test is independent
@Test @Order(1)
void test1() {
    driver.get(baseUrl + "/playground/apex_editor_main.html");
    waitForBlocklyWorkspaceToLoad();
    importYamlContent(yaml1);
    verifyBlockExists("apex_rule", 1);
}

@Test @Order(2)
void test2() {
    driver.get(baseUrl + "/playground/apex_editor_main.html");
    waitForBlocklyWorkspaceToLoad();
    importYamlContent(yaml2);
    verifyBlockExists("apex_rule", 2);
}
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Selenium Tests

on: [push, pull_request]

jobs:
  selenium-tests:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Install Chrome
        uses: browser-actions/setup-chrome@latest
      
      - name: Run Selenium Tests
        run: |
          cd apex-playground
          mvn test -Dtest=YamlImport*Test -Dselenium.headless=true
      
      - name: Upload Screenshots on Failure
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: selenium-screenshots
          path: apex-playground/target/selenium-screenshots/
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    
    stages {
        stage('Selenium Tests') {
            steps {
                sh 'cd apex-playground'
                sh 'mvn test -Dtest=YamlImport*Test -Dselenium.headless=true'
            }
        }
    }
    
    post {
        failure {
            archiveArtifacts artifacts: 'apex-playground/target/selenium-screenshots/*.png'
        }
    }
}
```

---

## Quick Reference

### Test Execution Commands
```bash
# All YAML import tests
mvn test -Dtest=YamlImport*Test

# Specific phase
mvn test -Dtest=YamlImportPhase1*Test

# Single test method
mvn test -Dtest=YamlImportValidationUITest#testImportBasicRulesConfiguration

# Headed mode (see browser)
mvn test -Dtest=YamlImportValidationUITest -Dselenium.headless=false

# With increased logging
mvn test -Dtest=YamlImportValidationUITest -Dlogging.level.dev.mars.apex=DEBUG
```

### Helper Method Quick Reference
| Method | Purpose | Example |
|--------|---------|---------|
| `loadYamlFile()` | Load YAML from file | `loadYamlFile("examples/test.yaml")` |
| `importYamlContent()` | Import YAML to editor | `importYamlContent(yaml)` |
| `exportYamlContent()` | Export YAML from editor | `String yaml = exportYamlContent()` |
| `waitForBlocklyWorkspaceToLoad()` | Wait for workspace ready | `waitForBlocklyWorkspaceToLoad()` |
| `waitForBlocksToRender()` | Wait after import | `waitForBlocksToRender()` |
| `getBlockCount()` | Count all blocks | `int count = getBlockCount()` |
| `verifyBlockExists()` | Verify block type + count | `verifyBlockExists("apex_rule", 3, "message")` |
| `getBlockFieldValue()` | Get field from block | `getBlockFieldValue("apex_rule", "ID")` |
| `verifyBlockFieldValues()` | Batch field verification | `verifyBlockFieldValues("apex_rule", map)` |
| `verifyNestedBlockStructure()` | Verify nested counts | `verifyNestedBlockStructure("parent", map)` |
| `getBlocksByType()` | Get all blocks of type | `List<String> ids = getBlocksByType("apex_rule")` |
| `verifyBlockOrder()` | Verify block sequence | `verifyBlockOrder(List.of("apex_config", "apex_rule"))` |
| `verifyYamlStructure()` | Verify YAML sections | `verifyYamlStructure(yaml, List.of("metadata", "rules"))` |

---

## Support

- **Documentation:** [APEX_BLOCKS_PROTOTYPE_GUIDE.md](../docs/APEX_BLOCKS_PROTOTYPE_GUIDE.md)
- **Test Coverage Analysis:** [YAML_IMPORT_TEST_COVERAGE_ANALYSIS.md](YAML_IMPORT_TEST_COVERAGE_ANALYSIS.md)
- **Implementation Plan:** [SELENIUM_IMPORT_TEST_IMPLEMENTATION_PLAN.md](SELENIUM_IMPORT_TEST_IMPLEMENTATION_PLAN.md)

For issues or questions, contact the APEX development team.
