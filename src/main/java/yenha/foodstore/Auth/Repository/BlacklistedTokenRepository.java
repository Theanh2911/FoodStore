package yenha.foodstore.Auth.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import yenha.foodstore.Auth.Entity.BlacklistedToken;

import java.time.LocalDateTime;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
    
    /**
     * Check if a token exists in the blacklist
     */
    boolean existsByToken(String token);
    
    /**
     * Delete all tokens that have expired
     * This helps keep the database clean
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM BlacklistedToken b WHERE b.expirationTime < :currentTime")
    int deleteExpiredTokens(LocalDateTime currentTime);
    
    /**
     * Count expired tokens (for monitoring before cleanup)
     */
    @Query("SELECT COUNT(b) FROM BlacklistedToken b WHERE b.expirationTime < :currentTime")
    long countExpiredTokens(LocalDateTime currentTime);
}


