package dev.mars.apex.yaml.manager.ui;

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

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.File;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI test for file dialog selection functionality using sendKeys().
 * 
 * Tests the file system dialog interaction by directly sending file paths
 * to the hidden file input element, simulating user file selection.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-21
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileDialogSelectionUITest {

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
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private String getTestYamlFilePath() {
        String userDir = System.getProperty("user.dir");
        return userDir + File.separator + "src" + File.separator + "test" + File.separator + 
               "resources" + File.separator + "apex-yaml-samples" + File.separator + 
               "graph-200" + File.separator + "000-master-registry.yaml";
    }

    private String getMultipleTestYamlFiles() {
        String userDir = System.getProperty("user.dir");
        String basePath = userDir + File.separator + "src" + File.separator + "test" + File.separator + 
                         "resources" + File.separator + "apex-yaml-samples" + File.separator + "graph-200";
        
        // Return multiple files separated by newlines (Chrome format)
        return basePath + File.separator + "000-master-registry.yaml\n" +
               basePath + File.separator + "010-equity-trading.yaml\n" +
               basePath + File.separator + "011-fixed-income.yaml";
    }

    @Test
    @Order(1)
    void test_single_file_selection_via_sendkeys() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Open folder modal
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        // Click browse button to activate file input
        WebElement browseBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("browseFolderBtn"))
        );
        browseBtn.click();
        
        // Find hidden file input and send file path
        WebElement fileInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderInput"))
        );
        
        String testFilePath = getTestYamlFilePath();
        fileInput.sendKeys(testFilePath);
        
        // Wait for file processing
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Verify folder path is populated
        WebElement folderPathInput = driver.findElement(By.id("folderPathInput"));
        String folderPath = folderPathInput.getAttribute("value");

        assertTrue(!folderPath.isEmpty(), "Folder path should be populated after file selection");
        assertTrue(folderPath.contains("graph-200") || folderPath.contains("Selected Folder"),
                   "Folder path should reference the test directory");
        
        // Verify file list shows the selected file
        List<WebElement> fileItems = driver.findElements(By.className("file-item"));
        assertTrue(fileItems.size() > 0, "File list should contain the selected file");
        
        // Verify Load Selected button is enabled
        WebElement loadSelectedBtn = driver.findElement(By.id("loadSelectedBtn"));
        assertTrue(loadSelectedBtn.isEnabled(), "Load Selected button should be enabled");
    }

    @Test
    @Order(2)
    void test_multiple_file_selection_via_sendkeys() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Open folder modal
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        // Click browse button
        WebElement browseBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("browseFolderBtn"))
        );
        browseBtn.click();
        
        // Send multiple file paths
        WebElement fileInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderInput"))
        );
        
        String multipleFiles = getMultipleTestYamlFiles();
        fileInput.sendKeys(multipleFiles);
        
        // Wait for processing
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Verify multiple files are listed
        List<WebElement> fileItems = driver.findElements(By.className("file-item"));
        assertTrue(fileItems.size() >= 3, "Should have at least 3 files from multiple selection");
        
        // Verify files contain expected names
        boolean foundMasterRegistry = false;
        boolean foundEquityTrading = false;
        
        for (WebElement item : fileItems) {
            String fileName = item.getText();
            if (fileName.contains("000-master-registry")) foundMasterRegistry = true;
            if (fileName.contains("010-equity-trading")) foundEquityTrading = true;
        }
        
        assertTrue(foundMasterRegistry, "Should find master registry file");
        assertTrue(foundEquityTrading, "Should find equity trading file");
    }

    @Test
    @Order(3)
    void test_file_selection_and_load_dependency_tree() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Open folder modal and select files
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        WebElement browseBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("browseFolderBtn"))
        );
        browseBtn.click();
        
        WebElement fileInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderInput"))
        );
        
        String testFilePath = getTestYamlFilePath();
        fileInput.sendKeys(testFilePath);
        
        // Wait for file processing
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Click Load Selected to load the dependency tree
        WebElement loadSelectedBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadSelectedBtn"))
        );
        loadSelectedBtn.click();
        
        // Wait for modal to close and tree to load
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("folderModal")));
        
        // Wait for tree nodes to appear
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        
        // Verify tree loaded successfully
        List<WebElement> treeNodes = driver.findElements(By.className("tree-node"));
        assertTrue(treeNodes.size() > 0, "Dependency tree should load with nodes");
        
        // Verify root node is present
        boolean foundRootNode = treeNodes.stream()
            .anyMatch(node -> node.getText().contains("000-master-registry"));
        
        assertTrue(foundRootNode, "Should find the root registry node in the tree");
        
        // Verify folder badge appears
        WebElement folderBadge = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("loadedFolderBadge"))
        );
        
        assertTrue(folderBadge.isDisplayed(), "Folder badge should be visible after loading");
        assertTrue(folderBadge.getText().contains("Folder:"), "Badge should show folder information");
    }

    @Test
    @Order(4)
    void test_invalid_file_path_handling() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Open folder modal
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        // Click browse button
        WebElement browseBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("browseFolderBtn"))
        );
        browseBtn.click();
        
        // Send invalid file path
        WebElement fileInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderInput"))
        );
        
        String invalidPath = "C:\\nonexistent\\path\\invalid.yaml";
        fileInput.sendKeys(invalidPath);
        
        // Wait for processing
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Verify Load Selected button remains disabled or shows error
        WebElement loadSelectedBtn = driver.findElement(By.id("loadSelectedBtn"));
        
        // Either button is disabled OR there's an error message
        boolean hasError = false;
        try {
            WebElement errorMsg = driver.findElement(By.className("error-message"));
            hasError = errorMsg.isDisplayed();
        } catch (Exception e) {
            // No error message element found
        }
        
        assertTrue(!loadSelectedBtn.isEnabled() || hasError, 
                   "Should either disable Load Selected button or show error for invalid path");
    }

    @Test
    @Order(5)
    void test_file_input_attributes() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Open folder modal
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        // Find the hidden file input
        WebElement fileInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderInput"))
        );
        
        // Verify file input attributes
        assertEquals("file", fileInput.getAttribute("type"), "Input should be file type");
        assertNotNull(fileInput.getAttribute("webkitdirectory"), "Should have webkitdirectory attribute");
        assertNotNull(fileInput.getAttribute("directory"), "Should have directory attribute");

        // Verify it's initially hidden
        assertFalse(fileInput.isDisplayed(), "File input should be hidden initially");
    }
}
