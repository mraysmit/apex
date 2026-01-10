package dev.mars.apex.sync;

/**
 * Global constants for TestContainers and Integration Tests.
 */
public class TestConstants {
    public static final String MSSQL_IMAGE = "mcr.microsoft.com/mssql/server:2022-latest";
    public static final String POSTGRES_IMAGE = "postgres:15-alpine"; // 15-alpine is smaller and sufficient
}
