package com.example.orderservice.integration;

import com.example.orderservice.application.dto.CreateProductRequest;
import com.example.orderservice.application.dto.ProductResponse;
import com.example.orderservice.integration.testauth.TestAuthConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(TestAuthConfig.class)
class ProductControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;
    private String adminToken;
    private static Long createdProductId;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/products";
        setupAuth();
    }

    private void setupAuth() {
        String authUrl = "http://localhost:" + port + "/api/auth";
        restTemplate.postForEntity(authUrl + "/register",
                Map.of("username", "adminuser", "password", "adminpass123", "roles", Set.of("ROLE_ADMIN", "ROLE_USER")),
                String.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> login = restTemplate.postForEntity(
                authUrl + "/login",
                Map.of("username", "adminuser", "password", "adminpass123", "roles", Set.of("ROLE_ADMIN", "ROLE_USER")),
                Map.class).getBody();
        adminToken = (String) login.get("token");
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @Order(1)
    void createProduct_shouldReturnCreatedProduct() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Test Widget")
                .sku("WIDGET-001")
                .description("A test widget for integration testing")
                .price(new BigDecimal("29.99"))
                .stockQuantity(100)
                .active(true)
                .build();

        ResponseEntity<ProductResponse> response = restTemplate.exchange(
                baseUrl, HttpMethod.POST, new HttpEntity<>(request, authHeaders()), ProductResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Widget", response.getBody().getName());
        assertEquals("WIDGET-001", response.getBody().getSku());
        assertEquals(0, new BigDecimal("29.99").compareTo(response.getBody().getPrice()));
        assertEquals(100, response.getBody().getStockQuantity());
        assertTrue(response.getBody().getActive());

        createdProductId = response.getBody().getId();
    }

    @Test
    @Order(2)
    void getProductById_shouldReturnProduct() {
        ResponseEntity<ProductResponse> response = restTemplate.exchange(
                baseUrl + "/" + createdProductId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                ProductResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(createdProductId, response.getBody().getId());
    }

    @Test
    @Order(3)
    void getProductById_notFound_shouldReturn404() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/99999",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @Order(4)
    void getAllProducts_shouldReturnPaginatedResults() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @Order(5)
    void getActiveProducts_shouldReturnOnlyActiveProducts() {
        ResponseEntity<List<ProductResponse>> response = restTemplate.exchange(
                baseUrl + "/active",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<List<ProductResponse>>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());
        response.getBody().forEach(product -> assertTrue(product.getActive()));
    }

    @Test
    @Order(6)
    void updateProduct_shouldReturnUpdatedProduct() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Updated Widget")
                .sku("WIDGET-001-UPDATED")
                .description("Updated description")
                .price(new BigDecimal("39.99"))
                .stockQuantity(50)
                .active(true)
                .build();

        ResponseEntity<ProductResponse> response = restTemplate.exchange(
                baseUrl + "/" + createdProductId,
                HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders()),
                ProductResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Widget", response.getBody().getName());
        assertEquals(0, new BigDecimal("39.99").compareTo(response.getBody().getPrice()));
    }

    @Test
    @Order(7)
    void createProduct_withInvalidData_shouldReturn400() {
        Map<String, Object> invalidRequest = Map.of(
                "name", "",
                "price", -10,
                "stockQuantity", -5
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                new HttpEntity<>(invalidRequest, authHeaders()),
                Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @Order(8)
    void deleteProduct_shouldReturn204() {
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/" + createdProductId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
