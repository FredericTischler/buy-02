package com.ecommerce.media.service;

import com.ecommerce.media.dto.MediaResponse;
import com.ecommerce.media.model.Media;
import com.ecommerce.media.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @TempDir
    Path tempDir;

    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        mediaService = new MediaService();
        ReflectionTestUtils.setField(mediaService, "mediaRepository", mediaRepository);
        ReflectionTestUtils.setField(mediaService, "uploadDirectory", tempDir.toString());
    }

    @Test
    void uploadMedia_shouldStoreFileAndPersistMetadata() throws Exception {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getOriginalFilename()).thenReturn("product.png");
        InputStream stream = new ByteArrayInputStream("fake-image".getBytes());
        when(multipartFile.getInputStream()).thenReturn(stream);

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> {
            Media media = invocation.getArgument(0);
            media.setId("media-id");
            return media;
        });

        MediaResponse response = mediaService.uploadMedia(multipartFile, "product-123", "seller-456");

        assertThat(response.getId()).isEqualTo("media-id");
        assertThat(response.getProductId()).isEqualTo("product-123");

        ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(mediaCaptor.capture());
        Media persisted = mediaCaptor.getValue();
        Path storedFile = tempDir.resolve("product-123").resolve(persisted.getFilename());
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(persisted.getUploadedBy()).isEqualTo("seller-456");
    }

    @Test
    void deleteMedia_shouldRejectWhenSellerIsNotOwner() {
        Media media = new Media();
        media.setId("media-1");
        media.setProductId("product-1");
        media.setFilename("file.png");
        media.setUploadedBy("owner-1");

        when(mediaRepository.findById("media-1")).thenReturn(Optional.of(media));

        assertThatThrownBy(() -> mediaService.deleteMedia("media-1", "another-seller"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("autorisé");

        verify(mediaRepository, never()).delete(any(Media.class));
    }

    @Test
    void deleteMedia_shouldRemoveFileAndDocumentWhenOwnerMatches() throws Exception {
        Path productDir = Files.createDirectories(tempDir.resolve("product-1"));
        Path storedFile = productDir.resolve("file.png");
        Files.writeString(storedFile, "content");

        Media media = new Media();
        media.setId("media-1");
        media.setProductId("product-1");
        media.setFilename("file.png");
        media.setUploadedBy("owner-1");

        when(mediaRepository.findById("media-1")).thenReturn(Optional.of(media));

        mediaService.deleteMedia("media-1", "owner-1");

        assertThat(Files.exists(storedFile)).isFalse();
        verify(mediaRepository).delete(media);
    }

    @Test
    void uploadMedia_shouldRejectEmptyFile() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> mediaService.uploadMedia(multipartFile, "product", "seller"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("vide");
    }

    @Test
    void uploadMedia_shouldRejectInvalidContentType() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("text/plain");

        assertThatThrownBy(() -> mediaService.uploadMedia(multipartFile, "product", "seller"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Format non autorisé");
    }

    @Test
    void uploadMedia_shouldRejectWhenFileIsTooLarge() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(5 * 1024 * 1024L);

        assertThatThrownBy(() -> mediaService.uploadMedia(multipartFile, "product", "seller"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dépasse 2 MB");
    }

    @Test
    void getMediaByProductId_shouldMapResponses() {
        Media media = new Media();
        media.setId("id");
        media.setProductId("product");
        when(mediaRepository.findByProductId("product")).thenReturn(List.of(media));

        List<MediaResponse> responses = mediaService.getMediaByProductId("product");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo("id");
    }

    @Test
    void getMediaFile_shouldReturnResource() throws Exception {
        Path productDir = Files.createDirectories(tempDir.resolve("product-1"));
        Path file = productDir.resolve("file.png");
        Files.writeString(file, "data");

        Resource resource = mediaService.getMediaFile("product-1", "file.png");

        assertThat(resource.exists()).isTrue();
        assertThat(resource.getFilename()).isEqualTo("file.png");
    }

    @Test
    void getMediaFile_shouldThrowWhenMissing() {
        assertThatThrownBy(() -> mediaService.getMediaFile("product-1", "missing.png"))
            .isInstanceOf(IOException.class);
    }

    @Test
    void deleteMedia_shouldThrowWhenMediaIsUnknown() {
        when(mediaRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.deleteMedia("missing", "seller"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Média non trouvé");

        verify(mediaRepository, never()).delete(any());
    }

    @Test
    void deleteAllByProductId_shouldDeleteFilesAndRecords() throws Exception {
        Path productDir = Files.createDirectories(tempDir.resolve("product-1"));
        Path file = productDir.resolve("file.png");
        Files.writeString(file, "content");

        Media media = new Media();
        media.setProductId("product-1");
        media.setFilename("file.png");
        when(mediaRepository.findByProductId("product-1")).thenReturn(List.of(media));

        mediaService.deleteAllByProductId("product-1");

        assertThat(Files.exists(file)).isFalse();
        assertThat(Files.exists(productDir)).isFalse();
        verify(mediaRepository).deleteAllByProductId("product-1");
    }
}
