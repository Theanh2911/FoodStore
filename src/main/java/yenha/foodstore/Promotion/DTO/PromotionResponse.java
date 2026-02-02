package yenha.foodstore.Promotion.DTO;

import lombok.*;
import yenha.foodstore.Promotion.Entity.PromotionStatus;
import yenha.foodstore.Promotion.Entity.PromotionType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponse {
    
    private Long promotionId;
    private String code;
    private PromotionType promotionType;
    private Double discountPercentage;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long productId;
    private String productName;
    private Long categoryId;
    private String categoryName;
    private Integer totalQuantity;
    private Integer usedCount;
    private Integer remainingCount;
    private Double minOrderAmount;
    private PromotionStatus status;
    private LocalDateTime createdAt;
}
