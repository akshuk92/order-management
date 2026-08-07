package com.example.order.controller;

import com.example.order.dto.OrderRequestDTO;
import com.example.order.dto.OrderResponseDTO;
import com.example.order.entity.OrderStatus;
import com.example.order.exception.ResourceNotFoundException;
import com.example.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderResponseDTO responseDTO;
    private OrderRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        responseDTO = OrderResponseDTO.builder()
                .id(1L)
                .customerName("Alice Johnson")
                .productName("Wireless Mouse")
                .quantity(2)
                .price(19.99)
                .totalAmount(39.98)
                .status(OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .build();

        requestDTO = new OrderRequestDTO();
        requestDTO.setCustomerName("Alice Johnson");
        requestDTO.setProductName("Wireless Mouse");
        requestDTO.setQuantity(2);
        requestDTO.setPrice(19.99);
    }

    @Test
    void getAllOrders_shouldReturnOrderList() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerName").value("Alice Johnson"));
    }

    @Test
    void getOrderById_whenExists_shouldReturnOrder() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Wireless Mouse"));
    }

    @Test
    void getOrderById_whenNotExists_shouldReturn404() throws Exception {
        when(orderService.getOrderById(anyLong()))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_withValidData_shouldReturn201() throws Exception {
        when(orderService.createOrder(any(OrderRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Alice Johnson"));
    }

    @Test
    void createOrder_withInvalidData_shouldReturn400() throws Exception {
        OrderRequestDTO invalid = new OrderRequestDTO();
        invalid.setCustomerName("");
        invalid.setProductName("");
        invalid.setQuantity(0);
        invalid.setPrice(-10.0);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOrder_whenExists_shouldReturn200() throws Exception {
        when(orderService.updateOrder(anyLong(), any(OrderRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void deleteOrder_whenExists_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isNoContent());
    }
}
