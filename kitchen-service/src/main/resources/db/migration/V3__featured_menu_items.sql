-- "Gợi ý của tiệm": items picked by the admin, shown at the top of the shop while there are not enough
-- sales to rank best sellers (see MenuHighlights).
ALTER TABLE menu_items ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE;

-- Counting the jars sold over the last 30 days scans held reservations by date
CREATE INDEX idx_slot_reservations_created ON slot_reservations (created_at) WHERE status = 'RESERVED';
