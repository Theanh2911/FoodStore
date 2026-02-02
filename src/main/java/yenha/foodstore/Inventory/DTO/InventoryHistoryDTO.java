package yenha.foodstore.Inventory.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryHistoryDTO {
    private Long productId;
    private String productName;
    private LocalDate date;
    private Integer dailyLimit;
    private Integer numberRemain;
    private Integer soldQuantity;
    private Double priceAtDate;
    private Double costAtDate;
    private Double profitPerUnit;
    private Double totalRevenue;
    private Double totalCost;
    private Double totalProfit;
}
