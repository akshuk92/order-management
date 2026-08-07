package com.example.order.service;

import com.example.order.dto.OrderRequestDTO;
import com.example.order.dto.OrderResponseDTO;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.exception.InvalidOrderStateException;
import com.example.order.exception.ResourceNotFoundException;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(1L)
                .customerName("Alice Johnson")
                .productName("Wireless Mouse")
                .quantity(2)
                .price(19.99)
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
    void getAllOrders_shouldReturnListOfOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<OrderResponseDTO> result = orderService.getAllOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerName()).isEqualTo("Alice Johnson");
        assertThat(result.get(0).getTotalAmount()).isEqualTo(39.98);
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void getOrderById_whenExists_shouldReturnOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponseDTO result = orderService.getOrderById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProductName()).isEqualTo("Wireless Mouse");
    }

    @Test
    void getOrderById_whenNotExists_shouldThrowException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createOrder_shouldDefaultStatusToPending() {
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponseDTO result = orderService.createOrder(requestDTO);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void updateOrder_whenExistsAndNotTerminal_shouldUpdateAndReturnOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        requestDTO.setQuantity(5);
        OrderResponseDTO result = orderService.updateOrder(1L, requestDTO);

        assertThat(result).isNotNull();
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void updateOrder_whenTerminalState_shouldThrowException() {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrder(1L, requestDTO))
                .isInstanceOf(InvalidOrderStateException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void deleteOrder_whenExists_shouldDeleteOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        doNothing().when(orderRepository).delete(order);

        orderService.deleteOrder(1L);

        verify(orderRepository, times(1)).delete(order);
    }

    @Test
    void deleteOrder_whenNotExists_shouldThrowException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
