package yenha.foodstore.Payment.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yenha.foodstore.Payment.Entity.Payment;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    // Kiểm tra giao dịch đã tồn tại chưa (tránh duplicate)
    boolean existsBySepayTransactionId(Long sepayTransactionId);
    
    // Tìm payment theo sepayTransactionId
    Optional<Payment> findBySepayTransactionId(Long sepayTransactionId);
    
    // Tìm payment theo orderId
    Optional<Payment> findByOrderId(Long orderId);
}

