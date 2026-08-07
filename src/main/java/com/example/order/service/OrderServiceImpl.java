package com.example.order.service;

import com.example.order.dto.OrderRequestDTO;
import com.example.order.dto.OrderResponseDTO;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.exception.InvalidOrderStateException;
import com.example.order.exception.ResourceNotFoundException;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Set<OrderStatus> TERMINAL_STATES =
            Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        log.info("Fetching all orders");
        return orderRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        log.info("Fetching order with id: {}", id);
        Order order = findOrderOrThrow(id);
        return mapToResponseDTO(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO requestDTO) {
        log.info("Creating new order for customer: {}", requestDTO.getCustomerName());

        Order order = Order.builder()
                .customerName(requestDTO.getCustomerName())
                .productName(requestDTO.getProductName())
                .quantity(requestDTO.getQuantity())
                .price(requestDTO.getPrice())
                .status(requestDTO.getStatus() != null ? requestDTO.getStatus() : OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order created successfully with id: {}", saved.getId());
        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrder(Long id, OrderRequestDTO requestDTO) {
        log.info("Updating order with id: {}", id);
        Order order = findOrderOrThrow(id);

        if (TERMINAL_STATES.contains(order.getStatus())) {
            throw new InvalidOrderStateException(
                    "Cannot update order with id: " + id + " because it is already " + order.getStatus());
        }

        order.setCustomerName(requestDTO.getCustomerName());
        order.setProductName(requestDTO.getProductName());
        order.setQuantity(requestDTO.getQuantity());
        order.setPrice(requestDTO.getPrice());
        if (requestDTO.getStatus() != null) {
            order.setStatus(requestDTO.getStatus());
        }

        Order updated = orderRepository.save(order);
        log.info("Order with id: {} updated successfully", id);
        return mapToResponseDTO(updated);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        log.info("Deleting order with id: {}", id);
        Order order = findOrderOrThrow(id);
        orderRepository.delete(order);
        log.info("Order with id: {} deleted successfully", id);
    }

    private Order findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private OrderResponseDTO mapToResponseDTO(Order order) {
        Double totalAmount = order.getPrice() != null && order.getQuantity() != null
                ? order.getPrice() * order.getQuantity()
                : null;

        return OrderResponseDTO.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .totalAmount(totalAmount)
                .status(order.getStatus())
                .orderDate(order.getOrderDate())
                .build();
    }
}
