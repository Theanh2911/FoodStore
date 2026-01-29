package yenha.foodstore.Auth.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import yenha.foodstore.Auth.Entity.Role;
import yenha.foodstore.Auth.Entity.User;
import yenha.foodstore.Auth.Repository.UserRepository;
import yenha.foodstore.Auth.DTO.LoginRequest;
import yenha.foodstore.Auth.DTO.RegisterRequest;
import yenha.foodstore.Auth.DTO.UpdateUserRequest;
import yenha.foodstore.Auth.DTO.UpdatePasswordRequest;
import yenha.foodstore.Auth.DTO.AuthResponse;
import yenha.foodstore.Auth.Security.JwtUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AuthResponse adminRegister(RegisterRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return new AuthResponse("Name is required");
        }
        
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            return new AuthResponse("Phone number is required");
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return new AuthResponse("Password must be at least 6 characters long");
        }
        
        try {
            if (userRepository.findByPhoneNumber(request.getPhoneNumber().trim()).isPresent()) {
                return new AuthResponse("User with this phone number already exists");
            }

            String hashedPassword = passwordEncoder.encode(request.getPassword());

            User user = new User();
            user.setName(request.getName().trim());
            user.setPasswordHashed(hashedPassword);
            user.setPhoneNumber(request.getPhoneNumber().trim());
            user.setRole(Role.STAFF);
            
            User savedUser = userRepository.save(user);

            String token = jwtUtils.generateToken(savedUser.getPhoneNumber(), savedUser.getRole().name());
            String refreshToken = jwtUtils.generateRefreshToken(savedUser.getPhoneNumber(), savedUser.getRole().name());

            return new AuthResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getPhoneNumber(),
                "Registration successful",
                token,
                refreshToken,
                savedUser.getRole().name()
            );
        } catch (Exception e) {
            return new AuthResponse("Registration failed: " + e.getMessage());
        }
    }

    public AuthResponse clientRegister(RegisterRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return new AuthResponse("Name is required");
        }

        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            return new AuthResponse("Phone number is required");
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return new AuthResponse("Password must be at least 6 characters long");
        }

        try {
            if (userRepository.findByPhoneNumber(request.getPhoneNumber().trim()).isPresent()) {
                return new AuthResponse("User with this phone number already exists");
            }

            String hashedPassword = passwordEncoder.encode(request.getPassword());

            User user = new User();
            user.setName(request.getName().trim());
            user.setPasswordHashed(hashedPassword);
            user.setPhoneNumber(request.getPhoneNumber().trim());
            user.setRole(Role.CLIENT);

            User savedUser = userRepository.save(user);

            String token = jwtUtils.generateToken(savedUser.getPhoneNumber(), savedUser.getRole().name());
            String refreshToken = jwtUtils.generateRefreshToken(savedUser.getPhoneNumber(), savedUser.getRole().name());

            return new AuthResponse(
                    savedUser.getId(),
                    savedUser.getName(),
                    savedUser.getPhoneNumber(),
                    "Registration successful",
                    token,
                    refreshToken,
                    savedUser.getRole().name()
            );
        } catch (Exception e) {
            return new AuthResponse("Registration failed: " + e.getMessage());
        }
    }
    
    public AuthResponse login(LoginRequest request) {
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            return new AuthResponse("Phone number is required");
        }
        
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            return new AuthResponse("Password is required");
        }
        
        try {
            // Find user by phone number
            Optional<User> userOpt = userRepository.findByPhoneNumber(request.getPhoneNumber().trim());
            if (userOpt.isEmpty()) {
                return new AuthResponse("Invalid credentials");
            }
            
            User user = userOpt.get();
            
            // Verify password using BCrypt
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHashed())) {
                return new AuthResponse("Invalid credentials");
            }
            
            // Generate JWT access token and refresh token with role
            String token = jwtUtils.generateToken(user.getPhoneNumber(), user.getRole().name());
            String refreshToken = jwtUtils.generateRefreshToken(user.getPhoneNumber(), user.getRole().name());

            return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getPhoneNumber(),
                "Login successful",
                token,
                refreshToken,
                user.getRole().name()
            );
        } catch (Exception e) {
            return new AuthResponse("Login failed: " + e.getMessage());
        }
    }
    
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }
    
    public Optional<User> findByName(String name) {
        return userRepository.findByName(name);
    }

    public List<User> findByRoles(List<Role> roles) {
        return userRepository.findAllByRoleIn(roles);
    }
    
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    /**
     * Logout business logic - parse token and blacklist it
     *
     * @param authHeader The Authorization header containing the Bearer token
     * @return Map with success or error message
     */
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    public Map<String, String> logout(String authHeader) {
        Map<String, String> response = new HashMap<>();

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("message", "Invalid authorization header");
            response.put("error", "true");
            return response;
        }

        String token = authHeader.substring(7);

        try {
            io.jsonwebtoken.Claims claims = jwtUtils.extractClaims(token, claims1 -> claims1);
            java.util.Date expiration = claims.getExpiration();
            String tokenType = jwtUtils.getTokenType(token);

            tokenBlacklistService.blacklistToken(token, expiration.getTime(),
                    tokenType != null ? tokenType : "access");

            response.put("message", "Logged out successfully");
            return response;
        } catch (io.jsonwebtoken.JwtException e) {
            response.put("message", "Invalid token");
            response.put("error", "true");
            return response;
        }
    }

    /**
     * Update user information (name, phone number, password)
     *
     * @param userId The ID of the user to update
     * @param request The update request containing new values
     * @return Map with success or error message
     */
    public Map<String, String> updateUser(Long userId, UpdateUserRequest request) {
        Map<String, String> response = new HashMap<>();

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                response.put("message", "User not found");
                response.put("error", "true");
                return response;
            }

            User user = userOpt.get();

            // Update name if provided
            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                user.setName(request.getName().trim());
            }

            // Update phone number if provided and not duplicate
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
                String newPhoneNumber = request.getPhoneNumber().trim();
                
                // Check if phone number already exists for another user
                Optional<User> existingUser = userRepository.findByPhoneNumber(newPhoneNumber);
                if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                    response.put("message", "Phone number already exists");
                    response.put("error", "true");
                    return response;
                }
                
                user.setPhoneNumber(newPhoneNumber);
            }

            // Update password if provided
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                if (request.getPassword().length() < 6) {
                    response.put("message", "Password must be at least 6 characters long");
                    response.put("error", "true");
                    return response;
                }
                String hashedPassword = passwordEncoder.encode(request.getPassword());
                user.setPasswordHashed(hashedPassword);
            }

            userRepository.save(user);

            response.put("message", "User updated successfully");
            response.put("userId", user.getId().toString());
            response.put("name", user.getName());
            response.put("phoneNumber", user.getPhoneNumber());
            return response;
        } catch (Exception e) {
            response.put("message", "Update failed: " + e.getMessage());
            response.put("error", "true");
            return response;
        }
    }

    /**
     * Delete user permanently from database (hard delete)
     *
     * @param userId The ID of the user to delete
     * @return Map with success or error message
     */
    public Map<String, String> deleteUser(Long userId) {
        Map<String, String> response = new HashMap<>();

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                response.put("message", "User not found");
                response.put("error", "true");
                return response;
            }

            userRepository.deleteById(userId);

            response.put("message", "User deleted successfully");
            response.put("userId", userId.toString());
            return response;
        } catch (Exception e) {
            response.put("message", "Delete failed: " + e.getMessage());
            response.put("error", "true");
            return response;
        }
    }

    /**
     * Update password for authenticated user (staff can update their own password)
     *
     * @param userId The ID of the user updating their password
     * @param request The update password request containing old and new passwords
     * @return Map with success or error message
     */
    public Map<String, String> updatePassword(Long userId, UpdatePasswordRequest request) {
        Map<String, String> response = new HashMap<>();

        try {
            // Validation
            if (request.getOldPassword() == null || request.getOldPassword().isEmpty()) {
                response.put("message", "Old password is required");
                response.put("error", "true");
                return response;
            }

            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                response.put("message", "New password is required");
                response.put("error", "true");
                return response;
            }

            if (request.getNewPassword().length() < 6) {
                response.put("message", "New password must be at least 6 characters long");
                response.put("error", "true");
                return response;
            }

            // Find user
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                response.put("message", "User not found");
                response.put("error", "true");
                return response;
            }

            User user = userOpt.get();

            // Verify old password
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHashed())) {
                response.put("message", "Old password is incorrect");
                response.put("error", "true");
                return response;
            }

            // Update to new password
            String hashedPassword = passwordEncoder.encode(request.getNewPassword());
            user.setPasswordHashed(hashedPassword);
            userRepository.save(user);

            response.put("message", "Password updated successfully");
            response.put("userId", user.getId().toString());
            return response;
        } catch (Exception e) {
            response.put("message", "Password update failed: " + e.getMessage());
            response.put("error", "true");
            return response;
        }
    }

    /**
     * Refresh token business logic - validate refresh token and generate new tokens
     *
     * @param refreshToken The refresh token to be validated and refreshed
     * @return AuthResponse with new tokens or error message
     */
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return new AuthResponse("Refresh token is required");
        }

        try {
            if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
                return new AuthResponse("Token is blacklisted");
            }

            if (!jwtUtils.isRefreshToken(refreshToken)) {
                return new AuthResponse("Invalid refresh token");
            }

            String phoneNumber = jwtUtils.getUsernameFromToken(refreshToken);

            Optional<User> userOpt = findByPhoneNumber(phoneNumber);
            if (userOpt.isEmpty()) {
                return new AuthResponse("User not found");
            }

            User user = userOpt.get();

            String newAccessToken = jwtUtils.generateToken(user.getPhoneNumber(), user.getRole().name());
            String newRefreshToken = jwtUtils.generateRefreshToken(user.getPhoneNumber(), user.getRole().name());

            io.jsonwebtoken.Claims claims = jwtUtils.extractClaims(refreshToken, claims1 -> claims1);
            tokenBlacklistService.blacklistToken(refreshToken, claims.getExpiration().getTime(), "refresh");

            return new AuthResponse(
                    user.getId(),
                    user.getName(),
                    user.getPhoneNumber(),
                    "Token refreshed successfully",
                    newAccessToken,
                    newRefreshToken,
                    user.getRole().name());
        } catch (io.jsonwebtoken.JwtException e) {
            return new AuthResponse("Invalid or expired refresh token");
        } catch (Exception e) {
            return new AuthResponse("Token refresh failed: " + e.getMessage());
        }
    }
}
