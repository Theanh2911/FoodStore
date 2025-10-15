package yenha.foodstore.Auth.Controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.client.RestTemplate;
import yenha.foodstore.Auth.Entity.OrderSession;
import yenha.foodstore.Auth.Service.OrderSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
        return ResponseEntity.ok(Map.of(
                "sessionId", session.getSessionId(),
                "tableNumber", session.getTableNumber()
        ));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<OrderSession> getSession(@PathVariable String sessionId) {
        OrderSession session = sessionService.getSession(sessionId);
        return session != null ? ResponseEntity.ok(session) : ResponseEntity.notFound().build();
    }

    @PutMapping("/end/{sessionId}")
    public ResponseEntity<Void> endSession(@PathVariable String sessionId) {
        sessionService.deactivateSession(sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all")
    public ResponseEntity<java.util.List<OrderSession>> getAllSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }
}
