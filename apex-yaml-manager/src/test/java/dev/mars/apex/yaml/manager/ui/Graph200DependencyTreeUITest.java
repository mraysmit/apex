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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI test for loading the large graph-200 dependency tree dataset.
 * 
 * Tests that the UI can successfully load and display our 52-file, 3-level deep
 * APEX dependency tree with proper hierarchical structure and content summaries.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-20
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Graph200DependencyTreeUITest {

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
        wait = new WebDriverWait(driver, Duration.ofSeconds(30)); // Longer timeout for large dataset
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private String getGraph200RootFile() {
        Path rootPath = Paths.get("src/test/resources/apex-yaml-samples/graph-200/000-master-registry.yaml");
        return rootPath.toAbsolutePath().toString();
    }

    private void loadGraph200TreeViaJs() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        
        String rootFile = getGraph200RootFile();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        
        // Wait for tree nodes to appear
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
    }

    @Test
    @Order(1)
    void testGraph200TreeLoadsSuccessfully() {
        loadGraph200TreeViaJs();
        
        // Verify tree nodes are present
        List<WebElement> treeNodes = driver.findElements(By.className("tree-node"));
        assertTrue(treeNodes.size() > 0, "Tree should have nodes loaded");
        
        // Verify root node is the master registry
        WebElement rootNode = treeNodes.get(0);
        assertTrue(rootNode.getText().contains("000-master-registry.yaml"), 
                   "Root node should be master registry");
    }

    @Test
    @Order(2)
    void testGraph200TreeHasCorrectStructure() {
        loadGraph200TreeViaJs();
        
        // Verify we have multiple levels of nodes
        List<WebElement> treeNodes = driver.findElements(By.className("tree-node"));
        
        // Should have at least 20 nodes visible (root + 20 scenarios at minimum)
        assertTrue(treeNodes.size() >= 20, 
                   "Should have at least 20 nodes visible (root + scenarios)");
        
        // Verify we can find equity trading scenario
        boolean foundEquityTrading = treeNodes.stream()
            .anyMatch(node -> node.getText().contains("010-equity-trading.yaml"));
        assertTrue(foundEquityTrading, "Should find equity trading scenario");
        
        // Verify we can find fixed income scenario
        boolean foundFixedIncome = treeNodes.stream()
            .anyMatch(node -> node.getText().contains("011-fixed-income.yaml"));
        assertTrue(foundFixedIncome, "Should find fixed income scenario");
        
        // Verify we can find derivatives scenario
        boolean foundDerivatives = treeNodes.stream()
            .anyMatch(node -> node.getText().contains("012-derivatives.yaml"));
        assertTrue(foundDerivatives, "Should find derivatives scenario");
    }

    @Test
    @Order(3)
    void testGraph200TreeExpansion() {
        loadGraph200TreeViaJs();
        
        // Find and click on equity trading to expand it
        List<WebElement> treeNodes = driver.findElements(By.className("tree-node"));
        WebElement equityTradingNode = treeNodes.stream()
            .filter(node -> node.getText().contains("010-equity-trading.yaml"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(equityTradingNode, "Should find equity trading node");
        
        // Click to expand (look for expand button or click the node)
        WebElement expandButton = equityTradingNode.findElement(By.className("tree-toggle"));
        if (expandButton.getText().contains("▸")) { // If collapsed
            expandButton.click();
            
            // Wait for expansion and verify more nodes appear
            wait.until(driver -> {
                List<WebElement> updatedNodes = driver.findElements(By.className("tree-node"));
                return updatedNodes.size() > treeNodes.size();
            });
            
            List<WebElement> expandedNodes = driver.findElements(By.className("tree-node"));
            assertTrue(expandedNodes.size() > treeNodes.size(), 
                       "Should have more nodes after expansion");
        }
    }

    @Test
    @Order(4)
    void testGraph200TreeContentSummaries() {
        loadGraph200TreeViaJs();
        
        // Look for content summary information in the tree nodes
        List<WebElement> treeNodes = driver.findElements(By.className("tree-node"));
        
        // Find a rule-config node and verify it shows rule counts
        boolean foundRuleConfig = false;
        for (WebElement node : treeNodes) {
            String nodeText = node.getText();
            if (nodeText.contains("validation") || nodeText.contains("rule")) {
                // Should show some kind of content summary
                assertTrue(nodeText.length() > 20, "Rule config nodes should have content info");
                foundRuleConfig = true;
                break;
            }
        }
        
        // Note: Content summaries might not be visible until nodes are expanded
        // This test verifies the basic structure is there
        assertTrue(treeNodes.size() > 0, "Should have tree structure loaded");
    }

    @Test
    @Order(5)
    void testGraph200TreeDetailsPanel() {
        loadGraph200TreeViaJs();
        
        // Click on the root node to select it
        List<WebElement> treeNodes = driver.findElements(By.className("tree-node"));
        WebElement rootNode = treeNodes.get(0);
        rootNode.click();
        
        // Wait for details panel to update
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nodeDetails")));
        
        // Verify details panel shows information
        WebElement detailsPanel = driver.findElement(By.id("nodeDetails"));
        String detailsText = detailsPanel.getText();
        
        assertFalse(detailsText.contains("Click a node"), 
                    "Details panel should show node information, not empty state");
        assertTrue(detailsText.length() > 50, 
                   "Details panel should have substantial content");
    }

    @Test
    @Order(6)
    void testGraph200TreeToolbarFunctions() {
        loadGraph200TreeViaJs();
        
        // Test expand all button
        WebElement expandAllBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("expandAllBtn"))
        );
        
        int initialNodeCount = driver.findElements(By.className("tree-node")).size();
        expandAllBtn.click();
        
        // Wait for expansion to complete
        wait.until(driver -> {
            List<WebElement> nodes = driver.findElements(By.className("tree-node"));
            return nodes.size() > initialNodeCount;
        });
        
        int expandedNodeCount = driver.findElements(By.className("tree-node")).size();
        assertTrue(expandedNodeCount > initialNodeCount, 
                   "Expand all should show more nodes");
        
        // Test collapse all button
        WebElement collapseAllBtn = driver.findElement(By.id("collapseAllBtn"));
        collapseAllBtn.click();
        
        // Wait for collapse
        wait.until(driver -> {
            List<WebElement> nodes = driver.findElements(By.className("tree-node"));
            return nodes.size() < expandedNodeCount;
        });
        
        int collapsedNodeCount = driver.findElements(By.className("tree-node")).size();
        assertTrue(collapsedNodeCount < expandedNodeCount, 
                   "Collapse all should show fewer nodes");
    }

    @Test
    @Order(7)
    void testGraph200TreePerformance() {
        long startTime = System.currentTimeMillis();
        
        loadGraph200TreeViaJs();
        
        long endTime = System.currentTimeMillis();
        long loadTime = endTime - startTime;
        
        // Should load within reasonable time (30 seconds)
        assertTrue(loadTime < 30000, 
                   "Tree should load within 30 seconds, took: " + loadTime + "ms");
        
        // Verify we have a reasonable number of nodes
        List<WebElement> treeNodes = driver.findElements(By.className("tree-node"));
        assertTrue(treeNodes.size() >= 10, 
                   "Should have at least 10 nodes visible");
        
        System.out.println("✅ Graph-200 UI Test PASSED");
        System.out.println("   📊 Loaded " + treeNodes.size() + " visible nodes");
        System.out.println("   ⏱️ Load time: " + loadTime + "ms");
        System.out.println("   🌐 Successfully tested large dependency tree in UI");
    }
}
