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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Content-based file format detector that analyzes the actual content structure.
 * 
 * This detector examines the content of input data to determine the format by
 * analyzing patterns, structure, and attempting to parse the content. It provides
 * higher accuracy than extension-based detection but is slower.
 * 
 * DETECTION STRATEGIES:
 * - Pattern matching for common format indicators
 * - Parse validation to confirm format validity
 * - Content structure analysis
 * - Confidence scoring based on multiple factors
 * 
 * SUPPORTED FORMATS:
 * - JSON: Objects, arrays, and primitive values
 * - XML: Well-formed XML documents with validation
 * - CSV: Comma-separated values with header detection
 * - Text: Plain text content
 * 
 * DESIGN PRINCIPLES:
 * - Uses existing APEX Jackson mappers for consistency
 * - Conservative confidence scoring
 * - Fast-fail for obviously invalid content
 * - Follows established APEX patterns
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ContentBasedFileFormatDetector implements FileFormatDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentBasedFileFormatDetector.class);
    
    private static final String DETECTOR_NAME = "content-based";
    private static final int PRIORITY = 2; // Lower priority than extension-based
    
    // Reuse existing APEX Jackson mappers for consistency
    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;
    
    public ContentBasedFileFormatDetector() {
        this.jsonMapper = new ObjectMapper();
        this.xmlMapper = new XmlMapper();
        logger.debug("Content-based file format detector initialized");
    }
    
    @Override
    public boolean canDetect(ClassificationContext context) {
        String content = context.getInputDataAsString();
        return content != null && !content.trim().isEmpty();
    }
    
    @Override
    public FileFormatResult detect(ClassificationContext context) {
        String content = context.getInputDataAsString();
        if (content == null || content.trim().isEmpty()) {
            return FileFormatResult.unknown();
        }
        
        String trimmed = content.trim();
        logger.debug("Analyzing content for format detection (length: {})", trimmed.length());
        
        // Try JSON detection first (most common in financial systems)
        FileFormatResult jsonResult = detectJson(trimmed);
        if (jsonResult.isConfident()) {
            logger.debug("Content detected as JSON with confidence: {}", jsonResult.getConfidence());
            return jsonResult;
        }
        
        // Try XML detection
        FileFormatResult xmlResult = detectXml(trimmed);
        if (xmlResult.isConfident()) {
            logger.debug("Content detected as XML with confidence: {}", xmlResult.getConfidence());
            return xmlResult;
        }
        
        // Try CSV detection
        FileFormatResult csvResult = detectCsv(trimmed);
        if (csvResult.isConfident()) {
            logger.debug("Content detected as CSV with confidence: {}", csvResult.getConfidence());
            return csvResult;
        }
        
        // Default to text if content exists but no specific format detected
        if (trimmed.length() > 0) {
            logger.debug("Content detected as text (fallback)");
            return FileFormatResult.of("text", 0.3, FileFormatResult.METHOD_CONTENT);
        }
        
        return FileFormatResult.unknown();
    }
    
    /**
     * Detect JSON format using pattern matching and parse validation.
     */
    private FileFormatResult detectJson(String content) {
        double confidence = 0.0;
        
        // Pattern-based detection
        if ((content.startsWith("{") && content.endsWith("}")) ||
            (content.startsWith("[") && content.endsWith("]"))) {
            confidence += 0.4; // Basic structure match
            
            try {
                // Parse validation using existing APEX Jackson mapper
                jsonMapper.readTree(content);
                confidence += 0.5; // Valid JSON structure
                
                // Additional confidence factors
                if (content.contains("\"") && content.contains(":")) {
                    confidence += 0.1; // Contains key-value pairs
                }
                
                return FileFormatResult.of("json", Math.min(confidence, 0.95), FileFormatResult.METHOD_CONTENT);

            } catch (Exception e) {
                logger.debug("JSON parse validation failed: {}", e.getMessage());
                // Return lower confidence for structure match without valid parsing
                return FileFormatResult.of("json", confidence * 0.5, FileFormatResult.METHOD_CONTENT);
            }
        }
        
        return FileFormatResult.unknown();
    }
    
    /**
     * Detect XML format using pattern matching and parse validation.
     */
    private FileFormatResult detectXml(String content) {
        double confidence = 0.0;
        
        // Pattern-based detection
        if (content.startsWith("<") && content.endsWith(">")) {
            confidence += 0.4; // Basic structure match
            
            // Check for XML declaration
            if (content.startsWith("<?xml")) {
                confidence += 0.2; // XML declaration present
            }
            
            try {
                // Parse validation using existing APEX XML mapper
                xmlMapper.readTree(content);
                confidence += 0.4; // Valid XML structure
                
                return FileFormatResult.of("xml", Math.min(confidence, 0.95), FileFormatResult.METHOD_CONTENT);

            } catch (Exception e) {
                logger.debug("XML parse validation failed: {}", e.getMessage());
                // Return lower confidence for structure match without valid parsing
                return FileFormatResult.of("xml", confidence * 0.5, FileFormatResult.METHOD_CONTENT);
            }
        }
        
        return FileFormatResult.unknown();
    }
    
    /**
     * Detect CSV format using pattern analysis.
     */
    private FileFormatResult detectCsv(String content) {
        double confidence = 0.0;
        
        // Split into lines for analysis
        String[] lines = content.split("\\r?\\n");
        if (lines.length < 2) {
            return FileFormatResult.unknown(); // Need at least 2 lines for CSV
        }
        
        // Analyze first few lines for CSV patterns
        int consistentCommaCount = 0;
        int expectedCommas = -1;
        
        for (int i = 0; i < Math.min(lines.length, 5); i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            
            int commaCount = countOccurrences(line, ',');
            if (commaCount > 0) {
                if (expectedCommas == -1) {
                    expectedCommas = commaCount;
                    consistentCommaCount = 1;
                } else if (commaCount == expectedCommas) {
                    consistentCommaCount++;
                }
            }
        }
        
        // Calculate confidence based on consistency
        if (consistentCommaCount >= 2 && expectedCommas > 0) {
            confidence = 0.6 + (consistentCommaCount * 0.1);
            
            // Additional confidence for quoted fields
            if (content.contains("\"")) {
                confidence += 0.1;
            }
            
            return FileFormatResult.of("csv", Math.min(confidence, 0.9), FileFormatResult.METHOD_CONTENT);
        }
        
        return FileFormatResult.unknown();
    }
    
    /**
     * Count occurrences of a character in a string.
     */
    private int countOccurrences(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    public String getName() {
        return DETECTOR_NAME;
    }
    
    @Override
    public String toString() {
        return "ContentBasedFileFormatDetector{" +
                "name='" + DETECTOR_NAME + '\'' +
                ", priority=" + PRIORITY +
                '}';
    }
}
