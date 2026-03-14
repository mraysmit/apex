package dev.mars.apex.core.integration;

import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration coverage for external data-source references used by lookup enrichments.
 *
 * This closes the gap between:
 * - configuration loading tests that only verify data-source-ref resolution, and
 * - lookup enrichment tests that only use inline data-sources.
 */
@DisplayName("External Data Source Reference Lookup Integration Test")
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class ExternalDataSourceReferenceLookupIntegrationTest {

    @TempDir
    Path tempDir;

    private Connection h2Connection;
    private String databaseName;

    @BeforeEach
    void setUp() throws Exception {
        databaseName = "external_lookup_" + System.nanoTime();
        h2Connection = DriverManager.getConnection(
            "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "sa",
            ""
        );

        try (Statement statement = h2Connection.createStatement()) {
            statement.execute("CREATE TABLE customers (customer_id VARCHAR(50) PRIMARY KEY, customer_name VARCHAR(100), risk_tier VARCHAR(20))");
            statement.execute("INSERT INTO customers (customer_id, customer_name, risk_tier) VALUES ('CUST001', 'Acme Corp', 'LOW')");
            statement.execute("INSERT INTO customers (customer_id, customer_name, risk_tier) VALUES ('CUST002', 'Global Industries', 'HIGH')");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (h2Connection != null && !h2Connection.isClosed()) {
            h2Connection.close();
        }
    }

    @Test
    @DisplayName("RulesEngine.fromFile should resolve external data-source refs for lookup enrichments")
    void testRulesEngineFromFileWithExternalDataSourceRefLookupEnrichment() throws Exception {
        Path externalDataSourceFile = tempDir.resolve("customer-database.yaml");
        Files.writeString(externalDataSourceFile, externalDataSourceYaml());

        Path mainConfigFile = tempDir.resolve("customer-rules.yaml");
        Files.writeString(mainConfigFile, mainConfigYaml(externalDataSourceFile));

        RulesEngine engine = RulesEngine.fromFile(mainConfigFile.toString());
        try {
            RuleResult firstResult = engine.evaluate(Map.of("customerId", "CUST001"));
            assertTrue(firstResult.isSuccess(), "Lookup via external data-source ref should succeed");
            assertFalse(firstResult.hasFailures(), "Lookup via external data-source ref should have no failures");
            assertTrue(firstResult.isTriggered(), "Rule depending on enriched fields should trigger");
            assertEquals("Acme Corp", firstResult.getEnrichedData().get("customerName"));
            assertEquals("LOW", firstResult.getEnrichedData().get("riskTier"));

            RuleResult secondResult = engine.evaluate(Map.of("customerId", "CUST002"));
            assertTrue(secondResult.isSuccess(), "Parameterized lookup should succeed for second customer");
            assertFalse(secondResult.hasFailures(), "Second lookup should have no failures");
            assertTrue(secondResult.isTriggered(), "Rule should trigger for second enriched result");
            assertEquals("Global Industries", secondResult.getEnrichedData().get("customerName"));
            assertEquals("HIGH", secondResult.getEnrichedData().get("riskTier"));
        } finally {
            engine.shutdown();
        }
    }

    private String externalDataSourceYaml() {
        return """
            metadata:
              id: "customer-database-config"
              name: "customer-database"
              type: "external-data-config"
              version: "1.0.0"
              description: "External H2 customer database configuration"

            data-sources:
              - name: "customer-database"
                type: "database"
                source-type: "h2"
                enabled: true
                connection:
                  database: "mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
                  username: "sa"
                  password: ""
                queries:
                  getCustomerById: "SELECT customer_name AS CUSTOMER_NAME, risk_tier AS RISK_TIER FROM customers WHERE customer_id = :customerId"
            """.formatted(databaseName);
    }

    private String mainConfigYaml(Path externalDataSourceFile) {
        return """
            metadata:
              id: "customer-rules"
              name: "Customer Rules"
              type: "rule-config"
              version: "1.0.0"

            data-source-refs:
              - name: "customer-database"
                source: "%s"

            enrichments:
              - id: "customer-lookup"
                name: "Customer Lookup"
                type: "lookup-enrichment"
                condition: "#customerId != null"
                lookup-config:
                  lookup-key: "#customerId"
                  lookup-dataset:
                    type: "database"
                    data-source-ref: "customer-database"
                    query-ref: "getCustomerById"
                    key-field: "customerId"
                    parameters:
                      - field: "customerId"
                        type: "string"
                field-mappings:
                  - source-field: "CUSTOMER_NAME"
                    target-field: "customerName"
                  - source-field: "RISK_TIER"
                    target-field: "riskTier"

            rules:
              - id: "customer-enrichment-complete"
                name: "Customer Enrichment Complete"
                condition: "#customerName != null && #riskTier != null"
                message: "Customer lookup completed"
                severity: "INFO"
            """.formatted(externalDataSourceFile.toString().replace("\\", "\\\\"));
    }
}