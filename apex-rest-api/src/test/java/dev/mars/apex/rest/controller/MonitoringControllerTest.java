package dev.mars.apex.rest.controller;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.rest.service.RuleEvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Unit tests for MonitoringController.
 * Tests controller logic using Spring Boot Test with MockBean.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Monitoring Controller Unit Tests")
public class MonitoringControllerTest {

    @Autowired
    private MonitoringController monitoringController;

    @MockBean
    private RulesService rulesService;

    @MockBean
    private RuleEvaluationService ruleEvaluationService;

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return healthy status")
        void shouldReturnHealthyStatus() {
            // Mock the basic functionality test to return true
            when(ruleEvaluationService.quickCheck(anyString(), anyMap())).thenReturn(true);

            // When
            ResponseEntity<Map<String, Object>> response = monitoringController.healthCheck();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("UP", response.getBody().get("status"));
            assertTrue(response.getBody().containsKey("timestamp"));
            assertTrue(response.getBody().containsKey("version"));
            assertTrue(response.getBody().containsKey("basicFunctionality"));
            assertTrue(response.getBody().containsKey("memory"));
            assertTrue(response.getBody().containsKey("system"));
            assertEquals("UP", response.getBody().get("basicFunctionality"));

            // Verify service was called for basic functionality test
            verify(ruleEvaluationService).quickCheck("#test == true", Map.of("test", true));
        }

        @Test
        @DisplayName("Should return unhealthy status when basic functionality fails")
        void shouldReturnUnhealthyStatusWhenBasicFunctionalityFails() {
            // Mock the basic functionality test to return false
            when(ruleEvaluationService.quickCheck(anyString(), anyMap())).thenReturn(false);

            // When
            ResponseEntity<Map<String, Object>> response = monitoringController.healthCheck();

            // Then
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("DOWN", response.getBody().get("status"));
            assertTrue(response.getBody().containsKey("basicFunctionality"));
            assertTrue(response.getBody().containsKey("memory"));
            assertEquals("DOWN", response.getBody().get("basicFunctionality"));

            // Verify service was called for basic functionality test
            verify(ruleEvaluationService).quickCheck("#test == true", Map.of("test", true));
        }
    }

    @Nested
    @DisplayName("Metrics Tests")
    class MetricsTests {

        @Test
        @DisplayName("Should return system metrics")
        void shouldReturnSystemMetrics() {
            // Mock the service response
            Map<String, Object> mockSystemInfo = new HashMap<>();
            mockSystemInfo.put("timestamp", java.time.Instant.now());
            when(ruleEvaluationService.getSystemInfo()).thenReturn(mockSystemInfo);

            // When
            ResponseEntity<Map<String, Object>> response = monitoringController.getSystemStats();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().containsKey("service"));
            assertTrue(response.getBody().containsKey("version"));
            assertTrue(response.getBody().containsKey("uptime"));
            assertTrue(response.getBody().containsKey("timestamp"));
            assertTrue(response.getBody().containsKey("jvm"));
            assertTrue(response.getBody().containsKey("garbageCollection"));
            assertEquals("Rules Engine REST API", response.getBody().get("service"));
            assertEquals("1.0.0", response.getBody().get("version"));

            // Verify service was called
            verify(ruleEvaluationService).getSystemInfo();
        }

        @Test
        @DisplayName("Should include JVM metrics")
        void shouldIncludeJvmMetrics() {
            // Mock the service response
            Map<String, Object> mockSystemInfo = new HashMap<>();
            mockSystemInfo.put("timestamp", java.time.Instant.now());
            when(ruleEvaluationService.getSystemInfo()).thenReturn(mockSystemInfo);

            // When
            ResponseEntity<Map<String, Object>> response = monitoringController.getSystemStats();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().containsKey("jvm"));

            @SuppressWarnings("unchecked")
            Map<String, Object> jvm = (Map<String, Object>) response.getBody().get("jvm");
            assertTrue(jvm.containsKey("totalMemory"));
            assertTrue(jvm.containsKey("freeMemory"));
            assertTrue(jvm.containsKey("usedMemory"));
            assertTrue(jvm.containsKey("maxMemory"));
            assertTrue(jvm.containsKey("availableProcessors"));
            assertTrue(jvm.containsKey("memoryUsagePercent"));

            // Verify service was called
            verify(ruleEvaluationService).getSystemInfo();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle exception in basic functionality test")
        void shouldHandleExceptionInBasicFunctionalityTest() {
            // Mock the basic functionality test to throw exception
            when(ruleEvaluationService.quickCheck(anyString(), anyMap()))
                .thenThrow(new RuntimeException("Service unavailable"));

            // When
            ResponseEntity<Map<String, Object>> response = monitoringController.healthCheck();

            // Then
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("DOWN", response.getBody().get("status"));
            assertEquals("DOWN", response.getBody().get("basicFunctionality"));

            // Verify service was called
            verify(ruleEvaluationService).quickCheck("#test == true", Map.of("test", true));
        }

        @Test
        @DisplayName("Should return performance metrics")
        void shouldReturnPerformanceMetrics() {
            // When
            ResponseEntity<Map<String, Object>> response = monitoringController.getPerformanceMetrics();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().containsKey("timestamp"));
            assertTrue(response.getBody().containsKey("service"));
            assertTrue(response.getBody().containsKey("ruleEvaluation"));
            assertTrue(response.getBody().containsKey("validation"));
            assertTrue(response.getBody().containsKey("system"));
            assertEquals("Rules Engine REST API", response.getBody().get("service"));
        }

        @Test
        @DisplayName("Should return readiness check")
        void shouldReturnReadinessCheck() {
            // Mock the basic functionality test to return true
            when(ruleEvaluationService.quickCheck(anyString(), anyMap())).thenReturn(true);

            // When
            ResponseEntity<Map<String, Object>> response = monitoringController.readinessCheck();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(true, response.getBody().get("ready"));
            assertTrue(response.getBody().containsKey("timestamp"));
            assertTrue(response.getBody().containsKey("service"));

            // Verify service was called
            verify(ruleEvaluationService).quickCheck("#test == true", Map.of("test", true));
        }

        @Test
        @DisplayName("Should return not ready when basic functionality fails")
        void shouldReturnNotReadyWhenBasicFunctionalityFails() {
            // Mock the basic functionality test to return false
            when(ruleEvaluationService.quickCheck(anyString(), anyMap())).thenReturn(false);

            // When
            ResponseEntity<Map<String, Object>> response = monitoringController.readinessCheck();

            // Then
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(false, response.getBody().get("ready"));

            // Verify service was called
            verify(ruleEvaluationService).quickCheck("#test == true", Map.of("test", true));
        }
    }


}
