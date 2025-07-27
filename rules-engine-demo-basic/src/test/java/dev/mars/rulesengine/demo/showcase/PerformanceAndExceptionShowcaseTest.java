package dev.mars.rulesengine.demo.showcase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the PerformanceAndExceptionShowcase.
 * Ensures that the performance monitoring and exception handling
 * demonstration runs correctly and showcases all expected features.
 */
class PerformanceAndExceptionShowcaseTest {
    
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(outputStream));
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Test
    @DisplayName("Performance and Exception Showcase should run successfully")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testPerformanceAndExceptionShowcase() {
        assertDoesNotThrow(() -> {
            PerformanceAndExceptionShowcase.main(new String[]{});
        }, "Performance and exception showcase should not throw any exceptions");
        
        String output = outputStream.toString();
        
        assertAll("Performance showcase output validation",
            () -> assertTrue(output.contains("PERFORMANCE & EXCEPTION HANDLING SHOWCASE"), 
                           "Should display showcase title"),
            () -> assertTrue(output.contains("Basic Performance Monitoring"), 
                           "Should demonstrate basic performance monitoring"),
            () -> assertTrue(output.contains("Concurrent Rule Execution"), 
                           "Should demonstrate concurrent execution"),
            () -> assertTrue(output.contains("Exception Handling"), 
                           "Should demonstrate exception handling"),
            () -> assertTrue(output.contains("Advanced Monitoring"), 
                           "Should demonstrate advanced monitoring"),
            () -> assertFalse(output.toLowerCase().contains("error running"), 
                            "Should not contain execution errors")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate basic performance monitoring")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testBasicPerformanceMonitoring() {
        assertDoesNotThrow(() -> {
            PerformanceAndExceptionShowcase.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Basic performance monitoring validation",
            () -> assertTrue(output.contains("Basic Performance Monitoring"), 
                           "Should demonstrate basic performance monitoring"),
            () -> assertTrue(output.contains("evaluation") || output.contains("ms") || 
                           output.contains("time"), 
                           "Should show performance metrics"),
            () -> assertTrue(output.contains("✓") || output.contains("completed"), 
                           "Should show successful evaluations"),
            () -> assertTrue(output.contains("rule") || output.contains("Rule"), 
                           "Should demonstrate rule evaluation")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate concurrent rule execution")
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void testConcurrentRuleExecution() {
        assertDoesNotThrow(() -> {
            PerformanceAndExceptionShowcase.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Concurrent execution validation",
            () -> assertTrue(output.contains("Concurrent Rule Execution"), 
                           "Should demonstrate concurrent execution"),
            () -> assertTrue(output.contains("thread") || output.contains("concurrent") || 
                           output.contains("parallel"), 
                           "Should show concurrent processing"),
            () -> assertTrue(output.contains("completed") || output.contains("finished"), 
                           "Should show completion of concurrent tasks"),
            () -> assertTrue(output.contains("ms") || output.contains("time"), 
                           "Should show timing information")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate performance optimization")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testPerformanceOptimization() {
        assertDoesNotThrow(() -> {
            PerformanceAndExceptionShowcase.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Performance optimization validation",
            () -> assertTrue(output.contains("Performance Optimization") || 
                           output.contains("optimization"), 
                           "Should demonstrate performance optimization"),
            () -> assertTrue(output.contains("caching") || output.contains("cache") || 
                           output.contains("optimization"), 
                           "Should show optimization techniques"),
            () -> assertTrue(output.contains("evaluation") || output.contains("ms"), 
                           "Should show performance measurements")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate exception handling")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testExceptionHandling() {
        assertDoesNotThrow(() -> {
            PerformanceAndExceptionShowcase.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Exception handling validation",
            () -> assertTrue(output.contains("Exception Handling"), 
                           "Should demonstrate exception handling"),
            () -> assertTrue(output.contains("Caught exception") || 
                           output.contains("exception") || 
                           output.contains("error"), 
                           "Should show exception handling"),
            () -> assertTrue(output.contains("✓") || output.contains("handled"), 
                           "Should show successful exception handling"),
            () -> assertTrue(output.contains("ms") || output.contains("time"), 
                           "Should show timing for error handling")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate error recovery")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testErrorRecovery() {
        assertDoesNotThrow(() -> {
            PerformanceAndExceptionShowcase.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Error recovery validation",
            () -> assertTrue(output.contains("Error Recovery") || 
                           output.contains("recovery"), 
                           "Should demonstrate error recovery"),
            () -> assertTrue(output.contains("success") || output.contains("✓") || 
                           output.contains("passed"), 
                           "Should show successful operations"),
            () -> assertTrue(output.contains("error") || output.contains("✗") || 
                           output.contains("exception"), 
                           "Should show error scenarios")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate advanced monitoring features")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testAdvancedMonitoring() {
        assertDoesNotThrow(() -> {
            PerformanceAndExceptionShowcase.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Advanced monitoring validation",
            () -> assertTrue(output.contains("Advanced Monitoring"), 
                           "Should demonstrate advanced monitoring"),
            () -> assertTrue(output.contains("trend") || output.contains("analysis") || 
                           output.contains("metrics"), 
                           "Should show advanced monitoring features"),
            () -> assertTrue(output.contains("batch") || output.contains("Batch"), 
                           "Should show batch processing monitoring")
        );
    }
    
    @Test
    @DisplayName("Demo should simulate monitoring dashboard")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testMonitoringDashboard() {
        assertDoesNotThrow(() -> {
            PerformanceAndExceptionShowcase.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Monitoring dashboard validation",
            () -> assertTrue(output.contains("Monitoring Dashboard") || 
                           output.contains("dashboard"), 
                           "Should simulate monitoring dashboard"),
            () -> assertTrue(output.contains("Total") || output.contains("Average") || 
                           output.contains("statistics"), 
                           "Should show dashboard statistics"),
            () -> assertTrue(output.contains("evaluation") || output.contains("rule"), 
                           "Should show rule evaluation statistics")
        );
    }
    
    @Test
    @DisplayName("Demo should handle cleanup properly")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testCleanupHandling() {
        assertDoesNotThrow(() -> {
            PerformanceAndExceptionShowcase.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        // The demo should complete without hanging or leaving resources open
        assertAll("Cleanup validation",
            () -> assertTrue(output.length() > 1000, 
                           "Should produce substantial output indicating full execution"),
            () -> assertFalse(output.toLowerCase().contains("failed to cleanup"), 
                            "Should not indicate cleanup failures"),
            () -> assertTrue(output.contains("monitoring") || output.contains("performance"), 
                           "Should demonstrate monitoring capabilities")
        );
    }
    
    @Test
    @DisplayName("Demo should show performance metrics and timing")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testPerformanceMetrics() {
        assertDoesNotThrow(() -> {
            PerformanceAndExceptionShowcase.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Performance metrics validation",
            () -> assertTrue(output.contains("ms") || output.contains("milliseconds") || 
                           output.contains("time"), 
                           "Should show timing measurements"),
            () -> assertTrue(output.contains("evaluation") || output.contains("execution"), 
                           "Should show evaluation metrics"),
            () -> assertTrue(output.contains("✓") || output.contains("completed") || 
                           output.contains("finished"), 
                           "Should show successful completions"),
            () -> assertTrue(output.matches(".*\\d+.*ms.*") || 
                           output.matches(".*\\d+.*milliseconds.*"), 
                           "Should contain actual timing numbers")
        );
    }
}
