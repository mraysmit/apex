package com.rulesengine.core.service.lookup;

import com.rulesengine.core.service.transform.GenericTransformer;
import com.rulesengine.core.service.validation.Validator;

/**
 * Implementation that combines validation and transformation.
 */
public class CompositeLookup implements IDataLookup {
    private String name;
    private Validator validator;
    private GenericTransformer<Object> transformer;

    public CompositeLookup(String name) {
        this.name = name;
    }

    public CompositeLookup withValidator(Validator validator) {
        this.validator = validator;
        return this;
    }

    public CompositeLookup withTransformer(GenericTransformer<Object> transformer) {
        this.transformer = transformer;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean validate(Object value) {
        return validator != null ? validator.validate(value) : true;
    }

    /**
     * @deprecated Use {@link #transform(Object)} instead.
     */
    @Deprecated
    @Override
    public Object enrich(Object value) {
        // Delegate to transform method
        return transform(value);
    }

    @Override
    public Object transform(Object value) {
        return transformer != null ? transformer.transform(value) : value;
    }

    @Override
    public Class<Object> getType() {
        return Object.class;
    }
}
