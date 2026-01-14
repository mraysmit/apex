/*
 * Copyright 2026 Mark Andrew Ray-Smith Cityline Ltd
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
 *
 * Created: 2026-01-14
 */

package dev.mars.apex.sync;

/**
 * Central constants for Docker image versions used in Testcontainers integration tests
 * for the apex-data-sync module.
 * 
 * <p>This class provides a single source of truth for all Docker image versions used
 * across the apex-data-sync integration tests. All Testcontainers should reference
 * these constants instead of hardcoding image versions.</p>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>{@code
 * // PostgreSQL Testcontainer
 * @Container
 * static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(TestContainerImages.POSTGRES)
 *         .withDatabaseName("test_db")
 *         .withUsername("test_user")
 *         .withPassword("test_pass");
 * 
 * // SQL Server Testcontainer
 * @Container
 * static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(TestContainerImages.MSSQL_SERVER)
 *         .acceptLicense();
 * }</pre>
 * 
 * <p><strong>Version Management:</strong></p>
 * <ul>
 *   <li>PostgreSQL 15 Alpine - Lightweight and fast for testing schema operations</li>
 *   <li>SQL Server 2022 Latest - Required for testing SQL Server to PostgreSQL migrations</li>
 *   <li>Update versions here to ensure consistency across all tests</li>
 * </ul>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public final class TestContainerImages {

    /**
     * PostgreSQL Docker image.
     * Version: 15-alpine (lightweight, optimized for testing)
     * Use Case: Schema reading, data synchronization, migration validation
     */
    public static final String POSTGRES = "postgres:15-alpine3.20";

    /**
     * Microsoft SQL Server Docker image.
     * Version: 2022-latest
     * Use Case: SQL Server to PostgreSQL migration testing, schema compatibility validation
     */
    public static final String MSSQL_SERVER = "mcr.microsoft.com/mssql/server:2022-latest";

    /**
     * MySQL Docker image.
     * Version: 8.0
     * Use Case: Multi-database schema reading and synchronization testing
     */
    public static final String MYSQL = "mysql:8.0";

    /**
     * Oracle Database Docker image (requires acceptance of Oracle license).
     * Version: 21-slim
     * Use Case: Oracle to PostgreSQL migration testing
     * Note: May require authentication to Oracle Container Registry
     */
    public static final String ORACLE_XE = "gvenzl/oracle-xe:21-slim";

    // Private constructor to prevent instantiation
    private TestContainerImages() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
