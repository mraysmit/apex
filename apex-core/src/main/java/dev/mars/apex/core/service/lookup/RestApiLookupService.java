package dev.mars.apex.core.service.lookup;

import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.DataSourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
 * REST API lookup service implementation.
 *
 * This service provides REST API-based lookup functionality for enrichments.
 * It uses the existing REST API data source infrastructure to execute API calls
 * and return enrichment data.
 *
 * Features:
 * - Parameter mapping from lookup keys to API parameters
 * - Endpoint resolution from data source configuration
 * - Default value handling for failed lookups
 * - Integration with existing REST API infrastructure
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class RestApiLookupService extends LookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiLookupService.class);

    private final ExternalDataSource dataSource;
    private final String endpoint;
    private final List<String> parameterFields;
    private final Map<String, Object> defaultValues;

    /**
     * Create a new REST API lookup service.
     *
     * @param name The service name
     * @param dataSource The REST API data source
     * @param endpoint The API endpoint name or path
     * @param parameterFields List of parameter field names
     * @param defaultValues Default values for failed lookups
     */
    public RestApiLookupService(String name,
                               ExternalDataSource dataSource,
                               String endpoint,
                               List<String> parameterFields,
                               Map<String, Object> defaultValues) {
        super(name, new ArrayList<>()); // Call parent constructor with empty lookup values
        this.dataSource = dataSource;
        this.endpoint = endpoint;
        this.parameterFields = parameterFields != null ? new ArrayList<>(parameterFields) : new ArrayList<>();
        this.defaultValues = defaultValues != null ? new HashMap<>(defaultValues) : new HashMap<>();

        LOGGER.info("Created RestApiLookupService '{}' with endpoint: {}", name, endpoint);
    }
    
    /**
     * Perform REST API lookup using the configured endpoint.
     * 
     * @param key The lookup key (can be single value or Map of parameters)
     * @return API response or default values if lookup fails
     */
    @Override
    public Object transform(Object key) {
        LOGGER.info("TRACE: RestApiLookupService.transform called with key: {}", key);
        if (key == null) {
            LOGGER.debug("Lookup key is null, returning default values");
            return defaultValues.isEmpty() ? null : new HashMap<>(defaultValues);
        }
        
        try {
            // Build parameters map from lookup key
            Map<String, Object> parameters = buildParametersMap(key);

            LOGGER.info("Executing REST API lookup with parameters: {} for endpoint: {}", parameters, endpoint);
            LOGGER.info("TRACE: RestApiLookupService calling dataSource.queryForObject - dataSource type: {}", dataSource.getClass().getName());

            // Execute REST API call
            System.out.println("DEBUG: RestApiLookupService about to call dataSource.queryForObject");
            Object result;
            try {
                result = dataSource.queryForObject(endpoint, parameters);
                System.out.println("DEBUG: RestApiLookupService dataSource.queryForObject completed successfully");
                System.out.println("DEBUG: RestApiLookupService result: " + result);
                System.out.println("DEBUG: RestApiLookupService result type: " + (result != null ? result.getClass().getName() : "null"));
            } catch (Exception e) {
                System.out.println("DEBUG: RestApiLookupService caught exception: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            System.out.println("DEBUG: RestApiLookupService received result: " + result);
            System.out.println("DEBUG: Result type: " + (result != null ? result.getClass().getName() : "null"));
            System.out.println("DEBUG: Is result instanceof Map? " + (result instanceof Map));

            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                System.out.println("DEBUG: Result map size: " + resultMap.size());
                System.out.println("DEBUG: Result map keys: " + resultMap.keySet());
                for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
                    System.out.println("DEBUG:   " + entry.getKey() + " = " + entry.getValue());
                }
            }

            if (result != null) {
                LOGGER.debug("REST API lookup successful for key '{}': {}", key, result);

                // Merge with default values (API result takes precedence)
                if (!defaultValues.isEmpty() && result instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    Map<String, Object> mergedResult = new HashMap<>(defaultValues);
                    mergedResult.putAll(resultMap);
                    System.out.println("DEBUG: RestApiLookupService returning merged result: " + mergedResult);
                    return mergedResult;
                }

                System.out.println("DEBUG: RestApiLookupService returning result: " + result);
                return result;
            } else {
                LOGGER.debug("REST API lookup returned null for key '{}', returning default values", key);
                return defaultValues.isEmpty() ? null : new HashMap<>(defaultValues);
            }
            
        } catch (Exception e) {
            System.out.println("DEBUG: Exception caught: " + e.getMessage());
            e.printStackTrace();
            LOGGER.error("Unexpected error during REST API lookup for key '{}': {}", key, e.getMessage(), e);
            return defaultValues.isEmpty() ? null : new HashMap<>(defaultValues);
        }
    }
    
    /**
     * Build parameters map from lookup key.
     * 
     * @param key The lookup key
     * @return Parameters map for REST API call
     */
    private Map<String, Object> buildParametersMap(Object key) {
        Map<String, Object> parameters = new HashMap<>();
        
        if (key instanceof Map) {
            // Key is already a map of parameters
            @SuppressWarnings("unchecked")
            Map<String, Object> keyMap = (Map<String, Object>) key;
            parameters.putAll(keyMap);
        } else if (!parameterFields.isEmpty()) {
            // Use first parameter field as the key
            String firstField = parameterFields.get(0);
            parameters.put(firstField, key);
        } else {
            // Default parameter name
            parameters.put("key", key);
        }
        
        LOGGER.debug("Built parameters map: {} from key: {}", parameters, key);
        return parameters;
    }
    

    
    /**
     * Get the REST API endpoint.
     * 
     * @return The endpoint name or path
     */
    public String getEndpoint() {
        return endpoint;
    }
    
    /**
     * Get the parameter fields.
     * 
     * @return List of parameter field names
     */
    public List<String> getParameterFields() {
        return new ArrayList<>(parameterFields);
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
     * Get the data source.
     * 
     * @return The REST API data source
     */
    public ExternalDataSource getDataSource() {
        return dataSource;
    }
}
