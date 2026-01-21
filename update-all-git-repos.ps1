# PowerShell script to update all Git repositories one level down
# This script scans subdirectories for .git folders and runs git pull on each

param(
    [string]$RootPath = ".",
    [switch]$Verbose,
    [switch]$DryRun,
    [switch]$SkipErrors
)

# Colors for output
$Colors = @{
    Success = "Green"
    Warning = "Yellow" 
    Error = "Red"
    Info = "Cyan"
    Header = "Magenta"
}

function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    $colorValue = $Colors[$Color]
    if ($colorValue) {
        Write-Host $Message -ForegroundColor $colorValue
    } else {
        Write-Host $Message -ForegroundColor White
    }
}

function Test-GitRepository {
    param([string]$Path)
    return Test-Path (Join-Path $Path ".git")
}

function Get-GitStatus {
    param([string]$RepoPath)
    
    Push-Location $RepoPath
    try {
        $branch = git rev-parse --abbrev-ref HEAD 2>$null
        $status = git status --porcelain 2>$null
        $hasChanges = $status.Count -gt 0
        
        return @{
            Branch = $branch
            HasUncommittedChanges = $hasChanges
            Status = $status
        }
    }
    finally {
        Pop-Location
    }
}

function Update-GitRepository {
    param(
        [string]$RepoPath,
        [string]$RepoName
    )
    
    Write-ColorOutput ("-" * 60) "Info"
    Write-ColorOutput "[REPO] Processing: $RepoName" "Info"
    
    if ($DryRun) {
        Write-ColorOutput "   [DRY RUN] Would run: git pull" "Warning"
        return $true
    }
    
    Push-Location $RepoPath
    try {
        # Get current status
        $gitStatus = Get-GitStatus -RepoPath $RepoPath
        
        if ($Verbose) {
            Write-ColorOutput "   [$RepoName] Current branch: $($gitStatus.Branch)" "Info"
            if ($gitStatus.HasUncommittedChanges) {
                Write-ColorOutput "   [$RepoName] [WARNING] Has uncommitted changes" "Warning"
            }
        }
        
        # Run git pull
        Write-ColorOutput "   [PULL] Running git pull..." "Info"
        $pullOutput = git pull 2>&1
        $exitCode = $LASTEXITCODE
        
        if ($exitCode -eq 0) {
            if ($pullOutput -match "Already up to date") {
                Write-ColorOutput "   [OK] '$RepoName' already up to date" "Success"
            } else {
                Write-ColorOutput "   [OK] '$RepoName' successfully updated" "Success"
                if ($Verbose -and $pullOutput) {
                    $pullOutput | ForEach-Object { Write-ColorOutput "      [$RepoName] $_" "Info" }
                }
            }
            return $true
        } else {
            Write-ColorOutput "   [ERROR] Git pull failed in '$RepoName' (exit code: $exitCode)" "Error"
            if ($pullOutput) {
                $pullOutput | ForEach-Object { Write-ColorOutput "      [$RepoName] $_" "Error" }
            }
            return $false
        }
    }
    catch {
        Write-ColorOutput "   [ERROR] Exception occurred in '$RepoName': $($_.Exception.Message)" "Error"
        return $false
    }
    finally {
        Pop-Location
    }
}

# Main script execution
Write-ColorOutput "Git Repository Updater" "Header"
Write-ColorOutput "=" * 50 "Header"

# Resolve the root path
$RootPath = Resolve-Path $RootPath
Write-ColorOutput "Scanning directory: $RootPath" "Info"

if ($DryRun) {
    Write-ColorOutput "[DRY RUN] No actual git operations will be performed" "Warning"
}

# Find all directories one level down that contain .git
$repositories = Get-ChildItem -Path $RootPath -Directory | Where-Object { 
    Test-GitRepository -Path $_.FullName 
}

if ($repositories.Count -eq 0) {
    Write-ColorOutput "[WARNING] No Git repositories found in subdirectories" "Warning"
    exit 0
}

Write-ColorOutput "Found $($repositories.Count) Git repositories:" "Info"
$repositories | ForEach-Object { Write-ColorOutput "   - $($_.Name)" "Info" }
Write-ColorOutput ""

# Statistics
$stats = @{
    Total = $repositories.Count
    Success = 0
    Failed = 0
    Skipped = 0
}

# Process each repository
foreach ($repo in $repositories) {
    try {
        $success = Update-GitRepository -RepoPath $repo.FullName -RepoName $repo.Name
        
        if ($success) {
            $stats.Success++
        } else {
            $stats.Failed++
            if (-not $SkipErrors) {
                Write-ColorOutput "[ERROR] Stopping due to error in '$($repo.Name)'. Use -SkipErrors to continue on failures." "Error"
                break
            }
        }
    }
    catch {
        Write-ColorOutput "[ERROR] Unexpected error processing $($repo.Name): $($_.Exception.Message)" "Error"
        $stats.Failed++
        if (-not $SkipErrors) {
            break
        }
    }
    
    Write-ColorOutput "" # Empty line between repos
}

# Final summary
Write-ColorOutput "Summary:" "Header"
Write-ColorOutput "   Total repositories: $($stats.Total)" "Info"
Write-ColorOutput "   Successfully updated: $($stats.Success)" "Success"
Write-ColorOutput "   Failed: $($stats.Failed)" "Error"

if ($stats.Failed -gt 0) {
    Write-ColorOutput "[WARNING] Some repositories failed to update. Check the output above for details." "Warning"
    exit 1
} else {
    Write-ColorOutput "[SUCCESS] All repositories processed successfully!" "Success"
    exit 0
}
