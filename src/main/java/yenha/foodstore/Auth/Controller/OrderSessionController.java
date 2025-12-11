package yenha.foodstore.Auth.Controller;

import org.springframework.security.access.prepost.PreAuthorize;
import yenha.foodstore.Auth.Entity.OrderSession;
import yenha.foodstore.Auth.Service.OrderSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/session")
@CrossOrigin(origins = "*")
public class OrderSessionController {

    private final OrderSessionService sessionService;

    public OrderSessionController(OrderSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(@RequestParam Integer tableNumber) {
        OrderSession session = sessionService.createSession(tableNumber);
        
        // Calculate expiration time (configured hours from creation)
        int expirationHours = sessionService.getSessionExpirationHours();
        java.time.LocalDateTime expiresAt = session.getCreatedAt().plusHours(expirationHours);
        
        return ResponseEntity.ok(Map.of(
                "sessionId", session.getSessionId(),
                "tableNumber", session.getTableNumber(),
                "createdAt", session.getCreatedAt().toString(),
                "expiresAt", expiresAt.toString(),
                "isActive", session.getIsActive()
        ));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId) {
        OrderSession session = sessionService.getSession(sessionId);
        
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Calculate expiration time and remaining time
        int expirationHours = sessionService.getSessionExpirationHours();
        java.time.LocalDateTime expiresAt = session.getCreatedAt().plusHours(expirationHours);
        java.time.Duration remainingTime = java.time.Duration.between(
            java.time.LocalDateTime.now(), expiresAt
        );
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("tableNumber", session.getTableNumber());
        response.put("createdAt", session.getCreatedAt().toString());
        response.put("expiresAt", expiresAt.toString());
        response.put("isActive", session.getIsActive());
        
        if (session.getIsActive() && remainingTime.toSeconds() > 0) {
            response.put("remainingMinutes", remainingTime.toMinutes());
            response.put("isExpired", false);
        } else {
            response.put("remainingMinutes", 0);
            response.put("isExpired", true);
        }
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/end/{sessionId}")
    public ResponseEntity<Void> endSession(@PathVariable String sessionId) {
        sessionService.deactivateSession(sessionId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/all")
    public ResponseEntity<java.util.List<OrderSession>> getAllSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }
}
