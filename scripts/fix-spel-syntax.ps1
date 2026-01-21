# Fix SpEL syntax errors in YAML files
# 1. Fix malformed #'field'] to #['field']
# 2. Fix missing # prefix: ['field'] to #['field']

$testDir = "c:\Users\markr\dev\java\corejava\apex-rules-engine\apex-core\src\test"
$files = Get-ChildItem -Path $testDir -Filter "*.yaml" -Recurse

$fixCount = 0

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $originalContent = $content
    
    # Fix 1: Replace #'field'] with #['field'] (add opening bracket)
    $content = $content -replace "#'([a-zA-Z_][a-zA-Z0-9_]*)']", "#[`'`$1`']"
    
    # Fix 2: Replace ['field'] with #['field'] in conditions (add # prefix)
    # Only in condition lines to avoid false positives
    $lines = $content -split "`n"
    $newLines = @()
    
    foreach ($line in $lines) {
        if ($line -match '^\s*condition:\s*".*\[''[a-zA-Z_]') {
            # This is a condition line with ['field'] syntax
            # Add # before each ['field'] that doesn't already have it
            $newLine = $line -replace '([^#])\[(''[a-zA-Z_][a-zA-Z0-9_]*'')\]', '$1#[$2]'
            # Fix start of string case
            $newLine = $newLine -replace '^(\s*condition:\s*")\[(''[a-zA-Z_][a-zA-Z0-9_]*'')\]', '$1#[$2]'
            $newLines += $newLine
        } else {
            $newLines += $line
        }
    }
    
    $content = $newLines -join "`n"
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -NoNewline
        Write-Host "Fixed: $($file.FullName)" -ForegroundColor Green
        $fixCount++
    }
}

Write-Host "`nFixed $fixCount files" -ForegroundColor Cyan
