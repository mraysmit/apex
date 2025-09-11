package dev.mars.apex.core.service.data.external.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for SqlErrorClassifier to ensure proper categorization of SQL errors.
 */
class SqlErrorClassifierTest {

    @Test
    @DisplayName("Should classify primary key violations as data integrity errors")
    void testPrimaryKeyViolationClassification() {
        // H2 primary key violation
        SQLException h2PrimaryKey = new SQLException(
            "Unique index or primary key violation: \"PRIMARY KEY ON PUBLIC.CUSTOMERS(CUSTOMER_ID)\"",
            "23505", 23505);
        
        SqlErrorClassifier.SqlErrorType result = SqlErrorClassifier.classifyError(h2PrimaryKey);
        assertEquals(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION, result);
        assertFalse(SqlErrorClassifier.shouldFailPipeline(result));
    }

    @Test
    @DisplayName("Should classify unique constraint violations as data integrity errors")
    void testUniqueConstraintViolationClassification() {
        SQLException uniqueConstraint = new SQLException(
            "duplicate key value violates unique constraint \"users_email_key\"",
            "23505", 23505);
        
        SqlErrorClassifier.SqlErrorType result = SqlErrorClassifier.classifyError(uniqueConstraint);
        assertEquals(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION, result);
        assertFalse(SqlErrorClassifier.shouldFailPipeline(result));
    }

    @Test
    @DisplayName("Should classify foreign key violations as data integrity errors")
    void testForeignKeyViolationClassification() {
        SQLException foreignKeyViolation = new SQLException(
            "insert or update on table \"orders\" violates foreign key constraint \"fk_customer\"",
            "23503", 23503);
        
        SqlErrorClassifier.SqlErrorType result = SqlErrorClassifier.classifyError(foreignKeyViolation);
        assertEquals(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION, result);
        assertFalse(SqlErrorClassifier.shouldFailPipeline(result));
    }

    @Test
    @DisplayName("Should classify not null violations as data integrity errors")
    void testNotNullViolationClassification() {
        SQLException notNullViolation = new SQLException(
            "NULL not allowed for column \"CUSTOMER_NAME\"",
            "23502", 23502);
        
        SqlErrorClassifier.SqlErrorType result = SqlErrorClassifier.classifyError(notNullViolation);
        assertEquals(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION, result);
        assertFalse(SqlErrorClassifier.shouldFailPipeline(result));
    }

    @Test
    @DisplayName("Should classify connection errors as transient errors")
    void testConnectionErrorClassification() {
        SQLException connectionError = new SQLException(
            "Connection refused: connect",
            "08001", 8001);
        
        SqlErrorClassifier.SqlErrorType result = SqlErrorClassifier.classifyError(connectionError);
        assertEquals(SqlErrorClassifier.SqlErrorType.TRANSIENT_ERROR, result);
        assertFalse(SqlErrorClassifier.shouldFailPipeline(result));
    }

    @Test
    @DisplayName("Should classify deadlock errors as transient errors")
    void testDeadlockErrorClassification() {
        SQLException deadlockError = new SQLException(
            "Deadlock detected. The current transaction was rolled back",
            "40001", 40001);
        
        SqlErrorClassifier.SqlErrorType result = SqlErrorClassifier.classifyError(deadlockError);
        assertEquals(SqlErrorClassifier.SqlErrorType.TRANSIENT_ERROR, result);
        assertFalse(SqlErrorClassifier.shouldFailPipeline(result));
    }

    @Test
    @DisplayName("Should classify table not found as configuration error")
    void testTableNotFoundClassification() {
        SQLException tableNotFound = new SQLException(
            "Table \"NONEXISTENT_TABLE\" not found",
            "42S02", 42000);
        
        SqlErrorClassifier.SqlErrorType result = SqlErrorClassifier.classifyError(tableNotFound);
        assertEquals(SqlErrorClassifier.SqlErrorType.CONFIGURATION_ERROR, result);
        assertTrue(SqlErrorClassifier.shouldFailPipeline(result));
    }

    @Test
    @DisplayName("Should classify column not found as configuration error")
    void testColumnNotFoundClassification() {
        SQLException columnNotFound = new SQLException(
            "Column \"NONEXISTENT_COLUMN\" not found",
            "42S22", 42000);
        
        SqlErrorClassifier.SqlErrorType result = SqlErrorClassifier.classifyError(columnNotFound);
        assertEquals(SqlErrorClassifier.SqlErrorType.CONFIGURATION_ERROR, result);
        assertTrue(SqlErrorClassifier.shouldFailPipeline(result));
    }

    @Test
    @DisplayName("Should classify syntax errors as configuration errors")
    void testSyntaxErrorClassification() {
        SQLException syntaxError = new SQLException(
            "Syntax error in SQL statement \"SELCT * FROM users\"",
            "42000", 42000);
        
        SqlErrorClassifier.SqlErrorType result = SqlErrorClassifier.classifyError(syntaxError);
        assertEquals(SqlErrorClassifier.SqlErrorType.CONFIGURATION_ERROR, result);
        assertTrue(SqlErrorClassifier.shouldFailPipeline(result));
    }

    @Test
    @DisplayName("Should classify unknown errors as fatal errors")
    void testUnknownErrorClassification() {
        SQLException unknownError = new SQLException(
            "Some unexpected database error",
            "99999", 99999);
        
        SqlErrorClassifier.SqlErrorType result = SqlErrorClassifier.classifyError(unknownError);
        assertEquals(SqlErrorClassifier.SqlErrorType.FATAL_ERROR, result);
        assertTrue(SqlErrorClassifier.shouldFailPipeline(result));
    }

    @Test
    @DisplayName("Should handle null SQLException gracefully")
    void testNullSQLExceptionHandling() {
        SqlErrorClassifier.SqlErrorType result = SqlErrorClassifier.classifyError(null);
        assertEquals(SqlErrorClassifier.SqlErrorType.FATAL_ERROR, result);
        assertTrue(SqlErrorClassifier.shouldFailPipeline(result));
    }

    @Test
    @DisplayName("Should provide meaningful error descriptions")
    void testErrorDescriptions() {
        assertEquals("Data integrity violation - record conflicts with existing data or constraints",
                    SqlErrorClassifier.getErrorDescription(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION));
        
        assertEquals("Transient database error - operation can be retried",
                    SqlErrorClassifier.getErrorDescription(SqlErrorClassifier.SqlErrorType.TRANSIENT_ERROR));
        
        assertEquals("Database configuration error - schema or query issue",
                    SqlErrorClassifier.getErrorDescription(SqlErrorClassifier.SqlErrorType.CONFIGURATION_ERROR));
        
        assertEquals("Fatal database error - requires investigation",
                    SqlErrorClassifier.getErrorDescription(SqlErrorClassifier.SqlErrorType.FATAL_ERROR));
    }

    @Test
    @DisplayName("Should classify errors based on message patterns when SQL state is missing")
    void testMessagePatternClassification() {
        // Test with null SQL state but recognizable message
        SQLException primaryKeyByMessage = new SQLException(
            "Unique index or primary key violation: customer_id already exists",
            null, 0);

        SqlErrorClassifier.SqlErrorType result = SqlErrorClassifier.classifyError(primaryKeyByMessage);
        assertEquals(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION, result);

        // Test connection error by message
        SQLException connectionByMessage = new SQLException(
            "Connection timeout occurred",
            null, 0);

        result = SqlErrorClassifier.classifyError(connectionByMessage);
        assertEquals(SqlErrorClassifier.SqlErrorType.TRANSIENT_ERROR, result);

        // Test configuration error by message
        SQLException configByMessage = new SQLException(
            "Table 'users' doesn't exist",
            null, 0);

        result = SqlErrorClassifier.classifyError(configByMessage);
        assertEquals(SqlErrorClassifier.SqlErrorType.CONFIGURATION_ERROR, result);
    }

    @Test
    @DisplayName("Should correctly identify DDL-related configuration errors")
    void testDDLConfigurationErrors() {
        // Test various DDL-related errors that should fail fast

        // Table not found
        SQLException tableNotFound = new SQLException(
            "Table \"CUSTOMERS\" not found; SQL statement: SELECT * FROM CUSTOMERS",
            "42S02", 42000);
        assertEquals(SqlErrorClassifier.SqlErrorType.CONFIGURATION_ERROR,
                    SqlErrorClassifier.classifyError(tableNotFound));
        assertTrue(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.classifyError(tableNotFound)));

        // Column not found
        SQLException columnNotFound = new SQLException(
            "Column \"INVALID_COLUMN\" not found; SQL statement: SELECT INVALID_COLUMN FROM CUSTOMERS",
            "42S22", 42000);
        assertEquals(SqlErrorClassifier.SqlErrorType.CONFIGURATION_ERROR,
                    SqlErrorClassifier.classifyError(columnNotFound));
        assertTrue(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.classifyError(columnNotFound)));

        // Invalid SQL syntax
        SQLException syntaxError = new SQLException(
            "Syntax error in SQL statement \"SELCT * FROM CUSTOMERS\"",
            "42000", 42000);
        assertEquals(SqlErrorClassifier.SqlErrorType.CONFIGURATION_ERROR,
                    SqlErrorClassifier.classifyError(syntaxError));
        assertTrue(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.classifyError(syntaxError)));
    }

    @Test
    @DisplayName("Should correctly identify DML-related data integrity violations")
    void testDMLDataIntegrityViolations() {
        // Test various DML-related integrity violations that should be handled gracefully

        // Primary key violation (H2 specific)
        SQLException h2PrimaryKey = new SQLException(
            "Unique index or primary key violation: \"PRIMARY KEY ON PUBLIC.CUSTOMERS(ID) ( /* key:1 */ 1)\"",
            "23505", 23505);
        assertEquals(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION,
                    SqlErrorClassifier.classifyError(h2PrimaryKey));
        assertFalse(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.classifyError(h2PrimaryKey)));

        // Foreign key violation
        SQLException foreignKey = new SQLException(
            "Referential integrity constraint violation: \"FK_ORDER_CUSTOMER: PUBLIC.ORDERS FOREIGN KEY(CUSTOMER_ID) REFERENCES PUBLIC.CUSTOMERS(ID) (1)\"",
            "23503", 23503);
        assertEquals(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION,
                    SqlErrorClassifier.classifyError(foreignKey));
        assertFalse(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.classifyError(foreignKey)));

        // Check constraint violation
        SQLException checkConstraint = new SQLException(
            "Check constraint violation: \"CHECK_STATUS\"",
            "23513", 23513);
        assertEquals(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION,
                    SqlErrorClassifier.classifyError(checkConstraint));
        assertFalse(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.classifyError(checkConstraint)));
    }

    @Test
    @DisplayName("Should correctly identify transient errors that can be retried")
    void testTransientErrors() {
        // Test various transient errors that should be retried

        // Connection failure
        SQLException connectionFailure = new SQLException(
            "Connection is closed",
            "08003", 8003);
        assertEquals(SqlErrorClassifier.SqlErrorType.TRANSIENT_ERROR,
                    SqlErrorClassifier.classifyError(connectionFailure));
        assertFalse(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.classifyError(connectionFailure)));

        // Deadlock
        SQLException deadlock = new SQLException(
            "Deadlock detected. The current transaction was rolled back",
            "40001", 40001);
        assertEquals(SqlErrorClassifier.SqlErrorType.TRANSIENT_ERROR,
                    SqlErrorClassifier.classifyError(deadlock));
        assertFalse(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.classifyError(deadlock)));

        // Timeout
        SQLException timeout = new SQLException(
            "Lock timeout; try restarting transaction",
            "40001", 40001);
        assertEquals(SqlErrorClassifier.SqlErrorType.TRANSIENT_ERROR,
                    SqlErrorClassifier.classifyError(timeout));
        assertFalse(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.classifyError(timeout)));
    }

    @Test
    @DisplayName("Should provide comprehensive error type coverage")
    void testComprehensiveErrorTypeCoverage() {
        // Verify that all error types have appropriate fail-fast behavior

        // Data integrity violations should NOT fail pipeline
        assertFalse(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION));

        // Transient errors should NOT fail pipeline (should be retried)
        assertFalse(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.SqlErrorType.TRANSIENT_ERROR));

        // Configuration errors SHOULD fail pipeline
        assertTrue(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.SqlErrorType.CONFIGURATION_ERROR));

        // Fatal errors SHOULD fail pipeline
        assertTrue(SqlErrorClassifier.shouldFailPipeline(SqlErrorClassifier.SqlErrorType.FATAL_ERROR));

        // Verify error descriptions are meaningful
        assertNotNull(SqlErrorClassifier.getErrorDescription(SqlErrorClassifier.SqlErrorType.DATA_INTEGRITY_VIOLATION));
        assertNotNull(SqlErrorClassifier.getErrorDescription(SqlErrorClassifier.SqlErrorType.TRANSIENT_ERROR));
        assertNotNull(SqlErrorClassifier.getErrorDescription(SqlErrorClassifier.SqlErrorType.CONFIGURATION_ERROR));
        assertNotNull(SqlErrorClassifier.getErrorDescription(SqlErrorClassifier.SqlErrorType.FATAL_ERROR));
    }
}
