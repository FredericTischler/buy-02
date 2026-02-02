package com.ecommerce.media.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {
    private String id;
    private String productId;
    private String filename;
    private String contentType;
    private Long size;
    private String uploadedBy;
    private String url;
    private LocalDateTime uploadedAt;
}
