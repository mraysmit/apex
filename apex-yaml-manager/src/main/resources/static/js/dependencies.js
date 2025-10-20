/**
 * Dependency Analysis UI JavaScript
 */

document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
});

function setupEventListeners() {
    document.getElementById('analyzeBtn').addEventListener('click', analyzeDependencies);
    document.getElementById('clearBtn').addEventListener('click', clearResults);
    document.getElementById('metricsBtn').addEventListener('click', loadMetrics);
    document.getElementById('circularBtn').addEventListener('click', loadCircularDependencies);
}

async function analyzeDependencies() {
    const filePath = document.getElementById('filePath').value.trim();
    
    if (!filePath) {
        showError('analysisResults', 'Please enter a file path');
        return;
    }

    showLoading('analysisResults');
    const startTime = Date.now();

    try {
        const response = await apiCall('/dependencies/analyze?filePath=' + encodeURIComponent(filePath), {
            method: 'POST'
        });

        const time = Date.now() - startTime;
        document.getElementById('analysisTime').textContent = `Time: ${time}ms`;

        displayAnalysisResults(response);
        loadMetrics();
        loadCircularDependencies();
        loadImpactAnalysis(filePath);

    } catch (error) {
        showError('analysisResults', error);
    }
}

function displayAnalysisResults(data) {
    const resultsDiv = document.getElementById('analysisResults');
    
    if (data.status === 'success') {
        resultsDiv.innerHTML = `
            <div class="alert alert-success">
                <strong>Analysis Complete</strong>
            </div>
            <pre>${formatJson({
                rootFile: data.rootFile,
                totalFiles: data.totalFiles,
                maxDepth: data.maxDepth,
                timestamp: new Date(data.timestamp).toLocaleString()
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

async function loadMetrics() {
    showLoading('metricsPanel');
    
    try {
        const response = await apiCall('/dependencies/metrics');
        
        if (response.status === 'success') {
            document.getElementById('metricsPanel').innerHTML = `
                <pre>${formatJson(response.metrics)}</pre>
            `;
        } else {
            showError('metricsPanel', response.message);
        }
    } catch (error) {
        showError('metricsPanel', error);
    }
}

async function loadCircularDependencies() {
    showLoading('circularPanel');
    
    try {
        const response = await apiCall('/dependencies/circular-dependencies');
        
        if (response.status === 'success') {
            const data = response.data || [];
            if (data.length === 0) {
                document.getElementById('circularPanel').innerHTML = `
                    <div class="alert alert-success">
                        No circular dependencies found
                    </div>
                `;
            } else {
                document.getElementById('circularPanel').innerHTML = `
                    <div class="alert alert-warning">
                        Found ${data.length} circular dependencies
                    </div>
                    <pre>${formatJson(data)}</pre>
                `;
            }
        } else {
            showError('circularPanel', response.message);
        }
    } catch (error) {
        showError('circularPanel', error);
    }
}

async function loadImpactAnalysis(filePath) {
    showLoading('impactPanel');
    
    try {
        const response = await apiCall('/dependencies/' + encodeURIComponent(filePath) + '/impact');
        
        if (response.status === 'success') {
            document.getElementById('impactPanel').innerHTML = `
                <pre>${formatJson({
                    analyzedFile: response.analyzedFile,
                    directDependents: response.directDependents,
                    transitiveDependents: response.transitiveDependents,
                    impactRadius: response.impactRadius,
                    impactScore: response.impactScore,
                    riskLevel: response.riskLevel
                })}</pre>
            `;
        } else {
            showError('impactPanel', response.message);
        }
    } catch (error) {
        showError('impactPanel', error);
    }
}

function clearResults() {
    document.getElementById('filePath').value = '';
    document.getElementById('analysisResults').innerHTML = '<p class="text-muted">Click "Analyze" to see dependency analysis results...</p>';
    document.getElementById('detailedResults').innerHTML = '<p class="text-muted">Results will appear here...</p>';
    document.getElementById('metricsPanel').innerHTML = '<p class="text-muted">Metrics will appear here...</p>';
    document.getElementById('circularPanel').innerHTML = '<p class="text-muted">Circular dependencies will appear here...</p>';
    document.getElementById('impactPanel').innerHTML = '<p class="text-muted">Impact analysis will appear here...</p>';
    document.getElementById('analysisTime').textContent = 'Time: --';
}

