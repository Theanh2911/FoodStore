package yenha.foodstore.Promotion.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yenha.foodstore.Constant.Error;
import yenha.foodstore.Menu.Entity.Category;
import yenha.foodstore.Menu.Entity.Product;
import yenha.foodstore.Menu.Repository.CategoryRepository;
import yenha.foodstore.Menu.Repository.ProductRepository;
import yenha.foodstore.Order.Entity.OrderItem;
import yenha.foodstore.Promotion.DTO.PromotionGenerateRequest;
import yenha.foodstore.Promotion.DTO.PromotionResponse;
import yenha.foodstore.Promotion.DTO.PromotionValidateResponse;
import yenha.foodstore.Promotion.Entity.Promotion;
import yenha.foodstore.Promotion.Entity.PromotionStatus;
import yenha.foodstore.Promotion.Entity.PromotionType;
import yenha.foodstore.Promotion.Exception.PromotionException;
import yenha.foodstore.Promotion.Repository.PromotionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {
    
    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();
    
    /**
     * Generate promotion code
     * Format: P/O + 5 random characters
     */
    @Transactional
    public Promotion generatePromotion(PromotionGenerateRequest request) {
        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new PromotionException("End date must be after start date");
        }
        
        // Validate promotion type and target
        if (request.getPromotionType() == PromotionType.PRODUCT) {
            if (request.getProductId() == null && request.getCategoryId() == null) {
                throw new PromotionException("PRODUCT type promotion requires productId or categoryId");
            }
            if (request.getProductId() != null && request.getCategoryId() != null) {
                throw new PromotionException("Cannot apply promotion to both product and category");
            }
        } else if (request.getPromotionType() == PromotionType.ORDER) {
            if (request.getProductId() != null || request.getCategoryId() != null) {
                throw new PromotionException("ORDER type promotion should not have productId or categoryId");
            }
        }
        
        Promotion promotion = new Promotion();
        
        // Generate unique code
        String code = generateUniqueCode(request.getPromotionType());
        promotion.setCode(code);
        
        promotion.setPromotionType(request.getPromotionType());
        promotion.setDiscountPercentage(request.getDiscountPercentage());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setTotalQuantity(request.getQuantity());
        promotion.setUsedCount(0);
        promotion.setMinOrderAmount(request.getMinOrderAmount());
        promotion.setStatus(PromotionStatus.ACTIVE);
        
        // Set product or category if applicable
        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new PromotionException(Error.PRODUCT_NOT_FOUND + request.getProductId()));
            promotion.setProduct(product);
        }
        
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new PromotionException(Error.CATEGORY_NOT_FOUND));
            promotion.setCategory(category);
        }
        
        return promotionRepository.save(promotion);
    }
    
    /**
     * Generate unique code with format P/O + 5 random chars
     */
    private String generateUniqueCode(PromotionType type) {
        String prefix = (type == PromotionType.PRODUCT) ? "P" : "O";
        String code;
        int attempts = 0;
        int maxAttempts = 100;
        
        do {
            StringBuilder sb = new StringBuilder(prefix);
            for (int i = 0; i < 5; i++) {
                sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
            }
            code = sb.toString();
            attempts++;
            
            if (attempts >= maxAttempts) {
                throw new PromotionException("Failed to generate unique promotion code after " + maxAttempts + " attempts");
            }
        } while (promotionRepository.existsByCode(code));
        
        return code;
    }
    
    /**
     * Validate promotion code (for user to check before applying)
     */
    @Transactional(readOnly = true)
    public PromotionValidateResponse validatePromotionCode(String code) {
        Optional<Promotion> promotionOpt = promotionRepository.findByCode(code.toUpperCase());
        
        if (promotionOpt.isEmpty()) {
            return PromotionValidateResponse.invalid("Promotion code not found");
        }
        
        Promotion promotion = promotionOpt.get();
        LocalDateTime now = LocalDateTime.now();
        
        // Check status
        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            return PromotionValidateResponse.invalid("Promotion code is inactive");
        }
        
        // Check dates
        if (now.isBefore(promotion.getStartDate())) {
            return PromotionValidateResponse.invalid("Promotion has not started yet");
        }
        if (now.isAfter(promotion.getEndDate())) {
            return PromotionValidateResponse.invalid("Promotion has expired");
        }
        
        // Check remaining usage
        if (promotion.getUsedCount() >= promotion.getTotalQuantity()) {
            return PromotionValidateResponse.invalid("Promotion has reached usage limit");
        }
        
        return PromotionValidateResponse.valid(
            promotion.getRemainingCount(),
            promotion.getDiscountPercentage(),
            promotion.getMinOrderAmount()
        );
    }
    
    /**
     * Apply promotion to order and calculate discount
     * Returns discount amount
     */
    @Transactional
    public double applyPromotion(String code, double totalAmount, List<OrderItem> orderItems) {
        Promotion promotion = promotionRepository.findByCode(code.toUpperCase())
            .orElseThrow(() -> new PromotionException("Promotion code not found"));
        
        // Validate promotion
        if (!promotion.isValid()) {
            throw new PromotionException("Promotion code is not valid");
        }
        
        // Check minimum order amount
        if (totalAmount < promotion.getMinOrderAmount()) {
            throw new PromotionException(
                String.format("Order amount must be at least %.0f VND to use this promotion", 
                    promotion.getMinOrderAmount())
            );
        }
        
        double discountAmount = 0.0;
        
        if (promotion.getPromotionType() == PromotionType.ORDER) {
            // Apply discount to entire order
            discountAmount = totalAmount * promotion.getDiscountPercentage() / 100.0;
        } else {
            // Apply discount to specific products/category
            for (OrderItem item : orderItems) {
                boolean matches = false;
                
                // Check if product matches
                if (promotion.getProduct() != null) {
                    matches = item.getProduct().getProductId().equals(promotion.getProduct().getProductId());
                }
                // Check if category matches
                else if (promotion.getCategory() != null) {
                    matches = item.getProduct().getCategory().getCategoryId()
                        .equals(promotion.getCategory().getCategoryId());
                }
                
                if (matches) {
                    double itemTotal = item.getPriceAtPurchase() * item.getQuantity();
                    discountAmount += itemTotal * promotion.getDiscountPercentage() / 100.0;
                }
            }
            
            // If no matching items, throw error
            if (discountAmount == 0.0) {
                throw new PromotionException("No items in order are eligible for this promotion");
            }
        }
        
        // Increase used count
        promotion.setUsedCount(promotion.getUsedCount() + 1);
        promotionRepository.save(promotion);
        
        return discountAmount;
    }
    
    /**
     * Deactivate promotion
     */
    @Transactional
    public Promotion deactivatePromotion(String code) {
        Promotion promotion = promotionRepository.findByCode(code.toUpperCase())
            .orElseThrow(() -> new PromotionException("Promotion code not found"));
        
        if (promotion.getStatus() == PromotionStatus.INACTIVE) {
            throw new PromotionException("Promotion is already inactive");
        }
        
        promotion.setStatus(PromotionStatus.INACTIVE);
        return promotionRepository.save(promotion);
    }
    
    /**
     * Get all promotions
     */
    @Transactional(readOnly = true)
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }
    
    /**
     * Get active promotions only
     */
    @Transactional(readOnly = true)
    public List<Promotion> getActivePromotions() {
        return promotionRepository.findByStatusOrderByCreatedAtDesc(PromotionStatus.ACTIVE);
    }
    
    /**
     * Get promotion by code
     */
    @Transactional(readOnly = true)
    public Optional<Promotion> getPromotionByCode(String code) {
        return promotionRepository.findByCode(code.toUpperCase());
    }
    
    /**
     * Convert entity to response DTO
     */
    public PromotionResponse convertToResponse(Promotion promotion) {
        PromotionResponse response = new PromotionResponse();
        response.setPromotionId(promotion.getPromotionId());
        response.setCode(promotion.getCode());
        response.setPromotionType(promotion.getPromotionType());
        response.setDiscountPercentage(promotion.getDiscountPercentage());
        response.setStartDate(promotion.getStartDate());
        response.setEndDate(promotion.getEndDate());
        response.setTotalQuantity(promotion.getTotalQuantity());
        response.setUsedCount(promotion.getUsedCount());
        response.setRemainingCount(promotion.getRemainingCount());
        response.setMinOrderAmount(promotion.getMinOrderAmount());
        response.setStatus(promotion.getStatus());
        response.setCreatedAt(promotion.getCreatedAt());
        
        if (promotion.getProduct() != null) {
            response.setProductId(promotion.getProduct().getProductId());
            response.setProductName(promotion.getProduct().getName());
        }
        
        if (promotion.getCategory() != null) {
            response.setCategoryId(promotion.getCategory().getCategoryId());
            response.setCategoryName(promotion.getCategory().getName());
        }
        
        return response;
    }
}
