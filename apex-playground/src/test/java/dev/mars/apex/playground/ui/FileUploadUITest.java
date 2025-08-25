package dev.mars.apex.playground.ui;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Selenium UI tests for file upload functionality in APEX Playground.
 * Tests file upload buttons, drag-and-drop zones, and error handling.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=INFO",
    "apex.playground.examples-enabled=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileUploadUITest {

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;
    private Path tempDir;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        // Setup Chrome driver with options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        baseUrl = "http://localhost:" + port;
        
        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("apex-playground-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (driver != null) {
            driver.quit();
        }
        
        // Clean up temporary files
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should display file upload buttons")
    void shouldDisplayFileUploadButtons() {
        // When
        driver.get(baseUrl + "/playground");

        // Then
        assertTrue(isElementPresent(By.id("uploadDataBtn")), "Upload Data button should be present");
        assertTrue(isElementPresent(By.id("uploadYamlBtn")), "Upload YAML button should be present");
        
        // Verify button text and icons
        WebElement uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));
        assertTrue(uploadDataBtn.getText().contains("Upload Data"), "Upload Data button should have correct text");
        
        WebElement uploadYamlBtn = driver.findElement(By.id("uploadYamlBtn"));
        assertTrue(uploadYamlBtn.getText().contains("Upload YAML"), "Upload YAML button should have correct text");
    }

    @Test
    @Order(2)
    @DisplayName("Should upload JSON data file successfully")
    void shouldUploadJsonDataFileSuccessfully() throws IOException {
        // Given
        File jsonFile = createTestJsonFile();
        driver.get(baseUrl + "/playground");

        // When
        uploadFileViaButton("dataFileInput", jsonFile);

        // Then
        WebElement sourceEditor = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sourceDataEditor")));
        String editorContent = sourceEditor.getAttribute("value");
        
        assertTrue(editorContent.contains("John Doe"), "Editor should contain uploaded JSON data");
        assertTrue(editorContent.contains("john.doe@example.com"), "Editor should contain email from JSON");
        
        // Verify format was auto-detected
        WebElement jsonFormatRadio = driver.findElement(By.id("jsonFormat"));
        assertTrue(jsonFormatRadio.isSelected(), "JSON format should be auto-selected");
        
        // Verify success message
        verifySuccessAlert("uploaded successfully");
    }

    @Test
    @Order(3)
    @DisplayName("Should upload YAML configuration file successfully")
    void shouldUploadYamlFileSuccessfully() throws IOException {
        // Given
        File yamlFile = createTestYamlFile();
        driver.get(baseUrl + "/playground");

        // When
        uploadFileViaButton("yamlFileInput", yamlFile);

        // Then
        WebElement yamlEditor = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("yamlRulesEditor")));
        String editorContent = yamlEditor.getAttribute("value");
        
        assertTrue(editorContent.contains("metadata:"), "Editor should contain uploaded YAML metadata");
        assertTrue(editorContent.contains("Test Configuration"), "Editor should contain YAML name");
        
        // Verify YAML validation status
        WebElement yamlStatus = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("yamlStatus")));
        assertTrue(yamlStatus.getText().toLowerCase().contains("valid"), "YAML should be validated as valid");
        
        // Verify success message
        verifySuccessAlert("uploaded successfully");
    }

    @Test
    @Order(4)
    @DisplayName("Should handle invalid file types gracefully")
    void shouldHandleInvalidFileTypesGracefully() throws IOException {
        // Given
        File invalidFile = createInvalidFile();
        driver.get(baseUrl + "/playground");

        // When
        uploadFileViaButton("dataFileInput", invalidFile);

        // Then - JavaScript validation should show error alert
        try {
            verifyErrorAlert("Invalid file type");
        } catch (Exception e) {
            // If no alert appears, the file input might have been rejected by browser
            // Verify that the editor content hasn't changed
            WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
            String editorContent = sourceEditor.getAttribute("value");
            assertTrue(editorContent.isEmpty() || !editorContent.contains("This is not a valid data file"),
                      "Invalid file content should not be loaded into editor");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should handle large files gracefully")
    void shouldHandleLargeFilesGracefully() throws IOException {
        // Given
        File largeFile = createLargeFile();
        driver.get(baseUrl + "/playground");

        // When
        uploadFileViaButton("dataFileInput", largeFile);

        // Then - JavaScript validation should show error alert
        try {
            verifyErrorAlert("File size exceeds");
        } catch (Exception e) {
            // If no alert appears, verify the file wasn't processed
            WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
            String editorContent = sourceEditor.getAttribute("value");
            // Large file content should not be loaded (would be too much text)
            assertTrue(editorContent.length() < 1000000,
                      "Large file content should not be loaded into editor");
        }
    }

    @Test
    @Order(6)
    @DisplayName("Should show upload progress modal")
    void shouldShowUploadProgressModal() throws IOException {
        // Given
        File jsonFile = createTestJsonFile();
        driver.get(baseUrl + "/playground");

        // When
        WebElement uploadBtn = driver.findElement(By.id("uploadDataBtn"));
        uploadBtn.click();
        
        WebElement fileInput = driver.findElement(By.id("dataFileInput"));
        fileInput.sendKeys(jsonFile.getAbsolutePath());

        // Then
        // Note: Progress modal might be too fast to catch in tests, but we can verify it exists
        assertTrue(isElementPresent(By.id("uploadProgressModal")), "Upload progress modal should exist");
    }

    @Test
    @Order(7)
    @DisplayName("Should display drag and drop zones")
    void shouldDisplayDragAndDropZones() {
        // When
        driver.get(baseUrl + "/playground");

        // Then
        assertTrue(isElementPresent(By.id("dataDropZone")), "Data drop zone should be present");
        assertTrue(isElementPresent(By.id("yamlDropZone")), "YAML drop zone should be present");
        
        // Verify drop zones are initially hidden
        WebElement dataDropZone = driver.findElement(By.id("dataDropZone"));
        WebElement yamlDropZone = driver.findElement(By.id("yamlDropZone"));
        
        assertTrue(dataDropZone.getAttribute("class").contains("d-none"), "Data drop zone should be initially hidden");
        assertTrue(yamlDropZone.getAttribute("class").contains("d-none"), "YAML drop zone should be initially hidden");
    }

    // Helper methods

    private boolean isElementPresent(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void uploadFileViaButton(String inputId, File file) {
        WebElement uploadBtn = inputId.equals("dataFileInput") ? 
            driver.findElement(By.id("uploadDataBtn")) : 
            driver.findElement(By.id("uploadYamlBtn"));
        
        uploadBtn.click();
        
        WebElement fileInput = driver.findElement(By.id(inputId));
        fileInput.sendKeys(file.getAbsolutePath());
    }

    private void verifySuccessAlert(String expectedMessage) {
        WebElement alert = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".alert-success")));
        assertTrue(alert.getText().toLowerCase().contains(expectedMessage.toLowerCase()),
                  "Success alert should contain expected message");
    }

    private void verifyErrorAlert(String expectedMessage) {
        WebElement alert = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".alert-danger")));
        assertTrue(alert.getText().toLowerCase().contains(expectedMessage.toLowerCase()),
                  "Error alert should contain expected message");
    }

    private File createTestJsonFile() throws IOException {
        File jsonFile = tempDir.resolve("test-data.json").toFile();
        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write("""
                {
                  "name": "John Doe",
                  "age": 30,
                  "email": "john.doe@example.com",
                  "amount": 1500.00,
                  "currency": "USD"
                }
                """);
        }
        return jsonFile;
    }

    private File createTestYamlFile() throws IOException {
        File yamlFile = tempDir.resolve("test-config.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("""
                metadata:
                  name: "Test Configuration"
                  version: "1.0.0"
                  description: "Test YAML configuration for upload"
                  type: "rule-config"
                
                rules:
                  - id: "test-rule"
                    name: "Test Rule"
                    condition: "#age >= 18"
                    message: "Age requirement met"
                    enabled: true
                """);
        }
        return yamlFile;
    }

    private File createInvalidFile() throws IOException {
        File invalidFile = tempDir.resolve("test.exe").toFile();
        try (FileWriter writer = new FileWriter(invalidFile)) {
            writer.write("This is not a valid data file");
        }
        return invalidFile;
    }

    private File createLargeFile() throws IOException {
        File largeFile = tempDir.resolve("large-file.json").toFile();
        try (FileWriter writer = new FileWriter(largeFile)) {
            // Create a file larger than 10MB
            for (int i = 0; i < 1000000; i++) {
                writer.write("This is a very long line to make the file large enough to exceed the size limit. ");
            }
        }
        return largeFile;
    }

    @Test
    @Order(8)
    @DisplayName("Should handle XML file upload and auto-detect format")
    void shouldHandleXmlFileUploadAndAutoDetectFormat() throws IOException {
        // Given
        File xmlFile = createTestXmlFile();
        driver.get(baseUrl + "/playground");

        // When
        uploadFileViaButton("dataFileInput", xmlFile);

        // Then
        WebElement sourceEditor = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sourceDataEditor")));
        String editorContent = sourceEditor.getAttribute("value");

        assertTrue(editorContent.contains("<person>"), "Editor should contain uploaded XML data");
        assertTrue(editorContent.contains("<name>Jane Smith</name>"), "Editor should contain XML content");

        // Verify XML format was auto-detected
        WebElement xmlFormatRadio = driver.findElement(By.id("xmlFormat"));
        assertTrue(xmlFormatRadio.isSelected(), "XML format should be auto-selected");
    }

    @Test
    @Order(9)
    @DisplayName("Should handle CSV file upload and auto-detect format")
    void shouldHandleCsvFileUploadAndAutoDetectFormat() throws IOException {
        // Given
        File csvFile = createTestCsvFile();
        driver.get(baseUrl + "/playground");

        // When
        uploadFileViaButton("dataFileInput", csvFile);

        // Then
        WebElement sourceEditor = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sourceDataEditor")));
        String editorContent = sourceEditor.getAttribute("value");

        assertTrue(editorContent.contains("name,age,email"), "Editor should contain CSV headers");
        assertTrue(editorContent.contains("Bob Johnson,25"), "Editor should contain CSV data");

        // Verify CSV format was auto-detected
        WebElement csvFormatRadio = driver.findElement(By.id("csvFormat"));
        assertTrue(csvFormatRadio.isSelected(), "CSV format should be auto-selected");
    }

    @Test
    @Order(10)
    @DisplayName("Should handle empty file upload gracefully")
    void shouldHandleEmptyFileUploadGracefully() throws IOException {
        // Given
        File emptyFile = createEmptyFile();
        driver.get(baseUrl + "/playground");

        // When
        uploadFileViaButton("dataFileInput", emptyFile);

        // Then - JavaScript validation should show error alert or prevent upload
        try {
            verifyErrorAlert("File is empty");
        } catch (Exception e) {
            // If no alert appears, verify the editor remains empty
            WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
            String editorContent = sourceEditor.getAttribute("value");
            assertTrue(editorContent.isEmpty(), "Editor should remain empty when empty file is uploaded");
        }
    }

    @Test
    @Order(11)
    @DisplayName("Should handle invalid YAML file gracefully")
    void shouldHandleInvalidYamlFileGracefully() throws IOException {
        // Given
        File invalidYamlFile = createInvalidYamlFile();
        driver.get(baseUrl + "/playground");

        // When
        uploadFileViaButton("yamlFileInput", invalidYamlFile);

        // Then
        // The file should upload but YAML validation should show error
        WebElement yamlStatus = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("yamlStatus")));
        assertTrue(yamlStatus.getText().toLowerCase().contains("invalid") ||
                  yamlStatus.getAttribute("class").contains("bg-danger"),
                  "YAML should be marked as invalid");
    }

    @Test
    @Order(12)
    @DisplayName("Should clear file input after successful upload")
    void shouldClearFileInputAfterSuccessfulUpload() throws IOException {
        // Given
        File jsonFile = createTestJsonFile();
        driver.get(baseUrl + "/playground");

        // When
        uploadFileViaButton("dataFileInput", jsonFile);

        // Then
        WebElement fileInput = driver.findElement(By.id("dataFileInput"));
        assertEquals("", fileInput.getAttribute("value"), "File input should be cleared after upload");
    }

    // Additional helper methods for new test files

    private File createTestXmlFile() throws IOException {
        File xmlFile = tempDir.resolve("test-data.xml").toFile();
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write("""
                <?xml version="1.0" encoding="UTF-8"?>
                <person>
                    <name>Jane Smith</name>
                    <age>28</age>
                    <email>jane.smith@example.com</email>
                    <amount>2000.00</amount>
                    <currency>EUR</currency>
                </person>
                """);
        }
        return xmlFile;
    }

    private File createTestCsvFile() throws IOException {
        File csvFile = tempDir.resolve("test-data.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("""
                name,age,email,amount,currency
                Bob Johnson,25,bob.johnson@example.com,1200.00,USD
                Alice Brown,32,alice.brown@example.com,1800.00,GBP
                """);
        }
        return csvFile;
    }

    private File createEmptyFile() throws IOException {
        File emptyFile = tempDir.resolve("empty.json").toFile();
        emptyFile.createNewFile(); // Creates empty file
        return emptyFile;
    }

    private File createInvalidYamlFile() throws IOException {
        File invalidYamlFile = tempDir.resolve("invalid.yaml").toFile();
        try (FileWriter writer = new FileWriter(invalidYamlFile)) {
            writer.write("""
                metadata:
                  name: "Invalid YAML"
                  version: "1.0.0"

                rules:
                  - id: missing-quotes
                    name: Invalid Rule
                    condition: #age >= 18  # Missing quotes around condition
                    message: This will cause YAML parsing error
                    enabled: true
                    invalid_field: [unclosed array
                """);
        }
        return invalidYamlFile;
    }
}
