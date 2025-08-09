# APEX Data Source Architecture Migration

## 🚨 Important Changes

This document explains the recent migration from `MockDataSource` to production data source implementations in the APEX demo module.

## What Changed?

### Before (Problematic)
- `MockDataSource` was used in main demo code as if it were a production implementation
- Demos suggested that mocks were the primary data source approach
- Confusion between testing utilities and production capabilities

### After (Fixed)
- `MockDataSource` moved to test packages (`src/test/`) for testing only
- New `ProductionDemoDataServiceManager` uses real `ExternalDataSource` implementations
- Clear separation between testing utilities and production data sources
- Proper YAML configuration for data sources

## 📁 File Changes

### Moved Files
- **From**: `apex-demo/src/main/java/dev/mars/apex/demo/rulesets/MockDataSource.java`
- **To**: `apex-demo/src/test/java/dev/mars/apex/demo/test/data/MockDataSource.java`

### New Files
- `apex-demo/src/main/java/dev/mars/apex/demo/data/ProductionDemoDataServiceManager.java`
- `apex-demo/src/main/java/dev/mars/apex/demo/examples/ProductionBatchProcessingDemo.java`
- `apex-demo/src/main/resources/config/demo-data-sources.yaml`
- `apex-demo/src/main/resources/config/production-demo-config.yaml`
- `docs/APEX_DATA_SOURCE_ARCHITECTURE_GUIDE.md`

### Updated Files
- `apex-demo/src/main/java/dev/mars/apex/demo/data/DemoDataServiceManager.java` (now delegates to production implementation)
- `apex-demo/src/main/java/dev/mars/apex/demo/examples/FileBasedProcessingDemo.java` (updated with production concepts)
- `docs/APEX_DATA_MANAGEMENT_GUIDE.md` (clarified MockDataSource usage)

## 🏗️ New Architecture

### Testing (MockDataSource)
```java
// Location: src/test/java/dev/mars/apex/demo/test/data/MockDataSource.java
// Usage: Unit tests only

@Test
public void testRuleExecution() {
    MockDataSource mockSource = new MockDataSource("test-source", "products");
    // ... test logic
}
```

### Production Demos (ExternalDataSource)
```java
// Location: src/main/java/dev/mars/apex/demo/data/ProductionDemoDataServiceManager.java
// Usage: Realistic demos and production

FileSystemDataSource fileSource = new FileSystemDataSource();
DataSourceConfiguration config = createFileConfig("demo-data/", "*.json");
fileSource.initialize(config);

CacheDataSource cacheSource = new CacheDataSource();
DataSourceConfiguration cacheConfig = createCacheConfig(1000, 3600);
cacheSource.initialize(cacheConfig);
```

## 🔧 Configuration Examples

### File System Data Source
```yaml
# demo-data-sources.yaml
data-sources:
  - name: "demo-json-files"
    type: "FILE_SYSTEM"
    enabled: true
    connection:
      base-path: "demo-data/json"
      file-pattern: "*.json"
    properties:
      auto-reload: true
      cache-parsed-files: true
```

### Cache Data Source
```yaml
data-sources:
  - name: "demo-products-cache"
    type: "CACHE"
    enabled: true
    properties:
      max-size: 1000
      ttl-seconds: 3600
      eviction-policy: "LRU"
```

## 🎯 Migration Guide

### If You Were Using MockDataSource in Demos

**Before**:
```java
// DON'T DO THIS ANYMORE
DemoDataServiceManager manager = new DemoDataServiceManager();
manager.initializeWithMockData(); // Uses MockDataSource internally
```

**After**:
```java
// DO THIS INSTEAD
ProductionDemoDataServiceManager manager = new ProductionDemoDataServiceManager();
manager.initializeWithMockData(); // Uses production ExternalDataSource implementations
```

### If You Were Using MockDataSource in Tests

**Before**:
```java
// This still works, but import path changed
import dev.mars.apex.demo.rulesets.MockDataSource; // OLD PATH
```

**After**:
```java
// Update import path
import dev.mars.apex.demo.test.data.MockDataSource; // NEW PATH
```

## 🚀 Benefits of New Architecture

### 1. Clear Separation
- **Testing**: MockDataSource for unit tests
- **Demos**: Production ExternalDataSource implementations
- **Production**: Same ExternalDataSource implementations with real configurations

### 2. Realistic Demonstrations
- Demos now show actual production patterns
- Proper configuration management
- Real error handling and monitoring

### 3. Better Learning Experience
- Users see how to configure real data sources
- Examples show production-ready patterns
- Clear path from demo to production

### 4. Maintainability
- Reduced confusion about data source purposes
- Cleaner separation of concerns
- Better documentation and examples

## 📚 Available Data Source Types

### Production ExternalDataSource Implementations
1. **DatabaseDataSource** - JDBC database connectivity
2. **FileSystemDataSource** - File processing (CSV, JSON, XML)
3. **RestApiDataSource** - REST API integration
4. **CacheDataSource** - High-performance caching

### Configuration-Driven Setup
All production data sources support:
- YAML-based configuration
- Health monitoring
- Metrics collection
- Connection pooling
- Error handling and recovery

## 🔍 How to Identify Usage

### MockDataSource (Testing Only)
- Import path: `dev.mars.apex.demo.test.data.MockDataSource`
- Location: `src/test/` directories
- Purpose: Unit testing only

### ExternalDataSource (Production)
- Import path: `dev.mars.apex.core.service.data.external.*`
- Location: `src/main/` directories
- Purpose: Production and realistic demos

## 📖 Documentation

- [APEX Data Source Architecture Guide](../docs/APEX_DATA_SOURCE_ARCHITECTURE_GUIDE.md)
- [APEX External Data Sources Guide](../docs/APEX_EXTERNAL_DATA_SOURCES_GUIDE.md)
- [APEX Data Management Guide](../docs/APEX_DATA_MANAGEMENT_GUIDE.md)

## ❓ FAQ

### Q: Can I still use MockDataSource?
**A**: Yes, but only in unit tests. Import from the new test package location.

### Q: What should I use for demos?
**A**: Use `ProductionDemoDataServiceManager` and the production `ExternalDataSource` implementations.

### Q: Will my existing tests break?
**A**: You may need to update import statements to use the new test package location.

### Q: How do I configure production data sources?
**A**: Use YAML configuration files. See examples in `src/main/resources/config/`.

### Q: What's the performance difference?
**A**: Production data sources include caching, connection pooling, and other optimizations that MockDataSource lacks.

## 🎉 Summary

This migration provides:
- ✅ Clear separation between testing and production data sources
- ✅ Realistic demo scenarios using production implementations
- ✅ Proper configuration management
- ✅ Better learning experience for users
- ✅ Cleaner architecture and maintainability

The APEX Rules Engine now demonstrates proper data source architecture patterns that users can confidently apply in their own production environments.
