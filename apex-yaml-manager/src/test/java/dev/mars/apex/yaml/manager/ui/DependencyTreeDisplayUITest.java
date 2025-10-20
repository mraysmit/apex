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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for Dependency Tree Display.
 *
 * Tests the tree display UI structure and rendering after loading files.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-19
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DependencyTreeDisplayUITest {

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
    void testTreeViewContainerExists() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement treeView = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("treeView"))
        );
        
        assertTrue(treeView.isDisplayed(), "Tree view container should be visible");
    }

    @Test
    @Order(2)
    void testEmptyStateDisplaysOnPageLoad() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement treeView = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("treeView"))
        );
        
        // Should show empty state message
        String text = treeView.getText();
        assertTrue(text.contains("Load Folder") || text.contains("dependency tree"), 
            "Should display empty state message on page load");
    }

    @Test
    @Order(3)
    void testDetailsPanelExists() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        WebElement detailsPanel = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("nodeDetails"))
        );

        assertTrue(detailsPanel.isDisplayed(), "Details panel should be visible");
    }

    @Test
    @Order(4)
    void testToolbarButtonsExist() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Check for toolbar buttons
        WebElement expandAllBtn = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("expandAllBtn"))
        );
        WebElement collapseAllBtn = driver.findElement(By.id("collapseAllBtn"));
        WebElement refreshBtn = driver.findElement(By.id("refreshBtn"));
        WebElement searchInput = driver.findElement(By.id("searchInput"));
        
        assertTrue(expandAllBtn.isDisplayed(), "Expand All button should be visible");
        assertTrue(collapseAllBtn.isDisplayed(), "Collapse All button should be visible");
        assertTrue(refreshBtn.isDisplayed(), "Refresh button should be visible");
        assertTrue(searchInput.isDisplayed(), "Search input should be visible");
    }

    @Test
    @Order(5)
    void testLeftPanelExists() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement leftPanel = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.className("left-panel"))
        );
        
        assertTrue(leftPanel.isDisplayed(), "Left panel should be visible");
    }

    @Test
    @Order(6)
    void testRightPanelExists() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement rightPanel = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.className("right-panel"))
        );
        
        assertTrue(rightPanel.isDisplayed(), "Right panel should be visible");
    }

    @Test
    @Order(7)
    void testDividerExists() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement divider = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("divider"))
        );
        
        assertTrue(divider.isDisplayed(), "Divider should be visible");
    }

    @Test
    @Order(8)
    void testHeaderExists() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement header = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.className("header"))
        );
        
        assertTrue(header.isDisplayed(), "Header should be visible");
        assertTrue(header.getText().contains("APEX"), "Header should contain APEX");
    }

    @Test
    @Order(9)
    void testLoadFolderButtonExists() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("loadFolderBtn"))
        );
        
        assertTrue(loadFolderBtn.isDisplayed(), "Load Folder button should be visible");
    }

    @Test
    @Order(10)
    void testTreeDisplayUIStructure() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        
        // Verify main container structure
        WebElement mainContainer = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.className("main-container"))
        );
        
        List<WebElement> panels = mainContainer.findElements(By.cssSelector(".left-panel, .right-panel"));
        assertEquals(2, panels.size(), "Should have left and right panels");
    }
}

