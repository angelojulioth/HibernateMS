package com.example.orderservice.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires external services (inventory, shipping, auth) to be running")
class ServiceCommunicationIntegrationTest {

    @Autowired
    private RestClient restClient;

    @Value("${app.service.inventory.base-url:http://localhost:8081}")
    private String inventoryBaseUrl;

    @Value("${app.service.shipping.base-url:http://localhost:8082}")
    private String shippingBaseUrl;

    @Value("${app.service.auth.base-url:http://localhost:8083}")
    private String authBaseUrl;

    @Test
    void shouldCommunicateWithInventoryService() {
        String inventoryItemJson = "{\"sku\":\"COMM-TEST-001\",\"quantity\":50,\"warehouseLocation\":\"E-505\",\"available\":true}";

        ResponseEntity<String> createResponse = restClient.post()
                .uri(inventoryBaseUrl + "/api/inventory")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(inventoryItemJson)
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertTrue(createResponse.getBody().contains("COMM-TEST-001"));

        ResponseEntity<String> getResponse = restClient.get()
                .uri(inventoryBaseUrl + "/api/inventory/{sku}", "COMM-TEST-001")
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertTrue(getResponse.getBody().contains("COMM-TEST-001"));
        assertTrue(getResponse.getBody().contains("50"));
    }

    @Test
    void shouldHandleInventoryNotFound() {
        ResponseEntity<String> response = restClient.get()
                .uri(inventoryBaseUrl + "/api/inventory/{sku}", "NONEXISTENT-SKU-XYZ")
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldUpdateInventoryViaRestCall() {
        String inventoryItemJson = "{\"sku\":\"COMM-TEST-002\",\"quantity\":100,\"warehouseLocation\":\"F-606\",\"available\":true}";

        restClient.post()
                .uri(inventoryBaseUrl + "/api/inventory")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(inventoryItemJson)
                .retrieve()
                .toEntity(String.class);

        ResponseEntity<String> updateResponse = restClient.put()
                .uri(inventoryBaseUrl + "/api/inventory/{sku}/quantity", "COMM-TEST-002")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("75")
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertTrue(updateResponse.getBody().contains("75"));
    }

    @Test
    void shouldListAllInventoryItems() {
        ResponseEntity<String> response = restClient.get()
                .uri(inventoryBaseUrl + "/api/inventory")
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldCommunicateWithShippingService() {
        ResponseEntity<String> createResponse = restClient.post()
                .uri(shippingBaseUrl + "/api/shipping")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"orderId\":1,\"shippingAddress\":\"123 Test Street\"}")
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertTrue(createResponse.getBody().contains("TRK-"));
        assertTrue(createResponse.getBody().contains("PENDING"));
    }

    @Test
    void shouldGetShipmentsByOrderId() {
        restClient.post()
                .uri(shippingBaseUrl + "/api/shipping")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"orderId\":999,\"shippingAddress\":\"456 Test Ave\"}")
                .retrieve()
                .toEntity(String.class);

        ResponseEntity<String> response = restClient.get()
                .uri(shippingBaseUrl + "/api/shipping/order/{orderId}", 999)
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("999"));
    }

    @Test
    void shouldUpdateShipmentStatus() {
        ResponseEntity<String> createResponse = restClient.post()
                .uri(shippingBaseUrl + "/api/shipping")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"orderId\":777,\"shippingAddress\":\"789 Test Blvd\"}")
                .retrieve()
                .toEntity(String.class);

        Map<String, Object> shipment;
        try {
            shipment = new com.fasterxml.jackson.databind.ObjectMapper().readValue(createResponse.getBody(), Map.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String trackingNumber = (String) shipment.get("trackingNumber");

        ResponseEntity<String> updateResponse = restClient.put()
                .uri(shippingBaseUrl + "/api/shipping/track/{trackingNumber}/status", trackingNumber)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"status\":\"SHIPPED\"}")
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertTrue(updateResponse.getBody().contains("SHIPPED"));
    }

    @Test
    void shouldAuthenticateViaAuthService() {
        ResponseEntity<String> registerResponse = restClient.post()
                .uri(authBaseUrl + "/api/auth/register")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"username\":\"commtestuser\",\"password\":\"password123\",\"roles\":[\"ROLE_USER\"]}")
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        ResponseEntity<String> loginResponse = restClient.post()
                .uri(authBaseUrl + "/api/auth/login")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"username\":\"commtestuser\",\"password\":\"password123\"}")
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertTrue(loginResponse.getBody().contains("token"));
    }

    @Test
    void shouldUseAuthTokenFromAuthServiceToAccessMainApp() {
        ResponseEntity<String> loginResponse = restClient.post()
                .uri(authBaseUrl + "/api/auth/login")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"username\":\"commtestuser\",\"password\":\"password123\"}")
                .retrieve()
                .toEntity(String.class);

        Map<String, Object> loginBody;
        try {
            loginBody = new com.fasterxml.jackson.databind.ObjectMapper().readValue(loginResponse.getBody(), Map.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String token = (String) loginBody.get("token");

        ResponseEntity<String> ordersResponse = restClient.get()
                .uri("http://localhost:8080/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.OK, ordersResponse.getStatusCode());
    }
}
