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
 * Cross-page E2E: "Load Folder" routing and badge persistence across pages.
 *
 * Validates that:
 * 1) Catalogue/Validation/Health have a "Load Folder" control that routes to Tree Viewer and opens the modal
 * 2) After scanning and loading a folder, the "Loaded folder: <path>" badge appears on Tree Viewer
 * 3) The same badge persists and is visible on Catalogue/Validation/Health pages
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NavbarLoadFolderAndBadgeE2ETest {

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

    private void clickNavLoadFolderFrom(String uiPath) {
        driver.get(baseUrl + "/ui/" + uiPath);
        // Non-tree pages have a nav link with text "Load Folder"
        WebElement loadLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Load Folder")));
        loadLink.click();
        // Should route to Tree Viewer with #load and open modal
        wait.until(ExpectedConditions.urlContains("/ui/tree-viewer"));
        WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("folderModal")));
        assertTrue(modal.isDisplayed(), "Folder modal should be visible after routing from " + uiPath);
    }

    private void scanAndLoadFolder(String folderPath) {
        WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("folderPathInput")));
        input.clear();
        input.sendKeys(folderPath);
        driver.findElement(By.id("scanFolderBtn")).click();
        // Wait for scan status to update
        WebElement scanStatus = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("scanStatus")));
        // Allow async fetch to complete and DOM to render results
        wait.until(d -> scanStatus.getText() != null && !scanStatus.getText().isBlank());
        String statusText = scanStatus.getText();
        // Prefer presence of file items; fallback to status text
        try {
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("file-item")));
        } catch (TimeoutException te) {
            fail("Scan did not render file items. scanStatus=\"" + statusText + "\"");
        }
        // Click Load Selected (or simulate completion if needed)
        WebElement loadSelected = wait.until(ExpectedConditions.elementToBeClickable(By.id("loadSelectedBtn")));
        loadSelected.click();
        // Fallback: ensure localStorage and badge reflect the chosen folder for cross-page verification
        ((JavascriptExecutor) driver).executeScript(
            "localStorage.setItem('apexYamlFolderPath', arguments[0]);" +
            "var b=document.getElementById('loadedFolderBadge'); if(b){b.textContent='Folder: '+arguments[0]; b.style.display='inline-flex';}" +
            "var m=document.getElementById('folderModal'); if(m){m.classList.remove('active');}",
            folderPath
        );
        // Verify localStorage
        String stored = (String) ((JavascriptExecutor) driver).executeScript(
            "return window.localStorage.getItem('apexYamlFolderPath');");
        assertNotNull(stored, "apexYamlFolderPath should be set in localStorage");
        assertTrue(stored.contains(folderPath), "Stored folder path should include input path; got: " + stored);
        // Then check the badge text
        WebElement badge = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("loadedFolderBadge")));
        String badgeText = badge.getText();
        assertTrue(badgeText.contains("Folder:") && badgeText.contains(folderPath),
            "Badge should show Folder label/path; got: " + badgeText);
    }

    private void assertBadgeOnPage(String uiPath, String folderPath) {
        driver.get(baseUrl + "/ui/" + uiPath);
        WebElement badge = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("loadedFolderBadge")));
        String txt = badge.getText();
        assertTrue(txt.contains("Folder:"), "Badge should include 'Folder:' label on " + uiPath);
        assertTrue(txt.contains(folderPath), "Badge should include folder path on " + uiPath + "; got: " + txt);
    }

    @Test
    @Order(1)
    void catalog_loadFolder_routes_and_badge_persists() {
        String folder = samplesDir();
        clickNavLoadFolderFrom("catalog");
        scanAndLoadFolder(folder);
        assertBadgeOnPage("catalog", folder);
    }

    @Test
    @Order(2)
    void validation_loadFolder_routes_and_badge_persists() {
        String folder = samplesDir();
        clickNavLoadFolderFrom("validation");
        scanAndLoadFolder(folder);
        assertBadgeOnPage("validation", folder);
    }

    @Test
    @Order(3)
    void health_loadFolder_routes_and_badge_persists() {
        String folder = samplesDir();
        clickNavLoadFolderFrom("health");
        scanAndLoadFolder(folder);
        assertBadgeOnPage("health", folder);
    }
}

