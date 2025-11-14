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
 * Selenium UI tests for D3 Tree Viewer Custom Path Loading functionality.
 * Tests the custom path input field and Load Custom Path button behavior.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-13
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class D3TreeViewerCustomPathLoadingUITest {

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
    @DisplayName("Test custom path input has correct placeholder text")
    void testCustomPathInputPlaceholder() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));

        assertTrue(customInput.isDisplayed(), "Custom path input should be visible");

        String placeholder = customInput.getAttribute("placeholder");
        assertEquals("e.g., path/to/file.yaml or path/to/directory", placeholder,
            "Custom input should have correct placeholder text indicating file or directory");
    }

    @Test
    @Order(2)
    @DisplayName("Test custom path label indicates file or directory")
    void testCustomPathLabelText() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Find the label for custom-directory-input
        WebElement label = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("label[for='custom-directory-input']")));

        String labelText = label.getText();
        assertTrue(labelText.contains("file or directory"),
            "Label should indicate that user can enter file or directory path. Actual: " + labelText);
    }

    @Test
    @Order(3)
    @DisplayName("Test Load Custom Path button exists and is clickable")
    void testLoadCustomPathButtonExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement loadCustomBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("load-custom-btn")));

        assertTrue(loadCustomBtn.isDisplayed(), "Load Custom Path button should be visible");
        assertTrue(loadCustomBtn.isEnabled(), "Load Custom Path button should be enabled");
        assertEquals("Load Custom Path", loadCustomBtn.getText(),
            "Load Custom Path button should have correct text");
    }

    @Test
    @Order(4)
    @DisplayName("Test Load Custom Path button shows custom alert (not native alert) when path is empty")
    void testLoadCustomPathButtonShowsCustomAlertForEmptyPath() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

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

        // Verify alert title and message
        WebElement alertTitle = driver.findElement(By.id("alert-title"));
        WebElement alertMessage = driver.findElement(By.id("alert-message"));

        assertEquals("No Path Entered", alertTitle.getText(),
            "Alert title should indicate no path was entered");

        String messageText = alertMessage.getText();
        assertTrue(messageText.contains("file path") && messageText.contains("directory path"),
            "Alert message should mention both file and directory paths. Actual: " + messageText);

        System.out.println("Custom alert displayed correctly for empty path");
    }

    @Test
    @Order(5)
    @DisplayName("Test no native browser alert is triggered")
    void testNoNativeBrowserAlert() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        // Ensure custom input is empty
        customInput.clear();

        // Click load custom button
        loadCustomBtn.click();

        // Wait a moment for any potential alert
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify NO native alert is present
        try {
            driver.switchTo().alert();
            fail("Native browser alert should NOT be present - custom UI alert should be used instead");
        } catch (NoAlertPresentException e) {
            // This is expected - no native alert should be present
            System.out.println("Confirmed: No native browser alert triggered (as expected)");
        }

        // Verify custom alert IS present
        WebElement alertContainer = driver.findElement(By.id("alert-container"));
        assertTrue(alertContainer.isDisplayed(), "Custom alert should be displayed instead of native alert");
    }

    @Test
    @Order(6)
    @DisplayName("Test custom path input accepts file path ending with .yaml")
    void testCustomPathAcceptsYamlFilePath() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));

        String yamlFilePath = "apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/00-scenario-registry.yaml";
        customInput.clear();
        customInput.sendKeys(yamlFilePath);

        String actualValue = customInput.getDomProperty("value");
        assertEquals(yamlFilePath, actualValue, "Custom input should accept YAML file path");

        System.out.println("Custom input accepted YAML file path: " + yamlFilePath);
    }

    @Test
    @Order(7)
    @DisplayName("Test custom path input accepts directory path")
    void testCustomPathAcceptsDirectoryPath() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));

        String directoryPath = "apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100";
        customInput.clear();
        customInput.sendKeys(directoryPath);

        String actualValue = customInput.getDomProperty("value");
        assertEquals(directoryPath, actualValue, "Custom input should accept directory path");

        System.out.println("Custom input accepted directory path: " + directoryPath);
    }

    @Test
    @Order(8)
    @DisplayName("Test Load Custom Path with valid file path triggers tree load")
    void testLoadCustomPathWithValidFilePath() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for initial tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg")));

        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        // Enter a valid YAML file path
        String yamlFilePath = "apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/00-scenario-registry.yaml";
        customInput.clear();
        customInput.sendKeys(yamlFilePath);

        // Use JavaScript to monitor when loadTreeData is called
        jsExecutor.executeScript(
            "window.loadTreeDataCalledWithFile = false;" +
            "const originalLoadTreeData = window.loadTreeData;" +
            "window.loadTreeData = function() {" +
            "  window.loadTreeDataCalledWithFile = true;" +
            "  return originalLoadTreeData.apply(this, arguments);" +
            "};"
        );

        // Click Load Custom Path button
        loadCustomBtn.click();

        // Wait a moment for the click to be processed
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify loadTreeData was called
        Boolean loadTreeDataCalled = (Boolean) jsExecutor.executeScript("return window.loadTreeDataCalledWithFile;");
        assertTrue(loadTreeDataCalled, "loadTreeData should be called when Load Custom Path button is clicked with file path");

        System.out.println("Load Custom Path successfully triggered tree reload with file path");
    }

    @Test
    @Order(9)
    @DisplayName("Test Load Custom Path with valid directory path triggers tree load")
    void testLoadCustomPathWithValidDirectoryPath() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for initial tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg")));

        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        // Enter a valid directory path
        String directoryPath = "apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100";
        customInput.clear();
        customInput.sendKeys(directoryPath);

        // Use JavaScript to monitor when loadTreeData is called
        jsExecutor.executeScript(
            "window.loadTreeDataCalledWithDir = false;" +
            "const originalLoadTreeData = window.loadTreeData;" +
            "window.loadTreeData = function() {" +
            "  window.loadTreeDataCalledWithDir = true;" +
            "  return originalLoadTreeData.apply(this, arguments);" +
            "};"
        );

        // Click Load Custom Path button
        loadCustomBtn.click();

        // Wait a moment for the click to be processed
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify loadTreeData was called
        Boolean loadTreeDataCalled = (Boolean) jsExecutor.executeScript("return window.loadTreeDataCalledWithDir;");
        assertTrue(loadTreeDataCalled, "loadTreeData should be called when Load Custom Path button is clicked with directory path");

        System.out.println("Load Custom Path successfully triggered tree reload with directory path");
    }
}

