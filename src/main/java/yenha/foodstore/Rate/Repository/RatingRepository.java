package yenha.foodstore.Rate.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yenha.foodstore.Rate.Entity.Rating;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    
    List<Rating> findAllByOrderByCreatedAtDesc();
    
    Optional<Rating> findByOrder_OrderId(Long orderId);
    
    List<Rating> findByUserId(String userId);
}
