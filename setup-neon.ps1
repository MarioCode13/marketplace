# Neon PostgreSQL Setup Script for Windows PowerShell
# Run this script to set up environment variables for Neon database

Write-Host "🚀 Neon PostgreSQL Setup Script" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green

# Check if .env file exists
if (Test-Path ".env") {
    Write-Host "⚠️  .env file already exists. Backing up to .env.backup" -ForegroundColor Yellow
    Copy-Item ".env" ".env.backup"
}

# Prompt for Neon credentials
Write-Host "`n📝 Please enter your Neon database credentials:" -ForegroundColor Cyan

$dbUrl = Read-Host "Enter your Neon connection URL (postgresql://user:pass@host/db?sslmode=require)"
$dbUsername = Read-Host "Enter your Neon username"
$dbPassword = Read-Host "Enter your Neon password" -AsSecureString
$jwtSecret = Read-Host "Enter your JWT secret (or press Enter for default)"

# Convert secure string to plain text
$BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($dbPassword)
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)

# Use default JWT secret if not provided
if ([string]::IsNullOrWhiteSpace($jwtSecret)) {
    $jwtSecret = "dev-jwt-secret-key-for-local-development-only"
    Write-Host "Using default JWT secret for development" -ForegroundColor Yellow
}

# Create .env file content
$envContent = @"
# Neon Database Configuration
DB_URL=$dbUrl
DB_USERNAME=$dbUsername
DB_PASSWORD=$plainPassword

# JWT Configuration
JWT_SECRET=$jwtSecret

# B2 Cloud Storage (Backblaze)
B2_APPLICATION_KEY_ID=00389696a4df7200000000002
B2_APPLICATION_KEY_NAME=marketplace-listings
B2_APPLICATION_KEY=K003ZxtpV8XrsMqdDL8ZThJwHLICw2Y
B2_BUCKET_ID=9859f6d9467a547d9f570210
B2_BUCKET_NAME=Listings1579

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=mariotyler13@gmail.com
MAIL_PASSWORD=oucx xetl rykd ryts

# Application Configuration
PORT=8080
ALLOWED_ORIGINS=http://localhost:3000
"@

# Write to .env file
$envContent | Out-File -FilePath ".env" -Encoding UTF8

Write-Host "`n✅ Environment variables saved to .env file" -ForegroundColor Green

# Set environment variables for current session
$env:DB_URL = $dbUrl
$env:DB_USERNAME = $dbUsername
$env:DB_PASSWORD = $plainPassword
$env:JWT_SECRET = $jwtSecret

Write-Host "✅ Environment variables set for current session" -ForegroundColor Green

# Test connection (optional)
$testConnection = Read-Host "`n🧪 Would you like to test the database connection? (y/n)"
if ($testConnection -eq "y" -or $testConnection -eq "Y") {
    Write-Host "`n🔍 Testing database connection..." -ForegroundColor Cyan
    
    try {
        # Try to run the application briefly to test connection
        Write-Host "Starting application to test connection..." -ForegroundColor Yellow
        Write-Host "Press Ctrl+C after a few seconds to stop the test" -ForegroundColor Yellow
        
        mvn spring-boot:run -Dspring.profiles.active=dev -q
    }
    catch {
        Write-Host "❌ Connection test failed. Check your credentials." -ForegroundColor Red
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n📋 Next steps:" -ForegroundColor Cyan
Write-Host "1. Run: mvn spring-boot:run -Dspring.profiles.active=dev" -ForegroundColor White
Write-Host "2. Check logs for successful Flyway migration" -ForegroundColor White
Write-Host "3. Test GraphQL endpoint: http://localhost:8080/graphiql" -ForegroundColor White
Write-Host "4. Update Render environment variables with the same credentials" -ForegroundColor White

Write-Host "`n🎉 Setup complete! Your Neon database is ready to use." -ForegroundColor Green 