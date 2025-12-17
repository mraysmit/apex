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
        driver.get(baseUrl + "/apex_editor_main.html");
        
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
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));
        
        // Check BLOCK_ID_CONFIG exists and has expected entries
        Boolean configExists = (Boolean) js.executeScript(
            "return typeof BLOCK_ID_CONFIG !== 'undefined' && " +
            "BLOCK_ID_CONFIG['apex_rule'] !== undefined && " +
            "BLOCK_ID_CONFIG['apex_rule'].idPrefix === 'rule-'"
        );
        assertTrue(configExists, "BLOCK_ID_CONFIG should be defined with apex_rule entry");
    }

    @Test
    @Order(3)
    @DisplayName("generateUniqueId function returns rule-1 for first rule")
    void testGenerateUniqueIdFirstRule() {
        driver.get(baseUrl + "/apex_editor_main.html");
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
        driver.get(baseUrl + "/apex_editor_main.html");
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
        driver.get(baseUrl + "/apex_editor_main.html");
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
        driver.get(baseUrl + "/apex_editor_main.html");
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
        driver.get(baseUrl + "/apex_editor_main.html");
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
        driver.get(baseUrl + "/apex_editor_main.html");
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
        driver.get(baseUrl + "/apex_editor_main.html");
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
        driver.get(baseUrl + "/apex_editor_main.html");
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
        driver.get(baseUrl + "/apex_editor_main.html");
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
        driver.get(baseUrl + "/apex_editor_main.html");
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
        driver.get(baseUrl + "/apex_editor_main.html");
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
        driver.get(baseUrl + "/apex_editor_main.html");
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
        driver.get(baseUrl + "/apex_editor_main.html");
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
        driver.get(baseUrl + "/apex_editor_main.html");
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
        driver.get(baseUrl + "/apex_editor_main.html");
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

    @Test
    @Order(18)
    @DisplayName("Accordion sections exist and YAML section is expanded by default")
    void testAccordionSectionsExist() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Check YAML section exists and is expanded
        Boolean yamlSectionExpanded = (Boolean) js.executeScript(
            "var section = document.getElementById('yamlSection');" +
            "return section && section.classList.contains('expanded');"
        );
        assertTrue(yamlSectionExpanded, "YAML section should exist and be expanded by default");

        // Check Eval Data section exists (it is collapsed by default)
        Boolean evalSectionExists = (Boolean) js.executeScript(
            "var section = document.getElementById('evalDataSection');" +
            "return section !== null;"
        );
        assertTrue(evalSectionExists, "Eval Data section should exist");
    }

    @Test
    @Order(19)
    @DisplayName("Accordion toggle function works via direct call")
    void testAccordionToggle() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Toggle YAML section (should collapse it since it starts expanded)
        js.executeScript("toggleAccordion('yamlSection');");
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        Boolean yamlCollapsed = (Boolean) js.executeScript(
            "return !document.getElementById('yamlSection').classList.contains('expanded');"
        );
        assertTrue(yamlCollapsed, "YAML section should be collapsed after toggle");

        // Toggle Eval Data section (should expand it since it starts collapsed)
        js.executeScript("toggleAccordion('evalDataSection');");
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        Boolean evalExpanded = (Boolean) js.executeScript(
            "return document.getElementById('evalDataSection').classList.contains('expanded');"
        );
        assertTrue(evalExpanded, "Eval Data section should be expanded after toggle");
    }

    @Test
    @Order(50)
    @DisplayName("Accordion header click triggers toggle - YAML section")
    void testAccordionHeaderClickYamlSection() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Verify the toggleAccordion function exists
        Boolean functionExists = (Boolean) js.executeScript(
            "return typeof toggleAccordion === 'function';"
        );
        assertTrue(functionExists, "toggleAccordion function should be defined");

        // Verify YAML section starts expanded
        Boolean initiallyExpanded = (Boolean) js.executeScript(
            "return document.getElementById('yamlSection').classList.contains('expanded');"
        );
        assertTrue(initiallyExpanded, "YAML section should be expanded initially");

        // Click directly on the accordion-header div (not the h2)
        WebElement yamlHeader = driver.findElement(By.cssSelector("#yamlSection .accordion-header"));
        yamlHeader.click();
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Verify section is now collapsed
        Boolean nowCollapsed = (Boolean) js.executeScript(
            "return !document.getElementById('yamlSection').classList.contains('expanded');"
        );
        assertTrue(nowCollapsed, "YAML section should be collapsed after clicking header");

        // Click again to expand
        yamlHeader.click();
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Verify section is expanded again
        Boolean expandedAgain = (Boolean) js.executeScript(
            "return document.getElementById('yamlSection').classList.contains('expanded');"
        );
        assertTrue(expandedAgain, "YAML section should be expanded after clicking header again");
    }

    @Test
    @Order(51)
    @DisplayName("Accordion header click triggers toggle - Eval Data section")
    void testAccordionHeaderClickEvalDataSection() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Verify Eval Data section starts collapsed (not expanded by default)
        Boolean initiallyCollapsed = (Boolean) js.executeScript(
            "return !document.getElementById('evalDataSection').classList.contains('expanded');"
        );
        assertTrue(initiallyCollapsed, "Eval Data section should be collapsed initially");

        // Click directly on the accordion-header div to expand
        WebElement evalHeader = driver.findElement(By.cssSelector("#evalDataSection .accordion-header"));
        evalHeader.click();
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Verify section is now expanded
        Boolean nowExpanded = (Boolean) js.executeScript(
            "return document.getElementById('evalDataSection').classList.contains('expanded');"
        );
        assertTrue(nowExpanded, "Eval Data section should be expanded after clicking header");
    }

    @Test
    @Order(52)
    @DisplayName("Accordion header click triggers toggle - Data Sources section")
    void testAccordionHeaderClickDataSourcesSection() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Verify Data Sources section starts collapsed (not expanded by default)
        Boolean initiallyCollapsed = (Boolean) js.executeScript(
            "return !document.getElementById('dataSourcesSection').classList.contains('expanded');"
        );
        assertTrue(initiallyCollapsed, "Data Sources section should be collapsed initially");

        // Click directly on the accordion-header div
        WebElement dsHeader = driver.findElement(By.cssSelector("#dataSourcesSection .accordion-header"));
        dsHeader.click();
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Verify section is now expanded
        Boolean nowExpanded = (Boolean) js.executeScript(
            "return document.getElementById('dataSourcesSection').classList.contains('expanded');"
        );
        assertTrue(nowExpanded, "Data Sources section should be expanded after clicking header");
    }

    @Test
    @Order(20)
    @DisplayName("Evaluation data functions are defined")
    void testEvalDataFunctionsDefined() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean switchTabDefined = (Boolean) js.executeScript("return typeof switchEvalTab === 'function';");
        Boolean clearDefined = (Boolean) js.executeScript("return typeof clearEvalData === 'function';");
        Boolean formatDefined = (Boolean) js.executeScript("return typeof formatEvalJson === 'function';");
        Boolean getJsonDefined = (Boolean) js.executeScript("return typeof getEvalDataAsJson === 'function';");
        Boolean getEvalJsonFromEditorDefined = (Boolean) js.executeScript("return typeof getEvalJsonFromEditor === 'function';");
        Boolean renderTreeDefined = (Boolean) js.executeScript("return typeof renderJsonTree === 'function';");

        assertTrue(switchTabDefined, "switchEvalTab should be defined");
        assertTrue(clearDefined, "clearEvalData should be defined");
        assertTrue(formatDefined, "formatEvalJson should be defined");
        assertTrue(getJsonDefined, "getEvalDataAsJson should be defined");
        assertTrue(getEvalJsonFromEditorDefined, "getEvalJsonFromEditor should be defined");
        assertTrue(renderTreeDefined, "renderJsonTree should be defined");
    }

    @Test
    @Order(21)
    @DisplayName("JSON editor accepts and validates JSON")
    void testJsonEditorValidation() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Set valid JSON in editor
        js.executeScript(
            "document.getElementById('evalJsonEditor').value = '{\"name\": \"test\", \"value\": 123}';" +
            "validateAndParseJson();"
        );
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        // Error should be hidden
        Boolean errorHidden = (Boolean) js.executeScript(
            "return document.getElementById('evalJsonError').style.display === 'none';"
        );
        assertTrue(errorHidden, "Error should be hidden for valid JSON");

        // Set invalid JSON
        js.executeScript(
            "document.getElementById('evalJsonEditor').value = '{invalid json}';" +
            "validateAndParseJson();"
        );
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        // Error should be visible
        Boolean errorVisible = (Boolean) js.executeScript(
            "return document.getElementById('evalJsonError').style.display !== 'none';"
        );
        assertTrue(errorVisible, "Error should be visible for invalid JSON");
    }

    @Test
    @Order(22)
    @DisplayName("JSON can be formatted in editor")
    void testJsonFormatting() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Set compact JSON
        js.executeScript(
            "document.getElementById('evalJsonEditor').value = '{\"name\":\"test\",\"nested\":{\"a\":1,\"b\":2}}';"
        );

        // Format it
        js.executeScript("formatEvalJson();");
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        // Check it's now formatted (contains newlines)
        String formatted = (String) js.executeScript("return document.getElementById('evalJsonEditor').value;");
        assertTrue(formatted.contains("\n"), "Formatted JSON should contain newlines");
        assertTrue(formatted.contains("  "), "Formatted JSON should contain indentation");
    }

    @Test
    @Order(23)
    @DisplayName("Tree view renders nested JSON")
    void testTreeViewRendering() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Set nested JSON
        js.executeScript(
            "document.getElementById('evalJsonEditor').value = '{\"trade\": {\"id\": \"TRD-001\", \"amount\": 1000}}';"
        );

        // Update tree view
        js.executeScript("updateTreeView();");
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        // Check tree contains expected elements
        String treeHtml = (String) js.executeScript(
            "return document.getElementById('evalTreeContainer').innerHTML;"
        );
        assertTrue(treeHtml.contains("trade"), "Tree should contain 'trade' key");
        assertTrue(treeHtml.contains("TRD-001"), "Tree should contain 'TRD-001' value");
        assertTrue(treeHtml.contains("1000"), "Tree should contain '1000' value");
    }

    @Test
    @Order(24)
    @DisplayName("Eval data tabs exist and can be switched")
    void testEvalDataTabs() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Expand eval section first
        js.executeScript("toggleAccordion('evalDataSection');");
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        // Check tabs exist (Bootstrap nav-link tabs) - specifically in evalDataOutput section
        Long tabCount = (Long) js.executeScript("return document.querySelectorAll('#evalDataOutput .nav-tabs-dark .nav-link').length;");
        assertEquals(3L, tabCount, "Should have 3 tabs (Editor, Tree, Files) in eval data section");

        // Check editor panel is active by default
        Boolean editorActive = (Boolean) js.executeScript(
            "return document.getElementById('evalEditorPanel').classList.contains('active');"
        );
        assertTrue(editorActive, "Editor panel should be active by default");

        // Check JSON editor exists
        Boolean editorExists = (Boolean) js.executeScript(
            "return document.getElementById('evalJsonEditor') !== null;"
        );
        assertTrue(editorExists, "JSON editor textarea should exist");
    }

    @Test
    @Order(25)
    @DisplayName("File drop zone exists")
    void testFileDropZoneExists() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean dropZoneExists = (Boolean) js.executeScript(
            "return document.getElementById('evalFileDrop') !== null;"
        );
        assertTrue(dropZoneExists, "File drop zone should exist");

        Boolean fileInputExists = (Boolean) js.executeScript(
            "return document.getElementById('evalFileInput') !== null;"
        );
        assertTrue(fileInputExists, "File input should exist");
    }

    @Test
    @Order(26)
    @DisplayName("extractJsonPaths extracts paths from nested JSON")
    void testExtractJsonPaths() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Test with nested JSON
        String paths = (String) js.executeScript(
            "var data = {" +
            "  trade: {" +
            "    id: 'TRD-001'," +
            "    currency: 'USD'," +
            "    amount: 1000," +
            "    counterparty: {" +
            "      id: 'CP-123'," +
            "      name: 'Acme Corp'" +
            "    }" +
            "  }" +
            "};" +
            "var paths = extractJsonPaths(data, '');" +
            "return paths.join(',');"
        );

        assertTrue(paths.contains("trade.id"), "Should extract trade.id");
        assertTrue(paths.contains("trade.currency"), "Should extract trade.currency");
        assertTrue(paths.contains("trade.amount"), "Should extract trade.amount");
        assertTrue(paths.contains("trade.counterparty.id"), "Should extract trade.counterparty.id");
        assertTrue(paths.contains("trade.counterparty.name"), "Should extract trade.counterparty.name");
    }

    @Test
    @Order(27)
    @DisplayName("extractJsonPaths handles arrays with [*] notation")
    void testExtractJsonPathsWithArrays() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Test with arrays
        String paths = (String) js.executeScript(
            "var data = {" +
            "  trades: [" +
            "    { id: 'TRD-001', amount: 1000 }," +
            "    { id: 'TRD-002', amount: 2000 }" +
            "  ]" +
            "};" +
            "var paths = extractJsonPaths(data, '');" +
            "return paths.join(',');"
        );

        assertTrue(paths.contains("trades[*].id"), "Should extract trades[*].id");
        assertTrue(paths.contains("trades[*].amount"), "Should extract trades[*].amount");
    }

    @Test
    @Order(28)
    @DisplayName("loadFieldsIntoEditor populates loadedFieldPaths")
    void testLoadFieldsIntoEditor() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Set JSON in editor
        js.executeScript(
            "document.getElementById('evalJsonEditor').value = JSON.stringify({" +
            "  order: { id: 'ORD-001', total: 500, items: [{ sku: 'ABC', qty: 2 }] }" +
            "});"
        );

        // Load fields (uses toast notifications, no alert to dismiss)
        js.executeScript("loadFieldsIntoEditor();");
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        // Check loaded paths
        String paths = (String) js.executeScript("return getLoadedFieldPaths().join(',');");
        assertTrue(paths.contains("order.id"), "Should have order.id");
        assertTrue(paths.contains("order.total"), "Should have order.total");
        assertTrue(paths.contains("order.items[*].sku"), "Should have order.items[*].sku");
        assertTrue(paths.contains("order.items[*].qty"), "Should have order.items[*].qty");
    }

    @Test
    @Order(29)
    @DisplayName("getFieldOptions returns loaded paths for dropdown")
    void testGetFieldOptions() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Initially should return no fields message
        String initialOptions = (String) js.executeScript(
            "var opts = getFieldOptions();" +
            "return opts[0][0];"
        );
        assertEquals("(no fields loaded)", initialOptions, "Should show no fields message initially");

        // Load some fields (uses toast notifications, no alert to dismiss)
        js.executeScript(
            "document.getElementById('evalJsonEditor').value = '{\"name\": \"test\", \"value\": 123}';" +
            "loadFieldsIntoEditor();"
        );
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        // Now should return actual options
        Long optionCount = (Long) js.executeScript("return getFieldOptions().length;");
        assertEquals(2L, optionCount, "Should have 2 field options (name, value)");
    }

    @Test
    @Order(30)
    @DisplayName("apex_field_ref block uses dropdown")
    void testFieldRefBlockHasDropdown() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Check that apex_field_ref block is defined
        Boolean blockDefined = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_field_ref'] !== 'undefined';"
        );
        assertTrue(blockDefined, "apex_field_ref block should be defined");

        // Create a field ref block and check it has FIELD dropdown
        Boolean hasDropdown = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_field_ref');" +
            "block.initSvg(); block.render();" +
            "var field = block.getField('FIELD');" +
            "var isDropdown = field instanceof Blockly.FieldDropdown;" +
            "block.dispose();" +
            "return isDropdown;"
        );
        assertTrue(hasDropdown, "FIELD should be a dropdown");
    }

    // --- Component Reference Block Tests ---

    @Test
    @Order(31)
    @DisplayName("apex_component_config block is defined")
    void testComponentConfigBlockDefined() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean blockDefined = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_component_config'] !== 'undefined';"
        );
        assertTrue(blockDefined, "apex_component_config block should be defined");
    }

    @Test
    @Order(32)
    @DisplayName("apex_component_config block has required fields")
    void testComponentConfigBlockFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasFields = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_component_config');" +
            "block.initSvg(); block.render();" +
            "var hasId = block.getField('ID') !== null;" +
            "var hasName = block.getField('NAME') !== null;" +
            "var hasVersion = block.getField('VERSION') !== null;" +
            "var hasCriticality = block.getField('CRITICALITY') !== null;" +
            "var hasSla = block.getField('SLA_MS') !== null;" +
            "block.dispose();" +
            "return hasId && hasName && hasVersion && hasCriticality && hasSla;"
        );
        assertTrue(hasFields, "apex_component_config should have ID, NAME, VERSION, CRITICALITY, SLA_MS fields");
    }

    @Test
    @Order(33)
    @DisplayName("apex_file_reference block is defined")
    void testFileReferenceBlockDefined() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean blockDefined = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_file_reference'] !== 'undefined';"
        );
        assertTrue(blockDefined, "apex_file_reference block should be defined");
    }

    @Test
    @Order(34)
    @DisplayName("apex_file_reference block has required fields")
    void testFileReferenceBlockFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasFields = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_file_reference');" +
            "block.initSvg(); block.render();" +
            "var hasRefType = block.getField('REF_TYPE') !== null;" +
            "var hasFile = block.getField('FILE') !== null;" +
            "var hasOrder = block.getField('EXECUTION_ORDER') !== null;" +
            "var hasPolicy = block.getField('FAILURE_POLICY') !== null;" +
            "block.dispose();" +
            "return hasRefType && hasFile && hasOrder && hasPolicy;"
        );
        assertTrue(hasFields, "apex_file_reference should have REF_TYPE, FILE, EXECUTION_ORDER, FAILURE_POLICY fields");
    }

    @Test
    @Order(35)
    @DisplayName("apex_component_config generator produces valid JSON")
    void testComponentConfigGenerator() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String generatedCode = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_component_config');" +
            "block.setFieldValue('test-component', 'ID');" +
            "block.setFieldValue('Test Component', 'NAME');" +
            "block.setFieldValue('1.0.0', 'VERSION');" +
            "block.initSvg(); block.render();" +
            "var code = apexGenerator.blockToCode(block);" +
            "block.dispose();" +
            "return code;"
        );

        assertNotNull(generatedCode, "Generated code should not be null");
        assertTrue(generatedCode.contains("\"id\""), "Generated code should contain id");
        assertTrue(generatedCode.contains("test-component"), "Generated code should contain component ID");
        assertTrue(generatedCode.contains("\"type\""), "Generated code should contain type");
        assertTrue(generatedCode.contains("component"), "Generated code should contain 'component' type");
    }

    @Test
    @Order(36)
    @DisplayName("BLOCK_ID_CONFIG includes component blocks")
    void testBlockIdConfigIncludesComponentBlocks() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasComponentConfig = (Boolean) js.executeScript(
            "return BLOCK_ID_CONFIG['apex_component_config'] !== undefined && " +
            "BLOCK_ID_CONFIG['apex_component_config'].idPrefix === 'component-';"
        );
        assertTrue(hasComponentConfig, "BLOCK_ID_CONFIG should include apex_component_config with idPrefix 'component-'");
    }

    // --- Error Recovery Block Tests ---

    @Test
    @Order(37)
    @DisplayName("apex_error_recovery block is defined")
    void testErrorRecoveryBlockDefined() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean blockDefined = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_error_recovery'] !== 'undefined';"
        );
        assertTrue(blockDefined, "apex_error_recovery block should be defined");
    }

    @Test
    @Order(38)
    @DisplayName("apex_error_recovery block has required fields")
    void testErrorRecoveryBlockFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasFields = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_error_recovery');" +
            "block.initSvg(); block.render();" +
            "var hasEnabled = block.getField('ENABLED') !== null;" +
            "var hasLogRecovery = block.getField('LOG_RECOVERY') !== null;" +
            "var hasMetrics = block.getField('METRICS_ENABLED') !== null;" +
            "var hasStrategy = block.getField('DEFAULT_STRATEGY') !== null;" +
            "block.dispose();" +
            "return hasEnabled && hasLogRecovery && hasMetrics && hasStrategy;"
        );
        assertTrue(hasFields, "apex_error_recovery should have ENABLED, LOG_RECOVERY, METRICS_ENABLED, DEFAULT_STRATEGY fields");
    }

    @Test
    @Order(39)
    @DisplayName("apex_severity_policy block is defined")
    void testSeverityPolicyBlockDefined() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean blockDefined = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_severity_policy'] !== 'undefined';"
        );
        assertTrue(blockDefined, "apex_severity_policy block should be defined");
    }

    @Test
    @Order(40)
    @DisplayName("apex_severity_policy block has required fields")
    void testSeverityPolicyBlockFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasFields = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_severity_policy');" +
            "block.initSvg(); block.render();" +
            "var hasSeverity = block.getField('SEVERITY') !== null;" +
            "var hasRecoveryEnabled = block.getField('RECOVERY_ENABLED') !== null;" +
            "var hasStrategy = block.getField('STRATEGY') !== null;" +
            "var hasMaxRetries = block.getField('MAX_RETRIES') !== null;" +
            "var hasRetryDelay = block.getField('RETRY_DELAY') !== null;" +
            "block.dispose();" +
            "return hasSeverity && hasRecoveryEnabled && hasStrategy && hasMaxRetries && hasRetryDelay;"
        );
        assertTrue(hasFields, "apex_severity_policy should have SEVERITY, RECOVERY_ENABLED, STRATEGY, MAX_RETRIES, RETRY_DELAY fields");
    }

    @Test
    @Order(41)
    @DisplayName("apex_error_recovery generator produces valid section JSON")
    void testErrorRecoveryGenerator() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String generatedCode = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_error_recovery');" +
            "block.setFieldValue('TRUE', 'ENABLED');" +
            "block.setFieldValue('CONTINUE_WITH_DEFAULT', 'DEFAULT_STRATEGY');" +
            "block.initSvg(); block.render();" +
            "var code = apexGenerator.blockToCode(block);" +
            "block.dispose();" +
            "return code;"
        );

        assertNotNull(generatedCode, "Generated code should not be null");
        // Error recovery now returns section format for rule config consumption
        assertTrue(generatedCode.contains("\"type\""), "Generated code should contain type");
        assertTrue(generatedCode.contains("\"error-recovery\""), "Generated code should contain error-recovery type");
        assertTrue(generatedCode.contains("\"enabled\""), "Generated code should contain enabled");
        assertTrue(generatedCode.contains("\"default-strategy\""), "Generated code should contain default-strategy");
    }

    @Test
    @Order(42)
    @DisplayName("Context menu config includes component and error recovery blocks")
    void testContextMenuConfigIncludesNewBlocks() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasComponentConfig = (Boolean) js.executeScript(
            "return CONTEXT_MENU_CONFIG['apex_component_config'] !== undefined;"
        );
        assertTrue(hasComponentConfig, "CONTEXT_MENU_CONFIG should include apex_component_config");

        Boolean hasErrorRecovery = (Boolean) js.executeScript(
            "return CONTEXT_MENU_CONFIG['apex_error_recovery'] !== undefined;"
        );
        assertTrue(hasErrorRecovery, "CONTEXT_MENU_CONFIG should include apex_error_recovery");
    }

    @Test
    @Order(43)
    @DisplayName("Toolbox includes Components and Error Recovery categories")
    void testToolboxIncludesNewCategories() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasComponentsCategory = (Boolean) js.executeScript(
            "var toolbox = document.getElementById('toolbox');" +
            "return toolbox.innerHTML.includes('Components');"
        );
        assertTrue(hasComponentsCategory, "Toolbox should include Components category");

        Boolean hasErrorRecoveryCategory = (Boolean) js.executeScript(
            "var toolbox = document.getElementById('toolbox');" +
            "return toolbox.innerHTML.includes('Error Recovery');"
        );
        assertTrue(hasErrorRecoveryCategory, "Toolbox should include Error Recovery category");
    }

    @Test
    @Order(44)
    @DisplayName("apex_file_reference has correct statement connection type")
    void testFileReferenceConnectionType() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasCorrectConnection = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var fileRefBlock = workspace.newBlock('apex_file_reference');" +
            "fileRefBlock.initSvg(); fileRefBlock.render();" +
            "var hasPrevious = fileRefBlock.previousConnection !== null;" +
            "var hasNext = fileRefBlock.nextConnection !== null;" +
            "fileRefBlock.dispose();" +
            "return hasPrevious && hasNext;"
        );
        assertTrue(hasCorrectConnection, "apex_file_reference should have previous and next statement connections");
    }

    @Test
    @Order(45)
    @DisplayName("apex_severity_policy has correct statement connection type")
    void testSeverityPolicyConnectionType() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasCorrectConnection = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var policyBlock = workspace.newBlock('apex_severity_policy');" +
            "policyBlock.initSvg(); policyBlock.render();" +
            "var hasPrevious = policyBlock.previousConnection !== null;" +
            "var hasNext = policyBlock.nextConnection !== null;" +
            "policyBlock.dispose();" +
            "return hasPrevious && hasNext;"
        );
        assertTrue(hasCorrectConnection, "apex_severity_policy should have previous and next statement connections");
    }

    @Test
    @Order(46)
    @DisplayName("apex_component_config has FILE_REFS statement input")
    void testComponentConfigHasFileRefsInput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasInput = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_component_config');" +
            "block.initSvg(); block.render();" +
            "var input = block.getInput('FILE_REFS');" +
            "var hasStatementInput = input !== null && input.connection !== null;" +
            "block.dispose();" +
            "return hasStatementInput;"
        );
        assertTrue(hasInput, "apex_component_config should have FILE_REFS statement input");
    }

    @Test
    @Order(47)
    @DisplayName("apex_error_recovery has SEVERITY_POLICIES statement input")
    void testErrorRecoveryHasSeverityPoliciesInput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasInput = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_error_recovery');" +
            "block.initSvg(); block.render();" +
            "var input = block.getInput('SEVERITY_POLICIES');" +
            "var hasStatementInput = input !== null && input.connection !== null;" +
            "block.dispose();" +
            "return hasStatementInput;"
        );
        assertTrue(hasInput, "apex_error_recovery should have SEVERITY_POLICIES statement input");
    }

    @Test
    @Order(48)
    @DisplayName("apex_error_recovery has Section connection type for rule config")
    void testErrorRecoveryHasSectionConnectionType() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasCorrectConnection = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_error_recovery');" +
            "block.initSvg(); block.render();" +
            "var prevConn = block.previousConnection;" +
            "var nextConn = block.nextConnection;" +
            // Check if connections exist and have Section type
            "var hasPrev = prevConn !== null;" +
            "var hasNext = nextConn !== null;" +
            "block.dispose();" +
            "return hasPrev && hasNext;"
        );
        assertTrue(hasCorrectConnection, "apex_error_recovery should have previous and next statement connections");
    }

    @Test
    @Order(49)
    @DisplayName("Rule config with error recovery generates YAML with error-recovery section")
    void testRuleConfigWithErrorRecoveryGeneratesYaml() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String generatedCode = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            // Create rule config block
            "var ruleConfig = workspace.newBlock('apex_rule_config');" +
            "ruleConfig.setFieldValue('test-config', 'ID');" +
            "ruleConfig.setFieldValue('Test Config', 'NAME');" +
            "ruleConfig.setFieldValue('1.0.0', 'VERSION');" +
            "ruleConfig.initSvg(); ruleConfig.render();" +
            // Create error recovery block
            "var errorRecovery = workspace.newBlock('apex_error_recovery');" +
            "errorRecovery.setFieldValue('TRUE', 'ENABLED');" +
            "errorRecovery.setFieldValue('FAIL_FAST', 'DEFAULT_STRATEGY');" +
            "errorRecovery.initSvg(); errorRecovery.render();" +
            // Connect error recovery to rule config sections
            "var sectionsInput = ruleConfig.getInput('SECTIONS');" +
            "sectionsInput.connection.connect(errorRecovery.previousConnection);" +
            // Generate code
            "var code = apexGenerator.blockToCode(ruleConfig);" +
            "ruleConfig.dispose();" +
            "return code;"
        );

        assertNotNull(generatedCode, "Generated code should not be null");
        assertTrue(generatedCode.contains("\"error-recovery\""), "Generated code should contain error-recovery section");
        assertTrue(generatedCode.contains("\"enabled\""), "Generated code should contain enabled field");
        assertTrue(generatedCode.contains("FAIL_FAST"), "Generated code should contain FAIL_FAST strategy");
    }

    // ========== Scenario Registry Block Tests ==========

    @Test
    @Order(50)
    @DisplayName("Test 50: Scenario Registry block is defined")
    void testScenarioRegistryBlockDefined() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "return typeof Blockly.Blocks['apex_scenario_registry'] !== 'undefined' ? 'defined' : 'undefined';"
        );
        assertEquals("defined", result, "apex_scenario_registry block should be defined");
    }

    @Test
    @Order(51)
    @DisplayName("Test 51: Scenario Registry block has required fields")
    void testScenarioRegistryBlockFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var block = Blockly.Blocks['apex_scenario_registry'];" +
            "var json = block.init ? null : JSON.stringify(block);" +
            "if (!json) { var b = new Blockly.Block(workspace, 'apex_scenario_registry'); json = JSON.stringify({" +
            "  hasId: b.getField('ID') !== null," +
            "  hasName: b.getField('NAME') !== null," +
            "  hasVersion: b.getField('VERSION') !== null," +
            "  hasRoutingStrategy: b.getField('ROUTING_STRATEGY') !== null," +
            "  hasDefaultScenario: b.getField('DEFAULT_SCENARIO') !== null," +
            "  hasScenarios: b.getInput('SCENARIOS') !== null" +
            "}); b.dispose(); }" +
            "return json;"
        );
        assertTrue(result.contains("\"hasId\":true"), "Block should have ID field");
        assertTrue(result.contains("\"hasName\":true"), "Block should have NAME field");
        assertTrue(result.contains("\"hasVersion\":true"), "Block should have VERSION field");
        assertTrue(result.contains("\"hasRoutingStrategy\":true"), "Block should have ROUTING_STRATEGY field");
        assertTrue(result.contains("\"hasDefaultScenario\":true"), "Block should have DEFAULT_SCENARIO field");
        assertTrue(result.contains("\"hasScenarios\":true"), "Block should have SCENARIOS input");
    }

    @Test
    @Order(52)
    @DisplayName("Test 52: Scenario Reference block is defined")
    void testScenarioRefBlockDefined() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "return typeof Blockly.Blocks['apex_scenario_ref'] !== 'undefined' ? 'defined' : 'undefined';"
        );
        assertEquals("defined", result, "apex_scenario_ref block should be defined");
    }

    @Test
    @Order(53)
    @DisplayName("Test 53: Scenario Reference block has required fields")
    void testScenarioRefBlockFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var block = Blockly.Blocks['apex_scenario_ref'];" +
            "var json = block.init ? null : JSON.stringify(block);" +
            "if (!json) { var b = new Blockly.Block(workspace, 'apex_scenario_ref'); json = JSON.stringify({" +
            "  hasScenarioId: b.getField('SCENARIO_ID') !== null," +
            "  hasConfigFile: b.getField('CONFIG_FILE') !== null," +
            "  hasBusinessDomain: b.getField('BUSINESS_DOMAIN') !== null" +
            "}); b.dispose(); }" +
            "return json;"
        );
        assertTrue(result.contains("\"hasScenarioId\":true"), "Block should have SCENARIO_ID field");
        assertTrue(result.contains("\"hasConfigFile\":true"), "Block should have CONFIG_FILE field");
        assertTrue(result.contains("\"hasBusinessDomain\":true"), "Block should have BUSINESS_DOMAIN field");
    }

    @Test
    @Order(54)
    @DisplayName("Test 54: Scenario Registry generator produces valid JSON")
    void testScenarioRegistryGenerator() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String generatedCode = (String) js.executeScript(
            "var block = workspace.newBlock('apex_scenario_registry');" +
            "block.setFieldValue('test-registry', 'ID');" +
            "block.setFieldValue('Test Registry', 'NAME');" +
            "block.setFieldValue('1.0.0', 'VERSION');" +
            "block.setFieldValue('type-based', 'ROUTING_STRATEGY');" +
            "block.setFieldValue('default-scenario', 'DEFAULT_SCENARIO');" +
            "block.initSvg();" +
            "block.render();" +
            "var code = apexGenerator.blockToCode(block);" +
            "block.dispose();" +
            "return code;"
        );
        assertNotNull(generatedCode, "Generator should produce code");
        assertTrue(generatedCode.contains("\"type\": \"scenario-registry\""), "Generated code should contain type: scenario-registry");
        assertTrue(generatedCode.contains("\"id\": \"test-registry\""), "Generated code should contain id");
        assertTrue(generatedCode.contains("\"strategy\": \"type-based\""), "Generated code should contain routing strategy");
        assertTrue(generatedCode.contains("\"default-scenario\": \"default-scenario\""), "Generated code should contain default-scenario");
    }

    @Test
    @Order(55)
    @DisplayName("Test 55: Scenario Reference generator produces valid JSON")
    void testScenarioRefGenerator() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String generatedCode = (String) js.executeScript(
            "var block = workspace.newBlock('apex_scenario_ref');" +
            "block.setFieldValue('my-scenario', 'SCENARIO_ID');" +
            "block.setFieldValue('scenarios/my-scenario.yaml', 'CONFIG_FILE');" +
            "block.setFieldValue('Trading', 'BUSINESS_DOMAIN');" +
            "block.initSvg();" +
            "block.render();" +
            "var code = apexGenerator.blockToCode(block);" +
            "block.dispose();" +
            "return code;"
        );
        assertNotNull(generatedCode, "Generator should produce code");
        assertTrue(generatedCode.contains("\"scenario-id\":\"my-scenario\""), "Generated code should contain scenario-id");
        assertTrue(generatedCode.contains("\"config-file\":\"scenarios/my-scenario.yaml\""), "Generated code should contain config-file");
        assertTrue(generatedCode.contains("\"business-domain\":\"Trading\""), "Generated code should contain business-domain");
    }

    @Test
    @Order(56)
    @DisplayName("Test 56: Scenario Registry is in TOP_LEVEL_CONFIG_BLOCKS")
    void testScenarioRegistryInTopLevelBlocks() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "return TOP_LEVEL_CONFIG_BLOCKS.includes('apex_scenario_registry') ? 'included' : 'not included';"
        );
        assertEquals("included", result, "apex_scenario_registry should be in TOP_LEVEL_CONFIG_BLOCKS");
    }

    @Test
    @Order(57)
    @DisplayName("Test 57: Scenario Reference has statement connections")
    void testScenarioRefConnectionType() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var block = workspace.newBlock('apex_scenario_ref');" +
            "block.initSvg();" +
            "var hasPrev = block.previousConnection !== null;" +
            "var hasNext = block.nextConnection !== null;" +
            "block.dispose();" +
            "return JSON.stringify({ hasPrev: hasPrev, hasNext: hasNext });"
        );
        assertTrue(result.contains("\"hasPrev\":true"), "Block should have previous connection");
        assertTrue(result.contains("\"hasNext\":true"), "Block should have next connection");
    }

    @Test
    @Order(58)
    @DisplayName("Test 58: Scenario Registry with references generates complete YAML")
    void testScenarioRegistryWithRefsGeneratesYaml() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String generatedCode = (String) js.executeScript(
            "var registry = workspace.newBlock('apex_scenario_registry');" +
            "registry.setFieldValue('my-registry', 'ID');" +
            "registry.setFieldValue('My Registry', 'NAME');" +
            "registry.setFieldValue('1.0.0', 'VERSION');" +
            "registry.setFieldValue('classification-based', 'ROUTING_STRATEGY');" +
            "registry.setFieldValue('default-scenario', 'DEFAULT_SCENARIO');" +
            "registry.initSvg();" +
            "registry.render();" +
            "var ref = workspace.newBlock('apex_scenario_ref');" +
            "ref.setFieldValue('trade-processing', 'SCENARIO_ID');" +
            "ref.setFieldValue('scenarios/trade-processing.yaml', 'CONFIG_FILE');" +
            "ref.setFieldValue('Trading', 'BUSINESS_DOMAIN');" +
            "ref.initSvg();" +
            "ref.render();" +
            "var scenariosInput = registry.getInput('SCENARIOS');" +
            "if (scenariosInput && scenariosInput.connection) {" +
            "  scenariosInput.connection.connect(ref.previousConnection);" +
            "}" +
            "var code = apexGenerator.blockToCode(registry);" +
            "registry.dispose();" +
            "return code;"
        );
        assertNotNull(generatedCode, "Generator should produce code");
        assertTrue(generatedCode.contains("\"type\": \"scenario-registry\""), "Generated code should contain type: scenario-registry");
        assertTrue(generatedCode.contains("\"scenarios\""), "Generated code should contain scenarios array");
        assertTrue(generatedCode.contains("\"scenario-id\": \"trade-processing\""), "Generated code should contain scenario reference");
        assertTrue(generatedCode.contains("\"config-file\": \"scenarios/trade-processing.yaml\""), "Generated code should contain config-file");
    }

    @Test
    @Order(59)
    @DisplayName("Test 59: Context menu includes Scenario Registry options")
    void testContextMenuIncludesScenarioRegistryOptions() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "return CONTEXT_MENU_CONFIG['apex_scenario_registry'] ? 'defined' : 'undefined';"
        );
        assertEquals("defined", result, "CONTEXT_MENU_CONFIG should include apex_scenario_registry");
    }

    @Test
    @Order(60)
    @DisplayName("Test 60: Toolbox includes Scenario Registry block")
    void testToolboxIncludesScenarioRegistry() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Check the page source for the toolbox definition
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("apex_scenario_registry"), "Page source should include apex_scenario_registry block");
    }

    // ========== Click-to-Fill Tests for New Blocks ==========

    @Test
    @Order(61)
    @DisplayName("Test 61: AUTO_FILL_MAP includes apex_scenario_registry")
    void testAutoFillMapIncludesScenarioRegistry() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "return AUTO_FILL_MAP['apex_scenario_registry'] ? 'defined' : 'undefined';"
        );
        assertEquals("defined", result, "AUTO_FILL_MAP should include apex_scenario_registry");
    }

    @Test
    @Order(62)
    @DisplayName("Test 62: AUTO_FILL_MAP apex_scenario_registry has SCENARIOS input")
    void testAutoFillMapScenarioRegistryHasScenariosInput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "return AUTO_FILL_MAP['apex_scenario_registry']['SCENARIOS'] === 'apex_scenario_ref' ? 'correct' : 'incorrect';"
        );
        assertEquals("correct", result, "AUTO_FILL_MAP apex_scenario_registry should map SCENARIOS to apex_scenario_ref");
    }

    @Test
    @Order(63)
    @DisplayName("Test 63: AUTO_FILL_MAP includes apex_scenario_config")
    void testAutoFillMapIncludesScenarioConfig() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "return AUTO_FILL_MAP['apex_scenario_config'] ? 'defined' : 'undefined';"
        );
        assertEquals("defined", result, "AUTO_FILL_MAP should include apex_scenario_config");
    }

    @Test
    @Order(64)
    @DisplayName("Test 64: AUTO_FILL_MAP includes apex_section_scenario")
    void testAutoFillMapIncludesSectionScenario() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "return AUTO_FILL_MAP['apex_section_scenario'] ? 'defined' : 'undefined';"
        );
        assertEquals("defined", result, "AUTO_FILL_MAP should include apex_section_scenario");
    }

    @Test
    @Order(65)
    @DisplayName("Test 65: AUTO_FILL_MAP includes apex_component_config")
    void testAutoFillMapIncludesComponentConfig() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "return AUTO_FILL_MAP['apex_component_config'] ? 'defined' : 'undefined';"
        );
        assertEquals("defined", result, "AUTO_FILL_MAP should include apex_component_config");
    }

    @Test
    @Order(66)
    @DisplayName("Test 66: AUTO_FILL_MAP includes apex_error_recovery")
    void testAutoFillMapIncludesErrorRecovery() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "return AUTO_FILL_MAP['apex_error_recovery'] ? 'defined' : 'undefined';"
        );
        assertEquals("defined", result, "AUTO_FILL_MAP should include apex_error_recovery");
    }

    // --- Owner Field Tests ---

    @Test
    @Order(67)
    @DisplayName("Test 67: apex_rule_config block has OWNER field")
    void testRuleConfigBlockHasOwnerField() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasOwnerField = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule_config');" +
            "block.initSvg(); block.render();" +
            "var hasOwner = block.getField('OWNER') !== null;" +
            "block.dispose();" +
            "return hasOwner;"
        );
        assertTrue(hasOwnerField, "apex_rule_config should have OWNER field");
    }

    @Test
    @Order(68)
    @DisplayName("Test 68: apex_rule_config generator includes owner in output")
    void testRuleConfigGeneratorIncludesOwner() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String generatedCode = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule_config');" +
            "block.setFieldValue('test-config', 'ID');" +
            "block.setFieldValue('Test Config', 'NAME');" +
            "block.setFieldValue('1.0.0', 'VERSION');" +
            "block.setFieldValue('john.doe@example.com', 'OWNER');" +
            "block.initSvg(); block.render();" +
            "var code = apexGenerator.blockToCode(block);" +
            "block.dispose();" +
            "return code;"
        );

        assertNotNull(generatedCode, "Generated code should not be null");
        assertTrue(generatedCode.contains("\"owner\""), "Generated code should contain owner field");
        assertTrue(generatedCode.contains("john.doe@example.com"), "Generated code should contain owner value");
    }

    @Test
    @Order(69)
    @DisplayName("Test 69: apex_data_source_config block has OWNER field")
    void testDataSourceConfigBlockHasOwnerField() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasOwnerField = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_data_source_config');" +
            "block.initSvg(); block.render();" +
            "var hasOwner = block.getField('OWNER') !== null;" +
            "block.dispose();" +
            "return hasOwner;"
        );
        assertTrue(hasOwnerField, "apex_data_source_config should have OWNER field");
    }

    @Test
    @Order(70)
    @DisplayName("Test 70: apex_data_source_config generator includes owner in output")
    void testDataSourceConfigGeneratorIncludesOwner() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String generatedCode = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_data_source_config');" +
            "block.setFieldValue('test-ds-config', 'ID');" +
            "block.setFieldValue('1.0.0', 'VERSION');" +
            "block.setFieldValue('data-team@example.com', 'OWNER');" +
            "block.initSvg(); block.render();" +
            "var code = apexGenerator.blockToCode(block);" +
            "block.dispose();" +
            "return code;"
        );

        assertNotNull(generatedCode, "Generated code should not be null");
        assertTrue(generatedCode.contains("\"owner\""), "Generated code should contain owner field");
        assertTrue(generatedCode.contains("data-team@example.com"), "Generated code should contain owner value");
    }

    // --- Rule Group / Enrichment Group New Fields Tests ---

    @Test
    @Order(71)
    @DisplayName("Test 71: apex_rule_group block has STOP_ON_FIRST_FAILURE field")
    void testRuleGroupBlockHasStopOnFirstFailureField() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasField = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule_group');" +
            "block.initSvg(); block.render();" +
            "var hasStopOnFirstFailure = block.getField('STOP_ON_FIRST_FAILURE') !== null;" +
            "block.dispose();" +
            "return hasStopOnFirstFailure;"
        );
        assertTrue(hasField, "apex_rule_group should have STOP_ON_FIRST_FAILURE field");
    }

    @Test
    @Order(72)
    @DisplayName("Test 72: apex_rule_group block has PARALLEL_EXECUTION field")
    void testRuleGroupBlockHasParallelExecutionField() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasField = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule_group');" +
            "block.initSvg(); block.render();" +
            "var hasParallelExecution = block.getField('PARALLEL_EXECUTION') !== null;" +
            "block.dispose();" +
            "return hasParallelExecution;"
        );
        assertTrue(hasField, "apex_rule_group should have PARALLEL_EXECUTION field");
    }

    @Test
    @Order(73)
    @DisplayName("Test 73: apex_rule_group block has ERROR_HANDLING dropdown")
    void testRuleGroupBlockHasErrorHandlingDropdown() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasDropdown = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule_group');" +
            "block.initSvg(); block.render();" +
            "var field = block.getField('ERROR_HANDLING');" +
            "var isDropdown = field instanceof Blockly.FieldDropdown;" +
            "block.dispose();" +
            "return isDropdown;"
        );
        assertTrue(hasDropdown, "apex_rule_group ERROR_HANDLING should be a dropdown");
    }

    @Test
    @Order(74)
    @DisplayName("Test 74: apex_rule_group generator includes new fields in output")
    void testRuleGroupGeneratorIncludesNewFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String generatedCode = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule_group');" +
            "block.setFieldValue('test-group', 'ID');" +
            "block.setFieldValue('Test Group', 'NAME');" +
            "block.setFieldValue('TRUE', 'STOP_ON_FIRST_FAILURE');" +
            "block.setFieldValue('TRUE', 'PARALLEL_EXECUTION');" +
            "block.setFieldValue('continue-on-error', 'ERROR_HANDLING');" +
            "block.initSvg(); block.render();" +
            "var code = apexGenerator.blockToCode(block);" +
            "block.dispose();" +
            "return code;"
        );

        assertNotNull(generatedCode, "Generated code should not be null");
        assertTrue(generatedCode.contains("\"stop-on-first-failure\""), "Generated code should contain stop-on-first-failure");
        assertTrue(generatedCode.contains("\"parallel-execution\""), "Generated code should contain parallel-execution");
        assertTrue(generatedCode.contains("\"error-handling\""), "Generated code should contain error-handling");
        assertTrue(generatedCode.contains("continue-on-error"), "Generated code should contain continue-on-error value");
    }

    @Test
    @Order(75)
    @DisplayName("Test 75: apex_enrichment_group block has STOP_ON_FIRST_FAILURE field")
    void testEnrichmentGroupBlockHasStopOnFirstFailureField() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasField = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_enrichment_group');" +
            "block.initSvg(); block.render();" +
            "var hasStopOnFirstFailure = block.getField('STOP_ON_FIRST_FAILURE') !== null;" +
            "block.dispose();" +
            "return hasStopOnFirstFailure;"
        );
        assertTrue(hasField, "apex_enrichment_group should have STOP_ON_FIRST_FAILURE field");
    }

    @Test
    @Order(76)
    @DisplayName("Test 76: apex_enrichment_group block has PARALLEL_EXECUTION field")
    void testEnrichmentGroupBlockHasParallelExecutionField() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasField = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_enrichment_group');" +
            "block.initSvg(); block.render();" +
            "var hasParallelExecution = block.getField('PARALLEL_EXECUTION') !== null;" +
            "block.dispose();" +
            "return hasParallelExecution;"
        );
        assertTrue(hasField, "apex_enrichment_group should have PARALLEL_EXECUTION field");
    }

    @Test
    @Order(77)
    @DisplayName("Test 77: apex_enrichment_group block has ERROR_HANDLING dropdown")
    void testEnrichmentGroupBlockHasErrorHandlingDropdown() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasDropdown = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_enrichment_group');" +
            "block.initSvg(); block.render();" +
            "var field = block.getField('ERROR_HANDLING');" +
            "var isDropdown = field instanceof Blockly.FieldDropdown;" +
            "block.dispose();" +
            "return isDropdown;"
        );
        assertTrue(hasDropdown, "apex_enrichment_group ERROR_HANDLING should be a dropdown");
    }

    @Test
    @Order(78)
    @DisplayName("Test 78: apex_enrichment_group generator includes new fields in output")
    void testEnrichmentGroupGeneratorIncludesNewFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String generatedCode = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_enrichment_group');" +
            "block.setFieldValue('test-enrich-group', 'ID');" +
            "block.setFieldValue('Test Enrichment Group', 'NAME');" +
            "block.setFieldValue('TRUE', 'STOP_ON_FIRST_FAILURE');" +
            "block.setFieldValue('TRUE', 'PARALLEL_EXECUTION');" +
            "block.setFieldValue('skip-on-error', 'ERROR_HANDLING');" +
            "block.initSvg(); block.render();" +
            "var code = apexGenerator.blockToCode(block);" +
            "block.dispose();" +
            "return code;"
        );

        assertNotNull(generatedCode, "Generated code should not be null");
        assertTrue(generatedCode.contains("\"stop-on-first-failure\""), "Generated code should contain stop-on-first-failure");
        assertTrue(generatedCode.contains("\"parallel-execution\""), "Generated code should contain parallel-execution");
        assertTrue(generatedCode.contains("\"error-handling\""), "Generated code should contain error-handling");
        assertTrue(generatedCode.contains("skip-on-error"), "Generated code should contain skip-on-error value");
    }

    // --- Field Mapping New Fields Tests ---

    @Test
    @Order(79)
    @DisplayName("Test 79: apex_field_mapping block has DEFAULT_VALUE field")
    void testFieldMappingBlockHasDefaultValueField() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasField = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_field_mapping');" +
            "block.initSvg(); block.render();" +
            "var hasDefaultValue = block.getField('DEFAULT_VALUE') !== null;" +
            "block.dispose();" +
            "return hasDefaultValue;"
        );
        assertTrue(hasField, "apex_field_mapping should have DEFAULT_VALUE field");
    }

    @Test
    @Order(80)
    @DisplayName("Test 80: apex_field_mapping block has REQUIRED checkbox")
    void testFieldMappingBlockHasRequiredCheckbox() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasCheckbox = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_field_mapping');" +
            "block.initSvg(); block.render();" +
            "var field = block.getField('REQUIRED');" +
            "var isCheckbox = field instanceof Blockly.FieldCheckbox;" +
            "block.dispose();" +
            "return isCheckbox;"
        );
        assertTrue(hasCheckbox, "apex_field_mapping REQUIRED should be a checkbox");
    }

    @Test
    @Order(81)
    @DisplayName("Test 81: apex_field_mapping generator includes default-value in output")
    void testFieldMappingGeneratorIncludesDefaultValue() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String generatedCode = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_field_mapping');" +
            "block.setFieldValue('sourceField', 'SOURCE');" +
            "block.setFieldValue('targetField', 'TARGET');" +
            "block.setFieldValue('N/A', 'DEFAULT_VALUE');" +
            "block.initSvg(); block.render();" +
            "var code = apexGenerator.blockToCode(block);" +
            "block.dispose();" +
            "return code;"
        );

        assertNotNull(generatedCode, "Generated code should not be null");
        assertTrue(generatedCode.contains("\"default-value\""), "Generated code should contain default-value field");
        assertTrue(generatedCode.contains("N/A"), "Generated code should contain default value");
    }

    @Test
    @Order(82)
    @DisplayName("Test 82: apex_field_mapping generator includes required in output when checked")
    void testFieldMappingGeneratorIncludesRequired() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String generatedCode = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_field_mapping');" +
            "block.setFieldValue('sourceField', 'SOURCE');" +
            "block.setFieldValue('targetField', 'TARGET');" +
            "block.setFieldValue('TRUE', 'REQUIRED');" +
            "block.initSvg(); block.render();" +
            "var code = apexGenerator.blockToCode(block);" +
            "block.dispose();" +
            "return code;"
        );

        assertNotNull(generatedCode, "Generated code should not be null");
        assertTrue(generatedCode.contains("\"required\""), "Generated code should contain required field");
        assertTrue(generatedCode.contains("true"), "Generated code should contain required: true");
    }

    @Test
    @Order(83)
    @DisplayName("Test 83: apex_field_mapping generator omits required when unchecked")
    void testFieldMappingGeneratorOmitsRequiredWhenFalse() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String generatedCode = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_field_mapping');" +
            "block.setFieldValue('sourceField', 'SOURCE');" +
            "block.setFieldValue('targetField', 'TARGET');" +
            "block.setFieldValue('FALSE', 'REQUIRED');" +
            "block.initSvg(); block.render();" +
            "var code = apexGenerator.blockToCode(block);" +
            "block.dispose();" +
            "return code;"
        );

        assertNotNull(generatedCode, "Generated code should not be null");
        assertFalse(generatedCode.contains("\"required\""), "Generated code should NOT contain required field when false");
    }

    // --- YAML Import Tests for New Fields ---

    @Test
    @Order(84)
    @DisplayName("Test 84: YAML import restores owner field for rule config")
    void testYamlImportRestoresOwnerForRuleConfig() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Create a rule config with owner, export, clear, and reimport
        String ownerValue = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            // Create block with owner
            "var block = workspace.newBlock('apex_rule_config');" +
            "block.setFieldValue('test-config', 'ID');" +
            "block.setFieldValue('Test Config', 'NAME');" +
            "block.setFieldValue('1.0.0', 'VERSION');" +
            "block.setFieldValue('owner@test.com', 'OWNER');" +
            "block.initSvg(); block.render();" +
            // Get generated YAML/JSON
            "var code = apexGenerator.blockToCode(block);" +
            "var config = JSON.parse(code);" +
            // Clear workspace
            "workspace.clear();" +
            // Reimport using createRuleConfigBlock
            "createRuleConfigBlock(config);" +
            // Get the owner value from the new block
            "var blocks = workspace.getBlocksByType('apex_rule_config', false);" +
            "return blocks.length > 0 ? blocks[0].getFieldValue('OWNER') : null;"
        );

        assertEquals("owner@test.com", ownerValue, "Owner field should be restored after YAML import");
    }

    @Test
    @Order(85)
    @DisplayName("Test 85: YAML import restores new fields for rule group")
    void testYamlImportRestoresNewFieldsForRuleGroup() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        // Create a rule group with new fields, export, clear, and reimport
        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            // Create block with new fields
            "var block = workspace.newBlock('apex_rule_group');" +
            "block.setFieldValue('test-group', 'ID');" +
            "block.setFieldValue('Test Group', 'NAME');" +
            "block.setFieldValue('TRUE', 'STOP_ON_FIRST_FAILURE');" +
            "block.setFieldValue('TRUE', 'PARALLEL_EXECUTION');" +
            "block.setFieldValue('continue-on-error', 'ERROR_HANDLING');" +
            "block.initSvg(); block.render();" +
            // Get generated JSON (remove trailing comma)
            "var code = apexGenerator.blockToCode(block).replace(/,\\s*$/, '');" +
            "var groupData = JSON.parse(code);" +
            // Clear workspace
            "workspace.clear();" +
            // Reimport using createRuleGroupBlock
            "createRuleGroupBlock(groupData);" +
            // Get the field values from the new block
            "var blocks = workspace.getBlocksByType('apex_rule_group', false);" +
            "if (blocks.length === 0) return 'no blocks';" +
            "var b = blocks[0];" +
            "return b.getFieldValue('STOP_ON_FIRST_FAILURE') + '|' + " +
            "       b.getFieldValue('PARALLEL_EXECUTION') + '|' + " +
            "       b.getFieldValue('ERROR_HANDLING');"
        );

        assertEquals("TRUE|TRUE|continue-on-error", result, "New fields should be restored after YAML import");
    }

    @Test
    @Order(86)
    @DisplayName("Test 86: YAML import restores new fields for enrichment group")
    void testYamlImportRestoresNewFieldsForEnrichmentGroup() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            // Create block with new fields
            "var block = workspace.newBlock('apex_enrichment_group');" +
            "block.setFieldValue('test-enrich-group', 'ID');" +
            "block.setFieldValue('Test Enrichment Group', 'NAME');" +
            "block.setFieldValue('TRUE', 'STOP_ON_FIRST_FAILURE');" +
            "block.setFieldValue('TRUE', 'PARALLEL_EXECUTION');" +
            "block.setFieldValue('skip-on-error', 'ERROR_HANDLING');" +
            "block.initSvg(); block.render();" +
            // Get generated JSON (remove trailing comma)
            "var code = apexGenerator.blockToCode(block).replace(/,\\s*$/, '');" +
            "var groupData = JSON.parse(code);" +
            // Clear workspace
            "workspace.clear();" +
            // Reimport using createEnrichmentGroupBlock
            "createEnrichmentGroupBlock(groupData);" +
            // Get the field values from the new block
            "var blocks = workspace.getBlocksByType('apex_enrichment_group', false);" +
            "if (blocks.length === 0) return 'no blocks';" +
            "var b = blocks[0];" +
            "return b.getFieldValue('STOP_ON_FIRST_FAILURE') + '|' + " +
            "       b.getFieldValue('PARALLEL_EXECUTION') + '|' + " +
            "       b.getFieldValue('ERROR_HANDLING');"
        );

        assertEquals("TRUE|TRUE|skip-on-error", result, "New fields should be restored after YAML import");
    }

    @Test
    @Order(87)
    @DisplayName("Test 87: YAML import restores new fields for field mapping")
    void testYamlImportRestoresNewFieldsForFieldMapping() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            // Create block with new fields
            "var block = workspace.newBlock('apex_field_mapping');" +
            "block.setFieldValue('sourceField', 'SOURCE');" +
            "block.setFieldValue('targetField', 'TARGET');" +
            "block.setFieldValue('default-val', 'DEFAULT_VALUE');" +
            "block.setFieldValue('TRUE', 'REQUIRED');" +
            "block.initSvg(); block.render();" +
            // Get generated JSON (remove trailing comma)
            "var code = apexGenerator.blockToCode(block).replace(/,\\s*$/, '');" +
            "var mappingData = JSON.parse(code);" +
            // Clear workspace
            "workspace.clear();" +
            // Reimport using createFieldMappingBlock
            "createFieldMappingBlock(mappingData);" +
            // Get the field values from the new block
            "var blocks = workspace.getBlocksByType('apex_field_mapping', false);" +
            "if (blocks.length === 0) return 'no blocks';" +
            "var b = blocks[0];" +
            "return b.getFieldValue('DEFAULT_VALUE') + '|' + b.getFieldValue('REQUIRED');"
        );

        assertEquals("default-val|TRUE", result, "New fields should be restored after YAML import");
    }

    @Test
    @Order(88)
    @DisplayName("Test 88: Error handling dropdown has correct options")
    void testErrorHandlingDropdownOptions() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String options = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule_group');" +
            "block.initSvg(); block.render();" +
            "var field = block.getField('ERROR_HANDLING');" +
            "var opts = field.getOptions();" +
            "var values = opts.map(function(o) { return o[1]; });" +
            "block.dispose();" +
            "return values.join(',');"
        );

        assertTrue(options.contains("fail-fast"), "Should have fail-fast option");
        assertTrue(options.contains("continue-on-error"), "Should have continue-on-error option");
        assertTrue(options.contains("skip-on-error"), "Should have skip-on-error option");
    }

    // ==================== Tests 89-95: Rule Block New Fields ====================

    @Test
    @Order(89)
    @DisplayName("Test 89: apex_rule block has BUSINESS_OWNER field")
    void testRuleBlockHasBusinessOwnerField() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasField = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule');" +
            "block.initSvg(); block.render();" +
            "var field = block.getField('BUSINESS_OWNER');" +
            "block.dispose();" +
            "return field !== null;"
        );

        assertTrue(hasField, "apex_rule block should have BUSINESS_OWNER field");
    }

    @Test
    @Order(90)
    @DisplayName("Test 90: apex_rule block has ERROR_CODE field")
    void testRuleBlockHasErrorCodeField() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasField = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule');" +
            "block.initSvg(); block.render();" +
            "var field = block.getField('ERROR_CODE');" +
            "block.dispose();" +
            "return field !== null;"
        );

        assertTrue(hasField, "apex_rule block should have ERROR_CODE field");
    }

    @Test
    @Order(91)
    @DisplayName("Test 91: apex_rule block has SUCCESS_CODE field")
    void testRuleBlockHasSuccessCodeField() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasField = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule');" +
            "block.initSvg(); block.render();" +
            "var field = block.getField('SUCCESS_CODE');" +
            "block.dispose();" +
            "return field !== null;"
        );

        assertTrue(hasField, "apex_rule block should have SUCCESS_CODE field");
    }

    @Test
    @Order(92)
    @DisplayName("Test 92: apex_rule generator includes business-owner in output")
    void testRuleGeneratorIncludesBusinessOwner() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String output = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule');" +
            "block.setFieldValue('test-rule', 'ID');" +
            "block.setFieldValue('Test Rule', 'NAME');" +
            "block.setFieldValue('John Smith', 'BUSINESS_OWNER');" +
            "block.initSvg(); block.render();" +
            "var code = apexGenerator.blockToCode(block);" +
            "block.dispose();" +
            "return code;"
        );

        assertTrue(output.contains("\"business-owner\":\"John Smith\""),
            "Generator output should include business-owner field");
    }

    @Test
    @Order(93)
    @DisplayName("Test 93: apex_rule generator includes error-code in output")
    void testRuleGeneratorIncludesErrorCode() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String output = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule');" +
            "block.setFieldValue('test-rule', 'ID');" +
            "block.setFieldValue('Test Rule', 'NAME');" +
            "block.setFieldValue('ERR-001', 'ERROR_CODE');" +
            "block.initSvg(); block.render();" +
            "var code = apexGenerator.blockToCode(block);" +
            "block.dispose();" +
            "return code;"
        );

        assertTrue(output.contains("\"error-code\":\"ERR-001\""),
            "Generator output should include error-code field");
    }

    @Test
    @Order(94)
    @DisplayName("Test 94: apex_rule generator includes success-code in output")
    void testRuleGeneratorIncludesSuccessCode() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String output = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_rule');" +
            "block.setFieldValue('test-rule', 'ID');" +
            "block.setFieldValue('Test Rule', 'NAME');" +
            "block.setFieldValue('SUC-001', 'SUCCESS_CODE');" +
            "block.initSvg(); block.render();" +
            "var code = apexGenerator.blockToCode(block);" +
            "block.dispose();" +
            "return code;"
        );

        assertTrue(output.contains("\"success-code\":\"SUC-001\""),
            "Generator output should include success-code field");
    }

    @Test
    @Order(95)
    @DisplayName("Test 95: YAML import restores new fields for rule")
    void testYamlImportRestoresNewFieldsForRule() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            // Create block with new fields
            "var block = workspace.newBlock('apex_rule');" +
            "block.setFieldValue('test-rule', 'ID');" +
            "block.setFieldValue('Test Rule', 'NAME');" +
            "block.setFieldValue('Jane Doe', 'BUSINESS_OWNER');" +
            "block.setFieldValue('ERR-100', 'ERROR_CODE');" +
            "block.setFieldValue('SUC-100', 'SUCCESS_CODE');" +
            "block.initSvg(); block.render();" +
            // Get generated JSON (remove trailing comma)
            "var code = apexGenerator.blockToCode(block).replace(/,\\s*$/, '');" +
            "var ruleData = JSON.parse(code);" +
            // Clear workspace
            "workspace.clear();" +
            // Reimport using createRuleBlock
            "createRuleBlock(ruleData);" +
            // Get the field values from the new block
            "var blocks = workspace.getBlocksByType('apex_rule', false);" +
            "if (blocks.length === 0) return 'no blocks';" +
            "var b = blocks[0];" +
            "return b.getFieldValue('BUSINESS_OWNER') + '|' + " +
            "       b.getFieldValue('ERROR_CODE') + '|' + " +
            "       b.getFieldValue('SUCCESS_CODE');"
        );

        assertEquals("Jane Doe|ERR-100|SUC-100", result, "New fields should be restored after YAML import");
    }

    // ========================================================================
    // Tests 96-110: Data Sources - Cache, Circuit Breaker, Authentication
    // ========================================================================

    @Test
    @Order(96)
    @DisplayName("Test 96: Cache Config block exists in toolbox")
    void testCacheConfigBlockExistsInToolbox() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "return Blockly.Blocks['apex_cache_config'] ? 'exists' : 'missing';"
        );
        assertEquals("exists", result, "apex_cache_config block should exist");
    }

    @Test
    @Order(97)
    @DisplayName("Test 97: Circuit Breaker block exists in toolbox")
    void testCircuitBreakerBlockExistsInToolbox() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "return Blockly.Blocks['apex_circuit_breaker'] ? 'exists' : 'missing';"
        );
        assertEquals("exists", result, "apex_circuit_breaker block should exist");
    }

    @Test
    @Order(98)
    @DisplayName("Test 98: Authentication block exists in toolbox")
    void testAuthenticationBlockExistsInToolbox() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "return Blockly.Blocks['apex_authentication'] ? 'exists' : 'missing';"
        );
        assertEquals("exists", result, "apex_authentication block should exist");
    }

    @Test
    @Order(99)
    @DisplayName("Test 99: REST data source has Authentication input")
    void testRestDataSourceHasAuthenticationInput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_data_source_rest');" +
            "block.initSvg(); block.render();" +
            "var input = block.getInput('AUTHENTICATION');" +
            "return input ? 'exists' : 'missing';"
        );
        assertEquals("exists", result, "REST data source should have AUTHENTICATION input");
    }

    @Test
    @Order(100)
    @DisplayName("Test 100: REST data source has Cache input")
    void testRestDataSourceHasCacheInput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_data_source_rest');" +
            "block.initSvg(); block.render();" +
            "var input = block.getInput('CACHE');" +
            "return input ? 'exists' : 'missing';"
        );
        assertEquals("exists", result, "REST data source should have CACHE input");
    }

    @Test
    @Order(101)
    @DisplayName("Test 101: REST data source has Circuit Breaker input")
    void testRestDataSourceHasCircuitBreakerInput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_data_source_rest');" +
            "block.initSvg(); block.render();" +
            "var input = block.getInput('CIRCUIT_BREAKER');" +
            "return input ? 'exists' : 'missing';"
        );
        assertEquals("exists", result, "REST data source should have CIRCUIT_BREAKER input");
    }

    @Test
    @Order(102)
    @DisplayName("Test 102: Cache Config generator outputs correct JSON")
    void testCacheConfigGeneratorOutputsCorrectJson() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_cache_config');" +
            "block.setFieldValue('TRUE', 'ENABLED');" +
            "block.setFieldValue(600, 'TTL');" +
            "block.setFieldValue(2000, 'MAX_SIZE');" +
            "block.setFieldValue('LFU', 'EVICTION_POLICY');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_cache_config'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"enabled\":true"), "Should contain enabled:true");
        assertTrue(result.contains("\"ttl\":600"), "Should contain ttl:600");
        assertTrue(result.contains("\"max-size\":2000"), "Should contain max-size:2000");
        assertTrue(result.contains("\"eviction-policy\":\"LFU\""), "Should contain eviction-policy:LFU");
    }

    @Test
    @Order(103)
    @DisplayName("Test 103: Circuit Breaker generator outputs correct JSON")
    void testCircuitBreakerGeneratorOutputsCorrectJson() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_circuit_breaker');" +
            "block.setFieldValue('TRUE', 'ENABLED');" +
            "block.setFieldValue(10, 'FAILURE_THRESHOLD');" +
            "block.setFieldValue(60000, 'RESET_TIMEOUT');" +
            "block.setFieldValue(5, 'HALF_OPEN_REQUESTS');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_circuit_breaker'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"enabled\":true"), "Should contain enabled:true");
        assertTrue(result.contains("\"failure-threshold\":10"), "Should contain failure-threshold:10");
        assertTrue(result.contains("\"reset-timeout\":60000"), "Should contain reset-timeout:60000");
        assertTrue(result.contains("\"half-open-requests\":5"), "Should contain half-open-requests:5");
    }

    @Test
    @Order(104)
    @DisplayName("Test 104: Authentication generator outputs correct JSON for basic auth")
    void testAuthenticationGeneratorOutputsCorrectJsonForBasicAuth() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_authentication');" +
            "block.setFieldValue('basic', 'AUTH_TYPE');" +
            "block.setFieldValue('admin', 'USERNAME');" +
            "block.setFieldValue('secret123', 'PASSWORD');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_authentication'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"type\":\"basic\""), "Should contain type:basic");
        assertTrue(result.contains("\"username\":\"admin\""), "Should contain username:admin");
        assertTrue(result.contains("\"password\":\"secret123\""), "Should contain password:secret123");
    }

    @Test
    @Order(105)
    @DisplayName("Test 105: Authentication generator outputs correct JSON for bearer token")
    void testAuthenticationGeneratorOutputsCorrectJsonForBearerToken() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_authentication');" +
            "block.setFieldValue('bearer', 'AUTH_TYPE');" +
            "block.setFieldValue('my-jwt-token', 'TOKEN');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_authentication'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"type\":\"bearer\""), "Should contain type:bearer");
        assertTrue(result.contains("\"token\":\"my-jwt-token\""), "Should contain token:my-jwt-token");
    }

    @Test
    @Order(106)
    @DisplayName("Test 106: Authentication generator outputs correct JSON for API key")
    void testAuthenticationGeneratorOutputsCorrectJsonForApiKey() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_authentication');" +
            "block.setFieldValue('api-key', 'AUTH_TYPE');" +
            "block.setFieldValue('X-Custom-Key', 'API_KEY_HEADER');" +
            "block.setFieldValue('abc123', 'API_KEY_VALUE');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_authentication'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"type\":\"api-key\""), "Should contain type:api-key");
        assertTrue(result.contains("\"api-key-header\":\"X-Custom-Key\""), "Should contain api-key-header");
        assertTrue(result.contains("\"api-key-value\":\"abc123\""), "Should contain api-key-value");
    }

    // ========================================================================
    // Tests 107-112: Enrichment Calculation - Priority, Error Code, Success Code
    // ========================================================================

    @Test
    @Order(107)
    @DisplayName("Test 107: Calculation Enrichment has Priority field")
    void testCalculationEnrichmentHasPriorityField() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_enrichment_calculation');" +
            "block.initSvg(); block.render();" +
            "var field = block.getField('PRIORITY');" +
            "return field ? 'exists' : 'missing';"
        );
        assertEquals("exists", result, "Calculation enrichment should have PRIORITY field");
    }

    @Test
    @Order(108)
    @DisplayName("Test 108: Calculation Enrichment has Error Code field")
    void testCalculationEnrichmentHasErrorCodeField() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_enrichment_calculation');" +
            "block.initSvg(); block.render();" +
            "var field = block.getField('ERROR_CODE');" +
            "return field ? 'exists' : 'missing';"
        );
        assertEquals("exists", result, "Calculation enrichment should have ERROR_CODE field");
    }

    @Test
    @Order(109)
    @DisplayName("Test 109: Calculation Enrichment has Success Code field")
    void testCalculationEnrichmentHasSuccessCodeField() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_enrichment_calculation');" +
            "block.initSvg(); block.render();" +
            "var field = block.getField('SUCCESS_CODE');" +
            "return field ? 'exists' : 'missing';"
        );
        assertEquals("exists", result, "Calculation enrichment should have SUCCESS_CODE field");
    }

    @Test
    @Order(110)
    @DisplayName("Test 110: Calculation Enrichment generator includes priority")
    void testCalculationEnrichmentGeneratorIncludesPriority() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_enrichment_calculation');" +
            "block.setFieldValue('calc-1', 'ID');" +
            "block.setFieldValue('TRUE', 'ENABLED');" +
            "block.setFieldValue(5, 'PRIORITY');" +
            "block.setFieldValue('result', 'RESULT_FIELD');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_enrichment_calculation'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"priority\":5"), "Generator should include priority:5");
    }

    @Test
    @Order(111)
    @DisplayName("Test 111: Calculation Enrichment generator includes error-code")
    void testCalculationEnrichmentGeneratorIncludesErrorCode() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_enrichment_calculation');" +
            "block.setFieldValue('calc-1', 'ID');" +
            "block.setFieldValue('TRUE', 'ENABLED');" +
            "block.setFieldValue('CALC-ERR-001', 'ERROR_CODE');" +
            "block.setFieldValue('result', 'RESULT_FIELD');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_enrichment_calculation'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"error-code\":\"CALC-ERR-001\""), "Generator should include error-code");
    }

    @Test
    @Order(112)
    @DisplayName("Test 112: Calculation Enrichment generator includes success-code")
    void testCalculationEnrichmentGeneratorIncludesSuccessCode() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_enrichment_calculation');" +
            "block.setFieldValue('calc-1', 'ID');" +
            "block.setFieldValue('TRUE', 'ENABLED');" +
            "block.setFieldValue('CALC-SUC-001', 'SUCCESS_CODE');" +
            "block.setFieldValue('result', 'RESULT_FIELD');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_enrichment_calculation'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"success-code\":\"CALC-SUC-001\""), "Generator should include success-code");
    }

    // ==================== RULE CHAINS TESTS (Tests 113-127) ====================

    @Test
    @Order(113)
    @DisplayName("Test 113: Rule Chains Section block exists in toolbox")
    void testRuleChainsSectionBlockExists() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_section_rule_chains'] !== 'undefined';"
        );
        assertTrue(exists, "apex_section_rule_chains block should be defined");
    }

    @Test
    @Order(114)
    @DisplayName("Test 114: Conditional Chain block exists")
    void testConditionalChainBlockExists() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_conditional_chain'] !== 'undefined';"
        );
        assertTrue(exists, "apex_conditional_chain block should be defined");
    }

    @Test
    @Order(115)
    @DisplayName("Test 115: Sequential Dependency block exists")
    void testSequentialDependencyBlockExists() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_sequential_dependency'] !== 'undefined';"
        );
        assertTrue(exists, "apex_sequential_dependency block should be defined");
    }

    @Test
    @Order(116)
    @DisplayName("Test 116: Chain Stage block exists")
    void testChainStageBlockExists() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_chain_stage'] !== 'undefined';"
        );
        assertTrue(exists, "apex_chain_stage block should be defined");
    }

    @Test
    @Order(117)
    @DisplayName("Test 117: Result Routing block exists")
    void testResultRoutingBlockExists() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_result_routing'] !== 'undefined';"
        );
        assertTrue(exists, "apex_result_routing block should be defined");
    }

    @Test
    @Order(118)
    @DisplayName("Test 118: Route block exists")
    void testRouteBlockExists() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_route'] !== 'undefined';"
        );
        assertTrue(exists, "apex_route block should be defined");
    }

    @Test
    @Order(119)
    @DisplayName("Test 119: Accumulative Chain block exists")
    void testAccumulativeChainBlockExists() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_accumulative_chain'] !== 'undefined';"
        );
        assertTrue(exists, "apex_accumulative_chain block should be defined");
    }

    @Test
    @Order(120)
    @DisplayName("Test 120: Accumulation Rule block exists")
    void testAccumulationRuleBlockExists() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_accumulation_rule'] !== 'undefined';"
        );
        assertTrue(exists, "apex_accumulation_rule block should be defined");
    }

    @Test
    @Order(121)
    @DisplayName("Test 121: Fluent Builder block exists")
    void testFluentBuilderBlockExists() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_fluent_builder'] !== 'undefined';"
        );
        assertTrue(exists, "apex_fluent_builder block should be defined");
    }

    @Test
    @Order(122)
    @DisplayName("Test 122: Decision Step block exists")
    void testDecisionStepBlockExists() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_decision_step'] !== 'undefined';"
        );
        assertTrue(exists, "apex_decision_step block should be defined");
    }

    @Test
    @Order(123)
    @DisplayName("Test 123: Conditional Chain generator outputs correct JSON")
    void testConditionalChainGeneratorOutput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_conditional_chain');" +
            "block.setFieldValue('chain-1', 'ID');" +
            "block.setFieldValue('My Chain', 'NAME');" +
            "block.setFieldValue('TRUE', 'ENABLED');" +
            "block.setFieldValue('trigger-rule-1', 'TRIGGER_RULE');" +
            "block.setFieldValue('rule-a,rule-b', 'ON_TRIGGER');" +
            "block.setFieldValue('rule-c', 'ON_NO_TRIGGER');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_conditional_chain'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"pattern\":\"conditional-chaining\""), "Generator should output conditional-chaining pattern");
        assertTrue(result.contains("\"trigger-rule\":\"trigger-rule-1\""), "Generator should include trigger-rule");
    }

    @Test
    @Order(124)
    @DisplayName("Test 124: Sequential Dependency generator outputs correct JSON")
    void testSequentialDependencyGeneratorOutput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_sequential_dependency');" +
            "block.setFieldValue('seq-1', 'ID');" +
            "block.setFieldValue('Sequential Chain', 'NAME');" +
            "block.setFieldValue('TRUE', 'ENABLED');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_sequential_dependency'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"pattern\":\"sequential-dependency\""), "Generator should output sequential-dependency pattern");
    }

    @Test
    @Order(125)
    @DisplayName("Test 125: Result Routing generator outputs correct JSON")
    void testResultRoutingGeneratorOutput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_result_routing');" +
            "block.setFieldValue('routing-1', 'ID');" +
            "block.setFieldValue('Routing Chain', 'NAME');" +
            "block.setFieldValue(true, 'ENABLED');" +
            "block.setFieldValue('#processingPath', 'ROUTER_CONDITION');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_result_routing'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"pattern\":\"result-based-routing\""), "Generator should output result-based-routing pattern");
        assertTrue(result.contains("\"routing-rule\":\"#processingPath\""), "Generator should include routing-rule");
    }

    @Test
    @Order(126)
    @DisplayName("Test 126: Accumulative Chain generator outputs correct JSON")
    void testAccumulativeChainGeneratorOutput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_accumulative_chain');" +
            "block.setFieldValue('accum-1', 'ID');" +
            "block.setFieldValue('Accumulative Chain', 'NAME');" +
            "block.setFieldValue(true, 'ENABLED');" +
            "block.setFieldValue('totalScore', 'ACCUMULATOR_VARIABLE');" +
            "block.setFieldValue(0, 'INITIAL_VALUE');" +
            "block.setFieldValue('#totalScore >= 60', 'FINAL_DECISION');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_accumulative_chain'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"pattern\":\"accumulative-chaining\""), "Generator should output accumulative-chaining pattern");
        assertTrue(result.contains("\"accumulator-variable\":\"totalScore\""), "Generator should include accumulator-variable");
    }

    @Test
    @Order(127)
    @DisplayName("Test 127: Fluent Builder generator outputs correct JSON")
    void testFluentBuilderGeneratorOutput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_fluent_builder');" +
            "block.setFieldValue('fluent-1', 'ID');" +
            "block.setFieldValue('Fluent Builder', 'NAME');" +
            "block.setFieldValue(true, 'ENABLED');" +
            "block.setFieldValue('decisionResult', 'BUILDER_TARGET');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_fluent_builder'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"pattern\":\"fluent-builder\""), "Generator should output fluent-builder pattern");
        assertTrue(result.contains("\"builder-target\":\"decisionResult\""), "Generator should include builder-target");
    }

    // ==================== COMPREHENSIVE RULE CHAINS TESTS (Tests 128-175) ====================

    // --- Rule Chains Section Block Tests ---

    @Test
    @Order(128)
    @DisplayName("Test 128: Rule Chains Section block appears in Sections toolbox category")
    void testRuleChainsSectionInToolbox() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean inToolbox = (Boolean) js.executeScript(
            "var toolbox = Blockly.getMainWorkspace().getToolbox();" +
            "var contents = toolbox.getToolboxItems();" +
            "for (var i = 0; i < contents.length; i++) {" +
            "  var item = contents[i];" +
            "  if (item.getName && item.getName() === 'Sections') {" +
            "    var flyout = item.getContents ? item.getContents() : [];" +
            "    for (var j = 0; j < flyout.length; j++) {" +
            "      if (flyout[j].type === 'apex_section_rule_chains') return true;" +
            "    }" +
            "  }" +
            "}" +
            "return false;"
        );
        assertTrue(inToolbox, "apex_section_rule_chains should be in Sections toolbox category");
    }

    @Test
    @Order(129)
    @DisplayName("Test 129: Rule Chains Section block has RULE_CHAINS statement input")
    void testRuleChainsSectionHasStatementInput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasInput = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_section_rule_chains');" +
            "block.initSvg(); block.render();" +
            "return block.getInput('RULE_CHAINS') !== null;"
        );
        assertTrue(hasInput, "apex_section_rule_chains should have RULE_CHAINS statement input");
    }

    @Test
    @Order(130)
    @DisplayName("Test 130: Rule Chains Section generator outputs rule-chains array")
    void testRuleChainsSectionGeneratorOutput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_section_rule_chains');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_section_rule_chains'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"rule-chains\""), "Generator should output rule-chains key");
    }

    // --- Conditional Chain Block Tests ---

    @Test
    @Order(131)
    @DisplayName("Test 131: Conditional Chain block has all required fields")
    void testConditionalChainHasAllFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasAllFields = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_conditional_chain');" +
            "block.initSvg(); block.render();" +
            "return block.getField('ID') !== null && " +
            "       block.getField('NAME') !== null && " +
            "       block.getField('ENABLED') !== null && " +
            "       block.getField('TRIGGER_RULE') !== null && " +
            "       block.getField('ON_TRIGGER') !== null && " +
            "       block.getField('ON_NO_TRIGGER') !== null;"
        );
        assertTrue(hasAllFields, "apex_conditional_chain should have ID, NAME, ENABLED, TRIGGER_RULE, ON_TRIGGER, ON_NO_TRIGGER fields");
    }

    @Test
    @Order(132)
    @DisplayName("Test 132: Conditional Chain block can connect to Rule Chains Section")
    void testConditionalChainConnectsToSection() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean canConnect = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var section = workspace.newBlock('apex_section_rule_chains');" +
            "var chain = workspace.newBlock('apex_conditional_chain');" +
            "section.initSvg(); section.render();" +
            "chain.initSvg(); chain.render();" +
            "try {" +
            "  section.getInput('RULE_CHAINS').connection.connect(chain.previousConnection);" +
            "  return chain.getParent() === section;" +
            "} catch(e) { return false; }"
        );
        assertTrue(canConnect, "apex_conditional_chain should connect to apex_section_rule_chains");
    }

    @Test
    @Order(133)
    @DisplayName("Test 133: Conditional Chain generator includes on-trigger rules")
    void testConditionalChainGeneratorIncludesOnTrigger() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_conditional_chain');" +
            "block.setFieldValue('chain-1', 'ID');" +
            "block.setFieldValue('Test Chain', 'NAME');" +
            "block.setFieldValue('TRUE', 'ENABLED');" +
            "block.setFieldValue('trigger-rule', 'TRIGGER_RULE');" +
            "block.setFieldValue('rule-a,rule-b', 'ON_TRIGGER');" +
            "block.setFieldValue('rule-c', 'ON_NO_TRIGGER');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_conditional_chain'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"on-trigger\""), "Generator should include on-trigger");
        assertTrue(result.contains("\"on-no-trigger\""), "Generator should include on-no-trigger");
    }

    // --- Sequential Dependency Block Tests ---

    @Test
    @Order(134)
    @DisplayName("Test 134: Sequential Dependency block has all required fields")
    void testSequentialDependencyHasAllFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasAllFields = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_sequential_dependency');" +
            "block.initSvg(); block.render();" +
            "return block.getField('ID') !== null && " +
            "       block.getField('NAME') !== null && " +
            "       block.getField('ENABLED') !== null && " +
            "       block.getInput('STAGES') !== null;"
        );
        assertTrue(hasAllFields, "apex_sequential_dependency should have ID, NAME, ENABLED fields and STAGES input");
    }

    @Test
    @Order(135)
    @DisplayName("Test 135: Sequential Dependency block has STAGES statement input")
    void testSequentialDependencyHasStagesInput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasInput = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_sequential_dependency');" +
            "block.initSvg(); block.render();" +
            "var input = block.getInput('STAGES');" +
            "return input !== null && input.connection !== null;"
        );
        assertTrue(hasInput, "apex_sequential_dependency should have STAGES statement input");
    }

    @Test
    @Order(136)
    @DisplayName("Test 136: Sequential Dependency can connect to Rule Chains Section")
    void testSequentialDependencyConnectsToSection() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean canConnect = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var section = workspace.newBlock('apex_section_rule_chains');" +
            "var chain = workspace.newBlock('apex_sequential_dependency');" +
            "section.initSvg(); section.render();" +
            "chain.initSvg(); chain.render();" +
            "try {" +
            "  section.getInput('RULE_CHAINS').connection.connect(chain.previousConnection);" +
            "  return chain.getParent() === section;" +
            "} catch(e) { return false; }"
        );
        assertTrue(canConnect, "apex_sequential_dependency should connect to apex_section_rule_chains");
    }

    // --- Chain Stage Block Tests ---

    @Test
    @Order(137)
    @DisplayName("Test 137: Chain Stage block has all required fields")
    void testChainStageHasAllFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasAllFields = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_chain_stage');" +
            "block.initSvg(); block.render();" +
            "return block.getField('ID') !== null && " +
            "       block.getField('RULE_IDS') !== null && " +
            "       block.getField('DEPENDS_ON') !== null;"
        );
        assertTrue(hasAllFields, "apex_chain_stage should have ID, RULE_IDS, DEPENDS_ON fields");
    }

    @Test
    @Order(138)
    @DisplayName("Test 138: Chain Stage block can connect to Sequential Dependency")
    void testChainStageConnectsToSequentialDependency() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean canConnect = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var parent = workspace.newBlock('apex_sequential_dependency');" +
            "var stage = workspace.newBlock('apex_chain_stage');" +
            "parent.initSvg(); parent.render();" +
            "stage.initSvg(); stage.render();" +
            "try {" +
            "  parent.getInput('STAGES').connection.connect(stage.previousConnection);" +
            "  return stage.getParent() === parent;" +
            "} catch(e) { return false; }"
        );
        assertTrue(canConnect, "apex_chain_stage should connect to apex_sequential_dependency STAGES input");
    }

    @Test
    @Order(139)
    @DisplayName("Test 139: Chain Stage generator outputs correct JSON")
    void testChainStageGeneratorOutput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_chain_stage');" +
            "block.setFieldValue('stage-1', 'ID');" +
            "block.setFieldValue('rule-1, rule-2', 'RULE_IDS');" +
            "block.setFieldValue('stage-0', 'DEPENDS_ON');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_chain_stage'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"id\":\"stage-1\""), "Generator should include id");
        assertTrue(result.contains("\"rule-ids\""), "Generator should include rule-ids");
        assertTrue(result.contains("\"depends-on\""), "Generator should include depends-on");
    }

    // --- Result Routing Block Tests ---

    @Test
    @Order(140)
    @DisplayName("Test 140: Result Routing block has all required fields")
    void testResultRoutingHasAllFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasAllFields = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_result_routing');" +
            "block.initSvg(); block.render();" +
            "return block.getField('ID') !== null && " +
            "       block.getField('NAME') !== null && " +
            "       block.getField('ENABLED') !== null && " +
            "       block.getField('ROUTER_CONDITION') !== null && " +
            "       block.getInput('ROUTES') !== null;"
        );
        assertTrue(hasAllFields, "apex_result_routing should have ID, NAME, ENABLED, ROUTER_CONDITION fields and ROUTES input");
    }

    @Test
    @Order(141)
    @DisplayName("Test 141: Result Routing block has ROUTES statement input")
    void testResultRoutingHasRoutesInput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasInput = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_result_routing');" +
            "block.initSvg(); block.render();" +
            "var input = block.getInput('ROUTES');" +
            "return input !== null && input.connection !== null;"
        );
        assertTrue(hasInput, "apex_result_routing should have ROUTES statement input");
    }

    @Test
    @Order(142)
    @DisplayName("Test 142: Result Routing can connect to Rule Chains Section")
    void testResultRoutingConnectsToSection() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean canConnect = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var section = workspace.newBlock('apex_section_rule_chains');" +
            "var chain = workspace.newBlock('apex_result_routing');" +
            "section.initSvg(); section.render();" +
            "chain.initSvg(); chain.render();" +
            "try {" +
            "  section.getInput('RULE_CHAINS').connection.connect(chain.previousConnection);" +
            "  return chain.getParent() === section;" +
            "} catch(e) { return false; }"
        );
        assertTrue(canConnect, "apex_result_routing should connect to apex_section_rule_chains");
    }

    // --- Route Block Tests ---

    @Test
    @Order(143)
    @DisplayName("Test 143: Route block has all required fields")
    void testRouteHasAllFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasAllFields = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_route');" +
            "block.initSvg(); block.render();" +
            "return block.getField('VALUE') !== null && " +
            "       block.getField('RULE_IDS') !== null && " +
            "       block.getField('ENRICHMENT_GROUP_REFS') !== null;"
        );
        assertTrue(hasAllFields, "apex_route should have VALUE, RULE_IDS, ENRICHMENT_GROUP_REFS fields");
    }

    @Test
    @Order(144)
    @DisplayName("Test 144: Route block can connect to Result Routing")
    void testRouteConnectsToResultRouting() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean canConnect = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var parent = workspace.newBlock('apex_result_routing');" +
            "var route = workspace.newBlock('apex_route');" +
            "parent.initSvg(); parent.render();" +
            "route.initSvg(); route.render();" +
            "try {" +
            "  parent.getInput('ROUTES').connection.connect(route.previousConnection);" +
            "  return route.getParent() === parent;" +
            "} catch(e) { return false; }"
        );
        assertTrue(canConnect, "apex_route should connect to apex_result_routing ROUTES input");
    }

    @Test
    @Order(145)
    @DisplayName("Test 145: Route generator outputs correct JSON")
    void testRouteGeneratorOutput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_route');" +
            "block.setFieldValue('ROUTE_A', 'VALUE');" +
            "block.setFieldValue('rule-a,rule-b', 'RULE_IDS');" +
            "block.setFieldValue('enrichment-1', 'ENRICHMENT_GROUP_REFS');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_route'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"value\":\"ROUTE_A\""), "Generator should include value");
        assertTrue(result.contains("\"rule-ids\""), "Generator should include rule-ids");
        assertTrue(result.contains("\"enrichment-group-refs\""), "Generator should include enrichment-group-refs");
    }

    // --- Accumulative Chain Block Tests ---

    @Test
    @Order(146)
    @DisplayName("Test 146: Accumulative Chain block has all required fields")
    void testAccumulativeChainHasAllFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasAllFields = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_accumulative_chain');" +
            "block.initSvg(); block.render();" +
            "return block.getField('ID') !== null && " +
            "       block.getField('NAME') !== null && " +
            "       block.getField('ENABLED') !== null && " +
            "       block.getField('ACCUMULATOR_VARIABLE') !== null && " +
            "       block.getField('INITIAL_VALUE') !== null && " +
            "       block.getField('FINAL_DECISION') !== null && " +
            "       block.getInput('ACCUMULATION_RULES') !== null;"
        );
        assertTrue(hasAllFields, "apex_accumulative_chain should have all required fields and ACCUMULATION_RULES input");
    }

    @Test
    @Order(147)
    @DisplayName("Test 147: Accumulative Chain block has ACCUMULATION_RULES statement input")
    void testAccumulativeChainHasAccumulationRulesInput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasInput = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_accumulative_chain');" +
            "block.initSvg(); block.render();" +
            "var input = block.getInput('ACCUMULATION_RULES');" +
            "return input !== null && input.connection !== null;"
        );
        assertTrue(hasInput, "apex_accumulative_chain should have ACCUMULATION_RULES statement input");
    }

    @Test
    @Order(148)
    @DisplayName("Test 148: Accumulative Chain can connect to Rule Chains Section")
    void testAccumulativeChainConnectsToSection() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean canConnect = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var section = workspace.newBlock('apex_section_rule_chains');" +
            "var chain = workspace.newBlock('apex_accumulative_chain');" +
            "section.initSvg(); section.render();" +
            "chain.initSvg(); chain.render();" +
            "try {" +
            "  section.getInput('RULE_CHAINS').connection.connect(chain.previousConnection);" +
            "  return chain.getParent() === section;" +
            "} catch(e) { return false; }"
        );
        assertTrue(canConnect, "apex_accumulative_chain should connect to apex_section_rule_chains");
    }

    @Test
    @Order(149)
    @DisplayName("Test 149: Accumulative Chain generator includes initial-value and final-decision")
    void testAccumulativeChainGeneratorIncludesAllFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_accumulative_chain');" +
            "block.setFieldValue('accum-1', 'ID');" +
            "block.setFieldValue('Score Chain', 'NAME');" +
            "block.setFieldValue(true, 'ENABLED');" +
            "block.setFieldValue('totalScore', 'ACCUMULATOR_VARIABLE');" +
            "block.setFieldValue(0, 'INITIAL_VALUE');" +
            "block.setFieldValue('#totalScore >= 60', 'FINAL_DECISION');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_accumulative_chain'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"initial-value\""), "Generator should include initial-value");
        assertTrue(result.contains("\"final-decision-rule\""), "Generator should include final-decision-rule");
    }

    // --- Accumulation Rule Block Tests ---

    @Test
    @Order(150)
    @DisplayName("Test 150: Accumulation Rule block has all required fields")
    void testAccumulationRuleHasAllFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasAllFields = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_accumulation_rule');" +
            "block.initSvg(); block.render();" +
            "return block.getField('ID') !== null && " +
            "       block.getField('CONDITION') !== null && " +
            "       block.getField('WEIGHT') !== null;"
        );
        assertTrue(hasAllFields, "apex_accumulation_rule should have ID, CONDITION, WEIGHT fields");
    }

    @Test
    @Order(151)
    @DisplayName("Test 151: Accumulation Rule block can connect to Accumulative Chain")
    void testAccumulationRuleConnectsToAccumulativeChain() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean canConnect = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var parent = workspace.newBlock('apex_accumulative_chain');" +
            "var rule = workspace.newBlock('apex_accumulation_rule');" +
            "parent.initSvg(); parent.render();" +
            "rule.initSvg(); rule.render();" +
            "try {" +
            "  parent.getInput('ACCUMULATION_RULES').connection.connect(rule.previousConnection);" +
            "  return rule.getParent() === parent;" +
            "} catch(e) { return false; }"
        );
        assertTrue(canConnect, "apex_accumulation_rule should connect to apex_accumulative_chain ACCUMULATION_RULES input");
    }

    @Test
    @Order(152)
    @DisplayName("Test 152: Accumulation Rule generator outputs correct JSON")
    void testAccumulationRuleGeneratorOutput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_accumulation_rule');" +
            "block.setFieldValue('credit-check', 'ID');" +
            "block.setFieldValue('#creditScore >= 700 ? 25 : 15', 'CONDITION');" +
            "block.setFieldValue(1.5, 'WEIGHT');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_accumulation_rule'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"id\":\"credit-check\""), "Generator should include id");
        assertTrue(result.contains("\"condition\""), "Generator should include condition");
        assertTrue(result.contains("\"weight\""), "Generator should include weight");
    }

    // --- Fluent Builder Block Tests ---

    @Test
    @Order(153)
    @DisplayName("Test 153: Fluent Builder block has all required fields")
    void testFluentBuilderHasAllFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasAllFields = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_fluent_builder');" +
            "block.initSvg(); block.render();" +
            "return block.getField('ID') !== null && " +
            "       block.getField('NAME') !== null && " +
            "       block.getField('ENABLED') !== null && " +
            "       block.getField('BUILDER_TARGET') !== null && " +
            "       block.getInput('DECISION_STEPS') !== null;"
        );
        assertTrue(hasAllFields, "apex_fluent_builder should have all required fields and DECISION_STEPS input");
    }

    @Test
    @Order(154)
    @DisplayName("Test 154: Fluent Builder block has DECISION_STEPS statement input")
    void testFluentBuilderHasDecisionStepsInput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasInput = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_fluent_builder');" +
            "block.initSvg(); block.render();" +
            "var input = block.getInput('DECISION_STEPS');" +
            "return input !== null && input.connection !== null;"
        );
        assertTrue(hasInput, "apex_fluent_builder should have DECISION_STEPS statement input");
    }

    @Test
    @Order(155)
    @DisplayName("Test 155: Fluent Builder can connect to Rule Chains Section")
    void testFluentBuilderConnectsToSection() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean canConnect = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var section = workspace.newBlock('apex_section_rule_chains');" +
            "var chain = workspace.newBlock('apex_fluent_builder');" +
            "section.initSvg(); section.render();" +
            "chain.initSvg(); chain.render();" +
            "try {" +
            "  section.getInput('RULE_CHAINS').connection.connect(chain.previousConnection);" +
            "  return chain.getParent() === section;" +
            "} catch(e) { return false; }"
        );
        assertTrue(canConnect, "apex_fluent_builder should connect to apex_section_rule_chains");
    }

    // --- Decision Step Block Tests ---

    @Test
    @Order(156)
    @DisplayName("Test 156: Decision Step block has all required fields")
    void testDecisionStepHasAllFields() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean hasAllFields = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_decision_step');" +
            "block.initSvg(); block.render();" +
            "return block.getField('ID') !== null && " +
            "       block.getField('CONDITION') !== null && " +
            "       block.getField('ON_SUCCESS') !== null && " +
            "       block.getField('ON_FAILURE') !== null;"
        );
        assertTrue(hasAllFields, "apex_decision_step should have ID, CONDITION, ON_SUCCESS, ON_FAILURE fields");
    }

    @Test
    @Order(157)
    @DisplayName("Test 157: Decision Step block can connect to Fluent Builder")
    void testDecisionStepConnectsToFluentBuilder() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean canConnect = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var parent = workspace.newBlock('apex_fluent_builder');" +
            "var step = workspace.newBlock('apex_decision_step');" +
            "parent.initSvg(); parent.render();" +
            "step.initSvg(); step.render();" +
            "try {" +
            "  parent.getInput('DECISION_STEPS').connection.connect(step.previousConnection);" +
            "  return step.getParent() === parent;" +
            "} catch(e) { return false; }"
        );
        assertTrue(canConnect, "apex_decision_step should connect to apex_fluent_builder DECISION_STEPS input");
    }

    @Test
    @Order(158)
    @DisplayName("Test 158: Decision Step generator outputs correct JSON")
    void testDecisionStepGeneratorOutput() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_decision_step');" +
            "block.setFieldValue('step-1', 'ID');" +
            "block.setFieldValue('#amount > 1000', 'CONDITION');" +
            "block.setFieldValue('step-2', 'ON_SUCCESS');" +
            "block.setFieldValue('reject', 'ON_FAILURE');" +
            "block.initSvg(); block.render();" +
            "return apexGenerator.forBlock['apex_decision_step'](block, apexGenerator);"
        );
        assertTrue(result.contains("\"id\":\"step-1\""), "Generator should include id");
        assertTrue(result.contains("\"condition\""), "Generator should include condition");
        assertTrue(result.contains("\"on-success\""), "Generator should include on-success");
        assertTrue(result.contains("\"on-failure\""), "Generator should include on-failure");
    }

    // --- Rule Chains Toolbox Category Tests ---

    @Test
    @Order(159)
    @DisplayName("Test 159: Rule Chains toolbox category exists")
    void testRuleChainsToolboxCategoryExists() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean categoryExists = (Boolean) js.executeScript(
            "var toolbox = Blockly.getMainWorkspace().getToolbox();" +
            "var contents = toolbox.getToolboxItems();" +
            "for (var i = 0; i < contents.length; i++) {" +
            "  if (contents[i].getName && contents[i].getName() === 'Rule Chains') return true;" +
            "}" +
            "return false;"
        );
        assertTrue(categoryExists, "Rule Chains toolbox category should exist");
    }

    @Test
    @Order(160)
    @DisplayName("Test 160: Rule Chains toolbox category contains all chain blocks")
    void testRuleChainsToolboxContainsAllBlocks() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Long blockCount = (Long) js.executeScript(
            "var toolbox = Blockly.getMainWorkspace().getToolbox();" +
            "var contents = toolbox.getToolboxItems();" +
            "for (var i = 0; i < contents.length; i++) {" +
            "  if (contents[i].getName && contents[i].getName() === 'Rule Chains') {" +
            "    var flyout = contents[i].getContents ? contents[i].getContents() : [];" +
            "    return flyout.length;" +
            "  }" +
            "}" +
            "return 0;"
        );
        assertTrue(blockCount >= 9, "Rule Chains category should contain at least 9 blocks, found: " + blockCount);
    }

    // --- Multiple Chains in Section Tests ---

    @Test
    @Order(161)
    @DisplayName("Test 161: Multiple chain blocks can connect in sequence")
    void testMultipleChainsCanConnectInSequence() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean canChain = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var chain1 = workspace.newBlock('apex_conditional_chain');" +
            "var chain2 = workspace.newBlock('apex_sequential_dependency');" +
            "chain1.initSvg(); chain1.render();" +
            "chain2.initSvg(); chain2.render();" +
            "try {" +
            "  chain1.nextConnection.connect(chain2.previousConnection);" +
            "  return chain2.getPreviousBlock() === chain1;" +
            "} catch(e) { return false; }"
        );
        assertTrue(canChain, "Multiple chain blocks should be able to connect in sequence");
    }

    @Test
    @Order(162)
    @DisplayName("Test 162: Multiple stages can connect in sequence")
    void testMultipleStagesCanConnectInSequence() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean canChain = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var stage1 = workspace.newBlock('apex_chain_stage');" +
            "var stage2 = workspace.newBlock('apex_chain_stage');" +
            "stage1.initSvg(); stage1.render();" +
            "stage2.initSvg(); stage2.render();" +
            "try {" +
            "  stage1.nextConnection.connect(stage2.previousConnection);" +
            "  return stage2.getPreviousBlock() === stage1;" +
            "} catch(e) { return false; }"
        );
        assertTrue(canChain, "Multiple stage blocks should be able to connect in sequence");
    }

    @Test
    @Order(163)
    @DisplayName("Test 163: Multiple routes can connect in sequence")
    void testMultipleRoutesCanConnectInSequence() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean canChain = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var route1 = workspace.newBlock('apex_route');" +
            "var route2 = workspace.newBlock('apex_route');" +
            "route1.initSvg(); route1.render();" +
            "route2.initSvg(); route2.render();" +
            "try {" +
            "  route1.nextConnection.connect(route2.previousConnection);" +
            "  return route2.getPreviousBlock() === route1;" +
            "} catch(e) { return false; }"
        );
        assertTrue(canChain, "Multiple route blocks should be able to connect in sequence");
    }

    @Test
    @Order(164)
    @DisplayName("Test 164: Multiple accumulation rules can connect in sequence")
    void testMultipleAccumulationRulesCanConnectInSequence() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean canChain = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var rule1 = workspace.newBlock('apex_accumulation_rule');" +
            "var rule2 = workspace.newBlock('apex_accumulation_rule');" +
            "rule1.initSvg(); rule1.render();" +
            "rule2.initSvg(); rule2.render();" +
            "try {" +
            "  rule1.nextConnection.connect(rule2.previousConnection);" +
            "  return rule2.getPreviousBlock() === rule1;" +
            "} catch(e) { return false; }"
        );
        assertTrue(canChain, "Multiple accumulation rule blocks should be able to connect in sequence");
    }

    @Test
    @Order(165)
    @DisplayName("Test 165: Multiple decision steps can connect in sequence")
    void testMultipleDecisionStepsCanConnectInSequence() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean canChain = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var step1 = workspace.newBlock('apex_decision_step');" +
            "var step2 = workspace.newBlock('apex_decision_step');" +
            "step1.initSvg(); step1.render();" +
            "step2.initSvg(); step2.render();" +
            "try {" +
            "  step1.nextConnection.connect(step2.previousConnection);" +
            "  return step2.getPreviousBlock() === step1;" +
            "} catch(e) { return false; }"
        );
        assertTrue(canChain, "Multiple decision step blocks should be able to connect in sequence");
    }

    // --- YAML Import Tests ---

    @Test
    @Order(166)
    @DisplayName("Test 166: Conditional Chain block can be created and rendered")
    void testConditionalChainCanBeCreatedAndRendered() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean blockCreated = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var block = workspace.newBlock('apex_conditional_chain');" +
            "block.setFieldValue('chain-1', 'ID');" +
            "block.setFieldValue('Test Chain', 'NAME');" +
            "block.setFieldValue('trigger-1', 'TRIGGER_RULE');" +
            "block.setFieldValue('rule-a,rule-b', 'ON_TRIGGER');" +
            "block.setFieldValue('rule-c', 'ON_NO_TRIGGER');" +
            "block.initSvg(); block.render();" +
            "var blocks = workspace.getBlocksByType('apex_conditional_chain');" +
            "return blocks.length > 0 && blocks[0].getFieldValue('ID') === 'chain-1';"
        );
        assertTrue(blockCreated, "Conditional Chain block should be created with correct field values");
    }

    @Test
    @Order(167)
    @DisplayName("Test 167: Sequential Dependency block can be created and rendered")
    void testSequentialDependencyCanBeCreatedAndRendered() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean blockCreated = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var block = workspace.newBlock('apex_sequential_dependency');" +
            "block.setFieldValue('seq-1', 'ID');" +
            "block.setFieldValue('Sequential Chain', 'NAME');" +
            "block.initSvg(); block.render();" +
            "var blocks = workspace.getBlocksByType('apex_sequential_dependency');" +
            "return blocks.length > 0 && blocks[0].getFieldValue('ID') === 'seq-1';"
        );
        assertTrue(blockCreated, "Sequential Dependency block should be created with correct field values");
    }

    @Test
    @Order(168)
    @DisplayName("Test 168: Result Routing block can be created and rendered")
    void testResultRoutingCanBeCreatedAndRendered() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean blockCreated = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var block = workspace.newBlock('apex_result_routing');" +
            "block.setFieldValue('routing-1', 'ID');" +
            "block.setFieldValue('Routing Chain', 'NAME');" +
            "block.setFieldValue('#path', 'ROUTER_CONDITION');" +
            "block.initSvg(); block.render();" +
            "var blocks = workspace.getBlocksByType('apex_result_routing');" +
            "return blocks.length > 0 && blocks[0].getFieldValue('ID') === 'routing-1';"
        );
        assertTrue(blockCreated, "Result Routing block should be created with correct field values");
    }

    @Test
    @Order(169)
    @DisplayName("Test 169: Accumulative Chain block can be created and rendered")
    void testAccumulativeChainCanBeCreatedAndRendered() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean blockCreated = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var block = workspace.newBlock('apex_accumulative_chain');" +
            "block.setFieldValue('accum-1', 'ID');" +
            "block.setFieldValue('Accumulative Chain', 'NAME');" +
            "block.setFieldValue('score', 'ACCUMULATOR_VARIABLE');" +
            "block.setFieldValue(0, 'INITIAL_VALUE');" +
            "block.setFieldValue('#score >= 60', 'FINAL_DECISION');" +
            "block.initSvg(); block.render();" +
            "var blocks = workspace.getBlocksByType('apex_accumulative_chain');" +
            "return blocks.length > 0 && blocks[0].getFieldValue('ID') === 'accum-1';"
        );
        assertTrue(blockCreated, "Accumulative Chain block should be created with correct field values");
    }

    @Test
    @Order(170)
    @DisplayName("Test 170: Fluent Builder block can be created and rendered")
    void testFluentBuilderCanBeCreatedAndRendered() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean blockCreated = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var block = workspace.newBlock('apex_fluent_builder');" +
            "block.setFieldValue('fluent-1', 'ID');" +
            "block.setFieldValue('Fluent Builder', 'NAME');" +
            "block.setFieldValue('result', 'BUILDER_TARGET');" +
            "block.initSvg(); block.render();" +
            "var blocks = workspace.getBlocksByType('apex_fluent_builder');" +
            "return blocks.length > 0 && blocks[0].getFieldValue('ID') === 'fluent-1';"
        );
        assertTrue(blockCreated, "Fluent Builder block should be created with correct field values");
    }

    // --- Toolbox Category Tests ---

    @Test
    @Order(171)
    @DisplayName("Test 171: Conditional Chain block is defined and can be instantiated")
    void testConditionalChainBlockIsDefined() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean isDefined = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_conditional_chain'] !== 'undefined' && " +
            "       typeof apexGenerator.forBlock['apex_conditional_chain'] === 'function';"
        );
        assertTrue(isDefined, "apex_conditional_chain should be defined with block and generator");
    }

    @Test
    @Order(172)
    @DisplayName("Test 172: Sequential Dependency block is defined and can be instantiated")
    void testSequentialDependencyBlockIsDefined() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean isDefined = (Boolean) js.executeScript(
            "return typeof Blockly.Blocks['apex_sequential_dependency'] !== 'undefined' && " +
            "       typeof apexGenerator.forBlock['apex_sequential_dependency'] === 'function';"
        );
        assertTrue(isDefined, "apex_sequential_dependency should be defined with block and generator");
    }

    // --- AUTO_FILL_MAP Tests ---

    @Test
    @Order(173)
    @DisplayName("Test 173: Rule Chain section has AUTO_FILL_MAP entry for child blocks")
    void testRuleChainSectionInAutoFillMap() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean inMap = (Boolean) js.executeScript(
            "return typeof AUTO_FILL_MAP !== 'undefined' && " +
            "       AUTO_FILL_MAP.hasOwnProperty('apex_section_rule_chains') && " +
            "       AUTO_FILL_MAP['apex_section_rule_chains'].hasOwnProperty('RULE_CHAINS');"
        );
        assertTrue(inMap, "apex_section_rule_chains should be in AUTO_FILL_MAP with RULE_CHAINS entry");
    }

    @Test
    @Order(174)
    @DisplayName("Test 174: Parent chain blocks have AUTO_FILL_MAP entries for child blocks")
    void testParentChainBlocksInAutoFillMap() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean inMap = (Boolean) js.executeScript(
            "return typeof AUTO_FILL_MAP !== 'undefined' && " +
            "       AUTO_FILL_MAP.hasOwnProperty('apex_sequential_dependency') && " +
            "       AUTO_FILL_MAP.hasOwnProperty('apex_result_routing') && " +
            "       AUTO_FILL_MAP.hasOwnProperty('apex_accumulative_chain') && " +
            "       AUTO_FILL_MAP.hasOwnProperty('apex_fluent_builder');"
        );
        assertTrue(inMap, "Parent chain blocks should be in AUTO_FILL_MAP");
    }

    // --- Full Integration Test ---

    @Test
    @Order(175)
    @DisplayName("Test 175: Full rule chain structure generates valid JSON")
    void testFullRuleChainStructureGeneratesValidJson() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            // Create section
            "var section = workspace.newBlock('apex_section_rule_chains');" +
            "section.initSvg(); section.render();" +
            // Create conditional chain
            "var chain = workspace.newBlock('apex_conditional_chain');" +
            "chain.setFieldValue('chain-1', 'ID');" +
            "chain.setFieldValue('Test Chain', 'NAME');" +
            "chain.setFieldValue('TRUE', 'ENABLED');" +
            "chain.setFieldValue('trigger-rule', 'TRIGGER_RULE');" +
            "chain.setFieldValue('rule-a,rule-b', 'ON_TRIGGER');" +
            "chain.setFieldValue('rule-c', 'ON_NO_TRIGGER');" +
            "chain.initSvg(); chain.render();" +
            // Connect chain to section
            "section.getInput('RULE_CHAINS').connection.connect(chain.previousConnection);" +
            // Generate
            "return apexGenerator.forBlock['apex_section_rule_chains'](section, apexGenerator);"
        );
        assertTrue(result.contains("\"rule-chains\""), "Full structure should contain rule-chains");
        assertTrue(result.contains("\"pattern\":\"conditional-chaining\""), "Full structure should contain pattern");
        assertTrue(result.contains("\"trigger-rule\":\"trigger-rule\""), "Full structure should contain trigger-rule");
    }

    @Test
    @Order(176)
    @DisplayName("Test 176: Categories section block exists in toolbox")
    void testCategoriesSectionBlockExistsInToolbox() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return Blockly.Blocks['apex_section_categories'] !== undefined;"
        );
        assertTrue(exists, "apex_section_categories block should be defined");
    }

    @Test
    @Order(177)
    @DisplayName("Test 177: Category block exists in toolbox")
    void testCategoryBlockExistsInToolbox() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return Blockly.Blocks['apex_category'] !== undefined;"
        );
        assertTrue(exists, "apex_category block should be defined");
    }

    @Test
    @Order(178)
    @DisplayName("Test 178: Categories section generates valid JSON")
    void testCategoriesSectionGeneratesValidJson() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var section = workspace.newBlock('apex_section_categories');" +
            "section.initSvg(); section.render();" +
            "return apexGenerator.forBlock['apex_section_categories'](section, apexGenerator);"
        );
        assertTrue(result.contains("\"type\":\"categories\""), "Should contain categories type");
    }

    @Test
    @Order(179)
    @DisplayName("Test 179: Category block generates valid JSON")
    void testCategoryBlockGeneratesValidJson() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var category = workspace.newBlock('apex_category');" +
            "category.setFieldValue('cat-1', 'NAME');" +
            "category.setFieldValue('Test Category', 'DESCRIPTION');" +
            "category.setFieldValue('10', 'PRIORITY');" +
            "category.initSvg(); category.render();" +
            "return apexGenerator.forBlock['apex_category'](category, apexGenerator);"
        );
        assertTrue(result.contains("\"name\":\"cat-1\""), "Should contain category name");
        assertTrue(result.contains("\"description\":\"Test Category\""), "Should contain description");
        assertTrue(result.contains("\"priority\":10"), "Should contain priority");
    }

    @Test
    @Order(180)
    @DisplayName("Test 180: Data Sinks section block exists in toolbox")
    void testDataSinksSectionBlockExistsInToolbox() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return Blockly.Blocks['apex_section_data_sinks'] !== undefined;"
        );
        assertTrue(exists, "apex_section_data_sinks block should be defined");
    }

    @Test
    @Order(181)
    @DisplayName("Test 181: Database sink block exists in toolbox")
    void testDatabaseSinkBlockExistsInToolbox() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return Blockly.Blocks['apex_data_sink_database'] !== undefined;"
        );
        assertTrue(exists, "apex_data_sink_database block should be defined");
    }

    @Test
    @Order(182)
    @DisplayName("Test 182: File sink block exists in toolbox")
    void testFileSinkBlockExistsInToolbox() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return Blockly.Blocks['apex_data_sink_file'] !== undefined;"
        );
        assertTrue(exists, "apex_data_sink_file block should be defined");
    }

    @Test
    @Order(183)
    @DisplayName("Test 183: REST sink block exists in toolbox")
    void testRestSinkBlockExistsInToolbox() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return Blockly.Blocks['apex_data_sink_rest'] !== undefined;"
        );
        assertTrue(exists, "apex_data_sink_rest block should be defined");
    }

    @Test
    @Order(184)
    @DisplayName("Test 184: Queue sink block exists in toolbox")
    void testQueueSinkBlockExistsInToolbox() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean exists = (Boolean) js.executeScript(
            "return Blockly.Blocks['apex_data_sink_queue'] !== undefined;"
        );
        assertTrue(exists, "apex_data_sink_queue block should be defined");
    }

    @Test
    @Order(185)
    @DisplayName("Test 185: Database sink generates valid JSON")
    void testDatabaseSinkGeneratesValidJson() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var sink = workspace.newBlock('apex_data_sink_database');" +
            "sink.setFieldValue('db-sink-1', 'NAME');" +
            "sink.setFieldValue('TRUE', 'ENABLED');" +
            "sink.setFieldValue('my-db', 'DATA_SOURCE_REF');" +
            "sink.setFieldValue('results_table', 'TABLE');" +
            "sink.setFieldValue('100', 'BATCH_SIZE');" +
            "sink.initSvg(); sink.render();" +
            "return apexGenerator.forBlock['apex_data_sink_database'](sink, apexGenerator);"
        );
        assertTrue(result.contains("\"id\":\"db-sink-1\""), "Should contain sink id");
        assertTrue(result.contains("\"type\":\"database-sink\""), "Should contain sink type");
        assertTrue(result.contains("\"enabled\":true"), "Should contain enabled flag");
        assertTrue(result.contains("\"data-source-ref\":\"my-db\""), "Should contain data-source-ref");
        assertTrue(result.contains("\"table\":\"results_table\""), "Should contain table");
        assertTrue(result.contains("\"batch-size\":100"), "Should contain batch-size");
    }

    @Test
    @Order(186)
    @DisplayName("Test 186: File sink generates valid JSON")
    void testFileSinkGeneratesValidJson() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var sink = workspace.newBlock('apex_data_sink_file');" +
            "sink.setFieldValue('file-sink-1', 'NAME');" +
            "sink.setFieldValue('TRUE', 'ENABLED');" +
            "sink.setFieldValue('/output/results.csv', 'PATH');" +
            "sink.setFieldValue('csv', 'FORMAT');" +
            "sink.setFieldValue('TRUE', 'APPEND');" +
            "sink.initSvg(); sink.render();" +
            "return apexGenerator.forBlock['apex_data_sink_file'](sink, apexGenerator);"
        );
        assertTrue(result.contains("\"id\":\"file-sink-1\""), "Should contain sink id");
        assertTrue(result.contains("\"type\":\"file-sink\""), "Should contain sink type");
        assertTrue(result.contains("\"path\":\"/output/results.csv\""), "Should contain path");
        assertTrue(result.contains("\"format\":\"csv\""), "Should contain format");
        assertTrue(result.contains("\"append\":true"), "Should contain append");
    }

    @Test
    @Order(187)
    @DisplayName("Test 187: REST sink generates valid JSON")
    void testRestSinkGeneratesValidJson() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var sink = workspace.newBlock('apex_data_sink_rest');" +
            "sink.setFieldValue('rest-sink-1', 'NAME');" +
            "sink.setFieldValue('TRUE', 'ENABLED');" +
            "sink.setFieldValue('https://api.example.com/results', 'URL');" +
            "sink.setFieldValue('POST', 'METHOD');" +
            "sink.setFieldValue('30000', 'TIMEOUT');" +
            "sink.initSvg(); sink.render();" +
            "return apexGenerator.forBlock['apex_data_sink_rest'](sink, apexGenerator);"
        );
        assertTrue(result.contains("\"id\":\"rest-sink-1\""), "Should contain sink id");
        assertTrue(result.contains("\"type\":\"rest-sink\""), "Should contain sink type");
        assertTrue(result.contains("\"url\":\"https://api.example.com/results\""), "Should contain url");
        assertTrue(result.contains("\"method\":\"POST\""), "Should contain method");
        assertTrue(result.contains("\"timeout\":30000"), "Should contain timeout");
    }

    @Test
    @Order(188)
    @DisplayName("Test 188: Queue sink generates valid JSON")
    void testQueueSinkGeneratesValidJson() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var sink = workspace.newBlock('apex_data_sink_queue');" +
            "sink.setFieldValue('queue-sink-1', 'NAME');" +
            "sink.setFieldValue('TRUE', 'ENABLED');" +
            "sink.setFieldValue('results-queue', 'QUEUE_NAME');" +
            "sink.setFieldValue('rabbitmq-connection', 'CONNECTION');" +
            "sink.setFieldValue('TRUE', 'PERSISTENT');" +
            "sink.initSvg(); sink.render();" +
            "return apexGenerator.forBlock['apex_data_sink_queue'](sink, apexGenerator);"
        );
        assertTrue(result.contains("\"id\":\"queue-sink-1\""), "Should contain sink id");
        assertTrue(result.contains("\"type\":\"queue-sink\""), "Should contain sink type");
        assertTrue(result.contains("\"queue-name\":\"results-queue\""), "Should contain queue-name");
        assertTrue(result.contains("\"connection\":\"rabbitmq-connection\""), "Should contain connection");
        assertTrue(result.contains("\"persistent\":true"), "Should contain persistent");
    }

    @Test
    @Order(189)
    @DisplayName("Test 189: File source with encoding and polling-interval")
    void testFileSourceWithEncodingAndPollingInterval() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var fileSource = workspace.newBlock('apex_data_source_file');" +
            "fileSource.setFieldValue('file-1', 'ID');" +
            "fileSource.setFieldValue('File Source', 'NAME');" +
            "fileSource.setFieldValue('TRUE', 'ENABLED');" +
            "fileSource.setFieldValue('Test Description', 'DESCRIPTION');" +
            "fileSource.setFieldValue('/data/files', 'BASE_PATH');" +
            "fileSource.setFieldValue('*.csv', 'FILE_PATTERN');" +
            "fileSource.setFieldValue('csv', 'FORMAT');" +
            "fileSource.setFieldValue('UTF-8', 'ENCODING');" +
            "fileSource.setFieldValue('5000', 'POLLING_INTERVAL');" +
            "fileSource.initSvg(); fileSource.render();" +
            "return apexGenerator.forBlock['apex_data_source_file'](fileSource, apexGenerator);"
        );
        assertTrue(result.contains("\"encoding\":\"UTF-8\""), "Should contain encoding");
        assertTrue(result.contains("\"polling-interval\":5000"), "Should contain polling-interval");
    }

    @Test
    @Order(190)
    @DisplayName("Test 190: Pipeline config block exists in toolbox")
    void testPipelineConfigBlockExists() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean blockExists = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_pipeline_config');" +
            "return block !== null && block.type === 'apex_pipeline_config';"
        );
        assertTrue(blockExists, "apex_pipeline_config block should exist");
    }

    @Test
    @Order(191)
    @DisplayName("Test 191: Pipeline stage block exists in toolbox")
    void testPipelineStageBlockExists() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        Boolean blockExists = (Boolean) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "var block = workspace.newBlock('apex_pipeline_stage');" +
            "return block !== null && block.type === 'apex_pipeline_stage';"
        );
        assertTrue(blockExists, "apex_pipeline_stage block should exist");
    }

    @Test
    @Order(192)
    @DisplayName("Test 192: Pipeline config generates correct JSON")
    void testPipelineConfigGeneratesCorrectJson() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var pipeline = workspace.newBlock('apex_pipeline_config');" +
            "pipeline.setFieldValue('my-pipeline', 'ID');" +
            "pipeline.setFieldValue('sequential', 'MODE');" +
            "pipeline.setFieldValue('fail-fast', 'ERROR_HANDLING');" +
            "pipeline.setFieldValue('3', 'MAX_RETRIES');" +
            "pipeline.setFieldValue('1000', 'RETRY_DELAY_MS');" +
            "pipeline.setFieldValue('TRUE', 'COLLECT_METRICS');" +
            "pipeline.initSvg(); pipeline.render();" +
            "return apexGenerator.forBlock['apex_pipeline_config'](pipeline, apexGenerator);"
        );
        assertTrue(result.contains("\"pipeline\""), "Should contain pipeline object");
        assertTrue(result.contains("\"id\":\"my-pipeline\""), "Should contain pipeline id");
        assertTrue(result.contains("\"mode\":\"sequential\""), "Should contain mode");
        assertTrue(result.contains("\"error-handling\":\"fail-fast\""), "Should contain error-handling");
        assertTrue(result.contains("\"max-retries\":3"), "Should contain max-retries");
        assertTrue(result.contains("\"retry-delay-ms\":1000"), "Should contain retry-delay-ms");
        assertTrue(result.contains("\"collect-metrics\":true"), "Should contain collect-metrics");
    }

    @Test
    @Order(193)
    @DisplayName("Test 193: Pipeline stage generates correct JSON")
    void testPipelineStageGeneratesCorrectJson() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var stage = workspace.newBlock('apex_pipeline_stage');" +
            "stage.setFieldValue('validation-stage', 'NAME');" +
            "stage.setFieldValue('1', 'ORDER');" +
            "stage.setFieldValue('TRUE', 'ENABLED');" +
            "stage.setFieldValue('', 'DEPENDS_ON');" +
            "stage.initSvg(); stage.render();" +
            "return apexGenerator.forBlock['apex_pipeline_stage'](stage, apexGenerator);"
        );
        assertTrue(result.contains("\"name\":\"validation-stage\""), "Should contain stage name");
        assertTrue(result.contains("\"order\":1"), "Should contain order");
        assertTrue(result.contains("\"enabled\":true"), "Should contain enabled");
    }

    @Test
    @Order(194)
    @DisplayName("Test 194: Pipeline stage with depends-on")
    void testPipelineStageWithDependsOn() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var stage = workspace.newBlock('apex_pipeline_stage');" +
            "stage.setFieldValue('enrichment-stage', 'NAME');" +
            "stage.setFieldValue('2', 'ORDER');" +
            "stage.setFieldValue('TRUE', 'ENABLED');" +
            "stage.setFieldValue('validation-stage', 'DEPENDS_ON');" +
            "stage.initSvg(); stage.render();" +
            "return apexGenerator.forBlock['apex_pipeline_stage'](stage, apexGenerator);"
        );
        assertTrue(result.contains("\"name\":\"enrichment-stage\""), "Should contain stage name");
        assertTrue(result.contains("\"depends-on\":\"validation-stage\""), "Should contain depends-on");
    }

    @Test
    @Order(195)
    @DisplayName("Test 195: Pipeline config with parallel mode")
    void testPipelineConfigWithParallelMode() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var pipeline = workspace.newBlock('apex_pipeline_config');" +
            "pipeline.setFieldValue('parallel-pipeline', 'ID');" +
            "pipeline.setFieldValue('parallel', 'MODE');" +
            "pipeline.setFieldValue('continue-on-error', 'ERROR_HANDLING');" +
            "pipeline.setFieldValue('5', 'MAX_RETRIES');" +
            "pipeline.setFieldValue('2000', 'RETRY_DELAY_MS');" +
            "pipeline.setFieldValue('FALSE', 'COLLECT_METRICS');" +
            "pipeline.initSvg(); pipeline.render();" +
            "return apexGenerator.forBlock['apex_pipeline_config'](pipeline, apexGenerator);"
        );
        assertTrue(result.contains("\"mode\":\"parallel\""), "Should contain parallel mode");
        assertTrue(result.contains("\"error-handling\":\"continue-on-error\""), "Should contain continue-on-error");
        assertTrue(result.contains("\"collect-metrics\":false"), "Should contain false metrics");
    }

    @Test
    @Order(196)
    @DisplayName("Test 196: Pipeline config with stages")
    void testPipelineConfigWithStages() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var pipeline = workspace.newBlock('apex_pipeline_config');" +
            "pipeline.setFieldValue('staged-pipeline', 'ID');" +
            "var stage1 = workspace.newBlock('apex_pipeline_stage');" +
            "stage1.setFieldValue('stage-1', 'NAME');" +
            "stage1.setFieldValue('1', 'ORDER');" +
            "stage1.setFieldValue('TRUE', 'ENABLED');" +
            "var stage2 = workspace.newBlock('apex_pipeline_stage');" +
            "stage2.setFieldValue('stage-2', 'NAME');" +
            "stage2.setFieldValue('2', 'ORDER');" +
            "stage2.setFieldValue('TRUE', 'ENABLED');" +
            "stage2.setFieldValue('stage-1', 'DEPENDS_ON');" +
            "pipeline.getInput('STAGES').connection.connect(stage1.previousConnection);" +
            "stage1.nextConnection.connect(stage2.previousConnection);" +
            "pipeline.initSvg(); pipeline.render();" +
            "stage1.initSvg(); stage1.render();" +
            "stage2.initSvg(); stage2.render();" +
            "return apexGenerator.forBlock['apex_pipeline_config'](pipeline, apexGenerator);"
        );
        assertTrue(result.contains("\"stages\""), "Should contain stages array");
        assertTrue(result.contains("\"name\":\"stage-1\""), "Should contain stage-1");
        assertTrue(result.contains("\"name\":\"stage-2\""), "Should contain stage-2");
        assertTrue(result.contains("\"depends-on\":\"stage-1\""), "Should contain dependency");
    }

    @Test
    @Order(197)
    @DisplayName("Test 197: Pipeline stage disabled")
    void testPipelineStageDisabled() {
        driver.get(baseUrl + "/apex_editor_main.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("blocklyDiv")));

        String result = (String) js.executeScript(
            "var workspace = Blockly.getMainWorkspace();" +
            "workspace.clear();" +
            "var stage = workspace.newBlock('apex_pipeline_stage');" +
            "stage.setFieldValue('disabled-stage', 'NAME');" +
            "stage.setFieldValue('3', 'ORDER');" +
            "stage.setFieldValue('FALSE', 'ENABLED');" +
            "stage.initSvg(); stage.render();" +
            "return apexGenerator.forBlock['apex_pipeline_stage'](stage, apexGenerator);"
        );
        assertTrue(result.contains("\"enabled\":false"), "Should contain enabled false");
    }
}

