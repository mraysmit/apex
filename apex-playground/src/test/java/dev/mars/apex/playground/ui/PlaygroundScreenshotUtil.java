package dev.mars.apex.playground.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility class for taking screenshots of the APEX Playground using Selenium WebDriver.
 * Supports multiple browsers and various screenshot scenarios.
 */
public class PlaygroundScreenshotUtil {

    public enum Browser {
        CHROME, FIREFOX, EDGE
    }

    public enum ScreenshotType {
        FULL_PAGE,           // Full page screenshot
        VIEWPORT_ONLY,       // Only visible viewport
        ELEMENT_SPECIFIC,    // Specific UI element
        MOBILE_VIEW,         // Mobile responsive view
        TABLET_VIEW,         // Tablet responsive view
        DESKTOP_VIEW         // Desktop view
    }

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;
    private String screenshotDir;

    public PlaygroundScreenshotUtil(String baseUrl) {
        this.baseUrl = baseUrl;
        this.screenshotDir = "target/screenshots";
        createScreenshotDirectory();
    }

    public PlaygroundScreenshotUtil(String baseUrl, String screenshotDir) {
        this.baseUrl = baseUrl;
        this.screenshotDir = screenshotDir;
        createScreenshotDirectory();
    }

    private void createScreenshotDirectory() {
        File dir = new File(screenshotDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Initialize WebDriver for the specified browser
     */
    public void initializeDriver(Browser browser, boolean headless) {
        setupWebDriverManager();
        driver = createDriver(browser, headless);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    private void setupWebDriverManager() {
        try {
            WebDriverManager.chromedriver().setup();
            WebDriverManager.firefoxdriver().setup();
            WebDriverManager.edgedriver().setup();
        } catch (Exception e) {
            System.out.println("WebDriver setup warning: " + e.getMessage());
        }
    }

    private WebDriver createDriver(Browser browser, boolean headless) {
        return switch (browser) {
            case CHROME -> {
                ChromeOptions options = new ChromeOptions();
                if (headless) {
                    options.addArguments("--headless");
                }
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--disable-gpu");
                options.addArguments("--window-size=1920,1080");
                yield new ChromeDriver(options);
            }
            case FIREFOX -> {
                FirefoxOptions options = new FirefoxOptions();
                if (headless) {
                    options.addArguments("--headless");
                }
                options.addArguments("--width=1920");
                options.addArguments("--height=1080");
                yield new FirefoxDriver(options);
            }
            case EDGE -> {
                EdgeOptions options = new EdgeOptions();
                if (headless) {
                    options.addArguments("--headless");
                }
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--disable-gpu");
                options.addArguments("--window-size=1920,1080");
                yield new EdgeDriver(options);
            }
        };
    }

    /**
     * Take a screenshot of the playground with specified parameters
     */
    public String takeScreenshot(Browser browser, ScreenshotType type, String scenario) throws IOException {
        return takeScreenshot(browser, type, scenario, null, false);
    }

    /**
     * Take a screenshot with custom data loaded
     */
    public String takeScreenshot(Browser browser, ScreenshotType type, String scenario, 
                                PlaygroundData data, boolean headless) throws IOException {
        
        if (driver == null) {
            initializeDriver(browser, headless);
        }

        // Navigate to playground
        driver.get(baseUrl + "/playground");
        
        // Wait for page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("processBtn")));
        
        // Load custom data if provided
        if (data != null) {
            loadPlaygroundData(data);
        }

        // Set viewport size based on screenshot type
        setViewportSize(type);
        
        // Take screenshot based on type
        String filename = generateFilename(browser, type, scenario);
        File screenshotFile = new File(screenshotDir, filename);
        
        switch (type) {
            case FULL_PAGE -> takeFullPageScreenshot(screenshotFile);
            case VIEWPORT_ONLY -> takeViewportScreenshot(screenshotFile);
            case ELEMENT_SPECIFIC -> takeElementScreenshot(screenshotFile, scenario);
            default -> takeViewportScreenshot(screenshotFile);
        }
        
        return screenshotFile.getAbsolutePath();
    }

    private void setViewportSize(ScreenshotType type) {
        Dimension size = switch (type) {
            case MOBILE_VIEW -> new Dimension(375, 667);    // iPhone 6/7/8
            case TABLET_VIEW -> new Dimension(768, 1024);   // iPad
            case DESKTOP_VIEW -> new Dimension(1920, 1080); // Desktop
            default -> new Dimension(1920, 1080);
        };
        driver.manage().window().setSize(size);
    }

    private void takeFullPageScreenshot(File file) throws IOException {
        // For full page screenshots, we need to scroll and capture
        TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
        File sourceFile = takesScreenshot.getScreenshotAs(OutputType.FILE);
        FileUtils.copyFile(sourceFile, file);
    }

    private void takeViewportScreenshot(File file) throws IOException {
        TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
        File sourceFile = takesScreenshot.getScreenshotAs(OutputType.FILE);
        FileUtils.copyFile(sourceFile, file);
    }

    private void takeElementScreenshot(File file, String elementId) throws IOException {
        WebElement element = driver.findElement(By.id(elementId));
        File sourceFile = element.getScreenshotAs(OutputType.FILE);
        FileUtils.copyFile(sourceFile, file);
    }

    private void loadPlaygroundData(PlaygroundData data) {
        if (data.getSourceData() != null) {
            WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
            sourceEditor.clear();
            sourceEditor.sendKeys(data.getSourceData());
        }

        if (data.getYamlRules() != null) {
            WebElement yamlEditor = driver.findElement(By.id("yamlRulesEditor"));
            yamlEditor.clear();
            yamlEditor.sendKeys(data.getYamlRules());
        }

        if (data.shouldProcess()) {
            WebElement processBtn = driver.findElement(By.id("processBtn"));
            processBtn.click();

            // Wait for the process button to be re-enabled (indicates processing is complete)
            wait.until(ExpectedConditions.not(ExpectedConditions.attributeContains(
                By.id("processBtn"), "disabled", "true")));

            // Wait for actual JSON content to appear in validation results
            wait.until(ExpectedConditions.textMatches(
                By.id("validationResults"),
                java.util.regex.Pattern.compile(".*\"valid\".*", java.util.regex.Pattern.DOTALL)));

            // Wait for actual JSON content to appear in enrichment results
            wait.until(ExpectedConditions.textMatches(
                By.id("enrichmentResults"),
                java.util.regex.Pattern.compile(".*\"enriched\".*", java.util.regex.Pattern.DOTALL)));

            // Additional wait to ensure all DOM updates are complete
            try {
                Thread.sleep(2000); // Give extra time for all results to fully render
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String generateFilename(Browser browser, ScreenshotType type, String scenario) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String browserName = browser.name().toLowerCase();
        String typeName = type.name().toLowerCase();
        String scenarioName = scenario != null ? "_" + scenario.replaceAll("[^a-zA-Z0-9]", "_") : "";
        
        return String.format("playground_%s_%s%s_%s.png", browserName, typeName, scenarioName, timestamp);
    }

    /**
     * Close the WebDriver
     */
    public void close() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    /**
     * Data class for playground content
     */
    public static class PlaygroundData {
        private String sourceData;
        private String yamlRules;
        private boolean process;

        public PlaygroundData(String sourceData, String yamlRules, boolean process) {
            this.sourceData = sourceData;
            this.yamlRules = yamlRules;
            this.process = process;
        }

        // Getters
        public String getSourceData() { return sourceData; }
        public String getYamlRules() { return yamlRules; }
        public boolean shouldProcess() { return process; }
    }
}

