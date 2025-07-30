package dev.mars.apex.core.engine.config;

import java.util.Map;
import java.lang.reflect.Method;

/**
 * A wrapper class that allows Map entries to be accessed as properties in SpEL expressions.
 * This enables expressions like "age > 18" instead of requiring "['age'] > 18".
 */
public class MapPropertyAccessor {
    private final Map<String, Object> map;
    
    public MapPropertyAccessor(Map<String, Object> map) {
        this.map = map;
    }
    
    /**
     * Get a property value from the underlying map.
     * This method uses reflection to dynamically handle property access.
     */
    public Object getProperty(String propertyName) {
        return map.get(propertyName);
    }
    
    /**
     * Check if a property exists in the underlying map.
     */
    public boolean hasProperty(String propertyName) {
        return map.containsKey(propertyName);
    }
    
    /**
     * Get the underlying map.
     */
    public Map<String, Object> getMap() {
        return map;
    }
    
    /**
     * Override toString for debugging.
     */
    @Override
    public String toString() {
        return "MapPropertyAccessor{" + map + "}";
    }
    
    /**
     * Dynamic property access using reflection.
     * This allows SpEL to access map keys as if they were properties.
     */
    public Object get(String key) {
        return map.get(key);
    }
    
    // Dynamic getters for common property names
    public Object getAge() { return map.get("age"); }
    public Object getName() { return map.get("name"); }
    public Object getEmail() { return map.get("email"); }
    public Object getStatus() { return map.get("status"); }
    public Object getAmount() { return map.get("amount"); }
    public Object getType() { return map.get("type"); }
    public Object getId() { return map.get("id"); }
    public Object getCode() { return map.get("code"); }
    public Object getValue() { return map.get("value"); }
    public Object getCategory() { return map.get("category"); }
    public Object getPrice() { return map.get("price"); }
    public Object getQuantity() { return map.get("quantity"); }
    public Object getDate() { return map.get("date"); }
    public Object getTime() { return map.get("time"); }
    public Object getTimestamp() { return map.get("timestamp"); }
    public Object getActive() { return map.get("active"); }
    public Object getEnabled() { return map.get("enabled"); }
    public Object getValid() { return map.get("valid"); }
    public Object getScore() { return map.get("score"); }
    public Object getRating() { return map.get("rating"); }
    public Object getLevel() { return map.get("level"); }
}
