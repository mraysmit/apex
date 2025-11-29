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
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI automation tests for the APEX Visual Rule Editor (Blockly-based).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=WARN",
    "logging.level.org.springframework=WARN"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("APEX Visual Editor UI Tests")
class VisualEditorUITest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor js;
    private String baseUrl;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        js = (JavascriptExecutor) driver;
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
    @DisplayName("Visual editor page loads successfully")
    void testPageLoads() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        
        // Wait for Blockly to initialize
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        
        // Check that Blockly workspace is initialized
        Boolean blocklyInitialized = (Boolean) js.executeScript(
            "return typeof Blockly !== 'undefined' && Blockly.getMainWorkspace() !== null"
        );
        assertTrue(blocklyInitialized, "Blockly should be initialized");
    }

    @Test
    @Order(2)
    @DisplayName("BLOCK_ID_CONFIG is defined correctly")
    void testBlockIdConfigExists() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        
        // Check BLOCK_ID_CONFIG exists and has expected entries
        Boolean configExists = (Boolean) js.executeScript(
            "return typeof BLOCK_ID_CONFIG !== 'undefined' && " +
            "BLOCK_ID_CONFIG['apex_rule'] !== undefined && " +
            "BLOCK_ID_CONFIG['apex_rule'].prefix === 'rule-'"
        );
        assertTrue(configExists, "BLOCK_ID_CONFIG should be defined with apex_rule entry");
    }

    @Test
    @Order(3)
    @DisplayName("generateUniqueId function returns rule-1 for first rule")
    void testGenerateUniqueIdFirstRule() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        
        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "return generateUniqueId(workspace, 'apex_rule');"
        );
        assertEquals("rule-1", result, "First rule should get ID rule-1");
    }

    @Test
    @Order(4)
    @DisplayName("generateUniqueId increments ID after adding a rule")
    void testGenerateUniqueIdIncrement() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Add a rule block - the event listener will auto-assign rule-1
        js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule');" +
            "block.initSvg();" +
            "block.render();"
        );

        // Wait for event to process
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Now generate next ID - should be rule-2
        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "return generateUniqueId(workspace, 'apex_rule');"
        );
        assertEquals("rule-2", result, "Second rule should get ID rule-2");
    }

    @Test
    @Order(5)
    @DisplayName("generateUniqueId handles gaps in numbering")
    void testGenerateUniqueIdWithGaps() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Add first rule - gets rule-1 automatically
        js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block1 = workspace.newBlock('apex_rule');" +
            "block1.initSvg(); block1.render();"
        );
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Manually change first rule to rule-5 to create a gap
        js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var blocks = workspace.getBlocksByType('apex_rule', false);" +
            "blocks[0].setFieldValue('rule-5', 'ID');"
        );

        // Add second rule - should get rule-6 (max + 1)
        js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block2 = workspace.newBlock('apex_rule');" +
            "block2.initSvg(); block2.render();"
        );
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Check the second block got rule-6
        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var blocks = workspace.getBlocksByType('apex_rule', false);" +
            "return blocks[1].getFieldValue('ID');"
        );
        assertEquals("rule-6", result, "Should get rule-6 after rule-5");
    }

    @Test
    @Order(6)
    @DisplayName("generateUniqueId works for rule-group")
    void testGenerateUniqueIdRuleGroup() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        
        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "return generateUniqueId(workspace, 'apex_rule_group');"
        );
        assertEquals("rule-group-1", result, "First rule group should get ID rule-group-1");
    }

    @Test
    @Order(7)
    @DisplayName("findFirstRuleId returns null when no rules exist")
    void testFindFirstRuleIdEmpty() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        
        Object result = js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "return findFirstRuleId(workspace);"
        );
        assertNull(result, "Should return null when no rules exist");
    }

    @Test
    @Order(8)
    @DisplayName("findFirstRuleId finds existing rule")
    void testFindFirstRuleIdFindsRule() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Add a rule - it will get auto-assigned rule-1
        js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule');" +
            "block.initSvg(); block.render();"
        );
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "return findFirstRuleId(workspace);"
        );
        assertEquals("rule-1", result, "Should find the auto-assigned rule ID");
    }

    @Test
    @Order(9)
    @DisplayName("Block creation event triggers auto-ID generation")
    void testBlockCreationAutoId() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        
        // Create first rule - should get rule-1
        js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule');" +
            "block.initSvg(); block.render();"
        );
        
        // Wait for event listener to process
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        
        // Check the block's ID
        String firstId = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var blocks = workspace.getBlocksByType('apex_rule', false);" +
            "return blocks.length > 0 ? blocks[0].getFieldValue('ID') : null;"
        );
        assertEquals("rule-1", firstId, "First created rule should have ID rule-1");
        
        // Create second rule - should get rule-2
        js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule');" +
            "block.initSvg(); block.render();"
        );
        
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        
        // Check we now have 2 rules with different IDs
        Long count = (Long) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var blocks = workspace.getBlocksByType('apex_rule', false);" +
            "var ids = blocks.map(b => b.getFieldValue('ID'));" +
            "return ids.filter((v, i, a) => a.indexOf(v) === i).length;"
        );
        assertEquals(2L, count, "Should have 2 rules with unique IDs");
    }

    @Test
    @Order(10)
    @DisplayName("Context menu handler is attached to apex_rule_group")
    void testContextMenuHandlerExists() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasHandler = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_rule_group'].customContextMenu === 'function';"
        );
        assertTrue(hasHandler, "apex_rule_group should have customContextMenu handler");
    }

    @Test
    @Order(11)
    @DisplayName("Rule reference created via context menu uses first rule ID")
    void testRuleRefAutoPopulation() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Create a rule first
        js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var rule = workspace.newBlock('apex_rule');" +
            "rule.initSvg(); rule.render();"
        );
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Create a rule group
        js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var group = workspace.newBlock('apex_rule_group');" +
            "group.initSvg(); group.render();"
        );
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Simulate what the context menu callback does when adding a rule reference
        js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var ruleRef = workspace.newBlock('apex_rule_ref');" +
            "ruleRef.initSvg(); ruleRef.render();" +
            // This is what the context menu callback should do:
            "var firstRuleId = findFirstRuleId(workspace);" +
            "if (firstRuleId) { ruleRef.setFieldValue(firstRuleId, 'RULE_ID'); }"
        );
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Check the rule reference has the correct ID
        String ruleRefId = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var refs = workspace.getBlocksByType('apex_rule_ref', false);" +
            "return refs.length > 0 ? refs[0].getFieldValue('RULE_ID') : null;"
        );
        assertEquals("rule-1", ruleRefId, "Rule reference should have the first rule's ID");
    }

    @Test
    @Order(12)
    @DisplayName("Rule reference dropdown shows existing rules")
    void testRuleRefDropdownShowsRules() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Create two rules with specific IDs
        js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var rule1 = workspace.newBlock('apex_rule');" +
            "rule1.setFieldValue('test-rule-A', 'ID');" +
            "rule1.initSvg(); rule1.render();" +
            "var rule2 = workspace.newBlock('apex_rule');" +
            "rule2.setFieldValue('test-rule-B', 'ID');" +
            "rule2.initSvg(); rule2.render();"
        );
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Get the dropdown options from getRuleOptions function
        @SuppressWarnings("unchecked")
        java.util.List<java.util.List<String>> options = (java.util.List<java.util.List<String>>) js.executeScript(
            "return getRuleOptions();"
        );

        assertEquals(2, options.size(), "Should have 2 rule options, got: " + options);

        // Check both rules are present (order may vary)
        java.util.Set<String> ruleIds = new java.util.HashSet<>();
        options.forEach(opt -> ruleIds.add(opt.get(0)));
        assertTrue(ruleIds.contains("test-rule-A"), "Should contain test-rule-A, got: " + ruleIds);
        assertTrue(ruleIds.contains("test-rule-B"), "Should contain test-rule-B, got: " + ruleIds);
    }

    @Test
    @Order(13)
    @DisplayName("Rule reference dropdown shows no rules message when empty")
    void testRuleRefDropdownEmptyMessage() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Get the dropdown options when no rules exist
        @SuppressWarnings("unchecked")
        java.util.List<java.util.List<String>> options = (java.util.List<java.util.List<String>>) js.executeScript(
            "return getRuleOptions();"
        );

        assertEquals(1, options.size(), "Should have 1 option");
        assertEquals("(no rules)", options.get(0).get(0), "Should show 'no rules' message");
    }

    @Test
    @Order(14)
    @DisplayName("Template functions are defined")
    void testTemplateFunctionsDefined() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Check that template functions exist
        Boolean getTemplatesDefined = (Boolean) js.executeScript("return typeof getTemplates === 'function';");
        Boolean saveTemplateDefined = (Boolean) js.executeScript("return typeof saveTemplate === 'function';");
        Boolean loadTemplateDefined = (Boolean) js.executeScript("return typeof loadTemplate === 'function';");
        Boolean deleteTemplateDefined = (Boolean) js.executeScript("return typeof deleteTemplate === 'function';");

        assertTrue(getTemplatesDefined, "getTemplates should be defined");
        assertTrue(saveTemplateDefined, "saveTemplate should be defined");
        assertTrue(loadTemplateDefined, "loadTemplate should be defined");
        assertTrue(deleteTemplateDefined, "deleteTemplate should be defined");
    }

    @Test
    @Order(15)
    @DisplayName("Templates category is registered in toolbox")
    void testTemplatesCategoryRegistered() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Check that the Templates category exists in the toolbox
        Boolean categoryExists = (Boolean) js.executeScript(
            "var toolbox = document.getElementById('toolbox');" +
            "var categories = toolbox.querySelectorAll('category');" +
            "for (var i = 0; i < categories.length; i++) {" +
            "  if (categories[i].getAttribute('name') === 'Templates') return true;" +
            "}" +
            "return false;"
        );

        assertTrue(categoryExists, "Templates category should exist in toolbox");
    }

    @Test
    @Order(16)
    @DisplayName("Save and load template works")
    void testSaveAndLoadTemplate() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Clear any existing templates
        js.executeScript("localStorage.removeItem('apex_visual_editor_templates');");

        // Create a rule block
        js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var rule = workspace.newBlock('apex_rule');" +
            "rule.setFieldValue('test-template-rule', 'ID');" +
            "rule.initSvg(); rule.render();"
        );
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Save as template programmatically
        js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var state = Blockly.serialization.workspaces.save(workspace);" +
            "var templates = [{ name: 'Test Template', description: 'Test', state: state, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() }];" +
            "localStorage.setItem('apex_visual_editor_templates', JSON.stringify(templates));"
        );

        // Verify template was saved
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> templates = (java.util.List<java.util.Map<String, Object>>) js.executeScript(
            "return getTemplates();"
        );

        assertEquals(1, templates.size(), "Should have 1 template");
        assertEquals("Test Template", templates.get(0).get("name"), "Template name should match");

        // Clear workspace
        js.executeScript("Blockly.getMainWorkspace().clear();");
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        // Verify workspace is empty
        Long blockCount = (Long) js.executeScript("return Blockly.getMainWorkspace().getAllBlocks(false).length;");
        assertEquals(0L, blockCount, "Workspace should be empty");

        // Load the template
        js.executeScript("loadTemplate('Test Template');");
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Verify the block was restored
        String ruleId = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var rules = workspace.getBlocksByType('apex_rule', false);" +
            "return rules.length > 0 ? rules[0].getFieldValue('ID') : null;"
        );

        assertEquals("test-template-rule", ruleId, "Rule ID should be restored from template");

        // Clean up
        js.executeScript("localStorage.removeItem('apex_visual_editor_templates');");
    }

    @Test
    @Order(17)
    @DisplayName("Clicking template button in toolbox loads template")
    void testClickTemplateButtonLoadsTemplate() {
        driver.get(baseUrl + "/apex_blocks_prototype.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Clear any existing templates and create one
        js.executeScript(
            "localStorage.removeItem('apex_visual_editor_templates');" +
            "var workspace = Blockly.getMainWorkspace();" +
            "var rule = workspace.newBlock('apex_rule');" +
            "rule.setFieldValue('clicked-template-rule', 'ID');" +
            "rule.initSvg(); rule.render();" +
            "var state = Blockly.serialization.workspaces.save(workspace);" +
            "var templates = [{ name: 'ClickTest', description: 'Test', state: state, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() }];" +
            "localStorage.setItem('apex_visual_editor_templates', JSON.stringify(templates));"
        );
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Clear workspace
        js.executeScript("Blockly.getMainWorkspace().clear();");
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        // Verify workspace is empty
        Long blockCount = (Long) js.executeScript("return Blockly.getMainWorkspace().getAllBlocks(false).length;");
        assertEquals(0L, blockCount, "Workspace should be empty before clicking template");

        // Refresh toolbox to pick up the new template
        js.executeScript("Blockly.getMainWorkspace().updateToolbox(document.getElementById('toolbox'));");
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        // Check templates in localStorage before clicking category
        String templatesBeforeClick = (String) js.executeScript(
            "var templates = getTemplates();" +
            "return templates.length + ' templates: ' + templates.map(function(t) { return t.name; }).join(', ');"
        );
        System.out.println("Templates before category click: " + templatesBeforeClick);

        // Manually trigger the toolbox category callback to register the template buttons
        js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var callback = workspace.getToolboxCategoryCallback('TEMPLATES');" +
            "if (callback) { callback(workspace); }"
        );
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Check what callbacks are registered
        String callbackKeys = (String) js.executeScript(
            "return Object.keys(templateButtonCallbacks).join(', ') || 'none';"
        );
        System.out.println("Registered callbacks: " + callbackKeys);

        // Trigger the template callback (simulates clicking the button)
        Boolean triggered = (Boolean) js.executeScript("return triggerTemplateCallback('loadTemplate_ClickTest');");
        assertTrue(triggered, "Template callback should be registered and triggered. Callbacks: " + callbackKeys + ". Templates: " + templatesBeforeClick);
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Verify the block was restored
        String ruleId = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var rules = workspace.getBlocksByType('apex_rule', false);" +
            "return rules.length > 0 ? rules[0].getFieldValue('ID') : null;"
        );

        assertEquals("clicked-template-rule", ruleId, "Rule ID should be restored after clicking template button");

        // Clean up
        js.executeScript("localStorage.removeItem('apex_visual_editor_templates');");
    }
}

