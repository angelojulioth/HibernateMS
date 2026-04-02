package com.example.inventoryservice;

import com.example.inventoryservice.domain.entity.InventoryItem;
import com.example.inventoryservice.infrastructure.repository.InventoryRepository;
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
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldCreateAndRetrieveInventoryItem() throws Exception {
        InventoryItem item = InventoryItem.builder()
                .sku("TEST-SKU-001")
                .quantity(100)
                .warehouseLocation("A-101")
                .available(true)
                .build();

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"TEST-SKU-001\",\"quantity\":100,\"warehouseLocation\":\"A-101\",\"available\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("TEST-SKU-001"))
                .andExpect(jsonPath("$.quantity").value(100));

        mockMvc.perform(get("/api/inventory/TEST-SKU-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("TEST-SKU-001"))
                .andExpect(jsonPath("$.quantity").value(100))
                .andExpect(jsonPath("$.warehouseLocation").value("A-101"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentSku() throws Exception {
        mockMvc.perform(get("/api/inventory/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateQuantity() throws Exception {
        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"TEST-SKU-002\",\"quantity\":50,\"warehouseLocation\":\"B-202\",\"available\":true}"))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/inventory/TEST-SKU-002/quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(25));
    }

    @Test
    void shouldReturnAllItems() throws Exception {
        inventoryRepository.save(InventoryItem.builder()
                .sku("LIST-SKU-1").quantity(10).warehouseLocation("C-303").available(true).build());
        inventoryRepository.save(InventoryItem.builder()
                .sku("LIST-SKU-2").quantity(20).warehouseLocation("C-304").available(true).build());

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void shouldDeleteItem() throws Exception {
        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"DELETE-SKU\",\"quantity\":5,\"warehouseLocation\":\"D-404\",\"available\":true}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/inventory/DELETE-SKU"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/inventory/DELETE-SKU"))
                .andExpect(status().isNotFound());
    }
}
