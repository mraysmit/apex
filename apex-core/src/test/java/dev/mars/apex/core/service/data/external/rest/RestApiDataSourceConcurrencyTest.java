package dev.mars.apex.core.service.data.external.rest;

import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.datasource.CircuitBreakerConfig;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RestApiDataSourceConcurrencyTest {

    private LocalHttpServer server;
    private RestApiDataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        server = new LocalHttpServer();
        dataSource = new RestApiDataSource(HttpClient.newHttpClient(), createConfiguration(server.getPort()));
        dataSource.initialize(dataSource.getConfiguration());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dataSource != null) {
            dataSource.shutdown();
        }
        if (server != null) {
            server.close();
        }
    }

    @Test
    @DisplayName("Should handle concurrent cached API reads")
    void shouldHandleConcurrentCachedApiReads() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> warmResult = (Map<String, Object>) dataSource.getData("item", 42);
        assertNotNull(warmResult);
        assertEquals(42, warmResult.get("id"));
        assertEquals(1, server.getItemRequests());

        int operations = 30;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Map<String, Object>>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < operations; i++) {
                futures.add(executor.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) dataSource.getData("item", 42);
                    return result;
                }));
            }

            start.countDown();

            for (Future<Map<String, Object>> future : futures) {
                Map<String, Object> result = future.get(10, TimeUnit.SECONDS);
                assertNotNull(result);
                assertEquals(42, result.get("id"));
                assertEquals("Item 42", result.get("name"));
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        assertEquals(1, server.getItemRequests());

        DataSourceMetrics metrics = dataSource.getMetrics();
        assertEquals(operations + 1, metrics.getSuccessfulRequests());
        assertEquals(0, metrics.getFailedRequests());
        assertTrue(metrics.getCacheHits() >= operations);
    }

    @Test
    @DisplayName("Should update circuit breaker safely under concurrent failures")
    void shouldUpdateCircuitBreakerSafelyUnderConcurrentFailures() throws Exception {
        int operations = 12;
        ExecutorService executor = Executors.newFixedThreadPool(6);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < operations; i++) {
                futures.add(executor.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    return dataSource.getData("failing-item");
                }));
            }

            start.countDown();

            for (Future<Object> future : futures) {
                assertNull(future.get(10, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        CircuitBreaker circuitBreaker = readCircuitBreaker();
        assertNotNull(circuitBreaker);
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        assertTrue(server.getFailedRequests() >= 3);

        DataSourceMetrics metrics = dataSource.getMetrics();
        assertEquals(operations, metrics.getFailedRequests());
        assertEquals(0, metrics.getSuccessfulRequests());
    }

    private DataSourceConfiguration createConfiguration(int port) {
        DataSourceConfiguration configuration = new DataSourceConfiguration();
        configuration.setName("rest-concurrency-test");
        configuration.setType("rest-api");
        configuration.setSourceType("rest-api");
        configuration.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setBaseUrl("http://127.0.0.1:" + port);
        connectionConfig.setTimeout(5_000);
        configuration.setConnection(connectionConfig);

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("item", "/items/{id}");
        endpoints.put("failing-item", "/fail");
        configuration.setEndpoints(endpoints);
        configuration.setParameterNames(new String[]{"id"});

        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setEnabled(true);
        cacheConfig.setTtlSeconds(60L);
        cacheConfig.setMaxSize(100);
        configuration.setCache(cacheConfig);

        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig();
        circuitBreakerConfig.setEnabled(true);
        circuitBreakerConfig.setFailureThreshold(3);
        circuitBreakerConfig.setRequestVolumeThreshold(1);
        circuitBreakerConfig.setFailureRateThreshold(50.0);
        circuitBreakerConfig.setSuccessThreshold(1);
        circuitBreakerConfig.setTimeoutSeconds(60L);
        configuration.setCircuitBreaker(circuitBreakerConfig);

        return configuration;
    }

    private CircuitBreaker readCircuitBreaker() throws Exception {
        Field field = RestApiDataSource.class.getDeclaredField("circuitBreaker");
        field.setAccessible(true);
        return (CircuitBreaker) field.get(dataSource);
    }

    private static final class LocalHttpServer implements AutoCloseable {

        private final AtomicBoolean running = new AtomicBoolean(true);
        private final java.util.concurrent.atomic.AtomicInteger itemRequests = new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger failedRequests = new java.util.concurrent.atomic.AtomicInteger();
        private final ExecutorService clientExecutor = Executors.newCachedThreadPool();
        private final ServerSocket serverSocket;
        private final Thread acceptThread;

        private LocalHttpServer() throws IOException {
            serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            acceptThread = new Thread(this::acceptLoop, "rest-concurrency-test-server");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        private int getPort() {
            return serverSocket.getLocalPort();
        }

        private int getItemRequests() {
            return itemRequests.get();
        }

        private int getFailedRequests() {
            return failedRequests.get();
        }

        private void acceptLoop() {
            while (running.get()) {
                try {
                    Socket socket = serverSocket.accept();
                    clientExecutor.submit(() -> handleClient(socket));
                } catch (IOException ignored) {
                    if (!running.get()) {
                        return;
                    }
                }
            }
        }

        private void handleClient(Socket socket) {
            try (Socket client = socket;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                 OutputStream outputStream = client.getOutputStream()) {

                String requestLine = reader.readLine();
                if (requestLine == null || requestLine.isBlank()) {
                    return;
                }

                while (true) {
                    String headerLine = reader.readLine();
                    if (headerLine == null || headerLine.isEmpty()) {
                        break;
                    }
                }

                String path = requestLine.split(" ")[1];
                int statusCode;
                String body;

                if (path.equals("/health")) {
                    statusCode = 200;
                    body = "{\"status\":\"ok\"}";
                } else if (path.startsWith("/items/")) {
                    itemRequests.incrementAndGet();
                    String id = path.substring(path.lastIndexOf('/') + 1);
                    statusCode = 200;
                    body = "{\"id\":" + id + ",\"name\":\"Item " + id + "\"}";
                } else if (path.equals("/fail")) {
                    failedRequests.incrementAndGet();
                    statusCode = 500;
                    body = "{\"error\":\"boom\"}";
                } else {
                    statusCode = 404;
                    body = "{\"error\":\"not found\"}";
                }

                byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
                String response = "HTTP/1.1 " + statusCode + " " + reasonPhrase(statusCode) + "\r\n"
                    + "Content-Type: application/json\r\n"
                    + "Content-Length: " + responseBody.length + "\r\n"
                    + "Connection: close\r\n\r\n";
                outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                outputStream.write(responseBody);
                outputStream.flush();
            } catch (IOException ignored) {
            }
        }

        private static String reasonPhrase(int statusCode) {
            return switch (statusCode) {
                case 200 -> "OK";
                case 404 -> "Not Found";
                case 500 -> "Internal Server Error";
                default -> "OK";
            };
        }

        @Override
        public void close() throws Exception {
            running.set(false);
            serverSocket.close();
            clientExecutor.shutdownNow();
            acceptThread.join(2_000L);
        }
    }
}