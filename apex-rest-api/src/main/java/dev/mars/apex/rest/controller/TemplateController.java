package dev.mars.apex.rest.controller;

import dev.mars.apex.core.service.engine.TemplateProcessorService;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;

/**
 * REST Controller for template processing operations.
 * Provides endpoints for processing JSON, XML, and generic templates using the APEX template engine.
 */
@RestController
@RequestMapping("/api/template")
@Tag(name = "Template Processing", description = "Template processing operations")
public class TemplateController {

    private static final Logger logger = LoggerFactory.getLogger(TemplateController.class);

    @Autowired
    private TemplateProcessorService templateProcessorService;

    /**
     * Process a JSON template with provided context data.
     */
    @PostMapping("/json")
    @Operation(
        summary = "Process JSON template",
        description = "Processes a JSON template by replacing expressions with values from the provided context data."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "JSON template processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid template processing request"),
        @ApiResponse(responseCode = "500", description = "Template processing error")
    })
    public ResponseEntity<Map<String, Object>> processJsonTemplate(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "JSON template processing request",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Customer JSON template",
                        value = """
                        {
                          "template": "{\\n  \\"customerId\\": \\"#{#customerId}\\",\\n  \\"customerName\\": \\"#{#customerName}\\",\\n  \\"totalAmount\\": #{#totalAmount},\\n  \\"currency\\": \\"#{#currency}\\",\\n  \\"timestamp\\": \\"#{T(java.time.Instant).now()}\\",\\n  \\"status\\": \\"#{#amount > 1000 ? 'HIGH_VALUE' : 'STANDARD'}\\"\\n}",
                          "context": {
                            "customerId": "CUST001",
                            "customerName": "John Doe",
                            "totalAmount": 1500.0,
                            "currency": "USD",
                            "amount": 1500.0
                          }
                        }
                        """
                    )
                )
            )
            @Valid @NotNull TemplateProcessingRequest request) {

        logger.info("Processing JSON template");
        logger.debug("Template length: {} characters", request.getTemplate().length());

        try {
            // Process the JSON template
            String processedJson = templateProcessorService.processJsonTemplate(
                request.getTemplate(),
                createEvaluationContext(request.getContext())
            );

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("templateType", "JSON");
            response.put("originalTemplate", request.getTemplate());
            response.put("processedTemplate", processedJson);
            response.put("context", request.getContext());
            response.put("timestamp", Instant.now());

            logger.info("JSON template processing completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing JSON template: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "JSON template processing failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Process an XML template with provided context data.
     */
    @PostMapping("/xml")
    @Operation(
        summary = "Process XML template",
        description = "Processes an XML template by replacing expressions with values from the provided context data."
    )
    @ApiResponse(responseCode = "200", description = "XML template processed successfully")
    public ResponseEntity<Map<String, Object>> processXmlTemplate(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "XML template processing request",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Trade XML template",
                        value = """
                        {
                          "template": "<?xml version=\\"1.0\\" encoding=\\"UTF-8\\"?>\\n<trade>\\n  <tradeId>#{#tradeId}</tradeId>\\n  <instrument>#{#instrumentName}</instrument>\\n  <quantity>#{#quantity}</quantity>\\n  <price>#{#price}</price>\\n  <totalValue>#{#quantity * #price}</totalValue>\\n  <currency>#{#currency}</currency>\\n  <timestamp>#{T(java.time.Instant).now()}</timestamp>\\n  <status>#{#quantity > 1000 ? 'LARGE_TRADE' : 'NORMAL_TRADE'}</status>\\n</trade>",
                          "context": {
                            "tradeId": "TRD001",
                            "instrumentName": "AAPL",
                            "quantity": 1500,
                            "price": 150.25,
                            "currency": "USD"
                          }
                        }
                        """
                    )
                )
            )
            @Valid @NotNull TemplateProcessingRequest request) {

        logger.info("Processing XML template");

        try {
            // Process the XML template
            String processedXml = templateProcessorService.processXmlTemplate(
                request.getTemplate(),
                createEvaluationContext(request.getContext())
            );

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("templateType", "XML");
            response.put("originalTemplate", request.getTemplate());
            response.put("processedTemplate", processedXml);
            response.put("context", request.getContext());
            response.put("timestamp", Instant.now());

            logger.info("XML template processing completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing XML template: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "XML template processing failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Process a generic text template with provided context data.
     */
    @PostMapping("/text")
    @Operation(
        summary = "Process text template",
        description = "Processes a generic text template by replacing expressions with values from the provided context data."
    )
    @ApiResponse(responseCode = "200", description = "Text template processed successfully")
    public ResponseEntity<Map<String, Object>> processTextTemplate(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Text template processing request",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Email text template",
                        value = """
                        {
                          "template": "Dear #{#customerName},\\n\\nYour trade #{#tradeId} for #{#quantity} shares of #{#instrumentName} has been executed at $#{#price} per share.\\n\\nTotal Value: $#{#quantity * #price}\\nCurrency: #{#currency}\\nExecution Time: #{T(java.time.Instant).now()}\\n\\nStatus: #{#quantity > 1000 ? 'Large Trade - Please review' : 'Standard Trade'}\\n\\nThank you for your business.\\n\\nBest regards,\\nTrading Team",
                          "context": {
                            "customerName": "John Smith",
                            "tradeId": "TRD001",
                            "instrumentName": "Apple Inc. (AAPL)",
                            "quantity": 1500,
                            "price": 150.25,
                            "currency": "USD"
                          }
                        }
                        """
                    )
                )
            )
            @Valid @NotNull TemplateProcessingRequest request) {

        logger.info("Processing text template");

        try {
            // Process the text template
            String processedText = templateProcessorService.processTemplate(
                request.getTemplate(),
                createEvaluationContext(request.getContext())
            );

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("templateType", "TEXT");
            response.put("originalTemplate", request.getTemplate());
            response.put("processedTemplate", processedText);
            response.put("context", request.getContext());
            response.put("timestamp", Instant.now());

            logger.info("Text template processing completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing text template: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Text template processing failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Process multiple templates in batch.
     */
    @PostMapping("/batch")
    @Operation(
        summary = "Process multiple templates in batch",
        description = "Processes multiple templates of different types using the same context data."
    )
    @ApiResponse(responseCode = "200", description = "Batch template processing completed")
    public ResponseEntity<Map<String, Object>> processBatchTemplates(
            @RequestBody @Valid @NotNull BatchTemplateProcessingRequest request) {

        logger.info("Processing {} templates in batch", request.getTemplates().size());

        try {
            List<Map<String, Object>> processedTemplates = new ArrayList<>();

            for (TemplateItem templateItem : request.getTemplates()) {
                Map<String, Object> templateResult = new HashMap<>();
                templateResult.put("templateName", templateItem.getName());
                templateResult.put("templateType", templateItem.getType());
                templateResult.put("originalTemplate", templateItem.getTemplate());

                try {
                    String processedTemplate;
                    switch (templateItem.getType().toUpperCase()) {
                        case "JSON":
                            processedTemplate = templateProcessorService.processJsonTemplate(
                                templateItem.getTemplate(), createEvaluationContext(request.getContext()));
                            break;
                        case "XML":
                            processedTemplate = templateProcessorService.processXmlTemplate(
                                templateItem.getTemplate(), createEvaluationContext(request.getContext()));
                            break;
                        case "TEXT":
                        default:
                            processedTemplate = templateProcessorService.processTemplate(
                                templateItem.getTemplate(), createEvaluationContext(request.getContext()));
                            break;
                    }

                    templateResult.put("success", true);
                    templateResult.put("processedTemplate", processedTemplate);

                } catch (Exception e) {
                    logger.warn("Error processing template '{}': {}", templateItem.getName(), e.getMessage());
                    templateResult.put("success", false);
                    templateResult.put("error", e.getMessage());
                }

                processedTemplates.add(templateResult);
            }

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("processedTemplates", processedTemplates);
            response.put("totalTemplates", request.getTemplates().size());
            response.put("successfulTemplates", processedTemplates.stream()
                .mapToInt(t -> (Boolean) t.get("success") ? 1 : 0).sum());
            response.put("context", request.getContext());
            response.put("timestamp", Instant.now());

            logger.info("Batch template processing completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during batch template processing: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Batch template processing failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // DTOs for request/response
    public static class TemplateProcessingRequest {
        @NotBlank
        private String template;
        
        @NotNull
        private Map<String, Object> context;

        // Getters and setters
        public String getTemplate() { return template; }
        public void setTemplate(String template) { this.template = template; }
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
    }

    public static class BatchTemplateProcessingRequest {
        @NotNull
        private List<TemplateItem> templates;
        
        @NotNull
        private Map<String, Object> context;

        // Getters and setters
        public List<TemplateItem> getTemplates() { return templates; }
        public void setTemplates(List<TemplateItem> templates) { this.templates = templates; }
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
    }

    public static class TemplateItem {
        @NotBlank
        private String name;
        @NotBlank
        private String type; // JSON, XML, TEXT
        @NotBlank
        private String template;

        // Constructors
        public TemplateItem() {}
        
        public TemplateItem(String name, String type, String template) {
            this.name = name;
            this.type = type;
            this.template = template;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTemplate() { return template; }
        public void setTemplate(String template) { this.template = template; }
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
