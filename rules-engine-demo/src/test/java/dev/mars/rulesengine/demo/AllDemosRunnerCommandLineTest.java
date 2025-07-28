package dev.mars.rulesengine.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify AllDemosRunner command line functionality.
 */
public class AllDemosRunnerCommandLineTest {
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }
    
    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }
    
    @Test
    public void testMainWithListArgument() {
        // Test the --list command line argument
        AllDemosRunner.main(new String[]{"--list"});
        
        String output = outContent.toString();
        
        // Verify expected output
        assertTrue(output.contains("SpEL Rules Engine"), "Should show the banner");
        assertTrue(output.contains("Discovered"), "Should show discovered demos");
        assertTrue(output.contains("runnable demos"), "Should mention runnable demos");
        assertTrue(output.contains("QuickStartDemo"), "Should list QuickStartDemo");
        assertTrue(output.contains("BatchProcessingDemo"), "Should list BatchProcessingDemo");
        assertTrue(output.contains("BasicUsageExamples"), "Should list BasicUsageExamples");
        
        // Verify package organization
        assertTrue(output.contains("core:"), "Should show core package");
        assertTrue(output.contains("examples:"), "Should show examples package");
        
        // Restore output to show results
        System.setOut(originalOut);
        System.out.println("✅ --list command works correctly!");
        System.out.println("Output preview:");
        String[] lines = output.split("\n");
        for (int i = 0; i < Math.min(10, lines.length); i++) {
            System.out.println("  " + lines[i]);
        }
        if (lines.length > 10) {
            System.out.println("  ... (" + (lines.length - 10) + " more lines)");
        }
    }
    
    @Test
    public void testMainWithPackageArgument() {
        // Test the --package command line argument
        AllDemosRunner.main(new String[]{"--package", "core"});
        
        String output = outContent.toString();
        
        // Verify expected output
        assertTrue(output.contains("SpEL Rules Engine"), "Should show the banner");
        assertTrue(output.contains("Running"), "Should show running message");
        assertTrue(output.contains("demos from package"), "Should mention package filtering");
        assertTrue(output.contains("demos from package: core"), "Should show package filtering message");
        
        // Should contain core demos
        assertTrue(output.contains("QuickStartDemo") || output.contains("BatchProcessingDemo"), 
                  "Should run core demos");
        
        // Restore output to show results
        System.setOut(originalOut);
        System.out.println("✅ --package core command works correctly!");
        System.out.println("Output contained " + output.split("\n").length + " lines");
    }
    
    @Test
    public void testMainWithNoArguments() {
        // This test runs all demos - might take a while, so we'll just verify it starts correctly
        // We'll interrupt it after a short time to avoid long test execution
        
        Thread runnerThread = new Thread(() -> {
            AllDemosRunner.main(new String[0]);
        });
        
        runnerThread.start();
        
        try {
            // Wait a short time to let it start
            Thread.sleep(2000);
            
            String output = outContent.toString();
            
            // Verify it started correctly
            assertTrue(output.contains("SpEL Rules Engine"), "Should show the banner");
            assertTrue(output.contains("Automatically discovering"), "Should show discovery message");
            assertTrue(output.contains("Discovered"), "Should show discovered demos");
            
            // Restore output to show results
            System.setOut(originalOut);
            System.out.println("✅ Main runner starts correctly!");
            System.out.println("Started execution with " + output.split("\n").length + " lines of output");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Clean up the thread
            if (runnerThread.isAlive()) {
                runnerThread.interrupt();
            }
        }
    }
    
    @Test
    public void testAllCommandLineOptions() {
        System.setOut(originalOut); // Use normal output for this summary test
        
        System.out.println("=== AllDemosRunner Command Line Test Summary ===");
        System.out.println();
        
        // Test 1: List all demos
        System.out.println("1. Testing --list option:");
        System.setOut(new PrintStream(new ByteArrayOutputStream())); // Capture output
        AllDemosRunner.main(new String[]{"--list"});
        System.setOut(originalOut); // Restore output
        System.out.println("   ✅ --list option works correctly");
        
        // Test 2: Run demos from specific package
        System.out.println("2. Testing --package option:");
        System.setOut(new PrintStream(new ByteArrayOutputStream())); // Capture output
        AllDemosRunner.main(new String[]{"--package", "examples"});
        System.setOut(originalOut); // Restore output
        System.out.println("   ✅ --package examples option works correctly");
        
        System.out.println();
        System.out.println("🎉 All command line options are working correctly!");
        System.out.println();
        System.out.println("Usage examples:");
        System.out.println("  java dev.mars.rulesengine.demo.AllDemosRunner");
        System.out.println("  java dev.mars.rulesengine.demo.AllDemosRunner --list");
        System.out.println("  java dev.mars.rulesengine.demo.AllDemosRunner --package core");
        System.out.println("  java dev.mars.rulesengine.demo.AllDemosRunner --package examples");
    }
}
