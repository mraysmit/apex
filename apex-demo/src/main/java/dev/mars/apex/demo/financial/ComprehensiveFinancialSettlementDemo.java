package dev.mars.apex.demo.financial;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.api.ValidationResult;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.demo.financial.model.TradeConfirmation;
import dev.mars.apex.demo.financial.model.TradeHeader;
import dev.mars.apex.demo.financial.model.PartyTradeIdentifier;
import dev.mars.apex.demo.financial.model.Security;
import dev.mars.apex.demo.financial.model.Counterparty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive Financial Settlement Enrichment Demo
 * 
 * This demo showcases comprehensive post-trade settlement enrichment using the APEX Rules Engine
 * with real-world financial examples from the "Types of Enrichment Relevant for Financial 
 * Services Post-Trade Settlement" guide.
 * 
 * ENRICHMENT CATEGORIES DEMONSTRATED:
 * - Reference Data Enrichment (LEI, ISIN, MIC, BIC, SSI)
 * - Counterparty Enrichment (Credit ratings, classifications, relationships)
 * - Regulatory Enrichment (UTI generation, MiFID II, EMIR, Dodd-Frank)
 * - Risk Enrichment (VaR, exposure, margin, stress testing)
 * - Settlement Enrichment (Dates, methods, priorities)
 * - Fee Calculation Enrichment (Commissions, taxes, regulatory fees)
 * 
 * INDUSTRY STANDARDS COVERED:
 * - ISO 20022: Financial messaging standards
 * - FpML: Financial Products Markup Language
 * - EMIR/MiFID II: European regulatory requirements
 * - Dodd-Frank: US derivatives regulation
 * - ISDA: Risk and legal documentation standards
 */
public class ComprehensiveFinancialSettlementDemo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComprehensiveFinancialSettlementDemo.class);

    private final YamlRulesEngineService yamlService;

    public ComprehensiveFinancialSettlementDemo() {
        this.yamlService = new YamlRulesEngineService();
    }

    /**
     * Demonstrates comprehensive settlement enrichment with multiple trade examples
     */
    public void runComprehensiveDemo() {
        LOGGER.info("=".repeat(80));
        LOGGER.info("COMPREHENSIVE FINANCIAL SETTLEMENT ENRICHMENT DEMO");
        LOGGER.info("=".repeat(80));

        // Load the comprehensive settlement enrichment configuration
        String configPath = "demos/industry/financial-services/settlement/comprehensive-settlement-enrichment.yaml";
        
        try {
            // Example 1: UK Equity Trade (Shell PLC)
            LOGGER.info("\n" + "=".repeat(60));
            LOGGER.info("EXAMPLE 1: UK EQUITY TRADE - Royal Dutch Shell PLC");
            LOGGER.info("=".repeat(60));
            
            TradeConfirmation ukEquityTrade = createUKEquityTrade();
            processTradeWithEnrichment(ukEquityTrade, configPath, "UK Equity Trade");

            // Example 2: US Government Bond Trade
            LOGGER.info("\n" + "=".repeat(60));
            LOGGER.info("EXAMPLE 2: US GOVERNMENT BOND TRADE - 10Y Treasury Note");
            LOGGER.info("=".repeat(60));
            
            TradeConfirmation usBondTrade = createUSBondTrade();
            processTradeWithEnrichment(usBondTrade, configPath, "US Bond Trade");

            // Example 3: German Equity Trade (SAP SE)
            LOGGER.info("\n" + "=".repeat(60));
            LOGGER.info("EXAMPLE 3: GERMAN EQUITY TRADE - SAP SE");
            LOGGER.info("=".repeat(60));
            
            TradeConfirmation germanEquityTrade = createGermanEquityTrade();
            processTradeWithEnrichment(germanEquityTrade, configPath, "German Equity Trade");

            // Example 4: US Equity Trade with High Value (Apple Inc)
            LOGGER.info("\n" + "=".repeat(60));
            LOGGER.info("EXAMPLE 4: HIGH-VALUE US EQUITY TRADE - Apple Inc");
            LOGGER.info("=".repeat(60));
            
            TradeConfirmation highValueTrade = createHighValueUSEquityTrade();
            processTradeWithEnrichment(highValueTrade, configPath, "High-Value US Equity Trade");

        } catch (Exception e) {
            LOGGER.error("Error running comprehensive settlement demo", e);
        }
    }

    /**
     * Process a trade with comprehensive enrichment and display results
     */
    private void processTradeWithEnrichment(TradeConfirmation trade, String configPath, String tradeDescription) {
        try {
            LOGGER.info("Processing: {}", tradeDescription);
            LOGGER.info("Trade ID: {}", trade.getTrade().getTradeHeader().getPartyTradeIdentifier().getTradeId());
            LOGGER.info("Security: {} ({})", trade.getTrade().getSecurity().getInstrumentId(), 
                       trade.getTrade().getSecurity().getIssuer());
            LOGGER.info("Counterparty: {}", trade.getTrade().getCounterparty().getPartyName());
            LOGGER.info("Trading Venue: {}", trade.getTrade().getTradingVenue());
            LOGGER.info("Trade Value: {} {} (Quantity: {}, Price: {})", 
                       trade.getTrade().getQuantity().multiply(trade.getTrade().getPrice()),
                       trade.getTrade().getCurrency(),
                       trade.getTrade().getQuantity(),
                       trade.getTrade().getPrice());

            // Create context for rule processing
            Map<String, Object> context = new HashMap<>();
            context.put("trade", trade.getTrade());
            context.put("header", trade.getHeader());

            // Create rules engine from YAML configuration
            RulesEngine rulesEngine = yamlService.createRulesEngineFromClasspath(configPath);
            
            // Execute rules
            RuleResult result = rulesEngine.executeRulesForCategory("settlement", context);

            // Display results
            displayRuleResults(result);

            // Display enrichment results (simplified for demo)
            LOGGER.info("Trade processing completed for: {}", tradeDescription);

            // Display rule execution summary
            displaySingleRuleResult(result);

        } catch (Exception e) {
            LOGGER.error("Error processing trade: {}", tradeDescription, e);
        }
    }

    /**
     * Display validation results
     */
    private void displayValidationResults(Map<String, ValidationResult> validationResults) {
        LOGGER.info("\n--- VALIDATION RESULTS ---");
        
        if (validationResults.isEmpty()) {
            LOGGER.info("No validation rules executed");
            return;
        }

        validationResults.forEach((ruleId, result) -> {
            String status = result.isValid() ? "✓ PASSED" : "✗ FAILED";
            LOGGER.info("{}: {}", status, ruleId);
            
            if (!result.isValid()) {
                LOGGER.warn("  Errors: {}", result.getErrorsAsString());
            }
        });
    }

    /**
     * Display enrichment results with categorized output
     */
    private void displayEnrichmentResults(Map<String, Object> enrichedData, String tradeDescription) {
        LOGGER.info("\n--- ENRICHMENT RESULTS FOR: {} ---", tradeDescription);

        if (enrichedData.isEmpty()) {
            LOGGER.info("No enrichment data available");
            return;
        }

        // Display Reference Data Enrichment
        displayReferenceDataEnrichment(enrichedData);

        // Display Counterparty Enrichment
        displayCounterpartyEnrichment(enrichedData);

        // Display Regulatory Enrichment
        displayRegulatoryEnrichment(enrichedData);

        // Display Risk Enrichment
        displayRiskEnrichment(enrichedData);

        // Display Settlement Enrichment
        displaySettlementEnrichment(enrichedData);

        // Display Fee Calculation Enrichment
        displayFeeEnrichment(enrichedData);
    }

    /**
     * Display reference data enrichment results
     */
    private void displayReferenceDataEnrichment(Map<String, Object> enrichedData) {
        LOGGER.info("\n  📋 REFERENCE DATA ENRICHMENT:");
        
        Object trade = enrichedData.get("trade");
        if (trade instanceof Map) {
            Map<String, Object> tradeMap = (Map<String, Object>) trade;
            
            // Counterparty LEI
            Object counterparty = tradeMap.get("counterparty");
            if (counterparty instanceof Map) {
                Map<String, Object> counterpartyMap = (Map<String, Object>) counterparty;
                logField("    LEI", counterpartyMap.get("lei"));
                logField("    Jurisdiction", counterpartyMap.get("jurisdiction"));
                logField("    Entity Type", counterpartyMap.get("entityType"));
            }

            // Security enrichment
            Object security = tradeMap.get("security");
            if (security instanceof Map) {
                Map<String, Object> securityMap = (Map<String, Object>) security;
                logField("    Security Name", securityMap.get("name"));
                logField("    CUSIP", securityMap.get("cusip"));
                logField("    SEDOL", securityMap.get("sedol"));
                logField("    Asset Class", securityMap.get("assetClass"));
                logField("    Country", securityMap.get("country"));
            }

            // Venue enrichment
            Object venue = tradeMap.get("venue");
            if (venue instanceof Map) {
                Map<String, Object> venueMap = (Map<String, Object>) venue;
                logField("    Venue Name", venueMap.get("venueName"));
                logField("    Venue Type", venueMap.get("venueType"));
                logField("    Operating Hours", venueMap.get("operatingHours"));
            }

            // Settlement instructions
            Object settlement = tradeMap.get("settlement");
            if (settlement instanceof Map) {
                Map<String, Object> settlementMap = (Map<String, Object>) settlement;
                logField("    Settlement Method", settlementMap.get("method"));
                logField("    Account Number", settlementMap.get("accountNumber"));
                logField("    Custodian", settlementMap.get("custodian"));
                logField("    BIC Code", settlementMap.get("counterpartyBIC"));
            }
        }
    }

    /**
     * Display counterparty enrichment results
     */
    private void displayCounterpartyEnrichment(Map<String, Object> enrichedData) {
        LOGGER.info("\n  🏦 COUNTERPARTY ENRICHMENT:");
        
        Object trade = enrichedData.get("trade");
        if (trade instanceof Map) {
            Map<String, Object> tradeMap = (Map<String, Object>) trade;
            Object counterparty = tradeMap.get("counterparty");
            
            if (counterparty instanceof Map) {
                Map<String, Object> counterpartyMap = (Map<String, Object>) counterparty;
                
                // Credit ratings
                Object creditRating = counterpartyMap.get("creditRating");
                if (creditRating instanceof Map) {
                    Map<String, Object> ratingMap = (Map<String, Object>) creditRating;
                    logField("    Moody's Rating", ratingMap.get("moodys"));
                    logField("    S&P Rating", ratingMap.get("sp"));
                    logField("    Fitch Rating", ratingMap.get("fitch"));
                }

                // Classification
                Object classification = counterpartyMap.get("classification");
                if (classification instanceof Map) {
                    Map<String, Object> classMap = (Map<String, Object>) classification;
                    logField("    Business Model", classMap.get("businessModel"));
                    logField("    Regulatory Status", classMap.get("regulatoryStatus"));
                }

                // Relationship
                Object relationship = counterpartyMap.get("relationship");
                if (relationship instanceof Map) {
                    Map<String, Object> relMap = (Map<String, Object>) relationship;
                    logField("    Client Tier", relMap.get("tier"));
                }

                // Legal agreements
                Object legalAgreements = counterpartyMap.get("legalAgreements");
                if (legalAgreements instanceof Map) {
                    Map<String, Object> legalMap = (Map<String, Object>) legalAgreements;
                    logField("    ISDA Master Agreement", legalMap.get("isdaMasterAgreement"));
                    logField("    CSA Agreement", legalMap.get("csaAgreement"));
                    logField("    CSA Threshold", legalMap.get("csaThreshold"));
                }
            }
        }
    }

    /**
     * Display regulatory enrichment results
     */
    private void displayRegulatoryEnrichment(Map<String, Object> enrichedData) {
        LOGGER.info("\n  📊 REGULATORY ENRICHMENT:");

        Object trade = enrichedData.get("trade");
        if (trade instanceof Map) {
            Map<String, Object> tradeMap = (Map<String, Object>) trade;
            Object regulatory = tradeMap.get("regulatory");

            if (regulatory instanceof Map) {
                Map<String, Object> regMap = (Map<String, Object>) regulatory;
                logField("    UTI", regMap.get("uti"));
                logField("    UTI Prefix", regMap.get("utiPrefix"));
                logField("    UTI Generation Time", regMap.get("utiGenerationTimestamp"));

                // MiFID II fields
                Object mifidII = regMap.get("mifidII");
                if (mifidII instanceof Map) {
                    Map<String, Object> mifidMap = (Map<String, Object>) mifidII;
                    logField("    MiFID II Applicable", mifidMap.get("applicable"));
                    logField("    Transaction Ref Number", mifidMap.get("transactionReferenceNumber"));
                    logField("    Venue Reporting", mifidMap.get("venueReporting"));
                }

                // EMIR fields
                Object emir = regMap.get("emir");
                if (emir instanceof Map) {
                    Map<String, Object> emirMap = (Map<String, Object>) emir;
                    logField("    EMIR Applicable", emirMap.get("applicable"));
                }

                // Dodd-Frank fields
                Object doddFrank = regMap.get("doddFrank");
                if (doddFrank instanceof Map) {
                    Map<String, Object> dfMap = (Map<String, Object>) doddFrank;
                    logField("    Dodd-Frank Applicable", dfMap.get("applicable"));
                }
            }
        }
    }

    /**
     * Display risk enrichment results
     */
    private void displayRiskEnrichment(Map<String, Object> enrichedData) {
        LOGGER.info("\n  ⚠️ RISK ENRICHMENT:");

        Object trade = enrichedData.get("trade");
        if (trade instanceof Map) {
            Map<String, Object> tradeMap = (Map<String, Object>) trade;

            // Trade value
            logField("    Trade Value", tradeMap.get("tradeValue"));
            logField("    Trade Value USD", tradeMap.get("tradeValueUSD"));

            // Risk metrics
            Object riskMetrics = tradeMap.get("riskMetrics");
            if (riskMetrics instanceof Map) {
                Map<String, Object> riskMap = (Map<String, Object>) riskMetrics;
                logField("    1-Day VaR (99%)", riskMap.get("var1Day99"));
                logField("    10-Day VaR (99%)", riskMap.get("var10Day99"));
                logField("    Implied Volatility", riskMap.get("impliedVolatility"));
                logField("    Historical Volatility", riskMap.get("historicalVolatility"));
                logField("    Beta", riskMap.get("beta"));
                logField("    Duration", riskMap.get("duration"));
            }

            // Stress test results
            Object stressTest = tradeMap.get("stressTest");
            if (stressTest instanceof Map) {
                Map<String, Object> stressMap = (Map<String, Object>) stressTest;
                Object marketCrash = stressMap.get("marketCrash");
                if (marketCrash instanceof Map) {
                    Map<String, Object> crashMap = (Map<String, Object>) marketCrash;
                    logField("    Market Crash Scenario", crashMap.get("scenario"));
                }
                logField("    Volatility Shock Scenario", stressMap.get("volatilityShock"));
                logField("    Risk Level", stressMap.get("riskLevel"));
                logField("    Action Required", stressMap.get("actionRequired"));
            }
        }
    }

    /**
     * Display settlement enrichment results
     */
    private void displaySettlementEnrichment(Map<String, Object> enrichedData) {
        LOGGER.info("\n  🏛️ SETTLEMENT ENRICHMENT:");

        Object trade = enrichedData.get("trade");
        if (trade instanceof Map) {
            Map<String, Object> tradeMap = (Map<String, Object>) trade;
            Object settlement = tradeMap.get("settlement");

            if (settlement instanceof Map) {
                Map<String, Object> settlementMap = (Map<String, Object>) settlement;
                logField("    Settlement Cycle", settlementMap.get("cycle"));
                logField("    Settlement Date", settlementMap.get("settlementDate"));
                logField("    Priority", settlementMap.get("priority"));
                logField("    Priority Code", settlementMap.get("priorityCode"));
                logField("    Cutoff Time", settlementMap.get("cutoffTime"));
            }
        }
    }

    /**
     * Display fee enrichment results
     */
    private void displayFeeEnrichment(Map<String, Object> enrichedData) {
        LOGGER.info("\n  💰 FEE CALCULATION ENRICHMENT:");

        Object trade = enrichedData.get("trade");
        if (trade instanceof Map) {
            Map<String, Object> tradeMap = (Map<String, Object>) trade;
            Object fees = tradeMap.get("fees");

            if (fees instanceof Map) {
                Map<String, Object> feesMap = (Map<String, Object>) fees;
                logField("    Broker Commission Rate", feesMap.get("brokerCommissionRate"));
                logField("    Broker Commission", feesMap.get("brokerCommission"));
                logField("    Exchange Fee", feesMap.get("exchangeFee"));
                logField("    Clearing Fee", feesMap.get("clearingFee"));
                logField("    Total Fees", feesMap.get("totalFees"));
                logField("    Net Settlement Amount", feesMap.get("netSettlementAmount"));
            }
        }
    }

    /**
     * Display rule execution summary
     */
    private void displayRuleExecutionSummary(Map<String, RuleResult> ruleResults) {
        LOGGER.info("\n--- RULE EXECUTION SUMMARY ---");

        if (ruleResults.isEmpty()) {
            LOGGER.info("No rules executed");
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (Map.Entry<String, RuleResult> entry : ruleResults.entrySet()) {
            RuleResult result = entry.getValue();
            if (result.isTriggered()) {
                successCount++;
            } else {
                failureCount++;
                LOGGER.warn("Rule execution failed: {} - {}", entry.getKey(), result.getMessage());
            }
        }

        LOGGER.info("Total Rules Executed: {}", ruleResults.size());
        LOGGER.info("Successful: {} | Failed: {}", successCount, failureCount);

        if (successCount > 0) {
            LOGGER.info("✓ Successfully executed {} enrichment rules", successCount);
        }
        if (failureCount > 0) {
            LOGGER.warn("✗ {} rules failed execution", failureCount);
        }
    }

    /**
     * Helper method to log field values safely
     */
    private void logField(String fieldName, Object value) {
        if (value != null) {
            LOGGER.info("{}: {}", fieldName, value);
        }
    }

    // ============================================================================
    // TRADE CREATION METHODS - Using Examples from the Document
    // ============================================================================

    /**
     * Create UK Equity Trade - Royal Dutch Shell PLC
     * Example from the document: GB00B03MLX29, Deutsche Bank AG counterparty
     */
    private TradeConfirmation createUKEquityTrade() {
        TradeConfirmation confirmation = new TradeConfirmation();

        // Header
        confirmation.setHeader(createHeader("TRD-20241224-001", "BANKGB2L", "DEUTDEFF"));

        // Trade
        confirmation.setTrade(createTrade(
            "TRD-001-2024",
            LocalDate.of(2024, 12, 24),
            "GB00B03MLX29",
            "EQUITY",
            "Royal Dutch Shell",
            "Deutsche Bank AG",
            "XLON",
            new BigDecimal("10000"),
            new BigDecimal("2750.50"),
            "GBP"
        ));

        return confirmation;
    }

    /**
     * Create US Government Bond Trade - 10Y Treasury Note
     * Example from the document: US912828XG93, JPMorgan Chase counterparty
     */
    private TradeConfirmation createUSBondTrade() {
        TradeConfirmation confirmation = new TradeConfirmation();

        // Header
        confirmation.setHeader(createHeader("BOND-20241224-002", "JPMUS33", "GSCCUS33"));

        // Trade
        confirmation.setTrade(createTrade(
            "BOND-002-2024",
            LocalDate.of(2024, 12, 24),
            "US912828XG93",
            "GOVERNMENT_BOND",
            "US Treasury",
            "JPMorgan Chase",
            "BONDDESK",
            new BigDecimal("1000000"),
            new BigDecimal("98.75"),
            "USD"
        ));

        return confirmation;
    }

    /**
     * Create German Equity Trade - SAP SE
     * Example from the document: DE0007164600, Barclays counterparty
     */
    private TradeConfirmation createGermanEquityTrade() {
        TradeConfirmation confirmation = new TradeConfirmation();

        // Header
        confirmation.setHeader(createHeader("TRD-20241224-003", "BARCGB22", "DEUTDEFF"));

        // Trade
        confirmation.setTrade(createTrade(
            "TRD-003-2024",
            LocalDate.of(2024, 12, 24),
            "DE0007164600",
            "EQUITY",
            "SAP SE",
            "Barclays Bank PLC",
            "XPAR",
            new BigDecimal("5000"),
            new BigDecimal("120.75"),
            "EUR"
        ));

        return confirmation;
    }

    /**
     * Create High-Value US Equity Trade - Apple Inc
     * Example from the document: US0378331005, Goldman Sachs counterparty
     * High value to trigger stress testing and priority settlement
     */
    private TradeConfirmation createHighValueUSEquityTrade() {
        TradeConfirmation confirmation = new TradeConfirmation();

        // Header
        confirmation.setHeader(createHeader("TRD-20241224-004", "GSCCUS33", "CHASUS33"));

        // Trade - High value trade (500,000 shares * $190 = $95M)
        confirmation.setTrade(createTrade(
            "TRD-004-2024",
            LocalDate.of(2024, 12, 24),
            "US0378331005",
            "EQUITY",
            "Apple Inc",
            "Goldman Sachs",
            "XNAS",
            new BigDecimal("500000"),
            new BigDecimal("190.00"),
            "USD"
        ));

        return confirmation;
    }

    /**
     * Helper method to create trade header
     */
    private dev.mars.apex.demo.financial.model.Header createHeader(String messageId, String sentBy, String sendTo) {
        dev.mars.apex.demo.financial.model.Header header = new dev.mars.apex.demo.financial.model.Header();
        header.setMessageId(messageId);
        header.setSentBy(sentBy);
        header.setSendTo(sendTo);
        header.setCreationTimestamp(java.time.Instant.now());
        return header;
    }

    /**
     * Helper method to create trade
     */
    private dev.mars.apex.demo.financial.model.Trade createTrade(
            String tradeId, LocalDate tradeDate, String instrumentId, String instrumentType,
            String issuer, String counterpartyName, String tradingVenue,
            BigDecimal quantity, BigDecimal price, String currency) {

        dev.mars.apex.demo.financial.model.Trade trade = new dev.mars.apex.demo.financial.model.Trade();

        // Trade Header
        TradeHeader tradeHeader = new TradeHeader();
        PartyTradeIdentifier partyTradeIdentifier = new PartyTradeIdentifier();
        partyTradeIdentifier.setTradeId(tradeId);
        tradeHeader.setPartyTradeIdentifier(partyTradeIdentifier);
        tradeHeader.setTradeDate(tradeDate);
        trade.setTradeHeader(tradeHeader);

        // Security
        Security security = new Security();
        security.setInstrumentId(instrumentId);
        security.setInstrumentType(instrumentType);
        security.setIssuer(issuer);
        trade.setSecurity(security);

        // Counterparty
        Counterparty counterparty = new Counterparty();
        counterparty.setPartyName(counterpartyName);
        trade.setCounterparty(counterparty);

        // Trade details
        trade.setTradingVenue(tradingVenue);
        trade.setQuantity(quantity);
        trade.setPrice(price);
        trade.setCurrency(currency);

        return trade;
    }

    /**
     * Display rule results for a single rule execution
     */
    private void displayRuleResults(RuleResult result) {
        LOGGER.info("\n" + "=".repeat(60));
        LOGGER.info("RULE EXECUTION RESULTS");
        LOGGER.info("=".repeat(60));

        if (result.isTriggered()) {
            LOGGER.info("✓ PASSED: Rule was triggered successfully");
            LOGGER.info("Rule Name: {}", result.getRuleName());
            LOGGER.info("Message: {}", result.getMessage());
        } else {
            LOGGER.info("✗ FAILED: Rule was not triggered");
            LOGGER.info("Rule Name: {}", result.getRuleName());
        }

        LOGGER.info("Result Type: {}", result.getResultType());
        LOGGER.info("Timestamp: {}", result.getTimestamp());
    }

    /**
     * Display single rule result summary
     */
    private void displaySingleRuleResult(RuleResult result) {
        LOGGER.info("\n--- SINGLE RULE RESULT SUMMARY ---");

        if (result.isTriggered()) {
            LOGGER.info("✓ Rule executed successfully: {}", result.getRuleName());
        } else {
            LOGGER.info("✗ Rule execution failed: {}", result.getRuleName());
        }

        LOGGER.info("Message: {}", result.getMessage());
        LOGGER.info("Result Type: {}", result.getResultType());
    }

    /**
     * Main method for running the comprehensive financial settlement demo.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            ComprehensiveFinancialSettlementDemo demo = new ComprehensiveFinancialSettlementDemo();
            demo.runComprehensiveDemo();
        } catch (Exception e) {
            LOGGER.error("Error running comprehensive financial settlement demo: {}", e.getMessage(), e);
        }
    }

}