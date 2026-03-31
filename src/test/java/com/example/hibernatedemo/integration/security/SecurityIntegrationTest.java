package com.example.hibernatedemo.integration.security;

import com.example.hibernatedemo.application.dto.CreateOrderRequest;
import com.example.hibernatedemo.application.dto.LoginRequest;
import com.example.hibernatedemo.application.dto.LoginResponse;
import com.example.hibernatedemo.application.dto.RegisterRequest;
import com.example.hibernatedemo.domain.enums.OrderStatus;
import com.example.hibernatedemo.domain.enums.Role;
import com.example.hibernatedemo.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private String baseUrl;
    private String adminToken;
    private String userToken;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        userRepository.deleteAll();
        restClient = RestClient.builder().baseUrl(baseUrl).build();
        registerAndLoginUsers();
    }

    private void registerAndLoginUsers() {
        RegisterRequest adminReq = RegisterRequest.builder()
                .username("admin")
                .password("admin123")
                .roles(Set.of(Role.ROLE_ADMIN, Role.ROLE_USER))
                .build();
        restTemplate.postForEntity(baseUrl + "/api/auth/register", adminReq, String.class);

        RegisterRequest userReq = RegisterRequest.builder()
                .username("regularuser")
                .password("user123")
                .roles(Set.of(Role.ROLE_USER))
                .build();
        restTemplate.postForEntity(baseUrl + "/api/auth/register", userReq, String.class);

        LoginResponse adminLogin = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                new LoginRequest("admin", "admin123"),
                LoginResponse.class).getBody();
        adminToken = adminLogin.getToken();

        LoginResponse userLogin = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                new LoginRequest("regularuser", "user123"),
                LoginResponse.class).getBody();
        userToken = userLogin.getToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @Order(1)
    void unauthenticatedRequest_shouldReturn401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/api/orders", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @Order(2)
    void user_canReadOrders() {
        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(
                CreateOrderRequest.builder()
                        .customerName("Test User")
                        .orderNumber("SEC-001")
                        .status(OrderStatus.PENDING)
                        .totalAmount(new BigDecimal("50.00"))
                        .build(),
                authHeaders(adminToken));

        restTemplate.postForEntity(baseUrl + "/api/orders", entity, String.class);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/orders",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @Order(3)
    void user_cannotDeleteOrders_shouldReturn403() {
        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(
                CreateOrderRequest.builder()
                        .customerName("Test User")
                        .orderNumber("SEC-002")
                        .status(OrderStatus.PENDING)
                        .totalAmount(new BigDecimal("50.00"))
                        .build(),
                authHeaders(adminToken));

        var created = restTemplate.postForEntity(baseUrl + "/api/orders", entity, Map.class);
        Long orderId = ((Number) created.getBody().get("id")).longValue();

        try {
            restClient.delete()
                    .uri("/api/orders/" + orderId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .retrieve()
                    .toBodilessEntity();
            fail("Expected 403 FORBIDDEN");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        }
    }

    @Test
    @Order(4)
    void admin_canDeleteOrders() {
        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(
                CreateOrderRequest.builder()
                        .customerName("Test User")
                        .orderNumber("SEC-003")
                        .status(OrderStatus.PENDING)
                        .totalAmount(new BigDecimal("50.00"))
                        .build(),
                authHeaders(adminToken));

        var created = restTemplate.postForEntity(baseUrl + "/api/orders", entity, Map.class);
        Long orderId = ((Number) created.getBody().get("id")).longValue();

        ResponseEntity<Void> response = restClient.delete()
                .uri("/api/orders/" + orderId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .toBodilessEntity();

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    @Order(5)
    void user_cannotCreateProducts_shouldReturn403() {
        Map<String, Object> product = Map.of(
                "name", "Test Product",
                "sku", "SKU-001",
                "price", 10.0,
                "stockQuantity", 100,
                "active", true);

        try {
            restClient.post()
                    .uri("/api/products")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(product)
                    .retrieve()
                    .toBodilessEntity();
            fail("Expected 403 FORBIDDEN");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        }
    }

    @Test
    @Order(6)
    void admin_canCreateProducts() {
        Map<String, Object> product = Map.of(
                "name", "Admin Product",
                "sku", "SKU-ADMIN",
                "price", 25.0,
                "stockQuantity", 50,
                "active", true);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/products",
                HttpMethod.POST,
                new HttpEntity<>(product, authHeaders(adminToken)),
                String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @Order(7)
    void user_canReadProducts() {
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/products",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @Order(8)
    void invalidToken_shouldReturn401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid.token.here");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/orders",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @Order(9)
    void noToken_shouldReturn401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/api/products/1", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @Order(10)
    void authEndpoints_shouldBePublic() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .password("newpass123")
                .roles(Set.of(Role.ROLE_USER))
                .build();

        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                baseUrl + "/api/auth/register", request, String.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                new LoginRequest("newuser", "newpass123"),
                LoginResponse.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody().getToken());
    }
}
