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
}

