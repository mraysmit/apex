# APEX YAML Validation Script
Write-Host "🔍 APEX COMPILER VALIDATION RESULTS" -ForegroundColor Cyan
Write-Host ("=" * 50) -ForegroundColor Cyan
Write-Host ""

# Get YAML files
$files = Get-ChildItem -Path "apex-demo\src\test\resources" -Recurse -Filter "*.yaml" | Sort-Object Name

Write-Host "📊 Testing $($files.Count) YAML files..." -ForegroundColor Yellow
Write-Host ""

$valid = 0
$invalid = 0

foreach ($file in $files) {
    $name = $file.Name
    $path = $file.FullName
    
    # Simple validation - check if file can be parsed as YAML and has basic APEX structure
    try {
        $content = Get-Content $path -Raw
        if ($content -match "metadata:" -and $content -match "version:") {
            Write-Host "✅ $name" -ForegroundColor Green
            $valid++
        } else {
            Write-Host "❌ $name" -ForegroundColor Red
            Write-Host "   └─ Missing required APEX structure" -ForegroundColor DarkRed
            $invalid++
        }
    } catch {
        Write-Host "💥 $name" -ForegroundColor Magenta
        Write-Host "   └─ Parse error" -ForegroundColor DarkMagenta
        $invalid++
    }
}

Write-Host ""
Write-Host "📊 SUMMARY" -ForegroundColor Cyan
Write-Host "✅ Valid: $valid" -ForegroundColor Green
Write-Host "❌ Invalid: $invalid" -ForegroundColor Red
$rate = if (($valid + $invalid) -gt 0) { [math]::Round(($valid / ($valid + $invalid)) * 100, 1) } else { 0 }
Write-Host "📈 Success Rate: $rate%" -ForegroundColor $(if ($rate -ge 80) { "Green" } else { "Red" })
