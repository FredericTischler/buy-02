package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductReviewStats;
import com.ecommerce.product.dto.ReviewRequest;
import com.ecommerce.product.dto.ReviewResponse;
import com.ecommerce.product.security.JwtAuthenticationFilter;
import com.ecommerce.product.security.JwtUtil;
import com.ecommerce.product.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    MongoRepositoriesAutoConfiguration.class
})
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestMongoConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.data.mongodb.core.mapping.MongoMappingContext mongoMappingContext() {
            return new org.springframework.data.mongodb.core.mapping.MongoMappingContext();
        }
    }

    private static final String USER_ID = "user-123";
    private static final String USER_NAME = "John Doe";
    private static final String AUTH_HEADER = "Bearer test-token";
    private static final String PRODUCT_ID = "product-1";

    @BeforeEach
    void setUp() {
        Mockito.reset(reviewService, jwtUtil);
        Mockito.when(jwtUtil.extractUserId("test-token")).thenReturn(USER_ID);
        Mockito.when(jwtUtil.extractName("test-token")).thenReturn(USER_NAME);
    }

    // ==================== CREATE REVIEW TESTS ====================

    @Test
    void createReview_shouldReturnCreated() throws Exception {
        ReviewRequest request = new ReviewRequest(5, "Great", "Excellent product");
        ReviewResponse response = createSampleReviewResponse();

        Mockito.when(reviewService.createReview(eq(PRODUCT_ID), any(ReviewRequest.class), eq(USER_ID), eq(USER_NAME)))
                .thenReturn(response);

        mockMvc.perform(post("/api/reviews/product/" + PRODUCT_ID)
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("review-1"))
            .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void createReview_shouldReturnErrorWhenServiceThrows() throws Exception {
        ReviewRequest request = new ReviewRequest(5, "Great", "Excellent product");

        Mockito.when(reviewService.createReview(eq(PRODUCT_ID), any(ReviewRequest.class), eq(USER_ID), eq(USER_NAME)))
                .thenThrow(new RuntimeException("You have already reviewed this product"));

        mockMvc.perform(post("/api/reviews/product/" + PRODUCT_ID)
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is4xxClientError());
    }

    // ==================== GET PRODUCT REVIEWS TESTS ====================

    @Test
    void getProductReviews_shouldReturnReviews() throws Exception {
        List<ReviewResponse> reviews = List.of(createSampleReviewResponse());
        Mockito.when(reviewService.getReviewsByProduct(PRODUCT_ID)).thenReturn(reviews);

        mockMvc.perform(get("/api/reviews/product/" + PRODUCT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("review-1"))
            .andExpect(jsonPath("$[0].rating").value(5));
    }

    @Test
    void getProductReviews_shouldReturnEmptyList() throws Exception {
        Mockito.when(reviewService.getReviewsByProduct(PRODUCT_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/reviews/product/" + PRODUCT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    // ==================== GET PRODUCT STATS TESTS ====================

    @Test
    void getProductStats_shouldReturnStats() throws Exception {
        ProductReviewStats stats = new ProductReviewStats(
                PRODUCT_ID, 4.5, 10L,
                Map.of(5, 5L, 4, 3L, 3, 1L, 2, 1L, 1, 0L)
        );
        Mockito.when(reviewService.getProductStats(PRODUCT_ID)).thenReturn(stats);

        mockMvc.perform(get("/api/reviews/product/" + PRODUCT_ID + "/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.averageRating").value(4.5))
            .andExpect(jsonPath("$.totalReviews").value(10));
    }

    // ==================== GET MY REVIEWS TESTS ====================

    @Test
    void getMyReviews_shouldReturnUserReviews() throws Exception {
        List<ReviewResponse> reviews = List.of(createSampleReviewResponse());
        Mockito.when(reviewService.getReviewsByUser(USER_ID)).thenReturn(reviews);

        mockMvc.perform(get("/api/reviews/my-reviews")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("review-1"));
    }

    // ==================== GET REVIEW BY ID TESTS ====================

    @Test
    void getReview_shouldReturnReview() throws Exception {
        ReviewResponse response = createSampleReviewResponse();
        Mockito.when(reviewService.getReviewById("review-1")).thenReturn(response);

        mockMvc.perform(get("/api/reviews/review-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("review-1"))
            .andExpect(jsonPath("$.rating").value(5));
    }

    // ==================== UPDATE REVIEW TESTS ====================

    @Test
    void updateReview_shouldReturnUpdatedReview() throws Exception {
        ReviewRequest request = new ReviewRequest(4, "Updated", "Better now");
        ReviewResponse response = createSampleReviewResponse();
        response.setRating(4);

        Mockito.when(reviewService.updateReview(eq("review-1"), any(ReviewRequest.class), eq(USER_ID)))
                .thenReturn(response);

        mockMvc.perform(put("/api/reviews/review-1")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rating").value(4));
    }

    // ==================== DELETE REVIEW TESTS ====================

    @Test
    void deleteReview_shouldReturnSuccessMessage() throws Exception {
        doNothing().when(reviewService).deleteReview("review-1", USER_ID);

        mockMvc.perform(delete("/api/reviews/review-1")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Review deleted successfully"));
    }

    // ==================== CAN REVIEW TESTS ====================

    @Test
    void canReview_shouldReturnTrue() throws Exception {
        Mockito.when(reviewService.canUserReview(PRODUCT_ID, USER_ID)).thenReturn(true);

        mockMvc.perform(get("/api/reviews/product/" + PRODUCT_ID + "/can-review")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canReview").value(true));
    }

    @Test
    void canReview_shouldReturnFalse() throws Exception {
        Mockito.when(reviewService.canUserReview(PRODUCT_ID, USER_ID)).thenReturn(false);

        mockMvc.perform(get("/api/reviews/product/" + PRODUCT_ID + "/can-review")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canReview").value(false));
    }

    // ==================== GET MY REVIEW FOR PRODUCT TESTS ====================

    @Test
    void getMyReviewForProduct_shouldReturnReview() throws Exception {
        ReviewResponse response = createSampleReviewResponse();
        Mockito.when(reviewService.getUserReviewForProduct(PRODUCT_ID, USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/reviews/product/" + PRODUCT_ID + "/my-review")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("review-1"));
    }

    @Test
    void getMyReviewForProduct_shouldReturn404WhenNoReview() throws Exception {
        Mockito.when(reviewService.getUserReviewForProduct(PRODUCT_ID, USER_ID)).thenReturn(null);

        mockMvc.perform(get("/api/reviews/product/" + PRODUCT_ID + "/my-review")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isNotFound());
    }

    // ==================== HELPER METHODS ====================

    private ReviewResponse createSampleReviewResponse() {
        ReviewResponse response = new ReviewResponse();
        response.setId("review-1");
        response.setProductId(PRODUCT_ID);
        response.setUserId(USER_ID);
        response.setUserName(USER_NAME);
        response.setRating(5);
        response.setTitle("Great");
        response.setComment("Excellent product");
        response.setVerified(false);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }
}