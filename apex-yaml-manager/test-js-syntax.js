// Quick syntax check for dependency-tree-viewer.js
const fs = require('fs');

try {
    const jsContent = fs.readFileSync('src/main/resources/static/js/dependency-tree-viewer.js', 'utf8');
    
    // Try to parse the JavaScript
    new Function(jsContent);
    console.log('JavaScript syntax is valid');
} catch (error) {
    console.error('JavaScript syntax error:', error.message);
    console.error('Line:', error.lineNumber || 'unknown');
}
