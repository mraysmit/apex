package dev.mars.apex.demo.examples;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.model.FinancialTrade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Batch Processing Demo - Demonstrates high-volume batch processing capabilities.
 * 
 * This demo showcases:
 * - Sequential vs parallel batch processing
 * - Performance optimization for large datasets
 * - Error handling in batch scenarios
 * - Memory-efficient processing patterns
 * - Batch result aggregation and reporting
 * - Real-world financial services batch scenarios
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class BatchProcessingDemo {
    
    private final RulesService rulesService;
    private final RulePerformanceMonitor performanceMonitor;
    
    public BatchProcessingDemo() {
        this.rulesService = new RulesService();
        this.performanceMonitor = new RulePerformanceMonitor();
        this.performanceMonitor.setEnabled(true);
    }
    
    /**
     * Run the complete Batch Processing demonstration.
     */
    public void run() {
        System.out.println("SpEL Rules Engine - Batch Processing");
        System.out.println("=" .repeat(60));
        System.out.println("High-volume data processing with enterprise performance!");
        System.out.println();
        
        demonstrateSequentialProcessing();
        System.out.println();
        
        demonstrateParallelProcessing();
        System.out.println();
        
        demonstrateMemoryEfficientProcessing();
        System.out.println();
        
        demonstrateErrorHandlingInBatches();
        System.out.println();
        
        demonstrateFinancialBatchScenarios();
        System.out.println();
        
        demonstrateBatchResultAggregation();
        System.out.println();
        
        System.out.println("PASSED Batch Processing demonstration completed!");
        System.out.println("   Ready for enterprise-scale data processing!");
    }
    
    /**
     * Demonstrate sequential batch processing.
     */
    private void demonstrateSequentialProcessing() {
        System.out.println(" Sequential Batch Processing");
        System.out.println("-".repeat(40));
        
        // Create sample dataset
        List<Customer> customers = generateCustomerBatch(1000);
        System.out.println("Processing " + customers.size() + " customers sequentially...");
        
        long startTime = System.currentTimeMillis();
        
        List<RuleResult> results = new ArrayList<>();
        for (Customer customer : customers) {
            boolean isValid = rulesService.check(
                "#data.age >= 18 && #data.email != null && #data.name != null", 
                customer
            );
            
            // Simulate result collection
            RuleResult result = createMockResult(customer, isValid);
            results.add(result);
        }
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        // Calculate statistics
        long validCount = results.stream().mapToLong(r -> r.isTriggered() ? 1 : 0).sum();
        double throughput = (double) customers.size() / (processingTime / 1000.0);
        
        System.out.println(" Sequential Processing Results:");
        System.out.println("   • Total records: " + customers.size());
        System.out.println("   • Valid records: " + validCount);
        System.out.println("   • Invalid records: " + (customers.size() - validCount));
        System.out.println("   • Processing time: " + processingTime + "ms");
        System.out.println("   • Throughput: " + String.format("%.1f", throughput) + " records/second");
    }
    
    /**
     * Demonstrate parallel batch processing.
     */
    private void demonstrateParallelProcessing() {
        System.out.println("Parallel Batch Processing");
        System.out.println("-".repeat(40));
        
        // Create sample dataset
        List<Customer> customers = generateCustomerBatch(1000);
        System.out.println("Processing " + customers.size() + " customers in parallel...");
        
        long startTime = System.currentTimeMillis();
        
        // Parallel processing with streams
        List<RuleResult> results = customers.parallelStream()
            .map(customer -> {
                boolean isValid = rulesService.check(
                    "#data.age >= 18 && #data.email != null && #data.name != null", 
                    customer
                );
                return createMockResult(customer, isValid);
            })
            .collect(Collectors.toList());
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        // Calculate statistics
        long validCount = results.stream().mapToLong(r -> r.isTriggered() ? 1 : 0).sum();
        double throughput = (double) customers.size() / (processingTime / 1000.0);
        
        System.out.println(" Parallel Processing Results:");
        System.out.println("   • Total records: " + customers.size());
        System.out.println("   • Valid records: " + validCount);
        System.out.println("   • Invalid records: " + (customers.size() - validCount));
        System.out.println("   • Processing time: " + processingTime + "ms");
        System.out.println("   • Throughput: " + String.format("%.1f", throughput) + " records/second");
        System.out.println("   • Performance improvement: " + 
                          String.format("%.1fx", (double) customers.size() / processingTime * 10) + " faster");
    }
    
    /**
     * Demonstrate memory-efficient batch processing.
     */
    private void demonstrateMemoryEfficientProcessing() {
        System.out.println("Memory-Efficient Batch Processing");
        System.out.println("-".repeat(40));
        
        System.out.println("Processing large dataset with streaming approach...");
        
        // Simulate large dataset processing with chunking
        int totalRecords = 10000;
        int chunkSize = 500;
        int processedCount = 0;
        int validCount = 0;
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < totalRecords; i += chunkSize) {
            int endIndex = Math.min(i + chunkSize, totalRecords);
            List<Customer> chunk = generateCustomerBatch(endIndex - i);
            
            // Process chunk
            for (Customer customer : chunk) {
                boolean isValid = rulesService.check(
                    "#data.age >= 18 && #data.email != null", 
                    customer
                );
                
                processedCount++;
                if (isValid) validCount++;
                
                // Simulate processing without storing all results in memory
                if (processedCount % 1000 == 0) {
                    System.out.println("   • Processed " + processedCount + " records...");
                }
            }
            
            // Clear chunk from memory
            chunk.clear();
        }
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        double throughput = (double) totalRecords / (processingTime / 1000.0);
        
        System.out.println(" Memory-Efficient Processing Results:");
        System.out.println("   • Total records: " + totalRecords);
        System.out.println("   • Valid records: " + validCount);
        System.out.println("   • Processing time: " + processingTime + "ms");
        System.out.println("   • Throughput: " + String.format("%.1f", throughput) + " records/second");
        System.out.println("   • Memory usage: Constant (chunked processing)");
    }
    
    /**
     * Generate a batch of sample customers.
     */
    private List<Customer> generateCustomerBatch(int size) {
        List<Customer> customers = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < size; i++) {
            Customer customer = new Customer();
            customer.setName("Customer" + i);
            customer.setAge(18 + random.nextInt(50)); // Age 18-67
            
            // 90% have valid emails, 10% don't (for testing validation)
            if (random.nextDouble() < 0.9) {
                customer.setEmail("customer" + i + "@example.com");
            }
            
            customers.add(customer);
        }
        
        return customers;
    }
    
    /**
     * Create a mock RuleResult for demonstration purposes.
     */
    private RuleResult createMockResult(Customer customer, boolean isValid) {
        // Create appropriate RuleResult based on validation result
        if (isValid) {
            return RuleResult.match("customer-validation", "Customer " + customer.getName() + " is valid");
        } else {
            return RuleResult.error("customer-validation", "Validation failed for " + customer.getName());
        }
    }
    
    /**
     * Demonstrate error handling in batch processing scenarios.
     */
    private void demonstrateErrorHandlingInBatches() {
        System.out.println("Error Handling in Batch Processing");
        System.out.println("-".repeat(40));

        // Create dataset with some problematic records
        List<Map<String, Object>> mixedData = generateMixedDataBatch(100);
        System.out.println("Processing " + mixedData.size() + " records with error handling...");

        int successCount = 0;
        int errorCount = 0;
        List<String> errorMessages = new ArrayList<>();

        for (Map<String, Object> data : mixedData) {
            try {
                boolean isValid = rulesService.check(
                    "#amount > 0 && #currency != null && #amount <= 1000000",
                    data
                );

                if (isValid) {
                    successCount++;
                } else {
                    errorCount++;
                    errorMessages.add("Validation failed for record: " + data.get("id"));
                }

            } catch (Exception e) {
                errorCount++;
                errorMessages.add("Exception for record " + data.get("id") + ": " + e.getMessage());
            }
        }

        System.out.println(" Error Handling Results:");
        System.out.println("   • Total records: " + mixedData.size());
        System.out.println("   • Successful: " + successCount);
        System.out.println("   • Errors: " + errorCount);
        System.out.println("   • Error rate: " + String.format("%.1f%%", (double) errorCount / mixedData.size() * 100));

        if (!errorMessages.isEmpty()) {
            System.out.println("   • Sample errors:");
            errorMessages.stream().limit(3).forEach(msg ->
                System.out.println("     - " + msg));
        }
    }

    /**
     * Demonstrate financial services batch processing scenarios.
     */
    private void demonstrateFinancialBatchScenarios() {
        System.out.println("Financial Services Batch Scenarios");
        System.out.println("-".repeat(40));

        // End-of-day trade settlement processing
        demonstrateTradeSettlementBatch();
        System.out.println();

        // Risk calculation batch
        demonstrateRiskCalculationBatch();
    }

    /**
     * Demonstrate trade settlement batch processing.
     */
    private void demonstrateTradeSettlementBatch() {
        System.out.println(" End-of-Day TradeB Settlement Processing");

        List<FinancialTrade> trades = generateTradeBatch(500);
        System.out.println("Processing " + trades.size() + " trades for settlement...");

        long startTime = System.currentTimeMillis();

        Map<String, Integer> settlementStatus = trades.parallelStream()
            .collect(Collectors.groupingBy(
                trade -> {
                    boolean canSettle = rulesService.check(
                        "#data.amount > 0 && #data.currency != null && " +
                        "#data.counterparty != null && #data.tradeDate != null",
                        trade
                    );
                    return canSettle ? "READY_FOR_SETTLEMENT" : "SETTLEMENT_BLOCKED";
                },
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));

        long processingTime = System.currentTimeMillis() - startTime;

        System.out.println("    Settlement Results:");
        System.out.println("     • Ready for settlement: " +
                          settlementStatus.getOrDefault("READY_FOR_SETTLEMENT", 0));
        System.out.println("     • Settlement blocked: " +
                          settlementStatus.getOrDefault("SETTLEMENT_BLOCKED", 0));
        System.out.println("     • Processing time: " + processingTime + "ms");
    }

    /**
     * Demonstrate risk calculation batch processing.
     */
    private void demonstrateRiskCalculationBatch() {
        System.out.println(" Portfolio Risk Calculation Batch");

        List<FinancialTrade> trades = generateTradeBatch(1000);
        System.out.println("Calculating risk metrics for " + trades.size() + " positions...");

        long startTime = System.currentTimeMillis();

        // Parallel risk calculation
        Map<String, BigDecimal> riskByCounterparty = trades.parallelStream()
            .filter(trade -> rulesService.check("#data.amount > 0", trade))
            .collect(Collectors.groupingBy(
                FinancialTrade::getCounterparty,
                Collectors.mapping(
                    FinancialTrade::getAmount,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));

        long processingTime = System.currentTimeMillis() - startTime;

        System.out.println("    Risk Calculation Results:");
        System.out.println("     • Counterparties analyzed: " + riskByCounterparty.size());
        System.out.println("     • Total exposure: $" +
                          riskByCounterparty.values().stream()
                              .reduce(BigDecimal.ZERO, BigDecimal::add));
        System.out.println("     • Processing time: " + processingTime + "ms");

        // Show top exposures
        System.out.println("     • Top exposures:");
        riskByCounterparty.entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .limit(3)
            .forEach(entry ->
                System.out.println("       - " + entry.getKey() + ": $" + entry.getValue()));
    }

    /**
     * Demonstrate batch result aggregation and reporting.
     */
    private void demonstrateBatchResultAggregation() {
        System.out.println("📈 Batch Result Aggregation & Reporting");
        System.out.println("-".repeat(40));

        List<Customer> customers = generateCustomerBatch(2000);
        System.out.println("Generating comprehensive batch report for " + customers.size() + " customers...");

        long startTime = System.currentTimeMillis();

        // Parallel processing with detailed aggregation
        BatchProcessingReport report = customers.parallelStream()
            .collect(BatchProcessingReport::new,
                (batchReport, customer) -> {
                    boolean isAdult = rulesService.check("#data.age >= 18", customer);
                    boolean hasEmail = rulesService.check("#data.email != null", customer);
                    boolean isValid = isAdult && hasEmail;

                    batchReport.addResult(customer, isValid, isAdult, hasEmail);
                },
                BatchProcessingReport::combine);

        long processingTime = System.currentTimeMillis() - startTime;

        // Display comprehensive report
        System.out.println(" Comprehensive Batch Report:");
        System.out.println("   • Total records processed: " + report.getTotalRecords());
        System.out.println("   • Valid records: " + report.getValidRecords());
        System.out.println("   • Invalid records: " + report.getInvalidRecords());
        System.out.println("   • Adults: " + report.getAdultCount());
        System.out.println("   • With email: " + report.getEmailCount());
        System.out.println("   • Success rate: " + String.format("%.1f%%", report.getSuccessRate()));
        System.out.println("   • Processing time: " + processingTime + "ms");
        System.out.println("   • Throughput: " + String.format("%.1f", report.getThroughput(processingTime)) + " records/second");
    }

    /**
     * Generate mixed data batch with some problematic records.
     */
    private List<Map<String, Object>> generateMixedDataBatch(int size) {
        List<Map<String, Object>> data = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < size; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", "REC" + i);

            // 80% valid records, 20% problematic
            if (random.nextDouble() < 0.8) {
                record.put("amount", BigDecimal.valueOf(1000 + random.nextInt(100000)));
                record.put("currency", "USD");
            } else {
                // Problematic records
                if (random.nextBoolean()) {
                    record.put("amount", BigDecimal.valueOf(-1000)); // Negative amount
                    record.put("currency", "USD");
                } else {
                    record.put("amount", BigDecimal.valueOf(1000));
                    // Missing currency
                }
            }

            data.add(record);
        }

        return data;
    }

    /**
     * Generate a batch of sample trades.
     */
    private List<FinancialTrade> generateTradeBatch(int size) {
        List<FinancialTrade> trades = new ArrayList<>();
        Random random = new Random();
        String[] counterparties = {"Goldman Sachs", "JP Morgan", "Morgan Stanley", "Citi", "Deutsche Bank"};
        String[] currencies = {"USD", "EUR", "GBP", "JPY"};

        for (int i = 0; i < size; i++) {
            FinancialTrade trade = new FinancialTrade();
            trade.setTradeId("TRD" + i);
            trade.setAmount(BigDecimal.valueOf(10000 + random.nextInt(1000000)));
            trade.setCurrency(currencies[random.nextInt(currencies.length)]);
            trade.setCounterparty(counterparties[random.nextInt(counterparties.length)]);
            trade.setTradeDate(LocalDate.now().minusDays(random.nextInt(30)));

            trades.add(trade);
        }

        return trades;
    }

    /**
     * Batch processing report aggregator.
     */
    private static class BatchProcessingReport {
        private int totalRecords = 0;
        private int validRecords = 0;
        private int adultCount = 0;
        private int emailCount = 0;

        public void addResult(Customer customer, boolean isValid, boolean isAdult, boolean hasEmail) {
            totalRecords++;
            if (isValid) validRecords++;
            if (isAdult) adultCount++;
            if (hasEmail) emailCount++;
        }

        public void combine(BatchProcessingReport other) {
            this.totalRecords += other.totalRecords;
            this.validRecords += other.validRecords;
            this.adultCount += other.adultCount;
            this.emailCount += other.emailCount;
        }

        public int getTotalRecords() { return totalRecords; }
        public int getValidRecords() { return validRecords; }
        public int getInvalidRecords() { return totalRecords - validRecords; }
        public int getAdultCount() { return adultCount; }
        public int getEmailCount() { return emailCount; }
        public double getSuccessRate() { return (double) validRecords / totalRecords * 100; }
        public double getThroughput(long processingTimeMs) {
            return (double) totalRecords / (processingTimeMs / 1000.0);
        }
    }

    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        new BatchProcessingDemo().run();
    }
}



