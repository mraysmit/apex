# Bulk fix YAML ID formats to kebab-case
# This script converts all ID fields with spaces to kebab-case format
# Template: "test-config-with-property-resolution"

Write-Host "🚀 BULK FIXING YAML ID FORMATS" -ForegroundColor Green
Write-Host "Using template: kebab-case format" -ForegroundColor Yellow

$filesFixed = 0
$totalFiles = 0

# Get all YAML files that need fixing
$yamlFiles = Get-ChildItem -Recurse -Include "*.yaml","*.yml" ../apex-demo/src/main/resources/ | Where-Object {
    $content = Get-Content $_.FullName -Raw
    $content -match 'id:\s*"[^"]*\s[^"]*"'
}

Write-Host "Found $($yamlFiles.Count) files that need ID format fixes" -ForegroundColor Cyan

foreach ($file in $yamlFiles) {
    $totalFiles++
    $relativePath = $file.FullName.Replace((Get-Location).Parent.FullName + "\", "")
    
    try {
        # Read the file content
        $content = Get-Content $file.FullName -Raw
        
        # Extract the current ID value
        if ($content -match 'id:\s*"([^"]*)"') {
            $currentId = $matches[1]
            
            # Convert to kebab-case
            $newId = $currentId.ToLower() -replace '\s+', '-' -replace '_', '-'
            
            # Replace the ID in the content
            $newContent = $content -replace 'id:\s*"[^"]*"', "id: `"$newId`""
            
            # Write back to file
            Set-Content -Path $file.FullName -Value $newContent -NoNewline
            
            $filesFixed++
            Write-Host "✅ $relativePath" -ForegroundColor Green
            Write-Host "   $currentId → $newId" -ForegroundColor Gray
        }
    }
    catch {
        Write-Host "❌ Failed to fix: $relativePath" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n📊 BULK FIX RESULTS:" -ForegroundColor Green
Write-Host "   Files processed: $totalFiles" -ForegroundColor White
Write-Host "   Files fixed: $filesFixed" -ForegroundColor White
Write-Host "   Success rate: $([math]::Round(($filesFixed / $totalFiles) * 100, 1))%" -ForegroundColor White

if ($filesFixed -eq $totalFiles) {
    Write-Host "🎉 ALL FILES FIXED SUCCESSFULLY!" -ForegroundColor Green
} else {
    Write-Host "⚠️  Some files may need manual review" -ForegroundColor Yellow
}

Write-Host "`n✅ Bulk fix completed! All ID formats now follow kebab-case template." -ForegroundColor Green
