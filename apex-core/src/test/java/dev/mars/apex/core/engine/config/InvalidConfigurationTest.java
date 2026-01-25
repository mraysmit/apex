package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeAll;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
public class InvalidConfigurationTest {

    private static final Logger logger = LoggerFactory.getLogger(InvalidConfigurationTest.class);

    @TempDir
    Path tempDir;

    @BeforeAll
    static void setupClass() {
        logger.info("[TEST-EXPECTED-ERROR] InvalidConfigurationTest - tests invalid/missing config that produce WARN/ERROR");
    }

    @Test
    public void testInvalidDataSourceConfiguration() throws Exception {
        logger.info("========== START OF INTENTIONAL ERROR TEST ==========");
            // Create a YAML file with invalid data source configuration
            String yamlContent = 
                "metadata:\n" +
                "  type: \"rule-config\"\n" +
                "  version: \"1.0\"\n" +
                "  name: \"invalid-config-test\"\n" +
                "\n" +
                "data-sources:\n" +
                "  - name: \"invalid-db\"\n" +
                "    type: \"database\"\n" +
                "    # Missing connection block which is required for database type\n";

            File yamlFile = tempDir.resolve("invalid-config.yaml").toFile();
            Files.writeString(yamlFile.toPath(), yamlContent);

            // Load the engine
            RulesEngine engine = RulesEngine.fromFile(yamlFile.getAbsolutePath());

            // Evaluate
            RuleResult result = engine.evaluate(new HashMap<>());

            // Verify failure
            assertFalse(result.isSuccess(), "Evaluation should fail due to initialization errors");
            assertEquals(RuleResult.ResultType.ERROR, result.getResultType());
            
            // Verify error messages
            List<String> failures = result.getFailureMessages();
            assertFalse(failures.isEmpty(), "Should have failure messages");
            
            boolean foundExpectedError = false;
            for (String msg : failures) {
                if (msg.contains("Failed to initialize data source 'invalid-db'") && 
                    msg.contains("Connection configuration is required")) {
                    foundExpectedError = true;
                    break;
                }
            }
            
            assertTrue(foundExpectedError, "Should contain specific initialization error message. Found: " + failures);
        
        logger.info("========== END OF INTENTIONAL ERROR TEST ===========");
    }
}
