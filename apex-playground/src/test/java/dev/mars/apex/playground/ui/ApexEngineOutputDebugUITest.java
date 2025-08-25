package dev.mars.apex.playground.ui;

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
 * Debug test to see what the APEX engine actually outputs in the UI.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=DEBUG",
    "apex.playground.examples-enabled=true"
})
class ApexEngineOutputDebugUITest {

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor jsExecutor;
    private String baseUrl;

    @LocalServerPort
    private int port;

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

    @Test
    @DisplayName("Debug: Show actual APEX engine output in validation and enrichment panels")
    void debugShowActualApexEngineOutput() {
        // Given
        driver.get(baseUrl + "/playground");
        
        // Simple test data
        String jsonData = """
            {
              "name": "John Doe",
              "age": 25,
              "email": "john.doe@example.com"
            }
            """;
            
        String yamlRules = """
            metadata:
              name: "Debug Test"
              version: "1.0.0"
            
            rules:
              - id: "age-check"
                name: "Age Check Rule"
                condition: "#age >= 18"
                message: "Age is valid"
                enabled: true
            """;

        // When - Enter data and process
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        WebElement yamlEditor = driver.findElement(By.id("yamlRulesEditor"));
        
        sourceEditor.clear();
        sourceEditor.sendKeys(jsonData);
        
        yamlEditor.clear();
        yamlEditor.sendKeys(yamlRules);
        
        WebElement processBtn = driver.findElement(By.id("processBtn"));
        processBtn.click();

        // Then - Debug what's actually displayed
        WebElement validationResults = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("validationResults")));
        WebElement enrichmentResults = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("enrichmentResults")));
        
        // Wait for processing to complete
        wait.until(ExpectedConditions.not(ExpectedConditions.textToBe(
            By.id("validationResults"), "Click \"Process\" to see validation results...")));

        String validationContent = validationResults.getText();
        String enrichmentContent = enrichmentResults.getText();
        
        // Debug output using assertions to see content
        assertNotNull(validationContent, "Validation content should not be null. Content: " + validationContent);
        assertNotNull(enrichmentContent, "Enrichment content should not be null. Content: " + enrichmentContent);

        // Verify APEX engine is working correctly
        assertTrue(validationContent.contains("rulesExecuted"), "Should contain APEX engine validation structure");
        assertTrue(enrichmentContent.contains("enrichedData"), "Should contain APEX engine enrichment structure");
        
        // Check processing time
        WebElement processingTime = driver.findElement(By.id("processingTime"));
        String timeText = processingTime.getText();
        System.out.println("=== PROCESSING TIME ===");
        System.out.println(timeText);
        
        assertTrue(timeText.contains("ms"), "Processing time should be displayed");
    }

    @Test
    @DisplayName("Debug: Test with known working example from playground")
    void debugTestWithKnownWorkingExample() {
        // Given
        driver.get(baseUrl + "/playground");
        
        // Use the same data as the working PlaygroundUITest
        String jsonData = """
            {
              "name": "John Doe",
              "age": 30,
              "email": "john.doe@example.com",
              "amount": 1500.00,
              "currency": "USD"
            }
            """;
            
        String yamlRules = """
            metadata:
              name: "Test Configuration"
              version: "1.0.0"
              description: "Test YAML configuration"
            
            rules:
              - id: "age-validation"
                name: "Age Validation"
                condition: "#age >= 18"
                message: "Age requirement met"
                enabled: true
                priority: 1
            
              - id: "amount-validation"
                name: "Amount Validation"
                condition: "#amount > 1000"
                message: "Amount exceeds minimum"
                enabled: true
                priority: 2
            """;

        // When
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        WebElement yamlEditor = driver.findElement(By.id("yamlRulesEditor"));
        
        sourceEditor.clear();
        sourceEditor.sendKeys(jsonData);
        
        yamlEditor.clear();
        yamlEditor.sendKeys(yamlRules);
        
        WebElement processBtn = driver.findElement(By.id("processBtn"));
        processBtn.click();

        // Then - Debug output
        WebElement validationResults = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("validationResults")));
        
        wait.until(ExpectedConditions.not(ExpectedConditions.textToBe(
            By.id("validationResults"), "Click \"Process\" to see validation results...")));

        String validationContent = validationResults.getText();
        String enrichmentContent = driver.findElement(By.id("enrichmentResults")).getText();
        
        System.out.println("=== WORKING EXAMPLE VALIDATION RESULTS ===");
        System.out.println(validationContent);
        System.out.println("=== WORKING EXAMPLE ENRICHMENT RESULTS ===");
        System.out.println(enrichmentContent);
        
        // Verify processing occurred
        assertFalse(validationContent.isEmpty(), "Validation results should not be empty");
        assertTrue(validationContent.length() > 50, "Validation results should contain substantial content");
    }

    @Test
    @DisplayName("Debug: Test API endpoint directly")
    void debugTestApiEndpointDirectly() {
        // This test will help us understand what the API returns
        driver.get(baseUrl + "/playground");
        
        // Use JavaScript to make direct API call and log response
        String script = """
            fetch('/playground/api/process', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    sourceData: '{"name": "Test", "age": 25}',
                    yamlRules: 'metadata:\\n  name: "Test"\\nrules:\\n  - id: "test"\\n    condition: "#age >= 18"\\n    message: "Valid"\\n    enabled: true',
                    dataFormat: 'JSON'
                })
            })
            .then(response => response.json())
            .then(data => {
                console.log('API Response:', data);
                window.apiResponse = data;
                return data;
            })
            .catch(error => {
                console.error('API Error:', error);
                window.apiError = error;
            });
            """;
        
        jsExecutor.executeScript(script);
        
        // Wait a moment for the API call
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Get the response from window object
        Object apiResponse = jsExecutor.executeScript("return window.apiResponse;");
        Object apiError = jsExecutor.executeScript("return window.apiError;");
        
        System.out.println("=== DIRECT API RESPONSE ===");
        System.out.println(apiResponse);
        if (apiError != null) {
            System.out.println("=== API ERROR ===");
            System.out.println(apiError);
        }
        
        assertNotNull(apiResponse, "API should return a response");
    }
}
