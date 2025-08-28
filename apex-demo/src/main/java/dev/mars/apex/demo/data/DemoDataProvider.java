package dev.mars.apex.demo.data;

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


import dev.mars.apex.demo.bootstrap.model.Customer;
import dev.mars.apex.demo.bootstrap.model.Product;
import dev.mars.apex.demo.bootstrap.model.Trade;
import dev.mars.apex.demo.bootstrap.model.FinancialTrade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Demo data provider for APEX Rules Engine demonstrations.
 * Provides standardized test data sets for consistent demo experiences.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class DemoDataProvider {

    /**
     * Creates a standard set of demo customers.
     */
    public static List<Customer> createDemoCustomers() {
        List<Customer> customers = new ArrayList<>();

        // Premium customer with high balance
        Customer premium = new Customer("John Premium", 45, "john.premium@example.com");
        premium.setMembershipLevel("Premium");
        premium.setBalance(250000.0);
        premium.setPreferredCategories(Arrays.asList("Investment", "Premium Banking"));
        customers.add(premium);

        // Gold customer
        Customer gold = new Customer("Sarah Gold", 38, "sarah.gold@example.com");
        gold.setMembershipLevel("Gold");
        gold.setBalance(150000.0);
        gold.setPreferredCategories(Arrays.asList("Investment", "Savings"));
        customers.add(gold);

        // Silver customer
        Customer silver = new Customer("Mike Silver", 32, "mike.silver@example.com");
        silver.setMembershipLevel("Silver");
        silver.setBalance(75000.0);
        silver.setPreferredCategories(Arrays.asList("Checking", "Savings"));
        customers.add(silver);

        // Basic customer
        Customer basic = new Customer("Lisa Basic", 28, "lisa.basic@example.com");
        basic.setMembershipLevel("Basic");
        basic.setBalance(25000.0);
        basic.setPreferredCategories(Arrays.asList("Checking"));
        customers.add(basic);

        // Young customer (under 18)
        Customer young = new Customer("Alex Young", 16, "alex.young@example.com");
        young.setMembershipLevel("Basic");
        young.setBalance(1000.0);
        young.setPreferredCategories(Arrays.asList("Savings"));
        customers.add(young);

        return customers;
    }

    /**
     * Creates a standard set of demo products.
     */
    public static List<Product> createDemoProducts() {
        List<Product> products = new ArrayList<>();

        products.add(new Product("Premium Investment Account", 100000.0, "Investment"));
        products.add(new Product("Gold Savings Account", 10000.0, "Savings"));
        products.add(new Product("Silver Checking Account", 1000.0, "Checking"));
        products.add(new Product("Basic Savings Account", 100.0, "Savings"));
        products.add(new Product("Student Account", 0.0, "Student"));
        products.add(new Product("Business Account", 5000.0, "Business"));
        products.add(new Product("Credit Card", 0.0, "Credit"));

        return products;
    }

    /**
     * Creates a standard set of demo trades.
     */
    public static List<Trade> createDemoTrades() {
        List<Trade> trades = new ArrayList<>();

        trades.add(new Trade("EQUITY_001", "150000.00", "Equity"));
        trades.add(new Trade("BOND_001", "75000.00", "Bond"));
        trades.add(new Trade("FX_001", "50000.00", "FX"));
        trades.add(new Trade("COMMODITY_001", "100000.00", "Commodity"));
        trades.add(new Trade("DERIVATIVE_001", "25000.00", "Derivative"));

        return trades;
    }

    /**
     * Creates a standard set of demo financial trades.
     */
    public static List<FinancialTrade> createDemoFinancialTrades() {
        List<FinancialTrade> trades = new ArrayList<>();

        // High-value equity trade
        FinancialTrade equity = new FinancialTrade("EQUITY_001", new BigDecimal("150000.00"), "USD", "GOLDMAN_SACHS");
        equity.setInstrumentType("EQUITY");
        equity.setTradingDesk("EQUITY_DESK");
        trades.add(equity);

        // Government bond trade
        FinancialTrade bond = new FinancialTrade("BOND_001", new BigDecimal("500000.00"), "USD", "JP_MORGAN");
        bond.setInstrumentType("BOND");
        bond.setTradingDesk("FIXED_INCOME_DESK");
        trades.add(bond);

        // FX trade
        FinancialTrade fx = new FinancialTrade("FX_001", new BigDecimal("1000000.00"), "USD", "MORGAN_STANLEY");
        fx.setInstrumentType("FX");
        fx.setTradingDesk("FX_DESK");
        trades.add(fx);

        // Commodity trade
        FinancialTrade commodity = new FinancialTrade("COMMODITY_001", new BigDecimal("250000.00"), "USD", "BARCLAYS");
        commodity.setInstrumentType("COMMODITY");
        commodity.setTradingDesk("COMMODITY_DESK");
        trades.add(commodity);

        return trades;
    }

    /**
     * Creates demo data for performance testing.
     */
    public static List<Customer> createPerformanceTestCustomers(int count) {
        List<Customer> customers = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            Customer customer = new Customer("Customer " + i, 25 + (i % 50), "customer" + i + "@example.com");
            customer.setMembershipLevel(getMembershipLevel(i));
            customer.setBalance(1000.0 + (i * 100.0));
            customer.setPreferredCategories(Arrays.asList("Category" + (i % 5)));
            customers.add(customer);
        }

        return customers;
    }

    /**
     * Helper method to determine membership level based on index.
     */
    private static String getMembershipLevel(int index) {
        return switch (index % 4) {
            case 0 -> "Premium";
            case 1 -> "Gold";
            case 2 -> "Silver";
            default -> "Basic";
        };
    }

    /**
     * Creates invalid demo data for testing error handling.
     */
    public static List<Customer> createInvalidDemoCustomers() {
        List<Customer> customers = new ArrayList<>();

        // Customer with invalid email
        Customer invalidEmail = new Customer("Invalid Email", 30, "not-an-email");
        invalidEmail.setMembershipLevel("Basic");
        invalidEmail.setBalance(5000.0);
        customers.add(invalidEmail);

        // Customer under 18
        Customer underAge = new Customer("Under Age", 16, "underage@example.com");
        underAge.setMembershipLevel("Basic");
        underAge.setBalance(500.0);
        customers.add(underAge);

        // Customer with negative balance
        Customer negativeBalance = new Customer("Negative Balance", 35, "negative@example.com");
        negativeBalance.setMembershipLevel("Basic");
        negativeBalance.setBalance(-1000.0);
        customers.add(negativeBalance);

        return customers;
    }
}