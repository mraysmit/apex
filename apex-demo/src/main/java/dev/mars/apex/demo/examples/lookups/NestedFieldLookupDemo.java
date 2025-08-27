package dev.mars.apex.demo.examples.lookups;

import dev.mars.apex.demo.model.TradeSettlement;
import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRule;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates nested field reference lookup using object navigation.
 * This example shows how to navigate through nested objects to create lookup keys
 * for country-specific settlement information.
 *
 * Pattern Demonstrated: lookup-key: "#trade.counterparty.countryCode"
 * Use Case: Country-specific settlement information lookup
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-22
 * @version 1.0
 */
public class NestedFieldLookupDemo extends AbstractLookupDemo {
    
    public static void main(String[] args) {
        new NestedFieldLookupDemo().runDemo();
    }
    
    @Override
    protected String getDemoTitle() {
        return "Nested Field Reference Lookup Demo - Settlement Information";
    }
    
    @Override
    protected String getDemoDescription() {
        return "Demonstrates nested field navigation pattern '#trade.counterparty.countryCode' to enrich\n" +
               "   trade settlements with country-specific settlement information. This pattern shows how to\n" +
               "   navigate through nested object hierarchies (TradeSettlement -> TradeB -> Counterparty -> countryCode)\n" +
               "   to create lookup keys that match against country-specific settlement data including\n" +
               "   regulatory zones, settlement systems, standard settlement days, and custodian banks.";
    }
    
    @Override
    protected String getYamlConfigPath() {
        return "examples/lookups/nested-field-lookup.yaml";
    }
    
    @Override
    protected void loadConfiguration() throws Exception {
        System.out.println("📁 Loading YAML configuration from: " + getYamlConfigPath());
        
        // Load the YAML configuration from classpath
        ruleConfiguration = yamlLoader.loadFromClasspath(getYamlConfigPath());
        rulesEngine = yamlService.createRulesEngineFromYamlConfig(ruleConfiguration);
        
        System.out.println("✅ Configuration loaded successfully");
        System.out.println("   - Enrichments: 1 (country-settlement-enrichment)");
        System.out.println("   - Validations: 4 (trade-exists, counterparty-exists, country-code-format, settlement-amount-positive)");
        System.out.println("   - Lookup Dataset: 12 countries with settlement information");
        System.out.println("   - Nested Navigation: trade.counterparty.countryCode");
    }
    
    @Override
    protected List<TradeSettlement> generateTestData() {
        System.out.println("🏭 Generating test trade settlements...");
        
        List<TradeSettlement> settlements = new ArrayList<>();
        LocalDateTime baseTradeTime = LocalDateTime.now().minusDays(2);
        LocalDate baseSettlementDate = LocalDate.now().plusDays(2);
        
        // Create diverse settlement data with different counterparty countries
        settlements.add(createTradeSettlement(
            "SETT-001",
            "TRD-001",
            "AAPL",
            new BigDecimal("1000"),
            new BigDecimal("150.25"),
            baseTradeTime,
            "CP-US-001",
            "Goldman Sachs",
            "US",
            "New York",
            "549300MARQ7EUFB1G129",
            baseSettlementDate,
            "PENDING",
            new BigDecimal("150250.00"),
            "USD"
        ));
        
        settlements.add(createTradeSettlement(
            "SETT-002",
            "TRD-002",
            "VOD.L",
            new BigDecimal("5000"),
            new BigDecimal("1.45"),
            baseTradeTime.plusHours(2),
            "CP-GB-001",
            "Barclays Capital",
            "GB",
            "London",
            "G5GSEF7VJP5I7OUK5573",
            baseSettlementDate,
            "PENDING",
            new BigDecimal("7250.00"),
            "GBP"
        ));
        
        settlements.add(createTradeSettlement(
            "SETT-003",
            "TRD-003",
            "SAP.DE",
            new BigDecimal("800"),
            new BigDecimal("120.50"),
            baseTradeTime.plusHours(4),
            "CP-DE-001",
            "Deutsche Bank AG",
            "DE",
            "Frankfurt",
            "7LTWFZYICNSX8D621K86",
            baseSettlementDate,
            "CONFIRMED",
            new BigDecimal("96400.00"),
            "EUR"
        ));
        
        settlements.add(createTradeSettlement(
            "SETT-004",
            "TRD-004",
            "7203.T",
            new BigDecimal("2000"),
            new BigDecimal("2150.00"),
            baseTradeTime.plusHours(6),
            "CP-JP-001",
            "Nomura Securities",
            "JP",
            "Tokyo",
            "549300XK8DIEQMB8GH67",
            baseSettlementDate,
            "PENDING",
            new BigDecimal("4300000.00"),
            "JPY"
        ));
        
        settlements.add(createTradeSettlement(
            "SETT-005",
            "TRD-005",
            "0700.HK",
            new BigDecimal("10000"),
            new BigDecimal("45.80"),
            baseTradeTime.plusHours(8),
            "CP-HK-001",
            "HSBC Securities",
            "HK",
            "Hong Kong",
            "MPID4BFWD7B6F1Q3JC15",
            baseSettlementDate,
            "PENDING",
            new BigDecimal("458000.00"),
            "HKD"
        ));
        
        settlements.add(createTradeSettlement(
            "SETT-006",
            "TRD-006",
            "D05.SI",
            new BigDecimal("3000"),
            new BigDecimal("28.50"),
            baseTradeTime.plusHours(10),
            "CP-SG-001",
            "DBS Vickers",
            "SG",
            "Singapore",
            "549300SGI6RJBSXJSM94",
            baseSettlementDate,
            "CONFIRMED",
            new BigDecimal("85500.00"),
            "SGD"
        ));
        
        settlements.add(createTradeSettlement(
            "SETT-007",
            "TRD-007",
            "MC.PA",
            new BigDecimal("1500"),
            new BigDecimal("285.75"),
            baseTradeTime.plusHours(12),
            "CP-FR-001",
            "BNP Paribas",
            "FR",
            "Paris",
            "R0MUWSFPU8MPRO8K5P83",
            baseSettlementDate,
            "PENDING",
            new BigDecimal("428625.00"),
            "EUR"
        ));
        
        settlements.add(createTradeSettlement(
            "SETT-008",
            "TRD-008",
            "NESN.SW",
            new BigDecimal("600"),
            new BigDecimal("108.20"),
            baseTradeTime.plusHours(14),
            "CP-CH-001",
            "UBS AG",
            "CH",
            "Zurich",
            "BFM8T61CT2L1QCEMIK50",
            baseSettlementDate,
            "PENDING",
            new BigDecimal("64920.00"),
            "CHF"
        ));
        
        System.out.println("✅ Generated " + settlements.size() + " test settlements");
        System.out.println("   - Countries: US, GB, DE, JP, HK, SG, FR, CH");
        System.out.println("   - Instruments: Equities from major exchanges");
        System.out.println("   - Counterparties: Major investment banks");
        System.out.println("   - Nested Navigation: Each settlement contains trade -> counterparty -> countryCode");
        
        return settlements;
    }
    
    /**
     * Helper method to create a TradeSettlement with nested TradeB and Counterparty objects
     */
    private TradeSettlement createTradeSettlement(String settlementId, String tradeId, String instrumentId,
                                                 BigDecimal quantity, BigDecimal price, LocalDateTime tradeDate,
                                                 String counterpartyId, String counterpartyName, String countryCode,
                                                 String city, String lei, LocalDate settlementDate, String status,
                                                 BigDecimal settlementAmount, String currency) {
        
        // Create nested counterparty
        TradeSettlement.Counterparty counterparty = new TradeSettlement.Counterparty(
            counterpartyId, counterpartyName, countryCode, city, lei
        );
        
        // Create nested trade
        TradeSettlement.Trade trade = new TradeSettlement.Trade(
            tradeId, instrumentId, quantity, price, tradeDate, counterparty
        );
        
        // Create settlement with nested objects
        return new TradeSettlement(settlementId, trade, settlementDate, status, settlementAmount, currency);
    }
    
    @Override
    protected List<TradeSettlement> processData(List<?> data) throws Exception {
        System.out.println("⚙️  Processing settlements with country-specific enrichment...");

        List<TradeSettlement> results = new ArrayList<>();

        for (Object item : data) {
            if (item instanceof TradeSettlement) {
                TradeSettlement settlement = (TradeSettlement) item;

                // Use actual APEX rules engine to process the settlement
                TradeSettlement enriched = processSettlementWithApexEngine(settlement);
                results.add(enriched);

                // Log the lookup process
                String countryCode = settlement.getTrade() != null &&
                                   settlement.getTrade().getCounterparty() != null ?
                                   settlement.getTrade().getCounterparty().getCountryCode() : "NULL";
                System.out.println("   🔍 Processed " + settlement.getSettlementId() +
                                 " (Country: " + countryCode + ") -> " +
                                 "System: " + enriched.getSettlementSystem() +
                                 ", Days: " + enriched.getStandardSettlementDays() +
                                 ", Zone: " + enriched.getRegulatoryZone());
            }
        }

        return results;
    }

    /**
     * Process settlement using the actual APEX rules engine with the loaded YAML configuration.
     * This replaces the previous simulation approach with real APEX functionality.
     */
    private TradeSettlement processSettlementWithApexEngine(TradeSettlement original) throws Exception {
        // Create a copy of the original settlement to enrich
        TradeSettlement enriched = new TradeSettlement(
            original.getSettlementId(),
            original.getTrade(),
            original.getSettlementDate(),
            original.getStatus(),
            original.getSettlementAmount(),
            original.getCurrency()
        );

        // Convert settlement to Map for APEX processing
        Map<String, Object> settlementData = convertSettlementToMap(enriched);

        // Apply enrichments using the APEX rules engine
        if (ruleConfiguration != null && ruleConfiguration.getEnrichments() != null) {
            for (YamlEnrichment enrichment : ruleConfiguration.getEnrichments()) {
                if (enrichment.getEnabled() != null && enrichment.getEnabled()) {
                    applyEnrichmentToSettlement(enrichment, settlementData);
                }
            }
        }

        // Apply validation rules using the APEX rules engine
        if (ruleConfiguration != null && ruleConfiguration.getRules() != null) {
            for (YamlRule rule : ruleConfiguration.getRules()) {
                if (rule.getEnabled() != null && rule.getEnabled()) {
                    applyValidationRuleToSettlement(rule, settlementData);
                }
            }
        }

        // Convert enriched data back to TradeSettlement object
        updateSettlementFromMap(enriched, settlementData);

        return enriched;
    }

    @Override
    protected List<TradeSettlement> generateErrorTestData() {
        System.out.println("⚠️  Generating error scenario test data...");

        List<TradeSettlement> errorSettlements = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now();
        LocalDate settlementDate = LocalDate.now().plusDays(2);

        // Null trade object
        TradeSettlement nullTrade = new TradeSettlement(
            "ERR-001",
            null, // Null trade
            settlementDate,
            "PENDING",
            new BigDecimal("1000.00"),
            "USD"
        );
        errorSettlements.add(nullTrade);

        // TradeB with null counterparty
        TradeSettlement.Trade tradeWithNullCounterparty = new TradeSettlement.Trade(
            "TRD-ERR-002",
            "TEST-INST",
            new BigDecimal("100"),
            new BigDecimal("10.00"),
            baseTime,
            null // Null counterparty
        );
        errorSettlements.add(new TradeSettlement(
            "ERR-002",
            tradeWithNullCounterparty,
            settlementDate,
            "PENDING",
            new BigDecimal("1000.00"),
            "USD"
        ));

        // Counterparty with null country code
        TradeSettlement.Counterparty counterpartyWithNullCountry = new TradeSettlement.Counterparty(
            "CP-ERR-003",
            "Test Counterparty",
            null, // Null country code
            "Test City",
            "TEST123456789012345"
        );
        TradeSettlement.Trade tradeWithNullCountryCode = new TradeSettlement.Trade(
            "TRD-ERR-003",
            "TEST-INST",
            new BigDecimal("100"),
            new BigDecimal("10.00"),
            baseTime,
            counterpartyWithNullCountry
        );
        errorSettlements.add(new TradeSettlement(
            "ERR-003",
            tradeWithNullCountryCode,
            settlementDate,
            "PENDING",
            new BigDecimal("1000.00"),
            "USD"
        ));

        // Invalid country code format
        TradeSettlement.Counterparty counterpartyWithInvalidCountry = new TradeSettlement.Counterparty(
            "CP-ERR-004",
            "Test Counterparty",
            "INVALID", // Invalid country code format
            "Test City",
            "TEST123456789012345"
        );
        TradeSettlement.Trade tradeWithInvalidCountryCode = new TradeSettlement.Trade(
            "TRD-ERR-004",
            "TEST-INST",
            new BigDecimal("100"),
            new BigDecimal("10.00"),
            baseTime,
            counterpartyWithInvalidCountry
        );
        errorSettlements.add(new TradeSettlement(
            "ERR-004",
            tradeWithInvalidCountryCode,
            settlementDate,
            "PENDING",
            new BigDecimal("1000.00"),
            "USD"
        ));

        // Unknown country code (valid format but not in dataset)
        TradeSettlement.Counterparty counterpartyWithUnknownCountry = new TradeSettlement.Counterparty(
            "CP-ERR-005",
            "Test Counterparty",
            "ZZ", // Unknown country code
            "Test City",
            "TEST123456789012345"
        );
        TradeSettlement.Trade tradeWithUnknownCountryCode = new TradeSettlement.Trade(
            "TRD-ERR-005",
            "TEST-INST",
            new BigDecimal("100"),
            new BigDecimal("10.00"),
            baseTime,
            counterpartyWithUnknownCountry
        );
        errorSettlements.add(new TradeSettlement(
            "ERR-005",
            tradeWithUnknownCountryCode,
            settlementDate,
            "PENDING",
            new BigDecimal("1000.00"),
            "USD"
        ));

        // Negative settlement amount
        TradeSettlement.Counterparty validCounterparty = new TradeSettlement.Counterparty(
            "CP-ERR-006",
            "Test Counterparty",
            "US",
            "New York",
            "TEST123456789012345"
        );
        TradeSettlement.Trade validTrade = new TradeSettlement.Trade(
            "TRD-ERR-006",
            "TEST-INST",
            new BigDecimal("100"),
            new BigDecimal("10.00"),
            baseTime,
            validCounterparty
        );
        errorSettlements.add(new TradeSettlement(
            "ERR-006",
            validTrade,
            settlementDate,
            "PENDING",
            new BigDecimal("-1000.00"), // Negative amount
            "USD"
        ));

        System.out.println("✅ Generated " + errorSettlements.size() + " error scenario settlements");

        return errorSettlements;
    }

    /**
     * Convert TradeSettlement to Map for APEX processing.
     */
    private Map<String, Object> convertSettlementToMap(TradeSettlement settlement) {
        Map<String, Object> map = new HashMap<>();
        map.put("settlementId", settlement.getSettlementId());
        map.put("settlementDate", settlement.getSettlementDate());
        map.put("status", settlement.getStatus());
        map.put("settlementAmount", settlement.getSettlementAmount());
        map.put("currency", settlement.getCurrency());

        // Add trade information
        if (settlement.getTrade() != null) {
            Map<String, Object> tradeMap = new HashMap<>();
            tradeMap.put("tradeId", settlement.getTrade().getTradeId());
            tradeMap.put("instrumentId", settlement.getTrade().getInstrumentId());
            tradeMap.put("quantity", settlement.getTrade().getQuantity());
            tradeMap.put("price", settlement.getTrade().getPrice());
            tradeMap.put("tradeDate", settlement.getTrade().getTradeDate());

            // Add counterparty information
            if (settlement.getTrade().getCounterparty() != null) {
                Map<String, Object> counterpartyMap = new HashMap<>();
                counterpartyMap.put("counterpartyId", settlement.getTrade().getCounterparty().getCounterpartyId());
                counterpartyMap.put("name", settlement.getTrade().getCounterparty().getName());
                counterpartyMap.put("countryCode", settlement.getTrade().getCounterparty().getCountryCode());
                counterpartyMap.put("city", settlement.getTrade().getCounterparty().getCity());
                counterpartyMap.put("legalEntityIdentifier", settlement.getTrade().getCounterparty().getLegalEntityIdentifier());
                tradeMap.put("counterparty", counterpartyMap);
            }
            map.put("trade", tradeMap);
        }

        // Add existing enriched fields if present
        if (settlement.getCountryName() != null) map.put("countryName", settlement.getCountryName());
        if (settlement.getRegulatoryZone() != null) map.put("regulatoryZone", settlement.getRegulatoryZone());
        if (settlement.getTimeZone() != null) map.put("timeZone", settlement.getTimeZone());
        if (settlement.getSettlementSystem() != null) map.put("settlementSystem", settlement.getSettlementSystem());
        if (settlement.getStandardSettlementDays() != null) map.put("standardSettlementDays", settlement.getStandardSettlementDays());
        if (settlement.getHolidayCalendar() != null) map.put("holidayCalendar", settlement.getHolidayCalendar());
        if (settlement.getSettlementFee() != null) map.put("settlementFee", settlement.getSettlementFee());
        if (settlement.getCustodianBank() != null) map.put("custodianBank", settlement.getCustodianBank());

        return map;
    }

    /**
     * Apply enrichment to settlement data using APEX engine.
     */
    private void applyEnrichmentToSettlement(YamlEnrichment enrichment, Map<String, Object> settlementData) throws Exception {
        // Apply lookup enrichment directly using the YAML configuration
        if (enrichment.getLookupConfig() != null) {
            applyLookupEnrichmentToSettlement(enrichment, settlementData);
        }
    }

    /**
     * Apply lookup enrichment using the APEX engine.
     */
    private void applyLookupEnrichmentToSettlement(YamlEnrichment enrichment, Map<String, Object> settlementData) throws Exception {
        // Create evaluation context
        StandardEvaluationContext context = new StandardEvaluationContext();
        settlementData.forEach(context::setVariable);

        // Evaluate condition
        if (enrichment.getCondition() != null) {
            ExpressionEvaluatorService evaluator = new ExpressionEvaluatorService();
            Boolean conditionResult = evaluator.evaluate(enrichment.getCondition(), context, Boolean.class);
            if (conditionResult == null || !conditionResult) {
                return; // Condition not met, skip enrichment
            }
        }

        // Apply lookup enrichment using the dataset from YAML
        if (enrichment.getLookupConfig() != null && enrichment.getLookupConfig().getLookupDataset() != null) {
            String lookupKey = enrichment.getLookupConfig().getLookupKey();
            if (lookupKey != null) {
                ExpressionEvaluatorService evaluator = new ExpressionEvaluatorService();
                String keyValue = evaluator.evaluate(lookupKey, context, String.class);

                // Find matching data in the lookup dataset
                var dataset = enrichment.getLookupConfig().getLookupDataset();
                if (dataset.getData() != null) {
                    String keyField = dataset.getKeyField();
                    for (Map<String, Object> dataRow : dataset.getData()) {
                        if (keyValue != null && keyValue.equals(dataRow.get(keyField))) {
                            // Apply field mappings
                            if (enrichment.getFieldMappings() != null) {
                                for (var mapping : enrichment.getFieldMappings()) {
                                    Object sourceValue = dataRow.get(mapping.getSourceField());
                                    if (sourceValue != null) {
                                        settlementData.put(mapping.getTargetField(), sourceValue);
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Apply validation rule to settlement data.
     */
    private void applyValidationRuleToSettlement(YamlRule rule, Map<String, Object> settlementData) throws Exception {
        if (rule.getCondition() != null) {
            StandardEvaluationContext context = new StandardEvaluationContext();
            settlementData.forEach(context::setVariable);

            ExpressionEvaluatorService evaluator = new ExpressionEvaluatorService();
            Boolean result = evaluator.evaluate(rule.getCondition(), context, Boolean.class);

            if (result == null || !result) {
                System.out.println("   ⚠️  Validation failed: " + rule.getName() + " - " + rule.getMessage());
            }
        }
    }

    /**
     * Update TradeSettlement from enriched Map data.
     */
    private void updateSettlementFromMap(TradeSettlement settlement, Map<String, Object> settlementData) {
        // Update enriched fields
        if (settlementData.containsKey("countryName")) {
            settlement.setCountryName((String) settlementData.get("countryName"));
        }
        if (settlementData.containsKey("regulatoryZone")) {
            settlement.setRegulatoryZone((String) settlementData.get("regulatoryZone"));
        }
        if (settlementData.containsKey("timeZone")) {
            settlement.setTimeZone((String) settlementData.get("timeZone"));
        }
        if (settlementData.containsKey("settlementSystem")) {
            settlement.setSettlementSystem((String) settlementData.get("settlementSystem"));
        }
        if (settlementData.containsKey("standardSettlementDays")) {
            Object standardSettlementDays = settlementData.get("standardSettlementDays");
            if (standardSettlementDays instanceof Number) {
                settlement.setStandardSettlementDays(((Number) standardSettlementDays).intValue());
            }
        }
        if (settlementData.containsKey("holidayCalendar")) {
            settlement.setHolidayCalendar((String) settlementData.get("holidayCalendar"));
        }
        if (settlementData.containsKey("settlementFee")) {
            Object settlementFee = settlementData.get("settlementFee");
            if (settlementFee instanceof Number) {
                settlement.setSettlementFee(new BigDecimal(settlementFee.toString()));
            }
        }
        if (settlementData.containsKey("custodianBank")) {
            settlement.setCustodianBank((String) settlementData.get("custodianBank"));
        }
    }
}
