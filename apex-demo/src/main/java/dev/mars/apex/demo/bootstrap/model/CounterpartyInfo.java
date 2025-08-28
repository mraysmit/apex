package dev.mars.apex.demo.bootstrap.model;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Counterparty Information model for enrichment from PostgreSQL database.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public class CounterpartyInfo {
    
    @JsonProperty("partyId")
    private String partyId;
    
    @JsonProperty("legalName")
    private String legalName;
    
    @JsonProperty("creditRating")
    private String creditRating;
    
    @JsonProperty("lei")
    private String lei; // Legal Entity Identifier
    
    @JsonProperty("jurisdiction")
    private String jurisdiction;
    
    @JsonProperty("settlementPreference")
    private String settlementPreference;
    
    // Default constructor
    public CounterpartyInfo() {}
    
    // Constructor
    public CounterpartyInfo(String partyId, String legalName, String creditRating, 
                           String lei, String jurisdiction, String settlementPreference) {
        this.partyId = partyId;
        this.legalName = legalName;
        this.creditRating = creditRating;
        this.lei = lei;
        this.jurisdiction = jurisdiction;
        this.settlementPreference = settlementPreference;
    }
    
    // Getters and Setters
    public String getPartyId() {
        return partyId;
    }
    
    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }
    
    public String getLegalName() {
        return legalName;
    }
    
    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }
    
    public String getCreditRating() {
        return creditRating;
    }
    
    public void setCreditRating(String creditRating) {
        this.creditRating = creditRating;
    }
    
    public String getLei() {
        return lei;
    }
    
    public void setLei(String lei) {
        this.lei = lei;
    }
    
    public String getJurisdiction() {
        return jurisdiction;
    }
    
    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }
    
    public String getSettlementPreference() {
        return settlementPreference;
    }
    
    public void setSettlementPreference(String settlementPreference) {
        this.settlementPreference = settlementPreference;
    }
    
    @Override
    public String toString() {
        return "CounterpartyInfo{" +
                "partyId='" + partyId + '\'' +
                ", legalName='" + legalName + '\'' +
                ", creditRating='" + creditRating + '\'' +
                ", lei='" + lei + '\'' +
                ", jurisdiction='" + jurisdiction + '\'' +
                ", settlementPreference='" + settlementPreference + '\'' +
                '}';
    }
}
