/*
 * Copyright 2024 APEX Development Team
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

package dev.mars.apex.core.service.data.external.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for JDBC parameter processing and PreparedStatement creation.
 * 
 * This class provides shared functionality for processing named parameters in SQL
 * statements and creating PreparedStatements with proper parameter binding.
 * Used by both DatabaseDataSource and DatabaseDataSink to avoid code duplication.
 */
public final class JdbcParameterUtils {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcParameterUtils.class);
    
    // Private constructor to prevent instantiation
    private JdbcParameterUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Prepare a SQL statement with named parameters.
     * 
     * This method processes named parameters in the format :paramName and replaces them
     * with ? placeholders, then binds the parameter values to the PreparedStatement.
     * 
     * @param connection the database connection
     * @param sql the SQL statement with named parameters (e.g., "SELECT * FROM users WHERE id = :userId")
     * @param parameters map of parameter names to values
     * @return PreparedStatement with parameters bound
     * @throws SQLException if there's an error preparing the statement or binding parameters
     */
    public static PreparedStatement prepareStatement(Connection connection, String sql, 
                                                   Map<String, Object> parameters) throws SQLException {
        // Validate inputs
        if (connection == null) {
            throw new SQLException("Connection cannot be null");
        }
        if (sql == null) {
            throw new SQLException("SQL cannot be null");
        }
        if (parameters == null) {
            parameters = new HashMap<>();
        }

        // Process named parameters in order they appear in SQL
        String processedSql = sql;
        List<Object> paramValues = new ArrayList<>();

        LOGGER.debug("Preparing statement with SQL: {}", sql);
        LOGGER.debug("Parameters: {}", parameters);

        // Find and replace named parameters (:paramName) with ? placeholders
        int searchIndex = 0;
        while (searchIndex < processedSql.length()) {
            int colonIndex = processedSql.indexOf(':', searchIndex);
            if (colonIndex == -1) {
                break;
            }

            // Find the end of the parameter name
            int endIndex = colonIndex + 1;
            while (endIndex < processedSql.length() && 
                   (Character.isLetterOrDigit(processedSql.charAt(endIndex)) || 
                    processedSql.charAt(endIndex) == '_')) {
                endIndex++;
            }

            String paramName = processedSql.substring(colonIndex + 1, endIndex);
            LOGGER.debug("Found parameter: {} at position {}-{}", paramName, colonIndex, endIndex);

            if (parameters.containsKey(paramName)) {
                Object paramValue = parameters.get(paramName);
                LOGGER.debug("Replacing parameter {} with value: {}", paramName, paramValue);

                // Replace this occurrence with ?
                processedSql = processedSql.substring(0, colonIndex) + "?" +
                             processedSql.substring(endIndex);
                paramValues.add(paramValue);
                searchIndex = colonIndex + 1;
            } else {
                LOGGER.warn("Parameter {} not found in parameters map: {}", paramName, parameters.keySet());
                searchIndex = endIndex;
            }
        }

        LOGGER.debug("Processed SQL: {}", processedSql);
        LOGGER.debug("Parameter values: {}", paramValues);

        PreparedStatement statement = connection.prepareStatement(processedSql);

        // Set parameter values
        for (int i = 0; i < paramValues.size(); i++) {
            Object value = paramValues.get(i);
            LOGGER.debug("Setting parameter {} to value: {}", i + 1, value);
            statement.setObject(i + 1, value);
        }

        return statement;
    }
    
    /**
     * Build parameter map from array of parameters using parameter names.
     * 
     * @param paramNames array of parameter names (can be null)
     * @param parameters array of parameter values
     * @return map of parameter names to values
     */
    public static Map<String, Object> buildParameterMap(String[] paramNames, Object... parameters) {
        Map<String, Object> paramMap = new HashMap<>();
        
        if (paramNames != null) {
            for (int i = 0; i < parameters.length && i < paramNames.length; i++) {
                paramMap.put(paramNames[i], parameters[i]);
            }
        } else {
            // Use generic parameter names
            for (int i = 0; i < parameters.length; i++) {
                paramMap.put("param" + (i + 1), parameters[i]);
            }
        }
        
        return paramMap;
    }
}
