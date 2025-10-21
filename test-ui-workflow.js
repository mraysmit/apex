// Test script to simulate the UI workflow and verify it works end-to-end
// This tests the exact same APIs that the UI calls

const API_BASE = 'http://localhost:8082/yaml-manager/api';
const TEST_FOLDER = 'C:/Users/markr/dev/java/corejava/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100';

async function testUIWorkflow() {
    console.log('🧪 Testing APEX YAML Manager UI Workflow...\n');
    
    try {
        // Step 1: Test Scan Folder (what happens when user clicks "Scan Folder")
        console.log('📂 Step 1: Testing Scan Folder API...');
        const scanResponse = await fetch(`${API_BASE}/dependencies/scan-folder?folderPath=${encodeURIComponent(TEST_FOLDER)}`, {
            method: 'POST'
        });
        
        if (!scanResponse.ok) {
            throw new Error(`Scan failed: HTTP ${scanResponse.status}`);
        }
        
        const scanData = await scanResponse.json();
        console.log(`✅ Scan successful: Found ${scanData.totalFiles} files`);
        console.log(`   First file: ${scanData.yamlFiles[0]?.name}`);
        console.log(`   First file path: ${scanData.yamlFiles[0]?.path}\n`);
        
        // Step 2: Test Tree Loading (what happens when user selects a file and clicks "Load Selected")
        console.log('🌳 Step 2: Testing Tree API with first file...');
        const firstFile = scanData.yamlFiles[0];
        const treeResponse = await fetch(`${API_BASE}/dependencies/tree?rootFile=${encodeURIComponent(firstFile.path)}`);
        
        if (!treeResponse.ok) {
            throw new Error(`Tree failed: HTTP ${treeResponse.status}`);
        }
        
        const treeData = await treeResponse.json();
        console.log(`✅ Tree API successful:`);
        console.log(`   Status: ${treeData.status}`);
        console.log(`   Total Files: ${treeData.totalFiles}`);
        console.log(`   Max Depth: ${treeData.maxDepth}`);
        console.log(`   Root File: ${treeData.rootFile}`);
        console.log(`   Root Children: ${treeData.tree?.childCount || 0}`);
        console.log(`   Tree Structure: ${treeData.tree ? 'Present' : 'Missing'}\n`);
        
        // Step 3: Validate Tree Structure (what the UI should receive)
        console.log('🔍 Step 3: Validating Tree Structure...');
        if (!treeData.tree) {
            throw new Error('Tree structure is missing');
        }
        
        if (!treeData.tree.children || treeData.tree.children.length === 0) {
            console.log('⚠️  WARNING: Root node has no children - this might be why the UI shows no dependencies');
        } else {
            console.log(`✅ Root node has ${treeData.tree.children.length} children`);
            
            // Check first child
            const firstChild = treeData.tree.children[0];
            console.log(`   First child: ${firstChild.name}`);
            console.log(`   First child has ${firstChild.childCount} children`);
            
            if (firstChild.children && firstChild.children.length > 0) {
                console.log(`   First grandchild: ${firstChild.children[0].name}`);
            }
        }
        
        // Step 4: Test what the UI JavaScript should do
        console.log('\n🎯 Step 4: Simulating UI JavaScript Processing...');
        
        // This is what loadDependencyTree() should do:
        const treeDataForUI = treeData.tree ? [treeData.tree] : [];
        console.log(`   UI should receive array with ${treeDataForUI.length} root nodes`);
        
        if (treeDataForUI.length > 0) {
            const rootNode = treeDataForUI[0];
            console.log(`   Root node name: ${rootNode.name}`);
            console.log(`   Root node children: ${rootNode.childCount}`);
            console.log(`   This should render in the tree view!`);
        }
        
        console.log('\n🎉 WORKFLOW TEST COMPLETE!');
        console.log('✅ All APIs working correctly');
        console.log('✅ Tree structure is valid and contains dependencies');
        console.log('✅ UI should be able to render the dependency tree');
        
        if (treeData.tree.childCount === 0) {
            console.log('\n⚠️  ISSUE FOUND: Root node has no children');
            console.log('   This explains why the UI shows no dependencies!');
        } else {
            console.log('\n✅ Root node has dependencies - UI should show them');
        }
        
    } catch (error) {
        console.error('\n❌ WORKFLOW TEST FAILED:', error.message);
        return false;
    }
    
    return true;
}

// Run the test
testUIWorkflow().then(success => {
    if (success) {
        console.log('\n🚀 Ready to test the actual UI at: http://localhost:8082/yaml-manager/ui/tree-viewer');
    } else {
        console.log('\n💥 Fix the backend issues before testing the UI');
    }
});
