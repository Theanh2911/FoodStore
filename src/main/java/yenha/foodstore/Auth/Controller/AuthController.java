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

    /**
     * Register endpoint for admins
     */
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

    /**
     * Register endpoint for clients
     */
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

    /**
     * Login with phone number and password
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        
        if (response.getUserId() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Logout user by blacklisting the provided token
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        Map<String, String> response = new HashMap<>();
        
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("message", "Invalid authorization header");
                return ResponseEntity.badRequest().body(response);
            }
            
            String token = authHeader.substring(7);
            
            try {
                Claims claims = jwtUtils.extractClaims(token, claims1 -> claims1);
                Date expiration = claims.getExpiration();
                String tokenType = jwtUtils.getTokenType(token);
                
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

    /**
     * Create new access and refresh tokens using a valid refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Refresh token is required"));
        }
        
        try {
            if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
                return ResponseEntity.status(401).body(Map.of("message", "Token is blacklisted"));
            }

            if (!jwtUtils.isRefreshToken(refreshToken)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid refresh token"));
            }

            String phoneNumber = jwtUtils.getUsernameFromToken(refreshToken);

            Optional<User> userOpt = userService.findByPhoneNumber(phoneNumber);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("message", "User not found"));
            }
            
            User user = userOpt.get();

            String newAccessToken = jwtUtils.generateToken(user.getPhoneNumber(), user.getRole().name());
            String newRefreshToken = jwtUtils.generateRefreshToken(user.getPhoneNumber(), user.getRole().name());

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

    /**
     * Get order history for a user by userId
     */
    @GetMapping("/orders/{userId}")
    public ResponseEntity<List<OrderResponseDTO>> getOrderHistory(@PathVariable Long userId) {
        String userIdStr = userId.toString();
        
        List<Order> orders = orderService.getOrdersByUserId(userIdStr);
        List<OrderResponseDTO> responseDTOs = orders.stream()
            .map(orderService::convertToResponseDTO)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(responseDTOs);
    }

    /**
     * Get a list of users by their roles
     */
    @PostMapping("/get-users-by-roles")
    public ResponseEntity<List<User>> getUsersByRoles(@RequestBody List<Role> roles) {
        return ResponseEntity.ok(userService.findByRoles(roles));
    }

}
