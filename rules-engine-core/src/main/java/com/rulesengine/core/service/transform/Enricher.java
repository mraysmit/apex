package com.rulesengine.core.service.transform;

import com.rulesengine.core.service.NamedService;

/**
 * Interface for enrichment services.
 */
public interface Enricher extends NamedService {
    Object enrich(Object value);
}