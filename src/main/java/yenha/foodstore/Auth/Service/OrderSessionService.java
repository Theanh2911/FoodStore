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

    public OrderSession getSession(String sessionId) {
        return repository.findBySessionId(sessionId).orElse(null);
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
}
