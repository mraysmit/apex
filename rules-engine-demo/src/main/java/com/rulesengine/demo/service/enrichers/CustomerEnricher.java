/**
 * An enricher for Customer objects.
 * This enricher adds additional information to customers based on their membership level and age.
 */
package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Customer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerEnricher extends AbstractEnricher<Customer> {
    private final Map<String, Double> membershipDiscounts;
    private final Map<String, List<String>> recommendedCategories;

    /**
     * Create a new CustomerEnricher with the specified name.
     *
     * @param name The name of the enricher
     */
    public CustomerEnricher(String name) {
        super(name, Customer.class);
        this.membershipDiscounts = new HashMap<>();
        this.recommendedCategories = new HashMap<>();

        // Initialize with default values
        initializeDefaultValues();
    }

    /**
     * Initialize the enricher with default values.
     */
    private void initializeDefaultValues() {
        // Set up membership discounts
        membershipDiscounts.put("Gold", 0.15);    // 15% discount
        membershipDiscounts.put("Silver", 0.10);  // 10% discount
        membershipDiscounts.put("Bronze", 0.05);  // 5% discount
        membershipDiscounts.put("Basic", 0.02);   // 2% discount

        // Set up recommended categories based on membership level
        List<String> goldCategories = new ArrayList<>();
        goldCategories.add("Equity");
        goldCategories.add("FixedIncome");
        goldCategories.add("ETF");
        recommendedCategories.put("Gold", goldCategories);

        List<String> silverCategories = new ArrayList<>();
        silverCategories.add("FixedIncome");
        silverCategories.add("ETF");
        recommendedCategories.put("Silver", silverCategories);

        List<String> bronzeCategories = new ArrayList<>();
        bronzeCategories.add("ETF");
        recommendedCategories.put("Bronze", bronzeCategories);

        List<String> basicCategories = new ArrayList<>();
        basicCategories.add("ETF");
        recommendedCategories.put("Basic", basicCategories);
    }

    /**
     * Add a membership discount.
     *
     * @param membershipLevel The membership level
     * @param discount The discount as a decimal (e.g., 0.15 for 15%)
     */
    public void addMembershipDiscount(String membershipLevel, double discount) {
        membershipDiscounts.put(membershipLevel, discount);
    }

    /**
     * Add recommended categories for a membership level.
     *
     * @param membershipLevel The membership level
     * @param categories The recommended categories
     */
    public void addRecommendedCategories(String membershipLevel, List<String> categories) {
        recommendedCategories.put(membershipLevel, new ArrayList<>(categories));
    }

    @Override
    public Customer enrich(Customer customer) {
        if (customer == null) {
            return null;
        }

        String membershipLevel = customer.getMembershipLevel();

        // Create a new customer with the same properties
        Customer enrichedCustomer = new Customer(
            customer.getName(),
            customer.getAge(),
            customer.getMembershipLevel(),
            new ArrayList<>(customer.getPreferredCategories())
        );

        // Add recommended categories if available for this membership level
        if (recommendedCategories.containsKey(membershipLevel)) {
            List<String> categories = recommendedCategories.get(membershipLevel);
            for (String category : categories) {
                if (!enrichedCustomer.getPreferredCategories().contains(category)) {
                    enrichedCustomer.addPreferredCategory(category);
                }
            }
        }

        return enrichedCustomer;
    }

    /**
     * Get the discount for a customer based on their membership level.
     *
     * @param customer The customer
     * @return The discount as a decimal, or 0 if no discount is available
     */
    public double getDiscountForCustomer(Customer customer) {
        String membershipLevel = customer.getMembershipLevel();
        return membershipDiscounts.getOrDefault(membershipLevel, 0.0);
    }
}
