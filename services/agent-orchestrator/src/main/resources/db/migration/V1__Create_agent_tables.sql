-- Agent Orchestrator Service Database Schema
-- Version 1.0 - Initial schema creation

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create learning_sessions table
CREATE TABLE learning_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    topic TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    estimated_duration_minutes INTEGER,
    actual_duration_minutes INTEGER,
    quality_score DECIMAL(3,2),
    cost_usd DECIMAL(10,4),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_session_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_quality_score CHECK (quality_score IS NULL OR (quality_score >= 0 AND quality_score <= 1)),
    CONSTRAINT chk_cost_usd CHECK (cost_usd IS NULL OR cost_usd >= 0)
);

-- Create agent_executions table
CREATE TABLE agent_executions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES learning_sessions(id) ON DELETE CASCADE,
    agent_type VARCHAR(50) NOT NULL,
    stage_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    input_data JSONB,
    output_data JSONB,
    error_message TEXT,
    llm_provider VARCHAR(50),
    llm_model VARCHAR(100),
    tokens_used INTEGER,
    cost_usd DECIMAL(10,6),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_execution_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_agent_type CHECK (agent_type IN ('RESEARCH', 'VERIFICATION', 'DECOMPOSITION', 'STRUCTURING', 'CONTENT_GENERATION', 'VALIDATION', 'SYNTHESIS', 'LEARNING_EXPERIENCE')),
    CONSTRAINT chk_stage_number CHECK (stage_number >= 1 AND stage_number <= 8),
    CONSTRAINT chk_tokens_used CHECK (tokens_used IS NULL OR tokens_used >= 0),
    CONSTRAINT chk_execution_cost_usd CHECK (cost_usd IS NULL OR cost_usd >= 0)
);

-- Create agent_checkpoints table
CREATE TABLE agent_checkpoints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    execution_id UUID NOT NULL REFERENCES agent_executions(id) ON DELETE CASCADE,
    checkpoint_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create agent_configurations table
CREATE TABLE agent_configurations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    agent_type VARCHAR(50) NOT NULL,
    configuration JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_config_agent_type CHECK (agent_type IN ('RESEARCH', 'VERIFICATION', 'DECOMPOSITION', 'STRUCTURING', 'CONTENT_GENERATION', 'VALIDATION', 'SYNTHESIS', 'LEARNING_EXPERIENCE'))
);

-- Create session_metrics table for performance tracking
CREATE TABLE session_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES learning_sessions(id) ON DELETE CASCADE,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(15,6) NOT NULL,
    metric_unit VARCHAR(20),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create agent_performance_logs table
CREATE TABLE agent_performance_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    execution_id UUID NOT NULL REFERENCES agent_executions(id) ON DELETE CASCADE,
    operation VARCHAR(100) NOT NULL,
    duration_ms BIGINT NOT NULL,
    memory_used_mb INTEGER,
    cpu_usage_percent DECIMAL(5,2),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_learning_sessions_user_id ON learning_sessions(user_id);
CREATE INDEX idx_learning_sessions_status ON learning_sessions(status);
CREATE INDEX idx_learning_sessions_created_at ON learning_sessions(created_at);
CREATE INDEX idx_learning_sessions_completed_at ON learning_sessions(completed_at);

CREATE INDEX idx_agent_executions_session_id ON agent_executions(session_id);
CREATE INDEX idx_agent_executions_agent_type ON agent_executions(agent_type);
CREATE INDEX idx_agent_executions_status ON agent_executions(status);
CREATE INDEX idx_agent_executions_stage_number ON agent_executions(stage_number);
CREATE INDEX idx_agent_executions_created_at ON agent_executions(created_at);

CREATE INDEX idx_agent_checkpoints_execution_id ON agent_checkpoints(execution_id);
CREATE INDEX idx_agent_checkpoints_created_at ON agent_checkpoints(created_at);

CREATE INDEX idx_agent_configurations_user_id ON agent_configurations(user_id);
CREATE INDEX idx_agent_configurations_agent_type ON agent_configurations(agent_type);
CREATE INDEX idx_agent_configurations_is_active ON agent_configurations(is_active);

CREATE INDEX idx_session_metrics_session_id ON session_metrics(session_id);
CREATE INDEX idx_session_metrics_name ON session_metrics(metric_name);
CREATE INDEX idx_session_metrics_recorded_at ON session_metrics(recorded_at);

CREATE INDEX idx_agent_performance_logs_execution_id ON agent_performance_logs(execution_id);
CREATE INDEX idx_agent_performance_logs_operation ON agent_performance_logs(operation);
CREATE INDEX idx_agent_performance_logs_recorded_at ON agent_performance_logs(recorded_at);

-- Create unique constraint for agent configurations (one active config per agent type per user)
CREATE UNIQUE INDEX idx_agent_configurations_user_agent_active 
ON agent_configurations(user_id, agent_type) 
WHERE is_active = true;

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_agent_configurations_updated_at BEFORE UPDATE ON agent_configurations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to automatically update session progress
CREATE OR REPLACE FUNCTION update_session_progress()
RETURNS TRIGGER AS $$
BEGIN
    -- Update session status based on agent execution status
    IF NEW.status = 'COMPLETED' THEN
        -- Check if all agents are completed
        IF (SELECT COUNT(*) FROM agent_executions 
            WHERE session_id = NEW.session_id AND status != 'COMPLETED') = 0 THEN
            UPDATE learning_sessions 
            SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP 
            WHERE id = NEW.session_id AND status != 'COMPLETED';
        ELSE
            -- Update to in progress if not already
            UPDATE learning_sessions 
            SET status = 'IN_PROGRESS' 
            WHERE id = NEW.session_id AND status = 'PENDING';
        END IF;
    ELSIF NEW.status = 'FAILED' THEN
        -- Mark session as failed if any agent fails
        UPDATE learning_sessions 
        SET status = 'FAILED', completed_at = CURRENT_TIMESTAMP 
        WHERE id = NEW.session_id AND status IN ('PENDING', 'IN_PROGRESS');
    ELSIF NEW.status = 'RUNNING' THEN
        -- Update to in progress
        UPDATE learning_sessions 
        SET status = 'IN_PROGRESS' 
        WHERE id = NEW.session_id AND status = 'PENDING';
    END IF;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for automatic session progress updates
CREATE TRIGGER update_session_progress_trigger 
AFTER UPDATE OF status ON agent_executions
FOR EACH ROW EXECUTE FUNCTION update_session_progress();