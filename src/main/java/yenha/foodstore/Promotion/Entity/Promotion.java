package yenha.foodstore.Promotion.Entity;

import jakarta.persistence.*;
import lombok.*;
import yenha.foodstore.Menu.Entity.Category;
import yenha.foodstore.Menu.Entity.Product;

import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long promotionId;
    
    @Column(unique = true, nullable = false, length = 6)
    private String code;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionType promotionType;
    
    @Column(nullable = false)
    private Double discountPercentage;
    
    @Column(nullable = false)
    private LocalDateTime startDate;
    
    @Column(nullable = false)
    private LocalDateTime endDate;
    
    // Áp dụng cho product cụ thể (nullable)
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    // Áp dụng cho category (nullable)
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    
    // Số lượng mã có thể sử dụng
    @Column(nullable = false)
    private Integer totalQuantity;
    
    // Số lần đã sử dụng
    @Column(nullable = false)
    private Integer usedCount = 0;
    
    // Đơn hàng tối thiểu (VD: 70000đ)
    @Column(nullable = false)
    private Double minOrderAmount = 70000.0;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionStatus status = PromotionStatus.ACTIVE;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Helper method
    public Integer getRemainingCount() {
        return totalQuantity - usedCount;
    }
    
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return status == PromotionStatus.ACTIVE 
            && now.isAfter(startDate) 
            && now.isBefore(endDate)
            && usedCount < totalQuantity;
    }
}
