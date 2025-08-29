$resourcesPath = "src\main\resources"
$yamlFiles = @()

Get-ChildItem -Path $resourcesPath -Recurse -Filter "*.yaml" | ForEach-Object {
    $relativePath = $_.FullName.Replace((Get-Location).Path + "\$resourcesPath\", "")
    $hash = (Get-FileHash $_.FullName -Algorithm MD5).Hash
    $yamlFiles += [PSCustomObject]@{
        Path = $relativePath
        Hash = $hash
        Size = $_.Length
        FullPath = $_.FullName
    }
}

$duplicates = $yamlFiles | Group-Object Hash | Where-Object { $_.Count -gt 1 }

Write-Host "DUPLICATE YAML FILES FOUND:"
Write-Host "=========================="
foreach ($group in $duplicates) {
    Write-Host ""
    Write-Host "Files with identical content (Hash: $($group.Name)):"
    foreach ($file in $group.Group) {
        Write-Host "  - $($file.Path) ($($file.Size) bytes)"
    }
}

if ($duplicates.Count -eq 0) {
    Write-Host "No duplicate YAML files found based on content hash."
}

Write-Host ""
Write-Host "SIMILAR FILENAMES (potential duplicates):"
Write-Host "========================================="
$yamlFiles | ForEach-Object {
    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($_.Path)
    $_ | Add-Member -NotePropertyName BaseName -NotePropertyValue $baseName
}

$similarNames = $yamlFiles | Group-Object BaseName | Where-Object { $_.Count -gt 1 }
foreach ($group in $similarNames) {
    Write-Host ""
    Write-Host "Files with similar names ($($group.Name)):"
    foreach ($file in $group.Group) {
        Write-Host "  - $($file.Path)"
    }
}
