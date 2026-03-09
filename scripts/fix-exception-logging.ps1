# Fix Exception Logging - Add exception parameter to logger calls
# This script ensures all logger.warn/error calls include the exception for stack trace

param(
    [string]$Path = "..\apex-core\src\main\java"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$totalFixed = 0

# Pattern 1: logger.warn("message: {}", e.getMessage()); -> logger.warn("message: {}", e.getMessage(), e);
# Pattern 2: logger.error("message: {}", e.getMessage()); -> logger.error("message: {}", e.getMessage(), e);
# ONLY if not already followed by logger.debug("Full exception details:", e);

function Fix-ExceptionLogging {
    param(
        [string]$FilePath
    )
    
    $content = Get-Content $FilePath -Raw -Encoding UTF8
    $originalContent = $content
    $fileFixed = 0
    
    # Pattern: logger.(warn|error)("...", e.getMessage()); NOT already having , e);
    # We need to add , e) before the closing );
    
    # Match logger.warn/error calls ending with e.getMessage()) that don't have , e) already
    $patterns = @(
        # Pattern for: logger.warn("...", e.getMessage()); - add , e before );
        @{
            Pattern = '(logger\.(warn|error)\([^;]+e\.getMessage\(\))(\);)'
            Check = ', e);'
        }
    )
    
    # Read line by line to check if next line has debug logging
    $lines = $content -split "`n"
    $newLines = @()
    
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        
        # Check if this line has logger.warn/error with e.getMessage() and ends with );
        if ($line -match 'logger\.(warn|error)\([^;]*e\.getMessage\(\)\);' -and $line -notmatch ', e\);') {
            # Check if next line already has debug logging
            $hasDebugNext = $false
            if ($i + 1 -lt $lines.Count) {
                if ($lines[$i + 1] -match 'logger\.debug\("Full exception details:"') {
                    $hasDebugNext = $true
                }
            }
            
            if (-not $hasDebugNext) {
                # Get indentation
                $indent = ""
                if ($line -match '^(\s*)') {
                    $indent = $Matches[1]
                }
                
                # Add debug line after this one
                $newLines += $line
                $newLines += "${indent}logger.debug(`"Full exception details:`", e);"
                $fileFixed++
                continue
            }
        }
        
        $newLines += $line
    }
    
    if ($fileFixed -gt 0) {
        $newContent = $newLines -join "`n"
        Set-Content $FilePath $newContent -NoNewline -Encoding UTF8
        Write-Host "Fixed $fileFixed occurrences in: $FilePath" -ForegroundColor Green
    }
    
    return $fileFixed
}

# Process all Java files
$javaFiles = Get-ChildItem -Path $Path -Recurse -Filter "*.java"
Write-Host "Processing $($javaFiles.Count) Java files in: $Path" -ForegroundColor Cyan

foreach ($file in $javaFiles) {
    $fixed = Fix-ExceptionLogging -FilePath $file.FullName
    $totalFixed += $fixed
}

Write-Host ""
Write-Host "===== Summary =====" -ForegroundColor Yellow
Write-Host "Total occurrences fixed: $totalFixed" -ForegroundColor Green
