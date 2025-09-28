package dev.mars.apex.core.service.classification;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Content classifier that analyzes message structure and content patterns.
 * 
 * This classifier examines the parsed content to identify message types,
 * data patterns, and business context based on field names, values, and
 * structure. It uses simple JSON path-like analysis for Phase 1.2.
 * 
 * CLASSIFICATION STRATEGIES:
 * - Field name pattern matching
 * - Value pattern analysis
 * - Message structure recognition
 * - Business context identification
 * 
 * SUPPORTED PATTERNS:
 * - Financial message types (TRADE, SETTLEMENT, POSITION)
 * - Instrument types (OTC_OPTION, COMMODITY_SWAP, etc.)
 * - Data record types based on field patterns
 * - Generic content classification
 * 
 * DESIGN PRINCIPLES:
 * - Uses existing APEX Jackson patterns
 * - Simple and fast classification logic
 * - Extensible pattern matching system
 * - Conservative confidence scoring
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ContentClassifier {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentClassifier.class);
    
    private final ObjectMapper jsonMapper;
    
    // Classification patterns for financial messages
    private final Map<String, String> messageTypePatterns;
    private final Map<String, String> instrumentTypePatterns;
    private final Map<String, String> dataTypePatterns;
    
    public ContentClassifier() {
        this.jsonMapper = new ObjectMapper();
        this.messageTypePatterns = createMessageTypePatterns();
        this.instrumentTypePatterns = createInstrumentTypePatterns();
        this.dataTypePatterns = createDataTypePatterns();
        logger.debug("Content classifier initialized with {} message patterns, {} instrument patterns, {} data patterns",
                    messageTypePatterns.size(), instrumentTypePatterns.size(), dataTypePatterns.size());
    }
    
    /**
     * Classify content based on structure and patterns.
     * 
     * @param fileFormat the detected file format (json, xml, csv, text)
     * @param content the raw content string
     * @param parsedData the parsed data object (if available)
     * @return content classification result
     */
    public ContentClassificationResult classify(String fileFormat, String content, Object parsedData) {
        if (content == null || content.trim().isEmpty()) {
            return ContentClassificationResult.unknown();
        }
        
        logger.debug("Classifying content with format: {}", fileFormat);
        
        try {
            switch (fileFormat.toLowerCase()) {
                case "json":
                    return classifyJsonContent(content, parsedData);
                case "xml":
                    return classifyXmlContent(content, parsedData);
                case "csv":
                    return classifyCsvContent(content, parsedData);
                default:
                    return classifyTextContent(content, parsedData);
            }
        } catch (Exception e) {
            logger.warn("Content classification failed: {}", e.getMessage());
            return ContentClassificationResult.unknown();
        }
    }
    
    /**
     * Classify JSON content using field analysis.
     */
    private ContentClassificationResult classifyJsonContent(String content, Object parsedData) {
        try {
            JsonNode jsonNode = jsonMapper.readTree(content);
            
            // Check for message type field
            String messageType = extractJsonField(jsonNode, "messageType", "type", "msgType");
            if (messageType != null) {
                String classification = messageTypePatterns.get(messageType.toUpperCase());
                if (classification != null) {
                    logger.debug("JSON classified by messageType: {} -> {}", messageType, classification);
                    return ContentClassificationResult.classified(classification, 0.9, "messageType=" + messageType);
                }
            }
            
            // Check for instrument type
            String instrumentType = extractJsonField(jsonNode, "instrument.type", "instrumentType", "assetType");
            if (instrumentType != null) {
                String classification = instrumentTypePatterns.get(instrumentType.toUpperCase());
                if (classification != null) {
                    logger.debug("JSON classified by instrumentType: {} -> {}", instrumentType, classification);
                    return ContentClassificationResult.classified(classification, 0.8, "instrumentType=" + instrumentType);
                }
            }
            
            // Check for data type patterns based on field presence
            String dataTypeClassification = classifyByFieldPatterns(jsonNode);
            if (dataTypeClassification != null) {
                logger.debug("JSON classified by field patterns: {}", dataTypeClassification);
                return ContentClassificationResult.classified(dataTypeClassification, 0.7, "fieldPattern");
            }
            
            // Default JSON classification
            return ContentClassificationResult.classified("json-data", 0.5, "generic");
            
        } catch (Exception e) {
            logger.debug("JSON content classification failed: {}", e.getMessage());
            return ContentClassificationResult.unknown();
        }
    }
    
    /**
     * Classify XML content (simplified for Phase 1.2).
     */
    private ContentClassificationResult classifyXmlContent(String content, Object parsedData) {
        // Simple XML classification based on root element
        if (content.contains("<trade") || content.contains("<Trade")) {
            return ContentClassificationResult.classified("trade-xml", 0.8, "rootElement=trade");
        } else if (content.contains("<position") || content.contains("<Position")) {
            return ContentClassificationResult.classified("position-xml", 0.8, "rootElement=position");
        } else if (content.contains("<settlement") || content.contains("<Settlement")) {
            return ContentClassificationResult.classified("settlement-xml", 0.8, "rootElement=settlement");
        }
        
        return ContentClassificationResult.classified("xml-data", 0.5, "generic");
    }
    
    /**
     * Classify CSV content based on headers.
     */
    private ContentClassificationResult classifyCsvContent(String content, Object parsedData) {
        String[] lines = content.split("\\r?\\n");
        if (lines.length > 0) {
            String header = lines[0].toLowerCase();
            
            if (header.contains("tradeid") || header.contains("trade_id")) {
                return ContentClassificationResult.classified("trade-csv", 0.8, "header=tradeId");
            } else if (header.contains("positionid") || header.contains("position_id")) {
                return ContentClassificationResult.classified("position-csv", 0.8, "header=positionId");
            } else if (header.contains("price") && header.contains("quantity")) {
                return ContentClassificationResult.classified("market-data-csv", 0.7, "header=priceQuantity");
            }
        }
        
        return ContentClassificationResult.classified("csv-data", 0.5, "generic");
    }
    
    /**
     * Classify text content (basic pattern matching).
     */
    private ContentClassificationResult classifyTextContent(String content, Object parsedData) {
        String lowerContent = content.toLowerCase();
        
        if (lowerContent.contains("trade") && lowerContent.contains("settlement")) {
            return ContentClassificationResult.classified("trade-text", 0.6, "keywords=trade,settlement");
        } else if (lowerContent.contains("position") && lowerContent.contains("portfolio")) {
            return ContentClassificationResult.classified("position-text", 0.6, "keywords=position,portfolio");
        }
        
        return ContentClassificationResult.classified("text-data", 0.4, "generic");
    }
    
    /**
     * Extract field value from JSON using multiple possible field names.
     */
    private String extractJsonField(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode fieldNode = node.at("/" + fieldName.replace(".", "/"));
            if (fieldNode != null && !fieldNode.isMissingNode() && fieldNode.isTextual()) {
                return fieldNode.asText();
            }
        }
        return null;
    }
    
    /**
     * Classify JSON by field patterns.
     */
    private String classifyByFieldPatterns(JsonNode node) {
        // Check for trade-related fields
        if (hasFields(node, "tradeId", "counterparty", "notional") ||
            hasFields(node, "trade_id", "amount", "currency")) {
            return "trade-data";
        }
        
        // Check for position-related fields
        if (hasFields(node, "positionId", "portfolio", "quantity") ||
            hasFields(node, "position_id", "holdings", "market_value")) {
            return "position-data";
        }
        
        // Check for market data fields
        if (hasFields(node, "symbol", "price", "timestamp") ||
            hasFields(node, "ticker", "bid", "ask")) {
            return "market-data";
        }
        
        return null;
    }
    
    /**
     * Check if JSON node has specified fields.
     */
    private boolean hasFields(JsonNode node, String... fieldNames) {
        int foundCount = 0;
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) || node.at("/" + fieldName).isValueNode()) {
                foundCount++;
            }
        }
        return foundCount >= fieldNames.length - 1; // Allow one missing field
    }
    
    /**
     * Create message type classification patterns.
     */
    private Map<String, String> createMessageTypePatterns() {
        Map<String, String> patterns = new HashMap<>();
        patterns.put("TRADE", "trade-message");
        patterns.put("SETTLEMENT", "settlement-message");
        patterns.put("POSITION", "position-message");
        patterns.put("MARKET_DATA", "market-data-message");
        patterns.put("RISK", "risk-message");
        patterns.put("COMPLIANCE", "compliance-message");
        return patterns;
    }
    
    /**
     * Create instrument type classification patterns.
     */
    private Map<String, String> createInstrumentTypePatterns() {
        Map<String, String> patterns = new HashMap<>();
        patterns.put("OTC_OPTION", "otc-option-instrument");
        patterns.put("COMMODITY_SWAP", "commodity-swap-instrument");
        patterns.put("EQUITY", "equity-instrument");
        patterns.put("BOND", "bond-instrument");
        patterns.put("FX", "fx-instrument");
        patterns.put("DERIVATIVE", "derivative-instrument");
        return patterns;
    }
    
    /**
     * Create data type classification patterns.
     */
    private Map<String, String> createDataTypePatterns() {
        Map<String, String> patterns = new HashMap<>();
        patterns.put("REFERENCE_DATA", "reference-data");
        patterns.put("TRANSACTION_DATA", "transaction-data");
        patterns.put("POSITION_DATA", "position-data");
        patterns.put("MARKET_DATA", "market-data");
        patterns.put("RISK_DATA", "risk-data");
        return patterns;
    }
}

/**
 * Result of content classification.
 */
class ContentClassificationResult {
    
    private final boolean successful;
    private final String contentType;
    private final double confidence;
    private final String reason;
    
    private ContentClassificationResult(boolean successful, String contentType, double confidence, String reason) {
        this.successful = successful;
        this.contentType = contentType;
        this.confidence = confidence;
        this.reason = reason;
    }
    
    public static ContentClassificationResult classified(String contentType, double confidence, String reason) {
        return new ContentClassificationResult(true, contentType, confidence, reason);
    }
    
    public static ContentClassificationResult unknown() {
        return new ContentClassificationResult(false, "unknown", 0.0, "no-pattern-match");
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public String toString() {
        return "ContentClassificationResult{" +
                "successful=" + successful +
                ", contentType='" + contentType + '\'' +
                ", confidence=" + confidence +
                ", reason='" + reason + '\'' +
                '}';
    }
}
