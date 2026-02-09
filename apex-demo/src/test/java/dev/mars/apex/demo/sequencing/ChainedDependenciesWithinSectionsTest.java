package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests chained dependencies within sections:
 * - E1 -> E2 (enrichment chain within enrichments section)
 * - R1 -> R2 (rule chain within rules section)
 *
 * This verifies that items WITHIN a section execute in document order.
 */
public class ChainedDependenciesWithinSectionsTest {

    private static final Logger LOGGER = Logger.getLogger(ChainedDependenciesWithinSectionsTest.class.getName());
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        yamlLoader = new YamlConfigurationLoader();
    }

    @Test
    public void testChainedEnrichmentsAndRules() throws Exception {
        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/ChainedDependenciesWithinSectionsTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", "trade-001");
        inputData.put("amount", 75000);
        inputData.put("currency", "USD");

        // Act
        RuleResult result = engine.evaluate(config, inputData);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Overall result should be success");

        Map<String, Object> resultData = result.getEnrichedData();

        // Verify E1 executed: riskScore should be calculated
        assertTrue(resultData.containsKey("riskScore"), "riskScore should be present (from E1)");
        assertEquals(0.75, resultData.get("riskScore"), "riskScore should be 75000/100000 = 0.75");

        // Verify E2 executed: approvalLevel should be determined based on riskScore
        assertTrue(resultData.containsKey("approvalLevel"), "approvalLevel should be present (from E2, depends on E1)");
        assertEquals("STANDARD", resultData.get("approvalLevel"), "approvalLevel should be STANDARD (0.3 <= 0.75 < 0.8)");

        // Verify R1 and R2 executed: both should trigger
        assertTrue(result.isTriggered(), "Rules should trigger");

        LOGGER.info("Chained dependencies test PASSED");
        LOGGER.info("   E1 (calculate-risk-score) -> E2 (determine-approval-level)");
        LOGGER.info("   R1 (validate-risk-score) -> R2 (validate-approval-level)");
        LOGGER.info("   riskScore: " + resultData.get("riskScore"));
        LOGGER.info("   approvalLevel: " + resultData.get("approvalLevel"));
    }
}

