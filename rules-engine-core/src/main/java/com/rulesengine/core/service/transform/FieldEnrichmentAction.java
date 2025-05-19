package com.rulesengine.core.service.transform;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FieldEnrichmentAction<T> {
    private final String fieldName;
    private final Function<T, Object> fieldValueExtractor;
    private final BiFunction<Object, Map<String, Object>, Object> fieldValueTransformer;
    private final BiConsumer<T, Object> fieldValueSetter;
    
    public FieldEnrichmentAction(String fieldName, Function<T, Object> fieldValueExtractor,
                                BiFunction<Object, Map<String, Object>, Object> fieldValueTransformer,
                                BiConsumer<T, Object> fieldValueSetter) {
        this.fieldName = fieldName;
        this.fieldValueExtractor = fieldValueExtractor;
        this.fieldValueTransformer = fieldValueTransformer;
        this.fieldValueSetter = fieldValueSetter;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public Function<T, Object> getFieldValueExtractor() {
        return fieldValueExtractor;
    }
    
    public BiFunction<Object, Map<String, Object>, Object> getFieldValueTransformer() {
        return fieldValueTransformer;
    }
    
    public BiConsumer<T, Object> getFieldValueSetter() {
        return fieldValueSetter;
    }
}