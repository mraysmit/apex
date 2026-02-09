package dev.mars.apex.core.engine.config.util;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for deep copying data structures.
 * 
 * <p>Provides thread-safe deep copy operations for Maps and Lists, ensuring complete
 * isolation for parallel processing scenarios where multiple threads may mutate
 * data structures concurrently.</p>
 * 
 * <p>This utility is essential for APEX's scenario processing where input data
 * needs to be protected from concurrent modifications across multiple stage executions.</p>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-22
 */
public final class DataCopyUtility {
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private DataCopyUtility() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Creates a deep copy of a Map, recursively copying all nested Maps and Lists.
     * This ensures complete isolation for parallel processing where enrichments
     * may mutate nested structures.
     * 
     * <p>Handles the following types:</p>
     * <ul>
     *   <li>Map - recursively deep copied</li>
     *   <li>List - recursively deep copied (elements that are Maps/Lists are also copied)</li>
     *   <li>Primitive wrappers, Strings, etc. - referenced directly (immutable)</li>
     * </ul>
     *
     * @param original The original map to copy
     * @return A deep copy of the map with all nested structures copied, or null if input is null
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepCopyMap(Map<String, Object> original) {
        if (original == null) {
            return null;
        }
        
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }
        return copy;
    }

    /**
     * Deep copies a single value, handling Maps, Lists, and other types.
     * 
     * <p>This method provides recursive deep copying for complex nested structures
     * while handling immutable types efficiently by returning them directly.</p>
     *
     * @param value The value to copy
     * @return A deep copy of the value if it's a Map or List, otherwise the original value
     */
    @SuppressWarnings("unchecked")
    public static Object deepCopyValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Map) {
            // Recursively copy nested maps
            Map<String, Object> mapValue = (Map<String, Object>) value;
            return deepCopyMap(mapValue);
        }
        
        if (value instanceof List) {
            // Recursively copy list elements
            List<Object> listValue = (List<Object>) value;
            List<Object> listCopy = new ArrayList<>(listValue.size());
            for (Object item : listValue) {
                listCopy.add(deepCopyValue(item));
            }
            return listCopy;
        }
        
        // For immutable types (String, Number, Boolean, etc.), return as-is
        // For other mutable types, we rely on the fact that enrichments typically
        // don't modify arbitrary objects - they work with Maps and primitives
        return value;
    }
    
    /**
     * Convert an object to a Map.
     * If the object is already a Map, return a shallow copy.
     * Otherwise, wrap it in a Map with key "data".
     * 
     * <p>Note: For parallel enrichment processing, use {@link #deepCopyMap(Map)} instead
     * to ensure nested structures are fully isolated between threads.</p>
     *
     * @param object The object to convert
     * @return A Map representation of the object
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> convertToMap(Object object) {
        if (object instanceof Map) {
            return new HashMap<>((Map<String, Object>) object);
        } else {
            Map<String, Object> wrappedMap = new HashMap<>();
            wrappedMap.put("data", object);
            return wrappedMap;
        }
    }
}
