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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that verifies the JdbcParameterUtils works correctly
 * with a real database connection and actual SQL execution.
 * 
 * This test proves the refactored parameter processing works end-to-end.
 */
class ParameterProcessingIntegrationTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        // Create unique in-memory H2 database for each test
        String dbName = "testdb_" + System.nanoTime();
        connection = DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "");

        // Create test table
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE test_users (id BIGINT, name VARCHAR(100), email VARCHAR(100), active BOOLEAN)");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Should successfully insert data using JdbcParameterUtils")
    void testInsertWithParameterUtils() throws SQLException {
        // Arrange
        String sql = "INSERT INTO test_users (id, name, email, active) VALUES (:id, :name, :email, :active)";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 1L);
        parameters.put("name", "John Doe");
        parameters.put("email", "john@example.com");
        parameters.put("active", true);

        // Act
        try (PreparedStatement stmt = JdbcParameterUtils.prepareStatement(connection, sql, parameters)) {
            int rowsAffected = stmt.executeUpdate();
            
            // Assert
            assertEquals(1, rowsAffected);
        }

        // Verify data was inserted correctly
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM test_users WHERE id = 1")) {
            
            assertTrue(rs.next());
            assertEquals(1L, rs.getLong("id"));
            assertEquals("John Doe", rs.getString("name"));
            assertEquals("john@example.com", rs.getString("email"));
            assertTrue(rs.getBoolean("active"));
        }
    }

    @Test
    @DisplayName("Should successfully query data using JdbcParameterUtils")
    void testQueryWithParameterUtils() throws SQLException {
        // Arrange - Insert test data first
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO test_users VALUES (1, 'John Doe', 'john@example.com', true)");
            stmt.execute("INSERT INTO test_users VALUES (2, 'Jane Smith', 'jane@example.com', false)");
        }

        String sql = "SELECT * FROM test_users WHERE active = :active AND name LIKE :namePattern";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("active", true);
        parameters.put("namePattern", "John%");

        // Act
        try (PreparedStatement stmt = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
             ResultSet rs = stmt.executeQuery()) {
            
            // Assert
            assertTrue(rs.next());
            assertEquals(1L, rs.getLong("id"));
            assertEquals("John Doe", rs.getString("name"));
            assertEquals("john@example.com", rs.getString("email"));
            assertTrue(rs.getBoolean("active"));
            
            assertFalse(rs.next()); // Should only have one result
        }
    }

    @Test
    @DisplayName("Should handle repeated parameters correctly")
    void testRepeatedParameters() throws SQLException {
        // Arrange - Insert test data
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO test_users VALUES (1, 'John Doe', 'john@example.com', true)");
            stmt.execute("INSERT INTO test_users VALUES (2, 'Jane Smith', 'jane@example.com', false)");
        }

        String sql = "SELECT * FROM test_users WHERE id = :userId OR name = (SELECT name FROM test_users WHERE id = :userId)";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", 1L);

        // Act
        try (PreparedStatement stmt = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
             ResultSet rs = stmt.executeQuery()) {
            
            // Assert
            assertTrue(rs.next());
            assertEquals(1L, rs.getLong("id"));
            assertEquals("John Doe", rs.getString("name"));
        }
    }

    @Test
    @DisplayName("Should handle null parameter values correctly")
    void testNullParameters() throws SQLException {
        // Arrange
        String sql = "INSERT INTO test_users (id, name, email, active) VALUES (:id, :name, :email, :active)";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 3L);
        parameters.put("name", null);
        parameters.put("email", "test@example.com");
        parameters.put("active", false);

        // Act
        try (PreparedStatement stmt = JdbcParameterUtils.prepareStatement(connection, sql, parameters)) {
            int rowsAffected = stmt.executeUpdate();
            
            // Assert
            assertEquals(1, rowsAffected);
        }

        // Verify null was inserted correctly
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM test_users WHERE id = 3")) {
            
            assertTrue(rs.next());
            assertEquals(3L, rs.getLong("id"));
            assertNull(rs.getString("name"));
            assertEquals("test@example.com", rs.getString("email"));
            assertFalse(rs.getBoolean("active"));
        }
    }

    @Test
    @DisplayName("Should handle complex SQL with multiple operations")
    void testComplexSql() throws SQLException {
        // Arrange - Insert test data
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO test_users VALUES (1, 'John Doe', 'john@example.com', true)");
            stmt.execute("INSERT INTO test_users VALUES (2, 'Jane Smith', 'jane@example.com', false)");
            stmt.execute("INSERT INTO test_users VALUES (3, 'Bob Johnson', 'bob@example.com', true)");
        }

        String sql = "UPDATE test_users SET active = :newStatus WHERE id IN (:id1, :id2) AND name LIKE :namePattern";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("newStatus", false);
        parameters.put("id1", 1L);
        parameters.put("id2", 3L);
        parameters.put("namePattern", "%o%"); // Matches "John Doe" and "Bob Johnson"

        // Act
        try (PreparedStatement stmt = JdbcParameterUtils.prepareStatement(connection, sql, parameters)) {
            int rowsAffected = stmt.executeUpdate();
            
            // Assert
            assertEquals(2, rowsAffected);
        }

        // Verify updates were applied correctly
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_users WHERE active = false")) {
            
            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1)); // All 3 users should now be inactive
        }
    }

    @Test
    @DisplayName("Should handle edge case with parameters in string literals")
    void testParametersInStringLiterals() throws SQLException {
        // Arrange
        String sql = "SELECT * FROM test_users WHERE name = :name AND email LIKE 'test:pattern%'";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Test User");

        // Act & Assert - Should not throw exception
        try (PreparedStatement stmt = JdbcParameterUtils.prepareStatement(connection, sql, parameters)) {
            assertNotNull(stmt);
            // The colon in 'test:pattern%' should not be treated as a parameter
        }
    }

    @Test
    @DisplayName("Should handle batch operations correctly")
    void testBatchOperations() throws SQLException {
        // Arrange
        String sql = "INSERT INTO test_users (id, name, email, active) VALUES (:id, :name, :email, :active)";
        
        // Insert multiple records using the same prepared statement
        Map<String, Object> params1 = new HashMap<>();
        params1.put("id", 10L);
        params1.put("name", "User 10");
        params1.put("email", "user10@example.com");
        params1.put("active", true);

        Map<String, Object> params2 = new HashMap<>();
        params2.put("id", 11L);
        params2.put("name", "User 11");
        params2.put("email", "user11@example.com");
        params2.put("active", false);

        // Act
        try (PreparedStatement stmt1 = JdbcParameterUtils.prepareStatement(connection, sql, params1);
             PreparedStatement stmt2 = JdbcParameterUtils.prepareStatement(connection, sql, params2)) {
            
            int rows1 = stmt1.executeUpdate();
            int rows2 = stmt2.executeUpdate();
            
            // Assert
            assertEquals(1, rows1);
            assertEquals(1, rows2);
        }

        // Verify both records were inserted
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_users WHERE id IN (10, 11)")) {
            
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }
}
