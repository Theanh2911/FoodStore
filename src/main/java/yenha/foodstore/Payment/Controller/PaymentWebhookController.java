package yenha.foodstore.Payment.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yenha.foodstore.Payment.DTO.SepayWebhookDTO;
import yenha.foodstore.Payment.DTO.WebhookResponseDTO;
import yenha.foodstore.Payment.Service.PaymentService;

@RestController
@RequestMapping("/api/payment/webhook")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {
    
    private final PaymentService paymentService;
    
    /**
     * Endpoint nhận webhook từ SePay
     * POST /api/payment/webhook/sepay
     */
    @PostMapping("/sepay")
    public ResponseEntity<WebhookResponseDTO> handleSepayWebhook(@RequestBody SepayWebhookDTO webhook) {
        log.info("Received webhook from SePay: id={}, amount={}, content={}", 
            webhook.getId(), 
            webhook.getTransferAmount(), 
            webhook.getContent());
        
        try {
            // Xử lý async để trả về response ngay lập tức
            paymentService.processWebhook(webhook);
            
            // Trả về 200 OK ngay cho SePay
            return ResponseEntity.ok(WebhookResponseDTO.success());
            
        } catch (Exception e) {
            log.error("Error handling webhook: ", e);
            // Vẫn trả về 200 OK để SePay không retry
            return ResponseEntity.ok(WebhookResponseDTO.success());
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Webhook endpoint is healthy");
    }
}

