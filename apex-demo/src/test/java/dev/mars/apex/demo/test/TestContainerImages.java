package dev.mars.apex.demo.test;

/**
 * Central constants for Docker image versions used in Testcontainers integration tests.
 * 
 * <p>This class provides a single source of truth for all Docker image versions used
 * across the APEX project's integration tests. All Testcontainers should reference
 * these constants instead of hardcoding image versions.</p>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>{@code
 * // PostgreSQL Testcontainer
 * @Container
 * static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(TestContainerImages.POSTGRES)
 *         .withDatabaseName("test")
 *         .withUsername("test")
 *         .withPassword("test");
 * 
 * // Vault Testcontainer
 * @Container
 * static GenericContainer<?> vault = new GenericContainer<>(TestContainerImages.VAULT)
 *         .withExposedPorts(8200);
 * 
 * // Redis Testcontainer
 * @Container
 * static GenericContainer<?> redis = new GenericContainer<>(TestContainerImages.REDIS)
 *         .withExposedPorts(6379);
 * }</pre>
 * 
 * <p><strong>Version Management:</strong></p>
 * <ul>
 *   <li>All versions are defined in the root {@code pom.xml} as Maven properties</li>
 *   <li>This class reads those properties via {@code @Value} annotations or system properties</li>
 *   <li>To update versions, modify the root {@code pom.xml} properties section</li>
 *   <li>All tests will automatically use the new versions without code changes</li>
 * </ul>
 * 
 * <p><strong>Benefits:</strong></p>
 * <ul>
 *   <li>Single source of truth for Docker image versions</li>
 *   <li>Easy version updates across entire project</li>
 *   <li>Consistent testing environment</li>
 *   <li>Reduced Docker image downloads (same versions reused)</li>
 *   <li>Better CI/CD performance with Docker layer caching</li>
 * </ul>
 * 
 * @author APEX Team
 * @since 1.0.0
 */
public final class TestContainerImages {

    /**
     * PostgreSQL Docker image version for database integration tests.
     * 
     * <p>Uses PostgreSQL 15 with Alpine Linux base for smaller image size
     * and faster container startup. This version provides all PostgreSQL
     * features needed for APEX database testing.</p>
     * 
     * <p>Defined in root pom.xml as: {@code <docker.postgres.version>postgres:15-alpine</docker.postgres.version>}</p>
     */
    public static final String POSTGRES = System.getProperty("docker.postgres.version", "postgres:15-alpine");

    /**
     * HashiCorp Vault Docker image version for secret management integration tests.
     * 
     * <p>Uses Vault 1.15 which provides stable secret management capabilities
     * for testing APEX password injection and secret retrieval functionality.</p>
     * 
     * <p>Defined in root pom.xml as: {@code <docker.vault.version>hashicorp/vault:1.15</docker.vault.version>}</p>
     */
    public static final String VAULT = System.getProperty("docker.vault.version", "hashicorp/vault:1.20.0");

    /**
     * Redis Docker image version for cache integration tests.
     * 
     * <p>Uses Redis 6 with Alpine Linux base for caching and session
     * management testing in APEX applications.</p>
     * 
     * <p>Defined in root pom.xml as: {@code <docker.redis.version>redis:6-alpine</docker.redis.version>}</p>
     */
    public static final String REDIS = System.getProperty("docker.redis.version", "redis:6-alpine");

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private TestContainerImages() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Validates that all Docker image versions are properly defined.
     * 
     * <p>This method can be called in test setup to ensure all required
     * Docker images are available and properly configured.</p>
     * 
     * @throws IllegalStateException if any image version is null or empty
     */
    public static void validateImageVersions() {
        if (POSTGRES == null || POSTGRES.trim().isEmpty()) {
            throw new IllegalStateException("PostgreSQL Docker image version is not defined");
        }
        if (VAULT == null || VAULT.trim().isEmpty()) {
            throw new IllegalStateException("Vault Docker image version is not defined");
        }
        if (REDIS == null || REDIS.trim().isEmpty()) {
            throw new IllegalStateException("Redis Docker image version is not defined");
        }
    }

    /**
     * Returns a summary of all configured Docker image versions.
     * 
     * <p>Useful for logging and debugging test environment setup.</p>
     * 
     * @return formatted string containing all image versions
     */
    public static String getVersionSummary() {
        return String.format(
            "Docker Image Versions:%n" +
            "  PostgreSQL: %s%n" +
            "  Vault: %s%n" +
            "  Redis: %s",
            POSTGRES, VAULT, REDIS
        );
    }
}
