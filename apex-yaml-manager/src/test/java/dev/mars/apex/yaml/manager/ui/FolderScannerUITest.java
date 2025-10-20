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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for Folder Scanner functionality.
 *
 * Tests file selection, deselection, and loading workflows.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-19
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FolderScannerUITest {

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
    void testEnterPathWithEnterKey() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        WebElement folderPathInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderPathInput"))
        );
        folderPathInput.sendKeys("C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-core/src/test/resources");
        folderPathInput.sendKeys(Keys.RETURN);
        
        // Wait for file list to appear
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("file-item")));
        
        var fileItems = driver.findElements(By.className("file-item"));
        assertTrue(fileItems.size() > 0, "Should find YAML files when pressing Enter");
    }

    @Test
    @Order(2)
    void testFileItemClickToggleCheckbox() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();

        WebElement folderPathInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderPathInput"))
        );
        folderPathInput.sendKeys("C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-core/src/test/resources");

        WebElement scanBtn = driver.findElement(By.id("scanFolderBtn"));
        scanBtn.click();

        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("file-item")));

        var fileItems = driver.findElements(By.className("file-item"));
        WebElement firstFileItem = fileItems.get(0);
        WebElement checkbox = firstFileItem.findElement(By.cssSelector("input[type='checkbox']"));

        boolean initialState = checkbox.isSelected();
        firstFileItem.click();

        // Wait a bit for the click to process
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        checkbox = driver.findElements(By.className("file-item")).get(0)
            .findElement(By.cssSelector("input[type='checkbox']"));

        assertNotEquals(initialState, checkbox.isSelected(),
            "Checkbox state should toggle when file item is clicked");
    }

    @Test
    @Order(3)
    void testLoadSelectedButtonDisabledWhenNoSelection() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();

        WebElement folderPathInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderPathInput"))
        );
        folderPathInput.sendKeys("C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-core/src/test/resources");

        WebElement scanBtn = driver.findElement(By.id("scanFolderBtn"));
        scanBtn.click();

        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("file-item")));

        var fileItems = driver.findElements(By.className("file-item"));
        for (WebElement item : fileItems) {
            WebElement checkbox = item.findElement(By.cssSelector("input[type='checkbox']"));
            if (checkbox.isSelected()) {
                item.click();
                try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }

        WebElement loadSelectedBtn = driver.findElement(By.id("loadSelectedBtn"));
        assertTrue(loadSelectedBtn.getAttribute("disabled") != null &&
                  loadSelectedBtn.getAttribute("disabled").equals("true"),
            "Load Selected button should be disabled when no files are selected");
    }

    @Test
    @Order(4)
    void testFileCountDisplay() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        WebElement folderPathInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderPathInput"))
        );
        folderPathInput.sendKeys("C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-core/src/test/resources");
        
        WebElement scanBtn = driver.findElement(By.id("scanFolderBtn"));
        scanBtn.click();
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("fileCount")));
        
        WebElement fileCountElement = driver.findElement(By.id("fileCount"));
        String fileCount = fileCountElement.getText();
        
        assertTrue(!fileCount.isEmpty() && !fileCount.equals("0"), 
            "File count should be displayed and greater than 0");
    }

    @Test
    @Order(5)
    void testCancelButtonClosesModal() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        WebElement modal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("folderModal"))
        );
        
        WebElement cancelBtn = driver.findElement(By.id("modalCancelBtn"));
        cancelBtn.click();

        wait.until(ExpectedConditions.invisibilityOf(modal));
        assertFalse(modal.getAttribute("class").contains("active"),
            "Modal should be closed after cancel");
    }

    @Test
    @Order(6)
    void testInvalidFolderPathError() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        WebElement folderPathInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderPathInput"))
        );
        folderPathInput.sendKeys("C:/nonexistent/path/that/does/not/exist");
        
        WebElement scanBtn = driver.findElement(By.id("scanFolderBtn"));
        scanBtn.click();
        
        WebElement scanStatus = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("scanStatus"))
        );
        
        assertTrue(scanStatus.getText().contains("Error") || 
                  scanStatus.getText().contains("error"),
            "Error message should be displayed for invalid path");
    }

    @Test
    @Order(7)
    void testEmptyFolderPathError() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        WebElement scanBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("scanFolderBtn"))
        );
        scanBtn.click();
        
        WebElement scanStatus = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("scanStatus"))
        );
        
        assertTrue(scanStatus.getText().contains("Please enter") ||
                  scanStatus.getText().contains("required"),
            "Error message should be displayed for empty path");
    }

    @Test
    @Order(8)
    void test_browse_folder_button() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();

        // Find browse button
        WebElement browseBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("browseFolderBtn"))
        );

        // Click browse button
        browseBtn.click();

        // Verify button is clickable and functional
        assertTrue(browseBtn.isEnabled(), "Browse button should be enabled");

        // Note: File dialog interaction is browser-dependent and hard to test
        // This test verifies the button is present and clickable
    }

    @Test
    @Order(9)
    void test_modal_close_button() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();

        // Verify modal is open
        WebElement modal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("folderModal"))
        );

        // Find and click close button (X)
        WebElement closeBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("modalCloseBtn"))
        );
        closeBtn.click();

        // Wait for modal to close
        wait.until(ExpectedConditions.invisibilityOf(modal));

        // Verify modal is closed
        assertFalse(modal.getAttribute("class").contains("active"),
            "Modal should be closed after clicking X button");
    }
}

