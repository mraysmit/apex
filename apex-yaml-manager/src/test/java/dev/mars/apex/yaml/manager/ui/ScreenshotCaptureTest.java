package dev.mars.apex.yaml.manager.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Disabled;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.JavascriptExecutor;

import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Screenshot Capture Test for APEX YAML Manager User Guide.
 *
 * Captures screenshots of various UI states and interactions for documentation.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-19
 * @version 1.0
 */
@Disabled("Disabled: screenshot capture is documentation-only and slows the test cadence. Enable locally when generating docs.")

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScreenshotCaptureTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;
    private String screenshotDir;

    @LocalServerPort
    private int port;


    private String samplesDir() {
        return Paths.get(System.getProperty("user.dir"),
                "apex-yaml-manager", "src", "test", "resources", "apex-yaml-samples")
            .toAbsolutePath().toString();
    }

    private String rootFile() {
        return Paths.get(samplesDir(), "scenario-registry.yaml").toString();
    }

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(45));
        baseUrl = "http://localhost:" + port;

        // Create screenshots directory - use absolute path
        String projectRoot = System.getProperty("user.dir");
        screenshotDir = projectRoot + "/docs/screenshots";
        File dir = new File(screenshotDir);
        dir.mkdirs();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void takeScreenshot(String filename) throws IOException {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        Path destination = Paths.get(screenshotDir, filename);
        Files.copy(screenshot.toPath(), destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Screenshot saved: " + destination.toAbsolutePath());
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @Order(1)
    void captureInitialPageLoad() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("loadFolderBtn")));
        sleep(1000);
        takeScreenshot("01-initial-page-load.png");
    }

    @Test
    @Order(2)
    void captureLoadFolderModal() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("loadFolderBtn")));

        WebElement loadBtn = driver.findElement(By.id("loadFolderBtn"));
        loadBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("folderModal")));
        sleep(500);
        takeScreenshot("02-load-folder-modal.png");
    }

    @Test
    @Order(3)
    void captureToolbar() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("toolbar")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
            "const tb=document.querySelector('.toolbar');" +
            "if(tb){" +
            "  tb.style.outline='3px solid orange'; tb.style.boxShadow='0 0 0 4px rgba(255,165,0,0.3)';" +
            "  const b=document.createElement('span'); b.textContent='Toolbar'; b.style.background='orange'; b.style.color='black'; b.style.padding='2px 6px'; b.style.marginLeft='8px'; b.style.borderRadius='4px'; b.id='tempToolbarBadge'; tb.appendChild(b);" +
            "  tb.scrollIntoView({block:'center'});" +
            "}"
        );
        sleep(500);
        takeScreenshot("03-toolbar-buttons.png");
    }

    @Test
    @Order(4)
    void captureEmptyTreeView() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        sleep(500);
        takeScreenshot("04-empty-tree-view.png");
    }

    @Test
    @Order(5)
    void captureSearchInput() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("searchInput")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("const i=document.getElementById('searchInput'); if(i){ i.focus(); i.value='rules'; i.style.outline='3px solid #1976d2'; i.style.boxShadow='0 0 0 4px rgba(25,118,210,0.25)'; }");
        sleep(500);
        takeScreenshot("05-search-input.png");
    }

    @Test
    @Order(5)
    void captureFileSelectionList() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn")));

        // Click Load Folder button
        WebElement loadBtn = driver.findElement(By.id("loadFolderBtn"));
        loadBtn.click();

        // Wait for modal and enter path
        WebElement folderPathInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("folderPathInput"))
        );
        String dir = samplesDir();
        System.out.println("TEST: Using samplesDir=" + dir);
        folderPathInput.sendKeys(dir);

        // Click Scan button
        WebElement scanBtn = driver.findElement(By.id("scanFolderBtn"));
        scanBtn.click();

        // Prefer a short wait for results, then fall back to client-side population for screenshots
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("file-item")));
        } catch (org.openqa.selenium.TimeoutException te) {
            // Fallback for CI/headless envs: directly populate the list for documentation screenshots
            String p1 = Paths.get(samplesDir(), "scenario-registry.yaml").toString();
            String p2 = Paths.get(samplesDir(), "02-validation-groups.yaml").toString();
            long s1 = 0L, s2 = 0L;
            try { s1 = Files.size(Paths.get(p1)); } catch (Exception ignore) {}
            try { s2 = Files.size(Paths.get(p2)); } catch (Exception ignore) {}

            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                "document.getElementById('folderPathInput').value = arguments[0];" +
                "if (typeof displayScannedFiles === 'function') {" +
                "  displayScannedFiles([" +
                "    {path: arguments[1], name: 'scenario-registry.yaml', size: arguments[3]}," +
                "    {path: arguments[2], name: '02-validation-groups.yaml', size: arguments[4]}" +
                "  ]);" +
                "}" +
                "if (typeof showScanStatus === 'function') { showScanStatus('Found 2 YAML file(s)', 'success'); }",
                dir, p1, p2, s1, s2
            );

            new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("file-item")));
        }

        sleep(500);
        takeScreenshot("05-file-selection-list.png");
    }

    @Test
    @Order(7)
    void captureModalFooter() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("loadFolderBtn")));

        WebElement loadBtn = driver.findElement(By.id("loadFolderBtn"));
        loadBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("folderModal")));
        sleep(500);
        takeScreenshot("06-modal-footer.png");
    }

    @Test
    @Order(8)
    void captureLeftPanel() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("left-panel")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("const lp=document.querySelector('.left-panel'); if(lp){ lp.style.width='50%'; lp.style.transition='none'; lp.style.outline='3px solid #8bc34a'; } const d=document.getElementById('divider'); if(d){ d.scrollIntoView({block:'center'}); }");
        sleep(500);
        takeScreenshot("07-left-panel.png");
    }

    @Test
    @Order(9)
    void captureRightPanel() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("right-panel")));
        sleep(500);
        takeScreenshot("08-right-panel.png");
    }

    @Test
    @Order(10)
    void captureMainContainer() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("main-container")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("const mc=document.querySelector('.main-container'); if(mc){ mc.style.outline='3px solid #4caf50'; mc.style.boxShadow='0 0 0 4px rgba(76,175,80,0.25)'; window.scrollTo(0,0); }");
        sleep(500);
        takeScreenshot("09-main-container.png");
    }

    @Test
    @Order(11)
    void captureHeader() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("header")));
        sleep(500);
        takeScreenshot("10-header.png");
    }
    @Test
    @Order(14)
    void captureTreeTopView() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        // Load a known sample root file
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        // Wait for render
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        // Scroll to top
        js.executeScript("document.getElementById('treeView').scrollTop = 0;");
        sleep(600);
        takeScreenshot("13-tree-top.png");
    }

    @Test
    @Order(15)
    void captureTreeMiddleView() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        // Scroll to middle
        js.executeScript("let el = document.getElementById('treeView'); el.scrollTop = el.scrollHeight/2;");
        sleep(600);
        takeScreenshot("14-tree-middle.png");
    }

    @Test
    @Order(16)
    void captureTreeBottomView() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        // Scroll to bottom
        js.executeScript("let el = document.getElementById('treeView'); el.scrollTop = el.scrollHeight;");
        sleep(600);
        takeScreenshot("15-tree-bottom.png");
    }

    @Test
    @Order(17)
    void captureDetailsForNode1() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        driver.findElements(By.className("tree-node")).get(0).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("nodeName"), "Selected:"));
        sleep(400);
        takeScreenshot("16-details-node-01.png");
    }

    @Test
    @Order(18)
    void captureDetailsForNode2() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        int size2 = driver.findElements(By.className("tree-node")).size();
        int idx2 = Math.max(0, Math.min(1, size2 - 1));
        driver.findElements(By.className("tree-node")).get(idx2).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("nodeName"), "Selected:"));
        ((JavascriptExecutor) driver).executeScript("let d=document.getElementById('nodeDetails'); d.scrollTop=0;");
        sleep(400);
        takeScreenshot("17-details-node-02.png");
    }

    @Test
    @Order(19)
    void captureDetailsForNode3() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        int size3 = driver.findElements(By.className("tree-node")).size();
        int idx3 = Math.max(0, Math.min(2, size3 - 1));
        driver.findElements(By.className("tree-node")).get(idx3).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("nodeName"), "Selected:"));
        ((JavascriptExecutor) driver).executeScript("let d=document.getElementById('nodeDetails'); d.scrollTop=200;");
        sleep(400);
        takeScreenshot("18-details-node-03.png");
    }

    @Test
    @Order(20)
    void captureDetailsForNode4() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        int size4 = driver.findElements(By.className("tree-node")).size();
        int idx4 = Math.max(0, Math.min(3, size4 - 1));
        driver.findElements(By.className("tree-node")).get(idx4).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("nodeName"), "Selected:"));
        ((JavascriptExecutor) driver).executeScript("let d=document.getElementById('nodeDetails'); d.scrollTop=d.scrollHeight/3;");
        sleep(400);
        takeScreenshot("19-details-node-04.png");
    }
    @Test
    @Order(21)
    void captureDetailsForNode5() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        int size5 = driver.findElements(By.className("tree-node")).size();
        int idx5 = Math.max(0, Math.min(4, size5 - 1));
        driver.findElements(By.className("tree-node")).get(idx5).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("nodeName"), "Selected:"));
        ((JavascriptExecutor) driver).executeScript("let d=document.getElementById('nodeDetails'); d.scrollTop=d.scrollHeight/2;");
        sleep(400);
        takeScreenshot("20-details-node-05.png");
    }

    @Test
    @Order(22)
    void captureDetailsForNode6() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        int size6 = driver.findElements(By.className("tree-node")).size();
        int idx6 = Math.max(0, Math.min(5, size6 - 1));
        driver.findElements(By.className("tree-node")).get(idx6).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("nodeName"), "Selected:"));
        ((JavascriptExecutor) driver).executeScript("let d=document.getElementById('nodeDetails'); d.scrollTop=d.scrollHeight*0.7;");
        sleep(400);
        takeScreenshot("21-details-node-06.png");
    }

    @Test
    @Order(23)
    void captureDetailsForNode7() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        int size7 = driver.findElements(By.className("tree-node")).size();
        int idx7 = Math.max(0, Math.min(6, size7 - 1));
        driver.findElements(By.className("tree-node")).get(idx7).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("nodeName"), "Selected:"));
        ((JavascriptExecutor) driver).executeScript("let d=document.getElementById('nodeDetails'); d.scrollTop=d.scrollHeight*0.9;");
        sleep(400);
        takeScreenshot("22-details-node-07.png");
    }

    @Test
    @Order(24)
    void captureDetailsForNode8() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        int size8 = driver.findElements(By.className("tree-node")).size();
        int idx8 = Math.max(0, Math.min(7, size8 - 1));
        driver.findElements(By.className("tree-node")).get(idx8).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("nodeName"), "Selected:"));
        ((JavascriptExecutor) driver).executeScript("let d=document.getElementById('nodeDetails'); d.scrollTop=d.scrollHeight;");
        sleep(400);
        takeScreenshot("23-details-node-08.png");
    }

    @Test
    @Order(12)
    void capturePopulatedTreeView() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));

        // Load a known sample root file directly via JS to ensure the tree is populated
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);

        // Wait for tree nodes to render
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        sleep(800);

        takeScreenshot("11-populated-tree-view.png");
    }

    @Test
    @Order(13)
    void captureDetailsPanelPopulated() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));

        // Load the dependency tree
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);

        // Click the first tree node
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        WebElement firstNode = driver.findElements(By.className("tree-node")).get(0);
        firstNode.click();

        // Wait for details to load
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("nodeName"), "Selected:"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("nodeDetails")));
        sleep(800);

        takeScreenshot("12-details-panel-populated.png");
    }


    @Test
    @Order(25)
    void captureExpandLevel1() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        js.executeScript("expandToLevel(arguments[0]);", 1);
        sleep(500);
        takeScreenshot("24-expand-level-1.png");
    }

    @Test
    @Order(26)
    void captureExpandLevel2() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        js.executeScript("expandToLevel(arguments[0]);", 2);
        sleep(500);
        takeScreenshot("25-expand-level-2.png");
    }

    @Test
    @Order(27)
    void captureExpandLevel3() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        js.executeScript("expandToLevel(arguments[0]);", 3);
        sleep(500);
        takeScreenshot("26-expand-level-3.png");
    }


    @Test
    @Order(28)
    void captureToggleCollapsedOnFirstParent() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));

        // Find first node with children from page global treeData
        String firstParentPath = (String) js.executeScript("" +
                "function f(n){if(n.children&&n.children.length>0)return n;" +
                "for(let i=0;i<(n.children||[]).length;i++){let r=f(n.children[i]);if(r)return r;}return null;}" +
                "var nodes=Array.isArray(window.treeData)?window.treeData:[window.treeData];" +
                "var n=f(nodes[0]); return n?n.path:null;");
        if (firstParentPath != null) {
            js.executeScript("toggleNode(arguments[0]);", firstParentPath); // collapse it
            sleep(600);
        }
        takeScreenshot("27-toggle-collapsed.png");
    }

    @Test
    @Order(29)
    void captureToggleExpandedOnFirstParent() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));

        String firstParentPath = (String) js.executeScript("" +
                "function f(n){if(n.children&&n.children.length>0)return n;" +
                "for(let i=0;i<(n.children||[]).length;i++){let r=f(n.children[i]);if(r)return r;}return null;}" +
                "var nodes=Array.isArray(window.treeData)?window.treeData:[window.treeData];" +
                "var n=f(nodes[0]); return n?n.path:null;");
        if (firstParentPath != null) {
            js.executeScript("toggleNode(arguments[0]);", firstParentPath); // collapse
            js.executeScript("toggleNode(arguments[0]);", firstParentPath); // expand
            sleep(600);
        }
        takeScreenshot("28-toggle-expanded.png");
    }

    @Test
    @Order(30)
    void captureMixedToggleState() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));

        java.util.List<?> twoParents = (java.util.List<?>) js.executeScript("" +
                "function parents(acc,n){if(n.children&&n.children.length>0)acc.push(n.path);" +
                "for(let c of (n.children||[])) parents(acc,c); return acc;}" +
                "var nodes=Array.isArray(window.treeData)?window.treeData:[window.treeData];" +
                "var acc=[]; parents(acc,nodes[0]); return acc.slice(0,2);");

        if (twoParents != null && !twoParents.isEmpty()) {
            // Collapse first parent
            js.executeScript("toggleNode(arguments[0]);", twoParents.get(0));
        }
        if (twoParents != null && twoParents.size() > 1) {
            // Ensure the second parent remains expanded
            js.executeScript("expandToLevel(arguments[0]);", 3);
        }
        sleep(700);
        takeScreenshot("29-toggle-mixed.png");
    }





    @Test
    @Order(31)
    void captureToolbarLevelButtonsStates() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));

        driver.findElement(By.id("expandLevel1Btn")).click();
        sleep(500);
        takeScreenshot("30-toolbar-L1.png");

        driver.findElement(By.id("expandLevel2Btn")).click();
        sleep(500);
        takeScreenshot("31-toolbar-L2.png");

        driver.findElement(By.id("expandLevel3Btn")).click();
        sleep(500);
        takeScreenshot("32-toolbar-L3.png");
    }

    @Test
    @Order(32)
    void captureSpecificNodeToggleByName() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        js.executeScript("expandToLevel(arguments[0]);", 3);
        sleep(400);

        String targetName = "02-validation-groups.yaml";
        String targetPath = (String) js.executeScript("" +
                "function findByName(n,t){if(n.name===t)return n;for(let c of (n.children||[])){let r=findByName(c,t);if(r)return r;}return null;}" +
                "var nodes=Array.isArray(window.treeData)?window.treeData:[window.treeData];" +
                "var n=findByName(nodes[0], arguments[0]); return n? n.path : null;", targetName);

        if (targetPath != null) {
            js.executeScript("toggleNode(arguments[0]);", targetPath); // collapse
            sleep(600);
            takeScreenshot("33-node-02-validation-collapsed.png");
            js.executeScript("toggleNode(arguments[0]);", targetPath); // expand
            sleep(600);
            takeScreenshot("34-node-02-validation-expanded.png");
        } else {
            takeScreenshot("33-node-02-validation-not-found.png");
        }
    }

    @Test
    @Order(33)
    void captureDeepBranchBeforeAfter() throws IOException {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rootFile = rootFile();
        js.executeScript("loadDependencyTree(arguments[0]);", rootFile);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        js.executeScript("expandToLevel(arguments[0]);", 3);
        sleep(400);

        String deepParentPath = (String) js.executeScript("" +
                "function findWithGrandChildren(n){if(n.children&&n.children.some(c=>c.children&&c.children.length>0))return n;" +
                "for(let c of (n.children||[])){let r=findWithGrandChildren(c);if(r)return r;}return null;}" +
                "var nodes=Array.isArray(window.treeData)?window.treeData:[window.treeData];" +
                "var n=findWithGrandChildren(nodes[0]); return n? n.path : null;");

        if (deepParentPath != null) {
            // Ensure collapsed state first
            js.executeScript("toggleNode(arguments[0]);", deepParentPath);
            sleep(600);
            takeScreenshot("35-deep-branch-before.png");
            // Now expand to show grandchildren
            js.executeScript("toggleNode(arguments[0]);", deepParentPath);
            js.executeScript("expandToLevel(arguments[0]);", 4);
            sleep(700);
            takeScreenshot("36-deep-branch-after.png");
        } else {
            takeScreenshot("35-deep-branch-not-found.png");
        }
}

    }
