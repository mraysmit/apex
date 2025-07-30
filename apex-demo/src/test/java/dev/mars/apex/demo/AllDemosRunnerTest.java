package dev.mars.apex.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AllDemosRunner to verify demo discovery and execution.
 */
public class AllDemosRunnerTest {
    
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
    public void testDemoDiscovery() {
        AllDemosRunner runner = new AllDemosRunner();
        runner.discoverDemos();
        
        // Should discover some demos
        assertTrue(runner.discoveredDemos.size() > 0, "Should discover at least some demos");
        
        // Print discovered demos for verification
        System.setOut(originalOut); // Restore output for this print
        System.out.println("Discovered " + runner.discoveredDemos.size() + " demos:");
        for (AllDemosRunner.DemoInfo demo : runner.discoveredDemos) {
            System.out.println("  - " + demo.getFullName() + " (" + demo.getExecutionType() + ")");
        }
    }
    
    @Test
    public void testListDemos() {
        AllDemosRunner runner = new AllDemosRunner();
        runner.listAllDemos();
        
        String output = outContent.toString();
        assertTrue(output.contains("Discovered"), "Should show discovered demos");
        assertTrue(output.contains("runnable demos"), "Should mention runnable demos");
    }
    
    @Test
    public void testRunSingleDemo() {
        AllDemosRunner runner = new AllDemosRunner();
        runner.discoverDemos();
        
        if (!runner.discoveredDemos.isEmpty()) {
            // Try to run the first demo
            AllDemosRunner.DemoInfo firstDemo = runner.discoveredDemos.get(0);
            
            System.setOut(originalOut); // Restore output for this test
            System.out.println("Testing execution of: " + firstDemo.getFullName());
            
            // This should not throw an exception
            assertDoesNotThrow(() -> {
                runner.runSingleDemo(firstDemo);
            }, "Demo execution should not throw exceptions");
        }
    }
    
    @Test
    public void testMainMethodWithListArg() {
        // Test the main method with --list argument
        assertDoesNotThrow(() -> {
            AllDemosRunner.main(new String[]{"--list"});
        }, "Main method with --list should not throw exceptions");
        
        String output = outContent.toString();
        assertTrue(output.contains("SpEL Rules Engine"), "Should show the banner");
    }
}
