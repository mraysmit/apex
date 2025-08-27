-- APEX Demo Database Initialization Script
-- Creates schemas and tables for PostgreSQL lookup demonstrations
-- Author: APEX Demo Team
-- Version: 1.0

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create schemas
CREATE SCHEMA IF NOT EXISTS customer_data;
CREATE SCHEMA IF NOT EXISTS trading_data;
CREATE SCHEMA IF NOT EXISTS reference_data;

-- Set search path
SET search_path TO customer_data, trading_data, reference_data, public;

-- Grant permissions
GRANT USAGE ON SCHEMA customer_data TO apex_user;
GRANT USAGE ON SCHEMA trading_data TO apex_user;
GRANT USAGE ON SCHEMA reference_data TO apex_user;

GRANT CREATE ON SCHEMA customer_data TO apex_user;
GRANT CREATE ON SCHEMA trading_data TO apex_user;
GRANT CREATE ON SCHEMA reference_data TO apex_user;

-- =====================================================
-- CUSTOMER DATA SCHEMA
-- =====================================================

-- Customers table for simple lookup demo
CREATE TABLE customer_data.customers (
    customer_id VARCHAR(20) PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    customer_type VARCHAR(50) NOT NULL CHECK (customer_type IN ('INDIVIDUAL', 'CORPORATE', 'INSTITUTIONAL')),
    tier VARCHAR(20) NOT NULL CHECK (tier IN ('BASIC', 'SILVER', 'GOLD', 'PLATINUM')),
    region VARCHAR(10) NOT NULL CHECK (region IN ('NA', 'EU', 'APAC', 'LATAM', 'ME', 'AFRICA')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING')) DEFAULT 'ACTIVE',
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Customer profiles with additional details
CREATE TABLE customer_data.customer_profiles (
    customer_id VARCHAR(20) PRIMARY KEY REFERENCES customer_data.customers(customer_id),
    email VARCHAR(255),
    phone VARCHAR(50),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(2),
    date_of_birth DATE,
    kyc_status VARCHAR(20) CHECK (kyc_status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED')),
    risk_rating VARCHAR(10) CHECK (risk_rating IN ('LOW', 'MEDIUM', 'HIGH')),
    credit_score INTEGER CHECK (credit_score BETWEEN 300 AND 850),
    annual_income DECIMAL(15,2),
    net_worth DECIMAL(15,2),
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- TRADING DATA SCHEMA
-- =====================================================

-- Counterparties table
CREATE TABLE trading_data.counterparties (
    counterparty_id VARCHAR(20) PRIMARY KEY,
    counterparty_name VARCHAR(255) NOT NULL,
    counterparty_type VARCHAR(50) NOT NULL CHECK (counterparty_type IN ('BANK', 'BROKER', 'CUSTODIAN', 'EXCHANGE', 'CLEARING_HOUSE')),
    credit_rating VARCHAR(10),
    credit_limit DECIMAL(15,2),
    jurisdiction VARCHAR(2) NOT NULL,
    bic_code VARCHAR(11),
    lei_code VARCHAR(20),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')) DEFAULT 'ACTIVE',
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Custodians table
CREATE TABLE trading_data.custodians (
    custodian_id VARCHAR(20) PRIMARY KEY,
    custodian_name VARCHAR(255) NOT NULL,
    custodian_bic VARCHAR(11) NOT NULL,
    custodian_address TEXT,
    jurisdiction VARCHAR(2) NOT NULL,
    services TEXT[], -- Array of services offered
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')) DEFAULT 'ACTIVE',
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Markets table
CREATE TABLE trading_data.markets (
    market_code VARCHAR(10) PRIMARY KEY,
    market_name VARCHAR(255) NOT NULL,
    country VARCHAR(2) NOT NULL,
    time_zone VARCHAR(50) NOT NULL,
    settlement_cycle VARCHAR(10) NOT NULL, -- T+0, T+1, T+2, etc.
    cut_off_time TIME,
    volatility_rating VARCHAR(10) CHECK (volatility_rating IN ('LOW', 'MEDIUM', 'HIGH')),
    liquidity_rating VARCHAR(10) CHECK (liquidity_rating IN ('LOW', 'MEDIUM', 'HIGH')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')) DEFAULT 'ACTIVE',
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Instruments table
CREATE TABLE trading_data.instruments (
    instrument_type VARCHAR(50) PRIMARY KEY,
    instrument_name VARCHAR(255) NOT NULL,
    instrument_class VARCHAR(50) NOT NULL CHECK (instrument_class IN ('EQUITY', 'FIXED_INCOME', 'DERIVATIVE', 'COMMODITY', 'FX')),
    settlement_currency VARCHAR(3),
    typical_settlement_days INTEGER DEFAULT 2,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Settlement instructions table (main table for multi-parameter lookup)
CREATE TABLE trading_data.settlement_instructions (
    instruction_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    counterparty_id VARCHAR(20) NOT NULL REFERENCES trading_data.counterparties(counterparty_id),
    custodian_id VARCHAR(20) REFERENCES trading_data.custodians(custodian_id),
    instrument_type VARCHAR(50) NOT NULL REFERENCES trading_data.instruments(instrument_type),
    currency VARCHAR(3) NOT NULL,
    market VARCHAR(10) NOT NULL REFERENCES trading_data.markets(market_code),
    settlement_method VARCHAR(20) NOT NULL CHECK (settlement_method IN ('DVP', 'DVP_PREMIUM', 'FOP', 'RVP')),
    delivery_instruction VARCHAR(50) NOT NULL CHECK (delivery_instruction IN ('DELIVER', 'RECEIVE', 'DELIVER_FREE', 'RECEIVE_FREE')),
    special_instructions TEXT,
    min_amount DECIMAL(15,2),
    max_amount DECIMAL(15,2),
    priority INTEGER DEFAULT 1,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING')) DEFAULT 'ACTIVE',
    effective_date DATE DEFAULT CURRENT_DATE,
    expiry_date DATE,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Risk assessments table
CREATE TABLE trading_data.risk_assessments (
    assessment_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    counterparty_id VARCHAR(20) NOT NULL REFERENCES trading_data.counterparties(counterparty_id),
    instrument_type VARCHAR(50) NOT NULL REFERENCES trading_data.instruments(instrument_type),
    market VARCHAR(10) NOT NULL REFERENCES trading_data.markets(market_code),
    risk_category VARCHAR(20) NOT NULL CHECK (risk_category IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    risk_score DECIMAL(5,2) CHECK (risk_score BETWEEN 0 AND 100),
    max_exposure DECIMAL(15,2),
    max_single_trade DECIMAL(15,2),
    approval_required BOOLEAN DEFAULT FALSE,
    monitoring_level VARCHAR(20) CHECK (monitoring_level IN ('STANDARD', 'ENHANCED', 'INTENSIVE')),
    effective_date DATE DEFAULT CURRENT_DATE,
    expiry_date DATE,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- REFERENCE DATA SCHEMA
-- =====================================================

-- Currencies table
CREATE TABLE reference_data.currencies (
    currency_code VARCHAR(3) PRIMARY KEY,
    currency_name VARCHAR(100) NOT NULL,
    currency_symbol VARCHAR(10),
    decimal_places INTEGER DEFAULT 2,
    country_code VARCHAR(2),
    is_base_currency BOOLEAN DEFAULT FALSE,
    region VARCHAR(50),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')) DEFAULT 'ACTIVE',
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Countries table
CREATE TABLE reference_data.countries (
    country_code VARCHAR(2) PRIMARY KEY,
    country_name VARCHAR(255) NOT NULL,
    region VARCHAR(50),
    time_zone VARCHAR(50),
    regulatory_zone VARCHAR(20),
    settlement_system VARCHAR(50),
    standard_settlement_days INTEGER DEFAULT 2,
    holiday_calendar VARCHAR(20),
    settlement_fee DECIMAL(10,2),
    custodian_bank VARCHAR(255),
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_customers_type_tier ON customer_data.customers(customer_type, tier);
CREATE INDEX idx_customers_region_status ON customer_data.customers(region, status);
CREATE INDEX idx_customer_profiles_risk_rating ON customer_data.customer_profiles(risk_rating);
CREATE INDEX idx_customer_profiles_kyc_status ON customer_data.customer_profiles(kyc_status);

CREATE INDEX idx_settlement_instructions_lookup ON trading_data.settlement_instructions(counterparty_id, instrument_type, currency, market, status);
CREATE INDEX idx_settlement_instructions_amount ON trading_data.settlement_instructions(min_amount, max_amount);
CREATE INDEX idx_settlement_instructions_priority ON trading_data.settlement_instructions(priority DESC, created_date DESC);

CREATE INDEX idx_risk_assessments_lookup ON trading_data.risk_assessments(counterparty_id, instrument_type, market);
CREATE INDEX idx_risk_assessments_dates ON trading_data.risk_assessments(effective_date, expiry_date);

CREATE INDEX idx_counterparties_type_status ON trading_data.counterparties(counterparty_type, status);
CREATE INDEX idx_markets_country_status ON trading_data.markets(country, status);

-- Grant table permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA customer_data TO apex_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA trading_data TO apex_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA reference_data TO apex_user;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA customer_data TO apex_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA trading_data TO apex_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA reference_data TO apex_user;

-- Create update triggers for last_updated timestamps
CREATE OR REPLACE FUNCTION update_last_updated_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_customers_last_updated BEFORE UPDATE ON customer_data.customers FOR EACH ROW EXECUTE FUNCTION update_last_updated_column();
CREATE TRIGGER update_customer_profiles_last_updated BEFORE UPDATE ON customer_data.customer_profiles FOR EACH ROW EXECUTE FUNCTION update_last_updated_column();
CREATE TRIGGER update_counterparties_last_updated BEFORE UPDATE ON trading_data.counterparties FOR EACH ROW EXECUTE FUNCTION update_last_updated_column();
CREATE TRIGGER update_settlement_instructions_last_updated BEFORE UPDATE ON trading_data.settlement_instructions FOR EACH ROW EXECUTE FUNCTION update_last_updated_column();
CREATE TRIGGER update_risk_assessments_last_updated BEFORE UPDATE ON trading_data.risk_assessments FOR EACH ROW EXECUTE FUNCTION update_last_updated_column();
