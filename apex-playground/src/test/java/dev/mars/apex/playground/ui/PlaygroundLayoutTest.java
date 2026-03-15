package dev.mars.apex.playground.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for APEX Playground 2x2 grid layout.
 * Uses @TestInstance(PER_CLASS) to reuse a single browser across all test methods.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=WARN",
    "logging.level.org.springframework=WARN"
})
@DisplayName("APEX Playground Layout Tests")
class PlaygroundLayoutTest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private String baseUrl;

    @BeforeAll
    void setupBrowser() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        baseUrl = "http://localhost:" + port;
    }

    @AfterAll
    void tearDownBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @DisplayName("Should fill browser viewport height")
    void shouldFillBrowserViewportHeight() {
        driver.get(baseUrl + "/playground");

        // Get viewport height
        long viewportHeight = (Long) ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("return window.innerHeight");

        // Get container
        WebElement container = driver.findElement(By.className("playground-container"));

        // Container should exist and have reasonable height
        int containerHeight = container.getSize().getHeight();

        // Container should take up a significant portion of viewport (at least 40%)
        assertTrue(containerHeight > viewportHeight * 0.4,
            String.format("Container height (%d) should be significant portion of viewport (%d)",
                containerHeight, viewportHeight));
    }

    @Test
    @DisplayName("Should have 4 panels in a grid")
    void shouldHave4PanelsInGrid() {
        driver.get(baseUrl + "/playground");

        // Check grid exists
        WebElement grid = driver.findElement(By.className("playground-grid"));
        assertNotNull(grid, "Playground grid should exist");

        // Check 4 panels exist
        int panelCount = driver.findElements(By.className("playground-panel")).size();
        assertEquals(4, panelCount, "Should have exactly 4 panels");
    }

    @Test
    @DisplayName("Panels should expand to fill grid cells")
    void panelsShouldExpandToFillGridCells() {
        driver.get(baseUrl + "/playground");

        WebElement grid = driver.findElement(By.className("playground-grid"));
        int gridHeight = grid.getSize().getHeight();

        // Get first panel
        WebElement firstPanel = driver.findElement(By.className("playground-panel"));
        int panelHeight = firstPanel.getSize().getHeight();

        // Panel should have reasonable height (at least 100px)
        assertTrue(panelHeight > 100,
            String.format("Panel height (%d) should be at least 100px", panelHeight));

        // Panel should not exceed grid height
        assertTrue(panelHeight <= gridHeight,
            String.format("Panel height (%d) should not exceed grid height (%d)",
                panelHeight, gridHeight));
    }

    @Test
    @DisplayName("Should have correct 2x2 grid layout coordinates")
    void shouldHaveCorrectGridCoordinates() {
        driver.get(baseUrl + "/playground");

        java.util.List<WebElement> columns = driver.findElements(By.className("playground-column"));
        assertEquals(2, columns.size(), "Should have exactly 2 playground columns");

        java.util.List<WebElement> leftPanels = columns.get(0).findElements(By.className("playground-panel"));
        java.util.List<WebElement> rightPanels = columns.get(1).findElements(By.className("playground-panel"));

        assertEquals(2, leftPanels.size(), "Left column should have 2 panels");
        assertEquals(2, rightPanels.size(), "Right column should have 2 panels");

        WebElement topLeft = leftPanels.get(0);
        WebElement bottomLeft = leftPanels.get(1);
        WebElement topRight = rightPanels.get(0);

        assertTrue(topRight.getLocation().getX() > topLeft.getLocation().getX(),
            "Top-right panel should be to the right of top-left panel");
        assertTrue(Math.abs(topLeft.getLocation().getY() - topRight.getLocation().getY()) < 50,
            "Top-left and top-right panels should be on the same row");
        assertTrue(bottomLeft.getLocation().getY() > topLeft.getLocation().getY(),
            "Bottom-left panel should be below top-left panel");
    }

    @Test
    @DisplayName("Should maintain grid layout on smaller screens (tablet)")
    void shouldMaintainGridLayoutOnTablet() {
        // Resize window to tablet size (e.g. iPad portrait width is 768px)
        // We want to ensure it stays as grid above 576px
        driver.manage().window().setSize(new Dimension(800, 1024));
        driver.get(baseUrl + "/playground");

        // Check that grid and panels exist
        WebElement grid = driver.findElement(By.className("playground-grid"));
        assertNotNull(grid, "Grid should exist on tablet size");

        java.util.List<WebElement> panels = driver.findElements(By.className("playground-panel"));
        assertEquals(4, panels.size(), "Should have 4 panels on tablet size");

        // Verify panels have reasonable dimensions
        WebElement p1 = panels.get(0);
        assertTrue(p1.getSize().getWidth() > 100, "Panel should have reasonable width");
        assertTrue(p1.getSize().getHeight() > 50, "Panel should have reasonable height");
    }
}
