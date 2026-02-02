package com.ecommerce.media.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "media")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Media {
    @Id
    private String id;
    
    private String productId;
    private String filename;
    private String contentType;
    private Long size;
    private String uploadedBy; // sellerId
    private String url;
    private LocalDateTime uploadedAt;
    
    public Media(String productId, String filename, String contentType, Long size, String uploadedBy, String url) {
        this.productId = productId;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.uploadedBy = uploadedBy;
        this.url = url;
        this.uploadedAt = LocalDateTime.now();
    }
}
