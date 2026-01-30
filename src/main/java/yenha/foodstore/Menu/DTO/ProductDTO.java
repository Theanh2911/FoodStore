package yenha.foodstore.Menu.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long productId;
    
    @NotBlank(message = "Product name is required")
    private String name;
    
    @NotNull(message = "Product price is required")
    @Positive(message = "Product price must be positive")
    private Double price;
    
    @NotNull(message = "Product cost is required")
    @Positive(message = "Product cost must be positive")
    private Double cost;
    
    @Positive(message = "Default daily limit must be positive")
    private Integer defaultDailyLimit;
    
    private String image;
    
    @NotNull(message = "Category ID is required")
    private Long categoryId;
}
