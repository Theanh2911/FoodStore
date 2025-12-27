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
     * This endpoint will track the progress of payment for a specific order in a real-time manner using Server-Sent Events (SSE).
     * GET /api/payment/events/{orderId}
     */
    @GetMapping(value = "/events/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribePaymentEvents(@PathVariable Long orderId) {
        log.info("FE subscribing to payment events for orderId: {}", orderId);
        return sseService.createEmitter(orderId);
    }
    
    /**
     * Check if client is listening
     */
    @GetMapping("/events/{orderId}/status")
    public ResponseEntity<Boolean> checkSubscriptionStatus(@PathVariable Long orderId) {
        boolean hasActiveEmitter = sseService.hasActiveEmitter(orderId);
        return ResponseEntity.ok(hasActiveEmitter);
    }
}





