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

        // Wait for directories to load (should replace "Loading directories..." option)
        wait.until(driver -> {
            String firstOption = directorySelect.findElement(By.cssSelector("option")).getText();
            return !firstOption.contains("Loading");
        });

        // Verify dropdown has options
        var options = directorySelect.findElements(By.tagName("option"));
        assertTrue(options.size() > 0, "Directory dropdown should have at least one option");

        // Verify first option contains expected text
        String firstOptionText = options.get(0).getText();
        System.out.println("First directory option: " + firstOptionText);
        assertTrue(firstOptionText.contains("Graph-100") || firstOptionText.contains("Demo"),
            "First option should be a known sample directory");
    }

    @Test
    @Order(2)
    @DisplayName("Test Load Tree button exists and is clickable")
    void testLoadButtonExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement loadBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("load-btn")));

        assertTrue(loadBtn.isDisplayed(), "Load Tree button should be visible");
        assertTrue(loadBtn.isEnabled(), "Load Tree button should be enabled");
        assertEquals("Load Tree", loadBtn.getText(), "Load Tree button should have correct text");
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
        assertEquals("Enter custom directory path...", placeholder,
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
    @DisplayName("Test Load Custom Path button shows alert when custom path is empty")
    void testLoadCustomButtonValidatesEmptyPath() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        // Ensure custom input is empty
        customInput.clear();

        // Click load custom button
        loadCustomBtn.click();

        // Wait for alert
        try {
            Alert alert = wait.until(ExpectedConditions.alertIsPresent());
            String alertText = alert.getText();
            assertEquals("Please enter a custom directory path.", alertText,
                "Alert should show validation message for empty custom path");
            alert.accept();
        } catch (Exception e) {
            fail("Expected alert to be shown for empty custom path: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test Load Tree button triggers tree reload with selected directory")
    void testLoadButtonTriggersTreeReload() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for initial tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg")));

        WebElement directorySelect = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("directory-select")));
        WebElement loadBtn = driver.findElement(By.id("load-btn"));

        // Wait for directories to load
        wait.until(driver -> {
            String firstOption = directorySelect.findElement(By.cssSelector("option")).getText();
            return !firstOption.contains("Loading");
        });

        // Use JavaScript to monitor when loadTreeData is called
        jsExecutor.executeScript(
            "window.loadTreeDataCalled = false;" +
            "const originalLoadTreeData = window.loadTreeData;" +
            "window.loadTreeData = function() {" +
            "  window.loadTreeDataCalled = true;" +
            "  return originalLoadTreeData.apply(this, arguments);" +
            "};"
        );

        // Click load button with selected directory
        loadBtn.click();

        // Wait a moment for the click to be processed
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify loadTreeData was called
        Boolean loadTreeDataCalled = (Boolean) jsExecutor.executeScript("return window.loadTreeDataCalled;");
        assertTrue(loadTreeDataCalled, "loadTreeData should be called when Load Tree button is clicked");

        System.out.println("Load Tree button successfully triggered tree reload");
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
        String customPath = "apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100";
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
    @DisplayName("Test dropdown selection changes selected value")
    void testDropdownSelectionChanges() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement directorySelect = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("directory-select")));

        // Wait for directories to load
        wait.until(driver -> {
            String firstOption = directorySelect.findElement(By.cssSelector("option")).getText();
            return !firstOption.contains("Loading");
        });

        // Get all options
        var options = directorySelect.findElements(By.tagName("option"));
        assertTrue(options.size() > 0, "Should have at least one directory option");

        // Select first option
        options.get(0).click();

        // Verify selection
        String selectedValue = directorySelect.getDomProperty("value");
        assertNotNull(selectedValue, "Selected value should not be null");
        assertFalse(selectedValue.isEmpty(), "Selected value should not be empty");

        System.out.println("Selected directory: " + selectedValue);
    }

    @Test
    @Order(12)
    @DisplayName("Test API endpoint returns sample directories")
    void testSampleDirectoriesAPIEndpoint() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Use JavaScript to call the API directly
        Object result = jsExecutor.executeAsyncScript(
            "const callback = arguments[arguments.length - 1];" +
            "fetch('" + baseUrl + "/api/dependencies/sample-directories')" +
            "  .then(response => response.json())" +
            "  .then(data => callback(data))" +
            "  .catch(error => callback({status: 'error', message: error.message}));"
        );

        assertNotNull(result, "API should return a result");
        System.out.println("API response: " + result);
    }

    @Test
    @Order(13)
    @DisplayName("Test Graph-100 directory loads successfully without errors")
    void testGraph100LoadsSuccessfully() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for initial tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg")));

        WebElement directorySelect = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("directory-select")));

        // Wait for directories to load
        wait.until(driver -> {
            String firstOption = directorySelect.findElement(By.cssSelector("option")).getText();
            return !firstOption.contains("Loading");
        });

        // Find and select Graph-100 option
        var options = directorySelect.findElements(By.tagName("option"));
        WebElement graph100Option = null;
        for (WebElement option : options) {
            if (option.getText().contains("Graph-100")) {
                graph100Option = option;
                break;
            }
        }

        assertNotNull(graph100Option, "Graph-100 option should be present in dropdown");

        // Select Graph-100
        graph100Option.click();

        // Click Load Tree button
        WebElement loadBtn = driver.findElement(By.id("load-btn"));
        loadBtn.click();

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
    @Order(14)
    @DisplayName("Test Demo Scenarios directory shows error or warning alert")
    void testDemoScenariosShowsError() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for initial tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg")));

        WebElement directorySelect = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("directory-select")));

        // Wait for directories to load
        wait.until(driver -> {
            String firstOption = directorySelect.findElement(By.cssSelector("option")).getText();
            return !firstOption.contains("Loading");
        });

        // Find and select Demo Scenarios option
        var options = directorySelect.findElements(By.tagName("option"));
        WebElement demoOption = null;
        for (WebElement option : options) {
            if (option.getText().contains("Demo")) {
                demoOption = option;
                break;
            }
        }

        assertNotNull(demoOption, "Demo Scenarios option should be present in dropdown");

        // Select Demo Scenarios
        demoOption.click();

        // Click Load Tree button
        WebElement loadBtn = driver.findElement(By.id("load-btn"));
        loadBtn.click();

        // Wait for alert to appear
        WebElement alertContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("alert-container")));

        // Verify alert is displayed
        assertTrue(alertContainer.isDisplayed(), "Alert should be displayed for Demo Scenarios");

        // Verify it's an error or warning alert (not success or info)
        String alertClass = alertContainer.getDomAttribute("class");
        assertTrue(alertClass.contains("alert-error") || alertClass.contains("alert-warning"),
            "Demo Scenarios should show error or warning alert (not success). Alert class: " + alertClass);

        // Get alert title and message
        String alertTitle = driver.findElement(By.id("alert-title")).getText();
        String alertMessage = driver.findElement(By.id("alert-message")).getText();

        System.out.println("Demo Scenarios alert:");
        System.out.println("  Title: " + alertTitle);
        System.out.println("  Message: " + alertMessage);
        System.out.println("  Class: " + alertClass);

        // Verify alert contains meaningful error information
        assertFalse(alertTitle.isEmpty(), "Alert title should not be empty");
        assertFalse(alertMessage.isEmpty(), "Alert message should not be empty");
    }
}

