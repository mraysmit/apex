#!/usr/bin/env pwsh
# Script to add TestClassLoggingExtension to all test classes
# Part of Task 1: ERROR_HANDLING_IMPROVEMENT_TASKS.md

$testFiles = Get-ChildItem -Path "apex-core\src\test\java" -Filter "*Test.java" -Recurse

$totalFiles = $testFiles.Count
$modifiedFiles = 0
$skippedFiles = 0
$errorFiles = 0

Write-Host "Found $totalFiles test files to process" -ForegroundColor Cyan
Write-Host ""

foreach ($file in $testFiles) {
    try {
        $content = Get-Content $file.FullName -Raw
        $originalContent = $content
        
        # Check if already has TestClassLoggingExtension
        if ($content -match "TestClassLoggingExtension") {
            Write-Host "  [SKIP] $($file.Name) - Already has TestClassLoggingExtension" -ForegroundColor Yellow
            $skippedFiles++
            continue
        }
        
        # Check if it's using ColoredTestOutputExtension
        if ($content -match "@ExtendWith\(ColoredTestOutputExtension\.class\)") {
            # Replace single extension with both
            $content = $content -replace `
                "@ExtendWith\(ColoredTestOutputExtension\.class\)", `
                "@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})"
            
            # Add import if not present
            if ($content -notmatch "import dev\.mars\.apex\.core\.test\.TestClassLoggingExtension") {
                # Find the import section and add the new import
                $content = $content -replace `
                    "(import dev\.mars\.apex\.core\.test\.ColoredTestOutputExtension;)", `
                    "`$1`nimport dev.mars.apex.core.test.TestClassLoggingExtension;"
            }
            
            Write-Host "  [OK] $($file.Name) - Added TestClassLoggingExtension" -ForegroundColor Green
            $modifiedFiles++
        }
        elseif ($content -match "class \w+Test") {
            # No @ExtendWith annotation - add both
            # First, add imports
            $hasColoredImport = $content -match "import dev\.mars\.apex\.core\.test\.ColoredTestOutputExtension"
            $hasLoggingImport = $content -match "import dev\.mars\.apex\.core\.test\.TestClassLoggingExtension"
            
            # Find where to add imports (after last org.junit import or package statement)
            if (-not $hasColoredImport -or -not $hasLoggingImport) {
                $importBlock = ""
                if (-not $hasColoredImport) {
                    $importBlock += "import dev.mars.apex.core.test.ColoredTestOutputExtension;`n"
                }
                if (-not $hasLoggingImport) {
                    $importBlock += "import dev.mars.apex.core.test.TestClassLoggingExtension;`n"
                }
                
                # Add imports after org.junit.jupiter.api imports
                if ($content -match "(import org\.junit\.jupiter\.api\.[^;]+;)") {
                    $content = $content -replace `
                        "(import org\.junit\.jupiter\.api\.[^;]+;(?:\s*\n)*)", `
                        "`$1`n$importBlock"
                }
                elseif ($content -match "(import org\.junit\.[^;]+;)") {
                    $content = $content -replace `
                        "(import org\.junit\.[^;]+;(?:\s*\n)*)", `
                        "`$1`n$importBlock"
                }
            }
            
            # Add @ExtendWith annotation before class declaration
            $content = $content -replace `
                "(\*/\s*\n)(class \w+Test)", `
                "`$1@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})`n`$2"
            
            # Alternative pattern if no javadoc
            if ($content -eq $originalContent) {
                $content = $content -replace `
                    "(\n)(class \w+Test)", `
                    "`$1@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})`n`$2"
            }
            
            if ($content -ne $originalContent) {
                Write-Host "  [OK] $($file.Name) - Added @ExtendWith annotation" -ForegroundColor Green
                $modifiedFiles++
            }
            else {
                Write-Host "  [WARN] $($file.Name) - Could not find insertion point" -ForegroundColor Magenta
                $skippedFiles++
                continue
            }
        }
        else {
            Write-Host "  [SKIP] $($file.Name) - No test class found" -ForegroundColor Yellow
            $skippedFiles++
            continue
        }
        
        # Write back if changed
        if ($content -ne $originalContent) {
            Set-Content -Path $file.FullName -Value $content -NoNewline
        }
    }
    catch {
        Write-Host "  [ERROR] $($file.Name) - $($_.Exception.Message)" -ForegroundColor Red
        $errorFiles++
    }
}

Write-Host ""
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  Total files: $totalFiles"
Write-Host "  Modified: $modifiedFiles" -ForegroundColor Green
Write-Host "  Skipped: $skippedFiles" -ForegroundColor Yellow
Write-Host "  Errors: $errorFiles" -ForegroundColor Red
