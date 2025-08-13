-- Create databases for each microservice
CREATE DATABASE user_service_db;
CREATE DATABASE agent_orchestrator_db;
CREATE DATABASE content_service_db;
CREATE DATABASE payment_service_db;
CREATE DATABASE notification_service_db;
CREATE DATABASE analytics_service_db;
CREATE DATABASE audit_service_db;

-- Create users for each service
CREATE USER user_service WITH PASSWORD 'user_service_password';
CREATE USER agent_orchestrator WITH PASSWORD 'agent_orchestrator_password';
CREATE USER content_service WITH PASSWORD 'content_service_password';
CREATE USER payment_service WITH PASSWORD 'payment_service_password';
CREATE USER notification_service WITH PASSWORD 'notification_service_password';
CREATE USER analytics_service WITH PASSWORD 'analytics_service_password';
CREATE USER audit_service WITH PASSWORD 'audit_service_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE user_service_db TO user_service;
GRANT ALL PRIVILEGES ON DATABASE agent_orchestrator_db TO agent_orchestrator;
GRANT ALL PRIVILEGES ON DATABASE content_service_db TO content_service;
GRANT ALL PRIVILEGES ON DATABASE payment_service_db TO payment_service;
GRANT ALL PRIVILEGES ON DATABASE notification_service_db TO notification_service;
GRANT ALL PRIVILEGES ON DATABASE analytics_service_db TO analytics_service;
GRANT ALL PRIVILEGES ON DATABASE audit_service_db TO audit_service;