package dev.mars.apex.demo.bootstrap.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Underlying Asset model for OTC Options.
 * Represents the commodity or asset that the option is based on.
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
