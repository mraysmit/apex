package dev.mars.rulesengine.demo.service.transformers;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.engine.model.TransformerRule;
import dev.mars.rulesengine.core.service.lookup.LookupServiceRegistry;
import dev.mars.rulesengine.core.service.transform.GenericTransformer;
import dev.mars.rulesengine.core.service.transform.GenericTransformerService;
import dev.mars.rulesengine.demo.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Demonstration of how to use the ProductTransformerDemoConfig class with GenericTransformer.
 * This class shows the step-by-step process of creating and using a GenericTransformer
 * for Product objects using the ProductTransformerDemoConfig to define transformation rules.
 *
 * This is a demo class with no public constructors or methods except for the main method.
 */
public class ProductTransformerDemo {
    private static final Logger LOGGER = Logger.getLogger(ProductTransformerDemo.class.getName());

    /**
     * Private constructor to prevent instantiation.
     * This is a demo class that should only be run via the main method.
     */
    private ProductTransformerDemo() {
        // Private constructor to prevent instantiation
    }

    /**
     * Main method to run the demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Run the demonstration
        runProductTransformationDemo();
    }

    /**
     * Run the product transformation demonstration.
     * This method shows the step-by-step process of creating and using a GenericTransformer.
     */
    private static void runProductTransformationDemo() {
        LOGGER.info("Starting product transformation demonstration");

        // Step 1: Create a RulesEngine
        LOGGER.info("Step 1: Creating a RulesEngine");
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Step 2: Create a LookupServiceRegistry
        LOGGER.info("Step 2: Creating a LookupServiceRegistry");
        LookupServiceRegistry registry = new LookupServiceRegistry();

        // Step 3: Create a ProductTransformerDemoConfig
        LOGGER.info("Step 3: Creating a ProductTransformerDemoConfig");
        ProductTransformerDemoConfig config = new ProductTransformerDemoConfig(rulesEngine);

        // Step 4: Create a GenericTransformerService
        LOGGER.info("Step 4: Creating a GenericTransformerService");
        GenericTransformerService transformerService = new GenericTransformerService(registry, rulesEngine);

        // Step 5: Create transformer rules
        LOGGER.info("Step 5: Creating transformer rules");
        List<TransformerRule<Product>> rules = new ArrayList<>();
        rules.add(config.createDiscountRule(0.05)); // 5% discount rule
        rules.add(config.createConditionalDiscountRule("#value.price > 1000.0", 0.10)); // 10% discount for expensive products

        // Step 6: Create a GenericTransformer
        LOGGER.info("Step 6: Creating a GenericTransformer");
        GenericTransformer<Product> transformer = transformerService.createTransformer(
                "ProductTransformer", Product.class, rules);

        // Step 7: Create test products
        LOGGER.info("Step 7: Creating test products");
        List<Product> products = createTestProducts();

        // Step 8: Transform each product and display the results
        LOGGER.info("Step 8: Transforming products and displaying results");
        for (Product product : products) {
            LOGGER.info("Original product: " + product);
            LOGGER.info("Original price: $" + product.getPrice());

            // Transform the product
            Product transformedProduct = transformer.transform(product);

            // Display the transformed product
            LOGGER.info("Transformed product: " + transformedProduct);
            LOGGER.info("Transformed price: $" + transformedProduct.getPrice());

            // Calculate the discount percentage
            double discountPercentage = 0.0;
            if (product.getPrice() > 0) {
                discountPercentage = (product.getPrice() - transformedProduct.getPrice()) / product.getPrice() * 100;
            }
            LOGGER.info("Discount for " + product.getName() + ": " + String.format("%.2f", discountPercentage) + "%");

            // Get transformation result
            RuleResult result = transformer.transformWithResult(product);
            LOGGER.info("Transformation result: " + result);

            // Add a separator
            LOGGER.info("----------------------------------------");
        }

        LOGGER.info("Product transformation demonstration completed");
    }

    /**
     * Create a list of test products.
     *
     * @return A list of test products
     */
    private static List<Product> createTestProducts() {
        List<Product> products = new ArrayList<>();

        // Create products with different prices and categories
        products.add(new Product("Budget Laptop", 499.99, "Electronics"));
        products.add(new Product("Premium Smartphone", 1299.99, "Electronics"));
        products.add(new Product("Office Chair", 199.99, "Furniture"));
        products.add(new Product("Luxury Sofa", 2499.99, "Furniture"));

        return products;
    }
}