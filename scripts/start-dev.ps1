# Multi-Agent Learning Platform - Development Startup Script (Windows PowerShell)

Write-Host "🚀 Starting Multi-Agent Learning Platform Development Environment" -ForegroundColor Green

# Check if Docker is running
try {
    docker info | Out-Null
} catch {
    Write-Host "❌ Docker is not running. Please start Docker and try again." -ForegroundColor Red
    exit 1
}

# Check if Docker Compose is available
try {
    docker-compose --version | Out-Null
} catch {
    Write-Host "❌ Docker Compose is not installed. Please install Docker Compose and try again." -ForegroundColor Red
    exit 1
}

Write-Host "📦 Starting infrastructure services..." -ForegroundColor Yellow
docker-compose up -d postgres redis redpanda minio prometheus grafana jaeger

Write-Host "⏳ Waiting for services to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Wait for PostgreSQL
Write-Host "🔍 Waiting for PostgreSQL..." -ForegroundColor Yellow
do {
    try {
        docker-compose exec -T postgres pg_isready -U postgres | Out-Null
        $postgresReady = $true
    } catch {
        Write-Host "   PostgreSQL is not ready yet..." -ForegroundColor Gray
        Start-Sleep -Seconds 2
        $postgresReady = $false
    }
} while (-not $postgresReady)
Write-Host "✅ PostgreSQL is ready" -ForegroundColor Green

# Wait for Redis
Write-Host "🔍 Waiting for Redis..." -ForegroundColor Yellow
do {
    try {
        docker-compose exec -T redis redis-cli ping | Out-Null
        $redisReady = $true
    } catch {
        Write-Host "   Redis is not ready yet..." -ForegroundColor Gray
        Start-Sleep -Seconds 2
        $redisReady = $false
    }
} while (-not $redisReady)
Write-Host "✅ Redis is ready" -ForegroundColor Green

# Wait for Redpanda
Write-Host "🔍 Waiting for Redpanda..." -ForegroundColor Yellow
do {
    try {
        docker-compose exec -T redpanda rpk cluster health | Out-Null
        $redpandaReady = $true
    } catch {
        Write-Host "   Redpanda is not ready yet..." -ForegroundColor Gray
        Start-Sleep -Seconds 2
        $redpandaReady = $false
    }
} while (-not $redpandaReady)
Write-Host "✅ Redpanda is ready" -ForegroundColor Green

# Create Redpanda topics
Write-Host "📝 Creating Redpanda topics..." -ForegroundColor Yellow
docker-compose exec -T redpanda rpk topic create user-events --partitions 12 --replicas 1
docker-compose exec -T redpanda rpk topic create session-events --partitions 24 --replicas 1
docker-compose exec -T redpanda rpk topic create agent-events --partitions 24 --replicas 1
docker-compose exec -T redpanda rpk topic create content-events --partitions 12 --replicas 1
docker-compose exec -T redpanda rpk topic create payment-events --partitions 6 --replicas 1
docker-compose exec -T redpanda rpk topic create system-events --partitions 6 --replicas 1

# Wait for MinIO
Write-Host "🔍 Waiting for MinIO..." -ForegroundColor Yellow
do {
    try {
        Invoke-WebRequest -Uri "http://localhost:9000/minio/health/live" -UseBasicParsing | Out-Null
        $minioReady = $true
    } catch {
        Write-Host "   MinIO is not ready yet..." -ForegroundColor Gray
        Start-Sleep -Seconds 2
        $minioReady = $false
    }
} while (-not $minioReady)
Write-Host "✅ MinIO is ready" -ForegroundColor Green

Write-Host ""
Write-Host "🎉 Development environment is ready!" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Service URLs:" -ForegroundColor Cyan
Write-Host "   PostgreSQL:     localhost:5432"
Write-Host "   Redis:          localhost:6379"
Write-Host "   Redpanda:       localhost:19092"
Write-Host "   MinIO:          http://localhost:9000 (admin/minioadmin)"
Write-Host "   MinIO Console:  http://localhost:9001"
Write-Host "   Prometheus:     http://localhost:9090"
Write-Host "   Grafana:        http://localhost:3000 (admin/admin)"
Write-Host "   Jaeger:         http://localhost:16686"
Write-Host ""
Write-Host "🔧 To build and run services:" -ForegroundColor Cyan
Write-Host "   .\gradlew.bat build"
Write-Host "   .\gradlew.bat :services:user-service:bootRun"
Write-Host ""
Write-Host "🛑 To stop all services:" -ForegroundColor Cyan
Write-Host "   .\scripts\stop-dev.ps1"