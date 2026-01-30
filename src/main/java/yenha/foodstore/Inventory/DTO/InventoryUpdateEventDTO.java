package yenha.foodstore.Inventory.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdateEventDTO {
    private Long productId;
    private Integer numberRemain;
    private Long timestamp;
}
