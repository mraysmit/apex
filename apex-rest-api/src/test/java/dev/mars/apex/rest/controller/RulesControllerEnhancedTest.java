package dev.mars.apex.rest.controller;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for enhanced RulesController functionality.
 * Tests the new rule execution and batch processing endpoints.
 */
@ExtendWith(MockitoExtension.class)
class RulesControllerEnhancedTest {

    @Mock
    private RulesEngine rulesEngine;

    @InjectMocks
    private RulesController rulesController;

    private Map<String, Object> testFacts;
    private RulesController.RuleDto testRuleDto;
    private Rule testRule;
    private RuleResult testRuleResult;

    @BeforeEach
    void setUp() {
        testFacts = new HashMap<>();
        testFacts.put("amount", 1500.0);
        testFacts.put("currency", "USD");
        testFacts.put("customerTier", "GOLD");
        testFacts.put("riskScore", 75);

        testRuleDto = new RulesController.RuleDto();
        testRuleDto.setName("high-value-transaction");
        testRuleDto.setCondition("#amount > 1000 && #currency == 'USD'");
        testRuleDto.setMessage("High value USD transaction detected");

        testRule = new Rule(
            testRuleDto.getName(),
            testRuleDto.getCondition(),
            testRuleDto.getMessage()
        );

        testRuleResult = RuleResult.match("high-value-transaction", "High value USD transaction detected");
    }

    @Test
    @DisplayName("Should execute single rule successfully")
    void testExecuteRuleSuccess() {
        // Arrange
        RulesController.RuleExecutionRequest request = new RulesController.RuleExecutionRequest();
        request.setRule(testRuleDto);
        request.setFacts(testFacts);

        when(rulesEngine.executeRule(any(Rule.class), eq(testFacts)))
            .thenReturn(testRuleResult);

        // Act
        ResponseEntity<Map<String, Object>> response = rulesController.executeRule(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(testFacts, responseBody.get("facts"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
        assertTrue((Boolean) result.get("triggered"));
        assertEquals("high-value-transaction", result.get("ruleName"));
        assertEquals("High value USD transaction detected", result.get("message"));

        verify(rulesEngine).executeRule(any(Rule.class), eq(testFacts));
    }

    @Test
    @DisplayName("Should handle rule execution error")
    void testExecuteRuleError() {
        // Arrange
        RulesController.RuleExecutionRequest request = new RulesController.RuleExecutionRequest();
        request.setRule(testRuleDto);
        request.setFacts(testFacts);

        when(rulesEngine.executeRule(any(Rule.class), eq(testFacts)))
            .thenThrow(new RuntimeException("Rule execution failed"));

        // Act
        ResponseEntity<Map<String, Object>> response = rulesController.executeRule(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Rule execution failed", responseBody.get("error"));
        assertTrue(responseBody.get("message").toString().contains("Rule execution failed"));
    }

    @Test
    @DisplayName("Should execute batch rules successfully")
    void testExecuteBatchRulesSuccess() {
        // Arrange
        List<RulesController.RuleDto> rules = Arrays.asList(
            new RulesController.RuleDto("high-value", "#amount > 1000", "High value transaction"),
            new RulesController.RuleDto("gold-customer", "#customerTier == 'GOLD'", "Gold tier customer"),
            new RulesController.RuleDto("low-risk", "#riskScore < 80", "Low risk transaction")
        );

        RulesController.BatchRuleExecutionRequest request = new RulesController.BatchRuleExecutionRequest();
        request.setRules(rules);
        request.setFacts(testFacts);

        // Mock results - 2 triggered, 1 not triggered
        when(rulesEngine.executeRule(any(Rule.class), eq(testFacts)))
            .thenReturn(RuleResult.match("high-value", "High value transaction"))
            .thenReturn(RuleResult.match("gold-customer", "Gold tier customer"))
            .thenReturn(RuleResult.noMatch());

        // Act
        ResponseEntity<Map<String, Object>> response = rulesController.executeBatchRules(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(3, responseBody.get("totalRules"));
        assertEquals(2, responseBody.get("triggeredRules"));
        assertEquals(testFacts, responseBody.get("facts"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");
        assertEquals(3, results.size());

        // Verify first two rules triggered
        assertTrue((Boolean) results.get(0).get("triggered"));
        assertTrue((Boolean) results.get(1).get("triggered"));
        assertFalse((Boolean) results.get(2).get("triggered"));

        verify(rulesEngine, times(3)).executeRule(any(Rule.class), eq(testFacts));
    }

    @Test
    @DisplayName("Should handle batch rule execution with some failures")
    void testExecuteBatchRulesPartialFailure() {
        // Arrange
        List<RulesController.RuleDto> rules = Arrays.asList(
            new RulesController.RuleDto("good-rule", "#amount > 0", "Valid rule"),
            new RulesController.RuleDto("bad-rule", "invalid syntax", "Invalid rule")
        );

        RulesController.BatchRuleExecutionRequest request = new RulesController.BatchRuleExecutionRequest();
        request.setRules(rules);
        request.setFacts(testFacts);

        when(rulesEngine.executeRule(any(Rule.class), eq(testFacts)))
            .thenReturn(RuleResult.match("good-rule", "Valid rule"))
            .thenThrow(new RuntimeException("Invalid rule syntax"));

        // Act
        ResponseEntity<Map<String, Object>> response = rulesController.executeBatchRules(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Batch rule execution failed", responseBody.get("error"));
    }

    @Test
    @DisplayName("Should validate RuleExecutionRequest DTO")
    void testRuleExecutionRequestDto() {
        // Test default constructor
        RulesController.RuleExecutionRequest request1 = new RulesController.RuleExecutionRequest();
        assertNotNull(request1);

        // Test setters and getters
        request1.setRule(testRuleDto);
        request1.setFacts(testFacts);

        assertEquals(testRuleDto, request1.getRule());
        assertEquals(testFacts, request1.getFacts());
    }

    @Test
    @DisplayName("Should validate BatchRuleExecutionRequest DTO")
    void testBatchRuleExecutionRequestDto() {
        // Test default constructor
        RulesController.BatchRuleExecutionRequest request1 = new RulesController.BatchRuleExecutionRequest();
        assertNotNull(request1);

        // Test setters and getters
        List<RulesController.RuleDto> rules = Arrays.asList(testRuleDto);
        request1.setRules(rules);
        request1.setFacts(testFacts);

        assertEquals(rules, request1.getRules());
        assertEquals(testFacts, request1.getFacts());
    }

    @Test
    @DisplayName("Should validate RuleDto creation and properties")
    void testRuleDtoValidation() {
        // Test default constructor
        RulesController.RuleDto rule1 = new RulesController.RuleDto();
        assertNotNull(rule1);

        // Test parameterized constructor
        RulesController.RuleDto rule2 = new RulesController.RuleDto("test-rule", "#value > 0", "Test rule");
        assertEquals("test-rule", rule2.getName());
        assertEquals("#value > 0", rule2.getCondition());
        assertEquals("Test rule", rule2.getMessage());

        // Test setters
        rule1.setName("another-rule");
        rule1.setCondition("#amount < 100");
        rule1.setMessage("Low amount rule");
        rule1.setPriority("HIGH");

        assertEquals("another-rule", rule1.getName());
        assertEquals("#amount < 100", rule1.getCondition());
        assertEquals("Low amount rule", rule1.getMessage());
        assertEquals("HIGH", rule1.getPriority());
    }

    @Test
    @DisplayName("Should execute rule with no facts")
    void testExecuteRuleWithEmptyFacts() {
        // Arrange
        RulesController.RuleExecutionRequest request = new RulesController.RuleExecutionRequest();
        request.setRule(testRuleDto);
        request.setFacts(new HashMap<>());

        when(rulesEngine.executeRule(any(Rule.class), eq(new HashMap<>())))
            .thenReturn(RuleResult.noMatch());

        // Act
        ResponseEntity<Map<String, Object>> response = rulesController.executeRule(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
        assertFalse((Boolean) result.get("triggered"));
    }

    @Test
    @DisplayName("Should handle batch execution with empty rules list")
    void testExecuteBatchRulesEmpty() {
        // Arrange
        RulesController.BatchRuleExecutionRequest request = new RulesController.BatchRuleExecutionRequest();
        request.setRules(new ArrayList<>());
        request.setFacts(testFacts);

        // Act
        ResponseEntity<Map<String, Object>> response = rulesController.executeBatchRules(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(0, responseBody.get("totalRules"));
        assertEquals(0, responseBody.get("triggeredRules"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");
        assertTrue(results.isEmpty());

        verifyNoInteractions(rulesEngine);
    }

    @Test
    @DisplayName("Should execute rule with complex facts structure")
    void testExecuteRuleWithComplexFacts() {
        // Arrange
        Map<String, Object> complexFacts = new HashMap<>();
        complexFacts.put("customer", Map.of(
            "id", "CUST001",
            "tier", "GOLD",
            "profile", Map.of("age", 35, "country", "US")
        ));
        complexFacts.put("transaction", Map.of(
            "amount", 2500.0,
            "currency", "USD",
            "type", "TRANSFER"
        ));

        RulesController.RuleDto complexRule = new RulesController.RuleDto();
        complexRule.setName("complex-rule");
        complexRule.setCondition("#customer.tier == 'GOLD' && #transaction.amount > 2000");
        complexRule.setMessage("High value transaction for gold customer");

        RulesController.RuleExecutionRequest request = new RulesController.RuleExecutionRequest();
        request.setRule(complexRule);
        request.setFacts(complexFacts);

        when(rulesEngine.executeRule(any(Rule.class), eq(complexFacts)))
            .thenReturn(RuleResult.match("complex-rule", "High value transaction for gold customer"));

        // Act
        ResponseEntity<Map<String, Object>> response = rulesController.executeRule(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(complexFacts, responseBody.get("facts"));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
        assertTrue((Boolean) result.get("triggered"));
        assertEquals("complex-rule", result.get("ruleName"));

        verify(rulesEngine).executeRule(any(Rule.class), eq(complexFacts));
    }

    @Test
    @DisplayName("Should handle rule execution with null result")
    void testExecuteRuleWithNullResult() {
        // Arrange
        RulesController.RuleExecutionRequest request = new RulesController.RuleExecutionRequest();
        request.setRule(testRuleDto);
        request.setFacts(testFacts);

        when(rulesEngine.executeRule(any(Rule.class), eq(testFacts)))
            .thenReturn(null);

        // Act
        ResponseEntity<Map<String, Object>> response = rulesController.executeRule(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Rule execution failed", responseBody.get("error"));
    }

    @Test
    @DisplayName("Should execute batch rules with mixed result types")
    void testExecuteBatchRulesWithMixedResults() {
        // Arrange
        List<RulesController.RuleDto> rules = Arrays.asList(
            new RulesController.RuleDto("boolean-rule", "#amount > 1000", "Boolean result"),
            new RulesController.RuleDto("string-rule", "#currency", "String result"),
            new RulesController.RuleDto("numeric-rule", "#amount * 0.1", "Numeric result")
        );

        RulesController.BatchRuleExecutionRequest request = new RulesController.BatchRuleExecutionRequest();
        request.setRules(rules);
        request.setFacts(testFacts);

        when(rulesEngine.executeRule(any(Rule.class), eq(testFacts)))
            .thenReturn(RuleResult.match("boolean-rule", "Boolean result"))
            .thenReturn(RuleResult.match("string-rule", "String result"))
            .thenReturn(RuleResult.match("numeric-rule", "Numeric result"));

        // Act
        ResponseEntity<Map<String, Object>> response = rulesController.executeBatchRules(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(3, responseBody.get("totalRules"));
        assertEquals(3, responseBody.get("triggeredRules"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");
        assertEquals(3, results.size());

        // All rules should have triggered
        for (Map<String, Object> result : results) {
            assertTrue((Boolean) result.get("triggered"));
            assertNotNull(result.get("ruleName"));
            assertNotNull(result.get("message"));
        }

        verify(rulesEngine, times(3)).executeRule(any(Rule.class), eq(testFacts));
    }
}
