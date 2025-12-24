package yenha.foodstore.Payment.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import yenha.foodstore.Payment.Service.SSEService;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentSSEController {
    
    private final SSEService sseService;
    
    /**
     * SSE endpoint để FE subscribe theo dõi trạng thái thanh toán
     * GET /api/payment/events/{orderId}
     * 
     * Cách FE sử dụng:
     * const eventSource = new EventSource('/api/payment/events/123');
     * eventSource.addEventListener('payment-status', (event) => {
     *     const data = JSON.parse(event.data);
     *     if (data.status === 'SUCCESS') {
     *         // Hiển thị thanh toán thành công
     *     }
     * });
     */
    @GetMapping(value = "/events/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribePaymentEvents(@PathVariable Long orderId) {
        log.info("FE subscribing to payment events for orderId: {}", orderId);
        return sseService.createEmitter(orderId);
    }
    
    /**
     * Kiểm tra có client đang subscribe không
     */
    @GetMapping("/events/{orderId}/status")
    public ResponseEntity<Boolean> checkSubscriptionStatus(@PathVariable Long orderId) {
        boolean hasActiveEmitter = sseService.hasActiveEmitter(orderId);
        return ResponseEntity.ok(hasActiveEmitter);
    }
}

