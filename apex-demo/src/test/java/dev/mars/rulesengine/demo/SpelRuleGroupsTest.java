package dev.mars.apex.demo;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import dev.mars.apex.demo.examples.SpelRulesEngineDemo;

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

/**
 * Implementation of SpelRuleGroupsTest functionality.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
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
