package yenha.foodstore.Auth.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtUtils {

    private static final long ACCESS_TOKEN_EXPIRATION = 1000L * 60L * 30L; // 30 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60L * 60L * 12L; // 12 hours

    private SecretKey key;

    @Value("${secretJwtString}")
    private String secretJwtString;

    @PostConstruct
    private void init() {
        if (secretJwtString == null || secretJwtString.trim().isEmpty()) {
            throw new IllegalStateException(
                    "No jwt secret found");
        }

        byte[] keyByte = secretJwtString.getBytes(StandardCharsets.UTF_8);

        if (keyByte.length < 32) {
            throw new IllegalStateException(
                    String.format(
                            "Jwt was too short",
                            keyByte.length));
        }
        this.key = new SecretKeySpec(keyByte, "HmacSHA256");
    }

    /**
     * Gen token with its payload
     */
    public String generateToken(String phoneNumber, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("type", "access");

        return Jwts.builder()
                .claims(claims)
                .subject(phoneNumber)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String phoneNumber, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(phoneNumber)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    public String getTokenType(String token) {
        return extractClaims(token, claims -> claims.get("type", String.class));
    }

    public boolean isRefreshToken(String token) {
        String type = getTokenType(token);
        return "refresh".equals(type);
    }

    public <T> T extractClaims(String token, Function<Claims, T> claimsTFunction) {
        return claimsTFunction.apply(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token, Claims::getExpiration).before(new Date());
    }

}
