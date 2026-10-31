package com.pameobeo.kitchen.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pameobeo.kitchen.domain.exception.InvalidImageException;
import com.pameobeo.kitchen.domain.exception.TooManyImagesException;
import com.pameobeo.kitchen.domain.model.MenuItemImage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MenuItemImageTest {

    private static final UUID ITEM = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-29T08:00:00Z");
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);

    @Test
    void detectsTheTypeFromTheFileContent() {
        assertThat(MenuItemImage.upload(ITEM, 0, JPEG, NOW).contentType()).isEqualTo("image/jpeg");
        assertThat(MenuItemImage.upload(ITEM, 0, PNG, NOW).contentType()).isEqualTo("image/png");
        assertThat(MenuItemImage.upload(ITEM, 0, WEBP, NOW).contentType()).isEqualTo("image/webp");
    }

    @Test
    void rejectsAFileThatIsNotAnImage() {
        byte[] script = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> MenuItemImage.upload(ITEM, 0, script, NOW)).isInstanceOf(InvalidImageException.class);
        assertThatThrownBy(() -> MenuItemImage.upload(ITEM, 0, new byte[0], NOW))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void rejectsAnImageAbove2Mb() {
        byte[] big = new byte[MenuItemImage.MAX_BYTES + 1];
        System.arraycopy(JPEG, 0, big, 0, JPEG.length);

        assertThatThrownBy(() -> MenuItemImage.upload(ITEM, 0, big, NOW)).isInstanceOf(InvalidImageException.class);
    }

    @Test
    void atMostThreeImagesAndANewOneGoesLast() {
        assertThat(MenuItemImage.upload(ITEM, 2, JPEG, NOW).position()).isEqualTo(2);
        assertThatThrownBy(() -> MenuItemImage.upload(ITEM, 3, JPEG, NOW)).isInstanceOf(TooManyImagesException.class);
    }
}
