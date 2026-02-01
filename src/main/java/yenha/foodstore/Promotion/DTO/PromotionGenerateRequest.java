package yenha.foodstore.Promotion.DTO;

import jakarta.validation.constraints.*;
import lombok.*;
import yenha.foodstore.Promotion.Entity.PromotionType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionGenerateRequest {
    
    @NotNull(message = "Promotion type is required")
    private PromotionType promotionType;
    
    @NotNull(message = "Discount percentage is required")
    @Min(value = 1, message = "Discount percentage must be at least 1%")
    @Max(value = 100, message = "Discount percentage cannot exceed 100%")
    private Double discountPercentage;
    
    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;
    
    @NotNull(message = "End date is required")
    private LocalDateTime endDate;
    
    // Chỉ 1 trong 2: productId hoặc categoryId (hoặc null nếu ORDER type)
    private Long productId;
    private Long categoryId;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @Min(value = 0, message = "Minimum order amount cannot be negative")
    private Double minOrderAmount = 70000.0; // Default 70k
}
