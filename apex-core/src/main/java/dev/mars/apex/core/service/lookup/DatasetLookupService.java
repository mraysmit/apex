package dev.mars.apex.core.service.lookup;

import dev.mars.apex.core.config.model.YamlEnrichment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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

/**
 * LookupService implementation that uses YAML-defined datasets.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class DatasetLookupService extends LookupService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatasetLookupService.class);
    
    private final Map<String, Map<String, Object>> datasetMap;
    private final Map<String, List<Map<String, Object>>> multiRowDatasetMap;
    private final String keyField;
    private final Map<String, Object> defaultValues;
    private final YamlEnrichment.LookupDataset datasetConfig;
    
    /**
     * Create a DatasetLookupService from a LookupDataset configuration.
     * 
     * @param serviceName The name of the service
     * @param dataset The dataset configuration
     */
    public DatasetLookupService(String serviceName, YamlEnrichment.LookupDataset dataset) {
        super(serviceName, extractKeys(dataset));
        
        this.datasetConfig = dataset;
        this.keyField = dataset.getKeyField();
        this.defaultValues = dataset.getDefaultValues() != null ? 
                           new HashMap<>(dataset.getDefaultValues()) : new HashMap<>();
        this.datasetMap = buildDatasetMap(dataset);
        this.multiRowDatasetMap = buildMultiRowDatasetMap(dataset);
        
        logger.info("Created DatasetLookupService '" + serviceName + "' with " + 
                   datasetMap.size() + " records, key field: " + keyField);
    }
    
    /**
     * Extract lookup keys from the dataset for validation purposes.
     * 
     * @param dataset The dataset configuration
     * @return List of lookup keys
     */
    private static List<String> extractKeys(YamlEnrichment.LookupDataset dataset) {
        if (dataset.getData() == null || dataset.getData().isEmpty()) {
            return new ArrayList<>();
        }
        
        String keyField = dataset.getKeyField();
        if (keyField == null) {
            throw new IllegalArgumentException("Key field must be specified for dataset");
        }
        
        return dataset.getData().stream()
            .map(record -> record.get(keyField))
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.toList());
    }
    
    /**
     * Build the internal dataset map for fast lookups.
     * 
     * @param dataset The dataset configuration
     * @return Map of key -> record data
     */
    private Map<String, Map<String, Object>> buildDatasetMap(YamlEnrichment.LookupDataset dataset) {
        Map<String, Map<String, Object>> map = new HashMap<>();
        
        if (dataset.getData() == null || dataset.getData().isEmpty()) {
            // Don't log warning for database/rest-api types - they use wrapper pattern with intentionally empty datasets
            String datasetType = dataset.getType();
            if (datasetType == null || 
                (!datasetType.equalsIgnoreCase("database") && 
                 !datasetType.equalsIgnoreCase("rest-api"))) {
                logger.error("Dataset has no data records - configuration error for type: {}", datasetType);
            }
            return map;
        }
        
        String keyField = dataset.getKeyField();
        if (keyField == null) {
            throw new IllegalArgumentException("Key field must be specified for dataset");
        }
        
        for (Map<String, Object> record : dataset.getData()) {
            Object keyValue = record.get(keyField);
            if (keyValue != null) {
                String key = keyValue.toString();
                
                // Create a copy of the record without the key field (to avoid duplication)
                Map<String, Object> recordData = new HashMap<>(record);
                
                map.put(key, recordData);
                logger.trace("Added dataset record: " + key + " -> " + recordData);
            } else {
                logger.warn("Record missing key field '" + keyField + "': " + record);
            }
        }
        
        return map;
    }
    
    @Override
    public Object transform(Object key) {
        if (key == null) {
            logger.debug("Lookup key is null, returning default values");
            return defaultValues.isEmpty() ? null : new HashMap<>(defaultValues);
        }
        
        String keyString = key.toString();
        Map<String, Object> record = datasetMap.get(keyString);
        
        if (record != null) {
            // Merge record data with default values (record data takes precedence)
            Map<String, Object> result = new HashMap<>(defaultValues);
            result.putAll(record);
            
            logger.trace("Dataset lookup successful for key '" + keyString + "': " + result);
            return result;
        } else {
            logger.debug("No dataset record found for key '" + keyString + "', returning default values");
            return defaultValues.isEmpty() ? null : new HashMap<>(defaultValues);
        }
    }
    
    /**
     * Return ALL matching records for the given key as a List.
     * For inline datasets, multiple records can share the same key-field value.
     *
     * @param key The lookup key
     * @return List of matching records, or empty list if no matches
     */
    @Override
    public List<Map<String, Object>> transformAll(Object key) {
        if (key == null) {
            logger.debug("Lookup key is null for transformAll, returning empty list");
            return new ArrayList<>();
        }

        String keyString = key.toString();
        List<Map<String, Object>> records = multiRowDatasetMap.get(keyString);

        if (records != null && !records.isEmpty()) {
            // Merge each record with default values
            List<Map<String, Object>> results = new ArrayList<>();
            for (Map<String, Object> record : records) {
                Map<String, Object> merged = new HashMap<>(defaultValues);
                merged.putAll(record);
                results.add(merged);
            }
            logger.debug("Dataset multi-row lookup for key '" + keyString + "' returned " + results.size() + " records");
            return results;
        } else {
            logger.debug("No dataset records found for key '" + keyString + "' in multi-row lookup");
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean validate(Object value) {
        if (value == null) {
            return false;
        }
        
        String keyString = value.toString();
        boolean isValid = datasetMap.containsKey(keyString);
        
        logger.trace("Dataset validation for key '" + keyString + "': " + isValid);
        return isValid;
    }
    
    /**
     * Get the dataset configuration.
     * 
     * @return The dataset configuration
     */
    public YamlEnrichment.LookupDataset getDatasetConfig() {
        return datasetConfig;
    }
    
    /**
     * Get the key field name.
     * 
     * @return The key field name
     */
    public String getKeyField() {
        return keyField;
    }
    
    /**
     * Get the default values.
     * 
     * @return Map of default values
     */
    public Map<String, Object> getDefaultValues() {
        return new HashMap<>(defaultValues);
    }
    
    /**
     * Get all dataset records.
     * 
     * @return Map of all dataset records
     */
    public Map<String, Map<String, Object>> getAllRecords() {
        return new HashMap<>(datasetMap);
    }
    
    /**
     * Build the multi-row dataset map that accumulates all records per key.
     *
     * @param dataset The dataset configuration
     * @return Map of key -> list of record data
     */
    private Map<String, List<Map<String, Object>>> buildMultiRowDatasetMap(YamlEnrichment.LookupDataset dataset) {
        Map<String, List<Map<String, Object>>> map = new HashMap<>();

        if (dataset.getData() == null || dataset.getData().isEmpty()) {
            return map;
        }

        String keyFld = dataset.getKeyField();
        if (keyFld == null) {
            return map;
        }

        for (Map<String, Object> record : dataset.getData()) {
            Object keyValue = record.get(keyFld);
            if (keyValue != null) {
                String key = keyValue.toString();
                Map<String, Object> recordData = new HashMap<>(record);
                map.computeIfAbsent(key, k -> new ArrayList<>()).add(recordData);
            }
        }

        return map;
    }
    
    /**
     * Get dataset statistics.
     * 
     * @return Map containing dataset statistics
     */
    public Map<String, Object> getDatasetStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("recordCount", datasetMap.size());
        stats.put("keyField", keyField);
        stats.put("hasDefaultValues", !defaultValues.isEmpty());
        stats.put("defaultValueCount", defaultValues.size());
        stats.put("datasetType", datasetConfig.getType());
        
        if (datasetConfig.getFilePath() != null) {
            stats.put("filePath", datasetConfig.getFilePath());
        }
        
        return stats;
    }
    
    @Override
    public String toString() {
        return "DatasetLookupService{" +
               "name='" + getName() + '\'' +
               ", recordCount=" + datasetMap.size() +
               ", keyField='" + keyField + '\'' +
               ", datasetType='" + datasetConfig.getType() + '\'' +
               '}';
    }
}
