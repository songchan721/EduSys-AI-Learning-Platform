-- Payment Service Database Schema
-- Version 1.0 - Initial schema creation

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create subscriptions table
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    plan_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    price_usd DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    features JSONB NOT NULL DEFAULT '[]',
    limits JSONB NOT NULL DEFAULT '{}',
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    current_period_start TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    canceled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_subscription_status CHECK (status IN ('ACTIVE', 'CANCELED', 'PAST_DUE', 'UNPAID', 'TRIALING')),
    CONSTRAINT chk_plan_type CHECK (plan_type IN ('FREE', 'PRO', 'ENTERPRISE', 'CUSTOM')),
    CONSTRAINT chk_billing_cycle CHECK (billing_cycle IN ('MONTHLY', 'YEARLY', 'LIFETIME')),
    CONSTRAINT chk_price_usd CHECK (price_usd >= 0),
    CONSTRAINT chk_currency CHECK (currency IN ('USD', 'EUR', 'GBP', 'JPY'))
);

-- Create payment_methods table
CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_payment_method_id VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    last_four VARCHAR(4),
    brand VARCHAR(50),
    exp_month INTEGER,
    exp_year INTEGER,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_payment_provider CHECK (provider IN ('STRIPE', 'PAYPAL', 'APPLE_PAY', 'GOOGLE_PAY')),
    CONSTRAINT chk_payment_type CHECK (type IN ('CARD', 'BANK_ACCOUNT', 'DIGITAL_WALLET')),
    CONSTRAINT chk_exp_month CHECK (exp_month IS NULL OR (exp_month >= 1 AND exp_month <= 12)),
    CONSTRAINT chk_exp_year CHECK (exp_year IS NULL OR exp_year >= EXTRACT(YEAR FROM CURRENT_DATE))
);

-- Create payment_transactions table
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    subscription_id UUID REFERENCES subscriptions(id) ON DELETE SET NULL,
    payment_method_id UUID REFERENCES payment_methods(id) ON DELETE SET NULL,
    provider VARCHAR(50) NOT NULL,
    provider_transaction_id VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount_usd DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    description TEXT,
    metadata JSONB DEFAULT '{}',
    failure_reason TEXT,
    refunded_amount_usd DECIMAL(10,2) DEFAULT 0,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_transaction_provider CHECK (provider IN ('STRIPE', 'PAYPAL', 'APPLE_PAY', 'GOOGLE_PAY')),
    CONSTRAINT chk_transaction_type CHECK (type IN ('PAYMENT', 'REFUND', 'CHARGEBACK', 'ADJUSTMENT')),
    CONSTRAINT chk_transaction_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELED', 'REFUNDED')),
    CONSTRAINT chk_amount_usd CHECK (amount_usd >= 0),
    CONSTRAINT chk_refunded_amount CHECK (refunded_amount_usd >= 0 AND refunded_amount_usd <= amount_usd)
);

-- Create invoices table
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    subscription_id UUID REFERENCES subscriptions(id) ON DELETE SET NULL,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    amount_due_usd DECIMAL(10,2) NOT NULL,
    amount_paid_usd DECIMAL(10,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    description TEXT,
    line_items JSONB NOT NULL DEFAULT '[]',
    tax_amount_usd DECIMAL(10,2) DEFAULT 0,
    discount_amount_usd DECIMAL(10,2) DEFAULT 0,
    due_date TIMESTAMP WITH TIME ZONE,
    paid_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_invoice_status CHECK (status IN ('DRAFT', 'OPEN', 'PAID', 'VOID', 'UNCOLLECTIBLE')),
    CONSTRAINT chk_amount_due CHECK (amount_due_usd >= 0),
    CONSTRAINT chk_amount_paid CHECK (amount_paid_usd >= 0),
    CONSTRAINT chk_tax_amount CHECK (tax_amount_usd >= 0),
    CONSTRAINT chk_discount_amount CHECK (discount_amount_usd >= 0)
);

-- Create usage_records table
CREATE TABLE usage_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    subscription_id UUID REFERENCES subscriptions(id) ON DELETE CASCADE,
    metric_name VARCHAR(100) NOT NULL,
    quantity DECIMAL(15,6) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    billing_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    billing_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_quantity CHECK (quantity >= 0)
);

-- Create payment_webhooks table
CREATE TABLE payment_webhooks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_webhook_provider CHECK (provider IN ('STRIPE', 'PAYPAL', 'APPLE_PAY', 'GOOGLE_PAY')),
    CONSTRAINT chk_retry_count CHECK (retry_count >= 0)
);

-- Create billing_addresses table
CREATE TABLE billing_addresses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(2) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create payment_analytics table
CREATE TABLE payment_analytics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(15,6) NOT NULL,
    dimensions JSONB DEFAULT '{}',
    recorded_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_plan_type ON subscriptions(plan_type);
CREATE INDEX idx_subscriptions_current_period_end ON subscriptions(current_period_end);
CREATE INDEX idx_subscriptions_created_at ON subscriptions(created_at);

CREATE INDEX idx_payment_methods_user_id ON payment_methods(user_id);
CREATE INDEX idx_payment_methods_provider ON payment_methods(provider);
CREATE INDEX idx_payment_methods_is_default ON payment_methods(is_default);
CREATE INDEX idx_payment_methods_is_active ON payment_methods(is_active);

CREATE INDEX idx_payment_transactions_user_id ON payment_transactions(user_id);
CREATE INDEX idx_payment_transactions_subscription_id ON payment_transactions(subscription_id);
CREATE INDEX idx_payment_transactions_provider ON payment_transactions(provider);
CREATE INDEX idx_payment_transactions_status ON payment_transactions(status);
CREATE INDEX idx_payment_transactions_type ON payment_transactions(type);
CREATE INDEX idx_payment_transactions_created_at ON payment_transactions(created_at);
CREATE INDEX idx_payment_transactions_processed_at ON payment_transactions(processed_at);

CREATE INDEX idx_invoices_user_id ON invoices(user_id);
CREATE INDEX idx_invoices_subscription_id ON invoices(subscription_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
CREATE INDEX idx_invoices_created_at ON invoices(created_at);

CREATE INDEX idx_usage_records_user_id ON usage_records(user_id);
CREATE INDEX idx_usage_records_subscription_id ON usage_records(subscription_id);
CREATE INDEX idx_usage_records_metric_name ON usage_records(metric_name);
CREATE INDEX idx_usage_records_recorded_at ON usage_records(recorded_at);
CREATE INDEX idx_usage_records_billing_period ON usage_records(billing_period_start, billing_period_end);

CREATE INDEX idx_payment_webhooks_provider ON payment_webhooks(provider);
CREATE INDEX idx_payment_webhooks_event_type ON payment_webhooks(event_type);
CREATE INDEX idx_payment_webhooks_processed ON payment_webhooks(processed);
CREATE INDEX idx_payment_webhooks_created_at ON payment_webhooks(created_at);

CREATE INDEX idx_billing_addresses_user_id ON billing_addresses(user_id);
CREATE INDEX idx_billing_addresses_is_default ON billing_addresses(is_default);

CREATE INDEX idx_payment_analytics_metric_name ON payment_analytics(metric_name);
CREATE INDEX idx_payment_analytics_recorded_date ON payment_analytics(recorded_date);

-- Create unique constraints
CREATE UNIQUE INDEX idx_subscriptions_user_active ON subscriptions(user_id) 
WHERE status IN ('ACTIVE', 'TRIALING');

CREATE UNIQUE INDEX idx_payment_methods_user_default ON payment_methods(user_id) 
WHERE is_default = true AND is_active = true;

CREATE UNIQUE INDEX idx_payment_webhooks_provider_event ON payment_webhooks(provider, event_id);

CREATE UNIQUE INDEX idx_billing_addresses_user_default ON billing_addresses(user_id) 
WHERE is_default = true;

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_subscriptions_updated_at BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payment_methods_updated_at BEFORE UPDATE ON payment_methods
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payment_transactions_updated_at BEFORE UPDATE ON payment_transactions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_invoices_updated_at BEFORE UPDATE ON invoices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_billing_addresses_updated_at BEFORE UPDATE ON billing_addresses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to generate invoice numbers
CREATE OR REPLACE FUNCTION generate_invoice_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.invoice_number IS NULL THEN
        NEW.invoice_number := 'INV-' || TO_CHAR(CURRENT_DATE, 'YYYY') || '-' || 
                             LPAD(NEXTVAL('invoice_number_seq')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create sequence for invoice numbers
CREATE SEQUENCE invoice_number_seq START 1;

-- Create trigger for automatic invoice number generation
CREATE TRIGGER generate_invoice_number_trigger 
BEFORE INSERT ON invoices
FOR EACH ROW EXECUTE FUNCTION generate_invoice_number();

-- Create function to update subscription status based on payments
CREATE OR REPLACE FUNCTION update_subscription_status()
RETURNS TRIGGER AS $$
BEGIN
    -- Update subscription status based on payment status
    IF NEW.status = 'SUCCEEDED' AND NEW.type = 'PAYMENT' THEN
        UPDATE subscriptions 
        SET status = 'ACTIVE',
            current_period_start = CURRENT_TIMESTAMP,
            current_period_end = CASE 
                WHEN (SELECT billing_cycle FROM subscriptions WHERE id = NEW.subscription_id) = 'MONTHLY' 
                THEN CURRENT_TIMESTAMP + INTERVAL '1 month'
                WHEN (SELECT billing_cycle FROM subscriptions WHERE id = NEW.subscription_id) = 'YEARLY' 
                THEN CURRENT_TIMESTAMP + INTERVAL '1 year'
                ELSE current_period_end
            END
        WHERE id = NEW.subscription_id;
    ELSIF NEW.status = 'FAILED' AND NEW.type = 'PAYMENT' THEN
        UPDATE subscriptions 
        SET status = 'PAST_DUE'
        WHERE id = NEW.subscription_id;
    END IF;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for automatic subscription status updates
CREATE TRIGGER update_subscription_status_trigger 
AFTER UPDATE OF status ON payment_transactions
FOR EACH ROW EXECUTE FUNCTION update_subscription_status();

-- Insert default subscription plans
INSERT INTO subscriptions (id, user_id, plan_type, status, billing_cycle, price_usd, features, limits, current_period_end) VALUES 
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'FREE', 'ACTIVE', 'MONTHLY', 0.00, 
 '["basic_learning", "limited_sessions"]', 
 '{"sessions_per_month": 5, "agents_per_session": 3}', 
 CURRENT_TIMESTAMP + INTERVAL '1 month');