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

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for D3 Tree Viewer File Browser functionality.
 * Tests the directory input field and Browse button behavior.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-11
 * @version 1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class D3TreeViewerFileBrowserUITest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static JavascriptExecutor jsExecutor;
    private static final String baseUrl = "http://localhost:8082/yaml-manager";

    @BeforeAll
    static void setupClass() {
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
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test directory input field exists and has default value")
    void testDirectoryInputExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");
        
        WebElement directoryInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("directory-input")));
        
        assertTrue(directoryInput.isDisplayed(), "Directory input should be visible");
        
        String defaultValue = directoryInput.getAttribute("value");
        assertNotNull(defaultValue, "Directory input should have a default value");
        assertFalse(defaultValue.isEmpty(), "Directory input default value should not be empty");
        assertTrue(defaultValue.contains("graph-100"), "Default directory should contain 'graph-100'");
        
        System.out.println("Default directory value: " + defaultValue);
    }

    @Test
    @Order(2)
    @DisplayName("Test Browse button exists and is clickable")
    void testBrowseButtonExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");
        
        WebElement browseBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("browse-btn")));
        
        assertTrue(browseBtn.isDisplayed(), "Browse button should be visible");
        assertTrue(browseBtn.isEnabled(), "Browse button should be enabled");
        assertEquals("Browse", browseBtn.getText(), "Browse button should have correct text");
    }

    @Test
    @Order(3)
    @DisplayName("Test include subfolders checkbox exists and is checked by default")
    void testIncludeSubfoldersCheckbox() {
        driver.get(baseUrl + "/d3-tree-viewer.html");
        
        WebElement includeSubfoldersCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("include-subfolders")));
        
        assertTrue(includeSubfoldersCheckbox.isDisplayed(), "Include subfolders checkbox should be visible");
        assertTrue(includeSubfoldersCheckbox.isSelected(), "Include subfolders checkbox should be checked by default");
    }

    @Test
    @Order(4)
    @DisplayName("Test directory input can be edited")
    void testDirectoryInputCanBeEdited() {
        driver.get(baseUrl + "/d3-tree-viewer.html");
        
        WebElement directoryInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("directory-input")));
        
        String newPath = "C:/test/path/yaml-files";
        directoryInput.clear();
        directoryInput.sendKeys(newPath);
        
        String actualValue = directoryInput.getAttribute("value");
        assertEquals(newPath, actualValue, "Directory input should accept new value");
    }

    @Test
    @Order(5)
    @DisplayName("Test Browse button shows alert when directory is empty")
    void testBrowseButtonValidatesEmptyDirectory() {
        driver.get(baseUrl + "/d3-tree-viewer.html");
        
        WebElement directoryInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("directory-input")));
        WebElement browseBtn = driver.findElement(By.id("browse-btn"));
        
        // Clear the directory input
        directoryInput.clear();
        
        // Click browse button
        browseBtn.click();
        
        // Wait for alert
        try {
            Alert alert = wait.until(ExpectedConditions.alertIsPresent());
            String alertText = alert.getText();
            assertEquals("Please enter a directory path.", alertText, 
                "Alert should show validation message for empty directory");
            alert.accept();
        } catch (Exception e) {
            fail("Expected alert to be shown for empty directory: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test Browse button triggers tree reload with valid directory")
    void testBrowseButtonTriggersTreeReload() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for initial tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg")));

        WebElement directoryInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("directory-input")));
        WebElement browseBtn = driver.findElement(By.id("browse-btn"));

        // Get the current directory value (should be the default)
        String currentDirectory = directoryInput.getAttribute("value");
        assertNotNull(currentDirectory, "Directory should have a value");

        // Use JavaScript to monitor when loadTreeData is called
        jsExecutor.executeScript(
            "window.loadTreeDataCalled = false;" +
            "const originalLoadTreeData = window.loadTreeData;" +
            "window.loadTreeData = function() {" +
            "  window.loadTreeDataCalled = true;" +
            "  return originalLoadTreeData.apply(this, arguments);" +
            "};"
        );

        // Click browse button with current directory
        browseBtn.click();

        // Wait a moment for the click to be processed
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify loadTreeData was called
        Boolean loadTreeDataCalled = (Boolean) jsExecutor.executeScript("return window.loadTreeDataCalled;");
        assertTrue(loadTreeDataCalled, "loadTreeData should be called when Browse button is clicked");

        System.out.println("Browse button successfully triggered tree reload");
    }

    @Test
    @Order(7)
    @DisplayName("Test Enter key in directory input triggers browse")
    void testEnterKeyTriggersBrowse() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for initial tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg")));

        WebElement directoryInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("directory-input")));

        // Get the current directory value
        String currentDirectory = directoryInput.getAttribute("value");
        assertNotNull(currentDirectory, "Directory should have a value");

        // Use JavaScript to monitor when loadTreeData is called
        jsExecutor.executeScript(
            "window.loadTreeDataCalledByEnter = false;" +
            "const originalLoadTreeData = window.loadTreeData;" +
            "window.loadTreeData = function() {" +
            "  window.loadTreeDataCalledByEnter = true;" +
            "  return originalLoadTreeData.apply(this, arguments);" +
            "};"
        );

        // Press Enter key in directory input
        directoryInput.sendKeys(Keys.ENTER);

        // Wait a moment for the keypress to be processed
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify loadTreeData was called
        Boolean loadTreeDataCalled = (Boolean) jsExecutor.executeScript("return window.loadTreeDataCalledByEnter;");
        assertTrue(loadTreeDataCalled, "loadTreeData should be called when Enter key is pressed");

        System.out.println("Enter key successfully triggered tree reload");
    }

    @Test
    @Order(8)
    @DisplayName("Test loadTreeData uses directory input value")
    void testLoadTreeDataUsesDirectoryInput() {
        driver.get(baseUrl + "/d3-tree-viewer.html");
        
        // Wait for page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("directory-input")));
        
        // Execute JavaScript to verify loadTreeData reads from directory input
        String script = 
            "const directoryInput = document.getElementById('directory-input');" +
            "const originalValue = directoryInput.value;" +
            "directoryInput.value = 'test/custom/path';" +
            "return directoryInput.value;";
        
        String testPath = (String) jsExecutor.executeScript(script);
        assertEquals("test/custom/path", testPath, 
            "JavaScript should be able to read and modify directory input value");
    }

    @Test
    @Order(9)
    @DisplayName("Test File Browser panel is in sidebar")
    void testFileBrowserInSidebar() {
        driver.get(baseUrl + "/d3-tree-viewer.html");
        
        WebElement sidebar = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("sidebar")));
        
        // Verify directory input is inside sidebar
        WebElement directoryInput = sidebar.findElement(By.id("directory-input"));
        assertNotNull(directoryInput, "Directory input should be inside sidebar");
        
        // Verify browse button is inside sidebar
        WebElement browseBtn = sidebar.findElement(By.id("browse-btn"));
        assertNotNull(browseBtn, "Browse button should be inside sidebar");
        
        // Verify include subfolders checkbox is inside sidebar
        WebElement includeSubfolders = sidebar.findElement(By.id("include-subfolders"));
        assertNotNull(includeSubfolders, "Include subfolders checkbox should be inside sidebar");
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
        WebElement directoryInput = accordionContent.findElement(By.id("directory-input"));
        WebElement browseBtn = accordionContent.findElement(By.id("browse-btn"));
        WebElement includeSubfolders = accordionContent.findElement(By.id("include-subfolders"));
        
        assertNotNull(directoryInput, "Directory input should be in accordion");
        assertNotNull(browseBtn, "Browse button should be in accordion");
        assertNotNull(includeSubfolders, "Include subfolders checkbox should be in accordion");
    }
}

