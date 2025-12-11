# Fix created-date to created in all YAML files
$testPath = "c:\Users\markr\dev\java\corejava\apex-rules-engine\apex-demo\src\test"
$files = Get-ChildItem -Path $testPath -Filter "*.yaml" -Recurse | Where-Object { 
    $content = Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue
    $content -and $content -match 'created-date:'
}

$count = 0
foreach ($file in $files) {
    try {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8
        if ($content) {
            $newContent = $content -replace '(\s+)created-date:', '$1created:'
            [System.IO.File]::WriteAllText($file.FullName, $newContent, [System.Text.Encoding]::UTF8)
            $count++
            Write-Host "Fixed: $($file.FullName)"
        }
    } catch {
        Write-Host "Error processing $($file.FullName): $_" -ForegroundColor Red
    }
}

Write-Host "`nFixed $count YAML files" -ForegroundColor Green
