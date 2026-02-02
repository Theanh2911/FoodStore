package yenha.foodstore.Inventory.Entity;

import jakarta.persistence.*;
import lombok.*;
import yenha.foodstore.Menu.Entity.Product;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_product_inventory",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "date"}),
    indexes = {
        @Index(name = "idx_product_date", columnList = "product_id, date"),
        @Index(name = "idx_date", columnList = "date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyProductInventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "price_at_date", nullable = false)
    private Double priceAtDate;

    @Column(name = "cost_at_date", nullable = false)
    private Double costAtDate;

    @Column(name = "daily_limit", nullable = false)
    private Integer dailyLimit;

    @Column(name = "number_remain", nullable = false)
    private Integer numberRemain;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Transient
    public Double getProfitPerUnit() {
        return priceAtDate - costAtDate;
    }

    @Transient
    public Integer getSoldQuantity() {
        return dailyLimit - numberRemain;
    }

    @Transient
    public Double getTotalProfit() {
        return getProfitPerUnit() * getSoldQuantity();
    }

    @Transient
    public Double getTotalRevenue() {
        return priceAtDate * getSoldQuantity();
    }

    @Transient
    public Double getTotalCost() {
        return costAtDate * getSoldQuantity();
    }
}
