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
 * Selenium UI test for the D3 Tree Viewer resizer functionality.
 * This test verifies that the vertical splitter between tree panel and content panel works correctly.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class D3TreeViewerResizerUITest {

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
    @DisplayName("Test that resizer element exists and is visible")
    void testResizerExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");
        
        WebElement resizer = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("resizer")));
        assertTrue(resizer.isDisplayed(), "Resizer should be visible");
        
        String cursor = resizer.getCssValue("cursor");
        assertEquals("col-resize", cursor, "Resizer should have col-resize cursor");
    }

    @Test
    @Order(2)
    @DisplayName("Test that panels exist with initial widths")
    void testInitialPanelWidths() {
        driver.get(baseUrl + "/d3-tree-viewer.html");
        
        WebElement treePanel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".tree-panel")));
        WebElement contentPanel = driver.findElement(By.cssSelector(".content-panel"));
        
        int treePanelWidth = treePanel.getSize().getWidth();
        int contentPanelWidth = contentPanel.getSize().getWidth();
        
        assertTrue(treePanelWidth > 0, "Tree panel should have width > 0");
        assertTrue(contentPanelWidth > 0, "Content panel should have width > 0");
        
        System.out.println("Initial tree panel width: " + treePanelWidth + "px");
        System.out.println("Initial content panel width: " + contentPanelWidth + "px");
    }

    @Test
    @Order(3)
    @DisplayName("Test resizer drag to the right increases tree panel width")
    void testResizerDragRight() {
        driver.get(baseUrl + "/d3-tree-viewer.html");
        
        WebElement treePanel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".tree-panel")));
        WebElement contentPanel = driver.findElement(By.cssSelector(".content-panel"));
        WebElement resizer = driver.findElement(By.id("resizer"));
        
        int initialTreeWidth = treePanel.getSize().getWidth();
        int initialContentWidth = contentPanel.getSize().getWidth();
        
        System.out.println("Before drag - Tree: " + initialTreeWidth + "px, Content: " + initialContentWidth + "px");
        
        // Drag resizer 100px to the right
        Actions actions = new Actions(driver);
        actions.clickAndHold(resizer)
               .moveByOffset(100, 0)
               .release()
               .perform();
        
        // Wait a bit for the resize to complete
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        int newTreeWidth = treePanel.getSize().getWidth();
        int newContentWidth = contentPanel.getSize().getWidth();
        
        System.out.println("After drag - Tree: " + newTreeWidth + "px, Content: " + newContentWidth + "px");
        
        assertTrue(newTreeWidth > initialTreeWidth, 
            "Tree panel width should increase after dragging right. Initial: " + initialTreeWidth + ", New: " + newTreeWidth);
        assertTrue(newContentWidth < initialContentWidth, 
            "Content panel width should decrease after dragging right. Initial: " + initialContentWidth + ", New: " + newContentWidth);
    }

    @Test
    @Order(4)
    @DisplayName("Test resizer drag to the left decreases tree panel width")
    void testResizerDragLeft() {
        driver.get(baseUrl + "/d3-tree-viewer.html");
        
        WebElement treePanel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".tree-panel")));
        WebElement contentPanel = driver.findElement(By.cssSelector(".content-panel"));
        WebElement resizer = driver.findElement(By.id("resizer"));
        
        int initialTreeWidth = treePanel.getSize().getWidth();
        int initialContentWidth = contentPanel.getSize().getWidth();
        
        System.out.println("Before drag - Tree: " + initialTreeWidth + "px, Content: " + initialContentWidth + "px");
        
        // Drag resizer 100px to the left
        Actions actions = new Actions(driver);
        actions.clickAndHold(resizer)
               .moveByOffset(-100, 0)
               .release()
               .perform();
        
        // Wait a bit for the resize to complete
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        int newTreeWidth = treePanel.getSize().getWidth();
        int newContentWidth = contentPanel.getSize().getWidth();
        
        System.out.println("After drag - Tree: " + newTreeWidth + "px, Content: " + newContentWidth + "px");
        
        assertTrue(newTreeWidth < initialTreeWidth, 
            "Tree panel width should decrease after dragging left. Initial: " + initialTreeWidth + ", New: " + newTreeWidth);
        assertTrue(newContentWidth > initialContentWidth, 
            "Content panel width should increase after dragging left. Initial: " + initialContentWidth + ", New: " + newContentWidth);
    }

    @Test
    @Order(5)
    @DisplayName("Test that clicking resizer without moving does not change panel widths")
    void testResizerClickWithoutDrag() {
        driver.get(baseUrl + "/d3-tree-viewer.html");
        
        WebElement treePanel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".tree-panel")));
        WebElement contentPanel = driver.findElement(By.cssSelector(".content-panel"));
        WebElement resizer = driver.findElement(By.id("resizer"));
        
        int initialTreeWidth = treePanel.getSize().getWidth();
        int initialContentWidth = contentPanel.getSize().getWidth();
        
        System.out.println("Before click - Tree: " + initialTreeWidth + "px, Content: " + initialContentWidth + "px");
        
        // Click without dragging
        Actions actions = new Actions(driver);
        actions.click(resizer).perform();
        
        // Wait a bit
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        int newTreeWidth = treePanel.getSize().getWidth();
        int newContentWidth = contentPanel.getSize().getWidth();
        
        System.out.println("After click - Tree: " + newTreeWidth + "px, Content: " + newContentWidth + "px");
        
        // Allow for small rounding differences (within 5px)
        int treeDiff = Math.abs(newTreeWidth - initialTreeWidth);
        int contentDiff = Math.abs(newContentWidth - initialContentWidth);
        
        assertTrue(treeDiff <= 5, 
            "Tree panel width should not change significantly on click without drag. Difference: " + treeDiff + "px");
        assertTrue(contentDiff <= 5, 
            "Content panel width should not change significantly on click without drag. Difference: " + contentDiff + "px");
    }

    @Test
    @Order(6)
    @DisplayName("Test resizer respects minimum width constraints")
    void testResizerMinimumWidths() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement treePanel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".tree-panel")));
        WebElement contentPanel = driver.findElement(By.cssSelector(".content-panel"));
        WebElement resizer = driver.findElement(By.id("resizer"));

        // Try to drag far to the left (should hit minimum tree width)
        Actions actions = new Actions(driver);
        actions.clickAndHold(resizer)
               .moveByOffset(-800, 0)
               .release()
               .perform();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int treeWidth = treePanel.getSize().getWidth();
        System.out.println("Tree panel width after dragging far left: " + treeWidth + "px");

        assertTrue(treeWidth >= 200,
            "Tree panel should respect minimum width of 200px. Actual: " + treeWidth + "px");
    }

    @Test
    @Order(7)
    @DisplayName("Test hiding content panel expands tree panel to full width")
    void testHideContentPanel() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement treePanel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".tree-panel")));
        WebElement contentPanel = driver.findElement(By.cssSelector(".content-panel"));
        WebElement contentCloseBtn = driver.findElement(By.id("content-close-btn"));
        WebElement resizer = driver.findElement(By.id("resizer"));

        int initialTreeWidth = treePanel.getSize().getWidth();
        int containerWidth = driver.findElement(By.cssSelector(".main-container")).getSize().getWidth();

        System.out.println("Before hiding - Tree: " + initialTreeWidth + "px, Container: " + containerWidth + "px");

        // Click the close button to hide content panel
        contentCloseBtn.click();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Check that content panel is hidden
        assertFalse(contentPanel.isDisplayed(), "Content panel should be hidden");

        // Check that resizer is hidden
        assertFalse(resizer.isDisplayed(), "Resizer should be hidden when content panel is closed");

        // Check that tree panel expanded
        int newTreeWidth = treePanel.getSize().getWidth();
        System.out.println("After hiding - Tree: " + newTreeWidth + "px");

        assertTrue(newTreeWidth > initialTreeWidth,
            "Tree panel should expand when content panel is hidden. Initial: " + initialTreeWidth + ", New: " + newTreeWidth);

        // Check that tree panel fills most of the container (accounting for sidebar)
        assertTrue(newTreeWidth > containerWidth * 0.8,
            "Tree panel should fill most of container width. Tree: " + newTreeWidth + ", Container: " + containerWidth);
    }

    @Test
    @Order(8)
    @DisplayName("Test showing content panel restores previous layout")
    void testShowContentPanel() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement treePanel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".tree-panel")));
        WebElement contentPanel = driver.findElement(By.cssSelector(".content-panel"));
        WebElement contentCloseBtn = driver.findElement(By.id("content-close-btn"));
        WebElement contentToggleBtn = driver.findElement(By.id("content-toggle-btn"));
        WebElement resizer = driver.findElement(By.id("resizer"));

        int initialTreeWidth = treePanel.getSize().getWidth();

        // Hide the content panel
        contentCloseBtn.click();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Show it again
        contentToggleBtn.click();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Check that content panel is visible
        assertTrue(contentPanel.isDisplayed(), "Content panel should be visible");

        // Check that resizer is visible
        assertTrue(resizer.isDisplayed(), "Resizer should be visible when content panel is open");

        // Check that tree panel width is restored (allow small difference)
        int restoredTreeWidth = treePanel.getSize().getWidth();
        int diff = Math.abs(restoredTreeWidth - initialTreeWidth);

        System.out.println("Initial: " + initialTreeWidth + "px, Restored: " + restoredTreeWidth + "px, Diff: " + diff + "px");

        assertTrue(diff < 50,
            "Tree panel width should be approximately restored. Initial: " + initialTreeWidth + ", Restored: " + restoredTreeWidth);
    }

    @Test
    @Order(9)
    @DisplayName("Test tree header expands to fill tree panel width when content panel is hidden")
    void testTreeHeaderExpandsWhenContentPanelHidden() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement treePanel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".tree-panel")));
        WebElement treeHeader = driver.findElement(By.cssSelector(".tree-header"));
        WebElement contentCloseBtn = driver.findElement(By.id("content-close-btn"));

        int initialTreePanelWidth = treePanel.getSize().getWidth();
        int initialTreeHeaderWidth = treeHeader.getSize().getWidth();

        System.out.println("Before hiding - Tree Panel: " + initialTreePanelWidth + "px, Tree Header: " + initialTreeHeaderWidth + "px");

        // Verify header matches panel width initially
        int initialDiff = Math.abs(initialTreePanelWidth - initialTreeHeaderWidth);
        assertTrue(initialDiff <= 5,
            "Tree header should match tree panel width initially. Panel: " + initialTreePanelWidth + ", Header: " + initialTreeHeaderWidth + ", Diff: " + initialDiff);

        // Hide the content panel
        contentCloseBtn.click();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int newTreePanelWidth = treePanel.getSize().getWidth();
        int newTreeHeaderWidth = treeHeader.getSize().getWidth();

        System.out.println("After hiding - Tree Panel: " + newTreePanelWidth + "px, Tree Header: " + newTreeHeaderWidth + "px");

        // Verify both expanded
        assertTrue(newTreePanelWidth > initialTreePanelWidth,
            "Tree panel should expand. Initial: " + initialTreePanelWidth + ", New: " + newTreePanelWidth);
        assertTrue(newTreeHeaderWidth > initialTreeHeaderWidth,
            "Tree header should expand. Initial: " + initialTreeHeaderWidth + ", New: " + newTreeHeaderWidth);

        // Verify header still matches panel width after expansion
        int newDiff = Math.abs(newTreePanelWidth - newTreeHeaderWidth);
        assertTrue(newDiff <= 5,
            "Tree header should match tree panel width after expansion. Panel: " + newTreePanelWidth + ", Header: " + newTreeHeaderWidth + ", Diff: " + newDiff);
    }

    @Test
    @Order(10)
    @DisplayName("Test content panel body fills to bottom of content panel")
    void testContentPanelBodyFillsToBottom() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement contentPanel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".content-panel")));
        WebElement contentBody = driver.findElement(By.cssSelector(".content-body"));
        WebElement contentHeader = driver.findElement(By.cssSelector(".content-header"));

        int contentPanelHeight = contentPanel.getSize().getHeight();
        int contentBodyHeight = contentBody.getSize().getHeight();
        int contentHeaderHeight = contentHeader.getSize().getHeight();

        System.out.println("Content Panel Height: " + contentPanelHeight + "px");
        System.out.println("Content Header Height: " + contentHeaderHeight + "px");
        System.out.println("Content Body Height: " + contentBodyHeight + "px");
        System.out.println("Expected Body Height: " + (contentPanelHeight - contentHeaderHeight) + "px");

        // The content body should fill the remaining space after the header
        int expectedBodyHeight = contentPanelHeight - contentHeaderHeight;
        int heightDiff = Math.abs(contentBodyHeight - expectedBodyHeight);

        // Allow for small rounding/padding differences (within 10px)
        assertTrue(heightDiff <= 10,
            "Content body should fill to bottom of content panel. " +
            "Panel: " + contentPanelHeight + "px, Header: " + contentHeaderHeight + "px, " +
            "Body: " + contentBodyHeight + "px, Expected: " + expectedBodyHeight + "px, Diff: " + heightDiff + "px");
    }

    @Test
    @Order(11)
    @DisplayName("Test sidebar toggle functionality")
    void testSidebarToggle() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement sidebar = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".sidebar")));
        WebElement sidebarCloseBtn = driver.findElement(By.id("sidebar-close-btn"));
        WebElement treePanel = driver.findElement(By.cssSelector(".tree-panel"));

        assertTrue(sidebar.isDisplayed(), "Sidebar should be visible initially");

        int initialTreeWidth = treePanel.getSize().getWidth();

        // Close sidebar
        sidebarCloseBtn.click();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Check sidebar has collapsed class
        String sidebarClass = sidebar.getDomAttribute("class");
        assertTrue(sidebarClass.contains("collapsed"), "Sidebar should have 'collapsed' class");

        // Tree panel should expand when sidebar collapses
        int newTreeWidth = treePanel.getSize().getWidth();
        System.out.println("Tree width - Before: " + initialTreeWidth + "px, After: " + newTreeWidth + "px");

        assertTrue(newTreeWidth > initialTreeWidth,
            "Tree panel should expand when sidebar collapses. Initial: " + initialTreeWidth + ", New: " + newTreeWidth);

        // Open sidebar again using the toggle button
        WebElement sidebarToggleBtn = driver.findElement(By.id("sidebar-toggle-btn"));
        sidebarToggleBtn.click();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        sidebarClass = sidebar.getDomAttribute("class");
        assertFalse(sidebarClass.contains("collapsed"), "Sidebar should not have 'collapsed' class when open");
    }

    @Test
    @Order(12)
    @DisplayName("Test tree container fills available space")
    void testTreeContainerFillsSpace() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement treePanel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".tree-panel")));
        WebElement treeHeader = driver.findElement(By.cssSelector(".tree-header"));
        WebElement treeContainer = driver.findElement(By.id("tree-container"));

        int treePanelHeight = treePanel.getSize().getHeight();
        int treeHeaderHeight = treeHeader.getSize().getHeight();
        int treeContainerHeight = treeContainer.getSize().getHeight();

        System.out.println("Tree Panel Height: " + treePanelHeight + "px");
        System.out.println("Tree Header Height: " + treeHeaderHeight + "px");
        System.out.println("Tree Container Height: " + treeContainerHeight + "px");

        // Tree container should fill remaining space after header
        int expectedContainerHeight = treePanelHeight - treeHeaderHeight;
        int heightDiff = Math.abs(treeContainerHeight - expectedContainerHeight);

        assertTrue(heightDiff <= 10,
            "Tree container should fill available space. " +
            "Panel: " + treePanelHeight + "px, Header: " + treeHeaderHeight + "px, " +
            "Container: " + treeContainerHeight + "px, Expected: " + expectedContainerHeight + "px, Diff: " + heightDiff + "px");
    }

    @Test
    @Order(13)
    @DisplayName("Test YAML content accordion fills to bottom when expanded")
    void testYamlContentAccordionFillsToBottom() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".content-panel")));

        // Load a YAML file by clicking the browse button and loading files
        // For now, we'll use JavaScript to simulate loading content
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
            "document.getElementById('yaml-section').style.display = 'block';" +
            "document.getElementById('metadata-section').style.display = 'block';" +
            "document.getElementById('placeholder-content').style.display = 'none';" +
            "document.getElementById('yaml-code').textContent = 'test: value\\nfoo: bar\\n'.repeat(50);"
        );

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        WebElement contentPanel = driver.findElement(By.cssSelector(".content-panel"));
        WebElement contentBody = driver.findElement(By.cssSelector(".content-body"));
        WebElement metadataSection = driver.findElement(By.id("metadata-section"));
        WebElement yamlSection = driver.findElement(By.id("yaml-section"));
        WebElement yamlAccordionContent = driver.findElement(By.id("content-accordion-yaml"));
        WebElement yamlContent = driver.findElement(By.id("yaml-content"));
        WebElement yamlPre = driver.findElement(By.cssSelector("#yaml-content pre"));

        // Get positions and sizes
        int contentPanelBottom = contentPanel.getLocation().getY() + contentPanel.getSize().getHeight();
        int contentBodyBottom = contentBody.getLocation().getY() + contentBody.getSize().getHeight();
        int yamlSectionBottom = yamlSection.getLocation().getY() + yamlSection.getSize().getHeight();
        int yamlAccordionContentBottom = yamlAccordionContent.getLocation().getY() + yamlAccordionContent.getSize().getHeight();
        int yamlContentBottom = yamlContent.getLocation().getY() + yamlContent.getSize().getHeight();
        int yamlPreBottom = yamlPre.getLocation().getY() + yamlPre.getSize().getHeight();

        System.out.println("=== POSITION ANALYSIS (Bottom positions) ===");
        System.out.println("Content Panel Bottom: " + contentPanelBottom + "px");
        System.out.println("Content Body Bottom: " + contentBodyBottom + "px");
        System.out.println("YAML Section Bottom: " + yamlSectionBottom + "px");
        System.out.println("YAML Accordion Content Bottom: " + yamlAccordionContentBottom + "px");
        System.out.println("YAML Content (.yaml-content) Bottom: " + yamlContentBottom + "px");
        System.out.println("YAML Pre Bottom: " + yamlPreBottom + "px");

        System.out.println("\n=== GAP ANALYSIS ===");
        System.out.println("Gap: Content Body -> YAML Section: " + (contentBodyBottom - yamlSectionBottom) + "px");
        System.out.println("Gap: Content Body -> YAML Accordion Content: " + (contentBodyBottom - yamlAccordionContentBottom) + "px");
        System.out.println("Gap: Content Body -> YAML Content div: " + (contentBodyBottom - yamlContentBottom) + "px");
        System.out.println("Gap: Content Body -> YAML Pre: " + (contentBodyBottom - yamlPreBottom) + "px");

        System.out.println("\n=== HEIGHT ANALYSIS ===");
        System.out.println("Content Body Height: " + contentBody.getSize().getHeight() + "px");
        System.out.println("Metadata Section Height: " + metadataSection.getSize().getHeight() + "px");
        System.out.println("YAML Section Height: " + yamlSection.getSize().getHeight() + "px");
        System.out.println("YAML Accordion Content Height: " + yamlAccordionContent.getSize().getHeight() + "px");
        System.out.println("YAML Content (.yaml-content) Height: " + yamlContent.getSize().getHeight() + "px");
        System.out.println("YAML Pre Height: " + yamlPre.getSize().getHeight() + "px");

        // Check if there's a visible gap at the bottom - measure from the actual YAML content div
        int gap = contentBodyBottom - yamlContentBottom;
        System.out.println("\n=== RESULT ===");
        System.out.println("Visual gap at bottom (Content Body -> YAML Content): " + gap + "px");

        // The YAML content should extend close to the bottom of content body
        // Allow for padding/borders (negative gap means content extends beyond, which is fine with overflow:auto)
        assertTrue(Math.abs(gap) <= 50,
            "YAML content should fill to bottom of content body. Gap: " + gap + "px");
    }
}

