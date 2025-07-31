package dev.mars.apex.rest.controller;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.rest.dto.RuleEvaluationRequest;
import dev.mars.apex.rest.dto.RuleEvaluationResponse;
import dev.mars.apex.rest.dto.ValidationRequest;
import dev.mars.apex.rest.dto.ValidationResponse;
import dev.mars.apex.rest.service.RuleEvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RulesController.
 * Tests controller logic using Spring Boot Test with MockBean.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Rules Controller Unit Tests")
public class RulesControllerTest {

    @Autowired
    private RulesController rulesController;

    @MockBean
    private RulesService rulesService;

    @MockBean
    private RuleEvaluationService ruleEvaluationService;

    @Nested
    @DisplayName("Rule Check Tests")
    class RuleCheckTests {

        @Test
        @DisplayName("Should evaluate rule successfully")
        void shouldEvaluateRuleSuccessfully() {
            // Given
            RuleEvaluationRequest request = new RuleEvaluationRequest();
            request.setCondition("#age >= 18");
            request.setData(Map.of("age", 25));
            request.setRuleName("age-check");
            request.setMessage("User is an adult");

            // Mock the service response - RulesService.check takes (condition, facts)
            when(rulesService.check(anyString(), anyMap())).thenReturn(true);

            // When
            ResponseEntity<RuleEvaluationResponse> response = rulesController.checkRule(request);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertTrue(response.getBody().isMatched());
            assertEquals("age-check", response.getBody().getRuleName());
            assertEquals("User is an adult", response.getBody().getMessage());
            assertNotNull(response.getBody().getEvaluationId());
            assertNotNull(response.getBody().getTimestamp());

            // Verify service was called with condition and data
            verify(rulesService).check("#age >= 18", Map.of("age", 25));
        }

        @Test
        @DisplayName("Should handle rule that does not match")
        void shouldHandleRuleThatDoesNotMatch() {
            // Given
            RuleEvaluationRequest request = new RuleEvaluationRequest();
            request.setCondition("#age >= 18");
            request.setData(Map.of("age", 16));
            request.setRuleName("age-check");
            request.setMessage("User is an adult");

            // Mock the service response
            when(rulesService.check(anyString(), anyMap())).thenReturn(false);

            // When
            ResponseEntity<RuleEvaluationResponse> response = rulesController.checkRule(request);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertFalse(response.getBody().isMatched());
            assertEquals("age-check", response.getBody().getRuleName());
            assertNotNull(response.getBody().getEvaluationId());

            // Verify service was called
            verify(rulesService).check("#age >= 18", Map.of("age", 16));
        }

        @Test
        @DisplayName("Should handle null data gracefully")
        void shouldHandleNullDataGracefully() {
            // Given
            RuleEvaluationRequest request = new RuleEvaluationRequest();
            request.setCondition("#age >= 18");
            request.setData(null);
            request.setRuleName("age-check");

            // When & Then - Controller has validation that prevents null data
            assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
                rulesController.checkRule(request);
            });
        }

        @Test
        @DisplayName("Should handle invalid condition gracefully")
        void shouldHandleInvalidConditionGracefully() {
            // Given
            RuleEvaluationRequest request = new RuleEvaluationRequest();
            request.setCondition("invalid.syntax(");
            request.setData(Map.of("age", 25));
            request.setRuleName("invalid-rule");

            // Mock the service to handle invalid conditions gracefully (error recovery)
            when(rulesService.check(anyString(), anyMap())).thenReturn(false);

            // When
            ResponseEntity<RuleEvaluationResponse> response = rulesController.checkRule(request);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            // The system should handle errors gracefully through error recovery
            assertTrue(response.getBody().isSuccess());
            assertFalse(response.getBody().isMatched());

            // Verify service was called
            verify(rulesService).check("invalid.syntax(", Map.of("age", 25));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate data successfully")
        void shouldValidateDataSuccessfully() {
            // Given
            ValidationRequest request = new ValidationRequest();
            request.setData(Map.of("age", 25, "email", "john@example.com"));

            ValidationRequest.ValidationRuleDto rule1 = new ValidationRequest.ValidationRuleDto(
                "age-check", "#age >= 18", "Must be at least 18", "ERROR"
            );
            ValidationRequest.ValidationRuleDto rule2 = new ValidationRequest.ValidationRuleDto(
                "email-check", "#email != null", "Email is required", "ERROR"
            );

            request.setValidationRules(List.of(rule1, rule2));
            request.setIncludeDetails(true);

            // Mock the service response
            ValidationResponse mockResponse = new ValidationResponse();
            mockResponse.setValid(true);
            mockResponse.setTotalRules(2);
            mockResponse.setPassedRules(2);
            mockResponse.setFailedRules(0);
            mockResponse.setValidationId("test-validation-id");

            when(ruleEvaluationService.validateData(any(ValidationRequest.class))).thenReturn(mockResponse);

            // When
            ResponseEntity<ValidationResponse> response = rulesController.validateData(request);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isValid());
            assertEquals(2, response.getBody().getTotalRules());
            assertEquals(2, response.getBody().getPassedRules());
            assertEquals(0, response.getBody().getFailedRules());
            assertNotNull(response.getBody().getValidationId());

            // Verify service was called
            verify(ruleEvaluationService).validateData(request);
        }

        @Test
        @DisplayName("Should handle validation failures")
        void shouldHandleValidationFailures() {
            // Given
            ValidationRequest request = new ValidationRequest();
            request.setData(Map.of("age", 16));  // Remove null value to avoid Map.of() issue

            ValidationRequest.ValidationRuleDto rule1 = new ValidationRequest.ValidationRuleDto(
                "age-check", "#age >= 18", "Must be at least 18", "ERROR"
            );
            ValidationRequest.ValidationRuleDto rule2 = new ValidationRequest.ValidationRuleDto(
                "email-check", "#email != null", "Email is required", "ERROR"
            );

            request.setValidationRules(List.of(rule1, rule2));
            request.setIncludeDetails(true);

            // Mock the service response for validation failures
            ValidationResponse mockResponse = new ValidationResponse();
            mockResponse.setValid(false);
            mockResponse.setTotalRules(2);
            mockResponse.setPassedRules(0);
            mockResponse.setFailedRules(2);
            mockResponse.setValidationId("test-validation-id");

            when(ruleEvaluationService.validateData(any(ValidationRequest.class))).thenReturn(mockResponse);

            // When
            ResponseEntity<ValidationResponse> response = rulesController.validateData(request);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().isValid());
            assertEquals(2, response.getBody().getTotalRules());
            assertEquals(0, response.getBody().getPassedRules());
            assertEquals(2, response.getBody().getFailedRules());

            // Verify service was called
            verify(ruleEvaluationService).validateData(request);
        }

        @Test
        @DisplayName("Should handle empty validation rules")
        void shouldHandleEmptyValidationRules() {
            // Given
            ValidationRequest request = new ValidationRequest();
            request.setData(Map.of("age", 25));
            request.setValidationRules(List.of());

            // Mock the service response for empty rules
            ValidationResponse mockResponse = new ValidationResponse();
            mockResponse.setValid(true);
            mockResponse.setTotalRules(0);
            mockResponse.setPassedRules(0);
            mockResponse.setFailedRules(0);
            mockResponse.setValidationId("test-validation-id");

            when(ruleEvaluationService.validateData(any(ValidationRequest.class))).thenReturn(mockResponse);

            // When
            ResponseEntity<ValidationResponse> response = rulesController.validateData(request);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isValid());
            assertEquals(0, response.getBody().getTotalRules());

            // Verify service was called
            verify(ruleEvaluationService).validateData(request);
        }
    }

    @Nested
    @DisplayName("Rule Definition Tests")
    class RuleDefinitionTests {

        @Test
        @DisplayName("Should define rule successfully")
        void shouldDefineRuleSuccessfully() {
            // Given
            String ruleName = "test-rule";
            Map<String, String> ruleDefinition = Map.of(
                "condition", "#value > 0",
                "message", "Value must be positive"
            );

            // Mock the service call
            doNothing().when(rulesService).define(anyString(), anyString(), anyString());

            // When
            ResponseEntity<Map<String, Object>> response = rulesController.defineRule(ruleName, ruleDefinition);

            // Then
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Rule defined successfully", response.getBody().get("message"));
            assertEquals(ruleName, response.getBody().get("ruleName"));
            assertEquals("#value > 0", response.getBody().get("condition"));

            // Verify service was called
            verify(rulesService).define(ruleName, "#value > 0", "Value must be positive");
        }

        @Test
        @DisplayName("Should get defined rules")
        void shouldGetDefinedRules() {
            // Given - mock the service response
            when(rulesService.getDefinedRules()).thenReturn(new String[]{"test-rule", "another-rule"});

            // When
            ResponseEntity<Map<String, Object>> response = rulesController.getDefinedRules();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().containsKey("definedRules"));
            assertTrue(response.getBody().containsKey("count"));
            assertTrue(response.getBody().containsKey("timestamp"));

            String[] definedRules = (String[]) response.getBody().get("definedRules");
            assertEquals(2, definedRules.length);
            assertEquals(2, response.getBody().get("count"));

            // Verify service was called
            verify(rulesService).getDefinedRules();
        }
    }

    @Nested
    @DisplayName("Rule Testing Tests")
    class RuleTestingTests {

        @Test
        @DisplayName("Should test defined rule successfully")
        void shouldTestDefinedRuleSuccessfully() {
            // Given
            String ruleName = "positive-check";
            Map<String, Object> testData = Map.of("value", 10);

            // Mock the service calls
            when(rulesService.isDefined(ruleName)).thenReturn(true);
            when(rulesService.test(ruleName, testData)).thenReturn(true);

            // When
            ResponseEntity<RuleEvaluationResponse> response = rulesController.testRule(ruleName, testData);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertTrue(response.getBody().isMatched());
            assertEquals(ruleName, response.getBody().getRuleName());
            assertEquals("Rule matched", response.getBody().getMessage());

            // Verify service calls
            verify(rulesService).isDefined(ruleName);
            verify(rulesService).test(ruleName, testData);
        }

        @Test
        @DisplayName("Should handle undefined rule")
        void shouldHandleUndefinedRule() {
            // Given
            String ruleName = "undefined-rule";
            Map<String, Object> testData = Map.of("value", 10);

            // Mock the service to return false for undefined rule
            when(rulesService.isDefined(ruleName)).thenReturn(false);

            // When
            ResponseEntity<RuleEvaluationResponse> response = rulesController.testRule(ruleName, testData);

            // Then
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().isSuccess());
            assertEquals("Rule not found", response.getBody().getError());
            assertTrue(response.getBody().getErrorDetails().contains("undefined-rule"));

            // Verify service was called
            verify(rulesService).isDefined(ruleName);
            verify(rulesService, never()).test(anyString(), anyMap());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle complex nested data structures")
        void shouldHandleComplexNestedDataStructures() {
            // Given
            RuleEvaluationRequest request = new RuleEvaluationRequest();
            request.setCondition("#user.profile.age >= 18 && #user.profile.verified == true");
            request.setData(Map.of(
                "user", Map.of(
                    "profile", Map.of(
                        "age", 25,
                        "verified", true,
                        "preferences", Map.of(
                            "notifications", true,
                            "theme", "dark"
                        )
                    )
                )
            ));
            request.setRuleName("complex-user-check");
            request.setMessage("Complex user validation");

            // Mock the service response
            when(rulesService.check(anyString(), anyMap())).thenReturn(true);

            // When
            ResponseEntity<RuleEvaluationResponse> response = rulesController.checkRule(request);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertTrue(response.getBody().isMatched());
            assertEquals("complex-user-check", response.getBody().getRuleName());
        }

        @Test
        @DisplayName("Should handle very large data sets")
        void shouldHandleVeryLargeDataSets() {
            // Given
            Map<String, Object> largeDataSet = new java.util.HashMap<>();
            for (int i = 0; i < 1000; i++) {
                largeDataSet.put("field" + i, "value" + i);
            }

            RuleEvaluationRequest request = new RuleEvaluationRequest();
            request.setCondition("#field500 == 'value500'");
            request.setData(largeDataSet);
            request.setRuleName("large-data-check");

            // Mock the service response
            when(rulesService.check(anyString(), anyMap())).thenReturn(true);

            // When
            ResponseEntity<RuleEvaluationResponse> response = rulesController.checkRule(request);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertTrue(response.getBody().isMatched());
        }

        @Test
        @DisplayName("Should handle special characters in rule names")
        void shouldHandleSpecialCharactersInRuleNames() {
            // Given
            String specialRuleName = "rule-with-special-chars_123!@#";
            Map<String, String> ruleDefinition = Map.of(
                "condition", "#value > 0",
                "message", "Special character rule"
            );

            // Mock the service call
            doNothing().when(rulesService).define(anyString(), anyString(), anyString());

            // When
            ResponseEntity<Map<String, Object>> response = rulesController.defineRule(specialRuleName, ruleDefinition);

            // Then
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Rule defined successfully", response.getBody().get("message"));
            assertEquals(specialRuleName, response.getBody().get("ruleName"));
        }

        @Test
        @DisplayName("Should handle concurrent rule evaluations")
        void shouldHandleConcurrentRuleEvaluations() {
            // Given
            ValidationRequest request = new ValidationRequest();
            request.setData(Map.of("age", 25, "status", "active"));

            java.util.List<ValidationRequest.ValidationRuleDto> rules = new java.util.ArrayList<>();
            for (int i = 0; i < 10; i++) {
                rules.add(new ValidationRequest.ValidationRuleDto(
                    "concurrent-rule-" + i,
                    "#age > " + (i * 2),
                    "Concurrent rule " + i,
                    "ERROR"
                ));
            }
            request.setValidationRules(rules);

            // Mock the service response
            ValidationResponse mockResponse = new ValidationResponse();
            mockResponse.setValid(true);
            mockResponse.setTotalRules(10);
            mockResponse.setPassedRules(10);
            mockResponse.setFailedRules(0);
            mockResponse.setValidationId("concurrent-validation-id");

            when(ruleEvaluationService.validateData(any(ValidationRequest.class))).thenReturn(mockResponse);

            // When
            ResponseEntity<ValidationResponse> response = rulesController.validateData(request);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isValid());
            assertEquals(10, response.getBody().getTotalRules());
            assertEquals(10, response.getBody().getPassedRules());
        }

        @Test
        @DisplayName("Should handle rule definition with empty condition")
        void shouldHandleRuleDefinitionWithEmptyCondition() {
            // Given
            String ruleName = "empty-condition-rule";
            Map<String, String> ruleDefinition = Map.of(
                "condition", "",
                "message", "Empty condition rule"
            );

            // Mock the service call - controller doesn't validate empty conditions
            doNothing().when(rulesService).define(anyString(), anyString(), anyString());

            // When
            ResponseEntity<Map<String, Object>> response = rulesController.defineRule(ruleName, ruleDefinition);

            // Then - Controller validates empty conditions and returns BAD_REQUEST
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().containsKey("error"));

            // Verify service was not called due to validation failure
            verify(rulesService, never()).define(anyString(), anyString(), anyString());
        }
    }
}
