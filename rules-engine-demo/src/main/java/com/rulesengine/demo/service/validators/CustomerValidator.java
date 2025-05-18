/**
 * A validator for Customer objects.
 * This validator checks if a customer meets certain criteria.
 */
package com.rulesengine.demo.service.validators;

import com.rulesengine.core.service.validation.Validator;
import com.rulesengine.demo.model.Customer;

import java.util.Arrays;
import java.util.List;

public class CustomerValidator implements Validator<Customer> {
    private final String name;
    private final int minAge;
    private final int maxAge;
    private final List<String> allowedMembershipLevels;

    /**
     * Create a new CustomerValidator with the specified criteria.
     *
     * @param name The name of the validator
     * @param minAge The minimum age for a valid customer
     * @param maxAge The maximum age for a valid customer
     * @param allowedMembershipLevels The allowed membership levels for a valid customer
     */
    public CustomerValidator(String name, int minAge, int maxAge, String... allowedMembershipLevels) {
        this.name = name;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.allowedMembershipLevels = Arrays.asList(allowedMembershipLevels);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean validate(Customer customer) {
        if (customer == null) {
            return false;
        }

        // Check age range
        if (customer.getAge() < minAge || customer.getAge() > maxAge) {
            return false;
        }

        // Check membership level if allowed levels are specified
        if (!allowedMembershipLevels.isEmpty() && !allowedMembershipLevels.contains(customer.getMembershipLevel())) {
            return false;
        }

        return true;
    }

    @Override
    public Class<Customer> getType() {
        return Customer.class;
    }
}
