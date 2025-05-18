package com.rulesengine.core.service.transform;

import com.rulesengine.core.service.common.NamedService;

/**
 * Interface for enrichment services.
 * @param <T> The type of object this enricher can enrich
 */
public interface Enricher<T> extends NamedService {
    /**
     * Enrich a value of type T.
     * 
     * @param value The value to enrich
     * @return The enriched value
     */
    T enrich(T value);

    /**
     * Get the type of objects this enricher can enrich.
     * 
     * @return The class of objects this enricher can enrich
     */
    Class<T> getType();
}
