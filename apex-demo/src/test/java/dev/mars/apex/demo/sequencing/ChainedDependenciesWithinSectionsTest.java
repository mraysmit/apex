package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.result.RuleResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests chained dependencies within sections:
 * - E1 -> E2 (enrichment chain within enrichments section)
 * - R1 -> R2 (rule chain within rules section)
 * 
 * This verifies that items WITHIN a section execute in document order.
 */
public class ChainedDependenciesWithinSectionsTest {

    @Test
    public void testChainedEnrichmentsAndRules() {
        // Arrange
        String yamlFile = "apex-demo/src/test/java/dev/mars/apex/demo/sequencing/ChainedDependenciesWithinSectionsTest.yaml";
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", "trade-001");
        inputData.put("amount", 75000);
        inputData.put("currency", "USD");

        // Act
        RulesEngine engine = new RulesEngine();
        RuleResult result = engine.evaluateFromYamlFile(yamlFile, inputData);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Overall result should be success");

        Map<String, Object> resultData = result.getData();
        
        // Verify E1 executed: riskScore should be calculated
        assertTrue(resultData.containsKey("riskScore"), "riskScore should be present (from E1)");
        assertEquals(0.75, resultData.get("riskScore"), "riskScore should be 75000/100000 = 0.75");
        
        // Verify E2 executed: approvalLevel should be determined based on riskScore
        assertTrue(resultData.containsKey("approvalLevel"), "approvalLevel should be present (from E2, depends on E1)");
        assertEquals("STANDARD", resultData.get("approvalLevel"), "approvalLevel should be STANDARD (0.3 <= 0.75 < 0.8)");
        
        // Verify R1 executed: should validate riskScore
        assertTrue(result.getMessages().stream()
            .anyMatch(msg -> msg.contains("Risk score is acceptable")),
            "R1 validation message should be present");
        
        // Verify R2 executed: should validate approvalLevel
        assertTrue(result.getMessages().stream()
            .anyMatch(msg -> msg.contains("Approval level determined successfully")),
            "R2 validation message should be present (depends on E2)");
        
        System.out.println("✅ Chained dependencies test PASSED");
        System.out.println("   E1 (calculate-risk-score) -> E2 (determine-approval-level)");
        System.out.println("   R1 (validate-risk-score) -> R2 (validate-approval-level)");
        System.out.println("   riskScore: " + resultData.get("riskScore"));
        System.out.println("   approvalLevel: " + resultData.get("approvalLevel"));
    }
}

