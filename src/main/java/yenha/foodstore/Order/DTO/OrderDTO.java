package yenha.foodstore.Order.DTO;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class OrderDTO {
    private String name;
    private Integer tableNumber;
    private Double total;
    private List<OrderItemDTO> items;
}
