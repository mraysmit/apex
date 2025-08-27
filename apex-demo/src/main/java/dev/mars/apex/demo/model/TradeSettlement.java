package dev.mars.apex.demo.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Model class representing a trade settlement for nested field reference lookup demonstrations.
 * Used to demonstrate lookup-key patterns with nested object navigation: "#trade.counterparty.countryCode"
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-22
 * @version 1.0
 */
public class TradeSettlement {
    
    private String settlementId;
    private Trade trade;
    private LocalDate settlementDate;
    private String status;
    private BigDecimal settlementAmount;
    private String currency;
    
    // Enriched fields (populated by nested field lookup)
    private String countryName;
    private String regulatoryZone;
    private String timeZone;
    private String settlementSystem;
    private Integer standardSettlementDays;
    private String holidayCalendar;
    private BigDecimal settlementFee;
    private String custodianBank;
    
    public TradeSettlement() {
    }
    
    public TradeSettlement(String settlementId, Trade trade, LocalDate settlementDate, 
                          String status, BigDecimal settlementAmount, String currency) {
        this.settlementId = settlementId;
        this.trade = trade;
        this.settlementDate = settlementDate;
        this.status = status;
        this.settlementAmount = settlementAmount;
        this.currency = currency;
    }
    
    // Getters and Setters
    
    public String getSettlementId() {
        return settlementId;
    }
    
    public void setSettlementId(String settlementId) {
        this.settlementId = settlementId;
    }
    
    public Trade getTrade() {
        return trade;
    }
    
    public void setTrade(Trade trade) {
        this.trade = trade;
    }
    
    public LocalDate getSettlementDate() {
        return settlementDate;
    }
    
    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public BigDecimal getSettlementAmount() {
        return settlementAmount;
    }
    
    public void setSettlementAmount(BigDecimal settlementAmount) {
        this.settlementAmount = settlementAmount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    // Enriched field getters and setters
    
    public String getCountryName() {
        return countryName;
    }
    
    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }
    
    public String getRegulatoryZone() {
        return regulatoryZone;
    }
    
    public void setRegulatoryZone(String regulatoryZone) {
        this.regulatoryZone = regulatoryZone;
    }
    
    public String getTimeZone() {
        return timeZone;
    }
    
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
    
    public String getSettlementSystem() {
        return settlementSystem;
    }
    
    public void setSettlementSystem(String settlementSystem) {
        this.settlementSystem = settlementSystem;
    }
    
    public Integer getStandardSettlementDays() {
        return standardSettlementDays;
    }
    
    public void setStandardSettlementDays(Integer standardSettlementDays) {
        this.standardSettlementDays = standardSettlementDays;
    }
    
    public String getHolidayCalendar() {
        return holidayCalendar;
    }
    
    public void setHolidayCalendar(String holidayCalendar) {
        this.holidayCalendar = holidayCalendar;
    }
    
    public BigDecimal getSettlementFee() {
        return settlementFee;
    }
    
    public void setSettlementFee(BigDecimal settlementFee) {
        this.settlementFee = settlementFee;
    }
    
    public String getCustodianBank() {
        return custodianBank;
    }
    
    public void setCustodianBank(String custodianBank) {
        this.custodianBank = custodianBank;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeSettlement that = (TradeSettlement) o;
        return Objects.equals(settlementId, that.settlementId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(settlementId);
    }
    
    @Override
    public String toString() {
        return "TradeSettlement{" +
                "settlementId='" + settlementId + '\'' +
                ", trade=" + trade +
                ", settlementDate=" + settlementDate +
                ", status='" + status + '\'' +
                ", settlementAmount=" + settlementAmount +
                ", currency='" + currency + '\'' +
                ", countryName='" + countryName + '\'' +
                ", regulatoryZone='" + regulatoryZone + '\'' +
                ", timeZone='" + timeZone + '\'' +
                ", settlementSystem='" + settlementSystem + '\'' +
                ", standardSettlementDays=" + standardSettlementDays +
                ", holidayCalendar='" + holidayCalendar + '\'' +
                ", settlementFee=" + settlementFee +
                ", custodianBank='" + custodianBank + '\'' +
                '}';
    }
    
    /**
     * Nested TradeB class to demonstrate nested field reference lookup
     */
    public static class Trade {
        private String tradeId;
        private String instrumentId;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDateTime tradeDate;
        private Counterparty counterparty;
        
        public Trade() {
        }
        
        public Trade(String tradeId, String instrumentId, BigDecimal quantity, 
                    BigDecimal price, LocalDateTime tradeDate, Counterparty counterparty) {
            this.tradeId = tradeId;
            this.instrumentId = instrumentId;
            this.quantity = quantity;
            this.price = price;
            this.tradeDate = tradeDate;
            this.counterparty = counterparty;
        }
        
        // Getters and Setters
        
        public String getTradeId() {
            return tradeId;
        }
        
        public void setTradeId(String tradeId) {
            this.tradeId = tradeId;
        }
        
        public String getInstrumentId() {
            return instrumentId;
        }
        
        public void setInstrumentId(String instrumentId) {
            this.instrumentId = instrumentId;
        }
        
        public BigDecimal getQuantity() {
            return quantity;
        }
        
        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }
        
        public BigDecimal getPrice() {
            return price;
        }
        
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        
        public LocalDateTime getTradeDate() {
            return tradeDate;
        }
        
        public void setTradeDate(LocalDateTime tradeDate) {
            this.tradeDate = tradeDate;
        }
        
        public Counterparty getCounterparty() {
            return counterparty;
        }
        
        public void setCounterparty(Counterparty counterparty) {
            this.counterparty = counterparty;
        }
        
        @Override
        public String toString() {
            return "TradeB{" +
                    "tradeId='" + tradeId + '\'' +
                    ", instrumentId='" + instrumentId + '\'' +
                    ", quantity=" + quantity +
                    ", price=" + price +
                    ", tradeDate=" + tradeDate +
                    ", counterparty=" + counterparty +
                    '}';
        }
    }
    
    /**
     * Nested Counterparty class to demonstrate deep nested field reference lookup
     */
    public static class Counterparty {
        private String counterpartyId;
        private String name;
        private String countryCode;
        private String city;
        private String legalEntityIdentifier;
        
        public Counterparty() {
        }
        
        public Counterparty(String counterpartyId, String name, String countryCode, 
                           String city, String legalEntityIdentifier) {
            this.counterpartyId = counterpartyId;
            this.name = name;
            this.countryCode = countryCode;
            this.city = city;
            this.legalEntityIdentifier = legalEntityIdentifier;
        }
        
        // Getters and Setters
        
        public String getCounterpartyId() {
            return counterpartyId;
        }
        
        public void setCounterpartyId(String counterpartyId) {
            this.counterpartyId = counterpartyId;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getCountryCode() {
            return countryCode;
        }
        
        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }
        
        public String getCity() {
            return city;
        }
        
        public void setCity(String city) {
            this.city = city;
        }
        
        public String getLegalEntityIdentifier() {
            return legalEntityIdentifier;
        }
        
        public void setLegalEntityIdentifier(String legalEntityIdentifier) {
            this.legalEntityIdentifier = legalEntityIdentifier;
        }
        
        @Override
        public String toString() {
            return "Counterparty{" +
                    "counterpartyId='" + counterpartyId + '\'' +
                    ", name='" + name + '\'' +
                    ", countryCode='" + countryCode + '\'' +
                    ", city='" + city + '\'' +
                    ", legalEntityIdentifier='" + legalEntityIdentifier + '\'' +
                    '}';
        }
    }
}
