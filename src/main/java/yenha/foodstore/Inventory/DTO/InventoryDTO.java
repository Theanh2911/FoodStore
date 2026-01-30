package yenha.foodstore.Inventory.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDTO {
    private Long productId;
    private String productName;
    private Integer numberRemain;
    private Integer dailyLimit;
    private Double priceAtDate;
    private Double costAtDate;
}
