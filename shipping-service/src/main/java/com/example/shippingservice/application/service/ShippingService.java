package com.example.shippingservice.application.service;

import com.example.shippingservice.domain.entity.Shipment;
import com.example.shippingservice.infrastructure.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingService {

    private final ShipmentRepository shipmentRepository;

    @Transactional
    public Shipment createShipment(Long orderId, String shippingAddress) {
        log.info("Creating shipment for order: {}", orderId);

        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .trackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .carrier("DefaultCarrier")
                .status("PENDING")
                .shippingAddress(shippingAddress)
                .build();

        return shipmentRepository.save(shipment);
    }

    @Transactional(readOnly = true)
    public List<Shipment> getByOrderId(Long orderId) {
        log.debug("Looking up shipments for order: {}", orderId);
        return shipmentRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Optional<Shipment> getByTrackingNumber(String trackingNumber) {
        log.debug("Looking up shipment by tracking number: {}", trackingNumber);
        return shipmentRepository.findByTrackingNumber(trackingNumber);
    }

    @Transactional(readOnly = true)
    public List<Shipment> getAll() {
        return shipmentRepository.findAll();
    }

    @Transactional
    public Optional<Shipment> updateStatus(String trackingNumber, String status) {
        log.info("Updating shipment {} status to: {}", trackingNumber, status);
        return shipmentRepository.findByTrackingNumber(trackingNumber).map(shipment -> {
            shipment.setStatus(status);
            if ("SHIPPED".equals(status)) {
                shipment.setShippedAt(LocalDateTime.now());
            } else if ("DELIVERED".equals(status)) {
                shipment.setDeliveredAt(LocalDateTime.now());
            }
            return shipmentRepository.save(shipment);
        });
    }
}
