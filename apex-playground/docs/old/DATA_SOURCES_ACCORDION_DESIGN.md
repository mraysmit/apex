# Data Sources Accordion - Design & Implementation Document

**Date:** December 5, 2025  
**Component:** APEX Visual Rule Editor - Data Sources Management  
**Version:** 1.0

---

## 1. Overview

### 1.1 Purpose
Add a new accordion section to `apex_editor_main.html` that enables users to:
- Create and manage database connections
- Execute SQL queries against connected databases
- View query results in a tabular format
- Load database schema information into Blockly Field blocks
- Integrate database metadata with APEX data-source blocks

### 1.2 Design Philosophy
Mirror the successful "Evaluation Data Sets" pattern:
- **Three-tab interface** for different interaction modes
- **Server-side connection management** for security and persistence
- **Live validation and testing** of connections
- **Seamless integration** with Blockly workspace

---

## 2. Architecture

### 2.1 Component Stack

```
┌─────────────────────────────────────────────────────┐
│  Frontend (apex_editor_main.html)                   │
│  ┌───────────────────────────────────────────────┐  │
│  │ Data Sources Accordion Section                │  │
│  │  • SQL Editor Tab                             │  │
│  │  • Table View Tab                             │  │
│  │  • Connections Tab                            │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                         ↕ REST API
┌─────────────────────────────────────────────────────┐
│  Backend (Spring Boot - apex-playground)            │
│  ┌───────────────────────────────────────────────┐  │
│  │ DataSourceController                          │  │
│  │  /playground/api/datasources/*                │  │
│  └───────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────┐  │
│  │ DataSourceService                             │  │
│  │  • Connection pooling (HikariCP)              │  │
│  │  • Query execution                            │  │
│  │  • Schema introspection                       │  │
│  └───────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────┐  │
│  │ DataSourceRepository                          │  │
│  │  • In-memory connection registry              │  │
│  │  • Optional file persistence (JSON)           │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### 2.2 Technology Choices

**Frontend:**
- HTML5 + Bootstrap 5.3
- Vanilla JavaScript (consistency with existing code)
- Dark theme matching Visual Editor aesthetic

**Backend:**
- Spring Boot 3.x (existing apex-playground infrastructure)
- HikariCP for connection pooling
- JDBC for database connectivity
- Support for: PostgreSQL, MySQL, Oracle, SQL Server, H2

---

## 3. Frontend Design

### 3.1 HTML Structure

```html
<!-- Data Sources Accordion Section -->
<div id="dataSourcesSection" class="accordion-section">
    <div class="accordion-header" onclick="toggleAccordion('dataSourcesSection')">
        <h2><span class="accordion-arrow">&#9654;</span> Data Sources</h2>
        <div class="d-flex gap-1 align-items-center" onclick="event.stopPropagation()">
            <button class="btn btn-success btn-sm" onclick="loadSchemaIntoEditor()" 
                    title="Load database schema into Field blocks">Load Schema</button>
            <button class="btn btn-outline-light btn-sm" onclick="executeQuery()" 
                    title="Execute SQL query">Execute</button>
            <button class="btn btn-outline-light btn-sm" onclick="clearDataSources()" 
                    title="Clear all">Clear</button>
        </div>
    </div>
    <div class="accordion-content">
        <div id="dataSourcesOutput">
            <!-- Tabs -->
            <ul class="nav nav-tabs nav-tabs-dark" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link active" onclick="switchDataSourceTab('sql')">SQL Editor</button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" onclick="switchDataSourceTab('table')">Table View</button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" onclick="switchDataSourceTab('connections')">Connections</button>
                </li>
            </ul>

            <!-- SQL Editor Panel -->
            <div id="sqlEditorPanel" class="datasource-panel active">
                <div class="sql-editor-toolbar">
                    <select id="activeConnectionSelect" class="form-select form-select-sm">
                        <option value="">Select connection...</option>
                    </select>
                    <div class="btn-group btn-group-sm ms-2">
                        <button class="btn btn-outline-light" onclick="formatSql()">Format</button>
                        <button class="btn btn-outline-light" onclick="loadSampleQuery()">Sample</button>
                    </div>
                </div>
                <textarea id="sqlEditor" class="form-control bg-dark text-light font-monospace" 
                          placeholder="SELECT * FROM customers WHERE ..."
                          rows="10"></textarea>
                <div id="sqlError" class="alert alert-danger mt-2 small" style="display: none;"></div>
                <div id="queryStats" class="query-stats mt-2" style="display: none;">
                    <small class="text-muted">
                        <span id="rowCount">0</span> rows • 
                        <span id="execTime">0</span>ms
                    </small>
                </div>
            </div>

            <!-- Table View Panel -->
            <div id="tableViewPanel" class="datasource-panel">
                <div class="table-responsive datasource-table-container">
                    <table id="queryResultsTable" class="table table-dark table-sm table-hover">
                        <thead id="tableHeaders"></thead>
                        <tbody id="tableBody"></tbody>
                    </table>
                    <div id="emptyTableMessage" class="text-muted fst-italic text-center py-4">
                        Execute a query to see results
                    </div>
                </div>
                <div class="table-pagination mt-2" id="tablePagination" style="display: none;">
                    <button class="btn btn-sm btn-outline-light" onclick="loadPreviousPage()">Previous</button>
                    <span class="mx-3 text-muted">Page <span id="currentPage">1</span></span>
                    <button class="btn btn-sm btn-outline-light" onclick="loadNextPage()">Next</button>
                </div>
            </div>

            <!-- Connections Panel -->
            <div id="connectionsPanel" class="datasource-panel">
                <div class="connection-actions mb-3">
                    <button class="btn btn-primary" onclick="showCreateConnectionDialog()">
                        <i class="fas fa-plus"></i> Create Connection
                    </button>
                    <button class="btn btn-outline-light" onclick="refreshConnections()">
                        <i class="fas fa-sync"></i> Refresh
                    </button>
                </div>
                <div class="connection-list" id="connectionList">
                    <!-- Dynamically populated connection cards -->
                </div>
            </div>
        </div>
    </div>
</div>
```

### 3.2 CSS Styling

```css
/* Data Sources Accordion */
.datasource-panel {
    display: none;
    flex: 1;
    overflow: auto;
    padding: 0.75rem;
}

.datasource-panel.active {
    display: flex;
    flex-direction: column;
}

/* SQL Editor */
#sqlEditor {
    flex: 1;
    min-height: 200px;
    background: #1a1a1a;
    border: 1px solid #3a3a3a;
    border-radius: 4px;
    font-family: 'Courier New', monospace;
    font-size: 13px;
    color: #d4d4d4;
    padding: 0.5rem;
    resize: vertical;
    line-height: 1.5;
}

#sqlEditor:focus {
    outline: none;
    border-color: #3498db;
}

.sql-editor-toolbar {
    display: flex;
    gap: 0.5rem;
    margin-bottom: 0.5rem;
    align-items: center;
}

#activeConnectionSelect {
    max-width: 250px;
    background: #2a2a2a;
    border: 1px solid #3a3a3a;
    color: #d4d4d4;
}

.query-stats {
    display: flex;
    justify-content: space-between;
    padding: 0.5rem;
    background: #2a2a2a;
    border-radius: 4px;
}

/* Table View */
.datasource-table-container {
    flex: 1;
    overflow: auto;
    max-height: 400px;
}

.table-dark {
    color: #d4d4d4;
    background-color: #1a1a1a;
}

.table-dark thead th {
    background-color: #2a2a2a;
    border-color: #3a3a3a;
    font-weight: 600;
    position: sticky;
    top: 0;
    z-index: 10;
}

.table-dark tbody td {
    border-color: #3a3a3a;
    font-family: 'Courier New', monospace;
    font-size: 12px;
}

.table-dark tbody tr:hover {
    background-color: #2a2a2a;
}

/* Connections List */
.connection-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.connection-card {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem;
    background: #2a2a2a;
    border: 1px solid #3a3a3a;
    border-radius: 6px;
    transition: all 0.2s;
}

.connection-card:hover {
    background: #333;
    border-color: #555;
}

.connection-card.active {
    border-color: #3498db;
    background: rgba(52, 152, 219, 0.1);
}

.connection-info {
    flex: 1;
}

.connection-name {
    color: #d4d4d4;
    font-size: 1rem;
    font-weight: 600;
    margin-bottom: 0.25rem;
}

.connection-details {
    color: #888;
    font-size: 0.85rem;
}

.connection-status {
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
    padding: 0.25rem 0.5rem;
    border-radius: 12px;
    font-size: 0.75rem;
    margin-top: 0.25rem;
}

.connection-status.connected {
    background: rgba(26, 188, 156, 0.2);
    color: #1abc9c;
}

.connection-status.disconnected {
    background: rgba(231, 76, 60, 0.2);
    color: #e74c3c;
}

.connection-actions {
    display: flex;
    gap: 0.5rem;
    align-items: center;
}

.connection-card .btn {
    padding: 0.25rem 0.75rem;
    font-size: 0.85rem;
}

/* Connection Dialog */
.connection-dialog {
    background: #2d2d2d;
}

.connection-dialog .modal-content {
    background: #2d2d2d;
    border: 1px solid #3a3a3a;
}

.connection-dialog .form-label {
    color: #d4d4d4;
    font-size: 0.9rem;
    margin-bottom: 0.25rem;
}

.connection-dialog .form-control,
.connection-dialog .form-select {
    background: #1a1a1a;
    border: 1px solid #3a3a3a;
    color: #d4d4d4;
}

.connection-dialog .form-control:focus,
.connection-dialog .form-select:focus {
    background: #1a1a1a;
    border-color: #3498db;
    color: #d4d4d4;
    box-shadow: 0 0 0 0.25rem rgba(52, 152, 219, 0.25);
}

.test-connection-result {
    padding: 0.75rem;
    border-radius: 4px;
    margin-top: 0.5rem;
}

.test-connection-result.success {
    background: rgba(26, 188, 156, 0.2);
    color: #1abc9c;
}

.test-connection-result.error {
    background: rgba(231, 76, 60, 0.2);
    color: #e74c3c;
}
```

### 3.3 JavaScript Functions

```javascript
// --- Data Sources Management ---

let dataSources = [];
let activeDataSource = null;
let queryResults = null;
let currentPage = 1;
let pageSize = 50;

// Tab Switching
function switchDataSourceTab(tabName) {
    document.querySelectorAll('.nav-tabs-dark .nav-link').forEach(tab => {
        tab.classList.remove('active');
    });
    event.target.classList.add('active');

    document.querySelectorAll('.datasource-panel').forEach(panel => {
        panel.classList.remove('active');
    });

    if (tabName === 'sql') {
        document.getElementById('sqlEditorPanel').classList.add('active');
    } else if (tabName === 'table') {
        document.getElementById('tableViewPanel').classList.add('active');
    } else if (tabName === 'connections') {
        document.getElementById('connectionsPanel').classList.add('active');
        refreshConnections();
    }
}

// Connection Management
async function showCreateConnectionDialog() {
    const modal = new bootstrap.Modal(document.getElementById('createConnectionModal'));
    modal.show();
}

async function createConnection() {
    const form = document.getElementById('connectionForm');
    const formData = {
        name: form.connectionName.value,
        type: form.dbType.value,
        host: form.dbHost.value,
        port: parseInt(form.dbPort.value),
        database: form.dbName.value,
        username: form.dbUsername.value,
        password: form.dbPassword.value,
        properties: parseConnectionProperties(form.dbProperties.value)
    };

    try {
        const response = await fetch('/playground/api/datasources/connections', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });

        if (response.ok) {
            const connection = await response.json();
            showToast('Connection created successfully', 'success');
            bootstrap.Modal.getInstance(document.getElementById('createConnectionModal')).hide();
            refreshConnections();
        } else {
            const error = await response.text();
            showToast('Failed to create connection: ' + error, 'error');
        }
    } catch (error) {
        showToast('Error creating connection: ' + error.message, 'error');
    }
}

async function testConnection() {
    const form = document.getElementById('connectionForm');
    const testButton = document.getElementById('testConnectionBtn');
    const resultDiv = document.getElementById('testConnectionResult');
    
    testButton.disabled = true;
    testButton.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Testing...';

    const formData = {
        type: form.dbType.value,
        host: form.dbHost.value,
        port: parseInt(form.dbPort.value),
        database: form.dbName.value,
        username: form.dbUsername.value,
        password: form.dbPassword.value
    };

    try {
        const response = await fetch('/playground/api/datasources/test', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });

        const result = await response.json();
        
        if (result.success) {
            resultDiv.className = 'test-connection-result success';
            resultDiv.innerHTML = '<i class="fas fa-check-circle"></i> Connection successful!';
        } else {
            resultDiv.className = 'test-connection-result error';
            resultDiv.innerHTML = '<i class="fas fa-times-circle"></i> ' + result.message;
        }
        resultDiv.style.display = 'block';
    } catch (error) {
        resultDiv.className = 'test-connection-result error';
        resultDiv.innerHTML = '<i class="fas fa-times-circle"></i> ' + error.message;
        resultDiv.style.display = 'block';
    } finally {
        testButton.disabled = false;
        testButton.innerHTML = '<i class="fas fa-plug"></i> Test Connection';
    }
}

async function refreshConnections() {
    try {
        const response = await fetch('/playground/api/datasources/connections');
        dataSources = await response.json();
        renderConnectionList();
        updateConnectionSelect();
    } catch (error) {
        showToast('Error loading connections: ' + error.message, 'error');
    }
}

function renderConnectionList() {
    const list = document.getElementById('connectionList');
    
    if (dataSources.length === 0) {
        list.innerHTML = '<div class="text-muted text-center py-4">No connections configured. Click "Create Connection" to add one.</div>';
        return;
    }

    let html = '';
    dataSources.forEach(conn => {
        const isActive = activeDataSource && activeDataSource.id === conn.id;
        html += `
            <div class="connection-card ${isActive ? 'active' : ''}">
                <div class="connection-info">
                    <div class="connection-name">${escapeHtml(conn.name)}</div>
                    <div class="connection-details">
                        ${conn.type} • ${conn.host}:${conn.port} • ${conn.database}
                    </div>
                    <div class="connection-status ${conn.connected ? 'connected' : 'disconnected'}">
                        <span class="status-dot">●</span>
                        ${conn.connected ? 'Connected' : 'Disconnected'}
                    </div>
                </div>
                <div class="connection-actions">
                    <button class="btn btn-sm btn-outline-success" onclick="selectConnection('${conn.id}')">
                        ${isActive ? 'Selected' : 'Select'}
                    </button>
                    <button class="btn btn-sm btn-outline-primary" onclick="editConnection('${conn.id}')">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteConnection('${conn.id}')">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
        `;
    });

    list.innerHTML = html;
}

function updateConnectionSelect() {
    const select = document.getElementById('activeConnectionSelect');
    
    select.innerHTML = '<option value="">Select connection...</option>';
    dataSources.forEach(conn => {
        const option = document.createElement('option');
        option.value = conn.id;
        option.textContent = `${conn.name} (${conn.type})`;
        if (activeDataSource && activeDataSource.id === conn.id) {
            option.selected = true;
        }
        select.appendChild(option);
    });
}

async function selectConnection(connectionId) {
    const connection = dataSources.find(c => c.id === connectionId);
    if (connection) {
        activeDataSource = connection;
        renderConnectionList();
        updateConnectionSelect();
        showToast(`Selected connection: ${connection.name}`, 'info');
    }
}

async function deleteConnection(connectionId) {
    if (!confirm('Delete this connection?')) return;

    try {
        const response = await fetch(`/playground/api/datasources/connections/${connectionId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showToast('Connection deleted', 'success');
            if (activeDataSource && activeDataSource.id === connectionId) {
                activeDataSource = null;
            }
            refreshConnections();
        }
    } catch (error) {
        showToast('Error deleting connection: ' + error.message, 'error');
    }
}

// Query Execution
async function executeQuery() {
    if (!activeDataSource) {
        showToast('Please select a connection first', 'warning');
        return;
    }

    const sql = document.getElementById('sqlEditor').value.trim();
    if (!sql) {
        showToast('Please enter a SQL query', 'warning');
        return;
    }

    const startTime = Date.now();
    
    try {
        const response = await fetch(`/playground/api/datasources/connections/${activeDataSource.id}/query`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sql: sql, limit: pageSize, offset: (currentPage - 1) * pageSize })
        });

        if (!response.ok) {
            const error = await response.text();
            document.getElementById('sqlError').textContent = 'Query Error: ' + error;
            document.getElementById('sqlError').style.display = 'block';
            return;
        }

        queryResults = await response.json();
        const execTime = Date.now() - startTime;

        document.getElementById('sqlError').style.display = 'none';
        document.getElementById('rowCount').textContent = queryResults.rowCount;
        document.getElementById('execTime').textContent = execTime;
        document.getElementById('queryStats').style.display = 'block';

        renderTableView();
        switchDataSourceTab('table');

    } catch (error) {
        document.getElementById('sqlError').textContent = 'Error: ' + error.message;
        document.getElementById('sqlError').style.display = 'block';
    }
}

function renderTableView() {
    if (!queryResults || !queryResults.rows || queryResults.rows.length === 0) {
        document.getElementById('emptyTableMessage').style.display = 'block';
        document.getElementById('queryResultsTable').style.display = 'none';
        return;
    }

    document.getElementById('emptyTableMessage').style.display = 'none';
    document.getElementById('queryResultsTable').style.display = 'table';

    // Render headers
    const headers = document.getElementById('tableHeaders');
    let headerHtml = '<tr>';
    queryResults.columns.forEach(col => {
        headerHtml += `<th>${escapeHtml(col)}</th>`;
    });
    headerHtml += '</tr>';
    headers.innerHTML = headerHtml;

    // Render rows
    const tbody = document.getElementById('tableBody');
    let bodyHtml = '';
    queryResults.rows.forEach(row => {
        bodyHtml += '<tr>';
        row.forEach(cell => {
            const cellValue = cell === null ? '<span class="text-muted">NULL</span>' : escapeHtml(String(cell));
            bodyHtml += `<td>${cellValue}</td>`;
        });
        bodyHtml += '</tr>';
    });
    tbody.innerHTML = bodyHtml;

    // Update pagination
    if (queryResults.hasMore) {
        document.getElementById('tablePagination').style.display = 'flex';
        document.getElementById('currentPage').textContent = currentPage;
    }
}

// Schema Loading
async function loadSchemaIntoEditor() {
    if (!activeDataSource) {
        showToast('Please select a connection first', 'warning');
        return;
    }

    try {
        const response = await fetch(`/playground/api/datasources/connections/${activeDataSource.id}/schema`);
        const schema = await response.json();

        const fieldPaths = [];
        schema.tables.forEach(table => {
            table.columns.forEach(column => {
                fieldPaths.push(`${table.name}.${column.name}`);
            });
        });

        loadedFieldPaths = fieldPaths;
        loadedFieldPaths.sort();

        showToast(`Loaded ${fieldPaths.length} fields from ${schema.tables.length} tables`, 'success', 
                  'Database Schema Loaded', 5000);

    } catch (error) {
        showToast('Error loading schema: ' + error.message, 'error');
    }
}

// Utility Functions
function formatSql() {
    // Simple SQL formatting (can be enhanced with a library)
    const sql = document.getElementById('sqlEditor').value;
    const formatted = sql
        .replace(/\bSELECT\b/gi, 'SELECT')
        .replace(/\bFROM\b/gi, '\nFROM')
        .replace(/\bWHERE\b/gi, '\nWHERE')
        .replace(/\bAND\b/gi, '\n  AND')
        .replace(/\bOR\b/gi, '\n  OR')
        .replace(/\bORDER BY\b/gi, '\nORDER BY')
        .replace(/\bGROUP BY\b/gi, '\nGROUP BY');
    document.getElementById('sqlEditor').value = formatted;
}

function loadSampleQuery() {
    const samples = [
        'SELECT * FROM customers WHERE active = true LIMIT 10',
        'SELECT product_id, SUM(quantity) as total\nFROM orders\nGROUP BY product_id\nORDER BY total DESC',
        'SELECT c.name, o.order_date, o.total\nFROM customers c\nJOIN orders o ON c.id = o.customer_id\nWHERE o.total > 1000'
    ];
    document.getElementById('sqlEditor').value = samples[Math.floor(Math.random() * samples.length)];
}

function clearDataSources() {
    if (confirm('Clear SQL editor and results?')) {
        document.getElementById('sqlEditor').value = '';
        document.getElementById('sqlError').style.display = 'none';
        document.getElementById('queryStats').style.display = 'none';
        queryResults = null;
        currentPage = 1;
        renderTableView();
    }
}

function parseConnectionProperties(propsText) {
    if (!propsText || !propsText.trim()) return {};
    
    const props = {};
    propsText.split('\n').forEach(line => {
        const [key, value] = line.split('=').map(s => s.trim());
        if (key && value) {
            props[key] = value;
        }
    });
    return props;
}
```

---

## 4. Backend Design

### 4.1 Model Classes

#### **DataSourceConnection.java**
```java
package dev.mars.apex.playground.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

public class DataSourceConnection {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("type")
    private DatabaseType type;
    
    @JsonProperty("host")
    private String host;
    
    @JsonProperty("port")
    private int port;
    
    @JsonProperty("database")
    private String database;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("password")
    private String password; // Encrypted in production
    
    @JsonProperty("properties")
    private Map<String, String> properties;
    
    @JsonProperty("connected")
    private boolean connected;
    
    @JsonProperty("createdAt")
    private Instant createdAt;
    
    @JsonProperty("lastUsed")
    private Instant lastUsed;
    
    public enum DatabaseType {
        POSTGRESQL, MYSQL, ORACLE, SQLSERVER, H2
    }
    
    // Getters, setters, constructors...
}
```

#### **QueryRequest.java**
```java
package dev.mars.apex.playground.model;

public class QueryRequest {
    private String sql;
    private int limit = 100;
    private int offset = 0;
    
    // Getters, setters...
}
```

#### **QueryResult.java**
```java
package dev.mars.apex.playground.model;

import java.util.List;

public class QueryResult {
    private List<String> columns;
    private List<List<Object>> rows;
    private int rowCount;
    private boolean hasMore;
    private long executionTimeMs;
    
    // Getters, setters...
}
```

#### **DatabaseSchema.java**
```java
package dev.mars.apex.playground.model;

import java.util.List;

public class DatabaseSchema {
    private String database;
    private List<TableInfo> tables;
    
    public static class TableInfo {
        private String name;
        private String schema;
        private List<ColumnInfo> columns;
        
        // Getters, setters...
    }
    
    public static class ColumnInfo {
        private String name;
        private String type;
        private boolean nullable;
        private boolean primaryKey;
        
        // Getters, setters...
    }
    
    // Getters, setters...
}
```

### 4.2 Service Layer

#### **DataSourceService.java**
```java
package dev.mars.apex.playground.service;

@Service
public class DataSourceService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceService.class);
    
    private final Map<String, HikariDataSource> connectionPools = new ConcurrentHashMap<>();
    private final Map<String, DataSourceConnection> connections = new ConcurrentHashMap<>();
    
    /**
     * Create a new database connection
     */
    public DataSourceConnection createConnection(DataSourceConnection connection) {
        connection.setId(UUID.randomUUID().toString());
        connection.setCreatedAt(Instant.now());
        
        // Create connection pool
        HikariDataSource dataSource = createDataSource(connection);
        
        // Test connection
        try (Connection conn = dataSource.getConnection()) {
            connection.setConnected(true);
        } catch (SQLException e) {
            dataSource.close();
            throw new RuntimeException("Failed to connect: " + e.getMessage());
        }
        
        connectionPools.put(connection.getId(), dataSource);
        connections.put(connection.getId(), connection);
        
        logger.info("Created connection: {} ({})", connection.getName(), connection.getId());
        return connection;
    }
    
    /**
     * Test a connection without creating it
     */
    public boolean testConnection(DataSourceConnection connection) {
        HikariDataSource testDataSource = null;
        try {
            testDataSource = createDataSource(connection);
            try (Connection conn = testDataSource.getConnection()) {
                return conn.isValid(5);
            }
        } catch (Exception e) {
            logger.warn("Connection test failed: {}", e.getMessage());
            return false;
        } finally {
            if (testDataSource != null) {
                testDataSource.close();
            }
        }
    }
    
    /**
     * Execute a query
     */
    public QueryResult executeQuery(String connectionId, QueryRequest request) {
        HikariDataSource dataSource = getDataSource(connectionId);
        DataSourceConnection connection = connections.get(connectionId);
        
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Set limits
            stmt.setMaxRows(request.getLimit() + 1); // +1 to check for more rows
            
            try (ResultSet rs = stmt.executeQuery(request.getSql())) {
                QueryResult result = new QueryResult();
                
                // Get column metadata
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(metaData.getColumnName(i));
                }
                result.setColumns(columns);
                
                // Get rows
                List<List<Object>> rows = new ArrayList<>();
                int rowCount = 0;
                
                while (rs.next() && rowCount < request.getLimit()) {
                    List<Object> row = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    rows.add(row);
                    rowCount++;
                }
                
                // Check if there are more rows
                result.setHasMore(rs.next());
                
                result.setRows(rows);
                result.setRowCount(rowCount);
                result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                
                connection.setLastUsed(Instant.now());
                
                return result;
            }
            
        } catch (SQLException e) {
            logger.error("Query execution failed", e);
            throw new RuntimeException("Query failed: " + e.getMessage());
        }
    }
    
    /**
     * Get database schema
     */
    public DatabaseSchema getSchema(String connectionId) {
        HikariDataSource dataSource = getDataSource(connectionId);
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            DatabaseSchema schema = new DatabaseSchema();
            schema.setDatabase(conn.getCatalog());
            
            List<DatabaseSchema.TableInfo> tables = new ArrayList<>();
            
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    DatabaseSchema.TableInfo table = new DatabaseSchema.TableInfo();
                    table.setName(rs.getString("TABLE_NAME"));
                    table.setSchema(rs.getString("TABLE_SCHEM"));
                    
                    // Get columns
                    List<DatabaseSchema.ColumnInfo> columns = new ArrayList<>();
                    try (ResultSet colRs = metaData.getColumns(null, table.getSchema(), table.getName(), "%")) {
                        while (colRs.next()) {
                            DatabaseSchema.ColumnInfo column = new DatabaseSchema.ColumnInfo();
                            column.setName(colRs.getString("COLUMN_NAME"));
                            column.setType(colRs.getString("TYPE_NAME"));
                            column.setNullable(colRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                            columns.add(column);
                        }
                    }
                    
                    // Get primary keys
                    try (ResultSet pkRs = metaData.getPrimaryKeys(null, table.getSchema(), table.getName())) {
                        Set<String> pkColumns = new HashSet<>();
                        while (pkRs.next()) {
                            pkColumns.add(pkRs.getString("COLUMN_NAME"));
                        }
                        columns.forEach(col -> col.setPrimaryKey(pkColumns.contains(col.getName())));
                    }
                    
                    table.setColumns(columns);
                    tables.add(table);
                }
            }
            
            schema.setTables(tables);
            return schema;
            
        } catch (SQLException e) {
            logger.error("Schema introspection failed", e);
            throw new RuntimeException("Failed to get schema: " + e.getMessage());
        }
    }
    
    /**
     * Get all connections
     */
    public List<DataSourceConnection> getAllConnections() {
        return new ArrayList<>(connections.values());
    }
    
    /**
     * Delete a connection
     */
    public void deleteConnection(String connectionId) {
        HikariDataSource dataSource = connectionPools.remove(connectionId);
        if (dataSource != null) {
            dataSource.close();
        }
        connections.remove(connectionId);
        logger.info("Deleted connection: {}", connectionId);
    }
    
    // Private helper methods
    
    private HikariDataSource createDataSource(DataSourceConnection connection) {
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(buildJdbcUrl(connection));
        config.setUsername(connection.getUsername());
        config.setPassword(connection.getPassword());
        
        // Connection pool settings
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(300000);
        
        // Add custom properties
        if (connection.getProperties() != null) {
            connection.getProperties().forEach(config::addDataSourceProperty);
        }
        
        return new HikariDataSource(config);
    }
    
    private String buildJdbcUrl(DataSourceConnection connection) {
        switch (connection.getType()) {
            case POSTGRESQL:
                return String.format("jdbc:postgresql://%s:%d/%s", 
                    connection.getHost(), connection.getPort(), connection.getDatabase());
            case MYSQL:
                return String.format("jdbc:mysql://%s:%d/%s", 
                    connection.getHost(), connection.getPort(), connection.getDatabase());
            case ORACLE:
                return String.format("jdbc:oracle:thin:@%s:%d:%s", 
                    connection.getHost(), connection.getPort(), connection.getDatabase());
            case SQLSERVER:
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", 
                    connection.getHost(), connection.getPort(), connection.getDatabase());
            case H2:
                return String.format("jdbc:h2:mem:%s", connection.getDatabase());
            default:
                throw new IllegalArgumentException("Unsupported database type: " + connection.getType());
        }
    }
    
    private HikariDataSource getDataSource(String connectionId) {
        HikariDataSource dataSource = connectionPools.get(connectionId);
        if (dataSource == null) {
            throw new IllegalArgumentException("Connection not found: " + connectionId);
        }
        return dataSource;
    }
}
```

### 4.3 Controller Layer

#### **DataSourceController.java**
```java
package dev.mars.apex.playground.controller;

@RestController
@RequestMapping("/playground/api/datasources")
@Tag(name = "Data Sources API", description = "Manage database connections and execute queries")
public class DataSourceController {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceController.class);
    
    private final DataSourceService dataSourceService;
    
    @Autowired
    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }
    
    /**
     * Get all connections
     */
    @GetMapping("/connections")
    @Operation(summary = "Get all database connections")
    public ResponseEntity<List<DataSourceConnection>> getAllConnections() {
        return ResponseEntity.ok(dataSourceService.getAllConnections());
    }
    
    /**
     * Create a new connection
     */
    @PostMapping("/connections")
    @Operation(summary = "Create a new database connection")
    public ResponseEntity<DataSourceConnection> createConnection(
            @RequestBody DataSourceConnection connection) {
        
        try {
            DataSourceConnection created = dataSourceService.createConnection(connection);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Failed to create connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }
    
    /**
     * Test a connection
     */
    @PostMapping("/test")
    @Operation(summary = "Test database connection without creating it")
    public ResponseEntity<Map<String, Object>> testConnection(
            @RequestBody DataSourceConnection connection) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = dataSourceService.testConnection(connection);
            response.put("success", success);
            if (success) {
                response.put("message", "Connection successful");
            } else {
                response.put("message", "Connection failed");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Delete a connection
     */
    @DeleteMapping("/connections/{connectionId}")
    @Operation(summary = "Delete a database connection")
    public ResponseEntity<Void> deleteConnection(@PathVariable String connectionId) {
        dataSourceService.deleteConnection(connectionId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Execute a query
     */
    @PostMapping("/connections/{connectionId}/query")
    @Operation(summary = "Execute SQL query")
    public ResponseEntity<QueryResult> executeQuery(
            @PathVariable String connectionId,
            @RequestBody QueryRequest request) {
        
        try {
            QueryResult result = dataSourceService.executeQuery(connectionId, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Query execution failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get database schema
     */
    @GetMapping("/connections/{connectionId}/schema")
    @Operation(summary = "Get database schema metadata")
    public ResponseEntity<DatabaseSchema> getSchema(@PathVariable String connectionId) {
        try {
            DatabaseSchema schema = dataSourceService.getSchema(connectionId);
            return ResponseEntity.ok(schema);
        } catch (Exception e) {
            logger.error("Schema introspection failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
```

---

## 5. Security Considerations

### 5.1 Password Management
- **Never log passwords**
- **Encrypt passwords at rest** using Spring Security Crypto
- **Use environment variables** for sensitive defaults
- **Clear passwords from memory** after use

### 5.2 SQL Injection Prevention
- **Parameterized queries only** - no string concatenation
- **Whitelist allowed SQL commands** for production
- **Query timeout limits** to prevent resource exhaustion
- **Row limits** enforced on all queries

### 5.3 Connection Security
- **SSL/TLS enforcement** for remote connections
- **Connection pooling limits** to prevent resource exhaustion
- **Automatic connection cleanup** on idle timeout
- **CORS configuration** for API endpoints

---

## 6. Testing Strategy

### 6.1 Unit Tests
```java
@Test
void shouldCreatePostgreSQLConnection() {
    DataSourceConnection connection = createTestConnection();
    DataSourceConnection created = dataSourceService.createConnection(connection);
    assertNotNull(created.getId());
    assertTrue(created.isConnected());
}

@Test
void shouldExecuteSimpleQuery() {
    QueryRequest request = new QueryRequest();
    request.setSql("SELECT 1 as value");
    QueryResult result = dataSourceService.executeQuery(connectionId, request);
    assertEquals(1, result.getRowCount());
}

@Test
void shouldGetDatabaseSchema() {
    DatabaseSchema schema = dataSourceService.getSchema(connectionId);
    assertNotNull(schema.getTables());
    assertTrue(schema.getTables().size() > 0);
}
```

### 6.2 Integration Tests
- Test with H2 embedded database
- Test with PostgreSQL testcontainer
- Test connection pooling behavior
- Test concurrent query execution

### 6.3 UI Tests
- Selenium tests for connection creation dialog
- Test SQL editor functionality
- Test table view rendering
- Test connection switching

---

## 7. Implementation Phases

### Phase 1: Backend Foundation (Week 1)
- [ ] Create model classes
- [ ] Implement DataSourceService
- [ ] Implement DataSourceController
- [ ] Add HikariCP dependency to pom.xml
- [ ] Write unit tests
- [ ] Test with H2 database

### Phase 2: Frontend Structure (Week 1)
- [ ] Add HTML accordion section
- [ ] Add CSS styling
- [ ] Implement tab switching
- [ ] Add connection dialog modal
- [ ] Test UI components

### Phase 3: Connection Management (Week 2)
- [ ] Implement connection CRUD operations
- [ ] Test connection functionality
- [ ] Connection list rendering
- [ ] Connection selection logic

### Phase 4: Query Execution (Week 2)
- [ ] SQL editor implementation
- [ ] Query execution logic
- [ ] Table view rendering
- [ ] Pagination support

### Phase 5: Schema Integration (Week 3)
- [ ] Schema introspection
- [ ] Load schema into Field blocks
- [ ] Integration with Blockly workspace
- [ ] Field path dropdown population

### Phase 6: Polish & Testing (Week 3)
- [ ] Error handling improvements
- [ ] Loading indicators
- [ ] Toast notifications
- [ ] Integration tests
- [ ] Documentation
- [ ] Security audit

---

## 8. Dependencies

### 8.1 Maven Dependencies (pom.xml)
```xml
<!-- Database Connection Pooling -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>

<!-- JDBC Drivers -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Password Encryption -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

### 8.2 Frontend Dependencies
- Bootstrap 5.3 (already included)
- Font Awesome (already included)
- Bootstrap Modal (already included)

---

## 9. Future Enhancements

### 9.1 Advanced Features
- **Query history** - Save and recall previous queries
- **Query templates** - Pre-defined query snippets
- **Visual query builder** - Drag-drop query construction
- **Export results** - CSV, JSON, Excel export
- **Query sharing** - Share queries between users
- **Connection profiles** - Pre-configured connection templates

### 9.2 Database Support
- **MongoDB** - NoSQL support
- **Redis** - Key-value store support
- **Cassandra** - Wide-column store support
- **Elasticsearch** - Search engine support

### 9.3 Performance
- **Query result caching** - Cache frequently used queries
- **Streaming results** - Handle large result sets
- **Connection pooling tuning** - Optimize pool size
- **Async query execution** - Non-blocking queries

---

## 10. Documentation

### 10.1 User Guide Topics
- Creating database connections
- Executing SQL queries
- Loading database schema into rules
- Connection troubleshooting
- Best practices

### 10.2 Developer Guide Topics
- Adding new database types
- Customizing connection pooling
- Extending query capabilities
- Security hardening
- Performance tuning

---

## 11. Success Criteria

### 11.1 Functional Requirements
- ✓ Create, edit, delete database connections
- ✓ Test connections before saving
- ✓ Execute SELECT queries
- ✓ Display query results in table format
- ✓ Load database schema into Blockly Field blocks
- ✓ Support PostgreSQL, MySQL, H2

### 11.2 Non-Functional Requirements
- ✓ Query execution < 5 seconds for typical queries
- ✓ Connection creation < 10 seconds
- ✓ Support up to 10 concurrent connections
- ✓ Handle result sets up to 1000 rows
- ✓ Graceful error handling and user feedback

### 11.3 User Experience
- ✓ Intuitive connection creation dialog
- ✓ Clear visual feedback for connection status
- ✓ Responsive table view
- ✓ Helpful error messages
- ✓ Consistent with existing Visual Editor design

---

## 12. Appendix

### 12.1 Database Type Configuration

| Database | Default Port | JDBC URL Pattern | Driver Class |
|----------|-------------|------------------|--------------|
| PostgreSQL | 5432 | jdbc:postgresql://host:port/db | org.postgresql.Driver |
| MySQL | 3306 | jdbc:mysql://host:port/db | com.mysql.cj.jdbc.Driver |
| Oracle | 1521 | jdbc:oracle:thin:@host:port:sid | oracle.jdbc.OracleDriver |
| SQL Server | 1433 | jdbc:sqlserver://host:port;databaseName=db | com.microsoft.sqlserver.jdbc.SQLServerDriver |
| H2 | N/A | jdbc:h2:mem:db | org.h2.Driver |

### 12.2 Example Connection Properties

**PostgreSQL:**
```
ssl=true
sslmode=require
```

**MySQL:**
```
useSSL=true
serverTimezone=UTC
```

**Oracle:**
```
oracle.net.encryption_client=REQUIRED
oracle.net.encryption_types_client=AES256
```

---

**End of Design Document**

---

This design document provides a complete blueprint for implementing the Data Sources accordion section. The design follows the established patterns from the "Evaluation Data Sets" section while adding sophisticated database connectivity and query execution capabilities. The implementation can be done incrementally following the phased approach outlined above.
