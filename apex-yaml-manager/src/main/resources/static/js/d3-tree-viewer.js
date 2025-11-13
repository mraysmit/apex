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
    
    // Load data from REST API (with small delay for testing)
    setTimeout(() => {
        loadTreeData();
    }, 100);
}

// Load tree data from REST API
function loadTreeData() {
    // Get the directory path from the File Browser panel input
    const directoryInput = document.getElementById('directory-input');
    const basePath = directoryInput ? directoryInput.value : 'src/test/resources/apex-yaml-samples/graph-100';

    // Look for scenario registry file (common root file pattern) or use first YAML file
    // The API will handle finding the appropriate root file
    const rootFile = `${basePath}/00-scenario-registry.yaml`;

    // Update the tree path display
    updateTreePath(basePath);

    // Use apex-yaml-manager API - use current origin for tests, fallback to port 8082 for development
    const baseUrl = window.location.origin.includes('localhost') && window.location.pathname.includes('/yaml-manager')
        ? window.location.origin  // Use current origin if already on yaml-manager
        : 'http://localhost:8082';  // Fallback for development
    const apiUrl = `${baseUrl}/yaml-manager/api/dependencies/tree?rootFile=${encodeURIComponent(rootFile)}`;

    fetch(apiUrl)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Loaded tree data:', data);
            // Handle both formats: apex-yaml-manager returns status=success, apex-rest-api returns success=true
            if (data.status === 'success' && data.tree) {
                // Current apex-yaml-manager format
                processTreeData(data.tree);
            } else if (data.success && data.data && data.data.tree) {
                // Future apex-rest-api format (if needed)
                processTreeData(data.data.tree);
            } else {
                throw new Error('Invalid API response: ' + (data.message || data.error || 'No tree data'));
            }
        })
        .catch(error => {
            console.error('Error loading tree data:', error);
            showError(`Failed to load tree data: ${error.message}`);
            // Hide loading message on error
            d3.select("#loading").style("display", "none");
        });
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
        showError(`Failed to process tree data: ${error.message}`);
        throw error; // Re-throw for testing purposes
    }
}

// Collapse a node and its children
function collapse(d) {
    if (d.children) {
        d._children = d.children;
        d._children.forEach(collapse);
        d.children = null;
    }
}

// Show error message
function showError(message) {
    document.getElementById('error').innerHTML = message;
    document.getElementById('error').style.display = 'block';
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
        .attr('dy', '.35em')
        .attr('x', d => d.children || d._children ? -13 : 13)
        .attr('text-anchor', d => d.children || d._children ? 'end' : 'start')
        .text(d => d.data.name || 'Unknown')
        .style('fill', 'white')
        .style('fill-opacity', 1e-6)
        .style('cursor', 'pointer')
        .style('pointer-events', 'all')
        .style('font-weight', '600')
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

        // Apply syntax highlighting
        Prism.highlightElement(tooltipCode);

        // Apply APEX keyword tooltips - DISABLED per user request
        // setTimeout(() => {
        //     if (typeof applyApexKeywordTooltips === 'function') {
        //         applyApexKeywordTooltips(tooltipCode);
        //     }
        // }, 10);

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

    // Browse button functionality
    const browseHandler = function() {
        const directory = document.getElementById('directory-input').value;
        const includeSubfolders = document.getElementById('include-subfolders').checked;
        console.log('Browse clicked:', directory, 'Include subfolders:', includeSubfolders);

        // Validate directory input
        if (!directory || directory.trim() === '') {
            alert('Please enter a directory path.');
            return;
        }

        // Clear existing tree
        if (g) {
            g.selectAll("*").remove();
        }

        // Show loading message
        document.getElementById('loading').style.display = 'block';
        document.getElementById('loading').textContent = 'Loading tree data from: ' + directory;

        // Reload tree data with new directory
        loadTreeData();
    };

    document.getElementById('browse-btn').addEventListener('click', browseHandler);

    // Allow Enter key in directory input to trigger browse
    document.getElementById('directory-input').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            browseHandler();
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

    // Show path if checkbox is checked
    const showPathToggle = document.getElementById('show-path-toggle');
    if (showPathToggle && showPathToggle.checked) {
        treePathElement.style.display = 'inline';
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

    // Add current node
    files.push({
        filename: node.name || 'Unknown',
        path: node.path || '',
        id: node.metadata?.id || '',
        name: node.metadata?.name || '',
        type: node.type || 'unknown',
        author: node.metadata?.author || '',
        description: node.metadata?.description || '',
        version: node.metadata?.version || '',
        rules: node.rules || 0,
        enrichments: node.enrichments || 0,
        circular: node.circular || false
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
 * Load file content from path (reuse existing functionality)
 */
async function loadFileContentFromPath(filePath) {
    try {
        // Find the file in the listViewData to get its metadata
        const fileData = listViewData.find(f => f.path === filePath);

        if (fileData) {
            // Use the existing loadFileContent function with the file's metadata
            loadFileContent(fileData.filename, fileData);
        } else {
            console.warn('File not found in list view data:', filePath);
        }

    } catch (error) {
        console.error('Error loading file content:', error);
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

