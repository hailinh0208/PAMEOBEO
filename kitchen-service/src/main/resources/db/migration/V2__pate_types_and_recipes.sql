-- 4 flavors (chicken, tuna, beef, salmon) x 4 types (single / mix of 2, with / without liver).
-- Every combination is its own menu item with its own price (full price table, editable by the admin).
-- The name and the recipe follow from (meat1, meat2, with_liver), so they are not stored.

ALTER TABLE menu_items
    ADD COLUMN meat1      VARCHAR(20),
    ADD COLUMN meat2      VARCHAR(20),
    ADD COLUMN with_liver BOOLEAN NOT NULL DEFAULT TRUE;

-- The 3 original items keep their ids (scripts, JMeter and old orders refer to them)
UPDATE menu_items SET meat1 = 'CHICKEN' WHERE id = '11111111-1111-1111-1111-111111111111';
UPDATE menu_items SET meat1 = 'SALMON'  WHERE id = '22222222-2222-2222-2222-222222222222';
UPDATE menu_items SET meat1 = 'BEEF'    WHERE id = '33333333-3333-3333-3333-333333333333';

ALTER TABLE menu_items
    ALTER COLUMN meat1 SET NOT NULL,
    ALTER COLUMN with_liver DROP DEFAULT,
    DROP COLUMN name,
    DROP COLUMN flavor,
    ADD CONSTRAINT chk_meat1 CHECK (meat1 IN ('CHICKEN', 'TUNA', 'BEEF', 'SALMON')),
    ADD CONSTRAINT chk_meat2 CHECK (meat2 IS NULL OR (meat2 IN ('CHICKEN', 'TUNA', 'BEEF', 'SALMON') AND meat2 <> meat1)),
    ADD CONSTRAINT uq_menu_combination UNIQUE NULLS NOT DISTINCT (meat1, meat2, with_liver);

-- Starting prices, to be adjusted by the admin. A mix is placed in canonical order: CHICKEN, TUNA, BEEF, SALMON.
INSERT INTO menu_items (id, meat1, meat2, with_liver, price_vnd, active) VALUES
    -- single flavor, with liver
    ('a1000000-0000-0000-0000-000000000002', 'TUNA',    NULL,     TRUE,  50000, TRUE),
    -- single flavor, without liver
    ('a2000000-0000-0000-0000-000000000001', 'CHICKEN', NULL,     FALSE, 50000, TRUE),
    ('a2000000-0000-0000-0000-000000000002', 'TUNA',    NULL,     FALSE, 55000, TRUE),
    ('a2000000-0000-0000-0000-000000000003', 'BEEF',    NULL,     FALSE, 60000, TRUE),
    ('a2000000-0000-0000-0000-000000000004', 'SALMON',  NULL,     FALSE, 70000, TRUE),
    -- mix of 2, with liver
    ('a3000000-0000-0000-0000-000000000012', 'CHICKEN', 'TUNA',   TRUE,  48000, TRUE),
    ('a3000000-0000-0000-0000-000000000013', 'CHICKEN', 'BEEF',   TRUE,  50000, TRUE),
    ('a3000000-0000-0000-0000-000000000014', 'CHICKEN', 'SALMON', TRUE,  55000, TRUE),
    ('a3000000-0000-0000-0000-000000000023', 'TUNA',    'BEEF',   TRUE,  53000, TRUE),
    ('a3000000-0000-0000-0000-000000000024', 'TUNA',    'SALMON', TRUE,  58000, TRUE),
    ('a3000000-0000-0000-0000-000000000034', 'BEEF',    'SALMON', TRUE,  60000, TRUE),
    -- mix of 2, without liver
    ('a4000000-0000-0000-0000-000000000012', 'CHICKEN', 'TUNA',   FALSE, 53000, TRUE),
    ('a4000000-0000-0000-0000-000000000013', 'CHICKEN', 'BEEF',   FALSE, 55000, TRUE),
    ('a4000000-0000-0000-0000-000000000014', 'CHICKEN', 'SALMON', FALSE, 60000, TRUE),
    ('a4000000-0000-0000-0000-000000000023', 'TUNA',    'BEEF',   FALSE, 58000, TRUE),
    ('a4000000-0000-0000-0000-000000000024', 'TUNA',    'SALMON', FALSE, 63000, TRUE),
    ('a4000000-0000-0000-0000-000000000034', 'BEEF',    'SALMON', FALSE, 65000, TRUE);

-- Which menu items each reservation holds: the kitchen sums the recipes to know what to buy for a cooking day.
-- Reservations made before this migration have no rows here.
CREATE TABLE slot_reservation_items (
    order_id     UUID NOT NULL REFERENCES slot_reservations (order_id),
    menu_item_id UUID NOT NULL,
    quantity     INT  NOT NULL CHECK (quantity > 0)
);
CREATE INDEX idx_slot_reservation_items_order ON slot_reservation_items (order_id);
