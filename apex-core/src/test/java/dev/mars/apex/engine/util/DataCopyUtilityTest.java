package dev.mars.apex.engine.util;

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

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DataCopyUtility}.
 *
 * <p>Verifies that deep copies produce fully independent structures:
 * modifying the copy must never affect the original, and vice versa.
 * This is the isolation boundary for concurrent scenario processing.</p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("DataCopyUtility contract tests")
class DataCopyUtilityTest {

    private static final Logger logger = LoggerFactory.getLogger(DataCopyUtilityTest.class);

    // ========================================================================
    // deepCopyMap
    // ========================================================================

    @Nested
    @DisplayName("deepCopyMap()")
    class DeepCopyMapTests {

        @Test
        @DisplayName("Null input returns null")
        void nullInputReturnsNull() {
            assertNull(DataCopyUtility.deepCopyMap(null));
        }

        @Test
        @DisplayName("Empty map returns new empty map")
        void emptyMapReturnsNewEmptyMap() {
            Map<String, Object> original = new HashMap<>();
            Map<String, Object> copy = DataCopyUtility.deepCopyMap(original);

            assertNotNull(copy);
            assertNotSame(original, copy, "Copy must be a different instance");
            assertTrue(copy.isEmpty());
        }

        @Test
        @DisplayName("Flat map values are equal but map instance is independent")
        void flatMapCopiedCorrectly() {
            Map<String, Object> original = new HashMap<>();
            original.put("name", "John");
            original.put("amount", 100.0);
            original.put("active", true);

            Map<String, Object> copy = DataCopyUtility.deepCopyMap(original);

            assertEquals(original, copy, "Copy should have same content");
            assertNotSame(original, copy, "Copy must be a different map instance");

            // Mutate copy — original must be unaffected
            copy.put("name", "Jane");
            assertEquals("John", original.get("name"),
                    "Mutating copy must not affect original");
        }

        @Test
        @DisplayName("Nested maps are independent — mutating copy does not affect original")
        void nestedMapIndependence() {
            Map<String, Object> inner = new HashMap<>();
            inner.put("city", "London");

            Map<String, Object> original = new HashMap<>();
            original.put("address", inner);

            Map<String, Object> copy = DataCopyUtility.deepCopyMap(original);

            @SuppressWarnings("unchecked")
            Map<String, Object> copiedInner = (Map<String, Object>) copy.get("address");

            assertNotSame(inner, copiedInner, "Nested map must be a different instance");
            assertEquals("London", copiedInner.get("city"));

            // Mutate the copy's nested map
            copiedInner.put("city", "Paris");
            assertEquals("London", inner.get("city"),
                    "ISOLATION BUG: mutating copied nested map affected the original");

            logger.info("PASSED: Nested map independence verified");
        }

        @Test
        @DisplayName("Deeply nested maps (3 levels) are fully independent")
        void deeplyNestedMapIndependence() {
            Map<String, Object> level3 = new HashMap<>();
            level3.put("value", 42);

            Map<String, Object> level2 = new HashMap<>();
            level2.put("level3", level3);

            Map<String, Object> level1 = new HashMap<>();
            level1.put("level2", level2);

            Map<String, Object> original = new HashMap<>();
            original.put("level1", level1);

            Map<String, Object> copy = DataCopyUtility.deepCopyMap(original);

            // Navigate to deepest level in copy and mutate
            @SuppressWarnings("unchecked")
            Map<String, Object> copyL1 = (Map<String, Object>) copy.get("level1");
            @SuppressWarnings("unchecked")
            Map<String, Object> copyL2 = (Map<String, Object>) copyL1.get("level2");
            @SuppressWarnings("unchecked")
            Map<String, Object> copyL3 = (Map<String, Object>) copyL2.get("level3");

            copyL3.put("value", 999);

            assertEquals(42, level3.get("value"),
                    "ISOLATION BUG: mutating 3-level-deep copy affected original");
        }

        @Test
        @DisplayName("Lists inside maps are independently copied")
        void listInsideMapIndependence() {
            List<Object> items = new ArrayList<>();
            items.add("item1");
            items.add("item2");

            Map<String, Object> original = new HashMap<>();
            original.put("items", items);

            Map<String, Object> copy = DataCopyUtility.deepCopyMap(original);

            @SuppressWarnings("unchecked")
            List<Object> copiedItems = (List<Object>) copy.get("items");

            assertNotSame(items, copiedItems, "List must be a different instance");

            copiedItems.add("item3");
            assertEquals(2, items.size(),
                    "ISOLATION BUG: mutating copied list affected original");
        }

        @Test
        @DisplayName("Maps inside lists inside maps are independently copied")
        void mapsInsideListsIndependence() {
            Map<String, Object> listEntry = new HashMap<>();
            listEntry.put("id", "entry1");

            List<Object> list = new ArrayList<>();
            list.add(listEntry);

            Map<String, Object> original = new HashMap<>();
            original.put("entries", list);

            Map<String, Object> copy = DataCopyUtility.deepCopyMap(original);

            @SuppressWarnings("unchecked")
            List<Object> copiedList = (List<Object>) copy.get("entries");
            @SuppressWarnings("unchecked")
            Map<String, Object> copiedEntry = (Map<String, Object>) copiedList.get(0);

            assertNotSame(listEntry, copiedEntry, "Map inside list must be independent");

            copiedEntry.put("id", "modified");
            assertEquals("entry1", listEntry.get("id"),
                    "ISOLATION BUG: mutating map-in-list copy affected original");
        }
    }

    // ========================================================================
    // deepCopyValue
    // ========================================================================

    @Nested
    @DisplayName("deepCopyValue()")
    class DeepCopyValueTests {

        @Test
        @DisplayName("Null value returns null")
        void nullValueReturnsNull() {
            assertNull(DataCopyUtility.deepCopyValue(null));
        }

        @Test
        @DisplayName("Immutable types returned as-is")
        void immutableTypesReturnedAsIs() {
            assertSame("hello", DataCopyUtility.deepCopyValue("hello"));
            assertEquals(42, DataCopyUtility.deepCopyValue(42));
            assertEquals(true, DataCopyUtility.deepCopyValue(true));
            assertEquals(3.14, DataCopyUtility.deepCopyValue(3.14));
        }

        @Test
        @DisplayName("Map value produces independent copy")
        void mapValueProducesIndependentCopy() {
            Map<String, Object> original = new HashMap<>();
            original.put("key", "value");

            Object copy = DataCopyUtility.deepCopyValue(original);

            assertInstanceOf(Map.class, copy);
            assertNotSame(original, copy);

            @SuppressWarnings("unchecked")
            Map<String, Object> copiedMap = (Map<String, Object>) copy;
            copiedMap.put("key", "modified");
            assertEquals("value", original.get("key"),
                    "ISOLATION BUG: deepCopyValue(Map) did not produce independent copy");
        }

        @Test
        @DisplayName("List value produces independent copy")
        void listValueProducesIndependentCopy() {
            List<Object> original = new ArrayList<>();
            original.add("a");
            original.add("b");

            Object copy = DataCopyUtility.deepCopyValue(original);

            assertInstanceOf(List.class, copy);
            assertNotSame(original, copy);

            @SuppressWarnings("unchecked")
            List<Object> copiedList = (List<Object>) copy;
            copiedList.add("c");
            assertEquals(2, original.size(),
                    "ISOLATION BUG: deepCopyValue(List) did not produce independent copy");
        }
    }

    // ========================================================================
    // deepMergeInto
    // ========================================================================

    @Nested
    @DisplayName("deepMergeInto()")
    class DeepMergeTests {

        @Test
        @DisplayName("Null target or source is handled gracefully")
        void nullTargetOrSourceHandledGracefully() {
            assertDoesNotThrow(() -> DataCopyUtility.deepMergeInto(null, new HashMap<>()));
            assertDoesNotThrow(() -> DataCopyUtility.deepMergeInto(new HashMap<>(), null));
            assertDoesNotThrow(() -> DataCopyUtility.deepMergeInto(null, null));
        }

        @Test
        @DisplayName("Merge adds new keys to target")
        void mergeAddsNewKeys() {
            Map<String, Object> target = new HashMap<>();
            target.put("a", 1);

            Map<String, Object> source = new HashMap<>();
            source.put("b", 2);

            DataCopyUtility.deepMergeInto(target, source);

            assertEquals(1, target.get("a"));
            assertEquals(2, target.get("b"));
        }

        @Test
        @DisplayName("Merge recursively merges nested maps")
        void mergeRecursivelyMergesNestedMaps() {
            Map<String, Object> targetInner = new HashMap<>();
            targetInner.put("existing", "keep");

            Map<String, Object> target = new HashMap<>();
            target.put("nested", targetInner);

            Map<String, Object> sourceInner = new HashMap<>();
            sourceInner.put("added", "new");

            Map<String, Object> source = new HashMap<>();
            source.put("nested", sourceInner);

            DataCopyUtility.deepMergeInto(target, source);

            @SuppressWarnings("unchecked")
            Map<String, Object> merged = (Map<String, Object>) target.get("nested");
            assertEquals("keep", merged.get("existing"), "Existing nested value should be preserved");
            assertEquals("new", merged.get("added"), "New nested value should be merged");
        }

        @Test
        @DisplayName("Merged values are deep-copied — source isolation preserved")
        void mergedValuesAreDeepCopied() {
            Map<String, Object> target = new HashMap<>();

            Map<String, Object> sourceInner = new HashMap<>();
            sourceInner.put("data", "original");

            Map<String, Object> source = new HashMap<>();
            source.put("payload", sourceInner);

            DataCopyUtility.deepMergeInto(target, source);

            // Mutate source after merge
            sourceInner.put("data", "modified");

            @SuppressWarnings("unchecked")
            Map<String, Object> targetPayload = (Map<String, Object>) target.get("payload");
            assertEquals("original", targetPayload.get("data"),
                    "ISOLATION BUG: merged value shares reference with source");
        }
    }

    // ========================================================================
    // convertToMap
    // ========================================================================

    @Nested
    @DisplayName("convertToMap()")
    class ConvertToMapTests {

        @Test
        @DisplayName("Map input returns shallow copy")
        void mapInputReturnsShallowCopy() {
            Map<String, Object> original = new HashMap<>();
            original.put("key", "value");

            Map<String, Object> result = DataCopyUtility.convertToMap(original);

            assertNotSame(original, result);
            assertEquals("value", result.get("key"));
        }

        @Test
        @DisplayName("Non-map input wrapped in map with 'data' key")
        void nonMapWrappedInDataKey() {
            Map<String, Object> result = DataCopyUtility.convertToMap("hello");

            assertEquals("hello", result.get("data"));
            assertEquals(1, result.size());
        }
    }
}
