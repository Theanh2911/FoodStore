package yenha.foodstore.Order.Entity;

import jakarta.persistence.*;
import lombok.*;
import yenha.foodstore.Menu.Entity.Product;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private Integer quantity = 1;

    @Column(name = "price_at_purchase", nullable = false)
    private Double priceAtPurchase;

    private String note;
}
