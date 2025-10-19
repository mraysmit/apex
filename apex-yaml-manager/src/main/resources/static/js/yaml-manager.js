/**
 * APEX YAML Manager JavaScript
 * 
 * Handles the interactive functionality of the YAML Manager UI
 * including dashboard stats, API calls, and real-time updates.
 */

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    loadDashboardStats();
});

/**
 * Load dashboard statistics from API
 */
async function loadDashboardStats() {
    try {
        // Load catalog stats
        const catalogResponse = await fetch(
            window.yamlManagerConfig.apiBaseUrl + '/catalog/all'
        );
        const catalogData = await catalogResponse.json();
        const totalConfigs = catalogData.data ? catalogData.data.length : 0;
        document.getElementById('totalConfigs').textContent = totalConfigs;

        // Load health stats
        const healthResponse = await fetch(
            window.yamlManagerConfig.apiBaseUrl + '/health-checks/score'
        );
        const healthData = await healthResponse.json();
        const healthScore = healthData.data ? Math.round(healthData.data.overallScore) : '--';
        document.getElementById('healthScore').textContent = healthScore;

        // Load validation stats
        const validationResponse = await fetch(
            window.yamlManagerConfig.apiBaseUrl + '/validation/all'
        );
        const validationData = await validationResponse.json();
        const validationIssues = validationData.data ? validationData.data.issues.length : 0;
        document.getElementById('validationIssues').textContent = validationIssues;

        // Load dependency stats
        const depsResponse = await fetch(
            window.yamlManagerConfig.apiBaseUrl + '/dependencies/circular-dependencies'
        );
        const depsData = await depsResponse.json();
        const circularDeps = depsData.data ? depsData.data.length : 0;
        document.getElementById('circularDeps').textContent = circularDeps;

    } catch (error) {
        console.error('Error loading dashboard stats:', error);
        // Set all to error state
        document.getElementById('totalConfigs').textContent = 'Error';
        document.getElementById('healthScore').textContent = 'Error';
        document.getElementById('validationIssues').textContent = 'Error';
        document.getElementById('circularDeps').textContent = 'Error';
    }
}

/**
 * Format JSON for display
 */
function formatJson(obj) {
    return JSON.stringify(obj, null, 2);
}

/**
 * Format error message for display
 */
function formatError(error) {
    if (typeof error === 'string') {
        return error;
    }
    if (error.message) {
        return error.message;
    }
    return JSON.stringify(error, null, 2);
}

/**
 * Show loading state on element
 */
function showLoading(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.innerHTML = '<div class="spinner"></div> Loading...';
    }
}

/**
 * Show error state on element
 */
function showError(elementId, error) {
    const element = document.getElementById(elementId);
    if (element) {
        element.innerHTML = `<div class="alert alert-danger">Error: ${formatError(error)}</div>`;
    }
}

/**
 * Show success message
 */
function showSuccess(message) {
    const alert = document.createElement('div');
    alert.className = 'alert alert-success alert-dismissible fade show';
    alert.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.insertBefore(alert, document.body.firstChild);
    setTimeout(() => alert.remove(), 5000);
}

/**
 * Make API call with error handling
 */
async function apiCall(endpoint, options = {}) {
    const url = window.yamlManagerConfig.apiBaseUrl + endpoint;
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json'
        }
    };
    
    try {
        const response = await fetch(url, { ...defaultOptions, ...options });
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        return await response.json();
    } catch (error) {
        console.error('API call failed:', error);
        throw error;
    }
}

/**
 * Refresh dashboard stats
 */
function refreshDashboard() {
    loadDashboardStats();
    showSuccess('Dashboard refreshed');
}

/**
 * Export data as JSON
 */
function exportAsJson(data, filename) {
    const json = JSON.stringify(data, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename || 'export.json';
    link.click();
    URL.revokeObjectURL(url);
}

/**
 * Copy text to clipboard
 */
function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        showSuccess('Copied to clipboard');
    }).catch(err => {
        console.error('Failed to copy:', err);
    });
}

