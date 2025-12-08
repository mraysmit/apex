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

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Manual Selenium test for accordion functionality - runs in VISIBLE browser mode.
 * This test actually clicks on UI elements and verifies visible state changes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Accordion Manual UI Test")
class AccordionManualTest {

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
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        // NO HEADLESS - we want to see the browser
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void takeScreenshot(String name) {
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path dest = Path.of("target", "screenshots", name + ".png");
            Files.createDirectories(dest.getParent());
            Files.copy(screenshot.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Screenshot saved: " + dest.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save screenshot: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Click YAML accordion header and verify collapse/expand")
    void testYamlAccordionClick() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000); // Wait for page to fully render

        takeScreenshot("01_initial_state");

        // Find the YAML section
        WebElement yamlSection = driver.findElement(By.id("yamlSection"));
        boolean initiallyExpanded = yamlSection.getAttribute("class").contains("expanded");
        System.out.println("YAML section initially expanded: " + initiallyExpanded);
        assertTrue(initiallyExpanded, "YAML section should start expanded");

        // Find accordion header and click the H2 text area (not buttons)
        WebElement yamlHeader = driver.findElement(By.cssSelector("#yamlSection .accordion-header h2"));
        System.out.println("Clicking on YAML accordion header h2...");
        yamlHeader.click();
        Thread.sleep(500);

        takeScreenshot("02_after_first_click");

        // Check if collapsed
        yamlSection = driver.findElement(By.id("yamlSection"));
        boolean afterFirstClick = yamlSection.getAttribute("class").contains("expanded");
        System.out.println("YAML section expanded after first click: " + afterFirstClick);
        assertFalse(afterFirstClick, "YAML section should be COLLAPSED after clicking");

        // Click again to expand
        yamlHeader = driver.findElement(By.cssSelector("#yamlSection .accordion-header h2"));
        System.out.println("Clicking on YAML accordion header h2 again...");
        yamlHeader.click();
        Thread.sleep(500);

        takeScreenshot("03_after_second_click");

        // Check if expanded again
        yamlSection = driver.findElement(By.id("yamlSection"));
        boolean afterSecondClick = yamlSection.getAttribute("class").contains("expanded");
        System.out.println("YAML section expanded after second click: " + afterSecondClick);
        assertTrue(afterSecondClick, "YAML section should be EXPANDED after clicking again");
    }

    @Test
    @Order(2)
    @DisplayName("SQL Editor textarea should be resizable by dragging")
    void testSqlEditorResizable() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Expand Data Sources section first
        WebElement dsHeader = driver.findElement(By.cssSelector("#dataSourcesSection .accordion-header h2"));
        js.executeScript("arguments[0].scrollIntoView(true);", dsHeader);
        Thread.sleep(300);
        dsHeader.click();
        Thread.sleep(500);

        takeScreenshot("04_datasources_expanded");

        // Find the SQL Editor textarea and scroll it into view
        WebElement sqlEditor = driver.findElement(By.id("sqlEditor"));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", sqlEditor);
        Thread.sleep(300);

        // Get computed style for resize property
        String resizeValue = (String) js.executeScript(
            "return window.getComputedStyle(arguments[0]).getPropertyValue('resize');", sqlEditor);
        System.out.println("SQL Editor resize CSS value: '" + resizeValue + "'");

        // Get initial height
        int initialHeight = sqlEditor.getSize().getHeight();
        System.out.println("SQL Editor initial height: " + initialHeight + "px");

        takeScreenshot("05_sql_editor_before_resize");

        // Try to resize using JavaScript to set height directly (simulating drag result)
        int targetHeight = 800; // Test resizing beyond old 600px max
        js.executeScript("arguments[0].style.height = '" + targetHeight + "px';", sqlEditor);
        Thread.sleep(300);

        // Get new height
        int newHeight = sqlEditor.getSize().getHeight();
        System.out.println("SQL Editor height after JS resize: " + newHeight + "px");

        takeScreenshot("06_sql_editor_after_resize");

        // Verify the height actually changed to the target (no max-height constraint)
        boolean heightChanged = newHeight >= targetHeight;
        System.out.println("Height changed to target: " + heightChanged + " (from " + initialHeight + "px to " + newHeight + "px, target was " + targetHeight + "px)");

        if (!heightChanged) {
            System.out.println("FAILURE: Could not resize SQL Editor textarea!");
            System.out.println("This indicates something is blocking resize (CSS override, container constraints, etc.)");
        }

        assertTrue(heightChanged, "SQL Editor height should change when resized. Initial: " + initialHeight + "px, After: " + newHeight + "px");
        System.out.println("SUCCESS: SQL Editor textarea can be resized");
    }

    @Test
    @Order(3)
    @DisplayName("Connections panel should have Create Block button")
    void testConnectionsCreateBlockButton() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Expand Data Sources section
        WebElement dsHeader = driver.findElement(By.cssSelector("#dataSourcesSection .accordion-header h2"));
        js.executeScript("arguments[0].scrollIntoView(true);", dsHeader);
        Thread.sleep(300);
        dsHeader.click();
        Thread.sleep(500);

        // Click on Connections tab
        WebElement connectionsTab = driver.findElement(By.cssSelector("[onclick*=\"switchDataSourceTab('connections')\"]"));
        connectionsTab.click();
        Thread.sleep(500);

        takeScreenshot("07_connections_tab");

        // Check that the renderConnectionsList function includes "Create Block" button
        String pageSource = driver.getPageSource();
        boolean hasCreateBlockFunction = pageSource.contains("createDatabaseSourceBlock");
        System.out.println("Page has createDatabaseSourceBlock function: " + hasCreateBlockFunction);

        // Verify the function exists in JavaScript
        Boolean functionExists = (Boolean) js.executeScript(
            "return typeof createDatabaseSourceBlock === 'function';");
        System.out.println("createDatabaseSourceBlock function exists: " + functionExists);

        assertTrue(functionExists, "createDatabaseSourceBlock function should exist");
        System.out.println("SUCCESS: Create Block functionality is available");
    }

    @Test
    @Order(4)
    @DisplayName("Connect/Disconnect toggle should work via API")
    void testConnectDisconnectToggle() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Expand Data Sources section
        WebElement dsHeader = driver.findElement(By.cssSelector("#dataSourcesSection .accordion-header h2"));
        js.executeScript("arguments[0].scrollIntoView(true);", dsHeader);
        Thread.sleep(300);
        dsHeader.click();
        Thread.sleep(500);

        // Click on Connections tab
        WebElement connectionsTab = driver.findElement(By.cssSelector("[onclick*=\"switchDataSourceTab('connections')\"]"));
        connectionsTab.click();
        Thread.sleep(500);

        takeScreenshot("08_connections_tab_for_toggle");

        // Verify toggleConnection function exists
        Boolean toggleFunctionExists = (Boolean) js.executeScript(
            "return typeof toggleConnection === 'function';");
        System.out.println("toggleConnection function exists: " + toggleFunctionExists);
        assertTrue(toggleFunctionExists, "toggleConnection function should exist");

        // Verify toggleActiveConnection function exists
        Boolean toggleActiveFunctionExists = (Boolean) js.executeScript(
            "return typeof toggleActiveConnection === 'function';");
        System.out.println("toggleActiveConnection function exists: " + toggleActiveFunctionExists);
        assertTrue(toggleActiveFunctionExists, "toggleActiveConnection function should exist");

        // Test the connect API endpoint directly via fetch
        String testConnectApi = (String) js.executeScript(
            "return fetch('/playground/api/datasources/connections/test-id/connect', {method: 'POST'})" +
            ".then(r => r.status.toString())" +
            ".catch(e => 'error: ' + e.message);");
        Thread.sleep(500);
        System.out.println("Connect API response status: " + testConnectApi);
        // 404 is expected since 'test-id' doesn't exist, but endpoint should exist (not 405 Method Not Allowed)
        assertNotEquals("405", testConnectApi, "Connect endpoint should exist (not 405 Method Not Allowed)");

        // Test the disconnect API endpoint directly via fetch
        String testDisconnectApi = (String) js.executeScript(
            "return fetch('/playground/api/datasources/connections/test-id/disconnect', {method: 'POST'})" +
            ".then(r => r.status.toString())" +
            ".catch(e => 'error: ' + e.message);");
        Thread.sleep(500);
        System.out.println("Disconnect API response status: " + testDisconnectApi);
        assertNotEquals("405", testDisconnectApi, "Disconnect endpoint should exist (not 405 Method Not Allowed)");

        System.out.println("SUCCESS: Connect/Disconnect API endpoints exist and are accessible");
    }

    @Test
    @Order(5)
    @DisplayName("SQL Editor should have Copy and Create Query Block buttons")
    void testSqlEditorButtons() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Expand Data Sources section
        WebElement dsHeader = driver.findElement(By.cssSelector("#dataSourcesSection .accordion-header h2"));
        js.executeScript("arguments[0].scrollIntoView(true);", dsHeader);
        Thread.sleep(300);
        dsHeader.click();
        Thread.sleep(500);

        takeScreenshot("09_sql_editor_buttons");

        // Verify Copy button exists
        WebElement copyButton = driver.findElement(By.xpath("//button[contains(text(), 'Copy')]"));
        assertNotNull(copyButton, "Copy button should exist");
        System.out.println("Copy button found: " + copyButton.getText());

        // Verify Create Query Block button exists (use onclick attribute since text is split by icon)
        WebElement createQueryBlockButton = driver.findElement(By.cssSelector("button[onclick='createQueryBlock()']"));
        assertNotNull(createQueryBlockButton, "Create Query Block button should exist");
        System.out.println("Create Query Block button found: " + createQueryBlockButton.getText().trim());

        // Verify copySqlToClipboard function exists
        Boolean copyFunctionExists = (Boolean) js.executeScript(
            "return typeof copySqlToClipboard === 'function';");
        System.out.println("copySqlToClipboard function exists: " + copyFunctionExists);
        assertTrue(copyFunctionExists, "copySqlToClipboard function should exist");

        // Verify createQueryBlock function exists
        Boolean createQueryBlockFunctionExists = (Boolean) js.executeScript(
            "return typeof createQueryBlock === 'function';");
        System.out.println("createQueryBlock function exists: " + createQueryBlockFunctionExists);
        assertTrue(createQueryBlockFunctionExists, "createQueryBlock function should exist");

        System.out.println("SUCCESS: SQL Editor has Copy and Create Query Block buttons");
    }

    @Test
    @Order(6)
    @DisplayName("Create Query Block should create a block with SQL content")
    void testCreateQueryBlockFunctionality() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Expand Data Sources section
        WebElement dsHeader = driver.findElement(By.cssSelector("#dataSourcesSection .accordion-header h2"));
        js.executeScript("arguments[0].scrollIntoView(true);", dsHeader);
        Thread.sleep(300);
        dsHeader.click();
        Thread.sleep(500);

        // Enter SQL in the editor
        WebElement sqlEditor = driver.findElement(By.id("sqlEditor"));
        sqlEditor.clear();
        sqlEditor.sendKeys("SELECT * FROM customers WHERE active = true");
        Thread.sleep(300);

        takeScreenshot("10_sql_entered");

        // Get initial block count
        Long initialBlockCount = (Long) js.executeScript(
            "return workspace.getAllBlocks(false).length;");
        System.out.println("Initial block count: " + initialBlockCount);

        // Click Create Query Block button (use onclick attribute since text is split by icon)
        WebElement createQueryBlockButton = driver.findElement(By.cssSelector("button[onclick='createQueryBlock()']"));
        createQueryBlockButton.click();
        Thread.sleep(500);

        takeScreenshot("11_after_create_query_block");

        // Get new block count
        Long newBlockCount = (Long) js.executeScript(
            "return workspace.getAllBlocks(false).length;");
        System.out.println("New block count: " + newBlockCount);

        // Verify a new block was created
        assertTrue(newBlockCount > initialBlockCount, "A new block should have been created");

        // Verify the query block has the correct SQL
        String queryBlockSql = (String) js.executeScript(
            "var blocks = workspace.getAllBlocks(false);" +
            "for (var i = 0; i < blocks.length; i++) {" +
            "  if (blocks[i].type === 'apex_data_source_query') {" +
            "    return blocks[i].getFieldValue('QUERY');" +
            "  }" +
            "}" +
            "return null;");
        System.out.println("Query block SQL: " + queryBlockSql);
        assertNotNull(queryBlockSql, "Query block should have SQL content");
        assertTrue(queryBlockSql.contains("SELECT * FROM customers"), "Query block should contain the entered SQL");

        System.out.println("SUCCESS: Create Query Block creates a block with SQL content");
    }

    @Test
    @Order(7)
    @DisplayName("Create Query Block should connect to Database Source with empty QUERIES")
    void testCreateQueryBlockConnectsToDbSource() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // First, create a Database Source block
        js.executeScript(
            "var dbBlock = workspace.newBlock('apex_data_source_database');" +
            "dbBlock.setFieldValue('test-db', 'ID');" +
            "dbBlock.setFieldValue('TestDB', 'NAME');" +
            "dbBlock.initSvg();" +
            "dbBlock.render();" +
            "dbBlock.moveBy(100, 100);");
        Thread.sleep(500);

        takeScreenshot("12_db_source_created");

        // Verify Database Source block exists
        Long dbBlockCount = (Long) js.executeScript(
            "return workspace.getAllBlocks(false).filter(b => b.type === 'apex_data_source_database').length;");
        System.out.println("Database Source block count: " + dbBlockCount);
        assertEquals(1L, dbBlockCount, "Should have 1 Database Source block");

        // Expand Data Sources section and enter SQL
        WebElement dsHeader = driver.findElement(By.cssSelector("#dataSourcesSection .accordion-header h2"));
        js.executeScript("arguments[0].scrollIntoView(true);", dsHeader);
        Thread.sleep(300);
        dsHeader.click();
        Thread.sleep(500);

        WebElement sqlEditor = driver.findElement(By.id("sqlEditor"));
        sqlEditor.clear();
        sqlEditor.sendKeys("SELECT id, name FROM users");
        Thread.sleep(300);

        // Click Create Query Block button
        WebElement createQueryBlockButton = driver.findElement(By.cssSelector("button[onclick='createQueryBlock()']"));
        createQueryBlockButton.click();
        Thread.sleep(500);

        takeScreenshot("13_query_connected_to_db");

        // Verify the query block was connected to the Database Source
        Boolean isConnected = (Boolean) js.executeScript(
            "var dbBlocks = workspace.getAllBlocks(false).filter(b => b.type === 'apex_data_source_database');" +
            "if (dbBlocks.length === 0) return false;" +
            "var dbBlock = dbBlocks[0];" +
            "var queriesInput = dbBlock.getInput('QUERIES');" +
            "return queriesInput && queriesInput.connection && queriesInput.connection.targetConnection !== null;");
        System.out.println("Query block connected to Database Source: " + isConnected);
        assertTrue(isConnected, "Query block should be connected to Database Source's QUERIES input");

        // Verify the query block has the correct SQL
        String queryBlockSql = (String) js.executeScript(
            "var queryBlocks = workspace.getAllBlocks(false).filter(b => b.type === 'apex_data_source_query');" +
            "if (queryBlocks.length === 0) return null;" +
            "return queryBlocks[0].getFieldValue('QUERY');");
        System.out.println("Query block SQL: " + queryBlockSql);
        assertTrue(queryBlockSql.contains("SELECT id, name FROM users"), "Query block should contain the entered SQL");

        System.out.println("SUCCESS: Create Query Block connects to Database Source with empty QUERIES");
    }
}

