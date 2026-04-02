package com.example.orderservice.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResilienceConfig {

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "inventoryFallback")
    @Retry(name = "inventoryService")
    public String checkInventoryWithResilience(String sku) {
        throw new RuntimeException("Delegated to ServiceClient");
    }

    public String inventoryFallback(String sku, Throwable t) {
        log.warn("Inventory service unavailable for SKU: {}. Using fallback.", sku, t);
        return "{\"status\": \"unavailable\", \"sku\": \"" + sku + "\"}";
    }

    @CircuitBreaker(name = "shippingService", fallbackMethod = "shippingFallback")
    @Retry(name = "shippingService")
    public String requestShippingWithResilience(Long orderId) {
        throw new RuntimeException("Delegated to ServiceClient");
    }

    public String shippingFallback(Long orderId, Throwable t) {
        log.warn("Shipping service unavailable for order: {}. Queued for retry.", orderId, t);
        return "{\"status\": \"queued\", \"orderId\": " + orderId + "}";
    }
}
