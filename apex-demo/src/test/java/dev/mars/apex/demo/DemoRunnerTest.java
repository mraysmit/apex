package dev.mars.apex.demo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for DemoRunner to verify the entry point functionality.
 * 
 * These tests ensure that:
 * - All command line arguments work correctly
 * - Interactive mode functions properly
 * - Error handling is robust
 * - Help and about information is displayed correctly
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
class DemoRunnerTest {
    
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private InputStream originalIn;
    
    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalIn = System.in;
        System.setOut(new PrintStream(outputStream));
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }
    
    @Test
    @DisplayName("DemoRunner should display banner")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testBannerDisplay() {
        // Simulate quit command for interactive mode
        System.setIn(new ByteArrayInputStream("q\n".getBytes()));
        
        assertDoesNotThrow(() -> {
            DemoRunner.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Banner display",
            () -> assertTrue(output.contains("SpEL Rules Engine - Demo Suite"), 
                           "Should display main title"),
            () -> assertTrue(output.contains("╔") && output.contains("╗") && 
                           output.contains("║") && output.contains("╚") && output.contains("╝"), 
                           "Should display banner box characters"),
            () -> assertTrue(output.contains("Comprehensive demonstrations"), 
                           "Should display description")
        );
    }
    
    @Test
    @DisplayName("DemoRunner should handle quickstart command")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testQuickStartCommand() {
        assertDoesNotThrow(() -> {
            DemoRunner.main(new String[]{"quickstart"});
        });
        
        String output = outputStream.toString();
        
        assertAll("QuickStart command",
            () -> assertTrue(output.contains("non-interactive mode: quickstart"), 
                           "Should show non-interactive mode"),
            () -> assertTrue(output.contains("Quick Start (5 Minutes)"), 
                           "Should run QuickStart demo")
        );
    }
    
    @Test
    @DisplayName("DemoRunner should handle layered command")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testLayeredCommand() {
        assertDoesNotThrow(() -> {
            DemoRunner.main(new String[]{"layered"});
        });
        
        String output = outputStream.toString();
        
        assertAll("Layered command",
            () -> assertTrue(output.contains("non-interactive mode: layered"), 
                           "Should show non-interactive mode"),
            () -> assertTrue(output.contains("Three-Layer API Design"), 
                           "Should run Layered API demo")
        );
    }
    
    @Test
    @DisplayName("DemoRunner should handle dataset command")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testDatasetCommand() {
        assertDoesNotThrow(() -> {
            DemoRunner.main(new String[]{"dataset"});
        });
        
        String output = outputStream.toString();
        
        assertAll("Dataset command",
            () -> assertTrue(output.contains("non-interactive mode: dataset"), 
                           "Should show non-interactive mode"),
            () -> assertTrue(output.contains("YAML Dataset Enrichment"), 
                           "Should run YAML Dataset demo")
        );
    }
    
    @Test
    @DisplayName("DemoRunner should handle financial command")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testFinancialCommand() {
        assertDoesNotThrow(() -> {
            DemoRunner.main(new String[]{"financial"});
        });
        
        String output = outputStream.toString();
        
        assertAll("Financial command",
            () -> assertTrue(output.contains("non-interactive mode: financial"), 
                           "Should show non-interactive mode"),
            () -> assertTrue(output.contains("Financial Services Demo"), 
                           "Should run Financial Services demo")
        );
    }
    
    @Test
    @DisplayName("DemoRunner should handle performance command")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testPerformanceCommand() {
        assertDoesNotThrow(() -> {
            DemoRunner.main(new String[]{"performance"});
        });
        
        String output = outputStream.toString();
        
        assertAll("Performance command",
            () -> assertTrue(output.contains("non-interactive mode: performance"), 
                           "Should show non-interactive mode"),
            () -> assertTrue(output.contains("Performance Monitoring Demo"), 
                           "Should run Performance demo")
        );
    }
    
    @Test
    @DisplayName("DemoRunner should handle help command")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testHelpCommand() {
        assertDoesNotThrow(() -> {
            DemoRunner.main(new String[]{"help"});
        });
        
        String output = outputStream.toString();
        
        assertAll("Help command",
            () -> assertTrue(output.contains("Command Line Help"), 
                           "Should display help title"),
            () -> assertTrue(output.contains("Usage:"), 
                           "Should show usage information"),
            () -> assertTrue(output.contains("Commands:"), 
                           "Should list available commands"),
            () -> assertTrue(output.contains("Examples:"), 
                           "Should provide examples")
        );
    }
    
    @Test
    @DisplayName("DemoRunner should handle unknown command gracefully")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testUnknownCommand() {
        assertDoesNotThrow(() -> {
            DemoRunner.main(new String[]{"unknown"});
        });
        
        String output = outputStream.toString();
        
        assertAll("Unknown command",
            () -> assertTrue(output.contains("Unknown command: unknown"), 
                           "Should show unknown command error"),
            () -> assertTrue(output.contains("Command Line Help") || output.contains("Usage:"), 
                           "Should show help after unknown command")
        );
    }
    
    @Test
    @DisplayName("DemoRunner should display interactive menu")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testInteractiveMenu() {
        // Simulate quit command
        System.setIn(new ByteArrayInputStream("q\n".getBytes()));
        
        assertDoesNotThrow(() -> {
            DemoRunner.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Interactive menu",
            () -> assertTrue(output.contains("Available Demonstrations:"), 
                           "Should show menu header"),
            () -> assertTrue(output.contains("1. QuickStart Demo"), 
                           "Should show QuickStart option"),
            () -> assertTrue(output.contains("2. Layered API Demo"), 
                           "Should show Layered API option"),
            () -> assertTrue(output.contains("3. YAML Dataset Demo"), 
                           "Should show YAML Dataset option"),
            () -> assertTrue(output.contains("4. Financial Services"), 
                           "Should show Financial Services option"),
            () -> assertTrue(output.contains("5. Performance Demo"), 
                           "Should show Performance option"),
            () -> assertTrue(output.contains("6. Run All Demos"), 
                           "Should show Run All option"),
            () -> assertTrue(output.contains("7. About"), 
                           "Should show About option")
        );
    }
    
    @Test
    @DisplayName("DemoRunner should handle interactive about command")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testInteractiveAbout() {
        // Simulate about command then quit
        System.setIn(new ByteArrayInputStream("7\nq\n".getBytes()));
        
        assertDoesNotThrow(() -> {
            DemoRunner.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Interactive about",
            () -> assertTrue(output.contains("About SpEL Rules Engine"), 
                           "Should show about title"),
            () -> assertTrue(output.contains("Key Features:"), 
                           "Should show key features"),
            () -> assertTrue(output.contains("Three-Layer API Design"), 
                           "Should mention API design"),
            () -> assertTrue(output.contains("YAML Dataset Enrichment"), 
                           "Should mention dataset enrichment"),
            () -> assertTrue(output.contains("Enterprise Performance Monitoring"), 
                           "Should mention performance monitoring")
        );
    }
    
    @Test
    @DisplayName("DemoRunner should handle interactive quit gracefully")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testInteractiveQuit() {
        // Test various quit commands
        String[] quitCommands = {"q", "quit", "exit"};
        
        for (String quitCommand : quitCommands) {
            outputStream.reset();
            System.setIn(new ByteArrayInputStream((quitCommand + "\n").getBytes()));
            
            assertDoesNotThrow(() -> {
                DemoRunner.main(new String[]{});
            }, "Should handle quit command: " + quitCommand);
            
            String output = outputStream.toString();
            assertTrue(output.contains("Thank you for exploring"), 
                      "Should show thank you message for: " + quitCommand);
        }
    }
    
    @Test
    @DisplayName("DemoRunner should handle invalid interactive choices")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testInvalidInteractiveChoice() {
        // Simulate invalid choice, press enter to continue, then quit
        System.setIn(new ByteArrayInputStream("99\n\nq\n".getBytes()));
        
        assertDoesNotThrow(() -> {
            DemoRunner.main(new String[]{});
        });
        
        String output = outputStream.toString();
        assertTrue(output.contains("Invalid choice"), 
                  "Should show invalid choice message");
    }
    
    @Test
    @DisplayName("DemoRunner should handle command aliases")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testCommandAliases() {
        // Test some command aliases
        String[][] aliases = {
            {"quick", "Quick Start"},
            {"api", "Three-Layer API"},
            {"yaml", "YAML Dataset"},
            {"finance", "Financial Services"},
            {"perf", "Performance"}
        };
        
        for (String[] alias : aliases) {
            outputStream.reset();
            
            assertDoesNotThrow(() -> {
                DemoRunner.main(new String[]{alias[0]});
            }, "Should handle alias: " + alias[0]);
            
            String output = outputStream.toString();
            assertTrue(output.contains("non-interactive mode: " + alias[0]), 
                      "Should show non-interactive mode for alias: " + alias[0]);
        }
    }
    
    @Test
    @DisplayName("DemoRunner should produce substantial output")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testOutputSubstance() {
        System.setIn(new ByteArrayInputStream("q\n".getBytes()));
        
        DemoRunner.main(new String[]{});
        String output = outputStream.toString();
        
        assertAll("Output substance",
            () -> assertTrue(output.length() > 500, 
                           "Should produce substantial output (>500 chars), got: " + output.length()),
            () -> assertTrue(output.split("\n").length > 10, 
                           "Should have multiple lines of output")
        );
    }
}
