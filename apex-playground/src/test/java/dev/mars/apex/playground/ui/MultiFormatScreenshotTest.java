package dev.mars.apex.playground.ui;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Screenshot tests for different data formats (JSON, XML, CSV) in APEX Playground.
 *
 * This test class generates screenshots showing the playground processing
 * different data formats to demonstrate multi-format support capabilities.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Multi-Format Screenshot Tests")
public class MultiFormatScreenshotTest {

    @LocalServerPort
    private int port;

    private PlaygroundScreenshotUtil screenshotUtil;

    @BeforeEach
    void setUp() {
        String baseUrl = "http://localhost:" + port;
        screenshotUtil = new PlaygroundScreenshotUtil(baseUrl);
    }

    @AfterEach
    void tearDown() {
        if (screenshotUtil != null) {
            screenshotUtil.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("JSON Format Processing Screenshot")
    void testJsonFormatProcessingScreenshot() throws IOException {
        // JSON data for financial processing
        String jsonData = """
            {
              "customer": {
                "id": "CUST-001",
                "name": "Acme Corporation",
                "type": "Enterprise",
                "creditRating": "AAA"
              },
              "transaction": {
                "id": "TXN-12345",
                "amount": 1500000.00,
                "currency": "USD",
                "type": "WIRE_TRANSFER",
                "date": "2025-08-23"
              },
              "compliance": {
                "amlChecked": true,
                "sanctionsChecked": true,
                "riskScore": 2.5
              }
            }""";

        String jsonYaml = """
            # APEX Rules Engine - JSON Processing Demo
            metadata:
              name: "JSON Financial Transaction Processing"
              version: "1.0.0"
              description: "Comprehensive JSON data validation and enrichment"
              type: "financial-processing"
            
            rules:
              # Transaction Validation Rules
              - id: "amount-threshold"
                name: "High Value Transaction Check"
                condition: "#transaction.amount > 1000000"
                message: "High value transaction requires additional approval"
                severity: "WARNING"
                priority: 1
                categories: ["financial", "compliance"]
              
              - id: "credit-rating-check"
                name: "Customer Credit Rating Validation"
                condition: "#customer.creditRating == 'AAA' || #customer.creditRating == 'AA'"
                message: "Customer has acceptable credit rating"
                severity: "INFO"
                priority: 2
                categories: ["credit", "validation"]
              
              - id: "compliance-complete"
                name: "Compliance Checks Complete"
                condition: "#compliance.amlChecked == true && #compliance.sanctionsChecked == true"
                message: "All compliance checks completed successfully"
                severity: "INFO"
                priority: 3
                categories: ["compliance", "regulatory"]
            """;

        PlaygroundScreenshotUtil.PlaygroundData data =
            new PlaygroundScreenshotUtil.PlaygroundData(jsonData, jsonYaml, true);

        String screenshotPath = screenshotUtil.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
            "json_format_processing",
            data,
            true
        );

        assertNotNull(screenshotPath);
        assertTrue(screenshotPath.contains("json_format_processing"));
        System.out.println("JSON format processing screenshot: " + screenshotPath);
    }

    @Test
    @Order(2)
    @DisplayName("XML Format Processing Screenshot")
    void testXmlFormatProcessingScreenshot() throws IOException {
        // XML data for trade processing
        String xmlData = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!-- Trade Settlement XML Data -->
            <TradeSettlement>
                <Trade id="TRD-789012">
                    <Instrument>
                        <Symbol>AAPL</Symbol>
                        <Type>EQUITY</Type>
                        <Exchange>NASDAQ</Exchange>
                        <Currency>USD</Currency>
                    </Instrument>
                    <Quantity>5000</Quantity>
                    <Price>175.50</Price>
                    <TradeDate>2025-08-23</TradeDate>
                    <SettlementDate>2025-08-25</SettlementDate>
                </Trade>
                <Counterparty>
                    <Name>Goldman Sachs</Name>
                    <LEI>7LTWFZYICNSX8D621K86</LEI>
                    <Country>US</Country>
                </Counterparty>
                <Settlement>
                    <Method>DVP</Method>
                    <Status>PENDING</Status>
                    <ClearingHouse>DTCC</ClearingHouse>
                </Settlement>
            </TradeSettlement>""";

        String xmlYaml = """
            # APEX Rules Engine - XML Trade Settlement Processing
            metadata:
              name: "XML Trade Settlement Validation"
              version: "1.0.0"
              description: "Trade settlement validation and enrichment for XML data"
              type: "trade-settlement"
            
            rules:
              # Trade Validation Rules
              - id: "trade-value-check"
                name: "Trade Value Validation"
                condition: "#Quantity * #Price > 100000"
                message: "High value trade detected - requires additional oversight"
                severity: "WARNING"
                priority: 1
                categories: ["trade", "risk"]
              
              - id: "settlement-date-check"
                name: "Settlement Date Validation"
                condition: "#SettlementDate != null"
                message: "Settlement date is properly specified"
                severity: "INFO"
                priority: 2
                categories: ["settlement", "validation"]
              
              - id: "counterparty-lei-check"
                name: "Counterparty LEI Validation"
                condition: "#LEI != null && #LEI.length() == 20"
                message: "Valid LEI code provided for counterparty"
                severity: "INFO"
                priority: 3
                categories: ["counterparty", "regulatory"]
              
              - id: "clearing-house-check"
                name: "Clearing House Validation"
                condition: "#ClearingHouse == 'DTCC' || #ClearingHouse == 'LCH' || #ClearingHouse == 'CME'"
                message: "Recognized clearing house specified"
                severity: "INFO"
                priority: 4
                categories: ["clearing", "infrastructure"]
            """;

        PlaygroundScreenshotUtil.PlaygroundData data =
            new PlaygroundScreenshotUtil.PlaygroundData(xmlData, xmlYaml, true);

        String screenshotPath = screenshotUtil.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
            "xml_format_processing",
            data,
            true
        );

        assertNotNull(screenshotPath);
        assertTrue(screenshotPath.contains("xml_format_processing"));
        System.out.println("XML format processing screenshot: " + screenshotPath);
    }

    @Test
    @Order(3)
    @DisplayName("CSV Format Processing Screenshot")
    void testCsvFormatProcessingScreenshot() throws IOException {
        // CSV data for employee validation
        String csvData = """
            employee_id,name,department,salary,hire_date,performance_rating,bonus_eligible
            EMP001,John Smith,Engineering,95000,2023-01-15,4.2,true
            EMP002,Sarah Johnson,Marketing,78000,2022-06-10,3.8,true
            EMP003,Michael Brown,Sales,82000,2023-03-20,4.5,true
            EMP004,Lisa Davis,HR,71000,2021-11-05,3.9,false
            EMP005,David Wilson,Finance,89000,2022-09-12,4.1,true""";

        String csvYaml = """
            # APEX Rules Engine - CSV Employee Data Processing
            metadata:
              name: "CSV Employee Data Validation"
              version: "1.0.0"
              description: "Employee data validation and bonus eligibility processing"
              type: "hr-processing"
            
            rules:
              # Employee Validation Rules
              - id: "salary-range-check"
                name: "Salary Range Validation"
                condition: "#salary >= 50000 && #salary <= 200000"
                message: "Salary is within acceptable range"
                severity: "INFO"
                priority: 1
                categories: ["compensation", "validation"]
              
              - id: "performance-bonus-check"
                name: "Performance Bonus Eligibility"
                condition: "#performance_rating >= 4.0 && #bonus_eligible == true"
                message: "Employee qualifies for performance bonus"
                severity: "INFO"
                priority: 2
                categories: ["performance", "bonus"]
              
              - id: "high-performer-check"
                name: "High Performer Identification"
                condition: "#performance_rating >= 4.5"
                message: "High performer identified for retention program"
                severity: "INFO"
                priority: 3
                categories: ["performance", "retention"]
              
              - id: "department-salary-check"
                name: "Department Salary Validation"
                condition: "(#department == 'Engineering' && #salary >= 80000) || (#department != 'Engineering')"
                message: "Engineering salary meets minimum threshold"
                severity: "WARNING"
                priority: 4
                categories: ["department", "compensation"]
            """;

        PlaygroundScreenshotUtil.PlaygroundData data =
            new PlaygroundScreenshotUtil.PlaygroundData(csvData, csvYaml, true);

        String screenshotPath = screenshotUtil.takeScreenshot(
            PlaygroundScreenshotUtil.Browser.CHROME,
            PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
            "csv_format_processing",
            data,
            true
        );

        assertNotNull(screenshotPath);
        assertTrue(screenshotPath.contains("csv_format_processing"));
        System.out.println("CSV format processing screenshot: " + screenshotPath);
    }
}
