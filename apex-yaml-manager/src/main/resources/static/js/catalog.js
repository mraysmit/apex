/**
 * Catalog Browser UI JavaScript
 */

let allConfigurations = [];

document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
    loadCatalog();
});

function setupEventListeners() {
    document.getElementById('refreshBtn').addEventListener('click', loadCatalog);
    document.getElementById('searchInput').addEventListener('input', filterCatalog);
    document.getElementById('categoryFilter').addEventListener('change', filterCatalog);
}

async function loadCatalog() {
    const tableDiv = document.getElementById('catalogTable');
    tableDiv.innerHTML = '<p class="text-muted"><i class="fas fa-spinner fa-spin"></i> Loading configurations...</p>';

    try {
        const response = await apiCall('/catalog/all');
        
        if (response.status === 'success') {
            allConfigurations = response.data || [];
            
            // Populate category filter
            const categories = new Set();
            allConfigurations.forEach(config => {
                if (config.categories) {
                    config.categories.forEach(cat => categories.add(cat));
                }
            });
            
            const categorySelect = document.getElementById('categoryFilter');
            const currentValue = categorySelect.value;
            categorySelect.innerHTML = '<option value="">All Categories</option>';
            categories.forEach(cat => {
                const option = document.createElement('option');
                option.value = cat;
                option.textContent = cat;
                categorySelect.appendChild(option);
            });
            categorySelect.value = currentValue;
            
            displayCatalog(allConfigurations);
        } else {
            tableDiv.innerHTML = `<div class="alert alert-danger">Error: ${response.message}</div>`;
        }
    } catch (error) {
        tableDiv.innerHTML = `<div class="alert alert-danger">Error loading catalog: ${error.message}</div>`;
    }
}

function displayCatalog(configurations) {
    const tableDiv = document.getElementById('catalogTable');
    
    if (configurations.length === 0) {
        tableDiv.innerHTML = '<p class="text-muted">No configurations found</p>';
        return;
    }

    let html = `
        <table class="table table-hover">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Type</th>
                    <th>Categories</th>
                    <th>Health</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
    `;

    configurations.forEach(config => {
        const healthClass = getHealthClass(config.healthScore);
        const categoriesHtml = config.categories ? config.categories.map(c => 
            `<span class="badge bg-secondary">${c}</span>`
        ).join(' ') : '';

        html += `
            <tr>
                <td><code>${config.id}</code></td>
                <td>${config.name || '--'}</td>
                <td><span class="badge bg-info">${config.type || '--'}</span></td>
                <td>${categoriesHtml}</td>
                <td>
                    <span class="badge ${healthClass}">
                        ${config.healthScore || 0}
                    </span>
                </td>
                <td>
                    <button class="btn btn-sm btn-outline-primary" 
                            onclick="showDetails('${config.id}')">
                        <i class="fas fa-eye"></i> View
                    </button>
                </td>
            </tr>
        `;
    });

    html += `
            </tbody>
        </table>
    `;

    tableDiv.innerHTML = html;
}

function filterCatalog() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const categoryFilter = document.getElementById('categoryFilter').value;

    const filtered = allConfigurations.filter(config => {
        const matchesSearch = !searchTerm || 
            config.id.toLowerCase().includes(searchTerm) ||
            (config.name && config.name.toLowerCase().includes(searchTerm)) ||
            (config.description && config.description.toLowerCase().includes(searchTerm));
        
        const matchesCategory = !categoryFilter || 
            (config.categories && config.categories.includes(categoryFilter));

        return matchesSearch && matchesCategory;
    });

    displayCatalog(filtered);
}

async function showDetails(configId) {
    try {
        const response = await apiCall('/catalog/' + encodeURIComponent(configId));
        
        if (response.status === 'success') {
            const config = response.data;
            const detailsDiv = document.getElementById('detailsContent');
            
            detailsDiv.innerHTML = `
                <div class="row mb-3">
                    <div class="col-md-6">
                        <h6>ID</h6>
                        <p><code>${config.id}</code></p>
                    </div>
                    <div class="col-md-6">
                        <h6>Name</h6>
                        <p>${config.name || '--'}</p>
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-md-6">
                        <h6>Type</h6>
                        <p>${config.type || '--'}</p>
                    </div>
                    <div class="col-md-6">
                        <h6>Version</h6>
                        <p>${config.version || '--'}</p>
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-12">
                        <h6>Description</h6>
                        <p>${config.description || '--'}</p>
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-md-6">
                        <h6>Author</h6>
                        <p>${config.author || '--'}</p>
                    </div>
                    <div class="col-md-6">
                        <h6>Health Score</h6>
                        <p><span class="badge ${getHealthClass(config.healthScore)}">${config.healthScore || 0}</span></p>
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-12">
                        <h6>Categories</h6>
                        <p>${config.categories ? config.categories.map(c => `<span class="badge bg-secondary">${c}</span>`).join(' ') : '--'}</p>
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-12">
                        <h6>Tags</h6>
                        <p>${config.tags ? config.tags.map(t => `<span class="badge bg-info">${t}</span>`).join(' ') : '--'}</p>
                    </div>
                </div>
            `;
            
            new bootstrap.Modal(document.getElementById('detailsModal')).show();
        }
    } catch (error) {
        alert('Error loading details: ' + error.message);
    }
}

function getHealthClass(score) {
    if (score >= 80) return 'bg-success';
    if (score >= 60) return 'bg-info';
    if (score >= 40) return 'bg-warning';
    return 'bg-danger';
}

