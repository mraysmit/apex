package dev.mars.apex.core.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Generic test data generation utilities for APEX Rules Engine.
 * Provides reusable test data generation methods that are not tied to specific domain models.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-25
 * @version 1.0
 */
public class TestDataGenerator {

    private static final Random random = new Random(42); // Fixed seed for reproducible results
    
    // Common test data arrays
    private static final String[] FIRST_NAMES = {
        "John", "Jane", "Bob", "Alice", "Charlie", "Diana", "Edward", "Fiona", "George", "Helen"
    };
    
    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"
    };
    
    private static final String[] CURRENCIES = {
        "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD", "SEK", "NOK"
    };
    
    private static final String[] COUNTRIES = {
        "US", "UK", "DE", "FR", "JP", "CA", "AU", "CH", "SE", "NO"
    };
    
    private static final String[] CITIES = {
        "New York", "London", "Frankfurt", "Paris", "Tokyo", "Toronto", "Sydney", "Zurich", "Stockholm", "Oslo"
    };

    /**
     * Generate a random string of specified length.
     */
    public static String randomString(int length) {
        StringBuilder sb = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * Generate a random alphanumeric ID.
     */
    public static String randomId(String prefix, int length) {
        return prefix + randomString(length);
    }
    
    /**
     * Generate a random email address.
     */
    public static String randomEmail() {
        String firstName = randomFirstName().toLowerCase();
        String lastName = randomLastName().toLowerCase();
        String[] domains = {"example.com", "test.org", "demo.net", "sample.io"};
        String domain = domains[random.nextInt(domains.length)];
        return firstName + "." + lastName + "@" + domain;
    }
    
    /**
     * Generate a random first name.
     */
    public static String randomFirstName() {
        return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
    }
    
    /**
     * Generate a random last name.
     */
    public static String randomLastName() {
        return LAST_NAMES[random.nextInt(LAST_NAMES.length)];
    }
    
    /**
     * Generate a random full name.
     */
    public static String randomFullName() {
        return randomFirstName() + " " + randomLastName();
    }
    
    /**
     * Generate a random currency code.
     */
    public static String randomCurrency() {
        return CURRENCIES[random.nextInt(CURRENCIES.length)];
    }
    
    /**
     * Generate a random country code.
     */
    public static String randomCountry() {
        return COUNTRIES[random.nextInt(COUNTRIES.length)];
    }
    
    /**
     * Generate a random city name.
     */
    public static String randomCity() {
        return CITIES[random.nextInt(CITIES.length)];
    }
    
    /**
     * Generate a random BigDecimal amount within specified range.
     */
    public static BigDecimal randomAmount(double min, double max) {
        double value = min + (max - min) * random.nextDouble();
        return BigDecimal.valueOf(Math.round(value * 100.0) / 100.0);
    }
    
    /**
     * Generate a random integer within specified range (inclusive).
     */
    public static int randomInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
    
    /**
     * Generate a random double within specified range.
     */
    public static double randomDouble(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }
    
    /**
     * Generate a random boolean.
     */
    public static boolean randomBoolean() {
        return random.nextBoolean();
    }
    
    /**
     * Generate a random date within the last N days.
     */
    public static LocalDate randomRecentDate(int daysBack) {
        return LocalDate.now().minusDays(random.nextInt(daysBack));
    }
    
    /**
     * Generate a random date within the next N days.
     */
    public static LocalDate randomFutureDate(int daysForward) {
        return LocalDate.now().plusDays(random.nextInt(daysForward));
    }
    
    /**
     * Generate a random datetime within the last N hours.
     */
    public static LocalDateTime randomRecentDateTime(int hoursBack) {
        return LocalDateTime.now().minusHours(random.nextInt(hoursBack));
    }
    
    /**
     * Generate a random element from an array.
     */
    public static <T> T randomElement(T[] array) {
        return array[random.nextInt(array.length)];
    }
    
    /**
     * Generate a random element from a list.
     */
    public static <T> T randomElement(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
    
    /**
     * Create a generic test data map with common fields.
     */
    public static Map<String, Object> createTestDataMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", randomId("TEST", 6));
        data.put("name", randomFullName());
        data.put("email", randomEmail());
        data.put("currency", randomCurrency());
        data.put("country", randomCountry());
        data.put("city", randomCity());
        data.put("amount", randomAmount(100.0, 10000.0));
        data.put("active", randomBoolean());
        data.put("createdDate", randomRecentDate(30));
        data.put("lastModified", randomRecentDateTime(24));
        return data;
    }
    
    /**
     * Create a list of test data maps.
     */
    public static List<Map<String, Object>> createTestDataList(int count) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(createTestDataMap());
        }
        return list;
    }
    
    /**
     * Validate that a map contains required fields.
     */
    public static boolean hasRequiredFields(Map<String, Object> data, String... requiredFields) {
        for (String field : requiredFields) {
            if (!data.containsKey(field) || data.get(field) == null) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Create a test result formatter for consistent output.
     */
    public static String formatTestResult(String testName, boolean passed, String details) {
        String status = passed ? "[PASS]" : "[FAIL]";
        return String.format("%-50s %s %s", testName, status, details != null ? details : "");
    }
    
    /**
     * Reset the random seed for reproducible test results.
     */
    public static void resetSeed() {
        random.setSeed(42);
    }
    
    /**
     * Set a custom seed for the random generator.
     */
    public static void setSeed(long seed) {
        random.setSeed(seed);
    }
}
