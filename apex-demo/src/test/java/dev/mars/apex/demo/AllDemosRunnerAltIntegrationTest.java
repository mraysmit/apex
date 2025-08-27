package dev.mars.apex.demo;

import dev.mars.apex.demo.runners.AllDemosRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for AllDemosRunnerAlt to verify actual demo execution.
 */
public class AllDemosRunnerAltIntegrationTest {
    
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
    public void testRunQuickStartDemo() {
        AllDemosRunner runner = new AllDemosRunner();
        runner.discoverDemos();
        
        // Find QuickStartDemo
        AllDemosRunner.DemoInfo quickStartDemo = runner.discoveredDemos.stream()
                .filter(demo -> demo.getClassName().equals("QuickStartDemo"))
                .findFirst()
                .orElse(null);

        assertNotNull(quickStartDemo, "QuickStartDemo should be discovered");
        
        // Run the demo
        assertDoesNotThrow(() -> {
            runner.runSingleDemo(quickStartDemo);
        }, "QuickStartDemo should run without exceptions");
        
        // Verify some output was produced
        String output = outContent.toString();
        assertTrue(output.length() > 0, "Demo should produce some output");
        
        // Restore output to show results
        System.setOut(originalOut);
        System.out.println("QuickStartDemoB output length: " + output.length() + " characters");
        System.out.println("Demo completed successfully!");
    }
    
    @Test
    public void testRunAllDemosFromCorePackage() {
        AllDemosRunner runner = new AllDemosRunner();
        
        // Test running demos from examples package
        assertDoesNotThrow(() -> {
            runner.runDemosFromPackage("examples");
        }, "Running examples demos should not throw exceptions");

        String output = outContent.toString();
        assertTrue(output.contains("demos from package: examples"), "Should show package filtering message");

        // Restore output to show results
        System.setOut(originalOut);
        System.out.println("Examples demos executed successfully!");
        System.out.println("Output contained " + output.split("\n").length + " lines");
    }
    
    @Test
    public void testMainMethodRunsAllDemos() {
        // This test runs all demos - might take a while
        assertDoesNotThrow(() -> {
            AllDemosRunner.main(new String[0]);
        }, "Running all demos should not throw exceptions");
        
        String output = outContent.toString();
        assertTrue(output.contains("SpEL Rules Engine"), "Should show the banner");
        assertTrue(output.contains("EXECUTION SUMMARY"), "Should show execution summary");
        
        // Restore output to show results
        System.setOut(originalOut);
        System.out.println("All demos executed successfully!");
        System.out.println("Total output: " + output.split("\n").length + " lines");
    }
}
