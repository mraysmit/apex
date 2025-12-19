package dev.mars.apex.playground.ui;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
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

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base abstract class for Selenium-based YAML import validation tests.
 * Provides common infrastructure, helper methods, and utilities for testing
 * YAML import/export functionality in the APEX Blockly visual editor.
 * 
 * <p>This class includes:</p>
 * <ul>
 *   <li>Selenium WebDriver setup and teardown</li>
 *   <li>Automatic ChromeDriver management via WebDriverManager</li>
 *   <li>Spring Boot test integration</li>
 *   <li>Screenshot capture on test failures</li>
 *   <li>Helper methods for YAML import/export and block validation</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-12-19
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=INFO",
    "apex.playground.examples-enabled=true"
})
@ExtendWith(ScreenshotOnFailureExtension.class)
public abstract class BaseYamlImportSeleniumTest {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected String baseUrl;
    protected JavascriptExecutor js;

    @LocalServerPort
    protected int port;

    /**
     * Setup WebDriverManager to automatically manage ChromeDriver versions.
     * This runs once before all tests in the class.
     */
    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    /**
     * Initialize WebDriver, WebDriverWait, and JavascriptExecutor before each test.
     * Configures Chrome in headless mode with standard options for CI/CD compatibility.
     */
    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        driver = new ChromeDriver(options);
        js = (JavascriptExecutor) driver;
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        baseUrl = "http://localhost:" + port;
    }

    /**
     * Clean up WebDriver resources after each test.
     */
    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // =============== Core Helper Methods ===============

    /**
     * Wait for the Blockly workspace to be fully loaded and ready.
     * Uses JavaScript polling to check workspace availability.
     */
    protected void waitForBlocklyWorkspaceToLoad() {
        wait.until(driver -> {
            Object workspace = js.executeScript("return typeof Blockly !== 'undefined' && Blockly.getMainWorkspace() != null");
            return Boolean.TRUE.equals(workspace);
        });
        
        // Additional wait for workspace to be fully rendered
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Import YAML content into the visual editor.
     * Clicks Import YAML button, waits for modal, enters YAML, and confirms import.
     *
     * @param yamlContent the YAML content to import
     */
    protected void importYamlContent(String yamlContent) {
        // Click Import YAML button
        WebElement importButton = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[contains(text(), 'Import YAML')]")));
        importButton.click();

        // Wait for modal to appear
        WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("yamlImportModal")));

        // Enter YAML content
        WebElement textarea = modal.findElement(By.id("yamlImportInput"));
        textarea.clear();
        textarea.sendKeys(yamlContent);

        // Click Import button in modal
        WebElement importModalBtn = modal.findElement(By.xpath("//button[contains(text(), 'Import')]"));
        importModalBtn.click();

        // Wait for modal to close
        wait.until(ExpectedConditions.invisibilityOf(modal));
    }

    /**
     * Wait for blocks to render after import.
     * Uses a fixed delay to ensure visual editor has finished processing.
     */
    protected void waitForBlocksToRender() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Export YAML content from the visual editor workspace.
     * Uses JavaScript to generate YAML from current Blockly workspace.
     *
     * @return the exported YAML as a string
     */
    protected String exportYamlContent() {
        // Use JavaScript to get generated YAML
        Object yaml = js.executeScript(
            "const workspace = Blockly.getMainWorkspace();" +
            "const code = Blockly.JavaScript.workspaceToCode(workspace);" +
            "return code;"
        );
        return yaml != null ? yaml.toString() : "";
    }

    /**
     * Get the total count of blocks in the workspace.
     *
     * @return the number of blocks
     */
    protected int getBlockCount() {
        Object count = js.executeScript(
            "return Blockly.getMainWorkspace().getAllBlocks(false).length;"
        );
        return count != null ? ((Number) count).intValue() : 0;
    }

    /**
     * Verify that a specific block type exists with the expected count.
     *
     * @param blockType the Blockly block type to search for
     * @param expectedCount the expected number of blocks
     * @param message the assertion message
     */
    protected void verifyBlockExists(String blockType, int expectedCount, String message) {
        Object count = js.executeScript(
            "return Blockly.getMainWorkspace().getAllBlocks(false)" +
            ".filter(b => b.type === '" + blockType + "').length;"
        );
        int actualCount = count != null ? ((Number) count).intValue() : 0;
        assertEquals(expectedCount, actualCount, message + " (found: " + actualCount + ")");
    }

    /**
     * Get the value of a field from the first block of the specified type.
     *
     * @param blockType the Blockly block type
     * @param fieldName the field name to retrieve
     * @return the field value as a string, or null if not found
     */
    protected String getBlockFieldValue(String blockType, String fieldName) {
        Object value = js.executeScript(
            "const blocks = Blockly.getMainWorkspace().getAllBlocks(false)" +
            ".filter(b => b.type === '" + blockType + "');" +
            "if (blocks.length > 0) {" +
            "  const field = blocks[0].getField('" + fieldName + "');" +
            "  return field ? field.getValue() : null;" +
            "}" +
            "return null;"
        );
        return value != null ? value.toString() : null;
    }

    /**
     * Verify that rule blocks exist with the specified severity values.
     *
     * @param expectedSeverities list of severities to verify (ERROR, WARNING, INFO)
     */
    protected void verifySeverityValues(List<String> expectedSeverities) {
        for (String severity : expectedSeverities) {
            Object found = js.executeScript(
                "const blocks = Blockly.getMainWorkspace().getAllBlocks(false)" +
                ".filter(b => b.type === 'apex_rule');" +
                "for (let block of blocks) {" +
                "  const field = block.getField('SEVERITY');" +
                "  if (field && field.getValue() === '" + severity + "') return true;" +
                "}" +
                "return false;"
            );
            assertTrue(Boolean.TRUE.equals(found), "Should have a rule with severity: " + severity);
        }
    }

    /**
     * Count the number of nested blocks within a parent block's input.
     *
     * @param parentBlockType the parent block type
     * @param inputName the name of the input containing nested blocks
     * @return the count of nested blocks
     */
    protected int countNestedBlocks(String parentBlockType, String inputName) {
        Object count = js.executeScript(
            "const parentBlocks = Blockly.getMainWorkspace().getAllBlocks(false)" +
            ".filter(b => b.type === '" + parentBlockType + "');" +
            "if (parentBlocks.length === 0) return 0;" +
            "const input = parentBlocks[0].getInput('" + inputName + "');" +
            "if (!input || !input.connection) return 0;" +
            "let nested = 0;" +
            "let current = input.connection.targetBlock();" +
            "while (current) {" +
            "  nested++;" +
            "  current = current.getNextBlock();" +
            "}" +
            "return nested;"
        );
        return count != null ? ((Number) count).intValue() : 0;
    }

    /**
     * Load YAML content from a file in the examples directory.
     *
     * @param relativePath the relative path from the examples directory
     * @return the YAML file contents as a string
     * @throws IOException if the file cannot be read
     */
    protected String loadYamlFile(String relativePath) throws IOException {
        // Load from project examples directory
        Path examplesPath = Path.of("examples").resolve(relativePath.replace("examples/", ""));
        
        if (!Files.exists(examplesPath)) {
            // Try absolute path from project root
            Path projectRoot = Path.of(System.getProperty("user.dir")).getParent();
            examplesPath = projectRoot.resolve("apex-playground").resolve(relativePath);
        }
        
        assertTrue(Files.exists(examplesPath), "Test YAML file should exist: " + examplesPath);
        return Files.readString(examplesPath);
    }

    // =============== Enhanced Helper Methods ===============

    /**
     * Verify nested block structure with expected counts for each block type.
     *
     * @param parentType the parent block type
     * @param expectedCounts map of input name to expected block count
     */
    protected void verifyNestedBlockStructure(String parentType, Map<String, Integer> expectedCounts) {
        for (Map.Entry<String, Integer> entry : expectedCounts.entrySet()) {
            String inputName = entry.getKey();
            int expectedCount = entry.getValue();
            int actualCount = countNestedBlocks(parentType, inputName);
            assertEquals(expectedCount, actualCount, 
                "Parent block '" + parentType + "' input '" + inputName + "' should have " + 
                expectedCount + " nested blocks (found: " + actualCount + ")");
        }
    }

    /**
     * Verify multiple field values on a block type in a single call.
     *
     * @param blockType the block type to check
     * @param expectedFields map of field name to expected value
     */
    protected void verifyBlockFieldValues(String blockType, Map<String, String> expectedFields) {
        for (Map.Entry<String, String> entry : expectedFields.entrySet()) {
            String fieldName = entry.getKey();
            String expectedValue = entry.getValue();
            String actualValue = getBlockFieldValue(blockType, fieldName);
            assertEquals(expectedValue, actualValue, 
                "Block '" + blockType + "' field '" + fieldName + "' should be '" + 
                expectedValue + "' (found: '" + actualValue + "')");
        }
    }

    /**
     * Get all blocks of a specific type for iteration.
     *
     * @param blockType the block type to retrieve
     * @return list of block IDs
     */
    @SuppressWarnings("unchecked")
    protected List<String> getBlocksByType(String blockType) {
        Object result = js.executeScript(
            "return Blockly.getMainWorkspace().getAllBlocks(false)" +
            ".filter(b => b.type === '" + blockType + "')" +
            ".map(b => b.id);"
        );
        return result != null ? (List<String>) result : List.of();
    }

    /**
     * Verify that blocks appear in the expected order in the workspace.
     *
     * @param expectedBlockTypes list of block types in expected order
     */
    protected void verifyBlockOrder(List<String> expectedBlockTypes) {
        Object result = js.executeScript(
            "return Blockly.getMainWorkspace().getTopBlocks(false).map(b => b.type);"
        );
        
        @SuppressWarnings("unchecked")
        List<String> actualOrder = result != null ? (List<String>) result : List.of();
        
        assertEquals(expectedBlockTypes.size(), actualOrder.size(), 
            "Block count mismatch. Expected: " + expectedBlockTypes + ", Found: " + actualOrder);
        
        for (int i = 0; i < expectedBlockTypes.size(); i++) {
            assertEquals(expectedBlockTypes.get(i), actualOrder.get(i), 
                "Block at position " + i + " should be '" + expectedBlockTypes.get(i) + 
                "' but found '" + actualOrder.get(i) + "'");
        }
    }

    /**
     * Verify that exported YAML contains all required sections.
     *
     * @param yaml the YAML content to validate
     * @param requiredSections list of section names that must be present
     */
    protected void verifyYamlStructure(String yaml, List<String> requiredSections) {
        assertNotNull(yaml, "YAML should not be null");
        assertFalse(yaml.isEmpty(), "YAML should not be empty");
        
        for (String section : requiredSections) {
            assertTrue(yaml.contains(section + ":"), 
                "YAML should contain section '" + section + "'");
        }
    }

    /**
     * Get the WebDriver instance for direct access in subclasses.
     *
     * @return the WebDriver instance
     */
    protected WebDriver getDriver() {
        return driver;
    }
}
