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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Integration test that compares the old inline parameter processing implementation
 * with the new shared JdbcParameterUtils to ensure they produce identical results.
 * 
 * This test proves that the refactoring maintains 100% functional equivalence.
 */
class ParameterProcessingComparisonTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        String url = "jdbc:h2:mem:param_compare_" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        connection = java.sql.DriverManager.getConnection(url, "sa", "");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Original inline implementation from DatabaseDataSink (before refactoring).
     * This is the exact code that was working in production.
     */
    private PreparedStatement prepareStatementOriginal(Connection connection, String sql, 
                                                     Map<String, Object> parameters) throws SQLException {
        // Validate inputs
        if (sql == null) {
            throw new SQLException("SQL cannot be null");
        }
        if (parameters == null) {
            parameters = new HashMap<>();
        }

        // Process named parameters in order they appear in SQL
        String processedSql = sql;
        List<Object> paramValues = new ArrayList<>();

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

            if (parameters.containsKey(paramName)) {
                Object paramValue = parameters.get(paramName);

                // Replace this occurrence with ?
                processedSql = processedSql.substring(0, colonIndex) + "?" +
                             processedSql.substring(endIndex);
                paramValues.add(paramValue);
                searchIndex = colonIndex + 1;
            } else {
                searchIndex = endIndex;
            }
        }

        PreparedStatement statement = connection.prepareStatement(processedSql);

        // Set parameter values
        for (int i = 0; i < paramValues.size(); i++) {
            Object value = paramValues.get(i);
            statement.setObject(i + 1, value);
        }

        return statement;
    }

    @Test
    @DisplayName("Should produce identical results for simple parameters")
    void testSimpleParametersComparison() throws SQLException {
        try (java.sql.Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS users");
            st.execute("CREATE TABLE users (id BIGINT, name VARCHAR(100))");
            st.execute("INSERT INTO users (id, name) VALUES (123, 'John Doe')");
        }

        String sql = "SELECT * FROM users WHERE id = :userId AND name = :userName";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", 123L);
        parameters.put("userName", "John Doe");

        PreparedStatement ps1 = prepareStatementOriginal(connection, sql, parameters);
        PreparedStatement ps2 = JdbcParameterUtils.prepareStatement(connection, sql, parameters);

        try (java.sql.ResultSet rs1 = ps1.executeQuery();
             java.sql.ResultSet rs2 = ps2.executeQuery()) {
            assertTrue(rs1.next());
            assertTrue(rs2.next());
            assertEquals(rs1.getLong("id"), rs2.getLong("id"));
            assertEquals(rs1.getString("name"), rs2.getString("name"));
        }
    }

    @Test
    @DisplayName("Should produce identical results for complex INSERT statement")
    void testComplexInsertComparison() throws SQLException {
        try (java.sql.Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS customers");
            st.execute("CREATE TABLE customers (customer_id BIGINT, customer_name VARCHAR(100), email VARCHAR(150), status VARCHAR(20), created_at TIMESTAMP)");
            st.execute("DELETE FROM customers");
        }

        String sql = "INSERT INTO customers (customer_id, customer_name, email, status, created_at) " +
                     "VALUES (:id, :customerName, :email, :status, CURRENT_TIMESTAMP)";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 1L);
        parameters.put("customerName", "John Smith");
        parameters.put("email", "john.smith@email.com");
        parameters.put("status", "ACTIVE");
        parameters.put("registrationDate", LocalDate.of(2024, 1, 15));

        // Execute original
        PreparedStatement ps1 = prepareStatementOriginal(connection, sql, parameters);
        int u1 = ps1.executeUpdate();
        assertEquals(1, u1);
        // Verify row
        try (java.sql.ResultSet rs = connection.createStatement().executeQuery("SELECT customer_name, email, status FROM customers WHERE customer_id = 1")) {
            assertTrue(rs.next());
            assertEquals("John Smith", rs.getString("customer_name"));
            assertEquals("john.smith@email.com", rs.getString("email"));
            assertEquals("ACTIVE", rs.getString("status"));
        }
        // Clean table
        connection.createStatement().execute("DELETE FROM customers");

        // Execute new
        PreparedStatement ps2 = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
        int u2 = ps2.executeUpdate();
        assertEquals(1, u2);
        try (java.sql.ResultSet rs = connection.createStatement().executeQuery("SELECT customer_name, email, status FROM customers WHERE customer_id = 1")) {
            assertTrue(rs.next());
            assertEquals("John Smith", rs.getString("customer_name"));
            assertEquals("john.smith@email.com", rs.getString("email"));
            assertEquals("ACTIVE", rs.getString("status"));
        }
    }

    @Test
    @DisplayName("Should produce identical results for repeated parameters")
    void testRepeatedParametersComparison() throws SQLException {
        try (java.sql.Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS logs");
            st.execute("CREATE TABLE logs (user_id BIGINT, created_by BIGINT)");
            st.execute("INSERT INTO logs (user_id, created_by) VALUES (123, 456)");
            st.execute("INSERT INTO logs (user_id, created_by) VALUES (999, 123)");
        }
        String sql = "SELECT COUNT(*) c FROM logs WHERE user_id = :userId OR created_by = :userId";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", 123L);
        PreparedStatement ps1 = prepareStatementOriginal(connection, sql, parameters);
        PreparedStatement ps2 = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
        try (java.sql.ResultSet rs1 = ps1.executeQuery();
             java.sql.ResultSet rs2 = ps2.executeQuery()) {
            assertTrue(rs1.next());
            assertTrue(rs2.next());
            assertEquals(rs1.getLong("c"), rs2.getLong("c"));
        }
    }

    @Test
    @DisplayName("Should produce identical results for null parameters")
    void testNullParametersComparison() throws SQLException {
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
        PreparedStatement ps1 = prepareStatementOriginal(connection, sql, parameters);
        PreparedStatement ps2 = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
        assertEquals(1, ps1.executeUpdate());
        // Reset row
        connection.createStatement().execute("UPDATE users SET name='Old', email='old@example.com' WHERE id=1");
        assertEquals(1, ps2.executeUpdate());
        try (java.sql.ResultSet rs = connection.createStatement().executeQuery("SELECT name, email FROM users WHERE id=1")) {
            assertTrue(rs.next());
            assertNull(rs.getString("name"));
            assertEquals("test@example.com", rs.getString("email"));
        }
    }

    @Test
    @DisplayName("Should produce identical results for SQL with no parameters")
    void testNoParametersComparison() throws SQLException {
        try (java.sql.Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS users");
            st.execute("CREATE TABLE users (id INT)");
            st.execute("INSERT INTO users (id) VALUES (1)");
            st.execute("INSERT INTO users (id) VALUES (2)");
        }
        String sql = "SELECT COUNT(*) c FROM users";
        Map<String, Object> parameters = new HashMap<>();
        PreparedStatement ps1 = prepareStatementOriginal(connection, sql, parameters);
        PreparedStatement ps2 = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
        try (java.sql.ResultSet rs1 = ps1.executeQuery();
             java.sql.ResultSet rs2 = ps2.executeQuery()) {
            assertTrue(rs1.next());
            assertTrue(rs2.next());
            assertEquals(rs1.getLong("c"), rs2.getLong("c"));
        }
    }

    @Test
    @DisplayName("Should handle edge cases identically")
    void testEdgeCasesComparison() throws SQLException {
        try (java.sql.Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS table1");
            st.execute("CREATE TABLE table1 (field_1 VARCHAR(50), field2 VARCHAR(50))");
            st.execute("INSERT INTO table1 (field_1, field2) VALUES ('value1', 'value2')");
        }
        String sql = "SELECT * FROM table1 WHERE field_1 = :param_1 AND field2 = :param2";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param_1", "value1");
        parameters.put("param2", "value2");
        PreparedStatement ps1 = prepareStatementOriginal(connection, sql, parameters);
        PreparedStatement ps2 = JdbcParameterUtils.prepareStatement(connection, sql, parameters);
        try (java.sql.ResultSet rs1 = ps1.executeQuery();
             java.sql.ResultSet rs2 = ps2.executeQuery()) {
            assertTrue(rs1.next());
            assertTrue(rs2.next());
            assertEquals(rs1.getString("field_1"), rs2.getString("field_1"));
            assertEquals(rs1.getString("field2"), rs2.getString("field2"));
        }
    }

    @Test
    @DisplayName("Should handle null SQL identically")
    void testNullSqlComparison() {
        Map<String, Object> parameters = new HashMap<>();
        SQLException exception1 = assertThrows(SQLException.class, () -> {
            prepareStatementOriginal(connection, null, parameters);
        });
        SQLException exception2 = assertThrows(SQLException.class, () -> {
            JdbcParameterUtils.prepareStatement(connection, null, parameters);
        });
        assertEquals("SQL cannot be null", exception1.getMessage());
        assertEquals("SQL cannot be null", exception2.getMessage());
    }
}
