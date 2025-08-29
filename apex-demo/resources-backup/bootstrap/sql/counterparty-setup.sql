-- ============================================================================
-- APEX Rules Engine - OTC Options Bootstrap Demo
-- PostgreSQL Counterparty Reference Table Setup
-- ============================================================================
--
-- This SQL script creates and populates the counterparty reference table
-- used by the OTC Options Bootstrap Demo to demonstrate PostgreSQL database
-- lookup enrichment capabilities.
--
-- OVERVIEW:
-- The counterparty_reference table contains detailed information about
-- financial institutions that participate in OTC derivatives trading.
-- This data is used to enrich OTC Options with counterparty details
-- including legal names, credit ratings, LEI codes, and jurisdictions.
--
-- TABLE STRUCTURE:
-- - party_id: Primary key, short identifier used in trading systems
-- - legal_name: Full legal name of the financial institution
-- - credit_rating: Credit rating from major rating agencies
-- - lei: Legal Entity Identifier (ISO 17442 standard)
-- - jurisdiction: Primary regulatory jurisdiction
-- - settlement_preference: Preferred settlement method (DVP/FOP)
-- - created_date: Timestamp of record creation
--
-- USAGE:
-- This script is executed automatically by the DatabaseSetup component
-- during the bootstrap demo infrastructure setup phase.
--
-- ============================================================================

-- Drop existing table if it exists (for clean setup)
DROP TABLE IF EXISTS counterparty_reference CASCADE;

-- Create the counterparty reference table
CREATE TABLE counterparty_reference (
    party_id VARCHAR(50) PRIMARY KEY,
    legal_name VARCHAR(200) NOT NULL,
    credit_rating VARCHAR(10),
    lei VARCHAR(20),
    jurisdiction VARCHAR(50),
    settlement_preference VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Add constraints for data quality
    CONSTRAINT chk_credit_rating CHECK (credit_rating IN ('AAA', 'AA+', 'AA', 'AA-', 'A+', 'A', 'A-', 'BBB+', 'BBB', 'BBB-', 'BB+', 'BB', 'BB-', 'B+', 'B', 'B-', 'CCC', 'CC', 'C', 'D', 'NR')),
    CONSTRAINT chk_settlement_preference CHECK (settlement_preference IN ('DVP', 'FOP', 'PVP'))
);

-- Create indexes for performance
CREATE INDEX idx_counterparty_jurisdiction ON counterparty_reference(jurisdiction);
CREATE INDEX idx_counterparty_rating ON counterparty_reference(credit_rating);
CREATE INDEX idx_counterparty_lei ON counterparty_reference(lei);

-- Insert sample counterparty data for major financial institutions
INSERT INTO counterparty_reference 
(party_id, legal_name, credit_rating, lei, jurisdiction, settlement_preference) 
VALUES 
    -- US Investment Banks
    ('GOLDMAN_SACHS', 'Goldman Sachs & Co. LLC', 'AAA', '784F5XWPLTWKTBV3E584', 'United States', 'DVP'),
    ('JP_MORGAN', 'JPMorgan Chase Bank, N.A.', 'AAA', '7H6GLXDRUGQFU57RNE97', 'United States', 'DVP'),
    ('MORGAN_STANLEY', 'Morgan Stanley & Co. LLC', 'AA+', 'IGJSJL3JD5P30I6NJZ34', 'United States', 'DVP'),
    ('CITI', 'Citigroup Global Markets Inc.', 'AA-', '6SHGI4ZSSLCXXQSBB395', 'United States', 'FOP'),
    ('BANK_OF_AMERICA', 'Bank of America, N.A.', 'AA-', 'B4TYDEB6GKMZO031MB27', 'United States', 'DVP'),
    
    -- European Banks
    ('BARCLAYS', 'Barclays Bank PLC', 'A+', 'G5GSEF7VJP5I7OUK5573', 'United Kingdom', 'DVP'),
    ('DEUTSCHE_BANK', 'Deutsche Bank AG', 'BBB+', '7LTWFZYICNSX8D621K86', 'Germany', 'FOP'),
    ('BNP_PARIBAS', 'BNP Paribas S.A.', 'A+', 'R0MUWSFPU8MPRO8K5P83', 'France', 'DVP'),
    ('CREDIT_AGRICOLE', 'Crédit Agricole Corporate and Investment Bank', 'A+', '1JKS5WJDQ2QWLGQP2V89', 'France', 'DVP'),
    ('SOCIETE_GENERALE', 'Société Générale', 'A', 'O2RNE8IBXP4R0TD8PU41', 'France', 'FOP'),
    
    -- Swiss Banks
    ('UBS', 'UBS AG', 'A+', 'BFM8T61CT2L1QCEMIK50', 'Switzerland', 'DVP'),
    ('CREDIT_SUISSE', 'Credit Suisse AG', 'BBB-', 'ANGGYXNX0JLX3X63JN86', 'Switzerland', 'FOP'),
    
    -- Asian Banks
    ('NOMURA', 'Nomura Securities Co., Ltd.', 'A-', 'DGQCSV2PHVF7I2743539', 'Japan', 'DVP'),
    ('MIZUHO', 'Mizuho Bank, Ltd.', 'A-', 'MHBK2D1KZQG0N5B8V627', 'Japan', 'DVP'),
    ('HSBC', 'HSBC Bank plc', 'AA-', 'MP6I5ZYZBEU3UXPYFY54', 'United Kingdom', 'DVP'),
    
    -- Canadian Banks
    ('RBC', 'Royal Bank of Canada', 'AA-', 'ES7IP3ZXJD1ZN8GV7K94', 'Canada', 'DVP'),
    ('TD_BANK', 'The Toronto-Dominion Bank', 'AA-', 'PT3QB789TSUIDF1YE703', 'Canada', 'DVP'),
    
    -- Australian Banks
    ('ANZ', 'Australia and New Zealand Banking Group Limited', 'AA-', 'HBMOB5HMQZ28I6NE1R72', 'Australia', 'DVP'),
    ('WESTPAC', 'Westpac Banking Corporation', 'AA-', 'WBPBNPPA2POBZD8N1E08', 'Australia', 'DVP'),
    
    -- Other Major Institutions
    ('ING', 'ING Bank N.V.', 'A+', 'BFXS5XCH7N0Y05NIXW11', 'Netherlands', 'DVP');

-- Add comments to the table and columns for documentation
COMMENT ON TABLE counterparty_reference IS 'Reference data for OTC derivatives counterparties used in bootstrap demo';
COMMENT ON COLUMN counterparty_reference.party_id IS 'Short identifier used in trading systems';
COMMENT ON COLUMN counterparty_reference.legal_name IS 'Full legal name of the financial institution';
COMMENT ON COLUMN counterparty_reference.credit_rating IS 'Credit rating from major rating agencies (S&P/Moody''s/Fitch)';
COMMENT ON COLUMN counterparty_reference.lei IS 'Legal Entity Identifier (ISO 17442 standard)';
COMMENT ON COLUMN counterparty_reference.jurisdiction IS 'Primary regulatory jurisdiction';
COMMENT ON COLUMN counterparty_reference.settlement_preference IS 'Preferred settlement method (DVP=Delivery vs Payment, FOP=Free of Payment, PVP=Payment vs Payment)';

-- Create a view for easy querying
CREATE VIEW v_counterparty_summary AS
SELECT 
    party_id,
    legal_name,
    credit_rating,
    jurisdiction,
    settlement_preference,
    CASE 
        WHEN credit_rating IN ('AAA', 'AA+', 'AA', 'AA-') THEN 'Investment Grade - High'
        WHEN credit_rating IN ('A+', 'A', 'A-') THEN 'Investment Grade - Medium'
        WHEN credit_rating IN ('BBB+', 'BBB', 'BBB-') THEN 'Investment Grade - Low'
        WHEN credit_rating IN ('BB+', 'BB', 'BB-', 'B+', 'B', 'B-') THEN 'Speculative Grade'
        WHEN credit_rating IN ('CCC', 'CC', 'C', 'D') THEN 'Distressed'
        ELSE 'Not Rated'
    END as rating_category
FROM counterparty_reference;

COMMENT ON VIEW v_counterparty_summary IS 'Summary view of counterparty data with rating categories';

-- Grant permissions (adjust as needed for your environment)
-- GRANT SELECT ON counterparty_reference TO apex_demo_user;
-- GRANT SELECT ON v_counterparty_summary TO apex_demo_user;

-- Display setup completion message
DO $$
BEGIN
    RAISE NOTICE 'OTC Options Bootstrap Demo - Counterparty reference table setup completed successfully';
    RAISE NOTICE 'Created table: counterparty_reference with % records', (SELECT COUNT(*) FROM counterparty_reference);
    RAISE NOTICE 'Created view: v_counterparty_summary';
    RAISE NOTICE 'Created indexes: idx_counterparty_jurisdiction, idx_counterparty_rating, idx_counterparty_lei';
END $$;
