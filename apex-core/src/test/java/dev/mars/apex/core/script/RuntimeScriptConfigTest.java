package dev.mars.apex.core.script;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.model.YamlRuntimeScriptConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for runtime script configuration model classes.
 * Verifies YAML binding for YamlRuntimeScriptConfig and ScriptMetadata record.
 */
@DisplayName("Runtime Script Configuration Tests")
class RuntimeScriptConfigTest {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Test
    @DisplayName("Should parse runtime-scripts YAML block with all fields")
    void testParseFullRuntimeScriptsConfig() throws Exception {
        String yaml = """
                runtime-scripts:
                  enabled: true
                  locations:
                    - "./config/scripts"
                    - "./extra-scripts"
                  engine: "groovy"
                  polling-interval-ms: 10000
                  execution-timeout-ms: 500
                  allowlist:
                    - "risk-score"
                    - "eligibility-check"
                  fail-mode: "fail-fast"
                rules:
                  - id: "test-rule"
                    condition: "true"
                    message: "always passes"
                    severity: "INFO"
                """;

        YamlRuleConfiguration config = yamlMapper.readValue(yaml, YamlRuleConfiguration.class);

        assertNotNull(config.getRuntimeScripts(), "runtime-scripts should be parsed");

        YamlRuntimeScriptConfig scripts = config.getRuntimeScripts();
        assertTrue(scripts.isEnabled());
        assertEquals(List.of("./config/scripts", "./extra-scripts"), scripts.getLocations());
        assertEquals("groovy", scripts.getEngine());
        assertEquals(10000, scripts.getPollingIntervalMs());
        assertEquals(500, scripts.getExecutionTimeoutMs());
        assertEquals(List.of("risk-score", "eligibility-check"), scripts.getAllowlist());
        assertEquals("fail-fast", scripts.getFailMode());

        // Rules should still parse alongside runtime-scripts
        assertNotNull(config.getRules());
        assertEquals(1, config.getRules().size());
        assertEquals("test-rule", config.getRules().get(0).getId());
    }

    @Test
    @DisplayName("Should load YAML without runtime-scripts (backward compat)")
    void testBackwardCompatNoRuntimeScripts() throws Exception {
        String yaml = """
                rules:
                  - id: "simple-rule"
                    condition: "#amount > 100"
                    message: "High amount"
                    severity: "WARNING"
                """;

        YamlRuleConfiguration config = yamlMapper.readValue(yaml, YamlRuleConfiguration.class);

        assertNull(config.getRuntimeScripts(), "runtime-scripts should be null when not present");
        assertNotNull(config.getRules());
        assertEquals(1, config.getRules().size());
    }

    @Test
    @DisplayName("Should use default values for optional fields")
    void testDefaultValues() throws Exception {
        String yaml = """
                runtime-scripts:
                  locations:
                    - "./scripts"
                """;

        YamlRuleConfiguration config = yamlMapper.readValue(yaml, YamlRuleConfiguration.class);
        YamlRuntimeScriptConfig scripts = config.getRuntimeScripts();

        assertNotNull(scripts);
        assertTrue(scripts.isEnabled(), "enabled should default to true");
        assertEquals("groovy", scripts.getEngine(), "engine should default to groovy");
        assertEquals(5000, scripts.getPollingIntervalMs(), "polling-interval-ms should default to 5000");
        assertEquals(200, scripts.getExecutionTimeoutMs(), "execution-timeout-ms should default to 200");
        assertEquals("use-last-good", scripts.getFailMode(), "fail-mode should default to use-last-good");
        assertNull(scripts.getAllowlist(), "allowlist should be null when not specified");
    }

    @Test
    @DisplayName("ScriptMetadata record should hold all fields")
    void testScriptMetadataRecord() {
        ScriptMetadata meta = new ScriptMetadata(
                "risk-score",
                Path.of("/scripts/risk-score.groovy"),
                "abc123def456",
                1700000000000L,
                true,
                1
        );

        assertEquals("risk-score", meta.id());
        assertEquals(Path.of("/scripts/risk-score.groovy"), meta.path());
        assertEquals("abc123def456", meta.checksum());
        assertEquals(1700000000000L, meta.lastModified());
        assertTrue(meta.enabled());
        assertEquals(1, meta.version());
    }

    @Test
    @DisplayName("ScriptMetadata withUpdated should increment version and update fields")
    void testScriptMetadataWithUpdated() {
        ScriptMetadata original = new ScriptMetadata(
                "risk-score",
                Path.of("/scripts/risk-score.groovy"),
                "abc123",
                1700000000000L,
                true,
                1
        );

        ScriptMetadata updated = original.withUpdated("def456", 1700001000000L);

        assertEquals("risk-score", updated.id());
        assertEquals(original.path(), updated.path());
        assertEquals("def456", updated.checksum());
        assertEquals(1700001000000L, updated.lastModified());
        assertTrue(updated.enabled());
        assertEquals(2, updated.version(), "version should be incremented");
    }
}
