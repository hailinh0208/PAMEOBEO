CREATE TABLE notification_logs (
    id         UUID PRIMARY KEY,
    order_id   UUID         NOT NULL,
    recipient  VARCHAR(20)  NOT NULL,
    to_address VARCHAR(255) NOT NULL,
    subject    VARCHAR(255) NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    thread     VARCHAR(255),
    sent_at    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_notification_logs_sent_at ON notification_logs (sent_at DESC);

CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
