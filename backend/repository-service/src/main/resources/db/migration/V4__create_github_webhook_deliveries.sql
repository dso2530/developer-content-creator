CREATE TABLE github_webhook_deliveries
(
    id           UUID                     NOT NULL,
    delivery_id  VARCHAR(255)             NOT NULL,
    event_type   VARCHAR(100)             NOT NULL,
    action       VARCHAR(100)             NOT NULL,
    status       VARCHAR(20)              NOT NULL,
    received_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_github_webhook_deliveries
        PRIMARY KEY (id),

    CONSTRAINT uk_github_webhook_deliveries_delivery_id
        UNIQUE (delivery_id),

    CONSTRAINT chk_github_webhook_deliveries_status
        CHECK (status IN ('RECEIVED', 'PROCESSED')),

    CONSTRAINT chk_github_webhook_deliveries_processed_at
        CHECK (
            (status = 'RECEIVED' AND processed_at IS NULL)
            OR
            (status = 'PROCESSED' AND processed_at IS NOT NULL)
        )
);

CREATE INDEX idx_github_webhook_deliveries_received_at
    ON github_webhook_deliveries (received_at);