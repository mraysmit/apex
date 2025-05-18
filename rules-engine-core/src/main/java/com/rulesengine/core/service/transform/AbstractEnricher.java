package com.rulesengine.core.service.transform;

/**
 * Abstract base class for enrichers.
 * Provides common functionality for all enrichers.
 * 
 * @param <T> The type of object this enricher can enrich
 */
public abstract class AbstractEnricher<T> implements Enricher<T> {
    private final String name;
    private final Class<T> type;
    
    /**
     * Create a new AbstractEnricher with the specified name and type.
     * 
     * @param name The name of the enricher
     * @param type The class of objects this enricher can enrich
     */
    protected AbstractEnricher(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * Get the type of objects this enricher can enrich.
     * 
     * @return The class of objects this enricher can enrich
     */
    public Class<T> getType() {
        return type;
    }
}