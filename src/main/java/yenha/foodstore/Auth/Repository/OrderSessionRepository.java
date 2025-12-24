package yenha.foodstore.Auth.Repository;

import yenha.foodstore.Auth.Entity.OrderSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OrderSessionRepository extends JpaRepository<OrderSession, Long> {

    Optional<OrderSession> findBySessionId(String sessionId);

}
