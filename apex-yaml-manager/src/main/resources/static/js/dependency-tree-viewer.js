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

/**
 * Utility function for safe DOM element access with error handling
 */
function safeGetElement(elementId, required = true) {
    const element = document.getElementById(elementId);
    if (!element && required) {
        console.error(`Required DOM element not found: ${elementId}`);
    }
    return element;
}

/**
 * Utility function for safe fetch with comprehensive error handling
 */
async function safeFetch(url, options = {}) {
    try {
        // Validate URL
        if (!url || typeof url !== 'string') {
            throw new Error('Invalid URL provided to fetch');
        }

        const response = await fetch(url, options);

        // Check if response is ok
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        // Validate response has content
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            throw new Error('Response is not valid JSON');
        }

        const data = await response.json();
        return { success: true, data };

    } catch (error) {
        console.error('Fetch error:', error);
        return { success: false, error: error.message };
    }
}

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

    // Validate required elements exist
    if (!divider || !mainContainer || !leftPanel || !rightPanel) {
        console.warn('Resizer initialization failed: missing required DOM elements');
        return;
    }

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
    // Helper function to safely add event listeners
    function safeAddEventListener(elementId, event, handler, description) {
        const element = document.getElementById(elementId);
        if (element) {
            element.addEventListener(event, handler);
        } else {
            console.warn(`Element not found for ${description}: ${elementId}`);
        }
    }

    // Setup all event listeners with validation
    safeAddEventListener('expandAllBtn', 'click', () => expandAll(), 'expand all button');
    safeAddEventListener('collapseAllBtn', 'click', () => collapseAll(), 'collapse all button');
    safeAddEventListener('refreshBtn', 'click', () => loadDependencyTree(), 'refresh button');
    safeAddEventListener('searchInput', 'input', filterTree, 'search input');
    safeAddEventListener('loadFolderBtn', 'click', () => openFolderModal(), 'load folder button');
    safeAddEventListener('modalCloseBtn', 'click', () => closeFolderModal(), 'modal close button');
    safeAddEventListener('modalCancelBtn', 'click', () => closeFolderModal(), 'modal cancel button');
    safeAddEventListener('scanFolderBtn', 'click', () => scanFolder(), 'scan folder button');
    safeAddEventListener('loadSelectedBtn', 'click', () => loadSelectedFiles(), 'load selected button');
    safeAddEventListener('browseFolderBtn', 'click', () => browseFolderDialog(), 'browse folder button');
    safeAddEventListener('folderInput', 'change', handleFolderSelection, 'folder input');

    // Level expansion buttons (optional elements)
    const lvl1Btn = document.getElementById('expandLevel1Btn');
    const lvl2Btn = document.getElementById('expandLevel2Btn');
    const lvl3Btn = document.getElementById('expandLevel3Btn');
    if (lvl1Btn) lvl1Btn.addEventListener('click', () => expandToLevel(1));
    if (lvl2Btn) lvl2Btn.addEventListener('click', () => expandToLevel(2));
    if (lvl3Btn) lvl3Btn.addEventListener('click', () => expandToLevel(3));

    // Folder path input with Enter key support and styling reset
    safeAddEventListener('folderPathInput', 'keypress', function(e) {
        if (e.key === 'Enter') scanFolder();
    }, 'folder path input');

    // Reset styling when user types manually
    safeAddEventListener('folderPathInput', 'input', function(e) {
        const input = e.target;
        if (input.style.fontStyle === 'italic') {
            input.style.fontStyle = 'normal';
            input.style.color = '';
        }
    }, 'folder path input styling reset');

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
        // Validate treeView element exists
        const treeView = safeGetElement('treeView');
        if (!treeView) {
            return;
        }

        // If no rootFile provided, show empty state
        if (!rootFile) {
            treeView.innerHTML =
                '<div class="empty-state"><p>📂 Click "Load Folder" to select YAML files and view the dependency tree</p></div>';
            return;
        }

        // Validate rootFile parameter
        if (typeof rootFile !== 'string' || rootFile.trim() === '') {
            console.error('Invalid rootFile parameter:', rootFile);
            treeView.innerHTML =
                '<div class="empty-state"><p>Error: Invalid file path provided</p></div>';
            return;
        }

        // Use safe fetch utility
        const result = await safeFetch(`${API_BASE}/dependencies/tree?rootFile=${encodeURIComponent(rootFile)}`);

        if (!result.success) {
            throw new Error(result.error);
        }

        const data = result.data;

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
        // Validate input parameter
        if (!filePath || typeof filePath !== 'string') {
            console.error('Invalid filePath parameter:', filePath);
            displayNodeDetails(null);
            return;
        }

        const response = await fetch(`${API_BASE}/dependencies/${encodeURIComponent(filePath)}/details`);

        // Validate response
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const data = await response.json();

        if (data.status === 'success') {
            displayNodeDetails(data.data);
        } else {
            console.error('Server error fetching node details:', data.message || 'Unknown error');
            displayNodeDetails(null);
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
    // Validate required DOM elements exist
    const nodeName = document.getElementById('nodeName');
    const nodeType = document.getElementById('nodeType');
    const nodeDetails = document.getElementById('nodeDetails');

    if (!nodeName || !nodeType || !nodeDetails) {
        console.error('Required DOM elements for node details not found');
        return;
    }

    if (!details) {
        nodeName.textContent = 'Error loading details';
        nodeType.textContent = '';
        nodeDetails.innerHTML =
            '<div class="empty-state"><p>Could not load node details</p></div>';
        return;
    }

    // Validate details object structure
    if (typeof details !== 'object') {
        console.error('Invalid details object:', details);
        nodeName.textContent = 'Error: Invalid data format';
        nodeType.textContent = '';
        nodeDetails.innerHTML =
            '<div class="empty-state"><p>Invalid node details format</p></div>';
        return;
    }

    // Update header with safe property access
    nodeName.textContent = `Selected: ${details.name || 'Unknown'}`;
    nodeType.textContent = `Type: ${details.type || 'Unknown'}`;

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
    // Validate input parameter
    if (!path || typeof path !== 'string') {
        console.error('Invalid path parameter for navigation:', path);
        return;
    }

    // Escape path for CSS selector to handle special characters
    const escapedPath = CSS.escape(path);
    const nodeElement = document.querySelector(`[data-path="${escapedPath}"]`);

    if (nodeElement) {
        nodeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
        nodeElement.click();
    } else {
        console.warn('Node not found for navigation:', path);
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
    const folderModal = document.getElementById('folderModal');
    const folderPathInput = document.getElementById('folderPathInput');

    if (!folderModal) {
        console.error('Folder modal element not found');
        return;
    }

    folderModal.classList.add('active');

    if (folderPathInput) {
        folderPathInput.focus();
    } else {
        console.warn('Folder path input element not found');
    }
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
    const folderInput = document.getElementById('folderInput');
    if (folderInput) {
        folderInput.click();
    } else {
        console.error('Folder input element not found');
        showScanStatus('Error: File selection not available', 'error');
    }
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

        // Extract the folder name from the relative path
        let folderDisplayName = 'Selected Folder';
        if (filePath.includes('/')) {
            // Get the root folder name from the relative path
            const pathParts = filePath.split('/');
            folderDisplayName = pathParts[0]; // First part is the root folder name
        }

        // Filter and process YAML files
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

            // Display a meaningful folder indication
            const folderPathInput = document.getElementById('folderPathInput');
            if (folderPathInput) {
                folderPathInput.value = `📁 ${folderDisplayName} (${yamlFiles.length} YAML files selected)`;
                folderPathInput.style.fontStyle = 'italic';
                folderPathInput.style.color = '#28a745';
            }

            // Display the YAML files found
            displayScannedFiles(yamlFiles);
            showScanStatus(`Found ${yamlFiles.length} YAML file(s) in folder "${folderDisplayName}"`, 'success');
        } else {
            showScanStatus('No YAML files found in selected folder', 'error');
        }
    }
}

/**
 * Scan folder for YAML files
 */
async function scanFolder() {
    const folderPathInput = document.getElementById('folderPathInput');
    const loadSelectedBtn = document.getElementById('loadSelectedBtn');

    // Validate required DOM elements
    if (!folderPathInput) {
        console.error('Folder path input element not found');
        showScanStatus('Error: UI element not found', 'error');
        return;
    }

    const folderPath = folderPathInput.value.trim();

    // Validate folder path input
    if (!folderPath) {
        showScanStatus('Please enter a folder path', 'error');
        return;
    }

    // Basic path validation
    if (folderPath.length < 2) {
        showScanStatus('Please enter a valid folder path', 'error');
        return;
    }

    showScanStatus('Scanning folder...', 'loading');
    if (loadSelectedBtn) {
        loadSelectedBtn.disabled = true;
    }

    try {
        const response = await fetch(`${API_BASE}/dependencies/scan-folder?folderPath=${encodeURIComponent(folderPath)}`, {
            method: 'POST'
        });

        // Validate response
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const data = await response.json();

        if (data.status === 'success') {
            // Validate response data structure
            if (!Array.isArray(data.yamlFiles)) {
                throw new Error('Invalid response: yamlFiles should be an array');
            }
            displayScannedFiles(data.yamlFiles);
            showScanStatus(`Found ${data.totalFiles || data.yamlFiles.length} YAML file(s)`, 'success');
        } else {
            showScanStatus('Error: ' + (data.message || 'Unknown server error'), 'error');
        }
    } catch (error) {
        console.error('Folder scan error:', error);
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
        checkbox.checked = false;
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
    const loadSelectedBtn = document.getElementById('loadSelectedBtn');

    if (!loadSelectedBtn) {
        console.warn('Load selected button element not found');
        return;
    }

    const selectedCount = Array.from(checkboxes).filter(cb => cb.checked).length;
    loadSelectedBtn.disabled = selectedCount === 0;
}

/**
 * Load selected files and generate dependency tree
 */
async function loadSelectedFiles() {
    try { window.__lastStep = 'loadSelected-start'; } catch (e) {}

    const checkboxes = document.querySelectorAll('#fileList input[type="checkbox"]:checked');

    // Validate that we found checkboxes
    if (!checkboxes || checkboxes.length === 0) {
        showScanStatus('No files selected', 'error');
        return;
    }

    const selectedFiles = Array.from(checkboxes).map(cb => {
        const fileItem = cb.closest('.file-item');
        if (!fileItem || !fileItem.dataset.path) {
            console.warn('Invalid file item found:', fileItem);
            return null;
        }
        return fileItem.dataset.path;
    }).filter(path => path !== null); // Remove any null entries

    try { window.__lastStep = 'after-selection count=' + selectedFiles.length; } catch (e) {}

    // Validate that we have valid file paths
    if (selectedFiles.length === 0) {
        showScanStatus('Please select at least one valid file', 'error');
        return;
    }

    // Validate file paths are strings
    const invalidPaths = selectedFiles.filter(path => typeof path !== 'string' || path.trim() === '');
    if (invalidPaths.length > 0) {
        console.error('Invalid file paths found:', invalidPaths);
        showScanStatus('Error: Invalid file paths detected', 'error');
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
    try {
        setLoadedFolder(assumedFolder);
        try { window.__lastStep = 'after-setLoadedFolder'; } catch (e) {}
    } catch (error) {
        console.error('DEBUG: setLoadedFolder failed:', error);
        try { window.__lastStep = 'setLoadedFolder-error: ' + error.message; } catch (e) {}
    }

    try {
        try { window.__lastStep = 'deciding-dialog-vs-server'; } catch (e) {}

        // Check if files were selected from dialog (browser-based) or from server (path-based)
        // For server-based scanning, selectedFilesFromDialog will be empty
        const isFromBrowserDialog = selectedFilesFromDialog.length > 0;

        if (isFromBrowserDialog) {
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
                    // For browser-selected files, we need to send the content to the server for analysis
                    const content = await rootFileObj.text();
                    const fileName = rootFileObj.webkitRelativePath || rootFileObj.name;

                    // Send file content to server for dependency analysis
                    try {
                        const response = await fetch(`${API_BASE}/dependencies/analyze-content`, {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify({
                                fileName: fileName,
                                content: content,
                                folderPath: assumedFolder
                            })
                        });

                        if (response.ok) {
                            const data = await response.json();

                            if (data.status === 'success') {
                                // Now get the tree structure using the /tree endpoint
                                const treeResponse = await fetch(`${API_BASE}/dependencies/tree?rootFile=${encodeURIComponent(fileName)}`);
                                if (treeResponse.ok) {
                                    const treeData = await treeResponse.json();

                                    if (treeData.status === 'success') {
                                        // Use the existing tree rendering logic
                                        const vr = validateTreePayload(treeData);
                                        if (!vr.ok) {
                                            showValidationErrors(vr.errors);
                                            return;
                                        }

                                        window.treeData = treeData.tree ? [treeData.tree] : [];
                                        parentByPath = {};
                                        buildParentIndex(window.treeData);
                                        expansionState = {};
                                        maxRenderDepth = Infinity;
                                        renderTree(window.treeData);
                                    } else {
                                        throw new Error('Failed to get tree structure: ' + (treeData.message || 'Unknown error'));
                                    }
                                } else {
                                    throw new Error('Failed to fetch tree structure');
                                }
                            } else {
                                throw new Error('Failed to analyze file content: ' + (data.message || 'Unknown error'));
                            }
                        } else {
                            throw new Error('Failed to analyze file content');
                        }
                    } catch (error) {
                        console.error('Error analyzing file content:', error);
                        // Fallback: try to load using the file path
                        await loadDependencyTree(fileName);
                    }
                }
            }
        } else {
            try { window.__lastStep = 'branch-server'; } catch (e) {}
            try { window.__lastStep = 'before-loadDependencyTree'; } catch (e) {}

            // Files from server path - use the full absolute path
            // rootFile should be the full path returned by the server scan
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

