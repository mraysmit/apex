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
let useD3 = false; // Feature flag: use D3 HTML-based renderer when true (default OFF for stability)

// Base URLs derived from server-provided config when available
const API_BASE = (window.yamlManagerConfig && window.yamlManagerConfig.apiBaseUrl) ? window.yamlManagerConfig.apiBaseUrl : '/yaml-manager/api';
const UI_BASE = API_BASE.replace(/\/api$/, '');

// Parent index for quick ancestor expansion during search
let parentByPath = {};

function buildParentIndex(nodes, parentPath = null) {
    if (!nodes) return;
    const arr = Array.isArray(nodes) ? nodes : [nodes];
    arr.forEach(n => {
        if (n && n.path) {
            if (parentPath !== null) parentByPath[n.path] = parentPath;
            if (n.children && n.children.length) {
                buildParentIndex(n.children, n.path);
            }
        }
    });
}

document.addEventListener('DOMContentLoaded', function() {
    try { window.__lastStep = 'dom-ready'; } catch (e) { /* ignore */ }


    initializeResizer();
    setupEventListeners();
    initLoadedFolderBadge();
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

    // If navigated with #load, open folder modal automatically
    try {
        if (window.location && window.location.hash === '#load') {
            openFolderModal();
            // Clean up the hash so refresh doesn't re-open unintentionally
            if (history && history.replaceState) {
                history.replaceState(null, '', window.location.pathname);
            }
        }
    } catch (e) { /* no-op */ }



    // Loaded folder indicator helpers (persist across pages)
    function dirName(path) {
        if (!path) return '';
        const s = String(path).replace(/\\\\/g, '/');
        const idx = s.lastIndexOf('/');
        return idx > 0 ? s.substring(0, idx) : s;
    }

    function setLoadedFolder(path) {
        if (!path) return;
        try { localStorage.setItem('apexYamlFolderPath', path); } catch (e) { /* ignore */ }
        const badge = document.getElementById('loadedFolderBadge');
        if (badge) {
            badge.textContent = 'Folder: ' + path;
            badge.style.display = 'inline-flex';
        }
    }

    function initLoadedFolderBadge() {
        let p = null;
        try { p = localStorage.getItem('apexYamlFolderPath'); } catch (e) { p = null; }
        if (p) {
            const badge = document.getElementById('loadedFolderBadge');
            if (badge) {
                badge.textContent = 'Folder: ' + p;
                badge.style.display = 'inline-flex';
            }
        }
    }

    // Feature flag toggle for D3 renderer
    const d3Toggle = document.getElementById('useD3Toggle');
    if (d3Toggle) {
        // Sync UI with default flag
        d3Toggle.checked = !!useD3;
        d3Toggle.addEventListener('change', (e) => {
            useD3 = e.target.checked;
            if (treeData) {
                const nodes = Array.isArray(treeData) ? treeData : [treeData];
                renderTree(nodes);
            }
        });
    }
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

        const response = await fetch(`${API_BASE}/dependencies/tree?rootFile=${encodeURIComponent(rootFile)}`);
        const data = await response.json();

        if (data.status === 'success') {
            // Client-side pre-render validation
            const vr = validateTreePayload(data);
            if (!vr.ok) {
                showValidationErrors(vr.errors);
                return;
            }
            // The API returns the tree in the 'tree' field
            treeData = data.tree ? [data.tree] : [];
            // Rebuild parent index for ancestor expansion
            parentByPath = {};
            buildParentIndex(treeData);

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
 * Client-side pre-render validator for the /tree payload
 */
function validateTreePayload(payload) {
    const errors = [];
    const stats = { nodeCount: 0, uniquePaths: 0, duplicatePaths: 0, maxDepthObserved: 0 };

    if (!payload || payload.status !== 'success') {
        errors.push('Invalid response status');
        return { ok: false, errors, stats };
    }
    const root = payload.tree;
    if (!root) {
        errors.push('Missing tree');
        return { ok: false, errors, stats };
    }

    const seen = new Set();
    const dups = new Set();
    const stack = [root];
    while (stack.length) {
        const n = stack.pop();
        stats.nodeCount++;
        if (!n || typeof n !== 'object') { errors.push('Encountered non-object node'); continue; }
        if (!n.path || typeof n.path !== 'string') errors.push('Node missing path');
        if (!n.name || typeof n.name !== 'string') errors.push(`Node missing name for path=${n.path || 'unknown'}`);
        if (n.depth != null && typeof n.depth === 'number') stats.maxDepthObserved = Math.max(stats.maxDepthObserved, n.depth);
        if (n.path) {
            if (seen.has(n.path)) dups.add(n.path); else seen.add(n.path);
        }
        if (n.children != null && !Array.isArray(n.children)) errors.push(`Children must be an array for path=${n.path || 'unknown'}`);
        if (Array.isArray(n.children)) n.children.forEach(c => stack.push(c));
    }
    stats.uniquePaths = seen.size; stats.duplicatePaths = dups.size;
    return { ok: errors.length === 0, errors, stats };
}

function showValidationErrors(errors) {
    const treeView = document.getElementById('treeView');
    const list = errors.map(e => `<li>${e}</li>`).join('');
    treeView.innerHTML = `<div class="empty-state"><p>Validation failed before rendering</p><ul style="text-align:left; font-size:12px; color:#b00;">${list}</ul></div>`;
}


/**
 * Render the tree structure
 */
function renderTree(nodes, parentElement = null, depth = 0) {
    if (!parentElement) {
        if (useD3) {
            return renderTreeD3(nodes);
        }
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

/**
 * D3-based HTML renderer: renders visible nodes as div.tree-node with indents
 */
function renderTreeD3(nodes) {
    const container = d3.select('#treeView');
    container.html('');
    if (!nodes || nodes.length === 0) return;

    // Build flat list of visible nodes with depth and isLast sibling flag
    const flat = [];
    function traverse(node, depth = 0, isLast = true) {
        flat.push({ node, depth, isLast });
        const hasChildren = node.children && node.children.length > 0;
        const isExpanded = expansionState[node.path] !== false; // default expanded
        if (hasChildren && isExpanded && (depth + 1) < maxRenderDepth) {
            node.children.forEach((child, idx) => {
                traverse(child, depth + 1, idx === node.children.length - 1);
            });
        }
    }
    nodes.forEach((n, idx) => traverse(n, 0, idx === nodes.length - 1));

    const rows = container.selectAll('div.tree-node')
        .data(flat, d => d.node.path);

    const rowsEnter = rows.enter().append('div')
        .attr('class', 'tree-node')
        .attr('data-path', d => d.node.path)
        .attr('data-depth', d => d.depth);

    // Update + enter
    rowsEnter.merge(rows)
        .style('margin-left', d => `${d.depth * 20}px`)
        .each(function(d) {
            const el = d3.select(this);
            el.classed('selected', selectedNode && selectedNode.path === d.node.path);
            el.html(''); // clear

            const hasChildren = d.node.children && d.node.children.length > 0;
            const isExpanded = expansionState[d.node.path] !== false;
            const toggle = hasChildren ? (isExpanded ? '▾' : '▸') : ' ';
            const prefix = d.isLast ? '└── ' : '├── ';

            if (hasChildren) {
                el.append('span')
                  .text(toggle + ' ')
                  .style('margin-right', '6px')
                  .style('cursor', 'pointer')
                  .on('click', function(e) {
                      e.stopPropagation();
                      toggleNode(d.node.path);
                  });
            } else {
                el.append('span').text('   ');
            }

            el.append('span')
              .text(`${prefix}${hasChildren ? '📦' : '📄'} ${d.node.name}`);

            el.on('click', function(e) {
                e.stopPropagation();
                selectedNode = d.node;
                d3.selectAll('.tree-node').classed('selected', false);
                d3.select(this).classed('selected', true);
                fetchNodeDetails(d.node.path);
            });
        });

    rows.exit().remove();
}


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
        const response = await fetch(`${API_BASE}/dependencies/${encodeURIComponent(filePath)}/details`);
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

    // Content Summary badges (Phase 2)
    if (details.contentSummary) {
        const cs = details.contentSummary;
        const badge = (label, count) => count && count > 0 ? `<span class="badge-kv">${label}: ${count}</span>` : '';
        const extras = cs.contentCounts ? Object.entries(cs.contentCounts)
            .map(([k,v]) => badge(k, v)).join('') : '';
        html += `
            <div class="detail-section">
                <h3>🧾 Summary:</h3>
                <div class="badges" style="margin-top:6px;">
                    ${badge('Rules', cs.ruleCount)}
                    ${badge('Rule Groups', cs.ruleGroupCount)}
                    ${badge('Enrichments', cs.enrichmentCount)}
                    ${badge('Configs', cs.configFileCount)}
                    ${badge('References', cs.referenceCount)}
                    ${extras}
                </div>
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

    // CTAs (Phase 2)
    const encodedPath = encodeURIComponent(details.path);
    html += `
        <div class="detail-section">
            <h3>🚀 Actions:</h3>
            <div class="cta cta-row" style="margin-top:6px; display:flex; gap:8px; flex-wrap:wrap;">
                <a href="${UI_BASE}/ui/catalog?path=${encodedPath}" class="btn-cta" title="Open this YAML in the Catalog browser">Open in Catalog</a>
                <a href="${UI_BASE}/ui/validation?path=${encodedPath}" class="btn-cta" title="Run validation checks for this YAML">Run Validation</a>
                <button type="button" onclick="runImpactAnalysis('${details.path}')" class="btn-cta" title="Analyze impact for downstream dependents">Impact Analysis</button>
            </div>
            <div id="impactAnalysisPanel" class="mt-2"></div>
        </div>
    `;

    document.getElementById('nodeDetails').innerHTML = html;

/**
 * Run impact analysis for the selected file and render a brief summary.
 */
async function runImpactAnalysis(filePath) {
    const panel = document.getElementById('impactAnalysisPanel');
    if (panel) panel.innerHTML = '<span class="text-muted small"><i class="fas fa-spinner fa-spin"></i> Running impact analysis...</span>';
    try {
        const res = await fetch(`/yaml-manager/api/dependencies/${encodeURIComponent(filePath)}/impact`);
        const data = await res.json();
        const panel = document.getElementById('impactAnalysisPanel');
        if (data.status === 'success') {
            panel.innerHTML = `
                <div class="impact-summary small">
                    <div><strong>Risk:</strong> ${data.riskLevel} | <strong>Impact Score:</strong> ${data.impactScore} | <strong>Radius:</strong> ${data.impactRadius}</div>
                    <div><strong>Direct dependents:</strong> ${data.directDependents?.length || 0} | <strong>Transitive dependents:</strong> ${data.transitiveDependents?.length || 0}</div>
                    <div class="mt-1"><strong>Recommendation:</strong> ${data.recommendation || '—'}</div>
                </div>`;
        } else {
            panel.innerHTML = `<div style="color:#b00; font-size:12px;">Error: ${data.message || 'Failed to run impact analysis'}</div>`;
        }
    } catch (e) {
        console.error('Impact analysis error:', e);
        const panel = document.getElementById('impactAnalysisPanel');
        if (panel) panel.innerHTML = `<div style="color:#b00; font-size:12px;">Error: ${e.message}</div>`;
    }
}

window.runImpactAnalysis = runImpactAnalysis;

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



// Expose loadDependencyTree for Selenium and console usage
window.loadDependencyTree = loadDependencyTree;

/**
 * Filter tree by search term
 */
function filterTree(e) {
    const term = (e.target.value || '').trim().toLowerCase();
    if (!treeData) return;

    // Empty search: show everything and re-render with current expansion state
    if (term === '') {
        const nodes = document.querySelectorAll('.tree-node');
        nodes.forEach(n => {
            n.style.display = 'block';
            n.classList.remove('search-match');
        });
        const nodesArr = Array.isArray(treeData) ? treeData : [treeData];
        renderTree(nodesArr);
        return;
    }

    // Ensure parent index is available
    if (!parentByPath || Object.keys(parentByPath).length === 0) {
        buildParentIndex(treeData);
    }

    const matchSet = new Set();
    const visibleSet = new Set();
    const roots = Array.isArray(treeData) ? treeData : [treeData];

    function walk(node) {
        const name = (node.name || '').toLowerCase();
        const path = (node.path || '').toLowerCase();
        const isMatch = name.includes(term) || path.includes(term);
        if (isMatch) {
            matchSet.add(node.path);
            // Expand ancestors so the match path is visible
            let p = node.path;
            while (p && parentByPath[p]) {
                const parent = parentByPath[p];
                visibleSet.add(parent);
                expansionState[parent] = true;
                p = parent;
            }
            // Include the node itself
            visibleSet.add(node.path);
            expansionState[node.path] = true;
        }
        if (node.children && node.children.length) {
            node.children.forEach(walk);
        }
    }
    roots.forEach(walk);

    // Re-render to apply updated expansion state (works for manual and D3 renderers)
    renderTree(roots);

    // Show only matches and their ancestors; highlight matches
    document.querySelectorAll('.tree-node').forEach(el => {
        const p = el.getAttribute('data-path');
        if (visibleSet.has(p) || matchSet.has(p)) {
            el.style.display = 'block';
            el.classList.toggle('search-match', matchSet.has(p));
        } else {
            el.style.display = 'none';
            el.classList.remove('search-match');
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
        const response = await fetch(`${API_BASE}/dependencies/scan-folder?folderPath=${encodeURIComponent(folderPath)}`, {
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
async function loadSelectedFiles() { try { window.__lastStep = 'loadSelected-start'; } catch (e) {}
    const checkboxes = document.querySelectorAll('#fileList input[type="checkbox"]:checked');
    const selectedFiles = Array.from(checkboxes).map(cb => {
        const fileItem = cb.closest('.file-item');
        return fileItem.dataset.path;
    });

    try { window.__lastStep = 'after-selection count=' + selectedFiles.length; } catch (e) {}

    if (selectedFiles.length === 0) {
        showScanStatus('Please select at least one file', 'error');
        return;
    }

    // Capture modal path BEFORE closing modal (since closeFolderModal clears the input)
    const modalPath = (document.getElementById('folderPathInput') || {}).value;

    try { window.__lastStep = 'before-closeModal'; } catch (e) {}

    // Be resilient: closing the modal is purely cosmetic for the flow
    try {
        closeFolderModal();
    } catch (e) {
        console.error('closeFolderModal failed (non-fatal):', e);
        try { window.__lastStep = 'closeModal-error'; } catch (ee) {}
    }

    // Load the first selected file as root
    const rootFile = selectedFiles[0];

    // Persist and show selected folder in header
    try { window.__lastStep = 'before-assumedFolder'; } catch (e) {}
    const assumedFolder = modalPath && modalPath.trim() ? modalPath.trim() : dirName(rootFile);
    try { window.__lastStep = 'before-setLoadedFolder'; } catch (e) {}
    setLoadedFolder(assumedFolder);
    try { window.__lastStep = 'after-setLoadedFolder'; } catch (e) {}

    try {
        try { window.__lastStep = 'deciding-dialog-vs-server'; } catch (e) {}

        // Check if files were selected from dialog (browser-based) or from server (path-based)
        if (selectedFilesFromDialog.length > 0) {
            try { window.__lastStep = 'branch-dialog'; } catch (e) {}

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
            try { window.__lastStep = 'branch-server'; } catch (e) {}
            try { window.__lastStep = 'before-loadDependencyTree'; } catch (e) {}

            // Files from server path - reuse main loader for consistency
            await loadDependencyTree(rootFile);
            try { window.__lastStep = 'after-loadDependencyTree'; } catch (e) {}
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

// Expose functions to global scope for testing
window.loadDependencyTree = loadDependencyTree;
window.selectNode = selectNode;
window.expandNode = expandNode;
window.collapseNode = collapseNode;

