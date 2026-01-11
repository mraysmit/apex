package dev.mars.apex.sync;

import dev.mars.apex.core.config.yaml.RulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;

/**
 * Standard APEX Runner for Table Synchronization.
 * Zero-Custom implementation delegating 100% to apex-core.
 */
public class TableSyncRunner {

    private static final Logger logger = LoggerFactory.getLogger(TableSyncRunner.class);

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java -jar apex-database-table-sync.jar --config=<path>");
            System.exit(1);
        }

        String configPath = null;
        for (String arg : args) {
            if (arg.startsWith("--config=")) {
                configPath = arg.substring("--config=".length());
            }
        }

        if (configPath == null) {
            System.err.println("Error: --config parameter is required");
            System.exit(1);
        }

        try {
            logger.info("Starting Zero-Custom APEX Sync Runner...");

            // Direct delegation to APEX Core Service
            RulesEngineService service = new RulesEngineService();
            RulesEngine engine = service.createRulesEngineFromFile(new File(configPath));
            RuleResult result = engine.evaluate(new HashMap<>());

            // Display execution summary
            System.out.println("\n" + "=".repeat(60));
            System.out.println("APEX PIPELINE EXECUTION COMPLETED");
            System.out.println("=".repeat(60));
            System.out.println("Success: " + result.isSuccess());
            System.out.println("Message: " + result.getMessage());
            System.out.println("=".repeat(60));

            // Extract and display step-level metrics
            System.out.println("\nPIPELINE STEP METRICS:");
            System.out.println("-".repeat(60));

            int totalRecordsProcessed = 0;
            int totalRecordsFailed = 0;
            long totalDuration = 0;

            for (ExecutionStep step : result.getExecutionPath()) {
                if ("PIPELINE_STEP".equals(step.getType())) {
                    System.out.printf("Step: %s%n", step.getName());
                    System.out.printf("  Status: %s%n", step.getStatus());
                    System.out.printf("  Duration: %d ms%n", step.getDurationMs());

                    if (step.getRecordsProcessed() != null) {
                        System.out.printf("  Records Processed: %d%n", step.getRecordsProcessed());
                        totalRecordsProcessed += step.getRecordsProcessed();
                    }

                    if (step.getRecordsFailed() != null) {
                        System.out.printf("  Records Failed: %d%n", step.getRecordsFailed());
                        totalRecordsFailed += step.getRecordsFailed();
                    }

                    // Success rate is always available (returns 100.0 or 0.0 if metrics not available)
                    if (step.getRecordsProcessed() != null && step.getRecordsFailed() != null) {
                        System.out.printf("  Success Rate: %.2f%%%n", step.getSuccessRate());
                    }

                    if (step.hasStepData()) {
                        Object stepData = step.getStepData();
                        if (stepData instanceof java.util.List) {
                            System.out.printf("  Data Size: %d records%n", ((java.util.List<?>) stepData).size());
                        }
                    }

                    totalDuration += step.getDurationMs();
                    System.out.println();
                }
            }

            // Display summary metrics
            System.out.println("-".repeat(60));
            System.out.println("SUMMARY:");
            System.out.printf("  Total Records Processed: %d%n", totalRecordsProcessed);
            System.out.printf("  Total Records Failed: %d%n", totalRecordsFailed);
            System.out.printf("  Total Duration: %d ms%n", totalDuration);
            System.out.println("=".repeat(60));

            // Cleanup
            engine.shutdown();

            System.exit(result.isSuccess() ? 0 : 1);
        } catch (Exception e) {
            logger.error("Runner failed", e);
            System.exit(2);
        }
    }
}
