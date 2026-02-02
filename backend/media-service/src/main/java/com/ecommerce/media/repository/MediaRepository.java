package com.ecommerce.media.repository;

import com.ecommerce.media.model.Media;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends MongoRepository<Media, String> {
    List<Media> findByProductId(String productId);
    List<Media> findByUploadedBy(String uploadedBy);
    void deleteAllByProductId(String productId);
}
