package dev.mars.apex.playground.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.apex.playground.model.DataSourceConnection;
import dev.mars.apex.playground.model.DataSourceConnection.DatabaseType;
import dev.mars.apex.playground.model.QueryRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DataSourceController with H2 and PostgreSQL databases.
 * Tests all CRUD operations and query execution via REST API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataSourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String h2ConnectionId;
    private static String postgresConnectionId;

    // API paths
    private static final String API_BASE = "/playground/api/datasources";
    
    // Test data
    private static final String H2_CONNECTION_NAME = "Test H2 Database";
    private static final String POSTGRES_CONNECTION_NAME = "Test PostgreSQL Database";
    private static final String POSTGRES_HOST = "localhost";
    private static final int POSTGRES_PORT = 5432;
    private static final String POSTGRES_DATABASE = "postgres";
    private static final String POSTGRES_USERNAME = "postgres";
    private static final String POSTGRES_PASSWORD = "postgres";

    @BeforeAll
    static void setupTestData() {
        System.out.println("\n========================================");
        System.out.println("Data Sources Integration Test Suite");
        System.out.println("========================================\n");
    }

    @Test
    @Order(1)
    @DisplayName("1. Create H2 Connection")
    void testCreateH2Connection() throws Exception {
        DataSourceConnection h2Connection = new DataSourceConnection();
        h2Connection.setName(H2_CONNECTION_NAME);
        h2Connection.setType(DatabaseType.H2);
        h2Connection.setDatabase("mem:testdb");
        h2Connection.setUsername("sa");
        h2Connection.setPassword("");

        String result = mockMvc.perform(post(API_BASE + "/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(h2Connection)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(H2_CONNECTION_NAME))
                .andExpect(jsonPath("$.type").value("H2"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        DataSourceConnection created = objectMapper.readValue(result, DataSourceConnection.class);
        h2ConnectionId = created.getId();

        System.out.println("✓ H2 Connection created with ID: " + h2ConnectionId);
    }

    @Test
    @Order(2)
    @DisplayName("2. Test H2 Connection")
    void testH2Connection() throws Exception {
        DataSourceConnection h2Connection = new DataSourceConnection();
        h2Connection.setName(H2_CONNECTION_NAME);
        h2Connection.setType(DatabaseType.H2);
        h2Connection.setDatabase("mem:testdb");
        h2Connection.setUsername("sa");
        h2Connection.setPassword("");

        mockMvc.perform(post(API_BASE + "/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(h2Connection)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

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

        mockMvc.perform(post(API_BASE + "/connections/{connectionId}/query", h2ConnectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTable)))
                .andDo(print())
                .andExpect(status().isOk());

        // Insert test data
        String[] inserts = {
                "INSERT INTO employees VALUES (1, 'Alice Smith', 'Engineering', 85000.00)",
                "INSERT INTO employees VALUES (2, 'Bob Johnson', 'Sales', 65000.00)",
                "INSERT INTO employees VALUES (3, 'Carol Williams', 'Engineering', 90000.00)",
                "INSERT INTO employees VALUES (4, 'David Brown', 'Marketing', 70000.00)",
                "INSERT INTO employees VALUES (5, 'Eve Davis', 'Engineering', 95000.00)"
        };

        for (String sql : inserts) {
            mockMvc.perform(post(API_BASE + "/connections/{connectionId}/query", h2ConnectionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new QueryRequest(sql))))
                    .andExpect(status().isOk());
        }

        System.out.println("✓ H2 Test data created: employees table with 5 rows");
    }

    @Test
    @Order(4)
    @DisplayName("4. Execute Query on H2")
    void testExecuteQueryOnH2() throws Exception {
        QueryRequest queryRequest = new QueryRequest("SELECT * FROM employees ORDER BY id");

        mockMvc.perform(post(API_BASE + "/connections/{connectionId}/query", h2ConnectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns").isArray())
                .andExpect(jsonPath("$.columns", hasSize(4)))
                .andExpect(jsonPath("$.rows").isArray())
                .andExpect(jsonPath("$.rows", hasSize(5)))
                .andExpect(jsonPath("$.rowCount").value(5));

        System.out.println("✓ Query executed on H2: Retrieved 5 employees");
    }

    @Test
    @Order(5)
    @DisplayName("5. Execute Filtered Query on H2")
    void testExecuteFilteredQueryOnH2() throws Exception {
        QueryRequest queryRequest = new QueryRequest(
                "SELECT name, salary FROM employees WHERE department = 'Engineering' ORDER BY salary DESC");

        mockMvc.perform(post(API_BASE + "/connections/{connectionId}/query", h2ConnectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns", hasSize(2)))
                .andExpect(jsonPath("$.rows", hasSize(3)))
                .andExpect(jsonPath("$.rows[0][0]").value("Eve Davis"));

        System.out.println("✓ Filtered query executed: 3 Engineering employees");
    }

    @Test
    @Order(6)
    @DisplayName("6. Get H2 Schema")
    void testGetH2Schema() throws Exception {
        mockMvc.perform(get(API_BASE + "/connections/{id}/schema", h2ConnectionId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database").value(anyOf(equalTo("testdb"), equalTo("TESTDB"))))
                .andExpect(jsonPath("$.tables").isArray());

        System.out.println("✓ H2 Schema retrieved successfully");
    }

    @Test
    @Order(7)
    @DisplayName("7. List All Connections")
    void testListConnections() throws Exception {
        mockMvc.perform(get(API_BASE + "/connections"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        System.out.println("✓ Connection list retrieved");
    }

    @Test
    @Order(8)
    @DisplayName("8. Create PostgreSQL Connection")
    void testCreatePostgreSQLConnection() throws Exception {
        DataSourceConnection pgConnection = new DataSourceConnection();
        pgConnection.setName(POSTGRES_CONNECTION_NAME);
        pgConnection.setType(DatabaseType.POSTGRESQL);
        pgConnection.setHost(POSTGRES_HOST);
        pgConnection.setPort(POSTGRES_PORT);
        pgConnection.setDatabase(POSTGRES_DATABASE);
        pgConnection.setUsername(POSTGRES_USERNAME);
        pgConnection.setPassword(POSTGRES_PASSWORD);

        try {
            String result = mockMvc.perform(post(API_BASE + "/connections")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(pgConnection)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value(POSTGRES_CONNECTION_NAME))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            DataSourceConnection created = objectMapper.readValue(result, DataSourceConnection.class);
            postgresConnectionId = created.getId();

            System.out.println("✓ PostgreSQL Connection created with ID: " + postgresConnectionId);
        } catch (Exception e) {
            System.out.println("⚠ PostgreSQL connection skipped - server not available");
            Assumptions.assumeTrue(false, "PostgreSQL not available");
        }
    }

    @Test
    @Order(9)
    @DisplayName("9. Test PostgreSQL Connection")
    void testPostgreSQLConnection() throws Exception {
        Assumptions.assumeTrue(postgresConnectionId != null, "PostgreSQL not created");

        DataSourceConnection pgConnection = new DataSourceConnection();
        pgConnection.setType(DatabaseType.POSTGRESQL);
        pgConnection.setHost(POSTGRES_HOST);
        pgConnection.setPort(POSTGRES_PORT);
        pgConnection.setDatabase(POSTGRES_DATABASE);
        pgConnection.setUsername(POSTGRES_USERNAME);
        pgConnection.setPassword(POSTGRES_PASSWORD);

        mockMvc.perform(post(API_BASE + "/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pgConnection)))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✓ PostgreSQL Connection tested");
    }

    @Test
    @Order(10)
    @DisplayName("10. Execute Query on PostgreSQL")
    void testExecuteQueryOnPostgreSQL() throws Exception {
        Assumptions.assumeTrue(postgresConnectionId != null, "PostgreSQL not created");

        QueryRequest queryRequest = new QueryRequest(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' LIMIT 5");

        mockMvc.perform(post(API_BASE + "/connections/{connectionId}/query", postgresConnectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns").isArray());

        System.out.println("✓ Query executed on PostgreSQL");
    }

    @Test
    @Order(11)
    @DisplayName("11. Get Single Connection")
    void testGetConnection() throws Exception {
        mockMvc.perform(get(API_BASE + "/connections/{id}", h2ConnectionId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(h2ConnectionId))
                .andExpect(jsonPath("$.name").value(H2_CONNECTION_NAME));

        System.out.println("✓ Single connection retrieved");
    }

    @Test
    @Order(98)
    @DisplayName("98. Delete PostgreSQL Connection")
    void testDeletePostgreSQLConnection() throws Exception {
        if (postgresConnectionId != null) {
            mockMvc.perform(delete(API_BASE + "/connections/{id}", postgresConnectionId))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            System.out.println("✓ PostgreSQL Connection deleted");
        }
    }

    @Test
    @Order(99)
    @DisplayName("99. Delete H2 Connection")
    void testDeleteH2Connection() throws Exception {
        mockMvc.perform(delete(API_BASE + "/connections/{id}", h2ConnectionId))
                .andDo(print())
                .andExpect(status().isNoContent());

        mockMvc.perform(get(API_BASE + "/connections/{id}", h2ConnectionId))
                .andExpect(status().isNotFound());

        System.out.println("✓ H2 Connection deleted");
    }

    @AfterAll
    static void printSummary() {
        System.out.println("\n========================================");
        System.out.println("Data Sources Tests Complete");
        System.out.println("========================================\n");
    }
}
