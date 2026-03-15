package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.cache.ApexCacheManager;
import dev.mars.apex.core.config.model.YamlDataSource;
import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.data.external.registry.DataSourceRegistry;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.engine.core.ExpressionEvaluatorService;
import dev.mars.apex.engine.core.UnifiedRuleEvaluator;
import dev.mars.apex.engine.execution.RuleGroupEvaluationService;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("EnrichmentProcessor concurrent configuration isolation")
class EnrichmentProcessorConcurrentConfigurationTest {

    private EnrichmentProcessor processor;

    @BeforeEach
    void setUp() throws Exception {
        ApexCacheManager.getInstance().clearAll();
        DataSourceRegistry.getInstance().clear();
        DataSourceFactory.getInstance().clearCache();

        processor = new EnrichmentProcessor(
                new LookupServiceRegistry(),
                new ExpressionEvaluatorService(),
                null,
                new RuleGroupEvaluationService(new UnifiedRuleEvaluator()));

        setupDatabase("enrichment_config_a", "Alpha Co");
        setupDatabase("enrichment_config_b", "Beta Co");
    }

    @AfterEach
    void tearDown() {
        ApexCacheManager.getInstance().clearAll();
        DataSourceRegistry.getInstance().clear();
        DataSourceFactory.getInstance().clearCache();
    }

    @Test
    @DisplayName("Shared processor isolates database lookup configuration per request")
    void sharedProcessorUsesPerRequestConfiguration() throws Exception {
        YamlRuleConfiguration configA = createDatabaseLookupConfig("customer-db-a", "mem:enrichment_config_a;DB_CLOSE_DELAY=-1");
        YamlRuleConfiguration configB = createDatabaseLookupConfig("customer-db-b", "mem:enrichment_config_b;DB_CLOSE_DELAY=-1");

        int threadCount = 6;
        int iterationsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int index = 0; index < threadCount; index++) {
            final boolean useConfigA = index % 2 == 0;
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int iteration = 0; iteration < iterationsPerThread; iteration++) {
                        Map<String, Object> input = new HashMap<>();
                        input.put("customerId", "CUST001");

                        YamlRuleConfiguration selectedConfig = useConfigA ? configA : configB;
                        String expectedName = useConfigA ? "Alpha Co" : "Beta Co";

                        RuleResult result = processor.processEnrichmentsWithResult(
                                new ArrayList<>(selectedConfig.getEnrichments()), input, selectedConfig);

                        assertTrue(result.isSuccess(), "Lookup should succeed for " + expectedName);
                        assertNotNull(result.getEnrichedData(), "Enriched data should be present");
                        assertEquals(expectedName, result.getEnrichedData().get("customerName"),
                                "Shared processor must not bleed database config across requests");
                    }
                } catch (Throwable throwable) {
                    errors.add(throwable);
                }
            }));
        }

        startLatch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "Concurrent enrichment tasks did not finish");

        for (Future<?> future : futures) {
            future.get();
        }

        assertTrue(errors.isEmpty(),
                "Shared processor leaked configuration state: " +
                        (errors.isEmpty() ? "" : errors.get(0).getMessage()));
    }

    private static void setupDatabase(String databaseName, String customerName) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1", "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS customers");
            statement.execute("CREATE TABLE customers (customer_id VARCHAR(50) PRIMARY KEY, customer_name VARCHAR(100) NOT NULL)");
            statement.execute("INSERT INTO customers VALUES ('CUST001', '" + customerName + "')");
        }
    }

    private static YamlRuleConfiguration createDatabaseLookupConfig(String dataSourceName, String databaseName) {
        YamlDataSource dataSource = new YamlDataSource();
        dataSource.setName(dataSourceName);
        dataSource.setType("database");
        dataSource.setSourceType("h2");

        Map<String, Object> connection = new HashMap<>();
        connection.put("database", databaseName);
        connection.put("username", "sa");
        connection.put("password", "");
        connection.put("pool-size", 3);
        dataSource.setConnection(connection);

        YamlEnrichment.LookupDataset.ParameterMapping parameter = new YamlEnrichment.LookupDataset.ParameterMapping();
        parameter.setField("customerId");
        parameter.setType("string");

        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("database");
        dataset.setDataSourceRef(dataSourceName);
        dataset.setQuery("SELECT customer_name FROM customers WHERE customer_id = :customerId");
        dataset.setKeyField("customerId");
        dataset.setParameters(List.of(parameter));

        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupKey("#customerId");
        lookupConfig.setLookupDataset(dataset);

        YamlEnrichment.FieldMapping fieldMapping = new YamlEnrichment.FieldMapping();
        fieldMapping.setSourceField("CUSTOMER_NAME");
        fieldMapping.setTargetField("customerName");
        fieldMapping.setRequired(true);

        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("customer-lookup");
        enrichment.setType("lookup-enrichment");
        enrichment.setLookupConfig(lookupConfig);
        enrichment.setFieldMappings(List.of(fieldMapping));

        YamlRuleConfiguration configuration = new YamlRuleConfiguration();
        configuration.setDataSources(List.of(dataSource));
        configuration.setEnrichments(List.of(enrichment));
        return configuration;
    }
}