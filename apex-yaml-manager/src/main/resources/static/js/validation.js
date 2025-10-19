/**
 * Validation UI JavaScript
 */

document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
});

function setupEventListeners() {
    document.getElementById('validateAllBtn').addEventListener('click', validateAll);
    document.getElementById('structureBtn').addEventListener('click', validateStructure);
    document.getElementById('referencesBtn').addEventListener('click', validateReferences);
    document.getElementById('consistencyBtn').addEventListener('click', validateConsistency);
    document.getElementById('clearBtn').addEventListener('click', clearResults);
}

async function validateAll() {
    const filePath = document.getElementById('filePath').value.trim();
    const baseDir = document.getElementById('baseDir').value.trim();
    
    if (!filePath) {
        showError('validationStatus', 'Please enter a file path');
        return;
    }

    showLoading('validationStatus');
    const startTime = Date.now();

    try {
        let url = '/validation/all?filePath=' + encodeURIComponent(filePath);
        if (baseDir) {
            url += '&baseDir=' + encodeURIComponent(baseDir);
        }

        const response = await apiCall(url, { method: 'POST' });
        const time = Date.now() - startTime;
        document.getElementById('validationTime').textContent = `Time: ${time}ms`;

        displayValidationResults(response);
        displayIssues(response);

    } catch (error) {
        showError('validationStatus', error);
    }
}

async function validateStructure() {
    const filePath = document.getElementById('filePath').value.trim();
    const baseDir = document.getElementById('baseDir').value.trim();
    
    if (!filePath) {
        showError('validationStatus', 'Please enter a file path');
        return;
    }

    showLoading('validationStatus');
    const startTime = Date.now();

    try {
        let url = '/validation/structure?filePath=' + encodeURIComponent(filePath);
        if (baseDir) {
            url += '&baseDir=' + encodeURIComponent(baseDir);
        }

        const response = await apiCall(url, { method: 'POST' });
        const time = Date.now() - startTime;
        document.getElementById('validationTime').textContent = `Time: ${time}ms`;

        displayValidationResults(response);
        displayIssues(response);

    } catch (error) {
        showError('validationStatus', error);
    }
}

async function validateReferences() {
    const filePath = document.getElementById('filePath').value.trim();
    const baseDir = document.getElementById('baseDir').value.trim();
    
    if (!filePath) {
        showError('validationStatus', 'Please enter a file path');
        return;
    }

    showLoading('validationStatus');
    const startTime = Date.now();

    try {
        let url = '/validation/references?filePath=' + encodeURIComponent(filePath);
        if (baseDir) {
            url += '&baseDir=' + encodeURIComponent(baseDir);
        }

        const response = await apiCall(url, { method: 'POST' });
        const time = Date.now() - startTime;
        document.getElementById('validationTime').textContent = `Time: ${time}ms`;

        displayValidationResults(response);
        displayIssues(response);

    } catch (error) {
        showError('validationStatus', error);
    }
}

async function validateConsistency() {
    const filePath = document.getElementById('filePath').value.trim();
    const baseDir = document.getElementById('baseDir').value.trim();
    
    if (!filePath) {
        showError('validationStatus', 'Please enter a file path');
        return;
    }

    showLoading('validationStatus');
    const startTime = Date.now();

    try {
        let url = '/validation/consistency?filePath=' + encodeURIComponent(filePath);
        if (baseDir) {
            url += '&baseDir=' + encodeURIComponent(baseDir);
        }

        const response = await apiCall(url, { method: 'POST' });
        const time = Date.now() - startTime;
        document.getElementById('validationTime').textContent = `Time: ${time}ms`;

        displayValidationResults(response);
        displayIssues(response);

    } catch (error) {
        showError('validationStatus', error);
    }
}

function displayValidationResults(data) {
    const resultsDiv = document.getElementById('validationResults');
    
    if (data.status === 'success') {
        const isValid = data.data && data.data.isValid;
        const alertClass = isValid ? 'alert-success' : 'alert-warning';
        const icon = isValid ? 'fa-check-circle' : 'fa-exclamation-triangle';
        
        resultsDiv.innerHTML = `
            <div class="alert ${alertClass}">
                <strong><i class="fas ${icon}"></i> ${isValid ? 'Valid' : 'Invalid'}</strong>
            </div>
            <pre>${formatJson({
                isValid: isValid,
                issueCount: data.data ? data.data.issues.length : 0,
                validationTime: data.data ? data.data.validationTime : 0
            })}</pre>
        `;
    } else {
        resultsDiv.innerHTML = `
            <div class="alert alert-danger">
                <strong>Error:</strong> ${data.message}
            </div>
        `;
    }
}

function displayIssues(data) {
    const issuesDiv = document.getElementById('issuesPanel');
    
    if (!data.data || !data.data.issues || data.data.issues.length === 0) {
        issuesDiv.innerHTML = '<div class="alert alert-success">No issues found</div>';
        return;
    }

    let html = '<div class="list-group">';
    
    data.data.issues.forEach(issue => {
        const severityClass = getSeverityClass(issue.severity);
        html += `
            <div class="list-group-item">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <h6 class="mb-1">${issue.code}</h6>
                        <p class="mb-1">${issue.message}</p>
                        <small class="text-muted">${issue.category}</small>
                    </div>
                    <span class="badge ${severityClass}">${issue.severity}</span>
                </div>
                ${issue.recommendation ? `<p class="mb-0 mt-2"><strong>Recommendation:</strong> ${issue.recommendation}</p>` : ''}
            </div>
        `;
    });
    
    html += '</div>';
    issuesDiv.innerHTML = html;
}

function getSeverityClass(severity) {
    switch(severity) {
        case 'ERROR': return 'bg-danger';
        case 'WARNING': return 'bg-warning';
        case 'INFO': return 'bg-info';
        default: return 'bg-secondary';
    }
}

function clearResults() {
    document.getElementById('filePath').value = '';
    document.getElementById('baseDir').value = '';
    document.getElementById('validationStatus').innerHTML = '<p class="text-muted">Click "Validate All" to start validation...</p>';
    document.getElementById('validationResults').innerHTML = '<p class="text-muted">Results will appear here...</p>';
    document.getElementById('issuesPanel').innerHTML = '<p class="text-muted">Issues will appear here...</p>';
    document.getElementById('validationTime').textContent = 'Time: --';
}

