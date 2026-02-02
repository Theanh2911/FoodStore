package yenha.foodstore.Promotion.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yenha.foodstore.Promotion.Entity.Promotion;
import yenha.foodstore.Promotion.Entity.PromotionStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    Optional<Promotion> findByCode(String code);
    
    boolean existsByCode(String code);
    
    List<Promotion> findByStatus(PromotionStatus status);
    
    List<Promotion> findByStatusOrderByCreatedAtDesc(PromotionStatus status);
}
