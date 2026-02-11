package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductEvent;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KafkaTemplate<String, ProductEvent> kafkaTemplate;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, kafkaTemplate);
        ReflectionTestUtils.setField(productService, "productEventsTopic", "product-events");
    }

    @Test
    void createProduct_shouldPersistEntityAndPublishEvent() {
        ProductRequest request = new ProductRequest(
                "Phone",
                "Flagship phone",
                999.99,
                "Tech",
                5
        );

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId("product-id");
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());
            return product;
        });

        ProductResponse response = productService.createProduct(request, "seller-1", "Alice");

        assertThat(response.getId()).isEqualTo("product-id");
        assertThat(response.getSellerId()).isEqualTo("seller-1");
        verify(productRepository).save(any(Product.class));

        ArgumentCaptor<ProductEvent> eventCaptor = ArgumentCaptor.forClass(ProductEvent.class);
        verify(kafkaTemplate).send(eq("product-events"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("CREATED");
        assertThat(eventCaptor.getValue().getProductId()).isEqualTo("product-id");
    }

    @Test
    void updateProduct_shouldThrowWhenCallerIsNotOwner() {
        Product existing = new Product();
        existing.setId("product-1");
        existing.setSellerId("owner-1");

        when(productRepository.findById("product-1")).thenReturn(Optional.of(existing));

        ProductRequest request = new ProductRequest("name", "desc", 10.0, "cat", 2);

        assertThatThrownBy(() -> productService.updateProduct("product-1", request, "other-user"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authorized");

        verify(productRepository, never()).save(any(Product.class));
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    void deleteProduct_shouldRemoveEntityAndSendDeletedEvent() {
        Product existing = new Product();
        existing.setId("product-1");
        existing.setSellerId("owner-1");
        existing.setName("Old name");

        when(productRepository.findById("product-1")).thenReturn(Optional.of(existing));

        productService.deleteProduct("product-1", "owner-1");

        verify(productRepository).delete(existing);
        ArgumentCaptor<ProductEvent> eventCaptor = ArgumentCaptor.forClass(ProductEvent.class);
        verify(kafkaTemplate).send(eq("product-events"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("DELETED");
        assertThat(eventCaptor.getValue().getProductId()).isEqualTo("product-1");
    }

    @Test
    void getAllProducts_shouldMapEntitiesToResponses() {
        Product p = new Product();
        p.setId("id");
        when(productRepository.findAll()).thenReturn(List.of(p));

        List<ProductResponse> responses = productService.getAllProducts();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo("id");
    }

    @Test
    void getProductById_shouldReturnOptionalResponse() {
        Product p = new Product();
        p.setId("id");
        when(productRepository.findById("id")).thenReturn(Optional.of(p));

        Optional<ProductResponse> response = productService.getProductById("id");

        assertThat(response).isPresent();
        assertThat(response.get().getId()).isEqualTo("id");
    }

    @Test
    void updateProduct_shouldSaveWhenOwnerMatches() {
        Product existing = new Product();
        existing.setId("product-1");
        existing.setSellerId("owner-1");
        when(productRepository.findById("product-1")).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductRequest request = new ProductRequest("name", "desc", 10.0, "cat", 2);

        ProductResponse response = productService.updateProduct("product-1", request, "owner-1");

        assertThat(response.getName()).isEqualTo("name");
        verify(kafkaTemplate).send(eq("product-events"), any(ProductEvent.class));
    }

    @Test
    void deleteProduct_shouldThrowWhenCallerIsNotOwner() {
        Product product = new Product();
        product.setId("product-1");
        product.setSellerId("owner-1");
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.deleteProduct("product-1", "another"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not authorized");

        verify(productRepository, never()).delete(any(Product.class));
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    void updateProduct_shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());
        ProductRequest request = new ProductRequest("Phone", "Desc", 10.0, "Tech", 1);

        assertThatThrownBy(() -> productService.updateProduct("missing", request, "seller"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Product not found");
    }

    @Test
    void getProductsBySeller_shouldMapResults() {
        Product product = new Product();
        product.setId("product-1");
        product.setSellerId("seller-1");
        when(productRepository.findBySellerId("seller-1")).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.getProductsBySeller("seller-1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo("product-1");
    }

    @Test
    void getProductsByCategory_shouldReturnResponses() {
        Product product = new Product();
        product.setId("product-2");
        product.setCategory("tech");
        when(productRepository.findByCategory("tech")).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.getProductsByCategory("tech");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCategory()).isEqualTo("tech");
    }

    @Test
    void searchProducts_shouldDelegateToRepository() {
        Product product = new Product();
        product.setId("product-3");
        when(productRepository.findByNameContainingIgnoreCase("phone")).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.searchProducts("phone");

        assertThat(responses).hasSize(1);
        verify(productRepository).findByNameContainingIgnoreCase("phone");
    }

    // ==================== PAGINATION & ADVANCED SEARCH TESTS ====================

    @Test
    void getAllProductsPaginated_shouldReturnPagedResults() {
        Product product = new Product();
        product.setId("product-1");
        product.setName("Test Product");
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<ProductResponse> result = productService.getAllProductsPaginated(0, 10, "name", "asc");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("product-1");
        verify(productRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAllProductsPaginated_shouldSortDescending() {
        Page<Product> page = new PageImpl<>(List.of());
        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

        productService.getAllProductsPaginated(0, 10, "price", "desc");

        verify(productRepository).findAll(any(Pageable.class));
    }

    @Test
    void searchProductsAdvanced_shouldSearchWithKeyword() {
        Product product = new Product();
        product.setId("product-1");
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.searchWithFilters(anyString(), anyDouble(), anyDouble(), anyInt(), any(Pageable.class)))
            .thenReturn(page);

        Page<ProductResponse> result = productService.searchProductsAdvanced(
            "phone", null, 10.0, 1000.0, 1, 0, 10, "name", "asc");

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).searchWithFilters(eq("phone"), eq(10.0), eq(1000.0), eq(1), any(Pageable.class));
    }

    @Test
    void searchProductsAdvanced_shouldFilterByCategory() {
        Product product = new Product();
        product.setId("product-1");
        product.setCategory("Electronics");
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findByCategoryWithFilters(anyString(), anyDouble(), anyDouble(), anyInt(), any(Pageable.class)))
            .thenReturn(page);

        Page<ProductResponse> result = productService.searchProductsAdvanced(
            null, "Electronics", null, null, null, 0, 10, "name", "asc");

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).findByCategoryWithFilters(eq("Electronics"), eq(0.0), eq(Double.MAX_VALUE), eq(0), any(Pageable.class));
    }

    @Test
    void searchProductsAdvanced_shouldUseDefaultsForNullValues() {
        Page<Product> page = new PageImpl<>(List.of());
        when(productRepository.searchWithFilters(anyString(), anyDouble(), anyDouble(), anyInt(), any(Pageable.class)))
            .thenReturn(page);

        productService.searchProductsAdvanced(null, null, null, null, null, 0, 10, "name", "desc");

        verify(productRepository).searchWithFilters(eq(".*"), eq(0.0), eq(Double.MAX_VALUE), eq(0), any(Pageable.class));
    }

    @Test
    void searchProductsAdvanced_shouldHandleEmptyKeyword() {
        Page<Product> page = new PageImpl<>(List.of());
        when(productRepository.searchWithFilters(anyString(), anyDouble(), anyDouble(), anyInt(), any(Pageable.class)))
            .thenReturn(page);

        productService.searchProductsAdvanced("", null, null, null, null, 0, 10, "name", "asc");

        verify(productRepository).searchWithFilters(eq(".*"), anyDouble(), anyDouble(), anyInt(), any(Pageable.class));
    }

    @Test
    void getAvailableProducts_shouldReturnProductsWithStock() {
        Product product = new Product();
        product.setId("product-1");
        product.setStock(10);
        when(productRepository.findByStockGreaterThan(0)).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getAvailableProducts();

        assertThat(result).hasSize(1);
        verify(productRepository).findByStockGreaterThan(0);
    }

    @Test
    void getAvailableProducts_shouldReturnEmptyListWhenNoStock() {
        when(productRepository.findByStockGreaterThan(0)).thenReturn(List.of());

        List<ProductResponse> result = productService.getAvailableProducts();

        assertThat(result).isEmpty();
    }

    @Test
    void getProductsByPriceRange_shouldFilterByPrice() {
        Product product = new Product();
        product.setId("product-1");
        product.setPrice(50.0);
        when(productRepository.findByPriceBetween(10.0, 100.0)).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getProductsByPriceRange(10.0, 100.0);

        assertThat(result).hasSize(1);
        verify(productRepository).findByPriceBetween(10.0, 100.0);
    }

    @Test
    void getProductsByPriceRange_shouldReturnEmptyListWhenNoMatch() {
        when(productRepository.findByPriceBetween(1000.0, 2000.0)).thenReturn(List.of());

        List<ProductResponse> result = productService.getProductsByPriceRange(1000.0, 2000.0);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteProduct_shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct("missing", "seller"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Product not found");
    }
}
