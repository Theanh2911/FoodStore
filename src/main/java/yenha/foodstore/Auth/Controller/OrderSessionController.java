package yenha.foodstore.Auth.Controller;

import org.springframework.security.access.prepost.PreAuthorize;
import yenha.foodstore.Auth.Entity.OrderSession;
import yenha.foodstore.Auth.Service.OrderSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class OrderSessionController {

    private final OrderSessionService sessionService;

    public OrderSessionController(OrderSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Create a new order session for a specific table number.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(@RequestParam Integer tableNumber) {
        OrderSession session = sessionService.createSession(tableNumber);
        return ResponseEntity.ok(Map.of(
                "sessionId", session.getSessionId(),
                "tableNumber", session.getTableNumber()
        ));
    }

    /**
     * Get session details by session ID.
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<OrderSession> getSession(@PathVariable String sessionId) {
        OrderSession session = sessionService.getSession(sessionId);
        return session != null ? ResponseEntity.ok(session) : ResponseEntity.notFound().build();
    }

    /**
     * Terminate an order session.
     */
    @PutMapping("/end/{sessionId}")
    public ResponseEntity<Void> endSession(@PathVariable String sessionId) {
        sessionService.deactivateSession(sessionId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get all order sessions.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/all")
    public ResponseEntity<java.util.List<OrderSession>> getAllSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

}
