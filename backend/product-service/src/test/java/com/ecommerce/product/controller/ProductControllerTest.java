package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.security.JwtAuthenticationFilter;
import com.ecommerce.product.service.ProductService;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    MongoRepositoriesAutoConfiguration.class
})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestMongoConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.data.mongodb.core.mapping.MongoMappingContext mongoMappingContext() {
            return new org.springframework.data.mongodb.core.mapping.MongoMappingContext();
        }
    }

    private ProductResponse sampleResponse() {
        return new ProductResponse("id", "Phone", "Desc", 10.0, "Tech", 5,
            "seller-1", "Alice", null, null);
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(productService);
    }

    @Test
    void getAllProducts_shouldReturnList() throws Exception {
        Mockito.when(productService.getAllProducts()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Phone"));
    }

    @Test
    void getProductById_shouldReturn404WhenMissing() throws Exception {
        Mockito.when(productService.getProductById("id")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/id"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Product not found"));
    }

    @Test
    void getProductById_shouldReturnProductWhenPresent() throws Exception {
        Mockito.when(productService.getProductById("id")).thenReturn(Optional.of(sampleResponse()));

        mockMvc.perform(get("/api/products/id"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("id"));
    }

    @Test
    void createProduct_shouldReturnCreatedProduct() throws Exception {
        ProductRequest request = new ProductRequest("Phone", "Desc", 10.0, "Tech", 5);
        Mockito.when(productService.createProduct(any(), eq("seller-1"), eq("Alice")))
            .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", "seller-1")
                .requestAttr("userName", "Alice"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sellerId").value("seller-1"));
    }

    @Test
    void createProduct_shouldReturnBadRequestWhenServiceThrows() throws Exception {
        ProductRequest request = new ProductRequest("Phone", "Desc", 10.0, "Tech", 5);
        Mockito.when(productService.createProduct(any(), any(), any()))
            .thenThrow(new RuntimeException("Invalid data"));

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", "seller-1")
                .requestAttr("userName", "Alice"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid data"));
    }

    @Test
    void updateProduct_shouldReturnErrorWhenServiceThrows() throws Exception {
        ProductRequest request = new ProductRequest("Phone", "Desc", 10.0, "Tech", 5);
        Mockito.when(productService.updateProduct(eq("id"), any(), eq("seller-1")))
            .thenThrow(new RuntimeException("not authorized"));

        mockMvc.perform(put("/api/products/id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("not authorized"));
    }

    @Test
    void updateProduct_shouldReturnUpdatedProduct() throws Exception {
        ProductRequest request = new ProductRequest("Phone", "Desc", 10.0, "Tech", 5);
        Mockito.when(productService.updateProduct(eq("id"), any(), eq("seller-1")))
            .thenReturn(sampleResponse());

        mockMvc.perform(put("/api/products/id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("id"));
    }

    @Test
    void deleteProduct_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/products/id").requestAttr("userId", "seller-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Product deleted successfully"));
        Mockito.verify(productService).deleteProduct("id", "seller-1");
    }

    @Test
    void deleteProduct_shouldReturnForbiddenWhenNotOwner() throws Exception {
        Mockito.doThrow(new RuntimeException("You are not authorized to delete this product"))
            .when(productService).deleteProduct("id", "seller-1");

        mockMvc.perform(delete("/api/products/id").requestAttr("userId", "seller-1"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("You are not authorized to delete this product"));
    }

    @Test
    void deleteProduct_shouldReturnNotFoundWhenMissing() throws Exception {
        Mockito.doThrow(new RuntimeException("Product not found"))
            .when(productService).deleteProduct("id", "seller-1");

        mockMvc.perform(delete("/api/products/id").requestAttr("userId", "seller-1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Product not found"));
    }

    @Test
    void searchProducts_shouldReturnResults() throws Exception {
        Mockito.when(productService.searchProducts("phone")).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/products/search").param("keyword", "phone"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("id"));
    }

    @Test
    void getProductsByCategory_shouldReturnResults() throws Exception {
        Mockito.when(productService.getProductsByCategory("tech")).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/products/category/tech"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].category").value("Tech"));
    }

    @Test
    void getMyProducts_shouldUseUserIdAttribute() throws Exception {
        Mockito.when(productService.getProductsBySeller("seller-1")).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/products/seller/my-products").requestAttr("userId", "seller-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sellerId").value("seller-1"));
    }

    // ==================== PAGINATION ENDPOINT TESTS ====================

    @Test
    void getProductsPaginated_shouldReturnPagedResults() throws Exception {
        List<ProductResponse> content = new ArrayList<>();
        content.add(sampleResponse());
        Pageable pageable = PageRequest.of(0, 12);
        Page<ProductResponse> page = new PageImpl<>(content, pageable, 1);
        Mockito.doReturn(page).when(productService).getAllProductsPaginated(
            Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyString());

        mockMvc.perform(get("/api/products/paginated"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("id"))
            .andExpect(jsonPath("$.content[0].name").value("Phone"));
    }

    @Test
    void getProductsPaginated_shouldAcceptCustomParameters() throws Exception {
        List<ProductResponse> content = new ArrayList<>();
        content.add(sampleResponse());
        Pageable pageable = PageRequest.of(1, 20);
        Page<ProductResponse> page = new PageImpl<>(content, pageable, 1);
        Mockito.doReturn(page).when(productService).getAllProductsPaginated(
            Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyString());

        mockMvc.perform(get("/api/products/paginated")
                .param("page", "1")
                .param("size", "20")
                .param("sortBy", "price")
                .param("sortDir", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void filterProducts_shouldReturnFilteredResults() throws Exception {
        List<ProductResponse> content = new ArrayList<>();
        content.add(sampleResponse());
        Pageable pageable = PageRequest.of(0, 12);
        Page<ProductResponse> page = new PageImpl<>(content, pageable, 1);
        Mockito.doReturn(page).when(productService).searchProductsAdvanced(
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyString());

        mockMvc.perform(get("/api/products/filter"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("id"));
    }

    @Test
    void filterProducts_shouldAcceptAllFilterParameters() throws Exception {
        List<ProductResponse> content = new ArrayList<>();
        content.add(sampleResponse());
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductResponse> page = new PageImpl<>(content, pageable, 1);
        Mockito.doReturn(page).when(productService).searchProductsAdvanced(
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyString());

        mockMvc.perform(get("/api/products/filter")
                .param("keyword", "phone")
                .param("category", "Tech")
                .param("minPrice", "10.0")
                .param("maxPrice", "100.0")
                .param("minStock", "1")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "price")
                .param("sortDir", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].category").value("Tech"));
    }

    @Test
    void filterProducts_shouldHandleEmptyResults() throws Exception {
        List<ProductResponse> content = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, 12);
        Page<ProductResponse> emptyPage = new PageImpl<>(content, pageable, 0);
        Mockito.doReturn(emptyPage).when(productService).searchProductsAdvanced(
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyString());

        mockMvc.perform(get("/api/products/filter")
                .param("keyword", "nonexistent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getAvailableProducts_shouldReturnProductsInStock() throws Exception {
        Mockito.when(productService.getAvailableProducts()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/products/available"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("id"))
            .andExpect(jsonPath("$[0].stock").value(5));
    }

    @Test
    void getAvailableProducts_shouldReturnEmptyListWhenNoStock() throws Exception {
        Mockito.when(productService.getAvailableProducts()).thenReturn(List.of());

        mockMvc.perform(get("/api/products/available"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void updateProduct_shouldReturnNotFoundWhenProductMissing() throws Exception {
        ProductRequest request = new ProductRequest("Phone", "Desc", 10.0, "Tech", 5);
        Mockito.when(productService.updateProduct(eq("missing"), any(), eq("seller-1")))
            .thenThrow(new RuntimeException("Product not found"));

        mockMvc.perform(put("/api/products/missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", "seller-1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Product not found"));
    }
}
