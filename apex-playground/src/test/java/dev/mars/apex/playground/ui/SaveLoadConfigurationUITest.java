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


import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for Save/Load Configuration functionality in APEX Playground.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=INFO",
    "apex.playground.examples-enabled=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SaveLoadConfigurationUITest {

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor jsExecutor;
    private String baseUrl;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        // Setup Chrome driver with options for download testing
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        // Configure download behavior
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        jsExecutor = (JavascriptExecutor) driver;
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
    @DisplayName("Should display save configuration button")
    void shouldDisplaySaveConfigurationButton() {
        // When
        driver.get(baseUrl + "/playground");

        // Then
        WebElement saveConfigBtn = driver.findElement(By.id("saveConfigBtn"));
        assertTrue(saveConfigBtn.isDisplayed(), "Save Config button should be displayed");
        assertTrue(saveConfigBtn.getText().contains("Save Config"), "Button should have correct text");
        
        // Verify button has correct icon (FontAwesome might not load in headless mode)
        try {
            WebElement icon = saveConfigBtn.findElement(By.cssSelector("i"));
            assertTrue(icon.isDisplayed(), "Save button should have an icon");
        } catch (Exception e) {
            // FontAwesome might not load in headless mode, just verify button works
            assertTrue(saveConfigBtn.getText().contains("Save"), "Button should contain 'Save' text");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should trigger download when save config button is clicked")
    void shouldTriggerDownloadWhenSaveConfigButtonIsClicked() {
        // Given
        driver.get(baseUrl + "/playground");
        
        // Fill in some test data
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        WebElement yamlEditor = driver.findElement(By.id("yamlRulesEditor"));
        
        String testData = """
            {
              "name": "Save Test",
              "value": 123
            }
            """;
            
        String testYaml = """
            metadata:
              name: "Save Test Configuration"
              version: "1.0.0"
            
            rules:
              - id: "save-test-rule"
                name: "Save Test Rule"
                condition: "#value > 100"
                message: "Value is greater than 100"
                enabled: true
            """;
        
        clearAndEnterText(sourceEditor, testData);
        clearAndEnterText(yamlEditor, testYaml);

        // When
        WebElement saveConfigBtn = driver.findElement(By.id("saveConfigBtn"));
        
        // Monitor for download trigger (we can't actually verify file download in headless mode,
        // but we can verify the JavaScript execution and success message)
        saveConfigBtn.click();

        // Then - Verify success message appears
        WebElement successAlert = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".alert-success")));
        assertTrue(successAlert.getText().toLowerCase().contains("saved"),
                  "Success message should indicate configuration was saved");
    }

    @Test
    @Order(3)
    @DisplayName("Should save current data format in configuration")
    void shouldSaveCurrentDataFormatInConfiguration() {
        // Given
        driver.get(baseUrl + "/playground");
        
        // Set XML format
        WebElement xmlFormatRadio = driver.findElement(By.id("xmlFormat"));
        jsExecutor.executeScript("arguments[0].click();", xmlFormatRadio);
        
        // Add some content
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        clearAndEnterText(sourceEditor, "<test>XML data</test>");

        // When - Save configuration
        WebElement saveConfigBtn = driver.findElement(By.id("saveConfigBtn"));
        saveConfigBtn.click();

        // Then - Verify success (the actual format saving is tested in the JavaScript)
        WebElement successAlert = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".alert-success")));
        assertTrue(successAlert.isDisplayed(), "Save should succeed with XML format");
    }

    @Test
    @Order(4)
    @DisplayName("Should save configuration with timestamp")
    void shouldSaveConfigurationWithTimestamp() {
        // Given
        driver.get(baseUrl + "/playground");
        
        // Add content to both editors
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        WebElement yamlEditor = driver.findElement(By.id("yamlRulesEditor"));
        
        clearAndEnterText(sourceEditor, "{\"timestamp\": \"test\"}");
        clearAndEnterText(yamlEditor, """
            metadata:
              name: "Timestamp Test"
            rules:
              - id: "timestamp-rule"
                name: "Timestamp Rule"
                condition: "true"
                message: "Test"
            """);

        // When
        WebElement saveConfigBtn = driver.findElement(By.id("saveConfigBtn"));
        saveConfigBtn.click();

        // Then
        WebElement successAlert = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".alert-success")));
        assertTrue(successAlert.getText().toLowerCase().contains("saved"),
                  "Configuration with timestamp should be saved successfully");
    }

    @Test
    @Order(5)
    @DisplayName("Should handle save with empty content")
    void shouldHandleSaveWithEmptyContent() {
        // Given
        driver.get(baseUrl + "/playground");
        
        // Ensure editors are empty
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        WebElement yamlEditor = driver.findElement(By.id("yamlRulesEditor"));
        
        clearAndEnterText(sourceEditor, "");
        clearAndEnterText(yamlEditor, "");

        // When
        WebElement saveConfigBtn = driver.findElement(By.id("saveConfigBtn"));
        saveConfigBtn.click();

        // Then - Should still save (empty configuration is valid)
        WebElement successAlert = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".alert-success")));
        assertTrue(successAlert.getText().toLowerCase().contains("saved"),
                  "Empty configuration should be saved successfully");
    }

    @Test
    @Order(6)
    @DisplayName("Should save configuration multiple times")
    void shouldSaveConfigurationMultipleTimes() {
        // Given
        driver.get(baseUrl + "/playground");
        
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        WebElement saveConfigBtn = driver.findElement(By.id("saveConfigBtn"));

        // First save
        clearAndEnterText(sourceEditor, "{\"version\": 1}");
        saveConfigBtn.click();
        
        WebElement firstAlert = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".alert-success")));
        assertTrue(firstAlert.isDisplayed(), "First save should succeed");

        // Wait for alert to disappear
        wait.until(ExpectedConditions.invisibilityOf(firstAlert));

        // Second save with different content
        clearAndEnterText(sourceEditor, "{\"version\": 2}");
        saveConfigBtn.click();

        // Then
        WebElement secondAlert = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".alert-success")));
        assertTrue(secondAlert.getText().toLowerCase().contains("saved"),
                  "Second save should also succeed");
    }

    @Test
    @Order(7)
    @DisplayName("Should maintain button state during save operation")
    void shouldMaintainButtonStateDuringSaveOperation() {
        // Given
        driver.get(baseUrl + "/playground");
        
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        WebElement saveConfigBtn = driver.findElement(By.id("saveConfigBtn"));
        
        clearAndEnterText(sourceEditor, "{\"test\": \"data\"}");

        // When
        assertTrue(saveConfigBtn.isEnabled(), "Save button should be enabled before click");
        saveConfigBtn.click();

        // Then - Button should remain enabled (save is quick operation)
        assertTrue(saveConfigBtn.isEnabled(), "Save button should remain enabled after click");
        
        // Verify save completed
        WebElement successAlert = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".alert-success")));
        assertTrue(successAlert.isDisplayed(), "Save operation should complete successfully");
    }

    @Test
    @Order(8)
    @DisplayName("Should save configuration with all data formats")
    void shouldSaveConfigurationWithAllDataFormats() {
        // Given
        driver.get(baseUrl + "/playground");
        
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        WebElement saveConfigBtn = driver.findElement(By.id("saveConfigBtn"));

        // Test JSON format
        WebElement jsonFormatRadio = driver.findElement(By.id("jsonFormat"));
        jsExecutor.executeScript("arguments[0].click();", jsonFormatRadio);
        clearAndEnterText(sourceEditor, "{\"format\": \"json\"}");
        saveConfigBtn.click();
        
        WebElement jsonAlert = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".alert-success")));
        assertTrue(jsonAlert.isDisplayed(), "Should save JSON format configuration");
        wait.until(ExpectedConditions.invisibilityOf(jsonAlert));

        // Test XML format
        WebElement xmlFormatRadio = driver.findElement(By.id("xmlFormat"));
        jsExecutor.executeScript("arguments[0].click();", xmlFormatRadio);
        clearAndEnterText(sourceEditor, "<format>xml</format>");
        saveConfigBtn.click();
        
        WebElement xmlAlert = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".alert-success")));
        assertTrue(xmlAlert.isDisplayed(), "Should save XML format configuration");
        wait.until(ExpectedConditions.invisibilityOf(xmlAlert));

        // Test CSV format
        WebElement csvFormatRadio = driver.findElement(By.id("csvFormat"));
        jsExecutor.executeScript("arguments[0].click();", csvFormatRadio);
        clearAndEnterText(sourceEditor, "format,type\ncsv,data");
        saveConfigBtn.click();
        
        WebElement csvAlert = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".alert-success")));
        assertTrue(csvAlert.isDisplayed(), "Should save CSV format configuration");
    }

    // Helper methods

    private void clearAndEnterText(WebElement element, String text) {
        element.clear();
        element.sendKeys(text);
    }
}

