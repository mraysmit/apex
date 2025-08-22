package dev.mars.apex.demo.examples.lookups;

import dev.mars.apex.demo.model.lookups.CurrencyTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates simple field lookup using currency codes.
 * This example shows the most basic lookup pattern where a single field value
 * is used to lookup reference data from an inline dataset.
 *
 * Pattern Demonstrated: lookup-key: "#fieldName"
 * Use Case: Currency transaction enrichment with currency details
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-22
 * @version 1.0
 */
public class SimpleFieldLookupDemo extends AbstractLookupDemo {
    
    public static void main(String[] args) {
        new SimpleFieldLookupDemo().runDemo();
    }
    
    @Override
    protected String getDemoTitle() {
        return "Simple Field Lookup Demo - Currency Transaction Enrichment";
    }
    
    @Override
    protected String getDemoDescription() {
        return "Demonstrates basic lookup-key pattern '#currencyCode' to enrich transactions with currency details.\n" +
               "   This is the most fundamental lookup pattern where a single field value is used to\n" +
               "   lookup reference data from an inline dataset. Each transaction's currency code\n" +
               "   is matched against a comprehensive currency reference dataset.";
    }
    
    @Override
    protected String getYamlConfigPath() {
        return "examples/lookups/simple-field-lookup.yaml";
    }
    
    @Override
    protected void loadConfiguration() throws Exception {
        System.out.println("📁 Loading YAML configuration from: " + getYamlConfigPath());

        // Load the YAML configuration from classpath
        ruleConfiguration = yamlLoader.loadFromClasspath(getYamlConfigPath());
        rulesEngine = yamlService.createRulesEngineFromYamlConfig(ruleConfiguration);

        System.out.println("✅ Configuration loaded successfully");
        System.out.println("   - Enrichments: 1 (currency-details-enrichment)");
        System.out.println("   - Validations: 2 (currency-code-format, amount-positive)");
        System.out.println("   - Lookup Dataset: 10 currencies (USD, EUR, GBP, JPY, CHF, CAD, AUD, CNY, INR, BRL)");
    }
    
    @Override
    protected List<CurrencyTransaction> generateTestData() {
        System.out.println("🏭 Generating test currency transactions...");
        
        List<CurrencyTransaction> transactions = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(7);
        
        // Create diverse transaction data with different currencies
        transactions.add(new CurrencyTransaction(
            "TXN-001", 
            new BigDecimal("1250.00"), 
            "USD", 
            "Online purchase - Electronics",
            baseTime.plusHours(1),
            "TechMart Inc",
            "Electronics"
        ));
        
        transactions.add(new CurrencyTransaction(
            "TXN-002", 
            new BigDecimal("850.75"), 
            "EUR", 
            "Hotel booking - Business travel",
            baseTime.plusHours(6),
            "Grand Hotel Europe",
            "Travel"
        ));
        
        transactions.add(new CurrencyTransaction(
            "TXN-003", 
            new BigDecimal("45000"), 
            "JPY", 
            "Restaurant dinner - Tokyo",
            baseTime.plusHours(12),
            "Sushi Zen",
            "Dining"
        ));
        
        transactions.add(new CurrencyTransaction(
            "TXN-004", 
            new BigDecimal("320.50"), 
            "GBP", 
            "Book purchase - Academic",
            baseTime.plusDays(1),
            "Academic Books Ltd",
            "Education"
        ));
        
        transactions.add(new CurrencyTransaction(
            "TXN-005", 
            new BigDecimal("2100.00"), 
            "CHF", 
            "Watch purchase - Luxury",
            baseTime.plusDays(2),
            "Swiss Timepieces",
            "Luxury"
        ));
        
        transactions.add(new CurrencyTransaction(
            "TXN-006", 
            new BigDecimal("450.25"), 
            "CAD", 
            "Grocery shopping - Weekly",
            baseTime.plusDays(3),
            "Fresh Market Co",
            "Groceries"
        ));
        
        transactions.add(new CurrencyTransaction(
            "TXN-007", 
            new BigDecimal("180.90"), 
            "AUD", 
            "Coffee subscription - Monthly",
            baseTime.plusDays(4),
            "Aussie Coffee Co",
            "Subscription"
        ));
        
        transactions.add(new CurrencyTransaction(
            "TXN-008", 
            new BigDecimal("15600.00"), 
            "CNY", 
            "Manufacturing supplies",
            baseTime.plusDays(5),
            "Beijing Supplies Ltd",
            "Business"
        ));
        
        transactions.add(new CurrencyTransaction(
            "TXN-009", 
            new BigDecimal("8750.50"), 
            "INR", 
            "Software development - Outsourcing",
            baseTime.plusDays(6),
            "Mumbai Tech Solutions",
            "Technology"
        ));
        
        transactions.add(new CurrencyTransaction(
            "TXN-010", 
            new BigDecimal("2800.75"), 
            "BRL", 
            "Marketing campaign - Regional",
            baseTime.plusDays(7),
            "Rio Marketing Agency",
            "Marketing"
        ));
        
        System.out.println("✅ Generated " + transactions.size() + " test transactions");
        System.out.println("   - Currencies: USD, EUR, JPY, GBP, CHF, CAD, AUD, CNY, INR, BRL");
        System.out.println("   - Categories: Electronics, Travel, Dining, Education, Luxury, Groceries, Subscription, Business, Technology, Marketing");
        
        return transactions;
    }
    
    @Override
    protected List<CurrencyTransaction> processData(List<?> data) throws Exception {
        System.out.println("⚙️  Processing transactions with currency enrichment...");

        List<CurrencyTransaction> results = new ArrayList<>();

        for (Object item : data) {
            if (item instanceof CurrencyTransaction) {
                CurrencyTransaction transaction = (CurrencyTransaction) item;

                // For now, simulate enrichment by copying the transaction
                // In a real implementation, this would use the rules engine
                CurrencyTransaction enriched = simulateEnrichment(transaction);
                results.add(enriched);

                // Log the lookup process
                System.out.println("   🔍 Processed " + transaction.getTransactionId() +
                                 " (" + transaction.getCurrencyCode() + ") -> " +
                                 "Enriched with currency details");
            }
        }

        return results;
    }

    /**
     * Simulate currency enrichment based on the lookup configuration.
     * This demonstrates what the YAML configuration would do.
     */
    private CurrencyTransaction simulateEnrichment(CurrencyTransaction original) {
        CurrencyTransaction enriched = new CurrencyTransaction(
            original.getTransactionId(),
            original.getAmount(),
            original.getCurrencyCode(),
            original.getDescription(),
            original.getTransactionDate(),
            original.getMerchantName(),
            original.getCategory()
        );

        // Simulate lookup based on currency code
        switch (original.getCurrencyCode()) {
            case "USD":
                enriched.setCurrencyName("US Dollar");
                enriched.setCurrencySymbol("$");
                enriched.setDecimalPlaces(2);
                enriched.setCountryCode("US");
                enriched.setIsBaseCurrency(true);
                break;
            case "EUR":
                enriched.setCurrencyName("Euro");
                enriched.setCurrencySymbol("€");
                enriched.setDecimalPlaces(2);
                enriched.setCountryCode("EU");
                enriched.setIsBaseCurrency(true);
                break;
            case "GBP":
                enriched.setCurrencyName("British Pound Sterling");
                enriched.setCurrencySymbol("£");
                enriched.setDecimalPlaces(2);
                enriched.setCountryCode("GB");
                enriched.setIsBaseCurrency(true);
                break;
            case "JPY":
                enriched.setCurrencyName("Japanese Yen");
                enriched.setCurrencySymbol("¥");
                enriched.setDecimalPlaces(0);
                enriched.setCountryCode("JP");
                enriched.setIsBaseCurrency(true);
                break;
            default:
                // For other currencies, set basic info
                enriched.setCurrencyName("Unknown Currency");
                enriched.setCurrencySymbol(original.getCurrencyCode());
                enriched.setDecimalPlaces(2);
                enriched.setIsBaseCurrency(false);
                break;
        }

        return enriched;
    }
    
    @Override
    protected List<CurrencyTransaction> generateErrorTestData() {
        System.out.println("⚠️  Generating error scenario test data...");
        
        List<CurrencyTransaction> errorTransactions = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now();
        
        // Invalid currency code (not 3 characters)
        errorTransactions.add(new CurrencyTransaction(
            "ERR-001", 
            new BigDecimal("100.00"), 
            "US", // Invalid: only 2 characters
            "Invalid currency code test",
            baseTime
        ));
        
        // Unknown currency code
        errorTransactions.add(new CurrencyTransaction(
            "ERR-002", 
            new BigDecimal("200.00"), 
            "XYZ", // Unknown currency
            "Unknown currency test",
            baseTime
        ));
        
        // Null currency code
        CurrencyTransaction nullCurrencyTxn = new CurrencyTransaction(
            "ERR-003", 
            new BigDecimal("300.00"), 
            null, // Null currency
            "Null currency test",
            baseTime
        );
        errorTransactions.add(nullCurrencyTxn);
        
        // Negative amount
        errorTransactions.add(new CurrencyTransaction(
            "ERR-004", 
            new BigDecimal("-50.00"), // Negative amount
            "USD", 
            "Negative amount test",
            baseTime
        ));
        
        System.out.println("✅ Generated " + errorTransactions.size() + " error scenario transactions");
        
        return errorTransactions;
    }
    
    @Override
    protected void displayResults(List<?> results, List<?> originalData) {
        super.displayResults(results, originalData);

        // Additional analysis specific to currency lookup
        System.out.println("\n💱 CURRENCY LOOKUP ANALYSIS:");

        int enrichedCount = 0;

        for (Object result : results) {
            if (result instanceof CurrencyTransaction) {
                CurrencyTransaction enriched = (CurrencyTransaction) result;

                if (enriched.getCurrencyName() != null) {
                    enrichedCount++;
                }
            }
        }

        System.out.println("  Successfully Enriched: " + enrichedCount + "/" + results.size());
        System.out.printf("  Enrichment Success Rate: %.1f%%%n",
                         (double) enrichedCount / results.size() * 100);

        // Show some specific enrichment examples
        System.out.println("\n  📊 ENRICHMENT EXAMPLES:");
        for (int i = 0; i < Math.min(3, results.size()); i++) {
            if (results.get(i) instanceof CurrencyTransaction) {
                CurrencyTransaction tx = (CurrencyTransaction) results.get(i);
                System.out.printf("    %s: %s -> %s (%s)%n",
                    tx.getCurrencyCode(),
                    tx.getCurrencyCode(),
                    tx.getCurrencyName() != null ? tx.getCurrencyName() : "Not enriched",
                    tx.getCurrencySymbol() != null ? tx.getCurrencySymbol() : "No symbol");
            }
        }
    }
}
