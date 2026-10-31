-- Photos of each product, uploaded by the admin (at most 3 per item, checked by MenuItemImage).
-- Stored in the database with the menu: one place to back up, nothing to lose when moving the app.
-- position 0 = cover shown on the menu cards.
CREATE TABLE menu_item_images (
    id           UUID PRIMARY KEY,
    menu_item_id UUID         NOT NULL REFERENCES menu_items (id),
    position     INT          NOT NULL CHECK (position BETWEEN 0 AND 2),
    content_type VARCHAR(50)  NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    data         BYTEA        NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    -- with the position check: never more than 3 photos, even if two uploads run at the same time.
    -- Deferred so that reordering (moving a photo to the cover) can swap positions within a transaction.
    CONSTRAINT uq_menu_item_image_position UNIQUE (menu_item_id, position) DEFERRABLE INITIALLY DEFERRED
);
