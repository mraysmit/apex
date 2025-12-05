package dev.mars.apex.playground.service;

import dev.mars.apex.playground.model.DataSourceConnection;
import dev.mars.apex.playground.model.QueryRequest;
import dev.mars.apex.playground.model.QueryResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Debug test for DataSourceService to isolate query execution issues.
 */
class DataSourceServiceDebugTest {

    @Test
    void testH2QueryExecution() {
        DataSourceService service = new DataSourceService();
        
        // Create H2 connection
        DataSourceConnection h2Connection = new DataSourceConnection();
        h2Connection.setName("Debug H2");
        h2Connection.setType(DataSourceConnection.DatabaseType.H2);
        h2Connection.setDatabase("mem:debugdb");
        h2Connection.setUsername("sa");
        h2Connection.setPassword("");
        
        DataSourceConnection created = service.createConnection(h2Connection);
        String connectionId = created.getId();
        
        System.out.println("Created connection with ID: " + connectionId);
        assertNotNull(connectionId);
        
        // Create table
        QueryRequest createTable = new QueryRequest(
            "CREATE TABLE test (id INT PRIMARY KEY, name VARCHAR(100))");
        
        try {
            QueryResult result1 = service.executeQuery(connectionId, createTable);
            System.out.println("Create table result: " + result1);
            assertNotNull(result1);
        } catch (Exception e) {
            System.err.println("CREATE TABLE failed: " + e.getMessage());
            e.printStackTrace();
            fail("CREATE TABLE should not throw exception: " + e.getMessage());
        }
        
        // Insert data
        QueryRequest insert = new QueryRequest(
            "INSERT INTO test VALUES (1, 'Alice')");
        
        try {
            QueryResult result2 = service.executeQuery(connectionId, insert);
            System.out.println("Insert result: " + result2);
            assertNotNull(result2);
        } catch (Exception e) {
            System.err.println("INSERT failed: " + e.getMessage());
            e.printStackTrace();
            fail("INSERT should not throw exception: " + e.getMessage());
        }
        
        // Select data
        QueryRequest select = new QueryRequest("SELECT * FROM test");
        
        try {
            QueryResult result3 = service.executeQuery(connectionId, select);
            System.out.println("Select result: " + result3);
            assertNotNull(result3);
            assertEquals(1, result3.getRowCount());
        } catch (Exception e) {
            System.err.println("SELECT failed: " + e.getMessage());
            e.printStackTrace();
            fail("SELECT should not throw exception: " + e.getMessage());
        }
    }
}
