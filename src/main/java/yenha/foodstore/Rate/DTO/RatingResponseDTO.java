package yenha.foodstore.Rate.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import yenha.foodstore.Order.DTO.OrderResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponseDTO {
    
    private Long ratingId;
    private Long orderId;
    private String userId;
    private String comment;
    private Integer rating;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
    private OrderResponseDTO orderDetails;
}
