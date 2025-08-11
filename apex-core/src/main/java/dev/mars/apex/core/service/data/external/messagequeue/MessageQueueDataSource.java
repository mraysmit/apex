package dev.mars.apex.core.service.data.external.messagequeue;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Message queue implementation of ExternalDataSource.
 * 
 * This class provides message queue-based data access with support for multiple
 * message queue backends including Kafka, RabbitMQ, ActiveMQ, and others.
 * 
 * Supported message queue types:
 * - Kafka (default)
 * - RabbitMQ
 * - ActiveMQ
 * - Custom message queue implementations
 * 
 * Features:
 * - Asynchronous message consumption
 * - Topic/queue-based messaging
 * - Message buffering and batching
 * - Producer and consumer capabilities
 * - Health monitoring and metrics
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class MessageQueueDataSource implements ExternalDataSource {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageQueueDataSource.class);
    
    private DataSourceConfiguration configuration;
    private ConnectionStatus connectionStatus;
    private DataSourceMetrics metrics;
    private final BlockingQueue<Object> messageBuffer;
    private volatile boolean running;
    private ExecutorService consumerExecutor;
    
    /**
     * Constructor for message queue data source.
     */
    public MessageQueueDataSource() {
        this.messageBuffer = new LinkedBlockingQueue<>();
        this.metrics = new DataSourceMetrics();
        this.connectionStatus = ConnectionStatus.disconnected("Not initialized");
        this.running = false;
    }
    
    @Override
    public void initialize(DataSourceConfiguration config) throws DataSourceException {
        this.configuration = config;
        
        try {
            // Initialize message queue connection based on source type
            String sourceType = config.getSourceType();
            if (sourceType == null) {
                sourceType = "kafka"; // Default to Kafka
            }
            
            switch (sourceType.toLowerCase()) {
                case "kafka":
                    initializeKafka();
                    break;
                case "rabbitmq":
                    initializeRabbitMQ();
                    break;
                case "activemq":
                    initializeActiveMQ();
                    break;
                default:
                    throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                        "Unsupported message queue type: " + sourceType);
            }
            
            this.running = true;
            this.connectionStatus = ConnectionStatus.connected("Message queue connection established");
            LOGGER.info("Message queue data source '{}' initialized successfully", config.getName());
            
        } catch (Exception e) {
            this.connectionStatus = ConnectionStatus.error("Initialization failed", e);
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to initialize message queue data source", e, config.getName(), "initialize", false);
        }
    }
    
    @Override
    public DataSourceType getSourceType() {
        return DataSourceType.MESSAGE_QUEUE;
    }
    
    @Override
    public String getName() {
        return configuration != null ? configuration.getName() : "message-queue";
    }
    
    @Override
    public String getDataType() {
        return configuration != null ? configuration.getSourceType() : "message";
    }

    @Override
    public boolean supportsDataType(String dataType) {
        if (dataType == null) {
            return false;
        }

        // Message queue data sources support "message" type and their specific source type
        return "message".equals(dataType) ||
               "message-queue".equals(dataType) ||
               (configuration != null && dataType.equals(configuration.getSourceType()));
    }
    
    @Override
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }
    
    @Override
    public DataSourceMetrics getMetrics() {
        return metrics;
    }
    
    @Override
    public boolean isHealthy() {
        return running && connectionStatus.isConnected();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(String dataType, Object... parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (!running) {
                return null;
            }
            
            // For message queues, we typically consume messages rather than query
            // The dataType parameter can specify the topic or queue to consume from
            Object message = consumeMessage(dataType, parameters);
            
            metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
            return (T) message;
            
        } catch (Exception e) {
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);
            LOGGER.error("Failed to get data from message queue", e);
            return null;
        }
    }
    
    @Override
    public <T> List<T> query(String query, Map<String, Object> parameters) throws DataSourceException {
        // For message queues, "query" typically means consuming multiple messages
        try {
            List<T> results = new ArrayList<>();
            int maxMessages = parameters.containsKey("maxMessages") ? 
                (Integer) parameters.get("maxMessages") : 10;
            long timeoutMs = parameters.containsKey("timeout") ? 
                (Long) parameters.get("timeout") : 5000L;
            
            for (int i = 0; i < maxMessages; i++) {
                @SuppressWarnings("unchecked")
                T message = (T) messageBuffer.poll(timeoutMs, TimeUnit.MILLISECONDS);
                if (message == null) {
                    break; // No more messages available
                }
                results.add(message);
            }
            
            metrics.recordRecordsProcessed(results.size());
            return results;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw DataSourceException.executionError("Message queue query interrupted", e, "query");
        }
    }
    
    @Override
    public <T> T queryForObject(String query, Map<String, Object> parameters) throws DataSourceException {
        List<T> results = query(query, parameters);
        return results.isEmpty() ? null : results.get(0);
    }
    
    @Override
    public <T> List<List<T>> batchQuery(List<String> queries) throws DataSourceException {
        List<List<T>> results = new ArrayList<>();
        
        for (String query : queries) {
            List<T> queryResult = query(query, Collections.emptyMap());
            results.add(queryResult);
        }
        
        return results;
    }
    
    @Override
    public void batchUpdate(List<String> updates) throws DataSourceException {
        // For message queues, updates typically mean publishing messages
        for (String update : updates) {
            try {
                publishMessage(update, Collections.emptyMap());
            } catch (Exception e) {
                throw DataSourceException.executionError("Message queue batch update failed", e, "batchUpdate");
            }
        }
    }
    
    @Override
    public DataSourceConfiguration getConfiguration() {
        return configuration;
    }
    
    @Override
    public void refresh() throws DataSourceException {
        // For message queues, refresh might mean reconnecting or clearing buffers
        if (running) {
            messageBuffer.clear();
            LOGGER.info("Message queue data source '{}' refreshed", getName());
        }
    }
    
    @Override
    public void shutdown() {
        running = false;
        if (consumerExecutor != null && !consumerExecutor.isShutdown()) {
            consumerExecutor.shutdown();
            try {
                if (!consumerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    consumerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                consumerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        messageBuffer.clear();
        connectionStatus = ConnectionStatus.shutdown();
        LOGGER.info("Message queue data source '{}' shut down", getName());
    }
    
    @Override
    public boolean testConnection() {
        return running && connectionStatus.isConnected();
    }
    
    /**
     * Initialize Kafka connection.
     */
    private void initializeKafka() throws DataSourceException {
        // Basic Kafka initialization - would need actual Kafka dependencies in production
        LOGGER.info("Initializing Kafka message queue connection");
        
        // For now, create a mock implementation that simulates Kafka behavior
        // In a real implementation, this would create KafkaProducer and KafkaConsumer
        this.consumerExecutor = Executors.newSingleThreadExecutor();
        
        // Start a background consumer simulation
        consumerExecutor.submit(this::simulateMessageConsumption);
    }
    
    /**
     * Initialize RabbitMQ connection.
     */
    private void initializeRabbitMQ() throws DataSourceException {
        LOGGER.info("Initializing RabbitMQ message queue connection");

        // Basic RabbitMQ initialization - would need actual RabbitMQ dependencies in production
        // For now, create a mock implementation that simulates RabbitMQ behavior
        this.consumerExecutor = Executors.newSingleThreadExecutor();

        // Start a background consumer simulation
        consumerExecutor.submit(this::simulateMessageConsumption);

        LOGGER.info("RabbitMQ message queue connection initialized (simulation mode)");
    }
    
    /**
     * Initialize ActiveMQ connection.
     */
    private void initializeActiveMQ() throws DataSourceException {
        LOGGER.info("Initializing ActiveMQ message queue connection");

        // Basic ActiveMQ initialization - would need actual ActiveMQ dependencies in production
        // For now, create a mock implementation that simulates ActiveMQ behavior
        this.consumerExecutor = Executors.newSingleThreadExecutor();

        // Start a background consumer simulation
        consumerExecutor.submit(this::simulateMessageConsumption);

        LOGGER.info("ActiveMQ message queue connection initialized (simulation mode)");
    }
    
    /**
     * Consume a message from the specified topic/queue.
     */
    private Object consumeMessage(String topic, Object... parameters) throws InterruptedException {
        // In a real implementation, this would consume from the specific topic
        long timeoutMs = parameters.length > 0 && parameters[0] instanceof Long ? 
            (Long) parameters[0] : 1000L;
        
        return messageBuffer.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Publish a message to the specified topic/queue.
     */
    private void publishMessage(String topic, Map<String, Object> parameters) {
        // In a real implementation, this would publish to the specific topic
        Object message = parameters.get("message");
        if (message != null) {
            messageBuffer.offer(message);
            LOGGER.debug("Published message to topic '{}': {}", topic, message);
        }
    }
    
    /**
     * Simulate message consumption for demo purposes.
     */
    private void simulateMessageConsumption() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Simulate receiving messages periodically
                Thread.sleep(2000);
                if (running) {
                    Map<String, Object> simulatedMessage = Map.of(
                        "timestamp", System.currentTimeMillis(),
                        "messageId", UUID.randomUUID().toString(),
                        "data", "Simulated message data"
                    );
                    messageBuffer.offer(simulatedMessage);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
