package yenha.foodstore.Auth.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yenha.foodstore.Auth.DTO.AuthResponse;
import yenha.foodstore.Auth.DTO.LoginRequest;
import yenha.foodstore.Auth.DTO.RegisterRequest;
import yenha.foodstore.Auth.Entity.User;
import yenha.foodstore.Auth.Service.UserService;
import yenha.foodstore.Order.DTO.OrderResponseDTO;
import yenha.foodstore.Order.Entity.Order;
import yenha.foodstore.Order.Service.OrderService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = userService.register(request);
            
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
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<AuthResponse> getUserById(@PathVariable Long userId) {
        Optional<User> userOpt = userService.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            AuthResponse response = new AuthResponse(user.getId(), user.getName(), 
                                                   user.getPhoneNumber(), "User found");
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/orders/{userId}")
    public ResponseEntity<List<OrderResponseDTO>> getOrderHistory(@PathVariable Long userId) {
        // Convert userId to String as it's stored as String in Order entity
        String userIdStr = userId.toString();
        
        List<Order> orders = orderService.getOrdersByUserId(userIdStr);
        List<OrderResponseDTO> responseDTOs = orders.stream()
            .map(orderService::convertToResponseDTO)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(responseDTOs);
    }
}
