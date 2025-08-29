# APEX PostgreSQL External Lookup Demo

This directory contains everything needed to set up and run PostgreSQL external lookup demonstrations with the APEX Rules Engine.

## 🎯 **Overview**

The PostgreSQL lookup demo showcases:
- **External Database Integration** with connection pooling and performance optimization
- **Simple Single-Parameter Lookups** for customer profile enrichment
- **Complex Multi-Parameter Lookups** with joins for settlement instructions and risk assessment
- **Caching Strategies** for high-performance data access
- **Fallback Mechanisms** for resilience and error handling
- **Real-time Data Enrichment** from PostgreSQL database

## 📋 **Prerequisites**

- Docker and Docker Compose installed
- Java 17+ for running the demo
- APEX Rules Engine configured
- At least 2GB RAM available for PostgreSQL

## 🚀 **Quick Start**

### 1. Start PostgreSQL Database

```bash
# Navigate to the PostgreSQL directory
cd apex-demo/src/main/resources/database/postgresql

# Start PostgreSQL with sample data
docker-compose up -d

# Check database status
docker-compose ps
```

### 2. Verify Database Setup

```bash
# Connect to the database
docker exec -it apex-demo-postgres psql -U apex_user -d apex_demo

# Check sample data
\dt customer_data.*
\dt trading_data.*
\dt reference_data.*

# Exit psql
\q
```

### 3. Run the Demo

```bash
# From the apex-demo root directory
java -cp target/classes dev.mars.apex.demo.examples.PostgreSQLLookupDemo
```

### 4. Access pgAdmin (Optional)

- URL: http://localhost:8080
- Email: admin@apex-demo.com
- Password: admin123

## 📊 **Database Schema**

### Customer Data Schema
```sql
customer_data.customers          -- Customer master data
customer_data.customer_profiles  -- Detailed customer profiles
```

### Trading Data Schema
```sql
trading_data.counterparties         -- Trading counterparties
trading_data.custodians            -- Custodian banks
trading_data.markets               -- Trading markets
trading_data.instruments           -- Financial instruments
trading_data.settlement_instructions -- Settlement routing
trading_data.risk_assessments      -- Risk management data
```

### Reference Data Schema
```sql
reference_data.currencies  -- Currency reference data
reference_data.countries   -- Country and regulatory data
```

## 🔧 **Configuration Files**

### YAML Lookup Configurations
- `postgresql-simple-lookup.yaml` - Single-parameter customer lookup
- `postgresql-multi-param-lookup.yaml` - Multi-parameter settlement lookup

### Database Setup
- `docker-compose.yml` - PostgreSQL container configuration
- `postgresql.conf` - Database performance tuning
- `init/01-create-schemas.sql` - Database schema creation
- `init/02-insert-sample-data.sql` - Sample data insertion

## 📈 **Demo Scenarios**

### 1. Simple Database Lookup
**Use Case**: Customer profile enrichment
- **Input**: Customer ID
- **Output**: Customer name, type, tier, region, status
- **Features**: Connection pooling, caching, fallback values

### 2. Multi-Parameter Database Lookup
**Use Case**: Settlement instruction routing
- **Input**: Counterparty, instrument type, currency, market, amount
- **Output**: Settlement instructions, custodian details, risk assessment
- **Features**: Complex SQL joins, cascade fallback, performance optimization

### 3. Performance Optimization
**Use Case**: High-volume lookup processing
- **Features**: Connection pooling, multi-level caching, concurrent processing
- **Metrics**: Lookup times, cache hit rates, connection pool utilization

### 4. Fallback Strategies
**Use Case**: Error handling and resilience
- **Features**: Default values, cascade queries, graceful degradation
- **Scenarios**: Missing data, database unavailability, timeout handling

## 🎛️ **Configuration Options**

### Connection Pool Settings
```yaml
connection-pool:
  max-pool-size: 20        # Maximum connections
  min-pool-size: 5         # Minimum connections
  connection-timeout: 30000 # Connection timeout (ms)
  idle-timeout: 600000     # Idle timeout (ms)
  max-lifetime: 1800000    # Maximum connection lifetime (ms)
```

### Caching Configuration
```yaml
cache:
  enabled: true
  ttl-seconds: 3600        # Cache TTL (1 hour)
  max-size: 10000          # Maximum cache entries
  partitions:              # Cache partitioning
    settlement-instructions:
      ttl-seconds: 1800    # 30 minutes
      max-size: 20000
```

### Performance Monitoring
```yaml
monitoring:
  metrics:
    enabled: true
    include-query-timing: true
    include-connection-pool-stats: true
    include-cache-stats: true
  alerts:
    slow-query-threshold-ms: 2000
    connection-pool-exhaustion-threshold: 0.9
    cache-miss-rate-threshold: 0.5
```

## 🔍 **Sample Data**

### Customers (10 records)
- CUST000001: Acme Corporation (PLATINUM, NA)
- CUST000002: Global Investment Partners (GOLD, EU)
- CUST000003: Pacific Asset Management (GOLD, APAC)
- ... and more

### Counterparties (10 major banks)
- CP_GS: Goldman Sachs (A+, $500M limit)
- CP_JPM: JP Morgan (AA-, $750M limit)
- CP_DB: Deutsche Bank (A-, $300M limit)
- ... and more

### Settlement Instructions (20+ routing rules)
- Multi-parameter matching on counterparty, instrument, currency, market
- Fallback strategies with DEFAULT counterparty
- Priority-based selection

## 🛠️ **Troubleshooting**

### Database Connection Issues
```bash
# Check if PostgreSQL is running
docker-compose ps

# View PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Performance Issues
```bash
# Check connection pool status
docker exec -it apex-demo-postgres psql -U apex_user -d apex_demo -c "SELECT * FROM pg_stat_activity;"

# Check slow queries
docker exec -it apex-demo-postgres psql -U apex_user -d apex_demo -c "SELECT query, mean_time, calls FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"
```

### Data Issues
```bash
# Verify sample data
docker exec -it apex-demo-postgres psql -U apex_user -d apex_demo -c "SELECT COUNT(*) FROM customer_data.customers;"
docker exec -it apex-demo-postgres psql -U apex_user -d apex_demo -c "SELECT COUNT(*) FROM trading_data.settlement_instructions;"

# Reset sample data
docker-compose down -v
docker-compose up -d
```

## 🧹 **Cleanup**

```bash
# Stop and remove containers
docker-compose down

# Remove all data (including volumes)
docker-compose down -v

# Remove images
docker-compose down --rmi all -v
```

## 📚 **Additional Resources**

- [APEX Data Management Guide](../../docs/APEX_DATA_MANAGEMENT_GUIDE.md)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Docker Compose Reference](https://docs.docker.com/compose/)

## 🤝 **Support**

For issues or questions:
1. Check the troubleshooting section above
2. Review PostgreSQL logs: `docker-compose logs postgres`
3. Verify configuration files match your environment
4. Ensure all prerequisites are met
