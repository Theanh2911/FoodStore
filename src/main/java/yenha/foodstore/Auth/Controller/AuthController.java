package yenha.foodstore.Auth.Controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import yenha.foodstore.Auth.DTO.AuthResponse;
import yenha.foodstore.Auth.DTO.LoginRequest;
import yenha.foodstore.Auth.DTO.RegisterRequest;
import yenha.foodstore.Auth.Entity.Role;
import yenha.foodstore.Auth.Entity.User;
import yenha.foodstore.Auth.Security.JwtUtils;
import yenha.foodstore.Auth.Service.TokenBlacklistService;
import yenha.foodstore.Auth.Service.UserService;
import yenha.foodstore.Order.DTO.OrderResponseDTO;
import yenha.foodstore.Order.Entity.Order;
import yenha.foodstore.Order.Service.OrderService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin-register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = userService.adminRegister(request);
            
            if (response.getUserId() != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            AuthResponse errorResponse = new AuthResponse("Registration failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/client-register")
    public ResponseEntity<AuthResponse> clientRegister(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = userService.clientRegister(request);

            if (response.getUserId() != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            AuthResponse errorResponse = new AuthResponse("Registration failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        
        if (response.getUserId() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        Map<String, String> response = new HashMap<>();
        
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("message", "Invalid authorization header");
                return ResponseEntity.badRequest().body(response);
            }
            
            String token = authHeader.substring(7);
            
            // Extract expiration time and type from token
            try {
                Claims claims = jwtUtils.extractClaims(token, claims1 -> claims1);
                Date expiration = claims.getExpiration();
                String tokenType = jwtUtils.getTokenType(token);
                
                // Add token to blacklist with its expiration time and type
                tokenBlacklistService.blacklistToken(token, expiration.getTime(), 
                        tokenType != null ? tokenType : "access");
                
                response.put("message", "Logged out successfully");
                return ResponseEntity.ok(response);
            } catch (JwtException e) {
                response.put("message", "Invalid token");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("message", "Logout failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Refresh token is required"));
        }
        
        try {
            // Check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
                return ResponseEntity.status(401).body(Map.of("message", "Token is blacklisted"));
            }
            
            // Verify it's a refresh token
            if (!jwtUtils.isRefreshToken(refreshToken)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid refresh token"));
            }
            
            // Extract user info from refresh token
            String phoneNumber = jwtUtils.getUsernameFromToken(refreshToken);
            
            // Verify user still exists
            Optional<User> userOpt = userService.findByPhoneNumber(phoneNumber);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("message", "User not found"));
            }
            
            User user = userOpt.get();
            
            // Generate new access token and refresh token
            String newAccessToken = jwtUtils.generateToken(user.getPhoneNumber(), user.getRole().name());
            String newRefreshToken = jwtUtils.generateRefreshToken(user.getPhoneNumber(), user.getRole().name());
            
            // Blacklist the old refresh token to prevent reuse
            Claims claims = jwtUtils.extractClaims(refreshToken, claims1 -> claims1);
            tokenBlacklistService.blacklistToken(refreshToken, claims.getExpiration().getTime(), "refresh");
            
            AuthResponse response = new AuthResponse(
                user.getId(),
                user.getName(),
                user.getPhoneNumber(),
                "Token refreshed successfully",
                newAccessToken,
                newRefreshToken,
                user.getRole().name()
            );
            
            return ResponseEntity.ok(response);
        } catch (JwtException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired refresh token"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Token refresh failed: " + e.getMessage()));
        }
    }

//    @GetMapping("/user/{userId}")
//    public ResponseEntity<AuthResponse> getUserById(@PathVariable Long userId) {
//        Optional<User> userOpt = userService.findById(userId);
//
//        if (userOpt.isPresent()) {
//            User user = userOpt.get();
//            AuthResponse response = new AuthResponse(user.getId(), user.getName(),
//                                                   user.getPhoneNumber(),user.getRole(),user "User found");
//            return ResponseEntity.ok(response);
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }
    
    @GetMapping("/orders/{userId}")
    public ResponseEntity<List<OrderResponseDTO>> getOrderHistory(@PathVariable Long userId) {
        String userIdStr = userId.toString();
        
        List<Order> orders = orderService.getOrdersByUserId(userIdStr);
        List<OrderResponseDTO> responseDTOs = orders.stream()
            .map(orderService::convertToResponseDTO)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(responseDTOs);
    }

    @PostMapping("/get-users-by-roles")
    public ResponseEntity<List<User>> getUsersByRoles(@RequestBody List<Role> roles) {
        return ResponseEntity.ok(userService.findByRoles(roles));
    }
    
    /**
     * DEBUG: Check token expiration time
     */
    @PostMapping("/debug-token")
    public ResponseEntity<Map<String, Object>> debugToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
        }
        
        try {
            Claims claims = jwtUtils.extractClaims(token, c -> c);
            
            long currentTime = System.currentTimeMillis();
            long issuedAtMs = claims.getIssuedAt().getTime();
            long expirationMs = claims.getExpiration().getTime();
            long durationMs = expirationMs - issuedAtMs;
            long remainingMs = expirationMs - currentTime;
            
            boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(token);
            boolean isExpired = claims.getExpiration().before(new Date());
            
            Map<String, Object> response = new HashMap<>();
            response.put("phoneNumber", claims.getSubject());
            response.put("role", claims.get("role"));
            response.put("type", claims.get("type"));
            response.put("issuedAt", claims.getIssuedAt().toString());
            response.put("expiresAt", claims.getExpiration().toString());
            response.put("currentTime", new Date(currentTime).toString());
            response.put("configuredDurationMinutes", 30);
            response.put("actualDurationMinutes", durationMs / 1000 / 60);
            response.put("remainingMinutes", remainingMs / 1000 / 60);
            response.put("remainingSeconds", (remainingMs / 1000) % 60);
            response.put("isExpired", isExpired);
            response.put("isBlacklisted", isBlacklisted);
            response.put("shouldBeValid", !isExpired && !isBlacklisted);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid token", "message", e.getMessage()));
        }
    }

}
