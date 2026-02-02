package com.ecommerce.media.controller;

import com.ecommerce.media.dto.MediaResponse;
import com.ecommerce.media.security.JwtAuthenticationFilter;
import com.ecommerce.media.service.MediaService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    MongoRepositoriesAutoConfiguration.class
})
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestMongoConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.data.mongodb.core.mapping.MongoMappingContext mongoMappingContext() {
            return new org.springframework.data.mongodb.core.mapping.MongoMappingContext();
        }
    }

    private MediaResponse sampleResponse() {
        return new MediaResponse("id", "product", "file.png", "image/png", 1024L,
            "seller", "/api/media/file/product/file.png", LocalDateTime.now());
    }

    @Test
    void uploadMedia_shouldReturnCreatedResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "data".getBytes());
        Mockito.when(mediaService.uploadMedia(any(), any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(multipart("/api/media/upload")
                .file(file)
                .param("productId", "product")
                .requestAttr("userId", "seller"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("id"));
    }

    @Test
    void uploadMedia_shouldReturnUnauthorizedWhenUserIdMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "data".getBytes());

        mockMvc.perform(multipart("/api/media/upload")
                .file(file)
                .param("productId", "product"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void uploadMedia_shouldReturnBadRequestOnValidationError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "data".getBytes());
        Mockito.when(mediaService.uploadMedia(any(), any(), any())).thenThrow(new IllegalArgumentException("invalid"));

        mockMvc.perform(multipart("/api/media/upload")
                .file(file)
                .param("productId", "product")
                .requestAttr("userId", "seller"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid"));
    }

    @Test
    void uploadMedia_shouldReturnServerErrorOnIOException() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "data".getBytes());
        Mockito.when(mediaService.uploadMedia(any(), any(), any())).thenThrow(new IOException("io"));

        mockMvc.perform(multipart("/api/media/upload")
                .file(file)
                .param("productId", "product")
                .requestAttr("userId", "seller"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void uploadMedia_shouldHandleGenericExceptions() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "data".getBytes());
        Mockito.when(mediaService.uploadMedia(any(), any(), any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(multipart("/api/media/upload")
                .file(file)
                .param("productId", "product")
                .requestAttr("userId", "seller"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("boom")));
    }

    @Test
    void getMediaByProduct_shouldReturnList() throws Exception {
        Mockito.when(mediaService.getMediaByProductId("product")).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/media/product/product"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].filename").value("file.png"));
    }

    @Test
    void downloadFile_shouldStreamResource() throws Exception {
        ByteArrayResource resource = new ByteArrayResource("data".getBytes());
        Mockito.when(mediaService.getMediaFile("product", "file.png")).thenReturn(resource);

        mockMvc.perform(get("/api/media/file/product/file.png"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("file.png")));
    }

    @Test
    void downloadFile_shouldReturnNotFoundOnIOException() throws Exception {
        Mockito.when(mediaService.getMediaFile("product", "file.png")).thenThrow(new IOException("missing"));

        mockMvc.perform(get("/api/media/file/product/file.png"))
            .andExpect(status().isNotFound());
    }

    @Test
    void downloadFile_shouldSetJpegContentType() throws Exception {
        ByteArrayResource resource = new ByteArrayResource("data".getBytes());
        Mockito.when(mediaService.getMediaFile("product", "file.jpg")).thenReturn(resource);

        mockMvc.perform(get("/api/media/file/product/file.jpg"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/jpeg"));
    }

    @Test
    void downloadFile_shouldSetWebpContentType() throws Exception {
        ByteArrayResource resource = new ByteArrayResource("data".getBytes());
        Mockito.when(mediaService.getMediaFile("product", "file.webp")).thenReturn(resource);

        mockMvc.perform(get("/api/media/file/product/file.webp"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/webp"));
    }

    @Test
    void deleteMedia_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/media/id").requestAttr("userId", "seller"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Média supprimé avec succès"));
        Mockito.verify(mediaService).deleteMedia("id", "seller");
    }

    @Test
    void deleteMedia_shouldReturnForbiddenWhenUnauthorized() throws Exception {
        doThrow(new IllegalArgumentException("not allowed"))
            .when(mediaService).deleteMedia("id", "seller");

        mockMvc.perform(delete("/api/media/id").requestAttr("userId", "seller"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("not allowed"));
    }

    @Test
    void deleteMedia_shouldReturnServerErrorOnIOException() throws Exception {
        doThrow(new IOException("io")).when(mediaService).deleteMedia("id", "seller");

        mockMvc.perform(delete("/api/media/id").requestAttr("userId", "seller"))
            .andExpect(status().isInternalServerError());
    }
}
