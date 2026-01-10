package dev.mars.apex.sync;

import dev.mars.apex.core.config.yaml.RulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
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

            System.out.println("\n" + "=".repeat(40));
            System.out.println("APEX PIPELINE EXECUTION COMPLETED");
            System.out.println("=".repeat(40));
            System.out.println("Success: " + result.isSuccess());
            System.out.println("Message: " + result.getMessage());
            System.out.println("=".repeat(40));

            System.exit(result.isSuccess() ? 0 : 1);
        } catch (Exception e) {
            logger.error("Runner failed", e);
            System.exit(2);
        }
    }
}
