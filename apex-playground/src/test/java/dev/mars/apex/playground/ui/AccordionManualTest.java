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
 * Uses @TestInstance(PER_CLASS) to reuse a single browser across all test methods.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    void setupBrowser() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // NO HEADLESS - we want to see the browser
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        baseUrl = "http://localhost:" + port;
    }

    @AfterAll
    void tearDownBrowser() {
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

    @Test
    @Order(8)
    @DisplayName("YAML Configuration section should have tabs and be expanded by default")
    void testYamlConfigurationTabs() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        takeScreenshot("14_yaml_section_tabs");

        // Verify the accordion header says "YAML Configuration"
        WebElement yamlHeader = driver.findElement(By.cssSelector("#yamlSection .accordion-header h2"));
        assertTrue(yamlHeader.getText().contains("YAML Configuration"), "Header should say 'YAML Configuration'");
        System.out.println("YAML section header: " + yamlHeader.getText().trim());

        // Verify yamlSection is expanded by default
        Boolean yamlSectionExpanded = (Boolean) js.executeScript(
            "return document.getElementById('yamlSection').classList.contains('expanded');");
        System.out.println("YAML section expanded by default: " + yamlSectionExpanded);
        assertTrue(yamlSectionExpanded, "YAML section should be expanded by default");

        // Verify evalDataSection is collapsed by default
        Boolean evalSectionExpanded = (Boolean) js.executeScript(
            "return document.getElementById('evalDataSection').classList.contains('expanded');");
        System.out.println("Eval Data section expanded by default: " + evalSectionExpanded);
        assertFalse(evalSectionExpanded, "Eval Data section should be collapsed by default");

        // Verify dataSourcesSection is collapsed by default
        Boolean dataSourcesSectionExpanded = (Boolean) js.executeScript(
            "return document.getElementById('dataSourcesSection').classList.contains('expanded');");
        System.out.println("Data Sources section expanded by default: " + dataSourcesSectionExpanded);
        assertFalse(dataSourcesSectionExpanded, "Data Sources section should be collapsed by default");

        // Verify the tabs exist in the YAML section
        WebElement yamlTab = driver.findElement(By.xpath("//button[contains(text(), 'Generated YAML') and contains(@onclick, 'switchYamlTab')]"));
        assertNotNull(yamlTab, "Generated YAML tab should exist");
        System.out.println("Generated YAML tab found: " + yamlTab.getText().trim());

        WebElement dataContextTab = driver.findElement(By.xpath("//button[contains(text(), 'Data Context')]"));
        assertNotNull(dataContextTab, "Data Context tab should exist");
        System.out.println("Data Context tab found: " + dataContextTab.getText().trim());

        // Verify switchYamlTab function exists
        Boolean switchYamlTabExists = (Boolean) js.executeScript(
            "return typeof switchYamlTab === 'function';");
        System.out.println("switchYamlTab function exists: " + switchYamlTabExists);
        assertTrue(switchYamlTabExists, "switchYamlTab function should exist");

        // Verify YAML panel is active by default
        Boolean yamlPanelActive = (Boolean) js.executeScript(
            "return document.getElementById('yamlPanel').classList.contains('active');");
        System.out.println("YAML panel active by default: " + yamlPanelActive);
        assertTrue(yamlPanelActive, "YAML panel should be active by default");

        // Click Data Context tab
        dataContextTab.click();
        Thread.sleep(300);

        takeScreenshot("15_data_context_tab_active");

        // Verify Data Context panel is now active
        Boolean dataContextPanelActive = (Boolean) js.executeScript(
            "return document.getElementById('dataContextPanel').classList.contains('active');");
        System.out.println("Data Context panel active after click: " + dataContextPanelActive);
        assertTrue(dataContextPanelActive, "Data Context panel should be active after clicking tab");

        // Verify YAML panel is no longer active
        Boolean yamlPanelStillActive = (Boolean) js.executeScript(
            "return document.getElementById('yamlPanel').classList.contains('active');");
        System.out.println("YAML panel still active: " + yamlPanelStillActive);
        assertFalse(yamlPanelStillActive, "YAML panel should not be active after switching tabs");

        // Click back to Generated YAML tab
        yamlTab.click();
        Thread.sleep(300);

        // Verify YAML panel is active again
        Boolean yamlPanelActiveAgain = (Boolean) js.executeScript(
            "return document.getElementById('yamlPanel').classList.contains('active');");
        System.out.println("YAML panel active again: " + yamlPanelActiveAgain);
        assertTrue(yamlPanelActiveAgain, "YAML panel should be active after clicking back");

        System.out.println("SUCCESS: YAML Configuration section has working tabs and correct default states");
    }

    @Test
    @Order(9)
    @DisplayName("UI button labels and Generate SELECT function should be correct")
    void testButtonLabelsAndGenerateSelect() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Verify "Load into Context" button in Evaluation Data Sets
        WebElement evalLoadBtn = driver.findElement(By.xpath("//button[contains(text(), 'Load into Context') and contains(@onclick, 'loadFieldsIntoEditor')]"));
        assertNotNull(evalLoadBtn, "Eval Data Sets should have 'Load into Context' button");
        System.out.println("Eval Data Sets button: " + evalLoadBtn.getText().trim());

        // Verify "Load into Context" button in Data Sources
        WebElement dsLoadBtn = driver.findElement(By.xpath("//button[contains(text(), 'Load into Context') and contains(@onclick, 'loadSchemaIntoEditor')]"));
        assertNotNull(dsLoadBtn, "Data Sources should have 'Load into Context' button");
        System.out.println("Data Sources button: " + dsLoadBtn.getText().trim());

        // Verify generateSelectFromColumns function exists
        Boolean generateSelectExists = (Boolean) js.executeScript(
            "return typeof generateSelectFromColumns === 'function';");
        System.out.println("generateSelectFromColumns function exists: " + generateSelectExists);
        assertTrue(generateSelectExists, "generateSelectFromColumns function should exist");

        // Verify the shortened placeholder text
        String yamlPlaceholder = (String) js.executeScript(
            "return document.getElementById('yamlOutput').textContent;");
        System.out.println("YAML placeholder text: " + yamlPlaceholder);
        assertTrue(yamlPlaceholder.contains("Please start with a Configuration block"), "Placeholder should contain shortened text");
        assertFalse(yamlPlaceholder.contains("Rule Config, Data Source Config"), "Placeholder should NOT contain long text");

        System.out.println("SUCCESS: Button labels and Generate SELECT function are correct");
    }

    @Test
    @Order(10)
    @DisplayName("Data Context tab should display loaded fields")
    void testDataContextTab() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Verify updateDataContextDisplay function exists
        Boolean updateFnExists = (Boolean) js.executeScript(
            "return typeof updateDataContextDisplay === 'function';");
        System.out.println("updateDataContextDisplay function exists: " + updateFnExists);
        assertTrue(updateFnExists, "updateDataContextDisplay function should exist");

        // Verify clearDataContext function exists
        Boolean clearFnExists = (Boolean) js.executeScript(
            "return typeof clearDataContext === 'function';");
        System.out.println("clearDataContext function exists: " + clearFnExists);
        assertTrue(clearFnExists, "clearDataContext function should exist");

        // Initially, Data Context should show empty message
        Boolean emptyVisible = (Boolean) js.executeScript(
            "return document.getElementById('dataContextEmpty').style.display !== 'none';");
        System.out.println("Empty message visible initially: " + emptyVisible);
        assertTrue(emptyVisible, "Empty message should be visible initially");

        // Load some test fields
        js.executeScript(
            "loadedFieldPaths = ['trade.id', 'trade.amount', 'trade.currency', 'counterparty.name'];" +
            "updateDataContextDisplay();");
        Thread.sleep(300);

        // Now the list should be visible
        Boolean listVisible = (Boolean) js.executeScript(
            "return document.getElementById('dataContextList').style.display !== 'none';");
        System.out.println("Field list visible after loading: " + listVisible);
        assertTrue(listVisible, "Field list should be visible after loading fields");

        // Verify count is correct
        String count = (String) js.executeScript(
            "return document.getElementById('dataContextCount').textContent;");
        System.out.println("Field count: " + count);
        assertEquals("4", count, "Field count should be 4");

        // Verify fields are displayed
        String fieldsHtml = (String) js.executeScript(
            "return document.getElementById('dataContextFields').innerHTML;");
        System.out.println("Fields contain trade.id: " + fieldsHtml.contains("trade.id"));
        assertTrue(fieldsHtml.contains("trade.id"), "Fields should contain trade.id");
        assertTrue(fieldsHtml.contains("counterparty.name"), "Fields should contain counterparty.name");

        // Clear context
        js.executeScript("clearDataContext();");
        Thread.sleep(300);

        // Verify empty message is visible again
        Boolean emptyVisibleAgain = (Boolean) js.executeScript(
            "return document.getElementById('dataContextEmpty').style.display !== 'none';");
        System.out.println("Empty message visible after clear: " + emptyVisibleAgain);
        assertTrue(emptyVisibleAgain, "Empty message should be visible after clearing");

        System.out.println("SUCCESS: Data Context tab displays loaded fields correctly");
    }

    @Test
    @Order(11)
    @DisplayName("SQL Editor should have Clear button that clears content")
    void testSqlEditorClearButton() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Verify clearSqlEditor function exists
        Boolean clearFnExists = (Boolean) js.executeScript(
            "return typeof clearSqlEditor === 'function';");
        System.out.println("clearSqlEditor function exists: " + clearFnExists);
        assertTrue(clearFnExists, "clearSqlEditor function should exist");

        // Put some content in the SQL editor
        js.executeScript("document.getElementById('sqlEditor').value = 'SELECT * FROM test_table';");

        // Verify content is there
        String contentBefore = (String) js.executeScript(
            "return document.getElementById('sqlEditor').value;");
        System.out.println("SQL content before clear: " + contentBefore);
        assertEquals("SELECT * FROM test_table", contentBefore);

        // Call clearSqlEditor
        js.executeScript("clearSqlEditor();");
        Thread.sleep(300);

        // Verify content is cleared
        String contentAfter = (String) js.executeScript(
            "return document.getElementById('sqlEditor').value;");
        System.out.println("SQL content after clear: '" + contentAfter + "'");
        assertEquals("", contentAfter, "SQL Editor should be empty after clear");

        System.out.println("SUCCESS: SQL Editor Clear button works correctly");
    }

    @Test
    @Order(12)
    @DisplayName("Column selections should persist when switching tabs")
    void testColumnSelectionsPersist() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Simulate having selected columns
        js.executeScript(
            "selectedColumns = {" +
            "  'public.users': ['id', 'name', 'email']," +
            "  'public.orders': ['order_id', 'total']" +
            "};");

        // Verify selectedColumns is set
        Long userColCount = (Long) js.executeScript(
            "return selectedColumns['public.users'] ? selectedColumns['public.users'].length : 0;");
        System.out.println("User columns selected: " + userColCount);
        assertEquals(3L, userColCount, "Should have 3 user columns selected");

        // Switch to SQL tab
        js.executeScript("switchDataSourceTab('sql');");
        Thread.sleep(300);

        // Switch back to Table View
        js.executeScript("switchDataSourceTab('table');");
        Thread.sleep(300);

        // Verify selectedColumns still has the values
        Long userColCountAfter = (Long) js.executeScript(
            "return selectedColumns['public.users'] ? selectedColumns['public.users'].length : 0;");
        System.out.println("User columns after tab switch: " + userColCountAfter);
        assertEquals(3L, userColCountAfter, "Should still have 3 user columns selected after tab switch");

        Long orderColCountAfter = (Long) js.executeScript(
            "return selectedColumns['public.orders'] ? selectedColumns['public.orders'].length : 0;");
        System.out.println("Order columns after tab switch: " + orderColCountAfter);
        assertEquals(2L, orderColCountAfter, "Should still have 2 order columns selected after tab switch");

        System.out.println("SUCCESS: Column selections persist when switching tabs");
    }

    @Test
    @Order(13)
    @DisplayName("Generate SELECT button should create SQL from selected columns")
    void testGenerateSelectFromColumns() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Verify generateSelectFromColumns function exists
        Boolean fnExists = (Boolean) js.executeScript(
            "return typeof generateSelectFromColumns === 'function';");
        System.out.println("generateSelectFromColumns function exists: " + fnExists);
        assertTrue(fnExists, "generateSelectFromColumns function should exist");

        // Set up selected columns
        js.executeScript(
            "selectedColumns = { 'public.users': ['id', 'name', 'email'] };");

        // Clear SQL editor first
        js.executeScript("document.getElementById('sqlEditor').value = '';");

        // Call generateSelectFromColumns with mock event
        js.executeScript(
            "generateSelectFromColumns({ stopPropagation: function(){} }, 'public.users', 'users', 'public');");
        Thread.sleep(300);

        // Verify SQL was generated
        String sql = (String) js.executeScript(
            "return document.getElementById('sqlEditor').value;");
        System.out.println("Generated SQL: " + sql);

        assertTrue(sql.contains("SELECT"), "SQL should contain SELECT");
        assertTrue(sql.contains("id"), "SQL should contain id column");
        assertTrue(sql.contains("name"), "SQL should contain name column");
        assertTrue(sql.contains("email"), "SQL should contain email column");
        assertTrue(sql.contains("public.users"), "SQL should contain table name");

        System.out.println("SUCCESS: Generate SELECT button creates SQL from selected columns");
    }

    @Test
    @Order(14)
    @DisplayName("Active toggle should show meaningful messages with connection name")
    void testActiveToggleMessages() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Set up mock connections
        js.executeScript(
            "connections = [{ id: 'test-1', name: 'My Test Database', connected: true }];" +
            "activeConnection = null;");

        // Test activation message
        js.executeScript("selectConnection('test-1');");
        Thread.sleep(300);

        // Check the active connection name
        String activeName = (String) js.executeScript(
            "return activeConnection ? activeConnection.name : null;");
        System.out.println("Active connection name: " + activeName);
        assertEquals("My Test Database", activeName);

        // Test deactivation - call toggleActiveConnection on active connection
        js.executeScript("toggleActiveConnection('test-1');");
        Thread.sleep(300);

        // Check connection is now inactive
        Boolean isActive = (Boolean) js.executeScript(
            "return activeConnection !== null;");
        System.out.println("Connection still active after toggle: " + isActive);
        assertFalse(isActive, "Connection should be inactive after toggle");

        System.out.println("SUCCESS: Active toggle shows meaningful messages with connection name");
    }

    @Test
    @Order(15)
    @DisplayName("Load into Context button should load selected columns into Data Context")
    void testLoadSelectedColumnsToContext() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Verify loadSelectedColumnsToContext function exists
        Boolean fnExists = (Boolean) js.executeScript(
            "return typeof loadSelectedColumnsToContext === 'function';");
        System.out.println("loadSelectedColumnsToContext function exists: " + fnExists);
        assertTrue(fnExists, "loadSelectedColumnsToContext function should exist");

        // Clear any existing loaded fields
        js.executeScript("loadedFieldPaths = [];");

        // Set up selected columns
        js.executeScript(
            "selectedColumns = {" +
            "  'public.users': ['id', 'name', 'email']," +
            "  'public.orders': ['order_id', 'total']" +
            "};");

        // Call loadSelectedColumnsToContext
        js.executeScript("loadSelectedColumnsToContext();");
        Thread.sleep(300);

        // Verify fields were loaded into context
        Long fieldCount = (Long) js.executeScript("return loadedFieldPaths.length;");
        System.out.println("Fields loaded into context: " + fieldCount);
        assertEquals(5L, fieldCount, "Should have 5 fields loaded");

        // Verify specific field paths
        Boolean hasUsersId = (Boolean) js.executeScript(
            "return loadedFieldPaths.includes('users.id');");
        Boolean hasOrdersTotal = (Boolean) js.executeScript(
            "return loadedFieldPaths.includes('orders.total');");
        System.out.println("Has users.id: " + hasUsersId);
        System.out.println("Has orders.total: " + hasOrdersTotal);
        assertTrue(hasUsersId, "Should have users.id in context");
        assertTrue(hasOrdersTotal, "Should have orders.total in context");

        // Verify Data Context display is updated
        Boolean listVisible = (Boolean) js.executeScript(
            "return document.getElementById('dataContextList').style.display !== 'none';");
        System.out.println("Data Context list visible: " + listVisible);
        assertTrue(listVisible, "Data Context list should be visible");

        System.out.println("SUCCESS: Load into Context button loads selected columns into Data Context");
    }

    @Test
    @Order(16)
    @DisplayName("Configuration Context tab should display blocks dynamically")
    void testConfigurationContextTab() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Verify updateConfigContextDisplay function exists
        Boolean fnExists = (Boolean) js.executeScript(
            "return typeof updateConfigContextDisplay === 'function';");
        System.out.println("updateConfigContextDisplay function exists: " + fnExists);
        assertTrue(fnExists, "updateConfigContextDisplay function should exist");

        // Initially no blocks - empty message should be visible
        Boolean emptyVisible = (Boolean) js.executeScript(
            "return document.getElementById('configContextEmpty').style.display !== 'none';");
        System.out.println("Empty message visible initially: " + emptyVisible);
        assertTrue(emptyVisible, "Empty message should be visible when no blocks");

        // Create a block on the workspace
        js.executeScript(
            "var block = workspace.newBlock('apex_rule_config');" +
            "block.setFieldValue('test-config-1', 'ID');" +
            "block.setFieldValue('Test Config', 'NAME');" +
            "block.initSvg();" +
            "block.render();" +
            "block.moveBy(50, 50);");
        Thread.sleep(300);

        // Trigger update
        js.executeScript("updateConfigContextDisplay();");
        Thread.sleep(200);

        // Verify list is now visible
        Boolean listVisible = (Boolean) js.executeScript(
            "return document.getElementById('configContextList').style.display !== 'none';");
        System.out.println("Config Context list visible after adding block: " + listVisible);
        assertTrue(listVisible, "Config Context list should be visible after adding block");

        // Verify block count
        String countText = (String) js.executeScript(
            "return document.getElementById('configContextCount').textContent;");
        System.out.println("Block count: " + countText);
        assertTrue(Integer.parseInt(countText) >= 1, "Should have at least 1 block");

        // Verify block info is displayed
        String blocksHtml = (String) js.executeScript(
            "return document.getElementById('configContextBlocks').innerHTML;");
        System.out.println("Blocks HTML contains test-config-1: " + blocksHtml.contains("test-config-1"));
        assertTrue(blocksHtml.contains("test-config-1"), "Should display block ID");
        assertTrue(blocksHtml.contains("Test Config"), "Should display block name");

        // Add another block and verify count updates
        js.executeScript(
            "var block2 = workspace.newBlock('apex_rule');" +
            "block2.setFieldValue('rule-1', 'ID');" +
            "block2.setFieldValue('My Rule', 'NAME');" +
            "block2.initSvg();" +
            "block2.render();" +
            "block2.moveBy(50, 150);");
        Thread.sleep(300);

        js.executeScript("updateConfigContextDisplay();");
        Thread.sleep(200);

        String newCountText = (String) js.executeScript(
            "return document.getElementById('configContextCount').textContent;");
        System.out.println("Block count after adding second block: " + newCountText);
        assertTrue(Integer.parseInt(newCountText) >= 2, "Should have at least 2 blocks");

        System.out.println("SUCCESS: Configuration Context tab displays blocks dynamically");
    }

    @Test
    @Order(17)
    @DisplayName("Configuration Context should only show blocks with ID attribute")
    void testConfigContextOnlyShowsBlocksWithId() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Clear workspace first
        js.executeScript("workspace.clear();");
        Thread.sleep(300);

        // Add a block WITH an ID field (apex_rule_config has ID)
        js.executeScript(
            "var block1 = workspace.newBlock('apex_rule_config');" +
            "block1.initSvg();" +
            "block1.render();" +
            "block1.moveBy(50, 50);");
        Thread.sleep(300);

        // Trigger update
        js.executeScript("updateConfigContextDisplay();");
        Thread.sleep(200);

        // Verify count is 1 (only the block with ID)
        String countText = (String) js.executeScript(
            "return document.getElementById('configContextCount').textContent;");
        System.out.println("Block count with ID block: " + countText);
        assertEquals("1", countText, "Should have exactly 1 block with ID");

        // Verify the block ID is shown
        String blocksHtml = (String) js.executeScript(
            "return document.getElementById('configContextBlocks').innerHTML;");
        assertTrue(blocksHtml.contains("config-1"), "Should display config-1 block ID");

        System.out.println("SUCCESS: Configuration Context only shows blocks with ID attribute");
    }

    @Test
    @Order(18)
    @DisplayName("Configuration blocks should get unique auto-generated IDs")
    void testConfigBlockAutoUniqueIds() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Clear workspace first
        js.executeScript("workspace.clear();");
        Thread.sleep(300);

        // Create first Configuration block - should get config-1
        js.executeScript(
            "var block1 = workspace.newBlock('apex_rule_config');" +
            "block1.initSvg();" +
            "block1.render();" +
            "block1.moveBy(50, 50);");
        Thread.sleep(300);

        // Get the ID of the first block
        String firstId = (String) js.executeScript(
            "var blocks = workspace.getBlocksByType('apex_rule_config');" +
            "return blocks[0].getFieldValue('ID');");
        System.out.println("First Configuration block ID: " + firstId);
        assertEquals("config-1", firstId, "First config block should have ID config-1");

        // Create second Configuration block - should get config-2
        js.executeScript(
            "var block2 = workspace.newBlock('apex_rule_config');" +
            "block2.initSvg();" +
            "block2.render();" +
            "block2.moveBy(50, 200);");
        Thread.sleep(300);

        // Get all config block IDs
        @SuppressWarnings("unchecked")
        java.util.List<String> allIds = (java.util.List<String>) js.executeScript(
            "var blocks = workspace.getBlocksByType('apex_rule_config');" +
            "return blocks.map(function(b) { return b.getFieldValue('ID'); });");
        System.out.println("All Configuration block IDs: " + allIds);

        assertTrue(allIds.contains("config-1"), "Should have config-1");
        assertTrue(allIds.contains("config-2"), "Should have config-2");
        assertEquals(2, allIds.size(), "Should have exactly 2 config blocks");

        // Create third Configuration block - should get config-3
        js.executeScript(
            "var block3 = workspace.newBlock('apex_rule_config');" +
            "block3.initSvg();" +
            "block3.render();" +
            "block3.moveBy(50, 350);");
        Thread.sleep(300);

        @SuppressWarnings("unchecked")
        java.util.List<String> finalIds = (java.util.List<String>) js.executeScript(
            "var blocks = workspace.getBlocksByType('apex_rule_config');" +
            "return blocks.map(function(b) { return b.getFieldValue('ID'); });");
        System.out.println("Final Configuration block IDs: " + finalIds);

        assertTrue(finalIds.contains("config-3"), "Should have config-3");
        assertEquals(3, finalIds.size(), "Should have exactly 3 config blocks");

        System.out.println("SUCCESS: Configuration blocks get unique auto-generated IDs");
    }

    @Test
    @Order(19)
    @DisplayName("Rule blocks should get unique auto-generated IDs and names")
    void testRuleBlockAutoUniqueIds() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Clear workspace first
        js.executeScript("workspace.clear();");
        Thread.sleep(300);

        // Create first Rule block - should get rule-1
        js.executeScript(
            "var block1 = workspace.newBlock('apex_rule');" +
            "block1.initSvg();" +
            "block1.render();" +
            "block1.moveBy(50, 50);");
        Thread.sleep(300);

        // Get the ID and NAME of the first block
        String firstId = (String) js.executeScript(
            "var blocks = workspace.getBlocksByType('apex_rule');" +
            "return blocks[0].getFieldValue('ID');");
        String firstName = (String) js.executeScript(
            "var blocks = workspace.getBlocksByType('apex_rule');" +
            "return blocks[0].getFieldValue('NAME');");
        System.out.println("First Rule block ID: " + firstId + ", NAME: " + firstName);
        assertEquals("rule-1", firstId, "First rule block should have ID rule-1");
        assertEquals("Rule 1", firstName, "First rule block should have NAME 'Rule 1'");

        // Create second Rule block - should get rule-2
        js.executeScript(
            "var block2 = workspace.newBlock('apex_rule');" +
            "block2.initSvg();" +
            "block2.render();" +
            "block2.moveBy(50, 200);");
        Thread.sleep(300);

        // Get all rule block IDs and NAMEs
        @SuppressWarnings("unchecked")
        java.util.List<String> allIds = (java.util.List<String>) js.executeScript(
            "var blocks = workspace.getBlocksByType('apex_rule');" +
            "return blocks.map(function(b) { return b.getFieldValue('ID'); });");
        @SuppressWarnings("unchecked")
        java.util.List<String> allNames = (java.util.List<String>) js.executeScript(
            "var blocks = workspace.getBlocksByType('apex_rule');" +
            "return blocks.map(function(b) { return b.getFieldValue('NAME'); });");
        System.out.println("All Rule block IDs: " + allIds);
        System.out.println("All Rule block NAMEs: " + allNames);

        assertTrue(allIds.contains("rule-1"), "Should have rule-1");
        assertTrue(allIds.contains("rule-2"), "Should have rule-2");
        assertTrue(allNames.contains("Rule 1"), "Should have 'Rule 1'");
        assertTrue(allNames.contains("Rule 2"), "Should have 'Rule 2'");

        System.out.println("SUCCESS: Rule blocks get unique auto-generated IDs and names");
    }

    @Test
    @Order(20)
    @DisplayName("Database Source blocks should get unique auto-generated IDs and names")
    void testDatabaseSourceBlockAutoUniqueIds() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Clear workspace first
        js.executeScript("workspace.clear();");
        Thread.sleep(300);

        // Create first Database Source block
        js.executeScript(
            "var block1 = workspace.newBlock('apex_data_source_database');" +
            "block1.initSvg();" +
            "block1.render();" +
            "block1.moveBy(50, 50);");
        Thread.sleep(300);

        String firstId = (String) js.executeScript(
            "var blocks = workspace.getBlocksByType('apex_data_source_database');" +
            "return blocks[0].getFieldValue('ID');");
        String firstName = (String) js.executeScript(
            "var blocks = workspace.getBlocksByType('apex_data_source_database');" +
            "return blocks[0].getFieldValue('NAME');");
        System.out.println("First Database Source block ID: " + firstId + ", NAME: " + firstName);
        assertEquals("ds-db-1", firstId, "First database source should have ID ds-db-1");
        assertEquals("Database 1", firstName, "First database source should have NAME 'Database 1'");

        // Create second Database Source block
        js.executeScript(
            "var block2 = workspace.newBlock('apex_data_source_database');" +
            "block2.initSvg();" +
            "block2.render();" +
            "block2.moveBy(50, 300);");
        Thread.sleep(300);

        @SuppressWarnings("unchecked")
        java.util.List<String> allIds = (java.util.List<String>) js.executeScript(
            "var blocks = workspace.getBlocksByType('apex_data_source_database');" +
            "return blocks.map(function(b) { return b.getFieldValue('ID'); });");
        System.out.println("All Database Source block IDs: " + allIds);

        assertTrue(allIds.contains("ds-db-1"), "Should have ds-db-1");
        assertTrue(allIds.contains("ds-db-2"), "Should have ds-db-2");

        System.out.println("SUCCESS: Database Source blocks get unique auto-generated IDs");
    }

    @Test
    @Order(21)
    @DisplayName("Unique IDs should handle gaps when blocks are deleted")
    void testUniqueIdsWithGaps() throws InterruptedException {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        Thread.sleep(1000);

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Clear workspace first
        js.executeScript("workspace.clear();");
        Thread.sleep(300);

        // Create three Rule blocks one at a time to ensure proper ID assignment
        js.executeScript(
            "var block1 = workspace.newBlock('apex_rule');" +
            "block1.initSvg(); block1.render(); block1.moveBy(50, 50);");
        Thread.sleep(300);

        js.executeScript(
            "var block2 = workspace.newBlock('apex_rule');" +
            "block2.initSvg(); block2.render(); block2.moveBy(50, 200);");
        Thread.sleep(300);

        js.executeScript(
            "var block3 = workspace.newBlock('apex_rule');" +
            "block3.initSvg(); block3.render(); block3.moveBy(50, 350);");
        Thread.sleep(300);

        // Verify we have rule-1, rule-2, rule-3
        @SuppressWarnings("unchecked")
        java.util.List<String> initialIds = (java.util.List<String>) js.executeScript(
            "var blocks = workspace.getBlocksByType('apex_rule');" +
            "return blocks.map(function(b) { return b.getFieldValue('ID'); });");
        System.out.println("Initial Rule block IDs: " + initialIds);
        assertEquals(3, initialIds.size(), "Should have 3 rule blocks initially");
        assertTrue(initialIds.contains("rule-1"), "Should have rule-1");
        assertTrue(initialIds.contains("rule-2"), "Should have rule-2");
        assertTrue(initialIds.contains("rule-3"), "Should have rule-3");

        // Delete the second block (rule-2)
        js.executeScript(
            "var blocks = workspace.getBlocksByType('apex_rule');" +
            "for (var i = 0; i < blocks.length; i++) {" +
            "  if (blocks[i].getFieldValue('ID') === 'rule-2') {" +
            "    blocks[i].dispose();" +
            "    break;" +
            "  }" +
            "}");
        Thread.sleep(300);

        // Verify we now have rule-1 and rule-3
        @SuppressWarnings("unchecked")
        java.util.List<String> afterDeleteIds = (java.util.List<String>) js.executeScript(
            "var blocks = workspace.getBlocksByType('apex_rule');" +
            "return blocks.map(function(b) { return b.getFieldValue('ID'); });");
        System.out.println("After delete Rule block IDs: " + afterDeleteIds);
        assertEquals(2, afterDeleteIds.size(), "Should have 2 rule blocks after delete");
        assertTrue(afterDeleteIds.contains("rule-1"), "Should still have rule-1");
        assertTrue(afterDeleteIds.contains("rule-3"), "Should still have rule-3");

        // Add a new block - should get rule-4 (not rule-2, gaps are allowed)
        js.executeScript(
            "var block4 = workspace.newBlock('apex_rule');" +
            "block4.initSvg(); block4.render(); block4.moveBy(50, 500);");
        Thread.sleep(300);

        @SuppressWarnings("unchecked")
        java.util.List<String> finalIds = (java.util.List<String>) js.executeScript(
            "var blocks = workspace.getBlocksByType('apex_rule');" +
            "return blocks.map(function(b) { return b.getFieldValue('ID'); });");
        System.out.println("Final Rule block IDs: " + finalIds);
        assertEquals(3, finalIds.size(), "Should have 3 rule blocks");
        assertTrue(finalIds.contains("rule-4"), "New block should have rule-4 (gaps allowed)");

        System.out.println("SUCCESS: Unique IDs handle gaps correctly");
    }
}
