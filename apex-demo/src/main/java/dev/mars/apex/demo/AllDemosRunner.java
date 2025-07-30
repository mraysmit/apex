package dev.mars.apex.demo;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AllDemosRunner - Automatically discovers and runs all demo classes.
 * 
 * This runner scans the demo package structure and automatically executes all demos
 * without requiring any interface implementation or ceremony. It works with demos that have:
 * - main(String[] args) methods
 * - run() methods (no parameters)
 * - Both (prioritizes run() for consistency)
 * 
 * Usage:
 * - java AllDemosRunner                # Run all demos
 * - java AllDemosRunner --list         # List all discovered demos
 * - java AllDemosRunner --package core # Run demos from specific package
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class AllDemosRunner {
    
    private static final String DEMO_BASE_PACKAGE = "dev.mars.apex.demo";
    private static final String[] EXCLUDED_CLASSES = {
        "AllDemosRunner", "DemoRunner", "DemoFramework", "Demo", "DemoCategory"
    };
    
    final List<DemoInfo> discoveredDemos = new ArrayList<>();
    private int successCount = 0;
    private int failureCount = 0;
    private int skippedCount = 0;
    
    public static void main(String[] args) {
        System.out.println("=== SpEL Rules Engine - All Demos Runner ===");
        System.out.println("Automatically discovering and running all available demos...\n");
        
        AllDemosRunner runner = new AllDemosRunner();
        
        if (args.length > 0) {
            if ("--list".equals(args[0])) {
                runner.listAllDemos();
                return;
            } else if ("--package".equals(args[0]) && args.length > 1) {
                runner.runDemosFromPackage(args[1]);
                return;
            }
        }
        
        runner.runAllDemos();
    }
    
    /**
     * Discover all demo classes in the demo package structure.
     */
    public void discoverDemos() {
        discoveredDemos.clear();

        try {
            // Manually register known demo classes to avoid reflection issues
            registerKnownDemoClasses();

            // Sort demos by package and class name for consistent execution order
            discoveredDemos.sort(Comparator.comparing(DemoInfo::getPackageName)
                    .thenComparing(DemoInfo::getClassName));

        } catch (Exception e) {
            System.err.println("Error discovering demos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Register known demo classes manually to avoid reflection issues.
     */
    private void registerKnownDemoClasses() {
        // Core demos
        registerDemoClass("dev.mars.apex.demo.core.QuickStartDemo");
        registerDemoClass("dev.mars.apex.demo.core.LayeredAPIDemo");

        // Examples demos
        registerDemoClass("dev.mars.apex.demo.examples.YamlDatasetDemo");
        registerDemoClass("dev.mars.apex.demo.examples.FinancialServicesDemo");
        registerDemoClass("dev.mars.apex.demo.examples.PerformanceDemo");
        registerDemoClass("dev.mars.apex.demo.examples.SimplifiedAPIDemo");
        registerDemoClass("dev.mars.apex.demo.examples.YamlConfigurationDemo");
        registerDemoClass("dev.mars.apex.demo.examples.BatchProcessingDemo");

        // Examples demos
        registerDemoClass("dev.mars.apex.demo.examples.BasicUsageExamples");
        registerDemoClass("dev.mars.apex.demo.examples.LayeredAPIDemo");
        registerDemoClass("dev.mars.apex.demo.examples.AdvancedFeaturesDemo");
        registerDemoClass("dev.mars.apex.demo.examples.CommoditySwapValidationDemo");
        registerDemoClass("dev.mars.apex.demo.examples.FinancialTradingDemo");
        registerDemoClass("dev.mars.apex.demo.examples.PerformanceMonitoringDemo");
        registerDemoClass("dev.mars.apex.demo.examples.SpelRulesEngineDemo");
        registerDemoClass("dev.mars.apex.demo.examples.CustodyAutoRepairDemo");
        registerDemoClass("dev.mars.apex.demo.examples.CustodyAutoRepairStandaloneDemo");
        registerDemoClass("dev.mars.apex.demo.examples.CustodyAutoRepairYamlDemo");
        registerDemoClass("dev.mars.apex.demo.examples.FluentRuleBuilderExample");

        // Advanced demos
        registerDemoClass("dev.mars.apex.demo.advanced.DataServiceManagerDemo");
        registerDemoClass("dev.mars.apex.demo.advanced.DynamicMethodExecutionDemo");
        registerDemoClass("dev.mars.apex.demo.advanced.PerformanceAndExceptionDemo");
        registerDemoClass("dev.mars.apex.demo.advanced.SpelAdvancedFeaturesDemo");
    }

    /**
     * Register a single demo class by name.
     */
    private void registerDemoClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!shouldSkipClass(clazz)) {
                DemoInfo demoInfo = analyzeDemoClass(clazz);
                if (demoInfo != null) {
                    discoveredDemos.add(demoInfo);
                }
            }
        } catch (ClassNotFoundException e) {
            // Skip classes that don't exist (silently)
        } catch (Exception e) {
            System.err.println("Error registering demo class " + className + ": " + e.getMessage());
        }
    }
    
    /**
     * Find all classes in the demo package structure.
     */
    private Set<Class<?>> findDemoClasses() throws Exception {
        Set<Class<?>> classes = new HashSet<>();
        String packagePath = DEMO_BASE_PACKAGE.replace('.', '/');

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(packagePath);

        if (resource != null) {
            File directory = new File(resource.getFile());
            if (directory.exists()) {
                findClassesInDirectory(directory, DEMO_BASE_PACKAGE, classes);
            }
        }
        return classes;
    }
    
    /**
     * Recursively find classes in directory structure.
     */
    private void findClassesInDirectory(File directory, String packageName, Set<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                findClassesInDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // Skip classes that can't be loaded
                }
            }
        }
    }
    
    /**
     * Check if a class should be skipped.
     */
    private boolean shouldSkipClass(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        
        // Skip excluded classes
        for (String excluded : EXCLUDED_CLASSES) {
            if (simpleName.equals(excluded)) {
                return true;
            }
        }
        
        // Skip abstract classes, interfaces, enums
        if (clazz.isInterface() || clazz.isEnum() || 
            java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
            return true;
        }
        
        // Skip inner classes and utility classes
        if (clazz.isMemberClass() || simpleName.contains("$")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Analyze a demo class to determine how it can be executed.
     */
    private DemoInfo analyzeDemoClass(Class<?> clazz) {
        Method mainMethod = null;
        Method runMethod = null;
        
        try {
            // Look for main method
            mainMethod = clazz.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            // No main method
        }
        
        try {
            // Look for run method (no parameters)
            runMethod = clazz.getMethod("run");
        } catch (NoSuchMethodException e) {
            // No run method
        }
        
        if (mainMethod == null && runMethod == null) {
            return null; // Not a runnable demo
        }
        
        return new DemoInfo(clazz, mainMethod, runMethod);
    }
    
    /**
     * Run all discovered demos.
     */
    public void runAllDemos() {
        discoverDemos();
        
        if (discoveredDemos.isEmpty()) {
            System.out.println("No runnable demos found.");
            return;
        }
        
        System.out.println("Discovered " + discoveredDemos.size() + " runnable demos:");
        for (DemoInfo demo : discoveredDemos) {
            System.out.println("  - " + demo.getFullName() + " (" + demo.getExecutionType() + ")");
        }
        System.out.println();
        
        // Group demos by package for organized execution
        Map<String, List<DemoInfo>> demosByPackage = discoveredDemos.stream()
                .collect(Collectors.groupingBy(DemoInfo::getPackageName));
        
        for (Map.Entry<String, List<DemoInfo>> entry : demosByPackage.entrySet()) {
            String packageName = entry.getKey();
            List<DemoInfo> packageDemos = entry.getValue();
            
            System.out.println("═".repeat(80));
            System.out.println("PACKAGE: " + packageName.toUpperCase());
            System.out.println("═".repeat(80));
            
            for (DemoInfo demo : packageDemos) {
                runSingleDemo(demo);
                System.out.println();
            }
        }
        
        printSummary();
    }
    
    /**
     * Run demos from a specific package.
     */
    public void runDemosFromPackage(String packageSuffix) {
        discoverDemos();
        
        List<DemoInfo> filteredDemos = discoveredDemos.stream()
                .filter(demo -> demo.getPackageName().contains(packageSuffix))
                .collect(Collectors.toList());
        
        if (filteredDemos.isEmpty()) {
            System.out.println("No demos found in package containing: " + packageSuffix);
            return;
        }
        
        System.out.println("Running " + filteredDemos.size() + " demos from package: " + packageSuffix);
        System.out.println();
        
        for (DemoInfo demo : filteredDemos) {
            runSingleDemo(demo);
            System.out.println();
        }
        
        printSummary();
    }
    
    /**
     * List all discovered demos without running them.
     */
    public void listAllDemos() {
        discoverDemos();
        
        if (discoveredDemos.isEmpty()) {
            System.out.println("No runnable demos found.");
            return;
        }
        
        System.out.println("Discovered " + discoveredDemos.size() + " runnable demos:\n");
        
        Map<String, List<DemoInfo>> demosByPackage = discoveredDemos.stream()
                .collect(Collectors.groupingBy(DemoInfo::getPackageName));
        
        for (Map.Entry<String, List<DemoInfo>> entry : demosByPackage.entrySet()) {
            System.out.println(entry.getKey() + ":");
            for (DemoInfo demo : entry.getValue()) {
                System.out.println("  - " + demo.getClassName() + " (" + demo.getExecutionType() + ")");
            }
            System.out.println();
        }
    }
    
    /**
     * Run a single demo with error handling.
     */
    void runSingleDemo(DemoInfo demo) {
        System.out.println("▶ Running: " + demo.getFullName());
        System.out.println("  Method: " + demo.getExecutionType());
        System.out.println("  " + "-".repeat(60));
        
        long startTime = System.currentTimeMillis();
        
        try {
            if (demo.hasRunMethod()) {
                // Prefer run() method for consistency
                Object instance = demo.getDemoClass().getDeclaredConstructor().newInstance();
                demo.getRunMethod().invoke(instance);
            } else if (demo.hasMainMethod()) {
                // Fall back to main method
                demo.getMainMethod().invoke(null, (Object) new String[0]);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("  " + "-".repeat(60));
            System.out.println("COMPLETED: " + demo.getClassName() + " (" + duration + "ms)");
            successCount++;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("  " + "-".repeat(60));
            System.out.println("FAILED: " + demo.getClassName() + " (" + duration + "ms)");
            System.out.println("   Error: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("   Cause: " + e.getCause().getMessage());
            }
            failureCount++;
        }
    }
    
    /**
     * Print execution summary.
     */
    private void printSummary() {
        System.out.println("═".repeat(80));
        System.out.println("EXECUTION SUMMARY");
        System.out.println("═".repeat(80));
        System.out.println("Total Demos: " + discoveredDemos.size());
        System.out.println("Successful: " + successCount);
        System.out.println("Failed: " + failureCount);
        System.out.println("Skipped: " + skippedCount);
        System.out.println();
        
        if (failureCount == 0) {
            System.out.println("All demos completed successfully!");
        } else {
            System.out.println(" Some demos failed. Check the output above for details.");
        }
    }
    
    /**
     * Information about a discovered demo class.
     */
    static class DemoInfo {
        private final Class<?> demoClass;
        private final Method mainMethod;
        private final Method runMethod;
        
        public DemoInfo(Class<?> demoClass, Method mainMethod, Method runMethod) {
            this.demoClass = demoClass;
            this.mainMethod = mainMethod;
            this.runMethod = runMethod;
        }
        
        public Class<?> getDemoClass() { return demoClass; }
        public Method getMainMethod() { return mainMethod; }
        public Method getRunMethod() { return runMethod; }
        
        public boolean hasMainMethod() { return mainMethod != null; }
        public boolean hasRunMethod() { return runMethod != null; }
        
        public String getClassName() { return demoClass.getSimpleName(); }
        public String getFullName() { return demoClass.getName(); }
        
        public String getPackageName() {
            String packageName = demoClass.getPackage().getName();
            return packageName.substring(packageName.lastIndexOf('.') + 1);
        }
        
        public String getExecutionType() {
            if (hasRunMethod()) return "run()";
            if (hasMainMethod()) return "main()";
            return "unknown";
        }
    }
}
