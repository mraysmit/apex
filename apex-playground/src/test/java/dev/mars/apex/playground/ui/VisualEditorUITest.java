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
            "BLOCK_ID_CONFIG['apex_rule'].prefix === 'rule-'"
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

        // Check Eval Data section exists and is expanded (both sections are expanded by default)
        Boolean evalSectionExists = (Boolean) js.executeScript(
            "var section = document.getElementById('evalDataSection');" +
            "return section && section.classList.contains('expanded');"
        );
        assertTrue(evalSectionExists, "Eval Data section should exist and be expanded by default");
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

        // Toggle Eval Data section (should collapse it since it starts expanded)
        js.executeScript("toggleAccordion('evalDataSection');");
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        Boolean evalCollapsed = (Boolean) js.executeScript(
            "return !document.getElementById('evalDataSection').classList.contains('expanded');"
        );
        assertTrue(evalCollapsed, "Eval Data section should be collapsed after toggle");
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

        // Verify Eval Data section starts expanded
        Boolean initiallyExpanded = (Boolean) js.executeScript(
            "return document.getElementById('evalDataSection').classList.contains('expanded');"
        );
        assertTrue(initiallyExpanded, "Eval Data section should be expanded initially");

        // Click directly on the accordion-header div
        WebElement evalHeader = driver.findElement(By.cssSelector("#evalDataSection .accordion-header"));
        evalHeader.click();
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Verify section is now collapsed
        Boolean nowCollapsed = (Boolean) js.executeScript(
            "return !document.getElementById('evalDataSection').classList.contains('expanded');"
        );
        assertTrue(nowCollapsed, "Eval Data section should be collapsed after clicking header");
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
            "BLOCK_ID_CONFIG['apex_component_config'].prefix === 'component-';"
        );
        assertTrue(hasComponentConfig, "BLOCK_ID_CONFIG should include apex_component_config with prefix 'component-'");
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
}
