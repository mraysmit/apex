/**
 * APEX Example Modal — Shared Logic
 * Used by both the Playground and the Visual Rule Editor.
 *
 * Usage:
 *   ExampleModal.init({
 *       apiBaseUrl: '/playground/api',       // API prefix
 *       loadBtnId: 'loadSelectedExampleBtn', // ID of the "Load Example" footer button
 *       escapeHtml: escapeHtml,              // function(text) → safe HTML string
 *       formatFileSize: formatFileSize,      // function(bytes) → "1.2 KB"
 *       onLoad: function(detail, options) {  // page-specific load callback
 *           // detail = full API response for the selected example
 *           // options = { loadYaml: bool, loadData: bool }
 *       },
 *       onToast: function(message, type) {}  // notification callback ('success'|'warning'|'error')
 *   });
 *
 *   // Then wire the toolbar button:
 *   document.getElementById('myBtn').addEventListener('click', ExampleModal.open);
 *
 *   // And wire the modal footer "Load Example" button onclick:
 *   ExampleModal.loadSelected()
 */
const ExampleModal = (function () {
    'use strict';

    // --- Config (set via init) ---
    let _config = {
        apiBaseUrl: '/playground/api',
        loadBtnId: 'loadSelectedExampleBtn',
        escapeHtml: function (t) { return t; },
        formatFileSize: function (b) { return (b / 1024).toFixed(1) + ' KB'; },
        onLoad: function () {},
        onToast: function () {}
    };

    // --- State ---
    let _modalInstance = null;
    let _catalog = null;           // cached { category: [items] }
    let _selectedExample = null;   // { category, id }
    let _selectedDetail = null;    // full API response

    // ================================================================
    // PUBLIC: init
    // ================================================================
    function init(config) {
        Object.assign(_config, config);
        _initResizer();
    }

    // ================================================================
    // PUBLIC: open
    // ================================================================
    function open() {
        if (!_modalInstance) {
            _modalInstance = new bootstrap.Modal(document.getElementById('loadExampleModal'));
        }
        _modalInstance.show();
        _selectedExample = null;
        _selectedDetail = null;
        _setLoadBtnDisabled(true);
        document.getElementById('examplePreview').style.display = 'none';
        document.getElementById('exampleResizer').style.display = 'none';
        document.getElementById('exampleSearchInput').value = '';

        if (!_catalog) {
            _fetchCatalog();
        } else {
            _renderCategoryList();
        }
    }

    // ================================================================
    // PUBLIC: loadSelected  (called from modal footer button onclick)
    // ================================================================
    function loadSelected() {
        if (!_selectedDetail) return;

        const loadYaml = document.getElementById('loadExampleYaml').checked;
        const loadData = document.getElementById('loadExampleData').checked;

        if (!loadYaml && !loadData) {
            _config.onToast('Select at least one option to load.', 'warning');
            return;
        }

        _config.onLoad(_selectedDetail, {
            loadYaml: loadYaml,
            loadData: loadData,
            category: _selectedExample.category,
            id: _selectedExample.id
        });

        if (_modalInstance) _modalInstance.hide();
    }

    // ================================================================
    // PUBLIC: getSelectedDetail (for pages that need pre-load checks)
    // ================================================================
    function getSelectedDetail() {
        return _selectedDetail;
    }

    // ================================================================
    // INTERNAL: fetch catalog
    // ================================================================
    async function _fetchCatalog() {
        const spinner = document.getElementById('exampleLoadingSpinner');
        const errorDiv = document.getElementById('exampleLoadError');
        spinner.style.display = 'block';
        errorDiv.style.display = 'none';
        document.getElementById('exampleBrowser').style.display = 'none';

        try {
            const response = await fetch(_config.apiBaseUrl + '/examples');
            if (!response.ok) throw new Error('HTTP ' + response.status);
            const data = await response.json();

            _catalog = {};
            for (const [key, val] of Object.entries(data)) {
                if (Array.isArray(val) && val.length > 0) {
                    _catalog[key] = val;
                }
            }

            spinner.style.display = 'none';
            document.getElementById('exampleBrowser').style.display = 'flex';
            _renderCategoryList();

        } catch (e) {
            spinner.style.display = 'none';
            errorDiv.textContent = 'Failed to load examples: ' + e.message;
            errorDiv.style.display = 'block';
        }
    }

    // ================================================================
    // INTERNAL: render category sidebar
    // ================================================================
    function _renderCategoryList() {
        const container = document.getElementById('exampleCategoryList');
        const categories = Object.keys(_catalog).sort();
        let html = '';
        categories.forEach(function (cat) {
            const count = _catalog[cat].length;
            const displayName = cat.replace(/-/g, ' ').replace(/\b\w/g, function (c) { return c.toUpperCase(); });
            html += '<div class="example-category-item" data-category="' + cat + '" onclick="ExampleModal.selectCategory(\'' + cat + '\')">';
            html += displayName + ' <span class="badge bg-secondary">' + count + '</span></div>';
        });
        container.innerHTML = html;

        if (categories.length > 0) {
            _selectCategory(categories[0]);
        }
    }

    // ================================================================
    // PUBLIC: selectCategory  (called from onclick in rendered HTML)
    // ================================================================
    function _selectCategory(category) {
        document.querySelectorAll('.example-category-item').forEach(function (el) {
            el.classList.toggle('active', el.dataset.category === category);
        });
        var searchTerm = (document.getElementById('exampleSearchInput').value || '').toLowerCase();
        _renderItems(category, searchTerm);
    }

    // ================================================================
    // INTERNAL: render item cards
    // ================================================================
    function _renderItems(category, searchTerm) {
        const container = document.getElementById('exampleItemList');
        const items = _catalog[category] || [];

        const filtered = searchTerm
            ? items.filter(function (item) {
                return item.name.toLowerCase().includes(searchTerm) || (item.id && item.id.toLowerCase().includes(searchTerm));
            })
            : items;

        if (filtered.length === 0) {
            container.innerHTML = '<p class="text-muted fst-italic p-3">No examples match the search</p>';
            return;
        }

        let html = '';
        filtered.forEach(function (item) {
            const isSelected = _selectedExample && _selectedExample.category === category && _selectedExample.id === item.id;
            html += '<div class="example-card' + (isSelected ? ' selected' : '') + '" onclick="ExampleModal.selectItem(\'' + category + '\', \'' + item.id + '\', this)">';
            html += '<div class="example-name">' + _config.escapeHtml(item.name) + '</div>';
            html += '<div class="example-meta">' + _config.escapeHtml(item.id) + '</div>';
            if (item.size) {
                html += '<div class="example-badges">';
                html += '<span class="badge bg-secondary">' + _config.formatFileSize(item.size) + '</span>';
                html += '</div>';
            }
            html += '</div>';
        });
        container.innerHTML = html;
    }

    // ================================================================
    // PUBLIC: filterExamples  (called from search input oninput)
    // ================================================================
    function _filterExamples() {
        const activeCategory = document.querySelector('.example-category-item.active');
        if (activeCategory) {
            const searchTerm = (document.getElementById('exampleSearchInput').value || '').toLowerCase();
            _renderItems(activeCategory.dataset.category, searchTerm);
        }
    }

    // ================================================================
    // PUBLIC: selectItem  (called from onclick in rendered HTML)
    // ================================================================
    async function _selectItem(category, id, cardEl) {
        _selectedExample = { category: category, id: id };
        _selectedDetail = null;

        document.querySelectorAll('.example-card').forEach(function (el) { el.classList.remove('selected'); });
        if (cardEl) cardEl.classList.add('selected');

        // Show preview area with loading state
        const previewDiv = document.getElementById('examplePreview');
        previewDiv.style.display = 'flex';
        previewDiv.style.flexDirection = 'column';
        document.getElementById('exampleResizer').style.display = 'flex';
        document.getElementById('exampleBrowser').style.flex = '1 1 60%';
        previewDiv.style.flex = '0 1 40%';
        document.getElementById('previewContent').textContent = 'Loading...';
        document.getElementById('previewName').textContent = id.replace(/-/g, ' ').replace(/\b\w/g, function (c) { return c.toUpperCase(); });
        document.getElementById('previewCategory').textContent = category;
        document.getElementById('previewHasYaml').style.display = 'none';
        document.getElementById('previewHasData').style.display = 'none';
        _setLoadBtnDisabled(true);

        try {
            const response = await fetch(_config.apiBaseUrl + '/examples/' + category + '/' + id);
            if (!response.ok) throw new Error('HTTP ' + response.status);
            const detail = await response.json();
            _selectedDetail = detail;

            if (detail.yaml) {
                document.getElementById('previewHasYaml').style.display = 'inline';
            }
            if (detail.sampleData) {
                document.getElementById('previewHasData').style.display = 'inline';
            }

            // YAML preview (first 50 lines)
            const yamlLines = detail.yaml ? detail.yaml.split('\n') : [];
            const preview = detail.yaml
                ? yamlLines.slice(0, 50).join('\n') + (yamlLines.length > 50 ? '\n# ...' : '')
                : '(no YAML content)';
            document.getElementById('previewContent').textContent = preview;

            // JSON preview
            const jsonPre = document.getElementById('previewContentJson');
            const tabGroup = document.getElementById('previewTabGroup');
            if (detail.sampleData) {
                try {
                    const jsonObj = typeof detail.sampleData === 'string' ? JSON.parse(detail.sampleData) : detail.sampleData;
                    const jsonStr = JSON.stringify(jsonObj, null, 2);
                    const jsonLines = jsonStr.split('\n');
                    jsonPre.textContent = jsonLines.slice(0, 50).join('\n') + (jsonLines.length > 50 ? '\n// ...' : '');
                } catch (e2) {
                    jsonPre.textContent = typeof detail.sampleData === 'string' ? detail.sampleData : JSON.stringify(detail.sampleData);
                }
                tabGroup.style.display = 'flex';
                _switchPreviewTab('yaml');
            } else {
                jsonPre.textContent = '';
                jsonPre.style.display = 'none';
                tabGroup.style.display = 'none';
                document.getElementById('previewContent').style.display = 'block';
            }

            _setLoadBtnDisabled(false);

        } catch (e) {
            document.getElementById('previewContent').textContent = 'Error loading: ' + e.message;
        }
    }

    // ================================================================
    // PUBLIC: switchPreviewTab  (called from tab button onclick)
    // ================================================================
    function _switchPreviewTab(tab) {
        const yamlPre = document.getElementById('previewContent');
        const jsonPre = document.getElementById('previewContentJson');
        const yamlBtn = document.getElementById('previewTabYaml');
        const jsonBtn = document.getElementById('previewTabJson');
        if (tab === 'json') {
            yamlPre.style.display = 'none';
            jsonPre.style.display = 'block';
            yamlBtn.classList.remove('active');
            jsonBtn.classList.add('active');
        } else {
            yamlPre.style.display = 'block';
            jsonPre.style.display = 'none';
            yamlBtn.classList.add('active');
            jsonBtn.classList.remove('active');
        }
    }

    // ================================================================
    // INTERNAL: resizer drag
    // ================================================================
    function _initResizer() {
        const resizer = document.getElementById('exampleResizer');
        if (!resizer) return; // guard for early init
        const container = document.getElementById('exampleModalBody');
        const topPanel = document.getElementById('exampleBrowser');
        const bottomPanel = document.getElementById('examplePreview');
        let isDragging = false;
        let startY = 0;
        let startTopHeight = 0;

        resizer.addEventListener('mousedown', function (e) {
            e.preventDefault();
            isDragging = true;
            startY = e.clientY;
            startTopHeight = topPanel.getBoundingClientRect().height;
            resizer.classList.add('dragging');
            document.body.style.cursor = 'ns-resize';
            document.body.style.userSelect = 'none';
        });

        document.addEventListener('mousemove', function (e) {
            if (!isDragging) return;
            const containerRect = container.getBoundingClientRect();
            const resizerHeight = resizer.getBoundingClientRect().height;
            const available = containerRect.height - resizerHeight;
            const delta = e.clientY - startY;
            let newTopHeight = startTopHeight + delta;
            newTopHeight = Math.max(120, Math.min(newTopHeight, available - 80));
            const topPct = (newTopHeight / available) * 100;
            const bottomPct = 100 - topPct;
            topPanel.style.flex = '0 0 ' + topPct + '%';
            bottomPanel.style.flex = '0 0 ' + bottomPct + '%';
        });

        document.addEventListener('mouseup', function () {
            if (!isDragging) return;
            isDragging = false;
            resizer.classList.remove('dragging');
            document.body.style.cursor = '';
            document.body.style.userSelect = '';
        });
    }

    // ================================================================
    // INTERNAL: helpers
    // ================================================================
    function _setLoadBtnDisabled(disabled) {
        const btn = document.getElementById(_config.loadBtnId);
        if (btn) btn.disabled = disabled;
    }

    // ================================================================
    // Public API
    // ================================================================
    return {
        init: init,
        open: open,
        loadSelected: loadSelected,
        getSelectedDetail: getSelectedDetail,
        // Called from onclick attributes in rendered HTML:
        selectCategory: _selectCategory,
        selectItem: _selectItem,
        filterExamples: _filterExamples,
        switchPreviewTab: _switchPreviewTab
    };
})();
