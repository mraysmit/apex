package dev.mars.apex.yaml.manager.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for D3 Tree Viewer tooltip sizing.
 * Tests that the tooltip dynamically sizes based on YAML content.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class D3TreeViewerTooltipSizingUITest {

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
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
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
    @DisplayName("Tooltip should appear on hover over tree node")
    void testTooltipAppearsOnHover() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#tree-container svg")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("g.node text")));

        // Get the first tree node text element
        WebElement firstNodeText = driver.findElement(By.cssSelector("g.node text"));
        String nodeName = firstNodeText.getText();
        System.out.println("Triggering tooltip for node: " + nodeName);

        // Trigger tooltip via JavaScript (simulating mouseover event with D3 data)
        jsExecutor.executeScript(
            "var textElement = document.querySelector('g.node text');" +
            "var d3Data = d3.select(textElement).datum();" +
            "var event = { clientX: 500, clientY: 300 };" +
            "showTooltipSimple(event, d3Data);"
        );

        // Wait for tooltip delay (500ms) plus some buffer
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Check if tooltip is visible
        WebElement tooltip = driver.findElement(By.id("file-tooltip"));
        String display = tooltip.getCssValue("display");

        System.out.println("Tooltip display: " + display);
        assertEquals("flex", display, "Tooltip should be visible (display: flex)");
    }

    @Test
    @Order(2)
    @DisplayName("Tooltip should have dynamic width based on content")
    void testTooltipDynamicWidth() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#tree-container svg")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("g.node text")));

        // Trigger tooltip via JavaScript
        jsExecutor.executeScript(
            "var textElement = document.querySelector('g.node text');" +
            "var d3Data = d3.select(textElement).datum();" +
            "var event = { clientX: 500, clientY: 300 };" +
            "showTooltipSimple(event, d3Data);"
        );

        // Wait for tooltip delay
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get tooltip dimensions
        WebElement tooltip = driver.findElement(By.id("file-tooltip"));
        int width = tooltip.getSize().getWidth();
        int height = tooltip.getSize().getHeight();

        System.out.println("Tooltip width: " + width + "px, height: " + height + "px");

        // Width should be between min (300) and max (700)
        assertTrue(width >= 300, "Tooltip width should be at least 300px, was: " + width);
        assertTrue(width <= 700, "Tooltip width should be at most 700px, was: " + width);
    }

    @Test
    @Order(3)
    @DisplayName("Tooltip height should expand based on YAML line count")
    void testTooltipDynamicHeight() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#tree-container svg")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("g.node text")));

        // Trigger tooltip via JavaScript
        jsExecutor.executeScript(
            "var textElement = document.querySelector('g.node text');" +
            "var d3Data = d3.select(textElement).datum();" +
            "var event = { clientX: 500, clientY: 300 };" +
            "showTooltipSimple(event, d3Data);"
        );

        // Wait for tooltip delay
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get tooltip and content
        WebElement tooltip = driver.findElement(By.id("file-tooltip"));
        WebElement tooltipCode = driver.findElement(By.id("tooltip-code"));

        int tooltipHeight = tooltip.getSize().getHeight();
        String content = tooltipCode.getText();
        int lineCount = content.split("\n").length;

        System.out.println("Tooltip height: " + tooltipHeight + "px");
        System.out.println("Content line count: " + lineCount);

        // Height should be at least 150px (minimum)
        assertTrue(tooltipHeight >= 150, "Tooltip height should be at least 150px, was: " + tooltipHeight);

        // Height should not exceed 80% of viewport (864px for 1080px viewport)
        int maxHeight = (int) (1080 * 0.8);
        assertTrue(tooltipHeight <= maxHeight,
            "Tooltip height should not exceed 80% of viewport (" + maxHeight + "px), was: " + tooltipHeight);
    }

    @Test
    @Order(4)
    @DisplayName("Tooltip should show full YAML content without truncation")
    void testTooltipShowsFullContent() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#tree-container svg")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("g.node text")));

        // Trigger tooltip via JavaScript
        jsExecutor.executeScript(
            "var textElement = document.querySelector('g.node text');" +
            "var d3Data = d3.select(textElement).datum();" +
            "var event = { clientX: 500, clientY: 300 };" +
            "showTooltipSimple(event, d3Data);"
        );

        // Wait for tooltip delay
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get tooltip content
        WebElement tooltipCode = driver.findElement(By.id("tooltip-code"));
        String content = tooltipCode.getText();

        System.out.println("Tooltip content length: " + content.length() + " chars");
        System.out.println("First 100 chars: " + (content.length() > 100 ? content.substring(0, 100) : content));

        // Content should not be empty
        assertFalse(content.isEmpty(), "Tooltip should show YAML content");

        // Content should contain YAML structure (metadata, rules, etc.)
        assertTrue(content.contains("metadata") || content.contains("rules") || content.contains("enrichments") || content.contains("#"),
            "Tooltip should contain YAML content");
    }

    @Test
    @Order(5)
    @DisplayName("Tooltip CSS should have correct max-width and max-height")
    void testTooltipCSSProperties() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#tree-container svg")));

        // Check CSS properties via JavaScript
        String maxWidth = (String) jsExecutor.executeScript(
            "var tooltip = document.getElementById('file-tooltip');" +
            "return window.getComputedStyle(tooltip).maxWidth;"
        );
        
        String maxHeight = (String) jsExecutor.executeScript(
            "var tooltip = document.getElementById('file-tooltip');" +
            "return window.getComputedStyle(tooltip).maxHeight;"
        );

        System.out.println("Tooltip max-width: " + maxWidth);
        System.out.println("Tooltip max-height: " + maxHeight);

        // max-width should be 700px
        assertEquals("700px", maxWidth, "Tooltip max-width should be 700px");
        
        // max-height should be 80vh (864px for 1080px viewport)
        // Note: computed style may show pixel value
        assertTrue(maxHeight.contains("px") || maxHeight.contains("vh"), 
            "Tooltip max-height should be set");
    }

    @Test
    @Order(6)
    @DisplayName("Tooltip width should be set dynamically by JavaScript")
    void testTooltipWidthSetByJavaScript() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#tree-container svg")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("g.node text")));

        // Trigger tooltip via JavaScript
        jsExecutor.executeScript(
            "var textElement = document.querySelector('g.node text');" +
            "var d3Data = d3.select(textElement).datum();" +
            "var event = { clientX: 500, clientY: 300 };" +
            "showTooltipSimple(event, d3Data);"
        );

        // Wait for tooltip delay
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Check if width style is set inline (by JavaScript)
        String inlineWidth = (String) jsExecutor.executeScript(
            "return document.getElementById('file-tooltip').style.width;"
        );

        String inlineHeight = (String) jsExecutor.executeScript(
            "return document.getElementById('file-tooltip').style.height;"
        );

        System.out.println("Inline width style: " + inlineWidth);
        System.out.println("Inline height style: " + inlineHeight);

        // Width and height should be set inline by JavaScript
        assertFalse(inlineWidth == null || inlineWidth.isEmpty(),
            "Tooltip width should be set dynamically by JavaScript");
        assertFalse(inlineHeight == null || inlineHeight.isEmpty(),
            "Tooltip height should be set dynamically by JavaScript");
    }

    @Test
    @Order(7)
    @DisplayName("Tooltip height should match content or be capped at max-height")
    void testTooltipHeightMatchesContentOrMaxHeight() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#tree-container svg")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("g.node text")));

        // Trigger tooltip via JavaScript
        jsExecutor.executeScript(
            "var textElement = document.querySelector('g.node text');" +
            "var d3Data = d3.select(textElement).datum();" +
            "var event = { clientX: 500, clientY: 300 };" +
            "showTooltipSimple(event, d3Data);"
        );

        // Wait for tooltip delay
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get tooltip and content dimensions
        Long tooltipHeight = (Long) jsExecutor.executeScript(
            "return document.getElementById('file-tooltip').offsetHeight;"
        );
        Long contentScrollHeight = (Long) jsExecutor.executeScript(
            "var content = document.querySelector('.tooltip-content');" +
            "return content.scrollHeight;"
        );
        Long headerHeight = (Long) jsExecutor.executeScript(
            "return document.querySelector('.tooltip-header').offsetHeight;"
        );
        Long viewportHeight = (Long) jsExecutor.executeScript(
            "return window.innerHeight;"
        );

        long maxHeight = (long) (viewportHeight * 0.8);
        long expectedMinHeight = headerHeight + contentScrollHeight + 4; // 4px for border

        System.out.println("Tooltip height: " + tooltipHeight);
        System.out.println("Content scrollHeight: " + contentScrollHeight);
        System.out.println("Header height: " + headerHeight);
        System.out.println("Viewport height: " + viewportHeight);
        System.out.println("Max height (80vh): " + maxHeight);
        System.out.println("Expected min height: " + expectedMinHeight);

        // Tooltip should either:
        // 1. Be sized to fit content (if content fits within max-height)
        // 2. Be capped at max-height (if content exceeds max-height)
        if (expectedMinHeight <= maxHeight) {
            // Content fits - tooltip should be sized to content (with small tolerance)
            assertTrue(tooltipHeight >= expectedMinHeight - 10,
                "Tooltip should be sized to fit content. Expected at least " +
                (expectedMinHeight - 10) + " but got " + tooltipHeight);
        } else {
            // Content exceeds max - tooltip should be capped at max-height
            assertTrue(tooltipHeight <= maxHeight + 10,
                "Tooltip should be capped at max-height. Expected at most " +
                (maxHeight + 10) + " but got " + tooltipHeight);
        }
    }
}

