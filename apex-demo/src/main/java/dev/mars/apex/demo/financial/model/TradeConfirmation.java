package dev.mars.apex.demo.financial.model;

/**
 * Trade Confirmation model representing the root element of a trade confirmation message
 * Based on FpML and ISO 20022 standards for financial messaging
 */
public class TradeConfirmation {
    
    private Header header;
    private Trade trade;

    public TradeConfirmation() {
    }

    public TradeConfirmation(Header header, Trade trade) {
        this.header = header;
        this.trade = trade;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    @Override
    public String toString() {
        return "TradeConfirmation{" +
                "header=" + header +
                ", trade=" + trade +
                '}';
    }
}
