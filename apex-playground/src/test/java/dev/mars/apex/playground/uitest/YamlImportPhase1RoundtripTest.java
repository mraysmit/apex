package dev.mars.apex.playground.uitest;

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

import dev.mars.apex.playground.ui.BaseYamlImportSeleniumTest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 Roundtrip Integrity Tests.
 *
 * <p>Validates the fixes for 3 categories of data-loss bugs:</p>
 * <ol>
 *   <li><b>Generator roundtrip</b> — rule-chains, categories, and data-sinks sections
 *       were silently dropped by the {@code apex_rule_config} generator.
 *       The section-level generators ({@code apex_section_rule_chains}, etc.) emitted
 *       correct JSON, but the parent generator never collected them.</li>
 *   <li><b>Rule-config metadata</b> — {@code created-by}, {@code created-date},
 *       {@code last-modified} fields were not set during YAML import into blocks.</li>
 *   <li><b>Data-source-config metadata</b> — same 3 fields missing on the
 *       data-source config block import.</li>
 * </ol>
 *
 * <p>Each test follows the pattern: load YAML → import to Blockly → verify blocks
 * → export YAML → verify structure preserved.</p>
 *
 * @author APEX Test Suite
 * @since 2025-12-19
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class YamlImportPhase1RoundtripTest extends BaseYamlImportSeleniumTest {

    // ========================================================================
    // RULE-CHAINS ROUNDTRIP (Generator Fix #8)
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("Phase1-RT-1: Import YAML with rule-chains creates chain blocks")
    void testRuleChainsImportCreatesBlocks() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/rule-chains-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
        // The YAML declares 2 rule-chains, so we expect 2 chain blocks
        // plus the section block that wraps them
        int blockCount = getBlockCount();
        assertTrue(blockCount >= 4,
                "Should have at least 4 blocks (config + section + 2 chains), found: " + blockCount);
    }

    @Test
    @Order(2)
    @DisplayName("Phase1-RT-2: Rule-chains survive export (roundtrip integrity)")
    void testRuleChainsRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/rule-chains-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);
        waitForBlocksToRender();
        String exported = exportYamlContent();

        assertNotNull(exported, "Exported YAML should not be null");
        assertFalse(exported.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exported.contains("rule-chains:"),
                "Exported YAML must contain 'rule-chains:' section — was silently dropped before fix");
        assertTrue(exported.contains("approval-chain") || exported.contains("approval_chain"),
                "Exported YAML should contain the first chain ID");
        assertTrue(exported.contains("sequential-check") || exported.contains("sequential_check"),
                "Exported YAML should contain the second chain ID");
    }

    // ========================================================================
    // METADATA ROUNDTRIP — Rule Config (Fix #11)
    // ========================================================================

    @Test
    @Order(3)
    @DisplayName("Phase1-RT-3: Import sets created-by on rule-config block")
    void testMetadataCreatedByImported() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/metadata-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        String value = getBlockFieldValue("apex_rule_config", "CREATED_BY");
        assertEquals("John Smith", value,
                "CREATED_BY should be imported from metadata.created-by");
    }

    @Test
    @Order(4)
    @DisplayName("Phase1-RT-4: Import sets created-date on rule-config block")
    void testMetadataCreatedDateImported() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/metadata-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        String value = getBlockFieldValue("apex_rule_config", "CREATED_DATE");
        assertEquals("2025-01-15", value,
                "CREATED_DATE should be imported from metadata.created-date");
    }

    @Test
    @Order(5)
    @DisplayName("Phase1-RT-5: Import sets last-modified on rule-config block")
    void testMetadataLastModifiedImported() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/metadata-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        String value = getBlockFieldValue("apex_rule_config", "LAST_MODIFIED");
        assertEquals("2025-06-20", value,
                "LAST_MODIFIED should be imported from metadata.last-modified");
    }

    @Test
    @Order(6)
    @DisplayName("Phase1-RT-6: All metadata fields survive rule-config roundtrip")
    void testMetadataRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/metadata-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);
        waitForBlocksToRender();
        String exported = exportYamlContent();

        assertNotNull(exported, "Exported YAML should not be null");
        assertFalse(exported.isEmpty(), "Exported YAML should not be empty");

        // Verify metadata fields that were previously lost
        assertTrue(exported.contains("created-by:"),
                "Exported YAML must contain 'created-by' — was lost before fix");
        assertTrue(exported.contains("John Smith"),
                "Exported YAML must preserve created-by value 'John Smith'");
        assertTrue(exported.contains("created-date:"),
                "Exported YAML must contain 'created-date'");
        assertTrue(exported.contains("2025-01-15"),
                "Exported YAML must preserve created-date value");
        assertTrue(exported.contains("last-modified:"),
                "Exported YAML must contain 'last-modified'");
        assertTrue(exported.contains("2025-06-20"),
                "Exported YAML must preserve last-modified value");

        // Verify existing fields still work
        assertTrue(exported.contains("business-domain:"),
                "Exported YAML must contain 'business-domain'");
        assertTrue(exported.contains("Trade Settlement"),
                "Exported YAML must preserve business-domain value");
        assertTrue(exported.contains("Compliance Team"),
                "Exported YAML must preserve owner value");
    }

    @Test
    @Order(7)
    @DisplayName("Phase1-RT-7: Verify all rule-config metadata block fields after import")
    void testAllMetadataBlockFields() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/metadata-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockFieldValues("apex_rule_config", Map.of(
                "ID", "metadata-roundtrip-test",
                "NAME", "Metadata Roundtrip Test",
                "VERSION", "2.1.0",
                "AUTHOR", "APEX Test Suite",
                "CREATED_BY", "John Smith",
                "CREATED_DATE", "2025-01-15",
                "LAST_MODIFIED", "2025-06-20",
                "BUSINESS_DOMAIN", "Trade Settlement",
                "OWNER", "Compliance Team"
        ));
    }

    // ========================================================================
    // METADATA ROUNDTRIP — Data Source Config (Fix #12)
    // ========================================================================

    @Test
    @Order(8)
    @DisplayName("Phase1-RT-8: Import sets created-by on data-source-config block")
    void testDsMetadataCreatedByImported() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/ds-metadata-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        String value = getBlockFieldValue("apex_data_source_config", "CREATED_BY");
        assertEquals("Jane Doe", value,
                "CREATED_BY should be imported from data-source metadata.created-by");
    }

    @Test
    @Order(9)
    @DisplayName("Phase1-RT-9: Import sets created-date on data-source-config block")
    void testDsMetadataCreatedDateImported() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/ds-metadata-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        String value = getBlockFieldValue("apex_data_source_config", "CREATED_DATE");
        assertEquals("2025-03-10", value,
                "CREATED_DATE should be imported from data-source metadata.created-date");
    }

    @Test
    @Order(10)
    @DisplayName("Phase1-RT-10: Import sets last-modified on data-source-config block")
    void testDsMetadataLastModifiedImported() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/ds-metadata-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        String value = getBlockFieldValue("apex_data_source_config", "LAST_MODIFIED");
        assertEquals("2025-07-15", value,
                "LAST_MODIFIED should be imported from data-source metadata.last-modified");
    }

    @Test
    @Order(11)
    @DisplayName("Phase1-RT-11: All data-source metadata fields survive roundtrip")
    void testDsMetadataRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/ds-metadata-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);
        waitForBlocksToRender();
        String exported = exportYamlContent();

        assertNotNull(exported, "Exported YAML should not be null");
        assertFalse(exported.isEmpty(), "Exported YAML should not be empty");

        assertTrue(exported.contains("created-by:"),
                "Data-source exported YAML must contain 'created-by'");
        assertTrue(exported.contains("Jane Doe"),
                "Data-source exported YAML must preserve created-by value");
        assertTrue(exported.contains("created-date:"),
                "Data-source exported YAML must contain 'created-date'");
        assertTrue(exported.contains("last-modified:"),
                "Data-source exported YAML must contain 'last-modified'");
    }

    // ========================================================================
    // COMBINED ROUNDTRIP (All Phase 1 Fixes Together)
    // ========================================================================

    @Test
    @Order(12)
    @DisplayName("Phase1-RT-12: Combined import creates all expected blocks")
    void testCombinedImportCreatesBlocks() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/combined-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");

        // Verify metadata fields set correctly
        verifyBlockFieldValues("apex_rule_config", Map.of(
                "ID", "combined-roundtrip-test",
                "CREATED_BY", "Phase1 Tester",
                "CREATED_DATE", "2025-08-01",
                "LAST_MODIFIED", "2025-12-15",
                "BUSINESS_DOMAIN", "Risk Management"
        ));

        int blockCount = getBlockCount();
        // Expect: config + rules section + 1 rule + rule-chains section + 1 chain
        //         + enrichments section + 1 enrichment = at least 7
        assertTrue(blockCount >= 5,
                "Should have at least 5 blocks (config + sections + children), found: " + blockCount);
    }

    @Test
    @Order(13)
    @DisplayName("Phase1-RT-13: Combined roundtrip preserves all sections and metadata")
    void testCombinedRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/combined-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);
        waitForBlocksToRender();
        String exported = exportYamlContent();

        assertNotNull(exported, "Exported YAML should not be null");
        assertFalse(exported.isEmpty(), "Exported YAML should not be empty");

        // Metadata roundtrip
        verifyYamlStructure(exported, List.of("metadata"));
        assertTrue(exported.contains("created-by:"),
                "Combined: created-by must survive roundtrip");
        assertTrue(exported.contains("Phase1 Tester"),
                "Combined: created-by value must survive roundtrip");
        assertTrue(exported.contains("created-date:"),
                "Combined: created-date must survive roundtrip");
        assertTrue(exported.contains("last-modified:"),
                "Combined: last-modified must survive roundtrip");

        // Rule-chains roundtrip (the critical generator fix)
        assertTrue(exported.contains("rule-chains:"),
                "Combined: rule-chains section must survive roundtrip");
        assertTrue(exported.contains("risk-chain") || exported.contains("risk_chain"),
                "Combined: rule chain ID must survive roundtrip");

        // Rules still work
        assertTrue(exported.contains("rules:"),
                "Combined: rules section must survive roundtrip");

        // Enrichments still work
        assertTrue(exported.contains("enrichments:"),
                "Combined: enrichments section must survive roundtrip");
    }

    // ========================================================================
    // REGRESSION — Existing Waterfall Example Still Works
    // ========================================================================

    @Test
    @Order(14)
    @DisplayName("Phase1-RT-14: Existing waterfall-approval.yaml roundtrips correctly")
    void testExistingWaterfallRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/conditional/waterfall-approval.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        int blockCount = getBlockCount();
        assertTrue(blockCount >= 4,
                "Waterfall YAML should import multiple blocks, found: " + blockCount);

        waitForBlocksToRender();
        String exported = exportYamlContent();

        assertNotNull(exported, "Waterfall exported YAML should not be null");
        assertFalse(exported.isEmpty(), "Waterfall exported YAML should not be empty");
        assertTrue(exported.contains("rule-chains:"),
                "Waterfall: rule-chains must now appear in exported YAML (was dropped before fix)");
        assertTrue(exported.contains("conditional-chaining") || exported.contains("conditional_chaining"),
                "Waterfall: chain pattern 'conditional-chaining' must survive roundtrip");
    }
}
