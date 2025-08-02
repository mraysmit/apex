package dev.mars.apex.demo.examples;

import dev.mars.apex.core.api.RulesService;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demonstration of processing JSON and XML data with APEX rules engine.
 * 
 * This example shows how to:
 * 1. Parse JSON and XML data into Maps
 * 2. Apply validation rules using APEX
 * 3. Enrich data with calculated fields
 * 4. Process multiple records
 * 
 * @author APEX Demo Team
 * @since 1.0.0
 */
public class JsonXmlFileProcessingDemo {
    
    private final RulesService rulesService;

    public JsonXmlFileProcessingDemo() throws Exception {
        // Initialize APEX services
        this.rulesService = new RulesService();

        // Setup validation rules
        setupValidationRules();
    }
    
    /**
     * Main demonstration method.
     */
    public static void main(String[] args) {
        try {
            JsonXmlFileProcessingDemo demo = new JsonXmlFileProcessingDemo();
            
            System.out.println("APEX JSON/XML File Processing Demo");
            System.out.println("=" .repeat(50));

            demo.demonstrateJsonProcessing();
            demo.demonstrateXmlProcessing();
            demo.demonstrateBatchProcessing();

            System.out.println("\nAll demonstrations completed successfully!");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrate JSON data processing.
     */
    private void demonstrateJsonProcessing() throws Exception {
        System.out.println("\n1. JSON Data Processing");
        System.out.println("-".repeat(30));
        
        // Sample JSON data (parsed into Map)
        Map<String, Object> customer = createSampleJsonCustomer();
        
        System.out.println("Original JSON data: " + customer);
        
        // Apply validation rules
        boolean validationResult = validateCustomer(customer);
        
        // Apply enrichments
        enrichCustomerData(customer);
        
        System.out.println("Enriched data: " + customer);
        System.out.println("Final status: " + (validationResult ? "APPROVED" : "REJECTED"));
    }
    
    /**
     * Demonstrate XML data processing.
     */
    private void demonstrateXmlProcessing() throws Exception {
        System.out.println("\n2. XML Data Processing");
        System.out.println("-".repeat(30));
        
        // Sample XML data
        String xmlData = """
            <?xml version="1.0" encoding="UTF-8"?>
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

        // Apply validation and enrichment
        boolean validationResult = validateCustomer(customer);
        enrichCustomerData(customer);

        System.out.println("Enriched data: " + customer);
        System.out.println("Final status: " + (validationResult ? "APPROVED" : "REJECTED"));

        // Convert enriched data back to XML and display
        String enrichedXml = convertMapToXml(customer);
        System.out.println("\nFinal Enriched XML:");
        System.out.println(enrichedXml);
    }
    
    /**
     * Demonstrate batch processing of multiple records.
     */
    private void demonstrateBatchProcessing() throws Exception {
        System.out.println("\n3. Batch Processing");
        System.out.println("-".repeat(25));
        
        // Create sample batch data
        List<Map<String, Object>> customers = createSampleBatch();
        
        int approved = 0, rejected = 0;
        
        for (Map<String, Object> customer : customers) {
            String customerId = (String) customer.get("customerId");
            System.out.println("\nProcessing customer: " + customerId);
            
            boolean isValid = validateCustomer(customer);
            enrichCustomerData(customer);
            
            if (isValid) {
                approved++;
                System.out.println("  -> APPROVED");
            } else {
                rejected++;
                System.out.println("  -> REJECTED");
            }
        }
        
        System.out.println("\nBatch Results:");
        System.out.println("  Total processed: " + customers.size());
        System.out.println("  Approved: " + approved);
        System.out.println("  Rejected: " + rejected);
    }
    
    /**
     * Setup validation rules using APEX RulesService.
     */
    private void setupValidationRules() {
        // Define named rules for reuse
        rulesService.define("adult", "#age >= 18");
        rulesService.define("validEmail", "#email != null && #email.contains('@')");
        rulesService.define("activeStatus", "#status == 'ACTIVE' || #status == 'PENDING'");
        rulesService.define("hasBalance", "#accountBalance != null && #accountBalance > 0");
        rulesService.define("requiredFields", 
            "#customerId != null && #firstName != null && #lastName != null");
    }
    
    /**
     * Validate customer data using APEX rules.
     */
    private boolean validateCustomer(Map<String, Object> customer) {
        System.out.println("Applying validations...");
        
        boolean hasRequired = rulesService.test("requiredFields", customer);
        boolean isAdult = rulesService.test("adult", customer);
        boolean hasValidEmail = rulesService.test("validEmail", customer);
        boolean hasBalance = rulesService.test("hasBalance", customer);
        boolean isActive = rulesService.test("activeStatus", customer);
        
        System.out.println("  * Required fields: " + (hasRequired ? "PASS" : "FAIL"));
        System.out.println("  * Age >= 18: " + (isAdult ? "PASS" : "FAIL"));
        System.out.println("  * Valid email: " + (hasValidEmail ? "PASS" : "FAIL"));
        System.out.println("  * Has balance: " + (hasBalance ? "PASS" : "FAIL"));
        System.out.println("  * Active status: " + (isActive ? "PASS" : "FAIL"));
        
        return hasRequired && isAdult && hasValidEmail && hasBalance && isActive;
    }
    
    /**
     * Enrich customer data with calculated fields.
     */
    private void enrichCustomerData(Map<String, Object> customer) {
        System.out.println("Applying enrichments...");
        
        // Calculate customer tier based on balance
        Double balance = (Double) customer.get("accountBalance");
        String tier;
        if (balance >= 50000) {
            tier = "PLATINUM";
        } else if (balance >= 25000) {
            tier = "GOLD";
        } else if (balance >= 10000) {
            tier = "SILVER";
        } else {
            tier = "BRONZE";
        }
        customer.put("customerTier", tier);
        
        // Calculate discount rate based on age
        Integer age = (Integer) customer.get("age");
        Double discountRate;
        if (age >= 30) {
            discountRate = 0.15;
        } else if (age >= 21) {
            discountRate = 0.10;
        } else {
            discountRate = 0.05;
        }
        customer.put("discountRate", discountRate);
        
        // Add country-specific data
        String country = (String) customer.get("country");
        switch (country) {
            case "US":
                customer.put("currency", "USD");
                customer.put("timezone", "EST");
                break;
            case "GB":
                customer.put("currency", "GBP");
                customer.put("timezone", "GMT");
                break;
            case "DE":
                customer.put("currency", "EUR");
                customer.put("timezone", "CET");
                break;
            default:
                customer.put("currency", "USD");
                customer.put("timezone", "UTC");
        }
        
        System.out.println("  -> Customer Tier: " + tier);
        System.out.println("  -> Discount Rate: " + (discountRate * 100) + "%");
        System.out.println("  -> Currency: " + customer.get("currency"));
    }
    
    /**
     * Create sample JSON customer data.
     */
    private Map<String, Object> createSampleJsonCustomer() {
        Map<String, Object> customer = new HashMap<>();
        customer.put("customerId", "CUST001");
        customer.put("firstName", "John");
        customer.put("lastName", "Smith");
        customer.put("email", "john.smith@example.com");
        customer.put("age", 25);
        customer.put("country", "US");
        customer.put("accountBalance", 15000.50);
        customer.put("status", "ACTIVE");
        return customer;
    }
    
    /**
     * Parse XML string to Map.
     */
    private Map<String, Object> parseXmlToMap(String xmlData) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlData.getBytes()));
        
        Element customerElement = doc.getDocumentElement();
        Map<String, Object> customer = new HashMap<>();
        
        // Extract attributes
        customer.put("customerId", customerElement.getAttribute("id"));
        
        // Extract nested elements
        customer.put("firstName", getNestedElementText(customerElement, "name", "first"));
        customer.put("lastName", getNestedElementText(customerElement, "name", "last"));
        customer.put("email", getNestedElementText(customerElement, "contact", "email"));
        
        // Extract simple elements
        customer.put("age", Integer.parseInt(getElementText(customerElement, "age")));
        customer.put("country", getElementText(customerElement, "country"));
        customer.put("accountBalance", Double.parseDouble(getElementText(customerElement, "accountBalance")));
        customer.put("status", getElementText(customerElement, "status"));

        return customer;
    }

    /**
     * Get text from nested XML elements.
     */
    private String getNestedElementText(Element parent, String parentTag, String childTag) {
        NodeList parentNodes = parent.getElementsByTagName(parentTag);
        if (parentNodes.getLength() > 0) {
            Element parentElement = (Element) parentNodes.item(0);
            return getElementText(parentElement, childTag);
        }
        return null;
    }

    /**
     * Get text content of XML element.
     */
    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }

    /**
     * Create sample batch data for demonstration.
     */
    private List<Map<String, Object>> createSampleBatch() {
        List<Map<String, Object>> customers = new ArrayList<>();

        // Customer 1 - Valid
        Map<String, Object> customer1 = new HashMap<>();
        customer1.put("customerId", "CUST003");
        customer1.put("firstName", "Alice");
        customer1.put("lastName", "Johnson");
        customer1.put("email", "alice.johnson@example.com");
        customer1.put("age", 32);
        customer1.put("country", "US");
        customer1.put("accountBalance", 25000.00);
        customer1.put("status", "ACTIVE");
        customers.add(customer1);

        // Customer 2 - Invalid (underage)
        Map<String, Object> customer2 = new HashMap<>();
        customer2.put("customerId", "CUST004");
        customer2.put("firstName", "Bob");
        customer2.put("lastName", "Wilson");
        customer2.put("email", "bob.wilson@example.com");
        customer2.put("age", 17);
        customer2.put("country", "CA");
        customer2.put("accountBalance", 5000.00);
        customer2.put("status", "PENDING");
        customers.add(customer2);

        // Customer 3 - Invalid (no balance)
        Map<String, Object> customer3 = new HashMap<>();
        customer3.put("customerId", "CUST005");
        customer3.put("firstName", "Charlie");
        customer3.put("lastName", "Brown");
        customer3.put("email", "charlie.brown@example.com");
        customer3.put("age", 28);
        customer3.put("country", "GB");
        customer3.put("accountBalance", 0.0);
        customer3.put("status", "INACTIVE");
        customers.add(customer3);

        // Customer 4 - Valid
        Map<String, Object> customer4 = new HashMap<>();
        customer4.put("customerId", "CUST006");
        customer4.put("firstName", "Diana");
        customer4.put("lastName", "Prince");
        customer4.put("email", "diana.prince@example.com");
        customer4.put("age", 29);
        customer4.put("country", "DE");
        customer4.put("accountBalance", 12000.00);
        customer4.put("status", "ACTIVE");
        customers.add(customer4);

        return customers;
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
}
