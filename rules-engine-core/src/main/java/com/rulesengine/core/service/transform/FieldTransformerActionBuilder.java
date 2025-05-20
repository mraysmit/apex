package com.rulesengine.core.service.transform;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FieldTransformerActionBuilder<T> {
    private String fieldName;
    private Function<T, Object> fieldValueExtractor;
    private BiFunction<Object, Map<String, Object>, Object> fieldValueTransformer;
    private BiConsumer<T, Object> fieldValueSetter;
    
    public FieldTransformerActionBuilder<T> withFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }
    
    public FieldTransformerActionBuilder<T> withFieldValueExtractor(Function<T, Object> fieldValueExtractor) {
        this.fieldValueExtractor = fieldValueExtractor;
        return this;
    }
    
    public FieldTransformerActionBuilder<T> withFieldValueTransformer(BiFunction<Object, Map<String, Object>, Object> fieldValueTransformer) {
        this.fieldValueTransformer = fieldValueTransformer;
        return this;
    }
    
    public FieldTransformerActionBuilder<T> withFieldValueSetter(BiConsumer<T, Object> fieldValueSetter) {
        this.fieldValueSetter = fieldValueSetter;
        return this;
    }
    
    public FieldTransformerAction<T> build() {
        return new FieldTransformerAction<>(fieldName, fieldValueExtractor, fieldValueTransformer, fieldValueSetter);
    }
}