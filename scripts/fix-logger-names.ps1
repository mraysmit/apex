# Fix Exception Logging - Smart detection of logger variable name
# This script fixes logger.debug("Full exception details:", e) calls by detecting the actual logger variable name

param(
    [string]$Path = "..\apex-core\src\main\java"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$totalFixed = 0

function Get-LoggerVariableName {
    param([string]$content)
    
    # Look for patterns like: private static final Logger LOGGER = or Logger logger =
    if ($content -match 'Logger\s+(\w+)\s*=\s*Logger(Factory)?\.') {
        return $Matches[1]
    }
    # Default to lowercase
    return "logger"
}

function Fix-ExceptionLogging {
    param(
        [string]$FilePath
    )
    
    $content = Get-Content $FilePath -Raw -Encoding UTF8
    $originalContent = $content
    
    # Detect the logger variable name used in this file
    $loggerVar = Get-LoggerVariableName -content $content
    
    # Only fix if there are mismatched logger calls
    $wrongLoggerPattern = 'logger\.debug\("Full exception details:", e\)'
    $rightLoggerPattern = "$loggerVar.debug(`"Full exception details:`", e)"
    
    if ($loggerVar -ne "logger" -and $content -match $wrongLoggerPattern) {
        $content = $content -replace 'logger\.debug\("Full exception details:", e\)', "$loggerVar.debug(`"Full exception details:`", e)"
        Write-Host "Fixed logger variable name in: $FilePath (using $loggerVar)" -ForegroundColor Green
        Set-Content $FilePath $content -NoNewline -Encoding UTF8
        return 1
    }
    
    # Check for uppercase LOGGER when lowercase was used
    $wrongUpperPattern = 'LOGGER\.debug\("Full exception details:", e\)'
    if ($loggerVar -eq "logger" -and $content -match $wrongUpperPattern) {
        $content = $content -replace 'LOGGER\.debug\("Full exception details:", e\)', 'logger.debug("Full exception details:", e)'
        Write-Host "Fixed logger variable name in: $FilePath (using $loggerVar)" -ForegroundColor Green
        Set-Content $FilePath $content -NoNewline -Encoding UTF8
        return 1
    }
    
    return 0
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
Write-Host "Total files fixed: $totalFixed" -ForegroundColor Green
