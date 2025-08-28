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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class demonstrating screenshot capabilities for the APEX Playground.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
/**
 * Test class demonstrating screenshot capabilities for the APEX Playground.
 * This class shows various ways to capture screenshots of the playground interface.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=WARN",
    "logging.level.org.springframework=WARN"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("APEX Playground Screenshot Tests")
class PlaygroundScreenshotTest {

    @LocalServerPort
    private int port;

    private PlaygroundScreenshotUtil screenshotUtil;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        screenshotUtil = new PlaygroundScreenshotUtil(baseUrl);
    }

    @AfterEach
    void tearDown() {
        if (screenshotUtil != null) {
            screenshotUtil.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Take basic playground screenshot")
    void testBasicPlaygroundScreenshot() throws IOException {
        String screenshotPath = screenshotUtil.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
            "basic_playground"
        );
        
        assertNotNull(screenshotPath);
        assertTrue(screenshotPath.contains("playground_chrome_desktop_view_basic_playground"));
        System.out.println("Basic screenshot saved to: " + screenshotPath);
    }

    @Test
    @Order(2)
    @DisplayName("Take screenshot with sample data loaded")
    void testScreenshotWithSampleData() throws IOException {
        // Sample JSON data
        String sampleJson = """
            {
              "customer": {
                "id": "CUST001",
                "name": "John Doe",
                "email": "john.doe@example.com",
                "status": "ACTIVE"
              },
              "order": {
                "id": "ORD001",
                "amount": 150.00,
                "currency": "USD"
              }
            }""";

        // Sample YAML rules
        String sampleYaml = """
            rules:
              - name: "Customer Validation"
                condition: "customer.status == 'ACTIVE'"
                actions:
                  - type: "enrich"
                    field: "customer.validated"
                    value: true
              
              - name: "Order Processing"
                condition: "order.amount > 100"
                actions:
                  - type: "enrich"
                    field: "order.priority"
                    value: "HIGH"
            """;

        PlaygroundScreenshotUtil.PlaygroundData data = 
            new PlaygroundScreenshotUtil.PlaygroundData(sampleJson, sampleYaml, true);

        String screenshotPath = screenshotUtil.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
            "with_sample_data",
            data,
            true // headless
        );
        
        assertNotNull(screenshotPath);
        assertTrue(screenshotPath.contains("with_sample_data"));
        System.out.println("Screenshot with sample data saved to: " + screenshotPath);
    }

    @Test
    @Order(3)
    @DisplayName("Take mobile responsive screenshot")
    void testMobileResponsiveScreenshot() throws IOException {
        String screenshotPath = screenshotUtil.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.MOBILE_VIEW,
            "mobile_responsive"
        );
        
        assertNotNull(screenshotPath);
        assertTrue(screenshotPath.contains("mobile_view"));
        System.out.println("Mobile screenshot saved to: " + screenshotPath);
    }

    @Test
    @Order(4)
    @DisplayName("Take tablet responsive screenshot")
    void testTabletResponsiveScreenshot() throws IOException {
        String screenshotPath = screenshotUtil.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.TABLET_VIEW,
            "tablet_responsive"
        );
        
        assertNotNull(screenshotPath);
        assertTrue(screenshotPath.contains("tablet_view"));
        System.out.println("Tablet screenshot saved to: " + screenshotPath);
    }

    @Test
    @Order(5)
    @DisplayName("Take screenshots across multiple browsers")
    void testMultipleBrowserScreenshots() throws IOException {
        PlaygroundScreenshotUtil.Browser[] browsers = {
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.Browser.FIREFOX,
            PlaygroundScreenshotUtil.Browser.EDGE
        };

        for (PlaygroundScreenshotUtil.Browser browser : browsers) {
            try {
                // Create new util instance for each browser to avoid conflicts
                PlaygroundScreenshotUtil util = new PlaygroundScreenshotUtil(baseUrl);
                
                String screenshotPath = util.takeScreenshot(
                    browser,
                    PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
                    "cross_browser_test",
                    null,
                    true // headless
                );
                
                assertNotNull(screenshotPath);
                assertTrue(screenshotPath.contains(browser.name().toLowerCase()));
                System.out.println(browser + " screenshot saved to: " + screenshotPath);
                
                util.close();
            } catch (Exception e) {
                System.out.println("Skipping " + browser + " - not available: " + e.getMessage());
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("Take element-specific screenshots")
    void testElementSpecificScreenshots() throws IOException {
        // Test taking screenshots of specific UI elements
        String[] elementIds = {
            "sourceDataEditor",
            "yamlRulesEditor", 
            "processBtn",
            "yamlStatus"
        };

        for (String elementId : elementIds) {
            try {
                PlaygroundScreenshotUtil util = new PlaygroundScreenshotUtil(baseUrl);
                
                String screenshotPath = util.takeScreenshot(
                    PlaygroundScreenshotUtil.Browser.CHROME,
                    PlaygroundScreenshotUtil.ScreenshotType.ELEMENT_SPECIFIC,
                    elementId,
                    null,
                    true
                );
                
                assertNotNull(screenshotPath);
                assertTrue(screenshotPath.contains(elementId));
                System.out.println("Element " + elementId + " screenshot saved to: " + screenshotPath);
                
                util.close();
            } catch (Exception e) {
                System.out.println("Could not capture element " + elementId + ": " + e.getMessage());
            }
        }
    }

    @Test
    @Order(7)
    @DisplayName("Take screenshot workflow demonstration")
    void testWorkflowScreenshots() throws IOException {
        // Demonstrate a complete workflow with screenshots at each step
        PlaygroundScreenshotUtil util = new PlaygroundScreenshotUtil(baseUrl);
        
        // Step 1: Empty playground
        String step1 = util.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
            "workflow_step1_empty",
            null,
            true
        );
        System.out.println("Workflow Step 1 (Empty): " + step1);

        // Step 2: Data loaded but not processed
        String jsonData = """
            {
              "user": {
                "name": "Alice Smith",
                "age": 30,
                "department": "Engineering"
              }
            }""";

        String yamlRules = """
            rules:
              - name: "Age Validation"
                condition: "user.age >= 18"
                actions:
                  - type: "enrich"
                    field: "user.adult"
                    value: true
            """;

        PlaygroundScreenshotUtil.PlaygroundData dataOnly = 
            new PlaygroundScreenshotUtil.PlaygroundData(jsonData, yamlRules, false);

        String step2 = util.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
            "workflow_step2_data_loaded",
            dataOnly,
            true
        );
        System.out.println("Workflow Step 2 (Data Loaded): " + step2);

        // Step 3: Data processed with results
        PlaygroundScreenshotUtil.PlaygroundData dataProcessed = 
            new PlaygroundScreenshotUtil.PlaygroundData(jsonData, yamlRules, true);

        String step3 = util.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
            "workflow_step3_processed",
            dataProcessed,
            true
        );
        System.out.println("Workflow Step 3 (Processed): " + step3);

        util.close();
        
        assertNotNull(step1);
        assertNotNull(step2);
        assertNotNull(step3);
    }
}

