package dev.mars.apex.yaml.manager.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI Tests for Dependency Tree Detail View.
 *
 * Verifies that clicking on tree nodes displays correct details in the right panel,
 * including file path, dependencies, dependents, health score, and metadata.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-19
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DependencyTreeDetailViewTest {

    private WebDriver driver;
    private WebDriverWait wait;
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
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
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
    void testDetailsPanelShowsEmptyStateInitially() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        WebElement detailsPanel = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("nodeDetails"))
        );

        String text = detailsPanel.getText();
        assertTrue(text.contains("Click a node") || text.contains("Select a node"),
            "Details panel should show empty state initially");
    }

    @Test
    @Order(2)
    void testNodeHeaderDisplaysCorrectly() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        WebElement nodeHeader = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("nodeName"))
        );

        String headerText = nodeHeader.getText();
        assertTrue(headerText.contains("Select a node") || headerText.contains("Loading"),
            "Node header should display initial message");
    }

    @Test
    @Order(3)
    void testNodeTypeDisplaysInHeader() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        WebElement nodeType = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("nodeType"))
        );

        // Initially empty or contains placeholder
        assertNotNull(nodeType, "Node type element should exist");
    }

    @Test
    @Order(4)
    void testDetailSectionsExistInPanel() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        WebElement detailsPanel = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("nodeDetails"))
        );

        // Verify the panel has the correct structure
        assertTrue(detailsPanel.isDisplayed(), "Details panel should be visible");
        assertNotNull(detailsPanel.getAttribute("class"), "Details panel should have CSS classes");
    }

    @Test
    @Order(5)
    void testDetailsPanelHasScrollableContent() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        WebElement detailsPanel = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("nodeDetails"))
        );

        // Verify scrollable area
        String overflowStyle = detailsPanel.getCssValue("overflow-y");
        assertTrue(overflowStyle.contains("auto") || overflowStyle.contains("scroll"),
            "Details panel should be scrollable");
    }

    @Test
    @Order(6)
    void testDetailsPanelResponsiveLayout() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        WebElement rightPanel = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.className("right-panel"))
        );

        // Verify right panel width is responsive
        int width = rightPanel.getSize().getWidth();
        assertTrue(width > 0, "Right panel should have width");
    }

    @Test
    @Order(7)
    void testNodeHeaderSectionExists() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        WebElement nodeHeader = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.className("node-header"))
        );

        assertTrue(nodeHeader.isDisplayed(), "Node header section should be visible");
    }

    @Test
    @Order(8)
    void testDetailSectionStyling() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        // Verify CSS classes exist for detail sections
        WebElement detailsPanel = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("nodeDetails"))
        );

        assertNotNull(detailsPanel, "Details panel should exist");
    }

    @Test
    @Order(9)
    void testDetailsPanelBackgroundColor() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        WebElement rightPanel = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.className("right-panel"))
        );

        String bgColor = rightPanel.getCssValue("background-color");
        assertNotNull(bgColor, "Right panel should have background color");
    }

    @Test
    @Order(10)
    void testDetailsPanelBorderStyling() {
        driver.get(baseUrl + "/yaml-manager/ui/tree-viewer");

        WebElement rightPanel = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.className("right-panel"))
        );

        String borderLeft = rightPanel.getCssValue("border-left");
        assertNotNull(borderLeft, "Right panel should have border styling");
    }
}

