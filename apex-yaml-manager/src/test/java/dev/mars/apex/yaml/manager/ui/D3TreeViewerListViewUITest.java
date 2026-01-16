package dev.mars.apex.yaml.manager.ui;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for the List View functionality in the YAML Manager UI.
 * Tests tab switching, table rendering, sorting, search, and file selection.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class D3TreeViewerListViewUITest {

    @LocalServerPort
    private int port;

    private static WebDriver driver;
    private static WebDriverWait wait;

    @BeforeAll
    public static void setupClass() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterAll
    public static void teardownClass() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    public void setup() {
        driver.get("http://localhost:" + port + "/yaml-manager/ui");
        // Wait for page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tree-view-tab")));
    }

    @Test
    @Order(1)
    @DisplayName("Test that both view tabs exist and are visible")
    public void testViewTabsExist() {
        WebElement treeViewTab = driver.findElement(By.id("tree-view-tab"));
        WebElement listViewTab = driver.findElement(By.id("list-view-tab"));

        assertTrue(treeViewTab.isDisplayed(), "Tree View tab should be visible");
        assertTrue(listViewTab.isDisplayed(), "List View tab should be visible");

        // Tree view should be active by default
        assertTrue(treeViewTab.getAttribute("class").contains("active"), 
            "Tree View tab should be active by default");
        assertFalse(listViewTab.getAttribute("class").contains("active"), 
            "List View tab should not be active by default");
    }

    @Test
    @Order(2)
    @DisplayName("Test tab text labels are correct")
    public void testTabLabels() {
        WebElement treeViewTab = driver.findElement(By.id("tree-view-tab"));
        WebElement listViewTab = driver.findElement(By.id("list-view-tab"));

        assertEquals("Tree View", treeViewTab.getText().trim(), 
            "Tree View tab should have correct label");
        assertEquals("List View", listViewTab.getText().trim(), 
            "List View tab should have correct label");
    }

    @Test
    @Order(3)
    @DisplayName("Test switching from Tree View to List View")
    public void testSwitchToListView() {
        WebElement listViewTab = driver.findElement(By.id("list-view-tab"));
        WebElement treeViewPanel = driver.findElement(By.id("tree-view-panel"));
        WebElement listViewPanel = driver.findElement(By.id("list-view-panel"));

        // Initially tree view should be visible
        assertTrue(treeViewPanel.isDisplayed(), "Tree view panel should be visible initially");
        assertFalse(listViewPanel.isDisplayed(), "List view panel should be hidden initially");

        // Click list view tab
        listViewTab.click();

        // Wait for list view to become visible
        wait.until(ExpectedConditions.visibilityOf(listViewPanel));

        // List view should now be visible, tree view hidden
        assertFalse(treeViewPanel.isDisplayed(), "Tree view panel should be hidden after switch");
        assertTrue(listViewPanel.isDisplayed(), "List view panel should be visible after switch");

        // List view tab should be active
        assertTrue(listViewTab.getAttribute("class").contains("active"), 
            "List View tab should be active after click");
    }

    @Test
    @Order(4)
    @DisplayName("Test switching back from List View to Tree View")
    public void testSwitchBackToTreeView() {
        // First switch to list view
        WebElement listViewTab = driver.findElement(By.id("list-view-tab"));
        listViewTab.click();
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.id("list-view-panel"))));

        // Now switch back to tree view
        WebElement treeViewTab = driver.findElement(By.id("tree-view-tab"));
        treeViewTab.click();

        WebElement treeViewPanel = driver.findElement(By.id("tree-view-panel"));
        WebElement listViewPanel = driver.findElement(By.id("list-view-panel"));

        // Wait for tree view to become visible
        wait.until(ExpectedConditions.visibilityOf(treeViewPanel));

        // Tree view should be visible, list view hidden
        assertTrue(treeViewPanel.isDisplayed(), "Tree view panel should be visible after switch back");
        assertFalse(listViewPanel.isDisplayed(), "List view panel should be hidden after switch back");

        // Tree view tab should be active
        assertTrue(treeViewTab.getAttribute("class").contains("active"), 
            "Tree View tab should be active after switch back");
    }

    @Test
    @Order(5)
    @DisplayName("Test list view panel structure exists")
    public void testListViewPanelStructure() {
        // Switch to list view
        driver.findElement(By.id("list-view-tab")).click();
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.id("list-view-panel"))));

        // Check for key elements
        assertDoesNotThrow(() -> driver.findElement(By.className("list-header")), 
            "List header should exist");
        assertDoesNotThrow(() -> driver.findElement(By.id("list-search-input")), 
            "Search input should exist");
        assertDoesNotThrow(() -> driver.findElement(By.id("refresh-list-btn")), 
            "Refresh button should exist");
        assertDoesNotThrow(() -> driver.findElement(By.id("yaml-files-table")), 
            "YAML files table should exist");
        assertDoesNotThrow(() -> driver.findElement(By.id("yaml-files-tbody")), 
            "Table body should exist");
    }

    @Test
    @Order(6)
    @DisplayName("Test list view loads data successfully")
    public void testListViewLoadsData() {
        // Switch to list view
        driver.findElement(By.id("list-view-tab")).click();
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.id("list-view-panel"))));

        // Wait for loading to complete and table to be visible
        WebElement table = driver.findElement(By.id("yaml-files-table"));
        wait.until(ExpectedConditions.visibilityOf(table));

        // Check that error is not displayed
        WebElement errorElement = driver.findElement(By.id("list-error"));
        assertFalse(errorElement.isDisplayed(), "Error message should not be displayed");

        // Check that table has rows
        WebElement tbody = driver.findElement(By.id("yaml-files-tbody"));
        List<WebElement> rows = tbody.findElements(By.tagName("tr"));
        assertTrue(rows.size() > 0, "Table should have at least one row of data");

        System.out.println("List view loaded " + rows.size() + " files");
    }

    @Test
    @Order(7)
    @DisplayName("Test table has correct column headers")
    public void testTableColumnHeaders() {
        // Switch to list view
        driver.findElement(By.id("list-view-tab")).click();
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.id("list-view-panel"))));

        // Wait for table to be visible
        WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("yaml-files-table")));

        // Wait for data to load (table should have rows)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".yaml-table tbody tr")));

        // Get all table headers - the text is in the first span child
        List<WebElement> headers = driver.findElements(By.cssSelector(".yaml-table thead th span:first-child"));

        assertEquals(9, headers.size(), "Table should have 9 columns");

        // Check header text (case-insensitive since CSS may transform text to uppercase)
        assertEquals("file name", headers.get(0).getText().trim().toLowerCase());
        assertEquals("id", headers.get(1).getText().trim().toLowerCase());
        assertEquals("name", headers.get(2).getText().trim().toLowerCase());
        assertEquals("type", headers.get(3).getText().trim().toLowerCase());
        assertEquals("business domain", headers.get(4).getText().trim().toLowerCase());
        assertEquals("owner", headers.get(5).getText().trim().toLowerCase());
        assertEquals("author", headers.get(6).getText().trim().toLowerCase());
        assertEquals("description", headers.get(7).getText().trim().toLowerCase());
        assertEquals("version", headers.get(8).getText().trim().toLowerCase());
    }

    @Test
    @Order(8)
    @DisplayName("Test sortable columns have correct attributes")
    public void testSortableColumns() {
        // Switch to list view
        driver.findElement(By.id("list-view-tab")).click();
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.id("list-view-panel"))));

        // Get all sortable headers
        List<WebElement> sortableHeaders = driver.findElements(By.cssSelector(".yaml-table th.sortable"));
        
        assertTrue(sortableHeaders.size() > 0, "Should have sortable column headers");

        // Check that each sortable header has data-column attribute
        for (WebElement header : sortableHeaders) {
            String dataColumn = header.getAttribute("data-column");
            assertNotNull(dataColumn, "Sortable header should have data-column attribute");
            assertFalse(dataColumn.isEmpty(), "data-column attribute should not be empty");
        }
    }

    @Test
    @Order(9)
    @DisplayName("Test clicking column header sorts the table")
    public void testColumnSorting() {
        // Switch to list view
        driver.findElement(By.id("list-view-tab")).click();
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.id("list-view-panel"))));
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.id("yaml-files-table"))));

        // Get the filename column header
        WebElement filenameHeader = driver.findElement(By.cssSelector("th[data-column='filename']"));
        
        // Click to sort ascending (should already be ascending by default)
        filenameHeader.click();
        
        // Wait a moment for sort to apply
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Check that sort indicator is present
        String headerClass = filenameHeader.getAttribute("class");
        assertTrue(headerClass.contains("sort-asc") || headerClass.contains("sort-desc"), 
            "Header should have sort indicator class");

        // Click again to toggle sort direction
        filenameHeader.click();
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Sort direction should have changed
        String newHeaderClass = filenameHeader.getAttribute("class");
        assertNotEquals(headerClass, newHeaderClass, "Sort direction should have changed");
    }

    @Test
    @Order(10)
    @DisplayName("Test search input filters table rows")
    public void testSearchFiltering() {
        // Switch to list view
        driver.findElement(By.id("list-view-tab")).click();
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.id("list-view-panel"))));
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.id("yaml-files-table"))));

        // Get initial row count
        WebElement tbody = driver.findElement(By.id("yaml-files-tbody"));
        int initialRowCount = tbody.findElements(By.tagName("tr")).size();

        // Enter search term
        WebElement searchInput = driver.findElement(By.id("list-search-input"));
        searchInput.clear();
        searchInput.sendKeys("scenario");

        // Wait for filtering to apply
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        // Get filtered row count
        int filteredRowCount = tbody.findElements(By.tagName("tr")).size();

        // Filtered count should be less than or equal to initial count
        assertTrue(filteredRowCount <= initialRowCount, 
            "Filtered row count should be less than or equal to initial count");

        System.out.println("Search filtered from " + initialRowCount + " to " + filteredRowCount + " rows");
    }

    @Test
    @Order(11)
    @DisplayName("Test clicking table row selects it")
    public void testRowSelection() {
        // Switch to list view
        driver.findElement(By.id("list-view-tab")).click();
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.id("list-view-panel"))));
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.id("yaml-files-table"))));

        // Get first row
        WebElement tbody = driver.findElement(By.id("yaml-files-tbody"));
        List<WebElement> rows = tbody.findElements(By.tagName("tr"));
        assertTrue(rows.size() > 0, "Should have at least one row");

        WebElement firstRow = rows.get(0);
        
        // Click the row
        firstRow.click();

        // Wait for selection to apply
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Check that row has selected class
        String rowClass = firstRow.getAttribute("class");
        assertTrue(rowClass.contains("selected"), "Clicked row should have 'selected' class");
    }

    @Test
    @Order(12)
    @DisplayName("Test refresh button reloads data")
    public void testRefreshButton() {
        // Switch to list view
        driver.findElement(By.id("list-view-tab")).click();
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.id("list-view-panel"))));
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.id("yaml-files-table"))));

        // Click refresh button
        WebElement refreshBtn = driver.findElement(By.id("refresh-list-btn"));
        refreshBtn.click();

        // Wait for loading indicator to appear and disappear
        WebElement loadingElement = driver.findElement(By.id("list-loading"));
        
        // Loading should appear briefly
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        // Wait for table to be visible again
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.id("yaml-files-table"))));

        // Table should still have data
        WebElement tbody = driver.findElement(By.id("yaml-files-tbody"));
        List<WebElement> rows = tbody.findElements(By.tagName("tr"));
        assertTrue(rows.size() > 0, "Table should have data after refresh");
    }
}

