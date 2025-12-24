package yenha.foodstore.Payment.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yenha.foodstore.Order.Entity.Order;
import yenha.foodstore.Order.Entity.OrderStatus;
import yenha.foodstore.Order.Repository.OrderRepository;
import yenha.foodstore.Payment.DTO.PaymentEventDTO;
import yenha.foodstore.Payment.DTO.SepayWebhookDTO;
import yenha.foodstore.Payment.Entity.Payment;
import yenha.foodstore.Payment.Entity.PaymentStatus;
import yenha.foodstore.Payment.Repository.PaymentRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SSEService sseService;
    
    // Pattern để extract orderId từ content
    // VD: "YHF123", "DH123", "Order 123", "don hang 123"
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("(?:YHF|DH|Order|don hang|donhang)\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    
    /**
     * Xử lý webhook từ SePay
     * @param webhook Data từ SePay
     */
    @Transactional
    public void processWebhook(SepayWebhookDTO webhook) {
        log.info("▶ [STEP 0] Starting webhook processing: sepayId={}", webhook.getId());
        
        try {
            // 1. Kiểm tra duplicate
            log.info("▶ [STEP 1] Checking for duplicate transaction...");
            if (paymentRepository.existsBySepayTransactionId(webhook.getId())) {
                log.warn("✗ [FAILED] Duplicate webhook detected, sepayTransactionId: {}", webhook.getId());
                return;
            }
            log.info("✓ [STEP 1] No duplicate found");
            
            // 2. Validate webhook
            log.info("▶ [STEP 2] Validating webhook data...");
            if (!validateWebhook(webhook)) {
                log.error("✗ [FAILED] Invalid webhook data - transferType: {}, amount: {}", 
                    webhook.getTransferType(), webhook.getTransferAmount());
                saveFailedPayment(webhook, "Invalid webhook data");
                return;
            }
            log.info("✓ [STEP 2] Webhook data is valid");
            
            // 3. Extract orderId từ content hoặc code
            log.info("▶ [STEP 3] Extracting orderId from code='{}' or content='{}'...", 
                webhook.getCode(), webhook.getContent());
            Long orderId = extractOrderId(webhook);
            
            if (orderId == null) {
                log.error("✗ [FAILED] Cannot extract orderId from webhook. Code='{}', Content='{}'", 
                    webhook.getCode(), webhook.getContent());
                saveFailedPayment(webhook, "Cannot extract orderId");
                return;
            }
            log.info("✓ [STEP 3] Extracted orderId: {}", orderId);
            
            // 4. Kiểm tra Order tồn tại
            log.info("▶ [STEP 4] Checking if order exists: orderId={}", orderId);
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                log.error("✗ [FAILED] Order not found in database: orderId={}", orderId);
                saveFailedPayment(webhook, "Order not found: " + orderId);
                sseService.sendPaymentEvent(orderId, PaymentEventDTO.failed(orderId, "Không tìm thấy đơn hàng"));
                return;
            }
            log.info("✓ [STEP 4] Order found");
            
            Order order = orderOpt.get();
            log.info("▶ [STEP 5] Order details: id={}, status={}, totalAmount={}", 
                order.getOrderId(), order.getStatus(), order.getTotalAmount());
            
            // 5. Kiểm tra order phải ở trạng thái SERVED mới cho thanh toán
            log.info("▶ [STEP 6] Checking order status...");
            if (order.getStatus() != OrderStatus.SERVED) {
                log.error("✗ [FAILED] Order status is not SERVED. Current status: {}, orderId: {}", 
                    order.getStatus(), orderId);
                saveFailedPayment(webhook, "Order must be SERVED before payment. Current status: " + order.getStatus());
                sseService.sendPaymentEvent(orderId, PaymentEventDTO.failed(orderId, "Đơn hàng chưa được phục vụ"));
                return;
            }
            log.info("✓ [STEP 6] Order status is SERVED");
            
            // 6. Validate số tiền
            log.info("▶ [STEP 7] Validating amount: expected={}, received={}", 
                order.getTotalAmount(), webhook.getTransferAmount());
            if (!validateAmount(webhook.getTransferAmount(), order.getTotalAmount())) {
                log.error("✗ [FAILED] Amount mismatch. Expected: {}, Got: {}, Difference: {}", 
                    order.getTotalAmount(), webhook.getTransferAmount(), 
                    Math.abs(webhook.getTransferAmount() - order.getTotalAmount()));
                saveAmountMismatchPayment(webhook, orderId, "Amount mismatch");
                sseService.sendPaymentEvent(orderId, PaymentEventDTO.failed(orderId, "Số tiền không khớp"));
                return;
            }
            log.info("✓ [STEP 7] Amount is valid");
            
            // 7. Lưu Payment thành công
            log.info("▶ [STEP 8] Saving payment record...");
            Payment payment = saveSuccessfulPayment(webhook, orderId);
            log.info("✓ [STEP 8] Payment saved: paymentId={}", payment.getId());
            
            // 8. Cập nhật Order status: SERVED → PAID
            log.info("▶ [STEP 9] Updating order status from SERVED to PAID...");
            updateOrderStatus(order);
            log.info("✓ [STEP 9] Order status updated to PAID");
            
            // 9. Gửi SSE event cho FE
            log.info("▶ [STEP 10] Sending SSE event to frontend...");
            PaymentEventDTO event = PaymentEventDTO.success(
                orderId, 
                payment.getId(), 
                payment.getTransferAmount(),
                payment.getGateway(),
                payment.getTransactionDate().toString()
            );
            
            sseService.sendPaymentEvent(orderId, event);
            log.info("✓ [STEP 10] SSE event sent");
            
            log.info("✓✓✓ Payment processed successfully for orderId: {}, sepayId: {}", orderId, webhook.getId());
            
        } catch (Exception e) {
            log.error("✗✗✗ EXCEPTION during webhook processing: ", e);
            throw e; // Re-throw to let controller handle it
        }
    }
    
    /**
     * Validate webhook data
     */
    private boolean validateWebhook(SepayWebhookDTO webhook) {
        // Kiểm tra chỉ nhận tiền vào
        if (!"in".equalsIgnoreCase(webhook.getTransferType())) {
            log.warn("Invalid transferType: {}", webhook.getTransferType());
            return false;
        }
        
        // Kiểm tra số tiền > 0
        if (webhook.getTransferAmount() == null || webhook.getTransferAmount() <= 0) {
            log.warn("Invalid transferAmount: {}", webhook.getTransferAmount());
            return false;
        }
        
        return true;
    }
    
    /**
     * Extract orderId từ content hoặc code
     */
    private Long extractOrderId(SepayWebhookDTO webhook) {
        // Ưu tiên lấy từ code nếu có
        if (webhook.getCode() != null && !webhook.getCode().isEmpty()) {
            // Thử parse trực tiếp là số
            try {
                Long orderId = Long.parseLong(webhook.getCode());
                log.info("✓ Extracted orderId {} from code (direct parsing)", orderId);
                return orderId;
            } catch (NumberFormatException e) {
                log.debug("Code '{}' is not a pure number, trying regex...", webhook.getCode());
                
                // Thử dùng regex để extract số từ code
                Matcher matcher = ORDER_ID_PATTERN.matcher(webhook.getCode());
                if (matcher.find()) {
                    try {
                        Long orderId = Long.parseLong(matcher.group(1));
                        log.info("✓ Extracted orderId {} from code '{}' using regex", orderId, webhook.getCode());
                        return orderId;
                    } catch (NumberFormatException e2) {
                        log.warn("Cannot parse orderId from code regex match: {}", matcher.group(1));
                    }
                }
            }
        }
        
        // Nếu không extract được từ code, thử từ content
        if (webhook.getContent() != null) {
            Matcher matcher = ORDER_ID_PATTERN.matcher(webhook.getContent());
            if (matcher.find()) {
                try {
                    Long orderId = Long.parseLong(matcher.group(1));
                    log.info("✓ Extracted orderId {} from content '{}' using regex", orderId, webhook.getContent());
                    return orderId;
                } catch (NumberFormatException e) {
                    log.warn("Cannot parse orderId from content regex match: {}", matcher.group(1));
                }
            }
        }
        
        log.error("✗ Failed to extract orderId from code='{}' or content='{}'", 
            webhook.getCode(), webhook.getContent());
        return null;
    }
    
    /**
     * Validate số tiền (cho phép sai số 1000 VND)
     */
    private boolean validateAmount(Double transferAmount, Double expectedAmount) {
        if (transferAmount == null || expectedAmount == null) {
            return false;
        }
        
        double difference = Math.abs(transferAmount - expectedAmount);
        return difference <= 1000; // Cho phép sai số 1000 VND
    }
    
    /**
     * Lưu payment thành công
     */
    private Payment saveSuccessfulPayment(SepayWebhookDTO webhook, Long orderId) {
        Payment payment = new Payment();
        payment.setSepayTransactionId(webhook.getId());
        payment.setOrderId(orderId);
        payment.setGateway(webhook.getGateway());
        payment.setTransactionDate(parseTransactionDate(webhook.getTransactionDate()));
        payment.setAccountNumber(webhook.getAccountNumber());
        payment.setContent(webhook.getContent());
        payment.setTransferAmount(webhook.getTransferAmount());
        payment.setReferenceCode(webhook.getReferenceCode());
        payment.setDescription(webhook.getDescription());
        payment.setStatus(PaymentStatus.SUCCESS);
        
        return paymentRepository.save(payment);
    }
    
    /**
     * Lưu payment khi số tiền không khớp
     */
    private void saveAmountMismatchPayment(SepayWebhookDTO webhook, Long orderId, String errorMessage) {
        Payment payment = new Payment();
        payment.setSepayTransactionId(webhook.getId());
        payment.setOrderId(orderId);
        payment.setGateway(webhook.getGateway());
        payment.setTransactionDate(parseTransactionDate(webhook.getTransactionDate()));
        payment.setAccountNumber(webhook.getAccountNumber());
        payment.setContent(webhook.getContent());
        payment.setTransferAmount(webhook.getTransferAmount());
        payment.setReferenceCode(webhook.getReferenceCode());
        payment.setDescription(webhook.getDescription());
        payment.setStatus(PaymentStatus.AMOUNT_MISMATCH);
        payment.setErrorMessage(errorMessage);
        
        paymentRepository.save(payment);
    }
    
    /**
     * Lưu payment khi thất bại
     */
    private void saveFailedPayment(SepayWebhookDTO webhook, String errorMessage) {
        Payment payment = new Payment();
        payment.setSepayTransactionId(webhook.getId());
        payment.setGateway(webhook.getGateway());
        payment.setTransactionDate(parseTransactionDate(webhook.getTransactionDate()));
        payment.setAccountNumber(webhook.getAccountNumber());
        payment.setContent(webhook.getContent());
        payment.setTransferAmount(webhook.getTransferAmount());
        payment.setReferenceCode(webhook.getReferenceCode());
        payment.setDescription(webhook.getDescription());
        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorMessage(errorMessage);
        
        paymentRepository.save(payment);
    }
    
    /**
     * Parse transaction date từ String sang LocalDateTime
     */
    private LocalDateTime parseTransactionDate(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateStr, formatter);
        } catch (Exception e) {
            log.error("Error parsing transaction date: {}", dateStr, e);
            return LocalDateTime.now();
        }
    }
    
    /**
     * Cập nhật trạng thái Order: SERVED → PAID
     */
    private void updateOrderStatus(Order order) {
        if (order.getStatus() == OrderStatus.SERVED) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            log.info("Order status updated from SERVED to PAID for orderId: {}", order.getOrderId());
        } else {
            log.warn("Cannot update order status. Order is not SERVED. Current status: {}, orderId: {}", 
                order.getStatus(), order.getOrderId());
        }
    }
}



