package dev.mars.apex.core.service.lookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Implementation of LookupService functionality.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class LookupService implements IDataLookup {
    private List<String> lookupValues;
    private String name;
    private Map<String, Object> enrichmentData;
    private Function<Object, Object> transformationFunction;

    public LookupService(String name, List<String> lookupValues) {
        this.name = name;
        this.lookupValues = lookupValues;
        this.enrichmentData = new HashMap<>();
        this.transformationFunction = value -> value; // Identity function by default
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean validate(Object value) {
        if (value instanceof String) {
            return lookupValues.contains(value);
        }
        return false;
    }

    @Override
    public Object transform(Object value) {
        // First apply enrichment if applicable
        Object enrichedValue = value;
        if (value instanceof String && enrichmentData.containsKey(value)) {
            enrichedValue = enrichmentData.get(value);
        }
        // Then apply transformation function
        return transformationFunction.apply(enrichedValue);
    }

    @Override
    public Class<Object> getType() {
        return Object.class;
    }

    public void setEnrichmentData(Map<String, Object> enrichmentData) {
        this.enrichmentData = enrichmentData;
    }

    public void setTransformationFunction(Function<Object, Object> transformationFunction) {
        this.transformationFunction = transformationFunction;
    }

    // Existing methods
    public List<String> getLookupValues() {
        return lookupValues;
    }

    public boolean containsValue(String value) {
        return lookupValues.contains(value);
    }
}
