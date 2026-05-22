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
package dev.mars.apex.core.integration;

import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.core.config.loader.ConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests that prove the sibling-folder reference resolution gap.
 *
 * <p>In a shared config-repo layout, YAML files for a particular stage live in deep
 * sub-folders such as {@code configuration/FX/NEW/}, while shared infrastructure
 * configs (data-sources, enrichments) live in sibling folders like
 * {@code configuration/DB/} and {@code configuration/RuleBuilder/}.
 *
 * <p>A stage YAML should therefore be able to write short references such as:
 * <pre>{@code
 * data-source-refs:
 *   - name: "postgres-db"
 *     source: "DB/database-postgres.yaml"
 *
 * enrichment-refs:
 *   - name: "Map Currency Code"
 *     source: "RuleBuilder/RB_Map_Currency_Code.yaml"
 * }</pre>
 *
 * <p>These tests FAIL with the current resolver (which only looks one level:
 * {@code sourceDirectory/<ref>}) and are intended to drive the fix of walking
 * up ancestor directories until the referenced file is found.
 *
 * <h2>Directory structure created by each test</h2>
 * <pre>
 * tempDir/
 *   configuration/
 *     FX/
 *       NEW/
 *         stage.yaml          ← loaded by ConfigurationLoader
 *     DB/
 *       database-postgres.yaml  ← sibling of FX, NOT under FX/NEW/
 *     RuleBuilder/
 *       RB_Map_Currency_Code.yaml  ← sibling of FX, NOT under FX/NEW/
 * </pre>
 *
 * <h2>Current (broken) resolver behaviour</h2>
 * {@code "DB/database-postgres.yaml"} resolves to
 * {@code configuration/FX/NEW/DB/database-postgres.yaml} — which does not exist.
 *
 * <h2>Required (fixed) resolver behaviour</h2>
 * The resolver should walk up from {@code configuration/FX/NEW/} through
 * {@code configuration/FX/} and then {@code configuration/}, finding
 * {@code configuration/DB/database-postgres.yaml} on the third attempt.
 */
@DisplayName("Sibling Folder Ref Resolution Tests (TDD — currently failing)")
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class SiblingFolderRefResolutionTest {

    private static final Logger logger = LoggerFactory.getLogger(SiblingFolderRefResolutionTest.class);

    @TempDir
    Path tempDir;

    private ConfigurationLoader configLoader;

    @BeforeEach
    void setUp() {
        configLoader = new ConfigurationLoader();
    }

    // =========================================================================
    // data-source-refs
    // =========================================================================

    @Test
    @DisplayName("data-source-refs: short sibling-folder path should resolve via ancestor walk-up")
    void dataSourceRefShouldResolveViaSiblingFolder() throws Exception {
        /*
         * Layout
         * ------
         *  configuration/
         *    FX/
         *      NEW/
         *        stage.yaml              ← loaded here
         *    DB/
         *      database-postgres.yaml   ← referenced as "DB/database-postgres.yaml"
         *                                  (two levels above stage.yaml's directory)
         */
        Path configRoot     = Files.createDirectories(tempDir.resolve("configuration"));
        Path stageDir       = Files.createDirectories(configRoot.resolve("FX/NEW"));
        Path dbDir          = Files.createDirectories(configRoot.resolve("DB"));

        Path dataSourceFile = dbDir.resolve("database-postgres.yaml");
        Files.writeString(dataSourceFile, externalDataSourceYaml("postgres-db"));

        Path stageFile = stageDir.resolve("stage.yaml");
        Files.writeString(stageFile, stageYamlWithDataSourceRef("DB/database-postgres.yaml"));

        logger.info("=== TEST: dataSourceRefShouldResolveViaSiblingFolder ===");
        logger.info("Stage YAML  : {}", stageFile);
        logger.info("DS ref file : {}", dataSourceFile);
        logger.info("Ref as written in YAML: DB/database-postgres.yaml");

        /*
         * With the current resolver this throws ConfigurationException because it
         * only looks at configuration/FX/NEW/DB/database-postgres.yaml, which does
         * not exist.  After the ancestor-walk-up fix is applied, the config should
         * load successfully and the data-source should be present.
         */
        YamlRuleConfiguration config = configLoader.loadFromFile(stageFile.toString());

        assertNotNull(config, "Configuration should load without error");
        assertNotNull(config.getDataSources(),
                "data-sources should be populated after resolving data-source-refs");
        assertFalse(config.getDataSources().isEmpty(),
                "At least one data-source should be present after resolving the sibling-folder ref");
        assertEquals("postgres-db", config.getDataSources().get(0).getName(),
                "The resolved data-source should carry the name declared in data-source-refs");

        logger.info("data-sources resolved: {}", config.getDataSources().size());
    }

    @Test
    @DisplayName("data-source-refs: path two levels deep from shared root should still resolve")
    void dataSourceRefShouldResolveWhenRefIsTwoLevelsBelowSharedRoot() throws Exception {
        /*
         * Layout
         * ------
         *  configuration/
         *    FX/
         *      NEW/
         *        stage.yaml           ← loaded here
         *    DB/
         *      database-postgres.yaml  ← only one hop needed, but ref is written as
         *                                "DB/database-postgres.yaml" (sibling of FX)
         *
         * This is identical to the first test but the name makes the relationship
         * between ref string and directory depth explicit for documentation.
         */
        Path configRoot     = Files.createDirectories(tempDir.resolve("configuration"));
        Path stageDir       = Files.createDirectories(configRoot.resolve("FX/NEW"));
        Path dbDir          = Files.createDirectories(configRoot.resolve("DB"));

        Path dataSourceFile = dbDir.resolve("database-postgres.yaml");
        Files.writeString(dataSourceFile, externalDataSourceYaml("postgres-db"));

        Path stageFile = stageDir.resolve("stage.yaml");
        Files.writeString(stageFile, stageYamlWithDataSourceRef("DB/database-postgres.yaml"));

        YamlRuleConfiguration config = configLoader.loadFromFile(stageFile.toString());

        assertNotNull(config);
        assertFalse(config.getDataSources().isEmpty(),
                "data-source ref 'DB/database-postgres.yaml' should resolve two levels up to configuration/DB/");
    }

    // =========================================================================
    // enrichment-refs
    // =========================================================================

    @Test
    @DisplayName("enrichment-refs: short sibling-folder path should resolve via ancestor walk-up")
    void enrichmentRefShouldResolveViaSiblingFolder() throws Exception {
        /*
         * Layout
         * ------
         *  configuration/
         *    FX/
         *      NEW/
         *        stage.yaml                    ← loaded here
         *    RuleBuilder/
         *      RB_Map_Currency_Code.yaml       ← referenced as "RuleBuilder/RB_Map_Currency_Code.yaml"
         */
        Path configRoot      = Files.createDirectories(tempDir.resolve("configuration"));
        Path stageDir        = Files.createDirectories(configRoot.resolve("FX/NEW"));
        Path ruleBuilderDir  = Files.createDirectories(configRoot.resolve("RuleBuilder"));

        Path enrichmentFile  = ruleBuilderDir.resolve("RB_Map_Currency_Code.yaml");
        Files.writeString(enrichmentFile, enrichmentYaml("map-currency-code"));

        Path stageFile = stageDir.resolve("stage.yaml");
        Files.writeString(stageFile, stageYamlWithEnrichmentRef("RuleBuilder/RB_Map_Currency_Code.yaml"));

        logger.info("=== TEST: enrichmentRefShouldResolveViaSiblingFolder ===");
        logger.info("Stage YAML     : {}", stageFile);
        logger.info("Enrichment file: {}", enrichmentFile);
        logger.info("Ref as written in YAML: RuleBuilder/RB_Map_Currency_Code.yaml");

        /*
         * With the current resolver this throws ConfigurationException because it
         * only looks at configuration/FX/NEW/RuleBuilder/RB_Map_Currency_Code.yaml,
         * which does not exist.  After the ancestor-walk-up fix is applied, the
         * config should load successfully and the enrichment should be present.
         */
        YamlRuleConfiguration config = configLoader.loadFromFile(stageFile.toString());

        assertNotNull(config, "Configuration should load without error");
        assertNotNull(config.getEnrichments(),
                "enrichments should be populated after resolving enrichment-refs");
        assertFalse(config.getEnrichments().isEmpty(),
                "At least one enrichment should be present after resolving the sibling-folder ref");
        assertEquals("map-currency-code", config.getEnrichments().get(0).getId(),
                "The resolved enrichment should carry the ID declared in the enrichment file");

        logger.info("enrichments resolved: {}", config.getEnrichments().size());
    }

    @Test
    @DisplayName("enrichment-refs: three-level nest — ref resolved two hops above stage dir")
    void enrichmentRefShouldResolveWhenStageDirIsThreeLevelsDeep() throws Exception {
        /*
         * Layout
         * ------
         *  root/
         *    app/
         *      config/
         *        FX/
         *          AMENDED/
         *            stage.yaml                  ← four levels deep
         *        Shared/
         *          enrichments.yaml              ← referenced as "Shared/enrichments.yaml"
         */
        Path root       = Files.createDirectories(tempDir.resolve("root/app/config"));
        Path stageDir   = Files.createDirectories(root.resolve("FX/AMENDED"));
        Path sharedDir  = Files.createDirectories(root.resolve("Shared"));

        Path enrichFile = sharedDir.resolve("enrichments.yaml");
        Files.writeString(enrichFile, enrichmentYaml("shared-enrich"));

        Path stageFile  = stageDir.resolve("stage.yaml");
        Files.writeString(stageFile, stageYamlWithEnrichmentRef("Shared/enrichments.yaml"));

        YamlRuleConfiguration config = configLoader.loadFromFile(stageFile.toString());

        assertNotNull(config);
        assertFalse(config.getEnrichments().isEmpty(),
                "enrichment-ref 'Shared/enrichments.yaml' should resolve two levels above the stage dir");
        assertEquals("shared-enrich", config.getEnrichments().get(0).getId());
    }

    // =========================================================================
    // rule-refs
    // =========================================================================

    @Test
    @DisplayName("rule-refs: short sibling-folder path should resolve via ancestor walk-up")
    void ruleRefShouldResolveViaSiblingFolder() throws Exception {
        /*
         * Layout
         * ------
         *  configuration/
         *    FX/
         *      NEW/
         *        stage.yaml           ← loaded here
         *    Rules/
         *      fx-rules.yaml          ← referenced as "Rules/fx-rules.yaml"
         *
         * rule-refs goes through loadRuleFileRecursive (same code path as enrichment-refs).
         * The same ancestor walk-up fix must cover it.
         */
        Path configRoot = Files.createDirectories(tempDir.resolve("configuration"));
        Path stageDir   = Files.createDirectories(configRoot.resolve("FX/NEW"));
        Path rulesDir   = Files.createDirectories(configRoot.resolve("Rules"));

        Path rulesFile  = rulesDir.resolve("fx-rules.yaml");
        Files.writeString(rulesFile, ruleFileYaml("fx-rule-1"));

        Path stageFile  = stageDir.resolve("stage.yaml");
        Files.writeString(stageFile, stageYamlWithRuleRef("Rules/fx-rules.yaml"));

        logger.info("=== TEST: ruleRefShouldResolveViaSiblingFolder ===");
        logger.info("Stage YAML : {}", stageFile);
        logger.info("Rules file : {}", rulesFile);

        YamlRuleConfiguration config = configLoader.loadFromFile(stageFile.toString());

        assertNotNull(config, "Configuration should load without error");
        assertNotNull(config.getRules(),
                "rules should be populated after resolving rule-refs");
        assertFalse(config.getRules().isEmpty(),
                "rule-ref 'Rules/fx-rules.yaml' should resolve via sibling-folder ancestor walk-up");
        assertEquals("fx-rule-1", config.getRules().get(0).getId(),
                "The resolved rule should carry the ID declared in the external rules file");

        logger.info("rules resolved: {}", config.getRules().size());
    }

    // =========================================================================
    // chained enrichment-refs
    // =========================================================================

    @Test
    @DisplayName("enrichment-refs: chained — A.yaml (sibling) itself refs B.yaml (another sibling)")
    void chainedEnrichmentRefShouldResolveViaSiblingFolders() throws Exception {
        /*
         * Layout
         * ------
         *  configuration/
         *    FX/
         *      NEW/
         *        stage.yaml          ← enrichment-refs: "RuleBuilder/A.yaml"
         *    RuleBuilder/
         *      A.yaml                ← enrichment-refs: "Shared/B.yaml"
         *    Shared/
         *      B.yaml                ← the terminal enrichment
         *
         * loadRuleFileRecursive sets sourceDirectory to the loaded file's own parent
         * before recursing, so the ancestor walk-up must work at EVERY level of the
         * chain, not just from the initial stage directory.
         */
        Path configRoot     = Files.createDirectories(tempDir.resolve("configuration"));
        Path stageDir       = Files.createDirectories(configRoot.resolve("FX/NEW"));
        Path ruleBuilderDir = Files.createDirectories(configRoot.resolve("RuleBuilder"));
        Path sharedDir      = Files.createDirectories(configRoot.resolve("Shared"));

        // B.yaml: standalone enrichment
        Path bFile = sharedDir.resolve("B.yaml");
        Files.writeString(bFile, enrichmentYaml("enrich-b"));

        // A.yaml: has its own enrichment-refs pointing to Shared/B.yaml
        Path aFile = ruleBuilderDir.resolve("A.yaml");
        Files.writeString(aFile, enrichmentYamlWithEnrichmentRef("enrich-a", "Shared/B.yaml"));

        // stage.yaml: has enrichment-refs pointing to RuleBuilder/A.yaml
        Path stageFile = stageDir.resolve("stage.yaml");
        Files.writeString(stageFile, stageYamlWithEnrichmentRef("RuleBuilder/A.yaml"));

        logger.info("=== TEST: chainedEnrichmentRefShouldResolveViaSiblingFolders ===");
        logger.info("Stage   : {}", stageFile);
        logger.info("A.yaml  : {}", aFile);
        logger.info("B.yaml  : {}", bFile);

        YamlRuleConfiguration config = configLoader.loadFromFile(stageFile.toString());

        assertNotNull(config, "Configuration should load without error");
        assertNotNull(config.getEnrichments(),
                "enrichments should be populated after resolving chained enrichment-refs");

        List<String> enrichmentIds = config.getEnrichments().stream()
                .map(e -> e.getId())
                .toList();

        assertTrue(enrichmentIds.contains("enrich-a"),
                "enrich-a (from A.yaml in RuleBuilder/) should be present after first ancestor walk-up");
        assertTrue(enrichmentIds.contains("enrich-b"),
                "enrich-b (from B.yaml in Shared/) should be present via chained ref from A.yaml");

        logger.info("enrichments resolved: {} — ids: {}", config.getEnrichments().size(), enrichmentIds);
    }

    // =========================================================================
    // Negative: current sourceDirectory-relative resolution still works
    // =========================================================================

    @Test
    @DisplayName("enrichment-refs: reference in same folder still resolves (existing behaviour preserved)")
    void enrichmentRefInSameFolderContinuesToWork() throws Exception {
        /*
         * If the referenced file is already a sibling of the loading YAML the existing
         * behaviour (sourceDirectory/<ref>) should continue to work unchanged.
         */
        Path stageDir   = Files.createDirectories(tempDir.resolve("FX/NEW"));
        Path enrichFile = stageDir.resolve("local-enrichment.yaml");
        Files.writeString(enrichFile, enrichmentYaml("local-enrich"));

        Path stageFile  = stageDir.resolve("stage.yaml");
        Files.writeString(stageFile, stageYamlWithEnrichmentRef("local-enrichment.yaml"));

        YamlRuleConfiguration config = configLoader.loadFromFile(stageFile.toString());

        assertNotNull(config);
        assertFalse(config.getEnrichments().isEmpty(),
                "enrichment in the same directory as the loading YAML should still resolve");
        assertEquals("local-enrich", config.getEnrichments().get(0).getId());
    }

    @Test
    @DisplayName("data-source-refs: absolute path still resolves (existing behaviour preserved)")
    void dataSourceRefWithAbsolutePathContinuesToWork() throws Exception {
        Path dbDir          = Files.createDirectories(tempDir.resolve("external"));
        Path dataSourceFile = dbDir.resolve("abs-database.yaml");
        Files.writeString(dataSourceFile, externalDataSourceYaml("abs-db"));

        // Use the absolute path directly — the resolver must still accept it
        Path stageFile = tempDir.resolve("stage.yaml");
        Files.writeString(stageFile,
                stageYamlWithDataSourceRef(dataSourceFile.toAbsolutePath().toString().replace("\\", "\\\\")));

        YamlRuleConfiguration config = configLoader.loadFromFile(stageFile.toString());

        assertNotNull(config);
        assertFalse(config.getDataSources().isEmpty(),
                "data-source ref with absolute path should still resolve");
        // The resolved name is taken from the data-source-refs[].name field in the stage YAML ("postgres-db"),
        // not from the name declared inside the external data-source file.
        assertEquals("postgres-db", config.getDataSources().get(0).getName(),
                "data-source name should match the name declared in data-source-refs");
    }

    // =========================================================================
    // YAML helpers
    // =========================================================================

    /**
     * Minimal APEX external-data-config YAML (used as the target of a data-source-ref).
     */
    private String externalDataSourceYaml(String dsName) {
        return """
                metadata:
                  id: "%s-config"
                  name: "%s"
                  type: "external-data-config"
                  version: "1.0.0"
                  description: "Test external data-source for sibling-folder resolution tests"
                data-sources:
                  - name: "%s"
                    type: "database"
                    source-type: "h2"
                    enabled: true
                    connection:
                      database: "testdb"
                      username: "sa"
                      password: ""
                    queries:
                      getRecord: "SELECT * FROM tbl WHERE id = :id"
                """.formatted(dsName, dsName, dsName);
    }

    /**
     * Minimal APEX enrichment YAML (used as the target of an enrichment-ref).
     */
    private String enrichmentYaml(String enrichmentId) {
        return """
                metadata:
                  id: "%s-config"
                  name: "%s"
                  version: "1.0.0"
                enrichments:
                  - id: "%s"
                    name: "%s"
                    type: field-enrichment
                    enabled: true
                    field-mappings:
                      - source-field: "'TEST_VALUE'"
                        target-field: 'resolvedField'
                """.formatted(enrichmentId, enrichmentId, enrichmentId, enrichmentId);
    }

    /**
     * Stage YAML that references a data-source via a short (possibly sibling-folder) path.
     */
    private String stageYamlWithDataSourceRef(String sourcePath) {
        return """
                metadata:
                  id: "fx-new-stage"
                  name: "FX NEW Stage"
                  version: "1.0.0"
                data-source-refs:
                  - name: "postgres-db"
                    source: "%s"
                    enabled: true
                rules:
                  - id: "placeholder-rule"
                    name: "Placeholder"
                    condition: "true"
                    message: "placeholder"
                    severity: "INFO"
                rule-groups:
                  - id: "placeholder-group"
                    name: "Placeholder Group"
                    operator: "AND"
                    rule-ids:
                      - "placeholder-rule"
                """.formatted(sourcePath);
    }

    /**
     * Minimal APEX rules YAML (used as the target of a rule-ref).
     */
    private String ruleFileYaml(String ruleId) {
        return """
                metadata:
                  id: "%s-config"
                  name: "%s"
                  version: "1.0.0"
                rules:
                  - id: "%s"
                    name: "%s"
                    condition: "true"
                    message: "rule passed"
                    severity: INFO
                """.formatted(ruleId, ruleId, ruleId, ruleId);
    }

    /**
     * Minimal APEX enrichment YAML that also declares its own enrichment-ref
     * (used to test chained / nested enrichment-refs resolution).
     */
    private String enrichmentYamlWithEnrichmentRef(String enrichmentId, String chainedRefSource) {
        return """
                metadata:
                  id: "%s-config"
                  name: "%s"
                  version: "1.0.0"
                enrichments:
                  - id: "%s"
                    name: "%s"
                    type: field-enrichment
                    enabled: true
                    field-mappings:
                      - source-field: "'TEST_VALUE'"
                        target-field: resolvedField
                enrichment-refs:
                  - name: "Chained Enrichment"
                    source: "%s"
                    enabled: true
                """.formatted(enrichmentId, enrichmentId, enrichmentId, enrichmentId, chainedRefSource);
    }

    /**
     * Stage YAML that references external rules via a short (possibly sibling-folder) path.
     */
    private String stageYamlWithRuleRef(String sourcePath) {
        return """
                metadata:
                  id: "fx-new-stage"
                  name: "FX NEW Stage"
                  version: "1.0.0"
                rule-refs:
                  - name: "FX Rules"
                    source: "%s"
                    enabled: true
                """.formatted(sourcePath);
    }

    /**
     * Stage YAML that references an enrichment file via a short (possibly sibling-folder) path.
     */
    private String stageYamlWithEnrichmentRef(String sourcePath) {
        return """
                metadata:
                  id: "fx-new-stage"
                  name: "FX NEW Stage"
                  version: "1.0.0"
                enrichment-refs:
                  - name: "Map Currency Code"
                    source: "%s"
                    enabled: true
                rules:
                  - id: "placeholder-rule"
                    name: "Placeholder"
                    condition: "true"
                    message: "placeholder"
                    severity: "INFO"
                rule-groups:
                  - id: "placeholder-group"
                    name: "Placeholder Group"
                    operator: "AND"
                    rule-ids:
                      - "placeholder-rule"
                """.formatted(sourcePath);
    }
}
