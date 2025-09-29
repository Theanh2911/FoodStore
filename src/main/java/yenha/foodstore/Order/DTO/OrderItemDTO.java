package yenha.foodstore.Order.DTO;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class OrderItemDTO {
    private Long productId;
    private String name;
    private Integer quantity;
    private String note;
}
