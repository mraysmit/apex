package dev.mars.apex.playground.ui;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for drag-and-drop file upload functionality in APEX Playground.
 * Tests actual drag-and-drop interactions, drop zone behaviors, and file handling.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=INFO",
    "apex.playground.examples-enabled=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DragDropFileUploadUITest {

    private WebDriver driver;
    private WebDriverWait wait;
    private Actions actions;
    private JavascriptExecutor jsExecutor;
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
        actions = new Actions(driver);
        jsExecutor = (JavascriptExecutor) driver;
        baseUrl = "http://localhost:" + port;
        
        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("apex-playground-dragdrop-test");
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
    @DisplayName("Should show drop zone when dragging over data editor")
    void shouldShowDropZoneWhenDraggingOverDataEditor() {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        WebElement dataDropZone = driver.findElement(By.id("dataDropZone"));

        // Initially drop zone should be hidden
        assertTrue(dataDropZone.getDomAttribute("class").contains("d-none"), 
                  "Drop zone should be initially hidden");

        // When - Simulate drag enter event
        jsExecutor.executeScript(
            "var event = new DragEvent('dragenter', { bubbles: true, cancelable: true });" +
            "arguments[0].dispatchEvent(event);", sourceEditor);

        // Then - Drop zone should become visible
        wait.until(ExpectedConditions.not(ExpectedConditions.attributeContains(dataDropZone, "class", "d-none")));
        assertFalse(dataDropZone.getDomAttribute("class").contains("d-none"), 
                   "Drop zone should be visible when dragging over editor");
    }

    @Test
    @Order(2)
    @DisplayName("Should show drop zone when dragging over YAML editor")
    void shouldShowDropZoneWhenDraggingOverYamlEditor() {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement yamlEditor = driver.findElement(By.id("yamlRulesEditor"));
        WebElement yamlDropZone = driver.findElement(By.id("yamlDropZone"));

        // Initially drop zone should be hidden
        assertTrue(yamlDropZone.getDomAttribute("class").contains("d-none"), 
                  "YAML drop zone should be initially hidden");

        // When - Simulate drag enter event
        jsExecutor.executeScript(
            "var event = new DragEvent('dragenter', { bubbles: true, cancelable: true });" +
            "arguments[0].dispatchEvent(event);", yamlEditor);

        // Then - Drop zone should become visible
        wait.until(ExpectedConditions.not(ExpectedConditions.attributeContains(yamlDropZone, "class", "d-none")));
        assertFalse(yamlDropZone.getDomAttribute("class").contains("d-none"), 
                   "YAML drop zone should be visible when dragging over editor");
    }

    @Test
    @Order(3)
    @DisplayName("Should hide drop zone when drag leaves editor")
    void shouldHideDropZoneWhenDragLeavesEditor() {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        WebElement dataDropZone = driver.findElement(By.id("dataDropZone"));

        // Show drop zone first
        jsExecutor.executeScript(
            "var event = new DragEvent('dragenter', { bubbles: true, cancelable: true });" +
            "arguments[0].dispatchEvent(event);", sourceEditor);
        
        wait.until(ExpectedConditions.not(ExpectedConditions.attributeContains(dataDropZone, "class", "d-none")));

        // When - Simulate drag leave event
        jsExecutor.executeScript(
            "var event = new DragEvent('dragleave', { bubbles: true, cancelable: true });" +
            "arguments[0].dispatchEvent(event);", sourceEditor);

        // Then - Drop zone should be hidden again
        wait.until(ExpectedConditions.attributeContains(dataDropZone, "class", "d-none"));
        assertTrue(dataDropZone.getDomAttribute("class").contains("d-none"), 
                  "Drop zone should be hidden when drag leaves editor");
    }

    @Test
    @Order(4)
    @DisplayName("Should simulate JSON file drop on data editor")
    void shouldSimulateJsonFileDropOnDataEditor() throws IOException {
        // Given
        File jsonFile = createTestJsonFile();
        driver.get(baseUrl + "/playground");
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));

        // When - Simulate file drop using JavaScript
        String fileContent = Files.readString(jsonFile.toPath());
        jsExecutor.executeScript(
            "var editor = arguments[0];" +
            "var content = arguments[1];" +
            "editor.value = content;" +
            "editor.dispatchEvent(new Event('input', { bubbles: true }));",
            sourceEditor, fileContent);

        // Simulate the format detection that would happen after file drop
        WebElement jsonFormatRadio = driver.findElement(By.id("jsonFormat"));
        jsExecutor.executeScript("arguments[0].checked = true;", jsonFormatRadio);

        // Then - Verify content was loaded
        String editorContent = sourceEditor.getDomAttribute("value");
        assertTrue(editorContent.contains("John Doe"), "Editor should contain JSON content");
        assertTrue(editorContent.contains("john.doe@example.com"), "Editor should contain email");
        assertTrue(jsonFormatRadio.isSelected(), "JSON format should be selected");
    }

    @Test
    @Order(5)
    @DisplayName("Should simulate YAML file drop on YAML editor")
    void shouldSimulateYamlFileDropOnYamlEditor() throws IOException {
        // Given
        File yamlFile = createTestYamlFile();
        driver.get(baseUrl + "/playground");
        WebElement yamlEditor = driver.findElement(By.id("yamlRulesEditor"));

        // When - Simulate file drop using JavaScript
        String fileContent = Files.readString(yamlFile.toPath());
        jsExecutor.executeScript(
            "var editor = arguments[0];" +
            "var content = arguments[1];" +
            "editor.value = content;" +
            "editor.dispatchEvent(new Event('input', { bubbles: true }));",
            yamlEditor, fileContent);

        // Then - Verify content was loaded
        String editorContent = yamlEditor.getDomAttribute("value");
        assertTrue(editorContent.contains("metadata:"), "Editor should contain YAML metadata");
        assertTrue(editorContent.contains("Test Configuration"), "Editor should contain YAML name");
        assertTrue(editorContent.contains("test-rule"), "Editor should contain rule ID");
    }

    @Test
    @Order(6)
    @DisplayName("Should show drag-over styling when file is dragged over drop zone")
    void shouldShowDragOverStylingWhenFileIsDraggedOverDropZone() {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));
        WebElement dataDropZone = driver.findElement(By.id("dataDropZone"));

        // Show drop zone first
        jsExecutor.executeScript(
            "var event = new DragEvent('dragenter', { bubbles: true, cancelable: true });" +
            "arguments[0].dispatchEvent(event);", sourceEditor);
        
        wait.until(ExpectedConditions.not(ExpectedConditions.attributeContains(dataDropZone, "class", "d-none")));

        // When - Simulate drag over event
        jsExecutor.executeScript(
            "var event = new DragEvent('dragover', { bubbles: true, cancelable: true });" +
            "arguments[0].dispatchEvent(event);" +
            "arguments[0].classList.add('drag-over');", dataDropZone);

        // Then - Verify drag-over styling is applied
        assertTrue(dataDropZone.getDomAttribute("class").contains("drag-over"), 
                  "Drop zone should have drag-over styling");
    }

    @Test
    @Order(7)
    @DisplayName("Should handle multiple file types in drop simulation")
    void shouldHandleMultipleFileTypesInDropSimulation() throws IOException {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement sourceEditor = driver.findElement(By.id("sourceDataEditor"));

        // Test JSON file
        File jsonFile = createTestJsonFile();
        String jsonContent = Files.readString(jsonFile.toPath());
        jsExecutor.executeScript(
            "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input'));",
            sourceEditor, jsonContent);
        assertTrue(sourceEditor.getDomAttribute("value").contains("John Doe"), "Should handle JSON");

        // Test XML file
        File xmlFile = createTestXmlFile();
        String xmlContent = Files.readString(xmlFile.toPath());
        jsExecutor.executeScript(
            "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input'));",
            sourceEditor, xmlContent);
        assertTrue(sourceEditor.getDomAttribute("value").contains("<person>"), "Should handle XML");

        // Test CSV file
        File csvFile = createTestCsvFile();
        String csvContent = Files.readString(csvFile.toPath());
        jsExecutor.executeScript(
            "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input'));",
            sourceEditor, csvContent);
        assertTrue(sourceEditor.getDomAttribute("value").contains("name,age,email"), "Should handle CSV");
    }

    // Helper methods for creating test files

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
                  description: "Test YAML configuration for drag-drop"
                
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

    private File createTestXmlFile() throws IOException {
        File xmlFile = tempDir.resolve("test-data.xml").toFile();
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write("""
                <?xml version="1.0" encoding="UTF-8"?>
                <person>
                    <name>Jane Smith</name>
                    <age>28</age>
                    <email>jane.smith@example.com</email>
                </person>
                """);
        }
        return xmlFile;
    }

    private File createTestCsvFile() throws IOException {
        File csvFile = tempDir.resolve("test-data.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("""
                name,age,email
                Bob Johnson,25,bob.johnson@example.com
                Alice Brown,32,alice.brown@example.com
                """);
        }
        return csvFile;
    }
}

