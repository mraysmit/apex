package com.rulesengine.demo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a customer with basic information.
 */
public class Customer {
    private String name;
    private int age;
    private String membershipLevel;
    private List<String> preferredCategories;

    /**
     * Create a new customer with the specified attributes.
     *
     * @param name The name of the customer
     * @param age The age of the customer
     * @param membershipLevel The membership level of the customer (e.g., "Basic", "Silver", "Gold")
     * @param preferredCategories The categories the customer prefers
     */
    public Customer(String name, int age, String membershipLevel, List<String> preferredCategories) {
        this.name = name;
        this.age = age;
        this.membershipLevel = membershipLevel;
        this.preferredCategories = new ArrayList<>(preferredCategories);
    }

    /**
     * Create a new customer with default values.
     */
    public Customer() {
        this.name = "Unknown";
        this.age = 0;
        this.membershipLevel = "Basic";
        this.preferredCategories = new ArrayList<>();
    }

    /**
     * Get the name of the customer.
     *
     * @return The customer's name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the customer.
     *
     * @param name The new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the age of the customer.
     *
     * @return The customer's age
     */
    public int getAge() {
        return age;
    }

    /**
     * Set the age of the customer.
     *
     * @param age The new age
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * Get the membership level of the customer.
     *
     * @return The customer's membership level
     */
    public String getMembershipLevel() {
        return membershipLevel;
    }

    /**
     * Set the membership level of the customer.
     *
     * @param membershipLevel The new membership level
     */
    public void setMembershipLevel(String membershipLevel) {
        this.membershipLevel = membershipLevel;
    }

    /**
     * Get the preferred categories of the customer.
     *
     * @return The customer's preferred categories
     */
    public List<String> getPreferredCategories() {
        return new ArrayList<>(preferredCategories);
    }

    /**
     * Set the preferred categories of the customer.
     *
     * @param preferredCategories The new preferred categories
     */
    public void setPreferredCategories(List<String> preferredCategories) {
        this.preferredCategories = new ArrayList<>(preferredCategories);
    }

    /**
     * Add a preferred category for the customer.
     *
     * @param category The category to add
     */
    public void addPreferredCategory(String category) {
        if (!preferredCategories.contains(category)) {
            preferredCategories.add(category);
        }
    }

    /**
     * Remove a preferred category for the customer.
     *
     * @param category The category to remove
     */
    public void removePreferredCategory(String category) {
        preferredCategories.remove(category);
    }

    /**
     * Check if the customer is eligible for a discount.
     * This is a demonstration method used in rule expressions.
     *
     * @return True if the customer is eligible for a discount, false otherwise
     */
    public boolean isEligibleForDiscount() {
        return age > 60 || "Gold".equals(membershipLevel);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", membershipLevel='" + membershipLevel + '\'' +
                ", preferredCategories=" + preferredCategories +
                '}';
    }
}