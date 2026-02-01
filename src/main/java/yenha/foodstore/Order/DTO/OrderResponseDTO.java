package yenha.foodstore.Order.DTO;

import lombok.*;
import yenha.foodstore.Order.Entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class OrderResponseDTO {
    private Long orderId;
    private String customerName;
    private Integer tableNumber;
    private Double totalAmount;
    private String promotionCode;
    private Double discountAmount;
    private Double finalAmount;
    private LocalDateTime orderTime;
    private OrderStatus status;
    private Boolean isRated;
    private List<OrderItemResponseDTO> items;
}

