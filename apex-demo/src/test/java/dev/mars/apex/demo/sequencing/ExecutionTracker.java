package dev.mars.apex.demo.sequencing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Utility class to track execution order of enrichments and rules during testing.
 * 
 * This class is used to DEFINITIVELY PROVE that APEX respects YAML document order.
 * 
 * Usage in YAML:
 * <pre>
 * enrichments:
 *   - id: "my-enrichment"
 *     type: "calculation-enrichment"
 *     expression: "T(dev.mars.apex.demo.sequencing.ExecutionTracker).record('my-enrichment'); 'result'"
 * </pre>
 * 
 * Usage in tests:
 * <pre>
 * ExecutionTracker.clear();
 * // ... execute APEX rules engine ...
 * List&lt;String&gt; actualOrder = ExecutionTracker.getExecutionLog();
 * List&lt;String&gt; expectedOrder = List.of("e1", "e2", "e3");
 * assertEquals(expectedOrder, actualOrder, "Execution order must match YAML document order");
 * </pre>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 */
public class ExecutionTracker {
    
    /**
     * Thread-safe list to record execution order.
     * Using CopyOnWriteArrayList for thread-safety without explicit synchronization.
     */
    private static final CopyOnWriteArrayList<String> executionLog = new CopyOnWriteArrayList<>();
    
    /**
     * Record an item execution.
     * This method is called from SpEL expressions in YAML files.
     *
     * @param itemId The ID of the item being executed
     */
    public static void record(String itemId) {
        executionLog.add(itemId);
        System.out.println("[ExecutionTracker] Recorded: " + itemId + " (position " + executionLog.size() + ")");
    }

    /**
     * Record an item execution and return a value.
     * This method is useful for SpEL expressions that need to return a value.
     *
     * @param itemId The ID of the item being executed
     * @param returnValue The value to return
     * @return The returnValue parameter
     */
    public static <T> T recordAndReturn(String itemId, T returnValue) {
        record(itemId);
        return returnValue;
    }
    
    /**
     * Get a copy of the execution log.
     * 
     * @return Immutable list of item IDs in execution order
     */
    public static List<String> getExecutionLog() {
        return Collections.unmodifiableList(new ArrayList<>(executionLog));
    }
    
    /**
     * Clear the execution log.
     * MUST be called before each test to ensure test isolation.
     */
    public static void clear() {
        executionLog.clear();
        System.out.println("[ExecutionTracker] Cleared execution log");
    }
    
    /**
     * Get the number of times an item was executed.
     * 
     * @param itemId The item ID to count
     * @return Number of times the item was executed
     */
    public static long getExecutionCount(String itemId) {
        return executionLog.stream().filter(id -> id.equals(itemId)).count();
    }
    
    /**
     * Check if an item was executed.
     * 
     * @param itemId The item ID to check
     * @return true if the item was executed at least once
     */
    public static boolean wasExecuted(String itemId) {
        return executionLog.contains(itemId);
    }
    
    /**
     * Get the execution position of an item (1-based).
     * 
     * @param itemId The item ID to find
     * @return The position (1-based) or -1 if not found
     */
    public static int getExecutionPosition(String itemId) {
        int index = executionLog.indexOf(itemId);
        return index == -1 ? -1 : index + 1;
    }
    
    /**
     * Print the execution log to console (for debugging).
     */
    public static void printLog() {
        System.out.println("[ExecutionTracker] Execution Log (" + executionLog.size() + " items):");
        for (int i = 0; i < executionLog.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + executionLog.get(i));
        }
    }
    
    /**
     * Get a formatted string representation of the execution log.
     * 
     * @return Formatted string showing execution order
     */
    public static String getFormattedLog() {
        StringBuilder sb = new StringBuilder();
        sb.append("Execution Log (").append(executionLog.size()).append(" items):\n");
        for (int i = 0; i < executionLog.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(executionLog.get(i)).append("\n");
        }
        return sb.toString();
    }
}

