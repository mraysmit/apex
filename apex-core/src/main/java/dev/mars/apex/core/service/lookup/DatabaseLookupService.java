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


import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.DataSourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Database-backed lookup service implementation.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public class DatabaseLookupService extends LookupService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseLookupService.class);
    
    private final ExternalDataSource dataSource;
    private final String query;
    private final List<String> parameterFields;
    private final Map<String, Object> defaultValues;
    
    /**
     * Create a database lookup service.
     * 
     * @param name The service name
     * @param dataSource The database data source
     * @param query The SQL query to execute
     * @param parameterFields List of parameter field names
     * @param defaultValues Default values to return when lookup fails
     */
    public DatabaseLookupService(String name, 
                                ExternalDataSource dataSource,
                                String query, 
                                List<String> parameterFields,
                                Map<String, Object> defaultValues) {
        super(name, Collections.emptyList()); // No static lookup values for database service
        this.dataSource = dataSource;
        this.query = query;
        this.parameterFields = parameterFields != null ? parameterFields : Collections.emptyList();
        this.defaultValues = defaultValues != null ? defaultValues : Collections.emptyMap();
        
        LOGGER.info("Created DatabaseLookupService '{}' with query: {}", name, query);
    }
    
    /**
     * Perform database lookup using the configured query.
     * 
     * @param key The lookup key (can be single value or Map of parameters)
     * @return Query result or default values if lookup fails
     */
    @Override
    public Object transform(Object key) {
        if (key == null) {
            LOGGER.debug("Lookup key is null, returning default values");
            return defaultValues.isEmpty() ? null : new HashMap<>(defaultValues);
        }
        
        try {
            // Build parameters map from lookup key
            Map<String, Object> parameters = buildParametersMap(key);

            LOGGER.info("Executing database lookup with parameters: {} for query: {}", parameters, query);
            
            // Execute database query
            Object result = dataSource.queryForObject(query, parameters);
            
            if (result != null) {
                LOGGER.debug("Database lookup successful for key '{}': {}", key, result);
                
                // Merge with default values (query result takes precedence)
                if (!defaultValues.isEmpty() && result instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    Map<String, Object> mergedResult = new HashMap<>(defaultValues);
                    mergedResult.putAll(resultMap);
                    return mergedResult;
                }
                
                return result;
            } else {
                LOGGER.debug("Database lookup returned null for key '{}', returning default values", key);
                return defaultValues.isEmpty() ? null : new HashMap<>(defaultValues);
            }
            
        } catch (DataSourceException e) {
            LOGGER.error("Database lookup failed for key '{}': {}", key, e.getMessage(), e);
            return defaultValues.isEmpty() ? null : new HashMap<>(defaultValues);
        } catch (Exception e) {
            LOGGER.error("Unexpected error during database lookup for key '{}': {}", key, e.getMessage(), e);
            return defaultValues.isEmpty() ? null : new HashMap<>(defaultValues);
        }
    }
    
    /**
     * Build parameters map from lookup key.
     * 
     * @param key The lookup key
     * @return Parameters map for SQL query
     */
    private Map<String, Object> buildParametersMap(Object key) {
        Map<String, Object> parameters = new HashMap<>();
        
        if (key instanceof Map) {
            // Key is already a map of parameters
            @SuppressWarnings("unchecked")
            Map<String, Object> keyMap = (Map<String, Object>) key;
            
            // Map specified parameter fields from the key map
            for (String paramField : parameterFields) {
                if (keyMap.containsKey(paramField)) {
                    parameters.put(paramField, keyMap.get(paramField));
                }
            }
            
            // If no parameter fields specified, use all key map entries
            if (parameterFields.isEmpty()) {
                parameters.putAll(keyMap);
            }
            
        } else {
            // Key is a single value
            if (!parameterFields.isEmpty()) {
                // Use first parameter field name
                parameters.put(parameterFields.get(0), key);
            } else {
                // Default parameter name for single value
                parameters.put("key", key);
            }
        }
        
        return parameters;
    }
    
    /**
     * Get the database data source.
     * 
     * @return The data source
     */
    public ExternalDataSource getDataSource() {
        return dataSource;
    }
    
    /**
     * Get the SQL query.
     * 
     * @return The query
     */
    public String getQuery() {
        return query;
    }
    
    /**
     * Get the parameter field names.
     * 
     * @return List of parameter fields
     */
    public List<String> getParameterFields() {
        return Collections.unmodifiableList(parameterFields);
    }
    
    /**
     * Get the default values.
     * 
     * @return Default values map
     */
    public Map<String, Object> getDefaultValues() {
        return Collections.unmodifiableMap(defaultValues);
    }
    
    @Override
    public String toString() {
        return "DatabaseLookupService{" +
                "name='" + getName() + '\'' +
                ", query='" + query + '\'' +
                ", parameterFields=" + parameterFields +
                ", defaultValues=" + defaultValues +
                '}';
    }
}
