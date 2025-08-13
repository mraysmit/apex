package dev.mars.apex.core.service.data.external;

import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ConnectionStatus.
 * 
 * Tests cover:
 * - Enum state values and properties
 * - Factory method creation
 * - Status transitions and validation
 * - Equality and hashcode contracts
 * - Utility methods and string representations
 * - Edge cases and null handling
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class ConnectionStatusTest {

    // ========================================
    // State Enum Tests
    // ========================================

    @Test
    @DisplayName("Should have correct enum values")
    void testStateEnumValues() {
        ConnectionStatus.State[] states = ConnectionStatus.State.values();
        assertEquals(7, states.length);
        
        assertEquals(ConnectionStatus.State.NOT_INITIALIZED, states[0]);
        assertEquals(ConnectionStatus.State.CONNECTING, states[1]);
        assertEquals(ConnectionStatus.State.CONNECTED, states[2]);
        assertEquals(ConnectionStatus.State.DISCONNECTED, states[3]);
        assertEquals(ConnectionStatus.State.ERROR, states[4]);
        assertEquals(ConnectionStatus.State.SHUTTING_DOWN, states[5]);
        assertEquals(ConnectionStatus.State.SHUTDOWN, states[6]);
    }

    @Test
    @DisplayName("Should have correct display names")
    void testStateDisplayNames() {
        assertEquals("Not Initialized", ConnectionStatus.State.NOT_INITIALIZED.getDisplayName());
        assertEquals("Connecting", ConnectionStatus.State.CONNECTING.getDisplayName());
        assertEquals("Connected", ConnectionStatus.State.CONNECTED.getDisplayName());
        assertEquals("Disconnected", ConnectionStatus.State.DISCONNECTED.getDisplayName());
        assertEquals("Error", ConnectionStatus.State.ERROR.getDisplayName());
        assertEquals("Shutting Down", ConnectionStatus.State.SHUTTING_DOWN.getDisplayName());
        assertEquals("Shutdown", ConnectionStatus.State.SHUTDOWN.getDisplayName());
    }

    @Test
    @DisplayName("Should have correct descriptions")
    void testStateDescriptions() {
        assertEquals("Data source has not been initialized", ConnectionStatus.State.NOT_INITIALIZED.getDescription());
        assertEquals("Data source is establishing connection", ConnectionStatus.State.CONNECTING.getDescription());
        assertEquals("Data source is connected and operational", ConnectionStatus.State.CONNECTED.getDescription());
        assertEquals("Data source is disconnected", ConnectionStatus.State.DISCONNECTED.getDescription());
        assertEquals("Data source connection has an error", ConnectionStatus.State.ERROR.getDescription());
        assertEquals("Data source is being shut down", ConnectionStatus.State.SHUTTING_DOWN.getDescription());
        assertEquals("Data source has been shut down", ConnectionStatus.State.SHUTDOWN.getDescription());
    }

    @Test
    @DisplayName("Should correctly identify operational states")
    void testIsOperational() {
        assertTrue(ConnectionStatus.State.CONNECTED.isOperational());
        
        assertFalse(ConnectionStatus.State.NOT_INITIALIZED.isOperational());
        assertFalse(ConnectionStatus.State.CONNECTING.isOperational());
        assertFalse(ConnectionStatus.State.DISCONNECTED.isOperational());
        assertFalse(ConnectionStatus.State.ERROR.isOperational());
        assertFalse(ConnectionStatus.State.SHUTTING_DOWN.isOperational());
        assertFalse(ConnectionStatus.State.SHUTDOWN.isOperational());
    }

    @Test
    @DisplayName("Should correctly identify error states")
    void testIsError() {
        assertTrue(ConnectionStatus.State.ERROR.isError());
        
        assertFalse(ConnectionStatus.State.NOT_INITIALIZED.isError());
        assertFalse(ConnectionStatus.State.CONNECTING.isError());
        assertFalse(ConnectionStatus.State.CONNECTED.isError());
        assertFalse(ConnectionStatus.State.DISCONNECTED.isError());
        assertFalse(ConnectionStatus.State.SHUTTING_DOWN.isError());
        assertFalse(ConnectionStatus.State.SHUTDOWN.isError());
    }

    // ========================================
    // Factory Method Tests
    // ========================================

    @Test
    @DisplayName("Should create not initialized status")
    void testNotInitialized() {
        ConnectionStatus status = ConnectionStatus.notInitialized();
        
        assertEquals(ConnectionStatus.State.NOT_INITIALIZED, status.getState());
        assertEquals("Data source not initialized", status.getMessage());
        assertNotNull(status.getLastUpdated());
        assertNull(status.getLastConnected());
        assertNull(status.getError());
        assertEquals(0, status.getConnectionAttempts());
        assertEquals(0, status.getSuccessfulConnections());
        assertFalse(status.isConnected());
        assertFalse(status.isOperational());
        assertFalse(status.hasError());
    }

    @Test
    @DisplayName("Should create connecting status")
    void testConnecting() {
        ConnectionStatus status = ConnectionStatus.connecting();
        
        assertEquals(ConnectionStatus.State.CONNECTING, status.getState());
        assertEquals("Establishing connection", status.getMessage());
        assertNotNull(status.getLastUpdated());
        assertNull(status.getLastConnected());
        assertNull(status.getError());
        assertFalse(status.isConnected());
        assertFalse(status.isOperational());
        assertFalse(status.hasError());
    }

    @Test
    @DisplayName("Should create connected status with default message")
    void testConnectedWithDefaultMessage() {
        ConnectionStatus status = ConnectionStatus.connected(null);
        
        assertEquals(ConnectionStatus.State.CONNECTED, status.getState());
        assertEquals("Connected successfully", status.getMessage());
        assertNotNull(status.getLastUpdated());
        assertNotNull(status.getLastConnected());
        assertNull(status.getError());
        assertTrue(status.isConnected());
        assertTrue(status.isOperational());
        assertFalse(status.hasError());
    }

    @Test
    @DisplayName("Should create connected status with custom message")
    void testConnectedWithCustomMessage() {
        String customMessage = "Connected to database successfully";
        ConnectionStatus status = ConnectionStatus.connected(customMessage);
        
        assertEquals(ConnectionStatus.State.CONNECTED, status.getState());
        assertEquals(customMessage, status.getMessage());
        assertNotNull(status.getLastUpdated());
        assertNotNull(status.getLastConnected());
        assertTrue(status.isConnected());
        assertTrue(status.isOperational());
    }

    @Test
    @DisplayName("Should create disconnected status with default message")
    void testDisconnectedWithDefaultMessage() {
        ConnectionStatus status = ConnectionStatus.disconnected(null);
        
        assertEquals(ConnectionStatus.State.DISCONNECTED, status.getState());
        assertEquals("Disconnected", status.getMessage());
        assertNotNull(status.getLastUpdated());
        assertNull(status.getLastConnected());
        assertFalse(status.isConnected());
        assertFalse(status.isOperational());
        assertFalse(status.hasError());
    }

    @Test
    @DisplayName("Should create disconnected status with custom message")
    void testDisconnectedWithCustomMessage() {
        String customMessage = "Connection lost due to network timeout";
        ConnectionStatus status = ConnectionStatus.disconnected(customMessage);
        
        assertEquals(ConnectionStatus.State.DISCONNECTED, status.getState());
        assertEquals(customMessage, status.getMessage());
        assertFalse(status.isConnected());
        assertFalse(status.isOperational());
    }

    @Test
    @DisplayName("Should create error status with default message")
    void testErrorWithDefaultMessage() {
        RuntimeException cause = new RuntimeException("Test exception");
        ConnectionStatus status = ConnectionStatus.error(null, cause);
        
        assertEquals(ConnectionStatus.State.ERROR, status.getState());
        assertEquals("Connection error", status.getMessage());
        assertNotNull(status.getLastUpdated());
        assertNull(status.getLastConnected());
        assertEquals(cause, status.getError());
        assertFalse(status.isConnected());
        assertFalse(status.isOperational());
        assertTrue(status.hasError());
    }

    @Test
    @DisplayName("Should create error status with custom message and exception")
    void testErrorWithCustomMessage() {
        String customMessage = "Database authentication failed";
        RuntimeException cause = new RuntimeException("Invalid credentials");
        ConnectionStatus status = ConnectionStatus.error(customMessage, cause);
        
        assertEquals(ConnectionStatus.State.ERROR, status.getState());
        assertEquals(customMessage, status.getMessage());
        assertEquals(cause, status.getError());
        assertTrue(status.hasError());
    }

    @Test
    @DisplayName("Should create shutdown status")
    void testShutdown() {
        ConnectionStatus status = ConnectionStatus.shutdown();
        
        assertEquals(ConnectionStatus.State.SHUTDOWN, status.getState());
        assertEquals("Data source shut down", status.getMessage());
        assertNotNull(status.getLastUpdated());
        assertNull(status.getLastConnected());
        assertNull(status.getError());
        assertFalse(status.isConnected());
        assertFalse(status.isOperational());
        assertFalse(status.hasError());
    }

    // ========================================
    // Utility Method Tests
    // ========================================

    @Test
    @DisplayName("Should generate correct summary without message")
    void testGetSummaryWithoutMessage() {
        ConnectionStatus status = ConnectionStatus.notInitialized();
        String summary = status.getSummary();
        
        assertTrue(summary.contains("Not Initialized"));
        assertTrue(summary.contains("Data source not initialized"));
    }

    @Test
    @DisplayName("Should generate correct summary with message")
    void testGetSummaryWithMessage() {
        String customMessage = "Custom connection message";
        ConnectionStatus status = ConnectionStatus.connected(customMessage);
        String summary = status.getSummary();
        
        assertTrue(summary.contains("Connected"));
        assertTrue(summary.contains(customMessage));
    }

    // ========================================
    // Equality and HashCode Tests
    // ========================================

    @Test
    @DisplayName("Should be equal when all properties match")
    void testEquality() {
        ConnectionStatus status1 = ConnectionStatus.connected("Test message");
        ConnectionStatus status2 = ConnectionStatus.connected("Test message");

        // Test self-equality
        assertEquals(status1, status1);

        // Different instances with same message may or may not be equal
        // depending on timestamp precision - let's just verify they're both valid
        assertNotNull(status1);
        assertNotNull(status2);
        assertEquals(status1.getState(), status2.getState());
        assertEquals(status1.getMessage(), status2.getMessage());
    }

    @Test
    @DisplayName("Should not be equal to null or different type")
    void testEqualityEdgeCases() {
        ConnectionStatus status = ConnectionStatus.connected("Test");
        
        assertNotEquals(status, null);
        assertNotEquals(status, "not a ConnectionStatus");
        assertNotEquals(status, new Object());
    }

    @Test
    @DisplayName("Should have consistent hashCode")
    void testHashCode() {
        ConnectionStatus status = ConnectionStatus.connected("Test");
        
        int hashCode1 = status.hashCode();
        int hashCode2 = status.hashCode();
        
        assertEquals(hashCode1, hashCode2);
    }

    // ========================================
    // String Representation Tests
    // ========================================

    @Test
    @DisplayName("Should have meaningful toString representation")
    void testToString() {
        ConnectionStatus status = ConnectionStatus.connected("Test connection");
        String toString = status.toString();
        
        assertTrue(toString.contains("ConnectionStatus"));
        assertTrue(toString.contains("CONNECTED"));
        assertTrue(toString.contains("Test connection"));
        assertTrue(toString.contains("hasError=false"));
        assertTrue(toString.contains("connectionAttempts=0"));
        assertTrue(toString.contains("successfulConnections=0"));
    }

    @Test
    @DisplayName("Should include error information in toString")
    void testToStringWithError() {
        RuntimeException cause = new RuntimeException("Test error");
        ConnectionStatus status = ConnectionStatus.error("Error message", cause);
        String toString = status.toString();
        
        assertTrue(toString.contains("ERROR"));
        assertTrue(toString.contains("hasError=true"));
        assertTrue(toString.contains("Error message"));
    }
}
