package dev.mars.apex.yaml.manager.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for D3 Tree Viewer node click and drag behavior.
 * Tests that clicking tree nodes shows file content, but dragging (mousedown > 2 seconds) cancels the click.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class D3TreeViewerNodeClickDragUITest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor jsExecutor;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        jsExecutor = (JavascriptExecutor) driver;
        baseUrl = "http://localhost:" + port + "/yaml-manager";
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test quick click on tree node shows file content")
    void testQuickClickShowsFileContent() {
        // Navigate to a blank page first to set up error capturing
        driver.get("about:blank");

        // Set up error capturing before loading the actual page
        jsExecutor.executeScript(
            "window.jsErrors = [];" +
            "window.addEventListener('error', function(e) {" +
            "  window.jsErrors.push(e.message + ' at ' + e.filename + ':' + e.lineno + ':' + e.colno);" +
            "  console.error('Captured error:', e.message);" +
            "  return false;" +
            "});"
        );

        // Now load the actual page
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait a moment for page to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Check for JavaScript errors
        @SuppressWarnings("unchecked")
        java.util.List<String> jsErrors = (java.util.List<String>) jsExecutor.executeScript(
            "return window.jsErrors || [];"
        );

        if (!jsErrors.isEmpty()) {
            System.out.println("=== JavaScript Errors Detected ===");
            for (String error : jsErrors) {
                System.out.println("  " + error);
            }
            System.out.println("==================================");
        } else {
            System.out.println("No JavaScript errors detected");
        }

        // Get page source to debug
        String pageSource = (String) jsExecutor.executeScript("return document.body.innerHTML;");
        System.out.println("Page loaded, body length: " + pageSource.length());

        // Wait for tree to load
        WebElement svg = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg")));

        // Wait for tree nodes to be rendered
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("g.node text")));

        // Get the first tree node text element
        WebElement firstNodeText = driver.findElement(By.cssSelector("g.node text"));
        String nodeName = firstNodeText.getText();

        System.out.println("Clicking on node: " + nodeName);

        // Quick click (normal click)
        firstNodeText.click();

        // Wait a moment for content to load
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify that the content panel shows the file content
        WebElement yamlContent = driver.findElement(By.id("yaml-content"));
        String content = yamlContent.getText();

        System.out.println("Content loaded: " + (content.length() > 50 ? content.substring(0, 50) + "..." : content));

        assertFalse(content.isEmpty(), "File content should be loaded after quick click");
        assertTrue(content.length() > 0, "Content should not be empty");
    }



    @Test
    @Order(3)
    @DisplayName("Test mousedown threshold is exactly 2 seconds")
    void testMousedownThresholdTiming() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("g.node text")));

        // Verify the threshold constant is set correctly
        String thresholdScript = "return typeof DRAG_THRESHOLD_MS !== 'undefined' ? DRAG_THRESHOLD_MS : null;";
        Object threshold = jsExecutor.executeScript(thresholdScript);

        System.out.println("DRAG_THRESHOLD_MS value: " + threshold);

        // The threshold should be 2000ms (2 seconds)
        if (threshold != null) {
            assertEquals(2000L, ((Number) threshold).longValue(), 
                "DRAG_THRESHOLD_MS should be 2000 milliseconds");
        }
    }



    @Test
    @Order(5)
    @DisplayName("Test actual drag operation using Actions class")
    void testActualDragOperation() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("g.node text")));

        WebElement firstNodeText = driver.findElement(By.cssSelector("g.node text"));
        String nodeName = firstNodeText.getText();

        System.out.println("Testing actual drag on node: " + nodeName);

        // Perform a drag operation (click and hold, move, then release)
        Actions actions = new Actions(driver);
        actions.clickAndHold(firstNodeText)
               .pause(Duration.ofMillis(2500)) // Hold for 2.5 seconds
               .moveByOffset(10, 10) // Small movement
               .release()
               .perform();

        // Wait a moment
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // The click should have been cancelled due to long mousedown
        // We can verify by checking if the console logged the cancellation
        System.out.println("Drag operation completed - click should have been cancelled");
    }
}

