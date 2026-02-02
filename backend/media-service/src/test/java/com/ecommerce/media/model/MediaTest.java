package com.ecommerce.media.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MediaTest {

    @Test
    void customConstructorShouldPopulateMandatoryFields() {
        Media media = new Media("product-1", "file.png", "image/png", 42L, "seller-1", "/media/file.png");

        assertThat(media)
            .extracting(Media::getProductId, Media::getUrl)
            .containsExactly("product-1", "/media/file.png");
        assertThat(media.getUploadedAt()).isNotNull();
    }

    @Test
    void allArgsConstructorShouldExposeValues() {
        LocalDateTime now = LocalDateTime.now();
        Media media = new Media("id", "product-2", "file.webp", "image/webp", 128L, "seller-2", "/media/file.webp", now);

        assertThat(media)
            .extracting(Media::getId, Media::getFilename, Media::getUploadedAt)
            .containsExactly("id", "file.webp", now);
    }

    @Test
    void equalsAndHashCodeShouldUseIdentifiers() {
        LocalDateTime now = LocalDateTime.now();
        Media first = new Media("id", "product-1", "file.png", "image/png", 42L, "seller", "/media/file.png", now);
        Media second = new Media("id", "product-1", "file.png", "image/png", 42L, "seller", "/media/file.png", now);

        assertThat(first)
            .isEqualTo(second)
            .hasSameHashCodeAs(second);
        assertThat(first.toString()).contains("product-1");

        second.setFilename("other.png");
        assertThat(first).isNotEqualTo(second);
    }
}
