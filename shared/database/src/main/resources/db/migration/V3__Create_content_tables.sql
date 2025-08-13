-- Content Service Database Schema
-- Version 3.0 - Content management and analytics

-- Create generated_content table
CREATE TABLE generated_content (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    content_data JSONB NOT NULL,
    file_path VARCHAR(1000),
    version INTEGER NOT NULL DEFAULT 1,
    quality_score DECIMAL(3,2),
    word_count INTEGER,
    estimated_reading_time_minutes INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_content_type CHECK (content_type IN ('TEXT', 'DIAGRAM', 'EXERCISE', 'QUIZ', 'ASSESSMENT', 'FLASHCARD', 'MIND_MAP', 'TUTORIAL', 'CODE_EXAMPLE', 'FORMULA')),
    CONSTRAINT chk_quality_score CHECK (quality_score IS NULL OR (quality_score >= 0 AND quality_score <= 1)),
    CONSTRAINT chk_version CHECK (version >= 1),
    CONSTRAINT chk_word_count CHECK (word_count IS NULL OR word_count >= 0),
    CONSTRAINT chk_reading_time CHECK (estimated_reading_time_minutes IS NULL OR estimated_reading_time_minutes >= 0)
);

-- Create content_versions table
CREATE TABLE content_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content_id UUID NOT NULL REFERENCES generated_content(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    content_data JSONB NOT NULL,
    changes_description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_version_number CHECK (version_number >= 1)
);

-- Create content_metadata table
CREATE TABLE content_metadata (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content_id UUID NOT NULL REFERENCES generated_content(id) ON DELETE CASCADE,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create content_tags table
CREATE TABLE content_tags (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content_id UUID NOT NULL REFERENCES generated_content(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    confidence_score DECIMAL(3,2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_confidence_score CHECK (confidence_score IS NULL OR (confidence_score >= 0 AND confidence_score <= 1))
);

-- Create content_feedback table
CREATE TABLE content_feedback (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content_id UUID NOT NULL REFERENCES generated_content(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    rating INTEGER NOT NULL,
    feedback_text TEXT,
    helpful BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_rating CHECK (rating >= 1 AND rating <= 5)
);

-- Create content_analytics table
CREATE TABLE content_analytics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content_id UUID NOT NULL REFERENCES generated_content(id) ON DELETE CASCADE,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(15,6) NOT NULL,
    metric_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create content_search_index table for full-text search
CREATE TABLE content_search_index (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content_id UUID NOT NULL REFERENCES generated_content(id) ON DELETE CASCADE,
    search_vector TSVECTOR NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'english',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create content_recommendations table
CREATE TABLE content_recommendations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    content_id UUID NOT NULL REFERENCES generated_content(id) ON DELETE CASCADE,
    recommendation_score DECIMAL(5,4) NOT NULL,
    recommendation_reason VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_recommendation_score CHECK (recommendation_score >= 0 AND recommendation_score <= 1)
);

-- Create indexes for performance
CREATE INDEX idx_generated_content_session_id ON generated_content(session_id);
CREATE INDEX idx_generated_content_content_type ON generated_content(content_type);
CREATE INDEX idx_generated_content_created_at ON generated_content(created_at);
CREATE INDEX idx_generated_content_quality_score ON generated_content(quality_score);
CREATE INDEX idx_generated_content_title ON generated_content USING gin(to_tsvector('english', title));

CREATE INDEX idx_content_versions_content_id ON content_versions(content_id);
CREATE INDEX idx_content_versions_version_number ON content_versions(version_number);
CREATE INDEX idx_content_versions_created_at ON content_versions(created_at);

CREATE INDEX idx_content_metadata_content_id ON content_metadata(content_id);
CREATE INDEX idx_content_metadata_key ON content_metadata(metadata_key);

CREATE INDEX idx_content_tags_content_id ON content_tags(content_id);
CREATE INDEX idx_content_tags_tag ON content_tags(tag);
CREATE INDEX idx_content_tags_confidence_score ON content_tags(confidence_score);

CREATE INDEX idx_content_feedback_content_id ON content_feedback(content_id);
CREATE INDEX idx_content_feedback_user_id ON content_feedback(user_id);
CREATE INDEX idx_content_feedback_rating ON content_feedback(rating);
CREATE INDEX idx_content_feedback_created_at ON content_feedback(created_at);

CREATE INDEX idx_content_analytics_content_id ON content_analytics(content_id);
CREATE INDEX idx_content_analytics_metric_name ON content_analytics(metric_name);
CREATE INDEX idx_content_analytics_metric_date ON content_analytics(metric_date);

CREATE INDEX idx_content_search_index_content_id ON content_search_index(content_id);
CREATE INDEX idx_content_search_vector ON content_search_index USING gin(search_vector);

CREATE INDEX idx_content_recommendations_user_id ON content_recommendations(user_id);
CREATE INDEX idx_content_recommendations_content_id ON content_recommendations(content_id);
CREATE INDEX idx_content_recommendations_score ON content_recommendations(recommendation_score);
CREATE INDEX idx_content_recommendations_created_at ON content_recommendations(created_at);

-- Create unique constraints
CREATE UNIQUE INDEX idx_content_versions_content_version ON content_versions(content_id, version_number);
CREATE UNIQUE INDEX idx_content_metadata_content_key ON content_metadata(content_id, metadata_key);
CREATE UNIQUE INDEX idx_content_tags_content_tag ON content_tags(content_id, tag);
CREATE UNIQUE INDEX idx_content_feedback_user_content ON content_feedback(user_id, content_id);
CREATE UNIQUE INDEX idx_content_search_index_content ON content_search_index(content_id);
CREATE UNIQUE INDEX idx_content_recommendations_user_content ON content_recommendations(user_id, content_id);

-- Create triggers for updated_at
CREATE TRIGGER update_generated_content_updated_at BEFORE UPDATE ON generated_content
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_content_search_index_updated_at BEFORE UPDATE ON content_search_index
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to update search index
CREATE OR REPLACE FUNCTION update_content_search_index()
RETURNS TRIGGER AS $$
BEGIN
    -- Update or insert search index
    INSERT INTO content_search_index (content_id, search_vector, language)
    VALUES (
        NEW.id,
        to_tsvector('english', NEW.title || ' ' || COALESCE(NEW.content_data->>'text', '')),
        'english'
    )
    ON CONFLICT (content_id) DO UPDATE SET
        search_vector = to_tsvector('english', NEW.title || ' ' || COALESCE(NEW.content_data->>'text', '')),
        updated_at = CURRENT_TIMESTAMP;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for automatic search index updates
CREATE TRIGGER update_content_search_index_trigger 
AFTER INSERT OR UPDATE OF title, content_data ON generated_content
FOR EACH ROW EXECUTE FUNCTION update_content_search_index();

-- Create function to calculate average rating
CREATE OR REPLACE FUNCTION calculate_average_rating()
RETURNS TRIGGER AS $$
BEGIN
    -- Update average rating in analytics
    INSERT INTO content_analytics (content_id, metric_name, metric_value, metric_date)
    SELECT 
        NEW.content_id,
        'average_rating',
        AVG(rating)::DECIMAL(3,2),
        CURRENT_DATE
    FROM content_feedback 
    WHERE content_id = NEW.content_id
    ON CONFLICT (content_id, metric_name, metric_date) DO UPDATE SET
        metric_value = (
            SELECT AVG(rating)::DECIMAL(3,2) 
            FROM content_feedback 
            WHERE content_id = NEW.content_id
        );
    
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for automatic rating calculation
CREATE TRIGGER calculate_average_rating_trigger 
AFTER INSERT OR UPDATE OF rating ON content_feedback
FOR EACH ROW EXECUTE FUNCTION calculate_average_rating();