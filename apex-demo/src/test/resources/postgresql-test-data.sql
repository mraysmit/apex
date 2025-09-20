-- PostgreSQL Test Data Initialization Script
-- Used by PostgreSQLSimpleLookupTest.java for Phase 1.1 implementation
-- Used by PostgreSQLMultiParamLookupTest.java for Phase 1.2 implementation
-- Creates customer table schema and trading-related tables with test data

-- Drop existing tables if they exist (for clean test runs)
DROP TABLE IF EXISTS settlement_instructions CASCADE;
DROP TABLE IF EXISTS risk_assessments CASCADE;
DROP TABLE IF EXISTS counterparties CASCADE;
DROP TABLE IF EXISTS custodians CASCADE;
DROP TABLE IF EXISTS markets CASCADE;
DROP TABLE IF EXISTS instruments CASCADE;
DROP TABLE IF EXISTS customers CASCADE;

-- Create customers table with PostgreSQL-specific features
CREATE TABLE customers (
    customer_id VARCHAR(20) PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    customer_type VARCHAR(20) NOT NULL,
    tier VARCHAR(20) NOT NULL,
    region VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_date DATE DEFAULT CURRENT_DATE,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- PostgreSQL-specific: JSON column for additional metadata
    metadata JSONB,
    
    -- PostgreSQL-specific: Array column for tags
    tags TEXT[],
    
    -- Constraints
    CONSTRAINT chk_customer_type CHECK (customer_type IN ('CORPORATE', 'INSTITUTIONAL', 'RETAIL')),
    CONSTRAINT chk_tier CHECK (tier IN ('PLATINUM', 'GOLD', 'SILVER', 'BRONZE')),
    CONSTRAINT chk_region CHECK (region IN ('NA', 'EU', 'APAC', 'LATAM')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

-- Create index for performance testing
CREATE INDEX idx_customers_type_tier ON customers(customer_type, tier);
CREATE INDEX idx_customers_region_status ON customers(region, status);

-- Insert comprehensive test data
INSERT INTO customers (
    customer_id, 
    customer_name, 
    customer_type, 
    tier, 
    region, 
    status, 
    created_date,
    metadata,
    tags
) VALUES 
-- Test data for basic lookup validation
('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15',
 '{"industry": "Technology", "employees": 5000, "revenue": 1000000000}',
 ARRAY['tech', 'large-cap', 'nasdaq']),

('CUST000002', 'Beta Industries', 'CORPORATE', 'GOLD', 'EU', 'ACTIVE', '2023-02-20',
 '{"industry": "Manufacturing", "employees": 2500, "revenue": 500000000}',
 ARRAY['manufacturing', 'mid-cap', 'ftse']),

('CUST000003', 'Gamma Holdings', 'INSTITUTIONAL', 'PLATINUM', 'APAC', 'ACTIVE', '2023-03-10',
 '{"industry": "Financial Services", "aum": 50000000000, "fund_type": "hedge"}',
 ARRAY['finance', 'institutional', 'hedge-fund']),

('CUST000004', 'Delta Partners', 'CORPORATE', 'SILVER', 'NA', 'ACTIVE', '2023-04-05',
 '{"industry": "Real Estate", "employees": 500, "revenue": 100000000}',
 ARRAY['real-estate', 'small-cap', 'reit']),

('CUST000005', 'Epsilon Fund', 'INSTITUTIONAL', 'GOLD', 'EU', 'ACTIVE', '2023-05-12',
 '{"industry": "Asset Management", "aum": 25000000000, "fund_type": "mutual"}',
 ARRAY['asset-mgmt', 'institutional', 'mutual-fund']),

-- Additional test data for edge cases
('CUST000006', 'Zeta Ventures', 'CORPORATE', 'BRONZE', 'LATAM', 'INACTIVE', '2023-06-01',
 '{"industry": "Venture Capital", "employees": 50, "revenue": 10000000}',
 ARRAY['vc', 'startup', 'early-stage']),

('CUST000007', 'Eta Systems', 'RETAIL', 'SILVER', 'APAC', 'SUSPENDED', '2023-07-15',
 '{"industry": "Software", "employees": 100, "revenue": 5000000}',
 ARRAY['software', 'saas', 'b2b']),

-- Test data with special characters and edge cases
('CUST000008', 'Theta & Associates LLC', 'CORPORATE', 'GOLD', 'NA', 'ACTIVE', '2023-08-20',
 '{"industry": "Legal Services", "employees": 200, "revenue": 50000000}',
 ARRAY['legal', 'professional-services', 'llc']),

-- Test data with NULL metadata (for NULL handling tests)
('CUST000009', 'Iota Consulting', 'CORPORATE', 'SILVER', 'EU', 'ACTIVE', '2023-09-10',
 NULL,
 ARRAY['consulting']),

-- Test data with empty arrays
('CUST000010', 'Kappa Industries', 'CORPORATE', 'BRONZE', 'APAC', 'ACTIVE', '2023-10-05',
 '{"industry": "Mining", "employees": 1000, "revenue": 200000000}',
 ARRAY[]::TEXT[]);

-- Create a view for testing complex queries
CREATE VIEW customer_summary AS
SELECT 
    customer_id,
    customer_name,
    customer_type,
    tier,
    region,
    status,
    EXTRACT(YEAR FROM created_date) as creation_year,
    CASE 
        WHEN metadata->>'revenue' IS NOT NULL THEN (metadata->>'revenue')::BIGINT
        WHEN metadata->>'aum' IS NOT NULL THEN (metadata->>'aum')::BIGINT
        ELSE 0
    END as financial_value,
    array_length(tags, 1) as tag_count
FROM customers;

-- Insert performance test data (for later phases)
-- This will help with concurrent access testing
DO $$
DECLARE
    i INTEGER;
BEGIN
    FOR i IN 1..100 LOOP
        INSERT INTO customers (
            customer_id,
            customer_name,
            customer_type,
            tier,
            region,
            status,
            created_date,
            metadata,
            tags
        ) VALUES (
            'PERF' || LPAD(i::TEXT, 6, '0'),
            'Performance Test Customer ' || i,
            CASE (i % 3) 
                WHEN 0 THEN 'CORPORATE'
                WHEN 1 THEN 'INSTITUTIONAL'
                ELSE 'RETAIL'
            END,
            CASE (i % 4)
                WHEN 0 THEN 'PLATINUM'
                WHEN 1 THEN 'GOLD'
                WHEN 2 THEN 'SILVER'
                ELSE 'BRONZE'
            END,
            CASE (i % 4)
                WHEN 0 THEN 'NA'
                WHEN 1 THEN 'EU'
                WHEN 2 THEN 'APAC'
                ELSE 'LATAM'
            END,
            'ACTIVE',
            CURRENT_DATE - (i || ' days')::INTERVAL,
            ('{"test_id": ' || i || ', "performance_test": true}')::JSONB,
            ARRAY['performance', 'test', 'customer-' || i]
        );
    END LOOP;
END $$;

-- Create statistics for query optimization
ANALYZE customers;

-- ============================================================================
-- TRADING-RELATED TABLES FOR MULTI-PARAMETER LOOKUP TESTING (Phase 1.2)
-- ============================================================================

-- Create counterparties table
CREATE TABLE counterparties (
    counterparty_id VARCHAR(20) PRIMARY KEY,
    counterparty_name VARCHAR(100) NOT NULL,
    counterparty_type VARCHAR(20) NOT NULL,
    credit_rating VARCHAR(10),
    credit_limit BIGINT,
    jurisdiction VARCHAR(10) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_date DATE DEFAULT CURRENT_DATE,

    CONSTRAINT chk_cp_type CHECK (counterparty_type IN ('BANK', 'BROKER', 'FUND', 'CORPORATE')),
    CONSTRAINT chk_cp_rating CHECK (credit_rating IN ('AAA', 'AA', 'A', 'BBB', 'BB', 'B', 'CCC')),
    CONSTRAINT chk_cp_jurisdiction CHECK (jurisdiction IN ('US', 'UK', 'EU', 'JP', 'HK', 'SG'))
);

-- Create custodians table
CREATE TABLE custodians (
    custodian_id VARCHAR(20) PRIMARY KEY,
    custodian_name VARCHAR(100) NOT NULL,
    custodian_bic VARCHAR(11) NOT NULL,
    custodian_address TEXT,
    jurisdiction VARCHAR(10) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',

    CONSTRAINT chk_cust_jurisdiction CHECK (jurisdiction IN ('US', 'UK', 'EU', 'JP', 'HK', 'SG'))
);

-- Create markets table
CREATE TABLE markets (
    market_code VARCHAR(10) PRIMARY KEY,
    market_name VARCHAR(100) NOT NULL,
    settlement_cycle INTEGER NOT NULL,
    cut_off_time TIME,
    time_zone VARCHAR(20),
    volatility_rating VARCHAR(10),
    liquidity_rating VARCHAR(10),
    jurisdiction VARCHAR(10) NOT NULL,

    CONSTRAINT chk_settlement_cycle CHECK (settlement_cycle BETWEEN 0 AND 5),
    CONSTRAINT chk_volatility CHECK (volatility_rating IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_liquidity CHECK (liquidity_rating IN ('HIGH', 'MEDIUM', 'LOW'))
);

-- Create instruments table
CREATE TABLE instruments (
    instrument_type VARCHAR(20) PRIMARY KEY,
    instrument_name VARCHAR(100) NOT NULL,
    instrument_class VARCHAR(20) NOT NULL,
    settlement_currency VARCHAR(3) NOT NULL,

    CONSTRAINT chk_inst_class CHECK (instrument_class IN ('EQUITY', 'BOND', 'DERIVATIVE', 'FX'))
);

-- Create settlement_instructions table
CREATE TABLE settlement_instructions (
    instruction_id VARCHAR(30) PRIMARY KEY,
    counterparty_id VARCHAR(20) NOT NULL,
    custodian_id VARCHAR(20) NOT NULL,
    instrument_type VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    market VARCHAR(10) NOT NULL,
    settlement_method VARCHAR(20) NOT NULL,
    delivery_instruction TEXT,
    special_instructions TEXT,
    min_amount DECIMAL(18,2),
    max_amount DECIMAL(18,2),
    priority INTEGER DEFAULT 1,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_date DATE DEFAULT CURRENT_DATE,

    CONSTRAINT fk_si_counterparty FOREIGN KEY (counterparty_id) REFERENCES counterparties(counterparty_id),
    CONSTRAINT fk_si_custodian FOREIGN KEY (custodian_id) REFERENCES custodians(custodian_id),
    CONSTRAINT fk_si_instrument FOREIGN KEY (instrument_type) REFERENCES instruments(instrument_type),
    CONSTRAINT fk_si_market FOREIGN KEY (market) REFERENCES markets(market_code),
    CONSTRAINT chk_si_method CHECK (settlement_method IN ('DVP', 'FOP', 'PVP', 'NETTING')),
    CONSTRAINT chk_si_priority CHECK (priority BETWEEN 1 AND 10)
);

-- Create risk_assessments table
CREATE TABLE risk_assessments (
    assessment_id VARCHAR(30) PRIMARY KEY,
    counterparty_id VARCHAR(20) NOT NULL,
    instrument_type VARCHAR(20) NOT NULL,
    market VARCHAR(10) NOT NULL,
    risk_category VARCHAR(20) NOT NULL,
    risk_score INTEGER NOT NULL,
    max_exposure DECIMAL(18,2),
    max_single_trade DECIMAL(18,2),
    approval_required BOOLEAN DEFAULT FALSE,
    monitoring_level VARCHAR(20) DEFAULT 'STANDARD',
    effective_date DATE NOT NULL,
    expiry_date DATE,

    CONSTRAINT fk_ra_counterparty FOREIGN KEY (counterparty_id) REFERENCES counterparties(counterparty_id),
    CONSTRAINT fk_ra_instrument FOREIGN KEY (instrument_type) REFERENCES instruments(instrument_type),
    CONSTRAINT fk_ra_market FOREIGN KEY (market) REFERENCES markets(market_code),
    CONSTRAINT chk_ra_category CHECK (risk_category IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_ra_score CHECK (risk_score BETWEEN 1 AND 100),
    CONSTRAINT chk_ra_monitoring CHECK (monitoring_level IN ('MINIMAL', 'STANDARD', 'ENHANCED', 'CRITICAL'))
);

-- ============================================================================
-- INSERT TEST DATA FOR TRADING TABLES
-- ============================================================================

-- Insert counterparties test data
INSERT INTO counterparties (counterparty_id, counterparty_name, counterparty_type, credit_rating, credit_limit, jurisdiction) VALUES
('CP001', 'Goldman Sachs', 'BANK', 'AA', 5000000000, 'US'),
('CP002', 'JPMorgan Chase', 'BANK', 'AAA', 10000000000, 'US'),
('CP003', 'Deutsche Bank', 'BANK', 'A', 3000000000, 'EU'),
('CP004', 'HSBC Holdings', 'BANK', 'AA', 4000000000, 'UK'),
('CP005', 'BlackRock Fund', 'FUND', 'AAA', 2000000000, 'US'),
('CP006', 'Vanguard Group', 'FUND', 'AA', 1500000000, 'US'),
('CP007', 'Nomura Securities', 'BROKER', 'A', 1000000000, 'JP'),
('CP008', 'UBS Group', 'BANK', 'AA', 3500000000, 'EU'),
('CP009', 'Morgan Stanley', 'BANK', 'AA', 4500000000, 'US'),
('CP010', 'Barclays Bank', 'BANK', 'A', 2500000000, 'UK');

-- Insert custodians test data
INSERT INTO custodians (custodian_id, custodian_name, custodian_bic, custodian_address, jurisdiction) VALUES
('CUST001', 'State Street Bank', 'SSBKUS33XXX', '1 Lincoln Street, Boston, MA 02111', 'US'),
('CUST002', 'BNY Mellon', 'IRVTUS3NXXX', '240 Greenwich Street, New York, NY 10286', 'US'),
('CUST003', 'Euroclear Bank', 'MGTCBEBEECL', '1 Boulevard du Roi Albert II, Brussels', 'EU'),
('CUST004', 'Clearstream Banking', 'DAKVDEFFXXX', 'Mergenthalerallee 61, Frankfurt', 'EU'),
('CUST005', 'HSBC Custody', 'HBUKGB4BXXX', '8 Canada Square, London E14 5HQ', 'UK'),
('CUST006', 'Citibank Custody', 'CITIUS33XXX', '388 Greenwich Street, New York, NY 10013', 'US'),
('CUST007', 'Standard Chartered', 'SCBLHKHKXXX', '32/F Standard Chartered Bank Building, Hong Kong', 'HK'),
('CUST008', 'DBS Bank Custody', 'DBSSSGSGXXX', '12 Marina Boulevard, Singapore 018982', 'SG'),
('CUST009', 'Mizuho Bank', 'MHCBJPJTXXX', '1-5-5 Otemachi, Chiyoda-ku, Tokyo', 'JP'),
('CUST010', 'Northern Trust', 'NRTRUS33XXX', '50 South LaSalle Street, Chicago, IL 60603', 'US');

-- Insert markets test data
INSERT INTO markets (market_code, market_name, settlement_cycle, cut_off_time, time_zone, volatility_rating, liquidity_rating, jurisdiction) VALUES
('NYSE', 'New York Stock Exchange', 2, '16:00:00', 'America/New_York', 'MEDIUM', 'HIGH', 'US'),
('NASDAQ', 'NASDAQ Global Market', 2, '16:00:00', 'America/New_York', 'HIGH', 'HIGH', 'US'),
('LSE', 'London Stock Exchange', 2, '16:30:00', 'Europe/London', 'MEDIUM', 'HIGH', 'UK'),
('XETRA', 'Deutsche Börse XETRA', 2, '17:30:00', 'Europe/Berlin', 'MEDIUM', 'HIGH', 'EU'),
('TSE', 'Tokyo Stock Exchange', 2, '15:00:00', 'Asia/Tokyo', 'MEDIUM', 'HIGH', 'JP'),
('HKEX', 'Hong Kong Exchange', 2, '16:00:00', 'Asia/Hong_Kong', 'HIGH', 'HIGH', 'HK'),
('SGX', 'Singapore Exchange', 2, '17:00:00', 'Asia/Singapore', 'MEDIUM', 'MEDIUM', 'SG'),
('EURONEXT', 'Euronext Paris', 2, '17:30:00', 'Europe/Paris', 'MEDIUM', 'HIGH', 'EU'),
('SIX', 'SIX Swiss Exchange', 3, '17:00:00', 'Europe/Zurich', 'LOW', 'MEDIUM', 'EU'),
('ASX', 'Australian Securities Exchange', 2, '16:00:00', 'Australia/Sydney', 'MEDIUM', 'MEDIUM', 'US');

-- Insert instruments test data
INSERT INTO instruments (instrument_type, instrument_name, instrument_class, settlement_currency) VALUES
('EQUITY_US', 'US Common Stock', 'EQUITY', 'USD'),
('EQUITY_EU', 'European Equity', 'EQUITY', 'EUR'),
('EQUITY_UK', 'UK Ordinary Shares', 'EQUITY', 'GBP'),
('EQUITY_JP', 'Japanese Equity', 'EQUITY', 'JPY'),
('BOND_GOVT', 'Government Bond', 'BOND', 'USD'),
('BOND_CORP', 'Corporate Bond', 'BOND', 'USD'),
('BOND_MUNI', 'Municipal Bond', 'BOND', 'USD'),
('FX_SPOT', 'FX Spot', 'FX', 'USD'),
('FX_FORWARD', 'FX Forward', 'FX', 'USD'),
('DERIVATIVE', 'Equity Derivative', 'DERIVATIVE', 'USD');

-- Insert settlement_instructions test data
INSERT INTO settlement_instructions (
    instruction_id, counterparty_id, custodian_id, instrument_type, currency, market,
    settlement_method, delivery_instruction, special_instructions, min_amount, max_amount, priority
) VALUES
('SI001', 'CP001', 'CUST001', 'EQUITY_US', 'USD', 'NYSE', 'DVP', 'Standard delivery via DTC', 'None', 1000.00, 10000000.00, 1),
('SI002', 'CP001', 'CUST002', 'EQUITY_US', 'USD', 'NASDAQ', 'DVP', 'Electronic delivery', 'High priority', 5000.00, 50000000.00, 1),
('SI003', 'CP002', 'CUST001', 'EQUITY_US', 'USD', 'NYSE', 'DVP', 'Standard DTC delivery', 'None', 10000.00, 100000000.00, 1),
('SI004', 'CP003', 'CUST003', 'EQUITY_EU', 'EUR', 'XETRA', 'DVP', 'Clearstream settlement', 'T+2 settlement', 1000.00, 25000000.00, 2),
('SI005', 'CP004', 'CUST005', 'EQUITY_UK', 'GBP', 'LSE', 'DVP', 'CREST settlement', 'Standard UK settlement', 500.00, 15000000.00, 1),
('SI006', 'CP005', 'CUST001', 'BOND_GOVT', 'USD', 'NYSE', 'DVP', 'Fed wire settlement', 'Government securities', 100000.00, 500000000.00, 1),
('SI007', 'CP006', 'CUST002', 'BOND_CORP', 'USD', 'NYSE', 'DVP', 'DTC book entry', 'Corporate bonds', 50000.00, 200000000.00, 2),
('SI008', 'CP007', 'CUST009', 'EQUITY_JP', 'JPY', 'TSE', 'DVP', 'JASDEC settlement', 'Japanese equity settlement', 100000.00, 1000000000.00, 1),
('SI009', 'CP008', 'CUST003', 'FX_SPOT', 'USD', 'EURONEXT', 'PVP', 'CLS settlement', 'FX spot settlement', 10000.00, 100000000.00, 1),
('SI010', 'CP009', 'CUST006', 'DERIVATIVE', 'USD', 'NYSE', 'NETTING', 'OCC clearing', 'Options and futures', 1000.00, 50000000.00, 3);

-- Insert risk_assessments test data
INSERT INTO risk_assessments (
    assessment_id, counterparty_id, instrument_type, market, risk_category, risk_score,
    max_exposure, max_single_trade, approval_required, monitoring_level, effective_date, expiry_date
) VALUES
('RA001', 'CP001', 'EQUITY_US', 'NYSE', 'LOW', 15, 1000000000.00, 50000000.00, FALSE, 'STANDARD', '2025-01-01', '2025-12-31'),
('RA002', 'CP001', 'EQUITY_US', 'NASDAQ', 'MEDIUM', 25, 500000000.00, 25000000.00, FALSE, 'STANDARD', '2025-01-01', '2025-12-31'),
('RA003', 'CP002', 'EQUITY_US', 'NYSE', 'LOW', 10, 2000000000.00, 100000000.00, FALSE, 'MINIMAL', '2025-01-01', '2025-12-31'),
('RA004', 'CP003', 'EQUITY_EU', 'XETRA', 'MEDIUM', 30, 750000000.00, 30000000.00, TRUE, 'ENHANCED', '2025-01-01', '2025-12-31'),
('RA005', 'CP004', 'EQUITY_UK', 'LSE', 'LOW', 20, 800000000.00, 40000000.00, FALSE, 'STANDARD', '2025-01-01', '2025-12-31'),
('RA006', 'CP005', 'BOND_GOVT', 'NYSE', 'LOW', 5, 5000000000.00, 500000000.00, FALSE, 'MINIMAL', '2025-01-01', '2025-12-31'),
('RA007', 'CP006', 'BOND_CORP', 'NYSE', 'MEDIUM', 35, 1000000000.00, 100000000.00, TRUE, 'STANDARD', '2025-01-01', '2025-12-31'),
('RA008', 'CP007', 'EQUITY_JP', 'TSE', 'HIGH', 45, 500000000.00, 25000000.00, TRUE, 'ENHANCED', '2024-01-01', '2024-12-31'),
('RA009', 'CP008', 'FX_SPOT', 'EURONEXT', 'MEDIUM', 25, 2000000000.00, 100000000.00, FALSE, 'STANDARD', '2024-01-01', '2024-12-31'),
('RA010', 'CP009', 'DERIVATIVE', 'NYSE', 'HIGH', 55, 300000000.00, 15000000.00, TRUE, 'CRITICAL', '2024-01-01', '2024-12-31');

-- Create indexes for performance
CREATE INDEX idx_settlement_instructions_lookup ON settlement_instructions(counterparty_id, instrument_type, currency, market);
CREATE INDEX idx_risk_assessments_lookup ON risk_assessments(counterparty_id, instrument_type, market);
CREATE INDEX idx_settlement_instructions_amounts ON settlement_instructions(min_amount, max_amount);
CREATE INDEX idx_risk_assessments_trade_amount ON risk_assessments(max_single_trade);

-- Analyze tables for query optimization
ANALYZE counterparties;
ANALYZE custodians;
ANALYZE markets;
ANALYZE instruments;
ANALYZE settlement_instructions;
ANALYZE risk_assessments;

-- Verify data insertion
SELECT
    'Customer data verification:' as info,
    COUNT(*) as total_customers,
    COUNT(CASE WHEN customer_type = 'CORPORATE' THEN 1 END) as corporate_count,
    COUNT(CASE WHEN customer_type = 'INSTITUTIONAL' THEN 1 END) as institutional_count,
    COUNT(CASE WHEN customer_type = 'RETAIL' THEN 1 END) as retail_count,
    COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_count
FROM customers;

-- Verify trading data insertion
SELECT
    'Trading data verification:' as info,
    (SELECT COUNT(*) FROM counterparties) as counterparties_count,
    (SELECT COUNT(*) FROM custodians) as custodians_count,
    (SELECT COUNT(*) FROM markets) as markets_count,
    (SELECT COUNT(*) FROM instruments) as instruments_count,
    (SELECT COUNT(*) FROM settlement_instructions) as settlement_instructions_count,
    (SELECT COUNT(*) FROM risk_assessments) as risk_assessments_count;
