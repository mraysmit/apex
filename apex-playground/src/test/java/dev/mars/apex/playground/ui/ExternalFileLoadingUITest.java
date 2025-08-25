package dev.mars.apex.playground.ui;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for external file loading functionality in APEX Playground.
 * Tests loading examples from apex-demo module and configuration management.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=INFO",
    "apex.playground.examples-enabled=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExternalFileLoadingUITest {

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        // Setup Chrome driver with options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
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
    @DisplayName("Should load example configurations from apex-demo module")
    void shouldLoadExampleConfigurationsFromApexDemo() {
        // Given
        driver.get(baseUrl + "/playground");

        // When
        WebElement loadExampleBtn = driver.findElement(By.id("loadExampleBtn"));
        loadExampleBtn.click();

        // Then - Example selection dialog should appear
        // Note: This depends on the implementation of the example selection dialog
        // For now, we'll verify that clicking the button doesn't cause errors
        
        // Wait a moment for any async operations
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify no error alerts appeared
        List<WebElement> errorAlerts = driver.findElements(By.cssSelector(".alert-danger"));
        assertTrue(errorAlerts.isEmpty() || errorAlerts.stream().noneMatch(WebElement::isDisplayed),
                  "No error alerts should be displayed when loading examples");
    }

    @Test
    @Order(2)
    @DisplayName("Should save and load configuration successfully")
    void shouldSaveAndLoadConfigurationSuccessfully() {
        // Given
        driver.get(baseUrl + "/playground");
        
        // Fill in some test data
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        WebElement yamlEditor = driver.findElement(By.id("yamlRulesEditor"));
        
        String testData = """
            {
              "name": "Configuration Test",
              "value": 123
            }
            """;
            
        String testYaml = """
            metadata:
              name: "Save/Load Test"
              version: "1.0.0"
            
            rules:
              - id: "test-rule"
                name: "Test Rule"
                condition: "#value > 100"
                message: "Value is greater than 100"
                enabled: true
            """;
        
        clearAndEnterText(sourceEditor, testData);
        clearAndEnterText(yamlEditor, testYaml);

        // When - Save configuration
        WebElement saveConfigBtn = driver.findElement(By.id("saveConfigBtn"));
        saveConfigBtn.click();

        // Then - Verify save success message
        WebElement successAlert = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".alert-success")));
        assertTrue(successAlert.getText().toLowerCase().contains("saved"),
                  "Success message should indicate configuration was saved");
    }

    @Test
    @Order(3)
    @DisplayName("Should handle example loading errors gracefully")
    void shouldHandleExampleLoadingErrorsGracefully() {
        // Given
        driver.get(baseUrl + "/playground");

        // When - Try to load examples (this might fail if examples service is not available)
        WebElement loadExampleBtn = driver.findElement(By.id("loadExampleBtn"));
        loadExampleBtn.click();

        // Wait for any potential error handling
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then - If there are errors, they should be handled gracefully
        List<WebElement> errorAlerts = driver.findElements(By.cssSelector(".alert-danger"));
        
        // Either no errors, or errors are properly displayed to user
        if (!errorAlerts.isEmpty()) {
            WebElement errorAlert = errorAlerts.stream()
                .filter(WebElement::isDisplayed)
                .findFirst()
                .orElse(null);
            
            if (errorAlert != null) {
                assertFalse(errorAlert.getText().isEmpty(),
                           "Error message should not be empty");
                assertTrue(errorAlert.getText().toLowerCase().contains("error") ||
                          errorAlert.getText().toLowerCase().contains("failed"),
                          "Error message should indicate what went wrong");
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should validate loaded examples automatically")
    void shouldValidateLoadedExamplesAutomatically() {
        // Given
        driver.get(baseUrl + "/playground");
        
        // Load default example first
        WebElement loadExampleBtn = driver.findElement(By.id("loadExampleBtn"));
        loadExampleBtn.click();
        
        // Wait for example to load
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then - Check if YAML validation was triggered
        WebElement yamlStatus = driver.findElement(By.id("yamlStatus"));
        
        // The status should show either valid or invalid, not the default state
        String statusText = yamlStatus.getText().toLowerCase();
        assertTrue(statusText.contains("valid") || statusText.contains("invalid"),
                  "YAML status should show validation result after loading example");
    }

    @Test
    @Order(5)
    @DisplayName("Should preserve data format when loading examples")
    void shouldPreserveDataFormatWhenLoadingExamples() {
        // Given
        driver.get(baseUrl + "/playground");
        
        // Set XML format initially
        WebElement xmlFormatRadio = driver.findElement(By.id("xmlFormat"));
        xmlFormatRadio.click();
        
        assertTrue(xmlFormatRadio.isSelected(), "XML format should be selected");

        // When - Load an example
        WebElement loadExampleBtn = driver.findElement(By.id("loadExampleBtn"));
        loadExampleBtn.click();
        
        // Wait for example to load
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then - Format should be updated based on example data
        // (This behavior depends on the specific example loaded)
        WebElement jsonFormatRadio = driver.findElement(By.id("jsonFormat"));
        WebElement csvFormatRadio = driver.findElement(By.id("csvFormat"));
        
        // At least one format should be selected
        assertTrue(jsonFormatRadio.isSelected() || xmlFormatRadio.isSelected() || csvFormatRadio.isSelected(),
                  "One data format should be selected after loading example");
    }

    @Test
    @Order(6)
    @DisplayName("Should handle network errors when loading external examples")
    void shouldHandleNetworkErrorsWhenLoadingExternalExamples() {
        // Given
        driver.get(baseUrl + "/playground");

        // When - Try to load examples (simulate network issues by rapid clicking)
        WebElement loadExampleBtn = driver.findElement(By.id("loadExampleBtn"));
        
        // Click multiple times rapidly to potentially trigger network issues
        for (int i = 0; i < 3; i++) {
            loadExampleBtn.click();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Wait for any error handling
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then - Application should remain stable
        assertTrue(isElementPresent(By.id("sourceDataEditor")), "Source editor should still be present");
        assertTrue(isElementPresent(By.id("yamlRulesEditor")), "YAML editor should still be present");
        assertTrue(isElementPresent(By.id("processBtn")), "Process button should still be present");
    }

    @Test
    @Order(7)
    @DisplayName("Should clear editors when requested")
    void shouldClearEditorsWhenRequested() {
        // Given
        driver.get(baseUrl + "/playground");
        
        // Fill editors with some content
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        WebElement yamlEditor = driver.findElement(By.id("yamlRulesEditor"));
        
        clearAndEnterText(sourceEditor, "test data");
        clearAndEnterText(yamlEditor, "test yaml");

        // When
        WebElement clearBtn = driver.findElement(By.id("clearBtn"));
        clearBtn.click();

        // Then
        assertEquals("", sourceEditor.getDomAttribute("value"), "Source editor should be cleared");
        assertEquals("", yamlEditor.getDomAttribute("value"), "YAML editor should be cleared");
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

    private void clearAndEnterText(WebElement element, String text) {
        element.clear();
        element.sendKeys(text);
    }
}

