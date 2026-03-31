package com.example.hibernatedemo.integration;

import com.example.hibernatedemo.application.dto.*;
import com.example.hibernatedemo.domain.enums.OrderStatus;
import com.example.hibernatedemo.domain.enums.Role;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;
    private String token;
    private static Long createdOrderId;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/orders";
        setupAuth();
    }

    private void setupAuth() {
        String authUrl = "http://localhost:" + port + "/api/auth";
        RegisterRequest registerReq = RegisterRequest.builder()
                .username("testuser")
                .password("testpass123")
                .roles(Set.of(Role.ROLE_ADMIN, Role.ROLE_USER))
                .build();
        restTemplate.postForEntity(authUrl + "/register", registerReq, String.class);

        LoginResponse login = restTemplate.postForEntity(
                authUrl + "/login",
                new LoginRequest("testuser", "testpass123"),
                LoginResponse.class).getBody();
        token = login.getToken();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @Order(1)
    void createOrder_shouldReturnCreatedOrder() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName("John Doe")
                .orderNumber("ORD-001")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("99.99"))
                .shippingAddress("123 Test Street")
                .build();

        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                baseUrl, HttpMethod.POST, new HttpEntity<>(request, authHeaders()), OrderResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("John Doe", response.getBody().getCustomerName());
        assertEquals("ORD-001", response.getBody().getOrderNumber());
        assertEquals(OrderStatus.PENDING, response.getBody().getStatus());
        assertEquals(0, new BigDecimal("99.99").compareTo(response.getBody().getTotalAmount()));

        createdOrderId = response.getBody().getId();
    }

    @Test
    @Order(2)
    void getOrderById_shouldReturnOrder() {
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                baseUrl + "/" + createdOrderId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                OrderResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(createdOrderId, response.getBody().getId());
    }

    @Test
    @Order(3)
    void getOrderById_notFound_shouldReturn404() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/99999",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @Order(4)
    void getAllOrders_shouldReturnPaginatedResults() {
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
    void updateOrder_shouldReturnUpdatedOrder() {
        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .status(OrderStatus.PROCESSING)
                .totalAmount(new BigDecimal("149.99"))
                .build();

        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                baseUrl + "/" + createdOrderId,
                HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders()),
                OrderResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(OrderStatus.PROCESSING, response.getBody().getStatus());
        assertEquals(0, new BigDecimal("149.99").compareTo(response.getBody().getTotalAmount()));
    }

    @Test
    @Order(6)
    void createOrder_withInvalidData_shouldReturn400() {
        Map<String, Object> invalidRequest = Map.of(
                "customerName", "",
                "orderNumber", "",
                "totalAmount", -10
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                new HttpEntity<>(invalidRequest, authHeaders()),
                Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @Order(7)
    void deleteOrder_shouldReturn204() {
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/" + createdOrderId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
