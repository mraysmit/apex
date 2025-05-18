package com.rulesengine.demo;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import com.rulesengine.demo.integration.SpelRulesEngineDemo;

public class SpelRuleGroupsTest {

    @Test
    public void testRuleGroups() {
        // Capture System.out to see the output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            // Test rule groups functionality
            SpelRulesEngineDemo.demoRuleGroupsDemo();

            // Print the output with DEBUG_LOG prefix so it appears in the test results
            String output = outContent.toString();
            String[] lines = output.split("\\r?\\n");
            for (String line : lines) {
                if (line.contains("=== Demonstrating Rule Groups") || 
                    line.contains("Processing scenario for:") ||
                    line.contains("rule triggered:") ||
                    line.contains("Result:")) {
                    System.out.println("[DEBUG_LOG] " + line);
                }
            }
        } finally {
            // Restore System.out
            System.setOut(originalOut);
        }
    }
}