package dev.mars.apex.rest.service;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.rest.dto.ValidationRequest;
import dev.mars.apex.rest.dto.ValidationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RuleEvaluationService using real objects and plain JUnit 5.
 * No mocking - uses actual RulesService instances.
 */
public class RuleEvaluationServiceTest {
    
    private RuleEvaluationService ruleEvaluationService;
    private RulesService rulesService;
    
    @BeforeEach
    void setUp() {
        rulesService = new RulesService();
        ruleEvaluationService = new RuleEvaluationService();
        
        // Use reflection to set the rulesService field since we can't use @Autowired in unit tests
        try {
            var field = RuleEvaluationService.class.getDeclaredField("rulesService");
            field.setAccessible(true);
            field.set(ruleEvaluationService, rulesService);
        } catch (Exception e) {
            fail("Failed to inject RulesService: " + e.getMessage());
        }
    }
    
    @Test
    public void testValidateData_AllRulesPass() {
        ValidationRequest request = new ValidationRequest();
        request.setData(Map.of("age", 25, "email", "john@example.com", "balance", 1000));
        
        ValidationRequest.ValidationRuleDto rule1 = new ValidationRequest.ValidationRuleDto(
            "age-check", "#age >= 18", "Must be at least 18", "ERROR"
        );
        ValidationRequest.ValidationRuleDto rule2 = new ValidationRequest.ValidationRuleDto(
            "email-check", "#email != null", "Email is required", "ERROR"
        );
        ValidationRequest.ValidationRuleDto rule3 = new ValidationRequest.ValidationRuleDto(
            "balance-check", "#balance > 0", "Balance must be positive", "ERROR"
        );
        
        request.setValidationRules(List.of(rule1, rule2, rule3));
        request.setIncludeDetails(true);
        
        ValidationResponse response = ruleEvaluationService.validateData(request);
        
        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals(3, response.getTotalRules());
        assertEquals(3, response.getPassedRules());
        assertEquals(0, response.getFailedRules());
        assertFalse(response.hasErrors());
        assertFalse(response.hasWarnings());
    }
    
    @Test
    public void testValidateData_SomeRulesFail() {
        // NOTE: This test intentionally uses data that fails validation to verify error handling
        // Expected: age=16 fails age>=18 rule, balance=-100 fails balance>0 rule
        ValidationRequest request = new ValidationRequest();
        request.setData(Map.of("age", 16, "email", "john@example.com", "balance", -100));
        
        ValidationRequest.ValidationRuleDto rule1 = new ValidationRequest.ValidationRuleDto(
            "age-check", "#age >= 18", "Must be at least 18", "ERROR"
        );
        ValidationRequest.ValidationRuleDto rule2 = new ValidationRequest.ValidationRuleDto(
            "email-check", "#email != null", "Email is required", "ERROR"
        );
        ValidationRequest.ValidationRuleDto rule3 = new ValidationRequest.ValidationRuleDto(
            "balance-check", "#balance > 0", "Balance must be positive", "ERROR"
        );
        
        request.setValidationRules(List.of(rule1, rule2, rule3));
        request.setIncludeDetails(true);
        
        ValidationResponse response = ruleEvaluationService.validateData(request);
        
        assertNotNull(response);
        assertFalse(response.isValid());
        assertEquals(3, response.getTotalRules());
        assertEquals(1, response.getPassedRules()); // Only email check passes
        assertEquals(2, response.getFailedRules()); // Age and balance fail
        assertTrue(response.hasErrors());
        assertEquals(2, response.getErrorCount());
    }
    
    @Test
    public void testValidateData_WithWarnings() {
        // NOTE: This test intentionally uses data that triggers warnings to verify warning handling
        // Expected: income=30000 fails income>=50000 rule (WARNING level)
        ValidationRequest request = new ValidationRequest();
        request.setData(Map.of("age", 25, "income", 30000));
        
        ValidationRequest.ValidationRuleDto rule1 = new ValidationRequest.ValidationRuleDto(
            "age-check", "#age >= 18", "Must be at least 18", "ERROR"
        );
        ValidationRequest.ValidationRuleDto rule2 = new ValidationRequest.ValidationRuleDto(
            "income-warning", "#income >= 50000", "Income below recommended level", "WARNING"
        );
        
        request.setValidationRules(List.of(rule1, rule2));
        request.setIncludeDetails(true);
        
        ValidationResponse response = ruleEvaluationService.validateData(request);
        
        assertNotNull(response);
        assertFalse(response.isValid()); // Should be false because warning rule failed
        assertEquals(2, response.getTotalRules());
        assertEquals(1, response.getPassedRules());
        assertEquals(1, response.getFailedRules());
        assertTrue(response.hasWarnings());
        assertEquals(1, response.getWarningCount());
    }
    
    @Test
    public void testValidateData_StopOnFirstFailure() {
        // NOTE: This test intentionally uses data that fails multiple validations to test stop-on-first-failure
        // Expected: Should stop after first validation failure (age=16 fails age>=18)
        ValidationRequest request = new ValidationRequest();
        request.setData(Map.of("age", 16, "email", "invalid", "balance", -100));
        
        ValidationRequest.ValidationRuleDto rule1 = new ValidationRequest.ValidationRuleDto(
            "age-check", "#age >= 18", "Must be at least 18", "ERROR"
        );
        ValidationRequest.ValidationRuleDto rule2 = new ValidationRequest.ValidationRuleDto(
            "email-check", "#email.contains('@')", "Email must contain @", "ERROR"
        );
        ValidationRequest.ValidationRuleDto rule3 = new ValidationRequest.ValidationRuleDto(
            "balance-check", "#balance > 0", "Balance must be positive", "ERROR"
        );
        
        request.setValidationRules(List.of(rule1, rule2, rule3));
        request.setStopOnFirstFailure(true);
        request.setIncludeDetails(true);
        
        ValidationResponse response = ruleEvaluationService.validateData(request);
        
        assertNotNull(response);
        assertFalse(response.isValid());
        assertEquals(3, response.getTotalRules());
        // Should stop after first failure, so only 1 error
        assertEquals(1, response.getErrorCount());
    }
    
    @Test
    public void testValidateData_EmptyRules() {
        ValidationRequest request = new ValidationRequest();
        request.setData(Map.of("age", 25));
        request.setValidationRules(List.of());
        
        ValidationResponse response = ruleEvaluationService.validateData(request);
        
        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals(0, response.getTotalRules());
        assertEquals(0, response.getPassedRules());
        assertEquals(0, response.getFailedRules());
    }
    
    @Test
    public void testValidateData_NullRules() {
        ValidationRequest request = new ValidationRequest();
        request.setData(Map.of("age", 25));
        request.setValidationRules(null);
        
        ValidationResponse response = ruleEvaluationService.validateData(request);
        
        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals(0, response.getTotalRules());
        assertEquals(0, response.getPassedRules());
        assertEquals(0, response.getFailedRules());
    }
    
    @Test
    public void testEvaluateRule_Success() {
        String condition = "#value > 100";
        Map<String, Object> data = Map.of("value", 150);
        
        var response = ruleEvaluationService.evaluateRule(condition, data, false);
        
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.isMatched());
        assertEquals("rule-evaluation", response.getRuleName());
        assertEquals("Rule matched", response.getMessage());
        assertNull(response.getMetrics()); // Not requested
    }
    
    @Test
    public void testEvaluateRule_WithMetrics() {
        String condition = "#value > 100";
        Map<String, Object> data = Map.of("value", 150);
        
        var response = ruleEvaluationService.evaluateRule(condition, data, true);
        
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.isMatched());
        assertNotNull(response.getMetrics());
        assertTrue(response.getMetrics().getEvaluationTimeMs() >= 0);
        assertTrue(response.getMetrics().isSuccessful());
    }
    
    @Test
    public void testEvaluateRule_InvalidCondition() {
        // NOTE: This test intentionally uses an invalid condition to verify graceful error handling
        // Expected: Rules Engine handles missing parameters gracefully without throwing exceptions
        String condition = "#invalid.syntax.here";
        Map<String, Object> data = Map.of("value", 150);

        var response = ruleEvaluationService.evaluateRule(condition, data, true);

        assertNotNull(response);
        // The Rules Engine handles invalid conditions gracefully
        assertTrue(response.isSuccess());
        assertFalse(response.isMatched()); // Should not match due to missing parameter
        assertNotNull(response.getMetrics());
        assertTrue(response.getMetrics().isSuccessful());
    }
    
    @Test
    public void testQuickCheck_Success() {
        String condition = "#age >= 18";
        Map<String, Object> data = Map.of("age", 25);
        
        boolean result = ruleEvaluationService.quickCheck(condition, data);
        
        assertTrue(result);
    }
    
    @Test
    public void testQuickCheck_Failure() {
        // NOTE: This test intentionally uses data that fails validation to verify failure handling
        // Expected: age=16 fails age>=18 condition, should return false
        String condition = "#age >= 18";
        Map<String, Object> data = Map.of("age", 16);

        boolean result = ruleEvaluationService.quickCheck(condition, data);
        
        assertFalse(result);
    }
    
    @Test
    public void testQuickCheck_InvalidCondition() {
        // NOTE: This test intentionally uses an invalid condition to verify error handling
        // Expected: Should return false on error (graceful degradation)
        String condition = "#invalid.syntax";
        Map<String, Object> data = Map.of("age", 25);

        boolean result = ruleEvaluationService.quickCheck(condition, data);
        
        assertFalse(result); // Should return false on error
    }
    
    @Test
    public void testGetSystemInfo() {
        Map<String, Object> info = ruleEvaluationService.getSystemInfo();
        
        assertNotNull(info);
        assertTrue(info.containsKey("totalMemory"));
        assertTrue(info.containsKey("freeMemory"));
        assertTrue(info.containsKey("usedMemory"));
        assertTrue(info.containsKey("maxMemory"));
        assertTrue(info.containsKey("availableProcessors"));
        assertTrue(info.containsKey("timestamp"));
        assertTrue(info.containsKey("definedRulesCount"));
        assertTrue(info.containsKey("definedRules"));
        
        // Verify data types
        assertTrue(info.get("totalMemory") instanceof Long);
        assertTrue(info.get("definedRulesCount") instanceof Integer);
        assertTrue(info.get("definedRules") instanceof String[]);
    }
}
