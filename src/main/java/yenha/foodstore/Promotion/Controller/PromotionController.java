package yenha.foodstore.Promotion.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import yenha.foodstore.Promotion.DTO.PromotionGenerateRequest;
import yenha.foodstore.Promotion.DTO.PromotionResponse;
import yenha.foodstore.Promotion.DTO.PromotionValidateResponse;
import yenha.foodstore.Promotion.Entity.Promotion;
import yenha.foodstore.Promotion.Exception.PromotionException;
import yenha.foodstore.Promotion.Service.PromotionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {
    
    private final PromotionService promotionService;
    
    /**
     * Generate new promotion (Admin only)
     */
    // @PreAuthorize("hasRole('ADMIN')")  // TODO: Uncomment this in production
    @PostMapping("/generate")
    public ResponseEntity<?> generatePromotion(
            @Valid @RequestBody PromotionGenerateRequest request,
            BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage()));
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }
        
        try {
            Promotion promotion = promotionService.generatePromotion(request);
            PromotionResponse response = promotionService.convertToResponse(promotion);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (PromotionException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Validate promotion code (Public - for users to check)
     */
    @GetMapping("/validate/{code}")
    public ResponseEntity<PromotionValidateResponse> validatePromotion(@PathVariable String code) {
        try {
            PromotionValidateResponse response = promotionService.validatePromotionCode(code);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                PromotionValidateResponse.invalid("Error validating promotion code"),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Get all promotions (Admin only)
     */
    // @PreAuthorize("hasRole('ADMIN')")  // TODO: Uncomment this in production
    @GetMapping
    public ResponseEntity<List<PromotionResponse>> getAllPromotions() {
        try {
            List<Promotion> promotions = promotionService.getAllPromotions();
            List<PromotionResponse> responses = promotions.stream()
                .map(promotionService::convertToResponse)
                .collect(Collectors.toList());
            return new ResponseEntity<>(responses, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get active promotions only (Public - for users to see available promotions)
     */
    @GetMapping("/active")
    public ResponseEntity<List<PromotionResponse>> getActivePromotions() {
        try {
            List<Promotion> promotions = promotionService.getActivePromotions();
            List<PromotionResponse> responses = promotions.stream()
                .map(promotionService::convertToResponse)
                .collect(Collectors.toList());
            return new ResponseEntity<>(responses, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get promotion details by code (Public)
     */
    @GetMapping("/{code}")
    public ResponseEntity<?> getPromotionByCode(@PathVariable String code) {
        try {
            Optional<Promotion> promotionOpt = promotionService.getPromotionByCode(code);
            if (promotionOpt.isPresent()) {
                PromotionResponse response = promotionService.convertToResponse(promotionOpt.get());
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Promotion not found");
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Deactivate promotion (Admin only)
     */
    // @PreAuthorize("hasRole('ADMIN')")  // TODO: Uncomment this in production
    @PutMapping("/{code}/deactivate")
    public ResponseEntity<?> deactivatePromotion(@PathVariable String code) {
        try {
            Promotion promotion = promotionService.deactivatePromotion(code);
            PromotionResponse response = promotionService.convertToResponse(promotion);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (PromotionException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
