# Test Registration Script
Write-Host "Testing Marketplace Registration..." -ForegroundColor Green

# Wait for application to start
Write-Host "Waiting for application to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Test registration
$registrationQuery = @"
{
  "query": "mutation { register(username: \"testuser\", email: \"test@example.com\", password: \"123\") { token email role userId } }"
}
"@

Write-Host "Testing registration..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/graphql" -Method POST -Body $registrationQuery -ContentType "application/json"
    Write-Host "Registration successful!" -ForegroundColor Green
    Write-Host "Response: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Cyan
} catch {
    Write-Host "Registration failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Response: $($_.Exception.Response)" -ForegroundColor Red
}

# Test login
$loginQuery = @"
{
  "query": "mutation { login(emailOrUsername: \"testuser\", password: \"123\") { token email role userId } }"
}
"@

Write-Host "`nTesting login..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/graphql" -Method POST -Body $loginQuery -ContentType "application/json"
    Write-Host "Login successful!" -ForegroundColor Green
    Write-Host "Response: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Cyan
} catch {
    Write-Host "Login failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Response: $($_.Exception.Response)" -ForegroundColor Red
}

Write-Host "`nTest completed!" -ForegroundColor Green 