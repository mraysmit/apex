package dev.mars.apex.yaml.manager.ui;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Strict E2E: scan folder -> load selected -> tree renders (no JS injection).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ScanLoadTreeStrictE2ETest {

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
        wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        baseUrl = "http://localhost:" + port + "/yaml-manager";
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }

    private String samplesDir() {
        return Paths.get(System.getProperty("user.dir"),
                "src", "test", "resources", "apex-yaml-samples")
            .toAbsolutePath().toString();
    }

    @Test
    @Order(1)
    void scan_loadSelected_renders_tree_and_details() {
        // Open Tree Viewer
        driver.get(baseUrl + "/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));

        // Debug: Check if page loaded correctly
        String pageTitle = driver.getTitle();
        System.out.println("TEST: Page title=" + pageTitle);

        // Debug: Check if loadFolderBtn exists
        try {
            WebElement loadFolderBtn = driver.findElement(By.id("loadFolderBtn"));
            System.out.println("TEST: loadFolderBtn found, displayed=" + loadFolderBtn.isDisplayed());
        } catch (Exception e) {
            System.out.println("TEST: loadFolderBtn not found: " + e.getMessage());
            // Print page source for debugging
            String pageSource = driver.getPageSource();
            System.out.println("TEST: Page source first 500 chars: " + pageSource.substring(0, Math.min(500, pageSource.length())));
            throw e;
        }

        // Open Load Folder modal
        WebElement loadFolderBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("loadFolderBtn")));
        loadFolderBtn.click();

        // Debug: Check if modal exists before waiting for visibility
        try {
            WebElement modal = driver.findElement(By.id("folderModal"));
            System.out.println("TEST: folderModal found, displayed=" + modal.isDisplayed());
        } catch (Exception e) {
            System.out.println("TEST: folderModal not found: " + e.getMessage());
        }

        WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("folderModal")));
        assertTrue(modal.isDisplayed(), "Folder modal should be visible");

        // Enter samples folder path and scan
        String folder = samplesDir();
        WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("folderPathInput")));
        input.clear();
        input.sendKeys(folder);
        driver.findElement(By.id("scanFolderBtn")).click();

        // Wait for scan results
        WebElement scanStatus = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("scanStatus")));
        wait.until(d -> scanStatus.getText() != null && !scanStatus.getText().isBlank());
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("file-item")));

        // Deselect all, then select a known-good root file (scenario-registry.yaml)
        java.util.List<WebElement> fileItems = driver.findElements(By.className("file-item"));
        // Uncheck all checkboxes to avoid loading the wrong root
        for (WebElement item : fileItems) {
            WebElement cb = item.findElement(By.cssSelector("input[type='checkbox']"));
            if (cb.isSelected()) {
                cb.click();
            }
        }
        // Select the registry file explicitly by checking its checkbox
        WebElement registryItem = fileItems.stream()
            .filter(it -> it.findElement(By.className("file-item-name")).getText().contains("scenario-registry.yaml"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("scenario-registry.yaml not found in scan results"));
        WebElement registryCb = registryItem.findElement(By.cssSelector("input[type='checkbox']"));
        if (!registryCb.isSelected()) {
            registryCb.click();
        }
        // Ensure Load Selected is enabled and click it
        // Sanity: count checked inputs before clicking Load Selected
        Object checkedCountBefore = ((JavascriptExecutor) driver).executeScript("return document.querySelectorAll('#fileList input[type=\\'checkbox\\']:checked').length;");
        System.out.println("TEST: checked inputs BEFORE Load Selected click=" + checkedCountBefore);

        WebElement loadSelected = wait.until(ExpectedConditions.elementToBeClickable(By.id("loadSelectedBtn")));
        assertNull(loadSelected.getAttribute("disabled"), "Load Selected should be enabled when a file is selected");
        loadSelected.click();

        // Modal should close
        wait.until(ExpectedConditions.invisibilityOf(modal));

        // Debug: call backend /tree directly with the expected root to verify server behavior
        try {
            String root = Paths.get(samplesDir(), "scenario-registry.yaml").toAbsolutePath().toString();
            String treeUrl = baseUrl + "/api/dependencies/tree?rootFile=" + java.net.URLEncoder.encode(root, java.nio.charset.StandardCharsets.UTF_8);
            java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(treeUrl)).GET().build();
            java.net.http.HttpResponse<String> resp = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            System.out.println("TEST: direct /tree GET status=" + resp.statusCode());
            System.out.println("TEST: direct /tree body first 200=" + (resp.body() == null ? "<null>" : resp.body().substring(0, Math.min(200, resp.body().length()))));
        } catch (Exception e) {
            System.out.println("TEST: direct /tree GET failed: " + e);
        }

        Object checkedCount = ((JavascriptExecutor) driver).executeScript("return document.querySelectorAll('#fileList input[type=\\'checkbox\\']:checked').length;");
        System.out.println("TEST: checked inputs before Load Selected click=" + checkedCount);

        // Debug: peek at treeView content in case of backend error
        WebElement treeView = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));
        String tvTextEarly = treeView.getText();
        System.out.println("TEST: treeView (early) first 200 chars: " + (tvTextEarly == null ? "<null>" : tvTextEarly.substring(0, Math.min(200, tvTextEarly.length()))));
        if (tvTextEarly != null && (tvTextEarly.contains("Error") || tvTextEarly.contains("Validation"))) {
            fail("TreeView shows error: " + tvTextEarly);
        }

        // Optional diagnostic: observe window.treeData length
        Object tdLenObj = ((JavascriptExecutor) driver).executeScript("return (window.treeData && Array.isArray(window.treeData)) ? window.treeData.length : -1;");
        long tdLen = (tdLenObj instanceof Number) ? ((Number) tdLenObj).longValue() : -1L;
        System.out.println("TEST: window.treeData length after click=" + tdLen);

        Object lastStep = ((JavascriptExecutor) driver).executeScript("return window.__lastStep || '<unset>';");
        System.out.println("TEST: window.__lastStep=" + lastStep);

        try { Thread.sleep(3000); } catch (InterruptedException ie) { /* ignore */ }
        Object nodeCountJs = ((JavascriptExecutor) driver).executeScript("return document.querySelectorAll('.tree-node').length;");
        System.out.println("TEST: .tree-node count via JS after 3s=" + nodeCountJs);

        // Tree should render without any JS injection (allow extra time for backend analysis)
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(60));
        longWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        int nodeCount = driver.findElements(By.className("tree-node")).size();
        assertTrue(nodeCount > 0, "Tree nodes should render after loading selected files");

        // Click first node and assert detail panel sections
        WebElement firstNode = driver.findElements(By.className("tree-node")).get(0);
        firstNode.click();

        WebElement nodeName = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nodeName")));
        String nodeNameText = nodeName.getText();
        assertTrue(nodeNameText.startsWith("Selected:"), "Header should show 'Selected: <name>'");

        WebElement nodeType = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nodeType")));
        String nodeTypeText = nodeType.getText();
        assertTrue(nodeTypeText.startsWith("Type:"), "Header should show 'Type: <type>'");

        WebElement detailsPanel = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nodeDetails")));
        wait.until(ExpectedConditions.textToBePresentInElement(detailsPanel, "File Path:"));
        String detailsText = detailsPanel.getText();
        assertTrue(detailsText.contains("File Path:"), "Details should include File Path section");
        assertTrue(detailsText.contains("Health Score"), "Details should include Health Score section");
        assertTrue(detailsText.contains("Metadata"), "Details should include Metadata section");
    }
}

