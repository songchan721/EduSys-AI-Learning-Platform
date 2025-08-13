#!/bin/bash

# Multi-Agent Learning Platform - Development Stop Script (Unix/Linux/macOS)

echo "🛑 Stopping Multi-Agent Learning Platform Development Environment"

# Stop all services
docker-compose down

echo "🧹 Cleaning up..."

# Optional: Remove volumes (uncomment if you want to reset all data)
# docker-compose down -v

echo "✅ Development environment stopped"