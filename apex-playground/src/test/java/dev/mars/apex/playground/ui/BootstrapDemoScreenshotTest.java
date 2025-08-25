package dev.mars.apex.playground.ui;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Screenshot tests specifically for Bootstrap Demo scenarios.
 * These screenshots will be used to enhance the Bootstrap Demos Guide documentation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=WARN",
    "logging.level.org.springframework=WARN"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Bootstrap Demo Screenshots for Documentation")
class BootstrapDemoScreenshotTest {

    @LocalServerPort
    private int port;

    private PlaygroundScreenshotUtil screenshotUtil;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        screenshotUtil = new PlaygroundScreenshotUtil(baseUrl, "docs/screenshots");
    }

    @AfterEach
    void tearDown() {
        if (screenshotUtil != null) {
            screenshotUtil.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("OTC Options Bootstrap Demo - Empty Playground")
    void testOtcOptionsBootstrapEmpty() throws IOException {
        String screenshotPath = screenshotUtil.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
            "otc_options_empty_playground",
            null,
            true
        );
        
        assertNotNull(screenshotPath);
        assertTrue(screenshotPath.contains("otc_options_empty_playground"));
        System.out.println("OTC Options empty playground screenshot: " + screenshotPath);
    }

    @Test
    @Order(2)
    @DisplayName("OTC Options Bootstrap Demo - With Sample Data")
    void testOtcOptionsBootstrapWithData() throws IOException {
        // Real OTC Options XML data from apex-demo
        String otcOptionsXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!-- OTC Options Bootstrap Demo - Sample Data -->
            <OtcOptions>
                <!-- Option 1: Natural Gas Call -->
                <OtcOption id="option-1">
                    <tradeDate>2025-08-02</tradeDate>
                    <buyerParty>GOLDMAN_SACHS</buyerParty>
                    <sellerParty>JP_MORGAN</sellerParty>
                    <optionType>Call</optionType>
                    <underlyingAsset>
                        <commodity>Natural Gas</commodity>
                        <unit>MMBtu</unit>
                    </underlyingAsset>
                    <strikePrice currency="USD">3.50</strikePrice>
                    <notionalQuantity>10000</notionalQuantity>
                    <expiryDate>2025-12-28</expiryDate>
                    <settlementType>Cash</settlementType>
                </OtcOption>

                <!-- Option 2: Brent Crude Oil Put -->
                <OtcOption id="option-2">
                    <tradeDate>2025-08-02</tradeDate>
                    <buyerParty>MORGAN_STANLEY</buyerParty>
                    <sellerParty>CITI</sellerParty>
                    <optionType>Put</optionType>
                    <underlyingAsset>
                        <commodity>Brent Crude Oil</commodity>
                        <unit>Barrel</unit>
                    </underlyingAsset>
                    <strikePrice currency="USD">75.00</strikePrice>
                    <notionalQuantity>1000</notionalQuantity>
                    <expiryDate>2026-03-15</expiryDate>
                    <settlementType>Physical</settlementType>
                </OtcOption>
            </OtcOptions>""";

        // Real OTC Options Bootstrap YAML rules from apex-demo
        String otcOptionsYaml = """
            # APEX Rules Engine - OTC Options Bootstrap Configuration
            metadata:
              name: "OTC Options Bootstrap Configuration"
              version: "1.0.0"
              description: "Complete OTC Options validation and enrichment"
              type: "bootstrap"
              business-domain: "OTC Derivatives"

            rules:
              # Structural Validation Rules
              - id: "trade-date-required"
                name: "Trade Date Required"
                condition: "#tradeDate != null"
                message: "Trade date is required for all OTC options"
                severity: "ERROR"
                priority: 1
                categories: ["structural", "required-fields"]

              - id: "parties-required"
                name: "Counterparties Required"
                condition: "#buyerParty != null && #sellerParty != null"
                message: "Both buyer and seller parties are required"
                severity: "ERROR"
                priority: 1
                categories: ["structural", "counterparty"]

              - id: "option-type-valid"
                name: "Valid Option Type"
                condition: "#optionType != null && (#optionType == 'Call' || #optionType == 'Put')"
                message: "Option type must be either 'Call' or 'Put'"
                severity: "ERROR"
                priority: 1
                categories: ["structural", "option-validation"]

              - id: "strike-price-positive"
                name: "Positive Strike Price"
                condition: "#strikePrice != null && #strikePrice > 0"
                message: "Strike price must be positive"
                severity: "ERROR"
                priority: 2
                categories: ["financial", "validation"]
            """;

        PlaygroundScreenshotUtil.PlaygroundData data = 
            new PlaygroundScreenshotUtil.PlaygroundData(otcOptionsXml, otcOptionsYaml, true);

        String screenshotPath = screenshotUtil.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
            "otc_options_with_data",
            data,
            true
        );
        
        assertNotNull(screenshotPath);
        assertTrue(screenshotPath.contains("otc_options_with_data"));
        System.out.println("OTC Options with data screenshot: " + screenshotPath);
    }

    @Test
    @Order(3)
    @DisplayName("Commodity Swap Bootstrap Demo")
    void testCommoditySwapBootstrap() throws IOException {
        // Real Commodity Swap JSON data for bootstrap demo
        String commoditySwapJson = """
            {
              "commoditySwaps": [
                {
                  "tradeId": "CS-2025-001",
                  "tradeDate": "2025-08-02",
                  "effectiveDate": "2025-08-05",
                  "maturityDate": "2026-08-05",
                  "commodity": "WTI Crude Oil",
                  "notionalAmount": 50000000,
                  "currency": "USD",
                  "fixedPrice": 72.50,
                  "payerParty": "ENERGY_CORP_A",
                  "receiverParty": "HEDGE_FUND_B",
                  "settlementFrequency": "Monthly",
                  "settlementType": "Cash",
                  "exchange": "NYMEX",
                  "contractSize": 1000,
                  "unit": "Barrel"
                },
                {
                  "tradeId": "CS-2025-002",
                  "tradeDate": "2025-08-02",
                  "effectiveDate": "2025-09-01",
                  "maturityDate": "2026-09-01",
                  "commodity": "Henry Hub Natural Gas",
                  "notionalAmount": 25000000,
                  "currency": "USD",
                  "fixedPrice": 3.75,
                  "payerParty": "UTILITY_COMPANY_C",
                  "receiverParty": "TRADING_HOUSE_D",
                  "settlementFrequency": "Monthly",
                  "settlementType": "Physical",
                  "exchange": "NYMEX",
                  "contractSize": 10000,
                  "unit": "MMBtu"
                }
              ]
            }""";

        // Real Commodity Swap Bootstrap YAML rules from apex-demo
        String commoditySwapYaml = """
            # APEX Rules Engine - Commodity Swap Validation Bootstrap
            metadata:
              name: "Commodity Swap Validation Bootstrap"
              version: "1.0"
              description: "Complete commodity derivatives validation and enrichment"
              type: "bootstrap"
              business-domain: "Commodity Derivatives"

            rules:
              # Basic Field Validation
              - id: "trade-id-required"
                name: "Trade ID Required"
                condition: "#tradeId != null && #tradeId.length() > 0"
                message: "Trade ID is mandatory for all commodity swaps"
                severity: "ERROR"
                priority: 1
                categories: ["structural", "required-fields"]

              - id: "trade-date-required"
                name: "Trade Date Required"
                condition: "#tradeDate != null"
                message: "Trade date is required"
                severity: "ERROR"
                priority: 1
                categories: ["structural", "dates"]

              - id: "notional-positive"
                name: "Positive Notional Amount"
                condition: "#notionalAmount != null && #notionalAmount > 0"
                message: "Notional amount must be positive"
                severity: "ERROR"
                priority: 1
                categories: ["financial", "amounts"]

              - id: "maturity-after-effective"
                name: "Maturity After Effective Date"
                condition: "#maturityDate != null && #effectiveDate != null && #maturityDate.isAfter(#effectiveDate)"
                message: "Maturity date must be after effective date"
                severity: "ERROR"
                priority: 2
                categories: ["business-logic", "dates"]

              - id: "high-value-approval"
                name: "High Value Trade Approval"
                condition: "#notionalAmount > 10000000"
                message: "High value trades require additional approval"
                severity: "WARNING"
                priority: 3
                categories: ["business-logic", "approval"]
            """;

        PlaygroundScreenshotUtil.PlaygroundData data = 
            new PlaygroundScreenshotUtil.PlaygroundData(commoditySwapJson, commoditySwapYaml, true);

        String screenshotPath = screenshotUtil.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
            "commodity_swap_demo",
            data,
            true
        );
        
        assertNotNull(screenshotPath);
        assertTrue(screenshotPath.contains("commodity_swap_demo"));
        System.out.println("Commodity Swap demo screenshot: " + screenshotPath);
    }

    @Test
    @Order(4)
    @DisplayName("Custody Auto-Repair Bootstrap Demo")
    void testCustodyAutoRepairBootstrap() throws IOException {
        // Real Custody Settlement data for Asian markets
        String custodySettlementJson = """
            {
              "settlementInstructions": [
                {
                  "instructionId": "SI-JP-2025-001",
                  "tradeDate": "2025-08-02",
                  "settlementDate": "2025-08-04",
                  "market": "JP",
                  "clientId": "PREMIUM_CLIENT_TOKYO",
                  "instrumentId": "7203.T",
                  "instrumentName": "Toyota Motor Corp",
                  "quantity": 10000,
                  "price": 2850.00,
                  "currency": "JPY",
                  "settlementAmount": 28500000,
                  "custodian": "MIZUHO_TRUST",
                  "status": "FAILED",
                  "failureReason": "INSUFFICIENT_CASH",
                  "failureTime": "2025-08-04T09:30:00+09:00"
                },
                {
                  "instructionId": "SI-HK-2025-002",
                  "tradeDate": "2025-08-02",
                  "settlementDate": "2025-08-04",
                  "market": "HK",
                  "clientId": "STANDARD_CLIENT_HK",
                  "instrumentId": "0700.HK",
                  "instrumentName": "Tencent Holdings Ltd",
                  "quantity": 5000,
                  "price": 385.60,
                  "currency": "HKD",
                  "settlementAmount": 1928000,
                  "custodian": "HSBC_CUSTODY",
                  "status": "PENDING_REPAIR",
                  "failureReason": "ACCOUNT_BLOCKED",
                  "failureTime": "2025-08-04T10:15:00+08:00"
                }
              ]
            }""";

        // Real Custody Auto-Repair Bootstrap YAML rules from apex-demo
        String custodyRepairYaml = """
            # APEX Rules Engine - Custody Auto-Repair Bootstrap Configuration
            metadata:
              name: "Custody Auto-Repair Bootstrap Rules"
              version: "1.0"
              description: "Standing Instruction auto-repair for Asian markets"
              type: "bootstrap"
              business-domain: "Custody Operations"

            rules:
              # Eligibility Pre-Checks
              - id: "client-authorization-check"
                name: "Client Authorization Check"
                condition: "#clientId != null && #status == 'FAILED'"
                message: "Client must be authorized for auto-repair"
                severity: "INFO"
                priority: 1
                categories: ["eligibility", "authorization"]

              - id: "market-hours-check"
                name: "Market Hours Check"
                condition: "#market != null && (#market == 'JP' || #market == 'HK' || #market == 'SG')"
                message: "Auto-repair only during Asian market hours"
                severity: "INFO"
                priority: 1
                categories: ["eligibility", "timing"]

              # Weighted Decision Rules
              - id: "premium-client-repair"
                name: "Premium Client Auto-Repair"
                condition: "#clientId.contains('PREMIUM') && #settlementAmount < 100000000"
                message: "Premium client eligible for full auto-repair"
                severity: "INFO"
                priority: 2
                categories: ["decision", "client-tier"]

              - id: "standard-client-partial"
                name: "Standard Client Partial Repair"
                condition: "#clientId.contains('STANDARD') && #settlementAmount < 50000000"
                message: "Standard client eligible for partial repair"
                severity: "WARNING"
                priority: 2
                categories: ["decision", "client-tier"]

              - id: "high-value-manual-review"
                name: "High Value Manual Review"
                condition: "#settlementAmount >= 100000000"
                message: "High value transactions require manual review"
                severity: "ERROR"
                priority: 1
                categories: ["decision", "risk-management"]
            """;

        PlaygroundScreenshotUtil.PlaygroundData data =
            new PlaygroundScreenshotUtil.PlaygroundData(custodySettlementJson, custodyRepairYaml, true);

        String screenshotPath = screenshotUtil.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
            "custody_auto_repair_demo",
            data,
            true
        );

        assertNotNull(screenshotPath);
        assertTrue(screenshotPath.contains("custody_auto_repair_demo"));
        System.out.println("Custody Auto-Repair demo screenshot: " + screenshotPath);
    }

    @Test
    @Order(5)
    @DisplayName("Playground Interface Overview")
    void testPlaygroundInterfaceOverview() throws IOException {
        // Real OTC Options XML data from the actual bootstrap demo
        String otcOptionsXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!-- OTC Options Bootstrap Demo - Sample Data -->
            <OtcOptions>
                <!-- Option 1: Natural Gas Call -->
                <OtcOption id="option-1">
                    <tradeDate>2025-08-02</tradeDate>
                    <buyerParty>GOLDMAN_SACHS</buyerParty>
                    <sellerParty>JP_MORGAN</sellerParty>
                    <optionType>Call</optionType>
                    <underlyingAsset>
                        <commodity>Natural Gas</commodity>
                        <unit>MMBtu</unit>
                    </underlyingAsset>
                    <strikePrice currency="USD">3.50</strikePrice>
                    <notionalQuantity>10000</notionalQuantity>
                    <expiryDate>2025-12-28</expiryDate>
                    <settlementType>Cash</settlementType>
                </OtcOption>

                <!-- Option 2: Brent Crude Oil Put -->
                <OtcOption id="option-2">
                    <tradeDate>2025-08-02</tradeDate>
                    <buyerParty>MORGAN_STANLEY</buyerParty>
                    <sellerParty>CITI</sellerParty>
                    <optionType>Put</optionType>
                    <underlyingAsset>
                        <commodity>Brent Crude Oil</commodity>
                        <unit>Barrel</unit>
                    </underlyingAsset>
                    <strikePrice currency="USD">75.00</strikePrice>
                    <notionalQuantity>1000</notionalQuantity>
                    <expiryDate>2026-03-15</expiryDate>
                    <settlementType>Physical</settlementType>
                </OtcOption>
            </OtcOptions>""";

        // Real YAML rules from the OTC Options bootstrap demo (simplified for overview)
        String overviewYaml = """
            # APEX Rules Engine - OTC Options Bootstrap Configuration
            metadata:
              name: "OTC Options Bootstrap Configuration"
              version: "1.0.0"
              description: "Complete OTC Options validation and enrichment"
              type: "bootstrap"
              business-domain: "OTC Derivatives"

            rules:
              # Structural Validation Rules
              - id: "trade-date-required"
                name: "Trade Date Required"
                condition: "#tradeDate != null"
                message: "Trade date is required for all OTC options"
                severity: "ERROR"
                priority: 1
                categories: ["structural", "required-fields"]

              - id: "parties-required"
                name: "Counterparties Required"
                condition: "#buyerParty != null && #sellerParty != null"
                message: "Both buyer and seller parties are required"
                severity: "ERROR"
                priority: 1
                categories: ["structural", "counterparty"]

              - id: "option-type-valid"
                name: "Valid Option Type"
                condition: "#optionType != null && (#optionType == 'Call' || #optionType == 'Put')"
                message: "Option type must be either 'Call' or 'Put'"
                severity: "ERROR"
                priority: 1
                categories: ["structural", "option-validation"]

              - id: "strike-price-positive"
                name: "Positive Strike Price"
                condition: "#strikePrice != null && #strikePrice > 0"
                message: "Strike price must be positive"
                severity: "ERROR"
                priority: 2
                categories: ["financial", "pricing"]
            """;

        PlaygroundScreenshotUtil.PlaygroundData data =
            new PlaygroundScreenshotUtil.PlaygroundData(otcOptionsXml, overviewYaml, true);

        String screenshotPath = screenshotUtil.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
            "playground_interface_overview",
            data,
            true
        );

        assertNotNull(screenshotPath);
        assertTrue(screenshotPath.contains("playground_interface_overview"));
        System.out.println("Playground interface overview screenshot: " + screenshotPath);
    }

    @Test
    @Order(6)
    @DisplayName("Mobile Responsive Bootstrap Demo")
    void testMobileBootstrapDemo() throws IOException {
        // Same OTC Options XML data as the desktop version for consistency
        String otcOptionsXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!-- OTC Options Bootstrap Demo - Sample Data -->
            <OtcOptions>
                <!-- Option 1: Natural Gas Call -->
                <OtcOption id="option-1">
                    <tradeDate>2025-08-02</tradeDate>
                    <buyerParty>GOLDMAN_SACHS</buyerParty>
                    <sellerParty>JP_MORGAN</sellerParty>
                    <optionType>Call</optionType>
                    <underlyingAsset>
                        <commodity>Natural Gas</commodity>
                        <unit>MMBtu</unit>
                    </underlyingAsset>
                    <strikePrice currency="USD">3.50</strikePrice>
                    <notionalQuantity>10000</notionalQuantity>
                    <expiryDate>2025-12-28</expiryDate>
                    <settlementType>Cash</settlementType>
                </OtcOption>

                <!-- Option 2: Brent Crude Oil Put -->
                <OtcOption id="option-2">
                    <tradeDate>2025-08-02</tradeDate>
                    <buyerParty>MORGAN_STANLEY</buyerParty>
                    <sellerParty>CITI</sellerParty>
                    <optionType>Put</optionType>
                    <underlyingAsset>
                        <commodity>Brent Crude Oil</commodity>
                        <unit>Barrel</unit>
                    </underlyingAsset>
                    <strikePrice currency="USD">75.00</strikePrice>
                    <notionalQuantity>1000</notionalQuantity>
                    <expiryDate>2026-03-15</expiryDate>
                    <settlementType>Physical</settlementType>
                </OtcOption>
            </OtcOptions>""";

        // Same YAML rules as desktop version for consistency
        String mobileYaml = """
            # APEX Rules Engine - OTC Options Bootstrap Configuration
            metadata:
              name: "OTC Options Bootstrap Configuration"
              version: "1.0.0"
              description: "Complete OTC Options validation and enrichment"
              type: "bootstrap"
              business-domain: "OTC Derivatives"

            rules:
              # Structural Validation Rules
              - id: "trade-date-required"
                name: "Trade Date Required"
                condition: "#tradeDate != null"
                message: "Trade date is required for all OTC options"
                severity: "ERROR"
                priority: 1
                categories: ["structural", "required-fields"]

              - id: "parties-required"
                name: "Counterparties Required"
                condition: "#buyerParty != null && #sellerParty != null"
                message: "Both buyer and seller parties are required"
                severity: "ERROR"
                priority: 1
                categories: ["structural", "counterparty"]

              - id: "option-type-valid"
                name: "Valid Option Type"
                condition: "#optionType != null && (#optionType == 'Call' || #optionType == 'Put')"
                message: "Option type must be either 'Call' or 'Put'"
                severity: "ERROR"
                priority: 1
                categories: ["structural", "option-validation"]
            """;

        PlaygroundScreenshotUtil.PlaygroundData data =
            new PlaygroundScreenshotUtil.PlaygroundData(otcOptionsXml, mobileYaml, true);

        String screenshotPath = screenshotUtil.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.MOBILE_VIEW,
            "bootstrap_demo_mobile",
            data,
            true
        );

        assertNotNull(screenshotPath);
        assertTrue(screenshotPath.contains("bootstrap_demo_mobile"));
        System.out.println("Mobile bootstrap demo screenshot: " + screenshotPath);
    }
}

