package com.example.order.dto;

import com.example.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {

    private Long id;
    private String customerName;
    private String productName;
    private Integer quantity;
    private Double price;
    private Double totalAmount;
    private OrderStatus status;
    private LocalDateTime orderDate;
}
