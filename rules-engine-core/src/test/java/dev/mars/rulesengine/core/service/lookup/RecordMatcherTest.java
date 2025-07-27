package dev.mars.rulesengine.core.service.lookup;

import dev.mars.rulesengine.core.service.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
 * Test class for RecordMatcher interface.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for RecordMatcher interface.
 */
public class RecordMatcherTest {
    private TestRecordMatcher recordMatcher;
    private List<TestRecord> sourceRecords;
    private List<String> validatorNames;
    private Map<String, Validator<Object>> validators;

    @BeforeEach
    public void setUp() {
        // Create validators
        validators = new HashMap<>();
        validators.put("evenValidator", new EvenValidator("evenValidator"));
        validators.put("positiveValidator", new PositiveValidator("positiveValidator"));
        validators.put("greaterThanTenValidator", new GreaterThanTenValidator("greaterThanTenValidator"));

        // Create record matcher
        recordMatcher = new TestRecordMatcher(validators);

        // Create source records
        sourceRecords = Arrays.asList(
            new TestRecord(1),    // Odd, positive, not > 10
            new TestRecord(2),    // Even, positive, not > 10
            new TestRecord(12),   // Even, positive, > 10
            new TestRecord(-5),   // Odd, negative, not > 10
            new TestRecord(-10),  // Even, negative, not > 10
            new TestRecord(15)    // Odd, positive, > 10
        );

        // Create validation names
        validatorNames = Arrays.asList("evenValidator", "positiveValidator", "greaterThanTenValidator");
    }

    @Test
    public void testFindMatchingRecords() {
        // Test finding records that match any of the validators
        List<TestRecord> matchingRecords = recordMatcher.findMatchingRecords(sourceRecords, validatorNames);

        // Verify the matching records
        assertNotNull(matchingRecords);
        assertEquals(5, matchingRecords.size());
        assertTrue(matchingRecords.contains(new TestRecord(1)));    // Matches positiveValidator
        assertTrue(matchingRecords.contains(new TestRecord(2)));    // Matches evenValidator and positiveValidator
        assertTrue(matchingRecords.contains(new TestRecord(12)));   // Matches evenValidator, positiveValidator, and greaterThanTenValidator
        assertTrue(matchingRecords.contains(new TestRecord(-10)));  // Matches evenValidator
        assertTrue(matchingRecords.contains(new TestRecord(15)));   // Matches positiveValidator and greaterThanTenValidator

        // Verify the non-matching record is not included
        assertFalse(matchingRecords.contains(new TestRecord(-5))); // Doesn't match any validation
    }

    @Test
    public void testFindMatchingRecordsWithSubsetOfValidators() {
        // Test finding records that match a subset of validators
        List<String> evenValidatorOnly = Collections.singletonList("evenValidator");
        List<TestRecord> matchingRecords = recordMatcher.findMatchingRecords(sourceRecords, evenValidatorOnly);

        // Verify the matching records
        assertNotNull(matchingRecords);
        assertEquals(3, matchingRecords.size());
        assertTrue(matchingRecords.contains(new TestRecord(2)));    // Matches evenValidator
        assertTrue(matchingRecords.contains(new TestRecord(12)));   // Matches evenValidator
        assertTrue(matchingRecords.contains(new TestRecord(-10)));  // Matches evenValidator

        // Verify the non-matching records are not included
        assertFalse(matchingRecords.contains(new TestRecord(1)));   // Doesn't match evenValidator
        assertFalse(matchingRecords.contains(new TestRecord(-5)));  // Doesn't match evenValidator
        assertFalse(matchingRecords.contains(new TestRecord(15)));  // Doesn't match evenValidator
    }

    @Test
    public void testFindNonMatchingRecords() {
        // Test finding records that don't match any of the validators
        List<TestRecord> nonMatchingRecords = recordMatcher.findNonMatchingRecords(sourceRecords, validatorNames);

        // Verify the non-matching records
        assertNotNull(nonMatchingRecords);
        assertEquals(1, nonMatchingRecords.size());
        assertTrue(nonMatchingRecords.contains(new TestRecord(-5))); // Doesn't match any validation

        // Verify the matching records are not included
        assertFalse(nonMatchingRecords.contains(new TestRecord(1)));    // Matches positiveValidator
        assertFalse(nonMatchingRecords.contains(new TestRecord(2)));    // Matches evenValidator and positiveValidator
        assertFalse(nonMatchingRecords.contains(new TestRecord(12)));   // Matches evenValidator, positiveValidator, and greaterThanTenValidator
        assertFalse(nonMatchingRecords.contains(new TestRecord(-10)));  // Matches evenValidator
        assertFalse(nonMatchingRecords.contains(new TestRecord(15)));   // Matches positiveValidator and greaterThanTenValidator
    }

    @Test
    public void testFindNonMatchingRecordsWithSubsetOfValidators() {
        // Test finding records that don't match a subset of validators
        List<String> evenValidatorOnly = Collections.singletonList("evenValidator");
        List<TestRecord> nonMatchingRecords = recordMatcher.findNonMatchingRecords(sourceRecords, evenValidatorOnly);

        // Verify the non-matching records
        assertNotNull(nonMatchingRecords);
        assertEquals(3, nonMatchingRecords.size());
        assertTrue(nonMatchingRecords.contains(new TestRecord(1)));   // Doesn't match evenValidator
        assertTrue(nonMatchingRecords.contains(new TestRecord(-5)));  // Doesn't match evenValidator
        assertTrue(nonMatchingRecords.contains(new TestRecord(15)));  // Doesn't match evenValidator

        // Verify the matching records are not included
        assertFalse(nonMatchingRecords.contains(new TestRecord(2)));    // Matches evenValidator
        assertFalse(nonMatchingRecords.contains(new TestRecord(12)));   // Matches evenValidator
        assertFalse(nonMatchingRecords.contains(new TestRecord(-10)));  // Matches evenValidator
    }

    @Test
    public void testFindMatchingRecordsWithEmptySourceRecords() {
        // Test finding matching records with empty source records
        List<TestRecord> matchingRecords = recordMatcher.findMatchingRecords(Collections.emptyList(), validatorNames);

        // Verify the result
        assertNotNull(matchingRecords);
        assertTrue(matchingRecords.isEmpty());
    }

    @Test
    public void testFindNonMatchingRecordsWithEmptySourceRecords() {
        // Test finding non-matching records with empty source records
        List<TestRecord> nonMatchingRecords = recordMatcher.findNonMatchingRecords(Collections.emptyList(), validatorNames);

        // Verify the result
        assertNotNull(nonMatchingRecords);
        assertTrue(nonMatchingRecords.isEmpty());
    }

    @Test
    public void testFindMatchingRecordsWithEmptyValidatorNames() {
        // Test finding matching records with empty validation names
        List<TestRecord> matchingRecords = recordMatcher.findMatchingRecords(sourceRecords, Collections.emptyList());

        // Verify the result
        assertNotNull(matchingRecords);
        assertTrue(matchingRecords.isEmpty());
    }

    @Test
    public void testFindNonMatchingRecordsWithEmptyValidatorNames() {
        // Test finding non-matching records with empty validation names
        List<TestRecord> nonMatchingRecords = recordMatcher.findNonMatchingRecords(sourceRecords, Collections.emptyList());

        // Verify the result
        assertNotNull(nonMatchingRecords);
        assertEquals(sourceRecords.size(), nonMatchingRecords.size());
        assertTrue(nonMatchingRecords.containsAll(sourceRecords));
    }

    @Test
    public void testFindMatchingRecordsWithNullSourceRecords() {
        // Test finding matching records with null source records
        List<TestRecord> matchingRecords = recordMatcher.findMatchingRecords(null, validatorNames);

        // Verify the result
        assertNotNull(matchingRecords);
        assertTrue(matchingRecords.isEmpty());
    }

    @Test
    public void testFindNonMatchingRecordsWithNullSourceRecords() {
        // Test finding non-matching records with null source records
        List<TestRecord> nonMatchingRecords = recordMatcher.findNonMatchingRecords(null, validatorNames);

        // Verify the result
        assertNotNull(nonMatchingRecords);
        assertTrue(nonMatchingRecords.isEmpty());
    }

    @Test
    public void testFindMatchingRecordsWithNullValidatorNames() {
        // Test finding matching records with null validation names
        List<TestRecord> matchingRecords = recordMatcher.findMatchingRecords(sourceRecords, null);

        // Verify the result
        assertNotNull(matchingRecords);
        assertTrue(matchingRecords.isEmpty());
    }

    @Test
    public void testFindNonMatchingRecordsWithNullValidatorNames() {
        // Test finding non-matching records with null validation names
        List<TestRecord> nonMatchingRecords = recordMatcher.findNonMatchingRecords(sourceRecords, null);

        // Verify the result
        assertNotNull(nonMatchingRecords);
        assertEquals(sourceRecords.size(), nonMatchingRecords.size());
        assertTrue(nonMatchingRecords.containsAll(sourceRecords));
    }

    @Test
    public void testFindMatchingRecordsWithNonExistentValidator() {
        // Test finding matching records with a non-existent validation
        List<String> nonExistentValidator = Collections.singletonList("nonExistentValidator");
        List<TestRecord> matchingRecords = recordMatcher.findMatchingRecords(sourceRecords, nonExistentValidator);

        // Verify the result
        assertNotNull(matchingRecords);
        assertTrue(matchingRecords.isEmpty());
    }

    @Test
    public void testFindNonMatchingRecordsWithNonExistentValidator() {
        // Test finding non-matching records with a non-existent validation
        List<String> nonExistentValidator = Collections.singletonList("nonExistentValidator");
        List<TestRecord> nonMatchingRecords = recordMatcher.findNonMatchingRecords(sourceRecords, nonExistentValidator);

        // Verify the result
        assertNotNull(nonMatchingRecords);
        assertEquals(sourceRecords.size(), nonMatchingRecords.size());
        assertTrue(nonMatchingRecords.containsAll(sourceRecords));
    }

    /**
     * Test implementation of RecordMatcher for testing.
     */
    private static class TestRecordMatcher implements RecordMatcher<TestRecord> {
        private final Map<String, Validator<Object>> validators;

        public TestRecordMatcher(Map<String, Validator<Object>> validators) {
            this.validators = validators;
        }

        @Override
        public List<TestRecord> findMatchingRecords(List<TestRecord> sourceRecords, List<String> validatorNames) {
            if (sourceRecords == null || validatorNames == null) {
                return new ArrayList<>();
            }

            List<TestRecord> matchingRecords = new ArrayList<>();

            for (TestRecord record : sourceRecords) {
                for (String validatorName : validatorNames) {
                    Validator<Object> validator = validators.get(validatorName);
                    if (validator != null && validator.validate((Object) record.getValue())) {
                        matchingRecords.add(record);
                        break;
                    }
                }
            }

            return matchingRecords;
        }

        @Override
        public List<TestRecord> findNonMatchingRecords(List<TestRecord> sourceRecords, List<String> validatorNames) {
            if (sourceRecords == null) {
                return new ArrayList<>();
            }

            if (validatorNames == null || validatorNames.isEmpty()) {
                return new ArrayList<>(sourceRecords);
            }

            List<TestRecord> nonMatchingRecords = new ArrayList<>();

            for (TestRecord record : sourceRecords) {
                boolean matches = false;

                for (String validatorName : validatorNames) {
                    Validator<Object> validator = validators.get(validatorName);
                    if (validator != null && validator.validate((Object) record.getValue())) {
                        matches = true;
                        break;
                    }
                }

                if (!matches) {
                    nonMatchingRecords.add(record);
                }
            }

            return nonMatchingRecords;
        }
    }

    /**
     * Simple record class for testing.
     */
    private static class TestRecord {
        private final int value;

        public TestRecord(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestRecord that = (TestRecord) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public String toString() {
            return "TestRecord{value=" + value + "}";
        }
    }

    /**
     * Validator that checks if a value is even.
     */
    private static class EvenValidator implements Validator<Object> {
        private final String name;

        public EvenValidator(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean validate(Object value) {
            if (value instanceof Integer) {
                return ((Integer) value) % 2 == 0;
            } else if (value instanceof TestRecord) {
                return ((TestRecord) value).getValue() % 2 == 0;
            }
            return false;
        }

        @Override
        public Class<Object> getType() {
            return Object.class;
        }
    }

    /**
     * Validator that checks if a value is positive.
     */
    private static class PositiveValidator implements Validator<Object> {
        private final String name;

        public PositiveValidator(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean validate(Object value) {
            if (value instanceof Integer) {
                return ((Integer) value) > 0;
            } else if (value instanceof TestRecord) {
                return ((TestRecord) value).getValue() > 0;
            }
            return false;
        }

        @Override
        public Class<Object> getType() {
            return Object.class;
        }
    }

    /**
     * Validator that checks if a value is greater than 10.
     */
    private static class GreaterThanTenValidator implements Validator<Object> {
        private final String name;

        public GreaterThanTenValidator(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean validate(Object value) {
            if (value instanceof Integer) {
                return ((Integer) value) > 10;
            } else if (value instanceof TestRecord) {
                return ((TestRecord) value).getValue() > 10;
            }
            return false;
        }

        @Override
        public Class<Object> getType() {
            return Object.class;
        }
    }
}
