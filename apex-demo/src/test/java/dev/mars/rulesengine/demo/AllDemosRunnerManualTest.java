package dev.mars.apex.demo;

import org.junit.jupiter.api.Test;

/**
 * Manual test to verify AllDemosRunner actually works by running it directly.
 */
public class AllDemosRunnerManualTest {
    
    @Test
    public void manualTestListCommand() {
        System.out.println("=== MANUAL TEST: AllDemosRunner --list ===");
        
        // Run the AllDemosRunner directly
        AllDemosRunner.main(new String[]{"--list"});
        
        System.out.println("=== END MANUAL TEST ===");
    }
    
    @Test
    public void manualTestPackageCommand() {
        System.out.println("=== MANUAL TEST: AllDemosRunner --package core ===");
        
        // Run the AllDemosRunner directly
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
