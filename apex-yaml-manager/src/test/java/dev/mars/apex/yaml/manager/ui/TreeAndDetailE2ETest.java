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
 * End-to-end UI tests focused on the two critical features:
 * - Tree view renders nodes for a valid root file
 * - Selecting a node populates the detail view with correct info
 *
 * Uses dynamic path resolution for the sample YAML to avoid machine-specific paths.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TreeAndDetailE2ETest {

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

    private String rootFile() {
        String path = Paths.get(
            System.getProperty("user.dir"),
            "src", "test", "resources", "apex-yaml-samples", "scenario-registry.yaml"
        ).toAbsolutePath().toString();
        System.out.println("TEST: Using rootFile=" + path);
        return path;
    }

    @Test
    @Order(1)
    void treeView_renders_nodes_for_valid_root() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));

        String rf = rootFile();
        ((JavascriptExecutor) driver).executeScript("loadDependencyTree(arguments[0]);", rf);

        // Wait for at least one node to render
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        int count = driver.findElements(By.className("tree-node")).size();
        System.out.println("TEST: Tree nodes rendered: " + count);
        assertTrue(count > 0, "Tree nodes should render for a valid root file");
    }

    @Test
    @Order(2)
    void selecting_node_populates_detail_view_header_and_path() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("treeView")));

        String rf = rootFile();
        ((JavascriptExecutor) driver).executeScript("loadDependencyTree(arguments[0]);", rf);

        // Wait for nodes then click the first one
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tree-node")));
        WebElement firstNode = driver.findElements(By.className("tree-node")).get(0);
        firstNode.click();

        // Header should show Selected: <name>
        WebElement nodeName = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nodeName")));
        String nodeNameText = nodeName.getText();
        System.out.println("TEST: nodeName header text: " + nodeNameText);
        assertTrue(nodeNameText.startsWith("Selected:"), "Header should show 'Selected: <name>'");

        // Type header should be present
        WebElement nodeType = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nodeType")));
        String nodeTypeText = nodeType.getText();
        System.out.println("TEST: nodeType header text: " + nodeTypeText);
        assertTrue(nodeTypeText.startsWith("Type:"), "Header should show 'Type: <type>'");

        // Details panel should contain core sections
        WebElement detailsPanel = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nodeDetails")));
        // Wait until the details panel shows at least the File Path section
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("nodeDetails"), "File Path:"));
        String detailsText = detailsPanel.getText();
        System.out.println("TEST: details panel (first 200 chars): " + detailsText.substring(0, Math.min(200, detailsText.length())));
        assertTrue(detailsText.contains("File Path:"), "Details should show 'File Path:' section");
        assertTrue(detailsText.contains("Health Score") && detailsText.contains("Metadata"),
                "Details should include Health Score and Metadata sections");
    }
}

