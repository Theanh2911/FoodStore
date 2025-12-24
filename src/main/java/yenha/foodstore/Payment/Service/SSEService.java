package yenha.foodstore.Payment.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import yenha.foodstore.Payment.DTO.PaymentEventDTO;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SSEService {
    
    // Map lưu trữ các SseEmitter theo orderId
    // Key: orderId, Value: SseEmitter
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    /**
     * Tạo SSE connection cho một orderId
     * @param orderId ID của đơn hàng
     * @return SseEmitter để FE subscribe
     */
    public SseEmitter createEmitter(Long orderId) {
        // Timeout sau 15 phút (900000ms) - đủ thời gian cho customer thanh toán
        SseEmitter emitter = new SseEmitter(900000L);
        
        // Thêm vào map
        emitters.put(orderId, emitter);
        
        log.info("SSE: Created emitter for orderId: {}", orderId);
        
        // Xử lý khi connection bị đóng
        emitter.onCompletion(() -> {
            log.info("SSE: Emitter completed for orderId: {}", orderId);
            emitters.remove(orderId);
        });
        
        // Xử lý khi timeout
        emitter.onTimeout(() -> {
            log.warn("SSE: Emitter timeout for orderId: {}", orderId);
            emitters.remove(orderId);
        });
        
        // Xử lý khi có lỗi
        emitter.onError((ex) -> {
            log.error("SSE: Emitter error for orderId: {}", orderId, ex);
            emitters.remove(orderId);
        });
        
        // Gửi event đầu tiên để test connection
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("Connected to payment status stream"));
        } catch (IOException e) {
            log.error("SSE: Error sending initial event for orderId: {}", orderId, e);
            emitters.remove(orderId);
        }
        
        return emitter;
    }
    
    /**
     * Gửi event thanh toán thành công tới FE
     * @param orderId ID đơn hàng
     * @param event Payment event data
     */
    public void sendPaymentEvent(Long orderId, PaymentEventDTO event) {
        SseEmitter emitter = emitters.get(orderId);
        
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("payment-status")
                    .data(event));
                
                log.info("SSE: Sent payment event for orderId: {}, status: {}", orderId, event.getStatus());
                
                // Đóng connection sau khi gửi thành công
                emitter.complete();
                emitters.remove(orderId);
                
            } catch (IOException e) {
                log.error("SSE: Error sending payment event for orderId: {}", orderId, e);
                emitter.completeWithError(e);
                emitters.remove(orderId);
            }
        } else {
            log.warn("SSE: No emitter found for orderId: {}", orderId);
        }
    }
    
    /**
     * Kiểm tra có client đang subscribe cho orderId không
     */
    public boolean hasActiveEmitter(Long orderId) {
        return emitters.containsKey(orderId);
    }
    
    /**
     * Đóng tất cả connections (dùng khi shutdown)
     */
    public void closeAllEmitters() {
        emitters.forEach((orderId, emitter) -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("Error closing emitter for orderId: {}", orderId, e);
            }
        });
        emitters.clear();
    }
    
    /**
     * Gửi heartbeat mỗi 30 giây để giữ connection sống
     * Tránh browser/proxy timeout
     */
    @Scheduled(fixedRate = 30000) // Chạy mỗi 30 giây
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }
        
        emitters.forEach((orderId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data("ping"));
                log.debug("SSE: Sent heartbeat for orderId: {}", orderId);
            } catch (IOException e) {
                log.warn("SSE: Heartbeat failed for orderId: {}, removing emitter", orderId);
                emitters.remove(orderId);
            }
        });
    }
}



