package dev.mars.apex.core.service.data.external.file;

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


import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.*;
import dev.mars.apex.core.service.data.external.cache.EnhancedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * File system implementation of ExternalDataSource.
 * 
 * This class provides file-based data access with support for multiple file formats,
 * file watching, caching, and automatic reloading when files change.
 * 
 * Supported file formats:
 * - CSV (Comma-separated values)
 * - JSON (JavaScript Object Notation)
 * - XML (Extensible Markup Language)
 * - Fixed-width text files
 * - Custom formats via pluggable parsers
 * 
 * Features:
 * - File watching and automatic reloading
 * - Multiple file format support
 * - Caching with TTL
 * - Pattern-based file discovery
 * - Health monitoring
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-30
 * @version 1.0
 */
public class FileSystemDataSource implements ExternalDataSource {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemDataSource.class);
    
    private DataSourceConfiguration configuration;
    private ConnectionStatus connectionStatus;
    private DataSourceMetrics metrics;
    
    // Data loaders for different file formats
    private final Map<String, DataLoader> dataLoaders = new HashMap<>();
    
    // Enhanced cache manager for file data
    private EnhancedCacheManager cacheManager;

    // File modification time tracking (for file monitoring)
    private final Map<String, Instant> fileModificationTimes = new ConcurrentHashMap<>();

    // File monitoring
    private ScheduledExecutorService fileMonitorExecutor;
    private volatile boolean monitoring = false;
    
    /**
     * Constructor with configuration.
     * 
     * @param configuration The data source configuration
     */
    public FileSystemDataSource(DataSourceConfiguration configuration) {
        this.configuration = configuration;
        this.connectionStatus = ConnectionStatus.notInitialized();
        this.metrics = new DataSourceMetrics();
        this.cacheManager = new EnhancedCacheManager(configuration);

        // Initialize data loaders
        initializeDataLoaders();
    }
    
    @Override
    public void initialize(DataSourceConfiguration config) throws DataSourceException {
        this.configuration = config;
        this.connectionStatus = ConnectionStatus.connecting();
        LOGGER.info("Initializing file system data source '{}': basePath='{}', filePattern='{}', pollingInterval={}s",
            sourceName(), configuredBasePath(), configuredFilePattern(),
            config.getConnection() != null ? config.getConnection().getPollingInterval() : null);
        
        try {
            // Validate base path exists
            Path basePath = Paths.get(config.getConnection().getBasePath());
            if (!Files.exists(basePath)) {
                throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                    "Base path does not exist: " + basePath);
            }
            
            if (!Files.isDirectory(basePath)) {
                throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                    "Base path is not a directory: " + basePath);
            }
            
            // Initialize file watcher if polling is enabled
            if (config.getConnection().getPollingInterval() != null && 
                config.getConnection().getPollingInterval() > 0) {
                startFileMonitoring();
            }
            
            // Load initial data
            loadInitialData();
            
            this.connectionStatus = ConnectionStatus.connected("File system data source initialized");
            LOGGER.info("File system data source '{}' initialized successfully: basePath='{}', filePattern='{}'",
                sourceName(), configuredBasePath(), configuredFilePattern());
            
        } catch (Exception e) {
            this.connectionStatus = ConnectionStatus.error("Initialization failed", e);
            LOGGER.error("Failed to initialize file system data source '{}': basePath='{}', filePattern='{}', error={}",
                sourceName(), configuredBasePath(), configuredFilePattern(), e.getMessage());
            LOGGER.debug("File system initialization failure for '{}':", sourceName(), e);
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to initialize file system data source", e, config.getName(), "initialize", false);
        }
    }
    
    @Override
    public DataSourceType getSourceType() {
        return DataSourceType.FILE_SYSTEM;
    }
    
    @Override
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }
    
    @Override
    public DataSourceMetrics getMetrics() {
        return metrics;
    }
    
    @Override
    public boolean isHealthy() {
        try {
            Path basePath = Paths.get(configuration.getConnection().getBasePath());
            return Files.exists(basePath) && Files.isDirectory(basePath);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean testConnection() {
        return isHealthy();
    }
    
    @Override
    public String getName() {
        return configuration != null ? configuration.getName() : "file-system-source";
    }
    
    @Override
    public String getDataType() {
        return configuration != null ? configuration.getSourceType() : "file-system";
    }
    
    @Override
    public boolean supportsDataType(String dataType) {
        return "file-system".equals(dataType) || 
               (configuration != null && configuration.getSourceType().equals(dataType));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(String dataType, Object... parameters) {
        long startTime = System.currentTimeMillis();
        LOGGER.debug("File system getData start for '{}': dataType='{}', parameters={}",
            sourceName(), dataType, Arrays.toString(parameters));
        
        try {
            // Check cache first if enabled
            if (cacheManager.isEnabled()) {
                String cacheKey = cacheManager.generateCacheKey(dataType, parameters);
                Object cached = cacheManager.get(cacheKey);
                if (cached != null) {
                    metrics.recordCacheHit();
                    metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
                    LOGGER.debug("File system cache hit for '{}': cacheKey='{}'", sourceName(), cacheKey);
                    return (T) cached;
                }
                metrics.recordCacheMiss();
                LOGGER.debug("File system cache miss for '{}': cacheKey='{}'", sourceName(), cacheKey);
            }

            // Load data from file
            Object result = loadDataFromFile(dataType, parameters);

            // Cache the result if caching is enabled
            if (cacheManager.isEnabled() && result != null) {
                String cacheKey = cacheManager.generateCacheKey(dataType, parameters);
                cacheManager.put(cacheKey, result);
                LOGGER.debug("Cached file system result for '{}': cacheKey='{}'", sourceName(), cacheKey);
            }

            metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
            LOGGER.debug("File system getData completed for '{}': dataType='{}', resultType='{}'",
                sourceName(), dataType, result != null ? result.getClass().getSimpleName() : "null");
            return (T) result;
            
        } catch (Exception e) {
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);
            LOGGER.error("Failed to get data from file system '{}': dataType='{}', parameters={}, error={}",
                sourceName(), dataType, Arrays.toString(parameters), e.getMessage());
            LOGGER.debug("File system getData failure for '{}':", sourceName(), e);
            return null;
        }
    }
    
    @Override
    public <T> List<T> query(String query, Map<String, Object> parameters) throws DataSourceException {
        try {
            LOGGER.debug("Executing file system query for '{}': query='{}', parameters={}", sourceName(), query, parameters);

            // First, check if this is a named query from configuration
            String actualQuery = resolveNamedQuery(query);
            LOGGER.debug("Resolved file system query for '{}': query='{}', resolved='{}'", sourceName(), query, actualQuery);

            // If it's a JSONPath query, execute it against loaded data
            if (actualQuery.startsWith("$.") || actualQuery.startsWith("$[")) {
                LOGGER.debug("Executing as JSONPath query: {}", actualQuery);
                return executeJsonPathQuery(actualQuery, parameters);
            }

            // If it's a SQL-like query for CSV, execute it against loaded data
            if (actualQuery.toUpperCase().startsWith("SELECT")) {
                LOGGER.debug("Executing file system query as CSV SQL for '{}': {}", sourceName(), actualQuery);
                return executeCsvQuery(actualQuery, parameters);
            }
            // Otherwise, treat it as a file pattern
            Path basePath = Paths.get(configuration.getConnection().getBasePath());
            List<Path> matchingFiles = findMatchingFiles(basePath, actualQuery);

            List<T> results = new ArrayList<>();
            for (Path file : matchingFiles) {
                List<T> fileData = loadDataFromFile(file);
                results.addAll(fileData);
            }

            metrics.recordRecordsProcessed(results.size());
            LOGGER.debug("File system query completed for '{}': query='{}', matchedFiles={}, results={}",
                sourceName(), actualQuery, matchingFiles.size(), results.size());
            return results;

        } catch (IOException e) {
            LOGGER.error("File system query failed for '{}': query='{}', parameters={}, error={}",
                sourceName(), query, parameters, e.getMessage());
            LOGGER.debug("File system query failure for '{}':", sourceName(), e);
            throw DataSourceException.executionError("File system query failed", e, "query");
        }
    }
    
    @Override
    public <T> T queryForObject(String query, Map<String, Object> parameters) throws DataSourceException {
        List<T> results = query(query, parameters);
        return results.isEmpty() ? null : results.get(0);
    }
    
    @Override
    public <T> List<List<T>> batchQuery(List<String> queries) throws DataSourceException {
        List<List<T>> results = new ArrayList<>();
        
        for (String query : queries) {
            List<T> queryResult = query(query, Collections.emptyMap());
            results.add(queryResult);
        }
        
        return results;
    }
    
    @Override
    public void batchUpdate(List<String> updates) throws DataSourceException {
        // File system updates involve writing data to files
        long startTime = System.currentTimeMillis();

        try {
            Path basePath = Paths.get(configuration.getConnection().getBasePath());

            for (String update : updates) {
                processFileUpdate(basePath, update);
            }

            // Clear cache after updates to ensure fresh data on next read
            cacheManager.clear();

            metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
            LOGGER.info("Completed batch update of {} operations for file system data source '{}'",
                updates.size(), getName());

        } catch (Exception e) {
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);
            LOGGER.error("File system batch update failed for '{}': updates={}, error={}",
                sourceName(), updates.size(), e.getMessage());
            LOGGER.debug("File system batch update failure for '{}':", sourceName(), e);
            throw DataSourceException.executionError("File system batch update failed", e, "batchUpdate");
        }
    }
    
    @Override
    public DataSourceConfiguration getConfiguration() {
        return configuration;
    }
    
    @Override
    public void refresh() throws DataSourceException {
        LOGGER.info("Refreshing file system data source '{}': basePath='{}', filePattern='{}'",
            sourceName(), configuredBasePath(), configuredFilePattern());
        // Clear cache
        cacheManager.clear();

        // Reload initial data
        loadInitialData();

        LOGGER.info("File system data source '{}' refreshed", getName());
    }
    
    @Override
    public void shutdown() {
        LOGGER.info("Shutting down file system data source '{}': monitoring={}, trackedFiles={}",
            sourceName(), monitoring, fileModificationTimes.size());
        // Stop file monitoring
        stopFileMonitoring();
        
        // Clear cache
        cacheManager.clear();
        
        connectionStatus = ConnectionStatus.shutdown();
        LOGGER.info("File system data source '{}' shut down", sourceName());
    }
    
    /**
     * Initialize data loaders for different file formats.
     */
    private void initializeDataLoaders() {
        dataLoaders.put("csv", new CsvDataLoader());
        dataLoaders.put("json", new JsonDataLoader());
        dataLoaders.put("xml", new XmlDataLoader());
        dataLoaders.put("txt", new TextDataLoader());
    }
    
    /**
     * Load initial data from files.
     */
    private void loadInitialData() {
        try {
            Path basePath = Paths.get(configuration.getConnection().getBasePath());
            String filePattern = configuration.getConnection().getFilePattern();
            
            if (filePattern != null) {
                List<Path> matchingFiles = findMatchingFiles(basePath, filePattern);
                
                for (Path file : matchingFiles) {
                    loadAndCacheFile(file);
                }
                
                LOGGER.info("Loaded {} files for data source '{}'", matchingFiles.size(), getName());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load initial data for file system data source '{}': basePath='{}', filePattern='{}', error={}",
                sourceName(), configuredBasePath(), configuredFilePattern(), e.getMessage());
            LOGGER.debug("Initial file load failure for '{}':", sourceName(), e);
        }
    }
    
    /**
     * Find files matching the given pattern.
     */
    private List<Path> findMatchingFiles(Path basePath, String pattern) throws IOException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(basePath)) {
            return StreamSupport.stream(stream.spliterator(), false)
                .filter(path -> matcher.matches(path.getFileName()))
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Load data from a specific file.
     */
    private Object loadDataFromFile(String dataType, Object... parameters) throws IOException {
        Path basePath = Paths.get(configuration.getConnection().getBasePath());
        String filePattern = configuration.getConnection().getFilePattern();
        
        // Find the most recent file matching the pattern
        List<Path> matchingFiles = findMatchingFiles(basePath, filePattern);
        if (matchingFiles.isEmpty()) {
            return null;
        }
        
        // Sort by last modified time and get the most recent
        Path mostRecentFile = matchingFiles.stream()
            .max(Comparator.comparing(path -> {
                try {
                    return Files.getLastModifiedTime(path);
                } catch (IOException e) {
                    return FileTime.fromMillis(0);
                }
            }))
            .orElse(null);
        
        if (mostRecentFile != null) {
            List<Object> fileData = loadDataFromFile(mostRecentFile);

            // Find specific data based on parameters
            return findDataInList(fileData, parameters);
        }
        
        return null;
    }
    
    /**
     * Load data from a specific file path.
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> loadDataFromFile(Path filePath) throws IOException {
        String fileExtension = getFileExtension(filePath);
        DataLoader loader = dataLoaders.get(fileExtension.toLowerCase());
        
        if (loader == null) {
            throw new IOException("No data loader available for file type: " + fileExtension);
        }
        
        return (List<T>) loader.loadData(filePath, configuration.getFileFormat());
    }
    
    /**
     * Load and cache a file.
     */
    private void loadAndCacheFile(Path filePath) {
        try {
            List<Object> data = loadDataFromFile(filePath);
            
            if (cacheManager.isEnabled()) {
                String cacheKey = filePath.toString();
                cacheManager.put(cacheKey, data);
                // Track file modification time for monitoring
                try {
                    FileTime lastModified = Files.getLastModifiedTime(filePath);
                    fileModificationTimes.put(cacheKey, lastModified.toInstant());
                } catch (IOException e) {
                    LOGGER.debug("Failed to read modification time for '{}': file='{}', error={}",
                        sourceName(), filePath, e.getMessage());
                }
            }
            
            LOGGER.debug("Loaded and cached file for '{}': file='{}', records={}", sourceName(), filePath, data.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to load file for '{}': file='{}', error={}", sourceName(), filePath, e.getMessage());
            LOGGER.debug("File load failure for '{}':", sourceName(), e);
        }
    }
    
    /**
     * Start file monitoring.
     */
    private void startFileMonitoring() {
        if (monitoring) {
            LOGGER.debug("File monitoring already active for '{}'", sourceName());
            return;
        }
        
        int pollingInterval = configuration.getConnection().getPollingInterval();
        
        fileMonitorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "FileSystemMonitor-" + configuration.getName());
            thread.setDaemon(true);
            return thread;
        });
        
        fileMonitorExecutor.scheduleAtFixedRate(
            this::checkForFileChanges,
            0, // Initial delay
            pollingInterval,
            TimeUnit.SECONDS
        );
        
        monitoring = true;
        LOGGER.info("Started file monitoring for '{}' with interval {}s", 
            sourceName(), pollingInterval);
    }
    
    /**
     * Stop file monitoring.
     */
    private void stopFileMonitoring() {
        if (!monitoring) {
            LOGGER.debug("File monitoring already stopped for '{}'", sourceName());
            return;
        }
        
        if (fileMonitorExecutor != null) {
            fileMonitorExecutor.shutdown();
            try {
                if (!fileMonitorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.info("File monitor executor for '{}' did not stop gracefully; forcing shutdown", sourceName());
                    fileMonitorExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                fileMonitorExecutor.shutdownNow();
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted while stopping file monitoring for '{}': {}", sourceName(), e.getMessage());
                LOGGER.debug("File monitoring stop interruption for '{}':", sourceName(), e);
            }
        }
        
        monitoring = false;
        LOGGER.info("Stopped file monitoring for '{}': trackedFiles={}", sourceName(), fileModificationTimes.size());
    }
    
    /**
     * Resolve named query from configuration.
     */
    private String resolveNamedQuery(String query) {
        if (configuration.getQueries() != null && configuration.getQueries().containsKey(query)) {
            String resolvedQuery = configuration.getQueries().get(query);
            LOGGER.debug("Resolved named file query for '{}': query='{}', resolved='{}'", sourceName(), query, resolvedQuery);
            return resolvedQuery;
        }

        LOGGER.debug("No named file query found for '{}': query='{}', using literal value", sourceName(), query);
        return query; // Return as-is if not found in named queries
    }

    /**
     * Execute JSONPath query against loaded data.
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> executeJsonPathQuery(String jsonPathQuery, Map<String, Object> parameters) throws DataSourceException {
        try {
            // Load data from files first
            Path basePath = Paths.get(configuration.getConnection().getBasePath());
            String filePattern = configuration.getConnection().getFilePattern();

            if (filePattern == null) {
                throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                    "File pattern is required for JSONPath queries");
            }

            List<Path> matchingFiles = findMatchingFiles(basePath, filePattern);
            if (matchingFiles.isEmpty()) {
                return new ArrayList<>();
            }

            // For now, use the most recent file
            Path mostRecentFile = matchingFiles.stream()
                .max(Comparator.comparing(path -> {
                    try {
                        return Files.getLastModifiedTime(path);
                    } catch (IOException e) {
                        return FileTime.fromMillis(0);
                    }
                }))
                .orElse(null);

            if (mostRecentFile == null) {
                return new ArrayList<>();
            }

            // Load and parse the file
            List<Object> fileData = loadDataFromFile(mostRecentFile);

            // Apply JSONPath query (simplified implementation)
            List<T> results = new ArrayList<>();
            String processedQuery = processQueryParameters(jsonPathQuery, parameters);



            // Simple JSONPath implementation for basic queries
            if (processedQuery.contains("[?(@.")) {
                // Extract filter condition
                results.addAll((List<T>) filterDataWithJsonPath(fileData, processedQuery, parameters));
            } else if (processedQuery.equals("$[*]") || processedQuery.equals("$.*") || processedQuery.equals("$.users[*]")) {
                // Return all data
                results.addAll((List<T>) fileData);
            }

            LOGGER.debug("JSONPath query completed for '{}': query='{}', file='{}', results={}",
                sourceName(), processedQuery, mostRecentFile, results.size());

            return results;

        } catch (IOException e) {
            LOGGER.error("JSONPath query failed for '{}': query='{}', parameters={}, error={}",
                sourceName(), jsonPathQuery, parameters, e.getMessage());
            LOGGER.debug("JSONPath query failure for '{}':", sourceName(), e);
            throw DataSourceException.executionError("JSONPath query execution failed", e, "query");
        }
    }

    /**
     * Process query parameters in JSONPath expression.
     */
    private String processQueryParameters(String query, Map<String, Object> parameters) {
        String processedQuery = query;
        for (Map.Entry<String, Object> param : parameters.entrySet()) {
            String placeholder = "{" + param.getKey() + "}";
            if (processedQuery.contains(placeholder)) {
                processedQuery = processedQuery.replace(placeholder, String.valueOf(param.getValue()));
            }
        }
        return processedQuery;
    }

    /**
     * Filter data using JSONPath-like expression.
     */
    private List<Object> filterDataWithJsonPath(List<Object> data, String jsonPath, Map<String, Object> parameters) {
        List<Object> results = new ArrayList<>();

        // Simple implementation for queries like "$[?(@.id == '1')]" or "$.users[?(@.id == '1')]"
        if (jsonPath.contains("[?(@.") && jsonPath.contains("==")) {
            String condition = jsonPath.substring(jsonPath.indexOf("[?(@.") + 5, jsonPath.lastIndexOf(")]"));
            String[] parts = condition.split("==");
            if (parts.length == 2) {
                String fieldName = parts[0].trim();
                String expectedValue = parts[1].trim().replace("'", "").replace("\"", "");

                for (Object item : data) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) item;
                        Object fieldValue = map.get(fieldName);
                        if (fieldValue != null && fieldValue.toString().equals(expectedValue)) {
                            results.add(item);
                        }
                    }
                }
            }
        }
        return results;
    }

    /**
     * Execute SQL-like query against CSV data.
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> executeCsvQuery(String sqlQuery, Map<String, Object> parameters) throws DataSourceException {
        try {
            // Load data from files first
            Path basePath = Paths.get(configuration.getConnection().getBasePath());
            String filePattern = configuration.getConnection().getFilePattern();

            if (filePattern == null) {
                throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                    "File pattern is required for CSV queries");
            }

            List<Path> matchingFiles = findMatchingFiles(basePath, filePattern);
            if (matchingFiles.isEmpty()) {
                return new ArrayList<>();
            }

            // For now, use the most recent file
            Path mostRecentFile = matchingFiles.stream()
                .max(Comparator.comparing(path -> {
                    try {
                        return Files.getLastModifiedTime(path);
                    } catch (IOException e) {
                        return FileTime.fromMillis(0);
                    }
                }))
                .orElse(null);

            if (mostRecentFile == null) {
                return new ArrayList<>();
            }

            // Load and parse the file
            LOGGER.debug("Loading file for CSV query on '{}': file='{}'", sourceName(), mostRecentFile);
            List<Object> fileData = loadDataFromFile(mostRecentFile);
            LOGGER.debug("Loaded {} records from file for CSV query on '{}'", fileData.size(), sourceName());

            // Apply SQL-like filtering (simplified implementation)
            List<T> results = new ArrayList<>();

            // Parse simple WHERE clause
            if (sqlQuery.toUpperCase().contains("WHERE")) {
                results.addAll((List<T>) filterCsvDataWithSql(fileData, sqlQuery, parameters));
            } else {
                // SELECT * - return all data
                results.addAll((List<T>) fileData);
            }

            // Record metrics
            metrics.recordSuccessfulRequest(0); // We don't track time here
            metrics.recordRecordsProcessed(results.size());

            LOGGER.debug("CSV query completed for '{}': query='{}', file='{}', results={}",
                sourceName(), sqlQuery, mostRecentFile, results.size());

            return results;

        } catch (IOException e) {
            LOGGER.error("CSV query failed for '{}': query='{}', parameters={}, error={}",
                sourceName(), sqlQuery, parameters, e.getMessage());
            LOGGER.debug("CSV query failure for '{}':", sourceName(), e);
            throw DataSourceException.executionError("CSV query execution failed", e, "query");
        }
    }

    /**
     * Filter CSV data using SQL-like WHERE clause.
     */
    private List<Object> filterCsvDataWithSql(List<Object> data, String sqlQuery, Map<String, Object> parameters) {
        List<Object> results = new ArrayList<>();

        // Simple implementation for queries like "SELECT * WHERE name = :name"
        String upperQuery = sqlQuery.toUpperCase();
        if (upperQuery.contains("WHERE") && upperQuery.contains("=")) {
            String whereClause = sqlQuery.substring(sqlQuery.toUpperCase().indexOf("WHERE") + 5).trim();

            // Parse simple condition like "name = :name"
            String[] parts = whereClause.split("=");
            if (parts.length == 2) {
                String fieldName = parts[0].trim();
                String parameterName = parts[1].trim();

                // Remove parameter prefix (:)
                if (parameterName.startsWith(":")) {
                    parameterName = parameterName.substring(1);
                }

                Object expectedValue = parameters.get(parameterName);
                if (expectedValue != null) {
                    for (Object item : data) {
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> map = (Map<String, Object>) item;
                            Object fieldValue = map.get(fieldName);
                            if (fieldValue != null && fieldValue.toString().equals(expectedValue.toString())) {
                                results.add(item);
                            }
                        }
                    }
                }
            }
        }

        return results;
    }

    /**
     * Check for file changes and reload if necessary.
     */
    private void checkForFileChanges() {
        try {
            Path basePath = Paths.get(configuration.getConnection().getBasePath());
            String filePattern = configuration.getConnection().getFilePattern();
            
            List<Path> currentFiles = findMatchingFiles(basePath, filePattern);
            
            for (Path file : currentFiles) {
                String cacheKey = file.toString();
                Instant cachedModTime = fileModificationTimes.get(cacheKey);

                try {
                    FileTime lastModified = Files.getLastModifiedTime(file);

                    if (cachedModTime == null || cachedModTime.isBefore(lastModified.toInstant())) {
                        loadAndCacheFile(file);
                        LOGGER.debug("Reloaded modified file for '{}': file='{}'", sourceName(), file);
                    }
                } catch (IOException e) {
                    LOGGER.debug("Failed to inspect file modification time for '{}': file='{}', error={}",
                        sourceName(), file, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error during file change check for '{}': basePath='{}', filePattern='{}', error={}",
                sourceName(), configuredBasePath(), configuredFilePattern(), e.getMessage());
            LOGGER.debug("File change check failure for '{}':", sourceName(), e);
        }
    }
    
    // Helper methods
    
    private String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }
    

    
    private Object findDataInList(List<Object> data, Object... parameters) {
        if (parameters.length == 0) {
            return data;
        }
        
        // Simple implementation - could be enhanced with more sophisticated filtering
        String keyColumn = configuration.getFileFormat() != null ? 
            configuration.getFileFormat().getKeyColumn() : null;
        
        if (keyColumn != null && parameters.length > 0) {
            Object searchValue = parameters[0];
            
            for (Object item : data) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) item;
                    if (Objects.equals(map.get(keyColumn), searchValue)) {
                        return item;
                    }
                }
            }
        }
        
        return data.isEmpty() ? null : data.get(0);
    }

    /**
     * Process a single file update operation.
     *
     * @param basePath The base path for file operations
     * @param update The update operation string
     * @throws IOException if file operation fails
     */
    private void processFileUpdate(Path basePath, String update) throws IOException {
        // Parse update string format: "operation:filename:data"
        // Examples:
        // - "write:output.csv:data"
        // - "append:log.txt:data"
        // - "delete:temp.json"

        String[] parts = update.split(":", 3);
        if (parts.length < 2) {
            throw new IOException("Invalid update format. Expected 'operation:filename[:data]'");
        }

        String operation = parts[0].trim().toLowerCase();
        String filename = parts[1].trim();
        String data = parts.length > 2 ? parts[2] : "";

        Path targetFile = basePath.resolve(filename);

        switch (operation) {
            case "write":
                writeDataToFile(targetFile, data);
                break;

            case "append":
                appendDataToFile(targetFile, data);
                break;

            case "delete":
                deleteFile(targetFile);
                break;

            case "create":
                createFile(targetFile, data);
                break;

            default:
                throw new IOException("Unsupported file operation: " + operation);
        }

        LOGGER.debug("Processed file update for '{}': operation='{}', file='{}'", sourceName(), operation, filename);
    }

    /**
     * Write data to a file, overwriting existing content.
     */
    private void writeDataToFile(Path filePath, String data) throws IOException {
        // Ensure parent directory exists
        Files.createDirectories(filePath.getParent());

        // Determine encoding
        String encoding = configuration.getFileFormat() != null &&
                         configuration.getFileFormat().getEncoding() != null ?
                         configuration.getFileFormat().getEncoding() : "UTF-8";

        Files.writeString(filePath, data, Charset.forName(encoding));
    }

    /**
     * Append data to a file.
     */
    private void appendDataToFile(Path filePath, String data) throws IOException {
        // Ensure parent directory exists
        Files.createDirectories(filePath.getParent());

        // Determine encoding
        String encoding = configuration.getFileFormat() != null &&
                         configuration.getFileFormat().getEncoding() != null ?
                         configuration.getFileFormat().getEncoding() : "UTF-8";

        Files.writeString(filePath, data, Charset.forName(encoding), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Delete a file.
     */
    private void deleteFile(Path filePath) throws IOException {
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }

    /**
     * Create a new file with data.
     */
    private void createFile(Path filePath, String data) throws IOException {
        // Ensure parent directory exists
        Files.createDirectories(filePath.getParent());

        // Only create if file doesn't exist
        if (!Files.exists(filePath)) {
            writeDataToFile(filePath, data);
        } else {
            throw new IOException("File already exists: " + filePath);
        }
    }

    private String sourceName() {
        return configuration != null && configuration.getName() != null ? configuration.getName() : "file-system-source";
    }

    private String configuredBasePath() {
        return configuration != null && configuration.getConnection() != null ? configuration.getConnection().getBasePath() : null;
    }

    private String configuredFilePattern() {
        return configuration != null && configuration.getConnection() != null ? configuration.getConnection().getFilePattern() : null;
    }
}
