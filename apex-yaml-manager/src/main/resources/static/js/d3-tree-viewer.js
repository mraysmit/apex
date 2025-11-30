// Global variables
let svg, g, tree, root, i = 0, zoom;
const duration = 750;
const width = window.innerWidth - 400 - 20; // Account for right panel (400px) and margins
const headerHeight = 47; // Height of the tree header (15px padding top + 16px font + 1px line-height + 15px padding bottom)
const height = window.innerHeight - headerHeight;
const initialTreeTransform = d3.zoomIdentity.translate(200, 0);

// Tooltip and selection state
let tooltipTimer = null;
let tooltipEnabled = true;
let selectedNode = null;

// Tooltip delay in milliseconds
const TOOLTIP_DELAY_MS = 500;

// Color scheme for different node attributes
const nodeAttributeColors = {
    filename: '#ffffff',      // White - primary text
    type: '#74b9ff',          // Light blue
    metadataId: '#ffeaa7',    // Yellow/gold
    counts: '#55efc4',        // Mint green
    description: '#b2bec3'    // Gray
};

// Generate node label lines based on checkbox settings
// Returns an array of {text, type} objects for multi-line display with colors
function getNodeLabelLines(d) {
    const data = d.data || d;
    const lines = [];

    // Check which options are enabled
    const showFilename = document.getElementById('node-show-filename')?.checked ?? true;
    const showType = document.getElementById('node-show-type')?.checked ?? false;
    const showRuleCount = document.getElementById('node-show-rule-count')?.checked ?? false;
    const showEnrichmentCount = document.getElementById('node-show-enrichment-count')?.checked ?? false;
    const showMetadataId = document.getElementById('node-show-metadata-id')?.checked ?? false;
    const showDescription = document.getElementById('node-show-description')?.checked ?? false;

    // Line 1: Filename (always on its own line for clarity)
    if (showFilename) {
        lines.push({ text: data.name || 'Unknown', type: 'filename' });
    }

    // Line 2: Type (if enabled and has value)
    if (showType) {
        const type = data.contentSummary?.fileType || data.type || '';
        if (type) {
            lines.push({ text: `[${type}]`, type: 'type' });
        }
    }

    // Line 3: Metadata ID (if enabled and has value)
    if (showMetadataId) {
        const metadataId = data.contentSummary?.id || '';
        if (metadataId) {
            lines.push({ text: `ID: ${metadataId}`, type: 'metadataId' });
        }
    }

    // Line 4: Counts (if enabled)
    const counts = [];
    if (showRuleCount) {
        const ruleCount = data.contentSummary?.ruleCount || 0;
        counts.push(`R: ${ruleCount}`);
    }
    if (showEnrichmentCount) {
        const enrichmentCount = data.contentSummary?.enrichmentCount || 0;
        counts.push(`E: ${enrichmentCount}`);
    }
    if (counts.length > 0) {
        lines.push({ text: counts.join(' | '), type: 'counts' });
    }

    // Line 5: Description (if enabled and has value)
    if (showDescription) {
        const description = data.contentSummary?.description || '';
        if (description) {
            // Truncate long descriptions
            const truncated = description.length > 40 ? description.substring(0, 40) + '...' : description;
            lines.push({ text: truncated, type: 'description' });
        }
    }

    // If nothing is selected, show filename as fallback
    if (lines.length === 0) {
        lines.push({ text: data.name || 'Unknown', type: 'filename' });
    }

    return lines;
}

// Render multi-line text using tspan elements with colors
function renderNodeText(textElement, d) {
    const lines = getNodeLabelLines(d);
    const isLeft = d.children || d._children;
    const lineHeight = 14; // pixels between lines

    // Clear existing tspans
    d3.select(textElement).selectAll('tspan').remove();

    // Calculate vertical offset to center the text block
    const totalHeight = (lines.length - 1) * lineHeight;
    const startY = -totalHeight / 2;

    // Add tspan for each line with appropriate color
    lines.forEach((line, i) => {
        const color = nodeAttributeColors[line.type] || '#ffffff';
        d3.select(textElement)
            .append('tspan')
            .attr('x', isLeft ? -13 : 13)
            .attr('dy', i === 0 ? startY : lineHeight)
            .attr('text-anchor', isLeft ? 'end' : 'start')
            .style('fill', color)
            .text(line.text);
    });
}

// Update all node labels and resize background rectangles
function updateNodeLabels() {
    if (!g) return;

    const padding = 4;

    g.selectAll('g.node text')
        .each(function(d) {
            // Render multi-line text
            renderNodeText(this, d);

            // Get the bounding box of the updated text
            const bbox = this.getBBox();

            // Update the background rectangle to match new text size
            d3.select(this.parentNode).select('.label-background')
                .attr('x', bbox.x - padding)
                .attr('y', bbox.y - padding)
                .attr('width', bbox.width + padding * 2)
                .attr('height', bbox.height + padding * 2);
        });
}

// Initialize the tree viewer
function initializeTree() {
    // Show loading message initially
    document.getElementById('loading').style.display = 'block';
    document.getElementById('loading').textContent = 'Loading tree data...';
    
    // Create SVG container
    svg = d3.select("#tree-container")
        .append("svg")
        .attr("width", width)
        .attr("height", height);
    
    // Create main group for zoom/pan
    g = svg.append("g");

    // Set up zoom behavior (store globally for toolbar access)
    zoom = d3.zoom()
        .scaleExtent([0.1, 3])
        .on("zoom", function(event) {
            g.attr("transform", event.transform);
        })
        .on("end", function(event) {
            // Re-enable tooltips after zoom/drag ends
            tooltipEnabled = true;
        });

    svg.call(zoom);

    // Close tooltip when clicking on SVG background
    svg.on("click", function(event) {
        // Only close if clicking on the SVG itself, not on nodes
        if (event.target === event.currentTarget || event.target.tagName === 'svg') {
            hideTooltipSimple();
        }
    });

    // Set initial zoom with left margin to account for root node label
    svg.call(zoom.transform, initialTreeTransform);
    
    // Create tree layout
    tree = d3.tree().size([height - 100, width - 200]);

    // Load recent directories first, then load tree data if any exist
    setTimeout(() => {
        loadRecentDirectories(true);  // Auto-load on initial page load
    }, 100);
}

// Load available sample directories into dropdown
function loadRecentDirectories(autoLoad = false) {
    const select = document.getElementById('directory-select');
    select.innerHTML = ''; // Clear loading message

    // Load recent directories from localStorage only
    const recentDirs = getRecentDirectories();

    if (recentDirs.length > 0) {
        recentDirs.forEach(dir => {
            const option = document.createElement('option');
            option.value = dir.path;
            // Show if it's a file or folder
            const isFile = dir.path.toLowerCase().endsWith('.yaml') || dir.path.toLowerCase().endsWith('.yml');
            option.textContent = (isFile ? '[File] ' : '[Folder] ') + dir.name;
            select.appendChild(option);
        });

        // Auto-load the first recent item only on initial page load
        if (autoLoad) {
            loadTreeData();
        }
    } else {
        select.innerHTML = '<option value="">No recent items - use Browse or enter a path</option>';
    }
}

// Get recent directories from localStorage (max 5)
function getRecentDirectories() {
    try {
        const stored = localStorage.getItem('recentDirectories');
        return stored ? JSON.parse(stored) : [];
    } catch (e) {
        console.error('Failed to load recent directories from localStorage:', e);
        return [];
    }
}

// Save a directory to recent directories (max 5, most recent first)
function saveRecentDirectory(path, name) {
    try {
        let recentDirs = getRecentDirectories();

        // Remove if already exists (to move to top)
        recentDirs = recentDirs.filter(dir => dir.path !== path);

        // Add to beginning
        recentDirs.unshift({ path, name });

        // Keep only last 5
        recentDirs = recentDirs.slice(0, 5);

        // Save to localStorage
        localStorage.setItem('recentDirectories', JSON.stringify(recentDirs));

        // Refresh the dropdown to show the new recent item
        loadRecentDirectories();
    } catch (e) {
        console.error('Failed to save recent directory to localStorage:', e);
    }
}

// Load tree data from REST API
function loadTreeData() {
    // Get the directory path from the dropdown or custom input
    const directorySelect = document.getElementById('directory-select');
    const customInput = document.getElementById('custom-directory-input');
    const basePath = customInput && customInput.value.trim()
        ? customInput.value.trim()
        : (directorySelect ? directorySelect.value : '');

    if (!basePath) {
        showAlert('warning', 'No Path Selected', 'Please select a recent item from the dropdown, use Browse, or enter a custom path.');
        return;
    }

    // Use apex-yaml-manager API - use current origin for tests, fallback to port 8082 for development
    const baseUrl = window.location.origin.includes('localhost') && window.location.pathname.includes('/yaml-manager')
        ? window.location.origin  // Use current origin if already on yaml-manager
        : 'http://localhost:8082';  // Fallback for development

    // Determine if the path is a file or directory
    const isFile = basePath.toLowerCase().endsWith('.yaml') || basePath.toLowerCase().endsWith('.yml');

    if (isFile) {
        // Load single file as dependency tree root
        loadSingleFileTree(basePath, baseUrl);
    } else {
        // Scan folder for all YAML files
        loadFolderFiles(basePath, baseUrl);
    }
}

// Load a single file as the root of a dependency tree
// skipSaveRecent: if true, don't save to recent items (used when called from loadFolderFiles)
function loadSingleFileTree(filePath, baseUrl, skipSaveRecent = false) {
    const displayPath = filePath.substring(0, filePath.lastIndexOf('/') || filePath.lastIndexOf('\\'));
    updateTreePath(displayPath || filePath);

    const apiUrl = `${baseUrl}/yaml-manager/api/dependencies/tree?rootFile=${encodeURIComponent(filePath)}`;

    fetch(apiUrl)
        .then(response => response.json().then(data => ({ ok: response.ok, status: response.status, data: data })))
        .then(result => {
            const data = result.data;
            console.log('Loaded tree data:', data);

            if (!result.ok || data.status === 'error') {
                const errorMsg = data.message || data.error || 'Unknown error occurred';
                if (errorMsg.includes('Root file does not exist') || errorMsg.includes('FILE_NOT_FOUND')) {
                    showAlert('error', 'File Not Found',
                        `The specified YAML file could not be found:\n${filePath}\n\nPlease check the file path and try again.`);
                } else if (errorMsg.includes('not valid YAML') || errorMsg.includes('INVALID_YAML')) {
                    showAlert('error', 'Invalid YAML',
                        `The YAML file is not valid:\n${errorMsg}\n\nPlease check your YAML files for syntax errors.`);
                } else {
                    showAlert('error', 'Failed to Load Tree', errorMsg);
                }
                d3.select("#loading").style("display", "none");
                return;
            }

            if (data.status === 'success' && data.tree) {
                if (!data.tree.name) {
                    showAlert('warning', 'Empty Tree', `No dependencies found for file:\n${filePath}`);
                    d3.select("#loading").style("display", "none");
                    return;
                }

                if (data.warnings && data.warnings.length > 0) {
                    showAlert('warning', 'Tree Loaded with Warnings', data.warnings.join('\n'), 8000);
                }

                processTreeData(data.tree);

                // Save to recent - extract filename (unless skipSaveRecent is true)
                if (!skipSaveRecent) {
                    const pathParts = filePath.replace(/\\/g, '/').split('/');
                    const fileName = pathParts[pathParts.length - 1] || 'File';
                    saveRecentDirectory(filePath, fileName);
                }
            } else {
                showAlert('error', 'Invalid API Response', data.message || 'No tree data returned from API');
                d3.select("#loading").style("display", "none");
            }
        })
        .catch(error => {
            console.error('Error loading tree data:', error);
            showAlert('error', 'Failed to Load Tree', error.message || 'Unknown error occurred');
            d3.select("#loading").style("display", "none");
        });
}

// Load all YAML files from a folder - always displays in List View only
function loadFolderFiles(folderPath, baseUrl) {
    updateTreePath(folderPath);

    const apiUrl = `${baseUrl}/yaml-manager/api/dependencies/scan-folder?folderPath=${encodeURIComponent(folderPath)}`;

    fetch(apiUrl, { method: 'POST' })
        .then(response => response.json().then(data => ({ ok: response.ok, status: response.status, data: data })))
        .then(result => {
            const data = result.data;
            console.log('Scanned folder:', data);

            if (!result.ok || data.status === 'error') {
                const errorMsg = data.message || data.error || 'Unknown error occurred';
                showAlert('error', 'Failed to Scan Folder', errorMsg);
                d3.select("#loading").style("display", "none");
                return;
            }

            if (data.status === 'success' && data.yamlFiles) {
                if (data.yamlFiles.length === 0) {
                    showAlert('warning', 'No YAML Files', `No YAML files found in folder:\n${folderPath}`);
                    d3.select("#loading").style("display", "none");
                    return;
                }

                const folderName = folderPath.replace(/\\/g, '/').split('/').pop() || 'Folder';

                // Folders always load into List View only
                console.log('Loading folder into List View:', folderPath);

                // Clear the tree view
                clearTreeView();

                // Populate list view with folder files
                populateListViewWithFolderFiles(data.yamlFiles);

                // Switch to List View tab
                switchToListViewTab();

                saveRecentDirectory(folderPath, folderName);
                d3.select("#loading").style("display", "none");
                showAlert('success', 'Folder Loaded',
                    `Found ${data.yamlFiles.length} YAML files.`, 3000);
            } else {
                showAlert('error', 'Invalid API Response', data.message || 'No file list returned from API');
                d3.select("#loading").style("display", "none");
            }
        })
        .catch(error => {
            console.error('Error scanning folder:', error);
            showAlert('error', 'Failed to Scan Folder', error.message || 'Unknown error occurred');
            d3.select("#loading").style("display", "none");
        });
}

// Switch to List View tab programmatically
function switchToListViewTab() {
    const treeViewTab = document.getElementById('tree-view-tab');
    const listViewTab = document.getElementById('list-view-tab');
    const treeViewPanel = document.getElementById('tree-view-panel');
    const listViewPanel = document.getElementById('list-view-panel');

    if (treeViewTab && listViewTab && treeViewPanel && listViewPanel) {
        // Update tab states
        listViewTab.classList.add('active');
        treeViewTab.classList.remove('active');

        // Update panel visibility
        listViewPanel.classList.add('active');
        listViewPanel.style.display = 'flex';
        treeViewPanel.classList.remove('active');
        treeViewPanel.style.display = 'none';

        console.log('Programmatically switched to List View');
    }
}

// Clear the tree view
function clearTreeView() {
    // Clear the SVG content
    if (g) {
        g.selectAll('*').remove();
    }
    // Reset tree data
    root = null;
    console.log('Tree view cleared');
}

// Populate list view directly with folder files (without loading from API)
function populateListViewWithFolderFiles(yamlFiles) {
    const listLoading = document.getElementById('list-loading');
    const listError = document.getElementById('list-error');
    const table = document.getElementById('yaml-files-table');

    if (!listLoading || !listError || !table) {
        console.error('List view elements not found in DOM');
        return;
    }

    // Convert folder files to list view format
    listViewData = yamlFiles.map(file => ({
        filename: file.name || 'Unknown',
        path: file.path || '',
        contentSummary: {
            fileType: file.type,
            id: file.id,
            description: file.description,
            ruleCount: file.ruleCount || 0,
            enrichmentCount: file.enrichmentCount || 0
        },
        id: file.id || '',
        name: file.name || '',
        type: file.type || 'unknown',
        author: file.author || '',
        description: file.description || '',
        version: file.version || '',
        businessDomain: file.businessDomain || '',
        owner: file.owner || '',
        rules: file.ruleCount || 0,
        enrichments: file.enrichmentCount || 0,
        circular: false,
        depth: 0,
        height: 0,
        childCount: 0,
        descendantCount: 0
    }));

    // Render the table
    renderListView();

    // Hide loading, show table
    listLoading.style.display = 'none';
    listError.style.display = 'none';
    table.style.display = 'table';

    // Update count
    updateListCount();

    console.log('List view populated with', listViewData.length, 'files from folder');
}

// Process and render tree data
function processTreeData(treeData) {
    try {
        // Validate data
        if (!treeData) {
            throw new Error("No data provided");
        }

        // Validate tree structure (must have name property)
        if (!treeData.name) {
            throw new Error("Invalid tree structure: missing 'name' property");
        }

        // Convert to D3 hierarchy
        root = d3.hierarchy(treeData);
        console.log('D3 hierarchy created. Root:', root.data.name);
        console.log('Root has children:', root.children ? root.children.length : 0);

        // Check for issues in the tree
        const issues = analyzeTreeIssues(root);

        // Show warnings if there are issues
        if (issues.circularDeps.length > 0) {
            const circularMsg = issues.circularDeps.map(dep => `• ${dep}`).join('\n');
            showAlert('warning', 'Circular Dependencies Detected',
                `The following circular dependencies were found:\n${circularMsg}\n\nThese may cause infinite loops during processing.`, 10000);
        } else if (issues.missingFiles.length > 0) {
            const missingMsg = issues.missingFiles.map(file => `• ${file}`).join('\n');
            showAlert('warning', 'Missing Files Detected',
                `The following referenced files could not be found:\n${missingMsg}\n\nPlease check your YAML references.`, 10000);
        } else if (issues.invalidFiles.length > 0) {
            const invalidMsg = issues.invalidFiles.map(file => `• ${file}`).join('\n');
            showAlert('warning', 'Invalid YAML Files Detected',
                `The following files have YAML syntax errors:\n${invalidMsg}\n\nThese files will be skipped during processing.`, 10000);
        }

        // Set initial positions
        root.x0 = height / 2;
        root.y0 = 0;

        // Collapse all children initially except first level
        if (root.children) {
            console.log('Collapsing', root.children.length, 'root children');
            root.children.forEach(collapse);
            console.log('After collapse, root children:', root.children.length);
            console.log('First child has _children:', root.children[0]._children ? root.children[0]._children.length : 0);
        }

        // Render the tree
        update(root);

        // Calculate and update statistics from actual tree data
        calculateAndUpdateStatistics(root);

        // Hide loading message and show success
        d3.select("#loading").style("display", "none");
        d3.select("#error").style("display", "none");

    } catch (error) {
        console.error('Error processing tree data:', error);
        showAlert('error', 'Failed to Process Tree',
            `An error occurred while processing the dependency tree:\n${error.message}`);
        throw error; // Re-throw for testing purposes
    }
}

/**
 * Analyze tree for issues (circular dependencies, missing files, invalid YAML)
 */
function analyzeTreeIssues(root) {
    const issues = {
        circularDeps: [],
        missingFiles: [],
        invalidFiles: []
    };

    function traverse(node) {
        if (!node) return;

        const data = node.data;

        // Check for circular dependency
        if (data.circularReference) {
            issues.circularDeps.push(data.name);
        }

        // Check for missing files (type = 'unknown type' usually indicates missing file)
        if (data.type === 'unknown type' && data.name && data.name.includes('missing')) {
            issues.missingFiles.push(data.name);
        }

        // Check for invalid YAML files
        if (data.type === 'rule configuration' && data.name && data.name.includes('invalid')) {
            issues.invalidFiles.push(data.name);
        }

        // Traverse children
        if (node.children) {
            node.children.forEach(traverse);
        }
        if (node._children) {
            node._children.forEach(traverse);
        }
    }

    traverse(root);
    return issues;
}

// Collapse a node and its children
function collapse(d) {
    if (d.children) {
        d._children = d.children;
        d._children.forEach(collapse);
        d.children = null;
    }
}

// Show error message (legacy - kept for compatibility)
function showError(message) {
    document.getElementById('error').innerHTML = message;
    document.getElementById('error').style.display = 'block';
}

/**
 * Show a nice-looking alert message
 * @param {string} type - 'error', 'warning', 'info', or 'success'
 * @param {string} title - Alert title
 * @param {string} message - Alert message
 * @param {number} autoClose - Auto-close after milliseconds (0 = no auto-close)
 */
function showAlert(type, title, message, autoClose = 0) {
    const alertContainer = document.getElementById('alert-container');
    const alertIcon = document.getElementById('alert-icon');
    const alertTitle = document.getElementById('alert-title');
    const alertMessage = document.getElementById('alert-message');

    if (!alertContainer) return;

    // Set icon based on type
    const icons = {
        'error': '❌',
        'warning': '⚠️',
        'info': 'ℹ️',
        'success': '✅'
    };

    // Remove all alert type classes
    alertContainer.classList.remove('alert-error', 'alert-warning', 'alert-info', 'alert-success');

    // Add the appropriate class
    alertContainer.classList.add(`alert-${type}`);

    // Set content
    alertIcon.textContent = icons[type] || 'ℹ️';
    alertTitle.textContent = title;
    alertMessage.textContent = message;

    // Show the alert
    alertContainer.style.display = 'block';

    // Auto-close if specified
    if (autoClose > 0) {
        setTimeout(() => {
            closeAlert();
        }, autoClose);
    }
}

/**
 * Close the alert
 */
function closeAlert() {
    const alertContainer = document.getElementById('alert-container');
    if (alertContainer) {
        alertContainer.style.display = 'none';
    }
}

// Update tree visualization
function update(source) {
    // Compute the new tree layout
    const treeData = tree(root);
    const nodes = treeData.descendants();
    const links = treeData.descendants().slice(1);
    
    // Normalize for fixed-depth
    nodes.forEach(d => d.y = d.depth * 180);
    
    // Update nodes
    const node = g.selectAll('g.node')
        .data(nodes, d => d.id || (d.id = ++i));
    
    // Enter new nodes
    const nodeEnter = node.enter().append('g')
        .attr('class', 'node')
        .attr('transform', d => `translate(${source.y0},${source.x0})`);

    // Add circles for nodes - clicking circle expands/collapses
    nodeEnter.append('circle')
        .attr('r', 1e-6)
        .style('fill', d => d._children ? '#ff6b6b' : '#69b3a2')
        .on('click', clickCircle);

    // Add background rectangles for labels
    nodeEnter.append('rect')
        .attr('class', 'label-background')
        .attr('rx', 3)
        .attr('ry', 3)
        .style('fill', '#2c3e50')
        .style('stroke', '#000000')
        .style('stroke-width', '1px')
        .style('fill-opacity', 1e-6)
        .style('cursor', 'pointer')
        .on('mousedown', handleMouseDown)
        .on('mouseup', handleMouseUp)
        .on('click', clickText)
        .on('mouseover', function(event, d) {
            showTooltipSimple(event, d);
        })
        .on('mouseout', function(event, d) {
            cancelTooltip();
        });

    // Add labels for nodes - clicking text shows file content
    nodeEnter.append('text')
        .style('fill', 'white')
        .style('fill-opacity', 1e-6)
        .style('cursor', 'pointer')
        .style('pointer-events', 'all')
        .style('font-weight', '600')
        .each(function(d) {
            // Render multi-line text using tspans
            renderNodeText(this, d);
        })
        .on('mousedown', handleMouseDown)
        .on('mouseup', handleMouseUp)
        .on('click', clickText)
        .on('mouseover', function(event, d) {
            showTooltipSimple(event, d);
        })
        .on('mouseout', function(event, d) {
            cancelTooltip();
        });

    // Update existing nodes
    const nodeUpdate = nodeEnter.merge(node);

    nodeUpdate.transition()
        .duration(duration)
        .attr('transform', d => `translate(${d.y},${d.x})`);

    nodeUpdate.select('circle')
        .attr('r', 6)
        .style('fill', d => d._children ? '#ff6b6b' : '#69b3a2')
        .attr('cursor', 'pointer');

    // Update text and calculate background size
    nodeUpdate.select('text')
        .style('fill-opacity', 1)
        .each(function(d) {
            // Re-render multi-line text (in case options changed)
            renderNodeText(this, d);

            // Get the bounding box of the text to size the background
            const bbox = this.getBBox();
            const padding = 4;

            // Update the background rectangle
            d3.select(this.parentNode).select('.label-background')
                .attr('x', bbox.x - padding)
                .attr('y', bbox.y - padding)
                .attr('width', bbox.width + padding * 2)
                .attr('height', bbox.height + padding * 2)
                .style('fill-opacity', 0.95);
        });
    
    // Remove exiting nodes
    const nodeExit = node.exit().transition()
        .duration(duration)
        .attr('transform', d => `translate(${source.y},${source.x})`)
        .remove();
    
    nodeExit.select('circle')
        .attr('r', 1e-6);
    
    nodeExit.select('text')
        .style('fill-opacity', 1e-6);
    
    // Update links
    const link = g.selectAll('path.link')
        .data(links, d => d.id);
    
    // Enter new links
    const linkEnter = link.enter().insert('path', 'g')
        .attr('class', 'link')
        .attr('d', d => {
            const o = {x: source.x0, y: source.y0};
            return diagonal(o, o);
        });
    
    // Update existing links
    const linkUpdate = linkEnter.merge(link);
    
    linkUpdate.transition()
        .duration(duration)
        .attr('d', d => diagonal(d, d.parent));
    
    // Remove exiting links
    link.exit().transition()
        .duration(duration)
        .attr('d', d => {
            const o = {x: source.x, y: source.y};
            return diagonal(o, o);
        })
        .remove();
    
    // Store old positions for transition
    nodes.forEach(d => {
        d.x0 = d.x;
        d.y0 = d.y;
    });
}

// Create diagonal path between nodes
function diagonal(s, d) {
    const path = `M ${s.y} ${s.x}
                 C ${(s.y + d.y) / 2} ${s.x},
                   ${(s.y + d.y) / 2} ${d.x},
                   ${d.y} ${d.x}`;
    return path;
}

// Handle mousedown - disable tooltip
function handleMouseDown(event, d) {
    tooltipEnabled = false;
    cancelTooltip();
}

// Handle mouseup - re-enable tooltip
function handleMouseUp(event, d) {
    tooltipEnabled = true;
}

// Handle circle click (expand/collapse only)
function clickCircle(event, d) {
    console.log('Circle clicked:', d.data.name);
    console.log('Has children:', d.children ? d.children.length : 0);
    console.log('Has _children:', d._children ? d._children.length : 0);

    // Stop event propagation to prevent triggering parent handlers
    event.stopPropagation();

    // Handle expand/collapse
    if (d.children) {
        console.log('Collapsing node:', d.data.name);
        d._children = d.children;
        d.children = null;
    } else if (d._children) {
        console.log('Expanding node:', d.data.name);
        d.children = d._children;
        d._children = null;
    } else {
        console.log('Node has no children to expand:', d.data.name);
    }
    update(d);
}

// Handle text click (show file content only)
function clickText(event, d) {
    console.log('Text clicked:', d.data.name);
    console.log('File path:', d.data.path);

    // Stop event propagation to prevent triggering parent handlers
    event.stopPropagation();

    // Hide tooltip if visible
    hideTooltipSimple();

    // Update selected node
    selectedNode = d;

    // Update all node styles to reflect selection
    updateNodeSelection();

    // Load file content in right panel
    loadFileContent(d.data.path, d.data);
}

// Update node selection styling
function updateNodeSelection() {
    // Remove selection from all nodes
    d3.selectAll('.label-background')
        .style('stroke', '#000000')
        .style('stroke-width', '1px');

    // Add selection to the selected node
    if (selectedNode) {
        d3.selectAll('.node')
            .filter(d => d === selectedNode)
            .select('.label-background')
            .style('stroke', '#4299e1')
            .style('stroke-width', '3px');
    }
}

// Simple tooltip - show with delay on hover
function showTooltipSimple(event, d) {
    // Check if tooltips are enabled
    if (!tooltipEnabled) {
        return;
    }

    // Clear any existing timer
    if (tooltipTimer) {
        clearTimeout(tooltipTimer);
    }

    // Set a delay before showing tooltip
    tooltipTimer = setTimeout(() => {
        const tooltip = document.getElementById('file-tooltip');
        const tooltipTitle = document.getElementById('tooltip-title');
        const tooltipMetadata = document.getElementById('tooltip-metadata');
        const tooltipCode = document.getElementById('tooltip-code');

        // Set title
        tooltipTitle.textContent = d.data.name || 'File Preview';

        // Hide metadata section (redundant with file content)
        tooltipMetadata.style.display = 'none';

        // Set content (no truncation - tooltip has scrollbar)
        const contentSummary = d.data.contentSummary || {};
        let content = contentSummary.rawContent || '# No content available';

        tooltipCode.textContent = content;

        // Calculate width based on longest line
        // Font: 12px Courier New, approx 7.2px per character
        // Content padding: 15px * 2 = 30px
        // Border: 2px * 2 = 4px
        // Scrollbar reserve: 20px
        const lines = content.split('\n');
        const maxLineLength = Math.max(...lines.map(line => line.length));
        const charWidth = 7.2;
        const widthPadding = 30 + 4 + 20;
        let tooltipWidth = Math.min(700, Math.max(300, maxLineLength * charWidth + widthPadding));

        // Apply width first so content can render
        tooltip.style.width = tooltipWidth + 'px';
        tooltip.style.height = 'auto';
        tooltip.style.display = 'flex';

        // Apply syntax highlighting
        Prism.highlightElement(tooltipCode);

        // Now measure the actual rendered content height
        const tooltipContent = document.querySelector('.tooltip-content');
        const tooltipHeader = document.querySelector('.tooltip-header');
        const headerHeight = tooltipHeader ? tooltipHeader.offsetHeight : 45;
        const contentScrollHeight = tooltipContent ? tooltipContent.scrollHeight : 0;
        const borderHeight = 4; // 2px border * 2

        // Calculate total height needed
        const maxHeight = window.innerHeight * 0.8;
        let tooltipHeight = Math.min(maxHeight, Math.max(150,
            headerHeight + contentScrollHeight + borderHeight));

        // Apply final height
        tooltip.style.height = tooltipHeight + 'px';

        // Show tooltip
        tooltip.style.display = 'flex';

        // Position to the right of the cursor with some offset (using clientX/clientY for fixed positioning)
        let left = event.clientX + 20;
        let top = event.clientY - 50;

        // Adjust if tooltip would go off screen
        setTimeout(() => {
            const tooltipRect = tooltip.getBoundingClientRect();
            if (left + tooltipRect.width > window.innerWidth - 20) {
                left = event.clientX - tooltipRect.width - 20;
            }
            if (top + tooltipRect.height > window.innerHeight - 20) {
                top = window.innerHeight - tooltipRect.height - 20;
            }
            if (top < 20) {
                top = 20;
            }

            tooltip.style.left = left + 'px';
            tooltip.style.top = top + 'px';
        }, 10);
    }, TOOLTIP_DELAY_MS);
}

// Cancel tooltip if mouse leaves before delay
function cancelTooltip() {
    if (tooltipTimer) {
        clearTimeout(tooltipTimer);
        tooltipTimer = null;
    }
}

// Simple tooltip - hide immediately
function hideTooltipSimple() {
    // Clear any pending tooltip timer
    if (tooltipTimer) {
        clearTimeout(tooltipTimer);
        tooltipTimer = null;
    }

    const tooltip = document.getElementById('file-tooltip');
    tooltip.style.display = 'none';
}

// Load file content for the right panel
function loadFileContent(filePath, nodeData) {
    console.log('Loading content for:', filePath, nodeData);

    // Build the full file path - relative to apex-yaml-manager directory
    const baseDirectory = `src/test/resources/apex-yaml-samples/graph-100/`;
    const fullPath = baseDirectory + filePath;

    // Use tree node data directly (no additional API calls needed)
    displayNodeData(filePath, fullPath, nodeData);
}

// Display information using tree node data
function displayNodeData(filePath, fullPath, nodeData) {
    // Hide placeholder and show accordion sections
    document.getElementById('placeholder-content').style.display = 'none';
    document.getElementById('metadata-section').style.display = 'block';
    document.getElementById('yaml-section').style.display = 'block';

    // Show actual YAML file content
    const contentSummary = nodeData.contentSummary || {};
    let displayContent = contentSummary.rawContent || `# Error: No content available for ${filePath}`;

    // Add circular dependency warning if applicable
    if (nodeData.circularReference) {
        displayContent += `\n\n# ⚠️ CIRCULAR DEPENDENCY WARNING
# ${nodeData.circularReference}`;
    }

    document.getElementById('yaml-code').textContent = displayContent;

    // Apply syntax highlighting
    Prism.highlightElement(document.getElementById('yaml-code'));

    // Apply APEX keyword tooltips after Prism highlighting - DISABLED per user request
    // setTimeout(() => {
    //     if (typeof applyApexKeywordTooltips === 'function') {
    //         applyApexKeywordTooltips(document.getElementById('yaml-code'));
    //     }
    // }, 50);



    // Load and display metadata using tree node data
    loadFileMetadata(fullPath, {}, nodeData);
}



// Helper function to format date
function formatDate(timestamp) {
    return new Date(timestamp).toLocaleString();
}

// Load and display file metadata
function loadFileMetadata(fullPath, fileData, nodeData) {
    // Metadata is now always visible in accordion, no need to toggle display

    const contentSummary = nodeData.contentSummary || {};

    // Start with ordered priority fields at the top
    const metadata = [
        { label: 'File Path', value: fullPath }
    ];

    // Add APEX ID if available (full-width)
    if (contentSummary.id) {
        metadata.push({ label: 'APEX ID', value: contentSummary.id });
    }

    // Add Display Name if available (full-width)
    if (contentSummary.name) {
        metadata.push({ label: 'Display Name', value: contentSummary.name });
    }

    // Add File Type (regular width)
    if (contentSummary.fileType) {
        metadata.push({ label: 'File Type', value: contentSummary.fileType });
    }

    // Add Version right after File Type (regular width)
    if (contentSummary.version) {
        metadata.push({ label: 'Version', value: contentSummary.version });
    }

    // Add Last Modified on the same line as File Type and Version (regular width)
    // Use lastModified from nodeData (which defaults to created-date if last-modified-date is not available)
    const lastModified = nodeData.lastModified || fileData.lastModified;
    metadata.push({ label: 'Last Modified', value: lastModified || 'Unknown' });

    // Add Description (full-width)
    if (contentSummary.description) {
        metadata.push({ label: 'Description', value: contentSummary.description });
    }

    // Add remaining file system information
    // Get YAML validity and file existence from contentSummary.contentCounts
    const contentCounts = contentSummary.contentCounts || {};
    const yamlValid = contentCounts['yaml-valid'];
    const fileExists = contentCounts['file-exists'];

    metadata.push(
        { label: 'YAML Valid', value: yamlValid !== undefined ? (yamlValid === 1 ? '✓ Valid' : '✗ Invalid') : 'Unknown',
          status: yamlValid !== undefined ? (yamlValid === 1 ? 'valid' : 'invalid') : 'warning' },
        { label: 'Readable', value: fileExists !== undefined ? (fileExists === 1 ? '✓ Yes' : '✗ No') : 'Unknown',
          status: fileExists !== undefined ? (fileExists === 1 ? 'valid' : 'invalid') : 'warning' }
    );

    // Add content counts
    if (contentSummary.ruleCount !== undefined) {
        metadata.push({ label: 'Rules', value: contentSummary.ruleCount.toString() });
    }
    if (contentSummary.ruleGroupCount !== undefined) {
        metadata.push({ label: 'Rule Groups', value: contentSummary.ruleGroupCount.toString() });
    }
    if (contentSummary.enrichmentCount !== undefined) {
        metadata.push({ label: 'Enrichments', value: contentSummary.enrichmentCount.toString() });
    }
    if (contentSummary.configFileCount !== undefined) {
        metadata.push({ label: 'Config Files', value: contentSummary.configFileCount.toString() });
    }
    if (contentSummary.referenceCount !== undefined) {
        metadata.push({ label: 'References', value: contentSummary.referenceCount.toString() });
    }

    // Add tree-specific metadata
    if (nodeData.depth !== undefined) {
        metadata.push({ label: 'Tree Depth', value: nodeData.depth.toString() });
    }
    if (nodeData.height !== undefined) {
        metadata.push({ label: 'Tree Height', value: nodeData.height.toString() });
    }
    if (nodeData.childCount !== undefined) {
        metadata.push({ label: 'Direct Children', value: nodeData.childCount.toString() });
    }
    if (nodeData.descendantCount !== undefined) {
        metadata.push({ label: 'Total Descendants', value: nodeData.descendantCount.toString() });
    }
    if (nodeData.circular !== undefined) {
        metadata.push({
            label: 'Circular Reference',
            value: nodeData.circular ? '⚠️ Yes' : '✓ No',
            status: nodeData.circular ? 'warning' : 'valid'
        });
    }
    if (nodeData.circularReference) {
        metadata.push({
            label: 'Circular Issue',
            value: nodeData.circularReference,
            status: 'invalid'
        });
    }

    // Render metadata immediately with available data
    renderMetadata(metadata);
}

// Render metadata in the grid
function renderMetadata(metadata) {
    const grid = document.getElementById('metadata-grid');
    grid.innerHTML = '';

    // Fields that should occupy full width
    const fullWidthFields = ['File Path', 'Description', 'APEX ID', 'Display Name'];

    metadata.forEach(item => {
        const metadataItem = document.createElement('div');
        metadataItem.className = 'metadata-item';

        // Add full-width class for long text fields
        if (fullWidthFields.includes(item.label)) {
            metadataItem.classList.add('full-width');
        }

        const label = document.createElement('span');
        label.className = 'metadata-label';
        label.textContent = item.label + ':';

        const value = document.createElement('span');
        value.className = 'metadata-value';

        if (item.status) {
            const indicator = document.createElement('span');
            indicator.className = `status-indicator status-${item.status}`;
            value.appendChild(indicator);
        }

        const textNode = document.createTextNode(item.value);
        value.appendChild(textNode);

        metadataItem.appendChild(label);
        metadataItem.appendChild(value);
        grid.appendChild(metadataItem);
    });
}

// APEX Keyword Colorization Function
function applyApexKeywordColorization(codeElement) {
    // Define APEX keywords by category based on actual YAML files
    const apexKeywords = {
        // Metadata keywords - Blue
        'metadata': 'apex-metadata',
        'id': 'apex-metadata',
        'name': 'apex-metadata',
        'version': 'apex-metadata',
        'description': 'apex-metadata',
        'type': 'apex-metadata',
        'author': 'apex-metadata',
        'created-date': 'apex-metadata',
        'created-by': 'apex-metadata',
        'last-modified': 'apex-metadata',
        'tags': 'apex-metadata',
        'categories': 'apex-metadata',

        // Rules keywords - Green
        'rules': 'apex-rules',
        'condition': 'apex-rules',
        'message': 'apex-rules',
        'severity': 'apex-rules',
        'enabled': 'apex-rules',
        'priority': 'apex-rules',
        'business-domain': 'apex-rules',
        'business-owner': 'apex-rules',
        'category': 'apex-rules',
        'effective-date': 'apex-rules',
        'expiration-date': 'apex-rules',
        'custom-properties': 'apex-rules',
        'validation': 'apex-rules',

        // Enrichment keywords - Purple
        'enrichment': 'apex-enrichment',
        'enrichments': 'apex-enrichment',
        'enrichment-refs': 'apex-enrichment',
        'enrichment-groups': 'apex-enrichment',
        'steps': 'apex-enrichment',
        'when': 'apex-enrichment',
        'action': 'apex-enrichment',
        'params': 'apex-enrichment',
        'field': 'apex-enrichment',
        'value': 'apex-enrichment',
        'lookup-config': 'apex-enrichment',
        'calculation-config': 'apex-enrichment',
        'field-mappings': 'apex-enrichment',
        'conditional-mappings': 'apex-enrichment',
        'mapping-rules': 'apex-enrichment',
        'target-field': 'apex-enrichment',
        'source-field': 'apex-enrichment',
        'transformation': 'apex-enrichment',
        'target-type': 'apex-enrichment',
        'execution-settings': 'apex-enrichment',

        // Rule Groups keywords - Orange
        'rule-groups': 'apex-rulegroup',
        'rule-ids': 'apex-rulegroup',
        'rule-references': 'apex-rulegroup',
        'rule-id': 'apex-rulegroup',
        'operator': 'apex-rulegroup',
        'parallel-execution': 'apex-rulegroup',
        'stop-on-first-failure': 'apex-rulegroup',
        'debug-mode': 'apex-rulegroup',
        'rule-group-references': 'apex-rulegroup',
        'sequence': 'apex-rulegroup',
        'override-priority': 'apex-rulegroup',

        // Scenario keywords - Pink
        'scenarios': 'apex-scenario',
        'scenario': 'apex-scenario',
        'scenario-id': 'apex-scenario',
        'config-file': 'apex-scenario',
        'owner': 'apex-scenario',
        'rule-configurations': 'apex-scenario',
        'data-types': 'apex-scenario',
        'processing-stages': 'apex-scenario',
        'stage-name': 'apex-scenario',
        'execution-order': 'apex-scenario',
        'depends-on': 'apex-scenario',
        'failure-policy': 'apex-scenario',
        'stage-metadata': 'apex-scenario',
        'condition': 'apex-scenario'
    };

    // Simple approach: find text nodes and wrap APEX keywords
    const walker = document.createTreeWalker(
        codeElement,
        NodeFilter.SHOW_TEXT,
        null,
        false
    );

    const textNodes = [];
    let node;
    while (node = walker.nextNode()) {
        textNodes.push(node);
    }

    textNodes.forEach(textNode => {
        let text = textNode.textContent;
        let modified = false;

        // Check each line for YAML key patterns
        const lines = text.split('\n');
        const newLines = lines.map(line => {
            // Match YAML key pattern: optional whitespace + keyword + optional whitespace + colon
            const match = line.match(/^(\s*)([a-zA-Z][a-zA-Z0-9_-]*)(\s*:)/);
            if (match) {
                const [, indent, keyword, colon] = match;
                if (apexKeywords[keyword]) {
                    modified = true;
                    return line.replace(keyword, `<span class="token ${apexKeywords[keyword]}">${keyword}</span>`);
                }
            }
            return line;
        });

        if (modified) {
            const newHTML = newLines.join('\n');
            const wrapper = document.createElement('span');
            wrapper.innerHTML = newHTML;
            textNode.parentNode.replaceChild(wrapper, textNode);
        }
    });

    // Highlight SpEL expressions separately
    setTimeout(() => {
        const allText = codeElement.innerHTML;
        const spelHighlighted = allText.replace(/("[^"]*#[^"]*"|'[^']*#[^']*')/g, '<span class="token apex-spel">$1</span>');
        if (spelHighlighted !== allText) {
            codeElement.innerHTML = spelHighlighted;
        }
    }, 10);
}

// Function to resize the SVG based on tree panel size
function resizeTreeSVG() {
    if (svg) {
        const treePanel = document.querySelector('.tree-panel');
        // Force a reflow to get the actual computed width after CSS transitions
        void treePanel.offsetHeight;
        const newWidth = treePanel.offsetWidth;
        const newHeight = window.innerHeight;
        console.log('resizeTreeSVG called - treePanel.offsetWidth:', treePanel.offsetWidth, 'newWidth:', newWidth);
        svg.attr("width", newWidth).attr("height", newHeight);
        tree.size([newHeight - 100, newWidth - 200]);
        if (root) update(root);
    }
}

// Handle window resize
window.addEventListener('resize', resizeTreeSVG);

// Resizable divider functionality
function initializeResizer() {
    const resizer = document.getElementById('resizer');
    const treePanel = document.querySelector('.tree-panel');
    const contentPanel = document.querySelector('.content-panel');
    const container = document.querySelector('.main-container');
    const sidebar = document.querySelector('.sidebar');

    let isResizing = false;
    let startX = 0;
    let startTreeWidth = 0;
    let startContentWidth = 0;

    resizer.addEventListener('mousedown', function(e) {
        isResizing = true;
        startX = e.clientX;
        startTreeWidth = treePanel.offsetWidth;
        startContentWidth = contentPanel.offsetWidth;

        document.body.style.cursor = 'col-resize';
        document.body.style.userSelect = 'none';
        e.preventDefault();
    });

    document.addEventListener('mousemove', function(e) {
        if (!isResizing) return;

        const deltaX = e.clientX - startX;
        const newTreeWidth = startTreeWidth + deltaX;
        const newContentWidth = startContentWidth - deltaX;

        // Apply minimum width constraints
        const minTreeWidth = 200;
        const minContentWidth = 300;

        if (newTreeWidth >= minTreeWidth && newContentWidth >= minContentWidth) {
            treePanel.style.flexGrow = '0';
            treePanel.style.flexShrink = '0';
            treePanel.style.flexBasis = `${newTreeWidth}px`;

            contentPanel.style.flexGrow = '0';
            contentPanel.style.flexShrink = '0';
            contentPanel.style.flexBasis = `${newContentWidth}px`;

            // Update tree dimensions if it exists
            if (svg) {
                const newWidth = treePanel.offsetWidth - 20;
                const newHeight = window.innerHeight;
                svg.attr("width", newWidth).attr("height", newHeight);
                tree.size([newHeight - 100, newWidth - 200]);
                if (root) update(root);
            }
        }
    });

    document.addEventListener('mouseup', function() {
        if (isResizing) {
            isResizing = false;
            document.body.style.cursor = '';
            document.body.style.userSelect = '';
        }
    });
}

// Initialize toolbar buttons
function initializeToolbar() {
    // Zoom controls
    document.getElementById('zoom-in-btn').addEventListener('click', function() {
        svg.transition().call(zoom.scaleBy, 1.3);
    });

    document.getElementById('zoom-out-btn').addEventListener('click', function() {
        svg.transition().call(zoom.scaleBy, 0.7);
    });

    document.getElementById('reset-zoom-btn').addEventListener('click', function() {
        svg.transition().call(zoom.transform, initialTreeTransform);
    });

    // Expand/Collapse all
    document.getElementById('expand-all-btn').addEventListener('click', function() {
        if (root) {
            expandAll(root);
            update(root);
        }
    });

    document.getElementById('collapse-all-btn').addEventListener('click', function() {
        if (root) {
            collapseAll(root);
            update(root);
        }
    });
}

// Expand all nodes
function expandAll(d) {
    if (d._children) {
        d.children = d._children;
        d._children = null;
    }
    if (d.children) {
        d.children.forEach(expandAll);
    }
}

// Collapse all nodes
function collapseAll(d) {
    if (d.children) {
        d._children = d.children;
        d.children = null;
        d._children.forEach(collapseAll);
    }
}

// Initialize sidebar toggle functionality
function initializeSidebar() {
    const sidebar = document.getElementById('sidebar');
    const sidebarCloseBtn = document.getElementById('sidebar-close-btn');
    const sidebarToggleBtn = document.getElementById('sidebar-toggle-btn');
    const mainContainer = document.querySelector('.main-container');

    // Close sidebar
    sidebarCloseBtn.addEventListener('click', function() {
        sidebar.classList.add('collapsed');
        sidebarToggleBtn.classList.add('visible');
        if (mainContainer) {
            mainContainer.style.paddingLeft = '0';
        }

        // Resize the SVG to fill the expanded tree panel - wait for CSS transition (300ms) + buffer
        setTimeout(resizeTreeSVG, 350);
    });

    // Open sidebar
    sidebarToggleBtn.addEventListener('click', function() {
        sidebar.classList.remove('collapsed');
        sidebarToggleBtn.classList.remove('visible');
        if (mainContainer) {
            mainContainer.style.paddingLeft = '';
        }

        // Resize the SVG to fit the reduced tree panel - wait for CSS transition (300ms) + buffer
        setTimeout(resizeTreeSVG, 350);
    });

    // Initialize accordion functionality
    initializeAccordion();

    // Initialize file browser and other sidebar functionality
    initializeFileBrowser();
}

// Initialize file browser functionality
let fileBrowserInitialized = false;
function initializeFileBrowser() {
    if (fileBrowserInitialized) {
        console.log('initializeFileBrowser() already called, skipping');
        return;
    }
    fileBrowserInitialized = true;
    console.log('initializeFileBrowser() called');

    // Load button functionality (for dropdown selection)
    const loadBtn = document.getElementById('load-btn');
    console.log('loadBtn:', loadBtn);
    if (loadBtn) {
        loadBtn.addEventListener('click', function() {
            const directorySelect = document.getElementById('directory-select');
            const selectedPath = directorySelect.value;

            if (!selectedPath) {
                showAlert('warning', 'No Item Selected', 'Please select a recent file or folder from the dropdown, or use Browse to find one.');
                return;
            }

            // Clear custom input
            const customInput = document.getElementById('custom-directory-input');
            if (customInput) customInput.value = '';

            // Clear existing tree
            if (g) {
                g.selectAll("*").remove();
            }

            // Show loading message
            document.getElementById('loading').style.display = 'block';
            document.getElementById('loading').textContent = 'Loading: ' + selectedPath;

            // Reload tree data with selected directory
            loadTreeData();
        });
    }

    // Clear Recent button functionality
    const clearRecentBtn = document.getElementById('clear-recent-btn');
    if (clearRecentBtn) {
        clearRecentBtn.addEventListener('click', function() {
            // Clear localStorage
            localStorage.removeItem('recentDirectories');

            // Reload the dropdown (will now be empty)
            loadRecentDirectories();

            // Clear the tree
            if (g) {
                g.selectAll("*").remove();
            }

            showAlert('success', 'Recent Items Cleared', 'All recent files and folders have been cleared.', 3000);
        });
    }

    // Load custom path button functionality
    const loadCustomBtn = document.getElementById('load-custom-btn');
    if (loadCustomBtn) {
        loadCustomBtn.addEventListener('click', function() {
            const customInput = document.getElementById('custom-directory-input');
            const customPath = customInput.value.trim();

            if (!customPath) {
                showAlert('warning', 'No Path Entered', 'Please enter a custom file path (e.g., path/to/file.yaml) or directory path.');
                return;
            }

            // Clear existing tree
            if (g) {
                g.selectAll("*").remove();
            }

            // Show loading message
            document.getElementById('loading').style.display = 'block';
            document.getElementById('loading').textContent = 'Loading tree data from: ' + customPath;

            // Reload tree data with custom directory
            loadTreeData();
        });
    }

    // Allow Enter key in custom directory input to trigger load
    const customInput = document.getElementById('custom-directory-input');
    if (customInput) {
        customInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                loadCustomBtn.click();
            }
        });
    }

    // File Browser functionality
    let currentBrowserPath = null;
    let currentBrowserParentPath = null;  // Store the parent path
    let selectedBrowserPath = null;

    const fileBrowserModal = document.getElementById('file-browser-modal');
    const browseBtn = document.getElementById('browse-btn');
    const closeBrowserModal = document.getElementById('close-browser-modal');
    const browserCancelBtn = document.getElementById('browser-cancel-btn');
    const browserSelectBtn = document.getElementById('browser-select-btn');
    const browserUpBtn = document.getElementById('browser-up-btn');
    const browserCurrentPath = document.getElementById('browser-current-path');
    const fileBrowserList = document.getElementById('file-browser-list');

    console.log('File browser elements:', {
        fileBrowserModal: fileBrowserModal,
        browseBtn: browseBtn,
        browserUpBtn: browserUpBtn,
        browserCurrentPath: browserCurrentPath,
        fileBrowserList: fileBrowserList
    });

    function openFileBrowser() {
        fileBrowserModal.classList.add('show');
        selectedBrowserPath = null;
        // Start from last used location, or current working directory if none
        const lastPath = localStorage.getItem('fileBrowserLastPath');
        loadFileBrowserDirectory(lastPath);
    }

    function closeFileBrowser() {
        fileBrowserModal.classList.remove('show');
    }

    function loadFileBrowserDirectory(path) {
        fileBrowserList.innerHTML = '<div class="loading">Loading...</div>';

        const url = path
            ? `/yaml-manager/api/dependencies/browse?path=${encodeURIComponent(path)}`
            : '/yaml-manager/api/dependencies/browse';

        console.log('Loading file browser directory from:', url);

        fetch(url)
            .then(response => {
                console.log('Response status:', response.status);
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Received data:', data);
                if (data.status === 'error') {
                    fileBrowserList.innerHTML = '<div class="loading">Error: ' + data.message + '</div>';
                    showAlert('error', 'Browse Error', data.message);
                    return;
                }

                currentBrowserPath = data.currentPath;
                currentBrowserParentPath = data.parentPath;  // Store the parent path
                browserCurrentPath.value = currentBrowserPath;

                // Save the current path to localStorage for next time
                localStorage.setItem('fileBrowserLastPath', currentBrowserPath);

                // Enable/disable Up button
                const shouldDisable = data.parentPath === null;
                if (browserUpBtn) {
                    browserUpBtn.disabled = shouldDisable;
                    console.log('Up button state updated in loadFileBrowserDirectory:', {
                        currentPath: data.currentPath,
                        parentPath: data.parentPath,
                        shouldDisable: shouldDisable,
                        actualDisabled: browserUpBtn.disabled
                    });
                } else {
                    console.error('browserUpBtn is null in loadFileBrowserDirectory, cannot update button state');
                }

                // Render file list
                console.log('Rendering', data.items.length, 'items');
                renderFileBrowserList(data.items);
            })
            .catch(error => {
                console.error('Failed to browse directory:', error);
                fileBrowserList.innerHTML = '<div class="loading">Error: ' + error.message + '</div>';
                showAlert('error', 'Browse Error', 'Failed to browse directory: ' + error.message);
            });
    }

    function renderFileBrowserList(items) {
        if (items.length === 0) {
            fileBrowserList.innerHTML = '<div class="loading">Empty directory</div>';
            return;
        }

        fileBrowserList.innerHTML = '';
        items.forEach(item => {
            const itemDiv = document.createElement('div');
            itemDiv.className = 'file-browser-item';
            itemDiv.dataset.path = item.path;
            itemDiv.dataset.isDirectory = item.isDirectory;

            const icon = document.createElement('span');
            icon.className = 'file-icon';
            if (item.isDirectory) {
                icon.className += ' directory';
                icon.textContent = '📁';
            } else if (item.isYaml) {
                icon.className += ' yaml';
                icon.textContent = '📄';
            } else {
                icon.className += ' file';
                icon.textContent = '📄';
            }

            const name = document.createElement('span');
            name.className = 'file-name';
            name.textContent = item.name;

            itemDiv.appendChild(icon);
            itemDiv.appendChild(name);

            itemDiv.addEventListener('click', function() {
                // Single-click to select any item (file or directory)
                document.querySelectorAll('.file-browser-item').forEach(el => {
                    el.classList.remove('selected');
                });
                itemDiv.classList.add('selected');
                selectedBrowserPath = item.path;
            });

            // Double-click on directory to navigate
            itemDiv.addEventListener('dblclick', function() {
                if (item.isDirectory) {
                    loadFileBrowserDirectory(item.path);
                }
            });

            fileBrowserList.appendChild(itemDiv);
        });
    }

    if (browseBtn) {
        browseBtn.addEventListener('click', openFileBrowser);
    }

    if (closeBrowserModal) {
        closeBrowserModal.addEventListener('click', closeFileBrowser);
    }

    if (browserCancelBtn) {
        browserCancelBtn.addEventListener('click', closeFileBrowser);
    }

    if (browserSelectBtn) {
        browserSelectBtn.addEventListener('click', function() {
            if (selectedBrowserPath) {
                customInput.value = selectedBrowserPath;
                closeFileBrowser();
            } else {
                showAlert('warning', 'No Selection', 'Please select a file or directory.');
            }
        });
    }

    if (browserUpBtn) {
        browserUpBtn.addEventListener('click', function() {
            console.log('Up button clicked, current path:', currentBrowserPath, 'parent path:', currentBrowserParentPath);
            // Navigate to parent directory using the stored parent path
            if (currentBrowserParentPath) {
                console.log('Navigating to parent:', currentBrowserParentPath);
                loadFileBrowserDirectory(currentBrowserParentPath);
            } else {
                console.log('Already at root, cannot navigate up');
            }
        });
    } else {
        console.error('browserUpBtn is null, cannot add click listener');
    }

    // Close modal when clicking outside
    fileBrowserModal.addEventListener('click', function(e) {
        if (e.target === fileBrowserModal) {
            closeFileBrowser();
        }
    });

    // Search functionality
    document.getElementById('search-input').addEventListener('input', function(e) {
        const searchTerm = e.target.value.toLowerCase();
        console.log('Search:', searchTerm);
        // TODO: Implement search functionality
    });

    // File type filter functionality
    const filterCheckboxes = document.querySelectorAll('.file-type-filter');
    filterCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            const fileType = this.value;
            const isChecked = this.checked;
            console.log('Filter changed:', fileType, isChecked);
            // TODO: Implement filtering functionality
        });
    });

    // Tooltip toggle functionality
    const tooltipToggle = document.getElementById('tooltip-toggle');
    tooltipToggle.addEventListener('change', function() {
        tooltipEnabled = this.checked;
        console.log('Tooltips', tooltipEnabled ? 'enabled' : 'disabled');

        // Hide tooltip immediately if disabling
        if (!tooltipEnabled) {
            hideTooltipSimple();
        }
    });

    // Show path toggle functionality
    const showPathToggle = document.getElementById('show-path-toggle');
    showPathToggle.addEventListener('change', function() {
        const treePath = document.getElementById('tree-path');
        treePath.style.display = this.checked ? 'inline' : 'none';
        console.log('Directory path', this.checked ? 'shown' : 'hidden');
    });

    // Node display options - update labels when any checkbox changes
    const nodeDisplayOptions = document.querySelectorAll('.node-display-option');
    nodeDisplayOptions.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            console.log('Node display option changed:', this.id, this.checked);
            updateNodeLabels();
        });
    });
}

// Initialize accordion functionality
function initializeAccordion() {
    const accordionHeaders = document.querySelectorAll('.accordion-header');

    accordionHeaders.forEach(header => {
        header.addEventListener('click', function() {
            const section = this.getAttribute('data-section');
            const content = document.getElementById('accordion-' + section);

            // Toggle collapsed state
            this.classList.toggle('collapsed');
            content.classList.toggle('collapsed');
        });
    });
}

// Initialize content panel functionality
function initializeContentPanel() {
    const contentPanel = document.getElementById('content-panel');
    const contentCloseBtn = document.getElementById('content-close-btn');
    const contentToggleBtn = document.getElementById('content-toggle-btn');
    const resizer = document.getElementById('resizer');
    const treePanel = document.querySelector('.tree-panel');

    // Close content panel
    contentCloseBtn.addEventListener('click', function() {
        console.log('Closing content panel...');
        console.log('Before - contentPanel classes:', contentPanel.className);
        console.log('Before - treePanel classes:', treePanel.className);

        contentPanel.classList.add('collapsed');
        contentToggleBtn.classList.add('visible');
        resizer.classList.add('hidden');
        treePanel.classList.add('expanded');

        console.log('After - contentPanel classes:', contentPanel.className);
        console.log('After - treePanel classes:', treePanel.className);
        console.log('After - contentPanel display:', window.getComputedStyle(contentPanel).display);
        console.log('After - treePanel flex:', window.getComputedStyle(treePanel).flex);

        // Resize the SVG to fill the expanded tree panel
        // Use multiple timeouts to ensure we catch the final state after CSS transitions
        setTimeout(resizeTreeSVG, 50);
        setTimeout(resizeTreeSVG, 200);
        setTimeout(resizeTreeSVG, 400);
    });

    // Open content panel
    contentToggleBtn.addEventListener('click', function() {
        contentPanel.classList.remove('collapsed');
        contentToggleBtn.classList.remove('visible');
        resizer.classList.remove('hidden');
        treePanel.classList.remove('expanded');

        // Resize the SVG to fit the reduced tree panel
        setTimeout(resizeTreeSVG, 100); // Small delay to allow CSS transition to complete
    });

    // Initialize content panel accordion
    const contentAccordionHeaders = document.querySelectorAll('.content-accordion-header');

    contentAccordionHeaders.forEach(header => {
        header.addEventListener('click', function() {
            const section = this.getAttribute('data-section');
            const content = document.getElementById('content-accordion-' + section);

            // Toggle collapsed state
            this.classList.toggle('collapsed');
            content.classList.toggle('collapsed');
        });
    });
}

// Calculate statistics from tree data
function calculateAndUpdateStatistics(rootNode) {
    const stats = {
        total: 0,
        byType: {}
    };

    // Recursively count all nodes
    function countNodes(node) {
        stats.total++;

        // Get file type from contentSummary
        const fileType = node.data.contentSummary?.fileType || 'unknown';
        stats.byType[fileType] = (stats.byType[fileType] || 0) + 1;

        // Count children
        if (node.children) {
            node.children.forEach(countNodes);
        }
        if (node._children) {
            node._children.forEach(countNodes);
        }
    }

    countNodes(rootNode);

    console.log('Calculated statistics:', stats);
    updateStatistics(stats);
}

// Update statistics display
function updateStatistics(stats) {
    document.getElementById('stat-total').textContent = stats.total || 0;

    // Get the stats container
    const statsContainer = document.getElementById('sidebar-stats');

    // Remove all existing type stats (keep only total)
    const existingTypeStats = statsContainer.querySelectorAll('.stat-item:not(:first-child)');
    existingTypeStats.forEach(item => item.remove());

    // Add stats for each file type found
    if (stats.byType) {
        // Sort types alphabetically
        const sortedTypes = Object.keys(stats.byType).sort();

        sortedTypes.forEach(type => {
            const count = stats.byType[type];
            const statItem = document.createElement('div');
            statItem.className = 'stat-item';

            // Capitalize first letter of type
            const displayType = type.charAt(0).toUpperCase() + type.slice(1) + 's:';

            statItem.innerHTML = `
                <span class="stat-label">${displayType}</span>
                <span class="stat-value">${count}</span>
            `;

            statsContainer.appendChild(statItem);
        });
    }
}

// Initialize tooltip close button
function initializeTooltip() {
    const tooltipClose = document.getElementById('tooltip-close');
    const tooltip = document.getElementById('file-tooltip');

    // Close button
    tooltipClose.addEventListener('click', function(event) {
        event.stopPropagation();
        hideTooltipSimple();
    });

    // Click anywhere in tooltip to close it (but don't interfere with tree node clicks)
    tooltip.addEventListener('click', function(event) {
        event.stopPropagation();
        hideTooltipSimple();
    });

    // Tooltip stays visible when mouse enters/leaves - must be manually closed
    tooltip.addEventListener('mouseenter', function() {
        // Tooltip stays visible
    });

    tooltip.addEventListener('mouseleave', function() {
        // Tooltip stays visible - must click to close
    });
}

// Update tree path display
function updateTreePath(path) {
    const treePathElement = document.getElementById('tree-path');
    treePathElement.textContent = path;

    // Show/hide path based on checkbox state
    const showPathToggle = document.getElementById('show-path-toggle');
    if (showPathToggle) {
        treePathElement.style.display = showPathToggle.checked ? 'inline' : 'none';
    }
}

// ========================================
// TAB SWITCHING FUNCTIONALITY
// ========================================

/**
 * Initialize tab switching between Tree View and List View
 */
function initializeTabSwitching() {
    const treeViewTab = document.getElementById('tree-view-tab');
    const listViewTab = document.getElementById('list-view-tab');
    const treeViewPanel = document.getElementById('tree-view-panel');
    const listViewPanel = document.getElementById('list-view-panel');

    // Tree View Tab Click
    treeViewTab.addEventListener('click', function() {
        // Update tab states
        treeViewTab.classList.add('active');
        listViewTab.classList.remove('active');

        // Update panel visibility
        treeViewPanel.classList.add('active');
        treeViewPanel.style.display = 'flex';
        listViewPanel.classList.remove('active');
        listViewPanel.style.display = 'none';

        console.log('Switched to Tree View');
    });

    // List View Tab Click
    listViewTab.addEventListener('click', function() {
        // Update tab states
        listViewTab.classList.add('active');
        treeViewTab.classList.remove('active');

        // Update panel visibility
        listViewPanel.classList.add('active');
        listViewPanel.style.display = 'flex';
        treeViewPanel.classList.remove('active');
        treeViewPanel.style.display = 'none';

        // Load list view data if not already loaded
        loadListViewData();

        console.log('Switched to List View');
    });
}

// ========================================
// LIST VIEW FUNCTIONALITY
// ========================================

// Global list view state
let listViewData = [];
let currentSortColumn = 'filename';
let currentSortDirection = 'asc';

/**
 * Load YAML files data for list view
 */
async function loadListViewData() {
    console.log('loadListViewData called');
    const listLoading = document.getElementById('list-loading');
    const listError = document.getElementById('list-error');
    const table = document.getElementById('yaml-files-table');

    if (!listLoading || !listError || !table) {
        console.error('List view elements not found in DOM');
        return;
    }

    // Show loading state
    listLoading.style.display = 'block';
    listError.style.display = 'none';
    table.style.display = 'none';

    try {
        // Get the directory path from the input
        const directoryInput = document.getElementById('directory-input');
        const basePath = directoryInput ? directoryInput.value : 'src/test/resources/apex-yaml-samples/graph-100';

        console.log('Base path:', basePath);

        if (!basePath) {
            throw new Error('No directory path specified');
        }

        // Construct the root file path (same as tree view)
        const rootFile = `${basePath}/00-scenario-registry.yaml`;
        console.log('Root file:', rootFile);

        // Use the same API endpoint as tree view (use relative URL to work with any port)
        const apiUrl = `/yaml-manager/api/dependencies/tree?rootFile=${encodeURIComponent(rootFile)}`;
        console.log('API URL:', apiUrl);

        const response = await fetch(apiUrl);
        console.log('Response status:', response.status);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('Response error:', errorText);
            throw new Error(`Failed to load data: ${response.statusText}`);
        }

        const data = await response.json();
        console.log('Response data:', data);

        // Handle the response format (same as tree view)
        let treeData = null;
        if (data.status === 'success' && data.tree) {
            treeData = data.tree;
        } else if (data.success && data.data && data.data.tree) {
            treeData = data.data.tree;
        } else {
            console.error('Invalid response format:', data);
            throw new Error('Invalid response format');
        }

        console.log('Tree data:', treeData);

        // Extract all files from the tree structure
        listViewData = extractFilesFromTree(treeData);
        console.log('Extracted files:', listViewData.length);

        // Render the table
        renderListView();

        // Hide loading, show table
        listLoading.style.display = 'none';
        table.style.display = 'table';

        // Update count
        updateListCount();

        console.log('List view loaded successfully');

    } catch (error) {
        console.error('Error loading list view data:', error);
        listLoading.style.display = 'none';
        listError.textContent = `Error: ${error.message}`;
        listError.style.display = 'block';
    }
}

/**
 * Extract all files from tree structure into flat array
 */
function extractFilesFromTree(node, files = []) {
    if (!node) return files;

    // Use contentSummary if available, otherwise fallback to node properties
    const metadata = node.contentSummary || {};

    // Add current node
    files.push({
        filename: node.name || 'Unknown',
        path: node.path || '',
        contentSummary: node.contentSummary || {}, // Preserve contentSummary for detail view
        circularReference: node.circularReference, // Preserve circular reference message
        id: metadata.id || node.id || '',
        name: metadata.name || node.name || '',
        type: metadata.fileType || node.type || 'unknown',
        author: metadata.author || node.author || '',
        description: metadata.description || '',
        version: metadata.version || node.version || '',
        businessDomain: metadata.businessDomain || '',
        owner: metadata.owner || '',
        rules: node.rules || 0,
        enrichments: node.enrichments || 0,
        circular: node.circular || false,
        depth: node.depth,
        height: node.height,
        childCount: node.childCount,
        descendantCount: node.descendantCount,
        lastModified: node.lastModified
    });

    // Recursively process children
    if (node.children && Array.isArray(node.children)) {
        node.children.forEach(child => extractFilesFromTree(child, files));
    }

    return files;
}

/**
 * Render the list view table
 */
function renderListView() {
    const tbody = document.getElementById('yaml-files-tbody');
    tbody.innerHTML = '';

    // Sort data
    const sortedData = sortListData(listViewData, currentSortColumn, currentSortDirection);

    // Apply search filter if any
    const searchInput = document.getElementById('list-search-input');
    const searchTerm = searchInput ? searchInput.value.toLowerCase() : '';
    const filteredData = searchTerm ?
        sortedData.filter(file =>
            file.filename.toLowerCase().includes(searchTerm) ||
            file.id.toLowerCase().includes(searchTerm) ||
            file.name.toLowerCase().includes(searchTerm) ||
            file.type.toLowerCase().includes(searchTerm) ||
            file.description.toLowerCase().includes(searchTerm)
        ) : sortedData;

    // Render rows
    filteredData.forEach(file => {
        const row = createTableRow(file);
        tbody.appendChild(row);
    });

    // Update sort indicators
    updateSortIndicators();
}

/**
 * Create a table row for a file
 */
function createTableRow(file) {
    const row = document.createElement('tr');
    row.dataset.path = file.path;

    // Filename
    const filenameCell = document.createElement('td');
    filenameCell.className = 'filename';
    filenameCell.textContent = file.filename;
    row.appendChild(filenameCell);

    // ID
    const idCell = document.createElement('td');
    idCell.textContent = file.id || '-';
    row.appendChild(idCell);

    // Name
    const nameCell = document.createElement('td');
    nameCell.textContent = file.name || '-';
    row.appendChild(nameCell);

    // Type
    const typeCell = document.createElement('td');
    const typeBadge = document.createElement('span');
    typeBadge.className = `type-badge ${file.type}`;
    typeBadge.textContent = file.type;
    typeCell.appendChild(typeBadge);
    row.appendChild(typeCell);

    // Business Domain
    const domainCell = document.createElement('td');
    domainCell.textContent = file.businessDomain || '-';
    row.appendChild(domainCell);

    // Owner
    const ownerCell = document.createElement('td');
    ownerCell.textContent = file.owner || '-';
    row.appendChild(ownerCell);

    // Author
    const authorCell = document.createElement('td');
    authorCell.textContent = file.author || '-';
    row.appendChild(authorCell);

    // Description
    const descCell = document.createElement('td');
    descCell.className = 'description';
    descCell.textContent = file.description || '-';
    descCell.title = file.description; // Full text on hover
    row.appendChild(descCell);

    // Version
    const versionCell = document.createElement('td');
    versionCell.textContent = file.version || '-';
    row.appendChild(versionCell);

    // Click handler to show file content
    row.addEventListener('click', function() {
        // Remove previous selection
        document.querySelectorAll('.yaml-table tbody tr').forEach(r => r.classList.remove('selected'));
        // Add selection to clicked row
        row.classList.add('selected');
        // Load file content
        loadFileContentFromPath(file.path);
    });

    return row;
}

/**
 * Sort list data by column
 */
function sortListData(data, column, direction) {
    return [...data].sort((a, b) => {
        let aVal = a[column] || '';
        let bVal = b[column] || '';

        // Convert to lowercase for string comparison
        if (typeof aVal === 'string') aVal = aVal.toLowerCase();
        if (typeof bVal === 'string') bVal = bVal.toLowerCase();

        if (aVal < bVal) return direction === 'asc' ? -1 : 1;
        if (aVal > bVal) return direction === 'asc' ? 1 : -1;
        return 0;
    });
}

/**
 * Update sort indicators in table headers
 */
function updateSortIndicators() {
    // Remove all sort classes
    document.querySelectorAll('.yaml-table th').forEach(th => {
        th.classList.remove('sort-asc', 'sort-desc');
    });

    // Add sort class to current column
    const currentHeader = document.querySelector(`.yaml-table th[data-column="${currentSortColumn}"]`);
    if (currentHeader) {
        currentHeader.classList.add(`sort-${currentSortDirection}`);
    }
}

/**
 * Update list count display
 */
function updateListCount() {
    const listCount = document.getElementById('list-count');
    if (listCount) {
        listCount.textContent = `${listViewData.length} files`;
    }
}

/**
 * Initialize list view event handlers
 */
function initializeListView() {
    // Column header click for sorting
    document.querySelectorAll('.yaml-table th.sortable').forEach(th => {
        th.addEventListener('click', function() {
            const column = this.dataset.column;

            // Toggle direction if same column, otherwise default to ascending
            if (currentSortColumn === column) {
                currentSortDirection = currentSortDirection === 'asc' ? 'desc' : 'asc';
            } else {
                currentSortColumn = column;
                currentSortDirection = 'asc';
            }

            renderListView();
        });
    });

    // Search input
    const searchInput = document.getElementById('list-search-input');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            renderListView();
        });
    }

    // Refresh button
    const refreshBtn = document.getElementById('refresh-list-btn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', function() {
            loadListViewData();
        });
    }
}

/**
 * Load file content from path by fetching from API
 */
async function loadFileContentFromPath(filePath) {
    try {
        console.log('Loading file content from path:', filePath);

        // Get base URL
        const baseUrl = window.location.origin + '/yaml-manager';
        const apiUrl = `${baseUrl}/api/dependencies/file-content?filePath=${encodeURIComponent(filePath)}`;

        const response = await fetch(apiUrl);
        const data = await response.json();

        if (!response.ok || data.status === 'error') {
            console.error('Failed to load file content:', data.message);
            showAlert('error', 'Failed to Load File', data.message || 'Unknown error');
            return;
        }

        // Build node data structure expected by displayNodeData
        const nodeData = {
            contentSummary: data.contentSummary,
            circularReference: null
        };

        // Display the file content
        displayNodeData(data.filename, filePath, nodeData);

        console.log('File content loaded successfully:', data.filename);

    } catch (error) {
        console.error('Error loading file content:', error);
        showAlert('error', 'Failed to Load File', error.message || 'Unknown error');
    }
}

// Initialize when page loads
document.addEventListener('DOMContentLoaded', function() {
    initializeTree();
    initializeResizer();
    initializeToolbar();
    initializeSidebar();
    initializeContentPanel();
    initializeTooltip();
    initializeTabSwitching();
    initializeListView();
    initializeFileBrowser();

    // Global mouseup listener to re-enable tooltips
    document.addEventListener('mouseup', function() {
        tooltipEnabled = true;
    });

    // Debug: Log actual header heights
    setTimeout(() => {
        const treeHeader = document.querySelector('.tree-header');
        const contentHeader = document.querySelector('.content-header');
        console.log('Tree header height:', treeHeader.offsetHeight + 'px');
        console.log('Content header height:', contentHeader.offsetHeight + 'px');
    }, 100);
});

