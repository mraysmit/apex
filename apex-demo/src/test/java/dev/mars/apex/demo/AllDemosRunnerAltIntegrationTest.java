package dev.mars.apex.demo;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import dev.mars.apex.demo.runners.AllDemosRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for AllDemosRunnerAlt to verify actual demo execution.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
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
