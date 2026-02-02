package yenha.foodstore.ai.suggestion.DTO.BusinessResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BusinessStrategyResponse(
    @JsonProperty("productId")
    Long productId,
    
    @JsonProperty("performanceTag")
    String performanceTag,
    
    @JsonProperty("productionStrategy")
    String productionStrategy,
    
    @JsonProperty("profitMarginStrategy")
    String profitMarginStrategy,
    
    @JsonProperty("promotionStrategy")
    PromotionStrategy promotionStrategy,
    
    @JsonProperty("note")
    String note
) {}
