# PowerShell script to run database migrations
# This script executes database migrations for all microservices

Write-Host "🚀 Running database migrations..." -ForegroundColor Green

# Function to execute SQL file
function Execute-SqlFile {
    param(
        [string]$Database,
        [string]$SqlFile
    )
    
    Write-Host "📊 Executing migration for $Database..." -ForegroundColor Yellow
    
    try {
        # Check if SQL file exists
        if (-not (Test-Path $SqlFile)) {
            Write-Host "❌ SQL file not found: $SqlFile" -ForegroundColor Red
            return $false
        }
        
        # Read SQL file content and execute
        $result = Get-Content $SqlFile -Raw | docker exec -i learning-platform-postgres psql -U postgres -d $Database 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Migration completed for $Database" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ Migration failed for $Database" -ForegroundColor Red
            Write-Host "Error: $result" -ForegroundColor Red
            return $false
        }
    }
    catch {
        Write-Host "❌ Exception during migration for $Database`: $_" -ForegroundColor Red
        return $false
    }
}

# Check if Docker containers are running
Write-Host "🔍 Checking Docker containers..." -ForegroundColor Cyan
$postgresRunning = docker ps --filter "name=learning-platform-postgres" --filter "status=running" --quiet
if (-not $postgresRunning) {
    Write-Host "❌ PostgreSQL container is not running. Please start it first with: docker-compose up -d postgres" -ForegroundColor Red
    exit 1
}

Write-Host "✅ PostgreSQL container is running" -ForegroundColor Green

# Run migrations for each service
$migrations = @(
    @{ Database = "user_service_db"; File = "services/user-service/src/main/resources/db/migration/V1__Create_user_tables.sql" },
    @{ Database = "agent_orchestrator_db"; File = "services/agent-orchestrator/src/main/resources/db/migration/V1__Create_agent_tables.sql" },
    @{ Database = "content_service_db"; File = "services/content-service/src/main/resources/db/migration/V1__Create_content_tables.sql" },
    @{ Database = "payment_service_db"; File = "services/payment-service/src/main/resources/db/migration/V1__Create_payment_tables.sql" }
)

$successCount = 0
$totalCount = $migrations.Count

foreach ($migration in $migrations) {
    if (Execute-SqlFile -Database $migration.Database -SqlFile $migration.File) {
        $successCount++
    }
}

Write-Host ""
Write-Host "📈 Migration Summary:" -ForegroundColor Cyan
Write-Host "✅ Successful: $successCount/$totalCount" -ForegroundColor Green

if ($successCount -eq $totalCount) {
    Write-Host "🎉 All migrations completed successfully!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "⚠️  Some migrations failed. Please check the errors above." -ForegroundColor Yellow
    exit 1
}