package yenha.foodstore.Auth.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import yenha.foodstore.Auth.Entity.BlacklistedToken;
import yenha.foodstore.Auth.Repository.BlacklistedTokenRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBlacklistService {
    
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    
    /**
     * Add a token to the blacklist with its expiration time
     * @param token The JWT token to blacklist
     * @param expirationTime When the token expires (in milliseconds since epoch)
     */
    public void blacklistToken(String token, Long expirationTime) {
        blacklistToken(token, expirationTime, "access");
    }
    
    /**
     * Add a token to the blacklist with its expiration time and type
     * @param token The JWT token to blacklist
     * @param expirationTime When the token expires (in milliseconds since epoch)
     * @param tokenType The type of token ("access" or "refresh")
     */
    @CacheEvict(value = "tokenBlacklist", key = "#token")
    public void blacklistToken(String token, Long expirationTime, String tokenType) {
        // Check if token is already blacklisted to avoid duplicates
        if (!blacklistedTokenRepository.existsByToken(token)) {
            LocalDateTime expiration = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(expirationTime), 
                ZoneId.systemDefault()
            );
            
            BlacklistedToken blacklistedToken = new BlacklistedToken(token, expiration, tokenType);
            blacklistedTokenRepository.save(blacklistedToken);
            
            log.info("Token blacklisted (type: {}). Total blacklisted tokens: {}", 
                    tokenType, blacklistedTokenRepository.count());
        }
    }
    
    /**
     * Check if a token is blacklisted
     * @param token The JWT token to check
     * @return true if token is blacklisted, false otherwise
     */
    @Cacheable(value = "tokenBlacklist", key = "#token")
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.existsByToken(token);
    }
    
    /**
     * Clean up expired tokens from blacklist every hour
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    @CacheEvict(value = "tokenBlacklist", allEntries = true)
    public void cleanupExpiredTokens() {
        LocalDateTime currentTime = LocalDateTime.now();

        long expiredCount = blacklistedTokenRepository.countExpiredTokens(currentTime);
        
        if (expiredCount > 0) {
            // Delete expired tokens
            int deletedCount = blacklistedTokenRepository.deleteExpiredTokens(currentTime);
            log.info("Cleaned up {} expired tokens from blacklist. Remaining: {}", 
                    deletedCount, blacklistedTokenRepository.count());
        }
    }

}

