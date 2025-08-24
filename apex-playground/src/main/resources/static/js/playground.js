/**
 * APEX Playground JavaScript
 * 
 * Handles the interactive functionality of the 4-panel playground interface
 * including editor management, API calls, and real-time validation.
 */

// Global variables
let sourceDataEditor, yamlRulesEditor;
let currentDataFormat = 'JSON';

// Initialize playground when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    initializePlayground();
    setupEventListeners();
    loadDefaultExample();
});

/**
 * Initialize the playground interface
 */
function initializePlayground() {
    console.log('Initializing APEX Playground...');
    
    // Initialize editors (placeholder - will be enhanced with CodeMirror in Phase 3)
    sourceDataEditor = document.getElementById('sourceDataEditor');
    yamlRulesEditor = document.getElementById('yamlRulesEditor');
    
    // Set initial data format
    updateDataFormat('JSON');
    
    console.log('Playground initialized successfully');
}

/**
 * Set up event listeners for UI interactions
 */
function setupEventListeners() {
    // Process button
    document.getElementById('processBtn').addEventListener('click', processData);
    
    // Validate button
    document.getElementById('validateBtn').addEventListener('click', validateYaml);
    
    // Clear button
    document.getElementById('clearBtn').addEventListener('click', clearAll);
    
    // Load example button
    document.getElementById('loadExampleBtn').addEventListener('click', loadExample);
    
    // Save config button
    document.getElementById('saveConfigBtn').addEventListener('click', saveConfiguration);
    
    // Data format radio buttons
    document.querySelectorAll('input[name="dataFormat"]').forEach(radio => {
        radio.addEventListener('change', function() {
            updateDataFormat(this.value);
        });
    });
    
    // Real-time YAML validation (debounced)
    let yamlValidationTimeout;
    yamlRulesEditor.addEventListener('input', function() {
        clearTimeout(yamlValidationTimeout);
        yamlValidationTimeout = setTimeout(() => {
            validateYamlRealtime(this.value);
        }, 500);
    });
}

/**
 * Process data with YAML rules
 */
async function processData() {
    const processBtn = document.getElementById('processBtn');
    const sourceData = sourceDataEditor.value.trim();
    const yamlRules = yamlRulesEditor.value.trim();
    
    if (!sourceData || !yamlRules) {
        showAlert('Please provide both source data and YAML rules configuration.', 'warning');
        return;
    }
    
    // Show processing state
    processBtn.disabled = true;
    processBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing...';
    
    const startTime = Date.now();
    
    try {
        const response = await fetch(window.playgroundConfig.apiBaseUrl + '/process', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                sourceData: sourceData,
                yamlRules: yamlRules,
                dataFormat: currentDataFormat
            })
        });
        
        const result = await response.json();
        const processingTime = Date.now() - startTime;
        
        // Display results
        displayValidationResults(result.validation || { message: result.message });
        displayEnrichmentResults(result.enrichment || { message: result.message });
        updateProcessingTime(processingTime);
        
    } catch (error) {
        console.error('Processing error:', error);
        showAlert('Error processing data: ' + error.message, 'danger');
    } finally {
        // Reset button state
        processBtn.disabled = false;
        processBtn.innerHTML = '<i class="fas fa-play"></i> Process';
    }
}

/**
 * Validate YAML configuration
 */
async function validateYaml() {
    const yamlRules = yamlRulesEditor.value.trim();
    
    if (!yamlRules) {
        showAlert('Please provide YAML rules configuration to validate.', 'warning');
        return;
    }
    
    try {
        const response = await fetch(window.playgroundConfig.apiBaseUrl + '/validate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                yamlContent: yamlRules
            })
        });
        
        const result = await response.json();
        updateYamlStatus(result.valid, result.message);
        
    } catch (error) {
        console.error('Validation error:', error);
        updateYamlStatus(false, 'Validation error: ' + error.message);
    }
}

/**
 * Real-time YAML validation (lightweight)
 */
function validateYamlRealtime(yamlContent) {
    if (!yamlContent.trim()) {
        updateYamlStatus(true, 'Valid');
        return;
    }
    
    // Basic YAML syntax check (placeholder - will be enhanced in Phase 2)
    try {
        // Simple validation - check for basic YAML structure
        const hasValidStructure = yamlContent.includes(':') && !yamlContent.includes('\t');
        updateYamlStatus(hasValidStructure, hasValidStructure ? 'Valid' : 'Invalid syntax');
    } catch (error) {
        updateYamlStatus(false, 'Syntax error');
    }
}

/**
 * Clear all editors and results
 */
function clearAll() {
    if (confirm('Are you sure you want to clear all content?')) {
        sourceDataEditor.value = '';
        yamlRulesEditor.value = '';
        document.getElementById('validationResults').innerHTML = '<p class="text-muted">Click "Process" to see validation results...</p>';
        document.getElementById('enrichmentResults').innerHTML = '<p class="text-muted">Click "Process" to see enrichment results and performance metrics...</p>';
        updateYamlStatus(true, 'Valid');
        updateProcessingTime(0);
    }
}

/**
 * Load an example configuration
 */
async function loadExample() {
    try {
        const response = await fetch(window.playgroundConfig.apiBaseUrl + '/examples');
        const data = await response.json();

        if (data.error) {
            console.error('Error from server:', data.error);
            loadDefaultExample();
            return;
        }

        // Show example selection dialog
        showExampleSelectionDialog(data);

    } catch (error) {
        console.error('Error loading examples:', error);
        loadDefaultExample();
    }
}

/**
 * Load default example data
 */
function loadDefaultExample() {
    const exampleData = {
        "name": "John Doe",
        "age": 30,
        "email": "john.doe@example.com",
        "amount": 1500.00,
        "currency": "USD"
    };
    
    const exampleYaml = `metadata:
  name: "Sample Validation Rules"
  version: "1.0.0"
  description: "Example validation rules for playground"

rules:
  - id: "age-check"
    name: "Age Validation"
    condition: "#age >= 18"
    message: "Age must be 18 or older"
    
  - id: "email-check"
    name: "Email Validation"
    condition: "#email != null && #email.contains('@')"
    message: "Valid email address required"`;
    
    sourceDataEditor.value = JSON.stringify(exampleData, null, 2);
    yamlRulesEditor.value = exampleYaml;
    
    validateYamlRealtime(exampleYaml);
}

/**
 * Show example selection dialog
 */
function showExampleSelectionDialog(examplesData) {
    // Create modal dialog
    const modal = document.createElement('div');
    modal.className = 'example-modal';
    modal.innerHTML = `
        <div class="example-modal-content">
            <div class="example-modal-header">
                <h3>Select Example</h3>
                <button class="example-modal-close">&times;</button>
            </div>
            <div class="example-modal-body">
                ${createExampleCategoriesHTML(examplesData)}
            </div>
        </div>
    `;

    document.body.appendChild(modal);

    // Add event listeners
    modal.querySelector('.example-modal-close').addEventListener('click', () => {
        document.body.removeChild(modal);
    });

    // Close on background click
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            document.body.removeChild(modal);
        }
    });
}

/**
 * Create HTML for example categories
 */
function createExampleCategoriesHTML(examplesData) {
    let html = '';

    Object.keys(examplesData).forEach(category => {
        if (category === 'timestamp' || category === 'message') return;

        const examples = examplesData[category];
        if (Array.isArray(examples)) {
            html += `
                <div class="example-category">
                    <h4>${category.charAt(0).toUpperCase() + category.slice(1)}</h4>
                    <div class="example-list">
                        ${examples.map(example => `
                            <div class="example-item ${example.available ? '' : 'unavailable'}"
                                 data-category="${category}"
                                 data-id="${example.id}">
                                <div class="example-name">${example.name}</div>
                                <div class="example-description">${example.description}</div>
                                ${!example.available ? '<div class="example-error">Not available</div>' : ''}
                            </div>
                        `).join('')}
                    </div>
                </div>
            `;
        }
    });

    // Add event listeners for example selection
    setTimeout(() => {
        document.querySelectorAll('.example-item.available, .example-item:not(.unavailable)').forEach(item => {
            item.addEventListener('click', () => {
                const category = item.dataset.category;
                const id = item.dataset.id;
                loadSpecificExample(category, id);
                document.querySelector('.example-modal').remove();
            });
        });
    }, 100);

    return html;
}

/**
 * Load a specific example by category and ID
 */
async function loadSpecificExample(category, id) {
    try {
        const response = await fetch(`${window.playgroundConfig.apiBaseUrl}/examples/${category}/${id}`);
        const example = await response.json();

        if (example.error) {
            console.error('Error loading example:', example.error);
            loadDefaultExample();
            return;
        }

        // Load the example data
        if (example.yaml) {
            yamlRulesEditor.value = example.yaml;
        }

        if (example.sampleData) {
            sourceDataEditor.value = JSON.stringify(example.sampleData, null, 2);
        }

        showAlert(`Example "${example.name}" loaded successfully`, 'success');

    } catch (error) {
        console.error('Error loading specific example:', error);
        loadDefaultExample();
    }
}

/**
 * Save current configuration
 */
function saveConfiguration() {
    const config = {
        sourceData: sourceDataEditor.value,
        yamlRules: yamlRulesEditor.value,
        dataFormat: currentDataFormat,
        timestamp: new Date().toISOString()
    };
    
    const blob = new Blob([JSON.stringify(config, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'apex-playground-config.json';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    
    showAlert('Configuration saved successfully!', 'success');
}

/**
 * Update data format
 */
function updateDataFormat(format) {
    currentDataFormat = format;
    console.log('Data format updated to:', format);
}

/**
 * Update YAML validation status
 */
function updateYamlStatus(isValid, message) {
    const statusBadge = document.getElementById('yamlStatus');
    statusBadge.textContent = message || (isValid ? 'Valid' : 'Invalid');
    statusBadge.className = isValid ? 'badge bg-success' : 'badge bg-danger';
}

/**
 * Display validation results
 */
function displayValidationResults(results) {
    const container = document.getElementById('validationResults');
    container.innerHTML = `<pre>${JSON.stringify(results, null, 2)}</pre>`;
}

/**
 * Display enrichment results
 */
function displayEnrichmentResults(results) {
    const container = document.getElementById('enrichmentResults');
    container.innerHTML = `<pre>${JSON.stringify(results, null, 2)}</pre>`;
}

/**
 * Update processing time display
 */
function updateProcessingTime(timeMs) {
    const element = document.getElementById('processingTime');
    element.textContent = timeMs > 0 ? `Processing time: ${timeMs}ms` : 'Processing time: --';
}

/**
 * Show alert message
 */
function showAlert(message, type = 'info') {
    // Create alert element
    const alert = document.createElement('div');
    alert.className = `alert alert-${type} alert-dismissible fade show`;
    alert.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    // Insert at top of container
    const container = document.querySelector('.container-fluid');
    container.insertBefore(alert, container.firstChild);
    
    // Auto-dismiss after 5 seconds
    setTimeout(() => {
        if (alert.parentNode) {
            alert.remove();
        }
    }, 5000);
}
