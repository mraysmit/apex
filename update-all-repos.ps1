<#
.NOTES
    File Name      : update-all-repos.ps1
    Author         : Mark A Ray-Smith
    Prerequisite   : PowerShell 5.0 or later, Git
    Copyright      : 2025 Mark A Ray-Smith Cityline Ltd.
    License        : MIT License

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
#>

param(
    [Parameter(Mandatory=$false)]
    [string]$RootDirectory = ".",

    [Parameter(Mandatory=$false)]
    [switch]$Verbose,

    [Parameter(Mandatory=$false)]
    [switch]$WhatIf
)

<#
.SYNOPSIS
    Runs git pull on all Git repositories found in a directory tree.

.DESCRIPTION
    This script recursively searches for Git repositories (directories containing .git folders)
    and executes 'git pull' on each one. It provides options for verbose output and dry-run mode.

.PARAMETER RootDirectory
    The root directory to search for Git repositories. Defaults to current directory.

.PARAMETER Verbose
    Enable verbose output showing detailed information about each operation.

.PARAMETER WhatIf
    Show what repositories would be updated without actually running git pull.

.EXAMPLE
    .\update-all-repos.ps1
    Updates all repositories in the current directory.

.EXAMPLE
    .\update-all-repos.ps1 -RootDirectory "C:\Dev\Projects" -Verbose
    Updates all repositories in C:\Dev\Projects with verbose output.

.EXAMPLE
    .\update-all-repos.ps1 -WhatIf
    Shows which repositories would be updated without making changes.
#>

# Function to check if a directory is a Git repository
function Test-GitRepository {
    param([string]$Path)
    return Test-Path (Join-Path $Path ".git")
}

# Function to update a single repository
function Update-Repository {
    param(
        [string]$RepoPath,
        [bool]$VerboseOutput,
        [bool]$DryRun
    )
    
    $repoName = Split-Path $RepoPath -Leaf
    
    if ($DryRun) {
        Write-Host "Would update: $repoName ($RepoPath)" -ForegroundColor Yellow
        return
    }
    
    Write-Host "Updating repository: $repoName" -ForegroundColor Green
    
    if ($VerboseOutput) {
        Write-Host "  Path: $RepoPath" -ForegroundColor Gray
    }
    
    try {
        Push-Location $RepoPath
        
        # Check if we're in a valid git repository
        $gitStatus = git status --porcelain 2>$null
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "  Skipping $repoName - not a valid Git repository or Git not available"
            return
        }
        
        # Check for uncommitted changes
        if ($gitStatus) {
            Write-Warning "  Skipping $repoName - has uncommitted changes"
            if ($VerboseOutput) {
                Write-Host "  Uncommitted files:" -ForegroundColor Gray
                $gitStatus | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
            }
            return
        }
        
        # Get current branch
        $currentBranch = git branch --show-current 2>$null
        if ($VerboseOutput -and $currentBranch) {
            Write-Host "  Current branch: $currentBranch" -ForegroundColor Gray
        }
        
        # Execute git pull
        Write-Host "  Running git pull..." -ForegroundColor Cyan
        $pullOutput = git pull 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            if ($VerboseOutput) {
                $pullOutput | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
            } else {
                # Show summary for non-verbose mode
                if ($pullOutput -match "Already up to date") {
                    Write-Host "  Already up to date" -ForegroundColor Gray
                } elseif ($pullOutput -match "(\d+) file[s]? changed") {
                    Write-Host "  $($matches[0])" -ForegroundColor Blue
                } else {
                    Write-Host "  Updated successfully" -ForegroundColor Blue
                }
            }
        } else {
            Write-Error "  Git pull failed for $repoName"
            if ($VerboseOutput) {
                $pullOutput | ForEach-Object { Write-Host "    $_" -ForegroundColor Red }
            }
        }
    }
    catch {
        Write-Error "  Error updating $repoName`: $_"
    }
    finally {
        Pop-Location
    }
    
    Write-Host ""
}

# Main script execution
try {
    # Resolve the root directory path
    $rootPath = Resolve-Path $RootDirectory -ErrorAction Stop
    
    Write-Host "Searching for Git repositories in: $rootPath" -ForegroundColor Magenta
    Write-Host ""
    
    # Find all Git repositories
    $gitRepos = Get-ChildItem -Path $rootPath -Directory -Recurse | Where-Object { 
        Test-GitRepository $_.FullName 
    }
    
    if ($gitRepos.Count -eq 0) {
        Write-Warning "No Git repositories found in $rootPath"
        exit 0
    }
    
    Write-Host "Found $($gitRepos.Count) Git repository/repositories:" -ForegroundColor Magenta
    $gitRepos | ForEach-Object { Write-Host "  - $($_.Name) ($($_.FullName))" -ForegroundColor Gray }
    Write-Host ""
    
    if ($WhatIf) {
        Write-Host "DRY RUN MODE - No changes will be made" -ForegroundColor Yellow
        Write-Host ""
    }
    
    # Update each repository
    $successCount = 0
    foreach ($repo in $gitRepos) {
        Update-Repository -RepoPath $repo.FullName -VerboseOutput $Verbose -DryRun $WhatIf
        if (-not $WhatIf) {
            $successCount++
        }
    }
    
    if (-not $WhatIf) {
        Write-Host "Completed updating $successCount repository/repositories." -ForegroundColor Magenta
    }
}
catch {
    Write-Error "Script execution failed: $_"
    exit 1
}
