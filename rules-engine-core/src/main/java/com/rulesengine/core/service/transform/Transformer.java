package com.rulesengine.core.service.transform;

import com.rulesengine.core.service.common.NamedService;

/**
 * Interface for transformation services.
 * @param <T> The type of object this transformer can transform
 */
public interface Transformer<T> extends NamedService {
    /**
     * Transform a value of type T.
     * 
     * @param value The value to transform
     * @return The transformed value
     */
    T transform(T value);

    /**
     * Get the type of objects this transformer can transform.
     * 
     * @return The class of objects this transformer can transform
     */
    Class<T> getType();
}
