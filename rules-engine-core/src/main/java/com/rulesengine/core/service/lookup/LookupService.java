package com.rulesengine.core.service.lookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
    public Object enrich(Object value) {
        if (value instanceof String && enrichmentData.containsKey(value)) {
            return enrichmentData.get(value);
        }
        return value;
    }

    @Override
    public Object transform(Object value) {
        return transformationFunction.apply(value);
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
