package yenha.foodstore.Order.DTO;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class OrderDTO {
    private String name;
    private Integer tableNumber; // Optional - will be overridden by session if sessionId provided
    private String sessionId; // New field for session-based orders
    private String userId; // User ID for authenticated orders
    private Double total;
    private List<OrderItemDTO> items;
}
