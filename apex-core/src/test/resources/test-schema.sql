-- Test schema for PostgreSQL integration tests
-- This schema supports all test scenarios from the External Data Sources Guide

-- Users table for basic CRUD operations
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Products table for complex data scenarios
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    in_stock BOOLEAN NOT NULL DEFAULT true,
    min_quantity INTEGER NOT NULL DEFAULT 1,
    max_quantity INTEGER NOT NULL DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Orders table for transaction scenarios
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    product_id INTEGER REFERENCES products(id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Countries table for reference data scenarios
CREATE TABLE countries (
    id SERIAL PRIMARY KEY,
    code VARCHAR(2) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    region VARCHAR(100) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    timezone VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true
);

-- Currencies table for enrichment scenarios
CREATE TABLE currencies (
    id SERIAL PRIMARY KEY,
    code VARCHAR(3) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    decimal_places INTEGER NOT NULL DEFAULT 2,
    active BOOLEAN NOT NULL DEFAULT true,
    major_currency BOOLEAN NOT NULL DEFAULT false,
    region VARCHAR(100) NOT NULL
);

-- Customer categories for validation scenarios
CREATE TABLE customer_categories (
    id SERIAL PRIMARY KEY,
    category VARCHAR(50) UNIQUE NOT NULL,
    min_balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    discount_rate DECIMAL(5,4) NOT NULL DEFAULT 0,
    approval_required_above DECIMAL(15,2) NOT NULL DEFAULT 10000,
    benefits TEXT[]
);

-- Audit log table for monitoring scenarios
CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    record_id INTEGER NOT NULL,
    old_values JSONB,
    new_values JSONB,
    user_id INTEGER,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Performance test table for large dataset scenarios
CREATE TABLE performance_test (
    id SERIAL PRIMARY KEY,
    test_data VARCHAR(1000),
    numeric_value DECIMAL(10,2),
    date_value TIMESTAMP,
    boolean_value BOOLEAN,
    json_data JSONB
);

-- Insert sample reference data
INSERT INTO countries (code, name, region, currency, timezone, active) VALUES
('US', 'United States', 'North America', 'USD', 'America/New_York', true),
('GB', 'United Kingdom', 'Europe', 'GBP', 'Europe/London', true),
('DE', 'Germany', 'Europe', 'EUR', 'Europe/Berlin', true),
('JP', 'Japan', 'Asia', 'JPY', 'Asia/Tokyo', true),
('CA', 'Canada', 'North America', 'CAD', 'America/Toronto', true);

INSERT INTO currencies (code, name, symbol, decimal_places, active, major_currency, region) VALUES
('USD', 'US Dollar', '$', 2, true, true, 'North America'),
('EUR', 'Euro', '€', 2, true, true, 'Europe'),
('GBP', 'British Pound Sterling', '£', 2, true, true, 'Europe'),
('JPY', 'Japanese Yen', '¥', 0, true, true, 'Asia'),
('CAD', 'Canadian Dollar', 'C$', 2, true, false, 'North America'),
('CHF', 'Swiss Franc', 'CHF', 2, true, false, 'Europe');

INSERT INTO customer_categories (category, min_balance, discount_rate, approval_required_above, benefits) VALUES
('PREMIUM', 100000, 0.15, 50000, ARRAY['Priority Support', 'Fee Waivers', 'Investment Advice']),
('STANDARD', 10000, 0.05, 10000, ARRAY['Online Support', 'Basic Reports']),
('BASIC', 0, 0.00, 5000, ARRAY['Online Support']);

INSERT INTO products (code, name, category, price, currency, in_stock, min_quantity, max_quantity) VALUES
('LAPTOP001', 'Business Laptop', 'Electronics', 1299.99, 'USD', true, 1, 10),
('CHAIR001', 'Office Chair', 'Furniture', 399.50, 'USD', true, 1, 5),
('SOFTWARE001', 'Productivity Suite', 'Software', 99.99, 'USD', true, 1, 100),
('SERVER001', 'Enterprise Server', 'Hardware', 15999.99, 'USD', true, 1, 2),
('TABLET001', 'Business Tablet', 'Electronics', 599.99, 'USD', false, 1, 20);

-- Create indexes for performance testing
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_code ON products(code);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_date ON orders(order_date);
CREATE INDEX idx_countries_code ON countries(code);
CREATE INDEX idx_currencies_code ON currencies(code);
CREATE INDEX idx_audit_log_table_operation ON audit_log(table_name, operation);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);

-- Create a function for updating timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for automatic timestamp updates
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create a view for complex queries
CREATE VIEW user_order_summary AS
SELECT 
    u.id as user_id,
    u.name as user_name,
    u.email,
    COUNT(o.id) as total_orders,
    COALESCE(SUM(o.total_amount), 0) as total_spent,
    MAX(o.order_date) as last_order_date
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.name, u.email;

-- Insert some performance test data
INSERT INTO performance_test (test_data, numeric_value, date_value, boolean_value, json_data)
SELECT 
    'Test data row ' || generate_series,
    random() * 1000,
    CURRENT_TIMESTAMP - (random() * interval '365 days'),
    random() > 0.5,
    jsonb_build_object('id', generate_series, 'random', random(), 'timestamp', CURRENT_TIMESTAMP)
FROM generate_series(1, 1000);
