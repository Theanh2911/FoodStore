package yenha.foodstore.Inventory.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAggregateDTO {
    private Long productId;
    private String productName;
    private String categoryName;
    private Double totalProfit;     // Sum of all profits in date range
    private Double soldRate;        // Average sold rate (0.0 - 1.0)
    private Double profitPerUnit;   // totalProfit / totalSoldQuantity
}
