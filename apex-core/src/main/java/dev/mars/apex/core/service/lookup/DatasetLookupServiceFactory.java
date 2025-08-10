package dev.mars.apex.core.service.lookup;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.service.enrichment.EnrichmentException;
import dev.mars.apex.core.service.data.external.file.CsvDataLoader;
import dev.mars.apex.core.config.datasource.FileFormatConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
 * Factory for creating DatasetLookupService instances from various dataset sources.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class DatasetLookupServiceFactory {
    
    private static final Logger LOGGER = Logger.getLogger(DatasetLookupServiceFactory.class.getName());
    
    /**
     * Create a DatasetLookupService from a LookupDataset configuration.
     * 
     * @param serviceName The name for the service
     * @param dataset The dataset configuration
     * @return A configured DatasetLookupService
     * @throws EnrichmentException if the dataset type is unsupported or configuration is invalid
     */
    public static DatasetLookupService createDatasetLookupService(String serviceName, 
                                                                  YamlEnrichment.LookupDataset dataset) {
        if (dataset == null) {
            throw new EnrichmentException("Dataset configuration cannot be null");
        }
        
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new EnrichmentException("Service name cannot be null or empty");
        }
        
        String datasetType = dataset.getType();
        if (datasetType == null || datasetType.trim().isEmpty()) {
            throw new EnrichmentException("Dataset type must be specified");
        }
        
        LOGGER.info("Creating DatasetLookupService '" + serviceName + "' with type: " + datasetType);
        
        switch (datasetType.toLowerCase()) {
            case "inline":
                return createInlineDatasetService(serviceName, dataset);
                
            case "yaml-file":
                return createYamlFileDatasetService(serviceName, dataset);
                
            case "csv-file":
                return createCsvFileDatasetService(serviceName, dataset);
                
            default:
                throw new EnrichmentException("Unsupported dataset type: " + datasetType + 
                                            ". Supported types: inline, yaml-file, csv-file");
        }
    }
    
    /**
     * Create a service for inline datasets.
     * 
     * @param serviceName The service name
     * @param dataset The dataset configuration
     * @return A configured DatasetLookupService
     */
    private static DatasetLookupService createInlineDatasetService(String serviceName, 
                                                                   YamlEnrichment.LookupDataset dataset) {
        LOGGER.fine("Creating inline dataset service: " + serviceName);
        
        if (dataset.getData() == null || dataset.getData().isEmpty()) {
            throw new EnrichmentException("Inline dataset must have data records");
        }
        
        if (dataset.getKeyField() == null || dataset.getKeyField().trim().isEmpty()) {
            throw new EnrichmentException("Inline dataset must specify a key field");
        }
        
        return new DatasetLookupService(serviceName, dataset);
    }
    
    /**
     * Create a service for YAML file datasets.
     * 
     * @param serviceName The service name
     * @param dataset The dataset configuration
     * @return A configured DatasetLookupService
     */
    private static DatasetLookupService createYamlFileDatasetService(String serviceName, 
                                                                     YamlEnrichment.LookupDataset dataset) {
        LOGGER.fine("Creating YAML file dataset service: " + serviceName);
        
        if (dataset.getFilePath() == null || dataset.getFilePath().trim().isEmpty()) {
            throw new EnrichmentException("YAML file dataset must specify a file path");
        }
        
        if (dataset.getKeyField() == null || dataset.getKeyField().trim().isEmpty()) {
            throw new EnrichmentException("YAML file dataset must specify a key field");
        }
        
        // Load data from YAML file
        YamlEnrichment.LookupDataset fileDataset = loadFromYamlFile(dataset);
        return new DatasetLookupService(serviceName, fileDataset);
    }
    
    /**
     * Create a service for CSV file datasets.
     * 
     * @param serviceName The service name
     * @param dataset The dataset configuration
     * @return A configured DatasetLookupService
     */
    private static DatasetLookupService createCsvFileDatasetService(String serviceName, 
                                                                    YamlEnrichment.LookupDataset dataset) {
        LOGGER.fine("Creating CSV file dataset service: " + serviceName);
        
        if (dataset.getFilePath() == null || dataset.getFilePath().trim().isEmpty()) {
            throw new EnrichmentException("CSV file dataset must specify a file path");
        }
        
        if (dataset.getKeyField() == null || dataset.getKeyField().trim().isEmpty()) {
            throw new EnrichmentException("CSV file dataset must specify a key field");
        }
        
        // Load data from CSV file
        YamlEnrichment.LookupDataset csvDataset = loadFromCsvFile(dataset);
        return new DatasetLookupService(serviceName, csvDataset);
    }
    
    /**
     * Load dataset from YAML file.
     *
     * @param dataset The dataset configuration
     * @return Dataset with loaded data
     */
    private static YamlEnrichment.LookupDataset loadFromYamlFile(YamlEnrichment.LookupDataset dataset) {
        try {
            LOGGER.info("Loading YAML dataset from file: " + dataset.getFilePath());

            // Create a copy of the dataset
            YamlEnrichment.LookupDataset fileDataset = new YamlEnrichment.LookupDataset();
            fileDataset.setType(dataset.getType());
            fileDataset.setFilePath(dataset.getFilePath());
            fileDataset.setKeyField(dataset.getKeyField());
            fileDataset.setDefaultValues(dataset.getDefaultValues());
            fileDataset.setCacheEnabled(dataset.getCacheEnabled());
            fileDataset.setCacheTtlSeconds(dataset.getCacheTtlSeconds());

            // Load actual data from YAML file
            Path yamlFilePath = Paths.get(dataset.getFilePath());
            if (!Files.exists(yamlFilePath)) {
                LOGGER.warning("YAML file not found: " + dataset.getFilePath() + ". Using empty dataset.");
                fileDataset.setData(Collections.emptyList());
                return fileDataset;
            }

            // For YAML files, we expect a simple structure with a list of maps
            // This is a basic implementation - could be enhanced for complex YAML structures
            try {
                // For now, return empty data as YAML parsing would require additional dependencies
                // In a full implementation, this would use a YAML parser like SnakeYAML to parse Files.readString(yamlFilePath)
                LOGGER.info("YAML file found but parsing not fully implemented. Using empty dataset for: " + dataset.getFilePath());
                fileDataset.setData(Collections.emptyList());

            } catch (Exception e) {
                LOGGER.warning("Failed to read YAML file: " + dataset.getFilePath() + ". Error: " + e.getMessage());
                fileDataset.setData(Collections.emptyList());
            }

            return fileDataset;

        } catch (Exception e) {
            LOGGER.severe("Failed to load YAML dataset from file: " + dataset.getFilePath() + ". Error: " + e.getMessage());

            // Return dataset with empty data on error
            YamlEnrichment.LookupDataset errorDataset = new YamlEnrichment.LookupDataset();
            errorDataset.setType(dataset.getType());
            errorDataset.setFilePath(dataset.getFilePath());
            errorDataset.setKeyField(dataset.getKeyField());
            errorDataset.setDefaultValues(dataset.getDefaultValues());
            errorDataset.setCacheEnabled(dataset.getCacheEnabled());
            errorDataset.setCacheTtlSeconds(dataset.getCacheTtlSeconds());
            errorDataset.setData(Collections.emptyList());

            return errorDataset;
        }
    }
    
    /**
     * Load dataset from CSV file.
     *
     * @param dataset The dataset configuration
     * @return Dataset with loaded data
     */
    private static YamlEnrichment.LookupDataset loadFromCsvFile(YamlEnrichment.LookupDataset dataset) {
        try {
            LOGGER.info("Loading CSV dataset from file: " + dataset.getFilePath());

            // Create a copy of the dataset
            YamlEnrichment.LookupDataset csvDataset = new YamlEnrichment.LookupDataset();
            csvDataset.setType(dataset.getType());
            csvDataset.setFilePath(dataset.getFilePath());
            csvDataset.setKeyField(dataset.getKeyField());
            csvDataset.setDefaultValues(dataset.getDefaultValues());
            csvDataset.setCacheEnabled(dataset.getCacheEnabled());
            csvDataset.setCacheTtlSeconds(dataset.getCacheTtlSeconds());

            // Load actual data from CSV file using CsvDataLoader
            Path csvFilePath = Paths.get(dataset.getFilePath());
            if (!Files.exists(csvFilePath)) {
                LOGGER.warning("CSV file not found: " + dataset.getFilePath() + ". Using empty dataset.");
                csvDataset.setData(Collections.emptyList());
                return csvDataset;
            }

            // Create a basic file format config for CSV
            FileFormatConfig formatConfig = new FileFormatConfig();
            formatConfig.setType("csv");
            formatConfig.setHeaderRow(true);
            formatConfig.setEncoding("UTF-8");
            formatConfig.setDelimiter(",");

            // Use CsvDataLoader to load the data
            CsvDataLoader csvLoader = new CsvDataLoader();
            List<Object> loadedData = csvLoader.loadData(csvFilePath, formatConfig);

            // Convert List<Object> to List<Map<String, Object>> for the dataset
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> mapData = (List<Map<String, Object>>) (List<?>) loadedData;
            csvDataset.setData(mapData);

            LOGGER.info("Successfully loaded " + loadedData.size() + " records from CSV file: " + dataset.getFilePath());

            return csvDataset;

        } catch (Exception e) {
            LOGGER.severe("Failed to load CSV dataset from file: " + dataset.getFilePath() + ". Error: " + e.getMessage());

            // Return dataset with empty data on error
            YamlEnrichment.LookupDataset errorDataset = new YamlEnrichment.LookupDataset();
            errorDataset.setType(dataset.getType());
            errorDataset.setFilePath(dataset.getFilePath());
            errorDataset.setKeyField(dataset.getKeyField());
            errorDataset.setDefaultValues(dataset.getDefaultValues());
            errorDataset.setCacheEnabled(dataset.getCacheEnabled());
            errorDataset.setCacheTtlSeconds(dataset.getCacheTtlSeconds());
            errorDataset.setData(Collections.emptyList());

            return errorDataset;
        }
    }
    
    /**
     * Validate dataset configuration.
     * 
     * @param dataset The dataset to validate
     * @throws EnrichmentException if the configuration is invalid
     */
    public static void validateDatasetConfiguration(YamlEnrichment.LookupDataset dataset) {
        if (dataset == null) {
            throw new EnrichmentException("Dataset configuration cannot be null");
        }
        
        String type = dataset.getType();
        if (type == null || type.trim().isEmpty()) {
            throw new EnrichmentException("Dataset type must be specified");
        }
        
        String keyField = dataset.getKeyField();
        if (keyField == null || keyField.trim().isEmpty()) {
            throw new EnrichmentException("Dataset key field must be specified");
        }
        
        switch (type.toLowerCase()) {
            case "inline":
                if (dataset.getData() == null || dataset.getData().isEmpty()) {
                    throw new EnrichmentException("Inline dataset must have data records");
                }
                break;
                
            case "yaml-file":
            case "csv-file":
                if (dataset.getFilePath() == null || dataset.getFilePath().trim().isEmpty()) {
                    throw new EnrichmentException("File-based dataset must specify a file path");
                }
                break;
                
            default:
                throw new EnrichmentException("Unsupported dataset type: " + type);
        }
        
        LOGGER.fine("Dataset configuration validation passed for type: " + type);
    }
}
