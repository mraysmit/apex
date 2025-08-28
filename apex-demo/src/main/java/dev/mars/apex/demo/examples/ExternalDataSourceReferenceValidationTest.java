package dev.mars.apex.demo.examples;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive validation test for external data-source reference functionality.
 * 
 * This test validates all aspects of the external data-source reference system:
 * - External data-source YAML configuration loading
 * - Named query resolution from external configurations
 * - Database connectivity and query execution
 * - Field mapping and enrichment processing
 * - Complete multi-table database scenarios
 * 
 * @author APEX Demo Team
 * @since 2025-08-28
 * @version 1.0.0
 */
public class ExternalDataSourceReferenceValidationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalDataSourceReferenceValidationTest.class);
    
    private final YamlConfigurationLoader configLoader;
    private final EnrichmentService enrichmentService;
    
    public ExternalDataSourceReferenceValidationTest() {
        this.configLoader = new YamlConfigurationLoader();
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        this.enrichmentService = new EnrichmentService(serviceRegistry, new ExpressionEvaluatorService());
        logger.info("ExternalDataSourceReferenceValidationTest initialized");
    }
    
    public static void main(String[] args) {
        ExternalDataSourceReferenceValidationTest test = new ExternalDataSourceReferenceValidationTest();
        test.runAllTests();
    }
    
    public void runAllTests() {
        logger.info("====================================================================================");
        logger.info("EXTERNAL DATA-SOURCE REFERENCE VALIDATION TESTS");
        logger.info("====================================================================================");
        
        try {
            // Initialize complete database with all tables
            initializeCompleteDatabase();
            
            // Test 1: Basic External Reference Resolution
            testBasicExternalReferenceResolution();
            
            // Test 2: Named Query Resolution
            testNamedQueryResolution();
            
            // Test 3: Customer Profile Enrichment with Real Data
            testCustomerProfileEnrichmentWithRealData();
            
            // Test 4: Multi-Table Settlement Enrichment
            testMultiTableSettlementEnrichment();
            
            // Test 5: Configuration Validation
            testConfigurationValidation();
            
            logger.info("====================================================================================");
            logger.info("ALL EXTERNAL DATA-SOURCE REFERENCE VALIDATION TESTS PASSED!");
            logger.info("====================================================================================");
            
        } catch (Exception e) {
            logger.error("Validation tests failed: " + e.getMessage(), e);
            System.exit(1);
        }
    }
    
    private void initializeCompleteDatabase() throws Exception {
        logger.info("Initializing complete H2 database with all required tables...");
        
        String jdbcUrl = "jdbc:h2:mem:apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();
            
            // Create customers table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    customer_id VARCHAR(20) PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    customer_type VARCHAR(20) NOT NULL,
                    tier VARCHAR(20) NOT NULL,
                    region VARCHAR(10) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    created_date DATE NOT NULL,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Insert customer data with ACTIVE status
            statement.execute("""
                INSERT INTO customers (customer_id, customer_name, customer_type, tier, region, status, created_date) VALUES
                ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15'),
                ('CUST000002', 'Global Industries Ltd', 'CORPORATE', 'GOLD', 'EU', 'ACTIVE', '2023-02-20'),
                ('CUST000003', 'Tech Innovations Inc', 'CORPORATE', 'SILVER', 'APAC', 'ACTIVE', '2023-03-10'),
                ('CUST000004', 'Financial Services Co', 'INSTITUTIONAL', 'PLATINUM', 'NA', 'ACTIVE', '2023-04-05'),
                ('CUST000005', 'Investment Partners', 'INSTITUTIONAL', 'GOLD', 'EU', 'ACTIVE', '2023-05-12')
            """);
            
            // Create counterparties table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS counterparties (
                    counterparty_id VARCHAR(20) PRIMARY KEY,
                    counterparty_name VARCHAR(100) NOT NULL,
                    counterparty_type VARCHAR(20) NOT NULL,
                    credit_rating VARCHAR(10),
                    status VARCHAR(20) NOT NULL,
                    created_date DATE NOT NULL
                )
            """);
            
            // Insert counterparty data
            statement.execute("""
                INSERT INTO counterparties (counterparty_id, counterparty_name, counterparty_type, credit_rating, status, created_date) VALUES
                ('CP_GS', 'Goldman Sachs', 'INVESTMENT_BANK', 'A+', 'ACTIVE', '2023-01-01'),
                ('CP_MS', 'Morgan Stanley', 'INVESTMENT_BANK', 'A', 'ACTIVE', '2023-01-01'),
                ('CP_JPM', 'JPMorgan Chase', 'COMMERCIAL_BANK', 'A+', 'ACTIVE', '2023-01-01'),
                ('CP_CITI', 'Citigroup', 'COMMERCIAL_BANK', 'A-', 'ACTIVE', '2023-01-01'),
                ('CP_BOA', 'Bank of America', 'COMMERCIAL_BANK', 'A', 'ACTIVE', '2023-01-01')
            """);
            
            // Create settlement_instructions table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS settlement_instructions (
                    instruction_id VARCHAR(20) PRIMARY KEY,
                    counterparty_id VARCHAR(20) NOT NULL,
                    custodian_name VARCHAR(100) NOT NULL,
                    custodian_bic VARCHAR(20) NOT NULL,
                    settlement_method VARCHAR(20) NOT NULL,
                    delivery_instruction VARCHAR(100),
                    market_name VARCHAR(20) NOT NULL,
                    settlement_cycle VARCHAR(10) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    created_date DATE NOT NULL,
                    FOREIGN KEY (counterparty_id) REFERENCES counterparties(counterparty_id)
                )
            """);
            
            // Insert settlement instructions data
            statement.execute("""
                INSERT INTO settlement_instructions (instruction_id, counterparty_id, custodian_name, custodian_bic, settlement_method, delivery_instruction, market_name, settlement_cycle, status, created_date) VALUES
                ('SI_GS_001', 'CP_GS', 'Goldman Sachs Custody', 'GSCCUS33', 'DVP', 'Free of Payment', 'NYSE', 'T+2', 'ACTIVE', '2023-01-01'),
                ('SI_MS_001', 'CP_MS', 'Morgan Stanley Custody', 'MSINUS33', 'DVP', 'Against Payment', 'NASDAQ', 'T+2', 'ACTIVE', '2023-01-01'),
                ('SI_JPM_001', 'CP_JPM', 'JPMorgan Custody', 'CHASUS33', 'RVP', 'Free of Payment', 'NYSE', 'T+1', 'ACTIVE', '2023-01-01'),
                ('SI_CITI_001', 'CP_CITI', 'Citibank Custody', 'CITIUS33', 'DVP', 'Against Payment', 'LSE', 'T+2', 'ACTIVE', '2023-01-01'),
                ('SI_BOA_001', 'CP_BOA', 'Bank of America Custody', 'BOFAUS3N', 'DVP', 'Free of Payment', 'NYSE', 'T+2', 'ACTIVE', '2023-01-01')
            """);
            
            // Create risk_assessments table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS risk_assessments (
                    assessment_id VARCHAR(20) PRIMARY KEY,
                    counterparty_id VARCHAR(20) NOT NULL,
                    risk_category VARCHAR(20) NOT NULL,
                    risk_score INTEGER NOT NULL,
                    max_exposure DECIMAL(15,2) NOT NULL,
                    approval_required BOOLEAN NOT NULL,
                    monitoring_level VARCHAR(20) NOT NULL,
                    last_assessment_date DATE NOT NULL,
                    FOREIGN KEY (counterparty_id) REFERENCES counterparties(counterparty_id)
                )
            """);
            
            // Insert risk assessments data
            statement.execute("""
                INSERT INTO risk_assessments (assessment_id, counterparty_id, risk_category, risk_score, max_exposure, approval_required, monitoring_level, last_assessment_date) VALUES
                ('RA_GS_001', 'CP_GS', 'LOW', 85, 1000000000.00, false, 'STANDARD', '2023-12-01'),
                ('RA_MS_001', 'CP_MS', 'LOW', 82, 800000000.00, false, 'STANDARD', '2023-12-01'),
                ('RA_JPM_001', 'CP_JPM', 'LOW', 88, 1200000000.00, false, 'STANDARD', '2023-12-01'),
                ('RA_CITI_001', 'CP_CITI', 'MEDIUM', 75, 600000000.00, true, 'ENHANCED', '2023-12-01'),
                ('RA_BOA_001', 'CP_BOA', 'LOW', 80, 900000000.00, false, 'STANDARD', '2023-12-01')
            """);
            
            logger.info("Complete database initialized successfully:");
            logger.info("  - customers: 5 records (all ACTIVE)");
            logger.info("  - counterparties: 5 records (all ACTIVE)");
            logger.info("  - settlement_instructions: 5 records (all ACTIVE)");
            logger.info("  - risk_assessments: 5 records");
        }
    }

    private void testBasicExternalReferenceResolution() throws Exception {
        logger.info("\n============================================================");
        logger.info("TEST 1: Basic External Reference Resolution");
        logger.info("============================================================");

        // Load configuration with external data-source references
        YamlRuleConfiguration config = configLoader.loadFromClasspath("enrichments/customer-profile-enrichment-lean.yaml");

        // Validate that external references were resolved
        assertNotNull("Configuration should not be null", config);
        assertNotNull("Data sources should not be null", config.getDataSources());
        assertFalse("Data sources should not be empty", config.getDataSources().isEmpty());

        // Find the resolved customer-database data source
        boolean foundCustomerDatabase = false;
        for (var dataSource : config.getDataSources()) {
            if ("customer-database".equals(dataSource.getName())) {
                foundCustomerDatabase = true;
                assertNotNull("Connection should not be null", dataSource.getConnection());
                assertNotNull("Queries should not be null", dataSource.getQueries());
                assertFalse("Queries should not be empty", dataSource.getQueries().isEmpty());
                logger.info("✅ Found resolved customer-database with " + dataSource.getQueries().size() + " named queries");
                break;
            }
        }

        assertTrue("Customer database should be resolved from external reference", foundCustomerDatabase);
        logger.info("✅ TEST 1 PASSED: External reference resolution working correctly");
    }

    private void testNamedQueryResolution() throws Exception {
        logger.info("\n============================================================");
        logger.info("TEST 2: Named Query Resolution");
        logger.info("============================================================");

        // Load configuration
        YamlRuleConfiguration config = configLoader.loadFromClasspath("enrichments/customer-profile-enrichment-lean.yaml");

        // Check that enrichments reference named queries
        assertNotNull("Enrichments should not be null", config.getEnrichments());
        assertFalse("Enrichments should not be empty", config.getEnrichments().isEmpty());

        // Find the customer profile enrichment
        boolean foundNamedQueryReference = false;
        for (var enrichment : config.getEnrichments()) {
            if ("customer-profile-lookup-lean".equals(enrichment.getId())) {
                var lookupConfig = enrichment.getLookupConfig();
                assertNotNull("Lookup config should not be null", lookupConfig);

                var dataset = lookupConfig.getLookupDataset();
                assertNotNull("Dataset should not be null", dataset);

                // Check for data-source-ref and query-ref
                assertNotNull("Data source ref should not be null", dataset.getDataSourceRef());
                assertNotNull("Query ref should not be null", dataset.getQueryRef());

                assertEquals("Data source ref should be customer-database", "customer-database", dataset.getDataSourceRef());
                assertEquals("Query ref should be getActiveCustomerById", "getActiveCustomerById", dataset.getQueryRef());

                foundNamedQueryReference = true;
                logger.info("✅ Found named query reference: " + dataset.getQueryRef() + " from data-source: " + dataset.getDataSourceRef());
                break;
            }
        }

        assertTrue("Named query reference should be found", foundNamedQueryReference);
        logger.info("✅ TEST 2 PASSED: Named query resolution configuration correct");
    }

    private void testCustomerProfileEnrichmentWithRealData() throws Exception {
        logger.info("\n============================================================");
        logger.info("TEST 3: Customer Profile Enrichment with Real Data");
        logger.info("============================================================");

        // Input data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("customerId", "CUST000001");

        logger.info("Input: customerId = " + inputData.get("customerId"));

        // Load configuration and process enrichment
        YamlRuleConfiguration config = configLoader.loadFromClasspath("enrichments/customer-profile-enrichment-lean.yaml");
        Map<String, Object> enrichedResult = new HashMap<>(inputData);
        enrichmentService.enrichObject(config, enrichedResult);

        // Validate results
        assertNotNull("Customer name should be enriched", enrichedResult.get("customerName"));
        assertNotNull("Customer type should be enriched", enrichedResult.get("customerType"));
        assertNotNull("Customer tier should be enriched", enrichedResult.get("customerTier"));
        assertNotNull("Customer region should be enriched", enrichedResult.get("customerRegion"));
        assertNotNull("Customer status should be enriched", enrichedResult.get("customerStatus"));

        // Validate specific values
        assertEquals("Customer name should be Acme Corporation", "Acme Corporation", enrichedResult.get("customerName"));
        assertEquals("Customer type should be CORPORATE", "CORPORATE", enrichedResult.get("customerType"));
        assertEquals("Customer tier should be PLATINUM", "PLATINUM", enrichedResult.get("customerTier"));
        assertEquals("Customer region should be NA", "NA", enrichedResult.get("customerRegion"));
        assertEquals("Customer status should be ACTIVE", "ACTIVE", enrichedResult.get("customerStatus"));

        logger.info("✅ Enriched Data:");
        logger.info("  Customer Name: " + enrichedResult.get("customerName"));
        logger.info("  Customer Type: " + enrichedResult.get("customerType"));
        logger.info("  Customer Tier: " + enrichedResult.get("customerTier"));
        logger.info("  Customer Region: " + enrichedResult.get("customerRegion"));
        logger.info("  Customer Status: " + enrichedResult.get("customerStatus"));

        logger.info("✅ TEST 3 PASSED: Customer profile enrichment working with real data");
    }

    private void testMultiTableSettlementEnrichment() throws Exception {
        logger.info("\n============================================================");
        logger.info("TEST 4: Multi-Table Settlement Enrichment");
        logger.info("============================================================");

        // Input data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("counterpartyId", "CP_GS");

        logger.info("Input: counterpartyId = " + inputData.get("counterpartyId"));

        // Load configuration and process enrichment
        YamlRuleConfiguration config = configLoader.loadFromClasspath("enrichments/settlement-instruction-enrichment-lean.yaml");
        Map<String, Object> enrichedResult = new HashMap<>(inputData);
        enrichmentService.enrichObject(config, enrichedResult);

        // Validate results from multi-table JOIN
        assertNotNull("Counterparty name should be enriched", enrichedResult.get("counterpartyName"));
        assertNotNull("Settlement instruction ID should be enriched", enrichedResult.get("settlementInstructionId"));
        assertNotNull("Custodian name should be enriched", enrichedResult.get("custodianName"));
        assertNotNull("Risk category should be enriched", enrichedResult.get("riskCategory"));

        // Validate specific values
        assertEquals("Counterparty name should be Goldman Sachs", "Goldman Sachs", enrichedResult.get("counterpartyName"));
        assertEquals("Settlement instruction ID should be SI_GS_001", "SI_GS_001", enrichedResult.get("settlementInstructionId"));
        assertEquals("Custodian name should be Goldman Sachs Custody", "Goldman Sachs Custody", enrichedResult.get("custodianName"));
        assertEquals("Risk category should be LOW", "LOW", enrichedResult.get("riskCategory"));

        logger.info("✅ Multi-Table Enriched Data:");
        logger.info("  Counterparty Name: " + enrichedResult.get("counterpartyName"));
        logger.info("  Settlement Instruction ID: " + enrichedResult.get("settlementInstructionId"));
        logger.info("  Custodian Name: " + enrichedResult.get("custodianName"));
        logger.info("  Custodian BIC: " + enrichedResult.get("custodianBic"));
        logger.info("  Settlement Method: " + enrichedResult.get("settlementMethod"));
        logger.info("  Risk Category: " + enrichedResult.get("riskCategory"));
        logger.info("  Risk Score: " + enrichedResult.get("riskScore"));

        logger.info("✅ TEST 4 PASSED: Multi-table settlement enrichment working correctly");
    }

    private void testConfigurationValidation() throws Exception {
        logger.info("\n============================================================");
        logger.info("TEST 5: Configuration Validation");
        logger.info("============================================================");

        // Test that both customer and settlement configurations load successfully
        YamlRuleConfiguration customerConfig = configLoader.loadFromClasspath("enrichments/customer-profile-enrichment-lean.yaml");
        YamlRuleConfiguration settlementConfig = configLoader.loadFromClasspath("enrichments/settlement-instruction-enrichment-lean.yaml");

        // Validate customer configuration
        assertNotNull("Customer config should not be null", customerConfig);
        assertNotNull("Customer config metadata should not be null", customerConfig.getMetadata());
        assertEquals("Customer config name should match", "Customer Profile Enrichment - Lean Version", customerConfig.getMetadata().getName());
        assertEquals("Customer config version should be 2.1.0", "2.1.0", customerConfig.getMetadata().getVersion());

        // Validate settlement configuration
        assertNotNull("Settlement config should not be null", settlementConfig);
        assertNotNull("Settlement config metadata should not be null", settlementConfig.getMetadata());
        assertEquals("Settlement config name should match", "Settlement Instruction Enrichment - Lean Version", settlementConfig.getMetadata().getName());
        assertEquals("Settlement config version should be 2.1.0", "2.1.0", settlementConfig.getMetadata().getVersion());

        logger.info("✅ Configuration Validation Results:");
        logger.info("  Customer Config: " + customerConfig.getMetadata().getName() + " v" + customerConfig.getMetadata().getVersion());
        logger.info("  Settlement Config: " + settlementConfig.getMetadata().getName() + " v" + settlementConfig.getMetadata().getVersion());
        logger.info("  Customer Enrichments: " + customerConfig.getEnrichments().size());
        logger.info("  Settlement Enrichments: " + settlementConfig.getEnrichments().size());
        logger.info("  Customer Data Sources: " + (customerConfig.getDataSources() != null ? customerConfig.getDataSources().size() : 0));
        logger.info("  Settlement Data Sources: " + (settlementConfig.getDataSources() != null ? settlementConfig.getDataSources().size() : 0));

        logger.info("✅ TEST 5 PASSED: Configuration validation successful");
    }

    // Simple assertion methods for testing
    private void assertNotNull(String message, Object obj) {
        if (obj == null) {
            throw new AssertionError(message + " - Expected non-null but was null");
        }
    }

    private void assertFalse(String message, boolean condition) {
        if (condition) {
            throw new AssertionError(message + " - Expected false but was true");
        }
    }

    private void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message + " - Expected true but was false");
        }
    }

    private void assertEquals(String message, Object expected, Object actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " - Expected: " + expected + " but was: " + actual);
        }
    }
}
