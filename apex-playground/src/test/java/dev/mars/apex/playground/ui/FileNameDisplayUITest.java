package dev.mars.apex.playground.ui;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
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
 * Selenium UI tests for file name display functionality in APEX Playground.
 * Tests that uploaded file names are properly displayed in the panel headers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=INFO",
    "apex.playground.examples-enabled=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileNameDisplayUITest {

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor jsExecutor;
    private String baseUrl;
    private Path tempDir;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        jsExecutor = (JavascriptExecutor) driver;
        baseUrl = "http://localhost:" + port;
        
        tempDir = Files.createTempDirectory("file-name-display-test");
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
    @DisplayName("Should display example file names on initial load")
    void shouldDisplayExampleFileNamesOnInitialLoad() {
        // Given
        driver.get(baseUrl + "/playground");

        // When - Check initial state (playground auto-loads example)
        WebElement sourceFileName = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("sourceDataFileName")));
        WebElement yamlFileName = driver.findElement(By.id("yamlRulesFileName"));

        // Then - Should show example file names (playground auto-loads example data)
        assertEquals("example-data.json", sourceFileName.getText(),
                    "Source data panel should show example file name on initial load");
        assertEquals("example-rules.yaml", yamlFileName.getText(),
                    "YAML rules panel should show example file name on initial load");

        // Verify styling
        assertTrue(sourceFileName.getDomAttribute("class").contains("text-success"),
                  "Example file name should be styled as success");
        assertTrue(yamlFileName.getDomAttribute("class").contains("text-success"),
                  "Example file name should be styled as success");
    }

    @Test
    @Order(2)
    @DisplayName("Should display file name when data file is uploaded via button")
    void shouldDisplayFileNameWhenDataFileIsUploadedViaButton() throws IOException {
        // Given
        File testFile = createTestJsonFile("test-data.json");
        driver.get(baseUrl + "/playground");

        // When - Upload file via button
        uploadFileToEditor("dataFileInput", testFile);

        // Then - Verify file name is displayed
        WebElement sourceFileName = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("sourceDataFileName")));
        
        assertEquals("test-data.json", sourceFileName.getText(),
                    "Should display the uploaded file name");
        assertTrue(sourceFileName.getDomAttribute("class").contains("text-success"),
                  "File name should be styled as success");
        
        // Verify file size is displayed
        WebElement sourceFileSize = driver.findElement(By.id("sourceDataFileSize"));
        assertTrue(sourceFileSize.getText().contains("Bytes") || sourceFileSize.getText().contains("KB"),
                  "Should display file size");
    }

    @Test
    @Order(3)
    @DisplayName("Should display file name when YAML file is uploaded via button")
    void shouldDisplayFileNameWhenYamlFileIsUploadedViaButton() throws IOException {
        // Given
        File testFile = createTestYamlFile("test-rules.yaml");
        driver.get(baseUrl + "/playground");

        // When - Upload file via button
        uploadFileToEditor("yamlFileInput", testFile);

        // Then - Verify file name is displayed
        WebElement yamlFileName = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("yamlRulesFileName")));
        
        assertEquals("test-rules.yaml", yamlFileName.getText(),
                    "Should display the uploaded YAML file name");
        assertTrue(yamlFileName.getDomAttribute("class").contains("text-success"),
                  "File name should be styled as success");
        
        // Verify file size is displayed
        WebElement yamlFileSize = driver.findElement(By.id("yamlRulesFileSize"));
        assertTrue(yamlFileSize.getText().contains("Bytes") || yamlFileSize.getText().contains("KB"),
                  "Should display file size");
    }

    @Test
    @Order(4)
    @DisplayName("Should display file name when files are uploaded via drag and drop")
    void shouldDisplayFileNameWhenFilesAreUploadedViaDragAndDrop() throws IOException {
        // Given
        File jsonFile = createTestJsonFile("dragged-data.json");
        File yamlFile = createTestYamlFile("dragged-rules.yaml");
        driver.get(baseUrl + "/playground");

        // When - Simulate drag and drop for data file
        simulateFileDrop("sourceDataEditor", jsonFile);
        
        // Then - Verify data file name is displayed
        WebElement sourceFileName = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("sourceDataFileName")));
        assertEquals("dragged-data.json", sourceFileName.getText(),
                    "Should display dragged data file name");

        // When - Simulate drag and drop for YAML file
        simulateFileDrop("yamlRulesEditor", yamlFile);
        
        // Then - Verify YAML file name is displayed
        WebElement yamlFileName = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("yamlRulesFileName")));
        assertEquals("dragged-rules.yaml", yamlFileName.getText(),
                    "Should display dragged YAML file name");
    }

    @Test
    @Order(5)
    @DisplayName("Should clear file names when Clear button is clicked")
    void shouldClearFileNamesWhenClearButtonIsClicked() throws IOException {
        // Given - Upload files first
        File jsonFile = createTestJsonFile("to-clear.json");
        File yamlFile = createTestYamlFile("to-clear.yaml");
        driver.get(baseUrl + "/playground");
        
        uploadFileToEditor("dataFileInput", jsonFile);
        uploadFileToEditor("yamlFileInput", yamlFile);

        // Verify files are loaded
        WebElement sourceFileName = driver.findElement(By.id("sourceDataFileName"));
        WebElement yamlFileName = driver.findElement(By.id("yamlRulesFileName"));
        assertEquals("to-clear.json", sourceFileName.getText());
        assertEquals("to-clear.yaml", yamlFileName.getText());

        // When - Click clear button and confirm
        WebElement clearBtn = driver.findElement(By.id("clearBtn"));
        clearBtn.click();
        
        // Handle confirmation dialog
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();

        // Then - Verify file names are cleared
        wait.until(ExpectedConditions.textToBe(By.id("sourceDataFileName"), "No file loaded"));
        wait.until(ExpectedConditions.textToBe(By.id("yamlRulesFileName"), "No file loaded"));
        
        assertEquals("No file loaded", sourceFileName.getText(),
                    "Source data file name should be cleared");
        assertEquals("No file loaded", yamlFileName.getText(),
                    "YAML rules file name should be cleared");
    }

    @Test
    @Order(6)
    @DisplayName("Should display example file names when Load Example is used")
    void shouldDisplayExampleFileNamesWhenLoadExampleIsUsed() {
        // Given
        driver.get(baseUrl + "/playground");

        // When - Click load example button
        WebElement loadExampleBtn = driver.findElement(By.id("loadExampleBtn"));
        loadExampleBtn.click();

        // Wait for example to load (this loads the default example)
        wait.until(ExpectedConditions.not(ExpectedConditions.textToBe(
            By.id("sourceDataFileName"), "No file loaded")));

        // Then - Verify example file names are displayed
        WebElement sourceFileName = driver.findElement(By.id("sourceDataFileName"));
        WebElement yamlFileName = driver.findElement(By.id("yamlRulesFileName"));
        
        assertTrue(sourceFileName.getText().contains("example") || sourceFileName.getText().contains("json"),
                  "Should display example data file name");
        assertTrue(yamlFileName.getText().contains("example") || yamlFileName.getText().contains("yaml"),
                  "Should display example YAML file name");
    }

    @Test
    @Order(7)
    @DisplayName("Should update file names when different files are uploaded")
    void shouldUpdateFileNamesWhenDifferentFilesAreUploaded() throws IOException {
        // Given
        File firstFile = createTestJsonFile("first-file.json");
        File secondFile = createTestJsonFile("second-file.json");
        driver.get(baseUrl + "/playground");

        // When - Upload first file
        uploadFileToEditor("dataFileInput", firstFile);
        WebElement sourceFileName = driver.findElement(By.id("sourceDataFileName"));
        assertEquals("first-file.json", sourceFileName.getText());

        // When - Upload second file
        uploadFileToEditor("dataFileInput", secondFile);
        
        // Then - Verify file name is updated
        wait.until(ExpectedConditions.textToBe(By.id("sourceDataFileName"), "second-file.json"));
        assertEquals("second-file.json", sourceFileName.getText(),
                    "Should update to show the new file name");
    }

    // Helper methods

    private void uploadFileToEditor(String inputId, File file) throws IOException {
        WebElement fileInput = driver.findElement(By.id(inputId));
        fileInput.sendKeys(file.getAbsolutePath());
        
        // Wait for upload to complete
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateFileDrop(String editorId, File file) throws IOException {
        String fileContent = Files.readString(file.toPath());
        String fileName = file.getName();
        long fileSize = file.length();
        
        // Simulate file drop by directly calling the JavaScript functions
        String script = String.format("""
            const editor = document.getElementById('%s');
            editor.value = `%s`;
            
            // Simulate the file drop behavior
            if ('%s'.includes('sourceData')) {
                updateSourceDataFileName('%s', %d);
            } else {
                updateYamlRulesFileName('%s', %d);
            }
            """, editorId, fileContent.replace("`", "\\`"), editorId, fileName, fileSize, fileName, fileSize);
        
        jsExecutor.executeScript(script);
    }

    private File createTestJsonFile(String fileName) throws IOException {
        File file = tempDir.resolve(fileName).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("""
                {
                  "name": "Test User",
                  "age": 25,
                  "email": "test@example.com"
                }
                """);
        }
        return file;
    }

    private File createTestYamlFile(String fileName) throws IOException {
        File file = tempDir.resolve(fileName).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("""
                metadata:
                  name: "Test Rules"
                  version: "1.0.0"
                
                rules:
                  - id: "test-rule"
                    name: "Test Rule"
                    condition: "#age >= 18"
                    message: "Valid"
                    enabled: true
                """);
        }
        return file;
    }
}
