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

import java.util.HashMap;
import java.util.Map;

/**
 * File format detector based on file extensions.
 * 
 * This detector analyzes file names to determine the format based on common
 * file extensions. It provides high confidence for well-known extensions
 * and is typically the fastest detection method.
 * 
 * SUPPORTED FORMATS:
 * - JSON: .json, .jsonl, .ndjson
 * - XML: .xml, .xsd, .soap
 * - CSV: .csv, .tsv, .psv
 * - Text: .txt, .log, .dat
 * 
 * DESIGN PRINCIPLES:
 * - Fast and lightweight detection
 * - High confidence for known extensions
 * - Configurable extension mappings
 * - Case-insensitive matching
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ExtensionBasedFileFormatDetector implements FileFormatDetector {
    
    private static final String DETECTOR_NAME = "extension-based";
    private static final int PRIORITY = 1; // High priority for fast detection
    
    // Default extension mappings
    private final Map<String, String> extensionMappings;
    
    public ExtensionBasedFileFormatDetector() {
        this.extensionMappings = createDefaultExtensionMappings();
    }
    
    public ExtensionBasedFileFormatDetector(Map<String, String> customMappings) {
        this.extensionMappings = new HashMap<>(customMappings);
    }
    
    @Override
    public boolean canDetect(ClassificationContext context) {
        String fileName = context.getFileName();
        return fileName != null && fileName.contains(".");
    }
    
    @Override
    public FileFormatResult detect(ClassificationContext context) {
        String fileName = context.getFileName();
        if (fileName == null || !fileName.contains(".")) {
            return FileFormatResult.unknown();
        }
        
        // Extract file extension
        String extension = extractExtension(fileName);
        if (extension == null || extension.isEmpty()) {
            return FileFormatResult.unknown();
        }
        
        // Look up format by extension
        String format = extensionMappings.get(extension.toLowerCase());
        if (format != null) {
            // High confidence for known extensions
            return FileFormatResult.of(format, 0.95, FileFormatResult.METHOD_EXTENSION);
        }
        
        return FileFormatResult.unknown();
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    public String getName() {
        return DETECTOR_NAME;
    }
    
    /**
     * Extracts the file extension from a file name.
     */
    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return null;
        }
        
        return fileName.substring(lastDotIndex + 1);
    }
    
    /**
     * Creates the default extension to format mappings.
     */
    private Map<String, String> createDefaultExtensionMappings() {
        Map<String, String> mappings = new HashMap<>();
        
        // JSON formats
        mappings.put("json", "json");
        mappings.put("jsonl", "json");
        mappings.put("ndjson", "json");
        
        // XML formats
        mappings.put("xml", "xml");
        mappings.put("xsd", "xml");
        mappings.put("soap", "xml");
        mappings.put("wsdl", "xml");
        
        // CSV formats
        mappings.put("csv", "csv");
        mappings.put("tsv", "csv");
        mappings.put("psv", "csv");
        mappings.put("tab", "csv");
        
        // Text formats
        mappings.put("txt", "text");
        mappings.put("log", "text");
        mappings.put("dat", "text");
        mappings.put("data", "text");
        
        return mappings;
    }
    
    /**
     * Gets the supported extensions for a given format.
     */
    public String[] getSupportedExtensions(String format) {
        return extensionMappings.entrySet().stream()
            .filter(entry -> format.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .toArray(String[]::new);
    }
    
    /**
     * Adds a custom extension mapping.
     */
    public void addExtensionMapping(String extension, String format) {
        extensionMappings.put(extension.toLowerCase(), format);
    }
    
    @Override
    public String toString() {
        return "ExtensionBasedFileFormatDetector{" +
                "name='" + DETECTOR_NAME + '\'' +
                ", priority=" + PRIORITY +
                ", supportedExtensions=" + extensionMappings.size() +
                '}';
    }
}
