package dev.mars.apex.yaml.manager.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled("Manual test for interactive browser debugging - not for automated test runs")
public class FileBrowserManualTest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testFileBrowserConsoleOutput() throws InterruptedException {
        // Navigate to the page
        driver.get("http://localhost:" + port + "/yaml-manager/d3-tree-viewer.html");
        
        // Wait for page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("browse-btn")));
        
        System.out.println("=== PAGE LOADED ===");
        
        // Click the Browse button
        WebElement browseBtn = driver.findElement(By.id("browse-btn"));
        browseBtn.click();
        
        System.out.println("=== BROWSE BUTTON CLICKED ===");
        
        // Wait for modal to appear
        Thread.sleep(2000);
        
        // Check if modal is visible
        WebElement modal = driver.findElement(By.id("file-browser-modal"));
        String display = modal.getCssValue("display");
        System.out.println("Modal display: " + display);
        System.out.println("Modal class: " + modal.getAttribute("class"));
        
        // Get browser console logs
        LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
        System.out.println("\n=== BROWSER CONSOLE LOGS ===");
        for (LogEntry entry : logs) {
            System.out.println(entry.getLevel() + " " + entry.getMessage());
        }
        
        // Check file list
        Thread.sleep(2000);
        List<WebElement> fileItems = driver.findElements(By.className("file-browser-item"));
        System.out.println("\n=== FILE ITEMS FOUND: " + fileItems.size() + " ===");
        
        // Get the file list HTML
        WebElement fileList = driver.findElement(By.id("file-browser-list"));
        String fileListHtml = fileList.getAttribute("innerHTML");
        System.out.println("\n=== FILE LIST HTML ===");
        System.out.println(fileListHtml);
        
        // Execute JavaScript to check what's happening
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String fetchUrl = (String) js.executeScript("return '/yaml-manager/api/dependencies/browse';");
        System.out.println("\n=== FETCH URL: " + fetchUrl + " ===");
        
        // Keep browser open for manual inspection
        System.out.println("\n=== SLEEPING FOR 30 SECONDS - INSPECT THE BROWSER ===");
        Thread.sleep(30000);
    }
}

