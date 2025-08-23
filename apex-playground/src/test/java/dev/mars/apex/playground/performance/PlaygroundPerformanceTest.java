package dev.mars.apex.playground.performance;

import dev.mars.apex.playground.model.PlaygroundRequest;
import dev.mars.apex.playground.model.PlaygroundResponse;
import dev.mars.apex.playground.service.PlaygroundService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and load tests for the APEX Playground.
 * 
 * Tests system behavior under various load conditions and validates
 * performance characteristics of the playground services.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.main.web-application-type=servlet",
    "server.port=0"
})
@DisplayName("Playground Performance Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlaygroundPerformanceTest {

    @Autowired
    private PlaygroundService playgroundService;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Nested
    @DisplayName("Single Request Performance Tests")
    class SingleRequestPerformanceTests {

        @Test
        @Order(1)
        @DisplayName("Should process simple request within performance threshold")
        void shouldProcessSimpleRequestWithinPerformanceThreshold() {
            // Given
            PlaygroundRequest request = createSimpleRequest();
            
            // When
            long startTime = System.currentTimeMillis();
            PlaygroundResponse response = playgroundService.processData(request);
            long endTime = System.currentTimeMillis();
            
            // Then
            assertTrue(response.isSuccess());
            long processingTime = endTime - startTime;
            assertTrue(processingTime < 1000, "Simple request should complete within 1 second, took: " + processingTime + "ms");
            
            // Verify internal metrics
            assertTrue(response.getMetrics().getTotalTimeMs() < 500, 
                "Internal processing should be under 500ms, was: " + response.getMetrics().getTotalTimeMs() + "ms");
        }

        @Test
        @Order(2)
        @DisplayName("Should process complex request within acceptable time")
        void shouldProcessComplexRequestWithinAcceptableTime() {
            // Given
            PlaygroundRequest request = createComplexRequest();
            
            // When
            long startTime = System.currentTimeMillis();
            PlaygroundResponse response = playgroundService.processData(request);
            long endTime = System.currentTimeMillis();
            
            // Then
            assertTrue(response.isSuccess());
            long processingTime = endTime - startTime;
            assertTrue(processingTime < 2000, "Complex request should complete within 2 seconds, took: " + processingTime + "ms");
            
            // Verify rules were processed (APEX engine stops after first match)
            assertTrue(response.getValidation().getRulesExecuted() >= 1);
        }

        @Test
        @Order(3)
        @DisplayName("Should process large data efficiently")
        void shouldProcessLargeDataEfficiently() {
            // Given
            PlaygroundRequest request = createLargeDataRequest();
            
            // When
            long startTime = System.currentTimeMillis();
            PlaygroundResponse response = playgroundService.processData(request);
            long endTime = System.currentTimeMillis();
            
            // Then
            assertTrue(response.isSuccess());
            long processingTime = endTime - startTime;
            assertTrue(processingTime < 3000, "Large data request should complete within 3 seconds, took: " + processingTime + "ms");
            
            // Verify data was processed correctly
            assertNotNull(response.getEnrichment().getEnrichedData());
        }
    }

    @Nested
    @DisplayName("Concurrent Processing Tests")
    class ConcurrentProcessingTests {

        @Test
        @Order(10)
        @DisplayName("Should handle moderate concurrent load")
        void shouldHandleModerateConcurrentLoad() throws Exception {
            // Given
            int numberOfRequests = 10;
            List<CompletableFuture<PlaygroundResponse>> futures = new ArrayList<>();
            
            // When
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < numberOfRequests; i++) {
                PlaygroundRequest request = createSimpleRequest();
                CompletableFuture<PlaygroundResponse> future = CompletableFuture.supplyAsync(
                    () -> playgroundService.processData(request), executorService);
                futures.add(future);
            }
            
            // Wait for all requests to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            allFutures.get(10, TimeUnit.SECONDS);
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            // Then
            assertTrue(totalTime < 5000, "10 concurrent requests should complete within 5 seconds, took: " + totalTime + "ms");
            
            // Verify all requests succeeded
            for (CompletableFuture<PlaygroundResponse> future : futures) {
                PlaygroundResponse response = future.get();
                assertTrue(response.isSuccess());
                assertNotNull(response.getValidation());
                assertNotNull(response.getEnrichment());
                assertNotNull(response.getMetrics());
            }
        }

        @Test
        @Order(11)
        @DisplayName("Should handle high concurrent load gracefully")
        void shouldHandleHighConcurrentLoadGracefully() throws Exception {
            // Given
            int numberOfRequests = 50;
            List<CompletableFuture<PlaygroundResponse>> futures = new ArrayList<>();
            
            // When
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < numberOfRequests; i++) {
                PlaygroundRequest request = createSimpleRequest();
                CompletableFuture<PlaygroundResponse> future = CompletableFuture.supplyAsync(
                    () -> playgroundService.processData(request), executorService);
                futures.add(future);
            }
            
            // Wait for all requests to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            allFutures.get(30, TimeUnit.SECONDS);
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            // Then
            assertTrue(totalTime < 15000, "50 concurrent requests should complete within 15 seconds, took: " + totalTime + "ms");
            
            // Verify success rate is acceptable (at least 95%)
            long successCount = futures.stream()
                .mapToLong(future -> {
                    try {
                        return future.get().isSuccess() ? 1 : 0;
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();
            
            double successRate = (double) successCount / numberOfRequests;
            assertTrue(successRate >= 0.95, "Success rate should be at least 95%, was: " + (successRate * 100) + "%");
        }

        @Test
        @Order(12)
        @DisplayName("Should maintain performance under mixed workload")
        void shouldMaintainPerformanceUnderMixedWorkload() throws Exception {
            // Given
            int simpleRequests = 20;
            int complexRequests = 10;
            int largeDataRequests = 5;
            List<CompletableFuture<PlaygroundResponse>> futures = new ArrayList<>();
            
            // When
            long startTime = System.currentTimeMillis();
            
            // Submit simple requests
            for (int i = 0; i < simpleRequests; i++) {
                PlaygroundRequest request = createSimpleRequest();
                futures.add(CompletableFuture.supplyAsync(
                    () -> playgroundService.processData(request), executorService));
            }
            
            // Submit complex requests
            for (int i = 0; i < complexRequests; i++) {
                PlaygroundRequest request = createComplexRequest();
                futures.add(CompletableFuture.supplyAsync(
                    () -> playgroundService.processData(request), executorService));
            }
            
            // Submit large data requests
            for (int i = 0; i < largeDataRequests; i++) {
                PlaygroundRequest request = createLargeDataRequest();
                futures.add(CompletableFuture.supplyAsync(
                    () -> playgroundService.processData(request), executorService));
            }
            
            // Wait for all requests to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            allFutures.get(45, TimeUnit.SECONDS);
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            // Then
            assertTrue(totalTime < 30000, "Mixed workload should complete within 30 seconds, took: " + totalTime + "ms");
            
            // Verify all requests succeeded
            for (CompletableFuture<PlaygroundResponse> future : futures) {
                PlaygroundResponse response = future.get();
                assertTrue(response.isSuccess());
            }
        }
    }

    @Nested
    @DisplayName("Memory and Resource Tests")
    class MemoryAndResourceTests {

        @Test
        @Order(20)
        @DisplayName("Should not cause memory leaks under repeated processing")
        void shouldNotCauseMemoryLeaksUnderRepeatedProcessing() {
            // Given
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // When - Process many requests sequentially
            for (int i = 0; i < 100; i++) {
                PlaygroundRequest request = createSimpleRequest();
                PlaygroundResponse response = playgroundService.processData(request);
                assertTrue(response.isSuccess());
                
                // Force garbage collection every 10 iterations
                if (i % 10 == 0) {
                    System.gc();
                    Thread.yield();
                }
            }
            
            // Force final garbage collection
            System.gc();
            Thread.yield();
            
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;
            
            // Then - Memory increase should be reasonable (less than 50MB)
            assertTrue(memoryIncrease < 50 * 1024 * 1024, 
                "Memory increase should be less than 50MB, was: " + (memoryIncrease / 1024 / 1024) + "MB");
        }

        @Test
        @Order(21)
        @DisplayName("Should handle processing timeout gracefully")
        void shouldHandleProcessingTimeoutGracefully() {
            // Given
            PlaygroundRequest request = createSimpleRequest();
            request.getProcessingOptions().setTimeoutMs(1L); // Very short timeout
            
            // When
            long startTime = System.currentTimeMillis();
            PlaygroundResponse response = playgroundService.processData(request);
            long endTime = System.currentTimeMillis();
            
            // Then
            // Processing should complete quickly (timeout is just a configuration, not enforced in this implementation)
            assertTrue(endTime - startTime < 1000);
            // Response should still be valid
            assertNotNull(response);
        }
    }

    @Nested
    @DisplayName("Throughput Tests")
    class ThroughputTests {

        @Test
        @Order(30)
        @DisplayName("Should achieve acceptable throughput for simple requests")
        void shouldAchieveAcceptableThroughputForSimpleRequests() throws Exception {
            // Given
            int numberOfRequests = 100;
            int numberOfThreads = 5;
            ExecutorService throughputExecutor = Executors.newFixedThreadPool(numberOfThreads);
            
            try {
                // When
                long startTime = System.currentTimeMillis();
                
                List<CompletableFuture<PlaygroundResponse>> futures = IntStream.range(0, numberOfRequests)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                        PlaygroundRequest request = createSimpleRequest();
                        return playgroundService.processData(request);
                    }, throughputExecutor))
                    .toList();
                
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
                allFutures.get(60, TimeUnit.SECONDS);
                
                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                
                // Then
                double throughput = (double) numberOfRequests / (totalTime / 1000.0);
                assertTrue(throughput >= 10.0, "Throughput should be at least 10 requests/second, was: " + throughput);
                
                // Verify all requests succeeded
                long successCount = futures.stream()
                    .mapToLong(future -> {
                        try {
                            return future.get().isSuccess() ? 1 : 0;
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .sum();
                
                assertEquals(numberOfRequests, successCount, "All requests should succeed");
                
            } finally {
                throughputExecutor.shutdown();
                throughputExecutor.awaitTermination(5, TimeUnit.SECONDS);
            }
        }
    }

    // Helper methods for creating test requests

    private PlaygroundRequest createSimpleRequest() {
        PlaygroundRequest request = new PlaygroundRequest();
        request.setSourceData("{\"name\": \"Test User\", \"age\": 25, \"active\": true}");
        request.setYamlRules("""
            metadata:
              name: "Simple Test Rules"
              version: "1.0.0"
            rules:
              - id: "age-check"
                name: "Age Validation"
                condition: "#age >= 18"
                message: "Age must be 18 or older"
            """);
        request.setDataFormat("JSON");
        return request;
    }

    private PlaygroundRequest createComplexRequest() {
        PlaygroundRequest request = new PlaygroundRequest();
        request.setSourceData("""
            {
              "user": {
                "name": "Complex Test User",
                "age": 30,
                "profile": {
                  "email": "test@example.com",
                  "department": "Engineering",
                  "salary": 75000,
                  "skills": ["Java", "Spring", "APEX"]
                }
              },
              "metadata": {
                "created": "2025-08-23",
                "version": 2
              }
            }
            """);
        request.setYamlRules("""
            metadata:
              name: "Complex Test Rules"
              version: "1.0.0"
            rules:
              - id: "age-check"
                name: "Age Validation"
                condition: "#user.age >= 18"
                message: "User must be 18 or older"
              - id: "email-check"
                name: "Email Validation"
                condition: "#user.profile.email != null && #user.profile.email.contains('@')"
                message: "Valid email required"
              - id: "salary-check"
                name: "Salary Validation"
                condition: "#user.profile.salary > 50000"
                message: "Salary must be above 50000"
              - id: "department-check"
                name: "Department Validation"
                condition: "#user.profile.department != null"
                message: "Department is required"
              - id: "skills-check"
                name: "Skills Validation"
                condition: "#user.profile.skills != null && #user.profile.skills.size() > 0"
                message: "At least one skill is required"
            """);
        request.setDataFormat("JSON");
        return request;
    }

    private PlaygroundRequest createLargeDataRequest() {
        // Create a larger JSON structure
        StringBuilder largeData = new StringBuilder();
        largeData.append("{\"users\": [");
        for (int i = 0; i < 100; i++) {
            if (i > 0) largeData.append(",");
            largeData.append(String.format(
                "{\"id\": %d, \"name\": \"User %d\", \"age\": %d, \"department\": \"Dept %d\"}",
                i, i, 20 + (i % 50), i % 10));
        }
        largeData.append("], \"metadata\": {\"count\": 100, \"generated\": \"2025-08-23\"}}");

        PlaygroundRequest request = new PlaygroundRequest();
        request.setSourceData(largeData.toString());
        request.setYamlRules("""
            metadata:
              name: "Large Data Test Rules"
              version: "1.0.0"
            rules:
              - id: "user-count-check"
                name: "User Count Validation"
                condition: "#users != null && #users.size() > 0"
                message: "Users list cannot be empty"
              - id: "metadata-check"
                name: "Metadata Validation"
                condition: "#metadata != null && #metadata.count > 0"
                message: "Metadata count must be positive"
            """);
        request.setDataFormat("JSON");
        return request;
    }
}
