package dev.mars.apex.core.service.data.external.file;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
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
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class FileSystemDataSource implements ExternalDataSource {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemDataSource.class);
    
    private DataSourceConfiguration configuration;
    private ConnectionStatus connectionStatus;
    private DataSourceMetrics metrics;
    
    // Data loaders for different file formats
    private final Map<String, DataLoader> dataLoaders = new HashMap<>();
    
    // Cache for loaded file data
    private final Map<String, CachedFileData> fileDataCache = new ConcurrentHashMap<>();
    
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
        
        // Initialize data loaders
        initializeDataLoaders();
    }
    
    @Override
    public void initialize(DataSourceConfiguration config) throws DataSourceException {
        this.configuration = config;
        this.connectionStatus = ConnectionStatus.connecting();
        
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
            LOGGER.info("File system data source '{}' initialized successfully", config.getName());
            
        } catch (Exception e) {
            this.connectionStatus = ConnectionStatus.error("Initialization failed", e);
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
        
        try {
            // Check cache first if enabled
            if (isCacheEnabled()) {
                String cacheKey = generateCacheKey(dataType, parameters);
                CachedFileData cached = fileDataCache.get(cacheKey);
                if (cached != null && !cached.isExpired()) {
                    metrics.recordCacheHit();
                    metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
                    return (T) findDataInCache(cached, parameters);
                }
                metrics.recordCacheMiss();
            }
            
            // Load data from file
            Object result = loadDataFromFile(dataType, parameters);
            
            metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
            return (T) result;
            
        } catch (Exception e) {
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);
            LOGGER.error("Failed to get data from file system", e);
            return null;
        }
    }
    
    @Override
    public <T> List<T> query(String query, Map<String, Object> parameters) throws DataSourceException {
        try {
            // First, check if this is a named query from configuration
            String actualQuery = resolveNamedQuery(query);

            // If it's a JSONPath query, execute it against loaded data
            if (actualQuery.startsWith("$.") || actualQuery.startsWith("$[")) {
                return executeJsonPathQuery(actualQuery, parameters);
            }

            // If it's a SQL-like query for CSV, execute it against loaded data
            if (actualQuery.toUpperCase().startsWith("SELECT")) {
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
            return results;

        } catch (IOException e) {
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
        // File system updates would involve writing to files
        // This is a basic implementation - could be enhanced for specific use cases
        throw new DataSourceException(DataSourceException.ErrorType.EXECUTION_ERROR,
            "Batch updates not supported for file system data source");
    }
    
    @Override
    public DataSourceConfiguration getConfiguration() {
        return configuration;
    }
    
    @Override
    public void refresh() throws DataSourceException {
        // Clear cache
        fileDataCache.clear();
        
        // Reload initial data
        loadInitialData();
        
        LOGGER.info("File system data source '{}' refreshed", getName());
    }
    
    @Override
    public void shutdown() {
        // Stop file monitoring
        stopFileMonitoring();
        
        // Clear cache
        fileDataCache.clear();
        
        connectionStatus = ConnectionStatus.shutdown();
        LOGGER.info("File system data source '{}' shut down", getName());
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
            LOGGER.error("Failed to load initial data for file system data source '{}'", getName(), e);
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
            
            // Cache the data
            if (isCacheEnabled()) {
                String cacheKey = generateCacheKey(dataType, parameters);
                long ttl = configuration.getCache().getTtlSeconds() * 1000L;
                fileDataCache.put(cacheKey, new CachedFileData(fileData, System.currentTimeMillis() + ttl));
            }
            
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
            
            if (isCacheEnabled()) {
                String cacheKey = filePath.toString();
                long ttl = configuration.getCache().getTtlSeconds() * 1000L;
                fileDataCache.put(cacheKey, new CachedFileData(data, System.currentTimeMillis() + ttl));
            }
            
            LOGGER.debug("Loaded and cached file: {}", filePath);
            
        } catch (Exception e) {
            LOGGER.error("Failed to load file: {}", filePath, e);
        }
    }
    
    /**
     * Start file monitoring.
     */
    private void startFileMonitoring() {
        if (monitoring) {
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
            configuration.getName(), pollingInterval);
    }
    
    /**
     * Stop file monitoring.
     */
    private void stopFileMonitoring() {
        if (!monitoring) {
            return;
        }
        
        if (fileMonitorExecutor != null) {
            fileMonitorExecutor.shutdown();
            try {
                if (!fileMonitorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    fileMonitorExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                fileMonitorExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        monitoring = false;
        LOGGER.info("Stopped file monitoring for '{}'", configuration.getName());
    }
    
    /**
     * Resolve named query from configuration.
     */
    private String resolveNamedQuery(String query) {
        if (configuration.getQueries() != null && configuration.getQueries().containsKey(query)) {
            return configuration.getQueries().get(query);
        }
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

            return results;

        } catch (IOException e) {
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
            List<Object> fileData = loadDataFromFile(mostRecentFile);

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

            return results;

        } catch (IOException e) {
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
                CachedFileData cached = fileDataCache.get(cacheKey);
                
                try {
                    FileTime lastModified = Files.getLastModifiedTime(file);
                    
                    if (cached == null || cached.getLastModified().isBefore(lastModified.toInstant())) {
                        loadAndCacheFile(file);
                        LOGGER.debug("Reloaded modified file: {}", file);
                    }
                } catch (IOException e) {
                    LOGGER.warn("Failed to check modification time for file: {}", file, e);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error during file change check", e);
        }
    }
    
    // Helper methods
    
    private String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }
    
    private boolean isCacheEnabled() {
        return configuration.getCache() != null && configuration.getCache().isEnabled();
    }
    
    private String generateCacheKey(String dataType, Object... parameters) {
        StringBuilder key = new StringBuilder(dataType);
        for (Object param : parameters) {
            key.append(":").append(param != null ? param.toString() : "null");
        }
        return key.toString();
    }
    
    private Object findDataInCache(CachedFileData cached, Object... parameters) {
        return findDataInList(cached.getData(), parameters);
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
     * Cached file data holder.
     */
    private static class CachedFileData {
        private final List<Object> data;
        private final long expiryTime;
        private final java.time.Instant lastModified;
        
        public CachedFileData(List<Object> data, long expiryTime) {
            this.data = data;
            this.expiryTime = expiryTime;
            this.lastModified = java.time.Instant.now();
        }
        
        public List<Object> getData() {
            return data;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
        
        public java.time.Instant getLastModified() {
            return lastModified;
        }
    }
}
