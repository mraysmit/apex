package dev.mars.apex.rest.controller;

import dev.mars.apex.core.service.transform.GenericTransformerService;
import dev.mars.apex.core.service.transform.TransformerRule;
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
 * Unit tests for TransformationController.
 * Tests transformation operations with mocked services for isolated testing.
 */
@ExtendWith(MockitoExtension.class)
class TransformationControllerTest {

    @Mock
    private GenericTransformerService transformerService;

    @InjectMocks
    private TransformationController transformationController;

    private Map<String, Object> testData;

    @BeforeEach
    void setUp() {
        testData = new HashMap<>();
        testData.put("firstName", "john");
        testData.put("lastName", "doe");
        testData.put("email", "JOHN.DOE@EXAMPLE.COM");
        testData.put("age", 25);
    }

    @Test
    @DisplayName("Should transform data using registered transformer successfully")
    void testTransformDataSuccess() {
        // Arrange
        String transformerName = "customer-normalizer";
        Map<String, Object> transformedData = new HashMap<>();
        transformedData.put("firstName", "John");
        transformedData.put("lastName", "Doe");
        transformedData.put("email", "john.doe@example.com");
        transformedData.put("age", 25);

        when(transformerService.transform(eq(transformerName), eq(testData)))
            .thenReturn(transformedData);

        // Act
        ResponseEntity<Map<String, Object>> response = transformationController.transformData(transformerName, testData);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(transformerName, responseBody.get("transformerName"));
        assertEquals(testData, responseBody.get("originalData"));
        assertEquals(transformedData, responseBody.get("transformedData"));
        assertNotNull(responseBody.get("timestamp"));

        verify(transformerService).transform(transformerName, testData);
    }

    @Test
    @DisplayName("Should return 404 when transformer not found")
    void testTransformDataTransformerNotFound() {
        // Arrange
        String transformerName = "non-existent-transformer";
        when(transformerService.transform(eq(transformerName), eq(testData)))
            .thenThrow(new IllegalArgumentException("Transformer not found: " + transformerName));

        // Act
        ResponseEntity<Map<String, Object>> response = transformationController.transformData(transformerName, testData);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Transformer not found", responseBody.get("error"));
        assertEquals(transformerName, responseBody.get("transformerName"));
    }

    @Test
    @DisplayName("Should return 500 when transformation fails")
    void testTransformDataTransformationError() {
        // Arrange
        String transformerName = "failing-transformer";
        when(transformerService.transform(eq(transformerName), eq(testData)))
            .thenThrow(new RuntimeException("Transformation failed"));

        // Act
        ResponseEntity<Map<String, Object>> response = transformationController.transformData(transformerName, testData);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Transformation failed", responseBody.get("error"));
        assertEquals(transformerName, responseBody.get("transformerName"));
    }

    @Test
    @DisplayName("Should transform data with detailed result successfully")
    void testTransformDataWithDetailedResult() {
        // Arrange
        String transformerName = "customer-normalizer";
        RuleResult ruleResult = RuleResult.match("customer-normalizer", "Data transformed successfully");

        when(transformerService.transformWithResult(eq(transformerName), eq(testData)))
            .thenReturn(ruleResult);

        // Act
        ResponseEntity<Map<String, Object>> response = transformationController.transformDataWithResult(transformerName, testData);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(transformerName, responseBody.get("transformerName"));
        assertEquals(testData, responseBody.get("originalData"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> ruleResultMap = (Map<String, Object>) responseBody.get("ruleResult");
        assertTrue((Boolean) ruleResultMap.get("triggered"));
        assertEquals("customer-normalizer", ruleResultMap.get("ruleName"));
        assertEquals("Data transformed successfully", ruleResultMap.get("message"));

        verify(transformerService).transformWithResult(transformerName, testData);
    }

    @Test
    @DisplayName("Should apply dynamic transformation successfully")
    void testTransformWithDynamicRules() {
        // Arrange
        TransformationController.DynamicTransformationRequest request = new TransformationController.DynamicTransformationRequest();
        request.setData(testData);
        
        List<TransformationController.TransformerRuleDto> rules = Arrays.asList(
            new TransformationController.TransformerRuleDto("normalize-name", "#firstName != null", 
                "#firstName.substring(0,1).toUpperCase() + #firstName.substring(1).toLowerCase()", "firstName"),
            new TransformationController.TransformerRuleDto("normalize-email", "#email != null", 
                "#email.toLowerCase()", "email")
        );
        request.setTransformerRules(rules);

        Map<String, Object> transformedData = new HashMap<>();
        transformedData.put("firstName", "John");
        transformedData.put("lastName", "doe");
        transformedData.put("email", "john.doe@example.com");
        transformedData.put("age", 25);

        when(transformerService.transform(eq(testData), anyList()))
            .thenReturn(transformedData);

        // Act
        ResponseEntity<Map<String, Object>> response = transformationController.transformWithDynamicRules(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(testData, responseBody.get("originalData"));
        assertEquals(transformedData, responseBody.get("transformedData"));
        assertEquals(2, responseBody.get("appliedRules"));

        verify(transformerService).transform(eq(testData), anyList());
    }

    @Test
    @DisplayName("Should return registered transformers successfully")
    void testGetRegisteredTransformers() {
        // Arrange
        String[] registeredTransformers = {"customer-normalizer", "address-formatter", "phone-validator"};
        when(transformerService.getRegisteredTransformers()).thenReturn(registeredTransformers);

        // Act
        ResponseEntity<Map<String, Object>> response = transformationController.getRegisteredTransformers();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        
        @SuppressWarnings("unchecked")
        List<String> transformers = (List<String>) responseBody.get("transformers");
        assertEquals(3, transformers.size());
        assertTrue(transformers.contains("customer-normalizer"));
        assertTrue(transformers.contains("address-formatter"));
        assertTrue(transformers.contains("phone-validator"));
        assertEquals(3, responseBody.get("count"));

        verify(transformerService).getRegisteredTransformers();
    }

    @Test
    @DisplayName("Should handle error when getting registered transformers")
    void testGetRegisteredTransformersError() {
        // Arrange
        when(transformerService.getRegisteredTransformers())
            .thenThrow(new RuntimeException("Service unavailable"));

        // Act
        ResponseEntity<Map<String, Object>> response = transformationController.getRegisteredTransformers();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Failed to retrieve transformers", responseBody.get("error"));
    }

    @Test
    @DisplayName("Should handle dynamic transformation with empty rules")
    void testTransformWithDynamicRulesEmpty() {
        // Arrange
        TransformationController.DynamicTransformationRequest request = new TransformationController.DynamicTransformationRequest();
        request.setData(testData);
        request.setTransformerRules(new ArrayList<>());

        when(transformerService.transform(eq(testData), anyList()))
            .thenReturn(testData); // No transformation applied

        // Act
        ResponseEntity<Map<String, Object>> response = transformationController.transformWithDynamicRules(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(testData, responseBody.get("originalData"));
        assertEquals(testData, responseBody.get("transformedData"));
        assertEquals(0, responseBody.get("appliedRules"));
    }

    @Test
    @DisplayName("Should validate TransformerRuleDto creation")
    void testTransformerRuleDtoCreation() {
        // Test default constructor
        TransformationController.TransformerRuleDto rule1 = new TransformationController.TransformerRuleDto();
        assertNotNull(rule1);

        // Test parameterized constructor
        TransformationController.TransformerRuleDto rule2 = new TransformationController.TransformerRuleDto(
            "test-rule", "#value != null", "#value.toString()", "targetField");
        
        assertEquals("test-rule", rule2.getName());
        assertEquals("#value != null", rule2.getCondition());
        assertEquals("#value.toString()", rule2.getTransformation());
        assertEquals("targetField", rule2.getTargetField());

        // Test setters
        rule1.setName("another-rule");
        rule1.setCondition("#amount > 0");
        rule1.setTransformation("#amount * 1.1");
        rule1.setTargetField("adjustedAmount");

        assertEquals("another-rule", rule1.getName());
        assertEquals("#amount > 0", rule1.getCondition());
        assertEquals("#amount * 1.1", rule1.getTransformation());
        assertEquals("adjustedAmount", rule1.getTargetField());
    }
}
