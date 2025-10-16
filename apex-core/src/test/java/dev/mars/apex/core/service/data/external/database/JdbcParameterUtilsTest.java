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


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;



/**
 * Rigorous unit tests for JdbcParameterUtils.
 * 
 * This test class ensures the shared parameter processing utility works correctly
 * for all edge cases and scenarios used throughout the APEX system.
 */
class JdbcParameterUtilsTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        String url = "jdbc:h2:mem:jdbc_utils_" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        connection = java.sql.DriverManager.getConnection(url, "sa", "");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Should handle simple named parameters correctly")
    void testSimpleNamedParameters() throws SQLException {
        try (java.sql.Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS users");
            st.execute("CREATE TABLE users (id BIGINT, name VARCHAR(100))");
            st.execute("INSERT INTO users (id, name) VALUES (123, 'John Doe')");
        }

        String sql = "SELECT * FROM users WHERE id = :userId AND name = :userName";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", 123L);
        parameters.put("userName", "John Doe");

        PreparedStatement ps = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
        try (java.sql.ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(123L, rs.getLong("id"));
            assertEquals("John Doe", rs.getString("name"));
        }
    }

    @Test
    @DisplayName("Should handle complex SQL with multiple parameter types")
    void testComplexSqlWithMultipleTypes() throws SQLException {
        try (java.sql.Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS customers");
            st.execute("CREATE TABLE customers (id BIGINT, name VARCHAR(100), email VARCHAR(150), created_date DATE, balance DOUBLE, active BOOLEAN)");
        }

        String sql = "INSERT INTO customers (id, name, email, created_date, balance, active) " +
                     "VALUES (:id, :customerName, :email, :createdDate, :balance, :active)";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 1L);
        parameters.put("customerName", "John Smith");
        parameters.put("email", "john.smith@email.com");
        parameters.put("createdDate", java.sql.Date.valueOf(LocalDate.of(2024, 1, 15)));
        parameters.put("balance", 1000.50);
        parameters.put("active", true);

        PreparedStatement ps = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
        int updated = ps.executeUpdate();
        assertEquals(1, updated);

        try (java.sql.Statement st2 = connection.createStatement();
             java.sql.ResultSet rs = st2.executeQuery("SELECT * FROM customers WHERE id = 1")) {
            assertTrue(rs.next());
            assertEquals("John Smith", rs.getString("name"));
            assertEquals("john.smith@email.com", rs.getString("email"));
            assertEquals(1000.50, rs.getDouble("balance"), 0.0001);
            assertTrue(rs.getBoolean("active"));
        }
    }

    @Test
    @DisplayName("Should handle parameters with underscores and numbers")
    void testParametersWithUnderscoresAndNumbers() throws SQLException {
        try (java.sql.Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS table1");
            st.execute("CREATE TABLE table1 (field_1 VARCHAR(50), field2 VARCHAR(50))");
            st.execute("INSERT INTO table1 (field_1, field2) VALUES ('value1', 'value2')");
            st.execute("INSERT INTO table1 (field_1, field2) VALUES ('other', 'else')");
        }

        String sql = "SELECT * FROM table1 WHERE field_1 = :param_1 AND field2 = :param2";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param_1", "value1");
        parameters.put("param2", "value2");

        PreparedStatement ps = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
        try (java.sql.ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals("value1", rs.getString("field_1"));
            assertEquals("value2", rs.getString("field2"));
        }
    }

    @Test
    @DisplayName("Should handle repeated parameters correctly")
    void testRepeatedParameters() throws SQLException {
        try (java.sql.Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS logs");
            st.execute("CREATE TABLE logs (user_id BIGINT, created_by BIGINT)");
            st.execute("INSERT INTO logs (user_id, created_by) VALUES (123, 456)");
            st.execute("INSERT INTO logs (user_id, created_by) VALUES (999, 123)");
        }

        String sql = "SELECT COUNT(*) c FROM logs WHERE user_id = :userId OR created_by = :userId";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", 123L);

        PreparedStatement ps = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
        try (java.sql.ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(2L, rs.getLong("c"));
        }
    }

    @Test
    @DisplayName("Should handle null parameter values")
    void testNullParameterValues() throws SQLException {
        try (java.sql.Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS users");
            st.execute("CREATE TABLE users (id BIGINT, name VARCHAR(100), email VARCHAR(100))");
            st.execute("INSERT INTO users (id, name, email) VALUES (1, 'Old', 'old@example.com')");
        }

        String sql = "UPDATE users SET name = :name, email = :email WHERE id = :id";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", null);
        parameters.put("email", "test@example.com");
        parameters.put("id", 1L);

        PreparedStatement ps = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
        int updated = ps.executeUpdate();
        assertEquals(1, updated);

        try (java.sql.Statement st2 = connection.createStatement();
             java.sql.ResultSet rs = st2.executeQuery("SELECT name, email FROM users WHERE id = 1")) {
            assertTrue(rs.next());
            assertNull(rs.getString("name"));
            assertEquals("test@example.com", rs.getString("email"));
        }
    }

    @Test
    @DisplayName("Should handle empty parameter map")
    void testEmptyParameterMap() throws SQLException {
        String sql = "SELECT 1";
        Map<String, Object> parameters = new HashMap<>();
        PreparedStatement ps = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
        try (java.sql.ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("Should handle null parameter map")
    void testNullParameterMap() throws SQLException {
        String sql = "SELECT 1";
        PreparedStatement ps = JdbcParameterUtils.prepareStatement(connection, sql, null);
        try (java.sql.ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("Should handle SQL with no parameters")
    void testSqlWithNoParameters() throws SQLException {
        try (java.sql.Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS users");
            st.execute("CREATE TABLE users (id INT)");
            st.execute("INSERT INTO users (id) VALUES (1)");
            st.execute("INSERT INTO users (id) VALUES (2)");
        }
        String sql = "SELECT COUNT(*) c FROM users";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("unused", "value");
        PreparedStatement ps = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
        try (java.sql.ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(2L, rs.getLong("c"));
        }
    }

    @Test
    @DisplayName("Should throw SQLException for null connection")
    void testNullConnection() {
        // Arrange
        String sql = "SELECT * FROM users WHERE id = :id";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 1L);

        // Act & Assert
        SQLException exception = assertThrows(SQLException.class, () -> {
            JdbcParameterUtils.prepareStatement(null, sql, parameters);
        });
        assertEquals("Connection cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw SQLException for null SQL")
    void testNullSql() {
        // Act & Assert
        SQLException exception = assertThrows(SQLException.class, () -> {
            JdbcParameterUtils.prepareStatement(connection, null, new HashMap<>());
        });
        assertEquals("SQL cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle buildParameterMap with parameter names")
    void testBuildParameterMapWithNames() {
        // Arrange
        String[] paramNames = {"id", "name", "email"};
        Object[] parameters = {1L, "John Doe", "john@example.com"};

        // Act
        Map<String, Object> result = JdbcParameterUtils.buildParameterMap(paramNames, parameters);

        // Assert
        assertEquals(3, result.size());
        assertEquals(1L, result.get("id"));
        assertEquals("John Doe", result.get("name"));
        assertEquals("john@example.com", result.get("email"));
    }

    @Test
    @DisplayName("Should handle buildParameterMap without parameter names")
    void testBuildParameterMapWithoutNames() {
        // Arrange
        Object[] parameters = {1L, "John Doe", "john@example.com"};

        // Act
        Map<String, Object> result = JdbcParameterUtils.buildParameterMap(null, parameters);

        // Assert
        assertEquals(3, result.size());
        assertEquals(1L, result.get("param1"));
        assertEquals("John Doe", result.get("param2"));
        assertEquals("john@example.com", result.get("param3"));
    }

    @Test
    @DisplayName("Should handle edge case with colon in string literals")
    void testColonInStringLiterals() throws SQLException {
        try (java.sql.Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS users");
            st.execute("CREATE TABLE users (name VARCHAR(50), description VARCHAR(50))");
            st.execute("INSERT INTO users (name, description) VALUES ('John', 'Time: 10:30 AM')");
        }
        String sql = "SELECT * FROM users WHERE name = :name AND description = 'Time: 10:30 AM'";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "John");
        PreparedStatement ps = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
        try (java.sql.ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals("John", rs.getString("name"));
            assertEquals("Time: 10:30 AM", rs.getString("description"));
        }
    }
}
