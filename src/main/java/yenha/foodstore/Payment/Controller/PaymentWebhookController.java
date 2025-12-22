package yenha.foodstore.Payment.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yenha.foodstore.Payment.DTO.SepayWebhookDTO;
import yenha.foodstore.Payment.DTO.WebhookResponseDTO;
import yenha.foodstore.Payment.Service.PaymentService;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {
    
    private final PaymentService paymentService;
    
    /**
     * Webhook endpoint để nhận thông báo từ SePay khi có giao dịch
     * POST /api/payment/webhook/sepay
     * 
     * Webhook này sẽ được SePay gọi khi có giao dịch chuyển tiền vào tài khoản
     * Content: {"gateway":"MBBank","transactionDate":"2025-12-22 09:22:00",
     *           "accountNumber":"696291102","subAccount":null,"code":"YHF36",
     *           "content":"YHF36","transferType":"in","description":"BankAPINotify YHF36",
     *           "transferAmount":2000,"referenceCode":"FT25356241317830",
     *           "accumulated":8000,"id":36346742}
     */
    @PostMapping("/webhook/sepay")
    public ResponseEntity<WebhookResponseDTO> handleSepayWebhook(@RequestBody SepayWebhookDTO webhook) {
        log.info("============================================================");
        log.info("Received webhook from SePay:");
        log.info("  - ID: {}", webhook.getId());
        log.info("  - Gateway: {}", webhook.getGateway());
        log.info("  - Code: {}", webhook.getCode());
        log.info("  - Content: {}", webhook.getContent());
        log.info("  - Transfer Amount: {}", webhook.getTransferAmount());
        log.info("  - Transfer Type: {}", webhook.getTransferType());
        log.info("  - Transaction Date: {}", webhook.getTransactionDate());
        log.info("============================================================");
        
        try {
            // Gọi service xử lý webhook (synchronous)
            paymentService.processWebhook(webhook);
            
            log.info("Webhook processed successfully for id: {}", webhook.getId());
            
            // Trả về response cho SePay
            return ResponseEntity.ok(new WebhookResponseDTO(true, "OK"));
            
        } catch (Exception e) {
            log.error("============================================================");
            log.error("ERROR processing webhook: {}", e.getMessage());
            log.error("Exception details: ", e);
            log.error("============================================================");
            return ResponseEntity.ok(new WebhookResponseDTO(false, "Error: " + e.getMessage()));
        }
    }
}
