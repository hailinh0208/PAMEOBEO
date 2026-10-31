CREATE TABLE orders (
    id               UUID PRIMARY KEY,
    customer_id      UUID         NOT NULL,   -- user id from auth-service (no FK across services)
    customer_email   VARCHAR(255) NOT NULL,
    customer_name    VARCHAR(255),
    cooking_date     DATE         NOT NULL,
    cutoff_at        TIMESTAMPTZ  NOT NULL,
    delivery_address VARCHAR(500) NOT NULL,
    idempotency_key  VARCHAR(100),
    status           VARCHAR(20)  NOT NULL,
    status_reason    VARCHAR(100),
    total_vnd        BIGINT       NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_orders_idempotency UNIQUE (customer_id, idempotency_key)
);
CREATE INDEX idx_orders_customer ON orders (customer_id, created_at DESC);
CREATE INDEX idx_orders_cooking_date_status ON orders (cooking_date, status);

-- Name and price are copied from the kitchen menu at order time (snapshot)
CREATE TABLE order_items (
    order_id       UUID         NOT NULL REFERENCES orders (id),
    line_no        INT          NOT NULL,
    menu_item_id   UUID         NOT NULL,
    name           VARCHAR(255) NOT NULL,
    unit_price_vnd BIGINT       NOT NULL,
    quantity       INT          NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (order_id, line_no)
);

CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY,
    topic        VARCHAR(100) NOT NULL,
    message_key  VARCHAR(100) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ
);
CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;

CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
