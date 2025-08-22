package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.exception.EnrichmentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for lookup key extraction functionality.
 * Tests all documented lookup-key patterns from lookups.md.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Lookup Key Extraction Tests")
class LookupKeyExtractionTest {

    private ExpressionParser parser;
    private StandardEvaluationContext context;

    @BeforeEach
    void setUp() {
        parser = new SpelExpressionParser();
        context = new StandardEvaluationContext();
    }

    @Test
    @DisplayName("Should extract simple field reference")
    void shouldExtractSimpleFieldReference() {
        // Given
        Map<String, Object> data = Map.of("customerId", "CUST123");
        context.setRootObject(data);
        
        String lookupKeyExpression = "#customerId";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("CUST123");
    }

    @Test
    @DisplayName("Should extract nested field reference")
    void shouldExtractNestedFieldReference() {
        // Given
        Map<String, Object> customer = Map.of("id", "CUST456");
        Map<String, Object> data = Map.of("customer", customer);
        context.setRootObject(data);
        
        String lookupKeyExpression = "#customer.id";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("CUST456");
    }

    @Test
    @DisplayName("Should extract complex nested field reference")
    void shouldExtractComplexNestedFieldReference() {
        // Given
        Map<String, Object> counterparty = Map.of("partyId", "PARTY789");
        Map<String, Object> transaction = Map.of("counterparty", counterparty);
        Map<String, Object> data = Map.of("transaction", transaction);
        context.setRootObject(data);
        
        String lookupKeyExpression = "#transaction.counterparty.partyId";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("PARTY789");
    }

    @Test
    @DisplayName("Should handle conditional expression")
    void shouldHandleConditionalExpression() {
        // Given - Customer type
        Map<String, Object> data = Map.of(
            "type", "CUSTOMER",
            "customerId", "CUST123",
            "vendorId", "VEND456"
        );
        context.setRootObject(data);
        
        String lookupKeyExpression = "#type == 'CUSTOMER' ? #customerId : #vendorId";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("CUST123");
        
        // Given - Vendor type
        data = Map.of(
            "type", "VENDOR",
            "customerId", "CUST123",
            "vendorId", "VEND456"
        );
        context.setRootObject(data);
        
        // When
        result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("VEND456");
    }

    @Test
    @DisplayName("Should handle string manipulation")
    void shouldHandleStringManipulation() {
        // Given
        Map<String, Object> data = Map.of("accountNumber", "USD123456789");
        context.setRootObject(data);
        
        String lookupKeyExpression = "#accountNumber.substring(0, 3)";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should create compound key with string concatenation")
    void shouldCreateCompoundKeyWithStringConcatenation() {
        // Given
        Map<String, Object> data = Map.of(
            "customerId", "CUST123",
            "region", "US"
        );
        context.setRootObject(data);
        
        String lookupKeyExpression = "#customerId + '-' + #region";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("CUST123-US");
    }

    @Test
    @DisplayName("Should create trading pair compound key")
    void shouldCreateTradingPairCompoundKey() {
        // Given
        Map<String, Object> data = Map.of(
            "baseCurrency", "eur",
            "quoteCurrency", "usd"
        );
        context.setRootObject(data);
        
        String lookupKeyExpression = "#baseCurrency.toUpperCase() + '/' + #quoteCurrency.toUpperCase()";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("EUR/USD");
    }

    @Test
    @DisplayName("Should create conditional compound key")
    void shouldCreateConditionalCompoundKey() {
        // Given - Customer party
        Map<String, Object> data = Map.of(
            "partyType", "CUSTOMER",
            "partyId", "12345"
        );
        context.setRootObject(data);
        
        String lookupKeyExpression = "#partyType == 'CUSTOMER' ? 'CUST-' + #partyId : (#partyType == 'VENDOR' ? 'VEND-' + #partyId : 'UNKN-' + #partyId)";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("CUST-12345");
        
        // Given - Vendor party
        data = Map.of(
            "partyType", "VENDOR",
            "partyId", "67890"
        );
        context.setRootObject(data);
        
        // When
        result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("VEND-67890");
        
        // Given - Unknown party
        data = Map.of(
            "partyType", "OTHER",
            "partyId", "99999"
        );
        context.setRootObject(data);
        
        // When
        result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("UNKN-99999");
    }

    @Test
    @DisplayName("Should create hierarchical compound key")
    void shouldCreateHierarchicalCompoundKey() {
        // Given
        Map<String, Object> instrument = Map.of("symbol", "AAPL");
        Map<String, Object> counterparty = Map.of("id", "GOLDMAN");
        Map<String, Object> trade = Map.of(
            "instrument", instrument,
            "counterparty", counterparty,
            "settlementDate", LocalDate.of(2025, 8, 25)
        );
        Map<String, Object> data = Map.of("trade", trade);
        context.setRootObject(data);
        
        String lookupKeyExpression = "#trade.instrument.symbol + ':' + #trade.counterparty.id + ':' + #trade.settlementDate.toString()";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("AAPL:GOLDMAN:2025-08-25");
    }

    @Test
    @DisplayName("Should create hash-based compound key")
    void shouldCreateHashBasedCompoundKey() {
        // Given
        Map<String, Object> portfolio = Map.of(
            "id", "PORT123",
            "strategy", "EQUITY_LONG",
            "region", "US"
        );
        Map<String, Object> data = Map.of(
            "portfolio", portfolio,
            "asOfDate", LocalDate.of(2025, 8, 22)
        );
        context.setRootObject(data);
        
        String lookupKeyExpression = "T(java.lang.String).valueOf((#portfolio.id + #portfolio.strategy + #portfolio.region + #asOfDate.toString()).hashCode())";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(String.class);
        // Verify it's a consistent hash
        Object result2 = expression.getValue(context);
        assertThat(result).isEqualTo(result2);
    }

    @Test
    @DisplayName("Should create multi-dimensional product lookup key")
    void shouldCreateMultiDimensionalProductLookupKey() {
        // Given
        Map<String, Object> product = Map.of(
            "category", "ELECTRONICS",
            "id", "PROD456"
        );
        Map<String, Object> customer = Map.of(
            "tier", "GOLD",
            "region", "US"
        );
        Map<String, Object> data = Map.of(
            "product", product,
            "customer", customer
        );
        context.setRootObject(data);
        
        String lookupKeyExpression = "#product.category + '|' + #product.id + '|' + #customer.tier + '|' + #customer.region";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("ELECTRONICS|PROD456|GOLD|US");
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() {
        // Given
        Map<String, Object> data = Map.of("customerId", "CUST123");
        // Note: region is missing/null
        context.setRootObject(data);
        
        String lookupKeyExpression = "#customerId + '-' + (#region ?: 'DEFAULT')";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("CUST123-DEFAULT");
    }

    @Test
    @DisplayName("Should handle missing fields with safe navigation")
    void shouldHandleMissingFieldsWithSafeNavigation() {
        // Given
        Map<String, Object> data = Map.of("customerId", "CUST123");
        // Note: customer object is missing
        context.setRootObject(data);
        
        String lookupKeyExpression = "#customer?.id ?: #customerId";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("CUST123");
    }

    @Test
    @DisplayName("Should throw exception for invalid expression")
    void shouldThrowExceptionForInvalidExpression() {
        // Given
        Map<String, Object> data = Map.of("customerId", "CUST123");
        context.setRootObject(data);
        
        String invalidExpression = "#customerId.invalidMethod()";
        Expression expression = parser.parseExpression(invalidExpression);
        
        // When & Then
        assertThatThrownBy(() -> expression.getValue(context))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should handle complex date operations")
    void shouldHandleComplexDateOperations() {
        // Given
        Map<String, Object> data = Map.of(
            "tradeDate", "2025-08-22",
            "settlementDays", 2
        );
        context.setRootObject(data);
        
        String lookupKeyExpression = "T(java.time.LocalDate).parse(#tradeDate).plusDays(#settlementDays).toString()";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("2025-08-24");
    }

    @Test
    @DisplayName("Should handle mathematical operations")
    void shouldHandleMathematicalOperations() {
        // Given
        Map<String, Object> data = Map.of(
            "baseAmount", 1000.0,
            "multiplier", 1.5
        );
        context.setRootObject(data);
        
        String lookupKeyExpression = "T(java.lang.String).valueOf((#baseAmount * #multiplier).intValue())";
        Expression expression = parser.parseExpression(lookupKeyExpression);
        
        // When
        Object result = expression.getValue(context);
        
        // Then
        assertThat(result).isEqualTo("1500");
    }
}
