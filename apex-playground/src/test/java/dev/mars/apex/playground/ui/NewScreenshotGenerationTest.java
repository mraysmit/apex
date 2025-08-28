package dev.mars.apex.playground.ui;

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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Generate new screenshots showing current APEX Playground functionality
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=INFO",
    "apex.playground.examples-enabled=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NewScreenshotGenerationTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor jsExecutor;
    private String baseUrl;
    private Path tempDir;
    private String screenshotDir = "docs/screenshots";

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-web-security");
        options.addArguments("--disable-features=VizDisplayCompositor");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        jsExecutor = (JavascriptExecutor) driver;
        baseUrl = "http://localhost:" + port;
        
        tempDir = Files.createTempDirectory("screenshot-test");
        
        // Create screenshots directory if it doesn't exist
        File screenshotDirFile = new File(screenshotDir);
        if (!screenshotDirFile.exists()) {
            screenshotDirFile.mkdirs();
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        if (driver != null) {
            driver.quit();
        }
        
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Generate initial playground interface with file names")
    void generateInitialPlaygroundWithFileNames() throws IOException {
        driver.get(baseUrl + "/playground");

        // Wait for page to fully load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sourceDataEditor")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("yamlRulesEditor")));

        // Wait extra time for file names to appear
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Check if file names are displayed
        WebElement sourceFileName = driver.findElement(By.id("sourceDataFileName"));
        WebElement yamlFileName = driver.findElement(By.id("yamlRulesFileName"));

        System.out.println("Source file name text: '" + sourceFileName.getText() + "'");
        System.out.println("YAML file name text: '" + yamlFileName.getText() + "'");
        System.out.println("Source file name HTML: " + sourceFileName.getAttribute("outerHTML"));
        System.out.println("YAML file name HTML: " + yamlFileName.getAttribute("outerHTML"));

        // Check if elements are visible
        System.out.println("Source file name visible: " + sourceFileName.isDisplayed());
        System.out.println("YAML file name visible: " + yamlFileName.isDisplayed());

        // Get the panel headers to see the full structure
        WebElement sourcePanel = driver.findElement(By.xpath("//div[contains(@class, 'card-header') and contains(., 'Source Data')]"));
        WebElement yamlPanel = driver.findElement(By.xpath("//div[contains(@class, 'card-header') and contains(., 'YAML Rules')]"));

        System.out.println("Source panel HTML: " + sourcePanel.getAttribute("outerHTML"));
        System.out.println("YAML panel HTML: " + yamlPanel.getAttribute("outerHTML"));

        // Take screenshot regardless of file name content
        takeScreenshot("playground_current_interface_state");
    }

    @Test
    @Order(2)
    @DisplayName("Generate file upload interface screenshot")
    void generateFileUploadInterface() throws IOException {
        driver.get(baseUrl + "/playground");
        
        // Wait for page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("uploadDataBtn")));
        
        // Highlight upload buttons
        WebElement uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));
        WebElement uploadYamlBtn = driver.findElement(By.id("uploadYamlBtn"));
        
        jsExecutor.executeScript("arguments[0].style.border='3px solid #007bff';", uploadDataBtn);
        jsExecutor.executeScript("arguments[0].style.border='3px solid #007bff';", uploadYamlBtn);
        
        takeScreenshot("playground_upload_buttons_highlighted");
    }

    @Test
    @Order(3)
    @DisplayName("Generate file upload with custom data")
    void generateFileUploadWithCustomData() throws IOException {
        driver.get(baseUrl + "/playground");
        
        // Create and upload custom files
        File jsonFile = createCustomJsonFile("customer-data.json");
        File yamlFile = createCustomYamlFile("validation-rules.yaml");
        
        // Upload files
        uploadFile("dataFileInput", jsonFile);
        uploadFile("yamlFileInput", yamlFile);
        
        // Wait for file names to update
        wait.until(ExpectedConditions.textToBe(By.id("sourceDataFileName"), "customer-data.json"));
        wait.until(ExpectedConditions.textToBe(By.id("yamlRulesFileName"), "validation-rules.yaml"));
        
        takeScreenshot("playground_custom_files_uploaded");
    }

    @Test
    @Order(4)
    @DisplayName("Generate APEX processing results")
    void generateApexProcessingResults() throws IOException {
        driver.get(baseUrl + "/playground");
        
        // Upload files and process
        File jsonFile = createCustomJsonFile("processing-data.json");
        File yamlFile = createCustomYamlFile("processing-rules.yaml");
        
        uploadFile("dataFileInput", jsonFile);
        uploadFile("yamlFileInput", yamlFile);
        
        // Process with APEX engine
        WebElement processBtn = driver.findElement(By.id("processBtn"));
        processBtn.click();
        
        // Wait for processing to complete
        wait.until(ExpectedConditions.not(ExpectedConditions.textToBe(
            By.id("validationResults"), "Click \"Process\" to see validation results...")));
        
        takeScreenshot("playground_apex_processing_results");
    }

    @Test
    @Order(5)
    @DisplayName("Generate drag and drop interface")
    void generateDragDropInterface() throws IOException {
        driver.get(baseUrl + "/playground");
        
        // Show drop zones by simulating drag over
        jsExecutor.executeScript("""
            // Show drop zones
            document.getElementById('dataDropZone').classList.remove('d-none');
            document.getElementById('yamlDropZone').classList.remove('d-none');
            document.getElementById('dataDropZone').classList.add('drag-over');
            document.getElementById('yamlDropZone').classList.add('drag-over');
            """);
        
        takeScreenshot("playground_drag_drop_zones_active");
    }

    @Test
    @Order(6)
    @DisplayName("Generate YAML validation error")
    void generateYamlValidationError() throws IOException {
        driver.get(baseUrl + "/playground");
        
        // Create invalid YAML
        WebElement yamlEditor = driver.findElement(By.id("yamlRulesEditor"));
        yamlEditor.clear();
        yamlEditor.sendKeys("""
            metadata:
              name: "Invalid YAML Example"
            rules:
              - id: invalid-rule
                condition: #invalid syntax here
                message: [unclosed array
            """);
        
        // Wait for validation to show error
        wait.until(ExpectedConditions.textToBe(By.id("yamlStatus"), "Invalid"));
        
        takeScreenshot("playground_yaml_validation_error");
    }

    @Test
    @Order(7)
    @DisplayName("Generate multi-format data processing")
    void generateMultiFormatProcessing() throws IOException {
        driver.get(baseUrl + "/playground");
        
        // Switch to XML format
        WebElement xmlFormatRadio = driver.findElement(By.id("xmlFormat"));
        jsExecutor.executeScript("arguments[0].click();", xmlFormatRadio);
        
        // Add XML data
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        sourceEditor.clear();
        sourceEditor.sendKeys("""
            <?xml version="1.0" encoding="UTF-8"?>
            <customer>
                <name>John Smith</name>
                <age>35</age>
                <email>john.smith@example.com</email>
                <amount>5000.00</amount>
                <currency>USD</currency>
            </customer>
            """);
        
        // Update file name display
        jsExecutor.executeScript("updateSourceDataFileName('customer-data.xml', 250);");
        
        takeScreenshot("playground_xml_format_processing");
    }

    // Helper methods

    private void takeScreenshot(String filename) throws IOException {
        TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
        File sourceFile = takesScreenshot.getScreenshotAs(OutputType.FILE);
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fullFilename = filename + "_" + timestamp + ".png";
        
        File destFile = new File(screenshotDir, fullFilename);
        FileUtils.copyFile(sourceFile, destFile);
        
        System.out.println("Screenshot saved: " + destFile.getAbsolutePath());
    }

    private void uploadFile(String inputId, File file) throws IOException {
        WebElement fileInput = driver.findElement(By.id(inputId));
        fileInput.sendKeys(file.getAbsolutePath());
        
        // Wait for upload to complete
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private File createCustomJsonFile(String fileName) throws IOException {
        File file = tempDir.resolve(fileName).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("""
                {
                  "name": "Alice Johnson",
                  "age": 28,
                  "email": "alice.johnson@company.com",
                  "amount": 3500.00,
                  "currency": "EUR",
                  "department": "Engineering",
                  "riskLevel": "medium",
                  "accountType": "premium"
                }
                """);
        }
        return file;
    }

    private File createCustomYamlFile(String fileName) throws IOException {
        File file = tempDir.resolve(fileName).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("""
                metadata:
                  name: "Customer Validation Rules"
                  version: "2.0.0"
                  description: "Comprehensive customer data validation"
                
                rules:
                  - id: "age-validation"
                    name: "Age Validation Rule"
                    condition: "#age >= 18 && #age <= 65"
                    message: "Customer age is within acceptable range"
                    enabled: true
                    priority: 1
                  
                  - id: "amount-validation"
                    name: "Amount Validation Rule"
                    condition: "#amount > 1000"
                    message: "Transaction amount exceeds minimum threshold"
                    enabled: true
                    priority: 2
                
                enrichments:
                  - type: "lookup-enrichment"
                    name: "Risk Assessment"
                    condition: "#amount > 5000"
                    enrichments:
                      riskCategory: "high-value"
                      requiresApproval: true
                """);
        }
        return file;
    }
}
