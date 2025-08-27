package dev.mars.apex.demo;

import org.junit.jupiter.api.Test;

/**
 * Manual test to verify AllDemosRunnerAlt actually works by running it directly.
 */
public class AllDemosRunnerAltManualTest {
    
    @Test
    public void manualTestListCommand() {
        System.out.println("=== MANUAL TEST: AllDemosRunnerAlt --list ===");
        
        // Run the AllDemosRunnerAlt directly
        AllDemosRunner.main(new String[]{"--list"});
        
        System.out.println("=== END MANUAL TEST ===");
    }
    
    @Test
    public void manualTestPackageCommand() {
        System.out.println("=== MANUAL TEST: AllDemosRunnerAlt --package core ===");
        
        // Run the AllDemosRunnerAlt directly
        AllDemosRunner.main(new String[]{"--package", "core"});
        
        System.out.println("=== END MANUAL TEST ===");
    }
    
    @Test
    public void manualTestDiscovery() {
        System.out.println("=== MANUAL TEST: Direct Discovery ===");
        
        AllDemosRunner runner = new AllDemosRunner();
        runner.discoverDemos();
        
        System.out.println("Discovered " + runner.discoveredDemos.size() + " demos:");
        for (AllDemosRunner.DemoInfo demo : runner.discoveredDemos) {
            System.out.println("  - " + demo.getFullName() + " (" + demo.getExecutionType() + ")");
        }
        
        System.out.println("=== END MANUAL TEST ===");
    }
}
