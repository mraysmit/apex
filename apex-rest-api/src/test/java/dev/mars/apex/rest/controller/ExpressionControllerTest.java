package dev.mars.apex.rest.controller;

import dev.mars.apex.core.service.expression.ExpressionEvaluationService;
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
 * Unit tests for ExpressionController.
 * Tests SpEL expression evaluation operations with mocked services for isolated testing.
 */
@ExtendWith(MockitoExtension.class)
class ExpressionControllerTest {

    @Mock
    private ExpressionEvaluationService expressionEvaluationService;

    @InjectMocks
    private ExpressionController expressionController;

    private Map<String, Object> testContext;
    private String testExpression;

    @BeforeEach
    void setUp() {
        testContext = new HashMap<>();
        testContext.put("amount", 1000.0);
        testContext.put("rate", 0.05);
        testContext.put("fee", 25.0);
        testContext.put("currency", "USD");
        testContext.put("age", 25);

        testExpression = "#amount * #rate + #fee";
    }

    @Test
    @DisplayName("Should evaluate expression successfully")
    void testEvaluateExpressionSuccess() {
        // Arrange
        ExpressionController.ExpressionEvaluationRequest request = new ExpressionController.ExpressionEvaluationRequest();
        request.setExpression(testExpression);
        request.setContext(testContext);

        Double expectedResult = 75.0; // 1000 * 0.05 + 25
        when(expressionEvaluationService.evaluate(eq(testExpression), eq(testContext)))
            .thenReturn(expectedResult);

        // Act
        ResponseEntity<Map<String, Object>> response = expressionController.evaluateExpression(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(testExpression, responseBody.get("expression"));
        assertEquals(testContext, responseBody.get("context"));
        assertEquals(expectedResult, responseBody.get("result"));
        assertEquals("Double", responseBody.get("resultType"));

        verify(expressionEvaluationService).evaluate(testExpression, testContext);
    }

    @Test
    @DisplayName("Should handle expression evaluation error")
    void testEvaluateExpressionError() {
        // Arrange
        ExpressionController.ExpressionEvaluationRequest request = new ExpressionController.ExpressionEvaluationRequest();
        request.setExpression("invalid expression");
        request.setContext(testContext);

        when(expressionEvaluationService.evaluate(eq("invalid expression"), eq(testContext)))
            .thenThrow(new RuntimeException("Invalid expression syntax"));

        // Act
        ResponseEntity<Map<String, Object>> response = expressionController.evaluateExpression(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Expression evaluation failed", responseBody.get("error"));
        assertEquals("invalid expression", responseBody.get("expression"));
        assertTrue(responseBody.get("message").toString().contains("Invalid expression syntax"));
    }

    @Test
    @DisplayName("Should evaluate expression with detailed result successfully")
    void testEvaluateExpressionWithDetailedResult() {
        // Arrange
        ExpressionController.ExpressionEvaluationRequest request = new ExpressionController.ExpressionEvaluationRequest();
        request.setExpression("#amount > 500");
        request.setContext(testContext);

        RuleResult mockResult = RuleResult.match("Expression", "Expression evaluated to true");
        when(expressionEvaluationService.evaluateWithResult(eq("#amount > 500"), eq(testContext)))
            .thenReturn(mockResult);

        // Act
        ResponseEntity<Map<String, Object>> response = expressionController.evaluateExpressionWithResult(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals("#amount > 500", responseBody.get("expression"));
        assertEquals(testContext, responseBody.get("context"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> ruleResult = (Map<String, Object>) responseBody.get("ruleResult");
        assertTrue((Boolean) ruleResult.get("triggered"));
        assertEquals("Expression", ruleResult.get("ruleName"));
        assertEquals("Expression evaluated to true", ruleResult.get("message"));

        verify(expressionEvaluationService).evaluateWithResult("#amount > 500", testContext);
    }

    @Test
    @DisplayName("Should evaluate batch expressions successfully")
    void testEvaluateBatchExpressionsSuccess() {
        // Arrange
        List<ExpressionController.ExpressionItem> expressions = Arrays.asList(
            new ExpressionController.ExpressionItem("total-calculation", "#amount * #rate + #fee"),
            new ExpressionController.ExpressionItem("age-check", "#age >= 18"),
            new ExpressionController.ExpressionItem("currency-check", "#currency == 'USD'")
        );

        ExpressionController.BatchExpressionEvaluationRequest request = new ExpressionController.BatchExpressionEvaluationRequest();
        request.setExpressions(expressions);
        request.setContext(testContext);

        when(expressionEvaluationService.evaluate("#amount * #rate + #fee", testContext)).thenReturn(75.0);
        when(expressionEvaluationService.evaluate("#age >= 18", testContext)).thenReturn(true);
        when(expressionEvaluationService.evaluate("#currency == 'USD'", testContext)).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = expressionController.evaluateBatchExpressions(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(3, responseBody.get("totalExpressions"));
        assertEquals(3, responseBody.get("successfulExpressions"));
        assertEquals(testContext, responseBody.get("context"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> expressionResults = (List<Map<String, Object>>) responseBody.get("expressionResults");
        assertEquals(3, expressionResults.size());

        // Verify each expression result
        for (Map<String, Object> result : expressionResults) {
            assertTrue((Boolean) result.get("success"));
            assertNotNull(result.get("result"));
            assertNotNull(result.get("resultType"));
        }

        verify(expressionEvaluationService, times(3)).evaluate(anyString(), eq(testContext));
    }

    @Test
    @DisplayName("Should handle batch expressions with some failures")
    void testEvaluateBatchExpressionsPartialFailure() {
        // Arrange
        List<ExpressionController.ExpressionItem> expressions = Arrays.asList(
            new ExpressionController.ExpressionItem("good-expression", "#amount > 0"),
            new ExpressionController.ExpressionItem("bad-expression", "invalid syntax")
        );

        ExpressionController.BatchExpressionEvaluationRequest request = new ExpressionController.BatchExpressionEvaluationRequest();
        request.setExpressions(expressions);
        request.setContext(testContext);

        when(expressionEvaluationService.evaluate("#amount > 0", testContext)).thenReturn(true);
        when(expressionEvaluationService.evaluate("invalid syntax", testContext))
            .thenThrow(new RuntimeException("Syntax error"));

        // Act
        ResponseEntity<Map<String, Object>> response = expressionController.evaluateBatchExpressions(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(2, responseBody.get("totalExpressions"));
        assertEquals(1, responseBody.get("successfulExpressions"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> expressionResults = (List<Map<String, Object>>) responseBody.get("expressionResults");
        assertEquals(2, expressionResults.size());

        // First expression should succeed
        assertTrue((Boolean) expressionResults.get(0).get("success"));
        // Second expression should fail
        assertFalse((Boolean) expressionResults.get(1).get("success"));
        assertTrue(expressionResults.get(1).get("error").toString().contains("Syntax error"));
    }

    @Test
    @DisplayName("Should validate expression syntax successfully")
    void testValidateExpressionSuccess() {
        // Arrange
        ExpressionController.ExpressionValidationRequest request = new ExpressionController.ExpressionValidationRequest();
        request.setExpression("#amount > 1000 && #currency == 'USD'");

        when(expressionEvaluationService.validateSyntax("#amount > 1000 && #currency == 'USD'"))
            .thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = expressionController.validateExpression(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals("#amount > 1000 && #currency == 'USD'", responseBody.get("expression"));
        assertTrue((Boolean) responseBody.get("valid"));
        assertEquals("Expression syntax is valid", responseBody.get("message"));

        verify(expressionEvaluationService).validateSyntax("#amount > 1000 && #currency == 'USD'");
    }

    @Test
    @DisplayName("Should validate invalid expression syntax")
    void testValidateExpressionInvalid() {
        // Arrange
        ExpressionController.ExpressionValidationRequest request = new ExpressionController.ExpressionValidationRequest();
        request.setExpression("invalid && syntax >");

        when(expressionEvaluationService.validateSyntax("invalid && syntax >"))
            .thenThrow(new RuntimeException("Unexpected token"));

        // Act
        ResponseEntity<Map<String, Object>> response = expressionController.validateExpression(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals("invalid && syntax >", responseBody.get("expression"));
        assertFalse((Boolean) responseBody.get("valid"));
        assertTrue(responseBody.get("message").toString().contains("Expression syntax is invalid"));
        assertTrue(responseBody.get("error").toString().contains("Unexpected token"));
    }

    @Test
    @DisplayName("Should get available SpEL functions successfully")
    void testGetAvailableFunctions() {
        // Act
        ResponseEntity<Map<String, Object>> response = expressionController.getAvailableFunctions();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        
        @SuppressWarnings("unchecked")
        Map<String, List<String>> functions = (Map<String, List<String>>) responseBody.get("functions");
        
        // Verify function categories exist
        assertTrue(functions.containsKey("mathematical"));
        assertTrue(functions.containsKey("string"));
        assertTrue(functions.containsKey("datetime"));
        assertTrue(functions.containsKey("collections"));
        assertTrue(functions.containsKey("logical"));
        assertTrue(functions.containsKey("comparison"));

        // Verify some specific functions
        assertTrue(functions.get("mathematical").contains("abs(number)"));
        assertTrue(functions.get("string").contains("length()"));
        assertTrue(functions.get("logical").contains("&&"));
        assertTrue(functions.get("comparison").contains("=="));
    }

    @Test
    @DisplayName("Should validate ExpressionEvaluationRequest DTO")
    void testExpressionEvaluationRequestDto() {
        // Test default constructor
        ExpressionController.ExpressionEvaluationRequest request1 = new ExpressionController.ExpressionEvaluationRequest();
        assertNotNull(request1);

        // Test setters and getters
        request1.setExpression(testExpression);
        request1.setContext(testContext);

        assertEquals(testExpression, request1.getExpression());
        assertEquals(testContext, request1.getContext());
    }

    @Test
    @DisplayName("Should validate BatchExpressionEvaluationRequest DTO")
    void testBatchExpressionEvaluationRequestDto() {
        // Test default constructor
        ExpressionController.BatchExpressionEvaluationRequest request1 = new ExpressionController.BatchExpressionEvaluationRequest();
        assertNotNull(request1);

        // Test setters and getters
        List<ExpressionController.ExpressionItem> expressions = Arrays.asList(
            new ExpressionController.ExpressionItem("test", testExpression)
        );
        request1.setExpressions(expressions);
        request1.setContext(testContext);

        assertEquals(expressions, request1.getExpressions());
        assertEquals(testContext, request1.getContext());
    }

    @Test
    @DisplayName("Should validate ExpressionItem DTO")
    void testExpressionItemDto() {
        // Test default constructor
        ExpressionController.ExpressionItem item1 = new ExpressionController.ExpressionItem();
        assertNotNull(item1);

        // Test parameterized constructor
        ExpressionController.ExpressionItem item2 = new ExpressionController.ExpressionItem("test-expr", testExpression);
        assertEquals("test-expr", item2.getName());
        assertEquals(testExpression, item2.getExpression());

        // Test setters
        item1.setName("another-expr");
        item1.setExpression("#value > 0");

        assertEquals("another-expr", item1.getName());
        assertEquals("#value > 0", item1.getExpression());
    }

    @Test
    @DisplayName("Should validate ExpressionValidationRequest DTO")
    void testExpressionValidationRequestDto() {
        // Test default constructor
        ExpressionController.ExpressionValidationRequest request1 = new ExpressionController.ExpressionValidationRequest();
        assertNotNull(request1);

        // Test setters and getters
        request1.setExpression(testExpression);
        assertEquals(testExpression, request1.getExpression());
    }

    @Test
    @DisplayName("Should handle expression evaluation with null result")
    void testEvaluateExpressionNullResult() {
        // Arrange
        ExpressionController.ExpressionEvaluationRequest request = new ExpressionController.ExpressionEvaluationRequest();
        request.setExpression("#nonExistentVariable");
        request.setContext(testContext);

        when(expressionEvaluationService.evaluate(eq("#nonExistentVariable"), eq(testContext)))
            .thenReturn(null);

        // Act
        ResponseEntity<Map<String, Object>> response = expressionController.evaluateExpression(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertNull(responseBody.get("result"));
        assertEquals("null", responseBody.get("resultType"));
    }

    @Test
    @DisplayName("Should handle batch expressions with empty list")
    void testEvaluateBatchExpressionsEmpty() {
        // Arrange
        ExpressionController.BatchExpressionEvaluationRequest request = new ExpressionController.BatchExpressionEvaluationRequest();
        request.setExpressions(new ArrayList<>());
        request.setContext(testContext);

        // Act
        ResponseEntity<Map<String, Object>> response = expressionController.evaluateBatchExpressions(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(0, responseBody.get("totalExpressions"));
        assertEquals(0, responseBody.get("successfulExpressions"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> expressionResults = (List<Map<String, Object>>) responseBody.get("expressionResults");
        assertTrue(expressionResults.isEmpty());

        verifyNoInteractions(expressionEvaluationService);
    }
}
