package com.example.orderservice.integration;

import com.example.orderservice.application.dto.CreateOrderRequest;
import com.example.orderservice.application.dto.UpdateOrderRequest;
import com.example.orderservice.application.events.EventBus;
import com.example.orderservice.application.events.OrderCreatedEvent;
import com.example.orderservice.application.events.OrderStatusChangedEvent;
import com.example.orderservice.domain.enums.OrderStatus;
import com.example.orderservice.integration.testauth.TestAuthConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import({TestAuthConfig.class, EventDispatchIntegrationTest.TestEventListenerConfig.class})
class EventDispatchIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private TestEventListener testEventListener;

    private String baseUrl;
    private String token;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        testEventListener.reset();

        restTemplate.postForEntity(baseUrl + "/api/auth/register",
                Map.of("username", "eventuser", "password", "eventpass123", "roles", Set.of("ROLE_USER")),
                String.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> login = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                Map.of("username", "eventuser", "password", "eventpass123", "roles", Set.of("ROLE_USER")),
                Map.class).getBody();
        token = (String) login.get("token");
    }

    @Test
    @Order(1)
    void createOrder_shouldDispatchOrderCreatedEvent() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName("Event Test User")
                .orderNumber("EVT-001")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("75.00"))
                .build();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        assertTrue(testEventListener.getOrderCreatedEvents().size() >= 1,
                "At least one OrderCreatedEvent should have been dispatched");

        OrderCreatedEvent event = testEventListener.getOrderCreatedEvents().get(0);
        assertEquals("EVT-001", event.getOrderNumber());
        assertEquals("Event Test User", event.getCustomerName());
        assertNotNull(event.getOrderId());
    }

    @Test
    @Order(2)
    void updateOrderStatus_shouldDispatchOrderStatusChangedEvent() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        CreateOrderRequest createReq = CreateOrderRequest.builder()
                .customerName("Status Change User")
                .orderNumber("EVT-002")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        ResponseEntity<Map> createResponse = restTemplate.exchange(
                baseUrl + "/api/orders",
                HttpMethod.POST,
                new HttpEntity<>(createReq, headers),
                Map.class);

        Long orderId = ((Number) createResponse.getBody().get("id")).longValue();

        UpdateOrderRequest updateReq = UpdateOrderRequest.builder()
                .status(OrderStatus.PROCESSING)
                .build();

        restTemplate.exchange(
                baseUrl + "/api/orders/" + orderId,
                HttpMethod.PUT,
                new HttpEntity<>(updateReq, headers),
                Map.class);

        assertTrue(testEventListener.getStatusChangedEvents().size() >= 1,
                "At least one OrderStatusChangedEvent should have been dispatched");

        OrderStatusChangedEvent event = testEventListener.getStatusChangedEvents().get(0);
        assertEquals(orderId, event.getOrderId());
        assertEquals(OrderStatus.PENDING, event.getOldStatus());
        assertEquals(OrderStatus.PROCESSING, event.getNewStatus());
    }

    @Test
    @Order(3)
    void directEventBusPublish_shouldDispatchEvent() {
        eventBus.publish(new OrderCreatedEvent(999L, "DIRECT-001", "Direct Test"));

        assertTrue(testEventListener.getOrderCreatedEvents().stream()
                        .anyMatch(e -> "DIRECT-001".equals(e.getOrderNumber())),
                "Directly published event should be received");
    }

    @Test
    @Order(4)
    void events_shouldHaveUniqueIds() {
        eventBus.publish(new OrderCreatedEvent(1L, "ID-001", "Test 1"));
        eventBus.publish(new OrderCreatedEvent(2L, "ID-002", "Test 2"));

        var events = testEventListener.getOrderCreatedEvents();
        assertEquals(2, events.size());
        assertNotEquals(events.get(0).getEventId(), events.get(1).getEventId(),
                "Each event should have a unique ID");
    }

    @TestConfiguration
    static class TestEventListenerConfig {
        @Bean
        TestEventListener testEventListener() {
            return new TestEventListener();
        }
    }

    static class TestEventListener {
        private final CopyOnWriteArrayList<OrderCreatedEvent> orderCreatedEvents = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<OrderStatusChangedEvent> statusChangedEvents = new CopyOnWriteArrayList<>();

        @org.springframework.context.event.EventListener
        public void onApplicationEvent(com.example.orderservice.application.events.DomainEvent event) {
            if (event instanceof OrderCreatedEvent e) {
                orderCreatedEvents.add(e);
            } else if (event instanceof OrderStatusChangedEvent e) {
                statusChangedEvents.add(e);
            }
        }

        public void reset() {
            orderCreatedEvents.clear();
            statusChangedEvents.clear();
        }

        public CopyOnWriteArrayList<OrderCreatedEvent> getOrderCreatedEvents() {
            return orderCreatedEvents;
        }

        public CopyOnWriteArrayList<OrderStatusChangedEvent> getStatusChangedEvents() {
            return statusChangedEvents;
        }
    }
}
