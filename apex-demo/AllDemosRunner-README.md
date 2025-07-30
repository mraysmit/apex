# AllDemosRunner - Automatic Demo Discovery and Execution

The `AllDemosRunner` is a simple, reflection-free demo runner that automatically discovers and executes all available demo classes in the SpEL Rules Engine demo module without requiring any interface implementation or ceremony.

## Features

- **Auto-discovery**: Automatically finds all runnable demo classes
- **No interfaces required**: Demos don't need to implement any interface
- **Self-contained execution**: Each demo runs independently with proper error handling
- **Flexible execution methods**: Works with demos that have `main()` methods, `run()` methods, or both
- **Package filtering**: Can run demos from specific packages
- **Clear reporting**: Provides execution summary with success/failure counts

## Usage

### Run All Demos
```bash
java dev.mars.rulesengine.demo.AllDemosRunner
```

### List All Available Demos
```bash
java dev.mars.rulesengine.demo.AllDemosRunner --list
```

### Run Demos from Specific Package
```bash
java dev.mars.apex.demo.AllDemosRunner --package core
java dev.mars.apex.demo.AllDemosRunner --package examples
java dev.mars.apex.demo.AllDemosRunner --package advanced
```

## Demo Discovery

The runner automatically discovers demos by looking for classes with:
- A `main(String[] args)` method
- A `run()` method (no parameters)
- Both (prioritizes `run()` for consistency)

Currently discovered demos include:

### Core Demos (8 demos)
- `QuickStartDemo` - 5-minute introduction to the rules engine
- `YamlDatasetDemo` - YAML dataset enrichment capabilities
- `FinancialServicesDemo` - Financial domain demonstrations
- `PerformanceDemo` - Performance monitoring features
- `LayeredAPIDemo` - Three-layer API design
- `SimplifiedAPIDemo` - Simplified API usage
- `YamlConfigurationDemo` - YAML configuration examples
- `BatchProcessingDemo` - Batch processing capabilities

### Examples Demos (6 demos)
- `BasicUsageExamples` - Fundamental concepts and operations
- `LayeredAPIDemo` - API layer demonstrations
- `AdvancedFeaturesDemo` - Advanced SpEL features
- `CommoditySwapValidationDemo` - Financial instrument validation
- `FinancialTradingDemo` - Trading system examples
- `PerformanceMonitoringDemo` - Performance monitoring

### Integration Demos (4 demos)
- `DataServiceManagerDemo` - Data service integration
- `DynamicMethodExecutionDemo` - Dynamic method execution
- `PerformanceAndExceptionDemo` - Error handling and monitoring
- `SpelAdvancedFeaturesDemo` - Advanced SpEL capabilities

### Financial Demos (1 demo)
- `CommoditySwapValidationDemo` - OTC commodity swap validation

## Output

The runner provides:
- Clear execution progress with package organization
- Individual demo status (success/failure with timing)
- Comprehensive execution summary
- Error details for failed demos

Example output:
```
=== SpEL Rules Engine - All Demos Runner ===
Automatically discovering and running all available demos...

Discovered 19 runnable demos:
  - dev.mars.rulesengine.demo.examples.QuickStartDemo (run())
  - dev.mars.rulesengine.demo.examples.BasicUsageExamples (run())
  ...

════════════════════════════════════════════════════════════════════════════════
PACKAGE: CORE
════════════════════════════════════════════════════════════════════════════════

▶ Running: dev.mars.rulesengine.demo.examples.QuickStartDemo
  Method: run()
  ------------------------------------------------------------
  [Demo output...]
  ------------------------------------------------------------
✅ COMPLETED: QuickStartDemo (1250ms)

════════════════════════════════════════════════════════════════════════════════
EXECUTION SUMMARY
════════════════════════════════════════════════════════════════════════════════
Total Demos: 19
Successful: 19
Failed: 0
Skipped: 0

🎉 All demos completed successfully!
```

## Error Handling

- Each demo runs in isolation - one failing demo doesn't stop the entire suite
- Clear error reporting with exception details
- Graceful handling of missing or broken demo classes
- Execution timing for performance analysis

## Adding New Demos

To add a new demo to the runner:

1. Create your demo class with either:
   - A `public static void main(String[] args)` method
   - A `public void run()` method (no parameters)
   - Both methods (runner will prefer `run()`)

2. Add the class name to the `registerKnownDemoClasses()` method in `AllDemosRunner.java`

3. The demo will be automatically discovered and executed

## Testing

The AllDemosRunner includes comprehensive tests:
- `AllDemosRunnerTest` - Unit tests for discovery and basic functionality
- `AllDemosRunnerIntegrationTest` - Integration tests that actually run demos

Run tests with:
```bash
mvn test -Dtest=AllDemosRunnerTest
mvn test -Dtest=AllDemosRunnerIntegrationTest
```

## Benefits

- **No ceremony**: Demos don't need to implement interfaces or follow complex patterns
- **Self-contained**: Each demo is independent and can be run individually
- **Automatic**: New demos are easily added without modifying runner logic
- **Robust**: Proper error handling ensures reliable execution
- **Informative**: Clear output and reporting for easy debugging
