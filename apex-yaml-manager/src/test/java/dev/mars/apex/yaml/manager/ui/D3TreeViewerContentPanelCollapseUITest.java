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
 * Selenium UI test for the D3 Tree Viewer content panel (right panel) collapse functionality.
 * This test verifies that when the right content panel is collapsed:
 * 1. The tree panel expands to fill the space
 * 2. The SVG inside tree-container resizes to match the new tree panel width
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class D3TreeViewerContentPanelCollapseUITest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setup() {
        ChromeOptions options = new ChromeOptions();
        options.setBinary("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
        // NOT headless - so we can see what's happening
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.setCapability("goog:loggingPrefs", java.util.Map.of("browser", "ALL"));

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        baseUrl = "http://localhost:" + port + "/yaml-manager";
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test content panel collapse - tree panel and SVG expand to fill space")
    void testContentPanelCollapse() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement treePanel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".tree-panel")));
        WebElement contentPanel = driver.findElement(By.id("content-panel"));
        WebElement contentCloseBtn = driver.findElement(By.id("content-close-btn"));
        WebElement treeContainer = driver.findElement(By.id("tree-container"));
        
        // Wait for SVG to be created
        WebElement svg = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg")));

        // Get initial widths
        int initialTreePanelWidth = treePanel.getSize().getWidth();
        int initialContentPanelWidth = contentPanel.getSize().getWidth();
        int initialTreeContainerWidth = treeContainer.getSize().getWidth();
        String initialSvgWidth = svg.getAttribute("width");

        System.out.println("\n=== BEFORE COLLAPSE ===");
        System.out.println("Tree panel width: " + initialTreePanelWidth + "px");
        System.out.println("Content panel width: " + initialContentPanelWidth + "px");
        System.out.println("Tree container width: " + initialTreeContainerWidth + "px");
        System.out.println("SVG width attribute: " + initialSvgWidth + "px");

        // Click the close button
        contentCloseBtn.click();

        // Wait for animation to complete and SVG to resize
        try {
            Thread.sleep(600); // Give time for CSS transition + setTimeout callbacks
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get widths after collapse
        int afterTreePanelWidth = treePanel.getSize().getWidth();
        int afterContentPanelWidth = contentPanel.getSize().getWidth();
        int afterTreeContainerWidth = treeContainer.getSize().getWidth();
        String afterSvgWidth = svg.getAttribute("width");

        System.out.println("\n=== AFTER COLLAPSE ===");
        System.out.println("Tree panel width: " + afterTreePanelWidth + "px");
        System.out.println("Content panel width: " + afterContentPanelWidth + "px");
        System.out.println("Tree container width: " + afterTreeContainerWidth + "px");
        System.out.println("SVG width attribute: " + afterSvgWidth + "px");

        // CRITICAL ASSERTIONS
        assertTrue(afterTreePanelWidth > initialTreePanelWidth,
            "Tree panel width should INCREASE after content panel collapse");

        assertTrue(afterContentPanelWidth < initialContentPanelWidth,
            "Content panel width should DECREASE (collapse) to near zero");

        assertTrue(afterTreeContainerWidth > initialTreeContainerWidth,
            "Tree container width should INCREASE to match expanded tree panel");

        // SVG should resize to match the new tree container width
        int afterSvgWidthInt = Integer.parseInt(afterSvgWidth);
        assertTrue(afterSvgWidthInt > Integer.parseInt(initialSvgWidth),
            "SVG width should INCREASE after content panel collapse");

        // SVG width should be approximately tree container width - 20px
        assertEquals(afterTreeContainerWidth - 20, afterSvgWidthInt, 50,
            "SVG width should be approximately tree container width - 20px");

        // Verify content panel has collapsed class
        String contentPanelClass = contentPanel.getAttribute("class");
        assertTrue(contentPanelClass.contains("collapsed"),
            "Content panel should have 'collapsed' class");

        // Verify tree panel has expanded class
        String treePanelClass = treePanel.getAttribute("class");
        assertTrue(treePanelClass.contains("expanded"),
            "Tree panel should have 'expanded' class");
    }

    @Test
    @Order(2)
    @DisplayName("Test content panel expand - tree panel and SVG shrink back")
    void testContentPanelExpand() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement treePanel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".tree-panel")));
        WebElement contentPanel = driver.findElement(By.id("content-panel"));
        WebElement contentCloseBtn = driver.findElement(By.id("content-close-btn"));
        WebElement treeContainer = driver.findElement(By.id("tree-container"));
        
        // Wait for SVG to be created
        WebElement svg = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg")));

        // Get initial widths
        int initialTreePanelWidth = treePanel.getSize().getWidth();
        String initialSvgWidth = svg.getAttribute("width");

        // Collapse content panel
        contentCloseBtn.click();
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get widths after collapse
        int collapsedTreePanelWidth = treePanel.getSize().getWidth();
        String collapsedSvgWidth = svg.getAttribute("width");

        System.out.println("\n=== AFTER COLLAPSE ===");
        System.out.println("Tree panel width: " + collapsedTreePanelWidth + "px");
        System.out.println("SVG width: " + collapsedSvgWidth + "px");

        // Now expand content panel again
        WebElement contentToggleBtn = driver.findElement(By.id("content-toggle-btn"));
        contentToggleBtn.click();

        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get widths after expand
        int expandedTreePanelWidth = treePanel.getSize().getWidth();
        String expandedSvgWidth = svg.getAttribute("width");

        System.out.println("\n=== AFTER EXPAND ===");
        System.out.println("Tree panel width: " + expandedTreePanelWidth + "px");
        System.out.println("SVG width: " + expandedSvgWidth + "px");

        // CRITICAL ASSERTIONS
        assertTrue(expandedTreePanelWidth < collapsedTreePanelWidth,
            "Tree panel width should DECREASE after content panel expands");

        assertEquals(initialTreePanelWidth, expandedTreePanelWidth, 50,
            "Tree panel should return to approximately initial width");

        // SVG should resize back
        int expandedSvgWidthInt = Integer.parseInt(expandedSvgWidth);
        assertTrue(expandedSvgWidthInt < Integer.parseInt(collapsedSvgWidth),
            "SVG width should DECREASE after content panel expands");

        assertEquals(Integer.parseInt(initialSvgWidth), expandedSvgWidthInt, 50,
            "SVG should return to approximately initial width");

        // Verify content panel does NOT have collapsed class
        String contentPanelClass = contentPanel.getAttribute("class");
        assertFalse(contentPanelClass.contains("collapsed"),
            "Content panel should NOT have 'collapsed' class after expand");

        // Verify tree panel does NOT have expanded class
        String treePanelClass = treePanel.getAttribute("class");
        assertFalse(treePanelClass.contains("expanded"),
            "Tree panel should NOT have 'expanded' class after content panel expands");
    }

    @Test
    @Order(3)
    @DisplayName("Test sidebar collapse also resizes SVG")
    void testSidebarCollapseResizesSVG() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement treePanel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".tree-panel")));
        WebElement sidebar = driver.findElement(By.id("sidebar"));
        WebElement sidebarCloseBtn = driver.findElement(By.id("sidebar-close-btn"));
        
        // Wait for SVG to be created
        WebElement svg = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg")));

        // Get initial widths
        int initialTreePanelWidth = treePanel.getSize().getWidth();
        String initialSvgWidth = svg.getAttribute("width");

        System.out.println("\n=== BEFORE SIDEBAR COLLAPSE ===");
        System.out.println("Tree panel width: " + initialTreePanelWidth + "px");
        System.out.println("SVG width: " + initialSvgWidth + "px");

        // Collapse sidebar
        sidebarCloseBtn.click();
        try {
            Thread.sleep(800); // Wait for CSS transition (300ms) + setTimeout (350ms) + buffer
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get widths after collapse
        int afterTreePanelWidth = treePanel.getSize().getWidth();
        String afterSvgWidth = svg.getAttribute("width");

        System.out.println("\n=== AFTER SIDEBAR COLLAPSE ===");
        System.out.println("Tree panel width: " + afterTreePanelWidth + "px");
        System.out.println("SVG width: " + afterSvgWidth + "px");

        // Print console logs to see if resizeTreeSVG was called
        System.out.println("\n=== BROWSER CONSOLE LOGS ===");
        driver.manage().logs().get("browser").forEach(entry -> {
            System.out.println(entry.getLevel() + " " + entry.getMessage());
        });

        // CRITICAL ASSERTIONS
        assertTrue(afterTreePanelWidth > initialTreePanelWidth,
            "Tree panel width should INCREASE after sidebar collapse");

        // SVG should resize to match the new tree panel width
        int afterSvgWidthInt = Integer.parseInt(afterSvgWidth);
        assertTrue(afterSvgWidthInt > Integer.parseInt(initialSvgWidth),
            "SVG width should INCREASE after sidebar collapse");

        // SVG width should be approximately tree panel width - 20px (allow more tolerance for sidebar)
        assertEquals(afterTreePanelWidth - 20, afterSvgWidthInt, 150,
            "SVG width should be approximately tree panel width - 20px");
    }
}

