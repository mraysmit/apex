#!/usr/bin/env pwsh
# Script to fix duplicate imports and add missing @ExtendWith import

$testFiles = Get-ChildItem -Path "apex-core\src\test\java" -Filter "*Test.java" -Recurse

$totalFiles = $testFiles.Count
$modifiedFiles = 0

Write-Host "Found $totalFiles test files to fix" -ForegroundColor Cyan

foreach ($file in $testFiles) {
    try {
        $lines = Get-Content $file.FullName
        $newLines = @()
        $importsAdded = @{}
        $inImportSection = $false
        $hasExtendWithImport = $false
        $packageLine = $null
        $originalCount = $lines.Count
        
        foreach ($line in $lines) {
            # Track package line
            if ($line -match "^package ") {
                $packageLine = $line
                $newLines += $line
                continue
            }
            
            # Check for @ExtendWith import
            if ($line -match "^import org\.junit\.jupiter\.api\.extension\.ExtendWith;") {
                $hasExtendWithImport = $true
            }
            
            # Handle import section
            if ($line -match "^import ") {
                $inImportSection = $true
                # Deduplicate imports
                if (-not $importsAdded.ContainsKey($line)) {
                    $importsAdded[$line] = $true
                    $newLines += $line
                }
                continue
            }
            
            # End of import section - add missing ExtendWith import if needed
            if ($inImportSection -and $line -notmatch "^import " -and $line.Trim() -ne "") {
                if (-not $hasExtendWithImport) {
                    # Add ExtendWith import after org.junit imports
                    $insertIndex = $newLines.Count - 1
                    while ($insertIndex -ge 0 -and $newLines[$insertIndex] -notmatch "^import org\.junit\.jupiter\.api\.") {
                        $insertIndex--
                    }
                    if ($insertIndex -ge 0) {
                        $newLines = $newLines[0..$insertIndex] + "import org.junit.jupiter.api.extension.ExtendWith;" + $newLines[($insertIndex+1)..($newLines.Count-1)]
                    }
                    $hasExtendWithImport = $true
                }
                $inImportSection = $false
            }
            
            $newLines += $line
        }
        
        # Write back if changed
        if ($newLines.Count -ne $originalCount -or ($newLines -join "`n") -ne ($lines -join "`n")) {
            Set-Content -Path $file.FullName -Value $newLines
            Write-Host "  [OK] $($file.Name)" -ForegroundColor Green
            $modifiedFiles++
        }
    }
    catch {
        Write-Host "  [ERROR] $($file.Name) - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Modified $modifiedFiles files" -ForegroundColor Cyan
