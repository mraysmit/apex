# Group exceptions by test method
$content = Get-Content test-output-full.txt
$testMethods = @{}

for ($i = 0; $i -lt $content.Length; $i++) {
    if ($content[$i] -match "^[a-z].*Exception:|^[a-z].*Error:") {
        # Found an exception, look for the test method
        $testMethod = ""
        for ($j = $i; $j -lt [Math]::Min($i + 100, $content.Length); $j++) {
            if ($content[$j] -match "at dev\.mars\.apex\.[^(]+Test[^.]*\.([^(]+)\(") {
                $testMethod = $matches[1]
                break
            }
        }
        
        if ($testMethod -ne "" -and -not $testMethods.ContainsKey($testMethod)) {
            # Check for intentional marker
            $hasMarker = $false
            for ($k = [Math]::Max(0, $i - 20); $k -lt $i; $k++) {
                if ($content[$k] -match "INTENTIONAL|TEST-EXPECTED-ERROR|expected to fail") {
                    $hasMarker = $true
                    break
                }
            }
            $testMethods[$testMethod] = $hasMarker
        }
    }
}

Write-Host "`n=== Test Methods WITHOUT 'INTENTIONAL' markers ===" -ForegroundColor Red
$testMethods.GetEnumerator() | Where-Object Value -eq $false | Sort-Object Name | ForEach-Object { Write-Host "  - $($_.Key)" }

Write-Host "`n=== Summary ===" -ForegroundColor Cyan
Write-Host "Total unique test methods with exceptions: $($testMethods.Count)" -ForegroundColor Yellow
Write-Host "With intentional markers: $($testMethods.GetEnumerator() | Where-Object Value -eq $true | Measure-Object | Select-Object -ExpandProperty Count)" -ForegroundColor Green
Write-Host "WITHOUT markers: $($testMethods.GetEnumerator() | Where-Object Value -eq $false | Measure-Object | Select-Object -ExpandProperty Count)" -ForegroundColor Red
