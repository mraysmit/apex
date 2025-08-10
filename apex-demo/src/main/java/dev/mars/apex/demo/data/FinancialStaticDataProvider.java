package dev.mars.apex.demo.data;

import dev.mars.apex.demo.examples.StaticDataEntities.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

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
 * Provides static data for financial instrument validation and enrichment examples.
 * This class contains comprehensive reference data for clients, counterparties,
 * currencies, and commodities used in APEX Rules Engine demonstrations.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class FinancialStaticDataProvider {
    
    // Static data repositories
    private static final Map<String, Client> CLIENTS = new HashMap<>();
    private static final Map<String, ClientAccount> CLIENT_ACCOUNTS = new HashMap<>();
    private static final Map<String, Counterparty> COUNTERPARTIES = new HashMap<>();
    private static final Map<String, CurrencyData> CURRENCIES = new HashMap<>();
    private static final Map<String, CommodityReference> COMMODITIES = new HashMap<>();
    
    static {
        initializeStaticData();
    }
    
    /**
     * Initialize sample static data for demonstration purposes.
     */
    private static void initializeStaticData() {
        initializeClients();
        initializeClientAccounts();
        initializeCounterparties();
        initializeCurrencies();
        initializeCommodities();
    }
    
    private static void initializeClients() {
        // Institutional clients
        Client client1 = new Client("CLI001", "Pension Fund Alpha", "INSTITUTIONAL", true);
        client1.setLegalEntityIdentifier("549300ABCDEF123456789");
        client1.setJurisdiction("US");
        client1.setRegulatoryClassification("ECP");
        client1.setOnboardingDate(LocalDate.of(2020, 1, 15));
        client1.setAuthorizedProducts(Arrays.asList("COMMODITY_SWAPS", "EQUITY_DERIVATIVES", "FIXED_INCOME"));
        client1.setCreditLimit(new BigDecimal("100000000"));
        client1.setRiskRating("LOW");
        CLIENTS.put(client1.getClientId(), client1);
        
        Client client2 = new Client("CLI002", "Hedge Fund Beta", "INSTITUTIONAL", true);
        client2.setLegalEntityIdentifier("549300GHIJKL123456789");
        client2.setJurisdiction("US");
        client2.setRegulatoryClassification("ECP");
        client2.setOnboardingDate(LocalDate.of(2019, 6, 10));
        client2.setAuthorizedProducts(Arrays.asList("COMMODITY_SWAPS", "EQUITY_DERIVATIVES", "FX_DERIVATIVES"));
        client2.setCreditLimit(new BigDecimal("50000000"));
        client2.setRiskRating("MEDIUM");
        CLIENTS.put(client2.getClientId(), client2);
        
        Client client3 = new Client("CLI003", "Corporate Treasury Gamma", "INSTITUTIONAL", true);
        client3.setLegalEntityIdentifier("549300MNOPQR123456789");
        client3.setJurisdiction("UK");
        client3.setRegulatoryClassification("PROFESSIONAL");
        client3.setOnboardingDate(LocalDate.of(2021, 3, 22));
        client3.setAuthorizedProducts(Arrays.asList("COMMODITY_SWAPS", "FX_DERIVATIVES"));
        client3.setCreditLimit(new BigDecimal("25000000"));
        client3.setRiskRating("LOW");
        CLIENTS.put(client3.getClientId(), client3);
        
        // Inactive client for testing
        Client client4 = new Client("CLI004", "Inactive Fund Delta", "INSTITUTIONAL", false);
        client4.setLegalEntityIdentifier("549300STUVWX123456789");
        client4.setJurisdiction("US");
        client4.setRegulatoryClassification("ECP");
        client4.setOnboardingDate(LocalDate.of(2018, 12, 5));
        client4.setAuthorizedProducts(Arrays.asList("COMMODITY_SWAPS"));
        client4.setCreditLimit(new BigDecimal("10000000"));
        client4.setRiskRating("HIGH");
        CLIENTS.put(client4.getClientId(), client4);
    }
    
    private static void initializeClientAccounts() {
        // Accounts for CLI001
        ClientAccount acc1 = new ClientAccount("ACC001", "CLI001", "SEGREGATED", "USD", true);
        acc1.setAccountName("Pension Fund Alpha - Main Account");
        acc1.setOpenDate(LocalDate.of(2020, 1, 20));
        acc1.setAuthorizedInstruments(Arrays.asList("COMMODITY_TRS", "EQUITY_SWAPS"));
        acc1.setAccountLimit(new BigDecimal("50000000"));
        acc1.setAccountStatus("ACTIVE");
        CLIENT_ACCOUNTS.put(acc1.getAccountId(), acc1);
        
        // Accounts for CLI002
        ClientAccount acc2 = new ClientAccount("ACC002", "CLI002", "OMNIBUS", "USD", true);
        acc2.setAccountName("Hedge Fund Beta - Trading Account");
        acc2.setOpenDate(LocalDate.of(2019, 6, 15));
        acc2.setAuthorizedInstruments(Arrays.asList("COMMODITY_TRS", "EQUITY_SWAPS", "FX_SWAPS"));
        acc2.setAccountLimit(new BigDecimal("30000000"));
        acc2.setAccountStatus("ACTIVE");
        CLIENT_ACCOUNTS.put(acc2.getAccountId(), acc2);
        
        // Accounts for CLI003
        ClientAccount acc3 = new ClientAccount("ACC003", "CLI003", "SEGREGATED", "GBP", true);
        acc3.setAccountName("Corporate Treasury Gamma - Hedging Account");
        acc3.setOpenDate(LocalDate.of(2021, 3, 25));
        acc3.setAuthorizedInstruments(Arrays.asList("COMMODITY_TRS", "FX_FORWARDS"));
        acc3.setAccountLimit(new BigDecimal("15000000"));
        acc3.setAccountStatus("ACTIVE");
        CLIENT_ACCOUNTS.put(acc3.getAccountId(), acc3);
        
        // Suspended account for testing
        ClientAccount acc4 = new ClientAccount("ACC004", "CLI002", "HOUSE", "EUR", false);
        acc4.setAccountName("Hedge Fund Beta - Suspended Account");
        acc4.setOpenDate(LocalDate.of(2020, 8, 10));
        acc4.setAuthorizedInstruments(Arrays.asList("COMMODITY_TRS"));
        acc4.setAccountLimit(new BigDecimal("5000000"));
        acc4.setAccountStatus("SUSPENDED");
        CLIENT_ACCOUNTS.put(acc4.getAccountId(), acc4);
    }
    
    private static void initializeCounterparties() {
        Counterparty cp1 = new Counterparty("CP001", "Global Investment Bank", "BANK", true);
        cp1.setLegalEntityIdentifier("549300BANK123456789AB");
        cp1.setJurisdiction("US");
        cp1.setRegulatoryStatus("AUTHORIZED");
        cp1.setRatingAgency("S&P");
        cp1.setCreditRating("AA-");
        cp1.setCreditLimit(new BigDecimal("500000000"));
        cp1.setAuthorizedProducts(Arrays.asList("COMMODITY_SWAPS", "EQUITY_DERIVATIVES", "FIXED_INCOME", "FX_DERIVATIVES"));
        COUNTERPARTIES.put(cp1.getCounterpartyId(), cp1);
        
        Counterparty cp2 = new Counterparty("CP002", "Prime Brokerage Services", "BROKER_DEALER", true);
        cp2.setLegalEntityIdentifier("549300BROKER123456789C");
        cp2.setJurisdiction("UK");
        cp2.setRegulatoryStatus("AUTHORIZED");
        cp2.setRatingAgency("Moody's");
        cp2.setCreditRating("A1");
        cp2.setCreditLimit(new BigDecimal("200000000"));
        cp2.setAuthorizedProducts(Arrays.asList("COMMODITY_SWAPS", "EQUITY_DERIVATIVES"));
        COUNTERPARTIES.put(cp2.getCounterpartyId(), cp2);
        
        // Inactive counterparty for testing
        Counterparty cp3 = new Counterparty("CP003", "Inactive Trading House", "CORPORATE", false);
        cp3.setLegalEntityIdentifier("549300INACTIVE123456789");
        cp3.setJurisdiction("US");
        cp3.setRegulatoryStatus("SUSPENDED");
        cp3.setRatingAgency("Fitch");
        cp3.setCreditRating("BBB");
        cp3.setCreditLimit(new BigDecimal("10000000"));
        cp3.setAuthorizedProducts(Arrays.asList("COMMODITY_SWAPS"));
        COUNTERPARTIES.put(cp3.getCounterpartyId(), cp3);
    }
    
    private static void initializeCurrencies() {
        CURRENCIES.put("USD", new CurrencyData("USD", "US Dollar", 2, true, true));
        CURRENCIES.get("USD").setRegion("AMERICAS");

        CURRENCIES.put("EUR", new CurrencyData("EUR", "Euro", 2, true, true));
        CURRENCIES.get("EUR").setRegion("EUROPE");

        CURRENCIES.put("GBP", new CurrencyData("GBP", "British Pound", 2, true, true));
        CURRENCIES.get("GBP").setRegion("EUROPE");

        CURRENCIES.put("JPY", new CurrencyData("JPY", "Japanese Yen", 0, true, true));
        CURRENCIES.get("JPY").setRegion("ASIA_PACIFIC");

        CURRENCIES.put("CHF", new CurrencyData("CHF", "Swiss Franc", 2, true, true));
        CURRENCIES.get("CHF").setRegion("EUROPE");

        // Inactive currency for testing
        CURRENCIES.put("XYZ", new CurrencyData("XYZ", "Test Currency", 2, false, false));
        CURRENCIES.get("XYZ").setRegion("TEST");
    }
    
    private static void initializeCommodities() {
        // Energy commodities
        CommodityReference oil1 = new CommodityReference("WTI", "West Texas Intermediate Crude Oil", "ENERGY", "WTI", "USD", true);
        oil1.setIndexProvider("NYMEX");
        oil1.setUnitOfMeasure("BARREL");
        oil1.setTradeable(true);
        COMMODITIES.put(oil1.getCommodityCode(), oil1);
        
        CommodityReference oil2 = new CommodityReference("BRENT", "Brent Crude Oil", "ENERGY", "BRENT", "USD", true);
        oil2.setIndexProvider("ICE");
        oil2.setUnitOfMeasure("BARREL");
        oil2.setTradeable(true);
        COMMODITIES.put(oil2.getCommodityCode(), oil2);
        
        CommodityReference gas = new CommodityReference("NG", "Natural Gas", "ENERGY", "HENRY_HUB", "USD", true);
        gas.setIndexProvider("NYMEX");
        gas.setUnitOfMeasure("MMBTU");
        gas.setTradeable(true);
        COMMODITIES.put(gas.getCommodityCode(), gas);
        
        // Metals
        CommodityReference gold = new CommodityReference("GOLD", "Gold", "METALS", "COMEX_GOLD", "USD", true);
        gold.setIndexProvider("COMEX");
        gold.setUnitOfMeasure("TROY_OUNCE");
        gold.setTradeable(true);
        COMMODITIES.put(gold.getCommodityCode(), gold);
        
        CommodityReference silver = new CommodityReference("SILVER", "Silver", "METALS", "COMEX_SILVER", "USD", true);
        silver.setIndexProvider("COMEX");
        silver.setUnitOfMeasure("TROY_OUNCE");
        silver.setTradeable(true);
        COMMODITIES.put(silver.getCommodityCode(), silver);
        
        // Agricultural
        CommodityReference corn = new CommodityReference("CORN", "Corn", "AGRICULTURAL", "CBOT_CORN", "USD", true);
        corn.setIndexProvider("CBOT");
        corn.setUnitOfMeasure("BUSHEL");
        corn.setTradeable(true);
        COMMODITIES.put(corn.getCommodityCode(), corn);
        
        // Inactive commodity for testing
        CommodityReference inactive = new CommodityReference("INACTIVE", "Inactive Commodity", "TEST", "TEST_INDEX", "USD", false);
        inactive.setIndexProvider("TEST");
        inactive.setUnitOfMeasure("UNIT");
        inactive.setTradeable(false);
        COMMODITIES.put(inactive.getCommodityCode(), inactive);
    }
    
    // Public accessor methods
    public static Client getClient(String clientId) {
        return CLIENTS.get(clientId);
    }
    
    public static ClientAccount getClientAccount(String accountId) {
        return CLIENT_ACCOUNTS.get(accountId);
    }
    
    public static Counterparty getCounterparty(String counterpartyId) {
        return COUNTERPARTIES.get(counterpartyId);
    }
    
    public static CurrencyData getCurrency(String currencyCode) {
        return CURRENCIES.get(currencyCode);
    }
    
    public static CommodityReference getCommodity(String commodityCode) {
        return COMMODITIES.get(commodityCode);
    }
    
    // Collection accessors
    public static Collection<Client> getAllClients() {
        return CLIENTS.values();
    }
    
    public static Collection<ClientAccount> getAllClientAccounts() {
        return CLIENT_ACCOUNTS.values();
    }
    
    public static Collection<Counterparty> getAllCounterparties() {
        return COUNTERPARTIES.values();
    }
    
    public static Collection<CurrencyData> getAllCurrencies() {
        return CURRENCIES.values();
    }
    
    public static Collection<CommodityReference> getAllCommodities() {
        return COMMODITIES.values();
    }
    
    // Utility methods
    public static List<ClientAccount> getClientAccountsForClient(String clientId) {
        return CLIENT_ACCOUNTS.values().stream()
                .filter(account -> clientId.equals(account.getClientId()))
                .toList();
    }
    
    public static boolean isValidClient(String clientId) {
        Client client = getClient(clientId);
        return client != null && client.getActive();
    }
    
    public static boolean isValidCounterparty(String counterpartyId) {
        Counterparty counterparty = getCounterparty(counterpartyId);
        return counterparty != null && counterparty.getActive();
    }
    
    public static boolean isValidCurrency(String currencyCode) {
        CurrencyData currency = getCurrency(currencyCode);
        return currency != null && currency.getActive() && currency.getTradeable();
    }
    
    public static boolean isValidCommodity(String commodityCode) {
        CommodityReference commodity = getCommodity(commodityCode);
        return commodity != null && commodity.getActive() && commodity.getTradeable();
    }
}
