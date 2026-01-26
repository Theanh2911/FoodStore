package yenha.foodstore.Rate.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yenha.foodstore.Menu.Service.S3Service;
import yenha.foodstore.Order.Entity.Order;
import yenha.foodstore.Order.Repository.OrderRepository;
import yenha.foodstore.Order.Service.OrderService;
import yenha.foodstore.Rate.DTO.RatingRequestDTO;
import yenha.foodstore.Rate.DTO.RatingResponseDTO;
import yenha.foodstore.Rate.Entity.Rating;
import yenha.foodstore.Rate.Repository.RatingRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final S3Service s3Service;

    private static final long RATING_TIME_LIMIT_HOURS = 12;

    @Transactional
    public RatingResponseDTO createRating(Long orderId, String userId, RatingRequestDTO requestDTO, List<MultipartFile> images) {
        
        // 1. Check if order exists
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        // 2. Verify order belongs to the user
        if (order.getUserId() == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("You can only rate your own orders");
        }

        // 3. Check if order is already rated
        if (order.getIsRated() != null && order.getIsRated()) {
            throw new RuntimeException("This order has already been rated");
        }

        // 4. Check 12-hour time limit from orderTime
        LocalDateTime orderTime = order.getOrderTime();
        LocalDateTime now = LocalDateTime.now();
        long hoursSinceOrder = ChronoUnit.HOURS.between(orderTime, now);

        if (hoursSinceOrder > RATING_TIME_LIMIT_HOURS) {
            throw new RuntimeException("Rating period has expired. You can only rate within 12 hours of placing the order");
        }

        // 5. Upload images to S3 if provided
        List<String> imageUrls = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    String imageUrl = s3Service.uploadFile(image);
                    imageUrls.add(imageUrl);
                }
            }
        }

        // 6. Create rating
        Rating rating = new Rating();
        rating.setOrder(order);
        rating.setUserId(userId);
        rating.setComment(requestDTO.getComment());
        rating.setRating(requestDTO.getRating());
        rating.setImageUrls(imageUrls);
        rating.setCreatedAt(LocalDateTime.now());

        rating = ratingRepository.save(rating);

        // 7. Update order's isRated flag
        order.setIsRated(true);
        orderRepository.save(order);

        // 8. Convert to response DTO
        return convertToResponseDTO(rating, true);
    }

    public List<RatingResponseDTO> getAllRatings() {
        List<Rating> ratings = ratingRepository.findAllByOrderByCreatedAtDesc();
        return ratings.stream()
                .map(rating -> convertToResponseDTO(rating, true))
                .collect(Collectors.toList());
    }

    private RatingResponseDTO convertToResponseDTO(Rating rating, boolean includeOrderDetails) {
        RatingResponseDTO dto = new RatingResponseDTO();
        dto.setRatingId(rating.getRatingId());
        dto.setOrderId(rating.getOrder().getOrderId());
        dto.setUserId(rating.getUserId());
        dto.setComment(rating.getComment());
        dto.setRating(rating.getRating());
        dto.setImageUrls(rating.getImageUrls());
        dto.setCreatedAt(rating.getCreatedAt());

        if (includeOrderDetails) {
            dto.setOrderDetails(orderService.convertToResponseDTO(rating.getOrder()));
        }

        return dto;
    }
}
