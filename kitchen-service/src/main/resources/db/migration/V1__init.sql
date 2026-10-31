CREATE TABLE menu_items (
    id        UUID PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    flavor    VARCHAR(50)  NOT NULL,
    price_vnd BIGINT       NOT NULL CHECK (price_vnd > 0),
    active    BOOLEAN      NOT NULL DEFAULT TRUE
);

-- One row per cooking day. version = optimistic locking.
CREATE TABLE cooking_days (
    cooking_date   DATE PRIMARY KEY,
    capacity       INT    NOT NULL CHECK (capacity >= 0),
    reserved_count INT    NOT NULL CHECK (reserved_count >= 0),
    version        BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_not_overbooked CHECK (reserved_count <= capacity)
);

CREATE TABLE slot_reservations (
    order_id     UUID PRIMARY KEY,
    cooking_date DATE        NOT NULL,
    quantity     INT         NOT NULL CHECK (quantity > 0),
    status       VARCHAR(20) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
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

INSERT INTO menu_items (id, name, flavor, price_vnd, active) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Pate gà bí đỏ',      'CHICKEN', 45000, TRUE),
    ('22222222-2222-2222-2222-222222222222', 'Pate cá hồi rau củ', 'SALMON',  65000, TRUE),
    ('33333333-3333-3333-3333-333333333333', 'Pate bò cà rốt',     'BEEF',    55000, TRUE);
