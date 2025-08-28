package dev.mars.apex.core.service.data.external;

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


import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents the connection status of an external data source.
 * 
 * This class provides detailed information about the current state of a data source
 * connection, including status, timestamps, and error information.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class ConnectionStatus {
    
    /**
     * Enumeration of possible connection states.
     */
    public enum State {
        /**
         * Data source is not yet initialized.
         */
        NOT_INITIALIZED("Not Initialized", "Data source has not been initialized"),
        
        /**
         * Data source is currently connecting.
         */
        CONNECTING("Connecting", "Data source is establishing connection"),
        
        /**
         * Data source is connected and healthy.
         */
        CONNECTED("Connected", "Data source is connected and operational"),
        
        /**
         * Data source is disconnected but may reconnect.
         */
        DISCONNECTED("Disconnected", "Data source is disconnected"),
        
        /**
         * Data source connection failed with an error.
         */
        ERROR("Error", "Data source connection has an error"),
        
        /**
         * Data source is shutting down.
         */
        SHUTTING_DOWN("Shutting Down", "Data source is being shut down"),
        
        /**
         * Data source has been shut down.
         */
        SHUTDOWN("Shutdown", "Data source has been shut down");
        
        private final String displayName;
        private final String description;
        
        State(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * Check if this state indicates the data source is operational.
         * 
         * @return true if the data source can be used
         */
        public boolean isOperational() {
            return this == CONNECTED;
        }
        
        /**
         * Check if this state indicates an error condition.
         * 
         * @return true if there is an error
         */
        public boolean isError() {
            return this == ERROR;
        }
    }
    
    private final State state;
    private final LocalDateTime lastUpdated;
    private final LocalDateTime lastConnected;
    private final String message;
    private final Throwable error;
    private final long connectionAttempts;
    private final long successfulConnections;
    
    /**
     * Private constructor for ConnectionStatus.
     * Use the builder or static factory methods to create instances.
     */
    private ConnectionStatus(State state, LocalDateTime lastUpdated, LocalDateTime lastConnected,
                           String message, Throwable error, long connectionAttempts, 
                           long successfulConnections) {
        this.state = state;
        this.lastUpdated = lastUpdated;
        this.lastConnected = lastConnected;
        this.message = message;
        this.error = error;
        this.connectionAttempts = connectionAttempts;
        this.successfulConnections = successfulConnections;
    }
    
    /**
     * Create a ConnectionStatus indicating not initialized state.
     * 
     * @return ConnectionStatus with NOT_INITIALIZED state
     */
    public static ConnectionStatus notInitialized() {
        return new ConnectionStatus(State.NOT_INITIALIZED, LocalDateTime.now(), null, 
                                  "Data source not initialized", null, 0, 0);
    }
    
    /**
     * Create a ConnectionStatus indicating connecting state.
     * 
     * @return ConnectionStatus with CONNECTING state
     */
    public static ConnectionStatus connecting() {
        return new ConnectionStatus(State.CONNECTING, LocalDateTime.now(), null,
                                  "Establishing connection", null, 0, 0);
    }
    
    /**
     * Create a ConnectionStatus indicating connected state.
     * 
     * @param message Optional status message
     * @return ConnectionStatus with CONNECTED state
     */
    public static ConnectionStatus connected(String message) {
        return new ConnectionStatus(State.CONNECTED, LocalDateTime.now(), LocalDateTime.now(),
                                  message != null ? message : "Connected successfully", null, 0, 0);
    }
    
    /**
     * Create a ConnectionStatus indicating disconnected state.
     * 
     * @param message Optional status message
     * @return ConnectionStatus with DISCONNECTED state
     */
    public static ConnectionStatus disconnected(String message) {
        return new ConnectionStatus(State.DISCONNECTED, LocalDateTime.now(), null,
                                  message != null ? message : "Disconnected", null, 0, 0);
    }
    
    /**
     * Create a ConnectionStatus indicating error state.
     * 
     * @param message Error message
     * @param error The exception that caused the error
     * @return ConnectionStatus with ERROR state
     */
    public static ConnectionStatus error(String message, Throwable error) {
        return new ConnectionStatus(State.ERROR, LocalDateTime.now(), null,
                                  message != null ? message : "Connection error", error, 0, 0);
    }
    
    /**
     * Create a ConnectionStatus indicating shutdown state.
     * 
     * @return ConnectionStatus with SHUTDOWN state
     */
    public static ConnectionStatus shutdown() {
        return new ConnectionStatus(State.SHUTDOWN, LocalDateTime.now(), null,
                                  "Data source shut down", null, 0, 0);
    }
    
    // Getters
    
    public State getState() {
        return state;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public LocalDateTime getLastConnected() {
        return lastConnected;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Throwable getError() {
        return error;
    }
    
    public long getConnectionAttempts() {
        return connectionAttempts;
    }
    
    public long getSuccessfulConnections() {
        return successfulConnections;
    }
    
    /**
     * Check if the data source is currently connected.
     *
     * @return true if the data source is connected
     */
    public boolean isConnected() {
        return state == State.CONNECTED;
    }

    /**
     * Check if the data source is currently operational.
     *
     * @return true if the data source can be used
     */
    public boolean isOperational() {
        return state.isOperational();
    }
    
    /**
     * Check if there is an error condition.
     * 
     * @return true if there is an error
     */
    public boolean hasError() {
        return state.isError();
    }
    
    /**
     * Get a summary string of the connection status.
     * 
     * @return Summary string
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(state.getDisplayName());
        if (message != null && !message.isEmpty()) {
            sb.append(": ").append(message);
        }
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionStatus that = (ConnectionStatus) o;
        return connectionAttempts == that.connectionAttempts &&
               successfulConnections == that.successfulConnections &&
               state == that.state &&
               Objects.equals(lastUpdated, that.lastUpdated) &&
               Objects.equals(lastConnected, that.lastConnected) &&
               Objects.equals(message, that.message);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(state, lastUpdated, lastConnected, message, 
                          connectionAttempts, successfulConnections);
    }
    
    @Override
    public String toString() {
        return "ConnectionStatus{" +
               "state=" + state +
               ", lastUpdated=" + lastUpdated +
               ", lastConnected=" + lastConnected +
               ", message='" + message + '\'' +
               ", hasError=" + hasError() +
               ", connectionAttempts=" + connectionAttempts +
               ", successfulConnections=" + successfulConnections +
               '}';
    }
}
