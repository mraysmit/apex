package dev.mars.rulesengine.demo.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Unified framework for managing and executing demos.
 * This provides a consistent interface for discovering, organizing, and running demos.
 */
public class DemoFramework {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoFramework.class);
    
    private final List<Demo> availableDemos;
    private final Scanner scanner;
    
    public DemoFramework() {
        this.availableDemos = new ArrayList<>();
        this.scanner = new Scanner(System.in);
        discoverDemos();
    }
    
    /**
     * Discover and register all available demos.
     */
    private void discoverDemos() {
        // This will be populated as we migrate existing demos
        LOGGER.info("Discovering available demos...");
        
        // For now, we'll manually register demos as we migrate them
        // In the future, this could use reflection or annotation scanning
        
        LOGGER.info("Found {} demos", availableDemos.size());
    }
    
    /**
     * Register a demo with the framework.
     * @param demo The demo to register
     */
    public void registerDemo(Demo demo) {
        if (demo != null && !availableDemos.contains(demo)) {
            availableDemos.add(demo);
            LOGGER.debug("Registered demo: {}", demo.getName());
        }
    }
    
    /**
     * Get all available demos.
     * @return List of all registered demos
     */
    public List<Demo> getAvailableDemos() {
        return new ArrayList<>(availableDemos);
    }
    
    /**
     * Get demos by category.
     * @param category The category to filter by
     * @return List of demos in the specified category
     */
    public List<Demo> getDemosByCategory(DemoCategory category) {
        return availableDemos.stream()
                .filter(demo -> demo.getCategory() == category)
                .sorted(Comparator.comparing(Demo::getName))
                .collect(Collectors.toList());
    }
    
    /**
     * Find a demo by name.
     * @param name The name of the demo to find
     * @return The demo if found, null otherwise
     */
    public Demo findDemoByName(String name) {
        return availableDemos.stream()
                .filter(demo -> demo.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Run the interactive demo menu.
     */
    public void runInteractiveMenu() {
        printBanner();
        
        boolean running = true;
        while (running) {
            printMainMenu();
            
            try {
                String input = scanner.nextLine().trim();
                
                if ("0".equals(input) || "exit".equalsIgnoreCase(input)) {
                    running = false;
                    printGoodbye();
                } else if ("list".equalsIgnoreCase(input)) {
                    listAllDemos();
                } else if ("categories".equalsIgnoreCase(input)) {
                    showCategoriesMenu();
                } else if (input.matches("\\d+")) {
                    int choice = Integer.parseInt(input);
                    runDemoByIndex(choice);
                } else {
                    // Try to find demo by name
                    Demo demo = findDemoByName(input);
                    if (demo != null) {
                        runDemo(demo);
                    } else {
                        System.out.println("❌ Unknown command or demo: " + input);
                        System.out.println("💡 Type 'list' to see all demos, 'categories' to browse by category, or '0' to exit");
                    }
                }
                
                if (running) {
                    System.out.println("\n" + "─".repeat(80));
                    System.out.print("Press Enter to continue...");
                    scanner.nextLine();
                }
                
            } catch (Exception e) {
                LOGGER.error("Error in interactive menu", e);
                System.out.println("❌ An error occurred: " + e.getMessage());
            }
        }
    }
    
    /**
     * Run a specific demo by name in non-interactive mode.
     * @param demoName The name of the demo to run
     */
    public void runDemo(String demoName) {
        Demo demo = findDemoByName(demoName);
        if (demo != null) {
            runDemo(demo);
        } else {
            System.out.println("❌ Demo not found: " + demoName);
            listAvailableDemoNames();
        }
    }
    
    /**
     * Run all demos in non-interactive mode.
     */
    public void runAllDemos() {
        System.out.println("🔄 Running all available demos...\n");
        
        Map<DemoCategory, List<Demo>> demosByCategory = availableDemos.stream()
                .collect(Collectors.groupingBy(Demo::getCategory));
        
        for (DemoCategory category : DemoCategory.values()) {
            List<Demo> categoryDemos = demosByCategory.get(category);
            if (categoryDemos != null && !categoryDemos.isEmpty()) {
                System.out.println("═".repeat(80));
                System.out.println(category.toString().toUpperCase());
                System.out.println("═".repeat(80));
                
                for (Demo demo : categoryDemos) {
                    runDemo(demo);
                    System.out.println();
                }
            }
        }
        
        System.out.println("✅ All demos completed!");
    }
    
    /**
     * Generate a report of all available demos.
     * @return Demo report as a formatted string
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("# Rules Engine Demo Report\n\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        
        Map<DemoCategory, List<Demo>> demosByCategory = availableDemos.stream()
                .collect(Collectors.groupingBy(Demo::getCategory));
        
        for (DemoCategory category : DemoCategory.values()) {
            List<Demo> categoryDemos = demosByCategory.get(category);
            if (categoryDemos != null && !categoryDemos.isEmpty()) {
                report.append("## ").append(category.getDisplayName()).append("\n\n");
                report.append(category.getDescription()).append("\n\n");
                
                for (Demo demo : categoryDemos) {
                    report.append("### ").append(demo.getName()).append("\n");
                    report.append(demo.getDescription()).append("\n");
                    
                    if (demo.getEstimatedRuntimeSeconds() > 0) {
                        report.append("**Runtime**: ~").append(demo.getEstimatedRuntimeSeconds()).append(" seconds\n");
                    }
                    
                    if (demo.hasPrerequisites()) {
                        report.append("**Prerequisites**: ").append(String.join(", ", demo.getPrerequisites())).append("\n");
                    }
                    
                    report.append("\n");
                }
            }
        }
        
        return report.toString();
    }
    
    // Private helper methods
    
    private void printBanner() {
        System.out.println("""
            ╔══════════════════════════════════════════════════════════════════════════════╗
            ║                        SpEL Rules Engine Demo Suite                          ║
            ║                         Unified Demo Framework                              ║
            ╠══════════════════════════════════════════════════════════════════════════════╣
            ║  Comprehensive demonstrations of rules engine capabilities                   ║
            ║  • Organized by category and complexity level                               ║
            ║  • Interactive and non-interactive execution modes                          ║
            ║  • Performance monitoring and detailed examples                             ║
            ╚══════════════════════════════════════════════════════════════════════════════╝
            """);
    }
    
    private void printMainMenu() {
        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                              DEMO MENU                                     │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  Commands:                                                                  │");
        System.out.println("│    list        - List all available demos                                  │");
        System.out.println("│    categories  - Browse demos by category                                  │");
        System.out.println("│    [demo-name] - Run a specific demo by name                              │");
        System.out.println("│    [number]    - Run a demo by index number                               │");
        System.out.println("│    0 or exit   - Exit the demo framework                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘");
        System.out.print("\nEnter command: ");
    }
    
    private void listAllDemos() {
        System.out.println("\n📋 Available Demos:");
        
        if (availableDemos.isEmpty()) {
            System.out.println("   No demos are currently registered.");
            return;
        }
        
        Map<DemoCategory, List<Demo>> demosByCategory = availableDemos.stream()
                .collect(Collectors.groupingBy(Demo::getCategory));
        
        int index = 1;
        for (DemoCategory category : DemoCategory.values()) {
            List<Demo> categoryDemos = demosByCategory.get(category);
            if (categoryDemos != null && !categoryDemos.isEmpty()) {
                System.out.println("\n" + category.toString());
                for (Demo demo : categoryDemos) {
                    String status = demo.isAvailable() ? "✅" : "❌";
                    System.out.printf("   %2d. %s %s%n", index++, status, demo.getName());
                    System.out.printf("       %s%n", demo.getDescription());
                }
            }
        }
    }
    
    private void showCategoriesMenu() {
        System.out.println("\n📂 Demo Categories:");
        
        for (DemoCategory category : DemoCategory.values()) {
            List<Demo> categoryDemos = getDemosByCategory(category);
            System.out.printf("   %s (%d demos)%n", category.toString(), categoryDemos.size());
            System.out.printf("      %s%n", category.getDescription());
        }
        
        System.out.print("\nEnter category name to browse (or press Enter to return): ");
        String categoryInput = scanner.nextLine().trim();
        
        if (!categoryInput.isEmpty()) {
            DemoCategory selectedCategory = findCategoryByName(categoryInput);
            if (selectedCategory != null) {
                showCategoryDemos(selectedCategory);
            } else {
                System.out.println("❌ Category not found: " + categoryInput);
            }
        }
    }
    
    private DemoCategory findCategoryByName(String name) {
        for (DemoCategory category : DemoCategory.values()) {
            if (category.name().equalsIgnoreCase(name) || 
                category.getDisplayName().equalsIgnoreCase(name)) {
                return category;
            }
        }
        return null;
    }
    
    private void showCategoryDemos(DemoCategory category) {
        List<Demo> categoryDemos = getDemosByCategory(category);
        
        System.out.println("\n" + category.toString() + " Demos:");
        
        if (categoryDemos.isEmpty()) {
            System.out.println("   No demos in this category.");
            return;
        }
        
        for (int i = 0; i < categoryDemos.size(); i++) {
            Demo demo = categoryDemos.get(i);
            String status = demo.isAvailable() ? "✅" : "❌";
            System.out.printf("   %d. %s %s%n", i + 1, status, demo.getName());
            System.out.printf("      %s%n", demo.getDescription());
        }
        
        System.out.print("\nEnter demo number to run (or press Enter to return): ");
        String input = scanner.nextLine().trim();
        
        if (!input.isEmpty()) {
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= categoryDemos.size()) {
                    runDemo(categoryDemos.get(choice - 1));
                } else {
                    System.out.println("❌ Invalid choice: " + choice);
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid number: " + input);
            }
        }
    }
    
    private void runDemoByIndex(int index) {
        if (index >= 1 && index <= availableDemos.size()) {
            // Create a flat list sorted by category and name
            List<Demo> sortedDemos = availableDemos.stream()
                    .sorted(Comparator.comparing((Demo d) -> d.getCategory().getSortOrder())
                            .thenComparing(Demo::getName))
                    .collect(Collectors.toList());
            
            runDemo(sortedDemos.get(index - 1));
        } else {
            System.out.println("❌ Invalid demo index: " + index);
            System.out.println("💡 Use 'list' to see available demos");
        }
    }
    
    private void runDemo(Demo demo) {
        if (!demo.isAvailable()) {
            System.out.println("❌ Demo is not available: " + demo.getName());
            if (demo.hasPrerequisites()) {
                System.out.println("Prerequisites: " + String.join(", ", demo.getPrerequisites()));
            }
            return;
        }
        
        System.out.println("🚀 Running demo: " + demo.getName());
        System.out.println("📝 " + demo.getDescription());
        
        if (demo.getEstimatedRuntimeSeconds() > 0) {
            System.out.println("⏱️  Estimated runtime: ~" + demo.getEstimatedRuntimeSeconds() + " seconds");
        }
        
        System.out.println("─".repeat(80));
        
        long startTime = System.currentTimeMillis();
        
        try {
            demo.runNonInteractive();
            
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("─".repeat(80));
            System.out.println("✅ Demo completed successfully in " + duration + "ms");
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.error("Demo failed: " + demo.getName(), e);
            System.out.println("─".repeat(80));
            System.out.println("❌ Demo failed after " + duration + "ms: " + e.getMessage());
        }
    }
    
    private void listAvailableDemoNames() {
        System.out.println("Available demos:");
        availableDemos.stream()
                .map(Demo::getName)
                .sorted()
                .forEach(name -> System.out.println("  - " + name));
    }
    
    private void printGoodbye() {
        System.out.println("\n👋 Thank you for exploring the SpEL Rules Engine!");
        System.out.println("Visit our documentation for more examples and advanced features.");
    }
}
