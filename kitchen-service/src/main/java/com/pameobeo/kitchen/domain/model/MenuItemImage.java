package com.pameobeo.kitchen.domain.model;

import com.pameobeo.kitchen.domain.exception.InvalidImageException;
import com.pameobeo.kitchen.domain.exception.TooManyImagesException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * A photo of a menu item, taken by the shop. Position 0 is the cover shown on the menu cards.
 * An image is never modified: a new photo gets a new id, so its URL can be cached forever.
 */
public record MenuItemImage(UUID id, UUID menuItemId, int position, String contentType, byte[] data,
                            Instant createdAt) {

    public static final int MAX_PER_ITEM = 3;
    public static final int MAX_BYTES = 2 * 1024 * 1024;

    public MenuItemImage {
        Objects.requireNonNull(id);
        Objects.requireNonNull(menuItemId);
        Objects.requireNonNull(contentType);
        Objects.requireNonNull(data);
        Objects.requireNonNull(createdAt);
    }

    /**
     * Checks the photo and places it after the existing ones.
     * The type is read from the file's first bytes, not trusted from the upload, so a renamed file is rejected.
     */
    public static MenuItemImage upload(UUID menuItemId, int existingImages, byte[] data, Instant now) {
        if (existingImages >= MAX_PER_ITEM) {
            throw new TooManyImagesException(MAX_PER_ITEM);
        }
        if (data == null || data.length == 0) {
            throw new InvalidImageException("The file is empty");
        }
        if (data.length > MAX_BYTES) {
            throw new InvalidImageException("The image is larger than " + MAX_BYTES / (1024 * 1024) + " MB");
        }
        String contentType = detectType(data);
        if (contentType == null) {
            throw new InvalidImageException("Only JPG, PNG or WebP images are accepted");
        }
        return new MenuItemImage(UUID.randomUUID(), menuItemId, existingImages, contentType, data, now);
    }

    /** @return the media type from the file signature, or null if it is not a JPG, PNG or WebP image */
    static String detectType(byte[] d) {
        if (startsWith(d, 0, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (startsWith(d, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "image/png";
        }
        if (startsWith(d, 0, 'R', 'I', 'F', 'F') && startsWith(d, 8, 'W', 'E', 'B', 'P')) {
            return "image/webp";
        }
        return null;
    }

    private static boolean startsWith(byte[] data, int offset, int... signature) {
        if (data.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((data[offset + i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    // records compare arrays by reference: compare the content instead
    @Override
    public boolean equals(Object o) {
        return o instanceof MenuItemImage other && id.equals(other.id) && menuItemId.equals(other.menuItemId)
                && position == other.position && contentType.equals(other.contentType)
                && Arrays.equals(data, other.data) && createdAt.equals(other.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, menuItemId, position, contentType, Arrays.hashCode(data), createdAt);
    }

    @Override
    public String toString() {
        return "MenuItemImage[id=" + id + ", menuItemId=" + menuItemId + ", position=" + position
                + ", contentType=" + contentType + ", bytes=" + data.length + "]";
    }
}
