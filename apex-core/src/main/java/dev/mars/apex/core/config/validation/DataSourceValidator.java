package dev.mars.apex.core.config.validation;

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

import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.core.config.model.YamlDataSource;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Validates data source configurations including type-specific requirements
 * for database (PostgreSQL, MySQL, Oracle, SQL Server), REST API, and file system sources.
 *
 * <p>Extracted from {@code ConfigurationLoader} as part of the validation layer refactoring.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 */
public class DataSourceValidator {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceValidator.class);

    /**
     * Validate all data sources in the configuration.
     *
     * @param config the YAML rule configuration to validate
     * @throws ConfigurationException if any data source is invalid
     */
    public void validate(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getDataSources() != null) {
            for (YamlDataSource dataSource : config.getDataSources()) {
                validateDataSource(dataSource);
            }
        }
    }

    /**
     * Validate a data source configuration.
     */
    private void validateDataSource(YamlDataSource dataSource) throws ConfigurationException {
        if (dataSource.getName() == null || dataSource.getName().trim().isEmpty()) {
            throw new ConfigurationException("Data source name is required");
        }
        if (dataSource.getType() == null || dataSource.getType().trim().isEmpty()) {
            throw new ConfigurationException("Data source type is required for data source: " + dataSource.getName());
        }

        // Validate type-specific requirements
        validateDataSourceTypeSpecificRequirements(dataSource);
    }

    /**
     * Validate type-specific requirements for data sources.
     */
    private void validateDataSourceTypeSpecificRequirements(YamlDataSource dataSource) throws ConfigurationException {
        String type = dataSource.getType().toLowerCase();
        String name = dataSource.getName();

        switch (type) {
            case "postgresql":
            case "mysql":
            case "oracle":
            case "sqlserver":
                validateDatabaseDataSource(dataSource, name);
                break;
            case "rest-api":
                validateRestApiDataSource(dataSource, name);
                break;
            case "file-system":
                validateFileSystemDataSource(dataSource, name);
                break;
            // Other types are optional validation
        }
    }

    /**
     * Validate database data source requirements.
     */
    private void validateDatabaseDataSource(YamlDataSource dataSource, String name) throws ConfigurationException {
        Map<String, Object> connection = dataSource.getConnection();
        if (connection == null || connection.isEmpty()) {
            throw new ConfigurationException("Missing required connection configuration for database data source: " + name);
        }

        if (!connection.containsKey("host") || connection.get("host") == null) {
            throw new ConfigurationException("Missing required connection property 'host' for database data source: " + name);
        }
        if (!connection.containsKey("port") || connection.get("port") == null) {
            throw new ConfigurationException("Missing required connection property 'port' for database data source: " + name);
        }
        if (!connection.containsKey("database") || connection.get("database") == null) {
            throw new ConfigurationException("Missing required connection property 'database' for database data source: " + name);
        }
    }

    /**
     * Validate REST API data source requirements.
     */
    private void validateRestApiDataSource(YamlDataSource dataSource, String name) throws ConfigurationException {
        Map<String, Object> connection = dataSource.getConnection();
        if (connection == null || connection.isEmpty()) {
            throw new ConfigurationException("Missing required connection configuration for REST API data source: " + name);
        }

        if (!connection.containsKey("base-url") || connection.get("base-url") == null) {
            throw new ConfigurationException("Missing required property 'base-url' for REST API data source: " + name);
        }
    }

    /**
     * Validate file system data source requirements.
     */
    private void validateFileSystemDataSource(YamlDataSource dataSource, String name) throws ConfigurationException {
        Map<String, Object> connection = dataSource.getConnection();
        if (connection == null || connection.isEmpty()) {
            throw new ConfigurationException("Missing required connection configuration for file system data source: " + name);
        }

        if (!connection.containsKey("base-path") || connection.get("base-path") == null) {
            throw new ConfigurationException("Missing required property 'base-path' for file system data source: " + name);
        }
    }
}
