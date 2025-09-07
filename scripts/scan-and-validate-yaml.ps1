# APEX YAML Scanning and Validation Script
# Comprehensive scanning and validation of all YAML files in the project

param(
    [string]$ProjectRoot = ".",
    [switch]$FixCommonIssues = $false,
    [switch]$GenerateReport = $false,
    [string]$ReportPath = "yaml-validation-report.html"
)

Write-Host "🔍 APEX YAML Scanning & Validation" -ForegroundColor Cyan
Write-Host "=" * 50 -ForegroundColor Cyan

# Change to project root
Set-Location $ProjectRoot

# Find all YAML files
$yamlFiles = Get-ChildItem -Recurse -Include "*.yaml","*.yml" | Where-Object { 
    $_.FullName -notlike "*target*" -and 
    $_.FullName -notlike "*node_modules*" -and
    $_.FullName -notlike "*.git*"
}

Write-Host "📊 Found $($yamlFiles.Count) YAML files" -ForegroundColor Green

# Validation using APEX Compiler
function Invoke-ApexValidation {
    param([string]$YamlFile)
    
    try {
        # Build classpath for APEX compiler
        $apexCompilerJar = "apex-compiler\target\classes"
        $apexCoreJar = "apex-core\target\classes"
        $snakeYamlJar = (Get-ChildItem -Recurse -Filter "snakeyaml*.jar" | Select-Object -First 1).FullName
        
        $classpath = "$apexCompilerJar;$apexCoreJar;$snakeYamlJar"
        
        # Run APEX compiler validation
        $result = java -cp $classpath dev.mars.apex.compiler.ApexYamlCompiler $YamlFile 2>&1
        
        return @{
            Success = $LASTEXITCODE -eq 0
            Output = $result
        }
    }
    catch {
        return @{
            Success = $false
            Output = $_.Exception.Message
        }
    }
}

# Quick metadata check function
function Test-YamlMetadata {
    param([string]$FilePath)
    
    $content = Get-Content $FilePath -Raw -ErrorAction SilentlyContinue
    if (-not $content) {
        return @{ Valid = $false; Issues = @("Cannot read file") }
    }
    
    $issues = @()
    
    # Check for metadata section
    if ($content -notmatch "metadata\s*:") {
        $issues += "Missing metadata section"
        return @{ Valid = $false; Issues = $issues }
    }
    
    # Check required fields
    $requiredFields = @("id", "name", "version", "description", "type")
    foreach ($field in $requiredFields) {
        if ($content -notmatch "$field\s*:") {
            $issues += "Missing required field: $field"
        }
    }
    
    return @{
        Valid = $issues.Count -eq 0
        Issues = $issues
    }
}

# Process files
$results = @()
$validCount = 0
$invalidCount = 0

Write-Host "`n🔍 Validating YAML files..." -ForegroundColor Yellow

foreach ($file in $yamlFiles) {
    $relativePath = $file.FullName.Replace((Get-Location).Path + "\", "")
    
    # Quick metadata check
    $metadataCheck = Test-YamlMetadata -FilePath $file.FullName
    
    $result = @{
        File = $relativePath
        Valid = $metadataCheck.Valid
        Issues = $metadataCheck.Issues
        Size = $file.Length
        LastModified = $file.LastWriteTime
    }
    
    $results += $result
    
    if ($metadataCheck.Valid) {
        $validCount++
        Write-Host "✅ $relativePath" -ForegroundColor Green
    } else {
        $invalidCount++
        Write-Host "❌ $relativePath" -ForegroundColor Red
        $metadataCheck.Issues | ForEach-Object { Write-Host "   • $_" -ForegroundColor Red }
    }
}

# Summary
Write-Host "`n" + "=" * 50 -ForegroundColor Cyan
Write-Host "📊 VALIDATION SUMMARY" -ForegroundColor Cyan
Write-Host "=" * 50 -ForegroundColor Cyan
Write-Host "Total files:     $($yamlFiles.Count)" -ForegroundColor White
Write-Host "Valid files:     $validCount" -ForegroundColor Green
Write-Host "Invalid files:   $invalidCount" -ForegroundColor Red
Write-Host "Success rate:    $([math]::Round(($validCount / $yamlFiles.Count) * 100, 1))%" -ForegroundColor Yellow

# Common issues analysis
$commonIssues = @{}
foreach ($result in $results) {
    if (-not $result.Valid) {
        foreach ($issue in $result.Issues) {
            if ($commonIssues.ContainsKey($issue)) {
                $commonIssues[$issue]++
            } else {
                $commonIssues[$issue] = 1
            }
        }
    }
}

Write-Host "`n📋 COMMON ISSUES:" -ForegroundColor Yellow
$commonIssues.GetEnumerator() | Sort-Object Value -Descending | ForEach-Object {
    Write-Host "  • $($_.Key): $($_.Value) files" -ForegroundColor White
}

# Generate HTML report if requested
if ($GenerateReport) {
    Write-Host "`n📄 Generating HTML report..." -ForegroundColor Yellow
    
    $html = @"
<!DOCTYPE html>
<html>
<head>
    <title>APEX YAML Validation Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #2c3e50; color: white; padding: 20px; border-radius: 5px; }
        .summary { background: #ecf0f1; padding: 15px; margin: 20px 0; border-radius: 5px; }
        .valid { color: #27ae60; }
        .invalid { color: #e74c3c; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .issues { font-size: 0.9em; color: #e74c3c; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🔍 APEX YAML Validation Report</h1>
        <p>Generated: $(Get-Date)</p>
    </div>
    
    <div class="summary">
        <h2>📊 Summary</h2>
        <p><strong>Total files:</strong> $($yamlFiles.Count)</p>
        <p><strong>Valid files:</strong> <span class="valid">$validCount</span></p>
        <p><strong>Invalid files:</strong> <span class="invalid">$invalidCount</span></p>
        <p><strong>Success rate:</strong> $([math]::Round(($validCount / $yamlFiles.Count) * 100, 1))%</p>
    </div>
    
    <h2>📋 File Details</h2>
    <table>
        <tr>
            <th>File</th>
            <th>Status</th>
            <th>Issues</th>
            <th>Size</th>
            <th>Last Modified</th>
        </tr>
"@
    
    foreach ($result in $results | Sort-Object File) {
        $status = if ($result.Valid) { '<span class="valid">✅ Valid</span>' } else { '<span class="invalid">❌ Invalid</span>' }
        $issues = if ($result.Issues) { ($result.Issues -join "<br>") } else { "" }
        $size = [math]::Round($result.Size / 1KB, 1)
        
        $html += @"
        <tr>
            <td>$($result.File)</td>
            <td>$status</td>
            <td class="issues">$issues</td>
            <td>${size} KB</td>
            <td>$($result.LastModified.ToString("yyyy-MM-dd HH:mm"))</td>
        </tr>
"@
    }
    
    $html += @"
    </table>
    
    <h2>🔧 Common Issues</h2>
    <ul>
"@
    
    $commonIssues.GetEnumerator() | Sort-Object Value -Descending | ForEach-Object {
        $html += "<li><strong>$($_.Key):</strong> $($_.Value) files</li>"
    }
    
    $html += @"
    </ul>
    
    <h2>💡 Recommendations</h2>
    <ol>
        <li>Add missing 'id' fields to all YAML files</li>
        <li>Add missing 'type' fields specifying document type</li>
        <li>Add missing 'author' fields for compliance</li>
        <li>Integrate APEX compiler validation into CI/CD pipeline</li>
        <li>Create automated fix scripts for common issues</li>
    </ol>
</body>
</html>
"@
    
    $html | Out-File -FilePath $ReportPath -Encoding UTF8
    Write-Host "✅ Report generated: $ReportPath" -ForegroundColor Green
}

# Recommendations
Write-Host "`n💡 RECOMMENDATIONS:" -ForegroundColor Cyan
Write-Host "1. Fix missing 'id' fields - add unique identifiers" -ForegroundColor White
Write-Host "2. Fix missing 'type' fields - specify document types" -ForegroundColor White
Write-Host "3. Fix missing 'author' fields - add author information" -ForegroundColor White
Write-Host "4. Integrate validation into build process" -ForegroundColor White
Write-Host "5. Create automated fix scripts" -ForegroundColor White

Write-Host "`n🚀 NEXT STEPS:" -ForegroundColor Cyan
Write-Host "• Run with -GenerateReport to create HTML report" -ForegroundColor White
Write-Host "• Run with -FixCommonIssues to auto-fix simple issues" -ForegroundColor White
Write-Host "• Add validation to Maven build process" -ForegroundColor White
Write-Host "• Set up pre-commit hooks for YAML validation" -ForegroundColor White

# Exit with appropriate code
exit $(if ($invalidCount -eq 0) { 0 } else { 1 })
