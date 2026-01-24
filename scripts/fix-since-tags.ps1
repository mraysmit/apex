# fix-since-tags.ps1
# Updates @since tags from version numbers to original file creation dates from git

param(
    [string]$Path = ".",
    [switch]$DryRun = $false
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  @since Tag Date Updater" -ForegroundColor Cyan
Write-Host "  Dry Run: $DryRun" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Find all Java files with version-based @since tags
$files = Get-ChildItem -Recurse -Path $Path -Filter "*.java" | 
    Select-String -Pattern "@since\s+(1\.|2\.)" | 
    Select-Object -ExpandProperty Path -Unique

Write-Host "`nFound $($files.Count) files with version-based @since tags`n" -ForegroundColor Yellow

$updated = 0
$failed = 0
$skipped = 0

foreach ($file in $files) {
    $relativePath = $file -replace [regex]::Escape((Get-Location).Path + "\"), ""
    
    # Get the original creation date from git
    $gitDate = git log --follow --format="%ai" --diff-filter=A -- $file 2>$null | Select-Object -Last 1
    
    if (-not $gitDate) {
        # If no git history, try to get the earliest commit date
        $gitDate = git log --follow --format="%ai" --reverse -- $file 2>$null | Select-Object -First 1
    }
    
    if (-not $gitDate) {
        Write-Host "  SKIP: No git history for $relativePath" -ForegroundColor Gray
        $skipped++
        continue
    }
    
    # Extract just the date part (YYYY-MM-DD)
    $dateOnly = ($gitDate -split " ")[0]
    
    # Read the file content
    $content = Get-Content -Path $file -Raw
    
    # Check for various @since version patterns and replace with date
    $patterns = @(
        '@since\s+3\.0',
        '@since\s+2\.3\.0',
        '@since\s+2\.2\.0',
        '@since\s+2\.1\.0\)',   # Handle malformed @since 2.1.0)
        '@since\s+2\.1\.0',
        '@since\s+2\.1\s*\)',   # Handle malformed @since 2.1)
        '@since\s+2\.1\s',      # Handle @since 2.1 (with trailing space)
        '@since\s+2\.1',
        '@since\s+2\.0',
        '@since\s+1\.1\.0',
        '@since\s+1\.0\.0',
        '@since\s+1\.0'
    )
    
    $originalContent = $content
    $patternMatched = $false
    
    foreach ($pattern in $patterns) {
        if ($content -match $pattern) {
            $content = $content -replace $pattern, "@since $dateOnly"
            $patternMatched = $true
            break
        }
    }
    
    if (-not $patternMatched) {
        Write-Host "  SKIP: No matching pattern in $relativePath" -ForegroundColor Gray
        $skipped++
        continue
    }
    
    if ($content -eq $originalContent) {
        Write-Host "  SKIP: No changes needed for $relativePath" -ForegroundColor Gray
        $skipped++
        continue
    }
    
    if ($DryRun) {
        Write-Host "  [DRY-RUN] Would update $relativePath -> @since $dateOnly" -ForegroundColor Green
        $updated++
    } else {
        try {
            Set-Content -Path $file -Value $content -NoNewline
            Write-Host "  UPDATED: $relativePath -> @since $dateOnly" -ForegroundColor Green
            $updated++
        } catch {
            Write-Host "  FAILED: $relativePath - $_" -ForegroundColor Red
            $failed++
        }
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Updated: $updated" -ForegroundColor Green
Write-Host "  Skipped: $skipped" -ForegroundColor Yellow
Write-Host "  Failed:  $failed" -ForegroundColor Red
Write-Host "========================================`n" -ForegroundColor Cyan
