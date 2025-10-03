package yenha.foodstore.Order.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import yenha.foodstore.Order.DTO.OrderResponseDTO;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class OrderEventService {
    
    private final ObjectMapper objectMapper;
    
    public OrderEventService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    
    public SseEmitter createEmitter(String clientId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        emitter.onCompletion(() -> removeEmitter(clientId, emitter));
        emitter.onTimeout(() -> removeEmitter(clientId, emitter));
        emitter.onError((e) -> {
            log.error("SSE error for client {}: {}", clientId, e.getMessage());
            removeEmitter(clientId, emitter);
        });
        
        emitters.computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        // Send initial connection confirmation
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to order updates"));
        } catch (IOException e) {
            log.error("Error sending initial message to client {}: {}", clientId, e.getMessage());
            removeEmitter(clientId, emitter);
        }
        
        log.info("New SSE connection established for client: {}", clientId);
        return emitter;
    }
    
    private void removeEmitter(String clientId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> clientEmitters = emitters.get(clientId);
        if (clientEmitters != null) {
            clientEmitters.remove(emitter);
            if (clientEmitters.isEmpty()) {
                emitters.remove(clientId);
            }
        }
        log.info("SSE connection removed for client: {}", clientId);
    }
    
    public void broadcastOrderCreated(OrderResponseDTO order) {
        broadcastEvent("order-created", order);
    }
    
    public void broadcastOrderUpdated(OrderResponseDTO order) {
        broadcastEvent("order-updated", order);
    }
    
    public void broadcastOrderStatusChanged(OrderResponseDTO order) {
        broadcastEvent("order-status-changed", order);
    }
    
    private void broadcastEvent(String eventName, OrderResponseDTO order) {
        log.info("Starting to broadcast event: {} for order ID: {}", eventName, order.getOrderId());
        
        if (emitters.isEmpty()) {
            log.warn("No active SSE connections to broadcast to");
            return;
        }
        
        try {
            String data = objectMapper.writeValueAsString(order);
            log.info("JSON data for broadcast: {}", data);
            
            int totalEmitters = emitters.values().stream().mapToInt(CopyOnWriteArrayList::size).sum();
            log.info("Broadcasting to {} emitters across {} clients", totalEmitters, emitters.size());
            
            emitters.forEach((clientId, clientEmitters) -> {
                log.info("Broadcasting to client: {} with {} connections", clientId, clientEmitters.size());
                
                clientEmitters.removeIf(emitter -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(eventName)
                                .data(data));
                        log.info("Successfully sent event {} to client {}", eventName, clientId);
                        return false;
                    } catch (IOException e) {
                        log.error("Error sending SSE event {} to client {}: {}", eventName, clientId, e.getMessage());
                        return true; // Remove this emitter
                    }
                });
            });
            
            log.info("Completed broadcasting event: {} for order ID: {}", eventName, order.getOrderId());
            
        } catch (JsonProcessingException e) {
            log.error("Error serializing order to JSON for event {}: {}", eventName, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error broadcasting event {}: {}", eventName, e.getMessage(), e);
        }
    }
    
    public void broadcastHeartbeat() {
        emitters.values().forEach(clientEmitters -> {
            clientEmitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data("ping"));
                    return false;
                } catch (IOException e) {
                    log.error("Error sending heartbeat: {}", e.getMessage());
                    return true; // Remove this emitter
                }
            });
        });
    }
    
    public int getActiveConnectionCount() {
        return emitters.values().stream()
                .mapToInt(CopyOnWriteArrayList::size)
                .sum();
    }
}
