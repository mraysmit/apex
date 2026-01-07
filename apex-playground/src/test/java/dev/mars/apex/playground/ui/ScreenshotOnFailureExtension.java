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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * JUnit 5 extension that captures screenshots on test failures for Selenium tests.
 * Screenshots are saved to target/selenium-screenshots/ with test name and timestamp.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-12-19
 * @version 1.0
 */
public class ScreenshotOnFailureExtension implements TestWatcher {

    private static final Logger logger = LoggerFactory.getLogger(ScreenshotOnFailureExtension.class);
    private static final String SCREENSHOT_DIR = "target/selenium-screenshots";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        captureScreenshot(context);
    }

    /**
     * Capture a screenshot from the WebDriver instance in the test context.
     *
     * @param context the test execution context
     */
    private void captureScreenshot(ExtensionContext context) {
        Optional<Object> testInstance = context.getTestInstance();
        
        if (testInstance.isEmpty()) {
            logger.warn("No test instance available for screenshot capture");
            return;
        }

        Object instance = testInstance.get();
        
        // Check if test class extends BaseYamlImportSeleniumTest
        if (!(instance instanceof BaseYamlImportSeleniumTest)) {
            logger.debug("Test class does not extend BaseYamlImportSeleniumTest, skipping screenshot");
            return;
        }

        BaseYamlImportSeleniumTest test = (BaseYamlImportSeleniumTest) instance;
        WebDriver driver = test.getDriver();

        if (driver == null) {
            logger.warn("WebDriver is null, cannot capture screenshot");
            return;
        }

        try {
            // Create screenshot directory if it doesn't exist
            Path screenshotDir = Paths.get(SCREENSHOT_DIR);
            if (!Files.exists(screenshotDir)) {
                Files.createDirectories(screenshotDir);
            }

            // Generate filename with test name and timestamp
            String testName = context.getDisplayName().replaceAll("[^a-zA-Z0-9.-]", "_");
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String filename = String.format("%s-%s.png", testName, timestamp);
            Path screenshotPath = screenshotDir.resolve(filename);

            // Capture screenshot
            if (driver instanceof TakesScreenshot) {
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                Files.write(screenshotPath, screenshot);
                logger.info("Screenshot captured: {}", screenshotPath.toAbsolutePath());
            } else {
                logger.warn("WebDriver does not support screenshot capture");
            }

        } catch (IOException e) {
            logger.error("Failed to capture screenshot", e);
        }
    }
}
