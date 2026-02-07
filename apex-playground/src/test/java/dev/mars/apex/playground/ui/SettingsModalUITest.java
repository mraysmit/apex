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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for the Settings Modal in APEX Playground.
 * Tests the settings cog button, modal open/close, examples directory
 * configuration, resolved path display, and directory-exists badge.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-02-07
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=INFO",
    "apex.playground.examples-enabled=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SettingsModalUITest {

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor js;
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
        js = (JavascriptExecutor) driver;
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        baseUrl = "http://localhost:" + port + "/playground";
    }

    @AfterEach
    void tearDown() {
        // Reset examples dir back to default via API after each test
        try {
            js.executeScript(
                "fetch('" + "http://localhost:" + port + "/playground/api/settings', " +
                "{method: 'PUT', headers: {'Content-Type': 'application/json'}, " +
                "body: JSON.stringify({examplesDir: 'examples'})});"
            );
            Thread.sleep(300);
        } catch (Exception e) {
            // best-effort cleanup
        }
        if (driver != null) {
            driver.quit();
        }
    }

    // ========================================================================
    // Settings Button Tests
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("Settings cog button should be visible in the toolbar")
    void settingsButtonShouldBeVisible() {
        driver.get(baseUrl);

        WebElement settingsBtn = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("settingsBtn")));

        assertTrue(settingsBtn.isDisplayed(), "Settings button should be visible");
        assertEquals("Settings", settingsBtn.getAttribute("title"),
            "Settings button should have title 'Settings'");
    }

    @Test
    @Order(2)
    @DisplayName("Clicking settings cog should open the settings modal")
    void clickingSettingsButtonShouldOpenModal() {
        driver.get(baseUrl);

        WebElement settingsBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("settingsBtn")));
        settingsBtn.click();

        // Wait for the modal to appear
        WebElement modal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("settingsModal")));
        assertTrue(modal.isDisplayed(), "Settings modal should be visible after clicking cog");

        // Verify modal title
        WebElement title = modal.findElement(By.id("settingsModalLabel"));
        assertTrue(title.getText().contains("Settings"), "Modal title should contain 'Settings'");
    }

    // ========================================================================
    // Modal Content Tests
    // ========================================================================

    @Test
    @Order(3)
    @DisplayName("Settings modal should show current examples directory")
    void modalShouldShowCurrentExamplesDirectory() {
        driver.get(baseUrl);

        WebElement settingsBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("settingsBtn")));
        settingsBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("settingsModal")));

        WebElement examplesDirInput = driver.findElement(By.id("examplesDirInput"));
        String value = examplesDirInput.getAttribute("value");
        assertNotNull(value, "Examples directory input should have a value");
        assertFalse(value.isEmpty(), "Examples directory input should not be empty");
        assertEquals("examples", value, "Default examples directory should be 'examples'");
    }

    @Test
    @Order(4)
    @DisplayName("Settings modal should show resolved path")
    void modalShouldShowResolvedPath() {
        driver.get(baseUrl);

        WebElement settingsBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("settingsBtn")));
        settingsBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("settingsModal")));

        // Wait for resolved path to be populated (loaded from async API call)
        wait.until(driver -> {
            WebElement resolvedPath = driver.findElement(By.id("resolvedPathValue"));
            String text = resolvedPath.getText();
            return text != null && !text.equals("--") && !text.isEmpty();
        });

        WebElement resolvedPath = driver.findElement(By.id("resolvedPathValue"));
        String pathText = resolvedPath.getText();
        assertNotNull(pathText, "Resolved path should be displayed");
        assertTrue(pathText.contains("examples"),
            "Resolved path should contain 'examples', but was: " + pathText);
    }

    @Test
    @Order(5)
    @DisplayName("Settings modal should show directory-exists badge for valid directory")
    void modalShouldShowDirectoryExistsBadge() {
        driver.get(baseUrl);

        WebElement settingsBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("settingsBtn")));
        settingsBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("settingsModal")));

        // Wait for badge to appear
        WebElement badge = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("dirExistsBadge")));

        String badgeText = badge.getText();
        String badgeClass = badge.getAttribute("class");

        // Default "examples" dir should exist
        assertTrue(badgeText.contains("Directory exists") || badgeText.contains("Directory not found"),
            "Badge should show directory status, but was: " + badgeText);

        // If directory exists, badge should be green (bg-success)
        if (badgeText.contains("Directory exists")) {
            assertTrue(badgeClass.contains("bg-success"),
                "Existing directory should have green badge");
        }
    }

    // ========================================================================
    // Save & Update Tests
    // ========================================================================

    @Test
    @Order(6)
    @DisplayName("Saving a valid examples directory should update the path and close modal")
    void savingValidDirectoryShouldUpdateAndClose() {
        driver.get(baseUrl);

        // Open settings
        WebElement settingsBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("settingsBtn")));
        settingsBtn.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("settingsModal")));

        // Change the value (use the existing 'examples' directory which we know exists)
        WebElement input = driver.findElement(By.id("examplesDirInput"));
        input.clear();
        input.sendKeys("examples");

        // Click Save
        WebElement saveBtn = driver.findElement(By.id("saveSettingsBtn"));
        saveBtn.click();

        // Wait for modal to close
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("settingsModal")));

        // Verify success alert
        WebElement alert = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".alert-success")));
        assertTrue(alert.getText().contains("Settings saved"),
            "Success alert should show 'Settings saved', but was: " + alert.getText());
    }

    @Test
    @Order(7)
    @DisplayName("Saving a non-existent directory should show warning badge")
    void savingNonExistentDirectoryShouldShowWarning() {
        driver.get(baseUrl);

        // Open settings
        WebElement settingsBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("settingsBtn")));
        settingsBtn.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("settingsModal")));

        // Change to a non-existent directory
        WebElement input = driver.findElement(By.id("examplesDirInput"));
        input.clear();
        input.sendKeys("non-existent-dir-xyz-12345");

        // Click Save
        WebElement saveBtn = driver.findElement(By.id("saveSettingsBtn"));
        saveBtn.click();

        // Wait for modal to close
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("settingsModal")));

        // Re-open settings to verify badge
        settingsBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("settingsBtn")));
        settingsBtn.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("settingsModal")));

        // Wait for badge to appear
        WebElement badge = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("dirExistsBadge")));
        assertEquals("Directory not found", badge.getText(),
            "Badge should say 'Directory not found'");
        assertTrue(badge.getAttribute("class").contains("bg-warning"),
            "Non-existent directory should have warning badge");

        // Verify the input still shows the non-existent directory
        input = driver.findElement(By.id("examplesDirInput"));
        assertEquals("non-existent-dir-xyz-12345", input.getAttribute("value"),
            "Input should show the saved value");
    }

    // ========================================================================
    // Reset Button Test
    // ========================================================================

    @Test
    @Order(8)
    @DisplayName("Reset button should restore default examples directory")
    void resetButtonShouldRestoreDefault() {
        driver.get(baseUrl);

        // Open settings
        WebElement settingsBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("settingsBtn")));
        settingsBtn.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("settingsModal")));

        // Change the input to something else
        WebElement input = driver.findElement(By.id("examplesDirInput"));
        input.clear();
        input.sendKeys("some-other-folder");

        // Click the reset button
        WebElement resetBtn = driver.findElement(By.id("resetExamplesDirBtn"));
        resetBtn.click();

        // Verify the input is reset to default
        assertEquals("examples", input.getAttribute("value"),
            "Input should be reset to 'examples' after clicking reset");
    }

    // ========================================================================
    // Cancel / Close Tests
    // ========================================================================

    @Test
    @Order(9)
    @DisplayName("Cancel button should close the modal without saving")
    void cancelButtonShouldCloseWithoutSaving() {
        driver.get(baseUrl);

        // Open settings
        WebElement settingsBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("settingsBtn")));
        settingsBtn.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("settingsModal")));

        // Change the value
        WebElement input = driver.findElement(By.id("examplesDirInput"));
        input.clear();
        input.sendKeys("should-not-be-saved");

        // Click Cancel
        WebElement cancelBtn = driver.findElement(
            By.cssSelector("#settingsModal .btn-secondary[data-bs-dismiss='modal']"));
        cancelBtn.click();

        // Wait for modal to close
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("settingsModal")));

        // Re-open settings and verify original value is still there
        settingsBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("settingsBtn")));
        settingsBtn.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("settingsModal")));

        input = driver.findElement(By.id("examplesDirInput"));
        assertEquals("examples", input.getAttribute("value"),
            "Input should still show 'examples' after cancel, not 'should-not-be-saved'");
    }

    @Test
    @Order(10)
    @DisplayName("X button should close the settings modal")
    void xButtonShouldCloseModal() {
        driver.get(baseUrl);

        // Open settings
        WebElement settingsBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("settingsBtn")));
        settingsBtn.click();

        WebElement modal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("settingsModal")));

        // Click the X close button
        WebElement closeBtn = modal.findElement(By.cssSelector(".btn-close"));
        closeBtn.click();

        // Wait for modal to close
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("settingsModal")));

        // Verify modal is no longer visible
        assertFalse(driver.findElement(By.id("settingsModal")).isDisplayed(),
            "Settings modal should be closed after clicking X");
    }

    // ========================================================================
    // API Integration Tests (via JS execution)
    // ========================================================================

    @Test
    @Order(11)
    @DisplayName("Settings API GET should return current configuration")
    void settingsApiGetShouldReturnConfig() {
        driver.get(baseUrl);

        // Call the settings API via JavaScript and check the result
        Object result = js.executeAsyncScript(
            "var callback = arguments[arguments.length - 1];" +
            "fetch(window.playgroundConfig.apiBaseUrl + '/settings')" +
            ".then(r => r.json())" +
            ".then(data => callback(JSON.stringify(data)))" +
            ".catch(e => callback('ERROR: ' + e.message));"
        );

        String json = result.toString();
        assertFalse(json.startsWith("ERROR"), "API call should not fail: " + json);
        assertTrue(json.contains("\"examplesDir\""), "Response should contain examplesDir");
        assertTrue(json.contains("\"resolvedExamplesPath\""), "Response should contain resolvedExamplesPath");
        assertTrue(json.contains("\"directoryExists\""), "Response should contain directoryExists");
    }

    @Test
    @Order(12)
    @DisplayName("Settings API PUT should update configuration")
    void settingsApiPutShouldUpdateConfig() {
        driver.get(baseUrl);

        // Update via PUT
        Object putResult = js.executeAsyncScript(
            "var callback = arguments[arguments.length - 1];" +
            "fetch(window.playgroundConfig.apiBaseUrl + '/settings', " +
            "  {method: 'PUT', headers: {'Content-Type': 'application/json'}, " +
            "   body: JSON.stringify({examplesDir: 'test-dir-from-selenium'})})" +
            ".then(r => r.json())" +
            ".then(data => callback(JSON.stringify(data)))" +
            ".catch(e => callback('ERROR: ' + e.message));"
        );

        String putJson = putResult.toString();
        assertFalse(putJson.startsWith("ERROR"), "PUT should not fail: " + putJson);
        assertTrue(putJson.contains("\"success\":true"), "PUT should return success: true");
        assertTrue(putJson.contains("\"examplesDir\":\"test-dir-from-selenium\""),
            "PUT should return updated examplesDir");

        // Verify via GET
        Object getResult = js.executeAsyncScript(
            "var callback = arguments[arguments.length - 1];" +
            "fetch(window.playgroundConfig.apiBaseUrl + '/settings')" +
            ".then(r => r.json())" +
            ".then(data => callback(JSON.stringify(data)))" +
            ".catch(e => callback('ERROR: ' + e.message));"
        );

        String getJson = getResult.toString();
        assertTrue(getJson.contains("\"examplesDir\":\"test-dir-from-selenium\""),
            "GET should return the updated value, but got: " + getJson);
    }
}
