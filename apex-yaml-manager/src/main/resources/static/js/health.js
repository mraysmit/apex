/**
 * Health Check UI JavaScript
 */

document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
});

function setupEventListeners() {
    document.getElementById('checkHealthBtn').addEventListener('click', checkHealth);
    document.getElementById('reportBtn').addEventListener('click', generateReport);
    document.getElementById('clearBtn').addEventListener('click', clearResults);
}

async function checkHealth() {
    const filePath = document.getElementById('filePath').value.trim();
    
    if (!filePath) {
        showError('healthStatus', 'Please enter a file path');
        return;
    }

    showLoading('healthStatus');
    const startTime = Date.now();

    try {
        const response = await apiCall('/health-checks/check?filePath=' + encodeURIComponent(filePath), {
            method: 'POST'
        });

        const time = Date.now() - startTime;
        document.getElementById('healthTime').textContent = `Time: ${time}ms`;

        displayHealthResults(response);
        loadRecommendations(filePath);
        loadComponentScores(filePath);

    } catch (error) {
        showError('healthStatus', error);
    }
}

function displayHealthResults(data) {
    const resultsDiv = document.getElementById('healthResults');
    
    if (data.status === 'success' && data.data) {
        const score = data.data;
        const scoreClass = getScoreClass(score.overallScore);
        
        resultsDiv.innerHTML = `
            <div class="alert ${scoreClass}">
                <strong>Overall Score: ${Math.round(score.overallScore)}/100</strong>
            </div>
            <pre>${formatJson({
                overallScore: Math.round(score.overallScore),
                grade: score.grade,
                isHealthy: score.isHealthy,
                trend: score.trend,
                lastChecked: new Date(score.lastChecked).toLocaleString()
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

async function loadRecommendations(filePath) {
    showLoading('recommendationsPanel');
    
    try {
        const response = await apiCall('/health-checks/check?filePath=' + encodeURIComponent(filePath), {
            method: 'POST'
        });
        
        if (response.status === 'success' && response.data && response.data.recommendations) {
            const recommendations = response.data.recommendations;
            
            if (recommendations.length === 0) {
                document.getElementById('recommendationsPanel').innerHTML = 
                    '<div class="alert alert-success">No recommendations - configuration is healthy!</div>';
                return;
            }

            let html = '<div class="list-group">';
            
            recommendations.forEach(rec => {
                const priorityClass = getPriorityClass(rec.priority);
                html += `
                    <div class="list-group-item">
                        <div class="d-flex justify-content-between align-items-start">
                            <div>
                                <h6 class="mb-1">${rec.title}</h6>
                                <p class="mb-1">${rec.description}</p>
                                <small class="text-muted">Effort: ${rec.estimatedEffortMinutes} min</small>
                            </div>
                            <span class="badge ${priorityClass}">${rec.priority}</span>
                        </div>
                    </div>
                `;
            });
            
            html += '</div>';
            document.getElementById('recommendationsPanel').innerHTML = html;
        }
    } catch (error) {
        showError('recommendationsPanel', error);
    }
}

async function loadComponentScores(filePath) {
    showLoading('componentScoresPanel');
    
    try {
        const response = await apiCall('/health-checks/check?filePath=' + encodeURIComponent(filePath), {
            method: 'POST'
        });
        
        if (response.status === 'success' && response.data && response.data.componentScores) {
            const scores = response.data.componentScores;
            
            let html = '<div class="row">';
            
            Object.entries(scores).forEach(([component, score]) => {
                const scoreClass = getScoreClass(score);
                html += `
                    <div class="col-md-4 mb-3">
                        <div class="card">
                            <div class="card-body">
                                <h6 class="card-title">${component}</h6>
                                <div class="progress">
                                    <div class="progress-bar ${scoreClass}" 
                                         style="width: ${score}%">
                                        ${Math.round(score)}%
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
            });
            
            html += '</div>';
            document.getElementById('componentScoresPanel').innerHTML = html;
        }
    } catch (error) {
        showError('componentScoresPanel', error);
    }
}

async function generateReport() {
    const filePath = document.getElementById('filePath').value.trim();
    
    if (!filePath) {
        showError('healthStatus', 'Please enter a file path');
        return;
    }

    try {
        const response = await apiCall('/health-checks/report?filePath=' + encodeURIComponent(filePath), {
            method: 'POST'
        });

        if (response.status === 'success') {
            exportAsJson(response.data, 'health-report.json');
            showSuccess('Report generated and downloaded');
        }
    } catch (error) {
        showError('healthStatus', error);
    }
}

function getScoreClass(score) {
    if (score >= 80) return 'bg-success';
    if (score >= 60) return 'bg-info';
    if (score >= 40) return 'bg-warning';
    return 'bg-danger';
}

function getPriorityClass(priority) {
    switch(priority) {
        case 'CRITICAL': return 'bg-danger';
        case 'HIGH': return 'bg-warning';
        case 'MEDIUM': return 'bg-info';
        case 'LOW': return 'bg-secondary';
        default: return 'bg-secondary';
    }
}

function clearResults() {
    document.getElementById('filePath').value = '';
    document.getElementById('healthStatus').innerHTML = '<p class="text-muted">Click "Check Health" to start health check...</p>';
    document.getElementById('healthResults').innerHTML = '<p class="text-muted">Results will appear here...</p>';
    document.getElementById('recommendationsPanel').innerHTML = '<p class="text-muted">Recommendations will appear here...</p>';
    document.getElementById('componentScoresPanel').innerHTML = '<p class="text-muted">Component scores will appear here...</p>';
    document.getElementById('healthTime').textContent = 'Time: --';
}

