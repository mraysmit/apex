#!/usr/bin/env pwsh
# Script to fix import paths for test extensions
# The extensions are in dev.mars.apex.core.test.extension, not dev.mars.apex.core.test

$testFiles = Get-ChildItem -Path "apex-core\src\test\java" -Filter "*Test.java" -Recurse

$totalFiles = $testFiles.Count
$modifiedFiles = 0

Write-Host "Found $totalFiles test files to fix" -ForegroundColor Cyan
Write-Host ""

foreach ($file in $testFiles) {
    try {
        $content = Get-Content $file.FullName -Raw
        $originalContent = $content
        
        # Fix ColoredTestOutputExtension import
        $content = $content -replace `
            "import dev\.mars\.apex\.core\.test\.ColoredTestOutputExtension;", `
            "import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;"
        
        # Fix TestClassLoggingExtension import  
        $content = $content -replace `
            "import dev\.mars\.apex\.core\.test\.TestClassLoggingExtension;", `
            "import dev.mars.apex.core.test.extension.TestClassLoggingExtension;"
        
        if ($content -ne $originalContent) {
            Set-Content -Path $file.FullName -Value $content -NoNewline
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
