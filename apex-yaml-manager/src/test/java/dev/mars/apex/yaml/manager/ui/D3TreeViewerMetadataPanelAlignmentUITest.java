package dev.mars.apex.yaml.manager.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class D3TreeViewerMetadataPanelAlignmentUITest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setup() {
        ChromeOptions options = new ChromeOptions();
        options.setBinary("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.setCapability("goog:loggingPrefs", java.util.Map.of("browser", "ALL"));

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        baseUrl = "http://localhost:" + port + "/yaml-manager";
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @DisplayName("Verify File Metadata panel alignment (no blue gap)")
    void testMetadataPanelAlignment() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        // Wait for page load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tree-container")));

        // Inject JS to show the metadata panel with dummy data
        // We need to simulate the state where a file is selected
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
            "document.getElementById('metadata-section').style.display = 'grid';" +
            "document.getElementById('content-accordion-metadata').style.display = 'flex';" +
            "document.getElementById('file-metadata').innerHTML = '<div class=\"metadata-grid\"></div>';"
        );

        WebElement metadataPanel = driver.findElement(By.cssSelector(".file-metadata"));
        WebElement parentContainer = driver.findElement(By.id("content-accordion-metadata"));

        // Get computed styles
        String width = metadataPanel.getCssValue("width");
        String margin = metadataPanel.getCssValue("margin");
        String border = metadataPanel.getCssValue("border-left-width");

        System.out.println("Metadata Panel Width: " + width);
        System.out.println("Metadata Panel Margin: " + margin);
        System.out.println("Metadata Panel Border Left: " + border);

        // Assertions to verify the fix
        
        // Check if width matches parent
        int parentWidth = parentContainer.getSize().getWidth();
        int childWidth = metadataPanel.getSize().getWidth();
        
        // Allow 1px difference for rounding errors
        assertTrue(Math.abs(parentWidth - childWidth) <= 1, 
            "Metadata panel width (" + childWidth + ") should match parent width (" + parentWidth + ")");

        // Check for no left border (which was causing the gap/misalignment visual)
        assertTrue(border.startsWith("0"), "Left border should be 0px, but was " + border);
        
        // Check margin is 0
        assertTrue(margin.equals("0px"), "Margin should be 0px, but was " + margin);
    }
}
