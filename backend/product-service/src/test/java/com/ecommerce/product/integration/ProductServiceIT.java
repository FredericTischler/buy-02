package com.ecommerce.product.integration;

import com.ecommerce.product.dto.ProductEvent;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceIT extends BaseIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @MockBean
    private KafkaTemplate<String, ProductEvent> kafkaTemplate;

    private static final String SELLER_ID = "seller-123";
    private static final String SELLER_NAME = "Test Seller";

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void createProduct_ShouldPersistProduct() {
        ProductRequest request = new ProductRequest("Laptop", "A nice laptop", 999.99, "Electronics", 10);

        ProductResponse response = productService.createProduct(request, SELLER_ID, SELLER_NAME);

        assertNotNull(response.getId());
        assertEquals("Laptop", response.getName());
        assertEquals(999.99, response.getPrice());
        assertEquals("Electronics", response.getCategory());
        assertEquals(SELLER_ID, response.getSellerId());
        assertEquals(SELLER_NAME, response.getSellerName());
    }

    @Test
    void getProductById_ShouldReturnProduct() {
        ProductRequest request = new ProductRequest("Phone", "Smartphone", 599.99, "Electronics", 20);
        ProductResponse created = productService.createProduct(request, SELLER_ID, SELLER_NAME);

        Optional<ProductResponse> found = productService.getProductById(created.getId());

        assertTrue(found.isPresent());
        assertEquals("Phone", found.get().getName());
    }

    @Test
    void getProductById_ShouldReturnEmptyForNonExistent() {
        Optional<ProductResponse> found = productService.getProductById("non-existent-id");
        assertTrue(found.isEmpty());
    }

    @Test
    void updateProduct_ShouldUpdateFields() {
        ProductRequest create = new ProductRequest("Old Name", "Description", 100.0, "Category", 5);
        ProductResponse created = productService.createProduct(create, SELLER_ID, SELLER_NAME);

        ProductRequest update = new ProductRequest("New Name", "Updated description", 150.0, "NewCategory", 10);
        ProductResponse updated = productService.updateProduct(created.getId(), update, SELLER_ID);

        assertEquals("New Name", updated.getName());
        assertEquals(150.0, updated.getPrice());
        assertEquals("NewCategory", updated.getCategory());
        assertEquals(10, updated.getStock());
    }

    @Test
    void updateProduct_ShouldThrowForWrongSeller() {
        ProductRequest create = new ProductRequest("Product", "Desc", 100.0, "Cat", 5);
        ProductResponse created = productService.createProduct(create, SELLER_ID, SELLER_NAME);

        ProductRequest update = new ProductRequest("Hacked", "Hacked", 0.0, "Cat", 0);

        assertThrows(RuntimeException.class, () ->
            productService.updateProduct(created.getId(), update, "other-seller")
        );
    }

    @Test
    void deleteProduct_ShouldRemoveProduct() {
        ProductRequest create = new ProductRequest("ToDelete", "Desc", 100.0, "Cat", 5);
        ProductResponse created = productService.createProduct(create, SELLER_ID, SELLER_NAME);

        productService.deleteProduct(created.getId(), SELLER_ID);

        assertTrue(productService.getProductById(created.getId()).isEmpty());
    }

    @Test
    void deleteProduct_ShouldThrowForWrongSeller() {
        ProductRequest create = new ProductRequest("Product", "Desc", 100.0, "Cat", 5);
        ProductResponse created = productService.createProduct(create, SELLER_ID, SELLER_NAME);

        assertThrows(RuntimeException.class, () ->
            productService.deleteProduct(created.getId(), "other-seller")
        );
    }

    @Test
    void getProductsByCategory_ShouldFilterCorrectly() {
        productService.createProduct(new ProductRequest("P1", "D1", 10.0, "Electronics", 5), SELLER_ID, SELLER_NAME);
        productService.createProduct(new ProductRequest("P2", "D2", 20.0, "Fashion", 5), SELLER_ID, SELLER_NAME);
        productService.createProduct(new ProductRequest("P3", "D3", 30.0, "Electronics", 5), SELLER_ID, SELLER_NAME);

        List<ProductResponse> electronics = productService.getProductsByCategory("Electronics");

        assertEquals(2, electronics.size());
        assertTrue(electronics.stream().allMatch(p -> p.getCategory().equals("Electronics")));
    }

    @Test
    void searchProducts_ShouldFindByName() {
        productService.createProduct(new ProductRequest("iPhone 15", "Apple phone", 999.0, "Electronics", 5), SELLER_ID, SELLER_NAME);
        productService.createProduct(new ProductRequest("Samsung Galaxy", "Samsung phone", 899.0, "Electronics", 5), SELLER_ID, SELLER_NAME);

        List<ProductResponse> results = productService.searchProducts("iPhone");

        assertEquals(1, results.size());
        assertEquals("iPhone 15", results.get(0).getName());
    }

    @Test
    void getAllProductsPaginated_ShouldReturnPage() {
        for (int i = 0; i < 15; i++) {
            productService.createProduct(
                new ProductRequest("Product " + i, "Desc", 10.0 + i, "Cat", 5), SELLER_ID, SELLER_NAME
            );
        }

        Page<ProductResponse> page0 = productService.getAllProductsPaginated(0, 10, "createdAt", "desc");
        Page<ProductResponse> page1 = productService.getAllProductsPaginated(1, 10, "createdAt", "desc");

        assertEquals(10, page0.getContent().size());
        assertEquals(5, page1.getContent().size());
        assertEquals(15, page0.getTotalElements());
        assertEquals(2, page0.getTotalPages());
    }

    @Test
    void searchProductsAdvanced_ShouldFilterByPriceRange() {
        productService.createProduct(new ProductRequest("Cheap", "D", 10.0, "Cat", 5), SELLER_ID, SELLER_NAME);
        productService.createProduct(new ProductRequest("Mid", "D", 50.0, "Cat", 5), SELLER_ID, SELLER_NAME);
        productService.createProduct(new ProductRequest("Expensive", "D", 200.0, "Cat", 5), SELLER_ID, SELLER_NAME);

        Page<ProductResponse> results = productService.searchProductsAdvanced(
            null, null, 20.0, 100.0, null, 0, 12, "price", "asc"
        );

        assertEquals(1, results.getTotalElements());
        assertEquals("Mid", results.getContent().get(0).getName());
    }

    @Test
    void getProductsBySeller_ShouldReturnOnlySellerProducts() {
        productService.createProduct(new ProductRequest("P1", "D1", 10.0, "Cat", 5), SELLER_ID, SELLER_NAME);
        productService.createProduct(new ProductRequest("P2", "D2", 20.0, "Cat", 5), "other-seller", "Other");

        List<ProductResponse> sellerProducts = productService.getProductsBySeller(SELLER_ID);

        assertEquals(1, sellerProducts.size());
        assertEquals(SELLER_ID, sellerProducts.get(0).getSellerId());
    }
}