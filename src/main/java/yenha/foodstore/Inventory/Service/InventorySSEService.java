package yenha.foodstore.Inventory.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import yenha.foodstore.Inventory.DTO.InventoryUpdateEventDTO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class InventorySSEService {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE emitter completed. Active emitters: {}", emitters.size());
        });
        
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("SSE emitter timed out. Active emitters: {}", emitters.size());
        });
        
        emitter.onError(e -> {
            emitters.remove(emitter);
            log.error("SSE emitter error: {}", e.getMessage());
        });
        
        emitters.add(emitter);
        log.info("New SSE emitter created. Active emitters: {}", emitters.size());
        
        return emitter;
    }

    public void broadcastInventoryUpdate(Long productId, Integer numberRemain) {
        if (emitters.isEmpty()) {
            return;
        }

        InventoryUpdateEventDTO event = new InventoryUpdateEventDTO(
            productId,
            numberRemain,
            System.currentTimeMillis()
        );

        List<SseEmitter> deadEmitters = new ArrayList<>();

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("inventory-update")
                    .data(event));
                log.debug("Broadcasted inventory update for product {} to client", productId);
            } catch (IOException e) {
                deadEmitters.add(emitter);
                log.error("Failed to send SSE event: {}", e.getMessage());
            }
        });

        emitters.removeAll(deadEmitters);
        
        if (!deadEmitters.isEmpty()) {
            log.info("Removed {} dead emitters. Active emitters: {}", deadEmitters.size(), emitters.size());
        }
    }

    public void sendInitialData(SseEmitter emitter, List<?> data) {
        try {
            emitter.send(SseEmitter.event()
                .name("inventory-init")
                .data(data));
            log.debug("Sent initial inventory data to client");
        } catch (IOException e) {
            emitters.remove(emitter);
            log.error("Failed to send initial data: {}", e.getMessage());
        }
    }

    public int getActiveConnections() {
        return emitters.size();
    }
}
