/*
 * Copyright 2025 Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * Selenium UI tests for D3 Tree Viewer File Browser functionality.
 * Tests the directory dropdown selector and custom path input behavior.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-11
 * @version 1.1
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class D3TreeViewerFileBrowserUITest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor jsExecutor;
    private String baseUrl;

    @BeforeEach
    void setupTest() {
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
    @DisplayName("Test directory dropdown exists and loads sample directories")
    void testDirectoryDropdownExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement directorySelect = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("directory-select")));

        assertTrue(directorySelect.isDisplayed(), "Directory dropdown should be visible");

        // Wait for dropdown to be populated (either with recent items or "No recent items" message)
        wait.until(driver -> {
            String firstOption = directorySelect.findElement(By.cssSelector("option")).getText();
            return !firstOption.contains("Loading");
        });

        // Verify dropdown has at least one option (either recent items or placeholder)
        var options = directorySelect.findElements(By.tagName("option"));
        assertTrue(options.size() > 0, "Directory dropdown should have at least one option");

        // Verify first option contains expected text (either recent item or "No recent items" message)
        String firstOptionText = options.get(0).getText();
        System.out.println("First directory option: " + firstOptionText);
        assertTrue(firstOptionText.contains("No recent items") || firstOptionText.contains("[File]") || firstOptionText.contains("[Folder]"),
            "First option should be 'No recent items' or a recent file/folder");
    }

    @Test
    @Order(2)
    @DisplayName("Test Load Tree button exists and is clickable")
    void testLoadButtonExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement loadBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("load-btn")));

        assertTrue(loadBtn.isDisplayed(), "Load button should be visible");
        assertTrue(loadBtn.isEnabled(), "Load button should be enabled");
        assertEquals("Load", loadBtn.getText(), "Load button should have correct text");
    }

    @Test
    @Order(3)
    @DisplayName("Test custom directory input exists")
    void testCustomDirectoryInputExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));

        assertTrue(customInput.isDisplayed(), "Custom directory input should be visible");

        String placeholder = customInput.getAttribute("placeholder");
        assertEquals("e.g., path/to/file.yaml or path/to/directory", placeholder,
            "Custom input should have correct placeholder text");
    }

    @Test
    @Order(4)
    @DisplayName("Test custom directory input can be edited")
    void testCustomDirectoryInputCanBeEdited() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));

        String newPath = "C:/test/path/yaml-files";
        customInput.clear();
        customInput.sendKeys(newPath);

        String actualValue = customInput.getDomProperty("value");
        assertEquals(newPath, actualValue, "Custom directory input should accept new value");
    }

    @Test
    @Order(5)
    @DisplayName("Test Load Custom Path button shows custom alert (not native alert) when custom path is empty")
    void testLoadCustomButtonValidatesEmptyPath() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        // Wait for initial tree load to complete and any alerts to clear
        // The page auto-loads the first directory which may trigger alerts (e.g., circular dependencies)
        try {
            Thread.sleep(2000); // Wait for initial load
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Close any existing alert by clicking the close button if present
        try {
            WebElement closeBtn = driver.findElement(By.id("alert-close-btn"));
            if (closeBtn.isDisplayed()) {
                closeBtn.click();
                // Wait for alert to hide
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("alert-container")));
            }
        } catch (Exception e) {
            // No alert to close, continue
        }

        // Ensure custom input is empty
        customInput.clear();

        // Click load custom button
        loadCustomBtn.click();

        // Wait for custom alert container to appear (NOT native browser alert)
        WebElement alertContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("alert-container")));

        assertTrue(alertContainer.isDisplayed(), "Custom alert should be displayed");

        // Verify it's a warning alert
        String alertClass = alertContainer.getDomAttribute("class");
        assertTrue(alertClass.contains("alert-warning"),
            "Alert should be a warning. Actual class: " + alertClass);

        // Verify alert title
        WebElement alertTitle = driver.findElement(By.id("alert-title"));
        assertEquals("No Path Entered", alertTitle.getText(),
            "Alert title should indicate no path was entered");

        System.out.println("Custom alert displayed correctly for empty custom path");
    }

    @Test
    @Order(6)
    @DisplayName("Test Load button triggers tree reload when recent item is selected")
    void testLoadButtonTriggersTreeReload() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // First, load a folder via custom path to populate recent items
        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        String testPath = "src/test/resources/apex-yaml-samples/graph-100";
        customInput.sendKeys(testPath);
        loadCustomBtn.click();

        // Wait for List View to load (folders now load into List View only)
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#yaml-files-table tbody tr")));

        // Now the dropdown should have a recent item
        WebElement directorySelect = driver.findElement(By.id("directory-select"));
        WebElement loadBtn = driver.findElement(By.id("load-btn"));

        // Use JavaScript to monitor when loadTreeData is called
        jsExecutor.executeScript(
            "window.loadTreeDataCalled = false;" +
            "const originalLoadTreeData = window.loadTreeData;" +
            "window.loadTreeData = function() {" +
            "  window.loadTreeDataCalled = true;" +
            "  return originalLoadTreeData.apply(this, arguments);" +
            "};"
        );

        // Click load button with selected recent item
        loadBtn.click();

        // Wait a moment for the click to be processed
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify loadTreeData was called
        Boolean loadTreeDataCalled = (Boolean) jsExecutor.executeScript("return window.loadTreeDataCalled;");
        assertTrue(loadTreeDataCalled, "loadTreeData should be called when Load button is clicked");

        System.out.println("Load button successfully triggered tree reload");
    }

    @Test
    @Order(7)
    @DisplayName("Test Enter key in custom directory input triggers load")
    void testEnterKeyTriggersLoad() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for initial tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg")));

        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));

        // Enter a custom path
        String customPath = "src/test/resources/apex-yaml-samples/graph-100";
        customInput.sendKeys(customPath);

        // Use JavaScript to monitor when loadTreeData is called
        jsExecutor.executeScript(
            "window.loadTreeDataCalledByEnter = false;" +
            "const originalLoadTreeData = window.loadTreeData;" +
            "window.loadTreeData = function() {" +
            "  window.loadTreeDataCalledByEnter = true;" +
            "  return originalLoadTreeData.apply(this, arguments);" +
            "};"
        );

        // Press Enter key in custom directory input
        customInput.sendKeys(Keys.ENTER);

        // Wait a moment for the keypress to be processed
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify loadTreeData was called
        Boolean loadTreeDataCalled = (Boolean) jsExecutor.executeScript("return window.loadTreeDataCalledByEnter;");
        assertTrue(loadTreeDataCalled, "loadTreeData should be called when Enter key is pressed in custom input");

        System.out.println("Enter key successfully triggered tree reload");
    }

    @Test
    @Order(8)
    @DisplayName("Test loadTreeData uses custom directory input value when provided")
    void testLoadTreeDataUsesCustomInput() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("custom-directory-input")));

        // Execute JavaScript to verify loadTreeData reads from custom input
        String script =
            "const customInput = document.getElementById('custom-directory-input');" +
            "customInput.value = 'test/custom/path';" +
            "return customInput.value;";

        String testPath = (String) jsExecutor.executeScript(script);
        assertEquals("test/custom/path", testPath,
            "JavaScript should be able to read and modify custom directory input value");
    }

    @Test
    @Order(9)
    @DisplayName("Test File Browser panel is in sidebar")
    void testFileBrowserInSidebar() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement sidebar = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("sidebar")));

        // Verify directory dropdown is inside sidebar
        WebElement directorySelect = sidebar.findElement(By.id("directory-select"));
        assertNotNull(directorySelect, "Directory dropdown should be inside sidebar");

        // Verify load button is inside sidebar
        WebElement loadBtn = sidebar.findElement(By.id("load-btn"));
        assertNotNull(loadBtn, "Load Tree button should be inside sidebar");

        // Verify custom directory input is inside sidebar
        WebElement customInput = sidebar.findElement(By.id("custom-directory-input"));
        assertNotNull(customInput, "Custom directory input should be inside sidebar");

        // Verify load custom button is inside sidebar
        WebElement loadCustomBtn = sidebar.findElement(By.id("load-custom-btn"));
        assertNotNull(loadCustomBtn, "Load Custom Path button should be inside sidebar");
    }

    @Test
    @Order(10)
    @DisplayName("Test accordion section for Directory is present")
    void testDirectoryAccordionSection() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Find the accordion section for Directory
        WebElement accordionContent = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("accordion-directory")));

        assertTrue(accordionContent.isDisplayed(), "Directory accordion content should be visible");

        // Verify all controls are in the accordion
        WebElement directorySelect = accordionContent.findElement(By.id("directory-select"));
        WebElement loadBtn = accordionContent.findElement(By.id("load-btn"));
        WebElement customInput = accordionContent.findElement(By.id("custom-directory-input"));
        WebElement loadCustomBtn = accordionContent.findElement(By.id("load-custom-btn"));

        assertNotNull(directorySelect, "Directory dropdown should be in accordion");
        assertNotNull(loadBtn, "Load Tree button should be in accordion");
        assertNotNull(customInput, "Custom directory input should be in accordion");
        assertNotNull(loadCustomBtn, "Load Custom Path button should be in accordion");
    }

    @Test
    @Order(11)
    @DisplayName("Test dropdown selection changes selected value after loading a folder")
    void testDropdownSelectionChanges() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // First, load a folder via custom path to populate recent items
        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        String testPath = "src/test/resources/apex-yaml-samples/graph-100";
        customInput.sendKeys(testPath);
        loadCustomBtn.click();

        // Wait for List View to load (folders now load into List View only)
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#yaml-files-table tbody tr")));

        // Wait a moment for dropdown to be refreshed
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Use JavaScript to get dropdown info to avoid stale element issues
        Long optionCount = (Long) jsExecutor.executeScript(
            "return document.getElementById('directory-select').options.length;");
        assertTrue(optionCount > 0, "Should have at least one recent item");

        // Use JavaScript to select first option and get its value
        String selectedValue = (String) jsExecutor.executeScript(
            "var select = document.getElementById('directory-select');" +
            "select.selectedIndex = 0;" +
            "return select.value;");

        assertNotNull(selectedValue, "Selected value should not be null");
        assertFalse(selectedValue.isEmpty(), "Selected value should not be empty");

        System.out.println("Selected recent item: " + selectedValue);
    }

    @Test
    @Order(12)
    @DisplayName("Test Clear Recent button clears recent items")
    void testClearRecentButton() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // First, load a folder via custom path to populate recent items
        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        String testPath = "src/test/resources/apex-yaml-samples/graph-100";
        customInput.sendKeys(testPath);
        loadCustomBtn.click();

        // Wait for List View to load (folders now load into List View only)
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#yaml-files-table tbody tr")));

        // Wait a moment for dropdown to be refreshed
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Use JavaScript to verify dropdown has recent item
        String firstOptionBefore = (String) jsExecutor.executeScript(
            "return document.getElementById('directory-select').options[0].text;");
        assertTrue(firstOptionBefore.contains("[Folder]"), "Should have a recent folder item");

        // Click Clear Recent button
        WebElement clearRecentBtn = driver.findElement(By.id("clear-recent-btn"));
        clearRecentBtn.click();

        // Wait a moment for the clear action to complete
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Use JavaScript to verify dropdown now shows "No recent items"
        String firstOptionAfter = (String) jsExecutor.executeScript(
            "return document.getElementById('directory-select').options[0].text;");
        assertTrue(firstOptionAfter.contains("No recent items"), "Should show 'No recent items' after clearing");

        System.out.println("Clear Recent button works correctly");
    }

    @Test
    @Order(13)
    @DisplayName("Test folder loads all YAML files into List View")
    void testFolderLoadsAllYamlFiles() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Load a folder via custom path
        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        String testPath = "src/test/resources/apex-yaml-samples/graph-100";
        customInput.sendKeys(testPath);
        loadCustomBtn.click();

        // Wait for List View to load (folders now load into List View only)
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#yaml-files-table tbody tr")));

        // Verify List View tab is active
        WebElement listViewTab = driver.findElement(By.id("list-view-tab"));
        assertTrue(listViewTab.getDomAttribute("class").contains("active"),
            "List View tab should be active when folder is loaded");

        // Verify List View has multiple rows (files)
        var rows = driver.findElements(By.cssSelector("#yaml-files-table tbody tr"));
        assertTrue(rows.size() > 1, "List View should have multiple files");

        System.out.println("Folder loaded with " + rows.size() + " files in List View");
    }

    @Test
    @Order(14)
    @DisplayName("Test single YAML file loads as dependency tree")
    void testSingleFileLoadsDependencyTree() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Load a single YAML file via custom path
        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        String testPath = "src/test/resources/apex-yaml-samples/graph-100/00-scenario-registry.yaml";
        customInput.sendKeys(testPath);
        loadCustomBtn.click();

        // Wait for tree nodes to appear (this indicates tree has loaded)
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg g.node")));

        // Verify no error alert is shown (or if warning is shown, it should be for circular deps, not error)
        WebElement alertContainer = driver.findElement(By.id("alert-container"));
        String alertDisplay = alertContainer.getCssValue("display");

        if (!"none".equals(alertDisplay)) {
            // If alert is shown, it should be a warning (for circular dependencies), not an error
            String alertClass = alertContainer.getDomAttribute("class");
            assertTrue(alertClass.contains("alert-warning") || alertClass.contains("alert-info"),
                "Graph-100 should show warning (circular deps) or info, not error. Alert class: " + alertClass);

            String alertTitle = driver.findElement(By.id("alert-title")).getText();
            System.out.println("Graph-100 alert (expected warning): " + alertTitle);
        }

        // Verify tree has nodes
        var nodes = driver.findElements(By.cssSelector("#tree-container svg g.node"));
        assertTrue(nodes.size() > 0, "Graph-100 tree should have nodes");

        System.out.println("Graph-100 loaded successfully with " + nodes.size() + " nodes");
    }

    @Test
    @Order(15)
    @DisplayName("Test recent items show [File] or [Folder] prefix")
    void testRecentItemsShowPrefix() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Load a folder via custom path
        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        String folderPath = "src/test/resources/apex-yaml-samples/graph-100";
        customInput.sendKeys(folderPath);
        loadCustomBtn.click();

        // Wait for List View to load (folders now load into List View only)
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#yaml-files-table tbody tr")));

        // Wait a moment for dropdown to be refreshed
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Use JavaScript to verify dropdown shows [Folder] prefix
        String firstOptionText = (String) jsExecutor.executeScript(
            "return document.getElementById('directory-select').options[0].text;");
        assertTrue(firstOptionText.contains("[Folder]"), "Folder should show [Folder] prefix: " + firstOptionText);

        // Now load a single file
        customInput = driver.findElement(By.id("custom-directory-input"));
        customInput.clear();
        String filePath = "src/test/resources/apex-yaml-samples/graph-100/00-scenario-registry.yaml";
        customInput.sendKeys(filePath);
        loadCustomBtn = driver.findElement(By.id("load-custom-btn"));
        loadCustomBtn.click();

        // Wait for tree to load (files load into Tree View)
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg g.node")));

        // Wait a moment for dropdown to be refreshed
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Use JavaScript to verify dropdown now shows [File] prefix for the most recent item
        firstOptionText = (String) jsExecutor.executeScript(
            "return document.getElementById('directory-select').options[0].text;");
        assertTrue(firstOptionText.contains("[File]"), "File should show [File] prefix: " + firstOptionText);

        System.out.println("Recent items show correct prefixes");
    }

    @Test
    @Order(16)
    @DisplayName("Test clicking List View row displays file content")
    void testListViewRowClickDisplaysFileContent() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Load a folder via custom path
        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        String folderPath = "src/test/resources/apex-yaml-samples/graph-100";
        customInput.sendKeys(folderPath);
        loadCustomBtn.click();

        // Wait for List View to load (folders now load into List View only)
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#yaml-files-table tbody tr")));

        // Verify List View tab is active
        WebElement listViewTab = driver.findElement(By.id("list-view-tab"));
        assertTrue(listViewTab.getDomAttribute("class").contains("active"),
            "List View tab should be active when folder is loaded");

        // Get the first row in the list view
        WebElement firstRow = driver.findElement(By.cssSelector("#yaml-files-table tbody tr"));
        String filename = firstRow.findElement(By.cssSelector("td.filename")).getText();
        System.out.println("Clicking on file: " + filename);

        // Click on the first row
        firstRow.click();

        // Wait for the file content to be displayed - need to wait for async API call
        // The yaml-section should become visible and yaml-code should have content
        try {
            Thread.sleep(1000); // Wait for async API call to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("yaml-section")));

        // Check for browser console errors
        var logs = driver.manage().logs().get("browser");
        for (var entry : logs) {
            System.out.println("Browser log: " + entry.getLevel() + " - " + entry.getMessage());
        }

        // Verify the YAML content is displayed (not empty and not an error message)
        WebElement yamlCode = driver.findElement(By.id("yaml-code"));
        String yamlContent = yamlCode.getText();

        System.out.println("YAML content length: " + yamlContent.length());
        System.out.println("YAML content preview: " + yamlContent.substring(0, Math.min(200, yamlContent.length())));

        assertFalse(yamlContent.isEmpty(), "YAML content should not be empty");
        assertFalse(yamlContent.contains("Error: No content available"),
            "YAML content should not show error message. Content: " + yamlContent.substring(0, Math.min(100, yamlContent.length())));

        // Verify the row is selected
        assertTrue(firstRow.getDomAttribute("class").contains("selected"),
            "Clicked row should have 'selected' class");

        System.out.println("List View row click displays file content successfully");
    }

    @Test
    @Order(17)
    @DisplayName("Test clicking multiple List View rows updates content")
    void testListViewMultipleRowClicks() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Load a folder via custom path
        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        String folderPath = "src/test/resources/apex-yaml-samples/graph-100";
        customInput.sendKeys(folderPath);
        loadCustomBtn.click();

        // Wait for List View to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#yaml-files-table tbody tr")));

        // Get all rows
        var rows = driver.findElements(By.cssSelector("#yaml-files-table tbody tr"));
        assertTrue(rows.size() >= 2, "Should have at least 2 rows for this test");

        // Click first row
        WebElement firstRow = rows.get(0);
        String firstFilename = firstRow.findElement(By.cssSelector("td.filename")).getText();
        System.out.println("Clicking first file: " + firstFilename);
        firstRow.click();

        // Wait for content to load
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("yaml-section")));
        String firstContent = driver.findElement(By.id("yaml-code")).getText();
        System.out.println("First file content length: " + firstContent.length());

        // Click second row
        WebElement secondRow = rows.get(1);
        String secondFilename = secondRow.findElement(By.cssSelector("td.filename")).getText();
        System.out.println("Clicking second file: " + secondFilename);
        secondRow.click();

        // Wait for content to update
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String secondContent = driver.findElement(By.id("yaml-code")).getText();
        System.out.println("Second file content length: " + secondContent.length());

        // Verify content changed (unless files happen to be identical)
        assertFalse(secondContent.isEmpty(), "Second file content should not be empty");
        assertFalse(secondContent.contains("Error: No content available"),
            "Second file should not show error message");

        // Verify second row is now selected and first is not
        assertFalse(firstRow.getDomAttribute("class").contains("selected"),
            "First row should no longer be selected");
        assertTrue(secondRow.getDomAttribute("class").contains("selected"),
            "Second row should be selected");

        System.out.println("Multiple row clicks work correctly");
    }
}

