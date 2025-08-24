
# Types of Enrichment Relevant for Financial Services Post-Trade Settlement

This comprehensive guide demonstrates various types of enrichment that are essential for financial services post-trade settlement, with detailed XML examples and YAML enrichment rules based on industry standards including ISO 20022, FIX Protocol, FpML, and SWIFT messaging formats.

## 1. Reference Data Enrichment

Reference data enrichment adds standardized identifiers and institutional information to trade records, ensuring proper identification and routing for settlement processing.

### Example Source XML (Trade Confirmation)
```xml
<tradeConfirmation xmlns="http://www.fpml.org/FpML-5/confirmation">
  <header>
    <messageId>TRD-20241224-001</messageId>
    <sentBy>BANKGB2L</sentBy>
    <sendTo>DEUTDEFF</sendTo>
    <creationTimestamp>2024-12-24T10:30:00Z</creationTimestamp>
  </header>
  <trade>
    <tradeHeader>
      <partyTradeIdentifier>
        <partyReference href="party1"/>
        <tradeId>TRD-001-2024</tradeId>
      </partyTradeIdentifier>
      <tradeDate>2024-12-24</tradeDate>
    </tradeHeader>
    <security>
      <instrumentId>GB00B03MLX29</instrumentId>
      <instrumentType>EQUITY</instrumentType>
      <issuer>Royal Dutch Shell</issuer>
    </security>
    <counterparty>
      <partyId>PARTY123</partyId>
      <partyName>Deutsche Bank AG</partyName>
    </counterparty>
    <tradingVenue>XLON</tradingVenue>
    <quantity>10000</quantity>
    <price>2750.50</price>
    <currency>GBP</currency>
  </trade>
</tradeConfirmation>
```

### Additional XML Examples

#### Example 2: Bond Trade Confirmation
```xml
<tradeConfirmation xmlns="http://www.fpml.org/FpML-5/confirmation">
  <header>
    <messageId>BOND-20241224-002</messageId>
    <sentBy>JPMUS33</sentBy>
    <sendTo>GSCCUS33</sendTo>
  </header>
  <trade>
    <tradeHeader>
      <partyTradeIdentifier>
        <tradeId>BOND-002-2024</tradeId>
      </partyTradeIdentifier>
      <tradeDate>2024-12-24</tradeDate>
    </tradeHeader>
    <security>
      <instrumentId>US912828XG93</instrumentId>
      <instrumentType>GOVERNMENT_BOND</instrumentType>
      <issuer>US Treasury</issuer>
      <maturityDate>2034-12-15</maturityDate>
      <couponRate>4.25</couponRate>
    </security>
    <counterparty>
      <partyId>GOLDMAN_SACHS</partyId>
      <partyName>Goldman Sachs Group Inc</partyName>
    </counterparty>
    <tradingVenue>BONDDESK</tradingVenue>
    <quantity>1000000</quantity>
    <price>98.75</price>
    <currency>USD</currency>
    <accruedInterest>1250.00</accruedInterest>
  </trade>
</tradeConfirmation>
```

#### Example 3: Derivative Trade Confirmation
```xml
<tradeConfirmation xmlns="http://www.fpml.org/FpML-5/confirmation">
  <header>
    <messageId>DERIV-20241224-003</messageId>
    <sentBy>DEUTDEFF</sentBy>
    <sendTo>BARCGB22</sendTo>
  </header>
  <trade>
    <tradeHeader>
      <partyTradeIdentifier>
        <tradeId>SWAP-003-2024</tradeId>
      </partyTradeIdentifier>
      <tradeDate>2024-12-24</tradeDate>
    </tradeHeader>
    <derivative>
      <productType>INTEREST_RATE_SWAP</productType>
      <underlyingIndex>USD-LIBOR-3M</underlyingIndex>
      <notionalAmount>50000000</notionalAmount>
      <fixedRate>3.75</fixedRate>
      <floatingRate>LIBOR+0.25</floatingRate>
      <maturityDate>2029-12-24</maturityDate>
    </derivative>
    <counterparty>
      <partyId>BARCLAYS_BANK</partyId>
      <partyName>Barclays Bank PLC</partyName>
    </counterparty>
    <tradingVenue>OTC</tradingVenue>
    <currency>USD</currency>
  </trade>
</tradeConfirmation>
```

### YAML Enrichment Rules
```yaml
metadata:
  name: "Reference Data Enrichment"
  description: "Enrich trades with standardized reference data identifiers"
  version: "1.0.0"
  type: "rule-config"

rules:
  - id: "isin-format-validation"
    name: "ISIN Format Validation"
    condition: "#data.security != null && #data.security.instrumentId != null && #data.security.instrumentId.matches('^[A-Z]{2}[A-Z0-9]{9}[0-9]$')"
    message: "ISIN must follow format: 2 country letters + 9 alphanumeric + 1 check digit"
    severity: "ERROR"

enrichments:
  - id: "lei-enrichment"
    type: "lookup-enrichment"
    condition: "#data.counterparty != null && #data.counterparty.partyName != null"
    lookup-config:
      lookup-key: "counterparty.partyName"
      lookup-dataset:
        type: "inline"
        key-field: "partyName"
        data:
          - partyName: "Deutsche Bank AG"
            lei: "7LTWFZYICNSX8D621K86"
            jurisdiction: "DE"
          - partyName: "JPMorgan Chase"
            lei: "8EE8DF3643E15DBFDA05"
            jurisdiction: "US"
          - partyName: "Goldman Sachs"
            lei: "784F5XWPLTWKTBV3E584"
            jurisdiction: "US"
    field-mappings:
      - source-field: "lei"
        target-field: "counterparty.lei"
      - source-field: "jurisdiction"
        target-field: "counterparty.jurisdiction"

  - id: "isin-security-enrichment"
    type: "lookup-enrichment"
    condition: "#data.security != null && #data.security.instrumentId != null"
    lookup-config:
      lookup-key: "security.instrumentId"
      lookup-dataset:
        type: "inline"
        key-field: "isin"
        data:
          - isin: "GB00B03MLX29"
            cusip: "780259206"
            sedol: "B03MLX2"
            name: "Royal Dutch Shell PLC"
          - isin: "US0378331005"
            cusip: "037833100"
            sedol: "2046251"
            name: "Apple Inc"
    field-mappings:
      - source-field: "cusip"
        target-field: "security.cusip"
      - source-field: "sedol"
        target-field: "security.sedol"
      - source-field: "name"
        target-field: "security.name"

  - id: "mic-code-enrichment"
    type: "lookup-enrichment"
    condition: "#data.tradingVenue != null"
    lookup-config:
      lookup-key: "tradingVenue"
      lookup-dataset:
        type: "inline"
        key-field: "micCode"
        data:
          - micCode: "XLON"
            venueName: "London Stock Exchange"
            country: "GB"
          - micCode: "XNYS"
            venueName: "New York Stock Exchange"
            country: "US"
          - micCode: "XNAS"
            venueName: "NASDAQ"
            country: "US"
    field-mappings:
      - source-field: "venueName"
        target-field: "venue.venueName"
      - source-field: "country"
        target-field: "venue.country"

  - id: "bic-code-enrichment"
    type: "lookup-enrichment"
    condition: "#data.counterparty != null && #data.counterparty.partyName != null"
    lookup-config:
      lookup-key: "counterparty.partyName"
      lookup-dataset:
        type: "inline"
        key-field: "partyName"
        data:
          - partyName: "Deutsche Bank AG"
            bic: "DEUTDEFF"
          - partyName: "JPMorgan Chase"
            bic: "CHASUS33"
          - partyName: "Goldman Sachs"
            bic: "GSCCUS33"
    field-mappings:
      - source-field: "bic"
        target-field: "settlement.counterpartyBIC"

  - id: "ssi-enrichment"
    type: "lookup-enrichment"
    condition: "#data.counterparty != null && #data.counterparty.lei != null && #data.venue != null && #data.venue.country != null"
    lookup-config:
      lookup-key: "#counterparty.lei + '_' + #venue.country"
      lookup-dataset:
        type: "inline"
        key-field: "key"
        data:
          - key: "7LTWFZYICNSX8D621K86_GB"
            method: "CREST"
            account: "CREST001234"
            custodian: "Euroclear UK & Ireland"
            cutoffTime: "16:00 GMT"
          - key: "7LTWFZYICNSX8D621K86_US"
            method: "DTC"
            account: "DTC567890"
            custodian: "The Depository Trust Company"
            cutoffTime: "15:00 EST"
          - key: "8EE8DF3643E15DBFDA05_US"
            method: "DTC"
            account: "DTC123456"
            custodian: "The Depository Trust Company"
            cutoffTime: "15:00 EST"
          - key: "G5GSEF7VJP5I7OUK5573_GB"
            method: "CREST"
            account: "CREST998877"
            custodian: "Euroclear UK & Ireland"
            cutoffTime: "16:00 GMT"
    field-mappings:
      - source-field: "method"
        target-field: "settlement.method"
      - source-field: "account"
        target-field: "settlement.accountNumber"
      - source-field: "custodian"
        target-field: "settlement.custodian"
      - source-field: "cutoffTime"
        target-field: "settlement.cutoffTime"

# Additional Examples for Different Asset Classes

  - id: "bond-specific-enrichment"
    type: "lookup-enrichment"
    condition: "#data.trade.security.instrumentType == 'GOVERNMENT_BOND'"
    lookup-config:
      lookup-key: "trade.security.instrumentId"
      lookup-dataset:
        type: "inline"
        key-field: "cusip"
        data:
          - cusip: "US912828XG93"
            bondType: "TREASURY_NOTE"
            creditRating: "AAA"
            duration: 8.5
            convexity: 0.85
            yieldToMaturity: 4.15
    field-mappings:
      - source-field: "bondType"
        target-field: "security.bondType"
      - source-field: "creditRating"
        target-field: "security.creditRating"
      - source-field: "duration"
        target-field: "security.duration"
      - source-field: "yieldToMaturity"
        target-field: "security.yieldToMaturity"

  - id: "derivative-enrichment"
    type: "lookup-enrichment"
    condition: "#data.trade.derivative != null && #data.trade.derivative.productType != null"
    lookup-config:
      lookup-key: "trade.derivative.productType"
      lookup-dataset:
        type: "inline"
        key-field: "productType"
        data:
          - productType: "INTEREST_RATE_SWAP"
            assetClass: "RATES"
            clearingEligible: true
            marginClass: "CLASS_1"
            regulatoryCategory: "CLEARED_DERIVATIVE"
            riskWeight: 0.02
          - productType: "CREDIT_DEFAULT_SWAP"
            assetClass: "CREDIT"
            clearingEligible: false
            marginClass: "CLASS_2"
            regulatoryCategory: "BILATERAL_DERIVATIVE"
            riskWeight: 0.08
    field-mappings:
      - source-field: "assetClass"
        target-field: "derivative.assetClass"
      - source-field: "clearingEligible"
        target-field: "derivative.clearingEligible"
      - source-field: "marginClass"
        target-field: "derivative.marginClass"
      - source-field: "riskWeight"
        target-field: "derivative.riskWeight"
```

## 2. Counterparty Enrichment

Counterparty enrichment adds critical information about trading partners, including credit ratings, classifications, and relationship details essential for risk management and settlement processing.

### Example Source XML (Counterparty Data)
```xml
<counterpartyInfo xmlns="http://www.iso20022.org/counterparty">
  <counterpartyId>
    <lei>7LTWFZYICNSX8D621K86</lei>
    <partyName>Deutsche Bank AG</partyName>
    <jurisdiction>DE</jurisdiction>
  </counterpartyId>
  <contactInfo>
    <businessUnit>Prime Brokerage</businessUnit>
    <tradingDesk>Equity Trading</tradingDesk>
  </contactInfo>
  <accountInfo>
    <accountNumber>DB-PB-001234</accountNumber>
    <accountType>PRIME_BROKERAGE</accountType>
  </accountInfo>
</counterpartyInfo>
```

#### Additional Counterparty Examples

##### Example 2: Investment Manager Counterparty
```xml
<counterpartyInfo xmlns="http://www.iso20022.org/counterparty">
  <counterpartyId>
    <lei>549300E9W2RQMQRQZ748</lei>
    <partyName>BlackRock Fund Advisors</partyName>
    <jurisdiction>US</jurisdiction>
  </counterpartyId>
  <contactInfo>
    <businessUnit>Institutional Client Services</businessUnit>
    <tradingDesk>Fixed Income Trading</tradingDesk>
  </contactInfo>
  <accountInfo>
    <accountNumber>BR-ICS-567890</accountNumber>
    <accountType>INSTITUTIONAL_INVESTMENT</accountType>
  </accountInfo>
  <riskProfile>
    <clientType>ASSET_MANAGER</clientType>
    <aum>9500000000000</aum>
    <riskRating>LOW</riskRating>
  </riskProfile>
</counterpartyInfo>
```

##### Example 3: Hedge Fund Counterparty
```xml
<counterpartyInfo xmlns="http://www.iso20022.org/counterparty">
  <counterpartyId>
    <lei>5493000F4ZG1KEXQJL62</lei>
    <partyName>Bridgewater Associates LP</partyName>
    <jurisdiction>US</jurisdiction>
  </counterpartyId>
  <contactInfo>
    <businessUnit>Trading Operations</businessUnit>
    <tradingDesk>Multi-Asset Trading</tradingDesk>
  </contactInfo>
  <accountInfo>
    <accountNumber>BW-TO-123456</accountNumber>
    <accountType>HEDGE_FUND</accountType>
  </accountInfo>
  <riskProfile>
    <clientType>HEDGE_FUND</clientType>
    <aum>140000000000</aum>
    <riskRating>MEDIUM</riskRating>
    <leverageRatio>3.2</leverageRatio>
  </riskProfile>
</counterpartyInfo>
```

### YAML Enrichment Rules
```yaml
metadata:
  name: "Counterparty Enrichment"
  description: "Enrich counterparty data with credit ratings, classifications, and relationship information"
  version: "1.0.0"
  type: "rule-config"

enrichments:
  - id: "credit-rating-enrichment"
    type: "lookup-enrichment"
    condition: "#data.counterpartyId != null && #data.counterpartyId.lei != null"
    lookup-config:
      lookup-key: "counterpartyId.lei"
      lookup-dataset:
        type: "inline"
        key-field: "lei"
        data:
          - lei: "7LTWFZYICNSX8D621K86"
            moodys: "A1"
            sp: "A+"
            fitch: "A+"
          - lei: "8EE8DF3643E15DBFDA05"
            moodys: "A2"
            sp: "A"
            fitch: "A"
          - lei: "784F5XWPLTWKTBV3E584"
            moodys: "A1"
            sp: "A+"
            fitch: "A"
    field-mappings:
      - source-field: "moodys"
        target-field: "creditRating.moodys"
      - source-field: "sp"
        target-field: "creditRating.sp"
      - source-field: "fitch"
        target-field: "creditRating.fitch"

  - id: "counterparty-classification"
    type: "lookup-enrichment"
    condition: "#data.counterpartyId != null && #data.counterpartyId.lei != null"
    lookup-config:
      lookup-key: "counterpartyId.lei"
      lookup-dataset:
        type: "inline"
        key-field: "lei"
        data:
          - lei: "7LTWFZYICNSX8D621K86"
            entityType: "BANK"
            businessModel: "UNIVERSAL_BANK"
            regulatoryStatus: "SYSTEMICALLY_IMPORTANT"
          - lei: "8EE8DF3643E15DBFDA05"
            entityType: "BANK"
            businessModel: "COMMERCIAL_BANK"
            regulatoryStatus: "SYSTEMICALLY_IMPORTANT"
          - lei: "784F5XWPLTWKTBV3E584"
            entityType: "INVESTMENT_BANK"
            businessModel: "INVESTMENT_BANK"
            regulatoryStatus: "SYSTEMICALLY_IMPORTANT"
    field-mappings:
      - source-field: "entityType"
        target-field: "classification.entityType"
      - source-field: "businessModel"
        target-field: "classification.businessModel"
      - source-field: "regulatoryStatus"
        target-field: "classification.regulatoryStatus"

  - id: "relationship-tier-enrichment"
    type: "lookup-enrichment"
    condition: "#data.accountInfo != null && #data.accountInfo.accountNumber != null"
    lookup-config:
      lookup-key: "accountInfo.accountNumber"
      lookup-dataset:
        type: "inline"
        key-field: "accountNumber"
        data:
          - accountNumber: "DB-PB-001234"
            tier: "TIER_1"
            status: "PREFERRED"
            creditLimit: 1000000000
          - accountNumber: "JPM-PB-005678"
            tier: "TIER_1"
            status: "PREFERRED"
            creditLimit: 750000000
          - accountNumber: "GS-PB-009876"
            tier: "TIER_2"
            status: "STANDARD"
            creditLimit: 500000000
    field-mappings:
      - source-field: "tier"
        target-field: "relationship.tier"
      - source-field: "status"
        target-field: "relationship.status"
      - source-field: "creditLimit"
        target-field: "relationship.creditLimit"

  - id: "netting-agreement-status"
    type: "lookup-enrichment"
    condition: "#data.counterpartyId != null && #data.counterpartyId.lei != null"
    lookup-config:
      lookup-key: "counterpartyId.lei"
      lookup-dataset:
        type: "inline"
        key-field: "lei"
        data:
          - lei: "7LTWFZYICNSX8D621K86"
            isdaMasterAgreement: true
            nettingEligible: true
            csaAgreement: true
          - lei: "8EE8DF3643E15DBFDA05"
            isdaMasterAgreement: true
            nettingEligible: true
            csaAgreement: true
          - lei: "784F5XWPLTWKTBV3E584"
            isdaMasterAgreement: true
            nettingEligible: true
            csaAgreement: false
    field-mappings:
      - source-field: "isdaMasterAgreement"
        target-field: "legalAgreements.isdaMasterAgreement"
      - source-field: "nettingEligible"
        target-field: "legalAgreements.nettingEligible"
      - source-field: "csaAgreement"
        target-field: "legalAgreements.csaAgreement"

  - id: "clearing-house-contribution"
    type: "lookup-enrichment"
    condition: "#data.counterpartyId != null && #data.counterpartyId.lei != null"
    lookup-config:
      lookup-key: "counterpartyId.lei"
      lookup-dataset:
        type: "inline"
        key-field: "lei"
        data:
          - lei: "7LTWFZYICNSX8D621K86"
            lchContribution: 50000000
            cmeContribution: 25000000
            membershipStatus: "CLEARING_MEMBER"
            defaultFundTier: "TIER_1"
          - lei: "8EE8DF3643E15DBFDA05"
            lchContribution: 75000000
            cmeContribution: 40000000
            membershipStatus: "CLEARING_MEMBER"
            defaultFundTier: "TIER_1"
          - lei: "784F5XWPLTWKTBV3E584"
            lchContribution: 45000000
            cmeContribution: 30000000
            membershipStatus: "CLEARING_MEMBER"
            defaultFundTier: "TIER_2"
          - lei: "549300E9W2RQMQRQZ748"
            lchContribution: 0
            cmeContribution: 0
            membershipStatus: "NON_CLEARING_MEMBER"
            defaultFundTier: "NOT_APPLICABLE"
          - lei: "5493000F4ZG1KEXQJL62"
            lchContribution: 15000000
            cmeContribution: 8000000
            membershipStatus: "CLIENT"
            defaultFundTier: "TIER_3"
    field-mappings:
      - source-field: "lchContribution"
        target-field: "clearingHouse.lchContribution"
      - source-field: "cmeContribution"
        target-field: "clearingHouse.cmeContribution"
      - source-field: "membershipStatus"
        target-field: "clearingHouse.membershipStatus"
      - source-field: "defaultFundTier"
        target-field: "clearingHouse.defaultFundTier"

# Additional Counterparty Enrichment Examples

  - id: "client-onboarding-status"
    type: "lookup-enrichment"
    condition: "#data.accountInfo != null && #data.accountInfo.accountNumber != null"
    lookup-config:
      lookup-key: "accountInfo.accountNumber"
      lookup-dataset:
        type: "inline"
        key-field: "accountNumber"
        data:
          - accountNumber: "DB-PB-001234"
            onboardingDate: "2020-03-15"
            kycStatus: "VERIFIED"
            amlStatus: "CLEARED"
            lastReviewDate: "2024-03-15"
            nextReviewDate: "2025-03-15"
            documentationComplete: true
          - accountNumber: "BR-ICS-567890"
            onboardingDate: "2019-08-22"
            kycStatus: "VERIFIED"
            amlStatus: "CLEARED"
            lastReviewDate: "2024-08-22"
            nextReviewDate: "2025-08-22"
            documentationComplete: true
          - accountNumber: "BW-TO-123456"
            onboardingDate: "2021-11-10"
            kycStatus: "VERIFIED"
            amlStatus: "UNDER_REVIEW"
            lastReviewDate: "2024-11-10"
            nextReviewDate: "2025-11-10"
            documentationComplete: false
    field-mappings:
      - source-field: "onboardingDate"
        target-field: "compliance.onboardingDate"
      - source-field: "kycStatus"
        target-field: "compliance.kycStatus"
      - source-field: "amlStatus"
        target-field: "compliance.amlStatus"
      - source-field: "documentationComplete"
        target-field: "compliance.documentationComplete"

  - id: "counterparty-risk-limits"
    type: "calculation-enrichment"
    condition: "#data.riskProfile != null && #data.riskProfile.aum != null"
    calculations:
      - field: "riskLimits.maxSingleTradeLimit"
        expression: "#riskProfile.aum * 0.001"  # 0.1% of AUM
      - field: "riskLimits.dailyTradingLimit"
        expression: "#riskProfile.aum * 0.01"   # 1% of AUM
      - field: "riskLimits.concentrationLimit"
        expression: "#riskProfile.aum * 0.05"   # 5% of AUM
      - field: "riskLimits.leverageLimit"
        expression: "#riskProfile.leverageRatio != null ? #riskProfile.leverageRatio : 1.0"
```

## 3. Regulatory Enrichment

Regulatory enrichment ensures compliance with financial regulations by adding required reporting fields, identifiers, and flags for various jurisdictions including MiFID II, EMIR, and Dodd-Frank.

### Example Source XML (Regulatory Trade Report)
```xml
<regulatoryReport xmlns="http://www.esma.europa.eu/emir">
  <reportHeader>
    <reportingEntity>
      <lei>7LTWFZYICNSX8D621K86</lei>
      <jurisdiction>EU</jurisdiction>
    </reportingEntity>
    <reportingTimestamp>2024-12-24T10:30:00Z</reportingTimestamp>
  </reportHeader>
  <transactionDetails>
    <transactionId>TXN-20241224-001</transactionId>
    <executionTimestamp>2024-12-24T10:15:00Z</executionTimestamp>
    <productType>EQUITY</productType>
    <underlyingInstrument>
      <isin>GB00B03MLX29</isin>
      <name>Royal Dutch Shell PLC</name>
    </underlyingInstrument>
    <counterparty>
      <lei>8EE8DF3643E15DBFDA05</lei>
      <jurisdiction>US</jurisdiction>
    </counterparty>
    <tradingVenue>XLON</tradingVenue>
    <notionalAmount currency="GBP">27505000</notionalAmount>
  </transactionDetails>
</regulatoryReport>
```

#### Additional Regulatory Examples

##### Example 2: MiFID II Transaction Report
```xml
<mifidReport xmlns="http://www.esma.europa.eu/mifid">
  <reportHeader>
    <reportingEntity>
      <lei>G5GSEF7VJP5I7OUK5573</lei>
      <jurisdiction>GB</jurisdiction>
      <reportingFirm>Barclays Bank PLC</reportingFirm>
    </reportingEntity>
    <reportingTimestamp>2024-12-24T11:45:00Z</reportingTimestamp>
  </reportHeader>
  <transactionDetails>
    <transactionId>MIFID-20241224-002</transactionId>
    <executionTimestamp>2024-12-24T11:30:00Z</executionTimestamp>
    <productType>DERIVATIVE</productType>
    <underlyingInstrument>
      <isin>DE0001102309</isin>
      <name>German Government Bond 10Y</name>
    </underlyingInstrument>
    <counterparty>
      <lei>549300E9W2RQMQRQZ748</lei>
      <jurisdiction>US</jurisdiction>
      <counterpartyType>PROFESSIONAL_CLIENT</counterpartyType>
    </counterparty>
    <tradingVenue>OTC</tradingVenue>
    <notionalAmount currency="EUR">100000000</notionalAmount>
    <transmissionFlag>true</transmissionFlag>
    <commodityDerivativeIndicator>false</commodityDerivativeIndicator>
  </transactionDetails>
</mifidReport>
```

##### Example 3: Dodd-Frank Swap Report
```xml
<doddFrankReport xmlns="http://www.cftc.gov/swaps">
  <reportHeader>
    <reportingEntity>
      <lei>8EE8DF3643E15DBFDA05</lei>
      <jurisdiction>US</jurisdiction>
      <reportingFirm>JPMorgan Chase Bank NA</reportingFirm>
    </reportingEntity>
    <reportingTimestamp>2024-12-24T14:20:00Z</reportingTimestamp>
  </reportHeader>
  <swapDetails>
    <uniqueSwapIdentifier>USI-JPMC-20241224-003</uniqueSwapIdentifier>
    <executionTimestamp>2024-12-24T14:00:00Z</executionTimestamp>
    <assetClass>INTEREST_RATE</assetClass>
    <productType>VANILLA_SWAP</productType>
    <underlyingAsset>
      <referenceRate>USD-SOFR</referenceRate>
      <tenor>5Y</tenor>
    </underlyingAsset>
    <counterparty>
      <lei>784F5XWPLTWKTBV3E584</lei>
      <jurisdiction>US</jurisdiction>
      <counterpartyType>SWAP_DEALER</counterpartyType>
    </counterparty>
    <executionVenue>SEF</executionVenue>
    <notionalAmount currency="USD">250000000</notionalAmount>
    <clearingIndicator>true</clearingIndicator>
    <clearingHouse>CME_CLEARING</clearingHouse>
  </swapDetails>
</doddFrankReport>
```

### YAML Enrichment Rules
```yaml
metadata:
  name: "Regulatory Enrichment"
  description: "Add regulatory reporting fields and compliance flags"
  version: "1.0.0"
  type: "rule-config"

enrichments:
  - id: "regulatory-jurisdiction-flags"
    type: "calculation-enrichment"
    condition: "#data.reportHeader != null && #data.reportHeader.reportingEntity != null && #data.reportHeader.reportingEntity.jurisdiction != null"
    calculations:
      - field: "regulatory.mifidII.applicable"
        expression: "#reportHeader.reportingEntity.jurisdiction == 'EU'"
      - field: "regulatory.emir.applicable"
        expression: "#reportHeader.reportingEntity.jurisdiction == 'EU'"
      - field: "regulatory.doddFrank.applicable"
        expression: "#counterparty != null && #counterparty.jurisdiction == 'US'"
      - field: "regulatory.mifidII.venueReporting"
        expression: "#tradingVenue == 'XLON' || #tradingVenue == 'XPAR'"

  - id: "uti-generation"
    type: "calculation-enrichment"
    condition: "#data.transactionDetails != null && #data.transactionDetails.transactionId != null && #data.reportHeader != null"
    calculations:
      - field: "regulatory.uti"
        expression: "#reportHeader.reportingEntity.lei + '-' + #transactionDetails.transactionId + '-' + T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))"
      - field: "regulatory.utiPrefix"
        expression: "#reportHeader.reportingEntity.lei"
      - field: "regulatory.utiGenerationTimestamp"
        expression: "T(java.time.Instant).now().toString()"

  - id: "upi-assignment"
    type: "lookup-enrichment"
    condition: "#data.transactionDetails != null && #data.transactionDetails.productType != null && #data.underlyingInstrument != null"
    lookup-config:
      lookup-key: "#transactionDetails.productType + '_' + #underlyingInstrument.isin"
      lookup-dataset:
        type: "inline"
        key-field: "key"
        data:
          - key: "EQUITY_GB00B03MLX29"
            upi: "EQ000000000000000001"
            productClassification: "EQUITY_SINGLE_NAME"
          - key: "EQUITY_US0378331005"
            upi: "EQ000000000000000002"
            productClassification: "EQUITY_SINGLE_NAME"
          - key: "BOND_US912828XG93"
            upi: "BD000000000000000001"
            productClassification: "FIXED_INCOME_GOVERNMENT"
    field-mappings:
      - source-field: "upi"
        target-field: "regulatory.upi"
      - source-field: "productClassification"
        target-field: "regulatory.productClassification"

  - id: "mifid-ii-fields"
    type: "calculation-enrichment"
    condition: "#data.regulatory != null && #data.regulatory.mifidII != null && #data.regulatory.mifidII.applicable == true"
    calculations:
      - field: "regulatory.mifidII.transactionReferenceNumber"
        expression: "T(java.util.UUID).randomUUID().toString()"

  - id: "mifid-venue-classification"
    type: "lookup-enrichment"
    condition: "#data.regulatory != null && #data.regulatory.mifidII != null && #data.regulatory.mifidII.applicable == true && #data.tradingVenue != null"
    lookup-config:
      lookup-key: "tradingVenue"
      lookup-dataset:
        type: "inline"
        key-field: "venue"
        data:
          - venue: "XLON"
            venueType: "REGULATED_MARKET"
            liquidityProvision: true
          - venue: "XPAR"
            venueType: "REGULATED_MARKET"
            liquidityProvision: true
          - venue: "BATS"
            venueType: "MULTILATERAL_TRADING_FACILITY"
            liquidityProvision: false
    field-mappings:
      - source-field: "venueType"
        target-field: "regulatory.mifidII.venueType"
      - source-field: "liquidityProvision"
        target-field: "regulatory.mifidII.liquidityProvision"

  - id: "emir-fields"
    type: "calculation-enrichment"
    condition: "#data.regulatory != null && #data.regulatory.emir != null && #data.regulatory.emir.applicable == true"
    calculations:
      - field: "regulatory.emir.reportingObligation"
        expression: "'BOTH_COUNTERPARTIES'"
      - field: "regulatory.emir.clearingObligation"
        expression: "#notionalAmount != null && #notionalAmount.value > 1000000"
      - field: "regulatory.emir.clearingThreshold"
        expression: "#notionalAmount != null && #notionalAmount.value > 1000000 ? 'ABOVE_THRESHOLD' : 'BELOW_THRESHOLD'"

  - id: "legal-documentation-status"
    type: "lookup-enrichment"
    condition: "#data.counterparty != null && #data.counterparty.lei != null && #data.reportHeader != null"
    lookup-config:
      lookup-key: "#reportHeader.reportingEntity.lei + '_' + #counterparty.lei"
      lookup-dataset:
        type: "inline"
        key-field: "key"
        data:
          - key: "7LTWFZYICNSX8D621K86_8EE8DF3643E15DBFDA05"
            masterAgreementType: "ISDA_MASTER_AGREEMENT"
            masterAgreementVersion: "2002"
            csaInPlace: true
            csaThreshold: 50000000
            minimumTransferAmount: 1000000
          - key: "7LTWFZYICNSX8D621K86_784F5XWPLTWKTBV3E584"
            masterAgreementType: "ISDA_MASTER_AGREEMENT"
            masterAgreementVersion: "2002"
            csaInPlace: true
            csaThreshold: 25000000
            minimumTransferAmount: 500000
          - key: "G5GSEF7VJP5I7OUK5573_549300E9W2RQMQRQZ748"
            masterAgreementType: "ISDA_MASTER_AGREEMENT"
            masterAgreementVersion: "2002"
            csaInPlace: false
            csaThreshold: 0
            minimumTransferAmount: 0
          - key: "8EE8DF3643E15DBFDA05_784F5XWPLTWKTBV3E584"
            masterAgreementType: "ISDA_MASTER_AGREEMENT"
            masterAgreementVersion: "2002"
            csaInPlace: true
            csaThreshold: 100000000
            minimumTransferAmount: 2000000
    field-mappings:
      - source-field: "masterAgreementType"
        target-field: "legalDocumentation.masterAgreementType"
      - source-field: "masterAgreementVersion"
        target-field: "legalDocumentation.masterAgreementVersion"
      - source-field: "csaInPlace"
        target-field: "legalDocumentation.csaInPlace"
      - source-field: "csaThreshold"
        target-field: "legalDocumentation.csaThreshold"
      - source-field: "minimumTransferAmount"
        target-field: "legalDocumentation.minimumTransferAmount"

# Additional Regulatory Enrichment Examples

  - id: "mifid-specific-fields"
    type: "calculation-enrichment"
    condition: "#data.reportHeader != null && #data.reportHeader.reportingEntity.jurisdiction == 'GB' && #data.transactionDetails != null"
    calculations:
      - field: "regulatory.mifidII.transactionReferenceNumber"
        expression: "T(java.util.UUID).randomUUID().toString()"
      - field: "regulatory.mifidII.reportingFlag"
        expression: "#transactionDetails.transmissionFlag != null ? #transactionDetails.transmissionFlag : false"
      - field: "regulatory.mifidII.commodityDerivativeIndicator"
        expression: "#transactionDetails.commodityDerivativeIndicator != null ? #transactionDetails.commodityDerivativeIndicator : false"
      - field: "regulatory.mifidII.clientType"
        expression: "#counterparty.counterpartyType != null ? #counterparty.counterpartyType : 'UNKNOWN'"

  - id: "dodd-frank-fields"
    type: "calculation-enrichment"
    condition: "#data.reportHeader != null && #data.reportHeader.reportingEntity.jurisdiction == 'US' && #data.swapDetails != null"
    calculations:
      - field: "regulatory.doddFrank.uniqueSwapIdentifier"
        expression: "#swapDetails.uniqueSwapIdentifier"
      - field: "regulatory.doddFrank.clearingIndicator"
        expression: "#swapDetails.clearingIndicator != null ? #swapDetails.clearingIndicator : false"
      - field: "regulatory.doddFrank.clearingHouse"
        expression: "#swapDetails.clearingHouse != null ? #swapDetails.clearingHouse : 'NOT_CLEARED'"
      - field: "regulatory.doddFrank.executionVenue"
        expression: "#swapDetails.executionVenue != null ? #swapDetails.executionVenue : 'OFF_FACILITY'"
      - field: "regulatory.doddFrank.assetClass"
        expression: "#swapDetails.assetClass"

  - id: "regulatory-threshold-check"
    type: "calculation-enrichment"
    condition: "#data.notionalAmount != null || #data.transactionDetails.notionalAmount != null || #data.swapDetails.notionalAmount != null"
    calculations:
      - field: "regulatory.thresholds.emirThreshold"
        expression: "(#notionalAmount != null ? #notionalAmount : (#transactionDetails != null && #transactionDetails.notionalAmount != null ? #transactionDetails.notionalAmount : (#swapDetails != null ? #swapDetails.notionalAmount : 0))) > 1000000"
      - field: "regulatory.thresholds.mifidThreshold"
        expression: "(#notionalAmount != null ? #notionalAmount : (#transactionDetails != null && #transactionDetails.notionalAmount != null ? #transactionDetails.notionalAmount : (#swapDetails != null ? #swapDetails.notionalAmount : 0))) > 500000"
      - field: "regulatory.thresholds.doddFrankThreshold"
        expression: "(#notionalAmount != null ? #notionalAmount : (#transactionDetails != null && #transactionDetails.notionalAmount != null ? #transactionDetails.notionalAmount : (#swapDetails != null ? #swapDetails.notionalAmount : 0))) > 8000000000"
```

## 4. Risk Enrichment

Risk enrichment adds critical risk metrics, exposure calculations, and margin requirements essential for risk management and regulatory capital calculations.

### Example Source XML (Risk Data)
```xml
<riskData xmlns="http://www.isda.org/risk">
  <portfolio>
    <portfolioId>PF-EQUITY-001</portfolioId>
    <counterparty>
      <lei>7LTWFZYICNSX8D621K86</lei>
      <name>Deutsche Bank AG</name>
    </counterparty>
    <asOfDate>2024-12-24</asOfDate>
  </portfolio>
  <positions>
    <position>
      <instrumentId>GB00B03MLX29</instrumentId>
      <quantity>10000</quantity>
      <marketValue currency="GBP">27505000</marketValue>
      <unrealizedPnL currency="GBP">125000</unrealizedPnL>
    </position>
  </positions>
  <riskMetrics>
    <baseDate>2024-12-24</baseDate>
    <currency>GBP</currency>
  </riskMetrics>
</riskData>
```

#### Additional Risk Data Examples

##### Example 2: Multi-Asset Portfolio Risk
```xml
<riskData xmlns="http://www.isda.org/risk">
  <portfolio>
    <portfolioId>PF-MULTI-002</portfolioId>
    <counterparty>
      <lei>549300E9W2RQMQRQZ748</lei>
      <name>BlackRock Fund Advisors</name>
    </counterparty>
    <asOfDate>2024-12-24</asOfDate>
  </portfolio>
  <positions>
    <position>
      <instrumentId>US0378331005</instrumentId>
      <instrumentType>EQUITY</instrumentType>
      <quantity>50000</quantity>
      <marketValue currency="USD">9500000</marketValue>
      <unrealizedPnL currency="USD">-75000</unrealizedPnL>
      <beta>1.2</beta>
    </position>
    <position>
      <instrumentId>US912828XG93</instrumentId>
      <instrumentType>GOVERNMENT_BOND</instrumentType>
      <quantity>10000000</quantity>
      <marketValue currency="USD">9875000</marketValue>
      <unrealizedPnL currency="USD">25000</unrealizedPnL>
      <duration>8.5</duration>
      <convexity>0.85</convexity>
    </position>
    <position>
      <instrumentId>SWAP-USD-SOFR-5Y</instrumentId>
      <instrumentType>INTEREST_RATE_SWAP</instrumentType>
      <notionalAmount currency="USD">100000000</notionalAmount>
      <marketValue currency="USD">-250000</marketValue>
      <unrealizedPnL currency="USD">-250000</unrealizedPnL>
      <dv01>8500</dv01>
    </position>
  </positions>
  <riskMetrics>
    <baseDate>2024-12-24</baseDate>
    <currency>USD</currency>
    <portfolioValue>19125000</portfolioValue>
  </riskMetrics>
</riskData>
```

##### Example 3: Derivatives Portfolio Risk
```xml
<riskData xmlns="http://www.isda.org/risk">
  <portfolio>
    <portfolioId>PF-DERIV-003</portfolioId>
    <counterparty>
      <lei>5493000F4ZG1KEXQJL62</lei>
      <name>Bridgewater Associates LP</name>
    </counterparty>
    <asOfDate>2024-12-24</asOfDate>
  </portfolio>
  <positions>
    <position>
      <instrumentId>CDS-ITRAXX-MAIN-S40</instrumentId>
      <instrumentType>CREDIT_DEFAULT_SWAP</instrumentType>
      <notionalAmount currency="EUR">50000000</notionalAmount>
      <marketValue currency="EUR">-125000</marketValue>
      <unrealizedPnL currency="EUR">-125000</unrealizedPnL>
      <cs01>2500</cs01>
      <creditSpread>85</creditSpread>
    </position>
    <position>
      <instrumentId>FX-FORWARD-EURUSD-3M</instrumentId>
      <instrumentType>FX_FORWARD</instrumentType>
      <notionalAmount currency="EUR">25000000</notionalAmount>
      <marketValue currency="EUR">75000</marketValue>
      <unrealizedPnL currency="EUR">75000</unrealizedPnL>
      <delta>1.0</delta>
      <gamma>0.0</gamma>
    </position>
  </positions>
  <riskMetrics>
    <baseDate>2024-12-24</baseDate>
    <currency>EUR</currency>
    <portfolioValue>-50000</portfolioValue>
  </riskMetrics>
</riskData>
```

### YAML Enrichment Rules
```yaml
name: "Risk Enrichment"
description: "Add risk metrics, exposure calculations, and margin requirements"
version: "1.0"

rules:
  - id: "var-calculation"
    type: "calculation-enrichment"
    condition: "#data.positions != null && #data.positions.position != null && #data.positions.position.marketValue != null"
    calculations:
      - field: "riskMetrics.var1Day99"
        expression: "#positions.position.marketValue * 0.025"
      - field: "riskMetrics.var10Day99"
        expression: "#riskMetrics.var1Day99 * T(java.lang.Math).sqrt(10)"

  - id: "volatility-lookup"
    type: "lookup-enrichment"
    condition: "#data.positions != null && #data.positions.position != null && #data.positions.position.instrumentId != null"
    lookup-config:
      lookup-key: "positions.position.instrumentId"
      lookup-dataset:
        type: "inline"
        key-field: "instrumentId"
        data:
          - instrumentId: "GB00B03MLX29"
            impliedVolatility: 0.28
            historicalVolatility: 0.25
          - instrumentId: "US0378331005"
            impliedVolatility: 0.32
            historicalVolatility: 0.30
          - instrumentId: "DE0007164600"
            impliedVolatility: 0.35
            historicalVolatility: 0.33
    field-mappings:
      - source-field: "impliedVolatility"
        target-field: "riskMetrics.impliedVolatility"
      - source-field: "historicalVolatility"
        target-field: "riskMetrics.historicalVolatility"

  - id: "counterparty-exposure-calculation"
    type: "calculation-enrichment"
    condition: "#data.counterparty != null && #data.counterparty.lei != null && #data.positions != null"
    calculations:
      - field: "exposure.grossExposure"
        expression: "#positions.position.marketValue"
      - field: "exposure.netExposure"
        expression: "#exposure.grossExposure - (#collateral != null && #collateral.heldAmount != null ? #collateral.heldAmount : 0)"

  - id: "credit-limits-lookup"
    type: "lookup-enrichment"
    condition: "#data.counterparty != null && #data.counterparty.lei != null"
    lookup-config:
      lookup-key: "counterparty.lei"
      lookup-dataset:
        type: "inline"
        key-field: "lei"
        data:
          - lei: "7LTWFZYICNSX8D621K86"
            creditLimit: 1000000000
          - lei: "8EE8DF3643E15DBFDA05"
            creditLimit: 750000000
          - lei: "784F5XWPLTWKTBV3E584"
            creditLimit: 500000000
    field-mappings:
      - source-field: "creditLimit"
        target-field: "exposure.creditLimit"

  - id: "utilization-ratio-calculation"
    type: "calculation-enrichment"
    condition: "#data.exposure != null && #data.exposure.netExposure != null && #data.exposure.creditLimit != null && #data.exposure.creditLimit > 0"
    calculations:
      - field: "exposure.utilizationRatio"
        expression: "#exposure.netExposure / #exposure.creditLimit"

  - id: "margin-parameters-lookup"
    type: "lookup-enrichment"
    condition: "#data.positions != null && #data.positions.position != null && #data.positions.position.instrumentId != null"
    lookup-config:
      lookup-key: "positions.position.instrumentId"
      lookup-dataset:
        type: "inline"
        key-field: "instrumentId"
        data:
          - instrumentId: "GB00B03MLX29"
            initialMarginRate: 0.15
          - instrumentId: "US0378331005"
            initialMarginRate: 0.12
          - instrumentId: "DE0007164600"
            initialMarginRate: 0.18
    field-mappings:
      - source-field: "initialMarginRate"
        target-field: "margin.initialMarginRate"

  - id: "margin-calculation"
    type: "calculation-enrichment"
    condition: "#data.positions != null && #data.positions.position != null && #data.margin != null && #data.margin.initialMarginRate != null"
    calculations:
      - field: "margin.initialMarginAmount"
        expression: "#positions.position.marketValue * #margin.initialMarginRate"
      - field: "margin.variationMargin"
        expression: "#positions.position.unrealizedPnL != null ? #positions.position.unrealizedPnL : 0"
      - field: "margin.variationMarginDirection"
        expression: "#margin.variationMargin > 0 ? 'RECEIVABLE' : 'PAYABLE'"

  - id: "collateral-eligibility-lookup"
    type: "lookup-enrichment"
    condition: "#data.positions != null && #data.positions.position != null && #data.positions.position.instrumentId != null"
    lookup-config:
      lookup-key: "positions.position.instrumentId"
      lookup-dataset:
        type: "inline"
        key-field: "instrumentId"
        data:
          - instrumentId: "GB00B03MLX29"
            eligible: true
            haircut: 0.08
            rating: "A+"
          - instrumentId: "US0378331005"
            eligible: true
            haircut: 0.06
            rating: "AA"
          - instrumentId: "DE0007164600"
            eligible: true
            haircut: 0.10
            rating: "A"
          - instrumentId: "JUNK_BOND_001"
            eligible: false
            haircut: 0.50
            rating: "CCC"
    field-mappings:
      - source-field: "eligible"
        target-field: "collateral.eligible"
      - source-field: "haircut"
        target-field: "collateral.haircut"
      - source-field: "rating"
        target-field: "collateral.rating"

  - id: "collateral-amount-calculation"
    type: "calculation-enrichment"
    condition: "#data.collateral != null && #data.collateral.haircut != null && #data.positions != null"
    calculations:
      - field: "collateral.eligibleAmount"
        expression: "#positions.position.marketValue * (1 - #collateral.haircut)"

  - id: "stress-test-calculations"
    type: "calculation-enrichment"
    condition: "#data.positions != null && #data.positions.position != null && #data.positions.position.marketValue != null"
    calculations:
      - field: "stressTest.marketCrash.scenario"
        expression: "#positions.position.marketValue * -0.30"
      - field: "stressTest.volatilityShock.scenario"
        expression: "#riskMetrics != null && #riskMetrics.var1Day99 != null ? #riskMetrics.var1Day99 * 2.5 : 0"
      - field: "stressTest.creditSpreadWidening.scenario"
        expression: "#positions.position.marketValue * -0.05"
      - field: "stressTest.riskLevel"
        expression: "#stressTest.marketCrash.scenario < -50000000 ? 'HIGH' : (#stressTest.marketCrash.scenario < -10000000 ? 'MEDIUM' : 'LOW')"
      - field: "stressTest.actionRequired"
        expression: "#stressTest.riskLevel == 'HIGH' ? 'IMMEDIATE_REVIEW' : (#stressTest.riskLevel == 'MEDIUM' ? 'MONITOR' : 'NONE')"

# Additional Risk Enrichment Examples

  - id: "multi-asset-risk-calculations"
    type: "calculation-enrichment"
    condition: "#data.positions != null && #data.positions.position != null"
    calculations:
      - field: "riskMetrics.equityDelta"
        expression: "#positions.position.instrumentType == 'EQUITY' && #positions.position.beta != null ? #positions.position.marketValue * #positions.position.beta : 0"
      - field: "riskMetrics.interestRateDV01"
        expression: "#positions.position.instrumentType == 'GOVERNMENT_BOND' && #positions.position.duration != null ? #positions.position.marketValue * #positions.position.duration * 0.0001 : (#positions.position.instrumentType == 'INTEREST_RATE_SWAP' && #positions.position.dv01 != null ? #positions.position.dv01 : 0)"
      - field: "riskMetrics.creditCS01"
        expression: "#positions.position.instrumentType == 'CREDIT_DEFAULT_SWAP' && #positions.position.cs01 != null ? #positions.position.cs01 : 0"
      - field: "riskMetrics.fxDelta"
        expression: "#positions.position.instrumentType == 'FX_FORWARD' && #positions.position.delta != null ? #positions.position.marketValue * #positions.position.delta : 0"

  - id: "portfolio-level-risk-aggregation"
    type: "calculation-enrichment"
    condition: "#data.riskMetrics != null && #data.riskMetrics.portfolioValue != null"
    calculations:
      - field: "portfolioRisk.totalVar1Day"
        expression: "#riskMetrics.portfolioValue * 0.02"  # 2% portfolio VaR
      - field: "portfolioRisk.leverageRatio"
        expression: "T(java.lang.Math).abs(#positions.position.notionalAmount != null ? #positions.position.notionalAmount : #positions.position.marketValue) / T(java.lang.Math).abs(#riskMetrics.portfolioValue)"
      - field: "portfolioRisk.concentrationRisk"
        expression: "T(java.lang.Math).abs(#positions.position.marketValue) / T(java.lang.Math).abs(#riskMetrics.portfolioValue)"
      - field: "portfolioRisk.riskCategory"
        expression: "#portfolioRisk.leverageRatio > 5.0 ? 'HIGH_LEVERAGE' : (#portfolioRisk.concentrationRisk > 0.25 ? 'HIGH_CONCENTRATION' : 'NORMAL')"

  - id: "regulatory-capital-calculations"
    type: "calculation-enrichment"
    condition: "#data.positions != null && #data.positions.position != null && #data.positions.position.marketValue != null"
    calculations:
      - field: "regulatoryCapital.riskWeightedAssets"
        expression: "#positions.position.instrumentType == 'EQUITY' ? #positions.position.marketValue * 1.0 : (#positions.position.instrumentType == 'GOVERNMENT_BOND' ? #positions.position.marketValue * 0.0 : (#positions.position.instrumentType == 'CREDIT_DEFAULT_SWAP' ? #positions.position.marketValue * 1.6 : #positions.position.marketValue * 0.8))"
      - field: "regulatoryCapital.capitalRequirement"
        expression: "#regulatoryCapital.riskWeightedAssets * 0.08"  # 8% capital requirement
      - field: "regulatoryCapital.leverageExposure"
        expression: "T(java.lang.Math).abs(#positions.position.notionalAmount != null ? #positions.position.notionalAmount : #positions.position.marketValue)"
      - field: "regulatoryCapital.leverageRatio"
        expression: "#regulatoryCapital.leverageExposure / 1000000000"  # Assuming 1B capital base

  - id: "counterparty-risk-metrics"
    type: "calculation-enrichment"
    condition: "#data.portfolio != null && #data.portfolio.counterparty != null && #data.positions != null"
    calculations:
      - field: "counterpartyRisk.currentExposure"
        expression: "T(java.lang.Math).max(#positions.position.marketValue, 0)"
      - field: "counterpartyRisk.potentialFutureExposure"
        expression: "#counterpartyRisk.currentExposure * 1.4"  # Add-on factor
      - field: "counterpartyRisk.exposureAtDefault"
        expression: "#counterpartyRisk.currentExposure + (#counterpartyRisk.potentialFutureExposure * 0.4)"
      - field: "counterpartyRisk.expectedLoss"
        expression: "#counterpartyRisk.exposureAtDefault * 0.45 * 0.02"  # LGD 45%, PD 2%

  - id: "market-risk-sensitivities"
    type: "lookup-enrichment"
    condition: "#data.positions != null && #data.positions.position != null && #data.positions.position.instrumentId != null"
    lookup-config:
      lookup-key: "positions.position.instrumentId"
      lookup-dataset:
        type: "inline"
        key-field: "instrumentId"
        data:
          - instrumentId: "GB00B03MLX29"
            equityVega: 0.15
            equityTheta: -0.02
            correlationFactor: 0.75
          - instrumentId: "US0378331005"
            equityVega: 0.18
            equityTheta: -0.01
            correlationFactor: 0.80
          - instrumentId: "US912828XG93"
            interestRateVega: 0.25
            convexity: 0.85
            correlationFactor: 0.95
          - instrumentId: "SWAP-USD-SOFR-5Y"
            interestRateVega: 0.30
            convexity: 1.20
            correlationFactor: 0.98
          - instrumentId: "CDS-ITRAXX-MAIN-S40"
            creditVega: 0.12
            recoveryRate: 0.40
            correlationFactor: 0.65
    field-mappings:
      - source-field: "equityVega"
        target-field: "sensitivities.equityVega"
      - source-field: "interestRateVega"
        target-field: "sensitivities.interestRateVega"
      - source-field: "creditVega"
        target-field: "sensitivities.creditVega"
      - source-field: "correlationFactor"
        target-field: "sensitivities.correlationFactor"
```
```

## 5. Settlement Enrichment

Settlement enrichment adds critical settlement processing information including dates, methods, priorities, and custodian details required for efficient trade settlement.

### Example Source XML (Settlement Instruction)
```xml
<settlementInstruction xmlns="http://www.iso20022.org/settlement">
  <instructionId>SI-20241224-001</instructionId>
  <tradeDetails>
    <tradeId>TRD-001-2024</tradeId>
    <tradeDate>2024-12-24</tradeDate>
    <valueDate>2024-12-26</valueDate>
  </tradeDetails>
  <security>
    <isin>GB00B03MLX29</isin>
    <quantity>10000</quantity>
    <price>2750.50</price>
    <currency>GBP</currency>
  </security>
  <counterparties>
    <deliverer>
      <lei>7LTWFZYICNSX8D621K86</lei>
      <account>CREST001234</account>
    </deliverer>
    <receiver>
      <lei>8EE8DF3643E15DBFDA05</lei>
      <account>CREST005678</account>
    </receiver>
  </counterparties>
  <market>
    <mic>XLON</mic>
    <country>GB</country>
  </market>
</settlementInstruction>
```

### YAML Enrichment Rules
```yaml
metadata:
  name: "Settlement Enrichment"
  description: "Add settlement processing information and routing details"
  version: "1.0.0"
  type: "rule-config"

enrichments:
  - id: "settlement-cycle-lookup"
    type: "lookup-enrichment"
    condition: "#data.market != null && #data.market.country != null && #data.security != null"
    lookup-config:
      lookup-key: "#market.country + '_EQUITY'"
      lookup-dataset:
        type: "inline"
        key-field: "key"
        data:
          - key: "GB_EQUITY"
            cycle: "T+2"
            cycleDays: 2
          - key: "US_EQUITY"
            cycle: "T+1"
            cycleDays: 1
          - key: "DE_EQUITY"
            cycle: "T+2"
            cycleDays: 2
          - key: "GB_BOND"
            cycle: "T+1"
            cycleDays: 1
          - key: "US_BOND"
            cycle: "T+1"
            cycleDays: 1
    field-mappings:
      - source-field: "cycle"
        target-field: "settlement.cycle"
      - source-field: "cycleDays"
        target-field: "settlement.cycleDays"

  - id: "settlement-date-calculation"
    type: "calculation-enrichment"
    condition: "#data.tradeDetails != null && #data.tradeDetails.tradeDate != null && #data.settlement != null && #data.settlement.cycleDays != null"
    calculations:
      - field: "settlement.settlementDate"
        expression: "#tradeDetails.tradeDate.plusDays(#settlement.cycleDays)"

  - id: "settlement-system-lookup"
    type: "lookup-enrichment"
    condition: "#data.market != null && #data.market.country != null && #data.security != null && #data.security.isin != null"
    lookup-config:
      lookup-key: "#market.country + '_' + #security.isin.substring(0, 2)"
      lookup-dataset:
        type: "inline"
        key-field: "key"
        data:
          - key: "GB_GB"
            system: "CREST"
            method: "DVP"
            systemBIC: "CRSTGB22"
          - key: "US_US"
            system: "DTC"
            method: "DVP"
            systemBIC: "DTCYUS33"
          - key: "DE_DE"
            system: "CLEARSTREAM"
            method: "DVP"
            systemBIC: "DAKVDEFF"
          - key: "FR_FR"
            system: "EUROCLEAR_FRANCE"
            method: "DVP"
            systemBIC: "SICVFRPP"
    field-mappings:
      - source-field: "system"
        target-field: "settlement.system"
      - source-field: "method"
        target-field: "settlement.method"
      - source-field: "systemBIC"
        target-field: "settlement.systemBIC"

  - id: "trade-value-calculation"
    type: "calculation-enrichment"
    condition: "#data.security != null && #data.security.quantity != null && #data.security.price != null"
    calculations:
      - field: "settlement.tradeValue"
        expression: "#security.quantity * #security.price"

  - id: "settlement-priority-assignment"
    type: "calculation-enrichment"
    condition: "#data.settlement != null && #data.settlement.tradeValue != null"
    calculations:
      - field: "settlement.priority"
        expression: "#settlement.tradeValue > 100000000 ? 'HIGH' : (#settlement.tradeValue > 10000000 ? 'MEDIUM' : 'NORMAL')"
      - field: "settlement.priorityCode"
        expression: "#settlement.tradeValue > 100000000 ? 1 : (#settlement.tradeValue > 10000000 ? 2 : 3)"

  - id: "client-priority-lookup"
    type: "lookup-enrichment"
    condition: "#data.counterparties != null && #data.counterparties.deliverer != null && #data.counterparties.deliverer.lei != null"
    lookup-config:
      lookup-key: "counterparties.deliverer.lei"
      lookup-dataset:
        type: "inline"
        key-field: "lei"
        data:
          - lei: "7LTWFZYICNSX8D621K86"
            clientPriority: "TIER_1"
          - lei: "8EE8DF3643E15DBFDA05"
            clientPriority: "TIER_1"
          - lei: "784F5XWPLTWKTBV3E584"
            clientPriority: "TIER_2"
    field-mappings:
      - source-field: "clientPriority"
        target-field: "settlement.clientPriority"

  - name: "Custodian_Information_Enrichment"
    description: "Add custodian and sub-custodian details"
    condition: "counterparties.deliverer.account != null"
    actions:
      - type: "lookup"
        source: "custodian_network"
        key: "counterparties.deliverer.account + '_' + market.country"
        targets:
          - field: "custodian.globalCustodian"
            mapping:
              "CREST001234_GB": "State Street Bank"
              "DTC567890_US": "Bank of New York Mellon"
              "CLEARSTREAM123_DE": "Deutsche Bank AG"
          - field: "custodian.globalCustodianBIC"
            mapping:
              "CREST001234_GB": "SSBTGB2L"
              "DTC567890_US": "IRVTUS3N"
              "CLEARSTREAM123_DE": "DEUTDEFF"
          - field: "custodian.subCustodian"
            mapping:
              "CREST001234_GB": "Euroclear UK & Ireland"
              "DTC567890_US": "The Depository Trust Company"
              "CLEARSTREAM123_DE": "Clearstream Banking AG"

  - name: "Depository_Information_Enrichment"
    description: "Add central securities depository information"
    condition: "settlement.system != null"
    actions:
      - type: "lookup"
        source: "csd_directory"
        key: "settlement.system"
        targets:
          - field: "depository.csdName"
            mapping:
              "CREST": "Euroclear UK & Ireland Limited"
              "DTC": "The Depository Trust Company"
              "CLEARSTREAM": "Clearstream Banking AG"
              "EUROCLEAR_FRANCE": "Euroclear France"
          - field: "depository.csdLEI"
            mapping:
              "CREST": "213800WSGIIZCXF1P572"
              "DTC": "549300DT6TBKBACAQJ13"
              "CLEARSTREAM": "529900T8BM49AURSDO55"
              "EUROCLEAR_FRANCE": "969500UP76J52A9OXU27"
          - field: "depository.operatingHours"
            mapping:
              "CREST": "06:00-18:00 GMT"
              "DTC": "08:00-16:00 EST"
              "CLEARSTREAM": "07:00-18:00 CET"
              "EUROCLEAR_FRANCE": "07:00-18:00 CET"
          - field: "depository.cutoffTime"
            mapping:
              "CREST": "16:00 GMT"
              "DTC": "15:00 EST"
              "CLEARSTREAM": "17:00 CET"
              "EUROCLEAR_FRANCE": "17:00 CET"
```

## 6. Fee and Commission Enrichment

Fee and commission enrichment calculates and adds all applicable fees, taxes, and charges associated with trade execution and settlement.

### Example Source XML (Fee Calculation Data)
```xml
<feeCalculation xmlns="http://www.iso20022.org/fees">
  <tradeReference>
    <tradeId>TRD-001-2024</tradeId>
    <executionVenue>XLON</executionVenue>
    <tradeValue currency="GBP">27505000</tradeValue>
  </tradeReference>
  <participants>
    <executingBroker>
      <lei>7LTWFZYICNSX8D621K86</lei>
      <membershipType>FULL_MEMBER</membershipType>
    </executingBroker>
    <clearingMember>
      <lei>7LTWFZYICNSX8D621K86</lei>
      <clearingHouse>LCH_LIMITED</clearingHouse>
    </clearingMember>
    <custodian>
      <bic>SSBTGB2L</bic>
      <serviceTier>PREMIUM</serviceTier>
    </custodian>
  </participants>
  <instrument>
    <isin>GB00B03MLX29</isin>
    <assetClass>EQUITY</assetClass>
    <jurisdiction>GB</jurisdiction>
  </instrument>
</feeCalculation>
```

### YAML Enrichment Rules
```yaml
metadata:
  name: "Fee and Commission Enrichment"
  description: "Calculate and add all applicable fees, commissions, and taxes"
  version: "1.0.0"
  type: "rule-config"

enrichments:
  - id: "broker-commission-lookup"
    type: "lookup-enrichment"
    condition: "#data.tradeReference != null && #data.tradeReference.tradeValue != null && #data.executingBroker != null"
    lookup-config:
      lookup-key: "#executingBroker.lei + '_TIER_1'"  # Simplified for example
      lookup-dataset:
        type: "inline"
        key-field: "key"
        data:
          - key: "7LTWFZYICNSX8D621K86_TIER_1"
            brokerCommissionRate: 0.0008  # 8 bps
            minimumCommission: 25.00
            maximumCommission: 5000.00
          - key: "7LTWFZYICNSX8D621K86_TIER_2"
            brokerCommissionRate: 0.0012  # 12 bps
            minimumCommission: 25.00
            maximumCommission: 5000.00
          - key: "8EE8DF3643E15DBFDA05_TIER_1"
            brokerCommissionRate: 0.0010  # 10 bps
            minimumCommission: 50.00
            maximumCommission: 7500.00
          - key: "8EE8DF3643E15DBFDA05_TIER_2"
            brokerCommissionRate: 0.0015  # 15 bps
            minimumCommission: 50.00
            maximumCommission: 7500.00
    field-mappings:
      - source-field: "brokerCommissionRate"
        target-field: "fees.brokerCommissionRate"
      - source-field: "minimumCommission"
        target-field: "fees.minimumCommission"
      - source-field: "maximumCommission"
        target-field: "fees.maximumCommission"

  - id: "broker-commission-calculation"
    type: "calculation-enrichment"
    condition: "#data.fees != null && #data.fees.brokerCommissionRate != null && #data.tradeReference != null"
    calculations:
      - field: "fees.brokerCommissionBase"
        expression: "#tradeReference.tradeValue * #fees.brokerCommissionRate"
      - field: "fees.brokerCommission"
        expression: "T(java.lang.Math).max(T(java.lang.Math).min(#fees.brokerCommissionBase, #fees.maximumCommission), #fees.minimumCommission)"

  - name: "Exchange_Fee_Calculation"
    description: "Calculate exchange-specific trading fees"
    condition: "tradeReference.executionVenue != null"
    actions:
      - type: "lookup"
        source: "exchange_fee_schedule"
        key: "tradeReference.executionVenue + '_' + instrument.assetClass"
        targets:
          - field: "fees.exchangeFeeRate"
            mapping:
              "XLON_EQUITY": "0.000045"  # 0.45 bps
              "XNYS_EQUITY": "0.000030"  # 0.30 bps
              "XNAS_EQUITY": "0.000025"  # 0.25 bps
          - field: "fees.exchangeMinimumFee"
            mapping:
              "XLON_EQUITY": "1.00"
              "XNYS_EQUITY": "0.50"
              "XNAS_EQUITY": "0.35"
      - type: "calculate"
        field: "fees.exchangeFee"
        formula: "max(tradeReference.tradeValue * fees.exchangeFeeRate, fees.exchangeMinimumFee)"

  - name: "Clearing_Fee_Calculation"
    description: "Calculate clearing house fees"
    condition: "participants.clearingMember.clearingHouse != null"
    actions:
      - type: "lookup"
        source: "clearing_fee_schedule"
        key: "participants.clearingMember.clearingHouse + '_' + instrument.assetClass"
        targets:
          - field: "fees.clearingFeeRate"
            mapping:
              "LCH_LIMITED_EQUITY": "0.000020"  # 0.20 bps
              "CME_CLEARING_EQUITY": "0.000015"  # 0.15 bps
              "EUREX_CLEARING_EQUITY": "0.000025"  # 0.25 bps
      - type: "calculate"
        field: "fees.clearingFee"
        formula: "tradeReference.tradeValue * fees.clearingFeeRate"
      - type: "lookup"
        source: "clearing_member_discounts"
        key: "participants.clearingMember.lei"
        targets:
          - field: "fees.clearingDiscount"
            mapping:
              "7LTWFZYICNSX8D621K86": "0.10"  # 10% discount
              "8EE8DF3643E15DBFDA05": "0.15"  # 15% discount
              "784F5XWPLTWKTBV3E584": "0.05"  # 5% discount
      - type: "calculate"
        field: "fees.clearingFeeNet"
        formula: "fees.clearingFee * (1 - fees.clearingDiscount)"

  - name: "Custody_Fee_Calculation"
    description: "Calculate custody and settlement fees"
    condition: "participants.custodian.bic != null"
    actions:
      - type: "lookup"
        source: "custody_fee_schedule"
        key: "participants.custodian.bic + '_' + participants.custodian.serviceTier"
        targets:
          - field: "fees.custodyFeeRate"
            mapping:
              "SSBTGB2L_PREMIUM": "0.000010"  # 0.10 bps
              "SSBTGB2L_STANDARD": "0.000015"  # 0.15 bps
              "IRVTUS3N_PREMIUM": "0.000008"  # 0.08 bps
              "IRVTUS3N_STANDARD": "0.000012"  # 0.12 bps
      - type: "calculate"
        field: "fees.custodyFee"
        formula: "tradeReference.tradeValue * fees.custodyFeeRate"
      - type: "set"
        field: "fees.settlementFee"
        value: "5.00"  # Flat settlement fee

  - name: "Transaction_Tax_Calculation"
    description: "Calculate applicable transaction taxes"
    condition: "instrument.jurisdiction != null"
    actions:
      - type: "lookup"
        source: "transaction_tax_rates"
        key: "instrument.jurisdiction + '_' + instrument.assetClass"
        targets:
          - field: "taxes.stampDutyRate"
            mapping:
              "GB_EQUITY": "0.005"  # 0.5% UK stamp duty
              "FR_EQUITY": "0.003"  # 0.3% French FTT
              "DE_EQUITY": "0.000"  # No transaction tax
              "US_EQUITY": "0.000"  # No federal transaction tax
      - type: "conditional"
        conditions:
          - if: "taxes.stampDutyRate > 0"
            then:
              - calculate:
                  field: "taxes.transactionTax"
                  formula: "tradeReference.tradeValue * taxes.stampDutyRate"
              - set_field: "taxes.taxType"
                value: "STAMP_DUTY"
          - else:
              - set_field: "taxes.transactionTax"
                value: "0"
              - set_field: "taxes.taxType"
                value: "NONE"

  - name: "Total_Fee_Summary"
    description: "Calculate total fees and charges"
    condition: "fees.brokerCommission != null"
    actions:
      - type: "calculate"
        field: "fees.totalFees"
        formula: "fees.brokerCommission + fees.exchangeFee + fees.clearingFeeNet + fees.custodyFee + fees.settlementFee"
      - type: "calculate"
        field: "fees.totalCharges"
        formula: "fees.totalFees + taxes.transactionTax"
      - type: "calculate"
        field: "fees.netSettlementAmount"
        formula: "tradeReference.tradeValue + fees.totalCharges"
      - type: "calculate"
        field: "fees.feeAsPercentage"
        formula: "(fees.totalCharges / tradeReference.tradeValue) * 100"
```

## 7. Corporate Action Enrichment

Corporate action enrichment adds dividend, split, and other corporate event information that affects settlement processing.

### Example YAML Rules (Corporate Actions)
```yaml
metadata:
  name: "Corporate Action Enrichment"
  version: "1.0.0"
  type: "rule-config"

enrichments:
  - id: "dividend-information"
    type: "lookup-enrichment"
    condition: "#data.security != null && #data.security.isin != null"
    lookup-config:
      lookup-key: "security.isin"
      lookup-dataset:
        type: "inline"
        key-field: "isin"
        data:
          - isin: "GB00B03MLX29"
            exDividendDate: "2024-12-15"
            recordDate: "2024-12-16"
            paymentDate: "2024-12-30"
            dividendAmount: 0.47
    field-mappings:
      - source-field: "exDividendDate"
        target-field: "corporateAction.exDividendDate"
      - source-field: "dividendAmount"
        target-field: "corporateAction.dividendAmount"
```

## 8. Pricing and Valuation Enrichment

Pricing enrichment adds current market values, yields, and valuation metrics for accurate settlement amounts.

### Example YAML Rules (Pricing)
```yaml
metadata:
  name: "Pricing and Valuation Enrichment"
  version: "1.0.0"
  type: "rule-config"

enrichments:
  - id: "mark-to-market-pricing"
    type: "lookup-enrichment"
    condition: "#data.security != null && #data.security.isin != null"
    lookup-config:
      lookup-key: "security.isin"
      lookup-dataset:
        type: "inline"
        key-field: "isin"
        data:
          - isin: "GB00B03MLX29"
            currentPrice: 2750.50
            priceSource: "BLOOMBERG"
            bidPrice: 2749.00
            askPrice: 2752.00
    field-mappings:
      - source-field: "currentPrice"
        target-field: "pricing.currentPrice"
      - source-field: "priceSource"
        target-field: "pricing.priceSource"
```

## 9. Compliance Enrichment

Compliance enrichment adds AML/KYC status, sanctions screening, and regulatory compliance flags.

### Example YAML Rules (Compliance)
```yaml
metadata:
  name: "Compliance Enrichment"
  version: "1.0.0"
  type: "rule-config"

enrichments:
  - id: "aml-kyc-screening"
    type: "lookup-enrichment"
    condition: "#data.counterparty != null && #data.counterparty.lei != null"
    lookup-config:
      lookup-key: "counterparty.lei"
      lookup-dataset:
        type: "inline"
        key-field: "lei"
        data:
          - lei: "7LTWFZYICNSX8D621K86"
            amlStatus: "CLEARED"
            kycStatus: "VERIFIED"
            sanctionsScreening: "PASSED"
    field-mappings:
      - source-field: "amlStatus"
        target-field: "compliance.amlStatus"
      - source-field: "kycStatus"
        target-field: "compliance.kycStatus"
```

## 10. Operational Enrichment

Operational enrichment adds processing sequence, STP eligibility, and exception handling information.

### Example YAML Rules (Operations)
```yaml
metadata:
  name: "Operational Enrichment"
  version: "1.0.0"
  type: "rule-config"

rules:
  - id: "stp-eligibility-check"
    name: "STP Eligibility Validation"
    condition: "#data.counterparty != null && #data.counterparty.lei != null && #data.security != null && #data.security.isin != null && #data.settlement != null"
    message: "Trade is eligible for straight-through processing"
    severity: "INFO"

enrichments:
  - id: "stp-status-calculation"
    type: "calculation-enrichment"
    condition: "#data.counterparty != null && #data.security != null"
    calculations:
      - field: "operations.stpEligible"
        expression: "#counterparty.lei != null && #security.isin != null"
```

## 11. Accounting Enrichment

Accounting enrichment adds general ledger codes, cost basis information, and P&L attribution.

### Example YAML Rules (Accounting)
```yaml
metadata:
  name: "Accounting Enrichment"
  version: "1.0.0"
  type: "rule-config"

enrichments:
  - id: "gl-code-assignment"
    type: "lookup-enrichment"
    condition: "#data.instrument != null && #data.instrument.assetClass != null && #data.trade != null && #data.trade.direction != null"
    lookup-config:
      lookup-key: "#instrument.assetClass + '_' + #trade.direction"
      lookup-dataset:
        type: "inline"
        key-field: "key"
        data:
          - key: "EQUITY_BUY"
            glCode: "1100-EQUITY-LONG"
            costCenter: "TRADING-DESK-1"
            profitCenter: "EQUITY-TRADING"
          - key: "EQUITY_SELL"
            glCode: "1100-EQUITY-SHORT"
            costCenter: "TRADING-DESK-1"
            profitCenter: "EQUITY-TRADING"
    field-mappings:
      - source-field: "glCode"
        target-field: "accounting.glCode"
      - source-field: "costCenter"
        target-field: "accounting.costCenter"
```

## 12. Market Data Enrichment

Market data enrichment adds benchmark information, liquidity metrics, and market sentiment indicators.

### Example YAML Rules (Market Data)
```yaml
metadata:
  name: "Market Data Enrichment"
  version: "1.0.0"
  type: "rule-config"

enrichments:
  - id: "benchmark-information"
    type: "lookup-enrichment"
    condition: "#data.security != null && #data.security.isin != null"
    lookup-config:
      lookup-key: "security.isin"
      lookup-dataset:
        type: "inline"
        key-field: "isin"
        data:
          - isin: "GB00B03MLX29"
            primaryBenchmark: "FTSE_100"
            sector: "ENERGY"
            averageDailyVolume: 15000000
            marketCap: 198000000000
    field-mappings:
      - source-field: "primaryBenchmark"
        target-field: "marketData.primaryBenchmark"
      - source-field: "sector"
        target-field: "marketData.sector"
```

## Implementation Guidelines

### XML Message Standards
The examples in this document follow industry standards:
- **ISO 20022**: For payment and securities settlement messages
- **FIX Protocol**: For trade execution and confirmation
- **FpML**: For derivatives and complex financial products
- **SWIFT MT/MX**: For cross-border settlement instructions

### YAML Rule Engine Benefits
- **Flexibility**: Easy to modify rules without code changes
- **Maintainability**: Clear, readable rule definitions
- **Scalability**: Support for complex conditional logic
- **Auditability**: Complete audit trail of enrichment decisions
- **Performance**: Efficient rule execution with caching

### Best Practices
1. **Data Quality**: Validate source data before enrichment
2. **Error Handling**: Implement comprehensive error handling and fallback mechanisms
3. **Performance**: Use caching and batch processing for high-volume scenarios
4. **Monitoring**: Track enrichment success rates and processing times
5. **Compliance**: Ensure all enrichment follows regulatory requirements

This comprehensive framework covers all major categories of enrichment essential for financial services post-trade settlement, providing the foundation for efficient, compliant, and risk-managed settlement processing.