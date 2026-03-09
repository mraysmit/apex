/*
 * Copyright (c) 2025 APEX Rules Engine Contributors
 * Licensed under the Apache License, Version 2.0
 */
package dev.mars.apex.playground.ui;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Utility helper for interacting with CodeMirror-wrapped editors in Selenium tests.
 * <p>
 * The YAML Rules Editor is wrapped by CodeMirror 5, which hides the original textarea.
 * Standard Selenium sendKeys/clear won't work on hidden elements. This helper detects
 * CodeMirror-wrapped elements and uses JavaScript to interact with them.
 */
public final class CodeMirrorTestHelper {

    private CodeMirrorTestHelper() {
        // Utility class
    }

    /**
     * Set text content in an element, handling CodeMirror-wrapped textareas.
     * If the element is the YAML editor (id=yamlRulesEditor), uses JavaScript
     * to call the page's setYamlContent() function. Otherwise uses standard
     * Selenium clear + sendKeys.
     *
     * @param driver the WebDriver instance
     * @param element the target element
     * @param text the text to enter
     */
    public static void clearAndEnterText(WebDriver driver, WebElement element, String text) {
        String elementId = element.getDomAttribute("id");
        if ("yamlRulesEditor".equals(elementId)) {
            setYamlContent(driver, text);
        } else {
            element.clear();
            element.sendKeys(text);
        }
    }

    /**
     * Set YAML editor content via the page's setYamlContent() JavaScript function.
     * Waits for CodeMirror to be initialized before setting content.
     */
    public static void setYamlContent(WebDriver driver, String content) {
        waitForCodeMirror(driver);
        ((JavascriptExecutor) driver).executeScript("setYamlContent(arguments[0])", content);
    }

    /**
     * Get YAML editor content via the page's getYamlContent() JavaScript function.
     * Waits for CodeMirror to be initialized before reading content.
     */
    public static String getYamlContent(WebDriver driver) {
        waitForCodeMirror(driver);
        return (String) ((JavascriptExecutor) driver).executeScript("return getYamlContent()");
    }

    /**
     * Wait for the CodeMirror editor to be initialized (yamlCmEditor != null).
     */
    private static void waitForCodeMirror(WebDriver driver) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .withMessage("Waiting for CodeMirror editor to initialize")
            .until(d -> {
                Object result = ((JavascriptExecutor) d).executeScript(
                    "return typeof yamlCmEditor !== 'undefined' && yamlCmEditor !== null");
                return Boolean.TRUE.equals(result);
            });
    }

    /**
     * Get text content from an element, handling CodeMirror-wrapped textareas.
     * If the element is the YAML editor, uses JavaScript getYamlContent().
     * Otherwise reads from the element's value property.
     *
     * @param driver the WebDriver instance
     * @param element the target element
     * @return the text content
     */
    public static String getTextContent(WebDriver driver, WebElement element) {
        String elementId = element.getDomAttribute("id");
        if ("yamlRulesEditor".equals(elementId)) {
            return getYamlContent(driver);
        }
        String value = element.getDomProperty("value");
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return element.getText();
    }
}
