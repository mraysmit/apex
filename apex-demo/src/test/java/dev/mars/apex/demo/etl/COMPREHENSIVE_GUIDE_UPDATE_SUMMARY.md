# APEX ETL Comprehensive Guide - OTC Options Update Summary

## Overview

Successfully updated the **APEX_ETL_COMPREHENSIVE_GUIDE.md** (1,683 lines) to replace all e-commerce examples (products, orders, customers) with **OTC (Over-The-Counter) options trade processing** examples, aligning with middle office trade processing use cases.

**Date**: 2025-10-27  
**Status**: ✅ **COMPLETE**  
**File**: `apex-demo/src/test/java/dev/mars/apex/demo/etl/APEX_ETL_COMPREHENSIVE_GUIDE.md`

## Sections Updated

### 1. Data Source Types - JSON Format (Lines 129-175)

**Before**: Product catalog examples
```yaml
operations:
  getAllProducts: "$[*]"
  getActiveProducts: "$[?(@.active == true)]"
  getElectronics: "$[?(@.category == 'Electronics')]"
```

**After**: OTC options trade examples
```yaml
operations:
  getAllTrades: "$[*]"
  getActiveTrades: "$[?(@.status == 'ACTIVE')]"
  getCallOptions: "$[?(@.optionType == 'Call')]"
  getHighValueTrades: "$[?(@.notionalAmount > 50000000)]"
```

**JSONPath Examples Updated**:
- `$[*]` - All products → All trades
- `$[?(@.price > 500)]` - Products over $500 → `$[?(@.notionalAmount > 50000000)]` - Trades over $50M
- `$[*].specifications` → `$[*].underlyingAsset`
- `$[*].tags[*]` - All tags from all products → All tags from all trades

### 2. Data Source Types - XML Format (Lines 182-243)

**Before**: Order processing examples
```xml
<orders>
    <order id="ORD-001" status="completed">
        <customer>...</customer>
        <items>...</items>
        <shipping>...</shipping>
    </order>
</orders>
```

**After**: OTC trade confirmation examples
```xml
<trades>
    <trade id="OTC-2025-001" status="CONFIRMED">
        <counterparties>
            <buyer>
                <lei>W22LROWP2IHZNBB6K528</lei>
            </buyer>
        </counterparties>
        <optionDetails>
            <underlyingAsset>...</underlyingAsset>
        </optionDetails>
        <riskMetrics>
            <delta>0.65</delta>
        </riskMetrics>
    </trade>
</trades>
```

**XML Operations Updated**:
- `getAllOrders` → `getAllTrades`
- `getCompletedOrders` → `getConfirmedTrades`
- `getPendingOrders` → `getPendingTrades`
- `record-element: "order"` → `record-element: "trade"`

### 3. Data Source Types - Database Queries (Lines 255-345)

**Before**: Customer and order analytics
```sql
SELECT
  c.id as customer_id,
  c.name as customer_name,
  COUNT(o.id) as total_orders,
  SUM(o.total_amount) as total_spent
FROM customers c
LEFT JOIN orders o ON c.id = o.customer_id
```

**After**: Counterparty exposure and trade analytics
```sql
SELECT
  cp.id as counterparty_id,
  cp.legal_name,
  cp.lei,
  COUNT(t.id) as total_trades,
  SUM(t.notional_amount) as total_notional
FROM counterparties cp
LEFT JOIN otc_trades t ON (cp.party_id = t.buyer_party_id OR cp.party_id = t.seller_party_id)
```

**Query Names Updated**:
- `getCustomerOrderSummary` → `getCounterpartyExposure`
- `getOrderDetails` → `getTradeDetails`
- `getTopCustomers` → `getTopCounterparties`

### 4. Practical Examples - JSON Data Extraction (Lines 494-659)

**Complete Section Rewrite**:

**Use Case Changed**:
- **Before**: "Extract product catalog data from a JSON file containing products with specifications and tags"
- **After**: "Extract OTC (Over-The-Counter) options trade data from a JSON file for middle office trade processing, including trade details, counterparties, option specifications, and underlying asset information"

**YAML Configuration Updated**:
- Pipeline name: `json-extract-pipeline` → `json-otc-extract-pipeline`
- Data source: `json-product-source` → `json-otc-options-source`
- Step name: `extract-products` → `extract-otc-options`
- File pattern: `products.json` → `otc-options.json`

**Sample Data Updated**:
```json
// Before: Product with specifications
{
  "id": "PROD-001",
  "name": "Gaming Laptop",
  "price": 1299.99,
  "specifications": { "cpu": "Intel i7", "ram": "16GB" }
}

// After: OTC option trade with underlying asset
{
  "tradeId": "OTC-2025-001",
  "buyerParty": "GOLDMAN_SACHS",
  "optionType": "Call",
  "notionalAmount": 75000000,
  "underlyingAsset": {
    "commodity": "Natural Gas",
    "unit": "MMBtu",
    "ticker": "NG"
  }
}
```

**Java Code Updated**:
- Variable names: `products` → `trades`, `firstProduct` → `firstTrade`
- Field access: `product.get("id")` → `trade.get("tradeId")`
- Nested objects: `specifications` → `underlyingAsset`
- Nested fields: `cpu/ram` → `commodity/unit/ticker`

### 5. Practical Examples - XML Data Extraction (Lines 669-846)

**Complete Section Rewrite**:

**Use Case Changed**:
- **Before**: "Extract order information from an XML file containing orders with customer details, line items, and shipping information"
- **After**: "Extract OTC trade confirmation data from an XML file (FpML-style) containing trade details, counterparty information, option specifications, and risk metrics for middle office confirmation matching and settlement processing"

**YAML Configuration Updated**:
- Pipeline name: `xml-extract-pipeline` → `xml-otc-extract-pipeline`
- Data source: `xml-order-source` → `xml-otc-trades-source`
- Step name: `extract-orders` → `extract-otc-trades`
- File pattern: `orders.xml` → `otc-trades.xml`
- Record element: `order` → `trade`

**Sample Data Updated**:
```xml
<!-- Before: Order with customer and shipping -->
<order id="ORD-001" status="completed">
    <customer>...</customer>
    <items>...</items>
    <shipping>...</shipping>
</order>

<!-- After: Trade with counterparties and risk metrics -->
<trade id="OTC-2025-001" status="CONFIRMED">
    <counterparties>
        <buyer>
            <lei>W22LROWP2IHZNBB6K528</lei>
        </buyer>
    </counterparties>
    <optionDetails>...</optionDetails>
    <riskMetrics>
        <delta>0.65</delta>
        <gamma>0.12</gamma>
    </riskMetrics>
</trade>
```

**Java Code Updated**:
- Variable names: `orders` → `trades`, `firstOrder` → `firstTrade`
- Attribute access: `order.get("@id")` → `trade.get("@id")` (OTC-2025-001)
- Nested objects: `customer` → `counterparties`, `shipping` → `riskMetrics`
- Deeply nested: `shipping.address.city` → `optionDetails.underlyingAsset.commodity`

### 6. Practical Examples - Advanced Database Extraction (Lines 848-1120)

**Complete Section Rewrite**:

**Use Case Changed**:
- **Before**: "Extract customer order analytics from a relational database with multiple tables, calculating summary statistics and aggregations"
- **After**: "Extract OTC trade analytics from a relational database with multiple tables, calculating counterparty exposure, trade volumes, and risk aggregations for middle office reporting and risk management"

**Database Schema Updated**:
```sql
-- Before: E-commerce schema
CREATE TABLE customers (id, name, email, status);
CREATE TABLE orders (id, customer_id, total_amount, status);
CREATE TABLE order_items (id, order_id, product_id, quantity);

-- After: OTC trade schema
CREATE TABLE counterparties (id, party_id, legal_name, lei, status);
CREATE TABLE otc_trades (id, trade_id, buyer_party_id, seller_party_id, notional_amount, option_type);
CREATE TABLE trade_positions (id, trade_id, commodity, ticker, delta, gamma, vega, theta);
```

**YAML Configuration Updated**:
- Pipeline name: `advanced-database-extract-pipeline` → `advanced-otc-database-extract-pipeline`
- Data source: `advanced-db-source` → `advanced-otc-db-source`
- Step name: `extract-customer-order-summary` → `extract-counterparty-exposure`
- Database: `customer_database` → `advanced_otc_test_db`

**SQL Queries Updated**:
- Added CASE statements for buy-side vs sell-side notional calculation
- Added risk aggregation queries (net_delta, total_vega)
- Added commodity exposure analysis
- Replaced customer/order joins with counterparty/trade joins

**Key Features Added**:
- ✅ CASE Statements for conditional aggregation
- ✅ Financial Analytics (counterparty exposure, commodity risk)
- ✅ Risk metrics aggregation (delta, gamma, vega, theta)

### 7. Minor Updates Throughout Document

**Data Sink Examples** (Line 431):
- `insertCustomer` → `insertTrade`
- Table: `customers` → `otc_trades`

**Troubleshooting Examples** (Lines 1408-1440):
- `record-element: "order"` → `record-element: "trade"`
- `order.get("@id")` → `trade.get("@id")`
- `getCustomers` → `getAllTrades`

**Example Configurations** (Lines 1556-1613):
- `products.json` → `otc-options.json`
- CSV to database pipeline description updated to mention OTC trade data

**Summary Table** (Lines 1626-1633):
- Customer data import → Trade data import
- Product catalog → OTC options trades
- Order processing → Trade confirmations (FpML-style)
- Analytics queries → Counterparty exposure analytics

## Domain Model Changes

### Key Entities

**Before (E-commerce)**:
- Products (id, name, price, category, specifications)
- Orders (id, customer_id, order_date, total_amount)
- Customers (id, name, email, status)
- Order Items (product_id, quantity, unit_price)

**After (OTC Options)**:
- Trades (trade_id, buyer_party_id, seller_party_id, option_type, notional_amount)
- Counterparties (party_id, legal_name, lei, status)
- Trade Positions (commodity, ticker, quantity, delta, gamma, vega, theta)
- Underlying Assets (commodity, unit, ticker)

### Financial Terminology Added

- **LEI (Legal Entity Identifier)**: 20-character codes for regulatory compliance
- **Option Types**: Call/Put
- **Settlement Types**: Cash/Physical
- **Risk Metrics**: Delta, Gamma, Vega, Theta (Greeks)
- **Notional Amount**: Total trade value
- **Strike Price**: Exercise price
- **Premium**: Option premium paid
- **Commodities**: Natural Gas, Brent Crude Oil, Gold, Silver, Copper, Wheat
- **Tickers**: NG, BZ, GC, SI, HG, ZW

## Benefits of OTC Options Data

1. **Domain Relevance**: Aligns with financial services and middle office processing
2. **Real-World Complexity**: Demonstrates handling of complex financial instruments
3. **Regulatory Context**: Includes LEI codes and compliance-relevant data
4. **Risk Management**: Shows integration with risk metrics (Greeks)
5. **Industry Standard**: Reflects actual OTC derivatives market practices
6. **Professional Examples**: More appropriate for enterprise documentation
7. **Educational Value**: Teaches financial domain concepts alongside ETL patterns

## Validation

✅ All test files updated and passing (6/6 tests)  
✅ All YAML configurations updated  
✅ All Java code examples updated  
✅ All SQL queries updated  
✅ All sample data updated  
✅ All documentation consistent with OTC options domain  
✅ No references to products, orders, or customers in practical examples  
✅ Financial terminology correctly applied throughout

---

**Update Status**: ✅ **COMPLETE**  
**Documentation Quality**: ✅ **PRODUCTION-READY**  
**Domain Alignment**: ✅ **MIDDLE OFFICE TRADE PROCESSING**

