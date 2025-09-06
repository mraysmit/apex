package dev.mars.apex.core.service.lookup;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.service.enrichment.EnrichmentException;
import dev.mars.apex.core.service.data.external.file.CsvDataLoader;
import dev.mars.apex.core.service.data.external.file.JsonDataLoader;
import dev.mars.apex.core.service.data.external.file.XmlDataLoader;
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
* This class is part of the APEX A powerful expression processor for Java applications.
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
        return createDatasetLookupService(serviceName, dataset, null);
    }

    /**
     * Create a DatasetLookupService from a LookupDataset configuration with full YAML configuration context.
     * This method is required for database lookups that need access to dataSources configuration.
     *
     * @param serviceName The name for the service
     * @param dataset The dataset configuration
     * @param configuration The full YAML configuration (required for database lookups)
     * @return A configured DatasetLookupService
     * @throws EnrichmentException if the dataset type is unsupported or configuration is invalid
     */
    public static DatasetLookupService createDatasetLookupService(String serviceName,
                                                                  YamlEnrichment.LookupDataset dataset,
                                                                  dev.mars.apex.core.config.yaml.YamlRuleConfiguration configuration) {
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

            case "file-system":
                return createFileSystemDatasetService(serviceName, dataset);

            case "database":
                if (configuration == null) {
                    throw new EnrichmentException("Database lookups require configuration context. Use createDatasetLookupService(serviceName, dataset, configuration) method instead.");
                }
                return createDatabaseDatasetService(serviceName, dataset, configuration);

            default:
                throw new EnrichmentException("Unsupported dataset type: " + datasetType +
                                            ". Supported types: inline, yaml-file, csv-file, file-system, database");
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
     * Create a service for database datasets.
     *
     * @param serviceName The service name
     * @param dataset The dataset configuration
     * @param configuration The full YAML configuration containing dataSources
     * @return A configured DatasetLookupService
     */
    private static DatasetLookupService createDatabaseDatasetService(String serviceName,
                                                                     YamlEnrichment.LookupDataset dataset,
                                                                     dev.mars.apex.core.config.yaml.YamlRuleConfiguration configuration) {
        LOGGER.fine("Creating database dataset service: " + serviceName);

        // Validate database-specific configuration
        // Support both connection-name (traditional) and data-source-ref (external reference)
        String connectionName = dataset.getConnectionName();
        String dataSourceRef = dataset.getDataSourceRef();

        if ((connectionName == null || connectionName.trim().isEmpty()) &&
            (dataSourceRef == null || dataSourceRef.trim().isEmpty())) {
            throw new EnrichmentException("Database dataset must specify either a connection-name or data-source-ref");
        }

        // For external data-source references, use the reference name as connection name
        if (dataSourceRef != null && !dataSourceRef.trim().isEmpty()) {
            connectionName = dataSourceRef;
        }

        // Validate query configuration
        // Support both inline query and query-ref (named query from external data-source)
        String query = dataset.getQuery();
        String queryRef = dataset.getQueryRef();

        if ((query == null || query.trim().isEmpty()) &&
            (queryRef == null || queryRef.trim().isEmpty())) {
            throw new EnrichmentException("Database dataset must specify either a query or query-ref");
        }

        // For query references, resolve the named query from the data-source configuration
        if (queryRef != null && !queryRef.trim().isEmpty()) {
            query = resolveNamedQuery(connectionName, queryRef, configuration);
        }

        // Find the data source configuration using the resolved connection name
        dev.mars.apex.core.config.yaml.YamlDataSource dataSourceConfig = null;
        if (configuration.getDataSources() != null) {
            for (dev.mars.apex.core.config.yaml.YamlDataSource ds : configuration.getDataSources()) {
                if (connectionName.equals(ds.getName())) {
                    dataSourceConfig = ds;
                    break;
                }
            }
        }

        if (dataSourceConfig == null) {
            throw new EnrichmentException("Data source not found: " + connectionName +
                                        ". Available data sources: " + getAvailableDataSourceNames(configuration));
        }

        try {
            // Create database data source using existing infrastructure
            dev.mars.apex.core.service.data.external.factory.DataSourceFactory factory =
                dev.mars.apex.core.service.data.external.factory.DataSourceFactory.getInstance();

            dev.mars.apex.core.service.data.external.ExternalDataSource dataSource =
                factory.createDataSource(dataSourceConfig.toDataSourceConfiguration());

            // Extract parameter field names from dataset configuration
            java.util.List<String> parameterFields = new java.util.ArrayList<>();
            if (dataset.getParameters() != null) {
                for (YamlEnrichment.LookupDataset.ParameterMapping param : dataset.getParameters()) {
                    parameterFields.add(param.getField());
                }
            }

            // Create database lookup service using resolved query
            DatabaseLookupService databaseService = new DatabaseLookupService(
                serviceName,
                dataSource,
                query,  // Use resolved query (either inline or from named query reference)
                parameterFields,
                dataset.getDefaultValues()
            );

            // Create a simple wrapper that delegates to the database service
            return new DatabaseDatasetLookupService(serviceName, databaseService, dataset);

        } catch (Exception e) {
            throw new EnrichmentException("Failed to create database dataset service '" + serviceName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Get available data source names for error messages.
     */
    private static String getAvailableDataSourceNames(dev.mars.apex.core.config.yaml.YamlRuleConfiguration configuration) {
        if (configuration.getDataSources() == null || configuration.getDataSources().isEmpty()) {
            return "none";
        }

        return configuration.getDataSources().stream()
            .map(dev.mars.apex.core.config.yaml.YamlDataSource::getName)
            .collect(java.util.stream.Collectors.joining(", "));
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
     * Create a file-system dataset lookup service.
     *
     * @param serviceName The name for the service
     * @param dataset The dataset configuration
     * @return A configured DatasetLookupService
     */
    private static DatasetLookupService createFileSystemDatasetService(String serviceName,
                                                                       YamlEnrichment.LookupDataset dataset) {
        LOGGER.info("Creating file-system dataset lookup service: " + serviceName);

        // Load data from file system
        YamlEnrichment.LookupDataset fileDataset = loadFromFileSystem(dataset);

        // Create and return the service
        return new DatasetLookupService(serviceName, fileDataset);
    }

    /**
     * Load dataset from file system (JSON, XML, or other formats).
     *
     * @param dataset The dataset configuration
     * @return Dataset with loaded data
     */
    private static YamlEnrichment.LookupDataset loadFromFileSystem(YamlEnrichment.LookupDataset dataset) {
        try {
            LOGGER.info("Loading file-system dataset from file: " + dataset.getFilePath());

            // Create a copy of the dataset
            YamlEnrichment.LookupDataset fileDataset = new YamlEnrichment.LookupDataset();
            fileDataset.setType(dataset.getType());
            fileDataset.setFilePath(dataset.getFilePath());
            fileDataset.setKeyField(dataset.getKeyField());
            fileDataset.setDefaultValues(dataset.getDefaultValues());
            fileDataset.setCacheEnabled(dataset.getCacheEnabled());
            fileDataset.setCacheTtlSeconds(dataset.getCacheTtlSeconds());

            // Load actual data from file
            Path filePath = Paths.get(dataset.getFilePath());
            if (!Files.exists(filePath)) {
                LOGGER.warning("File not found: " + dataset.getFilePath() + ". Using empty dataset.");
                fileDataset.setData(Collections.emptyList());
                return fileDataset;
            }

            // Determine file format and load data
            String fileName = filePath.getFileName().toString().toLowerCase();
            List<Map<String, Object>> loadedData;

            if (fileName.endsWith(".json")) {
                loadedData = loadJsonFile(filePath);
            } else if (fileName.endsWith(".xml")) {
                loadedData = loadXmlFile(filePath);
            } else {
                throw new EnrichmentException("Unsupported file format for file-system dataset: " + fileName +
                                            ". Supported formats: .json, .xml");
            }

            fileDataset.setData(loadedData);
            LOGGER.info("Successfully loaded " + loadedData.size() + " records from file: " + dataset.getFilePath());

            return fileDataset;

        } catch (Exception e) {
            LOGGER.severe("Failed to load file-system dataset from file: " + dataset.getFilePath() + ". Error: " + e.getMessage());

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
     * Load data from JSON file.
     *
     * @param filePath Path to the JSON file
     * @return List of data records
     * @throws Exception if loading fails
     */
    private static List<Map<String, Object>> loadJsonFile(Path filePath) throws Exception {
        LOGGER.fine("Loading JSON file: " + filePath);

        // Use JsonDataLoader if available, otherwise use simple JSON parsing
        try {
            FileFormatConfig formatConfig = new FileFormatConfig();
            formatConfig.setType("json");
            formatConfig.setEncoding("UTF-8");

            JsonDataLoader jsonLoader = new JsonDataLoader();
            List<Object> loadedData = jsonLoader.loadData(filePath, formatConfig);

            // Convert to List<Map<String, Object>>
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> mapData = (List<Map<String, Object>>) (List<?>) loadedData;
            return mapData;

        } catch (Exception e) {
            LOGGER.warning("JsonDataLoader failed, falling back to simple JSON parsing: " + e.getMessage());
            return loadJsonFileSimple(filePath);
        }
    }

    /**
     * Load data from XML file.
     *
     * @param filePath Path to the XML file
     * @return List of data records
     * @throws Exception if loading fails
     */
    private static List<Map<String, Object>> loadXmlFile(Path filePath) throws Exception {
        LOGGER.fine("Loading XML file: " + filePath);

        // Use XmlDataLoader if available, otherwise use simple XML parsing
        try {
            FileFormatConfig formatConfig = new FileFormatConfig();
            formatConfig.setType("xml");
            formatConfig.setEncoding("UTF-8");

            XmlDataLoader xmlLoader = new XmlDataLoader();
            List<Object> loadedData = xmlLoader.loadData(filePath, formatConfig);

            // Convert to List<Map<String, Object>>
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> mapData = (List<Map<String, Object>>) (List<?>) loadedData;
            return mapData;

        } catch (Exception e) {
            LOGGER.warning("XmlDataLoader failed, falling back to simple XML parsing: " + e.getMessage());
            return loadXmlFileSimple(filePath);
        }
    }

    /**
     * Simple JSON file loading fallback.
     */
    private static List<Map<String, Object>> loadJsonFileSimple(Path filePath) throws Exception {
        // For now, return empty list - this can be enhanced later
        LOGGER.warning("Simple JSON parsing not yet implemented for: " + filePath);
        return Collections.emptyList();
    }

    /**
     * Simple XML file loading fallback.
     */
    private static List<Map<String, Object>> loadXmlFileSimple(Path filePath) throws Exception {
        // For now, return empty list - this can be enhanced later
        LOGGER.warning("Simple XML parsing not yet implemented for: " + filePath);
        return Collections.emptyList();
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
            case "file-system":
                if (dataset.getFilePath() == null || dataset.getFilePath().trim().isEmpty()) {
                    throw new EnrichmentException("File-based dataset must specify a file path");
                }
                break;

            default:
                throw new EnrichmentException("Unsupported dataset type: " + type);
        }
        
        LOGGER.fine("Dataset configuration validation passed for type: " + type);
    }

    /**
     * Resolve a named query from the data-source configuration.
     *
     * @param connectionName The connection/data-source name
     * @param queryRef The named query reference
     * @param configuration The YAML rule configuration containing data sources
     * @return The resolved SQL query string
     * @throws EnrichmentException if the named query cannot be resolved
     */
    private static String resolveNamedQuery(String connectionName, String queryRef,
                                          dev.mars.apex.core.config.yaml.YamlRuleConfiguration configuration) {
        LOGGER.fine("Resolving named query '" + queryRef + "' from data-source '" + connectionName + "'");

        // Find the data source configuration
        dev.mars.apex.core.config.yaml.YamlDataSource dataSourceConfig = null;
        if (configuration.getDataSources() != null) {
            for (dev.mars.apex.core.config.yaml.YamlDataSource ds : configuration.getDataSources()) {
                if (connectionName.equals(ds.getName())) {
                    dataSourceConfig = ds;
                    break;
                }
            }
        }

        if (dataSourceConfig == null) {
            throw new EnrichmentException("Data source not found for named query resolution: " + connectionName);
        }

        // Look for the named query in the data source queries
        if (dataSourceConfig.getQueries() == null || dataSourceConfig.getQueries().isEmpty()) {
            throw new EnrichmentException("Data source '" + connectionName + "' has no named queries defined");
        }

        String resolvedQuery = dataSourceConfig.getQueries().get(queryRef);
        if (resolvedQuery == null || resolvedQuery.trim().isEmpty()) {
            // List available queries for better error message
            String availableQueries = String.join(", ", dataSourceConfig.getQueries().keySet());
            throw new EnrichmentException("Named query '" + queryRef + "' not found in data-source '" +
                                        connectionName + "'. Available queries: " + availableQueries);
        }

        LOGGER.fine("Successfully resolved named query '" + queryRef + "' to: " +
                   (resolvedQuery.length() > 100 ? resolvedQuery.substring(0, 100) + "..." : resolvedQuery));

        return resolvedQuery;
    }

    /**
     * Simple wrapper that extends DatasetLookupService to delegate to DatabaseLookupService.
     * This allows database lookups to work with the existing DatasetLookupService interface.
     */
    private static class DatabaseDatasetLookupService extends DatasetLookupService {
        private final DatabaseLookupService databaseService;

        public DatabaseDatasetLookupService(String serviceName,
                                          DatabaseLookupService databaseService,
                                          YamlEnrichment.LookupDataset dataset) {
            // Create a minimal dataset configuration for the parent constructor
            super(serviceName, createEmptyDataset(dataset));
            this.databaseService = databaseService;
        }

        private static YamlEnrichment.LookupDataset createEmptyDataset(YamlEnrichment.LookupDataset original) {
            YamlEnrichment.LookupDataset emptyDataset = new YamlEnrichment.LookupDataset();
            emptyDataset.setType("database");
            emptyDataset.setKeyField("key"); // Placeholder
            emptyDataset.setData(java.util.Collections.emptyList());
            emptyDataset.setDefaultValues(original != null ? original.getDefaultValues() : null);
            return emptyDataset;
        }

        @Override
        public Object transform(Object key) {
            // Delegate to the database service
            return databaseService.transform(key);
        }

        @Override
        public java.util.Map<String, java.util.Map<String, Object>> getAllRecords() {
            // Database services don't preload all records
            return java.util.Collections.emptyMap();
        }

        @Override
        public String toString() {
            return "DatabaseDatasetLookupService{" +
                    "name='" + getName() + '\'' +
                    ", databaseService=" + databaseService +
                    '}';
        }
    }
}
