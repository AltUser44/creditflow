$ErrorActionPreference = "Stop"
$BaseUser = if ($env:BASE_USER) { $env:BASE_USER } else { "http://localhost:8081" }
$BaseTx = if ($env:BASE_TX) { $env:BASE_TX } else { "http://localhost:8082" }
$BaseCredit = if ($env:BASE_CREDIT) { $env:BASE_CREDIT } else { "http://localhost:8083" }

Write-Host "=== 1) Register ==="
$email = "demo-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())@example.com"
$regBody = @{ email = $email; password = "secret123" } | ConvertTo-Json
$reg = Invoke-RestMethod -Uri "$BaseUser/users/register" -Method Post -Body $regBody -ContentType "application/json"
$reg | ConvertTo-Json -Depth 5
$userId = $reg.id

Write-Host "=== 2) Login ==="
$loginBody = @{ email = $email; password = "secret123" } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "$BaseUser/users/login" -Method Post -Body $loginBody -ContentType "application/json"
$login | ConvertTo-Json -Depth 5
$token = $login.token

Write-Host "=== 3) GET /users/me ==="
$me = Invoke-RestMethod -Uri "$BaseUser/users/me" -Headers @{ Authorization = "Bearer $token" }
$me | ConvertTo-Json -Depth 5

Write-Host "=== 4) Transactions ==="
$tx = @{ userId = $userId; amount = 100; type = "credit" } | ConvertTo-Json
Invoke-RestMethod -Uri "$BaseTx/transactions" -Method Post -Body $tx -ContentType "application/json" | ConvertTo-Json
Invoke-RestMethod -Uri "$BaseTx/transactions" -Method Post -Body $tx -ContentType "application/json" | ConvertTo-Json
Invoke-RestMethod -Uri "$BaseTx/transactions" -Method Post -Body $tx -ContentType "application/json" | ConvertTo-Json
$txDebit = @{ userId = $userId; amount = 1500; type = "debit" } | ConvertTo-Json
Invoke-RestMethod -Uri "$BaseTx/transactions" -Method Post -Body $txDebit -ContentType "application/json" | ConvertTo-Json

Write-Host "=== 5) Sleep for async consumer ==="
Start-Sleep -Seconds 3

Write-Host "=== 6) Credit score ==="
$score = Invoke-RestMethod -Uri "$BaseCredit/credit-score/$userId" -Method Get
$score | ConvertTo-Json -Depth 5

Write-Host "Done. Expected score (approx): 607 (see README for rule breakdown)."
