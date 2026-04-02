package com.example.shippingservice.interfaces.controller;

import com.example.shippingservice.application.service.ShippingService;
import com.example.shippingservice.domain.entity.Shipment;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping
    public ResponseEntity<Shipment> createShipment(@Valid @RequestBody Map<String, Object> request) {
        Long orderId = Long.valueOf(request.get("orderId").toString());
        String shippingAddress = request.getOrDefault("shippingAddress", "Default Address").toString();

        Shipment shipment = shippingService.createShipment(orderId, shippingAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(shipment);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Shipment>> getByOrderId(@PathVariable Long orderId) {
        List<Shipment> shipments = shippingService.getByOrderId(orderId);
        return ResponseEntity.ok(shipments);
    }

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<Shipment> getByTrackingNumber(@PathVariable String trackingNumber) {
        return shippingService.getByTrackingNumber(trackingNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Shipment>> getAll() {
        return ResponseEntity.ok(shippingService.getAll());
    }

    @PutMapping("/track/{trackingNumber}/status")
    public ResponseEntity<Shipment> updateStatus(@PathVariable String trackingNumber,
                                                  @RequestBody Map<String, String> request) {
        String status = request.get("status");
        return shippingService.updateStatus(trackingNumber, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
