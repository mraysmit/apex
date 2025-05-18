package com.rulesengine.core.service.transform;

import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import java.util.logging.Logger;

/**
 * Service for transformation operations.
 */
public class TransformationService {
    private static final Logger LOGGER = Logger.getLogger(TransformationService.class.getName());
    private final LookupServiceRegistry registry;

    /**
     * Create a new TransformationService with the specified registry.
     * 
     * @param registry The lookup service registry
     */
    public TransformationService(LookupServiceRegistry registry) {
        this.registry = registry;
        LOGGER.info("TransformationService initialized");
    }

    /**
     * Transform a value using the specified transformer with type safety.
     * 
     * @param <T> The type of the value to transform
     * @param transformerName The name of the transformer to use
     * @param value The value to transform
     * @return The transformed value, or the original value if the transformer is not found
     */
    @SuppressWarnings("unchecked")
    public <T> T transform(String transformerName, T value) {
        LOGGER.fine("Transforming value using transformer: " + transformerName);

        // Get the transformer from the registry
        Transformer<?> transformer = registry.getService(transformerName, Transformer.class);
        if (transformer == null) {
            LOGGER.warning("Transformer not found: " + transformerName);
            return value;
        }

        // Check if the transformer can handle this type
        if (value != null && !transformer.getType().isInstance(value)) {
            LOGGER.warning("Transformer " + transformerName + " cannot handle type: " + value.getClass().getName());
            return value;
        }

        // Call the transformer with the appropriate type
        Transformer<T> typedTransformer = (Transformer<T>) transformer;
        return typedTransformer.transform(value);
    }
}
