package dev.mars.apex.playground.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.apex.playground.model.DataSourceConnection;
import dev.mars.apex.playground.model.DataSourceConnection.DatabaseType;
import dev.mars.apex.playground.model.QueryRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for DataSourceController with H2 and PostgreSQL databases.
 * Tests all CRUD operations and query execution via REST API using real HTTP requests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataSourceControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    // Optional Testcontainer - only used if Docker is available
    private static PostgreSQLContainer<?> postgresContainer;

    @Autowired
    private ObjectMapper objectMapper;

    private static String h2ConnectionId;
    private static String postgresConnectionId;

    // API paths
    private static final String API_BASE = "/playground/api/datasources";
    
    // Test data
    private static final String H2_CONNECTION_NAME = "Test H2 Database";
    private static final String POSTGRES_CONNECTION_NAME = "Test PostgreSQL Database";
    
    // PostgreSQL connection details - populated from Testcontainers
    private static String postgresHost;
    private static int postgresPort;
    private static String postgresDatabase;
    private static String postgresUsername;
    private static String postgresPassword;

    @BeforeAll
    static void setupTestData() {
        System.out.println("\n========================================");
        System.out.println("Data Sources Integration Test Suite");
        System.out.println("========================================\n");
        
        // Try to start Testcontainer if Docker is available
        try {
            postgresContainer = new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");
            postgresContainer.start();
            
            postgresHost = postgresContainer.getHost();
            postgresPort = postgresContainer.getFirstMappedPort();
            postgresDatabase = postgresContainer.getDatabaseName();
            postgresUsername = postgresContainer.getUsername();
            postgresPassword = postgresContainer.getPassword();
            System.out.println("✓ PostgreSQL Testcontainer started at " + postgresHost + ":" + postgresPort);
        } catch (Exception e) {
            // Docker not available, try local PostgreSQL
            System.out.println("⚠ Docker not available, checking for local PostgreSQL...");
            postgresHost = "localhost";
            postgresPort = 5432;
            postgresDatabase = "postgres";
            postgresUsername = "postgres";
            postgresPassword = "postgres";

            if (isLocalPostgreSQLAvailable()) {
                System.out.println("✓ Local PostgreSQL found at " + postgresHost + ":" + postgresPort);
            } else {
                System.out.println("⚠ No PostgreSQL available (Docker or local)");
            }
        }
    }
    
    @AfterAll
    static void tearDown() {
        if (postgresContainer != null && postgresContainer.isRunning()) {
            postgresContainer.stop();
            System.out.println("✓ PostgreSQL Testcontainer stopped");
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Create H2 Connection")
    void testCreateH2Connection() throws Exception {
        DataSourceConnection h2Connection = new DataSourceConnection();
        h2Connection.setName(H2_CONNECTION_NAME);
        h2Connection.setType(DatabaseType.H2);
        h2Connection.setDatabase("mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        h2Connection.setUsername("sa");
        h2Connection.setPassword("");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DataSourceConnection> request = new HttpEntity<>(h2Connection, headers);

        ResponseEntity<DataSourceConnection> response = restTemplate.postForEntity(
                API_BASE + "/connections",
                request,
                DataSourceConnection.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(H2_CONNECTION_NAME, response.getBody().getName());
        assertEquals("H2", response.getBody().getType().toString());

        h2ConnectionId = response.getBody().getId();
        System.out.println("✓ H2 Connection created with ID: " + h2ConnectionId);
    }

    @Test
    @Order(2)
    @DisplayName("2. Test H2 Connection")
    void testH2Connection() throws Exception {
        DataSourceConnection h2Connection = new DataSourceConnection();
        h2Connection.setName(H2_CONNECTION_NAME);
        h2Connection.setType(DatabaseType.H2);
        h2Connection.setDatabase("mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        h2Connection.setUsername("sa");
        h2Connection.setPassword("");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DataSourceConnection> request = new HttpEntity<>(h2Connection, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                API_BASE + "/test",
                request,
                Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));

        System.out.println("✓ H2 Connection tested successfully");
    }

    @Test
    @Order(3)
    @DisplayName("3. Setup H2 Test Data")
    void testSetupH2TestData() throws Exception {
        System.out.println("Using connection ID: " + h2ConnectionId);
        
        // Create table
        QueryRequest createTable = new QueryRequest(
                "CREATE TABLE IF NOT EXISTS employees (" +
                        "id INT PRIMARY KEY, name VARCHAR(100), department VARCHAR(50), salary DECIMAL(10,2))");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<QueryRequest> createTableRequest = new HttpEntity<>(createTable, headers);

        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                API_BASE + "/connections/" + h2ConnectionId + "/query",
                createTableRequest,
                Map.class);

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());

        // Insert test data
        String[] inserts = {
                "INSERT INTO employees VALUES (1, 'Alice Smith', 'Engineering', 85000.00)",
                "INSERT INTO employees VALUES (2, 'Bob Johnson', 'Sales', 65000.00)",
                "INSERT INTO employees VALUES (3, 'Carol Williams', 'Engineering', 90000.00)",
                "INSERT INTO employees VALUES (4, 'David Brown', 'Marketing', 70000.00)",
                "INSERT INTO employees VALUES (5, 'Eve Davis', 'Engineering', 95000.00)"
        };

        for (String sql : inserts) {
            HttpEntity<QueryRequest> insertRequest = new HttpEntity<>(new QueryRequest(sql), headers);
            ResponseEntity<Map> insertResponse = restTemplate.postForEntity(
                    API_BASE + "/connections/" + h2ConnectionId + "/query",
                    insertRequest,
                    Map.class);
            assertEquals(HttpStatus.OK, insertResponse.getStatusCode());
        }

        System.out.println("✓ H2 Test data created: employees table with 5 rows");
    }

    @Test
    @Order(4)
    @DisplayName("4. Execute Query on H2")
    void testExecuteQueryOnH2() throws Exception {
        QueryRequest queryRequest = new QueryRequest("SELECT * FROM employees ORDER BY id");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<QueryRequest> request = new HttpEntity<>(queryRequest, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                API_BASE + "/connections/" + h2ConnectionId + "/query",
                request,
                Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("columns"));
        assertTrue(response.getBody().containsKey("rows"));
        assertTrue(response.getBody().containsKey("rowCount"));

        List<?> columns = (List<?>) response.getBody().get("columns");
        List<?> rows = (List<?>) response.getBody().get("rows");
        assertEquals(4, columns.size());
        assertEquals(5, rows.size());
        assertEquals(5, response.getBody().get("rowCount"));

        System.out.println("✓ Query executed on H2: Retrieved 5 employees");
    }

    @Test
    @Order(5)
    @DisplayName("5. Execute Filtered Query on H2")
    void testExecuteFilteredQueryOnH2() throws Exception {
        QueryRequest queryRequest = new QueryRequest(
                "SELECT name, salary FROM employees WHERE department = 'Engineering' ORDER BY salary DESC");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<QueryRequest> request = new HttpEntity<>(queryRequest, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                API_BASE + "/connections/" + h2ConnectionId + "/query",
                request,
                Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        List<?> columns = (List<?>) response.getBody().get("columns");
        List<List<?>> rows = (List<List<?>>) response.getBody().get("rows");
        assertEquals(2, columns.size());
        assertEquals(3, rows.size());
        assertEquals("Eve Davis", rows.get(0).get(0));

        System.out.println("✓ Filtered query executed: 3 Engineering employees");
    }

    @Test
    @Order(6)
    @DisplayName("6. Get H2 Schema")
    void testGetH2Schema() throws Exception {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                API_BASE + "/connections/" + h2ConnectionId + "/schema",
                Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("database"));
        assertTrue(response.getBody().containsKey("tables"));

        System.out.println("✓ H2 Schema retrieved successfully");
    }

    @Test
    @Order(7)
    @DisplayName("7. List All Connections")
    void testListConnections() throws Exception {
        ResponseEntity<List> response = restTemplate.getForEntity(
                API_BASE + "/connections",
                List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertThat(response.getBody().size(), greaterThanOrEqualTo(1));

        System.out.println("✓ Connection list retrieved");
    }

    @Test
    @Order(8)
    @DisplayName("8. Create PostgreSQL Connection")
    void testCreatePostgreSQLConnection() throws Exception {
        // Check if PostgreSQL is available before attempting connection
        if (!isPostgreSQLAvailable()) {
            System.out.println("⚠ PostgreSQL connection skipped - server not available at " + postgresHost + ":" + postgresPort);
            Assumptions.assumeFalse(true, "PostgreSQL not available");
            return;
        }

        DataSourceConnection pgConnection = new DataSourceConnection();
        pgConnection.setName(POSTGRES_CONNECTION_NAME);
        pgConnection.setType(DatabaseType.POSTGRESQL);
        pgConnection.setHost(postgresHost);
        pgConnection.setPort(postgresPort);
        pgConnection.setDatabase(postgresDatabase);
        pgConnection.setUsername(postgresUsername);
        pgConnection.setPassword(postgresPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DataSourceConnection> request = new HttpEntity<>(pgConnection, headers);

        ResponseEntity<DataSourceConnection> response = restTemplate.postForEntity(
                API_BASE + "/connections",
                request,
                DataSourceConnection.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(POSTGRES_CONNECTION_NAME, response.getBody().getName());

        postgresConnectionId = response.getBody().getId();

        System.out.println("✓ PostgreSQL Connection created with ID: " + postgresConnectionId);
    }

    @Test
    @Order(9)
    @DisplayName("9. Test PostgreSQL Connection")
    void testPostgreSQLConnection() throws Exception {
        Assumptions.assumeTrue(postgresConnectionId != null, "PostgreSQL not created");

        DataSourceConnection pgConnection = new DataSourceConnection();
        pgConnection.setType(DatabaseType.POSTGRESQL);
        pgConnection.setHost(postgresHost);
        pgConnection.setPort(postgresPort);
        pgConnection.setDatabase(postgresDatabase);
        pgConnection.setUsername(postgresUsername);
        pgConnection.setPassword(postgresPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DataSourceConnection> request = new HttpEntity<>(pgConnection, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                API_BASE + "/test",
                request,
                Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        System.out.println("✓ PostgreSQL Connection tested");
    }

    @Test
    @Order(10)
    @DisplayName("10. Execute Query on PostgreSQL")
    void testExecuteQueryOnPostgreSQL() throws Exception {
        Assumptions.assumeTrue(postgresConnectionId != null, "PostgreSQL not created");

        QueryRequest queryRequest = new QueryRequest(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' LIMIT 5");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<QueryRequest> request = new HttpEntity<>(queryRequest, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                API_BASE + "/connections/" + postgresConnectionId + "/query",
                request,
                Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("columns"));

        System.out.println("✓ Query executed on PostgreSQL");
    }

    @Test
    @Order(11)
    @DisplayName("11. Get Single Connection")
    void testGetConnection() throws Exception {
        ResponseEntity<DataSourceConnection> response = restTemplate.getForEntity(
                API_BASE + "/connections/" + h2ConnectionId,
                DataSourceConnection.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(h2ConnectionId, response.getBody().getId());
        assertEquals(H2_CONNECTION_NAME, response.getBody().getName());

        System.out.println("✓ Single connection retrieved");
    }

    @Test
    @Order(98)
    @DisplayName("98. Delete PostgreSQL Connection")
    void testDeletePostgreSQLConnection() throws Exception {
        if (postgresConnectionId != null) {
            restTemplate.delete(API_BASE + "/connections/" + postgresConnectionId);

            System.out.println("✓ PostgreSQL Connection deleted");
        }
    }

    @Test
    @Order(99)
    @DisplayName("99. Delete H2 Connection")
    void testDeleteH2Connection() throws Exception {
        restTemplate.delete(API_BASE + "/connections/" + h2ConnectionId);

        ResponseEntity<DataSourceConnection> response = restTemplate.getForEntity(
                API_BASE + "/connections/" + h2ConnectionId,
                DataSourceConnection.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        System.out.println("✓ H2 Connection deleted");
    }

    @AfterAll
    static void printSummary() {
        System.out.println("\n========================================");
        System.out.println("Data Sources Tests Complete");
        System.out.println("========================================\n");
    }

    /**
     * Check if PostgreSQL is available (Testcontainer or local).
     */
    private boolean isPostgreSQLAvailable() {
        // Check Testcontainer first
        if (postgresContainer != null && postgresContainer.isRunning()) {
            return true;
        }
        // Fallback to local PostgreSQL
        return isLocalPostgreSQLAvailable();
    }
    
    /**
     * Check if local PostgreSQL is available by attempting a socket connection.
     */
    private static boolean isLocalPostgreSQLAvailable() {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", 5432), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
