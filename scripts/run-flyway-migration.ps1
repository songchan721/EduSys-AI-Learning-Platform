#!/usr/bin/env pwsh

Write-Host "🚀 Running Flyway Database Migration..." -ForegroundColor Cyan

# Check if Docker containers are running
Write-Host "`n📊 Checking PostgreSQL connection..." -ForegroundColor Yellow
$dbStatus = docker exec learning-platform-postgres pg_isready -U postgres 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ PostgreSQL is not ready. Starting containers..." -ForegroundColor Red
    docker-compose up -d postgres
    Start-Sleep -Seconds 10
}

# Test database connection
Write-Host "`n🔍 Testing database connection..." -ForegroundColor Yellow
$testConnection = docker exec learning-platform-postgres psql -U postgres -d learning_platform -c "SELECT 1;" 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Database connection successful" -ForegroundColor Green
} else {
    Write-Host "❌ Database connection failed" -ForegroundColor Red
    exit 1
}

# Run Flyway migration using Gradle
Write-Host "`n🔄 Running Flyway migration..." -ForegroundColor Yellow
./gradlew :shared:database:flywayMigrate

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ Migration completed successfully!" -ForegroundColor Green
    
    # Show migration history
    Write-Host "`n📋 Migration history:" -ForegroundColor Cyan
    docker exec learning-platform-postgres psql -U postgres -d learning_platform -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
    
    # Show created tables
    Write-Host "`n📋 Database tables:" -ForegroundColor Cyan
    docker exec learning-platform-postgres psql -U postgres -d learning_platform -c "\dt"
} else {
    Write-Host "`n❌ Migration failed!" -ForegroundColor Red
    Write-Host "Check the error message above for details." -ForegroundColor Yellow
}

Write-Host "`n🎯 Migration process completed!" -ForegroundColor Cyan