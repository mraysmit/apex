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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Rigorous unit tests for JdbcParameterUtils.
 * 
 * This test class ensures the shared parameter processing utility works correctly
 * for all edge cases and scenarios used throughout the APEX system.
 */
class JdbcParameterUtilsTest {

    @Mock
    private Connection mockConnection;
    
    @Mock
    private PreparedStatement mockStatement;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should handle simple named parameters correctly")
    void testSimpleNamedParameters() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        
        String sql = "SELECT * FROM users WHERE id = :userId AND name = :userName";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", 123L);
        parameters.put("userName", "John Doe");

        // Act
        PreparedStatement result = JdbcParameterUtils.prepareStatement(mockConnection, sql, parameters);

        // Assert
        assertNotNull(result);
        verify(mockConnection).prepareStatement("SELECT * FROM users WHERE id = ? AND name = ?");
        verify(mockStatement).setObject(1, 123L);
        verify(mockStatement).setObject(2, "John Doe");
    }

    @Test
    @DisplayName("Should handle complex SQL with multiple parameter types")
    void testComplexSqlWithMultipleTypes() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        
        String sql = "INSERT INTO customers (id, name, email, created_date, balance, active) " +
                    "VALUES (:id, :customerName, :email, :createdDate, :balance, :active)";
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 1L);
        parameters.put("customerName", "John Smith");
        parameters.put("email", "john.smith@email.com");
        parameters.put("createdDate", LocalDate.of(2024, 1, 15));
        parameters.put("balance", 1000.50);
        parameters.put("active", true);

        // Act
        PreparedStatement result = JdbcParameterUtils.prepareStatement(mockConnection, sql, parameters);

        // Assert
        assertNotNull(result);
        verify(mockConnection).prepareStatement(
            "INSERT INTO customers (id, name, email, created_date, balance, active) " +
            "VALUES (?, ?, ?, ?, ?, ?)"
        );
        verify(mockStatement).setObject(1, 1L);
        verify(mockStatement).setObject(2, "John Smith");
        verify(mockStatement).setObject(3, "john.smith@email.com");
        verify(mockStatement).setObject(4, LocalDate.of(2024, 1, 15));
        verify(mockStatement).setObject(5, 1000.50);
        verify(mockStatement).setObject(6, true);
    }

    @Test
    @DisplayName("Should handle parameters with underscores and numbers")
    void testParametersWithUnderscoresAndNumbers() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        
        String sql = "SELECT * FROM table WHERE field_1 = :param_1 AND field2 = :param2";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param_1", "value1");
        parameters.put("param2", "value2");

        // Act
        PreparedStatement result = JdbcParameterUtils.prepareStatement(mockConnection, sql, parameters);

        // Assert
        assertNotNull(result);
        verify(mockConnection).prepareStatement("SELECT * FROM table WHERE field_1 = ? AND field2 = ?");
        verify(mockStatement).setObject(1, "value1");
        verify(mockStatement).setObject(2, "value2");
    }

    @Test
    @DisplayName("Should handle repeated parameters correctly")
    void testRepeatedParameters() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        
        String sql = "SELECT * FROM logs WHERE user_id = :userId OR created_by = :userId";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", 123L);

        // Act
        PreparedStatement result = JdbcParameterUtils.prepareStatement(mockConnection, sql, parameters);

        // Assert
        assertNotNull(result);
        verify(mockConnection).prepareStatement("SELECT * FROM logs WHERE user_id = ? OR created_by = ?");
        verify(mockStatement).setObject(1, 123L);
        verify(mockStatement).setObject(2, 123L);
    }

    @Test
    @DisplayName("Should handle null parameter values")
    void testNullParameterValues() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        
        String sql = "UPDATE users SET name = :name, email = :email WHERE id = :id";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", null);
        parameters.put("email", "test@example.com");
        parameters.put("id", 1L);

        // Act
        PreparedStatement result = JdbcParameterUtils.prepareStatement(mockConnection, sql, parameters);

        // Assert
        assertNotNull(result);
        verify(mockConnection).prepareStatement("UPDATE users SET name = ?, email = ? WHERE id = ?");
        verify(mockStatement).setObject(1, null);
        verify(mockStatement).setObject(2, "test@example.com");
        verify(mockStatement).setObject(3, 1L);
    }

    @Test
    @DisplayName("Should handle empty parameter map")
    void testEmptyParameterMap() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        
        String sql = "SELECT * FROM users";
        Map<String, Object> parameters = new HashMap<>();

        // Act
        PreparedStatement result = JdbcParameterUtils.prepareStatement(mockConnection, sql, parameters);

        // Assert
        assertNotNull(result);
        verify(mockConnection).prepareStatement("SELECT * FROM users");
        verifyNoMoreInteractions(mockStatement);
    }

    @Test
    @DisplayName("Should handle null parameter map")
    void testNullParameterMap() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        
        String sql = "SELECT * FROM users";

        // Act
        PreparedStatement result = JdbcParameterUtils.prepareStatement(mockConnection, sql, null);

        // Assert
        assertNotNull(result);
        verify(mockConnection).prepareStatement("SELECT * FROM users");
        verifyNoMoreInteractions(mockStatement);
    }

    @Test
    @DisplayName("Should handle SQL with no parameters")
    void testSqlWithNoParameters() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        
        String sql = "SELECT COUNT(*) FROM users";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("unused", "value");

        // Act
        PreparedStatement result = JdbcParameterUtils.prepareStatement(mockConnection, sql, parameters);

        // Assert
        assertNotNull(result);
        verify(mockConnection).prepareStatement("SELECT COUNT(*) FROM users");
        verifyNoMoreInteractions(mockStatement);
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
            JdbcParameterUtils.prepareStatement(mockConnection, null, new HashMap<>());
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
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        
        // This SQL has a colon in a string literal that should NOT be treated as a parameter
        String sql = "SELECT * FROM users WHERE name = :name AND description = 'Time: 10:30 AM'";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "John");

        // Act
        PreparedStatement result = JdbcParameterUtils.prepareStatement(mockConnection, sql, parameters);

        // Assert
        assertNotNull(result);
        verify(mockConnection).prepareStatement("SELECT * FROM users WHERE name = ? AND description = 'Time: 10:30 AM'");
        verify(mockStatement).setObject(1, "John");
    }
}
