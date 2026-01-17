# Find all stack traces with their test methods
$content = Get-Content test-output-full.txt
$exceptions = @()

for ($i = 0; $i -lt $content.Length; $i++) {
    if ($content[$i] -match "^[a-z].*Exception:|^[a-z].*Error:") {
        # Found an exception, now look for the test method
        $testMethod = ""
        for ($j = $i; $j -lt [Math]::Min($i + 100, $content.Length); $j++) {
            if ($content[$j] -match "at dev\.mars\.apex\.[^(]+Test[^.]*\.([^(]+)\(") {
                $testMethod = $matches[1]
                break
            }
        }
        
        # Look for intentional marker in the 20 lines before the exception
        $hasMarker = $false
        $markerText = ""
        for ($k = [Math]::Max(0, $i - 20); $k -lt $i; $k++) {
            if ($content[$k] -match "(INTENTIONAL|TEST-EXPECTED-ERROR|expected to fail)") {
                $hasMarker = $true
                $markerText = $matches[0]
                break
            }
        }
        
        $exceptions += [PSCustomObject]@{
            Line = $i + 1
            Exception = $content[$i].Substring(0, [Math]::Min(100, $content[$i].Length))
            TestMethod = $testMethod
            HasMarker = $hasMarker
            Marker = $markerText
        }
    }
}

Write-Host "`n=== SUMMARY ===" -ForegroundColor Cyan
Write-Host "Total exceptions found: $($exceptions.Count)" -ForegroundColor Yellow
Write-Host "With intentional markers: $($exceptions | Where-Object HasMarker -eq $true | Measure-Object | Select-Object -ExpandProperty Count)" -ForegroundColor Green
Write-Host "WITHOUT markers: $($exceptions | Where-Object HasMarker -eq $false | Measure-Object | Select-Object -ExpandProperty Count)" -ForegroundColor Red

Write-Host "`n=== Exceptions WITHOUT 'INTENTIONAL' markers ===" -ForegroundColor Red
$exceptions | Where-Object HasMarker -eq $false | Format-Table Line, TestMethod, Exception -AutoSize

Write-Host "`n=== All Exceptions ===" -ForegroundColor Cyan
$exceptions | Format-Table Line, HasMarker, TestMethod, Exception -AutoSize
