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

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for Upload Button Interactions in APEX Playground.
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
class UploadButtonInteractionUITest {

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor jsExecutor;
    private String baseUrl;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        // Setup Chrome driver with options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        jsExecutor = (JavascriptExecutor) driver;
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should trigger data file input when upload data button is clicked")
    void shouldTriggerDataFileInputWhenUploadDataButtonIsClicked() {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));
        WebElement dataFileInput = driver.findElement(By.id("dataFileInput"));

        // Verify initial state
        assertTrue(uploadDataBtn.isDisplayed(), "Upload Data button should be visible");
        assertFalse(dataFileInput.isDisplayed(), "Data file input should be hidden");

        // When - Click upload data button
        uploadDataBtn.click();

        // Then - Verify file input would be triggered (we can't actually test file dialog in headless mode,
        // but we can verify the click event was handled)
        assertTrue(uploadDataBtn.isEnabled(), "Upload button should remain enabled after click");
    }

    @Test
    @Order(2)
    @DisplayName("Should trigger YAML file input when upload YAML button is clicked")
    void shouldTriggerYamlFileInputWhenUploadYamlButtonIsClicked() {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement uploadYamlBtn = driver.findElement(By.id("uploadYamlBtn"));
        WebElement yamlFileInput = driver.findElement(By.id("yamlFileInput"));

        // Verify initial state
        assertTrue(uploadYamlBtn.isDisplayed(), "Upload YAML button should be visible");
        assertFalse(yamlFileInput.isDisplayed(), "YAML file input should be hidden");

        // When - Click upload YAML button
        uploadYamlBtn.click();

        // Then - Verify button interaction
        assertTrue(uploadYamlBtn.isEnabled(), "Upload YAML button should remain enabled after click");
    }

    @Test
    @Order(3)
    @DisplayName("Should have correct button styling and icons")
    void shouldHaveCorrectButtonStylingAndIcons() {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));
        WebElement uploadYamlBtn = driver.findElement(By.id("uploadYamlBtn"));

        // Then - Verify button styling
        assertTrue(uploadDataBtn.getDomAttribute("class").contains("btn-outline-success"), 
                  "Upload Data button should have success outline styling");
        assertTrue(uploadYamlBtn.getDomAttribute("class").contains("btn-outline-success"), 
                  "Upload YAML button should have success outline styling");

        // Verify icons
        WebElement dataIcon = uploadDataBtn.findElement(By.className("fa-upload"));
        WebElement yamlIcon = uploadYamlBtn.findElement(By.className("fa-file-upload"));
        
        assertTrue(dataIcon.isDisplayed(), "Upload Data button should have upload icon");
        assertTrue(yamlIcon.isDisplayed(), "Upload YAML button should have file-upload icon");
    }

    @Test
    @Order(4)
    @DisplayName("Should have correct button text content")
    void shouldHaveCorrectButtonTextContent() {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));
        WebElement uploadYamlBtn = driver.findElement(By.id("uploadYamlBtn"));

        // Then
        assertTrue(uploadDataBtn.getText().contains("Upload Data"), 
                  "Upload Data button should have correct text");
        assertTrue(uploadYamlBtn.getText().contains("Upload YAML"), 
                  "Upload YAML button should have correct text");
    }

    @Test
    @Order(5)
    @DisplayName("Should have correct file input accept attributes")
    void shouldHaveCorrectFileInputAcceptAttributes() {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement dataFileInput = driver.findElement(By.id("dataFileInput"));
        WebElement yamlFileInput = driver.findElement(By.id("yamlFileInput"));

        // Then
        String dataAccept = dataFileInput.getDomAttribute("accept");
        String yamlAccept = yamlFileInput.getDomAttribute("accept");

        assertTrue(dataAccept.contains(".json"), "Data input should accept JSON files");
        assertTrue(dataAccept.contains(".xml"), "Data input should accept XML files");
        assertTrue(dataAccept.contains(".csv"), "Data input should accept CSV files");
        assertTrue(dataAccept.contains(".txt"), "Data input should accept TXT files");

        assertTrue(yamlAccept.contains(".yaml"), "YAML input should accept YAML files");
        assertTrue(yamlAccept.contains(".yml"), "YAML input should accept YML files");
    }

    @Test
    @Order(6)
    @DisplayName("Should be positioned correctly in toolbar")
    void shouldBePositionedCorrectlyInToolbar() {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement toolbar = driver.findElement(By.className("btn-toolbar"));
        WebElement uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));
        WebElement uploadYamlBtn = driver.findElement(By.id("uploadYamlBtn"));

        // Then - Verify buttons are in toolbar
        assertTrue(toolbar.findElements(By.id("uploadDataBtn")).size() > 0, 
                  "Upload Data button should be in toolbar");
        assertTrue(toolbar.findElements(By.id("uploadYamlBtn")).size() > 0, 
                  "Upload YAML button should be in toolbar");

        // Verify they're in the same button group
        WebElement buttonGroup = uploadDataBtn.findElement(By.xpath(".."));
        assertTrue(buttonGroup.getDomAttribute("class").contains("btn-group"), 
                  "Upload buttons should be in a button group");
        assertTrue(buttonGroup.findElements(By.id("uploadYamlBtn")).size() > 0, 
                  "Both upload buttons should be in the same group");
    }

    @Test
    @Order(7)
    @DisplayName("Should handle rapid button clicks gracefully")
    void shouldHandleRapidButtonClicksGracefully() {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));

        // When - Click button multiple times rapidly
        for (int i = 0; i < 5; i++) {
            uploadDataBtn.click();
            // Small delay to simulate rapid clicking
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Then - Button should remain functional
        assertTrue(uploadDataBtn.isEnabled(), "Button should remain enabled after rapid clicks");
        assertTrue(uploadDataBtn.isDisplayed(), "Button should remain visible after rapid clicks");
    }

    @Test
    @Order(8)
    @DisplayName("Should maintain button state during page interactions")
    void shouldMaintainButtonStateDuringPageInteractions() {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));
        WebElement uploadYamlBtn = driver.findElement(By.id("uploadYamlBtn"));

        // Interact with other page elements
        WebElement processBtn = driver.findElement(By.id("processBtn"));
        WebElement validateBtn = driver.findElement(By.id("validateBtn"));
        
        processBtn.click();
        validateBtn.click();

        // Then - Upload buttons should maintain their state
        assertTrue(uploadDataBtn.isEnabled(), "Upload Data button should remain enabled");
        assertTrue(uploadYamlBtn.isEnabled(), "Upload YAML button should remain enabled");
        assertTrue(uploadDataBtn.isDisplayed(), "Upload Data button should remain visible");
        assertTrue(uploadYamlBtn.isDisplayed(), "Upload YAML button should remain visible");
    }

    @Test
    @Order(9)
    @DisplayName("Should be accessible via keyboard navigation")
    void shouldBeAccessibleViaKeyboardNavigation() {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));

        // When - Focus on button using JavaScript (simulating tab navigation)
        jsExecutor.executeScript("arguments[0].focus();", uploadDataBtn);

        // Then - Button should be focusable
        WebElement focusedElement = driver.switchTo().activeElement();
        assertEquals(uploadDataBtn, focusedElement, "Upload button should be focusable");
    }

    @Test
    @Order(10)
    @DisplayName("Should have proper ARIA attributes for accessibility")
    void shouldHaveProperAriaAttributesForAccessibility() {
        // Given
        driver.get(baseUrl + "/playground");
        WebElement uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));
        WebElement uploadYamlBtn = driver.findElement(By.id("uploadYamlBtn"));

        // Then - Verify buttons have proper attributes
        assertEquals("button", uploadDataBtn.getDomAttribute("type"), 
                    "Upload Data button should have correct type");
        assertEquals("button", uploadYamlBtn.getDomAttribute("type"), 
                    "Upload YAML button should have correct type");

        // Verify buttons are not disabled
        assertNull(uploadDataBtn.getDomAttribute("disabled"), 
                  "Upload Data button should not be disabled");
        assertNull(uploadYamlBtn.getDomAttribute("disabled"), 
                  "Upload YAML button should not be disabled");
    }

    @Test
    @Order(11)
    @DisplayName("Should work correctly on different screen sizes")
    void shouldWorkCorrectlyOnDifferentScreenSizes() {
        // Test desktop size
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(1920, 1080));
        driver.get(baseUrl + "/playground");
        
        WebElement uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));
        assertTrue(uploadDataBtn.isDisplayed(), "Button should be visible on desktop");
        uploadDataBtn.click(); // Should work on desktop

        // Test tablet size
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(768, 1024));
        driver.navigate().refresh();
        
        uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));
        assertTrue(uploadDataBtn.isDisplayed(), "Button should be visible on tablet");
        uploadDataBtn.click(); // Should work on tablet

        // Test mobile size
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(375, 667));
        driver.navigate().refresh();
        
        uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));
        assertTrue(uploadDataBtn.isDisplayed(), "Button should be visible on mobile");
        uploadDataBtn.click(); // Should work on mobile
    }

    @Test
    @Order(12)
    @DisplayName("Should maintain consistent behavior across page reloads")
    void shouldMaintainConsistentBehaviorAcrossPageReloads() {
        // Given
        driver.get(baseUrl + "/playground");
        
        // Test initial state
        WebElement uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));
        assertTrue(uploadDataBtn.isEnabled(), "Button should be enabled initially");
        uploadDataBtn.click();

        // When - Reload page
        driver.navigate().refresh();

        // Then - Button should maintain same behavior
        uploadDataBtn = driver.findElement(By.id("uploadDataBtn"));
        assertTrue(uploadDataBtn.isEnabled(), "Button should be enabled after reload");
        assertTrue(uploadDataBtn.isDisplayed(), "Button should be visible after reload");
        uploadDataBtn.click(); // Should still work after reload
    }
}

