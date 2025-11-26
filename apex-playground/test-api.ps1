$body = @{
    sourceData = '{"name":"John","age":15}'
    yamlRules = @"
metadata:
  name: Test
  version: 1.0.0
rules:
  - id: age-check
    name: Age Check
    condition: "#age >= 18"
"@
    dataFormat = "JSON"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri 'http://localhost:8081/playground/api/process' -Method POST -ContentType 'application/json' -Body $body
$response | ConvertTo-Json -Depth 10

