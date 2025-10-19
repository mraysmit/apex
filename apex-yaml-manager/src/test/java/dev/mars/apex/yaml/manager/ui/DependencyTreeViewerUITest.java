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

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for Dependency Tree Viewer.
 *
 * Tests the folder selector dialog, file scanning, and tree rendering
 * using actual browser automation with Chrome WebDriver.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-19
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DependencyTreeViewerUITest {

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
    void testPageLoads() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("loadFolderBtn")));
        
        String title = driver.getTitle();
        assertTrue(title.contains("APEX YAML"), "Page title should contain APEX YAML");
    }

    @Test
    @Order(2)
    void testLoadFolderButtonExists() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("loadFolderBtn"))
        );
        
        assertTrue(loadFolderBtn.isDisplayed(), "Load Folder button should be visible");
        assertEquals("📂 Load Folder", loadFolderBtn.getText());
    }

    @Test
    @Order(3)
    void testFolderModalOpensOnButtonClick() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        WebElement modal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("folderModal"))
        );
        
        assertTrue(modal.getAttribute("class").contains("active"), 
            "Modal should have active class");
    }

    @Test
    @Order(4)
    void testFolderPathInputField() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        WebElement folderPathInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderPathInput"))
        );
        
        assertTrue(folderPathInput.isDisplayed(), "Folder path input should be visible");
        folderPathInput.sendKeys("C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-core/src/test/resources");
        
        assertEquals("C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-core/src/test/resources", 
            folderPathInput.getAttribute("value"));
    }

    @Test
    @Order(5)
    void testScanFolderButton() {
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

        // Wait for file items to appear
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("file-item")));

        var fileItems = driver.findElements(By.className("file-item"));
        assertTrue(fileItems.size() > 0, "File items should be displayed after scan");
    }

    @Test
    @Order(6)
    void testFileListPopulation() {
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
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("file-item")));
        
        var fileItems = driver.findElements(By.className("file-item"));
        assertTrue(fileItems.size() > 0, "Should find YAML files");
    }

    @Test
    @Order(7)
    void testFileItemCheckboxes() {
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
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("file-item")));
        
        var checkboxes = driver.findElements(By.cssSelector(".file-item input[type='checkbox']"));
        assertTrue(checkboxes.size() > 0, "File items should have checkboxes");
        assertTrue(checkboxes.get(0).isSelected(), "Checkboxes should be selected by default");
    }

    @Test
    @Order(8)
    void testLoadSelectedButtonEnabled() {
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
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("file-item")));
        
        WebElement loadSelectedBtn = driver.findElement(By.id("loadSelectedBtn"));
        assertFalse(loadSelectedBtn.getAttribute("disabled") != null && 
                   loadSelectedBtn.getAttribute("disabled").equals("true"),
            "Load Selected button should be enabled when files are selected");
    }

    @Test
    @Order(9)
    void testModalCloseButton() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        
        WebElement modal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("folderModal"))
        );
        
        WebElement closeBtn = driver.findElement(By.id("modalCloseBtn"));
        closeBtn.click();

        wait.until(ExpectedConditions.invisibilityOf(modal));
        assertFalse(modal.getAttribute("class").contains("active"),
            "Modal should not have active class after close");
    }
}

