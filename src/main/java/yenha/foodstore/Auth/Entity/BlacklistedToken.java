package yenha.foodstore.Auth.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "blacklisted_tokens", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_expiration", columnList = "expirationTime")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistedToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 500, unique = true)
    private String token;
    
    @Column(nullable = false)
    private LocalDateTime expirationTime;
    
    @Column(nullable = false)
    private LocalDateTime blacklistedAt;
    
    @Column(length = 20)
    private String tokenType; // "access" or "refresh"
    
    public BlacklistedToken(String token, LocalDateTime expirationTime, String tokenType) {
        this.token = token;
        this.expirationTime = expirationTime;
        this.blacklistedAt = LocalDateTime.now();
        this.tokenType = tokenType;
    }
}


