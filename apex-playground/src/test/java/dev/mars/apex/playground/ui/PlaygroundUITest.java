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
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI automation tests for the APEX Playground web interface.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=WARN",
    "logging.level.org.springframework=WARN"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("APEX Playground UI Automation Tests")
class PlaygroundUITest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    @BeforeAll
    static void setupClass() {
        // Setup WebDriverManager to automatically manage ChromeDriver
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUp() {
        // Configure Chrome options for headless testing
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Run in headless mode for CI/CD
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should load playground page successfully")
    void shouldLoadPlaygroundPageSuccessfully() {
        // When
        driver.get(baseUrl + "/playground");

        // Then
        assertEquals("APEX Playground", driver.getTitle());
        
        // Verify main UI elements are present
        assertTrue(isElementPresent(By.id("processBtn")), "Process button should be present");
        assertTrue(isElementPresent(By.id("validateBtn")), "Validate button should be present");
        assertTrue(isElementPresent(By.id("clearBtn")), "Clear button should be present");
        assertTrue(isElementPresent(By.id("loadExampleBtn")), "Load Example button should be present");
        
        // Verify navigation elements
        assertTrue(isElementPresent(By.className("navbar-brand")), "Navigation brand should be present");
        assertTrue(driver.findElement(By.className("navbar-brand")).getText().contains("APEX Playground"));
    }

    @Test
    @Order(2)
    @DisplayName("Should validate YAML using the Validate button")
    void shouldValidateYamlUsingValidateButton() {
        // Given
        driver.get(baseUrl + "/playground");
        
        String validYaml = """
            metadata:
              name: "UI Test Rules"
              version: "1.0.0"
              description: "Testing YAML validation via UI"
            
            rules:
              - id: "ui-test-rule"
                name: "UI Test Rule"
                condition: "#age >= 18"
                message: "Must be 18 or older"
            """;

        // When - Fill in YAML and click validate
        WebElement yamlTextArea = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("yamlRulesEditor")));

        // Clear and enter YAML
        clearAndEnterText(yamlTextArea, validYaml);

        WebElement validateBtn = driver.findElement(By.id("validateBtn"));
        validateBtn.click();

        // Then - Wait for validation status badge to update
        WebElement statusBadge = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("yamlStatus")));

        // Wait for the status to change from default "Valid"
        wait.until(ExpectedConditions.or(
            ExpectedConditions.textToBe(By.id("yamlStatus"), "YAML configuration is valid"),
            ExpectedConditions.attributeContains(By.id("yamlStatus"), "class", "bg-success")
        ));

        // Verify validation success in status badge
        String statusText = statusBadge.getText().toLowerCase();
        String statusClass = statusBadge.getDomAttribute("class");

        assertTrue(statusText.contains("valid") || statusClass.contains("bg-success"),
                  "Validation status should indicate success. Status: " + statusText + ", Class: " + statusClass);
    }

    @Test
    @Order(3)
    @DisplayName("Should process data using the Process button")
    void shouldProcessDataUsingProcessButton() {
        // Given
        driver.get(baseUrl + "/playground");
        
        String jsonData = """
            {
              "name": "John Doe",
              "age": 25,
              "department": "Engineering"
            }
            """;
            
        String yamlRules = """
            metadata:
              name: "UI Process Test"
              version: "1.0.0"
            
            rules:
              - id: "age-check"
                name: "Age Check"
                condition: "#age >= 21"
                message: "Age requirement met"
            """;

        // When - Fill in data and rules, then process
        fillDataAndRules(jsonData, yamlRules);

        WebElement processBtn = driver.findElement(By.id("processBtn"));
        processBtn.click();

        // Then - Wait for processing results in both validation and enrichment panels
        WebElement validationArea = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("validationResults")));
        WebElement enrichmentArea = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("enrichmentResults")));

        // Wait for results to be populated
        wait.until(ExpectedConditions.not(ExpectedConditions.textToBe(
            By.id("validationResults"), "Click \"Process\" to see validation results...")));

        // Verify processing success in either panel
        String validationText = validationArea.getText().toLowerCase();
        String enrichmentText = enrichmentArea.getText().toLowerCase();

        assertTrue(validationText.contains("success") || validationText.contains("valid") ||
                  enrichmentText.contains("success") || enrichmentText.contains("processed") ||
                  enrichmentText.contains("enriched"),
                  "Processing result should indicate success. Validation: " + validationText +
                  ", Enrichment: " + enrichmentText);
    }

    @Test
    @Order(4)
    @DisplayName("Should clear all fields using the Clear button")
    void shouldClearAllFieldsUsingClearButton() {
        // Given
        driver.get(baseUrl + "/playground");
        
        // Fill in some data first
        String testData = "test data";
        String testYaml = "metadata:\n  name: \"test\"";
        
        fillDataAndRules(testData, testYaml);

        // When - Click clear button and handle confirmation alert
        WebElement clearBtn = driver.findElement(By.id("clearBtn"));
        clearBtn.click();

        // Handle the confirmation alert
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();

        // Then - Verify fields are cleared
        WebElement dataArea = driver.findElement(By.id("sourceDataEditor"));
        WebElement yamlArea = driver.findElement(By.id("yamlRulesEditor"));

        // Wait a moment for clear operation to complete
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(getTextAreaContent(dataArea).trim().isEmpty(),
                  "Data field should be cleared");
        assertTrue(getTextAreaContent(yamlArea).trim().isEmpty(),
                  "YAML field should be cleared");
    }

    @Test
    @Order(5)
    @DisplayName("Should allow manual data entry and processing")
    void shouldAllowManualDataEntryAndProcessing() {
        // Given
        driver.get(baseUrl + "/playground");

        String testData = """
            {
              "name": "Test User",
              "age": 25,
              "email": "test@example.com"
            }
            """;

        String testYaml = """
            metadata:
              name: "Manual Test Rules"
              version: "1.0.0"

            rules:
              - id: "manual-test"
                name: "Manual Test Rule"
                condition: "#age >= 18"
                message: "Age validation passed"
            """;

        // When - Manually enter data and process
        WebElement dataArea = driver.findElement(By.id("sourceDataEditor"));
        WebElement yamlArea = driver.findElement(By.id("yamlRulesEditor"));

        clearAndEnterText(dataArea, testData);
        clearAndEnterText(yamlArea, testYaml);

        WebElement processBtn = driver.findElement(By.id("processBtn"));
        processBtn.click();

        // Then - Verify processing works with manual data
        WebElement validationArea = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("validationResults")));

        // Wait for results to be populated
        wait.until(ExpectedConditions.not(ExpectedConditions.textToBe(
            By.id("validationResults"), "Click \"Process\" to see validation results...")));

        String validationText = validationArea.getText().toLowerCase();
        assertTrue(validationText.contains("success") || validationText.contains("valid") ||
                  validationText.contains("passed"),
                  "Manual data processing should work correctly. Got: " + validationText);
    }

    @Test
    @Order(6)
    @DisplayName("Should work on mobile viewport")
    void shouldWorkOnMobileViewport() {
        // Given - Set mobile viewport size
        driver.get(baseUrl + "/playground");
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(375, 667));

        // When - Test basic functionality on mobile
        String mobileData = """
            {
              "device": "mobile",
              "test": true
            }
            """;

        String mobileYaml = """
            metadata:
              name: "Mobile Test"
              version: "1.0.0"
            rules:
              - id: "mobile-test"
                condition: "#test == true"
                message: "Mobile test passed"
            """;

        WebElement dataArea = driver.findElement(By.id("sourceDataEditor"));
        WebElement yamlArea = driver.findElement(By.id("yamlRulesEditor"));

        // Verify elements are accessible on mobile
        assertTrue(dataArea.isDisplayed(), "Data editor should be visible on mobile");
        assertTrue(yamlArea.isDisplayed(), "YAML editor should be visible on mobile");

        clearAndEnterText(dataArea, mobileData);
        clearAndEnterText(yamlArea, mobileYaml);

        WebElement processBtn = driver.findElement(By.id("processBtn"));
        assertTrue(processBtn.isDisplayed(), "Process button should be visible on mobile");
        processBtn.click();

        // Then - Verify processing works on mobile
        WebElement validationArea = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("validationResults")));

        wait.until(ExpectedConditions.not(ExpectedConditions.textToBe(
            By.id("validationResults"), "Click \"Process\" to see validation results...")));

        String validationText = validationArea.getText().toLowerCase();
        assertTrue(validationText.contains("success") || validationText.contains("valid"),
                  "Processing should work on mobile viewport. Got: " + validationText);
    }

    @Test
    @Order(7)
    @DisplayName("Should handle invalid YAML gracefully in UI")
    void shouldHandleInvalidYamlGracefullyInUI() {
        // Given
        driver.get(baseUrl + "/playground");
        
        String invalidYaml = """
            metadata:
              name: "Invalid YAML"
            rules:
              - id: "test"
                condition: "#age > 18"
                message: "This is an unclosed string that will cause YAML parsing to fail
            """;

        // When - Enter invalid YAML and validate
        WebElement yamlArea = driver.findElement(By.id("yamlRulesEditor"));
        clearAndEnterText(yamlArea, invalidYaml);

        WebElement validateBtn = driver.findElement(By.id("validateBtn"));
        validateBtn.click();

        // Then - Verify error is displayed in status badge
        WebElement statusBadge = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("yamlStatus")));

        // Wait for the status to change to error state
        wait.until(ExpectedConditions.or(
            ExpectedConditions.attributeContains(By.id("yamlStatus"), "class", "bg-danger"),
            ExpectedConditions.textToBe(By.id("yamlStatus"), "Invalid")
        ));

        String statusText = statusBadge.getText().toLowerCase();
        String statusClass = statusBadge.getDomAttribute("class");

        assertTrue(statusText.contains("invalid") || statusText.contains("error") ||
                  statusClass.contains("bg-danger"),
                  "Validation should show error for invalid YAML. Status: " + statusText + ", Class: " + statusClass);
    }

    @Test
    @Order(8)
    @DisplayName("Should process XML data format correctly")
    void shouldProcessXmlDataFormat() {
        // Given
        driver.get(baseUrl + "/playground");

        String xmlData = """
            <person>
                <name>John Smith</name>
                <age>28</age>
                <department>Engineering</department>
                <active>true</active>
            </person>
            """;

        String yamlRules = """
            metadata:
              name: "XML Processing Test"
              version: "1.0.0"

            rules:
              - id: "xml-age-check"
                name: "XML Age Validation"
                condition: "#age >= 25"
                message: "Age requirement met for XML data"
              - id: "xml-department-check"
                name: "XML Department Check"
                condition: "#department == 'Engineering'"
                message: "Engineering department confirmed"
            """;

        // When - Select XML format and process
        WebElement xmlFormatRadio = driver.findElement(By.id("xmlFormat"));
        // Use JavaScript to click the radio button to avoid click interception
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", xmlFormatRadio);

        fillDataAndRules(xmlData, yamlRules);

        WebElement processBtn = driver.findElement(By.id("processBtn"));
        processBtn.click();

        // Then - Verify XML processing works
        WebElement validationArea = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("validationResults")));

        // Wait for results to be populated
        wait.until(ExpectedConditions.not(ExpectedConditions.textToBe(
            By.id("validationResults"), "Click \"Process\" to see validation results...")));

        String validationText = validationArea.getText();
        assertFalse(validationText.contains("Click \"Process\""),
                   "XML validation results should be populated");
        assertTrue(validationText.contains("valid") || validationText.contains("success"),
                   "XML processing should show success indicators");
    }

    @Test
    @Order(9)
    @DisplayName("Should process CSV data format correctly")
    void shouldProcessCsvDataFormat() {
        // Given
        driver.get(baseUrl + "/playground");

        String csvData = """
            name,age,department,salary,active
            Alice Johnson,30,Marketing,75000,true
            Bob Wilson,25,Sales,65000,false
            """;

        String yamlRules = """
            metadata:
              name: "CSV Processing Test"
              version: "1.0.0"

            rules:
              - id: "csv-age-check"
                name: "CSV Age Validation"
                condition: "#age >= 25"
                message: "Age requirement met for CSV data"
              - id: "csv-salary-check"
                name: "CSV Salary Check"
                condition: "#salary > 60000"
                message: "Salary threshold met"
            """;

        // When - Select CSV format and process
        WebElement csvFormatRadio = driver.findElement(By.id("csvFormat"));
        // Use JavaScript to click the radio button to avoid click interception
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", csvFormatRadio);

        fillDataAndRules(csvData, yamlRules);

        WebElement processBtn = driver.findElement(By.id("processBtn"));
        processBtn.click();

        // Then - Verify CSV processing works
        WebElement validationArea = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("validationResults")));

        // Wait for results to be populated
        wait.until(ExpectedConditions.not(ExpectedConditions.textToBe(
            By.id("validationResults"), "Click \"Process\" to see validation results...")));

        String validationText = validationArea.getText();
        assertFalse(validationText.contains("Click \"Process\""),
                   "CSV validation results should be populated");
        assertTrue(validationText.contains("valid") || validationText.contains("success"),
                   "CSV processing should show success indicators");
    }

    // Helper methods
    private boolean isElementPresent(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void fillDataAndRules(String data, String yaml) {
        WebElement dataArea = driver.findElement(By.id("sourceDataEditor"));
        clearAndEnterText(dataArea, data);

        WebElement yamlArea = driver.findElement(By.id("yamlRulesEditor"));
        clearAndEnterText(yamlArea, yaml);
    }

    private void clearAndEnterText(WebElement element, String text) {
        element.clear();
        element.sendKeys(text);
    }

    private String getTextAreaContent(WebElement element) {
        String value = element.getDomAttribute("value");
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return element.getText();
    }
}

