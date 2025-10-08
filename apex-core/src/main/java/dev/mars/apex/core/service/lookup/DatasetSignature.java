package dev.mars.apex.core.service.lookup;

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

import dev.mars.apex.core.config.yaml.YamlEnrichment.LookupDataset;

import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Generates unique signatures for datasets to enable caching and deduplication.
 *
 * <p>This class creates content-based signatures for datasets, allowing the system
 * to identify when two enrichments use the same dataset and share the underlying
 * DatasetLookupService instance.</p>
 *
 * <p><b>Why content-based signatures?</b> Previously, APEX created separate
 * DatasetLookupService instances for each enrichment, even when they used identical
 * datasets. This caused memory duplication and slower loading. Content-based signatures
 * enable deduplication: if two enrichments use the same data, they share one cached
 * DatasetLookupService instance.</p>
 *
 * <p><b>Signature generation strategy by dataset type:</b></p>
 * <ul>
 *   <li><b>inline</b>: MD5 hash of the data content + key field</li>
 *   <li><b>file-based</b>: File path + key field (same file = same data)</li>
 *   <li><b>database</b>: Connection + query + parameters + key field</li>
 *   <li><b>rest-api</b>: Connection + endpoint + operation + key field</li>
 * </ul>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DatasetSignature {
    
    private static final Logger LOGGER = Logger.getLogger(DatasetSignature.class.getName());
    
    private final String type;           // Dataset type: inline, file, database, rest-api
    private final String contentHash;    // Hash of dataset content
    private final String keyField;       // Lookup key field
    
    /**
     * Private constructor - use factory method from() instead.
     */
    private DatasetSignature(String type, String contentHash, String keyField) {
        this.type = type;
        this.contentHash = contentHash;
        this.keyField = keyField;
    }
    
    /**
     * Create a signature from a dataset configuration.
     * 
     * @param dataset The dataset configuration
     * @return DatasetSignature for the dataset
     * @throws IllegalArgumentException if dataset is null or invalid
     */
    public static DatasetSignature from(LookupDataset dataset) {
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset cannot be null");
        }
        
        String type = dataset.getType();
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Dataset type cannot be null or empty");
        }
        
        String keyField = dataset.getKeyField();
        if (keyField == null || keyField.trim().isEmpty()) {
            LOGGER.warning("Dataset key field is null or empty - signature may not be unique");
            keyField = "unknown";
        }
        
        String contentHash = generateContentHash(dataset);
        
        return new DatasetSignature(type.toLowerCase(), contentHash, keyField);
    }
    
    /**
     * Generate content hash based on dataset type.
     */
    private static String generateContentHash(LookupDataset dataset) {
        String type = dataset.getType().toLowerCase();
        
        switch (type) {
            case "inline":
                return hashInlineData(dataset.getData());
                
            case "yaml-file":
            case "csv-file":
            case "file-system":
                return hashFilePath(dataset.getFilePath());
                
            case "database":
                return hashDatabaseConfig(dataset);
                
            case "rest-api":
                return hashRestApiConfig(dataset);
                
            default:
                LOGGER.warning("Unknown dataset type: " + type + ". Using type as hash.");
                return type;
        }
    }
    
    /**
     * Generate hash for inline data.
     */
    private static String hashInlineData(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "empty";
        }
        
        try {
            // Create a stable string representation of the data
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> record : data) {
                sb.append(record.toString());
            }
            
            // Generate MD5 hash
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(sb.toString().getBytes("UTF-8"));
            
            // Convert to hex string (first 8 characters for brevity)
            String hexHash = bytesToHex(hash);
            return hexHash.substring(0, Math.min(8, hexHash.length()));
            
        } catch (Exception e) {
            LOGGER.warning("Failed to hash inline data: " + e.getMessage());
            return "hash-error-" + data.size();
        }
    }
    
    /**
     * Generate hash for file-based datasets.
     */
    private static String hashFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return "no-path";
        }
        
        // Use the file path as the signature (normalized)
        return filePath.replace("\\", "/").replace(" ", "_");
    }
    
    /**
     * Generate hash for database datasets.
     */
    private static String hashDatabaseConfig(LookupDataset dataset) {
        StringBuilder sb = new StringBuilder();
        
        // Include connection identifier
        if (dataset.getConnectionName() != null) {
            sb.append("conn:").append(dataset.getConnectionName()).append(";");
        }
        if (dataset.getDataSourceRef() != null) {
            sb.append("ds:").append(dataset.getDataSourceRef()).append(";");
        }
        
        // Include query
        if (dataset.getQuery() != null) {
            sb.append("q:").append(dataset.getQuery()).append(";");
        }
        if (dataset.getQueryRef() != null) {
            sb.append("qref:").append(dataset.getQueryRef()).append(";");
        }
        
        // Include parameters
        if (dataset.getParameters() != null && !dataset.getParameters().isEmpty()) {
            sb.append("params:");
            for (LookupDataset.ParameterMapping param : dataset.getParameters()) {
                sb.append(param.getField()).append(":").append(param.getType()).append(",");
            }
            sb.append(";");
        }
        
        if (sb.length() == 0) {
            return "db-no-config";
        }
        
        // Hash the configuration string
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(sb.toString().getBytes("UTF-8"));
            String hexHash = bytesToHex(hash);
            return hexHash.substring(0, Math.min(8, hexHash.length()));
        } catch (Exception e) {
            LOGGER.warning("Failed to hash database config: " + e.getMessage());
            return "db-hash-error";
        }
    }
    
    /**
     * Generate hash for REST API datasets.
     */
    private static String hashRestApiConfig(LookupDataset dataset) {
        StringBuilder sb = new StringBuilder();
        
        // Include connection/data source
        if (dataset.getConnectionName() != null) {
            sb.append("conn:").append(dataset.getConnectionName()).append(";");
        }
        if (dataset.getDataSourceRef() != null) {
            sb.append("ds:").append(dataset.getDataSourceRef()).append(";");
        }
        
        // Include endpoint
        if (dataset.getEndpoint() != null) {
            sb.append("ep:").append(dataset.getEndpoint()).append(";");
        }
        if (dataset.getOperationRef() != null) {
            sb.append("op:").append(dataset.getOperationRef()).append(";");
        }
        
        if (sb.length() == 0) {
            return "api-no-config";
        }
        
        // Hash the configuration string
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(sb.toString().getBytes("UTF-8"));
            String hexHash = bytesToHex(hash);
            return hexHash.substring(0, Math.min(8, hexHash.length()));
        } catch (Exception e) {
            LOGGER.warning("Failed to hash REST API config: " + e.getMessage());
            return "api-hash-error";
        }
    }
    
    /**
     * Convert byte array to hex string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * Get the dataset type.
     */
    public String getType() {
        return type;
    }
    
    /**
     * Get the content hash.
     */
    public String getContentHash() {
        return contentHash;
    }
    
    /**
     * Get the key field.
     */
    public String getKeyField() {
        return keyField;
    }
    
    /**
     * Get a short string representation suitable for service names.
     * Format: "type-hash"
     */
    public String toShortString() {
        return type + "-" + contentHash;
    }
    
    /**
     * Get full string representation.
     * Format: "type:hash:keyField"
     */
    @Override
    public String toString() {
        return type + ":" + contentHash + ":" + keyField;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatasetSignature)) return false;
        DatasetSignature that = (DatasetSignature) o;
        return Objects.equals(type, that.type) &&
               Objects.equals(contentHash, that.contentHash) &&
               Objects.equals(keyField, that.keyField);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, contentHash, keyField);
    }
}

