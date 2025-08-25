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

    // File upload buttons
    document.getElementById('uploadDataBtn').addEventListener('click', () => {
        document.getElementById('dataFileInput').click();
    });

    document.getElementById('uploadYamlBtn').addEventListener('click', () => {
        document.getElementById('yamlFileInput').click();
    });

    // File input change handlers
    document.getElementById('dataFileInput').addEventListener('change', handleDataFileUpload);
    document.getElementById('yamlFileInput').addEventListener('change', handleYamlFileUpload);
    document.getElementById('configFileInput').addEventListener('change', handleConfigFileUpload);

    // Drag and drop event listeners
    setupDragAndDrop();
    
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
                }

                if (config.yamlRules) {
                    yamlRulesEditor.value = config.yamlRules;
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
