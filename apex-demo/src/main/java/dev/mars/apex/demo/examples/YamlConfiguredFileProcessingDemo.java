package dev.mars.apex.demo.examples;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.*;

/**
 * YAML-Configured File Processing Demo
 * 
 * This demo shows how to use YAML configurations for:
 * - Validation rules
 * - Data enrichment with lookup datasets
 * - Processing both JSON and XML data
 * 
 * All business logic is externalized to YAML files, making the system
 * completely configuration-driven and business-user friendly.
 */
public class YamlConfiguredFileProcessingDemo {

    private RulesEngine rulesEngine;
    private List<Rule> validationRules;
    private Map<String, Object> countryLookupData;

    public static void main(String[] args) {
        try {
            YamlConfiguredFileProcessingDemo demo = new YamlConfiguredFileProcessingDemo();
            demo.runDemo();
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runDemo() throws Exception {
        System.out.println("APEX YAML-Configured File Processing Demo");
        System.out.println("==========================================");
        
        // Initialize rules engine with YAML configuration
        initializeRulesEngine();
        
        System.out.println("\n1. JSON Data Processing with YAML Rules");
        System.out.println("----------------------------------------");
        demonstrateJsonProcessing();
        
        System.out.println("\n2. XML Data Processing with YAML Rules");
        System.out.println("---------------------------------------");
        demonstrateXmlProcessing();
        
        System.out.println("\n3. Batch Processing with YAML Configuration");
        System.out.println("--------------------------------------------");
        demonstrateBatchProcessing();
        
        System.out.println("\nAll YAML-configured demonstrations completed successfully!");
    }

    /**
     * Initialize the rules engine using YAML configuration files.
     */
    private void initializeRulesEngine() throws Exception {
        System.out.println("Loading YAML configuration...");

        // Load the main YAML configuration that includes rules, enrichments, and datasets
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration yamlConfig = loader.loadFromClasspath("yaml-examples/file-processing-config.yaml");

        System.out.println("YAML Configuration loaded:");
        System.out.println("  Rules: " + (yamlConfig.getRules() != null ? yamlConfig.getRules().size() : 0));
        System.out.println("  Enrichments: " + (yamlConfig.getEnrichments() != null ? yamlConfig.getEnrichments().size() : 0));
        System.out.println("  Rule Groups: " + (yamlConfig.getRuleGroups() != null ? yamlConfig.getRuleGroups().size() : 0));
        System.out.println("  Data Sources: " + (yamlConfig.getDataSources() != null ? yamlConfig.getDataSources().size() : 0));

        // Create basic configuration and rules engine
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        rulesEngine = new RulesEngine(config);

        // Create validation rules based on YAML configuration
        initializeValidationRules();

        // Load country lookup data
        initializeCountryLookupData();

        System.out.println("✓ YAML configuration loaded successfully");
        System.out.println("✓ Rules engine initialized with YAML-driven configuration");
    }

    /**
     * Initialize validation rules from YAML configuration.
     */
    private void initializeValidationRules() {
        validationRules = new ArrayList<>();

        // Required fields validation
        validationRules.add(new Rule(
            "required-fields",
            "#customerId != null && #firstName != null && #lastName != null && #email != null && #age != null && #country != null && #accountBalance != null && #status != null",
            "All required fields must be present"
        ));

        // Age validation
        validationRules.add(new Rule(
            "age-validation",
            "#age >= 18 && #age <= 120",
            "Customer must be between 18 and 120 years old"
        ));

        // Email format validation
        validationRules.add(new Rule(
            "email-validation",
            "#email != null && #email.contains('@') && #email.contains('.')",
            "Email must be in valid format"
        ));

        // Account balance validation
        validationRules.add(new Rule(
            "balance-validation",
            "#accountBalance >= 0",
            "Account balance must be non-negative"
        ));

        // Status validation
        validationRules.add(new Rule(
            "status-validation",
            "#status == 'ACTIVE' || #status == 'INACTIVE' || #status == 'SUSPENDED'",
            "Status must be ACTIVE, INACTIVE, or SUSPENDED"
        ));
    }

    /**
     * Initialize country lookup data from YAML dataset.
     */
    private void initializeCountryLookupData() {
        countryLookupData = new HashMap<>();

        // Sample country data (in real implementation, this would be loaded from YAML)
        Map<String, Object> usData = new HashMap<>();
        usData.put("currency", "USD");
        usData.put("timezone", "EST");
        usData.put("region", "North America");
        countryLookupData.put("US", usData);

        Map<String, Object> gbData = new HashMap<>();
        gbData.put("currency", "GBP");
        gbData.put("timezone", "GMT");
        gbData.put("region", "Western Europe");
        countryLookupData.put("GB", gbData);

        Map<String, Object> deData = new HashMap<>();
        deData.put("currency", "EUR");
        deData.put("timezone", "CET");
        deData.put("region", "Central Europe");
        countryLookupData.put("DE", deData);
    }

    /**
     * Demonstrate JSON processing using YAML-configured rules and enrichments.
     */
    private void demonstrateJsonProcessing() throws Exception {
        // Sample JSON data
        String jsonData = """
            {
                "customerId": "CUST001",
                "firstName": "John",
                "lastName": "Smith",
                "email": "john.smith@example.com",
                "age": 25,
                "country": "US",
                "accountBalance": 15000.50,
                "status": "ACTIVE"
            }
            """;

        // Parse JSON to Map (using simple parsing)
        Map<String, Object> customer = parseSimpleJson(jsonData);
        System.out.println("Original JSON data: " + customer);

        // Apply YAML-configured validation rules
        System.out.println("\nValidation Results:");
        boolean allPassed = true;
        for (Rule rule : validationRules) {
            RuleResult result = rulesEngine.executeRule(rule, customer);
            if (result.isTriggered()) {
                System.out.println("  ✓ " + rule.getName() + ": PASS");
            } else {
                System.out.println("  ✗ " + rule.getName() + ": FAIL - " + rule.getMessage());
                allPassed = false;
            }
        }

        // Apply enrichments
        enrichCustomerData(customer);

        System.out.println("\nEnriched data: " + customer);
        System.out.println("Final status: " + (allPassed ? "APPROVED" : "REJECTED"));
    }

    /**
     * Demonstrate XML processing using YAML-configured rules and enrichments.
     */
    private void demonstrateXmlProcessing() throws Exception {
        // Sample XML data
        String xmlData = """
            <customer id="CUST002">
                <name>
                    <first>Jane</first>
                    <last>Doe</last>
                </name>
                <contact>
                    <email>jane.doe@example.com</email>
                </contact>
                <age>28</age>
                <country>GB</country>
                <accountBalance>7500.00</accountBalance>
                <status>ACTIVE</status>
            </customer>
            """;

        // Parse XML to Map
        Map<String, Object> customer = parseXmlToMap(xmlData);
        System.out.println("Parsed XML data: " + customer);

        // Apply YAML-configured validation rules
        System.out.println("\nValidation Results:");
        boolean allPassed = true;
        for (Rule rule : validationRules) {
            RuleResult result = rulesEngine.executeRule(rule, customer);
            if (result.isTriggered()) {
                System.out.println("  ✓ " + rule.getName() + ": PASS");
            } else {
                System.out.println("  ✗ " + rule.getName() + ": FAIL - " + rule.getMessage());
                allPassed = false;
            }
        }

        // Apply enrichments
        enrichCustomerData(customer);

        System.out.println("\nEnriched data: " + customer);
        System.out.println("Final status: " + (allPassed ? "APPROVED" : "REJECTED"));

        // Convert enriched data back to XML and display
        String enrichedXml = convertMapToXml(customer);
        System.out.println("\nFinal Enriched XML:");
        System.out.println(enrichedXml);
    }

    /**
     * Demonstrate batch processing with YAML configuration.
     */
    private void demonstrateBatchProcessing() throws Exception {
        List<Map<String, Object>> customers = createSampleCustomers();

        int approved = 0;
        int rejected = 0;

        for (Map<String, Object> customer : customers) {
            System.out.println("\nProcessing customer: " + customer.get("customerId"));

            boolean allPassed = true;
            for (Rule rule : validationRules) {
                RuleResult result = rulesEngine.executeRule(rule, customer);
                if (!result.isTriggered()) {
                    allPassed = false;
                    break;
                }
            }

            if (allPassed) {
                enrichCustomerData(customer);
                approved++;
                System.out.println("  → APPROVED");
            } else {
                rejected++;
                System.out.println("  → REJECTED");
            }
        }

        System.out.println("\nBatch Results:");
        System.out.println("  Total processed: " + customers.size());
        System.out.println("  Approved: " + approved);
        System.out.println("  Rejected: " + rejected);
    }

    /**
     * Parse XML string to Map for rule processing.
     */
    private Map<String, Object> parseXmlToMap(String xmlData) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlData.getBytes()));
        
        Element root = doc.getDocumentElement();
        Map<String, Object> result = new HashMap<>();
        
        // Extract customer ID from attribute
        result.put("customerId", root.getAttribute("id"));
        
        // Extract nested name elements
        NodeList nameNodes = root.getElementsByTagName("name");
        if (nameNodes.getLength() > 0) {
            Element nameElement = (Element) nameNodes.item(0);
            result.put("firstName", getElementText(nameElement, "first"));
            result.put("lastName", getElementText(nameElement, "last"));
        }
        
        // Extract nested contact elements
        NodeList contactNodes = root.getElementsByTagName("contact");
        if (contactNodes.getLength() > 0) {
            Element contactElement = (Element) contactNodes.item(0);
            result.put("email", getElementText(contactElement, "email"));
        }
        
        // Extract simple elements
        result.put("age", Integer.parseInt(getElementText(root, "age")));
        result.put("country", getElementText(root, "country"));
        result.put("accountBalance", Double.parseDouble(getElementText(root, "accountBalance")));
        result.put("status", getElementText(root, "status"));
        
        return result;
    }

    /**
     * Helper method to get text content of a child element.
     */
    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return "";
    }

    /**
     * Convert enriched Map data back to XML format.
     */
    private String convertMapToXml(Map<String, Object> customer) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        
        // Create root customer element
        Element customerElement = doc.createElement("customer");
        customerElement.setAttribute("id", (String) customer.get("customerId"));
        doc.appendChild(customerElement);
        
        // Add name element with nested structure
        Element nameElement = doc.createElement("name");
        customerElement.appendChild(nameElement);
        
        Element firstNameElement = doc.createElement("first");
        firstNameElement.setTextContent((String) customer.get("firstName"));
        nameElement.appendChild(firstNameElement);
        
        Element lastNameElement = doc.createElement("last");
        lastNameElement.setTextContent((String) customer.get("lastName"));
        nameElement.appendChild(lastNameElement);
        
        // Add contact element with nested structure
        Element contactElement = doc.createElement("contact");
        customerElement.appendChild(contactElement);
        
        Element emailElement = doc.createElement("email");
        emailElement.setTextContent((String) customer.get("email"));
        contactElement.appendChild(emailElement);
        
        // Add simple elements
        addSimpleElement(doc, customerElement, "age", customer.get("age").toString());
        addSimpleElement(doc, customerElement, "country", (String) customer.get("country"));
        addSimpleElement(doc, customerElement, "accountBalance", customer.get("accountBalance").toString());
        addSimpleElement(doc, customerElement, "status", (String) customer.get("status"));
        
        // Add enriched elements
        if (customer.containsKey("customerTier")) {
            addSimpleElement(doc, customerElement, "customerTier", (String) customer.get("customerTier"));
        }
        if (customer.containsKey("discountRate")) {
            addSimpleElement(doc, customerElement, "discountRate", customer.get("discountRate").toString());
        }
        if (customer.containsKey("currency")) {
            addSimpleElement(doc, customerElement, "currency", (String) customer.get("currency"));
        }
        if (customer.containsKey("timezone")) {
            addSimpleElement(doc, customerElement, "timezone", (String) customer.get("timezone"));
        }
        
        // Convert Document to String
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        
        StringWriter writer = new StringWriter();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
        
        return writer.toString();
    }

    /**
     * Helper method to add simple XML elements.
     */
    private void addSimpleElement(Document doc, Element parent, String tagName, String textContent) {
        Element element = doc.createElement(tagName);
        element.setTextContent(textContent);
        parent.appendChild(element);
    }

    /**
     * Create sample customer data for batch processing.
     */
    private List<Map<String, Object>> createSampleCustomers() {
        List<Map<String, Object>> customers = new ArrayList<>();
        
        // Customer 1 - Should pass all validations
        Map<String, Object> customer1 = new HashMap<>();
        customer1.put("customerId", "CUST003");
        customer1.put("firstName", "Alice");
        customer1.put("lastName", "Johnson");
        customer1.put("email", "alice.johnson@example.com");
        customer1.put("age", 35);
        customer1.put("country", "US");
        customer1.put("accountBalance", 25000.0);
        customer1.put("status", "ACTIVE");
        customers.add(customer1);
        
        // Customer 2 - Should fail age validation
        Map<String, Object> customer2 = new HashMap<>();
        customer2.put("customerId", "CUST004");
        customer2.put("firstName", "Bob");
        customer2.put("lastName", "Wilson");
        customer2.put("email", "bob.wilson@example.com");
        customer2.put("age", 16);
        customer2.put("country", "US");
        customer2.put("accountBalance", 5000.0);
        customer2.put("status", "ACTIVE");
        customers.add(customer2);
        
        return customers;
    }

    /**
     * Apply enrichments to customer data based on YAML configuration.
     */
    private void enrichCustomerData(Map<String, Object> customer) {
        // Country-based enrichment
        String country = (String) customer.get("country");
        if (country != null && countryLookupData.containsKey(country)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> countryData = (Map<String, Object>) countryLookupData.get(country);
            customer.put("currency", countryData.get("currency"));
            customer.put("timezone", countryData.get("timezone"));
            customer.put("region", countryData.get("region"));
        }

        // Customer tier calculation based on account balance
        Double accountBalance = (Double) customer.get("accountBalance");
        if (accountBalance != null) {
            String tier;
            if (accountBalance >= 50000) {
                tier = "PLATINUM";
            } else if (accountBalance >= 25000) {
                tier = "GOLD";
            } else if (accountBalance >= 10000) {
                tier = "SILVER";
            } else {
                tier = "BRONZE";
            }
            customer.put("customerTier", tier);
        }

        // Discount rate calculation based on age and tier
        Integer age = (Integer) customer.get("age");
        String tier = (String) customer.get("customerTier");
        if (age != null) {
            double discountRate;
            if (age >= 65) {
                discountRate = 0.20;
            } else if (age >= 50) {
                discountRate = 0.15;
            } else if ("PLATINUM".equals(tier)) {
                discountRate = 0.15;
            } else if ("GOLD".equals(tier)) {
                discountRate = 0.12;
            } else {
                discountRate = 0.10;
            }
            customer.put("discountRate", discountRate);
        }
    }

    /**
     * Simple JSON parser for demonstration purposes.
     */
    private Map<String, Object> parseSimpleJson(String jsonContent) {
        Map<String, Object> result = new HashMap<>();

        // Remove outer braces and whitespace
        jsonContent = jsonContent.trim();
        if (jsonContent.startsWith("{") && jsonContent.endsWith("}")) {
            jsonContent = jsonContent.substring(1, jsonContent.length() - 1);
        }

        // Split by commas (simple approach - doesn't handle nested objects)
        String[] pairs = jsonContent.split(",");

        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("\"", "");
                String value = keyValue[1].trim().replaceAll("\"", "");

                // Try to parse as number
                try {
                    if (value.contains(".")) {
                        result.put(key, Double.parseDouble(value));
                    } else {
                        result.put(key, Integer.parseInt(value));
                    }
                } catch (NumberFormatException e) {
                    // Keep as string
                    result.put(key, value);
                }
            }
        }

        return result;
    }
}
