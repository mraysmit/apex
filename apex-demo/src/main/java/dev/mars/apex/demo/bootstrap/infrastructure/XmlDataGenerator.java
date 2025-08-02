package dev.mars.apex.demo.bootstrap.infrastructure;

import dev.mars.apex.demo.bootstrap.model.OtcOption;
import dev.mars.apex.demo.bootstrap.model.UnderlyingAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * XML data generator for OTC Options Bootstrap Demo.
 * Creates sample OTC Option XML data files based on FpML standards.
 */
@Component
public class XmlDataGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(XmlDataGenerator.class);
    
    /**
     * Generates sample OTC Option XML data files.
     */
    public void generateSampleXmlData() {
        logger.info("Generating sample OTC Option XML data...");
        
        try {
            // Create the datasets directory if it doesn't exist
            Path datasetsDir = Paths.get("src/main/resources/bootstrap/datasets");
            Files.createDirectories(datasetsDir);
            
            // Generate individual XML files
            List<OtcOption> options = createSampleOtcOptions();
            
            for (int i = 0; i < options.size(); i++) {
                String xmlContent = generateXmlForOption(options.get(i));
                Path xmlFile = datasetsDir.resolve("otc-option-" + (i + 1) + ".xml");
                Files.writeString(xmlFile, xmlContent);
                logger.debug("Generated XML file: {}", xmlFile);
            }
            
            // Generate a combined XML file with all options
            String combinedXml = generateCombinedXml(options);
            Path combinedFile = datasetsDir.resolve("sample-otc-options.xml");
            Files.writeString(combinedFile, combinedXml);
            
            logger.info("Generated {} OTC Option XML files successfully", options.size() + 1);
            
        } catch (IOException e) {
            logger.error("Failed to generate XML data files: {}", e.getMessage(), e);
            throw new RuntimeException("XML data generation failed", e);
        }
    }
    
    /**
     * Creates sample OTC Option data objects.
     */
    private List<OtcOption> createSampleOtcOptions() {
        List<OtcOption> options = new ArrayList<>();
        
        // 1. Natural Gas Call Option
        options.add(new OtcOption(
            LocalDate.of(2025, 8, 2),
            "GOLDMAN_SACHS",
            "JP_MORGAN",
            "Call",
            new UnderlyingAsset("Natural Gas", "MMBtu"),
            new BigDecimal("3.50"),
            "USD",
            new BigDecimal("10000"),
            LocalDate.of(2025, 12, 28),
            "Cash"
        ));
        
        // 2. Brent Crude Oil Put Option
        options.add(new OtcOption(
            LocalDate.of(2025, 8, 2),
            "MORGAN_STANLEY",
            "CITI",
            "Put",
            new UnderlyingAsset("Brent Crude Oil", "Barrel"),
            new BigDecimal("75.00"),
            "USD",
            new BigDecimal("1000"),
            LocalDate.of(2026, 3, 15),
            "Physical"
        ));
        
        // 3. Gold Call Option
        options.add(new OtcOption(
            LocalDate.of(2025, 8, 2),
            "BARCLAYS",
            "DEUTSCHE_BANK",
            "Call",
            new UnderlyingAsset("Gold", "Troy Ounce"),
            new BigDecimal("2100.00"),
            "USD",
            new BigDecimal("100"),
            LocalDate.of(2025, 11, 30),
            "Cash"
        ));
        
        // 4. Silver Put Option
        options.add(new OtcOption(
            LocalDate.of(2025, 8, 2),
            "UBS",
            "CREDIT_SUISSE",
            "Put",
            new UnderlyingAsset("Silver", "Troy Ounce"),
            new BigDecimal("28.00"),
            "USD",
            new BigDecimal("5000"),
            LocalDate.of(2025, 10, 15),
            "Physical"
        ));
        
        // 5. Copper Call Option
        options.add(new OtcOption(
            LocalDate.of(2025, 8, 2),
            "JP_MORGAN",
            "GOLDMAN_SACHS",
            "Call",
            new UnderlyingAsset("Copper", "Pound"),
            new BigDecimal("4.25"),
            "USD",
            new BigDecimal("25000"),
            LocalDate.of(2026, 1, 20),
            "Cash"
        ));
        
        // 6. Wheat Put Option
        options.add(new OtcOption(
            LocalDate.of(2025, 8, 2),
            "CITI",
            "BARCLAYS",
            "Put",
            new UnderlyingAsset("Wheat", "Bushel"),
            new BigDecimal("6.50"),
            "USD",
            new BigDecimal("50000"),
            LocalDate.of(2025, 9, 30),
            "Physical"
        ));
        
        return options;
    }
    
    /**
     * Generates XML content for a single OTC Option.
     */
    private String generateXmlForOption(OtcOption option) {
        StringBuilder xml = new StringBuilder();
        
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<OtcOption>\n");
        xml.append("    <tradeDate>").append(option.getTradeDate()).append("</tradeDate>\n");
        xml.append("    <buyerParty>").append(option.getBuyerParty()).append("</buyerParty>\n");
        xml.append("    <sellerParty>").append(option.getSellerParty()).append("</sellerParty>\n");
        xml.append("    <optionType>").append(option.getOptionType()).append("</optionType>\n");
        xml.append("    <underlyingAsset>\n");
        xml.append("        <commodity>").append(option.getUnderlyingAsset().getCommodity()).append("</commodity>\n");
        xml.append("        <unit>").append(option.getUnderlyingAsset().getUnit()).append("</unit>\n");
        xml.append("    </underlyingAsset>\n");
        xml.append("    <strikePrice currency=\"").append(option.getStrikeCurrency()).append("\">")
           .append(option.getStrikePrice()).append("</strikePrice>\n");
        xml.append("    <notionalQuantity>").append(option.getNotionalQuantity()).append("</notionalQuantity>\n");
        xml.append("    <expiryDate>").append(option.getExpiryDate()).append("</expiryDate>\n");
        xml.append("    <settlementType>").append(option.getSettlementType()).append("</settlementType>\n");
        xml.append("</OtcOption>");
        
        return xml.toString();
    }
    
    /**
     * Generates a combined XML file with all OTC Options.
     */
    private String generateCombinedXml(List<OtcOption> options) {
        StringBuilder xml = new StringBuilder();
        
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<!-- OTC Options Bootstrap Demo - Sample Data -->\n");
        xml.append("<!-- Generated on: ").append(LocalDate.now()).append(" -->\n");
        xml.append("<OtcOptions>\n");
        
        for (int i = 0; i < options.size(); i++) {
            OtcOption option = options.get(i);
            xml.append("    <!-- Option ").append(i + 1).append(": ")
               .append(option.getUnderlyingAsset().getCommodity()).append(" ")
               .append(option.getOptionType()).append(" -->\n");
            xml.append("    <OtcOption id=\"option-").append(i + 1).append("\">\n");
            xml.append("        <tradeDate>").append(option.getTradeDate()).append("</tradeDate>\n");
            xml.append("        <buyerParty>").append(option.getBuyerParty()).append("</buyerParty>\n");
            xml.append("        <sellerParty>").append(option.getSellerParty()).append("</sellerParty>\n");
            xml.append("        <optionType>").append(option.getOptionType()).append("</optionType>\n");
            xml.append("        <underlyingAsset>\n");
            xml.append("            <commodity>").append(option.getUnderlyingAsset().getCommodity()).append("</commodity>\n");
            xml.append("            <unit>").append(option.getUnderlyingAsset().getUnit()).append("</unit>\n");
            xml.append("        </underlyingAsset>\n");
            xml.append("        <strikePrice currency=\"").append(option.getStrikeCurrency()).append("\">")
               .append(option.getStrikePrice()).append("</strikePrice>\n");
            xml.append("        <notionalQuantity>").append(option.getNotionalQuantity()).append("</notionalQuantity>\n");
            xml.append("        <expiryDate>").append(option.getExpiryDate()).append("</expiryDate>\n");
            xml.append("        <settlementType>").append(option.getSettlementType()).append("</settlementType>\n");
            xml.append("    </OtcOption>\n");
            
            if (i < options.size() - 1) {
                xml.append("\n");
            }
        }
        
        xml.append("</OtcOptions>");
        
        return xml.toString();
    }
    
    /**
     * Verifies that XML data files were generated successfully.
     */
    public boolean verifyXmlDataGeneration() {
        logger.debug("Verifying XML data generation...");
        
        try {
            Path datasetsDir = Paths.get("src/main/resources/bootstrap/datasets");
            Path combinedFile = datasetsDir.resolve("sample-otc-options.xml");
            
            if (!Files.exists(combinedFile)) {
                logger.error("Combined XML file not found: {}", combinedFile);
                return false;
            }
            
            String content = Files.readString(combinedFile);
            if (!content.contains("<OtcOptions>") || !content.contains("Natural Gas")) {
                logger.error("XML file does not contain expected content");
                return false;
            }
            
            logger.debug("XML data generation verified successfully");
            return true;
            
        } catch (Exception e) {
            logger.error("XML data verification failed: {}", e.getMessage(), e);
            return false;
        }
    }
}
