package yenha.foodstore.ai.suggestion.DTO.BusinessResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PromotionStrategy(
    @JsonProperty("mainProduct")
    String mainProduct,
    
    @JsonProperty("comboMainProduct")
    String comboMainProduct,
    
    @JsonProperty("sideDish")
    String sideDish,
    
    @JsonProperty("drink")
    String drink
) {}
