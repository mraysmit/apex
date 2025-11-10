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
 * Selenium UI test for the D3 Tree Viewer sidebar collapse functionality.
 * This test verifies that when the left sidebar is collapsed, the tree panel expands
 * and the right content panel shifts to the right (does NOT expand).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class D3TreeViewerSidebarCollapseUITest {

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
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        
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
    @DisplayName("Test initial state - sidebar visible, all panels have correct widths")
    void testInitialState() {
        driver.get(baseUrl + "/d3-tree-viewer.html");
        
        WebElement sidebar = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sidebar")));
        WebElement treePanel = driver.findElement(By.cssSelector(".tree-panel"));
        WebElement contentPanel = driver.findElement(By.id("content-panel"));
        
        // Sidebar should be visible
        assertTrue(sidebar.isDisplayed(), "Sidebar should be visible initially");
        
        // Get initial widths
        int sidebarWidth = sidebar.getSize().getWidth();
        int treePanelWidth = treePanel.getSize().getWidth();
        int contentPanelWidth = contentPanel.getSize().getWidth();
        
        System.out.println("Initial sidebar width: " + sidebarWidth + "px");
        System.out.println("Initial tree panel width: " + treePanelWidth + "px");
        System.out.println("Initial content panel width: " + contentPanelWidth + "px");
        
        assertTrue(sidebarWidth > 0, "Sidebar should have width > 0");
        assertTrue(treePanelWidth > 0, "Tree panel should have width > 0");
        assertTrue(contentPanelWidth > 0, "Content panel should have width > 0");
    }

    @Test
    @Order(2)
    @DisplayName("Test sidebar collapse - tree panel expands, content panel width stays same")
    void testSidebarCollapse() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement sidebar = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sidebar")));
        WebElement treePanel = driver.findElement(By.cssSelector(".tree-panel"));
        WebElement contentPanel = driver.findElement(By.id("content-panel"));
        WebElement sidebarCloseBtn = driver.findElement(By.id("sidebar-close-btn"));

        // Get initial widths
        int initialSidebarWidth = sidebar.getSize().getWidth();
        int initialTreePanelWidth = treePanel.getSize().getWidth();
        int initialContentPanelWidth = contentPanel.getSize().getWidth();

        System.out.println("\n=== BEFORE COLLAPSE ===");
        System.out.println("Sidebar width: " + initialSidebarWidth + "px");
        System.out.println("Tree panel width: " + initialTreePanelWidth + "px");
        System.out.println("Content panel width: " + initialContentPanelWidth + "px");

        // Click the close button
        sidebarCloseBtn.click();

        // Wait for animation to complete
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get widths after collapse
        int afterTreePanelWidth = treePanel.getSize().getWidth();
        int afterContentPanelWidth = contentPanel.getSize().getWidth();

        System.out.println("\n=== AFTER COLLAPSE ===");
        System.out.println("Tree panel width: " + afterTreePanelWidth + "px");
        System.out.println("Content panel width: " + afterContentPanelWidth + "px");

        // CRITICAL ASSERTIONS
        assertTrue(afterTreePanelWidth > initialTreePanelWidth,
            "Tree panel width should INCREASE after sidebar collapse (tree expands)");

        assertEquals(initialContentPanelWidth, afterContentPanelWidth, 50,
            "Content panel width should stay approximately the SAME (should NOT expand)");

        // Verify sidebar has collapsed class
        String sidebarClass = sidebar.getAttribute("class");
        assertTrue(sidebarClass.contains("collapsed"),
            "Sidebar should have 'collapsed' class");
    }

    @Test
    @Order(3)
    @DisplayName("Test sidebar expand - tree panel shrinks back, content panel width stays same")
    void testSidebarExpand() {
        driver.get(baseUrl + "/d3-tree-viewer.html");
        
        WebElement sidebar = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sidebar")));
        WebElement treePanel = driver.findElement(By.cssSelector(".tree-panel"));
        WebElement contentPanel = driver.findElement(By.id("content-panel"));
        WebElement sidebarCloseBtn = driver.findElement(By.id("sidebar-close-btn"));
        
        // Get initial widths
        int initialTreePanelWidth = treePanel.getSize().getWidth();
        int initialContentPanelWidth = contentPanel.getSize().getWidth();
        
        // Collapse sidebar
        sidebarCloseBtn.click();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Get widths after collapse
        int collapsedTreePanelWidth = treePanel.getSize().getWidth();
        int collapsedContentPanelWidth = contentPanel.getSize().getWidth();
        
        System.out.println("\n=== AFTER COLLAPSE ===");
        System.out.println("Tree panel width: " + collapsedTreePanelWidth + "px");
        System.out.println("Content panel width: " + collapsedContentPanelWidth + "px");
        
        // Now expand sidebar again
        WebElement sidebarToggleBtn = driver.findElement(By.id("sidebar-toggle-btn"));
        sidebarToggleBtn.click();
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Get widths after expand
        int expandedTreePanelWidth = treePanel.getSize().getWidth();
        int expandedContentPanelWidth = contentPanel.getSize().getWidth();
        
        System.out.println("\n=== AFTER EXPAND ===");
        System.out.println("Tree panel width: " + expandedTreePanelWidth + "px");
        System.out.println("Content panel width: " + expandedContentPanelWidth + "px");
        
        // CRITICAL ASSERTIONS
        assertTrue(expandedTreePanelWidth < collapsedTreePanelWidth, 
            "Tree panel width should DECREASE after sidebar expands (tree shrinks back)");
        
        assertEquals(initialTreePanelWidth, expandedTreePanelWidth, 50,
            "Tree panel should return to approximately initial width");
        
        assertEquals(initialContentPanelWidth, expandedContentPanelWidth, 50,
            "Content panel width should stay approximately the SAME (should NOT change)");
        
        // Verify sidebar does NOT have collapsed class
        String sidebarClass = sidebar.getAttribute("class");
        assertFalse(sidebarClass.contains("collapsed"), 
            "Sidebar should NOT have 'collapsed' class after expand");
    }

    @Test
    @Order(4)
    @DisplayName("Test tree panel expands to fill sidebar space when sidebar collapses")
    void testTreePanelExpandsToFillSpace() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement sidebar = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sidebar")));
        WebElement treePanel = driver.findElement(By.cssSelector(".tree-panel"));
        WebElement contentPanel = driver.findElement(By.id("content-panel"));
        WebElement sidebarCloseBtn = driver.findElement(By.id("sidebar-close-btn"));

        // Get initial widths and X positions
        int initialSidebarWidth = sidebar.getSize().getWidth();
        int initialTreePanelWidth = treePanel.getSize().getWidth();
        int initialTreePanelX = treePanel.getLocation().getX();
        int initialContentPanelWidth = contentPanel.getSize().getWidth();
        int initialContentPanelX = contentPanel.getLocation().getX();

        System.out.println("\n=== BEFORE COLLAPSE ===");
        System.out.println("Sidebar width: " + initialSidebarWidth + "px");
        System.out.println("Tree panel width: " + initialTreePanelWidth + "px, X: " + initialTreePanelX + "px");
        System.out.println("Content panel width: " + initialContentPanelWidth + "px, X: " + initialContentPanelX + "px");

        // Collapse sidebar
        sidebarCloseBtn.click();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get widths and positions after collapse
        int afterTreePanelWidth = treePanel.getSize().getWidth();
        int afterTreePanelX = treePanel.getLocation().getX();
        int afterContentPanelWidth = contentPanel.getSize().getWidth();
        int afterContentPanelX = contentPanel.getLocation().getX();

        System.out.println("\n=== AFTER COLLAPSE ===");
        System.out.println("Tree panel width: " + afterTreePanelWidth + "px, X: " + afterTreePanelX + "px");
        System.out.println("Content panel width: " + afterContentPanelWidth + "px, X: " + afterContentPanelX + "px");

        // Calculate changes
        int treePanelWidthIncrease = afterTreePanelWidth - initialTreePanelWidth;
        int treePanelXShift = afterTreePanelX - initialTreePanelX;
        int contentPanelXShift = afterContentPanelX - initialContentPanelX;

        System.out.println("\n=== ANALYSIS ===");
        System.out.println("Tree panel width increase: " + treePanelWidthIncrease + "px");
        System.out.println("Tree panel X shift: " + treePanelXShift + "px (negative = left)");
        System.out.println("Content panel X shift: " + contentPanelXShift + "px (negative = left)");

        // CRITICAL ASSERTIONS
        // Tree panel should expand by approximately the sidebar width
        assertEquals(initialSidebarWidth, treePanelWidthIncrease, 50,
            "Tree panel should expand by approximately the sidebar width (280px)");

        // Tree panel should shift LEFT by approximately the sidebar width
        assertEquals(-initialSidebarWidth, treePanelXShift, 50,
            "Tree panel should shift LEFT by approximately the sidebar width");

        // Content panel width should stay the same
        assertEquals(initialContentPanelWidth, afterContentPanelWidth, 10,
            "Content panel width should stay the same");

        // Content panel should NOT shift (stay in same position)
        assertEquals(0, contentPanelXShift, 10,
            "Content panel should NOT shift (stay in same X position)");
    }
}

