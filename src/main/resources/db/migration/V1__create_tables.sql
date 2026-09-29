CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    webhook_url VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_subscription_client_event UNIQUE (client_id, event_type)
);

CREATE TABLE notification_events (
    event_id VARCHAR(64) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    delivery_date TIMESTAMPTZ NOT NULL,
    delivery_status VARCHAR(20) NOT NULL,
    client_id VARCHAR(64) NOT NULL,
    webhook_url VARCHAR(500),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT
);

CREATE INDEX idx_notification_client_date
ON notification_events(client_id, delivery_date);

CREATE INDEX idx_notification_client_status
ON notification_events(client_id, delivery_status);

INSERT INTO subscriptions(client_id,event_type,webhook_url,active) VALUES
('CLIENT001','credit_card_payment','http://host.docker.internal:8081/webhook',true),
('CLIENT001','debit_card_withdrawal','http://host.docker.internal:8081/webhook',true),
('CLIENT001','credit_deposit','http://host.docker.internal:8081/webhook',true),
('CLIENT001','debit_subscription','http://host.docker.internal:8081/webhook',true),
('CLIENT002','credit_transfer','http://host.docker.internal:8081/webhook',true),
('CLIENT002','debit_automatic_payment','http://host.docker.internal:8081/webhook',true),
('CLIENT002','debit_purchase','http://host.docker.internal:8081/webhook',true),
('CLIENT003','credit_refund','http://host.docker.internal:8081/webhook',true),
('CLIENT003','debit_transfer','http://host.docker.internal:8081/webhook',true),
('CLIENT003','credit_cashback','http://host.docker.internal:8081/webhook',true);
