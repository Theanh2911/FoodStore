package yenha.foodstore.Order.DTO;

import lombok.*;
import yenha.foodstore.Order.Entity.OrderStatus;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class StatusUpdateDTO {
    private OrderStatus status;
}

