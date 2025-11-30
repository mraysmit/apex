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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for D3 Tree Viewer Node Options functionality.
 * Tests the Node Options accordion panel with checkboxes for customizing node display.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class D3TreeViewerNodeOptionsUITest {

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
    @DisplayName("Test Node Options accordion panel exists")
    void testNodeOptionsAccordionExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sidebar")));

        // Find the Node Options accordion header
        WebElement nodeOptionsHeader = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".accordion-header[data-section='node-options']")));
        assertNotNull(nodeOptionsHeader, "Node Options accordion header should exist");

        // Verify the header text - get text from the first span child
        String headerText = (String) jsExecutor.executeScript(
            "return arguments[0].querySelector('span').textContent;", nodeOptionsHeader);
        System.out.println("Header text: '" + headerText + "'");
        assertTrue(headerText.contains("Node Options"), "Header should contain 'Node Options'");

        System.out.println("Node Options accordion panel exists");
    }

    @Test
    @Order(2)
    @DisplayName("Test Node Options accordion content exists")
    void testNodeOptionsAccordionContent() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sidebar")));

        // Find the Node Options accordion content
        WebElement nodeOptionsContent = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("accordion-node-options")));
        assertNotNull(nodeOptionsContent, "Node Options accordion content should exist");

        System.out.println("Node Options accordion content exists");
    }

    @Test
    @Order(3)
    @DisplayName("Test Node Options has filename checkbox (checked by default)")
    void testFilenameCheckboxExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sidebar")));

        // Find the filename checkbox
        WebElement filenameCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("node-show-filename")));
        assertNotNull(filenameCheckbox, "Filename checkbox should exist");
        assertTrue(filenameCheckbox.isSelected(), "Filename checkbox should be checked by default");

        System.out.println("Filename checkbox exists and is checked by default");
    }

    @Test
    @Order(4)
    @DisplayName("Test Node Options has type checkbox (checked by default)")
    void testTypeCheckboxExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sidebar")));

        // Find the type checkbox
        WebElement typeCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("node-show-type")));
        assertNotNull(typeCheckbox, "Type checkbox should exist");
        assertTrue(typeCheckbox.isSelected(), "Type checkbox should be checked by default");

        System.out.println("Type checkbox exists and is checked by default");
    }

    @Test
    @Order(5)
    @DisplayName("Test Node Options has rule count checkbox")
    void testRuleCountCheckboxExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sidebar")));

        // Find the rule count checkbox
        WebElement ruleCountCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("node-show-rule-count")));
        assertNotNull(ruleCountCheckbox, "Rule count checkbox should exist");
        assertFalse(ruleCountCheckbox.isSelected(), "Rule count checkbox should be unchecked by default");

        System.out.println("Rule count checkbox exists and is unchecked by default");
    }

    @Test
    @Order(6)
    @DisplayName("Test Node Options has enrichment count checkbox")
    void testEnrichmentCountCheckboxExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sidebar")));

        // Find the enrichment count checkbox
        WebElement enrichmentCountCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("node-show-enrichment-count")));
        assertNotNull(enrichmentCountCheckbox, "Enrichment count checkbox should exist");
        assertFalse(enrichmentCountCheckbox.isSelected(), "Enrichment count checkbox should be unchecked by default");

        System.out.println("Enrichment count checkbox exists and is unchecked by default");
    }

    @Test
    @Order(7)
    @DisplayName("Test Node Options has metadata ID checkbox")
    void testMetadataIdCheckboxExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sidebar")));

        // Find the metadata ID checkbox
        WebElement metadataIdCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("node-show-metadata-id")));
        assertNotNull(metadataIdCheckbox, "Metadata ID checkbox should exist");
        assertFalse(metadataIdCheckbox.isSelected(), "Metadata ID checkbox should be unchecked by default");

        System.out.println("Metadata ID checkbox exists and is unchecked by default");
    }

    @Test
    @Order(8)
    @DisplayName("Test Node Options has description checkbox (checked by default)")
    void testDescriptionCheckboxExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sidebar")));

        // Find the description checkbox
        WebElement descriptionCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("node-show-description")));
        assertNotNull(descriptionCheckbox, "Description checkbox should exist");
        assertTrue(descriptionCheckbox.isSelected(), "Description checkbox should be checked by default");

        System.out.println("Description checkbox exists and is checked by default");
    }

    @Test
    @Order(9)
    @DisplayName("Test clicking checkbox updates node labels")
    void testCheckboxUpdatesNodeLabels() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Load a specific YAML file to get tree nodes (files load into Tree View)
        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        String testPath = "src/test/resources/apex-yaml-samples/graph-100/00-scenario-registry.yaml";
        customInput.sendKeys(testPath);
        loadCustomBtn.click();

        // Wait for tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg g.node")));

        // Get initial node text
        String initialNodeText = (String) jsExecutor.executeScript(
            "var node = document.querySelector('#tree-container svg g.node text');" +
            "return node ? node.textContent : null;");
        assertNotNull(initialNodeText, "Should have node text");
        System.out.println("Initial node text: " + initialNodeText);

        // Click the rule count checkbox to enable it (unchecked by default)
        WebElement ruleCountCheckbox = driver.findElement(By.id("node-show-rule-count"));
        ruleCountCheckbox.click();

        // Wait a moment for the update
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get updated node text
        String updatedNodeText = (String) jsExecutor.executeScript(
            "var node = document.querySelector('#tree-container svg g.node text');" +
            "return node ? node.textContent : null;");
        assertNotNull(updatedNodeText, "Should have updated node text");
        System.out.println("Updated node text: " + updatedNodeText);

        // The text should be different (should now include rule count)
        assertNotEquals(initialNodeText, updatedNodeText,
            "Node text should change when rule count checkbox is enabled");

        System.out.println("Checkbox updates node labels correctly");
    }

    @Test
    @Order(10)
    @DisplayName("Test all checkboxes are present in Node Options panel")
    void testAllCheckboxesPresent() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sidebar")));

        // Get all checkboxes in the Node Options panel
        List<WebElement> checkboxes = driver.findElements(
            By.cssSelector("#accordion-node-options input[type='checkbox']"));

        // Should have 6 checkboxes (filename, type, rule count, enrichment count, metadata id, description)
        assertTrue(checkboxes.size() >= 6,
            "Should have at least 6 checkboxes, found: " + checkboxes.size());

        System.out.println("Found " + checkboxes.size() + " checkboxes in Node Options panel");
    }

    @Test
    @Order(11)
    @DisplayName("Test background rectangle resizes when checkbox changes")
    void testBackgroundResizesOnCheckboxChange() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Load a specific YAML file to get tree nodes (files load into Tree View)
        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        String testPath = "src/test/resources/apex-yaml-samples/graph-100/00-scenario-registry.yaml";
        customInput.sendKeys(testPath);
        loadCustomBtn.click();

        // Wait for tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg g.node")));

        // Get initial background rectangle height
        Number initialHeightNum = (Number) jsExecutor.executeScript(
            "var rect = document.querySelector('#tree-container svg g.node .label-background');" +
            "return rect ? parseFloat(rect.getAttribute('height')) : null;");
        assertNotNull(initialHeightNum, "Should have initial background height");
        double initialHeight = initialHeightNum.doubleValue();
        System.out.println("Initial background height: " + initialHeight);

        // Click the rule count checkbox to enable it (adds a new line)
        WebElement ruleCountCheckbox = driver.findElement(By.id("node-show-rule-count"));
        ruleCountCheckbox.click();

        // Wait a moment for the update
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get updated background rectangle height
        Number updatedHeightNum = (Number) jsExecutor.executeScript(
            "var rect = document.querySelector('#tree-container svg g.node .label-background');" +
            "return rect ? parseFloat(rect.getAttribute('height')) : null;");
        assertNotNull(updatedHeightNum, "Should have updated background height");
        double updatedHeight = updatedHeightNum.doubleValue();
        System.out.println("Updated background height: " + updatedHeight);

        // The height should be larger (more lines = taller background)
        assertTrue(updatedHeight > initialHeight,
            "Background height should increase when rule count checkbox is enabled. Initial: " + initialHeight + ", Updated: " + updatedHeight);

        System.out.println("Background rectangle resizes correctly when checkbox changes");
    }

    @Test
    @Order(12)
    @DisplayName("Test multi-line display when multiple options selected")
    void testMultiLineDisplay() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Load a specific YAML file to get tree nodes (files load into Tree View)
        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        String testPath = "src/test/resources/apex-yaml-samples/graph-100/00-scenario-registry.yaml";
        customInput.sendKeys(testPath);
        loadCustomBtn.click();

        // Wait for tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg g.node")));

        // Get initial background height (single line)
        Number initialHeightNum = (Number) jsExecutor.executeScript(
            "var rect = document.querySelector('#tree-container svg g.node .label-background');" +
            "return rect ? parseFloat(rect.getAttribute('height')) : null;");
        assertNotNull(initialHeightNum, "Should have initial background height");
        double initialHeight = initialHeightNum.doubleValue();
        System.out.println("Initial background height (1 line): " + initialHeight);

        // Enable multiple options to trigger multi-line display
        WebElement ruleCountCheckbox = driver.findElement(By.id("node-show-rule-count"));
        ruleCountCheckbox.click();

        // Wait a moment for the update
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get updated background height (should be taller with 2 lines)
        Number updatedHeightNum = (Number) jsExecutor.executeScript(
            "var rect = document.querySelector('#tree-container svg g.node .label-background');" +
            "return rect ? parseFloat(rect.getAttribute('height')) : null;");
        assertNotNull(updatedHeightNum, "Should have updated background height");
        double updatedHeight = updatedHeightNum.doubleValue();
        System.out.println("Updated background height (2 lines): " + updatedHeight);

        // The height should be larger (more lines = taller background)
        assertTrue(updatedHeight > initialHeight,
            "Background height should increase with multiple lines. Initial: " + initialHeight + ", Updated: " + updatedHeight);

        // Count tspan elements (should have multiple lines)
        Long tspanCount = (Long) jsExecutor.executeScript(
            "var text = document.querySelector('#tree-container svg g.node text');" +
            "return text ? text.querySelectorAll('tspan').length : 0;");
        System.out.println("Number of tspan elements (lines): " + tspanCount);
        assertTrue(tspanCount >= 2, "Should have at least 2 tspan elements for multi-line display");

        System.out.println("Multi-line display works correctly");
    }

    @Test
    @Order(13)
    @DisplayName("Test different colors for different attribute types")
    void testAttributeColors() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Load a specific YAML file to get tree nodes (files load into Tree View)
        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("custom-directory-input")));
        WebElement loadCustomBtn = driver.findElement(By.id("load-custom-btn"));

        String testPath = "src/test/resources/apex-yaml-samples/graph-100/00-scenario-registry.yaml";
        customInput.sendKeys(testPath);
        loadCustomBtn.click();

        // Wait for tree to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#tree-container svg g.node")));

        // With defaults (filename, type, description checked), we should already have multiple colors
        // Get colors of all tspans
        String colors = (String) jsExecutor.executeScript(
            "var tspans = document.querySelectorAll('#tree-container svg g.node text tspan');" +
            "var colors = [];" +
            "tspans.forEach(function(t) { colors.push(t.style.fill); });" +
            "return colors.join(',');");
        System.out.println("Tspan colors: " + colors);

        // Should have at least 2 colors (filename and type are both checked by default)
        String[] colorArray = colors.split(",");
        assertTrue(colorArray.length >= 2, "Should have at least 2 tspans with colors");

        // The colors should be different (filename is white, type is light blue)
        assertNotEquals(colorArray[0], colorArray[1],
            "Filename and type should have different colors");

        System.out.println("Attribute colors are correctly applied");
    }
}

