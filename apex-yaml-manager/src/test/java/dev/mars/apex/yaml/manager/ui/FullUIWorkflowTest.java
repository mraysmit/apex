package dev.mars.apex.yaml.manager.ui;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the EXACT UI workflow that the user experiences:
 * 1. Click "Load Folder"
 * 2. Enter folder path
 * 3. Click "Scan Folder"
 * 4. Select a file
 * 5. Click "Load Selected"
 * 6. Verify dependency tree renders with dependencies
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullUIWorkflowTest {

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
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private String testFolderPath() {
        return Paths.get(
            System.getProperty("user.dir"),
            "src", "test", "resources", "apex-yaml-samples", "graph-100"
        ).toAbsolutePath().toString();
    }

    @Test
    @Order(1)
    void testFullUIWorkflow_LoadFolder_ScanFiles_SelectFile_LoadTree() {
        System.out.println("🧪 Testing FULL UI Workflow...");
        
        // Step 1: Load the page
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        System.out.println("✅ Page loaded");

        // Step 2: Click "Load Folder" button
        WebElement loadFolderBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn"))
        );
        loadFolderBtn.click();
        System.out.println("✅ Clicked Load Folder button");

        // Step 3: Wait for modal to appear
        WebElement modal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("folderModal"))
        );
        assertTrue(modal.getAttribute("class").contains("active"), "Modal should be active");
        System.out.println("✅ Modal opened");

        // Step 4: Enter folder path
        WebElement folderPathInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderPathInput"))
        );
        String testPath = testFolderPath();
        folderPathInput.clear();
        folderPathInput.sendKeys(testPath);
        System.out.println("✅ Entered folder path: " + testPath);

        // Step 5: Click "Scan Folder"
        WebElement scanBtn = driver.findElement(By.id("scanFolderBtn"));
        scanBtn.click();
        System.out.println("✅ Clicked Scan Folder button");

        // Step 6: Wait for files to be scanned and displayed
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("file-item")));
        List<WebElement> fileItems = driver.findElements(By.className("file-item"));
        assertTrue(fileItems.size() > 0, "Should find YAML files after scan");
        System.out.println("✅ Found " + fileItems.size() + " files");

        // Step 7: Verify first file is selected by default
        WebElement firstFileCheckbox = fileItems.get(0).findElement(By.cssSelector("input[type='checkbox']"));
        assertTrue(firstFileCheckbox.isSelected(), "First file should be selected by default");
        
        // Get the file name for verification
        String firstFileName = fileItems.get(0).findElement(By.className("file-item-name")).getText();
        System.out.println("✅ First file selected: " + firstFileName);

        // Step 8: Click "Load Selected"
        WebElement loadSelectedBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("loadSelectedBtn"))
        );
        loadSelectedBtn.click();
        System.out.println("✅ Clicked Load Selected button");

        // Step 9: Wait for modal to close
        wait.until(ExpectedConditions.invisibilityOf(modal));
        System.out.println("✅ Modal closed");

        // Step 10: Wait for dependency tree to render
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("tree-node")));
        List<WebElement> treeNodes = driver.findElements(By.className("tree-node"));
        
        System.out.println("🌳 Tree nodes found: " + treeNodes.size());
        
        // Verify tree nodes exist
        assertTrue(treeNodes.size() > 0, "Dependency tree should render nodes");
        
        // Verify the tree contains dependencies (more than just the root node)
        if (treeNodes.size() == 1) {
            System.out.println("⚠️  WARNING: Only 1 tree node found - this might indicate no dependencies are showing");
            
            // Check if the single node has children or dependencies
            WebElement treeView = driver.findElement(By.id("treeView"));
            String treeHTML = treeView.getAttribute("innerHTML");
            System.out.println("Tree HTML (first 500 chars): " + treeHTML.substring(0, Math.min(500, treeHTML.length())));
            
            // This is the issue the user is experiencing!
            fail("Only 1 tree node found - dependencies are not showing in the UI!");
        } else {
            System.out.println("✅ Multiple tree nodes found - dependencies are showing!");
        }

        // Step 11: Verify tree node content
        WebElement firstTreeNode = treeNodes.get(0);
        String nodeText = firstTreeNode.getText();
        assertFalse(nodeText.isEmpty(), "Tree node should have text content");
        System.out.println("✅ First tree node text: " + nodeText);

        // Step 12: Click on first tree node to test detail panel
        firstTreeNode.click();
        
        // Wait for detail panel to update
        WebElement nodeDetails = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nodeDetails")));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("nodeDetails"), "File Path"));
        
        String detailsText = nodeDetails.getText();
        assertTrue(detailsText.contains("File Path"), "Details panel should show file path");
        System.out.println("✅ Details panel updated with node information");

        System.out.println("🎉 FULL UI WORKFLOW TEST COMPLETE!");
    }
}
