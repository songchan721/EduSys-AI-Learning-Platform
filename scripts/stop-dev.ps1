# Multi-Agent Learning Platform - Development Stop Script (Windows PowerShell)

Write-Host "🛑 Stopping Multi-Agent Learning Platform Development Environment" -ForegroundColor Red

# Stop all services
docker-compose down

Write-Host "🧹 Cleaning up..." -ForegroundColor Yellow

# Optional: Remove volumes (uncomment if you want to reset all data)
# docker-compose down -v

Write-Host "✅ Development environment stopped" -ForegroundColor Green