package dev.mars.apex.core.service.data.external.database;

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


import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests to verify SQL error classification with real database errors.
 * Tests that the SqlErrorClassifier correctly identifies different types of SQL errors.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseErrorHandlingIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseErrorHandlingIntegrationTest.class);

    private static final String JDBC_URL = "jdbc:h2:mem:error_handling_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "";

    private Connection testConnection;

    @BeforeEach
    void setUp() throws SQLException {
        // Create test database connection
        testConnection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);

        // Create test table
        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS test_customers");
            stmt.execute("""
                CREATE TABLE test_customers (
                    customer_id VARCHAR(20) PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) UNIQUE,
                    status VARCHAR(20) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Insert initial test data
            stmt.execute("""
                INSERT INTO test_customers (customer_id, customer_name, email, status)
                VALUES ('CUST001', 'John Doe', 'john@example.com', 'ACTIVE')
            """);
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testConnection != null && !testConnection.isClosed()) {
            testConnection.close();
        }
    }

    // ========================================
    // REAL SQL ERROR CLASSIFICATION TESTS
    // ========================================

    @Test
    @Order(1)
    @DisplayName("Should classify real table not found error correctly")
    void testRealTableNotFoundError() throws SQLException {
        LOGGER.info("TEST: Testing real table not found error classification");

        try (Statement stmt = testConnection.createStatement()) {
            stmt.executeQuery("SELECT * FROM nonexistent_table");
            fail("Expected SQLException for table not found");
        } catch (SQLException e) {
            LOGGER.info("Caught SQLException: {}", e.getMessage());

            SqlErrorClassifier.SqlErrorType errorType = SqlErrorClassifier.classifyError(e);
            assertEquals(SqlErrorClassifier.SqlErrorType.CONFIGURATION_ERROR, errorType);
            assertTrue(SqlErrorClassifier.shouldFailPipeline(errorType));

            LOGGER.info("✅ PASS: Real table not found error correctly classified as CONFIGURATION_ERROR");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should classify real column not found error correctly")
    void testRealColumnNotFoundError() throws SQLException {
        LOGGER.info("TEST: Testing real column not found error classification");

        try (Statement stmt = testConnection.createStatement()) {
            stmt.executeQuery("SELECT nonexistent_column FROM test_customers");
            fail("Expected SQLException for column not found");
        } catch (SQLException e) {
            LOGGER.info("Caught SQLException: {}", e.getMessage());

            SqlErrorClassifier.SqlErrorType errorType = SqlErrorClassifier.classifyError(e);
            assertEquals(SqlErrorClassifier.SqlErrorType.CONFIGURATION_ERROR, errorType);
            assertTrue(SqlErrorClassifier.shouldFailPipeline(errorType));

            LOGGER.info("✅ PASS: Real column not found error correctly classified as CONFIGURATION_ERROR");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should classify real SQL syntax error correctly")
    void testRealSyntaxError() throws SQLException {
        LOGGER.info("TEST: Testing real SQL syntax error classification");

        try (Statement stmt = testConnection.createStatement()) {
            stmt.executeQuery("INVALID SQL SYNTAX SELECT WRONG");
            fail("Expected SQLException for syntax error");
        } catch (SQLException e) {
            LOGGER.info("Caught SQLException: {}", e.getMessage());

            SqlErrorClassifier.SqlErrorType errorType = SqlErrorClassifier.classifyError(e);
            assertEquals(SqlErrorClassifier.SqlErrorType.CONFIGURATION_ERROR, errorType);
            assertTrue(SqlErrorClassifier.shouldFailPipeline(errorType));

            LOGGER.info("✅ PASS: Real syntax error correctly classified as CONFIGURATION_ERROR");
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should classify real primary key violation correctly")
    void testRealPrimaryKeyViolation() throws SQLException {
        LOGGER.info("TEST: Testing real primary key violation classification");

        try (PreparedStatement stmt = testConnection.prepareStatement(
                "INSERT INTO test_customers (customer_id, customer_name, email, status) VALUES (?, ?, ?, ?)")) {

            // Try to insert duplicate primary key
            stmt.setString(1, "CUST001"); // This already exists
            stmt.setString(2, "Duplicate Customer");
            stmt.setString(3, "duplicate@example.com");
            stmt.setString(4, "ACTIVE");
            stmt.executeUpdate();

            fail("Expected SQLException for primary key violation");
        } catch (SQLException e) {
            LOGGER.info("Caught SQLException: {}", e.getMessage());

            SqlErrorClassifier.SqlErrorType errorType = SqlErrorClassifier.classifyError(e);
            assertEquals(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION, errorType);
            assertFalse(SqlErrorClassifier.shouldFailPipeline(errorType));

            LOGGER.info("✅ PASS: Real primary key violation correctly classified as DATA_INTEGRITY_VIOLATION");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should classify real unique constraint violation correctly")
    void testRealUniqueConstraintViolation() throws SQLException {
        LOGGER.info("TEST: Testing real unique constraint violation classification");

        try (PreparedStatement stmt = testConnection.prepareStatement(
                "INSERT INTO test_customers (customer_id, customer_name, email, status) VALUES (?, ?, ?, ?)")) {

            // Try to insert duplicate email (unique constraint)
            stmt.setString(1, "CUST002");
            stmt.setString(2, "Another Customer");
            stmt.setString(3, "john@example.com"); // This email already exists
            stmt.setString(4, "ACTIVE");
            stmt.executeUpdate();

            fail("Expected SQLException for unique constraint violation");
        } catch (SQLException e) {
            LOGGER.info("Caught SQLException: {}", e.getMessage());

            SqlErrorClassifier.SqlErrorType errorType = SqlErrorClassifier.classifyError(e);
            assertEquals(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION, errorType);
            assertFalse(SqlErrorClassifier.shouldFailPipeline(errorType));

            LOGGER.info("✅ PASS: Real unique constraint violation correctly classified as DATA_INTEGRITY_VIOLATION");
        }
    }

    @Test
    @Order(6)
    @DisplayName("Should classify real NOT NULL violation correctly")
    void testRealNotNullViolation() throws SQLException {
        LOGGER.info("TEST: Testing real NOT NULL violation classification");

        try (PreparedStatement stmt = testConnection.prepareStatement(
                "INSERT INTO test_customers (customer_id, customer_name, email, status) VALUES (?, ?, ?, ?)")) {

            // Try to insert NULL for NOT NULL column
            stmt.setString(1, "CUST003");
            stmt.setString(2, null); // customer_name is NOT NULL
            stmt.setString(3, "test3@example.com");
            stmt.setString(4, "ACTIVE");
            stmt.executeUpdate();

            fail("Expected SQLException for NOT NULL violation");
        } catch (SQLException e) {
            LOGGER.info("Caught SQLException: {}", e.getMessage());

            SqlErrorClassifier.SqlErrorType errorType = SqlErrorClassifier.classifyError(e);
            assertEquals(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION, errorType);
            assertFalse(SqlErrorClassifier.shouldFailPipeline(errorType));

            LOGGER.info("✅ PASS: Real NOT NULL violation correctly classified as DATA_INTEGRITY_VIOLATION");
        }
    }

    @Test
    @Order(7)
    @DisplayName("Comprehensive error classification verification")
    void testComprehensiveErrorClassification() {
        LOGGER.info("TEST: Comprehensive verification of error classification system");

        // Verify that different error types have appropriate fail-fast behavior

        // Configuration errors SHOULD fail pipeline
        assertTrue(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.SqlErrorType.CONFIGURATION_ERROR));

        // Data integrity violations should NOT fail pipeline
        assertFalse(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION));

        // Transient errors should NOT fail pipeline (should be retried)
        assertFalse(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.SqlErrorType.TRANSIENT_ERROR));

        // Fatal errors SHOULD fail pipeline
        assertTrue(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.SqlErrorType.FATAL_ERROR));

        LOGGER.info("✅ PASS: Comprehensive error classification working correctly");
    }
}
