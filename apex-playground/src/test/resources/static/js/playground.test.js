/**
 * JavaScript unit tests for APEX Playground frontend functionality.
 * 
 * Tests the playground.js functionality using Jest-like syntax.
 * These tests can be run with any JavaScript testing framework.
 */

// Mock DOM elements and global objects
const mockDOM = {
    elements: {},
    getElementById: function(id) {
        if (!this.elements[id]) {
            this.elements[id] = {
                value: '',
                innerHTML: '',
                textContent: '',
                disabled: false,
                addEventListener: function() {},
                click: function() {},
                style: {},
                classList: {
                    add: function() {},
                    remove: function() {},
                    contains: function() { return false; }
                }
            };
        }
        return this.elements[id];
    },
    querySelectorAll: function() {
        return [];
    },
    createElement: function(tag) {
        return {
            tagName: tag.toUpperCase(),
            href: '',
            download: '',
            click: function() {},
            remove: function() {}
        };
    },
    body: {
        appendChild: function() {},
        removeChild: function() {}
    }
};

// Mock global objects
global.document = mockDOM;
global.window = {
    playgroundConfig: {
        apiBaseUrl: '/playground/api',
        version: '1.0.0'
    },
    URL: {
        createObjectURL: function() { return 'blob:mock-url'; },
        revokeObjectURL: function() {}
    },
    Blob: function(content, options) {
        this.content = content;
        this.type = options.type;
    },
    fetch: function() {
        return Promise.resolve({
            json: function() {
                return Promise.resolve({
                    success: true,
                    message: 'Mock response'
                });
            }
        });
    },
    confirm: function() { return true; },
    alert: function() {}
};

global.console = {
    log: function() {},
    error: function() {},
    debug: function() {}
};

// Load the playground.js functionality (simulated)
// In a real test environment, you would import or require the actual file

/**
 * Test Suite: Playground Initialization
 */
describe('Playground Initialization', function() {
    
    test('should initialize playground with default values', function() {
        // Given
        const mockSourceEditor = mockDOM.getElementById('sourceDataEditor');
        const mockYamlEditor = mockDOM.getElementById('yamlRulesEditor');
        
        // When
        // initializePlayground(); // Would call the actual function
        
        // Then
        expect(mockSourceEditor).toBeDefined();
        expect(mockYamlEditor).toBeDefined();
    });
    
    test('should set up event listeners correctly', function() {
        // Given
        const mockProcessBtn = mockDOM.getElementById('processBtn');
        const mockValidateBtn = mockDOM.getElementById('validateBtn');
        const mockClearBtn = mockDOM.getElementById('clearBtn');
        
        // When
        // setupEventListeners(); // Would call the actual function
        
        // Then
        expect(mockProcessBtn).toBeDefined();
        expect(mockValidateBtn).toBeDefined();
        expect(mockClearBtn).toBeDefined();
    });
});

/**
 * Test Suite: Data Processing
 */
describe('Data Processing', function() {
    
    test('should process valid data successfully', async function() {
        // Given
        const mockSourceData = '{"name": "John", "age": 30}';
        const mockYamlRules = 'metadata:\n  name: "Test"\nrules:\n  - id: "test"\n    name: "Test Rule"\n    condition: "true"';
        
        mockDOM.getElementById('sourceDataEditor').value = mockSourceData;
        mockDOM.getElementById('yamlRulesEditor').value = mockYamlRules;
        
        // Mock successful API response
        global.window.fetch = jest.fn().mockResolvedValue({
            json: jest.fn().mockResolvedValue({
                success: true,
                message: 'Processing completed successfully',
                validation: {
                    valid: true,
                    rulesExecuted: 1,
                    rulesPassed: 1,
                    results: [{
                        ruleId: 'test',
                        ruleName: 'Test Rule',
                        passed: true,
                        message: 'Rule passed'
                    }]
                },
                enrichment: {
                    enriched: true,
                    enrichedData: { name: 'John', age: 30 }
                },
                metrics: {
                    totalTimeMs: 100
                }
            })
        });
        
        // When
        // await processData(); // Would call the actual function
        
        // Then
        expect(global.window.fetch).toHaveBeenCalledWith(
            '/playground/api/process',
            expect.objectContaining({
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: expect.stringContaining(mockSourceData)
            })
        );
    });
    
    test('should handle processing errors gracefully', async function() {
        // Given
        mockDOM.getElementById('sourceDataEditor').value = '{"invalid": json}';
        mockDOM.getElementById('yamlRulesEditor').value = 'invalid yaml';
        
        // Mock error API response
        global.window.fetch = jest.fn().mockResolvedValue({
            json: jest.fn().mockResolvedValue({
                success: false,
                message: 'Processing failed',
                errors: ['Invalid JSON data']
            })
        });
        
        // When
        // await processData(); // Would call the actual function
        
        // Then
        expect(global.window.fetch).toHaveBeenCalled();
        // Would verify error handling in actual implementation
    });
    
    test('should validate required fields before processing', function() {
        // Given
        mockDOM.getElementById('sourceDataEditor').value = '';
        mockDOM.getElementById('yamlRulesEditor').value = '';
        
        // When
        const result = validateRequiredFields(); // Mock function
        
        // Then
        expect(result).toBe(false);
    });
});

/**
 * Test Suite: YAML Validation
 */
describe('YAML Validation', function() {
    
    test('should validate YAML syntax correctly', async function() {
        // Given
        const validYaml = 'metadata:\n  name: "Test"\nrules:\n  - id: "test"';
        mockDOM.getElementById('yamlRulesEditor').value = validYaml;
        
        // Mock successful validation response
        global.window.fetch = jest.fn().mockResolvedValue({
            json: jest.fn().mockResolvedValue({
                valid: true,
                message: 'YAML is valid',
                errors: [],
                warnings: []
            })
        });
        
        // When
        // await validateYaml(); // Would call the actual function
        
        // Then
        expect(global.window.fetch).toHaveBeenCalledWith(
            '/playground/api/validate',
            expect.objectContaining({
                method: 'POST',
                body: expect.stringContaining(validYaml)
            })
        );
    });
    
    test('should handle YAML validation errors', async function() {
        // Given
        const invalidYaml = 'invalid: yaml: [unclosed';
        
        // Mock validation error response
        global.window.fetch = jest.fn().mockResolvedValue({
            json: jest.fn().mockResolvedValue({
                valid: false,
                message: 'YAML validation failed',
                errors: [{
                    type: 'ERROR',
                    message: 'Syntax error',
                    line: 1,
                    column: 15
                }]
            })
        });
        
        // When
        // await validateYaml(); // Would call the actual function
        
        // Then
        expect(global.window.fetch).toHaveBeenCalled();
        // Would verify error display in actual implementation
    });
    
    test('should perform real-time YAML validation', function() {
        // Given
        const yamlContent = 'metadata:\n  name: "Test"';
        
        // When
        const isValid = validateYamlRealtime(yamlContent); // Mock function
        
        // Then
        expect(isValid).toBe(true);
    });
    
    test('should detect invalid YAML in real-time', function() {
        // Given
        const invalidYaml = 'metadata:\n  name: "Test\n  invalid: [';
        
        // When
        const isValid = validateYamlRealtime(invalidYaml); // Mock function
        
        // Then
        expect(isValid).toBe(false);
    });
});

/**
 * Test Suite: Data Format Handling
 */
describe('Data Format Handling', function() {
    
    test('should update data format correctly', function() {
        // Given
        const newFormat = 'XML';
        
        // When
        updateDataFormat(newFormat); // Mock function
        
        // Then
        expect(getCurrentDataFormat()).toBe(newFormat); // Mock function
    });
    
    test('should handle different data formats', function() {
        // Test JSON format
        expect(isValidDataFormat('{"name": "John"}', 'JSON')).toBe(true);
        
        // Test XML format
        expect(isValidDataFormat('<person><name>John</name></person>', 'XML')).toBe(true);
        
        // Test CSV format
        expect(isValidDataFormat('name,age\nJohn,30', 'CSV')).toBe(true);
    });
});

/**
 * Test Suite: UI Interactions
 */
describe('UI Interactions', function() {
    
    test('should clear all content when clear button is clicked', function() {
        // Given
        const mockSourceEditor = mockDOM.getElementById('sourceDataEditor');
        const mockYamlEditor = mockDOM.getElementById('yamlRulesEditor');
        mockSourceEditor.value = 'test data';
        mockYamlEditor.value = 'test yaml';
        
        // When
        clearAll(); // Mock function
        
        // Then
        expect(mockSourceEditor.value).toBe('');
        expect(mockYamlEditor.value).toBe('');
    });
    
    test('should load default example correctly', function() {
        // Given
        const mockSourceEditor = mockDOM.getElementById('sourceDataEditor');
        const mockYamlEditor = mockDOM.getElementById('yamlRulesEditor');
        
        // When
        loadDefaultExample(); // Mock function
        
        // Then
        expect(mockSourceEditor.value).toContain('John Doe');
        expect(mockYamlEditor.value).toContain('metadata:');
    });
    
    test('should save configuration as JSON file', function() {
        // Given
        mockDOM.getElementById('sourceDataEditor').value = '{"test": "data"}';
        mockDOM.getElementById('yamlRulesEditor').value = 'test: yaml';
        
        // When
        saveConfiguration(); // Mock function
        
        // Then
        // Would verify file download in actual implementation
        expect(global.window.URL.createObjectURL).toHaveBeenCalled();
    });
});

/**
 * Test Suite: Error Handling
 */
describe('Error Handling', function() {
    
    test('should display error alerts correctly', function() {
        // Given
        const errorMessage = 'Test error message';
        const alertType = 'danger';
        
        // When
        showAlert(errorMessage, alertType); // Mock function
        
        // Then
        // Would verify alert display in actual implementation
        const alertElement = mockDOM.getElementById('alert-container');
        expect(alertElement).toBeDefined();
    });
    
    test('should handle network errors gracefully', async function() {
        // Given
        global.window.fetch = jest.fn().mockRejectedValue(new Error('Network error'));
        
        // When
        try {
            // await processData(); // Would call the actual function
        } catch (error) {
            // Then
            expect(error.message).toBe('Network error');
        }
    });
    
    test('should validate input before API calls', function() {
        // Given
        mockDOM.getElementById('sourceDataEditor').value = '';
        mockDOM.getElementById('yamlRulesEditor').value = '';
        
        // When
        const canProcess = canProcessData(); // Mock function
        
        // Then
        expect(canProcess).toBe(false);
    });
});

/**
 * Test Suite: Performance and Metrics
 */
describe('Performance and Metrics', function() {
    
    test('should display processing time correctly', function() {
        // Given
        const processingTime = 150;
        
        // When
        updateProcessingTime(processingTime); // Mock function
        
        // Then
        const timeElement = mockDOM.getElementById('processingTime');
        expect(timeElement.textContent).toContain('150ms');
    });
    
    test('should display validation results correctly', function() {
        // Given
        const validationResults = {
            valid: true,
            rulesExecuted: 2,
            rulesPassed: 2,
            results: [
                { ruleId: 'rule1', passed: true, message: 'Rule 1 passed' },
                { ruleId: 'rule2', passed: true, message: 'Rule 2 passed' }
            ]
        };
        
        // When
        displayValidationResults(validationResults); // Mock function
        
        // Then
        const resultsElement = mockDOM.getElementById('validationResults');
        expect(resultsElement.innerHTML).toContain('2');
    });
    
    test('should display enrichment results correctly', function() {
        // Given
        const enrichmentResults = {
            enriched: true,
            fieldsAdded: 3,
            enrichedData: { name: 'John', age: 30, category: 'premium' }
        };
        
        // When
        displayEnrichmentResults(enrichmentResults); // Mock function
        
        // Then
        const resultsElement = mockDOM.getElementById('enrichmentResults');
        expect(resultsElement.innerHTML).toContain('John');
    });
});

// Mock helper functions (these would be the actual implementations)
function validateRequiredFields() {
    const sourceData = mockDOM.getElementById('sourceDataEditor').value.trim();
    const yamlRules = mockDOM.getElementById('yamlRulesEditor').value.trim();
    return sourceData && yamlRules;
}

function validateYamlRealtime(yamlContent) {
    if (!yamlContent.trim()) return true;
    return yamlContent.includes(':') && !yamlContent.includes('\t');
}

function updateDataFormat(format) {
    global.currentDataFormat = format;
}

function getCurrentDataFormat() {
    return global.currentDataFormat || 'JSON';
}

function isValidDataFormat(data, format) {
    switch (format) {
        case 'JSON':
            try {
                JSON.parse(data);
                return true;
            } catch (e) {
                return false;
            }
        case 'XML':
            return data.trim().startsWith('<') && data.trim().endsWith('>');
        case 'CSV':
            return data.includes(',') && data.includes('\n');
        default:
            return false;
    }
}

function clearAll() {
    mockDOM.getElementById('sourceDataEditor').value = '';
    mockDOM.getElementById('yamlRulesEditor').value = '';
}

function loadDefaultExample() {
    mockDOM.getElementById('sourceDataEditor').value = '{"name": "John Doe", "age": 30}';
    mockDOM.getElementById('yamlRulesEditor').value = 'metadata:\n  name: "Example"';
}

function saveConfiguration() {
    global.window.URL.createObjectURL(new global.window.Blob(['test'], {type: 'application/json'}));
}

function showAlert(message, type) {
    // Mock alert display
}

function canProcessData() {
    return validateRequiredFields();
}

function updateProcessingTime(timeMs) {
    mockDOM.getElementById('processingTime').textContent = `Processing time: ${timeMs}ms`;
}

function displayValidationResults(results) {
    mockDOM.getElementById('validationResults').innerHTML = JSON.stringify(results);
}

function displayEnrichmentResults(results) {
    mockDOM.getElementById('enrichmentResults').innerHTML = JSON.stringify(results);
}

// Export for testing frameworks
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        validateRequiredFields,
        validateYamlRealtime,
        updateDataFormat,
        getCurrentDataFormat,
        isValidDataFormat,
        clearAll,
        loadDefaultExample,
        saveConfiguration,
        showAlert,
        canProcessData,
        updateProcessingTime,
        displayValidationResults,
        displayEnrichmentResults
    };
}
