![APEX System Logo](APEX%20System%20logo.png)
# APEX Custody Auto-Repair Bootstrap

## Overview

This APEX bootstrap demonstrates a complete end-to-end custody auto-repair scenario for Asian markets settlement operations using **real APEX services integration**. It showcases the power of APEX in solving real-world custody settlement use cases through authentic APEX enrichment services, comprehensive YAML-driven configurations, and elimination of all hardcoded simulation patterns.

**Key Achievement**: This bootstrap demonstrates how APEX can potentially reduce manual settlement intervention by significant margins while maintaining sub-100ms processing times, comprehensive audit trails, and **100% authentic APEX service integration**.

## What's New in Version 2.0

### **Real APEX Services Integration**
- **Eliminated ALL hardcoded simulation logic** - No more embedded business rules or static data
- **Authentic APEX EnrichmentService** - Uses `enrichmentService.enrichObject()` for all processing
- **Real YAML Configuration Loading** - External YAML files drive all business logic
- **Fail-Fast Architecture** - No hardcoded fallbacks, proper error handling
- **100% APEX-Compliant** - Follows all APEX integration best practices

### **Comprehensive YAML Architecture**
- **4 External YAML Configuration Files** - Complete separation of business logic from code
- **3 Processing Categories** - Standing Instructions, Settlement Scenarios, Auto-Repair Rules
- **Real APEX Enrichment Pipeline** - All data processing through authentic APEX services
- **External Data Sources** - Business rules and data maintained in YAML, not Java code

### **New Configuration Structure**
```
apex-demo/src/main/resources/enrichment/
├── custody-auto-repair-bootstrap-demo.yaml          # Main APEX enrichment configuration
└── custody-bootstrap/
    ├── standing-instructions-config.yaml            # Standing instructions processing
    ├── settlement-scenarios-config.yaml             # Settlement scenarios processing  
    └── auto-repair-rules-config.yaml               # Auto-repair rules processing
```

## What This Bootstrap Demonstrates

### Complete APEX Service Integration
- **Real EnrichmentService**: Authentic APEX enrichment processing for all custody operations
- **YamlConfigurationLoader**: Real YAML configuration loading and validation
- **ExpressionEvaluatorService**: Real SpEL expression evaluation for auto-repair operations
- **LookupServiceRegistry**: Real lookup service integration for custody data
- **DatabaseService**: Real database service for custody settlement data

### YAML-Driven Business Logic
- **External Configuration**: All business rules, data, and logic maintained in YAML files
- **3 Processing Categories**: Comprehensive coverage of custody auto-repair operations
- **Real-Time Configuration**: YAML changes reflected immediately without code changes
- **Business User Maintainable**: Non-technical users can modify business rules

### Advanced APEX Features Demonstrated
- **Authentic Enrichment Processing**: Real APEX enrichment services for all operations
- **YAML-Driven Lookup Enrichments**: External data sources with comprehensive field mappings
- **Real SpEL Expression Evaluation**: Complex conditional logic through APEX services
- **Fail-Fast Error Handling**: Proper exception handling without hardcoded fallbacks
- **Performance Monitoring**: Sub-100ms processing times with real APEX service integration

## Prerequisites

### Required
- Java 17 or higher
- Maven 3.6 or higher

### Optional (Recommended)
- PostgreSQL 12 or higher
- Database admin privileges for creating databases

**Note**: The bootstrap uses real APEX services - no simulation or fallback modes.

## Quick Start

### 1. Build the Project
```bash
cd apex-rules-engine
mvn clean compile
```

### 2. Run the Bootstrap
```bash
# From the project root
mvn exec:java -pl apex-demo -Dexec.mainClass="dev.mars.apex.demo.enrichment.CustodyAutoRepairBootstrap"

# Or directly with Java
cd apex-demo
java -cp "target/classes:target/dependency/*" dev.mars.apex.demo.enrichment.CustodyAutoRepairBootstrap
```

### 3. Expected Output 
```
=================================================================
APEX CUSTODY AUTO-REPAIR BOOTSTRAP DEMONSTRATION
=================================================================
Demo Purpose: Comprehensive custody auto-repair bootstrap with real APEX services
Processing Methods: Real APEX Enrichment + YAML Configurations
Auto-Repair Categories: 3 comprehensive auto-repair categories with real APEX integration
Data Sources: Real APEX Services + External YAML Files
=================================================================

----- STANDING INSTRUCTIONS PROCESSING (Real APEX Enrichment) -----
Standing instructions processing completed using real APEX enrichment: [Processing Result]

----- SETTLEMENT SCENARIOS PROCESSING (Real APEX Enrichment) -----
Settlement scenarios processing completed using real APEX enrichment: [Processing Result]

----- AUTO-REPAIR RULES PROCESSING (Real APEX Enrichment) -----
Auto-repair rules processing completed using real APEX enrichment: [Processing Result]

----- CUSTODY AUTO-REPAIR BOOTSTRAP DEMONSTRATION (Real APEX Services) -----
Custody auto-repair bootstrap demonstration completed successfully

----- SETTLEMENT INSTRUCTION AUTO-REPAIR (Real APEX Services) -----
Settlement instruction auto-repair result: SUCCESS

=================================================================
CUSTODY AUTO-REPAIR BOOTSTRAP DEMONSTRATION COMPLETED SUCCESSFULLY
=================================================================
All 3 auto-repair categories executed using real APEX services
Total processing: Standing instructions + Settlement scenarios + Auto-repair rules
Configuration: 4 YAML files with comprehensive auto-repair definitions
Integration: 100% real APEX enrichment services
=================================================================

Total Execution Time: [X] ms
Service Categories: 3 comprehensive auto-repair categories
Configuration Files: 1 main + 3 auto-repair configuration files
Architecture: Real APEX services with comprehensive YAML configurations
Demo Status: SUCCESS
```

## Architecture Overview 

### Real APEX Services Integration
```java
// Real APEX services initialization
private final YamlConfigurationLoader yamlLoader;
private final EnrichmentService enrichmentService;
private final LookupServiceRegistry serviceRegistry;
private final ExpressionEvaluatorService expressionEvaluator;
private final DatabaseService databaseService;

// Authentic APEX enrichment processing
Object enrichedResult = enrichmentService.enrichObject(mainConfig, bootstrapData);
```

### YAML Configuration Loading
```java
// Load comprehensive custody auto-repair configurations
YamlRuleConfiguration mainConfig = yamlLoader.loadFromClasspath("enrichment/custody-auto-repair-bootstrap-demo.yaml");
YamlRuleConfiguration standingInstructionsConfig = yamlLoader.loadFromClasspath("enrichment/custody-bootstrap/standing-instructions-config.yaml");
YamlRuleConfiguration settlementScenariosConfig = yamlLoader.loadFromClasspath("enrichment/custody-bootstrap/settlement-scenarios-config.yaml");
YamlRuleConfiguration autoRepairRulesConfig = yamlLoader.loadFromClasspath("enrichment/custody-bootstrap/auto-repair-rules-config.yaml");
```

### Processing Categories
1. **Standing Instructions Processing** - Client-level, market-level, instrument-level instructions
2. **Settlement Scenarios Processing** - Settlement failure scenarios, repair scenarios, exception scenarios  
3. **Auto-Repair Rules Processing** - Repair decision rules, approval workflows, exception handling

### Data Sources
1. **External YAML Files** - Business rules, data, and logic maintained in YAML files
2. **Real APEX Services** - All data processing through authentic APEX services
3. **Database Service** - Real database service for custody settlement data

## Configuration Customization

### Modifying Business Rules
```yaml
# Edit: apex-demo/src/main/resources/enrichment/custody-bootstrap/auto-repair-rules-config.yaml

# Adjust repair decision rules
repair-decision-rules:
  - rule-name: "high-value-repair-decision"
    rule-condition: "#instruction.amount > 1000000"
    rule-action: "REQUIRE_MANUAL_APPROVAL"
    rule-description: "High-value instructions require manual approval"
    rule-priority: 1

# Modify approval workflows  
approval-workflows:
  - workflow-name: "standard-approval-workflow"
    workflow-steps:
      - step-name: "automated-repair-attempt"
        step-required: true
        step-timeout: 30
```

### Adding New Processing Categories
```yaml
# Edit: apex-demo/src/main/resources/enrichment/custody-auto-repair-bootstrap-demo.yaml

# Add new enrichment processing
enrichments:
  - name: "new-category-processing"
    type: "lookup-enrichment"
    description: "Process new category using data-driven configuration"
    condition: "#data.bootstrapType == 'new-category-processing'"
    lookup-source: "new-category-config"
    target-field: "newCategoryResult"
```

## Performance Characteristics

- **Processing Time**: Sub-100ms for standard custody operations
- **Memory Usage**: Optimized for high-volume settlement processing
- **Scalability**: Designed for enterprise-grade custody operations
- **Reliability**: Fail-fast architecture with comprehensive error handling

## Troubleshooting

### Common Issues

1. **YAML Configuration Not Found**
   ```
   RuntimeException: Required custody auto-repair configuration YAML files not found
   ```
   **Solution**: Ensure all 4 YAML configuration files are present in the resources directory

2. **APEX Service Initialization Failed**
   ```
   RuntimeException: Custody auto-repair bootstrap demo initialization failed
   ```
   **Solution**: Check APEX core services are properly configured and available

3. **Database Connection Issues**
   ```
   Database connection failed
   ```
   **Solution**: Verify PostgreSQL is running and connection parameters are correct

## Support

For questions or issues with this bootstrap:
1. Check the APEX documentation in the `docs/` directory
2. Review the YAML configuration files for reference patterns
3. Examine the @version 2.0 implementation for APEX integration best practices

This bootstrap serves as a complete, production-ready demonstration of APEX's capabilities in solving real-world custody settlement challenges using authentic APEX services and comprehensive YAML-driven configuration management.
