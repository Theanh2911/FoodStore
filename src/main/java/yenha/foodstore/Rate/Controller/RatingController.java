package yenha.foodstore.Rate.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yenha.foodstore.Auth.Security.AuthUser;
import yenha.foodstore.Rate.DTO.RatingRequestDTO;
import yenha.foodstore.Rate.DTO.RatingResponseDTO;
import yenha.foodstore.Rate.Service.RatingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RatingController {

    private final RatingService ratingService;

    @PostMapping(value = "/ratings/{orderId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> createRating(
            @PathVariable Long orderId,
            @RequestParam("rating") Integer ratingValue,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        
        try {
            // Get userId from JWT token
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated"));
            }

            AuthUser authUser = (AuthUser) authentication.getPrincipal();
            String userId = String.valueOf(authUser.getUser().getId());

            // Create request DTO
            RatingRequestDTO requestDTO = new RatingRequestDTO();
            requestDTO.setRating(ratingValue);
            requestDTO.setComment(comment);

            // Validate rating value
            if (ratingValue < 1 || ratingValue > 5) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Rating must be between 1 and 5"));
            }

            RatingResponseDTO responseDTO = ratingService.createRating(orderId, userId, requestDTO, images);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Rating created successfully");
            response.put("data", responseDTO);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An error occurred while creating the rating: " + e.getMessage()));
        }
    }

    @GetMapping("/ratings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllRatings() {
        try {
            List<RatingResponseDTO> ratings = ratingService.getAllRatings();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Ratings retrieved successfully");
            response.put("data", ratings);
            response.put("total", ratings.size());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An error occurred while retrieving ratings: " + e.getMessage()));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
