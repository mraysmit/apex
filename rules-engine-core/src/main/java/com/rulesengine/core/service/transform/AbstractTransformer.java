package com.rulesengine.core.service.transform;

/**
 * Abstract base class for transformers.
 * Provides common functionality for all transformers.
 * 
 * @param <T> The type of object this transformer can transform
 */
public abstract class AbstractTransformer<T> implements Transformer<T> {
    private final String name;
    private final Class<T> type;
    
    /**
     * Create a new AbstractTransformer with the specified name and type.
     * 
     * @param name The name of the transformer
     * @param type The class of objects this transformer can transform
     */
    protected AbstractTransformer(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * Get the type of objects this transformer can transform.
     * 
     * @return The class of objects this transformer can transform
     */
    public Class<T> getType() {
        return type;
    }
}