![APEX System Logo](APEX%20System%20logo.png)

# FpML and XML Schemas for OTC Financial Products

**Version:** 2.1
**Date:** 2025-08-31
**Author:** Mark Andrew Ray-Smith Cityline Ltd

## Table of Contents

### Core OTC Derivatives
1. [Commodity Total Return Swap](#commodity-total-return-swap)
2. [Interest Rate Swap](#interest-rate-swap)
3. [Over-the-Counter Option](#over-the-counter-option)

### Additional Common OTC Products
4. [Credit Default Swap (CDS)](#credit-default-swap-cds)
5. [Foreign Exchange Forward (FX Forward)](#foreign-exchange-forward-fx-forward)
6. [Equity Swap](#equity-swap)
7. [Cross-Currency Swap](#cross-currency-swap)
8. [Swaption](#swaption)

### Technical Implementation
- [XML Message Examples](#xml-message-examples)
  - [Commodity Total Return Swap XML](#1-commodity-total-return-swap-commodity-trs)
  - [Interest Rate Swap XML](#2-interest-rate-swap-irs)
  - [OTC Option XML](#3-otc-option-eg-commodity-call-option)
  - [Credit Default Swap XML](#4-credit-default-swap-cds)
  - [FX Forward XML](#5-foreign-exchange-forward-fx-forward)
  - [Equity Swap XML](#6-equity-swap)
  - [Cross-Currency Swap XML](#7-cross-currency-swap)
  - [Swaption XML](#8-swaption)
- [XSD Schema Definitions](#xsd-schema-definitions)
  - [Generic Schema Template](#step-2-xsd-schema-generic-template)
  - [Core Product Types](#core-product-types)
  - [Extended Product Types](#extended-product-types)
- [Implementation Benefits](#why-use-this-approach)

---

## Overview

Here are the market-standard, open-source–style definitions (as you’d find on Wikipedia and in FpML) for the eight OTC derivatives:

## Core OTC Derivatives

### Commodity Total Return Swap
  A total return swap (TRS) is a derivative contract in which one party (the **total-return payer**) transfers to the other party (the **total-return receiver**) all the economic benefits and risks of a reference asset—both market-price changes and any income it generates—in exchange for periodic payments based on a fixed or floating financing rate on a notional amount. When the reference asset is a commodity or commodity index rather than equity or bonds, it is referred to as a **commodity total return swap**, allowing exposure to commodity price movements (and any lease yields) without taking delivery of the physical commodity ([en.wikipedia.org][1]).

### Interest Rate Swap
  An interest rate swap (IRS) is a linear OTC interest-rate derivative in which two counterparties agree to exchange streams of interest payments on a notional principal.  Typically one leg pays a fixed rate and the other pays a floating rate tied to an interbank benchmark (e.g. LIBOR, EURIBOR), over a set schedule of dates.  Each series of payments is called a “leg” of the swap, and the contract’s value derives from the difference between the present values of those fixed and floating legs ([en.wikipedia.org][2]).

### Over-the-Counter (OTC) Option
  An OTC option is a bilateral, privately negotiated option contract (often called a “dealer option”) that gives the holder the right—but not the obligation—to buy (a call) or sell (a put) a specified quantity of an underlying asset at a predetermined strike price on or before a specified expiration date.  Unlike standardized exchange-listed options, OTC options can be fully customized in terms, underlying, expiration, settlement, etc., but they carry greater counterparty credit risk and typically aren’t publicly reported ([en.wikipedia.org][3]).

### Credit Default Swap (CDS)
  A credit default swap is a financial derivative that allows an investor to "swap" or offset their credit risk with that of another investor. The buyer of a CDS makes periodic payments to the seller and, in return, receives a payoff if the underlying credit instrument defaults. CDSs are widely used for hedging credit risk or for speculative purposes ([en.wikipedia.org][4]).

### Foreign Exchange Forward (FX Forward)
  An FX forward is a contract to exchange one currency for another at a specified rate on a future date. Unlike FX futures, forwards are customizable and traded OTC. They are commonly used by corporations and investors to hedge against currency risk in international transactions ([en.wikipedia.org][5]).

### Equity Swap
  An equity swap is a financial derivative contract where a set of future cash flows are agreed to be exchanged between two counterparties at set dates in the future. One leg is typically based on equity returns while the other is based on a fixed or floating rate. This allows investors to gain exposure to equity returns without owning the underlying shares ([en.wikipedia.org][6]).

### Cross-Currency Swap
  A cross-currency swap involves the exchange of interest payments and principal in different currencies between two parties. It combines an interest rate swap with a foreign exchange transaction, allowing parties to manage both interest rate and currency risk simultaneously ([en.wikipedia.org][7]).

### Swaption
  A swaption is an option that gives the holder the right but not the obligation to enter into an interest rate swap. It can be either a payer swaption (right to pay fixed) or receiver swaption (right to receive fixed). Swaptions are used to hedge against interest rate volatility or to speculate on future rate movements ([en.wikipedia.org][8]).

[1]: https://en.wikipedia.org/wiki/Total_return_swap?utm_source=chatgpt.com "Total return swap"
[2]: https://en.wikipedia.org/wiki/Interest_rate_swap?utm_source=chatgpt.com "Interest rate swap"
[3]: https://en.wikipedia.org/wiki/Option_%28finance%29 "Option (finance) - Wikipedia"
[4]: https://en.wikipedia.org/wiki/Credit_default_swap "Credit default swap - Wikipedia"
[5]: https://en.wikipedia.org/wiki/Forward_contract "Forward contract - Wikipedia"
[6]: https://en.wikipedia.org/wiki/Equity_swap "Equity swap - Wikipedia"
[7]: https://en.wikipedia.org/wiki/Currency_swap "Currency swap - Wikipedia"
[8]: https://en.wikipedia.org/wiki/Swaption "Swaption - Wikipedia"


To represent these OTC derivatives as messages in XML and XSD format, we'll follow a structure inspired by Financial products Markup Language (FpML), which is the market standard XML-based protocol for OTC derivatives.

Below is a structured way to define each instrument as a trade message. I'll provide simplified but robust definitions that you can extend:

### Commodity Total Return Swap (TRS)

<CommodityTotalReturnSwap>
    <tradeDate>2025-06-28</tradeDate>
    <effectiveDate>2025-07-01</effectiveDate>
    <terminationDate>2026-07-01</terminationDate>
    <commodityReference>
        <commodity>Brent Crude Oil</commodity>
        <index>ICE Brent Index</index>
    </commodityReference>
    <notional currency="USD">5000000</notional>
    <payerParty>PartyA</payerParty>
    <receiverParty>PartyB</receiverParty>
    <financingRate type="floating" index="LIBOR">LIBOR+200bps</financingRate>
    <frequency>Quarterly</frequency>
</CommodityTotalReturnSwap>


### Interest Rate Swap (IRS)

<InterestRateSwap>
    <tradeDate>2025-06-28</tradeDate>
    <effectiveDate>2025-07-01</effectiveDate>
    <terminationDate>2030-07-01</terminationDate>
    <notional currency="USD">10000000</notional>
    <fixedLeg>
        <payer>PartyA</payer>
        <fixedRate>3.25</fixedRate>
        <dayCountConvention>30/360</dayCountConvention>
        <paymentFrequency>Annual</paymentFrequency>
    </fixedLeg>
    <floatingLeg>
        <payer>PartyB</payer>
        <floatingRateIndex>SOFR</floatingRateIndex>
        <spread>0.50</spread>
        <resetFrequency>Quarterly</resetFrequency>
        <dayCountConvention>Actual/360</dayCountConvention>
    </floatingLeg>
</InterestRateSwap>


### Over-the-Counter Option (OTC Option)

<OtcOption>
    <tradeDate>2025-06-28</tradeDate>
    <buyerParty>PartyA</buyerParty>
    <sellerParty>PartyB</sellerParty>
    <optionType>Call</optionType>
    <underlyingAsset>
        <commodity>Natural Gas</commodity>
        <unit>MMBtu</unit>
    </underlyingAsset>
    <strikePrice currency="USD">3.50</strikePrice>
    <notionalQuantity>10000</notionalQuantity>
    <expiryDate>2025-12-28</expiryDate>
    <settlementType>Cash</settlementType>
</OtcOption>

### Step 2: XSD Schema (Generic Template)
A simplified XSD schema you can expand upon might look like this:

<xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema">
<xsd:element name="Trade" type="TradeType"/>

    <xsd:complexType name="TradeType">
        <xsd:choice>
            <xsd:element name="CommodityTotalReturnSwap" type="CommodityTRSType"/>
            <xsd:element name="InterestRateSwap" type="InterestRateSwapType"/>
            <xsd:element name="OtcOption" type="OtcOptionType"/>
        </xsd:choice>
    </xsd:complexType>

    <xsd:complexType name="CommodityTRSType">
        <xsd:sequence>
            <xsd:element name="tradeDate" type="xsd:date"/>
            <xsd:element name="effectiveDate" type="xsd:date"/>
            <xsd:element name="terminationDate" type="xsd:date"/>
            <xsd:element name="commodityReference">
                <xsd:complexType>
                    <xsd:element name="commodity" type="xsd:string"/>
                    <xsd:element name="index" type="xsd:string"/>
                </xsd:complexType>
            </xsd:element>
            <xsd:element name="notional" type="NotionalAmount"/>
            <xsd:element name="payerParty" type="xsd:string"/>
            <xsd:element name="receiverParty" type="xsd:string"/>
            <xsd:element name="financingRate" type="xsd:string"/>
            <xsd:element name="frequency" type="xsd:string"/>
        </xsd:sequence>
    </xsd:complexType>

    <xsd:complexType name="InterestRateSwapType">
        <xsd:sequence>
            <xsd:element name="tradeDate" type="xsd:date"/>
            <xsd:element name="effectiveDate" type="xsd:date"/>
            <xsd:element name="terminationDate" type="xsd:date"/>
            <xsd:element name="notional" type="NotionalAmount"/>
            <xsd:element name="fixedLeg" type="LegType"/>
            <xsd:element name="floatingLeg" type="FloatingLegType"/>
        </xsd:sequence>
    </xsd:complexType>

    <xsd:complexType name="OtcOptionType">
        <xsd:sequence>
            <xsd:element name="tradeDate" type="xsd:date"/>
            <xsd:element name="buyerParty" type="xsd:string"/>
            <xsd:element name="sellerParty" type="xsd:string"/>
            <xsd:element name="optionType" type="xsd:string"/>
            <xsd:element name="underlyingAsset">
                <xsd:complexType>
                    <xsd:element name="commodity" type="xsd:string"/>
                    <xsd:element name="unit" type="xsd:string"/>
                </xsd:complexType>
            </xsd:element>
            <xsd:element name="strikePrice" type="PriceType"/>
            <xsd:element name="notionalQuantity" type="xsd:decimal"/>
            <xsd:element name="expiryDate" type="xsd:date"/>
            <xsd:element name="settlementType" type="xsd:string"/>
        </xsd:sequence>
    </xsd:complexType>

    <xsd:complexType name="NotionalAmount">
        <xsd:simpleContent>
            <xsd:extension base="xsd:decimal">
                <xsd:attribute name="currency" type="xsd:string" use="required"/>
            </xsd:extension>
        </xsd:simpleContent>
    </xsd:complexType>

    <xsd:complexType name="PriceType">
        <xsd:simpleContent>
            <xsd:extension base="xsd:decimal">
                <xsd:attribute name="currency" type="xsd:string" use="required"/>
            </xsd:extension>
        </xsd:simpleContent>
    </xsd:complexType>

    <xsd:complexType name="LegType">
        <xsd:sequence>
            <xsd:element name="payer" type="xsd:string"/>
            <xsd:element name="fixedRate" type="xsd:decimal"/>
            <xsd:element name="dayCountConvention" type="xsd:string"/>
            <xsd:element name="paymentFrequency" type="xsd:string"/>
        </xsd:sequence>
    </xsd:complexType>

    <xsd:complexType name="FloatingLegType">
        <xsd:sequence>
            <xsd:element name="payer" type="xsd:string"/>
            <xsd:element name="floatingRateIndex" type="xsd:string"/>
            <xsd:element name="spread" type="xsd:decimal"/>
            <xsd:element name="resetFrequency" type="xsd:string"/>
            <xsd:element name="dayCountConvention" type="xsd:string"/>
        </xsd:sequence>
    </xsd:complexType>
</xsd:schema>



To represent these OTC derivatives as messages in XML and XSD format, you'll want to follow a structure inspired by Financial products Markup Language (**FpML**), which is the market standard XML-based protocol for OTC derivatives.

Below is a structured way to define each instrument as a trade message. I'll provide simplified but robust definitions that you can extend:

---

## XML Message Examples

### Step 1: XML Examples for Trades

### 1. Commodity Total Return Swap (Commodity TRS)

```xml
<CommodityTotalReturnSwap>
    <tradeDate>2025-06-28</tradeDate>
    <effectiveDate>2025-07-01</effectiveDate>
    <terminationDate>2026-07-01</terminationDate>
    <commodityReference>
        <commodity>Brent Crude Oil</commodity>
        <index>ICE Brent Index</index>
    </commodityReference>
    <notional currency="USD">5000000</notional>
    <payerParty>PartyA</payerParty>
    <receiverParty>PartyB</receiverParty>
    <financingRate type="floating" index="LIBOR">LIBOR+200bps</financingRate>
    <frequency>Quarterly</frequency>
</CommodityTotalReturnSwap>
```

### 2. Interest Rate Swap (IRS)

```xml
<InterestRateSwap>
    <tradeDate>2025-06-28</tradeDate>
    <effectiveDate>2025-07-01</effectiveDate>
    <terminationDate>2030-07-01</terminationDate>
    <notional currency="USD">10000000</notional>
    <fixedLeg>
        <payer>PartyA</payer>
        <fixedRate>3.25</fixedRate>
        <dayCountConvention>30/360</dayCountConvention>
        <paymentFrequency>Annual</paymentFrequency>
    </fixedLeg>
    <floatingLeg>
        <payer>PartyB</payer>
        <floatingRateIndex>SOFR</floatingRateIndex>
        <spread>0.50</spread>
        <resetFrequency>Quarterly</resetFrequency>
        <dayCountConvention>Actual/360</dayCountConvention>
    </floatingLeg>
</InterestRateSwap>
```

### 3. OTC Option (e.g., Commodity Call Option)

```xml
<OtcOption>
    <tradeDate>2025-06-28</tradeDate>
    <buyerParty>PartyA</buyerParty>
    <sellerParty>PartyB</sellerParty>
    <optionType>Call</optionType>
    <underlyingAsset>
        <commodity>Natural Gas</commodity>
        <unit>MMBtu</unit>
    </underlyingAsset>
    <strikePrice currency="USD">3.50</strikePrice>
    <notionalQuantity>10000</notionalQuantity>
    <expiryDate>2025-12-28</expiryDate>
    <settlementType>Cash</settlementType>
</OtcOption>
```

---

## XSD Schema Definitions

### Step 2: XSD Schema (Generic Template)

A simplified XSD schema you can expand upon might look like this:

```xml
<xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema">
    <xsd:element name="Trade" type="TradeType"/>

    <xsd:complexType name="TradeType">
        <xsd:choice>
            <xsd:element name="CommodityTotalReturnSwap" type="CommodityTRSType"/>
            <xsd:element name="InterestRateSwap" type="InterestRateSwapType"/>
            <xsd:element name="OtcOption" type="OtcOptionType"/>
            <xsd:element name="CreditDefaultSwap" type="CreditDefaultSwapType"/>
            <xsd:element name="FxForward" type="FxForwardType"/>
            <xsd:element name="EquitySwap" type="EquitySwapType"/>
            <xsd:element name="CrossCurrencySwap" type="CrossCurrencySwapType"/>
            <xsd:element name="Swaption" type="SwaptionType"/>
        </xsd:choice>
    </xsd:complexType>

    <xsd:complexType name="CommodityTRSType">
        <xsd:sequence>
            <xsd:element name="tradeDate" type="xsd:date"/>
            <xsd:element name="effectiveDate" type="xsd:date"/>
            <xsd:element name="terminationDate" type="xsd:date"/>
            <xsd:element name="commodityReference">
                <xsd:complexType>
                    <xsd:element name="commodity" type="xsd:string"/>
                    <xsd:element name="index" type="xsd:string"/>
                </xsd:complexType>
            </xsd:element>
            <xsd:element name="notional" type="NotionalAmount"/>
            <xsd:element name="payerParty" type="xsd:string"/>
            <xsd:element name="receiverParty" type="xsd:string"/>
            <xsd:element name="financingRate" type="xsd:string"/>
            <xsd:element name="frequency" type="xsd:string"/>
        </xsd:sequence>
    </xsd:complexType>

    <xsd:complexType name="InterestRateSwapType">
        <xsd:sequence>
            <xsd:element name="tradeDate" type="xsd:date"/>
            <xsd:element name="effectiveDate" type="xsd:date"/>
            <xsd:element name="terminationDate" type="xsd:date"/>
            <xsd:element name="notional" type="NotionalAmount"/>
            <xsd:element name="fixedLeg" type="LegType"/>
            <xsd:element name="floatingLeg" type="FloatingLegType"/>
        </xsd:sequence>
    </xsd:complexType>

    <xsd:complexType name="OtcOptionType">
        <xsd:sequence>
            <xsd:element name="tradeDate" type="xsd:date"/>
            <xsd:element name="buyerParty" type="xsd:string"/>
            <xsd:element name="sellerParty" type="xsd:string"/>
            <xsd:element name="optionType" type="xsd:string"/>
            <xsd:element name="underlyingAsset">
                <xsd:complexType>
                    <xsd:element name="commodity" type="xsd:string"/>
                    <xsd:element name="unit" type="xsd:string"/>
                </xsd:complexType>
            </xsd:element>
            <xsd:element name="strikePrice" type="PriceType"/>
            <xsd:element name="notionalQuantity" type="xsd:decimal"/>
            <xsd:element name="expiryDate" type="xsd:date"/>
            <xsd:element name="settlementType" type="xsd:string"/>
        </xsd:sequence>
    </xsd:complexType>

    <xsd:complexType name="NotionalAmount">
        <xsd:simpleContent>
            <xsd:extension base="xsd:decimal">
                <xsd:attribute name="currency" type="xsd:string" use="required"/>
            </xsd:extension>
        </xsd:simpleContent>
    </xsd:complexType>

    <xsd:complexType name="PriceType">
        <xsd:simpleContent>
            <xsd:extension base="xsd:decimal">
                <xsd:attribute name="currency" type="xsd:string" use="required"/>
            </xsd:extension>
        </xsd:simpleContent>
    </xsd:complexType>

    <xsd:complexType name="LegType">
        <xsd:sequence>
            <xsd:element name="payer" type="xsd:string"/>
            <xsd:element name="fixedRate" type="xsd:decimal"/>
            <xsd:element name="dayCountConvention" type="xsd:string"/>
            <xsd:element name="paymentFrequency" type="xsd:string"/>
        </xsd:sequence>
    </xsd:complexType>

    <xsd:complexType name="FloatingLegType">
        <xsd:sequence>
            <xsd:element name="payer" type="xsd:string"/>
            <xsd:element name="floatingRateIndex" type="xsd:string"/>
            <xsd:element name="spread" type="xsd:decimal"/>
            <xsd:element name="resetFrequency" type="xsd:string"/>
            <xsd:element name="dayCountConvention" type="xsd:string"/>
        </xsd:sequence>
    </xsd:complexType>

    <xsd:complexType name="CreditDefaultSwapType">
        <xsd:sequence>
            <xsd:element name="tradeDate" type="xsd:date"/>
            <xsd:element name="effectiveDate" type="xsd:date"/>
            <xsd:element name="terminationDate" type="xsd:date"/>
            <xsd:element name="referenceEntity">
                <xsd:complexType>
                    <xsd:sequence>
                        <xsd:element name="entityName" type="xsd:string"/>
                        <xsd:element name="entityId" type="xsd:string"/>
                        <xsd:element name="sector" type="xsd:string"/>
                    </xsd:sequence>
                </xsd:complexType>
            </xsd:element>
            <xsd:element name="notional" type="NotionalAmount"/>
            <xsd:element name="protectionBuyer" type="xsd:string"/>
            <xsd:element name="protectionSeller" type="xsd:string"/>
            <xsd:element name="fixedRate" type="xsd:decimal"/>
            <xsd:element name="paymentFrequency" type="xsd:string"/>
            <xsd:element name="recoveryRate" type="xsd:decimal"/>
        </xsd:sequence>
    </xsd:complexType>

    <xsd:complexType name="FxForwardType">
        <xsd:sequence>
            <xsd:element name="tradeDate" type="xsd:date"/>
            <xsd:element name="valueDate" type="xsd:date"/>
            <xsd:element name="buyerParty" type="xsd:string"/>
            <xsd:element name="sellerParty" type="xsd:string"/>
            <xsd:element name="baseCurrency" type="xsd:string"/>
            <xsd:element name="quoteCurrency" type="xsd:string"/>
            <xsd:element name="notionalAmount" type="NotionalAmount"/>
            <xsd:element name="forwardRate" type="xsd:decimal"/>
            <xsd:element name="settlementType" type="xsd:string"/>
            <xsd:element name="fixingDate" type="xsd:date"/>
        </xsd:sequence>
    </xsd:complexType>

    <xsd:complexType name="EquitySwapType">
        <xsd:sequence>
            <xsd:element name="tradeDate" type="xsd:date"/>
            <xsd:element name="effectiveDate" type="xsd:date"/>
            <xsd:element name="terminationDate" type="xsd:date"/>
            <xsd:element name="notional" type="NotionalAmount"/>
            <xsd:element name="equityLeg">
                <xsd:complexType>
                    <xsd:sequence>
                        <xsd:element name="payer" type="xsd:string"/>
                        <xsd:element name="underlyingEquity">
                            <xsd:complexType>
                                <xsd:sequence>
                                    <xsd:element name="ticker" type="xsd:string"/>
                                    <xsd:element name="name" type="xsd:string"/>
                                    <xsd:element name="exchange" type="xsd:string"/>
                                </xsd:sequence>
                            </xsd:complexType>
                        </xsd:element>
                        <xsd:element name="returnType" type="xsd:string"/>
                        <xsd:element name="dividendTreatment" type="xsd:string"/>
                    </xsd:sequence>
                </xsd:complexType>
            </xsd:element>
            <xsd:element name="fundingLeg" type="FloatingLegType"/>
            <xsd:element name="paymentFrequency" type="xsd:string"/>
        </xsd:sequence>
    </xsd:complexType>

    <xsd:complexType name="CrossCurrencySwapType">
        <xsd:sequence>
            <xsd:element name="tradeDate" type="xsd:date"/>
            <xsd:element name="effectiveDate" type="xsd:date"/>
            <xsd:element name="terminationDate" type="xsd:date"/>
            <xsd:element name="initialExchangeRate" type="xsd:decimal"/>
            <xsd:element name="finalExchangeRate" type="xsd:string"/>
            <xsd:element name="baseCurrencyLeg">
                <xsd:complexType>
                    <xsd:sequence>
                        <xsd:element name="currency" type="xsd:string"/>
                        <xsd:element name="notional" type="xsd:decimal"/>
                        <xsd:element name="payer" type="xsd:string"/>
                        <xsd:element name="rateType" type="xsd:string"/>
                        <xsd:element name="fixedRate" type="xsd:decimal" minOccurs="0"/>
                        <xsd:element name="floatingRateIndex" type="xsd:string" minOccurs="0"/>
                        <xsd:element name="spread" type="xsd:decimal" minOccurs="0"/>
                        <xsd:element name="paymentFrequency" type="xsd:string"/>
                        <xsd:element name="dayCountConvention" type="xsd:string"/>
                    </xsd:sequence>
                </xsd:complexType>
            </xsd:element>
            <xsd:element name="quoteCurrencyLeg">
                <xsd:complexType>
                    <xsd:sequence>
                        <xsd:element name="currency" type="xsd:string"/>
                        <xsd:element name="notional" type="xsd:decimal"/>
                        <xsd:element name="payer" type="xsd:string"/>
                        <xsd:element name="rateType" type="xsd:string"/>
                        <xsd:element name="fixedRate" type="xsd:decimal" minOccurs="0"/>
                        <xsd:element name="floatingRateIndex" type="xsd:string" minOccurs="0"/>
                        <xsd:element name="spread" type="xsd:decimal" minOccurs="0"/>
                        <xsd:element name="resetFrequency" type="xsd:string" minOccurs="0"/>
                        <xsd:element name="dayCountConvention" type="xsd:string"/>
                    </xsd:sequence>
                </xsd:complexType>
            </xsd:element>
        </xsd:sequence>
    </xsd:complexType>

    <xsd:complexType name="SwaptionType">
        <xsd:sequence>
            <xsd:element name="tradeDate" type="xsd:date"/>
            <xsd:element name="buyerParty" type="xsd:string"/>
            <xsd:element name="sellerParty" type="xsd:string"/>
            <xsd:element name="optionType" type="xsd:string"/>
            <xsd:element name="expiryDate" type="xsd:date"/>
            <xsd:element name="exerciseStyle" type="xsd:string"/>
            <xsd:element name="premium" type="NotionalAmount"/>
            <xsd:element name="underlyingSwap">
                <xsd:complexType>
                    <xsd:sequence>
                        <xsd:element name="effectiveDate" type="xsd:date"/>
                        <xsd:element name="terminationDate" type="xsd:date"/>
                        <xsd:element name="notional" type="NotionalAmount"/>
                        <xsd:element name="fixedRate" type="xsd:decimal"/>
                        <xsd:element name="floatingRateIndex" type="xsd:string"/>
                        <xsd:element name="paymentFrequency" type="xsd:string"/>
                    </xsd:sequence>
                </xsd:complexType>
            </xsd:element>
            <xsd:element name="settlementType" type="xsd:string"/>
        </xsd:sequence>
    </xsd:complexType>
</xsd:schema>
```

---

## Why use this approach?

* Aligns with established market standards (FpML) for OTC derivatives.
* XML/XSD are widely supported, flexible, extensible, and clear.
* Can easily interface with systems for validation, messaging, storage, and transmission.


---

## Additional Common OTC Financial Products

The following section extends the original three OTC derivatives with five additional common over-the-counter financial products, representing the most actively traded instruments in global OTC markets:

1. **Credit Default Swap (CDS)** - Credit risk transfer instrument
2. **Foreign Exchange Forward (FX Forward)** - Currency exchange contract
3. **Equity Swap** - Equity return exchange mechanism
4. **Cross-Currency Swap** - Multi-currency interest rate swap
5. **Swaption** - Option on interest rate swap

### 4. Credit Default Swap (CDS)
A credit default swap is a financial derivative that allows an investor to "swap" or offset their credit risk with that of another investor. The buyer of a CDS makes periodic payments to the seller and, in return, receives a payoff if the underlying credit instrument defaults.

```xml
<CreditDefaultSwap>
    <tradeDate>2025-06-28</tradeDate>
    <effectiveDate>2025-07-01</effectiveDate>
    <terminationDate>2030-07-01</terminationDate>
    <referenceEntity>
        <entityName>ABC Corporation</entityName>
        <entityId>ABC123</entityId>
        <sector>Technology</sector>
    </referenceEntity>
    <notional currency="USD">10000000</notional>
    <protectionBuyer>PartyA</protectionBuyer>
    <protectionSeller>PartyB</protectionSeller>
    <fixedRate>250</fixedRate> <!-- basis points -->
    <paymentFrequency>Quarterly</paymentFrequency>
    <recoveryRate>0.40</recoveryRate>
    <creditEvents>
        <bankruptcy>true</bankruptcy>
        <failureToPay>true</failureToPay>
        <restructuring>true</restructuring>
    </creditEvents>
</CreditDefaultSwap>
```

### 5. Foreign Exchange Forward (FX Forward)
An FX forward is a contract to exchange one currency for another at a specified rate on a future date. Unlike FX futures, forwards are customizable and traded OTC.

```xml
<FxForward>
    <tradeDate>2025-06-28</tradeDate>
    <valueDate>2025-09-28</valueDate>
    <buyerParty>PartyA</buyerParty>
    <sellerParty>PartyB</sellerParty>
    <baseCurrency>EUR</baseCurrency>
    <quoteCurrency>USD</quoteCurrency>
    <notionalAmount currency="EUR">5000000</notionalAmount>
    <forwardRate>1.0850</forwardRate>
    <settlementType>Physical</settlementType>
    <fixingDate>2025-09-26</fixingDate>
</FxForward>
```

### 6. Equity Swap
An equity swap is a financial derivative contract where a set of future cash flows are agreed to be exchanged between two counterparties at set dates in the future. One leg is typically based on equity returns while the other is based on a fixed or floating rate.

```xml
<EquitySwap>
    <tradeDate>2025-06-28</tradeDate>
    <effectiveDate>2025-07-01</effectiveDate>
    <terminationDate>2026-07-01</terminationDate>
    <notional currency="USD">10000000</notional>
    <equityLeg>
        <payer>PartyA</payer>
        <underlyingEquity>
            <ticker>SPY</ticker>
            <name>SPDR S&P 500 ETF</name>
            <exchange>NYSE</exchange>
        </underlyingEquity>
        <returnType>Total</returnType> <!-- Price or Total -->
        <dividendTreatment>Reinvested</dividendTreatment>
    </equityLeg>
    <fundingLeg>
        <payer>PartyB</payer>
        <floatingRateIndex>SOFR</floatingRateIndex>
        <spread>150</spread> <!-- basis points -->
        <resetFrequency>Monthly</resetFrequency>
        <dayCountConvention>Actual/360</dayCountConvention>
    </fundingLeg>
    <paymentFrequency>Quarterly</paymentFrequency>
</EquitySwap>
```

### 7. Cross-Currency Swap
A cross-currency swap involves the exchange of interest payments and principal in different currencies between two parties. It combines an interest rate swap with a foreign exchange transaction.

```xml
<CrossCurrencySwap>
    <tradeDate>2025-06-28</tradeDate>
    <effectiveDate>2025-07-01</effectiveDate>
    <terminationDate>2030-07-01</terminationDate>
    <initialExchangeRate>1.0800</initialExchangeRate>
    <finalExchangeRate>AtMarket</finalExchangeRate>
    <baseCurrencyLeg>
        <currency>EUR</currency>
        <notional>10000000</notional>
        <payer>PartyA</payer>
        <rateType>Fixed</rateType>
        <fixedRate>2.75</fixedRate>
        <paymentFrequency>Annual</paymentFrequency>
        <dayCountConvention>30/360</dayCountConvention>
    </baseCurrencyLeg>
    <quoteCurrencyLeg>
        <currency>USD</currency>
        <notional>10800000</notional>
        <payer>PartyB</payer>
        <rateType>Floating</rateType>
        <floatingRateIndex>SOFR</floatingRateIndex>
        <spread>100</spread>
        <resetFrequency>Quarterly</resetFrequency>
        <dayCountConvention>Actual/360</dayCountConvention>
    </quoteCurrencyLeg>
</CrossCurrencySwap>
```

### 8. Swaption
A swaption is an option that gives the holder the right but not the obligation to enter into an interest rate swap. It can be either a payer swaption (right to pay fixed) or receiver swaption (right to receive fixed).

```xml
<Swaption>
    <tradeDate>2025-06-28</tradeDate>
    <buyerParty>PartyA</buyerParty>
    <sellerParty>PartyB</sellerParty>
    <optionType>Payer</optionType> <!-- Payer or Receiver -->
    <expiryDate>2025-12-28</expiryDate>
    <exerciseStyle>European</exerciseStyle>
    <premium currency="USD">250000</premium>
    <underlyingSwap>
        <effectiveDate>2026-01-02</effectiveDate>
        <terminationDate>2031-01-02</terminationDate>
        <notional currency="USD">50000000</notional>
        <fixedRate>3.50</fixedRate>
        <floatingRateIndex>SOFR</floatingRateIndex>
        <paymentFrequency>Quarterly</paymentFrequency>
    </underlyingSwap>
    <settlementType>Cash</settlementType>
</Swaption>
```

---

**Version:** 2.1 | **Author:** Mark Andrew Ray-Smith Cityline Ltd | **Date:** 2025-08-31

