package yenha.foodstore.Rate.Entity;

import jakarta.persistence.*;
import lombok.*;
import yenha.foodstore.Order.Entity.Order;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ratings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Rating {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ratingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String userId;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @ElementCollection
    @CollectionTable(name = "rating_images", joinColumns = @JoinColumn(name = "rating_id"))
    @Column(name = "image_url")
    private List<String> imageUrls;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
