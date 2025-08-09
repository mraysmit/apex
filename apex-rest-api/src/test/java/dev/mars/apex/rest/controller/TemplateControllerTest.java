package dev.mars.apex.rest.controller;

import dev.mars.apex.core.service.engine.TemplateProcessorService;
import org.springframework.expression.spel.support.StandardEvaluationContext;
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
 * Unit tests for TemplateController.
 * Tests template processing operations with mocked services for isolated testing.
 */
@ExtendWith(MockitoExtension.class)
class TemplateControllerTest {

    @Mock
    private TemplateProcessorService templateProcessorService;

    @InjectMocks
    private TemplateController templateController;

    private Map<String, Object> testContext;
    private String jsonTemplate;
    private String xmlTemplate;
    private String textTemplate;

    @BeforeEach
    void setUp() {
        testContext = new HashMap<>();
        testContext.put("customerId", "CUST001");
        testContext.put("customerName", "John Doe");
        testContext.put("totalAmount", 1500.0);
        testContext.put("currency", "USD");
        testContext.put("amount", 1500.0);

        jsonTemplate = """
            {
              "customerId": "#{#customerId}",
              "customerName": "#{#customerName}",
              "totalAmount": #{#totalAmount},
              "currency": "#{#currency}",
              "timestamp": "#{T(java.time.Instant).now()}",
              "status": "#{#amount > 1000 ? 'HIGH_VALUE' : 'STANDARD'}"
            }
            """;

        xmlTemplate = """
            <?xml version="1.0" encoding="UTF-8"?>
            <customer>
              <id>#{#customerId}</id>
              <name>#{#customerName}</name>
              <amount>#{#totalAmount}</amount>
              <currency>#{#currency}</currency>
            </customer>
            """;

        textTemplate = """
            Dear #{#customerName},
            
            Your transaction #{#customerId} for #{#totalAmount} #{#currency} has been processed.
            
            Status: #{#amount > 1000 ? 'High Value Transaction' : 'Standard Transaction'}
            
            Thank you for your business.
            """;
    }

    @Test
    @DisplayName("Should process JSON template successfully")
    void testProcessJsonTemplateSuccess() {
        // Arrange
        TemplateController.TemplateProcessingRequest request = new TemplateController.TemplateProcessingRequest();
        request.setTemplate(jsonTemplate);
        request.setContext(testContext);

        String processedJson = """
            {
              "customerId": "CUST001",
              "customerName": "John Doe",
              "totalAmount": 1500.0,
              "currency": "USD",
              "timestamp": "2024-01-15T10:30:00Z",
              "status": "HIGH_VALUE"
            }
            """;

        when(templateProcessorService.processJsonTemplate(eq(jsonTemplate), any(StandardEvaluationContext.class)))
            .thenReturn(processedJson);

        // Act
        ResponseEntity<Map<String, Object>> response = templateController.processJsonTemplate(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals("JSON", responseBody.get("templateType"));
        assertEquals(jsonTemplate, responseBody.get("originalTemplate"));
        assertEquals(processedJson, responseBody.get("processedTemplate"));
        assertEquals(testContext, responseBody.get("context"));

        verify(templateProcessorService).processJsonTemplate(eq(jsonTemplate), any(StandardEvaluationContext.class));
    }

    @Test
    @DisplayName("Should handle JSON template processing error")
    void testProcessJsonTemplateError() {
        // Arrange
        TemplateController.TemplateProcessingRequest request = new TemplateController.TemplateProcessingRequest();
        request.setTemplate(jsonTemplate);
        request.setContext(testContext);

        when(templateProcessorService.processJsonTemplate(eq(jsonTemplate), any(StandardEvaluationContext.class)))
            .thenThrow(new RuntimeException("Template processing failed"));

        // Act
        ResponseEntity<Map<String, Object>> response = templateController.processJsonTemplate(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("JSON template processing failed", responseBody.get("error"));
        assertTrue(responseBody.get("message").toString().contains("Template processing failed"));
    }

    @Test
    @DisplayName("Should process XML template successfully")
    void testProcessXmlTemplateSuccess() {
        // Arrange
        TemplateController.TemplateProcessingRequest request = new TemplateController.TemplateProcessingRequest();
        request.setTemplate(xmlTemplate);
        request.setContext(testContext);

        String processedXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <customer>
              <id>CUST001</id>
              <name>John Doe</name>
              <amount>1500.0</amount>
              <currency>USD</currency>
            </customer>
            """;

        when(templateProcessorService.processXmlTemplate(eq(xmlTemplate), any(StandardEvaluationContext.class)))
            .thenReturn(processedXml);

        // Act
        ResponseEntity<Map<String, Object>> response = templateController.processXmlTemplate(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals("XML", responseBody.get("templateType"));
        assertEquals(xmlTemplate, responseBody.get("originalTemplate"));
        assertEquals(processedXml, responseBody.get("processedTemplate"));
        assertEquals(testContext, responseBody.get("context"));

        verify(templateProcessorService).processXmlTemplate(eq(xmlTemplate), any(StandardEvaluationContext.class));
    }

    @Test
    @DisplayName("Should process text template successfully")
    void testProcessTextTemplateSuccess() {
        // Arrange
        TemplateController.TemplateProcessingRequest request = new TemplateController.TemplateProcessingRequest();
        request.setTemplate(textTemplate);
        request.setContext(testContext);

        String processedText = """
            Dear John Doe,
            
            Your transaction CUST001 for 1500.0 USD has been processed.
            
            Status: High Value Transaction
            
            Thank you for your business.
            """;

        when(templateProcessorService.processTemplate(eq(textTemplate), any(StandardEvaluationContext.class)))
            .thenReturn(processedText);

        // Act
        ResponseEntity<Map<String, Object>> response = templateController.processTextTemplate(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals("TEXT", responseBody.get("templateType"));
        assertEquals(textTemplate, responseBody.get("originalTemplate"));
        assertEquals(processedText, responseBody.get("processedTemplate"));
        assertEquals(testContext, responseBody.get("context"));

        verify(templateProcessorService).processTemplate(eq(textTemplate), any(StandardEvaluationContext.class));
    }

    @Test
    @DisplayName("Should process batch templates successfully")
    void testProcessBatchTemplatesSuccess() {
        // Arrange
        List<TemplateController.TemplateItem> templates = Arrays.asList(
            new TemplateController.TemplateItem("customer-json", "JSON", jsonTemplate),
            new TemplateController.TemplateItem("customer-xml", "XML", xmlTemplate),
            new TemplateController.TemplateItem("customer-text", "TEXT", textTemplate)
        );

        TemplateController.BatchTemplateProcessingRequest request = new TemplateController.BatchTemplateProcessingRequest();
        request.setTemplates(templates);
        request.setContext(testContext);

        when(templateProcessorService.processJsonTemplate(eq(jsonTemplate), any(StandardEvaluationContext.class)))
            .thenReturn("processed json");
        when(templateProcessorService.processXmlTemplate(eq(xmlTemplate), any(StandardEvaluationContext.class)))
            .thenReturn("processed xml");
        when(templateProcessorService.processTemplate(eq(textTemplate), any(StandardEvaluationContext.class)))
            .thenReturn("processed text");

        // Act
        ResponseEntity<Map<String, Object>> response = templateController.processBatchTemplates(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(3, responseBody.get("totalTemplates"));
        assertEquals(3, responseBody.get("successfulTemplates"));
        assertEquals(testContext, responseBody.get("context"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> processedTemplates = (List<Map<String, Object>>) responseBody.get("processedTemplates");
        assertEquals(3, processedTemplates.size());

        // Verify each template was processed
        for (Map<String, Object> template : processedTemplates) {
            assertTrue((Boolean) template.get("success"));
            assertNotNull(template.get("processedTemplate"));
        }

        verify(templateProcessorService).processJsonTemplate(eq(jsonTemplate), any(StandardEvaluationContext.class));
        verify(templateProcessorService).processXmlTemplate(eq(xmlTemplate), any(StandardEvaluationContext.class));
        verify(templateProcessorService).processTemplate(eq(textTemplate), any(StandardEvaluationContext.class));
    }

    @Test
    @DisplayName("Should handle batch processing with some failures")
    void testProcessBatchTemplatesPartialFailure() {
        // Arrange
        List<TemplateController.TemplateItem> templates = Arrays.asList(
            new TemplateController.TemplateItem("good-template", "JSON", jsonTemplate),
            new TemplateController.TemplateItem("bad-template", "JSON", "invalid template")
        );

        TemplateController.BatchTemplateProcessingRequest request = new TemplateController.BatchTemplateProcessingRequest();
        request.setTemplates(templates);
        request.setContext(testContext);

        when(templateProcessorService.processJsonTemplate(eq(jsonTemplate), any(StandardEvaluationContext.class)))
            .thenReturn("processed json");
        when(templateProcessorService.processJsonTemplate(eq("invalid template"), any(StandardEvaluationContext.class)))
            .thenThrow(new RuntimeException("Invalid template"));

        // Act
        ResponseEntity<Map<String, Object>> response = templateController.processBatchTemplates(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(2, responseBody.get("totalTemplates"));
        assertEquals(1, responseBody.get("successfulTemplates"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> processedTemplates = (List<Map<String, Object>>) responseBody.get("processedTemplates");
        assertEquals(2, processedTemplates.size());

        // First template should succeed
        assertTrue((Boolean) processedTemplates.get(0).get("success"));
        // Second template should fail
        assertFalse((Boolean) processedTemplates.get(1).get("success"));
        assertTrue(processedTemplates.get(1).get("error").toString().contains("Invalid template"));
    }

    @Test
    @DisplayName("Should validate TemplateProcessingRequest DTO")
    void testTemplateProcessingRequestDto() {
        // Test default constructor
        TemplateController.TemplateProcessingRequest request1 = new TemplateController.TemplateProcessingRequest();
        assertNotNull(request1);

        // Test setters and getters
        request1.setTemplate(jsonTemplate);
        request1.setContext(testContext);

        assertEquals(jsonTemplate, request1.getTemplate());
        assertEquals(testContext, request1.getContext());
    }

    @Test
    @DisplayName("Should validate BatchTemplateProcessingRequest DTO")
    void testBatchTemplateProcessingRequestDto() {
        // Test default constructor
        TemplateController.BatchTemplateProcessingRequest request1 = new TemplateController.BatchTemplateProcessingRequest();
        assertNotNull(request1);

        // Test setters and getters
        List<TemplateController.TemplateItem> templates = Arrays.asList(
            new TemplateController.TemplateItem("test", "JSON", jsonTemplate)
        );
        request1.setTemplates(templates);
        request1.setContext(testContext);

        assertEquals(templates, request1.getTemplates());
        assertEquals(testContext, request1.getContext());
    }

    @Test
    @DisplayName("Should validate TemplateItem DTO")
    void testTemplateItemDto() {
        // Test default constructor
        TemplateController.TemplateItem item1 = new TemplateController.TemplateItem();
        assertNotNull(item1);

        // Test parameterized constructor
        TemplateController.TemplateItem item2 = new TemplateController.TemplateItem("test-template", "JSON", jsonTemplate);
        assertEquals("test-template", item2.getName());
        assertEquals("JSON", item2.getType());
        assertEquals(jsonTemplate, item2.getTemplate());

        // Test setters
        item1.setName("another-template");
        item1.setType("XML");
        item1.setTemplate(xmlTemplate);

        assertEquals("another-template", item1.getName());
        assertEquals("XML", item1.getType());
        assertEquals(xmlTemplate, item1.getTemplate());
    }

    @Test
    @DisplayName("Should handle batch processing with empty template list")
    void testProcessBatchTemplatesEmpty() {
        // Arrange
        TemplateController.BatchTemplateProcessingRequest request = new TemplateController.BatchTemplateProcessingRequest();
        request.setTemplates(new ArrayList<>());
        request.setContext(testContext);

        // Act
        ResponseEntity<Map<String, Object>> response = templateController.processBatchTemplates(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(0, responseBody.get("totalTemplates"));
        assertEquals(0, responseBody.get("successfulTemplates"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> processedTemplates = (List<Map<String, Object>>) responseBody.get("processedTemplates");
        assertTrue(processedTemplates.isEmpty());

        verifyNoInteractions(templateProcessorService);
    }

    @Test
    @DisplayName("Should handle unknown template type in batch processing")
    void testProcessBatchTemplatesUnknownType() {
        // Arrange
        List<TemplateController.TemplateItem> templates = Arrays.asList(
            new TemplateController.TemplateItem("unknown-template", "UNKNOWN", "some template")
        );

        TemplateController.BatchTemplateProcessingRequest request = new TemplateController.BatchTemplateProcessingRequest();
        request.setTemplates(templates);
        request.setContext(testContext);

        when(templateProcessorService.processTemplate(eq("some template"), any(StandardEvaluationContext.class)))
            .thenReturn("processed as text");

        // Act
        ResponseEntity<Map<String, Object>> response = templateController.processBatchTemplates(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(1, responseBody.get("successfulTemplates"));

        // Unknown type should default to text processing
        verify(templateProcessorService).processTemplate(eq("some template"), any(StandardEvaluationContext.class));
    }

    /**
     * Helper method to create evaluation context from a map of variables.
     */
    private StandardEvaluationContext createEvaluationContext(Map<String, Object> contextVariables) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        if (contextVariables != null) {
            for (Map.Entry<String, Object> entry : contextVariables.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }
        }

        return context;
    }
}
