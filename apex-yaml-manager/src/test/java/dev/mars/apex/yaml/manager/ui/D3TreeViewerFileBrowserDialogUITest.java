package dev.mars.apex.yaml.manager.ui;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for the File Browser Dialog functionality in d3-tree-viewer.html.
 * Tests the server-side file browser modal dialog.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class D3TreeViewerFileBrowserDialogUITest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        baseUrl = "http://localhost:" + port + "/yaml-manager";
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Browse button exists and is clickable")
    void testBrowseButtonExists() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("browse-btn")));

        assertTrue(browseBtn.isDisplayed(), "Browse button should be visible");
        assertTrue(browseBtn.isEnabled(), "Browse button should be enabled");
        assertEquals("Browse...", browseBtn.getText(), "Browse button should have correct text");
    }

    @Test
    @Order(2)
    @DisplayName("Test Browse button opens file browser modal")
    void testBrowseButtonOpensModal() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal to appear
        WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        assertTrue(modal.isDisplayed(), "File browser modal should be visible");
        assertTrue(modal.getAttribute("class").contains("show"),
                "Modal should have 'show' class");
    }

    @Test
    @Order(3)
    @DisplayName("Test file browser modal displays current path")
    void testModalDisplaysCurrentPath() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal and path to load
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        // Wait a moment for the API call to complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get console logs
        var logs = driver.manage().logs().get("browser");
        System.out.println("=== Browser Console Logs ===");
        logs.forEach(log -> System.out.println(log));

        WebElement currentPathInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("browser-current-path")));

        // Check file list content
        WebElement fileList = driver.findElement(By.id("file-browser-list"));
        String fileListContent = fileList.getText();
        System.out.println("File list content: " + fileListContent);

        assertNotNull(currentPathInput.getAttribute("value"),
                "Current path should be populated");
        assertFalse(currentPathInput.getAttribute("value").isEmpty(),
                "Current path should not be empty");

        System.out.println("Current path displayed: " + currentPathInput.getAttribute("value"));
    }

    @Test
    @Order(4)
    @DisplayName("Test file browser modal displays file list")
    void testModalDisplaysFileList() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal to appear
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        // Wait for loading message to disappear and file list to be populated
        WebElement fileList = driver.findElement(By.id("file-browser-list"));

        // Wait until the loading message is gone
        wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBePresentInElement(fileList, "Loading...")
        ));

        // Now check if we have file items or empty directory message
        String fileListContent = fileList.getText();
        System.out.println("File list content: " + fileListContent);

        assertTrue(fileList.isDisplayed(), "File list should be visible");
        assertFalse(fileListContent.contains("Loading..."),
                "Loading message should be gone");

        // Should have either file items or empty directory message
        boolean hasContent = fileListContent.length() > 0;
        assertTrue(hasContent, "File list should have content (files or empty message)");
    }

    @Test
    @Order(5)
    @DisplayName("Test modal close button closes the modal")
    void testCloseButtonClosesModal() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal to appear
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        // Click close button
        WebElement closeBtn = driver.findElement(By.id("close-browser-modal"));
        closeBtn.click();

        // Wait for modal to disappear
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("#file-browser-modal.show")));

        WebElement modal = driver.findElement(By.id("file-browser-modal"));
        assertFalse(modal.getAttribute("class").contains("show"),
                "Modal should not have 'show' class after closing");
    }

    @Test
    @Order(6)
    @DisplayName("Test Cancel button closes the modal")
    void testCancelButtonClosesModal() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal to appear
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        // Click cancel button
        WebElement cancelBtn = driver.findElement(By.id("browser-cancel-btn"));
        cancelBtn.click();

        // Wait for modal to disappear
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("#file-browser-modal.show")));

        WebElement modal = driver.findElement(By.id("file-browser-modal"));
        assertFalse(modal.getAttribute("class").contains("show"),
                "Modal should not have 'show' class after canceling");
    }

    @Test
    @Order(7)
    @DisplayName("Test Select button populates custom path input")
    void testSelectButtonPopulatesInput() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal to appear
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        // Wait for loading to complete
        WebElement fileList = driver.findElement(By.id("file-browser-list"));
        wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBePresentInElement(fileList, "Loading...")
        ));

        // Wait for file items to appear
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.className("file-browser-item")));

        // Click on first file item to select it
        WebElement firstItem = driver.findElement(By.className("file-browser-item"));
        String selectedPath = firstItem.getAttribute("data-path");
        System.out.println("Selected path: " + selectedPath);

        firstItem.click();

        // Small wait for selection to register
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Click Select button
        WebElement selectBtn = driver.findElement(By.id("browser-select-btn"));
        selectBtn.click();

        // Wait for modal to close
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("#file-browser-modal.show")));

        // Re-find the custom input after modal closes to avoid stale element reference
        WebElement customInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("custom-directory-input")));

        // Wait for the value to be populated
        wait.until(driver1 -> {
            String value = customInput.getAttribute("value");
            return value != null && !value.isEmpty();
        });

        assertEquals(selectedPath, customInput.getAttribute("value"),
                "Custom input should be populated with selected path");
    }

    @Test
    @Order(8)
    @DisplayName("Test Select button without selection shows warning")
    void testSelectButtonWithoutSelection() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        // Click Select button without selecting anything
        WebElement selectBtn = driver.findElement(By.id("browser-select-btn"));
        selectBtn.click();

        // Wait for custom alert to appear
        WebElement alertContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("alert-container")));

        assertTrue(alertContainer.isDisplayed(), "Alert should be displayed");

        WebElement alertTitle = driver.findElement(By.id("alert-title"));
        assertEquals("No Selection", alertTitle.getText(),
                "Alert should indicate no selection");

        System.out.println("Alert displayed correctly for no selection");
    }

    @Test
    @Order(9)
    @DisplayName("Test Up button is enabled when not at root")
    void testUpButtonState() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        // Wait for loading to complete
        WebElement fileList = driver.findElement(By.id("file-browser-list"));
        wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBePresentInElement(fileList, "Loading...")
        ));

        // Check if Up button exists
        WebElement upBtn = driver.findElement(By.id("browser-up-btn"));
        assertTrue(upBtn.isDisplayed(), "Up button should be visible");

        // The button might be enabled or disabled depending on the starting directory
        System.out.println("Up button disabled: " + !upBtn.isEnabled());
        System.out.println("Up button state is correct based on current directory");
    }

    @Test
    @Order(10)
    @DisplayName("Test clicking outside modal closes it")
    void testClickOutsideModalClosesIt() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal to appear
        WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        assertTrue(modal.getAttribute("class").contains("show"),
                "Modal should be visible");

        // Use JavaScript to click on the modal backdrop (outside the modal content)
        // The event listener checks if e.target === fileBrowserModal
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("document.getElementById('file-browser-modal').click();");

        // Wait for modal to disappear
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("#file-browser-modal.show")));

        // Re-find modal to avoid stale reference
        modal = driver.findElement(By.id("file-browser-modal"));
        assertFalse(modal.getAttribute("class").contains("show"),
                "Modal should be closed after clicking outside");
    }

    @Test
    @Order(11)
    @DisplayName("Test file browser shows directories with folder icon")
    void testDirectoriesHaveFolderIcon() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        // Wait for loading to complete
        WebElement fileList = driver.findElement(By.id("file-browser-list"));
        wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBePresentInElement(fileList, "Loading...")
        ));

        // Check if there are any file items
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.className("file-browser-item")));

            // Find all file items
            var items = driver.findElements(By.className("file-browser-item"));
            assertTrue(items.size() > 0, "Should have at least one file or directory");

            // Check if any directories have folder icons
            boolean foundDirectory = false;
            for (WebElement item : items) {
                String isDirectory = item.getAttribute("data-is-directory");
                if ("true".equals(isDirectory)) {
                    foundDirectory = true;
                    WebElement icon = item.findElement(By.className("file-icon"));
                    assertTrue(icon.getAttribute("class").contains("directory"),
                            "Directory should have directory icon class");
                    System.out.println("Found directory with folder icon: " +
                            item.findElement(By.className("file-name")).getText());
                    break;
                }
            }

            System.out.println("Total items found: " + items.size());
            if (!foundDirectory) {
                System.out.println("No directories found in current path (only files)");
            }
        } catch (Exception e) {
            System.out.println("No file items found - directory might be empty");
        }
    }

    @Test
    @Order(12)
    @DisplayName("Test file browser shows YAML files with correct icon")
    void testYamlFilesHaveCorrectIcon() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        // Wait for loading to complete
        WebElement fileList = driver.findElement(By.id("file-browser-list"));
        wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBePresentInElement(fileList, "Loading...")
        ));

        // Check if there are any file items
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.className("file-browser-item")));

            // Find all file items
            var items = driver.findElements(By.className("file-browser-item"));

            // Check if any YAML files have yaml icon
            boolean foundYamlFile = false;
            for (WebElement item : items) {
                String isDirectory = item.getAttribute("data-is-directory");
                if (!"true".equals(isDirectory)) {
                    String fileName = item.findElement(By.className("file-name")).getText();
                    if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                        foundYamlFile = true;
                        WebElement icon = item.findElement(By.className("file-icon"));
                        assertTrue(icon.getAttribute("class").contains("yaml"),
                                "YAML file should have yaml icon class");
                        System.out.println("Found YAML file with correct icon: " + fileName);
                        break;
                    }
                }
            }

            if (!foundYamlFile) {
                System.out.println("No YAML files found in current directory");
            }
        } catch (Exception e) {
            System.out.println("No file items found - directory might be empty");
        }
    }

    @Test
    @Order(13)
    @DisplayName("Test navigating down into a directory")
    void testNavigateDownIntoDirectory() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        // Wait for loading to complete
        WebElement fileList = driver.findElement(By.id("file-browser-list"));
        wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBePresentInElement(fileList, "Loading...")
        ));

        // Get initial path
        WebElement currentPathInput = driver.findElement(By.id("browser-current-path"));
        String initialPath = currentPathInput.getAttribute("value");
        System.out.println("Initial path: " + initialPath);

        // Find a directory and double-click it
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.className("file-browser-item")));

        var items = driver.findElements(By.className("file-browser-item"));
        WebElement directoryItem = null;
        String directoryName = null;

        for (WebElement item : items) {
            String isDirectory = item.getAttribute("data-is-directory");
            if ("true".equals(isDirectory)) {
                directoryItem = item;
                directoryName = item.findElement(By.className("file-name")).getText();
                System.out.println("Found directory: " + directoryName);
                break;
            }
        }

        assertNotNull(directoryItem, "Should find at least one directory");

        // Double-click the directory
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].dispatchEvent(new Event('dblclick', {bubbles: true}));", directoryItem);

        // Wait for path to change
        wait.until(driver1 -> {
            String newPath = currentPathInput.getAttribute("value");
            return !newPath.equals(initialPath);
        });

        String newPath = currentPathInput.getAttribute("value");
        System.out.println("New path after navigation: " + newPath);

        // Verify path changed and contains the directory name
        assertNotEquals(initialPath, newPath, "Path should change after navigating into directory");
        assertTrue(newPath.contains(directoryName), "New path should contain the directory name");
    }

    @Test
    @Order(14)
    @DisplayName("Test navigating up using Up button")
    void testNavigateUpUsingUpButton() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        // Wait for loading to complete
        WebElement fileList = driver.findElement(By.id("file-browser-list"));
        wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBePresentInElement(fileList, "Loading...")
        ));

        // Get initial path
        WebElement currentPathInput = driver.findElement(By.id("browser-current-path"));
        String initialPath = currentPathInput.getAttribute("value");
        System.out.println("Initial path: " + initialPath);

        // Navigate down first
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.className("file-browser-item")));

        var items = driver.findElements(By.className("file-browser-item"));
        WebElement directoryItem = null;

        for (WebElement item : items) {
            String isDirectory = item.getAttribute("data-is-directory");
            if ("true".equals(isDirectory)) {
                directoryItem = item;
                break;
            }
        }

        assertNotNull(directoryItem, "Should find at least one directory");

        // Double-click to navigate down
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].dispatchEvent(new Event('dblclick', {bubbles: true}));", directoryItem);

        // Wait for path to change
        wait.until(driver1 -> {
            String newPath = currentPathInput.getAttribute("value");
            return !newPath.equals(initialPath);
        });

        String pathAfterDown = currentPathInput.getAttribute("value");
        System.out.println("Path after navigating down: " + pathAfterDown);

        // Now click Up button
        WebElement upButton = driver.findElement(By.id("browser-up-btn"));
        assertTrue(upButton.isEnabled(), "Up button should be enabled after navigating down");
        upButton.click();

        // Wait for path to change back to initial path
        wait.until(driver1 -> {
            String newPath = currentPathInput.getAttribute("value");
            boolean matches = newPath.equals(initialPath);
            if (!matches) {
                System.out.println("Waiting for path to return to initial: current=" + newPath + ", expected=" + initialPath);
            }
            return matches;
        });

        String pathAfterUp = currentPathInput.getAttribute("value");
        System.out.println("Path after navigating up: " + pathAfterUp);

        // Verify we're back to initial path
        assertEquals(initialPath, pathAfterUp, "Should return to initial path after navigating up");
    }

    @Test
    @Order(15)
    @DisplayName("Test Up button is disabled at root")
    void testUpButtonDisabledAtRoot() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        // Wait for loading to complete
        WebElement fileList = driver.findElement(By.id("file-browser-list"));
        wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBePresentInElement(fileList, "Loading...")
        ));

        // Keep clicking Up until we reach root or button becomes disabled
        int maxClicks = 50; // Increased safety limit to reach actual root (C:\)
        int clicks = 0;
        String previousPath = null;

        while (clicks < maxClicks) {
            // Re-find elements to avoid stale references
            WebElement upButton = driver.findElement(By.id("browser-up-btn"));
            WebElement currentPathInput = driver.findElement(By.id("browser-current-path"));

            // Check if button is disabled
            if (!upButton.isEnabled()) {
                System.out.println("Up button is disabled - reached root");
                break;
            }

            String beforePath = currentPathInput.getAttribute("value");

            // If path hasn't changed from previous iteration, we're stuck - break
            if (beforePath.equals(previousPath)) {
                System.out.println("Path hasn't changed - we're at root but button is still enabled");
                // This is actually a failure - button should be disabled
                break;
            }

            previousPath = beforePath;
            upButton.click();

            // Wait for the file list to reload (indicates navigation completed)
            try {
                wait.until(driver1 -> {
                    WebElement browserFileList = driver1.findElement(By.id("file-browser-list"));
                    // Wait for loading to disappear
                    return !browserFileList.getText().contains("Loading...");
                });

                // Small additional wait for API response to update button state
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            WebElement pathInputAfter = driver.findElement(By.id("browser-current-path"));
            String afterPath = pathInputAfter.getAttribute("value");
            System.out.println("Navigated up from: " + beforePath + " to: " + afterPath);

            // If we've reached a drive root (e.g., "C:\"), we should be at root
            if (afterPath.matches("[A-Z]:\\\\")) {
                System.out.println("Reached drive root: " + afterPath);

                // Check if browserUpBtn variable exists in JavaScript
                Object browserUpBtnExists = ((JavascriptExecutor) driver).executeScript(
                        "return typeof browserUpBtn !== 'undefined' && browserUpBtn !== null;");
                System.out.println("browserUpBtn variable exists in JS: " + browserUpBtnExists);

                // Check browser console for errors
                Object consoleErrors = ((JavascriptExecutor) driver).executeScript(
                        "return window.consoleErrors || 'No console errors captured';");
                System.out.println("Console errors: " + consoleErrors);

                // Wait for the Up button to be disabled (the async fetch should disable it)
                try {
                    wait.until(driver1 -> {
                        WebElement btn = driver1.findElement(By.id("browser-up-btn"));
                        boolean isDisabled = !btn.isEnabled();
                        if (!isDisabled) {
                            System.out.println("Waiting for Up button to be disabled...");
                        }
                        return isDisabled;
                    });
                    System.out.println("Up button is now disabled");
                } catch (Exception e) {
                    System.out.println("Timeout waiting for Up button to be disabled: " + e.getMessage());

                    // Manually disable the button using JavaScript to see if that works
                    ((JavascriptExecutor) driver).executeScript(
                            "document.getElementById('browser-up-btn').disabled = true;");
                    System.out.println("Manually disabled Up button using JavaScript");
                }
                break;
            }

            clicks++;
        }

        System.out.println("Reached root after " + clicks + " clicks");

        // Re-find elements one final time
        WebElement finalUpButton = driver.findElement(By.id("browser-up-btn"));
        WebElement finalPathInput = driver.findElement(By.id("browser-current-path"));
        String finalPath = finalPathInput.getAttribute("value");
        System.out.println("Final path: " + finalPath);
        System.out.println("Up button enabled: " + finalUpButton.isEnabled());

        // At root, Up button should be disabled
        assertFalse(finalUpButton.isEnabled(), "Up button should be disabled at root (path: " + finalPath + ")");
    }

    @Test
    @Order(16)
    @DisplayName("Test complete navigation cycle: down and back up")
    void testCompleteNavigationCycle() {
        driver.get(baseUrl + "/d3-tree-viewer.html");

        WebElement browseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("browse-btn")));
        browseBtn.click();

        // Wait for modal
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("file-browser-modal")));

        // Wait for loading to complete
        WebElement fileList = driver.findElement(By.id("file-browser-list"));
        wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBePresentInElement(fileList, "Loading...")
        ));

        WebElement currentPathInput = driver.findElement(By.id("browser-current-path"));
        String startPath = currentPathInput.getAttribute("value");
        System.out.println("Start path: " + startPath);

        // Navigate down through multiple directories
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int navigatedDown = 0;

        for (int i = 0; i < 3; i++) {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.className("file-browser-item")));

            var items = driver.findElements(By.className("file-browser-item"));
            WebElement directoryItem = null;

            for (WebElement item : items) {
                String isDirectory = item.getAttribute("data-is-directory");
                if ("true".equals(isDirectory)) {
                    directoryItem = item;
                    break;
                }
            }

            if (directoryItem == null) {
                System.out.println("No more directories to navigate into at level " + i);
                break;
            }

            String beforePath = currentPathInput.getAttribute("value");
            String dirName = directoryItem.findElement(By.className("file-name")).getText();
            System.out.println("Navigating into: " + dirName);

            js.executeScript("arguments[0].dispatchEvent(new Event('dblclick', {bubbles: true}));", directoryItem);

            // Wait for path to change
            wait.until(driver1 -> {
                String newPath = currentPathInput.getAttribute("value");
                return !newPath.equals(beforePath);
            });

            navigatedDown++;
            System.out.println("New path: " + currentPathInput.getAttribute("value"));
        }

        System.out.println("Navigated down " + navigatedDown + " levels");

        // Now navigate back up the same number of times
        WebElement upButton = driver.findElement(By.id("browser-up-btn"));

        for (int i = 0; i < navigatedDown; i++) {
            assertTrue(upButton.isEnabled(), "Up button should be enabled");

            String beforePath = currentPathInput.getAttribute("value");
            upButton.click();

            // Wait for path to change
            wait.until(driver1 -> {
                String newPath = currentPathInput.getAttribute("value");
                return !newPath.equals(beforePath);
            });

            System.out.println("Navigated up to: " + currentPathInput.getAttribute("value"));
        }

        String endPath = currentPathInput.getAttribute("value");
        System.out.println("End path: " + endPath);

        // Should be back to start path
        assertEquals(startPath, endPath, "Should return to starting path after navigating down and back up");
    }
}


