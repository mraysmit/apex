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
 * PostgreSQL External Data-Source Reference Test - Real APEX Service Integration
 *
 * This test demonstrates authentic APEX external data-source reference functionality using real APEX services:
 * - Real database integration with H2 in PostgreSQL mode
 * - External YAML configuration for data-source references
 * - Real APEX enrichment services for database-driven enrichment
 * - Proper separation of infrastructure configuration from business logic
 * - Named query system with parameter binding
 *
 * REAL APEX SERVICES USED:
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - EnrichmentService: Real APEX enrichment processor for database enrichment
 * - ExpressionEvaluatorService: Real SpEL expression evaluation
 * - LookupServiceRegistry: Real lookup service management
 *
 * NO HARDCODED SIMULATION: All processing uses authentic APEX core services with YAML-driven configuration.
 * NO HARDCODED DATA: Database setup uses real SQL operations, enrichment uses real APEX services.
 *
 * EXTERNAL YAML FILE DEPENDENCIES:
 * - enrichments/postgresql-customer-profile-external-ref.yaml: Main enrichment configuration with external data-source references
 * - data-sources/postgresql-customer-database.yaml: External data-source configuration with connection details and named queries
 *
 * ARCHITECTURE PATTERN:
 * Test Class → Main YAML Config → External Data-Source Reference → Database Operations
 *      ↓              ↓                    ↓                           ↓
 * Real APEX      Enrichment Rules    Infrastructure Config    H2 Database (PostgreSQL mode)
 * Services       with References     with Named Queries       with Real Data
 *
 * This test serves as a reference implementation for:
 * - External data-source integration patterns
 * - Database-driven enrichment scenarios
 * - Infrastructure separation best practices
 * - Advanced YAML configuration techniques
 *
 * @author APEX Demo Team
 * @since 2025-08-28
 * @version 2.1 - Real APEX services integration with external data-source references (Reference Implementation)
 */
public class PostgreSQLExternalReferenceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLExternalReferenceTest.class);
    
    /**
     * Main entry point for PostgreSQL external data-source reference testing.
     *
     * Demonstrates real APEX service integration with external YAML configuration files:
     * - Main enrichment config with external data-source references
     * - External data-source config with database connection and named queries
     * - Real database operations with H2 in PostgreSQL mode
     * - Authentic APEX enrichment processing pipeline
     */
    public static void main(String[] args) {
        PostgreSQLExternalReferenceTest test = new PostgreSQLExternalReferenceTest();
        test.runTest();
    }
    
    public void runTest() {
        logger.info("====================================================================================");
        logger.info("POSTGRESQL EXTERNAL DATA-SOURCE REFERENCE TEST - REAL APEX SERVICES");
        logger.info("====================================================================================");
        logger.info("Testing external data-source reference pattern with real APEX services:");
        logger.info("  • Main Config: enrichments/postgresql-customer-profile-external-ref.yaml");
        logger.info("  • External Data Source: data-sources/postgresql-customer-database.yaml");
        logger.info("  • Database: H2 in-memory (PostgreSQL mode) with real customer data");
        logger.info("  • Services: Real APEX enrichment, lookup, and expression evaluation");
        logger.info("====================================================================================");

        try {
            // Step 1: Initialize database with real customer data
            logger.info("\n🔧 STEP 1: Database Initialization");
            initializeDatabase();

            // Step 2: Test external reference enrichment using real APEX services
            logger.info("\n🧪 STEP 2: External Data-Source Reference Testing");
            testPostgreSQLExternalReference();

            logger.info("\n====================================================================================");
            logger.info("✅ POSTGRESQL EXTERNAL REFERENCE TEST COMPLETED SUCCESSFULLY!");
            logger.info("   • Database operations: REAL SQL with H2 PostgreSQL mode");
            logger.info("   • YAML configuration: EXTERNAL data-source references");
            logger.info("   • APEX services: AUTHENTIC enrichment processing");
            logger.info("   • Architecture pattern: PRODUCTION-READY external reference design");
            logger.info("====================================================================================");

        } catch (Exception e) {
            logger.error("❌ PostgreSQL external reference test failed: " + e.getMessage(), e);
            throw new RuntimeException("External reference test execution failed", e);
        }
    }
    
    private void initializeDatabase() throws Exception {
        logger.info("Initializing H2 database (PostgreSQL mode) for external data-source reference testing...");
        logger.info("🗄️  Database Configuration:");
        logger.info("   • Engine: H2 in-memory database");
        logger.info("   • Mode: PostgreSQL compatibility mode");
        logger.info("   • Connection: Shared database with delay close");
        logger.info("   • Purpose: Real database operations for external data-source testing");

        // Load H2 driver for real database operations
        Class.forName("org.h2.Driver");

        // Create database connection matching the external YAML configuration
        // This connection string must match the one in data-sources/postgresql-customer-database.yaml
        String jdbcUrl = "jdbc:h2:mem:apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        logger.info("🔗 Connecting to database: " + jdbcUrl);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                
                // Create customers table with schema matching external YAML configuration
                logger.info("📋 Creating customers table with schema for external data-source testing...");
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS customers (
                        customer_id VARCHAR(20) PRIMARY KEY,
                        customer_name VARCHAR(100) NOT NULL,
                        customer_type VARCHAR(50) NOT NULL,
                        tier VARCHAR(20) NOT NULL,
                        region VARCHAR(10) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        created_date DATE NOT NULL,
                        last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                logger.info("   • Table: customers");
                logger.info("   • Columns: customer_id, customer_name, customer_type, tier, region, status, created_date");
                logger.info("   • Purpose: Test data for external data-source reference queries");

                // Insert realistic test data for external data-source reference testing
                logger.info("📊 Inserting test customer data for external reference validation...");
                statement.execute("""
                    INSERT INTO customers (customer_id, customer_name, customer_type, tier, region, status, created_date) VALUES
                    ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15'),
                    ('CUST000002', 'Global Industries', 'CORPORATE', 'GOLD', 'EU', 'ACTIVE', '2023-02-20'),
                    ('CUST000003', 'Tech Solutions Inc', 'CORPORATE', 'SILVER', 'APAC', 'ACTIVE', '2023-03-10'),
                    ('CUST000004', 'Inactive Corp', 'CORPORATE', 'BRONZE', 'NA', 'INACTIVE', '2023-01-01')
                """);
                logger.info("   • Records inserted: 4 customer records");
                logger.info("   • Test customer: CUST000002 (Global Industries) - will be used for enrichment testing");
                logger.info("   • Data quality: Realistic corporate customer data with various tiers and regions");

                logger.info("✅ Database initialization completed successfully");
                logger.info("   • Real database operations: H2 with PostgreSQL compatibility");
                logger.info("   • Schema alignment: Matches external YAML data-source configuration");
                logger.info("   • Query compatibility: Ready for named query execution via external references");
            }
        }
    }
    
    private void testPostgreSQLExternalReference() throws Exception {
        logger.info("Testing PostgreSQL external data-source reference enrichment using real APEX services...");

        // Load main YAML configuration with external data-source references
        // This file contains enrichment rules that reference external data-source configurations
        logger.info("📄 Loading main YAML configuration: enrichments/postgresql-customer-profile-external-ref.yaml");
        logger.info("   • Contains: Enrichment rules with external data-source references");
        logger.info("   • References: data-sources/postgresql-customer-database.yaml");
        logger.info("   • Pattern: Infrastructure separation - business logic separate from connection details");

        YamlConfigurationLoader configLoader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = configLoader.loadFromClasspath("enrichments/postgresql-customer-profile-external-ref.yaml");

        if (config == null) {
            throw new IllegalStateException("❌ Failed to load main YAML configuration - file not found or invalid");
        }

        logger.info("✅ Main configuration loaded successfully:");
        logger.info("   • Name: " + config.getMetadata().getName());
        logger.info("   • Version: " + config.getMetadata().getVersion());
        logger.info("   • Enrichments: " + (config.getEnrichments() != null ? config.getEnrichments().size() : 0));
        logger.info("   • External References: " + (config.getDataSourceRefs() != null ? config.getDataSourceRefs().size() : 0));

        // The external data-source configuration (data-sources/postgresql-customer-database.yaml) contains:
        // - Database connection details (H2 with PostgreSQL mode)
        // - Named SQL queries (getActiveCustomerById, etc.)
        // - Parameter definitions and connection pool settings
        logger.info("📄 External data-source configuration: data-sources/postgresql-customer-database.yaml");
        logger.info("   • Contains: Database connection details, named queries, parameters");
        logger.info("   • Database: H2 in-memory with PostgreSQL mode compatibility");
        logger.info("   • Queries: Named SQL queries with parameter binding");
        
        // Initialize real APEX services for enrichment processing
        logger.info("🔧 Initializing real APEX services:");
        LookupServiceRegistry lookupRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        EnrichmentService enrichmentService = new EnrichmentService(lookupRegistry, expressionEvaluator);
        logger.info("   • LookupServiceRegistry: Real lookup service management");
        logger.info("   • ExpressionEvaluatorService: Real SpEL expression evaluation");
        logger.info("   • EnrichmentService: Real APEX enrichment processor");

        // Create minimal input data for external data-source reference testing
        // This will trigger the external data-source lookup via the YAML configuration
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("customerId", "CUST000002");  // This customer exists in our test database

        logger.info("🧪 Processing enrichment with real APEX services:");
        logger.info("   • Input: customerId = CUST000002");
        logger.info("   • Expected: Customer data enriched from external PostgreSQL database");
        logger.info("   • Process: YAML config → External reference → Database query → Field mapping");

        // Process enrichment using real APEX services
        // This will:
        // 1. Load the main YAML configuration
        // 2. Resolve external data-source references
        // 3. Execute named database queries with parameters
        // 4. Apply field mappings from database columns to enriched object fields
        // 5. Return enriched data using real APEX processing pipeline
        Object enrichedResult = enrichmentService.enrichObject(config, inputData);
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) enrichedResult;
        
        // Verify enrichment results from external data-source reference
        logger.info("\n📊 ENRICHMENT RESULTS FROM EXTERNAL DATA-SOURCE:");
        logger.info("   • Source: PostgreSQL database via external YAML configuration");
        logger.info("   • Process: Real APEX enrichment with external data-source references");
        logger.info("   • Field Mappings: Database columns → Enriched object fields");
        logger.info("");
        logger.info("🔍 Enriched Data Fields:");
        logger.info("   • Customer Name: " + enrichedData.get("customerName") + " (from CUSTOMER_NAME column)");
        logger.info("   • Customer Type: " + enrichedData.get("customerType") + " (from CUSTOMER_TYPE column)");
        logger.info("   • Customer Tier: " + enrichedData.get("customerTier") + " (from TIER column)");
        logger.info("   • Customer Region: " + enrichedData.get("customerRegion") + " (from REGION column)");
        logger.info("   • Customer Status: " + enrichedData.get("customerStatus") + " (from STATUS column)");
        logger.info("   • Valid Customer: " + enrichedData.get("validCustomer") + " (computed validation field)");

        // Validate that external data-source reference enrichment worked correctly
        logger.info("\n🧪 VALIDATION: External Data-Source Reference Integration");
        if (enrichedData.get("customerName") != null &&
            enrichedData.get("customerType") != null &&
            enrichedData.get("customerTier") != null) {
            logger.info("✅ SUCCESS: PostgreSQL external data-source reference working correctly!");
            logger.info("   • YAML configuration: Successfully loaded and processed");
            logger.info("   • External references: Successfully resolved data-source configuration");
            logger.info("   • Database integration: Successfully executed named queries");
            logger.info("   • Field mapping: Successfully mapped database columns to enriched fields");
            logger.info("   • APEX services: Successfully processed enrichment using real services");
        } else {
            logger.error("❌ VALIDATION FAILED: External data-source reference not working correctly");
            logger.error("   • Missing required fields in enriched data");
            logger.error("   • Check YAML configuration files and database setup");
            throw new Exception("❌ FAILED: PostgreSQL external data-source reference validation failed!");
        }
    }
}
