package dev.mars.rulesengine.core.service.transform;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Implementation of FieldTransformerActionBuilder functionality.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
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