Here are the market-standard, open-source–style definitions (as you’d find on Wikipedia and in FpML) for the three OTC derivatives:

* **Commodity Total Return Swap**
  A total return swap (TRS) is a derivative contract in which one party (the **total-return payer**) transfers to the other party (the **total-return receiver**) all the economic benefits and risks of a reference asset—both market-price changes and any income it generates—in exchange for periodic payments based on a fixed or floating financing rate on a notional amount. When the reference asset is a commodity or commodity index rather than equity or bonds, it is referred to as a **commodity total return swap**, allowing exposure to commodity price movements (and any lease yields) without taking delivery of the physical commodity ([en.wikipedia.org][1]).

* **Interest Rate Swap**
  An interest rate swap (IRS) is a linear OTC interest-rate derivative in which two counterparties agree to exchange streams of interest payments on a notional principal.  Typically one leg pays a fixed rate and the other pays a floating rate tied to an interbank benchmark (e.g. LIBOR, EURIBOR), over a set schedule of dates.  Each series of payments is called a “leg” of the swap, and the contract’s value derives from the difference between the present values of those fixed and floating legs ([en.wikipedia.org][2]).

* **Over-the-Counter (OTC) Option**
  An OTC option is a bilateral, privately negotiated option contract (often called a “dealer option”) that gives the holder the right—but not the obligation—to buy (a call) or sell (a put) a specified quantity of an underlying asset at a predetermined strike price on or before a specified expiration date.  Unlike standardized exchange-listed options, OTC options can be fully customized in terms, underlying, expiration, settlement, etc., but they carry greater counterparty credit risk and typically aren’t publicly reported ([en.wikipedia.org][3]).

[1]: https://en.wikipedia.org/wiki/Total_return_swap?utm_source=chatgpt.com "Total return swap"
[2]: https://en.wikipedia.org/wiki/Interest_rate_swap?utm_source=chatgpt.com "Interest rate swap"
[3]: https://en.wikipedia.org/wiki/Option_%28finance%29 "Option (finance) - Wikipedia"


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

## Step 1: XML Examples for Trades

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

## Step 2: XSD Schema (Generic Template)

A simplified XSD schema you can expand upon might look like this:

```xml
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
```

---

## Why use this approach?

* Aligns with established market standards (FpML) for OTC derivatives.
* XML/XSD are widely supported, flexible, extensible, and clear.
* Can easily interface with systems for validation, messaging, storage, and transmission.


Mark A Ray-Smith May 2025

