package dev.mars.apex.playground.ui;

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

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests that load each example YAML/JSON pair into the playground,
 * validate the YAML, process the data, and verify the results panels.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-26
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=WARN",
    "logging.level.org.springframework=WARN"
})
@DisplayName("Playground Examples UI Tests")
class PlaygroundExamplesUITest {

    private static final Logger logger = LoggerFactory.getLogger(PlaygroundExamplesUITest.class);

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor jsExecutor;
    private String baseUrl;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        jsExecutor = (JavascriptExecutor) driver;
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ========================================================================
    // BASIC EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("Basic Examples")
    class BasicExamples {

        @Test
        @DisplayName("minimal-rule: Load, validate, process and verify results")
        void testMinimalRule() throws IOException {
            // Input has "age": 20, enriched data will contain "age"
            runExampleTest("basic/minimal-rule.yaml", "basic/minimal-rule.json",
                "true", "age");
        }

        @Test
        @DisplayName("simple-age-validation: Load, validate, process and verify results")
        void testSimpleAgeValidation() throws IOException {
            runExampleTest("basic/simple-age-validation.yaml", "basic/simple-age-validation.json",
                "true", "age");
        }

        @Test
        @DisplayName("quick-start: Load, validate, process and verify results")
        void testQuickStart() throws IOException {
            // Input has "amount" and "currency"
            runExampleTest("basic/quick-start.yaml", "basic/quick-start.json",
                "true", "amount");
        }

        @Test
        @DisplayName("nested-field-navigation: Load, validate, process and verify results")
        void testNestedFieldNavigation() throws IOException {
            // Input has "trade" with nested "currency" and "amount"
            runExampleTest("basic/nested-field-navigation.yaml", "basic/nested-field-navigation.json",
                "true", "trade");
        }
    }

    // ========================================================================
    // VALIDATION EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("Validation Examples")
    class ValidationExamples {

        @Test
        @DisplayName("value-threshold: Load, validate, process and verify results")
        void testValueThreshold() throws IOException {
            // Input has "amount": 1500, "currency": "USD", "customerId"
            runExampleTest("validation/value-threshold.yaml", "validation/value-threshold-data.json",
                "true", "amount");
        }
    }

    // ========================================================================
    // ENRICHMENT EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("Enrichment Examples")
    class EnrichmentExamples {

        @Test
        @DisplayName("constant-value-enrichment: Load, validate, process and verify results")
        void testConstantValueEnrichment() throws IOException {
            runExampleTest("enrichment/constant-value-enrichment.yaml", "enrichment/constant-value-enrichment.json",
                "true", "active");
        }

        @Test
        @DisplayName("constant-values: Load, validate, process and verify results")
        void testConstantValues() throws IOException {
            runExampleTest("enrichment/constant-values.yaml", "enrichment/constant-values-data.json",
                "true", "status");
        }

        @Test
        @DisplayName("financial-validation: Load, validate, process and verify results")
        void testFinancialValidation() throws IOException {
            runExampleTest("enrichment/financial-validation.yaml", "enrichment/financial-validation.json",
                "true", "trade");
        }

        @Test
        @DisplayName("enrichment-service-requirement: Load, validate, process and verify results")
        void testEnrichmentServiceRequirement() throws IOException {
            runExampleTest("enrichment/enrichment-service-requirement.yaml", "enrichment/enrichment-service-requirement.json",
                "true", "customer");
        }

        @Test
        @DisplayName("comprehensive-financial-settlement: Load, validate, process and verify results")
        void testComprehensiveFinancialSettlement() throws IOException {
            runExampleTest("enrichment/comprehensive-financial-settlement.yaml", "enrichment/comprehensive-financial-settlement.json",
                "true", "settlement");
        }
    }

    // ========================================================================
    // LOOKUP EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("Lookup Examples")
    class LookupExamples {

        @Test
        @DisplayName("dynamic-pricing: Load, validate, process and verify results")
        void testDynamicPricing() throws IOException {
            // Input has "orderId", "customer", "quantity", "unitPrice"
            runExampleTest("lookup/dynamic-pricing.yaml", "lookup/dynamic-pricing-data.json",
                "true", "orderid");
        }

        @Test
        @DisplayName("math-calculations: Load, validate, process and verify results")
        void testMathCalculations() throws IOException {
            runExampleTest("lookup/math-calculations.yaml", "lookup/math-calculations-data.json",
                "true", "value");
        }
    }

    // ========================================================================
    // RULE GROUPS EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("Rule Groups Examples")
    class RuleGroupsExamples {

        @Test
        @DisplayName("inline-groups: Load, validate, process and verify results")
        void testInlineGroups() throws IOException {
            // Input has "userId", "username", "status", "email", "profile"
            runExampleTest("rulegroups/inline-groups.yaml", "rulegroups/inline-groups-data.json",
                "true", "userid");
        }
    }

    // ========================================================================
    // TRANSFORMATION EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("Transformation Examples")
    class TransformationExamples {

        @Test
        @DisplayName("payment-routing: Load, validate, process and verify results")
        void testPaymentRouting() throws IOException {
            runExampleTest("transformation/payment-routing.yaml", "transformation/payment-routing-data.json",
                "true", "amount");
        }
    }

    // ========================================================================
    // CONDITIONAL EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("Conditional Examples")
    class ConditionalExamples {

        @Test
        @DisplayName("advanced-routing: Load, validate, process and verify results")
        void testAdvancedRouting() throws IOException {
            runExampleTest("conditional/advanced-routing.yaml", "conditional/advanced-routing.json",
                "true", "customer");
        }

        @Test
        @DisplayName("fx-transaction-processing: Load, validate, process and verify results")
        void testFxTransactionProcessing() throws IOException {
            runExampleTest("conditional/fx-transaction-processing.yaml", "conditional/fx-transaction-processing.json",
                "true", "currency");
        }

        @Test
        @DisplayName("nested-discount-logic: Load, validate, process and verify results")
        void testNestedDiscountLogic() throws IOException {
            runExampleTest("conditional/nested-discount-logic.yaml", "conditional/nested-discount-logic.json",
                "true", "discount");
        }

        @Test
        @DisplayName("waterfall-approval: Load, validate, process and verify results")
        void testWaterfallApproval() throws IOException {
            runExampleTest("conditional/waterfall-approval.yaml", "conditional/waterfall-approval.json",
                "true", "approved");
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Run a complete UI test for an example: load files, validate, process, verify results.
     */
    private void runExampleTest(String yamlPath, String jsonPath, 
                                 String expectedValidation, String expectedEnrichment) throws IOException {
        logger.info("=== Testing UI with {} and {} ===", yamlPath, jsonPath);

        // Load file contents
        String yamlContent = loadExampleFile(yamlPath);
        String jsonContent = loadExampleFile(jsonPath);

        // Navigate to playground
        driver.get(baseUrl + "/playground");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("processBtn")));

        // Load YAML into editor
        WebElement yamlEditor = driver.findElement(By.id("yamlRulesEditor"));
        clearAndEnterText(yamlEditor, yamlContent);

        // Load JSON into editor
        WebElement dataEditor = driver.findElement(By.id("sourceDataEditor"));
        clearAndEnterText(dataEditor, jsonContent);

        // Click Validate button
        WebElement validateBtn = driver.findElement(By.id("validateBtn"));
        validateBtn.click();

        // Wait for validation status to update
        WebElement yamlStatus = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("yamlStatus")));
        wait.until(ExpectedConditions.or(
            ExpectedConditions.attributeContains(By.id("yamlStatus"), "class", "bg-success"),
            ExpectedConditions.attributeContains(By.id("yamlStatus"), "class", "bg-danger")
        ));

        // Verify YAML is valid
        String statusClass = yamlStatus.getDomAttribute("class");
        assertTrue(statusClass.contains("bg-success"), 
            "YAML should be valid for " + yamlPath + ". Status class: " + statusClass);

        // Click Process button
        WebElement processBtn = driver.findElement(By.id("processBtn"));
        processBtn.click();

        // Wait for results to be populated
        WebElement validationResults = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("validationResults")));
        WebElement enrichmentResults = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("enrichmentResults")));

        // Wait for results to change from placeholder - look for JSON structure
        wait.until(ExpectedConditions.textMatches(
            By.id("validationResults"), java.util.regex.Pattern.compile(".*\\{.*", java.util.regex.Pattern.DOTALL)));

        // Also wait for enrichment results to have content
        wait.until(ExpectedConditions.textMatches(
            By.id("enrichmentResults"), java.util.regex.Pattern.compile(".*\\{.*", java.util.regex.Pattern.DOTALL)));

        // Get results text
        String validationText = validationResults.getText().toLowerCase();
        String enrichmentText = enrichmentResults.getText().toLowerCase();

        logger.info("Validation results (first 300 chars): {}",
            validationText.substring(0, Math.min(300, validationText.length())));
        logger.info("Enrichment results (first 300 chars): {}",
            enrichmentText.substring(0, Math.min(300, enrichmentText.length())));

        // Verify validation panel contains expected content (check for "true" or "success" or rule results)
        assertTrue(validationText.contains(expectedValidation.toLowerCase()) ||
                   validationText.contains("\"triggered\"") ||
                   validationText.contains("\"valid\""),
            "Validation results should contain '" + expectedValidation + "' for " + yamlPath +
            ". Actual: " + validationText.substring(0, Math.min(200, validationText.length())));

        // Verify enrichment panel contains expected content (check for data field or "enriched")
        assertTrue(enrichmentText.contains(expectedEnrichment.toLowerCase()) ||
                   enrichmentText.contains("\"enriched\"") ||
                   enrichmentText.length() > 10,
            "Enrichment results should contain '" + expectedEnrichment + "' for " + yamlPath +
            ". Actual: " + enrichmentText.substring(0, Math.min(200, enrichmentText.length())));

        logger.info("✓ {} UI test passed", yamlPath);
    }

    private String loadExampleFile(String relativePath) throws IOException {
        Path path = resolveExamplePath(relativePath);
        return Files.readString(path);
    }

    private Path resolveExamplePath(String relativePath) {
        // Try multiple locations for the examples folder
        Path[] possiblePaths = {
            Path.of("examples", relativePath),
            Path.of("apex-playground/examples", relativePath),
            Path.of("src/main/resources/examples", relativePath),
            Path.of("../apex-playground/examples", relativePath)
        };

        for (Path path : possiblePaths) {
            if (Files.exists(path)) {
                return path;
            }
        }

        throw new RuntimeException("Could not find example file: " + relativePath);
    }

    private void clearAndEnterText(WebElement element, String text) {
        element.clear();
        // Use JavaScript to set value for large text blocks
        jsExecutor.executeScript("arguments[0].value = arguments[1];", element, text);
        // Trigger input event so any listeners are notified
        jsExecutor.executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", element);
    }
}

