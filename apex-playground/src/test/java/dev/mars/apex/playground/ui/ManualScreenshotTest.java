package dev.mars.apex.playground.ui;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manual screenshot test to debug file name display issue
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=INFO",
    "apex.playground.examples-enabled=true"
})
class ManualScreenshotTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor jsExecutor;
    private String baseUrl;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-web-security");
        options.addArguments("--disable-features=VizDisplayCompositor");
        // Remove headless mode to see what's actually happening
        // options.addArguments("--headless");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        jsExecutor = (JavascriptExecutor) driver;
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        // Keep browser open for manual inspection
        try {
            Thread.sleep(10000); // Wait 10 seconds to see the result
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @DisplayName("Screenshot file upload buttons")
    void screenshotFileUploadButtons() throws Exception {
        System.out.println("Opening playground at: " + baseUrl + "/playground");
        driver.get(baseUrl + "/playground");

        // Wait for page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("uploadDataBtn")));
        Thread.sleep(2000);

        // Highlight upload buttons
        WebElement uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));
        WebElement uploadYamlBtn = driver.findElement(By.id("uploadYamlBtn"));
        WebElement saveConfigBtn = driver.findElement(By.id("saveConfigBtn"));
        WebElement loadExampleBtn = driver.findElement(By.id("loadExampleBtn"));

        // Add highlighting
        jsExecutor.executeScript("arguments[0].style.border='3px solid #007bff'; arguments[0].style.boxShadow='0 0 10px #007bff';", uploadDataBtn);
        jsExecutor.executeScript("arguments[0].style.border='3px solid #007bff'; arguments[0].style.boxShadow='0 0 10px #007bff';", uploadYamlBtn);
        jsExecutor.executeScript("arguments[0].style.border='3px solid #28a745'; arguments[0].style.boxShadow='0 0 10px #28a745';", saveConfigBtn);
        jsExecutor.executeScript("arguments[0].style.border='3px solid #ffc107'; arguments[0].style.boxShadow='0 0 10px #ffc107';", loadExampleBtn);

        takeScreenshot("file_upload_buttons_highlighted");
    }

    @Test
    @DisplayName("Screenshot drag and drop zones")
    void screenshotDragDropZones() throws Exception {
        System.out.println("Opening playground for drag-drop screenshot");
        driver.get(baseUrl + "/playground");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sourceDataEditor")));
        Thread.sleep(2000);

        // Show drag-drop zones by simulating drag over
        jsExecutor.executeScript("""
            // Show drop zones
            var dataDropZone = document.getElementById('dataDropZone');
            var yamlDropZone = document.getElementById('yamlDropZone');

            if (dataDropZone) {
                dataDropZone.classList.remove('d-none');
                dataDropZone.classList.add('drag-over');
                dataDropZone.style.display = 'block';
                dataDropZone.style.border = '3px dashed #007bff';
                dataDropZone.style.backgroundColor = 'rgba(0, 123, 255, 0.1)';
            }

            if (yamlDropZone) {
                yamlDropZone.classList.remove('d-none');
                yamlDropZone.classList.add('drag-over');
                yamlDropZone.style.display = 'block';
                yamlDropZone.style.border = '3px dashed #28a745';
                yamlDropZone.style.backgroundColor = 'rgba(40, 167, 69, 0.1)';
            }
            """);

        Thread.sleep(1000);
        takeScreenshot("drag_drop_zones_active");
    }

    @Test
    @DisplayName("Manual screenshot with browser visible")
    void manualScreenshotTest() throws Exception {
        System.out.println("Opening playground at: " + baseUrl + "/playground");
        driver.get(baseUrl + "/playground");
        
        // Wait for page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sourceDataEditor")));
        System.out.println("Page loaded, waiting for file names...");
        
        // Wait extra time
        Thread.sleep(5000);
        
        // Check file name elements
        try {
            WebElement sourceFileName = driver.findElement(By.id("sourceDataFileName"));
            WebElement yamlFileName = driver.findElement(By.id("yamlRulesFileName"));
            
            System.out.println("=== FILE NAME DEBUG INFO ===");
            System.out.println("Source file name text: '" + sourceFileName.getText() + "'");
            System.out.println("YAML file name text: '" + yamlFileName.getText() + "'");
            System.out.println("Source file name displayed: " + sourceFileName.isDisplayed());
            System.out.println("YAML file name displayed: " + yamlFileName.isDisplayed());
            
            // Get computed styles
            String sourceColor = (String) jsExecutor.executeScript("return window.getComputedStyle(arguments[0]).color;", sourceFileName);
            String yamlColor = (String) jsExecutor.executeScript("return window.getComputedStyle(arguments[0]).color;", yamlFileName);
            String sourceDisplay = (String) jsExecutor.executeScript("return window.getComputedStyle(arguments[0]).display;", sourceFileName);
            String yamlDisplay = (String) jsExecutor.executeScript("return window.getComputedStyle(arguments[0]).display;", yamlFileName);
            
            System.out.println("Source file name color: " + sourceColor);
            System.out.println("YAML file name color: " + yamlColor);
            System.out.println("Source file name display: " + sourceDisplay);
            System.out.println("YAML file name display: " + yamlDisplay);
            
            // Get parent elements
            WebElement sourceHeader = driver.findElement(By.xpath("//div[contains(@class, 'card-header') and contains(., 'Source Data')]"));
            WebElement yamlHeader = driver.findElement(By.xpath("//div[contains(@class, 'card-header') and contains(., 'YAML Rules')]"));
            
            System.out.println("Source header text: '" + sourceHeader.getText() + "'");
            System.out.println("YAML header text: '" + yamlHeader.getText() + "'");
            
        } catch (Exception e) {
            System.out.println("Error finding file name elements: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Take screenshot
        TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
        File sourceFile = takesScreenshot.getScreenshotAs(OutputType.FILE);
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "manual_screenshot_debug_" + timestamp + ".png";
        
        File destFile = new File("docs/screenshots", filename);
        FileUtils.copyFile(sourceFile, destFile);
        
        System.out.println("Screenshot saved: " + destFile.getAbsolutePath());
        System.out.println("Browser will stay open for 10 seconds for manual inspection...");
    }

    private void takeScreenshot(String filename) throws Exception {
        TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
        File sourceFile = takesScreenshot.getScreenshotAs(OutputType.FILE);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fullFilename = filename + "_" + timestamp + ".png";

        File destFile = new File("docs/screenshots", fullFilename);
        FileUtils.copyFile(sourceFile, destFile);

        System.out.println("Screenshot saved: " + destFile.getAbsolutePath());
    }
}
