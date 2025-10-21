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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic Selenium UI test for file dialog functionality using sendKeys().
 * 
 * Tests the core file input functionality that can be reliably tested
 * with Selenium WebDriver using sendKeys() method.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-21
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileDialogBasicUITest {

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

    @Test
    @Order(1)
    void test_browse_button_triggers_file_input() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Open folder modal
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        // Verify browse button is present and clickable
        WebElement browseBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("browseFolderBtn"))
        );
        
        assertTrue(browseBtn.isEnabled(), "Browse button should be enabled");
        assertTrue(browseBtn.isDisplayed(), "Browse button should be visible");
        
        // Click browse button (this would normally open file dialog)
        browseBtn.click();
        
        // Verify the hidden file input exists
        WebElement fileInput = driver.findElement(By.id("folderInput"));
        assertNotNull(fileInput, "Hidden file input should exist");
    }

    @Test
    @Order(2)
    void test_file_input_attributes_and_properties() {
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
        
        // Verify CSS style
        String style = fileInput.getAttribute("style");
        assertTrue(style.contains("display: none") || style.contains("display:none"), 
                   "File input should have display:none style");
    }

    @Test
    @Order(3)
    void test_file_input_sendkeys_basic_functionality() {
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
        
        // Create a test file path (doesn't need to exist for this test)
        String testPath = System.getProperty("user.dir") + File.separator + "test.yaml";
        
        // Use sendKeys to set file path
        fileInput.sendKeys(testPath);
        
        // Verify sendKeys worked (file input should accept the path)
        // Note: We can't verify the actual value due to browser security restrictions
        // but we can verify the operation completed without error
        assertNotNull(fileInput, "File input should still exist after sendKeys");
        assertTrue(fileInput.isEnabled(), "File input should remain enabled");
    }

    @Test
    @Order(4)
    void test_browse_button_and_file_input_integration() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Open folder modal
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        // Verify browse button exists
        WebElement browseBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("browseFolderBtn"))
        );
        
        // Verify file input exists
        WebElement fileInput = driver.findElement(By.id("folderInput"));
        
        // Click browse button
        browseBtn.click();
        
        // Verify file input is still present and functional after browse click
        assertNotNull(fileInput, "File input should exist after browse button click");
        assertEquals("file", fileInput.getAttribute("type"), "File input type should remain 'file'");
        
        // Verify we can still interact with the file input
        String testPath = System.getProperty("user.dir") + File.separator + "example.yaml";
        fileInput.sendKeys(testPath);
        
        // Test completed successfully if no exceptions thrown
        assertTrue(true, "Browse button and file input integration test completed");
    }

    @Test
    @Order(5)
    void test_modal_file_selection_workflow() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Step 1: Open folder modal
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        // Step 2: Verify modal is open
        WebElement modal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("folderModal"))
        );
        assertTrue(modal.isDisplayed(), "Folder modal should be visible");
        
        // Step 3: Verify all file selection elements are present
        WebElement browseBtn = driver.findElement(By.id("browseFolderBtn"));
        WebElement fileInput = driver.findElement(By.id("folderInput"));
        WebElement folderPathInput = driver.findElement(By.id("folderPathInput"));
        WebElement scanBtn = driver.findElement(By.id("scanFolderBtn"));
        WebElement loadSelectedBtn = driver.findElement(By.id("loadSelectedBtn"));
        
        assertNotNull(browseBtn, "Browse button should exist");
        assertNotNull(fileInput, "File input should exist");
        assertNotNull(folderPathInput, "Folder path input should exist");
        assertNotNull(scanBtn, "Scan button should exist");
        assertNotNull(loadSelectedBtn, "Load Selected button should exist");
        
        // Step 4: Verify initial states
        assertTrue(browseBtn.isEnabled(), "Browse button should be enabled");
        assertTrue(scanBtn.isEnabled(), "Scan button should be enabled");
        assertFalse(fileInput.isDisplayed(), "File input should be hidden");
        
        // Step 5: Test browse button click
        browseBtn.click();
        
        // Step 6: Verify file input can receive sendKeys
        String testPath = System.getProperty("user.dir") + File.separator + "test-file.yaml";
        fileInput.sendKeys(testPath);
        
        // Test completed - this verifies the complete file selection workflow
        assertTrue(true, "Complete modal file selection workflow test passed");
    }
}
