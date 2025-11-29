/**
 * APEX Playground JavaScript
 * 
 * Handles the interactive functionality of the 4-panel playground interface
 * including editor management, API calls, and real-time validation.
 */

// Global variables
let sourceDataEditor, yamlRulesEditor;
let currentDataFormat = 'JSON';
let currentExample = null;

// Initialize playground when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    initializePlayground();
    setupEventListeners();
    // loadDefaultExample(); // Disabled to start with empty UI
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

    // Check for data transferred from Visual Editor
    loadTransferredData();

    console.log('Playground initialized successfully');
}

/**
 * Load data transferred from the Visual Editor via localStorage
 */
function loadTransferredData() {
    try {
        const transferDataStr = localStorage.getItem('apex_visual_editor_transfer');
        if (!transferDataStr) {
            return;
        }

        const transferData = JSON.parse(transferDataStr);

        // Check if data is recent (within last 30 seconds) to avoid loading stale data
        const age = Date.now() - transferData.timestamp;
        if (age > 30000) {
            console.log('Transfer data is stale, ignoring');
            localStorage.removeItem('apex_visual_editor_transfer');
            return;
        }

        // Load YAML into the rules editor
        if (transferData.yaml && yamlRulesEditor) {
            yamlRulesEditor.value = transferData.yaml;
            document.getElementById('yamlRulesFileName').textContent = 'From Visual Editor';
            console.log('Loaded YAML from Visual Editor');
        }

        // Load JSON into the source data editor
        if (transferData.json && sourceDataEditor) {
            sourceDataEditor.value = transferData.json;
            document.getElementById('sourceDataFileName').textContent = 'From Visual Editor';
            // Ensure JSON format is selected
            document.getElementById('jsonFormat').checked = true;
            updateDataFormat('JSON');
            console.log('Loaded JSON data from Visual Editor');
        }

        // Clear the transfer data so it's not loaded again on refresh
        localStorage.removeItem('apex_visual_editor_transfer');

        console.log('Successfully loaded data from Visual Editor');
    } catch (e) {
        console.error('Error loading transferred data:', e);
        localStorage.removeItem('apex_visual_editor_transfer');
    }
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

    // File upload buttons
    document.getElementById('uploadDataBtn').addEventListener('click', () => {
        document.getElementById('dataFileInput').click();
    });

    document.getElementById('uploadYamlBtn').addEventListener('click', () => {
        document.getElementById('yamlFileInput').click();
    });

    // Save buttons
    document.getElementById('saveDataBtn').addEventListener('click', saveData);
    document.getElementById('saveYamlBtn').addEventListener('click', saveYaml);

    // File input change handlers
    document.getElementById('dataFileInput').addEventListener('change', handleDataFileUpload);
    document.getElementById('yamlFileInput').addEventListener('change', handleYamlFileUpload);
    document.getElementById('configFileInput').addEventListener('change', handleConfigFileUpload);

    // Drag and drop event listeners
    setupDragAndDrop();
    
    // Reset validation status when YAML content changes
    yamlRulesEditor.addEventListener('input', function() {
        resetYamlValidationStatus();
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
    processBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Validating...';

    // Validate YAML first
    const validationResult = await validateYaml();
    if (!validationResult.valid) {
        showAlert('YAML validation failed: ' + validationResult.message, 'danger');
        processBtn.disabled = false;
        processBtn.innerHTML = '<i class="fas fa-play"></i> Process';
        return;
    }

    // Update button to show processing
    processBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing...';

    const startTime = Date.now();

    try {
        // Add timestamp to prevent caching
        const response = await fetch(window.playgroundConfig.apiBaseUrl + '/process?t=' + new Date().getTime(), {
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

        console.log('Processing result:', result);

        // Display results
        displayValidationResults(result.validation || { message: result.message });
        displayEnrichmentResults(result.enrichment || { message: result.message }, result.metrics);
        displayTraceResults(result.trace);
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
 * @returns {Promise<{valid: boolean, message: string}>} Validation result
 */
async function validateYaml() {
    const yamlRules = yamlRulesEditor.value.trim();

    if (!yamlRules) {
        showAlert('Please provide YAML rules configuration to validate.', 'warning');
        return { valid: false, message: 'No YAML content provided' };
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
        return result;

    } catch (error) {
        console.error('Validation error:', error);
        updateYamlStatus(false, 'Validation error: ' + error.message);
        return { valid: false, message: 'Validation error: ' + error.message };
    }
}

/**
 * Reset YAML validation status to "Not Validated"
 */
function resetYamlValidationStatus() {
    const statusBadge = document.getElementById('yamlStatus');
    statusBadge.textContent = 'Not Validated';
    statusBadge.className = 'badge bg-secondary';
}

/**
 * Real-time YAML validation (lightweight)
 */
function validateYamlRealtime(yamlContent) {
    if (!yamlContent.trim()) {
        resetYamlValidationStatus();
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
    showConfirmationModal('Are you sure you want to clear all content? This action cannot be undone.', () => {
        resetPlayground();
    });
}

/**
 * Show confirmation modal
 */
function showConfirmationModal(message, onConfirm) {
    const modalElement = document.getElementById('confirmationModal');
    const messageElement = document.getElementById('confirmationMessage');
    const confirmBtn = document.getElementById('confirmActionBtn');
    
    messageElement.textContent = message;
    
    // Remove existing event listeners to prevent multiple firings
    const newConfirmBtn = confirmBtn.cloneNode(true);
    confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);
    
    newConfirmBtn.addEventListener('click', () => {
        const modal = bootstrap.Modal.getInstance(modalElement);
        modal.hide();
        onConfirm();
    });
    
    const modal = new bootstrap.Modal(modalElement);
    modal.show();
}

/**
 * Reset playground to initial state without confirmation
 */
function resetPlayground() {
    sourceDataEditor.value = '';
    yamlRulesEditor.value = '';
    document.getElementById('validationResults').innerHTML = '<p class="text-muted">Click "Process" to see validation results...</p>';
    document.getElementById('enrichmentResults').innerHTML = '<p class="text-muted">Click "Process" to see enrichment results and performance metrics...</p>';
    updateYamlStatus(true, 'Valid');
    updateProcessingTime(0);

    // Clear file name displays
    clearSourceDataFileName();
    clearYamlRulesFileName();
    
    // Clear current example context
    currentExample = null;
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
    // Clear existing content
    resetPlayground();

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

    // Update file name displays for example
    updateSourceDataFileName('example-data.json', JSON.stringify(exampleData, null, 2).length);
    updateYamlRulesFileName('example-rules.yaml', exampleYaml.length);

    resetYamlValidationStatus();
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

    // Attach listeners to example items
    attachExampleListeners();

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
 * Create HTML for example categories using Bootstrap Accordion
 */
function createExampleCategoriesHTML(examplesData) {
    let html = '<div class="accordion" id="examplesAccordion">';
    let index = 0;

    Object.keys(examplesData).forEach(category => {
        if (category === 'timestamp' || category === 'message' || category === 'error') return;

        const examples = examplesData[category];
        if (Array.isArray(examples) && examples.length > 0) {
            const categoryId = `category-${index}`;
            const isFirst = index === 0;
            
            html += `
                <div class="accordion-item">
                    <h2 class="accordion-header" id="heading-${categoryId}">
                        <button class="accordion-button ${isFirst ? '' : 'collapsed'}" type="button" 
                                data-bs-toggle="collapse" data-bs-target="#collapse-${categoryId}" 
                                aria-expanded="${isFirst}" aria-controls="collapse-${categoryId}">
                            ${category.charAt(0).toUpperCase() + category.slice(1)}
                            <span class="badge bg-secondary ms-2">${examples.length}</span>
                        </button>
                    </h2>
                    <div id="collapse-${categoryId}" class="accordion-collapse collapse ${isFirst ? 'show' : ''}" 
                         aria-labelledby="heading-${categoryId}" data-bs-parent="#examplesAccordion">
                        <div class="accordion-body p-0">
                            <div class="list-group list-group-flush">
                                ${examples.map(example => `
                                    <button type="button" class="list-group-item list-group-item-action example-item ${example.available ? '' : 'disabled'}"
                                            data-category="${category}"
                                            data-id="${example.id}">
                                        <div class="d-flex w-100 justify-content-between">
                                            <h6 class="mb-1">${example.name}</h6>
                                            <small class="text-muted">${formatFileSize(example.size)}</small>
                                        </div>
                                        <p class="mb-1 small text-muted">${example.description || 'No description available'}</p>
                                    </button>
                                `).join('')}
                            </div>
                        </div>
                    </div>
                </div>
            `;
            index++;
        }
    });

    html += '</div>';

    return html;
}

/**
 * Attach event listeners to example items
 */
function attachExampleListeners() {
    document.querySelectorAll('.example-item:not(.disabled)').forEach(item => {
        item.addEventListener('click', () => {
            const category = item.dataset.category;
            const id = item.dataset.id;
            loadSpecificExample(category, id);
            document.querySelector('.example-modal').remove();
        });
    });
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

        // Clear existing content before loading new example
        resetPlayground();
        
        // Set current example context
        currentExample = {
            category: category,
            id: id,
            name: example.name
        };

        // Load the example data
        if (example.yaml) {
            yamlRulesEditor.value = example.yaml;
            updateYamlRulesFileName(`${example.name.toLowerCase().replace(/\s+/g, '-')}.yaml`, example.yaml.length);
        }

        if (example.sampleData) {
            sourceDataEditor.value = JSON.stringify(example.sampleData, null, 2);
            updateSourceDataFileName(`${example.name.toLowerCase().replace(/\s+/g, '-')}-data.json`, JSON.stringify(example.sampleData, null, 2).length);
        }

        // Reset validation status when loading a new example
        resetYamlValidationStatus();

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
 * Save source data to file or server
 */
async function saveData() {
    const content = sourceDataEditor.value;
    if (!content) {
        showAlert('No data to save.', 'warning');
        return;
    }
    
    // If loaded from example, save back to server
    if (currentExample) {
        try {
            const response = await fetch(`${window.playgroundConfig.apiBaseUrl}/examples/${currentExample.category}/${currentExample.id}/data`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'text/plain',
                },
                body: content
            });
            
            const result = await response.json();
            
            if (result.success) {
                showAlert(`Example "${currentExample.name}" data saved successfully!`, 'success');
            } else {
                showAlert(`Failed to save example data: ${result.error}`, 'danger');
            }
        } catch (error) {
            console.error('Error saving example data:', error);
            showAlert(`Error saving example data: ${error.message}`, 'danger');
        }
        return;
    }
    
    const extension = currentDataFormat ? currentDataFormat.toLowerCase() : 'txt';
    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `data.${extension}`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

/**
 * Save YAML rules to file or server
 */
async function saveYaml() {
    const content = yamlRulesEditor.value;
    if (!content) {
        showAlert('No YAML rules to save.', 'warning');
        return;
    }
    
    // If loaded from example, save back to server
    if (currentExample) {
        try {
            const response = await fetch(`${window.playgroundConfig.apiBaseUrl}/examples/${currentExample.category}/${currentExample.id}/yaml`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'text/plain',
                },
                body: content
            });
            
            const result = await response.json();
            
            if (result.success) {
                showAlert(`Example "${currentExample.name}" YAML saved successfully!`, 'success');
            } else {
                showAlert(`Failed to save example: ${result.error}`, 'danger');
            }
        } catch (error) {
            console.error('Error saving example:', error);
            showAlert(`Error saving example: ${error.message}`, 'danger');
        }
        return;
    }
    
    // Otherwise download as file
    const blob = new Blob([content], { type: 'text/yaml' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'rules.yaml';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
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
    console.log('displayValidationResults called with:', results);
    console.log('results.valid =', results?.valid, 'type:', typeof results?.valid);

    const container = document.getElementById('validationResults');
    if (!container) {
        console.error('validationResults container not found!');
        return;
    }

    // Determine validation status - check for "valid" field in results
    const isValid = results && results.valid === true;
    console.log('isValid =', isValid);
    const statusIcon = isValid
        ? '<span class="validation-status-icon valid">✓</span>'
        : '<span class="validation-status-icon invalid">✗</span>';
    const statusText = isValid ? 'PASSED' : 'FAILED';
    const statusClass = isValid ? 'validation-passed' : 'validation-failed';

    // Build the HTML with status indicator at the top
    const statusHtml = `
        <div class="validation-status ${statusClass}">
            ${statusIcon}
            <span class="validation-status-text">${statusText}</span>
        </div>
        <pre>${JSON.stringify(results, null, 2)}</pre>
    `;

    container.innerHTML = statusHtml;

    // Visual feedback for update
    const originalBg = container.style.backgroundColor;
    container.style.transition = 'background-color 0.3s';
    container.style.backgroundColor = isValid ? '#d4edda' : '#f8d7da'; // Green or red tint

    setTimeout(() => {
        container.style.backgroundColor = originalBg || '#f8f9fa';
    }, 500);
}

/**
 * Display enrichment results
 */
function displayEnrichmentResults(enrichment, metrics) {
    console.log('Displaying enrichment results. Metrics:', metrics);
    const container = document.getElementById('enrichmentResults');
    
    let html = '';
    
    // Metrics section
    if (metrics) {
        html += '<div class="mb-3 border-bottom pb-2">';
        html += '<h6 class="text-muted mb-2">Performance Metrics</h6>';
        html += '<div class="row g-2 small">';
        html += `<div class="col-6">Total Time: <span class="fw-bold text-primary">${metrics.totalTimeMs}ms</span></div>`;
        html += `<div class="col-6">Rules Execution: <span class="fw-bold">${metrics.rulesExecutionTimeMs}ms</span></div>`;
        html += `<div class="col-6">YAML Parsing: <span class="fw-bold">${metrics.yamlParsingTimeMs}ms</span></div>`;
        html += `<div class="col-6">Data Parsing: <span class="fw-bold">${metrics.dataParsingTimeMs}ms</span></div>`;
        if (metrics.enrichmentTimeMs > 0) {
            html += `<div class="col-6">Enrichment: <span class="fw-bold">${metrics.enrichmentTimeMs}ms</span></div>`;
        }
        html += '</div></div>';
    }
    
    // Enrichment section
    html += '<h6 class="text-muted mb-2">Enrichment Data</h6>';
    html += `<pre>${JSON.stringify(enrichment, null, 2)}</pre>`;
    
    container.innerHTML = html;
}

/**
 * Display execution trace results
 */
function displayTraceResults(trace) {
    const container = document.getElementById('traceResults');
    
    if (!trace || trace.length === 0) {
        container.innerHTML = '<p class="text-muted">No execution trace available.</p>';
        return;
    }

    let html = '<div class="list-group list-group-flush">';
    
    trace.forEach(step => {
        const statusClass = step.status === 'SUCCESS' ? 'text-success' : 'text-danger';
        const iconClass = step.status === 'SUCCESS' ? 'fa-check-circle' : 'fa-times-circle';
        const statusText = step.status === 'SUCCESS' ? 'Step Processed' : step.status;
        const duration = step.durationMs >= 0 ? `${step.durationMs}ms` : '';
        
        // Determine indentation based on type
        let indentClass = '';
        let icon = 'fa-cog';
        
        if (step.type === 'SCENARIO_STAGE') {
            indentClass = 'fw-bold bg-light';
            icon = 'fa-layer-group';
        } else if (step.type === 'SECTION') {
            indentClass = 'ps-4';
            icon = 'fa-folder';
        } else {
            indentClass = 'ps-5 small';
            icon = 'fa-code';
        }

        html += `
            <div class="list-group-item ${indentClass}">
                <div class="d-flex w-100 justify-content-between align-items-center">
                    <div>
                        <i class="fas ${icon} me-2 text-muted"></i>
                        <span class="me-2">${step.name}</span>
                        <span class="badge bg-secondary rounded-pill" style="font-size: 0.7em">${step.type}</span>
                    </div>
                    <div class="text-end">
                        <span class="me-3 ${statusClass}">
                            <i class="fas ${iconClass} me-1"></i>${statusText}
                        </span>
                        <small class="text-muted" style="min-width: 50px; display: inline-block;">${duration}</small>
                    </div>
                </div>
                ${step.message ? `<div class="small text-muted mt-1 ms-4"><i class="fas fa-info-circle me-1"></i>${step.message}</div>` : ''}
            </div>
        `;
    });
    
    html += '</div>';
    container.innerHTML = html;
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

// File Upload Functions

/**
 * Setup drag and drop functionality
 */
function setupDragAndDrop() {
    const dataDropZone = document.getElementById('dataDropZone');
    const yamlDropZone = document.getElementById('yamlDropZone');
    const sourceDataEditor = document.getElementById('sourceDataEditor');
    const yamlRulesEditor = document.getElementById('yamlRulesEditor');

    // Data editor drag and drop
    setupDropZone(sourceDataEditor, dataDropZone, handleDataFileDrop);

    // YAML editor drag and drop
    setupDropZone(yamlRulesEditor, yamlDropZone, handleYamlFileDrop);
}

/**
 * Setup drop zone for an editor
 */
function setupDropZone(editor, dropZone, dropHandler) {
    // Prevent default drag behaviors
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        editor.addEventListener(eventName, preventDefaults, false);
        document.body.addEventListener(eventName, preventDefaults, false);
    });

    // Highlight drop zone when item is dragged over it
    ['dragenter', 'dragover'].forEach(eventName => {
        editor.addEventListener(eventName, () => {
            dropZone.classList.remove('d-none');
            dropZone.classList.add('drag-over');
        }, false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        editor.addEventListener(eventName, () => {
            dropZone.classList.add('d-none');
            dropZone.classList.remove('drag-over');
        }, false);
    });

    // Handle dropped files
    editor.addEventListener('drop', dropHandler, false);
}

/**
 * Prevent default drag behaviors
 */
function preventDefaults(e) {
    e.preventDefault();
    e.stopPropagation();
}

/**
 * Handle data file drop
 */
function handleDataFileDrop(e) {
    const files = e.dataTransfer.files;
    if (files.length > 0) {
        const file = files[0];
        if (validateDataFile(file)) {
            readFileContent(file, (content) => {
                sourceDataEditor.value = content;
                showAlert(`Data file "${file.name}" loaded successfully!`, 'success');

                // Update file name display
                updateSourceDataFileName(file.name, file.size);

                // Auto-detect format based on file extension
                autoDetectDataFormat(file.name);
            });
        }
    }
}

/**
 * Handle YAML file drop
 */
function handleYamlFileDrop(e) {
    const files = e.dataTransfer.files;
    if (files.length > 0) {
        const file = files[0];
        if (validateYamlFile(file)) {
            readFileContent(file, (content) => {
                yamlRulesEditor.value = content;
                showAlert(`YAML file "${file.name}" loaded successfully!`, 'success');

                // Update file name display
                updateYamlRulesFileName(file.name, file.size);

                // Trigger YAML validation
                validateYaml();
            });
        }
    }
}

/**
 * Handle data file upload via button
 */
function handleDataFileUpload(event) {
    const file = event.target.files[0];
    if (file && validateDataFile(file)) {
        showUploadProgress(file);

        readFileContent(file, (content) => {
            sourceDataEditor.value = content;
            hideUploadProgress();
            showAlert(`Data file "${file.name}" uploaded successfully!`, 'success');

            // Update file name display
            updateSourceDataFileName(file.name, file.size);

            // Auto-detect format
            autoDetectDataFormat(file.name);

            // Clear the input
            event.target.value = '';
        });
    }
}

/**
 * Handle YAML file upload via button
 */
function handleYamlFileUpload(event) {
    const file = event.target.files[0];
    if (file && validateYamlFile(file)) {
        showUploadProgress(file);

        readFileContent(file, (content) => {
            yamlRulesEditor.value = content;
            hideUploadProgress();
            showAlert(`YAML file "${file.name}" uploaded successfully!`, 'success');

            // Update file name display
            updateYamlRulesFileName(file.name, file.size);

            // Trigger validation
            validateYaml();

            // Clear the input
            event.target.value = '';
        });
    }
}

/**
 * Handle configuration file upload
 */
function handleConfigFileUpload(event) {
    const file = event.target.files[0];
    if (file && validateConfigFile(file)) {
        showUploadProgress(file);

        readFileContent(file, (content) => {
            try {
                const config = JSON.parse(content);

                if (config.sourceData) {
                    sourceDataEditor.value = config.sourceData;
                    updateSourceDataFileName('loaded-data.json', config.sourceData.length);
                }

                if (config.yamlRules) {
                    yamlRulesEditor.value = config.yamlRules;
                    updateYamlRulesFileName('loaded-rules.yaml', config.yamlRules.length);
                }

                if (config.dataFormat) {
                    updateDataFormat(config.dataFormat);
                    // Update radio button
                    const formatRadio = document.getElementById(config.dataFormat.toLowerCase() + 'Format');
                    if (formatRadio) {
                        formatRadio.checked = true;
                    }
                }

                hideUploadProgress();
                showAlert(`Configuration "${file.name}" loaded successfully!`, 'success');

                // Trigger validation
                validateYaml();

            } catch (error) {
                hideUploadProgress();
                showAlert(`Error parsing configuration file: ${error.message}`, 'danger');
            }

            // Clear the input
            event.target.value = '';
        });
    }
}

// File Validation Functions

/**
 * Validate data file
 */
function validateDataFile(file) {
    const maxSize = 10 * 1024 * 1024; // 10MB
    const allowedTypes = ['.json', '.xml', '.csv', '.txt'];

    // Check file size
    if (file.size > maxSize) {
        showAlert(`File size (${formatFileSize(file.size)}) exceeds maximum allowed size (10MB)`, 'danger');
        return false;
    }

    // Check file type
    const fileName = file.name.toLowerCase();
    const isValidType = allowedTypes.some(type => fileName.endsWith(type));

    if (!isValidType) {
        showAlert(`Invalid file type. Allowed types: ${allowedTypes.join(', ')}`, 'danger');
        return false;
    }

    return true;
}

/**
 * Validate YAML file
 */
function validateYamlFile(file) {
    const maxSize = 10 * 1024 * 1024; // 10MB
    const allowedTypes = ['.yaml', '.yml'];

    // Check file size
    if (file.size > maxSize) {
        showAlert(`File size (${formatFileSize(file.size)}) exceeds maximum allowed size (10MB)`, 'danger');
        return false;
    }

    // Check file type
    const fileName = file.name.toLowerCase();
    const isValidType = allowedTypes.some(type => fileName.endsWith(type));

    if (!isValidType) {
        showAlert(`Invalid file type. Allowed types: ${allowedTypes.join(', ')}`, 'danger');
        return false;
    }

    return true;
}

/**
 * Validate configuration file
 */
function validateConfigFile(file) {
    const maxSize = 10 * 1024 * 1024; // 10MB

    // Check file size
    if (file.size > maxSize) {
        showAlert(`File size (${formatFileSize(file.size)}) exceeds maximum allowed size (10MB)`, 'danger');
        return false;
    }

    // Check file type
    if (!file.name.toLowerCase().endsWith('.json')) {
        showAlert('Configuration file must be a JSON file (.json)', 'danger');
        return false;
    }

    return true;
}

// Utility Functions

/**
 * Read file content
 */
function readFileContent(file, callback) {
    const reader = new FileReader();

    reader.onload = function(e) {
        callback(e.target.result);
    };

    reader.onerror = function() {
        hideUploadProgress();
        showAlert(`Error reading file: ${file.name}`, 'danger');
    };

    reader.readAsText(file);
}

/**
 * Auto-detect data format based on file extension
 */
function autoDetectDataFormat(fileName) {
    const extension = fileName.toLowerCase().split('.').pop();

    let format = 'JSON'; // default

    switch (extension) {
        case 'xml':
            format = 'XML';
            break;
        case 'csv':
            format = 'CSV';
            break;
        case 'json':
        case 'txt':
        default:
            format = 'JSON';
            break;
    }

    // Update the radio button
    const formatRadio = document.getElementById(format.toLowerCase() + 'Format');
    if (formatRadio) {
        formatRadio.checked = true;
        updateDataFormat(format);
    }
}

/**
 * Format file size for display
 */
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * Update source data file name display
 */
function updateSourceDataFileName(fileName, fileSize) {
    const fileNameElement = document.getElementById('sourceDataFileName');
    const fileSizeElement = document.getElementById('sourceDataFileSize');

    if (fileName) {
        fileNameElement.textContent = fileName;
        fileNameElement.className = 'text-success fw-bold';
        if (fileSize) {
            fileSizeElement.textContent = `(${formatFileSize(fileSize)})`;
        }
    } else {
        fileNameElement.textContent = 'No file loaded';
        fileNameElement.className = '';
        fileSizeElement.textContent = '';
    }
}

/**
 * Update YAML rules file name display
 */
function updateYamlRulesFileName(fileName, fileSize) {
    const fileNameElement = document.getElementById('yamlRulesFileName');
    const fileSizeElement = document.getElementById('yamlRulesFileSize');

    if (fileName) {
        fileNameElement.textContent = fileName;
        fileNameElement.className = 'text-success fw-bold';
        if (fileSize) {
            fileSizeElement.textContent = `(${formatFileSize(fileSize)})`;
        }
    } else {
        fileNameElement.textContent = 'No file loaded';
        fileNameElement.className = '';
        fileSizeElement.textContent = '';
    }
}

/**
 * Clear source data file name display
 */
function clearSourceDataFileName() {
    updateSourceDataFileName(null);
}

/**
 * Clear YAML rules file name display
 */
function clearYamlRulesFileName() {
    updateYamlRulesFileName(null);
}

/**
 * Show upload progress modal
 */
function showUploadProgress(file) {
    const modal = new bootstrap.Modal(document.getElementById('uploadProgressModal'));
    const fileName = document.getElementById('uploadFileName');
    const fileSize = document.getElementById('uploadFileSize');
    const progressBar = document.getElementById('uploadProgressBar');
    const progressText = document.getElementById('uploadProgressText');

    fileName.textContent = file.name;
    fileSize.textContent = formatFileSize(file.size);

    // Simulate progress (since FileReader doesn't provide real progress for small files)
    let progress = 0;
    const interval = setInterval(() => {
        progress += Math.random() * 30;
        if (progress > 90) progress = 90;

        progressBar.style.width = progress + '%';
        progressBar.setAttribute('aria-valuenow', progress);
        progressText.textContent = Math.round(progress) + '%';
    }, 100);

    // Store interval ID for cleanup
    modal._progressInterval = interval;

    modal.show();
}

/**
 * Hide upload progress modal
 */
function hideUploadProgress() {
    const modal = bootstrap.Modal.getInstance(document.getElementById('uploadProgressModal'));
    if (modal) {
        // Complete the progress bar
        const progressBar = document.getElementById('uploadProgressBar');
        const progressText = document.getElementById('uploadProgressText');

        progressBar.style.width = '100%';
        progressBar.setAttribute('aria-valuenow', 100);
        progressText.textContent = '100%';

        // Clear interval
        if (modal._progressInterval) {
            clearInterval(modal._progressInterval);
        }

        // Hide modal after a brief delay
        setTimeout(() => {
            modal.hide();
        }, 500);
    }
}

/**
 * Toggle the bottom right panel collapse state
 */
function toggleBottomRightPanel() {
    const grid = document.querySelector('.playground-grid');
    const btn = document.getElementById('collapseBottomRightBtn');

    if (grid.classList.contains('bottom-right-collapsed')) {
        grid.classList.remove('bottom-right-collapsed');
        btn.classList.remove('collapsed');
        btn.title = 'Collapse panel';
    } else {
        grid.classList.add('bottom-right-collapsed');
        btn.classList.add('collapsed');
        btn.title = 'Expand panel';
    }
}

/**
 * Toggle the bottom left panel collapse state
 */
function toggleBottomLeftPanel() {
    const grid = document.querySelector('.playground-grid');
    const btn = document.getElementById('collapseBottomLeftBtn');

    if (grid.classList.contains('bottom-left-collapsed')) {
        grid.classList.remove('bottom-left-collapsed');
        btn.classList.remove('collapsed');
        btn.title = 'Collapse panel';
    } else {
        grid.classList.add('bottom-left-collapsed');
        btn.classList.add('collapsed');
        btn.title = 'Expand panel';
    }
}
