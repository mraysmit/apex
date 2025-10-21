package dev.mars.apex.yaml.manager.ui;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI tests for toolbar controls (expand/collapse/levels) and search behavior.
 * Uses JS-triggered tree load for stability, then interacts only via UI controls.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ToolbarAndSearchUITest {

    private WebDriver driver;
    private WebDriverWait wait;
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
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        baseUrl = "http://localhost:" + port + "/yaml-manager";
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }

    private String rootFile() {
        return Paths.get(System.getProperty("user.dir"),
                "src", "test", "resources", "apex-yaml-samples", "scenario-registry.yaml")
            .toAbsolutePath().toString();
    }

    private void loadTreeViaJs() {
        driver.get(baseUrl + "/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        String rf = rootFile();
        ((JavascriptExecutor) driver).executeScript("loadDependencyTree(arguments[0]);", rf);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
    }

    private int visibleNodeCount() {
        List<WebElement> nodes = driver.findElements(By.className("tree-node"));
        // Non-D3 renderer only creates visible nodes, so count elements directly
        return nodes.size();
    }

    @Test
    @Order(1)
    void collapse_all_then_expand_levels_increases_visible_nodes() {
        loadTreeViaJs();

        // Collapse to root only
        WebElement collapseAllBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("collapseAllBtn")));
        collapseAllBtn.click();
        int level1 = visibleNodeCount();
        assertTrue(level1 >= 1, "At least the root node should be visible");

        // Expand to level 2
        WebElement level2Btn = wait.until(ExpectedConditions.elementToBeClickable(By.id("expandLevel2Btn")));
        level2Btn.click();
        int level2 = visibleNodeCount();
        assertTrue(level2 > level1, "Expanding to level 2 should increase visible nodes");

        // Expand all
        WebElement expandAllBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("expandAllBtn")));
        expandAllBtn.click();
        int all = visibleNodeCount();
        assertTrue(all >= level2, "Expand all should not reduce visible nodes");
    }

    @Test
    @Order(2)
    void search_filters_to_matches_and_expands_ancestors() {
        loadTreeViaJs();

        WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("searchInput")));
        // Use a term that should exist in names/paths
        searchInput.clear();
        searchInput.sendKeys("scenario");

        // After search, nodes not matching are hidden; matches get 'search-match'
        // Give it a moment to re-render
        try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

        List<WebElement> allNodes = driver.findElements(By.className("tree-node"));
        long matchCount = driver.findElements(By.cssSelector(".tree-node.search-match")).size();
        // Count nodes that are currently hidden by style display:none
        long hiddenCount = allNodes.stream().filter(el -> {
            try {
                return !el.isDisplayed();
            } catch (StaleElementReferenceException e) {
                return false;
            }
        }).count();

        assertTrue(matchCount > 0, "There should be at least one match highlighted");
        assertTrue(hiddenCount > 0, "Some non-matching nodes should be hidden after search");

        // Clear search should restore nodes
        searchInput.clear();
        try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        int restored = visibleNodeCount();
        assertTrue(restored >= allNodes.size(), "Clearing search should restore visibility");
    }

    @Test
    @Order(3)
    void test_expand_level1_button() {
        loadTreeViaJs();

        // Collapse all first
        WebElement collapseAllBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("collapseAllBtn")));
        collapseAllBtn.click();
        int collapsedCount = visibleNodeCount();

        // Click Level 1 button
        WebElement level1Btn = wait.until(ExpectedConditions.elementToBeClickable(By.id("expandLevel1Btn")));
        level1Btn.click();

        // Wait for expansion
        wait.until(driver -> visibleNodeCount() > collapsedCount);
        int level1Count = visibleNodeCount();

        assertTrue(level1Count > collapsedCount, "Level 1 expansion should show more nodes");
    }

    @Test
    @Order(4)
    void test_expand_level3_button() {
        loadTreeViaJs();

        // Start with level 2
        WebElement level2Btn = wait.until(ExpectedConditions.elementToBeClickable(By.id("expandLevel2Btn")));
        level2Btn.click();
        wait.until(driver -> visibleNodeCount() > 1);
        int level2Count = visibleNodeCount();

        // Click Level 3 button
        WebElement level3Btn = wait.until(ExpectedConditions.elementToBeClickable(By.id("expandLevel3Btn")));
        level3Btn.click();

        // Wait for expansion
        wait.until(driver -> visibleNodeCount() >= level2Count);
        int level3Count = visibleNodeCount();

        assertTrue(level3Count >= level2Count, "Level 3 expansion should show same or more nodes");
    }

    @Test
    @Order(5)
    void test_refresh_button() {
        loadTreeViaJs();

        int initialCount = visibleNodeCount();
        assertTrue(initialCount > 0, "Should have nodes before refresh");

        // Click refresh button
        WebElement refreshBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("refreshBtn")));
        refreshBtn.click();

        // Wait for refresh to complete
        wait.until(driver -> {
            List<WebElement> nodes = driver.findElements(By.className("tree-node"));
            return nodes.size() > 0;
        });

        int refreshedCount = visibleNodeCount();
        assertTrue(refreshedCount > 0, "Should have nodes after refresh");
    }

    @Test
    @Order(6)
    void test_filter_button() {
        loadTreeViaJs();

        // Click filter button
        WebElement filterBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("filterBtn")));
        filterBtn.click();

        // Filter button should be clickable (basic functionality test)
        assertTrue(filterBtn.isEnabled(), "Filter button should be enabled");

        // Note: Actual filter functionality would depend on implementation
        // This test verifies the button is present and clickable
    }

    @Test
    @Order(7)
    void test_d3_toggle_checkbox() {
        loadTreeViaJs();

        // Find D3 toggle checkbox
        WebElement d3Toggle = wait.until(ExpectedConditions.elementToBeClickable(By.id("useD3Toggle")));

        // Check initial state
        boolean initialState = d3Toggle.isSelected();

        // Click to toggle
        d3Toggle.click();

        // Verify state changed
        boolean newState = d3Toggle.isSelected();
        assertNotEquals(initialState, newState, "D3 toggle should change state when clicked");

        // Toggle back
        d3Toggle.click();
        boolean finalState = d3Toggle.isSelected();
        assertEquals(initialState, finalState, "D3 toggle should return to original state");
    }
}

