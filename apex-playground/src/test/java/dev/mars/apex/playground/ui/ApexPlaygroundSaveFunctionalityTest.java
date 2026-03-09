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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for APEX Playground Save Functionality.
 * Uses @TestInstance(PER_CLASS) to reuse a single browser across all test methods.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-28
 * @version 1.1
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=INFO",
    "apex.playground.examples-enabled=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApexPlaygroundSaveFunctionalityTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    @LocalServerPort
    private int port;

    @BeforeAll
    void setupBrowser() {
        io.github.bonigarcia.wdm.WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        baseUrl = "http://localhost:" + port + "/playground";
    }

    @AfterAll
    void tearDownBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Verify UI starts empty")
    void verifyUiStartsEmpty() {
        driver.get(baseUrl);
        
        WebElement sourceDataEditor = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("sourceDataEditor")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("yamlRulesEditor")));
        
        assertEquals("", sourceDataEditor.getAttribute("value"), "Source Data editor should be empty on startup");
        assertEquals("", CodeMirrorTestHelper.getYamlContent(driver), "YAML Rules editor should be empty on startup");
    }

    @Test
    @Order(2)
    @DisplayName("Verify Save YAML saves to server when example is loaded")
    void verifySaveYamlSavesToServer() {
        driver.get(baseUrl);
        
        // 1. Load an example
        WebElement loadExampleBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("loadExampleBtn")));
        loadExampleBtn.click();
        
        // Wait for modal and click the first example (assuming at least one exists)
        // Note: This depends on the example structure. We might need to be more specific.
        // For now, we'll try to find an example item.
        WebElement exampleItem = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".example-item")));
        String exampleName = exampleItem.findElement(By.tagName("h6")).getText();
        exampleItem.click();
        
        // Wait for editors to be populated
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("yamlRulesEditor")));
        // Wait for CodeMirror to have content
        wait.until(d -> !CodeMirrorTestHelper.getYamlContent(d).isEmpty());
        
        // 2. Modify the YAML content
        String originalContent = CodeMirrorTestHelper.getYamlContent(driver);
        String newContent = originalContent + "\n# Modified by Selenium Test";
        CodeMirrorTestHelper.setYamlContent(driver, newContent);
        
        // 3. Click Save YAML
        WebElement saveYamlBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("saveYamlBtn")));
        saveYamlBtn.click();
        
        // 4. Verify Success Alert
        // Note: There might be a "loaded successfully" alert present. We need to wait for the "saved" message.
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".alert-success"), "saved successfully"));
        
        WebElement alert = driver.findElement(By.cssSelector(".alert-success"));
        assertTrue(alert.getText().contains(exampleName), "Alert should contain example name");
    }
    
    @Test
    @Order(3)
    @DisplayName("Verify Save Data saves to server when example is loaded")
    void verifySaveDataSavesToServer() {
        driver.get(baseUrl);
        
        // 1. Load an example
        WebElement loadExampleBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("loadExampleBtn")));
        loadExampleBtn.click();
        
        WebElement exampleItem = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".example-item")));
        String exampleName = exampleItem.findElement(By.tagName("h6")).getText();
        exampleItem.click();
        
        // Wait for editors to be populated
        WebElement sourceDataEditor = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("sourceDataEditor")));
        wait.until(ExpectedConditions.not(ExpectedConditions.attributeToBe(sourceDataEditor, "value", "")));
        
        // 2. Modify the Data content
        String originalContent = sourceDataEditor.getAttribute("value");
        // Simple modification assuming JSON
        sourceDataEditor.sendKeys(" "); 
        
        // 3. Click Save Data
        WebElement saveDataBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("saveDataBtn")));
        saveDataBtn.click();
        
        // 4. Verify Success Alert
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".alert-success"), "saved successfully"));
        
        WebElement alert = driver.findElement(By.cssSelector(".alert-success"));
        assertTrue(alert.getText().contains(exampleName), "Alert should contain example name");
    }
}
