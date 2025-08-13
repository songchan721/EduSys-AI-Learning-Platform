#!/bin/bash

# Multi-Agent Learning Platform - Development Startup Script (Unix/Linux/macOS)

set -e

echo "🚀 Starting Multi-Agent Learning Platform Development Environment"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose > /dev/null 2>&1; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose and try again."
    exit 1
fi

echo "📦 Starting infrastructure services..."
docker-compose up -d postgres redis redpanda minio prometheus grafana jaeger

echo "⏳ Waiting for services to be ready..."
sleep 10

# Wait for PostgreSQL
echo "🔍 Waiting for PostgreSQL..."
until docker-compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1; do
    echo "   PostgreSQL is not ready yet..."
    sleep 2
done
echo "✅ PostgreSQL is ready"

# Wait for Redis
echo "🔍 Waiting for Redis..."
until docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; do
    echo "   Redis is not ready yet..."
    sleep 2
done
echo "✅ Redis is ready"

# Wait for Redpanda
echo "🔍 Waiting for Redpanda..."
until docker-compose exec -T redpanda rpk cluster health > /dev/null 2>&1; do
    echo "   Redpanda is not ready yet..."
    sleep 2
done
echo "✅ Redpanda is ready"

# Create Redpanda topics
echo "📝 Creating Redpanda topics..."
docker-compose exec -T redpanda rpk topic create user-events --partitions 12 --replicas 1
docker-compose exec -T redpanda rpk topic create session-events --partitions 24 --replicas 1
docker-compose exec -T redpanda rpk topic create agent-events --partitions 24 --replicas 1
docker-compose exec -T redpanda rpk topic create content-events --partitions 12 --replicas 1
docker-compose exec -T redpanda rpk topic create payment-events --partitions 6 --replicas 1
docker-compose exec -T redpanda rpk topic create system-events --partitions 6 --replicas 1

# Wait for MinIO
echo "🔍 Waiting for MinIO..."
until curl -f http://localhost:9000/minio/health/live > /dev/null 2>&1; do
    echo "   MinIO is not ready yet..."
    sleep 2
done
echo "✅ MinIO is ready"

echo ""
echo "🎉 Development environment is ready!"
echo ""
echo "📊 Service URLs:"
echo "   PostgreSQL:     localhost:5432"
echo "   Redis:          localhost:6379"
echo "   Redpanda:       localhost:19092"
echo "   MinIO:          http://localhost:9000 (admin/minioadmin)"
echo "   MinIO Console:  http://localhost:9001"
echo "   Prometheus:     http://localhost:9090"
echo "   Grafana:        http://localhost:3000 (admin/admin)"
echo "   Jaeger:         http://localhost:16686"
echo ""
echo "🔧 To build and run services:"
echo "   ./gradlew build"
echo "   ./gradlew :services:user-service:bootRun"
echo ""
echo "🛑 To stop all services:"
echo "   ./scripts/stop-dev.sh"