# fix-since-tags.ps1
# Normalises all @since tags in production Java files to date format (YYYY-MM-DD)
# Uses git history to determine original file creation date

param(
    [string[]]$Path = @(
        "apex-core/src/main",
        "apex-demo/src/main",
        "apex-rest-api/src/main",
        "apex-playground/src/main",
        "apex-compiler/src/main",
        "apex-data-sync/src/main",
        "apex-yaml-manager/src/main"
    ),
    [switch]$DryRun = $false
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  @since Tag Date Normaliser" -ForegroundColor Cyan
Write-Host "  Dry Run: $DryRun" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$updated = 0
$failed = 0
$skipped = 0

foreach ($dir in $Path) {
    if (-not (Test-Path $dir)) { continue }
    
    Get-ChildItem -Recurse -Filter "*.java" -Path $dir | ForEach-Object {
        $file = $_
        $content = Get-Content $file.FullName -Raw
        
        # Match @since with any value
        if ($content -match '\*\s+@since\s+(.+?)(\r?\n)') {
            $currentValue = $matches[1].Trim()
            
            # Skip if already in YYYY-MM-DD format
            if ($currentValue -match '^\d{4}-\d{2}-\d{2}$') {
                $skipped++
                return
            }
            
            # Get file creation date from git
            $relPath = (Resolve-Path -Relative $file.FullName) -replace '\\','/'
            $relPath = $relPath -replace '^\.\/',''
            
            $gitDate = git log --diff-filter=A --format="%ai" -- $relPath 2>$null | Select-Object -First 1
            if (-not $gitDate) {
                $gitDate = git log --format="%ai" --reverse -- $relPath 2>$null | Select-Object -First 1
            }
            
            if (-not $gitDate) {
                Write-Host "  SKIP: No git history for $relPath" -ForegroundColor Gray
                $skipped++
                return
            }
            
            $dateOnly = ($gitDate -split ' ')[0]
            
            if ($DryRun) {
                Write-Host "  [DRY] $relPath : '@since $currentValue' -> '@since $dateOnly'" -ForegroundColor Green
                $updated++
            } else {
                # Replace the @since line — match the full pattern to avoid partial replacements
                $newContent = $content -replace '(\*\s+@since\s+).+?(\r?\n)', "`${1}$dateOnly`${2}"
                Set-Content -Path $file.FullName -Value $newContent -NoNewline
                Write-Host "  [OK]  $relPath : '@since $currentValue' -> '@since $dateOnly'" -ForegroundColor Green
                $updated++
            }
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
