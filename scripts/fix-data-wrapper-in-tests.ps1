# Fix all test files that still wrap data in {"data": {...}} pattern
# This removes the data wrapper to pass data directly at root context level

$testDir = "c:\Users\markr\dev\java\corejava\apex-rules-engine\apex-core\src\test"
$files = Get-ChildItem -Path $testDir -Filter "*.java" -Recurse

$fixCount = 0
$patternMatches = 0

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $originalContent = $content
    
    # Pattern 1: Simple wrapper removal for single-line Map.of
    # facts.put("data", Map.of("value", 100)); => facts.put("value", 100);
    $content = $content -replace 'facts\.put\("data",\s*Map\.of\((.*?)\)\);', 'facts.putAll(Map.of($1));'
    
    # Pattern 2: More complex - data variable then facts.put("data", data)
    # We'll need to merge the data map into facts directly
    # This is harder to do with regex, so we'll handle it differently
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -NoNewline
        Write-Host "Fixed: $($file.Name)" -ForegroundColor Green
        $fixCount++
    }
}

Write-Host "`nFixed $fixCount files with Map.of pattern" -ForegroundColor Cyan
Write-Host "⚠️  Manual review needed for files with separate data variable pattern" -ForegroundColor Yellow
