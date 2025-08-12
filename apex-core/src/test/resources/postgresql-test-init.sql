-- PostgreSQL Test Database Initialization Script
-- This script sets up the test database for YAML PostgreSQL lookup tests

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "hstore";

-- Create test schema
CREATE SCHEMA IF NOT EXISTS test_schema;

-- Set search path
SET search_path TO public, test_schema;

-- Create users table with PostgreSQL-specific features
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    uuid UUID DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(50) DEFAULT 'active',
    preferences JSONB,
    tags TEXT[],
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_users_preferences_gin ON users USING GIN(preferences);
CREATE INDEX IF NOT EXISTS idx_users_tags_gin ON users USING GIN(tags);

-- Create a function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create products table for additional testing
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    price DECIMAL(10,2),
    attributes JSONB,
    tags TEXT[],
    in_stock BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create orders table for relational testing
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    product_id INTEGER REFERENCES products(id),
    quantity INTEGER DEFAULT 1,
    total_amount DECIMAL(10,2),
    order_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'pending'
);

-- Insert initial test data
INSERT INTO users (name, email, status, preferences, tags) VALUES 
('Alice Johnson', 'alice@example.com', 'active', 
 '{"theme": "dark", "language": "en", "notifications": true, "timezone": "UTC"}', 
 ARRAY['admin', 'developer', 'power-user']),
('Bob Smith', 'bob@example.com', 'active', 
 '{"theme": "light", "language": "en", "notifications": false, "timezone": "EST"}', 
 ARRAY['user', 'customer']),
('Charlie Brown', 'charlie@example.com', 'inactive', 
 '{"theme": "dark", "language": "es", "notifications": true, "timezone": "PST"}', 
 ARRAY['user', 'tester', 'beta']),
('Diana Prince', 'diana@example.com', 'active', 
 '{"theme": "auto", "language": "fr", "notifications": true, "timezone": "CET"}', 
 ARRAY['admin', 'manager']),
('Eve Wilson', 'eve@example.com', 'pending', 
 '{"theme": "light", "language": "de", "notifications": false, "timezone": "GMT"}', 
 ARRAY['user'])
ON CONFLICT (email) DO NOTHING;

-- Insert test products
INSERT INTO products (name, category, price, attributes, tags, in_stock) VALUES 
('Laptop Pro', 'Electronics', 1299.99, 
 '{"brand": "TechCorp", "model": "LP-2024", "specs": {"ram": "16GB", "storage": "512GB SSD"}}', 
 ARRAY['electronics', 'computers', 'premium'], true),
('Wireless Mouse', 'Electronics', 29.99, 
 '{"brand": "TechCorp", "model": "WM-100", "specs": {"dpi": "1600", "battery": "AA"}}', 
 ARRAY['electronics', 'accessories'], true),
('Office Chair', 'Furniture', 199.99, 
 '{"brand": "ComfortCorp", "model": "OC-500", "specs": {"material": "leather", "adjustable": true}}', 
 ARRAY['furniture', 'office'], false),
('Coffee Mug', 'Kitchen', 12.99, 
 '{"brand": "MugCorp", "model": "CM-001", "specs": {"capacity": "350ml", "material": "ceramic"}}', 
 ARRAY['kitchen', 'drinkware'], true),
('Notebook', 'Stationery', 5.99, 
 '{"brand": "PaperCorp", "model": "NB-A5", "specs": {"pages": "200", "ruled": true}}', 
 ARRAY['stationery', 'office'], true)
ON CONFLICT DO NOTHING;

-- Insert test orders
INSERT INTO orders (user_id, product_id, quantity, total_amount, status) VALUES 
(1, 1, 1, 1299.99, 'completed'),
(1, 2, 2, 59.98, 'completed'),
(2, 3, 1, 199.99, 'pending'),
(2, 4, 3, 38.97, 'shipped'),
(3, 5, 5, 29.95, 'cancelled'),
(4, 1, 1, 1299.99, 'processing'),
(4, 4, 1, 12.99, 'completed')
ON CONFLICT DO NOTHING;

-- Create a view for testing complex queries
CREATE OR REPLACE VIEW user_order_summary AS
SELECT 
    u.id as user_id,
    u.name as user_name,
    u.email,
    u.status as user_status,
    COUNT(o.id) as total_orders,
    COALESCE(SUM(o.total_amount), 0) as total_spent,
    MAX(o.order_date) as last_order_date,
    u.preferences->>'theme' as preferred_theme,
    array_length(u.tags, 1) as tag_count
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.name, u.email, u.status, u.preferences, u.tags;

-- Create a function for testing stored procedure calls
CREATE OR REPLACE FUNCTION get_user_stats(user_email VARCHAR)
RETURNS TABLE(
    user_name VARCHAR,
    order_count BIGINT,
    total_spent NUMERIC,
    avg_order_value NUMERIC,
    favorite_category VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        u.name::VARCHAR,
        COUNT(o.id),
        COALESCE(SUM(o.total_amount), 0),
        CASE 
            WHEN COUNT(o.id) > 0 THEN COALESCE(SUM(o.total_amount), 0) / COUNT(o.id)
            ELSE 0
        END,
        (SELECT p.category 
         FROM orders o2 
         JOIN products p ON o2.product_id = p.id 
         WHERE o2.user_id = u.id 
         GROUP BY p.category 
         ORDER BY COUNT(*) DESC 
         LIMIT 1)::VARCHAR
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id
    WHERE u.email = user_email
    GROUP BY u.id, u.name;
END;
$$ LANGUAGE plpgsql;

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO testuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO testuser;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO testuser;

-- Create additional test data for performance testing
DO $$
DECLARE
    i INTEGER;
BEGIN
    FOR i IN 1..100 LOOP
        INSERT INTO users (name, email, status, preferences, tags) VALUES 
        ('Test User ' || i, 'testuser' || i || '@example.com', 
         CASE WHEN i % 3 = 0 THEN 'inactive' ELSE 'active' END,
         '{"theme": "' || (CASE WHEN i % 2 = 0 THEN 'dark' ELSE 'light' END) || '", "test_id": ' || i || '}',
         ARRAY['test', 'batch-' || (i % 10)])
        ON CONFLICT (email) DO NOTHING;
    END LOOP;
END $$;

-- Analyze tables for better query planning
ANALYZE users;
ANALYZE products;
ANALYZE orders;

-- Display initialization summary
SELECT 
    'users' as table_name, 
    COUNT(*) as record_count 
FROM users
UNION ALL
SELECT 
    'products' as table_name, 
    COUNT(*) as record_count 
FROM products
UNION ALL
SELECT 
    'orders' as table_name, 
    COUNT(*) as record_count 
FROM orders
ORDER BY table_name;
