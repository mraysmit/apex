package dev.mars.rulesengine.demo;

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
 * Comprehensive test suite for the ComprehensiveRulesEngineDemo.
 * These tests ensure that all demo functionality runs without errors
 * and produces expected output without requiring manual validation.
 */
class ComprehensiveRulesEngineDemoTest {
    
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    @BeforeEach
    void setUp() {
        // Capture system output for testing
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(outputStream));
    }
    
    @AfterEach
    void tearDown() {
        // Restore original system output
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Test
    @DisplayName("Financial Demo should run successfully without errors")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testFinancialDemo() {
        // Run the financial demo in non-interactive mode
        assertDoesNotThrow(() -> {
            ComprehensiveRulesEngineDemo.main(new String[]{"financial"});
        }, "Financial demo should not throw any exceptions");
        
        String output = outputStream.toString();
        
        // Verify key components of the financial demo ran
        assertAll("Financial demo output validation",
            () -> assertTrue(output.contains("Running in non-interactive mode: financial"), 
                           "Should indicate non-interactive mode"),
            () -> assertTrue(output.contains("COMMODITY SWAP VALIDATION"), 
                           "Should contain commodity swap validation content"),
            () -> assertTrue(output.contains("LAYER 1: ULTRA-SIMPLE API"), 
                           "Should demonstrate ultra-simple API"),
            () -> assertTrue(output.contains("LAYER 2: TEMPLATE-BASED RULES"), 
                           "Should demonstrate template-based rules"),
            () -> assertTrue(output.contains("LAYER 3: ADVANCED CONFIGURATION"), 
                           "Should demonstrate advanced configuration"),
            () -> assertFalse(output.contains("Error"), 
                            "Should not contain error messages"),
            () -> assertFalse(output.contains("Exception"), 
                            "Should not contain exception messages")
        );
    }
    
    @Test
    @DisplayName("Simplified API Demo should run successfully without errors")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testSimplifiedAPIDemo() {
        assertDoesNotThrow(() -> {
            ComprehensiveRulesEngineDemo.main(new String[]{"simplified"});
        }, "Simplified API demo should not throw any exceptions");
        
        String output = outputStream.toString();
        
        assertAll("Simplified API demo output validation",
            () -> assertTrue(output.contains("Running in non-interactive mode: simplified"), 
                           "Should indicate non-interactive mode"),
            () -> assertTrue(output.contains("SIMPLIFIED APIS DEMONSTRATION"), 
                           "Should contain simplified APIs content"),
            () -> assertTrue(output.contains("Ultra-Simple API"), 
                           "Should demonstrate ultra-simple API"),
            () -> assertTrue(output.contains("Template-Based Rules"), 
                           "Should demonstrate template-based rules"),
            () -> assertTrue(output.contains("Advanced Configuration"), 
                           "Should demonstrate advanced configuration"),
            () -> assertFalse(output.contains("Error"), 
                            "Should not contain error messages")
        );
    }
    
    @Test
    @DisplayName("Performance Showcase should run successfully without errors")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testPerformanceShowcase() {
        assertDoesNotThrow(() -> {
            ComprehensiveRulesEngineDemo.main(new String[]{"performance"});
        }, "Performance showcase should not throw any exceptions");
        
        String output = outputStream.toString();
        
        assertAll("Performance showcase output validation",
            () -> assertTrue(output.contains("Running in non-interactive mode: performance"), 
                           "Should indicate non-interactive mode"),
            () -> assertTrue(output.contains("PERFORMANCE & EXCEPTION HANDLING"), 
                           "Should contain performance content"),
            () -> assertTrue(output.contains("Basic Performance Monitoring"), 
                           "Should demonstrate basic performance monitoring"),
            () -> assertTrue(output.contains("Concurrent Rule Execution"), 
                           "Should demonstrate concurrent execution"),
            () -> assertTrue(output.contains("Exception Handling"), 
                           "Should demonstrate exception handling"),
            () -> assertFalse(output.toLowerCase().contains("error running"), 
                            "Should not contain execution errors")
        );
    }
    
    @Test
    @DisplayName("Static Data Demo should run successfully without errors")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testStaticDataDemo() {
        assertDoesNotThrow(() -> {
            ComprehensiveRulesEngineDemo.main(new String[]{"static"});
        }, "Static data demo should not throw any exceptions");
        
        String output = outputStream.toString();
        
        assertAll("Static data demo output validation",
            () -> assertTrue(output.contains("Running in non-interactive mode: static"), 
                           "Should indicate non-interactive mode"),
            () -> assertTrue(output.contains("Static Data Validation"), 
                           "Should contain static data content"),
            () -> assertFalse(output.contains("Error"), 
                            "Should not contain error messages")
        );
    }
    
    @Test
    @DisplayName("Quick Start Guide should run successfully without errors")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testQuickStartGuide() {
        assertDoesNotThrow(() -> {
            ComprehensiveRulesEngineDemo.main(new String[]{"quickstart"});
        }, "Quick start guide should not throw any exceptions");
        
        String output = outputStream.toString();
        
        assertAll("Quick start guide output validation",
            () -> assertTrue(output.contains("Running in non-interactive mode: quickstart"), 
                           "Should indicate non-interactive mode"),
            () -> assertTrue(output.contains("Quick Start Guide"), 
                           "Should contain quick start content"),
            () -> assertFalse(output.contains("Error"), 
                            "Should not contain error messages")
        );
    }
    
    @Test
    @DisplayName("Complete Demo should run all components successfully")
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void testCompleteDemo() {
        assertDoesNotThrow(() -> {
            ComprehensiveRulesEngineDemo.main(new String[]{"complete"});
        }, "Complete demo should not throw any exceptions");
        
        String output = outputStream.toString();
        
        assertAll("Complete demo output validation",
            () -> assertTrue(output.contains("Running in non-interactive mode: complete"), 
                           "Should indicate non-interactive mode"),
            () -> assertTrue(output.contains("Complete demonstration finished"), 
                           "Should indicate successful completion"),
            () -> assertTrue(output.contains("COMMODITY SWAP VALIDATION"), 
                           "Should include financial demo"),
            () -> assertTrue(output.contains("SIMPLIFIED APIS DEMONSTRATION"), 
                           "Should include simplified API demo"),
            () -> assertTrue(output.contains("PERFORMANCE & EXCEPTION HANDLING"), 
                           "Should include performance showcase"),
            () -> assertFalse(output.toLowerCase().contains("error running"), 
                            "Should not contain execution errors")
        );
    }
    
    @Test
    @DisplayName("Invalid demo type should show available options")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testInvalidDemoType() {
        assertDoesNotThrow(() -> {
            ComprehensiveRulesEngineDemo.main(new String[]{"invalid"});
        }, "Invalid demo type should not throw exceptions");
        
        String output = outputStream.toString();
        
        assertAll("Invalid demo type output validation",
            () -> assertTrue(output.contains("Unknown demo type: invalid"), 
                           "Should indicate unknown demo type"),
            () -> assertTrue(output.contains("Available types:"), 
                           "Should show available demo types"),
            () -> assertTrue(output.contains("financial"), 
                           "Should list financial as available type"),
            () -> assertTrue(output.contains("simplified"), 
                           "Should list simplified as available type"),
            () -> assertTrue(output.contains("performance"), 
                           "Should list performance as available type")
        );
    }
    
    @Test
    @DisplayName("Demo should handle no arguments gracefully")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testNoArguments() {
        // This test verifies that the demo can start in interactive mode
        // but we'll interrupt it quickly since we can't provide interactive input
        assertDoesNotThrow(() -> {
            // Create a thread to run the demo
            Thread demoThread = new Thread(() -> {
                try {
                    ComprehensiveRulesEngineDemo.main(new String[]{});
                } catch (Exception e) {
                    // Expected when we interrupt the thread
                }
            });
            
            demoThread.start();
            
            // Give it a moment to start and display the banner
            Thread.sleep(1000);
            
            // Interrupt the thread since we can't provide interactive input
            demoThread.interrupt();
            
            // Wait for thread to finish
            demoThread.join(2000);
            
        }, "Demo should start without throwing exceptions");
        
        String output = outputStream.toString();
        
        // Verify the banner and menu are displayed
        assertAll("Interactive mode startup validation",
            () -> assertTrue(output.contains("SpEL Rules Engine"), 
                           "Should display the banner"),
            () -> assertTrue(output.contains("Financial Instrument Validation") || 
                           output.contains("Enter your choice"), 
                           "Should display menu or start interactive mode")
        );
    }
}
