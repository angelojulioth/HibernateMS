package com.example.shippingservice;

import com.example.shippingservice.domain.entity.Shipment;
import com.example.shippingservice.infrastructure.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShippingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Test
    void shouldCreateShipment() throws Exception {
        mockMvc.perform(post("/api/shipping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":1,\"shippingAddress\":\"123 Test Street\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.trackingNumber").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldGetShipmentsByOrderId() throws Exception {
        shipmentRepository.save(Shipment.builder()
                .orderId(100L).trackingNumber("TRK-TEST001").carrier("TestCarrier")
                .status("PENDING").shippingAddress("456 Test Ave").build());

        mockMvc.perform(get("/api/shipping/order/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].orderId").value(100));
    }

    @Test
    void shouldGetShipmentByTrackingNumber() throws Exception {
        shipmentRepository.save(Shipment.builder()
                .orderId(200L).trackingNumber("TRK-TRACK01").carrier("FastShip")
                .status("SHIPPED").shippingAddress("789 Test Blvd").build());

        mockMvc.perform(get("/api/shipping/track/TRK-TRACK01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value("TRK-TRACK01"))
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void shouldReturnNotFoundForUnknownTrackingNumber() throws Exception {
        mockMvc.perform(get("/api/shipping/track/TRK-NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateShipmentStatus() throws Exception {
        shipmentRepository.save(Shipment.builder()
                .orderId(300L).trackingNumber("TRK-STATUS01").carrier("TestCarrier")
                .status("PENDING").shippingAddress("321 Test Rd").build());

        mockMvc.perform(put("/api/shipping/track/TRK-STATUS01/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void shouldReturnAllShipments() throws Exception {
        shipmentRepository.save(Shipment.builder()
                .orderId(400L).trackingNumber("TRK-ALL001").carrier("CarrierA")
                .status("PENDING").shippingAddress("Addr 1").build());
        shipmentRepository.save(Shipment.builder()
                .orderId(401L).trackingNumber("TRK-ALL002").carrier("CarrierB")
                .status("DELIVERED").shippingAddress("Addr 2").build());

        mockMvc.perform(get("/api/shipping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }
}
