package dev.mars.apex.demo.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Represents a customer with basic information.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class Customer {
    private String name;
    private int age;
    private String email;
    private String membershipLevel;
    private double balance;
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
        this.balance = 0.0;
        this.preferredCategories = new ArrayList<>(preferredCategories);
    }

    /**
     * Create a new customer with default values.
     */
    public Customer() {
        this.name = "Unknown";
        this.age = 0;
        this.membershipLevel = "Basic";
        this.balance = 0.0;
        this.preferredCategories = new ArrayList<>();
    }

    /**
     * Create a new customer with basic information (commonly used in demos).
     *
     * @param name The name of the customer
     * @param age The age of the customer
     * @param email The email of the customer
     */
    public Customer(String name, int age, String email) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.membershipLevel = "Basic";
        this.balance = 0.0;
        this.preferredCategories = new ArrayList<>();
    }

    /**
     * Create a new customer as a copy of another customer.
     *
     * @param other The customer to copy
     */
    public Customer(Customer other) {
        this.name = other.name;
        this.age = other.age;
        this.email = other.email;
        this.membershipLevel = other.membershipLevel;
        this.balance = other.balance;
        this.preferredCategories = new ArrayList<>(other.getPreferredCategories());
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
     * Get the email of the customer.
     *
     * @return The customer's email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set the email of the customer.
     *
     * @param email The new email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Get the balance of the customer.
     *
     * @return The customer's balance
     */
    public double getBalance() {
        return balance;
    }

    /**
     * Set the balance of the customer.
     *
     * @param balance The new balance
     */
    public void setBalance(double balance) {
        this.balance = balance;
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
                ", email='" + email + '\'' +
                ", membershipLevel='" + membershipLevel + '\'' +
                ", balance=" + balance +
                ", preferredCategories=" + preferredCategories +
                '}';
    }
}
