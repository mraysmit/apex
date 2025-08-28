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
 * Underlying Asset model for OTC Options.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public class UnderlyingAsset {
    
    @JsonProperty("commodity")
    private String commodity;
    
    @JsonProperty("unit")
    private String unit;
    
    // Default constructor
    public UnderlyingAsset() {}
    
    // Constructor
    public UnderlyingAsset(String commodity, String unit) {
        this.commodity = commodity;
        this.unit = unit;
    }
    
    // Getters and Setters
    public String getCommodity() {
        return commodity;
    }
    
    public void setCommodity(String commodity) {
        this.commodity = commodity;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    @Override
    public String toString() {
        return "UnderlyingAsset{" +
                "commodity='" + commodity + '\'' +
                ", unit='" + unit + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        UnderlyingAsset that = (UnderlyingAsset) o;
        
        if (commodity != null ? !commodity.equals(that.commodity) : that.commodity != null) return false;
        return unit != null ? unit.equals(that.unit) : that.unit == null;
    }
    
    @Override
    public int hashCode() {
        int result = commodity != null ? commodity.hashCode() : 0;
        result = 31 * result + (unit != null ? unit.hashCode() : 0);
        return result;
    }
}
