package yenha.foodstore.Auth.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yenha.foodstore.Auth.Entity.User;
import yenha.foodstore.Auth.Repository.UserRepository;
import yenha.foodstore.Auth.DTO.LoginRequest;
import yenha.foodstore.Auth.DTO.RegisterRequest;
import yenha.foodstore.Auth.DTO.AuthResponse;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public AuthResponse register(RegisterRequest request) {
        // Validate input
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return new AuthResponse("Name is required");
        }
        
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return new AuthResponse("Password must be at least 6 characters long");
        }
        
        try {
            if (userRepository.existsByName(request.getName().trim())) {
                return new AuthResponse("User with this name already exists");
            }

            String hashedPassword = hashPassword(request.getPassword());

            User user = new User();
            user.setName(request.getName().trim());
            user.setPasswordHashed(hashedPassword);
            user.setPhoneNumber(request.getPhoneNumber());
            
            User savedUser = userRepository.save(user);
            
            return new AuthResponse(savedUser.getId(), savedUser.getName(), 
                                  savedUser.getPhoneNumber(), "Registration successful");
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
            
            // Verify password
            String hashedPassword = hashPassword(request.getPassword());
            if (!hashedPassword.equals(user.getPasswordHashed())) {
                return new AuthResponse("Invalid credentials");
            }
            
            return new AuthResponse(user.getId(), user.getName(), 
                                  user.getPhoneNumber(), "Login successful");
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
    
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(password.getBytes());
        
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
