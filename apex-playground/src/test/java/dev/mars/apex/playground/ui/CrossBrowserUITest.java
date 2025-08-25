package dev.mars.apex.playground.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-browser UI tests for the APEX Playground.
 * Tests the same functionality across Chrome, Firefox, and Edge browsers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=WARN",
    "logging.level.org.springframework=WARN"
})
@DisplayName("Cross-Browser UI Tests")
class CrossBrowserUITest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    /**
     * Supported browsers for cross-browser testing
     */
    enum Browser {
        CHROME, FIREFOX, EDGE
    }

    @BeforeAll
    static void setupClass() {
        // Setup WebDriverManager for available browsers only
        try {
            WebDriverManager.chromedriver().setup();
        } catch (Exception e) {
            System.out.println("Chrome driver setup failed: " + e.getMessage());
        }

        try {
            WebDriverManager.firefoxdriver().setup();
        } catch (Exception e) {
            System.out.println("Firefox driver setup failed: " + e.getMessage());
        }

        try {
            WebDriverManager.edgedriver().setup();
        } catch (Exception e) {
            System.out.println("Edge driver setup failed: " + e.getMessage());
        }
    }

    void setUp(Browser browser) {
        driver = createDriver(browser);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private WebDriver createDriver(Browser browser) {
        try {
            return switch (browser) {
                case CHROME -> {
                    ChromeOptions options = new ChromeOptions();
                    options.addArguments("--headless");
                    options.addArguments("--no-sandbox");
                    options.addArguments("--disable-dev-shm-usage");
                    options.addArguments("--disable-gpu");
                    options.addArguments("--window-size=1920,1080");
                    yield new ChromeDriver(options);
                }
                case FIREFOX -> {
                    FirefoxOptions options = new FirefoxOptions();
                    options.addArguments("--headless");
                    options.addArguments("--width=1920");
                    options.addArguments("--height=1080");
                    yield new FirefoxDriver(options);
                }
                case EDGE -> {
                    EdgeOptions options = new EdgeOptions();
                    options.addArguments("--headless");
                    options.addArguments("--no-sandbox");
                    options.addArguments("--disable-dev-shm-usage");
                    options.addArguments("--disable-gpu");
                    options.addArguments("--window-size=1920,1080");
                    yield new EdgeDriver(options);
                }
            };
        } catch (Exception e) {
            // If browser is not available, skip the test
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                browser + " browser is not available: " + e.getMessage());
            return null; // This line won't be reached due to assumption failure
        }
    }

    @ParameterizedTest(name = "Should load playground page successfully in {0}")
    @EnumSource(Browser.class)
    @DisplayName("Page Loading Test")
    void shouldLoadPlaygroundPageInAllBrowsers(Browser browser) {
        // Given
        setUp(browser);

        // When
        driver.get(baseUrl + "/playground");

        // Then
        assertEquals("APEX Playground", driver.getTitle());
        
        // Verify main UI elements are present
        assertTrue(isElementPresent(By.id("processBtn")), 
                  "Process button should be present in " + browser);
        assertTrue(isElementPresent(By.id("validateBtn")), 
                  "Validate button should be present in " + browser);
        assertTrue(isElementPresent(By.id("clearBtn")), 
                  "Clear button should be present in " + browser);
        assertTrue(isElementPresent(By.id("loadExampleBtn")), 
                  "Load Example button should be present in " + browser);
        
        // Verify editors are present
        assertTrue(isElementPresent(By.id("sourceDataEditor")), 
                  "Source data editor should be present in " + browser);
        assertTrue(isElementPresent(By.id("yamlRulesEditor")), 
                  "YAML rules editor should be present in " + browser);
    }

    @ParameterizedTest(name = "Should process data successfully in {0}")
    @EnumSource(Browser.class)
    @DisplayName("Data Processing Test")
    void shouldProcessDataInAllBrowsers(Browser browser) {
        // Given
        setUp(browser);
        driver.get(baseUrl + "/playground");
        
        String jsonData = """
            {
              "name": "Cross-Browser Test",
              "age": 30,
              "browser": "%s"
            }
            """.formatted(browser.name());
            
        String yamlRules = """
            metadata:
              name: "Cross-Browser Test Rules"
              version: "1.0.0"
            
            rules:
              - id: "browser-test"
                name: "Browser Test Rule"
                condition: "#age >= 18"
                message: "Age requirement met for %s"
            """.formatted(browser.name());

        // When - Fill in data and process
        WebElement dataArea = driver.findElement(By.id("sourceDataEditor"));
        WebElement yamlArea = driver.findElement(By.id("yamlRulesEditor"));
        
        clearAndEnterText(dataArea, jsonData);
        clearAndEnterText(yamlArea, yamlRules);
        
        WebElement processBtn = driver.findElement(By.id("processBtn"));
        processBtn.click();

        // Then - Verify processing works
        WebElement validationArea = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("validationResults")));
        
        // Wait for results to be populated
        wait.until(ExpectedConditions.not(ExpectedConditions.textToBe(
            By.id("validationResults"), "Click \"Process\" to see validation results...")));
        
        String validationText = validationArea.getText().toLowerCase();
        assertTrue(validationText.contains("success") || validationText.contains("valid"), 
                  "Processing should work in " + browser + ". Got: " + validationText);
    }

    @ParameterizedTest(name = "Should validate YAML successfully in {0}")
    @EnumSource(Browser.class)
    @DisplayName("YAML Validation Test")
    void shouldValidateYamlInAllBrowsers(Browser browser) {
        // Given
        setUp(browser);
        driver.get(baseUrl + "/playground");
        
        String validYaml = """
            metadata:
              name: "%s Browser Test"
              version: "1.0.0"
              description: "Testing YAML validation in %s"
            
            rules:
              - id: "browser-validation-test"
                name: "Browser Validation Test"
                condition: "#age >= 21"
                message: "Validation test for %s"
            """.formatted(browser.name(), browser.name(), browser.name());

        // When - Fill in YAML and validate
        WebElement yamlTextArea = driver.findElement(By.id("yamlRulesEditor"));
        clearAndEnterText(yamlTextArea, validYaml);
        
        WebElement validateBtn = driver.findElement(By.id("validateBtn"));
        validateBtn.click();

        // Then - Verify validation status updates
        WebElement statusBadge = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("yamlStatus")));
        
        // Wait for the status to update
        wait.until(ExpectedConditions.or(
            ExpectedConditions.textToBe(By.id("yamlStatus"), "YAML configuration is valid"),
            ExpectedConditions.attributeContains(By.id("yamlStatus"), "class", "bg-success")
        ));
        
        String statusText = statusBadge.getText().toLowerCase();
        String statusClass = statusBadge.getDomAttribute("class");
        
        assertTrue(statusText.contains("valid") || statusClass.contains("bg-success"), 
                  "YAML validation should work in " + browser + 
                  ". Status: " + statusText + ", Class: " + statusClass);
    }

    @ParameterizedTest(name = "Should handle responsive design in {0}")
    @EnumSource(Browser.class)
    @DisplayName("Responsive Design Test")
    void shouldHandleResponsiveDesignInAllBrowsers(Browser browser) {
        // Given
        setUp(browser);
        
        // Test different viewport sizes
        testViewportSize(browser, 1920, 1080, "Desktop");
        testViewportSize(browser, 768, 1024, "Tablet");
        testViewportSize(browser, 375, 667, "Mobile");
    }

    private void testViewportSize(Browser browser, int width, int height, String deviceType) {
        // Set viewport size
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(width, height));
        driver.get(baseUrl + "/playground");
        
        // Verify essential elements are still accessible
        assertTrue(isElementPresent(By.id("processBtn")), 
                  "Process button should be accessible on " + deviceType + " in " + browser);
        assertTrue(isElementPresent(By.id("sourceDataEditor")), 
                  "Source editor should be accessible on " + deviceType + " in " + browser);
        assertTrue(isElementPresent(By.id("yamlRulesEditor")), 
                  "YAML editor should be accessible on " + deviceType + " in " + browser);
        
        // Verify elements are visible (not hidden by responsive design)
        WebElement processBtn = driver.findElement(By.id("processBtn"));
        assertTrue(processBtn.isDisplayed(), 
                  "Process button should be visible on " + deviceType + " in " + browser);
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

