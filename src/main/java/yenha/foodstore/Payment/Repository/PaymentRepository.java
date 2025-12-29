package yenha.foodstore.Payment.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yenha.foodstore.Payment.Entity.Payment;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsBySepayTransactionId(Long sepayTransactionId);

    Optional<Payment> findBySepayTransactionId(Long sepayTransactionId);

    Optional<Payment> findByOrderId(Long orderId);
}
