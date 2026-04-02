package com.example.orderservice.infrastructure.client;

import com.example.orderservice.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceClient {

    private final RestClient restClient;
    private final JwtService jwtService;

    @Value("${app.service.inventory.base-url:http://inventory-service:8081}")
    private String inventoryBaseUrl;

    @Value("${app.service.shipping.base-url:http://shipping-service:8082}")
    private String shippingBaseUrl;

    @Value("${app.service.timeout:5000}")
    private long timeoutMs;

    public String checkInventory(String sku) {
        log.debug("Checking inventory for SKU: {}", sku);
        return restClient.get()
                .uri(inventoryBaseUrl + "/api/inventory/{sku}", sku)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(String.class);
    }

    public String requestShipping(Long orderId) {
        log.debug("Requesting shipping for order: {}", orderId);
        return restClient.post()
                .uri(shippingBaseUrl + "/api/shipping")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"orderId\": " + orderId + "}")
                .retrieve()
                .body(String.class);
    }
}
