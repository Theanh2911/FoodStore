package yenha.foodstore.Order.DTO;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class OrderItemResponseDTO {
    private Long orderItemId;
    private Long productId;
    private String productName;
    private Double productPrice;
    private Integer quantity;
    private String note;
}

