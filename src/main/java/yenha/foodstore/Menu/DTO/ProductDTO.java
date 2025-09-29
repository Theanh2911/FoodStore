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
    
    private String image;
    
    @NotNull(message = "Category ID is required")
    private Long categoryId; // Using categoryId instead of full Category object
}
