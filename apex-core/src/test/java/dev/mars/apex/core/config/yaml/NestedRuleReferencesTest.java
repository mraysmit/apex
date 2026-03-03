package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.loader.*;
import dev.mars.apex.core.config.exception.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for nested rule-ref processing in ConfigurationLoader.
 *
 * These tests specifically target {@code processRuleReferencesRecursive()},
 * the recursive path triggered when a referenced rule file itself contains
 * rule-refs pointing to further files (depth 2+).
 *
 * Coverage targets:
 * <ul>
 *   <li>{@code processRuleReferencesRecursive()} — 43 lines, previously 0% covered</li>
 *   <li>{@code loadRuleFileRecursive()} — cycle detection / duplicate prevention branches</li>
 * </ul>
 */
@DisplayName("Nested Rule References (recursive rule-ref processing)")
class NestedRuleReferencesTest {

    private ConfigurationLoader loader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new ConfigurationLoader();
    }

    // ==================================================================================
    // Depth-2 nesting: main → level1 → level2
    // ==================================================================================

    @Test
    @DisplayName("Should resolve 2-level nested rule-refs (main → L1 → L2)")
    void testTwoLevelNestedRuleRefs() throws Exception {
        // Level 2 — leaf file with rules (no further rule-refs)
        Path level2 = tempDir.resolve("level2-rules.yaml");
        Files.writeString(level2, """
            metadata:
              name: "Level 2 Rules"
              version: "1.0.0"
            
            rules:
              - id: "l2-rule-1"
                name: "Level 2 Rule 1"
                condition: "#amount > 1000"
                message: "Amount exceeds Level 2 threshold"
              - id: "l2-rule-2"
                name: "Level 2 Rule 2"
                condition: "#currency != null"
                message: "Currency is required at Level 2"
            """);

        // Level 1 — references level2 via rule-refs
        Path level1 = tempDir.resolve("level1-rules.yaml");
        Files.writeString(level1, """
            metadata:
              name: "Level 1 Rules"
              version: "1.0.0"
            
            rules:
              - id: "l1-rule-1"
                name: "Level 1 Rule 1"
                condition: "#tradeType != null"
                message: "Trade type is required at Level 1"
            
            rule-refs:
              - name: "level2-ref"
                source: "%s"
                description: "References level 2 rules"
            """.formatted(level1ToLevel2Path(level2)));

        // Main config references level1
        YamlRuleConfiguration config = new YamlRuleConfiguration();
        List<YamlRuleRef> ruleRefs = new ArrayList<>();
        ruleRefs.add(new YamlRuleRef("level1-ref", level1.toString()));
        config.setRuleRefs(ruleRefs);

        loader.processReferencesAndValidate(config);

        // Should have all 3 rules merged (1 from L1 + 2 from L2)
        assertNotNull(config.getRules(), "Rules should not be null after nested processing");
        assertEquals(3, config.getRules().size(),
                "Should have 3 rules total (1 from L1 + 2 from L2)");

        List<String> ruleIds = config.getRules().stream()
                .map(YamlRule::getId)
                .toList();
        assertTrue(ruleIds.contains("l1-rule-1"), "Should contain L1 rule");
        assertTrue(ruleIds.contains("l2-rule-1"), "Should contain first L2 rule");
        assertTrue(ruleIds.contains("l2-rule-2"), "Should contain second L2 rule");
    }

    // ==================================================================================
    // Depth-3 nesting: main → L1 → L2 → L3
    // ==================================================================================

    @Test
    @DisplayName("Should resolve 3-level nested rule-refs (main → L1 → L2 → L3)")
    void testThreeLevelNestedRuleRefs() throws Exception {
        // Level 3 — deepest leaf
        Path level3 = tempDir.resolve("level3-rules.yaml");
        Files.writeString(level3, """
            metadata:
              name: "Level 3 Rules"
              version: "1.0.0"
            
            rules:
              - id: "l3-rule-1"
                name: "Level 3 Rule"
                condition: "#counterparty != null"
                message: "Counterparty required (L3)"
            """);

        // Level 2 — references level3
        Path level2 = tempDir.resolve("level2-rules.yaml");
        Files.writeString(level2, """
            metadata:
              name: "Level 2 Rules"
              version: "1.0.0"
            
            rules:
              - id: "l2-rule-1"
                name: "Level 2 Rule"
                condition: "#amount > 500"
                message: "Amount check (L2)"
            
            rule-refs:
              - name: "level3-ref"
                source: "%s"
            """.formatted(level1ToLevel2Path(level3)));

        // Level 1 — references level2
        Path level1 = tempDir.resolve("level1-rules.yaml");
        Files.writeString(level1, """
            metadata:
              name: "Level 1 Rules"
              version: "1.0.0"
            
            rules:
              - id: "l1-rule-1"
                name: "Level 1 Rule"
                condition: "#tradeId != null"
                message: "Trade ID required (L1)"
            
            rule-refs:
              - name: "level2-ref"
                source: "%s"
            """.formatted(level1ToLevel2Path(level2)));

        // Main config
        YamlRuleConfiguration config = new YamlRuleConfiguration();
        config.setRuleRefs(List.of(new YamlRuleRef("level1-ref", level1.toString())));

        loader.processReferencesAndValidate(config);

        // 3 rules total: 1 from each level
        assertNotNull(config.getRules());
        assertEquals(3, config.getRules().size(),
                "Should have 3 rules (1 from L1, 1 from L2, 1 from L3)");

        List<String> ids = config.getRules().stream().map(YamlRule::getId).toList();
        assertTrue(ids.contains("l1-rule-1"), "L1 rule present");
        assertTrue(ids.contains("l2-rule-1"), "L2 rule present");
        assertTrue(ids.contains("l3-rule-1"), "L3 rule present");
    }

    // ==================================================================================
    // Cycle detection: L1 → L2 → L1 (should not loop infinitely)
    // ==================================================================================

    @Test
    @DisplayName("Should handle circular rule-refs without infinite recursion")
    void testCircularRuleRefsSafeHandling() throws Exception {
        // Create two files that reference each other (circular)
        Path fileA = tempDir.resolve("file-a.yaml");
        Path fileB = tempDir.resolve("file-b.yaml");

        // Write file A — references file B
        Files.writeString(fileA, """
            metadata:
              name: "File A Rules"
              version: "1.0.0"
            
            rules:
              - id: "rule-a"
                name: "Rule A"
                condition: "#a != null"
                message: "Rule A"
            
            rule-refs:
              - name: "ref-to-b"
                source: "%s"
            """.formatted(level1ToLevel2Path(fileB)));

        // Write file B — references file A (circular!)
        Files.writeString(fileB, """
            metadata:
              name: "File B Rules"
              version: "1.0.0"
            
            rules:
              - id: "rule-b"
                name: "Rule B"
                condition: "#b != null"
                message: "Rule B"
            
            rule-refs:
              - name: "ref-to-a"
                source: "%s"
            """.formatted(level1ToLevel2Path(fileA)));

        // Main config references file A
        YamlRuleConfiguration config = new YamlRuleConfiguration();
        config.setRuleRefs(List.of(new YamlRuleRef("ref-a", fileA.toString())));

        // Should NOT infinite-loop — loadRuleFileRecursive uses loadedFiles set
        loader.processReferencesAndValidate(config);

        // Both rules should be loaded (each file loaded exactly once)
        assertNotNull(config.getRules());
        assertEquals(2, config.getRules().size(),
                "Should have exactly 2 rules (one from each file, no duplicates from cycle)");

        List<String> ids = config.getRules().stream().map(YamlRule::getId).toList();
        assertTrue(ids.contains("rule-a"), "Rule from file A present");
        assertTrue(ids.contains("rule-b"), "Rule from file B present");
    }

    // ==================================================================================
    // Disabled nested ref: L1 has a disabled rule-ref → L2 should not be loaded
    // ==================================================================================

    @Test
    @DisplayName("Should skip disabled nested rule-ref in recursive processing")
    void testDisabledNestedRuleRef() throws Exception {
        // Level 2 — should NOT be loaded
        Path level2 = tempDir.resolve("level2-rules.yaml");
        Files.writeString(level2, """
            metadata:
              name: "Level 2 Rules"
              version: "1.0.0"
            
            rules:
              - id: "l2-rule-should-not-load"
                name: "Should Not Load"
                condition: "#x > 0"
                message: "This should not appear"
            """);

        // Level 1 — has a DISABLED rule-ref to level2
        Path level1 = tempDir.resolve("level1-rules.yaml");
        Files.writeString(level1, """
            metadata:
              name: "Level 1 Rules"
              version: "1.0.0"
            
            rules:
              - id: "l1-rule-1"
                name: "Level 1 Rule"
                condition: "#y > 0"
                message: "L1 rule"
            
            rule-refs:
              - name: "disabled-l2-ref"
                source: "%s"
                enabled: false
                description: "This ref is disabled"
            """.formatted(level1ToLevel2Path(level2)));

        YamlRuleConfiguration config = new YamlRuleConfiguration();
        config.setRuleRefs(List.of(new YamlRuleRef("level1-ref", level1.toString())));

        loader.processReferencesAndValidate(config);

        // Only L1 rule should be loaded; L2 skipped because ref is disabled
        assertNotNull(config.getRules());
        assertEquals(1, config.getRules().size(),
                "Should have only 1 rule from L1 (disabled L2 ref skipped)");
        assertEquals("l1-rule-1", config.getRules().get(0).getId());
    }

    // ==================================================================================
    // Nested rule-refs with rule groups at a nested level
    // ==================================================================================

    @Test
    @DisplayName("Should merge rule groups from nested rule-ref levels")
    void testNestedRuleRefsWithRuleGroups() throws Exception {
        // Level 2 — has rules AND rule-groups
        Path level2 = tempDir.resolve("level2-grouped.yaml");
        Files.writeString(level2, """
            metadata:
              name: "Level 2 Grouped Rules"
              version: "1.0.0"
            
            rules:
              - id: "l2-grouped-rule-1"
                name: "L2 Grouped Rule 1"
                condition: "#amount > 100"
                message: "Amount above 100"
              - id: "l2-grouped-rule-2"
                name: "L2 Grouped Rule 2"
                condition: "#amount < 1000000"
                message: "Amount below limit"
            
            rule-groups:
              - id: "l2-amount-validation"
                name: "L2 Amount Validation Group"
                operator: "AND"
                rule-ids:
                  - "l2-grouped-rule-1"
                  - "l2-grouped-rule-2"
            """);

        // Level 1 — references level2
        Path level1 = tempDir.resolve("level1-rules.yaml");
        Files.writeString(level1, """
            metadata:
              name: "Level 1 Rules"
              version: "1.0.0"
            
            rules:
              - id: "l1-basic-rule"
                name: "L1 Basic Rule"
                condition: "#tradeId != null"
                message: "Trade ID is required"
            
            rule-refs:
              - name: "level2-grouped-ref"
                source: "%s"
            """.formatted(level1ToLevel2Path(level2)));

        YamlRuleConfiguration config = new YamlRuleConfiguration();
        config.setRuleRefs(List.of(new YamlRuleRef("level1-ref", level1.toString())));

        loader.processReferencesAndValidate(config);

        // Should have 3 rules (1 from L1 + 2 from L2)
        assertNotNull(config.getRules());
        assertEquals(3, config.getRules().size(),
                "Should have 3 rules (1 L1 + 2 L2 grouped)");

        // Should have rule group from L2
        assertNotNull(config.getRuleGroups(), "Rule groups should be merged from nested level");
        assertEquals(1, config.getRuleGroups().size(),
                "Should have 1 rule group from L2");
        assertEquals("l2-amount-validation", config.getRuleGroups().get(0).getId(),
                "Rule group ID should match");
    }

    // ==================================================================================
    // Diamond pattern: main → L1-A + L1-B → both reference same L2 (no duplicate load)
    // ==================================================================================

    @Test
    @DisplayName("Should handle diamond dependency (shared L2 loaded only once)")
    void testDiamondDependencyDeduplication() throws Exception {
        // Shared level 2 file
        Path shared = tempDir.resolve("shared-rules.yaml");
        Files.writeString(shared, """
            metadata:
              name: "Shared Rules"
              version: "1.0.0"
            
            rules:
              - id: "shared-rule"
                name: "Shared Rule"
                condition: "#valid == true"
                message: "Shared validation"
            """);

        // Level 1-A — references shared
        Path level1A = tempDir.resolve("level1-a.yaml");
        Files.writeString(level1A, """
            metadata:
              name: "Level 1-A"
              version: "1.0.0"
            
            rules:
              - id: "l1a-rule"
                name: "L1-A Rule"
                condition: "#a != null"
                message: "A present"
            
            rule-refs:
              - name: "shared-ref"
                source: "%s"
            """.formatted(level1ToLevel2Path(shared)));

        // Level 1-B — also references shared
        Path level1B = tempDir.resolve("level1-b.yaml");
        Files.writeString(level1B, """
            metadata:
              name: "Level 1-B"
              version: "1.0.0"
            
            rules:
              - id: "l1b-rule"
                name: "L1-B Rule"
                condition: "#b != null"
                message: "B present"
            
            rule-refs:
              - name: "shared-ref-again"
                source: "%s"
            """.formatted(level1ToLevel2Path(shared)));

        // Main references both L1-A and L1-B
        YamlRuleConfiguration config = new YamlRuleConfiguration();
        config.setRuleRefs(List.of(
                new YamlRuleRef("ref-1a", level1A.toString()),
                new YamlRuleRef("ref-1b", level1B.toString())
        ));

        loader.processReferencesAndValidate(config);

        // Should have 3 rules: l1a-rule, shared-rule (loaded once), l1b-rule
        // shared-rule should NOT be duplicated even though both L1-A and L1-B reference it
        assertNotNull(config.getRules());

        long sharedCount = config.getRules().stream()
                .filter(r -> "shared-rule".equals(r.getId()))
                .count();
        assertEquals(1, sharedCount,
                "Shared rule should appear exactly once (deduplication via loadedFiles set)");

        assertEquals(3, config.getRules().size(),
                "Should have 3 unique rules (l1a, shared, l1b)");
    }

    // ==================================================================================
    // Nested ref to missing file — error should propagate cleanly
    // ==================================================================================

    @Test
    @DisplayName("Should propagate error when nested rule-ref file is missing")
    void testNestedMissingFileError() throws Exception {
        // Level 1 — references a non-existent level2 file
        Path level1 = tempDir.resolve("level1-broken.yaml");
        Files.writeString(level1, """
            metadata:
              name: "Level 1 Broken"
              version: "1.0.0"
            
            rules:
              - id: "l1-rule"
                name: "L1 Rule"
                condition: "#x > 0"
                message: "L1"
            
            rule-refs:
              - name: "broken-nested-ref"
                source: "totally-missing-file.yaml"
            """);

        YamlRuleConfiguration config = new YamlRuleConfiguration();
        config.setRuleRefs(List.of(new YamlRuleRef("level1-ref", level1.toString())));

        ConfigurationException ex = assertThrows(
                ConfigurationException.class,
                () -> loader.processReferencesAndValidate(config),
                "Should throw when nested rule-ref target file is missing"
        );

        assertTrue(ex.getMessage().contains("level1-ref") || ex.getCause() != null,
                "Exception should reference the failing ref chain");
    }

    // ==================================================================================
    // Helper
    // ==================================================================================

    /**
     * Returns the absolute path string suitable for embedding in YAML.
     * On Windows, backslashes are converted to forward slashes to avoid
     * YAML escaping issues.
     */
    private String level1ToLevel2Path(Path target) {
        return target.toAbsolutePath().toString().replace('\\', '/');
    }
}
