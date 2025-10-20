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
 * Selenium UI test for header navigation buttons and cross-page functionality.
 * 
 * Tests all header navigation buttons including Load Folder button and 
 * navigation links to other pages (Catalog, Validation, Health Checks).
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-20
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HeaderNavigationButtonsUITest {

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
    void test_header_load_folder_button() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Find header Load Folder button (Bootstrap button with warning style)
        WebElement headerLoadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class, 'btn-warning') and contains(text(), 'Load Folder')]"))
        );
        
        // Click header Load Folder button
        headerLoadFolderBtn.click();
        
        // Verify modal opens
        WebElement modal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("folderModal"))
        );
        
        assertTrue(modal.isDisplayed(), "Modal should open when header Load Folder button is clicked");
        
        // Close modal
        WebElement closeBtn = driver.findElement(By.id("modalCloseBtn"));
        closeBtn.click();
        
        wait.until(ExpectedConditions.invisibilityOf(modal));
    }

    @Test
    @Order(2)
    void test_catalog_browser_navigation() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Find Catalog Browser link
        WebElement catalogLink = wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@href, '/ui/catalog')]"))
        );
        
        // Click Catalog Browser link
        catalogLink.click();
        
        // Wait for page to load and verify URL
        wait.until(driver -> driver.getCurrentUrl().contains("/ui/catalog"));
        
        assertTrue(driver.getCurrentUrl().contains("/ui/catalog"), 
                   "Should navigate to catalog page");
        
        // Verify page title or content
        WebElement pageContent = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.tagName("body"))
        );
        
        assertNotNull(pageContent, "Catalog page should load");
    }

    @Test
    @Order(3)
    void test_validation_navigation() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Find Validation link
        WebElement validationLink = wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@href, '/ui/validation')]"))
        );
        
        // Click Validation link
        validationLink.click();
        
        // Wait for page to load and verify URL
        wait.until(driver -> driver.getCurrentUrl().contains("/ui/validation"));
        
        assertTrue(driver.getCurrentUrl().contains("/ui/validation"), 
                   "Should navigate to validation page");
        
        // Verify page loads
        WebElement pageContent = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.tagName("body"))
        );
        
        assertNotNull(pageContent, "Validation page should load");
    }

    @Test
    @Order(4)
    void test_health_checks_navigation() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Find Health Checks link
        WebElement healthLink = wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@href, '/ui/health')]"))
        );
        
        // Click Health Checks link
        healthLink.click();
        
        // Wait for page to load and verify URL
        wait.until(driver -> driver.getCurrentUrl().contains("/ui/health"));
        
        assertTrue(driver.getCurrentUrl().contains("/ui/health"), 
                   "Should navigate to health checks page");
        
        // Verify page loads
        WebElement pageContent = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.tagName("body"))
        );
        
        assertNotNull(pageContent, "Health checks page should load");
    }

    @Test
    @Order(5)
    void test_dependency_tree_viewer_active_state() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Find Dependency Tree Viewer link (should be active)
        WebElement treeViewerLink = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@href, '/ui/tree-viewer')]"))
        );
        
        // Verify it has active class
        String classes = treeViewerLink.getAttribute("class");
        assertTrue(classes.contains("active"), 
                   "Dependency Tree Viewer link should have active class when on tree viewer page");
    }

    @Test
    @Order(6)
    void test_folder_badge_functionality() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Find folder badge (initially hidden)
        WebElement folderBadge = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("loadedFolderBadge"))
        );
        
        // Initially should be hidden
        assertFalse(folderBadge.isDisplayed(), "Folder badge should be hidden initially");
        
        // Load a folder to make badge visible
        WebElement loadFolderBtn = driver.findElement(By.id("loadFolderBtn"));
        loadFolderBtn.click();
        
        WebElement folderPathInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderPathInput"))
        );
        folderPathInput.sendKeys("C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-core/src/test/resources");
        
        WebElement scanBtn = driver.findElement(By.id("scanFolderBtn"));
        scanBtn.click();
        
        // Wait for scan to complete
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("file-item")));
        
        WebElement loadSelectedBtn = driver.findElement(By.id("loadSelectedBtn"));
        loadSelectedBtn.click();
        
        // Wait for badge to become visible
        wait.until(ExpectedConditions.visibilityOf(folderBadge));
        
        assertTrue(folderBadge.isDisplayed(), "Folder badge should be visible after loading folder");
        assertTrue(folderBadge.getText().contains("Folder:"), "Badge should show folder information");
    }

    @Test
    @Order(7)
    void test_navigation_preserves_folder_state() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Load a folder first
        WebElement loadFolderBtn = driver.findElement(By.id("loadFolderBtn"));
        loadFolderBtn.click();
        
        WebElement folderPathInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderPathInput"))
        );
        folderPathInput.sendKeys("C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-core/src/test/resources");
        
        WebElement scanBtn = driver.findElement(By.id("scanFolderBtn"));
        scanBtn.click();
        
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("file-item")));
        
        WebElement loadSelectedBtn = driver.findElement(By.id("loadSelectedBtn"));
        loadSelectedBtn.click();
        
        // Wait for folder badge to appear
        WebElement folderBadge = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("loadedFolderBadge"))
        );
        
        String originalBadgeText = folderBadge.getText();
        
        // Navigate to catalog page
        WebElement catalogLink = driver.findElement(By.xpath("//a[contains(@href, '/ui/catalog')]"));
        catalogLink.click();
        
        wait.until(driver -> driver.getCurrentUrl().contains("/ui/catalog"));
        
        // Navigate back to tree viewer
        WebElement treeViewerLink = wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@href, '/ui/tree-viewer')]"))
        );
        treeViewerLink.click();
        
        wait.until(driver -> driver.getCurrentUrl().contains("/ui/tree-viewer"));
        
        // Verify folder badge is still there
        WebElement restoredBadge = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("loadedFolderBadge"))
        );
        
        assertEquals(originalBadgeText, restoredBadge.getText(), 
                     "Folder badge should preserve state across navigation");
    }
}
