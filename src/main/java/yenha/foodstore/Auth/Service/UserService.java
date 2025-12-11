package yenha.foodstore.Auth.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import yenha.foodstore.Auth.Entity.Role;
import yenha.foodstore.Auth.Entity.User;
import yenha.foodstore.Auth.Repository.UserRepository;
import yenha.foodstore.Auth.DTO.LoginRequest;
import yenha.foodstore.Auth.DTO.RegisterRequest;
import yenha.foodstore.Auth.DTO.AuthResponse;
import yenha.foodstore.Auth.Security.JwtUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
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
            if (userRepository.existsByName(request.getName().trim())) {
                return new AuthResponse("User with this name already exists");
            }

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
            if (userRepository.existsByName(request.getName().trim())) {
                return new AuthResponse("User with this name already exists");
            }

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
}
