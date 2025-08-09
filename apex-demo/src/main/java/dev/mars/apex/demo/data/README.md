# APEX Demo Data Service Managers

## Overview

This package contains data service managers for APEX demonstrations, including both legacy and production-ready implementations.

## Classes

### ProductionDemoDataServiceManager ✅ **RECOMMENDED**

**Purpose**: Production-ready demo implementation using ExternalDataSource implementations.

**Features**:
- 🚀 Uses production ExternalDataSource implementations (FileSystemDataSource, CacheDataSource)
- 📁 Automatically creates demo data files (JSON format)
- ⚡ High-performance caching with configurable TTL
- 📊 Health monitoring and metrics collection
- 🛡️ Proper error handling and recovery
- 🔧 Configuration-driven setup

**Usage**:
```java
// RECOMMENDED APPROACH
ProductionDemoDataServiceManager manager = new ProductionDemoDataServiceManager();
manager.initializeWithMockData(); // Uses production data sources with demo data

// Request data
List<Product> products = manager.requestData("products");
Customer customer = manager.requestData("customer");
```

**Data Sources Created**:
- **FileSystemDataSource**: Processes JSON files from `demo-data/json/` directory
- **CacheDataSource**: High-performance in-memory caching with LRU eviction

**Demo Data Files**:
- `demo-data/json/products.json` - Product catalog data
- `demo-data/json/customers.json` - Customer information
- `demo-data/json/inventory.json` - Inventory levels

### DemoDataServiceManager ⚠️ **DEPRECATED**

**Purpose**: Legacy demo implementation using deprecated MockDataSource.

**Status**: 
- ❌ **DEPRECATED** - Use ProductionDemoDataServiceManager instead
- 🔒 Maintained for backward compatibility only
- ⚠️ Uses deprecated MockDataSource with hardcoded data
- 🚫 Not suitable for realistic demonstrations

**Migration**:
```java
// OLD (Deprecated)
DemoDataServiceManager oldManager = new DemoDataServiceManager();
oldManager.initializeWithMockData();

// NEW (Recommended)
ProductionDemoDataServiceManager newManager = new ProductionDemoDataServiceManager();
newManager.initializeWithMockData();
```

## Migration Guide

### Why Migrate?

1. **Production Readiness**: ProductionDemoDataServiceManager uses real ExternalDataSource implementations
2. **Better Performance**: Includes caching, connection pooling, and optimization features
3. **Realistic Demos**: Shows actual production capabilities rather than mock behavior
4. **Future-Proof**: Aligned with APEX architecture best practices

### Migration Steps

1. **Replace Class Usage**:
   ```java
   // Change this:
   DemoDataServiceManager manager = new DemoDataServiceManager();
   
   // To this:
   ProductionDemoDataServiceManager manager = new ProductionDemoDataServiceManager();
   ```

2. **Update Imports**:
   ```java
   // Change this:
   import dev.mars.apex.demo.data.DemoDataServiceManager;
   
   // To this:
   import dev.mars.apex.demo.data.ProductionDemoDataServiceManager;
   ```

3. **Test Your Code**: The API is identical, but data sources behave more realistically

### Compatibility

- ✅ **API Compatible**: Same method signatures and return types
- ✅ **Data Compatible**: Same data types and structures
- ⚡ **Performance Improved**: Better caching and optimization
- 🔧 **Configuration Enhanced**: More flexible and configurable

## Configuration

### FileSystemDataSource Configuration
- **Base Path**: `demo-data/json/`
- **File Pattern**: `*.json`
- **Encoding**: UTF-8
- **Auto-reload**: Enabled
- **Cache TTL**: 5 minutes

### CacheDataSource Configuration
- **Max Size**: 1000 entries
- **TTL**: 1 hour
- **Eviction Policy**: LRU (Least Recently Used)
- **Statistics**: Enabled

## Troubleshooting

### Common Issues

1. **Demo data files not found**:
   - Files are created automatically on first run
   - Check `demo-data/json/` directory exists
   - Verify write permissions

2. **Performance concerns**:
   - Caching is enabled by default
   - File watching is disabled to avoid overhead
   - Adjust cache settings if needed

3. **Migration warnings**:
   - Deprecation warnings are expected for DemoDataServiceManager
   - Update to ProductionDemoDataServiceManager to resolve

### Support

For questions or issues:
1. Check the [APEX Data Source Architecture Guide](../../../docs/APEX_DATA_SOURCE_ARCHITECTURE_GUIDE.md)
2. Review [APEX External Data Sources Guide](../../../docs/APEX_EXTERNAL_DATA_SOURCES_GUIDE.md)
3. See [APEX Data Management Guide](../../../docs/APEX_DATA_MANAGEMENT_GUIDE.md)

## Examples

### Basic Usage
```java
ProductionDemoDataServiceManager manager = new ProductionDemoDataServiceManager();
manager.initializeWithMockData();

// Get all products
List<Product> products = manager.requestData("products");
System.out.println("Found " + products.size() + " products");

// Get customer data
Customer customer = manager.requestData("customer");
System.out.println("Customer: " + customer.getName());
```

### Advanced Usage
```java
ProductionDemoDataServiceManager manager = new ProductionDemoDataServiceManager();
manager.initializeWithMockData();

// Check data source health
DataSource fileSource = manager.getDataSourceByName("ProductionDemoFileSource");
if (fileSource instanceof ExternalDataSource) {
    ExternalDataSource extSource = (ExternalDataSource) fileSource;
    HealthStatus health = extSource.getHealthStatus();
    System.out.println("File source health: " + health.getStatus());
}

// Get performance metrics
DataSource cacheSource = manager.getDataSourceByName("ProductionDemoCacheSource");
if (cacheSource instanceof ExternalDataSource) {
    ExternalDataSource extSource = (ExternalDataSource) cacheSource;
    PerformanceMetrics metrics = extSource.getPerformanceMetrics();
    System.out.println("Cache hit rate: " + metrics.getCacheHitRate());
}
```
