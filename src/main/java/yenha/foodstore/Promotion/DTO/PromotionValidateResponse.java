package yenha.foodstore.Promotion.DTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionValidateResponse {
    
    private boolean valid;
    private String message;
    private Integer remainingCount;
    private Double discountPercentage;
    private Double minOrderAmount;
    
    // Constructor for valid case
    public static PromotionValidateResponse valid(Integer remainingCount, Double discountPercentage, Double minOrderAmount) {
        return new PromotionValidateResponse(
            true, 
            "Promotion code is valid", 
            remainingCount,
            discountPercentage,
            minOrderAmount
        );
    }
    
    // Constructor for invalid case
    public static PromotionValidateResponse invalid(String message) {
        return new PromotionValidateResponse(false, message, null, null, null);
    }
}
