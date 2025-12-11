package yenha.foodstore.Auth.Service;

import yenha.foodstore.Auth.Entity.OrderSession;
import yenha.foodstore.Auth.Repository.OrderSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderSessionService {

    private static final Logger logger = LoggerFactory.getLogger(OrderSessionService.class);
    private static final int SESSION_EXPIRATION_HOURS = 6; // Session expire sau 6 giờ (managed by MySQL Event)
    
    private final OrderSessionRepository repository;

    public OrderSessionService(OrderSessionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public OrderSession createSession(Integer tableNumber) {
        logger.info("Creating session for table number: {}", tableNumber);
        OrderSession session = new OrderSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setTableNumber(tableNumber);
        session.setCreatedAt(LocalDateTime.now());
        
        OrderSession savedSession = repository.save(session);
        logger.info("Session created successfully: sessionId={}, tableNumber={}, id={}", 
                   savedSession.getSessionId(), savedSession.getTableNumber(), savedSession.getId());

        return savedSession;
    }

    /**
     * Get session by ID
     * Note: Session expiration is handled by MySQL Event (runs every 5 minutes)
     * Event auto-sets is_active = 0 for sessions older than 6 hours
     */
    public OrderSession getSession(String sessionId) {
        return repository.findBySessionId(sessionId).orElse(null);
    }
    
    /**
     * Check if session is valid (exists and active)
     * Note: is_active is automatically managed by MySQL Event
     */
    public boolean isSessionValid(String sessionId) {
        OrderSession session = getSession(sessionId);
        return session != null && session.getIsActive();
    }

    @Transactional
    public void deactivateSession(String sessionId) {
        repository.findBySessionId(sessionId).ifPresent(s -> {
            s.setIsActive(false);
            repository.save(s);
        });
    }

    public java.util.List<OrderSession> getAllSessions() {
        return repository.findAll();
    }
    
    /**
     * Get session expiration hours (used for calculating expiresAt in API response)
     */
    public int getSessionExpirationHours() {
        return SESSION_EXPIRATION_HOURS;
    }
    
    /**
     * Note: Session expiration is managed by MySQL Event, not Java code
     * 
     * MySQL Event runs every 5 minutes:
     * UPDATE order_sessions 
     * SET is_active = 0 
     * WHERE is_active = 1 AND created_at < NOW() - INTERVAL 6 HOUR
     * 
     * This approach is better because:
     * - Centralized logic at database level
     * - Independent of application state
     * - More reliable and consistent
     * - Simpler Java code
     */
}
