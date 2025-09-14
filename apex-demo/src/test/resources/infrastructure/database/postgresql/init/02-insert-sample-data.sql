-- APEX Demo Database Sample Data
-- Inserts realistic financial services test data for PostgreSQL lookup demonstrations
-- Author: APEX Demo Team
-- Version: 1.0

SET search_path TO customer_data, trading_data, reference_data, public;

-- =====================================================
-- REFERENCE DATA (Insert first due to foreign keys)
-- =====================================================

-- Insert currencies
INSERT INTO reference_data.currencies (currency_code, currency_name, currency_symbol, decimal_places, country_code, region) VALUES
('USD', 'US Dollar', '$', 2, 'US', 'North America'),
('EUR', 'Euro', '€', 2, 'DE', 'Europe'),
('GBP', 'British Pound', '£', 2, 'GB', 'Europe'),
('JPY', 'Japanese Yen', '¥', 0, 'JP', 'Asia Pacific'),
('CHF', 'Swiss Franc', 'CHF', 2, 'CH', 'Europe'),
('CAD', 'Canadian Dollar', 'C$', 2, 'CA', 'North America'),
('AUD', 'Australian Dollar', 'A$', 2, 'AU', 'Asia Pacific'),
('HKD', 'Hong Kong Dollar', 'HK$', 2, 'HK', 'Asia Pacific'),
('SGD', 'Singapore Dollar', 'S$', 2, 'SG', 'Asia Pacific'),
('KRW', 'Korean Won', '₩', 0, 'KR', 'Asia Pacific');

-- Insert countries
INSERT INTO reference_data.countries (country_code, country_name, region, time_zone, regulatory_zone, settlement_system, standard_settlement_days) VALUES
('US', 'United States', 'North America', 'America/New_York', 'SEC', 'DTC', 2),
('GB', 'United Kingdom', 'Europe', 'Europe/London', 'FCA', 'CREST', 2),
('DE', 'Germany', 'Europe', 'Europe/Berlin', 'BAFIN', 'CBF', 2),
('JP', 'Japan', 'Asia Pacific', 'Asia/Tokyo', 'JFSA', 'JASDEC', 2),
('CH', 'Switzerland', 'Europe', 'Europe/Zurich', 'FINMA', 'SIX SIS', 2),
('CA', 'Canada', 'North America', 'America/Toronto', 'CSA', 'CDS', 2),
('AU', 'Australia', 'Asia Pacific', 'Australia/Sydney', 'ASIC', 'ASX Settlement', 2),
('HK', 'Hong Kong', 'Asia Pacific', 'Asia/Hong_Kong', 'SFC', 'CCASS', 2),
('SG', 'Singapore', 'Asia Pacific', 'Asia/Singapore', 'MAS', 'CDP', 2),
('KR', 'South Korea', 'Asia Pacific', 'Asia/Seoul', 'FSC', 'KSD', 2);

-- =====================================================
-- CUSTOMER DATA
-- =====================================================

-- Insert customers
INSERT INTO customer_data.customers (customer_id, customer_name, customer_type, tier, region, status) VALUES
('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE'),
('CUST000002', 'Global Investment Partners', 'INSTITUTIONAL', 'GOLD', 'EU', 'ACTIVE'),
('CUST000003', 'Pacific Asset Management', 'INSTITUTIONAL', 'GOLD', 'APAC', 'ACTIVE'),
('CUST000004', 'John Smith', 'INDIVIDUAL', 'SILVER', 'NA', 'ACTIVE'),
('CUST000005', 'European Pension Fund', 'INSTITUTIONAL', 'PLATINUM', 'EU', 'ACTIVE'),
('CUST000006', 'Asia Trading Ltd', 'CORPORATE', 'GOLD', 'APAC', 'ACTIVE'),
('CUST000007', 'Sarah Johnson', 'INDIVIDUAL', 'BASIC', 'EU', 'ACTIVE'),
('CUST000008', 'Latin America Holdings', 'CORPORATE', 'SILVER', 'LATAM', 'ACTIVE'),
('CUST000009', 'Middle East Investment Co', 'INSTITUTIONAL', 'GOLD', 'ME', 'ACTIVE'),
('CUST000010', 'Test Customer Inactive', 'INDIVIDUAL', 'BASIC', 'NA', 'INACTIVE');

-- Insert customer profiles
INSERT INTO customer_data.customer_profiles (customer_id, email, phone, city, country, kyc_status, risk_rating, credit_score, annual_income, net_worth) VALUES
('CUST000001', 'contact@acmecorp.com', '+1-555-0101', 'New York', 'US', 'APPROVED', 'LOW', 750, 50000000.00, 200000000.00),
('CUST000002', 'info@globalinvest.com', '+44-20-7000-0001', 'London', 'GB', 'APPROVED', 'LOW', 780, 25000000.00, 100000000.00),
('CUST000003', 'contact@pacificasset.com', '+65-6000-0001', 'Singapore', 'SG', 'APPROVED', 'MEDIUM', 720, 15000000.00, 75000000.00),
('CUST000004', 'john.smith@email.com', '+1-555-0104', 'Chicago', 'US', 'APPROVED', 'MEDIUM', 680, 150000.00, 500000.00),
('CUST000005', 'pension@europefund.eu', '+49-30-0000-0001', 'Frankfurt', 'DE', 'APPROVED', 'LOW', 800, 100000000.00, 500000000.00),
('CUST000006', 'trading@asiatrading.com', '+852-2000-0001', 'Hong Kong', 'HK', 'APPROVED', 'MEDIUM', 710, 10000000.00, 50000000.00),
('CUST000007', 'sarah.johnson@email.com', '+44-20-7000-0007', 'London', 'GB', 'APPROVED', 'HIGH', 620, 75000.00, 200000.00),
('CUST000008', 'holdings@latam.com', '+55-11-0000-0001', 'São Paulo', 'BR', 'PENDING', 'MEDIUM', 650, 5000000.00, 25000000.00),
('CUST000009', 'invest@meinvest.com', '+971-4-000-0001', 'Dubai', 'AE', 'APPROVED', 'MEDIUM', 700, 20000000.00, 80000000.00),
('CUST000010', 'test@inactive.com', '+1-555-0110', 'Test City', 'US', 'EXPIRED', 'HIGH', 500, 0.00, 0.00);

-- =====================================================
-- TRADING DATA
-- =====================================================

-- Insert markets
INSERT INTO trading_data.markets (market_code, market_name, country, time_zone, settlement_cycle, cut_off_time, volatility_rating, liquidity_rating) VALUES
('NYSE', 'New York Stock Exchange', 'US', 'America/New_York', 'T+2', '16:00:00', 'MEDIUM', 'HIGH'),
('NASDAQ', 'NASDAQ', 'US', 'America/New_York', 'T+2', '16:00:00', 'HIGH', 'HIGH'),
('LSE', 'London Stock Exchange', 'GB', 'Europe/London', 'T+2', '16:30:00', 'MEDIUM', 'HIGH'),
('XETRA', 'XETRA', 'DE', 'Europe/Berlin', 'T+2', '17:30:00', 'MEDIUM', 'HIGH'),
('TSE', 'Tokyo Stock Exchange', 'JP', 'Asia/Tokyo', 'T+2', '15:00:00', 'MEDIUM', 'HIGH'),
('SIX', 'SIX Swiss Exchange', 'CH', 'Europe/Zurich', 'T+2', '17:30:00', 'LOW', 'MEDIUM'),
('TSX', 'Toronto Stock Exchange', 'CA', 'America/Toronto', 'T+2', '16:00:00', 'MEDIUM', 'MEDIUM'),
('ASX', 'Australian Securities Exchange', 'AU', 'Australia/Sydney', 'T+2', '16:00:00', 'MEDIUM', 'MEDIUM'),
('HKEX', 'Hong Kong Exchange', 'HK', 'Asia/Hong_Kong', 'T+2', '16:00:00', 'HIGH', 'HIGH'),
('SGX', 'Singapore Exchange', 'SG', 'Asia/Singapore', 'T+2', '17:00:00', 'MEDIUM', 'MEDIUM');

-- Insert instruments
INSERT INTO trading_data.instruments (instrument_type, instrument_name, instrument_class, settlement_currency, typical_settlement_days) VALUES
('EQUITY', 'Equity Securities', 'EQUITY', NULL, 2),
('BOND', 'Fixed Income Securities', 'FIXED_INCOME', NULL, 2),
('GOVT_BOND', 'Government Bonds', 'FIXED_INCOME', NULL, 1),
('CORP_BOND', 'Corporate Bonds', 'FIXED_INCOME', NULL, 2),
('COMMODITY_SWAP', 'Commodity Total Return Swap', 'DERIVATIVE', 'USD', 2),
('FX_SWAP', 'Foreign Exchange Swap', 'FX', NULL, 2),
('DERIVATIVE', 'OTC Derivatives', 'DERIVATIVE', NULL, 2),
('ETF', 'Exchange Traded Fund', 'EQUITY', NULL, 2),
('REIT', 'Real Estate Investment Trust', 'EQUITY', NULL, 2),
('COMMODITY', 'Physical Commodities', 'COMMODITY', 'USD', 5);

-- Insert counterparties
INSERT INTO trading_data.counterparties (counterparty_id, counterparty_name, counterparty_type, credit_rating, credit_limit, jurisdiction, bic_code, lei_code) VALUES
('CP_GS', 'Goldman Sachs', 'BANK', 'A+', 500000000.00, 'US', 'GSCCUS33XXX', '784F5XWPLTWKTBV3E584'),
('CP_DB', 'Deutsche Bank', 'BANK', 'A-', 300000000.00, 'DE', 'DEUTDEFFXXX', '7LTWFZYICNSX8D621K86'),
('CP_JPM', 'JP Morgan', 'BANK', 'AA-', 750000000.00, 'US', 'CHASUS33XXX', '8I5DZWZKVSZI9VCR0T23'),
('CP_BARC', 'Barclays', 'BANK', 'A', 400000000.00, 'GB', 'BARCGB22XXX', 'G5GSEF7VJP5I7OUK5573'),
('CP_CS', 'Credit Suisse', 'BANK', 'BBB+', 250000000.00, 'CH', 'CRESCHZZXXX', 'ANGGYXNX0JLX3X63JN86'),
('CP_UBS', 'UBS', 'BANK', 'A+', 600000000.00, 'CH', 'UBSWCHZH80A', 'BFM8T61CT2L1QCEMIK50'),
('CP_HSBC', 'HSBC', 'BANK', 'A', 450000000.00, 'GB', 'HBUKGB4BXXX', '80528SBNKL29W9W3XY82'),
('CP_BNP', 'BNP Paribas', 'BANK', 'A', 350000000.00, 'FR', 'BNPAFRPPXXX', 'R0MUWSFPU8MPRO8K5P83'),
('CP_CITI', 'Citigroup', 'BANK', 'A+', 550000000.00, 'US', 'CITIUS33XXX', '6SHGI4ZSSLCXXQSBB395'),
('CP_MS', 'Morgan Stanley', 'BANK', 'A', 400000000.00, 'US', 'MSINUS33XXX', 'IGJSJL3JD5P30I6NJZ34');

-- Insert custodians
INSERT INTO trading_data.custodians (custodian_id, custodian_name, custodian_bic, custodian_address, jurisdiction, services) VALUES
('CUST_GS_US', 'Goldman Sachs Custody', 'GSCCUS33XXX', '200 West Street, New York, NY 10282', 'US', ARRAY['CUSTODY', 'SETTLEMENT', 'REPORTING']),
('CUST_JPM_US', 'JP Morgan Custody', 'CHASUS33XXX', '383 Madison Avenue, New York, NY 10179', 'US', ARRAY['CUSTODY', 'SETTLEMENT', 'REPORTING', 'FX']),
('CUST_DB_DE', 'Deutsche Bank Custody', 'DEUTDEFFXXX', 'Taunusanlage 12, 60325 Frankfurt am Main', 'DE', ARRAY['CUSTODY', 'SETTLEMENT', 'REPORTING']),
('CUST_BARC_GB', 'Barclays Custody', 'BARCGB22XXX', '1 Churchill Place, London E14 5HP', 'GB', ARRAY['CUSTODY', 'SETTLEMENT', 'REPORTING']),
('CUST_UBS_CH', 'UBS Custody', 'UBSWCHZH80A', 'Bahnhofstrasse 45, 8001 Zurich', 'CH', ARRAY['CUSTODY', 'SETTLEMENT', 'REPORTING', 'WEALTH']),
('CUST_HSBC_HK', 'HSBC Custody Hong Kong', 'HSBCHKHHHKH', '1 Queen\'s Road Central, Hong Kong', 'HK', ARRAY['CUSTODY', 'SETTLEMENT', 'REPORTING']),
('CUST_DBS_SG', 'DBS Custody Singapore', 'DBSSSGSGXXX', '12 Marina Boulevard, Singapore 018982', 'SG', ARRAY['CUSTODY', 'SETTLEMENT', 'REPORTING']),
('CUST_SMBC_JP', 'SMBC Custody Japan', 'SMBCJPJTXXX', '1-1-2 Yurakucho, Chiyoda-ku, Tokyo 100-0006', 'JP', ARRAY['CUSTODY', 'SETTLEMENT', 'REPORTING']),
('CUST_RBC_CA', 'RBC Custody Canada', 'ROYCCAT2XXX', '200 Bay Street, Toronto, ON M5J 2J5', 'CA', ARRAY['CUSTODY', 'SETTLEMENT', 'REPORTING']),
('CUST_CBA_AU', 'Commonwealth Bank Custody', 'CTBAAU2SXXX', 'Ground Floor, Tower 1, 201 Sussex Street, Sydney NSW 2000', 'AU', ARRAY['CUSTODY', 'SETTLEMENT', 'REPORTING']);

-- Insert settlement instructions (for multi-parameter lookup demo)
INSERT INTO trading_data.settlement_instructions (counterparty_id, custodian_id, instrument_type, currency, market, settlement_method, delivery_instruction, special_instructions, min_amount, max_amount, priority) VALUES
-- Goldman Sachs instructions
('CP_GS', 'CUST_GS_US', 'EQUITY', 'USD', 'NYSE', 'DVP', 'DELIVER', 'Standard equity settlement', 1000.00, 100000000.00, 1),
('CP_GS', 'CUST_GS_US', 'BOND', 'USD', 'NYSE', 'DVP', 'DELIVER', 'Fixed income settlement', 10000.00, 500000000.00, 1),
('CP_GS', 'CUST_JPM_US', 'DERIVATIVE', 'USD', 'NYSE', 'DVP_PREMIUM', 'DELIVER', 'OTC derivative settlement', 100000.00, 1000000000.00, 2),

-- Deutsche Bank instructions
('CP_DB', 'CUST_DB_DE', 'EQUITY', 'EUR', 'XETRA', 'DVP', 'DELIVER', 'European equity settlement', 1000.00, 50000000.00, 1),
('CP_DB', 'CUST_DB_DE', 'BOND', 'EUR', 'XETRA', 'DVP', 'DELIVER', 'European bond settlement', 10000.00, 200000000.00, 1),
('CP_DB', 'CUST_UBS_CH', 'FX_SWAP', 'CHF', 'SIX', 'DVP', 'DELIVER', 'FX swap settlement via UBS', 50000.00, 25000000.00, 2),

-- JP Morgan instructions
('CP_JPM', 'CUST_JPM_US', 'EQUITY', 'USD', 'NYSE', 'DVP', 'DELIVER', 'Prime brokerage equity', 1000.00, 200000000.00, 1),
('CP_JPM', 'CUST_JPM_US', 'COMMODITY_SWAP', 'USD', 'NYSE', 'DVP_PREMIUM', 'DELIVER', 'Commodity swap settlement', 1000000.00, 500000000.00, 1),
('CP_JPM', 'CUST_HSBC_HK', 'EQUITY', 'HKD', 'HKEX', 'DVP', 'DELIVER', 'Asia Pacific equity via HSBC', 10000.00, 100000000.00, 2),

-- Barclays instructions
('CP_BARC', 'CUST_BARC_GB', 'EQUITY', 'GBP', 'LSE', 'DVP', 'DELIVER', 'UK equity settlement', 1000.00, 75000000.00, 1),
('CP_BARC', 'CUST_BARC_GB', 'BOND', 'GBP', 'LSE', 'DVP', 'DELIVER', 'UK gilt settlement', 10000.00, 300000000.00, 1),
('CP_BARC', 'CUST_DB_DE', 'EQUITY', 'EUR', 'XETRA', 'DVP', 'DELIVER', 'Cross-border EU equity', 5000.00, 50000000.00, 3),

-- Credit Suisse instructions
('CP_CS', 'CUST_UBS_CH', 'EQUITY', 'CHF', 'SIX', 'DVP', 'DELIVER', 'Swiss equity settlement', 1000.00, 30000000.00, 1),
('CP_CS', 'CUST_UBS_CH', 'BOND', 'CHF', 'SIX', 'DVP', 'DELIVER', 'Swiss bond settlement', 10000.00, 100000000.00, 1),

-- UBS instructions
('CP_UBS', 'CUST_UBS_CH', 'EQUITY', 'CHF', 'SIX', 'DVP_PREMIUM', 'DELIVER', 'Premium Swiss equity', 1000.00, 100000000.00, 1),
('CP_UBS', 'CUST_UBS_CH', 'DERIVATIVE', 'USD', 'SIX', 'DVP_PREMIUM', 'DELIVER', 'Structured products', 100000.00, 500000000.00, 1),

-- HSBC instructions
('CP_HSBC', 'CUST_HSBC_HK', 'EQUITY', 'HKD', 'HKEX', 'DVP', 'DELIVER', 'Hong Kong equity', 1000.00, 100000000.00, 1),
('CP_HSBC', 'CUST_DBS_SG', 'EQUITY', 'SGD', 'SGX', 'DVP', 'DELIVER', 'Singapore equity via DBS', 1000.00, 50000000.00, 2),

-- Default fallback instructions
('DEFAULT', 'CUST_JPM_US', 'EQUITY', 'USD', 'NYSE', 'DVP', 'DELIVER', 'Default US equity fallback', 1000.00, 10000000.00, 10),
('DEFAULT', 'CUST_DB_DE', 'EQUITY', 'EUR', 'XETRA', 'DVP', 'DELIVER', 'Default EU equity fallback', 1000.00, 10000000.00, 10),
('DEFAULT', 'CUST_BARC_GB', 'EQUITY', 'GBP', 'LSE', 'DVP', 'DELIVER', 'Default UK equity fallback', 1000.00, 10000000.00, 10);

-- Insert risk assessments (for multi-parameter lookup demo)
INSERT INTO trading_data.risk_assessments (counterparty_id, instrument_type, market, risk_category, risk_score, max_exposure, max_single_trade, approval_required, monitoring_level) VALUES
-- Goldman Sachs risk assessments
('CP_GS', 'EQUITY', 'NYSE', 'LOW', 15.50, 500000000.00, 100000000.00, FALSE, 'STANDARD'),
('CP_GS', 'BOND', 'NYSE', 'LOW', 12.25, 750000000.00, 200000000.00, FALSE, 'STANDARD'),
('CP_GS', 'DERIVATIVE', 'NYSE', 'MEDIUM', 35.75, 1000000000.00, 500000000.00, TRUE, 'ENHANCED'),

-- Deutsche Bank risk assessments
('CP_DB', 'EQUITY', 'XETRA', 'MEDIUM', 25.80, 300000000.00, 50000000.00, FALSE, 'STANDARD'),
('CP_DB', 'BOND', 'XETRA', 'LOW', 18.90, 400000000.00, 100000000.00, FALSE, 'STANDARD'),
('CP_DB', 'FX_SWAP', 'SIX', 'MEDIUM', 28.45, 200000000.00, 25000000.00, TRUE, 'ENHANCED'),

-- JP Morgan risk assessments
('CP_JPM', 'EQUITY', 'NYSE', 'LOW', 10.25, 750000000.00, 200000000.00, FALSE, 'STANDARD'),
('CP_JPM', 'COMMODITY_SWAP', 'NYSE', 'HIGH', 65.30, 500000000.00, 100000000.00, TRUE, 'INTENSIVE'),
('CP_JPM', 'EQUITY', 'HKEX', 'MEDIUM', 32.15, 200000000.00, 50000000.00, FALSE, 'ENHANCED'),

-- Barclays risk assessments
('CP_BARC', 'EQUITY', 'LSE', 'MEDIUM', 22.60, 400000000.00, 75000000.00, FALSE, 'STANDARD'),
('CP_BARC', 'BOND', 'LSE', 'LOW', 16.40, 500000000.00, 150000000.00, FALSE, 'STANDARD'),

-- Credit Suisse risk assessments
('CP_CS', 'EQUITY', 'SIX', 'HIGH', 45.85, 250000000.00, 30000000.00, TRUE, 'ENHANCED'),
('CP_CS', 'BOND', 'SIX', 'MEDIUM', 28.70, 300000000.00, 50000000.00, FALSE, 'STANDARD'),

-- UBS risk assessments
('CP_UBS', 'EQUITY', 'SIX', 'LOW', 14.20, 600000000.00, 100000000.00, FALSE, 'STANDARD'),
('CP_UBS', 'DERIVATIVE', 'SIX', 'MEDIUM', 38.95, 800000000.00, 200000000.00, TRUE, 'ENHANCED'),

-- HSBC risk assessments
('CP_HSBC', 'EQUITY', 'HKEX', 'MEDIUM', 26.75, 450000000.00, 75000000.00, FALSE, 'STANDARD'),
('CP_HSBC', 'EQUITY', 'SGX', 'MEDIUM', 29.40, 300000000.00, 50000000.00, FALSE, 'ENHANCED');
