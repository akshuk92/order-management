package com.example.order.service;

import com.example.order.dto.OrderRequestDTO;
import com.example.order.dto.OrderResponseDTO;

import java.util.List;

public interface OrderService {

    List<OrderResponseDTO> getAllOrders();

    OrderResponseDTO getOrderById(Long id);

    OrderResponseDTO createOrder(OrderRequestDTO requestDTO);

    OrderResponseDTO updateOrder(Long id, OrderRequestDTO requestDTO);

    void deleteOrder(Long id);
}
