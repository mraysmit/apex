/**
 * Dependency Tree Viewer - JavaScript
 */

let treeData = null;
let selectedNode = null;
let isResizing = false;
let leftPanelWidth = 30;
let selectedFilesFromDialog = [];
// Controls how many levels are rendered; Infinity means fully expanded
let maxRenderDepth = Infinity;
// Per-node expansion state: path -> boolean (true=expanded, false=collapsed)
let expansionState = {};

document.addEventListener('DOMContentLoaded', function() {
    initializeResizer();
    setupEventListeners();
    loadDependencyTree();
});

/**
 * Initialize the resizable divider
 */
function initializeResizer() {
    const divider = document.getElementById('divider');
    const mainContainer = document.querySelector('.main-container');
    const leftPanel = document.querySelector('.left-panel');
    const rightPanel = document.querySelector('.right-panel');

    divider.addEventListener('mousedown', function(e) {
        isResizing = true;
        document.body.style.cursor = 'col-resize';
        document.body.style.userSelect = 'none';
    });

    document.addEventListener('mousemove', function(e) {
        if (!isResizing) return;

        const containerWidth = mainContainer.offsetWidth;
        const newLeftWidth = (e.clientX / containerWidth) * 100;

        if (newLeftWidth > 20 && newLeftWidth < 80) {
            leftPanel.style.width = newLeftWidth + '%';
            rightPanel.style.width = (100 - newLeftWidth - 0.3) + '%';
            leftPanelWidth = newLeftWidth;
        }
    });

    document.addEventListener('mouseup', function() {
        isResizing = false;
        document.body.style.cursor = 'default';
        document.body.style.userSelect = 'auto';
    });
}

/**
 * Setup event listeners for toolbar buttons
 */
function setupEventListeners() {
    document.getElementById('expandAllBtn').addEventListener('click', expandAll);
    document.getElementById('collapseAllBtn').addEventListener('click', collapseAll);
    const lvl1Btn = document.getElementById('expandLevel1Btn');
    const lvl2Btn = document.getElementById('expandLevel2Btn');
    const lvl3Btn = document.getElementById('expandLevel3Btn');
    if (lvl1Btn) lvl1Btn.addEventListener('click', () => expandToLevel(1));
    if (lvl2Btn) lvl2Btn.addEventListener('click', () => expandToLevel(2));
    if (lvl3Btn) lvl3Btn.addEventListener('click', () => expandToLevel(3));
    document.getElementById('refreshBtn').addEventListener('click', loadDependencyTree);
    document.getElementById('searchInput').addEventListener('input', filterTree);
    document.getElementById('loadFolderBtn').addEventListener('click', openFolderModal);
    document.getElementById('modalCloseBtn').addEventListener('click', closeFolderModal);
    document.getElementById('modalCancelBtn').addEventListener('click', closeFolderModal);
    document.getElementById('scanFolderBtn').addEventListener('click', scanFolder);
    document.getElementById('loadSelectedBtn').addEventListener('click', loadSelectedFiles);
    document.getElementById('browseFolderBtn').addEventListener('click', browseFolderDialog);
    document.getElementById('folderInput').addEventListener('change', handleFolderSelection);
    document.getElementById('folderPathInput').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') scanFolder();
    });
}

/**
 * Load dependency tree from API
 */
async function loadDependencyTree(rootFile = null) {
    try {
        // If no rootFile provided, show empty state
        if (!rootFile) {
            document.getElementById('treeView').innerHTML =
                '<div class="empty-state"><p>📂 Click "Load Folder" to select YAML files and view the dependency tree</p></div>';
            return;
        }

        const response = await fetch('/yaml-manager/api/dependencies/tree?rootFile=' + encodeURIComponent(rootFile));
        const data = await response.json();

        if (data.status === 'success') {
            // The API returns the tree in the 'tree' field
            treeData = data.tree ? [data.tree] : [];
            // Expose for Selenium and console
            window.treeData = treeData;
            // Reset expansion state on load
            expansionState = {};
            maxRenderDepth = Infinity;
            renderTree(treeData);
        } else {
            document.getElementById('treeView').innerHTML =
                '<div class="empty-state"><p>Error: ' + (data.message || 'Failed to load tree') + '</p></div>';
        }
    } catch (error) {
        console.error('Error loading tree:', error);
        document.getElementById('treeView').innerHTML =
            '<div class="empty-state"><p>Error loading dependency tree</p></div>';
    }
}

/**
 * Render the tree structure
 */
function renderTree(nodes, parentElement = null, depth = 0) {
    if (!parentElement) {
        parentElement = document.getElementById('treeView');
        parentElement.innerHTML = '';
    }

    if (!nodes || nodes.length === 0) return;

    nodes.forEach((node, index) => {
        const isLast = index === nodes.length - 1;
        const prefix = isLast ? '└── ' : '├── ';
        const hasChildren = node.children && node.children.length > 0;
        const isExpanded = expansionState[node.path] !== false; // default expanded
        const icon = hasChildren ? (isExpanded ? '📦' : '📦') : '📄';
        const toggle = hasChildren ? (isExpanded ? '▾' : '▸') : ' ';

        const nodeElement = document.createElement('div');
        nodeElement.className = `tree-node tree-node-indent-${depth}`;
        nodeElement.dataset.path = node.path;
        nodeElement.dataset.depth = depth;

        // Build inner content with a small toggle control for parents
        const labelSpan = document.createElement('span');
        labelSpan.textContent = `${prefix}${icon} ${node.name}`;

        if (hasChildren) {
            const toggleSpan = document.createElement('span');
            toggleSpan.textContent = `${toggle} `;
            toggleSpan.style.marginRight = '6px';
            toggleSpan.style.cursor = 'pointer';
            toggleSpan.addEventListener('click', function(e) {
                e.stopPropagation();
                toggleNode(node.path);
            });
            nodeElement.appendChild(toggleSpan);
        }

        nodeElement.appendChild(labelSpan);

        nodeElement.addEventListener('click', function(e) {
            e.stopPropagation();
            selectNode(node, nodeElement);
        });

        parentElement.appendChild(nodeElement);

        // Render children
        if (hasChildren) {
            // Only render children if within maxRenderDepth and this node is expanded
            if (isExpanded && (depth + 1) < maxRenderDepth) {
                renderTree(node.children, parentElement, depth + 1);
            }
        }
    });
}

/**
 * Select a node and display its details
 */
function selectNode(node, element) {
    // Remove previous selection
    document.querySelectorAll('.tree-node.selected').forEach(el => {
        el.classList.remove('selected');
    });

    // Add selection to current node
    element.classList.add('selected');
    selectedNode = node;

    // Fetch and display node details
    fetchNodeDetails(node.path);
}

/**
 * Fetch node details from API
 */
async function fetchNodeDetails(filePath) {
    try {
        const response = await fetch(`/yaml-manager/api/dependencies/${encodeURIComponent(filePath)}/details`);
        const data = await response.json();

        if (data.status === 'success') {
            displayNodeDetails(data.data);
        }
    } catch (error) {
        console.error('Error fetching node details:', error);
        displayNodeDetails(null);
    }
}

/**
 * Display node details in right panel
 */
function displayNodeDetails(details) {
    if (!details) {
        document.getElementById('nodeName').textContent = 'Error loading details';
        document.getElementById('nodeType').textContent = '';
        document.getElementById('nodeDetails').innerHTML =
            '<div class="empty-state"><p>Could not load node details</p></div>';
        return;
    }

    // Update header
    document.getElementById('nodeName').textContent = `Selected: ${details.name}`;
    document.getElementById('nodeType').textContent = `Type: ${details.type || 'Unknown'}`;

    // Build details HTML
    let html = '';

    // File Path
    html += `
        <div class="detail-section">
            <h3>📁 File Path:</h3>
            <div class="file-path">${details.path}</div>
        </div>
    `;

    // Dependencies (Direct)
    if (details.dependencies && details.dependencies.length > 0) {
        html += `
            <div class="detail-section">
                <h3>📤 Dependencies (Direct):</h3>
                <ul>
                    ${details.dependencies.map(dep =>
                        `<li><a onclick="navigateTo('${dep}')">${dep}</a></li>`
                    ).join('')}
                </ul>
            </div>
        `;
    }

    // All Dependencies (Transitive)
    if (details.allDependencies && details.allDependencies.length > 0) {
        html += `
            <div class="detail-section">
                <h3>📤 All Dependencies (Transitive):</h3>
                <ul>
                    ${details.allDependencies.map(dep =>
                        `<li><a onclick="navigateTo('${dep}')">${dep}</a></li>`
                    ).join('')}
                </ul>
            </div>
        `;
    }

    // Dependents
    if (details.dependents && details.dependents.length > 0) {
        html += `
            <div class="detail-section">
                <h3>📥 Dependents (Files that use this):</h3>
                <ul>
                    ${details.dependents.map(dep =>
                        `<li><a onclick="navigateTo('${dep}')">${dep}</a></li>`
                    ).join('')}
                </ul>
            </div>
        `;
    }

    // Health Score
    if (details.healthScore !== undefined) {
        const grade = getHealthGrade(details.healthScore);
        html += `
            <div class="detail-section">
                <h3>💚 Health Score:</h3>
                <div class="health-score ${grade.class}">${details.healthScore}/100 - ${grade.label}</div>
            </div>
        `;
    }

    // Metadata
    html += `
        <div class="detail-section">
            <h3>📋 Metadata:</h3>
            <ul>
                <li>Author: ${details.author || 'Unknown'}</li>
                <li>Created: ${details.created || 'Unknown'}</li>
                <li>Last Modified: ${details.lastModified || 'Unknown'}</li>
                <li>Version: ${details.version || 'Unknown'}</li>
            </ul>
        </div>
    `;

    // Circular Dependencies
    if (details.circularDependencies && details.circularDependencies.length > 0) {
        html += `
            <div class="detail-section">
                <h3>⚠️ Circular Dependencies:</h3>
                <ul>
                    ${details.circularDependencies.map(dep =>
                        `<li>${dep}</li>`
                    ).join('')}
                </ul>
            </div>
        `;
    } else {
        html += `
            <div class="detail-section">
                <h3>⚠️ Circular Dependencies:</h3>
                <p style="color: #198754; font-size: 11px;">None detected</p>
            </div>
        `;
    }

    document.getElementById('nodeDetails').innerHTML = html;
}

/**
 * Get health grade and styling
 */
function getHealthGrade(score) {
    if (score >= 80) return { label: 'EXCELLENT', class: 'excellent' };
    if (score >= 60) return { label: 'GOOD', class: 'good' };
    if (score >= 40) return { label: 'FAIR', class: 'fair' };
    return { label: 'POOR', class: 'poor' };
}

/**
 * Navigate to a node by path
 */
function navigateTo(path) {
    const nodeElement = document.querySelector(`[data-path="${path}"]`);
    if (nodeElement) {
        nodeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
        nodeElement.click();
    }
}

/**
 * Toggle a node's expanded/collapsed state and re-render
 */
function toggleNode(path) {
    const current = expansionState[path];
    expansionState[path] = current === false ? true : false;
    if (treeData) {
        const nodes = Array.isArray(treeData) ? treeData : [treeData];
        renderTree(nodes);
    }
}

window.toggleNode = toggleNode;

/**
 * Expand all nodes
 */
function expandAll() {
    maxRenderDepth = Infinity;
    if (treeData) {
        const nodes = Array.isArray(treeData) ? treeData : [treeData];
        renderTree(nodes);
    }
}

/**
 * Collapse all nodes
 */
function collapseAll() {
    // Show only root
    maxRenderDepth = 1;
    if (treeData) {
        const nodes = Array.isArray(treeData) ? treeData : [treeData];
        renderTree(nodes);
    }
}

/**
 * Expand to a specific level (1=root only, 2=root+children, ...)
 */
function expandToLevel(level) {
    const lvl = Math.max(1, parseInt(level, 10) || 1);
    maxRenderDepth = lvl;
    if (treeData) {
        const nodes = Array.isArray(treeData) ? treeData : [treeData];
        renderTree(nodes);
    }
}

// Expose for Selenium and console usage
window.expandToLevel = expandToLevel;


/**
 * Filter tree by search term
 */
function filterTree(e) {
    const searchTerm = e.target.value.toLowerCase();
    const nodes = document.querySelectorAll('.tree-node');

    nodes.forEach(node => {
        const text = node.textContent.toLowerCase();
        if (searchTerm === '' || text.includes(searchTerm)) {
            node.style.display = 'block';
        } else {
            node.style.display = 'none';
        }
    });
}

/**
 * Open folder selector modal
 */
function openFolderModal() {
    document.getElementById('folderModal').classList.add('active');
    document.getElementById('folderPathInput').focus();
}

/**
 * Close folder selector modal
 */
function closeFolderModal() {
    document.getElementById('folderModal').classList.remove('active');
    document.getElementById('folderPathInput').value = '';
    document.getElementById('fileListContainer').style.display = 'none';
    document.getElementById('scanStatus').style.display = 'none';
    document.getElementById('fileList').innerHTML = '';
    document.getElementById('loadSelectedBtn').disabled = true;
}

/**
 * Open file system dialog to browse for folder
 */
function browseFolderDialog() {
    document.getElementById('folderInput').click();
}

/**
 * Handle folder selection from file dialog
 */
function handleFolderSelection(event) {
    const files = event.target.files;
    if (files && files.length > 0) {
        // Get the folder path from the first file
        const firstFile = files[0];
        const filePath = firstFile.webkitRelativePath || firstFile.name;

        // Extract the folder path (remove the filename)
        const folderPath = filePath.substring(0, filePath.lastIndexOf('/'));

        // Get the full path from the file system
        // For web, we'll use the relative path and scan all selected files
        const yamlFiles = Array.from(files)
            .filter(f => f.name.endsWith('.yaml') || f.name.endsWith('.yml'))
            .map(f => ({
                name: f.name,
                path: f.webkitRelativePath || f.name,
                size: f.size
            }));

        if (yamlFiles.length > 0) {
            // Store the files for later use
            selectedFilesFromDialog = yamlFiles;

            // Display the folder path
            document.getElementById('folderPathInput').value = folderPath || 'Selected Folder';

            // Display the YAML files found
            displayScannedFiles(yamlFiles);
            showScanStatus(`Found ${yamlFiles.length} YAML file(s)`, 'success');
        } else {
            showScanStatus('No YAML files found in selected folder', 'error');
        }
    }
}

/**
 * Scan folder for YAML files
 */
async function scanFolder() {
    const folderPath = document.getElementById('folderPathInput').value.trim();

    if (!folderPath) {
        showScanStatus('Please enter a folder path', 'error');
        return;
    }

    showScanStatus('Scanning folder...', 'loading');
    document.getElementById('loadSelectedBtn').disabled = true;

    try {
        const response = await fetch('/yaml-manager/api/dependencies/scan-folder?folderPath=' + encodeURIComponent(folderPath), {
            method: 'POST'
        });
        const data = await response.json();

        if (data.status === 'success') {
            displayScannedFiles(data.yamlFiles);
            showScanStatus(`Found ${data.totalFiles} YAML file(s)`, 'success');
        } else {
            showScanStatus('Error: ' + data.message, 'error');
        }
    } catch (error) {
        showScanStatus('Error scanning folder: ' + error.message, 'error');
    }
}

/**
 * Display scanned files in the modal
 */
function displayScannedFiles(files) {
    const fileList = document.getElementById('fileList');
    const fileCount = document.getElementById('fileCount');

    fileList.innerHTML = '';
    fileCount.textContent = files.length;

    if (files.length === 0) {
        fileList.innerHTML = '<div style="padding: 20px; text-align: center; color: #999;">No YAML files found</div>';
        document.getElementById('fileListContainer').style.display = 'none';
        return;
    }

    files.forEach((file, index) => {
        const fileItem = document.createElement('div');
        fileItem.className = 'file-item';
        fileItem.dataset.index = index;
        fileItem.dataset.path = file.path;

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.checked = true;
        checkbox.addEventListener('change', updateLoadButton);

        const nameSpan = document.createElement('span');
        nameSpan.className = 'file-item-name';
        nameSpan.textContent = file.name;

        const sizeSpan = document.createElement('span');
        sizeSpan.className = 'file-item-size';
        sizeSpan.textContent = formatFileSize(file.size);

        fileItem.appendChild(checkbox);
        fileItem.appendChild(nameSpan);
        fileItem.appendChild(sizeSpan);
        fileItem.addEventListener('click', function(e) {
            if (e.target !== checkbox) {
                checkbox.checked = !checkbox.checked;
                updateLoadButton();
            }
        });

        fileList.appendChild(fileItem);
    });

    document.getElementById('fileListContainer').style.display = 'block';
    updateLoadButton();
}

/**
 * Update load button state
 */
function updateLoadButton() {
    const checkboxes = document.querySelectorAll('#fileList input[type="checkbox"]');
    const selectedCount = Array.from(checkboxes).filter(cb => cb.checked).length;
    document.getElementById('loadSelectedBtn').disabled = selectedCount === 0;
}

/**
 * Load selected files and generate dependency tree
 */
async function loadSelectedFiles() {
    const checkboxes = document.querySelectorAll('#fileList input[type="checkbox"]:checked');
    const selectedFiles = Array.from(checkboxes).map(cb => {
        const fileItem = cb.closest('.file-item');
        return fileItem.dataset.path;
    });

    if (selectedFiles.length === 0) {
        showScanStatus('Please select at least one file', 'error');
        return;
    }

    closeFolderModal();

    // Load the first selected file as root
    const rootFile = selectedFiles[0];

    try {
        // Check if files were selected from dialog (browser-based) or from server (path-based)
        if (selectedFilesFromDialog.length > 0) {
            // Files from dialog - load directly from browser
            const fileInput = document.getElementById('folderInput');
            const files = fileInput.files;

            if (files && files.length > 0) {
                // Find the root file in the selected files
                const rootFileObj = Array.from(files).find(f =>
                    (f.webkitRelativePath || f.name) === rootFile
                );

                if (rootFileObj) {
                    const content = await rootFileObj.text();
                    // For now, just show a message that the file was loaded
                    // In a real implementation, you would parse the YAML and build the tree
                    document.getElementById('treeView').innerHTML =
                        '<div class="empty-state"><p>Loaded: ' + rootFileObj.name + '</p><p>File size: ' + formatFileSize(rootFileObj.size) + '</p></div>';
                }
            }
        } else {
            // Files from server path - use API
            const response = await fetch('/yaml-manager/api/dependencies/tree?rootFile=' + encodeURIComponent(rootFile));
            const data = await response.json();

            if (data.status === 'success') {
                treeData = data.tree ? [data.tree] : [];
                renderTree(treeData);
            } else {
                document.getElementById('treeView').innerHTML =
                    '<div class="empty-state"><p>Error loading tree: ' + data.message + '</p></div>';
            }
        }
    } catch (error) {
        console.error('Error loading tree:', error);
        document.getElementById('treeView').innerHTML =
            '<div class="empty-state"><p>Error loading dependency tree</p></div>';
    }
}

/**
 * Show scan status message
 */
function showScanStatus(message, type) {
    const statusDiv = document.getElementById('scanStatus');
    statusDiv.textContent = message;
    statusDiv.style.display = 'block';
    statusDiv.style.backgroundColor = type === 'error' ? '#ffebee' : type === 'success' ? '#e8f5e9' : '#f5f5f5';
    statusDiv.style.color = type === 'error' ? '#c62828' : type === 'success' ? '#2e7d32' : '#666';
}

/**
 * Format file size for display
 */
function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

