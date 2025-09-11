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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration test that compares the old inline parameter processing implementation
 * with the new shared JdbcParameterUtils to ensure they produce identical results.
 * 
 * This test proves that the refactoring maintains 100% functional equivalence.
 */
class ParameterProcessingComparisonTest {

    @Mock
    private Connection mockConnection;
    
    @Mock
    private PreparedStatement mockStatement1;
    
    @Mock
    private PreparedStatement mockStatement2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
        // Arrange
        when(mockConnection.prepareStatement(anyString()))
            .thenReturn(mockStatement1)
            .thenReturn(mockStatement2);
        
        String sql = "SELECT * FROM users WHERE id = :userId AND name = :userName";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", 123L);
        parameters.put("userName", "John Doe");

        // Act
        PreparedStatement result1 = prepareStatementOriginal(mockConnection, sql, parameters);
        PreparedStatement result2 = JdbcParameterUtils.prepareStatement(mockConnection, sql, parameters);

        // Assert - Both should call prepareStatement with identical processed SQL
        verify(mockConnection, times(2)).prepareStatement("SELECT * FROM users WHERE id = ? AND name = ?");
        
        // Both should set identical parameters
        verify(mockStatement1).setObject(1, 123L);
        verify(mockStatement1).setObject(2, "John Doe");
        verify(mockStatement2).setObject(1, 123L);
        verify(mockStatement2).setObject(2, "John Doe");
    }

    @Test
    @DisplayName("Should produce identical results for complex INSERT statement")
    void testComplexInsertComparison() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString()))
            .thenReturn(mockStatement1)
            .thenReturn(mockStatement2);
        
        String sql = "INSERT INTO customers (customer_id, customer_name, email, status, created_at) " +
                    "VALUES (:id, :customerName, :email, :status, CURRENT_TIMESTAMP)";
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 1L);
        parameters.put("customerName", "John Smith");
        parameters.put("email", "john.smith@email.com");
        parameters.put("status", "ACTIVE");
        parameters.put("registrationDate", LocalDate.of(2024, 1, 15)); // Extra unused parameter

        // Act
        PreparedStatement result1 = prepareStatementOriginal(mockConnection, sql, parameters);
        PreparedStatement result2 = JdbcParameterUtils.prepareStatement(mockConnection, sql, parameters);

        // Assert - Both should call prepareStatement with identical processed SQL
        String expectedSql = "INSERT INTO customers (customer_id, customer_name, email, status, created_at) " +
                           "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        verify(mockConnection, times(2)).prepareStatement(expectedSql);
        
        // Both should set identical parameters in identical order
        verify(mockStatement1).setObject(1, 1L);
        verify(mockStatement1).setObject(2, "John Smith");
        verify(mockStatement1).setObject(3, "john.smith@email.com");
        verify(mockStatement1).setObject(4, "ACTIVE");
        
        verify(mockStatement2).setObject(1, 1L);
        verify(mockStatement2).setObject(2, "John Smith");
        verify(mockStatement2).setObject(3, "john.smith@email.com");
        verify(mockStatement2).setObject(4, "ACTIVE");
    }

    @Test
    @DisplayName("Should produce identical results for repeated parameters")
    void testRepeatedParametersComparison() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString()))
            .thenReturn(mockStatement1)
            .thenReturn(mockStatement2);
        
        String sql = "SELECT * FROM logs WHERE user_id = :userId OR created_by = :userId";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", 123L);

        // Act
        PreparedStatement result1 = prepareStatementOriginal(mockConnection, sql, parameters);
        PreparedStatement result2 = JdbcParameterUtils.prepareStatement(mockConnection, sql, parameters);

        // Assert
        verify(mockConnection, times(2)).prepareStatement("SELECT * FROM logs WHERE user_id = ? OR created_by = ?");
        
        // Both should set the same parameter value twice
        verify(mockStatement1).setObject(1, 123L);
        verify(mockStatement1).setObject(2, 123L);
        verify(mockStatement2).setObject(1, 123L);
        verify(mockStatement2).setObject(2, 123L);
    }

    @Test
    @DisplayName("Should produce identical results for null parameters")
    void testNullParametersComparison() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString()))
            .thenReturn(mockStatement1)
            .thenReturn(mockStatement2);
        
        String sql = "UPDATE users SET name = :name, email = :email WHERE id = :id";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", null);
        parameters.put("email", "test@example.com");
        parameters.put("id", 1L);

        // Act
        PreparedStatement result1 = prepareStatementOriginal(mockConnection, sql, parameters);
        PreparedStatement result2 = JdbcParameterUtils.prepareStatement(mockConnection, sql, parameters);

        // Assert
        verify(mockConnection, times(2)).prepareStatement("UPDATE users SET name = ?, email = ? WHERE id = ?");
        
        // Both should handle null values identically
        verify(mockStatement1).setObject(1, null);
        verify(mockStatement1).setObject(2, "test@example.com");
        verify(mockStatement1).setObject(3, 1L);
        
        verify(mockStatement2).setObject(1, null);
        verify(mockStatement2).setObject(2, "test@example.com");
        verify(mockStatement2).setObject(3, 1L);
    }

    @Test
    @DisplayName("Should produce identical results for SQL with no parameters")
    void testNoParametersComparison() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString()))
            .thenReturn(mockStatement1)
            .thenReturn(mockStatement2);
        
        String sql = "SELECT COUNT(*) FROM users";
        Map<String, Object> parameters = new HashMap<>();

        // Act
        PreparedStatement result1 = prepareStatementOriginal(mockConnection, sql, parameters);
        PreparedStatement result2 = JdbcParameterUtils.prepareStatement(mockConnection, sql, parameters);

        // Assert
        verify(mockConnection, times(2)).prepareStatement("SELECT COUNT(*) FROM users");
        
        // Neither should set any parameters
        verifyNoMoreInteractions(mockStatement1);
        verifyNoMoreInteractions(mockStatement2);
    }

    @Test
    @DisplayName("Should handle edge cases identically")
    void testEdgeCasesComparison() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString()))
            .thenReturn(mockStatement1)
            .thenReturn(mockStatement2);
        
        String sql = "SELECT * FROM table WHERE field_1 = :param_1 AND field2 = :param2";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param_1", "value1");
        parameters.put("param2", "value2");

        // Act
        PreparedStatement result1 = prepareStatementOriginal(mockConnection, sql, parameters);
        PreparedStatement result2 = JdbcParameterUtils.prepareStatement(mockConnection, sql, parameters);

        // Assert
        verify(mockConnection, times(2)).prepareStatement("SELECT * FROM table WHERE field_1 = ? AND field2 = ?");
        
        verify(mockStatement1).setObject(1, "value1");
        verify(mockStatement1).setObject(2, "value2");
        verify(mockStatement2).setObject(1, "value1");
        verify(mockStatement2).setObject(2, "value2");
    }

    @Test
    @DisplayName("Should handle null SQL identically")
    void testNullSqlComparison() {
        // Arrange
        Map<String, Object> parameters = new HashMap<>();

        // Act & Assert - Both should throw identical exceptions
        SQLException exception1 = assertThrows(SQLException.class, () -> {
            prepareStatementOriginal(mockConnection, null, parameters);
        });
        
        SQLException exception2 = assertThrows(SQLException.class, () -> {
            JdbcParameterUtils.prepareStatement(mockConnection, null, parameters);
        });
        
        assertEquals("SQL cannot be null", exception1.getMessage());
        assertEquals("SQL cannot be null", exception2.getMessage());
    }
}
